<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import {
		getAgeGroups,
		createAgeGroup,
		updateAgeGroup,
		deleteAgeGroup,
		type AgeGroupDTO,
		type AgeGroupRequestDTO
	} from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Input from '$lib/components/Input.svelte';
	import Button from '$lib/components/Button.svelte';

	let ageGroups = $state<AgeGroupDTO[] | null>(null);
	let error = $state(false);
	let editingId = $state<number | null>(null);
	let saving = $state(false);

	const blank: AgeGroupRequestDTO = {
		titleDe: '',
		titleEn: '',
		titleUa: '',
		minAge: 0,
		maxAge: 99
	};
	let form = $state<AgeGroupRequestDTO>({ ...blank });

	function load() {
		getAgeGroups()
			.then((data) => (ageGroups = data))
			.catch(() => (error = true));
	}
	$effect(() => {
		load();
	});

	function startEdit(a: AgeGroupDTO) {
		editingId = a.id;
		form = {
			titleDe: a.titleDe,
			titleEn: a.titleEn,
			titleUa: a.titleUa,
			minAge: a.minAge,
			maxAge: a.maxAge
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
			if (editingId !== null) await updateAgeGroup(editingId, form);
			else await createAgeGroup(form);
			cancelEdit();
			load();
		} finally {
			saving = false;
		}
	}

	async function handleDelete(id: number) {
		await deleteAgeGroup(id);
		load();
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.admin_nav_age_groups()}</title>
</svelte:head>

<h1 class="font-display text-3xl font-semibold text-paper">{m.admin_nav_age_groups()}</h1>

<Card>
	<form onsubmit={handleSubmit}>
		<h2 class="font-display text-lg font-semibold text-paper">
			{editingId !== null ? m.admin_edit() : m.admin_create_new()}
		</h2>
		<div class="mt-4 grid gap-4 sm:grid-cols-3">
			<Input id="agTitleDe" label="Titel (DE)" required bind:value={form.titleDe} />
			<Input id="agTitleEn" label="Title (EN)" required bind:value={form.titleEn} />
			<Input id="agTitleUa" label="Назва (UA)" required bind:value={form.titleUa} />
		</div>
		<div class="mt-4 grid gap-4 sm:grid-cols-2">
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="agMinAge">{m.admin_age_group_min_age()}</label>
				<input
					id="agMinAge"
					type="number"
					min="0"
					value={form.minAge}
					oninput={(e) => (form.minAge = Number(e.currentTarget.value))}
					required
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="agMaxAge">{m.admin_age_group_max_age()}</label>
				<input
					id="agMaxAge"
					type="number"
					min="0"
					value={form.maxAge}
					oninput={(e) => (form.maxAge = Number(e.currentTarget.value))}
					required
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
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
{:else if ageGroups === null}
	<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
{:else if ageGroups.length === 0}
	<p class="mt-8 text-paper-dim">{m.state_empty()}</p>
{:else}
	<div class="mt-8 grid gap-4 sm:grid-cols-2">
		{#each ageGroups as a (a.id)}
			<Card>
				<h3 class="font-display text-lg font-semibold text-paper">{a.titleDe}</h3>
				<p class="mt-1 text-sm text-paper-dim">{a.minAge}–{a.maxAge}</p>
				<div class="mt-4 flex gap-3 text-sm">
					<button onclick={() => startEdit(a)} class="text-gold hover:underline">{m.admin_edit()}</button>
					<button onclick={() => handleDelete(a.id)} class="text-error hover:underline">{m.admin_delete()}</button>
				</div>
			</Card>
		{/each}
	</div>
{/if}
