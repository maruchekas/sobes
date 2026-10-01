"use client";

import { useRouter } from "next/navigation";
import { getToken } from "@/lib/auth";

/** Кнопка выхода для кабинета (в хедере имя теперь ведёт в /me). */
export function LogoutButton() {
  const router = useRouter();
  return (
    <button
      type="button"
      onClick={() => {
        localStorage.removeItem("sobes_token");
        router.replace("/");
        router.refresh();
      }}
      className="text-sm text-ink-600 underline hover:text-brand-600 dark:text-brand-50"
    >
      Выйти
    </button>
  );
}
