import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { doctorApi, clinicApi } from '../../api/api'
import toast from 'react-hot-toast'
import { Users, Plus, Trash2, Loader2, Stethoscope } from 'lucide-react'

export default function ManageDoctors() {
  const queryClient = useQueryClient()
  const page = 0
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ userId: '', clinicId: '', specialization: '', avgConsultMin: 15, consultationFee: '' })

  const { data, isLoading } = useQuery({
    queryKey: ['adminDoctors', page],
    queryFn:  () => doctorApi.search(null, page).then(r => r.data.data),
  })

  const { data: clinics } = useQuery({
    queryKey: ['clinics'],
    queryFn:  () => clinicApi.list().then(r => r.data.data?.content ?? []),
  })

  const createMutation = useMutation({
    mutationFn: () => doctorApi.create({ ...form, userId: Number(form.userId), clinicId: Number(form.clinicId), avgConsultMin: Number(form.avgConsultMin) }),
    onSuccess: () => {
      toast.success('Doctor created')
      queryClient.invalidateQueries({ queryKey: ['adminDoctors'] })
      setShowForm(false)
      setForm({ userId: '', clinicId: '', specialization: '', avgConsultMin: 15, consultationFee: '' })
    },
    onError: (err) => toast.error(err.response?.data?.error ?? 'Failed'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id) => doctorApi.remove(id),
    onSuccess: () => { toast.success('Doctor removed'); queryClient.invalidateQueries({ queryKey: ['adminDoctors'] }) },
    onError: (err) => toast.error(err.response?.data?.error ?? 'Delete failed'),
  })

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <div className="flex items-center justify-between animate-slide-up">
        <div>
          <h1 className="text-3xl font-bold text-white flex items-center gap-3">
            <Users className="w-7 h-7 text-primary-400" /> Manage Doctors
          </h1>
          <p className="text-slate-400 mt-1">Add, update, or remove doctor profiles</p>
        </div>
        <button onClick={() => setShowForm(s => !s)} className="btn-primary flex items-center gap-2">
          <Plus className="w-4 h-4" /> Add Doctor
        </button>
      </div>

      {/* Create form */}
      {showForm && (
        <div className="glass p-6 space-y-4 animate-slide-up">
          <h2 className="font-semibold text-white">New Doctor Profile</h2>
          <div className="grid grid-cols-2 gap-4">
            <div><label className="input-label">User ID</label>
              <input value={form.userId} onChange={e => setForm(f => ({...f, userId: e.target.value}))}
                     placeholder="User ID (from users table)" className="input" /></div>
            <div><label className="input-label">Clinic</label>
              <select value={form.clinicId} onChange={e => setForm(f => ({...f, clinicId: e.target.value}))} className="input">
                <option value="">Select clinic…</option>
                {(clinics ?? []).map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select></div>
            <div><label className="input-label">Specialization</label>
              <input value={form.specialization} onChange={e => setForm(f => ({...f, specialization: e.target.value}))}
                     placeholder="e.g. Cardiologist" className="input" /></div>
            <div><label className="input-label">Avg Consult (min)</label>
              <input type="number" value={form.avgConsultMin} onChange={e => setForm(f => ({...f, avgConsultMin: e.target.value}))}
                     className="input" min={5} max={120} /></div>
            <div><label className="input-label">Fee (₹)</label>
              <input type="number" value={form.consultationFee} onChange={e => setForm(f => ({...f, consultationFee: e.target.value}))}
                     placeholder="500" className="input" /></div>
          </div>
          <div className="flex gap-3 pt-2">
            <button onClick={() => createMutation.mutate()} disabled={createMutation.isPending}
                    className="btn-primary flex items-center gap-2">
              {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
              Create
            </button>
            <button onClick={() => setShowForm(false)} className="btn-secondary">Cancel</button>
          </div>
        </div>
      )}

      {/* Doctors list */}
      {isLoading ? (
        <div className="flex items-center justify-center h-40">
          <Loader2 className="w-7 h-7 text-primary-400 animate-spin" />
        </div>
      ) : (
        <div className="glass divide-y divide-white/5 animate-fade-in">
          {(data?.content ?? []).map(doc => (
            <div key={doc.id} className="p-4 flex items-center gap-4">
              <div className="w-10 h-10 rounded-xl bg-primary-600/20 flex items-center justify-center
                              text-primary-300 font-bold text-lg">
                {doc.name.charAt(0)}
              </div>
              <div className="flex-1 min-w-0">
                <p className="font-medium text-slate-200">Dr. {doc.name}</p>
                <p className="text-sm text-primary-400">{doc.specialization}</p>
                <p className="text-xs text-slate-500">{doc.clinicName} · {doc.avgConsultMin} min · {doc.consultationFee ? `₹${doc.consultationFee}` : 'Fee N/A'}</p>
              </div>
              <button onClick={() => { if (window.confirm('Remove doctor?')) deleteMutation.mutate(doc.id) }}
                      className="p-2 rounded-lg text-slate-500 hover:text-red-400 hover:bg-red-500/10 transition-all">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
          {(data?.content ?? []).length === 0 && (
            <div className="p-10 text-center">
              <Stethoscope className="w-10 h-10 text-slate-600 mx-auto mb-3" />
              <p className="text-slate-500">No doctors added yet</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
