import { beforeEach, expect, it, vi } from 'vitest'

vi.mock('../src/main/tv', () => ({
  packageSets: vi.fn(), homeComponent: vi.fn(), sh: vi.fn(), tv: vi.fn()
}))
import * as T from '../src/main/tv'
import { HOME_PKG, removeHome } from '../src/main/launcher'

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(T.packageSets).mockResolvedValue({ installed: new Set([HOME_PKG]) } as Awaited<ReturnType<typeof T.packageSets>>)
  vi.mocked(T.sh).mockResolvedValue('Success')
  vi.mocked(T.tv).mockReturnValue({ shell: vi.fn().mockResolvedValue('null') } as unknown as ReturnType<typeof T.tv>)
})

it.each([null, HOME_PKG + '/.MainActivity', 'android/.ResolverActivity', 'com.google.android.tungsten.setupwraith/.RecoveryActivity'])('keeps the launcher when no working replacement is verified: %s', async (component) => {
  vi.mocked(T.homeComponent).mockResolvedValue(component)
  expect((await removeHome()).ok).toBe(false)
  expect(T.sh).not.toHaveBeenCalled()
})

it('removes only CleanMyTV and preserves other accessibility and screensaver services', async () => {
  vi.mocked(T.homeComponent).mockResolvedValue('com.google.android.apps.tv.launcherx/.HomeActivity')
  vi.mocked(T.tv).mockReturnValue({ shell: vi.fn(async (cmd: string) => cmd.includes('enabled_accessibility') ? `${HOME_PKG}/.InputService:com.other/.Service` : `${HOME_PKG}/.Dream,com.other/.Dream`) } as unknown as ReturnType<typeof T.tv>)
  expect((await removeHome()).ok).toBe(true)
  expect(vi.mocked(T.sh).mock.calls.map((c) => c[1])).toEqual([
    'settings put secure enabled_accessibility_services com.other/.Service',
    'settings put secure screensaver_components com.other/.Dream',
    `pm uninstall ${HOME_PKG}`
  ])
})

it('reports an uninstall failure after returning to another home', async () => {
  vi.mocked(T.homeComponent).mockResolvedValue('com.google.android.apps.tv.launcherx/.HomeActivity')
  vi.mocked(T.sh).mockResolvedValue('Failure [DELETE_FAILED_INTERNAL_ERROR]')
  expect(await removeHome()).toMatchObject({ ok: false, warnings: ['Failure [DELETE_FAILED_INTERNAL_ERROR]'] })
})
