# TLab29.com — ZFU/FernUSG Compliance Brief (для Claude Code)

Контекст для ИИ-агента, работающего над кодом/контентом сайта tlab29.com (студия "Lebens Rhythmus", курс "Theaterlabor"). Письмо-запрос в ZFU прилагается отдельно пользователем — читай его вместе с этим брифом.

---

## 1. Юридический контекст (кратко, для понимания задачи)

- В Германии платное онлайн-обучение может подпадать под **FernUSG** (Fernunterrichtsschutzgesetz) — если подпадает, нужна либо дорогая Zulassung у ZFU, либо (для хобби-курсов) упрощённая регистрация.
- Owner (Олена) отправляет в **ZFU** (Staatliche Zentralstelle für Fernunterricht, Köln, poststelle@zfu.nrw.de) запрос с главным аргументом: курс **"Theaterlabor" не подпадает под FernUSG вообще**, потому что проходит **исключительно синхронно** (живая видеоконференция, без записей) — по критерию "überwiegende räumliche Trennung" (§1 Abs.1 Nr.1 FernUSG) это условие не выполняется. Запасной аргумент — регистрация как "Hobby-Lehrgang" (§12 Abs.1 Satz 4 FernUSG), если ZFU не согласится с первым.
- **Пока ответ ZFU не получен, сайт должен быть на 100% консистентен с этим аргументом.** Любое расхождение между тем, что написано в письме, и тем, что видно на сайте (или в AGB) — ослабляет позицию Олены и может привести к отказу/штрафу.
- Задача: страница курса Theaterlabor + AGB должны технически и текстуально поддерживать этот аргумент, а не противоречить ему.

---

## 2. Требования к странице курса "Theaterlabor" (Angebote/Workshops)

**Обязательно должно присутствовать в тексте страницы:**
- Явное указание: курс только для взрослых (Erwachsene)
- Явное указание формата: живая видеоконференция (Live-Videokonferenz), синхронно
- Явное указание: занятия **не записываются**, доступа "посмотреть позже" нет
- Явное указание: нет экзаменов, оценок, сертификата, диплома; это не профессиональное обучение/квалификация
- Формулировка цели: досуг, творчество, личностное развитие (Freizeitgestaltung, kreative Betätigung, persönliche Entwicklung)

**Категорически НЕ должно быть на странице (рядом с Theaterlabor):**
- Слов/фраз: "Aufzeichnung", "Video zum Nachschauen", "on-demand", "jederzeit abrufbar", "Wiederholung des Streams" и т.п.
- Слов: "Zertifikat", "Abschluss", "Qualifikation", "Prüfung", "Diplom"
- Любых упоминаний асинхронных материалов, самостоятельных модулей, домашних заданий с проверкой

**Практическая рекомендация:** держать эти формулировки в одном источнике (например, JSON/CMS-поле "course_format_disclaimer"), чтобы при апдейте контента не разъехались версии на сайте и в письме ZFU.

---

## 3. Правки в AGB (https://tlab29.com/agb)

Текущий AGB общий для всей студии (дети + взрослые, очно + онлайн, все "Angebote/Workshops/Aufführungen"). Для консистентности с письмом в ZFU нужно:

### 3.1 Разграничение общего AGB и специфики Theaterlabor
Добавить оговорку в начало документа или в раздел 2 (Leistungsumfang):

```
Für das Online-Angebot "Theaterlabor" gelten ergänzend die besonderen 
Bedingungen gemäß Ziffer [X]: Es richtet sich ausschließlich an 
erwachsene Teilnehmende und wird ausschließlich synchron per 
Videokonferenz durchgeführt; Aufzeichnungen der Kurssitzungen 
werden nicht erstellt.
```

### 3.2 Änderungsvorbehalt (право менять AGB в будущем)
Важно: свободная формулировка типа "мы можем менять AGB когда угодно" **недействительна** по §308 Nr.4 BGB (контроль справедливости в AGB-праве). Нужна форма с уведомлением и правом возражения:

```
X. Änderung dieser AGB

X.1 Der Anbieter behält sich vor, diese Allgemeinen 
Geschäftsbedingungen mit Wirkung für die Zukunft zu ändern, 
soweit dies aufgrund von Änderungen der Rechtslage, der 
Rechtsprechung oder aus anderen sachlichen Gründen erforderlich 
ist und die Änderung für den Kunden zumutbar ist.

X.2 Über Änderungen wird der Kunde spätestens vier Wochen vor 
deren Inkrafttreten in Textform (z. B. per E-Mail) informiert. 
Widerspricht der Kunde der Änderung nicht innerhalb von vier 
Wochen nach Zugang der Mitteilung, gilt die Änderung als 
genehmigt. Der Anbieter weist den Kunden in der 
Änderungsmitteilung gesondert auf sein Widerspruchsrecht und 
die Bedeutung der Frist hin.

X.3 Für bereits laufende Kursverhältnisse gelten Änderungen nur 
für nach Wirksamwerden neu geschlossene Verträge, sofern nicht 
der Kunde der Anwendung auf das bestehende Vertragsverhältnis 
ausdrücklich zustimmt.
```

### 3.3 Резерв на будущее: запись Aufführungen (НЕ Theaterlabor!)
Задача пользователя: оставить юридический манёвр для будущей записи **выступлений/перформансов (Aufführungen)** с согласия участников — но так, чтобы это **не затронуло** аргумент "Theaterlabor не записывается", который сейчас в письме ZFU. Aufführungen — это отдельный пункт меню сайта (Angebote / Workshops / Aufführungen), т.е. формально отдельный продукт от Theaterlabor. Это нужно явно закрепить текстом, а не оставлять подразумеваемым:

```
X. Bild- und Tonaufnahmen bei Aufführungen

X.1 Im Rahmen von Aufführungen und Präsenzveranstaltungen 
(ausdrücklich NICHT: Online-Angebote wie das "Theaterlabor") 
können Foto-, Video- und Tonaufnahmen zu Dokumentations- und 
Werbezwecken erstellt werden.

X.2 Aufnahmen, auf denen einzelne Teilnehmende erkennbar sind, 
werden nur mit vorheriger, gesonderter Einwilligung der 
betroffenen Person bzw. bei Minderjährigen ihrer 
Erziehungsberechtigten erstellt und veröffentlicht 
(Art. 6 Abs. 1 lit. a DSGVO, § 22 KunstUrhG).

X.3 Die Einwilligung ist freiwillig und kann jederzeit mit 
Wirkung für die Zukunft widerrufen werden; bereits 
veröffentlichtes Material wird nach Widerruf im zumutbaren 
Rahmen entfernt.

X.4 Für das Online-Angebot "Theaterlabor" werden keine 
Aufzeichnungen der Kurssitzungen erstellt oder bereitgestellt; 
diese Ziffer findet auf dieses Angebot keine Anwendung.
```

**Критично:** пункт X.4 — это юридический файрвол. Без него общая формулировка про запись выступлений может быть прочитана как относящаяся и к Theaterlabor, что убивает аргумент в письме ZFU. Не убирать и не смягчать эту фразу без пересмотра всей стратегии переписки с ZFU.

Понадобится отдельная **форма согласия на съёмку** (Einwilligungserklärung) для участников Aufführungen — не часть AGB, отдельный документ/чекбокс при регистрации.

### 3.4 Флаг для проверки человеком (не для авто-правки кодом)
П. 4.1 AGB: "Preise... inklusive der gesetzlichen Mehrwertsteuer" — если Олена работает по Kleinunternehmerregelung (§19 UStG), эта формулировка юридически некорректна (Kleinunternehmer не начисляет НДС). Нужно уточнить статус у бухгалтера и поправить либо AGB, либо счета. **Это не техническая задача для кода — пометить как TODO для владельца.**

---

## 4. Что НЕ трогать без консультации с человеком
- Любое изменение форматулировок про "запись/не запись" Theaterlabor — только после согласования, т.к. это напрямую влияет на активную переписку с госорганом.
- Если в будущем решат всё-таки записывать сам Theaterlabor (не Aufführungen) — это требует **повторного уведомления ZFU**, нельзя просто тихо поменять AGB/сайт.

---

## 5. Источники/для сверки
- ZFU: poststelle@zfu.nrw.de, Peter-Welter-Platz 2, 50676 Köln
- §1 Abs.1 FernUSG (определение Fernunterricht), §12 Abs.1 Satz 3–4 FernUSG (Hobby-Lehrgang, Anzeigepflicht)
- Текст письма в ZFU — приложен пользователем отдельно к этому брифу
