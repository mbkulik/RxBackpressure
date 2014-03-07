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
package rx.subscriptions;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.functions.Action0;

public class CompositeSubscriptionTest {

    @Test
    public void testSuccess() {
        final AtomicInteger counter = new AtomicInteger();
        CompositeSubscription s = new CompositeSubscription();
        s.add(BooleanSubscription.create(new Action0() {
            @Override
            public void call() {
                counter.incrementAndGet();
            }
        }));

        s.add(BooleanSubscription.create(new Action0() {
            @Override
            public void call() {
                counter.incrementAndGet();
            }
        }));

        s.unsubscribe();

        assertEquals(2, counter.get());
    }

    @Test(timeout = 1000)
    public void shouldUnsubscribeAll() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        final CompositeSubscription s = new CompositeSubscription();

        final int count = 10;
        final CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < count; i++) {
            s.add(BooleanSubscription.create(new Action0() {
                @Override
                public void call() {
                    counter.incrementAndGet();
                }
            }));
        }

        final List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < count; i++) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        start.await();
                        s.unsubscribe();
                    } catch (final InterruptedException e) {
                        fail(e.getMessage());
                    }
                }
            };
            t.start();
            threads.add(t);
        }

        start.countDown();
        for (final Thread t : threads) {
            t.join();
        }

        assertEquals(count, counter.get());
    }

    @Test
    public void testException() {
        final AtomicInteger counter = new AtomicInteger();
        CompositeSubscription s = new CompositeSubscription();
        s.add(BooleanSubscription.create(new Action0() {
            @Override
            public void call() {
                throw new RuntimeException("failed on first one");
            }
        }));

        s.add(BooleanSubscription.create(new Action0() {
            @Override
            public void call() {
                counter.incrementAndGet();
            }
        }));

        try {
            s.unsubscribe();
            fail("Expecting an exception");
        } catch (RuntimeException e) {
            // we expect this
            assertEquals(e.getMessage(), "failed on first one");
        }

        // we should still have unsubscribed to the second one
        assertEquals(1, counter.get());
    }

    @Test
    public void testCompositeException() {
        final AtomicInteger counter = new AtomicInteger();
        CompositeSubscription s = new CompositeSubscription();
        s.add(BooleanSubscription.create(new Action0() {
            @Override
            public void call() {
                throw new RuntimeException("failed on first one");
            }
        }));

        s.add(BooleanSubscription.create(new Action0() {
            @Override
            public void call() {
                throw new RuntimeException("failed on second one too");
            }
        }));

        s.add(BooleanSubscription.create(new Action0() {
            @Override
            public void call() {
                counter.incrementAndGet();
            }
        }));

        try {
            s.unsubscribe();
            fail("Expecting an exception");
        } catch (CompositeException e) {
            // we expect this
            assertEquals(e.getExceptions().size(), 2);
        }

        // we should still have unsubscribed to the second one
        assertEquals(1, counter.get());
    }

    @Test
    public void testRemoveUnsubscribes() {
        BooleanSubscription s1 = BooleanSubscription.create();
        BooleanSubscription s2 = BooleanSubscription.create();

        CompositeSubscription s = new CompositeSubscription();
        s.add(s1);
        s.add(s2);

        s.remove(s1);

        assertTrue(s1.isUnsubscribed());
        assertFalse(s2.isUnsubscribed());
    }

    @Test
    public void testClear() {
        BooleanSubscription s1 = BooleanSubscription.create();
        BooleanSubscription s2 = BooleanSubscription.create();

        CompositeSubscription s = new CompositeSubscription();
        s.add(s1);
        s.add(s2);

        assertFalse(s1.isUnsubscribed());
        assertFalse(s2.isUnsubscribed());

        s.clear();

        assertTrue(s1.isUnsubscribed());
        assertTrue(s2.isUnsubscribed());
        assertFalse(s.isUnsubscribed());

        BooleanSubscription s3 = BooleanSubscription.create();

        s.add(s3);
        s.unsubscribe();

        assertTrue(s3.isUnsubscribed());
        assertTrue(s.isUnsubscribed());
    }

    @Test
    public void testUnsubscribeIdempotence() {
        final AtomicInteger counter = new AtomicInteger();
        CompositeSubscription s = new CompositeSubscription();
        s.add(BooleanSubscription.create(new Action0() {
            @Override
            public void call() {
                counter.incrementAndGet();
            }
        }));

        s.unsubscribe();
        s.unsubscribe();
        s.unsubscribe();

        // we should have only unsubscribed once
        assertEquals(1, counter.get());
    }

    @Test(timeout = 1000)
    public void testUnsubscribeIdempotenceConcurrently()
            throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        final CompositeSubscription s = new CompositeSubscription();

        final int count = 10;
        final CountDownLatch start = new CountDownLatch(1);
        s.add(BooleanSubscription.create(new Action0() {
            @Override
            public void call() {
                counter.incrementAndGet();
            }
        }));

        final List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < count; i++) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        start.await();
                        s.unsubscribe();
                    } catch (final InterruptedException e) {
                        fail(e.getMessage());
                    }
                }
            };
            t.start();
            threads.add(t);
        }

        start.countDown();
        for (final Thread t : threads) {
            t.join();
        }

        // we should have only unsubscribed once
        assertEquals(1, counter.get());
    }
    
    @Test
    public void testPauseCompability() {
        final AtomicInteger completedCount = new AtomicInteger();
        Observable<Integer> src = Observable.empty();
        Observable<Integer> mid = src.lift(new Operator<Integer, Integer>() {
            @Override
            public Subscriber<? super Integer> call(final Subscriber<? super Integer> out) {
                return new Subscriber<Integer>(out) {
                    @Override
                    public void onCompleted() {
                        completedCount.incrementAndGet();
                        out.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        out.onError(e);
                    }

                    @Override
                    public void onNext(Integer t) {
                        out.onNext(t);
                    }
                };
            }
        });
        class EndSubscriber extends Subscriber<Integer> {
            @Override
            public void onCompleted() {
                completedCount.incrementAndGet();
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer t) {
            }

            public void request(int n) {
                super.request(n);
            }
        }
        final EndSubscriber endSubscriber = new EndSubscriber();
        //endSubscriber.pause();
        mid.subscribe(endSubscriber);
        assertEquals(0, completedCount.get());
        endSubscriber.request(2);
        assertEquals(2, completedCount.get());
    }
}
