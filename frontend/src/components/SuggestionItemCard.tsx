import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { ChevronDown, ChevronRight } from 'lucide-react';
import type { BudgetItem } from '../types';

interface SuggestionItemCardProps {
  id: string;
  title: string;
  description: string;
  budgetBreakdown: BudgetItem[];
  reason?: string;
  variant?: 'accepted' | 'rejected';
  className?: string;
}


export function SuggestionItemCard({ 
  title, 
  description, 
  budgetBreakdown, 
  reason, 
  variant = 'accepted',
  className = ''
}: SuggestionItemCardProps) {
  const [isDescriptionExpanded, setIsDescriptionExpanded] = useState(false);
  const [isBudgetExpanded, setIsBudgetExpanded] = useState(false);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('sv-SE', {
      style: 'currency',
      currency: 'SEK',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const calculateTotalCost = () => {
    return budgetBreakdown?.reduce((total, item) => total + item.amount, 0) || 0;
  };


  const truncateText = (text: string, maxLength: number) => {
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  };

  const cardClass = variant === 'rejected' 
    ? 'border-muted bg-muted/20 text-muted-foreground' 
    : '';

  return (
    <Card className={`mb-3 ${cardClass} ${className}`}>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium leading-tight">
          {truncateText(title, 60)}
        </CardTitle>
        
        {/* Collapsible Description */}
        <div className="mt-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setIsDescriptionExpanded(!isDescriptionExpanded)}
            className="h-6 px-2 -ml-2"
          >
            {isDescriptionExpanded ? (
              <ChevronDown className="w-3 h-3" />
            ) : (
              <ChevronRight className="w-3 h-3" />
            )}
            <span className="ml-1 text-xs">Description</span>
          </Button>
          
          {isDescriptionExpanded && (
            <p className="text-xs text-muted-foreground mt-1 pl-2">
              {description}
            </p>
          )}
        </div>
      </CardHeader>
      
      <CardContent className="pt-0">
        <div className="flex items-center justify-between mb-2">
          <div className="text-lg font-bold">
            {formatCurrency(calculateTotalCost())}
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setIsBudgetExpanded(!isBudgetExpanded)}
            className="h-6 px-2"
          >
            {isBudgetExpanded ? (
              <ChevronDown className="w-3 h-3" />
            ) : (
              <ChevronRight className="w-3 h-3" />
            )}
            <span className="ml-1 text-xs">Budget Details</span>
          </Button>
        </div>

        {isBudgetExpanded && budgetBreakdown && budgetBreakdown.length > 0 && (
          <div className="border-t pt-2 mt-2">
            <div className="space-y-1">
              {budgetBreakdown.map((item, index) => (
                <div key={index} className="flex justify-between items-center text-xs">
                  <div className="flex-1 min-w-0">
                    <span className="font-medium">{item.category}</span>
                    <span className="text-muted-foreground ml-1">- {item.description}</span>
                  </div>
                  <span className="font-medium ml-2 flex-shrink-0">
                    {formatCurrency(item.amount)}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {reason && variant === 'rejected' && (
          <div className="border-t pt-2 mt-2">
            <p className="text-xs text-muted-foreground">
              <span className="font-medium">Reason:</span> {reason}
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}