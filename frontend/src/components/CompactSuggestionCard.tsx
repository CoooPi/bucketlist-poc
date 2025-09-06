import { useState } from "react";
import type {
  BucketListSuggestion,
  RejectedBucketListSuggestion,
} from "../types";
import { Card, CardContent } from "./ui/card";
import { Badge } from "./ui/badge";
import { Button } from "./ui/button";
import { Check, X, ChevronDown, ChevronUp } from "lucide-react";

interface CompactSuggestionCardProps {
  suggestion: BucketListSuggestion | RejectedBucketListSuggestion;
  status: "accepted" | "rejected";
  onClick?: () => void;
}

export function CompactSuggestionCard({
  suggestion,
  status,
  onClick,
}: CompactSuggestionCardProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: currency,
    }).format(amount);
  };

  const getPriceColor = (totalCost: number) => {
    if (totalCost < 100) return "bg-green-100 text-green-800";
    if (totalCost < 500) return "bg-yellow-100 text-yellow-800";
    return "bg-red-100 text-red-800";
  };

  const getStatusColor = (status: "accepted" | "rejected") => {
    return status === "accepted" ? "border-l-green-500" : "border-l-red-500";
  };

  const isRejected = (
    suggestion: BucketListSuggestion | RejectedBucketListSuggestion,
  ): suggestion is RejectedBucketListSuggestion => {
    return "rejectionReason" in suggestion;
  };

  const toggleExpanded = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsExpanded(!isExpanded);
  };

  return (
    <Card
      className={`w-full border-l-4 cursor-pointer hover:shadow-md transition-shadow ${getStatusColor(status)}`}
      onClick={onClick}
    >
      <CardContent className="p-3">
        <div className="space-y-2">
          {/* Header */}
          <div className="flex gap-2 justify-between items-start">
            <div className="flex flex-1 gap-2 items-center min-w-0">
              {status === "accepted" ? (
                <Check className="flex-shrink-0 w-4 h-4 text-green-600" />
              ) : (
                <X className="flex-shrink-0 w-4 h-4 text-red-600" />
              )}
              <h3 className="text-sm font-medium truncate">
                {suggestion.title}
              </h3>
            </div>
            <div className="flex flex-shrink-0 gap-1 items-center">
              <Badge variant="outline" className="text-xs">
                {suggestion.category}
              </Badge>
              <Badge
                className={`text-xs ${getPriceColor(suggestion.priceBreakdown.totalCost)}`}
              >
                {formatCurrency(
                  suggestion.priceBreakdown.totalCost,
                  suggestion.priceBreakdown.currency,
                )}
              </Badge>
            </div>
          </div>

          {/* Rejection reason for rejected suggestions */}
          {status === "rejected" && isRejected(suggestion) && (
            <div className="py-1 px-2 text-xs text-red-700 bg-red-100 rounded">
              <span className="font-medium">Rejected:</span>{" "}
              {suggestion.rejectionReason}
              {suggestion.isCustomReason && (
                <Badge variant="outline" className="ml-1 text-xs">
                  Custom
                </Badge>
              )}
            </div>
          )}

          {/* Expand/Collapse button */}
          <div className="flex justify-center">
            <Button
              variant="ghost"
              size="sm"
              onClick={toggleExpanded}
              className="p-0 w-6 h-6 text-gray-400 hover:text-gray-600"
            >
              {isExpanded ? (
                <ChevronUp className="w-3 h-3" />
              ) : (
                <ChevronDown className="w-3 h-3" />
              )}
            </Button>
          </div>

          {/* Expanded details */}
          {isExpanded && (
            <div className="pt-2 space-y-2 border-t border-gray-200">
              <p className="text-xs leading-relaxed text-gray-600">
                {suggestion.description}
              </p>

              <div className="space-y-1">
                <h4 className="text-xs font-medium text-white-100">
                  Cost Breakdown:
                </h4>
                <div className="space-y-1">
                  {suggestion.priceBreakdown.lineItems.map((item, index) => (
                    <div
                      key={index}
                      className="flex justify-between items-center text-xs"
                    >
                      <div>
                        <span className="font-medium">{item.name}</span>
                        {item.description && (
                          <span className="ml-1 text-gray-500">
                            - {item.description}
                          </span>
                        )}
                      </div>
                      <span className="text-gray-700">
                        {formatCurrency(
                          item.price,
                          suggestion.priceBreakdown.currency,
                        )}
                      </span>
                    </div>
                  ))}
                  <div className="pt-1 mt-1 border-t">
                    <div className="flex justify-between items-center text-xs font-semibold">
                      <span>Total:</span>
                      <span>
                        {formatCurrency(
                          suggestion.priceBreakdown.totalCost,
                          suggestion.priceBreakdown.currency,
                        )}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

