export interface QuestionDto {
  questionId: number;
  body: string;
  category: string;
  categorySlug: string;
  difficulty: string;
  index: number;
  total: number;
  secondsLimit: number;
}

export interface SessionStateDto {
  sessionId: number;
  question: QuestionDto | null;
  answeredCount: number;
  total: number;
}

export interface ReportItemDto {
  questionId: number;
  body: string;
  category: string;
  difficulty: string;
  rating: string;
  secondsSpent: number;
  userTextLength: number;
  referenceAnswer: string;
  followup: string | null;
}

export interface ReportDto {
  sessionId: number;
  total: number;
  answeredCount: number;
  totalSeconds: number;
  byRating: Record<string, number>;
  weakCategories: string[];
  items: ReportItemDto[];
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

async function post<T>(path: string, body?: unknown): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("sobes_token") : null;
  const res = await fetch(`${API_URL}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body ?? {}),
  });
  if (!res.ok) throw new Error(`Запрос ${path}: ${res.status}`);
  return (await res.json()) as T;
}

async function get<T>(path: string): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("sobes_token") : null;
  const res = await fetch(`${API_URL}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) throw new Error(`Запрос ${path}: ${res.status}`);
  return (await res.json()) as T;
}

export const interviewApi = {
  start: (categories: string[], total: number) =>
    post<SessionStateDto>("/api/v1/interview/start", { categories, total }),
  answer: (sessionId: number, questionId: number, userText: string, rating: string, secondsSpent: number) =>
    post<SessionStateDto>(`/api/v1/interview/${sessionId}/answer`, {
      questionId, userText, rating, secondsSpent,
    }),
  finish: (sessionId: number) => post<ReportDto>(`/api/v1/interview/${sessionId}/finish`),
};
