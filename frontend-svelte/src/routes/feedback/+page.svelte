<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import { isAuthenticated, submitFeedback, ApiError } from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Button from '$lib/components/Button.svelte';
	import ErrorText from '$lib/components/ErrorText.svelte';
	import Textarea from '$lib/components/Textarea.svelte';
	import Select from '$lib/components/Select.svelte';

	let content = $state('');
	let rating = $state('');
	let busy = $state(false);
	let error = $state('');
	let success = $state(false);

	async function handleSubmit(e: SubmitEvent) {
		e.preventDefault();
		if (!isAuthenticated()) {
			window.location.href = '/login';
			return;
		}
		error = '';
		busy = true;
		try {
			await submitFeedback({ content, rating: rating ? Number(rating) : undefined });
			success = true;
			content = '';
			rating = '';
		} catch (err) {
			error = err instanceof ApiError ? err.message : 'Fehler beim Senden.';
		} finally {
			busy = false;
		}
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.feedback_title()}</title>
</svelte:head>

<section class="mx-auto max-w-xl px-6 py-16 sm:py-24">
	<h1 class="page-title font-display font-semibold text-paper">{m.feedback_title()}</h1>
	<p class="mt-4 text-paper-dim">{m.feedback_intro()}</p>

	<div class="mt-8">
		<Card>
			{#if success}
				<p class="text-success">{m.feedback_success()}</p>
			{:else}
				<form onsubmit={handleSubmit}>
					<Textarea id="content" label={m.feedback_content_label()} required bind:value={content} />

					<Select id="rating" label={m.feedback_rating_label()} bind:value={rating}>
						<option value="">—</option>
						<option value="5">⭐⭐⭐⭐⭐</option>
						<option value="4">⭐⭐⭐⭐</option>
						<option value="3">⭐⭐⭐</option>
						<option value="2">⭐⭐</option>
						<option value="1">⭐</option>
					</Select>

					<ErrorText message={error} />

					<div class="mt-6">
						<Button type="submit" {busy}>{m.feedback_submit()}</Button>
					</div>
				</form>
			{/if}
		</Card>
	</div>
</section>
