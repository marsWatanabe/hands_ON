'use client';

import React from 'react';
import { Button, Th } from '@chakra-ui/react';
import { FiArrowDown, FiArrowUp } from 'react-icons/fi';
import type { SortOrder } from '../types/api';

interface SortableThProps<K extends string> {
  /** 列の見出し。 */
  label: string;
  /** 並び替えの項目。指定しなければ並び替えできない列になる。 */
  sortKey?: K;
  /** 現在の並び替え項目。 */
  activeKey: string;
  order: SortOrder;
  onSort: (key: K) => void;
  isNumeric?: boolean;
}

/** 表の見出しのセル。並び替えできる列は、クリックで並び替え（`aria-sort` 付き）。 */
export function SortableTh<K extends string>({
  label,
  sortKey,
  activeKey,
  order,
  onSort,
  isNumeric,
}: SortableThProps<K>) {
  const active = sortKey !== undefined && sortKey === activeKey;
  return (
    <Th
      scope="col"
      isNumeric={isNumeric}
      aria-sort={sortKey ? (active ? (order === 'asc' ? 'ascending' : 'descending') : 'none') : undefined}
    >
      {sortKey ? (
        <Button
          variant="unstyled"
          display="inline-flex"
          alignItems="center"
          height="auto"
          fontSize="inherit"
          fontWeight="inherit"
          textTransform="inherit"
          letterSpacing="inherit"
          onClick={() => onSort(sortKey)}
          aria-label={`${label}で並び替え`}
        >
          {label}
          {active && (order === 'asc' ? <FiArrowUp aria-hidden /> : <FiArrowDown aria-hidden />)}
        </Button>
      ) : (
        label
      )}
    </Th>
  );
}
