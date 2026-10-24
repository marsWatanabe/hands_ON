'use client';

import React from 'react';
import { Button, HStack, Select, Text } from '@chakra-ui/react';
import type { PageMeta } from '../types/api';

/** 1 ページの件数の選択肢（バックエンドの上限は 100）。 */
export const PAGE_SIZE_OPTIONS = [10, 20, 50] as const;

interface PaginationProps {
  meta: PageMeta;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
}

/** ページ送りと、1 ページの件数の選択。 */
export const Pagination: React.FC<PaginationProps> = ({ meta, onPageChange, onSizeChange }) => {
  // 結果が 0 件のときも「1 / 1」と表示する
  const totalPages = Math.max(meta.totalPages, 1);
  return (
    <HStack justify="space-between" wrap="wrap" spacing={4} mt={4}>
      <Text aria-live="polite">
        全 {meta.total.toLocaleString('ja-JP')} 件（{meta.page} / {totalPages} ページ）
      </Text>
      <HStack spacing={2}>
        <Select
          size="sm"
          w="120px"
          value={meta.size}
          onChange={(e) => onSizeChange(Number(e.target.value))}
          aria-label="1 ページの件数"
          bg="white"
          _dark={{ bg: 'gray.800' }}
        >
          {PAGE_SIZE_OPTIONS.map((n) => (
            <option key={n} value={n}>
              {n} 件ずつ
            </option>
          ))}
        </Select>
        <Button size="sm" onClick={() => onPageChange(meta.page - 1)} isDisabled={meta.page <= 1}>
          前へ
        </Button>
        <Button size="sm" onClick={() => onPageChange(meta.page + 1)} isDisabled={meta.page >= totalPages}>
          次へ
        </Button>
      </HStack>
    </HStack>
  );
};
