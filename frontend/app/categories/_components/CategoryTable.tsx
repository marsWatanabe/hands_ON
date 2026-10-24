'use client';

import React from 'react';
import {
  Badge,
  Box,
  Button,
  HStack,
  Skeleton,
  Table,
  TableContainer,
  Tbody,
  Td,
  Text,
  Thead,
  Tr,
} from '@chakra-ui/react';
import { FiEdit2, FiTrash2 } from 'react-icons/fi';
import type { Category, CategorySortKey, SortOrder } from '../../types/api';
import { SortableTh } from '../../components/SortableTh';
import { CATEGORY_TYPE_LABELS } from '../../lib/category-form';

interface CategoryTableProps {
  categories: Category[];
  loading: boolean;
  sort: CategorySortKey;
  order: SortOrder;
  onSort: (key: CategorySortKey) => void;
  onEdit: (category: Category) => void;
  onDelete: (category: Category) => void;
}

/** 列の定義。sortKey があれば見出しクリックで並び替えできる。 */
const COLUMNS: Array<{ label: string; sortKey?: CategorySortKey }> = [
  { label: 'カテゴリ種別', sortKey: 'categoryType' },
  { label: 'カテゴリ名', sortKey: 'categoryName' },
  { label: '説明' },
  { label: '操作' },
];

/** ローディング中に表示する行数。 */
const SKELETON_ROWS = 5;

/** カテゴリ一覧のテーブル。 */
export const CategoryTable: React.FC<CategoryTableProps> = ({
  categories,
  loading,
  sort,
  order,
  onSort,
  onEdit,
  onDelete,
}) => (
  <TableContainer bg="white" _dark={{ bg: 'gray.800' }} borderRadius="lg" shadow="sm">
    <Table variant="simple" size="md" aria-busy={loading}>
      <Thead>
        <Tr>
          {COLUMNS.map((col) => (
            <SortableTh key={col.label} label={col.label} sortKey={col.sortKey} activeKey={sort} order={order} onSort={onSort} />
          ))}
        </Tr>
      </Thead>
      <Tbody>
        {loading && categories.length === 0
          ? Array.from({ length: SKELETON_ROWS }, (_, i) => (
              <Tr key={`skeleton-${i}`}>
                {COLUMNS.map((col) => (
                  <Td key={col.label}>
                    <Skeleton height="20px" />
                  </Td>
                ))}
              </Tr>
            ))
          : categories.map((c) => (
              <Tr key={c.categoryId} opacity={loading ? 0.6 : 1}>
                <Td>
                  <Badge colorScheme={c.categoryType === 'part' ? 'blue' : 'purple'}>
                    {CATEGORY_TYPE_LABELS[c.categoryType]}
                  </Badge>
                </Td>
                <Td fontWeight="semibold">{c.categoryName}</Td>
                <Td whiteSpace="normal" maxW="360px">
                  <Text noOfLines={2}>{c.description ?? ''}</Text>
                </Td>
                <Td>
                  <HStack spacing={2}>
                    <Button
                      size="sm"
                      variant="outline"
                      leftIcon={<FiEdit2 />}
                      onClick={() => onEdit(c)}
                      aria-label={`${c.categoryName}を編集`}
                    >
                      編集
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      colorScheme="red"
                      leftIcon={<FiTrash2 />}
                      onClick={() => onDelete(c)}
                      aria-label={`${c.categoryName}を削除`}
                    >
                      削除
                    </Button>
                  </HStack>
                </Td>
              </Tr>
            ))}
      </Tbody>
    </Table>
    {!loading && categories.length === 0 && (
      <Box p={8} textAlign="center">
        <Text color="gray.500">条件に一致するカテゴリがありません。</Text>
      </Box>
    )}
  </TableContainer>
);
