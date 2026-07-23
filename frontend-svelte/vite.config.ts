import { paraglideVitePlugin } from '@inlang/paraglide-js';
import tailwindcss from '@tailwindcss/vite';
import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig as defineViteConfig, mergeConfig } from 'vite';
import { defineConfig as defineVitestConfig } from 'vitest/config';

const viteConfig = defineViteConfig({
	// Vite/Vitest otherwise resolves Svelte's server (SSR) build under
	// Node — components then throw "mount(...) is not available on the
	// server". Scoped to `process.env.VITEST` only: setting this globally
	// would break `vite build`'s actual prerendering step, which needs the
	// real server build.
	resolve: process.env.VITEST ? { conditions: ['browser'] } : undefined,
	plugins: [
		tailwindcss(),
		sveltekit({
			compilerOptions: {
				// Force runes mode for the project, except for libraries. Can be removed in svelte 6.
				runes: ({ filename }) =>
					filename.split(/[/\\]/).includes('node_modules') ? undefined : true
			},
			// SPA fallback mode, not prerendering every route: every page here
			// fetches its data client-side via $effect (never SvelteKit load()),
			// and several routes are inherently dynamic/auth-gated (dashboard,
			// admin/*, teacher, workshops/[id]) — trying to force
			// prerender=true across all 23 routes fights the app's actual
			// shape instead of matching it. adapter-static's own error message
			// suggests this first: "set the fallback option". The static
			// server (nginx, per the CI/Docker work this unblocks) needs a
			// catch-all `try_files $uri /index.html;` rule for deep links to
			// this fallback shell to work — not yet wired, tracked as part of
			// LR-002's remaining CI/Docker/Helm work, not solved here.
			adapter: adapter({ fallback: 'index.html' })
		}),

		paraglideVitePlugin({
			project: './project.inlang',
			outdir: './src/lib/paraglide',
			emitTsDeclarations: true,
			// Without "url" first, visiting /en or /uk directly (or a fresh
			// tab with no cookie set yet) silently fell back to baseLocale
			// (de) — the URL prefix was only ever used for generating links
			// (localizeHref), never for detecting the active locale.
			strategy: ['url', 'cookie', 'baseLocale']
		})
	]
});

// Kept as a separate defineConfig + mergeConfig (Vitest's own recommended
// pattern) rather than one defineConfig from 'vitest/config' — this project's
// `vite` version and the one vitest bundles internally don't structurally
// match on the Plugin type, which broke `svelte-check` with a large type
// error even though the tests themselves ran fine.
const vitestConfig = defineVitestConfig({
	test: {
		environment: 'jsdom',
		setupFiles: ['./vitest-setup.ts'],
		// Component tests only — Playwright e2e is deliberately not installed
		// yet (see LR-ADR-020): no CI stage exists to run it and no reachable
		// backend for the frontend repo to hit, both blocked on LR-002.
		include: ['src/**/*.{test,spec}.ts']
	}
});

export default mergeConfig(viteConfig, vitestConfig);
