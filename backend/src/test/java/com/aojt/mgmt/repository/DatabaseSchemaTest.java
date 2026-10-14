package com.aojt.mgmt.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aojt.mgmt.model.Center;
import com.aojt.mgmt.repository.center.CenterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the Flyway-managed schema (db/migration/V1, db/seed-dev/V2) on H2:
 * Hibernate validates the entities against it at startup, constraints reject bad data,
 * and the seed data follows docs/table-definitions.md.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class DatabaseSchemaTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CenterRepository centerRepository;

    private int count(String sql) {
        Integer n = jdbc.queryForObject(sql, Integer.class);
        return n == null ? 0 : n;
    }

    private void insertCenter(String name, Object status, Object deleteFlag, Object maxCapacity) {
        jdbc.update("INSERT INTO center_info (center_name, post_code, address, phone_number, manager_name,"
                + " operational_status, delete_flag, max_storage_capacity) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                name, "100-0001", "Tokyo", "03-1234-5678", "Manager", status, deleteFlag, maxCapacity);
    }

    // ---- seed data ----

    @Test
    void seedDataIsLoaded() {
        assertEquals(3, count("SELECT COUNT(*) FROM center_info"));
        assertEquals(16, count("SELECT COUNT(*) FROM category_info"));
        assertEquals(20, count("SELECT COUNT(*) FROM parts_stock_info"));
        assertEquals(12, count("SELECT COUNT(*) FROM products_stock_info"));
    }

    @Test
    void seedDataFollowsTableDefinitions() {
        assertEquals(10, count("SELECT COUNT(*) FROM category_info WHERE category_type = 'part'"));
        assertEquals(6, count("SELECT COUNT(*) FROM category_info WHERE category_type = 'product'"));
        assertEquals(3, count("SELECT COUNT(*) FROM center_info WHERE operational_status = 0"));
        assertEquals(0, count("SELECT COUNT(*) FROM parts_stock_info p JOIN category_info c"
                + " ON p.category_id = c.category_id WHERE c.category_type <> 'part'"));
        assertEquals(0, count("SELECT COUNT(*) FROM products_stock_info p JOIN category_info c"
                + " ON p.category_id = c.category_id WHERE c.category_type <> 'product'"));
    }

    // ---- constraints: center_info ----

    @Test
    void centerNameMustBeUnique() {
        assertThrows(DataIntegrityViolationException.class, () -> insertCenter("東京センター", 0, 0, null));
    }

    @Test
    void centerNameLongerThan20CharactersIsRejected() {
        assertThrows(DataIntegrityViolationException.class, () -> insertCenter("a".repeat(21), 0, 0, null));
    }

    @Test
    void centerRejectsInvalidStatusAndFlag() {
        assertThrows(DataIntegrityViolationException.class, () -> insertCenter("S1", 2, 0, null));
        assertThrows(DataIntegrityViolationException.class, () -> insertCenter("S2", 0, 2, null));
    }

    @Test
    void centerRejectsNegativeCapacityButAcceptsNull() {
        assertThrows(DataIntegrityViolationException.class, () -> insertCenter("C1", 0, 0, -1));
        insertCenter("C2", 0, 0, null);
        insertCenter("C3", 0, 0, 0);
        assertTrue(centerRepository.existsByCenterName("C2"));
    }

    @Test
    void centerRequiresMandatoryColumns() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO center_info (center_name, post_code, address, phone_number)"
                        + " VALUES ('N1', '100-0001', 'Tokyo', '03-1234-5678')"));
    }

    // ---- constraints: other tables ----

    @Test
    void stockRequiresExistingCenterAndCategory() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO parts_stock_info (name, center_id, category_id, amount) VALUES ('x', 9999, 1, 1)"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO products_stock_info (name, center_id, category_id, amount) VALUES ('x', 1, 9999, 1)"));
    }

    @Test
    void stockAmountMustNotBeNegative() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO parts_stock_info (name, center_id, category_id, amount) VALUES ('x', 1, 1, -1)"));
    }

    @Test
    void categoryTypeIsRestricted() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO category_info (category_type, category_name, delete_flag) VALUES ('PARTS', 'x', 0)"));
    }

    @Test
    void userEmailIsUniqueAndRoleIsRestricted() {
        jdbc.update("INSERT INTO _user (firstname, lastname, email, password, role)"
                + " VALUES ('a', 'b', 'a@example.com', 'hash', 'USER')");
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO _user (firstname, lastname, email, password, role)"
                        + " VALUES ('c', 'd', 'a@example.com', 'hash', 'USER')"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO _user (firstname, lastname, email, password, role)"
                        + " VALUES ('c', 'd', 'b@example.com', 'hash', 'ROLE_USER')"));
    }

    // ---- repository ----

    @Test
    void repositoryFindsOnlyNotDeletedCenters() {
        insertCenter("Gone", 0, 1, null);
        Integer deletedId = jdbc.queryForObject(
                "SELECT center_id FROM center_info WHERE center_name = 'Gone'", Integer.class);
        assertFalse(centerRepository.findByCenterIdAndDeleteFlag(deletedId, Center.NOT_DELETED).isPresent());
        assertTrue(centerRepository.findByCenterIdAndDeleteFlag(deletedId, Center.DELETED).isPresent());
        // The unique constraint still covers logically deleted rows (requirements.md Q-5 is undecided).
        assertTrue(centerRepository.existsByCenterName("Gone"));
    }
}
