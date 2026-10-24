import { z } from 'zod';
import type { Center, CenterRequest, OperationalStatus } from '../types/api';
import { joinAddress, splitAddress } from './address';

/** 入力値の上限。バックエンド（CenterRequest）と一致させる。 */
export const CENTER_LIMITS = {
  centerName: 20,
  address: 255,
  phoneNumber: 20,
  managerName: 100,
  notes: 255,
  capacity: 999_999_999,
} as const;

/** 稼働状況の表示名。 */
export const OPERATIONAL_STATUS_LABELS: Record<OperationalStatus, string> = {
  0: '稼働中',
  1: '稼働停止',
};

/** フォームの入力値（すべて文字列で保持する）。 */
export interface CenterFormValues {
  centerName: string;
  postCode: string;
  prefecture: string;
  city: string;
  street: string;
  phoneNumber: string;
  managerName: string;
  operationalStatus: '0' | '1';
  maxStorageCapacity: string;
  currentStorageCapacity: string;
  notes: string;
}

/** エラーを表示する項目。 */
export type CenterFormField = keyof CenterFormValues;

/** 項目ごとのエラーメッセージ。 */
export type CenterFormErrors = Partial<Record<CenterFormField, string>>;

/** 検証の結果。 */
export type CenterFormResult =
  | { ok: true; request: CenterRequest }
  | { ok: false; errors: CenterFormErrors };

const NO_ANGLE_BRACKETS = /^[^<>]*$/;
const MSG_ANGLE_BRACKETS = (label: string) => `${label}に < または > は使用できません`;

/** 容量の入力欄（任意の非負整数）。 */
const capacityField = (label: string) =>
  z
    .string()
    .refine((v) => v === '' || /^\d+$/.test(v), `${label}は 0 以上の整数で入力してください`)
    .refine(
      (v) => v === '' || !/^\d+$/.test(v) || Number(v) <= CENTER_LIMITS.capacity,
      `${label}が大きすぎます`,
    );

/** フォームの検証スキーマ。メッセージはバックエンドと同じ文言にそろえる。 */
export const centerFormSchema = z
  .object({
    centerName: z
      .string()
      .min(1, '在庫センター名は必須です')
      .max(CENTER_LIMITS.centerName, `在庫センター名は${CENTER_LIMITS.centerName}文字以内で入力してください`)
      .regex(NO_ANGLE_BRACKETS, MSG_ANGLE_BRACKETS('在庫センター名')),
    postCode: z
      .string()
      .min(1, '郵便番号は必須です')
      .regex(/^\d{3}-\d{4}$/, '郵便番号は 123-4567 の形式で入力してください'),
    prefecture: z.string(),
    city: z.string().regex(NO_ANGLE_BRACKETS, MSG_ANGLE_BRACKETS('市区町村')),
    street: z.string().regex(NO_ANGLE_BRACKETS, MSG_ANGLE_BRACKETS('番地・建物名')),
    phoneNumber: z
      .string()
      .min(1, '電話番号は必須です')
      .max(CENTER_LIMITS.phoneNumber, `電話番号は${CENTER_LIMITS.phoneNumber}文字以内で入力してください`)
      .regex(/^\d{2,5}-\d{1,4}-\d{4}$/, '電話番号は 03-1234-5678 の形式で入力してください'),
    managerName: z
      .string()
      .min(1, '管理者名は必須です')
      .max(CENTER_LIMITS.managerName, `管理者名は${CENTER_LIMITS.managerName}文字以内で入力してください`)
      .regex(NO_ANGLE_BRACKETS, MSG_ANGLE_BRACKETS('管理者名')),
    operationalStatus: z.enum(['0', '1'], { errorMap: () => ({ message: '稼働状況を選択してください' }) }),
    maxStorageCapacity: capacityField('最大保管容量'),
    currentStorageCapacity: capacityField('現在保管容量'),
    notes: z
      .string()
      .max(CENTER_LIMITS.notes, `備考は${CENTER_LIMITS.notes}文字以内で入力してください`)
      .regex(NO_ANGLE_BRACKETS, MSG_ANGLE_BRACKETS('備考')),
  })
  .superRefine((v, ctx) => {
    // 住所: 都道府県は必須。市区町村と番地は、どちらか一方があればよい
    // （既存データの住所を分割できなかった場合に、編集を妨げないため）
    if (!v.prefecture) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['prefecture'], message: '都道府県を選択してください' });
    }
    if (!v.city && !v.street) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['city'], message: '市区町村または番地を入力してください' });
    }
    if (joinAddress(v).length > CENTER_LIMITS.address) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['street'],
        message: `住所は${CENTER_LIMITS.address}文字以内で入力してください`,
      });
    }
    // 業務ルール: 現在保管容量は最大保管容量以下
    if (
      /^\d+$/.test(v.maxStorageCapacity) &&
      /^\d+$/.test(v.currentStorageCapacity) &&
      Number(v.currentStorageCapacity) > Number(v.maxStorageCapacity)
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['currentStorageCapacity'],
        message: '現在保管容量は最大保管容量以下にしてください',
      });
    }
  });

/** 新規作成時のフォームの初期値。 */
export const EMPTY_CENTER_FORM: CenterFormValues = {
  centerName: '',
  postCode: '',
  prefecture: '',
  city: '',
  street: '',
  phoneNumber: '',
  managerName: '',
  operationalStatus: '0',
  maxStorageCapacity: '',
  currentStorageCapacity: '',
  notes: '',
};

/**
 * 在庫センターからフォームの初期値を作る（編集時）。住所は 3 項目に分割する。
 *
 * @param center 在庫センター
 * @returns フォームの初期値
 */
export function centerToFormValues(center: Center): CenterFormValues {
  const { prefecture, city, street } = splitAddress(center.address);
  return {
    centerName: center.centerName,
    postCode: center.postCode,
    prefecture,
    city,
    street,
    phoneNumber: center.phoneNumber,
    managerName: center.managerName,
    operationalStatus: String(center.operationalStatus) as '0' | '1',
    maxStorageCapacity: center.maxStorageCapacity === null ? '' : String(center.maxStorageCapacity),
    currentStorageCapacity: center.currentStorageCapacity === null ? '' : String(center.currentStorageCapacity),
    notes: center.notes ?? '',
  };
}

/** 前後の空白を除いた入力値を返す（稼働状況は除く）。 */
function trimValues(values: CenterFormValues): CenterFormValues {
  return {
    ...values,
    centerName: values.centerName.trim(),
    postCode: values.postCode.trim(),
    city: values.city.trim(),
    street: values.street.trim(),
    phoneNumber: values.phoneNumber.trim(),
    managerName: values.managerName.trim(),
    maxStorageCapacity: values.maxStorageCapacity.trim(),
    currentStorageCapacity: values.currentStorageCapacity.trim(),
    notes: values.notes.trim(),
  };
}

/**
 * フォームの入力値を検証し、API に送るリクエストへ変換する。
 *
 * @param values 入力値
 * @returns 成功時はリクエスト、失敗時は項目ごとのエラー
 */
export function validateCenterForm(values: CenterFormValues): CenterFormResult {
  const trimmed = trimValues(values);
  const parsed = centerFormSchema.safeParse(trimmed);
  if (!parsed.success) {
    const errors: CenterFormErrors = {};
    for (const issue of parsed.error.issues) {
      const key = issue.path[0] as CenterFormField;
      // 同じ項目に複数のエラーがあるときは、最初のものを表示する
      if (key && !errors[key]) errors[key] = issue.message;
    }
    return { ok: false, errors };
  }
  const v = parsed.data;
  const toNumber = (s: string): number | null => (s === '' ? null : Number(s));
  return {
    ok: true,
    request: {
      centerName: v.centerName,
      postCode: v.postCode,
      address: joinAddress(v),
      phoneNumber: v.phoneNumber,
      managerName: v.managerName,
      operationalStatus: Number(v.operationalStatus) as OperationalStatus,
      maxStorageCapacity: toNumber(v.maxStorageCapacity),
      currentStorageCapacity: toNumber(v.currentStorageCapacity),
      notes: v.notes === '' ? null : v.notes,
    },
  };
}

/** サーバーのエラーメッセージを、表示する項目に対応づけるための手がかり。 */
const SERVER_ERROR_FIELDS: Array<[string, CenterFormField]> = [
  ['在庫センター名', 'centerName'],
  ['郵便番号', 'postCode'],
  ['住所', 'street'],
  ['電話番号', 'phoneNumber'],
  ['管理者名', 'managerName'],
  ['稼働状況', 'operationalStatus'],
  ['最大保管容量', 'maxStorageCapacity'],
  ['現在保管容量', 'currentStorageCapacity'],
  ['備考', 'notes'],
];

/**
 * サーバーが返したエラーメッセージから、対応する入力項目を推定する。
 * 項目名で始まるメッセージ（例: 「在庫センター名は必須です」）だけを対象にする。
 * 該当しないとき（重複など）は undefined を返し、フォーム全体のエラーとして表示する。
 *
 * @param message サーバーのエラーメッセージ
 * @returns 対応する項目
 */
export function guessErrorField(message: string): CenterFormField | undefined {
  // 「在庫センター名は…」と「在庫センター名「…」は既に…」の両方を扱う
  const hit = SERVER_ERROR_FIELDS.find(([label]) => message.startsWith(label));
  return hit?.[1];
}
