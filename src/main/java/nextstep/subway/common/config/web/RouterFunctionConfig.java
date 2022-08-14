package nextstep.subway.common.config.web;

import nextstep.subway.station.handler.StationHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

@Configuration
public class RouterFunctionConfig {
    private static final String STATIONS = "/stations";

    // @formatter:off
    @Bean
    public RouterFunction<ServerResponse> routeIndex(
            @Value("classpath:/templates/index.html") final Resource indexHtml) {
        return RouterFunctions.route()
                .nest(RouterFunctionConfig::isAcceptHtml, builder -> builder
                        .GET(STATIONS, request -> response(indexHtml))
                        .GET("/lines", request -> response(indexHtml))
                        .GET("/sections", request -> response(indexHtml))
                        .GET("/path", request -> response(indexHtml))
                        .GET("/login", request -> response(indexHtml))
                        .GET("/join", request -> response(indexHtml))
                        .GET("/mypage", request -> response(indexHtml))
                        .GET("/mypage/edit", request -> response(indexHtml))
                        .GET("/favorites", request -> response(indexHtml)))
                .GET("/", request -> response(indexHtml))
                .build();
    }
    // @formatter:on

    // @formatter:off
    @Bean
    public RouterFunction<ServerResponse> routeStations(StationHandler stationHandler) {
        return RouterFunctions.route()
                .path(STATIONS, builder -> builder
                        .GET("", request -> stationHandler.showStations())
                        .POST("", stationHandler::createStation)
                        .GET("/pages", stationHandler::pagingStations)
                        .DELETE("/{id}", stationHandler::deleteStation)
                )
                .build();
    }
    // @formatter:on

    private static boolean isAcceptHtml(ServerRequest request) {
        return isFirstAccept(request, MediaType.TEXT_HTML);
    }

    // @formatter:off
    private static boolean isFirstAccept(ServerRequest request, MediaType mediaType) {
        return Optional.of(request.headers()
                                   .accept())
                .orElse(Collections.emptyList())
                .stream()
                .findFirst()
                .filter(type -> Objects.equals(type, mediaType))
                .isPresent();
    }
    // @formatter:on

    // @formatter:off
    private Mono<ServerResponse> response(Resource indexHtml) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .bodyValue(indexHtml);
    }
    // @formatter:on
}
