import { useState } from 'react';
import { OnboardingForm } from './components/OnboardingForm';
import { CategorySelectionPage } from './components/CategorySelectionPage';
import { SuggestionCard } from './components/SuggestionCard';
import { Card, CardContent } from './components/ui/card';
import { Button } from './components/ui/button';
import { api } from './services/api';
import type { CreateProfileRequest, CreateProfileResponse, Suggestion } from './types';
import type { SpendingCategory, SuggestionMode } from './types/categories';

type AppState = 'onboarding' | 'category-selection' | 'suggestions' | 'loading' | 'error';

function App() {
  const [state, setState] = useState<AppState>('onboarding');
  const [profile, setProfile] = useState<CreateProfileResponse | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<SpendingCategory | null>(null);
  const [selectedMode, setSelectedMode] = useState<SuggestionMode | null>(null);
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
      setState('category-selection');
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create profile');
      setState('error');
    } finally {
      setLoading(false);
    }
  };

  const handleCategorySelect = async (category: SpendingCategory, mode: SuggestionMode) => {
    setSelectedCategory(category);
    setSelectedMode(mode);
    
    if (!profile) return;
    
    setState('loading');
    
    // Try to get first suggestion with the selected category and mode
    await loadNextSuggestion(profile.profileId, category, mode);
  };

  const loadNextSuggestion = async (profileId: string, category: SpendingCategory, mode: SuggestionMode) => {
    try {
      let suggestion = await api.getNextSuggestion(profileId, category, mode);
      
      if (!suggestion) {
        // Show loading state while generating new suggestions
        setState('loading');
        setCurrentSuggestion(null);
        
        // Try to refill suggestions
        await api.refillSuggestions(profileId, category, mode, 5);
        suggestion = await api.getNextSuggestion(profileId, category, mode);
      }
      
      if (suggestion) {
        setCurrentSuggestion(suggestion);
        setState('suggestions');
      } else {
        setError('No more suggestions available');
        setState('error');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load suggestion');
      setState('error');
    }
  };

  const handleAccept = async () => {
    if (!profile || !currentSuggestion || !selectedCategory || !selectedMode) return;
    
    setLoading(true);
    try {
      await api.submitFeedback({
        profileId: profile.profileId,
        suggestionId: currentSuggestion.id,
        verdict: 'ACCEPT'
      });
      
      setAcceptedCount(prev => prev + 1);
      await loadNextSuggestion(profile.profileId, selectedCategory, selectedMode);
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to accept suggestion');
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async () => {
    if (!profile || !currentSuggestion || !selectedCategory || !selectedMode) return;
    
    setLoading(true);
    try {
      await api.submitFeedback({
        profileId: profile.profileId,
        suggestionId: currentSuggestion.id,
        verdict: 'REJECT'
      });
      
      await loadNextSuggestion(profile.profileId, selectedCategory, selectedMode);
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reject suggestion');
    } finally {
      setLoading(false);
    }
  };

  const resetApp = () => {
    setState('onboarding');
    setProfile(null);
    setSelectedCategory(null);
    setSelectedMode(null);
    setCurrentSuggestion(null);
    setAcceptedCount(0);
    setError('');
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="w-full max-w-2xl">

        {/* Main Content */}
        <div className="flex justify-center">
          {state === 'onboarding' && (
            <OnboardingForm onSubmit={handleCreateProfile} loading={loading} />
          )}

          {state === 'category-selection' && profile && (
            <CategorySelectionPage 
              profileId={profile.profileId}
              onCategorySelect={handleCategorySelect}
            />
          )}

          {state === 'loading' && (
            <Card className="w-full max-w-md">
              <CardContent className="flex flex-col items-center justify-center py-12">
                <div className="animate-spin rounded-full h-12 w-12 border-2 border-primary border-t-transparent mb-4"></div>
                <p className="text-muted-foreground">
                  {profile ? 'Generating new suggestions...' : 'Generating personalized suggestions...'}
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
                        {acceptedCount} suggestions added
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
                  Start over
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
                    Try again
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
