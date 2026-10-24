'use client';

import React, { useState } from 'react';
import { Button, FormControl, FormLabel, HStack, Input, Select, Stack } from '@chakra-ui/react';
import type { CenterFilters } from '../../hooks/useCenters';
import { CENTER_LIMITS, OPERATIONAL_STATUS_LABELS } from '../../lib/center-form';

/** 検索条件の入力欄の上限（バックエンドの CenterSearchRequest に合わせる）。 */
const SEARCH_ADDRESS_MAX = 100;

interface CenterSearchBarProps {
  /** 現在の絞り込み条件（クリア後の初期値として使う）。 */
  initial: CenterFilters;
  /** 検索ボタンが押されたとき。 */
  onSearch: (filters: CenterFilters) => void;
}

/** 在庫センター一覧の検索バー（センター名、住所、稼働状況）。 */
export const CenterSearchBar: React.FC<CenterSearchBarProps> = ({ initial, onSearch }) => {
  const [centerName, setCenterName] = useState(initial.centerName);
  const [address, setAddress] = useState(initial.address);
  const [operationalStatus, setOperationalStatus] = useState<CenterFilters['operationalStatus']>(
    initial.operationalStatus,
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch({ centerName: centerName.trim(), address: address.trim(), operationalStatus });
  };

  const handleClear = () => {
    setCenterName('');
    setAddress('');
    setOperationalStatus('');
    onSearch({ centerName: '', address: '', operationalStatus: '' });
  };

  return (
    <form onSubmit={handleSubmit} role="search" aria-label="在庫センター検索">
      <Stack direction={{ base: 'column', md: 'row' }} spacing={4} align={{ md: 'flex-end' }}>
        <FormControl>
          <FormLabel>センター名</FormLabel>
          <Input
            value={centerName}
            onChange={(e) => setCenterName(e.target.value)}
            maxLength={CENTER_LIMITS.centerName}
            placeholder="例: 大阪"
            bg="white"
            _dark={{ bg: 'gray.800' }}
          />
        </FormControl>
        <FormControl>
          <FormLabel>住所</FormLabel>
          <Input
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            maxLength={SEARCH_ADDRESS_MAX}
            placeholder="例: 東京都"
            bg="white"
            _dark={{ bg: 'gray.800' }}
          />
        </FormControl>
        <FormControl maxW={{ md: '200px' }}>
          <FormLabel>稼働状況</FormLabel>
          <Select
            value={operationalStatus}
            onChange={(e) => setOperationalStatus(e.target.value as CenterFilters['operationalStatus'])}
            bg="white"
            _dark={{ bg: 'gray.800' }}
          >
            <option value="">すべて</option>
            <option value="0">{OPERATIONAL_STATUS_LABELS[0]}</option>
            <option value="1">{OPERATIONAL_STATUS_LABELS[1]}</option>
          </Select>
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
