import { expect, type APIRequestContext, type BrowserContext, type Page } from '@playwright/test';

/**
 * E2E テスト（カテゴリ、部品在庫、製品在庫）の共通部品。
 * 実際のバックエンド（既定 http://localhost:8080）と Next.js の開発サーバーを使う。
 * テストごとに新しいユーザーと一意な名称を使うため、既存データに依存しない。
 */

export const BASE_URL = 'http://localhost:3000';
const PASSWORD = 'password123';

export interface Session {
  email: string;
  accessToken: string;
  refreshToken: string;
  user: { id: number; firstname: string; lastname: string; email: string; role: string };
}

/** API でユーザーを登録し、トークンを得る。 */
export async function registerUser(request: APIRequestContext): Promise<Session> {
  const email = `e2e-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;
  const res = await request.post('/api/v1/auth/register', {
    data: { firstname: '太郎', lastname: '試験', email, password: PASSWORD },
  });
  expect(res.ok(), 'バックエンドが起動していることを確認してください').toBeTruthy();
  const body = await res.json();
  return { email, accessToken: body.accessToken, refreshToken: body.refreshToken, user: body.user };
}

/** ブラウザの Cookie にログイン状態を入れる（画面のログイン操作を省く）。 */
export async function signIn(context: BrowserContext, s: Session) {
  await context.addCookies([
    { name: 'accessToken', value: s.accessToken, url: BASE_URL },
    { name: 'refreshToken', value: s.refreshToken, url: BASE_URL },
    { name: 'user', value: encodeURIComponent(JSON.stringify(s.user)), url: BASE_URL },
  ]);
}

/** 一意な名称（20 文字以内）。 */
export function uniqueName(prefix: string): string {
  return `${prefix}${Date.now().toString().slice(-7)}${Math.floor(Math.random() * 90 + 10)}`;
}

/** 一意な製品コード（20 文字以内の半角英数字とハイフン）。 */
export function uniqueCode(): string {
  return `E2E-${Date.now().toString().slice(-8)}-${Math.floor(Math.random() * 900 + 100)}`;
}

function headers(s: Session) {
  return { Authorization: `Bearer ${s.accessToken}` };
}

/** 名称の一部で、センターの ID を取得する（開発用データの東京、大阪など）。 */
export async function centerIdByName(request: APIRequestContext, s: Session, name: string): Promise<number> {
  const res = await request.get('/api/v1/centers', { headers: headers(s), params: { centerName: name } });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).data[0].centerId;
}

/** API でカテゴリを作成し、ID を返す。 */
export async function apiCreateCategory(
  request: APIRequestContext,
  s: Session,
  type: 'part' | 'product',
  name: string,
): Promise<number> {
  const res = await request.post('/api/v1/categories', {
    headers: headers(s),
    data: { categoryType: type, categoryName: name, description: 'E2E' },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).categoryId;
}

/** API で部品在庫を作成し、ID を返す。 */
export async function apiCreatePart(
  request: APIRequestContext,
  s: Session,
  v: { categoryId: number; centerId: number; name: string; amount?: number },
): Promise<number> {
  const res = await request.post('/api/v1/parts-stocks', {
    headers: headers(s),
    data: { categoryId: v.categoryId, centerId: v.centerId, name: v.name, description: 'E2E', amount: v.amount ?? 10 },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).stockId;
}

/** API で製品在庫を作成し、ID を返す。 */
export async function apiCreateProduct(
  request: APIRequestContext,
  s: Session,
  v: { productCode: string; categoryId: number; centerId: number; name: string; amount?: number },
): Promise<number> {
  const res = await request.post('/api/v1/products-stocks', {
    headers: headers(s),
    data: {
      productCode: v.productCode,
      categoryId: v.categoryId,
      centerId: v.centerId,
      name: v.name,
      description: 'E2E',
      amount: v.amount ?? 10,
    },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).stockId;
}

/** API で在庫・カテゴリを削除する（後片付け。すでに削除済みでもよい）。 */
export async function apiDelete(request: APIRequestContext, s: Session, path: string, id: number) {
  await request.delete(`/api/v1/${path}/${id}`, { headers: headers(s) });
}

/** 検索バー（role=search）の中で、名称などの入力欄に入力して検索する。 */
export async function searchBy(page: Page, label: string, value: string) {
  const search = page.getByRole('search');
  await search.getByLabel(label).fill(value);
  await search.getByRole('button', { name: '検索' }).click();
}
