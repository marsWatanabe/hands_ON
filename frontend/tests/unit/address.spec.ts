import { test, expect } from '@playwright/test';
import { PREFECTURES, joinAddress, splitAddress, summarizeAddress } from '../../app/lib/address';

/**
 * 住所ユーティリティ（app/lib/address.ts）の単体テスト。
 * 都道府県・市区町村・番地の分割と結合、一覧表示用の要約を確認する。
 */

test.describe('splitAddress', () => {
  /** 通常の市と、市名に「市」を含む例、政令指定都市の区、郡と町村を、期待どおりに分けること。 */
  const cases: Array<[string, string, string, string]> = [
    ['大阪府大阪市北区梅田1-1-1', '大阪府', '大阪市北区', '梅田1-1-1'],
    ['東京都千代田区丸の内1-1', '東京都', '千代田区', '丸の内1-1'],
    ['北海道札幌市中央区北1条西2丁目', '北海道', '札幌市中央区', '北1条西2丁目'],
    ['神奈川県川崎市川崎区駅前本町1', '神奈川県', '川崎市川崎区', '駅前本町1'],
    ['京都府京都市中京区烏丸通1', '京都府', '京都市中京区', '烏丸通1'],
    ['三重県四日市市諏訪町1-5', '三重県', '四日市市', '諏訪町1-5'],
    ['長野県大町市大町1-1', '長野県', '大町市', '大町1-1'],
    ['東京都町田市森野2-2', '東京都', '町田市', '森野2-2'],
    ['北海道上川郡美瑛町大町1-1', '北海道', '上川郡美瑛町', '大町1-1'],
    ['沖縄県中頭郡北谷町美浜1', '沖縄県', '中頭郡北谷町', '美浜1'],
  ];
  for (const [input, prefecture, city, street] of cases) {
    test(`「${input}」を分割できる`, () => {
      expect(splitAddress(input)).toEqual({ prefecture, city, street });
    });
  }

  test('都道府県がない住所は、都道府県を空にして市区町村から分ける', () => {
    expect(splitAddress('大阪市北区梅田1-1-1')).toEqual({ prefecture: '', city: '大阪市北区', street: '梅田1-1-1' });
  });

  test('都道府県だけの住所は、市区町村と番地が空になる', () => {
    expect(splitAddress('東京都')).toEqual({ prefecture: '東京都', city: '', street: '' });
  });

  test('市区町村を判定できない住所は、残りを番地に入れる', () => {
    expect(splitAddress('大阪府テスト倉庫')).toEqual({ prefecture: '大阪府', city: '', street: 'テスト倉庫' });
  });

  test('空文字と前後の空白を扱える', () => {
    expect(splitAddress('')).toEqual({ prefecture: '', city: '', street: '' });
    expect(splitAddress('  東京都千代田区丸の内1-1  ').street).toBe('丸の内1-1');
  });

  test('すべての都道府県を先頭から判定できる', () => {
    expect(PREFECTURES).toHaveLength(47);
    for (const p of PREFECTURES) {
      expect(splitAddress(`${p}テスト市1-1`).prefecture).toBe(p);
    }
  });

  test('分割して結合すると元の住所に戻る', () => {
    for (const [input] of [
      ['大阪府大阪市北区梅田1-1-1'],
      ['長野県大町市大町1-1'],
      ['北海道上川郡美瑛町大町1-1'],
      ['大阪府テスト倉庫'],
    ]) {
      expect(joinAddress(splitAddress(input))).toBe(input);
    }
  });
});

test.describe('joinAddress', () => {
  test('3 項目を区切りなしで結合し、各項目の前後の空白を除く', () => {
    expect(joinAddress({ prefecture: '大阪府', city: ' 大阪市北区 ', street: ' 梅田1-1-1 ' })).toBe('大阪府大阪市北区梅田1-1-1');
  });

  test('都道府県だけでも結合できる', () => {
    expect(joinAddress({ prefecture: '東京都', city: '', street: '' })).toBe('東京都');
  });
});

test.describe('summarizeAddress', () => {
  test('都道府県と市区町村までを返す', () => {
    expect(summarizeAddress('大阪府大阪市北区梅田1-1-1')).toBe('大阪府大阪市北区');
  });

  test('市区町村を判定できないときは住所の全体を返す', () => {
    expect(summarizeAddress('大阪府テスト倉庫')).toBe('大阪府テスト倉庫');
    expect(summarizeAddress('東京都')).toBe('東京都');
  });
});
