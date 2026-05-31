import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { appointmentApi } from '../../api/api'
import { format } from 'date-fns'
import { Activity, Clock, Users, ChevronRight, Loader2, Calendar } from 'lucide-react'

const STATUS_BADGE = {
  CONFIRMED: 'badge-green',
  PENDING:   'badge-amber',
  COMPLETED: 'badge-blue',
  IN_PROGRESS:'badge-purple',
}

export default function DoctorDashboard() {
  const { user } = useAuth()

  const { data: appts, isLoading } = useQuery({
    queryKey: ['doctorToday'],
    queryFn:  () => appointmentApi.doctorToday().then(r => r.data.data),
    refetchInterval: 60000,
  })

  const appointments = appts ?? []
  const completed    = appointments.filter(a => a.status === 'COMPLETED').length
  const waiting      = appointments.filter(a => ['PENDING','CONFIRMED'].includes(a.status)).length

  return (
    <div className="max-w-5xl mx-auto space-y-8">
      <div className="animate-slide-up">
        <h1 className="text-3xl font-bold text-white">
          Good {getGreeting()}, <span className="text-gradient">Dr. {user?.name?.split(' ').slice(-1)}</span>
        </h1>
        <p className="text-slate-400 mt-1">{format(new Date(), 'EEEE, dd MMMM yyyy')}</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 animate-slide-up">
        {[
          { label: "Today's Total",   value: appointments.length,  icon: <Calendar className="w-5 h-5 text-blue-400" />,    color: 'text-blue-300' },
          { label: 'Waiting',          value: waiting,              icon: <Users className="w-5 h-5 text-amber-400" />,      color: 'text-amber-300' },
          { label: 'Completed',        value: completed,            icon: <Activity className="w-5 h-5 text-emerald-400" />, color: 'text-emerald-300' },
        ].map(s => (
          <div key={s.label} className="glass p-5">
            <div className="flex items-center justify-between mb-2">
              <p className="text-slate-400 text-sm">{s.label}</p>
              {s.icon}
            </div>
            <p className={`text-4xl font-bold ${s.color}`}>{s.value}</p>
          </div>
        ))}
      </div>

      {/* Queue Manager CTA */}
      <Link to="/doctor/queue"
            className="glass-hover p-6 flex items-center gap-5 group animate-slide-up">
        <div className="w-14 h-14 rounded-2xl bg-primary-600/30 flex items-center justify-center">
          <Activity className="w-7 h-7 text-primary-400" />
        </div>
        <div className="flex-1">
          <h2 className="text-xl font-bold text-white">Open Queue Manager</h2>
          <p className="text-slate-400 text-sm mt-0.5">Call next patient, skip, and manage today's queue in real-time</p>
        </div>
        <ChevronRight className="w-6 h-6 text-slate-500 group-hover:text-primary-400 transition-colors" />
      </Link>

      {/* Today's list */}
      <div>
        <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
          <Clock className="w-5 h-5 text-primary-400" /> Today's Appointments
        </h2>
        {isLoading ? (
          <div className="flex items-center justify-center h-32">
            <Loader2 className="w-6 h-6 text-primary-400 animate-spin" />
          </div>
        ) : appointments.length === 0 ? (
          <div className="glass p-10 text-center">
            <Calendar className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <p className="text-slate-400">No appointments scheduled for today</p>
          </div>
        ) : (
          <div className="glass divide-y divide-white/5">
            {appointments.map(apt => (
              <div key={apt.id} className="p-4 flex items-center gap-4">
                <div className="w-10 h-10 rounded-xl bg-primary-600/20 flex items-center justify-center
                                text-primary-300 font-bold text-sm">
                  #{apt.tokenNumber}
                </div>
                <div className="flex-1">
                  <p className="font-medium text-slate-200">{apt.patientName ?? 'Patient'}</p>
                  <p className="text-xs text-slate-500">{format(new Date(apt.scheduledAt), 'h:mm a')}</p>
                </div>
                <span className={`badge ${STATUS_BADGE[apt.status] ?? 'badge-blue'}`}>{apt.status}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function getGreeting() {
  const h = new Date().getHours()
  return h < 12 ? 'morning' : h < 17 ? 'afternoon' : 'evening'
}
