-- ============================================================================
-- V5: カテゴリ名と在庫の名称の「未削除どうしの重複禁止」を、DB の一意制約で保証する
--
-- これまでは Service の事前照合だけで守っており、同時リクエストの競合を防げなかった
-- （docs/detailed-design-stocks.md 第 6.1 章）。
--
-- 削除済みの行は重複してよいため、「削除済みを含めた一意制約」は使えない。H2 には部分ユニークインデックス
-- （WHERE delete_flag = 0）が無いため、H2 と PostgreSQL の両方で動く次の方法にする。
--
--   active_name: 未削除の間だけ名称と同じ値を持ち、削除済みは NULL にする列。
--   一意制約は active_name を含める。NULL どうしは重複として扱われない（H2、PostgreSQL とも）ため、
--   削除済みの行は何件あっても、未削除の行は 1 件だけになる。
--
-- active_name はアプリ（エンティティ）が名称と削除フラグから設定する。直接 SQL で行を追加・更新するときは、
-- 未削除なら name と同じ値を入れること（入れ忘れた行は一意制約の対象にならない）。
--
--   category_info      : (category_type, active_name)  ← 同じ種別の中で一意
--   parts_stock_info   : (center_id, active_name)      ← 同じ在庫センターの中で一意
--   products_stock_info: (center_id, active_name)      ← 同上
--
-- 既存のデータに、未削除どうしの重複があると、この移行は失敗する（制約違反）。その場合は、重複している行の
-- 名称を直すか削除してから、再度実行する。
-- ============================================================================

ALTER TABLE category_info ADD COLUMN active_name VARCHAR(20);

UPDATE category_info
   SET active_name = category_name
 WHERE delete_flag = 0;

ALTER TABLE category_info
    ADD CONSTRAINT uk_category_active_name UNIQUE (category_type, active_name);

ALTER TABLE parts_stock_info ADD COLUMN active_name VARCHAR(255);

UPDATE parts_stock_info
   SET active_name = name
 WHERE delete_flag = 0;

ALTER TABLE parts_stock_info
    ADD CONSTRAINT uk_parts_stock_active_name UNIQUE (center_id, active_name);

ALTER TABLE products_stock_info ADD COLUMN active_name VARCHAR(255);

UPDATE products_stock_info
   SET active_name = name
 WHERE delete_flag = 0;

ALTER TABLE products_stock_info
    ADD CONSTRAINT uk_products_stock_active_name UNIQUE (center_id, active_name);
