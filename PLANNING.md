# PLANNING.md — Chiaro

Piano di lavoro a fasi, con step spuntabili. **Ogni decisione e ogni deviazione si
annotano qui con il motivo** (regola della serie, ereditata da tweather). Il perimetro
del prodotto sta in `VISION.md`, il sistema di design in `DESIGN.md`, la provenienza
del core in `UPSTREAM.md`.

Chiaro è la *daylight edition* di tweather: stesse feature, stessi motori, UI Material 3
per un pubblico che non apre un terminale. Non è un rewrite e non è un re-skin: è la
stessa app sotto, con sopra un prodotto diverso.

---

## Fase 0 — Repo, build, e il core che arriva già verificato ✅

Obiettivo: uno scheletro che compili, e i motori di tweather dentro casa **con la loro
suite verde**. La tesi del progetto è "il rischio non è tecnico, è di presentazione":
questa fase è dove quella tesi si dimostra o cade.

- [x] Scheletro Gradle multi-modulo (`:app`, `:core:domain`, `:core:data`), wrapper 9.1,
      version catalog, `gradle.properties` con configuration cache
- [x] `:core:domain` come modulo **Kotlin/JVM puro** — nessun Android, e il modulo è il
      posto dove quel vincolo è verificabile invece che sperato
- [x] `:core:data` come Android library (Open-Meteo, mapper, Room, DataStore)
- [x] Seed del core da tweather via `tools/seed_core.py` + `tools/seed_edits.py`
      (78 file), ledger in `UPSTREAM.md`
- [x] Keystore di debug condiviso (`keystore/debug.keystore`, alias `chiaro-debug`),
      `applicationIdSuffix ".debug"` per l'installazione affiancata
- [x] `signingConfig` release dietro le quattro proprietà `CHIARO_KEYSTORE*`, con
      `-PsignReleaseWithDebugKey` come opt-in per gli smoke test
- [x] Overlay `src/debug/res`: l'etichetta del launcher è `Chiaro (dev)`, così le due
      icone si distinguono quando sono installate affiancate
- [x] CI: test di tutti i moduli e lint **prima** degli APK
- [x] `release.yml`: sul tag `v*`, test + lint, APK firmato con la chiave vera dai
      GitHub Secrets, pubblicato insieme al mapping R8 come GitHub Release
- [x] `LICENSE` (GPL-3.0), `CHANGELOG.md` (Keep a Changelog, sezione per tag) e
      `licenses/` con l'OFL di Inter
- [x] `:app` minimo che compila e produce un APK installabile
- [x] **248 test verdi**: 141 in `:core:domain` (16 classi), 107 in `:core:data`
      (15 classi), zero failure, zero skipped

### Le tre modifiche non meccaniche, e perché

Il resto del seed è rinomina di package. Queste tre no, quindi stanno in un file a
parte (`tools/seed_edits.py`) con la motivazione accanto:

1. **Le impostazioni che i motori leggono si spostano nel dominio.**
   `TemperatureUnit`, `WindSpeedUnit`, `UnitSettings` e `NotificationSettings` stavano
   in `SettingsStore` accanto alle chiavi DataStore, e `RuleVariables` le importava:
   il dominio dipendeva dal layer dati per valutare una regola. Ora vivono in
   `domain/settings/`, e sotto `:core:domain` non c'è più niente. Quello che riguarda
   solo la UI (tema, intervallo, opacità del widget) **non** si è spostato: non è
   input di nessun motore.
2. **`ServiceLocator` smette di importare l'app.** Prendeva lo User-Agent da
   `BuildConfig` e il callback "sono arrivati dati nuovi" da una classe del widget.
   Adesso li riceve da `ServiceLocator.install()`, chiamato da `ChiaroApplication`:
   una libreria non conosce la versione dell'app, ed è esattamente il motivo per cui
   è una libreria.
3. **`sampleWeatherReport` diventa pubblica.** Ha attraversato un confine di modulo,
   quindi `internal` non arriva più ai suoi lettori (i test di `:core:data`, e dalla
   Fase 2 le preview dell'app).

### Cosa la Fase 0 NON semina, e perché

- **`:core:sync`** (il job WorkManager, gli scheduler degli allarmi). Il worker chiama
  i notifier, e i notifier sono *testo*: titoli, corpi, canali. Spostarli adesso
  vorrebbe dire inventare il vocabolario delle notifiche di Chiaro dentro un refactor
  meccanico. Arriva in **Fase 6**, insieme alla schermata che lo rende visibile.
  Il modulo non esiste ancora nemmeno vuoto: un modulo vuoto è un TODO che sembra
  architettura.
- **Il layer UI di tweather** (~6.000 righe): editor kit, document builder, syntax
  highlighter, componenti terminale, layout RemoteViews, i tre profili di tema. Buttato
  per intero, che è il punto del progetto.

### Deviazioni registrate

- **`cron-utils` resta**, come dipendenza di soli test di `:core:domain`. L'avevo tolto
  dal catalogo ("Chiaro non disegna nessun crontab") e due test di `SkyJobCatalogTest`
  sono caduti. Rimetterlo è la scelta giusta: **togliere una guardia in Fase 0
  contraddice la premessa del fork**, che è "il motore arriva già verificato". Se la
  resa cron di `SkyJob` non sopravvive alla Fase 5, spariscono insieme test e
  dipendenza, in quella fase e con quella motivazione.
- **`EditorSettings` e `showDetails` sono ancora in `AppSettings`.** Sono concetti da
  editor (numeri di riga, a capo automatico) e in Chiaro non vogliono dire niente.
  Non li ho tolti qui perché la Fase 0 è meccanica per scelta e toccarli significa
  toccare i test del data layer: si rimuovono in **Fase 4**, con le impostazioni.
- **I commenti ereditati parlano ancora il vocabolario di tweather** (dieci righe:
  `$ tweather init`, `$ tweather run rules`, un hint su un file che qui non esiste).
  Lasciati apposta: ognuno nomina una *superficie* di tweather, e la sostituzione
  onesta è il nome della superficie di Chiaro che fa lo stesso lavoro, che per quasi
  tutte non è ancora stata disegnata. **Ogni fase riscrive i commenti del codice che
  tocca**, e il conteggio in `UPSTREAM.md` è il metro di "fatto".
- **Toolchain**: `:core:domain` non usa `jvmToolchain(17)` ma `sourceCompatibility`
  come i moduli Android. Un toolchain pretende un JDK 17 su ogni macchina che builda,
  e quello di Android Studio non lo è.

### La verifica nel nuovo repo (2026-09-02)

Il trapianto da `tweather/docs/chiaro/` è stato verificato per intero sulla prima
macchina che non l'aveva prodotto: 271 test verdi (141 domain, 107 data, 23 app, zero
skip), lint a zero errori, APK debug `com.callbackdev.chiaro.debug` etichettato
`Chiaro (dev)`, release minificata a 2,2 MB. Due cose non erano sopravvissute alla
copia, entrambe invisibili sulla macchina d'origine:

- **`gradlew` aveva perso il bit eseguibile nell'indice git** (100644): su Windows il
  working tree non lo distingue, ma il runner Linux della CI sì — la prima run è morta
  in 18 secondi con `Permission denied`. Sistemato con `git update-index --chmod=+x`,
  che è anche l'unico posto dove su Windows quel bit esiste davvero.
- **`tools/palette_sheet.py` presumeva uno stdout UTF-8.** Su Windows console e
  redirezione partono in cp1252, e il glifo `✓` dei verdetti la faceva esplodere —
  peggio, una redirezione su file avrebbe scritto un HTML corrotto che dichiara
  `charset=utf-8`. Ora lo script riconfigura il proprio stdout, che è il posto giusto:
  il foglio dichiara l'encoding, quindi lo deve garantire.

---

## Fase 1 — Il sistema di design in Compose ✅

`DESIGN.md` era scritto; questa fase lo rende codice, e in tre punti lo ha corretto.

- [x] `ui/theme/`: `Scheme.kt` (**generato**), `ChiaroColors.kt` (i token semantici),
      `SkyPalette.kt`, `Type.kt`, `Shape.kt`, `Motion.kt`, `ChiaroTheme.kt`
- [x] Inter variabile impacchettato (OFL in `licenses/`), cifre tabulari dove servono
- [x] Le guardie: `PaletteContrastTest`, `ScrimContractTest`, `NoRawColorTest`,
      più `SkyPaletteTest` — **23 test, verdi**
- [x] I componenti primitivi dell'§8: `SkyCanvas`, `DaylightRibbon`, `VerdictChip`,
      `FreshnessChip`, `MetricTile`, `RainSparkline`, `TemperatureRangeBar`, con preview
- [x] Decisione icone: **Meteocons** (MIT), con `ui/icons/ChiaroIcons` come seam
- [x] `tools/palette_sheet.py`: il foglio della palette, letto dai sorgenti Kotlin

### Lo schema non si sceglie a mano, si genera

`tools/gen_scheme.py` produce i 36 ruoli Material dalle tre tinte sorgente: prende tinta e
croma di ognuna in CIELAB LCh, mette L\* alla tonalità che Material nomina per quel ruolo,
e abbassa la croma finché il colore sta dentro sRGB. Il tono di Material **è** L\*, quindi
il tono è esatto e solo la croma approssima HCT — motivo per cui `PaletteContrastTest`
verifica il risultato invece di fidarsi del metodo. I neutri hanno la croma fissata a 3 e
7: è la differenza fra una superficie che legge come carta calda e una che legge beige.

### Le tre cose che il DESIGN diceva male, e come si è visto

1. **Le nuvole non mescolano verso un grigio fisso.** Lo diceva la §3.3, e a implementarla
   una mezzanotte coperta usciva più chiara di un crepuscolo sereno: un grigio fisso è più
   luminoso di un cielo notturno. Le nuvole tolgono il *colore* al cielo, non ci mettono
   dentro una quantità fissa di luce. Ora ogni stop si desatura verso la propria
   luminosità (0,7 × nuvole) e poi si smorza (0,15 × nuvole). Trovato da `SkyPaletteTest`.
2. **La luna va scalata dalle nuvole.** La §3.4 applicava il sollevamento lunare dopo il
   mix nuvoloso senza scalarlo, e una notte di luna piena coperta usciva più chiara di una
   serena. Ora il sollevamento è moltiplicato per `(1 − nuvole)`. Stesso test.
3. **L'ora d'oro non era dorata.** Con una sola ancora sull'orizzonte, a 3° il canvas era
   il punto medio fra un sole basso freddo e l'ambra: un beige slavato. Ora le ancore
   dorate sono **due** (4° e 0°). Questo **nessun test lo ha trovato**: contrasto,
   monotonia e continuità passavano tutti. L'ha trovato guardare il foglio della palette,
   che è il motivo per cui `tools/palette_sheet.py` è committato e non era uno scratch.

### Altre decisioni della fase

- **`NoRawColorTest` ha beccato la prima violazione il giorno in cui è stato scritto**: il
  colore dello scrim, che avevo messo nel componente che lo disegna. Ora sta in
  `SkyPalette` e `ScrimContractTest` verifica il valore che il canvas usa davvero invece
  di una sua copia. La guardia ha già ripagato il costo di scriverla.
- **Font impacchettato, non scaricato.** Un downloadable font è una dipendenza a runtime
  da Play Services: un'app che rende male su un telefono senza Google rende male. Costa
  880 KB e serve `@OptIn(ExperimentalTextApi::class)` per le `variationSettings`, senza le
  quali Android sintetizza i pesi sbavando i contorni — esattamente il difetto che
  scegliere Inter doveva evitare.
- **Le icone sono un seam, non un set.** Meteocons è deciso (MIT, ~475 icone), ma
  convertire centinaia di SVG in vector drawable vuole l'importer di Android Studio e uno
  sguardo al risultato: è Fase 2. Fino ad allora `ChiaroIcons` mappa i bucket WMO sul set
  outlined di Material, dietro la stessa funzione. Quello che non deve succedere è la cosa
  che tweather poteva permettersi: le emoji.
- **`MetricTile.meaning` è un parametro obbligatorio.** La regola "ogni numero dice cosa
  farne" la fa rispettare il compilatore, non una code review.
- **Il canvas non segue il tema del lettore**, ed è l'unica eccezione a "ruoli, mai hex":
  alle 23:00 fuori è buio comunque. A renderla sicura è il contratto di scrim.
- **`app_name` è `translatable="false"`.** Il lint l'ha chiesto e ha ragione: la regola di
  Chiaro è "tutto si localizza" perché tutto sullo schermo è prosa o dato, ma il **nome**
  non è né l'uno né l'altro. Un marchio non viaggia. È l'unica eccezione e sta scritta
  accanto alla stringa, non in un documento lontano.

## Fase 2 — Oggi ✅

- [x] Import di Meteocons come vector drawable, dietro `ChiaroIcons` (deviazione Fase 1)
- [x] Composizione della schermata sui primitivi della Fase 1
- [x] **La frase**: motori + `WeatherRecency` → una riga di prosa in cima
- [x] Strip orario + sparkline pioggia, timeline "il resto della giornata"
- [x] La settimana con le barre di range su scala condivisa
- [x] Griglia dei dettagli, ogni numero con la sua riga di significato
- [x] Chip di freschezza, stati vuoto/errore/stale
- [x] **Anticipo minimo della Fase 3**: il foglio dei luoghi (ricerca, aggiunta,
      selezione) — vedi deviazioni

### Le icone: import riproducibile, e una palette ri-ancorata

- **Niente importer di Android Studio.** La Fase 1 lo prevedeva; al momento di farlo,
  un import a mano di ~50 SVG è irriproducibile e non lascia traccia delle scelte.
  Invece: `tools/import_meteocons.py`, il gemello di `seed_core.py` — legge un checkout
  di Meteocons **v2.0.0** (il tag: il `main` attuale è un ridisegno v3 a 128px che non
  va mischiato con questa famiglia), converte lo stile *line* in vector drawable e
  scrive i `mc_*.xml`. Rilanciarlo È l'import. Le tre scelte non meccaniche stanno nel
  docstring del tool: animazioni SMIL eliminate (i VD non le portano), tratteggi
  **ridisegnati** come archi e segmenti veri (i VD non hanno dasharray; le due volute
  del vento diventano piene — lì il tratteggio esisteva solo per essere animato), e la
  palette qui sotto.
- **La palette di Meteocons è ri-ancorata, non copiata.** Il set line è disegnato per
  fondale scuro: il tratto delle nuvole è `#E5E7EB`, **1,18:1** sulla superficie chiara
  — invisibile, e nella strip oraria l'icona è l'unico portatore di "che tempo fa".
  Ogni tinta è mantenuta, ogni luminanza è spostata nella banda `Y ∈ [0.120, 0.283]`
  che supera il pavimento 3:1 dei segni non testuali (DESIGN §10) su **entrambe** le
  superfici. Peggior caso dopo lo spostamento: 3,04:1. La tabella misurata è nel tool;
  `IconContrastTest` rimisura l'XML emesso a ogni build, perché quello che spedisce è
  il file, non la tabella.
- **`material-icons-extended` rimosso** dal catalogo e dalle dipendenze: l'APK di
  debug scende da 64 a 33 MB. Il `Refresh` del chip di freschezza e la lente della
  ricerca vengono da `material-icons-core`, che material3 porta comunque.
- **Il polline non ha un'icona in v2** (la v3 ce l'ha, ma è l'altro disegno). La tile
  usa `dust` — particelle sospese, che è letteralmente il soggetto — finché la famiglia
  v3 non si stabilizza. Le icone del cielo (fasi lunari, stelle) sono importate già
  adesso: stesso giro del tool, le usa la Fase 5.

### Lo stato della schermata, e la regola che lo governa

- `TodayUiState`: `Starting` (scheletro), `NoPlace`, `Empty(city)` (luogo sì, dati mai
  arrivati), `Content`. **Niente stato "Loading"**: un refresh alza un flag sul
  contenuto che c'è già, non lo sostituisce. La cache si emette PRIMA che il fetch
  parta, sempre.
- `TodayStateBuilder` è puro e senza orologio (il tempo è un parametro): tutto lo
  stato — trim di recency, staleness, canvas, ribbon, frase, timeline, settimana — è
  testabile a tavolino come i motori. Un tick al minuto nel ViewModel rifà i conti
  anche quando non arrivano dati: l'età dichiarata e il cielo si muovono col tempo.
- Un report oltre il proprio orizzonte è `Empty`, non "vecchio": la settimana scorsa
  sotto un titolo "Oggi" non sarebbe dato stantio, sarebbe dato sbagliato (la regola
  di `WeatherRecency`, che qui diventa visibile).
- `ActiveSource.Gps` senza fix è trattato come `NoPlace`: il flusso GPS vero
  (permesso, fix, pseudo-città) è della Fase 3, e fino ad allora "nessun luogo" è
  l'unica cosa onesta che la schermata può dire.

### La frase in cima

- `HeadlineEngine`, puro, con le **stesse soglie di `AlertEngine`** (70% sulle
  prossime 6 ore, severi su 12): la frase e la notifica non devono mai essere in
  disaccordo su cosa conta come "sta arrivando pioggia". Casi: severo (per bucket),
  "ombrello verso le X, schiarisce dopo le Y", "smette verso le X", "pioggia per il
  resto della giornata", varianti neve. **Il silenzio è una risposta**: giornata
  tranquilla → la riga non esiste. Nessuna frase di riempimento, mai.
- La localizzazione avviene nel renderer: l'engine risponde in tipi, non in lingua.

### Il vocabolario WMO è dell'app, non del dominio

La VISION §7.2 prevedeva di spostare le tabelle di traduzione nel dominio. Facendolo
si è visto che per Chiaro è il posto sbagliato: qui tutto ciò che si vede è una
risorsa Android (plurali, picker di lingua per-app), e `:core:domain` è JVM puro senza
risorse. Le parole delle condizioni (`WeatherText.condition`) e le righe di significato
dei numeri vivono in `:app` come string resources IT/EN; la `description` inglese del
dominio non arriva mai a schermo. I notifier della Fase 6 renderizzano comunque in
`:app`, quindi il vocabolario sarà già dove serve.

### Deviazioni e rinvii registrati

- **Fase 3 anticipata al minimo**: un'installazione nuova non ha città e seminarne una
  finta è vietato, quindi la schermata vuota ha bisogno del foglio dei luoghi. Fatto il
  minimo che la Fase 2 non può non avere: ricerca-mentre-scrivi (debounce 350 ms),
  tocco per aggiungere e selezionare, lista dei salvati per cambiare. GPS, riordino,
  swipe-per-rimuovere, primo avvio e `migrateFirstRun` restano Fase 3. Scegliere un
  luogo dal foglio chiama già `markInitDone`: rispondere alla domanda del primo avvio
  da un'altra porta è comunque rispondere.
- **Niente bottom navigation** finché non esiste la seconda destinazione (Cielo,
  Fase 5): una barra con tre tab morte è lo schermo che mente su cosa sa fare l'app.
- **L'app bar non è ancora il collasso del canvas** (§8.1): per ora è una barra
  normale con il selettore del luogo, e il canvas sta sotto. Il collasso con la
  transizione è rifinitura, non struttura.
- **Il canvas si aggiorna al tick del minuto**, senza il crossfade a 30 s della §3.5:
  arriva con la passata di motion della Fase 9.
- **Le unità sono i default** (`UnitSettings()`) finché la Fase 4 non costruisce
  l'interruttore: leggere da uno store che nessuna UI può cambiare è un interruttore
  finto.
- **I verdetti sulla timeline** (chip della §8.4) arrivano con il cablaggio di
  `SkyVerdictEngine` in Fase 5; la riga ha già lo slot per il trailing.
- **L'espansione della riga del giorno** è un `AnimatedVisibility` con le ore di quel
  giorno; la shared-element transition della §7 arriva con la motion pass.
- **Icone importate ma non ancora usate** (fasi lunari, stelle cadenti, bussola…):
  deliberate, sono il fabbisogno della Fase 5 e l'import è un giro solo del tool. Le
  segnalazioni UnusedResources del lint restano warning, e restano vere.

## Fase 3 — Luoghi e primo avvio

- [ ] Pager tra i luoghi, sheet di gestione completo (riordino, rimozione con undo),
      GPS, `migrateFirstRun`/`firstRun` cablati
- [ ] Primo avvio: una schermata, due risposte

## Fase 4 — Impostazioni e guida

- [ ] Preferenze M3; rimozione di `EditorSettings`/`showDetails` (deviazione Fase 0)
- [ ] La guida: dove nascono i dati, cosa vogliono dire i verdetti, perché niente radar

## Fase 5 — Cielo

- [ ] Stasera, i momenti di oggi, il catalogo raggruppato, i prossimi eventi
- [ ] Promemoria (allarmi inesatti, soglia 15 minuti)
- [ ] Decisione sulla resa cron di `SkyJob` (deviazione Fase 0)

## Fase 6 — Avvisi, e `:core:sync`

- [ ] Il modulo `:core:sync` con il job periodico condiviso e gli scheduler
- [ ] I notifier in `:app` dietro un'interfaccia, con il testo di Chiaro
- [ ] Avvisi pronti + template + builder a chip + anteprima

## Fase 7 — Diario

- [ ] Voci per fetch (snapshot, previsioni, regole scattate, run del cielo)
- [ ] La striscia di deriva delle previsioni, con vista tabellare

## Fase 8 — Widget

- [ ] Glance: Ora, Oggi, Cielo; `ServiceLocator.install` riceve il repaint

## Fase 9 — Accessibilità e prestazioni, con i numeri

- [ ] Contrasti, scala testo 200%, TalkBack, motion ridotto
- [ ] Avvio a freddo sotto 400 ms, canvas sotto 2 ms/frame
- [ ] Passata IT/EN completa

## Fase 10 — Store e v1.0.0

- [ ] Icona definitiva, screenshot, scheda dello store
- [ ] Sezione `## [1.0.0]` nel `CHANGELOG.md` **prima** del tag: `release.yml` la legge
      e la usa come corpo della Release
- [ ] Chiave di release: **generata** (2 set 2026, in anticipo sulla fase — una chiave
      è immutabile per la vita dell'app, averla presto non costa nulla e permette di
      collaudare `release.yml` molto prima del tag). `C:\Fiorenzo\keys\
      chiaro-release.jks`, alias `chiaro`, RSA 4096, 30 anni, `CN=callbackdev`, come
      il resto della serie (convenzione registrata in tweather, Fase 12); impronta
      SHA-256 del certificato
      `7B:05:E2:47:E4:22:F9:1F:E7:BD:FA:C9:E6:3E:72:2C:BB:6A:3A:7E:8D:43:56:36:66:53:6A:DA:75:8E:7B:22`.
      La firma è già collaudata in locale: `assembleRelease` con le quattro proprietà
      passate via ambiente produce un APK il cui certificato combacia con quell'impronta
      (verificato con apksigner). Restano al committente: le quattro `CHIARO_KEYSTORE*`
      in `~/.gradle/gradle.properties`, i quattro Secrets sul repo, il backup della
      password nel password manager (e la cancellazione del file di transito
      `chiaro-release.password.txt`), poi il tag `v1.0.0`

---

## Note trasversali

- **Il fork non si dimentica**: quando un bug del core va corretto due volte, si estrae
  `weather-core` (VISION §7.3). `UPSTREAM.md` è quello che rende l'estrazione un
  pomeriggio invece che uno scavo.
- **Batteria**: un solo job periodico per tutto, allarmi inesatti, nessun servizio in
  foreground, nessuna posizione in background. Vale già da adesso, non da una fase di
  ottimizzazione.
- **Niente radar**: il provider non ha immagini. È una posizione dichiarata, non una
  mancanza da nascondere.
