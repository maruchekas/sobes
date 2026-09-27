import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Отдельная сборка для Docker: в образ попадает только необходимое.
  output: "standalone",
  reactStrictMode: true,
};

export default nextConfig;
