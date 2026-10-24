import type { AuthResponse, ErrorBody } from '../types/api';
import { clearSession, getAccessToken, getRefreshToken, saveTokens } from './auth-storage';

/** API のベースパス。Next.js の rewrites でバックエンドへ転送される。 */
export const API_BASE = '/api/v1';

/** 認証 API のパス。 */
export const AUTH_PATHS = {
  register: '/auth/register',
  authenticate: '/auth/authenticate',
  refresh: '/auth/refresh-token',
} as const;

/** セッションが失効したことを通知するイベント名（AuthContext が購読する）。 */
export const SESSION_EXPIRED_EVENT = 'inventrack:session-expired';

/** 通信エラー（接続できない）のエラーコード。 */
export const NETWORK_ERROR_CODE = 'NETWORK_ERROR';

/** API が返したエラー（またはネットワークエラー）。 */
export class ApiError extends Error {
  /** HTTP ステータス。通信できなかったときは 0。 */
  readonly status: number;
  /** バックエンドの ErrorCode 名（例: VALIDATION_ERROR）。 */
  readonly code: string;

  /**
   * @param status HTTP ステータス
   * @param code エラーコード
   * @param message ユーザー向けメッセージ
   */
  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

/** ステータスごとの既定メッセージ（レスポンスに本文がないときに使う）。 */
function defaultMessage(status: number): string {
  if (status === 401) return '認証に失敗しました。もう一度ログインしてください。';
  if (status === 403) return 'この操作を行う権限がありません。';
  if (status === 404) return '対象のデータが見つかりません。';
  if (status >= 500) return 'サーバーでエラーが発生しました。しばらくしてからもう一度お試しください。';
  return 'リクエストに失敗しました。';
}

/**
 * エラーレスポンスを ApiError に変換する。
 *
 * @param res レスポンス
 * @returns ApiError
 */
async function toApiError(res: Response): Promise<ApiError> {
  try {
    const body = (await res.json()) as Partial<ErrorBody>;
    if (body?.error?.message) {
      return new ApiError(res.status, body.error.code ?? 'UNKNOWN', body.error.message);
    }
  } catch {
    // JSON 以外の本文は無視する
  }
  return new ApiError(res.status, 'UNKNOWN', defaultMessage(res.status));
}

/**
 * fetch を呼び、通信できないときは ApiError（NETWORK_ERROR）にする。
 *
 * @param input URL
 * @param init オプション
 * @returns レスポンス
 */
async function safeFetch(input: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(input, init);
  } catch {
    throw new ApiError(0, NETWORK_ERROR_CODE, 'サーバーに接続できません。ネットワークを確認してください。');
  }
}

/** 同時に複数のリクエストが 401 になっても、トークン更新は 1 回にまとめる。 */
let refreshInFlight: Promise<boolean> | null = null;

/**
 * リフレッシュトークンでトークンを更新する。成功したら新しいトークンを保存する。
 *
 * @returns 更新できたら true
 */
function refreshTokens(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      const refreshToken = getRefreshToken();
      if (!refreshToken) return false;
      try {
        const res = await safeFetch(`${API_BASE}${AUTH_PATHS.refresh}`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${refreshToken}` },
        });
        if (!res.ok) return false;
        const body = (await res.json()) as AuthResponse;
        saveTokens(body.accessToken, body.refreshToken);
        return true;
      } catch {
        return false;
      }
    })().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

/** セッションを破棄し、失効を通知する。 */
function expireSession(): void {
  clearSession();
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
  }
}

/** apiFetch のオプション。 */
export interface ApiOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  /** JSON として送る本文。 */
  body?: unknown;
  /** false のときは Authorization を付けず、401 でもトークン更新をしない（ログイン等）。 */
  auth?: boolean;
}

/**
 * API を呼ぶ。認証が必要な API で 401 が返ったときは、トークンを 1 回だけ更新して再試行する。
 * 更新にも失敗したらセッションを破棄し、SESSION_EXPIRED_EVENT を通知する。
 *
 * @param path API_BASE からの相対パス（例: `/centers`）
 * @param options オプション
 * @returns レスポンス本文（JSON）
 * @throws ApiError ステータスが 2xx 以外、または通信できないとき
 */
export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const { method = 'GET', body, auth = true } = options;

  const send = (): Promise<Response> => {
    const headers: Record<string, string> = {};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (auth) {
      const token = getAccessToken();
      if (token) headers.Authorization = `Bearer ${token}`;
    }
    return safeFetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  };

  let res = await send();

  if (res.status === 401 && auth) {
    if (await refreshTokens()) {
      res = await send();
    }
    if (res.status === 401) {
      expireSession();
      throw await toApiError(res);
    }
  }

  if (!res.ok) {
    throw await toApiError(res);
  }
  return (await res.json()) as T;
}
