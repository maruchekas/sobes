"use client";

import { useEffect, useState } from "react";
import { clearSession, fetchMe, getToken } from "@/lib/auth";
import type { UserView } from "@/lib/auth";

/** Индикатор сессии для хедера: «…» / «Войти» / имя-ссылка в кабинет. */
export function AuthButton() {
  const [user, setUser] = useState<UserView | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      setLoading(false);
      return;
    }
    fetchMe(token)
      .then(setUser)
      .catch(() => {
        clearSession();
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <span className="text-sm text-ink-600 dark:text-brand-50">…</span>;

  if (user) {
    return (
      <a href="/me" className="text-sm font-medium text-ink-600 hover:text-brand-600 dark:text-brand-50" title="Личный кабинет">
        {user.displayName}
      </a>
    );
  }

  return (
    <a href="/login" className="text-sm font-medium hover:text-brand-600">
      Войти
    </a>
  );
}
