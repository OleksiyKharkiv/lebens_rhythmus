<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import { isAuthenticated, getStoredRole } from '$lib/api';

	let { children } = $props();

	let ready = $state(false);

	// Client-side only, same reasoning as dashboard/+page.svelte and
	// +layout.svelte's loggedIn check — adapter-static has no server here.
	// This is a UX guard, not the security boundary: every admin write
	// endpoint has its own @PreAuthorize (ADMIN or BUSINESS_OWNER, some
	// ADMIN-only — see SecurityConfig/individual controllers), which is
	// what actually enforces access.
	$effect(() => {
		if (!isAuthenticated()) {
			window.location.href = '/login';
			return;
		}
		const role = getStoredRole();
		if (role !== 'ADMIN' && role !== 'BUSINESS_OWNER') {
			window.location.href = '/dashboard';
			return;
		}
		ready = true;
	});

	const navItems = [
		{ href: '/admin', label: m.admin_nav_overview() },
		{ href: '/admin/users', label: m.admin_nav_users() },
		{ href: '/admin/activities', label: m.admin_nav_activities() },
		{ href: '/admin/workshops', label: m.admin_nav_workshops() },
		{ href: '/admin/groups', label: m.admin_nav_groups() },
		{ href: '/admin/venues', label: m.admin_nav_venues() },
		{ href: '/admin/performances', label: m.admin_nav_performances() }
	];
</script>

{#if ready}
	<div class="mx-auto max-w-6xl px-6 py-12 sm:py-16">
		<nav class="mb-10 flex flex-wrap gap-2 border-b border-ink-line pb-6">
			{#each navItems as item (item.href)}
				<a
					href={item.href}
					class="rounded-full border border-ink-line px-4 py-1.5 text-sm text-paper-dim transition-colors hover:border-gold hover:text-paper"
				>
					{item.label}
				</a>
			{/each}
		</nav>
		{@render children()}
	</div>
{/if}
