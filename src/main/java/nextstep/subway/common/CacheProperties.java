package nextstep.subway.common;

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

    public static class Cache {
        private final String name;
        private final Long ttl;

        public Cache(String name, Long ttl) {
            this.name = name;
            this.ttl = ttl;
        }

        public String getName() {
            return name;
        }

        public Long getTtl() {
            return ttl;
        }
    }
}
