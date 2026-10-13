/** A thin bar across the top of the window while a route chunk loads; it creeps forward and never claims to finish. */
export function RouteProgress() {
  return (
    <div role="progressbar" aria-label="Loading page" className="pointer-events-none fixed inset-x-0 top-0 z-[60] h-0.5 overflow-hidden">
      <span className="animate-route-progress block h-full origin-left bg-primary shadow-[0_0_8px_var(--color-primary)]" />
    </div>
  )
}
