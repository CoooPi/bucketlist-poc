export const SpendingCategory = {
  TRAVEL_VACATION: "TRAVEL_VACATION" as const,
  LUXURY_THINGS: "LUXURY_THINGS" as const,
  HEALTH_WELLNESS: "HEALTH_WELLNESS" as const,
  SOCIAL_LIFESTYLE: "SOCIAL_LIFESTYLE" as const,
  MENTAL_EMOTIONAL: "MENTAL_EMOTIONAL" as const,
  SMALL_LUXURY: "SMALL_LUXURY" as const,
  FREEDOM_COMFORT: "FREEDOM_COMFORT" as const,
  OPTIONAL_ADDONS: "OPTIONAL_ADDONS" as const,
} as const;

export type SpendingCategory = typeof SpendingCategory[keyof typeof SpendingCategory];

export const SuggestionMode = {
  PROVEN: "PROVEN" as const,
  CREATIVE: "CREATIVE" as const,
} as const;

export type SuggestionMode = typeof SuggestionMode[keyof typeof SuggestionMode];

export interface CategoryConfig {
  key: SpendingCategory;
  displayName: string;
  icon: string; // Lucide icon name
  description: string;
}

export const CATEGORY_CONFIGS: CategoryConfig[] = [
  {
    key: SpendingCategory.TRAVEL_VACATION,
    displayName: "Travel & Vacation",
    icon: "Plane",
    description: "Travel destinations, vacation experiences, hotels, and tourism"
  },
  {
    key: SpendingCategory.LUXURY_THINGS,
    displayName: "Luxury Things", 
    icon: "Crown",
    description: "High-end products, luxury goods, premium services, and exclusive experiences"
  },
  {
    key: SpendingCategory.HEALTH_WELLNESS,
    displayName: "Health & Wellness",
    icon: "Heart",
    description: "Physical health, mental wellbeing, fitness, nutrition, and self-care"
  },
  {
    key: SpendingCategory.SOCIAL_LIFESTYLE,
    displayName: "Social & Lifestyle",
    icon: "Users",
    description: "Social activities, entertainment, lifestyle enhancements, and experiences with others"
  },
  {
    key: SpendingCategory.MENTAL_EMOTIONAL,
    displayName: "Mental & Emotional Wellbeing",
    icon: "Brain", 
    description: "Mental health, personal development, therapy, coaching, and emotional wellbeing"
  },
  {
    key: SpendingCategory.SMALL_LUXURY,
    displayName: "Small Luxury Treats",
    icon: "Gift",
    description: "Affordable luxury treats, small indulgences, and everyday pleasures"
  },
  {
    key: SpendingCategory.FREEDOM_COMFORT,
    displayName: "Freedom & Comfort",
    icon: "Home",
    description: "Experiences and purchases that provide freedom, comfort, and convenience"
  },
  {
    key: SpendingCategory.OPTIONAL_ADDONS,
    displayName: "Optional Add-ons",
    icon: "Plus",
    description: "Supplementary experiences, add-on services, upgrades, and extra features"
  }
];

export const MODE_CONFIGS = {
  [SuggestionMode.PROVEN]: {
    displayName: "Proven Ideas",
    description: "Popular, well-known bucket list items that most people would enjoy"
  },
  [SuggestionMode.CREATIVE]: {
    displayName: "Creative Ideas",
    description: "Unique, uncommon experiences that people generally wouldn't think of"
  }
};