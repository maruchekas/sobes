import type { Metadata } from "next";
import Link from "next/link";
import { AuthButton } from "@/components/AuthButton";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Sobes — подготовка к техническим собеседованиям",
    template: "%s · Sobes",
  },
  description:
    "Банк вопросов по Java, Kotlin и Spring с эталонными ответами, интервальные повторения и аналитика слабых тем.",
  openGraph: {
    type: "website",
    locale: "ru_RU",
    siteName: "Sobes",
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ru">
      <body>
        <header className="border-b border-black/5 dark:border-white/10">
          <nav className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4">
            <Link href="/" className="text-lg font-semibold tracking-tight">
              Sobes
            </Link>
            <div className="flex items-center gap-6 text-sm">
              <Link href="/questions" className="hover:text-brand-600">
                Вопросы
              </Link>
              <Link href="/practice" className="font-medium text-brand-600 hover:text-brand-700">
                Практика
              </Link>
              <AuthButton />
              <a
                href="https://github.com/maruchekas/sobes"
                className="hover:text-brand-600"
                rel="noreferrer"
              >
                GitHub
              </a>
            </div>
          </nav>
        </header>
        <main className="mx-auto max-w-5xl px-4 py-10">{children}</main>
        <footer className="border-t border-black/5 py-6 text-center text-sm text-ink-600 dark:border-white/10">
          Sobes — открытый проект подготовки к собеседованиям
        </footer>
      </body>
    </html>
  );
}
