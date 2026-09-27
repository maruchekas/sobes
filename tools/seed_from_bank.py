#!/usr/bin/env python3
"""Переносит банк вопросов (decks/interview/questions.json) в Flyway-миграцию V2.

Использование:
    python seed_from_bank.py SOURCE.json TARGET.sql
"""
from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path

# Русские названия категорий -> slug для URL и API.
SLUGS = {
    "Java Core": ("java-core", 10),
    "Многопоточность": ("concurrency", 20),
    "JVM": ("jvm", 30),
    "Kotlin": ("kotlin", 40),
    "Spring Core": ("spring-core", 50),
    "Spring Data / JPA / Hibernate": ("spring-data-jpa", 60),
    "Spring Security + Keycloak": ("spring-security", 70),
    "Spring AOP и транзакции": ("spring-aop-transactions", 80),
    "Микросервисы": ("microservices", 90),
    "Kafka": ("kafka", 100),
    "PostgreSQL": ("postgresql", 110),
    "Поведенческие": ("behavioral", 120),
    "Java 17/21": ("java-17-21", 15),
    "Stream API": ("stream-api", 35),
    "Web (Spring MVC)": ("spring-mvc", 85),
    "MongoDB": ("mongodb", 112),
    "Redis": ("redis", 114),
    "Миграции БД": ("db-migrations", 116),
    "Docker/Kubernetes": ("docker-kubernetes", 130),
    "Jenkins CI/CD": ("ci-cd", 135),
    "Мониторинг": ("monitoring", 140),
    "Тестирование": ("testing", 145),
}

DIFFICULTY = {"junior": "JUNIOR", "middle": "MIDDLE", "senior": "SENIOR"}
FALLBACK_SLUG = "other"


def slug_for(category: str) -> tuple[str, int]:
    return SLUGS.get(category, (FALLBACK_SLUG, 999))


def quote(value: str | None) -> str:
    if value is None or value.strip() == "":
        return "null"
    return "'" + value.replace("'", "''") + "'"


def main() -> int:
    source = Path(sys.argv[1])
    target = Path(sys.argv[2])
    items = json.loads(source.read_text(encoding="utf-8"))

    categories: dict[str, tuple[str, int]] = {}
    unknown = Counter()
    normalized: list[tuple[str, str, str, str, str | None]] = []

    for item in items:
        category = (item.get("category") or "").strip()
        if category not in SLUGS:
            unknown[category] += 1
        slug, order = slug_for(category)
        categories[category] = (slug, order)

        difficulty = DIFFICULTY.get((item.get("difficulty") or "").strip().lower(), "MIDDLE")
        normalized.append(
            (
                slug,
                difficulty,
                (item.get("question") or "").strip(),
                (item.get("answer") or "").strip(),
                (item.get("followup") or "").strip() or None,
            )
        )

    lines = [
        "-- V2: стартовый банк вопросов, перенесённый из сборника (112 вопросов).",
        "-- Сгенерировано скриптом scripts/seed_from_bank.py, правится через админку платформы.",
        "",
    ]

    for category, (slug, order) in categories.items():
        lines.append(
            "insert into categories (slug, title, sort_order) values "
            f"({quote(slug)}, {quote(category)}, {order}) "
            "on conflict (slug) do nothing;"
        )
    lines.append("")

    for slug, difficulty, body, answer, followup in normalized:
        lines.append(
            "insert into questions (category_id, difficulty, body, answer, followup) "
            f"select id, {quote(difficulty)}, {quote(body)}, {quote(answer)}, {quote(followup)} "
            f"from categories where slug = {quote(slug)};"
        )

    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print(f"вопросов перенесено: {len(normalized)}")
    print(f"категорий: {len(categories)}")
    for name, (slug, _) in sorted(categories.items(), key=lambda kv: kv[1][1]):
        count = sum(1 for n in normalized if n[0] == slug)
        print(f"  {slug:24} {count:3}  {name}")
    if unknown:
        print("ВНИМАНИЕ, неизвестные категории:", dict(unknown))
    print(f"файл: {target}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
