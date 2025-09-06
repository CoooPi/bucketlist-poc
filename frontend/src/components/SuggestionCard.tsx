import { useState } from "react";
import type { BucketListSuggestion } from "../types";
import { Button } from "./ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { Badge } from "./ui/badge";
import { Input } from "./ui/input";

interface SuggestionCardProps {
  suggestion: BucketListSuggestion;
  disabled?: boolean;
  onAccept: () => void;
  onReject: (reason: string, isCustom: boolean) => void;
}

export function SuggestionCard({
  suggestion,
  disabled = false,
  onAccept,
  onReject,
}: SuggestionCardProps) {
  const [showRejectOptions, setShowRejectOptions] = useState(false);
  const [customReason, setCustomReason] = useState("");
  const [showCustomInput, setShowCustomInput] = useState(false);

  const handleRejectWithReason = (reason: string, isCustom = false) => {
    onReject(reason, isCustom);
    setShowRejectOptions(false);
    setShowCustomInput(false);
    setCustomReason("");
  };

  const handleCustomReject = () => {
    if (customReason.trim()) {
      handleRejectWithReason(customReason.trim(), true);
    }
  };

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

  return (
    <Card className="w-full">
      <CardHeader>
        <div className="flex gap-2 justify-between items-start">
          <CardTitle className="text-xl">{suggestion.title}</CardTitle>
          <div className="flex gap-2">
            <Badge variant="outline">{suggestion.category}</Badge>
            <Badge
              className={getPriceColor(suggestion.priceBreakdown.totalCost)}
            >
              {formatCurrency(
                suggestion.priceBreakdown.totalCost,
                suggestion.priceBreakdown.currency,
              )}
            </Badge>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <p className="text-gray-700">{suggestion.description}</p>

        <div className="space-y-2">
          <h4 className="text-sm font-medium text-white-100">
            Cost Breakdown:
          </h4>
          <div className="space-y-1">
            {suggestion.priceBreakdown.lineItems.map((item, index) => (
              <div
                key={index}
                className="flex justify-between items-center text-sm"
              >
                <div>
                  <span className="font-medium">{item.name}</span>
                  {item.description && (
                    <span className="ml-1 text-gray-600">
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
            <div className="pt-1 mt-2 border-t">
              <div className="flex justify-between items-center text-sm font-semibold">
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

        {!showRejectOptions ? (
          <div className="flex gap-2">
            <Button onClick={onAccept} className="flex-1" disabled={disabled}>
              Accept
            </Button>
            <Button
              onClick={() => setShowRejectOptions(true)}
              variant="outline"
              className="flex-1"
              disabled={disabled}
            >
              Reject
            </Button>
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-sm font-medium">Why would you reject this?</p>
            <div className="space-y-2">
              {suggestion.rejectionReasons.map((reason, index) => (
                <Button
                  key={index}
                  variant="outline"
                  size="sm"
                  className="justify-start p-3 w-full h-auto text-left"
                  onClick={() => handleRejectWithReason(reason)}
                  disabled={disabled}
                >
                  {reason}
                </Button>
              ))}

              {!showCustomInput ? (
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full"
                  onClick={() => setShowCustomInput(true)}
                  disabled={disabled}
                >
                  Other reason...
                </Button>
              ) : (
                <div className="space-y-2">
                  <Input
                    placeholder="Enter your reason..."
                    value={customReason}
                    onChange={(e) => setCustomReason(e.target.value)}
                  />
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      onClick={handleCustomReject}
                      disabled={disabled}
                    >
                      Submit
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => {
                        setShowCustomInput(false);
                        setCustomReason("");
                      }}
                      disabled={disabled}
                    >
                      Cancel
                    </Button>
                  </div>
                </div>
              )}
            </div>

            <Button
              variant="ghost"
              size="sm"
              onClick={() => setShowRejectOptions(false)}
              className="w-full"
              disabled={disabled}
            >
              Back
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

