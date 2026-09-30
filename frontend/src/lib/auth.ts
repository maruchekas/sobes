// Клиентская часть авторизации: Telegram Login Widget + JWT в localStorage.

export interface UserView {
  id: number;
  displayName: string;
  telegramUsername: string | null;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  user: UserView;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const TOKEN_KEY = "sobes_token";

export const BOT_USERNAME = process.env.NEXT_PUBLIC_TELEGRAM_BOT ?? "";

const telegramBotId = Number(process.env.NEXT_PUBLIC_TELEGRAM_BOT_ID ?? "");
export const BOT_ID = Number.isSafeInteger(telegramBotId) && telegramBotId > 0 ? telegramBotId : null;

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setSession(response: LoginResponse): void {
  localStorage.setItem(TOKEN_KEY, response.token);
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY);
}

/** Обмен данных Telegram Login Widget на JWT платформы. */
export async function loginWithTelegram(widgetData: Record<string, string | number>): Promise<LoginResponse> {
  const response = await fetch(`${API_URL}/api/v1/auth/telegram`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(widgetData),
  });
  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new Error(`Вход не удался (${response.status}): ${text.slice(0, 200)}`);
  }
  return (await response.json()) as LoginResponse;
}

export async function fetchMe(token: string): Promise<UserView> {
  const response = await fetch(`${API_URL}/api/v1/auth/me`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  if (response.status === 401) {
    clearSession();
    throw new Error("Сессия истекла");
  }
  if (!response.ok) {
    throw new Error(`/me вернул ${response.status}`);
  }
  return (await response.json()) as UserView;
}
