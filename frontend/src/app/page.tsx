import Link from "next/link";
import { fetchCategories } from "@/lib/api";

export const revalidate = 300;

export default async function HomePage() {
  const categories = await fetchCategories().catch(() => []);
  const total = categories.reduce((sum, category) => sum + category.questions, 0);

  return (
    <div className="space-y-14">
      <section className="space-y-5">
        <p className="text-sm font-medium text-brand-600">Открытая платформа подготовки</p>
        <h1 className="text-4xl font-semibold tracking-tight sm:text-5xl">
          Готовься к собеседованию системно, а не по случайным спискам
        </h1>
        <p className="max-w-2xl text-lg text-ink-600 dark:text-brand-50">
          {total} вопросов по Java, Kotlin, Spring, Kafka и базам данных с эталонными ответами.
          Скоро добавим интервальные повторения, мок-интервью и напоминания в Telegram.
        </p>
        <div className="flex gap-3">
          <Link
            href="/questions"
            className="rounded-lg bg-brand-600 px-5 py-2.5 font-medium text-white hover:bg-brand-700"
          >
            Открыть каталог вопросов
          </Link>
          <a
            href="https://github.com/maruchekas/sobes"
            className="rounded-lg border border-black/10 px-5 py-2.5 font-medium hover:bg-black/5 dark:border-white/15 dark:hover:bg-white/5"
          >
            Смотреть код
          </a>
        </div>
      </section>

      <section className="grid gap-6 sm:grid-cols-3">
        {[
          {
            title: "Банк с эталонами",
            text: "Каждый вопрос — с развёрнутым ответом и уточняющим вопросом, который любят задавать следом.",
          },
          {
            title: "Повторения по расписанию",
            text: "Алгоритм SM-2 подскажет, что повторить сегодня, чтобы знания не выветривались.",
          },
          {
            title: "Прогресс по темам",
            text: "Видно, где вы сильны, а какие темы стоит подтянуть перед собеседованием.",
          },
        ].map((feature) => (
          <article key={feature.title} className="rounded-xl border border-black/10 p-5 dark:border-white/15">
            <h2 className="mb-2 font-semibold">{feature.title}</h2>
            <p className="text-sm text-ink-600 dark:text-brand-50">{feature.text}</p>
          </article>
        ))}
      </section>

      <section className="space-y-4">
        <h2 className="text-2xl font-semibold tracking-tight">Темы</h2>
        {categories.length === 0 ? (
          <p className="text-ink-600">
            Каталог пока недоступен: бэкенд не отвечает. Запустите его командой{" "}
            <code>cd backend &amp;&amp; ./gradlew bootRun</code>.
          </p>
        ) : (
          <ul className="flex flex-wrap gap-2">
            {categories.map((category) => (
              <li key={category.slug}>
                <Link
                  href={`/questions?category=${category.slug}`}
                  className="inline-flex items-center gap-2 rounded-full border border-black/10 px-4 py-1.5 text-sm hover:border-brand-500 hover:text-brand-600 dark:border-white/15"
                >
                  {category.title}
                  <span className="text-xs text-ink-600 dark:text-brand-50">{category.questions}</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
