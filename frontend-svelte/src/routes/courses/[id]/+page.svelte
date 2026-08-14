<script lang="ts">
	import { page } from '$app/state';
	import * as m from '$lib/paraglide/messages.js';
	import { getCourse, type CourseDetail, type IsoDayOfWeek, type RecurrenceDay } from '$lib/api';

	let course = $state<CourseDetail | null>(null);
	let error = $state(false);

	// SvelteKit guarantees params.id is set for a matched [id] route, same
	// reasoning as routes/workshops/[id]/+page.svelte.
	const courseId = $derived(page.params.id as string);

	function load() {
		getCourse(courseId)
			.then((data) => (course = data))
			.catch(() => (error = true));
	}

	$effect(() => {
		load();
	});

	// Mirrors SessionService.generateSessionsFromRecurrence's date-iteration
	// logic (backend, Java) — every date from start to end inclusive whose
	// weekday matches a selected day counts as one session. Recomputed here
	// rather than fetched, since GET /groups requires auth and this page is
	// public (2026-08-14).
	const ISO_WEEKDAY_TO_JS: Record<IsoDayOfWeek, number> = {
		SUNDAY: 0,
		MONDAY: 1,
		TUESDAY: 2,
		WEDNESDAY: 3,
		THURSDAY: 4,
		FRIDAY: 5,
		SATURDAY: 6
	};

	function countSessions(startDate: string, endDate: string, days: RecurrenceDay[]): number {
		const selected = new Set(days.map((d) => ISO_WEEKDAY_TO_JS[d.dayOfWeek]));
		const start = new Date(`${startDate}T00:00:00Z`);
		const end = new Date(`${endDate}T00:00:00Z`);
		let count = 0;
		for (let d = new Date(start); d.getTime() <= end.getTime(); d.setUTCDate(d.getUTCDate() + 1)) {
			if (selected.has(d.getUTCDay())) count++;
		}
		return count;
	}

	function formatDate(iso: string) {
		return new Date(`${iso}T00:00:00Z`).toLocaleDateString('de-DE', {
			day: '2-digit',
			month: '2-digit',
			year: 'numeric',
			timeZone: 'UTC'
		});
	}

	const sessionCount = $derived(
		course?.scheduleStartDate && course?.scheduleEndDate && course?.scheduleDays?.length
			? countSessions(course.scheduleStartDate, course.scheduleEndDate, course.scheduleDays)
			: null
	);
</script>

<svelte:head>
	<title>{m.site_name()} — {course?.titleDe ?? m.courses_title()}</title>
</svelte:head>

<section class="mx-auto max-w-3xl px-6 py-16 sm:py-24">
	{#if error}
		<p class="text-error">{m.state_error()}</p>
	{:else if course === null}
		<p class="text-paper-dim">{m.state_loading()}</p>
	{:else}
		<h1 class="font-display text-3xl font-semibold text-paper sm:text-4xl">{course.titleDe}</h1>

		<!-- backgroundImageUrl (added 2026-08-14) — falls back to the page's
		     normal background when unset, per spec. whitespace-pre-line
		     (same date) — plain HTML text collapses newlines/blank lines by
		     default; this preserves the admin's paragraph breaks without
		     needing a rich-text editor. -->
		<div
			class="mt-4 rounded-lg {course.backgroundImageUrl ? 'bg-cover bg-center p-6' : ''}"
			style={course.backgroundImageUrl ? `background-image: url('${course.backgroundImageUrl}')` : undefined}
		>
			{#if course.descriptionDe}
				<p class="whitespace-pre-line leading-relaxed text-paper-dim">{course.descriptionDe}</p>
			{/if}
		</div>

		{#if course.price != null || course.priceDescription}
			<div class="mt-6">
				{#if course.price != null}
					<p class="text-lg font-semibold text-paper">{course.price} €</p>
				{/if}
				{#if course.priceDescription}
					<p class="mt-1 whitespace-pre-line text-xs text-paper-dim">{course.priceDescription}</p>
				{/if}
			</div>
		{/if}

		<dl class="mt-6 grid grid-cols-2 gap-4 text-sm text-paper-dim">
			{#if course.teacher}
				<div>
					<dt class="font-semibold text-paper">Kursleitung</dt>
					<dd>{course.teacher.firstName} {course.teacher.lastName}</dd>
				</div>
			{/if}
			{#if course.ageGroupName}
				<div>
					<dt class="font-semibold text-paper">Altersgruppe</dt>
					<dd>{course.ageGroupName}</dd>
				</div>
			{/if}
			{#if course.scheduleStartDate && course.scheduleEndDate && sessionCount !== null}
				<div>
					<dt class="font-semibold text-paper">{m.courses_duration_label()}</dt>
					<dd>
						{formatDate(course.scheduleStartDate)} – {formatDate(course.scheduleEndDate)} · {sessionCount}
						{m.courses_sessions_suffix()}
					</dd>
				</div>
			{/if}
		</dl>

		<!-- Single source of the ZFU-compliance-sensitive format text
		     (docs/compliance/tlab29-zfu-compliance-brief.md) — rendered
		     as-is, not paraphrased per page, so the site stays consistent
		     with the letter to ZFU. -->
		{#if course.formatDisclaimerDe}
			<div class="mt-8 rounded-lg border border-ink-line bg-ink px-5 py-4">
				<p class="whitespace-pre-line text-sm leading-relaxed text-paper-dim">{course.formatDisclaimerDe}</p>
			</div>
		{/if}
	{/if}
</section>
