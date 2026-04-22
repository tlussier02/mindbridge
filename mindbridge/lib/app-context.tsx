"use client"

import React, { createContext, useContext, useState, useCallback } from "react"

export type Screen =
  | "welcome"
  | "user-type"
  | "privacy"
  | "assessment"
  | "personalization"
  | "onboarding-complete"
  | "dashboard"
  | "session-library"
  | "session-intro"
  | "session-scenario"
  | "session-thought-challenge"
  | "session-progress"
  | "session-completion"
  | "session-rating"
  | "session-chat"
  | "session-multimodal"
  | "session-history"
  | "session-detail"
  | "diary-home"
  | "diary-detail"
  | "diary-new"
  | "diary-situation"
  | "diary-thoughts"
  | "diary-distortions"
  | "diary-reframe"
  | "diary-saved"
  | "diary-insights"
  | "crisis-detection"
  | "crisis-coping"
  | "crisis-resources"
  | "crisis-safety-plan"
  | "progress-weekly"
  | "progress-trends"
  | "progress-achievements"

export type ViewMode = "mobile" | "web"

interface AppState {
  screen: Screen
  viewMode: ViewMode
  userName: string
  userType: "individual" | "therapist-referred" | null
  assessmentScore: number
  sessionProgress: number
  diaryEntries: DiaryEntry[]
  weeklyMood: number[]
  burnoutScore: number
  sessionsCompleted: number
  streakDays: number
  selectedSessionId: number | null
  selectedDiaryId: string | null
}

export interface DiaryEntry {
  id: string
  date: string
  situation: string
  thoughts: string
  emotions: string[]
  distortions: string[]
  reframe: string
  moodBefore: number
  moodAfter: number
}

interface AppContextType extends AppState {
  setScreen: (screen: Screen) => void
  setViewMode: (mode: ViewMode) => void
  setUserName: (name: string) => void
  setUserType: (type: "individual" | "therapist-referred") => void
  setAssessmentScore: (score: number) => void
  setSessionProgress: (progress: number) => void
  addDiaryEntry: (entry: DiaryEntry) => void
  setBurnoutScore: (score: number) => void
  incrementSessions: () => void
  selectSession: (id: number) => void
  selectDiaryEntry: (id: string) => void
}

const AppContext = createContext<AppContextType | null>(null)

export function AppProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AppState>({
    screen: "welcome",
    viewMode: "web",
    userName: "",
    userType: null,
    assessmentScore: 0,
    sessionProgress: 0,
    diaryEntries: [
      {
        id: "1",
        date: "2026-02-18",
        situation: "Overwhelmed by work deadlines",
        thoughts: "I can never keep up. Everyone else manages fine.",
        emotions: ["Anxious", "Frustrated"],
        distortions: ["All-or-Nothing", "Comparison"],
        reframe: "I have successfully managed heavy workloads before. I can prioritize and ask for help.",
        moodBefore: 3,
        moodAfter: 6,
      },
      {
        id: "2",
        date: "2026-02-16",
        situation: "Skipped exercise for the third day",
        thoughts: "I have no discipline. What is the point?",
        emotions: ["Guilty", "Sad"],
        distortions: ["Labeling", "Catastrophizing"],
        reframe: "Missing a few days does not define me. I can start again today with a short walk.",
        moodBefore: 2,
        moodAfter: 5,
      },
    ],
    weeklyMood: [4, 5, 3, 6, 5, 7, 6],
    burnoutScore: 72,
    sessionsCompleted: 4,
    streakDays: 7,
    selectedSessionId: null,
    selectedDiaryId: null,
  })

  const setScreen = useCallback((screen: Screen) => {
    setState((prev) => ({ ...prev, screen }))
  }, [])

  const setViewMode = useCallback((viewMode: ViewMode) => {
    setState((prev) => ({ ...prev, viewMode }))
  }, [])

  const setUserName = useCallback((userName: string) => {
    setState((prev) => ({ ...prev, userName }))
  }, [])

  const setUserType = useCallback((userType: "individual" | "therapist-referred") => {
    setState((prev) => ({ ...prev, userType }))
  }, [])

  const setAssessmentScore = useCallback((assessmentScore: number) => {
    setState((prev) => ({ ...prev, assessmentScore }))
  }, [])

  const setSessionProgress = useCallback((sessionProgress: number) => {
    setState((prev) => ({ ...prev, sessionProgress }))
  }, [])

  const addDiaryEntry = useCallback((entry: DiaryEntry) => {
    setState((prev) => ({
      ...prev,
      diaryEntries: [entry, ...prev.diaryEntries],
    }))
  }, [])

  const setBurnoutScore = useCallback((burnoutScore: number) => {
    setState((prev) => ({ ...prev, burnoutScore }))
  }, [])

  const incrementSessions = useCallback(() => {
    setState((prev) => ({
      ...prev,
      sessionsCompleted: prev.sessionsCompleted + 1,
    }))
  }, [])

  const selectSession = useCallback((id: number) => {
    setState((prev) => ({ ...prev, selectedSessionId: id, screen: "session-detail" as Screen }))
  }, [])

  const selectDiaryEntry = useCallback((id: string) => {
    setState((prev) => ({ ...prev, selectedDiaryId: id, screen: "diary-detail" as Screen }))
  }, [])

  return (
    <AppContext.Provider
      value={{
        ...state,
        setScreen,
        setViewMode,
        setUserName,
        setUserType,
        setAssessmentScore,
        setSessionProgress,
        addDiaryEntry,
        setBurnoutScore,
        incrementSessions,
        selectSession,
        selectDiaryEntry,
      }}
    >
      {children}
    </AppContext.Provider>
  )
}

export function useApp() {
  const context = useContext(AppContext)
  if (!context) throw new Error("useApp must be used within AppProvider")
  return context
}
