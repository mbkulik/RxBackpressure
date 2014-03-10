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

import rx.Observable;
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscriber;
import rx.functions.Action1;

/**
 * Subscribes Observers on the specified Scheduler.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/subscribeOn.png">
 */
public class OperatorSubscribeOn<T> implements Operator<T, Observable<T>> {

    private final Scheduler scheduler;

    public OperatorSubscribeOn(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super Observable<T>> call(final Subscriber<? super T> subscriber) {
        return new Subscriber<Observable<T>>(subscriber) {

            @Override
            public void onCompleted() {
                // ignore because this is a nested Observable and we expect only 1 Observable<T> emitted to onNext
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onNext(final Observable<T> o) {
                System.out.println("onNext in SubscribeOn <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                subscriber.add(scheduler.schedule(new Action1<Inner>() {

                    @Override
                    public void call(final Inner inner) {
                        System.out.println("******************* subscribe");
                        o.unsafeSubscribe(new Subscriber<T>(subscriber) {

                            @Override
                            public void onCompleted() {
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onError(Throwable e) {
                                subscriber.onError(e);
                            }

                            @Override
                            public void onNext(T t) {
                                subscriber.onNext(t);
                            }

                            @Override
                            public void setProducer(final Action1<rx.Subscriber.Request> producer) {
                                subscriber.setProducer(new Action1<Request>() {

                                    @Override
                                    public void call(final rx.Subscriber.Request r) {
                                        inner.schedule(new Action1<Inner>() {

                                            @Override
                                            public void call(Inner inner) {
                                                producer.call(r);
                                            }

                                        });

                                    }

                                });

                            }

                        });
                    }
                }));
            }

        };
    }
}
