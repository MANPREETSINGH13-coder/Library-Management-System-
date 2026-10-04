import React, { useEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { ArrowLeftRight, BookCopy, BookOpen, BookOpenCheck, Camera, ChartNoAxesCombined, CircleCheck, Clock3, GraduationCap, LayoutDashboard, LibraryBig, LogOut, ReceiptIndianRupee, ScanLine, Search, Settings, ShieldCheck, Upload, UserRound, UserRoundCheck, UsersRound } from 'lucide-react';
import './styles.css';

const demoBooks = [
  { id: 101, title: 'The Discovery of India', author: 'Jawaharlal Nehru', category: 'History', copies: 8, available: 5 },
  { id: 102, title: 'Wings of Fire', author: 'A. P. J. Abdul Kalam', category: 'Biography', copies: 12, available: 3 },
  { id: 103, title: 'The God of Small Things', author: 'Arundhati Roy', category: 'Fiction', copies: 6, available: 0 },
  { id: 104, title: 'Introduction to Algorithms', author: 'Thomas H. Cormen', category: 'Computer science', copies: 5, available: 2 },
  { id: 105, title: 'The White Tiger', author: 'Aravind Adiga', category: 'Fiction', copies: 7, available: 1 },
];
const navigation = {
  Student: [['My dashboard', LayoutDashboard], ['Browse books', Search], ['Issued books', BookOpen], ['OCR scanner', ScanLine], ['My fines', ReceiptIndianRupee], ['My account', UserRound]],
  Administration: [['Overview', LayoutDashboard], ['Book catalogue', LibraryBig], ['Issue & return', ArrowLeftRight], ['OCR scanner', ScanLine], ['Members', UsersRound], ['Student approvals', UserRoundCheck], ['Fines & fees', ReceiptIndianRupee], ['Reports', ChartNoAxesCombined]],
  'Super Admin': [['Overview', LayoutDashboard], ['Administration', ShieldCheck], ['System settings', Settings]],
};
const today = () => new Date().toISOString().slice(0, 10);
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

function App() {
  const [role, setRole] = useState('');
  const [name, setName] = useState('');
  const [studentId, setStudentId] = useState('');
  const [token, setToken] = useState('');
  const [page, setPage] = useState('Overview');
  const [mode, setMode] = useState('home');
  const [form, setForm] = useState({ name: '', email: '', id: '', password: '', title: '', author: '', category: 'Fiction', access: 'Student' });
  const [books, setBooks] = useState(demoBooks);
  const [students, setStudents] = useState([]);
  const [admins, setAdmins] = useState([]);
  const [loans, setLoans] = useState([]);
  const [fines, setFines] = useState([]);
  const [query, setQuery] = useState('');
  const [modal, setModal] = useState('');
  const [toast, setToast] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [cameraStream, setCameraStream] = useState(null);
  const [scanBusy, setScanBusy] = useState(false);
  const [scanProgress, setScanProgress] = useState(0);
  const [scanStatus, setScanStatus] = useState('');
  const [ocrText, setOcrText] = useState('');
  const [scanBookId, setScanBookId] = useState(null);
  const [profileImage, setProfileImage] = useState(() => { try { return localStorage.getItem('bbau-profile-image') || ''; } catch { return ''; } });
  const [accountEmail, setAccountEmail] = useState('');
  const [profileForm, setProfileForm] = useState({ name: '', currentPassword: '', newPassword: '' });
  const videoRef = useRef(null);
  const fileRef = useRef(null);
  const profileFileRef = useRef(null);
  const student = role === 'Student';
  const admin = role === 'Administration';
  const root = role === 'Super Admin';
  const set = (key, value) => setForm(current => ({ ...current, [key]: value }));
  const say = message => { setToast(message); window.setTimeout(() => setToast(''), 3000); };

  async function request(path, method = 'GET', body = null) {
    let response;
    try {
      response = await fetch(`${API_BASE_URL}/api/${path}`, {
        method,
        headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        ...(body ? { body: JSON.stringify(body) } : {}),
      });
    } catch {
      throw new Error('Java library API connect nahi hua. Pehle project ka Java backend start karein.');
    }
    const data = response.status === 204 ? {} : await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.message || data.error || data.detail || data.title || `Request fail hui (HTTP ${response.status}).`);
    return data;
  }

  async function signIn(event) {
    event.preventDefault(); setError(''); setBusy(true);
    try {
      const session = await request('auth/login', 'POST', { email: form.email, password: form.password, role: form.access });
      setToken(session.token); setRole(session.role); setName(session.name); setStudentId(session.studentId || ''); setAccountEmail(form.email.trim().toLowerCase());
      try { setProfileImage(localStorage.getItem(`bbau-profile-image:${form.email.trim().toLowerCase()}`) || ''); } catch { setProfileImage(''); }
      setPage(session.role === 'Student' ? 'My dashboard' : 'Overview');
      await loadDashboard(session.token, session.role, session.studentId || '');
    } catch (e) { setError(e.message); }
    finally { setBusy(false); }
  }

  async function loadDashboard(authToken = token, currentRole = role, ownStudentId = studentId) {
    const call = async (path, method = 'GET', body = null) => {
      const response = await fetch(`${API_BASE_URL}/api/${path}`, { method, headers: { 'Content-Type': 'application/json', ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}) }, ...(body ? { body: JSON.stringify(body) } : {}) });
      const data = response.status === 204 ? {} : await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || data.error || data.detail || data.title || `Dashboard request fail hui (HTTP ${response.status}).`);
      return data;
    };
    try {
      const [rawBooks, rawStudents, rawAdmins, rawLoans, rawFines] = await Promise.all([
        call('books'),
        currentRole === 'Administration' ? call('admin/students') : Promise.resolve([]),
        currentRole === 'Super Admin' ? call('super-admin/administrators') : Promise.resolve([]),
        currentRole === 'Administration' ? call('admin/loans') : currentRole === 'Student' ? call(`student/loans?studentId=${encodeURIComponent(ownStudentId)}`) : Promise.resolve([]),
        currentRole === 'Administration' ? call('admin/fines') : currentRole === 'Student' ? call(`student/fines?studentId=${encodeURIComponent(ownStudentId)}`) : Promise.resolve([]),
      ]);
      setBooks(rawBooks);
      setStudents(rawStudents.map(s => ({ ...s, status: s.status[0] + s.status.slice(1).toLowerCase() })));
      setAdmins(rawAdmins);
      const people = rawStudents;
      setLoans(rawLoans.filter(l => !l.returned).map(l => ({ ...l, memberName: currentRole === 'Student' ? 'Your account' : people.find(s => s.studentId === l.studentId)?.name || l.studentId, bookTitle: rawBooks.find(b => b.id === l.bookId)?.title || 'Book' })));
      setFines(rawFines.map(f => ({ ...f, memberName: currentRole === 'Student' ? 'Your account' : people.find(s => s.studentId === f.studentId)?.name || f.studentId })));
    } catch (e) { say(e.message); }
  }

  useEffect(() => { if (role && token) loadDashboard(); }, [page]);
  useEffect(() => {
    if (!cameraStream) return undefined;
    if (page !== 'OCR scanner') { cameraStream.getTracks().forEach(track => track.stop()); setCameraStream(null); return undefined; }
    if (!videoRef.current) return undefined;
    videoRef.current.srcObject = cameraStream;
    videoRef.current.play().catch(() => {});
    return () => cameraStream.getTracks().forEach(track => track.stop());
  }, [cameraStream, page]);

  async function registerStudent(event) {
    event.preventDefault(); setError(''); setBusy(true);
    try {
      await request('students/register', 'POST', { name: form.name, studentId: form.id, email: form.email, password: form.password });
      setMode('login'); setForm(current => ({ ...current, access: 'Student', password: '' }));
      setError('Registration submit ho gayi. Administration ki approval ke baad aap sign in kar sakte hain.');
    } catch (e) { setError(e.message); }
    finally { setBusy(false); }
  }

  async function signOut() {
    try { await request('auth/logout', 'POST'); } catch { /* Clear this browser session even if the server is offline. */ }
    setToken(''); setRole(''); setName(''); setStudentId(''); setAccountEmail(''); setProfileImage(''); setPage('Overview'); setMode('home'); setError('');
  }

  async function addBook(event) {
    event.preventDefault();
    try { await request('admin/books', 'POST', { title: form.title, author: form.author, category: form.category, copies: 1 }); setModal(''); setForm(current => ({ ...current, title: '', author: '' })); await loadDashboard(); say('Book catalogue mein add ho gayi.'); }
    catch (e) { say(e.message); }
  }
  async function removeBook(book) {
    if (!window.confirm(`“${book.title}” delete karein?`)) return;
    try { await request(`admin/books?title=${encodeURIComponent(book.title)}`, 'DELETE'); await loadDashboard(); say('Book delete ho gayi.'); }
    catch (e) { say(e.message); }
  }
  async function createAdmin(event) {
    event.preventDefault();
    try { await request('super-admin/administrators', 'POST', { name: form.name, email: form.email, staffRole: form.category, password: form.password }); setModal(''); setForm(current => ({ ...current, name: '', email: '', password: '' })); await loadDashboard(); say('Administration account bana diya.'); }
    catch (e) { say(e.message); }
  }
  async function reviewStudent(s, approve) {
    try { await request(`admin/students/${s.id}/review`, 'POST', { approve }); await loadDashboard(); say(approve ? 'Student registration approve ho gayi.' : 'Student registration reject ho gayi.'); }
    catch (e) { say(e.message); }
  }
  async function issueBook(book) {
    if (!book.available) return say('Is book ki copy abhi available nahi hai.');
    const member = student ? studentId : window.prompt('Student ID');
    if (!member) return;
    try {
      await request(student ? 'student/loans' : 'admin/loans', 'POST', { bookId: book.id, studentId: member, dueDate: new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10) });
      await loadDashboard(); say('Book issue ho gayi.');
    } catch (e) { say(e.message); }
  }
  async function returnBook(loan) {
    try { await request(`${student ? 'student' : 'admin'}/loans/${loan.id}/return`, 'POST'); await loadDashboard(); say(student ? 'Book return submit ho gaya.' : 'Book return record ho gaya.'); }
    catch (e) { say(e.message); }
  }
  async function startCamera() {
    setScanStatus('');
    if (!navigator.mediaDevices?.getUserMedia) return setScanStatus('Camera access ke liye localhost ya HTTPS par page kholen, phir browser mein camera allow karein.');
    try { setCameraStream(await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false })); }
    catch (e) { setScanStatus(e.name === 'NotAllowedError' ? 'Browser camera permission allow karein.' : `Camera start nahi hua: ${e.message}`); }
  }
  function stopCamera() { cameraStream?.getTracks().forEach(track => track.stop()); setCameraStream(null); }
  function matchScannedBook(text) {
    const flattened = text.toLowerCase().replace(/[^a-z0-9]/g, '');
    const idFromLabel = text.match(/\bBK[\s:#-]*(\d{2,})\b/i)?.[1];
    let match = idFromLabel ? books.find(book => String(book.id) === idFromLabel) : null;
    if (!match) match = books.find(book => flattened.includes(book.title.toLowerCase().replace(/[^a-z0-9]/g, '')));
    if (!match) {
      const ranked = books.map(book => {
        const titleWords = book.title.toLowerCase().split(/[^a-z0-9]+/).filter(word => word.length > 3);
        return { book, score: titleWords.filter(word => flattened.includes(word)).length };
      }).sort((a, b) => b.score - a.score);
      if (ranked[0]?.score >= 2 && ranked[0].score > (ranked[1]?.score || 0)) match = ranked[0].book;
    }
    setScanBookId(match?.id ?? null);
  }
  async function recognizeBook(image) {
    setScanBusy(true); setScanProgress(0); setScanStatus('OCR worker load ho raha hai…'); setOcrText(''); setScanBookId(null);
    let worker;
    try {
      const { createWorker } = await import('tesseract.js');
      worker = await createWorker('eng', 1, { logger: message => { if (message.status) setScanStatus(`${message.status}${message.progress ? ` · ${Math.round(message.progress * 100)}%` : ''}`); if (message.progress) setScanProgress(message.progress); } });
      const { data: { text } } = await worker.recognize(image);
      setOcrText(text.trim());
      matchScannedBook(text);
      if (!text.trim()) setScanStatus('Photo mein text nahi mila. Book title ya BK-101 jaisa ID frame mein saaf dikhayein.');
      else setScanStatus('Scan complete. Result verify karke Issue ya Return karein.');
    } catch (e) { setScanStatus(`OCR scan nahi ho saka: ${e.message || 'Internet connection check karein.'}`); }
    finally { if (worker) await worker.terminate(); setScanBusy(false); }
  }
  async function captureCameraFrame() {
    const video = videoRef.current;
    if (!video?.videoWidth) return setScanStatus('Pehle camera start hone dein.');
    const canvas = document.createElement('canvas'); canvas.width = video.videoWidth; canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0);
    const image = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.92));
    if (image) recognizeBook(image);
  }
  function handleImage(file) { if (file) recognizeBook(file); }
  function handleProfileImage(file) {
    if (!file) return;
    if (!file.type.startsWith('image/')) return say('Sirf image file upload karein.');
    if (file.size > 3 * 1024 * 1024) return say('Profile image 3 MB se chhoti honi chahiye.');
    const reader = new FileReader();
    reader.onload = () => { const image = String(reader.result); setProfileImage(image); localStorage.setItem(`bbau-profile-image:${accountEmail}`, image); say('Profile image update ho gayi.'); };
    reader.readAsDataURL(file);
  }
  function openProfile() { setProfileForm({ name, currentPassword: '', newPassword: '' }); setModal('profile'); }
  async function updateProfile(event) {
    event.preventDefault();
    try {
      const profile = await request('auth/profile', 'PUT', profileForm);
      setName(profile.name); setProfileForm(current => ({ ...current, currentPassword: '', newPassword: '' })); setModal(''); say('Profile update ho gayi.');
    } catch (e) { say(e.message); }
  }
  async function addFine() {
    const member = window.prompt('Student ID for fine'); if (!member) return;
    const reason = window.prompt('Fine reason'); if (!reason) return;
    const amount = Number(window.prompt('Fine amount in rupees'));
    if (!Number.isInteger(amount) || amount < 1) return say('Fine amount ₹1 ya usse zyada hona chahiye.');
    try { await request('admin/fines', 'POST', { studentId: member, reason, amount }); await loadDashboard(); say('Fine record ho gaya.'); }
    catch (e) { say(e.message); }
  }
  async function submitFine(fine) {
    try { await request(`student/fines/${fine.id}/submit`, 'POST', { studentId }); await loadDashboard(); say('Fee payment review ke liye submit ho gayi.'); }
    catch (e) { say(e.message); }
  }
  async function confirmFine(fine) {
    try { await request(`admin/fines/${fine.id}/confirm`, 'POST'); await loadDashboard(); say('Fee paid mark kar di.'); }
    catch (e) { say(e.message); }
  }

  const filteredBooks = useMemo(() => books.filter(b => `${b.title} ${b.author} ${b.category}`.toLowerCase().includes(query.toLowerCase())), [books, query]);
  if (!role && mode === 'home') return <div className="home-page">
    <header className="home-nav"><a className="home-brand" href="#home"><img src="/university-logo.svg" alt="Babasaheb Bhimrao Ambedkar University crest"/><span><b>BBAU UNIVERSITY</b><small>LIBRARY MANAGEMENT SYSTEM</small></span></a><div><span className="demo-chip">DEMO PROJECT</span><button className="primary" onClick={() => { setMode('login'); setError(''); }}>Login</button></div></header>
    <main id="home"><section className="home-hero"><div className="home-copy"><small className="home-eyebrow">BABASAHEB BHIMRAO AMBEDKAR UNIVERSITY · LUCKNOW</small><h1>Knowledge for everyone.<br/><i>Books for every journey.</i></h1><p>Welcome to the BBAU University Library demo. Explore the catalogue, issue and return books, and manage student access from one place.</p><div className="home-actions"><button className="primary" onClick={() => { setMode('login'); setError(''); }}>Login to library　→</button><button className="home-secondary" onClick={() => { setMode('register'); setError(''); }}>Student registration</button></div><div className="home-proof"><span><b>12,480+</b><small>library books</small></span><span><b>3 dashboards</b><small>Student · Administration · Super Admin</small></span></div></div><div className="home-feature"><div className="home-crest"><img className="library-photo" src="/central-library-exterior.jpeg" alt="Exterior of BBAU Central Library" onError={e => { e.currentTarget.style.display = 'none'; }}/><img className="crest-watermark" src="/university-logo.svg" alt="BBAU university crest"/></div><div className="home-feature-label"><span>BB</span><div><b>BBAU Central Library</b><small>Babasaheb Bhimrao Ambedkar University · Demo</small></div></div></div></section><section className="home-gallery"><div><small>INSIDE THE LIBRARY</small><h2>Explore your campus library</h2><p>Photos from BBAU Central Library, Lucknow.</p></div><div className="home-gallery-grid"><figure><img src="/library-main-hall.jpeg" alt="BBAU Central Library atrium"/><figcaption>Main library atrium</figcaption></figure><figure><img src="/library-bookshelves.jpeg" alt="Books in the BBAU library stacks"/><figcaption>Books and collections</figcaption></figure><figure><img src="/library-reading-room.jpeg" alt="Reading desks inside the library"/><figcaption>Reading spaces</figcaption></figure><figure><img src="/library-study-area.jpeg" alt="Study area in the library"/><figcaption>Student study area</figcaption></figure></div></section><section className="home-services"><div><small>LIBRARY SERVICES</small><h2>Everything you need to manage the library</h2></div><div className="home-service-grid"><article><BookOpen/><b>Browse & issue</b><p>Find available titles and issue books to student accounts.</p></article><article><ArrowLeftRight/><b>Return books</b><p>Students and Administration can record book returns.</p></article><article><ScanLine/><b>OCR camera scanner</b><p>Scan a book title or printed library ID, then issue or return.</p></article><article><ShieldCheck/><b>Approved access</b><p>Administration approves student signups; Super Admin creates staff access.</p></article></div></section><footer className="home-footer">© 2026 Babasaheb Bhimrao Ambedkar University <span>BBAU UNIVERSITY LIBRARY · DEMO</span></footer></main>
  </div>;

  if (!role) return <div className="auth">
    <div className="auth-left"><div className="auth-brand"><img src="/university-logo.svg"/><span><b>Babasaheb Bhimrao Ambedkar University</b><small>UNIVERSITY LIBRARY · LUCKNOW</small></span></div><div className="auth-message"><small>KNOWLEDGE FOR EVERYONE</small><h1>Your next great<br/>read is <i>waiting.</i></h1><p>Discover, issue and return books from your university library in one simple space.</p><b>12,480+ books　·　1,846 student members</b></div><footer>PRAGYA SHEEL KARUNA · ESTABLISHED 1996</footer></div>
    <div className="auth-right"><button className="home-link" type="button" onClick={() => { setMode('home'); setError(''); }}>← BBAU Home</button><form className="auth-box" onSubmit={mode === 'register' ? registerStudent : signIn}>
      <small>BBAU UNIVERSITY LIBRARY</small><h2>{mode === 'login' ? 'Welcome back' : 'Student registration'}</h2><p>{mode === 'login' ? 'Sign in with your approved library account.' : 'Public registration is available for students only.'}</p>
      {mode === 'login' ? <><label>Account type<select value={form.access} onChange={e => set('access', e.target.value)}><option>Student</option><option>Administration</option><option>Super Admin</option></select></label><Field label="University email" type="email" autoComplete="username" required value={form.email} set={v => set('email', v)}/><Field label="Password" type="password" autoComplete="current-password" required value={form.password} set={v => set('password', v)}/><button className="primary full" disabled={busy}>{busy ? 'Signing in…' : 'Sign in　→'}</button><p className="switch">Student new to the library? <button type="button" onClick={() => { setMode('register'); setError(''); }}>Register here</button></p><div className="demo-logins"><b>Demo accounts</b><button type="button" onClick={() => setForm(f => ({ ...f, email: 'manpreet@bbau.ac.in', password: 'Student@123', access: 'Student' }))}>Student</button><button type="button" onClick={() => setForm(f => ({ ...f, email: 'meera.joshi@bbau.ac.in', password: 'Library@123', access: 'Administration' }))}>Administration</button></div></> : <><Field label="Full name" required value={form.name} set={v => set('name', v)}/><Field label="University email" type="email" required value={form.email} set={v => set('email', v)}/><Field label="Student ID" required value={form.id} set={v => set('id', v)}/><Field label="Password (minimum 8 characters)" type="password" autoComplete="new-password" minLength={8} required value={form.password} set={v => set('password', v)}/><p className="approval"><ShieldCheck/> Account ko sign in se pehle Administration approve karega.</p><button className="primary full" disabled={busy}>{busy ? 'Submitting…' : 'Submit registration'}</button><p className="switch">Already registered? <button type="button" onClick={() => { setMode('login'); setError(''); }}>Sign in</button></p></>}
      {error && <p className="auth-error" role="alert">{error}</p>}
    </form><small className="copyright">© 2026 Babasaheb Bhimrao Ambedkar University</small></div>
  </div>;

  const nav = navigation[role] || [];
  const showBooks = ['Overview', 'My dashboard', 'Book catalogue', 'Browse books'].includes(page);
  const showLoans = ['Issue & return', 'Issued books'].includes(page);
  const showMembers = ['Members', 'Student approvals'].includes(page);
  const showFines = ['Fines & fees', 'My fines'].includes(page);
  const showScanner = page === 'OCR scanner';
  const scannedBook = books.find(book => book.id === Number(scanBookId));
  const scannedLoans = scannedBook ? loans.filter(loan => loan.bookId === scannedBook.id) : [];
  const stats = root
    ? [['Administration accounts', admins.length, ShieldCheck], ['Student accounts', students.length, GraduationCap], ['System status', 'Online', CircleCheck]]
    : [['Total books', books.length, BookCopy], [student ? 'Books I have issued' : 'Books issued', loans.length, BookOpenCheck], [student ? 'My fines' : 'Members', student ? fines.length : students.length, UsersRound], [student ? 'Due soon' : 'Pending approvals', student ? loans.filter(l => l.dueDate < today()).length : students.filter(s => s.status === 'Pending').length, Clock3]];

  return <div className="app"><aside className="sidebar"><div className="brand"><img src="/university-logo.svg"/><span><b>BBAU</b><small>UNIVERSITY LIBRARY</small></span></div><div className="campus"><LibraryBig/> Lucknow Campus</div><small className="nav-label">{role.toUpperCase()} PORTAL</small><nav>{nav.map(([label, Icon]) => <button className={page === label ? 'active' : ''} key={label} onClick={() => setPage(label)}><Icon/>{label}{label === 'Student approvals' && students.some(s => s.status === 'Pending') && <em>{students.filter(s => s.status === 'Pending').length}</em>}</button>)}</nav><div className="user"><span>{name.split(' ').map(x => x[0]).slice(0, 2).join('').toUpperCase()}</span><div><b>{name}</b><small>{role}</small></div><button onClick={signOut} title="Sign out"><LogOut/></button></div></aside>
    <main className="main"><header><span>Library　›　<b>{page}</b></span><div>{new Date().toLocaleDateString('en-IN', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' })}<button onClick={signOut}>{role}　·　Sign out</button></div></header><section className="welcome"><div><small>BBAU LIBRARY · {role.toUpperCase()}</small><h1>{page === 'Overview' || page === 'My dashboard' ? root ? 'System overview' : `Welcome, ${name.split(' ')[0]}` : page}</h1><p>{root ? 'Manage Administration access and university library settings.' : student ? 'Issue books, return them, and submit fine fees.' : 'Manage books, student approvals, issues, returns and fees.'}</p></div>{admin && showBooks && <button className="primary" onClick={() => setModal('book')}>＋ Add a book</button>}</section><section className="profile-upload panel"><div className="profile-avatar">{profileImage ? <img src={profileImage} alt="Profile"/> : <span>{name.split(' ').map(x => x[0]).slice(0, 2).join('').toUpperCase()}</span>}</div><div className="profile-copy"><small>PROFILE PHOTO</small><h2>{name}</h2><p>{role} dashboard · Is account ka photo isi browser mein save hota hai.</p></div><div className="profile-actions"><button className="primary" type="button" onClick={() => profileFileRef.current?.click()}><Upload/> Upload photo</button><button className="small" type="button" onClick={openProfile}>Edit profile</button></div><input ref={profileFileRef} className="hidden-file" type="file" accept="image/png,image/jpeg,image/webp" onChange={e => { handleProfileImage(e.target.files?.[0]); e.target.value = ''; }}/></section>
      {(page === 'Overview' || page === 'My dashboard') && <><div className="stats">{stats.map(([label, value, Icon]) => <article className="stat" key={label}><Icon/><small>{label}</small><b>{value}</b><span>BBAU University Library</span></article>)}</div>{root ? <section className="panel"><PanelHead title="Administration accounts" note="Staff accounts created by Super Admin" action={<button className="primary" onClick={() => setModal('admin')}>＋ Create administrator</button>}/><AdminTable admins={admins}/></section> : <section className="panel"><PanelHead title={student ? 'Browse books' : 'Recent books'} note="University library catalogue"/><BookTable books={filteredBooks.slice(0, 5)} student={student} admin={admin} issue={issueBook} remove={removeBook}/></section>}</>}
      {showBooks && page !== 'Overview' && page !== 'My dashboard' && <section className="panel"><PanelHead title={student ? 'Browse books' : 'Book catalogue'} note="Search titles, authors and categories" action={admin && <button className="primary" onClick={() => setModal('book')}>＋ Add a book</button>}/><label className="search"><Search/><input placeholder="Search title, author or category" value={query} onChange={e => setQuery(e.target.value)}/></label><BookTable books={filteredBooks} student={student} admin={admin} issue={issueBook} remove={removeBook}/></section>}
      {showScanner && <section className="panel scanner-panel"><PanelHead title="OCR book scanner" note="Scan the printed title or library ID, then issue or return the matched book."/><div className="scanner-layout"><div className="scanner-camera">{cameraStream ? <video ref={videoRef} autoPlay muted playsInline/> : <div className="camera-placeholder"><Camera/><b>Camera ready</b><small>Camera access sirf scan karte waqt use hota hai.</small></div>}<div className="scanner-actions">{!cameraStream ? <button className="primary" onClick={startCamera}><Camera/> Start camera</button> : <><button className="primary" disabled={scanBusy} onClick={captureCameraFrame}><ScanLine/> Capture & scan</button><button className="small" onClick={stopCamera}>Stop camera</button></>}<button className="small" disabled={scanBusy} onClick={() => fileRef.current?.click()}><Upload/> Upload photo</button><input ref={fileRef} className="hidden-file" type="file" accept="image/*" capture="environment" onChange={e => { handleImage(e.target.files?.[0]); e.target.value = ''; }}/></div>{scanBusy && <div className="ocr-progress"><span style={{ width: `${Math.round(scanProgress * 100)}%` }}/></div>}<p className="scan-status" aria-live="polite">{scanStatus || 'Cover/title ya book label par focus karke scan karein. Printed library ID jaise BK-101 sabse achha match deta hai.'}</p></div><div className="scan-match"><small>SCAN RESULT</small>{scannedBook ? <><h2>{scannedBook.title}</h2><p>{scannedBook.author} · BK-{scannedBook.id}</p><p>{scannedBook.available} of {scannedBook.copies} copies available</p>{student ? scannedLoans.length ? <button className="primary" onClick={() => returnBook(scannedLoans[0])}>Return book</button> : <button className="primary" disabled={!scannedBook.available} onClick={() => issueBook(scannedBook)}>Issue to my account</button> : <>{scannedBook.available > 0 && <button className="primary" onClick={() => issueBook(scannedBook)}>Issue to student</button>}{scannedLoans.map(loan => <div className="scan-loan" key={loan.id}><span>Issued to {loan.studentId} · due {loan.dueDate}</span><button className="small" onClick={() => returnBook(loan)}>Return</button></div>)}</>}</> : <><p className="scan-empty">{ocrText ? 'Book automatically match nahi hui. List se select karein.' : 'Scan ke baad recognized book yahan dikhegi.'}</p><select aria-label="Select matched book" value={scanBookId || ''} onChange={e => setScanBookId(e.target.value ? Number(e.target.value) : null)}><option value="">Select a book</option>{books.map(book => <option key={book.id} value={book.id}>{book.title} · BK-{book.id}</option>)}</select></>}{ocrText && <details className="recognized-text"><summary>OCR text</summary><pre>{ocrText}</pre></details>}</div></div></section>}
      {showLoans && <section className="panel"><PanelHead title={student ? 'Issued books' : 'Issue & return'} note={student ? 'Books issued to your account. Return them from here.' : 'Current issued books and their return status'}/><Table heads={['MEMBER', 'STUDENT ID', 'BOOK', 'DUE DATE', 'STATUS', '']}>{loans.map(l => <tr key={l.id}><td>{l.memberName}</td><td>{l.studentId}</td><td>{l.bookTitle}</td><td>{l.dueDate}</td><td><i className={l.dueDate < today() ? 'pending' : 'good'}>{l.dueDate < today() ? 'OVERDUE' : 'ISSUED'}</i></td><td>{(admin || student) && <button className="small" onClick={() => returnBook(l)}>{student ? 'Return book' : 'Return'}</button>}</td></tr>)}</Table>{!loans.length && <p className="empty">No books currently issued.</p>}</section>}
      {showMembers && <section className="panel"><PanelHead title={page === 'Student approvals' ? 'Student registration approvals' : 'Library members'} note={page === 'Student approvals' ? 'Administration reviews public student signup requests.' : 'Registered students and account status'}/><Table heads={['STUDENT', 'STUDENT ID', 'EMAIL', 'STATUS', '']}>{students.filter(s => page !== 'Student approvals' || s.status === 'Pending').map(s => <tr key={s.id}><td>{s.name}</td><td>{s.studentId}</td><td>{s.email}</td><td><i className={s.status === 'Active' ? 'good' : 'pending'}>{s.status.toUpperCase()}</i></td><td>{admin && s.status === 'Pending' && <><button className="small" onClick={() => reviewStudent(s, true)}>Approve</button> <button className="small danger" onClick={() => reviewStudent(s, false)}>Reject</button></>}</td></tr>)}</Table>{page === 'Student approvals' && !students.some(s => s.status === 'Pending') && <p className="empty">No student requests awaiting approval.</p>}</section>}
      {showFines && <section className="panel"><PanelHead title={student ? 'My fines' : 'Fines & fee submissions'} note={student ? 'Submit a due fee for Administration review.' : 'Record fines and confirm submitted student payments.'} action={admin && <button className="primary" onClick={addFine}>＋ Add fine</button>}/><Table heads={student ? ['REASON', 'FINE FEE', 'STATUS', ''] : ['STUDENT', 'STUDENT ID', 'REASON', 'FINE FEE', 'STATUS', '']}>{fines.map(f => <tr key={f.id}>{!student && <><td>{f.memberName}</td><td>{f.studentId}</td></>}<td>{f.reason}</td><td>₹{f.amount}</td><td><i className={f.status === 'PAID' ? 'good' : f.status === 'SUBMITTED' ? 'submitted' : 'pending'}>{f.status}</i></td><td>{student && f.status === 'DUE' && <button className="small" onClick={() => submitFine(f)}>Submit fee</button>}{admin && f.status === 'SUBMITTED' && <button className="small" onClick={() => confirmFine(f)}>Confirm paid</button>}</td></tr>)}</Table>{!fines.length && <p className="empty">No fine fees to show.</p>}</section>}
      {page === 'Administration' && <section className="panel"><PanelHead title="Administration accounts" note="Staff accounts created by Super Admin" action={<button className="primary" onClick={() => setModal('admin')}>＋ Create administrator</button>}/><AdminTable admins={admins}/></section>}
      {page === 'System settings' && <section className="panel empty"><ShieldCheck/><h2>Account access policy</h2><p>Students register publicly and need Administration approval.</p><p>Super Admin creates all Administration accounts.</p></section>}
      {page === 'Reports' && <section className="panel empty"><ChartNoAxesCombined/><h2>Library reports</h2><p>Current issue and membership summaries are shown on this dashboard.</p></section>}
      {page === 'My account' && <section className="panel empty"><UserRound/><h2>{name}</h2><p>Student ID: {studentId}</p><p>Sign-in email: your approved university email</p></section>}
      <footer className="foot">© 2026 Babasaheb Bhimrao Ambedkar University · Library Management System <span>● System operational</span></footer>
    </main>{toast && <div className="toast">✓　{toast}</div>}
    {modal && <div className="shade" onClick={e => e.target === e.currentTarget && setModal('')}><form className="dialog" onSubmit={modal === 'book' ? addBook : modal === 'profile' ? updateProfile : createAdmin}><button type="button" className="x" onClick={() => setModal('')}>×</button><h2>{modal === 'book' ? 'Add a book' : modal === 'profile' ? 'Edit profile' : 'Create Administration account'}</h2><p>{modal === 'book' ? 'Add a title to the university catalogue.' : modal === 'profile' ? 'Update your name or reset your password.' : 'Set a password for this new staff account.'}</p>{modal === 'book' ? <><Field label="Title" required value={form.title} set={v => set('title', v)}/><Field label="Author" required value={form.author} set={v => set('author', v)}/><label>Category<select value={form.category} onChange={e => set('category', e.target.value)}><option>Fiction</option><option>History</option><option>Science</option><option>Computer science</option><option>Biography</option></select></label></> : modal === 'profile' ? <><Field label="Full name" required value={profileForm.name} set={v => setProfileForm(current => ({ ...current, name: v }))}/><Field label="Current password (only for password reset)" type="password" autoComplete="current-password" value={profileForm.currentPassword} set={v => setProfileForm(current => ({ ...current, currentPassword: v }))}/><Field label="New password (minimum 8 characters)" type="password" autoComplete="new-password" minLength={8} value={profileForm.newPassword} set={v => setProfileForm(current => ({ ...current, newPassword: v }))}/></> : <><Field label="Full name" required value={form.name} set={v => set('name', v)}/><Field label="University email" type="email" required value={form.email} set={v => set('email', v)}/><label>Staff role<select value={form.category} onChange={e => set('category', e.target.value)}><option>Librarian</option><option>Library staff</option></select></label><Field label="Temporary password (minimum 8 characters)" type="password" minLength={8} required value={form.password} set={v => set('password', v)}/></>}<div className="modal-actions"><button type="button" className="cancel" onClick={() => setModal('')}>Cancel</button><button className="primary">{modal === 'book' ? 'Add book' : modal === 'profile' ? 'Save profile' : 'Create account'}</button></div></form></div>}
  </div>;
}

function Field({ label, set, ...props }) { return <label>{label}<input {...props} onChange={e => set(e.target.value)}/></label>; }
function PanelHead({ title, note, action }) { return <div className="panel-head"><span><h2>{title}</h2><small>{note}</small></span>{action}</div>; }
function Table({ heads, children }) { return <div className="scroll"><table><thead><tr>{heads.map(h => <th key={h}>{h}</th>)}</tr></thead><tbody>{children}</tbody></table></div>; }
function BookTable({ books, student, admin, issue, remove }) { return <Table heads={['TITLE', 'BOOK ID', 'CATEGORY', 'AVAILABLE', '']}>{books.map(b => <tr key={b.id}><td><b>{b.title}</b><small className="sub">{b.author}</small></td><td>BK-{b.id}</td><td>{b.category}</td><td>{b.available} of {b.copies}</td><td>{(student || admin) && <button className="small" disabled={!b.available} onClick={() => issue(b)}>Issue</button>} {admin && <button className="small danger" onClick={() => remove(b)}>Delete</button>}</td></tr>)}</Table>; }
function AdminTable({ admins }) { return <Table heads={['NAME', 'EMAIL', 'ACCESS ROLE', 'STATUS']}>{admins.map(a => <tr key={a.email}><td>{a.name}</td><td>{a.email}</td><td>{a.staffRole}</td><td><i className="good">ACTIVE</i></td></tr>)}</Table>; }

createRoot(document.getElementById('root')).render(<App/>);
