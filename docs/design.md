# Design

Cohort's interface is built around one idea: a job search is a transit line. Each pipeline stage is a station, upcoming interviews are departures, and stalled applications are delayed. The look borrows from station signage and departure boards: ink and paper, one signal yellow, condensed capitals and mono numbers.

## Users and tasks

Students and early-career job seekers running many applications at once, mostly on a laptop and sometimes on a phone between classes. The main tasks are: see where every application stands, act on what is stalled or coming up, and practise for the next interview. Screens favour scanning and quick actions over decoration.

## Colour

Tokens live in `frontend/src/index.css`, light theme under `:root` and dark under `.dark`. `frontend/src/lib/contrast.test.ts` checks every text pair at 4.5:1 and every UI colour at 3:1 in both themes.

| Token | Use |
|---|---|
| `background`, `card` | Warm paper (light) or near-black (dark) |
| `foreground` | Ink |
| `primary` | Ink in light mode with yellow text on top, yellow in dark mode with ink on top. Main buttons and the departures title bar |
| `signal` | Yellow in both themes. The call-to-action button, active markers, highlights. Always with ink text, never yellow text on paper |
| `rule` | Heavy rules under page headers and on top of panels. Ink in light mode, yellow in dark |
| `line-saved` ... `line-closed` | One colour per pipeline stage. Used on the route, board columns, ticket edges and status chips, always next to a text label |
| `sidebar` | The sidebar and phone navigation are ink in both themes |

## Type

- Display: Bricolage Grotesque at its condensed width, heavy, in capitals (`text-display`). Page titles, panel titles, company names on the board, big numbers.
- Body: Geist.
- Labels and figures: Geist Mono in small spaced capitals (`board-label`, `eyebrow`) for column headings and section labels; `tabular` for dates, times and counts.

## Shape

- 4px radius at most. No pills, no shadows, no glows, no blur.
- Page headers end in a 2px rule. Panels (`SectionCard`, stat tiles) have a 3px top edge in the rule colour.
- Tables have mono column headings over a 2px rule.

## Signature components

- `RouteLine` (`components/board/RouteLine.tsx`): the pipeline as stations on a coloured line, horizontal on wide screens and vertical on phones.
- `FlapText` (`components/board/FlapText.tsx`): text that settles one character at a time, like a split-flap board. Used for counts, dates and times.
- The departures board on the dashboard: an ink or yellow title bar over date, time, company and round columns.
- Status chips: a square in the stage's line colour and the stage name in mono capitals.

## Motion

Motion is used only where it explains something:

- Switching theme grows the new theme out from the clicked button.
- The route draws in once, station by station, when the dashboard loads.
- Board numbers settle like a split-flap display when they load or change.
- Rows get a yellow edge marker on hover and focus.

Every animation is skipped when the user prefers reduced motion.

## Writing

Short and plain. Page titles are nouns. No exclamation marks, no slogans, no em dashes.
