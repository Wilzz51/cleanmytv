import { useSyncExternalStore } from 'react'
import french from '../locales/fr.json'
import messages from '../locales/fr-messages.json'

export type Locale = 'en' | 'fr'
export const LANGUAGE_KEY = 'cleanmytv.language'
const normalize = (text: string): string => text.replace(/\s+/g, ' ').trim()
const translations: Readonly<Record<string, string>> = Object.fromEntries(
  Object.entries({ ...french, ...messages }).map(([key, value]) => [normalize(key), value])
)
const messagePatterns = Object.entries(messages).filter(([key]) => /\{\d+\}/.test(key)).map(([key, value]) => {
  const indexes: number[] = []
  const parts = key.split(/(\{\d+\})/g).map((part) => {
    if (/^\{\d+\}$/.test(part)) { indexes.push(Number(part.slice(1, -1))); return '(.*?)' }
    return part.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  })
  return { pattern: new RegExp(`^${parts.join('')}$`), indexes, value }
})

/** Saved choice wins; otherwise follow the computer's language on first launch. */
export function preferredLocale(saved: string | null, languages: readonly string[]): Locale {
  if (saved === 'en' || saved === 'fr') return saved
  return languages[0]?.toLowerCase().split('-')[0] === 'fr' ? 'fr' : 'en'
}

function initialLocale(): Locale {
  let saved: string | null = null
  try { saved = localStorage.getItem(LANGUAGE_KEY) } catch { /* Storage may be unavailable. */ }
  return preferredLocale(saved, typeof navigator === 'undefined' ? [] : navigator.languages)
}

let locale = initialLocale()
const listeners = new Set<() => void>()
const subscribe = (listener: () => void): (() => void) => {
  listeners.add(listener)
  return () => { listeners.delete(listener) }
}
export const getLocale = (): Locale => locale
export const useLocale = (): Locale => useSyncExternalStore(subscribe, getLocale, getLocale)

function updateDocument(): void {
  if (typeof document !== 'undefined') document.documentElement.lang = locale
}
updateDocument()

export function setLocale(next: Locale): void {
  if (next !== 'en' && next !== 'fr') return
  locale = next
  try { localStorage.setItem(LANGUAGE_KEY, next) } catch { /* Keep the in-memory choice. */ }
  updateDocument()
  listeners.forEach((listener) => listener())
}

/** Translate presentation text only. Unknown text, identifiers and React elements pass through. */
export function t<T>(text: T, values?: readonly unknown[]): T {
  if (typeof text !== 'string') return text
  const format = (template: string, params?: readonly unknown[]): string => params
    ? template.replace(/\{(\d+)\}/g, (token, index: string) => Number(index) < params.length ? String(params[Number(index)]) : token)
    : template
  if (locale === 'en') return format(text, values) as T
  const key = normalize(text)
  let translated = Object.hasOwn(translations, key) ? translations[key] : undefined
  let params = values
  // The main process stays language-neutral; localise its known status messages at display time.
  if (translated === undefined && !values && text.length <= 4096) {
    for (const entry of messagePatterns) {
      const match = entry.pattern.exec(text)
      if (!match) continue
      translated = entry.value
      const captured: string[] = []
      entry.indexes.forEach((index, i) => { captured[index] = match[i + 1] })
      params = captured
      break
    }
  }
  if (translated === undefined) return format(text, values) as T
  return ((/^\s/.test(text) ? ' ' : '') + format(translated, params) + (/\s$/.test(text) ? ' ' : '')) as T
}
