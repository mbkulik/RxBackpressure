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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.functions.Action1;

/**
 * Subscription that represents a group of Subscriptions that are unsubscribed
 * together.
 * 
 * @see <a
 *      href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable(v=vs.103).aspx">Rx.Net
 *      equivalent CompositeDisposable</a>
 */
public final class CompositeSubscription implements Subscription {

    private final AtomicReference<State> state = new AtomicReference<State>(CLEAR_STATE);

    /** Empty initial state. */
    private static final State CLEAR_STATE;
    /** Unsubscribed empty state. */
    private static final State CLEAR_STATE_UNSUBSCRIBED;

    static {
        CLEAR_STATE = new State(false, new Subscription[0], null);
        CLEAR_STATE_UNSUBSCRIBED = new State(true, new Subscription[0], null);
    }

    private static final class State {
        private final boolean isUnsubscribed;
        private final Subscription[] subscriptions;
        private final Action1<Integer> worker;

        State(boolean u, Subscription[] s, Action1<Integer> worker) {
            this.isUnsubscribed = u;
            this.subscriptions = s;
            this.worker = worker;
        }

        State unsubscribe() {
            return CLEAR_STATE_UNSUBSCRIBED;
        }

        State add(Subscription s) {
            int idx = subscriptions.length;
            Subscription[] newSubscriptions = new Subscription[idx + 1];
            System.arraycopy(subscriptions, 0, newSubscriptions, 0, idx);
            newSubscriptions[idx] = s;
            return new State(isUnsubscribed, newSubscriptions, null);
        }

        State remove(Subscription s) {
            if ((subscriptions.length == 1 && subscriptions[0].equals(s)) || subscriptions.length == 0) {
                return clear();
            }
            Subscription[] newSubscriptions = new Subscription[subscriptions.length - 1];
            int idx = 0;
            for (Subscription _s : subscriptions) {
                if (!_s.equals(s)) {
                    // was not in this composite
                    if (idx == subscriptions.length) {
                        return this;
                    }
                    newSubscriptions[idx] = _s;
                    idx++;
                }
            }
            if (idx == 0) {
                return clear();
            }
            // subscription appeared more than once
            if (idx < newSubscriptions.length) {
                Subscription[] newSub2 = new Subscription[idx];
                System.arraycopy(newSubscriptions, 0, newSub2, 0, idx);
                return new State(isUnsubscribed, newSub2, worker);
            }
            return new State(isUnsubscribed, newSubscriptions, worker);
        }

        State clear() {
            return isUnsubscribed ? CLEAR_STATE_UNSUBSCRIBED : CLEAR_STATE;
        }

        public State setWorker(Action1<Integer> worker) {
            return new State(isUnsubscribed, subscriptions, worker);
        }
    }

    public CompositeSubscription() {
        state.set(CLEAR_STATE);
    }

    public CompositeSubscription(final Subscription... subscriptions) {
        state.set(new State(false, subscriptions, null));
    }

    @Override
    public boolean isUnsubscribed() {
        return state.get().isUnsubscribed;
    }

    public void add(final Subscription s) {
        State oldState;
        State newState;
        do {
            oldState = state.get();
            if (oldState.isUnsubscribed) {
                s.unsubscribe();
                return;
            } else {
                newState = oldState.add(s);
            }
        } while (!state.compareAndSet(oldState, newState));

        s.setWorker(new Action1<Integer>() {
            @Override
            public void call(Integer t1) {
                state.get().worker.call(t1);
            }
        });
    }

    public void remove(final Subscription s) {
        State oldState;
        State newState;
        do {
            oldState = state.get();
            if (oldState.isUnsubscribed) {
                return;
            } else {
                newState = oldState.remove(s);
            }
        } while (!state.compareAndSet(oldState, newState));
        // if we removed successfully we then need to call unsubscribe on it
        s.unsubscribe();
    }
    
    public void set(Subscription s) {
        State oldState;
        State newState;
        do {
            oldState = state.get();
            if (oldState.isUnsubscribed) {
                s.unsubscribe();
                return;
            } else {
                newState = oldState.clear().add(s);
            }
        } while (!state.compareAndSet(oldState, newState));
        // if we cleared successfully we then need to call unsubscribe on all previous
        unsubscribeFromAll(oldState.subscriptions);
    }

    public void clear() {
        State oldState;
        State newState;
        do {
            oldState = state.get();
            if (oldState.isUnsubscribed) {
                return;
            } else {
                newState = oldState.clear();
            }
        } while (!state.compareAndSet(oldState, newState));
        // if we cleared successfully we then need to call unsubscribe on all previous
        unsubscribeFromAll(oldState.subscriptions);
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
        unsubscribeFromAll(oldState.subscriptions);
    }

    private static void unsubscribeFromAll(Subscription[] subscriptions) {
        final List<Throwable> es = new ArrayList<Throwable>();
        for (Subscription s : subscriptions) {
            try {
                s.unsubscribe();
            } catch (Throwable e) {
                es.add(e);
            }
        }
        if (!es.isEmpty()) {
            if (es.size() == 1) {
                Throwable t = es.get(0);
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else {
                    throw new CompositeException(
                            "Failed to unsubscribe to 1 or more subscriptions.", es);
                }
            } else {
                throw new CompositeException(
                        "Failed to unsubscribe to 2 or more subscriptions.", es);
            }
        }
    }
    
    @Override
    public void setWorker(Action1<Integer> worker) {
        State oldState;
        State newState;
        do {
            oldState = state.get();
            if (oldState.worker != null) {
                throw new IllegalStateException("worker already set");
            } else {
                newState = oldState.setWorker(worker);
            }
        } while (!state.compareAndSet(oldState, newState));
    }
    
    @Override
    public Action1<Integer> getWorker() {
        return state.get().worker;
    }

    public Subscription get() {
        Subscription[] subscriptions = state.get().subscriptions;
        if (subscriptions.length > 1) {
            throw new IllegalStateException("expected only one or zero subscriptions");
        }
        if (subscriptions.length == 0) {
            return null;
        }
        return subscriptions[0];
    }
}
