import type { Suggestion, PriceBand } from '../types';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { X, Check } from 'lucide-react';

interface SuggestionCardProps {
  suggestion: Suggestion;
  onAccept: () => void;
  onReject: () => void;
  loading?: boolean;
}

const getPriceBandVariant = (priceBand: PriceBand) => {
  switch (priceBand) {
    case 'LOW': return 'default';
    case 'MEDIUM': return 'secondary';
    case 'HIGH': return 'destructive';
  }
};

const getPriceBandText = (priceBand: PriceBand): string => {
  switch (priceBand) {
    case 'LOW': return 'Low cost';
    case 'MEDIUM': return 'Medium cost';
    case 'HIGH': return 'High cost';
  }
};


export function SuggestionCard({ suggestion, onAccept, onReject, loading = false }: SuggestionCardProps) {
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('sv-SE', {
      style: 'currency',
      currency: 'SEK',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const calculateTotalCost = () => {
    return suggestion.budgetBreakdown?.reduce((total, item) => total + item.amount, 0) || 0;
  };

  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <div className="flex flex-wrap gap-2 mb-2">
          <Badge variant={getPriceBandVariant(suggestion.priceBand)}>
            {getPriceBandText(suggestion.priceBand)}
          </Badge>
        </div>
        <CardTitle className="text-xl">
          {suggestion.title}
        </CardTitle>
        <div className="text-2xl font-bold text-primary">
          {formatCurrency(calculateTotalCost())}
        </div>
      </CardHeader>
      <CardContent>
        <CardDescription className="text-base mb-4 leading-relaxed">
          {suggestion.description}
        </CardDescription>

        {/* Budget Breakdown */}
        {suggestion.budgetBreakdown && suggestion.budgetBreakdown.length > 0 && (
          <div className="mb-6">
            <h4 className="text-sm font-medium mb-3 text-muted-foreground">Budget breakdown:</h4>
            <div className="space-y-2">
              {suggestion.budgetBreakdown.map((item, index) => (
                <div key={index} className="flex justify-between items-center text-sm">
                  <div className="flex-1">
                    <span className="font-medium">{item.category}</span>
                    <span className="text-muted-foreground ml-1">- {item.description}</span>
                  </div>
                  <span className="font-medium">
                    {formatCurrency(item.amount)}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
        
        <div className="flex gap-3">
          <Button
            onClick={onReject}
            disabled={loading}
            variant="outline"
            size="lg"
            className="flex-1"
          >
            <X className="w-4 h-4 mr-2" />
            No thanks
          </Button>
          
          <Button
            onClick={onAccept}
            disabled={loading}
            size="lg"
            className="flex-1 bg-green-600 hover:bg-green-700 text-gray-100"
          >
            <Check className="w-4 h-4 mr-2" />
            Add to list
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}