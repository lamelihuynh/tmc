package vn.tmc.gatestation.bootstrap;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Gate Store (ADR-14): gate-station's own PostgreSQL database, only with the "gate-store" profile.
 * Migrations live in classpath:db/gate-store and run at start-up. TODO Team A (A-04):
 * JdbcGateConfigRepository over table gates; gate audit through JdbcOutboxStore (C-08).
 */
@Configuration
@Profile("gate-store")
class GateStoreConfig {

  @Bean(destroyMethod = "close")
  DataSource gateStoreDataSource(
      @Value("${tmc.gate.store.url}") String url,
      @Value("${tmc.gate.store.username}") String user,
      @Value("${tmc.gate.store.password}") String password) {
    var ds = new HikariDataSource();
    ds.setJdbcUrl(url);
    ds.setUsername(user);
    ds.setPassword(password);
    ds.setPoolName("gate-store");
    ds.setMaximumPoolSize(5);
    return ds;
  }

  @Bean
  Flyway gateStoreFlyway(DataSource gateStoreDataSource) {
    var flyway =
        Flyway.configure()
            .dataSource(gateStoreDataSource)
            .locations("classpath:db/gate-store")
            .load();
    flyway.migrate();
    return flyway;
  }

  @Bean
  JdbcTemplate gateStoreJdbc(DataSource gateStoreDataSource, Flyway gateStoreFlyway) {
    return new JdbcTemplate(gateStoreDataSource);
  }

  @Bean
  PlatformTransactionManager gateStoreTransactions(DataSource gateStoreDataSource) {
    return new DataSourceTransactionManager(gateStoreDataSource);
  }
}
