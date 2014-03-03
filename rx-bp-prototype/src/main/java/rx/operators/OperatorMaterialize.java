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

import rx.Notification;
import rx.Observable.Operator;
import rx.Subscriber;

/**
 * Turns all of the notifications from an Observable into <code>onNext</code> emissions, and marks
 * them with their original notification types within {@link Notification} objects.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/materialize.png">
 * <p>
 * See <a href="http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx">here</a> for the
 * Microsoft Rx equivalent.
 */
public final class OperatorMaterialize<T> implements Operator<Notification<T>, T> {

    /**
     * Materializes the implicit notifications of an observable sequence as explicit notification values.
     * 
     * @param sequence
     *            An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of materializing the notifications of the given sequence.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx">Observable.Materialize(TSource) Method </a>
     */
    @Override
    public Subscriber<? super T> call(final Subscriber<? super Notification<T>> o) {
        return new Subscriber<T>(o) {
            @Override
            public void onCompleted() {
                o.onNext(Notification.<T>createOnCompleted());
                o.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                o.onNext(Notification.<T>createOnError(e));
                o.onCompleted();
            }

            @Override
            public void onNext(T t) {
                o.onNext(Notification.createOnNext(t));
            }
        };
    }
}
