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
		getVenues,
		getAgeGroups,
		getSessions,
		replaceSessions,
		type GroupDTO,
		type GroupWriteDTO,
		type GroupCreateRequestDTO,
		type GroupUpdateRequestDTO,
		type WorkshopListItem,
		type TeacherInfoDTO,
		type ActivityDTO,
		type VenueDTO,
		type AgeGroupDTO,
		type SessionWriteDTO
	} from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Input from '$lib/components/Input.svelte';
	import Button from '$lib/components/Button.svelte';

	let groups = $state<GroupDTO[] | null>(null);
	let workshops = $state<WorkshopListItem[]>([]);
	let teachers = $state<TeacherInfoDTO[]>([]);
	let activities = $state<ActivityDTO[]>([]);
	let venues = $state<VenueDTO[]>([]);
	let ageGroups = $state<AgeGroupDTO[]>([]);
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
		venue: null,
		ageGroup: null,
		// LR-081 — this page only manages Workshop-linked groups; Course
		// scheduling is managed inline on admin/courses instead.
		course: null,
		recurrenceDays: null,
		recurrenceStartDate: null,
		recurrenceEndDate: null,
		active: true
	};
	let form = $state<GroupWriteDTO>({ ...blank });

	// LR-074 — replaces the single startDateTime/endDateTime pair with a
	// per-day list (Session, LR-067/LR-ADR-022). form.startDateTime/
	// endDateTime are still sent on the Group itself (required field,
	// kept in sync as "day 1 / last day" for code that reads Group
	// directly without knowing about Session) — derived from `days` at
	// submit time, not bound to an input directly anymore.
	type DayInput = { startDateTime: string; endDateTime: string; venueId: number | null };
	const blankDay = (): DayInput => ({ startDateTime: '', endDateTime: '', venueId: null });
	const MAX_DAYS = 10;
	let days = $state<DayInput[]>([blankDay()]);

	function setDayCount(n: number) {
		const count = Math.min(Math.max(1, n), MAX_DAYS);
		if (count > days.length) {
			days = [...days, ...Array.from({ length: count - days.length }, blankDay)];
		} else if (count < days.length) {
			days = days.slice(0, count);
		}
	}

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
		getVenues()
			.then((data) => (venues = data))
			.catch(() => {});
		getAgeGroups()
			.then((data) => (ageGroups = data))
			.catch(() => {});
	});

	async function startEdit(g: GroupDTO) {
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
			venue: g.venueId ? { id: g.venueId } : null,
			ageGroup: g.ageGroupId ? { id: g.ageGroupId } : null,
			active: g.active,
			// Preserved as-is, not managed by this page — a Course-linked
			// group opened here (e.g. for its title/capacity) must not lose
			// its recurrence config on save.
			course: g.courseId ? { id: g.courseId } : null,
			recurrenceDays: g.recurrenceDays,
			recurrenceStartDate: g.recurrenceStartDate,
			recurrenceEndDate: g.recurrenceEndDate
		};
		try {
			const existing = await getSessions(g.id);
			days =
				existing.length > 0
					? existing.map((s) => ({
							startDateTime: s.startDateTime,
							endDateTime: s.endDateTime ?? '',
							venueId: s.venueId
						}))
					: // No Session rows yet (group predates LR-074, or was created with
						// exactly one day) — fall back to Group's own fields, the
						// "day 1 / only day" values per LR-ADR-022.
						[{ startDateTime: g.startDateTime, endDateTime: g.endDateTime ?? '', venueId: g.venueId ?? null }];
		} catch {
			days = [{ startDateTime: g.startDateTime, endDateTime: g.endDateTime ?? '', venueId: g.venueId ?? null }];
		}
	}

	function cancelEdit() {
		editingId = null;
		form = { ...blank };
		days = [blankDay()];
	}

	// LR-030 — POST /groups takes flat ids (backend: GroupCreateDTO), not
	// the nested-object GroupWriteDTO shape `form` is otherwise kept in
	// (that shape matches the <select> bindings below).
	function toCreateRequest(f: GroupWriteDTO): GroupCreateRequestDTO {
		return {
			titleDe: f.titleDe,
			titleEn: f.titleEn,
			titleUa: f.titleUa,
			capacity: f.capacity,
			startDateTime: f.startDateTime,
			endDateTime: f.endDateTime,
			workshopId: f.workshop?.id ?? null,
			teacherId: f.teacher?.id ?? null,
			activityId: f.activity?.id ?? null,
			venueId: f.venue?.id ?? null,
			ageGroupId: f.ageGroup?.id ?? null,
			active: f.active,
			// This page only ever creates Workshop-linked groups.
			courseId: null,
			recurrenceDays: null,
			recurrenceStartDate: null,
			recurrenceEndDate: null
		};
	}

	// Artefact-audit 2026-08-14 — PUT /groups/{id} now also takes flat ids
	// (backend: GroupUpdateDTO, closed the last raw-entity mass-assignment
	// gap on GroupController). No workshopId — this page never reassigns a
	// group's workshop on edit, same as before this fix.
	function toUpdateRequest(f: GroupWriteDTO): GroupUpdateRequestDTO {
		return {
			titleDe: f.titleDe,
			titleEn: f.titleEn,
			titleUa: f.titleUa,
			capacity: f.capacity,
			startDateTime: f.startDateTime,
			endDateTime: f.endDateTime,
			teacherId: f.teacher?.id ?? null,
			activityId: f.activity?.id ?? null,
			venueId: f.venue?.id ?? null,
			ageGroupId: f.ageGroup?.id ?? null,
			active: f.active,
			courseId: f.course?.id ?? null,
			recurrenceDays: f.recurrenceDays,
			recurrenceStartDate: f.recurrenceStartDate,
			recurrenceEndDate: f.recurrenceEndDate
		};
	}

	async function handleSubmit(e: SubmitEvent) {
		e.preventDefault();
		saving = true;
		try {
			// Group.startDateTime/endDateTime stay in sync as "day 1 / last
			// day" (LR-ADR-022) — the backend re-derives the same values from
			// the Session list on replaceSessions below, this is just so the
			// initial create/update call (before any Session exists) doesn't
			// send a stale or empty value for the required field.
			const sorted = [...days].sort((a, b) => a.startDateTime.localeCompare(b.startDateTime));
			form.startDateTime = sorted[0].startDateTime;
			form.endDateTime = sorted[sorted.length - 1].endDateTime || sorted[sorted.length - 1].startDateTime;

			const groupId =
				editingId !== null
					? (await updateGroup(editingId, toUpdateRequest(form))).id
					: (await createGroup(toCreateRequest(form))).id;

			const sessionPayload: SessionWriteDTO[] = days.map((d) => ({
				startDateTime: d.startDateTime,
				endDateTime: d.endDateTime || null,
				venueId: d.venueId
			}));
			await replaceSessions(groupId, sessionPayload);

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
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="gVenue">{m.admin_group_venue()}</label>
				<select
					id="gVenue"
					value={form.venue?.id ?? ''}
					onchange={(e) => (form.venue = e.currentTarget.value ? { id: Number(e.currentTarget.value) } : null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				>
					<option value="">—</option>
					{#each venues as v (v.id)}
						<option value={v.id}>{v.name}{v.room ? ` — ${v.room}` : ''}</option>
					{/each}
				</select>
			</div>
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="gAgeGroup">{m.admin_group_age_group()}</label>
				<select
					id="gAgeGroup"
					value={form.ageGroup?.id ?? ''}
					onchange={(e) => (form.ageGroup = e.currentTarget.value ? { id: Number(e.currentTarget.value) } : null)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				>
					<option value="">—</option>
					{#each ageGroups as a (a.id)}
						<option value={a.id}>{a.titleDe} ({a.minAge}–{a.maxAge})</option>
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
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="gDayCount">{m.admin_group_day_count()}</label>
				<input
					id="gDayCount"
					type="number"
					min="1"
					max={MAX_DAYS}
					value={days.length}
					oninput={(e) => setDayCount(Number(e.currentTarget.value) || 1)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
		</div>

		<!-- LR-074/LR-067 — one Session row per day (LR-ADR-022), each with
		     its own venue (a multi-day workshop's days can run at different
		     places). Group's own capacity/enrollments above stay shared
		     across every day — one registration per Group, not per day. -->
		<div class="mt-4 space-y-3">
			{#each days as day, i (i)}
				<div class="rounded-lg border border-ink-line p-4">
					<p class="text-sm font-semibold text-paper">{m.admin_group_day_label()} {i + 1}</p>
					<div class="mt-2 grid gap-4 sm:grid-cols-3">
						<div>
							<label class="block text-sm text-paper-dim" for={`gDayStart${i}`}>{m.admin_group_day_start()}</label>
							<input
								id={`gDayStart${i}`}
								type="datetime-local"
								value={day.startDateTime}
								oninput={(e) => (days[i].startDateTime = e.currentTarget.value)}
								required
								class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
							/>
						</div>
						<div>
							<label class="block text-sm text-paper-dim" for={`gDayEnd${i}`}>{m.admin_group_day_end()}</label>
							<input
								id={`gDayEnd${i}`}
								type="datetime-local"
								value={day.endDateTime}
								oninput={(e) => (days[i].endDateTime = e.currentTarget.value)}
								class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
							/>
						</div>
						<div>
							<label class="block text-sm text-paper-dim" for={`gDayVenue${i}`}>{m.admin_group_day_venue()}</label>
							<select
								id={`gDayVenue${i}`}
								value={day.venueId ?? ''}
								onchange={(e) => (days[i].venueId = e.currentTarget.value ? Number(e.currentTarget.value) : null)}
								class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
							>
								<option value="">—</option>
								{#each venues as v (v.id)}
									<option value={v.id}>{v.name}{v.room ? ` — ${v.room}` : ''}</option>
								{/each}
							</select>
						</div>
					</div>
				</div>
			{/each}
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
				{#if g.venueName}<p class="mt-1 text-sm text-paper-dim">{g.venueName}</p>{/if}
				{#if g.ageGroupName}<p class="mt-1 text-sm text-paper-dim">{g.ageGroupName}</p>{/if}
				<p class="mt-1 text-sm text-paper-dim">{g.enrolledCount}/{g.capacity}</p>
				<div class="mt-4 flex gap-3 text-sm">
					<button onclick={() => startEdit(g)} class="text-gold hover:underline">{m.admin_edit()}</button>
					<button onclick={() => handleDelete(g.id)} class="text-error hover:underline">{m.admin_delete()}</button>
				</div>
			</Card>
		{/each}
	</div>
{/if}
