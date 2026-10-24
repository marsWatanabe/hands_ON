import { test, expect, type APIRequestContext, type BrowserContext, type Page } from '@playwright/test';

/**
 * 在庫センター管理画面の E2E テスト（Chromium）。
 * 実際のバックエンド（既定 http://localhost:8080）と、Next.js の開発サーバーを使う。
 * テストごとに新しいユーザーと一意なセンター名を使うため、既存データに依存しない。
 * 作成したセンターは、各テストの最後（または afterEach）で削除する。
 */

const BASE_URL = 'http://localhost:3000';
const PASSWORD = 'password123';

interface Session {
  email: string;
  accessToken: string;
  refreshToken: string;
  user: { id: number; firstname: string; lastname: string; email: string; role: string };
}

/** API でユーザーを登録し、トークンを得る。 */
async function registerUser(request: APIRequestContext): Promise<Session> {
  const email = `e2e-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;
  const res = await request.post('/api/v1/auth/register', {
    data: { firstname: '太郎', lastname: '試験', email, password: PASSWORD },
  });
  expect(res.ok(), 'バックエンドが起動していることを確認してください').toBeTruthy();
  const body = await res.json();
  return { email, accessToken: body.accessToken, refreshToken: body.refreshToken, user: body.user };
}

/** ブラウザの Cookie にログイン状態を入れる（画面のログイン操作を省く）。 */
async function signIn(context: BrowserContext, s: Session, overrides: { accessToken?: string; refreshToken?: string } = {}) {
  await context.addCookies([
    { name: 'accessToken', value: overrides.accessToken ?? s.accessToken, url: BASE_URL },
    { name: 'refreshToken', value: overrides.refreshToken ?? s.refreshToken, url: BASE_URL },
    { name: 'user', value: encodeURIComponent(JSON.stringify(s.user)), url: BASE_URL },
  ]);
}

/** 一意なセンター名（20 文字以内）。 */
function uniqueName(prefix = 'E2E'): string {
  return `${prefix}${Date.now().toString().slice(-8)}${Math.floor(Math.random() * 90 + 10)}`;
}

/** API でセンターを作成する。 */
async function apiCreateCenter(request: APIRequestContext, s: Session, name: string, extra: Record<string, unknown> = {}) {
  const res = await request.post('/api/v1/centers', {
    headers: { Authorization: `Bearer ${s.accessToken}` },
    data: {
      centerName: name,
      postCode: '530-0001',
      address: '大阪府大阪市北区梅田1-1-1',
      phoneNumber: '06-1234-5678',
      managerName: '田中一郎',
      operationalStatus: 0,
      maxStorageCapacity: 800,
      currentStorageCapacity: 100,
      notes: null,
      ...extra,
    },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()) as { centerId: number };
}

/** API でセンターを削除する（後片付け）。 */
async function apiDeleteCenter(request: APIRequestContext, s: Session, centerId: number) {
  await request.delete(`/api/v1/centers/${centerId}`, { headers: { Authorization: `Bearer ${s.accessToken}` } });
}

/** 検索バーでセンター名を絞り込む。 */
async function searchByName(page: Page, name: string) {
  const search = page.getByRole('search');
  await search.getByLabel('センター名').fill(name);
  await search.getByRole('button', { name: '検索' }).click();
}

/** 登録・編集モーダルの共通項目を入力する。 */
async function fillCenterForm(page: Page, v: { name?: string; post?: string; pref?: string; city?: string; street?: string; tel?: string; manager?: string; max?: string; current?: string }) {
  const dialog = page.getByRole('dialog');
  if (v.name !== undefined) await dialog.getByLabel('在庫センター名').fill(v.name);
  if (v.post !== undefined) await dialog.getByLabel('郵便番号').fill(v.post);
  if (v.pref !== undefined) await dialog.getByLabel('都道府県').selectOption(v.pref);
  if (v.city !== undefined) await dialog.getByLabel('市区町村').fill(v.city);
  if (v.street !== undefined) await dialog.getByLabel('番地・建物名').fill(v.street);
  if (v.tel !== undefined) await dialog.getByLabel('電話番号').fill(v.tel);
  if (v.manager !== undefined) await dialog.getByLabel('管理者名').fill(v.manager);
  if (v.max !== undefined) await dialog.getByLabel('最大保管容量').fill(v.max);
  if (v.current !== undefined) await dialog.getByLabel('現在保管容量').fill(v.current);
}

test.describe('認証', () => {
  test('未ログインで /centers を開くと /login へ移動する', async ({ page }) => {
    await page.goto('/centers');
    await page.waitForURL('**/login');
    await expect(page.getByRole('heading', { name: 'ログイン' })).toBeVisible();
  });

  test('トップページは未ログインなら /login、ログイン済みなら /centers へ移動する', async ({ page, context, request }) => {
    await page.goto('/');
    await page.waitForURL('**/login');
    await signIn(context, await registerUser(request));
    await page.goto('/');
    await page.waitForURL('**/centers');
  });

  test('誤ったパスワードではエラーを表示し、画面を移動しない', async ({ page, request }) => {
    const s = await registerUser(request);
    await page.goto('/login');
    await page.getByLabel('メールアドレス').fill(s.email);
    await page.getByLabel('パスワード').fill('wrong-password');
    await page.getByRole('button', { name: 'ログイン' }).click();
    await expect(page.getByText('ログインに失敗しました')).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
  });

  test('正しい資格情報でログインすると在庫センター管理に移動し、ログアウトで戻る', async ({ page, request }) => {
    const s = await registerUser(request);
    await page.goto('/login');
    await page.getByLabel('メールアドレス').fill(s.email);
    await page.getByLabel('パスワード').fill(PASSWORD);
    await page.getByRole('button', { name: 'ログイン' }).click();
    await page.waitForURL('**/centers');
    await expect(page.getByRole('heading', { name: '在庫センター管理', level: 1 })).toBeVisible();
    await expect(page).toHaveTitle(/在庫センター管理 \| Inventrack/);

    // ログアウト
    await page.getByText('試験 太郎').first().click();
    await page.getByRole('menuitem', { name: 'ログアウト' }).click();
    await page.waitForURL('**/login');

    // ログアウト後は保護された画面に入れない
    await page.goto('/centers');
    await page.waitForURL('**/login');
  });

  test('アカウント登録の画面から登録すると、そのままログインして在庫センター管理へ移動する', async ({ page }) => {
    await page.goto('/register');
    await page.getByLabel('氏名').fill('試験');
    await page.getByLabel('名前').fill('花子');
    await page.getByLabel('メールアドレス').fill(`e2e-reg-${Date.now()}@example.com`);
    await page.getByPlaceholder('8文字以上で入力').fill(PASSWORD);
    await page.getByPlaceholder('パスワードを再入力').fill(PASSWORD);
    await page.getByRole('button', { name: 'アカウントを作成' }).click();
    await page.waitForURL('**/centers');
    await expect(page.getByText('試験 花子').first()).toBeVisible();
  });

  test('アクセストークンが無効でも、リフレッシュトークンで更新して一覧を表示する', async ({ page, context, request }) => {
    const s = await registerUser(request);
    await signIn(context, s, { accessToken: 'invalid-token' });
    await page.goto('/centers');
    await expect(page.getByRole('table')).toBeVisible();
    await expect(page.getByText('在庫センターの取得に失敗しました')).toHaveCount(0);
    // 更新後のトークンが Cookie に保存される
    await expect
      .poll(async () => (await context.cookies(BASE_URL)).find((c) => c.name === 'accessToken')?.value)
      .not.toBe('invalid-token');
    await expect(page).toHaveURL(/\/centers$/);
  });

  test('両方のトークンが無効なら /login へ移動する', async ({ page, context, request }) => {
    const s = await registerUser(request);
    await signIn(context, s, { accessToken: 'invalid-token', refreshToken: 'invalid-refresh' });
    await page.goto('/centers');
    await page.waitForURL('**/login');
  });
});

test.describe('在庫センターの一覧・検索・並び替え', () => {
  let session: Session;
  const created: number[] = [];

  test.beforeEach(async ({ context, request }) => {
    session = await registerUser(request);
    await signIn(context, session);
  });

  test.afterEach(async ({ request }) => {
    for (const id of created.splice(0)) await apiDeleteCenter(request, session, id);
  });

  test('一覧の見出し、件数、ページ送りの表示', async ({ page }) => {
    await page.goto('/centers');
    const headers = page.getByRole('columnheader');
    await expect(headers).toHaveText([
      'センター名',
      '住所（都道府県・市区町村）',
      '電話番号',
      '管理者名',
      '稼働状況',
      '最大保管容量（㎥）',
      '現在保管容量（㎥）',
      '操作',
    ].map((t) => new RegExp(t.replace(/[（）]/g, '.'))));
    await expect(page.getByText(/全 [\d,]+ 件（1 \/ \d+ ページ）/)).toBeVisible();
    await expect(page.getByRole('button', { name: '前へ' })).toBeDisabled();
  });

  test('センター名で検索でき、クリアで解除できる', async ({ page, request }) => {
    const name = uniqueName('検索');
    created.push((await apiCreateCenter(request, session, name)).centerId);
    await page.goto('/centers');
    await searchByName(page, name);
    const rows = page.getByRole('row').filter({ hasText: name });
    await expect(rows).toHaveCount(1);
    await expect(page.getByText('全 1 件')).toBeVisible();
    // 住所は「都道府県・市区町村」までを表示する
    await expect(rows.first()).toContainText('大阪府大阪市北区');
    await expect(rows.first()).not.toContainText('梅田');
    await expect(rows.first()).toContainText('800');

    await page.getByRole('button', { name: 'クリア' }).click();
    await expect(page.getByText('全 1 件')).toHaveCount(0);
  });

  test('条件に一致しないときは 0 件のメッセージを表示する', async ({ page }) => {
    await page.goto('/centers');
    await searchByName(page, 'ZZZ存在しない名前');
    await expect(page.getByText('条件に一致する在庫センターがありません。')).toBeVisible();
    await expect(page.getByText('全 0 件')).toBeVisible();
  });

  test('稼働状況で絞り込める', async ({ page, request }) => {
    const name = uniqueName('停止');
    created.push((await apiCreateCenter(request, session, name, { operationalStatus: 1 })).centerId);
    await page.goto('/centers');
    const search = page.getByRole('search');
    await search.getByLabel('センター名').fill(name);
    await search.getByLabel('稼働状況').selectOption('0');
    await search.getByRole('button', { name: '検索' }).click();
    await expect(page.getByText('条件に一致する在庫センターがありません。')).toBeVisible();
    await search.getByLabel('稼働状況').selectOption('1');
    await search.getByRole('button', { name: '検索' }).click();
    const row = page.getByRole('row').filter({ hasText: name });
    await expect(row).toHaveCount(1);
    await expect(row).toContainText('稼働停止');
  });

  test('見出しのクリックで並び替え、もう一度クリックで降順にする', async ({ page, request }) => {
    const prefix = uniqueName('並');
    for (const suffix of ['B', 'A', 'C']) {
      created.push((await apiCreateCenter(request, session, `${prefix}${suffix}`)).centerId);
    }
    await page.goto('/centers');
    await searchByName(page, prefix);
    const names = page.getByRole('row').filter({ hasText: prefix });
    await expect(names).toHaveCount(3);

    const sortButton = page.getByRole('button', { name: 'センター名で並び替え' });
    await sortButton.click();
    await expect(page.getByRole('columnheader', { name: /センター名/ })).toHaveAttribute('aria-sort', 'ascending');
    await expect(names.nth(0)).toContainText(`${prefix}A`);
    await expect(names.nth(2)).toContainText(`${prefix}C`);

    await sortButton.click();
    await expect(page.getByRole('columnheader', { name: /センター名/ })).toHaveAttribute('aria-sort', 'descending');
    await expect(names.nth(0)).toContainText(`${prefix}C`);
  });

  test('1 ページの件数を変えて、ページを移動できる', async ({ page, request }) => {
    const prefix = uniqueName('頁');
    for (let i = 0; i < 11; i++) {
      created.push((await apiCreateCenter(request, session, `${prefix}-${String(i).padStart(2, '0')}`)).centerId);
    }
    await page.goto('/centers');
    await page.getByLabel('1 ページの件数').selectOption('10');
    await searchByName(page, prefix);
    await expect(page.getByText('全 11 件（1 / 2 ページ）')).toBeVisible();
    await expect(page.getByRole('row').filter({ hasText: prefix })).toHaveCount(10);

    await page.getByRole('button', { name: '次へ' }).click();
    await expect(page.getByText('全 11 件（2 / 2 ページ）')).toBeVisible();
    await expect(page.getByRole('row').filter({ hasText: prefix })).toHaveCount(1);
    await expect(page.getByRole('button', { name: '次へ' })).toBeDisabled();
  });
});

test.describe('在庫センターの登録・編集・削除', () => {
  let session: Session;
  const created: number[] = [];

  test.beforeEach(async ({ context, request }) => {
    session = await registerUser(request);
    await signIn(context, session);
  });

  test.afterEach(async ({ request }) => {
    for (const id of created.splice(0)) await apiDeleteCenter(request, session, id);
  });

  test('登録、編集、削除までの一連の操作', async ({ page }) => {
    const name = uniqueName('一連');
    await page.goto('/centers');

    // 登録
    await page.getByRole('button', { name: '新規登録' }).click();
    await fillCenterForm(page, {
      name,
      post: '530-0001',
      pref: '大阪府',
      city: '大阪市北区',
      street: '梅田1-1-1',
      tel: '06-1234-5678',
      manager: '田中一郎',
      max: '800',
      current: '300',
    });
    await page.getByRole('dialog').getByRole('button', { name: '登録' }).click();
    await expect(page.getByText('在庫センターを登録しました')).toBeVisible();
    await expect(page.getByRole('dialog')).toHaveCount(0);

    await searchByName(page, name);
    const row = page.getByRole('row').filter({ hasText: name });
    await expect(row).toHaveCount(1);
    await expect(row).toContainText('大阪府大阪市北区');
    await expect(row).toContainText('300');

    // 編集（住所が 3 項目に分かれて表示される）
    await row.getByRole('button', { name: `${name}を編集` }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByLabel('都道府県')).toHaveValue('大阪府');
    await expect(dialog.getByLabel('市区町村')).toHaveValue('大阪市北区');
    await expect(dialog.getByLabel('番地・建物名')).toHaveValue('梅田1-1-1');
    await fillCenterForm(page, { pref: '東京都', city: '千代田区', street: '丸の内1-1', current: '500' });
    await dialog.getByLabel('稼働状況').selectOption('1');
    await dialog.getByRole('button', { name: '更新' }).click();
    await expect(page.getByText('在庫センターを更新しました')).toBeVisible();
    await expect(row).toContainText('東京都千代田区');
    await expect(row).toContainText('稼働停止');
    await expect(row).toContainText('500');

    // 削除（キャンセルすると残り、確認すると消える）
    await row.getByRole('button', { name: `${name}を削除` }).click();
    await page.getByRole('alertdialog').getByRole('button', { name: 'キャンセル' }).click();
    await expect(row).toHaveCount(1);

    await row.getByRole('button', { name: `${name}を削除` }).click();
    await expect(page.getByRole('alertdialog')).toContainText(name);
    await page.getByRole('alertdialog').getByRole('button', { name: '削除' }).click();
    await expect(page.getByText('在庫センターを削除しました')).toBeVisible();
    await expect(row).toHaveCount(0);
  });

  test('必須項目が空のまま登録すると、項目ごとにエラーを表示して送信しない', async ({ page }) => {
    await page.goto('/centers');
    await page.getByRole('button', { name: '新規登録' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('在庫センター名は必須です')).toBeVisible();
    await expect(dialog.getByText('郵便番号は必須です')).toBeVisible();
    await expect(dialog.getByText('都道府県を選択してください')).toBeVisible();
    await expect(dialog.getByText('電話番号は必須です')).toBeVisible();
    await expect(dialog.getByText('管理者名は必須です')).toBeVisible();
    await expect(dialog).toBeVisible();

    // 入力し直した項目のエラーは消える
    await dialog.getByLabel('在庫センター名').fill('あ');
    await expect(dialog.getByText('在庫センター名は必須です')).toHaveCount(0);
  });

  test('形式の誤りと容量の超過を表示する', async ({ page }) => {
    await page.goto('/centers');
    await page.getByRole('button', { name: '新規登録' }).click();
    await fillCenterForm(page, {
      name: 'あ'.repeat(21),
      post: '5300001',
      pref: '大阪府',
      city: '大阪市北区',
      tel: '0612345678',
      manager: '田中',
      max: '100',
      current: '101',
    });
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('在庫センター名は20文字以内で入力してください')).toBeVisible();
    await expect(dialog.getByText('郵便番号は 123-4567 の形式で入力してください')).toBeVisible();
    await expect(dialog.getByText('電話番号は 03-1234-5678 の形式で入力してください')).toBeVisible();
    await expect(dialog.getByText('現在保管容量は最大保管容量以下にしてください')).toBeVisible();
  });

  test('同じセンター名で登録すると、サーバーの重複エラーを名前の欄に表示する', async ({ page, request }) => {
    const name = uniqueName('重複');
    created.push((await apiCreateCenter(request, session, name)).centerId);
    await page.goto('/centers');
    await page.getByRole('button', { name: '新規登録' }).click();
    await fillCenterForm(page, {
      name,
      post: '530-0001',
      pref: '大阪府',
      city: '大阪市北区',
      tel: '06-1234-5678',
      manager: '田中一郎',
    });
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('同じ在庫センター名が既に登録されています。')).toBeVisible();
    await expect(dialog).toBeVisible();
  });

  test('編集中に他で削除されたセンターを更新すると、エラーを表示する', async ({ page, request }) => {
    const name = uniqueName('消失');
    const { centerId } = await apiCreateCenter(request, session, name);
    await page.goto('/centers');
    await searchByName(page, name);
    await page.getByRole('button', { name: `${name}を編集` }).click();
    await apiDeleteCenter(request, session, centerId);
    await page.getByRole('dialog').getByRole('button', { name: '更新' }).click();
    await expect(page.getByRole('dialog').getByRole('alert')).toContainText('見つかりません');
  });

  test('すでに削除されているセンターの削除は、成功として一覧から消す', async ({ page, request }) => {
    const name = uniqueName('既削');
    const { centerId } = await apiCreateCenter(request, session, name);
    await page.goto('/centers');
    await searchByName(page, name);
    await page.getByRole('button', { name: `${name}を削除` }).click();
    await apiDeleteCenter(request, session, centerId);
    await page.getByRole('alertdialog').getByRole('button', { name: '削除' }).click();
    await expect(page.getByText('在庫センターを削除しました')).toBeVisible();
    await expect(page.getByRole('row').filter({ hasText: name })).toHaveCount(0);
  });

  test('住所を分割できない既存データも、そのまま編集して保存できる', async ({ page, request }) => {
    const name = uniqueName('旧住所');
    created.push((await apiCreateCenter(request, session, name, { address: '大阪府テスト倉庫' })).centerId);
    await page.goto('/centers');
    await searchByName(page, name);
    const row = page.getByRole('row').filter({ hasText: name });
    // 市区町村を判定できないときは、住所の全体を表示する
    await expect(row).toContainText('大阪府テスト倉庫');
    await row.getByRole('button', { name: `${name}を編集` }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByLabel('市区町村')).toHaveValue('');
    await expect(dialog.getByLabel('番地・建物名')).toHaveValue('テスト倉庫');
    await dialog.getByRole('button', { name: '更新' }).click();
    await expect(page.getByText('在庫センターを更新しました')).toBeVisible();
    await expect(row).toContainText('大阪府テスト倉庫');
  });
});

test.describe('画面の基本的な表示', () => {
  test('狭い画面ではサイドバーをメニューボタンから開ける', async ({ page, context, request }) => {
    await signIn(context, await registerUser(request));
    await page.setViewportSize({ width: 390, height: 800 });
    await page.goto('/centers');
    await page.getByRole('button', { name: 'open menu' }).click();
    await expect(page.getByRole('link', { name: '在庫センター' })).toBeVisible();
  });

  test('サイドバーには実装済みの画面だけを表示する', async ({ page, context, request }) => {
    await signIn(context, await registerUser(request));
    await page.goto('/centers');
    await expect(page.getByRole('link', { name: '在庫センター' })).toHaveCount(1);
    await expect(page.getByRole('link', { name: 'ダッシュボード' })).toHaveCount(0);
  });
});
