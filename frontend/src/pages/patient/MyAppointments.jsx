import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { appointmentApi } from '../../api/api'
import toast from 'react-hot-toast'
import { format } from 'date-fns'
import { Calendar, Clock, X, Loader2, Activity } from 'lucide-react'

const STATUS_BADGE = {
  CONFIRMED: 'badge-green',
  PENDING:   'badge-amber',
  COMPLETED: 'badge-blue',
  CANCELLED: 'badge-red',
  NO_SHOW:   'badge-red',
}

export default function MyAppointments() {
  const [page, setPage] = useState(0)
  const queryClient     = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['myAppointments', page],
    queryFn:  () => appointmentApi.getMyList(page).then(r => r.data.data),
  })

  const cancelMutation = useMutation({
    mutationFn: (id) => appointmentApi.cancel(id, 'Cancelled by patient', 'PATIENT'),
    onSuccess: () => {
      toast.success('Appointment cancelled')
      queryClient.invalidateQueries({ queryKey: ['myAppointments'] })
    },
    onError: (err) => toast.error(err.response?.data?.error ?? 'Cancel failed'),
  })

  const appointments = data?.content ?? []

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="animate-slide-up">
        <h1 className="text-3xl font-bold text-white">My Appointments</h1>
        <p className="text-slate-400 mt-1">All your bookings in one place</p>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center h-48">
          <Loader2 className="w-7 h-7 text-primary-400 animate-spin" />
        </div>
      ) : appointments.length === 0 ? (
        <div className="glass p-16 text-center">
          <Calendar className="w-14 h-14 text-slate-600 mx-auto mb-4" />
          <p className="text-slate-300 font-semibold text-lg">No appointments yet</p>
          <Link to="/doctors" className="btn-primary inline-flex mt-4 text-sm gap-2">
            Book your first appointment
          </Link>
        </div>
      ) : (
        <>
          <div className="space-y-3">
            {appointments.map(apt => (
              <div key={apt.id} className="glass p-5 flex items-center gap-5 animate-fade-in">
                {/* Token */}
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-primary-600 to-violet-600
                                flex flex-col items-center justify-center flex-shrink-0 text-white">
                  <span className="text-[8px] uppercase tracking-wider opacity-70">Token</span>
                  <span className="text-lg font-bold">{apt.tokenNumber}</span>
                </div>

                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-white">Dr. {apt.doctorName}</p>
                  <p className="text-sm text-slate-400">{apt.doctorSpecialization} · {apt.clinicName}</p>
                  <div className="flex items-center gap-1 text-xs text-slate-500 mt-1">
                    <Clock className="w-3 h-3" />
                    {format(new Date(apt.scheduledAt), 'dd MMM yyyy, h:mm a')}
                  </div>
                </div>

                <div className="flex flex-col items-end gap-2">
                  <span className={`badge ${STATUS_BADGE[apt.status] ?? 'badge-blue'}`}>{apt.status}</span>
                  <div className="flex gap-2">
                    {['CONFIRMED', 'PENDING'].includes(apt.status) && (
                      <>
                        <Link to={`/queue/${apt.id}`}
                              className="text-xs text-primary-400 hover:text-primary-300 transition-colors flex items-center gap-1">
                          <Activity className="w-3 h-3" /> Track
                        </Link>
                        <button
                          onClick={() => {
                            if (window.confirm('Cancel this appointment?')) cancelMutation.mutate(apt.id)
                          }}
                          className="text-xs text-red-400 hover:text-red-300 transition-colors flex items-center gap-1"
                          disabled={cancelMutation.isPending}
                        >
                          <X className="w-3 h-3" /> Cancel
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-center gap-3">
              <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
                      className="btn-secondary disabled:opacity-40">← Prev</button>
              <span className="text-slate-400 text-sm">Page {page + 1} of {data.totalPages}</span>
              <button disabled={data.last} onClick={() => setPage(p => p + 1)}
                      className="btn-secondary disabled:opacity-40">Next →</button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
