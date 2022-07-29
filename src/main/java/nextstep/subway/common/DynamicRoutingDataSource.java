package nextstep.subway.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {
    private static final String MASTER = "master";
    private static final String SLAVE = "slave";

    public DynamicRoutingDataSource(DataSource masterDataSource, DataSource slaveDataSource) {
        Map<Object, Object> datasourceMap = new ConcurrentHashMap<>();
        datasourceMap.put(MASTER, masterDataSource);
        datasourceMap.put(SLAVE, slaveDataSource);
        setTargetDataSources(datasourceMap);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            log.debug("connection: {}", SLAVE);
            return SLAVE;
        }
        log.debug("connection: {}", MASTER);
        return MASTER;
    }
}
