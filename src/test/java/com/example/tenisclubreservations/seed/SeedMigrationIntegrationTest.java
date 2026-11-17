package com.example.tenisclubreservations.seed;

import static org.junit.jupiter.api.Assertions.*;

import com.example.tenisclubreservations.TenisClubReservationsApplication;
import java.sql.DriverManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class SeedMigrationIntegrationTest {
  private final String url = "jdbc:h2:mem:seed" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1";

  private ConfigurableApplicationContext start(String... extra) {
    var args = new java.util.ArrayList<String>();
    args.add("--spring.datasource.url=" + url);
    args.addAll(java.util.List.of(extra));
    return new SpringApplicationBuilder(TenisClubReservationsApplication.class)
        .web(WebApplicationType.NONE).run(args.toArray(String[]::new));
  }

  private int count(String sql) throws Exception {
    try (var connection = DriverManager.getConnection(url, "sa", "");
        var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
      result.next();
      return result.getInt(1);
    }
  }

  private void execute(String sql) throws Exception {
    try (var connection = DriverManager.getConnection(url, "sa", "");
        var statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    }
  }

  @Test
  void disabledThenEnabledThenDisabledAgain() throws Exception {
    try (var ignored = start()) {
      assertEquals(0, count("SELECT COUNT(*) FROM surface"));
      assertEquals(0, count("SELECT COUNT(*) FROM court"));
      assertEquals(0, count("SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID='2-seed-courts-surfaces'"));
    }
    try (var ignored = start("--SEED_DATA_ENABLED=false")) {
      assertEquals(0, count("SELECT COUNT(*) FROM surface"));
    }
    try (var ignored = start("--SEED_DATA_ENABLED=true")) {
      assertEquals(2, count("SELECT COUNT(*) FROM surface WHERE removed_at IS NULL AND created_at IS NOT NULL AND updated_at IS NOT NULL AND ((name='Clay' AND active_name='clay' AND price_per_minute=3.60) OR (name='Hard' AND active_name='hard' AND price_per_minute=5.00))"));
      assertEquals(4, count("SELECT COUNT(*) FROM court c JOIN surface s ON c.surface_id=s.id WHERE c.created_at IS NOT NULL AND c.updated_at IS NOT NULL AND c.removed_at IS NULL AND c.active_number=c.court_number AND ((c.court_number IN (1,2) AND s.active_name='clay') OR (c.court_number IN (3,4) AND s.active_name='hard'))"));
      assertEquals(1, count("SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID='2-seed-courts-surfaces'"));
    }
    try (var ignored = start("--SEED_DATA_ENABLED=true")) {
      assertEquals(4, count("SELECT COUNT(*) FROM court"));
    }
    try (var ignored = start("--SEED_DATA_ENABLED=false")) {
      assertEquals(2, count("SELECT COUNT(*) FROM surface"));
      assertEquals(4, count("SELECT COUNT(*) FROM court"));
    }
  }

  @Test
  void conflictsRollbackAndRetry() throws Exception {
    try (var ignored = start()) { }
    execute("INSERT INTO surface(created_at,updated_at,name,active_name,price_per_minute) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'Other','other',1)");
    execute("INSERT INTO court(created_at,updated_at,court_number,active_number,surface_id) SELECT CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,4,4,id FROM surface WHERE active_name='other'");
    assertThrows(Exception.class, () -> start("--SEED_DATA_ENABLED=true"));
    assertEquals(1, count("SELECT COUNT(*) FROM surface"));
    assertEquals(1, count("SELECT COUNT(*) FROM court"));
    assertEquals(0, count("SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID='2-seed-courts-surfaces'"));
    execute("UPDATE court SET removed_at=CURRENT_TIMESTAMP, active_number=NULL");
    execute("INSERT INTO surface(created_at,updated_at,name,active_name,price_per_minute) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'CLAY','clay',1)");
    assertThrows(Exception.class, () -> start("--SEED_DATA_ENABLED=true"));
    assertEquals(2, count("SELECT COUNT(*) FROM surface"));
    assertEquals(1, count("SELECT COUNT(*) FROM court"));
    assertEquals(0, count("SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID='2-seed-courts-surfaces'"));
    execute("UPDATE surface SET removed_at=CURRENT_TIMESTAMP, active_name=NULL WHERE active_name='clay'");
    try (var ignored = start("--SEED_DATA_ENABLED=true")) {
      assertEquals(2, count("SELECT COUNT(*) FROM surface WHERE active_name IN ('clay','hard')"));
      assertEquals(4, count("SELECT COUNT(*) FROM court WHERE active_number IN (1,2,3,4)"));
    }
  }
}
