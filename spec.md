# Bucket List and Rejected Suggestions Display Feature

## Overview

This feature adds two sidebar components to enhance the suggestion experience by showing the user's accepted bucket list items and rejected suggestions. This provides better context and helps users track their progress and past decisions.

## Feature Requirements

### 1. Your Bucket List Card (Right Side)

#### Location & Layout
- Positioned to the right of the main SuggestionCard component
- Fixed width card with consistent styling using Shadcn components

#### Components & Structure
1. **Progress Bar Section (Top)**
   - Shadcn Progress component showing percentage of total capital used
   - Formula: `(sum of all accepted suggestion costs / user capital) * 100`
   - Visual indicator of budget utilization
   - Label showing "X% of budget used" or similar

2. **Accepted Suggestions List**
   - Shadcn ScrollArea component for scrollable list
   - Each accepted suggestion displayed as a compact card
   - Ordered by acceptance date (most recent first)

#### Individual Bucket List Item Card
- **Title**: Suggestion title (truncated if too long)
- **Description**: Brief description (truncated if needed)
- **Budget Breakdown**: Collapsible or compact view of cost categories
- **Total Cost**: Prominently displayed formatted currency
- **Visual Design**: Compact card with consistent spacing

### 2. Rejected Suggestions Card (Left Side)

#### Location & Layout
- Positioned to the left of the main SuggestionCard component
- Similar width and styling to the bucket list card

#### Components & Structure
1. **Header**
   - "Rejected Suggestions" title
   - No progress bar (budget tracking not relevant)

2. **Rejected Suggestions List**
   - Shadcn ScrollArea component for scrollable list
   - Each rejected suggestion displayed as a compact card
   - Ordered by rejection date (most recent first)

#### Individual Rejected Item Card
- **Title**: Suggestion title (truncated if too long)
- **Description**: Brief description (truncated if needed)
- **Budget Breakdown**: Collapsible or compact view of cost categories
- **Total Cost**: Formatted currency display
- **Rejection Reason**: If provided, displayed at bottom in muted text
- **Visual Design**: Slightly muted/grayed styling to indicate rejected status

## Technical Implementation

### 1. Data Requirements

#### Backend API Enhancements
- Existing `getAcceptedSuggestions(profileId)` endpoint  (already available)
- New endpoint: `getRejectedSuggestions(profileId)` returning rejected suggestions with reasons
- Both endpoints should return suggestions ordered by feedback creation date (desc)

#### Data Models
```typescript
interface BucketListItem {
  id: string;
  title: string;
  description: string;
  budgetBreakdown: BudgetItem[];
  totalCost: number;
  acceptedAt: string; // ISO date
}

interface RejectedItem {
  id: string;
  title: string;
  description: string;
  budgetBreakdown: BudgetItem[];
  totalCost: number;
  rejectedAt: string; // ISO date
  reason?: string; // Optional rejection reason
}
```

### 2. Frontend Components

#### New Components to Create
1. **BucketListCard**
   - Progress bar calculation and display
   - Scrollable list of accepted items
   - Individual bucket list item cards

2. **RejectedSuggestionsCard**
   - Scrollable list of rejected items
   - Individual rejected item cards with reason display

3. **SuggestionItemCard** (Shared Component)
   - Reusable compact card for both accepted and rejected items
   - Props for styling variants (accepted vs rejected)
   - Collapsible budget breakdown

#### Layout Updates
- **Main Suggestions Page**: Update to three-column layout
  - Left: RejectedSuggestionsCard (25% width)
  - Center: Current SuggestionCard (50% width)
  - Right: BucketListCard (25% width)
- **Responsive Design**: Stack vertically on smaller screens

### 3. State Management

#### App State Additions
```typescript
const [acceptedSuggestions, setAcceptedSuggestions] = useState<BucketListItem[]>([]);
const [rejectedSuggestions, setRejectedSuggestions] = useState<RejectedItem[]>([]);
```

#### Data Fetching
- Load accepted and rejected suggestions when entering suggestions view
- Update lists in real-time when user accepts/rejects suggestions
- Implement loading states for both sidebar cards

## User Experience Flow

### 1. Initial Load
1. User completes onboarding and category selection
2. Main suggestion loads in center
3. Sidebar cards load accepted/rejected suggestions (if any exist)
4. Progress bar shows current budget utilization

### 2. User Accepts Suggestion
1. Suggestion added to bucket list card (top of list)
2. Progress bar updates to reflect new budget usage
3. New suggestion loads in center
4. Smooth animations for list updates

### 3. User Rejects Suggestion
1. If reason provided, suggestion added to rejected list with reason
2. If no reason, suggestion added to rejected list without reason
3. New suggestion loads in center
4. Rejected card updates with most recent rejection at top

## Visual Design Specifications

### 1. Progress Bar
- **Colors**: Standard styling for 0-100%, red color when over 100% (over-budget indicator)
- **Label**: "X% of Y SEK budget used"
- **Positioning**: Top of bucket list card with adequate padding
- **Note**: Pure display feature - shows calculated percentage regardless of value (could be 500%)

### 2. Card Styling
- **Accepted Items**: Standard card styling with slight green accent
- **Rejected Items**: Muted colors (gray text, lighter background)
- **Spacing**: Consistent padding and margins between items
- **Truncation**: Long titles/descriptions truncated with ellipsis

### 3. Responsive Behavior
- **Desktop**: Three-column layout as specified
- **Tablet**: Stack rejected suggestions below main card, bucket list to right
- **Mobile**: Single column with collapsible sidebar sections

## Implementation Priority

### Phase 1: Backend Enhancements
1. Create `getRejectedSuggestions` endpoint
2. Ensure proper ordering by feedback date
3. Include rejection reasons in response

### Phase 2: Core Components
1. Create shared `SuggestionItemCard` component
2. Implement `BucketListCard` with progress bar
3. Implement `RejectedSuggestionsCard`

### Phase 3: Integration & Polish
1. Update main layout for three-column design
2. Add real-time updates on accept/reject actions
3. Implement responsive design
4. Add loading states and error handling

### Phase 4: Enhancements
1. Add collapse/expand functionality for budget breakdowns
2. Implement smooth animations for list updates
3. Add keyboard navigation support
4. Performance optimization for large lists

## Success Metrics

### User Experience
- Users can easily track their bucket list progress
- Clear visibility of budget utilization
- Historical context helps inform future decisions

### Technical Performance
- Fast loading of sidebar content
- Smooth real-time updates
- Responsive design works across devices
- Minimal impact on main suggestion flow

## Edge Cases & Considerations

### Data Handling
- **Empty States**: Appropriate messaging when no accepted/rejected items exist
- **Large Lists**: Virtualization for performance with many items
- **Network Errors**: Graceful handling of API failures

### Budget Calculations
- **Over-Budget**: Simply display the percentage (could be >100%) with red styling for over-budget indicator
- **Decimal Precision**: Ensure accurate percentage calculations
- **Currency Formatting**: Consistent formatting across all cost displays
- **No Business Logic**: Pure display feature with no validation or constraints

### User Interaction
- **Card Interactions**: Hover states and potential click actions
- **Accessibility**: Proper ARIA labels and keyboard navigation
- **Loading States**: Clear indicators when data is being fetched

This feature will significantly enhance the user experience by providing context, progress tracking, and historical reference while maintaining the focus on the primary suggestion flow.