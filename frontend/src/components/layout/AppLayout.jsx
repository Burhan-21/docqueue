import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import {
  Home, Search, Calendar, Users, BarChart3,
  Building2, Menu, LogOut, ChevronRight, Activity
} from 'lucide-react'

const NAV = {
  PATIENT: [
    { to: '/dashboard',   icon: Home,      label: 'Dashboard'      },
    { to: '/doctors',     icon: Search,    label: 'Find Doctors'   },
    { to: '/appointments',icon: Calendar,  label: 'Appointments'   },
  ],
  DOCTOR: [
    { to: '/doctor',      icon: Home,      label: 'Dashboard'      },
    { to: '/doctor/queue',icon: Activity,  label: 'Queue Manager'  },
  ],
  ADMIN: [
    { to: '/admin',         icon: BarChart3,  label: 'Analytics'   },
    { to: '/admin/doctors', icon: Users,      label: 'Doctors'     },
    { to: '/admin/clinics', icon: Building2,  label: 'Clinics'     },
  ],
}

export default function AppLayout({ children }) {
  const { user, logout } = useAuth()
  const navigate  = useNavigate()
  const [open, setOpen] = useState(false)
  const links = NAV[user?.role] ?? []

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <div className="flex min-h-screen">
      {/* ===== Mobile overlay ===== */}
      {open && (
        <div className="fixed inset-0 bg-black/60 z-20 lg:hidden"
             onClick={() => setOpen(false)} />
      )}

      {/* ===== Sidebar ===== */}
      <aside className={`
        fixed inset-y-0 left-0 z-30 w-64 flex flex-col
        bg-[#0d0d1f] border-r border-white/5
        transition-transform duration-300 ease-in-out
        ${open ? 'translate-x-0' : '-translate-x-full'}
        lg:translate-x-0 lg:static lg:flex
      `}>
        {/* Logo */}
        <div className="p-6 border-b border-white/5">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-500 to-violet-600 flex items-center justify-center">
              <Activity className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="font-bold text-white tracking-tight">DocQueue</h1>
              <p className="text-[10px] text-slate-500 uppercase tracking-widest">{user?.role}</p>
            </div>
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
          {links.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to} to={to}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              onClick={() => setOpen(false)}
            >
              <Icon className="w-5 h-5" />
              <span>{label}</span>
              <ChevronRight className="w-4 h-4 ml-auto opacity-30" />
            </NavLink>
          ))}
        </nav>

        {/* User info + Logout */}
        <div className="p-4 border-t border-white/5">
          <div className="glass p-3 mb-3">
            <p className="text-sm font-medium text-slate-200 truncate">{user?.name}</p>
            <p className="text-xs text-slate-500 truncate">{user?.email}</p>
          </div>
          <button
            onClick={handleLogout}
            className="btn-danger w-full flex items-center justify-center gap-2 text-sm"
          >
            <LogOut className="w-4 h-4" />
            Sign Out
          </button>
        </div>
      </aside>

      {/* ===== Main content ===== */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Mobile header */}
        <header className="lg:hidden sticky top-0 z-10 bg-[#0a0a14]/80 backdrop-blur-md
                           border-b border-white/5 px-4 py-3 flex items-center gap-3">
          <button onClick={() => setOpen(true)} className="p-2 rounded-lg hover:bg-white/5">
            <Menu className="w-5 h-5 text-slate-400" />
          </button>
          <div className="flex items-center gap-2">
            <Activity className="w-5 h-5 text-primary-400" />
            <span className="font-semibold text-white">DocQueue</span>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 p-6 animate-fade-in overflow-auto">
          {children}
        </main>
      </div>
    </div>
  )
}
