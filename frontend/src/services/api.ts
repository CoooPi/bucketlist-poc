import type {
  BucketListSuggestion,
  RejectedBucketListSuggestion,
  RejectedSuggestionsResponse,
  SessionResponse,
  SuggestionsResponse,
  PersonDescriptionRequest,
  AcceptRequest,
  RejectRequest
} from '../types';

const API_BASE_URL = 'http://localhost:8080/api';

class ApiService {
  async createSession(personDescription: string): Promise<SessionResponse> {
    const response = await fetch(`${API_BASE_URL}/session/create`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ personDescription } as PersonDescriptionRequest),
    });

    if (!response.ok) {
      throw new Error('Failed to create session');
    }

    return response.json();
  }

  async getSuggestions(sessionId: string): Promise<BucketListSuggestion[]> {
    const response = await fetch(`${API_BASE_URL}/suggestions/${sessionId}`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });

    if (response.status === 401) {
      throw new Error('API key required - please configure your OpenAI API key');
    }

    if (!response.ok) {
      throw new Error('Failed to get suggestions');
    }

    const data: SuggestionsResponse = await response.json();
    return data.suggestions;
  }

  async acceptSuggestion(sessionId: string, suggestionId: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/suggestions/accept`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId, suggestionId } as AcceptRequest),
    });

    if (!response.ok) {
      throw new Error('Failed to accept suggestion');
    }
  }

  async rejectSuggestion(
    sessionId: string,
    suggestionId: string,
    reason: string,
    customReason: boolean
  ): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/suggestions/reject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ 
        sessionId, 
        suggestionId, 
        reason, 
        customReason 
      } as RejectRequest),
    });

    if (!response.ok) {
      throw new Error('Failed to reject suggestion');
    }
  }

  async getAcceptedSuggestions(sessionId: string): Promise<BucketListSuggestion[]> {
    const response = await fetch(`${API_BASE_URL}/suggestions/accepted/${sessionId}`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });

    if (!response.ok) {
      throw new Error('Failed to get accepted suggestions');
    }

    const data: SuggestionsResponse = await response.json();
    return data.suggestions;
  }

  async getRejectedSuggestions(sessionId: string): Promise<RejectedBucketListSuggestion[]> {
    const response = await fetch(`${API_BASE_URL}/suggestions/rejected/${sessionId}`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });

    if (!response.ok) {
      throw new Error('Failed to get rejected suggestions');
    }

    const data: RejectedSuggestionsResponse = await response.json();
    return data.suggestions;
  }

  async getNextSuggestion(sessionId: string): Promise<BucketListSuggestion | null> {
    const response = await fetch(`${API_BASE_URL}/suggestions/next/${sessionId}`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });

    if (response.status === 404) {
      return null; // No more suggestions available
    }

    if (response.status === 401) {
      throw new Error('API key required - please configure your OpenAI API key');
    }

    if (!response.ok) {
      throw new Error('Failed to get next suggestion');
    }

    return response.json();
  }

  async regenerateSuggestions(sessionId: string): Promise<BucketListSuggestion[]> {
    const response = await fetch(`${API_BASE_URL}/suggestions/regenerate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId }),
    });

    if (response.status === 401) {
      throw new Error('API key required - please configure your OpenAI API key');
    }

    if (!response.ok) {
      throw new Error('Failed to regenerate suggestions');
    }

    const data: SuggestionsResponse = await response.json();
    return data.suggestions;
  }

  async checkApiKeyStatus(): Promise<boolean> {
    const response = await fetch(`${API_BASE_URL}/config/api-key/status`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });

    if (!response.ok) {
      throw new Error('Failed to check API key status');
    }

    const data = await response.json();
    return data.hasValidKey;
  }

  async submitApiKey(apiKey: string): Promise<boolean> {
    const response = await fetch(`${API_BASE_URL}/config/api-key`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ apiKey }),
    });

    if (!response.ok) {
      throw new Error('Failed to validate API key');
    }

    const data = await response.json();
    return data.valid;
  }
}

export const apiService = new ApiService();