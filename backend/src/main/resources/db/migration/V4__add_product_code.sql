-- ============================================================================
-- V4: 製品在庫に製品コード（product_code）を追加する
--
-- 原典の画面仕様（製品在庫一覧）に「製品コード」があるが、products_stock_info に列が無かった
-- （docs/requirements.md C-5、docs/detailed-design-stocks.md 第 5.1 章）。
--
--   1. 列を NULL 可で追加する。
--   2. 既存の行には、stock_id から採番する（例: PRD-000012）。LPAD は桁あふれで切り捨てるため、6 桁にしている。
--   3. NOT NULL と一意制約を付ける（削除済みを含めて一意）。
--
-- H2（開発）と PostgreSQL（本番）の両方で動く記述にする。
-- ============================================================================

ALTER TABLE products_stock_info ADD COLUMN product_code VARCHAR(20);

UPDATE products_stock_info
   SET product_code = 'PRD-' || LPAD(CAST(stock_id AS VARCHAR(10)), 6, '0')
 WHERE product_code IS NULL;

ALTER TABLE products_stock_info ALTER COLUMN product_code SET NOT NULL;

ALTER TABLE products_stock_info
    ADD CONSTRAINT uk_products_stock_product_code UNIQUE (product_code);
