export type AuthResponse = { accessToken: string; refreshToken: string; expiresIn: number; userId: string; name: string }
export type SessionModule = { id: string; title?: string; name?: string; description?: string; estimatedMinutes?: number; difficulty?: string }
export type SessionHistoryEntry = Record<string, any>
export type ActiveSession = { sessionId?: string; id?: string; moduleId?: string; status?: string; startedAt?: string }
export type ChatResponse = { response?: string; message?: string; content?: string; crisisDetected?: boolean; riskLevel?: string }
export type DiaryPage<T> = { content?: T[]; totalPages?: number; totalElements?: number; number?: number; size?: number }
export type DiarySummary = { id: string; situation?: string; automaticThought?: string; date?: string; createdAt?: string; moodBefore?: number; moodAfter?: number }
export type DiaryDetail = Record<string, any>
export type DistortionSuggestion = { id?: string; distortionId?: string; name?: string; distortionName?: string; confidence?: number; description?: string }

const STORAGE_KEY = "mindbridge.auth"
const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || ""

export function getAuth(): AuthResponse | null {
  if (typeof window === "undefined") return null
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try { return JSON.parse(raw) as AuthResponse } catch { return null }
}

export function saveAuth(auth: AuthResponse | null) {
  if (typeof window === "undefined") return
  if (!auth) localStorage.removeItem(STORAGE_KEY)
  else localStorage.setItem(STORAGE_KEY, JSON.stringify(auth))
}

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const auth = getAuth()
  const headers = new Headers(init.headers || {})
  if (!headers.has("Content-Type") && init.body) headers.set("Content-Type", "application/json")
  if (auth?.accessToken) headers.set("Authorization", `Bearer ${auth.accessToken}`)

  const res = await fetch(`${API_BASE}${path}`, { ...init, headers })
  if (res.status === 401 && retry && auth?.refreshToken) {
    try {
      const refreshed = await request<AuthResponse>("/auth/refresh", { method: "POST", body: JSON.stringify({ refreshToken: auth.refreshToken }) }, false)
      saveAuth(refreshed)
      return request<T>(path, init, false)
    } catch {
      saveAuth(null)
    }
  }
  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      const body = await res.json()
      message = body.message || body.error?.message || body.error?.code || message
    } catch {}
    throw new Error(message)
  }
  if (res.status === 204) return undefined as T

  const text = await res.text()
  if (!text) return undefined as T
  
  try {
    return JSON.parse(text) as T
  } catch {
    return text as T
  }
}

export const api = {
  auth: {
    async register(input: { name: string; email: string; password: string }) {
      const auth = await request<AuthResponse>("/auth/register", { method: "POST", body: JSON.stringify(input) })
      saveAuth(auth); return auth
    },
    async login(input: { email: string; password: string }) {
      const auth = await request<AuthResponse>("/auth/login", { method: "POST", body: JSON.stringify(input) })
      saveAuth(auth); return auth
    },
    async logout() {
      try { await request<void>("/auth/logout", { method: "POST" }) } finally { saveAuth(null) }
    },
  },
  sessions: {
    library: () => request<SessionModule[]>("/sessions"),
    history: () => request<SessionHistoryEntry[]>("/sessions/history"),
    detail: (id: string) => request<any>(`/sessions/${id}`),
    start: (id: string) => request<ActiveSession>(`/sessions/${id}/start`, { method: "POST", body: JSON.stringify({}) }),
    chat: (id: string, message: string) => request<ChatResponse>(`/sessions/${id}/chat`, { method: "POST", body: JSON.stringify({ message }) }),
    end: (id: string, reason = "completed") => request<any>(`/sessions/${id}/end`, { method: "POST", body: JSON.stringify({ reason }) }),
  },
  diary: {
    entries: (page = 0, size = 10) => request<DiaryPage<DiarySummary>>(`/diary/entries?page=${page}&size=${size}`),
    detail: (id: string) => request<DiaryDetail>(`/diary/entries/${id}`),
    create: (input: any) => request<any>("/diary/entries", { method: "POST", body: JSON.stringify(input) }),
    remove: (id: string) => request<void>(`/diary/entries/${id}`, { method: "DELETE" }),
    suggest: (thought: string) => request<DistortionSuggestion[]>("/diary/distortions/suggest", { method: "POST", body: JSON.stringify({ thought }) }),
    insights: () => request<any>("/diary/insights"),
  },
  progress: {
    weekly: () => request<any>("/progress/weekly"),
    monthly: () => request<any>("/progress/monthly"),
    burnout: () => request<any>("/progress/burnout"),
    achievements: () => request<any[]>("/progress/achievements"),
  },
  crisis: {
    hub: () => request<any>("/crisis"),
    coping: () => request<any[]>("/crisis/coping-strategies"),
    detect: (text: string) => request<any>("/crisis/detect", { method: "POST", body: JSON.stringify({ text }) }),
    safetyPlan: () => request<any>("/crisis/safety-plan"),
    updateSafetyPlan: (input: any) => request<any>("/crisis/safety-plan", { method: "PUT", body: JSON.stringify(input) }),
  },
}
