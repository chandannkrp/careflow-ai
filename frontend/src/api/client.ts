import type {
  AgentDashboard,
  AssignDoctorRequest,
  AiChatRequest,
  AiChatResponse,
  CreateThreadCommentRequest,
  CreateIntakeRequest,
  IntakeResponse,
  HospitalAllocation,
  HospitalChatMessage,
  KnowledgeDocument,
  PatientReportResponse,
  QueueMetrics,
  QueueEntry,
  QueueFilters,
  SaveStaffUserRequest,
  RemoveQueueEntryRequest,
  SaveSystemAgentRequest,
  StaffUser,
  SystemAgent,
  ThreadComment,
  UpdatePlacementRequest,
  UpdateQueueStatusRequest,
} from '../types/careflow';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

interface ApiRequestOptions extends RequestInit {
  timeoutMs?: number;
}

export async function apiRequest<TResponse>(path: string, init?: ApiRequestOptions): Promise<TResponse> {
  const { timeoutMs, ...requestInit } = init ?? {};
  const controller = timeoutMs ? new AbortController() : null;
  const timeoutId = controller
    ? window.setTimeout(() => controller.abort(), timeoutMs)
    : null;

  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        ...requestInit.headers,
      },
      ...requestInit,
      signal: controller?.signal ?? requestInit.signal,
    });

    if (!response.ok) {
      const body = await response.text().catch(() => '');
      throw new Error(body || `Request failed with status ${response.status}`);
    }

    if (response.status === 204) {
      return undefined as TResponse;
    }

    return response.json() as Promise<TResponse>;
  } finally {
    if (timeoutId) {
      window.clearTimeout(timeoutId);
    }
  }
}

export function getQueueEntries(filters: QueueFilters = {}) {
  const params = new URLSearchParams();

  if (filters.category) {
    params.set('category', filters.category);
  }

  if (filters.department) {
    params.set('department', filters.department);
  }

  if (filters.status) {
    params.set('status', filters.status);
  }

  const query = params.toString();
  return apiRequest<QueueEntry[]>(`/api/queue${query ? `?${query}` : ''}`);
}

export function createIntake(request: CreateIntakeRequest) {
  return apiRequest<IntakeResponse>('/api/intakes', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function getNextPatientDisplayId() {
  const response = await apiRequest<{ patientDisplayId: string }>('/api/intakes/next-patient-display-id');
  return response.patientDisplayId;
}

export function getIntake(intakeId: string) {
  return apiRequest<IntakeResponse>(`/api/intakes/${intakeId}`);
}

export function updateQueueStatus(patientId: string, request: UpdateQueueStatusRequest) {
  return apiRequest<QueueEntry>(`/api/queue/${patientId}/status`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function updateQueuePlacement(patientId: string, request: UpdatePlacementRequest) {
  return apiRequest<QueueEntry>(`/api/queue/${patientId}/placement`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function assignQueueDoctor(patientId: string, request: AssignDoctorRequest) {
  return apiRequest<QueueEntry>(`/api/queue/${patientId}/doctor`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function removeQueueEntry(patientId: string, request: RemoveQueueEntryRequest) {
  return apiRequest<void>(`/api/queue/${patientId}`, {
    method: 'DELETE',
    body: JSON.stringify(request),
  });
}

export function getDepartments() {
  return apiRequest<string[]>('/api/departments');
}

export function getStaffUser(staffUserId: string) {
  return apiRequest<StaffUser>(`/api/staff/${staffUserId}`);
}

export function getStaffUsers(filters: { role?: StaffUser['role']; department?: string } = {}) {
  const params = new URLSearchParams();
  if (filters.role) {
    params.set('role', filters.role);
  }
  if (filters.department) {
    params.set('department', filters.department);
  }
  const query = params.toString();
  return apiRequest<StaffUser[]>(`/api/staff${query ? `?${query}` : ''}`);
}

export function createStaffUser(request: SaveStaffUserRequest) {
  return apiRequest<StaffUser>('/api/staff', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function updateStaffUser(staffUserId: string, request: SaveStaffUserRequest & { staffCode: string; active: boolean }) {
  return apiRequest<StaffUser>(`/api/staff/${staffUserId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

export function deactivateStaffUser(staffUserId: string) {
  return apiRequest<void>(`/api/staff/${staffUserId}`, {
    method: 'DELETE',
  });
}

export function getSystemAgents() {
  return apiRequest<SystemAgent[]>('/api/agents');
}

export function createSystemAgent(request: SaveSystemAgentRequest) {
  return apiRequest<SystemAgent>('/api/agents', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function updateSystemAgent(agentId: string, request: SaveSystemAgentRequest) {
  return apiRequest<SystemAgent>(`/api/agents/${agentId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

export function toggleSystemAgent(agentId: string, active: boolean) {
  return apiRequest<SystemAgent>(`/api/agents/${agentId}/active`, {
    method: 'PATCH',
    body: JSON.stringify({ active }),
  });
}

export function getQueueMetrics() {
  return apiRequest<QueueMetrics>('/api/metrics/queue');
}

export function getHospitalAllocation() {
  return apiRequest<HospitalAllocation>('/api/allocation');
}

export function getAgentDashboard(staffLookup?: string, department?: string) {
  const params = new URLSearchParams();

  if (staffLookup) {
    params.set('staffLookup', staffLookup);
  }

  if (department) {
    params.set('department', department);
  }

  const query = params.toString();
  return apiRequest<AgentDashboard>(`/api/agent/dashboard${query ? `?${query}` : ''}`);
}

export function resolveFlashcard(flashcardId: string, staffName?: string) {
  return apiRequest(`/api/agent/flashcards/${flashcardId}/resolve`, {
    method: 'POST',
    body: JSON.stringify({ staffName }),
  });
}

export function getPatientThread(patientId: string) {
  return apiRequest<ThreadComment[]>(`/api/patients/${patientId}/thread`);
}

export function createThreadComment(intakeId: string, request: CreateThreadCommentRequest) {
  return apiRequest<ThreadComment>(`/api/intakes/${intakeId}/thread`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function sendAiChatMessage(request: AiChatRequest) {
  return apiRequest<AiChatResponse>('/api/ai/chat', {
    method: 'POST',
    body: JSON.stringify(request),
    timeoutMs: 45000,
  });
}

export function sendAiTestChatMessage(request: AiChatRequest) {
  return apiRequest<AiChatResponse>('/api/ai/test-chat', {
    method: 'POST',
    body: JSON.stringify(request),
    timeoutMs: 45000,
  });
}

export function generatePatientReport(intakeId: string) {
  return apiRequest<PatientReportResponse>(`/api/intakes/${intakeId}/report`, {
    method: 'POST',
    timeoutMs: 45000,
  });
}

export function getKnowledgeDocuments() {
  return apiRequest<KnowledgeDocument[]>('/api/knowledge');
}

export async function uploadKnowledgeDocument(file: File, title?: string) {
  const formData = new FormData();
  formData.append('file', file);
  if (title?.trim()) {
    formData.append('title', title.trim());
  }

  const response = await fetch(`${API_BASE_URL}/api/knowledge`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const body = await response.text().catch(() => '');
    throw new Error(body || `Request failed with status ${response.status}`);
  }

  return response.json() as Promise<KnowledgeDocument>;
}

export function getHospitalChatMessages() {
  return apiRequest<HospitalChatMessage[]>('/api/hospital-chat');
}

export function sendHospitalChatMessage(request: { authorName: string; authorRole: StaffUser['role']; body: string }) {
  return apiRequest<HospitalChatMessage[]>('/api/hospital-chat', {
    method: 'POST',
    body: JSON.stringify(request),
    timeoutMs: 45000,
  });
}
