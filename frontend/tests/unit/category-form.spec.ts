import { test, expect } from '@playwright/test';
import {
  EMPTY_CATEGORY_FORM,
  categoryToFormValues,
  guessCategoryErrorField,
  validateCategoryForm,
  type CategoryFormValues,
} from '../../app/lib/category-form';
import type { Category } from '../../app/types/api';

/**
 * カテゴリフォームの検証（app/lib/category-form.ts）の単体テスト。
 * バックエンド（CategoryRequest）と同じ規則・文言で検証できること、リクエストへの変換、
 * サーバーのエラーメッセージの項目推定を確認する。
 */

const valid: CategoryFormValues = { categoryType: 'part', categoryName: 'フレーム', description: '機体フレーム' };

function errorsOf(patch: Partial<CategoryFormValues>) {
  const result = validateCategoryForm({ ...valid, ...patch });
  return result.ok ? {} : result.errors;
}

test.describe('validateCategoryForm', () => {
  test('正しい入力は API のリクエストに変換される', () => {
    expect(validateCategoryForm(valid)).toEqual({
      ok: true,
      request: { categoryType: 'part', categoryName: 'フレーム', description: '機体フレーム' },
    });
  });

  test('説明が空なら null で送り、前後の空白を除く', () => {
    const result = validateCategoryForm({ categoryType: 'product', categoryName: '  災害用 ', description: '  ' });
    expect(result.ok && result.request).toEqual({ categoryType: 'product', categoryName: '災害用', description: null });
  });

  test('空のフォームは、種別とカテゴリ名の必須エラーになる', () => {
    const result = validateCategoryForm(EMPTY_CATEGORY_FORM);
    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.errors).toEqual({
        categoryType: 'カテゴリ種別は必須です',
        categoryName: 'カテゴリ名は必須です',
      });
    }
  });

  test('空白だけのカテゴリ名は未入力として扱う', () => {
    expect(errorsOf({ categoryName: '   ' }).categoryName).toBe('カテゴリ名は必須です');
  });

  test('カテゴリ名は 20 文字まで', () => {
    expect(errorsOf({ categoryName: 'あ'.repeat(20) })).toEqual({});
    expect(errorsOf({ categoryName: 'あ'.repeat(21) }).categoryName).toBe('カテゴリ名は20文字以内で入力してください');
  });

  test('説明は 255 文字まで（DB の桁数）', () => {
    expect(errorsOf({ description: 'あ'.repeat(255) })).toEqual({});
    expect(errorsOf({ description: 'あ'.repeat(256) }).description).toBe('説明は255文字以内で入力してください');
  });

  test('< と > を含む入力はエラー', () => {
    expect(errorsOf({ categoryName: '<b>' }).categoryName).toBe('カテゴリ名に < または > は使用できません');
    expect(errorsOf({ description: 'a>b' }).description).toBe('説明に < または > は使用できません');
  });

  test('種別は part または product だけ', () => {
    expect(errorsOf({ categoryType: 'product' })).toEqual({});
    expect(errorsOf({ categoryType: 'other' as unknown as 'part' }).categoryType).toBe('カテゴリ種別は必須です');
  });
});

test.describe('categoryToFormValues', () => {
  const category: Category = {
    categoryId: 1,
    categoryType: 'product',
    categoryName: '災害用ドローン',
    description: null,
    createDate: '2026-10-21T00:00:00',
    updateDate: '2026-10-21T00:00:00',
  };

  test('null の説明は空文字にし、変更せずに保存すると同じリクエストになる', () => {
    const values = categoryToFormValues(category);
    expect(values).toEqual({ categoryType: 'product', categoryName: '災害用ドローン', description: '' });
    const result = validateCategoryForm(values);
    expect(result.ok && result.request).toEqual({
      categoryType: 'product',
      categoryName: '災害用ドローン',
      description: null,
    });
  });
});

test.describe('guessCategoryErrorField', () => {
  test('項目名で始まるメッセージから項目を推定する', () => {
    expect(guessCategoryErrorField('カテゴリ種別は必須です')).toBe('categoryType');
    expect(guessCategoryErrorField('カテゴリ名は20文字以内で入力してください')).toBe('categoryName');
    expect(guessCategoryErrorField('説明は255文字以内で入力してください')).toBe('description');
  });

  test('推定できないメッセージ（使用中の種別変更など）は undefined', () => {
    expect(guessCategoryErrorField('カテゴリは在庫で使用されているため、種別を変更できません。')).toBeUndefined();
    expect(guessCategoryErrorField('')).toBeUndefined();
  });
});
