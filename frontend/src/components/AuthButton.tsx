"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { clearSession, fetchMe, getToken } from "@/lib/auth";
import type { UserView } from "@/lib/auth";

/** Индикатор сессии для хедера: «…» / «Войти» / «Имя · выйти». */
export function AuthButton() {
  const router = useRouter();
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
      <button
        type="button"
        onClick={() => {
          clearSession();
          setUser(null);
          router.refresh();
        }}
        className="text-sm font-medium text-ink-600 hover:text-brand-600 dark:text-brand-50"
        title="Выйти"
      >
        {user.displayName} · выйти
      </button>
    );
  }

  return (
    <a href="/login" className="text-sm font-medium hover:text-brand-600">
      Войти
    </a>
  );
}
