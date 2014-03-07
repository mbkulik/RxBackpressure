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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OperatorFromIterableTest {

    @Test
    public void testIterable() {
        Observable<String> observable = Observable.create(new OnSubscribeFromIterable<String>(Arrays.<String> asList("one", "two", "three")));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testObservableFromIterable() {
        Observable<String> observable = Observable.from(Arrays.<String> asList("one", "two", "three"));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testBackpressure() {
        Observable<Integer> observable = Observable.from(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int i = 0;

                    @Override
                    public void remove() {
                    }

                    @Override
                    public Integer next() {
                        try {
                            Thread.sleep(0);
                        } catch (InterruptedException e) {
                        }
                        return i++;
                    }

                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                };
            }
        });

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                System.err.println("c t = "+ t +" thread "+ Thread.currentThread());
                super.onNext(t);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        };

        observable
        .observeOn(Schedulers.newThread())
        .take(7)
        .subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        System.err.println(testSubscriber.getOnNextEvents());
        testSubscriber.assertReceivedOnNext(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
    }

    
    @Test
    public void testBackpressureWithTake() {
        Observable<Integer> observable = Observable.from(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int i = 0;

                    @Override
                    public void remove() {
                    }

                    @Override
                    public Integer next() {
                        try {
                            Thread.sleep(0);
                        } catch (InterruptedException e) {
                        }
                        return i++;
                    }

                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                };
            }
        });

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>();
        observable
        .take(7)
        .observeOn(Schedulers.newThread())
//        .doOnEach(new Action1<Notification<? super Integer>>() {
//            @Override
//            public void call(Notification<? super Integer> t1) {
//                //System.err.println("c t = "+ t1.getValue() +" thread "+ Thread.currentThread());
//            }
//        })
        .subscribe(testSubscriber);
        
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertReceivedOnNext(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
    }
}
