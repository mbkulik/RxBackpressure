package rx.examples;

import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.schedulers.Schedulers;

public class BackpressureAcrossThreads {

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

    @Test
    public void testFileAcrossThread() {
        final AtomicInteger sentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        Sources.getFile().map((i) -> {
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
    }
}
