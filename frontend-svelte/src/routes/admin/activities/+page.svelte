<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import {
		getActivities,
		createActivity,
		updateActivity,
		deleteActivity,
		type ActivityDTO,
		type ActivityRequestDTO
	} from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Input from '$lib/components/Input.svelte';
	import Textarea from '$lib/components/Textarea.svelte';
	import Button from '$lib/components/Button.svelte';

	let activities = $state<ActivityDTO[] | null>(null);
	let error = $state(false);
	let editingId = $state<number | null>(null);
	let saving = $state(false);

	const blank: ActivityRequestDTO = {
		titleDe: '',
		titleEn: '',
		titleUa: '',
		descriptionDe: '',
		descriptionEn: '',
		descriptionUa: '',
		price: 0,
		durationMinutes: 60,
		active: true
	};
	let form = $state<ActivityRequestDTO>({ ...blank });

	function load() {
		getActivities()
			.then((data) => (activities = data))
			.catch(() => (error = true));
	}
	$effect(() => {
		load();
	});

	function startEdit(a: ActivityDTO) {
		editingId = a.id;
		form = {
			titleDe: a.titleDe,
			titleEn: a.titleEn,
			titleUa: a.titleUa,
			descriptionDe: a.descriptionDe,
			descriptionEn: a.descriptionEn,
			descriptionUa: a.descriptionUa,
			price: a.price,
			durationMinutes: a.durationMinutes,
			active: a.active
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
			if (editingId !== null) await updateActivity(editingId, form);
			else await createActivity(form);
			cancelEdit();
			load();
		} finally {
			saving = false;
		}
	}

	async function handleDelete(id: number) {
		await deleteActivity(id);
		load();
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.admin_nav_activities()}</title>
</svelte:head>

<h1 class="font-display text-3xl font-semibold text-paper">{m.admin_nav_activities()}</h1>

<Card>
	<form onsubmit={handleSubmit}>
		<h2 class="font-display text-lg font-semibold text-paper">
			{editingId !== null ? m.admin_edit() : m.admin_create_new()}
		</h2>
		<div class="mt-4 grid gap-4 sm:grid-cols-3">
			<Input id="titleDe" label="Titel (DE)" required bind:value={form.titleDe} />
			<Input id="titleEn" label="Title (EN)" required bind:value={form.titleEn} />
			<Input id="titleUa" label="Назва (UA)" required bind:value={form.titleUa} />
		</div>
		<div class="mt-4 grid gap-4 sm:grid-cols-3">
			<Textarea id="descDe" label="Beschreibung (DE)" bind:value={form.descriptionDe} />
			<Textarea id="descEn" label="Description (EN)" bind:value={form.descriptionEn} />
			<Textarea id="descUa" label="Опис (UA)" bind:value={form.descriptionUa} />
		</div>
		<div class="mt-4 grid gap-4 sm:grid-cols-3">
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="price">{m.admin_price()}</label>
				<input
					id="price"
					type="number"
					step="0.01"
					min="0"
					bind:value={form.price}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="duration">{m.admin_duration_min()}</label>
				<input
					id="duration"
					type="number"
					min="1"
					bind:value={form.durationMinutes}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<label class="mt-4 flex items-end gap-2 pb-2.5 text-sm text-paper-dim">
				<input type="checkbox" bind:checked={form.active} class="accent-teal" />
				{m.admin_active()}
			</label>
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
{:else if activities === null}
	<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
{:else if activities.length === 0}
	<p class="mt-8 text-paper-dim">{m.state_empty()}</p>
{:else}
	<div class="mt-8 grid gap-4 sm:grid-cols-2">
		{#each activities as a (a.id)}
			<Card>
				<h3 class="font-display text-lg font-semibold text-paper">{a.titleDe}</h3>
				<p class="mt-1 text-sm text-paper-dim">{a.price} € · {a.durationMinutes} {m.activities_duration_min()}</p>
				<div class="mt-4 flex gap-3 text-sm">
					<button onclick={() => startEdit(a)} class="text-gold hover:underline">{m.admin_edit()}</button>
					<button onclick={() => handleDelete(a.id)} class="text-error hover:underline">{m.admin_delete()}</button>
				</div>
			</Card>
		{/each}
	</div>
{/if}
