package rx.examples;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import rx.Observable;
import rx.Subscriber;

public class Sources {

    public static Iterable<Long> numbers(final long num) {
        return new Iterable<Long>() {

            @Override
            public Iterator<Long> iterator() {
                return new Iterator<Long>() {

                    long l = 0;

                    @Override
                    public boolean hasNext() {
                        return l < num;
                    }

                    @Override
                    public Long next() {
                        return l++;
                    }

                };
            }

        };
    }

    public static Iterable<Long> million() {
        return numbers(1000000);
    }

    /**
     * Runs forever ... will loop over Long.MAX_VALUE if needed.
     */
    public static Iterable<Long> infinite() {
        return new Iterable<Long>() {

            @Override
            public Iterator<Long> iterator() {
                return new Iterator<Long>() {

                    long l = 0;

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Long next() {
                        return l++;
                    }

                };
            }

        };
    }

    public static Observable<String> getFile() {
        return Observable.create((Subscriber<? super String> subscriber) -> {
            InputStream input = Sources.class.getResourceAsStream("/rx/examples/sample.txt");
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(input));
                String temp = null;
                while ((temp = in.readLine()) != null) {
                    subscriber.onNext(temp);
                }
                subscriber.onCompleted();
            } catch (Throwable e) {
                subscriber.onError(e);
            } finally {
                try {
                    input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
