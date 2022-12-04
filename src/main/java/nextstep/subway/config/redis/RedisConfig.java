package nextstep.subway.config.redis;

import nextstep.subway.common.cache.annotation.ReactiveCacheAnnotationParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheAnnotationParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Profile({"local", "prod"})
@Configuration
public class RedisConfig {
    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${spring.redis.password}")
    private String password;

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory()  {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(host, port);
        redisStandaloneConfiguration.setPassword(password);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisStandaloneConfiguration);
        factory.setEagerInitialization(true);
        return factory;
    }

    @Bean
    public CacheAnnotationParser cacheAnnotationParser() {
        return new ReactiveCacheAnnotationParser();
    }
}
