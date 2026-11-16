import { t as tr } from '../lib/i18n'
import { useState, type ReactNode } from 'react'
import type { ConnStatus, KnownTv } from '@shared/api'
import { errorText } from './ui'

/** The TV's last snapshot, or a plain screen shape until there is one. */
export function TvThumb({ tv, className }: { tv?: KnownTv; className?: string }): ReactNode {
  return tv?.thumb ? (
    <img className={`tv-thumb ${className ?? ''}`} src={tv.thumb} alt={tr("Screen of {0}", [tv.model ?? 'the TV'])} />
  ) : (
    <div className={`tv-thumb no-shot ${className ?? ''}`} aria-hidden />
  )
}

function ago(t: number): string {
  if (!t) return 'a while ago'
  const m = Math.round((Date.now() - t) / 60000)
  if (m < 2) return 'just now'
  if (m < 60) return tr("{0} min ago", [m])
  const h = Math.round(m / 60)
  if (h < 24) return tr("{0} h ago", [h])
  return tr("{0} d ago", [Math.round(h / 24)])
}

/** TVs used before. Picking one connects to it (checking the same model answers there). */
export function TvPicker(props: { tvs: KnownTv[]; status: ConnStatus; onChange: () => void; title: string; hint?: string }): ReactNode {
  const [busy, setBusy] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const current = props.status.state === 'connected' ? props.status.host : undefined
  const list = props.tvs.filter((t) => t.host !== current)
  if (!list.length) return null

  async function connect(host: string): Promise<void> {
    setBusy(host)
    setError(null)
    try {
      const s = await window.cytv.connectKnown(host)
      if (s.state === 'error') setError(s.error ?? 'Could not connect.')
    } catch (e) {
      setError(errorText(e))
    } finally {
      setBusy(null)
      props.onChange()
    }
  }

  async function forget(host: string): Promise<void> {
    await window.cytv.forgetTv(host)
    props.onChange()
  }

  return (
    <div className="card white">
      <h2>{tr(props.title)}</h2>
      {props.hint && <p className="muted small">{tr(props.hint)}</p>}
      {error && <div className="notice warn">{tr(error)}</div>}
      <div className="tv-list">
        {list.map((tv) => (
          <div key={tv.host} className="tv-item">
            <TvThumb tv={tv} />
            <div className="tv-item-text">
              <b>{tr([tv.manufacturer, tv.model].filter(Boolean).join(' ') || tv.host)}</b>
              <span className="muted small">
                {tv.host}{tr(" · used ")}{tr(ago(tv.lastUsed))}
              </span>
            </div>
            <div className="row">
              <button className="btn primary small" disabled={!!busy} onClick={() => connect(tv.host)}>
                {tr(busy === tv.host ? 'Connecting…' : 'Connect')}
              </button>
              <button className="btn ghost small" disabled={!!busy} onClick={() => forget(tv.host)} title={tr("Remove from this list")}>{tr("Forget")}</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
