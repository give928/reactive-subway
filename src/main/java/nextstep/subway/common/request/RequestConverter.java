package nextstep.subway.common.request;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

@Component
public class RequestConverter {
    @Value("${spring.data.rest.default-page-size}")
    private Integer defaultPageSize;
    @Value("${spring.data.rest.max-page-size}")
    private Integer maxPageSize;

    // @formatter:off
    public Pageable pageable(ServerRequest request) {
        return PageRequest.of(0, request.queryParam("size")
                .map(Integer::parseInt)
                .filter(size -> size <= maxPageSize)
                .orElse(defaultPageSize));
    }
    // @formatter:on
}
