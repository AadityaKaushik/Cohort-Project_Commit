import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ProfileForm } from '@/components/account/ProfileForm'
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
      <div>
        <h2 className="text-lg font-semibold">Profile</h2>
        <p className="text-sm text-muted-foreground">{user?.email}</p>
      </div>

      <ProfileForm />

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Change password</CardTitle>
          <CardDescription>You will be signed out everywhere and asked to sign in again.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={submitPassword}>
            <div className="space-y-1.5">
              <Label htmlFor="current-password">Current password</Label>
              <Input id="current-password" type="password" autoComplete="current-password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="new-password">New password</Label>
              <Input id="new-password" type="password" autoComplete="new-password" minLength={10} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
            </div>
            {passwordMutation.isError ? (
              <p role="alert" className="text-sm text-destructive">
                {messageOf(passwordMutation.error, 'Could not change the password.')}
              </p>
            ) : null}
            <Button type="submit" disabled={passwordMutation.isPending}>
              Change password
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Export my data</CardTitle>
          <CardDescription>Download everything you have entered as a JSON file, including your resume files.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {exportMutation.isError ? (
            <p role="alert" className="text-sm text-destructive">
              {messageOf(exportMutation.error, 'Could not export your data.')}
            </p>
          ) : null}
          <Button variant="outline" onClick={() => exportMutation.mutate()} disabled={exportMutation.isPending}>
            {exportMutation.isPending ? 'Preparing...' : 'Download my data'}
          </Button>
        </CardContent>
      </Card>

      <Card className="border-destructive/50">
        <CardHeader>
          <CardTitle className="text-base text-destructive">Delete account</CardTitle>
          <CardDescription>
            Permanently removes your account, applications, resumes, files and federated-learning data. This cannot be undone.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="space-y-1.5">
            <Label htmlFor="delete-confirmation">Type {DELETE_CONFIRMATION} to confirm</Label>
            <Input id="delete-confirmation" value={confirmation} onChange={(e) => setConfirmation(e.target.value)} autoComplete="off" />
          </div>
          {deleteMutation.isError ? (
            <p role="alert" className="text-sm text-destructive">
              {messageOf(deleteMutation.error, 'Could not delete the account.')}
            </p>
          ) : null}
          <Button
            variant="destructive"
            onClick={() => deleteMutation.mutate()}
            disabled={confirmation !== DELETE_CONFIRMATION || deleteMutation.isPending}
          >
            Delete my account
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
