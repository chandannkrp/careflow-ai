import { AlertCircle, ClipboardPlus, RotateCcw, Save } from 'lucide-react';
import { type ChangeEvent, type FormEvent, type ReactNode, useEffect, useMemo, useState } from 'react';
import { createIntake } from '../../api/client';
import type {
  AgeBand,
  ArrivalMode,
  CreateIntakeRequest,
  QueueStatus,
  RiskFlags,
  Vitals,
} from '../../types/careflow';

const ageBands: Array<{ value: AgeBand; label: string }> = [
  { value: 'ADULT', label: 'Adult' },
  { value: 'CHILD', label: 'Child' },
  { value: 'OLDER_ADULT', label: 'Senior citizen' },
];

const arrivalModes: Array<{ value: ArrivalMode; label: string }> = [
  { value: 'WALK_IN', label: 'Walk-in' },
  { value: 'AMBULANCE', label: 'Ambulance' },
  { value: 'TRANSFER', label: 'Transfer' },
  { value: 'REFERRAL', label: 'Referral' },
];

const intakeStatuses: Array<{ value: QueueStatus; label: string }> = [
  { value: 'WAITING', label: 'Waiting' },
  { value: 'IN_TRIAGE', label: 'In triage' },
];

const distressLevels = [
  { value: 0, label: 'None observed' },
  { value: 3, label: 'Mild distress' },
  { value: 6, label: 'Moderate distress' },
  { value: 9, label: 'Severe distress' },
];

const riskFlagOptions: Array<{ key: keyof RiskFlags; label: string }> = [
  { key: 'chestPain', label: 'Chest pain' },
  { key: 'breathingDifficulty', label: 'Breathing difficulty' },
  { key: 'alteredMentalState', label: 'Altered mental state' },
  { key: 'severeBleeding', label: 'Severe bleeding' },
  { key: 'pregnancy', label: 'Pregnancy' },
  { key: 'pediatricRisk', label: 'Pediatric risk' },
  { key: 'fallOrTrauma', label: 'Fall or trauma' },
  { key: 'immunocompromised', label: 'Immunocompromised' },
];

const emptyRiskFlags: RiskFlags = {
  chestPain: false,
  breathingDifficulty: false,
  alteredMentalState: false,
  severeBleeding: false,
  pregnancy: false,
  pediatricRisk: false,
  fallOrTrauma: false,
  immunocompromised: false,
};

const defaultVitals: Vitals = {
  temperatureC: 36.8,
  heartRate: 82,
  systolicPressure: 120,
  diastolicPressure: 78,
  respiratoryRate: 16,
  oxygenSaturation: 98,
};

interface IntakeFormState {
  patientDisplayId: string;
  ageBand: AgeBand;
  arrivalTimestamp: string;
  arrivalMode: ArrivalMode;
  chiefComplaint: string;
  symptomNotes: string;
  structuredSymptoms: string;
  distressScore: number;
  vitals: Vitals;
  riskFlags: RiskFlags;
  department: string;
  currentStatus: QueueStatus;
  staffNotes: string;
  staffName: string;
}

interface IntakeFormProps {
  departments: string[];
  onCreated?: () => void;
}

function toDateTimeLocalValue(date: Date) {
  const timezoneOffsetMs = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - timezoneOffsetMs).toISOString().slice(0, 16);
}

function createInitialState(departments: string[]): IntakeFormState {
  return {
    patientDisplayId: '',
    ageBand: 'ADULT',
    arrivalTimestamp: toDateTimeLocalValue(new Date()),
    arrivalMode: 'WALK_IN',
    chiefComplaint: '',
    symptomNotes: '',
    structuredSymptoms: '',
    distressScore: 0,
    vitals: { ...defaultVitals },
    riskFlags: { ...emptyRiskFlags },
    department: departments[0] ?? 'Emergency',
    currentStatus: 'WAITING',
    staffNotes: '',
    staffName: 'INTAKE-01',
  };
}

function parseNumberInput(value: string) {
  if (value === '') {
    return undefined;
  }

  return Number(value);
}

function normalizeOptionalText(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

export function IntakeForm({ departments, onCreated }: IntakeFormProps) {
  const [form, setForm] = useState<IntakeFormState>(() => createInitialState(departments));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const structuredSymptoms = useMemo(
    () =>
      form.structuredSymptoms
        .split(',')
        .map((symptom) => symptom.trim())
        .filter(Boolean),
    [form.structuredSymptoms],
  );

  useEffect(() => {
    setForm((current) => {
      if (current.department || departments.length === 0) {
        return current;
      }

      return { ...current, department: departments[0] };
    });
  }, [departments]);

  const updateField = <TKey extends keyof IntakeFormState>(key: TKey, value: IntakeFormState[TKey]) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  const updateVitals = (key: keyof Vitals, value: string) => {
    setForm((current) => ({
      ...current,
      vitals: {
        ...current.vitals,
        [key]: parseNumberInput(value),
      },
    }));
  };

  const updateRiskFlag = (key: keyof RiskFlags, checked: boolean) => {
    setForm((current) => ({
      ...current,
      riskFlags: {
        ...current.riskFlags,
        [key]: checked,
      },
    }));
  };

  const resetForm = () => {
    setForm(createInitialState(departments));
    setError(null);
    setSuccessMessage(null);
  };

  const buildRequest = (): CreateIntakeRequest => ({
    patientDisplayId: form.patientDisplayId.trim(),
    ageBand: form.ageBand,
    arrivalTimestamp: new Date(form.arrivalTimestamp).toISOString(),
    arrivalMode: form.arrivalMode,
    chiefComplaint: form.chiefComplaint.trim(),
    symptomNotes: normalizeOptionalText(form.symptomNotes),
    structuredSymptoms,
    painLevel: form.distressScore,
    vitals: form.vitals,
    riskFlags: form.riskFlags,
    department: form.department,
    currentStatus: form.currentStatus,
    staffNotes: normalizeOptionalText(form.staffNotes),
    staffName: normalizeOptionalText(form.staffName),
  });

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSubmitting(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const created = await createIntake(buildRequest());
      setSuccessMessage(`${created.patientDisplayId} added to the queue.`);
      setForm(createInitialState(departments));
      onCreated?.();
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : 'Unable to create intake.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleTextChange =
    (key: keyof IntakeFormState) => (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      updateField(key, event.target.value as never);
    };

  return (
    <section aria-labelledby="intake-title" className="py-6">
      <form onSubmit={handleSubmit} className="border-b border-sky-100 pb-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p className="text-sm font-medium text-sky-700">Patient intake</p>
            <h2 id="intake-title" className="mt-1 text-2xl font-semibold text-slate-950">
              Register arrival
            </h2>
          </div>

          <div className="flex gap-2">
            <button
              type="button"
              onClick={resetForm}
              className="inline-flex h-10 items-center justify-center gap-2 rounded-md border border-sky-200 bg-white px-3 text-sm font-medium text-slate-800 shadow-sm transition hover:bg-sky-50"
            >
              <RotateCcw size={16} aria-hidden="true" />
              Reset
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-slate-950 px-3 text-sm font-medium text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <Save size={16} aria-hidden="true" />
              {isSubmitting ? 'Saving' : 'Save intake'}
            </button>
          </div>
        </div>

        {error ? (
          <div className="mt-5 flex items-start gap-3 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-800">
            <AlertCircle size={18} className="mt-0.5 shrink-0" aria-hidden="true" />
            <p>{error}</p>
          </div>
        ) : null}

        {successMessage ? (
          <div className="mt-5 flex items-start gap-3 rounded-md border border-sky-200 bg-sky-50 p-4 text-sm text-sky-900">
            <ClipboardPlus size={18} className="mt-0.5 shrink-0" aria-hidden="true" />
            <p>{successMessage}</p>
          </div>
        ) : null}

        <div className="mt-5 grid gap-4 lg:grid-cols-[1fr_1fr]">
          <FormBlock title="Patient and arrival">
            <div className="grid gap-3 sm:grid-cols-2">
              <TextInput label="Patient display ID" value={form.patientDisplayId} onChange={handleTextChange('patientDisplayId')} required placeholder="ER-1048" />

              <label className="text-sm font-medium text-slate-700">
                Department
                <select
                  required
                  value={form.department}
                  onChange={(event) => updateField('department', event.target.value)}
                  className="input-field"
                >
                  {departments.map((department) => (
                    <option key={department} value={department}>
                      {department}
                    </option>
                  ))}
                </select>
              </label>

              <SelectInput label="Age band" value={form.ageBand} onChange={(value) => updateField('ageBand', value as AgeBand)} options={ageBands} />
              <SelectInput label="Arrival mode" value={form.arrivalMode} onChange={(value) => updateField('arrivalMode', value as ArrivalMode)} options={arrivalModes} />

              <label className="text-sm font-medium text-slate-700">
                Arrival time
                <input required type="datetime-local" value={form.arrivalTimestamp} onChange={handleTextChange('arrivalTimestamp')} className="input-field" />
              </label>

              <SelectInput label="Initial status" value={form.currentStatus} onChange={(value) => updateField('currentStatus', value as QueueStatus)} options={intakeStatuses} />
            </div>
          </FormBlock>

          <FormBlock title="Presentation">
            <div className="grid gap-3">
              <TextInput label="Chief complaint" value={form.chiefComplaint} onChange={handleTextChange('chiefComplaint')} required placeholder="Chest pressure and nausea" />
              <TextInput label="Structured symptoms" value={form.structuredSymptoms} onChange={handleTextChange('structuredSymptoms')} placeholder="shortness of breath, nausea" />

              <label className="text-sm font-medium text-slate-700">
                Symptom notes
                <textarea value={form.symptomNotes} onChange={handleTextChange('symptomNotes')} rows={3} className="input-field h-auto resize-y py-2" />
              </label>

              <label className="text-sm font-medium text-slate-700">
                Clinical distress
                <select
                  value={form.distressScore}
                  onChange={(event) => updateField('distressScore', Number(event.target.value))}
                  className="input-field"
                >
                  {distressLevels.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </FormBlock>
        </div>

        <div className="mt-4 grid gap-4 lg:grid-cols-[1fr_1fr]">
          <FormBlock title="Vitals">
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              <VitalInput label="Temp C" value={form.vitals.temperatureC} onChange={(value) => updateVitals('temperatureC', value)} step="0.1" />
              <VitalInput label="Heart rate" value={form.vitals.heartRate} onChange={(value) => updateVitals('heartRate', value)} />
              <VitalInput label="Systolic" value={form.vitals.systolicPressure} onChange={(value) => updateVitals('systolicPressure', value)} />
              <VitalInput label="Diastolic" value={form.vitals.diastolicPressure} onChange={(value) => updateVitals('diastolicPressure', value)} />
              <VitalInput label="Resp rate" value={form.vitals.respiratoryRate} onChange={(value) => updateVitals('respiratoryRate', value)} />
              <VitalInput label="SpO2" value={form.vitals.oxygenSaturation} onChange={(value) => updateVitals('oxygenSaturation', value)} />
            </div>
          </FormBlock>

          <FormBlock title="Risk flags">
            <div className="grid gap-2 sm:grid-cols-2">
              {riskFlagOptions.map((option) => (
                <label
                  key={option.key}
                  className="flex min-h-10 items-center gap-2 rounded-md border border-sky-100 bg-sky-50 px-3 text-sm font-medium text-slate-700"
                >
                  <input
                    type="checkbox"
                    checked={form.riskFlags[option.key]}
                    onChange={(event) => updateRiskFlag(option.key, event.target.checked)}
                    className="h-4 w-4 rounded border-slate-300 accent-sky-600"
                  />
                  {option.label}
                </label>
              ))}
            </div>
          </FormBlock>
        </div>

        <FormBlock title="Staff" className="mt-4">
          <div className="grid gap-3 lg:grid-cols-[1fr_2fr]">
            <TextInput label="Staff name or code" value={form.staffName} onChange={handleTextChange('staffName')} placeholder="INTAKE-01" />
            <TextInput label="Staff notes" value={form.staffNotes} onChange={handleTextChange('staffNotes')} />
          </div>
        </FormBlock>
      </form>
    </section>
  );
}

interface FormBlockProps {
  title: string;
  className?: string;
  children: ReactNode;
}

function FormBlock({ title, className = '', children }: FormBlockProps) {
  return (
    <fieldset className={`rounded-lg border border-sky-100 bg-white p-4 shadow-sm ${className}`}>
      <legend className="px-1 text-sm font-semibold text-slate-900">{title}</legend>
      <div className="mt-3">{children}</div>
    </fieldset>
  );
}

interface TextInputProps {
  label: string;
  value: string;
  onChange: (event: ChangeEvent<HTMLInputElement>) => void;
  placeholder?: string;
  required?: boolean;
}

function TextInput({ label, value, onChange, placeholder, required }: TextInputProps) {
  return (
    <label className="text-sm font-medium text-slate-700">
      {label}
      <input required={required} value={value} onChange={onChange} placeholder={placeholder} className="input-field" />
    </label>
  );
}

interface SelectInputProps {
  label: string;
  value: string;
  options: Array<{ value: string; label: string }>;
  onChange: (value: string) => void;
}

function SelectInput({ label, value, options, onChange }: SelectInputProps) {
  return (
    <label className="text-sm font-medium text-slate-700">
      {label}
      <select value={value} onChange={(event) => onChange(event.target.value)} className="input-field">
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

interface VitalInputProps {
  label: string;
  value?: number;
  onChange: (value: string) => void;
  step?: string;
}

function VitalInput({ label, value, onChange, step = '1' }: VitalInputProps) {
  return (
    <label className="text-sm font-medium text-slate-700">
      {label}
      <input type="number" value={value ?? ''} onChange={(event) => onChange(event.target.value)} step={step} className="input-field" />
    </label>
  );
}
