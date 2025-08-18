import { useState } from 'react';
import type { Gender, Mode, CreateProfileRequest } from '../types';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';

interface OnboardingFormProps {
  onSubmit: (profile: CreateProfileRequest) => void;
  loading?: boolean;
}

export function OnboardingForm({ onSubmit, loading = false }: OnboardingFormProps) {
  const [gender, setGender] = useState<Gender>('UNSPECIFIED');
  const [age, setAge] = useState<number>(25);
  const [capital, setCapital] = useState<number>(50000);
  const [mode, setMode] = useState<Mode>('CREATIVE');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({ gender, age, capital, mode });
  };

  return (
    <Card className="w-full max-w-md">
      <CardHeader className="text-center">
        <CardTitle>Skapa din profil</CardTitle>
        <CardDescription>
          Berätta lite om dig själv så genererar vi personliga förslag
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="gender">Kön</Label>
            <select
              id="gender"
              value={gender}
              onChange={(e) => setGender(e.target.value as Gender)}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              <option value="MALE">Man</option>
              <option value="FEMALE">Kvinna</option>
              <option value="OTHER">Annat</option>
              <option value="UNSPECIFIED">Vill inte ange</option>
            </select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="age">Ålder</Label>
            <Input
              id="age"
              type="number"
              min="18"
              max="100"
              value={age}
              onChange={(e) => setAge(parseInt(e.target.value))}
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="capital">Budget för fritidsaktiviteter (SEK)</Label>
            <Input
              id="capital"
              type="number"
              min="0"
              step="1000"
              value={capital}
              onChange={(e) => setCapital(parseInt(e.target.value))}
              required
            />
            <p className="text-xs text-muted-foreground">
              Ungefärlig budget för kommande 12-24 månader
            </p>
          </div>

          <div className="space-y-3">
            <Label>Förslag-stil</Label>
            <div className="space-y-3">
              <div className="flex items-center space-x-2">
                <input
                  type="radio"
                  id="creative"
                  value="CREATIVE"
                  checked={mode === 'CREATIVE'}
                  onChange={(e) => setMode(e.target.value as Mode)}
                  className="h-4 w-4"
                />
                <Label htmlFor="creative" className="text-sm font-normal cursor-pointer">
                  <span className="font-medium">Kreativ</span> - Överraskande och unika förslag
                </Label>
              </div>
              <div className="flex items-center space-x-2">
                <input
                  type="radio"
                  id="aligned"
                  value="ALIGNED"
                  checked={mode === 'ALIGNED'}
                  onChange={(e) => setMode(e.target.value as Mode)}
                  className="h-4 w-4"
                />
                <Label htmlFor="aligned" className="text-sm font-normal cursor-pointer">
                  <span className="font-medium">Anpassad</span> - Populära och beprövade aktiviteter
                </Label>
              </div>
            </div>
          </div>

          <Button 
            type="submit" 
            disabled={loading}
            className="w-full"
            size="lg"
          >
            {loading ? 'Skapar profil...' : 'Skapa profil'}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}