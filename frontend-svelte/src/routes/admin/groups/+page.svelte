<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import {
		getGroups,
		createGroup,
		updateGroup,
		deleteGroup,
		getWorkshops,
		getTeachers,
		getActivities,
		type GroupDTO,
		type GroupWriteDTO,
		type WorkshopListItem,
		type TeacherInfoDTO,
		type ActivityDTO
	} from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Input from '$lib/components/Input.svelte';
	import Button from '$lib/components/Button.svelte';

	let groups = $state<GroupDTO[] | null>(null);
	let workshops = $state<WorkshopListItem[]>([]);
	let teachers = $state<TeacherInfoDTO[]>([]);
	let activities = $state<ActivityDTO[]>([]);
	let error = $state(false);
	let editingId = $state<number | null>(null);
	let saving = $state(false);

	const blank: GroupWriteDTO = {
		titleDe: '',
		titleEn: '',
		titleUa: '',
		capacity: 10,
		startDateTime: '',
		endDateTime: null,
		workshop: null,
		teacher: null,
		activity: null,
		active: true
	};
	let form = $state<GroupWriteDTO>({ ...blank });

	function load() {
		getGroups()
			.then((data) => (groups = data))
			.catch(() => (error = true));
	}
	$effect(() => {
		load();
		getWorkshops(false)
			.then((data) => (workshops = data))
			.catch(() => {});
		// Teacher.id, NOT User.id — see api.ts note on GroupWriteDTO.
		getTeachers()
			.then((data) => (teachers = data))
			.catch(() => {});
		getActivities()
			.then((data) => (activities = data))
			.catch(() => {});
	});

	function startEdit(g: GroupDTO) {
		editingId = g.id;
		form = {
			titleDe: g.titleDe,
			titleEn: g.titleEn,
			titleUa: g.titleUa,
			capacity: g.capacity,
			startDateTime: g.startDateTime,
			endDateTime: g.endDateTime,
			workshop: g.workshopId ? { id: g.workshopId } : null,
			teacher: g.teacherId ? { id: g.teacherId } : null,
			activity: g.activityId ? { id: g.activityId } : null,
			active: g.active
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
			if (editingId !== null) await updateGroup(editingId, form);
			else await createGroup(form);
			cancelEdit();
			load();
		} finally {
			saving = false;
		}
	}

	async function handleDelete(id: number) {
		await deleteGroup(id);
		load();
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.admin_nav_groups()}</title>
</svelte:head>

<h1 class="font-display text-3xl font-semibold text-paper">{m.admin_nav_groups()}</h1>

<Card>
	<form onsubmit={handleSubmit}>
		<h2 class="font-display text-lg font-semibold text-paper">
			{editingId !== null ? m.admin_edit() : m.admin_create_new()}
		</h2>
		<div class="mt-4 grid gap-4 sm:grid-cols-3">
			<Input id="gTitleDe" label="Titel (DE)" required bind:value={form.titleDe} />
			<Input id="gTitleEn" label="Title (EN)" required bind:value={form.titleEn} />
			<Input id="gTitleUa" label="Назва (UA)" required bind:value={form.titleUa} />
		</div>
		<div class="mt-4 grid gap-4 sm:grid-cols-2">
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="gWorkshop">{m.admin_group_workshop()}</label>
				<select
					id="gWorkshop"
					value={form.workshop?.id ?? ''}
					disabled={editingId !== null}
					onchange={(e) => (form.workshop = e.currentTarget.value ? { id: Number(e.currentTarget.value) } : null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold disabled:opacity-50"
				>
					<option value="">—</option>
					{#each workshops as w (w.id)}
						<option value={w.id}>{w.title}</option>
					{/each}
				</select>
				{#if editingId !== null}
					<!-- GroupService.update() deliberately never reassigns workshop
					     (LR-009 — open question about existing enrollments) — a
					     writable select here would silently no-op on save. -->
					<p class="mt-1 text-xs text-paper-dim">{m.admin_group_workshop_locked_note()}</p>
				{/if}
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="gActivity">{m.admin_group_activity()}</label>
				<select
					id="gActivity"
					value={form.activity?.id ?? ''}
					onchange={(e) => (form.activity = e.currentTarget.value ? { id: Number(e.currentTarget.value) } : null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				>
					<option value="">—</option>
					{#each activities as a (a.id)}
						<option value={a.id}>{a.titleDe}</option>
					{/each}
				</select>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="gTeacher">{m.admin_workshop_teacher()}</label>
				<select
					id="gTeacher"
					value={form.teacher?.id ?? ''}
					onchange={(e) => (form.teacher = e.currentTarget.value ? { id: Number(e.currentTarget.value) } : null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				>
					<option value="">—</option>
					{#each teachers as t (t.id)}
						<option value={t.id}>{t.firstName} {t.lastName}</option>
					{/each}
				</select>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="gCapacity">{m.admin_group_capacity()}</label>
				<input
					id="gCapacity"
					type="number"
					min="1"
					bind:value={form.capacity}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="gStart">{m.admin_group_start()}</label>
				<input
					id="gStart"
					type="datetime-local"
					value={form.startDateTime}
					oninput={(e) => (form.startDateTime = e.currentTarget.value)}
					required
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="gEnd">{m.admin_group_end()}</label>
				<input
					id="gEnd"
					type="datetime-local"
					value={form.endDateTime ?? ''}
					oninput={(e) => (form.endDateTime = e.currentTarget.value || null)}
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
{:else if groups === null}
	<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
{:else if groups.length === 0}
	<p class="mt-8 text-paper-dim">{m.state_empty()}</p>
{:else}
	<div class="mt-8 grid gap-4 sm:grid-cols-2">
		{#each groups as g (g.id)}
			<Card>
				<h3 class="font-display text-lg font-semibold text-paper">{g.titleDe}</h3>
				<p class="mt-1 text-sm text-paper-dim">{g.workshopTitle ?? '—'}</p>
				<p class="mt-1 text-sm text-paper-dim">{g.enrolledCount}/{g.capacity}</p>
				<div class="mt-4 flex gap-3 text-sm">
					<button onclick={() => startEdit(g)} class="text-gold hover:underline">{m.admin_edit()}</button>
					<button onclick={() => handleDelete(g.id)} class="text-error hover:underline">{m.admin_delete()}</button>
				</div>
			</Card>
		{/each}
	</div>
{/if}
