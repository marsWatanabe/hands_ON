import { test, expect } from '@playwright/test';
import {
  EMPTY_CENTER_FORM,
  centerToFormValues,
  guessErrorField,
  validateCenterForm,
  type CenterFormValues,
} from '../../app/lib/center-form';
import type { Center } from '../../app/types/api';

/**
 * 在庫センターのフォーム検証（app/lib/center-form.ts）の単体テスト。
 * バックエンド（CenterRequest）と同じ規則・文言で検証できること、
 * リクエストへの変換、サーバーのエラーメッセージの項目推定を確認する。
 */

/** 正常な入力値。各テストで一部だけ変える。 */
const valid: CenterFormValues = {
  centerName: '大阪センター',
  postCode: '530-0001',
  prefecture: '大阪府',
  city: '大阪市北区',
  street: '梅田1-1-1',
  phoneNumber: '06-1234-5678',
  managerName: '田中一郎',
  operationalStatus: '0',
  maxStorageCapacity: '800',
  currentStorageCapacity: '300',
  notes: '備考',
};

/** 1 項目だけ変えた入力値を検証し、エラーを返す（エラーがなければ空のオブジェクト）。 */
function errorsOf(patch: Partial<CenterFormValues>) {
  const result = validateCenterForm({ ...valid, ...patch });
  return result.ok ? {} : result.errors;
}

test.describe('validateCenterForm: 正常系', () => {
  test('正しい入力は API のリクエストに変換される', () => {
    const result = validateCenterForm(valid);
    expect(result).toEqual({
      ok: true,
      request: {
        centerName: '大阪センター',
        postCode: '530-0001',
        address: '大阪府大阪市北区梅田1-1-1',
        phoneNumber: '06-1234-5678',
        managerName: '田中一郎',
        operationalStatus: 0,
        maxStorageCapacity: 800,
        currentStorageCapacity: 300,
        notes: '備考',
      },
    });
  });

  test('任意項目（容量、備考）が空なら null で送る', () => {
    const result = validateCenterForm({ ...valid, maxStorageCapacity: '', currentStorageCapacity: '', notes: '' });
    expect(result.ok && result.request).toMatchObject({
      maxStorageCapacity: null,
      currentStorageCapacity: null,
      notes: null,
    });
  });

  test('稼働停止（1）を数値で送る', () => {
    const result = validateCenterForm({ ...valid, operationalStatus: '1' });
    expect(result.ok && result.request.operationalStatus).toBe(1);
  });

  test('前後の空白を除いて送る', () => {
    const result = validateCenterForm({ ...valid, centerName: '  大阪センター ', phoneNumber: ' 06-1234-5678 ' });
    expect(result.ok && result.request).toMatchObject({ centerName: '大阪センター', phoneNumber: '06-1234-5678' });
  });

  test('市区町村と番地のどちらか一方だけでも通る（分割できない既存データの編集用）', () => {
    expect(errorsOf({ city: '' })).toEqual({});
    expect(errorsOf({ street: '' })).toEqual({});
  });
});

test.describe('validateCenterForm: 必須項目', () => {
  test('空のフォームは必須項目のエラーになる', () => {
    const result = validateCenterForm(EMPTY_CENTER_FORM);
    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(Object.keys(result.errors).sort()).toEqual(
        ['centerName', 'city', 'managerName', 'phoneNumber', 'postCode', 'prefecture'].sort(),
      );
      expect(result.errors.centerName).toBe('在庫センター名は必須です');
      expect(result.errors.prefecture).toBe('都道府県を選択してください');
    }
  });

  test('空白だけの入力は未入力として扱う', () => {
    expect(errorsOf({ centerName: '   ' }).centerName).toBe('在庫センター名は必須です');
    // 全角空白も JavaScript の trim() で除かれる
    expect(errorsOf({ managerName: '　' }).managerName).toBe('管理者名は必須です');
  });

  test('市区町村と番地が両方空ならエラー', () => {
    expect(errorsOf({ city: '', street: '' }).city).toBe('市区町村または番地を入力してください');
  });
});

test.describe('validateCenterForm: 文字数の境界', () => {
  test('センター名は 20 文字まで', () => {
    expect(errorsOf({ centerName: 'あ'.repeat(20) })).toEqual({});
    expect(errorsOf({ centerName: 'あ'.repeat(21) }).centerName).toBe('在庫センター名は20文字以内で入力してください');
  });

  test('管理者名は 100 文字まで', () => {
    expect(errorsOf({ managerName: 'あ'.repeat(100) })).toEqual({});
    expect(errorsOf({ managerName: 'あ'.repeat(101) }).managerName).toBe('管理者名は100文字以内で入力してください');
  });

  test('備考は 255 文字まで', () => {
    expect(errorsOf({ notes: 'あ'.repeat(255) })).toEqual({});
    expect(errorsOf({ notes: 'あ'.repeat(256) }).notes).toBe('備考は255文字以内で入力してください');
  });

  test('住所は結合後に 255 文字まで', () => {
    // 大阪府（3）＋ 大阪市北区（5）＋ 番地 247 文字 = 255
    expect(errorsOf({ street: 'あ'.repeat(247) })).toEqual({});
    expect(errorsOf({ street: 'あ'.repeat(248) }).street).toBe('住所は255文字以内で入力してください');
  });
});

test.describe('validateCenterForm: 形式', () => {
  test('郵便番号は 123-4567 の形式', () => {
    for (const bad of ['5300001', '530-001', '530-00011', '５３０-０００１', 'abc-defg']) {
      expect(errorsOf({ postCode: bad }).postCode).toBe('郵便番号は 123-4567 の形式で入力してください');
    }
  });

  test('電話番号は 03-1234-5678 の形式（市外局番 2〜5 桁、局番 1〜4 桁、番号 4 桁）', () => {
    for (const ok of ['03-1234-5678', '0123-4-5678', '09999-9999-0000']) {
      expect(errorsOf({ phoneNumber: ok }).phoneNumber).toBeUndefined();
    }
    for (const bad of ['0312345678', '0-1234-5678', '03-1234-567', '03-12345-5678']) {
      expect(errorsOf({ phoneNumber: bad }).phoneNumber).toBe('電話番号は 03-1234-5678 の形式で入力してください');
    }
  });

  test('< と > を含む入力はエラー', () => {
    expect(errorsOf({ centerName: 'a<b' }).centerName).toBe('在庫センター名に < または > は使用できません');
    expect(errorsOf({ city: '<市>' }).city).toBe('市区町村に < または > は使用できません');
    expect(errorsOf({ street: '1>2' }).street).toBe('番地・建物名に < または > は使用できません');
    expect(errorsOf({ managerName: '<x>' }).managerName).toBe('管理者名に < または > は使用できません');
    expect(errorsOf({ notes: '<script>' }).notes).toBe('備考に < または > は使用できません');
  });
});

test.describe('validateCenterForm: 容量', () => {
  test('容量は 0 から 999,999,999 まで', () => {
    expect(errorsOf({ maxStorageCapacity: '0', currentStorageCapacity: '0' })).toEqual({});
    expect(errorsOf({ maxStorageCapacity: '999999999', currentStorageCapacity: '999999999' })).toEqual({});
    expect(errorsOf({ maxStorageCapacity: '1000000000' }).maxStorageCapacity).toBe('最大保管容量が大きすぎます');
    expect(errorsOf({ currentStorageCapacity: '1000000000', maxStorageCapacity: '' }).currentStorageCapacity).toBe(
      '現在保管容量が大きすぎます',
    );
  });

  test('整数以外（負数、小数、文字）はエラー', () => {
    for (const bad of ['-1', '1.5', 'abc', '1e3', '１００']) {
      expect(errorsOf({ maxStorageCapacity: bad }).maxStorageCapacity).toBe('最大保管容量は 0 以上の整数で入力してください');
    }
  });

  test('現在保管容量は最大保管容量以下（等しいのは可、1 超過は不可）', () => {
    expect(errorsOf({ maxStorageCapacity: '800', currentStorageCapacity: '800' })).toEqual({});
    expect(errorsOf({ maxStorageCapacity: '800', currentStorageCapacity: '801' }).currentStorageCapacity).toBe(
      '現在保管容量は最大保管容量以下にしてください',
    );
  });

  test('片方だけ入力されているときは大小を比べない', () => {
    expect(errorsOf({ maxStorageCapacity: '', currentStorageCapacity: '500' })).toEqual({});
    expect(errorsOf({ maxStorageCapacity: '500', currentStorageCapacity: '' })).toEqual({});
  });
});

test.describe('centerToFormValues', () => {
  const center: Center = {
    centerId: 1,
    centerName: '大阪センター',
    postCode: '530-0001',
    address: '大阪府大阪市北区梅田1-1-1',
    phoneNumber: '06-1234-5678',
    managerName: '田中一郎',
    operationalStatus: 1,
    maxStorageCapacity: 800,
    currentStorageCapacity: null,
    notes: null,
    createDate: '2026-10-21T00:00:00',
    updateDate: '2026-10-21T00:00:00',
  };

  test('住所を 3 項目に分け、null は空文字にする', () => {
    expect(centerToFormValues(center)).toEqual({
      centerName: '大阪センター',
      postCode: '530-0001',
      prefecture: '大阪府',
      city: '大阪市北区',
      street: '梅田1-1-1',
      phoneNumber: '06-1234-5678',
      managerName: '田中一郎',
      operationalStatus: '1',
      maxStorageCapacity: '800',
      currentStorageCapacity: '',
      notes: '',
    });
  });

  test('変更せずに保存すると、元の値と同じリクエストになる', () => {
    const result = validateCenterForm(centerToFormValues(center));
    expect(result.ok && result.request).toMatchObject({
      address: center.address,
      operationalStatus: 1,
      maxStorageCapacity: 800,
      currentStorageCapacity: null,
      notes: null,
    });
  });
});

test.describe('guessErrorField', () => {
  test('項目名で始まるメッセージから項目を推定する', () => {
    expect(guessErrorField('在庫センター名は必須です')).toBe('centerName');
    expect(guessErrorField('郵便番号は 123-4567 の形式で入力してください')).toBe('postCode');
    expect(guessErrorField('現在保管容量は最大保管容量以下にしてください。')).toBe('currentStorageCapacity');
    expect(guessErrorField('最大保管容量が大きすぎます')).toBe('maxStorageCapacity');
  });

  test('推定できないメッセージは undefined', () => {
    expect(guessErrorField('サーバーでエラーが発生しました。')).toBeUndefined();
    expect(guessErrorField('')).toBeUndefined();
  });
});
