import type { NextConfig } from "next";
import { PHASE_DEVELOPMENT_SERVER } from "next/constants";

const BACK_ADMIN_API =
  process.env.BACK_ADMIN_API_URL ??
  process.env.NEXT_PUBLIC_ADMIN_API_BASE_URL ??
  "https://dev-admin-api-kyvc.khuoo.synology.me";
const BACKEND_API =
  process.env.BACKEND_API_URL ??
  process.env.NEXT_PUBLIC_BACKEND_API_BASE_URL ??
  "https://dev-api-kyvc.khuoo.synology.me";

const baseConfig: NextConfig = {
  trailingSlash: true,
  turbopack: {
    root: __dirname,
  },
  images: {
    unoptimized: true,
  },
};

export default function nextConfig(phase: string): NextConfig {
  if (phase === PHASE_DEVELOPMENT_SERVER) {
    return {
      ...baseConfig,
      async rewrites() {
        return [
          { source: "/api/admin/auth/:path*", destination: `${BACK_ADMIN_API}/api/admin/auth/:path*` },
          { source: "/api/admin/backend/:path*", destination: `${BACK_ADMIN_API}/api/admin/backend/:path*` },
          { source: "/api/admin/me/:path*", destination: `${BACK_ADMIN_API}/api/admin/me/:path*` },
          { source: "/api/finance/:path*", destination: `${BACKEND_API}/api/finance/:path*` },
          { source: "/api/mobile/:path*", destination: `${BACKEND_API}/api/mobile/:path*` },
        ];
      },
    };
  }

  return {
    ...baseConfig,
    output: "export",
  };
}
