import type { Category, Difficulty, Page, QuestionDetails, QuestionSummary } from "./types";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    next: { revalidate: 60 },
  });

  if (!response.ok) {
    throw new Error(`Запрос ${path} вернул ${response.status}`);
  }

  return (await response.json()) as T;
}

export function fetchCategories(): Promise<Category[]> {
  return getJson<Category[]>("/api/v1/categories");
}

export function fetchQuestions(params: {
  category?: string;
  difficulty?: Difficulty;
  page?: number;
  size?: number;
}): Promise<Page<QuestionSummary>> {
  const search = new URLSearchParams();
  if (params.category) search.set("category", params.category);
  if (params.difficulty) search.set("difficulty", params.difficulty);
  search.set("page", String(params.page ?? 0));
  search.set("size", String(params.size ?? 20));

  return getJson<Page<QuestionSummary>>(`/api/v1/questions?${search.toString()}`);
}

export async function fetchQuestion(id: number): Promise<QuestionDetails | null> {
  const response = await fetch(`${API_URL}/api/v1/questions/${id}`, {
    next: { revalidate: 60 },
  });

  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`Запрос вопроса ${id} вернул ${response.status}`);
  }

  return (await response.json()) as QuestionDetails;
}
