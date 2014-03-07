package rx.examples;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import rx.Observable;
import rx.Subscriber;
import rx.Subscriber.Request;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

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

    public static Observable<String> getFileWithoutBackpressureSupport() {
        return Observable.create((Subscriber<? super String> subscriber) -> {
            InputStream input = Sources.class.getResourceAsStream("/rx/examples/sample.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            Subscription dispose = Subscriptions.create(() -> {
                try {
                    System.out.println("getFileWithoutBackpressureSupport => close file");
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            // register for unsubscribe when error or unsubscribe occurs
                subscriber.add(dispose);

                try {
                    String temp = null;
                    while ((temp = in.readLine()) != null) {
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }
                        subscriber.onNext(temp);
                    }

                    // shutting down gracefully so dispose eagerly
                    dispose.unsubscribe();
                    subscriber.onCompleted();
                } catch (Throwable e) {
                    subscriber.onError(e);
                }

            });
    }

    public static Observable<String> getFileWithBackpressureSupport() {
        return Observable.create((Subscriber<? super String> subscriber) -> {
            InputStream input = Sources.class.getResourceAsStream("/rx/examples/sample.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(input));

            Subscription dispose = Subscriptions.create(() -> {
                try {
                    System.out.println("getFileWithBackpressureSupport => close file");
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            // register for unsubscribe when error or unsubscribe occurs
                subscriber.add(dispose);

                subscriber.setProducer((Request r) -> {
                    System.out.println("*** requested: " + r);
                    try {
                        String temp = null;
                        if (!r.countDown()) {
                            // we are not able to emit
                        return;
                    }
                    while ((temp = in.readLine()) != null) {
                        System.out.println("emit: " + temp);
                        subscriber.onNext(temp);
                        if (!r.countDown()) {
                            return;
                        }
                    }
                    // shutting down gracefully so dispose eagerly
                    dispose.unsubscribe();
                    // emission of this is covered by the r.countDown() checks above
                    subscriber.onCompleted();
                } catch (Throwable e) {
                    subscriber.onError(e);
                }
            })  ;
            });
    }
}
