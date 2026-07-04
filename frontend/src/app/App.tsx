import {
  BedDouble,
  CalendarDays,
  ClipboardPlus,
  FileUp,
  Gauge,
  GripVertical,
  Hospital,
  Home,
  ListOrdered,
  LogIn,
  LogOut,
  KanbanSquare,
  MessageSquareText,
  Search,
  UsersRound,
  type LucideIcon,
} from 'lucide-react';
import { type PointerEvent, useCallback, useEffect, useState } from 'react';
import { getDepartments, getStaffUsers } from '../api/client';
import { AiAgentChat, AiChatPage } from '../features/ai-chat';
import { HospitalLiveChat } from '../features/ai-chat/HospitalLiveChat';
import { AllocationDashboard } from '../features/allocation';
import { CareBoard } from '../features/board';
import { AppointmentsCalendar } from '../features/calendar/AppointmentsCalendar';
import { IntakeForm } from '../features/intake';
import { KnowledgePage } from '../features/knowledge/KnowledgePage';
import { MetricsDashboard } from '../features/metrics';
import { PeopleDirectory } from '../features/people';
import { QueueTable } from '../features/queue';
import type { Appointment, StaffRole, StaffUser } from '../types/careflow';

type WorkspaceRoute = 'home' | 'queue' | 'board' | 'intake' | 'allocation' | 'dashboard' | 'people' | 'knowledge' | 'calendar';
type DemoUser = { id: string; name: string; role: StaffRole; dashboard: string };

const navigationItems: Array<{ route: WorkspaceRoute; label: string; icon: LucideIcon }> = [
  { route: 'home', label: 'Home', icon: Home },
  { route: 'queue', label: 'Queue', icon: ListOrdered },
  { route: 'board', label: 'Board', icon: KanbanSquare },
  { route: 'intake', label: 'Intake', icon: ClipboardPlus },
  { route: 'allocation', label: 'Allocation', icon: BedDouble },
  { route: 'dashboard', label: 'Dashboard', icon: Gauge },
  { route: 'people', label: 'People', icon: UsersRound },
  { route: 'knowledge', label: 'Knowledge', icon: FileUp },
  { route: 'calendar', label: 'Calendar', icon: CalendarDays },
];

const demoUsers: DemoUser[] = [
  { id: 'doctor', name: 'Demo Doctor', role: 'DOCTOR', dashboard: 'Doctor dashboard' },
  { id: 'nurse', name: 'Demo Nurse', role: 'TRIAGE_NURSE', dashboard: 'Nursing dashboard' },
  { id: 'staff', name: 'Demo Staff', role: 'INTAKE_STAFF', dashboard: 'Intake staff dashboard' },
  { id: 'admin', name: 'Demo Admin', role: 'ADMIN', dashboard: 'Admin dashboard' },
];

const defaultDepartments = ['Emergency', 'Pediatrics', 'Orthopedics', 'General'];

function routeFromHash(): WorkspaceRoute {
  const hash = window.location.hash.replace('#/', '');
  if (
    hash === 'home' ||
    hash === 'intake' ||
    hash === 'dashboard' ||
    hash === 'queue' ||
    hash === 'people' ||
    hash === 'board' ||
    hash === 'allocation' ||
    hash === 'knowledge' ||
    hash === 'calendar'
  ) {
    return hash;
  }
  return 'home';
}

export function App() {
  if (window.location.pathname === '/ai-chat') {
    return <AiChatPage />;
  }

  return <WorkspaceApp />;
}

function WorkspaceApp() {
  const [activeRoute, setActiveRoute] = useState<WorkspaceRoute>(() => routeFromHash());
  const [queueRefreshKey, setQueueRefreshKey] = useState(0);
  const [metricsRefreshKey, setMetricsRefreshKey] = useState(0);
  const [navWidth, setNavWidth] = useState(288);
  const [searchQuery, setSearchQuery] = useState('');
  const [departments, setDepartments] = useState<string[]>(defaultDepartments);
  const [staffUsers, setStaffUsers] = useState<StaffUser[]>([]);
  const [activeStaffId, setActiveStaffId] = useState('');
  const [currentUser, setCurrentUser] = useState<DemoUser | null>(() => {
    const stored = window.localStorage.getItem('careflow-user');
    return demoUsers.find((user) => user.id === stored) ?? null;
  });
  const [appointments, setAppointments] = useState<Appointment[]>(() => {
    const stored = window.localStorage.getItem('careflow-appointments');
    if (!stored) {
      return [];
    }
    try {
      return JSON.parse(stored) as Appointment[];
    } catch {
      return [];
    }
  });
  const activeStaff =
    staffUsers.find((staff) => staff.id === activeStaffId) ??
    staffUsers.find((staff) => staff.role === currentUser?.role) ??
    (currentUser
      ? {
          id: currentUser.id,
          staffCode: currentUser.id.toUpperCase(),
          displayName: currentUser.name,
          role: currentUser.role,
          active: true,
        }
      : null);

  useEffect(() => {
    window.localStorage.setItem('careflow-appointments', JSON.stringify(appointments));
  }, [appointments]);

  useEffect(() => {
    const handleHashChange = () => setActiveRoute(routeFromHash());
    window.addEventListener('hashchange', handleHashChange);
    if (!window.location.hash) {
      window.history.replaceState(null, '', '#/home');
    }
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, []);

  useEffect(() => {
    if (!currentUser || staffUsers.length === 0 || activeStaffId) {
      return;
    }
    setActiveStaffId(staffUsers.find((staff) => staff.role === currentUser.role)?.id ?? staffUsers[0]?.id ?? '');
  }, [activeStaffId, currentUser, staffUsers]);

  useEffect(() => {
    let isMounted = true;

    async function loadStaff() {
      try {
        const fetchedStaff = await getStaffUsers();
        if (isMounted) {
          const activeStaffUsers = fetchedStaff.filter((staff) => staff.active);
          setStaffUsers(activeStaffUsers);
          setActiveStaffId((current) => current || activeStaffUsers[0]?.id || '');
        }
      } catch {
        if (isMounted) {
          setStaffUsers([]);
        }
      }
    }

    void loadStaff();
    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    let isMounted = true;

    async function loadDepartments() {
      try {
        const fetchedDepartments = await getDepartments();
        if (isMounted && fetchedDepartments.length > 0) {
          setDepartments(fetchedDepartments);
        }
      } catch {
        if (isMounted) {
          setDepartments(defaultDepartments);
        }
      }
    }

    void loadDepartments();
    return () => {
      isMounted = false;
    };
  }, []);

  const navigate = (route: WorkspaceRoute) => {
    window.location.hash = `#/${route}`;
    setActiveRoute(route);
  };

  const handleLogin = (user: DemoUser) => {
    window.localStorage.setItem('careflow-user', user.id);
    setCurrentUser(user);
    setActiveStaffId(staffUsers.find((staff) => staff.role === user.role)?.id ?? '');
  };

  const handleLogout = () => {
    window.localStorage.removeItem('careflow-user');
    setCurrentUser(null);
  };

  const saveAppointment = (appointment: Appointment) => {
    setAppointments((current) => [appointment, ...current.filter((item) => item.id !== appointment.id)]);
  };

  const refreshOperationalViews = () => {
    setQueueRefreshKey((current) => current + 1);
    setMetricsRefreshKey((current) => current + 1);
  };

  const handleNavResize = useCallback(
    (event: PointerEvent<HTMLButtonElement>) => {
      const startX = event.clientX;
      const startWidth = navWidth;
      event.currentTarget.setPointerCapture(event.pointerId);

      const handleMove = (moveEvent: globalThis.PointerEvent) => {
        setNavWidth(Math.min(380, Math.max(236, startWidth + moveEvent.clientX - startX)));
      };

      const handleUp = () => {
        window.removeEventListener('pointermove', handleMove);
        window.removeEventListener('pointerup', handleUp);
      };

      window.addEventListener('pointermove', handleMove);
      window.addEventListener('pointerup', handleUp);
    },
    [navWidth],
  );

  const handleAiAction = (action: string) => {
    if (action === 'refresh_queue') {
      setQueueRefreshKey((current) => current + 1);
    } else if (action === 'refresh_dashboard') {
      setMetricsRefreshKey((current) => current + 1);
    } else if (action === 'open_intake') {
      navigate('intake');
    } else if (action === 'filter_critical_high') {
      navigate('queue');
      setSearchQuery('critical');
    }
  };

  if (!currentUser) {
    return <LoginScreen onLogin={handleLogin} />;
  }

  return (
    <main className="min-h-screen bg-slate-100 font-sans text-slate-950 transition-colors">
      <div className="flex min-h-screen">
        <aside
          className="relative hidden shrink-0 border-r border-slate-200 bg-white p-4 shadow-sm lg:block"
          style={{ width: navWidth }}
        >
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-md bg-slate-950 text-white">
              <Hospital size={21} aria-hidden="true" />
            </div>
            <div>
              <p className="text-base font-semibold text-slate-950">CareFlow AI</p>
              <p className="text-xs font-medium text-emerald-700">Healthcare workspace</p>
            </div>
          </div>

          <label className="mt-6 block text-sm font-medium text-slate-700">
            Active staff
            <select
              value={activeStaff?.id ?? ''}
              onChange={(event) => setActiveStaffId(event.target.value)}
              className="input-field"
            >
              {staffUsers.length === 0 ? <option value="">Demo Staff</option> : null}
              {staffUsers.map((staff) => (
                <option key={staff.id} value={staff.id}>
                  {staff.displayName} - {staff.staffCode}
                </option>
              ))}
            </select>
          </label>

          <div className="mt-3 rounded-md border border-emerald-100 bg-emerald-50 p-3 text-xs text-emerald-900">
            <p className="font-semibold">{currentUser.dashboard}</p>
            <p className="mt-1">{currentUser.name} - {currentUser.role.replace(/_/g, ' ')}</p>
          </div>

          <label className="mt-4 block text-sm font-medium text-slate-700">
            Queue search
            <span className="mt-1 flex h-10 items-center gap-2 rounded-md border border-slate-200 bg-slate-50 px-3">
              <Search size={16} className="text-emerald-700" aria-hidden="true" />
              <input
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                className="min-w-0 flex-1 bg-transparent text-sm text-slate-950 outline-none placeholder:text-slate-400"
                placeholder="Patient, complaint, status"
              />
            </span>
          </label>

          <nav className="mt-6 space-y-1" aria-label="Workspace routes">
            {navigationItems.map(({ route, label, icon: Icon }) => (
              <button
                key={route}
                type="button"
                onClick={() => navigate(route)}
                className={`flex h-10 w-full items-center gap-2 rounded-md px-3 text-sm font-medium transition ${
                  activeRoute === route
                    ? 'bg-slate-950 text-white'
                    : 'text-slate-700 hover:bg-emerald-50 hover:text-slate-950'
                }`}
              >
                <Icon size={17} aria-hidden="true" />
                {label}
              </button>
            ))}
          </nav>

          <button
            type="button"
            onClick={handleLogout}
            className="mt-4 flex h-10 w-full items-center gap-2 rounded-md px-3 text-sm font-medium text-rose-700 transition hover:bg-rose-50"
          >
            <LogOut size={17} aria-hidden="true" />
            Logout
          </button>

          <button
            type="button"
            onPointerDown={handleNavResize}
            className="absolute right-0 top-0 flex h-full w-3 translate-x-1/2 cursor-col-resize items-center justify-center text-emerald-700 opacity-60 transition hover:opacity-100"
            aria-label="Resize navigation"
          >
            <GripVertical size={14} aria-hidden="true" />
          </button>
        </aside>

        <section className="flex min-w-0 flex-1 flex-col">
          <header className="sticky top-0 z-10 border-b border-slate-200 bg-white/95 px-4 py-3 backdrop-blur lg:hidden">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-md bg-slate-950 text-white">
                  <Hospital size={19} aria-hidden="true" />
                </div>
                <p className="font-semibold">CareFlow AI</p>
              </div>
            </div>
            <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-3">
              {navigationItems.map(({ route, label }) => (
                <button
                  key={route}
                  type="button"
                  onClick={() => navigate(route)}
                  className={`h-9 rounded-md text-sm font-medium ${
                    activeRoute === route
                      ? 'bg-slate-950 text-white'
                      : 'border border-slate-200 bg-white text-slate-700'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
            <select
              value={activeStaff?.id ?? ''}
              onChange={(event) => setActiveStaffId(event.target.value)}
              className="mt-3 h-10 w-full rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-800 shadow-sm"
            >
              {staffUsers.length === 0 ? <option value="">Demo Staff</option> : null}
              {staffUsers.map((staff) => (
                <option key={staff.id} value={staff.id}>
                  {staff.displayName} - {staff.staffCode}
                </option>
              ))}
            </select>
            <button
              type="button"
              onClick={handleLogout}
              className="mt-3 inline-flex h-10 w-full items-center justify-center gap-2 rounded-md border border-rose-200 bg-white text-sm font-medium text-rose-700"
            >
              <LogOut size={16} aria-hidden="true" />
              Logout
            </button>
          </header>

          <div className="mx-auto w-full min-w-0 max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
            {activeRoute === 'home' ? (
              <HomeWorkspace
                activeStaff={activeStaff}
                currentUser={currentUser}
                onAction={handleAiAction}
                onNavigate={navigate}
              />
            ) : null}
            {activeRoute === 'queue' ? (
              <QueueTable
                refreshSignal={queueRefreshKey}
                searchQuery={searchQuery}
                activeStaff={activeStaff}
                onAppointmentSaved={saveAppointment}
                onStatusUpdated={refreshOperationalViews}
              />
            ) : null}
            {activeRoute === 'intake' ? (
              <IntakeForm departments={departments} activeStaff={activeStaff} onCreated={refreshOperationalViews} />
            ) : null}
            {activeRoute === 'board' ? <CareBoard departments={departments} /> : null}
            {activeRoute === 'allocation' ? <AllocationDashboard /> : null}
            {activeRoute === 'dashboard' ? <MetricsDashboard refreshSignal={metricsRefreshKey} /> : null}
            {activeRoute === 'people' ? <PeopleDirectory departments={departments} /> : null}
            {activeRoute === 'knowledge' ? <KnowledgePage /> : null}
            {activeRoute === 'calendar' ? <AppointmentsCalendar appointments={appointments} /> : null}
          </div>
        </section>
      </div>
    </main>
  );
}

function LoginScreen({ onLogin }: { onLogin: (user: DemoUser) => void }) {
  const [selectedUserId, setSelectedUserId] = useState(demoUsers[0].id);
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const selectedUser = demoUsers.find((user) => user.id === selectedUserId) ?? demoUsers[0];

  const submit = () => {
    if (password !== 'careflow') {
      setError('Use password careflow for the demo login.');
      return;
    }
    onLogin(selectedUser);
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-100 p-4 text-slate-950">
      <section className="w-full max-w-md rounded-lg border border-sky-100 bg-white p-5 shadow-xl">
        <div className="flex items-center gap-3">
          <span className="flex h-11 w-11 items-center justify-center rounded-md bg-slate-950 text-white">
            <Hospital size={21} aria-hidden="true" />
          </span>
          <div>
            <p className="text-sm font-medium text-sky-700">CareFlow login</p>
            <h1 className="text-xl font-semibold text-slate-950">Choose workspace role</h1>
          </div>
        </div>

        <label className="mt-5 block text-sm font-medium text-slate-700">
          User
          <select value={selectedUserId} onChange={(event) => setSelectedUserId(event.target.value)} className="input-field">
            {demoUsers.map((user) => (
              <option key={user.id} value={user.id}>
                {user.name} - {user.role.replace(/_/g, ' ')}
              </option>
            ))}
          </select>
        </label>
        <label className="mt-3 block text-sm font-medium text-slate-700">
          Password
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                submit();
              }
            }}
            className="input-field"
            placeholder="careflow"
          />
        </label>

        {error ? <p className="mt-3 rounded-md bg-red-50 p-3 text-sm text-red-800">{error}</p> : null}

        <button
          type="button"
          onClick={submit}
          className="mt-5 inline-flex h-10 w-full items-center justify-center gap-2 rounded-md bg-slate-950 px-3 text-sm font-medium text-white transition hover:bg-slate-800"
        >
          <LogIn size={16} aria-hidden="true" />
          Login
        </button>
      </section>
    </main>
  );
}

function HomeWorkspace({
  activeStaff,
  currentUser,
  onAction,
  onNavigate,
}: {
  activeStaff: StaffUser | null;
  currentUser: DemoUser;
  onAction: (action: string) => void;
  onNavigate: (route: WorkspaceRoute) => void;
}) {
  return (
    <section aria-labelledby="home-title" className="py-6">
      <div className="min-w-0 rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="min-w-0">
            <p className="text-sm font-medium text-emerald-700">Home</p>
            <h2 id="home-title" className="mt-1 text-2xl font-semibold text-slate-950 sm:text-3xl">
              Careflow Healthcare Autonomous systems
            </h2>
            <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-600">
              Coordinate triage, intake intelligence, doctor assignment, beds, reports, hospital knowledge, and live care-team messaging from one clinical operations view.
            </p>
          </div>
          <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-md bg-emerald-700 text-white animate-soft-pulse">
            <MessageSquareText size={20} aria-hidden="true" />
          </span>
        </div>

        <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-6">
          {[
            { label: 'Live queue', route: 'queue' as WorkspaceRoute },
            { label: 'LLM intake', route: 'intake' as WorkspaceRoute },
            { label: 'Beds and doctors', route: 'allocation' as WorkspaceRoute },
            { label: 'Care board', route: 'board' as WorkspaceRoute },
            { label: 'Savi knowledge', route: 'knowledge' as WorkspaceRoute },
            { label: 'Calendar', route: 'calendar' as WorkspaceRoute },
          ].map((item) => (
            <button
              key={item.route}
              type="button"
              onClick={() => onNavigate(item.route)}
              className="rounded-md border border-slate-200 bg-slate-50 p-3 text-left text-sm font-medium text-slate-700 transition hover:-translate-y-0.5 hover:border-emerald-300 hover:bg-emerald-50"
            >
              {item.label}
            </button>
          ))}
        </div>

        <div className="mt-5 grid gap-3 text-sm sm:grid-cols-4">
          {[
            currentUser.dashboard,
            'Autonomous triage',
            'Semantic intake memory',
            'Directory-aware Savi chat',
          ].map((label) => (
            <div key={label} className="min-w-0 rounded-md border border-emerald-100 bg-emerald-50 p-3 font-medium text-emerald-900">
              {label}
            </div>
          ))}
        </div>

        <div className="mt-5 rounded-md bg-slate-950 p-4 text-sm text-white">
          Active staff: {activeStaff ? `${activeStaff.displayName} (${activeStaff.staffCode})` : currentUser.name}
        </div>
      </div>

      <div className="mt-5 grid min-w-0 gap-5 xl:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)]">
        <AiAgentChat activeStaff={activeStaff} onAction={onAction} embedded />
        <HospitalLiveChat activeStaff={activeStaff} />
      </div>

    </section>
  );
}
