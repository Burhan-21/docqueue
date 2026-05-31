import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import toast from 'react-hot-toast'
import { Activity, Mail, Lock, User, Phone, ArrowRight, Loader2, KeyRound } from 'lucide-react'

export default function Register() {
  const { register, verifyOtp, sendOtp } = useAuth()
  const navigate     = useNavigate()
  const [form, setForm]       = useState({ name: '', email: '', phone: '', password: '' })
  const [otpCode, setOtpCode] = useState('')
  const [otpStep, setOtpStep] = useState(false)
  const [loading, setLoading] = useState(false)
  const [verifiedHuman, setVerifiedHuman] = useState(false)

  const handleChange = e => setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))

  const handleSubmit = async e => {
    e.preventDefault()
    if (!form.name || !form.email || !form.password) return toast.error('Please fill all required fields')
    if (form.password.length < 8) return toast.error('Password must be at least 8 characters')

    setLoading(true)
    try {
      const response = await register(form)
      if (response.otpRequired) {
        toast.success(response.message || 'Account registered. Verification OTP sent.')
        setOtpStep(true)
      }
    } catch (err) {
      toast.error(err.response?.data?.error ?? 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  const handleVerifyOtp = async e => {
    e.preventDefault()
    if (!otpCode) return toast.error('Please enter the verification code')
    setLoading(true)
    try {
      await verifyOtp(form.email, otpCode)
      toast.success('Account verified! Welcome to DocQueue 🎉')
      navigate('/dashboard')
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
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-96 h-96 bg-violet-600/15 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 -left-40 w-96 h-96 bg-primary-600/20 rounded-full blur-3xl" />
      </div>

      <div className="w-full max-w-md animate-slide-up relative z-10">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl
                          bg-gradient-to-br from-primary-500 to-violet-600 mb-4 shadow-xl shadow-primary-900/40">
            <Activity className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white mb-1">Create account</h1>
          <p className="text-slate-400">Join DocQueue — skip the waiting room</p>
        </div>

        <div className="glass p-8">
          {!otpStep ? (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="input-label">Full Name <span className="text-red-400">*</span></label>
                <div className="relative">
                  <User className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <input name="name" value={form.name} onChange={handleChange}
                         placeholder="Rahul Sharma" className="input pl-10" />
                </div>
              </div>
              <div>
                <label className="input-label">Email <span className="text-red-400">*</span></label>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <input name="email" type="email" value={form.email} onChange={handleChange}
                         placeholder="rahul@gmail.com" className="input pl-10" />
                </div>
              </div>
              <div>
                <label className="input-label">Phone (Indian)</label>
                <div className="relative">
                  <Phone className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <input name="phone" value={form.phone} onChange={handleChange}
                         placeholder="9876543210" className="input pl-10" maxLength={10} />
                </div>
              </div>
              <div>
                <label className="input-label">Password <span className="text-red-400">*</span></label>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <input name="password" type="password" value={form.password} onChange={handleChange}
                         placeholder="Min 8 chars, uppercase + number" className="input pl-10" />
                </div>
                <p className="text-xs text-slate-500 mt-1">Must contain uppercase, lowercase, and a digit</p>
              </div>

              <button type="submit" disabled={loading}
                      className="btn-primary w-full flex items-center justify-center gap-2 mt-2">
                {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowRight className="w-4 h-4" />}
                {loading ? 'Creating account…' : 'Create Account'}
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
                  Back to register
                </button>
              </div>
            </form>
          )}

          <div className="mt-6 pt-6 border-t border-white/5 text-center">
            <p className="text-slate-500 text-sm">
              Already have an account?{' '}
              <Link to="/login" className="text-primary-400 font-medium hover:text-primary-300 transition-colors">
                Sign in
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
