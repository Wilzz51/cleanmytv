import { describe, expect, it } from 'vitest'
import { clean } from '../src/main/home'

describe('new Home settings from the desktop app', () => {
  it('accepts custom header text and independent visibility without dropping other labels', () => {
    expect(clean({ names: { brand: 'Mon salon', wifi: 'Réseau' }, show_brand: false }))
      .toEqual({ names: { brand: 'Mon salon', wifi: 'Réseau' }, show_brand: false })
    expect(clean({ names: { brand: '', wifi: 'Réseau' }, show_brand: true }))
      .toEqual({ names: { wifi: 'Réseau' }, show_brand: true })
    expect(clean({ names: { brand: 'x'.repeat(40) } })).toEqual({ names: { brand: 'x'.repeat(24) } })
    expect(() => clean({ show_brand: 'false' })).toThrow(/show_brand/)
    expect(() => clean({ names: { brand: 123 } })).toThrow(/names/)
  })
  it('accepts Cinema and existing layouts, and rejects unsupported layouts', () => {
    for (const layout of ['shelf', 'split', 'rail', 'cinema', 'horizon', 'epure']) expect(clean({ layout })).toEqual({ layout })
    expect(() => clean({ layout: 'unknown' })).toThrow(/layout/)
  })
  it('takes daily limits of 5 to 720 minutes for real package names only', () => {
    expect(clean({ limits: { 'com.google.android.youtube.tv': 60 } })).toEqual({ limits: { 'com.google.android.youtube.tv': 60 } })
    expect(() => clean({ limits: { 'com.netflix.ninja': 3 } })).toThrow(/limits/)
    expect(() => clean({ limits: { 'rm -rf /': 60 } })).toThrow(/limits/)
  })

  it('takes the font, idle style, Bing every day and the editor lock', () => {
    expect(clean({ font: 'serif', idle_style: 'photos', bing_daily: true, lock_editor: true })).toEqual({ font: 'serif', idle_style: 'photos', bing_daily: true, lock_editor: true })
    expect(() => clean({ font: 'comic' })).toThrow(/font/)
    expect(clean({ quick: ['timer', 'settings'] })).toEqual({ quick: ['timer', 'settings'] })
  })
})
