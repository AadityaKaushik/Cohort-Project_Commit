import { SHORTCUTS } from '@/hooks/use-shortcuts'

export function ShortcutList() {
  return (
    <ul className="divide-y text-sm">
      {SHORTCUTS.map((shortcut) => (
        <li key={shortcut.label} className="flex items-center justify-between py-2">
          <span>{shortcut.label}</span>
          <span className="flex items-center gap-1">
            {shortcut.keys.map((key, index) => (
              <span key={key} className="flex items-center gap-1">
                {index > 0 && <span className="text-xs text-muted-foreground">then</span>}
                <kbd className="kbd">{key}</kbd>
              </span>
            ))}
          </span>
        </li>
      ))}
      <li className="flex items-center justify-between py-2">
        <span>Open the command palette</span>
        <span className="flex items-center gap-1">
          <kbd className="kbd">Ctrl</kbd>
          <kbd className="kbd">K</kbd>
          <span className="text-xs text-muted-foreground">(Cmd K on a Mac)</span>
        </span>
      </li>
    </ul>
  )
}
