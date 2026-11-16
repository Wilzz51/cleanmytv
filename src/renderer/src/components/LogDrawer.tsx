import { t as tr } from '../lib/i18n'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import type { LogLine } from '@shared/api'

const MAX_LINES = 500

const line = (l: LogLine): string => `${new Date(l.time).toISOString()} [${l.task}] ${l.level === 'cmd' ? '$ ' : ''}${l.text}`
/** IPv4 addresses, which say nothing a bug report needs. */
const noAddresses = (s: string): string => s.replace(/\b(?:\d{1,3}\.){3}\d{1,3}\b/g, 'x.x.x.x')

export function LogDrawer(): ReactNode {
  const [lines, setLines] = useState<LogLine[]>([])
  const [open, setOpen] = useState(false)
  const body = useRef<HTMLDivElement>(null)

  useEffect(
    () =>
      window.cytv.onLog((line) => {
        setLines((prev) => {
          const next = prev.length >= MAX_LINES ? prev.slice(prev.length - MAX_LINES + 1) : prev.slice()
          next.push(line)
          return next
        })
        if (line.level === 'error') setOpen(true)
      }),
    []
  )

  useEffect(() => {
    if (open && body.current) body.current.scrollTop = body.current.scrollHeight
  }, [lines, open])

  const last = lines[lines.length - 1]
  return (
    <div className="log">
      <button className="log-bar" onClick={() => setOpen(!open)} aria-expanded={open}>
        <span>{tr(open ? '▾' : '▸')}{tr(" Activity")}</span>
        <span className="last">{last ? (last.level === 'cmd' ? `$ ${last.text}` : last.text) : tr('Nothing has run yet.')}</span>
        {lines.length > 0 && <span className="muted small">{tr(lines.length)}{tr(" lines")}</span>}
      </button>
      {open && (
        <div className="log-body" ref={body}>
          {lines.length === 0 && <p className="muted">{tr("Commands sent to the TV show up here.")}</p>}
          {lines.map((l, i) => (
            <div key={i} className={`log-line ${l.level}`}>
              <span className="t">{tr(new Date(l.time).toLocaleTimeString([], { hour12: false }))}</span>
              <span className="task">{tr(l.task)}</span>
              <span className="x">{l.text}</span>
            </div>
          ))}
          <div className="row" style={{ marginTop: 8 }}>
            {lines.length > 0 && (
              <>
                <button className="btn small ghost" onClick={() => setLines([])}>{tr("Clear")}</button>
                <button className="btn small ghost" onClick={() => navigator.clipboard.writeText(lines.map(line).join('\n'))}>{tr("Copy all")}</button>
              </>
            )}
            <button
              className="btn small ghost"
              title={tr("Your TV model, Android version, app versions and the TV's app list, then this log, without IP addresses. Paste it into a GitHub issue.")}
              onClick={async () => {
                const details = await window.cytv.bugReport().catch((e) => tr("Could not read the details: {0}", [(e as Error).message]))
                navigator.clipboard.writeText(noAddresses(tr("{0}\n\nActivity:\n{1}", [details, lines.map(line).join('\n') || '(empty)'])))
              }}
            >{tr("Copy for a bug report")}</button>
          </div>
        </div>
      )}
    </div>
  )
}
