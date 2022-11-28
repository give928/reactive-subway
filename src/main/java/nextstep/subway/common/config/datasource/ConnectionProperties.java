package nextstep.subway.common.config.datasource;

import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.Getter;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@Getter
public class ConnectionProperties {
    private final String driver;
    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String database;

    public ConnectionProperties(String driver, String host, int port, String user, String password, String database) {
        this.driver = driver;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = database;
    }

    public ConnectionFactoryOptions getConnectionFactoryOptions() {
        return ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, driver)
                .option(ConnectionFactoryOptions.HOST, host)
                .option(ConnectionFactoryOptions.PORT, port)
                .option(ConnectionFactoryOptions.USER, user)
                .option(ConnectionFactoryOptions.PASSWORD, password)
                .option(ConnectionFactoryOptions.DATABASE, database)
                .build();
    }
}
