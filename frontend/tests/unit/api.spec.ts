import { test, expect } from '@playwright/test';
import { ApiError, SESSION_EXPIRED_EVENT, apiFetch } from '../../app/lib/api-client';
import { buildCenterQuery, createCenter, deleteCenter, fetchCenters, updateCenter } from '../../app/lib/centers-api';
import { DEFAULT_CENTER_SEARCH } from '../../app/hooks/useCenters';
import { getAccessToken, getRefreshToken, saveTokens } from '../../app/lib/auth-storage';
import type { CenterRequest } from '../../app/types/api';

/**
 * API クライアント（app/lib/api-client.ts）と在庫センター API（app/lib/centers-api.ts）の単体テスト。
 * fetch と Cookie をテスト内の偽物に置き換え、認証ヘッダー、401 のときのトークン更新と再試行、
 * エラーレスポンスの変換、通信エラー、リクエストの組み立てを確認する。
 */

interface Call {
  url: string;
  method: string;
  authorization: string | undefined;
  body: string | undefined;
}

let calls: Call[] = [];
let expiredEvents = 0;
const jar = new Map<string, string>();

/** レスポンスの手順（先頭から順に使う）。 */
type Step = { status: number; body?: unknown } | 'network-error';

/** Cookie（document.cookie）、window、fetch を偽物にする。 */
function setup(steps: Step[]) {
  calls = [];
  expiredEvents = 0;
  jar.clear();
  const g = globalThis as unknown as Record<string, unknown>;
  g.document = {
    get cookie() {
      return Array.from(jar.entries()).map(([k, v]) => `${k}=${v}`).join('; ');
    },
    set cookie(s: string) {
      const [pair, ...attrs] = s.split(';');
      const i = pair.indexOf('=');
      const key = pair.slice(0, i);
      // 有効期限が過去の Cookie は削除として扱う（js-cookie の remove は expires に過去の日時を指定する）
      const expires = attrs.map((a) => a.trim()).find((a) => a.toLowerCase().startsWith('expires='));
      if (expires && new Date(expires.slice('expires='.length)).getTime() < Date.now()) jar.delete(key);
      else jar.set(key, pair.slice(i + 1));
    },
  };
  g.window = {
    location: { protocol: 'http:' },
    dispatchEvent: (e: Event) => {
      if (e.type === SESSION_EXPIRED_EVENT) expiredEvents++;
      return true;
    },
  };
  const queue = [...steps];
  g.fetch = async (url: string, init: RequestInit = {}) => {
    const headers = (init.headers ?? {}) as Record<string, string>;
    calls.push({
      url,
      method: init.method ?? 'GET',
      authorization: headers.Authorization,
      body: init.body as string | undefined,
    });
    const step = queue.shift();
    if (!step) throw new Error('想定外の fetch 呼び出し');
    if (step === 'network-error') throw new TypeError('fetch failed');
    return new Response(step.body === undefined ? null : JSON.stringify(step.body), { status: step.status });
  };
}

const request: CenterRequest = {
  centerName: '大阪センター',
  postCode: '530-0001',
  address: '大阪府大阪市北区梅田1-1-1',
  phoneNumber: '06-1234-5678',
  managerName: '田中一郎',
  operationalStatus: 0,
  maxStorageCapacity: 800,
  currentStorageCapacity: 0,
  notes: null,
};

test.describe('apiFetch', () => {
  test('保存済みのアクセストークンを Bearer ヘッダーで送る', async () => {
    setup([{ status: 200, body: { ok: true } }]);
    saveTokens('access-1', 'refresh-1');
    await apiFetch('/centers');
    expect(calls).toHaveLength(1);
    expect(calls[0]).toMatchObject({ url: '/api/v1/centers', method: 'GET', authorization: 'Bearer access-1' });
  });

  test('auth: false のときはトークンを付けない', async () => {
    setup([{ status: 200, body: {} }]);
    saveTokens('access-1', 'refresh-1');
    await apiFetch('/auth/authenticate', { method: 'POST', body: { email: 'a@b.c', password: 'x' }, auth: false });
    expect(calls[0].authorization).toBeUndefined();
    expect(calls[0].body).toBe(JSON.stringify({ email: 'a@b.c', password: 'x' }));
  });

  test('エラーレスポンスを ApiError（ステータス、コード、メッセージ）に変換する', async () => {
    setup([{ status: 409, body: { error: { code: 'CONFLICT', message: '同じ在庫センター名が既に登録されています。' } } }]);
    saveTokens('a', 'r');
    const err = await apiFetch('/centers', { method: 'POST', body: request }).catch((e) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err).toMatchObject({ status: 409, code: 'CONFLICT', message: '同じ在庫センター名が既に登録されています。' });
  });

  test('本文のないエラーはステータスごとの既定メッセージにする', async () => {
    setup([{ status: 500 }]);
    saveTokens('a', 'r');
    const err = (await apiFetch('/centers').catch((e) => e)) as ApiError;
    expect(err.status).toBe(500);
    expect(err.message).toContain('サーバーでエラーが発生しました');
  });

  test('通信できないときは NETWORK_ERROR（ステータス 0）にする', async () => {
    setup(['network-error']);
    saveTokens('a', 'r');
    const err = (await apiFetch('/centers').catch((e) => e)) as ApiError;
    expect(err).toMatchObject({ status: 0, code: 'NETWORK_ERROR' });
  });

  test('401 のときはリフレッシュトークンで更新し、新しいトークンで 1 回だけ再試行する', async () => {
    setup([
      { status: 401, body: { error: { code: 'AUTH_ERROR', message: '認証に失敗しました。' } } },
      { status: 200, body: { accessToken: 'access-2', refreshToken: 'refresh-2' } },
      { status: 200, body: { data: [] } },
    ]);
    saveTokens('access-1', 'refresh-1');
    const res = await apiFetch<{ data: unknown[] }>('/centers');
    expect(res).toEqual({ data: [] });
    expect(calls.map((c) => [c.url, c.authorization])).toEqual([
      ['/api/v1/centers', 'Bearer access-1'],
      ['/api/v1/auth/refresh-token', 'Bearer refresh-1'],
      ['/api/v1/centers', 'Bearer access-2'],
    ]);
    expect(getAccessToken()).toBe('access-2');
    expect(getRefreshToken()).toBe('refresh-2');
    expect(expiredEvents).toBe(0);
  });

  test('更新に失敗したらセッションを破棄し、失効を通知して 401 を返す', async () => {
    setup([
      { status: 401, body: { error: { code: 'AUTH_ERROR', message: '認証に失敗しました。' } } },
      { status: 401, body: { error: { code: 'AUTH_ERROR', message: '認証に失敗しました。' } } },
    ]);
    saveTokens('access-1', 'refresh-1');
    const err = (await apiFetch('/centers').catch((e) => e)) as ApiError;
    expect(err.status).toBe(401);
    expect(getAccessToken()).toBeUndefined();
    expect(getRefreshToken()).toBeUndefined();
    expect(expiredEvents).toBe(1);
  });

  test('リフレッシュトークンがないときは更新を試さずセッションを破棄する', async () => {
    setup([{ status: 401, body: { error: { code: 'AUTH_ERROR', message: '認証に失敗しました。' } } }]);
    const err = (await apiFetch('/centers').catch((e) => e)) as ApiError;
    expect(err.status).toBe(401);
    expect(calls).toHaveLength(1);
    expect(expiredEvents).toBe(1);
  });

  test('再試行も 401 なら、さらに繰り返さない', async () => {
    setup([
      { status: 401 },
      { status: 200, body: { accessToken: 'access-2', refreshToken: 'refresh-2' } },
      { status: 401 },
    ]);
    saveTokens('access-1', 'refresh-1');
    const err = (await apiFetch('/centers').catch((e) => e)) as ApiError;
    expect(err.status).toBe(401);
    expect(calls).toHaveLength(3);
    expect(expiredEvents).toBe(1);
  });

  test('ログイン API の 401 ではトークン更新をしない', async () => {
    setup([{ status: 401, body: { error: { code: 'AUTH_ERROR', message: '認証に失敗しました。' } } }]);
    const err = (await apiFetch('/auth/authenticate', { method: 'POST', body: {}, auth: false }).catch((e) => e)) as ApiError;
    expect(err.status).toBe(401);
    expect(calls).toHaveLength(1);
    expect(expiredEvents).toBe(0);
  });

  test('403 は権限エラーとして返し、セッションは維持する', async () => {
    setup([{ status: 403, body: { error: { code: 'FORBIDDEN', message: '権限がありません。' } } }]);
    saveTokens('a', 'r');
    const err = (await apiFetch('/centers').catch((e) => e)) as ApiError;
    expect(err).toMatchObject({ status: 403, code: 'FORBIDDEN' });
    expect(getAccessToken()).toBe('a');
    expect(expiredEvents).toBe(0);
  });
});

test.describe('buildCenterQuery', () => {
  test('既定の条件はページとソートだけを送る', () => {
    expect(buildCenterQuery(DEFAULT_CENTER_SEARCH)).toBe('?page=1&size=20&sort=centerId&order=desc');
  });

  test('絞り込み条件を前後の空白を除いて送り、日本語は URL エンコードする', () => {
    const q = buildCenterQuery({
      ...DEFAULT_CENTER_SEARCH,
      centerName: ' 大阪 ',
      address: '東京都',
      operationalStatus: '1',
      page: 3,
      size: 50,
      sort: 'maxStorageCapacity',
      order: 'asc',
    });
    const params = new URLSearchParams(q);
    expect(params.get('centerName')).toBe('大阪');
    expect(params.get('address')).toBe('東京都');
    expect(params.get('operationalStatus')).toBe('1');
    expect(params.get('page')).toBe('3');
    expect(params.get('sort')).toBe('maxStorageCapacity');
    expect(q).toContain('%E5%A4%A7%E9%98%AA');
  });

  test('空白だけの条件と稼働状況「すべて」は送らない', () => {
    const q = buildCenterQuery({ ...DEFAULT_CENTER_SEARCH, centerName: '   ', address: '', operationalStatus: '' });
    expect(q).not.toContain('centerName');
    expect(q).not.toContain('address');
    expect(q).not.toContain('operationalStatus');
  });

  test('稼働中（0）は絞り込み条件として送る', () => {
    expect(buildCenterQuery({ ...DEFAULT_CENTER_SEARCH, operationalStatus: '0' })).toContain('operationalStatus=0');
  });
});

test.describe('centers-api', () => {
  test('fetchCenters は GET /api/v1/centers を呼ぶ', async () => {
    setup([{ status: 200, body: { data: [], meta: { total: 0, page: 1, size: 20, totalPages: 0 } } }]);
    saveTokens('a', 'r');
    const res = await fetchCenters(DEFAULT_CENTER_SEARCH);
    expect(res.meta.total).toBe(0);
    expect(calls[0].url).toBe('/api/v1/centers?page=1&size=20&sort=centerId&order=desc');
  });

  test('createCenter は POST で JSON を送る', async () => {
    setup([{ status: 200, body: { centerId: 1, ...request } }]);
    saveTokens('a', 'r');
    const created = await createCenter(request);
    expect(created.centerId).toBe(1);
    expect(calls[0]).toMatchObject({ url: '/api/v1/centers', method: 'POST', body: JSON.stringify(request) });
  });

  test('updateCenter は PUT /centers/{id} を呼ぶ', async () => {
    setup([{ status: 200, body: { centerId: 7, ...request } }]);
    saveTokens('a', 'r');
    await updateCenter(7, request);
    expect(calls[0]).toMatchObject({ url: '/api/v1/centers/7', method: 'PUT' });
  });

  test('deleteCenter は DELETE /centers/{id} を呼び、404 は ApiError になる', async () => {
    setup([
      { status: 200, body: { result: 'success' } },
      { status: 404, body: { error: { code: 'NOT_FOUND', message: '在庫センターが見つかりません。ID: 9' } } },
    ]);
    saveTokens('a', 'r');
    await deleteCenter(7);
    expect(calls[0]).toMatchObject({ url: '/api/v1/centers/7', method: 'DELETE' });
    const err = (await deleteCenter(9).catch((e) => e)) as ApiError;
    expect(err).toMatchObject({ status: 404, code: 'NOT_FOUND' });
  });
});
