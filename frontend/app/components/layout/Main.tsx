'use client';

import React from 'react';
import { Box, useColorModeValue } from '@chakra-ui/react';

interface MainProps {
  children: React.ReactNode;
}

export const Main: React.FC<MainProps> = ({ children }) => {
  return (
    <Box
      as="main"
      ml={{ base: 0, md: 60 }}
      px={{ base: 4, md: 8 }}
      py={6}
      role="main"
      bg={useColorModeValue('gray.100', 'gray.900')}
      minH="calc(100vh - 5rem)"
    >
      {children}
    </Box>
  );
};

