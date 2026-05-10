import type { NextConfig } from "next";

const BACK_ADMIN_API = process.env.BACK_ADMIN_API_URL ?? "https://dev-api-admin-kyvc.synology.me";

const nextConfig: NextConfig = {
  images: { unoptimized: true },
  async rewrites() {
    return [
      { source: "/api/admin/auth/:path*", destination: `${BACK_ADMIN_API}/api/admin/auth/:path*` },
      { source: "/api/admin/backend/:path*", destination: `${BACK_ADMIN_API}/api/admin/backend/:path*` },
      { source: "/api/admin/me/:path*", destination: `${BACK_ADMIN_API}/api/admin/me/:path*` },
    ];
  },
};

export default nextConfig;
