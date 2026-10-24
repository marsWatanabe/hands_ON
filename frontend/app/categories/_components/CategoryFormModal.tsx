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
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  Select,
  Textarea,
  VStack,
} from '@chakra-ui/react';
import type { Category } from '../../types/api';
import { ApiError } from '../../lib/api-client';
import { createCategory, updateCategory } from '../../lib/categories-api';
import {
  CATEGORY_TYPE_LABELS,
  EMPTY_CATEGORY_FORM,
  categoryToFormValues,
  guessCategoryErrorField,
  validateCategoryForm,
  type CategoryFormErrors,
  type CategoryFormField,
  type CategoryFormValues,
} from '../../lib/category-form';

interface CategoryFormModalProps {
  isOpen: boolean;
  /** 編集するカテゴリ。null のときは新規登録。 */
  category: Category | null;
  onClose: () => void;
  /** 保存に成功したとき。 */
  onSaved: (saved: Category, mode: 'created' | 'updated') => void;
}

/** カテゴリの新規登録・編集のモーダル。 */
export const CategoryFormModal: React.FC<CategoryFormModalProps> = ({ isOpen, category, onClose, onSaved }) => {
  const [values, setValues] = useState<CategoryFormValues>(EMPTY_CATEGORY_FORM);
  const [errors, setErrors] = useState<CategoryFormErrors>({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const isEdit = category !== null;

  // 開くたびに入力値とエラーを初期化する
  useEffect(() => {
    if (isOpen) {
      setValues(category ? categoryToFormValues(category) : EMPTY_CATEGORY_FORM);
      setErrors({});
      setFormError('');
      setSubmitting(false);
    }
  }, [isOpen, category]);

  const setField = (field: CategoryFormField) => (
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
      // 同じ種別での名称の重複
      setErrors({ categoryName: err.message });
      return;
    }
    const field = err.status === 400 ? guessCategoryErrorField(err.message) : undefined;
    if (field) {
      setErrors({ [field]: err.message });
    } else {
      setFormError(err.message);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError('');
    const result = validateCategoryForm(values);
    if (!result.ok) {
      setErrors(result.errors);
      return;
    }
    setErrors({});
    setSubmitting(true);
    try {
      const saved = category
        ? await updateCategory(category.categoryId, result.request)
        : await createCategory(result.request);
      onSaved(saved, category ? 'updated' : 'created');
    } catch (err) {
      showServerError(err);
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="lg" scrollBehavior="inside" closeOnOverlayClick={!submitting}>
      <ModalOverlay />
      <ModalContent as="form" onSubmit={handleSubmit} noValidate>
        <ModalHeader>{isEdit ? 'カテゴリを編集' : 'カテゴリを新規登録'}</ModalHeader>
        <ModalCloseButton isDisabled={submitting} />
        <ModalBody>
          <VStack spacing={4} align="stretch">
            {formError && (
              <Alert status="error" borderRadius="md" role="alert">
                <AlertIcon />
                {formError}
              </Alert>
            )}

            <FormControl isRequired isInvalid={!!errors.categoryType}>
              <FormLabel>カテゴリ種別</FormLabel>
              <Select value={values.categoryType} onChange={setField('categoryType')} placeholder="選択してください">
                <option value="part">{CATEGORY_TYPE_LABELS.part}</option>
                <option value="product">{CATEGORY_TYPE_LABELS.product}</option>
              </Select>
              <FormErrorMessage>{errors.categoryType}</FormErrorMessage>
            </FormControl>

            <FormControl isRequired isInvalid={!!errors.categoryName}>
              <FormLabel>カテゴリ名</FormLabel>
              <Input value={values.categoryName} onChange={setField('categoryName')} />
              <FormErrorMessage>{errors.categoryName}</FormErrorMessage>
            </FormControl>

            <FormControl isInvalid={!!errors.description}>
              <FormLabel>説明</FormLabel>
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
