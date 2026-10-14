package com.aojt.mgmt.controller.user;

import com.aojt.mgmt.dto.user.UserResponse;
import com.aojt.mgmt.exception.NotFoundException;
import com.aojt.mgmt.model.Role;
import com.aojt.mgmt.model.User;
import com.aojt.mgmt.repository.user.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ユーザー API。
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "ユーザー関連 API")
public class UserController {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String MSG_NOT_FOUND = "ユーザーが見つかりません。ID: ";
    private static final String MSG_ACCESS_DENIED = "他のユーザーの情報は参照できません。";

    private final UserRepository userRepository;

    /**
     * コントローラーを生成する。
     *
     * @param userRepository ユーザーのリポジトリ
     */
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * ユーザー情報を返す。パスワードは含めない。管理者以外は、本人の情報だけ参照できる。
     *
     * @param id ユーザーID
     * @param authentication 認証情報
     * @return ユーザー情報
     * @throws NotFoundException ユーザーが存在しない場合
     * @throws AccessDeniedException 管理者以外が他のユーザーを参照した場合
     */
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Integer id, Authentication authentication) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(MSG_NOT_FOUND + id));
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(ROLE_PREFIX + Role.ADMIN.name()));
        if (!admin && !user.getEmail().equals(authentication.getName())) {
            throw new AccessDeniedException(MSG_ACCESS_DENIED);
        }
        return UserResponse.from(user);
    }
}
