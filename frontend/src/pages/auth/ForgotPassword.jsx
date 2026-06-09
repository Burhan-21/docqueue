import { useState } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../../api/api'
import toast from 'react-hot-toast'
import { Activity, Mail, ArrowRight, Loader2, ArrowLeft } from 'lucide-react'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [submitted, setSubmitted] = useState(false)

  const handleSubmit = async e => {
    e.preventDefault()
    if (!email) return toast.error('Please enter your email address')
    setLoading(true)
    try {
      const res = await authApi.forgotPassword({ email })
      toast.success(res.data?.message || 'Password reset link sent')
      setSubmitted(true)
    } catch (err) {
      toast.error(err.response?.data?.error ?? 'Failed to send reset link')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      {/* Background glow */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -left-40 w-96 h-96 bg-primary-600/20 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-violet-600/15 rounded-full blur-3xl" />
      </div>

      <div className="w-full max-w-md animate-slide-up relative z-10">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl
                          bg-gradient-to-br from-primary-500 to-violet-600 mb-4 shadow-xl shadow-primary-900/40">
            <Activity className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white mb-1">Reset Password</h1>
          <p className="text-slate-400">We'll send you instructions in email</p>
        </div>

        <div className="glass p-8">
          {!submitted ? (
            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label className="input-label">Email address</label>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <input
                    name="email" type="email" value={email}
                    onChange={e => setEmail(e.target.value)} placeholder="you@example.com"
                    className="input pl-10" required
                  />
                </div>
              </div>

              <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2 mt-2">
                {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowRight className="w-4 h-4" />}
                {loading ? 'Sending...' : 'Send Reset Link'}
              </button>
            </form>
          ) : (
            <div className="text-center py-4">
              <Mail className="w-12 h-12 text-primary-400 mx-auto mb-4" />
              <h2 className="text-xl font-semibold text-white mb-2">Check your email</h2>
              <p className="text-slate-400 text-sm mb-6">
                We sent a password reset link to <span className="text-white">{email}</span>. 
                Please check your inbox and spam folder.
              </p>
              <button 
                onClick={() => { setSubmitted(false); setEmail(''); }}
                className="btn-secondary w-full"
              >
                Try another email
              </button>
            </div>
          )}

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
