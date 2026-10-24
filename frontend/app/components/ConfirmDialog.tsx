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
} from '@chakra-ui/react';

interface ConfirmDialogProps {
  /** 開いているか。 */
  isOpen: boolean;
  title: string;
  /** 本文（確認するメッセージ）。 */
  children: React.ReactNode;
  /** 実行ボタンの文言。 */
  confirmLabel?: string;
  /** キャンセルされたとき。 */
  onCancel: () => void;
  /** 実行ボタンが押されたとき。完了（成功・失敗とも）まで、ボタンは処理中表示になる。 */
  onConfirm: () => Promise<void>;
}

/** 削除などの前に出す確認ダイアログ（危険な操作用の赤いボタン）。 */
export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  title,
  children,
  confirmLabel = '削除',
  onCancel,
  onConfirm,
}) => {
  const cancelRef = useRef<HTMLButtonElement>(null);
  const [busy, setBusy] = useState(false);

  const handleConfirm = async () => {
    setBusy(true);
    try {
      await onConfirm();
    } finally {
      setBusy(false);
    }
  };

  return (
    <AlertDialog isOpen={isOpen} leastDestructiveRef={cancelRef} onClose={onCancel} isCentered>
      <AlertDialogOverlay>
        <AlertDialogContent>
          <AlertDialogHeader fontSize="lg" fontWeight="bold">
            {title}
          </AlertDialogHeader>
          <AlertDialogBody>{children}</AlertDialogBody>
          <AlertDialogFooter gap={3}>
            <Button ref={cancelRef} onClick={onCancel} isDisabled={busy}>
              キャンセル
            </Button>
            <Button colorScheme="red" onClick={handleConfirm} isLoading={busy} loadingText="処理中...">
              {confirmLabel}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialogOverlay>
    </AlertDialog>
  );
};
