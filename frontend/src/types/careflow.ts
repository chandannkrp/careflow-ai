export type AgeBand = 'CHILD' | 'ADULT' | 'OLDER_ADULT';
export type ArrivalMode = 'WALK_IN' | 'AMBULANCE' | 'TRANSFER' | 'REFERRAL';
export type QueueStatus = 'WAITING' | 'IN_TRIAGE' | 'IN_TREATMENT' | 'DISCHARGED' | 'LEFT_WITHOUT_BEING_SEEN';
export type UrgencyCategory = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type StaffRole = 'INTAKE_STAFF' | 'DOCTOR' | 'TRIAGE_NURSE' | 'CHARGE_NURSE' | 'ADMIN';
export type QueueSortKey = 'backend' | 'urgencyScore' | 'waitingMinutes' | 'patientDisplayId' | 'department' | 'status';
export type SortDirection = 'asc' | 'desc';

export interface QueueFilters {
  category?: UrgencyCategory;
  department?: string;
  status?: QueueStatus;
}

export interface Vitals {
  temperatureC?: number;
  heartRate?: number;
  systolicPressure?: number;
  diastolicPressure?: number;
  respiratoryRate?: number;
  oxygenSaturation?: number;
}

export interface RiskFlags {
  chestPain: boolean;
  breathingDifficulty: boolean;
  alteredMentalState: boolean;
  severeBleeding: boolean;
  pregnancy: boolean;
  pediatricRisk: boolean;
  fallOrTrauma: boolean;
  immunocompromised: boolean;
}

export interface QueueEntry {
  patientId: string;
  intakeId: string;
  patientDisplayId: string;
  urgencyCategory: UrgencyCategory;
  urgencyScore: number;
  chiefComplaint: string;
  waitingMinutes: number;
  waitPriorityLevel: 'ON_TRACK' | 'NEAR_TARGET' | 'OVER_TARGET';
  waitThresholdExceeded: boolean;
  department: string;
  status: QueueStatus;
  staffEscalated: boolean;
  waitingSince: string;
}

export interface UrgencyAssessment {
  id: string;
  finalCategory: UrgencyCategory;
  finalScore: number;
  scoreFactors: string[];
  suggestedCategory: UrgencyCategory | null;
  suggestedScore: number | null;
  redFlagIndicators: string[];
  missingOrAmbiguousDetails: string[];
  structuredSymptomSummary: string | null;
  staffFacingExplanation: string | null;
  confidenceLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  assessedAt: string;
}

export interface IntakeResponse {
  intakeId: string;
  patientId: string;
  patientDisplayId: string;
  ageBand: AgeBand;
  arrivalTimestamp: string;
  arrivalMode: ArrivalMode;
  chiefComplaint: string;
  symptomNotes?: string;
  structuredSymptoms: string[];
  painLevel: number;
  vitals: Vitals;
  riskFlags: RiskFlags;
  department: string;
  currentStatus: QueueStatus;
  staffNotes?: string;
  assessment: UrgencyAssessment | null;
  createdAt: string;
}

export interface CreateIntakeRequest {
  patientDisplayId: string;
  ageBand: AgeBand;
  arrivalTimestamp?: string;
  arrivalMode: ArrivalMode;
  chiefComplaint: string;
  symptomNotes?: string;
  structuredSymptoms: string[];
  painLevel: number;
  vitals: Vitals;
  riskFlags: RiskFlags;
  department: string;
  currentStatus?: QueueStatus;
  staffNotes?: string;
  staffName?: string;
}

export interface UpdateQueueStatusRequest {
  status: QueueStatus;
  actorName?: string;
  actorRole: StaffRole;
}

export interface StaffUser {
  id: string;
  staffCode: string;
  displayName: string;
  role: StaffRole;
  department?: string;
  specialty?: string;
  active: boolean;
}

export interface SaveStaffUserRequest {
  displayName: string;
  staffCode?: string;
  role: StaffRole;
  department?: string;
  specialty?: string;
  active?: boolean;
}

export interface SystemAgent {
  id: string;
  code: string;
  name: string;
  taskType: string;
  description: string;
  instructions?: string;
  active: boolean;
  updatedAt: string;
}

export interface SaveSystemAgentRequest {
  code: string;
  name: string;
  taskType: string;
  description: string;
  instructions?: string;
  active: boolean;
}

export interface ThreadAttachment {
  id: string;
  fileName: string;
  fileType?: string;
  url: string;
}

export interface ThreadComment {
  id: string;
  patientId: string;
  intakeId: string;
  patientDisplayId: string;
  authorName: string;
  body: string;
  attachments: ThreadAttachment[];
  createdAt: string;
}

export interface CreateThreadCommentRequest {
  authorName: string;
  body: string;
  attachments: Array<{
    fileName: string;
    fileType?: string;
    url: string;
  }>;
}

export interface UpdatePlacementRequest {
  status: QueueStatus;
  department: string;
  actorName?: string;
  actorRole: StaffRole;
}

export interface QueueMetrics {
  currentQueueSize: number;
  criticalAndHighWaiting: number;
  patientsByUrgency: Record<UrgencyCategory, number>;
  averageWaitMinutesByUrgency: Record<UrgencyCategory, number>;
  longestWaitMinutesByUrgency: Record<UrgencyCategory, number>;
  overrideCount: number;
}

export interface AiChatRequest {
  message: string;
  actorName?: string;
  actorRole?: StaffRole;
}

export interface AiChatResponse {
  message: string;
  suggestedActions: string[];
  aiBacked: boolean;
  createdAt: string;
}

export interface PatientFlashcard {
  id: string;
  patientId: string;
  intakeId: string;
  patientDisplayId: string;
  assignedStaffName: string | null;
  audienceRole: StaffRole;
  department: string;
  title: string;
  summary: string;
  actionLabel: string;
  urgencyCategory: UrgencyCategory;
  urgencyScore: number;
  status: QueueStatus;
  resolved: boolean;
  resolvedBy: string | null;
  resolvedAt: string | null;
  updatedAt: string;
}

export interface PatientTimelineEvent {
  id: string;
  patientId: string;
  intakeId: string;
  patientDisplayId: string;
  actorName: string | null;
  department: string;
  eventType: string;
  title: string;
  description: string;
  source: 'AGENT' | 'STAFF';
  createdAt: string;
}

export interface AgentDashboard {
  flashcards: PatientFlashcard[];
  timeline: PatientTimelineEvent[];
}
