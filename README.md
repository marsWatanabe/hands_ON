# Inventrack（ドローン部品・製品管理システム）

ドローンの部品と製品の在庫管理を効率化し、在庫センターの運営を最適化するシステム。
手順の基準は原典プロジェクト（`ai-driven-dev-course-MRS`）の README とし、このリポジトリの現状に合わせて記載している。

作業を引き継ぐ場合は、まず [HANDOFF.md](HANDOFF.md)（引継ぎ指示書、残作業、保留事項）を読む。

## プロジェクト構造概要

```
hands_ON/
├── backend/                 # Spring Boot バックエンド
│   ├── src/main/java/       # Java ソース（com.aojt.mgmt）
│   │   ├── config/ controller/{auth,user,center,category,stock}/ service/{auth,jwt,center,category,stock,common}/
│   │   ├── repository/{user,center,category,stock,common}/ model/ dto/{auth,user,center,category,stock,common}/ exception/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/
│   │       ├── migration/   # Flyway マイグレーション（スキーマ。全環境）
│   │       ├── seed-dev/    # 開発・テスト用の最小の初期データ（センター 3 件）
│   │       └── seed-demo/   # 動作確認用のデモデータ（センターを 22 件に増やす。開発のみ、テストは読み込まない）
│   └── pom.xml
├── docs/                    # 設計書
│   ├── requirements.md              # 要件定義書
│   ├── er-diagram.md                # ER 図・テーブル設計
│   ├── detailed-design.md           # 詳細設計書（在庫センター API）
│   ├── frontend-centers-design.md   # フロントエンド設計（在庫センター管理画面）
│   ├── non-functional-requirements.md  # 非機能要件
│   ├── screen-spec/                 # 画面仕様書（Excel、画面キャプチャ付き）
│   └── review/                      # レビュー報告書、動作確認レポート
├── .github/                 # Copilot 指示、CI、Issue テンプレート
└── frontend/                # Next.js フロントエンド（在庫センター管理画面、ログイン・登録）
    ├── app/                 # ページ、コンポーネント、hooks、lib（API クライアント、検証）、types
    └── tests/               # unit（ブラウザ不要）と e2e（Chromium）
```

## 技術スタック

- Backend: Java 21、Spring Boot 3.5、Spring Security（JWT）、Spring Data JPA、Flyway、H2（開発）、PostgreSQL（本番想定）、SpringDoc（OpenAPI）、JUnit 5
- Frontend: Next.js 15、React 19、TypeScript、Chakra UI、Zod、Playwright

## 開発を始める前に

- **JDK 21 以上**が必要。`java -version` で確認する（この環境では JDK 26 でもビルドと起動を確認済み。ビルドは Java 21 向け）。
- フロントエンドには Node.js 20 以上が必要（確認済み: Node.js 24、npm 11）。

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

在庫センター管理、部品在庫一覧、製品在庫一覧、カテゴリ管理（いずれも一覧、検索、並び替え、登録、編集、削除）と、ログイン・アカウント登録。設計は [docs/frontend-centers-design.md](docs/frontend-centers-design.md)。

1. **先にバックエンドを起動する**（上の Backend の手順 4）。
2. `cd frontend` で frontend ディレクトリに移動する。
3. `.env.sample` を `.env.local` にコピーする（`BACKEND_API_URL` は既定で `http://localhost:8080`）。
4. `npm install`（ロックファイルどおりに入れるなら `npm ci`）。
5. `npm run dev` で開発サーバーを起動する。
6. http://localhost:3000 を開く。「新規登録」でユーザーを作るか、登録済みのユーザーでログインする。

```bash
$ cd frontend
$ cp .env.sample .env.local     # PowerShell: Copy-Item .env.sample .env.local
$ npm install
$ npm run dev
```

この画面はバックエンドを直接呼ばず、同じオリジン（`/api/...`）を Next.js がバックエンドへ転送する。バックエンドを再起動したら、トークンの署名鍵が変わるため、ログインし直す。

ビルドと Lint:

```bash
$ npm run lint
$ npm run typecheck
$ npm run build
```

## ログイン方法

1. バックエンドを起動する。
2. Swagger UI の `POST /api/v1/auth/register` でユーザーを登録する。レスポンスの `accessToken` が得られる。
3. Swagger UI 右上の **Authorize** に `accessToken` だけを貼り付ける（`Bearer ` は付けない）。
4. `GET /api/v1/centers` などの認証が必要な API を実行できる。

トークンは起動のたびに作り直される鍵で署名される（開発用）。再起動したら、ログインし直す。

## 動作確認用の画面（開発用）

ブラウザで API を試せる簡易画面。バックエンドを起動し、http://localhost:8080/ を開く（`/dev-ui/index.html` へ転送される）。

- できること: ユーザー登録・ログイン・トークン更新、在庫センターの一覧（検索、並び替え、ページング）、新規登録・編集・削除、エラー確認用の入力例（重複、文字数超過、形式不正、容量超過）、通信ログ（ステータスと本文。トークンとパスワードは表示しない）。
- ファイル: `backend/src/main/resources/static/dev-ui/`（HTML、CSS、JavaScript。フレームワークなし）。
- 公開範囲: `app.dev-ui.enabled=true`（`application.yml` の既定）のときだけ未認証で開ける。本番では `false` にする（認証が必要になり、開けなくなる）。API の認証は変わらない。
- 注意: これは開発用の確認画面。業務用の画面は `frontend/`（Next.js）。

## DB マイグレーション

H2 Database を使用して Flyway でマイグレーションを管理する。スキーマは Flyway だけが変更し、Hibernate は `ddl-auto=validate` でエンティティとの一致を検証するだけである。

1. H2 Database の Web コンソール http://localhost:8080/h2-console にアクセスする。
   - ユーザー名とパスワードは `application.yml` を確認する。
   - JDBC URL は `jdbc:h2:file:./target/testdb;DB_CLOSE_DELAY=-1` を設定する。
2. マイグレーションファイルは `backend/src/main/resources/db/migration/` に作成する。
   - 命名規則は `V1__initial.sql` のように、バージョン番号と説明を含める（現在は `V1__initial.sql`）。
   - 開発・テスト用の初期データは `db/seed-dev/`（現在は `V2__seed_dev_data.sql`）に置く。動作確認用のデモデータ（センター 22 件と紐づく在庫）は `db/seed-demo/`（`V3__seed_demo_data.sql`。開発だけで読み込み、テストは読み込まない）。本番では `spring.flyway.locations` を `classpath:db/migration` だけにして読み込まない。
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

JUnit を使用してテストを実行する。テストはプロファイル `test`（テストごとに独立したインメモリ H2）で動く。Controller、Service、Repository、Config の層ごとのユニットテストと、結合テスト（同時リクエストのテストを含む）、DB スキーマテストがある（計 300 件）。

```bash
$ ./mvnw test
```

カバレッジ（命令カバレッジ 80% 以上。現在 97.3%）を検査する場合は、プロファイル `coverage` を付ける。レポートは `target/site/jacoco/index.html`。CI（GitHub Actions）では毎回これを実行する。

```bash
$ ./mvnw verify -Pcoverage
```

**注意**: JaCoCo は、プロジェクトのパスに日本語などの非 ASCII 文字（ユーザー名を含む）がある Windows では、JVM がエージェントの引数を文字化けさせて失敗する。その場合は、ASCII のドライブに割り当てて実行する。

```powershell
subst X: C:\Users\<ユーザー名>\Documents\hands_ON
cd X:\backend
.\mvnw.cmd verify -Pcoverage
subst X: /D
```

### Frontend

Playwright Test を使う。初回だけ、E2E 用のブラウザを入れる（`npx playwright install chromium`）。

```bash
$ npm run test:unit    # 単体テスト（ブラウザとバックエンドは不要。CI でも実行）
$ npm run test:e2e     # E2E（Chromium）。バックエンドを起動してから実行する
```

- `test:e2e` は開発サーバー（http://localhost:3000）を自動で起動する（起動済みならそれを使う）。バックエンド（既定 http://localhost:8080）は自動では起動しない。
- E2E はテストごとに新しいユーザーと一意なセンター名を作り、作ったセンターは削除する。ユーザーは開発用 DB に残る（DB を作り直すには、「DB マイグレーション」の手順）。
- 単体テスト 109 件、E2E 46 件。

## ドキュメント

設計書は `docs/` を参照。

- **`docs/requirements.md`**: 要件定義書
- **`docs/er-diagram.md`**: ER 図・テーブル設計
- **`docs/detailed-design.md`**: 詳細設計書（在庫センター API）
- **`docs/frontend-centers-design.md`**: フロントエンド設計（在庫センター、カテゴリ、部品在庫、製品在庫の画面）
- **`docs/detailed-design-stocks.md`**: 詳細設計書（カテゴリ・部品在庫・製品在庫 API）
- **`docs/non-functional-requirements.md`**: 非機能要件
- **`docs/review/backend-review-centers.md`**: レビュー報告書（バックエンド）
- **`docs/review/frontend-review-centers.md`**: レビュー報告書（フロントエンド）
- **`docs/review/frontend-verification-report-20261028.md`**: 動作確認レポート（フロントエンド）
- **`docs/screen-spec/Inventrack_screen-spec.xlsx`**: 画面仕様書（Excel。実ブラウザの通し操作のキャプチャ付き）

原典の `docs/api-interface-spec.md`、`docs/screen-specifications.md`、`docs/table-definitions.md` は、このリポジトリにはまだ取り込んでいない（原典の zip を参照）。
