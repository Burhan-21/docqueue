import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { authApi } from '../../api/api'
import toast from 'react-hot-toast'
import { Activity, Lock, ArrowRight, Loader2, ArrowLeft } from 'lucide-react'

export default function ResetPassword() {
  const [searchParams] = useSearchParams()
  // Allow token to come from URL ?token=xyz or be manually typed if they only got the code
  const initialToken = searchParams.get('token') || ''
  
  const navigate = useNavigate()
  const [form, setForm] = useState({ token: initialToken, password: '', confirmPassword: '' })
  const [loading, setLoading] = useState(false)

  const handleChange = e => setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))

  const handleSubmit = async e => {
    e.preventDefault()
    if (!form.token || !form.password || !form.confirmPassword) {
      return toast.error('All fields are required')
    }
    if (form.password.length < 8) {
      return toast.error('Password must be at least 8 characters long')
    }
    if (form.password !== form.confirmPassword) {
      return toast.error('Passwords do not match')
    }

    setLoading(true)
    try {
      const res = await authApi.resetPassword({ 
        resetToken: form.token, 
        newPassword: form.password 
      })
      toast.success(res.data?.message || 'Password reset successfully')
      navigate('/login')
    } catch (err) {
      toast.error(err.response?.data?.error ?? 'Failed to reset password')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -left-40 w-96 h-96 bg-primary-600/20 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-emerald-600/15 rounded-full blur-3xl" />
      </div>

      <div className="w-full max-w-md animate-slide-up relative z-10">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl
                          bg-gradient-to-br from-primary-500 to-emerald-600 mb-4 shadow-xl shadow-primary-900/40">
            <Activity className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white mb-1">Create New Password</h1>
          <p className="text-slate-400">Your new password must be at least 8 characters</p>
        </div>

        <div className="glass p-8">
          <form onSubmit={handleSubmit} className="space-y-5">
            {!initialToken && (
              <div>
                <label className="input-label">Reset Token</label>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <input
                    name="token" type="text" value={form.token}
                    onChange={handleChange} placeholder="Enter your reset token"
                    className="input pl-10" required
                  />
                </div>
              </div>
            )}

            <div>
              <label className="input-label">New Password</label>
              <div className="relative">
                <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  name="password" type="password" value={form.password}
                  onChange={handleChange} placeholder="••••••••"
                  className="input pl-10" required
                />
              </div>
            </div>

            <div>
              <label className="input-label">Confirm Password</label>
              <div className="relative">
                <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  name="confirmPassword" type="password" value={form.confirmPassword}
                  onChange={handleChange} placeholder="••••••••"
                  className="input pl-10" required
                />
              </div>
            </div>

            <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2 mt-2">
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowRight className="w-4 h-4" />}
              {loading ? 'Resetting...' : 'Reset Password'}
            </button>
          </form>

          <div className="mt-6 pt-6 border-t border-white/5 text-center">
            <Link to="/login" className="inline-flex items-center gap-2 text-slate-400 hover:text-white transition-colors text-sm font-medium">
              <ArrowLeft className="w-4 h-4" /> Back to login
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
