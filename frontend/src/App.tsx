import { useState } from 'react';
import { PersonDescriptionInput } from './components/PersonDescriptionInput';
import { SuggestionGrid } from './components/SuggestionGrid';
import { ApiKeyGuard } from './components/ApiKeyGuard';
import { Card, CardContent } from './components/ui/card';
import { Button } from './components/ui/button';
import type { BucketListSuggestion } from './types';
import { apiService } from './services/api';

type AppState = 'input' | 'suggestions' | 'loading' | 'error';

function App() {
  const [state, setState] = useState<AppState>('input');
  const [sessionId, setSessionId] = useState<string>('');
  const [suggestions, setSuggestions] = useState<BucketListSuggestion[]>([]);
  const [error, setError] = useState<string>('');

  const handlePersonDescriptionSubmit = async (description: string) => {
    setState('loading');
    setError('');
    
    try {
      const sessionResponse = await apiService.createSession(description);
      setSessionId(sessionResponse.sessionId);
      
      const suggestionsData = await apiService.getSuggestions(sessionResponse.sessionId);
      setSuggestions(suggestionsData);
      setState('suggestions');
    } catch (err) {
      if (err instanceof Error && err.message.includes('Failed to get suggestions')) {
        // This might be an API key issue, let the ApiKeyGuard handle it
        const keyStatus = await checkApiKeyStatus();
        if (!keyStatus) {
          window.location.reload(); // Force ApiKeyGuard to show
          return;
        }
      }
      setError(err instanceof Error ? err.message : 'Failed to generate suggestions');
      setState('error');
    }
  };

  const handleAcceptSuggestion = async (suggestionId: string) => {
    try {
      await apiService.acceptSuggestion(sessionId, suggestionId);
      // Remove accepted suggestion from the list
      setSuggestions(prev => prev.filter(s => s.id !== suggestionId));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to accept suggestion');
    }
  };

  const handleRejectSuggestion = async (suggestionId: string, reason: string, isCustom: boolean) => {
    try {
      await apiService.rejectSuggestion(sessionId, suggestionId, reason, isCustom);
      // Remove rejected suggestion from the list
      setSuggestions(prev => prev.filter(s => s.id !== suggestionId));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reject suggestion');
    }
  };

  const resetApp = () => {
    setState('input');
    setSessionId('');
    setSuggestions([]);
    setError('');
  };

  // Real API key functions
  const checkApiKeyStatus = () => apiService.checkApiKeyStatus();
  const submitApiKey = (apiKey: string) => apiService.submitApiKey(apiKey);

  return (
    <ApiKeyGuard
      checkApiKeyStatus={checkApiKeyStatus}
      submitApiKey={submitApiKey}
    >
      <div className="min-h-screen bg-background">
        {state === 'input' && (
          <PersonDescriptionInput
            onSubmit={handlePersonDescriptionSubmit}
            isLoading={false}
          />
        )}

        {state === 'loading' && (
          <PersonDescriptionInput
            onSubmit={handlePersonDescriptionSubmit}
            isLoading={true}
          />
        )}

        {state === 'suggestions' && (
          <>
            <SuggestionGrid
              suggestions={suggestions}
              onAcceptSuggestion={handleAcceptSuggestion}
              onRejectSuggestion={handleRejectSuggestion}
            />
            {suggestions.length === 0 && (
              <div className="text-center p-8">
                <p className="text-gray-600 mb-4">All suggestions have been reviewed!</p>
                <Button
                  onClick={resetApp}
                  variant="outline"
                >
                  Start over with a new person
                </Button>
              </div>
            )}
          </>
        )}

        {state === 'error' && (
          <div className="min-h-screen flex items-center justify-center p-4">
            <Card className="w-full max-w-md">
              <CardContent className="py-8">
                <div className="text-center space-y-4">
                  <div className="p-4 bg-destructive/10 border border-destructive/20 rounded-md">
                    <p className="text-destructive font-medium">{error}</p>
                  </div>
                  <Button onClick={resetApp}>
                    Try Again
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>
        )}
      </div>
    </ApiKeyGuard>
  );
}

export default App;