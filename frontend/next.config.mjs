import path from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * バックエンドの接続先。未設定のときはローカル開発用の既定値を使う。
 * ブラウザからは同一オリジン（/api/...）で呼び、Next.js が転送する。
 */
const backendApiUrl = process.env.BACKEND_API_URL || 'http://localhost:8080';

/** @type {import('next').NextConfig} */
const nextConfig = {
  // 上位のディレクトリに別の package-lock.json があっても、このディレクトリをルートとして扱う
  outputFileTracingRoot: path.dirname(fileURLToPath(import.meta.url)),
  images: {
    unoptimized: true,
  },
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${backendApiUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
