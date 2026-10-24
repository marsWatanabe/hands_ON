import type {
  Center,
  CenterRequest,
  CenterSearchParams,
  PageResponse,
} from '../types/api';
import { apiFetch } from './api-client';

/** 在庫センター API のパス。 */
const CENTERS_PATH = '/centers';

/**
 * 検索条件をクエリ文字列に変換する。空の条件は送らない。
 *
 * @param params 検索条件
 * @returns クエリ文字列（先頭の ? を含む）
 */
export function buildCenterQuery(params: CenterSearchParams): string {
  const query = new URLSearchParams();
  const centerName = params.centerName.trim();
  const address = params.address.trim();
  if (centerName) query.set('centerName', centerName);
  if (address) query.set('address', address);
  if (params.operationalStatus !== '') query.set('operationalStatus', params.operationalStatus);
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  query.set('sort', params.sort);
  query.set('order', params.order);
  return `?${query.toString()}`;
}

/**
 * 在庫センター一覧を取得する。
 *
 * @param params 検索条件
 * @returns 一覧とページング情報
 */
export function fetchCenters(params: CenterSearchParams): Promise<PageResponse<Center>> {
  return apiFetch<PageResponse<Center>>(`${CENTERS_PATH}${buildCenterQuery(params)}`);
}

/**
 * 在庫センターを作成する。
 *
 * @param request 作成内容
 * @returns 作成した在庫センター
 */
export function createCenter(request: CenterRequest): Promise<Center> {
  return apiFetch<Center>(CENTERS_PATH, { method: 'POST', body: request });
}

/**
 * 在庫センターを更新する。
 *
 * @param centerId 在庫センターID
 * @param request 更新内容
 * @returns 更新後の在庫センター
 */
export function updateCenter(centerId: number, request: CenterRequest): Promise<Center> {
  return apiFetch<Center>(`${CENTERS_PATH}/${centerId}`, { method: 'PUT', body: request });
}

/**
 * 在庫センターを削除する（論理削除）。
 *
 * @param centerId 在庫センターID
 */
export async function deleteCenter(centerId: number): Promise<void> {
  await apiFetch<{ result: string }>(`${CENTERS_PATH}/${centerId}`, { method: 'DELETE' });
}
