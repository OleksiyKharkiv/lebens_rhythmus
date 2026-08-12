<script lang="ts">
	let {
		id,
		label,
		required = false,
		rows = 5,
		value = $bindable('')
	}: {
		id: string;
		label: string;
		required?: boolean;
		rows?: number;
		value?: string;
	} = $props();
</script>

<label class="mt-4 block text-sm text-paper-dim first:mt-0" for={id}>
	{label}{#if required}<span class="text-gold" aria-hidden="true"> *</span>{/if}
</label>
<textarea
	{id}
	{required}
	{rows}
	bind:value
	class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
></textarea>
