<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import { localizeHref } from '$lib/paraglide/runtime';
	import {
		getUserStatistics,
		getWorkshops,
		getAdminMetrics,
		type UserStatistics,
		type WorkshopListItem,
		type AdminMetricsDTO,
		type AlertLevel
	} from '$lib/api';
	import Card from '$lib/components/Card.svelte';

	let stats = $state<UserStatistics | null>(null);
	let recentWorkshops = $state<WorkshopListItem[] | null>(null);
	let metrics = $state<AdminMetricsDTO | null>(null);
	let error = $state(false);

	$effect(() => {
		Promise.all([getUserStatistics(), getWorkshops(true), getAdminMetrics()])
			.then(([s, w, met]) => {
				stats = s;
				recentWorkshops = w.slice(0, 5);
				metrics = met;
			})
			.catch(() => (error = true));
	});

	function formatDateTime(d: string) {
		return new Date(d).toLocaleString('de-DE', { dateStyle: 'medium', timeStyle: 'short' });
	}

	function formatPercent(ratio: number) {
		return `${Math.round(ratio * 100)}%`;
	}

	// Only existing semantic color tokens (layout.css) — no new ones for
	// four alert levels: teal < gold < error < error-deep in perceived urgency.
	const levelColor: Record<AlertLevel, string> = {
		info: 'text-teal',
		warning: 'text-gold',
		urgent: 'text-error',
		critical: 'text-error-deep'
	};

	// $derived, not a plain const — m.xxx() reads the active locale at call
	// time, needs to re-run if the user switches language client-side.
	const levelLabel = $derived<Record<AlertLevel, string>>({
		info: m.admin_metrics_alert_info(),
		warning: m.admin_metrics_alert_warning(),
		urgent: m.admin_metrics_alert_urgent(),
		critical: m.admin_metrics_alert_critical()
	});

	const maxTrendValue = $derived(
		metrics ? Math.max(1, ...metrics.registrationTrend.map((p) => p.newUsers)) : 1
	);
</script>

<svelte:head>
	<title>{m.site_name()} — {m.admin_nav_overview()}</title>
</svelte:head>

<h1 class="font-display text-3xl font-semibold text-paper">{m.admin_overview_title()}</h1>

{#if error}
	<p class="mt-8 text-error">{m.state_error()}</p>
{:else if stats === null || recentWorkshops === null || metrics === null}
	<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
{:else}
	<div class="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
		<Card>
			<p class="text-sm text-paper-dim">{m.admin_stats_total_users()}</p>
			<p class="font-display text-3xl text-paper">{stats.totalUsers}</p>
		</Card>
		<Card>
			<p class="text-sm text-paper-dim">{m.admin_stats_active_users()}</p>
			<p class="font-display text-3xl text-teal">{stats.activeUsers}</p>
		</Card>
		<Card>
			<p class="text-sm text-paper-dim">{m.admin_stats_customers()}</p>
			<p class="font-display text-3xl text-paper">{stats.userCount}</p>
		</Card>
		<Card>
			<p class="text-sm text-paper-dim">{m.admin_stats_teachers()}</p>
			<p class="font-display text-3xl text-paper">{stats.teacherCount}</p>
		</Card>
		<Card>
			<p class="text-sm text-paper-dim">{m.admin_stats_admins()}</p>
			<p class="font-display text-3xl text-paper">{stats.adminCount}</p>
		</Card>
	</div>

	<div class="mt-12">
		<h2 class="font-display text-xl font-semibold text-paper">{m.admin_overview_recent_workshops()}</h2>
		{#if recentWorkshops.length === 0}
			<p class="mt-4 text-paper-dim">{m.state_empty()}</p>
		{:else}
			<div class="mt-4 grid gap-4 sm:grid-cols-2">
				{#each recentWorkshops as w (w.id)}
					<Card>
						<h3 class="font-display text-lg font-semibold text-paper">{w.title}</h3>
						<p class="mt-1 text-sm text-paper-dim">{w.startDate ?? '—'}</p>
						<a href={localizeHref('/admin/workshops')} class="mt-3 inline-block text-sm text-gold hover:underline">
							{m.admin_manage()}
						</a>
					</Card>
				{/each}
			</div>
		{/if}
	</div>

	<!-- M5 — needing attention -->
	<div class="mt-12">
		<h2 class="font-display text-xl font-semibold text-paper">{m.admin_metrics_alerts_title()}</h2>
		{#if metrics.attentionAlerts.length === 0}
			<p class="mt-4 text-paper-dim">{m.admin_metrics_alerts_empty()}</p>
		{:else}
			<div class="mt-4 space-y-3">
				{#each metrics.attentionAlerts as a (a.groupId)}
					<Card>
						<div class="flex flex-wrap items-center justify-between gap-3">
							<div>
								<p class="font-display font-semibold text-paper">
									{a.workshopTitle ?? '—'} — {a.groupTitle}
								</p>
								<p class="mt-1 text-sm text-paper-dim">
									{formatDateTime(a.startDateTime)} · {formatPercent(a.fillRatio)}
									{m.admin_metrics_fill_ratio_suffix()}
								</p>
							</div>
							<span class="font-display text-sm font-semibold uppercase {levelColor[a.level]}">
								{levelLabel[a.level]}
							</span>
						</div>
					</Card>
				{/each}
			</div>
		{/if}
	</div>

	<!-- M1 — fill rate per group -->
	<div class="mt-12">
		<h2 class="font-display text-xl font-semibold text-paper">{m.admin_metrics_fill_rate_title()}</h2>
		{#if metrics.fillRates.length === 0}
			<p class="mt-4 text-paper-dim">{m.state_empty()}</p>
		{:else}
			<div class="mt-4 grid gap-4 sm:grid-cols-2">
				{#each metrics.fillRates as g (g.groupId)}
					<Card>
						<p class="font-display font-semibold text-paper">{g.workshopTitle ?? '—'} — {g.groupTitle}</p>
						<p class="mt-1 text-sm text-paper-dim">{formatDateTime(g.startDateTime)}</p>
						<p class="mt-1 text-sm text-paper-dim">
							{g.enrolledCount}/{g.capacity} ({formatPercent(g.fillRatio)})
						</p>
					</Card>
				{/each}
			</div>
		{/if}
	</div>

	<!-- M4 — new registrations trend + M6 — retention -->
	<div class="mt-12 grid gap-8 lg:grid-cols-2">
		<div>
			<h2 class="font-display text-xl font-semibold text-paper">{m.admin_metrics_trend_title()}</h2>
			<Card>
				<div class="flex h-32 items-end gap-0.5">
					{#each metrics.registrationTrend as p (p.date)}
						<div
							class="flex-1 rounded-t bg-teal"
							style="height: {(p.newUsers / maxTrendValue) * 100}%; min-height: {p.newUsers > 0 ? '2px' : '0'}"
							title="{p.date}: {p.newUsers}"
						></div>
					{/each}
				</div>
				<p class="mt-3 text-sm text-paper-dim">{m.admin_metrics_trend_note()}</p>
			</Card>
		</div>
		<div>
			<h2 class="font-display text-xl font-semibold text-paper">{m.admin_metrics_retention_title()}</h2>
			<Card>
				<p class="text-sm text-paper-dim">{m.admin_metrics_retention_rate()}</p>
				<p class="font-display text-3xl text-teal">{formatPercent(metrics.retention.retentionRate)}</p>
				<p class="mt-2 text-sm text-paper-dim">
					{metrics.retention.repeatCustomers} / {metrics.retention.totalCustomers}
					{m.admin_metrics_retention_note()}
				</p>
			</Card>
		</div>
	</div>
{/if}
