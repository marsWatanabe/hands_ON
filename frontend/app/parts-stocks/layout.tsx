import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: '部品在庫一覧',
  description: '部品在庫の一覧、検索、登録、編集、削除を行います。',
};

export default function PartsStocksLayout({ children }: { children: React.ReactNode }) {
  return children;
}
