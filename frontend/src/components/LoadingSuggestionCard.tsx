import { Card, CardContent, CardHeader, CardTitle } from './ui/card';

export function LoadingSuggestionCard() {
  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <CardTitle className="text-xl">
          Generating new suggestions...
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex flex-col items-center justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-2 border-primary border-t-transparent mb-4"></div>
          <p className="text-muted-foreground text-center">
            Creating personalized suggestions based on your preferences
          </p>
        </div>
      </CardContent>
    </Card>
  );
}