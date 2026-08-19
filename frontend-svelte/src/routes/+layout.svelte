<script lang="ts">
	import { page } from '$app/state';
	import { afterNavigate } from '$app/navigation';
	// Bug report 2026-08-17 — only the "My area" link preserved the active
	// language; every other nav/footer link was a bare unprefixed href
	// ("/courses" etc., some via SvelteKit's resolve()). With
	// strategy: ['url', 'cookie', 'baseLocale'] (vite.config.ts), an
	// unprefixed href always resolves to baseLocale ('de') — clicking any
	// of them silently reset the language. localizeHref(path) (no locale
	// option) defaults to the CURRENT locale, so it's the fix for every
	// internal link, not just the language-switcher links that already
	// used it. Deliberately NOT combined with resolve() here — an earlier
	// resolve(localizeHref(...)) attempt on this same file broke
	// svelte-check because resolve() needs a literal route id, not a
	// dynamically localized string (see docs/tickets/archive.md).
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
		window.location.href = localizeHref('/');
	}

	// The mobile nav stayed open after picking a link — SvelteKit's client
	// router doesn't remount this layout on navigation, so mobileOpen just
	// kept sitting at true (found in production, iPhone 14 portrait: menu
	// stuck open eating ~60% of the viewport after tapping "About").
	function closeMobileMenu() {
		mobileOpen = false;
	}
</script>

<!-- No bg-ink here (2026-08-17) — this div's own opaque background was
     painting over layout.css's body::before background-art layer,
     which sits at z-index:-1 behind normal-flow content. html's own
     `background: var(--color-ink)` (layout.css) already provides the
     same solid fallback color underneath, so this div only needs to
     stay transparent and let that show through. -->
<div class="flex min-h-screen flex-col text-paper">
	<!-- Bumped 90% -> 96% (2026-08-17, beta feedback) — the new page
	     background art (layout.css) shows through backdrop-blur enough
	     at 90% to visibly reduce nav-link contrast, something that
	     wasn't a problem when the page behind the header was ever just
	     a flat ground color. -->
	<header
		class="border-ink-line/60 sticky top-0 z-20 border-b bg-ink/96 backdrop-blur"
		style="background-color: color-mix(in srgb, var(--color-ink) 96%, transparent);"
	>
		<!-- UI fix 2026-08-15 (beta feedback) — was `mx-auto max-w-6xl`, same
		     reading-width constraint the page body uses. Correct for body
		     copy, wrong for a navbar: on wide monitors the whole header sat
		     in a narrow centered box, so logo/utility controls read as
		     "clumped in the middle" instead of anchored to the real left/
		     right edges of the window. Full-width bar now, 3-column grid
		     `1fr auto 1fr` (not flex + flex-1 — that only centers the nav
		     within logo/lang-block leftover space, which is off-center
		     whenever those two differ in width; grid's outer columns are
		     equal by definition, so the middle column is exactly centered
		     regardless of what's in the side columns). -->
		<div class="grid grid-cols-[1fr_auto_1fr] items-center px-6 py-4">
			<a
				href={localizeHref('/')}
				class="shrink-0 justify-self-start font-display text-lg font-semibold whitespace-nowrap text-paper tracking-wide"
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
			<nav class="hidden items-center justify-self-center gap-5 lg:flex">
				<a href={localizeHref('/')} class="text-paper-dim hover:text-gold transition-colors">
					{m.nav_home()}
				</a>
				<a href={localizeHref('/about')} class="text-paper-dim hover:text-gold transition-colors">
					{m.nav_about()}
				</a>
				<a href={localizeHref('/activities')} class="text-paper-dim hover:text-gold transition-colors">
					{m.nav_activities()}
				</a>
				<a href={localizeHref('/workshops')} class="text-paper-dim hover:text-gold transition-colors">
					{m.nav_workshops()}
				</a>
				<a href={localizeHref('/courses')} class="text-paper-dim hover:text-gold transition-colors">
					{m.nav_courses()}
				</a>
				<a href={localizeHref('/performances')} class="text-paper-dim hover:text-gold transition-colors">
					{m.nav_performances()}
				</a>
				{#if loggedIn}
					<a href={localizeHref(roleAreaHref)} class="text-paper-dim hover:text-gold transition-colors">
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
						href={localizeHref('/login')}
						class="rounded-full border border-gold/70 px-4 py-1.5 text-paper transition-colors hover:bg-gold hover:text-ink"
					>
						{m.nav_login()}
					</a>
				{/if}
			</nav>

			<!-- Language + theme — pinned to the right edge, separate from the
			     centered nav group above (was inside the same <nav>, which is
			     why it used to drift with the nav block instead of anchoring). -->
			<div class="hidden items-center justify-self-end gap-2 lg:flex">
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
			</div>

			<button
				class="col-start-3 justify-self-end text-paper lg:hidden"
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
				<a href={localizeHref('/')} onclick={closeMobileMenu} class="text-paper-dim">{m.nav_home()}</a>
				<a href={localizeHref('/about')} onclick={closeMobileMenu} class="text-paper-dim">{m.nav_about()}</a>
				<a href={localizeHref('/activities')} onclick={closeMobileMenu} class="text-paper-dim">{m.nav_activities()}</a>
				<a href={localizeHref('/workshops')} onclick={closeMobileMenu} class="text-paper-dim">{m.nav_workshops()}</a>
				<a href={localizeHref('/courses')} onclick={closeMobileMenu} class="text-paper-dim">{m.nav_courses()}</a>
				<a href={localizeHref('/performances')} onclick={closeMobileMenu} class="text-paper-dim">{m.nav_performances()}</a>
				{#if loggedIn}
					<a href={localizeHref(roleAreaHref)} onclick={closeMobileMenu} class="text-paper-dim">{roleAreaLabel}</a>
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
					<a href={localizeHref('/login')} onclick={closeMobileMenu} class="text-gold">{m.nav_login()}</a>
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

	<!-- UI fix 2026-08-15 (beta feedback) — was 4 stacked block-level rows
	     (nav, nav, p, p), each forced onto its own line regardless of how
	     much horizontal room was actually free. Functional/legal/disclaimer
	     now share one flex-wrap row with thin dividers; copyright on its
	     own line below. flex-wrap still degrades to stacked lines on narrow
	     viewports, same as before. -->
	<footer class="border-t border-ink-line px-6 py-8 text-center text-sm text-paper-dim">
		<!-- UI fix 2026-08-15 (round 2, beta feedback) — a single flex row
		     with one uniform gap-x on 5 children (nav, divider, nav, divider,
		     p) puts equal space on both sides of the divider between any two
		     neighbors — the legal block (center) ends up equidistant from
		     the functional block (left) and the disclaimer (right) by
		     construction, without pinning either to the viewport edge (that
		     was explicitly not wanted). Widened both the inter-block gap and
		     the gap between links within each block — was too cramped. -->
		<div class="flex flex-wrap items-center justify-center gap-x-10 gap-y-3">
			<nav class="flex flex-wrap items-center justify-center gap-x-8 gap-y-2">
				<a href={localizeHref('/contact')} class="hover:text-paper">{m.nav_contact()}</a>
				<a href={localizeHref('/corporate')} class="hover:text-paper">{m.nav_corporate()}</a>
				<a href={localizeHref('/feedback')} class="hover:text-paper">{m.feedback_title()}</a>
			</nav>
			<span class="hidden text-paper-dim/30 sm:inline" aria-hidden="true">|</span>
			<nav class="flex flex-wrap items-center justify-center gap-x-8 gap-y-2">
				<a href={localizeHref('/impressum')} class="hover:text-paper">{m.footer_impressum()}</a>
				<a href={localizeHref('/datenschutz')} class="hover:text-paper">{m.footer_datenschutz()}</a>
				<a href={localizeHref('/agb')} class="hover:text-paper">{m.footer_agb()}</a>
				<a href={localizeHref('/widerruf')} class="hover:text-paper">{m.footer_widerruf()}</a>
			</nav>
			<span class="hidden text-paper-dim/30 sm:inline" aria-hidden="true">|</span>
			<p class="text-xs text-paper-dim/60">{m.footer_legal_note()}</p>
		</div>
		<p class="mt-3">© {new Date().getFullYear()} {m.site_name()} · TLab29</p>
	</footer>
</div>
