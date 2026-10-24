'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Center, Spinner } from '@chakra-ui/react';
import { useAuth } from './contexts/AuthContext';

/** トップページ。ログイン状態に応じて在庫センター管理かログイン画面へ移動する。 */
export default function HomePage() {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    router.replace(user ? '/centers' : '/login');
  }, [user, loading, router]);

  return (
    <Center minH="100vh">
      <Spinner size="xl" />
    </Center>
  );
}
