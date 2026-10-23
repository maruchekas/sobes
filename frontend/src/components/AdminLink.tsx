"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { fetchMe, getToken } from "@/lib/auth";

/** Ссылка «Админка» в шапке — рендерится только для роли ADMIN. */
export function AdminLink() {
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    const token = getToken();
    if (!token) return;
    fetchMe(token)
      .then((u) => setIsAdmin(u.role === "ADMIN"))
      .catch(() => setIsAdmin(false));
  }, []);

  if (!isAdmin) return null;

  return (
    <Link href="/admin" className="hover:text-brand-600">
      Админка
    </Link>
  );
}
