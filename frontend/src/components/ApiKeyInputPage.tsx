import React, { useState } from 'react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';

interface ApiKeyInputPageProps {
  onApiKeySubmit: (apiKey: string) => Promise<boolean>;
  error?: string;
}

export const ApiKeyInputPage: React.FC<ApiKeyInputPageProps> = ({ onApiKeySubmit, error }) => {
  const [apiKey, setApiKey] = useState('');
  const [isValidating, setIsValidating] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!apiKey.trim()) {
      setLocalError('Please enter an API key');
      return;
    }

    setIsValidating(true);
    setLocalError(null);

    try {
      const isValid = await onApiKeySubmit(apiKey.trim());
      if (!isValid) {
        setLocalError('Invalid API key. Please check your key and try again.');
      }
    } catch {
      setLocalError('Failed to validate API key. Please try again.');
    } finally {
      setIsValidating(false);
    }
  };

  const displayError = error || localError;

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl font-bold">Enter OpenAI API Key</CardTitle>
          <CardDescription>
            To use the bucket list generator, please provide your OpenAI API key. 
            Your key will be stored securely in memory and never logged or persisted.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="apiKey">OpenAI API Key</Label>
              <Input
                id="apiKey"
                type="password"
                placeholder="sk-..."
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                className={displayError ? 'border-destructive' : ''}
                disabled={isValidating}
              />
              {displayError && (
                <p className="text-sm text-destructive">{displayError}</p>
              )}
            </div>
            
            <div className="space-y-2">
              <Button 
                type="submit" 
                className="w-full" 
                disabled={isValidating || !apiKey.trim()}
              >
                {isValidating ? 'Validating...' : 'Validate and Continue'}
              </Button>
            </div>

            <div className="text-xs text-muted-foreground space-y-1">
              <p>• Your API key is validated against OpenAI servers</p>
              <p>• The key is stored only in memory while the app runs</p>
              <p>• Get your API key from: platform.openai.com</p>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};