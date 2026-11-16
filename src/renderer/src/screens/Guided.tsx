import { t as tr } from '../lib/i18n'
// The guided "Clean & launch" run (#9): one button that walks the happy path —
// pick the best-matching list, disable its suggested packages, install
// CleanMyTV Home, and land on the Home screen step — while keeping every step
// inspectable and skippable. It composes the same IPC calls the individual
// screens use; no new main-process behaviour.
import { useEffect, useRef, useState } from 'react'
import type { ApplyResult, DebloatPlan } from '@shared/api'
import type { ScreenProps } from '../App'
import { Busy, StatusChip, Stepper, errorText } from '../components/ui'
import { defaultOff } from '../lib/ticks'

type Phase =
  | { t: 'plan' }                       // picking the list, building the plan
  | { t: 'ready'; plan: DebloatPlan; disable: string[] }  // showing what will happen
  | { t: 'run'; step: string }          // executing
  | { t: 'done'; result: ApplyResult; customised: boolean }
  | { t: 'failed'; message: string }

const STAGES = ['Pick the list', 'Clean up', 'Install Home', 'Last pass']

export default function Guided({ go, markDone }: ScreenProps): React.ReactElement {
  const [phase, setPhase] = useState<Phase>({ t: 'plan' })
  // Which of STAGES is running; past the end means all done.
  const [at, setAt] = useState(0)
  const [skipped, setSkipped] = useState(false)
  const alive = useRef(true)
  useEffect(() => () => { alive.current = false }, [])

  useEffect(() => { if (phase.t === 'plan') void prepare() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  async function prepare(): Promise<void> {
    try {
      const lists = await window.cytv.lists()
      const best = lists.reduce((a, b) => (b.score > a.score ? b : a))
      if (best.score <= 0) {
        setPhase({ t: 'failed', message: 'No list matches this TV closely. Use Clean up to pick one by hand — the guided run needs a matching list to be safe.' })
        return
      }
      const plan = await window.cytv.plan(best.id)
      // What Clean up would switch off with its defaults: features people use stay on (#57).
      const suggested = defaultOff(plan.entries)
      setPhase({ t: 'ready', plan, disable: suggested })
      setAt(1)
    } catch (e) {
      setPhase({ t: 'failed', message: errorText(e) })
    }
  }

  async function run(skipDebloat: boolean): Promise<void> {
    const p = phase
    if (p.t !== 'ready') return
    setPhase({ t: 'run', step: 'Preparing' })
    setSkipped(skipDebloat)
    const notes: string[] = []
    try {
      if (!skipDebloat && p.disable.length) {
        setPhase({ t: 'run', step: tr("Disabling {0} packages from \"{1}\"", [p.disable.length, p.plan.title]) })
        // No safety overrides: the stock launcher and setupwraith stay on until CleanMyTV Home is Home,
        // and the Wi-Fi check runs (#33).
        const r = await window.cytv.applyDebloat({ listId: p.plan.listId, disable: p.disable, enable: [] })
        if (!r.ok) throw new Error(r.summary)
        notes.push(tr("Cleaned up: {0}", [r.summary.replace(/\.$/, '')]))
        if (alive.current) markDone('debloat')
      } else {
        notes.push(skipDebloat ? 'Clean up skipped' : 'Nothing left to clean up')
      }

      setAt(2)
      const before = await window.cytv.homeStatus()
      if (before.unsupported) {
        notes.push(before.unsupported)
        setAt(STAGES.length)
        setPhase({ t: 'done', result: { ok: true, summary: notes.join(' · '), warnings: [] }, customised: false })
        return
      }
      const outdated = before.installed && before.bundledVersion && before.versionName !== before.bundledVersion
      if (!before.isHome || outdated) {
        setPhase({ t: 'run', step: 'Installing CleanMyTV Home' })
        const r = await window.cytv.installHome()
        if (!r.ok) throw new Error(r.summary + (r.stockChoices ? ' Open the Home screen step and pick it there.' : ''))
        notes.push('CleanMyTV Home installed and set as Home')
        if (alive.current) markDone('home')
      } else {
        notes.push('CleanMyTV Home already Home — left as it was')
      }

      setAt(3)
      if (!skipDebloat) {
        // What the first pass kept on because CleanMyTV Home wasn't Home yet.
        const plan = await window.cytv.plan(p.plan.listId)
        const left = plan.entries.filter((e) => e.state === 'enabled' && p.disable.includes(e.pkg)).map((e) => e.pkg)
        if (left.length) {
          setPhase({ t: 'run', step: tr("Switching off {0}", [left.join(', ')]) })
          const r = await window.cytv.applyDebloat({ listId: p.plan.listId, disable: left, enable: [] })
          notes.push(r.warnings.length ? tr("Kept on: {0}", [r.warnings.join(' ')]) : tr("Then switched off {0} more", [left.length]))
        }
      }
      notes.push('Next: the restart check on Finish')

      setAt(STAGES.length)
      setPhase({ t: 'done', result: { ok: true, summary: notes.join(' · '), warnings: [] }, customised: true })
    } catch (e) {
      notes.push('Stopped: ' + errorText(e))
      setPhase({ t: 'failed', message: notes.join(' · ') })
    }
  }

  return (
    <>
      <h1>{tr("Clean & launch")}</h1>
      <p className="muted">{tr("The one-button path: the best-matching list for your TV, its suggested packages off, CleanMyTV Home installed and set as Home — then you land on the Home screen step to arrange it. Nothing is uninstalled and every step stays open in the sidebar if you want to go slower.")}</p>

      <Stepper steps={STAGES} at={at} failed={phase.t === 'failed'} skipped={skipped ? [1, 3] : []} />

      {phase.t === 'plan' && <Busy text={tr("Reading your TV and picking the matching list")} />}

      {phase.t === 'ready' && (
        <div className="stack" style={{ gap: 16 }}>
          <div className="card">
            <h2 style={{ margin: 0 }}>{tr("Ready to run")}</h2>
            <p style={{ margin: '8px 0' }}>{tr("List: ")}<b>{tr(phase.plan.title)}</b>{tr(" — the closest match for your TV.")}</p>
            <p style={{ margin: '8px 0' }}>
              {phase.disable.length > 0 ? (
                <>{tr("Will disable ")}<b>{tr(phase.disable.length)}{tr(" packages")}</b>{tr(" (the list's suggested set — the same defaults Clean up pre-ticks), then install CleanMyTV Home and hand Home to it.")}</>
              ) : (
                <>{tr("Nothing on this list is left to switch off with Clean up's defaults, so this installs CleanMyTV Home and hands Home to it.")}</>
              )}
            </p>
            <p className="muted small" style={{ margin: 0 }}>{tr("Restore puts every package back exactly as it is now; Home can go back to the stock launcher from the Home screen step at any time.")}</p>
          </div>
          <div className="row">
            <button className="btn primary" onClick={() => void run(false)}>{tr("Clean & launch")}</button>
            <button className="btn" onClick={() => void run(true)}>{tr("Home screen only (skip clean up)")}</button>
            <button className="btn ghost" onClick={() => go('debloat')}>{tr("Let me pick by hand instead")}</button>
          </div>
        </div>
      )}

      {phase.t === 'run' && <Busy text={tr(phase.step)} />}

      {phase.t === 'done' && (
        <div className="stack" style={{ gap: 16 }}>
          <div className="card">
            <StatusChip status="ok" text={tr("Done")} />
            <p style={{ margin: '8px 0 0' }}>{tr(phase.result.summary)}</p>
          </div>
          <div className="row">
            <button className="btn primary" onClick={() => go('home')}>{tr("Arrange my home screen")}</button>
            <button className="btn ghost" onClick={() => go('finish')}>{tr("Finish (restart check, then close the debug port)")}</button>
          </div>
        </div>
      )}

      {phase.t === 'failed' && (
        <div className="stack" style={{ gap: 16 }}>
          <div className="notice error">{tr(phase.message)}</div>
          <div className="row">
            <button className="btn" onClick={() => { setPhase({ t: 'plan' }); setAt(0); setSkipped(false); void prepare() }}>{tr("Try again")}</button>
            <button className="btn ghost" onClick={() => go('debloat')}>{tr("Use the step-by-step screens")}</button>
          </div>
        </div>
      )}
    </>
  )
}
