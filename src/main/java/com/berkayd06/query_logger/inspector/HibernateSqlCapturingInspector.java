package com.berkayd06.query_logger.inspector;

import com.berkayd06.query_logger.config.QueryLoggerProperties;
import com.berkayd06.query_logger.util.QueryOrigin;
import org.hibernate.resource.jdbc.spi.StatementInspector;

public class HibernateSqlCapturingInspector implements StatementInspector {
    private final QueryLoggerProperties props;

    public HibernateSqlCapturingInspector(QueryLoggerProperties props) {
        this.props = props;
    }

    @Override
    public String inspect(String sql) {
        QueryOrigin.markJpa();
        return sql;
    }
}
