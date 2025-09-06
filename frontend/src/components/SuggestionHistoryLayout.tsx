import type { BucketListSuggestion, RejectedBucketListSuggestion } from '../types';
import { AcceptedSuggestionsPanel } from './AcceptedSuggestionsPanel';
import { RejectedSuggestionsPanel } from './RejectedSuggestionsPanel';
import { SuggestionCard } from './SuggestionCard';
import { LoadingSuggestionCard } from './LoadingSuggestionCard';
import { Card, CardContent } from './ui/card';
import { Button } from './ui/button';

interface SuggestionHistoryLayoutProps {
  // Current suggestion in center
  currentSuggestion: BucketListSuggestion | null;
  isLoading: boolean;
  isRegenerating: boolean;
  loadingNext: boolean;
  
  // History panels
  acceptedSuggestions: BucketListSuggestion[];
  rejectedSuggestions: RejectedBucketListSuggestion[];
  
  // Progress info
  suggestionsReviewed: number;
  totalSuggestions: number;
  
  // Action handlers
  onAcceptSuggestion: (suggestionId: string) => void;
  onRejectSuggestion: (suggestionId: string, reason: string, isCustom: boolean) => void;
  onStartOver: () => void;
  
  // Optional click handlers for history panels
  onAcceptedSuggestionClick?: (suggestion: BucketListSuggestion) => void;
  onRejectedSuggestionClick?: (suggestion: RejectedBucketListSuggestion) => void;
}

export function SuggestionHistoryLayout({
  currentSuggestion,
  isLoading,
  isRegenerating,
  loadingNext,
  acceptedSuggestions,
  rejectedSuggestions,
  suggestionsReviewed,
  totalSuggestions: _totalSuggestions,
  onAcceptSuggestion,
  onRejectSuggestion,
  onStartOver,
  onAcceptedSuggestionClick,
  onRejectedSuggestionClick,
}: SuggestionHistoryLayoutProps) {
  
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
      <div className="p-6 min-h-screen">
        <div className="max-w-7xl mx-auto">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left Panel - Accepted */}
            <div className="lg:col-span-1">
              <AcceptedSuggestionsPanel
                suggestions={acceptedSuggestions}
                onSuggestionClick={onAcceptedSuggestionClick}
              />
            </div>

            {/* Center Panel - No more suggestions */}
            <div className="lg:col-span-1 flex justify-center items-start">
              <Card className="w-full max-w-md mt-8">
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

            {/* Right Panel - Rejected */}
            <div className="lg:col-span-1">
              <RejectedSuggestionsPanel
                suggestions={rejectedSuggestions}
                onSuggestionClick={onRejectedSuggestionClick}
              />
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 min-h-screen">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-6 text-center">
          <h1 className="mb-2 text-3xl font-bold">Bucket List Suggestion</h1>
          <div className="text-sm text-gray-600">
            Progress: {acceptedSuggestions.length + rejectedSuggestions.length} reviewed • {acceptedSuggestions.length} accepted • {rejectedSuggestions.length} rejected
          </div>
        </div>

        {/* Three-column layout */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Panel - Accepted Suggestions */}
          <div className="lg:col-span-1 order-2 lg:order-1">
            <AcceptedSuggestionsPanel
              suggestions={acceptedSuggestions}
              onSuggestionClick={onAcceptedSuggestionClick}
            />
          </div>

          {/* Center Panel - Current Suggestion */}
          <div className="lg:col-span-1 order-1 lg:order-2">
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

          {/* Right Panel - Rejected Suggestions */}
          <div className="lg:col-span-1 order-3 lg:order-3">
            <RejectedSuggestionsPanel
              suggestions={rejectedSuggestions}
              onSuggestionClick={onRejectedSuggestionClick}
            />
          </div>
        </div>
      </div>
    </div>
  );
}