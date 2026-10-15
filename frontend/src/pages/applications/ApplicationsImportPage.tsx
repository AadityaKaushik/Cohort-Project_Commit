import { useNavigate } from 'react-router-dom'
import { SaveFromSitesCard } from '@/components/applications/SaveFromSitesCard'
import { CsvImport } from '@/components/applications/CsvImport'
import { PageHeader } from '@/components/shared/PageHeader'
import { SectionCard } from '@/components/shared/SectionCard'

export function ApplicationsImportPage() {
  const navigate = useNavigate()

  return (
    <div className="max-w-4xl space-y-6">
      <PageHeader
        back={{ to: '/applications', label: 'Applications' }}
        title="Import applications"
        description="Bring in a spreadsheet you already keep. You see a preview before anything is saved."
      />
      <SectionCard>
        <CsvImport onImported={() => navigate('/applications')} />
      </SectionCard>
      <SaveFromSitesCard />
    </div>
  )
}
