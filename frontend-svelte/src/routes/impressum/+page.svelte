<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';

	// Legal pages are deliberately German-only content (not run through
	// Paraglide messages) — see LR-ADR-013 (DE is the legally binding
	// language for a German business) and the compliance fix in
	// frontend/pages/impressum/impressum.html (2026-07-22), ported here
	// verbatim. Do not machine-translate this page.
</script>

<svelte:head>
	<title>{m.site_name()} — Impressum</title>
</svelte:head>

<section class="mx-auto max-w-2xl px-6 py-16 sm:py-24">
	<h1 class="font-display text-3xl font-semibold text-paper sm:text-4xl">Impressum</h1>
	<p class="mt-2 text-paper-dim">Rechtliche Informationen</p>

	<div class="mt-10 space-y-8 text-paper-dim">
		<div>
			<h2 class="font-display text-lg font-semibold text-paper">Angaben gemäß § 5 TMG</h2>
			<p class="mt-2"><strong class="text-paper">Olena Khudoshyna</strong></p>
			<p>Theater Lab 29 – Studio für kreative Entwicklung</p>
			<p>Ritterspornweg 1<br />50129 Bergheim<br />Deutschland</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">Kontakt</h2>
			<p class="mt-2">
				<strong class="text-paper">E-Mail:</strong>
				<a href="mailto:info@tlab29.com" class="text-gold hover:underline">info@tlab29.com</a>
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">Umsatzsteuer</h2>
			<p class="mt-2">Als Kleinunternehmer im Sinne von § 19 Abs. 1 UStG wird Umsatzsteuer nicht berechnet.</p>
		</div>

		<div class="border-ink-line border-t pt-8">
			<h2 class="font-display text-lg font-semibold text-paper">Verantwortlich für den Inhalt</h2>
			<p class="mt-2">Gemäß § 18 Abs. 2 MStV:</p>
			<p><strong class="text-paper">Olena Khudoshyna</strong><br />Ritterspornweg 1, 50129 Bergheim</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">Streitschlichtung</h2>
			<p class="mt-2">
				Die Europäische Kommission stellt eine Plattform zur Online-Streitbeilegung (OS) bereit:
				<a href="https://ec.europa.eu/consumers/odr/" class="text-gold hover:underline"
					>https://ec.europa.eu/consumers/odr/</a
				>
			</p>
			<p class="mt-2">Unsere E-Mail-Adresse finden Sie oben im Impressum.</p>
			<p class="mt-2">
				Wir sind nicht bereit oder verpflichtet, an Streitbeilegungsverfahren vor einer
				Verbraucherschlichtungsstelle teilzunehmen (§ 36 VSBG).
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">Haftungsausschluss</h2>
			<p class="mt-2">
				Trotz sorgfältiger inhaltlicher Kontrolle übernehmen wir keine Haftung für die Inhalte externer
				Links. Für den Inhalt der verlinkten Seiten sind ausschließlich deren Betreiber verantwortlich.
			</p>
		</div>
	</div>

	<p class="mt-12 text-xs text-paper-dim/70">
		© 2026 Theater Lab 29. Alle Rechte vorbehalten. Die Inhalte dieser Website sind urheberrechtlich
		geschützt.
	</p>
</section>
