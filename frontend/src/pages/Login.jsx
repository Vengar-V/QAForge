import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'

export default function Login() {
  const navigate = useNavigate()
  const [tab, setTab] = useState('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const endpoint = tab === 'login' ? '/api/auth/login' : '/api/auth/register'
      const body = tab === 'login'
        ? { email, password }
        : { email, password, fullName }

      const res = await axios.post(`http://localhost:8080${endpoint}`, body)

      // Зберігаємо токен
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('email', res.data.email)
      localStorage.setItem('fullName', res.data.fullName || '')

      // Переходимо на дашборд
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.message || 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.card}>
        {/* LOGO */}
        <div style={styles.logo}>
          <div style={styles.logoIcon}>Q</div>
          <span style={styles.logoText}>QAForge</span>
          <span style={styles.logoBadge}>v0.1-alpha</span>
        </div>

        <div style={styles.terminalLine}>
          <span style={styles.prompt}>$</span>
          <span> init auth module... ready.</span>
        </div>

        {/* TABS */}
        <div style={styles.tabs}>
          <button
            style={{ ...styles.tab, ...(tab === 'login' ? styles.tabActive : {}) }}
            onClick={() => { setTab('login'); setError('') }}
          >
            // login
          </button>
          <button
            style={{ ...styles.tab, ...(tab === 'register' ? styles.tabActive : {}) }}
            onClick={() => { setTab('register'); setError('') }}
          >
            // register
          </button>
        </div>

        {/* FORM */}
        <form onSubmit={handleSubmit}>
          <div style={styles.field}>
            <label style={styles.label}><span style={styles.arrow}>→</span> email</label>
            <input
              style={styles.input}
              type="email"
              placeholder="dev@company.com"
              value={email}
              onChange={e => setEmail(e.target.value)}
              required
            />
          </div>

          {tab === 'register' && (
            <div style={styles.field}>
              <label style={styles.label}><span style={styles.arrow}>→</span> full_name</label>
              <input
                style={styles.input}
                type="text"
                placeholder="John Doe"
                value={fullName}
                onChange={e => setFullName(e.target.value)}
              />
            </div>
          )}

          <div style={styles.field}>
            <label style={styles.label}><span style={styles.arrow}>→</span> password</label>
            <input
              style={styles.input}
              type="password"
              placeholder="••••••••••••"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              minLength={8}
            />
          </div>

          {error && <div style={styles.error}>{error}</div>}

          <button style={styles.submitBtn} type="submit" disabled={loading}>
            {loading ? 'Loading...' : tab === 'login' ? 'AUTHENTICATE' : 'CREATE ACCOUNT'}
          </button>
        </form>

        <div style={styles.footerLink}>
          {tab === 'login' ? (
            <span>no account? <span style={styles.link} onClick={() => setTab('register')}>register →</span></span>
          ) : (
            <span>have account? <span style={styles.link} onClick={() => setTab('login')}>login →</span></span>
          )}
        </div>
      </div>
    </div>
  )
}

const styles = {
  page: {
    minHeight: '100vh',
    background: '#080c10',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontFamily: "'JetBrains Mono', monospace",
    backgroundImage: 'linear-gradient(#1e2d3d 1px, transparent 1px), linear-gradient(90deg, #1e2d3d 1px, transparent 1px)',
    backgroundSize: '48px 48px',
  },
  card: {
    width: 440,
    background: '#0d1117',
    border: '1px solid #2a3f52',
    borderRadius: 12,
    padding: 40,
    position: 'relative',
  },
  logo: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    marginBottom: 24,
  },
  logoIcon: {
    width: 36, height: 36,
    borderRadius: 8,
    border: '1px solid #00d4aa',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    color: '#00d4aa', fontWeight: 700, fontSize: 16,
  },
  logoText: {
    fontWeight: 800, fontSize: 18,
    background: 'linear-gradient(90deg, #00d4aa, #0ea5e9)',
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent',
  },
  logoBadge: {
    marginLeft: 'auto',
    fontSize: 10, color: '#3d4f60',
    border: '1px solid #2a3f52',
    borderRadius: 4, padding: '2px 7px',
  },
  terminalLine: {
    fontSize: 11, color: '#3d4f60',
    marginBottom: 20,
  },
  prompt: { color: '#00d4aa' },
  tabs: {
    display: 'flex',
    border: '1px solid #2a3f52',
    borderRadius: 8,
    padding: 3,
    marginBottom: 24,
  },
  tab: {
    flex: 1, padding: '8px',
    background: 'transparent',
    border: 'none',
    color: '#8b949e',
    cursor: 'pointer',
    borderRadius: 5,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: 12,
    letterSpacing: '.5px',
  },
  tabActive: {
    background: '#131920',
    color: '#00d4aa',
    boxShadow: '0 0 0 1px #2a3f52',
  },
  field: { marginBottom: 16 },
  label: {
    display: 'block',
    fontSize: 11, color: '#8b949e',
    marginBottom: 6, letterSpacing: '.8px',
  },
  arrow: { color: '#00d4aa', marginRight: 4 },
  input: {
    width: '100%',
    background: '#131920',
    border: '1px solid #2a3f52',
    borderRadius: 7,
    padding: '10px 14px',
    fontSize: 13,
    fontFamily: "'JetBrains Mono', monospace",
    color: '#e6edf3',
    outline: 'none',
    boxSizing: 'border-box',
  },
  error: {
    color: '#f85149',
    fontSize: 12,
    marginBottom: 12,
    padding: '8px 12px',
    background: '#f8514915',
    borderRadius: 6,
    border: '1px solid #f8514930',
  },
  submitBtn: {
    width: '100%',
    padding: 12,
    background: 'linear-gradient(90deg, #00d4aa, #0ea5e9)',
    border: 'none',
    borderRadius: 7,
    color: '#080c10',
    fontSize: 13,
    fontWeight: 700,
    fontFamily: "'JetBrains Mono', monospace",
    cursor: 'pointer',
    letterSpacing: '.8px',
    marginTop: 8,
  },
  footerLink: {
    textAlign: 'center',
    marginTop: 20,
    fontSize: 11,
    color: '#3d4f60',
  },
  link: {
    color: '#00d4aa',
    cursor: 'pointer',
  },
}
