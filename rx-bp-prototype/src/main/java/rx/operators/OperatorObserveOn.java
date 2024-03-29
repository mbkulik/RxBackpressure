/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Scheduler;
import rx.Subscriber;
import rx.Observable.Operator;
import rx.Scheduler.Inner;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.ImmediateScheduler;
import rx.schedulers.TrampolineScheduler;
import rx.subscriptions.Subscriptions;

/**
 * Delivers events on the specified Scheduler asynchronously via an unbounded buffer.
 * 
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/observeOn.png">
 */
public class OperatorObserveOn<T> implements Operator<T, T> {

    private final Scheduler scheduler;

    /**
     * @param scheduler
     */
    public OperatorObserveOn(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        if (scheduler instanceof ImmediateScheduler) {
            // avoid overhead, execute directly
            return child;
        } else if (scheduler instanceof TrampolineScheduler) {
            // avoid overhead, execute directly
            return child;
        } else {
            return new ObserveOnSubscriber(child);
        }
    }

    private static class Sentinel {

    }

    private static Sentinel NULL_SENTINEL = new Sentinel();
    private static Sentinel COMPLETE_SENTINEL = new Sentinel();

    private static class ErrorSentinel extends Sentinel {
        final Throwable e;

        ErrorSentinel(Throwable e) {
            this.e = e;
        }
    }

    /** Observe through individual queue per observer. */
    private class ObserveOnSubscriber extends Subscriber<T> {
        final Subscriber<? super T> observer;
        private volatile Scheduler.Inner recursiveScheduler;
        private static final int SIZE = 10;

        private final ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(SIZE + 1); // +1 for onCompleted if onNext fills buffer
        final AtomicInteger counter = new AtomicInteger(0);
        private int requested = 0;

        public ObserveOnSubscriber(Subscriber<? super T> observer) {
            this.observer = observer;
            requested += SIZE;
            request(SIZE);
            observer.add(Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    // propagate unsubscribe from child to parent
                    unsubscribe();
                }

            }));
        }

        @Override
        public void onNext(final T t) {
            boolean success = false;
            if (t == null) {
                success = queue.offer(NULL_SENTINEL);
            } else {
                success = queue.offer(t);
            }
            if (!success) {
                // TODO schedule onto inner after clearing the queue and cancelling existing work
                observer.onError(new IllegalStateException("Unable to queue onNext as queue full => " + SIZE + " items. Backpressure request ignored."));
                return;
            }
            schedule();
        }

        @Override
        public void onCompleted() {
            if (!queue.offer(COMPLETE_SENTINEL)) {
                observer.onError(new IllegalStateException("Unable to queue onCompleted as queue full => " + SIZE + " items. Backpressure request ignored."));
            }
            schedule();
        }

        @Override
        public void onError(final Throwable e) {
            unsubscribe(); // unsubscribe upwards to shut down (do this here so we don't have delay across threads of final SafeSubscriber doing this)

            queue.clear(); // is this the right way to deal with this? it's the equivalent of tearing down the stack which is what we want
            // TODO schedule onto inner after clearing the queue and cancelling existing work
            if (!queue.offer(new ErrorSentinel(e))) {
                observer.onError(new IllegalStateException("Unable to queue onError as queue full => " + SIZE + " items. Backpressure request ignored."));
            }
            schedule();
        }

        protected void schedule() {
            if (counter.getAndIncrement() == 0) {
                if (recursiveScheduler == null) {
                    // attach subscription to child so when it is cleaned up this gets cleaned up
                    // don't attach to this (parent) as unsubscribing up should not prevent terminal events from sending
                    observer.add(scheduler.schedule(new Action1<Inner>() {

                        @Override
                        public void call(Inner inner) {
                            recursiveScheduler = inner;
                            pollQueue();
                        }

                    }));
                } else {
                    recursiveScheduler.schedule(new Action1<Inner>() {

                        @Override
                        public void call(Inner inner) {
                            pollQueue();
                        }

                    });
                }
            }
        }

        /**
         * This will be invoked by only a single thread, managed by the counter.increment/decrement
         */
        @SuppressWarnings("unchecked")
        private void pollQueue() {
            do {
                Object v = queue.poll();
                if (v != null) {
                    if (v instanceof Sentinel) {
                        if (v == NULL_SENTINEL) {
                            observer.onNext(null);
                        } else if (v == COMPLETE_SENTINEL) {
                            observer.onCompleted();
                        } else if (v instanceof ErrorSentinel) {
                            observer.onError(((ErrorSentinel) v).e);
                        }
                    } else {
                        observer.onNext((T) v);
                    }
                }
                requested--;
            } while (counter.decrementAndGet() > 0 && !observer.isUnsubscribed());
            if (requested == 0) {
                requested += SIZE;
                System.err.println(">>> ObserveOn => Request More: " + SIZE + " requested: " + requested + " counter: " + counter.get());
                request(SIZE);
            }
        }

    }
}