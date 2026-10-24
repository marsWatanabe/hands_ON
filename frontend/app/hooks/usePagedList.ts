'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import type { PageMeta, PageResponse, SortOrder } from '../types/api';
import { ApiError } from '../lib/api-client';

/** usePagedList が扱う検索条件の共通部分。 */
export interface PagedParams<S extends string = string> {
  page: number;
  size: number;
  sort: S;
  order: SortOrder;
}

/** usePagedList の戻り値。 */
export interface UsePagedListResult<T, P extends PagedParams> {
  /** 現在の検索条件。 */
  params: P;
  /** 表示中のページの要素。 */
  items: T[];
  /** ページング情報（まだ取得していなければ null）。 */
  meta: PageMeta | null;
  /** 取得中は true。 */
  loading: boolean;
  /** 取得に失敗したときのメッセージ。 */
  error: string | null;
  /** 絞り込み条件などを変えて、1 ページ目から検索し直す。 */
  applyFilters: (filters: Partial<P>) => void;
  /** 並び替える。同じ項目を続けて選ぶと昇順・降順を切り替える。 */
  toggleSort: (key: P['sort']) => void;
  /** ページを移動する。 */
  goToPage: (page: number) => void;
  /** 1 ページの件数を変えて、1 ページ目から検索し直す。 */
  changePageSize: (size: number) => void;
  /** 現在の条件で取り直す（登録・更新・削除のあと）。 */
  reload: () => void;
}

/**
 * 一覧の取得と、検索条件（絞り込み、ソート、ページ）の管理を行う汎用フック。
 * 古いリクエストの結果は破棄する（条件を素早く変えたときの順序逆転を防ぐ）。
 * 最後のページの最後の 1 件を削除して空になったときは、1 つ前のページへ戻る。
 *
 * @param fetcher 検索条件から一覧を取得する関数（安定した参照で渡すこと）
 * @param defaults 検索条件の初期値
 * @returns 一覧の状態と操作
 */
export function usePagedList<T, P extends PagedParams>(
  fetcher: (params: P) => Promise<PageResponse<T>>,
  defaults: P,
): UsePagedListResult<T, P> {
  const [params, setParams] = useState<P>(defaults);
  const [items, setItems] = useState<T[]>([]);
  const [meta, setMeta] = useState<PageMeta | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  // 取り直しの要求番号。変わると再取得する。
  const [reloadKey, setReloadKey] = useState(0);
  const latestRequest = useRef(0);

  useEffect(() => {
    const requestId = ++latestRequest.current;
    setLoading(true);
    fetcher(params)
      .then((res) => {
        if (requestId !== latestRequest.current) return;
        if (res.data.length === 0 && params.page > 1 && res.meta.totalPages < params.page) {
          setParams((p) => ({ ...p, page: Math.max(1, res.meta.totalPages) }));
          return;
        }
        setItems(res.data);
        setMeta(res.meta);
        setError(null);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (requestId !== latestRequest.current) return;
        setError(err instanceof ApiError ? err.message : '一覧の取得に失敗しました。');
        setLoading(false);
      });
  }, [fetcher, params, reloadKey]);

  const applyFilters = useCallback((filters: Partial<P>) => {
    setParams((p) => ({ ...p, ...filters, page: 1 }));
  }, []);

  const toggleSort = useCallback((key: P['sort']) => {
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

  return { params, items, meta, loading, error, applyFilters, toggleSort, goToPage, changePageSize, reload };
}
