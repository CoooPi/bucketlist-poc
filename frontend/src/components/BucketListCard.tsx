import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Progress } from './ui/progress';
import { ScrollArea } from './ui/scroll-area';
import { SuggestionItemCard } from './SuggestionItemCard';
import { api } from '../services/api';
import type { Suggestion } from '../types';

interface BucketListCardProps {
  profileId: string;
  userBudget: number;
  refreshTrigger?: number; // Used to trigger refresh when new items are accepted
}

export function BucketListCard({ profileId, userBudget, refreshTrigger }: BucketListCardProps) {
  const [acceptedSuggestions, setAcceptedSuggestions] = useState<Suggestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>('');

  const loadAcceptedSuggestions = async () => {
    try {
      setLoading(true);
      const suggestions = await api.getAcceptedSuggestions(profileId);
      setAcceptedSuggestions(suggestions);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load bucket list');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAcceptedSuggestions();
  }, [profileId, refreshTrigger]);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('sv-SE', {
      style: 'currency',
      currency: 'SEK',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const calculateTotalCost = () => {
    return acceptedSuggestions.reduce((total, suggestion) => {
      const suggestionCost = suggestion.budgetBreakdown?.reduce((sum, item) => sum + item.amount, 0) || 0;
      return total + suggestionCost;
    }, 0);
  };

  const calculateBudgetPercentage = () => {
    const totalCost = calculateTotalCost();
    return Math.min((totalCost / userBudget) * 100, 100);
  };

  const getBudgetPercentageDisplay = () => {
    const totalCost = calculateTotalCost();
    return ((totalCost / userBudget) * 100).toFixed(1);
  };

  const isOverBudget = () => {
    const totalCost = calculateTotalCost();
    return totalCost > userBudget;
  };

  if (loading) {
    return (
      <Card className="w-full h-[500px] flex flex-col">
        <CardHeader className="flex-shrink-0">
          <CardTitle className="text-lg">Your Bucket List</CardTitle>
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
          <CardTitle className="text-lg">Your Bucket List</CardTitle>
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
        <CardTitle className="text-lg">Your Bucket List</CardTitle>
        
        {/* Progress Bar Section */}
        <div className="space-y-2">
          <Progress 
            value={calculateBudgetPercentage()} 
            className={`h-3 ${isOverBudget() ? 'bg-red-100' : ''}`}
          />
          <div className="flex justify-between items-center text-sm">
            <span className={isOverBudget() ? 'text-red-600 font-medium' : 'text-muted-foreground'}>
              {getBudgetPercentageDisplay()}% of {formatCurrency(userBudget)} budget used
            </span>
            {isOverBudget() && (
              <span className="text-red-600 text-xs font-medium">Over Budget!</span>
            )}
          </div>
        </div>
      </CardHeader>

      <CardContent className="pt-0 flex-1 flex flex-col min-h-0">
        {acceptedSuggestions.length === 0 ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <p className="text-muted-foreground text-sm">
                No items in your bucket list yet.
              </p>
              <p className="text-muted-foreground text-xs mt-1">
                Accept suggestions to start building your list!
              </p>
            </div>
          </div>
        ) : (
          <ScrollArea className="flex-1">
            <div className="space-y-3 pr-3">
              {acceptedSuggestions.map((suggestion) => (
                <SuggestionItemCard
                  key={suggestion.id}
                  id={suggestion.id}
                  title={suggestion.title}
                  description={suggestion.description}
                  budgetBreakdown={suggestion.budgetBreakdown}
                  variant="accepted"
                />
              ))}
            </div>
          </ScrollArea>
        )}
      </CardContent>
    </Card>
  );
}