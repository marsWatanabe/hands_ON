"use client";

import React from "react";
import {
  Box,
  Flex,
  Text,
  CloseButton,
  Icon,
  BoxProps,
  FlexProps,
  useColorModeValue,
} from "@chakra-ui/react";
import { usePathname } from "next/navigation";
import Link from "next/link";
import { IconType } from "react-icons";
import { FiArchive, FiBox, FiHome, FiTag } from "react-icons/fi";

interface LinkItemProps {
  name: string;
  icon: IconType;
  href?: string;
}

// 実装済みの画面だけを載せる。画面を追加するときはここに追記する。
const LinkItems: Array<LinkItemProps> = [
  { name: "在庫センター", icon: FiHome, href: "/centers" },
  { name: "部品在庫一覧", icon: FiBox, href: "/parts-stocks" },
  { name: "製品在庫一覧", icon: FiArchive, href: "/products-stocks" },
  { name: "カテゴリ管理", icon: FiTag, href: "/categories" },
];
interface SidebarProps extends BoxProps {
  onClose: () => void;
}

interface NavItemProps extends FlexProps {
  icon: IconType;
  children: React.ReactNode;
  href?: string;
  active?: boolean;
}

const NavItem = ({
  icon,
  children,
  href = "#",
  active,
  ...rest
}: NavItemProps) => {
  const activeStyles = active ? { bg: "cyan.500", color: "white" } : {};
  return (
    <Box
      as={Link}
      href={href}
      style={{ textDecoration: "none" }}
      _focus={{ boxShadow: "none" }}
    >
      <Flex
        align="center"
        p="4"
        mx="4"
        borderRadius="lg"
        data-group
        cursor="pointer"
        _hover={{ bg: "cyan.400", color: "white" }}
        {...activeStyles}
        {...rest}
      >
        {icon && (
          <Icon
            mr="4"
            fontSize="16"
            _groupHover={{ color: "white" }}
            as={icon}
          />
        )}
        {children}
      </Flex>
    </Box>
  );
};

export const Sidebar: React.FC<SidebarProps> = ({ onClose, ...rest }) => {
  const pathname = usePathname();
  return (
    <Box
      transition="0.2s ease"
      bg={useColorModeValue("white", "gray.900")}
      borderRight="1px"
      borderRightColor={useColorModeValue("gray.200", "gray.700")}
      w={{ base: "full", md: 60 }}
      pos="fixed"
      h="full"
      {...rest}
    >
      <Flex h="20" alignItems="center" mx="8" justifyContent="space-between">
        <Text fontSize="2xl" fontFamily="monospace" fontWeight="bold">
          Inventrack
        </Text>
        <CloseButton display={{ base: "flex", md: "none" }} onClick={onClose} />
      </Flex>
      {LinkItems.map((link) => (
        <NavItem
          key={link.name}
          icon={link.icon}
          href={link.href}
          active={link.href === pathname}
        >
          {link.name}
        </NavItem>
      ))}
    </Box>
  );
};
