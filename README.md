# Totem — Android-app

Vertaling van de iOS-app naar Android. Fysieke focus-app: koppel je Totem via
NFC, maak modi aan (werk, slaap, relaxen) met per modus geblokkeerde apps en
een eigen schema, en start of stop een sessie door je telefoon tegen de Totem
te houden.

## Openen

Open de map `TotemAndroid` in Android Studio (Ladybug of nieuwer). Gradle haalt
de rest zelf op. Doelplatform: Android 8.0 (API 26) en hoger.

## Voor je kunt bouwen

1. **Package-naam** — staat op `nl.totem.app` in `app/build.gradle.kts`. Wil je
   een andere, wijzig dan `namespace` én `applicationId`, en de `taskAffinity`
   van `BlockActivity` in het manifest.

2. **Domein voor de tap-link** — het manifest verwijst naar `tap2totem.app`.
   Zet op je eigen domein een `/.well-known/assetlinks.json` met de
   SHA-256-vingerafdruk van je ondertekensleutel, anders opent Android eerst de
   browser in plaats van de app. Voor testen zonder domein werkt het eigen
   schema `totem://t/<uid>`.

3. **Publieke sleutel** — vul in `nfc/TotemAuth.kt` je eigen publieke sleutel
   in (DER, base64). Zolang daar `VERVANG_...` staat accepteert de app elke
   tag, zodat je kunt ontwikkelen voordat de productie loopt. De tags zelf zijn
   hetzelfde als bij iOS: dezelfde Totem werkt op beide telefoons.

4. **Testen op een echt toestel** — NFC werkt niet in de emulator, en de
   blokkade evenmin (de emulator heeft geen echte apps om te blokkeren).

## Wat de gebruiker moet toestaan

Dit is het grootste verschil met iOS. Daar volstond één Schermtijd-verzoek;
hier zijn het vier losse toestemmingen, die de app in *Instellingen* met hun
stand erbij toont:

| Toestemming | Waarvoor |
| --- | --- |
| Toegankelijkheid | Zien welke app naar voren komt. Zonder dit blokkeert Totem niets. |
| Over andere apps tekenen | Het blokkadescherm mogen openen bovenop een andere app. |
| Wekkers en herinneringen | Een schema op de minuut laten beginnen. |
| Batterij zonder beperkingen | Voorkomen dat de fabrikant Totem stilzet. |

## Hoe de iOS-onderdelen zijn vertaald

| iOS | Android |
| --- | --- |
| SwiftUI | Jetpack Compose met Material 3 |
| `ObservableObject` + `@Published` | `ViewModel` met `StateFlow` |
| CoreNFC `NFCTagReaderSession` | `NfcAdapter.enableReaderMode` |
| FamilyControls + ManagedSettings | AccessibilityService + blokkadescherm |
| `FamilyActivityPicker` | Eigen app-kiezer met namen en iconen |
| DeviceActivity-schema's | AlarmManager + WorkManager als vangnet |
| DeviceActivityMonitor-extensie | `ScheduleReceiver` en `SessionEngine` |
| App Group + `SessionBridge` | Eén `SharedStore`; alles draait in één proces |
| Live Activity | Doorlopende melding met chronometer |
| WidgetKit | Glance-widget |
| Sleutelhanger (`PINService`) | `EncryptedSharedPreferences` |
| Universal Links | App Links met `autoVerify` |

## Structuur

```
app/src/main/java/nl/totem/app/
├── TotemApplication.kt      alles wat één keer moet gebeuren
├── MainActivity.kt          het enige scherm; NFC en tap-links
├── model/Models.kt          PairedTotem, FocusMode, Schedule, ActiveSession
├── data/
│   ├── SharedStore.kt       de opslag, op één plek
│   └── PinService.kt        pincode van de strikte modus
├── nfc/
│   ├── NfcService.kt        leest de tag-UID
│   ├── TagMemory.kt         leest het taggeheugen vanaf pagina 4
│   ├── TotemAuth.kt         echtheidscontrole (ECDSA P-256)
│   └── TapLink.kt           tikken terwijl de app dicht is
├── shield/
│   ├── ShieldService.kt              wat er geblokkeerd is, en de toestemmingen
│   ├── TotemAccessibilityService.kt  de waakhond
│   ├── Onaantastbaar.kt              wat nooit geblokkeerd mag worden
│   ├── BlockActivity.kt              het blokkadescherm
│   ├── SessionService.kt             de doorlopende melding
│   └── InstalledApps.kt              de lijst voor de app-kiezer
├── schedule/
│   ├── ScheduleService.kt   hoort deze modus nu te lopen?
│   ├── ActivityScheduler.kt zet de wekkers
│   ├── ScheduleReceiver.kt  wat er gebeurt als een wekker afgaat
│   ├── ScheduleWorker.kt    het vangnet, elk kwartier
│   └── SessionEngine.kt     hier begint en eindigt elke sessie
├── notify/NotificationService.kt
├── store/AppStore.kt        centrale staat voor de schermen
├── ui/                      alle Compose-schermen
└── widget/TotemWidget.kt    de widget op het startscherm
```

## Hoe het blokkeren werkt

Android heeft geen enkele API waarmee een gewone app andere apps kan
tegenhouden. Wat wél mag: een toegankelijkheidsdienst krijgt bericht als er een
ander venster naar voren komt. Staat dat pakket in de lijst van de lopende
modus, dan drukt Totem op de startknop en zet er meteen zijn eigen scherm
overheen.

Twee dingen zijn daarbij van levensbelang:

- **`Onaantastbaar.kt`** — de launcher, de instellingen, de telefoon-app, het
  toetsenbord en het systeem-UI kunnen nooit geblokkeerd worden. Zonder die
  lijst kun je jezelf permanent buitensluiten: geen instellingen betekent geen
  manier om de dienst uit te zetten of de app te verwijderen.
- **De dienst leest niets van schijf.** Alles wat hij nodig heeft staat klaar
  in het geheugen; hij heeft honderd milliseconden per bericht.

## Aandachtspunten voor Google Play

- Een app die de toegankelijkheidsdienst gebruikt voor iets anders dan
  toegankelijkheid wordt streng beoordeeld. Beschrijf in de Play Console
  precies waarvoor hij dient, en toon in de app dezelfde uitleg vóór je erom
  vraagt. Dat laatste doet het instellingenscherm al.
- `QUERY_ALL_PACKAGES` moet je verantwoorden; voor een app-blokkeerder is dat
  een geaccepteerde reden, maar je moet het invullen.
- `USE_EXACT_ALARM` staat er bewust níét in: die is voorbehouden aan wekker- en
  agenda-apps. De gebruiker geeft `SCHEDULE_EXACT_ALARM` dus zelf.
- De voorgronddienst is van het type `specialUse`; ook daar hoort een
  toelichting bij.

## Nog niet gedaan

- Het project is geschreven maar niet gecompileerd; er was in deze omgeving
  geen Android SDK beschikbaar. Reken op een paar kleine correcties bij de
  eerste build.
- Geen tests. De rekenkern (`ScheduleService`) leent zich goed voor unit-tests
  en heeft geen Android nodig.
- De ringen op het scanscherm zijn getekend, niet geanimeerd zoals de
  iOS-versie; de Totem-afbeeldingen zijn wel overgenomen.

## Google Play: locatie op de achtergrond

Locatieschema's gebruiken geofences en vragen daarom `ACCESS_BACKGROUND_LOCATION`.
Google Play vraagt hiervoor in de Play Console een aparte verklaring
(App-inhoud → Locatierechten) met een korte video die laat zien hoe de gebruiker
een locatieschema instelt en toestemming geeft. Zonder die verklaring wordt de
update afgewezen. Locatie staat standaard uit; alleen wie zelf een
locatieschema maakt, krijgt de vraag om toestemming.
