<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import { getUserStatistics, getWorkshops, type UserStatistics, type WorkshopListItem } from '$lib/api';
	import Card from '$lib/components/Card.svelte';

	let stats = $state<UserStatistics | null>(null);
	let recentWorkshops = $state<WorkshopListItem[] | null>(null);
	let error = $state(false);

	$effect(() => {
		Promise.all([getUserStatistics(), getWorkshops(true)])
			.then(([s, w]) => {
				stats = s;
				recentWorkshops = w.slice(0, 5);
			})
			.catch(() => (error = true));
	});
</script>

<svelte:head>
	<title>{m.site_name()} — {m.admin_nav_overview()}</title>
</svelte:head>

<h1 class="font-display text-3xl font-semibold text-paper">{m.admin_overview_title()}</h1>

{#if error}
	<p class="mt-8 text-error">{m.state_error()}</p>
{:else if stats === null || recentWorkshops === null}
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
						<a href="/admin/workshops" class="mt-3 inline-block text-sm text-gold hover:underline">
							{m.admin_manage()}
						</a>
					</Card>
				{/each}
			</div>
		{/if}
	</div>
{/if}
