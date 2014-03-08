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

import com.sun.org.apache.bcel.internal.generic.ISUB;

import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscriber.Request;
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
        Action1<Request> func = new Action1<Request>() {
            @Override
            public void call(final Request r) {
                System.out.println("**** OnSubscribeFromIterable => OnSubscribe start: " + r);
                if (!r.countDown())
                    return;

                while (iter.hasNext()) {
                    final T value = iter.next();
                    System.err.println("OnSubscribeFromIterable => p t = " + value + " thread " + Thread.currentThread());
                    o.onNext(value);

                    if (!r.countDown())
                        return;
                }
                System.out.println("*** OnSubscribeFromIterable => onCompleted");
                o.onCompleted();
            }

        };

        System.out.println("**** OnSubscribeFromIterable => OnSubscribe setProducer: " + func);
        o.setProducer(func);
    }
}
