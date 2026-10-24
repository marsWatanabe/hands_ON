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
import type { Center } from '../../types/api';
import { ApiError } from '../../lib/api-client';
import { PREFECTURES } from '../../lib/address';
import { createCenter, updateCenter } from '../../lib/centers-api';
import {
  EMPTY_CENTER_FORM,
  OPERATIONAL_STATUS_LABELS,
  centerToFormValues,
  guessErrorField,
  validateCenterForm,
  type CenterFormErrors,
  type CenterFormField,
  type CenterFormValues,
} from '../../lib/center-form';

interface CenterFormModalProps {
  isOpen: boolean;
  /** 編集する在庫センター。null のときは新規登録。 */
  center: Center | null;
  onClose: () => void;
  /** 保存に成功したとき。 */
  onSaved: (saved: Center, mode: 'created' | 'updated') => void;
}

/** 在庫センターの新規登録・編集のモーダル。 */
export const CenterFormModal: React.FC<CenterFormModalProps> = ({ isOpen, center, onClose, onSaved }) => {
  const [values, setValues] = useState<CenterFormValues>(EMPTY_CENTER_FORM);
  const [errors, setErrors] = useState<CenterFormErrors>({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const isEdit = center !== null;

  // 開くたびに入力値とエラーを初期化する
  useEffect(() => {
    if (isOpen) {
      setValues(center ? centerToFormValues(center) : EMPTY_CENTER_FORM);
      setErrors({});
      setFormError('');
      setSubmitting(false);
    }
  }, [isOpen, center]);

  const setField = (field: CenterFormField) => (
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
      // センター名の重複
      setErrors({ centerName: err.message });
      return;
    }
    const field = err.status === 400 ? guessErrorField(err.message) : undefined;
    if (field) {
      setErrors({ [field]: err.message });
    } else {
      setFormError(err.message);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError('');
    const result = validateCenterForm(values);
    if (!result.ok) {
      setErrors(result.errors);
      return;
    }
    setErrors({});
    setSubmitting(true);
    try {
      const saved = center
        ? await updateCenter(center.centerId, result.request)
        : await createCenter(result.request);
      onSaved(saved, center ? 'updated' : 'created');
    } catch (err) {
      showServerError(err);
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="2xl" scrollBehavior="inside" closeOnOverlayClick={!submitting}>
      <ModalOverlay />
      <ModalContent as="form" onSubmit={handleSubmit} noValidate>
        <ModalHeader>{isEdit ? '在庫センターを編集' : '在庫センターを新規登録'}</ModalHeader>
        <ModalCloseButton isDisabled={submitting} />
        <ModalBody>
          <VStack spacing={4} align="stretch">
            {formError && (
              <Alert status="error" borderRadius="md" role="alert">
                <AlertIcon />
                {formError}
              </Alert>
            )}

            <FormControl isRequired isInvalid={!!errors.centerName}>
              <FormLabel>在庫センター名</FormLabel>
              <Input value={values.centerName} onChange={setField('centerName')} />
              <FormErrorMessage>{errors.centerName}</FormErrorMessage>
            </FormControl>

            <SimpleGrid columns={{ base: 1, md: 2 }} spacing={4}>
              <FormControl isRequired isInvalid={!!errors.postCode}>
                <FormLabel>郵便番号</FormLabel>
                <Input value={values.postCode} onChange={setField('postCode')} placeholder="123-4567" inputMode="numeric" />
                <FormErrorMessage>{errors.postCode}</FormErrorMessage>
              </FormControl>
              <FormControl isRequired isInvalid={!!errors.prefecture}>
                <FormLabel>都道府県</FormLabel>
                <Select value={values.prefecture} onChange={setField('prefecture')} placeholder="選択してください">
                  {PREFECTURES.map((p) => (
                    <option key={p} value={p}>
                      {p}
                    </option>
                  ))}
                </Select>
                <FormErrorMessage>{errors.prefecture}</FormErrorMessage>
              </FormControl>
            </SimpleGrid>

            <SimpleGrid columns={{ base: 1, md: 2 }} spacing={4}>
              <FormControl isInvalid={!!errors.city}>
                <FormLabel>市区町村</FormLabel>
                <Input value={values.city} onChange={setField('city')} placeholder="大阪市北区" />
                <FormErrorMessage>{errors.city}</FormErrorMessage>
              </FormControl>
              <FormControl isInvalid={!!errors.street}>
                <FormLabel>番地・建物名</FormLabel>
                <Input value={values.street} onChange={setField('street')} placeholder="梅田1-1-1" />
                <FormErrorMessage>{errors.street}</FormErrorMessage>
              </FormControl>
            </SimpleGrid>

            <SimpleGrid columns={{ base: 1, md: 2 }} spacing={4}>
              <FormControl isRequired isInvalid={!!errors.phoneNumber}>
                <FormLabel>電話番号</FormLabel>
                <Input value={values.phoneNumber} onChange={setField('phoneNumber')} placeholder="06-1234-5678" inputMode="tel" />
                <FormErrorMessage>{errors.phoneNumber}</FormErrorMessage>
              </FormControl>
              <FormControl isRequired isInvalid={!!errors.managerName}>
                <FormLabel>管理者名</FormLabel>
                <Input value={values.managerName} onChange={setField('managerName')} />
                <FormErrorMessage>{errors.managerName}</FormErrorMessage>
              </FormControl>
            </SimpleGrid>

            <FormControl isRequired isInvalid={!!errors.operationalStatus}>
              <FormLabel>稼働状況</FormLabel>
              <Select value={values.operationalStatus} onChange={setField('operationalStatus')}>
                <option value="0">{OPERATIONAL_STATUS_LABELS[0]}</option>
                <option value="1">{OPERATIONAL_STATUS_LABELS[1]}</option>
              </Select>
              <FormErrorMessage>{errors.operationalStatus}</FormErrorMessage>
            </FormControl>

            <SimpleGrid columns={{ base: 1, md: 2 }} spacing={4}>
              <FormControl isInvalid={!!errors.maxStorageCapacity}>
                <FormLabel>最大保管容量</FormLabel>
                <InputGroup>
                  <Input value={values.maxStorageCapacity} onChange={setField('maxStorageCapacity')} inputMode="numeric" />
                  <InputRightAddon>㎥</InputRightAddon>
                </InputGroup>
                <FormErrorMessage>{errors.maxStorageCapacity}</FormErrorMessage>
              </FormControl>
              <FormControl isInvalid={!!errors.currentStorageCapacity}>
                <FormLabel>現在保管容量</FormLabel>
                <InputGroup>
                  <Input value={values.currentStorageCapacity} onChange={setField('currentStorageCapacity')} inputMode="numeric" />
                  <InputRightAddon>㎥</InputRightAddon>
                </InputGroup>
                <FormErrorMessage>{errors.currentStorageCapacity}</FormErrorMessage>
              </FormControl>
            </SimpleGrid>

            <FormControl isInvalid={!!errors.notes}>
              <FormLabel>備考</FormLabel>
              <Textarea value={values.notes} onChange={setField('notes')} rows={3} />
              <FormErrorMessage>{errors.notes}</FormErrorMessage>
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
