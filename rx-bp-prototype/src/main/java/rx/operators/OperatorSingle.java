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

import rx.Observable.Operator;
import rx.Subscriber;

/**
 * If the Observable completes after emitting a single item that matches a
 * predicate, return an Observable containing that item. If it emits more than
 * one such item or no item, throw an IllegalArgumentException.
 */
public class OperatorSingle<T> implements Operator<T, T> {
    
    private boolean hasDefaultValue;
    private T defaultValue;

    public OperatorSingle() {
        this(false, null);
    }
    
    public OperatorSingle(T defaultValue) {
        this(true, defaultValue);
    }

    private OperatorSingle(boolean hasDefaultValue, T defaultValue) {
        this.hasDefaultValue = hasDefaultValue;
        this.defaultValue = defaultValue;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> o) {
        return new Subscriber<T>(o) {
            private T value;
            private boolean isEmpty = true;
            private boolean hasTooManyElemenets;

            @Override
            public void onCompleted() {
                if (hasTooManyElemenets) {
                    // We have already sent an onError message
                } else {
                    if (isEmpty) {
                        if (hasDefaultValue) {
                            o.onNext(defaultValue);
                            o.onCompleted();
                        } else {
                            o.onError(new IllegalArgumentException(
                                    "Sequence contains no elements"));
                        }
                    } else {
                        o.onNext(value);
                        o.onCompleted();
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onNext(T t) {
                if (isEmpty) {
                    this.value = t;
                    isEmpty = false;
                } else {
                    hasTooManyElemenets = true;
                    o.onError(new IllegalArgumentException(
                            "Sequence contains too many elements"));
                    o.unsubscribe();
                }
            }
        };
    }
}
