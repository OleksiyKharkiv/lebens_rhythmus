<script lang="ts">
	import type { Pathname } from '$app/types';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { locales, localizeHref, getLocale } from '$lib/paraglide/runtime';
	import * as m from '$lib/paraglide/messages.js';
	import { isAuthenticated, clearSession, getStoredRole } from '$lib/api';
	import './layout.css';

	let { children } = $props();

	const localeLabels: Record<string, string> = { de: 'DE', en: 'EN', uk: 'УКР' };

	let mobileOpen = $state(false);
	// isAuthenticated() reads localStorage — only known once mounted in the
	// browser, hence the effect rather than an initializer (adapter-static
	// prerenders this layout with no window at all).
	let loggedIn = $state(false);
	let roleAreaHref = $state('/dashboard');
	let roleAreaLabel = $state('');
	$effect(() => {
		loggedIn = isAuthenticated();
		const role = getStoredRole();
		if (role === 'ADMIN' || role === 'BUSINESS_OWNER') {
			roleAreaHref = '/admin';
			roleAreaLabel = m.nav_admin();
		} else if (role === 'TEACHER') {
			roleAreaHref = '/teacher';
			roleAreaLabel = m.nav_teacher();
		} else {
			roleAreaHref = '/dashboard';
			roleAreaLabel = m.nav_dashboard();
		}
	});

	function handleLogout() {
		clearSession();
		loggedIn = false;
		window.location.href = '/';
	}

	// The mobile nav stayed open after picking a link — SvelteKit's client
	// router doesn't remount this layout on navigation, so mobileOpen just
	// kept sitting at true (found in production, iPhone 14 portrait: menu
	// stuck open eating ~60% of the viewport after tapping "About").
	function closeMobileMenu() {
		mobileOpen = false;
	}
</script>

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
				<a href="/activities" class="text-paper-dim hover:text-gold transition-colors">
					{m.nav_activities()}
				</a>
				<a href="/workshops" class="text-paper-dim hover:text-gold transition-colors">
					{m.nav_workshops()}
				</a>
				<a href="/performances" class="text-paper-dim hover:text-gold transition-colors">
					{m.nav_performances()}
				</a>
				{#if loggedIn}
					<a href={roleAreaHref} class="text-paper-dim hover:text-gold transition-colors">
						{roleAreaLabel}
					</a>
					<button
						onclick={handleLogout}
						class="rounded-full border border-gold/70 px-4 py-1.5 text-paper transition-colors hover:bg-gold hover:text-ink"
					>
						{m.nav_logout()}
					</button>
				{:else}
					<a
						href={resolve('/login')}
						class="rounded-full border border-gold/70 px-4 py-1.5 text-paper transition-colors hover:bg-gold hover:text-ink"
					>
						{m.nav_login()}
					</a>
				{/if}
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
				<a href={resolve('/')} onclick={closeMobileMenu} class="text-paper-dim">{m.nav_home()}</a>
				<a href={resolve('/about')} onclick={closeMobileMenu} class="text-paper-dim">{m.nav_about()}</a>
				<a href="/activities" onclick={closeMobileMenu} class="text-paper-dim">{m.nav_activities()}</a>
				<a href="/workshops" onclick={closeMobileMenu} class="text-paper-dim">{m.nav_workshops()}</a>
				<a href="/performances" onclick={closeMobileMenu} class="text-paper-dim">{m.nav_performances()}</a>
				{#if loggedIn}
					<a href={roleAreaHref} onclick={closeMobileMenu} class="text-paper-dim">{roleAreaLabel}</a>
					<button
						onclick={() => {
							closeMobileMenu();
							handleLogout();
						}}
						class="text-left text-gold"
					>
						{m.nav_logout()}
					</button>
				{:else}
					<a href={resolve('/login')} onclick={closeMobileMenu} class="text-gold">{m.nav_login()}</a>
				{/if}
				<div class="flex gap-4 pt-2 text-sm">
					{#each locales as locale (locale)}
						<a
							href={resolve(localizeHref(page.url.pathname, { locale }) as Pathname)}
							onclick={closeMobileMenu}
							class="text-paper-dim"
						>
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
		<nav class="flex flex-wrap items-center justify-center gap-x-6 gap-y-2">
			<a href="/contact" class="hover:text-paper">{m.nav_contact()}</a>
			<a href="/corporate" class="hover:text-paper">{m.nav_corporate()}</a>
			<a href="/feedback" class="hover:text-paper">{m.feedback_title()}</a>
		</nav>
		<nav class="mt-3 flex flex-wrap items-center justify-center gap-x-6 gap-y-2">
			<a href="/impressum" class="hover:text-paper">{m.footer_impressum()}</a>
			<a href="/datenschutz" class="hover:text-paper">{m.footer_datenschutz()}</a>
			<a href="/agb" class="hover:text-paper">{m.footer_agb()}</a>
			<a href="/widerruf" class="hover:text-paper">{m.footer_widerruf()}</a>
		</nav>
		<p class="mt-4">© {new Date().getFullYear()} {m.site_name()} · TLab29</p>
		<p class="mt-1 text-xs text-paper-dim/60">{m.footer_legal_note()}</p>
	</footer>
</div>
