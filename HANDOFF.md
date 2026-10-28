# 引継ぎ指示書: hands_ON（Inventrack）

| 項目 | 内容 |
| --- | --- |
| 作成日 | 2026-10-14 |
| リポジトリ | https://github.com/marsWatanabe/hands_ON（public、ブランチは `main` のみ） |
| ローカル | `C:\Users\渡邉天\Documents\hands_ON`（VS Code のワークスペースでは、`seiseki-project-v0.15.0` とは別のルートフォルダ） |
| 目的 | AIDD 開発コース ハンズオン Day1 の課題: 「在庫センター一覧取得 API」を、詳細設計から実装まで AI と進める |
| 最新の状態 | `git log` と `git status` で確認する |

このファイルを最初に読む。次に「4. 読む順序」の資料を読み、「7. 残作業」から着手する。

## 1. 注意（最初に確認）

- **`seiseki-project-v0.15.0` の `AGENTS.md`（継続性の起動手順、保存ルールなど）は、hands_ON には適用しない。** 別プロジェクトである。
- リポジトリは **public**。秘密情報（鍵、パスワード、トークン）を書かない、コミットしない。
- 原典の教材（`ai-driven-dev-course-MRS-main.zip`）の内容を要約した資料が `docs/` に含まれている。教材を公開したくない場合は、リポジトリを private にする（あなたが判断する）。
- 原典の zip は**リポジトリに含めていない**。ローカルの `C:\Users\渡邉天\Downloads\ai-driven-dev-course-MRS-main.zip` を参照する（展開先の `%TEMP%\mrs-src` は消えることがある）。

## 2. 作業の進め方（合意済み）

| 項目 | 内容 |
| --- | --- |
| 修正方針 | 実装を**原典（zip）の文書とコードに合わせる**。原典の優先順位は、1. `docs/table-definitions.md`、2. API 仕様、3. 画面仕様、4. コード。不一致は**ドキュメントを先に直してから**実装を合わせる |
| 承認が必要 | 新規の依存、DB スキーマ変更、API 契約の変更（原典 `copilot-instructions.md`）。あなた（ユーザー）に確認してから行う |
| ブランチ | `main` で進めてよい。PR も `main` 宛てでよい |
| コミットメッセージ | **英語にする**。PowerShell から日本語を渡すと文字化けする |
| 保留 | アクセストークンの有効期限は**今は変更しない**。未決事項（第 7 章の「保留」）は、あなたが決めるまで実装しない |
| 作業指示の出し方 | VS Code のチャットで指示する。Issue に書いた場合も、チャットで「Issue #n を進めて」と伝える（Issue を自動で拾う仕組みは無い） |

## 3. 現在の状態

### 3.1 できていること

- 認証（登録、ログイン、トークン更新、ユーザー取得）と、在庫センター、カテゴリ、部品在庫、製品在庫の API（いずれも一覧、作成、編集、論理削除）。カテゴリ・部品在庫・製品在庫の詳細設計は `docs/detailed-design-stocks.md`。製品コードの列は V4、カテゴリ名・在庫の名称の重複禁止の DB 制約（`active_name`）は V5 で追加。カテゴリの削除と在庫の登録の同時実行は、カテゴリの行ロックで直列にしている。
- Flyway で管理するスキーマ（`_user`、`category_info`、`center_info`、`parts_stock_info`、`products_stock_info`）と、開発用の初期データ。
- 設計書: 要件定義、ER 図、詳細設計、非機能要件、レビュー報告書（すべて `docs/`）。
- テスト 300 件が成功する。層ごとのユニットテスト（Controller、Service、Repository、Config）、結合テスト 35 件（センター 19、カテゴリ・在庫 11、同時実行 5）、DB スキーマテスト 15 件、デモデータテスト 6 件、開発用UIアクセステスト 4 件。命令カバレッジ 97.3%（`-Pcoverage`）。
- パッケージは原典のドメイン単位、Javadoc とユーザー向けメッセージは日本語、エラーコードは原典に統一済み。
- GitHub Actions: `CI`（`./mvnw verify -Pcoverage`）と `Copilot Setup Steps`。
- **動作確認用の簡易画面**（http://localhost:8080/ → `/dev-ui/`）: 登録・ログイン、一覧・検索、登録・編集・削除、エラー確認用の入力例、通信ログ。`app.dev-ui.enabled`（既定 true、本番では false）。業務用の画面（`frontend/`）ではない。テストは 134 件。
- **業務用フロントエンド**（`frontend/`、Next.js 15、Chakra UI）: ログイン、アカウント登録、在庫センター管理、部品在庫一覧、製品在庫一覧、カテゴリ管理（いずれも一覧、検索、並び替え、ページ送り、登録、編集、削除）。設計は `docs/frontend-centers-design.md`。単体テスト 109 件と E2E 46 件（Playwright）。`npm run lint`、`typecheck`、`build` が通る。トークンは原典どおり Cookie に保存している（既知の制限。設計書第 7 章）。
- 起動、Swagger UI、H2 コンソール、`flyway:info` の確認済み。

### 3.2 構成

```
hands_ON/
├── backend/                  # Spring Boot（com.aojt.mgmt）。ここで mvnw を実行する
│   ├── src/main/java/...     # config, controller/{auth,user,center,category,stock}, service/{auth,jwt,center,category,stock,common},
│   │                         # repository/{user,center}, model, dto/{auth,user,center,common}, exception
│   ├── src/main/resources/application.yml
│   ├── src/main/resources/db/migration/V1__initial.sql   # スキーマ（V4: 製品コード、V5: 名称の一意制約）
│   ├── src/main/resources/db/seed-dev/V2__seed_dev_data.sql  # 最小の開発用データ（センター 3 件。テストも使う）
│   ├── src/main/resources/db/seed-demo/V3__seed_demo_data.sql  # デモデータ（センターを 22 件に。開発のみ、テストは読み込まない）
│   └── src/test/...          # 層ごとのユニットテスト、CenterApiIntegrationTest、repository/DatabaseSchemaTest
├── frontend/                 # Next.js。ここで npm を実行する。app/{centers,login,register,components,contexts,hooks,lib,types}、tests/{unit,e2e}
├── docs/                     # 設計書
├── .github/                  # copilot-instructions.md、workflows（CI、Copilot Setup Steps）、ISSUE_TEMPLATE
└── README.md                 # 手順（原典の README を基準にしている）
```

### 3.3 実行方法（Windows PowerShell）

```powershell
cd C:\Users\渡邉天\Documents\hands_ON\backend
.\mvnw.cmd spring-boot:run      # 起動。Swagger: http://localhost:8080/swagger-ui/index.html
.\mvnw.cmd test                 # テスト
.\mvnw.cmd flyway:info          # アプリを停止してから実行する
```

- カバレッジの検査（`-Pcoverage`）は、この PC（ユーザー名が日本語）では ASCII のドライブが必要: `subst X: C:\Users\渡邉天\Documents\hands_ON; cd X:\backend; .\mvnw.cmd verify -Pcoverage; cd C:\; subst X: /D`。

フロントエンド（バックエンドを起動してから）:

```powershell
cd C:\Users\渡邉天\Documents\hands_ON\frontend
Copy-Item .env.sample .env.local   # 初回のみ
npm install                        # 初回のみ。ロックファイルどおりなら npm ci
npm run dev                        # http://localhost:3000
npm run lint; npm run typecheck; npm run build
npm run test:unit                  # 単体テスト
npx playwright install chromium    # 初回のみ
npm run test:e2e                   # E2E（バックエンドが必要）
```

- 手順は `README.md` の「環境構築」「DB マイグレーション」を基準にする。**変更しない**（合意済み）。
- ログイン方法: Swagger の `POST /api/v1/auth/register` でユーザーを登録し、`accessToken` を **Authorize** に貼る（`Bearer ` は付けない）。

## 4. 読む順序

1. この `HANDOFF.md`
2. `README.md`
3. `docs/requirements.md`（要件、業務ルール、**未決事項と原典の不整合の一覧**）
4. `docs/er-diagram.md`（ER 図、テーブル設計、原典との差分）
5. `docs/detailed-design.md`（在庫センター API の詳細設計、原典との差分表 第 8 章）と `docs/detailed-design-stocks.md`（カテゴリ・部品在庫・製品在庫 API）
6. `docs/non-functional-requirements.md`
7. `docs/review/backend-review-centers.md`（レビュー指摘。冒頭の訂正を先に読む）
   `docs/review/test-scope-and-production-considerations-20261021.md`（テスト範囲と本番向け検討項目。現状は開発用で、本番に出す前の対応をまとめている）
8. 原典 zip の `docs/api-interface-spec.md`、`docs/screen-specifications.md`、`docs/table-definitions.md`、`adr/`、`.github/copilot-instructions.md`、`.github/instructions/`

`docs/detailed-design.md` は、スライドの「出力: api_design.md」に相当する。名前を `api_design.md` にする必要があれば、リネームする。

## 5. 技術メモ（つまずきやすい点）

| 事象 | 原因と対応 |
| --- | --- |
| `.\mvnw.cmd` が見つからない | `backend/` で実行していない。`cd backend` してから実行する |
| `.mvnw.cmd`（`\` 抜け）はエラー | PowerShell では `.\` が必要 |
| テストで Mockito の初期化エラー | ユーザー名に日本語（渡邉天）を含むパスで、Mockito のエージェント自己接続が失敗する。`backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`（`mock-maker-subclass`）で回避済み。**消さない** |
| JaCoCo で `forked VM terminated` | 同じ原因（パスの日本語）。JVM がエージェントの引数（パス）を文字化けさせる。そのため JaCoCo はプロファイル `coverage` にし、既定のビルドに入れていない。この PC では `subst X:` で ASCII パスにして実行する（第 3.3 章）。CI（Linux）では問題ない |
| JDK | JDK は 26 のみ（`C:\jdk-26.0.1`）。ビルドは Java 21 向け（`pom.xml` の `java.version=21`）。原典は JDK 21 を前提にしている |
| Maven | `mvnw` を使う（Maven のインストールは不要）。ラッパーは `backend/.mvn/` にある |
| H2 のファイル DB | `./target/testdb` は 1 プロセスしか開けない。アプリの起動中に `flyway:info`/`migrate` を実行しない。DB を作り直すには、アプリを止めて `backend/target/testdb.mv.db` を削除する |
| `git push` が「エラー」表示で終わる | PowerShell が git の進捗出力（stderr）を誤ってエラー扱いにしているだけ。`git status -sb` で `main...origin/main` と出れば成功 |
| フォルダの移動が `Permission denied` | VS Code などがフォルダを掴んでいる場合がある。コピーして元を削除する |
| JWT の鍵 | 起動ごとに自動生成（開発用）。再起動したらトークンを取り直す |
| テストのデータ分離 | テストは `application-test.yml`（プロファイル `test`）で、コンテキストごとに独立したインメモリ DB を使う |
| 日本語を含むコマンド | PowerShell への日本語の直接入力は、コミットメッセージなどで文字化けする。ファイルはエディタ系のツールで書く |
| 適用済みの Flyway SQL を編集すると起動に失敗する（`Migration checksum mismatch`） | コメントの修正でもチェックサムが変わる。適用済みのファイルは編集しない。誤って編集した場合、この PC では `.\mvnw.cmd flyway:repair` が DB に反映されなかった（H2 のファイル DB の終了処理が Maven の JVM で失敗するため）。アプリを止めて、H2 の RunScript で `UPDATE "flyway_schema_history" SET "checksum" = <ログの Resolved locally の値> WHERE "version" = '<版>'` を実行して直した。または `backend/target/testdb.mv.db` を削除して作り直す |
| `npm ci` が EUSAGE で失敗する | 原典の `package-lock.json` は、macOS で作られ、他 OS 用のオプションパッケージ（sharp など）が欠けていた。`npm install` で再生成して解消済み（依存のバージョン範囲は変えていない）。変更したときは `npm ci --dry-run` で確認する |
| `npm install` が遅い | 初回は 5 分前後かかることがある（Windows）。`allowScripts` の警告（sharp、unrs-resolver）は無視してよい |
| E2E がすべて失敗する | バックエンドが起動していない（`http://localhost:8080`）。先に `.\mvnw.cmd spring-boot:run` を実行する。初回は `npx playwright install chromium` が必要 |
| Next.js の「multiple lockfiles」警告 | 上位フォルダーに別の `package-lock.json` がある。`next.config.mjs` の `outputFileTracingRoot` で解消済み |

## 6. 原典との差分（現状）

詳細は `docs/detailed-design.md` 第 8 章、`docs/er-diagram.md` 第 7 章。

- 解消済み: テーブル名と列（`center_info` ほか）、Flyway、初期データの分離、`ddl-auto=validate`、パッケージ構成、クラス名、日本語の Javadoc とメッセージ、エラーコード、ユニットテスト、JaCoCo。
- 型は `TINYINT` を `SMALLINT` に、`DATETIME` を `TIMESTAMP` に置き換えた（PostgreSQL 互換）。原典の定義書はこのリポジトリの外にあるため、このリポジトリでは `docs/er-diagram.md` を正とする。
- 初期データは原典の誤りを直した（`category_type` を `part/product`、稼働状況を 0）。
- **未対応**: Lombok（保留）、`error.details`（要承認）。

## 7. 残作業

### 7.1 原典に合わせる修正（2026-10-14 に完了）

- [x] パッケージをドメイン単位に分ける。
- [x] クラス名を `InventoryCenter` から `Center` に変える。
- [x] Javadoc（`@param`、`@return`、`@throws`）とメッセージを日本語にする。
- [x] エラーコードを原典に合わせる（`AUTH_ERROR`、`INTERNAL_ERROR`、`BUSINESS_ERROR`）。409 用に `CONFLICT` を追加。
- [x] 型の置き換えをドキュメントに反映する（`docs/er-diagram.md`）。
- [x] API 仕様をドキュメントに反映する（`docs/detailed-design.md` 第 6.1 章: `GET /users/{id}` は DTO、本人または ADMIN のみ。409。リフレッシュのヘッダなしは 401）。

### 7.2 実装・テスト

- [x] ユニットテストを層ごとに追加する（Controller、Service、Repository、Config）。
- [x] JaCoCo の命令カバレッジ 80% の検査（プロファイル `coverage`、CI で毎回実行）。
- [ ] PostgreSQL での検証（Flyway、検索、制約）と本番用の設定。**要承認**: PostgreSQL の JDBC ドライバ（`org.postgresql:postgresql`）と Flyway の PostgreSQL モジュール（`flyway-database-postgresql`）を追加する必要がある。この PC には Docker が無いため、検証は GitHub Actions の PostgreSQL サービスコンテナで行う案。本番用設定は `application-prod.yml`（`spring.flyway.locations` を `classpath:db/migration` のみ、Swagger と H2 コンソールを無効、ログを INFO、鍵を固定、DB 接続を環境変数）。
- [ ] 入力エラーの項目別表示（`error.details`）。**API 契約の変更なので要承認**。
- [x] カテゴリ、部品在庫、製品在庫の API（`docs/detailed-design-stocks.md`）。エンティティ、画面も作成済み。製品コード（V4）、名称の重複禁止はセンター内、使用中カテゴリは削除・種別変更不可、は仮決定（設計書第 7 章）。
  - [ ] 横断検索 API（`GET /api/v1/stocks/search`。想定のみ、未実装）。
  - [ ] カテゴリ画面の CSV 一括登録（要件未定義。Q-8）。
  - [x] カテゴリ名・在庫の名称の重複禁止を DB でも保証する（V5。`active_name` 列と一意制約）。同時リクエストの競合は、先にコミットした 1 件だけが成功し、残りは 409。カテゴリの削除・種別変更と在庫の登録・更新の競合も、カテゴリの行ロックで防いでいる（`ConcurrencyIntegrationTest`。`docs/detailed-design-stocks.md` 第 5.2、6.1 章）。**既存データに未削除どうしの重複がある環境では V5 が失敗する**。本番への適用前にデータのコピーで確認する。PostgreSQL での実行確認は未実施（下の「PostgreSQL での検証」に含める）。
  - [ ] センター詳細画面（センターからその在庫への導線）。API は `centerId` で絞り込み済み。
- [x] フロントエンド（`frontend/`）: 在庫センター画面（一覧、検索、新規・編集・削除）。原典の `frontend/` を土台にした。画面仕様の不整合 C-4、C-9、C-10 は仮決定し、`requirements.md` 第 7 章に追記した（住所は画面で結合・分割、初期ソートは `centerId` 降順、操作列あり）。
  - [ ] フロントエンドの E2E を CI で実行する（バックエンドを CI で起動する必要がある。いまはローカルのみ）。
  - [ ] トークンの保存を HttpOnly Cookie（BFF）にするかを決める（NFR-SEC-03。設計の変更）。
  - [ ] 他の画面（ダッシュボード、操作履歴、アカウント）は、API と要件が定まってから作る。
- [ ] 操作履歴、ダッシュボードの要件を決めてから実装する（未定義）。
- [ ] 非機能の優先対応（`docs/non-functional-requirements.md` 第 9 章）。

### 7.3 課題の進行（スライド）

- [ ] 別のモデルで同じレビューを行い、`docs/review/backend-review-centers.md` と比較する（工程 IV。レビュー依頼用プロンプトは同書の第 7 章）。
- [ ] 別ブランチで別のアプローチを試し、比較する（V-1）。
- [ ] Slack に振り返りを投稿する（**ユーザーの作業**）: 1. どう進めたか、2. AI とのやり取りで印象に残ったこと、3. 次にどう取り組むか。
- [ ] Day1 の次の工程（実装の続き、動作確認）を、スライド 19〜24 ページに沿って進める。

### 7.4 保留中（決めない方針。**ユーザーが決めるまで実装しない**）

| 項目 | 内容 | 関連 |
| --- | --- | --- |
| アクセストークンの有効期限 | 今のまま（hands_ON は 15 分、原典は 24 時間）。変更しない | NFR-SEC-02 |
| 自己登録の可否 | 本番でも誰でも登録できてよいか | Q-1、H-1 |
| ADMIN の付与方法と権限差 | USER と ADMIN の操作の違い | Q-2 |
| 在庫があるセンターの削除 | 禁止するか、警告のみか | Q-3 |
| 現在保管容量の更新元 | 在庫テーブルに体積が無く、在庫から計算できない | Q-4 |
| センター名の重複 | 削除済みを含めるか（現状は含める） | Q-5 |
| 操作履歴の範囲 | 何を記録するか | Q-6 |
| ダッシュボードの項目 | 発注点、安全在庫、入出庫履歴の持ち方 | Q-7 |
| CSV 一括登録 | 対象項目、形式、エラー時の扱い | Q-8 |
| 海外拠点 | 郵便番号と電話番号の形式 | Q-9 |
| 非機能の数値目標 | 規模、稼働時間、RTO/RPO、応答時間 | Q-10 |
| 楽観ロック | `version` 列の追加。同じ行を 2 人が編集したときの「更新の消失」は現状防げない（リクエスト・レスポンスに `version` を追加する API 契約の変更。画面の改修前に決めるのが安い） | M-2 |
| カテゴリ名のユニーク制約 | **解消済み**（種別内で一意、DB 制約あり。V5） | BR-03 |
| 製品コード | **解消済み**（列を追加。V4） | C-5 |
| 住所の分割 | 画面は都道府県などに分割、DB は 1 列。**仮決定**: 画面で結合・簡易判定で分割（`docs/frontend-centers-design.md` 第 4 章）。列を分けるなら要承認 | C-4 |
| Lombok の採用 | 原典は使用。新規依存の扱い | — |

質問の全体と選択肢は `docs/requirements.md` 第 7 章にある。

## 8. 作業の終え方

- 変更したら、`.\mvnw.cmd test` を通してから、英語のメッセージでコミットして `main` に push する。
- 設計の判断を変えたら、`docs/` の該当箇所と、この `HANDOFF.md` の「残作業」を更新する。
- 原典と食い違う点を見つけたら、`docs/requirements.md` 第 7 章に追記する。
