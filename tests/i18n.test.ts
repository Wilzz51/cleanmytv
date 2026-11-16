import { afterEach, describe, expect, it, vi } from 'vitest'
import { getLocale, LANGUAGE_KEY, preferredLocale, setLocale, t } from '../src/renderer/src/lib/i18n'
import french from '../src/renderer/src/locales/fr.json'
import messages from '../src/renderer/src/locales/fr-messages.json'

afterEach(() => { setLocale('en'); vi.unstubAllGlobals() })

describe('desktop language', () => {
  it('uses an explicit choice before the computer language and safely falls back to English', () => {
    expect(preferredLocale('en', ['fr-FR'])).toBe('en')
    expect(preferredLocale('fr', ['en-US'])).toBe('fr')
    expect(preferredLocale(null, ['fr-CA'])).toBe('fr')
    expect(preferredLocale('invalid', ['de-DE', 'fr-FR'])).toBe('en')
    expect(preferredLocale(null, [])).toBe('en')
  })

  it('saves a changed language and updates the document without restarting', () => {
    const setItem = vi.fn()
    const document = { documentElement: { lang: 'en' } }
    vi.stubGlobal('localStorage', { setItem })
    vi.stubGlobal('document', document)
    setLocale('fr')
    expect(getLocale()).toBe('fr')
    expect(setItem).toHaveBeenCalledWith(LANGUAGE_KEY, 'fr')
    expect(document.documentElement.lang).toBe('fr')
    expect(t('Clean up')).toBe('Nettoyage')
    setLocale('en')
    expect(t('Clean up')).toBe('Clean up')
  })

  it('keeps working when storage is unavailable', () => {
    vi.stubGlobal('localStorage', { setItem: () => { throw new Error('blocked') } })
    expect(() => setLocale('fr')).not.toThrow()
    expect(t('Restore')).toBe('Restaurer')
  })

  it('preserves technical data and unknown messages, and does not recursively translate parameters', () => {
    setLocale('fr')
    for (const value of ['com.vendor.ads', '192.168.1.50', 'pm disable-user --user 0 com.vendor.ads', 'Custom TV name', 'constructor', '__proto__']) {
      expect(t(value)).toBe(value)
    }
    expect(t('Show {0}', ['Home'])).toBe('Afficher Home')
    expect(t('Connecting to {0}', ['192.168.1.50'])).toBe('Connexion à 192.168.1.50')
    expect(t('  Unknown diagnostic\n')).toBe('  Unknown diagnostic\n')
    expect(t(null)).toBeNull()
  })

  it('translates known main-process results while keeping their values', () => {
    setLocale('fr')
    expect(t('Disabled 3 of 5 packages.')).toBe('3 paquets désactivés sur 5.')
    expect(t('This computer is on 192.168.1.x.')).toBe('Cet ordinateur est sur 192.168.1.x.')
    expect(t('Every app\'s DNS now goes to dns.example.org.')).toBe('Le DNS de toutes les applications utilise désormais dns.example.org.')
    setLocale('en')
    expect(t('Disabled 3 of 5 packages.')).toBe('Disabled 3 of 5 packages.')
    expect(t('Connecting to {0}', ['TV'])).toBe('Connecting to TV')
  })

  it('keeps every interpolation value in the French catalogs', () => {
    const tokens = (text: string): string[] => (text.match(/\{\d+\}/g) ?? []).sort()
    for (const [english, translated] of Object.entries({ ...french, ...messages })) {
      expect(translated.trim(), english).not.toBe('')
      expect(tokens(translated), english).toEqual(tokens(english))
    }
  })
})
