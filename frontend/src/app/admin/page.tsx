"use client";

import { useCallback, useEffect, useState } from "react";
import { getToken } from "@/lib/auth";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

interface QuestionRow {
  id: number;
  categorySlug: string;
  categoryTitle: string;
  difficulty: string;
  body: string;
  answer: string;
  followup: string | null;
}

interface CategoryRow {
  slug: string;
  title: string;
  sortOrder: number;
}

type Form = {
  id: number | null;
  categorySlug: string;
  difficulty: string;
  body: string;
  answer: string;
  followup: string;
};

const EMPTY: Form = { id: null, categorySlug: "", difficulty: "MIDDLE", body: "", answer: "", followup: "" };

async function api<T>(method: string, path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${getToken()}`,
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 403) throw new Error("Недостаточно прав: /admin только для админов (ADMIN_TELEGRAM_IDS)");
  if (!res.ok) throw new Error(`${res.status}`);
  return (await res.json()) as T;
}

export default function AdminPage() {
  const [questions, setQuestions] = useState<QuestionRow[]>([]);
  const [categories, setCategories] = useState<CategoryRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [form, setForm] = useState<Form>(EMPTY);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);

  const load = useCallback(async () => {
    try {
      const q = await api<{ content: QuestionRow[]; totalElements: number }>(
        "GET",
        `/api/v1/admin/questions?page=${page}&size=20${search ? `&search=${encodeURIComponent(search)}` : ""}`
      );
      setQuestions(q.content);
      setTotal(q.totalElements);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "ошибка загрузки");
    }
  }, [page, search]);

  useEffect(() => {
    load();
    api<CategoryRow[]>("GET", "/api/v1/admin/categories").then(setCategories).catch(() => {});
  }, [load]);

  async function save() {
    try {
      await api("POST", "/api/v1/admin/questions", {
        id: form.id,
        categorySlug: form.categorySlug || categories[0]?.slug,
        difficulty: form.difficulty,
        body: form.body,
        answer: form.answer,
        followup: form.followup || null,
      });
      setNotice(form.id ? "Вопрос обновлён" : "Вопрос создан");
      setForm(EMPTY);
      setEditing(false);
      load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "ошибка сохранения");
    }
  }

  async function remove(id: number) {
    if (!confirm("Удалить вопрос?")) return;
    try {
      await api("DELETE", `/api/v1/admin/questions/${id}`);
      setNotice("Вопрос удалён");
      load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "ошибка удаления");
    }
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Админка банка вопросов</h1>
        <span className="text-sm text-ink-500">{total} вопросов</span>
      </div>

      {error && <p className="rounded-lg bg-red-100 p-3 text-sm text-red-700 dark:bg-red-900/30 dark:text-red-300">{error}</p>}
      {notice && <p className="rounded-lg bg-emerald-100 p-3 text-sm text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300">{notice}</p>}

      {/* форма создания/редактирования */}
      {editing ? (
        <div className="space-y-3 rounded-xl border border-black/10 p-5 dark:border-white/15">
          <h2 className="text-sm font-medium uppercase tracking-wide text-brand-600">
            {form.id ? `Правка вопроса #${form.id}` : "Новый вопрос"}
          </h2>
          <div className="flex flex-wrap gap-3">
            <select
              value={form.categorySlug}
              onChange={(e) => setForm({ ...form, categorySlug: e.target.value })}
              className="rounded-lg border border-black/15 bg-transparent px-3 py-2 text-sm dark:border-white/20"
            >
              <option value="">Категория…</option>
              {categories.map((c) => (
                <option key={c.slug} value={c.slug}>{c.title}</option>
              ))}
            </select>
            <select
              value={form.difficulty}
              onChange={(e) => setForm({ ...form, difficulty: e.target.value })}
              className="rounded-lg border border-black/15 bg-transparent px-3 py-2 text-sm dark:border-white/20"
            >
              <option value="JUNIOR">Junior</option>
              <option value="MIDDLE">Middle</option>
              <option value="SENIOR">Senior</option>
            </select>
          </div>
          <textarea
            value={form.body}
            onChange={(e) => setForm({ ...form, body: e.target.value })}
            placeholder="Формулировка вопроса"
            className="min-h-16 w-full rounded-lg border border-black/15 bg-transparent p-3 text-sm dark:border-white/20"
          />
          <textarea
            value={form.answer}
            onChange={(e) => setForm({ ...form, answer: e.target.value })}
            placeholder="Эталонный ответ"
            className="min-h-32 w-full rounded-lg border border-black/15 bg-transparent p-3 text-sm dark:border-white/20"
          />
          <textarea
            value={form.followup}
            onChange={(e) => setForm({ ...form, followup: e.target.value })}
            placeholder="Уточняющий вопрос (опционально)"
            className="min-h-12 w-full rounded-lg border border-black/15 bg-transparent p-3 text-sm dark:border-white/20"
          />
          <div className="flex gap-2">
            <button onClick={save} className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-500">
              Сохранить
            </button>
            <button
              onClick={() => { setEditing(false); setForm(EMPTY); }}
              className="rounded-lg border border-black/15 px-4 py-2 text-sm dark:border-white/20"
            >
              Отмена
            </button>
          </div>
        </div>
      ) : (
        <button
          onClick={() => { setEditing(true); setForm(EMPTY); }}
          className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-500"
        >
          + Новый вопрос
        </button>
      )}

      {/* поиск и пагинация */}
      <div className="flex items-center gap-3">
        <input
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          placeholder="Поиск по тексту вопроса…"
          className="flex-1 rounded-lg border border-black/15 bg-transparent px-3 py-2 text-sm dark:border-white/20"
        />
        <button
          onClick={() => { setPage((p) => Math.max(0, p - 1)); }}
          disabled={page === 0}
          className="rounded-lg border border-black/15 px-3 py-2 text-sm disabled:opacity-40 dark:border-white/20"
        >
          ←
        </button>
        <span className="text-sm text-ink-500">стр. {page + 1}</span>
        <button
          onClick={() => setPage((p) => p + 1)}
          disabled={(page + 1) * 20 >= total}
          className="rounded-lg border border-black/15 px-3 py-2 text-sm disabled:opacity-40 dark:border-white/20"
        >
          →
        </button>
      </div>

      {/* список */}
      <div className="space-y-2">
        {questions.map((q) => (
          <div key={q.id} className="flex items-start gap-3 rounded-xl border border-black/10 p-4 dark:border-white/15">
            <div className="flex-1">
              <div className="mb-1 flex flex-wrap items-center gap-2 text-xs">
                <span className="rounded-full bg-brand-50 px-2 py-0.5 text-brand-700 dark:bg-brand-900/40">{q.categoryTitle}</span>
                <span className="text-ink-500">{q.difficulty}</span>
              </div>
              <p className="text-sm">{q.body.slice(0, 140)}{q.body.length > 140 ? "…" : ""}</p>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => {
                  setForm({
                    id: q.id, categorySlug: q.categorySlug, difficulty: q.difficulty,
                    body: q.body, answer: q.answer, followup: q.followup ?? "",
                  });
                  setEditing(true);
                  window.scrollTo({ top: 0, behavior: "smooth" });
                }}
                className="rounded-lg border border-black/15 px-3 py-1.5 text-xs hover:border-brand-400 dark:border-white/20"
              >
                Править
              </button>
              <button
                onClick={() => remove(q.id)}
                className="rounded-lg border border-red-300 px-3 py-1.5 text-xs text-red-600 hover:bg-red-50 dark:border-red-800 dark:hover:bg-red-900/20"
              >
                Удалить
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
