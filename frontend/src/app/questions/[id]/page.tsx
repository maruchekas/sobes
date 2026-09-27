import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { fetchQuestion } from "@/lib/api";
import { DIFFICULTY_LABELS } from "@/lib/types";

interface Params {
  params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { id } = await params;
  const question = await fetchQuestion(Number(id)).catch(() => null);

  if (!question) {
    return { title: "Вопрос не найден" };
  }

  return {
    title: question.body.slice(0, 70),
    description: question.answer.slice(0, 160),
  };
}

export default async function QuestionPage({ params }: Params) {
  const { id } = await params;
  const question = await fetchQuestion(Number(id)).catch(() => null);

  if (!question) {
    notFound();
  }

  return (
    <article className="space-y-6">
      <nav className="text-sm">
        <Link href="/questions" className="text-brand-600 hover:underline">
          ← Все вопросы
        </Link>
      </nav>

      <header className="space-y-3">
        <div className="flex flex-wrap items-center gap-3 text-sm text-ink-600 dark:text-brand-50">
          <Link href={`/questions?category=${question.categorySlug}`} className="hover:text-brand-600">
            {question.category}
          </Link>
          <span className="rounded-full bg-brand-50 px-2 py-0.5 text-xs text-brand-700">
            {DIFFICULTY_LABELS[question.difficulty]}
          </span>
        </div>
        <h1 className="text-2xl font-semibold tracking-tight">{question.body}</h1>
      </header>

      <section className="rounded-xl border border-black/10 p-5 dark:border-white/15">
        <h2 className="mb-3 text-sm font-medium uppercase tracking-wide text-brand-600">Эталонный ответ</h2>
        <div className="prose-answer">{question.answer}</div>
      </section>

      {question.followup && (
        <section className="rounded-xl border border-dashed border-black/15 p-5 dark:border-white/20">
          <h2 className="mb-3 text-sm font-medium uppercase tracking-wide text-ink-600 dark:text-brand-50">
            Уточняющий вопрос
          </h2>
          <p className="prose-answer">{question.followup}</p>
        </section>
      )}
    </article>
  );
}
