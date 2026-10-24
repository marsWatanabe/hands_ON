import { test, expect } from '@playwright/test';
import {
  apiCreateCategory,
  apiCreatePart,
  apiDelete,
  centerIdByName,
  registerUser,
  searchBy,
  signIn,
  uniqueName,
  type Session,
} from './helpers';

/**
 * 部品在庫一覧画面の E2E テスト（Chromium）。
 * 実際のバックエンドを使う。作成したデータは、各テストの最後（または afterEach）で削除する。
 */

test.describe('部品在庫一覧', () => {
  let session: Session;
  let tokyoId: number;
  let osakaId: number;
  const categories: number[] = [];
  const stocks: number[] = [];

  test.beforeEach(async ({ context, request }) => {
    session = await registerUser(request);
    await signIn(context, session);
    tokyoId = await centerIdByName(request, session, '東京');
    osakaId = await centerIdByName(request, session, '大阪');
  });

  test.afterEach(async ({ request }) => {
    for (const id of stocks.splice(0)) await apiDelete(request, session, 'parts-stocks', id);
    for (const id of categories.splice(0)) await apiDelete(request, session, 'categories', id);
  });

  test('一覧の見出しと、カテゴリ名・センター名を結合した行の表示', async ({ page }) => {
    await page.goto('/parts-stocks');
    await expect(page.getByRole('columnheader')).toHaveText([
      /カテゴリ/,
      /名称/,
      /在庫数/,
      /在庫センター/,
      /在庫説明/,
      /操作/,
    ]);
    // 開発用データ: 部品名、カテゴリ名、センター名が結合して表示される（同じ部品名はデモデータの他のセンターにもあるため、センターで絞る）
    await searchBy(page, '名称', 'カーボンフレーム');
    const row = page
      .getByRole('row')
      .filter({ hasText: 'カーボンフレーム 450mm' })
      .filter({ hasText: '東京センター' });
    await expect(row).toHaveCount(1);
    await expect(row).toContainText('フレーム');
    await expect(row).toContainText('85');
    await expect(page.getByRole('columnheader', { name: /カテゴリ/ })).toHaveAttribute('aria-sort', 'ascending');
  });

  test('登録フォームのカテゴリの選択肢は、部品用のカテゴリだけ', async ({ page }) => {
    await page.goto('/parts-stocks');
    await page.getByRole('button', { name: '新規部品在庫登録' }).click();
    const category = page.getByRole('dialog').getByLabel('カテゴリ');
    await expect(category.locator('option', { hasText: 'フレーム' })).toHaveCount(1);
    await expect(category.locator('option', { hasText: '災害用ドローン' })).toHaveCount(0);
    await expect(page.getByRole('dialog').getByLabel('在庫センター').locator('option', { hasText: '東京センター' })).toHaveCount(1);
  });

  test('登録、編集、削除までの一連の操作', async ({ page, request }) => {
    const categoryName = uniqueName('部');
    categories.push(await apiCreateCategory(request, session, 'part', categoryName));
    const name = uniqueName('部品');
    await page.goto('/parts-stocks');

    // 登録
    await page.getByRole('button', { name: '新規部品在庫登録' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('名称').fill(name);
    await dialog.getByLabel('カテゴリ').selectOption({ label: categoryName });
    await dialog.getByLabel('在庫センター').selectOption({ label: '東京センター' });
    await dialog.getByLabel('在庫数').fill('30');
    await dialog.getByLabel('在庫説明').fill('E2E の在庫説明');
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(page.getByText('部品在庫を登録しました')).toBeVisible();

    await searchBy(page, '名称', name);
    const row = page.getByRole('row').filter({ hasText: name });
    await expect(row).toHaveCount(1);
    await expect(row).toContainText(categoryName);
    await expect(row).toContainText('東京センター');
    await expect(row).toContainText('30');
    await expect(row).toContainText('E2E の在庫説明');

    // 編集（フォームに現在の値が入る）
    await row.getByRole('button', { name: `${name}（東京センター）を編集` }).click();
    await expect(dialog.getByLabel('名称')).toHaveValue(name);
    await expect(dialog.getByLabel('在庫センター')).toHaveValue(String(tokyoId));
    await expect(dialog.getByLabel('在庫数')).toHaveValue('30');
    await dialog.getByLabel('在庫センター').selectOption({ label: '大阪センター' });
    await dialog.getByLabel('在庫数').fill('45');
    await dialog.getByRole('button', { name: '更新' }).click();
    await expect(page.getByText('部品在庫を更新しました')).toBeVisible();
    await expect(row).toContainText('大阪センター');
    await expect(row).toContainText('45');

    // 削除（キャンセルすると残り、確認すると消える）
    await row.getByRole('button', { name: `${name}（大阪センター）を削除` }).click();
    await page.getByRole('alertdialog').getByRole('button', { name: 'キャンセル' }).click();
    await expect(row).toHaveCount(1);
    await row.getByRole('button', { name: `${name}（大阪センター）を削除` }).click();
    await expect(page.getByRole('alertdialog')).toContainText(name);
    await page.getByRole('alertdialog').getByRole('button', { name: '削除' }).click();
    await expect(page.getByText('部品在庫を削除しました')).toBeVisible();
    await expect(row).toHaveCount(0);
  });

  test('必須項目が空のまま登録すると、項目ごとにエラーを表示して送信しない', async ({ page }) => {
    await page.goto('/parts-stocks');
    await page.getByRole('button', { name: '新規部品在庫登録' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('名称は必須です')).toBeVisible();
    await expect(dialog.getByText('カテゴリは必須です')).toBeVisible();
    await expect(dialog.getByText('在庫センターは必須です')).toBeVisible();
    await expect(dialog.getByText('在庫数は必須です')).toBeVisible();
    await expect(dialog).toBeVisible();

    await dialog.getByLabel('名称').fill('あ');
    await expect(dialog.getByText('名称は必須です')).toHaveCount(0);
    await dialog.getByLabel('在庫数').fill('-1');
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('在庫数は 0 以上の整数で入力してください')).toBeVisible();
  });

  test('同じセンターの同名はエラーを名前の欄に表示し、別のセンターには登録できる', async ({ page, request }) => {
    const categoryId = await apiCreateCategory(request, session, 'part', uniqueName('部'));
    categories.push(categoryId);
    const name = uniqueName('同名');
    stocks.push(await apiCreatePart(request, session, { categoryId, centerId: tokyoId, name }));
    await page.goto('/parts-stocks');

    await page.getByRole('button', { name: '新規部品在庫登録' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('名称').fill(name);
    await dialog.getByLabel('カテゴリ').selectOption(String(categoryId));
    await dialog.getByLabel('在庫センター').selectOption({ label: '東京センター' });
    await dialog.getByLabel('在庫数').fill('1');
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('同じ在庫センターに同じ名称の部品在庫が既に登録されています。')).toBeVisible();

    // センターを変えると登録できる（センターごとに別の管理）
    await dialog.getByLabel('在庫センター').selectOption({ label: '大阪センター' });
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(page.getByText('部品在庫を登録しました')).toBeVisible();
    await searchBy(page, '名称', name);
    await expect(page.getByRole('row').filter({ hasText: name })).toHaveCount(2);

    const res = await request.get('/api/v1/parts-stocks', {
      headers: { Authorization: `Bearer ${session.accessToken}` },
      params: { name },
    });
    for (const s of (await res.json()).data) stocks.push(s.stockId);
  });

  test('センター、個数の範囲で検索でき、範囲が逆のときは検索せずエラーを表示する', async ({ page, request }) => {
    const categoryId = await apiCreateCategory(request, session, 'part', uniqueName('部'));
    categories.push(categoryId);
    const prefix = uniqueName('検');
    stocks.push(await apiCreatePart(request, session, { categoryId, centerId: osakaId, name: `${prefix}少`, amount: 5 }));
    stocks.push(await apiCreatePart(request, session, { categoryId, centerId: osakaId, name: `${prefix}多`, amount: 500 }));
    stocks.push(await apiCreatePart(request, session, { categoryId, centerId: tokyoId, name: `${prefix}東`, amount: 50 }));

    await page.goto('/parts-stocks');
    const search = page.getByRole('search');
    await search.getByLabel('名称').fill(prefix);
    await search.getByLabel('カテゴリ').selectOption(String(categoryId));
    await search.getByRole('button', { name: '検索' }).click();
    await expect(page.getByText('全 3 件')).toBeVisible();

    await search.getByLabel('在庫センター').selectOption({ label: '大阪センター' });
    await search.getByRole('button', { name: '検索' }).click();
    await expect(page.getByText('全 2 件')).toBeVisible();

    await search.getByLabel('個数（以上）').fill('100');
    await search.getByRole('button', { name: '検索' }).click();
    await expect(page.getByText('全 1 件')).toBeVisible();
    await expect(page.getByRole('row').filter({ hasText: `${prefix}多` })).toHaveCount(1);

    // 範囲が逆
    await search.getByLabel('個数（以下）').fill('10');
    await search.getByRole('button', { name: '検索' }).click();
    await expect(search.getByText('個数（以上）は個数（以下）以下にしてください')).toBeVisible();
    await expect(page.getByText('全 1 件')).toBeVisible();

    // クリアで全条件が解除される
    await page.getByRole('button', { name: 'クリア' }).click();
    await expect(page.getByText('全 1 件')).toHaveCount(0);
  });

  test('在庫数で並び替えできる', async ({ page, request }) => {
    const categoryId = await apiCreateCategory(request, session, 'part', uniqueName('部'));
    categories.push(categoryId);
    const prefix = uniqueName('並');
    stocks.push(await apiCreatePart(request, session, { categoryId, centerId: tokyoId, name: `${prefix}1`, amount: 30 }));
    stocks.push(await apiCreatePart(request, session, { categoryId, centerId: tokyoId, name: `${prefix}2`, amount: 10 }));
    stocks.push(await apiCreatePart(request, session, { categoryId, centerId: tokyoId, name: `${prefix}3`, amount: 20 }));

    await page.goto('/parts-stocks');
    await searchBy(page, '名称', prefix);
    const rows = page.getByRole('row').filter({ hasText: prefix });
    await expect(rows).toHaveCount(3);

    await page.getByRole('button', { name: '在庫数（個）で並び替え' }).click();
    await expect(rows.nth(0)).toContainText(`${prefix}2`);
    await expect(rows.nth(2)).toContainText(`${prefix}1`);
    await page.getByRole('button', { name: '在庫数（個）で並び替え' }).click();
    await expect(rows.nth(0)).toContainText(`${prefix}1`);
  });

  test('すでに削除されている在庫の削除は、成功として一覧から消す', async ({ page, request }) => {
    const categoryId = await apiCreateCategory(request, session, 'part', uniqueName('部'));
    categories.push(categoryId);
    const name = uniqueName('既削');
    const stockId = await apiCreatePart(request, session, { categoryId, centerId: tokyoId, name });
    await page.goto('/parts-stocks');
    await searchBy(page, '名称', name);
    await page.getByRole('button', { name: `${name}（東京センター）を削除` }).click();
    await apiDelete(request, session, 'parts-stocks', stockId);
    await page.getByRole('alertdialog').getByRole('button', { name: '削除' }).click();
    await expect(page.getByText('部品在庫を削除しました')).toBeVisible();
    await expect(page.getByRole('row').filter({ hasText: name })).toHaveCount(0);
  });
});
