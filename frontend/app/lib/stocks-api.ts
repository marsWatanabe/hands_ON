import type { PageResponse, Stock, StockKind, StockRequest, StockSearchParams } from '../types/api';
import { apiFetch } from './api-client';

/** 在庫の種類ごとの API のパス。 */
export const STOCK_PATHS: Record<StockKind, string> = {
  parts: '/parts-stocks',
  products: '/products-stocks',
};

/**
 * 検索条件をクエリ文字列に変換する。空の条件は送らない。製品コードは製品在庫のときだけ送る。
 *
 * @param kind 在庫の種類
 * @param params 検索条件
 * @returns クエリ文字列（先頭の ? を含む）
 */
export function buildStockQuery(kind: StockKind, params: StockSearchParams): string {
  const query = new URLSearchParams();
  const name = params.name.trim();
  const productCode = params.productCode.trim();
  if (params.categoryId !== '') query.set('categoryId', params.categoryId);
  if (params.centerId !== '') query.set('centerId', params.centerId);
  if (name) query.set('name', name);
  if (kind === 'products' && productCode) query.set('productCode', productCode);
  if (params.amountMin.trim() !== '') query.set('amountMin', params.amountMin.trim());
  if (params.amountMax.trim() !== '') query.set('amountMax', params.amountMax.trim());
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  query.set('sort', params.sort);
  query.set('order', params.order);
  return `?${query.toString()}`;
}

/**
 * 在庫一覧を取得する。カテゴリ名とセンター名は API が結合して返す。
 *
 * @param kind 在庫の種類
 * @param params 検索条件
 * @returns 一覧とページング情報
 */
export function fetchStocks(kind: StockKind, params: StockSearchParams): Promise<PageResponse<Stock>> {
  return apiFetch<PageResponse<Stock>>(`${STOCK_PATHS[kind]}${buildStockQuery(kind, params)}`);
}

/**
 * 在庫を作成する。
 *
 * @param kind 在庫の種類
 * @param request 作成内容
 * @returns 作成した在庫
 */
export function createStock(kind: StockKind, request: StockRequest): Promise<Stock> {
  return apiFetch<Stock>(STOCK_PATHS[kind], { method: 'POST', body: request });
}

/**
 * 在庫を更新する。
 *
 * @param kind 在庫の種類
 * @param stockId 在庫ID
 * @param request 更新内容
 * @returns 更新後の在庫
 */
export function updateStock(kind: StockKind, stockId: number, request: StockRequest): Promise<Stock> {
  return apiFetch<Stock>(`${STOCK_PATHS[kind]}/${stockId}`, { method: 'PUT', body: request });
}

/**
 * 在庫を削除する（論理削除）。
 *
 * @param kind 在庫の種類
 * @param stockId 在庫ID
 */
export async function deleteStock(kind: StockKind, stockId: number): Promise<void> {
  await apiFetch<{ result: string }>(`${STOCK_PATHS[kind]}/${stockId}`, { method: 'DELETE' });
}
