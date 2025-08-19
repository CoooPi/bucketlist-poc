import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { ScrollArea } from './ui/scroll-area';
import { SuggestionItemCard } from './SuggestionItemCard';
import { api } from '../services/api';
import type { RejectedSuggestion } from '../types';

interface RejectedSuggestionsCardProps {
  profileId: string;
  refreshTrigger?: number; // Used to trigger refresh when new items are rejected
}

export function RejectedSuggestionsCard({ profileId, refreshTrigger }: RejectedSuggestionsCardProps) {
  const [rejectedSuggestions, setRejectedSuggestions] = useState<RejectedSuggestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>('');

  const loadRejectedSuggestions = async () => {
    try {
      setLoading(true);
      const suggestions = await api.getRejectedSuggestions(profileId);
      setRejectedSuggestions(suggestions);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load rejected suggestions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRejectedSuggestions();
  }, [profileId, refreshTrigger]);

  if (loading) {
    return (
      <Card className="w-full h-[500px] flex flex-col">
        <CardHeader className="flex-shrink-0">
          <CardTitle className="text-lg">Rejected Suggestions</CardTitle>
        </CardHeader>
        <CardContent className="flex-1 flex items-center justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-2 border-primary border-t-transparent"></div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card className="w-full h-[500px] flex flex-col">
        <CardHeader className="flex-shrink-0">
          <CardTitle className="text-lg">Rejected Suggestions</CardTitle>
        </CardHeader>
        <CardContent className="flex-1 flex items-center justify-center">
          <p className="text-destructive text-sm">{error}</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="w-full h-[500px] flex flex-col">
      <CardHeader className="pb-3 flex-shrink-0">
        <CardTitle className="text-lg">Rejected Suggestions</CardTitle>
        <p className="text-sm text-muted-foreground">
          Suggestions you've passed on
        </p>
      </CardHeader>

      <CardContent className="pt-0 flex-1 flex flex-col min-h-0">
        {rejectedSuggestions.length === 0 ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <p className="text-muted-foreground text-sm">
                No rejected suggestions yet.
              </p>
              <p className="text-muted-foreground text-xs mt-1">
                Suggestions you reject will appear here.
              </p>
            </div>
          </div>
        ) : (
          <ScrollArea className="flex-1">
            <div className="space-y-3 pr-3">
              {rejectedSuggestions.map((suggestion) => (
                <SuggestionItemCard
                  key={suggestion.id}
                  id={suggestion.id}
                  title={suggestion.title}
                  description={suggestion.description}
                  budgetBreakdown={suggestion.budgetBreakdown}
                  reason={suggestion.reason}
                  variant="rejected"
                />
              ))}
            </div>
          </ScrollArea>
        )}
      </CardContent>
    </Card>
  );
}