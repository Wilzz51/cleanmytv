import { t as tr } from '../lib/i18n'
import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import type { ApplyResult, CheckItem, DownloadProgress, FoundTv, ScanHit, ToolId } from '@shared/api'
import type { ScreenProps } from '../App'
import { TvPicker, TvThumb } from '../components/TvPicker'
import { Busy, ResultNotice, StatusChip, errorText } from '../components/ui'

const IP_RE = /^\d{1,3}(\.\d{1,3}){3}(:\d{2,5})?$/
const DISCOVER_EVERY_MS = 2000
// The first answers take a moment; asking again soon means the list doesn't wait a full round.
const DISCOVER_FIRST_MS = 500

function foundStatus(tv: FoundTv): { text: string; dot: string } {
  if (tv.ready) return { text: 'ready to connect', dot: 'ok' }
  if (tv.pairPort) return { text: 'ready to pair', dot: 'ok' }
  if (tv.connectPort) return { text: 'open the pairing screen', dot: 'busy' }
  return { text: 'debugging is off', dot: 'idle' }
}

export function Setup({ status, go, tvs, refreshTvs }: ScreenProps): ReactNode {
  const [checks, setChecks] = useState<CheckItem[] | null>(null)
  const [progress, setProgress] = useState<Partial<Record<ToolId, DownloadProgress>>>({})
  const [downloading, setDownloading] = useState<Set<ToolId>>(new Set())
  const [downloadResult, setDownloadResult] = useState<ApplyResult | null>(null)

  const [hits, setHits] = useState<ScanHit[] | null>(null)
  const [scanning, setScanning] = useState(false)
  const [found, setFound] = useState<FoundTv[]>([])
  const [host, setHost] = useState('')
  // Once the user has typed or picked an address, a TV showing up doesn't change it.
  const [hostChosen, setHostChosen] = useState(false)
  const [connectError, setConnectError] = useState<string | null>(null)
  const [wantPair, setWantPair] = useState(false)
  const [pairing, setPairing] = useState(false)

  const refreshChecks = useCallback(() => {
    window.cytv.checks().then(setChecks, () => setChecks([]))
  }, [])

  useEffect(() => {
    refreshChecks()
    window.cytv.lastHost().then((h) => h && setHost((cur) => cur || h))
    return window.cytv.onProgress((p) => setProgress((prev) => ({ ...prev, [p.tool]: p })))
  }, [refreshChecks])

  // The checklist depends on the connection.
  useEffect(() => {
    if (status.state === 'connected' || status.state === 'error' || status.state === 'idle') refreshChecks()
  }, [status.state, refreshChecks])

  async function download(tool: ToolId): Promise<void> {
    setDownloading((s) => new Set(s).add(tool))
    try {
      setDownloadResult(await window.cytv.download(tool))
    } catch (e) {
      setDownloadResult({ ok: false, summary: errorText(e), warnings: [] })
    } finally {
      setDownloading((s) => {
        const n = new Set(s)
        n.delete(tool)
        return n
      })
      refreshChecks()
    }
  }

  async function scan(): Promise<void> {
    setScanning(true)
    try {
      const found = await window.cytv.scan()
      setHits(found)
      if (found.length === 1 && !host) setHost(found[0].host)
    } catch (e) {
      setConnectError(errorText(e))
    } finally {
      setScanning(false)
    }
  }

  // The saved TV wasn't at its address: look for it, and let the user pick (the app never logs in to
  // scanned devices by itself).
  useEffect(() => {
    if (status.moved && hits === null && !scanning) scan()
  }, [status.moved])

  async function connect(h = host): Promise<void> {
    setConnectError(null)
    setHostChosen(true)
    try {
      const s = await window.cytv.connect(h.trim())
      if (s.state === 'error' && !s.needsPairing) setConnectError(s.error ?? 'Could not connect.')
    } catch (e) {
      setConnectError(errorText(e))
    }
  }

  const busy = status.state === 'connecting' || status.state === 'waiting-auth'
  const connected = status.state === 'connected'
  const required = checks?.filter((c) => c.id !== 'platform-tools') ?? []
  const optional = checks?.filter((c) => c.id === 'platform-tools') ?? []

  // Listen for TVs while nothing is connected. This only fills in the form: the app never logs in to a
  // TV it found by itself.
  // Not while pairing either: some TVs serve one connection at a time, and adb needs it.
  const looking = !connected && !busy && !pairing
  useEffect(() => {
    if (!looking) return
    let stopped = false
    let first = true
    let timer: ReturnType<typeof setTimeout>
    const poll = async (): Promise<void> => {
      try {
        const tvs = await window.cytv.discover()
        if (!stopped) setFound(tvs)
      } catch {
        // The scan and the address field still work.
      }
      if (!stopped) timer = setTimeout(poll, first ? DISCOVER_FIRST_MS : DISCOVER_EVERY_MS)
      first = false
    }
    poll()
    return () => {
      stopped = true
      clearTimeout(timer)
      window.cytv.stopDiscovery()
    }
  }, [looking])

  useEffect(() => {
    if (hostChosen) {
      // The TV the user picked before opening its pairing screen.
      const chosen = found.find((t) => t.host === host)
      if (chosen?.pairPort && !chosen.ready) setWantPair(true)
      return
    }
    const open = found.filter((t) => t.pairPort && !t.ready)
    const ready = found.filter((t) => t.ready)
    // Two pairing screens at once: leave the choice to the user, and don't close the form they're in.
    const target = open.length === 1 ? open[0].host : host
    if (open.length === 1) setHost(target)
    else if (ready.length === 1) setHost((cur) => cur || ready[0].host)
    setWantPair(open.some((t) => t.host === target))
  }, [found, host, hostChosen])

  function pick(tv: FoundTv): void {
    setHost(tv.host)
    setHostChosen(true)
    setWantPair(!tv.ready && Boolean(tv.pairPort || tv.connectPort))
  }

  const pairHost = wantPair ? host : status.host ?? host
  // A pairing error belongs to the address it came from, not to one picked since.
  const showPair = wantPair || (status.needsPairing && (!hostChosen || host === status.host))

  return (
    <>
      <h1>{tr("Set up")}</h1>
      <p className="lede">{tr("This app talks to the TV over your Wi-Fi. You don't need Python, Java or Android tools to clean the TV and install the home screen. The one optional download is only for pairing some Google TVs.")}</p>

      {status.state === 'waiting-auth' && <WaitingForAllow />}

      {!connected && status.state !== 'waiting-auth' && (
        <TvPicker
          tvs={tvs}
          status={status}
          onChange={refreshTvs}
          title={tr(tvs.length > 1 ? 'Which TV?' : 'Your TV')}
          hint={tr(tvs.length > 1 ? "You've used this app with more than one TV, so it waits for you to pick." : undefined)}
        />
      )}

      {connected && status.probe ? (
        <div className="card white">
          <div className="tv-card">
            {tvs.find((t) => t.host === status.host)?.thumb ? <TvThumb tv={tvs.find((t) => t.host === status.host)} /> : <div className="tv-glyph" aria-hidden />}
            <div>
              <div className="row between">
                <h2 style={{ margin: 0 }}>
                  {status.probe.manufacturer} {status.probe.model}
                </h2>
                <div className="row">
                  <StatusChip status="ok" text={tr("Connected")} />
                  <button className="btn small ghost" onClick={() => window.cytv.disconnect()}>{tr("Disconnect")}</button>
                </div>
              </div>
              <div className="facts">
                <div>
                  <span>{tr("Address")}</span>
                  <b>{status.host}</b>
                </div>
                <div>
                  <span>{tr("Android")}</span>
                  <b>
                    {status.probe.release}{tr(" (API ")}{tr(status.probe.sdk)})
                  </b>
                </div>
                <div>
                  <span>{tr("Device")}</span>
                  <b>{status.probe.device}</b>
                </div>
                <div>
                  <span>{tr("Build")}</span>
                  <b className="mono">{status.probe.build}</b>
                </div>
              </div>
            </div>
          </div>
          {status.probe.adbSecure === '0' && (
            <div className="notice warn" style={{ marginTop: 16, marginBottom: 0 }}>{tr("This TV reports ")}<code>{tr("ro.adb.secure=0")}</code>{tr(". Any device on your network can reach its debug port without the Allow prompt. Close it when you're done: the Finish step walks you through it.")}</div>
          )}
          {status.probe.sdk > 0 && status.probe.sdk < 28 && (
            <div className="notice warn" style={{ marginTop: 16, marginBottom: 0 }}>{tr("This TV reports Android ")}{status.probe.release}{tr(" (API ")}{tr(status.probe.sdk)}{tr("). CleanMyTV Home needs Android 9 (API 28) or newer, so the Home screen step will refuse to install here. Cleanup stays available and reversible (nothing is uninstalled).")}</div>
          )}
          <div className="row" style={{ marginTop: 16 }}>
            <button className="btn primary" onClick={() => go('debloat')}>{tr("Next: clean up the TV")}</button>
          </div>
        </div>
      ) : null}

      {connected && status.probe ? (
        <TvPicker tvs={tvs} status={status} onChange={refreshTvs} title={tr("Your other TVs")} hint={tr("Connecting to one disconnects this TV.")} />
      ) : (
        <div className="grid-2">
          <div className="card">
            <h2>{tr("1. On the TV")}</h2>
            <ol className="guide">
              <li>
                <span>{tr("Open ")}<span className="path">{tr("Settings")}<i>→</i>{tr("Device Preferences")}<i>→</i>{tr("About")}</span>{tr(". On some Google TVs it is")}{tr(' ')}
                  <span className="path">{tr("Settings")}<i>→</i>{tr("System")}<i>→</i>{tr("About")}</span>.
                </span>
              </li>
              <li>
                <span>{tr("Scroll to ")}<b>{tr("Build")}</b>{tr(" (or ")}<b>{tr("Android TV OS build")}</b>{tr(") and press OK on it seven times. The TV says you are now a developer.")}</span>
              </li>
              <li>
                <span>{tr("Go back one screen and open ")}<b>{tr("Developer options")}</b>.
                </span>
              </li>
              <li>
                <span>{tr("Turn on ")}<b>{tr("USB debugging")}</b>{tr(". If you also see ")}<b>{tr("Network debugging")}</b>{tr(" or ")}<b>{tr("ADB debugging")}</b>{tr(", turn that on too.")}</span>
              </li>
              <li>
                <span>{tr("Keep the TV on and on the same Wi-Fi as this computer.")}</span>
              </li>
            </ol>
          </div>

          <div className="card">
            <h2>{tr("2. Find your TV")}</h2>
            <p className="muted small">{tr("TVs on your Wi-Fi show up here by themselves. Pick yours to fill in its address.")}</p>
            {status.moved && status.error && (
              <div className="notice warn" style={{ marginBottom: 12 }}>
                {tr(status.error)}{tr(" Only connect to an address you recognise as your TV.")}</div>
            )}
            {found.length > 0 ? (
              <ul className="host-list">
                {found.map((tv) => {
                  const s = foundStatus(tv)
                  return (
                    <li key={tv.host}>
                      <button
                        className={`host-item${host === tv.host ? ' selected' : ''}`}
                        onClick={() => pick(tv)}
                        onDoubleClick={() => tv.ready && connect(tv.host)}
                      >
                        <span className={`dot ${s.dot}`} />
                        <span style={{ flex: 1 }}>
                          {tr(tv.name || 'Android device')} <span className="muted small">{tv.host}</span>
                        </span>
                        <span className="muted small">{tr(s.text)}</span>
                      </button>
                    </li>
                  )
                })}
              </ul>
            ) : (
              looking && <Busy text={tr("Looking for TVs")} />
            )}
            <p className="muted small" style={{ marginTop: 12 }}>{tr("Not listed? Older TVs don't announce themselves: the scan checks your network for port 5555. It takes a few seconds.")}</p>
            <div className="row" style={{ marginBottom: 12 }}>
              <button className="btn" onClick={scan} disabled={scanning || busy}>
                {tr(scanning ? 'Scanning…' : hits ? 'Scan again' : 'Scan the network')}
              </button>
              {scanning && <span className="spinner" />}
            </div>
            {hits && hits.length === 0 && (
              <div className="notice warn">{tr("No TV answered. Check that USB debugging is on and the TV is on this Wi-Fi, or type its address below (TV Settings → Network → your network).")}</div>
            )}
            {hits && hits.length > 0 && (
              <ul className="host-list">
                {hits.map((h) => (
                  <li key={h.host}>
                    <button
                      className={`host-item${host === h.host ? ' selected' : ''}`}
                      onClick={() => {
                        setHost(h.host)
                        setHostChosen(true)
                        setWantPair(false)
                      }}
                      onDoubleClick={() => connect(h.host)}
                    >
                      <span className="dot ok" />
                      <span style={{ flex: 1 }}>{h.host}</span>
                      <span className="muted small">{tr("port 5555 open")}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <form
              className="stack"
              onSubmit={(e) => {
                e.preventDefault()
                connect()
              }}
            >
              <label className="field">{tr("TV address")}<input type="text" value={host} onChange={(e) => { setHost(e.target.value); setHostChosen(true) }} placeholder={tr("192.168.1.50")} />
              </label>
              <div className="row">
                <button className="btn primary" type="submit" disabled={busy || !IP_RE.test(host.trim())}>
                  {tr(status.state === 'connecting' ? 'Connecting…' : 'Connect')}
                </button>
                {status.state === 'connecting' && <Busy text={tr("Opening the connection")} />}
              </div>
            </form>
            {connectError && (
              <div className="notice error" style={{ marginTop: 14, marginBottom: 0, whiteSpace: 'pre-line' }}>
                {tr(connectError)}
                {connectError.includes('/Applications/CleanMyTV.app/') && (
                  <div style={{ marginTop: 10 }}>
                    <button className="btn small" onClick={() => void navigator.clipboard.writeText('/Applications/CleanMyTV.app/Contents/MacOS/CleanMyTV')}>{tr("Copy the Terminal command")}</button>
                  </div>
                )}
                {/network debugging is off|only works for pairing/.test(connectError) && !wantPair && (
                  <div style={{ marginTop: 10 }}>
                    <button className="btn small" onClick={() => { setHostChosen(true); setWantPair(true) }}>{tr("Pair with a code")}</button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {showPair && !connected && (
        // Keyed by the TV, not the address: typing in the address field mustn't wipe the code.
        <PairForm
          key={found.find((t) => t.host === pairHost)?.host ?? 'typed'}
          host={pairHost}
          tv={found.find((t) => t.host === pairHost)}
          onBusy={setPairing}
          onPaired={() => connect(pairHost)}
        />
      )}

      <div className="card white" style={{ marginTop: 8 }}>
        <div className="card-head">
          <h2>{tr("What's ready")}</h2>
          <button className="btn small ghost" onClick={refreshChecks}>{tr("Check again")}</button>
        </div>
        <ResultNotice result={downloadResult} onDismiss={() => setDownloadResult(null)} />
        {!checks ? (
          <Busy text={tr("Checking this computer")} />
        ) : (
          <>
            <Checklist items={required} progress={progress} downloading={downloading} onDownload={download} />
            {optional.length > 0 && (
              <>
                <h3 style={{ marginTop: 22 }}>{tr("Optional")}</h3>
                <p className="muted small">{tr("Only needed to pair a TV that asks for a code. Each one downloads into this app's own folder.")}</p>
                <Checklist items={optional} progress={progress} downloading={downloading} onDownload={download} />
              </>
            )}
          </>
        )}
      </div>
    </>
  )
}

function Checklist(props: {
  items: CheckItem[]
  progress: Partial<Record<ToolId, DownloadProgress>>
  downloading: Set<ToolId>
  onDownload: (t: ToolId) => void
}): ReactNode {
  return (
    <ul className="checklist">
      {props.items.map((c) => {
        const p = c.download ? props.progress[c.download] : undefined
        const active = c.download ? props.downloading.has(c.download) : false
        return (
          <li key={c.id} className="check">
            <div className="check-title">
              <StatusChip status={c.status} />
              <span>{tr(c.label)}</span>
            </div>
            <div className="check-detail">
              {tr(c.detail)}
              {active && p && <DownloadBar p={p} />}
              {active && !p && <div className="progress indeterminate"><div /></div>}
            </div>
            <div className="check-actions">
              {c.download && (
                <button className="btn small" onClick={() => props.onDownload(c.download!)} disabled={active}>
                  {tr(active ? 'Downloading…' : 'Download')}
                </button>
              )}
            </div>
          </li>
        )
      })}
    </ul>
  )
}

function DownloadBar({ p }: { p: DownloadProgress }): ReactNode {
  const pct = p.total > 0 ? Math.min(100, (p.received / p.total) * 100) : 0
  const label =
    p.phase === 'download'
      ? tr("{0} of {1}", [mb(p.received), p.total ? mb(p.total) : '?'])
      : p.phase === 'extract'
        ? 'Unpacking'
        : p.phase === 'install'
          ? p.message ?? 'Installing'
          : p.phase === 'error'
            ? p.message ?? 'Failed'
            : 'Done'
  return (
    <div>
      <div className={`progress${p.phase === 'download' && p.total ? '' : ' indeterminate'}`}>
        <div style={p.phase === 'download' && p.total ? { width: `${pct}%` } : undefined} />
      </div>
      <span className="small muted">{tr(label)}</span>
    </div>
  )
}

const mb = (n: number): string => tr("{0} MB", [(n / 1e6).toFixed(n > 1e8 ? 0 : 1)])

function WaitingForAllow(): ReactNode {
  return (
    <div className="waiting" role="status">
      <div className="allow-mock" aria-hidden>
        <b>{tr("Allow USB debugging?")}</b>
        <div className="small">{tr("RSA key fingerprint…")}</div>
        <div className="small">{tr("☑ Always allow from this computer")}</div>
        <div className="am-btn">{tr("Cancel")}</div>
        <div className="am-btn focus">{tr("Allow")}</div>
      </div>
      <div>
        <div className="big">{tr("Look at the TV and choose Allow.")}</div>
        <p className="muted" style={{ marginTop: 6 }}>{tr("Tick ")}<b>{tr("Always allow from this computer")}</b>{tr(" first, so it doesn't ask again. If nothing shows, press Home on the remote: the prompt can hide behind a running app.")}</p>
        <Busy text={tr("Waiting for the TV")} />
      </div>
    </div>
  )
}

function PairForm({ host, tv, onBusy, onPaired }: { host: string; tv?: FoundTv; onBusy: (busy: boolean) => void; onPaired: () => Promise<void> }): ReactNode {
  // Null until typed in: the ports follow what the TV advertises, unless the user has overridden them.
  const [pairTyped, setPairTyped] = useState<string | null>(null)
  const [connectTyped, setConnectTyped] = useState<string | null>(null)
  // The last connect port seen: one slow check shouldn't empty the field while the user types the code.
  const lastConnect = useRef<number | undefined>(undefined)
  if (tv?.connectPort) lastConnect.current = tv.connectPort
  const pairPort = pairTyped ?? (tv?.pairPort ? String(tv.pairPort) : '')
  const connectPort = connectTyped ?? (lastConnect.current ? String(lastConnect.current) : '')
  const [code, setCode] = useState('')
  const [busy, setBusy] = useState(false)
  const codeInput = useRef<HTMLInputElement>(null)
  const card = useRef<HTMLDivElement>(null)

  // It opens by itself, below the fold on a small window.
  useEffect(() => {
    card.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [])

  // The pairing screen just opened: the code is all that's left. Not while the user is typing elsewhere.
  useEffect(() => {
    if (tv?.pairPort && document.activeElement?.tagName !== 'INPUT') codeInput.current?.focus()
  }, [tv?.pairPort])
  const [result, setResult] = useState<ApplyResult | null>(null)

  async function pair(): Promise<void> {
    setBusy(true)
    onBusy(true)
    setResult(null)
    try {
      const r = await window.cytv.pair(host, Number(pairPort), code.trim(), Number(connectPort))
      setResult(r)
      if (r.ok) await onPaired()
    } catch (e) {
      setResult({ ok: false, summary: errorText(e), warnings: [] })
    } finally {
      setBusy(false)
      onBusy(false)
    }
  }

  const valid = /^\d{4,5}$/.test(pairPort) && /^\d{6}$/.test(code.trim()) && /^\d{4,5}$/.test(connectPort)
  return (
    <div className="card white" ref={card}>
      <h2>{tr("Pair with a code")}{tr(tv?.name ? `: ${tv.name}` : '')}</h2>
      <p className="muted">{tr("Many Google TVs and Chromecasts only offer Wireless debugging, and keep port 5555 closed until you pair once. This uses Google's adb, which the app downloads if it isn't here yet.")}</p>
      <ol className="guide" style={{ marginBottom: 14 }}>
        <li>
          <span>{tr("On the TV: ")}<span className="path">{tr("Developer options")}<i>→</i>{tr("Wireless debugging")}</span>{tr(", turn it on, and note the")}{tr(' ')}
            <b>{tr("IP address & port")}</b>{tr(" on that screen. That is the connect port.")}</span>
        </li>
        <li>
          <span>{tr("Choose ")}<b>{tr("Pair device with pairing code")}</b>{tr(". Note the 6-digit code and the port shown under it. That is the pair port.")}</span>
        </li>
      </ol>
      {tv?.pairPort && connectPort ? (
        <div className="notice ok">{tr("Found the pairing screen on ")}{tr(host)}{tr(". Type the 6-digit code it shows.")}</div>
      ) : (
        tv && !tv.pairPort && <p className="muted small">{tr("Waiting for the pairing screen on ")}{tr(host)}{tr(". The ports fill in when you open it.")}</p>
      )}
      <ResultNotice result={result} />
      <div className="row" style={{ alignItems: 'flex-end' }}>
        <label className="field" style={{ width: 120 }}>{tr("Pair port")}<input type="text" inputMode="numeric" value={pairPort} onChange={(e) => setPairTyped(e.target.value)} />
        </label>
        <label className="field" style={{ width: 130 }}>{tr("Code")}<input ref={codeInput} type="text" inputMode="numeric" value={code} onChange={(e) => setCode(e.target.value)} />
        </label>
        <label className="field" style={{ width: 130 }}>{tr("Connect port")}<input type="text" inputMode="numeric" value={connectPort} onChange={(e) => setConnectTyped(e.target.value)} />
        </label>
        <button className="btn primary" onClick={pair} disabled={!valid || busy}>
          {tr(busy ? 'Pairing…' : 'Pair and connect')}
        </button>
      </div>
      {/* The example numbers that used to sit in these fields read like real ones, and got sent as such. */}
      <p className="small muted" style={{ marginTop: 8 }}>{tr("Pair port and code: from the pairing window. Connect port: the port on the main Wireless debugging screen, after the IP address.")}</p>
    </div>
  )
}
