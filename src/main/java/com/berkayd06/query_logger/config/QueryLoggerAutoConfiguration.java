package com.berkayd06.query_logger.config;

import com.berkayd06.query_logger.core.QueryLoggingDataSource;
import com.berkayd06.query_logger.inspector.HibernateSqlCapturingInspector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(QueryLoggerProperties.class)
@ConditionalOnProperty(prefix = "querylogger", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(name = {
    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
public class QueryLoggerAutoConfiguration {

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnProperty(prefix = "querylogger", name = "wrapDataSource", havingValue = "true", matchIfMissing = true)
    public static BeanPostProcessor queryLoggingDataSourcePostProcessor(QueryLoggerProperties props) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSource && !(bean instanceof QueryLoggingDataSource)) {
                    return new QueryLoggingDataSource((DataSource) bean, props);
                }
                return bean;
            }
        };
    }

    @Bean
    @ConditionalOnClass(name = "org.hibernate.resource.jdbc.spi.StatementInspector")
    public HibernateSqlCapturingInspector statementInspector(QueryLoggerProperties props) {
        return new HibernateSqlCapturingInspector(props);
    }
}