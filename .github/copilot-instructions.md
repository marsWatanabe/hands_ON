# Copilot コーディングエージェント向け指示（hands_ON / Inventrack）

このリポジトリで、コード生成・編集・PR 作成を行うときに必ず従うルール。
最初に `HANDOFF.md` を読み、「7. 残作業」から Issue に書かれた作業だけを行う。

## プロジェクト

- ドローン部品・製品管理システム Inventrack。バックエンドは `backend/`（Java 21、Spring Boot 3.5、Spring Security（JWT）、Spring Data JPA、Flyway、H2、SpringDoc）。
- フロントエンド（`frontend/`）は未作成。Issue で指示されるまで作らない。
- 設計書は `docs/`。作業前に、関連する設計書を読む。

## ビルド・テスト（変更後に必ず実行する）

```bash
cd backend
./mvnw -B test
```

- テストが通らない変更は PR にしない。
- `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` は削除しない。
- アプリの起動確認が必要なときは `./mvnw spring-boot:run`（http://localhost:8080/swagger-ui/index.html）。

## 参照元の優先順位

1. `docs/er-diagram.md` と `backend/src/main/resources/db/migration/`（DB の定義）
2. `docs/detailed-design.md`（API の詳細設計）
3. `docs/requirements.md`（要件と業務ルール）
4. コード

不一致を見つけたら、**ドキュメントを先に直してから**実装を合わせる。原典との食い違いは `docs/requirements.md` 第 7 章に追記する。

## 承認が必要な変更（Issue に明記されていなければ行わない）

- 新規の依存ライブラリの追加（`pom.xml`）
- DB スキーマの変更（Flyway のマイグレーションの追加・変更）
- API 契約の変更（エンドポイント、リクエスト・レスポンスの形）
- `HANDOFF.md` の「7.4 保留中」の項目の実装（アクセストークンの有効期限、自己登録の可否、権限差、削除ルール、楽観ロックなど）

迷ったら実装せず、PR の説明か Issue のコメントに質問を書く。

## コーディング規約

- Java: Google Java Style。クラスとメソッドには日本語の Javadoc（`@param`、`@return`、必要なら `@throws`）を付ける。複雑なロジックには日本語のコメントを付ける。
- 構成は Controller、Service、Repository の MVC。依存はコンストラクタインジェクション。
- Controller は認証・認可、`@Valid` による入力検証、Service への委譲。Service は業務ロジックと `@Transactional`。Repository は Spring Data JPA。
- 独自例外を使い、`GlobalExceptionHandler` で共通のエラー形式 `{ "error": { "code", "message", "timestamp" } }` に変換する。
- ハードコーディング禁止。定数と enum を使う。
- パッケージは、ドメイン単位（`controller/center` のように）に整理する方針（`HANDOFF.md` 7.1）。新規のクラスはこの構成で作る。
- SQL を文字列連結で組み立てない（JPA のパラメータ化クエリ、`Specification`）。ソート項目は許可リストで検証する。
- 個人情報（氏名、メール、電話番号）、パスワード、トークンをログに出さない。秘密情報をコードやドキュメントにハードコードしない（リポジトリは public）。
- 削除は論理削除（`delete_flag`）。

## テスト規約

- Controller: `MockMvc` と `@WithMockUser`。認証・認可・検証・異常系を網羅する。
- Service: JUnit 5 と Mockito（`@Mock`、`@InjectMocks`）。正常系、異常系、境界値。
- Repository: H2。正常、異常、境界値、制約違反。
- テストメソッド名は期待動作を表す。テストクラスに目的、メソッドに検証内容の Javadoc を書く。
- テストは `test` プロファイル（`application-test.yml`、テストごとに独立したインメモリ DB）で動かす。

## DB

- スキーマの変更は Flyway だけで行う。Hibernate の `ddl-auto` は `validate` のまま。
- 型は H2 と PostgreSQL の両方で動くものにする（`TINYINT` は使わず `SMALLINT`、`DATETIME` は使わず `TIMESTAMP`）。
- 開発用データは `db/seed-dev/` に置く。`db/migration/` に入れない。

## PR・Issue・コミット

- PR の説明と Issue のコメントは日本語。PR には「開発内容」「実装方法」「仕様」「確認事項」と、テスト結果を書く。
- コミットメッセージは英語（短い命令形）。
- PR の宛先は `main`。1 つの PR で 1 つの Issue だけを扱う。
- 作業が `HANDOFF.md` の残作業に当たるときは、PR で該当の項目を更新する（チェックを付ける）。
