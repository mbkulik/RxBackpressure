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
package rx.subjects;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

/* package */class SubjectSubscriptionManager<T> {

    private AtomicReference<State<T>> state = new AtomicReference<State<T>>(new State<T>());

    /**
     * 
     * @param onSubscribe
     *            Always runs at the beginning of 'subscribe' regardless of terminal state.
     * @param onTerminated
     *            Only runs if Subject is in terminal state and the Observer ends up not being
     *            registered.
     * @return
     */
    public OnSubscribe<T> getOnSubscribe(final Action1<SubjectSubscriber<? super T>> onSubscribe, final Action1<SubjectSubscriber<? super T>> onTerminated) {
        return new OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> actualOperator) {
                final SubjectSubscriber<T> observer = new SubjectSubscriber<T>(actualOperator);
                // invoke onSubscribe logic
                if (onSubscribe != null) {
                    onSubscribe.call(observer);
                }

                State<T> current;
                State<T> newState = null;
                boolean addedObserver = false;
                do {
                    current = state.get();
                    if (current.terminated) {
                        // we are terminated so don't need to do anything
                        addedObserver = false;
                        // break out and don't try to modify state
                        newState = current;
                        // wait for termination to complete if
                        try {
                            current.terminationLatch.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted waiting for termination.", e);
                        }
                        break;
                    } else {
                        actualOperator.add(Subscriptions.create(new Action0() {

                            @Override
                            public void call() {
                                State<T> current;
                                State<T> newState;
                                do {
                                    current = state.get();
                                    // on unsubscribe remove it from the map of outbound observers
                                    // to notify
                                    newState = current.removeObserver(observer);
                                } while (!state.compareAndSet(current, newState));
                            }
                        }));

                        // on subscribe add it to the map of outbound observers to notify
                        newState = current.addObserver(observer);
                    }
                } while (!state.compareAndSet(current, newState));

                /**
                 * Whatever happened above, if we are terminated we run `onTerminated`
                 */
                if (newState.terminated && !addedObserver) {
                    onTerminated.call(observer);
                }
            }

        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void terminate(Action1<Collection<SubjectSubscriber<? super T>>> onTerminate) {
        State<T> current;
        State<T> newState = null;
        do {
            current = state.get();
            if (current.terminated) {
                // already terminated so do nothing
                return;
            } else {
                newState = current.terminate();
            }
        } while (!state.compareAndSet(current, newState));

        /*
         * if we get here then we won setting the state to terminated
         * and have a deterministic set of Observers to emit to (concurrent subscribes
         * will have failed and will try again and see we are term
         * inated)
         */
        try {
            // had to circumvent type check, we know what the array contains
            onTerminate.call((Collection) Arrays.asList(newState.subscribers));
        } finally {
            // mark that termination is completed
            newState.terminationLatch.countDown();
        }
    }

    /**
     * Returns the array of observers directly. <em>Don't modify the array!</em>
     * 
     * @return the array of current observers
     */
    @SuppressWarnings("unchecked")
    public SubjectSubscriber<Object>[] rawSnapshot() {
        return state.get().subscribers;
    }

    @SuppressWarnings("rawtypes")
    protected static class State<T> {
        final boolean terminated;
        final CountDownLatch terminationLatch;
        final SubjectSubscriber[] subscribers;

        private State(boolean isTerminated, CountDownLatch terminationLatch, SubjectSubscriber[] subscribers) {
            this.terminationLatch = terminationLatch;
            this.terminated = isTerminated;
            this.subscribers = subscribers;
        }

        State() {
            this.terminated = false;
            this.terminationLatch = null;
            this.subscribers = new SubjectSubscriber[0];
        }

        public State<T> terminate() {
            if (terminated) {
                throw new IllegalStateException("Already terminated.");
            }
            return new State<T>(true, new CountDownLatch(1), subscribers);
        }

        public State<T> addObserver(SubjectSubscriber<? super T> subscriber) {
            int n = this.subscribers.length;

            // only allow one instance of each subscriber in
            for (int i = 0; i < n; i++) {
                if (this.subscribers[i] == subscriber) {
                    return this;
                }
            }

            SubjectSubscriber[] newSubscribers = Arrays.copyOf(this.subscribers, n + 1);

            newSubscribers[n] = subscriber;

            return createNewWith(newSubscribers);
        }

        private State<T> createNewWith(SubjectSubscriber[] newSubscribers) {
            return new State<T>(terminated, terminationLatch, newSubscribers);
        }

        public State<T> removeObserver(SubjectSubscriber<? super T> subscriber) {
            // we are empty, nothing to remove
            if (this.subscribers.length == 0) {
                return this;
            }

            int n = this.subscribers.length;

            // find the subscriber in the array
            int index = -1;
            for (int i = 0; i < n; i++) {
                if (this.subscribers[i] == subscriber) {
                    if (index != -1) {
                        throw new IllegalStateException("Somehow dupilcate subscribers got in");
                    }
                    index = i;
                }
            }

            if (index == -1) {
                // subscriber wasn't found
                return this;
            }

            int newN = this.subscribers.length - 1;
            if (newN == 0) {
                // it was the last subscriber
                return createNewWith(new SubjectSubscriber[0]);
            }

            SubjectSubscriber[] newSubscribers = new SubjectSubscriber[newN];
            // example: n=10, newN=9, index=4.
            // copy everything before the subscriber. example: then copy 0-3 to 0-3
            System.arraycopy(subscribers, 0, newSubscribers, 0, index);
            // copy everything after the subscriber. example: and copy 5-9 to 4-8
            System.arraycopy(subscribers, index + 1, newSubscribers, index, newN - index);
            
            return createNewWith(newSubscribers);
        }
    }

    protected static class SubjectSubscriber<T> extends Subscriber<T> {

        private final Subscriber<? super T> actual;
        protected volatile boolean caughtUp = false;

        SubjectSubscriber(Subscriber<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void onCompleted() {
            this.actual.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            this.actual.onError(e);
        }

        @Override
        public void onNext(T v) {
            this.actual.onNext(v);
        }

    }

}