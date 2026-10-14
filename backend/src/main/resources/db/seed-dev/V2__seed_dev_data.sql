-- ============================================================================
-- V2: 開発用の初期データ（開発・テスト環境のみ。本番では db/seed-dev を読み込まない）
--
-- 原典 V1__create_categories_table.sql の初期データに基づく。
-- 原典との差分（docs/table-definitions.md を優先するため）:
--   * category_type は 'PARTS' / 'PRODUCT' ではなく 'part' / 'product'
--   * operational_status は 0 = 稼働中（原典の初期データは全件 1 = 稼働停止だった）
-- ============================================================================

-- カテゴリ（部品 10 件、製品 6 件）
INSERT INTO category_info (category_type, category_name, description, delete_flag, create_date, update_date) VALUES
('part', 'フレーム', 'ドローンの機体フレーム', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('part', 'プロペラ', '回転翼プロペラ', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('part', '電動モーター', 'ブラシレス電動モーター', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('part', '電子速度調整器', 'ESC（Electronic Speed Controller）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('part', 'バッテリー', 'リチウムポリマーバッテリー', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('part', 'フライトコントローラー', '飛行制御システム', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('part', 'リモートコントローラー', '送信機・プロポ', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('part', '受信機', '受信機・レシーバー', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('part', 'GPSモジュール', 'GPS測位システム', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('part', 'カメラ／センサー', 'カメラ、センサー類', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('product', '災害用ドローン', '災害時の救助・調査用ドローン', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('product', '点検用ドローン', 'インフラ点検専用ドローン', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('product', '配送用ドローン', '小型配送サービス用ドローン', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('product', '農業用ドローン', '農薬散布・作物監視用ドローン', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('product', '警備用ドローン', 'セキュリティ・監視用ドローン', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('product', '撮影用ドローン', '映像撮影・空撮用プロ仕様ドローン', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 在庫センター（3 件）
INSERT INTO center_info (center_name, post_code, address, phone_number, manager_name, operational_status, max_storage_capacity, current_storage_capacity, notes, delete_flag, create_date, update_date) VALUES
('東京センター', '100-0001', '東京都千代田区千代田1-1-1', '03-1234-5678', '田中太郎', 0, 10000, 3500, '本社併設の主要センター', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('大阪センター', '530-0001', '大阪府大阪市北区梅田1-1-1', '06-1234-5678', '佐藤花子', 0, 8000, 2800, '関西地区の拠点センター', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('名古屋センター', '450-0002', '愛知県名古屋市中村区名駅1-1-1', '052-123-4567', '鈴木次郎', 0, 6000, 1900, '中部地区の配送センター', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 部品在庫（20 件）
INSERT INTO parts_stock_info (category_id, name, center_id, description, amount, delete_flag, create_date, update_date) VALUES
(1, 'カーボンフレーム 450mm', 1, 'クアッドコプター用カーボンフレーム', 85, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'アルミフレーム 650mm', 2, 'ヘキサコプター用アルミフレーム', 45, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '9インチプロペラ', 1, 'カーボン製プロペラ（CW/CCW）', 320, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '11インチプロペラ', 3, '大型機用プロペラ（CW/CCW）', 180, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'ブラシレスモーター 2212', 1, '920KV ブラシレスモーター', 150, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'ブラシレスモーター 2814', 2, '700KV 大型機用モーター', 80, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, '30A ESC', 1, '30アンペアESC（BLHeli_S）', 200, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, '60A ESC', 3, '60アンペア大容量ESC', 95, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'LiPo 3S 2200mAh', 1, '11.1V リチウムポリマーバッテリー', 120, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'LiPo 4S 5000mAh', 2, '14.8V 大容量バッテリー', 65, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'Pixhawk 6C', 1, 'PX4フライトコントローラー', 75, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'F4 Flight Controller', 3, 'F4プロセッサー搭載FC', 110, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'FrSky Taranis X9D+', 1, '16ch送信機', 25, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'Radiolink AT10II', 2, '12ch送信機', 35, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'FrSky X8R', 1, '8ch受信機（テレメトリー対応）', 90, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'FlySky FS-iA6B', 3, '6ch受信機', 150, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'u-blox NEO-M8N', 1, 'GPS/GLONASS対応モジュール', 85, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'Here3 GPS', 2, 'RTK対応高精度GPSモジュール', 30, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'FPVカメラ 1200TVL', 1, 'CMOS 1/3インチFPVカメラ', 140, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, '超音波センサー HC-SR04', 3, '距離測定センサー', 200, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 製品在庫（12 件）
INSERT INTO products_stock_info (category_id, name, center_id, description, amount, delete_flag, create_date, update_date) VALUES
(11, '災害用ドローン-X Pro', 1, '耐候性・長時間飛行対応災害救助ドローン', 12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, '災害用ドローン-X Lite', 3, '軽量コンパクト災害調査ドローン', 18, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, '点検用ドローン-Y Standard', 1, 'インフラ点検専用標準モデル', 25, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, '点検用ドローン-Y Advanced', 2, '高解像度カメラ搭載上位モデル', 15, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, '配送用ドローン Express', 1, '最大2kg積載小型配送ドローン', 30, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, '配送用ドローン Cargo', 3, '最大5kg積載中型配送ドローン', 20, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, '農業用ドローン-Z Sprayer', 2, '農薬散布専用10L タンク搭載', 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, '農業用ドローン-Z Monitor', 1, '作物監視・マルチスペクトルカメラ搭載', 14, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(15, '警備用ドローン Patrol', 1, '夜間撮影対応セキュリティドローン', 22, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(15, '警備用ドローン Guard', 3, 'AI顔認識機能付き警備ドローン', 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, '撮影用ドローン Cinema', 2, '4K/60fps プロ仕様空撮ドローン', 16, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, '撮影用ドローン Studio', 1, '8K対応シネマ撮影ドローン', 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
