import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { DownloadIcon, Trash2Icon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ProfileForm } from '@/components/account/ProfileForm'
import { CompanyMark } from '@/components/shared/CompanyMark'
import { FormActions, FormError, FormField } from '@/components/shared/Form'
import { PageHeader } from '@/components/shared/PageHeader'
import { SectionCard } from '@/components/shared/SectionCard'
import { useAuth } from '@/hooks/use-auth'
import { ApiError } from '@/lib/api-client'
import { saveBlob } from '@/lib/download'
import { changeMyPassword, deleteMyAccount, exportMyData } from '@/lib/users-api'

const DELETE_CONFIRMATION = 'DELETE'

function messageOf(cause: unknown, fallback: string) {
  return cause instanceof ApiError ? cause.message : fallback
}

/** US-06/07/08: change password, export everything, delete the account. */
export function AccountPage() {
  const { logout, user } = useAuth()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')

  const passwordMutation = useMutation({
    mutationFn: () => changeMyPassword({ current_password: currentPassword, new_password: newPassword }),
    onSuccess: () => logout(),
  })
  const exportMutation = useMutation({
    mutationFn: exportMyData,
    onSuccess: ({ blob, filename }) => saveBlob(blob, filename ?? 'cohort-data-export.json'),
  })
  const deleteMutation = useMutation({
    mutationFn: () => deleteMyAccount(confirmation),
    onSuccess: () => logout(),
  })

  function submitPassword(event: FormEvent) {
    event.preventDefault()
    passwordMutation.mutate()
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <PageHeader
        title="Profile"
        description={user?.email}
        meta={
          user ? (
            <span className="flex items-center gap-2 text-xs text-muted-foreground">
              <CompanyMark name={user.display_name ?? user.email ?? 'You'} className="size-6 rounded-full text-[11px]" />
              {user.display_name}
            </span>
          ) : undefined
        }
      />

      <div className="stagger space-y-6">
        <ProfileForm />

        <SectionCard title="Change password" description="You will be signed out everywhere and asked to sign in again.">
          <form className="space-y-4" onSubmit={submitPassword}>
            <div className="grid gap-4 sm:grid-cols-2">
              <FormField id="current-password" label="Current password">
                <Input id="current-password" type="password" autoComplete="current-password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required />
              </FormField>
              <FormField id="new-password" label="New password" hint="At least 10 characters.">
                <Input id="new-password" type="password" autoComplete="new-password" minLength={10} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
              </FormField>
            </div>
            {passwordMutation.isError ? <FormError>{messageOf(passwordMutation.error, 'Could not change the password.')}</FormError> : null}
            <FormActions>
              <Button type="submit" disabled={passwordMutation.isPending}>
                Change password
              </Button>
            </FormActions>
          </form>
        </SectionCard>

        <SectionCard
          title="Export my data"
          description="Download everything you have entered as a JSON file, including your resume files."
          action={
            <Button variant="outline" onClick={() => exportMutation.mutate()} disabled={exportMutation.isPending}>
              <DownloadIcon aria-hidden />
              {exportMutation.isPending ? 'Preparing...' : 'Download my data'}
            </Button>
          }
        >
          {exportMutation.isError ? <FormError>{messageOf(exportMutation.error, 'Could not export your data.')}</FormError> : null}
        </SectionCard>

        <SectionCard
          tone="danger"
          title="Delete account"
          description="Permanently removes your account, applications, resumes, files and federated-learning data. This cannot be undone."
        >
          <div className="space-y-4">
            <FormField id="delete-confirmation" label={`Type ${DELETE_CONFIRMATION} to confirm`} className="sm:max-w-xs">
              <Input id="delete-confirmation" value={confirmation} onChange={(e) => setConfirmation(e.target.value)} autoComplete="off" />
            </FormField>
            {deleteMutation.isError ? <FormError>{messageOf(deleteMutation.error, 'Could not delete the account.')}</FormError> : null}
            <Button
              variant="destructive"
              onClick={() => deleteMutation.mutate()}
              disabled={confirmation !== DELETE_CONFIRMATION || deleteMutation.isPending}
            >
              <Trash2Icon aria-hidden />
              Delete my account
            </Button>
          </div>
        </SectionCard>
      </div>
    </div>
  )
}
