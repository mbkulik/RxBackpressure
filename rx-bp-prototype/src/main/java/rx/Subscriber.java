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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Provides a mechanism for receiving push-based notifications.
 * <p>
 * After an Observer calls an {@link Observable}'s <code>Observable.subscribe</code> method, the {@link Observable} calls the Observer's <code>onNext</code> method to provide notifications. A
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

    protected Subscriber(final Subscriber<?> op) {
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

    public void request(int n) {
        State previous;
        State intermediate;
        State newState;
        do {
            previous = state.get();
            newState = previous.request(n);
            intermediate = newState;
            if (intermediate.p != null) {
                // we want to try and claim
                newState = intermediate.claim();
            }
        } while (!state.compareAndSet(previous, newState));
        if (intermediate.p != null && intermediate.n > 0) {
            // we have both P and N and won the claim so invoke
            System.err.println("Subscriber => request => producer.call in " + this);
            intermediate.p.call(new Request(this, intermediate.n));
        }
    }

    public void pauseProducer() {
        System.out.println("Pausing producer: state => " + state.get().n + " p : " + state.get().p);
    }

    public void setProducer(Action1<Request> producer) {
        if (op == null) {
            // end of chain, we must run
            int claimed = claim(producer);
            System.out.println("setProducer: " + claimed);
            if (claimed > 0) {
                // use count if we have it
                producer.call(new Request(this, claimed));
            } else {
                System.out.println("run with infinite");
                // otherwise we must run when at the end of the chain
                producer.call(new Request(this, State.INFINITE));
            }
        } else {
            // middle operator ... we pass thru unless a request has been made

            // if we have a non-0 value we will run as that means this operator has expressed interest
            int claimed = claim(producer);
            if (claimed == State.NOT_SET) {
                // we pass-thru to the next producer as it has not been set
                System.err.println("Subscriber => pass thru from " + this + "  to  " + op);
                op.setProducer(producer);
            } else if (claimed != State.PAUSED) {
                // it has been set and is not paused so we'll execute
                System.err.println("Subscriber => setProducer => producer.call in " + this);
                producer.call(new Request(this, claimed));
            }
        }
    }

    public static final class Request {

        private final Subscription s;
        private final AtomicInteger request;

        private Request(Subscription s, int num) {
            this(s, new AtomicInteger(num));
        }

        private Request(Subscription s, AtomicInteger numRef) {
            this.s = s;
            this.request = numRef;
        }
        

        public Request min(int num) {
            if (this.request.get() < num) {
                return this;
            }
            return new Request(s, this.request);
        }

        public Request add(int num) {
            return new Request(s, this.request.addAndGet(num));
        }
        
        public Request max(int num) {
            if (this.request.get() > num) {
                return this;
            }
            return new Request(s, this.request);
        }

        public final boolean countDown() {
            if (s.isUnsubscribed()) {
                return false;
            }
            if (request.get() == State.INFINITE) {
                // infinite
                return true;
            }
            return request.getAndDecrement() != 0;
        }

        @Override
        public String toString() {
            return "Subscriber.Request => unsubscribed: " + s.isUnsubscribed() + " numRequested: " + request.get();
        }
    }

    private int claim(Action1<Request> producer) {
        State previous;
        State newState;
        do {
            previous = state.get();
            newState = previous.claim(producer);
        } while (!state.compareAndSet(previous, newState));
        return previous.n;
    }

    private final AtomicReference<State> state = new AtomicReference<State>(State.create());

    private static class State {
        private final int n;
        private final Action1<Request> p;

        private final static int PAUSED = 0;
        private final static int INFINITE = -1;
        private final static int NOT_SET = -2;

        public State(int n, Action1<Request> p) {
            this.n = n;
            this.p = p;
        }

        public static State create() {
            return new State(-2, null); // not set
        }

        public State request(int _n) {
            int newN = _n;
            if (n > 0) {
                // add to existing if it hasn't been used yet
                newN = n + _n;
            }
            return new State(newN, p);
        }

        public State claim(Action1<Request> producer) {
            if (n == -2) {
                return new State(-2, producer);
            } else {
                return new State(0, producer);
            }
        }

        public State claim() {
            if (n == -2) {
                return this; // not set so return as is
            } else {
                return new State(0, p);
            }
        }

        public State producer(Action1<Request> _p) {
            return new State(n, _p);
        }

    }
}
