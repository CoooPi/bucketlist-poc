import type { BucketListSuggestion } from "../types";
import { SuggestionCard } from "./SuggestionCard";
import { LoadingSuggestionCard } from "./LoadingSuggestionCard";
import { Button } from "./ui/button";
import { Card, CardContent } from "./ui/card";

interface SingleSuggestionViewProps {
  currentSuggestion: BucketListSuggestion | null;
  isLoading: boolean;
  isRegenerating: boolean;
  loadingNext: boolean;
  suggestionsReviewed: number;
  totalSuggestions: number;
  onAcceptSuggestion: (suggestionId: string) => void;
  onRejectSuggestion: (
    suggestionId: string,
    reason: string,
    isCustom: boolean,
  ) => void;
  onStartOver: () => void;
}

export function SingleSuggestionView({
  currentSuggestion,
  isLoading,
  isRegenerating,
  loadingNext,
  suggestionsReviewed,
  totalSuggestions,
  onAcceptSuggestion,
  onRejectSuggestion,
  onStartOver,
}: SingleSuggestionViewProps) {
  if (isRegenerating) {
    return (
      <div className="flex justify-center items-center p-6 min-h-screen">
        <div className="mx-auto space-y-4 max-w-2xl text-center">
          <LoadingSuggestionCard />
          <div className="text-center">
            <h2 className="mb-2 text-2xl font-bold">
              Generating Better Suggestions
            </h2>
            <p className="text-gray-600">
              Learning from your feedback to create more personalized
              recommendations...
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="flex justify-center items-center p-6 min-h-screen">
        <div className="mx-auto max-w-2xl">
          <LoadingSuggestionCard />
        </div>
      </div>
    );
  }

  if (!currentSuggestion) {
    return (
      <div className="flex justify-center items-center p-6 min-h-screen">
        <Card className="w-full max-w-md">
          <CardContent className="py-8">
            <div className="space-y-4 text-center">
              <h2 className="text-2xl font-bold">All suggestions reviewed!</h2>
              <p className="mb-4 text-gray-600">
                You've reviewed {suggestionsReviewed} suggestions. Ready to
                start over with a new person?
              </p>
              <Button
                onClick={onStartOver}
                variant="outline"
                className="w-full"
              >
                Start over with a new person
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="p-6 min-h-screen">
      <div className="mx-auto max-w-2xl">
        <div className="mb-6 text-center">
          <h1 className="mb-2 text-3xl font-bold">Bucket List Suggestion</h1>
        </div>

        {loadingNext ? (
          <div className="flex justify-center">
            <LoadingSuggestionCard />
          </div>
        ) : (
          <SuggestionCard
            suggestion={currentSuggestion}
            disabled={loadingNext}
            onAccept={() => onAcceptSuggestion(currentSuggestion.id)}
            onReject={(reason, isCustom) =>
              onRejectSuggestion(currentSuggestion.id, reason, isCustom)
            }
          />
        )}
      </div>
    </div>
  );
}

