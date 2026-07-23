<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import {
		getAllUsers,
		searchUsers,
		updateUserRole,
		deactivateUser,
		reactivateUser,
		type UserBasicDTO,
		type Role
	} from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Button from '$lib/components/Button.svelte';

	const roles: Role[] = ['USER', 'TEACHER', 'BUSINESS_OWNER', 'CONTENT_MANAGER', 'ADMIN'];

	let users = $state<UserBasicDTO[] | null>(null);
	let error = $state(false);
	let query = $state('');
	let busyId = $state<number | null>(null);

	function load() {
		(query.trim() ? searchUsers(query.trim()) : getAllUsers())
			.then((data) => (users = data))
			.catch(() => (error = true));
	}

	$effect(() => {
		load();
	});

	async function handleRoleChange(user: UserBasicDTO, role: Role) {
		busyId = user.id;
		try {
			await updateUserRole(user.id, role);
			load();
		} finally {
			busyId = null;
		}
	}

	async function handleToggleEnabled(user: UserBasicDTO) {
		busyId = user.id;
		try {
			if (user.enabled) await deactivateUser(user.id);
			else await reactivateUser(user.id);
			load();
		} finally {
			busyId = null;
		}
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.admin_nav_users()}</title>
</svelte:head>

<h1 class="font-display text-3xl font-semibold text-paper">{m.admin_nav_users()}</h1>

<form
	onsubmit={(e) => {
		e.preventDefault();
		load();
	}}
	class="mt-6 flex gap-2"
>
	<input
		bind:value={query}
		placeholder={m.admin_users_search_placeholder()}
		class="w-full max-w-sm rounded-lg border border-ink-line bg-ink px-4 py-2 text-paper outline-none focus:border-gold"
	/>
	<Button type="submit" fullWidth={false}>{m.admin_users_search()}</Button>
</form>

{#if error}
	<p class="mt-8 text-error">{m.state_error()}</p>
{:else if users === null}
	<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
{:else if users.length === 0}
	<p class="mt-8 text-paper-dim">{m.state_empty()}</p>
{:else}
	<div class="mt-8 overflow-x-auto">
		<table class="w-full text-left text-sm">
			<thead class="text-paper-dim">
				<tr class="border-b border-ink-line">
					<th class="py-2 pr-4">{m.admin_users_name()}</th>
					<th class="py-2 pr-4">Email</th>
					<th class="py-2 pr-4">{m.admin_users_role()}</th>
					<th class="py-2 pr-4">{m.admin_users_status()}</th>
					<th class="py-2"></th>
				</tr>
			</thead>
			<tbody>
				{#each users as u (u.id)}
					<tr class="border-b border-ink-line/50 text-paper">
						<td class="py-2 pr-4">{u.firstName} {u.lastName}</td>
						<td class="py-2 pr-4 text-paper-dim">{u.email}</td>
						<td class="py-2 pr-4">
							<select
								value={u.role}
								disabled={busyId === u.id}
								onchange={(e) => handleRoleChange(u, e.currentTarget.value as Role)}
								class="rounded-lg border border-ink-line bg-ink px-2 py-1 text-paper outline-none focus:border-gold"
							>
								{#each roles as r (r)}
									<option value={r}>{r}</option>
								{/each}
							</select>
						</td>
						<td class="py-2 pr-4">
							<span class={u.enabled ? 'text-teal' : 'text-error'}>
								{u.enabled ? m.admin_users_active() : m.admin_users_deactivated()}
							</span>
						</td>
						<td class="py-2">
							<button
								disabled={busyId === u.id}
								onclick={() => handleToggleEnabled(u)}
								class="rounded-full border border-ink-line px-3 py-1 text-xs text-paper transition-colors hover:border-gold disabled:opacity-50"
							>
								{u.enabled ? m.admin_users_deactivate() : m.admin_users_reactivate()}
							</button>
						</td>
					</tr>
				{/each}
			</tbody>
		</table>
	</div>
{/if}
