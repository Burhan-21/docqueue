import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import toast from 'react-hot-toast'
import { Activity, Mail, Lock, ArrowRight, Loader2, KeyRound } from 'lucide-react'

export default function Login() {
  const { login, verifyOtp, sendOtp } = useAuth()
  const navigate  = useNavigate()
  const [form, setForm]       = useState({ email: '', password: '' })
  const [otpCode, setOtpCode] = useState('')
  const [otpStep, setOtpStep] = useState(false)
  const [loading, setLoading] = useState(false)
  const [verifiedHuman, setVerifiedHuman] = useState(false)

  const handleChange = e => setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))

  const handleSubmit = async e => {
    e.preventDefault()
    if (!form.email || !form.password) return toast.error('All fields are required')
    setLoading(true)
    try {
      const response = await login(form.email, form.password)
      if (response.otpRequired) {
        toast.success(response.message || 'OTP sent to email. Please verify.')
        setOtpStep(true)
      }
    } catch (err) {
      toast.error(err.response?.data?.error ?? 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  const handleVerifyOtp = async e => {
    e.preventDefault()
    if (!otpCode) return toast.error('Please enter the verification code')
    setLoading(true)
    try {
      const user = await verifyOtp(form.email, otpCode)
      toast.success(`Welcome back, ${user.name}! 👋`)
      if (user.role === 'DOCTOR') navigate('/doctor')
      else if (user.role === 'ADMIN') navigate('/admin')
      else navigate('/dashboard')
    } catch (err) {
      toast.error(err.response?.data?.error ?? 'Verification failed')
    } finally {
      setLoading(false)
    }
  }

  const handleResendOtp = async () => {
    if (!verifiedHuman) {
      return toast.error('Please verify that you are human (CAPTCHA)')
    }
    setLoading(true)
    try {
      const res = await sendOtp(form.email, 'g-recaptcha-response-dummy')
      toast.success(res.message || 'Verification OTP resent successfully')
    } catch (err) {
      toast.error(err.response?.data?.error ?? 'Failed to resend OTP')
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
        {/* Brand */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl
                          bg-gradient-to-br from-primary-500 to-violet-600 mb-4 shadow-xl shadow-primary-900/40">
            <Activity className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white mb-1">DocQueue Portal</h1>
          <p className="text-slate-400">Secure Two-Factor Authentication</p>
        </div>

        {/* Form card */}
        <div className="glass p-8">
          {!otpStep ? (
            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label className="input-label">Email address</label>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <input
                    name="email" type="email" value={form.email}
                    onChange={handleChange} placeholder="you@example.com"
                    className="input pl-10" autoComplete="email"
                  />
                </div>
              </div>

              <div>
                <label className="input-label">Password</label>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <input
                    name="password" type="password" value={form.password}
                    onChange={handleChange} placeholder="••••••••"
                    className="input pl-10" autoComplete="current-password"
                  />
                </div>
              </div>

              <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2 mt-2">
                {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowRight className="w-4 h-4" />}
                {loading ? 'Verifying...' : 'Continue'}
              </button>
            </form>
          ) : (
            <form onSubmit={handleVerifyOtp} className="space-y-5">
              <div className="text-center mb-2">
                <p className="text-sm text-slate-300">
                  Enter the 6-digit OTP code sent to <br />
                  <span className="font-semibold text-primary-400">{form.email}</span>
                </p>
              </div>

              <div>
                <label className="input-label">One-Time Password (OTP)</label>
                <div className="relative">
                  <KeyRound className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <input
                    name="otp" type="text" maxLength={6} value={otpCode}
                    onChange={e => setOtpCode(e.target.value.replace(/\D/g, ''))}
                    placeholder="123456" className="input pl-10 text-center tracking-widest text-lg font-bold"
                  />
                </div>
              </div>

              {/* Simulated CAPTCHA Box */}
              <div className="bg-slate-900/50 border border-white/5 rounded-xl p-4 flex items-center justify-between">
                <label className="flex items-center gap-3 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={verifiedHuman}
                    onChange={e => setVerifiedHuman(e.target.checked)}
                    className="w-5 h-5 rounded border-white/10 bg-slate-950 text-primary-500 focus:ring-0 focus:ring-offset-0"
                  />
                  <span className="text-xs text-slate-300 font-medium">I am not a robot</span>
                </label>
                <div className="flex flex-col items-end">
                  <span className="text-[10px] text-slate-500">reCAPTCHA</span>
                  <span className="text-[8px] text-slate-600">Privacy & Terms</span>
                </div>
              </div>

              <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2">
                {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowRight className="w-4 h-4" />}
                {loading ? 'Verifying OTP...' : 'Verify & Sign In'}
              </button>

              <div className="flex justify-between items-center text-xs mt-2">
                <button
                  type="button"
                  onClick={handleResendOtp}
                  disabled={loading}
                  className="text-primary-400 hover:text-primary-300 font-medium disabled:text-slate-500"
                >
                  Resend OTP Code
                </button>
                <button
                  type="button"
                  onClick={() => setOtpStep(false)}
                  className="text-slate-400 hover:text-slate-300"
                >
                  Back to login
                </button>
              </div>
            </form>
          )}

          <div className="mt-6 pt-6 border-t border-white/5 text-center">
            <p className="text-slate-500 text-sm">
              New patient?{' '}
              <Link to="/register" className="text-primary-400 font-medium hover:text-primary-300 transition-colors">
                Create an account
              </Link>
            </p>
          </div>
        </div>

        {/* Demo credentials hint */}
        <div className="mt-4 glass p-4 text-center">
          <p className="text-xs text-slate-500">
            <span className="text-slate-400 font-medium">Demo Admin:</span>{' '}
            admin@docqueue.in / Admin@1234
          </p>
        </div>
      </div>
    </div>
  )
}
