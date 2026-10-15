import { useEffect, useRef } from 'react'
import { buildBookmarklet } from '@/lib/bookmarklet'
import { SectionCard } from '@/components/shared/SectionCard'

export function SaveFromSitesCard() {
  const linkRef = useRef<HTMLAnchorElement>(null)

  useEffect(() => {
    // React refuses javascript: hrefs, so the bookmarklet is set on the element directly.
    linkRef.current?.setAttribute('href', buildBookmarklet(window.location.origin))
  }, [])

  return (
    <SectionCard title="Save jobs from other sites" description="Works on LinkedIn, Naukri, Indeed, Internshala and company career pages.">
      <ol className="list-decimal space-y-1 pl-5 text-sm">
        <li>
          Drag this link to your bookmarks bar:{' '}
          <a
            ref={linkRef}
            onClick={(event) => event.preventDefault()}
            className="rounded-md border px-2 py-1 font-medium"
            draggable
          >
            Save to Cohort
          </a>
        </li>
        <li>Open a job page and click the bookmark.</li>
        <li>Check the details in the form that opens, then save.</li>
      </ol>
      <p className="mt-3 text-xs text-muted-foreground">
        The bookmark only reads the page you are looking at, and only when you click it. Nothing is sent until you save the form.
      </p>
    </SectionCard>
  )
}
