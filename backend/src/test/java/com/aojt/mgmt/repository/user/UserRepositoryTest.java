package com.aojt.mgmt.repository.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aojt.mgmt.model.Role;
import com.aojt.mgmt.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * {@link UserRepository} のテスト。H2 で、保存、メールアドレス検索、一意制約、ロールの保存を確認する。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    UserRepository repository;

    private User save(String email, Role role) {
        return repository.saveAndFlush(new User("太郎", "山田", email, "hash", role));
    }

    /** 保存すると ID が採番され、メールアドレスで取得できること。 */
    @Test
    void save_assignsIdAndFindByEmailReturnsTheUser() {
        User saved = save("taro@example.com", Role.USER);

        assertNotNull(saved.getId());
        User found = repository.findByEmail("taro@example.com").orElseThrow();
        assertEquals("山田", found.getLastname());
        assertEquals(Role.USER, found.getRole());
    }

    /** ロールが文字列（USER・ADMIN）で保存され、復元できること。 */
    @Test
    void save_persistsAdminRole() {
        save("admin@example.com", Role.ADMIN);

        assertEquals(Role.ADMIN, repository.findByEmail("admin@example.com").orElseThrow().getRole());
    }

    /** 存在しないメールアドレスは、空で返ること。 */
    @Test
    void findByEmail_returnsEmptyForUnknownEmail() {
        assertFalse(repository.findByEmail("none@example.com").isPresent());
    }

    /** メールアドレスの登録有無を確認できること。 */
    @Test
    void existsByEmail_reflectsRegistration() {
        save("taro@example.com", Role.USER);

        assertTrue(repository.existsByEmail("taro@example.com"));
        assertFalse(repository.existsByEmail("other@example.com"));
    }

    /** 同じメールアドレスは保存できず、制約違反になること。 */
    @Test
    void save_rejectsDuplicateEmail() {
        save("taro@example.com", Role.USER);

        assertThrows(DataIntegrityViolationException.class, () -> save("taro@example.com", Role.USER));
    }
}
