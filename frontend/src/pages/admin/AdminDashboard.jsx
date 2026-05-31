import { useQuery } from '@tanstack/react-query'
import { analyticsApi } from '../../api/api'
import {
  AreaChart, Area, BarChart, Bar, XAxis, YAxis, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell
} from 'recharts'
import {
  BarChart3, Users, Clock, CheckCircle2, XCircle,
  Activity, RefreshCw, Loader2
} from 'lucide-react'

const CLINIC_ID = 1 // TODO: admin selects clinic from dropdown

const COLORS = ['#6366f1', '#8b5cf6', '#06b6d4', '#10b981', '#f59e0b', '#ef4444']

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null
  return (
    <div className="glass px-3 py-2 text-sm">
      <p className="text-slate-400 mb-1">{label}</p>
      {payload.map((p, i) => (
        <p key={i} style={{ color: p.color }} className="font-semibold">
          {p.name}: {p.value}
        </p>
      ))}
    </div>
  )
}

export default function AdminDashboard() {
  const { data, isLoading, refetch, isFetching } = useQuery({
    queryKey: ['clinicSummary', CLINIC_ID],
    queryFn:  () => analyticsApi.getClinicSummary(CLINIC_ID).then(r => r.data.data),
    refetchInterval: 60000,
  })

  const weeklyData = Object.entries(data?.weeklyTrend ?? {}).map(([date, count]) => ({
    date: date.slice(5), count
  }))

  const peakData = Object.entries(data?.peakHourDistribution ?? {})
    .map(([h, c]) => ({ hour: `${h}:00`, count: c }))
    .sort((a, b) => parseInt(a.hour) - parseInt(b.hour))

  const doctorData = Object.entries(data?.doctorThroughput ?? {})
    .map(([name, count]) => ({ name: name.split(' ').slice(-1)[0], count }))

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between animate-slide-up">
        <div>
          <h1 className="text-3xl font-bold text-white flex items-center gap-3">
            <BarChart3 className="w-7 h-7 text-primary-400" />
            Analytics Dashboard
          </h1>
          <p className="text-slate-400 mt-1">
            {data?.clinicName ?? 'Loading…'} — Real-time clinic performance
          </p>
        </div>
        <button
          onClick={() => refetch()}
          disabled={isFetching}
          className="btn-secondary flex items-center gap-2 text-sm"
        >
          <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 text-primary-400 animate-spin" />
        </div>
      ) : (
        <>
          {/* Stat cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 animate-slide-up">
            {[
              { label: "Today's Total",  value: data?.totalAppointmentsToday ?? 0,  icon: <Activity className="w-5 h-5 text-blue-400" />,    color: 'text-blue-300',    bg: 'bg-blue-500/10' },
              { label: 'Completed',      value: data?.completedToday ?? 0,           icon: <CheckCircle2 className="w-5 h-5 text-emerald-400" />, color: 'text-emerald-300', bg: 'bg-emerald-500/10' },
              { label: 'Cancelled',      value: data?.cancelledToday ?? 0,           icon: <XCircle className="w-5 h-5 text-red-400" />,      color: 'text-red-300',     bg: 'bg-red-500/10' },
              { label: 'Currently Waiting', value: data?.currentlyWaiting ?? 0,      icon: <Users className="w-5 h-5 text-amber-400" />,      color: 'text-amber-300',   bg: 'bg-amber-500/10' },
            ].map(s => (
              <div key={s.label} className="glass p-5">
                <div className={`w-10 h-10 rounded-xl ${s.bg} flex items-center justify-center mb-3`}>
                  {s.icon}
                </div>
                <p className={`text-3xl font-bold ${s.color}`}>{s.value}</p>
                <p className="text-slate-400 text-sm mt-1">{s.label}</p>
              </div>
            ))}
          </div>

          {/* Avg wait time highlight */}
          <div className="glass p-5 flex items-center gap-4 animate-slide-up">
            <div className="w-12 h-12 rounded-xl bg-primary-600/20 flex items-center justify-center">
              <Clock className="w-6 h-6 text-primary-400" />
            </div>
            <div>
              <p className="text-slate-400 text-sm">Average Wait Time Today</p>
              <p className="text-3xl font-bold text-primary-300">
                {data?.avgWaitTimeMinutes ? `${Math.round(data.avgWaitTimeMinutes)} min` : '—'}
              </p>
            </div>
          </div>

          {/* Charts row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 animate-slide-up">
            {/* Weekly trend */}
            <div className="glass p-6">
              <h2 className="font-semibold text-white mb-4">7-Day Appointment Trend</h2>
              <ResponsiveContainer width="100%" height={200}>
                <AreaChart data={weeklyData}>
                  <defs>
                    <linearGradient id="grad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%"   stopColor="#6366f1" stopOpacity={0.4} />
                      <stop offset="95%"  stopColor="#6366f1" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <XAxis dataKey="date" stroke="#475569" tick={{ fontSize: 11 }} />
                  <YAxis stroke="#475569" tick={{ fontSize: 11 }} />
                  <Tooltip content={<CustomTooltip />} />
                  <Area type="monotone" dataKey="count" name="Appointments"
                        stroke="#6366f1" fill="url(#grad)" strokeWidth={2} dot={false} />
                </AreaChart>
              </ResponsiveContainer>
            </div>

            {/* Peak hours */}
            <div className="glass p-6">
              <h2 className="font-semibold text-white mb-4">Peak Hour Distribution</h2>
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={peakData} barSize={16}>
                  <XAxis dataKey="hour" stroke="#475569" tick={{ fontSize: 10 }} />
                  <YAxis stroke="#475569" tick={{ fontSize: 11 }} />
                  <Tooltip content={<CustomTooltip />} />
                  <Bar dataKey="count" name="Appointments" fill="#8b5cf6" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Doctor throughput */}
          {doctorData.length > 0 && (
            <div className="glass p-6 animate-slide-up">
              <h2 className="font-semibold text-white mb-4">Doctor Throughput Today</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <ResponsiveContainer width="100%" height={200}>
                  <PieChart>
                    <Pie data={doctorData} dataKey="count" nameKey="name"
                         cx="50%" cy="50%" outerRadius={80} paddingAngle={3}>
                      {doctorData.map((_, i) => (
                        <Cell key={i} fill={COLORS[i % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip content={<CustomTooltip />} />
                  </PieChart>
                </ResponsiveContainer>
                <div className="space-y-3">
                  {doctorData.map((d, i) => (
                    <div key={d.name} className="flex items-center gap-3">
                      <div className="w-3 h-3 rounded-full flex-shrink-0"
                           style={{ backgroundColor: COLORS[i % COLORS.length] }} />
                      <span className="text-slate-300 text-sm flex-1">Dr. {d.name}</span>
                      <span className="font-semibold text-white">{d.count}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
