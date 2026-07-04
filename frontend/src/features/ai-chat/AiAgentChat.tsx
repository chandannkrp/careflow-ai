import { Bot, ClipboardList, Loader2, Send, Sparkles, X } from 'lucide-react';
import { type FormEvent, useState } from 'react';
import { sendAiChatMessage } from '../../api/client';
import type { StaffUser } from '../../types/careflow';

interface ChatMessage {
  id: number;
  role: 'staff' | 'assistant';
  text: string;
  aiBacked?: boolean;
  isPending?: boolean;
}

interface AiAgentChatProps {
  activeStaff: StaffUser | null;
  onAction: (action: string) => void;
  embedded?: boolean;
}

const exampleQueries = [
  'Which high urgency patients need attention first?',
  'Find chest pain patients and summarize their triage notes.',
  'Which doctors are assigned and who is still waiting?',
  'What beds are occupied by department?',
];

function localAgentFallback(message: string) {
  const normalized = message.toLowerCase();

  if (normalized.includes('critical') || normalized.includes('urgent')) {
    return {
      text: 'I can switch you to the queue and search for critical cases. Backend AI is not reachable right now, so this is a local workflow fallback.',
      actions: ['filter_critical_high'],
    };
  }

  if (normalized.includes('intake')) {
    return {
      text: 'Open the intake route and capture arrival mode, complaint, symptoms, vitals, distress, risk flags, and department. Staff attribution is filled from the active staff profile.',
      actions: ['open_intake'],
    };
  }

  if (normalized.includes('refresh') || normalized.includes('reload')) {
    return {
      text: 'I can refresh the queue and dashboard views. Backend AI is not reachable right now, so this is a local workflow fallback.',
      actions: ['refresh_queue', 'refresh_dashboard'],
    };
  }

  if (normalized.includes('wait')) {
    return {
      text: 'Use the dashboard wait-time graph for overall trends, or sort the queue by wait time for row-level review.',
      actions: ['refresh_dashboard'],
    };
  }

  if (normalized.includes('next') || normalized.includes('phase')) {
    return {
      text: 'Next, tighten the demo workflow: verify intake creation, queue ordering, treatment start, dashboard refresh, and AI advisory fallback. Then add patient detail and audit history.',
      actions: ['refresh_queue', 'refresh_dashboard'],
    };
  }

  return {
    text: 'I can help with queue status, intake completeness, wait-time visibility, workflow navigation, and demo next steps. Try asking about critical patients, intake, waits, or what to do next.',
    actions: [],
  };
}

export function AiAgentChat({ activeStaff, onAction, embedded = false }: AiAgentChatProps) {
  const [isOpen, setIsOpen] = useState(embedded);
  const [message, setMessage] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 1,
      role: 'assistant',
      text: 'I can answer from live queue, intake, doctor, bed, and hospital directory context.',
      aiBacked: false,
    },
  ]);
  const hasStaffMessages = messages.some((item) => item.role === 'staff');

  const sendMessage = async (rawMessage: string) => {
    const trimmedMessage = rawMessage.trim();
    if (!trimmedMessage) {
      return;
    }

    const pendingId = Date.now() + 1;
    setMessage('');
    setIsSending(true);
    setMessages((current) => [
      ...current,
      { id: Date.now(), role: 'staff', text: trimmedMessage },
      { id: pendingId, role: 'assistant', text: 'Savi is checking live workspace context...', aiBacked: false, isPending: true },
    ]);

    try {
      const response = await sendAiChatMessage({
        message: trimmedMessage,
        actorName: activeStaff?.displayName,
        actorRole: activeStaff?.role,
      });
      response.suggestedActions.forEach(onAction);
      setMessages((current) =>
        current.map((item) => item.id === pendingId ? {
          ...item,
          role: 'assistant',
          text: response.message,
          aiBacked: response.aiBacked,
          isPending: false,
        } : item),
      );
    } catch {
      const fallback = localAgentFallback(trimmedMessage);
      fallback.actions.forEach(onAction);
      setMessages((current) =>
        current.map((item) => item.id === pendingId ? {
          ...item,
          role: 'assistant',
          text: fallback.text,
          aiBacked: false,
          isPending: false,
        } : item),
      );
    } finally {
      setIsSending(false);
    }
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void sendMessage(message);
  };

  return (
    <div className={embedded ? 'w-full min-w-0' : 'fixed bottom-5 right-5 z-40'}>
      {isOpen ? (
        <section className={`flex min-w-0 flex-col overflow-hidden rounded-lg border border-sky-200 bg-white shadow-2xl ${embedded ? 'h-[min(38rem,calc(100vh-8rem))] min-h-[30rem] w-full' : 'h-[32rem] w-[22rem] max-w-[calc(100vw-2.5rem)]'}`}>
          <header className="flex items-center justify-between border-b border-sky-100 px-4 py-3">
            <div className="flex min-w-0 items-center gap-2">
              <span className="flex h-8 w-8 items-center justify-center rounded-md bg-slate-950 text-white">
                <Bot size={17} aria-hidden="true" />
              </span>
              <div className="min-w-0">
                <p className="text-sm font-semibold text-slate-950">Healthcare AI Agent</p>
                <p className="text-xs text-slate-500">Live hospital context</p>
              </div>
            </div>
            {embedded ? null : (
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                className="flex h-8 w-8 items-center justify-center rounded-md text-slate-500 transition hover:bg-sky-50 hover:text-slate-950"
                aria-label="Close AI chat"
              >
                <X size={16} aria-hidden="true" />
              </button>
            )}
          </header>

          <div className="flex-1 space-y-3 overflow-y-auto p-4">
            {messages.map((item) => (
              <div
                key={item.id}
                className={`animate-message-in max-w-full rounded-lg px-3 py-2 text-sm ${
                  item.role === 'staff'
                    ? 'ml-8 bg-slate-950 text-white'
                    : 'mr-8 border border-emerald-100 bg-emerald-50 text-slate-800'
                }`}
              >
                <MessageText text={item.text} />
                {item.role === 'assistant' ? (
                  <p className="mt-2 inline-flex items-center gap-1.5 text-[11px] font-medium uppercase tracking-normal opacity-60">
                    {item.isPending ? <Loader2 size={12} className="animate-spin" aria-hidden="true" /> : null}
                    {item.isPending ? 'Working' : item.aiBacked ? 'LLM context answer' : 'Workflow fallback'}
                  </p>
                ) : null}
              </div>
            ))}
            {!hasStaffMessages ? (
              <div className="rounded-lg border border-slate-200 bg-white p-3 shadow-sm">
                <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-normal text-slate-500">
                  <ClipboardList size={14} aria-hidden="true" />
                  Try asking
                </div>
                <div className="mt-2 grid gap-2 sm:grid-cols-2">
                  {exampleQueries.map((query) => (
                    <button
                      key={query}
                      type="button"
                      disabled={isSending}
                      onClick={() => void sendMessage(query)}
                      className="min-w-0 rounded-md border border-slate-200 bg-slate-50 px-2.5 py-2 text-left text-xs font-medium leading-5 text-slate-700 transition hover:border-emerald-300 hover:bg-emerald-50 hover:text-slate-950 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {query}
                    </button>
                  ))}
                </div>
              </div>
            ) : null}
          </div>

          <form onSubmit={handleSubmit} className="border-t border-sky-100 p-3">
            <div className="flex gap-2">
              <input
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                className="input-field mt-0"
                placeholder="Ask about a patient, doctor, bed, booking, or queue action"
              />
              <button
                type="submit"
                disabled={isSending}
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-slate-950 text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                aria-label="Send AI message"
              >
                <Send size={16} aria-hidden="true" />
              </button>
            </div>
          </form>
        </section>
      ) : (
        <button
          type="button"
          onClick={() => setIsOpen(true)}
          className="flex h-14 w-14 items-center justify-center rounded-full bg-slate-950 text-white shadow-2xl transition hover:scale-105"
          aria-label="Open AI agent chat"
        >
          <Sparkles size={22} aria-hidden="true" />
        </button>
      )}
    </div>
  );
}

function MessageText({ text }: { text: string }) {
  const lines = text.split('\n').filter((line, index, allLines) => line.trim() || index < allLines.length - 1);

  return (
    <div className="space-y-1.5 break-words leading-6">
      {lines.map((line, index) => {
        const trimmed = line.trim();
        if (!trimmed) {
          return <div key={`${line}-${index}`} className="h-1" />;
        }
        const bulletText = trimmed.replace(/^[-*]\s*/, '');
        const labelMatch = bulletText.match(/^([^:]{2,34}):\s*(.*)$/);

        return (
          <p key={`${line}-${index}`} className={trimmed.match(/^[-*]\s*/) ? 'flex gap-2' : ''}>
            {trimmed.match(/^[-*]\s*/) ? <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-current opacity-50" /> : null}
            <span className="min-w-0">
              {labelMatch ? (
                <>
                  <span className="font-semibold">{labelMatch[1]}:</span>
                  {labelMatch[2] ? ` ${labelMatch[2]}` : ''}
                </>
              ) : (
                bulletText
              )}
            </span>
          </p>
        );
      })}
    </div>
  );
}
