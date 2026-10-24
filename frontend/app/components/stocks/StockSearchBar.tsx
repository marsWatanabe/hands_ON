'use client';

import React, { useState } from 'react';
import { Button, FormControl, FormErrorMessage, FormLabel, HStack, Input, Select, SimpleGrid, Stack } from '@chakra-ui/react';
import type { Category, Center, StockKind, StockSearchParams } from '../../types/api';
import { STOCK_LIMITS, validateAmountRange } from '../../lib/stock-form';

/** 検索の絞り込み条件。 */
export type StockFilters = Pick<
  StockSearchParams,
  'categoryId' | 'centerId' | 'name' | 'productCode' | 'amountMin' | 'amountMax'
>;

/** 検索条件を空にした値。 */
export const EMPTY_STOCK_FILTERS: StockFilters = {
  categoryId: '',
  centerId: '',
  name: '',
  productCode: '',
  amountMin: '',
  amountMax: '',
};

interface StockSearchBarProps {
  kind: StockKind;
  /** カテゴリの選択肢（種類に合うカテゴリ）。 */
  categories: Category[];
  /** センターの選択肢。 */
  centers: Center[];
  /** 現在の絞り込み条件（初期値として使う）。 */
  initial: StockFilters;
  /** 検索ボタンが押されたとき（入力が正しいときだけ）。 */
  onSearch: (filters: StockFilters) => void;
}

/** 検索語の名称の上限（バックエンドの StockSearchRequest に合わせる）。 */
const SEARCH_NAME_MAX = 100;

/** 在庫一覧の検索バー（カテゴリ、名称、個数の以上・以下、センター。製品在庫は製品コードも）。 */
export const StockSearchBar: React.FC<StockSearchBarProps> = ({ kind, categories, centers, initial, onSearch }) => {
  const [filters, setFilters] = useState<StockFilters>(initial);
  const [errors, setErrors] = useState<{ amountMin?: string; amountMax?: string }>({});

  const setField = (field: keyof StockFilters) => (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => setFilters((f) => ({ ...f, [field]: e.target.value }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const found = validateAmountRange(filters.amountMin, filters.amountMax);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    onSearch({
      ...filters,
      name: filters.name.trim(),
      productCode: filters.productCode.trim(),
      amountMin: filters.amountMin.trim(),
      amountMax: filters.amountMax.trim(),
    });
  };

  const handleClear = () => {
    setFilters(EMPTY_STOCK_FILTERS);
    setErrors({});
    onSearch(EMPTY_STOCK_FILTERS);
  };

  return (
    <form onSubmit={handleSubmit} role="search" aria-label={kind === 'parts' ? '部品在庫検索' : '製品在庫検索'} noValidate>
      <Stack spacing={4}>
        <SimpleGrid columns={{ base: 1, md: kind === 'products' ? 4 : 3 }} spacing={4}>
          <FormControl>
            <FormLabel>カテゴリ</FormLabel>
            <Select value={filters.categoryId} onChange={setField('categoryId')} bg="white" _dark={{ bg: 'gray.800' }}>
              <option value="">すべて</option>
              {categories.map((c) => (
                <option key={c.categoryId} value={c.categoryId}>
                  {c.categoryName}
                </option>
              ))}
            </Select>
          </FormControl>
          {kind === 'products' && (
            <FormControl>
              <FormLabel>製品コード</FormLabel>
              <Input
                value={filters.productCode}
                onChange={setField('productCode')}
                maxLength={STOCK_LIMITS.productCode}
                placeholder="例: PRD-0000"
                bg="white"
                _dark={{ bg: 'gray.800' }}
              />
            </FormControl>
          )}
          <FormControl>
            <FormLabel>名称</FormLabel>
            <Input
              value={filters.name}
              onChange={setField('name')}
              maxLength={SEARCH_NAME_MAX}
              bg="white"
              _dark={{ bg: 'gray.800' }}
            />
          </FormControl>
          <FormControl>
            <FormLabel>在庫センター</FormLabel>
            <Select value={filters.centerId} onChange={setField('centerId')} bg="white" _dark={{ bg: 'gray.800' }}>
              <option value="">すべて</option>
              {centers.map((c) => (
                <option key={c.centerId} value={c.centerId}>
                  {c.centerName}
                </option>
              ))}
            </Select>
          </FormControl>
        </SimpleGrid>
        <Stack direction={{ base: 'column', md: 'row' }} spacing={4} align={{ md: 'flex-start' }}>
          <FormControl isInvalid={!!errors.amountMin} maxW={{ md: '220px' }}>
            <FormLabel>個数（以上）</FormLabel>
            <Input
              value={filters.amountMin}
              onChange={setField('amountMin')}
              inputMode="numeric"
              bg="white"
              _dark={{ bg: 'gray.800' }}
            />
            <FormErrorMessage>{errors.amountMin}</FormErrorMessage>
          </FormControl>
          <FormControl isInvalid={!!errors.amountMax} maxW={{ md: '220px' }}>
            <FormLabel>個数（以下）</FormLabel>
            <Input
              value={filters.amountMax}
              onChange={setField('amountMax')}
              inputMode="numeric"
              bg="white"
              _dark={{ bg: 'gray.800' }}
            />
            <FormErrorMessage>{errors.amountMax}</FormErrorMessage>
          </FormControl>
          <HStack spacing={2} pt={{ md: 8 }}>
            <Button type="submit" colorScheme="cyan" color="white">
              検索
            </Button>
            <Button type="button" variant="outline" onClick={handleClear}>
              クリア
            </Button>
          </HStack>
        </Stack>
      </Stack>
    </form>
  );
};
