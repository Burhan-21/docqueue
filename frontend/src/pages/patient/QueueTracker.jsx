import { useState, useCallback, useRef, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { queueApi } from '../../api/api'
import { useWebSocket } from '../../hooks/useWebSocket'
import { Activity, Users, Clock, Wifi, WifiOff, Loader2, CheckCircle2 } from 'lucide-react'

export default function QueueTracker() {
  const { appointmentId } = useParams()
  const queryClient       = useQueryClient()
  const [wsConnected, setWsConnected] = useState(false)
  const [liveData,    setLiveData]    = useState(null)
  const wsConnectedRef = useRef(false)
  const doctorIdRef    = useRef(null)

  const { data: entry, isLoading } = useQuery({
    queryKey: ['queueEntry', appointmentId],
    queryFn:  () => queueApi.getPosition(appointmentId).then(r => r.data.data),
    refetchInterval: 30000, // fallback polling every 30s
  })

  // Stabilize doctorId — only set it once when first resolved to prevent
  // the topic string from changing on every re-render and causing reconnect loops
  useEffect(() => {
    if (entry?.doctorId && !doctorIdRef.current) {
      doctorIdRef.current = entry.doctorId
    }
  }, [entry])
  const doctorId = doctorIdRef.current

  // WebSocket subscription for live updates
  const handleWsMessage = useCallback((msg) => {
    setLiveData(msg)
    if (!wsConnectedRef.current) {
      wsConnectedRef.current = true
      setWsConnected(true) // Only trigger re-render once
    }
    queryClient.invalidateQueries({ queryKey: ['queueEntry', appointmentId] })
  }, [appointmentId, queryClient])

  useWebSocket(
    doctorId ? `/topic/queue/${doctorId}` : null,
    handleWsMessage
  )

  if (isLoading) return (
    <div className="flex items-center justify-center h-64">
      <Loader2 className="w-8 h-8 text-primary-400 animate-spin" />
    </div>
  )

  const position    = entry?.queuePosition ?? 0
  const status      = entry?.status ?? 'WAITING'
  const waitMin     = liveData?.estimatedWaitMinutes ?? entry?.estimatedWait ?? 0
  const waiting     = liveData?.patientsWaiting ?? 0
  const currentTok  = liveData?.currentToken ?? 0
  const isCompleted = status === 'COMPLETED'
  const isInProgress= status === 'IN_PROGRESS'

  return (
    <div className="max-w-lg mx-auto space-y-6">
      {/* Header */}
      <div className="animate-slide-up">
        <h1 className="text-3xl font-bold text-white">Live Queue Tracker</h1>
        <div className="flex items-center gap-2 mt-1">
          {wsConnected
            ? <><Wifi className="w-4 h-4 text-emerald-400" /><span className="text-emerald-400 text-sm">Live updates connected</span></>
            : <><WifiOff className="w-4 h-4 text-slate-500" /><span className="text-slate-500 text-sm">Polling every 30s</span></>
          }
        </div>
      </div>

      {/* Status states */}
      {isCompleted ? (
        <CompletedCard />
      ) : (
        <>
          {/* Token / Position display */}
          <div className="glass p-8 text-center animate-bounce-in relative overflow-hidden">
            {/* Background glow */}
            <div className="absolute inset-0 bg-gradient-to-b from-primary-600/10 to-transparent" />

            {isInProgress ? (
              <>
                <div className="live-ring w-4 h-4 bg-emerald-500 rounded-full mx-auto mb-4" />
                <p className="text-emerald-400 font-semibold text-lg animate-pulse">Your turn now!</p>
                <p className="text-slate-400 mt-1">Please proceed to the doctor's room</p>
              </>
            ) : (
              <>
                <p className="text-slate-400 text-sm uppercase tracking-widest mb-2">Your Position</p>
                <div className="text-8xl font-black text-gradient token-glow mb-2">
                  {position}
                </div>
                <p className="text-slate-400">
                  {position === 1 ? 'You are next!' : `${position - 1} patient${position > 2 ? 's' : ''} ahead`}
                </p>
              </>
            )}
          </div>

          {/* Stats row */}
          <div className="grid grid-cols-3 gap-3 animate-slide-up">
            <StatCard
              icon={<Clock className="w-5 h-5 text-amber-400" />}
              label="Est. Wait"
              value={waitMin > 0 ? `~${waitMin} min` : '—'}
              color="text-amber-300"
            />
            <StatCard
              icon={<Users className="w-5 h-5 text-blue-400" />}
              label="In Queue"
              value={waiting || '—'}
              color="text-blue-300"
            />
            <StatCard
              icon={<Activity className="w-5 h-5 text-primary-400" />}
              label="Current Token"
              value={currentTok || '—'}
              color="text-primary-300"
            />
          </div>

          {/* Progress visualization */}
          {position > 0 && (
            <div className="glass p-5 animate-slide-up">
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm text-slate-400">Queue progress</span>
                <span className="text-sm text-slate-300 font-medium">Token #{entry?.appointment?.tokenNumber}</span>
              </div>
              <div className="h-2 bg-white/5 rounded-full overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-primary-600 to-violet-600 rounded-full transition-all duration-1000"
                  style={{ width: `${Math.max(5, 100 - (position - 1) * 10)}%` }}
                />
              </div>
              <p className="text-xs text-slate-500 mt-2 text-center">
                This page updates automatically via WebSocket
              </p>
            </div>
          )}
        </>
      )}
    </div>
  )
}

function StatCard({ icon, label, value, color }) {
  return (
    <div className="glass p-4 text-center">
      <div className="flex justify-center mb-2">{icon}</div>
      <p className={`text-xl font-bold ${color}`}>{value}</p>
      <p className="text-xs text-slate-500 mt-0.5">{label}</p>
    </div>
  )
}

function CompletedCard() {
  return (
    <div className="glass p-10 text-center animate-bounce-in">
      <CheckCircle2 className="w-16 h-16 text-emerald-400 mx-auto mb-4" />
      <h2 className="text-2xl font-bold text-white mb-2">Consultation Complete</h2>
      <p className="text-slate-400">Your appointment has been completed. Take care! 🌟</p>
    </div>
  )
}
