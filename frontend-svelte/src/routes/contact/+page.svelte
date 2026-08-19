<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';

	const address = 'Quartiersbüro im Euel 2, 50129 Bergheim-Niederaußem';
	// A live Google Maps <iframe> (as the old site had) sends visitor data to
	// Google on every page load without consent — same DSGVO concern as the
	// Google Fonts CDN we already avoided (see layout.css). A plain link the
	// visitor chooses to click doesn't have that problem.
	const mapHref = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(address)}`;
</script>

<svelte:head>
	<title>{m.site_name()} — {m.contact_title()}</title>
</svelte:head>

<section class="mx-auto max-w-2xl px-6 py-16 sm:py-24">
	<h1 class="page-title font-display font-semibold text-paper">{m.contact_title()}</h1>
	<p class="mt-4 text-paper-dim">{m.contact_intro()}</p>

	<dl class="mt-10 space-y-4 text-paper-dim">
		<div>
			<dt class="font-semibold text-paper">{m.contact_person()}</dt>
			<dd>Olena Khudoshyna</dd>
		</div>
		<div>
			<dt class="font-semibold text-paper">{m.contact_whatsapp()}</dt>
			<dd><a href="tel:+4917683394253" class="text-gold hover:underline">+49 176 83394253</a></dd>
		</div>
		<div>
			<dt class="font-semibold text-paper">E-Mail</dt>
			<dd><a href="mailto:info@tlab29.com" class="text-gold hover:underline">info@tlab29.com</a></dd>
		</div>
		<div>
			<dt class="font-semibold text-paper">{m.contact_address()}</dt>
			<dd>
				{address}
				<br />
				<a href={mapHref} target="_blank" rel="noopener noreferrer" class="text-teal hover:underline">
					{m.contact_map_link()} ↗
				</a>
			</dd>
		</div>
	</dl>
</section>
