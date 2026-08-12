<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { afterNavigate } from '$app/navigation';
	import { locales, localizeHref, getLocale } from '$lib/paraglide/runtime';
	import * as m from '$lib/paraglide/messages.js';
	import { isAuthenticated, clearSession, getStoredRole } from '$lib/api';
	import './layout.css';

	let { children } = $props();

	const localeLabels: Record<string, string> = { de: 'DE', en: 'EN', uk: 'УКР' };

	let mobileOpen = $state(false);
	// isAuthenticated() reads localStorage — only known once mounted in the
	// browser, hence checking it explicitly rather than at initializer time
	// (adapter-static prerenders this layout with no window at all).
	let loggedIn = $state(false);
	let roleAreaHref = $state('/dashboard');
	let roleAreaLabel = $state('');

	// LR-019 — dark is the default/prerendered assumption (matches
	// app.html's blocking inline script, which only ever sets the
	// attribute for 'light'), corrected once mounted in the browser, same
	// reasoning as loggedIn above.
	let theme = $state<'dark' | 'light'>('dark');

	function applyTheme(t: 'dark' | 'light') {
		if (t === 'light') document.documentElement.dataset.theme = 'light';
		else delete document.documentElement.dataset.theme;
		localStorage.setItem('lr-theme', t);
	}

	function toggleTheme() {
		theme = theme === 'dark' ? 'light' : 'dark';
		applyTheme(theme);
	}

	function refreshAuthState() {
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
	}

	// Nav kept showing "Anmelden" after a successful login — isAuthenticated()/
	// getStoredRole() read localStorage, which isn't reactive Svelte state, so
	// a plain $effect() here only ever ran once on mount and never again.
	// SvelteKit's client router doesn't remount this root layout on the
	// goto('/dashboard') that follows login, so nothing re-checked
	// localStorage after persistSession() wrote the new session. afterNavigate
	// fires after every client-side navigation completes (including that
	// post-login redirect) as well as on first load, so this actually reacts
	// to login/logout instead of only ever reflecting whatever was true at
	// the moment this layout first mounted.
	afterNavigate(() => {
		refreshAuthState();
	});

	// Runs once on mount (no reactive reads) — mirrors app.html's inline
	// script so the toggle's displayed icon matches whatever theme was
	// actually applied pre-hydration, not always assuming 'dark'.
	$effect(() => {
		theme = localStorage.getItem('lr-theme') === 'light' ? 'light' : 'dark';
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
		<div class="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
			<a
				href={resolve('/')}
				class="shrink-0 font-display text-lg font-semibold whitespace-nowrap text-paper tracking-wide"
			>
				{m.site_name()}
			</a>

			<!-- Beta report, 2026-08-12: at gap-8/sm: (640px) the full nav no
			     longer fit its container once "Kurse" was added (LR-076) —
			     overflowed past ~1024px viewports with nothing to wrap or
			     contain it (see layout.css's new overflow-x: hidden for the
			     page-wide symptom this caused). Tightened spacing + moved the
			     breakpoint out to lg: (1024px) so anything narrower gets the
			     hamburger menu instead of a cramped/overflowing full nav. -->
			<nav class="hidden items-center gap-5 lg:flex">
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
				<a href="/courses" class="text-paper-dim hover:text-gold transition-colors">
					{m.nav_courses()}
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
							href={localizeHref(page.url.pathname, { locale })}
							class={`transition-colors ${getLocale() === locale ? 'text-gold' : 'text-paper-dim hover:text-paper'}`}
						>
							{localeLabels[locale] ?? locale}
						</a>
					{/each}
				</div>

				<button
					onclick={toggleTheme}
					aria-label={theme === 'dark' ? m.nav_theme_switch_to_light() : m.nav_theme_switch_to_dark()}
					class="text-paper-dim hover:text-gold ml-2 transition-colors"
				>
					{#if theme === 'dark'}
						<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
							<circle cx="12" cy="12" r="4" />
							<path
								stroke-linecap="round"
								d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"
							/>
						</svg>
					{:else}
						<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
							<path stroke-linecap="round" stroke-linejoin="round" d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5Z" />
						</svg>
					{/if}
				</button>
			</nav>

			<button
				class="text-paper lg:hidden"
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
			<nav class="flex flex-col gap-4 border-t border-ink-line px-6 py-4 lg:hidden">
				<a href={resolve('/')} onclick={closeMobileMenu} class="text-paper-dim">{m.nav_home()}</a>
				<a href={resolve('/about')} onclick={closeMobileMenu} class="text-paper-dim">{m.nav_about()}</a>
				<a href="/activities" onclick={closeMobileMenu} class="text-paper-dim">{m.nav_activities()}</a>
				<a href="/workshops" onclick={closeMobileMenu} class="text-paper-dim">{m.nav_workshops()}</a>
				<a href="/courses" onclick={closeMobileMenu} class="text-paper-dim">{m.nav_courses()}</a>
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
				<div class="flex items-center justify-between pt-2">
					<div class="flex gap-4 text-sm">
						{#each locales as locale (locale)}
							<a
								href={localizeHref(page.url.pathname, { locale })}
								onclick={closeMobileMenu}
								class="text-paper-dim"
							>
								{localeLabels[locale] ?? locale}
							</a>
						{/each}
					</div>
					<button onclick={toggleTheme} aria-label={theme === 'dark' ? m.nav_theme_switch_to_light() : m.nav_theme_switch_to_dark()} class="text-paper-dim">
						{#if theme === 'dark'}
							<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
								<circle cx="12" cy="12" r="4" />
								<path
									stroke-linecap="round"
									d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"
								/>
							</svg>
						{:else}
							<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
								<path stroke-linecap="round" stroke-linejoin="round" d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5Z" />
							</svg>
						{/if}
					</button>
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
