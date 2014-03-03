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

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;

/**
 * Subscription that can be checked for status such as in a loop inside an {@link Observable} to exit the loop if unsubscribed.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.booleandisposable(v=vs.103).aspx">Rx.Net equivalent BooleanDisposable</a>
 */
public final class BooleanSubscription implements Subscription {

    private final AtomicBoolean unsubscribed = new AtomicBoolean(false);
    private final Action0 unsubscribeAction;
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final Action0 pauseAction;

    private BooleanSubscription(Action0 unsubscribeAction, Action0 pauseAction) {
        this.unsubscribeAction = unsubscribeAction;
        this.pauseAction = pauseAction;
    }

    public static BooleanSubscription create() {
        return new BooleanSubscription(null, null);
    }

    public static BooleanSubscription create(Action0 onUnsubscribe) {
        return new BooleanSubscription(onUnsubscribe, null);
    }

    public static BooleanSubscription create(Action0 onUnsubscribe, Action0 onPause) {
        return new BooleanSubscription(onUnsubscribe, onPause);
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
    public boolean isPaused() {
        return paused.get();
    }
    
    @Override
    public void pause() {
        if (paused.compareAndSet(false, true)) {
            if (pauseAction != null)
                pauseAction.call();
        }
    }
    
    @Override
    public void resumeWith(Action0 resume) {
        resume.call();
    }
}
