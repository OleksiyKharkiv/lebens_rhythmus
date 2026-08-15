<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import {
		getTeachers,
		createTeacher,
		updateTeacher,
		deleteTeacher,
		type TeacherInfoDTO,
		type TeacherRequestDTO
	} from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Input from '$lib/components/Input.svelte';
	import Textarea from '$lib/components/Textarea.svelte';
	import Button from '$lib/components/Button.svelte';

	let teachers = $state<TeacherInfoDTO[] | null>(null);
	let error = $state(false);
	let editingId = $state<number | null>(null);
	let saving = $state(false);
	// UI restructure 2026-08-15 — form is hidden by default; "Create new"
	// button shows it blank, "Edit" on a list card shows it pre-filled.
	let showForm = $state(false);

	const blank: TeacherRequestDTO = {
		firstName: '',
		lastName: '',
		email: '',
		phone: '',
		title: '',
		approved: false,
		bioDe: '',
		bioEn: '',
		bioUa: '',
		active: true
	};
	let form = $state<TeacherRequestDTO>({ ...blank });

	function load() {
		getTeachers()
			.then((data) => (teachers = data))
			.catch(() => (error = true));
	}
	$effect(() => {
		load();
	});

	function startEdit(t: TeacherInfoDTO) {
		editingId = t.id;
		showForm = true;
		form = {
			firstName: t.firstName,
			lastName: t.lastName,
			email: t.email,
			phone: t.phone ?? '',
			title: t.title ?? '',
			approved: t.approved,
			bioDe: t.bioDe ?? '',
			bioEn: t.bioEn ?? '',
			bioUa: t.bioUa ?? '',
			active: t.active
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
			if (editingId !== null) await updateTeacher(editingId, form);
			else await createTeacher(form);
			cancelEdit();
			load();
		} finally {
			saving = false;
		}
	}

	async function handleDelete(id: number) {
		await deleteTeacher(id);
		load();
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.admin_nav_teachers()}</title>
</svelte:head>

<h1 class="font-display text-3xl font-semibold text-paper">{m.admin_nav_teachers()}</h1>

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
			<div><Input id="tFirstName" label={m.admin_teacher_first_name()} required bind:value={form.firstName} /></div>
			<div><Input id="tLastName" label={m.admin_teacher_last_name()} required bind:value={form.lastName} /></div>
			<div><Input id="tEmail" type="email" label={m.admin_teacher_email()} required bind:value={form.email} /></div>
			<div><Input id="tPhone" label={m.admin_teacher_phone()} bind:value={form.phone} /></div>
			<div><Input id="tTitle" label={m.admin_teacher_title()} bind:value={form.title} /></div>
		</div>
		<div class="mt-4 grid gap-4 sm:grid-cols-3">
			<div><Textarea id="tBioDe" label={m.admin_teacher_bio_de()} bind:value={form.bioDe} /></div>
			<div><Textarea id="tBioEn" label={m.admin_teacher_bio_en()} bind:value={form.bioEn} /></div>
			<div><Textarea id="tBioUa" label={m.admin_teacher_bio_ua()} bind:value={form.bioUa} /></div>
		</div>
		<div class="mt-4 flex flex-wrap gap-6">
			<label class="flex items-center gap-2 text-sm text-paper-dim">
				<input type="checkbox" bind:checked={form.approved} class="rounded border-ink-line" />
				{m.admin_teacher_approved()}
			</label>
			<label class="flex items-center gap-2 text-sm text-paper-dim">
				<input type="checkbox" bind:checked={form.active} class="rounded border-ink-line" />
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
{/if}

{#if error}
	<p class="mt-8 text-error">{m.state_error()}</p>
{:else if teachers === null}
	<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
{:else if teachers.length === 0}
	<p class="mt-8 text-paper-dim">{m.state_empty()}</p>
{:else}
	<div class="mt-8 grid gap-4 sm:grid-cols-2">
		{#each teachers as t (t.id)}
			<Card>
				<h3 class="font-display text-lg font-semibold text-paper">{t.firstName} {t.lastName}</h3>
				<p class="mt-1 text-sm text-paper-dim">{t.email}</p>
				{#if t.title}<p class="mt-1 text-sm text-paper-dim">{t.title}</p>{/if}
				<p class="mt-1 text-xs text-paper-dim">
					{t.approved ? m.admin_teacher_approved() : '—'} · {t.active ? m.admin_active() : '—'}
				</p>
				<div class="mt-4 flex gap-3 text-sm">
					<button onclick={() => startEdit(t)} class="text-gold hover:underline">{m.admin_edit()}</button>
					<button onclick={() => handleDelete(t.id)} class="text-error hover:underline">{m.admin_delete()}</button>
				</div>
			</Card>
		{/each}
	</div>
{/if}
