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

import java.util.concurrent.Future;

import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Helper methods and utilities for creating and working with {@link Subscription} objects
 */
public final class Subscriptions {
    /**
     * A {@link Subscription} that does nothing.
     * 
     * @return {@link Subscription}
     */
    public static Subscription empty() {
        return EMPTY;
    }

    /**
     * A {@link Subscription} which invokes the given {@link Action0} when unsubscribed.
     * 
     * @param unsubscribe
     *            Action to invoke on unsubscribe.
     * @return {@link Subscription}
     */
    public static Subscription create(final Action0 unsubscribe) {
        return BooleanSubscription.create(unsubscribe);
    }

    /**
     * A {@link Subscription} that wraps a {@link Future} and cancels it when unsubscribed.
     * 
     * 
     * @param f
     *            {@link Future}
     * @return {@link Subscription}
     */
    public static Subscription from(final Future<?> f) {
        return new Subscription() {

            @Override
            public void unsubscribe() {
                f.cancel(true);
            }

            @Override
            public boolean isUnsubscribed() {
                return f.isCancelled();
            }
        };
    }

    /**
     * A {@link Subscription} that groups multiple Subscriptions together and unsubscribes from all of them together.
     * 
     * @param subscriptions
     *            Subscriptions to group together
     * @return {@link Subscription}
     */

    public static CompositeSubscription from(Subscription... subscriptions) {
        return new CompositeSubscription(subscriptions);
    }

    /**
     * A {@link Subscription} that does nothing when its unsubscribe method is called.
     */
    private static Subscription EMPTY = new Subscription() {
        public void unsubscribe() {
        }

        @Override
        public boolean isUnsubscribed() {
            return false;
        }
    };
}
