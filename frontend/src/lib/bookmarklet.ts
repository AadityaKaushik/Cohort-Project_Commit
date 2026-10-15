import { cleanName, url as urlRule } from '@/lib/validation'

export const BOOKMARK_TEXT_LIMIT = 5000
const LIMITS = { company: 200, title: 300, location: 200, url: 2000 }

export interface BookmarkParams {
  company: string
  title: string
  location: string
  url: string
  text: string
}

// Runs inside the job page, so it has to be self-contained plain JavaScript.
const COLLECTOR_SOURCE = `
(function () {
  var LIMIT = ${BOOKMARK_TEXT_LIMIT};
  var d = document;
  function clean(v) { return String(v == null ? '' : v).replace(/\\s+/g, ' ').trim(); }
  function meta(name) {
    var el = d.querySelector('meta[property="' + name + '"],meta[name="' + name + '"]');
    return el ? clean(el.getAttribute('content')) : '';
  }
  function findPosting(node) {
    if (!node) return null;
    if (Array.isArray(node)) {
      for (var i = 0; i < node.length; i++) { var f = findPosting(node[i]); if (f) return f; }
      return null;
    }
    if (typeof node !== 'object') return null;
    var type = node['@type'];
    if (type === 'JobPosting' || (Array.isArray(type) && type.indexOf('JobPosting') >= 0)) return node;
    return findPosting(node['@graph']);
  }
  function place(loc) {
    if (Array.isArray(loc)) loc = loc[0];
    if (!loc) return '';
    if (typeof loc === 'string') return clean(loc);
    var a = loc.address || loc;
    if (typeof a === 'string') return clean(a);
    return clean([a.addressLocality, a.addressRegion, a.addressCountry].filter(Boolean).join(', '));
  }
  function salary(base) {
    if (!base || typeof base !== 'object') return '';
    var v = base.value || {};
    var parts = [base.currency, v.minValue, v.maxValue || v.value].filter(Boolean);
    return parts.length ? 'Salary: ' + parts.join(' ') : '';
  }
  function strip(html) {
    var box = d.createElement('div');
    box.innerHTML = String(html || '');
    return clean(box.textContent);
  }
  var posting = null;
  var scripts = d.querySelectorAll('script[type="application/ld+json"]');
  for (var i = 0; i < scripts.length && !posting; i++) {
    try { posting = findPosting(JSON.parse(scripts[i].textContent)); } catch (e) {}
  }
  var title = '', company = '', location = '', text = '';
  if (posting) {
    title = clean(posting.title);
    var org = posting.hiringOrganization;
    company = clean(org && typeof org === 'object' ? org.name : org);
    location = place(posting.jobLocation);
    text = [title, company, location, posting.employmentType, salary(posting.baseSalary), strip(posting.description)]
      .filter(Boolean).join('\\n');
  }
  if (!title) title = meta('og:title') || clean(d.title);
  if (!company) company = meta('og:site_name');
  if (!text) {
    var selected = clean(String(window.getSelection ? window.getSelection() : ''));
    var main = d.querySelector('main') || d.body;
    text = title + '\\n' + (selected || clean(main && main.innerText));
  }
  var q = 'src=bookmarklet'
    + '&company=' + encodeURIComponent(company.slice(0, 200))
    + '&title=' + encodeURIComponent(title.slice(0, 300))
    + '&location=' + encodeURIComponent(location.slice(0, 200))
    + '&url=' + encodeURIComponent(String(window.location.href).slice(0, 2000))
    + '&text=' + encodeURIComponent(text.slice(0, LIMIT));
  window.open(ORIGIN + '/applications/new?' + q, '_blank');
})();
`

export function buildBookmarklet(origin: string): string {
  const source = COLLECTOR_SOURCE.replace('ORIGIN', JSON.stringify(origin)).replace(/\s*\n\s*/g, ' ').trim()
  return `javascript:${encodeURI(source).replace(/#/g, '%23')}`
}

export function buildSaveUrl(origin: string, params: Partial<BookmarkParams>): string {
  const query = new URLSearchParams({ src: 'bookmarklet' })
  for (const key of ['company', 'title', 'location', 'url', 'text'] as const) {
    const value = params[key]
    if (value) query.set(key, value)
  }
  return `${origin}/applications/new?${query.toString()}`
}

function sanitise(value: string | null, max: number): string {
  return (value ?? '')
    .replace(/<[^>]*>/g, ' ')
    .replace(/[<>]/g, '')
    .replace(/[\p{Cc}\p{Cs}\p{Co}]/gu, (char) => (char === '\n' ? '\n' : ' '))
    .replace(/[ \t]+/g, ' ')
    .trim()
    .slice(0, max)
}

function oneLine(label: string, value: string | null, max: number): string {
  const text = sanitise(value, max).replace(/\s+/g, ' ')
  return cleanName(label)(text) === null ? text : ''
}

export function readBookmarkParams(search: string): BookmarkParams | null {
  const query = new URLSearchParams(search)
  if (query.get('src') !== 'bookmarklet') return null
  const link = sanitise(query.get('url'), LIMITS.url)
  return {
    company: oneLine('Company', query.get('company'), LIMITS.company),
    title: oneLine('Job title', query.get('title'), LIMITS.title),
    location: oneLine('Location', query.get('location'), LIMITS.location),
    url: urlRule(link) === null ? link : '',
    text: sanitise(query.get('text'), BOOKMARK_TEXT_LIMIT),
  }
}
