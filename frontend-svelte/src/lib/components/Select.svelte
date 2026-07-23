<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		id,
		label,
		value = $bindable(''),
		children
	}: {
		id: string;
		label: string;
		value?: string;
		children: Snippet;
	} = $props();
</script>

<label class="mt-4 block text-sm text-paper-dim first:mt-0" for={id}>{label}</label>
<select
	{id}
	bind:value
	class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
>
	{@render children()}
</select>
