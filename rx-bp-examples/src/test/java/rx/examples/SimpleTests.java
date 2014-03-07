package rx.examples;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

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
}
