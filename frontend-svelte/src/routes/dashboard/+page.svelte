<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import {
		isAuthenticated,
		getMyEnrollments,
		getMyPayments,
		getWorkshop,
		type EnrollmentDTO,
		type PaymentDTO,
		type WorkshopFileDTO
	} from '$lib/api';
	import Card from '$lib/components/Card.svelte';

	let ready = $state(false);
	let enrollments = $state<EnrollmentDTO[] | null>(null);
	let payments = $state<PaymentDTO[] | null>(null);
	let media = $state<(WorkshopFileDTO & { workshopTitle: string })[] | null>(null);
	let error = $state(false);

	// adapter-static prerenders this route with no window/localStorage at
	// all — the redirect and the data fetch both have to wait for the
	// browser, same reasoning as +layout.svelte's loggedIn check.
	$effect(() => {
		if (!isAuthenticated()) {
			window.location.href = '/login';
			return;
		}
		ready = true;

		Promise.all([getMyEnrollments(), getMyPayments()])
			.then(async ([enrollmentData, paymentData]) => {
				enrollments = enrollmentData;
				payments = paymentData;

				// No dedicated "my media" endpoint exists yet — files live on
				// WorkshopDetail (LR-ADR-016 scoped this as photos/videos from
				// classes the user is enrolled in, not a separate media store).
				// LR-084 — workshopId is now nullable (Course enrollments have
				// none); Course has no equivalent media feature yet, so those
				// rows are simply excluded here, not an oversight.
				const workshopIds = [...new Set(enrollmentData.map((e) => e.workshopId).filter((id) => id !== null))];
				const details = await Promise.all(workshopIds.map((id) => getWorkshop(id).catch(() => null)));
				media = details
					.filter((d) => d !== null)
					.flatMap((d) => d!.files.map((f) => ({ ...f, workshopTitle: d!.title })));
			})
			.catch(() => (error = true));
	});

	function formatDate(d: string | null) {
		if (!d) return '—';
		return new Date(d).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' });
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.dashboard_title()}</title>
</svelte:head>

{#if ready}
	<section class="mx-auto max-w-4xl px-6 py-16 sm:py-24">
		<h1 class="font-display text-3xl font-semibold text-paper sm:text-4xl">{m.dashboard_title()}</h1>

		{#if error}
			<p class="mt-8 text-error">{m.state_error()}</p>
		{:else if enrollments === null || payments === null || media === null}
			<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
		{:else}
			<!-- Schedule -->
			<div class="mt-12">
				<h2 class="font-display text-xl font-semibold text-paper">{m.dashboard_schedule_title()}</h2>
				{#if enrollments.length === 0}
					<p class="mt-4 text-paper-dim">{m.dashboard_schedule_empty()}</p>
				{:else}
					<div class="mt-4 grid gap-4 sm:grid-cols-2">
						{#each enrollments as e (e.id)}
							<Card>
								<!-- LR-084 — workshopTitle/courseTitle mutually exclusive now,
								     never both set. -->
								<h3 class="font-display text-lg font-semibold text-paper">
									{e.workshopTitle ?? e.courseTitle}
								</h3>
								{#if e.groupTitle}<p class="mt-1 text-sm text-paper-dim">{e.groupTitle}</p>{/if}
								<p class="mt-2 text-sm text-teal">{e.status}</p>
								{#if e.status === 'PENDING' && e.orderAmount != null}
									<p class="mt-1 text-sm text-paper-dim">
										{m.enroll_pending_label()} {e.orderAmount} {e.orderCurrency}
									</p>
								{/if}
							</Card>
						{/each}
					</div>
				{/if}
			</div>

			<!-- Media -->
			<div class="mt-12">
				<h2 class="font-display text-xl font-semibold text-paper">{m.dashboard_media_title()}</h2>
				{#if media.length === 0}
					<p class="mt-4 text-paper-dim">{m.dashboard_media_empty()}</p>
				{:else}
					<div class="mt-4 grid gap-4 sm:grid-cols-3">
						{#each media as f (f.id)}
							<a
								href={f.url}
								target="_blank"
								rel="noopener"
								class="block rounded-xl border border-ink-line p-4 text-sm text-paper-dim transition-colors hover:border-gold"
							>
								<p class="text-paper">{f.filename}</p>
								<p class="mt-1 text-xs">{f.workshopTitle}</p>
							</a>
						{/each}
					</div>
				{/if}
			</div>

			<!-- Payments -->
			<div class="mt-12">
				<h2 class="font-display text-xl font-semibold text-paper">{m.dashboard_payments_title()}</h2>
				{#if payments.length === 0}
					<p class="mt-4 text-paper-dim">{m.dashboard_payments_empty()}</p>
				{:else}
					<div class="mt-4 overflow-x-auto">
						<table class="w-full text-left text-sm">
							<thead class="text-paper-dim">
								<tr class="border-b border-ink-line">
									<th class="py-2 pr-4">{m.dashboard_payments_date()}</th>
									<th class="py-2 pr-4">{m.dashboard_payments_order()}</th>
									<th class="py-2 pr-4">{m.dashboard_payments_amount()}</th>
									<th class="py-2">{m.dashboard_payments_status()}</th>
								</tr>
							</thead>
							<tbody>
								{#each payments as p (p.id)}
									<tr class="border-b border-ink-line/50 text-paper">
										<td class="py-2 pr-4">{formatDate(p.paidAt ?? p.createdAt)}</td>
										<td class="py-2 pr-4">{p.orderNumber ?? '—'}</td>
										<td class="py-2 pr-4">{p.amount} {p.currency}</td>
										<td class="py-2 text-teal">{p.status}</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				{/if}
			</div>
		{/if}
	</section>
{/if}
