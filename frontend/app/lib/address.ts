/**
 * 住所の分割・結合ユーティリティ。
 *
 * バックエンドの住所は 1 項目（address、255 文字以内）。画面では
 * 「都道府県」「市区町村」「番地・建物名」の 3 項目で入力させ、送信時に結合する。
 * 既存データの編集時は、ここで 3 項目に分割して入力欄へ戻す。
 * 分割は先頭からの判定による簡易的なもの（docs/frontend-centers-design.md の C-4）。
 * 分割できなかった部分は「番地・建物名」に残り、住所の文字列そのものは変わらない。
 */

/** 都道府県の一覧（JIS X 0401 の順）。 */
export const PREFECTURES = [
  '北海道', '青森県', '岩手県', '宮城県', '秋田県', '山形県', '福島県',
  '茨城県', '栃木県', '群馬県', '埼玉県', '千葉県', '東京都', '神奈川県',
  '新潟県', '富山県', '石川県', '福井県', '山梨県', '長野県', '岐阜県',
  '静岡県', '愛知県', '三重県', '滋賀県', '京都府', '大阪府', '兵庫県',
  '奈良県', '和歌山県', '鳥取県', '島根県', '岡山県', '広島県', '山口県',
  '徳島県', '香川県', '愛媛県', '高知県', '福岡県', '佐賀県', '長崎県',
  '熊本県', '大分県', '宮崎県', '鹿児島県', '沖縄県',
] as const;

/** 分割した住所。 */
export interface AddressParts {
  /** 都道府県（判定できなければ空文字）。 */
  prefecture: string;
  /** 市区町村（判定できなければ空文字）。政令指定都市は「大阪市北区」のように区まで含む。 */
  city: string;
  /** 番地・建物名など、残りの部分。 */
  street: string;
}

/** 市区町村の末尾になる文字。 */
const CITY_REGEX = /^(.+?[市区町村郡])([\s\S]*)$/;
/** 政令指定都市の区（市の直後に続く「〇〇区」）。区名は 1〜4 文字。 */
const WARD_REGEX = /^([^市区町村郡0-9０-９丁\s-]{1,4}?区)([\s\S]*)$/;
/** 「四日市市」「大町市」のように、名前の途中の市町村が先に一致したときの続き。 */
const REPEAT_REGEX = /^([市町村])([\s\S]*)$/;
/** 「〇〇郡△△町」のうち、郡の次に続く町村。 */
const COUNTY_TOWN_REGEX = /^([^市区町村郡0-9０-９\s-]+?[町村])([\s\S]*)$/;

/**
 * 住所を「都道府県」「市区町村」「番地・建物名」に分割する。
 * 結合し直すと元の文字列に戻る（空白は前後のみ除去）。
 *
 * @param address 住所
 * @returns 分割結果
 */
export function splitAddress(address: string): AddressParts {
  const text = (address ?? '').trim();
  const prefecture = PREFECTURES.find((p) => text.startsWith(p)) ?? '';
  const rest = text.slice(prefecture.length);

  const match = CITY_REGEX.exec(rest);
  if (!match) {
    return { prefecture, city: '', street: rest };
  }

  let city = match[1];
  let street = match[2];

  // 「四日市市」「大町市」のように名前の途中で一致した場合は、続く市町村までを含める
  const repeat = REPEAT_REGEX.exec(street);
  if (repeat) {
    city += repeat[1];
    street = repeat[2];
  }

  if (city.endsWith('市')) {
    // 政令指定都市は区までを市区町村とする
    const ward = WARD_REGEX.exec(street);
    if (ward) {
      city += ward[1];
      street = ward[2];
    }
  } else if (city.endsWith('郡')) {
    // 郡は町村までを市区町村とする
    const town = COUNTY_TOWN_REGEX.exec(street);
    if (town) {
      city += town[1];
      street = town[2];
    }
  }

  return { prefecture, city, street };
}

/**
 * 3 項目を結合して住所の文字列にする。各項目の前後の空白は除く。
 *
 * @param parts 分割した住所
 * @returns 住所
 */
export function joinAddress(parts: AddressParts): string {
  return `${parts.prefecture}${parts.city.trim()}${parts.street.trim()}`.trim();
}

/**
 * 一覧に表示する住所（都道府県＋市区町村）を返す。
 * 市区町村を判定できないときは、住所の全体を返す。
 *
 * @param address 住所
 * @returns 表示用の住所
 */
export function summarizeAddress(address: string): string {
  const { prefecture, city } = splitAddress(address);
  const summary = `${prefecture}${city}`;
  return city ? summary : (address ?? '').trim();
}
