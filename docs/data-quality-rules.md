# Data quality rules

Cohort stores things users type: company names, job titles, links, salaries, dates, interview notes and company notes. This page records which checks the app applies to that input, why, and where they can be wrong.

The rules are plain code. There is no model call and no word list that grows over time. Clear junk is rejected with a field-level message. Doubtful input is saved if the user confirms, and the form shows a warning first.

## Prior art

Job trackers keep the application form small and mostly optional, and experience sites rely on moderation after posting. None of the pages we looked at describe an automatic gibberish or minimum-length check, so the text rules here come from the common failure cases below. The limits are our own choices.

## Data problems seen in this domain

- Paste accidents: whole job posts or page titles ("Careers | Acme - Apply now") in the title field.
- Placeholders and test input: "test", "asdf", "n/a", "xxx".
- Keyboard mash and repeated characters in free-text fields, often from a user who just wants to get past a required field.
- Off-topic text in an experience field, for example a comment about the canteen. It is real text, but it does not describe the interview or company.
- Unusable numbers: a salary of 5 with no currency, a minimum above the maximum, 120000 typed as a monthly figure.
- Impossible dates: year 0001 from a bad date picker, or a date years ahead.
- Personal details of other people (an interviewer's email or phone number) copied into notes.

## Field rules

| Field | Rule | Reason |
|---|---|---|
| Company name | Required, 1 to 200 characters after trimming, letters in any script, digits, spaces and common punctuation, at least one letter or digit, no control characters, no run of 6 or more identical letters or digits | Real names include "3M", "X", "H&M", "L'Oreal", "Zoho (India)". Length 1 is allowed because short names exist |
| Job title | Required, 1 to 300 characters, same character rules | Titles carry slashes, plus signs and brackets ("Engineer (Backend) / SRE", "C++ Developer") |
| Location | Optional, up to 200 characters, same character rules | "Remote - EU", "Bengaluru, KA" |
| Source | Optional, up to 100 characters, same character rules | Free label like "LinkedIn" or "Referral" |
| Whitespace | Trimmed, runs of spaces and tabs collapsed to one space on single-line fields | Duplicate-detection and company grouping depend on it |
| Job URL | Optional, up to 2048 characters, must parse as http or https with a host | A link without a host or with another scheme (javascript:, file:) is not a job link |
| Date applied | Not before 2000-01-01, not in the future | Existing rule was no future date; the lower bound catches year 0001 and 1900 |
| Deadline | Not before 2000-01-01, not more than one year ahead | A deadline is a future date, but a year is the practical limit |
| Interview times | Not before 2000, not more than one year ahead | Same reasoning |
| Salary min and max | 0 to 100,000,000 each, min not above max | Catches typos by an order of magnitude without guessing the currency |
| Salary currency | Required when a salary is given, a real ISO 4217 code, stored upper case | Three letters alone would accept "ABC". The JDK knows the current code list |
| Interview duration | 1 to 600 minutes | Full onsite days are the longest realistic single entry |
| Interviewer name and role | Optional, up to 200 characters, same character rules | Names are not validated further because name formats vary too much |
| Recruiter email | Optional, valid address shape, up to 254 characters | Shape only. A valid shape does not prove the mailbox exists |
| Notes, feedback, company notes | See text quality below | Free text |

## Text quality checks

These run on notes, interview feedback and company notes. A field can be left empty. If text is given it must pass the hard checks.

Hard rejections:

1. Only whitespace.
2. No letters at all, in any script. This catches emoji-only, digits-only and punctuation-only text. Letters are Unicode category L, so Hindi, Arabic, Chinese, Cyrillic and accented text count as letters.
3. Very few letters: fewer than 15 percent of the non-space characters when the text has 10 or more of them. The threshold is low on purpose so that short code or complexity notes such as "arr[i] += 1; O(n)" pass.
4. A run of 8 or more identical letters or digits ("aaaaaaaa"). Dashes and other punctuation are ignored, because separators like "--------" are normal in pasted notes.
5. One token repeated 4 or more times ("test test test test"), or a single token made of a 1 to 4 character unit repeated 3 or more times ("asdfasdfasdf", "hahahahaha").
6. Keyboard mash: tokens of 6 or more ASCII letters where 60 percent or more of the adjacent letter pairs are neighbours on the same QWERTY row, plus a short list of common smash fragments (asdf, qwer, zxcv, hjkl, uiop). The text is rejected when mash tokens are at least half of all tokens. A real sentence with one odd token is not rejected.

Soft warnings (saved if the user chooses to keep the text):

- `OFF_TOPIC`: for interview and company experience fields only. The text is Latin-script, has at least 5 words, and mentions none of about 70 topic words and word starts (round, interview, question, offer, manager, culture, salary, approach, solution and so on). "The tea there was not good" lands here.
- `CONTAINS_CONTACT_DETAILS`: an email address or phone-like number in the text.
- `STOPWORDS_ONLY`: 3 or more words and every one is an English function word ("this is the and it").

Short real answers pass: "Rejected after round 2", "No feedback yet", "Offer accepted".

## Limits and false-positive risks

- The keyboard check assumes QWERTY. AZERTY and QWERTZ users produce different mash, and they are mostly not caught. That is a miss, not a false positive, and misses are accepted.
- Only ASCII tokens are tested for mash. Non-Latin scripts are never flagged as mash.
- Some real words sit on one keyboard row ("typewriter", "pretty"). The 60 percent threshold together with the 6 character minimum keeps those below the line, but an unusual acronym or a pasted identifier such as "qwertzuiop" can be flagged. The message tells the user what to change.
- The off-topic check only reads English and other Latin-script text. Spanish or German feedback with no English topic word would get a warning, which is why it is never a rejection.
- A text can pass every rule and still be useless ("Good good company I like it"). Rules cannot judge truth or helpfulness, and the app does not try.
- A letter ratio check could reject text that is mostly numbers, for example a list of timings. The 15 percent threshold and the 10 character minimum are there for that case.
- Emoji-only notes are rejected on purpose. A note with at least one word plus emoji passes.
- Phone-number detection looks for 9 or more digits in a number-like pattern. Order numbers and ID strings can match.

## Names, emails and phone numbers

The app does not try to find names in free text. It cannot do that reliably, and a wrong guess would change the user's own text. It only warns when an email address or phone number is present, because those are easy to recognise and are the details most likely to be someone else's. Nothing is redacted or removed. The text belongs to the user, who decides whether to keep it. The ICO guidance read for this says a name combined with a work email address is personal data about a living person, and that people can be identifiable indirectly, which is why a plain warning is the most the app does automatically.

Experience text in Cohort is private to the account that wrote it, so the public-site rules about naming people (Blind) are not enforced here. If notes are ever published or shared, that decision needs a separate rule.

Profanity is not checked. The notes are private, and a word list produces false positives on names and place names while missing most real abuse.

## Where the rules live

- Server: Bean Validation annotations in `com.cohort.api.validation`, used on the application and interview request records. Field messages use the existing `VALIDATION_ERROR` response with `fieldErrors`.
- Dry run: `POST /api/v1/validate/text` returns errors and warnings for a piece of text without saving it. Forms call it when a text field loses focus.
- Client: the same cheap checks in `frontend/src/lib/validation.ts` and `frontend/src/lib/text-quality.ts`, so the form can say what is wrong before a request goes out. The server stays the authority.

## Sources

Read for this page:

1. Huntr help centre, "Understanding the job card" - https://help.huntr.co/en/articles/12640406-understanding-the-job-card
2. Simplify help centre, "Adding custom applications" - https://help.simplify.jobs/en/articles/9861158-adding-custom-applications
3. Glassdoor, "When does Glassdoor remove user content?" - https://api.glassdoor.com/about/trust/when-is-content-removed/
4. Blind help centre, "What are the community policies?" - https://help.teamblind.com/article/77-community-policies
5. OWASP Input Validation Cheat Sheet - https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html
6. Wikipedia, "Keyboard mashing" - https://en.wikipedia.org/wiki/Keyboard_mashing
7. Wikipedia, "ISO 4217" - https://en.wikipedia.org/wiki/ISO_4217
8. Wikipedia, "Unicode character property" - https://en.wikipedia.org/wiki/Unicode_character_property
9. ICO, "What is personal data?" - https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/personal-information-what-is-it/what-is-personal-data/what-is-personal-data/

Seen only as search-result summaries (the pages returned an access error when fetched, so nothing here depends on their exact wording):

- LeetCode Discuss, "How to write an Interview Experience post" - https://leetcode.com/discuss/interview-experience/128008/how-to-write-an-interview-experience-post
- Glassdoor, "Our promise: transparency and trust" - https://www.glassdoor.com/about/trust/
- Levels.fyi, About - https://levels.fyi/about/
