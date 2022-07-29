package nextstep.subway.common.config.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "cache")
@ConstructorBinding
public class CacheProperties {
    private final Map<String, Cache> subway = new HashMap<>();

    public Map<String, Cache> getSubway() {
        return subway;
    }

    @RequiredArgsConstructor
    @Getter
    public static class Cache {
        private final String name;
        private final Long ttl;
    }
}
