export type Gender = "MALE" | "FEMALE" | "OTHER" | "UNSPECIFIED";
export type Mode = "CREATIVE" | "ALIGNED";
export type PriceBand = "LOW" | "MEDIUM" | "HIGH";
export type Category = "TRAVEL" | "ADVENTURE" | "LEARNING" | "WELLNESS" | "FAMILY" | "OTHER";
export type Verdict = "ACCEPT" | "REJECT";

export interface CreateProfileRequest {
  gender: Gender;
  age: number;
  capital: number;
}

export interface CreateProfileResponse {
  profileId: string;
  profileSummary: string;
  capital: number;
}

export interface Suggestion {
  id: string;
  title: string;
  description: string;
  priceBand: PriceBand;
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