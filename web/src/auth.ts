export type UserRole = 'customer' | 'admin'

export interface SessionUser {
  role: UserRole
  name: string
}

const STORAGE_KEY = 'smart-customer-service-session'

export function login(role: UserRole, username: string, password: string): SessionUser | null {
  const normalized = username.trim()
  const matched =
    (role === 'customer' && normalized === 'user' && password === '123456') ||
    (role === 'admin' && normalized === 'admin' && password === 'admin123')

  if (!matched) return null

  const user: SessionUser = {
    role,
    name: role === 'admin' ? '后台管理员' : '演示用户'
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
  return user
}

export function currentUser(): SessionUser | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as SessionUser
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

export function logout() {
  localStorage.removeItem(STORAGE_KEY)
}
