export type Difficulty = "JUNIOR" | "MIDDLE" | "SENIOR";

export interface Category {
  slug: string;
  title: string;
  questions: number;
}

export interface QuestionSummary {
  id: number;
  category: string;
  categorySlug: string;
  difficulty: Difficulty;
  body: string;
}

export interface QuestionDetails extends QuestionSummary {
  answer: string;
  followup: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const DIFFICULTY_LABELS: Record<Difficulty, string> = {
  JUNIOR: "Junior",
  MIDDLE: "Middle",
  SENIOR: "Senior",
};
