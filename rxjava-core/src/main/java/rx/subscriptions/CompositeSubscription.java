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
import rx.functions.Action0;

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
        CLEAR_STATE = new State(false, false, new Subscription[0], new Action0[0]);
        CLEAR_STATE_PAUSED = new State(false, true, new Subscription[0], new Action0[0]);
        CLEAR_STATE_UNSUBSCRIBED = new State(true, false, new Subscription[0], new Action0[0]);
        CLEAR_STATE_UNSUBSCRIBED_PAUSED = new State(true, true, new Subscription[0], new Action0[0]);
    }

    private static final class State {
        private final boolean isUnsubscribed;
        private final boolean isPaused;
        private final Subscription[] subscriptions;
        private final Action0[] resumes;

        State(boolean u, boolean p, Subscription[] s, Action0[] r) {
            this.isUnsubscribed = u;
            this.isPaused = p;
            this.subscriptions = s;
            this.resumes = r;
        }

        State unsubscribe() {
            return CLEAR_STATE_UNSUBSCRIBED;
        }

        State add(Subscription s) {
            int idx = subscriptions.length;
            Subscription[] newSubscriptions = new Subscription[idx + 1];
            System.arraycopy(subscriptions, 0, newSubscriptions, 0, idx);
            newSubscriptions[idx] = s;
            return new State(isUnsubscribed, isPaused, newSubscriptions, new Action0[0]);
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
                return new State(isUnsubscribed, isPaused, newSub2, resumes);
            }
            return new State(isUnsubscribed, isPaused, newSubscriptions, resumes);
        }

        State clear() {
            return isUnsubscribed ? isPaused ? CLEAR_STATE_UNSUBSCRIBED_PAUSED : CLEAR_STATE_UNSUBSCRIBED : isPaused ? CLEAR_STATE_PAUSED : CLEAR_STATE;
        }

        public State pause() {
            if (this == CLEAR_STATE)
                return CLEAR_STATE_PAUSED;
            if (this == CLEAR_STATE_UNSUBSCRIBED)
                return CLEAR_STATE_UNSUBSCRIBED_PAUSED;
            return new State(isUnsubscribed, true, subscriptions, resumes);
        }

        public State resume() {
            if (this == CLEAR_STATE_PAUSED)
                return CLEAR_STATE;
            if (this == CLEAR_STATE_UNSUBSCRIBED_PAUSED)
                return CLEAR_STATE_UNSUBSCRIBED;
            return new State(isUnsubscribed, false, subscriptions, new Action0[0]);
        }

        public State toResume(Action0 r) {
            int idx = resumes.length;
            Action0[] newresumes = new Action0[idx + 1];
            System.arraycopy(resumes, 0, newresumes, 0, idx);
            newresumes[idx] = r;
            return new State(isUnsubscribed, isPaused, subscriptions, newresumes);
        }
    }

    public CompositeSubscription() {
        state.set(CLEAR_STATE);
    }

    public CompositeSubscription(final Subscription... subscriptions) {
        state.set(new State(false, false, subscriptions, new Action0[0]));
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
            if (oldState.isPaused) {
                s.pause();
            }
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
        pauseAll(oldState.subscriptions);
    }

    private void pauseAll(Subscription[] subscriptions) {
        final List<Throwable> es = new ArrayList<Throwable>();
        for (Subscription s : subscriptions) {
            try {
                s.pause();
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
                            "Failed to pause to 1 or more subscribers.", es);
                }
            } else {
                throw new CompositeException(
                        "Failed to pause to 2 or more subscribers.", es);
            }
        }
    }

    public void resume() {
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
        resumeFromAll(oldState.resumes);
    }

    private void resumeFromAll(Action0[] resumes) {
        final List<Throwable> es = new ArrayList<Throwable>();
        for (Action0 r : resumes) {
            try {
                r.call();
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

    public void resumeWith(Action0 resume) {
        State oldState;
        State newState;
        do {
            oldState = state.get();
            if (!oldState.isPaused) {
                resume.call();
                return;
            } else {
                newState = oldState.toResume(resume);
            }
        } while (!state.compareAndSet(oldState, newState));
    }

    public boolean isPaused() {
        return state.get().isPaused;
    }
}
