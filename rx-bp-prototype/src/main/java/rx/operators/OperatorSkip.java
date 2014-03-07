package rx.operators;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;

/**
 * Returns an Observable that skips the first <code>num</code> items emitted by the source
 * Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/skip.png">
 * <p>
 * You can ignore the first <code>num</code> items emitted by an Observable and attend only to
 * those items that come after, by modifying the Observable with the skip operation.
 */
public final class OperatorSkip<T> implements Observable.Operator<T, T> {

    final int n;

    public OperatorSkip(int n) {
        this.n = n;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {

            int skipped = 0;

            @Override
            public void onCompleted() {
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(T t) {
                if (skipped >= n) {
                    child.onNext(t);
                } else {
                    skipped += 1;
                }
            }

            @Override
            public void setProducer(final Action1<Request> producer) {
                child.setProducer(new Action1<Request>() {

                    @Override
                    public void call(Request r) {
                        // add the skip num to the requested amount, since we'll skip everything and then emit to the buffer downstream
                        producer.call(r.add((n - skipped)));
                    }

                });
            }
        };
    }

}
