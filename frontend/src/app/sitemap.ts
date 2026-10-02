import type { MetadataRoute } from "next";
import type { Category, Page, QuestionSummary } from "@/lib/types";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

/** Базовый URL сайта: в dev локальный, в прод из env. */
const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://wisereport.online";

async function fetchCategoriesRaw(): Promise<Category[]> {
  return getJsonNoStore<Category[]>("/api/v1/categories");
}

async function fetchQuestionsRaw(page: number): Promise<Page<QuestionSummary>> {
  return getJsonNoStore<Page<QuestionSummary>>(`/api/v1/questions?page=${page}&size=100`);
}

async function getJsonNoStore<T>(path: string): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, { cache: "no-store" });
  if (!response.ok) {
    throw new Error(`Запрос ${path} вернул ${response.status}`);
  }
  return (await response.json()) as T;
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const staticEntries: MetadataRoute.Sitemap = [
    { url: SITE_URL, changeFrequency: "daily", priority: 1 },
    { url: `${SITE_URL}/questions`, changeFrequency: "daily", priority: 0.9 },
    { url: `${SITE_URL}/login`, changeFrequency: "monthly", priority: 0.3 },
  ];

  const categoryEntries: MetadataRoute.Sitemap = (await fetchCategoriesRaw().catch(() => []))
    .map((c) => ({
      url: `${SITE_URL}/questions?category=${c.slug}`,
      changeFrequency: "weekly" as const,
      priority: 0.7,
    }));

  // Все вопросы пачками по 100 (банк маленький, но пусть масштабируется).
  const questionEntries: MetadataRoute.Sitemap = [];
  let page = 0;
  let last: number;
  do {
    const batch = await fetchQuestionsRaw(page).catch(() => null);
    if (!batch) break;
    last = batch.content?.length ?? 0;
    for (const q of batch.content ?? []) {
      questionEntries.push({
        url: `${SITE_URL}/questions/${q.id}`,
        changeFrequency: "monthly",
        priority: 0.6,
      });
    }
    page++;
  } while (last === 100 && page < 20);

  return [...staticEntries, ...categoryEntries, ...questionEntries];
}
