export type UserRole = 'customer' | 'admin'

export interface SessionUser {
  token: string
  userId: number
  username: string
  role: UserRole
  name: string
}

const STORAGE_KEY = 'smart-customer-service-session'

export function saveSession(user: SessionUser) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
}

export function saveAccessToken(token: string) {
  const user = currentUser()
  if (!user) return
  user.token = token
  saveSession(user)
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

export function sessionToken(): string | null {
  return currentUser()?.token ?? null
}

export function logout() {
  localStorage.removeItem(STORAGE_KEY)
}
