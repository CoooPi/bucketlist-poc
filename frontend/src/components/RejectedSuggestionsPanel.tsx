import type { RejectedBucketListSuggestion } from '../types';
import { CompactSuggestionCard } from './CompactSuggestionCard';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { XCircle, TrendingDown } from 'lucide-react';
import { Badge } from './ui/badge';

interface RejectedSuggestionsPanelProps {
  suggestions: RejectedBucketListSuggestion[];
  onSuggestionClick?: (suggestion: RejectedBucketListSuggestion) => void;
  className?: string;
}

export function RejectedSuggestionsPanel({ 
  suggestions, 
  onSuggestionClick,
  className = '' 
}: RejectedSuggestionsPanelProps) {
  const customRejections = suggestions.filter(s => s.isCustomReason).length;

  return (
    <div className={`space-y-4 ${className}`}>
      <Card className="border-red-200">
        <CardHeader className="pb-3">
          <CardTitle className="text-lg flex items-center gap-2 text-red-800">
            <TrendingDown className="w-5 h-5" />
            Not Interested
            <span className="text-sm font-normal text-red-600">
              ({suggestions.length} {suggestions.length === 1 ? 'suggestion' : 'suggestions'})
            </span>
          </CardTitle>
          {suggestions.length > 0 && customRejections > 0 && (
            <div className="text-sm text-red-700">
              <span className="font-medium">Custom reasons:</span>{' '}
              <Badge variant="outline" className="ml-1 text-xs">
                {customRejections}
              </Badge>
            </div>
          )}
        </CardHeader>
      </Card>

      {suggestions.length === 0 ? (
        <Card>
          <CardContent className="py-8 text-center">
            <XCircle className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <h3 className="font-medium text-gray-600 mb-2">No rejected suggestions yet</h3>
            <p className="text-sm text-gray-500">
              Rejected suggestions will appear here to help improve future recommendations.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-2 max-h-96 overflow-y-auto">
          {suggestions.map((suggestion) => (
            <CompactSuggestionCard
              key={suggestion.id}
              suggestion={suggestion}
              status="rejected"
              onClick={() => onSuggestionClick?.(suggestion)}
            />
          ))}
        </div>
      )}
    </div>
  );
}