package rx.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.Subscribers;

public class OperatorWhilePausedTest {
    private static final Object VALUE = new Object();

    @Test
    public void testDrop() {
        final AtomicReference<Action1<Integer>> resume = new AtomicReference<Action1<Integer>>();
        Subscriber<? super Object> out = spy(Subscribers.empty());
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                assertTrue(resume.compareAndSet(null, (Action1<Integer>) invocation.getArguments()[0]));
                return null;
            }
        }).when(out).setProducer(any(Action1.class));

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
        //in.pause();
        {
            assertNotNull(resume.get());
            order.verify(out).setProducer(resume.get());
        }
        in.onNext(VALUE);
        {
            assertEquals(1, onNextCount.get());
        }
        resume.get().call(1);
        {
        }
        in.onNext(VALUE);
        {
            assertEquals(2, onNextCount.get());
        }
    }

}
