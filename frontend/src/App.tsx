import { useState } from 'react';
import { OnboardingForm } from './components/OnboardingForm';
import { CategorySelectionPage } from './components/CategorySelectionPage';
import { SuggestionCard } from './components/SuggestionCard';
import { BucketListCard } from './components/BucketListCard';
import { RejectedSuggestionsCard } from './components/RejectedSuggestionsCard';
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
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

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
      
      setRefreshTrigger(prev => prev + 1); // Trigger sidebar refresh
      await loadNextSuggestion(profile.profileId, selectedCategory, selectedMode);
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to accept suggestion');
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async (reason?: string) => {
    if (!profile || !currentSuggestion || !selectedCategory || !selectedMode) return;
    
    setLoading(true);
    try {
      await api.submitFeedback({
        profileId: profile.profileId,
        suggestionId: currentSuggestion.id,
        verdict: 'REJECT',
        reason
      });
      
      setRefreshTrigger(prev => prev + 1); // Trigger sidebar refresh
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
    setError('');
  };

  return (
    <div className="min-h-screen bg-background p-4">
      {/* Non-suggestions states: center in screen */}
      {(state === 'onboarding' || state === 'category-selection' || state === 'loading' || state === 'error') && (
        <div className="min-h-screen flex items-center justify-center">
          <div className="w-full max-w-2xl">
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
      )}

      {/* Suggestions state: three-column layout */}
      {state === 'suggestions' && currentSuggestion && profile && (
        <div className="min-h-screen flex items-center justify-center">
          <div className="w-full max-w-7xl flex items-center gap-6 px-4">
            {/* Left Column: Rejected Suggestions */}
            <div className="w-80 flex-shrink-0">
              <RejectedSuggestionsCard
                profileId={profile.profileId}
                refreshTrigger={refreshTrigger}
              />
            </div>

            {/* Center Column: Main Suggestion */}
            <div className="flex-1 flex flex-col justify-center items-center">
              <div className="w-full max-w-md space-y-6">
                {/* Main Suggestion Card */}
                <SuggestionCard
                  suggestion={currentSuggestion}
                  userBudget={profile.capital}
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
            </div>

            {/* Right Column: Bucket List */}
            <div className="w-80 flex-shrink-0">
              <BucketListCard
                profileId={profile.profileId}
                userBudget={profile.capital}
                refreshTrigger={refreshTrigger}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App
