import type { Metadata } from "next";
import Link from "next/link";
import { fetchCategories, fetchQuestions } from "@/lib/api";
import { DIFFICULTY_LABELS, type Difficulty } from "@/lib/types";

export const metadata: Metadata = {
  title: "Каталог вопросов",
  description:
    "Вопросы для собеседований по Java, Kotlin, Spring, Kafka и базам данных с эталонными ответами.",
};

const DIFFICULTIES: Difficulty[] = ["JUNIOR", "MIDDLE", "SENIOR"];

export default async function QuestionsPage({
  searchParams,
}: {
  searchParams: Promise<{ category?: string; difficulty?: Difficulty; page?: string }>;
}) {
  const params = await searchParams;
  const page = Number(params.page ?? "0");

  const [categories, questions] = await Promise.all([
    fetchCategories().catch(() => []),
    fetchQuestions({
      category: params.category,
      difficulty: params.difficulty,
      page: Number.isNaN(page) ? 0 : page,
      size: 20,
    }).catch(() => null),
  ]);

  return (
    <div className="space-y-8">
      <header className="space-y-2">
        <h1 className="text-3xl font-semibold tracking-tight">Каталог вопросов</h1>
        {questions ? (
          <p className="text-ink-600 dark:text-brand-50">Найдено вопросов: {questions.totalElements}</p>
        ) : (
          <p className="text-ink-600">Бэкенд недоступен — запустите его локально, чтобы увидеть вопросы.</p>
        )}
      </header>

      <nav className="space-y-3">
        <div className="flex flex-wrap gap-2">
          <Link
            href="/questions"
            className={`rounded-full border px-4 py-1.5 text-sm ${
              params.difficulty ? "border-black/10 dark:border-white/15" : "border-brand-500 text-brand-600"
            }`}
          >
            Все уровни
          </Link>
          {DIFFICULTIES.map((difficulty) => (
            <Link
              key={difficulty}
              href={`/questions?difficulty=${difficulty}${params.category ? `&category=${params.category}` : ""}`}
              className={`rounded-full border px-4 py-1.5 text-sm ${
                params.difficulty === difficulty
                  ? "border-brand-500 text-brand-600"
                  : "border-black/10 dark:border-white/15"
              }`}
            >
              {DIFFICULTY_LABELS[difficulty]}
            </Link>
          ))}
        </div>
        {categories.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {categories.map((category) => (
              <Link
                key={category.slug}
                href={`/questions?category=${category.slug}`}
                className={`rounded-full border px-4 py-1.5 text-sm ${
                  params.category === category.slug
                    ? "border-brand-500 text-brand-600"
                    : "border-black/10 dark:border-white/15"
                }`}
              >
                {category.title}
              </Link>
            ))}
          </div>
        )}
      </nav>

      <ol className="space-y-3">
        {questions?.content.map((question, index) => (
          <li key={question.id}>
            <Link
              href={`/questions/${question.id}`}
              className="block rounded-xl border border-black/10 p-4 hover:border-brand-500 dark:border-white/15"
            >
              <div className="mb-1 flex items-center gap-3 text-xs text-ink-600 dark:text-brand-50">
                <span>#{index + 1 + (questions.number * questions.size)}</span>
                <span>{question.category}</span>
                <span className="rounded-full bg-brand-50 px-2 py-0.5 text-brand-700">
                  {DIFFICULTY_LABELS[question.difficulty]}
                </span>
              </div>
              <p className="font-medium">{question.body}</p>
            </Link>
          </li>
        ))}
      </ol>

      {questions && questions.totalPages > 1 && (
        <nav className="flex justify-between">
          {questions.number > 0 ? (
            <Link
              href={`/questions?page=${questions.number - 1}${params.category ? `&category=${params.category}` : ""}`}
              className="rounded-lg border border-black/10 px-4 py-2 text-sm dark:border-white/15"
            >
              Назад
            </Link>
          ) : (
            <span />
          )}
          {questions.number + 1 < questions.totalPages && (
            <Link
              href={`/questions?page=${questions.number + 1}${params.category ? `&category=${params.category}` : ""}`}
              className="rounded-lg border border-black/10 px-4 py-2 text-sm dark:border-white/15"
            >
              Вперёд
            </Link>
          )}
        </nav>
      )}
    </div>
  );
}
