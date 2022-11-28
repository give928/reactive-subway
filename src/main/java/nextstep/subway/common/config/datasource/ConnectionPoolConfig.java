package nextstep.subway.common.config.datasource;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

import java.time.Duration;

//@Profile({"local", "prod"})
//@Configuration
//@EnableR2dbcRepositories
//@EnableR2dbcAuditing
//@ConfigurationPropertiesScan
public class ConnectionPoolConfig extends AbstractR2dbcConfiguration {
    private final MasterConnectionPoolProperties masterConnectionPoolProperties;
    private final SlaveConnectionPoolProperties slaveConnectionPoolProperties;

    public ConnectionPoolConfig(MasterConnectionPoolProperties masterConnectionPoolProperties,
                                SlaveConnectionPoolProperties slaveConnectionPoolProperties) {
        this.masterConnectionPoolProperties = masterConnectionPoolProperties;
        this.slaveConnectionPoolProperties = slaveConnectionPoolProperties;
    }

    @Bean
    @Primary
    @Override
    public ConnectionFactory connectionFactory() {
        return new ConnectionPool(masterConnectionPoolProperties.getConnectionPoolConfiguration());
    }

    @Bean
    @Primary
    public ReactiveTransactionManager writeTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    private ConnectionFactory slaveConnectionFactory() {
        return new ConnectionPool(slaveConnectionPoolProperties.getConnectionPoolConfiguration());
    }

    @Bean
    public ReactiveTransactionManager readTransactionManager() {
        R2dbcTransactionManager r2dbcTransactionManager = new R2dbcTransactionManager(slaveConnectionFactory());
        r2dbcTransactionManager.setEnforceReadOnly(true);
        return r2dbcTransactionManager;
    }

    @ConfigurationProperties(prefix = "spring.r2dbc.pool.master")
    public static class MasterConnectionPoolProperties extends ConnectionPoolProperties {
        public MasterConnectionPoolProperties(String driver, String protocol, String host, Integer port, String username,
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

    @ConfigurationProperties(prefix = "spring.r2dbc.pool.slave")
    public static class SlaveConnectionPoolProperties extends ConnectionPoolProperties {
        public SlaveConnectionPoolProperties(String driver, String protocol, String host, Integer port, String username,
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
