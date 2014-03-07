package rx.examples;

import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.schedulers.Schedulers;

public class BackpressureAcrossThreads {

    /**
     * Fails as of RxJava 0.17.0 RC6
     */
    @Test
    public void testIterableAcrossThread() {
        final AtomicInteger sentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        Observable.from(Sources.million()).map((i) -> {
            sentCount.incrementAndGet();
            return "Value_" + i;
        }).observeOn(Schedulers.newThread()).map((s) -> {
            receivedCount.incrementAndGet();
            // simulate doing computational work
                return Util.busyWork(1000);
            })
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .take(5)
                .toBlockingObservable().forEach(s -> {
                    long diff = sentCount.get() - receivedCount.get();
                    System.out.println("Sent: " + sentCount.get() + "  Received: " + receivedCount.get() + "  => " + diff);
                });
        long diff = sentCount.get() - receivedCount.get();
        if (diff > 10) {
            fail("No back pressure. " + diff + " items buffered.");
        }
    }

    /**
     * Fails as of RxJava 0.17.0 RC6
     * 
     * Expects IllegalStateException as buffer overflows without support for backpressure.
     */
    @Test(expected = IllegalStateException.class)
    public void testFileAcrossThreadWithoutBackpressureSupport() {
        final AtomicInteger sentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        Sources.getFileWithoutBackpressureSupport().map((i) -> {
            sentCount.incrementAndGet();
            return "Value_" + i;
        }).observeOn(Schedulers.newThread()).map((s) -> {
            receivedCount.incrementAndGet();
            // simulate doing computational work
                return Util.busyWork(1000);
            })
                .skip(1000)
                .take(1)
                .toBlockingObservable().forEach(s -> {
                    long diff = sentCount.get() - receivedCount.get();
                    System.out.println("Sent: " + sentCount.get() + "  Received: " + receivedCount.get() + "  => " + diff);
                });
        long diff = sentCount.get() - receivedCount.get();
        if (diff > 10) {
            fail("No back pressure. " + diff + " items buffered.");
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testFileAcrossThreadWithBackpressureSupport() {
        final AtomicInteger sentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        Sources.getFileWithBackpressureSupport().map((i) -> {
            sentCount.incrementAndGet();
            return "Value_" + i;
        }).skip(100).observeOn(Schedulers.newThread()).map((s) -> {
            receivedCount.incrementAndGet();
            // simulate doing computational work
                return Util.busyWork(1000);
            })
                .skip(50)
                .take(1)
                .doOnEach((n) -> System.out.println(n))
                .toBlockingObservable().forEach(s -> {
                    long diff = sentCount.get() - receivedCount.get();
                    System.out.println("Sent: " + sentCount.get() + "  Received: " + receivedCount.get() + "  => " + diff);
                });
        long diff = sentCount.get() - receivedCount.get();
        if (diff > 110) { // 100 skipped + 10 in buffer
            fail("No back pressure. " + diff + " items buffered.");
        }
        if (sentCount.get() < 110) {
            fail("Expected over 110 emitted but was: " + sentCount.get());
        }
    }

}
