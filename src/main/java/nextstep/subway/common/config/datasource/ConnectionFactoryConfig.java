package nextstep.subway.common.config.datasource;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;

import java.time.Duration;

@Profile({"local", "prod"})
@Configuration
@ConfigurationPropertiesScan
@EnableConfigurationProperties
public class ConnectionFactoryConfig extends AbstractR2dbcConfiguration {
    private final MasterConnectionProperties masterConnectionProperties;

    public ConnectionFactoryConfig(MasterConnectionProperties masterConnectionProperties) {
        this.masterConnectionProperties = masterConnectionProperties;
    }

    @Bean
    @Override
    public ConnectionFactory connectionFactory() {
        return masterConnectionProperties.createConnectionPool();
    }

    @ConfigurationProperties(prefix = "spring.r2dbc")
    public static class MasterConnectionProperties extends ConnectionProperties {
        public MasterConnectionProperties(String driver, String protocol, String host, Integer port, String username,
                                          String password, String database, Duration connectionTimeout, Boolean ssl,
                                          String poolName, Integer initialSize, Integer maxSize, Duration maxIdleTime,
                                          Duration maxLifeTime, Duration maxCreateConnectionTime,
                                          Duration maxAcquireTime, Integer acquireRetry, String validationQuery,
                                          Boolean registerJmx) {
            super(driver, protocol, host, port, username, password, database, connectionTimeout, ssl, poolName,
                  initialSize, maxSize, maxIdleTime, maxLifeTime, maxCreateConnectionTime, maxAcquireTime, acquireRetry,
                  validationQuery, registerJmx);
        }
    }
}
