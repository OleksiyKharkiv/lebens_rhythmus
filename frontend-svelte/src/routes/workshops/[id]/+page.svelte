<script lang="ts">
	import { page } from '$app/state';
	import * as m from '$lib/paraglide/messages.js';
	import { isAuthenticated, getWorkshop, type WorkshopDetail, type EnrollmentDTO } from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import EnrollButton from '$lib/components/EnrollButton.svelte';
	import ErrorText from '$lib/components/ErrorText.svelte';

	let workshop = $state<WorkshopDetail | null>(null);
	let error = $state(false);
	let enrollError = $state('');
	let enrollResult = $state<EnrollmentDTO | null>(null);

	// SvelteKit guarantees params.id is set for a matched [id] route — the
	// `string | undefined` in its generated type is generic route typing,
	// not a real runtime possibility here.
	const workshopId = $derived(page.params.id as string);

	function load() {
		getWorkshop(workshopId)
			.then((data) => (workshop = data))
			.catch(() => (error = true));
	}

	$effect(() => {
		load();
	});

	function formatDateTime(d: string) {
		return new Date(d).toLocaleString('de-DE', { dateStyle: 'medium', timeStyle: 'short' });
	}

	function handleEnrollSuccess(result: EnrollmentDTO) {
		enrollError = '';
		enrollResult = result;
		load();
	}

	function handleEnrollError(message: string) {
		enrollResult = null;
		enrollError = message;
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {workshop?.title ?? m.workshops_title()}</title>
</svelte:head>

<section class="mx-auto max-w-4xl px-6 py-16 sm:py-24">
	{#if error}
		<p class="text-error">{m.state_error()}</p>
	{:else if workshop === null}
		<p class="text-paper-dim">{m.state_loading()}</p>
	{:else}
		<h1 class="font-display text-3xl font-semibold text-paper sm:text-4xl">{workshop.title}</h1>
		<!-- whitespace-pre-line (2026-08-14, same fix as courses/[id]) — plain
		     HTML text collapses newlines/blank lines by default; this
		     preserves the admin's paragraph breaks without a rich-text editor. -->
		{#if workshop.description}<p class="mt-4 whitespace-pre-line text-lg leading-relaxed text-paper-dim">{workshop.description}</p>{/if}

		<dl class="mt-6 grid grid-cols-2 gap-4 text-sm text-paper-dim">
			{#if workshop.startDate}
				<div><dt class="font-semibold text-paper">Start</dt><dd>{workshop.startDate}</dd></div>
			{/if}
			{#if workshop.teacher}
				<div>
					<dt class="font-semibold text-paper">Kursleitung</dt>
					<dd>{workshop.teacher.firstName} {workshop.teacher.lastName}</dd>
				</div>
			{/if}
			<div>
				<dt class="font-semibold text-paper">Preis</dt>
				<dd>{workshop.price ? `${workshop.price} €` : m.activities_price_on_request()}</dd>
			</div>
		</dl>

		<h2 class="mt-10 font-display text-xl font-semibold text-paper">{m.workshop_detail_groups_title()}</h2>

		{#if enrollResult}
			{#if enrollResult.status === 'PENDING' && enrollResult.orderAmount != null}
				<p class="mt-3 text-success">
					{m.enroll_pending_label()} {enrollResult.orderAmount} {enrollResult.orderCurrency}
				</p>
			{:else}
				<p class="mt-3 text-success">{m.workshop_detail_enroll_success()}</p>
			{/if}
		{/if}
		<ErrorText message={enrollError} />

		{#if !workshop.groups || workshop.groups.length === 0}
			<p class="mt-3 text-paper-dim">{m.workshop_detail_no_groups()}</p>
		{:else}
			<div class="mt-4 space-y-4">
				{#each workshop.groups.filter((g) => g.active) as group (group.id)}
					{@const full = group.enrolledCount >= group.capacity}
					<Card>
						<div class="flex flex-wrap items-center justify-between gap-4">
							<div>
								<p class="font-display font-semibold text-paper">{group.titleDe || group.titleEn}</p>
								<p class="mt-1 text-sm text-paper-dim">
									{formatDateTime(group.startDateTime)}
									{#if group.endDateTime}— {formatDateTime(group.endDateTime)}{/if}
								</p>
								{#if group.venueName}
									<p class="mt-1 text-sm text-paper-dim">{group.venueName}</p>
								{/if}
								<p class="mt-1 text-sm text-paper-dim">
									{group.enrolledCount}/{group.capacity} {m.workshops_spots_left()}
								</p>
							</div>
							<EnrollButton
								targetType="workshop"
								targetId={workshop.id}
								groupId={group.id}
								disabled={full}
								label={full
									? m.workshop_detail_full()
									: isAuthenticated()
										? m.workshops_enroll()
										: m.workshops_enroll_login_first()}
								onSuccess={handleEnrollSuccess}
								onError={handleEnrollError}
							/>
						</div>
					</Card>
				{/each}
			</div>
		{/if}
	{/if}
</section>
