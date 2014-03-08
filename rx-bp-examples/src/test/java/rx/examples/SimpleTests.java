package rx.examples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class SimpleTests {

    @Test
    public void basicSubscribe() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.from(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).subscribe(ts);

        ts.assertReceivedOnNext(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    public void takeSubscribe() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.from(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).take(2).subscribe(ts);

        ts.assertReceivedOnNext(Arrays.asList(0, 1));
    }

    @Test
    public void mapTakeSubscribe() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Observable.from(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).map(i -> "hello " + i).take(2).subscribe(ts);

        ts.assertReceivedOnNext(Arrays.asList("hello 0", "hello 1"));
    }

    @Test
    public void takeMapSubscribe() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Observable.from(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).take(2).map(i -> "hello " + i).subscribe(ts);

        ts.assertReceivedOnNext(Arrays.asList("hello 0", "hello 1"));
    }

    @Test
    public void observeOnSubscribe() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.from(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).observeOn(Schedulers.newThread()).subscribe(ts);

        ts.awaitTerminalEvent();
        System.out.println("Received => " + ts.getOnNextEvents());
        ts.assertReceivedOnNext(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    public void takeObserveOnSubscribe() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.from(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).take(2).observeOn(Schedulers.newThread()).subscribe(ts);

        ts.awaitTerminalEvent();
        System.out.println("Received => " + ts.getOnNextEvents());
        ts.assertReceivedOnNext(Arrays.asList(0, 1));
    }

    @Test
    public void observeOnTakeSubscribe() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.from(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).observeOn(Schedulers.newThread()).take(2).subscribe(ts);

        ts.awaitTerminalEvent();
        System.out.println("Received => " + ts.getOnNextEvents());
        ts.assertReceivedOnNext(Arrays.asList(0, 1));
    }

    @Test
    public void mapTakeObserveOnSubscribe() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Observable.from(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).map(i -> "hello " + i).take(2).observeOn(Schedulers.newThread()).subscribe(ts);

        ts.awaitTerminalEvent();
        System.out.println("Received => " + ts.getOnNextEvents());
        ts.assertReceivedOnNext(Arrays.asList("hello 0", "hello 1"));
    }

    @Test
    public void skipObserveOnSubscribe() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.from(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).skip(8).observeOn(Schedulers.newThread()).subscribe(ts);

        ts.awaitTerminalEvent();
        System.out.println("Received => " + ts.getOnNextEvents());
        ts.assertReceivedOnNext(Arrays.asList(8, 9));
    }

    @Test
    public void skipObserveOnSkipSubscribe() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.from(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).skip(2).observeOn(Schedulers.newThread()).skip(4).subscribe(ts);

        ts.awaitTerminalEvent();
        System.out.println("Received => " + ts.getOnNextEvents());
        ts.assertReceivedOnNext(Arrays.asList(6, 7, 8, 9));
    }

    @Test
    public void testIterableAcrossThread() {
        final AtomicInteger sentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();
        TestSubscriber<Long> ts = new TestSubscriber<Long>();

        Observable.from(Sources.million()).map((i) -> {
            sentCount.incrementAndGet();
            return "Value_" + i;
        }).observeOn(Schedulers.newThread()).map((s) -> {
            receivedCount.incrementAndGet();
            // simulate doing computational work
                return Util.busyWork(1000);
            })
                .take(5)
                .subscribe(ts);

        ts.awaitTerminalEvent();
        long diff = sentCount.get() - receivedCount.get();
        System.out.println("testIterableAcrossThread => sent: " + sentCount.get() + " received: " + receivedCount.get());
        if (diff > 10) { // 10 is working because observeOn is set to a buffer size of 10 right now
            fail("No back pressure. " + diff + " items buffered.");
        }
    }

    @Test
    public void testRequestsExactlySkipPlusTake() {
        final AtomicInteger sentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();
        TestSubscriber<Long> ts = new TestSubscriber<Long>();

        Observable.from(Sources.infinite()).map((i) -> {
            sentCount.incrementAndGet();
            return "Value_" + i;
        }).skip(12).take(6).observeOn(Schedulers.newThread()).map((s) -> {
            receivedCount.incrementAndGet();
            // simulate doing computational work
                return Util.busyWork(1000);
            }).subscribe(ts);

        ts.awaitTerminalEvent();
        long diff = sentCount.get() - receivedCount.get();
        System.out.println("testRequestsExactlySkipPlusTake => sent: " + sentCount.get() + " received: " + receivedCount.get());
        if (diff != 12) { // 10 is working because observeOn is set to a buffer size of 10 right now
            fail("Expected exactly 18 requested with 6 delivered but got diff of: " + diff);
        }
        assertEquals(18, sentCount.get());
        assertEquals(6, receivedCount.get());
    }

    @Test
    public void testTakeBiggerThanBufferAndLargeSkip() {
        final AtomicInteger sentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();
        TestSubscriber<Long> ts = new TestSubscriber<Long>();

        Observable.from(Sources.infinite()).map((i) -> {
            sentCount.incrementAndGet();
            return "Value_" + i;
        }).skip(5000).take(25).observeOn(Schedulers.newThread()).map((s) -> {
            receivedCount.incrementAndGet();
            // simulate doing computational work
                return Util.busyWork(1000);
            }).subscribe(ts);

        ts.awaitTerminalEvent();
        long diff = sentCount.get() - receivedCount.get();
        System.out.println("testTakeBiggerThanBufferAndLargeSkip => sent: " + sentCount.get() + " received: " + receivedCount.get());
        if (diff != 5000) {
            fail("Expected exactly 5025 requested with 25 delivered but got diff of: " + diff);
        }
        assertEquals(5025, sentCount.get());
        assertEquals(25, receivedCount.get());
    }

    @Test
    public void testSkipThenTakeAfterObserveOn() {
        final AtomicInteger sentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();
        TestSubscriber<Long> ts = new TestSubscriber<Long>();

        Observable.from(Sources.infinite()).map((i) -> {
            sentCount.incrementAndGet();
            return "Value_" + i;
        }).skip(5000).observeOn(Schedulers.newThread()).map((s) -> {
            receivedCount.incrementAndGet();
            // simulate doing computational work
                return Util.busyWork(1000);
            }).take(25).subscribe(ts);

        ts.awaitTerminalEvent();
        long diff = sentCount.get() - receivedCount.get();
        System.out.println("testSkipThenTakeAfterObserveOn => sent: " + sentCount.get() + " received: " + receivedCount.get());
        if (diff != 5005) { // since take happens AFTER observeOn it can't be exact in the request size so we fetch batches of 10 ... so 10+10+10 to achieve 25 = 5 extra
            fail("Backpressure diff wrong: " + diff);
        }
        assertEquals(5030, sentCount.get());
        assertEquals(25, receivedCount.get());
    }

    @Test
    public void testAsyncInfiniteThatCorrectlySchedulesItself() {
        final AtomicInteger sentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();
        final AtomicReference<Thread> emittedThread = new AtomicReference<Thread>();
        final AtomicReference<Thread> receivedThread = new AtomicReference<Thread>();
        TestSubscriber<Long> ts = new TestSubscriber<Long>();

        Sources.asyncInfinite().map((i) -> {
            sentCount.incrementAndGet();
            if (emittedThread.get() == null) {
                emittedThread.set(Thread.currentThread());
            } else {
                if (emittedThread.get() != Thread.currentThread()) {
                    System.err.println("*************** Should not have seen different threads");
                    throw new RuntimeException("Producer Thread should not change");
                }
            }
            return "Value_" + i;
        }).skip(5000).observeOn(Schedulers.newThread()).map((s) -> {
            receivedCount.incrementAndGet();
            if (receivedThread.get() == null) {
                receivedThread.set(Thread.currentThread());
            } else {
                if (receivedThread.get() != Thread.currentThread()) {
                    System.err.println("*************** Should not have seen different threads");
                    throw new RuntimeException("Receiver Thread should not change");
                }
            }
            // simulate doing computational work
                return Util.busyWork(1000);
            }).take(255).subscribe(ts);

        ts.awaitTerminalEvent();
        assertEquals(5260, sentCount.get()); // 5255 is smallest possible ... round up due to buffer of 10
        assertEquals(255, receivedCount.get());

        long diff = sentCount.get() - receivedCount.get();
        System.out.println("testAsyncInfiniteThatCorrectlySchedulesItself => sent: " + sentCount.get() + " received: " + receivedCount.get());
        if (diff != 5005) { // since take happens AFTER observeOn it can't be exact in the request size so we fetch batches of 10 ... so 10+10+10 to achieve 25 = 5 extra
            fail("Backpressure diff wrong: " + diff);
        }
    }

    @Test
    public void testInfiniteMadeAsyncViaSubscribeOn() {
        final AtomicInteger sentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();
        final AtomicReference<Thread> emittedThread = new AtomicReference<Thread>();
        final AtomicReference<Thread> receivedThread = new AtomicReference<Thread>();
        TestSubscriber<Long> ts = new TestSubscriber<Long>();

        Observable.from(Sources.infinite()).subscribeOn(Schedulers.newThread()).map((i) -> {
            sentCount.incrementAndGet();
            if (emittedThread.get() == null) {
                emittedThread.set(Thread.currentThread());
            } else {
                if (emittedThread.get() != Thread.currentThread()) {
                    System.err.println("*************** Should not have seen different threads");
                    throw new RuntimeException("Producer Thread should not change");
                }
            }
            return "Value_" + i;
        }).observeOn(Schedulers.newThread()).map((s) -> {
            receivedCount.incrementAndGet();
            if (receivedThread.get() == null) {
                receivedThread.set(Thread.currentThread());
            } else {
                if (receivedThread.get() != Thread.currentThread()) {
                    System.err.println("*************** Should not have seen different threads");
                    throw new RuntimeException("Receiver Thread should not change");
                }
            }
            // simulate doing computational work
                return Util.busyWork(1000);
            }).take(20).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertOnCompleted();
        assertEquals(20, sentCount.get());
        assertEquals(20, receivedCount.get());

        long diff = sentCount.get() - receivedCount.get();
        System.out.println("testInfiniteMadeAsyncViaSubscribeOn => sent: " + sentCount.get() + " received: " + receivedCount.get());
        if (diff != 0) {
            fail("Backpressure diff wrong: " + diff);
        }
    }
}
