<script lang="ts">
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import * as m from '$lib/paraglide/messages.js';
	import { verifyEmail } from '$lib/api';
	import Card from '$lib/components/Card.svelte';

	let status = $state<'loading' | 'success' | 'error'>('loading');

	$effect(() => {
		const token = page.url.searchParams.get('token');
		if (!token) {
			status = 'error';
			return;
		}
		verifyEmail(token)
			.then(() => (status = 'success'))
			.catch(() => (status = 'error'));
	});
</script>

<svelte:head>
	<title>{m.site_name()} — {m.verify_email_title()}</title>
</svelte:head>

<section class="mx-auto max-w-md px-6 py-16 sm:py-24">
	<Card>
		<h1 class="font-display text-2xl font-semibold text-paper">{m.verify_email_title()}</h1>

		{#if status === 'loading'}
			<p class="mt-4 text-paper-dim">{m.verify_email_loading()}</p>
		{:else if status === 'success'}
			<p class="mt-4 text-success">{m.verify_email_success()}</p>
			<a href={resolve('/login')} class="mt-6 inline-block text-teal underline hover:no-underline">
				{m.verify_email_go_to_login()}
			</a>
		{:else}
			<p class="mt-4 text-error">{m.verify_email_error()}</p>
			<a href={resolve('/login')} class="mt-6 inline-block text-teal underline hover:no-underline">
				{m.verify_email_go_to_login()}
			</a>
		{/if}
	</Card>
</section>
