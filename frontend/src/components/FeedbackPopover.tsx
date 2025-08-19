import { useState } from 'react';
import { Popover, PopoverContent, PopoverTrigger } from './ui/popover';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';

interface FeedbackPopoverProps {
  children: React.ReactNode;
  onFeedbackSubmit: (reason?: string) => void;
}

export function FeedbackPopover({ children, onFeedbackSubmit }: FeedbackPopoverProps) {
  const [open, setOpen] = useState(false);
  const [feedback, setFeedback] = useState('');

  const handleSubmit = () => {
    onFeedbackSubmit(feedback.trim() || undefined);
    setFeedback('');
    setOpen(false);
  };

  const handleSkip = () => {
    onFeedbackSubmit();
    setFeedback('');
    setOpen(false);
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        {children}
      </PopoverTrigger>
      <PopoverContent className="w-80" align="center">
        <div className="space-y-4">
          <div className="space-y-2">
            <h4 className="font-medium leading-none">Help us improve</h4>
            <p className="text-sm text-muted-foreground">
              Would you like to tell us why this suggestion wasn't right for you? (Optional)
            </p>
          </div>
          <div className="space-y-2">
            <Label htmlFor="feedback">Your feedback</Label>
            <Input
              id="feedback"
              placeholder="e.g., Too expensive, Not interested in this type of activity..."
              value={feedback}
              onChange={(e) => setFeedback(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  handleSubmit();
                }
              }}
            />
          </div>
          <div className="flex gap-2">
            <Button 
              variant="outline" 
              size="sm" 
              onClick={handleSkip}
              className="flex-1"
            >
              Skip
            </Button>
            <Button 
              size="sm" 
              onClick={handleSubmit}
              className="flex-1"
            >
              Submit Feedback
            </Button>
          </div>
        </div>
      </PopoverContent>
    </Popover>
  );
}