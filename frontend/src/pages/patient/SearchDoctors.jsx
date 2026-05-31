import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { doctorApi } from '../../api/api'
import { Search, MapPin, Clock, Loader2, Stethoscope, ChevronRight } from 'lucide-react'

const SPECIALIZATIONS = [
  'All', 'Cardiologist', 'Dermatologist', 'Neurologist', 'Orthopedic',
  'Pediatrician', 'Psychiatrist', 'General Physician', 'ENT', 'Ophthalmologist',
]

export default function SearchDoctors() {
  const [query, setQuery] = useState('')
  const [spec,  setSpec]  = useState('')
  const [page,  setPage]  = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['doctors', spec, page],
    queryFn:  () => doctorApi.search(spec || null, page).then(r => r.data.data),
    placeholderData: (prev) => prev,
  })

  const handleSpec = s => { setSpec(s === 'All' ? '' : s); setPage(0) }

  const filtered = (data?.content ?? []).filter(d =>
    !query || d.name.toLowerCase().includes(query.toLowerCase()) ||
    d.specialization?.toLowerCase().includes(query.toLowerCase())
  )

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <div className="animate-slide-up">
        <h1 className="text-3xl font-bold text-white">Find a Doctor</h1>
        <p className="text-slate-400 mt-1">Search by name or specialization, then book instantly</p>
      </div>

      {/* Search bar */}
      <div className="relative animate-slide-up">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500" />
        <input
          value={query} onChange={e => setQuery(e.target.value)}
          placeholder="Search doctors by name…"
          className="input pl-12 py-4 text-lg"
        />
      </div>

      {/* Specialization filter chips */}
      <div className="flex gap-2 flex-wrap animate-slide-up">
        {SPECIALIZATIONS.map(s => (
          <button
            key={s}
            onClick={() => handleSpec(s)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium border transition-all duration-200
              ${(s === 'All' && spec === '') || spec === s
                ? 'bg-primary-600 border-primary-500 text-white'
                : 'bg-white/5 border-white/10 text-slate-400 hover:border-white/20 hover:text-slate-200'
              }`}
          >
            {s}
          </button>
        ))}
      </div>

      {/* Results */}
      {isLoading ? (
        <div className="flex items-center justify-center h-40">
          <Loader2 className="w-8 h-8 text-primary-400 animate-spin" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="glass p-16 text-center">
          <Stethoscope className="w-14 h-14 text-slate-600 mx-auto mb-4" />
          <p className="text-slate-300 font-semibold text-lg">No doctors found</p>
          <p className="text-slate-500 mt-1">Try a different specialization or search term</p>
        </div>
      ) : (
        <>
          <p className="text-sm text-slate-500">{data?.totalElements ?? filtered.length} doctors found</p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {filtered.map(doctor => (
              <DoctorCard key={doctor.id} doctor={doctor} />
            ))}
          </div>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-center gap-3 pt-4">
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

function DoctorCard({ doctor }) {
  return (
    <div className="glass-hover p-5 group">
      <div className="flex items-start gap-4">
        {/* Avatar */}
        <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-primary-600/40 to-violet-600/40
                        flex items-center justify-center flex-shrink-0 text-xl font-bold text-primary-300">
          {doctor.name.charAt(0)}
        </div>
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-white text-lg">Dr. {doctor.name}</h3>
          <p className="text-primary-400 text-sm font-medium">{doctor.specialization}</p>
          {doctor.qualification && (
            <p className="text-slate-500 text-xs mt-0.5">{doctor.qualification}</p>
          )}
          <div className="flex items-center gap-4 mt-2 text-xs text-slate-400">
            <span className="flex items-center gap-1">
              <MapPin className="w-3 h-3" /> {doctor.clinicName}
            </span>
            <span className="flex items-center gap-1">
              <Clock className="w-3 h-3" /> {doctor.avgConsultMin} min
            </span>
            {doctor.consultationFee && (
              <span className="text-emerald-400 font-medium">₹{doctor.consultationFee}</span>
            )}
          </div>
        </div>
      </div>

      {doctor.bio && (
        <p className="text-slate-500 text-sm mt-3 line-clamp-2">{doctor.bio}</p>
      )}

      <Link
        to={`/doctors/${doctor.id}/book`}
        className="btn-primary w-full mt-4 flex items-center justify-center gap-2 text-sm"
      >
        Book Appointment <ChevronRight className="w-4 h-4" />
      </Link>
    </div>
  )
}
