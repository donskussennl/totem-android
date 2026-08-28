# Play Store-vermelding Totem

Google keurt de vermelding af omdat de lange beschrijving nergens vertelt dat de
app de AccessibilityService API gebruikt. De beoordeling let op drie dingen:
dat de dienst genoemd wordt, wat hij doet, en dat duidelijk is dat er geen
gegevens worden verzameld of gedeeld. Het blok **Toestemmingen die Totem nodig
heeft** hieronder dekt dat af — dat is het deel dat er sowieso in moet.

Wil je je eigen tekst houden, plak dan alleen dat blok erin. De rest is een
volledige beschrijving voor als je opnieuw wilt beginnen.

Onderaan staat wat er naast de tekst nog moet gebeuren: de video, de
IsAccessibilityTool-vlag en het argument waarom er geen smallere API bestaat.

---

## Korte beschrijving (max. 80 tekens)

```
Leg je telefoon weg met een tik. Totem blokkeert apps tot je hem weer aantikt.
```

---

## Lange beschrijving — Nederlands (nl-NL)

```
Totem is een fysiek blok dat je afleidende apps uitzet. Je tikt je telefoon
tegen de Totem en de blokkade begint. Tik nog een keer en hij stopt. Meer
knoppen zijn er niet.

WAAROM EEN BLOK

Een schermtijdlimiet zet je uit met hetzelfde duimgebaar waarmee je hem
aanzette. Een Totem ligt op je bureau, in de keuken of naast je bed. Wil je
eerder stoppen, dan moet je opstaan en er heen lopen. Dat kleine stukje moeite
is precies genoeg om de automatische piloot te onderbreken.

ZO WERKT HET

1. Kies een focusmodus en vink de apps aan die eruit moeten.
2. Tik je telefoon tegen de Totem. De blokkade begint meteen.
3. Open je zo'n app toch, dan schuift er een rustig scherm overheen dat je
   eraan herinnert waar je mee bezig was.
4. Tik opnieuw tegen de Totem om te stoppen. Ligt de Totem op kantoor en jij
   op de bank, dan blijft de blokkade staan.

WAT JE KUNT INSTELLEN

- Meerdere focusmodi naast elkaar: werk, slapen, gezin. Elk met eigen apps.
- Schema's per dag: laat een modus vanzelf beginnen en eindigen.
- Strikte modus met pincode, voor als je jezelf niet vertrouwt.
- Vijf noodontgrendelingen per maand, voor als je de Totem niet bij je hebt.
- Statistieken: hoeveel tijd je per week geblokkeerd hield.

TOESTEMMINGEN DIE TOTEM NODIG HEEFT

Toegankelijkheidsservice (AccessibilityService API)
Totem gebruikt de AccessibilityService API van Android om te zien welke app je
op de voorgrond opent. Dat is de enige manier waarop de app kan herkennen dat
je een geblokkeerde app start en er het blokkadescherm overheen kan tonen.
Totem leest niet wat er op je scherm staat, slaat geen toetsaanslagen op en
maakt geen schermafbeeldingen. De naam van de geopende app blijft op je
toestel: hij wordt niet bewaard en niet verzameld, gedeeld of verkocht — niet
aan ons en niet aan derden. Je kunt de service op elk moment uitzetten via
Instellingen op je toestel.

Over andere apps tekenen
Nodig om het blokkadescherm te tonen bovenop de app die je zojuist opende.

Wekkers en herinneringen
Nodig als je schema's gebruikt, zodat een blokkade op de minuut begint in
plaats van soms uren later.

PRIVACY

Totem heeft geen account en geen inlog. Welke apps je blokkeert en hoe lang je
blokkades duren, staat op je toestel. Er gaat geen doen-en-laten naar een
server.

WAT JE NODIG HEBT

Een Android-toestel met NFC en een Totem. Zonder Totem kun je de app
installeren en instellen, maar niet starten.
```

---

## Long description — English (en-US)

```
Totem is a physical block that switches off your distracting apps. Tap your
phone against the Totem and the block starts. Tap again and it stops. There are
no other buttons.

WHY A BLOCK

A screen time limit gets switched off with the same thumb that switched it on.
A Totem sits on your desk, in the kitchen or next to your bed. To stop early,
you have to get up and walk over to it. That small bit of effort is exactly
enough to interrupt the autopilot.

HOW IT WORKS

1. Pick a focus mode and tick the apps that should go.
2. Tap your phone against the Totem. The block starts right away.
3. Open one of those apps anyway and a calm screen slides over it, reminding
   you what you were doing.
4. Tap the Totem again to stop. If the Totem is at the office and you are on
   the couch, the block stays on.

WHAT YOU CAN SET UP

- Several focus modes side by side: work, sleep, family. Each with its own apps.
- Daily schedules: let a mode start and end by itself.
- Strict mode with a PIN, for when you do not trust yourself.
- Five emergency unlocks a month, for when the Totem is not with you.
- Statistics: how much time you kept blocked each week.

PERMISSIONS TOTEM NEEDS

Accessibility service (AccessibilityService API)
Totem uses Android's AccessibilityService API to see which app you bring to the
foreground. That is the only way the app can recognise that you are opening a
blocked app and show the blocking screen over it. Totem does not read what is
on your screen, does not record keystrokes and does not take screenshots. The
name of the opened app stays on your device: it is not stored, and it is not
collected, shared or sold — not to us and not to third parties. You can turn
the service off at any time in your device settings.

Display over other apps
Needed to show the blocking screen on top of the app you just opened.

Alarms and reminders
Needed if you use schedules, so a block starts on the minute instead of hours
later.

PRIVACY

Totem has no account and no login. Which apps you block and how long your
blocks last stays on your device. Nothing about your behaviour goes to a server.

WHAT YOU NEED

An Android device with NFC, and a Totem. Without a Totem you can install and
set up the app, but not start it.
```

---

## De video die Google bij elke inzending wil

De brief noemt dit los van de beschrijving: *"include in the Play Console, a
link to an updated video showcasing the core functionality feature that uses
the AccessibilityService API with each new app submission."* De link zet je in
Play Console in het verklaringsformulier voor de Accessibility API (bij App
content / Verklaringen). Zet hem op YouTube als **verborgen** (unlisted), niet
op privé — anders kan de beoordelaar er niet bij. En werk hem bij zodra het
scherm verandert: hij vraagt er bij élke nieuwe inzending om.

Opnemen vanaf een verse installatie, in één take, zonder knippen:

1. Startscherm van het toestel; open Totem.
2. Het scherm 'Totem klaarzetten' komt in beeld. Tik op *Toegankelijkheid
   openen*.
3. De kennisgeving verschijnt. **Scroll er langzaam doorheen** zodat elke regel
   leesbaar in beeld is geweest — dit is het punt waar ze op afkeurden.
4. Tik naast het venster en druk daarna op de terugknop. Laat zien dat het
   venster blijft staan: weglopen is geen toestemming.
5. Tik op *Niet akkoord*. Het venster sluit en de service blijft uit.
6. Tik opnieuw op *Toegankelijkheid openen*, en nu op *Ik ga akkoord*. De
   Android-instelling opent; zet de schakelaar bij Totem aan.
7. Terug in Totem: start een focusmodus door de telefoon tegen de Totem te
   tikken, open een geblokkeerde app en laat het blokkadescherm verschijnen.

Stap 3 tot en met 6 gaan over de afwijzing; stap 7 is de "core functionality"
die ze willen zien. Anderhalve minuut is genoeg.

## IsAccessibilityTool: laten staan zoals het staat

Die vlag is voor apps die er zijn vóór mensen met een beperking. Totem is dat
niet, dus de vlag mag er niet in — en hij staat er ook niet in
(`accessibility_service_config.xml` heeft hem niet). Zet hem er niet alsnog bij
om de beoordeling te versnellen; dat is precies waar dit beleid tegen
geschreven is.

## "Gebruik een smallere API als het kan"

Daar kun je op antwoorden, mocht het gevraagd worden. Voor het herkennen van de
app die op de voorgrond komt bestaat op Android geen smaller alternatief:

- `UsageStatsManager` vereist PACKAGE_USAGE_STATS, geeft alleen een geschiedenis
  achteraf en vraagt om doorlopend pollen. Een blokkade zou dan seconden te laat
  aanslaan, en dat is genoeg om de app alsnog te openen.
- Er is op Android geen tegenhanger van de Screen Time API van iOS.

Wat Totem wél doet is de dienst zo krap mogelijk afstellen, en dat is
controleerbaar in het bestand:

- `canRetrieveWindowContent="false"` — de inhoud van schermen wordt niet
  opgevraagd;
- alleen `typeWindowStateChanged` — geen toetsaanslagen, geen tekstwijzigingen;
- geen `QUERY_ALL_PACKAGES` in het manifest.

Die drie regels zijn het sterkste antwoord op dit punt: er wordt niet méér
opgehaald dan de naam van de app die naar voren komt.
