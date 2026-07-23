<script lang="ts">
	import type { Snippet } from 'svelte';

	let { children }: { children: Snippet } = $props();
</script>

<div class="rounded-2xl border border-ink-line bg-ink-soft/40 p-8">
	{@render children()}
</div>
