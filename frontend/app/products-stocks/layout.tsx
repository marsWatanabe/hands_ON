import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: '製品在庫一覧',
  description: '製品在庫の一覧、検索、登録、編集、削除を行います。',
};

export default function ProductsStocksLayout({ children }: { children: React.ReactNode }) {
  return children;
}
