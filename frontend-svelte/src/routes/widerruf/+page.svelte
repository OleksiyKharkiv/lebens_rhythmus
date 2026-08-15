<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	// German-only, see note in impressum/+page.svelte.
</script>

<svelte:head>
	<title>{m.site_name()} — Widerrufsbelehrung</title>
</svelte:head>

<section class="mx-auto max-w-2xl px-6 py-16 sm:py-24">
	<h1 class="font-display text-3xl font-semibold text-paper sm:text-4xl">Widerrufsbelehrung</h1>
	<p class="mt-2 text-paper-dim">Verbraucher haben ein vierzehntägiges Widerrufsrecht</p>

	<div class="mt-10 space-y-8 text-paper-dim">
		<div>
			<h2 class="font-display text-lg font-semibold text-paper">Widerrufsrecht für Verbraucher</h2>
			<p class="mt-2">
				Sie haben das Recht, binnen vierzehn Tagen ohne Angabe von Gründen diesen Vertrag zu
				widerrufen. Die Widerrufsfrist beträgt vierzehn Tage ab dem Tag des Vertragsabschlusses.
			</p>
			<p class="mt-2">Um Ihr Widerrufsrecht auszuüben, müssen Sie uns</p>
			<p class="mt-2">
				<strong class="text-paper">Olena Khudoshyna</strong><br />
				Theater Lab 29 – Studio für kreative Entwicklung<br />
				Ritterspornweg 1<br />
				50129 Bergheim, Deutschland<br />
				E-Mail: info@tlab29.com
			</p>
			<p class="mt-2">
				mittels einer eindeutigen Erklärung (z. B. ein mit der Post versandter Brief oder E-Mail)
				über Ihren Entschluss, diesen Vertrag zu widerrufen, informieren. Sie können dafür das
				beigefügte Muster-Widerrufsformular verwenden, das jedoch nicht vorgeschrieben ist.
			</p>
			<p class="mt-2">
				Zur Wahrung der Widerrufsfrist reicht es aus, dass Sie die Mitteilung über die Ausübung des
				Widerrufsrechts vor Ablauf der Widerrufsfrist absenden.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">Folgen des Widerrufs</h2>
			<p class="mt-2">
				Wenn Sie diesen Vertrag widerrufen, haben wir Ihnen alle Zahlungen, die wir von Ihnen
				erhalten haben, unverzüglich und spätestens binnen vierzehn Tagen ab dem Tag zurückzuzahlen,
				an dem die Mitteilung über Ihren Widerruf dieses Vertrags bei uns eingegangen ist. Für diese
				Rückzahlung verwenden wir dasselbe Zahlungsmittel, das Sie bei der ursprünglichen Transaktion
				eingesetzt haben, es sei denn, mit Ihnen wurde ausdrücklich etwas anderes vereinbart; in
				keinem Fall werden Ihnen wegen dieser Rückzahlung Entgelte berechnet.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">Ausschluss bzw. vorzeitiges Erlöschen des Widerrufsrechts</h2>
			<p class="mt-2">
				Das Widerrufsrecht besteht nicht bei Verträgen über die Erbringung von Dienstleistungen,
				wenn wir diese vollständig erbracht haben und Sie ausdrücklich zugestimmt haben, dass wir mit
				der Ausführung des Vertrags vor Ablauf der Widerrufsfrist beginnen, und Sie Ihre Kenntnis
				davon bestätigt haben, dass Sie durch Ihre Zustimmung mit Beginn der Ausführung des Vertrags
				Ihr Widerrufsrecht verlieren. Das Widerrufsrecht erlischt vorzeitig, wenn die Dienstleistung
				vollständig erbracht wurde und mit der Ausführung erst begonnen wurde, nachdem Sie dazu Ihre
				ausdrückliche Zustimmung gegeben haben.
			</p>
			<!-- TODO (dev): diese Ausschlussklausel greift rechtlich nur, wenn die Zustimmung im
			     Buchungs-/Anmeldeflow tatsächlich aktiv eingeholt wird (z.B. Checkbox "Ich stimme zu, dass
			     Sie vor Ablauf der Widerrufsfrist beginnen, und weiß, dass ich dadurch mein Widerrufsrecht
			     verliere"). Diese Checkbox existiert im aktuellen Anmeldeflow nicht (weder alt noch neu) —
			     vor Verlass auf diese Klausel im Betrieb ergänzen. Siehe LR-001. -->
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">Muster-Widerrufsformular</h2>
			<p class="mt-2">(Wenn Sie den Vertrag widerrufen wollen, dann füllen Sie bitte dieses Formular aus und senden Sie es zurück.)</p>
			<p class="mt-2">
				<strong class="text-paper">An</strong><br />
				Olena Khudoshyna<br />
				Theater Lab 29 – Studio für kreative Entwicklung<br />
				Ritterspornweg 1<br />
				50129 Bergheim, Deutschland<br />
				E-Mail: info@tlab29.com
			</p>
			<p class="mt-2">
				<strong class="text-paper"
					>Hiermit widerruf(e) ich/wir (*) den von mir/uns (*) abgeschlossenen Vertrag über die
					Erbringung der folgenden Dienstleistung (*)</strong
				>
			</p>
			<p class="mt-2">
				Bestellt am (*)/erhalten am (*)<br />
				Name des/der Verbraucher(s)<br />
				Anschrift des/der Verbraucher(s)<br />
				Unterschrift des/der Verbraucher(s) (nur bei Mitteilung auf Papier)<br />
				Datum
			</p>
			<p class="mt-2 text-sm text-paper-dim/70">(*) Unzutreffendes streichen.</p>
		</div>
	</div>

	<p class="mt-12 text-xs text-paper-dim/70">© 2026 Theater Lab 29. Alle Rechte vorbehalten.</p>
</section>
