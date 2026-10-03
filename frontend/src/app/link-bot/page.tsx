"use client";

import { useEffect, useState } from "react";
import { getToken } from "@/lib/auth";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type Phase = "loading" | "ok" | "unauthorized" | "error";

export default function LinkBotPage() {
  const [phase, setPhase] = useState<Phase>("loading");
  const [message, setMessage] = useState<string>("");

  useEffect(() => {
    const token = getToken();
    const urlToken = new URLSearchParams(window.location.search).get("token");
    if (!urlToken) {
      setPhase("error");
      setMessage("Ссылка без токена — запросите новую в боте.");
      return;
    }
    if (!token) {
      // Не залогинен: после входа вернём на эту же страницу с токеном.
      window.location.href = `/login?next=/link-bot?token=${encodeURIComponent(urlToken)}`;
      return;
    }
    fetch(`${API_URL}/api/v1/bot-link/confirm`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ token: urlToken }),
    }).then(async (r) => {
      if (r.status === 401) {
        window.location.href = `/login?next=/link-bot?token=${encodeURIComponent(urlToken)}`;
        return;
      }
      if (r.ok) {
        setPhase("ok");
        return;
      }
      const body = await r.json().catch(() => ({}));
      setPhase("error");
      setMessage(body.error ?? `Привязка не удалась (${r.status})`);
    }).catch((e) => {
      setPhase("error");
      setMessage(e instanceof Error ? e.message : String(e));
    });
  }, []);

  return (
    <div className="mx-auto max-w-md space-y-4 py-14 text-center">
      {phase === "loading" && <p className="text-ink-600 dark:text-brand-50">Связываем аккаунты…</p>}
      {phase === "ok" && (
        <>
          <h1 className="text-2xl font-semibold tracking-tight">Готово! 🔗</h1>
          <p className="text-ink-600 dark:text-brand-50">
            Бот теперь знает твой аккаунт. Возвращайся в Telegram — скоро там появятся вопрос дня и напоминания.
          </p>
        </>
      )}
      {phase === "error" && (
        <>
          <h1 className="text-2xl font-semibold tracking-tight">Не получилось</h1>
          <p className="text-sm text-red-600">{message}</p>
          <a href="/login" className="text-sm underline">Войти заново</a>
        </>
      )}
    </div>
  );
}
