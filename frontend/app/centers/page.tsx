'use client';

import React, { useState } from 'react';
import { Alert, AlertIcon, Box, Button, Flex, Heading, VStack, useToast } from '@chakra-ui/react';
import { FiPlus } from 'react-icons/fi';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { SidebarLayout } from '../components/layout/SidebarLayout';
import { useCenters } from '../hooks/useCenters';
import type { Center } from '../types/api';
import { CenterFormModal } from './_components/CenterFormModal';
import { CenterSearchBar } from './_components/CenterSearchBar';
import { CenterTable } from './_components/CenterTable';
import { DeleteCenterDialog } from './_components/DeleteCenterDialog';
import { Pagination } from '../components/Pagination';

/** 通知（トースト）の表示時間（ミリ秒）。 */
const TOAST_DURATION = 4000;

/** 在庫センター管理画面（一覧、検索、並び替え、ページ送り、登録、編集、削除）。 */
export default function CentersPage() {
  const { params, centers, meta, loading, error, applyFilters, toggleSort, goToPage, changePageSize, reload } =
    useCenters();
  const toast = useToast();

  // 登録・編集のモーダル。formOpen と editing で「新規 / 編集 / 閉じている」を表す
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Center | null>(null);
  const [deleting, setDeleting] = useState<Center | null>(null);

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };

  const openEdit = (center: Center) => {
    setEditing(center);
    setFormOpen(true);
  };

  const handleSaved = (saved: Center, mode: 'created' | 'updated') => {
    setFormOpen(false);
    toast({
      status: 'success',
      title: mode === 'created' ? '在庫センターを登録しました' : '在庫センターを更新しました',
      description: saved.centerName,
      duration: TOAST_DURATION,
      isClosable: true,
    });
    reload();
  };

  const handleDeleted = (center: Center) => {
    setDeleting(null);
    toast({
      status: 'success',
      title: '在庫センターを削除しました',
      description: center.centerName,
      duration: TOAST_DURATION,
      isClosable: true,
    });
    reload();
  };

  const handleDeleteError = (message: string) => {
    setDeleting(null);
    toast({ status: 'error', title: '削除できませんでした', description: message, duration: TOAST_DURATION, isClosable: true });
  };

  return (
    <ProtectedRoute>
      <SidebarLayout title="在庫センター管理">
        <VStack align="stretch" spacing={6}>
          <Flex justify="space-between" align="center" wrap="wrap" gap={3}>
            <Heading as="h1" size="lg">
              在庫センター管理
            </Heading>
            <Button leftIcon={<FiPlus />} colorScheme="cyan" color="white" onClick={openCreate}>
              新規登録
            </Button>
          </Flex>

          <Box bg="whiteAlpha.700" _dark={{ bg: 'blackAlpha.300' }} p={4} borderRadius="lg">
            <CenterSearchBar initial={params} onSearch={applyFilters} />
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
            <CenterTable
              centers={centers}
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

        <CenterFormModal isOpen={formOpen} center={editing} onClose={() => setFormOpen(false)} onSaved={handleSaved} />
        <DeleteCenterDialog
          center={deleting}
          onClose={() => setDeleting(null)}
          onDeleted={handleDeleted}
          onError={handleDeleteError}
        />
      </SidebarLayout>
    </ProtectedRoute>
  );
}
