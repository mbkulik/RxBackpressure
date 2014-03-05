package rx.operators;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import rx.Notification;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;

public class OperatorWhilePaused {
    public static final Operator<Object, Object> DROP = new Operator<Object, Object>() {
        @Override
        public Subscriber<? super Object> call(final Subscriber<? super Object> o) {
            return new Subscriber<Object>(o) {
                @Override
                public void onCompleted() {
                    o.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    o.onError(e);
                }

                @Override
                public void onNext(Object t) {
                    if (!isPaused()) {
                        o.onNext(t);
                    }
                }
            };
        }
    };

    public static final Operator<Object, Object> BUFFER = new Operator<Object, Object>() {
        private Queue<Notification<Object>> buffer = new LinkedBlockingQueue<Notification<Object>>();

        @Override
        public Subscriber<? super Object> call(final Subscriber<? super Object> o) {
            return new Subscriber<Object>() {
                @Override
                public void onCompleted() {
                    doIt(Notification.<Object> createOnCompleted());
                }

                @Override
                public void onError(Throwable e) {
                    o.onError(e);
                }

                @Override
                public void onNext(Object t) {
                    doIt(Notification.createOnNext(t));
                }

                // a latch for blocking while we play out the backlog on resume to make sure things
                // come out in the order they went in.
                private CountDownLatch latch = new CountDownLatch(0);

                public void doIt(final Notification<Object> n) {
                    while (true) {
                        if (isPaused()) {
                            buffer.add(n);
                            // if this is the first call after this subscribers been paused.
                            if (latch.getCount() == 0) {
                                latch = new CountDownLatch(1);
                                o.setProducer(new Action1<Integer>() {
                                    @Override
                                    public void call(Integer n) {
                                        // implemented just like Observable.from but uses
                                        // isEmpty/remove
                                        // instead of an iterator's hasNext/next.
                                        if (isUnsubscribed()) {
                                            buffer.clear();
                                            return;
                                        }
                                        if (isPaused()) {
                                            o.setProducer(this);
                                            return;
                                        }
                                        while (!buffer.isEmpty()) {
                                            buffer.remove().accept(o);
                                            if (isUnsubscribed()) {
                                                buffer.clear();
                                                return;
                                            }
                                            if (isPaused()) {
                                                o.setProducer(this);
                                                return;
                                            }
                                        }
                                        latch.countDown();
                                    }
                                });
                            }
                            return;
                        }
                        else {
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                continue;
                            }
                            n.accept(o);
                            return;
                        }
                    }
                }
            };
        }
    };

    public static final Operator<Object, Object> BLOCK = new Operator<Object, Object>() {
        @Override
        public Subscriber<? super Object> call(final Subscriber<? super Object> o) {
            return new Subscriber<Object>() {

                @Override
                public void onCompleted() {
                    doIt(Notification.createOnCompleted());
                }

                @Override
                public void onError(Throwable e) {
                    o.onError(e);
                }

                @Override
                public void onNext(final Object t) {
                    doIt(Notification.createOnNext(t));
                }

                public void doIt(final Notification<Object> n) {
                    while (isPaused()) {
                        final CountDownLatch latch = new CountDownLatch(1);

                        // give the subscriber the means to unblock this thread.
                        setProducer(new Action1<Integer>() {
                            @Override
                            public void call(Integer n) {
                                latch.countDown();
                            }
                        });

                        // BLOCK!!!
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            // interrupted check to see if we are still paused.
                        }
                    }
                    n.accept(o);
                }
            };
        }
    };

    public static final class Unsubscribe<T> implements Operator<T, T> {
        private final Observable<T> observable;

        public Unsubscribe(Observable<T> observable) {
            this.observable = observable;
        }

        @Override
        public Subscriber<? super T> call(final Subscriber<? super T> out) {
            final Unsubscribe<T> self = this;
            return new Subscriber<T>() {
                @Override
                public void onCompleted() {
                    doIt(Notification.<T> createOnCompleted());
                }

                @Override
                public void onError(Throwable e) {
                    out.onError(e);
                }

                @Override
                public void onNext(T t) {
                    doIt(Notification.createOnNext(t));
                }

                private void doIt(Notification<T> n) {
                    if (isUnsubscribed()) {
                        return;
                    }
                    if (isPaused()) {
                        unsubscribe();
                        out.setProducer(new Action1<Integer>() {
                            @Override
                            public void call(Integer n) {
                                observable.lift(self).subscribe(out);
                            }
                        });
                        return;
                    }
                    n.accept(out);
                }
            };
        }
    }
}
