# Inventrack（ドローン部品・製品管理システム）

ドローンの部品と製品の在庫管理を効率化し、在庫センターの運営を最適化するシステム。
手順の基準は原典プロジェクト（`ai-driven-dev-course-MRS`）の README とし、このリポジトリの現状に合わせて記載している。

## プロジェクト構造概要

```
hands_ON/
├── backend/                 # Spring Boot バックエンド
│   ├── src/main/java/       # Java ソース（com.aojt.mgmt）
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/
│   │       ├── migration/   # Flyway マイグレーション（スキーマ。全環境）
│   │       └── seed-dev/    # 開発・テスト用の初期データ
│   └── pom.xml
├── docs/                    # 設計書
│   ├── requirements.md              # 要件定義書
│   ├── er-diagram.md                # ER 図・テーブル設計
│   ├── detailed-design.md           # 詳細設計書（在庫センター API）
│   ├── non-functional-requirements.md  # 非機能要件
│   └── review/                      # レビュー報告書
└── frontend/                # Next.js フロントエンド（未作成）
```

## 技術スタック

- Backend: Java 21、Spring Boot 3.5、Spring Security（JWT）、Spring Data JPA、Flyway、H2（開発）、PostgreSQL（本番想定）、SpringDoc（OpenAPI）、JUnit 5
- Frontend: Next.js、TypeScript、Chakra UI、Playwright（未作成）

## 開発を始める前に

- **JDK 21 以上**が必要。`java -version` で確認する（この環境では JDK 26 でもビルドと起動を確認済み。ビルドは Java 21 向け）。
- フロントエンドを作る際は Node.js 20 以上が必要。

## 環境構築

### Backend

1. `cd backend` で backend ディレクトリに移動する。
2. `java -version` で Java のバージョンを確認する。
3. `./mvnw clean package` でビルドする（テストも実行される）。
4. `./mvnw spring-boot:run` で実行する。
5. http://localhost:8080/swagger-ui/index.html にアクセスすると、Swagger UI で API ドキュメントを確認できる。

Windows の PowerShell ではスクリプトの前に `.\` を付け、`.\mvnw.cmd` を使う。

```powershell
cd backend
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

### Frontend

未作成。

## ログイン方法

1. バックエンドを起動する。
2. Swagger UI の `POST /api/v1/auth/register` でユーザーを登録する。レスポンスの `accessToken` が得られる。
3. Swagger UI 右上の **Authorize** に `accessToken` だけを貼り付ける（`Bearer ` は付けない）。
4. `GET /api/v1/centers` などの認証が必要な API を実行できる。

トークンは起動のたびに作り直される鍵で署名される（開発用）。再起動したら、ログインし直す。

## DB マイグレーション

H2 Database を使用して Flyway でマイグレーションを管理する。スキーマは Flyway だけが変更し、Hibernate は `ddl-auto=validate` でエンティティとの一致を検証するだけである。

1. H2 Database の Web コンソール http://localhost:8080/h2-console にアクセスする。
   - ユーザー名とパスワードは `application.yml` を確認する。
   - JDBC URL は `jdbc:h2:file:./target/testdb;DB_CLOSE_DELAY=-1` を設定する。
2. マイグレーションファイルは `backend/src/main/resources/db/migration/` に作成する。
   - 命名規則は `V1__initial.sql` のように、バージョン番号と説明を含める（現在は `V1__initial.sql`）。
   - 開発・テスト用の初期データは `db/seed-dev/`（現在は `V2__seed_dev_data.sql`）に置く。本番では `spring.flyway.locations` を `classpath:db/migration` だけにして読み込まない。
3. アプリ起動時に自動でスキーマが適用される。

**備考**

本コースでは、開発環境として H2 Database を利用しているが、実際の運用環境（AWS や Azure 上）では PostgreSQL などの RDBMS を使用する。
そのため、`application.yml` でデータベースを切り替え、スキーマ変更時には migrate コマンドを直接実行してマイグレーションを適用する。

```bash
$ ./mvnw flyway:info
$ ./mvnw flyway:migrate
```

`flyway:info` と `flyway:migrate` は `./target/testdb`（H2 のファイル DB）を対象にする。H2 のファイル DB は 1 プロセスからしか開けないため、**アプリを停止してから**実行する。

開発用 DB を作り直すときは、アプリを停止して `backend/target/testdb.mv.db` を削除する。

## テスト実行

### Backend

JUnit を使用してテストを実行する。テストはプロファイル `test`（テストごとに独立したインメモリ H2）で動く。

```bash
$ ./mvnw test
```

### Frontend

未作成。

## ドキュメント

設計書は `docs/` を参照。

- **`docs/requirements.md`**: 要件定義書
- **`docs/er-diagram.md`**: ER 図・テーブル設計
- **`docs/detailed-design.md`**: 詳細設計書（在庫センター API）
- **`docs/non-functional-requirements.md`**: 非機能要件
- **`docs/review/backend-review-centers.md`**: レビュー報告書

原典の `docs/api-interface-spec.md`、`docs/screen-specifications.md`、`docs/table-definitions.md` は、このリポジトリにはまだ取り込んでいない（原典の zip を参照）。
