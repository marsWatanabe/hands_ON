'use client';

import React from 'react';
import { Box, Drawer, DrawerContent, useDisclosure } from '@chakra-ui/react';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { Main } from './Main';

interface SidebarLayoutProps {
  /** ヘッダー（モバイル表示）に出すタイトル。 */
  title?: string;
  children: React.ReactNode;
}

/**
 * 認証後の画面の共通レイアウト（サイドバー、ヘッダー、本文）。
 * 狭い画面ではサイドバーをドロワーで表示する。
 */
export const SidebarLayout: React.FC<SidebarLayoutProps> = ({ title, children }) => {
  const { isOpen, onOpen, onClose } = useDisclosure();
  return (
    <Box minH="100vh">
      <Sidebar onClose={onClose} display={{ base: 'none', md: 'block' }} />
      <Drawer
        isOpen={isOpen}
        placement="left"
        onClose={onClose}
        returnFocusOnClose={false}
        onOverlayClick={onClose}
        size="full"
      >
        <DrawerContent>
          <Sidebar onClose={onClose} />
        </DrawerContent>
      </Drawer>
      <Header onOpen={onOpen} title={title} />
      <Main>{children}</Main>
    </Box>
  );
};
