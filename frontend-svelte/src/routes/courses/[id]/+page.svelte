<script lang="ts">
	import { page } from '$app/state';
	import * as m from '$lib/paraglide/messages.js';
	import { getCourse, type CourseDetail } from '$lib/api';

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
