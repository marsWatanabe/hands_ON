import { z } from 'zod';
import type { Stock, StockKind, StockRequest } from '../types/api';

/** 入力値の上限。バックエンド（PartsStockRequest、ProductsStockRequest）と一致させる。 */
export const STOCK_LIMITS = {
  productCode: 20,
  name: 255,
  description: 255,
  amount: 999_999_999,
} as const;

/** 在庫の種類ごとの表示名。 */
export const STOCK_KIND_LABELS: Record<StockKind, { title: string; item: string }> = {
  parts: { title: '部品在庫一覧', item: '部品在庫' },
  products: { title: '製品在庫一覧', item: '製品在庫' },
};

/** フォームの入力値（すべて文字列で保持する）。 */
export interface StockFormValues {
  /** 製品在庫のみ。 */
  productCode: string;
  /** 未選択は空文字。 */
  categoryId: string;
  name: string;
  /** 未選択は空文字。 */
  centerId: string;
  description: string;
  amount: string;
}

/** エラーを表示する項目。 */
export type StockFormField = keyof StockFormValues;

/** 項目ごとのエラーメッセージ。 */
export type StockFormErrors = Partial<Record<StockFormField, string>>;

/** 検証の結果。 */
export type StockFormResult =
  | { ok: true; request: StockRequest }
  | { ok: false; errors: StockFormErrors };

const NO_ANGLE_BRACKETS = /^[^<>]*$/;
const DIGITS = /^\d+$/;

/** 在庫のフォームの検証スキーマ。メッセージはバックエンドと同じ文言にそろえる。 */
const stockFormSchema = z.object({
  productCode: z.string(),
  categoryId: z.string().min(1, 'カテゴリは必須です'),
  name: z
    .string()
    .min(1, '名称は必須です')
    .max(STOCK_LIMITS.name, `名称は${STOCK_LIMITS.name}文字以内で入力してください`)
    .regex(NO_ANGLE_BRACKETS, '名称に < または > は使用できません'),
  centerId: z.string().min(1, '在庫センターは必須です'),
  description: z
    .string()
    .max(STOCK_LIMITS.description, `説明は${STOCK_LIMITS.description}文字以内で入力してください`)
    .regex(NO_ANGLE_BRACKETS, '説明に < または > は使用できません'),
  amount: z
    .string()
    .min(1, '在庫数は必須です')
    .refine((v) => v === '' || DIGITS.test(v), '在庫数は 0 以上の整数で入力してください')
    .refine((v) => !DIGITS.test(v) || Number(v) <= STOCK_LIMITS.amount, '在庫数が大きすぎます'),
});

/** 製品コードの検証（製品在庫のみ）。 */
const productCodeSchema = z
  .string()
  .min(1, '製品コードは必須です')
  .max(STOCK_LIMITS.productCode, `製品コードは${STOCK_LIMITS.productCode}文字以内で入力してください`)
  .regex(/^[A-Za-z0-9_-]*$/, '製品コードは半角英数字、ハイフン、アンダースコアで入力してください');

/** 新規作成時のフォームの初期値。 */
export const EMPTY_STOCK_FORM: StockFormValues = {
  productCode: '',
  categoryId: '',
  name: '',
  centerId: '',
  description: '',
  amount: '',
};

/**
 * 在庫からフォームの初期値を作る（編集時）。
 *
 * @param stock 在庫
 * @returns フォームの初期値
 */
export function stockToFormValues(stock: Stock): StockFormValues {
  return {
    productCode: stock.productCode ?? '',
    categoryId: stock.categoryId === null ? '' : String(stock.categoryId),
    name: stock.name,
    centerId: stock.centerId === null ? '' : String(stock.centerId),
    description: stock.description ?? '',
    amount: stock.amount === null ? '' : String(stock.amount),
  };
}

/**
 * フォームの入力値を検証し、API に送るリクエストへ変換する。
 *
 * @param kind 在庫の種類（製品在庫のときだけ製品コードを検証して送る）
 * @param values 入力値
 * @returns 成功時はリクエスト、失敗時は項目ごとのエラー
 */
export function validateStockForm(kind: StockKind, values: StockFormValues): StockFormResult {
  const trimmed: StockFormValues = {
    productCode: values.productCode.trim(),
    categoryId: values.categoryId,
    name: values.name.trim(),
    centerId: values.centerId,
    description: values.description.trim(),
    amount: values.amount.trim(),
  };
  const errors: StockFormErrors = {};
  const base = stockFormSchema.safeParse(trimmed);
  if (!base.success) {
    for (const issue of base.error.issues) {
      const key = issue.path[0] as StockFormField;
      // 同じ項目に複数のエラーがあるときは、最初のものを表示する
      if (key && !errors[key]) errors[key] = issue.message;
    }
  }
  if (kind === 'products') {
    const code = productCodeSchema.safeParse(trimmed.productCode);
    if (!code.success) errors.productCode = code.error.issues[0].message;
  }
  if (Object.keys(errors).length > 0) {
    return { ok: false, errors };
  }
  const request: StockRequest = {
    categoryId: Number(trimmed.categoryId),
    name: trimmed.name,
    centerId: Number(trimmed.centerId),
    description: trimmed.description === '' ? null : trimmed.description,
    amount: Number(trimmed.amount),
  };
  if (kind === 'products') request.productCode = trimmed.productCode;
  return { ok: true, request };
}

/** サーバーのエラーメッセージを、入力項目に対応づけるための手がかり（項目名で始まるメッセージ）。 */
const SERVER_ERROR_FIELDS: Array<[string, StockFormField]> = [
  ['製品コード', 'productCode'],
  ['カテゴリ', 'categoryId'],
  ['在庫センター', 'centerId'],
  ['名称', 'name'],
  ['説明', 'description'],
  ['在庫数', 'amount'],
];

/**
 * サーバーのエラーメッセージから、対応する入力項目を推定する。
 * 項目名で始まるメッセージ（例: 「カテゴリは部品用のカテゴリを指定してください。」）だけを対象にする。
 * 該当しないときは undefined を返し、フォーム全体のエラーとして表示する。
 *
 * @param message サーバーのエラーメッセージ
 * @returns 対応する項目
 */
export function guessStockErrorField(message: string): StockFormField | undefined {
  return SERVER_ERROR_FIELDS.find(([label]) => message.startsWith(label))?.[1];
}

/**
 * 重複（409）のメッセージから、重複した項目を推定する。製品コードの重複は製品コード、それ以外（同じセンターの
 * 同名）は名称。
 *
 * @param message サーバーのエラーメッセージ
 * @returns 重複した項目
 */
export function conflictFieldOf(message: string): StockFormField {
  return message.includes('製品コード') ? 'productCode' : 'name';
}

/**
 * 個数（以上・以下）の検索条件の入力を検証する。空欄は条件なしとして扱う。
 *
 * @param min 個数（以上）
 * @param max 個数（以下）
 * @returns 項目ごとのエラー。なければ空のオブジェクト
 */
export function validateAmountRange(min: string, max: string): { amountMin?: string; amountMax?: string } {
  const errors: { amountMin?: string; amountMax?: string } = {};
  const a = min.trim();
  const b = max.trim();
  if (a !== '' && !DIGITS.test(a)) errors.amountMin = '個数（以上）は 0 以上の整数で入力してください';
  if (b !== '' && !DIGITS.test(b)) errors.amountMax = '個数（以下）は 0 以上の整数で入力してください';
  if (!errors.amountMin && !errors.amountMax && a !== '' && b !== '' && Number(a) > Number(b)) {
    errors.amountMin = '個数（以上）は個数（以下）以下にしてください';
  }
  return errors;
}

/**
 * 在庫の行を識別する名前（操作ボタンのラベル用）。同じ名称が別のセンターにもあり得るため、センター名を添える。
 *
 * @param stock 在庫
 * @returns 名称（センター名）
 */
export function stockLabel(stock: Stock): string {
  return stock.centerName ? `${stock.name}（${stock.centerName}）` : stock.name;
}
