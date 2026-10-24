import { z } from 'zod';
import type { Category, CategoryRequest, CategoryType } from '../types/api';

/** 入力値の上限。バックエンド（CategoryRequest）と一致させる。 */
export const CATEGORY_LIMITS = {
  categoryName: 20,
  description: 255,
} as const;

/** カテゴリ種別の表示名。 */
export const CATEGORY_TYPE_LABELS: Record<CategoryType, string> = {
  part: '部品',
  product: '製品',
};

/** フォームの入力値。 */
export interface CategoryFormValues {
  /** 未選択は空文字。 */
  categoryType: '' | CategoryType;
  categoryName: string;
  description: string;
}

/** エラーを表示する項目。 */
export type CategoryFormField = keyof CategoryFormValues;

/** 項目ごとのエラーメッセージ。 */
export type CategoryFormErrors = Partial<Record<CategoryFormField, string>>;

/** 検証の結果。 */
export type CategoryFormResult =
  | { ok: true; request: CategoryRequest }
  | { ok: false; errors: CategoryFormErrors };

const NO_ANGLE_BRACKETS = /^[^<>]*$/;

/** フォームの検証スキーマ。メッセージはバックエンドと同じ文言にそろえる。 */
export const categoryFormSchema = z.object({
  categoryType: z.enum(['part', 'product'], { errorMap: () => ({ message: 'カテゴリ種別は必須です' }) }),
  categoryName: z
    .string()
    .min(1, 'カテゴリ名は必須です')
    .max(CATEGORY_LIMITS.categoryName, `カテゴリ名は${CATEGORY_LIMITS.categoryName}文字以内で入力してください`)
    .regex(NO_ANGLE_BRACKETS, 'カテゴリ名に < または > は使用できません'),
  description: z
    .string()
    .max(CATEGORY_LIMITS.description, `説明は${CATEGORY_LIMITS.description}文字以内で入力してください`)
    .regex(NO_ANGLE_BRACKETS, '説明に < または > は使用できません'),
});

/** 新規作成時のフォームの初期値。 */
export const EMPTY_CATEGORY_FORM: CategoryFormValues = {
  categoryType: '',
  categoryName: '',
  description: '',
};

/**
 * カテゴリからフォームの初期値を作る（編集時）。
 *
 * @param category カテゴリ
 * @returns フォームの初期値
 */
export function categoryToFormValues(category: Category): CategoryFormValues {
  return {
    categoryType: category.categoryType,
    categoryName: category.categoryName,
    description: category.description ?? '',
  };
}

/**
 * フォームの入力値を検証し、API に送るリクエストへ変換する。
 *
 * @param values 入力値
 * @returns 成功時はリクエスト、失敗時は項目ごとのエラー
 */
export function validateCategoryForm(values: CategoryFormValues): CategoryFormResult {
  const parsed = categoryFormSchema.safeParse({
    categoryType: values.categoryType,
    categoryName: values.categoryName.trim(),
    description: values.description.trim(),
  });
  if (!parsed.success) {
    const errors: CategoryFormErrors = {};
    for (const issue of parsed.error.issues) {
      const key = issue.path[0] as CategoryFormField;
      // 同じ項目に複数のエラーがあるときは、最初のものを表示する
      if (key && !errors[key]) errors[key] = issue.message;
    }
    return { ok: false, errors };
  }
  const v = parsed.data;
  return {
    ok: true,
    request: {
      categoryType: v.categoryType,
      categoryName: v.categoryName,
      description: v.description === '' ? null : v.description,
    },
  };
}

/** サーバーのエラーメッセージを、入力項目に対応づけるための手がかり（項目名で始まるメッセージ）。 */
const SERVER_ERROR_FIELDS: Array<[string, CategoryFormField]> = [
  ['カテゴリ種別', 'categoryType'],
  ['カテゴリ名', 'categoryName'],
  ['説明', 'description'],
];

/**
 * サーバーのエラーメッセージから、対応する入力項目を推定する。
 * 該当しないとき（使用中のカテゴリの種別変更など）は undefined を返し、フォーム全体のエラーとして表示する。
 *
 * @param message サーバーのエラーメッセージ
 * @returns 対応する項目
 */
export function guessCategoryErrorField(message: string): CategoryFormField | undefined {
  return SERVER_ERROR_FIELDS.find(([label]) => message.startsWith(label))?.[1];
}
