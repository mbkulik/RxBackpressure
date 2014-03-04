package rx.examples;

import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observable;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class BackpressureOnCombinatorialOperators {

    @Test
    public void testZipInfiniteAtEnd() {
        final AtomicInteger o1SentCount = new AtomicInteger();
        final AtomicInteger o2SentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        Observable<Long> o1 = Observable.from(Sources.numbers(200)).doOnEach((n) -> {
            o1SentCount.incrementAndGet();
        });
        Observable<Long> o2 = Observable.from(Sources.infinite()).doOnEach((n) -> {
            o2SentCount.incrementAndGet();
        });
        zipAndAssert(o1SentCount, o2SentCount, receivedCount, o1, o2);
    }

    @Test
    public void testZipInfiniteAtEndAsync() {
        final AtomicInteger o1SentCount = new AtomicInteger();
        final AtomicInteger o2SentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        Observable<Long> o1 = Observable.from(Sources.numbers(200)).doOnEach((n) -> {
            o1SentCount.incrementAndGet();
        }).subscribeOn(Schedulers.newThread());
        Observable<Long> o2 = Observable.from(Sources.infinite()).doOnEach((n) -> {
            o2SentCount.incrementAndGet();
        }).subscribeOn(Schedulers.newThread());
        zipAndAssert(o1SentCount, o2SentCount, receivedCount, o1, o2);
    }

    /**
     * Fails as of RxJava 0.17.0 RC6
     */
    @Test(timeout = 1000)
    public void testZipInfiniteAtBeginning() {
        final AtomicInteger o1SentCount = new AtomicInteger();
        final AtomicInteger o2SentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        Observable<Long> o1 = Observable.from(Sources.infinite()).doOnEach((n) -> {
            o1SentCount.incrementAndGet();
        });
        Observable<Long> o2 = Observable.from(Sources.numbers(200)).doOnEach((n) -> {
            o2SentCount.incrementAndGet();
        });
        zipAndAssert(o1SentCount, o2SentCount, receivedCount, o1, o2);
    }

    @Test
    public void testZipInfiniteAtBeginningAsync() {
        final AtomicInteger o1SentCount = new AtomicInteger();
        final AtomicInteger o2SentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        Observable<Long> o1 = Observable.from(Sources.infinite()).doOnEach((n) -> {
            o1SentCount.incrementAndGet();
        }).subscribeOn(Schedulers.newThread());
        Observable<Long> o2 = Observable.from(Sources.numbers(200)).doOnEach((n) -> {
            o2SentCount.incrementAndGet();
        }).subscribeOn(Schedulers.newThread());
        zipAndAssert(o1SentCount, o2SentCount, receivedCount, o1, o2);
    }

    /**
     * Fails as of RxJava 0.17.0 RC6
     */
    @Test
    public void testZipLargeAtBeginning() {
        final AtomicInteger o1SentCount = new AtomicInteger();
        final AtomicInteger o2SentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        Observable<Long> o1 = Observable.from(Sources.numbers(10000)).doOnEach((n) -> {
            o1SentCount.incrementAndGet();
        });
        Observable<Long> o2 = Observable.from(Sources.numbers(200)).doOnEach((n) -> {
            o2SentCount.incrementAndGet();
        });
        zipAndAssert(o1SentCount, o2SentCount, receivedCount, o1, o2);
    }

    /**
     * Fails as of RxJava 0.17.0 RC6
     */
    @Test
    public void testZipLargeAtBeginningAsync() {
        final AtomicInteger o1SentCount = new AtomicInteger();
        final AtomicInteger o2SentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        Observable<Long> o1 = Observable.from(Sources.numbers(10000)).doOnEach((n) -> {
            o1SentCount.incrementAndGet();
        }).subscribeOn(Schedulers.newThread());
        Observable<Long> o2 = Observable.from(Sources.numbers(200)).doOnEach((n) -> {
            o2SentCount.incrementAndGet();
        }).subscribeOn(Schedulers.newThread());
        zipAndAssert(o1SentCount, o2SentCount, receivedCount, o1, o2);
    }

    @Test
    public void testZipLargeAtEnd() {
        final AtomicInteger o1SentCount = new AtomicInteger();
        final AtomicInteger o2SentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        Observable<Long> o1 = Observable.from(Sources.numbers(200)).doOnEach((n) -> {
            o1SentCount.incrementAndGet();
        });
        Observable<Long> o2 = Observable.from(Sources.numbers(10000)).doOnEach((n) -> {
            o2SentCount.incrementAndGet();
        });
        zipAndAssert(o1SentCount, o2SentCount, receivedCount, o1, o2);
    }

    @Test
    public void testZipLargeAtEndAsync() {
        final AtomicInteger o1SentCount = new AtomicInteger();
        final AtomicInteger o2SentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        Observable<Long> o1 = Observable.from(Sources.numbers(200)).doOnEach((n) -> {
            o1SentCount.incrementAndGet();
        }).subscribeOn(Schedulers.newThread());
        Observable<Long> o2 = Observable.from(Sources.numbers(10000)).doOnEach((n) -> {
            o2SentCount.incrementAndGet();
        }).subscribeOn(Schedulers.newThread());
        zipAndAssert(o1SentCount, o2SentCount, receivedCount, o1, o2);
    }

    /**
     * Fails as of RxJava 0.17.0 RC6
     */
    @Test
    public void testZipFastAndSlowAsync() {
        final AtomicInteger o1SentCount = new AtomicInteger();
        final AtomicInteger o2SentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        Observable<Long> o1 = Observable.interval(1, TimeUnit.MILLISECONDS).doOnEach((n) -> {
            o1SentCount.incrementAndGet();
        }).subscribeOn(Schedulers.newThread());
        Observable<Long> o2 = Observable.from(Sources.numbers(10000)).doOnEach((n) -> {
            o2SentCount.incrementAndGet();
        }).subscribeOn(Schedulers.newThread());
        zipAndAssert(o1SentCount, o2SentCount, receivedCount, o1, o2);
    }

    private void zipAndAssert(final AtomicInteger o1SentCount, final AtomicInteger o2SentCount, final AtomicInteger receivedCount, Observable<Long> o1, Observable<Long> o2) {
        Observable.zip(o1, o2, (t1, t2) -> {
            Util.busyWork(1000);
            receivedCount.incrementAndGet();
            return t1 + t2;
        }).throttleFirst(500, TimeUnit.MILLISECONDS).take(5).toBlockingObservable().forEach(s -> {
            long sentDiff = Math.abs(o1SentCount.get() - o2SentCount.get());
            long receivedDiff = Math.max(o1SentCount.get(), o2SentCount.get()) - receivedCount.get();
            System.out.println("Sent 1: " + o1SentCount.get() + "  Sent 2: " + o2SentCount.get() + " Received: " + receivedCount.get() +
                    "  => Sent Diff: " + sentDiff + "  Received Diff: " + receivedDiff);
        });
        long sentDiff = Math.abs(o1SentCount.get() - o2SentCount.get());
        long receivedDiff = Math.max(o1SentCount.get(), o2SentCount.get()) - receivedCount.get();
        if (sentDiff > 10 || receivedDiff > 10) {
            fail("No back pressure. " + receivedDiff + " items buffered and " + sentDiff + " difference between senders.");
        }
    }

    /**
     * Fails as of RxJava 0.17.0 RC6
     */
    @Test
    public void testMergeFastAndSlow() {
        performCombine((o1, o2) -> {
            return Observable.merge(o1, o2);
        });
    }

//    @Test
//    public void testConcatFastAndSlow() {
//        performCombine((o1, o2) -> {
//            return Observable.concat(o1, o2);
//        });
//    }

//    /**
//     * Fails as of RxJava 0.17.0 RC6
//     */
//    @Test
//    public void testCombineLatestFastAndSlow() {
//        performCombine((o1, o2) -> {
//            return Observable.combineLatest(o1, o2, (t1, t2) -> {
//                return t1 + t2;
//            });
//        });
//    }

    private void performCombine(Func2<Observable<Long>, Observable<Long>, Observable<Long>> f) {
        final AtomicInteger o1SentCount = new AtomicInteger();
        final AtomicInteger o2SentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        final AtomicReference<Thread> t1 = new AtomicReference<Thread>();
        final AtomicReference<Thread> t2 = new AtomicReference<Thread>();

        Observable<Long> o1 = Observable.interval(1, TimeUnit.MILLISECONDS).doOnEach((n) -> {
            o1SentCount.incrementAndGet();
            if (t1.get() == null) {
                t1.set(Thread.currentThread());
            }
        }).subscribeOn(Schedulers.newThread());
        Observable<Long> o2 = Observable.from(Sources.numbers(10000000)).doOnEach((n) -> {
            o2SentCount.incrementAndGet();
            if (t2.get() == null) {
                t2.set(Thread.currentThread());
            }
        }).subscribeOn(Schedulers.newThread());

        AtomicBoolean t1Blocked = new AtomicBoolean();
        AtomicBoolean t2Blocked = new AtomicBoolean();

        f.call(o1, o2).doOnEach(s -> {
            markIfThreadIsBlocked(t1, t1Blocked);
            markIfThreadIsBlocked(t2, t2Blocked);
            receivedCount.incrementAndGet();
        }).throttleFirst(500, TimeUnit.MILLISECONDS).take(5).toBlockingObservable().forEach(s -> {
            long diff = (o1SentCount.get() + o2SentCount.get()) - receivedCount.get();
            System.out.println("Sent 1: " + o1SentCount.get() + "  Sent 2: " + o2SentCount.get() + " Received: " + receivedCount.get() +
                    "  => Diff: " + diff);
        });

        System.out.println("Threads Blocked => t1: " + t1Blocked.get() + " t2: " + t2Blocked.get());

        long diff = (o1SentCount.get() + o2SentCount.get()) - receivedCount.get();
        if (diff > 10) {
            fail("No back pressure. " + diff + " items buffered.");
        }

        if (t1Blocked.get() || t2Blocked.get()) {
            fail("Threads were blocked.");
        }
    }

    private void markIfThreadIsBlocked(final AtomicReference<Thread> t, AtomicBoolean tBlocked) {
        if (t.get() != null && (t.get().getState() == Thread.State.BLOCKED)) {
            //            System.out.println(t.get() + " => " + t.get().getState());
            tBlocked.set(true);
        }
    }
}
