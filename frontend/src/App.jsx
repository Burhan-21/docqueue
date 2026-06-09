import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'

// Auth pages
import Login          from './pages/auth/Login'
import Register       from './pages/auth/Register'
import ForgotPassword from './pages/auth/ForgotPassword'
import ResetPassword  from './pages/auth/ResetPassword'

// Patient pages
import PatientDashboard   from './pages/patient/PatientDashboard'
import SearchDoctors      from './pages/patient/SearchDoctors'
import BookAppointment    from './pages/patient/BookAppointment'
import MyAppointments     from './pages/patient/MyAppointments'
import QueueTracker       from './pages/patient/QueueTracker'

// Doctor pages
import DoctorDashboard from './pages/doctor/DoctorDashboard'
import QueueManager    from './pages/doctor/QueueManager'

// Admin pages
import AdminDashboard  from './pages/admin/AdminDashboard'
import ManageDoctors   from './pages/admin/ManageDoctors'
import ManageClinics   from './pages/admin/ManageClinics'

// Layout
import ProtectedRoute from './components/layout/ProtectedRoute'
import AiAssistant from './components/AiAssistant'

export default function App() {
  const { user } = useAuth()

  return (
    <>
      <Routes>
      {/* Public */}
      <Route path="/login"           element={user ? <Navigate to="/" /> : <Login />} />
      <Route path="/register"        element={user ? <Navigate to="/" /> : <Register />} />
      <Route path="/forgot-password" element={user ? <Navigate to="/" /> : <ForgotPassword />} />
      <Route path="/reset-password"  element={user ? <Navigate to="/" /> : <ResetPassword />} />
      
      {/* Root redirect based on role */}
      <Route path="/"         element={<RoleDashboard />} />

      {/* Patient routes */}
      <Route element={<ProtectedRoute roles={['PATIENT']} />}>
        <Route path="/dashboard"             element={<PatientDashboard />} />
        <Route path="/doctors"               element={<SearchDoctors />} />
        <Route path="/doctors/:id/book"      element={<BookAppointment />} />
        <Route path="/appointments"          element={<MyAppointments />} />
        <Route path="/queue/:appointmentId"  element={<QueueTracker />} />
      </Route>

      {/* Doctor routes */}
      <Route element={<ProtectedRoute roles={['DOCTOR']} />}>
        <Route path="/doctor"         element={<DoctorDashboard />} />
        <Route path="/doctor/queue"   element={<QueueManager />} />
      </Route>

      {/* Admin routes */}
      <Route element={<ProtectedRoute roles={['ADMIN']} />}>
        <Route path="/admin"          element={<AdminDashboard />} />
        <Route path="/admin/doctors"  element={<ManageDoctors />} />
        <Route path="/admin/clinics"  element={<ManageClinics />} />
      </Route>

      <Route path="*" element={<Navigate to="/" />} />
      </Routes>
      
      {/* Global AI Assistant for Patients */}
      {user?.role === 'PATIENT' && <AiAssistant />}
    </>
  )
}

function RoleDashboard() {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" />
  if (user.role === 'DOCTOR') return <Navigate to="/doctor" />
  if (user.role === 'ADMIN')  return <Navigate to="/admin" />
  return <Navigate to="/dashboard" />
}
