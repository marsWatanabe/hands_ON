import { test, expect } from '@playwright/test';
import {
  apiCreateCategory,
  apiCreateProduct,
  apiDelete,
  centerIdByName,
  registerUser,
  searchBy,
  signIn,
  uniqueCode,
  uniqueName,
  type Session,
} from './helpers';

/**
 * 製品在庫一覧画面の E2E テスト（Chromium）。部品在庫との違い（製品コード）を中心に確認する。
 * 実際のバックエンドを使う。作成したデータは、各テストの最後（または afterEach）で削除する。
 */

test.describe('製品在庫一覧', () => {
  let session: Session;
  let tokyoId: number;
  const categories: number[] = [];
  const stocks: number[] = [];

  test.beforeEach(async ({ context, request }) => {
    session = await registerUser(request);
    await signIn(context, session);
    tokyoId = await centerIdByName(request, session, '東京');
  });

  test.afterEach(async ({ request }) => {
    for (const id of stocks.splice(0)) await apiDelete(request, session, 'products-stocks', id);
    for (const id of categories.splice(0)) await apiDelete(request, session, 'categories', id);
  });

  test('一覧の見出し（製品コードを含む）と、製品コードの表示、製品コードでの検索', async ({ page }) => {
    await page.goto('/products-stocks');
    await expect(page.getByRole('columnheader')).toHaveText([
      /製品コード/,
      /名称/,
      /カテゴリ/,
      /在庫数/,
      /在庫センター/,
      /在庫説明/,
      /操作/,
    ]);
    // 開発用データの製品コードは、マイグレーション（V4）が PRD-000001 から採番している
    await searchBy(page, '製品コード', 'prd-000001');
    const row = page.getByRole('row').filter({ hasText: 'PRD-000001' });
    await expect(row).toHaveCount(1);
    await expect(row).toContainText('災害用ドローン');
    await expect(row).toContainText('東京センター');
  });

  test('登録フォームのカテゴリの選択肢は、製品用のカテゴリだけ', async ({ page }) => {
    await page.goto('/products-stocks');
    await page.getByRole('button', { name: '新規製品在庫登録' }).click();
    const category = page.getByRole('dialog').getByLabel('カテゴリ');
    await expect(category.locator('option', { hasText: '災害用ドローン' })).toHaveCount(1);
    await expect(category.locator('option', { hasText: 'フレーム' })).toHaveCount(0);
  });

  test('登録、編集、削除までの一連の操作（製品コードを含む）', async ({ page, request }) => {
    const categoryName = uniqueName('製');
    categories.push(await apiCreateCategory(request, session, 'product', categoryName));
    const code = uniqueCode();
    const name = uniqueName('製品');
    await page.goto('/products-stocks');

    // 登録
    await page.getByRole('button', { name: '新規製品在庫登録' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('製品コード').fill(code);
    await dialog.getByLabel('名称').fill(name);
    await dialog.getByLabel('カテゴリ').selectOption({ label: categoryName });
    await dialog.getByLabel('在庫センター').selectOption({ label: '東京センター' });
    await dialog.getByLabel('在庫数').fill('7');
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(page.getByText('製品在庫を登録しました')).toBeVisible();

    await searchBy(page, '製品コード', code);
    const row = page.getByRole('row').filter({ hasText: code });
    await expect(row).toHaveCount(1);
    await expect(row).toContainText(name);
    await expect(row).toContainText(categoryName);

    // 編集（製品コードは自分自身なら変えなくてよい。在庫数を更新）
    await row.getByRole('button', { name: `${name}（東京センター）を編集` }).click();
    await expect(dialog.getByLabel('製品コード')).toHaveValue(code);
    await dialog.getByLabel('在庫数').fill('8');
    await dialog.getByRole('button', { name: '更新' }).click();
    await expect(page.getByText('製品在庫を更新しました')).toBeVisible();
    await expect(row).toContainText('8');

    // 削除
    await row.getByRole('button', { name: `${name}（東京センター）を削除` }).click();
    await page.getByRole('alertdialog').getByRole('button', { name: '削除' }).click();
    await expect(page.getByText('製品在庫を削除しました')).toBeVisible();
    await expect(row).toHaveCount(0);
  });

  test('製品コードの入力検証と、重複（削除後も再利用できない）のエラー表示', async ({ page, request }) => {
    const categoryId = await apiCreateCategory(request, session, 'product', uniqueName('製'));
    categories.push(categoryId);
    const code = uniqueCode();
    const existingId = await apiCreateProduct(request, session, {
      productCode: code,
      categoryId,
      centerId: tokyoId,
      name: uniqueName('既存'),
    });
    stocks.push(existingId);
    await page.goto('/products-stocks');

    await page.getByRole('button', { name: '新規製品在庫登録' }).click();
    const dialog = page.getByRole('dialog');

    // 必須・形式（画面で検証し、送信しない）
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('製品コードは必須です')).toBeVisible();
    await dialog.getByLabel('製品コード').fill('製品コード');
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('製品コードは半角英数字、ハイフン、アンダースコアで入力してください')).toBeVisible();
    await dialog.getByLabel('製品コード').fill('A'.repeat(21));
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('製品コードは20文字以内で入力してください')).toBeVisible();

    // 重複（サーバーのエラーを製品コードの欄に表示）
    await dialog.getByLabel('製品コード').fill(code);
    await dialog.getByLabel('名称').fill(uniqueName('別名'));
    await dialog.getByLabel('カテゴリ').selectOption(String(categoryId));
    await dialog.getByLabel('在庫センター').selectOption({ label: '東京センター' });
    await dialog.getByLabel('在庫数').fill('1');
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('同じ製品コードが既に登録されています。')).toBeVisible();

    // 既存の製品を削除しても、製品コードは再利用できない
    await apiDelete(request, session, 'products-stocks', existingId);
    await dialog.getByRole('button', { name: '登録' }).click();
    await expect(dialog.getByText('同じ製品コードが既に登録されています。')).toBeVisible();
  });

  test('製品コードで並び替えできる', async ({ page, request }) => {
    const categoryId = await apiCreateCategory(request, session, 'product', uniqueName('製'));
    categories.push(categoryId);
    const prefix = uniqueName('並');
    const base = uniqueCode();
    for (const s of ['B', 'A', 'C']) {
      stocks.push(
        await apiCreateProduct(request, session, {
          productCode: `${base}-${s}`,
          categoryId,
          centerId: tokyoId,
          name: `${prefix}${s}`,
        }),
      );
    }
    await page.goto('/products-stocks');
    await searchBy(page, '名称', prefix);
    const rows = page.getByRole('row').filter({ hasText: prefix });
    await expect(rows).toHaveCount(3);

    await page.getByRole('button', { name: '製品コードで並び替え' }).click();
    await expect(page.getByRole('columnheader', { name: /製品コード/ })).toHaveAttribute('aria-sort', 'ascending');
    await expect(rows.nth(0)).toContainText(`${base}-A`);
    await expect(rows.nth(2)).toContainText(`${base}-C`);
    await page.getByRole('button', { name: '製品コードで並び替え' }).click();
    await expect(rows.nth(0)).toContainText(`${base}-C`);
  });

  test('ページ送りと、1 ページの件数の変更', async ({ page }) => {
    await page.goto('/products-stocks');
    await page.getByLabel('1 ページの件数').selectOption('10');
    await expect(page.getByText(/全 [\d,]+ 件（1 \/ [2-9]\d* ページ）/)).toBeVisible();
    await page.getByRole('button', { name: '次へ' }).click();
    await expect(page.getByText(/（2 \/ \d+ ページ）/)).toBeVisible();
    await expect(page.getByRole('button', { name: '前へ' })).toBeEnabled();
  });
});
