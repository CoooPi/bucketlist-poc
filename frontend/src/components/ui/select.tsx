import * as React from "react"
import { ChevronDown } from "lucide-react"
import { cn } from "../../lib/utils"

interface SelectProps {
  value: string
  onValueChange: (value: string) => void
  placeholder?: string
  children: React.ReactNode
  className?: string
}

interface SelectItemProps {
  value: string
  children: React.ReactNode
}

const Select = ({ value, onValueChange, placeholder, children, className }: SelectProps) => {
  const [isOpen, setIsOpen] = React.useState(false)
  const [selectedLabel, setSelectedLabel] = React.useState('')

  React.useEffect(() => {
    // Find the selected item's label
    React.Children.forEach(children, (child) => {
      if (React.isValidElement<SelectItemProps>(child) && child.props.value === value) {
        setSelectedLabel(child.props.children as string)
      }
    })
  }, [value, children])

  return (
    <div className="relative">
      <button
        type="button"
        className={cn(
          "flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
          className
        )}
        onClick={() => setIsOpen(!isOpen)}
      >
        <span>{selectedLabel || placeholder}</span>
        <ChevronDown className="h-4 w-4 opacity-50" />
      </button>
      {isOpen && (
        <div className="absolute top-full z-50 mt-1 w-full rounded-md border bg-popover p-1 text-popover-foreground shadow-md">
          {React.Children.map(children, (child) => {
            if (React.isValidElement<SelectItemProps>(child)) {
              return (
                <div
                  key={child.props.value}
                  className="relative flex cursor-default select-none items-center rounded-sm px-2 py-1.5 text-sm outline-none hover:bg-accent hover:text-accent-foreground cursor-pointer"
                  onClick={() => {
                    onValueChange(child.props.value)
                    setIsOpen(false)
                  }}
                >
                  {child.props.children}
                </div>
              )
            }
            return child
          })}
        </div>
      )}
    </div>
  )
}

const SelectItem = ({ children }: SelectItemProps) => {
  return <>{children}</>
}

export { Select, SelectItem }