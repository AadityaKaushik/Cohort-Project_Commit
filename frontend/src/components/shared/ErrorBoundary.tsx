import { Component, type ErrorInfo, type ReactNode } from 'react'
import { BugIcon, RotateCwIcon } from 'lucide-react'
import { FullPageState } from '@/components/shared/FullPageState'
import { ErrorState } from '@/components/shared/QueryStates'
import { Button } from '@/components/ui/button'

interface ErrorBoundaryProps {
  children: ReactNode
  variant?: 'page' | 'inline'
  resetKey?: string
}

interface ErrorBoundaryState {
  failed: boolean
  resetKey?: string
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { failed: false, resetKey: this.props.resetKey }

  static getDerivedStateFromError(): Partial<ErrorBoundaryState> {
    return { failed: true }
  }

  static getDerivedStateFromProps(props: ErrorBoundaryProps, state: ErrorBoundaryState): ErrorBoundaryState | null {
    if (props.resetKey !== state.resetKey) {
      return { failed: false, resetKey: props.resetKey }
    }
    return null
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error(error, info.componentStack)
  }

  private retry = () => this.setState({ failed: false })

  render() {
    if (!this.state.failed) {
      return this.props.children
    }
    if (this.props.variant === 'inline') {
      return (
        <ErrorState
          title="This page hit a problem"
          description="Something broke while showing this page. Try again, or go back to the dashboard."
          onRetry={this.retry}
          action={
            <Button asChild variant="outline" size="sm">
              <a href="/">Dashboard</a>
            </Button>
          }
        />
      )
    }
    return (
      <FullPageState
        icon={BugIcon}
        eyebrow="Something broke"
        title="Cohort ran into a problem"
        description="The page stopped working. Reloading usually fixes it. Your saved data is not affected."
        actions={
          <>
            <Button onClick={() => window.location.reload()}>
              <RotateCwIcon aria-hidden />
              Reload page
            </Button>
            <Button asChild variant="outline">
              <a href="/">Go to dashboard</a>
            </Button>
          </>
        }
      />
    )
  }
}
