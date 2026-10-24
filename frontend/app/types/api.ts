/**
 * バックエンド API の型定義。
 * 参照元: docs/detailed-design.md、backend の dto パッケージ。
 */

/** ユーザーのロール。 */
export type Role = 'USER' | 'ADMIN';

/** ユーザー情報（パスワードを含まない）。 */
export interface User {
  id: number;
  firstname: string;
  lastname: string;
  email: string;
  role: Role;
}

/** 認証 API のレスポンス。トークン更新のときは user を含まない。 */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user?: User;
}

/** 稼働状況（0: 稼働中、1: 稼働停止）。 */
export type OperationalStatus = 0 | 1;

/** 在庫センター。 */
export interface Center {
  centerId: number;
  centerName: string;
  postCode: string;
  address: string;
  phoneNumber: string;
  managerName: string;
  operationalStatus: OperationalStatus;
  maxStorageCapacity: number | null;
  currentStorageCapacity: number | null;
  notes: string | null;
  createDate: string;
  updateDate: string;
}

/** 在庫センターの作成・更新リクエスト。 */
export interface CenterRequest {
  centerName: string;
  postCode: string;
  address: string;
  phoneNumber: string;
  managerName: string;
  operationalStatus: OperationalStatus;
  maxStorageCapacity: number | null;
  currentStorageCapacity: number | null;
  notes: string | null;
}

/** ページング情報。page は 1 始まり。 */
export interface PageMeta {
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

/** 一覧 API の共通レスポンス。 */
export interface PageResponse<T> {
  data: T[];
  meta: PageMeta;
}

/** 一覧のソート項目（バックエンドの許可リストと一致させる）。 */
export type CenterSortKey =
  | 'centerId'
  | 'centerName'
  | 'address'
  | 'operationalStatus'
  | 'maxStorageCapacity'
  | 'currentStorageCapacity'
  | 'createDate'
  | 'updateDate';

/** ソート順。 */
export type SortOrder = 'asc' | 'desc';

/** 在庫センター一覧の検索条件。 */
export interface CenterSearchParams {
  centerName: string;
  address: string;
  /** 空文字は絞り込みなし。 */
  operationalStatus: '' | '0' | '1';
  page: number;
  size: number;
  sort: CenterSortKey;
  order: SortOrder;
}

/** 共通のエラーレスポンス形式。 */
export interface ErrorBody {
  error: {
    code: string;
    message: string;
    timestamp?: string;
  };
}

// ---------------------------------------------------------------------------
// カテゴリ、部品在庫、製品在庫（docs/detailed-design-stocks.md）
// ---------------------------------------------------------------------------

/** カテゴリ種別（part: 部品、product: 製品）。 */
export type CategoryType = 'part' | 'product';

/** カテゴリ。 */
export interface Category {
  categoryId: number;
  categoryType: CategoryType;
  categoryName: string;
  description: string | null;
  createDate: string;
  updateDate: string;
}

/** カテゴリの作成・更新リクエスト。 */
export interface CategoryRequest {
  categoryType: CategoryType;
  categoryName: string;
  description: string | null;
}

/** カテゴリ一覧のソート項目。 */
export type CategorySortKey = 'categoryId' | 'categoryType' | 'categoryName' | 'createDate' | 'updateDate';

/** カテゴリ一覧の検索条件。 */
export interface CategorySearchParams {
  /** 空文字はすべて。 */
  categoryType: '' | CategoryType;
  categoryName: string;
  page: number;
  size: number;
  sort: CategorySortKey;
  order: SortOrder;
}

/** 在庫の種類（部品在庫、製品在庫）。API のパスと画面を切り替える。 */
export type StockKind = 'parts' | 'products';

/** 在庫（部品在庫、製品在庫共通）。カテゴリ名とセンター名は API が結合して返す。 */
export interface Stock {
  stockId: number;
  /** 製品在庫のみ。 */
  productCode?: string;
  categoryId: number | null;
  categoryName: string | null;
  name: string;
  centerId: number | null;
  centerName: string | null;
  description: string | null;
  amount: number | null;
  createDate: string;
  updateDate: string;
}

/** 在庫の作成・更新リクエスト。 */
export interface StockRequest {
  /** 製品在庫のみ。 */
  productCode?: string;
  categoryId: number;
  name: string;
  centerId: number;
  description: string | null;
  amount: number;
}

/** 在庫一覧のソート項目。productCode は製品在庫のみ。 */
export type StockSortKey =
  | 'stockId'
  | 'name'
  | 'amount'
  | 'categoryName'
  | 'centerName'
  | 'createDate'
  | 'updateDate'
  | 'productCode';

/** 在庫一覧の検索条件。空文字は絞り込みなし。 */
export interface StockSearchParams {
  categoryId: string;
  centerId: string;
  name: string;
  /** 製品在庫のみ。 */
  productCode: string;
  amountMin: string;
  amountMax: string;
  page: number;
  size: number;
  sort: StockSortKey;
  order: SortOrder;
}