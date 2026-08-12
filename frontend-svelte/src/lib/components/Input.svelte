<script lang="ts">
	import type { HTMLInputAttributes } from 'svelte/elements';

	let {
		id,
		label,
		type = 'text',
		required = false,
		minlength,
		accent = 'gold',
		autocomplete,
		value = $bindable('')
	}: {
		id: string;
		label: string;
		type?: string;
		required?: boolean;
		minlength?: number;
		accent?: 'gold' | 'teal';
		autocomplete?: HTMLInputAttributes['autocomplete'];
		value?: string;
	} = $props();

	const focusClass = { gold: 'focus:border-gold', teal: 'focus:border-teal' };

	// Show/hide toggle for password fields, per usual browser UX
	// convention — found missing 2026-08-12 (customer report while
	// investigating a real registration bug on the same page).
	const isPassword = $derived(type === 'password');
	let revealed = $state(false);
</script>

<label class="mt-4 block text-sm text-paper-dim first:mt-0" for={id}>
	{label}{#if required}<span class="text-gold" aria-hidden="true"> *</span>{/if}
</label>
{#if isPassword}
	<div class="relative mt-1">
		<input
			{id}
			type={revealed ? 'text' : 'password'}
			{required}
			{minlength}
			{autocomplete}
			bind:value
			class="w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 pr-11 text-paper outline-none {focusClass[
				accent
			]}"
		/>
		<button
			type="button"
			tabindex="-1"
			onclick={() => (revealed = !revealed)}
			aria-label={revealed ? 'Passwort verbergen' : 'Passwort anzeigen'}
			class="absolute right-3 top-1/2 -translate-y-1/2 text-paper-dim hover:text-paper"
		>
			{#if revealed}
				<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a18.5 18.5 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
					<line x1="1" y1="1" x2="23" y2="23" />
				</svg>
			{:else}
				<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8Z" />
					<circle cx="12" cy="12" r="3" />
				</svg>
			{/if}
		</button>
	</div>
{:else}
	<input
		{id}
		{type}
		{required}
		{minlength}
		{autocomplete}
		bind:value
		class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none {focusClass[
			accent
		]}"
	/>
{/if}
