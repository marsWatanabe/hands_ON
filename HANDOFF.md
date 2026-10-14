# 引継ぎ指示書: hands_ON（Inventrack）

| 項目 | 内容 |
| --- | --- |
| 作成日 | 2026-10-14 |
| リポジトリ | https://github.com/marsWatanabe/hands_ON（public、ブランチは `main` のみ） |
| ローカル | `C:\Users\渡邉天\Documents\hands_ON`（VS Code のワークスペースでは、`seiseki-project-v0.15.0` とは別のルートフォルダ） |
| 目的 | AIDD 開発コース ハンズオン Day1 の課題: 「在庫センター一覧取得 API」を、詳細設計から実装まで AI と進める |
| 最新の状態 | `git log` で確認する。作業ツリーはクリーン |

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

- 認証（登録、ログイン、トークン更新、ユーザー取得）と在庫センター API（一覧、作成、編集、論理削除）。
- Flyway で管理するスキーマ（`_user`、`category_info`、`center_info`、`parts_stock_info`、`products_stock_info`）と、開発用の初期データ。
- 設計書: 要件定義、ER 図、詳細設計、非機能要件、レビュー報告書（すべて `docs/`）。
- テスト 25 件（結合テスト 13 件と DB スキーマテスト 12 件）が成功する。
- 起動、Swagger UI、H2 コンソール、`flyway:info` の確認済み。

### 3.2 構成

```
hands_ON/
├── backend/                  # Spring Boot（com.aojt.mgmt）。ここで mvnw を実行する
│   ├── src/main/resources/application.yml
│   ├── src/main/resources/db/migration/V1__initial.sql   # スキーマ
│   ├── src/main/resources/db/seed-dev/V2__seed_dev_data.sql  # 開発用データ
│   └── src/test/...          # InventoryCenterApiTest、repository/DatabaseSchemaTest
├── docs/                     # 設計書
└── README.md                 # 手順（原典の README を基準にしている）
```

### 3.3 実行方法（Windows PowerShell）

```powershell
cd C:\Users\渡邉天\Documents\hands_ON\backend
.\mvnw.cmd spring-boot:run      # 起動。Swagger: http://localhost:8080/swagger-ui/index.html
.\mvnw.cmd test                 # テスト
.\mvnw.cmd flyway:info          # アプリを停止してから実行する
```

- 手順は `README.md` の「環境構築」「DB マイグレーション」を基準にする。**変更しない**（合意済み）。
- ログイン方法: Swagger の `POST /api/v1/auth/register` でユーザーを登録し、`accessToken` を **Authorize** に貼る（`Bearer ` は付けない）。

## 4. 読む順序

1. この `HANDOFF.md`
2. `README.md`
3. `docs/requirements.md`（要件、業務ルール、**未決事項と原典の不整合の一覧**）
4. `docs/er-diagram.md`（ER 図、テーブル設計、原典との差分）
5. `docs/detailed-design.md`（在庫センター API の詳細設計、原典との差分表 第 8 章）
6. `docs/non-functional-requirements.md`
7. `docs/review/backend-review-centers.md`（レビュー指摘。冒頭の訂正を先に読む）
8. 原典 zip の `docs/api-interface-spec.md`、`docs/screen-specifications.md`、`docs/table-definitions.md`、`adr/`、`.github/copilot-instructions.md`、`.github/instructions/`

`docs/detailed-design.md` は、スライドの「出力: api_design.md」に相当する。名前を `api_design.md` にする必要があれば、リネームする。

## 5. 技術メモ（つまずきやすい点）

| 事象 | 原因と対応 |
| --- | --- |
| `.\mvnw.cmd` が見つからない | `backend/` で実行していない。`cd backend` してから実行する |
| `.mvnw.cmd`（`\` 抜け）はエラー | PowerShell では `.\` が必要 |
| テストで Mockito の初期化エラー | ユーザー名に日本語（渡邉天）を含むパスで、Mockito のエージェント自己接続が失敗する。`backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`（`mock-maker-subclass`）で回避済み。**消さない** |
| JDK | JDK は 26 のみ（`C:\jdk-26.0.1`）。ビルドは Java 21 向け（`pom.xml` の `java.version=21`）。原典は JDK 21 を前提にしている |
| Maven | `mvnw` を使う（Maven のインストールは不要）。ラッパーは `backend/.mvn/` にある |
| H2 のファイル DB | `./target/testdb` は 1 プロセスしか開けない。アプリの起動中に `flyway:info`/`migrate` を実行しない。DB を作り直すには、アプリを止めて `backend/target/testdb.mv.db` を削除する |
| `git push` が「エラー」表示で終わる | PowerShell が git の進捗出力（stderr）を誤ってエラー扱いにしているだけ。`git status -sb` で `main...origin/main` と出れば成功 |
| フォルダの移動が `Permission denied` | VS Code などがフォルダを掴んでいる場合がある。コピーして元を削除する |
| JWT の鍵 | 起動ごとに自動生成（開発用）。再起動したらトークンを取り直す |
| テストのデータ分離 | テストは `application-test.yml`（プロファイル `test`）で、コンテキストごとに独立したインメモリ DB を使う |
| 日本語を含むコマンド | PowerShell への日本語の直接入力は、コミットメッセージなどで文字化けする。ファイルはエディタ系のツールで書く |

## 6. 原典との差分（現状）

詳細は `docs/detailed-design.md` 第 8 章、`docs/er-diagram.md` 第 7 章。

- 解消済み: テーブル名と列（`center_info` ほか）、Flyway、初期データの分離、`ddl-auto=validate`。
- 型は `TINYINT` を `SMALLINT` に、`DATETIME` を `TIMESTAMP` に置き換えた（PostgreSQL 互換。原典の定義書の更新が必要）。
- 初期データは原典の誤りを直した（`category_type` を `part/product`、稼働状況を 0）。
- **未対応**: パッケージ構成（原典はドメイン単位）、日本語の Javadoc とメッセージ、エラーコード、ユニットテスト、JaCoCo。

## 7. 残作業

### 7.1 原典に合わせる修正（方針は合意済み。着手可）

- [ ] パッケージをドメイン単位に分ける（`controller/center`、`service/center`、`repository/center`、`dto/center`、`dto/common`、`model`）。
- [ ] クラス名を `InventoryCenter` から `Center` に変える。
- [ ] Javadoc（`@param`、`@return`、`@throws`）とメッセージを日本語にする。
- [ ] エラーコードを原典に合わせる（`AUTH_ERROR`、`INTERNAL_ERROR`、`BUSINESS_ERROR`）。409 用に `CONFLICT` を追加する。
- [ ] 原典の `docs/table-definitions.md` に、型の置き換え（`TINYINT` から `SMALLINT`）を反映する。
- [ ] API 仕様書に、次を反映する: `GET /users/{id}` は DTO を返し本人か ADMIN のみ、409 を返す場合がある、`error.details`（追加する場合）。

### 7.2 実装・テスト

- [ ] ユニットテストを層ごとに追加する（Controller は `@WithMockUser`、Service は Mockito、Repository は H2）。メソッド名は期待動作、Javadoc に目的を書く。
- [ ] JaCoCo の命令カバレッジ 80% の検査を `pom.xml` に入れる（要承認: 新規の依存）。
- [ ] PostgreSQL での検証（Flyway、検索、制約）と、本番用の設定（`spring.flyway.locations` を `classpath:db/migration` のみにする、Swagger と H2 コンソールを無効にする、ログを INFO にする、鍵を固定する）。
- [ ] 入力エラーの項目別表示（`error.details`）。**API 契約の変更なので要承認**。
- [ ] カテゴリ、部品在庫、製品在庫の API（`docs/detailed-design.md` 第 9 章の案）。エンティティも作る。
- [ ] フロントエンド（`frontend/`）: 在庫センター画面（一覧、検索、新規・編集・削除）。原典の `frontend/` が土台（Next.js、Chakra UI）。画面仕様の不整合（`requirements.md` C-4、C-9、C-10）を先に整理する。
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
| 楽観ロック | `version` 列の追加 | M-2 |
| カテゴリ名のユニーク制約 | 種別内で一意にするか | BR-03 |
| 製品コード | 製品在庫に列を追加するか | C-5 |
| 住所の分割 | 画面は都道府県などに分割、DB は 1 列 | C-4 |
| Lombok の採用 | 原典は使用。新規依存の扱い | — |

質問の全体と選択肢は `docs/requirements.md` 第 7 章にある。

## 8. 作業の終え方

- 変更したら、`.\mvnw.cmd test` を通してから、英語のメッセージでコミットして `main` に push する。
- 設計の判断を変えたら、`docs/` の該当箇所と、この `HANDOFF.md` の「残作業」を更新する。
- 原典と食い違う点を見つけたら、`docs/requirements.md` 第 7 章に追記する。
