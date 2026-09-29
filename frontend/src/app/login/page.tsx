"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { BOT_USERNAME, loginWithTelegram, setSession } from "@/lib/auth";
import type { UserView } from "@/lib/auth";

interface TelegramWidgetUser {
  id: number;
  first_name: string;
  last_name?: string;
  username?: string;
  photo_url?: string;
  auth_date: number;
  hash: string;
}

const CALLBACK_NAME = "sobesOnTelegramAuth";

export default function LoginPage() {
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [done, setDone] = useState<UserView | null>(null);
  const widgetRef = useRef<HTMLDivElement>(null);

  const handleAuth = useCallback(async (u: TelegramWidgetUser) => {
    setPending(true);
    setError(null);
    try {
      const session = await loginWithTelegram({
        id: u.id,
        first_name: u.first_name,
        last_name: u.last_name ?? "",
        username: u.username ?? "",
        photo_url: u.photo_url ?? "",
        auth_date: u.auth_date,
        hash: u.hash,
      });
      setSession(session);
      setDone(session.user);
      setTimeout(() => {
        window.location.href = "/";
      }, 800);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
      setPending(false);
    }
  }, []);

  useEffect(() => {
    (window as unknown as Record<string, unknown>)[CALLBACK_NAME] = (u: TelegramWidgetUser) => void handleAuth(u);
    return () => {
      delete (window as unknown as Record<string, unknown>)[CALLBACK_NAME];
    };
  }, [handleAuth]);

  useEffect(() => {
    if (!widgetRef.current || done) return;
    const script = document.createElement("script");
    script.src = "https://telegram.org/js/telegram-widget.js?22";
    script.async = true;
    script.setAttribute("data-telegram-login", BOT_USERNAME);
    script.setAttribute("data-size", "large");
    script.setAttribute("data-userpic", "false");
    script.setAttribute("data-request-access", "write");
    script.setAttribute("data-onauth", `${CALLBACK_NAME}(user)`);
    widgetRef.current.appendChild(script);
  }, [done]);

  return (
    <div className="mx-auto max-w-md space-y-6 py-10 text-center">
      <h1 className="text-2xl font-semibold tracking-tight">Вход через Telegram</h1>
      <p className="text-ink-600 dark:text-brand-50">
        Без паролей и почты: аккаунт создаётся автоматически при первом входе.
      </p>

      {!BOT_USERNAME && (
        <p className="rounded-lg border border-black/10 p-4 text-sm text-ink-600 dark:border-white/15 dark:text-brand-50">
          Вход скоро будет открыт — бот ещё не подключён. Следите за обновлениями.
        </p>
      )}

      {BOT_USERNAME && !done && (
        <div className="flex justify-center">
          <div ref={widgetRef} className="inline-block min-h-10" />
        </div>
      )}

      {pending && !done && <p className="text-sm text-ink-600 dark:text-brand-50">Входим…</p>}
      {done && (
        <p className="text-sm font-medium text-brand-600">
          Привет, {done.displayName}! Перенаправляем на главную…
        </p>
      )}
      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  );
}
