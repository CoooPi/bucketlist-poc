import React, { useState } from 'react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';

interface PersonDescriptionInputProps {
  onSubmit: (description: string) => void;
  isLoading?: boolean;
}

export function PersonDescriptionInput({ onSubmit, isLoading = false }: PersonDescriptionInputProps) {
  const [description, setDescription] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (description.trim()) {
      onSubmit(description.trim());
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl font-bold">Describe a Person</CardTitle>
          <p className="text-gray-600">
            Tell us about someone you'd like to create bucket list suggestions for
          </p>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              type="text"
              placeholder="e.g., My adventurous friend who loves travel and trying new foods..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={isLoading}
              className="w-full"
            />
            <Button 
              type="submit" 
              className="w-full"
              disabled={!description.trim() || isLoading}
            >
              {isLoading ? 'Generating Suggestions...' : 'Get Suggestions'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}