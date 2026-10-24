'use client';

import React, { useState } from 'react';
import { Button, FormControl, FormLabel, HStack, Input, Select, Stack } from '@chakra-ui/react';
import type { CategorySearchParams } from '../../types/api';
import { CATEGORY_LIMITS, CATEGORY_TYPE_LABELS } from '../../lib/category-form';

/** 検索の絞り込み条件。 */
export type CategoryFilters = Pick<CategorySearchParams, 'categoryType' | 'categoryName'>;

interface CategorySearchBarProps {
  /** 現在の絞り込み条件（初期値として使う）。 */
  initial: CategoryFilters;
  /** 検索ボタンが押されたとき。 */
  onSearch: (filters: CategoryFilters) => void;
}

/** カテゴリ一覧の検索バー（カテゴリ種別、カテゴリ名）。 */
export const CategorySearchBar: React.FC<CategorySearchBarProps> = ({ initial, onSearch }) => {
  const [categoryType, setCategoryType] = useState<CategoryFilters['categoryType']>(initial.categoryType);
  const [categoryName, setCategoryName] = useState(initial.categoryName);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch({ categoryType, categoryName: categoryName.trim() });
  };

  const handleClear = () => {
    setCategoryType('');
    setCategoryName('');
    onSearch({ categoryType: '', categoryName: '' });
  };

  return (
    <form onSubmit={handleSubmit} role="search" aria-label="カテゴリ検索">
      <Stack direction={{ base: 'column', md: 'row' }} spacing={4} align={{ md: 'flex-end' }}>
        <FormControl maxW={{ md: '220px' }}>
          <FormLabel>カテゴリ種別</FormLabel>
          <Select
            value={categoryType}
            onChange={(e) => setCategoryType(e.target.value as CategoryFilters['categoryType'])}
            bg="white"
            _dark={{ bg: 'gray.800' }}
          >
            <option value="">すべて</option>
            <option value="part">{CATEGORY_TYPE_LABELS.part}</option>
            <option value="product">{CATEGORY_TYPE_LABELS.product}</option>
          </Select>
        </FormControl>
        <FormControl>
          <FormLabel>カテゴリ名</FormLabel>
          <Input
            value={categoryName}
            onChange={(e) => setCategoryName(e.target.value)}
            maxLength={CATEGORY_LIMITS.categoryName}
            placeholder="例: フレーム"
            bg="white"
            _dark={{ bg: 'gray.800' }}
          />
        </FormControl>
        <HStack spacing={2}>
          <Button type="submit" colorScheme="cyan" color="white">
            検索
          </Button>
          <Button type="button" variant="outline" onClick={handleClear}>
            クリア
          </Button>
        </HStack>
      </Stack>
    </form>
  );
};
