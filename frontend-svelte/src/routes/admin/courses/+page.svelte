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
		getGroups,
		createGroup,
		updateGroup,
		generateSessionsFromRecurrence,
		type CourseListItem,
		type CourseCreateDTO,
		type UserBasicDTO,
		type AgeGroupDTO,
		type IsoDayOfWeek,
		type RecurrenceDay,
		type GroupUpdateRequestDTO,
		type GroupCreateRequestDTO
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

	// LR-081/082 (LR-ADR-023) — schedule for this Course's (single, MVP)
	// Group, managed inline here rather than on a separate admin/groups
	// visit, per direct customer request 2026-08-12. Course itself stays
	// schedule-free (LR-ADR-023) — this section writes to a linked Group,
	// not to the Course entity.
	const WEEKDAYS: { day: IsoDayOfWeek; label: string }[] = [
		{ day: 'MONDAY', label: m.admin_weekday_mon() },
		{ day: 'TUESDAY', label: m.admin_weekday_tue() },
		{ day: 'WEDNESDAY', label: m.admin_weekday_wed() },
		{ day: 'THURSDAY', label: m.admin_weekday_thu() },
		{ day: 'FRIDAY', label: m.admin_weekday_fri() },
		{ day: 'SATURDAY', label: m.admin_weekday_sat() },
		{ day: 'SUNDAY', label: m.admin_weekday_sun() }
	];
	type WeekdayRow = { day: IsoDayOfWeek; label: string; enabled: boolean; startTime: string; durationMinutes: number };
	const blankWeekdays = (): WeekdayRow[] =>
		WEEKDAYS.map((w) => ({ ...w, enabled: false, startTime: '18:00', durationMinutes: 60 }));

	let scheduleGroupId = $state<number | null>(null);
	let scheduleStartDate = $state('');
	let durationMonths = $state(1);
	let durationDays = $state(0);
	let maxParticipants = $state(15);
	let weekdays = $state<WeekdayRow[]>(blankWeekdays());
	// Snapshot at load time — regeneration only fires when recurrence
	// fields actually changed (LR-ADR-023 п.3: explicit guard, not a side
	// effect of every save), not e.g. when only the course description changed.
	let scheduleSnapshot = $state('');

	function currentScheduleSnapshot(): string {
		return JSON.stringify({ scheduleStartDate, durationMonths, durationDays, maxParticipants, weekdays });
	}

	function resetSchedule() {
		scheduleGroupId = null;
		scheduleStartDate = '';
		durationMonths = 1;
		durationDays = 0;
		maxParticipants = 15;
		weekdays = blankWeekdays();
		scheduleSnapshot = currentScheduleSnapshot();
	}

	function addMonthsDays(startIso: string, months: number, days: number): string {
		const d = new Date(startIso + 'T00:00:00');
		d.setMonth(d.getMonth() + months);
		d.setDate(d.getDate() + days);
		return d.toISOString().slice(0, 10);
	}

	// Approximate, editing-UX-only inverse of addMonthsDays — doesn't need
	// to be perfectly bijective, just a reasonable value to show back when
	// re-opening an existing course's schedule for editing.
	function diffMonthsDays(startIso: string, endIso: string): { months: number; days: number } {
		const start = new Date(startIso + 'T00:00:00');
		const end = new Date(endIso + 'T00:00:00');
		let months = (end.getFullYear() - start.getFullYear()) * 12 + (end.getMonth() - start.getMonth());
		const probe = new Date(start);
		probe.setMonth(probe.getMonth() + months);
		if (probe > end) {
			months -= 1;
			probe.setMonth(probe.getMonth() - 1);
		}
		const days = Math.round((end.getTime() - probe.getTime()) / 86400000);
		return { months: Math.max(0, months), days: Math.max(0, days) };
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

		try {
			const linkedGroups = await getGroups(undefined, c.id);
			const linked = linkedGroups[0]; // one Course = one Group, MVP scope
			if (linked) {
				scheduleGroupId = linked.id;
				maxParticipants = linked.capacity;
				scheduleStartDate = linked.recurrenceStartDate ?? '';
				if (linked.recurrenceStartDate && linked.recurrenceEndDate) {
					const diff = diffMonthsDays(linked.recurrenceStartDate, linked.recurrenceEndDate);
					durationMonths = diff.months;
					durationDays = diff.days;
				}
				weekdays = WEEKDAYS.map((w) => {
					const found = linked.recurrenceDays?.find((r) => r.dayOfWeek === w.day);
					return {
						...w,
						enabled: !!found,
						startTime: found ? found.startTime.slice(0, 5) : '18:00',
						durationMinutes: found?.durationMinutes ?? 60
					};
				});
				scheduleSnapshot = currentScheduleSnapshot();
			} else {
				resetSchedule();
			}
		} catch {
			resetSchedule();
		}
	}

	function cancelEdit() {
		editingId = null;
		form = { ...blank };
		selectedTeacherLabel = null;
		teacherQuery = '';
		teacherResults = [];
		resetSchedule();
	}

	async function handleSubmit(e: SubmitEvent) {
		e.preventDefault();
		saving = true;
		try {
			const course = editingId !== null ? await updateCourse(editingId, form) : await createCourse(form);

			// Schedule is optional — only touch the linked Group if the admin
			// actually set a start date. maxParticipants without a date isn't
			// meaningful on its own (no Group to hold it).
			if (scheduleStartDate) {
				const recurrenceDays: RecurrenceDay[] = weekdays
					.filter((w) => w.enabled)
					.map((w) => ({ dayOfWeek: w.day, startTime: w.startTime, durationMinutes: w.durationMinutes }));
				const recurrenceEndDate = addMonthsDays(scheduleStartDate, durationMonths, durationDays);
				const changed = currentScheduleSnapshot() !== scheduleSnapshot;

				let groupId = scheduleGroupId;
				if (groupId !== null) {
					const groupUpdate: GroupUpdateRequestDTO = {
						titleDe: course.titleDe,
						titleEn: course.titleEn,
						titleUa: course.titleUa,
						capacity: maxParticipants,
						startDateTime: `${scheduleStartDate}T00:00`,
						endDateTime: null,
						teacherId: null,
						activityId: null,
						venueId: null,
						ageGroupId: null,
						active: true,
						courseId: course.id,
						recurrenceDays,
						recurrenceStartDate: scheduleStartDate,
						recurrenceEndDate
					};
					await updateGroup(groupId, groupUpdate);
				} else {
					const groupCreate: GroupCreateRequestDTO = {
						titleDe: course.titleDe,
						titleEn: course.titleEn,
						titleUa: course.titleUa,
						capacity: maxParticipants,
						startDateTime: `${scheduleStartDate}T00:00`,
						endDateTime: null,
						workshopId: null,
						teacherId: null,
						activityId: null,
						venueId: null,
						ageGroupId: null,
						active: true,
						courseId: course.id,
						recurrenceDays,
						recurrenceStartDate: scheduleStartDate,
						recurrenceEndDate
					};
					const created = await createGroup(groupCreate);
					groupId = created.id;
				}

				// Explicit guard (LR-ADR-023 п.3) — only regenerate Sessions
				// when the recurrence fields actually changed, or this is a
				// brand-new schedule. Regeneration is destructive (clears
				// existing Sessions, LR-067) — not run on unrelated saves
				// (e.g. only the course description changed).
				if (changed && recurrenceDays.length > 0) {
					await generateSessionsFromRecurrence(groupId);
				}
			}

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
						<button type="button" onclick={clearTeacher} class="text-sm text-gold hover:underline">{m.admin_change()}</button>
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

		<!-- LR-081/082 (LR-ADR-023) — writes to this Course's linked Group,
		     not to the Course entity itself (Course stays schedule-free). -->
		<h2 class="mt-8 font-display text-lg font-semibold text-paper">{m.admin_course_schedule_title()}</h2>
		<div class="mt-4 grid gap-4 sm:grid-cols-3">
			<div>
				<label class="block text-sm text-paper-dim" for="csStart">{m.admin_course_schedule_start()}</label>
				<input
					id="csStart"
					type="date"
					value={scheduleStartDate}
					oninput={(e) => (scheduleStartDate = e.currentTarget.value)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<div>
				<label class="block text-sm text-paper-dim" for="csMonths">{m.admin_course_schedule_duration_months()}</label>
				<input
					id="csMonths"
					type="number"
					min="0"
					value={durationMonths}
					oninput={(e) => (durationMonths = Number(e.currentTarget.value) || 0)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<div>
				<label class="block text-sm text-paper-dim" for="csDays">{m.admin_course_schedule_duration_days()}</label>
				<input
					id="csDays"
					type="number"
					min="0"
					value={durationDays}
					oninput={(e) => (durationDays = Number(e.currentTarget.value) || 0)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
			<div>
				<label class="block text-sm text-paper-dim" for="csMax">{m.admin_course_schedule_max_participants()}</label>
				<input
					id="csMax"
					type="number"
					min="1"
					value={maxParticipants}
					oninput={(e) => (maxParticipants = Number(e.currentTarget.value) || 1)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
		</div>

		<div class="mt-4 space-y-2">
			{#each weekdays as w, i (w.day)}
				<div class="flex flex-wrap items-center gap-4 rounded-lg border border-ink-line p-3">
					<label class="flex w-16 items-center gap-2 text-sm text-paper">
						<input type="checkbox" bind:checked={w.enabled} class="h-4 w-4" />
						{w.label}
					</label>
					{#if w.enabled}
						<div>
							<label class="text-xs text-paper-dim" for={`csTime${i}`}>{m.admin_course_schedule_time()}</label>
							<input
								id={`csTime${i}`}
								type="time"
								value={w.startTime}
								oninput={(e) => (weekdays[i].startTime = e.currentTarget.value)}
								class="mt-1 block rounded-lg border border-ink-line bg-ink px-3 py-1.5 text-sm text-paper outline-none focus:border-gold"
							/>
						</div>
						<div>
							<label class="text-xs text-paper-dim" for={`csDur${i}`}>{m.admin_course_schedule_duration_minutes()}</label>
							<div class="mt-1 flex items-center gap-1">
								<button
									type="button"
									onclick={() => (weekdays[i].durationMinutes = Math.max(5, weekdays[i].durationMinutes - 15))}
									class="rounded-lg border border-ink-line px-2 py-1 text-sm text-paper hover:border-gold"
								>
									−
								</button>
								<input
									id={`csDur${i}`}
									type="number"
									min="5"
									step="5"
									value={w.durationMinutes}
									oninput={(e) => (weekdays[i].durationMinutes = Number(e.currentTarget.value) || 60)}
									class="w-20 rounded-lg border border-ink-line bg-ink px-2 py-1.5 text-center text-sm text-paper outline-none focus:border-gold"
								/>
								<button
									type="button"
									onclick={() => (weekdays[i].durationMinutes = weekdays[i].durationMinutes + 15)}
									class="rounded-lg border border-ink-line px-2 py-1 text-sm text-paper hover:border-gold"
								>
									+
								</button>
							</div>
						</div>
					{/if}
				</div>
			{/each}
		</div>
		{#if scheduleStartDate}
			<p class="mt-2 text-xs text-paper-dim">{m.admin_course_schedule_regenerate_note()}</p>
		{/if}

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
