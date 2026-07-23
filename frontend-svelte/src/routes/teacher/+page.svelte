<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import {
		getCurrentUser,
		getWorkshopsByTeacherUserId,
		getTeachers,
		getGroupsByTeacherId,
		getGroupParticipants,
		type WorkshopListItem,
		type GroupDTO,
		type EnrollmentAdminDTO
	} from '$lib/api';
	import Card from '$lib/components/Card.svelte';

	let workshops = $state<WorkshopListItem[] | null>(null);
	let groups = $state<GroupDTO[] | null>(null);
	let teacherRowMissing = $state(false);
	let error = $state(false);
	let expandedGroupId = $state<number | null>(null);
	let participants = $state<Record<number, EnrollmentAdminDTO[]>>({});

	$effect(() => {
		getCurrentUser()
			.then(async (user) => {
				// Workshop.teacher is a User FK — teacherId here = User.id.
				getWorkshopsByTeacherUserId(user.id)
					.then((data) => (workshops = data))
					.catch(() => (error = true));

				// Group.teacher is a Teacher entity FK, a SEPARATE id space from
				// User — there is no link field between the two today (real gap,
				// LR-ADR-004 territory). Interim workaround, confirmed with the
				// product owner 2026-07-23: resolve by matching email against
				// GET /teachers. Fragile if emails ever diverge between the two
				// records — a proper User->Teacher FK is tracked as follow-up
				// tech debt, not solved here.
				const allTeachers = await getTeachers();
				const myTeacherRow = allTeachers.find((t) => t.email === user.email);
				if (!myTeacherRow) {
					teacherRowMissing = true;
					groups = [];
					return;
				}
				getGroupsByTeacherId(myTeacherRow.id)
					.then((data) => (groups = data))
					.catch(() => (error = true));
			})
			.catch(() => (error = true));
	});

	async function toggleParticipants(groupId: number) {
		if (expandedGroupId === groupId) {
			expandedGroupId = null;
			return;
		}
		expandedGroupId = groupId;
		if (!participants[groupId]) {
			try {
				participants[groupId] = await getGroupParticipants(groupId);
			} catch {
				participants[groupId] = [];
			}
		}
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.teacher_dashboard_title()}</title>
</svelte:head>

<h1 class="font-display text-3xl font-semibold text-paper">{m.teacher_dashboard_title()}</h1>

{#if error}
	<p class="mt-8 text-error">{m.state_error()}</p>
{:else if workshops === null || groups === null}
	<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
{:else}
	<div class="mt-10">
		<h2 class="font-display text-xl font-semibold text-paper">{m.teacher_my_workshops()}</h2>
		{#if workshops.length === 0}
			<p class="mt-4 text-paper-dim">{m.state_empty()}</p>
		{:else}
			<div class="mt-4 grid gap-4 sm:grid-cols-2">
				{#each workshops as w (w.id)}
					<Card>
						<h3 class="font-display text-lg font-semibold text-paper">{w.title}</h3>
						<p class="mt-1 text-sm text-paper-dim">{w.status} · {w.startDate ?? '—'}</p>
					</Card>
				{/each}
			</div>
		{/if}
	</div>

	<div class="mt-12">
		<h2 class="font-display text-xl font-semibold text-paper">{m.teacher_my_groups()}</h2>
		{#if teacherRowMissing}
			<p class="mt-4 text-paper-dim">{m.teacher_no_teacher_row()}</p>
		{:else if groups.length === 0}
			<p class="mt-4 text-paper-dim">{m.state_empty()}</p>
		{:else}
			<div class="mt-4 space-y-4">
				{#each groups as g (g.id)}
					<Card>
						<div class="flex items-center justify-between">
							<div>
								<h3 class="font-display text-lg font-semibold text-paper">{g.titleDe}</h3>
								<p class="mt-1 text-sm text-paper-dim">{g.workshopTitle ?? '—'} · {g.enrolledCount}/{g.capacity}</p>
							</div>
							<button
								onclick={() => toggleParticipants(g.id)}
								class="rounded-full border border-ink-line px-4 py-1.5 text-sm text-paper transition-colors hover:border-gold"
							>
								{expandedGroupId === g.id ? m.teacher_hide_participants() : m.teacher_show_participants()}
							</button>
						</div>
						{#if expandedGroupId === g.id}
							<div class="mt-4 border-t border-ink-line pt-4">
								{#if !participants[g.id]}
									<p class="text-sm text-paper-dim">{m.state_loading()}</p>
								{:else if participants[g.id].length === 0}
									<p class="text-sm text-paper-dim">{m.state_empty()}</p>
								{:else}
									<ul class="space-y-1 text-sm text-paper">
										{#each participants[g.id] as p (p.id)}
											<li>{p.user.firstName} {p.user.lastName} — <span class="text-paper-dim">{p.status}</span></li>
										{/each}
									</ul>
								{/if}
							</div>
						{/if}
					</Card>
				{/each}
			</div>
		{/if}
	</div>
{/if}
