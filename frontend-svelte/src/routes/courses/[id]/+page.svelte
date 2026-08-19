<script lang="ts">
	import { page } from '$app/state';
	import * as m from '$lib/paraglide/messages.js';
	import { getCourse, isAuthenticated, type CourseDetail, type EnrollmentDTO } from '$lib/api';
	import { countSessions, formatDateDE } from '$lib/scheduleUtils';
	import EnrollButton from '$lib/components/EnrollButton.svelte';
	import ErrorText from '$lib/components/ErrorText.svelte';

	let course = $state<CourseDetail | null>(null);
	let error = $state(false);
	let enrollError = $state('');
	let enrollResult = $state<EnrollmentDTO | null>(null);

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

	// Recomputed client-side rather than fetched from Group directly, since
	// GET /groups requires auth and this page is public (2026-08-14).
	const sessionCount = $derived(
		course?.scheduleStartDate && course?.scheduleEndDate && course?.scheduleDays?.length
			? countSessions(course.scheduleStartDate, course.scheduleEndDate, course.scheduleDays)
			: null
	);

	function handleEnrollSuccess(result: EnrollmentDTO) {
		enrollError = '';
		enrollResult = result;
	}

	function handleEnrollError(message: string) {
		enrollResult = null;
		enrollError = message;
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {course?.titleDe ?? m.courses_title()}</title>
</svelte:head>

<section class="mx-auto max-w-4xl px-6 py-16 sm:py-24">
	{#if error}
		<p class="text-error">{m.state_error()}</p>
	{:else if course === null}
		<p class="text-paper-dim">{m.state_loading()}</p>
	{:else}
		<h1 class="page-title font-display font-semibold text-paper">{course.titleDe}</h1>

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
				<p class="whitespace-pre-line text-lg leading-relaxed text-paper-dim">{course.descriptionDe}</p>
			{/if}
		</div>

		<!-- Duration, before price per beta-test feedback 2026-08-14 — two
		     lines: date range, then computed session count. -->
		{#if course.scheduleStartDate && course.scheduleEndDate && sessionCount !== null}
			<div class="mt-6">
				<p class="text-paper">
					{m.courses_duration_label()} {m.courses_duration_from()} {formatDateDE(course.scheduleStartDate)}
					{m.courses_duration_to()} {formatDateDE(course.scheduleEndDate)}
				</p>
				<p class="text-paper">{sessionCount} {m.courses_sessions_suffix()}</p>
			</div>
		{/if}

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

		<!-- LR-084 — registration. No pre-emptive "full" check here (unlike
		     Workshop): CourseDetailDTO doesn't expose the linked Group's
		     capacity/enrolledCount, only its schedule (needed for the
		     duration display above) — a full course surfaces as an error
		     from the enroll call itself rather than a disabled button. -->
		<div class="mt-6">
			<h2 class="font-display text-lg font-semibold text-paper">{m.courses_enroll_title()}</h2>
			{#if enrollResult}
				{#if enrollResult.status === 'PENDING' && enrollResult.orderAmount != null}
					<p class="mt-3 text-success">
						{m.enroll_pending_label()} {enrollResult.orderAmount} {enrollResult.orderCurrency}
					</p>
				{:else}
					<p class="mt-3 text-success">{m.workshop_detail_enroll_success()}</p>
				{/if}
			{:else}
				<ErrorText message={enrollError} />
				{#if course.scheduleStartDate}
					<div class="mt-3">
						<EnrollButton
							targetType="course"
							targetId={course.id}
							label={isAuthenticated() ? m.workshops_enroll() : m.workshops_enroll_login_first()}
							onSuccess={handleEnrollSuccess}
							onError={handleEnrollError}
						/>
					</div>
				{:else}
					<p class="mt-3 text-paper-dim">{m.courses_no_schedule_yet()}</p>
				{/if}
			{/if}
		</div>

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
