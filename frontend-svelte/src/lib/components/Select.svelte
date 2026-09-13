<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		id,
		label,
		ariaLabel,
		disabled = false,
		value = $bindable(''),
		class: customClass = '',
		onchange,
		children
	}: {
		id?: string;
		label?: string;
		ariaLabel?: string;
		disabled?: boolean;
		value?: any;
		class?: string;
		onchange?: (value: string) => void;
		children: Snippet;
	} = $props();

	function handleChange(e: Event & { currentTarget: EventTarget & HTMLSelectElement }) {
		const val = e.currentTarget.value;
		value = val;
		if (onchange) {
			onchange(val);
		}
	}
</script>

{#if label}
	<label class="mt-4 block text-sm text-paper-dim first:mt-0" for={id}>{label}</label>
{/if}
<select
	{id}
	{disabled}
	aria-label={ariaLabel}
	bind:value
	onchange={handleChange}
	class="rounded-lg border border-ink-line bg-ink text-paper outline-none focus:border-gold disabled:opacity-50 {customClass ||
		'mt-1 w-full px-4 py-2.5'}"
>
	{@render children()}
</select>
