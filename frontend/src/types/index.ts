export interface LineItem {
  name: string;
  price: number;
  description: string;
}

export interface PriceBreakdown {
  lineItems: LineItem[];
  currency: string;
  totalCost: number;
}

export interface BucketListSuggestion {
  id: string;
  title: string;
  description: string;
  category: string;
  priceBreakdown: PriceBreakdown;
  rejectionReasons: string[];
}

export interface SessionResponse {
  sessionId: string;
}

export interface SuggestionsResponse {
  suggestions: BucketListSuggestion[];
}

export interface PersonDescriptionRequest {
  personDescription: string;
}

export interface AcceptRequest {
  sessionId: string;
  suggestionId: string;
}

export interface RejectRequest {
  sessionId: string;
  suggestionId: string;
  reason: string;
  customReason: boolean;
}