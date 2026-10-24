-- ============================================================================
-- V3: 動作確認用のデモデータ（開発環境のみ。本番では db/seed-demo を読み込まない）
--
-- V2（開発用の初期データ）に続けて、在庫センターを 3 件から 22 件に増やす（19 件を追加）。
-- 各センターに、紐づく部品在庫（2 件）と製品在庫（1 件）を入れる（部品 38 件、製品 19 件を追加）。
--
-- 方針:
--   * テスト（application-test.yml）はこのデータを読み込まない。テストの期待値は V2 の 3 件のまま。
--   * 住所、電話番号、担当者名は架空のサンプル（番地は 1-1 などの仮の値）。
--   * 在庫のセンターは、ID ではなくセンター名で引く（開発用 DB に手動で追加したセンターがあっても、ID がずれない）。
--   * カテゴリ ID は V2 の採番（部品 1〜10、製品 11〜16）に従う。
--   * 稼働停止（operational_status = 1）は 3 件、容量が未設定（NULL）は 1 件、備考なしは 4 件。
--   * 開発用 DB に同名のセンターが既にあると、一意制約（uk_center_name）で失敗する。その場合は
--     backend/target/testdb.mv.db を削除して作り直す。
-- ============================================================================

-- 在庫センター（19 件）。東京、大阪、名古屋（V2）と合わせて 22 件
INSERT INTO center_info (center_name, post_code, address, phone_number, manager_name, operational_status, max_storage_capacity, current_storage_capacity, notes, delete_flag, create_date, update_date) VALUES
('札幌センター', '060-0001', '北海道札幌市中央区北1条西2-1', '011-123-4567', '高橋誠', 0, 7000, 2100, '北海道全域の拠点', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('仙台センター', '980-0021', '宮城県仙台市青葉区中央1-1-1', '022-123-4567', '伊藤美咲', 0, 5500, 1800, '東北地区の災害対応拠点', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('さいたまセンター', '330-0854', '埼玉県さいたま市大宮区桜木町1-1-1', '048-123-4567', '渡辺健', 0, 4500, 1500, '首都圏北部の配送拠点', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('千葉センター', '261-0023', '千葉県千葉市美浜区中瀬1-1', '043-123-4567', '山本由美', 0, 5000, 2300, '撮影機材の集約センター', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('横浜センター', '220-0012', '神奈川県横浜市西区みなとみらい2-2-1', '045-123-4567', '中村大輔', 0, 6500, 3000, '港湾地区の配送センター', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('新潟センター', '950-0087', '新潟県新潟市中央区花園1-1-1', '025-123-4567', '小林恵子', 1, 3500, 0, '設備改修のため一時停止中', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('金沢センター', '920-0858', '石川県金沢市木ノ新保町1-1', '076-123-4567', '加藤秀樹', 0, 3000, 900, '北陸地区の拠点', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('静岡センター', '420-0851', '静岡県静岡市葵区黒金町1-1', '054-123-4567', '吉田直子', 0, 4000, 1200, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('浜松センター', '430-0926', '静岡県浜松市中央区砂山町1-1', '053-123-4567', '山田浩二', 0, 4200, 2600, '部品の一次保管', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('四日市センター', '510-0075', '三重県四日市市諏訪栄町1-1', '059-123-4567', '佐々木愛', 0, 3800, 1100, '中部地区の予備センター', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('京都センター', '600-8216', '京都府京都市下京区東塩小路町1-1', '075-123-4567', '松本和也', 0, 3200, 1400, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('神戸センター', '650-0001', '兵庫県神戸市中央区加納町1-1-1', '078-123-4567', '井上真理', 0, 5200, 2000, '関西地区の補完拠点', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('岡山センター', '700-0024', '岡山県岡山市北区駅元町1-1', '086-123-4567', '木村拓也', 0, 3600, 800, '中国地方の拠点', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('広島センター', '732-0822', '広島県広島市南区松原町1-1', '082-123-4567', '林幸子', 0, 4800, 2200, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('高松センター', '760-0011', '香川県高松市浜ノ町1-1', '087-123-4567', '清水俊介', 1, 2500, 0, '契約更新のため一時停止中', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('福岡センター', '812-0012', '福岡県福岡市博多区博多駅中央街1-1', '092-123-4567', '斎藤洋子', 0, 9000, 4100, '九州地区の主要センター', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('熊本センター', '860-0047', '熊本県熊本市西区春日3-1-1', '096-123-4567', '森田隆', 0, 4000, 1700, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('鹿児島センター', '890-0053', '鹿児島県鹿児島市中央町1-1', '099-123-4567', '池田里奈', 0, 3000, 700, '南九州の拠点', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('沖縄センター', '904-0115', '沖縄県中頭郡北谷町美浜1-1', '098-123-4567', '原田孝', 1, NULL, NULL, '開設準備中（容量未定）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 部品在庫（38 件。各センター 2 件）
INSERT INTO parts_stock_info (category_id, name, center_id, description, amount, delete_flag, create_date, update_date) VALUES
(5,  'LiPo 3S 2200mAh',        (SELECT center_id FROM center_info WHERE center_name = '札幌センター'),     '11.1V リチウムポリマーバッテリー',   90, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2,  '9インチプロペラ',         (SELECT center_id FROM center_info WHERE center_name = '札幌センター'),     'カーボン製プロペラ（CW/CCW）',       240, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4,  '30A ESC',                (SELECT center_id FROM center_info WHERE center_name = '仙台センター'),     '30アンペアESC（BLHeli_S）',          130, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6,  'Pixhawk 6C',             (SELECT center_id FROM center_info WHERE center_name = '仙台センター'),     'PX4フライトコントローラー',           40, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1,  'カーボンフレーム 450mm',  (SELECT center_id FROM center_info WHERE center_name = 'さいたまセンター'), 'クアッドコプター用カーボンフレーム',   55, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3,  'ブラシレスモーター 2212', (SELECT center_id FROM center_info WHERE center_name = 'さいたまセンター'), '920KV ブラシレスモーター',           110, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2,  '9インチプロペラ',         (SELECT center_id FROM center_info WHERE center_name = '千葉センター'),     'カーボン製プロペラ（CW/CCW）',       200, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'FPVカメラ 1200TVL',      (SELECT center_id FROM center_info WHERE center_name = '千葉センター'),     'CMOS 1/3インチFPVカメラ',            160, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6,  'Pixhawk 6C',             (SELECT center_id FROM center_info WHERE center_name = '横浜センター'),     'PX4フライトコントローラー',           70, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9,  'u-blox NEO-M8N',         (SELECT center_id FROM center_info WHERE center_name = '横浜センター'),     'GPS/GLONASS対応モジュール',           95, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5,  'LiPo 3S 2200mAh',        (SELECT center_id FROM center_info WHERE center_name = '新潟センター'),     '11.1V リチウムポリマーバッテリー',   0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7,  'FrSky Taranis X9D+',     (SELECT center_id FROM center_info WHERE center_name = '新潟センター'),     '16ch送信機',                          0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3,  'ブラシレスモーター 2212', (SELECT center_id FROM center_info WHERE center_name = '金沢センター'),     '920KV ブラシレスモーター',            60, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8,  'FrSky X8R',              (SELECT center_id FROM center_info WHERE center_name = '金沢センター'),     '8ch受信機（テレメトリー対応）',       75, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1,  'カーボンフレーム 450mm',  (SELECT center_id FROM center_info WHERE center_name = '静岡センター'),     'クアッドコプター用カーボンフレーム',   35, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4,  '30A ESC',                (SELECT center_id FROM center_info WHERE center_name = '静岡センター'),     '30アンペアESC（BLHeli_S）',          100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3,  'ブラシレスモーター 2212', (SELECT center_id FROM center_info WHERE center_name = '浜松センター'),     '920KV ブラシレスモーター',           180, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5,  'LiPo 3S 2200mAh',        (SELECT center_id FROM center_info WHERE center_name = '浜松センター'),     '11.1V リチウムポリマーバッテリー',   140, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4,  '30A ESC',                (SELECT center_id FROM center_info WHERE center_name = '四日市センター'),   '30アンペアESC（BLHeli_S）',           85, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2,  '9インチプロペラ',         (SELECT center_id FROM center_info WHERE center_name = '四日市センター'),   'カーボン製プロペラ（CW/CCW）',       150, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'FPVカメラ 1200TVL',      (SELECT center_id FROM center_info WHERE center_name = '京都センター'),     'CMOS 1/3インチFPVカメラ',             90, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9,  'u-blox NEO-M8N',         (SELECT center_id FROM center_info WHERE center_name = '京都センター'),     'GPS/GLONASS対応モジュール',           50, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8,  'FrSky X8R',              (SELECT center_id FROM center_info WHERE center_name = '神戸センター'),     '8ch受信機（テレメトリー対応）',       65, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7,  'FrSky Taranis X9D+',     (SELECT center_id FROM center_info WHERE center_name = '神戸センター'),     '16ch送信機',                          20, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1,  'カーボンフレーム 450mm',  (SELECT center_id FROM center_info WHERE center_name = '岡山センター'),     'クアッドコプター用カーボンフレーム',   30, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6,  'Pixhawk 6C',             (SELECT center_id FROM center_info WHERE center_name = '岡山センター'),     'PX4フライトコントローラー',           45, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2,  '9インチプロペラ',         (SELECT center_id FROM center_info WHERE center_name = '広島センター'),     'カーボン製プロペラ（CW/CCW）',       170, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3,  'ブラシレスモーター 2212', (SELECT center_id FROM center_info WHERE center_name = '広島センター'),     '920KV ブラシレスモーター',            95, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5,  'LiPo 3S 2200mAh',        (SELECT center_id FROM center_info WHERE center_name = '高松センター'),     '11.1V リチウムポリマーバッテリー',   0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8,  'FrSky X8R',              (SELECT center_id FROM center_info WHERE center_name = '高松センター'),     '8ch受信機（テレメトリー対応）',       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6,  'Pixhawk 6C',             (SELECT center_id FROM center_info WHERE center_name = '福岡センター'),     'PX4フライトコントローラー',          125, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'FPVカメラ 1200TVL',      (SELECT center_id FROM center_info WHERE center_name = '福岡センター'),     'CMOS 1/3インチFPVカメラ',            210, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9,  'u-blox NEO-M8N',         (SELECT center_id FROM center_info WHERE center_name = '熊本センター'),     'GPS/GLONASS対応モジュール',           70, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4,  '30A ESC',                (SELECT center_id FROM center_info WHERE center_name = '熊本センター'),     '30アンペアESC（BLHeli_S）',           80, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1,  'カーボンフレーム 450mm',  (SELECT center_id FROM center_info WHERE center_name = '鹿児島センター'),   'クアッドコプター用カーボンフレーム',   25, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5,  'LiPo 3S 2200mAh',        (SELECT center_id FROM center_info WHERE center_name = '鹿児島センター'),   '11.1V リチウムポリマーバッテリー',   60, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2,  '9インチプロペラ',         (SELECT center_id FROM center_info WHERE center_name = '沖縄センター'),     'カーボン製プロペラ（CW/CCW）',        50, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'FPVカメラ 1200TVL',      (SELECT center_id FROM center_info WHERE center_name = '沖縄センター'),     'CMOS 1/3インチFPVカメラ',             30, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 製品在庫（19 件。各センター 1 件）
INSERT INTO products_stock_info (category_id, name, center_id, description, amount, delete_flag, create_date, update_date) VALUES
(14, '農業用ドローン-Z Sprayer',   (SELECT center_id FROM center_info WHERE center_name = '札幌センター'),     '農薬散布専用10L タンク搭載',         18, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, '災害用ドローン-X Lite',      (SELECT center_id FROM center_info WHERE center_name = '仙台センター'),     '軽量コンパクト災害調査ドローン',     14, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, '点検用ドローン-Y Standard',  (SELECT center_id FROM center_info WHERE center_name = 'さいたまセンター'), 'インフラ点検専用標準モデル',         20, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, '撮影用ドローン Cinema',      (SELECT center_id FROM center_info WHERE center_name = '千葉センター'),     '4K/60fps プロ仕様空撮ドローン',      12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, '配送用ドローン Express',     (SELECT center_id FROM center_info WHERE center_name = '横浜センター'),     '最大2kg積載小型配送ドローン',        28, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, '農業用ドローン-Z Sprayer',   (SELECT center_id FROM center_info WHERE center_name = '新潟センター'),     '農薬散布専用10L タンク搭載',          0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, '点検用ドローン-Y Standard',  (SELECT center_id FROM center_info WHERE center_name = '金沢センター'),     'インフラ点検専用標準モデル',         10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(15, '警備用ドローン Patrol',      (SELECT center_id FROM center_info WHERE center_name = '静岡センター'),     '夜間撮影対応セキュリティドローン',   16, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, '配送用ドローン Express',     (SELECT center_id FROM center_info WHERE center_name = '浜松センター'),     '最大2kg積載小型配送ドローン',        24, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, '点検用ドローン-Y Standard',  (SELECT center_id FROM center_info WHERE center_name = '四日市センター'),   'インフラ点検専用標準モデル',         11, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, '撮影用ドローン Cinema',      (SELECT center_id FROM center_info WHERE center_name = '京都センター'),     '4K/60fps プロ仕様空撮ドローン',       9, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(15, '警備用ドローン Patrol',      (SELECT center_id FROM center_info WHERE center_name = '神戸センター'),     '夜間撮影対応セキュリティドローン',   13, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, '農業用ドローン-Z Sprayer',   (SELECT center_id FROM center_info WHERE center_name = '岡山センター'),     '農薬散布専用10L タンク搭載',          7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, '災害用ドローン-X Lite',      (SELECT center_id FROM center_info WHERE center_name = '広島センター'),     '軽量コンパクト災害調査ドローン',     17, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, '配送用ドローン Express',     (SELECT center_id FROM center_info WHERE center_name = '高松センター'),     '最大2kg積載小型配送ドローン',         0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, '配送用ドローン Express',     (SELECT center_id FROM center_info WHERE center_name = '福岡センター'),     '最大2kg積載小型配送ドローン',        35, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, '災害用ドローン-X Lite',      (SELECT center_id FROM center_info WHERE center_name = '熊本センター'),     '軽量コンパクト災害調査ドローン',     15, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, '農業用ドローン-Z Sprayer',   (SELECT center_id FROM center_info WHERE center_name = '鹿児島センター'),   '農薬散布専用10L タンク搭載',         12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, '撮影用ドローン Cinema',      (SELECT center_id FROM center_info WHERE center_name = '沖縄センター'),     '4K/60fps プロ仕様空撮ドローン',       4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
