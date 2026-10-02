import type { MetadataRoute } from "next";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://wisereport.online";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: "*",
        allow: "/",
        // Приватные страницы не индексируем.
        disallow: ["/me", "/dashboard", "/practice", "/link-bot"],
      },
    ],
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
