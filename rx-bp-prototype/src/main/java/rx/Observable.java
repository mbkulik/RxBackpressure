/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package rx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.functions.Func5;
import rx.functions.Func6;
import rx.functions.Func7;
import rx.functions.Func8;
import rx.functions.Func9;
import rx.functions.FuncN;
import rx.functions.Function;
import rx.observables.BlockingObservable;
import rx.observables.GroupedObservable;
import rx.observers.SafeSubscriber;
import rx.operators.OnSubscribeFromIterable;
import rx.operators.OperationInterval;
import rx.operators.OperatorSkip;
import rx.operators.OperationThrottleFirst;
import rx.operators.OperatorDematerialize;
import rx.operators.OperatorDoOnEach;
import rx.operators.OperatorFilter;
import rx.operators.OperatorGroupBy;
import rx.operators.OperatorLast;
import rx.operators.OperatorMap;
import rx.operators.OperatorMaterialize;
import rx.operators.OperatorMerge;
import rx.operators.OperatorObserveOn;
import rx.operators.OperatorScan;
import rx.operators.OperatorSingle;
import rx.operators.OperatorSubscribeOn;
import rx.operators.OperatorTake;
import rx.operators.OperatorToObservableList;
import rx.operators.OperatorZip;
import rx.subscriptions.Subscriptions;

/**
 * The Observable class that implements the Reactive Pattern.
 * <p>
 * This class provides methods for subscribing to the Observable as well as delegate methods to the various
 * Observers.
 * <p>
 * The documentation for this class makes use of marble diagrams. The following legend explains these diagrams:
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/legend.png">
 * <p>
 * For more information see the <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
 * 
 * @param <T>
 *            the type of the items emitted by the Observable
 */
public class Observable<T> {

    final OnSubscribe<T> f;

    /**
     * Observable with Function to execute when subscribed to.
     * <p>
     * <em>Note:</em> Use {@link #create(OnSubscribe)} to create an Observable, instead of this constructor,
     * unless you specifically have a need for inheritance.
     * 
     * @param f
     *            {@link OnSubscribe} to be executed when {@link #subscribe(Subscriber)} is called
     */
    protected Observable(OnSubscribe<T> f) {
        this.f = f;
    }

    /**
     * Returns an Observable that will execute the specified function when a {@link Subscriber} subscribes to
     * it.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/create.png">
     * <p>
     * Write the function you pass to {@code create} so that it behaves as an Observable: It should invoke the
     * Subscriber's {@link Subscriber#onNext onNext}, {@link Subscriber#onError onError}, and {@link Subscriber#onCompleted onCompleted} methods appropriately.
     * <p>
     * A well-formed Observable must invoke either the Subscriber's {@code onCompleted} method exactly once or
     * its {@code onError} method exactly once.
     * <p>
     * See <a href="http://go.microsoft.com/fwlink/?LinkID=205219">Rx Design Guidelines (PDF)</a> for detailed
     * information.
     * 
     * @param <T>
     *            the type of the items that this Observable emits
     * @param f
     *            a function that accepts an {@code Subscriber<T>}, and invokes its {@code onNext}, {@code onError}, and {@code onCompleted} methods as appropriate
     * @return an Observable that, when a {@link Subscriber} subscribes to it, will execute the specified
     *         function
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-create">RxJava Wiki: create()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.create.aspx">MSDN: Observable.Create</a>
     */
    public final static <T> Observable<T> create(final OnSubscribe<T> f) {
        return new Observable<T>(new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> o) {
                f.call(o);
            }

            //            @Override
            //            public void call(final Subscriber<? super T> o) {
            //                f.call(new Subscriber<T>(o) {
            //                    @Override
            //                    public void onCompleted() {
            //                        if (isUnsubscribed())
            //                            onError(new IllegalStateException("Yo this subscriber wanted you to unsubscribe"));
            //                        if (isPaused())
            //                            onError(new IllegalStateException("Yo this subscriber wanted you to pause"));
            //                        o.onCompleted();
            //                    }
            //
            //                    @Override
            //                    public void onError(Throwable e) {
            //                        o.onError(e);
            //                    }
            //
            //                    @Override
            //                    public void onNext(T t) {
            //                        if (isUnsubscribed())
            //                            onError(new IllegalStateException("Yo this subscriber wanted you to unsubscribe"));
            //                        if (isPaused())
            //                            onError(new IllegalStateException("Yo this subscriber wanted you to pause"));
            //                        o.onNext(t);
            //                    }
            //                });
            //            }
        });
    }

    /**
     * Invoked when Obserable.subscribe is called.
     */
    public static interface OnSubscribe<T> extends Action1<Subscriber<? super T>> {
        // cover for generics insanity
    }

    /**
     * Operator function for lifting into an Observable.
     */
    public interface Operator<R, T> extends Func1<Subscriber<? super R>, Subscriber<? super T>> {
        // cover for generics insanity
    }

    /**
     * @deprecated use {@link #create(OnSubscribe)}
     */
    @Deprecated
    public final static <T> Observable<T> create(final OnSubscribeFunc<T> f) {
        return new Observable<T>(new OnSubscribe<T>() {

            @Override
            public void call(Subscriber<? super T> observer) {
                Subscription s = f.onSubscribe(observer);
                if (s != null && s != observer) {
                    observer.add(s);
                }
            }

        });
    }

    /**
     * 
     */
    public static interface OnSubscribeFunc<T> extends Function {

        public Subscription onSubscribe(Observer<? super T> op);

    }

    /**
     * Lift a function to the current Observable and return a new Observable that when subscribed to will pass
     * the values of the current Observable through the function.
     * <p>
     * In other words, this allows chaining Observers together on an Observable for acting on the values within
     * the Observable.
     * <p> {@code
     * observable.map(...).filter(...).take(5).lift(new ObserverA()).lift(new ObserverB(...)).subscribe()
     * }
     * 
     * @param bind
     * @return an Observable that emits values that are the result of applying the bind function to the values
     *         of the current Observable
     */
    public <R> Observable<R> lift(final Operator<? extends R, ? super T> lift) {
        return new Observable<R>(new OnSubscribe<R>() {
            @Override
            public void call(Subscriber<? super R> o) {
                try {
                    final Subscriber<? super T> i = lift.call(o);
                    f.call(i);
                } catch (Throwable e) {
                    // localized capture of errors rather than it skipping all operators 
                    // and ending up in the try/catch of the subscribe method which then
                    // prevents onErrorResumeNext and other similar approaches to error handling
                    if (e instanceof OnErrorNotImplementedException) {
                        throw (OnErrorNotImplementedException) e;
                    }
                    o.onError(e);
                }
            }
        });
    }

    /* *********************************************************************************************************
     * Observers Below Here
     * *********************************************************************************************************
     */

    public final Observable<T> throttleFirst(long windowDuration, TimeUnit unit) {
        return create(OperationThrottleFirst.throttleFirst(this, windowDuration, unit));
    }

    //    public final static <T1, T2, R> Observable<R> combineLatest(Observable<? extends T1> o1, Observable<? extends T2> o2, Func2<? super T1, ? super T2, ? extends R> combineFunction) {
    //        return create(OperationCombineLatest.combineLatest(o1, o2, combineFunction));
    //    }

    public final static Observable<Long> interval(long interval, TimeUnit unit) {
        return create(OperationInterval.interval(interval, unit));
    }

    //
    //    public final static <T> Observable<T> concat(Observable<? extends T> t1, Observable<? extends T> t2) {
    //        return create(OperationConcat.concat(t1, t2));
    //    }

    public final Observable<T> filter(Func1<? super T, Boolean> predicate) {
        return lift(new OperatorFilter<T>(predicate));
    }

    public final Observable<T> skip(int num) {
        return lift(new OperatorSkip(num));
    }

    public final static <T> Observable<T> just(T value) {
        return from(Arrays.asList(value));
    }

    /**
     * Returns an Observable that emits no items to the {@link Observer} and immediately invokes its {@link Observer#onCompleted onCompleted} method.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/empty.png">
     * 
     * @param <T>
     *            the type of the items (ostensibly) emitted by the Observable
     * @return an Observable that emits no items to the {@link Observer} but immediately invokes the {@link Observer}'s {@link Observer#onCompleted() onCompleted} method
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-empty-error-and-never">RxJava Wiki: empty()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229670.aspx">MSDN: Observable.Empty</a>
     */
    public final static <T> Observable<T> empty() {
        return from(new ArrayList<T>());
    }

    /**
     * Returns an Observable that emits no items to the {@link Observer} and immediately invokes its {@link Observer#onCompleted onCompleted} method on the specified Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/empty.s.png">
     * 
     * @param scheduler
     *            the Scheduler to use to call the {@link Observer#onCompleted onCompleted} method
     * @param <T>
     *            the type of the items (ostensibly) emitted by the Observable
     * @return an Observable that emits no items to the {@link Observer} but immediately invokes the {@link Observer}'s {@link Observer#onCompleted() onCompleted} method with the specified
     *         {@code scheduler}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-empty-error-and-never">RxJava Wiki: empty()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229066.aspx">MSDN: Observable.Empty Method (IScheduler)</a>
     */
    public final static <T> Observable<T> empty(Scheduler scheduler) {
        return Observable.<T> empty().subscribeOn(scheduler);
    }

    public final Observable<T> doOnEach(final Action1<Notification<? super T>> onNotification) {
        Observer<T> observer = new Observer<T>() {
            @Override
            public final void onCompleted() {
                onNotification.call(Notification.createOnCompleted());
            }

            @Override
            public final void onError(Throwable e) {
                onNotification.call(Notification.createOnError(e));
            }

            @Override
            public final void onNext(T v) {
                onNotification.call(Notification.createOnNext(v));
            }

        };

        return lift(new OperatorDoOnEach<T>(observer));
    }

    /**
     * Returns an Observable that invokes an {@link Observer}'s {@link Observer#onError onError} method when the
     * Observer subscribes to it.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/error.png">
     * 
     * @param exception
     *            the particular Throwable to pass to {@link Observer#onError onError}
     * @param <T>
     *            the type of the items (ostensibly) emitted by the Observable
     * @return an Observable that invokes the {@link Observer}'s {@link Observer#onError onError} method when
     *         the Observer subscribes to it
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-empty-error-and-never">RxJava Wiki: error()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh244299.aspx">MSDN: Observable.Throw</a>
     */
    public final static <T> Observable<T> error(Throwable exception) {
        return new ThrowObservable<T>(exception);
    }

    /**
     * Returns an Observable that invokes an {@link Observer}'s {@link Observer#onError onError} method on the
     * specified Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/error.s.png">
     * 
     * @param exception
     *            the particular Throwable to pass to {@link Observer#onError onError}
     * @param scheduler
     *            the Scheduler on which to call {@link Observer#onError onError}
     * @param <T>
     *            the type of the items (ostensibly) emitted by the Observable
     * @return an Observable that invokes the {@link Observer}'s {@link Observer#onError onError} method, on
     *         the specified Scheduler
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-empty-error-and-never">RxJava Wiki: error()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211711.aspx">MSDN: Observable.Throw</a>
     */
    public final static <T> Observable<T> error(Throwable exception, Scheduler scheduler) {
        return Observable.<T> error(exception).subscribeOn(scheduler);
    }

    /**
     * Converts an {@link Iterable} sequence into an Observable that emits the items in the sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/from.png">
     * 
     * @param iterable
     *            the source {@link Iterable} sequence
     * @param <T>
     *            the type of items in the {@link Iterable} sequence and the type of items to be emitted by the
     *            resulting Observable
     * @return an Observable that emits each item in the source {@link Iterable} sequence
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-from">RxJava Wiki: from()</a>
     */
    public final static <T> Observable<T> from(Iterable<? extends T> iterable) {
        return create(new OnSubscribeFromIterable<T>(iterable));
    }

    /**
     * Converts an {@link Iterable} sequence into an Observable that operates on the specified Scheduler,
     * emitting each item from the sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/from.s.png">
     * 
     * @param iterable
     *            the source {@link Iterable} sequence
     * @param scheduler
     *            the Scheduler on which the Observable is to emit the items of the Iterable
     * @param <T>
     *            the type of items in the {@link Iterable} sequence and the type of items to be emitted by the
     *            resulting Observable
     * @return an Observable that emits each item in the source {@link Iterable} sequence, on the specified
     *         Scheduler
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-from">RxJava Wiki: from()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212140.aspx">MSDN: Observable.ToObservable</a>
     */
    public final static <T> Observable<T> from(Iterable<? extends T> iterable, Scheduler scheduler) {
        return create(new OnSubscribeFromIterable<T>(iterable)).subscribeOn(scheduler);
    }

    /**
     * Converts an item into an Observable that emits that item.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/from.png">
     * 
     * @param t1
     *            the item
     * @param <T>
     *            the type of the item
     * @return an Observable that emits the item
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-from">RxJava Wiki: from()</a>
     */
    // suppress unchecked because we are using varargs inside the method
    public final static <T> Observable<T> from(T t1) {
        return from(Arrays.asList(t1));
    }

    /**
     * Converts an Array into an Observable that emits the items in the Array.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/from.png">
     * 
     * @param items
     *            the source Array
     * @param <T>
     *            the type of items in the Array and the type of items to be emitted by the resulting Observable
     * @return an Observable that emits each item in the source Array
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-from">RxJava Wiki: from()</a>
     */
    //    @SafeVarargs // commenting out until we figure out if we can do Java7 compilation without breaking Android for just this feature
    public final static <T> Observable<T> from(T... t1) {
        return from(Arrays.asList(t1));
    }

    /**
     * Converts an Array into an Observable that emits the items in the Array on a specified Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/from.png">
     * 
     * @param items
     *            the source Array
     * @param scheduler
     *            the Scheduler on which the Observable emits the items of the Array
     * @param <T>
     *            the type of items in the Array and the type of items to be emitted by the resulting Observable
     * @return an Observable that emits each item in the source Array
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-from">RxJava Wiki: from()</a>
     */
    public final static <T> Observable<T> from(T[] items, Scheduler scheduler) {
        return from(Arrays.asList(items), scheduler);
    }

    /**
     * Flattens an Iterable of Observables into one Observable, without any transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * <p>
     * You can combine the items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * 
     * @param sequences
     *            the Iterable of Observables
     * @return an Observable that emits items that are the result of flattening the items emitted by the
     *         Observables in the Iterable
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-merge">RxJava Wiki: merge()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229590.aspx">MSDN: Observable.Merge</a>
     */
    public final static <T> Observable<T> merge(Iterable<? extends Observable<? extends T>> sequences) {
        return merge(from(sequences));
    }

    /**
     * Flattens an Iterable of Observables into one Observable, without any transformation, subscribing to these
     * Observables on a specified Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * <p>
     * You can combine the items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * 
     * @param sequences
     *            the Iterable of Observables
     * @param scheduler
     *            the Scheduler on which to traverse the Iterable of Observables
     * @return an Observable that emits items that are the result of flattening the items emitted by the
     *         Observables in the Iterable
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-merge">RxJava Wiki: merge()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh244336.aspx">MSDN: Observable.Merge</a>
     */
    public final static <T> Observable<T> merge(Iterable<? extends Observable<? extends T>> sequences, Scheduler scheduler) {
        return merge(from(sequences, scheduler));
    }

    /**
     * Flattens an Observable that emits Observables into a single Observable that emits the items emitted by
     * those Observables, without any transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.oo.png">
     * <p>
     * You can combine the items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * 
     * @param source
     *            an Observable that emits Observables
     * @return an Observable that emits items that are the result of flattening the Observables emitted by the {@code source} Observable
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-merge">RxJava Wiki: merge()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099.aspx">MSDN: Observable.Merge</a>
     */
    public final static <T> Observable<T> merge(Observable<? extends Observable<? extends T>> source) {
        return source.lift(new OperatorMerge<T>());
    }

    /**
     * Flattens two Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * 
     * @param t1
     *            an Observable to be merged
     * @param t2
     *            an Observable to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-merge">RxJava Wiki: merge()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099.aspx">MSDN: Observable.Merge</a>
     */
    public final static <T> Observable<T> merge(Observable<? extends T> t1, Observable<? extends T> t2) {
        return merge(from(Arrays.asList(t1, t2)));
    }

    /**
     * Flattens three Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * 
     * @param t1
     *            an Observable to be merged
     * @param t2
     *            an Observable to be merged
     * @param t3
     *            an Observable to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-merge">RxJava Wiki: merge()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099.aspx">MSDN: Observable.Merge</a>
     */
    public final static <T> Observable<T> merge(Observable<? extends T> t1, Observable<? extends T> t2, Observable<? extends T> t3) {
        return merge(from(Arrays.asList(t1, t2, t3)));
    }

    /**
     * Flattens four Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * 
     * @param t1
     *            an Observable to be merged
     * @param t2
     *            an Observable to be merged
     * @param t3
     *            an Observable to be merged
     * @param t4
     *            an Observable to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-merge">RxJava Wiki: merge()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099.aspx">MSDN: Observable.Merge</a>
     */
    public final static <T> Observable<T> merge(Observable<? extends T> t1, Observable<? extends T> t2, Observable<? extends T> t3, Observable<? extends T> t4) {
        return merge(from(Arrays.asList(t1, t2, t3, t4)));
    }

    /**
     * Flattens five Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * 
     * @param t1
     *            an Observable to be merged
     * @param t2
     *            an Observable to be merged
     * @param t3
     *            an Observable to be merged
     * @param t4
     *            an Observable to be merged
     * @param t5
     *            an Observable to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-merge">RxJava Wiki: merge()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099.aspx">MSDN: Observable.Merge</a>
     */
    public final static <T> Observable<T> merge(Observable<? extends T> t1, Observable<? extends T> t2, Observable<? extends T> t3, Observable<? extends T> t4, Observable<? extends T> t5) {
        return merge(from(Arrays.asList(t1, t2, t3, t4, t5)));
    }

    /**
     * Flattens six Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * 
     * @param t1
     *            an Observable to be merged
     * @param t2
     *            an Observable to be merged
     * @param t3
     *            an Observable to be merged
     * @param t4
     *            an Observable to be merged
     * @param t5
     *            an Observable to be merged
     * @param t6
     *            an Observable to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-merge">RxJava Wiki: merge()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099.aspx">MSDN: Observable.Merge</a>
     */
    public final static <T> Observable<T> merge(Observable<? extends T> t1, Observable<? extends T> t2, Observable<? extends T> t3, Observable<? extends T> t4, Observable<? extends T> t5, Observable<? extends T> t6) {
        return merge(from(Arrays.asList(t1, t2, t3, t4, t5, t6)));
    }

    /**
     * Flattens seven Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * 
     * @param t1
     *            an Observable to be merged
     * @param t2
     *            an Observable to be merged
     * @param t3
     *            an Observable to be merged
     * @param t4
     *            an Observable to be merged
     * @param t5
     *            an Observable to be merged
     * @param t6
     *            an Observable to be merged
     * @param t7
     *            an Observable to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-merge">RxJava Wiki: merge()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099.aspx">MSDN: Observable.Merge</a>
     */
    public final static <T> Observable<T> merge(Observable<? extends T> t1, Observable<? extends T> t2, Observable<? extends T> t3, Observable<? extends T> t4, Observable<? extends T> t5, Observable<? extends T> t6, Observable<? extends T> t7) {
        return merge(from(Arrays.asList(t1, t2, t3, t4, t5, t6, t7)));
    }

    /**
     * Flattens eight Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * 
     * @param t1
     *            an Observable to be merged
     * @param t2
     *            an Observable to be merged
     * @param t3
     *            an Observable to be merged
     * @param t4
     *            an Observable to be merged
     * @param t5
     *            an Observable to be merged
     * @param t6
     *            an Observable to be merged
     * @param t7
     *            an Observable to be merged
     * @param t8
     *            an Observable to be merged
     * @return an Observable that emits items that are the result of flattening
     *         the items emitted by the {@code source} Observables
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099.aspx">MSDN: Observable.Merge</a>
     */
    public final static <T> Observable<T> merge(Observable<? extends T> t1, Observable<? extends T> t2, Observable<? extends T> t3, Observable<? extends T> t4, Observable<? extends T> t5, Observable<? extends T> t6, Observable<? extends T> t7, Observable<? extends T> t8) {
        return merge(from(Arrays.asList(t1, t2, t3, t4, t5, t6, t7, t8)));
    }

    /**
     * Flattens nine Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * 
     * @param t1
     *            an Observable to be merged
     * @param t2
     *            an Observable to be merged
     * @param t3
     *            an Observable to be merged
     * @param t4
     *            an Observable to be merged
     * @param t5
     *            an Observable to be merged
     * @param t6
     *            an Observable to be merged
     * @param t7
     *            an Observable to be merged
     * @param t8
     *            an Observable to be merged
     * @param t9
     *            an Observable to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-merge">RxJava Wiki: merge()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099.aspx">MSDN: Observable.Merge</a>
     */
    // suppress because the types are checked by the method signature before using a vararg
    public final static <T> Observable<T> merge(Observable<? extends T> t1, Observable<? extends T> t2, Observable<? extends T> t3, Observable<? extends T> t4, Observable<? extends T> t5, Observable<? extends T> t6, Observable<? extends T> t7, Observable<? extends T> t8, Observable<? extends T> t9) {
        return merge(from(Arrays.asList(t1, t2, t3, t4, t5, t6, t7, t8, t9)));
    }

    /**
     * Flattens an Array of Observables into one Observable, without any transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.io.png">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * 
     * @param sequences
     *            the Array of Observables
     * @return an Observable that emits all of the items emitted by the Observables in the Array
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-merge">RxJava Wiki: merge()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099.aspx">MSDN: Observable.Merge</a>
     */
    public final static <T> Observable<T> merge(Observable<? extends T>[] sequences) {
        return merge(from(sequences));
    }

    /**
     * Flattens an Array of Observables into one Observable, without any transformation, traversing the array on
     * a specified Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.ios.png">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * 
     * @param sequences
     *            the Array of Observables
     * @param scheduler
     *            the Scheduler on which to traverse the Array
     * @return an Observable that emits all of the items emitted by the Observables in the Array
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-merge">RxJava Wiki: merge()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229061.aspx">MSDN: Observable.Merge</a>
     */
    public final static <T> Observable<T> merge(Observable<? extends T>[] sequences, Scheduler scheduler) {
        return merge(from(sequences, scheduler));
    }

    /**
     * Convert the current {@code Observable<T>} into an {@code Observable<Observable<T>>}.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/nest.png">
     * 
     * @return an Observable that emits a single item: the source Observable
     */
    public final Observable<Observable<T>> nest() {
        return from(this);
    }

    /**
     * Returns an Observable that never sends any items or notifications to an {@link Observer}.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/never.png">
     * <p>
     * This Observable is useful primarily for testing purposes.
     * 
     * @param <T>
     *            the type of items (not) emitted by the Observable
     * @return an Observable that never emits any items or sends any notifications to an {@link Observer}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-empty-error-and-never">RxJava Wiki: never()</a>
     */
    public final static <T> Observable<T> never() {
        return new NeverObservable<T>();
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to combinations of
     * items emitted, in sequence, by an Iterable of other Observables.
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by each of the source Observables;
     * the second item emitted by the new Observable will be the result of the function applied to the second
     * item emitted by each of those Observables; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@code onNext} as many times as
     * the number of {@code onNext} invokations of the source Observable that emits the fewest items.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * 
     * @param ws
     *            an Iterable of source Observables
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-zip">RxJava Wiki: zip()</a>
     */
    public final static <R> Observable<R> zip(Iterable<? extends Observable<?>> ws, FuncN<? extends R> zipFunction) {
        List<Observable<?>> os = new ArrayList<Observable<?>>();
        for (Observable<?> o : ws) {
            os.add(o);
        }
        return Observable.just(os.toArray(new Observable<?>[os.size()])).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to combinations of
     * <i>n</i> items emitted, in sequence, by the <i>n</i> Observables emitted by a specified Observable.
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by each of the Observables emitted
     * by the source Observable; the second item emitted by the new Observable will be the result of the
     * function applied to the second item emitted by each of those Observables; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@code onNext} as many times as
     * the number of {@code onNext} invokations of the source Observable that emits the fewest items.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.o.png">
     * 
     * @param ws
     *            an Observable of source Observables
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the Observables emitted by {@code ws}, results in an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-zip">RxJava Wiki: zip()</a>
     */
    public final static <R> Observable<R> zip(Observable<? extends Observable<?>> ws, final FuncN<? extends R> zipFunction) {
        return ws.toList().map(new Func1<List<? extends Observable<?>>, Observable<?>[]>() {

            @Override
            public Observable<?>[] call(List<? extends Observable<?>> o) {
                return o.toArray(new Observable<?>[o.size()]);
            }

        }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to combinations of
     * two items emitted, in sequence, by two other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by {@code o1} and the first item
     * emitted by {@code o2}; the second item emitted by the new Observable will be the result of the function
     * applied to the second item emitted by {@code o1} and the second item emitted by {@code o2}; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results
     *            in an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-zip">RxJava Wiki: zip()</a>
     */
    public final static <T1, T2, R> Observable<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, final Func2<? super T1, ? super T2, ? extends R> zipFunction) {
        return just(new Observable<?>[] { o1, o2 }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to combinations of
     * three items emitted, in sequence, by three other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by {@code o1}, the first item
     * emitted by {@code o2}, and the first item emitted by {@code o3}; the second item emitted by the new
     * Observable will be the result of the function applied to the second item emitted by {@code o1}, the
     * second item emitted by {@code o2}, and the second item emitted by {@code o3}; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-zip">RxJava Wiki: zip()</a>
     */
    public final static <T1, T2, T3, R> Observable<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Func3<? super T1, ? super T2, ? super T3, ? extends R> zipFunction) {
        return just(new Observable<?>[] { o1, o2, o3 }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to combinations of
     * four items emitted, in sequence, by four other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by {@code o1}, the first item
     * emitted by {@code o2}, the first item emitted by {@code o3}, and the first item emitted by {@code 04};
     * the second item emitted by the new Observable will be the result of the function applied to the second
     * item emitted by each of those Observables; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param o4
     *            a fourth source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-zip">RxJava Wiki: zip()</a>
     */
    public final static <T1, T2, T3, T4, R> Observable<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Func4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipFunction) {
        return just(new Observable<?>[] { o1, o2, o3, o4 }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to combinations of
     * five items emitted, in sequence, by five other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by {@code o1}, the first item
     * emitted by {@code o2}, the first item emitted by {@code o3}, the first item emitted by {@code o4}, and
     * the first item emitted by {@code o5}; the second item emitted by the new Observable will be the result of
     * the function applied to the second item emitted by each of those Observables; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param o4
     *            a fourth source Observable
     * @param o5
     *            a fifth source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-zip">RxJava Wiki: zip()</a>
     */
    public final static <T1, T2, T3, T4, T5, R> Observable<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, Func5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipFunction) {
        return just(new Observable<?>[] { o1, o2, o3, o4, o5 }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to combinations of
     * six items emitted, in sequence, by six other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by each source Observable, the
     * second item emitted by the new Observable will be the result of the function applied to the second item
     * emitted by each of those Observables, and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param o4
     *            a fourth source Observable
     * @param o5
     *            a fifth source Observable
     * @param o6
     *            a sixth source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-zip">RxJava Wiki: zip()</a>
     */
    public final static <T1, T2, T3, T4, T5, T6, R> Observable<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, Observable<? extends T6> o6,
            Func6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipFunction) {
        return just(new Observable<?>[] { o1, o2, o3, o4, o5, o6 }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to combinations of
     * seven items emitted, in sequence, by seven other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by each source Observable, the
     * second item emitted by the new Observable will be the result of the function applied to the second item
     * emitted by each of those Observables, and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param o4
     *            a fourth source Observable
     * @param o5
     *            a fifth source Observable
     * @param o6
     *            a sixth source Observable
     * @param o7
     *            a seventh source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-zip">RxJava Wiki: zip()</a>
     */
    public final static <T1, T2, T3, T4, T5, T6, T7, R> Observable<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, Observable<? extends T6> o6, Observable<? extends T7> o7,
            Func7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipFunction) {
        return just(new Observable<?>[] { o1, o2, o3, o4, o5, o6, o7 }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to combinations of
     * eight items emitted, in sequence, by eight other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by each source Observable, the
     * second item emitted by the new Observable will be the result of the function applied to the second item
     * emitted by each of those Observables, and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param o4
     *            a fourth source Observable
     * @param o5
     *            a fifth source Observable
     * @param o6
     *            a sixth source Observable
     * @param o7
     *            a seventh source Observable
     * @param o8
     *            an eighth source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-zip">RxJava Wiki: zip()</a>
     */
    public final static <T1, T2, T3, T4, T5, T6, T7, T8, R> Observable<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, Observable<? extends T6> o6, Observable<? extends T7> o7, Observable<? extends T8> o8,
            Func8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipFunction) {
        return just(new Observable<?>[] { o1, o2, o3, o4, o5, o6, o7, o8 }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to combinations of
     * nine items emitted, in sequence, by nine other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by each source Observable, the
     * second item emitted by the new Observable will be the result of the function applied to the second item
     * emitted by each of those Observables, and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param o4
     *            a fourth source Observable
     * @param o5
     *            a fifth source Observable
     * @param o6
     *            a sixth source Observable
     * @param o7
     *            a seventh source Observable
     * @param o8
     *            an eighth source Observable
     * @param o9
     *            a ninth source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-zip">RxJava Wiki: zip()</a>
     */
    public final static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Observable<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, Observable<? extends T6> o6, Observable<? extends T7> o7, Observable<? extends T8> o8,
            Observable<? extends T9> o9, Func9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipFunction) {
        return just(new Observable<?>[] { o1, o2, o3, o4, o5, o6, o7, o8, o9 }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Synonymous with {@code reduce()}.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/aggregate.png">
     * 
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Mathematical-and-Aggregate-Operators#wiki-reduce">RxJava Wiki: reduce()</a>
     * @see #reduce(Func2)
     * @deprecated use {@link #reduce(Func2)}
     */
    @Deprecated
    public final Observable<T> aggregate(Func2<T, T, T> accumulator) {
        return reduce(accumulator);
    }

    /**
     * Synonymous with {@code reduce()}.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/aggregateSeed.png">
     * 
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Mathematical-and-Aggregate-Operators#wiki-reduce">RxJava Wiki: reduce()</a>
     * @see #reduce(Object, Func2)
     * @deprecated use {@link #reduce(Object, Func2)}
     */
    @Deprecated
    public final <R> Observable<R> aggregate(R initialValue, Func2<R, ? super T, R> accumulator) {
        return reduce(initialValue, accumulator);
    }

    /**
     * Collect values into a single mutable data structure.
     * <p>
     * This is a simplified version of {@code reduce} that does not need to return the state on each pass.
     * <p>
     * 
     * @param state
     *            FIXME FIXME FIXME
     * @param collector
     *            FIXME FIXME FIXME
     * @return FIXME FIXME FIXME
     */
    public final <R> Observable<R> collect(R state, final Action2<R, ? super T> collector) {
        Func2<R, T, R> accumulator = new Func2<R, T, R>() {

            @Override
            public final R call(R state, T value) {
                collector.call(state, value);
                return state;
            }

        };
        return reduce(state, accumulator);
    }

    /**
     * Returns an Observable that emits the count of the total number of items emitted by the source Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/count.png">
     * 
     * @return an Observable that emits a single item: the number of elements emitted by the source Observable
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Mathematical-and-Aggregate-Operators#wiki-count-and-longcount">RxJava Wiki: count()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229470.aspx">MSDN: Observable.Count</a>
     * @see #longCount()
     */
    public final Observable<Integer> count() {
        return reduce(0, new Func2<Integer, T, Integer>() {
            @Override
            public final Integer call(Integer t1, T t2) {
                return t1 + 1;
            }
        });
    }

    /**
     * Returns an Observable that reverses the effect of {@link #materialize materialize} by transforming the {@link Notification} objects emitted by the source Observable into the items or
     * notifications they
     * represent.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/dematerialize.png">
     * 
     * @return an Observable that emits the items and notifications embedded in the {@link Notification} objects
     *         emitted by the source Observable
     * @throws Throwable
     *             if the source Observable is not of type {@code Observable<Notification<T>>}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable-Utility-Operators#wiki-dematerialize">RxJava Wiki: dematerialize()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229047.aspx">MSDN: Observable.dematerialize</a>
     */
    @SuppressWarnings("unchecked")
    public final <T2> Observable<T2> dematerialize() {
        return lift(new OperatorDematerialize());
    }

    /**
     * Returns an Observable that emits only the very first item emitted by the source Observable, or raises an {@code IllegalArgumentException} if the source Observable is empty.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/first.png">
     * 
     * @return an Observable that emits only the very first item emitted by the source Observable, or raises an {@code IllegalArgumentException} if the source Observable is empty
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Filtering-Observables#wiki-first">RxJava Wiki: first()</a>
     * @see MSDN: {@code Observable.firstAsync()}
     */
    public final Observable<T> first() {
        return take(1).single();
    }

    /**
     * Returns an Observable that emits items based on applying a function that you supply to each item emitted
     * by the source Observable, where that function returns an Observable, and then merging those resulting
     * Observables and emitting the results of this merger.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/flatMap.png">
     * 
     * @param func
     *            a function that, when applied to an item emitted by the source Observable, returns an
     *            Observable
     * @return an Observable that emits the result of applying the transformation function to each item emitted
     *         by the source Observable and merging the results of the Observables obtained from this
     *         transformation
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Transforming-Observables#wiki-mapmany-or-flatmap-and-mapmanydelayerror">RxJava Wiki: flatMap()</a>
     */
    public final <R> Observable<R> flatMap(Func1<? super T, ? extends Observable<? extends R>> func) {
        return mergeMap(func);
    }

    /**
     * Groups the items emitted by an Observable according to a specified criterion, and emits these grouped
     * items as {@link GroupedObservable}s, one {@code GroupedObservable} per group.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/groupBy.png">
     * 
     * @param keySelector
     *            a function that extracts the key for each item
     * @param <K>
     *            the key type
     * @return an Observable that emits {@link GroupedObservable}s, each of which corresponds to a unique key
     *         value and each of which emits those items from the source Observable that share that key value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Transforming-Observables#wiki-groupby-and-groupbyuntil">RxJava Wiki: groupBy</a>
     */
    public final <K> Observable<GroupedObservable<K, T>> groupBy(final Func1<? super T, ? extends K> keySelector) {
        return lift(new OperatorGroupBy<K, T>(keySelector));
    }

    /**
     * Returns an Observable that emits the last item emitted by the source Observable or notifies observers of
     * an {@code IllegalArgumentException} if the source Observable is empty.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/last.png">
     * 
     * @return an Observable that emits the last item from the source Observable or notifies observers of an
     *         error
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Filtering-Observable-Operators#wiki-last">RxJava Wiki: last()</a>
     * @see MSDN: {@code Observable.lastAsync()}
     */
    public final Observable<T> last() {
        return lift(OperatorLast.<T> getInstance());
    }

    /**
     * Returns an Observable that counts the total number of items emitted by the source Observable and emits
     * this count as a 64-bit Long.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/longCount.png">
     * 
     * @return an Observable that emits a single item: the number of items emitted by the source Observable as a
     *         64-bit Long item
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Mathematical-and-Aggregate-Operators#wiki-count-and-longcount">RxJava Wiki: count()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229120.aspx">MSDN: Observable.LongCount</a>
     * @see #count()
     */
    public final Observable<Long> longCount() {
        return reduce(0L, new Func2<Long, T, Long>() {
            @Override
            public final Long call(Long t1, T t2) {
                return t1 + 1;
            }
        });
    }

    /**
     * Returns an Observable that applies a specified function to each item emitted by the source Observable and
     * emits the results of these function applications.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/map.png">
     * 
     * @param func
     *            a function to apply to each item emitted by the Observable
     * @return an Observable that emits the items from the source Observable, transformed by the specified
     *         function
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Transforming-Observables#wiki-map">RxJava Wiki: map()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh244306.aspx">MSDN: Observable.Select</a>
     */
    public final <R> Observable<R> map(Func1<? super T, ? extends R> func) {
        return lift(new OperatorMap<T, R>(func));
    }

    /**
     * Returns a new Observable by applying a function that you supply to each item emitted by the source
     * Observable, where that function returns an Observable, and then merging those resulting Observables and
     * emitting the results of this merger.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mapMany.png">
     * <p>
     * <em>Note:</em> {@code mapMany} and {@code flatMap} are equivalent.
     * 
     * @param func
     *            a function that, when applied to an item emitted by the source Observable, returns an
     *            Observable
     * @return an Observable that emits the result of applying the transformation function to each item emitted
     *         by the source Observable and merging the results of the Observables obtained from these
     *         transformations
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Transforming-Observables#wiki-mapmany-or-flatmap-and-mapmanydelayerror">RxJava Wiki: mapMany()</a>
     * @see #flatMap(Func1)
     * @deprecated use {@link #flatMap(Func1)}
     */
    @Deprecated
    public final <R> Observable<R> mapMany(Func1<? super T, ? extends Observable<R>> func) {
        return mergeMap(func);
    }

    /**
     * Turns all of the emissions and notifications from a source Observable into emissions marked with their
     * original types within {@link Notification} objects.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/materialize.png">
     * 
     * @return an Observable that emits items that are the result of materializing the items and notifications
     *         of the source Observable
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable-Utility-Operators#wiki-materialize">RxJava Wiki: materialize()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229453.aspx">MSDN: Observable.materialize</a>
     */
    public final Observable<Notification<T>> materialize() {
        return lift(new OperatorMaterialize<T>());
    }

    /**
     * Move notifications to the specified {@link Scheduler} asynchronously with an unbounded buffer.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/observeOn.png">
     * 
     * @param scheduler
     *            the {@link Scheduler} to notify {@link Observer}s on
     * @return the source Observable modified so that its {@link Observer}s are notified on the specified {@link Scheduler}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable-Utility-Operators#wiki-observeon">RxJava Wiki: observeOn()</a>
     */
    public final Observable<T> observeOn(Scheduler scheduler) {
        return lift(new OperatorObserveOn<T>(scheduler));
    }

    /**
     * Returns an Observable that emits the results of applying a specified function to each item emitted by the
     * source Observable, where that function returns an Observable, and then merging those resulting
     * Observables and emitting the results of this merger.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mergeMap.png">
     * 
     * @param func
     *            a function that, when applied to an item emitted by the source Observable, returns an
     *            Observable
     * @return an Observable that emits the result of applying the transformation function to each item emitted
     *         by the source Observable and merging the results of the Observables obtained from these
     *         transformations
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Transforming-Observables#wiki-mapmany-or-flatmap-and-mapmanydelayerror">RxJava Wiki: flatMap()</a>
     * @see #flatMap(Func1)
     */
    public final <R> Observable<R> mergeMap(Func1<? super T, ? extends Observable<? extends R>> func) {
        return merge(map(func));
    }

    /**
     * Protects against errors being thrown from Observer implementations and ensures
     * onNext/onError/onCompleted contract compliance.
     * <p>
     * See https://github.com/Netflix/RxJava/issues/216 for a discussion on "Guideline 6.4: Protect calls to
     * user code from within an Observer"
     */
    private Subscription protectivelyWrapAndSubscribe(Subscriber<? super T> o) {
        return subscribe(new SafeSubscriber<T>(o));
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a source
     * Observable, then feeds the result of that function along with the second item emitted by the source
     * Observable into the same function, and so on until all items have been emitted by the source Observable,
     * and emits the final result from the final call to your function as its sole item.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduce.png">
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "aggregate," "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an {@code inject()} method that does a similar operation on lists.
     * 
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source Observable, whose
     *            result will be used in the next accumulator call
     * @return an Observable that emits a single item that is the result of accumulating the items emitted by
     *         the source Observable
     * @throws IllegalArgumentException
     *             if the source Observable emits no items
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Mathematical-and-Aggregate-Operators#wiki-reduce">RxJava Wiki: reduce()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154.aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public final Observable<T> reduce(Func2<T, T, T> accumulator) {
        /*
         * Discussion and confirmation of implementation at
         * https://github.com/Netflix/RxJava/issues/423#issuecomment-27642532
         * 
         * It should use last() not takeLast(1) since it needs to emit an error if the sequence is empty.
         */
        return scan(accumulator).last();
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a source
     * Observable and a specified seed value, then feeds the result of that function along with the second item
     * emitted by an Observable into the same function, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your function as its sole item.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduceSeed.png">
     * <p>
     * This technique, which is called "reduce" here, is sometimec called "aggregate," "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an {@code inject()} method that does a similar operation on lists.
     * 
     * @param initialValue
     *            the initial (seed) accumulator value
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source Observable, the
     *            result of which will be used in the next accumulator call
     * @return an Observable that emits a single item that is the result of accumulating the output from the
     *         items emitted by the source Observable
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Mathematical-and-Aggregate-Operators#wiki-reduce">RxJava Wiki: reduce()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154.aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public final <R> Observable<R> reduce(R initialValue, Func2<R, ? super T, R> accumulator) {
        return scan(initialValue, accumulator).last();
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a source
     * Observable, then feeds the result of that function along with the second item emitted by the source
     * Observable into the same function, and so on until all items have been emitted by the source Observable,
     * emitting the result of each of these iterations.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scan.png">
     * <p>
     * This sort of function is sometimes called an accumulator.
     * 
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source Observable, whose
     *            result will be emitted to {@link Observer}s via {@link Observer#onNext onNext} and used in the
     *            next accumulator call
     * @return an Observable that emits the results of each call to the accumulator function
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Transforming-Observables#wiki-scan">RxJava Wiki: scan()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665.aspx">MSDN: Observable.Scan</a>
     */
    public final Observable<T> scan(Func2<T, T, T> accumulator) {
        return lift(new OperatorScan<T, T>(accumulator));
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a source
     * Observable and a seed value, then feeds the result of that function along with the second item emitted by
     * the source Observable into the same function, and so on until all items have been emitted by the source
     * Observable, emitting the result of each of these iterations.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scanSeed.png">
     * <p>
     * This sort of function is sometimes called an accumulator.
     * <p>
     * Note that the Observable that results from this method will emit {@code initialValue} as its first
     * emitted item.
     * 
     * @param initialValue
     *            the initial (seed) accumulator item
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source Observable, whose
     *            result will be emitted to {@link Observer}s via {@link Observer#onNext onNext} and used in the
     *            next accumulator call
     * @return an Observable that emits {@code initialValue} followed by the results of each call to the
     *         accumulator function
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Transforming-Observables#wiki-scan">RxJava Wiki: scan()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665.aspx">MSDN: Observable.Scan</a>
     */
    public final <R> Observable<R> scan(R initialValue, Func2<R, ? super T, R> accumulator) {
        return lift(new OperatorScan<R, T>(initialValue, accumulator));
    }

    /**
     * If the source Observable completes after emitting a single item, return an Observable that emits that
     * item. If the source Observable emits more than one item or no items, throw an {@code IllegalArgumentException}.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/single.png">
     * 
     * @return an Observable that emits the single item emitted by the source Observable
     * @throws IllegalArgumentException
     *             if the source emits more than one item or no items
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable-Utility-Operators#wiki-single-and-singleordefault">RxJava Wiki: single()</a>
     * @see MSDN: {@code Observable.singleAsync()}
     */
    public final Observable<T> single() {
        return lift(new OperatorSingle<T>());
    }

    /**
     * If the source Observable completes after emitting a single item, return an Observable that emits that
     * item; if the source Observable is empty, return an Observable that emits a default item. If the source
     * Observable emits more than one item, throw an {@code IllegalArgumentException.} <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/singleOrDefault.png">
     * 
     * @param defaultValue
     *            a default value to emit if the source Observable emits no item
     * @return an Observable that emits the single item emitted by the source Observable, or a default item if
     *         the source Observable is empty
     * @throws IllegalArgumentException
     *             if the source Observable emits more than one item
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable-Utility-Operators#wiki-single-and-singleordefault">RxJava Wiki: single()</a>
     * @see MSDN: {@code Observable.singleOrDefaultAsync()}
     */
    public final Observable<T> singleOrDefault(T defaultValue) {
        return lift(new OperatorSingle<T>(defaultValue));
    }

    /**
     * Subscribe and ignore all events.
     * 
     * @return a {@link Subscription} reference with which the {@link Observer} can stop receiving items before
     *         the Observable has finished sending them
     * @throws OnErrorNotImplementedException
     *             if the Observable tries to call {@code onError}
     */
    public final Subscription subscribe() {
        return protectivelyWrapAndSubscribe(new Subscriber<T>() {

            @Override
            public final void onCompleted() {
                // do nothing
            }

            @Override
            public final void onError(Throwable e) {
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public final void onNext(T args) {
                // do nothing
            }

        });
    }

    /**
     * An {@link Observer} must call an Observable's {@code subscribe} method in order to receive
     * items and notifications from the Observable.
     * 
     * @param onNext
     *            FIXME FIXME FIXME
     * @return a {@link Subscription} reference with which the {@link Observer} can stop receiving items before
     *         the Observable has finished sending them
     * @throws IllegalArgumentException
     *             if {@code onNext} is null
     * @throws OnErrorNotImplementedException
     *             if the Observable tries to call {@code onError}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable#wiki-onnext-oncompleted-and-onerror">RxJava Wiki: onNext, onCompleted, and onError</a>
     */
    public final Subscription subscribe(final Action1<? super T> onNext) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to
         * user code from within an Observer"
         */
        return protectivelyWrapAndSubscribe(new Subscriber<T>() {

            @Override
            public final void onCompleted() {
                // do nothing
            }

            @Override
            public final void onError(Throwable e) {
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public final void onNext(T args) {
                onNext.call(args);
            }

        });
    }

    /**
     * An {@link Observer} must call an Observable's {@code subscribe} method in order to receive items and
     * notifications from the Observable.
     * 
     * @param onNext
     *            FIXME FIXME FIXME
     * @param onError
     *            FIXME FIXME FIXME
     * @return a {@link Subscription} reference with which the {@link Observer} can stop receiving items before
     *         the Observable has finished sending them
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable#wiki-onnext-oncompleted-and-onerror">RxJava Wiki: onNext, onCompleted, and onError</a>
     * @throws IllegalArgumentException
     *             if {@code onNext} is null
     * @throws IllegalArgumentException
     *             if {@code onError} is null
     */
    public final Subscription subscribe(final Action1<? super T> onNext, final Action1<Throwable> onError) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on
         * "Guideline 6.4: Protect calls to user code from within an Observer"
         */
        return protectivelyWrapAndSubscribe(new Subscriber<T>() {

            @Override
            public final void onCompleted() {
                // do nothing
            }

            @Override
            public final void onError(Throwable e) {
                onError.call(e);
            }

            @Override
            public final void onNext(T args) {
                onNext.call(args);
            }

        });
    }

    /**
     * An {@link Observer} must call an Observable's {@code subscribe} method in order to receive items and
     * notifications from the Observable.
     * 
     * @param onNext
     *            FIXME FIXME FIXME
     * @param onError
     *            FIXME FIXME FIXME
     * @param onComplete
     *            FIXME FIXME FIXME
     * @return a {@link Subscription} reference with which the {@link Observer} can stop receiving items before
     *         the Observable has finished sending them
     * @throws IllegalArgumentException
     *             if {@code onNext} is null
     * @throws IllegalArgumentException
     *             if {@code onError} is null
     * @throws IllegalArgumentException
     *             if {@code onComplete} is null
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable#wiki-onnext-oncompleted-and-onerror">RxJava Wiki: onNext, onCompleted, and onError</a>
     */
    public final Subscription subscribe(final Action1<? super T> onNext, final Action1<Throwable> onError, final Action0 onComplete) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }
        if (onComplete == null) {
            throw new IllegalArgumentException("onComplete can not be null");
        }

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an Observer"
         */
        return protectivelyWrapAndSubscribe(new Subscriber<T>() {

            @Override
            public final void onCompleted() {
                onComplete.call();
            }

            @Override
            public final void onError(Throwable e) {
                onError.call(e);
            }

            @Override
            public final void onNext(T args) {
                onNext.call(args);
            }

        });
    }

    /**
     * An {@link Observer} must call an Observable's {@code subscribe} method in order to receive items and
     * notifications from the Observable.
     * 
     * @param onNext
     *            FIXME FIXME FIXME
     * @param onError
     *            FIXME FIXME FIXME
     * @param onComplete
     *            FIXME FIXME FIXME
     * @param scheduler
     *            FIXME FIXME FIXME
     * @return a {@link Subscription} reference with which the {@link Observer} can stop receiving items before
     *         the Observable has finished sending them
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable#wiki-onnext-oncompleted-and-onerror">RxJava Wiki: onNext, onCompleted, and onError</a>
     */
    public final Subscription subscribe(final Action1<? super T> onNext, final Action1<Throwable> onError, final Action0 onComplete, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(onNext, onError, onComplete);
    }

    /**
     * An {@link Observer} must call an Observable's {@code subscribe} method in order to receive items and
     * notifications from the Observable.
     * 
     * @param onNext
     *            FIXME FIXME FIXME
     * @param onError
     *            FIXME FIXME FIXME
     * @param scheduler
     *            FIXME FIXME FIXME
     * @return a {@link Subscription} reference with which the {@link Observer} can stop receiving items before
     *         the Observable has finished sending them
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable#wiki-onnext-oncompleted-and-onerror">RxJava Wiki: onNext, onCompleted, and onError</a>
     */
    public final Subscription subscribe(final Action1<? super T> onNext, final Action1<Throwable> onError, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(onNext, onError);
    }

    /**
     * An {@link Observer} must call an Observable's {@code subscribe} method in order to receive items and
     * notifications from the Observable.
     * 
     * @param onNext
     *            FIXME FIXME FIXME
     * @param scheduler
     *            FIXME FIXME FIXME
     * @return a {@link Subscription} reference with which the {@link Observer} can stop receiving items before
     *         the Observable has finished sending them
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable#wiki-onnext-oncompleted-and-onerror">RxJava Wiki: onNext, onCompleted, and onError</a>
     */
    public final Subscription subscribe(final Action1<? super T> onNext, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(onNext);
    }

    /**
     * An {@link Observer} must subscribe to an Observable in order to receive items and notifications from the
     * Observable.
     *
     * @param observer
     *            FIXME FIXME FIXME
     * @param scheduler
     *            FIXME FIXME FIXME
     * @return a {@link Subscription} reference with which the {@link Observer} can stop receiving items before
     *         the Observable has finished sending them
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable#wiki-onnext-oncompleted-and-onerror">RxJava Wiki: onNext, onCompleted, and onError</a>
     */
    public final Subscription subscribe(final Observer<? super T> observer, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(observer);
    }

    /**
     * An {@link Observer} must subscribe to an Observable in order to receive items and notifications from the
     * Observable.
     *
     * @param observer
     *            FIXME FIXME FIXME
     * @return a {@link Subscription} reference with which the {@link Observer} can stop receiving items before
     *         the Observable has finished sending them
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable#wiki-onnext-oncompleted-and-onerror">RxJava Wiki: onNext, onCompleted, and onError</a>
     */
    public final Subscription subscribe(final Observer<? super T> observer) {
        return subscribe(new Subscriber<T>() {

            @Override
            public void onCompleted() {
                observer.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onNext(T t) {
                observer.onNext(t);
            }

        });
    }

    /**
     * A {@link Subscriber} must call an Observable's {@code subscribe} method in order to receive items and
     * notifications from the Observable.
     * <p>
     * A typical implementation of {@code subscribe} does the following:
     * <ol>
     * <li>It stores a reference to the Subscriber in a collection object, such as a {@code List<T>} object.</li>
     * <li>It returns a reference to the {@link Subscription} interface. This enables Observers to unsubscribe,
     * that is, to stop receiving items and notifications before the Observable stops sending them, which also
     * invokes the Observer's {@link Observer#onCompleted onCompleted} method.</li>
     * </ol><p>
     * An {@code Observable<T>} instance is responsible for accepting all subscriptions and notifying all
     * Subscribers. Unless the documentation for a particular {@code Observable<T>} implementation indicates
     * otherwise, Subscriber should make no assumptions about the order in which multiple Subscribers will
     * receive their notifications.
     * <p>
     * For more information see the
     * <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
     * 
     * @param observer
     *            the {@link Subscriber}
     * @return a {@link Subscription} reference with which Subscribers that are {@link Observer}s can
     *         unsubscribe from the Observable
     * @throws IllegalStateException
     *             if {@code subscribe()} is unable to obtain an {@code OnSubscribe<>} function
     * @throws IllegalArgumentException
     *             if the {@link Subscriber} provided as the argument to {@code subscribe()} is {@code null}
     * @throws OnErrorNotImplementedException
     *             if the {@link Subscriber}'s {@code onError} method is null
     * @throws RuntimeException
     *             if the {@link Subscriber}'s {@code onError} method itself threw a {@code Throwable}
     */
    public final Subscription subscribe(Subscriber<? super T> observer) {
        // allow the hook to intercept and/or decorate
        OnSubscribe<T> onSubscribeFunction = f;
        // validate and proceed
        if (observer == null) {
            throw new IllegalArgumentException("observer can not be null");
        }
        if (onSubscribeFunction == null) {
            throw new IllegalStateException("onSubscribe function can not be null.");
            /*
             * the subscribe function can also be overridden but generally that's not the appropriate approach
             * so I won't mention that in the exception
             */
        }
        try {
            /*
             * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls
             * to user code from within an Observer"
             */
            if (isInternalImplementation(observer)) {
                onSubscribeFunction.call(observer);
            } else {
                // assign to `observer` so we return the protected version
                observer = new SafeSubscriber<T>(observer);
                onSubscribeFunction.call(observer);
            }
            final Subscription returnSubscription = observer;
            // we return it inside a Subscription so it can't be cast back to Subscriber
            return Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    returnSubscription.unsubscribe();
                }

            });
        } catch (Throwable e) {
            // special handling for certain Throwable/Error/Exception types
            Exceptions.throwIfFatal(e);
            // if an unhandled error occurs executing the onSubscribe we will propagate it
            try {
                observer.onError(e);
            } catch (OnErrorNotImplementedException e2) {
                // special handling when onError is not implemented ... we just rethrow
                throw e2;
            } catch (Throwable e2) {
                // if this happens it means the onError itself failed (perhaps an invalid function implementation)
                // so we are unable to propagate the error correctly and will just throw
                RuntimeException r = new RuntimeException("Error occurred attempting to subscribe [" + e.getMessage() + "] and then again while trying to pass to onError.", e2);
                throw r;
            }
            return Subscriptions.empty();
        }
    }

    /**
     * A {@link Subscriber} must call an Observable's {@code subscribe} method in order to receive items and
     * notifications from the Observable.
     * <p>
     * A typical implementation of {@code subscribe} does the following:
     * <ol>
     * <li>It stores a reference to the Subscriber in a collection object, such as a {@code List<T>} object.</li>
     * <li>It returns a reference to the {@link Subscription} interface. This enables Observers to unsubscribe,
     * that is, to stop receiving items and notifications before the Observable stops sending them, which also
     * invokes the Observer's {@link Observer#onCompleted onCompleted} method.</li>
     * </ol><p>
     * An {@code Observable<T>} instance is responsible for accepting all subscriptions and notifying all
     * Subscribers. Unless the documentation for a particular {@code Observable<T>} implementation indicates
     * otherwise, Subscribers should make no assumptions about the order in which multiple Subscribers will
     * receive their notifications.
     * <p>
     * For more information see the
     * <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
     * 
     * @param observer
     *            the {@link Subscriber}
     * @param scheduler
     *            the {@link Scheduler} on which Subscribers subscribe to the Observable
     * @return a {@link Subscription} reference with which Subscribers that are {@link Observer}s can
     *         unsubscribe from the Observable
     * @throws IllegalArgumentException
     *             if an argument to {@code subscribe()} is {@code null}
     */
    public final Subscription subscribe(Subscriber<? super T> observer, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(observer);
    }

    /**
     * Asynchronously subscribes Observers to this Observable on the specified {@link Scheduler}.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/subscribeOn.png">
     * 
     * @param scheduler
     *            the {@link Scheduler} to perform subscription actions on
     * @return the source Observable modified so that its subscriptions happen on the
     *         specified {@link Scheduler}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable-Utility-Operators#wiki-subscribeon">RxJava Wiki: subscribeOn()</a>
     * @see #subscribeOn(rx.Scheduler, int)
     */
    public final Observable<T> subscribeOn(Scheduler scheduler) {
        return nest().lift(new OperatorSubscribeOn<T>(scheduler));
    }

    /**
     * Returns an Observable that emits only the first {@code num} items emitted by the source Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/take.png">
     * <p>
     * This method returns an Observable that will invoke a subscribing {@link Observer}'s {@link Observer#onNext onNext} function a maximum of {@code num} times before invoking
     * {@link Observer#onCompleted onCompleted}.
     * 
     * @param num
     *            the maximum number of items to emit
     * @return an Observable that emits only the first {@code num} items emitted by the source Observable, or
     *         all of the items from the source Observable if that Observable emits fewer than {@code num} items
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Filtering-Observables#wiki-take">RxJava Wiki: take()</a>
     */
    public final Observable<T> take(final int num) {
        return lift(new OperatorTake<T>(num));
    }

    /**
     * Converts an Observable into a {@link BlockingObservable} (an Observable with blocking operators).
     * 
     * @return a {@code BlockingObservable} version of this Observable
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators">RxJava Wiki: Blocking Observable Observers</a>
     */
    public final BlockingObservable<T> toBlockingObservable() {
        return BlockingObservable.from(this);
    }

    /**
     * Returns an Observable that emits a single item, a list composed of all the items emitted by the source
     * Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toList.png">
     * <p>
     * Normally, an Observable that returns multiple items will do so by invoking its {@link Observer}'s {@link Observer#onNext onNext} method for each such item. You can change this behavior,
     * instructing the
     * Observable to compose a list of all of these items and then to invoke the Observer's {@code onNext} function once, passing it the entire list, by calling the Observable's {@code toList} method
     * prior to
     * calling its {@link #subscribe} method.
     * <p>
     *
     * <!-- IS THE FOLLOWING NOTE STILL VALID? -->
     *
     * Be careful not to use this operator on Observables that emit infinite or very large numbers of items, as
     * you do not have the option to unsubscribe.
     * 
     * @return an Observable that emits a single item: a List containing all of the items emitted by the source
     *         Observable
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Mathematical-and-Aggregate-Operators#wiki-tolist">RxJava Wiki: toList()</a>
     */
    public final Observable<List<T>> toList() {
        return lift(new OperatorToObservableList<T>());
    }

    /**
     * Returns an Observable that emits items that are the result of applying a specified function to pairs of
     * values, one each from the source Observable and another specified Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * 
     * @param <T2>
     *            the type of items emitted by the {@code other} Observable
     * @param <R>
     *            the type of items emitted by the resulting Observable
     * @param other
     *            the other Observable
     * @param zipFunction
     *            a function that combines the pairs of items from the two Observables to generate the items to
     *            be emitted by the resulting Observable
     * @return an Observable that pairs up values from the source Observable and the {@code other} Observable
     *         and emits the results of {@code zipFunction} applied to these pairs
     */
    public final <T2, R> Observable<R> zip(Observable<? extends T2> other, Func2<? super T, ? super T2, ? extends R> zipFunction) {
        return zip(this, other, zipFunction);
    }

    /**
     * An Observable that never sends any information to an {@link Observer}.
     * 
     * This Observable is useful primarily for testing purposes.
     * 
     * @param <T>
     *            the type of item (not) emitted by the Observable
     */
    private static class NeverObservable<T> extends Observable<T> {
        public NeverObservable() {
            super(new OnSubscribe<T>() {

                @Override
                public void call(Subscriber<? super T> observer) {
                    // do nothing
                }

            });
        }
    }

    /**
     * An Observable that invokes {@link Observer#onError onError} when the {@link Observer} subscribes to it.
     * 
     * @param <T>
     *            the type of item (ostensibly) emitted by the Observable
     */
    private static class ThrowObservable<T> extends Observable<T> {

        public ThrowObservable(final Throwable exception) {
            super(new OnSubscribe<T>() {

                /**
                 * Accepts an {@link Observer} and calls its {@link Observer#onError onError} method.
                 * 
                 * @param observer
                 *            an {@link Observer} of this Observable
                 * @return a reference to the subscription
                 */
                @Override
                public void call(Subscriber<? super T> observer) {
                    observer.onError(exception);
                }

            });
        }
    }

    @SuppressWarnings("rawtypes")
    private final static ConcurrentHashMap<Class, Boolean> internalClassMap = new ConcurrentHashMap<Class, Boolean>();

    /**
     * Whether a given {@link Function} is an internal implementation inside rx.* packages or not.
     * <p>
     * For why this is being used see https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline
     * 6.4: Protect calls to user code from within an Observer"
     * <p>
     * Note: If strong reasons for not depending on package names comes up then the implementation of this
     * method can change to looking for a marker interface.
     * 
     * @return {@code true} if the given function is an internal implementation, and {@code false} otherwise
     */
    private boolean isInternalImplementation(Object o) {
        if (o == null) {
            return true;
        }
        // prevent double-wrapping (yeah it happens)
        if (o instanceof SafeSubscriber) {
            return true;
        }

        Class<?> clazz = o.getClass();
        if (internalClassMap.containsKey(clazz)) {
            //don't need to do reflection
            return internalClassMap.get(clazz);
        } else {
            // we treat the following package as "internal" and don't wrap it
            Package p = o.getClass().getPackage(); // it can be null
            Boolean isInternal = (p != null && p.getName().startsWith("rx."));
            internalClassMap.put(clazz, isInternal);
            return isInternal;
        }
    }
}
