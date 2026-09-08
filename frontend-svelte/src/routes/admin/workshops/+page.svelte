<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import {
		getWorkshops,
		getWorkshop,
		createWorkshop,
		updateWorkshop,
		deleteWorkshop,
		getTeachers,
		getVenues,
		getGroups,
		createGroup,
		updateGroup,
		getSessions,
		replaceSessions,
		type WorkshopListItem,
		type WorkshopCreateDTO,
		type WorkshopStatus,
		type TeacherInfoDTO,
		type VenueDTO,
		type GroupCreateRequestDTO,
		type GroupUpdateRequestDTO,
		type SessionWriteDTO
	} from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Input from '$lib/components/Input.svelte';
	import Textarea from '$lib/components/Textarea.svelte';
	import Button from '$lib/components/Button.svelte';

	const statuses: WorkshopStatus[] = ['DRAFT', 'PUBLISHED', 'ARCHIVED', 'CANCELLED'];

	let workshops = $state<WorkshopListItem[] | null>(null);
	// LR-072 — Workshop.teacher is a Teacher entity now (was User), same
	// id space as Group.teacher — GET /teachers, not getAllUsers().filter().
	let teachers = $state<TeacherInfoDTO[]>([]);
	let venues = $state<VenueDTO[]>([]);
	let error = $state(false);
	let editingId = $state<number | null>(null);
	let saving = $state(false);
	// UI restructure 2026-08-15 — form is hidden by default; "Create new"
	// button shows it blank, "Edit" on a list card shows it pre-filled.
	let showForm = $state(false);

	const blank: WorkshopCreateDTO = {
		title: '',
		description: '',
		teacherId: null,
		startDate: null,
		endDate: null,
		maxParticipants: null,
		price: null,
		status: 'DRAFT'
	};
	let form = $state<WorkshopCreateDTO>({ ...blank });

	// LR-074 (direct customer request, п.4) — the single Start/Ende pair is
	// replaced with a per-day list (Session, LR-067/LR-ADR-022), same
	// pattern already built on admin/groups — this page just surfaces it
	// inline so the admin doesn't need a second visit to add the schedule,
	// same reasoning as admin/courses's inline Group (LR-081/082). One
	// "primary" Group per Workshop, MVP scope — a workshop needing more
	// than one group/date-set still uses admin/groups directly, unchanged.
	//
	// Workshop.startDate/endDate (the entity's own fields, not Group's)
	// are derived from this list at submit time, not entered directly
	// anymore — WorkshopService.listWorkshops(upcoming=true) filters on
	// them directly, so they can't just go null (Workshop isn't
	// schedule-free like Course, LR-ADR-023 — confirmed with the owner
	// before implementing this, see LR-074's Architecture Pre-Check).
	type DayInput = { startDateTime: string; endDateTime: string; venueId: number | null };
	const blankDay = (): DayInput => ({ startDateTime: '', endDateTime: '', venueId: null });
	const MAX_DAYS = 10;
	let days = $state<DayInput[]>([]);
	let scheduleGroupId = $state<number | null>(null);

	function setDayCount(n: number) {
		const count = Math.min(Math.max(0, n), MAX_DAYS);
		if (count > days.length) {
			days = [...days, ...Array.from({ length: count - days.length }, blankDay)];
		} else if (count < days.length) {
			days = days.slice(0, count);
		}
	}

	function load() {
		getWorkshops(false)
			.then((data) => (workshops = data))
			.catch(() => (error = true));
	}
	$effect(() => {
		load();
		getTeachers()
			.then((data) => (teachers = data))
			.catch(() => {});
		getVenues()
			.then((data) => (venues = data))
			.catch(() => {});
	});

	async function startEdit(w: WorkshopListItem) {
		editingId = w.id;
		showForm = true;
		const detail = await getWorkshop(w.id);
		form = {
			title: detail.title,
			description: detail.description,
			teacherId: detail.teacher?.id ?? null,
			startDate: detail.startDate,
			endDate: detail.endDate,
			maxParticipants: null, // WorkshopDetailDTO doesn't expose this — see KNOWN_ISSUES.md
			price: detail.price,
			status: (detail.status as WorkshopStatus) ?? 'DRAFT'
		};

		try {
			const linkedGroups = await getGroups(w.id);
			const linked = linkedGroups[0]; // one Workshop = one "primary" Group, MVP scope
			if (linked) {
				scheduleGroupId = linked.id;
				const existing = await getSessions(linked.id);
				days =
					existing.length > 0
						? existing.map((s) => ({
								startDateTime: s.startDateTime,
								endDateTime: s.endDateTime ?? '',
								venueId: s.venueId
							}))
						: // No Session rows yet (group predates LR-074, or was created
							// with exactly one day) — fall back to the Group's own fields,
							// the "day 1 / only day" values per LR-ADR-022.
							[{ startDateTime: linked.startDateTime, endDateTime: linked.endDateTime ?? '', venueId: linked.venueId ?? null }];
			} else {
				scheduleGroupId = null;
				days = [];
			}
		} catch {
			scheduleGroupId = null;
			days = [];
		}
	}

	function cancelEdit() {
		editingId = null;
		form = { ...blank };
		scheduleGroupId = null;
		days = [];
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
			// Derive Workshop.startDate/endDate from the day list — see the
			// comment on `days` above for why these can't just be left null.
			if (days.length > 0) {
				const sorted = [...days].sort((a, b) => a.startDateTime.localeCompare(b.startDateTime));
				form.startDate = sorted[0].startDateTime.slice(0, 10);
				form.endDate = (sorted[sorted.length - 1].endDateTime || sorted[sorted.length - 1].startDateTime).slice(0, 10);
			}

			const workshop = editingId !== null ? await updateWorkshop(editingId, form) : await createWorkshop(form);

			if (days.length > 0) {
				const sortedDays = [...days].sort((a, b) => a.startDateTime.localeCompare(b.startDateTime));
				const groupStart = sortedDays[0].startDateTime;
				const groupEnd = sortedDays[sortedDays.length - 1].endDateTime || sortedDays[sortedDays.length - 1].startDateTime;
				// Group.capacity is required and drives the atomic
				// capacityLeft tracking (LR-084) — 15 matches the default
				// already used for Course's own inline schedule section.
				const capacity = form.maxParticipants ?? 15;

				let groupId = scheduleGroupId;
				if (groupId !== null) {
					const groupUpdate: GroupUpdateRequestDTO = {
						titleDe: workshop.title,
						titleEn: workshop.title,
						titleUa: workshop.title,
						capacity,
						startDateTime: groupStart,
						endDateTime: groupEnd,
						teacherId: form.teacherId,
						activityId: null,
						venueId: null,
						ageGroupId: null,
						active: true,
						courseId: null,
						recurrenceDays: null,
						recurrenceStartDate: null,
						recurrenceEndDate: null
					};
					await updateGroup(groupId, groupUpdate);
				} else {
					const groupCreate: GroupCreateRequestDTO = {
						titleDe: workshop.title,
						titleEn: workshop.title,
						titleUa: workshop.title,
						capacity,
						startDateTime: groupStart,
						endDateTime: groupEnd,
						workshopId: workshop.id,
						teacherId: form.teacherId,
						activityId: null,
						venueId: null,
						ageGroupId: null,
						active: true,
						courseId: null,
						recurrenceDays: null,
						recurrenceStartDate: null,
						recurrenceEndDate: null
					};
					const created = await createGroup(groupCreate);
					groupId = created.id;
				}

				const sessionPayload: SessionWriteDTO[] = days.map((d) => ({
					startDateTime: d.startDateTime,
					endDateTime: d.endDateTime || null,
					venueId: d.venueId
				}));
				await replaceSessions(groupId, sessionPayload);
			}

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
			<div>
				<label class="mt-4 block text-sm text-paper-dim first:mt-0" for="wDayCount">{m.admin_group_day_count()}</label>
				<input
					id="wDayCount"
					type="number"
					min="0"
					max={MAX_DAYS}
					value={days.length}
					oninput={(e) => setDayCount(Number(e.currentTarget.value) || 0)}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
				/>
			</div>
		</div>

		<!-- LR-074/LR-067 — one Session row per day (LR-ADR-022), each with
		     its own venue (a multi-day workshop's days can run at different
		     places). Capacity/enrollments live on the Group above, shared
		     across every day — one registration per Group, not per day. -->
		{#if days.length > 0}
			<div class="mt-4 space-y-3">
				{#each days as day, i (i)}
					<div class="rounded-lg border border-ink-line p-4">
						<p class="text-sm font-semibold text-paper">{m.admin_group_day_label()} {i + 1}</p>
						<div class="mt-2 grid gap-4 sm:grid-cols-3">
							<div>
								<label class="block text-sm text-paper-dim" for={`wDayStart${i}`}>{m.admin_group_day_start()}</label>
								<input
									id={`wDayStart${i}`}
									type="datetime-local"
									value={day.startDateTime}
									oninput={(e) => (days[i].startDateTime = e.currentTarget.value)}
									required
									class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
								/>
							</div>
							<div>
								<label class="block text-sm text-paper-dim" for={`wDayEnd${i}`}>{m.admin_group_day_end()}</label>
								<input
									id={`wDayEnd${i}`}
									type="datetime-local"
									value={day.endDateTime}
									oninput={(e) => (days[i].endDateTime = e.currentTarget.value)}
									class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
								/>
							</div>
							<div>
								<label class="block text-sm text-paper-dim" for={`wDayVenue${i}`}>{m.admin_group_day_venue()}</label>
								<select
									id={`wDayVenue${i}`}
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
{/if}

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
