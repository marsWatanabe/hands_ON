import { test, expect } from '@playwright/test';
import {
  EMPTY_STOCK_FORM,
  conflictFieldOf,
  guessStockErrorField,
  stockLabel,
  stockToFormValues,
  validateAmountRange,
  validateStockForm,
  type StockFormValues,
} from '../../app/lib/stock-form';
import type { Stock } from '../../app/types/api';

/**
 * 在庫フォームの検証（app/lib/stock-form.ts）の単体テスト。
 * バックエンド（PartsStockRequest、ProductsStockRequest）と同じ規則・文言で検証できること、
 * リクエストへの変換、製品コードの扱い、サーバーのエラーメッセージの項目推定、検索の個数範囲の検証を確認する。
 */

/** 正常な入力値。各テストで一部だけ変える。 */
const valid: StockFormValues = {
  productCode: 'PRD-000001',
  categoryId: '11',
  name: '災害用ドローン-X',
  centerId: '2',
  description: '説明',
  amount: '12',
};

/** 1 項目だけ変えた入力値を検証し、エラーを返す（エラーがなければ空のオブジェクト）。 */
function errorsOf(kind: 'parts' | 'products', patch: Partial<StockFormValues>) {
  const result = validateStockForm(kind, { ...valid, ...patch });
  return result.ok ? {} : result.errors;
}

test.describe('validateStockForm: 正常系', () => {
  test('部品在庫は、製品コードを含めずに API のリクエストへ変換される', () => {
    const result = validateStockForm('parts', valid);
    expect(result).toEqual({
      ok: true,
      request: { categoryId: 11, name: '災害用ドローン-X', centerId: 2, description: '説明', amount: 12 },
    });
  });

  test('製品在庫は、製品コードを含めて変換される', () => {
    const result = validateStockForm('products', valid);
    expect(result.ok && result.request).toMatchObject({ productCode: 'PRD-000001', categoryId: 11, amount: 12 });
  });

  test('説明が空なら null で送り、在庫数 0 を受け付ける', () => {
    const result = validateStockForm('parts', { ...valid, description: '', amount: '0' });
    expect(result.ok && result.request).toMatchObject({ description: null, amount: 0 });
  });

  test('前後の空白を除いて送る', () => {
    const result = validateStockForm('products', {
      ...valid,
      productCode: ' PRD-1 ',
      name: '  名称 ',
      amount: ' 5 ',
    });
    expect(result.ok && result.request).toMatchObject({ productCode: 'PRD-1', name: '名称', amount: 5 });
  });

  test('部品在庫では、製品コードが空でも不正な値でも検証しない', () => {
    expect(errorsOf('parts', { productCode: '' })).toEqual({});
    expect(errorsOf('parts', { productCode: '日本語' })).toEqual({});
  });
});

test.describe('validateStockForm: 必須項目', () => {
  test('空のフォームは、必須項目のエラーになる（部品在庫）', () => {
    const result = validateStockForm('parts', EMPTY_STOCK_FORM);
    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(Object.keys(result.errors).sort()).toEqual(['amount', 'categoryId', 'centerId', 'name'].sort());
      expect(result.errors.categoryId).toBe('カテゴリは必須です');
      expect(result.errors.centerId).toBe('在庫センターは必須です');
      expect(result.errors.name).toBe('名称は必須です');
      expect(result.errors.amount).toBe('在庫数は必須です');
    }
  });

  test('製品在庫では、製品コードも必須になる', () => {
    expect(errorsOf('products', { productCode: '' }).productCode).toBe('製品コードは必須です');
    expect(errorsOf('products', { productCode: '   ' }).productCode).toBe('製品コードは必須です');
  });

  test('空白だけの名称は未入力として扱う', () => {
    expect(errorsOf('parts', { name: '   ' }).name).toBe('名称は必須です');
  });
});

test.describe('validateStockForm: 文字数の境界', () => {
  test('名称は 255 文字まで', () => {
    expect(errorsOf('parts', { name: 'あ'.repeat(255) })).toEqual({});
    expect(errorsOf('parts', { name: 'あ'.repeat(256) }).name).toBe('名称は255文字以内で入力してください');
  });

  test('説明は 255 文字まで', () => {
    expect(errorsOf('parts', { description: 'あ'.repeat(255) })).toEqual({});
    expect(errorsOf('parts', { description: 'あ'.repeat(256) }).description).toBe(
      '説明は255文字以内で入力してください',
    );
  });

  test('製品コードは 20 文字まで', () => {
    expect(errorsOf('products', { productCode: 'A'.repeat(20) })).toEqual({});
    expect(errorsOf('products', { productCode: 'A'.repeat(21) }).productCode).toBe(
      '製品コードは20文字以内で入力してください',
    );
  });
});

test.describe('validateStockForm: 形式', () => {
  test('製品コードは半角英数字、ハイフン、アンダースコアだけ', () => {
    for (const ok of ['PRD-000001', 'abc_DEF-123', '0']) {
      expect(errorsOf('products', { productCode: ok }).productCode).toBeUndefined();
    }
    for (const bad of ['製品コード', 'PRD 01', 'PRD/01', '<PRD>', 'ＰＲＤ']) {
      expect(errorsOf('products', { productCode: bad }).productCode).toBe(
        '製品コードは半角英数字、ハイフン、アンダースコアで入力してください',
      );
    }
  });

  test('< と > を含む入力はエラー', () => {
    expect(errorsOf('parts', { name: 'a<b' }).name).toBe('名称に < または > は使用できません');
    expect(errorsOf('parts', { description: '1>0' }).description).toBe('説明に < または > は使用できません');
  });

  test('在庫数は 0 から 999,999,999 までの整数', () => {
    expect(errorsOf('parts', { amount: '0' })).toEqual({});
    expect(errorsOf('parts', { amount: '999999999' })).toEqual({});
    expect(errorsOf('parts', { amount: '1000000000' }).amount).toBe('在庫数が大きすぎます');
    for (const bad of ['-1', '1.5', 'abc', '1e3', '１２']) {
      expect(errorsOf('parts', { amount: bad }).amount).toBe('在庫数は 0 以上の整数で入力してください');
    }
  });
});

test.describe('stockToFormValues', () => {
  const stock: Stock = {
    stockId: 1,
    productCode: 'PRD-000001',
    categoryId: 11,
    categoryName: '災害用ドローン',
    name: '災害用ドローン-X',
    centerId: 2,
    centerName: '大阪センター',
    description: null,
    amount: 0,
    createDate: '2026-10-21T00:00:00',
    updateDate: '2026-10-21T00:00:00',
  };

  test('API の値を文字列のフォームの値にし、null は空文字にする', () => {
    expect(stockToFormValues(stock)).toEqual({
      productCode: 'PRD-000001',
      categoryId: '11',
      name: '災害用ドローン-X',
      centerId: '2',
      description: '',
      amount: '0',
    });
  });

  test('カテゴリ・センター・数量が未設定の既存行は、空の選択として表示する', () => {
    const orphan: Stock = { ...stock, productCode: undefined, categoryId: null, centerId: null, amount: null };
    expect(stockToFormValues(orphan)).toMatchObject({ productCode: '', categoryId: '', centerId: '', amount: '' });
  });

  test('変更せずに保存すると、元の値と同じリクエストになる', () => {
    const result = validateStockForm('products', stockToFormValues(stock));
    expect(result.ok && result.request).toEqual({
      productCode: 'PRD-000001',
      categoryId: 11,
      name: '災害用ドローン-X',
      centerId: 2,
      description: null,
      amount: 0,
    });
  });
});

test.describe('サーバーのエラーメッセージの項目推定', () => {
  test('項目名で始まるメッセージから項目を推定する', () => {
    expect(guessStockErrorField('カテゴリが見つかりません。ID: 9')).toBe('categoryId');
    expect(guessStockErrorField('カテゴリは部品用のカテゴリを指定してください。')).toBe('categoryId');
    expect(guessStockErrorField('在庫センターが見つかりません。ID: 9')).toBe('centerId');
    expect(guessStockErrorField('名称は必須です')).toBe('name');
    expect(guessStockErrorField('在庫数が大きすぎます')).toBe('amount');
    expect(guessStockErrorField('製品コードは必須です')).toBe('productCode');
  });

  test('推定できないメッセージは undefined', () => {
    expect(guessStockErrorField('サーバーでエラーが発生しました。')).toBeUndefined();
    expect(guessStockErrorField('')).toBeUndefined();
  });

  test('重複（409）は、製品コードの重複なら製品コード、それ以外は名称に対応づける', () => {
    expect(conflictFieldOf('同じ製品コードが既に登録されています。')).toBe('productCode');
    expect(conflictFieldOf('同じ在庫センターに同じ名称の部品在庫が既に登録されています。')).toBe('name');
  });
});

test.describe('validateAmountRange（検索の個数）', () => {
  test('空欄、片方だけ、同じ値、範囲内はエラーにならない', () => {
    expect(validateAmountRange('', '')).toEqual({});
    expect(validateAmountRange('10', '')).toEqual({});
    expect(validateAmountRange('', '10')).toEqual({});
    expect(validateAmountRange('5', '5')).toEqual({});
    expect(validateAmountRange(' 1 ', ' 9 ')).toEqual({});
    expect(validateAmountRange('0', '0')).toEqual({});
  });

  test('以上が以下を超えるとエラー', () => {
    expect(validateAmountRange('10', '9').amountMin).toBe('個数（以上）は個数（以下）以下にしてください');
  });

  test('整数以外（負数、小数、文字）は項目ごとにエラー', () => {
    expect(validateAmountRange('-1', '').amountMin).toBe('個数（以上）は 0 以上の整数で入力してください');
    expect(validateAmountRange('', '1.5').amountMax).toBe('個数（以下）は 0 以上の整数で入力してください');
    expect(validateAmountRange('a', 'b')).toEqual({
      amountMin: '個数（以上）は 0 以上の整数で入力してください',
      amountMax: '個数（以下）は 0 以上の整数で入力してください',
    });
  });
});

test.describe('stockLabel', () => {
  const base = { name: 'LiPo 3S 2200mAh' } as Stock;

  test('センター名があれば名称に添え、なければ名称だけを返す', () => {
    expect(stockLabel({ ...base, centerName: '東京センター' })).toBe('LiPo 3S 2200mAh（東京センター）');
    expect(stockLabel({ ...base, centerName: null })).toBe('LiPo 3S 2200mAh');
  });
});
