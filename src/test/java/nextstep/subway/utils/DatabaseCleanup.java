package nextstep.subway.utils;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@ActiveProfiles("test")
public class DatabaseCleanup implements InitializingBean {
    private final R2dbcEntityTemplate template;

    private boolean initProperties = false;
    private List<String> tableNames;

    public DatabaseCleanup(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    public void afterPropertiesSet() {
        tableNames = Optional.ofNullable(template.getDatabaseClient()
                                                 .sql("show tables")
                                                 .fetch()
                                                 .all()
                                                 .collectList()
                                                 .block())
                .orElse(Collections.emptyList())
                .stream()
                .map(stringObjectMap -> (String) stringObjectMap.get("TABLE_NAME"))
                .collect(Collectors.toList());
        initProperties = !tableNames.isEmpty();
    }

    public boolean isInitProperties() {
        return initProperties;
    }

    public void execute() {
        template.getDatabaseClient()
                .sql("set referential_integrity false")
                .fetch()
                .rowsUpdated()
                .block();

        tableNames.forEach(tableName -> template.getDatabaseClient()
                .sql(String.format("TRUNCATE TABLE %s RESTART IDENTITY", tableName))
                .fetch()
                .rowsUpdated()
                .block());

        template.getDatabaseClient()
                .sql("set referential_integrity true")
                .fetch()
                .rowsUpdated()
                .block();
    }
}
