<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import {
		getCourses,
		getCourse,
		createCourse,
		updateCourse,
		deleteCourse,
		searchUsers,
		getAgeGroups,
		type CourseListItem,
		type CourseCreateDTO,
		type UserBasicDTO,
		type AgeGroupDTO
	} from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Input from '$lib/components/Input.svelte';
	import Textarea from '$lib/components/Textarea.svelte';
	import Button from '$lib/components/Button.svelte';

	let courses = $state<CourseListItem[] | null>(null);
	let ageGroups = $state<AgeGroupDTO[]>([]);
	let error = $state(false);
	let editingId = $state<number | null>(null);
	let saving = $state(false);

	const blank: CourseCreateDTO = {
		titleDe: '',
		titleEn: '',
		titleUa: '',
		descriptionDe: '',
		descriptionEn: '',
		descriptionUa: '',
		ageGroupId: null,
		teacherId: null,
		isOnline: false,
		isSynchronous: true,
		hasRecordings: false,
		formatDisclaimerDe: '',
		formatDisclaimerEn: '',
		formatDisclaimerUa: ''
	};
	let form = $state<CourseCreateDTO>({ ...blank });

	// Teacher search-by-name/email (LR-069 direct request) — teacherId is a
	// User.id, same temporary shape as Workshop.teacherId (see LR-072), NOT
	// yet backed by a proper Teacher-entity picker.
	let teacherQuery = $state('');
	let teacherResults = $state<UserBasicDTO[]>([]);
	let teacherSearching = $state(false);
	let selectedTeacherLabel = $state<string | null>(null);
	let teacherSearchTimeout: ReturnType<typeof setTimeout> | undefined;

	function onTeacherQueryInput(value: string) {
		teacherQuery = value;
		selectedTeacherLabel = null;
		clearTimeout(teacherSearchTimeout);
		if (value.trim().length < 2) {
			teacherResults = [];
			return;
		}
		teacherSearchTimeout = setTimeout(async () => {
			teacherSearching = true;
			try {
				teacherResults = await searchUsers(value.trim());
			} catch {
				teacherResults = [];
			} finally {
				teacherSearching = false;
			}
		}, 300);
	}

	function pickTeacher(u: UserBasicDTO) {
		form.teacherId = u.id;
		selectedTeacherLabel = `${u.firstName} ${u.lastName} (${u.email})`;
		teacherQuery = '';
		teacherResults = [];
	}

	function clearTeacher() {
		form.teacherId = null;
		selectedTeacherLabel = null;
	}

	function load() {
		getCourses()
			.then((data) => (courses = data))
			.catch(() => (error = true));
	}
	$effect(() => {
		load();
		getAgeGroups()
			.then((data) => (ageGroups = data))
			.catch(() => {});
	});

	async function startEdit(c: CourseListItem) {
		editingId = c.id;
		const detail = await getCourse(c.id);
		form = {
			titleDe: detail.titleDe,
			titleEn: detail.titleEn,
			titleUa: detail.titleUa,
			descriptionDe: detail.descriptionDe ?? '',
			descriptionEn: detail.descriptionEn ?? '',
			descriptionUa: detail.descriptionUa ?? '',
			ageGroupId: detail.ageGroupId,
			teacherId: detail.teacher?.id ?? null,
			isOnline: detail.isOnline,
			isSynchronous: detail.isSynchronous,
			hasRecordings: detail.hasRecordings,
			formatDisclaimerDe: detail.formatDisclaimerDe ?? '',
			formatDisclaimerEn: detail.formatDisclaimerEn ?? '',
			formatDisclaimerUa: detail.formatDisclaimerUa ?? ''
		};
		selectedTeacherLabel = detail.teacher ? `${detail.teacher.firstName} ${detail.teacher.lastName}` : null;
		teacherQuery = '';
		teacherResults = [];
	}

	function cancelEdit() {
		editingId = null;
		form = { ...blank };
		selectedTeacherLabel = null;
		teacherQuery = '';
		teacherResults = [];
	}

	async function handleSubmit(e: SubmitEvent) {
		e.preventDefault();
		saving = true;
		try {
			if (editingId !== null) await updateCourse(editingId, form);
			else await createCourse(form);
			cancelEdit();
			load();
		} finally {
			saving = false;
		}
	}

	async function handleDelete(id: number) {
		await deleteCourse(id);
		load();
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.admin_nav_courses()}</title>
</svelte:head>

<h1 class="font-display text-3xl font-semibold text-paper">{m.admin_nav_courses()}</h1>

<Card>
	<form onsubmit={handleSubmit}>
		<h2 class="font-display text-lg font-semibold text-paper">
			{editingId !== null ? m.admin_edit() : m.admin_create_new()}
		</h2>
		<div class="mt-4 grid gap-4 sm:grid-cols-3">
			<div><Input id="cTitleDe" label="Titel (DE)" required bind:value={form.titleDe} /></div>
			<div><Input id="cTitleEn" label="Title (EN)" required bind:value={form.titleEn} /></div>
			<div><Input id="cTitleUa" label="Назва (UA)" required bind:value={form.titleUa} /></div>
		</div>
		<div class="mt-4 grid gap-4 sm:grid-cols-3">
			<div><Textarea id="cDescDe" label="Beschreibung (DE)" bind:value={form.descriptionDe} /></div>
			<div><Textarea id="cDescEn" label="Description (EN)" bind:value={form.descriptionEn} /></div>
			<div><Textarea id="cDescUa" label="Опис (UA)" bind:value={form.descriptionUa} /></div>
		</div>

		<div class="mt-4 grid gap-4 sm:grid-cols-2">
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="cAgeGroup">{m.admin_group_age_group()}</label>
				<select
					id="cAgeGroup"
					value={form.ageGroupId ?? ''}
					onchange={(e) => (form.ageGroupId = e.currentTarget.value ? Number(e.currentTarget.value) : null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				>
					<option value="">—</option>
					{#each ageGroups as a (a.id)}
						<option value={a.id}>{a.titleDe} ({a.minAge}–{a.maxAge})</option>
					{/each}
				</select>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="cTeacherSearch">{m.admin_workshop_teacher()}</label>
				{#if selectedTeacherLabel}
					<div class="mt-1 flex items-center justify-between rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper">
						<span>{selectedTeacherLabel}</span>
						<button type="button" onclick={clearTeacher} class="text-sm text-error hover:underline">{m.admin_delete()}</button>
					</div>
				{:else}
					<div class="relative">
						<input
							id="cTeacherSearch"
							type="text"
							value={teacherQuery}
							oninput={(e) => onTeacherQueryInput(e.currentTarget.value)}
							placeholder={m.admin_users_search_placeholder()}
							class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
						/>
						{#if teacherSearching}
							<p class="mt-1 text-xs text-paper-dim">{m.state_loading()}</p>
						{:else if teacherResults.length > 0}
							<ul class="mt-1 max-h-48 overflow-y-auto rounded-lg border border-ink-line bg-ink">
								{#each teacherResults as u (u.id)}
									<li>
										<button
											type="button"
											onclick={() => pickTeacher(u)}
											class="block w-full px-4 py-2 text-left text-sm text-paper hover:bg-ink-line"
										>
											{u.firstName} {u.lastName} — {u.email}
										</button>
									</li>
								{/each}
							</ul>
						{/if}
					</div>
				{/if}
			</div>
		</div>

		<div class="mt-6 grid gap-3 sm:grid-cols-3">
			<label class="flex items-center gap-2 text-sm text-paper-dim">
				<input type="checkbox" bind:checked={form.isOnline} class="h-4 w-4" />
				{m.admin_course_is_online()}
			</label>
			<label class="flex items-center gap-2 text-sm text-paper-dim">
				<input type="checkbox" bind:checked={form.isSynchronous} class="h-4 w-4" />
				{m.admin_course_is_synchronous()}
			</label>
			<label class="flex items-center gap-2 text-sm text-paper-dim">
				<input type="checkbox" bind:checked={form.hasRecordings} class="h-4 w-4" />
				{m.admin_course_has_recordings()}
			</label>
		</div>

		<div class="mt-4 grid gap-4 sm:grid-cols-3">
			<div><Textarea id="cDisclaimerDe" label="{m.admin_course_format_disclaimer()} (DE)" bind:value={form.formatDisclaimerDe} /></div>
			<div><Textarea id="cDisclaimerEn" label="{m.admin_course_format_disclaimer()} (EN)" bind:value={form.formatDisclaimerEn} /></div>
			<div><Textarea id="cDisclaimerUa" label="{m.admin_course_format_disclaimer()} (UA)" bind:value={form.formatDisclaimerUa} /></div>
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
{:else if courses === null}
	<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
{:else if courses.length === 0}
	<p class="mt-8 text-paper-dim">{m.state_empty()}</p>
{:else}
	<div class="mt-8 grid gap-4 sm:grid-cols-2">
		{#each courses as c (c.id)}
			<Card>
				<h3 class="font-display text-lg font-semibold text-paper">{c.titleDe}</h3>
				{#if c.teacher}<p class="mt-1 text-sm text-paper-dim">{c.teacher.firstName} {c.teacher.lastName}</p>{/if}
				<div class="mt-4 flex gap-3 text-sm">
					<button onclick={() => startEdit(c)} class="text-gold hover:underline">{m.admin_edit()}</button>
					<button onclick={() => handleDelete(c.id)} class="text-error hover:underline">{m.admin_delete()}</button>
				</div>
			</Card>
		{/each}
	</div>
{/if}
