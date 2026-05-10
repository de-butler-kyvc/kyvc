import type { NextConfig } from "next";
import { PHASE_DEVELOPMENT_SERVER } from "next/constants";

const BACK_ADMIN_API = process.env.BACK_ADMIN_API_URL ?? "http://192.168.219.108:8083";

const baseConfig: NextConfig = {
  trailingSlash: true,
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
        ];
      },
    };
  }

  return {
    ...baseConfig,
    output: "export",
  };
}
