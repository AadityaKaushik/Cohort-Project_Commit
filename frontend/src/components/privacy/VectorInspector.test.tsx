import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { VectorInspector } from '@/components/privacy/VectorInspector'

describe('VectorInspector', () => {
  it('renders every value of the transmitted vector, not a summary', () => {
    render(<VectorInspector vector={[0.123456, -0.987654, 0.5]} />)

    expect(screen.getByText('Transmitted vector (3 values)')).toBeInTheDocument()
    expect(screen.getByText('0.123456')).toBeInTheDocument()
    expect(screen.getByText('-0.987654')).toBeInTheDocument()
    expect(screen.getByText('0.500000')).toBeInTheDocument()
    expect(screen.getByText('[0]')).toBeInTheDocument()
    expect(screen.getByText('[2]')).toBeInTheDocument()
  })

  it('labels values with schema feature names when they line up with the vector', () => {
    render(<VectorInspector vector={[0.1, 0.2]} featureNames={['mastery_mean', 'confidence_trend']} />)

    expect(screen.getByText('mastery_mean')).toBeInTheDocument()
    expect(screen.getByText('confidence_trend')).toBeInTheDocument()
    expect(screen.queryByText('[0]')).not.toBeInTheDocument()
  })

  it('falls back to indexes when the schema does not match the vector length', () => {
    render(<VectorInspector vector={[0.1, 0.2, 0.3]} featureNames={['only_one']} />)

    expect(screen.queryByText('only_one')).not.toBeInTheDocument()
    expect(screen.getByText('[1]')).toBeInTheDocument()
  })

  it('labels a model update as one change per feature weight plus the intercept', () => {
    render(<VectorInspector vector={[0.1, -0.2, 0.3]} featureNames={['mastery_mean', 'coverage_ratio']} />)

    expect(screen.getByText(/model update sent \(3 values\)/i)).toBeInTheDocument()
    expect(screen.getByText('coverage_ratio weight')).toBeInTheDocument()
    expect(screen.getByText('intercept')).toBeInTheDocument()
  })
})
