package nextstep.subway.common.config.datasource;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.pool.PoolingConnectionFactoryProvider;
import io.r2dbc.spi.*;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.time.Duration;

@ConstructorBinding
@RequiredArgsConstructor
@Builder
@Getter
public class ConnectionProperties {
    private final String driver;
    private final String protocol;
    private final String host;
    private final Integer port;
    private final String username;
    private final String password;
    private final String database;
    private final Duration connectionTimeout;
    private final Boolean ssl;
    private final String poolName;
    private final Integer initialSize;
    private final Integer maxSize;
    private final Duration maxIdleTime;
    private final Duration maxLifeTime;
    private final Duration maxCreateConnectionTime;
    private final Duration maxAcquireTime;
    private final Integer acquireRetry;
    private final String validationQuery;
    private final Boolean registerJmx;

    public ConnectionPool createConnectionPool() {
        ConnectionFactory connectionFactory = ConnectionFactories.get(
                ConnectionFactoryOptions.builder()
                        .option(ConnectionFactoryOptions.DRIVER, driver)
                        .option(ConnectionFactoryOptions.PROTOCOL, protocol)
                        .option(ConnectionFactoryOptions.HOST, host)
                        .option(ConnectionFactoryOptions.PORT, port)
                        .option(ConnectionFactoryOptions.USER, username)
                        .option(ConnectionFactoryOptions.PASSWORD, password)
                        .option(ConnectionFactoryOptions.DATABASE, database)
                        .option(ConnectionFactoryOptions.CONNECT_TIMEOUT, connectionTimeout)
                        .option(ConnectionFactoryOptions.SSL, ssl)
                        .option(Option.valueOf("zeroDate"), "use_null")
                        .option(PoolingConnectionFactoryProvider.MAX_SIZE, maxSize)
                        .option(PoolingConnectionFactoryProvider.INITIAL_SIZE, initialSize)
                        .option(PoolingConnectionFactoryProvider.MAX_IDLE_TIME, maxIdleTime)
                        .option(PoolingConnectionFactoryProvider.MAX_LIFE_TIME, maxLifeTime)
                        .option(PoolingConnectionFactoryProvider.MAX_CREATE_CONNECTION_TIME, maxCreateConnectionTime)
                        .option(PoolingConnectionFactoryProvider.MAX_ACQUIRE_TIME, maxAcquireTime)
                        .option(PoolingConnectionFactoryProvider.ACQUIRE_RETRY, acquireRetry)
                        .option(PoolingConnectionFactoryProvider.VALIDATION_DEPTH, ValidationDepth.LOCAL)
                        .option(PoolingConnectionFactoryProvider.VALIDATION_QUERY, validationQuery)
                        .build());
        return new ConnectionPool(ConnectionPoolConfiguration.builder(connectionFactory)
                                          .name(poolName)
                                          .initialSize(initialSize)
                                          .maxSize(maxSize)
                                          .maxIdleTime(maxIdleTime)
                                          .maxLifeTime(maxLifeTime)
                                          .maxCreateConnectionTime(maxCreateConnectionTime)
                                          .maxAcquireTime(maxAcquireTime)
                                          .acquireRetry(acquireRetry)
                                          .validationDepth(ValidationDepth.LOCAL)
                                          .validationQuery(validationQuery)
                                          .registerJmx(registerJmx)
                                          .build());
    }
}
