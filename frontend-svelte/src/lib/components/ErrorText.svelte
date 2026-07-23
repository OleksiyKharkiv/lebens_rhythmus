<script lang="ts">
	let { message }: { message: string } = $props();
</script>

{#if message}
	<p class="mt-3 text-sm text-error" role="alert">{message}</p>
{/if}
