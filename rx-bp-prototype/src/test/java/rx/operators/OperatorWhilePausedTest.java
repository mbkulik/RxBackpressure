package rx.operators;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observers.Subscribers;

public class OperatorWhilePausedTest {
    private static final Object VALUE = new Object();

    @Test
    @Ignore
    public void testDrop() {
        final AtomicReference<Action0> resume = new AtomicReference<Action0>();
        Subscriber<? super Object> out = spy(Subscribers.empty());
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                assertTrue(resume.compareAndSet(null, (Action0) invocation.getArguments()[0]));
                return null;
            }
        }).when(out).resumeWith(any(Action0.class));

        final AtomicInteger onNextCount = new AtomicInteger();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                onNextCount.incrementAndGet();
                return null;
            }
        }).when(out).onNext(any());
        
        Subscriber<? super Object> in = OperatorWhilePaused.DROP.call(out);
        InOrder order = inOrder(out);

        in.onNext(VALUE);
        {
            order.verify(out).onNext(VALUE);
            assertEquals(1, onNextCount.get());
            assertNull(resume.get());
        }
        in.pause();
        {
            assertNotNull(resume.get());
            order.verify(out).resumeWith(resume.get());
        }
        in.onNext(VALUE);
        {
            assertEquals(1, onNextCount.get());
        }
        resume.get().call();
        {
        }
        in.onNext(VALUE);
        {
            assertEquals(2, onNextCount.get());
        }
    }

}
