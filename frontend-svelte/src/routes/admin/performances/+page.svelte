<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import {
		getPerformances,
		createPerformance,
		updatePerformance,
		deletePerformance,
		getWorkshops,
		type PerformanceDTO,
		type PerformanceWriteDTO,
		type PerformanceStatus,
		type WorkshopListItem
	} from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Input from '$lib/components/Input.svelte';
	import Textarea from '$lib/components/Textarea.svelte';
	import Button from '$lib/components/Button.svelte';

	const statuses: PerformanceStatus[] = ['PLANNED', 'CONFIRMED', 'COMPLETED', 'CANCELLED'];

	let performances = $state<PerformanceDTO[] | null>(null);
	let workshops = $state<WorkshopListItem[]>([]);
	let error = $state(false);
	let editingId = $state<number | null>(null);
	let saving = $state(false);
	// UI restructure 2026-08-15 — form is hidden by default; "Create new"
	// button shows it blank, "Edit" on a list card shows it pre-filled.
	let showForm = $state(false);

	const blank: PerformanceWriteDTO = {
		workshopId: null,
		title: '',
		description: '',
		performanceDate: '',
		venue: '',
		maxAttendees: null,
		status: 'PLANNED'
	};
	let form = $state<PerformanceWriteDTO>({ ...blank });

	function load() {
		getPerformances()
			.then((data) => (performances = data))
			.catch(() => (error = true));
	}
	$effect(() => {
		load();
		getWorkshops(false)
			.then((data) => (workshops = data))
			.catch(() => {});
	});

	function startEdit(p: PerformanceDTO) {
		editingId = p.id;
		showForm = true;
		form = {
			workshopId: p.workshopId,
			title: p.title,
			description: p.description,
			performanceDate: p.performanceDate,
			venue: p.venue ?? '',
			maxAttendees: p.maxAttendees,
			status: (p.status as PerformanceStatus) ?? 'PLANNED'
		};
	}

	function cancelEdit() {
		editingId = null;
		form = { ...blank };
		showForm = false;
	}

	function startCreate() {
		cancelEdit();
		showForm = true;
	}

	async function handleSubmit(e: SubmitEvent) {
		e.preventDefault();
		saving = true;
		try {
			if (editingId !== null) await updatePerformance(editingId, form);
			else await createPerformance(form);
			cancelEdit();
			load();
		} finally {
			saving = false;
		}
	}

	async function handleDelete(id: number) {
		await deletePerformance(id);
		load();
	}

	function formatDate(d: string | null) {
		if (!d) return '—';
		return new Date(d).toLocaleString('de-DE');
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.admin_nav_performances()}</title>
</svelte:head>

<h1 class="font-display text-3xl font-semibold text-paper">{m.admin_nav_performances()}</h1>

<!-- UI restructure 2026-08-15 — button first, form only while creating/editing. -->
{#if !showForm}
	<div class="mt-6">
		<Button onclick={startCreate} fullWidth={false}>{m.admin_create_new()}</Button>
	</div>
{/if}

{#if showForm}
<Card>
	<form onsubmit={handleSubmit}>
		<h2 class="font-display text-lg font-semibold text-paper">
			{editingId !== null ? m.admin_edit() : m.admin_create_new()}
		</h2>
		<Input id="pTitle" label={m.admin_performance_title()} required bind:value={form.title} />
		<Textarea id="pDesc" label={m.admin_workshop_description()} bind:value={form.description} />
		<div class="mt-4 grid gap-4 sm:grid-cols-2">
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="pWorkshop">{m.admin_group_workshop()}</label>
				<select
					id="pWorkshop"
					value={form.workshopId ?? ''}
					onchange={(e) => (form.workshopId = e.currentTarget.value ? Number(e.currentTarget.value) : null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				>
					<option value="">—</option>
					{#each workshops as w (w.id)}
						<option value={w.id}>{w.title}</option>
					{/each}
				</select>
			</div>
			<!-- `venue` is a free-text field on the real backend DTO, not a Venue
			     FK (the old static UI wrongly sent venueId, silently ignored) -->
			<Input id="pVenue" label={m.admin_performance_venue()} required bind:value={form.venue} />
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="pDate">{m.admin_performance_date()}</label>
				<input
					id="pDate"
					type="datetime-local"
					value={form.performanceDate}
					oninput={(e) => (form.performanceDate = e.currentTarget.value)}
					required
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="pMax">{m.admin_performance_max_attendees()}</label>
				<input
					id="pMax"
					type="number"
					min="1"
					value={form.maxAttendees ?? ''}
					oninput={(e) => (form.maxAttendees = e.currentTarget.value ? Number(e.currentTarget.value) : null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="pStatus">{m.admin_workshop_status()}</label>
				<select
					id="pStatus"
					bind:value={form.status}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				>
					{#each statuses as s (s)}
						<option value={s}>{s}</option>
					{/each}
				</select>
			</div>
		</div>
		<div class="mt-6 flex gap-3">
			<Button type="submit" fullWidth={false} busy={saving}>
				{editingId !== null ? m.admin_save() : m.admin_create_new()}
			</Button>
			{#if editingId !== null}
				<button type="button" onclick={cancelEdit} class="text-sm text-paper-dim hover:text-paper">
					{m.admin_cancel()}
				</button>
			{/if}
		</div>
	</form>
</Card>
{/if}

{#if error}
	<p class="mt-8 text-error">{m.state_error()}</p>
{:else if performances === null}
	<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
{:else if performances.length === 0}
	<p class="mt-8 text-paper-dim">{m.state_empty()}</p>
{:else}
	<div class="mt-8 grid gap-4 sm:grid-cols-2">
		{#each performances as p (p.id)}
			<Card>
				<h3 class="font-display text-lg font-semibold text-paper">{p.title}</h3>
				<p class="mt-1 text-sm text-paper-dim">{formatDate(p.performanceDate)} · {p.venue ?? '—'}</p>
				<div class="mt-4 flex gap-3 text-sm">
					<button onclick={() => startEdit(p)} class="text-gold hover:underline">{m.admin_edit()}</button>
					<button onclick={() => handleDelete(p.id)} class="text-error hover:underline">{m.admin_delete()}</button>
				</div>
			</Card>
		{/each}
	</div>
{/if}
