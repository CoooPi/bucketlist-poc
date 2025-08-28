import { useState } from 'react';
import { PersonDescriptionInput } from './components/PersonDescriptionInput';
import { SingleSuggestionView } from './components/SingleSuggestionView';
import { ApiKeyGuard } from './components/ApiKeyGuard';
import { Card, CardContent } from './components/ui/card';
import { Button } from './components/ui/button';
import type { BucketListSuggestion } from './types';
import { apiService } from './services/api';

type AppState = 'input' | 'suggestions' | 'loading' | 'error' | 'regenerating';

function App() {
  const [state, setState] = useState<AppState>('input');
  const [sessionId, setSessionId] = useState<string>('');
  const [currentSuggestion, setCurrentSuggestion] = useState<BucketListSuggestion | null>(null);
  const [suggestionsReviewed, setSuggestionsReviewed] = useState<number>(0);
  const [totalSuggestions] = useState<number>(5); // Each batch has 5 suggestions
  const [error, setError] = useState<string>('');
  const [loadingNext, setLoadingNext] = useState<boolean>(false);

  const handlePersonDescriptionSubmit = async (description: string) => {
    setState('loading');
    setError('');
    
    try {
      const sessionResponse = await apiService.createSession(description);
      setSessionId(sessionResponse.sessionId);
      
      // Initialize suggestions and get the first one
      await apiService.getSuggestions(sessionResponse.sessionId); // This generates the initial batch
      const firstSuggestion = await apiService.getNextSuggestion(sessionResponse.sessionId);
      
      if (firstSuggestion) {
        setCurrentSuggestion(firstSuggestion);
        setSuggestionsReviewed(0);
        setState('suggestions');
      } else {
        throw new Error('No suggestions were generated');
      }
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

  const loadNextSuggestion = async () => {
    setLoadingNext(true);
    try {
      const nextSuggestion = await apiService.getNextSuggestion(sessionId);
      
      if (nextSuggestion) {
        setCurrentSuggestion(nextSuggestion);
      } else {
        // No more suggestions, need to regenerate
        setState('regenerating');
        const newSuggestions = await apiService.regenerateSuggestions(sessionId);
        
        if (newSuggestions.length > 0) {
          const firstNewSuggestion = await apiService.getNextSuggestion(sessionId);
          setCurrentSuggestion(firstNewSuggestion);
          setSuggestionsReviewed(0); // Reset counter for new batch
          setState('suggestions');
        } else {
          setCurrentSuggestion(null);
          setState('suggestions');
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load next suggestion');
      setState('error');
    } finally {
      setLoadingNext(false);
    }
  };

  const handleAcceptSuggestion = async (suggestionId: string) => {
    try {
      await apiService.acceptSuggestion(sessionId, suggestionId);
      setSuggestionsReviewed(prev => prev + 1);
      await loadNextSuggestion();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to accept suggestion');
      setState('error');
    }
  };

  const handleRejectSuggestion = async (suggestionId: string, reason: string, isCustom: boolean) => {
    try {
      await apiService.rejectSuggestion(sessionId, suggestionId, reason, isCustom);
      setSuggestionsReviewed(prev => prev + 1);
      await loadNextSuggestion();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reject suggestion');
      setState('error');
    }
  };

  const resetApp = () => {
    setState('input');
    setSessionId('');
    setCurrentSuggestion(null);
    setSuggestionsReviewed(0);
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

        {(state === 'suggestions' || state === 'regenerating') && (
          <SingleSuggestionView
            currentSuggestion={currentSuggestion}
            isLoading={false}
            isRegenerating={state === 'regenerating'}
            loadingNext={loadingNext}
            suggestionsReviewed={suggestionsReviewed}
            totalSuggestions={totalSuggestions}
            onAcceptSuggestion={handleAcceptSuggestion}
            onRejectSuggestion={handleRejectSuggestion}
            onStartOver={resetApp}
          />
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