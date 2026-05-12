import type { NextConfig } from "next";

const CORE_ADMIN_API =
  process.env.CORE_ADMIN_API_URL ??
  process.env.NEXT_PUBLIC_CORE_ADMIN_API_BASE_URL ??
  "https://dev-admin-core-kyvc.khuoo.synology.me";

const baseConfig: NextConfig = {
  turbopack: {
    root: __dirname,
  },
  images: { unoptimized: true },
  async rewrites() {
    return [
      { source: "/admin/:path*", destination: `${CORE_ADMIN_API}/admin/:path*` },
      { source: "/health", destination: `${CORE_ADMIN_API}/health` },
    ];
  },
};

export default baseConfig;
