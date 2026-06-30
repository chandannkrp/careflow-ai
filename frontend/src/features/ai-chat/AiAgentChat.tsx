import { Bot, Send, Sparkles, X } from 'lucide-react';
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
}

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
      text: 'Open the intake route and capture arrival mode, complaint, symptoms, vitals, distress, risk flags, department, and staff notes.',
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

export function AiAgentChat({ activeStaff, onAction }: AiAgentChatProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 1,
      role: 'assistant',
      text: 'I can help with intake completeness, queue visibility, wait-time questions, and operational next steps.',
      aiBacked: false,
    },
  ]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedMessage = message.trim();
    if (!trimmedMessage) {
      return;
    }

    const pendingId = Date.now() + 1;
    setMessage('');
    setIsSending(true);
    setMessages((current) => [
      ...current,
      { id: Date.now(), role: 'staff', text: trimmedMessage },
      { id: pendingId, role: 'assistant', text: 'Checking queue context...', aiBacked: false, isPending: true },
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

  return (
    <div className="fixed bottom-5 right-5 z-40">
      {isOpen ? (
        <section className="flex h-[32rem] w-[22rem] max-w-[calc(100vw-2.5rem)] flex-col overflow-hidden rounded-lg border border-sky-200 bg-white shadow-2xl">
          <header className="flex items-center justify-between border-b border-sky-100 px-4 py-3">
            <div className="flex items-center gap-2">
              <span className="flex h-8 w-8 items-center justify-center rounded-md bg-slate-950 text-white">
                <Bot size={17} aria-hidden="true" />
              </span>
              <div>
                <p className="text-sm font-semibold text-slate-950">AI Agent</p>
                <p className="text-xs text-slate-500">Operations support with fallback</p>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setIsOpen(false)}
              className="flex h-8 w-8 items-center justify-center rounded-md text-slate-500 transition hover:bg-sky-50 hover:text-slate-950"
              aria-label="Close AI chat"
            >
              <X size={16} aria-hidden="true" />
            </button>
          </header>

          <div className="flex-1 space-y-3 overflow-y-auto p-4">
            {messages.map((item) => (
              <div
                key={item.id}
                className={`rounded-lg px-3 py-2 text-sm ${
                  item.role === 'staff'
                    ? 'ml-8 bg-slate-950 text-white'
                    : 'mr-8 border border-sky-100 bg-sky-50 text-slate-800'
                }`}
              >
                <p>{item.text}</p>
                {item.role === 'assistant' ? (
                  <p className="mt-2 text-[11px] font-medium uppercase tracking-normal opacity-60">
                    {item.isPending ? 'Working' : item.aiBacked ? 'OpenAI backed' : 'Workflow fallback'}
                  </p>
                ) : null}
              </div>
            ))}
          </div>

          <form onSubmit={handleSubmit} className="border-t border-sky-100 p-3">
            <div className="flex gap-2">
              <input
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                className="input-field mt-0"
                placeholder="Ask or request an action"
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
