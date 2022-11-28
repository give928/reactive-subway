package nextstep.subway.common.config.datasource;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

@Profile({"local", "prod"})
@Configuration
@EnableR2dbcRepositories
@EnableR2dbcAuditing
@ConfigurationPropertiesScan
public class ConnectionConfig extends AbstractR2dbcConfiguration {
    private final MasterConnectionProperties masterConnectionProperties;
    private final SlaveConnectionProperties slaveConnectionProperties;

    public ConnectionConfig(MasterConnectionProperties masterConnectionProperties,
                            SlaveConnectionProperties slaveConnectionProperties) {
        this.masterConnectionProperties = masterConnectionProperties;
        this.slaveConnectionProperties = slaveConnectionProperties;
    }

    @Bean
    @Primary
    @Override
    public ConnectionFactory connectionFactory() {
//        return ConnectionFactories.get(masterConnectionProperties.getConnectionFactoryOptions());
//        return new DynamicRoutingConnectionFactory(
//                ConnectionFactories.get(masterConnectionProperties.getConnectionFactoryOptions()),
//                ConnectionFactories.get(slaveConnectionProperties.getConnectionFactoryOptions()));
        return ConnectionFactories.get(masterConnectionProperties.getConnectionFactoryOptions());
    }

    @Bean
    @Primary
    public ReactiveTransactionManager writeTransactionManager(ConnectionFactory connectionFactory) {
//        return new R2dbcTransactionManager(new TransactionAwareConnectionFactoryProxy(connectionFactory));
        return new R2dbcTransactionManager(connectionFactory);
    }

    private ConnectionFactory slaveConnectionFactory() {
        return ConnectionFactories.get(slaveConnectionProperties.getConnectionFactoryOptions());
    }

    @Bean
    public ReactiveTransactionManager readTransactionManager() {
        R2dbcTransactionManager r2dbcTransactionManager = new R2dbcTransactionManager(slaveConnectionFactory());
        r2dbcTransactionManager.setEnforceReadOnly(true);
        return r2dbcTransactionManager;
    }

    @ConfigurationProperties(prefix = "spring.r2dbc.master")
    public static class MasterConnectionProperties extends ConnectionProperties {
        public MasterConnectionProperties(String driver, String host, int port, String user, String password,
                                          String database) {
            super(driver, host, port, user, password, database);
        }
    }

    @ConfigurationProperties(prefix = "spring.r2dbc.slave")
    public static class SlaveConnectionProperties extends ConnectionProperties {
        public SlaveConnectionProperties(String driver, String host, int port, String user, String password,
                                         String database) {
            super(driver, host, port, user, password, database);
        }
    }
}
