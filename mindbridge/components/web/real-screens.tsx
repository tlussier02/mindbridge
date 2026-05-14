"use client"

import React, { useEffect, useMemo, useState } from "react"
import { useApp } from "@/lib/app-context"
import { api, getAuth, saveAuth, type DiarySummary, type SessionModule } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { Brain, BookOpen, HeartPulse, ShieldAlert, TrendingUp } from "lucide-react"

function Notice({ loading, error, success }: { loading?: boolean; error?: string | null; success?: string | null }) {
  if (!loading && !error && !success) return null
  return <div className={`rounded-xl border p-3 text-sm ${error ? "border-destructive/30 bg-destructive/10 text-destructive" : "border-primary/30 bg-primary/10 text-primary"}`}>{loading ? "Loading..." : error || success}</div>
}

function Section({ title, children, action }: { title: string; children: React.ReactNode; action?: React.ReactNode }) {
  return <div className="space-y-4"><div className="flex items-center justify-between gap-3"><h1 className="text-3xl font-bold tracking-tight">{title}</h1>{action}</div>{children}</div>
}

export function WebAuthScreen() {
  const { setScreen, setUserName } = useApp()
  const [mode, setMode] = useState<"login" | "register">("login")
  const [form, setForm] = useState({ name: "", email: "", password: "" })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const auth = getAuth()
    if (auth?.accessToken) { setUserName(auth.name || "User"); setScreen("dashboard") }
  }, [setScreen, setUserName])

  async function submit(e: React.FormEvent) {
    e.preventDefault(); setLoading(true); setError(null)
    try {
      const auth = mode === "login" ? await api.auth.login(form) : await api.auth.register(form)
      setUserName(auth.name || form.name || "User")
      setScreen("dashboard")
    } catch (err: any) { setError(err.message) } finally { setLoading(false) }
  }

  return <div className="mx-auto max-w-md space-y-6 py-8">
    <div className="text-center"><Brain className="mx-auto mb-3 h-12 w-12 text-primary"/><h1 className="text-3xl font-bold">MindBridge</h1><p className="text-muted-foreground">Sign in to use the live Digital Therapy Assistant backend.</p></div>
    <Card className="space-y-4 p-6"><div className="grid grid-cols-2 gap-2"><Button variant={mode === "login" ? "default" : "outline"} onClick={() => setMode("login")}>Login</Button><Button variant={mode === "register" ? "default" : "outline"} onClick={() => setMode("register")}>Register</Button></div>
      <form className="space-y-3" onSubmit={submit}>{mode === "register" && <Input placeholder="Name" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })}/>}<Input type="email" placeholder="Email" required value={form.email} onChange={e => setForm({ ...form, email: e.target.value })}/><Input type="password" placeholder="Password" required minLength={8} value={form.password} onChange={e => setForm({ ...form, password: e.target.value })}/><Notice loading={loading} error={error}/><Button className="w-full" type="submit" disabled={loading}>{mode === "login" ? "Login" : "Create account"}</Button></form>
    </Card>
  </div>
}

export function RealDashboard() {
  const { setScreen, setUserName } = useApp()
  const [weekly, setWeekly] = useState<any>(null), [entries, setEntries] = useState<DiarySummary[]>([]), [error, setError] = useState<string | null>(null)
  useEffect(() => { const a=getAuth(); if(!a){setScreen("welcome"); return} setUserName(a.name||"User"); Promise.all([api.progress.weekly(), api.diary.entries(0,3)]).then(([w,d])=>{setWeekly(w); setEntries(d.content||[])}).catch(e=>setError(e.message)) }, [setScreen,setUserName])
  return <Section title="Dashboard" action={<Button variant="outline" onClick={async()=>{await api.auth.logout(); setScreen("welcome")}}>Logout</Button>}>
    <Notice error={error}/><div className="grid gap-4 md:grid-cols-4">{[[Brain,"CBT Sessions","session-library"],[BookOpen,"Thought Diary","diary-home"],[TrendingUp,"Progress","progress-weekly"],[ShieldAlert,"Crisis Support","crisis-detection"]].map(([Icon,label,target]: any)=><Card key={label} className="cursor-pointer p-5 hover:bg-secondary" onClick={()=>setScreen(target)}><Icon className="mb-3 h-7 w-7 text-primary"/><h3 className="font-semibold">{label}</h3><p className="text-sm text-muted-foreground">Open live backend workflow</p></Card>)}</div>
    <div className="grid gap-4 md:grid-cols-3"><Card className="p-5"><p className="text-sm text-muted-foreground">Sessions this week</p><p className="text-3xl font-bold">{weekly?.sessionsCompleted ?? "—"}</p></Card><Card className="p-5"><p className="text-sm text-muted-foreground">Diary entries</p><p className="text-3xl font-bold">{weekly?.diaryEntries ?? entries.length}</p></Card><Card className="p-5"><p className="text-sm text-muted-foreground">Average mood</p><p className="text-3xl font-bold">{weekly?.averageMood ?? "—"}</p></Card></div>
    <Card className="p-5"><h2 className="mb-3 font-semibold">Recent diary entries</h2>{entries.length ? entries.map(e=><p key={e.id} className="border-t py-2 text-sm">{e.situation || e.automaticThought || e.id}</p>) : <p className="text-sm text-muted-foreground">No entries yet.</p>}</Card>
  </Section>
}

export function RealSessionLibrary() {
  const { setScreen, selectSession } = useApp(); const [items,setItems]=useState<SessionModule[]>([]); const [history,setHistory]=useState<any[]>([]); const [error,setError]=useState<string|null>(null)
  useEffect(()=>{Promise.all([api.sessions.library(), api.sessions.history()]).then(([l,h])=>{setItems(l); setHistory(h)}).catch(e=>setError(e.message))},[])
  async function start(id:string){try{const active=await api.sessions.start(id); selectSession((active.userSessionId || active.sessionId || id) as any); setScreen("session-chat")}catch(e:any){setError(e.message)}}
  return <Section title="CBT Sessions"><Notice error={error}/><div className="grid gap-4 md:grid-cols-2">{items.map(s=><Card key={s.id} className="p-5"><h2 className="font-semibold">{s.title || s.name || "CBT Session"}</h2><p className="mt-2 text-sm text-muted-foreground">{s.description || "Therapeutic session module"}</p><Button className="mt-4" onClick={()=>start(s.id)}>Start session</Button></Card>)}</div><Card className="p-5"><h2 className="mb-2 font-semibold">Session history</h2>{history.length ? history.map((h,i)=><p key={i} className="border-t py-2 text-sm">{h.title || h.sessionTitle || h.id}</p>) : <p className="text-sm text-muted-foreground">No completed sessions yet.</p>}</Card></Section>
}

export function RealSessionChat() {
  const { selectedSessionId, setScreen } = useApp(); const sid = String(selectedSessionId || ""); const [messages,setMessages]=useState<{role:string;text:string}[]>([]); const [input,setInput]=useState(""); const [error,setError]=useState<string|null>(null); const [loading,setLoading]=useState(false)
  async function send(){ if(!input.trim() || !sid) return; const msg=input; setInput(""); setMessages(m=>[...m,{role:"You",text:msg}]); setLoading(true); try{const r=await api.sessions.chat(sid,msg); setMessages(m=>[...m,{role:"MindBridge",text:r.response||r.message||r.content||"I hear you. Let's explore that thought."}])}catch(e:any){setError(e.message)}finally{setLoading(false)} }
  async function end(){ if(sid) await api.sessions.end(sid); setScreen("session-library") }
  return <Section title="AI CBT Chat" action={<Button variant="outline" onClick={end}>End session</Button>}><Notice loading={loading} error={error}/><Card className="min-h-[360px] space-y-3 p-4">{messages.length?messages.map((m,i)=><div key={i} className={m.role==="You"?"text-right":"text-left"}><span className="inline-block max-w-[80%] rounded-2xl bg-secondary p-3 text-sm"><b>{m.role}: </b>{m.text}</span></div>):<p className="text-sm text-muted-foreground">Start the conversation. Your message is sent to the Spring Boot session chat endpoint.</p>}</Card><div className="flex gap-2"><Input value={input} onChange={e=>setInput(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') send()}} placeholder="Type your thought or concern..."/><Button onClick={send}>Send</Button></div></Section>
}

export function RealDiaryHome() {
  const { setScreen, selectDiaryEntry } = useApp(); const [entries,setEntries]=useState<DiarySummary[]>([]); const [error,setError]=useState<string|null>(null)
  function load(){api.diary.entries().then(p=>setEntries(p.content||[])).catch(e=>setError(e.message))} useEffect(load,[])
  async function remove(id:string){if(!confirm("Delete this entry?"))return; await api.diary.remove(id); load()}
  return <Section title="Thought Diary" action={<Button onClick={()=>setScreen("diary-new")}>New entry</Button>}><Notice error={error}/>{entries.length?entries.map(e=><Card key={e.id} className="p-5"><div className="flex justify-between gap-3"><div><h2 className="font-semibold">{e.situation || "Diary entry"}</h2><p className="text-sm text-muted-foreground">{e.automaticThought || e.createdAt || e.date}</p></div><div className="flex gap-2"><Button variant="outline" onClick={()=>selectDiaryEntry(e.id)}>View</Button><Button variant="destructive" onClick={()=>remove(e.id)}>Delete</Button></div></div></Card>):<Card className="p-8 text-center text-muted-foreground">No diary entries yet.</Card>}</Section>
}

export function RealDiaryNew() {
  const { setScreen } = useApp(); const [situation,setSituation]=useState(""); const [thought,setThought]=useState(""); const [emotion,setEmotion]=useState("Anxious"); const [suggestions,setSuggestions]=useState<any[]>([]); const [error,setError]=useState<string|null>(null); const [success,setSuccess]=useState<string|null>(null)
  async function suggest(){try{setSuggestions(await api.diary.suggest(thought))}catch(e:any){setError(e.message)}}
  async function save(){try{await api.diary.create({ situation, automaticThought: thought, emotions:[{emotion,intensity:5}], distortionIds: suggestions.map(s=>s.distortionId||s.id).filter(Boolean), moodBefore:4, moodAfter:6 }); setSuccess("Entry saved."); setScreen("diary-home")}catch(e:any){setError(e.message)}}
  return <Section title="New Thought Diary Entry"><Notice error={error} success={success}/><Card className="space-y-4 p-5"><Input placeholder="Situation" value={situation} onChange={e=>setSituation(e.target.value)}/><Textarea placeholder="Automatic thought" value={thought} onChange={e=>setThought(e.target.value)}/><Input placeholder="Main emotion" value={emotion} onChange={e=>setEmotion(e.target.value)}/><div className="flex gap-2"><Button variant="outline" onClick={suggest}>Suggest distortions</Button><Button onClick={save}>Save entry</Button></div><div className="flex flex-wrap gap-2">{suggestions.length > 0 ? suggestions.map((s,i)=><Badge key={i}>{s.name || s.distortionName || s.distortionId || "Suggested distortion"} {s.confidence ? `${Math.round(s.confidence*100)}%` : ""}</Badge>) : <p className="text-sm text-muted-foreground">No distortion suggestions returned for this thought.</p>}</div></Card></Section>
}

export function RealDiaryDetail() { const { selectedDiaryId, setScreen }=useApp(); const [d,setD]=useState<any>(null); const [error,setError]=useState<string|null>(null); useEffect(()=>{if(selectedDiaryId) api.diary.detail(String(selectedDiaryId)).then(setD).catch(e=>setError(e.message))},[selectedDiaryId]); return <Section title="Diary Detail" action={<Button variant="outline" onClick={()=>setScreen("diary-home")}>Back</Button>}><Notice error={error}/><Card className="space-y-3 p-5"><pre className="whitespace-pre-wrap text-sm">{d?JSON.stringify(d,null,2):"Loading entry..."}</pre></Card></Section> }

export function RealProgress() { const [data,setData]=useState<any>({}); const [error,setError]=useState<string|null>(null); useEffect(()=>{Promise.all([api.progress.weekly(),api.progress.monthly(),api.progress.burnout(),api.progress.achievements()]).then(([weekly,monthly,burnout,achievements])=>setData({weekly,monthly,burnout,achievements})).catch(e=>setError(e.message))},[]); return <Section title="Progress Dashboard"><Notice error={error}/><div className="grid gap-4 md:grid-cols-4"><Card className="p-5"><p className="text-sm text-muted-foreground">Weekly sessions</p><p className="text-3xl font-bold">{data.weekly?.sessionsCompleted ?? "—"}</p></Card><Card className="p-5"><p className="text-sm text-muted-foreground">Streak</p><p className="text-3xl font-bold">{data.weekly?.streakDays ?? "—"}</p></Card><Card className="p-5"><p className="text-sm text-muted-foreground">Burnout score</p><p className="text-3xl font-bold">{data.burnout?.score ?? data.burnout?.burnoutScore ?? "—"}</p></Card><Card className="p-5"><p className="text-sm text-muted-foreground">Achievements</p><p className="text-3xl font-bold">{data.achievements?.length ?? "—"}</p></Card></div><Card className="p-5"><pre className="whitespace-pre-wrap text-sm">{JSON.stringify(data,null,2)}</pre></Card></Section> }

export function RealCrisis() { const auth=useMemo(()=>getAuth(),[]); const [text,setText]=useState(""); const [result,setResult]=useState<any>(null); const [hub,setHub]=useState<any>(null); const [plan,setPlan]=useState<any>(null); const [error,setError]=useState<string|null>(null); useEffect(()=>{api.crisis.hub().then(setHub).catch(e=>setError(e.message)); if(auth?.userId) api.crisis.safetyPlan().then(setPlan).catch(()=>{})},[auth?.userId]); async function detect(){try{setResult(await api.crisis.detect(text))}catch(e:any){setError(e.message)}} return <Section title="Crisis Support"><Notice error={error}/><Card className="space-y-3 p-5"><h2 className="font-semibold">Crisis detection</h2><Textarea placeholder="Describe what is happening..." value={text} onChange={e=>setText(e.target.value)}/><Button onClick={detect}>Check risk level</Button>{result&&<pre className="whitespace-pre-wrap rounded-xl bg-secondary p-3 text-sm">{JSON.stringify(result,null,2)}</pre>}</Card><div className="grid gap-4 md:grid-cols-2"><Card className="p-5"><h2 className="mb-2 font-semibold">Emergency resources</h2>{hub?.emergencyResources?.map((r:any,i:number)=><p key={i} className="border-t py-2 text-sm"><b>{r.name}</b> — {r.phone} {r.available24x7 ? "(24/7)" : ""}</p>) || <p className="text-sm text-muted-foreground">Loading resources...</p>}</Card><Card className="p-5"><h2 className="mb-2 font-semibold">Safety plan</h2><pre className="whitespace-pre-wrap text-sm">{plan?JSON.stringify(plan,null,2):"No saved safety plan loaded."}</pre></Card></div></Section> }
