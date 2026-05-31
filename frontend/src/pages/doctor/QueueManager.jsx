import { useState, useCallback, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../context/AuthContext'
import { appointmentApi, queueApi } from '../../api/api'
import { useWebSocket } from '../../hooks/useWebSocket'
import toast from 'react-hot-toast'
import { format } from 'date-fns'
import {
  ChevronRight, SkipForward, Loader2, Users,
  Activity, Clock, Wifi, WifiOff, CheckCircle2
} from 'lucide-react'

export default function QueueManager() {
  const { user }    = useAuth()
  const queryClient = useQueryClient()
  const [wsLive, setWsLive] = useState(false)
  const wsLiveRef = useRef(false)

  // We need doctor ID — for demo, use user.id; in real app, resolve from /doctors?userId
  const doctorId = user?.id

  const { data: appts } = useQuery({
    queryKey: ['doctorToday'],
    queryFn:  () => appointmentApi.doctorToday().then(r => r.data.data),
    refetchInterval: 30000,
  })

  const { data: queueEntries } = useQuery({
    queryKey: ['doctorQueue', doctorId],
    queryFn:  () => queueApi.getQueue(doctorId).then(r => r.data.data),
    refetchInterval: 15000,
    enabled: !!doctorId,
  })

  // WebSocket for real-time updates
  const handleWs = useCallback(() => {
    if (!wsLiveRef.current) {
      wsLiveRef.current = true
      setWsLive(true) // Only trigger re-render once
    }
    queryClient.invalidateQueries({ queryKey: ['doctorQueue', doctorId] })
    queryClient.invalidateQueries({ queryKey: ['doctorToday'] })
  }, [doctorId, queryClient])

  useWebSocket(doctorId ? `/topic/queue/${doctorId}` : null, handleWs)

  const callNextMutation = useMutation({
    mutationFn: () => queueApi.callNext(doctorId),
    onSuccess: () => {
      toast.success('Next patient called!')
      queryClient.invalidateQueries({ queryKey: ['doctorQueue', doctorId] })
      queryClient.invalidateQueries({ queryKey: ['doctorToday'] })
    },
    onError: (err) => toast.error(err.response?.data?.error ?? 'Error calling next'),
  })

  const skipMutation = useMutation({
    mutationFn: (apptId) => queueApi.skip(apptId, doctorId),
    onSuccess: () => {
      toast.success('Patient skipped')
      queryClient.invalidateQueries({ queryKey: ['doctorQueue', doctorId] })
    },
    onError: (err) => toast.error(err.response?.data?.error ?? 'Skip failed'),
  })

  // Use queue entries (QueueEntryDto) for real-time status
  const entries      = queueEntries ?? []
  const inProgress   = entries.find(e => e.status === 'IN_PROGRESS')
  const waiting      = entries.filter(e => e.status === 'WAITING')
  const completed    = (appts ?? []).filter(a => a.status === 'COMPLETED').length

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="animate-slide-up flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Queue Manager</h1>
          <p className="text-slate-400 mt-1">
            {format(new Date(), 'EEEE, dd MMMM yyyy')}
          </p>
        </div>
        <div className="flex items-center gap-2 text-sm">
          {wsLive
            ? <><Wifi className="w-4 h-4 text-emerald-400" /><span className="text-emerald-400">Live</span></>
            : <><WifiOff className="w-4 h-4 text-slate-500" /><span className="text-slate-500">Polling</span></>
          }
        </div>
      </div>

      {/* Stats bar */}
      <div className="grid grid-cols-3 gap-3 animate-slide-up">
        {[
          { label: 'Waiting',   value: waiting.length, icon: <Users className="w-4 h-4 text-amber-400" />,    color: 'text-amber-300' },
          { label: 'Completed', value: completed,       icon: <CheckCircle2 className="w-4 h-4 text-emerald-400" />, color: 'text-emerald-300' },
          { label: 'Total',     value: (appts ?? []).length, icon: <Activity className="w-4 h-4 text-blue-400" />, color: 'text-blue-300' },
        ].map(s => (
          <div key={s.label} className="glass p-4 flex items-center gap-3">
            {s.icon}
            <div>
              <p className={`text-2xl font-bold ${s.color}`}>{s.value}</p>
              <p className="text-xs text-slate-500">{s.label}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Currently serving */}
      {inProgress && (
        <div className="glass border border-primary-500/30 p-6 animate-fade-in">
          <p className="text-xs text-primary-400 uppercase tracking-widest font-semibold mb-3">
            Currently Consulting
          </p>
          <div className="flex items-center gap-4">
            <div className="relative">
              <div className="w-3 h-3 bg-emerald-500 rounded-full" />
              <div className="absolute inset-0 w-3 h-3 bg-emerald-500 rounded-full animate-ping opacity-50" />
            </div>
            <div>
              <p className="text-xl font-bold text-white">
                Token #{inProgress.tokenNumber} — {inProgress.patientName}
              </p>
              <p className="text-slate-400 text-sm">
                {inProgress.scheduledAt ? format(new Date(inProgress.scheduledAt), 'h:mm a') : ''}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Call next button */}
      <button
        onClick={() => callNextMutation.mutate()}
        disabled={callNextMutation.isPending || waiting.length === 0}
        className="btn-primary w-full flex items-center justify-center gap-3 text-lg py-5 animate-slide-up"
      >
        {callNextMutation.isPending
          ? <Loader2 className="w-5 h-5 animate-spin" />
          : <ChevronRight className="w-6 h-6" />
        }
        {waiting.length === 0 ? 'No patients waiting' : `Call Next Patient (${waiting.length} waiting)`}
      </button>

      {/* Queue list */}
      <div>
        <h2 className="text-lg font-semibold text-white mb-3 flex items-center gap-2">
          <Clock className="w-5 h-5 text-primary-400" /> Waiting Queue
        </h2>
        {waiting.length === 0 ? (
          <div className="glass p-10 text-center">
            <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto mb-3" />
            <p className="text-slate-300 font-medium">Queue is empty — all done! 🎉</p>
          </div>
        ) : (
          <div className="space-y-2">
            {waiting.map((entry, idx) => (
              <div key={entry.id}
                   className={`glass p-4 flex items-center gap-4 ${idx === 0 ? 'border border-primary-500/20' : ''}`}>
                <div className={`w-10 h-10 rounded-xl flex items-center justify-center font-bold text-sm
                  ${idx === 0 ? 'bg-primary-600 text-white' : 'bg-white/5 text-slate-400'}`}>
                  #{entry.tokenNumber}
                </div>
                <div className="flex-1">
                  <p className="font-medium text-slate-200">{entry.patientName ?? 'Patient'}</p>
                  <p className="text-xs text-slate-500">
                    {entry.scheduledAt ? format(new Date(entry.scheduledAt), 'h:mm a') : `Position ${entry.queuePosition}`}
                  </p>
                </div>
                {idx === 0 && (
                  <span className="badge badge-green text-[10px]">NEXT</span>
                )}
                <button
                  onClick={() => skipMutation.mutate(entry.appointmentId)}
                  disabled={skipMutation.isPending}
                  className="flex items-center gap-1 text-xs text-slate-500 hover:text-amber-400 transition-colors"
                  title="Skip patient"
                >
                  <SkipForward className="w-4 h-4" /> Skip
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
