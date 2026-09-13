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
	aria-busy={busy}
	{onclick}
	class="rounded-full px-6 py-3 font-display font-semibold transition-colors disabled:opacity-60 {fullWidth
		? 'w-full'
		: ''} {variantClass[variant]}"
>
	{#if busy}
		<span class="inline-flex items-center justify-center">
			<svg
				class="h-5 w-5 animate-spin text-current"
				xmlns="http://www.w3.org/2000/svg"
				fill="none"
				viewBox="0 0 24 24"
				aria-hidden="true"
				data-testid="spinner"
			>
				<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
				<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
			</svg>
		</span>
	{:else}
		{@render children()}
	{/if}
</button>
