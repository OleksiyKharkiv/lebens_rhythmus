import type { IsoDayOfWeek, RecurrenceDay } from './api';
import { getLocale } from '$lib/paraglide/runtime';

const ISO_WEEKDAY_TO_JS: Record<IsoDayOfWeek, number> = {
	SUNDAY: 0,
	MONDAY: 1,
	TUESDAY: 2,
	WEDNESDAY: 3,
	THURSDAY: 4,
	FRIDAY: 5,
	SATURDAY: 6
};

// Mirrors SessionService.generateSessionsFromRecurrence's date-iteration
// logic (backend, Java) — every date from start to end inclusive whose
// weekday matches a selected day counts as one session. Shared between the
// admin course form (live preview while picking weekdays) and the public
// course page (2026-08-14) — a single implementation so the two counts
// can't drift apart.
export function countSessions(startDate: string, endDate: string, days: RecurrenceDay[]): number {
	if (!startDate || !endDate || days.length === 0) return 0;
	const start = new Date(`${startDate}T00:00:00Z`);
	const end = new Date(`${endDate}T00:00:00Z`);
	if (end.getTime() < start.getTime()) return 0;
	const selected = new Set(days.map((d) => ISO_WEEKDAY_TO_JS[d.dayOfWeek]));
	let count = 0;
	for (let d = new Date(start); d.getTime() <= end.getTime(); d.setUTCDate(d.getUTCDate() + 1)) {
		if (selected.has(d.getUTCDay())) count++;
	}
	return count;
}

// LR-103 — formats ISO date according to the active or specified locale
export function formatDate(iso: string, locale?: string): string {
	const currentLocale = locale ?? getLocale();
	const intlLocale = currentLocale === 'uk' ? 'uk-UA' : currentLocale === 'en' ? 'en-US' : 'de-DE';
	return new Date(`${iso}T00:00:00Z`).toLocaleDateString(intlLocale, {
		day: '2-digit',
		month: '2-digit',
		year: 'numeric',
		timeZone: 'UTC'
	});
}

export function formatDateDE(iso: string): string {
	return formatDate(iso, 'de');
}
