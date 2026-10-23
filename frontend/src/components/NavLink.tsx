"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

/** Пункт навигации с активной подсветкой по текущему пути. */
export function NavLink({ href, children }: { href: string; children: React.ReactNode }) {
  const pathname = usePathname();
  const active = href === "/" ? pathname === "/" : pathname.startsWith(href);
  return (
    <Link
      href={href}
      className={active ? "font-medium text-brand-600 dark:text-brand-400" : "hover:text-brand-600"}
    >
      {children}
    </Link>
  );
}
