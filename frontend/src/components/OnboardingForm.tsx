import { useState } from 'react';
import type { Gender, CreateProfileRequest } from '../types';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Select, SelectItem } from './ui/select';

interface OnboardingFormProps {
  onSubmit: (profile: CreateProfileRequest) => void;
  loading?: boolean;
}

export function OnboardingForm({ onSubmit, loading = false }: OnboardingFormProps) {
  const [gender, setGender] = useState<Gender>('UNSPECIFIED');
  const [age, setAge] = useState<number>(25);
  const [capital, setCapital] = useState<number>(50000);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({ gender, age, capital });
  };

  return (
    <Card className="w-full max-w-md">
      <CardHeader className="text-center">
        <CardTitle>Create Your Profile</CardTitle>
        <CardDescription>
          Tell us about yourself so we can generate personalized suggestions
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="gender">Gender</Label>
            <Select
              value={gender}
              onValueChange={(value) => setGender(value as Gender)}
              placeholder="Select gender"
            >
              <SelectItem value="MALE">Male</SelectItem>
              <SelectItem value="FEMALE">Female</SelectItem>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="age">Age</Label>
            <Input
              id="age"
              type="text"
              placeholder="25"
              value={age.toString()}
              onChange={(e) => {
                const val = e.target.value.replace(/\D/g, '');
                const numVal = parseInt(val) || 0;
                if (numVal >= 18 && numVal <= 100) {
                  setAge(numVal);
                } else if (val === '') {
                  setAge(18);
                }
              }}
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="capital">Budget for leisure activities (SEK)</Label>
            <Input
              id="capital"
              type="text"
              placeholder="50000"
              value={capital.toLocaleString()}
              onChange={(e) => {
                const val = e.target.value.replace(/\D/g, '');
                const numVal = parseInt(val) || 0;
                setCapital(numVal);
              }}
              required
            />
            <p className="text-xs text-muted-foreground">
              Approximate budget for the next 12-24 months
            </p>
          </div>


          <Button 
            type="submit" 
            disabled={loading}
            className="w-full"
            size="lg"
          >
            {loading ? 'Creating profile...' : 'Create profile'}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}