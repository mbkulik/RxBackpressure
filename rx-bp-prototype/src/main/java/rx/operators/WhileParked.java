package rx.operators;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscription;
import rx.Observable.Operator;
import rx.Subscriber.Request;
import rx.functions.Action1;
import rx.Subscriber;

public class WhileParked {
    public static final Operator BUFFER = new Buffer();

    private static class Buffer<T> implements Operator<T, T> {
        @Override
        public Subscriber<? super T> call(final Subscriber<? super T> o) {
            final AtomicReference<Request> reqRef = new AtomicReference<Request>(Request.EMPTY);
            o.setProducer(new Action1<Request>() {
                @Override
                public void call(Request req) {
                    Request oldReq;
                    Request newReq;
                    do {
                        oldReq = reqRef.get();
                        newReq = oldReq == null ? req : req.add(oldReq);
                    } while (!reqRef.compareAndSet(oldReq, newReq));
                }
            });
            return new Subscriber<T>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                    o.onError(e);
                }

                @Override
                public void onNext(T t) {
                    Request req = reqRef.get();
                    if (req.countDown())
                        o.onNext(t);
                }
            };
        }
    }

    public static final Operator BLOCK = new Block();

    private static class Block<T> implements Operator<T, T> {
        @Override
        public Subscriber<? super T> call(final Subscriber<? super T> o) {
            final AtomicReference<Request> reqRef = new AtomicReference<Request>(Request.EMPTY);
            o.setProducer(new Action1<Request>() {
                @Override
                public void call(Request req) {
                    Request oldReq;
                    Request newReq;
                    do {
                        oldReq = reqRef.get();
                        newReq = oldReq == null ? req : req.add(oldReq);
                    } while (!reqRef.compareAndSet(oldReq, newReq));
                }
            });
            return new Subscriber<T>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                    o.onError(e);
                }

                @Override
                public void onNext(T t) {
                    Request req = reqRef.get();
                    if (req.countDown())
                        o.onNext(t);
                }
            };
        }
    }

    public static final Operator DROP = new Drop();

    private static class Drop<T> implements Operator<T, T> {
        @Override
        public Subscriber<? super T> call(final Subscriber<? super T> o) {
            final AtomicReference<Request> reqRef = new AtomicReference<Request>(Request.EMPTY);
            final AtomicBoolean completed = new AtomicBoolean();
            o.setProducer(new Action1<Request>() {
                @Override
                public void call(Request req) {
                    Request oldReq;
                    Request newReq;
                    do {
                        oldReq = reqRef.get();
                        newReq = oldReq == null ? req : req.add(oldReq);
                    } while (!reqRef.compareAndSet(oldReq, newReq));

                    if (completed.get())
                        o.onCompleted();
                }
            });
            return new Subscriber<T>() {
                @Override
                public void onCompleted() {
                    if (reqRef.get().countDown())
                        o.onCompleted();
                    else
                        completed.set(true);
                }

                @Override
                public void onError(Throwable e) {
                    o.onError(e);
                }

                @Override
                public void onNext(T t) {
                    Request req = reqRef.get();
                    if (req.countDown())
                        o.onNext(t);
                }
            };
        }
    }

    public static class Unsubscribe<T> implements Operator<T, T> {
        private Observable<T> observable;

        public Unsubscribe(Observable<T> observable) {
            this.observable = observable;
        }

        @Override
        public Subscriber<? super T> call(Subscriber<? super T> o) {
            final Subscriber<T> inner = new Subscriber<T>() {
                @Override
                public void onCompleted() {
                    // TODO Auto-generated method stub
                    
                }
                
                @Override
                public void onError(Throwable e) {
                    // TODO Auto-generated method stub
                    
                }
                
                @Override
                public void onNext(T t) {
                    // TODO Auto-generated method stub
                    
                }
            };
            
            o.setProducer(new Action1<Request>() {
                @Override
                public void call(Request req) {
                    observable.subscribe(inner);
                }
            });
            return new Subscriber<T>() {
                @Override
                public void onCompleted() {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onError(Throwable e) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onNext(T t) {
                    // TODO Auto-generated method stub

                }
            };
        }
    }
}
