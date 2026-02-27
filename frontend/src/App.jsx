import { useState, useEffect, useCallback, createContext, useContext } from "react";

// ─── API CLIENT ──────────────────────────────────────────────────────────────
const BASE = "http://localhost:8080/api";

const api = {
  _token: () => localStorage.getItem("tf_token"),
  _headers() {
    const h = { "Content-Type": "application/json" };
    if (this._token()) h["Authorization"] = `Bearer ${this._token()}`;
    return h;
  },
  async req(method, path, body) {
    const res = await fetch(BASE + path, {
      method,
      headers: this._headers(),
      body: body ? JSON.stringify(body) : undefined,
    });
    const json = await res.json();
    if (!json.success) throw new Error(json.message || "Request failed");
    return json.data;
  },
  auth:      { register: (d) => api.req("POST", "/auth/register", d), login: (d) => api.req("POST", "/auth/login", d) },
  tasks:     { create: (d) => api.req("POST", "/tasks", d), list: (p=0) => api.req("GET", `/tasks/my?page=${p}&size=50`), update: (id, d) => api.req("PATCH", `/tasks/${id}`, d), del: (id) => api.req("DELETE", `/tasks/${id}`) },
  dashboard: { get: () => api.req("GET", "/dashboard") },
};

// ─── AUTH CONTEXT ────────────────────────────────────────────────────────────
const AuthCtx = createContext(null);
function useAuth() { return useContext(AuthCtx); }

function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const u = localStorage.getItem("tf_user");
    return u ? JSON.parse(u) : null;
  });
  const login = (data) => {
    localStorage.setItem("tf_token", data.accessToken);
    localStorage.setItem("tf_user", JSON.stringify({ username: data.username }));
    setUser({ username: data.username });
  };
  const logout = () => {
    localStorage.removeItem("tf_token");
    localStorage.removeItem("tf_user");
    setUser(null);
  };
  return <AuthCtx.Provider value={{ user, login, logout }}>{children}</AuthCtx.Provider>;
}

// ─── DESIGN TOKENS ──────────────────────────────────────────────────────────
const STATUS_META = {
  TODO:        { label: "To Do",       color: "#6B7280", bg: "rgba(107,114,128,0.15)" },
  IN_PROGRESS: { label: "In Progress", color: "#3B82F6", bg: "rgba(59,130,246,0.15)"  },
  COMPLETED:   { label: "Completed",   color: "#10B981", bg: "rgba(16,185,129,0.15)"  },
  CANCELLED:   { label: "Cancelled",   color: "#EF4444", bg: "rgba(239,68,68,0.15)"   },
  FAILED:      { label: "Failed",      color: "#F59E0B", bg: "rgba(245,158,11,0.15)"  },
};
const PRIORITY_META = {
  LOW:      { color: "#6B7280", label: "Low"      },
  MEDIUM:   { color: "#3B82F6", label: "Medium"   },
  HIGH:     { color: "#F59E0B", label: "High"     },
  CRITICAL: { color: "#EF4444", label: "Critical" },
};
const TYPE_ICONS = { BUG_FIX: "🐛", FEATURE: "✨", MAINTENANCE: "🔧", INVESTIGATION: "🔍" };

// ─── SMALL COMPONENTS ────────────────────────────────────────────────────────
const Badge = ({ status, priority }) => {
  const meta = status ? STATUS_META[status] : PRIORITY_META[priority];
  if (!meta) return null;
  return (
    <span style={{ background: meta.bg || "rgba(255,255,255,0.1)", color: meta.color,
      padding: "2px 10px", borderRadius: 20, fontSize: 11, fontWeight: 600,
      border: `1px solid ${meta.color}40`, letterSpacing: "0.05em" }}>
      {status ? meta.label : meta.label}
    </span>
  );
};

const Spinner = () => (
  <div style={{ display:"flex", justifyContent:"center", padding:40 }}>
    <div style={{ width:32, height:32, border:"3px solid rgba(255,255,255,0.1)",
      borderTop:"3px solid #3B82F6", borderRadius:"50%", animation:"spin 0.8s linear infinite" }} />
  </div>
);

// ─── AUTH PAGE ───────────────────────────────────────────────────────────────
function AuthPage() {
  const { login } = useAuth();
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ username:"", email:"", password:"" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handle = useCallback(async (e) => {
    e.preventDefault();
    setError(""); setLoading(true);
    try {
      const data = mode === "login"
        ? await api.auth.login({ username: form.username, password: form.password })
        : await api.auth.register(form);
      login(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [form, mode, login]);

  return (
    <div style={{ minHeight:"100vh", background:"#080C14", display:"flex", alignItems:"center",
      justifyContent:"center", fontFamily:"'DM Sans', sans-serif", position:"relative", overflow:"hidden" }}>
      {/* Background mesh */}
      <div style={{ position:"absolute", inset:0, backgroundImage:
        "radial-gradient(ellipse 80% 50% at 20% 20%, rgba(59,130,246,0.12) 0%, transparent 60%)," +
        "radial-gradient(ellipse 60% 40% at 80% 80%, rgba(139,92,246,0.08) 0%, transparent 60%)" }} />

      <div style={{ position:"relative", width:"100%", maxWidth:420, padding:"0 24px" }}>
        {/* Logo */}
        <div style={{ textAlign:"center", marginBottom:40 }}>
          <div style={{ display:"inline-flex", alignItems:"center", gap:10, marginBottom:8 }}>
            <div style={{ width:36, height:36, background:"linear-gradient(135deg,#3B82F6,#8B5CF6)",
              borderRadius:10, display:"flex", alignItems:"center", justifyContent:"center",
              fontSize:18, boxShadow:"0 0 20px rgba(59,130,246,0.4)" }}>⚡</div>
            <span style={{ fontSize:24, fontWeight:700, color:"#fff", letterSpacing:"-0.03em" }}>TaskFlow</span>
          </div>
          <p style={{ color:"#6B7280", fontSize:14, margin:0 }}>
            {mode === "login" ? "Sign in to your workspace" : "Create your account"}
          </p>
        </div>

        <div style={{ background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.08)",
          borderRadius:16, padding:32, backdropFilter:"blur(20px)" }}>

          {error && (
            <div style={{ background:"rgba(239,68,68,0.1)", border:"1px solid rgba(239,68,68,0.3)",
              borderRadius:8, padding:"10px 14px", color:"#FCA5A5", fontSize:13, marginBottom:20 }}>
              {error}
            </div>
          )}

          <form onSubmit={handle} style={{ display:"flex", flexDirection:"column", gap:16 }}>
            <Input label="Username" value={form.username} onChange={v => setForm({...form, username:v})} placeholder="johndoe" />
            {mode === "register" && (
              <Input label="Email" type="email" value={form.email} onChange={v => setForm({...form, email:v})} placeholder="john@acme.com" />
            )}
            <Input label="Password" type="password" value={form.password} onChange={v => setForm({...form, password:v})} placeholder="••••••••" />

            <button type="submit" disabled={loading} style={{
              marginTop:8, padding:"12px 0", borderRadius:10, fontWeight:600, fontSize:15,
              background:"linear-gradient(135deg,#3B82F6,#2563EB)", color:"#fff", border:"none",
              cursor: loading ? "not-allowed" : "pointer", opacity: loading ? 0.7 : 1,
              boxShadow:"0 4px 15px rgba(59,130,246,0.3)", transition:"all 0.2s",
              letterSpacing:"0.02em" }}>
              {loading ? "Please wait…" : mode === "login" ? "Sign In" : "Create Account"}
            </button>
          </form>

          <p style={{ textAlign:"center", color:"#6B7280", fontSize:13, marginTop:20, marginBottom:0 }}>
            {mode === "login" ? "New to TaskFlow? " : "Already have an account? "}
            <span onClick={() => { setMode(mode === "login" ? "register" : "login"); setError(""); }}
              style={{ color:"#3B82F6", cursor:"pointer", fontWeight:600 }}>
              {mode === "login" ? "Create account" : "Sign in"}
            </span>
          </p>
        </div>
      </div>
    </div>
  );
}

function Input({ label, value, onChange, type="text", placeholder }) {
  return (
    <div>
      <label style={{ display:"block", color:"#9CA3AF", fontSize:12, fontWeight:600,
        letterSpacing:"0.06em", marginBottom:6, textTransform:"uppercase" }}>{label}</label>
      <input type={type} value={value} placeholder={placeholder}
        onChange={e => onChange(e.target.value)}
        style={{ width:"100%", boxSizing:"border-box", background:"rgba(255,255,255,0.05)",
          border:"1px solid rgba(255,255,255,0.1)", borderRadius:8, padding:"10px 14px",
          color:"#fff", fontSize:14, outline:"none", transition:"border-color 0.2s",
          fontFamily:"inherit" }}
        onFocus={e => e.target.style.borderColor = "#3B82F6"}
        onBlur={e => e.target.style.borderColor = "rgba(255,255,255,0.1)"}
      />
    </div>
  );
}

// ─── MAIN APP SHELL ───────────────────────────────────────────────────────────
function AppShell() {
  const { user, logout } = useAuth();
  const [page, setPage] = useState("dashboard");
  const [tasks, setTasks] = useState([]);
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);

  const loadTasks = useCallback(async () => {
    try {
      const data = await api.tasks.list();
      setTasks(data.content || []);
    } catch {}
  }, []);

  const loadDashboard = useCallback(async () => {
    try { setDashboard(await api.dashboard.get()); } catch {}
  }, []);

  useEffect(() => {
    (async () => {
      setLoading(true);
      await Promise.all([loadTasks(), loadDashboard()]);
      setLoading(false);
    })();
    const interval = setInterval(() => { loadDashboard(); loadTasks(); }, 10000);
    return () => clearInterval(interval);
  }, [loadTasks, loadDashboard]);

  const nav = [
    { id:"dashboard", icon:"▦", label:"Dashboard"  },
    { id:"kanban",    icon:"⊞", label:"Task Board"  },
    { id:"analytics", icon:"◈", label:"Analytics"   },
  ];

  return (
    <div style={{ minHeight:"100vh", background:"#080C14", display:"flex",
      fontFamily:"'DM Sans', sans-serif", color:"#fff" }}>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap');
        * { box-sizing: border-box; }
        ::-webkit-scrollbar { width: 4px; } ::-webkit-scrollbar-track { background: transparent; }
        ::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 4px; }
        @keyframes spin { to { transform: rotate(360deg); } }
        @keyframes fadeIn { from { opacity:0; transform:translateY(8px); } to { opacity:1; transform:translateY(0); } }
        @keyframes pulse { 0%,100% { opacity:1; } 50% { opacity:0.5; } }
      `}</style>

      {/* Sidebar */}
      <div style={{ width:220, background:"rgba(255,255,255,0.025)", borderRight:"1px solid rgba(255,255,255,0.06)",
        display:"flex", flexDirection:"column", padding:"24px 16px", flexShrink:0 }}>
        <div style={{ display:"flex", alignItems:"center", gap:10, marginBottom:32, paddingLeft:4 }}>
          <div style={{ width:30, height:30, background:"linear-gradient(135deg,#3B82F6,#8B5CF6)",
            borderRadius:8, display:"flex", alignItems:"center", justifyContent:"center", fontSize:15 }}>⚡</div>
          <span style={{ fontWeight:700, fontSize:17, letterSpacing:"-0.03em" }}>TaskFlow</span>
        </div>

        {nav.map(n => (
          <button key={n.id} onClick={() => setPage(n.id)} style={{
            display:"flex", alignItems:"center", gap:10, padding:"9px 12px",
            borderRadius:8, border:"none", cursor:"pointer", marginBottom:4,
            background: page === n.id ? "rgba(59,130,246,0.15)" : "transparent",
            color: page === n.id ? "#3B82F6" : "#6B7280",
            borderLeft: page === n.id ? "2px solid #3B82F6" : "2px solid transparent",
            fontFamily:"inherit", fontSize:14, fontWeight: page === n.id ? 600 : 500,
            transition:"all 0.15s", textAlign:"left" }}>
            <span style={{ fontSize:16 }}>{n.icon}</span> {n.label}
          </button>
        ))}

        <div style={{ marginTop:"auto" }}>
          <div style={{ borderTop:"1px solid rgba(255,255,255,0.06)", paddingTop:16 }}>
            <div style={{ display:"flex", alignItems:"center", gap:10, marginBottom:12, paddingLeft:4 }}>
              <div style={{ width:28, height:28, background:"linear-gradient(135deg,#3B82F6,#8B5CF6)",
                borderRadius:"50%", display:"flex", alignItems:"center", justifyContent:"center",
                fontSize:12, fontWeight:700 }}>
                {user?.username?.[0]?.toUpperCase()}
              </div>
              <span style={{ fontSize:13, color:"#9CA3AF", fontWeight:500, overflow:"hidden",
                textOverflow:"ellipsis", whiteSpace:"nowrap" }}>{user?.username}</span>
            </div>
            <button onClick={logout} style={{ width:"100%", padding:"8px 12px", borderRadius:8,
              background:"rgba(239,68,68,0.1)", border:"1px solid rgba(239,68,68,0.2)",
              color:"#FCA5A5", fontSize:13, cursor:"pointer", fontFamily:"inherit", fontWeight:500 }}>
              Sign out
            </button>
          </div>
        </div>
      </div>

      {/* Main content */}
      <div style={{ flex:1, overflow:"auto", padding:32 }}>
        <div style={{ maxWidth:1200, margin:"0 auto", animation:"fadeIn 0.3s ease" }}>
          {/* Header */}
          <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:32 }}>
            <div>
              <h1 style={{ margin:0, fontSize:24, fontWeight:700, letterSpacing:"-0.03em" }}>
                {nav.find(n=>n.id===page)?.label}
              </h1>
              <p style={{ margin:"4px 0 0", color:"#6B7280", fontSize:13 }}>
                {new Date().toLocaleDateString("en-US", { weekday:"long", year:"numeric", month:"long", day:"numeric" })}
              </p>
            </div>
            {(page === "kanban" || page === "dashboard") && (
              <button onClick={() => setShowCreate(true)} style={{
                padding:"10px 20px", borderRadius:10, background:"linear-gradient(135deg,#3B82F6,#2563EB)",
                border:"none", color:"#fff", fontSize:14, fontWeight:600, cursor:"pointer",
                boxShadow:"0 4px 15px rgba(59,130,246,0.3)", fontFamily:"inherit",
                display:"flex", alignItems:"center", gap:8 }}>
                + New Task
              </button>
            )}
          </div>

          {loading ? <Spinner /> : (
            <>
              {page === "dashboard" && <DashboardPage dashboard={dashboard} tasks={tasks} />}
              {page === "kanban"    && <KanbanPage tasks={tasks} onRefresh={loadTasks} />}
              {page === "analytics" && <AnalyticsPage dashboard={dashboard} />}
            </>
          )}
        </div>
      </div>

      {showCreate && <CreateTaskModal onClose={() => setShowCreate(false)} onCreated={() => { loadTasks(); loadDashboard(); }} />}
    </div>
  );
}

// ─── DASHBOARD PAGE ───────────────────────────────────────────────────────────
function DashboardPage({ dashboard, tasks }) {
  if (!dashboard) return <Spinner />;
  const stats = [
    { label:"Total Tasks",      value: dashboard.totalTasks,     icon:"📋", color:"#3B82F6" },
    { label:"Resources",        value: dashboard.totalResources, icon:"👥", color:"#8B5CF6" },
    { label:"In Progress",      value: dashboard.tasksByStatus?.IN_PROGRESS || 0, icon:"⚡", color:"#F59E0B" },
    { label:"Completed",        value: dashboard.tasksByStatus?.COMPLETED   || 0, icon:"✅", color:"#10B981" },
  ];

  return (
    <div>
      <div style={{ display:"grid", gridTemplateColumns:"repeat(4,1fr)", gap:16, marginBottom:32 }}>
        {stats.map(s => (
          <div key={s.label} style={{ background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.07)",
            borderRadius:12, padding:20 }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start" }}>
              <div>
                <p style={{ margin:"0 0 8px", color:"#6B7280", fontSize:12, fontWeight:600,
                  textTransform:"uppercase", letterSpacing:"0.06em" }}>{s.label}</p>
                <p style={{ margin:0, fontSize:32, fontWeight:700, color:s.color,
                  letterSpacing:"-0.03em" }}>{s.value}</p>
              </div>
              <span style={{ fontSize:24 }}>{s.icon}</span>
            </div>
          </div>
        ))}
      </div>

      {/* Engine metrics */}
      {dashboard.engineMetrics && (
        <div style={{ background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.07)",
          borderRadius:12, padding:20, marginBottom:24 }}>
          <h3 style={{ margin:"0 0 16px", fontSize:14, fontWeight:600, color:"#9CA3AF",
            textTransform:"uppercase", letterSpacing:"0.06em" }}>Assignment Engine</h3>
          <div style={{ display:"flex", gap:24 }}>
            {Object.entries(dashboard.engineMetrics).map(([k,v]) => (
              <div key={k}>
                <p style={{ margin:"0 0 4px", color:"#6B7280", fontSize:11, textTransform:"uppercase",
                  letterSpacing:"0.05em" }}>{k.replace(/([A-Z])/g," $1").trim()}</p>
                <p style={{ margin:0, fontWeight:600, fontSize:18, color:"#fff" }}>{String(v)}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recent tasks */}
      <div style={{ background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.07)",
        borderRadius:12, padding:20 }}>
        <h3 style={{ margin:"0 0 16px", fontSize:14, fontWeight:600, color:"#9CA3AF",
          textTransform:"uppercase", letterSpacing:"0.06em" }}>Recent Tasks</h3>
        {tasks.slice(0,5).map(t => (
          <div key={t.id} style={{ display:"flex", alignItems:"center", justifyContent:"space-between",
            padding:"12px 0", borderBottom:"1px solid rgba(255,255,255,0.04)" }}>
            <div style={{ display:"flex", alignItems:"center", gap:12 }}>
              <span style={{ fontSize:18 }}>{TYPE_ICONS[t.type]}</span>
              <div>
                <p style={{ margin:0, fontWeight:500, fontSize:14 }}>{t.title}</p>
                <p style={{ margin:"2px 0 0", fontSize:12, color:"#6B7280" }}>
                  {t.resourceName ? `→ ${t.resourceName}` : "Unassigned"}
                </p>
              </div>
            </div>
            <div style={{ display:"flex", gap:8 }}>
              <Badge status={t.status} />
              <Badge priority={t.priority} />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── KANBAN PAGE ──────────────────────────────────────────────────────────────
function KanbanPage({ tasks, onRefresh }) {
  const columns = ["TODO", "IN_PROGRESS", "COMPLETED", "CANCELLED"];

  const updateTask = async (id, status) => {
    try { await api.tasks.update(id, { status }); onRefresh(); } catch {}
  };

  return (
    <div style={{ display:"grid", gridTemplateColumns:"repeat(4,1fr)", gap:16, alignItems:"start" }}>
      {columns.map(col => {
        const colTasks = tasks.filter(t => t.status === col);
        const meta = STATUS_META[col];
        return (
          <div key={col} style={{ background:"rgba(255,255,255,0.03)",
            border:"1px solid rgba(255,255,255,0.06)", borderRadius:12, overflow:"hidden" }}>
            <div style={{ padding:"14px 16px", borderBottom:"1px solid rgba(255,255,255,0.06)",
              display:"flex", justifyContent:"space-between", alignItems:"center" }}>
              <span style={{ fontSize:12, fontWeight:700, textTransform:"uppercase",
                letterSpacing:"0.08em", color: meta.color }}>{meta.label}</span>
              <span style={{ background: meta.bg, color: meta.color, borderRadius:20,
                padding:"2px 8px", fontSize:11, fontWeight:700 }}>{colTasks.length}</span>
            </div>
            <div style={{ padding:12, display:"flex", flexDirection:"column", gap:8, minHeight:200 }}>
              {colTasks.map(task => (
                <TaskCard key={task.id} task={task} onUpdate={updateTask} onRefresh={onRefresh} />
              ))}
              {colTasks.length === 0 && (
                <div style={{ color:"#374151", fontSize:12, textAlign:"center", marginTop:20 }}>
                  No tasks
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

function TaskCard({ task, onUpdate, onRefresh }) {
  const [expanded, setExpanded] = useState(false);

  const del = async () => {
    if (!window.confirm("Delete this task?")) return;
    try { await api.tasks.del(task.id); onRefresh(); } catch {}
  };

  return (
    <div style={{ background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.07)",
      borderRadius:8, padding:12, cursor:"pointer", transition:"all 0.15s",
      animation:"fadeIn 0.3s ease" }}
      onMouseEnter={e => e.currentTarget.style.borderColor = "rgba(59,130,246,0.4)"}
      onMouseLeave={e => e.currentTarget.style.borderColor = "rgba(255,255,255,0.07)"}
      onClick={() => setExpanded(!expanded)}>
      <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start" }}>
        <span style={{ fontSize:16 }}>{TYPE_ICONS[task.type]}</span>
        <Badge priority={task.priority} />
      </div>
      <p style={{ margin:"8px 0 4px", fontWeight:500, fontSize:13, lineHeight:1.4 }}>{task.title}</p>
      {task.resourceName && (
        <p style={{ margin:"0 0 4px", fontSize:11, color:"#3B82F6" }}>⚙ {task.resourceName}</p>
      )}
      {expanded && (
        <div style={{ marginTop:8, borderTop:"1px solid rgba(255,255,255,0.06)", paddingTop:8 }}>
          {task.description && <p style={{ margin:"0 0 8px", fontSize:12, color:"#9CA3AF" }}>{task.description}</p>}
          <div style={{ display:"flex", gap:6, flexWrap:"wrap" }}>
            {task.status !== "COMPLETED" && (
              <button onClick={(e)=>{e.stopPropagation();onUpdate(task.id,"COMPLETED");}}
                style={btnStyle("#10B981")}>✓ Complete</button>
            )}
            {task.status !== "CANCELLED" && task.status !== "COMPLETED" && (
              <button onClick={(e)=>{e.stopPropagation();onUpdate(task.id,"CANCELLED");}}
                style={btnStyle("#6B7280")}>✕ Cancel</button>
            )}
            <button onClick={(e)=>{e.stopPropagation();del();}}
              style={btnStyle("#EF4444")}>Delete</button>
          </div>
        </div>
      )}
    </div>
  );
}

const btnStyle = (color) => ({
  padding:"4px 10px", borderRadius:6, border:`1px solid ${color}40`,
  background:`${color}15`, color, fontSize:11, cursor:"pointer",
  fontFamily:"inherit", fontWeight:600
});

// ─── ANALYTICS PAGE ───────────────────────────────────────────────────────────
function AnalyticsPage({ dashboard }) {
  if (!dashboard) return <Spinner />;

  const BarChart = ({ data, title, colorFn }) => {
    const max = Math.max(...Object.values(data || {}), 1);
    return (
      <div style={{ background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.07)",
        borderRadius:12, padding:20 }}>
        <h3 style={{ margin:"0 0 20px", fontSize:13, fontWeight:600, color:"#9CA3AF",
          textTransform:"uppercase", letterSpacing:"0.06em" }}>{title}</h3>
        <div style={{ display:"flex", flexDirection:"column", gap:10 }}>
          {Object.entries(data || {}).map(([k, v]) => (
            <div key={k}>
              <div style={{ display:"flex", justifyContent:"space-between", marginBottom:4 }}>
                <span style={{ fontSize:12, color:"#9CA3AF", fontWeight:500 }}>{k}</span>
                <span style={{ fontSize:12, fontWeight:700, color:"#fff" }}>{v}</span>
              </div>
              <div style={{ background:"rgba(255,255,255,0.06)", borderRadius:4, height:6, overflow:"hidden" }}>
                <div style={{ height:"100%", borderRadius:4,
                  background: colorFn ? colorFn(k) : "linear-gradient(90deg,#3B82F6,#8B5CF6)",
                  width:`${(v/max)*100}%`, transition:"width 0.8s ease" }} />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  };

  const statusColor = (k) => STATUS_META[k]?.color || "#3B82F6";
  const priorityColor = (k) => PRIORITY_META[k]?.color || "#3B82F6";

  return (
    <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:20 }}>
      <BarChart data={dashboard.tasksByStatus}   title="Tasks by Status"   colorFn={statusColor}   />
      <BarChart data={dashboard.tasksByPriority} title="Tasks by Priority" colorFn={priorityColor} />
      <BarChart data={dashboard.tasksByType}     title="Tasks by Type"     />
    </div>
  );
}

// ─── CREATE TASK MODAL ────────────────────────────────────────────────────────
function CreateTaskModal({ onClose, onCreated }) {
  const [form, setForm] = useState({ title:"", description:"", type:"FEATURE", priority:"MEDIUM", deadline:"", estimatedHours:"" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setLoading(true);
    try {
      await api.tasks.create({
        ...form,
        deadline: form.deadline || null,
        estimatedHours: form.estimatedHours ? parseFloat(form.estimatedHours) : null,
      });
      onCreated(); onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const sel = (label, key, opts) => (
    <div>
      <label style={{ display:"block", color:"#9CA3AF", fontSize:11, fontWeight:600,
        letterSpacing:"0.06em", marginBottom:6, textTransform:"uppercase" }}>{label}</label>
      <select value={form[key]} onChange={e => setForm({...form, [key]:e.target.value})}
        style={{ width:"100%", background:"rgba(255,255,255,0.05)", border:"1px solid rgba(255,255,255,0.1)",
          borderRadius:8, padding:"10px 12px", color:"#fff", fontSize:14,
          fontFamily:"inherit", outline:"none" }}>
        {opts.map(o => <option key={o} value={o} style={{ background:"#1F2937" }}>{o}</option>)}
      </select>
    </div>
  );

  return (
    <div style={{ position:"fixed", inset:0, background:"rgba(0,0,0,0.7)", display:"flex",
      alignItems:"center", justifyContent:"center", zIndex:1000, padding:24,
      backdropFilter:"blur(4px)" }} onClick={onClose}>
      <div style={{ background:"#0F1623", border:"1px solid rgba(255,255,255,0.1)",
        borderRadius:16, padding:32, width:"100%", maxWidth:480,
        animation:"fadeIn 0.2s ease" }} onClick={e => e.stopPropagation()}>
        <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:24 }}>
          <h2 style={{ margin:0, fontSize:18, fontWeight:700 }}>Create New Task</h2>
          <button onClick={onClose} style={{ background:"none", border:"none", color:"#6B7280",
            fontSize:20, cursor:"pointer" }}>✕</button>
        </div>

        {error && (
          <div style={{ background:"rgba(239,68,68,0.1)", border:"1px solid rgba(239,68,68,0.3)",
            borderRadius:8, padding:"10px 14px", color:"#FCA5A5", fontSize:13, marginBottom:16 }}>
            {error}
          </div>
        )}

        <form onSubmit={submit} style={{ display:"flex", flexDirection:"column", gap:16 }}>
          <Input label="Title" value={form.title} onChange={v => setForm({...form, title:v})} placeholder="Task title" />
          <div>
            <label style={{ display:"block", color:"#9CA3AF", fontSize:11, fontWeight:600,
              letterSpacing:"0.06em", marginBottom:6, textTransform:"uppercase" }}>Description</label>
            <textarea value={form.description} onChange={e => setForm({...form, description:e.target.value})}
              placeholder="Optional description…" rows={3}
              style={{ width:"100%", background:"rgba(255,255,255,0.05)", border:"1px solid rgba(255,255,255,0.1)",
                borderRadius:8, padding:"10px 12px", color:"#fff", fontSize:14, fontFamily:"inherit",
                outline:"none", resize:"vertical", boxSizing:"border-box" }} />
          </div>
          <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:12 }}>
            {sel("Type",     "type",     ["FEATURE","BUG_FIX","MAINTENANCE","INVESTIGATION"])}
            {sel("Priority", "priority", ["LOW","MEDIUM","HIGH","CRITICAL"])}
          </div>
          <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:12 }}>
            <Input label="Deadline" type="datetime-local" value={form.deadline}
              onChange={v => setForm({...form, deadline:v})} />
            <Input label="Est. Hours" type="number" value={form.estimatedHours}
              onChange={v => setForm({...form, estimatedHours:v})} placeholder="e.g. 4" />
          </div>

          <button type="submit" disabled={loading} style={{
            marginTop:8, padding:"12px 0", borderRadius:10, fontWeight:600, fontSize:15,
            background:"linear-gradient(135deg,#3B82F6,#2563EB)", color:"#fff", border:"none",
            cursor: loading ? "not-allowed" : "pointer", opacity: loading ? 0.7 : 1,
            fontFamily:"inherit" }}>
            {loading ? "Creating…" : "Create Task"}
          </button>
        </form>
      </div>
    </div>
  );
}

// ─── ROOT ─────────────────────────────────────────────────────────────────────
export default function App() {
  const { user } = useAuth();
  return user ? <AppShell /> : <AuthPage />;
}

export function Root() {
  return (
    <AuthProvider>
      <App />
    </AuthProvider>
  );
}
