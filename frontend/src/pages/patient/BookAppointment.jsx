import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { doctorApi, appointmentApi } from '../../api/api'
import toast from 'react-hot-toast'
import { format, addDays, startOfTomorrow } from 'date-fns'
import { Calendar, Clock, ChevronLeft, Loader2, Check, MapPin } from 'lucide-react'

export default function BookAppointment() {
  const { id }       = useParams()
  const navigate     = useNavigate()
  const [date, setDate]       = useState(format(startOfTomorrow(), 'yyyy-MM-dd'))
  const [slot, setSlot]       = useState(null)
  const [notes, setNotes]     = useState('')

  const { data: doctor, isLoading: loadingDoc } = useQuery({
    queryKey: ['doctor', id],
    queryFn:  () => doctorApi.getById(id).then(r => r.data.data),
  })

  const { data: slots, isLoading: loadingSlots } = useQuery({
    queryKey: ['slots', id, date],
    queryFn:  () => doctorApi.getSlots(id, date).then(r => r.data.data),
    enabled: !!date,
  })

  const bookMutation = useMutation({
    mutationFn: () => appointmentApi.book({
      doctorId:    Number(id),
      scheduledAt: `${date}T${slot}:00`,
      notes,
    }),
    onSuccess: (res) => {
      toast.success(`Appointment booked! Token #${res.data.data.tokenNumber} 🎉`)
      navigate(`/queue/${res.data.data.id}`)
    },
    onError: (err) => {
      toast.error(err.response?.data?.error ?? 'Booking failed')
    },
  })

  if (loadingDoc) return <LoadingState />

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Back */}
      <button onClick={() => navigate(-1)}
              className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors text-sm">
        <ChevronLeft className="w-4 h-4" /> Back to search
      </button>

      {/* Doctor info card */}
      <div className="glass p-6 flex items-center gap-5 animate-slide-up">
        <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-primary-600/40 to-violet-600/40
                        flex items-center justify-center text-2xl font-bold text-primary-300">
          {doctor?.name?.charAt(0)}
        </div>
        <div>
          <h1 className="text-xl font-bold text-white">Dr. {doctor?.name}</h1>
          <p className="text-primary-400">{doctor?.specialization}</p>
          <div className="flex items-center gap-3 text-sm text-slate-400 mt-1">
            <span className="flex items-center gap-1"><MapPin className="w-3.5 h-3.5" />{doctor?.clinicName}</span>
            <span className="flex items-center gap-1"><Clock className="w-3.5 h-3.5" />{doctor?.avgConsultMin} min</span>
            {doctor?.consultationFee && <span className="text-emerald-400 font-semibold">₹{doctor.consultationFee}</span>}
          </div>
        </div>
      </div>

      {/* Date picker */}
      <div className="glass p-6 space-y-4 animate-slide-up">
        <h2 className="font-semibold text-white flex items-center gap-2">
          <Calendar className="w-5 h-5 text-primary-400" /> Select Date
        </h2>
        <div className="flex gap-2 flex-wrap">
          {Array.from({ length: 7 }).map((_, i) => {
            const d    = addDays(startOfTomorrow(), i)
            const val  = format(d, 'yyyy-MM-dd')
            const disp = i === 0 ? 'Tomorrow' : format(d, 'EEE, dd MMM')
            return (
              <button
                key={val} onClick={() => { setDate(val); setSlot(null) }}
                className={`px-4 py-2 rounded-xl text-sm font-medium border transition-all
                  ${date === val
                    ? 'bg-primary-600 border-primary-500 text-white'
                    : 'bg-white/5 border-white/10 text-slate-400 hover:border-white/20'}`}
              >
                {disp}
              </button>
            )
          })}
        </div>
      </div>

      {/* Time slots */}
      <div className="glass p-6 space-y-4 animate-slide-up">
        <h2 className="font-semibold text-white flex items-center gap-2">
          <Clock className="w-5 h-5 text-violet-400" /> Available Slots
        </h2>
        {loadingSlots ? (
          <div className="flex items-center gap-2 text-slate-500 py-4">
            <Loader2 className="w-4 h-4 animate-spin" /> Loading slots…
          </div>
        ) : !slots || slots.length === 0 ? (
          <p className="text-slate-500 py-4">No slots available for this date.</p>
        ) : (
          <div className="grid grid-cols-4 sm:grid-cols-6 gap-2">
            {slots.map(s => (
              <button
                key={s} onClick={() => setSlot(s)}
                className={`py-2 rounded-xl text-sm font-medium border transition-all
                  ${slot === s
                    ? 'bg-violet-600 border-violet-500 text-white'
                    : 'bg-white/5 border-white/10 text-slate-400 hover:border-white/20 hover:text-white'}`}
              >
                {s}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Notes */}
      <div className="glass p-6 space-y-3 animate-slide-up">
        <label className="input-label">Notes / Symptoms (optional)</label>
        <textarea
          value={notes} onChange={e => setNotes(e.target.value)}
          placeholder="Describe your symptoms or reason for visit…"
          rows={3}
          className="input resize-none"
          maxLength={500}
        />
        <p className="text-xs text-slate-600 text-right">{notes.length}/500</p>
      </div>

      {/* Confirm button */}
      <button
        disabled={!slot || bookMutation.isPending}
        onClick={() => bookMutation.mutate()}
        className="btn-primary w-full flex items-center justify-center gap-2 text-base py-4 animate-slide-up"
      >
        {bookMutation.isPending
          ? <><Loader2 className="w-5 h-5 animate-spin" /> Booking…</>
          : <><Check className="w-5 h-5" /> Confirm Appointment {slot && `at ${slot}`}</>
        }
      </button>
    </div>
  )
}

function LoadingState() {
  return (
    <div className="flex items-center justify-center h-48">
      <Loader2 className="w-8 h-8 text-primary-400 animate-spin" />
    </div>
  )
}
