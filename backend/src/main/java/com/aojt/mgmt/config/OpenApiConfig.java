package com.aojt.mgmt.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI（Swagger UI）の設定。
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";
    private static final String AUTH_CONTROLLER_PREFIX_1 = "Authentication";
    private static final String AUTH_CONTROLLER_PREFIX_2 = "TokenRefresh";
    private static final String CENTER_CONTROLLER_PREFIX = "Center";

    /**
     * API の基本情報と、Bearer トークンの認証方式（Authorize ボタン）を定義する。
     *
     * @return OpenAPI 定義
     */
    @Bean
    OpenAPI inventrackOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Inventrack API").version("v1"))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME));
    }

    /**
     * 各 API に、エラー時のステータスの説明を追加する（Swagger UI で「Undocumented」と表示されないようにする）。
     *
     * @return オペレーションのカスタマイザー
     */
    @Bean
    OperationCustomizer errorResponses() {
        return (operation, handlerMethod) -> {
            String controller = handlerMethod.getBeanType().getSimpleName();
            boolean auth = controller.startsWith(AUTH_CONTROLLER_PREFIX_1)
                    || controller.startsWith(AUTH_CONTROLLER_PREFIX_2);
            add(operation, "400", "入力エラー・業務ルール違反（本文の error.message を参照）");
            add(operation, "401", "未認証、トークンが無効、または認証情報が誤り");
            if (!auth) {
                add(operation, "403", "権限がありません");
                add(operation, "404", "リソースが見つかりません");
            }
            if (auth || controller.startsWith(CENTER_CONTROLLER_PREFIX)) {
                add(operation, "409", "重複（在庫センター名・メールアドレス）");
            }
            return operation;
        };
    }

    private static void add(Operation operation, String status, String description) {
        if (operation.getResponses().get(status) == null) {
            operation.getResponses().addApiResponse(status, new ApiResponse().description(description));
        }
    }
}
