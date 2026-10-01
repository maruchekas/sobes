"use client";

import { useEffect, useState } from "react";
import { getToken } from "@/lib/auth";
import { fetchQuestionDetails, fetchQueue, sendAnswer } from "@/lib/practice";
import type { PracticeSummary, QueueCard, Rating } from "@/lib/practice";
import { fetchSummary } from "@/lib/practice";

type Phase = "loading" | "unauthorized" | "question" | "answer" | "done" | "error";

const RATING_LABELS: { rating: Rating; label: string; hint: string; className: string }[] = [
  { rating: "AGAIN", label: "Забыл", hint: "завтра", className: "bg-red-600 hover:bg-red-700" },
  { rating: "HARD", label: "Трудно", hint: "короче интервал", className: "bg-amber-600 hover:bg-amber-700" },
  { rating: "GOOD", label: "Хорошо", hint: "по плану", className: "bg-brand-600 hover:bg-brand-700" },
  { rating: "EASY", label: "Легко", hint: "дольше интервал", className: "bg-emerald-600 hover:bg-emerald-700" },
];

export default function PracticePage() {
  const [phase, setPhase] = useState<Phase>("loading");
  const [error, setError] = useState<string | null>(null);
  const [queue, setQueue] = useState<QueueCard[]>([]);
  const [index, setIndex] = useState(0);
  const [summary, setSummary] = useState<PracticeSummary | null>(null);
  const [answer, setAnswer] = useState<string | null>(null);
  const [followup, setFollowup] = useState<string | null>(null);
  const [rated, setRated] = useState(false);
  const [sessionAnswered, setSessionAnswered] = useState(0);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      setPhase("unauthorized");
      return;
    }
    Promise.all([fetchQueue(token, 20), fetchSummary(token)])
      .then(([q, s]) => {
        setQueue(q);
        setSummary(s);
        setPhase(q.length > 0 ? "question" : "done");
      })
      .catch((e) => {
        setError(e instanceof Error ? e.message : String(e));
        setPhase("error");
      });
  }, []);

  const card = queue[index];

  const showAnswer = async () => {
    const token = getToken();
    if (!token || !card) return;
    setPhase("answer");
    try {
      const details = await fetchQuestionDetails(token, card.questionId);
      setAnswer(details.answer);
      setFollowup(details.followup);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  const rate = async (rating: Rating) => {
    const token = getToken();
    if (!token || !card || rated) return;
    setRated(true);
    try {
      await sendAnswer(token, card.questionId, rating);
      setSessionAnswered((n) => n + 1);
      const next = index + 1;
      setAnswer(null);
      setFollowup(null);
      setRated(false);
      if (next < queue.length) {
        setIndex(next);
        setPhase("question");
      } else {
        setPhase("done");
        const s = await fetchSummary(token);
        setSummary(s);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
      setRated(false);
    }
  };

  if (phase === "loading") {
    return <p className="py-10 text-center text-ink-600 dark:text-brand-50">Собираем карточки…</p>;
  }

  if (phase === "unauthorized") {
    return (
      <div className="mx-auto max-w-md space-y-4 py-10 text-center">
        <h1 className="text-2xl font-semibold tracking-tight">Практика</h1>
        <p className="text-ink-600 dark:text-brand-50">Войдите через Telegram, чтобы тренироваться.</p>
        <a href="/login" className="inline-block rounded-lg bg-brand-600 px-5 py-2.5 font-medium text-white hover:bg-brand-700">
          Войти
        </a>
      </div>
    );
  }

  if (phase === "error") {
    return (
      <div className="mx-auto max-w-md space-y-4 py-10 text-center">
        <p className="text-sm text-red-600">{error}</p>
        <a href="/practice" className="text-sm underline">Попробовать снова</a>
      </div>
    );
  }

  if (phase === "done") {
    return (
      <div className="mx-auto max-w-md space-y-4 py-14 text-center">
        <h1 className="text-2xl font-semibold tracking-tight">Сессия закрыта 🎉</h1>
        <p className="text-ink-600 dark:text-brand-50">
          Ответов за сессию: {sessionAnswered}. Всего ответов: {summary?.answeredTotal ?? "—"}.
        </p>
        <p className="text-sm text-ink-600 dark:text-brand-50">
          Следующие карточки появятся, когда подойдут по расписанию повторений.
        </p>
        <a href="/practice" className="inline-block rounded-lg bg-brand-600 px-5 py-2.5 font-medium text-white hover:bg-brand-700">
          Ещё сессия
        </a>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div className="flex items-center justify-between text-sm text-ink-600 dark:text-brand-50">
        <span>Карточка {index + 1} из {queue.length}</span>
        <span className="rounded-full border border-black/10 px-3 py-0.5 text-xs dark:border-white/15">
          {card.state === "NEW" ? "новая" : "повторение"} · {card.category}
        </span>
      </div>

      <div className="rounded-xl border border-black/10 p-6 dark:border-white/15">
        <p className="mb-2 text-xs uppercase tracking-wide text-ink-600 dark:text-brand-50">{card.difficulty}</p>
        <h2 className="text-lg font-medium leading-relaxed">{card.body}</h2>
      </div>

      {phase === "question" ? (
        <button
          type="button"
          onClick={showAnswer}
          className="w-full rounded-lg bg-brand-600 px-5 py-3 font-medium text-white hover:bg-brand-700"
        >
          Показать ответ
        </button>
      ) : (
        <div className="space-y-6">
          <div className="rounded-xl border border-black/10 bg-black/[0.02] p-6 dark:border-white/15 dark:bg-white/[0.03]">
            <p className="mb-2 text-xs uppercase tracking-wide text-ink-600 dark:text-brand-50">Эталонный ответ</p>
            <p className="whitespace-pre-wrap leading-relaxed">{answer}</p>
            {followup && (
              <div className="mt-4 border-t border-black/10 pt-4 text-sm dark:border-white/15">
                <span className="font-medium">Уточняющий вопрос: </span>
                <span className="text-ink-600 dark:text-brand-50">{followup}</span>
              </div>
            )}
          </div>

          <div>
            <p className="mb-2 text-center text-sm text-ink-600 dark:text-brand-50">Как ты оцениваешь свой ответ?</p>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              {RATING_LABELS.map(({ rating, label, hint, className }) => (
                <button
                  key={rating}
                  type="button"
                  disabled={rated}
                  onClick={() => rate(rating)}
                  className={`rounded-lg px-3 py-2.5 text-white disabled:opacity-50 ${className}`}
                >
                  <span className="block text-sm font-medium">{label}</span>
                  <span className="block text-xs opacity-80">{hint}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
