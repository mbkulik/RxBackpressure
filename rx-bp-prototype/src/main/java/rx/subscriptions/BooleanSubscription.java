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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Subscription that can be checked for status such as in a loop inside an {@link Observable} to
 * exit the loop if unsubscribed.
 * 
 * @see <a
 *      href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.booleandisposable(v=vs.103).aspx">Rx.Net
 *      equivalent BooleanDisposable</a>
 */
public final class BooleanSubscription implements Subscription {

    private final AtomicBoolean unsubscribed = new AtomicBoolean(false);
    private final Action0 unsubscribeAction;
    private final AtomicReference<Action1<Integer>> producer = new AtomicReference<Action1<Integer>>();

    private BooleanSubscription(Action0 unsubscribeAction) {
        this.unsubscribeAction = unsubscribeAction;
    }

    public static BooleanSubscription create() {
        return new BooleanSubscription(null);
    }

    public static BooleanSubscription create(Action0 onUnsubscribe) {
        return new BooleanSubscription(onUnsubscribe);
    }

    public static BooleanSubscription create(Action0 onUnsubscribe, Action0 onPause) {
        return new BooleanSubscription(onUnsubscribe);
    }

    public boolean isUnsubscribed() {
        return unsubscribed.get();
    }

    @Override
    public final void unsubscribe() {
        if (unsubscribed.compareAndSet(false, true)) {
            if (unsubscribeAction != null) {
                unsubscribeAction.call();
            }
        }
    }

    @Override
    public void setProducer(Action1<Integer> producer) {
        this.producer.set(producer);
    }
}
