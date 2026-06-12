import axios from 'axios'
import toast from 'react-hot-toast'

const API_BASE = import.meta.env.VITE_API_URL || '/api/v1'

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// ===== Request Interceptor: attach JWT =====
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// ===== Response Interceptor: handle 401 / errors =====
api.interceptors.response.use(
  res => res,
  async err => {
    const { response } = err
    if (!response) {
      toast.error('Network error. Check your connection.')
      return Promise.reject(err)
    }
    if (response.status === 429) {
      toast.error('Too many requests. Please slow down.', { id: 'rate-limit' })
      return Promise.reject(err)
    }
    if (response.status === 401 && !err.config.url.includes('/auth/login')) {
      // Try refresh
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken && !err.config._retry) {
        err.config._retry = true
        try {
          const { data } = await axios.post(`${API_BASE}/auth/refresh`, { refreshToken })
          const newToken = data.data.accessToken
          localStorage.setItem('accessToken', newToken)
          err.config.headers.Authorization = `Bearer ${newToken}`
          return api(err.config)
        } catch {
          localStorage.clear()
          window.location.href = '/login'
        }
      } else {
        localStorage.clear()
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

export default api

// ===== Auth =====
export const authApi = {
  login:          (data) => api.post('/auth/login', data),
  register:       (data) => api.post('/auth/register', data),
  logout:         ()     => api.post('/auth/logout'),
  sendOtp:        (data) => api.post('/auth/otp/send', data),
  verifyOtp:      (data) => api.post('/auth/otp/verify', data),
  forgotPassword: (data) => api.post('/auth/password/forgot', data),
  resetPassword:  (data) => api.post('/auth/password/reset', data),
}

// ===== Doctors =====
export const doctorApi = {
  search:          (spec, page = 0) => api.get('/doctors', { params: { specialization: spec, page, size: 12 } }),
  getById:         (id)             => api.get(`/doctors/${id}`),
  getSlots:        (id, date)       => api.get(`/doctors/${id}/slots`, { params: { date } }),
  getAvailability: (id)             => api.get(`/doctors/${id}/availability`),
  setAvailability: (id, slots)      => api.put(`/doctors/${id}/availability`, slots),
  create:          (data)           => api.post('/doctors', data),
  update:          (id, data)       => api.put(`/doctors/${id}`, data),
  remove:          (id)             => api.delete(`/doctors/${id}`),
}

// ===== Clinics =====
export const clinicApi = {
  list:   (page = 0) => api.get('/clinics', { params: { page } }),
  getById:(id)       => api.get(`/clinics/${id}`),
  create: (data)     => api.post('/clinics', data),
  update: (id, data) => api.put(`/clinics/${id}`, data),
  remove: (id)       => api.delete(`/clinics/${id}`),
}

// ===== Appointments =====
export const appointmentApi = {
  book:       (data)              => api.post('/appointments', data),
  getMyList:  (page = 0)          => api.get('/appointments/my', { params: { page, size: 10 } }),
  cancel:     (id, reason, by)    => api.put(`/appointments/${id}/cancel`, null, { params: { reason, cancelledBy: by } }),
  reschedule: (id, data)          => api.put(`/appointments/${id}/reschedule`, data),
  doctorToday:()                  => api.get('/appointments/doctor/today'),
}

// ===== Queue =====
export const queueApi = {
  getQueue:    (doctorId)      => api.get(`/queue/doctor/${doctorId}`),
  getPosition: (appointmentId) => api.get(`/queue/position/${appointmentId}`),
  callNext:    (doctorId)      => api.post(`/queue/next/${doctorId}`),
  skip:        (apptId, dId)   => api.put(`/queue/skip/${apptId}`, null, { params: { doctorId: dId } }),
}

// ===== Patient =====
export const patientApi = {
  getProfile:    ()     => api.get('/patients/me'),
  updateProfile: (data) => api.put('/patients/me', data),
}

// ===== Analytics =====
export const analyticsApi = {
  getClinicSummary: (clinicId) => api.get(`/analytics/clinic/${clinicId}/summary`),
}

// ===== AI Assistant =====
export const aiApi = {
  chat: (query) => api.post('/ai/chat', { query }),
}
