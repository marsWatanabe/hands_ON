# ER 図・テーブル設計: Inventrack

| 項目 | 内容 |
| --- | --- |
| 作成日 | 2026-10-14 |
| 参照元 | 原典 `docs/table-definitions.md`、`backend/src/main/resources/db/migration/V1__create_categories_table.sql`、`backend/.../model/User.java` |
| 関連資料 | [requirements.md](requirements.md)、[detailed-design.md](detailed-design.md) |

出典の記号は [requirements.md](requirements.md) と同じ。**[原典]** は原典、**[提案]** は本書の仮置き。

## 1. ER 図（原典の定義）

テーブルは 5 つ。`center_info` と `category_info` が、部品在庫と製品在庫から参照される。`_user` は他のテーブルと関連を持たない。

```mermaid
erDiagram
    CENTER_INFO ||--o{ PARTS_STOCK_INFO : "center_id"
    CENTER_INFO ||--o{ PRODUCTS_STOCK_INFO : "center_id"
    CATEGORY_INFO ||--o{ PARTS_STOCK_INFO : "category_id (type=part)"
    CATEGORY_INFO ||--o{ PRODUCTS_STOCK_INFO : "category_id (type=product)"

    _USER {
        int id PK "自動採番"
        varchar firstname "名"
        varchar lastname "姓"
        varchar email UK "ログインID"
        varchar password "BCryptハッシュ"
        varchar role "USER / ADMIN"
    }

    CATEGORY_INFO {
        int category_id PK "自動採番"
        varchar category_type "part / product"
        varchar category_name "20文字"
        varchar description "255文字"
        tinyint delete_flag "0:未削除 1:削除済"
        datetime create_date
        datetime update_date
    }

    CENTER_INFO {
        int center_id PK "自動採番"
        varchar center_name "20文字 必須 重複禁止"
        varchar post_code "10文字 必須"
        varchar address "255文字 必須"
        varchar phone_number "20文字 必須"
        varchar manager_name "100文字 必須"
        tinyint operational_status "0:稼働中 1:稼働停止"
        int max_storage_capacity "m3"
        int current_storage_capacity "m3"
        varchar notes "255文字"
        tinyint delete_flag "0:未削除 1:削除済"
        datetime create_date
        datetime update_date
    }

    PARTS_STOCK_INFO {
        int stock_id PK "自動採番"
        int category_id FK
        varchar name "255文字 必須"
        int center_id FK
        varchar description "255文字"
        int amount "数量(個)"
        tinyint delete_flag "0:未削除 1:削除済"
        datetime create_date
        datetime update_date
    }

    PRODUCTS_STOCK_INFO {
        int stock_id PK "自動採番"
        int category_id FK
        varchar name "255文字 必須"
        int center_id FK
        varchar description "255文字"
        int amount "数量(個)"
        tinyint delete_flag "0:未削除 1:削除済"
        datetime create_date
        datetime update_date
    }
```

## 2. 関連（リレーション）

| 親 | 子 | 外部キー | 多重度 | 備考 |
| --- | --- | --- | --- | --- |
| `center_info` | `parts_stock_info` | `center_id` | 1 対 0 以上 | [原典] 外部キー制約あり。カスケード指定なし |
| `center_info` | `products_stock_info` | `center_id` | 1 対 0 以上 | 同上 |
| `category_info` | `parts_stock_info` | `category_id` | 1 対 0 以上 | [原典] 参照先の `category_type` が `part` であることは、運用ルール（アプリの検証）で保証する |
| `category_info` | `products_stock_info` | `category_id` | 1 対 0 以上 | 同上。`product` であることを保証する |

- 在庫テーブルの `category_id`、`center_id` は NULL を許す [原典]。画面仕様は必須としているため、アプリ側で必須検証する。
- 論理削除のため、削除したセンターやカテゴリを参照する在庫行が残る。外部キー制約では防げない。**削除時の扱いは要確認**（[requirements.md](requirements.md) Q-3）。

## 3. テーブル定義の要点

### 3.1 `center_info`（在庫センター。今回の対象）

| 物理名 | 型 | NOT NULL | 説明 | 出典 |
| --- | --- | :-: | --- | --- |
| center_id | INT（自動採番） | ◯ | 主キー | [原典] |
| center_name | VARCHAR(20) | ◯ | 在庫センター名。重複禁止 | [原典] |
| post_code | VARCHAR(10) | ◯ | 郵便番号 | [原典] |
| address | VARCHAR(255) | ◯ | 住所 | [原典] |
| phone_number | VARCHAR(20) | ◯ | 電話番号（管理者の電話番号） | [原典] |
| manager_name | VARCHAR(100) | ◯ | 管理者名 | [原典] |
| operational_status | TINYINT | ◯ | 0 = 稼働中、1 = 稼働停止。SQL の既定値は 0 | [原典] |
| max_storage_capacity | INT |  | 最大保管容量（m³） | [原典] |
| current_storage_capacity | INT |  | 現在保管容量（m³） | [原典] |
| notes | VARCHAR(255) |  | 備考 | [原典] |
| delete_flag | TINYINT | ◯ | 0 = 未削除、1 = 削除済。SQL の既定値は 0 | [原典] |
| create_date | DATETIME |  | 作成日時 | [原典] |
| update_date | DATETIME |  | 更新日時 | [原典] |

### 3.2 `_user`

| 物理名 | 型 | NOT NULL | 説明 | 出典 |
| --- | --- | :-: | --- | --- |
| id | INT（自動採番） | ◯ | 主キー | [原典] |
| firstname、lastname | VARCHAR(100) | ◯ | 名、姓 | [原典 SQL] |
| email | VARCHAR(255) | ◯ | ユニーク制約あり。テーブル定義書は 320 桁、SQL は 255 | [原典] |
| password | VARCHAR(255) | ◯ | BCrypt のハッシュ | [原典] |
| role | VARCHAR(50) | ◯ | `USER` または `ADMIN`（CHECK 制約）。既定値 `USER` | [原典 SQL] |

テーブル名が `_user` なのは、エンティティの `@Table(name = "_user")` に合わせたため [原典 テーブル定義書の備考]。

### 3.3 `category_info`、`parts_stock_info`、`products_stock_info`

原典の `docs/table-definitions.md` と上の ER 図のとおり。`category_info.category_name` は 20 文字、`name`（在庫）は 255 文字。

## 4. インデックスと制約

原典のテーブル定義書は、索引を主キーと外部キーの列にだけ示している。**2026-10-14 に DB を再設計し、次のうち「実装済」を Flyway の `V1__initial.sql` に反映した**。「未決」は要確認のため入れていない。

| 対象 | 内容 | 目的 | 状況 |
| --- | --- | --- | --- |
| `center_info.center_name` | ユニーク制約（削除済みを含む） | BR-02（重複禁止）を DB でも守る。API 仕様書の「DB ユニーク制約」に対応 | **実装済**。削除済みを含めるかは未決（Q-5）。含めない場合は PostgreSQL の部分ユニークインデックス（`WHERE delete_flag = 0`）が必要で、H2 には無い |
| `parts_stock_info`、`products_stock_info` の `category_id`、`center_id` | インデックス | 結合、絞り込み。原典の「索引 2」に対応。外部キーは PostgreSQL では自動で索引にならない | **実装済** |
| `delete_flag`、`operational_status`、`category_type`、`role` | CHECK 制約（許可値のみ） | 不正値の防止。`role` は原典 SQL にもある | **実装済** |
| 在庫の `amount`、センターの容量 | CHECK 制約（0 以上、NULL は許可） | 負数の防止。画面仕様「在庫数は 0 以上」に対応 | **実装済** |
| `_user.email` | ユニーク制約 | ログイン ID の重複防止 | **実装済**（原典 SQL にもある） |
| `center_info (delete_flag, operational_status)` | 複合インデックス | 一覧の絞り込み | 未実施（件数が増えた場合のみ） |
| `category_info (category_type, category_name)` | ユニーク制約 | BR-03 | 未決（要確認） |
| `center_info.version` ほか | 楽観ロック用の `version` 列 | 同時更新による更新の消失の防止 | 未決（要確認） |

## 5. 拡張案: 操作履歴（提案）

原典のサイドバーに「操作履歴一覧」があるが、テーブルも仕様も無い。次の 1 テーブルを仮置きする。**スコープと記録項目は要確認**（Q-6）。

```mermaid
erDiagram
    _USER ||--o{ OPERATION_HISTORY : "user_id"

    OPERATION_HISTORY {
        bigint history_id PK "自動採番"
        int user_id FK "操作者"
        varchar target_table "対象テーブル名"
        int target_id "対象の主キー"
        varchar operation "CREATE / UPDATE / DELETE"
        varchar summary "変更内容の要約(個人情報を含めない)"
        datetime operated_at "操作日時"
    }
```

## 6. データ移行・環境差異

- 開発は H2、本番は PostgreSQL [原典 ADR-0003]。Flyway のマイグレーション SQL は両方で動く記述にする。
- 差異に注意する項目:
  - `TINYINT` は PostgreSQL に無い（`SMALLINT` に置き換える）。
  - `DATETIME` は PostgreSQL では `TIMESTAMP`。
  - 大文字小文字、`LIKE` の照合順序、`ESCAPE` の挙動。
  - 部分インデックス。
- 原典の `V1__create_categories_table.sql` は `TINYINT` を使っており、PostgreSQL には無い型のため、そのままでは動かない。→ **hands_ON では `SMALLINT` に置き換え済み**。列の意味（0/1 のフラグ）は変わらない。原典の `docs/table-definitions.md` の型表記（TINYINT）も、この置き換えを反映する更新が必要（「ドキュメントを先に直す」原則）。
- PostgreSQL での実際の検証は未実施（次の作業）。

## 7. 原典と hands_ON の差分（2026-10-14 の DB 再設計後）

初稿では hands_ON の実装が原典と食い違っていたが、原典のテーブル定義に合わせて修正した。現在の差分と対応状況を示す。

| 項目 | 原典 | hands_ON（現在） | 状況 |
| --- | --- | --- | --- |
| テーブル名、列名 | `center_info` ほか | 同じ | 解消済 |
| `post_code`、`address`、`phone_number`、`manager_name` | 桁数 10/255/20/100、必須 | 同じ（API の検証も必須と桁数を合わせた） | 解消済 |
| `max_storage_capacity`、`current_storage_capacity` | 任意（NULL 可） | 同じ（API も任意） | 解消済 |
| `notes` | VARCHAR(255) | 同じ | 解消済 |
| `_user` のテーブル名と列の桁数 | `_user`、100/100/255 | 同じ | 解消済 |
| スキーマ管理 | Flyway（`ddl-auto=update` も有効で二重管理） | Flyway のみ。Hibernate は `ddl-auto=validate` | 解消済（原典の二重管理を解消） |
| 初期データ | V1 SQL に同居。`PARTS`/`PRODUCT`、稼働状況 1 | `db/seed-dev/V2` に分離。`part`/`product`、稼働状況 0（テーブル定義を優先） | 解消済（C-1、C-2） |
| 型 | `TINYINT`、`DATETIME` | `SMALLINT`、`TIMESTAMP`（PostgreSQL 互換） | 変更。原典のドキュメントの更新が必要 |
| エンティティ | `center_info` 用のエンティティ（クラス名は別） | クラス名 `Center`（`model` パッケージ、`center_info` にマップ） | 解消済 |
| カテゴリ、部品・製品在庫のエンティティ | あり | テーブルのみ（エンティティは未作成） | 後続の API 実装時に作成 |
