import type { BucketListSuggestion } from '../types';
import { CompactSuggestionCard } from './CompactSuggestionCard';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { CheckCircle, Trophy } from 'lucide-react';

interface AcceptedSuggestionsPanelProps {
  suggestions: BucketListSuggestion[];
  onSuggestionClick?: (suggestion: BucketListSuggestion) => void;
  className?: string;
}

export function AcceptedSuggestionsPanel({ 
  suggestions, 
  onSuggestionClick,
  className = '' 
}: AcceptedSuggestionsPanelProps) {
  const totalCost = suggestions.reduce((sum, suggestion) => {
    return sum + suggestion.priceBreakdown.totalCost;
  }, 0);

  const currency = suggestions.length > 0 ? suggestions[0].priceBreakdown.currency : 'USD';

  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
    }).format(amount);
  };

  return (
    <div className={`space-y-4 ${className}`}>
      <Card className="border-green-200">
        <CardHeader className="pb-3">
          <CardTitle className="text-lg flex items-center gap-2 text-green-800">
            <Trophy className="w-5 h-5" />
            Your Bucket List
            <span className="text-sm font-normal text-green-600">
              ({suggestions.length} {suggestions.length === 1 ? 'experience' : 'experiences'})
            </span>
          </CardTitle>
          {suggestions.length > 0 && (
            <div className="text-sm text-green-700">
              <span className="font-medium">Total estimated cost:</span>{' '}
              {formatCurrency(totalCost, currency)}
            </div>
          )}
        </CardHeader>
      </Card>

      {suggestions.length === 0 ? (
        <Card>
          <CardContent className="py-8 text-center">
            <CheckCircle className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <h3 className="font-medium text-gray-600 mb-2">No accepted experiences yet</h3>
            <p className="text-sm text-gray-500">
              Accept suggestions to build your bucket list!
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-2 max-h-96 overflow-y-auto">
          {suggestions.map((suggestion) => (
            <CompactSuggestionCard
              key={suggestion.id}
              suggestion={suggestion}
              status="accepted"
              onClick={() => onSuggestionClick?.(suggestion)}
            />
          ))}
        </div>
      )}
    </div>
  );
}