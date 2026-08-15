<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import { getVenues, createVenue, updateVenue, deleteVenue, type VenueDTO, type VenueRequestDTO } from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Input from '$lib/components/Input.svelte';
	import Textarea from '$lib/components/Textarea.svelte';
	import Button from '$lib/components/Button.svelte';

	let venues = $state<VenueDTO[] | null>(null);
	let error = $state(false);
	let editingId = $state<number | null>(null);
	let saving = $state(false);
	// UI restructure 2026-08-15 — form is hidden by default; "Create new"
	// button shows it blank, "Edit" on a list card shows it pre-filled.
	let showForm = $state(false);

	const blank: VenueRequestDTO = {
		name: '',
		room: '',
		address: '',
		city: '',
		postalCode: '',
		country: 'Deutschland',
		capacity: null,
		description: '',
		contactPhone: '',
		contactEmail: ''
	};
	let form = $state<VenueRequestDTO>({ ...blank });

	function load() {
		getVenues()
			.then((data) => (venues = data))
			.catch(() => (error = true));
	}
	$effect(() => {
		load();
	});

	function startEdit(v: VenueDTO) {
		editingId = v.id;
		showForm = true;
		form = {
			name: v.name,
			room: v.room ?? '',
			address: v.address,
			city: v.city,
			postalCode: v.postalCode ?? '',
			country: v.country ?? '',
			capacity: v.capacity,
			description: v.description ?? '',
			contactPhone: v.contactPhone ?? '',
			contactEmail: v.contactEmail ?? ''
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
			if (editingId !== null) await updateVenue(editingId, form);
			else await createVenue(form);
			cancelEdit();
			load();
		} finally {
			saving = false;
		}
	}

	async function handleDelete(id: number) {
		await deleteVenue(id);
		load();
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.admin_nav_venues()}</title>
</svelte:head>

<h1 class="font-display text-3xl font-semibold text-paper">{m.admin_nav_venues()}</h1>

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
		<div class="mt-4 grid gap-4 sm:grid-cols-2">
			<Input id="vName" label={m.admin_venue_name()} required bind:value={form.name} />
			<Input id="vRoom" label={m.admin_venue_room()} bind:value={form.room} />
			<Input id="vAddress" label={m.admin_venue_address()} required bind:value={form.address} />
			<Input id="vCity" label={m.admin_venue_city()} required bind:value={form.city} />
			<Input id="vPostal" label={m.admin_venue_postal()} bind:value={form.postalCode} />
			<Input id="vCountry" label={m.admin_venue_country()} bind:value={form.country} />
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="vCapacity">{m.admin_venue_capacity()}</label>
				<input
					id="vCapacity"
					type="number"
					min="0"
					value={form.capacity ?? ''}
					oninput={(e) => (form.capacity = e.currentTarget.value ? Number(e.currentTarget.value) : null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<Input id="vPhone" label={m.admin_venue_phone()} bind:value={form.contactPhone} />
			<Input id="vEmail" label={m.admin_venue_email()} type="email" bind:value={form.contactEmail} />
		</div>
		<Textarea id="vDesc" label={m.admin_venue_description()} bind:value={form.description} />
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
{:else if venues === null}
	<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
{:else if venues.length === 0}
	<p class="mt-8 text-paper-dim">{m.state_empty()}</p>
{:else}
	<div class="mt-8 grid gap-4 sm:grid-cols-2">
		{#each venues as v (v.id)}
			<Card>
				<h3 class="font-display text-lg font-semibold text-paper">
					{v.name}{#if v.room} — {v.room}{/if}
				</h3>
				<p class="mt-1 text-sm text-paper-dim">{v.address}, {v.city}</p>
				{#if v.capacity}<p class="mt-1 text-sm text-paper-dim">{m.admin_venue_capacity()}: {v.capacity}</p>{/if}
				<div class="mt-4 flex gap-3 text-sm">
					<button onclick={() => startEdit(v)} class="text-gold hover:underline">{m.admin_edit()}</button>
					<button onclick={() => handleDelete(v.id)} class="text-error hover:underline">{m.admin_delete()}</button>
				</div>
			</Card>
		{/each}
	</div>
{/if}
