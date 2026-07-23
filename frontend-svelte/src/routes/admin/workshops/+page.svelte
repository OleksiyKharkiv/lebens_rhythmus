<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import {
		getWorkshops,
		getWorkshop,
		createWorkshop,
		updateWorkshop,
		deleteWorkshop,
		getAllUsers,
		getVenues,
		type WorkshopListItem,
		type WorkshopCreateDTO,
		type WorkshopStatus,
		type UserBasicDTO,
		type VenueDTO
	} from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Input from '$lib/components/Input.svelte';
	import Textarea from '$lib/components/Textarea.svelte';
	import Button from '$lib/components/Button.svelte';

	const statuses: WorkshopStatus[] = ['DRAFT', 'PUBLISHED', 'ARCHIVED', 'CANCELLED'];

	let workshops = $state<WorkshopListItem[] | null>(null);
	let teachers = $state<UserBasicDTO[]>([]);
	let venues = $state<VenueDTO[]>([]);
	let error = $state(false);
	let editingId = $state<number | null>(null);
	let saving = $state(false);

	const blank: WorkshopCreateDTO = {
		title: '',
		description: '',
		teacherId: null,
		startDate: null,
		endDate: null,
		venueId: null,
		maxParticipants: null,
		price: null,
		status: 'DRAFT'
	};
	let form = $state<WorkshopCreateDTO>({ ...blank });

	function load() {
		getWorkshops(false)
			.then((data) => (workshops = data))
			.catch(() => (error = true));
	}
	$effect(() => {
		load();
		getAllUsers()
			.then((users) => (teachers = users.filter((u) => u.role === 'TEACHER')))
			.catch(() => {});
		getVenues()
			.then((data) => (venues = data))
			.catch(() => {});
	});

	async function startEdit(w: WorkshopListItem) {
		editingId = w.id;
		const detail = await getWorkshop(w.id);
		form = {
			title: detail.title,
			description: detail.description,
			teacherId: detail.teacher?.id ?? null,
			startDate: detail.startDate,
			endDate: detail.endDate,
			venueId: detail.venueId,
			maxParticipants: null, // WorkshopDetailDTO doesn't expose this — see KNOWN_ISSUES.md
			price: detail.price,
			status: (detail.status as WorkshopStatus) ?? 'DRAFT'
		};
	}

	function cancelEdit() {
		editingId = null;
		form = { ...blank };
	}

	async function handleSubmit(e: SubmitEvent) {
		e.preventDefault();
		saving = true;
		try {
			if (editingId !== null) await updateWorkshop(editingId, form);
			else await createWorkshop(form);
			cancelEdit();
			load();
		} finally {
			saving = false;
		}
	}

	async function handleDelete(id: number) {
		await deleteWorkshop(id);
		load();
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.admin_nav_workshops()}</title>
</svelte:head>

<h1 class="font-display text-3xl font-semibold text-paper">{m.admin_nav_workshops()}</h1>

<Card>
	<form onsubmit={handleSubmit}>
		<h2 class="font-display text-lg font-semibold text-paper">
			{editingId !== null ? m.admin_edit() : m.admin_create_new()}
		</h2>
		{#if editingId !== null}
			<!-- WorkshopDetailDTO has no maxParticipants field at all (real backend
			     gap, not a frontend bug) — the value exists in the DB but can't be
			     read back here, so this field always starts empty on edit. -->
			<p class="mt-2 text-xs text-paper-dim">{m.admin_workshop_max_participants_note()}</p>
		{/if}
		<Input id="wTitle" label={m.admin_workshop_title()} required bind:value={form.title} />
		<Textarea id="wDesc" label={m.admin_workshop_description()} bind:value={form.description} />
		<div class="mt-4 grid gap-4 sm:grid-cols-2">
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="wTeacher">{m.admin_workshop_teacher()}</label>
				<select
					id="wTeacher"
					value={form.teacherId ?? ''}
					onchange={(e) => (form.teacherId = e.currentTarget.value ? Number(e.currentTarget.value) : null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				>
					<option value="">—</option>
					{#each teachers as t (t.id)}
						<option value={t.id}>{t.firstName} {t.lastName}</option>
					{/each}
				</select>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="wVenue">{m.admin_workshop_venue()}</label>
				<select
					id="wVenue"
					value={form.venueId ?? ''}
					onchange={(e) => (form.venueId = e.currentTarget.value ? Number(e.currentTarget.value) : null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				>
					<option value="">—</option>
					{#each venues as v (v.id)}
						<option value={v.id}>{v.name}</option>
					{/each}
				</select>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="wStart">{m.admin_workshop_start()}</label>
				<input
					id="wStart"
					type="date"
					value={form.startDate ?? ''}
					oninput={(e) => (form.startDate = e.currentTarget.value || null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="wEnd">{m.admin_workshop_end()}</label>
				<input
					id="wEnd"
					type="date"
					value={form.endDate ?? ''}
					oninput={(e) => (form.endDate = e.currentTarget.value || null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="wMax">{m.admin_workshop_max_participants()}</label>
				<input
					id="wMax"
					type="number"
					min="1"
					value={form.maxParticipants ?? ''}
					oninput={(e) => (form.maxParticipants = e.currentTarget.value ? Number(e.currentTarget.value) : null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="wPrice">{m.admin_price()}</label>
				<input
					id="wPrice"
					type="number"
					step="0.01"
					min="0"
					value={form.price ?? ''}
					oninput={(e) => (form.price = e.currentTarget.value ? Number(e.currentTarget.value) : null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="wStatus">{m.admin_workshop_status()}</label>
				<select
					id="wStatus"
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

{#if error}
	<p class="mt-8 text-error">{m.state_error()}</p>
{:else if workshops === null}
	<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
{:else if workshops.length === 0}
	<p class="mt-8 text-paper-dim">{m.state_empty()}</p>
{:else}
	<div class="mt-8 grid gap-4 sm:grid-cols-2">
		{#each workshops as w (w.id)}
			<Card>
				<h3 class="font-display text-lg font-semibold text-paper">{w.title}</h3>
				<p class="mt-1 text-sm text-paper-dim">{w.status} · {w.startDate ?? '—'}</p>
				<div class="mt-4 flex gap-3 text-sm">
					<button onclick={() => startEdit(w)} class="text-gold hover:underline">{m.admin_edit()}</button>
					<button onclick={() => handleDelete(w.id)} class="text-error hover:underline">{m.admin_delete()}</button>
				</div>
			</Card>
		{/each}
	</div>
{/if}
