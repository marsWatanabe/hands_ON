'use client';

import React, { useRef, useState } from 'react';
import {
  AlertDialog,
  AlertDialogBody,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogOverlay,
  Button,
  Text,
} from '@chakra-ui/react';
import type { Center } from '../../types/api';
import { ApiError } from '../../lib/api-client';
import { deleteCenter } from '../../lib/centers-api';

interface DeleteCenterDialogProps {
  /** 削除の対象。null のときは閉じている。 */
  center: Center | null;
  onClose: () => void;
  /** 削除に成功したとき（すでに削除済みだったときも含む）。 */
  onDeleted: (center: Center) => void;
  /** 削除に失敗したとき。メッセージを通知する。 */
  onError: (message: string) => void;
}

/** 在庫センターを削除する前の確認ダイアログ。 */
export const DeleteCenterDialog: React.FC<DeleteCenterDialogProps> = ({ center, onClose, onDeleted, onError }) => {
  const cancelRef = useRef<HTMLButtonElement>(null);
  const [deleting, setDeleting] = useState(false);

  const handleDelete = async () => {
    if (!center) return;
    setDeleting(true);
    try {
      await deleteCenter(center.centerId);
      onDeleted(center);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        // 他の操作で先に削除されていた。一覧から消えればよいので成功として扱う
        onDeleted(center);
      } else {
        onError(err instanceof ApiError ? err.message : '削除に失敗しました。');
      }
    } finally {
      setDeleting(false);
    }
  };

  return (
    <AlertDialog isOpen={center !== null} leastDestructiveRef={cancelRef} onClose={onClose} isCentered>
      <AlertDialogOverlay>
        <AlertDialogContent>
          <AlertDialogHeader fontSize="lg" fontWeight="bold">
            在庫センターの削除
          </AlertDialogHeader>
          <AlertDialogBody>
            <Text>
              「{center?.centerName}」を削除します。よろしいですか？
            </Text>
          </AlertDialogBody>
          <AlertDialogFooter gap={3}>
            <Button ref={cancelRef} onClick={onClose} isDisabled={deleting}>
              キャンセル
            </Button>
            <Button colorScheme="red" onClick={handleDelete} isLoading={deleting} loadingText="削除中...">
              削除
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialogOverlay>
    </AlertDialog>
  );
};
