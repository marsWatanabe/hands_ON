import { test, expect } from '@playwright/test';
import {
  buildCategoryQuery,
  createCategory,
  deleteCategory,
  fetchCategories,
  updateCategory,
} from '../../app/lib/categories-api';
import {
  STOCK_PATHS,
  buildStockQuery,
  createStock,
  deleteStock,
  fetchStocks,
  updateStock,
} from '../../app/lib/stocks-api';
import type { CategorySearchParams, StockRequest, StockSearchParams } from '../../app/types/api';

/**
 * カテゴリ API（app/lib/categories-api.ts）と在庫 API（app/lib/stocks-api.ts）の単体テスト。
 * クエリの組み立て（空の条件は送らない、製品コードは製品在庫だけ）と、種類ごとの API のパス、HTTP メソッドを確認する。
 * fetch はテスト内の偽物に置き換える。
 */

interface Call {
  url: string;
  method: string;
  body: string | undefined;
}

let calls: Call[] = [];

/** fetch を、呼び出しを記録して 200 を返す偽物にする。 */
function stubFetch(responseBody: unknown = { data: [], meta: { total: 0, page: 1, size: 20, totalPages: 0 } }) {
  calls = [];
  (globalThis as unknown as Record<string, unknown>).fetch = async (url: string, init: RequestInit = {}) => {
    calls.push({ url, method: init.method ?? 'GET', body: init.body as string | undefined });
    return new Response(JSON.stringify(responseBody), { status: 200 });
  };
}

const categoryDefaults: CategorySearchParams = {
  categoryType: '',
  categoryName: '',
  page: 1,
  size: 20,
  sort: 'categoryType',
  order: 'desc',
};

const stockDefaults: StockSearchParams = {
  categoryId: '',
  centerId: '',
  name: '',
  productCode: '',
  amountMin: '',
  amountMax: '',
  page: 1,
  size: 20,
  sort: 'categoryName',
  order: 'asc',
};

const stockRequest: StockRequest = {
  categoryId: 1,
  name: '部品',
  centerId: 2,
  description: null,
  amount: 3,
};

test.describe('buildCategoryQuery', () => {
  test('既定の条件は、ページとソートだけを送る', () => {
    expect(buildCategoryQuery(categoryDefaults)).toBe('?page=1&size=20&sort=categoryType&order=desc');
  });

  test('種別と名称（前後の空白を除く）を送り、日本語は URL エンコードする', () => {
    const q = buildCategoryQuery({ ...categoryDefaults, categoryType: 'part', categoryName: ' フレーム ', page: 2 });
    const params = new URLSearchParams(q);
    expect(params.get('categoryType')).toBe('part');
    expect(params.get('categoryName')).toBe('フレーム');
    expect(params.get('page')).toBe('2');
    expect(q).toContain('%E3%83%95');
  });

  test('空白だけの名称と、種別「すべて」は送らない', () => {
    const q = buildCategoryQuery({ ...categoryDefaults, categoryName: '   ' });
    expect(q).not.toContain('categoryName=');
    expect(q).not.toContain('categoryType=');
  });
});

test.describe('buildStockQuery', () => {
  test('既定の条件は、ページとソートだけを送る', () => {
    expect(buildStockQuery('parts', stockDefaults)).toBe('?page=1&size=20&sort=categoryName&order=asc');
  });

  test('すべての条件を送る（製品在庫は製品コードも）', () => {
    const params: StockSearchParams = {
      ...stockDefaults,
      categoryId: '11',
      centerId: '2',
      name: ' ドローン ',
      productCode: ' PRD ',
      amountMin: '1',
      amountMax: '50',
      sort: 'amount',
      order: 'desc',
      page: 3,
      size: 50,
    };
    const products = new URLSearchParams(buildStockQuery('products', params));
    expect(Object.fromEntries(products.entries())).toEqual({
      categoryId: '11',
      centerId: '2',
      name: 'ドローン',
      productCode: 'PRD',
      amountMin: '1',
      amountMax: '50',
      page: '3',
      size: '50',
      sort: 'amount',
      order: 'desc',
    });
  });

  test('部品在庫では、製品コードの条件が残っていても送らない', () => {
    expect(buildStockQuery('parts', { ...stockDefaults, productCode: 'PRD' })).not.toContain('productCode');
  });

  test('空白だけの条件と、個数の 0 を区別する（0 は条件として送る）', () => {
    const q = buildStockQuery('parts', { ...stockDefaults, name: '  ', amountMin: '0', amountMax: ' ' });
    expect(q).not.toContain('name=');
    expect(q).toContain('amountMin=0');
    expect(q).not.toContain('amountMax');
  });
});

test.describe('categories-api', () => {
  test('fetchCategories は GET /api/v1/categories を呼ぶ', async () => {
    stubFetch();
    const res = await fetchCategories(categoryDefaults);
    expect(res.meta.total).toBe(0);
    expect(calls[0]).toMatchObject({
      url: '/api/v1/categories?page=1&size=20&sort=categoryType&order=desc',
      method: 'GET',
    });
  });

  test('作成は POST、更新は PUT /{id}、削除は DELETE /{id}', async () => {
    stubFetch({ categoryId: 1, result: 'success' });
    const request = { categoryType: 'part' as const, categoryName: 'フレーム', description: null };
    await createCategory(request);
    await updateCategory(7, request);
    await deleteCategory(7);
    expect(calls.map((c) => [c.method, c.url])).toEqual([
      ['POST', '/api/v1/categories'],
      ['PUT', '/api/v1/categories/7'],
      ['DELETE', '/api/v1/categories/7'],
    ]);
    expect(calls[0].body).toBe(JSON.stringify(request));
  });
});

test.describe('stocks-api', () => {
  test('種類ごとの API のパス', () => {
    expect(STOCK_PATHS).toEqual({ parts: '/parts-stocks', products: '/products-stocks' });
  });

  test('一覧は、種類に応じたパスを呼ぶ', async () => {
    stubFetch();
    await fetchStocks('parts', stockDefaults);
    await fetchStocks('products', { ...stockDefaults, productCode: 'PRD' });
    expect(calls[0].url).toBe('/api/v1/parts-stocks?page=1&size=20&sort=categoryName&order=asc');
    expect(calls[1].url).toBe('/api/v1/products-stocks?productCode=PRD&page=1&size=20&sort=categoryName&order=asc');
  });

  test('作成は POST、更新は PUT /{id}、削除は DELETE /{id}（種類のパス）', async () => {
    stubFetch({ stockId: 1, result: 'success' });
    await createStock('parts', stockRequest);
    await updateStock('products', 9, { ...stockRequest, productCode: 'PRD-1' });
    await deleteStock('parts', 4);
    expect(calls.map((c) => [c.method, c.url])).toEqual([
      ['POST', '/api/v1/parts-stocks'],
      ['PUT', '/api/v1/products-stocks/9'],
      ['DELETE', '/api/v1/parts-stocks/4'],
    ]);
    expect(JSON.parse(calls[1].body as string)).toMatchObject({ productCode: 'PRD-1', amount: 3 });
  });
});
