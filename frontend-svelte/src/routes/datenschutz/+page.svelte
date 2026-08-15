<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	// German-only, see note in impressum/+page.svelte.
</script>

<svelte:head>
	<title>{m.site_name()} — Datenschutzerklärung</title>
</svelte:head>

<section class="mx-auto max-w-2xl px-6 py-16 sm:py-24">
	<h1 class="font-display text-3xl font-semibold text-paper sm:text-4xl">Datenschutzerklärung</h1>
	<p class="mt-2 text-paper-dim">Information gemäß Art. 13 DSGVO</p>

	<div class="mt-10 space-y-8 text-paper-dim">
		<div>
			<h2 class="font-display text-lg font-semibold text-paper">1. Verantwortliche Stelle</h2>
			<p class="mt-2">
				Olena Khudoshyna<br />
				Theater Lab 29 – Studio für kreative Entwicklung<br />
				Ritterspornweg 1<br />
				50129 Bergheim, Deutschland<br />
				E-Mail: <a href="mailto:info@tlab29.com" class="text-gold hover:underline">info@tlab29.com</a>
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">2. Erhebung und Speicherung personenbezogener Daten</h2>
			<p class="mt-2">
				Bei jedem Zugriff eines Nutzers auf unsere Webseite werden folgende Daten automatisch
				übermittelt und temporär gespeichert:
			</p>
			<ul class="mt-2 list-inside list-disc space-y-1">
				<li>IP-Adresse</li>
				<li>Datum und Uhrzeit der Anfrage</li>
				<li>Zeitzonendifferenz zur Greenwich Mean Time (GMT)</li>
				<li>Inhalt der Anforderung (konkrete Seite)</li>
				<li>Zugriffsstatus/HTTP-Statuscode</li>
				<li>jeweils übertragene Datenmenge</li>
				<li>Website, von der die Anforderung kommt</li>
				<li>Browser (inkl. Sprache und Version)</li>
				<li>Betriebssystem und dessen Oberfläche</li>
			</ul>
			<p class="mt-2">
				Die vorübergehende Speicherung der IP-Adresse ist erforderlich, um die Auslieferung der
				Website an den Rechner des Nutzers zu ermöglichen. Die Daten werden gelöscht, sobald sie für
				die Erreichung des Zwecks ihrer Erhebung nicht mehr erforderlich sind.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">3. Registrierung und Nutzerkonto</h2>
			<p class="mt-2">
				Wenn Sie sich auf unserer Plattform registrieren, erheben wir folgende Daten zur
				Durchführung des Vertrags über die Teilnahme an unseren Kursen (Art. 6 Abs. 1 lit. b DSGVO):
			</p>
			<ul class="mt-2 list-inside list-disc space-y-1">
				<li>Vor- und Nachname, E-Mail-Adresse, Telefonnummer</li>
				<li>Geburtsdatum (zur Alterszuordnung der Kursgruppen)</li>
				<li>Anschrift</li>
			</ul>
			<p class="mt-2">Bei minderjährigen Teilnehmern erfolgt die Registrierung durch die Erziehungsberechtigten.</p>
			<p class="mt-2">
				Für Kursleiter:innen, die als Honorarkräfte für uns tätig sind, verarbeiten wir zusätzlich
				Bankverbindung (IBAN) und Steuer-Identifikationsnummer zur Abwicklung der Honorarzahlung
				(Art. 6 Abs. 1 lit. b DSGVO). Diese Felder werden verschlüsselt gespeichert.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">4. Weitergabe von Daten</h2>
			<p class="mt-2">
				Eine Übermittlung Ihrer persönlichen Daten an Dritte findet ausschließlich zu den in dieser
				Datenschutzerklärung genannten Zwecken statt, insbesondere an:
			</p>
			<ul class="mt-2 list-inside list-disc space-y-1">
				<li>Technische Dienstleister und Hosting-Provider (Auftragsverarbeiter gem. Art. 28 DSGVO)</li>
				<li>Zahlungsdienstleister (nur bei kostenpflichtigen Angeboten, Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO)</li>
			</ul>
			<p class="mt-2">
				Eine Datenübermittlung in Drittländer findet nur statt, soweit dies zur Vertragserfüllung
				erforderlich ist, gesetzlich vorgeschrieben ist oder Sie uns Ihre Einwilligung erteilt haben.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">5. Betroffenenrechte</h2>
			<p class="mt-2">Sie haben das Recht auf:</p>
			<ul class="mt-2 list-inside list-disc space-y-1">
				<li>Auskunft (Art. 15 DSGVO)</li>
				<li>Berichtigung (Art. 16 DSGVO)</li>
				<li>Löschung (Art. 17 DSGVO)</li>
				<li>Einschränkung der Verarbeitung (Art. 18 DSGVO)</li>
				<li>Widerspruch gegen die Verarbeitung (Art. 21 DSGVO)</li>
				<li>Datenübertragbarkeit (Art. 20 DSGVO)</li>
			</ul>
			<p class="mt-2">Senden Sie Ihr Verlangen bitte an die oben genannte E-Mail-Adresse.</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">6. Lokale Speicherung im Browser (localStorage)</h2>
			<p class="mt-2">
				Nach dem Login speichert unsere Anwendung ein Sitzungs-Token (JWT) sowie einige Basisdaten
				Ihres Kontos (Name, E-Mail, Rolle) technisch notwendig im <em>localStorage</em> Ihres Browsers
				gemäß Art. 6 Abs. 1 lit. f DSGVO. Diese Daten verlassen Ihr Gerät nicht von selbst und werden
				beim Abmelden bzw. Ablauf des Tokens automatisch entfernt. Es werden keine Tracking- oder
				Analyse-Cookies gesetzt.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">7. SSL-/TLS-Verschlüsselung</h2>
			<p class="mt-2">
				Aus Gründen der Sicherheit und zum Schutz der Übertragung vertraulicher Inhalte nutzt diese
				Seite SSL- bzw. TLS-Verschlüsselung.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">8. Änderungsvorbehalt</h2>
			<p class="mt-2">
				Wir behalten uns vor, diese Datenschutzerklärung anzupassen, damit sie stets den aktuellen
				rechtlichen Anforderungen entspricht.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">9. Automatisierte Entscheidungsfindung</h2>
			<p class="mt-2">
				Eine automatisierte Entscheidungsfindung einschließlich Profiling gemäß Art. 22 DSGVO findet
				nicht statt.
			</p>
		</div>
	</div>

	<p class="mt-12 text-xs text-paper-dim/70">© 2026 Theater Lab 29. Alle Rechte vorbehalten.</p>
</section>
