package com.berkayd06.query_logger.config;

import com.berkayd06.query_logger.core.QueryLoggingDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "querylogger", name = "manual-config-example", havingValue = "true", matchIfMissing = false)
public class QueryLoggerManualConfiguration {

    @Bean(name = "actualDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource actualDataSource() {
        return DataSourceBuilder.create().build();
    }
    @Bean
    @Primary
    public DataSource dataSource(
            @Qualifier("actualDataSource") DataSource actualDataSource,
            QueryLoggerProperties properties) {
        return new QueryLoggingDataSource(actualDataSource, properties);
    }
}
