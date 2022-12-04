package nextstep.subway.config.exception;

import lombok.extern.slf4j.Slf4j;
import nextstep.subway.auth.application.AuthorizationException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Order(-2)
@Slf4j
public class GlobalFunctionalEndpointsExceptionHandler implements WebExceptionHandler {
    private static final HandlerStrategiesResponseContext handlerStrategiesResponseContext;

    static {
        handlerStrategiesResponseContext = new HandlerStrategiesResponseContext();
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
        log.error("handle throwable", throwable);
        return response(throwable).flatMap(serverResponse -> serverResponse.writeTo(exchange, handlerStrategiesResponseContext));
    }

    private Mono<ServerResponse> response(Throwable throwable) {
        if (throwable instanceof AuthorizationException) {
            return response(HttpStatus.UNAUTHORIZED, throwable);
        }
        if (throwable instanceof RuntimeException) {
            return response(HttpStatus.BAD_REQUEST, throwable);
        }
        return response(HttpStatus.INTERNAL_SERVER_ERROR, throwable);
    }

    private Mono<ServerResponse> response(HttpStatus httpStatus, Throwable throwable) {
        return ServerResponse.status(httpStatus).bodyValue(throwable.getMessage());
    }

    private static class HandlerStrategiesResponseContext implements ServerResponse.Context {
        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return HandlerStrategies.withDefaults().messageWriters();
        }

        @Override
        public List<ViewResolver> viewResolvers() {
            return HandlerStrategies.withDefaults().viewResolvers();
        }
    }
}
