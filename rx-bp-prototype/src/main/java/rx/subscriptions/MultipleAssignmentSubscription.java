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
import rx.functions.Action1;

/**
 * Subscription that can be checked for status such as in a loop inside an {@link Observable} to
 * exit the loop if unsubscribed.
 * 
 * @see <a
 *      href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.multipleassignmentdisposable">Rx.Net
 *      equivalent MultipleAssignmentDisposable</a>
 */
public final class MultipleAssignmentSubscription implements Subscription {

    private final AtomicReference<State> state = new AtomicReference<State>(new State(false, Subscriptions.empty(), null, -1));

    private static final class State {
        final boolean isUnsubscribed;
        final Subscription subscription;
        final Action1<Integer> producer;
        final int n;

        State(boolean u, Subscription s, Action1<Integer> p, int n) {
            this.isUnsubscribed = u;
            this.subscription = s;
            this.producer = p;
            this.n = n;
        }

        State unsubscribe() {
            return new State(true, subscription, null, 0);
        }

        State pause() {
            return new State(isUnsubscribed, subscription, producer, 0);
        }

        State resume(int n) {
            return new State(isUnsubscribed, subscription, producer, n);
        }

        State setProducer(Action1<Integer> producer) {
            return new State(isUnsubscribed, subscription, producer, n);
        }

        State set(Subscription subscription) {
            return new State(isUnsubscribed, subscription, producer, n);
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
    public void setProducer(final Action1<Integer> producer) {
        state.get().subscription.setProducer(new Action1<Integer>() {
            @Override
            public void call(Integer n) {
                State oldState;
                State newState;
                do {
                    oldState = state.get();
                    if (oldState.n != 0) {
                        return;
                    } else {
                        newState = oldState.resume(n);
                    }
                } while (!state.compareAndSet(oldState, newState));
                producer.call(n);
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
