package com.aojt.mgmt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Inventrack（ドローン部品・製品管理システム）のバックエンドの起動クラス。
 */
@SpringBootApplication
public class InventrackApplication {

    /**
     * アプリケーションを起動する。
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        SpringApplication.run(InventrackApplication.class, args);
    }
}
