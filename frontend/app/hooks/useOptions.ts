'use client';

import { useEffect, useState } from 'react';
import type { Category, CategoryType, Center } from '../types/api';
import { ApiError } from '../lib/api-client';
import { fetchCategories } from '../lib/categories-api';
import { fetchCenters } from '../lib/centers-api';

/** 選択肢として一度に取得する件数（API の 1 回の上限）。 */
const OPTIONS_SIZE = 100;

/** 選択肢の読み込み状態。 */
export interface OptionsState<T> {
  options: T[];
  loading: boolean;
  /** 取得に失敗したときのメッセージ。 */
  error: string | null;
}

/**
 * 非同期に選択肢を読み込む共通処理。アンマウント後の状態更新は行わない。
 *
 * @param load 選択肢を取得する関数
 * @param deps 取得し直す条件
 * @returns 選択肢と読み込み状態
 */
function useOptions<T>(load: () => Promise<T[]>, deps: unknown[]): OptionsState<T> {
  const [state, setState] = useState<OptionsState<T>>({ options: [], loading: true, error: null });

  useEffect(() => {
    let active = true;
    setState((s) => ({ ...s, loading: true, error: null }));
    load()
      .then((options) => {
        if (active) setState({ options, loading: false, error: null });
      })
      .catch((err: unknown) => {
        if (active) {
          setState({
            options: [],
            loading: false,
            error: err instanceof ApiError ? err.message : '選択肢の取得に失敗しました。',
          });
        }
      });
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return state;
}

/**
 * カテゴリの選択肢（名前の昇順、最大 100 件）を取得する。
 *
 * @param type カテゴリ種別（part: 部品、product: 製品）
 * @returns 選択肢と読み込み状態
 */
export function useCategoryOptions(type: CategoryType): OptionsState<Category> {
  return useOptions(
    async () =>
      (
        await fetchCategories({
          categoryType: type,
          categoryName: '',
          page: 1,
          size: OPTIONS_SIZE,
          sort: 'categoryName',
          order: 'asc',
        })
      ).data,
    [type],
  );
}

/**
 * 在庫センターの選択肢（名前の昇順、最大 100 件）を取得する。
 *
 * @returns 選択肢と読み込み状態
 */
export function useCenterOptions(): OptionsState<Center> {
  return useOptions(
    async () =>
      (
        await fetchCenters({
          centerName: '',
          address: '',
          operationalStatus: '',
          page: 1,
          size: OPTIONS_SIZE,
          sort: 'centerName',
          order: 'asc',
        })
      ).data,
    [],
  );
}
