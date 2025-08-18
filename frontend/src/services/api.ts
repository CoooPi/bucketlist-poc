import type { CreateProfileRequest, CreateProfileResponse, Suggestion, FeedbackRequest } from '../types';

const API_BASE = 'http://localhost:8080/api';

export const api = {
  async createProfile(request: CreateProfileRequest): Promise<CreateProfileResponse> {
    const response = await fetch(`${API_BASE}/profile`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    
    if (!response.ok) {
      throw new Error(`Failed to create profile: ${response.statusText}`);
    }
    
    return response.json();
  },

  async getNextSuggestion(profileId: string): Promise<Suggestion | null> {
    const response = await fetch(`${API_BASE}/suggestions/next?profileId=${profileId}`);
    
    if (response.status === 204) {
      return null; // No suggestions available
    }
    
    if (!response.ok) {
      throw new Error(`Failed to get suggestion: ${response.statusText}`);
    }
    
    return response.json();
  },

  async refillSuggestions(profileId: string, batchSize: number = 5): Promise<Suggestion[]> {
    const response = await fetch(`${API_BASE}/suggestions/refill?profileId=${profileId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ batchSize }),
    });
    
    if (!response.ok) {
      throw new Error(`Failed to refill suggestions: ${response.statusText}`);
    }
    
    const result = await response.json();
    return result.added;
  },

  async submitFeedback(feedback: FeedbackRequest): Promise<void> {
    const response = await fetch(`${API_BASE}/suggestions/feedback`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(feedback),
    });
    
    if (!response.ok) {
      throw new Error(`Failed to submit feedback: ${response.statusText}`);
    }
  },

  async getAcceptedSuggestions(profileId: string): Promise<Suggestion[]> {
    const response = await fetch(`${API_BASE}/suggestions/accepted?profileId=${profileId}`);
    
    if (!response.ok) {
      throw new Error(`Failed to get accepted suggestions: ${response.statusText}`);
    }
    
    return response.json();
  },
};