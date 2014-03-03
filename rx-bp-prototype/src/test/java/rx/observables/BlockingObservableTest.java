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
package rx.observables;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action1;
import rx.subscriptions.BooleanSubscription;

public class BlockingObservableTest {

    @Mock
    Subscriber<Integer> w;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    public void testSingle() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.from("one"));
        assertEquals("one", observable.single());
    }

    @Test
    public void testSingleDefault() {
        BlockingObservable<Object> observable = BlockingObservable.from(Observable.empty());
        assertEquals("default", observable.singleOrDefault("default"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSingleDefaultWithMoreThanOne() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.from("one", "two", "three"));
        observable.singleOrDefault("default");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSingleWrong() {
        BlockingObservable<Integer> observable = BlockingObservable.from(Observable.from(1, 2));
        observable.single();
    }

    @Test
    public void testToIterable() {
        BlockingObservable<String> obs = BlockingObservable.from(Observable.from("one", "two", "three"));

        Iterator<String> it = obs.toIterable().iterator();

        assertEquals(true, it.hasNext());
        assertEquals("one", it.next());

        assertEquals(true, it.hasNext());
        assertEquals("two", it.next());

        assertEquals(true, it.hasNext());
        assertEquals("three", it.next());

        assertEquals(false, it.hasNext());

    }

    @Test(expected = NoSuchElementException.class)
    public void testToIterableNextOnly() {
        BlockingObservable<Integer> obs = BlockingObservable.from(Observable.from(1, 2, 3));

        Iterator<Integer> it = obs.toIterable().iterator();

        Assert.assertEquals((Integer) 1, it.next());
        Assert.assertEquals((Integer) 2, it.next());
        Assert.assertEquals((Integer) 3, it.next());

        it.next();
    }

    @Test(expected = NoSuchElementException.class)
    public void testToIterableNextOnlyTwice() {
        BlockingObservable<Integer> obs = BlockingObservable.from(Observable.from(1, 2, 3));

        Iterator<Integer> it = obs.toIterable().iterator();

        Assert.assertEquals((Integer) 1, it.next());
        Assert.assertEquals((Integer) 2, it.next());
        Assert.assertEquals((Integer) 3, it.next());

        boolean exc = false;
        try {
            it.next();
        } catch (NoSuchElementException ex) {
            exc = true;
        }
        Assert.assertEquals(true, exc);

        it.next();
    }

    @Test
    public void testToIterableManyTimes() {
        BlockingObservable<Integer> obs = BlockingObservable.from(Observable.from(1, 2, 3));

        Iterable<Integer> iter = obs.toIterable();

        for (int j = 0; j < 3; j++) {
            Iterator<Integer> it = iter.iterator();

            Assert.assertTrue(it.hasNext());
            Assert.assertEquals((Integer) 1, it.next());
            Assert.assertTrue(it.hasNext());
            Assert.assertEquals((Integer) 2, it.next());
            Assert.assertTrue(it.hasNext());
            Assert.assertEquals((Integer) 3, it.next());
            Assert.assertFalse(it.hasNext());
        }
    }

    @Test(expected = TestException.class)
    public void testToIterableWithException() {
        BlockingObservable<String> obs = BlockingObservable.from(Observable.create(new OnSubscribe<String>() {

            @Override
            public void call(final Subscriber<? super String> observer) {
                observer.onNext("one");
                observer.onError(new TestException());
            }
        }));

        Iterator<String> it = obs.toIterable().iterator();

        assertEquals(true, it.hasNext());
        assertEquals("one", it.next());

        assertEquals(true, it.hasNext());
        it.next();

    }

    @Test
    public void testForEachWithError() {
        try {
            BlockingObservable.from(Observable.create(new OnSubscribe<String>() {

                @Override
                public void call(final Subscriber<? super String> observer) {
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            observer.onNext("one");
                            observer.onNext("two");
                            observer.onNext("three");
                            observer.onCompleted();
                        }
                    }).start();
                }
            })).forEach(new Action1<String>() {

                @Override
                public void call(String t1) {
                    throw new RuntimeException("fail");
                }
            });
            fail("we expect an exception to be thrown");
        } catch (Throwable e) {
            // do nothing as we expect this
        }
    }

    @Test
    public void testFirst() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.from("one", "two", "three"));
        assertEquals("one", observable.first());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFirstWithEmpty() {
        BlockingObservable.from(Observable.<String> empty()).first();
    }

    @Test
    public void testFirstOrDefault() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.from("one", "two", "three"));
        assertEquals("one", observable.firstOrDefault("default"));
    }

    @Test
    public void testFirstOrDefaultWithEmpty() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.<String> empty());
        assertEquals("default", observable.firstOrDefault("default"));
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
