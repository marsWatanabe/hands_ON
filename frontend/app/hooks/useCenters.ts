'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import type { Center, CenterSearchParams, CenterSortKey, PageMeta } from '../types/api';
import { ApiError } from '../lib/api-client';
import { fetchCenters } from '../lib/centers-api';

/** 一覧の既定の検索条件（新しい順、20 件）。 */
export const DEFAULT_CENTER_SEARCH: CenterSearchParams = {
  centerName: '',
  address: '',
  operationalStatus: '',
  page: 1,
  size: 20,
  sort: 'centerId',
  order: 'desc',
};

/** 検索の絞り込み条件（ページ・ソートを除く）。 */
export type CenterFilters = Pick<CenterSearchParams, 'centerName' | 'address' | 'operationalStatus'>;

/** useCenters の戻り値。 */
export interface UseCentersResult {
  /** 現在の検索条件。 */
  params: CenterSearchParams;
  /** 表示中のページの在庫センター。 */
  centers: Center[];
  /** ページング情報（まだ取得していなければ null）。 */
  meta: PageMeta | null;
  /** 取得中は true。 */
  loading: boolean;
  /** 取得に失敗したときのメッセージ。 */
  error: string | null;
  /** 絞り込み条件を変えて 1 ページ目から検索し直す。 */
  applyFilters: (filters: CenterFilters) => void;
  /** 並び替える。同じ項目を続けて選ぶと昇順・降順を切り替える。 */
  toggleSort: (key: CenterSortKey) => void;
  /** ページを移動する。 */
  goToPage: (page: number) => void;
  /** 1 ページの件数を変えて 1 ページ目から検索し直す。 */
  changePageSize: (size: number) => void;
  /** 現在の条件で取り直す（登録・更新・削除のあと）。 */
  reload: () => void;
}

/**
 * 在庫センター一覧の取得と、検索条件（絞り込み・ソート・ページ）の管理を行うフック。
 * 古いリクエストの結果は破棄する（条件を素早く変えたときの順序逆転を防ぐ）。
 *
 * @returns 一覧の状態と操作
 */
export function useCenters(): UseCentersResult {
  const [params, setParams] = useState<CenterSearchParams>(DEFAULT_CENTER_SEARCH);
  const [centers, setCenters] = useState<Center[]>([]);
  const [meta, setMeta] = useState<PageMeta | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  // 取り直しの要求番号。変わると再取得する。
  const [reloadKey, setReloadKey] = useState(0);
  const latestRequest = useRef(0);

  useEffect(() => {
    const requestId = ++latestRequest.current;
    setLoading(true);
    fetchCenters(params)
      .then((res) => {
        if (requestId !== latestRequest.current) return;
        // 最後のページの最後の 1 件を削除したときなどは、1 つ前のページへ戻る
        if (res.data.length === 0 && params.page > 1 && res.meta.totalPages < params.page) {
          setParams((p) => ({ ...p, page: Math.max(1, res.meta.totalPages) }));
          return;
        }
        setCenters(res.data);
        setMeta(res.meta);
        setError(null);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (requestId !== latestRequest.current) return;
        setError(err instanceof ApiError ? err.message : '在庫センターの取得に失敗しました。');
        setLoading(false);
      });
  }, [params, reloadKey]);

  const applyFilters = useCallback((filters: CenterFilters) => {
    setParams((p) => ({ ...p, ...filters, page: 1 }));
  }, []);

  const toggleSort = useCallback((key: CenterSortKey) => {
    setParams((p) => ({
      ...p,
      sort: key,
      order: p.sort === key && p.order === 'asc' ? 'desc' : 'asc',
      page: 1,
    }));
  }, []);

  const goToPage = useCallback((page: number) => {
    setParams((p) => ({ ...p, page }));
  }, []);

  const changePageSize = useCallback((size: number) => {
    setParams((p) => ({ ...p, size, page: 1 }));
  }, []);

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  return { params, centers, meta, loading, error, applyFilters, toggleSort, goToPage, changePageSize, reload };
}
