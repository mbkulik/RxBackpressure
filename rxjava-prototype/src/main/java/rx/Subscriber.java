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
package rx;

import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;

/**
 * Provides a mechanism for receiving push-based notifications.
 * <p>
 * After an Observer calls an {@link Observable}'s <code>Observable.subscribe</code> method, the
 * {@link Observable} calls the Observer's <code>onNext</code> method to provide notifications. A
 * well-behaved {@link Observable} will call an Observer's <code>onCompleted</code> closure exactly
 * once or the Observer's <code>onError</code> closure exactly once.
 * <p>
 * For more information see the <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava
 * Wiki</a>
 * 
 * @param <T>
 */
public abstract class Subscriber<T> implements Observer<T>, Subscription {

    private final CompositeSubscription cs;
    private final Subscriber<?> op;

    private Subscriber(CompositeSubscription cs, Subscriber<?> op) {
        if (cs == null) {
            throw new IllegalArgumentException("The CompositeSubscription can not be null");
        }
        this.cs = cs;
        this.op = op;
    }

    protected Subscriber() {
        this(new CompositeSubscription(), null);
    }

    protected Subscriber(Subscriber<?> op) {
        this(op.cs, op);
    }

    protected Subscriber(CompositeSubscription cs) {
        this(cs, null);
    }

    /**
     * Used to register an unsubscribe callback.
     */
    public final void add(Subscription s) {
        cs.add(s);
    }

    @Override
    public final void unsubscribe() {
        cs.unsubscribe();
    }

    public final boolean isUnsubscribed() {
        return cs.isUnsubscribed();
    }

    public final void pause() {
        cs.pause();
    }

    protected void resume() {
        cs.resume();
    }

    public void resumeWith(Action0 resume) {
        cs.resumeWith(resume);
        if (op != null && op.isPaused()) {
            op.resumeWith(new Action0() {
                @Override
                public void call() {
                    resume();
                }
            });
        }
    }

    public final boolean isPaused() {
        return cs.isPaused();
    }
}