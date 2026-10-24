import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: '在庫センター管理',
  description: '在庫センターの一覧、検索、登録、編集、削除を行います。',
};

export default function CentersLayout({ children }: { children: React.ReactNode }) {
  return children;
}
