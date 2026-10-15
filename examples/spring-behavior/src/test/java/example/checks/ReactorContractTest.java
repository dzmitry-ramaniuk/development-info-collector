package example.checks;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class ReactorContractTest {
    @Test
    void publishOnSwitchesMapAfterSourceSubscribeOn() {
        Scheduler io = Schedulers.newBoundedElastic(1, 10, "source-io");
        Scheduler cpu = Schedulers.newSingle("processing");
        AtomicReference<String> sourceThread = new AtomicReference<>();
        AtomicReference<String> mapThread = new AtomicReference<>();
        try {
            Mono<String> result = Mono.fromCallable(() -> {
                sourceThread.set(Thread.currentThread().getName());
                return "data";
            }).subscribeOn(io).publishOn(cpu).map(data -> {
                mapThread.set(Thread.currentThread().getName());
                return data.toUpperCase();
            });
            StepVerifier.create(result).expectNext("DATA").expectComplete().verify(Duration.ofSeconds(2));
            assertThat(sourceThread.get()).startsWith("source-io");
            assertThat(mapThread.get()).startsWith("processing");
        } finally {
            io.dispose();
            cpu.dispose();
        }
    }

    @Test
    void intervalIsBuiltInsideVirtualTimeSupplier() {
        StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofHours(1)).take(3))
            .expectSubscription().thenAwait(Duration.ofHours(3)).expectNext(0L, 1L, 2L)
            .expectComplete().verify(Duration.ofSeconds(2));
    }
}
