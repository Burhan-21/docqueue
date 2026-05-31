import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { clinicApi } from '../../api/api'
import toast from 'react-hot-toast'
import { Building2, Plus, Trash2, MapPin, Loader2 } from 'lucide-react'

export default function ManageClinics() {
  const queryClient = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ name: '', address: '', phone: '', email: '', city: '', state: '' })

  const { data, isLoading } = useQuery({
    queryKey: ['adminClinics'],
    queryFn:  () => clinicApi.list().then(r => r.data.data),
  })

  const createMutation = useMutation({
    mutationFn: () => clinicApi.create(form),
    onSuccess: () => {
      toast.success('Clinic created')
      queryClient.invalidateQueries({ queryKey: ['adminClinics'] })
      setShowForm(false)
      setForm({ name: '', address: '', phone: '', email: '', city: '', state: '' })
    },
    onError: (err) => toast.error(err.response?.data?.error ?? 'Failed'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id) => clinicApi.remove(id),
    onSuccess: () => { toast.success('Clinic deactivated'); queryClient.invalidateQueries({ queryKey: ['adminClinics'] }) },
  })

  const change = (k, v) => setForm(f => ({ ...f, [k]: v }))

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <div className="flex items-center justify-between animate-slide-up">
        <div>
          <h1 className="text-3xl font-bold text-white flex items-center gap-3">
            <Building2 className="w-7 h-7 text-primary-400" /> Manage Clinics
          </h1>
          <p className="text-slate-400 mt-1">Add and manage clinic locations</p>
        </div>
        <button onClick={() => setShowForm(s => !s)} className="btn-primary flex items-center gap-2">
          <Plus className="w-4 h-4" /> Add Clinic
        </button>
      </div>

      {showForm && (
        <div className="glass p-6 space-y-4 animate-slide-up">
          <h2 className="font-semibold text-white">New Clinic</h2>
          <div className="grid grid-cols-2 gap-4">
            {[
              ['name',    'Clinic Name *', 'City Health Center'],
              ['city',    'City *',        'Mumbai'],
              ['state',   'State',         'Maharashtra'],
              ['phone',   'Phone',         '9876543210'],
              ['email',   'Email',         'clinic@example.com'],
            ].map(([k, l, p]) => (
              <div key={k}>
                <label className="input-label">{l}</label>
                <input value={form[k]} onChange={e => change(k, e.target.value)}
                       placeholder={p} className="input" />
              </div>
            ))}
            <div className="col-span-2">
              <label className="input-label">Address</label>
              <textarea value={form.address} onChange={e => change('address', e.target.value)}
                        placeholder="Full clinic address" rows={2} className="input resize-none" />
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            <button onClick={() => createMutation.mutate()} disabled={createMutation.isPending}
                    className="btn-primary flex items-center gap-2">
              {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
              Create Clinic
            </button>
            <button onClick={() => setShowForm(false)} className="btn-secondary">Cancel</button>
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="flex items-center justify-center h-40">
          <Loader2 className="w-7 h-7 text-primary-400 animate-spin" />
        </div>
      ) : (
        <div className="glass divide-y divide-white/5 animate-fade-in">
          {(data?.content ?? []).map(clinic => (
            <div key={clinic.id} className="p-4 flex items-center gap-4">
              <div className="w-10 h-10 rounded-xl bg-violet-600/20 flex items-center justify-center">
                <Building2 className="w-5 h-5 text-violet-400" />
              </div>
              <div className="flex-1">
                <p className="font-medium text-slate-200">{clinic.name}</p>
                <p className="text-sm text-slate-500 flex items-center gap-1">
                  <MapPin className="w-3 h-3" /> {clinic.city}{clinic.state ? `, ${clinic.state}` : ''}
                </p>
              </div>
              <span className={`badge ${clinic.isActive ? 'badge-green' : 'badge-red'}`}>
                {clinic.isActive ? 'Active' : 'Inactive'}
              </span>
              <button onClick={() => { if (window.confirm('Deactivate clinic?')) deleteMutation.mutate(clinic.id) }}
                      className="p-2 rounded-lg text-slate-500 hover:text-red-400 hover:bg-red-500/10 transition-all">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
          {(data?.content ?? []).length === 0 && (
            <div className="p-10 text-center">
              <Building2 className="w-10 h-10 text-slate-600 mx-auto mb-3" />
              <p className="text-slate-500">No clinics added yet</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
