package com.preppal;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
        "spring.ai.openai.api-key=test-key",
        "auth.jwt.secret=test-secret-key-with-at-least-32-characters",
        "auth.mock-enabled=true"
})
/**
 * 应用上下文装配冒烟测试。
 *
 * 本测试不依赖 MySQL：排除数据源自动配置，用桩件替代 MyBatis 的 SqlSessionTemplate 与事务管理器，
 * 目的只是验证所有 Bean（Mapper 代理、各模块 Service、拦截器、线程池）能够完成装配。
 * 真实读写行为由各 Service 的单元测试与本地 MySQL 联调覆盖。
 */
class PrepPalApplicationTests {

    @TestConfiguration
    static class PersistenceStubs {

        /**
         * MapperFactoryBean 需要一个 SqlSessionFactory 来注册 Mapper 接口。
         * Configuration 必须带 Environment，否则 SqlSessionTemplate 构造时会取不到 DataSource。
         */
        @Bean
        SqlSessionFactory sqlSessionFactory() {
            Configuration configuration = new Configuration();
            configuration.setEnvironment(new Environment(
                    "preppal-test", new JdbcTransactionFactory(), Mockito.mock(DataSource.class)));
            SqlSessionFactory factory = Mockito.mock(SqlSessionFactory.class);
            when(factory.getConfiguration()).thenReturn(configuration);
            return factory;
        }

        /** MaterialServiceImpl 通过 TransactionTemplate 落库，需要事务管理器存在才能装配 */
        @Bean
        PlatformTransactionManager platformTransactionManager() {
            return Mockito.mock(PlatformTransactionManager.class);
        }
    }

    @Test
    void contextLoads() {
    }

}