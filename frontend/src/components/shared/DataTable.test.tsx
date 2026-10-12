import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { DataTable } from '@/components/shared/DataTable'

const mobile = vi.hoisted(() => ({ value: true }))
vi.mock('@/hooks/use-mobile', () => ({ useIsMobile: () => mobile.value }))

const columns = [
  { key: 'company', header: 'Company', render: (row: { company: string; role: string }) => row.company },
  { key: 'role', header: 'Role', render: (row: { company: string; role: string }) => row.role },
]
const rows = [{ company: 'Acme', role: 'Engineer' }]

describe('DataTable', () => {
  it('renders card rows on a narrow screen', () => {
    mobile.value = true
    render(<DataTable columns={columns} data={rows} getRowId={(row) => row.company} />)

    expect(screen.getByTestId('data-cards')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(screen.getByText('Role')).toBeInTheDocument()
  })

  it('renders a table on a wide screen', () => {
    mobile.value = false
    render(<DataTable columns={columns} data={rows} getRowId={(row) => row.company} />)

    expect(screen.getByRole('table')).toBeInTheDocument()
  })
})
