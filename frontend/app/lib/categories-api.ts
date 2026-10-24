import type {
  Category,
  CategoryRequest,
  CategorySearchParams,
  PageResponse,
} from '../types/api';
import { apiFetch } from './api-client';

/** カテゴリ API のパス。 */
const CATEGORIES_PATH = '/categories';

/**
 * 検索条件をクエリ文字列に変換する。空の条件は送らない。
 *
 * @param params 検索条件
 * @returns クエリ文字列（先頭の ? を含む）
 */
export function buildCategoryQuery(params: CategorySearchParams): string {
  const query = new URLSearchParams();
  const categoryName = params.categoryName.trim();
  if (params.categoryType !== '') query.set('categoryType', params.categoryType);
  if (categoryName) query.set('categoryName', categoryName);
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  query.set('sort', params.sort);
  query.set('order', params.order);
  return `?${query.toString()}`;
}

/**
 * カテゴリ一覧を取得する。
 *
 * @param params 検索条件
 * @returns 一覧とページング情報
 */
export function fetchCategories(params: CategorySearchParams): Promise<PageResponse<Category>> {
  return apiFetch<PageResponse<Category>>(`${CATEGORIES_PATH}${buildCategoryQuery(params)}`);
}

/**
 * カテゴリを作成する。
 *
 * @param request 作成内容
 * @returns 作成したカテゴリ
 */
export function createCategory(request: CategoryRequest): Promise<Category> {
  return apiFetch<Category>(CATEGORIES_PATH, { method: 'POST', body: request });
}

/**
 * カテゴリを更新する。
 *
 * @param categoryId カテゴリID
 * @param request 更新内容
 * @returns 更新後のカテゴリ
 */
export function updateCategory(categoryId: number, request: CategoryRequest): Promise<Category> {
  return apiFetch<Category>(`${CATEGORIES_PATH}/${categoryId}`, { method: 'PUT', body: request });
}

/**
 * カテゴリを削除する（論理削除。在庫で使用中のカテゴリは削除できない）。
 *
 * @param categoryId カテゴリID
 */
export async function deleteCategory(categoryId: number): Promise<void> {
  await apiFetch<{ result: string }>(`${CATEGORIES_PATH}/${categoryId}`, { method: 'DELETE' });
}
