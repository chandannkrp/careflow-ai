import {
  AlertCircle,
  ArrowDownAZ,
  ArrowUpAZ,
  Clock3,
  Filter,
  PlayCircle,
  RefreshCw,
  ShieldAlert,
  X,
} from 'lucide-react';
import type { ReactNode } from 'react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { createThreadComment, getIntake, getPatientThread, getQueueEntries, updateQueueStatus } from '../../api/client';
import type {
  IntakeResponse,
  QueueEntry,
  QueueFilters,
  QueueSortKey,
  QueueStatus,
  SortDirection,
  ThreadComment,
  UrgencyCategory,
} from '../../types/careflow';

const urgencyStyles: Record<UrgencyCategory, string> = {
  CRITICAL: 'bg-red-100 text-red-800 ring-red-200',
  HIGH: 'bg-amber-100 text-amber-800 ring-amber-200',
  MEDIUM: 'bg-sky-100 text-sky-800 ring-sky-200',
  LOW: 'bg-emerald-100 text-emerald-800 ring-emerald-200',
};

const urgencyOptions: Array<{ value: UrgencyCategory; label: string }> = [
  { value: 'CRITICAL', label: 'Critical' },
  { value: 'HIGH', label: 'High' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'LOW', label: 'Low' },
];

const statusOptions: Array<{ value: QueueStatus; label: string }> = [
  { value: 'WAITING', label: 'Waiting' },
  { value: 'IN_TRIAGE', label: 'In triage' },
  { value: 'IN_TREATMENT', label: 'In treatment' },
  { value: 'DISCHARGED', label: 'Discharged' },
  { value: 'LEFT_WITHOUT_BEING_SEEN', label: 'Left without being seen' },
];

const sortOptions: Array<{ value: QueueSortKey; label: string }> = [
  { value: 'backend', label: 'Queue rule' },
  { value: 'urgencyScore', label: 'Score' },
  { value: 'waitingMinutes', label: 'Wait time' },
  { value: 'patientDisplayId', label: 'Patient ID' },
  { value: 'department', label: 'Department' },
  { value: 'status', label: 'Status' },
];

const urgencyRank: Record<UrgencyCategory, number> = {
  CRITICAL: 0,
  HIGH: 1,
  MEDIUM: 2,
  LOW: 3,
};

const statusRank: Record<QueueStatus, number> = {
  WAITING: 0,
  IN_TRIAGE: 1,
  IN_TREATMENT: 2,
  DISCHARGED: 3,
  LEFT_WITHOUT_BEING_SEEN: 4,
};

function formatUrgency(category: UrgencyCategory) {
  return category.charAt(0) + category.slice(1).toLowerCase();
}

function formatEnumLabel(value: string) {
  return value
    .split('_')
    .map((part) => part.charAt(0) + part.slice(1).toLowerCase())
    .join(' ');
}

function formatWaitingTime(minutes: number) {
  if (minutes < 60) {
    return `${minutes} min`;
  }

  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  return `${hours}h ${remainingMinutes}m`;
}

function waitLabel(entry: QueueEntry) {
  if (entry.waitPriorityLevel === 'OVER_TARGET') {
    return '40m+ target';
  }
  if (entry.waitPriorityLevel === 'NEAR_TARGET') {
    return '30m+ target';
  }
  return null;
}

function compareText(first: string, second: string) {
  return first.localeCompare(second, undefined, { sensitivity: 'base' });
}

function sortEntries(entries: QueueEntry[], sortKey: QueueSortKey, sortDirection: SortDirection) {
  if (sortKey === 'backend') {
    return entries;
  }

  const direction = sortDirection === 'asc' ? 1 : -1;
  return [...entries].sort((first, second) => {
    let result = 0;

    if (sortKey === 'urgencyScore') {
      result =
        first.urgencyScore === second.urgencyScore
          ? urgencyRank[first.urgencyCategory] - urgencyRank[second.urgencyCategory]
          : first.urgencyScore - second.urgencyScore;
    } else if (sortKey === 'waitingMinutes') {
      result = first.waitingMinutes - second.waitingMinutes;
    } else if (sortKey === 'patientDisplayId') {
      result = compareText(first.patientDisplayId, second.patientDisplayId);
    } else if (sortKey === 'department') {
      result = compareText(first.department, second.department);
    } else if (sortKey === 'status') {
      result = statusRank[first.status] - statusRank[second.status];
    }

    if (result === 0) {
      result = compareText(first.patientDisplayId, second.patientDisplayId);
    }

    return result * direction;
  });
}

function entryMatchesFilters(entry: QueueEntry, filters: QueueFilters) {
  return (
    (!filters.category || entry.urgencyCategory === filters.category) &&
    (!filters.status || entry.status === filters.status) &&
    (!filters.department || entry.department.toLowerCase() === filters.department.toLowerCase())
  );
}

interface QueueTableProps {
  refreshSignal?: number;
  searchQuery: string;
  onStatusUpdated?: () => void;
}

export function QueueTable({ refreshSignal = 0, searchQuery, onStatusUpdated }: QueueTableProps) {
  const [entries, setEntries] = useState<QueueEntry[]>([]);
  const [filters, setFilters] = useState<QueueFilters>({});
  const [sortKey, setSortKey] = useState<QueueSortKey>('backend');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const [updatingPatientId, setUpdatingPatientId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedIntake, setSelectedIntake] = useState<IntakeResponse | null>(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  const loadQueue = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      setEntries(await getQueueEntries(filters));
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : 'Unable to load queue.');
    } finally {
      setIsLoading(false);
    }
  }, [filters]);

  const visibleEntries = useMemo(() => {
    const normalizedSearch = searchQuery.trim().toLowerCase();
    const searchedEntries =
      normalizedSearch.length === 0
        ? entries
        : entries.filter((entry) =>
            [
              entry.patientDisplayId,
              entry.chiefComplaint,
              entry.department,
              entry.status,
              entry.urgencyCategory,
            ].some((value) => value.toLowerCase().includes(normalizedSearch)),
          );

    return sortEntries(searchedEntries, sortKey, sortDirection);
  }, [entries, searchQuery, sortDirection, sortKey]);

  useEffect(() => {
    void loadQueue();
  }, [loadQueue, refreshSignal]);

  const updateFilter = <TKey extends keyof QueueFilters>(key: TKey, value: QueueFilters[TKey] | '') => {
    setFilters((current) => {
      const next = { ...current };
      if (value === '') {
        delete next[key];
      } else {
        next[key] = value;
      }
      return next;
    });
  };

  const resetFilters = () => {
    setFilters({});
    setSortKey('backend');
    setSortDirection('desc');
  };

  const handleSortKeyChange = (value: QueueSortKey) => {
    setSortKey(value);
    if (value === 'patientDisplayId' || value === 'department' || value === 'status') {
      setSortDirection('asc');
    } else {
      setSortDirection('desc');
    }
  };

  const handleStatusChange = async (entry: QueueEntry, status: QueueStatus) => {
    if (entry.status === status) {
      return;
    }

    setUpdatingPatientId(entry.patientId);
    setError(null);

    try {
      const updated = await updateQueueStatus(entry.patientId, {
        status,
        actorName: 'Demo Triage',
        actorRole: 'TRIAGE_NURSE',
      });
      setEntries((current) => {
        if (!entryMatchesFilters(updated, filters)) {
          return current.filter((item) => item.patientId !== updated.patientId);
        }

        return current.map((item) => (item.patientId === updated.patientId ? updated : item));
      });
      onStatusUpdated?.();
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : 'Unable to update queue status.');
    } finally {
      setUpdatingPatientId(null);
    }
  };

  const openIntakeDetail = async (entry: QueueEntry) => {
    setSelectedIntake(null);
    setDetailError(null);
    setIsDetailLoading(true);

    try {
      setSelectedIntake(await getIntake(entry.intakeId));
    } catch (caughtError) {
      setDetailError(caughtError instanceof Error ? caughtError.message : 'Unable to load intake details.');
    } finally {
      setIsDetailLoading(false);
    }
  };

  return (
    <section aria-labelledby="queue-title" className="py-6">
      <div className="flex flex-col gap-4 border-b border-sky-100 pb-5 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-sm font-medium text-sky-700">Patient queue</p>
          <h2 id="queue-title" className="mt-1 text-2xl font-semibold text-slate-950">
            Treatment priority
          </h2>
        </div>

        <button
          type="button"
          onClick={() => void loadQueue()}
          disabled={isLoading}
          className="inline-flex h-10 items-center justify-center gap-2 rounded-md border border-sky-200 bg-white px-3 text-sm font-medium text-slate-800 shadow-sm transition hover:bg-sky-50 disabled:cursor-not-allowed disabled:opacity-60"
        >
          <RefreshCw size={16} className={isLoading ? 'animate-spin' : ''} aria-hidden="true" />
          Refresh
        </button>
      </div>

      <div className="mt-5 grid gap-3 rounded-lg border border-sky-100 bg-white p-4 shadow-sm lg:grid-cols-[1fr_1fr_1fr_1fr_auto]">
        <label className="text-sm font-medium text-slate-700">
          <span className="inline-flex items-center gap-1.5">
            <Filter size={15} aria-hidden="true" />
            Urgency
          </span>
          <select
            value={filters.category ?? ''}
            onChange={(event) => updateFilter('category', event.target.value as UrgencyCategory | '')}
            className="input-field"
          >
            <option value="">All urgency</option>
            {urgencyOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label className="text-sm font-medium text-slate-700">
          Status
          <select
            value={filters.status ?? ''}
            onChange={(event) => updateFilter('status', event.target.value as QueueStatus | '')}
            className="input-field"
          >
            <option value="">All statuses</option>
            {statusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label className="text-sm font-medium text-slate-700">
          Department
          <input
            value={filters.department ?? ''}
            onChange={(event) => updateFilter('department', event.target.value)}
            className="input-field"
            placeholder="Emergency"
          />
        </label>

        <label className="text-sm font-medium text-slate-700">
          Sort
          <select
            value={sortKey}
            onChange={(event) => handleSortKeyChange(event.target.value as QueueSortKey)}
            className="input-field"
          >
            {sortOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <div className="flex items-end gap-2">
          <button
            type="button"
            onClick={() => setSortDirection((current) => (current === 'asc' ? 'desc' : 'asc'))}
            disabled={sortKey === 'backend'}
            className="inline-flex h-10 w-10 items-center justify-center rounded-md border border-sky-200 bg-white text-slate-800 shadow-sm transition hover:bg-sky-50 disabled:cursor-not-allowed disabled:opacity-50"
            title="Toggle sort direction"
          >
            {sortDirection === 'asc' ? <ArrowUpAZ size={16} aria-hidden="true" /> : <ArrowDownAZ size={16} aria-hidden="true" />}
          </button>
          <button
            type="button"
            onClick={resetFilters}
            className="inline-flex h-10 w-10 items-center justify-center rounded-md border border-sky-200 bg-white text-slate-800 shadow-sm transition hover:bg-sky-50"
            title="Reset filters"
          >
            <X size={16} aria-hidden="true" />
          </button>
        </div>
      </div>

      {error ? (
        <div className="mt-5 flex items-start gap-3 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          <AlertCircle size={18} className="mt-0.5 shrink-0" aria-hidden="true" />
          <p>{error}</p>
        </div>
      ) : null}

      <div className="mt-5 overflow-hidden rounded-lg border border-sky-100 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-sky-100 text-left text-sm">
            <thead className="bg-sky-50 text-xs font-semibold uppercase tracking-normal text-slate-600">
              <tr>
                <th scope="col" className="px-4 py-3">
                  Patient
                </th>
                <th scope="col" className="px-4 py-3">
                  Urgency
                </th>
                <th scope="col" className="px-4 py-3">
                  Complaint
                </th>
                <th scope="col" className="px-4 py-3">
                  Wait
                </th>
                <th scope="col" className="px-4 py-3">
                  Department
                </th>
                <th scope="col" className="px-4 py-3">
                  Status
                </th>
                <th scope="col" className="px-4 py-3">
                  Action
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-sky-50">
              {isLoading ? (
                Array.from({ length: 4 }).map((_, index) => (
                  <tr key={index} className="animate-pulse">
                    {Array.from({ length: 7 }).map((__, cellIndex) => (
                      <td key={cellIndex} className="px-4 py-4">
                        <div className="h-4 w-full max-w-32 rounded bg-sky-100" />
                      </td>
                    ))}
                  </tr>
                ))
              ) : visibleEntries.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center text-slate-500">
                    No patients are currently waiting in the treatment queue.
                  </td>
                </tr>
              ) : (
                visibleEntries.map((entry) => (
                  <tr
                    key={entry.patientId}
                    onClick={() => void openIntakeDetail(entry)}
                    className="cursor-pointer hover:bg-sky-50"
                  >
                    <td className="whitespace-nowrap px-4 py-4 font-medium text-slate-950">
                      <div className="flex items-center gap-2">
                        {entry.staffEscalated ? (
                          <ShieldAlert size={16} className="text-red-600" aria-label="Staff escalated" />
                        ) : null}
                        {entry.patientDisplayId}
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-4 py-4">
                      <span
                        className={`inline-flex min-w-24 justify-center rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset ${urgencyStyles[entry.urgencyCategory]}`}
                      >
                        {formatUrgency(entry.urgencyCategory)} - {entry.urgencyScore}
                      </span>
                    </td>
                    <td className="min-w-56 px-4 py-4 text-slate-700">{entry.chiefComplaint}</td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-700">
                      <div className="flex flex-col gap-1">
                        <span className="inline-flex items-center gap-1.5">
                          <Clock3 size={15} aria-hidden="true" />
                          {formatWaitingTime(entry.waitingMinutes)}
                        </span>
                        {waitLabel(entry) ? (
                          <span className="text-xs font-semibold text-amber-700">{waitLabel(entry)}</span>
                        ) : null}
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-700">{entry.department}</td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-700">
                      <select
                        value={entry.status}
                        disabled={updatingPatientId === entry.patientId}
                        onClick={(event) => event.stopPropagation()}
                        onChange={(event) => void handleStatusChange(entry, event.target.value as QueueStatus)}
                        className="h-9 min-w-44 rounded-md border border-sky-200 bg-white px-2 text-sm text-slate-800 shadow-sm focus:border-sky-500 focus:outline-none focus:ring-2 focus:ring-sky-100 disabled:cursor-not-allowed disabled:opacity-60"
                        aria-label={`Status for ${entry.patientDisplayId}`}
                      >
                        {statusOptions.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td className="whitespace-nowrap px-4 py-4">
                      <button
                        type="button"
                        onClick={(event) => {
                          event.stopPropagation();
                          void handleStatusChange(entry, 'IN_TREATMENT');
                        }}
                        disabled={
                          entry.status === 'IN_TREATMENT' ||
                          entry.status === 'DISCHARGED' ||
                          entry.status === 'LEFT_WITHOUT_BEING_SEEN' ||
                          updatingPatientId === entry.patientId
                        }
                        className="inline-flex h-9 items-center justify-center gap-2 rounded-md border border-sky-200 bg-white px-3 text-sm font-medium text-slate-800 shadow-sm transition hover:bg-sky-50 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        <PlayCircle size={15} aria-hidden="true" />
                        Start
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {isDetailLoading || selectedIntake || detailError ? (
        <IntakeDetailDialog
          intake={selectedIntake}
          isLoading={isDetailLoading}
          error={detailError}
          onClose={() => {
            setSelectedIntake(null);
            setDetailError(null);
            setIsDetailLoading(false);
          }}
        />
      ) : null}
    </section>
  );
}

function formatValue(value: string | number | undefined | null) {
  if (value === undefined || value === null || value === '') {
    return 'Not recorded';
  }
  return String(value);
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function activeRiskFlags(intake: IntakeResponse) {
  return Object.entries(intake.riskFlags)
    .filter(([, enabled]) => enabled)
    .map(([key]) => key.replace(/([A-Z])/g, ' $1').toLowerCase());
}

interface IntakeDetailDialogProps {
  intake: IntakeResponse | null;
  isLoading: boolean;
  error: string | null;
  onClose: () => void;
}

function IntakeDetailDialog({ intake, isLoading, error, onClose }: IntakeDetailDialogProps) {
  const [thread, setThread] = useState<ThreadComment[]>([]);
  const [authorName, setAuthorName] = useState('Care team');
  const [commentBody, setCommentBody] = useState('');
  const [attachmentName, setAttachmentName] = useState('');
  const [attachmentUrl, setAttachmentUrl] = useState('');
  const [threadError, setThreadError] = useState<string | null>(null);
  const [isThreadLoading, setIsThreadLoading] = useState(false);

  const loadThread = useCallback(async () => {
    if (!intake) {
      return;
    }
    setIsThreadLoading(true);
    setThreadError(null);
    try {
      setThread(await getPatientThread(intake.patientId));
    } catch (caughtError) {
      setThreadError(caughtError instanceof Error ? caughtError.message : 'Unable to load thread.');
    } finally {
      setIsThreadLoading(false);
    }
  }, [intake]);

  useEffect(() => {
    void loadThread();
  }, [loadThread]);

  const submitComment = async () => {
    if (!intake || commentBody.trim().length === 0) {
      return;
    }
    setThreadError(null);
    try {
      await createThreadComment(intake.intakeId, {
        authorName,
        body: commentBody,
        attachments:
          attachmentName.trim() && attachmentUrl.trim()
            ? [{ fileName: attachmentName.trim(), url: attachmentUrl.trim() }]
            : [],
      });
      setCommentBody('');
      setAttachmentName('');
      setAttachmentUrl('');
      await loadThread();
    } catch (caughtError) {
      setThreadError(caughtError instanceof Error ? caughtError.message : 'Unable to add comment.');
    }
  };

  return (
    <div className="fixed inset-0 z-40 bg-slate-950/45 p-3 backdrop-blur-sm sm:p-6" role="dialog" aria-modal="true">
      <div className="mx-auto flex h-full max-w-6xl flex-col overflow-hidden rounded-lg bg-white shadow-xl">
        <div className="flex items-center justify-between gap-3 border-b border-sky-100 px-5 py-4">
          <div>
            <p className="text-sm font-medium text-sky-700">Intake details</p>
            <h2 className="text-xl font-semibold text-slate-950">
              {intake ? intake.patientDisplayId : 'Loading patient'}
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="inline-flex h-10 w-10 items-center justify-center rounded-md border border-sky-200 bg-white text-slate-800 transition hover:bg-sky-50"
            aria-label="Close intake details"
          >
            <X size={18} aria-hidden="true" />
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto p-5">
          {isLoading ? (
            <div className="h-64 animate-pulse rounded-lg bg-sky-100" />
          ) : error ? (
            <div className="flex items-start gap-3 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-800">
              <AlertCircle size={18} className="mt-0.5 shrink-0" aria-hidden="true" />
              <p>{error}</p>
            </div>
          ) : intake ? (
            <div className="grid gap-4 xl:grid-cols-[1.05fr_0.95fr]">
              <DetailSection title="Arrival">
                <DetailGrid
                  items={[
                    ['Patient ID', intake.patientDisplayId],
                    ['Age band', formatEnumLabel(intake.ageBand)],
                    ['Arrival mode', formatEnumLabel(intake.arrivalMode)],
                    ['Arrival time', formatDateTime(intake.arrivalTimestamp)],
                    ['Department', intake.department],
                    ['Current status', formatEnumLabel(intake.currentStatus)],
                  ]}
                />
              </DetailSection>

              <DetailSection title="Presentation">
                <DetailGrid
                  items={[
                    ['Chief complaint', intake.chiefComplaint],
                    ['Pain/distress', intake.painLevel],
                    ['Symptoms', intake.structuredSymptoms.length > 0 ? intake.structuredSymptoms.join(', ') : 'Not recorded'],
                    ['Symptom notes', intake.symptomNotes ?? 'Not recorded'],
                    ['Staff notes', intake.staffNotes ?? 'Not recorded'],
                  ]}
                />
              </DetailSection>

              <DetailSection title="Vitals">
                <DetailGrid
                  items={[
                    ['Temp C', formatValue(intake.vitals.temperatureC)],
                    ['Heart rate', formatValue(intake.vitals.heartRate)],
                    ['Blood pressure', `${formatValue(intake.vitals.systolicPressure)}/${formatValue(intake.vitals.diastolicPressure)}`],
                    ['Resp rate', formatValue(intake.vitals.respiratoryRate)],
                    ['SpO2', formatValue(intake.vitals.oxygenSaturation)],
                  ]}
                />
              </DetailSection>

              <DetailSection title="Risk and assessment">
                <DetailGrid
                  items={[
                    ['Risk flags', activeRiskFlags(intake).length > 0 ? activeRiskFlags(intake).join(', ') : 'None selected'],
                    ['Final urgency', intake.assessment ? `${intake.assessment.finalCategory} - ${intake.assessment.finalScore}` : 'Not assessed'],
                    ['AI suggestion', intake.assessment?.suggestedCategory ? `${intake.assessment.suggestedCategory} - ${intake.assessment.suggestedScore}` : 'No suggestion'],
                    ['Score factors', intake.assessment?.scoreFactors.join(', ') ?? 'Not recorded'],
                    ['Explanation', intake.assessment?.staffFacingExplanation ?? 'Not recorded'],
                  ]}
                />
              </DetailSection>

              <DetailSection title="Care thread">
                <div className="space-y-3">
                  {threadError ? <p className="rounded-md bg-red-50 p-3 text-sm text-red-800">{threadError}</p> : null}
                  {isThreadLoading ? (
                    <div className="h-24 animate-pulse rounded-md bg-sky-100" />
                  ) : thread.length === 0 ? (
                    <p className="rounded-md bg-white p-3 text-sm text-slate-500">No care-team comments yet.</p>
                  ) : (
                    thread.map((comment) => (
                      <article key={comment.id} className="rounded-md bg-white p-3">
                        <div className="flex items-center justify-between gap-3">
                          <p className="text-sm font-semibold text-slate-950">{comment.authorName}</p>
                          <p className="text-xs text-slate-500">{formatDateTime(comment.createdAt)}</p>
                        </div>
                        <p className="mt-2 text-sm leading-6 text-slate-700">{comment.body}</p>
                        {comment.attachments.length > 0 ? (
                          <div className="mt-2 flex flex-wrap gap-2">
                            {comment.attachments.map((attachment) => (
                              <a
                                key={attachment.id}
                                href={attachment.url}
                                target="_blank"
                                rel="noreferrer"
                                className="rounded-md bg-sky-50 px-2 py-1 text-xs font-semibold text-sky-800"
                              >
                                {attachment.fileName}
                              </a>
                            ))}
                          </div>
                        ) : null}
                      </article>
                    ))
                  )}

                  <div className="rounded-md bg-white p-3">
                    <div className="grid gap-2 sm:grid-cols-2">
                      <input
                        value={authorName}
                        onChange={(event) => setAuthorName(event.target.value)}
                        className="input-field"
                        placeholder="Author"
                      />
                      <input
                        value={attachmentName}
                        onChange={(event) => setAttachmentName(event.target.value)}
                        className="input-field"
                        placeholder="Attachment name"
                      />
                      <input
                        value={attachmentUrl}
                        onChange={(event) => setAttachmentUrl(event.target.value)}
                        className="input-field sm:col-span-2"
                        placeholder="Attachment URL"
                      />
                    </div>
                    <textarea
                      value={commentBody}
                      onChange={(event) => setCommentBody(event.target.value)}
                      className="input-field mt-2 h-24 resize-y py-2"
                      placeholder="Add a care-team update"
                    />
                    <button
                      type="button"
                      onClick={() => void submitComment()}
                      className="mt-2 inline-flex h-9 items-center rounded-md bg-slate-950 px-3 text-sm font-medium text-white transition hover:bg-slate-800"
                    >
                      Add comment
                    </button>
                  </div>
                </div>
              </DetailSection>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}

function DetailSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-sky-100 bg-sky-50 p-4">
      <h3 className="text-sm font-semibold text-slate-950">{title}</h3>
      <div className="mt-3">{children}</div>
    </section>
  );
}

function DetailGrid({ items }: { items: Array<[string, string | number]> }) {
  return (
    <dl className="grid gap-3 sm:grid-cols-2">
      {items.map(([label, value]) => (
        <div key={label} className="rounded-md bg-white p-3">
          <dt className="text-xs font-semibold uppercase tracking-normal text-slate-500">{label}</dt>
          <dd className="mt-1 text-sm leading-6 text-slate-800">{value}</dd>
        </div>
      ))}
    </dl>
  );
}
