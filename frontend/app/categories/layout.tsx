import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'カテゴリ管理',
  description: '部品・製品のカテゴリの一覧、検索、登録、編集、削除を行います。',
};

export default function CategoriesLayout({ children }: { children: React.ReactNode }) {
  return children;
}
