package nextstep.subway.common.config.datasource;

import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.Duration;

@Profile({"local", "prod"})
@Configuration
@ConfigurationPropertiesScan
@EnableConfigurationProperties
@EnableTransactionManagement
@RequiredArgsConstructor
public class ConnectionFactoryConfig extends AbstractR2dbcConfiguration {
    private final MasterConnectionProperties masterConnectionProperties;
    private final SlaveConnectionProperties slaveConnectionProperties;

    @Bean
    @Override
    public ConnectionFactory connectionFactory() {
        return new DynamicRoutingConnectionFactory(masterConnectionFactory(), slaveConnectionFactory());
    }

    private ConnectionFactory masterConnectionFactory() {
        return masterConnectionProperties.createConnectionPool();
    }

    private ConnectionFactory slaveConnectionFactory() {
        return slaveConnectionProperties.createConnectionPool();
    }

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
//        return new R2dbcTransactionManager(new DynamicRoutingConnectionFactory(masterConnectionFactory(), slaveConnectionFactory()));
//        return new R2dbcTransactionManager(new TransactionAwareConnectionFactoryProxy(connectionFactory));
    }

    @ConfigurationProperties(prefix = "spring.r2dbc.master")
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

    @ConfigurationProperties(prefix = "spring.r2dbc.slave")
    public static class SlaveConnectionProperties extends ConnectionProperties {
        public SlaveConnectionProperties(String driver, String protocol, String host, Integer port, String username,
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
