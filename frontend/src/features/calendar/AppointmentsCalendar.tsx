import { CalendarDays } from 'lucide-react';
import type { Appointment } from '../../types/careflow';

interface AppointmentsCalendarProps {
  appointments: Appointment[];
}

function startOfDay(date: Date) {
  const copy = new Date(date);
  copy.setHours(0, 0, 0, 0);
  return copy;
}

export function AppointmentsCalendar({ appointments }: AppointmentsCalendarProps) {
  const today = startOfDay(new Date());
  const days = Array.from({ length: 14 }).map((_, index) => {
    const date = new Date(today);
    date.setDate(today.getDate() + index);
    return date;
  });

  return (
    <section className="py-6">
      <div className="border-b border-sky-100 pb-5">
        <p className="text-sm font-medium text-sky-700">Appointments</p>
        <h2 className="mt-1 text-2xl font-semibold text-slate-950">Patient calendar</h2>
      </div>

      <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {days.map((day) => {
          const dayAppointments = appointments.filter((appointment) => startOfDay(new Date(appointment.startsAt)).getTime() === day.getTime());
          return (
            <article key={day.toISOString()} className="min-h-40 rounded-lg border border-sky-100 bg-white p-4 shadow-sm">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-slate-950">{day.toLocaleDateString(undefined, { weekday: 'short' })}</p>
                  <p className="text-xs text-slate-500">{day.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</p>
                </div>
                <span className="inline-flex h-8 w-8 items-center justify-center rounded-md bg-sky-50 text-sky-700">
                  <CalendarDays size={16} aria-hidden="true" />
                </span>
              </div>
              <div className="mt-4 space-y-2">
                {dayAppointments.length === 0 ? (
                  <p className="rounded-md border border-dashed border-slate-200 p-3 text-xs text-slate-500">No marked visits</p>
                ) : (
                  dayAppointments.map((appointment) => (
                    <div key={appointment.id} className="rounded-md bg-emerald-50 p-3 text-xs text-emerald-900">
                      <p className="font-semibold">{appointment.patientDisplayId}</p>
                      <p>{new Date(appointment.startsAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - {appointment.note}</p>
                    </div>
                  ))
                )}
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}
