package nextstep.subway.auth.infrastructure;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorizationExtractor {
    private static final String BEARER_TYPE = "Bearer";
    private static final String AUTHORIZATION_SPLITTER = ",";

    // @formatter:off
    public static Mono<String> extract(String value) {
        return Mono.just(value)
                .filter(AuthorizationExtractor::isBearerType)
                .map(AuthorizationExtractor::extractAuthHeader)
                .switchIfEmpty(Mono.empty());
    }
    // @formatter:on

    private static String extractAuthHeader(String value) {
        String authHeaderValue = value.substring(BEARER_TYPE.length()).trim();
        if (authHeaderValue.contains(AUTHORIZATION_SPLITTER)) {
            return authHeaderValue.split(AUTHORIZATION_SPLITTER)[0];
        }
        return authHeaderValue;
    }

    private static boolean isBearerType(String value) {
        return value.toLowerCase().startsWith(BEARER_TYPE.toLowerCase());
    }
}
