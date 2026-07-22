<script lang="ts">
	import type { Pathname } from '$app/types';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { locales, localizeHref, getLocale } from '$lib/paraglide/runtime';
	import * as m from '$lib/paraglide/messages.js';
	import './layout.css';
	import favicon from '$lib/assets/favicon.svg';

	let { children } = $props();

	const localeLabels: Record<string, string> = { de: 'DE', en: 'EN', uk: 'УКР' };

	let mobileOpen = $state(false);
</script>

<svelte:head><link rel="icon" href={favicon} /></svelte:head>

<div class="flex min-h-screen flex-col bg-ink text-paper">
	<header
		class="border-ink-line/60 sticky top-0 z-20 border-b bg-ink/90 backdrop-blur"
		style="background-color: color-mix(in srgb, var(--color-ink) 90%, transparent);"
	>
		<div class="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
			<a href={resolve('/')} class="font-display text-lg font-semibold tracking-wide text-paper">
				{m.site_name()}
			</a>

			<nav class="hidden items-center gap-8 sm:flex">
				<a href={resolve('/')} class="text-paper-dim hover:text-gold transition-colors">
					{m.nav_home()}
				</a>
				<a href={resolve('/about')} class="text-paper-dim hover:text-gold transition-colors">
					{m.nav_about()}
				</a>
				<a
					href={resolve('/login')}
					class="rounded-full border border-gold/70 px-4 py-1.5 text-paper transition-colors hover:bg-gold hover:text-ink"
				>
					{m.nav_login()}
				</a>
				<div class="flex items-center gap-2 border-l border-ink-line pl-6 text-sm">
					{#each locales as locale (locale)}
						<a
							href={resolve(localizeHref(page.url.pathname, { locale }) as Pathname)}
							class={`transition-colors ${getLocale() === locale ? 'text-gold' : 'text-paper-dim hover:text-paper'}`}
						>
							{localeLabels[locale] ?? locale}
						</a>
					{/each}
				</div>
			</nav>

			<button
				class="text-paper sm:hidden"
				aria-label="Menu"
				aria-expanded={mobileOpen}
				onclick={() => (mobileOpen = !mobileOpen)}
			>
				<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
					<path stroke-linecap="round" d="M4 6h16M4 12h16M4 18h16" />
				</svg>
			</button>
		</div>

		{#if mobileOpen}
			<nav class="flex flex-col gap-4 border-t border-ink-line px-6 py-4 sm:hidden">
				<a href={resolve('/')} class="text-paper-dim">{m.nav_home()}</a>
				<a href={resolve('/about')} class="text-paper-dim">{m.nav_about()}</a>
				<a href={resolve('/login')} class="text-gold">{m.nav_login()}</a>
				<div class="flex gap-4 pt-2 text-sm">
					{#each locales as locale (locale)}
						<a href={resolve(localizeHref(page.url.pathname, { locale }) as Pathname)} class="text-paper-dim">
							{localeLabels[locale] ?? locale}
						</a>
					{/each}
				</div>
			</nav>
		{/if}
	</header>

	<main class="flex-1">
		{@render children()}
	</main>

	<footer class="border-t border-ink-line px-6 py-8 text-center text-sm text-paper-dim">
		© {new Date().getFullYear()} {m.site_name()} · TLab29
	</footer>
</div>
