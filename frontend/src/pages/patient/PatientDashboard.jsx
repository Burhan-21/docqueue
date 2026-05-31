import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { appointmentApi } from '../../api/api'
import { Calendar, Clock, Search, Activity, ChevronRight, Loader2 } from 'lucide-react'
import { format, isToday, isTomorrow } from 'date-fns'

const STATUS_BADGE = {
  CONFIRMED:  'badge-green',
  PENDING:    'badge-amber',
  COMPLETED:  'badge-blue',
  CANCELLED:  'badge-red',
  NO_SHOW:    'badge-red',
}

function formatDate(dateStr) {
  const d = new Date(dateStr)
  if (isToday(d))    return `Today, ${format(d, 'h:mm a')}`
  if (isTomorrow(d)) return `Tomorrow, ${format(d, 'h:mm a')}`
  return format(d, 'dd MMM yyyy, h:mm a')
}

export default function PatientDashboard() {
  const { user } = useAuth()

  const { data, isLoading } = useQuery({
    queryKey: ['myAppointments'],
    queryFn:  () => appointmentApi.getMyList(0).then(r => r.data.data),
  })

  const upcoming = data?.content?.filter(a =>
    ['CONFIRMED', 'PENDING'].includes(a.status) && new Date(a.scheduledAt) >= new Date()
  ) ?? []

  const recent = data?.content?.slice(0, 5) ?? []

  return (
    <div className="max-w-5xl mx-auto space-y-8">
      {/* Header */}
      <div className="animate-slide-up">
        <h1 className="text-3xl font-bold text-white">
          Good {getGreeting()},{' '}
          <span className="text-gradient">{user?.name?.split(' ')[0]}</span> 👋
        </h1>
        <p className="text-slate-400 mt-1">Manage your appointments and track your queue position</p>
      </div>

      {/* Quick actions */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 animate-slide-up">
        <Link to="/doctors"
              className="glass-hover p-5 flex items-center gap-4 group">
          <div className="w-12 h-12 rounded-xl bg-primary-600/20 flex items-center justify-center group-hover:bg-primary-600/30 transition-colors">
            <Search className="w-6 h-6 text-primary-400" />
          </div>
          <div>
            <p className="font-semibold text-white">Find a Doctor</p>
            <p className="text-xs text-slate-400">Search by specialization</p>
          </div>
          <ChevronRight className="w-4 h-4 text-slate-600 ml-auto group-hover:text-primary-400 transition-colors" />
        </Link>
        <Link to="/appointments"
              className="glass-hover p-5 flex items-center gap-4 group">
          <div className="w-12 h-12 rounded-xl bg-violet-600/20 flex items-center justify-center group-hover:bg-violet-600/30 transition-colors">
            <Calendar className="w-6 h-6 text-violet-400" />
          </div>
          <div>
            <p className="font-semibold text-white">My Appointments</p>
            <p className="text-xs text-slate-400">View all bookings</p>
          </div>
          <ChevronRight className="w-4 h-4 text-slate-600 ml-auto group-hover:text-violet-400 transition-colors" />
        </Link>
        <div className="glass p-5 flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-600/20 flex items-center justify-center">
            <Activity className="w-6 h-6 text-emerald-400" />
          </div>
          <div>
            <p className="font-semibold text-white">{upcoming.length} Upcoming</p>
            <p className="text-xs text-slate-400">Active appointments</p>
          </div>
        </div>
      </div>

      {/* Upcoming appointments */}
      <div>
        <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
          <Clock className="w-5 h-5 text-primary-400" />
          Upcoming Appointments
        </h2>
        {isLoading ? (
          <div className="flex items-center justify-center h-32">
            <Loader2 className="w-6 h-6 text-primary-400 animate-spin" />
          </div>
        ) : upcoming.length === 0 ? (
          <div className="glass p-10 text-center">
            <Calendar className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <p className="text-slate-400 font-medium">No upcoming appointments</p>
            <Link to="/doctors" className="btn-primary inline-flex mt-4 text-sm gap-2">
              <Search className="w-4 h-4" /> Find a Doctor
            </Link>
          </div>
        ) : (
          <div className="space-y-3">
            {upcoming.map(apt => (
              <Link
                key={apt.id}
                to={`/queue/${apt.id}`}
                className="glass-hover p-5 flex items-center gap-5 group"
              >
                {/* Token badge */}
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-primary-600 to-violet-600
                                flex flex-col items-center justify-center flex-shrink-0">
                  <span className="text-[9px] text-primary-200 uppercase tracking-wider">Token</span>
                  <span className="text-xl font-bold text-white">{apt.tokenNumber}</span>
                </div>

                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-white truncate">Dr. {apt.doctorName}</p>
                  <p className="text-sm text-slate-400">{apt.doctorSpecialization} · {apt.clinicName}</p>
                  <p className="text-sm text-primary-400 mt-0.5">{formatDate(apt.scheduledAt)}</p>
                </div>

                <div className="flex flex-col items-end gap-2">
                  <span className={`badge ${STATUS_BADGE[apt.status] ?? 'badge-blue'}`}>{apt.status}</span>
                  <span className="text-xs text-slate-500 group-hover:text-primary-400 transition-colors">
                    Track queue →
                  </span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>

      {/* Recent history */}
      {recent.filter(a => a.status === 'COMPLETED').length > 0 && (
        <div>
          <h2 className="text-lg font-semibold text-white mb-4">Recent History</h2>
          <div className="glass divide-y divide-white/5">
            {recent.filter(a => a.status === 'COMPLETED').map(apt => (
              <div key={apt.id} className="p-4 flex items-center gap-4">
                <div className="flex-1">
                  <p className="text-sm font-medium text-slate-300">Dr. {apt.doctorName}</p>
                  <p className="text-xs text-slate-500">{format(new Date(apt.scheduledAt), 'dd MMM yyyy')}</p>
                </div>
                <span className="badge badge-blue">COMPLETED</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function getGreeting() {
  const h = new Date().getHours()
  if (h < 12) return 'morning'
  if (h < 17) return 'afternoon'
  return 'evening'
}
