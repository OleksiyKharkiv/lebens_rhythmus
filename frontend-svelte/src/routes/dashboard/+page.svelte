<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import { localizeHref } from '$lib/paraglide/runtime';
	import {
		isAuthenticated,
		getMyEnrollments,
		getMyPayments,
		getMyMedia,
		type EnrollmentDTO,
		type PaymentDTO,
		type UserMediaDTO
	} from '$lib/api';
	import { getLocale } from '$lib/paraglide/runtime';
	import Card from '$lib/components/Card.svelte';

	let ready = $state(false);
	let enrollments = $state<EnrollmentDTO[] | null>(null);
	let payments = $state<PaymentDTO[] | null>(null);
	let media = $state<UserMediaDTO[] | null>(null);
	let error = $state(false);

	// adapter-static prerenders this route with no window/localStorage at
	// all — the redirect and the data fetch both have to wait for the
	// browser, same reasoning as +layout.svelte's loggedIn check.
	$effect(() => {
		if (!isAuthenticated()) {
			window.location.href = localizeHref('/login');
			return;
		}
		ready = true;

		// LR-107 — parallel fetch with dedicated getMyMedia endpoint (eliminates client-side N+1)
		Promise.all([getMyEnrollments(), getMyPayments(), getMyMedia()])
			.then(([enrollmentData, paymentData, mediaData]) => {
				enrollments = enrollmentData;
				payments = paymentData;
				media = mediaData;
			})
			.catch(() => (error = true));
	});

	function formatDate(d: string | null) {
		if (!d) return '—';
		const loc = getLocale();
		const intlLocale = loc === 'uk' ? 'uk-UA' : loc === 'en' ? 'en-US' : 'de-DE';
		return new Date(d).toLocaleDateString(intlLocale, { day: '2-digit', month: '2-digit', year: 'numeric' });
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.dashboard_title()}</title>
</svelte:head>

{#if ready}
	<section class="mx-auto max-w-5xl px-6 py-10 sm:py-16">
		<div class="border-b border-ink-line/70 pb-6">
			<h1 class="page-title font-display font-semibold text-paper">{m.dashboard_title()}</h1>
			<p class="lead-text mt-3 text-paper-dim">
				{m.dashboard_subtitle()}
			</p>
		</div>

		{#if error}
			<p class="mt-8 text-error">{m.state_error()}</p>
		{:else if enrollments === null || payments === null || media === null}
			<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
		{:else}
			<!-- Schedule -->
			<div class="mt-10">
				<h2 class="section-title font-display font-semibold text-paper">{m.dashboard_schedule_title()}</h2>
				{#if enrollments.length === 0}
					<p class="mt-4 text-paper-dim">{m.dashboard_schedule_empty()}</p>
				{:else}
					<div class="mt-5 grid gap-5 sm:grid-cols-2">
						{#each enrollments as e (e.id)}
							<div class="rounded-2xl border border-ink-line bg-ink-soft/40 p-6 transition-colors hover:border-gold/50 shadow-sm">
								<div class="flex items-start justify-between gap-3">
									<h3 class="list-card-title font-display font-semibold text-paper">
										{e.workshopTitle ?? e.courseTitle}
									</h3>
									<span
										class="inline-block shrink-0 rounded-full px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wider {e.status === 'APPROVED' || e.status === 'ACTIVE'
											? 'border border-teal/40 bg-teal/15 text-teal'
											: e.status === 'PENDING'
												? 'border border-gold/40 bg-gold/15 text-gold'
												: 'border border-ink-line bg-ink text-paper-dim'}"
									>
										{e.status}
									</span>
								</div>
								{#if e.groupTitle}
									<p class="mt-2 text-sm text-paper-dim">{e.groupTitle}</p>
								{/if}
								{#if e.status === 'PENDING' && e.orderAmount != null}
									<p class="mt-3 text-sm text-paper-dim">
										{m.enroll_pending_label()} <span class="font-semibold text-gold">{e.orderAmount} {e.orderCurrency}</span>
									</p>
								{/if}
							</div>
						{/each}
					</div>
				{/if}
			</div>

			<!-- Media -->
			<div class="mt-12">
				<h2 class="section-title font-display font-semibold text-paper">{m.dashboard_media_title()}</h2>
				{#if media.length === 0}
					<p class="mt-4 text-paper-dim">{m.dashboard_media_empty()}</p>
				{:else}
					<div class="mt-5 grid gap-4 sm:grid-cols-3">
						{#each media as f (f.id)}
							<a
								href={f.url}
								target="_blank"
								rel="noopener"
								class="group flex flex-col justify-between rounded-xl border border-ink-line bg-ink-soft/30 p-4 text-sm transition-all hover:border-gold/70 hover:bg-ink-soft/60"
							>
								<div class="flex items-center gap-2.5">
									<svg class="h-5 w-5 shrink-0 text-gold" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
										<path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
										<polyline points="14 2 14 8 20 8"/>
									</svg>
									<p class="font-medium text-paper truncate group-hover:text-gold transition-colors">{f.filename}</p>
								</div>
								{#if f.workshopTitle}
									<p class="mt-3 text-xs text-paper-dim">{f.workshopTitle}</p>
								{/if}
							</a>
						{/each}
					</div>
				{/if}
			</div>

			<!-- Payments -->
			<div class="mt-12">
				<h2 class="section-title font-display font-semibold text-paper">{m.dashboard_payments_title()}</h2>
				{#if payments.length === 0}
					<p class="mt-4 text-paper-dim">{m.dashboard_payments_empty()}</p>
				{:else}
					<div class="mt-5 overflow-hidden rounded-2xl border border-ink-line bg-ink-soft/30">
						<div class="overflow-x-auto">
							<table class="w-full text-left text-sm">
								<thead class="border-b border-ink-line bg-ink/60 text-paper-dim">
									<tr>
										<th class="py-3 px-4 font-semibold">{m.dashboard_payments_date()}</th>
										<th class="py-3 px-4 font-semibold">{m.dashboard_payments_order()}</th>
										<th class="py-3 px-4 font-semibold">{m.dashboard_payments_amount()}</th>
										<th class="py-3 px-4 font-semibold">{m.dashboard_payments_status()}</th>
									</tr>
								</thead>
								<tbody class="divide-y divide-ink-line/40">
									{#each payments as p (p.id)}
										<tr class="text-paper hover:bg-ink-soft/40 transition-colors">
											<td class="py-3 px-4">{formatDate(p.paidAt ?? p.createdAt)}</td>
											<td class="py-3 px-4 font-mono text-xs">{p.orderNumber ?? '—'}</td>
											<td class="py-3 px-4 font-medium">{p.amount} {p.currency}</td>
											<td class="py-3 px-4">
												<span class="inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold {p.status === 'PAID' || p.status === 'COMPLETED' ? 'text-teal bg-teal/10' : 'text-gold bg-gold/10'}">
													{p.status}
												</span>
											</td>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>
					</div>
				{/if}
			</div>
		{/if}
	</section>
{/if}
