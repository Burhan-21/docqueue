import { createContext, useContext, useState, useCallback } from 'react'
import { authApi } from '../api/api'
import toast from 'react-hot-toast'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  const login = useCallback(async (email, password) => {
    const { data } = await authApi.login({ email, password })
    const payload = data.data
    if (!payload.otpRequired) {
      localStorage.setItem('accessToken',  payload.accessToken)
      localStorage.setItem('refreshToken', payload.refreshToken)
      const userInfo = { id: payload.userId, name: payload.name, role: payload.role, email }
      localStorage.setItem('user', JSON.stringify(userInfo))
      setUser(userInfo)
    }
    return payload // Will contain { otpRequired: true/false, message: "..." }
  }, [])

  const register = useCallback(async (formData) => {
    const { data } = await authApi.register(formData)
    return data.data // Will contain { otpRequired: true, message: "..." }
  }, [])

  const verifyOtp = useCallback(async (email, code) => {
    const { data } = await authApi.verifyOtp({ email, code })
    const payload = data.data
    localStorage.setItem('accessToken',  payload.accessToken)
    localStorage.setItem('refreshToken', payload.refreshToken)
    const userInfo = { id: payload.userId, name: payload.name, role: payload.role, email }
    localStorage.setItem('user', JSON.stringify(userInfo))
    setUser(userInfo)
    return userInfo
  }, [])

  const sendOtp = useCallback(async (email, captchaToken) => {
    const { data } = await authApi.sendOtp({ email, captchaToken })
    return data.data
  }, [])

  const logout = useCallback(async () => {
    try { await authApi.logout() } catch (err) { console.warn(err) }
    localStorage.clear()
    setUser(null)
    toast.success('Logged out successfully')
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, register, verifyOtp, sendOtp, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
