import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Отдельная сборка для Docker: в образ попадает только необходимое.
  output: "standalone",
  reactStrictMode: true,
  // Локальная разработка через hosts-домен (Telegram Login Widget требует домен с точкой):
  // браузер заходит на wisereport.online:3000, dev-сервер должен разрешать этот origin.
  allowedDevOrigins: ["wisereport.online"],
};

export default nextConfig;
