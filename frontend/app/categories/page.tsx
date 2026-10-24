'use client';

import React, { useState } from 'react';
import { Alert, AlertIcon, Box, Button, Flex, Heading, Text, VStack, useToast } from '@chakra-ui/react';
import { FiPlus } from 'react-icons/fi';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Pagination } from '../components/Pagination';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { SidebarLayout } from '../components/layout/SidebarLayout';
import { usePagedList } from '../hooks/usePagedList';
import { ApiError } from '../lib/api-client';
import { deleteCategory, fetchCategories } from '../lib/categories-api';
import type { Category, CategorySearchParams } from '../types/api';
import { CategoryFormModal } from './_components/CategoryFormModal';
import { CategorySearchBar } from './_components/CategorySearchBar';
import { CategoryTable } from './_components/CategoryTable';

/** 通知（トースト）の表示時間（ミリ秒）。 */
const TOAST_DURATION = 4000;

/** 一覧の既定の検索条件（画面仕様どおり、種別の降順、カテゴリ名の降順）。 */
const DEFAULT_SEARCH: CategorySearchParams = {
  categoryType: '',
  categoryName: '',
  page: 1,
  size: 20,
  sort: 'categoryType',
  order: 'desc',
};

/** カテゴリ管理画面（一覧、検索、並び替え、ページ送り、登録、編集、削除）。 */
export default function CategoriesPage() {
  const { params, items, meta, loading, error, applyFilters, toggleSort, goToPage, changePageSize, reload } =
    usePagedList<Category, CategorySearchParams>(fetchCategories, DEFAULT_SEARCH);
  const toast = useToast();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);
  const [deleting, setDeleting] = useState<Category | null>(null);

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };

  const openEdit = (category: Category) => {
    setEditing(category);
    setFormOpen(true);
  };

  const handleSaved = (saved: Category, mode: 'created' | 'updated') => {
    setFormOpen(false);
    toast({
      status: 'success',
      title: mode === 'created' ? 'カテゴリを登録しました' : 'カテゴリを更新しました',
      description: saved.categoryName,
      duration: TOAST_DURATION,
      isClosable: true,
    });
    reload();
  };

  /** 削除を実行する。在庫で使用中（400）などの失敗はトーストで通知し、すでに削除済み（404）は成功として扱う。 */
  const handleDelete = async () => {
    if (!deleting) return;
    const target = deleting;
    try {
      await deleteCategory(target.categoryId);
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
      title: 'カテゴリを削除しました',
      description: target.categoryName,
      duration: TOAST_DURATION,
      isClosable: true,
    });
    reload();
  };

  return (
    <ProtectedRoute>
      <SidebarLayout title="カテゴリ管理">
        <VStack align="stretch" spacing={6}>
          <Flex justify="space-between" align="center" wrap="wrap" gap={3}>
            <Heading as="h1" size="lg">
              カテゴリ管理
            </Heading>
            <Button leftIcon={<FiPlus />} colorScheme="cyan" color="white" onClick={openCreate}>
              新規カテゴリ登録
            </Button>
          </Flex>

          <Box bg="whiteAlpha.700" _dark={{ bg: 'blackAlpha.300' }} p={4} borderRadius="lg">
            <CategorySearchBar initial={params} onSearch={applyFilters} />
          </Box>

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
            <CategoryTable
              categories={items}
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

        <CategoryFormModal
          isOpen={formOpen}
          category={editing}
          onClose={() => setFormOpen(false)}
          onSaved={handleSaved}
        />
        <ConfirmDialog
          isOpen={deleting !== null}
          title="カテゴリの削除"
          onCancel={() => setDeleting(null)}
          onConfirm={handleDelete}
        >
          <Text>「{deleting?.categoryName}」を削除します。よろしいですか？</Text>
          <Text mt={2} fontSize="sm" color="gray.500">
            在庫で使用されているカテゴリは削除できません。
          </Text>
        </ConfirmDialog>
      </SidebarLayout>
    </ProtectedRoute>
  );
}
