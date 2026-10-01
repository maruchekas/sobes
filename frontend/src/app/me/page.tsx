"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getToken } from "@/lib/auth";
import { fetchCabinet, saveSettings, TIMEZONES } from "@/lib/cabinet";
import type { CabinetDto, SettingsDto } from "@/lib/cabinet";
import { LogoutButton } from "@/components/LogoutButton";

type Phase = "loading" | "ready" | "error" | "unauthorized";

export default function CabinetPage() {
  const router = useRouter();
  const [phase, setPhase] = useState<Phase>("loading");
  const [error, setError] = useState<string | null>(null);
  const [cabinet, setCabinet] = useState<CabinetDto | null>(null);
  const [form, setForm] = useState<SettingsDto | null>(null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      setPhase("unauthorized");
      return;
    }
    fetchCabinet(token)
      .then((c) => {
        setCabinet(c);
        setForm(c.settings);
        setPhase("ready");
      })
      .catch((e) => {
        if (e instanceof Error && e.message.includes("Сессия истекла")) {
          setPhase("unauthorized");
        } else {
          setError(e instanceof Error ? e.message : String(e));
          setPhase("error");
        }
      });
  }, []);

  if (phase === "loading") {
    return <p className="py-10 text-center text-ink-600 dark:text-brand-50">Загружаем кабинет…</p>;
  }

  if (phase === "unauthorized") {
    return (
      <div className="mx-auto max-w-md space-y-4 py-10 text-center">
        <h1 className="text-2xl font-semibold tracking-tight">Личный кабинет</h1>
        <p className="text-ink-600 dark:text-brand-50">Войдите через Telegram, чтобы увидеть кабинет.</p>
        <a href="/login" className="inline-block rounded-lg bg-brand-600 px-5 py-2.5 font-medium text-white hover:bg-brand-700">
          Войти
        </a>
      </div>
    );
  }

  if (phase === "error" || !cabinet || !form) {
    return (
      <div className="mx-auto max-w-md space-y-4 py-10 text-center">
        <h1 className="text-2xl font-semibold tracking-tight">Личный кабинет</h1>
        <p className="text-sm text-red-600">{error ?? "Неизвестная ошибка"}</p>
        <button type="button" onClick={() => router.refresh()} className="text-sm underline">
          Попробовать снова
        </button>
      </div>
    );
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    const token = getToken();
    if (!token) return;
    setSaving(true);
    setSaved(false);
    setError(null);
    try {
      const updated = await saveSettings(token, form);
      setForm(updated);
      setSaved(true);
      setTimeout(() => setSaved(false), 2500);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="mx-auto max-w-xl space-y-8">
      <section className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">Личный кабинет</h1>
        <dl className="space-y-1 text-sm">
          <div className="flex gap-2">
            <dt className="text-ink-600 dark:text-brand-50">Имя:</dt>
            <dd className="font-medium">{cabinet.displayName}</dd>
          </div>
          <div className="flex gap-2">
            <dt className="text-ink-600 dark:text-brand-50">Telegram:</dt>
            <dd className="font-medium">
              {cabinet.telegramUsername ? `@${cabinet.telegramUsername}` : "—"}
            </dd>
          </div>
        </dl>
        <div className="pt-1">
          <LogoutButton />
        </div>
      </section>

      <form onSubmit={submit} className="space-y-5 rounded-xl border border-black/10 p-6 dark:border-white/15">
        <h2 className="font-semibold">Напоминания о повторениях</h2>
        <p className="text-sm text-ink-600 dark:text-brand-50">
          Когда практика на карточках будет готова, бот будет присылать вопрос дня и напоминать о повторениях.
        </p>

        <label className="block space-y-1 text-sm">
          <span className="font-medium">Часовой пояс</span>
          <select
            value={form.timezone}
            onChange={(e) => setForm({ ...form, timezone: e.target.value })}
            className="w-full rounded-lg border border-black/10 bg-transparent px-3 py-2 dark:border-white/15"
          >
            {TIMEZONES.map((tz) => (
              <option key={tz} value={tz}>
                {tz}
              </option>
            ))}
          </select>
        </label>

        <div className="flex items-end gap-3">
          <label className="block space-y-1 text-sm">
            <span className="font-medium">Часы</span>
            <input
              type="number"
              min={0}
              max={23}
              value={form.reminderHour}
              onChange={(e) => setForm({ ...form, reminderHour: Number(e.target.value) })}
              className="w-20 rounded-lg border border-black/10 bg-transparent px-3 py-2 dark:border-white/15"
            />
          </label>
          <span className="pb-2">:</span>
          <label className="block space-y-1 text-sm">
            <span className="font-medium">Минуты</span>
            <input
              type="number"
              min={0}
              max={59}
              value={form.reminderMinute}
              onChange={(e) => setForm({ ...form, reminderMinute: Number(e.target.value) })}
              className="w-20 rounded-lg border border-black/10 bg-transparent px-3 py-2 dark:border-white/15"
            />
          </label>
        </div>

        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={form.remindersEnabled}
            onChange={(e) => setForm({ ...form, remindersEnabled: e.target.checked })}
          />
          <span>Присылать напоминания в Telegram</span>
        </label>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <div className="flex items-center gap-3">
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-brand-600 px-5 py-2.5 font-medium text-white hover:bg-brand-700 disabled:opacity-50"
          >
            {saving ? "Сохраняем…" : "Сохранить"}
          </button>
          {saved && <span className="text-sm font-medium text-brand-600">Сохранено ✓</span>}
        </div>
      </form>
    </div>
  );
}
