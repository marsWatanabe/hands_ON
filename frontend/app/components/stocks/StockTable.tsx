'use client';

import React from 'react';
import { Box, Button, HStack, Skeleton, Table, TableContainer, Tbody, Td, Text, Thead, Tr } from '@chakra-ui/react';
import { FiEdit2, FiTrash2 } from 'react-icons/fi';
import type { SortOrder, Stock, StockKind, StockSortKey } from '../../types/api';
import { SortableTh } from '../SortableTh';
import { stockLabel } from '../../lib/stock-form';

interface StockTableProps {
  kind: StockKind;
  stocks: Stock[];
  loading: boolean;
  sort: StockSortKey;
  order: SortOrder;
  onSort: (key: StockSortKey) => void;
  onEdit: (stock: Stock) => void;
  onDelete: (stock: Stock) => void;
}

/** 列の定義。sortKey があれば見出しクリックで並び替えできる。 */
interface Column {
  label: string;
  sortKey?: StockSortKey;
  isNumeric?: boolean;
}

/** 在庫の種類ごとの列（画面仕様どおりの並び）。 */
const COLUMNS: Record<StockKind, Column[]> = {
  parts: [
    { label: 'カテゴリ', sortKey: 'categoryName' },
    { label: '名称', sortKey: 'name' },
    { label: '在庫数（個）', sortKey: 'amount', isNumeric: true },
    { label: '在庫センター', sortKey: 'centerName' },
    { label: '在庫説明' },
    { label: '操作' },
  ],
  products: [
    { label: '製品コード', sortKey: 'productCode' },
    { label: '名称', sortKey: 'name' },
    { label: 'カテゴリ', sortKey: 'categoryName' },
    { label: '在庫数（個）', sortKey: 'amount', isNumeric: true },
    { label: '在庫センター', sortKey: 'centerName' },
    { label: '在庫説明' },
    { label: '操作' },
  ],
};

/** ローディング中に表示する行数。 */
const SKELETON_ROWS = 5;

/** 在庫一覧のテーブル（部品在庫、製品在庫共通）。 */
export const StockTable: React.FC<StockTableProps> = ({
  kind,
  stocks,
  loading,
  sort,
  order,
  onSort,
  onEdit,
  onDelete,
}) => {
  const columns = COLUMNS[kind];
  return (
    <TableContainer bg="white" _dark={{ bg: 'gray.800' }} borderRadius="lg" shadow="sm">
      <Table variant="simple" size="md" aria-busy={loading}>
        <Thead>
          <Tr>
            {columns.map((col) => (
              <SortableTh
                key={col.label}
                label={col.label}
                sortKey={col.sortKey}
                activeKey={sort}
                order={order}
                onSort={onSort}
                isNumeric={col.isNumeric}
              />
            ))}
          </Tr>
        </Thead>
        <Tbody>
          {loading && stocks.length === 0
            ? Array.from({ length: SKELETON_ROWS }, (_, i) => (
                <Tr key={`skeleton-${i}`}>
                  {columns.map((col) => (
                    <Td key={col.label}>
                      <Skeleton height="20px" />
                    </Td>
                  ))}
                </Tr>
              ))
            : stocks.map((s) => (
                <Tr key={s.stockId} opacity={loading ? 0.6 : 1}>
                  {kind === 'products' && <Td fontFamily="monospace">{s.productCode}</Td>}
                  {kind === 'parts' && <Td>{s.categoryName ?? '-'}</Td>}
                  <Td fontWeight="semibold">{s.name}</Td>
                  {kind === 'products' && <Td>{s.categoryName ?? '-'}</Td>}
                  <Td isNumeric>{s.amount === null ? '-' : s.amount.toLocaleString('ja-JP')}</Td>
                  <Td>{s.centerName ?? '-'}</Td>
                  <Td whiteSpace="normal" maxW="320px">
                    <Text noOfLines={2}>{s.description ?? ''}</Text>
                  </Td>
                  <Td>
                    <HStack spacing={2}>
                      <Button
                        size="sm"
                        variant="outline"
                        leftIcon={<FiEdit2 />}
                        onClick={() => onEdit(s)}
                        aria-label={`${stockLabel(s)}を編集`}
                      >
                        編集
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        colorScheme="red"
                        leftIcon={<FiTrash2 />}
                        onClick={() => onDelete(s)}
                        aria-label={`${stockLabel(s)}を削除`}
                      >
                        削除
                      </Button>
                    </HStack>
                  </Td>
                </Tr>
              ))}
        </Tbody>
      </Table>
      {!loading && stocks.length === 0 && (
        <Box p={8} textAlign="center">
          <Text color="gray.500">条件に一致する在庫がありません。</Text>
        </Box>
      )}
    </TableContainer>
  );
};
