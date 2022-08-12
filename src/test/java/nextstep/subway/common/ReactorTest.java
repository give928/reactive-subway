package nextstep.subway.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("논블로킹 테스트")
class ReactorTest {
    @DisplayName("블로킹 콜이 감지되면 예외가 발생한다.")
    @Test
    void block() {
        // when
        Mono<Long> mono = Mono.delay(Duration.ofSeconds(1))
                .doOnNext(it -> {
                    try {
                        Thread.sleep(100); // blocking call
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });

        // then
        StepVerifier.create(mono)
                .expectError(reactor.blockhound.BlockingOperationError.class)
                .verify();
    }

    @DisplayName("논블로킹 콜은 예외가 발생하지 않는다.")
    @Test
    void nonblock() {
        // when
        Mono<Long> mono = Mono.delay(Duration.ofSeconds(1));

        // then
        StepVerifier.create(mono)
                .assertNext(l -> assertThat(l).isZero())
                .verifyComplete();
    }
}
