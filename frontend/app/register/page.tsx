'use client';

import {
  Container,
  VStack,
  Heading,
  FormControl,
  FormLabel,
  Input,
  Button,
  Text,
  Link,
  Alert,
  AlertIcon,
  Card,
  CardBody,
  useColorModeValue,
} from '@chakra-ui/react';
import { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { ApiError } from '../lib/api-client';
import { useRouter } from 'next/navigation';
import NextLink from 'next/link';

/** アカウント登録画面。登録後はそのままログインし、在庫センター管理へ移動する。 */
export default function RegisterPage() {
  const [lastname, setLastname] = useState('');
  const [firstname, setFirstname] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { register } = useAuth();
  const router = useRouter();

  const cardBg = useColorModeValue('white', 'gray.800');
  const shadowColor = useColorModeValue('lg', 'dark-lg');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!lastname.trim() || !firstname.trim()) {
      setError('氏名と名前を入力してください。');
      return;
    }

    if (password !== confirmPassword) {
      setError('パスワードが一致しません。');
      return;
    }

    if (password.length < 8) {
      setError('パスワードは8文字以上で入力してください。');
      return;
    }

    setSubmitting(true);
    try {
      await register(firstname.trim(), lastname.trim(), email.trim(), password);
      router.push('/centers');
    } catch (err) {
      // 重複（409）や入力エラー（400）はサーバーのメッセージをそのまま表示する
      setError(err instanceof ApiError ? err.message : '登録に失敗しました。入力内容を確認してください。');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Container maxW="md" py={20}>
      <VStack spacing={8}>
        <Heading
          fontSize="3xl"
          fontWeight="bold"
          color="primary.600"
          textAlign="center"
        >
          アカウント登録
        </Heading>

        <Card
          w="full"
          bg={cardBg}
          shadow={shadowColor}
          borderRadius="xl"
          overflow="hidden"
        >
          <CardBody p={8}>
            <form onSubmit={handleSubmit}>
              <VStack spacing={6}>
                {error && (
                  <Alert status="error" borderRadius="lg">
                    <AlertIcon />
                    {error}
                  </Alert>
                )}

                <FormControl isRequired>
                  <FormLabel color="gray.600">氏名</FormLabel>
                  <Input
                    type="text"
                    value={lastname}
                    onChange={(e) => setLastname(e.target.value)}
                    placeholder="田中"
                    size="lg"
                  />
                </FormControl>

                <FormControl isRequired>
                  <FormLabel color="gray.600">名前</FormLabel>
                  <Input
                    type="text"
                    value={firstname}
                    onChange={(e) => setFirstname(e.target.value)}
                    placeholder="太郎"
                    size="lg"
                  />
                </FormControl>

                <FormControl isRequired>
                  <FormLabel color="gray.600">メールアドレス</FormLabel>
                  <Input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="your@email.com"
                    size="lg"
                  />
                </FormControl>

                <FormControl isRequired>
                  <FormLabel color="gray.600">パスワード</FormLabel>
                  <Input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="8文字以上で入力"
                    size="lg"
                  />
                </FormControl>

                <FormControl isRequired>
                  <FormLabel color="gray.600">パスワード確認</FormLabel>
                  <Input
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="パスワードを再入力"
                    size="lg"
                  />
                </FormControl>

                <Button
                  type="submit"
                  size="lg"
                  w="full"
                  isLoading={submitting}
                  loadingText="登録中..."
                  colorScheme="primary"
                >
                  アカウントを作成
                </Button>
              </VStack>
            </form>
          </CardBody>
        </Card>

        <Text textAlign="center" color="gray.600">
          すでにアカウントをお持ちの方は{' '}
          <Link
            as={NextLink}
            href="/login"
            color="primary.500"
            fontWeight="semibold"
            _hover={{ textDecoration: 'underline' }}
          >
            ログイン
          </Link>
        </Text>
      </VStack>
    </Container>
  );
}
