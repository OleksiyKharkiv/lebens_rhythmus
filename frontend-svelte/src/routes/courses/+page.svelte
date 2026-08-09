<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import { getCourses, type CourseListItem } from '$lib/api';
	import Card from '$lib/components/Card.svelte';

	let courses = $state<CourseListItem[] | null>(null);
	let error = $state(false);

	$effect(() => {
		getCourses()
			.then((data) => (courses = data))
			.catch(() => (error = true));
	});
</script>

<svelte:head>
	<title>{m.site_name()} — {m.courses_title()}</title>
</svelte:head>

<section class="mx-auto max-w-4xl px-6 py-16 sm:py-24">
	<h1 class="font-display text-3xl font-semibold text-paper sm:text-4xl">{m.courses_title()}</h1>

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
					<h2 class="font-display text-xl font-semibold text-paper">{c.titleDe}</h2>
					{#if c.shortDescriptionDe}<p class="mt-2 text-sm text-paper-dim">{c.shortDescriptionDe}</p>{/if}
					{#if c.teacher}
						<p class="mt-2 text-sm text-paper-dim">
							<span class="font-semibold text-paper">Kursleitung:</span> {c.teacher.firstName} {c.teacher.lastName}
						</p>
					{/if}
					<div class="mt-4 flex gap-2">
						<a
							href={`/courses/${c.id}`}
							class="rounded-full border border-ink-line px-4 py-2 text-sm text-paper transition-colors hover:border-gold"
						>
							{m.workshops_details()}
						</a>
					</div>
				</Card>
			{/each}
		</div>
	{/if}
</section>
