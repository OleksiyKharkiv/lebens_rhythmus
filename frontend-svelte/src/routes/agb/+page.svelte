<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import { localizeHref } from '$lib/paraglide/runtime';
	// German-only, see note in impressum/+page.svelte. Cross-links below
	// still go through localizeHref — the legal text itself stays German,
	// but the site chrome around the linked page should keep whatever
	// locale the user is actually browsing in (2026-08-17 nav-locale fix).
</script>

<svelte:head>
	<title>{m.site_name()} — Allgemeine Geschäftsbedingungen</title>
</svelte:head>

<section class="mx-auto max-w-2xl px-6 py-16 sm:py-24">
	<h1 class="page-title font-display font-semibold text-paper">Allgemeine Geschäftsbedingungen</h1>
	<p class="mt-2 text-paper-dim">Theater Lab 29 – Studio für kreative Entwicklung</p>

	<div class="mt-10 space-y-8 text-paper-dim">
		<div>
			<h2 class="font-display text-lg font-semibold text-paper">1. Geltungsbereich &amp; Widerrufsrecht</h2>
			<p class="mt-2">
				1.1 Für alle Geschäftsbeziehungen zwischen dem Betreiber des Studios „Theater Lab 29"
				(nachfolgend „Anbieter") und seinen Kunden/Kursteilnehmern (nachfolgend „Kunde") gelten
				ausschließlich die nachfolgenden Allgemeinen Geschäftsbedingungen in ihrer zum Zeitpunkt des
				Vertragsschlusses gültigen Fassung.
			</p>
			<p class="mt-2">
				1.2 Verbrauchern steht ein gesetzliches Widerrufsrecht von 14 Tagen zu. Die Widerrufsfrist
				beginnt mit dem Tag des Vertragsschlusses. Näheres regelt die separate
				<a href={localizeHref('/widerruf')} class="text-gold hover:underline">Widerrufsbelehrung</a>.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">2. Leistungsumfang</h2>
			<p class="mt-2">
				2.1 Der Anbieter bietet kreative Kurse und Workshops für Kinder und Erwachsene an. Art,
				Umfang und Dauer der jeweiligen Leistung ergeben sich aus der Kursbeschreibung.<br />
				2.2 Der Anbieter behält sich vor, Kurszeiten und -orte aus wichtigem Grund zu ändern. Die
				Teilnehmer werden rechtzeitig informiert.<br />
				2.3 Bei Ausfall eines Termins durch den Anbieter wird ein Ersatztermin angeboten. Ist dies
				nicht möglich, erfolgt eine anteilige Erstattung der Kursgebühr.<br />
				2.4 Für das Online-Angebot „Theaterlabor" gelten ergänzend die besonderen Bedingungen
				gemäß Ziffer 9.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">3. Anmeldung &amp; Vertragsabschluss</h2>
			<p class="mt-2">
				3.1 Die Darstellung der Kurse auf der Webseite stellt kein rechtlich bindendes Angebot,
				sondern eine Aufforderung zur Bestellung dar.<br />
				3.2 Die Anmeldung erfolgt online über die Webseite, per E-Mail oder persönlich vor Ort und
				stellt ein verbindliches Angebot dar.<br />
				3.3 Der Vertrag kommt erst durch die Annahmebestätigung des Anbieters zustande.<br />
				3.4 Die Teilnehmerzahl ist begrenzt; maßgeblich ist der zeitliche Eingang der Anmeldung.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">4. Preise &amp; Zahlungsbedingungen</h2>
			<p class="mt-2">
				4.1 Die jeweils gültigen Preise sind auf der Webseite ausgewiesen. Alle Preise verstehen
				sich inklusive der gesetzlichen Mehrwertsteuer.<br />
				4.2 Die Kursgebühr ist sofort nach Erhalt der Rechnung fällig und innerhalb von 7 Tagen ohne
				Abzug zu zahlen.<br />
				4.3 Bei Zahlungsverzug behält sich der Anbieter die Geltendmachung von Verzugszinsen in Höhe
				von 5 Prozentpunkten über dem Basiszinssatz der EZB vor.<br />
				4.4 Die Aufrechnung ist nur mit unbestrittenen oder rechtskräftig festgestellten Forderungen
				zulässig.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">5. Stornierung &amp; Kündigung</h2>
			<p class="mt-2">
				5.1 Stornierungen durch den Kunden sind schriftlich (per E-Mail oder Brief) an den Anbieter
				zu richten.<br />
				5.2 Bei Stornierung bis 14 Tage vor Kursbeginn wird die Kursgebühr vollständig erstattet. Bei
				späterer Stornierung oder Nichterscheinen besteht kein Anspruch auf Erstattung.<br />
				5.3 Bei mehrteiligen Kursen ist eine Kündigung mit einer Frist von 14 Tagen zum Monatsende
				möglich. Bereits gezahlte Gebühren für besuchte Termine werden nicht erstattet.<br />
				5.4 Das gesetzliche Widerrufsrecht von Verbrauchern (siehe gesonderte Widerrufsbelehrung)
				bleibt von den Regelungen in 5.1–5.3 unberührt und geht diesen vor, soweit es anwendbar ist.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">6. Haftung</h2>
			<p class="mt-2">
				6.1 Der Anbieter haftet unbeschränkt für Vorsatz und grobe Fahrlässigkeit sowie bei
				Verletzung von Leben, Körper oder Gesundheit.<br />
				6.2 Bei leicht fahrlässiger Verletzung wesentlicher Vertragspflichten haftet der Anbieter der
				Höhe nach begrenzt auf den vertragstypischen, vorhersehbaren Schaden.<br />
				6.3 Im Übrigen ist die Haftung ausgeschlossen. Die Haftung nach dem Produkthaftungsgesetz
				bleibt unberührt.<br />
				6.4 Teilnehmer haften für Schäden, die sie am Eigentum des Anbieters verursachen. Eltern
				haften im Rahmen ihrer gesetzlichen Aufsichtspflicht (§ 832 BGB) für Schäden, die ihre
				minderjährigen Kinder verursachen.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">7. Datenschutz &amp; Online-Streitbeilegung</h2>
			<p class="mt-2">
				7.1 Der Anbieter erhebt, verarbeitet und nutzt personenbezogene Daten des Kunden
				ausschließlich zur Vertragsdurchführung gemäß den Bestimmungen der DSGVO. Nähere Hinweise
				finden Sie in der <a href={localizeHref('/datenschutz')} class="text-gold hover:underline">Datenschutzerklärung</a>.
			</p>
			<p class="mt-2">
				7.2 Die Europäische Kommission stellt eine Plattform zur Online-Streitbeilegung (OS) bereit:
				<a href="https://ec.europa.eu/consumers/odr/" class="text-gold hover:underline"
					>https://ec.europa.eu/consumers/odr/</a
				>. Unsere E-Mail-Adresse finden Sie im
				<a href={localizeHref('/impressum')} class="text-gold hover:underline">Impressum</a>.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">8. Schlussbestimmungen</h2>
			<p class="mt-2">
				8.1 Es gilt deutsches Recht unter Ausschluss des UN-Kaufrechts. Bei Verbrauchern gilt diese
				Rechtswahl nur insoweit, als nicht der gewährte Schutz durch zwingende Bestimmungen des
				Rechts des Staates, in dem der Verbraucher seinen gewöhnlichen Aufenthalt hat, entzogen
				wird.<br />
				8.2 Sollten einzelne Bestimmungen dieser AGB unwirksam sein oder werden, so wird dadurch die
				Wirksamkeit der übrigen Bestimmungen nicht berührt.<br />
				8.3 Gerichtsstand ist – soweit gesetzlich zulässig – Bergheim, Deutschland. Für Verbraucher
				gelten die gesetzlichen Gerichtsstände.
			</p>
		</div>

		<!-- Anchor for direct linking from the ZFU permission-request letter
		     (tlab29.com/agb#theaterlabor) — docs/compliance/tlab29-zfu-compliance-brief.md §3.1. -->
		<div id="theaterlabor">
			<h2 class="font-display text-lg font-semibold text-paper">
				9. Besondere Bedingungen für das Online-Angebot Theaterlabor
			</h2>
			<p class="mt-2">
				Für das Online-Angebot „Theaterlabor" gelten ergänzend die besonderen Bedingungen gemäß
				dieser Ziffer: Es richtet sich ausschließlich an erwachsene Teilnehmende und wird
				ausschließlich synchron per Videokonferenz durchgeführt; Aufzeichnungen der Kurssitzungen
				werden nicht erstellt.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">10. Änderung dieser AGB</h2>
			<p class="mt-2">
				10.1 Der Anbieter behält sich vor, diese Allgemeinen Geschäftsbedingungen mit Wirkung für
				die Zukunft zu ändern, soweit dies aufgrund von Änderungen der Rechtslage, der
				Rechtsprechung oder aus anderen sachlichen Gründen erforderlich ist und die Änderung für
				den Kunden zumutbar ist.<br />
				10.2 Über Änderungen wird der Kunde spätestens vier Wochen vor deren Inkrafttreten in
				Textform (z. B. per E-Mail) informiert. Widerspricht der Kunde der Änderung nicht innerhalb
				von vier Wochen nach Zugang der Mitteilung, gilt die Änderung als genehmigt. Der Anbieter
				weist den Kunden in der Änderungsmitteilung gesondert auf sein Widerspruchsrecht und die
				Bedeutung der Frist hin.<br />
				10.3 Für bereits laufende Kursverhältnisse gelten Änderungen nur für nach Wirksamwerden neu
				geschlossene Verträge, sofern nicht der Kunde der Anwendung auf das bestehende
				Vertragsverhältnis ausdrücklich zustimmt.
			</p>
		</div>

		<div>
			<h2 class="font-display text-lg font-semibold text-paper">11. Bild- und Tonaufnahmen bei Aufführungen</h2>
			<p class="mt-2">
				11.1 Im Rahmen von Aufführungen und Präsenzveranstaltungen (ausdrücklich NICHT:
				Online-Angebote wie das „Theaterlabor") können Foto-, Video- und Tonaufnahmen zu
				Dokumentations- und Werbezwecken erstellt werden.<br />
				11.2 Aufnahmen, auf denen einzelne Teilnehmende erkennbar sind, werden nur mit vorheriger,
				gesonderter Einwilligung der betroffenen Person bzw. bei Minderjährigen ihrer
				Erziehungsberechtigten erstellt und veröffentlicht (Art. 6 Abs. 1 lit. a DSGVO, § 22
				KunstUrhG).<br />
				11.3 Die Einwilligung ist freiwillig und kann jederzeit mit Wirkung für die Zukunft
				widerrufen werden; bereits veröffentlichtes Material wird nach Widerruf im zumutbaren Rahmen
				entfernt.<br />
				11.4 Für das Online-Angebot „Theaterlabor" werden keine Aufzeichnungen der Kurssitzungen
				erstellt oder bereitgestellt; diese Ziffer findet auf dieses Angebot keine Anwendung.
			</p>
		</div>
	</div>

	<p class="mt-12 text-xs text-paper-dim/70">© 2026 Theater Lab 29. Alle Rechte vorbehalten.</p>
</section>
