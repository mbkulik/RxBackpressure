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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.FuncN;
import rx.subscriptions.CompositeSubscription;

public final class OperatorZip<R> implements Operator<R, Observable<?>> {
    private final FuncN<? extends R> zipFunc;

    public OperatorZip(FuncN<? extends R> zipFunction) {
        this.zipFunc = zipFunction;
    }

    @Override
    public Subscriber<? super Observable<?>> call(final Subscriber<? super R> out) {
        final CompositeSubscription innerSubscriptions = new CompositeSubscription();
        // if the outter is unsubscribed add it so that it can propagate it to the inner
        out.add(innerSubscriptions);

        final Subscriber<Observable<?>> outerSubscriber = new Subscriber<Observable<?>>(out) {
            /** the number of Inner observables not the final value until startUpMode is false */
            private AtomicInteger width = new AtomicInteger();
            /**
             * true until we get the onCompleted to the outer subscriber and we know the actual
             * width of river
             */
            private final AtomicBoolean startUpMode = new AtomicBoolean(true);
            private final AtomicBoolean done = new AtomicBoolean(false);
            private final ConcurrentHashMap<Integer, Object> values = new ConcurrentHashMap<Integer, Object>();
            private final AtomicInteger count = new AtomicInteger();
            private final List<Inner> innerSubscribers = new ArrayList<Inner>();

            @Override
            public void onError(Throwable e) {
                if (done.compareAndSet(false, true) && !isUnsubscribed())
                    out.onError(e);
            }

            @Override
            public void onCompleted() {
                if (startUpMode.compareAndSet(true, false)) {
                    if (width.get() == 0) {
                        out.onCompleted();
                        return;
                    }

                    // start all of the inner observers
                    for (Inner inner : innerSubscribers) {
                        inner.resume();
                    }
                }
            }

            @Override
            public void onNext(Observable<?> inner) {
                final Inner innerSubscriber = new Inner(width.getAndIncrement());
                innerSubscribers.add(innerSubscriber);
                // when one inner subscriber gets an onComplete this allows the unsubscription from
                // the others. when unsubscribing from the inner propagate that to the outer.
                innerSubscriptions.add(innerSubscriber);
                innerSubscriber.pause();
                inner.subscribe(innerSubscriber);
            }

            class Inner extends Subscriber<Object> {
                private final int index;

                // constructor to make it clear that the index is set on creation from the outer
                // onNext
                private Inner(int index) {
                    super();
                    this.index = index;
                }

                @Override
                public void onCompleted() {
                    if (done.compareAndSet(false, true) && !isUnsubscribed()) {
                        out.onCompleted();
                        innerSubscriptions.unsubscribe();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    if (done.compareAndSet(false, true) && !isUnsubscribed()) {
                        out.onError(e);
                        innerSubscriptions.unsubscribe();
                    }
                }

                @Override
                public void onNext(Object value) {
                    Object oldValue = values.putIfAbsent(index, value == null ? NULL : value);
                    if (oldValue != null) {
                        // TODO handle misbehaving observables
                        out.onError(new IllegalStateException("one of the observables didn't pause"));
                    }

                    // check to see if we have enough data to call the downstream onNext
                    int c = count.incrementAndGet();
                    if (c == width.get()) {
                        Object[] args = new Object[width.get()];
                        for (Entry<Integer, Object> arg : values.entrySet()) {
                            args[arg.getKey()] = arg.getValue() == NULL ? null : arg.getValue();
                        }
                        values.clear();
                        R result = zipFunc.call(args);
                        out.onNext(result);
                        count.set(0);

                        // restart all of the inner subscribers again.
                        for (Inner inner : innerSubscribers) {
                            inner.resume();
                        }
                    }
                    else {
                        pause();
                    }
                }

                @Override
                public void resume() {
                    super.resume();
                }
            }
        };
        return outerSubscriber;
    }

    private static final Object NULL = new Object();
}
