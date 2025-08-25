import type { BucketListSuggestion } from '../types';
import { SuggestionCard } from './SuggestionCard';

interface SuggestionGridProps {
  suggestions: BucketListSuggestion[];
  onAcceptSuggestion: (suggestionId: string) => void;
  onRejectSuggestion: (suggestionId: string, reason: string, isCustom: boolean) => void;
}

export function SuggestionGrid({ 
  suggestions, 
  onAcceptSuggestion, 
  onRejectSuggestion 
}: SuggestionGridProps) {
  if (suggestions.length === 0) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-gray-500">No suggestions available</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen p-6">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold mb-2">Bucket List Suggestions</h1>
          <p className="text-gray-600">Here are some personalized suggestions across different categories</p>
        </div>
        
        <div className="grid gap-6 md:grid-cols-1 lg:grid-cols-2 xl:grid-cols-1">
          {suggestions.map((suggestion) => (
            <SuggestionCard
              key={suggestion.id}
              suggestion={suggestion}
              onAccept={() => onAcceptSuggestion(suggestion.id)}
              onReject={(reason, isCustom) => onRejectSuggestion(suggestion.id, reason, isCustom)}
            />
          ))}
        </div>
      </div>
    </div>
  );
}