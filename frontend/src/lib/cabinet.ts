// API личного кабинета (авторизовано Bearer-токеном).

export interface SettingsDto {
  timezone: string;
  reminderHour: number;
  reminderMinute: number;
  remindersEnabled: boolean;
}

export interface CabinetDto {
  userId: number;
  displayName: string;
  telegramUsername: string | null;
  settings: SettingsDto;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export async function fetchCabinet(token: string): Promise<CabinetDto> {
  const response = await fetch(`${API_URL}/api/v1/me/settings`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  if (response.status === 401) throw new Error("Сессия истекла — войдите заново");
  if (!response.ok) throw new Error(`Кабинет вернул ${response.status}`);
  return (await response.json()) as CabinetDto;
}

export async function saveSettings(token: string, settings: SettingsDto): Promise<SettingsDto> {
  const response = await fetch(`${API_URL}/api/v1/me/settings`, {
    method: "PUT",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify(settings),
  });
  if (response.status === 401) throw new Error("Сессия истекла — войдите заново");
  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new Error(`Сохранение не удалось (${response.status}): ${text.slice(0, 200)}`);
  }
  return (await response.json()) as SettingsDto;
}

/** Популярные зоны для селекта: РФ-центрированно, остальное — по алфавиту. */
export const TIMEZONES = [
  "Europe/Moscow",
  "Europe/Kaliningrad",
  "Europe/Samara",
  "Asia/Yekaterinburg",
  "Asia/Omsk",
  "Asia/Novosibirsk",
  "Asia/Krasnoyarsk",
  "Asia/Irkutsk",
  "Asia/Yakutsk",
  "Asia/Vladivostok",
  "Asia/Magadan",
  "Asia/Kamchatka",
  "Europe/Berlin",
  "Europe/London",
  "Europe/Lisbon",
  "Asia/Almaty",
  "Asia/Tbilisi",
  "Asia/Yerevan",
  "America/New_York",
  "America/Los_Angeles",
];
