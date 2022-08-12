package nextstep.subway.auth.infrastructure;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.Collections;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorizationExtractor {
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER_TYPE = "Bearer";

    // @formatter:off
    public static String extract(ServerHttpRequest request) {
        return Optional.ofNullable(request.getHeaders().get(AUTHORIZATION))
                .orElse(Collections.emptyList())
                .stream()
                .filter(AuthorizationExtractor::isBearerType)
                .findFirst()
                .map(AuthorizationExtractor::extractAuthHeader)
                .orElse(null);
    }
    // @formatter:on

    private static String extractAuthHeader(String value) {
        String authHeaderValue = value.substring(BEARER_TYPE.length()).trim();
        if (authHeaderValue.contains(",")) {
            return authHeaderValue.split(",")[0];
        }
        return authHeaderValue;
    }

    private static boolean isBearerType(String value) {
        return value.toLowerCase().startsWith(BEARER_TYPE.toLowerCase());
    }
}
