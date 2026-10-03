// API дашборда прогресса.

export interface CategoryProgress {
  slug: string;
  title: string;
  answered: number;
  confidencePercent: number;
}

export interface DashboardDto {
  answeredTotal: number;
  streakDays: number;
  weakestTopics: CategoryProgress[];
  categoryProgress: CategoryProgress[];
  readinessPercent: number;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export async function fetchDashboard(token: string): Promise<DashboardDto> {
  const response = await fetch(`${API_URL}/api/v1/dashboard`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  if (response.status === 401) throw new Error("Сессия истекла — войдите заново");
  if (!response.ok) throw new Error(`Дашборд вернул ${response.status}`);
  return (await response.json()) as DashboardDto;
}
