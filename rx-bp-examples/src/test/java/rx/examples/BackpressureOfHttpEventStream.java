package rx.examples;

import static org.junit.Assert.fail;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.server.HttpRequest;
import io.reactivex.netty.protocol.http.server.HttpResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.schedulers.Schedulers;

public class BackpressureOfHttpEventStream {

    /**
     * Fails as of RxJava 0.17.0 RC6
     */
    @Test
    public void testIterableOverNetwork() throws InterruptedException {
        final AtomicInteger sentCount = new AtomicInteger();
        final AtomicInteger receivedCount = new AtomicInteger();

        HttpServer<ByteBuf, ServerSentEvent> server = RxNetty.createHttpServer(8181, (HttpRequest<ByteBuf> request, HttpResponse<ServerSentEvent> response) -> {
            return Observable.from(Sources.numbers(1000000)).flatMap((i) -> {
                sentCount.incrementAndGet();
                return response.writeAndFlush(new ServerSentEvent("id_" + i, "data", String.valueOf(i)));
            });
        }, PipelineConfigurators.<ByteBuf> sseServerConfigurator());

        try {
            server.start();

            HttpClient<ByteBuf, ServerSentEvent> client = RxNetty.createHttpClient("localhost", 8181, PipelineConfigurators.<ByteBuf> sseClientConfigurator());
            client.submit(io.reactivex.netty.protocol.http.client.HttpRequest.createGet("/")).flatMap((response) -> {
                return response.getContent().map(event -> {
                    // do some work to make it a slow consumer 
                        long v = Util.busyWork(10000);
                        receivedCount.incrementAndGet();
                        return event.getEventData() + "_" + v;
                    });
            }).throttleFirst(500, TimeUnit.MILLISECONDS).take(5).toBlockingObservable().forEach(s -> {
                long diff = sentCount.get() - receivedCount.get();
                System.out.println("Sent: " + sentCount.get() + "  Received: " + receivedCount.get() + "  => " + diff);
            });
            long diff = sentCount.get() - receivedCount.get();
            if (diff > 10) {
                fail("No back pressure. " + diff + " items buffered.");
            }
        } finally {
            server.shutdown();
        }

    }

    /**
     * Fails as of RxJava 0.17.0 RC6
     */
    @Test
    public void testIterableOverNetworkThenAcrossThread() throws InterruptedException {
        final AtomicInteger sentCount = new AtomicInteger();
        final AtomicInteger clientReceivedCount = new AtomicInteger();
        final AtomicInteger threadReceivedCount = new AtomicInteger();

        HttpServer<ByteBuf, ServerSentEvent> server = RxNetty.createHttpServer(8181, (HttpRequest<ByteBuf> request, HttpResponse<ServerSentEvent> response) -> {
            return Observable.from(Sources.numbers(1000000)).flatMap((i) -> {
                sentCount.incrementAndGet();
                return response.writeAndFlush(new ServerSentEvent("id_" + i, "data", String.valueOf(i)));
            });
        }, PipelineConfigurators.<ByteBuf> sseServerConfigurator());

        try {
            server.start();

            HttpClient<ByteBuf, ServerSentEvent> client = RxNetty.createHttpClient("localhost", 8181, PipelineConfigurators.<ByteBuf> sseClientConfigurator());
            client.submit(io.reactivex.netty.protocol.http.client.HttpRequest.createGet("/")).flatMap((response) -> {
                return response.getContent().map(event -> {
                    // do some work to make it a slow consumer 
                        long v = Util.busyWork(5000);
                        clientReceivedCount.incrementAndGet();
                        return event.getEventData() + "_" + v;
                    });
            }).observeOn(Schedulers.newThread()).map(event -> {
                // do some work to make it a slow consumer 
                    long v = Util.busyWork(10000);
                    threadReceivedCount.incrementAndGet();
                    return event + "_" + v;
                }).throttleFirst(500, TimeUnit.MILLISECONDS).take(5).toBlockingObservable().forEach(s -> {
                long diff = sentCount.get() - threadReceivedCount.get();
                System.out.println("Sent: " + sentCount.get() + "  Client Received: " + clientReceivedCount.get() + "  Thread Received: " + threadReceivedCount.get() + "  => " + diff);
            });
            long diff = sentCount.get() - threadReceivedCount.get();
            if (diff > 10) {
                fail("No back pressure. " + diff + " items buffered.");
            }

        } finally {
            server.shutdown();
        }

    }

}
