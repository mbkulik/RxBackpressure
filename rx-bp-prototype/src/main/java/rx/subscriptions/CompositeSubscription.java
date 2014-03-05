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
import rx.functions.Actions;

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
    /** Empty initial state. */
    private static final State CLEAR_STATE_PAUSED;
    /** Unsubscribed empty state. */
    private static final State CLEAR_STATE_UNSUBSCRIBED;
    /** Unsubscribed empty state. */
    private static final State CLEAR_STATE_UNSUBSCRIBED_PAUSED;
    static {
        CLEAR_STATE = new State(false, new Subscription[0], null, -1);
        CLEAR_STATE_PAUSED = new State(false, new Subscription[0], null, 0);
        CLEAR_STATE_UNSUBSCRIBED = new State(true, new Subscription[0], null, -1);
        CLEAR_STATE_UNSUBSCRIBED_PAUSED = new State(true, new Subscription[0], null, 0);
    }

    private static final class State {
        private final boolean isUnsubscribed;
        private final Subscription[] subscriptions;
        private final Action1<Integer> producer;
        private final int n;

        State(boolean u, Subscription[] s, Action1<Integer> p, int n) {
            this.isUnsubscribed = u;
            this.subscriptions = s;
            this.producer = p;
            this.n = n;
        }

        State unsubscribe() {
            return CLEAR_STATE_UNSUBSCRIBED;
        }

        State add(Subscription s) {
            int idx = subscriptions.length;
            Subscription[] newSubscriptions = new Subscription[idx + 1];
            System.arraycopy(subscriptions, 0, newSubscriptions, 0, idx);
            newSubscriptions[idx] = s;
            return new State(isUnsubscribed, newSubscriptions, null, n);
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
                return new State(isUnsubscribed, newSub2, producer, n);
            }
            return new State(isUnsubscribed, newSubscriptions, producer, n);
        }

        // FIXME
        State clear() {
            return isUnsubscribed ? n == 0 ? CLEAR_STATE_UNSUBSCRIBED_PAUSED : CLEAR_STATE_UNSUBSCRIBED : n == 0 ? CLEAR_STATE_PAUSED : CLEAR_STATE;
        }

        public State pause() {
            // producer should already be null
            return new State(isUnsubscribed, subscriptions, producer, 0);
        }

        public State request(int n) {
            int newN = -1;
            if (n != -1) {
                newN = (this.n == -1 ? 0 : n) + n;
            }

            return new State(isUnsubscribed, subscriptions, producer, newN);
        }

        public State setProducer(Action1<Integer> producer) {
            return new State(isUnsubscribed, subscriptions, producer, n);
        }
    }

    public CompositeSubscription() {
        state.set(CLEAR_STATE);
    }

    public CompositeSubscription(final Subscription... subscriptions) {
        state.set(new State(false, subscriptions, null, -1));
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

    public void request(int n) {
        State oldState;
        State newState;
        State midState;
        do {
            oldState = state.get();
            midState = oldState.request(n);
            if (midState.n != 0 && midState.producer != null)
                newState = midState.pause();
            else {
                newState = midState;
            }
        } while (!state.compareAndSet(oldState, newState));

        if (midState.n != 0 && midState.producer != null) {
            midState.producer.call(midState.n);
        }
    }

    private void resumeFromAll(Action1<Integer>[] resumes, int n) {
        final List<Throwable> es = new ArrayList<Throwable>();
        for (Action1<Integer> r : resumes) {
            try {
                r.call(n);
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
                            "Failed to resume to 1 or more resume actions.", es);
                }
            } else {
                throw new CompositeException(
                        "Failed to resume to 2 or more resume actions.", es);
            }
        }
    }

    public void setProducer(Action1<Integer> producer) {
        State oldState;
        State midState;
        State newState;
        do {
            oldState = state.get();
            midState = oldState.setProducer(producer);
            if (midState.n != 0 && midState.producer != null)
                newState = oldState.clear();
            else
                newState = midState;
        } while (!state.compareAndSet(oldState, newState));
        midState.producer.call(midState.n);
    }

    public boolean isPaused() {
        return state.get().n == 0;
    }
}
