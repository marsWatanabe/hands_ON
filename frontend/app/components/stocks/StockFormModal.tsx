'use client';

import React, { useEffect, useState } from 'react';
import {
  Alert,
  AlertIcon,
  Button,
  FormControl,
  FormErrorMessage,
  FormLabel,
  Input,
  InputGroup,
  InputRightAddon,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  Select,
  SimpleGrid,
  Textarea,
  VStack,
} from '@chakra-ui/react';
import type { Category, Center, Stock, StockKind } from '../../types/api';
import { ApiError } from '../../lib/api-client';
import { createStock, updateStock } from '../../lib/stocks-api';
import {
  EMPTY_STOCK_FORM,
  STOCK_KIND_LABELS,
  conflictFieldOf,
  guessStockErrorField,
  stockToFormValues,
  validateStockForm,
  type StockFormErrors,
  type StockFormField,
  type StockFormValues,
} from '../../lib/stock-form';

interface StockFormModalProps {
  kind: StockKind;
  isOpen: boolean;
  /** 編集する在庫。null のときは新規登録。 */
  stock: Stock | null;
  /** カテゴリの選択肢（種類に合うカテゴリ）。 */
  categories: Category[];
  /** センターの選択肢。 */
  centers: Center[];
  onClose: () => void;
  /** 保存に成功したとき。 */
  onSaved: (saved: Stock, mode: 'created' | 'updated') => void;
}

/** 在庫（部品在庫、製品在庫）の新規登録・編集のモーダル。製品在庫は製品コードも入力する。 */
export const StockFormModal: React.FC<StockFormModalProps> = ({
  kind,
  isOpen,
  stock,
  categories,
  centers,
  onClose,
  onSaved,
}) => {
  const [values, setValues] = useState<StockFormValues>(EMPTY_STOCK_FORM);
  const [errors, setErrors] = useState<StockFormErrors>({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const isEdit = stock !== null;
  const itemLabel = STOCK_KIND_LABELS[kind].item;

  // 開くたびに入力値とエラーを初期化する
  useEffect(() => {
    if (isOpen) {
      setValues(stock ? stockToFormValues(stock) : EMPTY_STOCK_FORM);
      setErrors({});
      setFormError('');
      setSubmitting(false);
    }
  }, [isOpen, stock]);

  const setField = (field: StockFormField) => (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>,
  ) => {
    setValues((v) => ({ ...v, [field]: e.target.value }));
    // 入力し直した項目のエラーは消す
    setErrors((prev) => (prev[field] ? { ...prev, [field]: undefined } : prev));
  };

  /** サーバーのエラーを、対応する項目またはフォーム全体のエラーとして表示する。 */
  const showServerError = (err: unknown) => {
    if (!(err instanceof ApiError)) {
      setFormError('保存に失敗しました。もう一度お試しください。');
      return;
    }
    if (err.status === 409) {
      // 製品コードの重複、または同じセンターでの名称の重複
      setErrors({ [conflictFieldOf(err.message)]: err.message });
      return;
    }
    const field = err.status === 400 ? guessStockErrorField(err.message) : undefined;
    if (field) {
      setErrors({ [field]: err.message });
    } else {
      setFormError(err.message);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError('');
    const result = validateStockForm(kind, values);
    if (!result.ok) {
      setErrors(result.errors);
      return;
    }
    setErrors({});
    setSubmitting(true);
    try {
      const saved = stock
        ? await updateStock(kind, stock.stockId, result.request)
        : await createStock(kind, result.request);
      onSaved(saved, stock ? 'updated' : 'created');
    } catch (err) {
      showServerError(err);
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="xl" scrollBehavior="inside" closeOnOverlayClick={!submitting}>
      <ModalOverlay />
      <ModalContent as="form" onSubmit={handleSubmit} noValidate>
        <ModalHeader>{isEdit ? `${itemLabel}を編集` : `${itemLabel}を新規登録`}</ModalHeader>
        <ModalCloseButton isDisabled={submitting} />
        <ModalBody>
          <VStack spacing={4} align="stretch">
            {formError && (
              <Alert status="error" borderRadius="md" role="alert">
                <AlertIcon />
                {formError}
              </Alert>
            )}

            {kind === 'products' && (
              <FormControl isRequired isInvalid={!!errors.productCode}>
                <FormLabel>製品コード</FormLabel>
                <Input value={values.productCode} onChange={setField('productCode')} placeholder="PRD-000001" />
                <FormErrorMessage>{errors.productCode}</FormErrorMessage>
              </FormControl>
            )}

            <FormControl isRequired isInvalid={!!errors.name}>
              <FormLabel>名称</FormLabel>
              <Input value={values.name} onChange={setField('name')} />
              <FormErrorMessage>{errors.name}</FormErrorMessage>
            </FormControl>

            <SimpleGrid columns={{ base: 1, md: 2 }} spacing={4}>
              <FormControl isRequired isInvalid={!!errors.categoryId}>
                <FormLabel>カテゴリ</FormLabel>
                <Select value={values.categoryId} onChange={setField('categoryId')} placeholder="選択してください">
                  {categories.map((c) => (
                    <option key={c.categoryId} value={c.categoryId}>
                      {c.categoryName}
                    </option>
                  ))}
                </Select>
                <FormErrorMessage>{errors.categoryId}</FormErrorMessage>
              </FormControl>
              <FormControl isRequired isInvalid={!!errors.centerId}>
                <FormLabel>在庫センター</FormLabel>
                <Select value={values.centerId} onChange={setField('centerId')} placeholder="選択してください">
                  {centers.map((c) => (
                    <option key={c.centerId} value={c.centerId}>
                      {c.centerName}
                    </option>
                  ))}
                </Select>
                <FormErrorMessage>{errors.centerId}</FormErrorMessage>
              </FormControl>
            </SimpleGrid>

            <FormControl isRequired isInvalid={!!errors.amount}>
              <FormLabel>在庫数</FormLabel>
              <InputGroup>
                <Input value={values.amount} onChange={setField('amount')} inputMode="numeric" />
                <InputRightAddon>個</InputRightAddon>
              </InputGroup>
              <FormErrorMessage>{errors.amount}</FormErrorMessage>
            </FormControl>

            <FormControl isInvalid={!!errors.description}>
              <FormLabel>在庫説明</FormLabel>
              <Textarea value={values.description} onChange={setField('description')} rows={3} />
              <FormErrorMessage>{errors.description}</FormErrorMessage>
            </FormControl>
          </VStack>
        </ModalBody>
        <ModalFooter gap={3}>
          <Button variant="ghost" onClick={onClose} isDisabled={submitting}>
            キャンセル
          </Button>
          <Button type="submit" colorScheme="cyan" color="white" isLoading={submitting} loadingText="保存中...">
            {isEdit ? '更新' : '登録'}
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
};
