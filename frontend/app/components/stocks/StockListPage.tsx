'use client';

import React, { useCallback, useState } from 'react';
import { Alert, AlertIcon, Box, Button, Flex, Heading, Text, VStack, useToast } from '@chakra-ui/react';
import { FiPlus } from 'react-icons/fi';
import { ConfirmDialog } from '../ConfirmDialog';
import { Pagination } from '../Pagination';
import { ProtectedRoute } from '../ProtectedRoute';
import { SidebarLayout } from '../layout/SidebarLayout';
import { useCategoryOptions, useCenterOptions } from '../../hooks/useOptions';
import { usePagedList } from '../../hooks/usePagedList';
import { ApiError } from '../../lib/api-client';
import { STOCK_KIND_LABELS, stockLabel } from '../../lib/stock-form';
import { deleteStock, fetchStocks } from '../../lib/stocks-api';
import type { Stock, StockKind, StockSearchParams } from '../../types/api';
import { StockFormModal } from './StockFormModal';
import { StockSearchBar, type StockFilters } from './StockSearchBar';
import { StockTable } from './StockTable';

/** 通知（トースト）の表示時間（ミリ秒）。 */
const TOAST_DURATION = 4000;

/** 一覧の既定の検索条件（画面仕様どおり、カテゴリ、名称の昇順）。 */
const DEFAULT_SEARCH: StockSearchParams = {
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

interface StockListPageProps {
  /** 部品在庫か製品在庫か。 */
  kind: StockKind;
}

/**
 * 在庫一覧の画面（一覧、検索、並び替え、ページ送り、登録、編集、削除）。部品在庫と製品在庫で共通。
 * カテゴリ名とセンター名は API が結合して返す。検索条件と登録フォームの選択肢は、カテゴリとセンターの API から取る。
 */
export function StockListPage({ kind }: StockListPageProps) {
  const labels = STOCK_KIND_LABELS[kind];
  const fetcher = useCallback((p: StockSearchParams) => fetchStocks(kind, p), [kind]);
  const { params, items, meta, loading, error, applyFilters, toggleSort, goToPage, changePageSize, reload } =
    usePagedList<Stock, StockSearchParams>(fetcher, DEFAULT_SEARCH);
  const categories = useCategoryOptions(kind === 'parts' ? 'part' : 'product');
  const centers = useCenterOptions();
  const toast = useToast();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Stock | null>(null);
  const [deleting, setDeleting] = useState<Stock | null>(null);

  const filters: StockFilters = {
    categoryId: params.categoryId,
    centerId: params.centerId,
    name: params.name,
    productCode: params.productCode,
    amountMin: params.amountMin,
    amountMax: params.amountMax,
  };

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };

  const openEdit = (stock: Stock) => {
    setEditing(stock);
    setFormOpen(true);
  };

  const handleSaved = (saved: Stock, mode: 'created' | 'updated') => {
    setFormOpen(false);
    toast({
      status: 'success',
      title: mode === 'created' ? `${labels.item}を登録しました` : `${labels.item}を更新しました`,
      description: saved.name,
      duration: TOAST_DURATION,
      isClosable: true,
    });
    reload();
  };

  /** 削除を実行する。すでに削除済み（404）は成功として扱い、それ以外の失敗はトーストで通知する。 */
  const handleDelete = async () => {
    if (!deleting) return;
    const target = deleting;
    try {
      await deleteStock(kind, target.stockId);
    } catch (err) {
      if (!(err instanceof ApiError && err.status === 404)) {
        setDeleting(null);
        toast({
          status: 'error',
          title: '削除できませんでした',
          description: err instanceof ApiError ? err.message : '削除に失敗しました。',
          duration: TOAST_DURATION,
          isClosable: true,
        });
        return;
      }
    }
    setDeleting(null);
    toast({
      status: 'success',
      title: `${labels.item}を削除しました`,
      description: target.name,
      duration: TOAST_DURATION,
      isClosable: true,
    });
    reload();
  };

  const optionsError = categories.error ?? centers.error;

  return (
    <ProtectedRoute>
      <SidebarLayout title={labels.title}>
        <VStack align="stretch" spacing={6}>
          <Flex justify="space-between" align="center" wrap="wrap" gap={3}>
            <Heading as="h1" size="lg">
              {labels.title}
            </Heading>
            <Button leftIcon={<FiPlus />} colorScheme="cyan" color="white" onClick={openCreate}>
              {`新規${labels.item}登録`}
            </Button>
          </Flex>

          <Box bg="whiteAlpha.700" _dark={{ bg: 'blackAlpha.300' }} p={4} borderRadius="lg">
            <StockSearchBar
              kind={kind}
              categories={categories.options}
              centers={centers.options}
              initial={filters}
              onSearch={applyFilters}
            />
          </Box>

          {optionsError && (
            <Alert status="warning" borderRadius="md" role="alert">
              <AlertIcon />
              カテゴリまたは在庫センターの選択肢を取得できませんでした。{optionsError}
            </Alert>
          )}

          {error && (
            <Alert status="error" borderRadius="md" role="alert">
              <AlertIcon />
              {error}
              <Button ml="auto" size="sm" onClick={reload}>
                再読み込み
              </Button>
            </Alert>
          )}

          <Box>
            <StockTable
              kind={kind}
              stocks={items}
              loading={loading}
              sort={params.sort}
              order={params.order}
              onSort={toggleSort}
              onEdit={openEdit}
              onDelete={setDeleting}
            />
            {meta && <Pagination meta={meta} onPageChange={goToPage} onSizeChange={changePageSize} />}
          </Box>
        </VStack>

        <StockFormModal
          kind={kind}
          isOpen={formOpen}
          stock={editing}
          categories={categories.options}
          centers={centers.options}
          onClose={() => setFormOpen(false)}
          onSaved={handleSaved}
        />
        <ConfirmDialog
          isOpen={deleting !== null}
          title={`${labels.item}の削除`}
          onCancel={() => setDeleting(null)}
          onConfirm={handleDelete}
        >
          <Text>{deleting ? `「${stockLabel(deleting)}」を削除します。よろしいですか？` : ''}</Text>
        </ConfirmDialog>
      </SidebarLayout>
    </ProtectedRoute>
  );
}
