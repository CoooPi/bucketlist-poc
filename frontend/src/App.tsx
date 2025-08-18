import { useState } from 'react';
import { OnboardingForm } from './components/OnboardingForm';
import { SuggestionCard } from './components/SuggestionCard';
import { Card, CardContent } from './components/ui/card';
import { Button } from './components/ui/button';
import { api } from './services/api';
import type { CreateProfileRequest, CreateProfileResponse, Suggestion } from './types';

type AppState = 'onboarding' | 'suggestions' | 'loading' | 'error';

function App() {
  const [state, setState] = useState<AppState>('onboarding');
  const [profile, setProfile] = useState<CreateProfileResponse | null>(null);
  const [currentSuggestion, setCurrentSuggestion] = useState<Suggestion | null>(null);
  const [acceptedCount, setAcceptedCount] = useState(0);
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState(false);

  const handleCreateProfile = async (profileRequest: CreateProfileRequest) => {
    setLoading(true);
    setError('');
    
    try {
      const profileResponse = await api.createProfile(profileRequest);
      setProfile(profileResponse);
      setState('loading');
      
      // Try to get first suggestion
      await loadNextSuggestion(profileResponse.profileId);
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create profile');
      setState('error');
    } finally {
      setLoading(false);
    }
  };

  const loadNextSuggestion = async (profileId: string) => {
    try {
      let suggestion = await api.getNextSuggestion(profileId);
      
      if (!suggestion) {
        // Show loading state while generating new suggestions
        setState('loading');
        setCurrentSuggestion(null);
        
        // Try to refill suggestions
        await api.refillSuggestions(profileId, 5);
        suggestion = await api.getNextSuggestion(profileId);
      }
      
      if (suggestion) {
        setCurrentSuggestion(suggestion);
        setState('suggestions');
      } else {
        setError('Inga fler förslag tillgängliga');
        setState('error');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load suggestion');
      setState('error');
    }
  };

  const handleAccept = async () => {
    if (!profile || !currentSuggestion) return;
    
    setLoading(true);
    try {
      await api.submitFeedback({
        profileId: profile.profileId,
        suggestionId: currentSuggestion.id,
        verdict: 'ACCEPT'
      });
      
      setAcceptedCount(prev => prev + 1);
      await loadNextSuggestion(profile.profileId);
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to accept suggestion');
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async () => {
    if (!profile || !currentSuggestion) return;
    
    setLoading(true);
    try {
      await api.submitFeedback({
        profileId: profile.profileId,
        suggestionId: currentSuggestion.id,
        verdict: 'REJECT'
      });
      
      await loadNextSuggestion(profile.profileId);
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reject suggestion');
    } finally {
      setLoading(false);
    }
  };

  const resetApp = () => {
    setState('onboarding');
    setProfile(null);
    setCurrentSuggestion(null);
    setAcceptedCount(0);
    setError('');
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="w-full max-w-2xl">
        
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold tracking-tight mb-2">
            Bucket List Generator
          </h1>
          <p className="text-muted-foreground text-lg">
            AI-genererade förslag anpassade för dig
          </p>
        </div>

        {/* Main Content */}
        <div className="flex justify-center">
          {state === 'onboarding' && (
            <OnboardingForm onSubmit={handleCreateProfile} loading={loading} />
          )}

          {state === 'loading' && (
            <Card className="w-full max-w-md">
              <CardContent className="flex flex-col items-center justify-center py-12">
                <div className="animate-spin rounded-full h-12 w-12 border-2 border-primary border-t-transparent mb-4"></div>
                <p className="text-muted-foreground">
                  {profile ? 'Genererar nya förslag...' : 'Genererar personliga förslag...'}
                </p>
              </CardContent>
            </Card>
          )}

          {state === 'suggestions' && currentSuggestion && (
            <div className="w-full max-w-md space-y-6">
              {/* Progress Info */}
              <Card>
                <CardContent className="py-4">
                  <div className="text-center space-y-2">
                    {profile && (
                      <p className="text-sm text-muted-foreground">
                        {profile.profileSummary}
                      </p>
                    )}
                    <div className="flex items-center justify-center gap-2">
                      <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                      <span className="text-sm font-medium text-green-600">
                        {acceptedCount} förslag tillagda
                      </span>
                    </div>
                  </div>
                </CardContent>
              </Card>
              
              {/* Suggestion Card */}
              <SuggestionCard
                suggestion={currentSuggestion}
                onAccept={handleAccept}
                onReject={handleReject}
                loading={loading}
              />
              
              {/* Reset Button */}
              <div className="text-center">
                <Button
                  onClick={resetApp}
                  variant="ghost"
                  size="sm"
                  className="text-muted-foreground hover:text-foreground"
                >
                  Starta om
                </Button>
              </div>
            </div>
          )}

          {state === 'error' && (
            <Card className="w-full max-w-md">
              <CardContent className="py-8">
                <div className="text-center space-y-4">
                  <div className="p-4 bg-destructive/10 border border-destructive/20 rounded-md">
                    <p className="text-destructive font-medium">{error}</p>
                  </div>
                  <Button onClick={resetApp}>
                    Försök igen
                  </Button>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
        
      </div>
    </div>
  );
}

export default App
