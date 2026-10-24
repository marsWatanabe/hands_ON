'use client';

import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import type { AuthResponse, User } from '../types/api';
import { AUTH_PATHS, SESSION_EXPIRED_EVENT, apiFetch } from '../lib/api-client';
import {
  clearSession,
  getAccessToken,
  getRefreshToken,
  getStoredUser,
  saveTokens,
  saveUser,
} from '../lib/auth-storage';

/** 認証状態と操作。 */
interface AuthContextType {
  /** ログイン中のユーザー。未ログインは null。 */
  user: User | null;
  /** 起動時に保存済みの認証情報を確認している間は true。 */
  loading: boolean;
  /** ログインする。失敗時は ApiError を投げる。 */
  login: (email: string, password: string) => Promise<void>;
  /** アカウントを登録し、そのままログインする。失敗時は ApiError を投げる。 */
  register: (firstname: string, lastname: string, email: string, password: string) => Promise<void>;
  /** ログアウトし、ログイン画面へ移動する。 */
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

/**
 * 認証状態を提供する。トークンとユーザー情報は Cookie に保存する（auth-storage.ts）。
 *
 * @param props.children 子要素
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  // 起動時: 保存済みのトークンとユーザー情報があればログイン状態にする
  useEffect(() => {
    const stored = getStoredUser();
    if (stored && (getAccessToken() || getRefreshToken())) {
      setUser(stored);
    } else {
      clearSession();
    }
    setLoading(false);
  }, []);

  // API クライアントがトークン更新に失敗したとき（セッション失効）は未ログインにする
  useEffect(() => {
    const onExpired = () => setUser(null);
    window.addEventListener(SESSION_EXPIRED_EVENT, onExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired);
  }, []);

  /** 認証 API のレスポンスを保存し、ログイン状態にする。 */
  const startSession = useCallback((res: AuthResponse) => {
    if (!res.user) {
      throw new Error('認証レスポンスにユーザー情報がありません');
    }
    saveTokens(res.accessToken, res.refreshToken);
    saveUser(res.user);
    setUser(res.user);
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await apiFetch<AuthResponse>(AUTH_PATHS.authenticate, {
        method: 'POST',
        body: { email, password },
        auth: false,
      });
      startSession(res);
    },
    [startSession],
  );

  const register = useCallback(
    async (firstname: string, lastname: string, email: string, password: string) => {
      const res = await apiFetch<AuthResponse>(AUTH_PATHS.register, {
        method: 'POST',
        body: { firstname, lastname, email, password },
        auth: false,
      });
      startSession(res);
    },
    [startSession],
  );

  const logout = useCallback(() => {
    clearSession();
    setUser(null);
    router.push('/login');
  }, [router]);

  const value = useMemo(
    () => ({ user, loading, login, register, logout }),
    [user, loading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * 認証状態を取得するフック。AuthProvider の内側でのみ使える。
 *
 * @returns 認証状態と操作
 */
export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth は AuthProvider の内側で使用してください');
  }
  return context;
}
