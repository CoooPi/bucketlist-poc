import React, { useState } from 'react';
import { Button } from './ui/button';
import { Switch } from './ui/switch';
import { Card } from './ui/card';
import { 
  Plane, 
  Crown, 
  Heart, 
  Users, 
  Brain, 
  Gift, 
  Home, 
  Plus 
} from 'lucide-react';
import { CATEGORY_CONFIGS, MODE_CONFIGS, SpendingCategory, SuggestionMode } from '../types/categories';

// Icon mapping
const iconComponents = {
  Plane,
  Crown,
  Heart,
  Users,
  Brain,
  Gift,
  Home,
  Plus
};

interface CategorySelectionPageProps {
  profileId: string;
  onCategorySelect: (category: SpendingCategory, mode: SuggestionMode) => void;
}

export const CategorySelectionPage: React.FC<CategorySelectionPageProps> = ({ onCategorySelect }) => {
  const [selectedMode, setSelectedMode] = useState<SuggestionMode>(SuggestionMode.PROVEN);

  const handleCategorySelect = (category: SpendingCategory) => {
    onCategorySelect(category, selectedMode);
  };

  const renderCategoryButton = (config: typeof CATEGORY_CONFIGS[0]) => {
    const IconComponent = iconComponents[config.icon as keyof typeof iconComponents];
    
    return (
      <Button
        key={config.key}
        variant="outline"
        size="lg"
        onClick={() => handleCategorySelect(config.key)}
        className="w-full h-auto p-6 flex items-center justify-start gap-4 text-left hover:bg-accent transition-colors"
      >
        <IconComponent className="h-6 w-6 flex-shrink-0" />
        <span className="flex-1 text-base font-medium">{config.displayName}</span>
      </Button>
    );
  };

  return (
    <Card className="w-full max-w-md mx-auto">
      <div className="p-6 space-y-6">
        {/* Header */}
        <div className="text-center space-y-2">
          <h2 className="text-2xl font-bold">What do you want to spend your time and money on?</h2>
        </div>

        {/* Mode Selection - Compact */}
        <div className="flex items-center justify-between">
          <span className={selectedMode === SuggestionMode.PROVEN ? "text-sm font-medium" : "text-sm text-muted-foreground"}>
            {MODE_CONFIGS[SuggestionMode.PROVEN].displayName}
          </span>
          <div className="flex-shrink-0">
            <Switch
              checked={selectedMode === SuggestionMode.CREATIVE}
              onCheckedChange={(checked) => 
                setSelectedMode(checked ? SuggestionMode.CREATIVE : SuggestionMode.PROVEN)
              }
            />
          </div>
          <span className={selectedMode === SuggestionMode.CREATIVE ? "text-sm font-medium" : "text-sm text-muted-foreground"}>
            {MODE_CONFIGS[SuggestionMode.CREATIVE].displayName}
          </span>
        </div>

        {/* Category Selection */}
        <div className="space-y-3">
          {CATEGORY_CONFIGS.map(renderCategoryButton)}
        </div>

        {/* Helper text */}
        <p className="text-center text-xs text-muted-foreground">
          Select a category to start getting personalized suggestions
        </p>
      </div>
    </Card>
  );
};

export default CategorySelectionPage;