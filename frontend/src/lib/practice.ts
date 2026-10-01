// API практики: очередь карточек, самооценка, сводка.

export type Rating = "AGAIN" | "HARD" | "GOOD" | "EASY";

export interface QueueCard {
  questionId: number;
  state: "DUE" | "NEW";
  dueAt: string | null;
  category: string;
  difficulty: string;
  body: string;
}

export interface AnswerResult {
  questionId: number;
  rating: Rating;
  intervalDays: number;
  dueAt: string;
}

export interface PracticeSummary {
  dueCount: number;
  newCount: number;
  answeredTotal: number;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

async function api<T>(path: string, token: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    cache: "no-store",
  });
  if (response.status === 401) throw new Error("Сессия истекла — войдите заново");
  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new Error(`Запрос ${path} вернул ${response.status}: ${text.slice(0, 200)}`);
  }
  return (await response.json()) as T;
}

export function fetchQueue(token: string, limit = 20): Promise<QueueCard[]> {
  return api(`/api/v1/practice/queue?limit=${limit}`, token);
}

export function sendAnswer(token: string, questionId: number, rating: Rating): Promise<AnswerResult> {
  return api("/api/v1/practice/answer", token, {
    method: "POST",
    body: JSON.stringify({ questionId, rating }),
  });
}

export function fetchSummary(token: string): Promise<PracticeSummary> {
  return api("/api/v1/practice/summary", token);
}

export function fetchQuestionDetails(token: string, id: number): Promise<{ body: string; answer: string; followup: string | null }> {
  return api(`/api/v1/questions/${id}`, token);
}
