package com.aojt.mgmt;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 動作確認用の画面（{@code /dev-ui/}）の公開範囲のテスト。
 * {@code app.dev-ui.enabled=true} のときだけ未認証で開け、{@code false} なら認証が必要になること、
 * どちらの場合も API の認証は変わらないことを確認する。
 */
class DevUiAccessTest {

    /** 有効（開発の既定）のとき。 */
    @Nested
    @SpringBootTest(properties = "app.dev-ui.enabled=true")
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    class WhenEnabled {

        @Autowired
        MockMvc mvc;

        /** 画面（HTML、スクリプト、CSS）を未認証で取得できること。 */
        @Test
        void devUiIsServedWithoutAuthentication() throws Exception {
            mvc.perform(get("/dev-ui/index.html")).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Inventrack")));
            mvc.perform(get("/dev-ui/app.js")).andExpect(status().isOk());
            mvc.perform(get("/dev-ui/style.css")).andExpect(status().isOk());
            mvc.perform(get("/index.html")).andExpect(status().isOk());
        }

        /** 画面を開けても、API は未認証なら 401 のままであること。 */
        @Test
        void apiStillRequiresAuthentication() throws Exception {
            mvc.perform(get("/api/v1/centers")).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("AUTH_ERROR"));
        }

        /** 画面のスクリプトが、HTML として解釈される書き方（innerHTML など）を使っていないこと（XSS 対策）。 */
        @Test
        void scriptDoesNotInjectHtml() throws Exception {
            String js = mvc.perform(get("/dev-ui/app.js")).andReturn().getResponse().getContentAsString();
            assertTrue(!js.contains("innerHTML") && !js.contains("outerHTML") && !js.contains("insertAdjacentHTML")
                    && !js.contains("document.write") && !js.contains("eval("));
        }
    }

    /** 無効（本番）のとき。 */
    @Nested
    @SpringBootTest(properties = "app.dev-ui.enabled=false")
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    class WhenDisabled {

        @Autowired
        MockMvc mvc;

        /** 画面は認証が必要になり、未認証では取得できない（401）こと。 */
        @Test
        void devUiRequiresAuthentication() throws Exception {
            mvc.perform(get("/dev-ui/index.html")).andExpect(status().isUnauthorized());
            mvc.perform(get("/dev-ui/app.js")).andExpect(status().isUnauthorized());
            mvc.perform(get("/index.html")).andExpect(status().isUnauthorized());
        }
    }
}
