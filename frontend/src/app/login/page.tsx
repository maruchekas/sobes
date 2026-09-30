"use client";

import { useCallback, useEffect, useState } from "react";
import { BOT_ID, loginWithTelegram, setSession } from "@/lib/auth";
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

interface TelegramLoginApi {
  auth(
    options: { bot_id: number; request_access?: "write" },
    callback: (user: TelegramWidgetUser | false) => void,
  ): void;
}

declare global {
  interface Window {
    Telegram?: { Login?: TelegramLoginApi };
  }
}

const TELEGRAM_SCRIPT_ID = "telegram-login-script";

export default function LoginPage() {
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [done, setDone] = useState<UserView | null>(null);
  const [widgetReady, setWidgetReady] = useState(false);

  const handleAuth = useCallback(async (u: TelegramWidgetUser) => {
    setPending(true);
    setError(null);
    try {
      // HMAC покрывает точный набор полей Telegram. Нельзя добавлять пустые
      // optional-поля или отбрасывать дополнительные поля вроде language_code.
      const widgetData = Object.fromEntries(
        Object.entries(u).filter(([, value]) => value !== undefined),
      ) as Record<string, string | number>;
      const session = await loginWithTelegram(widgetData);
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
    if (!BOT_ID) return;

    const handleLoad = () => setWidgetReady(typeof window.Telegram?.Login?.auth === "function");
    const handleError = () => setError("Не удалось загрузить авторизацию Telegram. Попробуйте ещё раз.");
    let script = document.getElementById(TELEGRAM_SCRIPT_ID) as HTMLScriptElement | null;

    if (!script) {
      script = document.createElement("script");
      script.id = TELEGRAM_SCRIPT_ID;
      script.src = "https://telegram.org/js/telegram-widget.js?22";
      script.async = true;
      document.head.appendChild(script);
    }

    script.addEventListener("load", handleLoad);
    script.addEventListener("error", handleError);
    handleLoad();

    return () => {
      script.removeEventListener("load", handleLoad);
      script.removeEventListener("error", handleError);
    };
  }, []);

  const openTelegramLogin = () => {
    if (!BOT_ID || pending) return;
    const auth = window.Telegram?.Login?.auth;
    if (!auth) {
      setError("Авторизация Telegram ещё не загрузилась. Попробуйте ещё раз.");
      return;
    }

    setError(null);
    setPending(true);
    try {
      auth({ bot_id: BOT_ID, request_access: "write" }, (user) => {
        if (!user) {
          setPending(false);
          setError("Вход через Telegram был отменён.");
          return;
        }
        void handleAuth(user);
      });
    } catch (e) {
      setPending(false);
      setError(e instanceof Error ? e.message : "Не удалось открыть Telegram.");
    }
  };

  return (
    <div className="mx-auto max-w-md space-y-6 py-10 text-center">
      <h1 className="text-2xl font-semibold tracking-tight">Вход через Telegram</h1>
      <p className="text-ink-600 dark:text-brand-50">
        Без паролей и почты: аккаунт создаётся автоматически при первом входе.
      </p>

      {!BOT_ID && (
        <p className="rounded-lg border border-black/10 p-4 text-sm text-ink-600 dark:border-white/15 dark:text-brand-50">
          Вход скоро будет открыт — не задан NEXT_PUBLIC_TELEGRAM_BOT_ID.
        </p>
      )}

      {BOT_ID && !done && (
        <button
          type="button"
          onClick={openTelegramLogin}
          disabled={!widgetReady || pending}
          className="mx-auto inline-flex min-h-10 items-center gap-2 rounded-full bg-[#2AABEE] px-6 py-2.5 text-base font-medium text-white shadow-sm transition hover:bg-[#229ED9] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#2AABEE] disabled:cursor-wait disabled:opacity-60"
        >
          <svg aria-hidden="true" viewBox="0 0 24 24" className="h-5 w-5 fill-current">
            <path d="M21.8 3.6 18.7 19c-.2 1.1-.9 1.4-1.8.9l-4.7-3.5-2.3 2.2c-.2.3-.5.5-.9.5l.3-4.8 8.8-8c.4-.3-.1-.5-.6-.2L6.6 13l-4.7-1.5c-1-.3-1-1 .2-1.5L20.4 3c.8-.3 1.6.2 1.4.6Z" />
          </svg>
          {pending ? "Ожидаем Telegram…" : widgetReady ? "Войти через Telegram" : "Загрузка…"}
        </button>
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
