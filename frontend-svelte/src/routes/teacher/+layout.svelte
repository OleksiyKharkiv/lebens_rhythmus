<script lang="ts">
	import { isAuthenticated, getStoredRole } from '$lib/api';

	let { children } = $props();

	let ready = $state(false);

	// Same client-side-guard caveat as admin/+layout.svelte — the real
	// boundary is the backend's @PreAuthorize on each teacher-scoped
	// endpoint (hasRole('TEACHER') or BUSINESS_OWNER/ADMIN).
	$effect(() => {
		if (!isAuthenticated()) {
			window.location.href = '/login';
			return;
		}
		const role = getStoredRole();
		if (role !== 'TEACHER' && role !== 'BUSINESS_OWNER' && role !== 'ADMIN') {
			window.location.href = '/dashboard';
			return;
		}
		ready = true;
	});
</script>

{#if ready}
	<div class="mx-auto max-w-5xl px-6 py-12 sm:py-16">
		{@render children()}
	</div>
{/if}
