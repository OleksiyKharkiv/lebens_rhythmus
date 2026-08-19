<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import { getCourses, isAuthenticated, type CourseListItem, type EnrollmentDTO } from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import EnrollButton from '$lib/components/EnrollButton.svelte';
	import ErrorText from '$lib/components/ErrorText.svelte';

	let courses = $state<CourseListItem[] | null>(null);
	let error = $state(false);

	// Keyed by course id — one list, many cards, each with its own
	// independent registration outcome (same EnrollButton used on
	// courses/[id], just per-card here instead of a single course).
	let enrollResults = $state<Record<number, EnrollmentDTO>>({});
	let enrollErrors = $state<Record<number, string>>({});

	$effect(() => {
		getCourses()
			.then((data) => (courses = data))
			.catch(() => (error = true));
	});

	function handleEnrollSuccess(courseId: number, result: EnrollmentDTO) {
		delete enrollErrors[courseId];
		enrollResults[courseId] = result;
	}

	function handleEnrollError(courseId: number, message: string) {
		delete enrollResults[courseId];
		enrollErrors[courseId] = message;
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.courses_title()}</title>
</svelte:head>

<section class="mx-auto max-w-4xl px-6 py-16 sm:py-24">
	<h1 class="page-title font-display font-semibold text-paper">{m.courses_title()}</h1>

	{#if error}
		<p class="mt-8 text-error">{m.state_error()}</p>
	{:else if courses === null}
		<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
	{:else if courses.length === 0}
		<p class="mt-8 text-paper-dim">{m.courses_no_upcoming()}</p>
	{:else}
		<div class="mt-10 grid gap-6 sm:grid-cols-2">
			{#each courses as c (c.id)}
				<Card>
					<h2 class="list-card-title font-display font-semibold text-paper">{c.titleDe}</h2>
					{#if c.shortDescriptionDe}<p class="mt-2 text-sm text-paper-dim">{c.shortDescriptionDe}</p>{/if}
					{#if c.teacher}
						<p class="mt-2 text-sm text-paper-dim">
							<span class="font-semibold text-paper">Kursleitung:</span> {c.teacher.firstName} {c.teacher.lastName}
						</p>
					{/if}
					{#if c.price != null || c.priceDescription}
						<div class="mt-2">
							<p class="text-sm font-semibold text-paper">
								{c.price != null ? `${c.price} €` : m.activities_price_on_request()}
							</p>
							{#if c.priceDescription}
								<p class="mt-1 whitespace-pre-line text-xs text-paper-dim">{c.priceDescription}</p>
							{/if}
						</div>
					{/if}
					<div class="mt-4 flex items-center justify-between gap-2">
						<a
							href={`/courses/${c.id}`}
							class="rounded-full border border-ink-line px-4 py-2 text-sm text-paper transition-colors hover:border-gold"
						>
							{m.workshops_details()}
						</a>
						{#if c.status === 'PUBLISHED'}
							<EnrollButton
								targetType="course"
								targetId={c.id}
								label={isAuthenticated() ? m.workshops_enroll() : m.workshops_enroll_login_first()}
								onSuccess={(result) => handleEnrollSuccess(c.id, result)}
								onError={(message) => handleEnrollError(c.id, message)}
							/>
						{/if}
					</div>
					{#if enrollResults[c.id]}
						{#if enrollResults[c.id].status === 'PENDING' && enrollResults[c.id].orderAmount != null}
							<p class="mt-2 text-sm text-success">
								{m.enroll_pending_label()} {enrollResults[c.id].orderAmount} {enrollResults[c.id].orderCurrency}
							</p>
						{:else}
							<p class="mt-2 text-sm text-success">{m.workshop_detail_enroll_success()}</p>
						{/if}
					{:else if enrollErrors[c.id]}
						<div class="mt-2"><ErrorText message={enrollErrors[c.id]} /></div>
					{/if}
				</Card>
			{/each}
		</div>
	{/if}
</section>
