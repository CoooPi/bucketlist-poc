export type Gender = "MALE" | "FEMALE" | "OTHER" | "UNSPECIFIED";
export type Mode = "CREATIVE" | "ALIGNED";
export type PriceBand = "LOW" | "MEDIUM" | "HIGH";
export type Category = "TRAVEL" | "ADVENTURE" | "LEARNING" | "WELLNESS" | "FAMILY" | "OTHER";
export type Verdict = "ACCEPT" | "REJECT";

export interface CreateProfileRequest {
  gender: Gender;
  age: number;
  capital: number;
  mode: Mode;
}

export interface CreateProfileResponse {
  profileId: string;
  profileSummary: string;
  mode: Mode;
}

export interface Suggestion {
  id: string;
  title: string;
  description: string;
  priceBand: PriceBand;
  category: Category;
  estimatedCost: number;
  budgetBreakdown: BudgetItem[];
}

export interface BudgetItem {
  category: string;
  description: string;
  amount: number;
}

export interface FeedbackRequest {
  profileId: string;
  suggestionId: string;
  verdict: Verdict;
  reason?: string;
}