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

/** ログイン画面。ログイン後は在庫センター管理へ移動する。 */
export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { login } = useAuth();
  const router = useRouter();

  const cardBg = useColorModeValue('white', 'gray.800');
  const shadowColor = useColorModeValue('lg', 'dark-lg');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);

    try {
      await login(email.trim(), password);
      router.push('/centers');
    } catch (err) {
      // 認証エラー（401）はメッセージを固定し、それ以外（接続できない等）はサーバーのメッセージを出す
      if (err instanceof ApiError && err.status !== 401 && err.status !== 400) {
        setError(err.message);
      } else {
        setError('ログインに失敗しました。メールアドレスとパスワードを確認してください。');
      }
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
          ログイン
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
                    placeholder="パスワードを入力"
                    size="lg"
                  />
                </FormControl>

                <Button
                  type="submit"
                  size="lg"
                  w="full"
                  isLoading={submitting}
                  loadingText="ログイン中..."
                  colorScheme="primary"
                >
                  ログイン
                </Button>
              </VStack>
            </form>
          </CardBody>
        </Card>

        <Text textAlign="center" color="gray.600">
          アカウントをお持ちでない方は{' '}
          <Link 
            as={NextLink} 
            href="/register" 
            color="primary.500" 
            fontWeight="semibold"
            _hover={{ textDecoration: 'underline' }}
          >
            新規登録
          </Link>
        </Text>
      </VStack>
    </Container>
  );
}