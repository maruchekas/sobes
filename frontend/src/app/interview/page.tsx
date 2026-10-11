"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { interviewApi, ReportDto, SessionStateDto } from "@/lib/interview";
import { fetchCategories } from "@/lib/api";

const RATING_BUTTONS = [
  { key: "AGAIN", label: "Не помню", emoji: "🔁" },
  { key: "HARD", label: "Трудно", emoji: "😣" },
  { key: "GOOD", label: "Норм", emoji: "🙂" },
  { key: "EASY", label: "Легко", emoji: "😀" },
];

export default function InterviewPage() {
  const [phase, setPhase] = useState<"setup" | "question" | "report">("setup");
  const [categories, setCategories] = useState<{ slug: string; title: string }[]>([]);
  const [selected, setSelected] = useState<string[]>([]);
  const [total, setTotal] = useState(5);
  const [state, setState] = useState<SessionStateDto | null>(null);
  const [answerText, setAnswerText] = useState("");
  const [showReference, setShowReference] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(180);
  const [report, setReport] = useState<ReportDto | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [startedAt, setStartedAt] = useState(Date.now());

  useEffect(() => {
    fetchCategories().then(setCategories).catch(() => setCategories([]));
  }, []);

  // таймер на вопрос
  useEffect(() => {
    if (phase !== "question") return;
    setSecondsLeft(state?.question?.secondsLimit ?? 180);
    setStartedAt(Date.now());
    const timer = setInterval(() => {
      setSecondsLeft((s) => {
        if (s <= 1) {
          clearInterval(timer);
          return 0;
        }
        return s - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [phase, state?.question?.questionId]);

  // автопереход при 0
  useEffect(() => {
    if (phase === "question" && secondsLeft === 0 && !busy) {
      submit("AGAIN", true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [secondsLeft]);

  const minutes = Math.floor(secondsLeft / 60);
  const seconds = String(secondsLeft % 60).padStart(2, "0");

  async function start() {
    setBusy(true);
    setError(null);
    try {
      const s = await interviewApi.start(selected, total);
      setState(s);
      setAnswerText("");
      setShowReference(false);
      setPhase(s.question ? "question" : "report");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось начать интервью");
    } finally {
      setBusy(false);
    }
  }

  async function submit(rating: string, timedOut = false) {
    if (!state || busy) return;
    setBusy(true);
    try {
      const spent = Math.min(
        Math.round((Date.now() - startedAt) / 1000),
        (state.question?.secondsLimit ?? 180) + 60
      );
      const next = await interviewApi.answer(
        state.sessionId,
        state.question!.questionId,
        timedOut ? answerText : answerText,
        rating,
        spent
      );
      if (next.question) {
        setState(next);
        setAnswerText("");
        setShowReference(false);
        setPhase("question");
      } else {
        const rep = await interviewApi.finish(next.sessionId);
        setReport(rep);
        setPhase("report");
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Ошибка отправки");
    } finally {
      setBusy(false);
    }
  }

  async function finishEarly() {
    if (!state) return;
    setBusy(true);
    try {
      const rep = await interviewApi.finish(state.sessionId);
      setReport(rep);
      setPhase("report");
    } finally {
      setBusy(false);
    }
  }

  if (phase === "setup") {
    return (
      <div className="mx-auto max-w-2xl space-y-6">
        <h1 className="text-2xl font-semibold tracking-tight">Мок-интервью</h1>
        <p className="text-ink-600 dark:text-brand-50">
          Сессия из N вопросов с таймером 3 минуты на каждый. Отвечай своими словами, сверяйся с эталоном
          и оценивай себя честно — оценка уходит в интервалы повторений.
        </p>
        <div className="space-y-2">
          <h2 className="text-sm font-medium uppercase tracking-wide text-brand-600">Категории</h2>
          <div className="flex flex-wrap gap-2">
            {categories.map((c) => {
              const on = selected.includes(c.slug);
              return (
                <button
                  key={c.slug}
                  onClick={() => setSelected((s) => (on ? s.filter((x) => x !== c.slug) : [...s, c.slug]))}
                  className={`rounded-full px-3 py-1 text-sm border transition ${
                    on
                      ? "bg-brand-600 text-white border-brand-600"
                      : "border-black/15 dark:border-white/20 hover:border-brand-400"
                  }`}
                >
                  {c.title}
                </button>
              );
            })}
          </div>
          <p className="text-xs text-ink-500">
            {selected.length === 0 ? "Не выбрано — вопросы из всех категорий" : `Выбрано: ${selected.length}`}
          </p>
        </div>
        <div className="space-y-2">
          <h2 className="text-sm font-medium uppercase tracking-wide text-brand-600">Сколько вопросов</h2>
          <div className="flex gap-2">
            {[5, 10, 15].map((n) => (
              <button
                key={n}
                onClick={() => setTotal(n)}
                className={`rounded-lg px-4 py-2 text-sm border transition ${
                  total === n
                    ? "bg-brand-600 text-white border-brand-600"
                    : "border-black/15 dark:border-white/20 hover:border-brand-400"
                }`}
              >
                {n}
              </button>
            ))}
          </div>
        </div>
        {error && <p className="text-sm text-red-500">{error}</p>}
        <button
          onClick={start}
          disabled={busy}
          className="rounded-lg bg-brand-600 px-5 py-2.5 font-medium text-white hover:bg-brand-500 disabled:opacity-50"
        >
          {busy ? "Готовлю вопросы..." : "Начать интервью"}
        </button>
      </div>
    );
  }

  if (phase === "question" && state?.question) {
    const q = state.question;
    return (
      <div className="mx-auto max-w-3xl space-y-5">
        <div className="flex items-center justify-between">
          <span className="text-sm text-ink-600 dark:text-brand-50">
            Вопрос {q.index} / {q.total}
          </span>
          <span
            className={`rounded-lg px-3 py-1 font-mono text-lg ${
              secondsLeft <= 30 ? "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300" : "bg-brand-50 text-brand-700 dark:bg-brand-900/40"
            }`}
          >
            {minutes}:{seconds}
          </span>
        </div>
        <div className="h-1 rounded bg-black/10 dark:bg-white/10">
          <div
            className="h-1 rounded bg-brand-500 transition-all"
            style={{ width: `${(secondsLeft / q.secondsLimit) * 100}%` }}
          />
        </div>
        <div className="rounded-xl border border-black/10 p-5 dark:border-white/15">
          <div className="mb-2 text-xs uppercase tracking-wide text-brand-600">
            {q.category} · {q.difficulty}
          </div>
          <h1 className="text-xl font-semibold">{q.body}</h1>
        </div>
        <textarea
          value={answerText}
          onChange={(e) => setAnswerText(e.target.value)}
          placeholder="Твой ответ своими словами (необязательно, но полезно)..."
          className="min-h-32 w-full rounded-xl border border-black/10 bg-transparent p-4 text-sm outline-none focus:border-brand-400 dark:border-white/15"
        />
        {showReference ? (
          <div className="rounded-xl border border-dashed border-black/15 p-4 text-sm dark:border-white/20">
            <h3 className="mb-2 text-xs font-medium uppercase tracking-wide text-brand-600">Эталонный ответ</h3>
            <p className="text-ink-500">
              Эталон открывается в отчёте после самооценки — сначала ответь сам.
            </p>
          </div>
        ) : null}
        <div className="flex flex-wrap gap-2">
          {RATING_BUTTONS.map((b) => (
            <button
              key={b.key}
              onClick={() => submit(b.key)}
              disabled={busy}
              className="rounded-lg border border-black/15 px-4 py-2 text-sm transition hover:border-brand-400 hover:bg-brand-50 dark:border-white/20 dark:hover:bg-brand-900/30 disabled:opacity-50"
            >
              {b.emoji} {b.label}
            </button>
          ))}
        </div>
        <div className="flex justify-between text-sm">
          <button
            onClick={() => setShowReference((s) => !s)}
            className="text-brand-600 hover:underline"
          >
            {showReference ? "Скрыть эталон" : "Показать эталон (после ответа!)"}
          </button>
          <button onClick={finishEarly} className="text-ink-500 hover:underline">
            Завершить досрочно
          </button>
        </div>
        {error && <p className="text-sm text-red-500">{error}</p>}
      </div>
    );
  }

  if (phase === "report" && report) {
    return (
      <div className="mx-auto max-w-3xl space-y-6">
        <h1 className="text-2xl font-semibold tracking-tight">Отчёт по интервью</h1>
        <div className="grid grid-cols-3 gap-3 text-center">
          <div className="rounded-xl border border-black/10 p-4 dark:border-white/15">
            <div className="text-2xl font-bold text-brand-600">{report.answeredCount}</div>
            <div className="text-xs text-ink-500">ответов</div>
          </div>
          <div className="rounded-xl border border-black/10 p-4 dark:border-white/15">
            <div className="text-2xl font-bold text-brand-600">
              {Math.round(report.totalSeconds / 60)} мин
            </div>
            <div className="text-xs text-ink-500">всего времени</div>
          </div>
          <div className="rounded-xl border border-black/10 p-4 dark:border-white/15">
            <div className="text-2xl font-bold text-brand-600">
              {(report.byRating.EASY ?? 0) + (report.byRating.GOOD ?? 0)}
            </div>
            <div className="text-xs text-ink-500">уверенных ответов</div>
          </div>
        </div>
        {report.weakCategories.length > 0 && (
          <p className="rounded-xl bg-amber-50 p-4 text-sm text-amber-900 dark:bg-amber-900/20 dark:text-amber-200">
            Слабые темы этой сессии: {report.weakCategories.join(", ")} — добавь их в практику.
          </p>
        )}
        <div className="space-y-3">
          {report.items.map((it) => (
            <details key={it.questionId} className="rounded-xl border border-black/10 p-4 dark:border-white/15">
              <summary className="cursor-pointer text-sm font-medium">
                <span className="mr-2 rounded-full bg-brand-50 px-2 py-0.5 text-xs text-brand-700 dark:bg-brand-900/40">
                  {it.rating}
                </span>
                {it.body.slice(0, 80)}...
              </summary>
              <div className="mt-3 space-y-2 text-sm">
                <p className="text-ink-500">
                  {it.category} · {it.difficulty} · {it.secondsSpent}с · твой ответ: {it.userTextLength} симв.
                </p>
                <div>
                  <h4 className="mb-1 text-xs font-medium uppercase tracking-wide text-brand-600">Эталон</h4>
                  <div className="prose-answer">{it.referenceAnswer}</div>
                </div>
                {it.followup && (
                  <div>
                    <h4 className="mb-1 text-xs font-medium uppercase tracking-wide text-brand-600">
                      Уточняющий вопрос (для самопроверки)
                    </h4>
                    <div className="prose-answer">{it.followup}</div>
                  </div>
                )}
              </div>
            </details>
          ))}
        </div>
        <div className="flex gap-3">
          <button
            onClick={() => {
              setPhase("setup");
              setReport(null);
            }}
            className="rounded-lg bg-brand-600 px-5 py-2.5 font-medium text-white hover:bg-brand-500"
          >
            Ещё интервью
          </button>
          <Link href="/dashboard" className="rounded-lg border border-black/15 px-5 py-2.5 dark:border-white/20">
            На дашборд
          </Link>
        </div>
      </div>
    );
  }

  return null;
}
