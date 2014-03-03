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

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.observers.SafeSubscriber;
import rx.operators.OperationToIterator;

/**
 * An extension of {@link Observable} that provides blocking operators.
 * <p>
 * You construct a <code>BlockingObservable</code> from an
 * <code>Observable</code> with {@link #from(Observable)} or {@link Observable#toBlockingObservable()} <p>
 * The documentation for this interface makes use of a form of marble diagram
 * that has been modified to illustrate blocking operators. The following legend
 * explains these marble diagrams:
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.legend.png">
 * <p>
 * For more information see the
 * <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators">Blocking
 * Observable Operators</a> page at the RxJava Wiki.
 * 
 * @param <T>
 */
public class BlockingObservable<T> {

    private final Observable<? extends T> o;

    private BlockingObservable(Observable<? extends T> o) {
        this.o = o;
    }

    /**
     * Convert an Observable into a BlockingObservable.
     */
    public static <T> BlockingObservable<T> from(final Observable<? extends T> o) {
        return new BlockingObservable<T>(o);
    }

    /**
     * Used for protecting against errors being thrown from {@link Subscriber} implementations and ensuring onNext/onError/onCompleted contract
     * compliance.
     * <p>
     * See https://github.com/Netflix/RxJava/issues/216 for discussion on
     * "Guideline 6.4: Protect calls to user code from within an operator"
     */
    private Subscription protectivelyWrapAndSubscribe(Subscriber<? super T> observer) {
        return o.subscribe(new SafeSubscriber<T>(observer));
    }

    /**
     * Invoke a method on each item emitted by the {@link Observable}; block
     * until the Observable completes.
     * <p>
     * NOTE: This will block even if the Observable is asynchronous.
     * <p>
     * This is similar to {@link Observable#subscribe(Subscriber)}, but it blocks.
     * Because it blocks it does not need the {@link Subscriber#onCompleted()} or {@link Subscriber#onError(Throwable)} methods.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.forEach.png">
     * 
     * @param onNext
     *            the {@link Action1} to invoke for every item emitted by the {@link Observable}
     * @throws RuntimeException
     *             if an error occurs
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#foreach">RxJava Wiki: forEach()</a>
     */
    public void forEach(final Action1<? super T> onNext) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> exceptionFromOnError = new AtomicReference<Throwable>();

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on
         * "Guideline 6.4: Protect calls to user code from within an operator"
         */
        protectivelyWrapAndSubscribe(new Subscriber<T>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                /*
                 * If we receive an onError event we set the reference on the
                 * outer thread so we can git it and throw after the
                 * latch.await().
                 * 
                 * We do this instead of throwing directly since this may be on
                 * a different thread and the latch is still waiting.
                 */
                exceptionFromOnError.set(e);
                latch.countDown();
            }

            @Override
            public void onNext(T args) {
                onNext.call(args);
            }
        });
        // block until the subscription completes and then return
        try {
            latch.await();
        } catch (InterruptedException e) {
            // set the interrupted flag again so callers can still get it
            // for more information see https://github.com/Netflix/RxJava/pull/147#issuecomment-13624780
            Thread.currentThread().interrupt();
            // using Runtime so it is not checked
            throw new RuntimeException("Interrupted while waiting for subscription to complete.", e);
        }

        if (exceptionFromOnError.get() != null) {
            if (exceptionFromOnError.get() instanceof RuntimeException) {
                throw (RuntimeException) exceptionFromOnError.get();
            } else {
                throw new RuntimeException(exceptionFromOnError.get());
            }
        }
    }

    /**
     * Returns an {@link Iterator} that iterates over all items emitted by a
     * specified {@link Observable}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.getIterator.png">
     * 
     * @return an {@link Iterator} that can iterate over the items emitted by
     *         the {@link Observable}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#transformations-tofuture-toiterable-and-toiteratorgetiterator">RxJava Wiki: getIterator()</a>
     */
    public Iterator<T> getIterator() {
        return OperationToIterator.toIterator(o);
    }

    /**
     * Returns the first item emitted by a specified {@link Observable}, or
     * <code>IllegalArgumentException</code> if source contains no elements.
     * 
     * @return the first item emitted by the source {@link Observable}
     * @throws IllegalArgumentException
     *             if source contains no elements
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#first-and-firstordefault">RxJava Wiki: first()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229177.aspx">MSDN: Observable.First</a>
     */
    public T first() {
        return from(o.first()).single();
    }
    
    public T last() {
        return from(o.last()).single();
    }

    /**
     * Returns the first item emitted by a specified {@link Observable}, or a
     * default value if no items are emitted.
     * 
     * @param defaultValue
     *            a default value to return if the {@link Observable} emits no items
     * @return the first item emitted by the {@link Observable}, or the default
     *         value if no items are emitted
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#first-and-firstordefault">RxJava Wiki: firstOrDefault()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229320.aspx">MSDN: Observable.FirstOrDefault</a>
     */
    public T firstOrDefault(T defaultValue) {
        return from(o.take(1)).singleOrDefault(defaultValue);
    }

    /**
     * If the {@link Observable} completes after emitting a single item, return
     * that item, otherwise throw an <code>IllegalArgumentException</code>.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.single.png">
     * 
     * @return the single item emitted by the {@link Observable}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#single-and-singleordefault">RxJava Wiki: single()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.single.aspx">MSDN: Observable.Single</a>
     */
    public T single() {
        return from(o.single()).toIterable().iterator().next();
    }

    /**
     * If the {@link Observable} completes after emitting a single item, return
     * that item; if it emits more than one item, throw an
     * <code>IllegalArgumentException</code>; if it emits no items, return a
     * default value.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.singleOrDefault.png">
     * 
     * @param defaultValue
     *            a default value to return if the {@link Observable} emits no items
     * @return the single item emitted by the {@link Observable}, or the default
     *         value if no items are emitted
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#single-and-singleordefault">RxJava Wiki: singleOrDefault()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.singleordefault.aspx">MSDN: Observable.SingleOrDefault</a>
     */
    public T singleOrDefault(T defaultValue) {
        Iterator<? extends T> it = this.toIterable().iterator();

        if (!it.hasNext()) {
            return defaultValue;
        }

        T result = it.next();
        if (it.hasNext()) {
            throw new IllegalArgumentException("Sequence contains too many elements");
        }
        return result;
    }

    /**
     * Converts an {@link Observable} into an {@link Iterable}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.toIterable.png">
     * 
     * @return an {@link Iterable} version of the underlying {@link Observable}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#transformations-tofuture-toiterable-and-toiteratorgetiterator">RxJava Wiki: toIterable()</a>
     */
    public Iterable<T> toIterable() {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return getIterator();
            }
        };
    }
}
