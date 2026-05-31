import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import AppLayout from './AppLayout'

export default function ProtectedRoute({ roles }) {
  const { user } = useAuth()

  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />

  return (
    <AppLayout>
      <Outlet />
    </AppLayout>
  )
}
