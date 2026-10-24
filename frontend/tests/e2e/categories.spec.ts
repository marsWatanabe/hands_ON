import { test, expect } from '@playwright/test';
import { apiCreateCategory, apiCreatePart, apiDelete, centerIdByName, registerUser, searchBy, signIn, uniqueName, type Session } from './helpers';

/**
 * カテゴリ管理画面と、サイドバーの移動の E2E テスト（Chromium）。
 * 実際のバックエンドを使う。作成したデータは、各テストの最後（または afterEach）で削除する。
 */

test.describe('サイドバーの移動', () => {
  test('各画面のリンクから、在庫センター、部品在庫、製品在庫、カテゴリの画面に移動できる', async ({
    page,
    context,
    request,
  }) => {
    await signIn(context, await registerUser(request));
    await page.goto('/centers');
    await expect(page.getByRole('heading', { name: '在庫センター管理', level: 1 })).toBeVisible();

    await page.getByRole('link', { name: '部品在庫一覧' }).click();
    await page.waitForURL('**/parts-stocks');
    await expect(page.getByRole('heading', { name: '部品在庫一覧', level: 1 })).toBeVisible();
    await expect(page).toHaveTitle(/部品在庫一覧 \| Inventrack/);

    await page.getByRole('link', { name: '製品在庫一覧' }).click();
    await page.waitForURL('**/products-stocks');
    await expect(page.getByRole('heading', { name: '製品在庫一覧', level: 1 })).toBeVisible();

    await page.getByRole('link', { name: 'カテゴリ管理' }).click();
    await page.waitForURL('**/categories');
    await expect(page.getByRole('heading', { name: 'カテゴリ管理', level: 1 })).toBeVisible();
    await expect(page).toHaveTitle(/カテゴリ管理 \| Inventrack/);
  });

  test('未ログインで新しい画面を開くと、/login へ移動する', async ({ page }) => {
    for (const path of ['/categories', '/parts-stocks', '/products-stocks']) {
      await page.context().clearCookies();
      await page.goto(path);
      await page.waitForURL('**/login');
    }
  });
});

test.describe('カテゴリ管理', () => {
  let session: Session;
  const created: number[] = [];

  test.beforeEach(async ({ context, request }) => {
    session = await registerUser(request);
    await signIn(context, session);
  });

  test.afterEach(async ({ request }) => {
    for (const id of created.splice(0)) await apiDelete(request, session, 'categories', id);
  });

  test('一覧の見出しと、既定の並び（種別の降順）、件数の表示', async ({ page }) => {
    await page.goto('/categories');
    await expect(page.getByRole('columnheader')).toHaveText([/カテゴリ種別/, /カテゴリ名/, /説明/, /操作/]);
    // 既定は種別の降順（製品が先）
    await expect(page.getByRole('row').nth(1)).toContainText('製品');
    await expect(page.getByText(/全 [\d,]+ 件（1 \/ \d+ ページ）/)).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /カテゴリ種別/ })).toHaveAttribute('aria-sort', 'descending');
  });

  test('種別とカテゴリ名で検索でき、クリアで解除できる', async ({ page }) => {
    await page.goto('/categories');
    const search = page.getByRole('search');

    await search.getByLabel('カテゴリ種別').selectOption('part');
    await search.getByRole('button', { name: '検索' }).click();
    await expect(page.getByRole('row').filter({ hasText: '部品' }).first()).toBeVisible();
    await expect(page.getByRole('row').filter({ hasText: '災害用ドローン' })).toHaveCount(0);

    await search.getByLabel('カテゴリ種別').selectOption('');
    await searchBy(page, 'カテゴリ名', 'ドローン');
    await expect(page.getByRole('row').filter({ hasText: '災害用ドローン' })).toHaveCount(1);
    await expect(page.getByRole('row').filter({ hasText: 'フレーム' })).toHaveCount(0);

    await page.getByRole('button', { name: 'クリア' }).click();
    await expect(page.getByRole('row').filter({ hasText: 'フレーム' })).toHaveCount(1);
  });

  test('条件に一致しないときは 0 件のメッセージを表示する', async ({ page }) => {
    await page.goto('/categories');
    await searchBy(page, 'カテゴリ名', 'ZZZ存在しない');
    await expect(page.getByText('条件に一致するカテゴリがありません。')).toBeVisible();
    await expect(page.getByText('全 0 件')).toBeVisible();
  });

  test('見出しのクリックで、カテゴリ名を昇順、降順に並び替える', async ({ page, request }) => {
    const prefix = uniqueName('並');
    for (const s of ['B', 'A', 'C']) created.push(await apiCreateCategory(request, session, 'part', `${prefix}${s}`));
    await page.goto('/categories');
    await searchBy(page, 'カテゴリ名', prefix);
    const rows = page.getByRole('row').filter({ hasText: prefix });
    await expect(rows).toHaveCount(3);

    await page.getByRole('button', { name: 'カテゴリ名で並び替え' }).click();
    await expect(rows.nth(0)).toContainText(`${prefix}A`);
    await expect(rows.nth(2)).toContainText(`${prefix}C`);
    await page.getByRole('button', { name: 'カテゴリ名で並び替え' }).click();
    await expect(rows.nth(0)).toContainText(`${prefix}C`);
  });

  test('登録、編集、削除までの一連の操作', async ({ page }) => {
    const name = uniqueName('一連');
    await page.goto('/categories');

    // 登録
    await page.getByRole('button', { name: '新規カテゴリ登録' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('カテゴリ種別').selectOption('part');
    await dialog.getByLabel('カテゴリ名').fill(name);
    await dialog.getByLabel('説明').fill('E2E の説明');
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(page.getByText('カテゴリを登録しました')).toBeVisible();

    await searchBy(page, 'カテゴリ名', name);
    const row = page.getByRole('row').filter({ hasText: name });
    await expect(row).toHaveCount(1);
    await expect(row).toContainText('部品');
    await expect(row).toContainText('E2E の説明');

    // 編集（種別は、在庫で使われていなければ変えられる）
    await row.getByRole('button', { name: `${name}を編集` }).click();
    await expect(dialog.getByLabel('カテゴリ名')).toHaveValue(name);
    await dialog.getByLabel('カテゴリ種別').selectOption('product');
    await dialog.getByLabel('説明').fill('更新した説明');
    await dialog.getByRole('button', { name: '更新' }).click();
    await expect(page.getByText('カテゴリを更新しました')).toBeVisible();
    await expect(row).toContainText('製品');
    await expect(row).toContainText('更新した説明');

    // 削除（キャンセルすると残り、確認すると消える）
    await row.getByRole('button', { name: `${name}を削除` }).click();
    await page.getByRole('alertdialog').getByRole('button', { name: 'キャンセル' }).click();
    await expect(row).toHaveCount(1);
    await row.getByRole('button', { name: `${name}を削除` }).click();
    await expect(page.getByRole('alertdialog')).toContainText(name);
    await page.getByRole('alertdialog').getByRole('button', { name: '削除' }).click();
    await expect(page.getByText('カテゴリを削除しました')).toBeVisible();
    await expect(row).toHaveCount(0);
  });

  test('必須項目が空のまま登録すると、項目ごとにエラーを表示して送信しない', async ({ page }) => {
    await page.goto('/categories');
    await page.getByRole('button', { name: '新規カテゴリ登録' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('カテゴリ種別は必須です')).toBeVisible();
    await expect(dialog.getByText('カテゴリ名は必須です')).toBeVisible();
    await expect(dialog).toBeVisible();

    await dialog.getByLabel('カテゴリ名').fill('あ'.repeat(21));
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('カテゴリ名は20文字以内で入力してください')).toBeVisible();
  });

  test('同じ種別の同名はサーバーの重複エラーを名前の欄に表示し、別の種別なら登録できる', async ({ page, request }) => {
    const name = uniqueName('重複');
    created.push(await apiCreateCategory(request, session, 'part', name));
    await page.goto('/categories');

    await page.getByRole('button', { name: '新規カテゴリ登録' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('カテゴリ種別').selectOption('part');
    await dialog.getByLabel('カテゴリ名').fill(name);
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('同じ種別に同じカテゴリ名が既に登録されています。')).toBeVisible();

    // 種別を変えると登録できる
    await dialog.getByLabel('カテゴリ種別').selectOption('product');
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(page.getByText('カテゴリを登録しました')).toBeVisible();
    await searchBy(page, 'カテゴリ名', name);
    await expect(page.getByRole('row').filter({ hasText: name })).toHaveCount(2);
    // 後片付け（画面で作った製品側）
    const ids = await request.get('/api/v1/categories', {
      headers: { Authorization: `Bearer ${session.accessToken}` },
      params: { categoryName: name },
    });
    for (const c of (await ids.json()).data) created.push(c.categoryId);
  });

  test('在庫で使用中のカテゴリは、削除できないことをトーストで通知する', async ({ page, request }) => {
    const name = uniqueName('使用中');
    const categoryId = await apiCreateCategory(request, session, 'part', name);
    const tokyoId = await centerIdByName(request, session, '東京');
    const stockId = await apiCreatePart(request, session, { categoryId, centerId: tokyoId, name: `在庫${name}` });
    created.push(categoryId);

    await page.goto('/categories');
    await searchBy(page, 'カテゴリ名', name);
    const row = page.getByRole('row').filter({ hasText: name });
    await row.getByRole('button', { name: `${name}を削除` }).click();
    await page.getByRole('alertdialog').getByRole('button', { name: '削除' }).click();
    await expect(page.getByText('削除できませんでした')).toBeVisible();
    await expect(page.getByText('在庫で使用されているため削除できません', { exact: false }).first()).toBeVisible();
    await expect(row).toHaveCount(1);

    // 使用中のカテゴリの種別は変えられない（フォームにエラーを表示する）
    await row.getByRole('button', { name: `${name}を編集` }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('カテゴリ種別').selectOption('product');
    await dialog.getByRole('button', { name: '更新' }).click();
    await expect(dialog.getByRole('alert')).toContainText('種別を変更できません');

    await apiDelete(request, session, 'parts-stocks', stockId);
  });
});
