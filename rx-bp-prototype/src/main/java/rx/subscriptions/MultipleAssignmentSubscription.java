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

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;

/**
 * Subscription that can be checked for status such as in a loop inside an {@link Observable} to exit the loop if unsubscribed.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.multipleassignmentdisposable">Rx.Net equivalent MultipleAssignmentDisposable</a>
 */
public final class MultipleAssignmentSubscription implements Subscription {

    private final AtomicReference<State> state = new AtomicReference<State>(new State(false, false, Subscriptions.empty()));

    private static final class State {
        final boolean isUnsubscribed;
        final boolean isPaused;
        final Subscription subscription;

        State(boolean u, boolean p, Subscription s) {
            this.isUnsubscribed = u;
            this.isPaused = p;
            this.subscription = s;
        }

        State unsubscribe() {
            return new State(true, isPaused, subscription);
        }
        
        State pause() {
            return new State(isUnsubscribed, true, subscription);
        }
        
        public State resume() {
            return new State(isUnsubscribed, false, subscription);
        }

        State set(Subscription s) {
            return new State(isUnsubscribed, isPaused, s);
        }
    }

    public boolean isUnsubscribed() {
        return state.get().isUnsubscribed;
    }

    @Override
    public void unsubscribe() {
        State oldState;
        State newState;
        do {
            oldState = state.get();
            if (oldState.isUnsubscribed) {
                return;
            } else {
                newState = oldState.unsubscribe();
            }
        } while (!state.compareAndSet(oldState, newState));
        oldState.subscription.unsubscribe();
    }
    
    @Override
    public boolean isPaused() {
        return state.get().isPaused;
    }
    
    @Override
    public void pause() {
        State oldState;
        State newState;
        do {
            oldState = state.get();
            if (oldState.isPaused) {
                return;
            } else {
                newState = oldState.pause();
            }
        } while (!state.compareAndSet(oldState, newState));
        oldState.subscription.pause();
    }

    @Override
    public void resumeWith(final Action0 resume) {
        state.get().subscription.resumeWith(new Action0() {
            @Override
            public void call() {
                State oldState;
                State newState;
                do {
                    oldState = state.get();
                    if (!oldState.isPaused) {
                        return;
                    } else {
                        newState = oldState.resume();
                    }
                } while (!state.compareAndSet(oldState, newState));
                resume.call();
            }
        });
    }

    public void set(Subscription s) {
        if (s == null) {
            throw new IllegalArgumentException("Subscription can not be null");
        }
        State oldState;
        State newState;
        do {
            oldState = state.get();
            if (oldState.isUnsubscribed) {
                s.unsubscribe();
                return;
            } else {
                newState = oldState.set(s);
            }
        } while (!state.compareAndSet(oldState, newState));
    }

    public Subscription get() {
        return state.get().subscription;
    }

}
