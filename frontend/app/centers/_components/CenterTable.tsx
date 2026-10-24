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
  Th,
  Thead,
  Tr,
} from '@chakra-ui/react';
import { FiArrowDown, FiArrowUp, FiEdit2, FiTrash2 } from 'react-icons/fi';
import type { Center, CenterSortKey, SortOrder } from '../../types/api';
import { summarizeAddress } from '../../lib/address';
import { OPERATIONAL_STATUS_LABELS } from '../../lib/center-form';

interface CenterTableProps {
  centers: Center[];
  loading: boolean;
  sort: CenterSortKey;
  order: SortOrder;
  onSort: (key: CenterSortKey) => void;
  onEdit: (center: Center) => void;
  onDelete: (center: Center) => void;
}

/** 列の定義。sortKey があれば見出しクリックで並び替えできる。 */
const COLUMNS: Array<{ label: string; sortKey?: CenterSortKey; isNumeric?: boolean }> = [
  { label: 'センター名', sortKey: 'centerName' },
  { label: '住所（都道府県・市区町村）', sortKey: 'address' },
  { label: '電話番号' },
  { label: '管理者名' },
  { label: '稼働状況', sortKey: 'operationalStatus' },
  { label: '最大保管容量（㎥）', sortKey: 'maxStorageCapacity', isNumeric: true },
  { label: '現在保管容量（㎥）', sortKey: 'currentStorageCapacity', isNumeric: true },
  { label: '操作' },
];

/** ローディング中に表示する行数。 */
const SKELETON_ROWS = 5;

/**
 * 容量を表示用に整形する。
 *
 * @param value 容量（未設定は null）
 * @returns 3 桁区切りの文字列。未設定は「-」
 */
function formatCapacity(value: number | null): string {
  return value === null ? '-' : value.toLocaleString('ja-JP');
}

/** 在庫センター一覧のテーブル。 */
export const CenterTable: React.FC<CenterTableProps> = ({
  centers,
  loading,
  sort,
  order,
  onSort,
  onEdit,
  onDelete,
}) => {
  return (
    <TableContainer bg="white" _dark={{ bg: 'gray.800' }} borderRadius="lg" shadow="sm">
      <Table variant="simple" size="md" aria-busy={loading}>
        <Thead>
          <Tr>
            {COLUMNS.map((col) => {
              const active = col.sortKey === sort;
              return (
                <Th
                  key={col.label}
                  scope="col"
                  isNumeric={col.isNumeric}
                  aria-sort={
                    col.sortKey ? (active ? (order === 'asc' ? 'ascending' : 'descending') : 'none') : undefined
                  }
                >
                  {col.sortKey ? (
                    <Button
                      variant="unstyled"
                      display="inline-flex"
                      alignItems="center"
                      height="auto"
                      fontSize="inherit"
                      fontWeight="inherit"
                      textTransform="inherit"
                      letterSpacing="inherit"
                      onClick={() => onSort(col.sortKey as CenterSortKey)}
                      aria-label={`${col.label}で並び替え`}
                    >
                      {col.label}
                      {active && (order === 'asc' ? <FiArrowUp aria-hidden /> : <FiArrowDown aria-hidden />)}
                    </Button>
                  ) : (
                    col.label
                  )}
                </Th>
              );
            })}
          </Tr>
        </Thead>
        <Tbody>
          {loading && centers.length === 0
            ? Array.from({ length: SKELETON_ROWS }, (_, i) => (
                <Tr key={`skeleton-${i}`}>
                  {COLUMNS.map((col) => (
                    <Td key={col.label}>
                      <Skeleton height="20px" />
                    </Td>
                  ))}
                </Tr>
              ))
            : centers.map((c) => (
                <Tr key={c.centerId} opacity={loading ? 0.6 : 1}>
                  <Td fontWeight="semibold">{c.centerName}</Td>
                  <Td>{summarizeAddress(c.address)}</Td>
                  <Td>{c.phoneNumber}</Td>
                  <Td>{c.managerName}</Td>
                  <Td>
                    <Badge colorScheme={c.operationalStatus === 0 ? 'green' : 'gray'}>
                      {OPERATIONAL_STATUS_LABELS[c.operationalStatus]}
                    </Badge>
                  </Td>
                  <Td isNumeric>{formatCapacity(c.maxStorageCapacity)}</Td>
                  <Td isNumeric>{formatCapacity(c.currentStorageCapacity)}</Td>
                  <Td>
                    <HStack spacing={2}>
                      <Button
                        size="sm"
                        variant="outline"
                        leftIcon={<FiEdit2 />}
                        onClick={() => onEdit(c)}
                        aria-label={`${c.centerName}を編集`}
                      >
                        編集
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        colorScheme="red"
                        leftIcon={<FiTrash2 />}
                        onClick={() => onDelete(c)}
                        aria-label={`${c.centerName}を削除`}
                      >
                        削除
                      </Button>
                    </HStack>
                  </Td>
                </Tr>
              ))}
        </Tbody>
      </Table>
      {!loading && centers.length === 0 && (
        <Box p={8} textAlign="center">
          <Text color="gray.500">条件に一致する在庫センターがありません。</Text>
        </Box>
      )}
    </TableContainer>
  );
};
