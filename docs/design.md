# Design

Cohort's interface is built around one idea: a job search is a climb. Each pipeline stage is a step higher than the last, and an offer is the top step. Interview preparation uses the same picture: a topic climbs from "no data yet" to "strong" as you practise it. The look is pine ink on warm ivory, one apricot accent, condensed capitals and mono figures.

## Users and tasks

Students and early-career job seekers running many applications at once, mostly on a laptop and sometimes on a phone between classes. The main tasks are: see where every application stands, act on what is stalled or coming up, and practise for the next interview. Screens favour scanning and quick actions over decoration.

## Colour

Tokens live in `frontend/src/index.css`, light theme under `:root` and dark under `.dark`. `frontend/src/lib/contrast.test.ts` checks every text pair at 4.5:1 and every UI colour at 3:1 in both themes.

| Token | Use |
|---|---|
| `background`, `card` | Warm ivory (light) or deep pine (dark) |
| `foreground` | Pine ink in light mode, ivory in dark |
| `primary` | Pine in light mode, ivory in dark. Main buttons, links, progress fills |
| `brand` | The pine panel used on the landing hero, the sign-in side panel and the "Next interviews" title bar. Pine in both themes |
| `signal` | Apricot. The one call to action per screen, the active sidebar item, the top step and accent words in headlines. Always with pine text on it, and never used to fill large areas |
| `rule` | Heavy rules under page headers and on top of panels. Same as `foreground` |
| `line-saved` ... `line-final` | One colour per pipeline stage, from pale sage to deep pine in light mode (reversed in dark), so stages read as a progression, not a rainbow. Always next to a text label |
| `line-offer` | Apricot, the top of the climb |
| `line-closed` | Muted brick for rejected applications |
| `sidebar` | Pine in light mode, near-black pine in dark |

## Type

- Display: Bricolage Grotesque at its condensed width, heavy, in capitals (`text-display`). Page titles, panel titles and big numbers only.
- Body: Geist. Company names and anything a user typed stay in Geist at normal case so they are easy to read and truncate cleanly.
- Labels and figures: Geist Mono in small spaced capitals (`board-label`, `eyebrow`) for column headings and section labels; `tabular` for dates, times and counts.

## Shape

- 6px radius at most. No pills, no shadows, no glows, no blur.
- Page headers end in a 2px rule. Panels (`SectionCard`, stat tiles) have a 3px top edge in the rule colour.
- Tables have mono column headings over a 2px rule.

## Signature components

- `Ascent` (`components/board/Ascent.tsx`): stages drawn as a staircase with the count on each step. Used for the pipeline on the dashboard, the sign-in panel (ivory steps on pine) and the readiness levels on the prep page. On phones it turns into rows whose bar grows one step per stage.
- `ReadinessSteps` (`components/prep/MasteryBadge.tsx`): a three-step glyph next to each topic's mastery, filled to its level; a strong topic reaches the top step in apricot.
- The logo: a three-step stair in an apricot square.
- The landing page (`components/landing/`): an example application climbing the stages with what Cohort does at each one, three problems each paired with a small live example of the fix, a stage picker, a picture of federated learning, and a FAQ.
- Status chips: a square in the stage colour and the stage name in mono capitals.

## Motion

Motion is used only where it explains something:

- Switching theme grows the new theme out from the clicked button.
- The steps rise once, one after another, when the dashboard loads, and lift slightly on hover.
- Counts run up to their value on load; dates and times rise into place (`FlapText`).
- On the landing page, sections rise in as they scroll into view, the hero example steps through the stages (it pauses on hover), and summaries travel to the shared model in the privacy picture.
- Rows get an apricot edge marker on hover and focus.

Every animation is skipped when the user prefers reduced motion.

## Writing

Short and plain. Page titles are nouns. No exclamation marks, no slogans, no em dashes.
