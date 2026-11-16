import { t as tr } from '../lib/i18n'
import { useEffect, useState, type ReactNode } from 'react'
import type { ApplyResult, SystemState } from '@shared/api'
import type { ScreenProps } from '../App'
import { Busy, Confirm, ResultNotice, errorText } from '../components/ui'

const ADGUARD = 'dns.adguard-dns.com'

const SPEEDS: [number, string][] = [
  [0, 'Off'],
  [0.5, 'Fast (0.5×)'],
  [1, 'Normal (1×)']
]

export function Extras({ markDone }: ScreenProps): ReactNode {
  const [sys, setSys] = useState<SystemState | null>(null)
  const [result, setResult] = useState<ApplyResult | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [dnsHost, setDnsHost] = useState('')
  const [askReboot, setAskReboot] = useState(false)

  const refresh = (): void => {
    window.cytv.systemState().then((s) => {
      setSys(s)
      if (s.privateDnsMode === 'hostname' && s.privateDnsHost && s.privateDnsHost !== ADGUARD) setDnsHost(s.privateDnsHost)
    })
  }
  useEffect(refresh, [])

  async function run(label: string, fn: () => Promise<ApplyResult>): Promise<void> {
    setBusy(label)
    try {
      const r = await fn()
      setResult(r)
      if (r.ok) markDone('extras')
    } catch (e) {
      setResult({ ok: false, summary: errorText(e), warnings: [] })
    } finally {
      setBusy(null)
      refresh()
    }
  }

  if (!sys) return <Busy text={tr("Reading the TV's settings")} />

  const dnsOn = sys.privateDnsMode === 'hostname' && !!sys.privateDnsHost
  const scale = Number(sys.animationScale === 'null' ? 1 : sys.animationScale)

  return (
    <>
      <h1>{tr("TV extras")}</h1>
      <p className="lede">{tr("Settings outside the launcher that the TV's own menus hide or don't offer.")}</p>
      <ResultNotice result={result} onDismiss={() => setResult(null)} />
      {busy && (
        <div className="notice">
          <Busy text={tr(busy)} />
        </div>
      )}

      {sys.storage && <Storage storage={sys.storage} busy={!!busy} onClear={() => run('Clearing app caches', () => window.cytv.clearCaches())} />}

      <div className="card">
          <h2>{tr("Animation speed")}</h2>
          <p className="muted small">{tr("How fast menus and apps open, across the whole TV. Off feels snappiest on slow TVs.")}</p>
          <div className="seg">
            {SPEEDS.map(([v, label]) => (
              <button key={v} className={scale === v ? 'on' : ''} disabled={!!busy} onClick={() => run('Setting animation speed', () => window.cytv.setAnimationScale(v))}>
                {tr(label)}
              </button>
            ))}
          </div>
      </div>

      <div className="card">
        <h2>{tr("Google screensaver")}</h2>
        <p className="muted small">{tr("Shows Google Photos, art and the weather while the TV rests. If it stopped after a clean-up, this turns its two apps back on and sets it as the screensaver again. Clean up leaves them alone after that.")}</p>
        <button className="btn" disabled={!!busy} onClick={() => run('Bringing back the screensaver', () => window.cytv.fixScreensaver())}>{tr("Bring it back")}</button>
      </div>

      <div className="card">
        <div className="card-head">
          <h2>{tr("Block ads inside apps")}</h2>
          <span className={`chip ${dnsOn ? 'ok' : 'optional'}`}>{tr(dnsOn ? tr("On · {0}", [sys.privateDnsHost]) : 'Off')}</span>
        </div>
        <p className="muted">{tr("Points the TV's Private DNS at an ad-blocking resolver, so ad and tracker domains stop resolving in every app. It survives reboots. The trade-off: that resolver sees every DNS query the TV makes. It does not remove YouTube ads.")}</p>
        {sys.sdk < 28 ? (
          <div className="notice warn">{tr("Private DNS needs Android 9 or newer. This TV runs API ")}{tr(sys.sdk)}.</div>
        ) : (
          <>
            <div className="row">
              <button className="btn" disabled={!!busy} onClick={() => run('Turning on AdGuard DNS', () => window.cytv.setPrivateDns(ADGUARD))}>{tr("Use AdGuard DNS")}</button>
              <input type="text" value={dnsHost} onChange={(e) => setDnsHost(e.target.value)} placeholder={tr("or another resolver, e.g. dns.nextdns.io")} style={{ width: 280 }} />
              <button
                className="btn"
                disabled={!!busy || !/^[a-z0-9.-]+\.[a-z]{2,}$/i.test(dnsHost.trim())}
                onClick={() => run('Setting Private DNS', () => window.cytv.setPrivateDns(dnsHost.trim()))}
              >{tr("Use this one")}</button>
              {dnsOn && (
                <button className="btn ghost" disabled={!!busy} onClick={() => run('Turning Private DNS off', () => window.cytv.setPrivateDns(null))}>{tr("Turn off")}</button>
              )}
            </div>
            <p className="small muted" style={{ marginTop: 10 }}>{tr("If an app stops playing or the TV says “No internet”, the resolver is down. Turn this off.")}</p>
          </>
        )}
      </div>

      <div>
        <div className="card">
          <h2>{tr("Remote")}</h2>
          <p className="muted small">{tr("Press buttons on the TV from here, handy when the remote is in the other room.")}</p>
          <div className="row" style={{ gap: 24, alignItems: 'flex-start' }}>
            <div className="remote" role="group" aria-label={tr("Remote control")}>
              <span />
              <Key code={19} label={tr("▲")} name="Up" />
              <span />
              <Key code={21} label={tr("◀")} name="Left" />
              <Key code={23} label={tr("OK")} name="OK" round />
              <Key code={22} label={tr("▶")} name="Right" />
              <span />
              <Key code={20} label={tr("▼")} name="Down" />
              <span />
            </div>
            <div className="stack" style={{ gap: 6 }}>
              <Key code={3} label={tr("Home")} wide />
              <Key code={4} label={tr("Back")} wide />
              <div className="row" style={{ gap: 6 }}>
                <Key code={25} label={tr("Vol −")} wide />
                <Key code={24} label={tr("Vol +")} wide />
              </div>
            </div>
          </div>
          <div style={{ marginTop: 14 }}>
            <button className="btn small ghost" onClick={() => setAskReboot(true)}>{tr("Restart the TV")}</button>
          </div>
        </div>
      </div>

      {askReboot && (
        <Confirm
          title={tr("Restart the TV?")}
          confirmLabel={tr("Restart")}
          onCancel={() => setAskReboot(false)}
          onConfirm={() => {
            setAskReboot(false)
            window.cytv.reboot().then(
              () => setResult({ ok: true, summary: 'The TV is restarting. Connect again from Setup once it is back.', warnings: [] }),
              (e) => setResult({ ok: false, summary: errorText(e), warnings: [] })
            )
          }}
        >
          <p>{tr("The connection drops while it restarts. On some TVs, port 5555 stays closed after a restart until you pair again.")}</p>
        </Confirm>
      )}
    </>
  )
}

// Below this, installs and app updates start failing on most TVs (#48).
const LOW_MB = 500

function Storage({ storage, busy, onClear }: { storage: NonNullable<SystemState['storage']>; busy: boolean; onClear: () => void }): ReactNode {
  const free = Math.round(storage.freeKb / 1024)
  const total = Math.round(storage.totalKb / 1024)
  const used = Math.min(100, Math.max(0, 100 - (storage.freeKb / storage.totalKb) * 100))
  const gb = (mb: number): string => (mb >= 1024 ? tr("{0} GB", [(mb / 1024).toFixed(1)]) : tr("{0} MB", [mb]))
  const low = free < LOW_MB
  return (
    <div className="card">
      <div className="card-head">
        <h2>{tr("Storage")}</h2>
        <span className={`chip ${low ? 'warn' : 'ok'}`}>{tr(gb(free))}{tr(" free")}</span>
      </div>
      <div className={`progress${low ? ' low' : ''}`} role="meter" aria-valuenow={Math.round(used)} aria-valuemin={0} aria-valuemax={100} aria-label={tr("Storage used")}>
        <div style={{ width: `${used}%` }} />
      </div>
      <p className="muted small" style={{ marginTop: 8 }}>
        {tr(gb(total - free))}{tr(" of ")}{tr(gb(total))}{tr(" used.")}{tr(' ')}
        {tr(low ? 'Low enough that installs and app updates can fail.' : 'Apps keep caches here that they rebuild when needed.')}
      </p>
      <div className="row" style={{ marginTop: 12 }}>
        <button className="btn" disabled={busy} onClick={onClear}>{tr("Clear app caches")}</button>
        <span className="muted small">{tr("Logins and settings stay. Apps may open a little slower the first time.")}</span>
      </div>
    </div>
  )
}

function Key({ code, label, name, round, wide }: { code: number; label: string; name?: string; round?: boolean; wide?: boolean }): ReactNode {
  return (
    <button
      className={`btn${round ? ' ok-key' : ''}${wide ? ' small' : ''}`}
      aria-label={tr(name ?? label)}
      style={wide ? { minWidth: 72, justifyContent: 'center' } : undefined}
      onClick={() => window.cytv.key(code)}
    >
      {tr(label)}
    </button>
  )
}
