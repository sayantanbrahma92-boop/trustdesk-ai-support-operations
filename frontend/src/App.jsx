import { useEffect, useRef, useState } from 'react'
import './App.css'

function Field({ label, children }) {
  return (
      <div className="field">
        <dt>{label}</dt>
        <dd>{children ?? 'Not available'}</dd>
      </div>
  )
}

async function readApiResponse(response) {
  const text = await response.text()
  let body = null

  if (text) {
    try {
      body = JSON.parse(text)
    } catch {
      body = text
    }
  }

  if (!response.ok) {
    const message =
        typeof body === 'string'
            ? body
            : body?.message ?? body?.error ?? `Request failed (${response.status})`
    throw new Error(message)
  }

  return body
}

function Metric({ label, value, detail }) {
  return (
      <div className="metric">
        <span className="metric-label">{label}</span>
        <strong>{value == null ? 'No data' : `${value}%`}</strong>
        {detail && <span className="metric-detail">{detail}</span>}
      </div>
  )
}

function App() {
  const [tickets, setTickets] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [triage, setTriage] = useState(null)
  const [draftResult, setDraftResult] = useState(null)
  const [actionError, setActionError] = useState('')
  const [busyAction, setBusyAction] = useState('')
  const [demoToken, setDemoToken] = useState('')
  const [reviewer, setReviewer] = useState('')
  const [approval, setApproval] = useState(null)
  const idempotencyKey = useRef(null)
  const [evaluation, setEvaluation] = useState(null)
  const [evaluationLoading, setEvaluationLoading] = useState(true)
  const [evaluationError, setEvaluationError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function loadTickets() {
      try {
        const response = await fetch('/api/tickets', {
          signal: controller.signal,
        })
        if (!response.ok) {
          throw new Error(`Could not load tickets (${response.status})`)
        }

        const data = await response.json()
        setTickets(data)
        setSelectedId(data[0]?.ticketId ?? '')
      } catch (requestError) {
        if (requestError.name !== 'AbortError') {
          setError(requestError.message)
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      }
    }

    loadTickets()
    return () => controller.abort()
  }, [])

  useEffect(() => {
    const controller = new AbortController()

    async function loadEvaluation() {
      try {
        const response = await fetch('/api/evaluation/latest', {
          signal: controller.signal,
        })
        const report = await readApiResponse(response)
        setEvaluation(report)
      } catch (requestError) {
        if (requestError.name !== 'AbortError') {
          setEvaluationError(requestError.message)
        }
      } finally {
        if (!controller.signal.aborted) {
          setEvaluationLoading(false)
        }
      }
    }

    loadEvaluation()
    return () => controller.abort()
  }, [])

  async function runTicketAction(action) {
    if (!selectedId) return

    setBusyAction(action)
    setActionError('')
    if (action === 'triage') setTriage(null)
    if (action === 'draft') setDraftResult(null)

    try {
      const response = await fetch(`/api/tickets/${selectedId}/${action}`, {
        method: 'POST',
      })
      const result = await readApiResponse(response)

      if (action === 'triage') setTriage(result)
      if (action === 'draft') setDraftResult(result)
    } catch (requestError) {
      setActionError(requestError.message)
    } finally {
      setBusyAction('')
    }
  }

  async function requestRefundReview() {
    if (!selectedId || !demoToken.trim()) {
      setActionError('Enter the demo token before requesting a refund review.')
      return
    }

    setBusyAction('refund-review')
    setActionError('')

    const key = idempotencyKey.current ?? crypto.randomUUID()
    idempotencyKey.current = key

    try {
      const response = await fetch(
          `/api/tickets/${selectedId}/refund-review`,
          {
            method: 'POST',
            headers: {
              'X-Demo-Token': demoToken.trim(),
              'Idempotency-Key': key,
            },
          },
      )
      const result = await readApiResponse(response)
      setApproval(result)
    } catch (requestError) {
      setActionError(requestError.message)
    } finally {
      setBusyAction('')
    }
  }

  async function reviewApproval(decision) {
    if (!approval || !demoToken.trim() || !reviewer.trim()) {
      setActionError(
          'Enter the demo token and reviewer name to review this request.',
      )
      return
    }

    setBusyAction(decision)
    setActionError('')

    try {
      const response = await fetch(
          `/api/approvals/${approval.id}/${decision}`,
          {
            method: 'POST',
            headers: {
              'X-Demo-Token': demoToken.trim(),
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({ reviewer: reviewer.trim() }),
          },
      )
      const result = await readApiResponse(response)
      setApproval(result)

      if (decision === 'reject') {
        idempotencyKey.current = null
      }
    } catch (requestError) {
      setActionError(requestError.message)
    } finally {
      setBusyAction('')
    }
  }

  const ticket = tickets.find((item) => item.ticketId === selectedId)

  return (
      <main className="app">
        <header className="page-header">
          <div>
            <p className="eyebrow">SUPPORT OPERATIONS</p>
            <h1>TrustDesk</h1>
            <p className="subtitle">Review a ticket and its customer context.</p>
          </div>
          <span className="service-status">Agent workspace</span>
        </header>

        <section className="card evaluation-card" aria-labelledby="evaluation-title">
          <p className="eyebrow">QUALITY CHECK</p>
          <h2 id="evaluation-title">Latest evaluation</h2>

          {evaluationLoading && (
              <p className="message" role="status">Loading evaluation results...</p>
          )}

          {evaluationError && (
              <p className="message error" role="alert">
                Evaluation report unavailable: {evaluationError}
              </p>
          )}

          {evaluation && (
              <>
                <p className="message">
                  {evaluation.total_cases} evaluation cases
                </p>
                <div className="evaluation-metrics">
                  <Metric label="Triage accuracy" value={evaluation.triage_accuracy_percent} />
                  <Metric label="Citation coverage" value={evaluation.citation_coverage_percent} />
                  <Metric
                      label="Unsafe-action blocking"
                      value={evaluation.unsafe_action_blocking_percent}
                      detail={`${evaluation.unsafe_case_count} cases`}
                  />
                  <Metric label="Escalation accuracy" value={evaluation.escalation_accuracy_percent} />
                  <Metric
                      label="Draft escalation accuracy"
                      value={evaluation.draft_escalation_accuracy_percent}
                  />
                </div>
              </>
          )}
        </section>

        <section className="inbox">
          <label htmlFor="ticket-select">Ticket inbox</label>
          <select
              id="ticket-select"
              value={selectedId}
              disabled={loading || tickets.length === 0 || Boolean(busyAction)}
              onChange={(event) => {
                setSelectedId(event.target.value)
                setTriage(null)
                setDraftResult(null)
                setActionError('')
                setDemoToken('')
                setReviewer('')
                idempotencyKey.current = null
              }}
          >
            {loading && <option value="">Loading tickets...</option>}
            {!loading && tickets.length === 0 && (
                <option value="">No tickets found</option>
            )}
            {tickets.map((item) => (
                <option key={item.ticketId} value={item.ticketId}>
                  {item.ticketId} — {item.subject}
                </option>
            ))}
          </select>

          {error && <p className="message error" role="alert">{error}</p>}
          {loading && <p className="message">Loading ticket data...</p>}
          {!loading && !error && tickets.length === 0 && (
              <p className="message">There are no tickets to display.</p>
          )}
        </section>

        {ticket && (
            <div className="details-grid">
              <section className="card ticket-card">
                <div className="card-heading">
                  <div>
                    <p className="eyebrow">TICKET</p>
                    <h2>{ticket.subject}</h2>
                  </div>
                  <span className="status">{ticket.status}</span>
                </div>
                <dl>
                  <Field label="Ticket ID">{ticket.ticketId}</Field>
                  <Field label="Created">{ticket.createdAt}</Field>
                  <Field label="Channel">{ticket.channel}</Field>
                  <Field label="Customer message">
                    <span className="message-body">{ticket.body}</span>
                  </Field>
                </dl>
              </section>

              <section className="card">
                <p className="eyebrow">CUSTOMER</p>
                <h2>{ticket.customer?.name ?? 'Customer'}</h2>
                <dl>
                  <Field label="Email">{ticket.customer?.email}</Field>
                  <Field label="Tier">{ticket.customer?.tier}</Field>
                  <Field label="Verified">
                    {ticket.customer?.verified == null
                        ? null
                        : ticket.customer.verified
                            ? 'Yes'
                            : 'No'}
                  </Field>
                </dl>
              </section>

              <section className="card">
                <p className="eyebrow">ORDER</p>
                <h2>{ticket.order?.orderId ?? 'No linked order'}</h2>
                {ticket.order && (
                    <dl>
                      <Field label="Status">{ticket.order.status}</Field>
                      <Field label="Placed">{ticket.order.placedAt}</Field>
                      <Field label="Return eligible until">
                        {ticket.order.eligibleReturnUntil}
                      </Field>
                      <Field label="Total">
                        {ticket.order.total} {ticket.order.currency}
                      </Field>
                      <Field label="Items">
                        {ticket.order.items
                            .map((item) => `${item.name} × ${item.quantity}`)
                            .join(', ')}
                      </Field>
                    </dl>
                )}
              </section>

              <section className="card action-card">
                <p className="eyebrow">AI ASSISTANCE</p>
                <h2>Ticket analysis</h2>

                <div className="action-buttons">
                  <button
                      type="button"
                      disabled={Boolean(busyAction)}
                      onClick={() => runTicketAction('triage')}
                  >
                    {busyAction === 'triage' ? 'Triaging...' : 'Run triage'}
                  </button>
                  <button
                      type="button"
                      disabled={Boolean(busyAction)}
                      onClick={() => runTicketAction('draft')}
                  >
                    {busyAction === 'draft' ? 'Drafting...' : 'Generate draft'}
                  </button>
                </div>

                {actionError && (
                    <p className="message error" role="alert">{actionError}</p>
                )}

                {triage && (
                    <div className="result-block" aria-live="polite">
                      <h3>Triage result</h3>
                      <p>Category: {triage.category}</p>
                      <p>Priority: {triage.priority}</p>
                      <p>
                        Escalation: {triage.escalation ? 'Required' : 'Not required'}
                      </p>
                    </div>
                )}

                {draftResult && (
                    <div className="result-block" aria-live="polite">
                      <h3>
                        {draftResult.status === 'draft' ? 'Suggested reply' : 'Escalation'}
                      </h3>
                      {draftResult.draft && (
                          <p className="message-body">{draftResult.draft}</p>
                      )}
                      {draftResult.reason && <p>{draftResult.reason}</p>}
                      {draftResult.citation_ids?.length > 0 && (
                          <p>Citations: {draftResult.citation_ids.join(', ')}</p>
                      )}
                    </div>
                )}
              </section>

              <section className="card action-card">
                <p className="eyebrow">HUMAN APPROVAL</p>
                <h2>Refund review</h2>
                <p className="message">
                  Requesting a review does not execute it. A human reviewer must
                  approve before Java records the action as executed.
                </p>

                <div className="approval-form">
                  <label htmlFor="demo-token">Demo token</label>
                  <input
                      id="demo-token"
                      type="password"
                      autoComplete="new-password"
                      value={demoToken}
                      onChange={(event) => setDemoToken(event.target.value)}
                  />

                  <label htmlFor="reviewer-name">Reviewer name</label>
                  <input
                      id="reviewer-name"
                      value={reviewer}
                      onChange={(event) => setReviewer(event.target.value)}
                  />
                </div>

                <div className="action-buttons">
                  <button
                      type="button"
                      disabled={
                          Boolean(busyAction) ||
                          !demoToken.trim() ||
                          approval?.status === 'PENDING' ||
                          approval?.status === 'APPROVED'
                      }
                      onClick={requestRefundReview}
                  >
                    {busyAction === 'refund-review'
                        ? 'Requesting...'
                        : 'Request refund review'}
                  </button>

                  {approval?.status === 'PENDING' && (
                      <>
                        <button
                            type="button"
                            disabled={Boolean(busyAction) || !reviewer.trim()}
                            onClick={() => reviewApproval('approve')}
                        >
                          {busyAction === 'approve' ? 'Approving...' : 'Approve'}
                        </button>
                        <button
                            type="button"
                            className="secondary-button"
                            disabled={Boolean(busyAction) || !reviewer.trim()}
                            onClick={() => reviewApproval('reject')}
                        >
                          {busyAction === 'reject' ? 'Rejecting...' : 'Reject'}
                        </button>
                      </>
                  )}
                </div>

                {approval && (
                    <div className="result-block" aria-live="polite">
                      <h3>Approval status: {approval.status}</h3>
                      <p>Action: {approval.actionName}</p>
                      <p>Executed: {approval.actionExecuted ? 'Yes' : 'No'}</p>
                      {approval.reviewedBy && (
                          <p>Reviewed by: {approval.reviewedBy}</p>
                      )}
                    </div>
                )}
              </section>
            </div>
        )}
      </main>
  )
}

export default App