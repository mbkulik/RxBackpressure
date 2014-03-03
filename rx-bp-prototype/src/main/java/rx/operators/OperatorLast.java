package rx.operators;

import rx.Observable.Operator;
import rx.Subscriber;

public class OperatorLast implements Operator<Object, Object> {
    private static final Operator INSANCE = new OperatorLast();
    private static final Object MARKER = new Object();

    public static <T> Operator<? extends T, ? super T> getInstance() {
        return INSANCE;
    }

    @Override
    public Subscriber<? super Object> call(final Subscriber<? super Object> o) {
        return new Subscriber<Object>() {
            private Object last = MARKER;

            @Override
            public void onCompleted() {
                if (last == MARKER) {
                    o.onError(new IllegalArgumentException("Can't get last from empty Observable"));
                    return;
                }
                o.onNext(last);
                o.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onNext(Object t) {
                last = t;
            }
        };
    }
}
