'use client';

import React from 'react';
import {
  Flex,
  IconButton,
  Avatar,
  HStack,
  Text,
  VStack,
  Box,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  useColorModeValue,
  FlexProps,
  useColorMode, Badge,
} from '@chakra-ui/react';
import { FiMenu, FiChevronDown, FiLogOut } from 'react-icons/fi';
import { SunIcon, MoonIcon } from '@chakra-ui/icons';
import { useAuth } from '../../contexts/AuthContext';

interface MobileProps extends FlexProps {
  onOpen: () => void;
  title?: string;
}

export const Header: React.FC<MobileProps> = ({ onOpen, title, ...rest }) => {
  const { user, logout } = useAuth();
  const { colorMode, toggleColorMode } = useColorMode();
  return (
    <Flex
      ml={{ base: 0, md: 60 }}
      px={{ base: 4, md: 6 }}
      height="20"
      alignItems="center"
      bg={useColorModeValue('white', 'gray.900')}
      borderBottomWidth="1px"
      borderBottomColor={useColorModeValue('gray.200', 'gray.700')}
      justifyContent={{ base: 'space-between', md: 'flex-end' }}
      position="sticky"
      top={0}
      zIndex={100}
      {...rest}
    >
      <IconButton
        display={{ base: 'flex', md: 'none' }}
        onClick={onOpen}
        variant="outline"
        aria-label="open menu"
        icon={<FiMenu />}
      />
      <Text
        display={{ base: 'flex', md: 'none' }}
        fontSize="2xl"
        fontFamily="monospace"
        fontWeight="bold"
      >
        {title || 'Inventrack'}
      </Text>
      <HStack spacing={{ base: '0', md: '6' }}>
        <IconButton
          size="lg"
          variant="ghost"
          aria-label="toggle color mode"
          icon={colorMode === 'light' ? <MoonIcon /> : <SunIcon />}
          onClick={toggleColorMode}
        />
        <Flex alignItems={'center'}>
          <Menu>
            <MenuButton py={2} transition="all 0.3s" _focus={{ boxShadow: 'none' }}>
              <HStack>
                <Avatar size={'sm'} name={user ? `${user.lastname} ${user.firstname}` : ''} />
                <VStack
                  display={{ base: 'none', md: 'flex' }}
                  alignItems="flex-start"
                  spacing="1px"
                  ml="2"
                >
                  <Text fontSize="sm" noOfLines={1} maxW="120px">{user ? `${user.lastname} ${user.firstname}` : 'User'}</Text>
                  <Text fontSize="xs" color="gray.600" noOfLines={1} maxW="140px">
                    {user?.email}
                  </Text>
                </VStack>
                <Box display={{ base: 'none', md: 'flex' }}>
                  <FiChevronDown />
                </Box>
              </HStack>
            </MenuButton>
            <MenuList
              bg={useColorModeValue('white', 'gray.900')}
              borderColor={useColorModeValue('gray.200', 'gray.700')}
            >
              <MenuItem icon={<FiLogOut />} onClick={logout} color="red.500">
                ログアウト
              </MenuItem>
            </MenuList>
          </Menu>
        </Flex>
      </HStack>
    </Flex>
  );
};

