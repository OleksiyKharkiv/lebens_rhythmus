<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import { getWorkshops, isAuthenticated, type WorkshopListItem, type EnrollmentDTO } from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import EnrollButton from '$lib/components/EnrollButton.svelte';
	import ErrorText from '$lib/components/ErrorText.svelte';

	let workshops = $state<WorkshopListItem[] | null>(null);
	let error = $state(false);

	// Keyed by workshop id — same per-card pattern as courses/+page.svelte.
	let enrollResults = $state<Record<number, EnrollmentDTO>>({});
	let enrollErrors = $state<Record<number, string>>({});

	$effect(() => {
		getWorkshops(true)
			.then((data) => (workshops = data))
			.catch(() => (error = true));
	});

	function formatDate(d: string | null) {
		if (!d) return null;
		return new Date(d).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' });
	}

	// A Workshop can have multiple Groups (sessions/dates), unlike Course's
	// single-Group MVP shape — only auto-resolve a direct enroll target
	// when there's exactly one active one; otherwise there's no session to
	// default to, so the list card sends the user to the detail page to
	// actually pick a date instead of silently guessing.
	function soleActiveGroup(w: WorkshopListItem) {
		const active = w.groups.filter((g) => g.active);
		return active.length === 1 ? active[0] : null;
	}

	function handleEnrollSuccess(workshopId: number, result: EnrollmentDTO) {
		delete enrollErrors[workshopId];
		enrollResults[workshopId] = result;
	}

	function handleEnrollError(workshopId: number, message: string) {
		delete enrollResults[workshopId];
		enrollErrors[workshopId] = message;
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.workshops_title()}</title>
</svelte:head>

<section class="mx-auto max-w-4xl px-6 py-16 sm:py-24">
	<h1 class="page-title font-display font-semibold text-paper">{m.workshops_title()}</h1>

	{#if error}
		<p class="mt-8 text-error">{m.state_error()}</p>
	{:else if workshops === null}
		<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
	{:else if workshops.length === 0}
		<p class="mt-8 text-paper-dim">{m.workshops_no_upcoming()}</p>
	{:else}
		<div class="mt-10 grid gap-6 sm:grid-cols-2">
			{#each workshops as w (w.id)}
				<Card>
					<h2 class="list-card-title font-display font-semibold text-paper">{w.title}</h2>
					{#if w.shortDescription}<p class="lead-text mt-2 text-paper-dim">{w.shortDescription}</p>{/if}
					<dl class="mt-4 space-y-1 text-sm text-paper-dim">
						{#if w.startDate}
							<div><dt class="inline font-semibold text-paper">Start:</dt> <dd class="inline">{formatDate(w.startDate)}</dd></div>
						{/if}
					</dl>
					<p class="mt-2 font-display text-teal">
						{w.price ? `${w.price} €` : m.activities_price_on_request()}
					</p>
					<div class="mt-4 flex items-center justify-between gap-2">
						<a
							href={`/workshops/${w.id}`}
							class="rounded-full border border-ink-line px-4 py-2 text-sm text-paper transition-colors hover:border-gold"
						>
							{m.workshops_details()}
						</a>
						{#if w.status === 'PUBLISHED'}
							{@const sole = soleActiveGroup(w)}
							{#if sole}
								{@const full = sole.enrolledCount >= sole.capacity}
								<EnrollButton
									targetType="workshop"
									targetId={w.id}
									groupId={sole.id}
									disabled={full}
									label={full
										? m.workshop_detail_full()
										: isAuthenticated()
											? m.workshops_enroll()
											: m.workshops_enroll_login_first()}
									onSuccess={(result) => handleEnrollSuccess(w.id, result)}
									onError={(message) => handleEnrollError(w.id, message)}
								/>
							{:else if w.groups.some((g) => g.active)}
								<!-- 2+ active groups — no single date to enroll into
								     directly, send the user to pick one. -->
								<a
									href={`/workshops/${w.id}`}
									class="rounded-full bg-teal px-4 py-2 text-sm font-semibold text-ink transition-colors hover:opacity-90"
								>
									{m.workshops_choose_date()}
								</a>
							{/if}
						{/if}
					</div>
					{#if enrollResults[w.id]}
						{#if enrollResults[w.id].status === 'PENDING' && enrollResults[w.id].orderAmount != null}
							<p class="mt-2 text-sm text-success">
								{m.enroll_pending_label()} {enrollResults[w.id].orderAmount} {enrollResults[w.id].orderCurrency}
							</p>
						{:else}
							<p class="mt-2 text-sm text-success">{m.workshop_detail_enroll_success()}</p>
						{/if}
					{:else if enrollErrors[w.id]}
						<div class="mt-2"><ErrorText message={enrollErrors[w.id]} /></div>
					{/if}
				</Card>
			{/each}
		</div>
	{/if}
</section>
