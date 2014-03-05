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

import java.util.Iterator;

import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Converts an Iterable sequence into an Observable.
 * <p>
 * <img width="640"
 * src="https://github.com/Netflix/RxJava/wiki/images/rx-Observers/toObservable.png">
 * <p>
 * You can convert any object that supports the Iterable interface into an Observable that emits
 * each item in the object, with the toObservable operation.
 */
public final class OnSubscribeFromIterable<T> implements OnSubscribe<T> {

    final Iterable<? extends T> is;

    public OnSubscribeFromIterable(Iterable<? extends T> iterable) {
        this.is = iterable;
    }

    @Override
    public void call(final Subscriber<? super T> o) {
        final Iterator<? extends T> iter = is.iterator();
        Action1<Integer> func = new Action1<Integer>() {
            @Override
            public void call(Integer n) {
                int count = 0;

                if (checkInvarient(o, n, count))
                    return;

                while (iter.hasNext()) {
                    final T value = iter.next();
                    o.onNext(value);
                    count++;

                    if (checkInvarient(o, n, count))
                        return;
                }
                o.onCompleted();
            }

            private boolean checkInvarient(final Subscriber<? super T> o, Integer n, int count) {
                if (o.isUnsubscribed()) {
                    return true;
                }
                if (o.isPaused() || (count >= n && count != -1)) {
                    o.setProducer(this);
                    return true;
                }
                return false;
            }
        };

        o.setProducer(func);
    }
}
