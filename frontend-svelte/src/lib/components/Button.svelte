<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		variant = 'gold',
		type = 'button',
		disabled = false,
		busy = false,
		fullWidth = true,
		onclick,
		children
	}: {
		variant?: 'gold' | 'teal';
		type?: 'button' | 'submit';
		disabled?: boolean;
		busy?: boolean;
		fullWidth?: boolean;
		onclick?: () => void;
		children: Snippet;
	} = $props();

	const variantClass = {
		gold: 'bg-gold text-ink hover:bg-gold-deep',
		teal: 'bg-teal text-ink hover:bg-teal-deep'
	};
</script>

<button
	{type}
	disabled={disabled || busy}
	{onclick}
	class="rounded-full px-6 py-3 font-display font-semibold transition-colors disabled:opacity-60 {fullWidth
		? 'w-full'
		: ''} {variantClass[variant]}"
>
	{#if busy}…{:else}{@render children()}{/if}
</button>
