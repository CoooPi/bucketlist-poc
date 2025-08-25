import React, { useEffect, useState } from 'react';
import { ApiKeyInputPage } from './ApiKeyInputPage';

interface ApiKeyGuardProps {
  children: React.ReactNode;
  checkApiKeyStatus: () => Promise<boolean>;
  submitApiKey: (apiKey: string) => Promise<boolean>;
}

export const ApiKeyGuard: React.FC<ApiKeyGuardProps> = ({ 
  children, 
  checkApiKeyStatus, 
  submitApiKey 
}) => {
  const [hasValidKey, setHasValidKey] = useState<boolean | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const checkStatus = async () => {
      try {
        const status = await checkApiKeyStatus();
        setHasValidKey(status);
      } catch {
        setError('Failed to check API key status');
        setHasValidKey(false);
      }
    };
    
    checkStatus();
  }, [checkApiKeyStatus]);

  const handleApiKeySubmit = async (apiKey: string): Promise<boolean> => {
    try {
      setError(null);
      const success = await submitApiKey(apiKey);
      if (success) {
        setHasValidKey(true);
        return true;
      }
      return false;
    } catch {
      setError('Failed to validate API key');
      return false;
    }
  };

  // Still checking status
  if (hasValidKey === null) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Checking API key status...</p>
        </div>
      </div>
    );
  }

  // No valid key, show input page
  if (!hasValidKey) {
    return (
      <ApiKeyInputPage 
        onApiKeySubmit={handleApiKeySubmit}
        error={error || undefined}
      />
    );
  }

  // Valid key exists, show protected content
  return <>{children}</>;
};