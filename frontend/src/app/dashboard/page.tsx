"use client";

import { useEffect, useState } from "react";
import { getToken } from "@/lib/auth";
import { fetchDashboard } from "@/lib/dashboard";
import type { DashboardDto } from "@/lib/dashboard";

type Phase = "loading" | "unauthorized" | "ready" | "error";

function confidenceColor(percent: number): string {
  if (percent < 40) return "bg-red-500";
  if (percent < 70) return "bg-amber-500";
  return "bg-emerald-500";
}

export default function DashboardPage() {
  const [phase, setPhase] = useState<Phase>("loading");
  const [data, setData] = useState<DashboardDto | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      setPhase("unauthorized");
      return;
    }
    fetchDashboard(token)
      .then((d) => {
        setData(d);
        setPhase("ready");
      })
      .catch((e) => {
        setError(e instanceof Error ? e.message : String(e));
        setPhase("error");
      });
  }, []);

  if (phase === "loading") {
    return <p className="py-10 text-center text-ink-600 dark:text-brand-50">Считаем прогресс…</p>;
  }

  if (phase === "unauthorized") {
    return (
      <div className="mx-auto max-w-md space-y-4 py-10 text-center">
        <h1 className="text-2xl font-semibold tracking-tight">Дашборд</h1>
        <p className="text-ink-600 dark:text-brand-50">Войдите через Telegram, чтобы видеть прогресс.</p>
        <a href="/login" className="inline-block rounded-lg bg-brand-600 px-5 py-2.5 font-medium text-white hover:bg-brand-700">
          Войти
        </a>
      </div>
    );
  }

  if (phase === "error" || !data) {
    return (
      <div className="mx-auto max-w-md space-y-4 py-10 text-center">
        <p className="text-sm text-red-600">{error}</p>
        <a href="/dashboard" className="text-sm underline">Попробовать снова</a>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-10">
      <section className="grid grid-cols-3 gap-4">
        <div className="rounded-xl border border-black/10 p-5 text-center dark:border-white/15">
          <p className="text-3xl font-semibold">{data.readinessPercent}%</p>
          <p className="mt-1 text-xs text-ink-600 dark:text-brand-50">готовность к собеседованию</p>
        </div>
        <div className="rounded-xl border border-black/10 p-5 text-center dark:border-white/15">
          <p className="text-3xl font-semibold">{data.streakDays}</p>
          <p className="mt-1 text-xs text-ink-600 dark:text-brand-50">{data.streakDays === 1 ? "день подряд" : "дней подряд"}</p>
        </div>
        <div className="rounded-xl border border-black/10 p-5 text-center dark:border-white/15">
          <p className="text-3xl font-semibold">{data.answeredTotal}</p>
          <p className="mt-1 text-xs text-ink-600 dark:text-brand-50">всего ответов</p>
        </div>
      </section>

      <section className="space-y-4">
        <h2 className="text-lg font-semibold">Слабые темы</h2>
        {data.weakestTopics.length === 0 ? (
          <p className="text-sm text-ink-600 dark:text-brand-50">
            Пока пусто: отвечай на карточки — и здесь появятся темы, где ты менее уверен.
          </p>
        ) : (
          <div className="space-y-3">
            {data.weakestTopics.map((topic) => (
              <div key={topic.slug} className="space-y-1">
                <div className="flex items-baseline justify-between text-sm">
                  <span className="font-medium">{topic.title}</span>
                  <span className="text-ink-600 dark:text-brand-50">{topic.confidencePercent}% · {topic.answered} отв.</span>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-black/5 dark:bg-white/10">
                  <div className={`h-full rounded-full ${confidenceColor(topic.confidencePercent)}`} style={{ width: `${Math.max(topic.confidencePercent, 3)}%` }} />
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="space-y-4">
        <h2 className="text-lg font-semibold">Карта знаний</h2>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          {data.categoryProgress.map((c) => (
            <a
              key={c.slug}
              href={`/questions?category=${c.slug}`}
              className={`rounded-lg border border-black/10 p-3 text-left transition hover:border-brand-500 dark:border-white/15`}
              title={`${c.answered} ответов, уверенность ${c.confidencePercent}%`}
            >
              <p className="truncate text-sm font-medium">{c.title}</p>
              <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-black/5 dark:bg-white/10">
                <div className={`h-full rounded-full ${confidenceColor(c.confidencePercent)}`} style={{ width: `${Math.max(c.confidencePercent, 3)}%` }} />
              </div>
              <p className="mt-1 text-xs text-ink-600 dark:text-brand-50">{c.confidencePercent}%</p>
            </a>
          ))}
          {data.categoryProgress.length === 0 && (
            <p className="col-span-3 text-sm text-ink-600 dark:text-brand-50">
              Карта заполнится после первых ответов в <a href="/practice" className="underline">практике</a>.
            </p>
          )}
        </div>
      </section>

      <section className="rounded-xl border border-brand-200 bg-brand-50/50 p-5 text-center dark:border-brand-500/30 dark:bg-brand-500/10">
        <p className="text-sm text-ink-600 dark:text-brand-50">
          {data.weakestTopics.length > 0
            ? "Прокачай слабые темы — «Практика» подберёт карточки с умом."
            : "15 минут в день — и карта знаний начнёт зеленеть."}
        </p>
        <a href="/practice" className="mt-3 inline-block rounded-lg bg-brand-600 px-5 py-2.5 font-medium text-white hover:bg-brand-700">
          Тренироваться
        </a>
      </section>
    </div>
  );
}
