<script lang="ts">
	let {
		id,
		label,
		type = 'text',
		required = false,
		minlength,
		accent = 'gold',
		value = $bindable('')
	}: {
		id: string;
		label: string;
		type?: string;
		required?: boolean;
		minlength?: number;
		accent?: 'gold' | 'teal';
		value?: string;
	} = $props();

	const focusClass = { gold: 'focus:border-gold', teal: 'focus:border-teal' };
</script>

<label class="mt-4 block text-sm text-paper-dim first:mt-0" for={id}>{label}</label>
<input
	{id}
	{type}
	{required}
	{minlength}
	bind:value
	class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none {focusClass[
		accent
	]}"
/>
