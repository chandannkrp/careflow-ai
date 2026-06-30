import {
  ClipboardPlus,
  Gauge,
  GripVertical,
  Hospital,
  ListOrdered,
  KanbanSquare,
  Bot,
  Search,
  UsersRound,
  type LucideIcon,
} from 'lucide-react';
import { type PointerEvent, useCallback, useEffect, useState } from 'react';
import { getDepartments } from '../api/client';
import { AiAgentChat } from '../features/ai-chat';
import { AgentDashboardPanel } from '../features/agent';
import { SystemAgentsPanel } from '../features/agents';
import { CareBoard } from '../features/board';
import { IntakeForm } from '../features/intake';
import { MetricsDashboard } from '../features/metrics';
import { PeopleDirectory } from '../features/people';
import { QueueTable } from '../features/queue';

type WorkspaceRoute = 'queue' | 'board' | 'intake' | 'dashboard' | 'people' | 'agents';

const navigationItems: Array<{ route: WorkspaceRoute; label: string; icon: LucideIcon }> = [
  { route: 'queue', label: 'Queue', icon: ListOrdered },
  { route: 'board', label: 'Board', icon: KanbanSquare },
  { route: 'intake', label: 'Intake', icon: ClipboardPlus },
  { route: 'dashboard', label: 'Dashboard', icon: Gauge },
  { route: 'people', label: 'People', icon: UsersRound },
  { route: 'agents', label: 'Agents', icon: Bot },
];

const defaultDepartments = ['Emergency', 'Pediatrics', 'Orthopedics', 'General'];

function routeFromHash(): WorkspaceRoute {
  const hash = window.location.hash.replace('#/', '');
  if (hash === 'intake' || hash === 'dashboard' || hash === 'queue' || hash === 'people' || hash === 'board' || hash === 'agents') {
    return hash;
  }
  return 'queue';
}

export function App() {
  const [activeRoute, setActiveRoute] = useState<WorkspaceRoute>(() => routeFromHash());
  const [queueRefreshKey, setQueueRefreshKey] = useState(0);
  const [metricsRefreshKey, setMetricsRefreshKey] = useState(0);
  const [navWidth, setNavWidth] = useState(288);
  const [searchQuery, setSearchQuery] = useState('');
  const [departments, setDepartments] = useState<string[]>(defaultDepartments);

  useEffect(() => {
    const handleHashChange = () => setActiveRoute(routeFromHash());
    window.addEventListener('hashchange', handleHashChange);
    if (!window.location.hash) {
      window.history.replaceState(null, '', '#/queue');
    }
    return () => window.removeEventListener('hashchange', handleHashChange);
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

  return (
    <main className="min-h-screen bg-sky-50 font-sans text-slate-950 transition-colors">
      <div className="flex min-h-screen">
        <aside
          className="relative hidden shrink-0 border-r border-sky-100 bg-white p-4 shadow-sm lg:block"
          style={{ width: navWidth }}
        >
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-md bg-slate-950 text-white">
              <Hospital size={21} aria-hidden="true" />
            </div>
            <div>
              <p className="text-base font-semibold text-slate-950">CareFlow AI</p>
              <p className="text-xs font-medium text-sky-700">Emergency workspace</p>
            </div>
          </div>

          <label className="mt-6 block text-sm font-medium text-slate-700">
            Queue search
            <span className="mt-1 flex h-10 items-center gap-2 rounded-md border border-sky-200 bg-sky-50 px-3">
              <Search size={16} className="text-sky-700" aria-hidden="true" />
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
                    : 'text-slate-700 hover:bg-sky-50 hover:text-slate-950'
                }`}
              >
                <Icon size={17} aria-hidden="true" />
                {label}
              </button>
            ))}
          </nav>

          <button
            type="button"
            onPointerDown={handleNavResize}
            className="absolute right-0 top-0 flex h-full w-3 translate-x-1/2 cursor-col-resize items-center justify-center text-sky-700 opacity-60 transition hover:opacity-100"
            aria-label="Resize navigation"
          >
            <GripVertical size={14} aria-hidden="true" />
          </button>
        </aside>

        <section className="flex min-w-0 flex-1 flex-col">
          <header className="sticky top-0 z-10 border-b border-sky-100 bg-sky-50/95 px-4 py-3 backdrop-blur lg:hidden">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-md bg-slate-950 text-white">
                  <Hospital size={19} aria-hidden="true" />
                </div>
                <p className="font-semibold">CareFlow AI</p>
              </div>
            </div>
            <div className="mt-3 grid grid-cols-3 gap-2">
              {navigationItems.map(({ route, label }) => (
                <button
                  key={route}
                  type="button"
                  onClick={() => navigate(route)}
                  className={`h-9 rounded-md text-sm font-medium ${
                    activeRoute === route
                      ? 'bg-slate-950 text-white'
                      : 'border border-sky-200 bg-white text-slate-700'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
          </header>

          <div className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
            {activeRoute === 'queue' ? (
              <QueueTable
                refreshSignal={queueRefreshKey}
                searchQuery={searchQuery}
                onStatusUpdated={refreshOperationalViews}
              />
            ) : null}
            {activeRoute === 'intake' ? (
              <IntakeForm departments={departments} onCreated={refreshOperationalViews} />
            ) : null}
            {activeRoute === 'board' ? <CareBoard departments={departments} /> : null}
            {activeRoute === 'dashboard' ? (
              <div className="space-y-6">
                <AgentDashboardPanel refreshSignal={metricsRefreshKey} />
                <MetricsDashboard refreshSignal={metricsRefreshKey} />
              </div>
            ) : null}
            {activeRoute === 'people' ? <PeopleDirectory departments={departments} /> : null}
            {activeRoute === 'agents' ? <SystemAgentsPanel /> : null}
          </div>
        </section>
      </div>

      <AiAgentChat activeStaff={null} onAction={handleAiAction} />
    </main>
  );
}
