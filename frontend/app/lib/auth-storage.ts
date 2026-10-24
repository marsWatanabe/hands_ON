import Cookies from 'js-cookie';
import type { User } from '../types/api';

/** Cookie 名。 */
export const COOKIE_ACCESS_TOKEN = 'accessToken';
export const COOKIE_REFRESH_TOKEN = 'refreshToken';
export const COOKIE_USER = 'user';

/** Cookie の有効日数。サーバー側の有効期限（保留事項）とは独立した、ブラウザ側の保持期間。 */
const ACCESS_TOKEN_DAYS = 1;
const REFRESH_TOKEN_DAYS = 7;

/**
 * Cookie の共通オプション。
 * 現状はトークンを JavaScript から読める Cookie に保存する（NFR-SEC-03 の既知の制限）。
 * 他サイトからの送信を避けるため SameSite=Strict、HTTPS では Secure を付ける。
 */
function cookieOptions(days: number): Cookies.CookieAttributes {
  return {
    expires: days,
    sameSite: 'strict',
    secure: typeof window !== 'undefined' && window.location.protocol === 'https:',
  };
}

/** 保存済みのアクセストークンを返す。 */
export function getAccessToken(): string | undefined {
  return Cookies.get(COOKIE_ACCESS_TOKEN);
}

/** 保存済みのリフレッシュトークンを返す。 */
export function getRefreshToken(): string | undefined {
  return Cookies.get(COOKIE_REFRESH_TOKEN);
}

/**
 * 保存済みのユーザー情報を返す。壊れた値のときは undefined。
 *
 * @returns ユーザー情報
 */
export function getStoredUser(): User | undefined {
  const raw = Cookies.get(COOKIE_USER);
  if (!raw) return undefined;
  try {
    const parsed = JSON.parse(raw) as Partial<User>;
    if (typeof parsed?.email === 'string') return parsed as User;
  } catch {
    // 壊れた Cookie は無視する
  }
  return undefined;
}

/**
 * トークンを保存する。
 *
 * @param accessToken アクセストークン
 * @param refreshToken リフレッシュトークン
 */
export function saveTokens(accessToken: string, refreshToken: string): void {
  Cookies.set(COOKIE_ACCESS_TOKEN, accessToken, cookieOptions(ACCESS_TOKEN_DAYS));
  Cookies.set(COOKIE_REFRESH_TOKEN, refreshToken, cookieOptions(REFRESH_TOKEN_DAYS));
}

/**
 * ユーザー情報を保存する。
 *
 * @param user ユーザー情報
 */
export function saveUser(user: User): void {
  Cookies.set(COOKIE_USER, JSON.stringify(user), cookieOptions(REFRESH_TOKEN_DAYS));
}

/** 保存済みの認証情報をすべて削除する。 */
export function clearSession(): void {
  Cookies.remove(COOKIE_ACCESS_TOKEN);
  Cookies.remove(COOKIE_REFRESH_TOKEN);
  Cookies.remove(COOKIE_USER);
}
