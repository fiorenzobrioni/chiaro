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

### Verifica su device (committente, 2 set 2026)

Prima installazione reale (APK release firmato con la chiave vera), screenshot alla
mano: canvas notturno corretto alle 20:57, la frase assente su una serata serena (il
silenzio come risposta, osservato funzionare), timeline con "Buio pieno 21:45" e
"Sorge la luna 22:01", settimana su scala condivisa, espansione in-place delle ore di
un giorno, griglia dettagli con i significati. Un difetto trovato dall'occhio e non
dai test: **le card dei dettagli, accoppiate per riga, avevano altezze indipendenti**
— due fondi che non combaciano leggono come un disallineamento, non come contenuti di
lunghezza diversa. Corretto: la riga usa `IntrinsicSize.Max` e le due tile riempiono
la stessa altezza.

## Fase 3 — Luoghi e primo avvio ✅

- [x] Pager tra i luoghi: una pagina per città salvata più la posizione del telefono
      quando il GPS è attivo; fermarsi su una pagina È selezionarla (pager e foglio
      scrivono lo stesso store); pallini di posizione nell'app bar
- [x] Foglio dei luoghi completo: riga GPS appuntata in cima con switch e stato,
      temperatura (dalla cache, mai dalla rete) accanto a ogni salvato, ricerche
      recenti, riordino col drag da pressione lunga, rimozione con swipe e undo
- [x] GPS: permesso → fix → pseudo-città → attivazione, errori in parole
      (permesso negato / localizzazione spenta / timeout / non disponibile)
- [x] `migrateFirstRun`/`firstRun` cablati nella shell: `Unknown` non disegna nulla,
      un solo controllo per installazione
- [x] Primo avvio: una schermata, due risposte, «Non ora» concesso — e atterra sul
      vero stato "nessun luogo"

### Decisioni della fase

- **`CityStore` cresce di due metodi** (`move`, `insert`) con i loro test: il riordino
  e l'undo non erano esprimibili con l'API ereditata. Drift additivo, registrato in
  `UPSTREAM.md`. Toccando il file, i suoi commenti col vocabolario di tweather sono
  stati riscritti — questa è la fase che costruisce le superfici (primo avvio, foglio
  dei luoghi) che i sostituti onesti dovevano nominare.
- **Niente FAB nel foglio** (deviazione dall'inciso della VISION §5.6): il primo
  elemento interattivo del foglio È già "aggiungi un luogo" — un bottone che galleggia
  sopra l'affordance che duplica è decorazione.
- **Nel flusso GPS il fix viene prima del toggle**: attivare una sorgente che non sa
  ancora nominare un luogo farebbe lampeggiare "nessun luogo" a chi guarda. Toccare la
  riga GPS già attiva la seleziona e rinnova il fix in silenzio (un fallimento tiene
  il fix vecchio: posizione di prima, meteo vero). Il rinnovo periodico del fix è
  lavoro del job condiviso, Fase 6.
- **Il permesso si chiede solo dal bottone che lo spiega** (VISION §5.8), e l'esito
  negato non ha un ramo suo: si chiama comunque il provider, che risponde con l'errore
  onesto. Un percorso solo, un vocabolario solo.
- **Il refresh è indirizzato alla pagina**: il pull su Milano non è una richiesta di
  spendere due GET su ogni vicino che il pager tiene caldo.
- **Riordino**: durante il drag l'ordine vive in uno stato locale e le emissioni dello
  store si ignorano (strapperebbero la riga da sotto il dito); si persiste a fine
  gesto con una sola `move`. Per TalkBack le stesse mosse sono azioni custom
  ("Sposta su"/"Sposta giù") sulle righe.
- **L'undo ripristina posizione E selezione**: `insert` rimette la riga dov'era senza
  selezionarla, e se la città rimossa era attiva l'undo la riattiva. Un undo che non
  ripristina lo stato esatto non è un undo.
- **Lo scheletro anche prima degli store**: il `PagerModel` parte `null` e la shell
  disegna lo scheletro, mai la schermata sbagliata per un frame (né il primo avvio a
  un utente di lunga data, né "nessun luogo" a chi ha tre città).

### Verifica su device e ritocchi (committente, 2 set 2026, sera)

Fase 3 provata sul telefono: pager, foglio, GPS e primo avvio funzionano. Dallo
screenshot, tre richieste — due accolte e una che era un difetto:

- **La scritta "Chiaro" in cima era l'ActionBar di sistema**, mai disegnata dall'app:
  `Theme.DeviceDefault.DayNight` la porta con sé e nessuno gliel'aveva chiesta. Il
  tema ora è `NoActionBar`. Trovato dall'occhio del committente, invisibile a ogni
  test.
- **Il selettore del luogo vive SUL canvas** (nome, chevron e pallini in bianco sul
  cielo), e il canvas arriva fino al bordo alto dello schermo, dietro la status bar.
  È un passo verso la §8.1, non una violazione della §3.6: il contratto dello scrim si
  ESTENDE a una seconda banda simmetrica in alto — stesso colore, stessa alfa, stessa
  garanzia misurata — che copre selettore e icone di stato (bianche sopra il canvas,
  inchiostro del tema negli stati senza canvas). Costo onesto, detto al committente:
  scorrendo, il nome se ne va col cielo che etichetta; il ritorno in barra compatta è
  il collasso della §8.1, che resta alla motion pass.
- **Icone meteo più grandi**: 32dp nella strip oraria (in celle da 56 le 24dp
  intrinseche erano timide accanto alle cifre), 28dp nelle righe della settimana.
- **Secondo giro (stessa sera)**: il selettore del luogo sale a `titleLarge` — è
  l'etichetta del cielo che governa la pagina, a 16sp competeva con i titoli di
  sezione — e la pagina GPS porta il pin della posizione prima del nome. Non è solo
  grafica: una "Cavenago" salvata e il fix GPS fermo a Cavenago erano due pagine
  identiche, e l'origine di un dato è parte della sua verità. Il pin non parla mai da
  solo: la descrizione della riga premette "La mia posizione" per TalkBack.

DESIGN.md §3.6 aggiornato con il contratto a due bande.

## Fase 4 — Impostazioni e guida ✅

- [x] Preferenze M3 in gruppi: unità (temperatura, vento), aspetto (tema, colori
      dinamici), aggiornamenti (frequenza), lingua (il picker per-app di sistema),
      informazioni (versione, dati, sorgente, privacy), ripristino con conferma
- [x] Rimozione di `EditorSettings`/`showDetails` (deviazione Fase 0) — e con loro
      il resto del lessico da editor rimasto nel data layer (sotto)
- [x] Il tema segue le scelte: `ThemeMode` (sistema/chiaro/scuro) e `dynamicColor`
      letti da `MainActivity`; l'ingranaggio vive accanto al selettore del luogo
- [x] Le unità vere arrivano a Oggi (chiusa la nota "Fase 4" in `ContentState`)
- [x] La guida: dove nascono i dati, cosa dicono i verdetti, perché niente radar —
      prosa IT/EN, illustrata coi componenti veri (i quattro `VerdictChip`).
      **Riscritta come giro delle quattro schermate nella passata su device del 3 set
      (in fondo a Fase 8), col capitolo sul radar tolto**
- [x] La card una-tantum su Oggi che punta alla guida: usata o chiusa, sparisce per
      sempre; la guida resta raggiungibile dalle Impostazioni

### Decisioni della fase

- **La pulizia del data layer va oltre la deviazione registrata.** La Fase 0
  prometteva la rimozione di `EditorSettings` e `showDetails`; toccando il file sono
  caduti per lo stesso motivo anche `themeProfileName` (nominava i tre profili tema di
  tweather: l'aspetto di Chiaro è `themeMode` + `dynamicColor`, e ora sono quelle le
  chiavi) e `lastModifiedEpochSeconds` (rendeva la riga `// Last modified:` di
  `settings.config`, una superficie che qui non esiste). `WorkspaceStore` perde
  `MainEditorFile` — lo stato del tab di un editor che non c'è — e tiene il puntatore
  una-tantum, rinominato sulla superficie che serve davvero: la card della guida.
  I test sono cambiati insieme al codice che coprivano, e il nuovo
  `SettingsStoreTest` copre quello che prima era senza guardia: default, round-trip,
  fallback su valori non riconosciuti, reset. Registrato in `UPSTREAM.md`.
- **Niente gruppo notifiche né widget in Impostazioni, per ora.** VISION §5.7 li
  elenca, ma i notifier arrivano in Fase 6 e i widget in Fase 8: un interruttore che
  oggi non cambia niente è lo schermo che mente (§1.1). Ogni gruppo compare nella
  fase che accende la funzione che governa. Stesso criterio per "movimento del
  canvas": il canvas oggi è statico (la motion pass è più avanti), lo switch arriva
  con l'animazione che spegne.
- **Navigazione a stato, non NavHost**: tre destinazioni e due archi non giustificano
  un grafo. `BackHandler` riporta la guida alla porta da cui è entrata (Oggi o
  Impostazioni); la bottom navigation di Fase 5 riporrà la domanda.
- **La guida spedisce tre capitoli su quattro**: "come funzionano gli avvisi"
  (VISION §5.7) arriva in Fase 6 insieme alla schermata che racconta — la guida non
  descrive quello che l'app non fa ancora. I verdetti invece si insegnano già: le
  quattro parole (*Bello*, *Così così*, *Niente da fare*, *Presto per dirlo*) nascono
  qui, nella pagina che le spiega, e la Fase 5 parlerà le stesse.
- **Il tono della guida è una regola, non un caso**: ogni scelta di prodotto vi
  compare come un fatto su come funzionano le cose, mai come un giudizio di valore, e
  senza paragoni con altre app. La guida inoltre non spiega mai un elemento
  dell'interfaccia: un elemento che ha bisogno di spiegazione è un bug (VISION §5.7).
  *(Il corollario «e non giustifica un'assenza» è arrivato dopo, col taglio del
  capitolo sul radar: la risposta a «sta per piovere?» è rimasta, il paragone
  implicito no.)*
- **Il dialogo di reset dice cosa NON tocca**: luoghi, storico e card della guida
  sopravvivono, e non per caso — il reset pulisce il solo DataStore delle
  impostazioni, e la card vive in `workspace` proprio perché un ripristino non deve
  rimostrarla a chi la guida l'ha già letta.
- **Le icone della status bar leggono la luminanza della `surface`** invece di
  `isSystemInDarkTheme`: col tema forzabile dalle impostazioni i due possono
  divergere, e le icone devono seguire la scelta, non il sistema.
- **`units` parte dai default e insegue lo store**: il valore iniziale è quello di
  un'installazione fresca, e lo store risponde entro il primo frame. Un °C mostrato a
  chi ha scelto °F per un frame è un numero vero in un'unità vera, non una bugia; la
  costante è che nessun placeholder si vesta mai da valore.

### Verifica

297 test verdi (141 domain, 114 data — con i 5 nuovi di `SettingsStoreTest` — e 42
app), zero failure, zero skip; lint a zero errori; APK debug ok. Il lint ha fatto il
suo mestiere una volta: i `%` degli esempi di evidenza nella guida ("nuvole 10%")
leggevano come stringhe di formato — marcate `formatted="false"`, che è la
dichiarazione onesta: sono prosa, non template.

## Fase 5 — Cielo ✅

- [x] Stasera: il verdetto-eroe sulla finestra di buio, con i numeri che l'hanno
      deciso e la ragione quando non erano le nuvole (la luna, con la percentuale)
- [x] I momenti di oggi: le sottoscrizioni giornaliere risolte nel fuso della città,
      in ordine di orario, verdetto con evidenza, campanella per il promemoria
- [x] In arrivo: i picchi delle meteore, la prossima luna piena, solstizi ed
      equinozi — col verdetto dove la previsione arriva e l'onesto "troppo lontano
      per dirlo" dove non arriva
- [x] Il catalogo dei 32 momenti, raggruppato (Sole · Notte · Luna · Stagioni ·
      Stelle cadenti), ognuno con la riga che insegna cos'è
- [x] Promemoria: allarmi inesatti (`setAndAllowWhileIdle`), soglia minima 15 minuti,
      uno alla volta, re-arm su boot/avvio/modifica, notifica in prosa localizzata
- [x] La barra di navigazione: Oggi e Cielo — i tab arrivano con le loro schermate
- [x] Decisione sulla resa cron di `SkyJob` (deviazione Fase 0): chiusa, sotto

### Decisioni della fase

- **La resa cron resta, con la sua guardia.** La tassonomia di `SkyJobKind`
  (giornaliero/annuale/polling) è ciò che fa avanzare lo scheduler, e l'espressione
  cron che ogni kind porta con sé è parte dell'identità del job nel motore condiviso
  con tweather: lì è una riga visibile del file, qui è metadato mai renderizzato.
  Toglierla sarebbe deriva della copia senza guadagno funzionale, e renderebbe più
  costosa l'estrazione del core condiviso (VISION §7.3). `cron-utils` resta
  `testImplementation` con i suoi due test, che continuano a verificare un fatto vero
  del motore.
- **I promemoria vivono in `:app/notifications`, non in `:core:sync`.** Il trio
  (scheduler, receiver, notifier) è ereditato da tweather quasi verbatim — le ragioni
  (inesatto, uno alla volta, solo il luogo attivo) sono di prodotto e restano nei
  commenti — ma il testo della notifica è di Chiaro: prosa localizzata, il nome del
  momento come titolo, il verdetto col suo numero nel corpo, mai un id puntato.
  `:core:sync` arriva in Fase 6 col job periodico condiviso, che farà da secondo
  re-arm; intanto rearmano il boot, l'avvio del processo e ogni modifica sul Cielo.
- **"Stasera" non dipende dalle sottoscrizioni**: è l'eroe fisso della schermata, e
  alle tre di notte significa la notte in corso (la finestra di ieri finché la sua
  alba non è passata), non il prossimo tramonto.
- **I momenti di oggi sono i job giornalieri; gli annuali stanno in "In arrivo"** —
  che è a catalogo intero, non a sottoscrizioni: le Perseidi arrivano comunque, la
  campanella però viaggia solo sulle righe sottoscritte. La luna piena entra in
  calendario come quarto nominato, senza verdetto: una fase è un fatto del giorno,
  non uno spettacolo da giudicare (regola `observable` del catalogo).
- **Lead per momento senza drift dello store**: null segue il predefinito, zero è
  l'esplicito "mai" — `SkyLead.ofMinutes(0)` legge già OFF, quindi la distinzione è
  rappresentabile senza toccare `SkySubscriptionStore`.
- **Gli equinozi si chiamano coi mesi** (marzo, giugno, settembre, dicembre), non con
  le stagioni: "equinozio di primavera" è una bugia per mezzo pianeta, e la riga di
  spiegazione dice a chi tocca cosa.
- **`moon.today` mostra fase e illuminazione al posto del verdetto**, e l'icona è la
  fase vera: il valore del momento È la luna, non l'orario di mezzogiorno a cui il
  motore lo appende.
- **`skyEnabled` resta plumbing senza interruttore**: in tweather toglieva un file
  dalla strip dell'editor; qui la destinazione Cielo è metà del prodotto e nascondere
  un tab principale non è un'opzione che offriamo. La chiave resta nel data layer
  (lo scheduler la onora) per il giorno in cui una superficie la reclamasse.
- **Il permesso notifiche si chiede alla prima campanella** (VISION §5.8): mai
  all'avvio, mai dalla schermata — dal gesto che lo rende necessario.
- **Il test dell'eroe vive su una luna nuova** (11 set 2026): sul 2 settembre la luna
  vera lavava la notte e il PASS atteso era in realtà un UNSTABLE con nota MOONLIGHT —
  il motore aveva ragione e il test torto, che è esattamente il motivo per cui il
  builder è puro e la data è un parametro.

### Verifica

307 test verdi (141 domain, 114 data, 52 app — con i 10 nuovi di
`SkyStateBuilderTest`), zero failure, zero skip; lint a zero errori; APK debug ok.

### Verifica su device (committente, 3 set 2026)

Tutto funziona, con un difetto di layout trovato dall'occhio, invisibile a ogni test:
nelle righe dei momenti il chip del verdetto stava nel `trailingContent` della
`ListItem`, che prende tutta la larghezza che chiede — un chip largo («Niente da
fare · nuvole 100%») strizzava la colonna del nome a una lettera per riga. Il chip
ora vive sotto al nome, nella colonna del testo, e a destra resta solo la campanella
a larghezza fissa. Stessa correzione sulle righe di «In arrivo», che avevano la
stessa struttura e avrebbero mostrato lo stesso difetto al primo verdetto largo.

## Fase 6 — Avvisi, e `:core:sync` ✅

- [x] Il modulo `:core:sync`: il worker periodico condiviso (fetch, avvisi integrati,
      regole, osservazione del cielo, re-arm del promemoria), `SyncScheduler` con la
      riconciliazione a stato desiderato, e i suoi 4 test puri
- [x] I notifier in `:app` dietro `SyncNotifiers`: `AlertNotifier` (prosa, tre
      canali), `RuleNotifier` (il messaggio del lettore, interpolato, mai tradotto),
      installati da `ChiaroApplication` come lo User-Agent di Fase 0
- [x] La schermata Avvisi: i tre pronti con interruttore e descrizione esatta di
      cosa mandano e quando; le regole del lettore come card (frase in parole, stato,
      ultimo scatto); i cinque template che creano regole vere, già accese
- [x] Il builder a chip: variabile in parole, operatore, soglia su slider (mai un
      campo di testo per un valore con un range), seconda condizione opzionale, il
      messaggio con i segnaposto spiegati, «Prova adesso» senza notifiche
- [x] Il terzo tab della barra, e il quarto capitolo della guida (arrivato con la
      schermata che racconta, come promesso in Fase 4)

### Decisioni della fase

- **Gli interruttori delle notifiche vivono sulla schermata Avvisi, non nelle
  Impostazioni.** VISION §5.7 elenca un gruppo "notifiche" tra le preferenze e §5.4
  mette gli interruttori accanto a ciò che governano: tenerli in due posti sarebbe
  lo stesso interruttore che può divergere. Vince §5.4 — la descrizione onesta di
  cosa manda un avviso sta meglio accanto all'avviso.
- **Il quinto template parla di pioggia, non di sereno.** VISION abbozzava "una notte
  serena", ma il registro delle variabili non ha la copertura nuvolosa: promettere
  "sereno" su un controllo che legge solo la pioggia sarebbe la notifica che mente.
  «Una notte senza pioggia» è quello che il motore può davvero verificare; se il
  registro un giorno crescesse di `cloud_cover`, la crescita spetta a monte (è motore
  condiviso), e il template potrà dire la parola che oggi non può.
- **`RuleStore` cresce di un `add` parametrizzato** (nome, condizioni, messaggio, che
  ritorna la regola creata): i template di Chiaro nascono nella lingua del lettore,
  mentre l'`add()` ereditato semina il testo inglese fisso di tweather. Additivo,
  con i suoi test, registrato in `UPSTREAM.md`. Stesso giro per
  `WeatherRepository.firedRules(entry)`: la decodifica dei nomi scattati sta dove
  sta la codifica, non in un ViewModel che dovrebbe importare la serializzazione.
- **"Ultimo scatto" si legge dalla storia, per nome.** I commit annotano i nomi delle
  regole scattate (`recordFiredRules`): la card mostra il più recente per il luogo
  attivo. Una regola rinominata riparte da zero — è la lettura onesta di quello che
  i dati sanno dire, e il Diario di Fase 7 racconterà il resto.
- **La riconciliazione segue ogni modifica**: interruttori, regole, frequenza di
  aggiornamento, avvio del processo. Il worker si auto-cancella quando non resta
  nessuno da servire (i widget di Fase 8 aggiungeranno il loro motivo per restare).
- **Le modifiche a chip persistono subito, i campi di testo alla chiusura
  dell'editor**: un tap è una scelta discreta, una tastiera no — persistere a ogni
  battuta sarebbe una scrittura DataStore per lettera.
- **Il permesso notifiche si chiede al primo interruttore che si accende** (VISION
  §5.8), mai all'ingresso nella schermata.

### Verifica

313 test verdi (141 domain, 116 data — con i 2 nuovi dell'`add` parametrizzato —
4 sync, 52 app), zero failure, zero skip; lint a zero errori; APK debug ok. Il lint
ha ripetuto la lezione di Fase 5 sulle percentuali nude nelle descrizioni dei
template: `formatted="false"`, sono prosa.

### Verifica su device (committente, 3 set 2026)

Tutto funziona. Un rilievo grafico sulla barra: le meteocons di Oggi e Cielo
riempiono ~70% del loro box con un tratto da ~1,1dp, la campana Material di Avvisi
~80% con ~2dp — tre tab, due pesi. Risolto con due varianti da tab degli stessi
glifi (`ic_tab_today`, `ic_tab_sky`): un group scala il disegno al riempimento della
campana e il tratto cresce fino ai suoi ~2dp ottici. Le meteocons originali non si
toccano — nelle liste, accanto al testo, il loro peso è quello giusto; è la barra
che chiede un altro registro.

## Fase 7 — Diario ✅

- [x] Le voci per fetch, lette come prosa e raggruppate per giorno: revisioni della
      previsione ("Sabato 5 è migliorato: pioggia 70% → 30%"), regole scattate, run
      del cielo osservati (col verdetto o con l'onesto "nessun aggiornamento
      abbastanza vicino"), e gli aggiornamenti falliti con il loro motivo
- [x] La striscia di deriva: una riga per giorno bersaglio, una colonna per fetch,
      colore sulla rampa della metrica (pioggia/massime), legenda sempre presente,
      giudizio nella frase accanto, numeri dietro un tocco lungo
- [x] Il quarto tab: la barra di VISION §5.1 è completa, nessun tab mai nato morto
- [x] "Cos'è cambiato" su Oggi (VISION §5.2.5): fino a tre frasi dopo la timeline,
      il tocco apre il Diario

### Decisioni della fase

- **L'orizzonte di `flattenForecast` passa da 2 a 7 giorni.** Il seme di tweather
  conserva domani e dopodomani perché i suoi Logs mostravano solo quelli; la deriva e
  "Cos'è cambiato" sono SULLA settimana — "sabato è migliorato" pretende sabato su
  disco. `ForecastDiff` è per-data e non cambia; `dayLabel` passa dalla posizione
  alla distanza di data (identico sui 2 giorni, giusto sui 7). Deriva registrata in
  `UPSTREAM.md`, test ereditati aggiornati. Le righe vecchie con 2 giorni restano
  leggibili: le celle che un fetch non copriva si disegnano come assenza (bordo
  sottile), mai come uno zero.
- **I fetch falliti vivono in un loro store (`FetchLogStore`), non nella tabella
  Room.** La storia registra ciò che l'app ha imparato; un fallimento è ciò che non
  ha potuto imparare, e infilarlo come commit con snapshot nullo avrebbe sporcato la
  macchina dei diff ereditata. Anello limitato a 30 voci, scritto dai due soli punti
  che fanno fetch (il pull di Oggi e il worker), letto dal Diario.
- **Il giudizio lo decide la pioggia** ("migliorato"/"peggiorato"): è il numero su cui
  la gente pianifica, ed è l'esempio che VISION stessa usa. La sola temperatura resta
  neutra — più caldo non è universalmente meglio — e il giudizio sta nella frase, mai
  nel colore della striscia (DESIGN §8.10).
- **Lo `status` memorizzato non arriva a schermo**: il suo valore è l'etichetta
  inglese del motore ("Rain"), e una parola inglese non deve comparire (VISION §8).
  Le frasi delle revisioni parlano coi numeri: pioggia, massima, minima.
- **Un giorno che entra in orizzonte non è una revisione**: il diff lo emette come
  "file nuovo", la prosa lo tace — il calendario che avanza non è una notizia.
- **"Cos'è cambiato" lo riempie il ViewModel, non il builder di Oggi**: viene dalla
  storia, che `TodayStateBuilder` deliberatamente non legge; il campo ha un default
  e i test del builder non si toccano. Si ricalcola sui movimenti dei dati, mai sul
  tick del minuto.
- **Il vocabolario delle revisioni è condiviso** (`JournalText`): Oggi e il Diario
  citano la stessa frase, per costruzione.

### Verifica

321 test verdi (141 domain, 118 data — con i 2 di `FetchLogStoreTest` — 4 sync,
58 app — con i 6 di `JournalStateBuilderTest`), zero failure, zero skip; lint a zero
errori; APK debug ok. Nota di macchina: la trappola dei backslash negli heredoc di
questa workstation ha mangiato gli escape degli apostrofi nelle stringhe — riscritti
con l'edit diretto, ed è il promemoria di usare quello per le risorse Android.

## Intervento su richiesta (committente, 3 set 2026) — il tema Fill e il marchio

- [x] Il set **Fill** di Meteocons v2.0.0 entra come secondo tema di icone (48
      drawable `mcf_*`), scelto in Impostazioni → Aspetto; **default FILL**, deciso
      col committente: le forme piene si leggono più in fretta a 24–32dp per un
      pubblico che scorre, e il tratto resta a un tocco di distanza
- [x] Il marchio vero al posto del segnaposto di Fase 0: la falce stellata della
      famiglia, stile fill, tavolozza di Chiaro, bassa nel badge sopra due onde

### Decisioni dell'intervento

- **Anche il Fill è ri-ancorato, non copiato.** La tavolozza del sito vive su fondo
  neutro: sulla superficie chiara di Chiaro misura 1,0–2,4:1 (la faccia delle nuvole
  è bianca al 93% — sparirebbe). `FILL_REMAP` nel tool: tinte conservate, luminanze
  in `Y ∈ [0.120, 0.283]`, ordine tonale dentro ogni famiglia preservato così
  l'elemento in ombra resta più scuro del suo vicino illuminato. `IconContrastTest`
  ora spazza entrambi i prefissi: il pavimento 3:1 vale per tutte e due le penne.
- **Due deroghe proprie del Fill** (documentate nel tool): i gradienti si appiattiscono
  al colore di faccia — a 24–32dp la rampa non si vede e la macchina dei gradienti
  di VectorDrawable non comprerebbe niente — e gli hairline di bordo (0.5) cadono:
  esistevano per orlare un riempimento quasi bianco su pagina bianca. I tratti veri
  (≥1, i raggi del sole) restano e si rimappano.
- **Un refuso upstream corretto per nome**: `fill/drizzle.svg` tratteggia `url(#e)`
  ma definisce a/b/c/d — le gocce sono a/c/d, quindi `e` può solo voler dire `d`.
  Il fix sta nel tool con questo commento, mai come fallback silenzioso.
- **Le icone della barra restano fuori dal tema**: sono sagome che la barra tinge di
  un colore solo — fill e line sarebbero identiche — e la coppia è già calibrata
  sulla campana Material accanto (rilievo device di Fase 6).
- **Lo stile viaggia in un CompositionLocal** (`LocalWeatherIcons`), fornito da
  `MainActivity` accanto al tema: ogni schermata cambia insieme, nessuna schermata
  deve saperlo. Le funzioni `*Res` prendono lo stile come parametro: i widget Glance
  di Fase 8 vorranno id di risorsa, non ImageVector.

### Verifica

Suite completa verde (`IconContrastTest` misura ora 96 drawable), lint a zero errori,
APK ok.

## Fase 8 — Widget ✅

- [x] Glance: **Ora** (icona, temperatura, luogo), **Oggi** (l'adesso, la frase del
      giorno, le prossime cinque ore), **Cielo** (il prossimo momento seguito e il
      suo verdetto — il widget che nessun altro spedisce)
- [x] `ServiceLocator.install` riceve il repaint: ogni commit della storia ridipinge
      i tre widget, così home e app non possono raccontare due pomeriggi diversi
- [x] Il gruppo «Widget» nelle Impostazioni (l'opacità dello sfondo che aspettava
      dalla Fase 4), applicata al solo cartoncino: il testo resta a piena tinta
- [x] Un widget piazzato tiene vivo il job periodico da solo (`shouldRun` cresce di
      `hasWidgets`, col suo test); un fetch fallito ridipinge perché il marcatore di
      freschezza possa comparire

### Decisioni della fase

- **I widget non inventano** (VISION §5.9), e il modo più corto per non inventare è
  non calcolare: il contenuto è la risposta di `TodayStateBuilder` sul report in
  cache — stessi numeri, stesso verdetto di freschezza, stesso taglio delle ore
  passate che mostra l'app, zero rete al momento del disegno. Il Cielo usa
  `SkyScheduler.nextToFire` e lo stesso `SkyVerdictEngine` della schermata.
- **I widget seguono il sistema, non il tema forzato dell'app**: vivono sul launcher,
  e un widget scuro su una home chiara sarebbe una bugia del launcher, non nostra.
  I colori sono coppie giorno/notte costruite dagli stessi schemi dell'app (dinamico
  o Chiaro secondo l'impostazione); i verdetti restano le coppie fisse di DESIGN
  §2.3 — un verdetto significa la stessa cosa qualunque sia lo sfondo.
- **Niente città appuntate nella v1**: VISION §5.9 non le chiede, e i tre widget
  seguono il luogo attivo. `WidgetCityStore` ereditato resta in panchina come
  `skyEnabled`; se un giorno una superficie lo reclama, il costo sarà solo la UI.
- **Le vie del repaint sono tre e arrivano in un punto solo** (`ChiaroWidgets`):
  il commit del repository (dati nuovi), il fallimento del worker (deve comparire
  lo stale), e un collettore di processo su luogo attivo + impostazioni — che copre
  anche unità, stile delle icone e opacità senza che nessuna schermata debba
  ricordarsene.
- **`updatePeriodMillis` è 0 di proposito**: il job condiviso guida ogni repaint al
  ritmo scelto dal lettore; un secondo orologio sarebbe batteria spesa due volte.
- **Niente preview nel picker per ora**: `previewLayout` pretende un layout
  RemoteViews disegnato a mano da tenere allineato ai widget veri. Arriva con gli
  asset dello store (Fase 10), quando si disegnano comunque schermate di vetrina.
  **Ripreso l'8 set 2026** (committente): le anteprime generiche erano la prima cosa
  che un lettore vedeva dei widget, e aspettare la Fase 10 non le rendeva migliori —
  vedi «L'anteprima nel picker» più sotto.

### Verifica

322 test verdi (141 domain, 118 data, 5 sync — col nuovo test dei widget che tengono
vivo il job — 58 app), zero failure, zero skip; lint a zero errori; APK debug ok.

### Verifica su device e rifacimento (committente, 3 set 2026)

Quattro rilievi, due dei quali hanno rifatto il vestito dei widget:

- **"L'ultimo widget messo vince"**: su One UI ogni repaint riversava il contenuto
  dell'ultimo tipo piazzato su tutti e tre. Colpevole: la mappatura id→classe interna
  di Glance dietro `updateAll`. Ora gli aggiornamenti sono guidati dagli id di
  sistema per `ComponentName` — la verità del launcher, che non può mescolarsi — e
  ogni id riceve la composizione del SUO ricevitore.
- **Il vestito nuovo**: lo sfondo di default è il gradiente del cielo del canvas —
  la stessa `SkyPalette.gradient` sulla stessa fotografia del momento, resa bitmap
  con lo scrim §3.6 cotto dentro e inchiostro bianco sopra, come l'eroe dell'app.
  Icona 56dp, temperatura 32sp, nome città su riga intera (i «Cavenago di Brianza»
  ci stanno). In alternativa: cartoncino Chiaro/Scuro/Come il telefono.
- **Impostazioni per-widget** dal flusso di riconfigurazione del launcher (tocco
  prolungato): luogo (il luogo attivo, o una città salvata appuntata —
  `WidgetCityStore` ereditato esce dalla panchina, e il worker fetcha anche le
  città appuntate), sfondo, opacità con slider fino a Trasparente. Ogni scelta
  persiste al tocco e ridipinge quel solo widget. Il gruppo Widget delle
  Impostazioni globali sparisce: due posti per lo stesso pomello divergono.
- **Nota onesta sull'opacità bassa** (rivista due volte, 3 set): «la leggibilità
  è scelta del lettore» su device era semplicemente testo invisibile; la prima
  correzione — sotto il 50% l'inchiostro segue il tema del telefono — è durata uno
  screenshot: tema chiaro su wallpaper quasi nero è una combinazione comune, e
  l'inchiostro spariva lo stesso. Ora sotto il 50% l'inchiostro chiede al wallpaper
  stesso (`WallpaperColors`, l'hint «regge testo scuro» del launcher), col tema come
  riserva se il wallpaper non risponde; l'Application ridipinge quando i colori del
  wallpaper cambiano.
- **L'icona dell'app** stringe le distanze: falce giù, onde su, composizione centrata
  nel badge (il primo taglio abbracciava i bordi e lasciava un golfo in mezzo).
- **Seconda passata su device (3 set 2026)** — tre correzioni grafiche:
  - Icona meteo più grande (Now 56→68 dp, Today 52→64 dp, Sky 46→54 dp) e nome
    della città a 15 sp: a distanza di braccio erano i primi a sparire.
  - **I colori dei widget si risolvono al render, non nel launcher**: i ColorProvider
    day/night di Glance sono risolti dall'host, e un host che gira il cartoncino senza
    girare l'inchiostro lascia parole scure su cartoncino scuro (visto su device).
    Ora ogni colore si risolve contro una sola configurazione (`isNight` in WidgetUi)
    e l'Application ridipinge a ogni cambio di configurazione (tema E lingua); il
    costo onesto è che un flip di tema a processo morto resta indietro fino al sync
    successivo — un cartoncino del tema sbagliato ma leggibile, mai illeggibile.
  - **Le icone della status bar seguono ciò che hanno sotto, davvero**: seguivano lo
    STATO di Today (Content ⇒ bianche) ma non lo scroll, e il contenuto chiaro
    scrollato sotto la barra le rendeva invisibili. Ora il default è legato al tema
    APPLICATO in MainActivity (status e navigation bar: un tema forzato contro il
    sistema lasciava le icone del sistema anche sugli altri schermi); su Today
    restano bianche solo finché la banda di scrim del canvas è dietro la barra
    (`SkyCanvasTopScrimEnd`, una costante sola per canvas e soglia); quando Today
    esce di scena la barra torna al tema.
  - Quattro rifiniture applicate nella stessa passata: temperatura del Now a 34 sp
    (bilancia l'icona cresciuta); minima e massima del giorno accanto alla
    temperatura del Now (↓ prima di ↑, l'ordine di lettura delle righe della
    settimana; vicino a mezzanotte la riga daily di oggi può mancare dal report
    ritagliato e allora la coppia non si disegna); la striscia oraria del Today passa
    a `SizeMode.Exact` e conta le celle sulla larghezza reale concessa dal launcher
    (minimo 4, massimo 7 — al minimo di 4 celle launcher restano le 5 di sempre);
    padding del cartoncino a 12 dp sui widget a una cella (Now e Sky), parametro
    `contentPadding` con default 14 dp per il Today.
  - **Terza passata (screenshot su device, 3 set sera)**: il chip del verdetto del
    widget Cielo usciva dal fondo del cartoncino su una cella — icona 54→48 dp e
    pillola più bassa (4 dp verticali, raggio 12). Il widget Oggi riempie lo spazio
    che già occupava: temperatura a 36 sp con minima/massima accanto (lo stesso
    `dayRangeText` del Now, estratto in WidgetUi), città a 16 sp, striscia con icone
    a 32 dp, ore a 12 sp, gradi a 14 sp e la riga della pioggia — compare quando
    almeno un'ora visibile ha qualcosa da dire, e allora ogni cella stampa la sua
    cifra, 0% compreso (la regola della striscia dell'app); la `WidgetPalette`
    impara `darkGround`, così la rampa della pioggia sceglie il set selezionato per
    il fondo vero (cielo scrimmato e cartoncino scuro compresi).
  - **Il vestito «Il cielo adesso» resta** (valutato su screenshot, 3 set): è
    l'unico sfondo che dice qualcosa di vero sul cielo ed è l'eredità diretta del
    canvas; un'opzione che costa poco non si toglie per un dubbio estetico. Se il
    grigio del velo non convince, il posto dove intervenire è la saturazione del
    gradiente, non l'esistenza dell'opzione.
  - **Quarta passata (screenshot su device, 3 set sera)** — tre richieste del
    committente, la terza delle quali era un'incoerenza vera fra widget e app:
    - **L'icona meteo cresce fino a riempire la cella.** Un numero fisso può essere
      giusto su una sola dimensione di widget, e il disegno Meteocons occupa circa
      metà della sua scatola (la falce di `mcf_clear_night` copre 36 unità su 64):
      a 68 dp sul vetro arrivavano ~38 dp di luna, meno del widget Samsung accanto. Ora
      `heroIconSize` in WidgetUi prende l'altezza davvero concessa dal launcher e ci
      sta dentro (pavimento 52 dp, soffitto 96 dp); il Now passa a `SizeMode.Exact`
      per poterla leggere, il Today toglie dal conto quello che gli servono frase e
      striscia (`HeroReserve`, 116 dp). Il widget Cielo non si tocca: la sua icona a
      48 dp è quella che alla terza passata ha fatto entrare la pillola nel
      cartoncino.
    - **Minima e massima escono dai due widget** (Now e Today): erano l'aggiunta
      della seconda passata, e su device la coppia accanto al numero grande è
      rumore — VISION §5.9 chiede «icona, temperatura, luogo» e adesso è quello che
      c'è. `dayRangeText` esce da WidgetUi con loro; l'escursione del giorno resta
      dove è nata, sulle righe della settimana.
    - **Il widget Cielo e la schermata Cielo mostravano due albe diverse.** Alle
      21:19 la lista «I momenti di oggi» teneva l'alba della mattina, grigia e
      marcata `Passato`, con un `? Presto per dirlo` che le ore trascorse non
      potevano più sostenere; il widget sulla stessa home mostrava già l'alba di
      DOMANI, giudicata `Bello · nuvole 0%`. Nessuna delle due superfici sbagliava
      per conto suo: la finestra che ciascuna guardava non era scritta da nessuna
      parte. Ora è scritta una volta sola, in `ui/sky/SkyUpcoming.kt`, e la leggono
      entrambe. La regola: un momento smette di essere di oggi quando è finito, e
      allora la riga diventa quella di domani e lo dice («Domani · 06:47»). Tre
      eccezioni, che sono fatti e non comodità: una finestra aperta e non chiusa
      vince e si marca «Adesso» (la regola per cui alle 03:00 «stanotte» è il cielo
      fuori, che finora viveva solo nella card Stanotte e ora la card legge da qui);
      un `∅` resta di oggi, perché «oggi la luna lo salta» è una risposta su oggi
      (dottrina di `SkyScheduler`, tenuta); e il momento-lunare del giorno non
      scorre mai, perché è un'affermazione SU oggi e non un appuntamento — stampare
      «Domani» sotto «La luna di oggi» sarebbe assurdo, e «Passato» alle nove di
      sera già lo era.
    - Ricadute oneste: la sezione si chiama «I prossimi momenti» (un elenco che può
      contenere domani non è «di oggi»); i quattro `sky_none_*` perdono l'«oggi»
      incorporato e il giorno passa nel marcatore, perché ora un `∅` può essere di
      domani; `sky_moment_past` sparisce. Il widget Cielo stampa il marcatore con lo
      stesso vocabolario e, per un evento oltre domani, la data — un'ora nuda su una
      home si legge come quella di oggi. Il momento-lunare non è candidato del
      widget (`SkyUpcoming.firstAt` lo salta): fissato a oggi, vincerebbe per sempre
      ogni confronto — e lo stato vuoto del widget dice «nessun momento in arrivo»
      invece di «nessun momento seguito», che con la sola luna sottoscritta sarebbe
      falso. Cinque test nuovi in `SkyStateBuilderTest`, fra cui quello che
      lega le due superfici: il momento del widget è la prima riga della schermata
      che fira davvero.
  - **Quinta passata (misurata sullo screenshot, 3 set notte)** — il widget Ora
    ancora troppo timido accanto a quello Samsung. Misure vere invece che a occhio:
    dal raggio della card (24 dp = 54 px) lo screenshot dà ~2.25 px/dp, quindi il
    widget Ora riceve ~101 dp di altezza e l'Oggi ~235 dp; il glifo Samsung è 101 px,
    quello di Chiaro era 90 px e partiva 25 px più a destra. Due correzioni: il
    cartoncino del Now scende a 6 dp sopra e sotto e a **zero sul bordo iniziale** (il
    disegno Meteocons porta già un quarto di scatola come margine: è quello l'incasso,
    e l'icona parte dove parte la card), e il soffitto di `heroIconSize` sale a 104 dp.
    Sulla stessa densità l'icona passa da 77 a 89 dp — glifo ~103 px, bordo a ~121 px
    contro i 118 px del vicino. Il testo si stacca dall'icona a 8 dp invece di 12,
    perché quel quarto di margine è già aria. Oggi e Cielo non si toccano.
  - **L'icona sembrava ancora allineata in alto** (committente, sullo screenshot
    successivo): non era un'impressione, ed è misurabile. Un blocco di testo è più
    alto dell'inchiostro che si vede — il font di sistema lascia circa un quarto di em
    vuoto sopra le maiuscole di «23°», mentre la «g» di Cavenago arriva al bordo
    inferiore della sua riga — quindi il suo inchiostro sta basso nella propria
    scatola, e centrare le due SCATOLE lascia l'icona in alto di metà di quella banda:
    5,1 dp misurati sullo screenshot con la luna (disegno centrato), contro i 5,2 dp
    che il modello prevede. Il rimedio è simmetria, non un numero magico:
    `textInkBalance` mette sotto l'ultima riga la stessa banda che il font lascia
    sopra la prima (0,24 em della temperatura, moltiplicato per la scala di testo del
    lettore, letto da un `Context` come `isNight` — sul launcher non esiste
    `LocalConfiguration`), e a quel punto basta il centraggio verticale. Applicato a
    Ora e a Oggi: è la stessa riga.
  - **Quello che resta non si tocca**: rasterizzando la famiglia Meteocons (script
    ad hoc con cairosvg) la maggior parte dei disegni è esattamente al centro della
    sua scatola, la notte nuvolosa sta il 4% in alto e il temporale il 10% in basso —
    il fulmine pende, di proposito. È la composizione dell'illustratore, non un
    difetto: una tabella di correzioni per icona sarebbe l'app che discute con la
    propria grafica, e romperebbe la linea d'orizzonte condivisa dalla famiglia.
  - **La guida riscritta** (chiesta dal committente insieme alla passata widget):
    - **Il capitolo «perché non c'è il radar» esce.** Valutato e condiviso: una guida
      è il posto dove un prodotto dice cosa fa, non dove difende ciò che non è, e
      metterlo terzo su quattro capitoli piazzava un'assenza nel posto migliore della
      pagina. La metà utile — dove sta la risposta a «sta per piovere?» — sopravvive
      dentro il giro di Oggi, senza nominare il radar.
    - **Da quattro domande a un giro delle quattro schermate**: la mappa della barra
      in basso (con le icone vere), poi Oggi, Cielo, Avvisi, Diario, poi luoghi,
      widget, impostazioni, e in chiusura da dove arrivano i numeri. Ogni schermata:
      una frase su cosa risponde, poi le sue funzioni una per una, col titolo in
      primary e la riga che dice a cosa serve.
    - **Riferimenti grafici, non screenshot**: la guida mostra i componenti VERI —
      i quattro chip di verdetto (già c'erano), una riga di momento del Cielo col
      marcatore «Domani», il chip di freschezza, due tessere dei dettagli, una
      miniatura della striscia di deriva sulla rampa della pioggia — ognuno con la
      didascalia che dice che è un esempio. Uno screenshot invecchia al primo
      restyling; un componente vero no, e segue tema, unità e stile delle icone.
    - **La regola che resta**: la guida non insegna un comando. Dice a cosa serve una
      schermata e cosa può fare, mai quale bottone premere — un elemento che ha
      bisogno di spiegazioni resta un bug di questa edizione (VISION §5.7, riscritto).
    - Settanta stringhe nuove in due lingue in una sola sessione sono esattamente il
      posto dove se ne dimentica una: arriva `StringsParityTest` (ogni stringa
      traducibile esiste in entrambe le lingue, nessun nome dichiarato due volte,
      stessi argomenti di formato nelle due versioni). Ha già pagato l'affitto: ha
      trovato i doppioni del vecchio capitolo Avvisi rimasti nel file.

## Verifica su device (committente, 4 set 2026) — quattro difetti, quattro correzioni

Quattro segnalazioni dopo l'uso vero, nessuna di gusto: tre sono comportamento sbagliato
e una è una forma che non regge.

- **«La mia posizione» non seguiva chi si sposta.** Il fix GPS veniva ripreso solo
  all'accensione della sorgente (`enableGps`) e al tocco della riga nel foglio dei luoghi
  (`selectGps`); il pull-to-refresh di Oggi chiedeva soltanto il meteo per il fix
  *salvato*. Chi cambiava paese e aggiornava a mano otteneva numeri freschi del paese che
  aveva lasciato. Ora `TodayViewModel.refresh` sulla pagina della posizione **riprende
  prima la posizione e poi il meteo**, e anche l'atterraggio sulla pagina (`setActive`, il
  pager che si assesta) rifà il fix — con una soglia di 5 minuti, perché il settle scatta
  a ogni avvio e a ogni swipe di ritorno e «la batteria è una funzionalità». Un fix che
  fallisce è **silenzioso** e tiene l'ultimo: posizione vecchia con meteo vero batte un
  errore sopra numeri ancora giusti. Quando il fix si sposta, la `cacheKey` cambia, la
  pagina si ricostruisce e lo stato della nuova chiave fa il suo fetch da solo — chiedere
  anche un refresh spenderebbe due GET sullo stesso arrivo. Il pull resta girevole durante
  l'acquisizione (`locating`): un fix può prendere quindici secondi, e un gesto che non
  risponde si legge come rotto. La **posizione in background resta fuori discussione**:
  il worker continua a usare l'ultimo fix persistito.
- **I widget non si riaggiornavano.** Il difetto vero della fase 8, e non era nostro
  codice mancante ma un contratto di Glance letto male: `provideGlance` gira **una volta
  per sessione**, e la sessione resta viva ~45 secondi dopo la prima composizione
  (`TimeoutOptions.initialTimeout`, +5 s per evento). Dentro quella finestra `update()`
  non riesegue la funzione: manda `UpdateGlanceState` alla composizione già aperta, che
  ricompone con lo **stesso modello** caricato prima di `provideContent`. AndroidX lo
  scrive nel KDoc di `provideGlance` («observe your sources of data within the
  composition»). Da qui tutti e tre i sintomi: refresh manuale, sync del mattino, e
  soprattutto il «a volte» delle proprietà del widget — fuori finestra la sessione era
  scaduta, `update()` ne apriva una nuova e sembrava funzionare. Correzione:
  `WidgetRefresh` (un contatore in-process) più `rememberWidgetModel`, che rilegge il
  modello **dentro** la composizione a ogni tick; `ChiaroWidgets.updateAll/updateOne`
  invalidano prima di ridipingere, così la stessa chiamata copre sia la sessione viva sia
  quella da aprire. Contatore in memoria e non persistito di proposito: una sessione non
  sopravvive al processo che la esegue, quindi alla morte del processo muore anche la
  composizione vecchia. Anche `skyGradientBitmap` e gli schemi passano dentro la
  composizione, `remember`izzati sul dato che li muove (è una bitmap 320×320: una
  ricomposizione non è un motivo per allocarne un'altra).
- **Widget trasparente illeggibile.** Sotto `InkTrustFloorPct` (50%) la card non è più il
  terreno dell'inchiostro e la decisione passava alla tappezzeria — ma quando
  `getWallpaperColors` non risponde (colori non ancora estratti, live wallpaper, OEM che
  torna null) il ripiego era il **tema del telefono**. Tema chiaro su sfondo nero =
  scritte nere su nero; lo stesso telefono in scura andava bene, che è esattamente il
  racconto della segnalazione. Ora l'`HINT_SUPPORTS_DARK_TEXT` è letto per quello che è,
  un segnale **affermativo**: inchiostro scuro solo dove il sistema dichiara il terreno
  chiaro, chiaro in tutti gli altri casi (e la schermata di blocco è interrogata come
  seconda fonte). Il ripiego non può sbagliare nello stesso verso, perché una tappezzeria
  chiara è precisamente il caso che l'hint esiste per annunciare. In più: **LIGHT e DARK
  scelti a mano decidono l'inchiostro a qualsiasi opacità** — prima, a opacità zero,
  scegliere «scuro» consegnava comunque la decisione alla tappezzeria, quindi chi si
  ritrovava il widget illeggibile non aveva **nessuna** via d'uscita. La regola è pura
  (`widgetInk` in `WidgetInk.kt`) e fissata da `WidgetInkTest`: uno screenshot non può
  pinnarla, perché il guasto si vede solo sui telefoni la cui tappezzeria non pubblica i
  colori, che è il caso che nessuno ha davanti.
- **Notifiche: chiusa e aperta sono due testi.** Segnalazione del committente: espandere
  una notifica restituiva esattamente quello che già diceva — tutti e tre i notificatori
  passavano la STESSA stringa a `setContentText` e a `BigTextStyle.bigText`. tweather fa
  già la cosa giusta (riga singola ripiegata / oggetto JSON stampato), e qui vale lo stesso
  principio nel registro di questo prodotto: niente JSON, prosa. Chiusa resta la frase (il
  sistema le dà una riga e taglia il resto); aperta la frase diventa il titolo e sotto va
  il resto, **un fatto per riga con la sua conseguenza** — la regola della griglia dei
  dettagli (DESIGN §1.2) applicata al posto in cui si legge prima di aprire l'app.
  - **Maltempo e pioggia**: la finestra vera (`AlertDetails`, puro e con tabella di test)
    è la corsa di ore consecutive attorno all'ora dell'avviso, non l'ora sola che
    l'`Alert` porta per il suo fingerprint; più il picco di pioggia con la sua ora,
    l'escursione di temperatura nella finestra, e le due letture di adesso.
  - **Riepilogo del mattino**: alba e tramonto, UV massimo, vento e aria, ognuno con la
    riga che dice cosa farne (`WeatherText` era già scritto: le bande sono le stesse dei
    dettagli di Oggi, quindi zero voce editoriale nuova da mantenere).
  - **Regole del lettore**: il messaggio resta suo e in cima; sotto, «Perché è scattata»
    e ogni condizione come la frase che mostra la schermata Avvisi, col valore letto
    accanto (`RuleText.sentence` + `RuleVariables.resolve`). Un verdetto viaggia con la
    sua aritmetica, e «perché è partita?» è l'unica domanda che una regola scattata pone.
    Una variabile non risolvibile stampa la frase **senza** lettura: il motore si rifiuta
    di chiamare «falso» un dato assente, e la notifica non può disfarlo a parole.
  - **Promemoria del cielo**: gli stessi pezzi smettono di dividersi una riga, e sotto
    arriva la spiegazione del momento dal catalogo — già scritta e già stampata dalla
    schermata Cielo, quindi le due non possono divergere.
  - **Onestà del blocco**: una corsa che arriva in fondo alla previsione dice «dalle 17:00»
    e si ferma (una fine mai vista sarebbe la bugia che il lettore non può verificare);
    una riga senza dato non si disegna; il vento dice «adesso» **a parole**, perché le ore
    previste non portano vento e stamparlo nudo sotto una finestra di temporale lo
    farebbe leggere come il vento del temporale.
  - **Conseguenza sull'interfaccia di `:core:sync`**: `notifyAlert` riceve ora il
    `WeatherReport` e le `UnitSettings` intere. Il `Alert` del dominio resta stretto quanto
    il suo dedup richiede — allargarlo per far parlare una notifica sarebbe stato il
    contrario del taglio dei moduli.

- **Widget Cielo: tanti momenti quanti ce ne stanno.** Chiesto dal committente (4 set):
  ne mostrava sempre e solo uno. Non era una scelta di layout, era l'unico dei tre widget
  rimasto sul `SizeMode.Single` di default — quindi `LocalSize` gli riportava la dimensione
  **minima dichiarata nel provider** e non sapeva di essere stato allargato. Passa a
  `SizeMode.Exact` come Ora e Oggi, e l'altezza concessa decide: una cella resta l'eroe con
  la sua pillola esattamente come prima, ogni cella in più aggiunge righe compatte (glifo
  20dp, nome, quando, e la **parola** del verdetto nel colore del verdetto). La parola e non
  la pillola, e senza il numero: in una riga così ci sta uno dei tre, e DESIGN §2.3 dice
  quale — un verdetto è un glifo e una parola prima di essere un colore; l'aritmetica resta
  a un tocco di distanza sulla schermata che ha lo spazio per stamparla.
  - La lista è `SkyUpcoming.allAt`, cioè `firstAt` che smette di buttare via N-1 risposte:
    stesso ordinamento delle righe programmate della schermata Cielo, che è l'invariante
    protetta da quando le due superfici stampavano due albe diverse (3 set). Un widget che
    ne stampa PARECCHIE ha di che discordare molto di più.
  - Il budget (`skyExtraRows`, puro e con tabella in `SkyWidgetRowsTest`) è aritmetica sulle
    altezze vere della card, non una tabella di dimensioni di cella: i launcher non sono
    d'accordo su cosa sia una cella, e l'unica cosa che riportano tutti onestamente è
    quanti dp hanno concesso. 24 di padding, 80 per eroe+pillola, 26 per ogni riga.
  - Un eroe **senza** verdetto (mezzogiorno solare, la fase lunare: niente che le nuvole
    possano rovinare) libera i 32dp della pillola, e una riga se li prende. Spazio vero
    lasciato vuoto sarebbe il widget che si rifiuta di dire una cosa vera che ci sta.
  - La lista non si imbottisce mai: quattro sottoscrizioni disegnano quattro righe su un
    widget che ne reggerebbe sei. Inventare un quinto momento è l'unica cosa che questo
    widget non deve fare.
- **Widget Ora: lo stato del cielo accanto alla temperatura**, spento di default e acceso
  per singolo widget dalle sue proprietà. Valutata e **scartata** la comparsa automatica sul
  ridimensionamento (proposta e respinta dal committente, e a ragione): il widget cambierebbe
  contenuto mentre se ne trascinano le maniglie, e un widget che si riscrive da solo mentre
  lo si dimensiona non si può mirare. Acceso disegna sempre; su una card stretta la riga si
  tronca, che è una cosa che si vede e si disfa. Spento di default perché ogni widget già
  piazzato deve tenere il vestito con cui è stato messo. L'interruttore compare **solo** per
  i widget Ora (`ChiaroWidgets.isNowWidget`): la schermata di configurazione è una sola per
  tutti e tre, e un'opzione che due di loro ignorano non va offerta. Allineamento a
  `Alignment.Bottom` perché Glance non ha l'allineamento alla linea di base: l'eroe dell'app
  mette due corpi su una riga con `alignByBaseline` e dice perché (TodayScreen, 2 set); da
  questa parte dello steccato il fondo delle due scatole è la cosa vera più vicina.
- **I nomi dei modelli di avviso hanno l'iniziale maiuscola** («Bici», non «bici»). Non è
  gusto: il minuscolo arrivava da tweather, dove `alerts.rules` è un file di configurazione
  e un identificatore minuscolo è **registro codice** — l'unico registro che questo prodotto
  ha buttato via di proposito (CLAUDE.md). Qui il nome sta in posizione di titolo: la card
  della regola e la notifica «Bici» · Milano. Cambia **solo il seme**: una regola già salvata
  tiene il nome che le ha dato il lettore, che è testo suo e non nostro da correggere.

- **Ritocco sui due widget, sullo screenshot del device (committente, 4 set sera).**
  - **Ora, lo stato**: a 15sp e 6dp dal numero si leggeva come una cosa attaccata al
    segno di grado. Ora 20sp — circa tre quinti dell'eroe, che mette tre gradini chiari
    sulla card (34 il numero, 20 cosa fa il cielo, 15 dove) invece di due corpi che si
    fanno concorrenza — e 12dp d'aria, i 12 della card. Allineamento **ottico**
    (`CenterVertically`) e non più al fondo: Glance non ha l'allineamento alla linea di
    base, ma a questi due corpi il font di sistema mette l'inchiostro visibile quasi
    esattamente al centro della propria scatola, quindi centrare le scatole centra gli
    inchiostri — che è poi il trattamento che una parolina accanto a un numerone vuole.
  - **Cielo, il verdetto delle righe**: il verde nudo era poco leggibile su card scura.
    La causa è la stessa dell'inchiostro trasparente di stamattina — gli inchiostri dei
    verdetti sono misurati contro la **superficie dell'app**, e il terreno di un widget è
    un cielo scrimato, una tappezzeria, o quello che ha scelto il lettore. Inchiostro e
    contenitore sono una coppia **misurata** (`PaletteContrastTest`: «verdict ink reads on
    its own container», 4,5:1), quindi un chip si porta dietro il proprio terreno ed è
    leggibile su qualunque card. È esattamente il motivo per cui l'eroe ha una pillola
    dal primo giorno; le righe ne prendono la sorella piccola, parola sola.
  - **Cielo, «Tramonto19:55»**: la `Row` della riga compatta non era `fillMaxWidth`, quindi
    il `defaultWeight` sul nome non aveva niente da distribuire e l'ora gli restava
    incollata. E il disallineamento verticale era nome 13sp contro ora 12sp: due corpi
    diversi centrati sono due linee di base che si mancano. Stessa dimensione per
    entrambi, `fillMaxWidth` sulla riga, e 8dp prima dell'ora.
  - **Cielo, la composizione**: una lista che finisce prima della card lasciava tutto in
    alto e un buco in fondo. Ora il blocco sta in mezzo allo spazio che non riempie; con
    zero righe la pillola tiene il bordo inferiore su cui era stata tarata (3 set).
  - Il budget cambia di conseguenza (riga 22dp + 6 di stacco, e 10dp d'aria fra il momento
    e la sua lista, contati una volta sola perché contarli dentro il ciclo farebbe
    dipendere il budget dalla propria risposta). `SkyWidgetRowsTest` aggiornato: prima riga
    a 142dp di concessione, a 110 se l'eroe non porta verdetto.

- **Massima e minima tornano sui widget Ora e Oggi** (committente, 4 set sera), ancorate
  al bordo destro e all'altezza della temperatura invece che sotto il numero. Erano state
  tolte al terzo giro perché «accanto a un numero da 34sp si leggevano come disordine»:
  il problema era **dove stavano**, non la coppia. Massima prima e in inchiostro forte,
  minima dopo e attenuata — che è l'enfasi delle righe della settimana (`DayRow` stampa la
  minima in `onSurfaceVariant` e la massima in quello pieno), quindi la coppia dice quale
  è quale senza una parola per dirlo. Accese di default, spegnibili per singolo widget.
  Sul widget Ora è lo stato a prendere lo spazio elastico e a troncarsi: pesare le parole
  invece che spaziarle decide chi cede su una card stretta, e uno stato che vincesse la
  discussione spingerebbe i numeri fuori dal widget.
- **Spaziature: verificate, e una sola era davvero sbagliata.** Domanda del committente
  («le spaziature ti sembrano troppo grandi?») sullo screenshot di Oggi. Misurate contro
  DESIGN §6: 24dp fra sezioni, 12 sotto un'intestazione, 16 di margine, righe della
  timeline a 36dp di passo — che è più **stretto** dei 48 di una riga di lista Material,
  non più largo. Quindi no, il ritmo verticale è giusto. Quello che si vedeva era altro:
  - **La sparkline della pioggia su una giornata asciutta.** Con tutte le ore a 0% il
    tracciato è una riga piatta sul fondo di una scatola da 28dp: sullo schermo si legge
    come un separatore capitato lì con un buco sopra, e non dice niente che la fila di
    «0%» sopra non abbia già detto. §1.1 — una sezione senza dati non si disegna, e un
    grafico di soli zeri è una di quelle. Via anche la sua descrizione per TalkBack, che
    non aveva più chi la chiamasse.
  - **Il ritmo delle intestazioni era cresciuto in tre valori diversi su cinque schermate**
    (20 su quattro, 12 su Oggi dove i 12 di `spacedBy` pagano la differenza; e i gruppi a
    16 in due posti, 20 in un terzo). Nessuno se n'era accorto perché nessuno li aveva mai
    messi in fila. Ora i tre numeri stanno in un posto solo (`SectionTop`, `GroupTop`,
    `SectionBottom` in `ui/theme/Shape.kt`) con l'aritmetica scritta — quello che
    l'intestazione **spende**, non quello che si vede, perché il vicino ci aggiunge il suo
    — e DESIGN §6 lo dice invece di lasciarlo dedurre.
  - **Correzione, subito dopo** (domanda del committente: «adesso è omogeneo?»). Verificato
    riga per riga invece che a memoria: **no**, e la prima risposta era troppo generosa.
    Il costo dell'intestazione ora è uniforme davvero (una sorgente, tre costanti, sei
    punti di chiamata), ma il divario che si vede no, perché il vicino non è lo stesso
    ovunque: righe dell'app con 8dp su Oggi e nelle note, `FilterChip` senza padding
    verticale nel Diario, e soprattutto **`ListItem` di Material** per i momenti del Cielo,
    gli interruttori e le card degli Avvisi, e le righe delle Impostazioni — che porta il
    proprio padding e una altezza minima che centra il testo. Quel caso **resta com'è**:
    una lista fatta con `ListItem` si legge bene perché segue la piattaforma, e limare due
    dp a un componente per centrare un numero scritto in un documento sarebbe il sistema
    di design che discute con Material su una cosa che nessuno può vedere. DESIGN §6 e il
    KDoc delle costanti dicono adesso questo, invece dell'aritmetica generalizzata che
    valeva solo per due schermate su cinque.

- **Il canvas finisce diritto.** I due angoli inferiori portavano un raggio di 28dp: si
  leggeva come una card che galleggia sopra lo scroll invece che come il cielo con cui la
  schermata si apre. Tolti; `DESIGN.md` §6 aggiornato (il cielo non ha angoli).

---

## Review della posizione (committente, 4 set 2026) — lo spinner all'avvio e la strategia dei fix

Una review dell'intero percorso della posizione, chiesta dopo che «ogni volta che apro
l'app c'è lo spinner che gira per un po'». Lo spinner era la punta: sotto c'era una
strategia di acquisizione rovesciata rispetto alle linee guida della piattaforma, e una
soglia che non sopravviveva al processo.

**Cosa era giusto e resta com'è**: solo `ACCESS_COARSE_LOCATION`, niente play-services,
nessuna posizione in background (il worker e i widget usano l'ultimo fix persistito e se
non c'è escono), coordinate arrotondate a due decimali prima di uscire, permesso chiesto
in contesto dopo la frase che lo spiega. Su queste cose Chiaro è più rigoroso della media
della categoria e non è stato toccato niente.

- **Lo spinner a ogni avvio.** Catena deterministica: il pager si assesta sulla pagina
  attiva → `setActive` → `refix` → la soglia dei 5 minuti si leggeva da `lastFixAt`, un
  campo del ViewModel, quindi `null` a processo appena nato e quindi *sempre* dovuta →
  15 s di timeout più 5 s di geocoding con `locating` acceso → e `TodayScreen` legava
  `locating` (e `content.refreshing`) al `PullToRefreshBox`. L'indicatore del pull, per
  contratto Material, vuol dire «sto facendo quello che mi hai appena chiesto»: acceso da
  codice per un aggiornamento automatico dice una bugia sull'origine, e viola la regola
  scritta in VISION §5.2 e DESIGN §1.4 — *content first, freshness stated, refresh
  silent*. Ora `refreshing` si chiama `userRefreshing` e lo alza solo `refresh()`; il
  fetch automatico non spende nemmeno un frame per dire che è partito, e quello che il
  lettore sente dire di un aggiornamento automatico resta il chip di freschezza, che
  l'età vera la dice comunque. Nota: tweather la distinzione ce l'aveva già
  (`revalidateFix` non accende `acquiringFix`); si era persa nel reskin.
- **La soglia ora sopravvive al processo.** `CityStore` persiste `gps_fixed_at` accanto
  a `gps_city_json` e `LocationSettings` lo porta: un avvio a freddo dietro un fix di due
  minuti fa non chiede più niente a nessuno. Era il difetto che costava di più — il
  processo muore molto più spesso di quanto cambi la città in cui ti trovi.
- **Prima l'ultimo noto, poi l'acquisizione.** `AndroidLocationProvider.currentFix`
  prende ora `maxAge` e `timeout`: sotto `maxAge` risponde con la posizione che il
  sistema già possiede (gratis, istantanea, nessuna radio accesa), e solo oltre avvia
  `getCurrentLocation`. È l'ordine che la documentazione Android raccomanda e che le app
  meteo diffuse usano; prima era esattamente il contrario, con l'ultimo noto relegato a
  ripiego dopo il timeout. L'ultimo noto viene anche **cercato su tutti i provider
  abilitati** e **filtrato per età** (`elapsedRealtimeNanos`, monotono): senza tetto
  poteva essere di ieri o della città da cui eri tornato, ed entrava come «dove sei
  adesso». Tetto di 24 ore per il ripiego. Il timeout del percorso silenzioso scende a
  8 s: dietro contenuto già disegnato, quindici secondi non li aspetta nessuno.
- **Un'azione, un fix.** `CachedLocationProvider` avvolge il provider vero nel
  `ServiceLocator` e tiene l'ultimo `GeoFix` con il suo istante, dietro un `Mutex`.
  Attivare il GPS costava due acquisizioni di fila — `PlacesViewModel` prendeva il fix,
  la scrittura sullo store spostava il pager sulla pagina posizione, e il settle ne
  chiedeva un altro a un oggetto che del primo non sapeva niente — perché la soglia
  stava dentro i due ViewModel invece che sotto entrambi. La finestra di coalescenza è
  di 10 secondi: la larghezza di una catena di UI, e ben sotto l'intervallo in cui possa
  cambiare qualcosa che il lettore noti.
- **Un fix fallito lo dice, se qualcuno l'ha chiesto.** Restava silenzioso anche dopo un
  pull, e anche quando il motivo era che il permesso non c'era più: la pagina «La mia
  posizione» mostrava per sempre il posto di prima senza che nulla lo segnalasse (il chip
  di freschezza parla dell'età del *meteo*). Ora il fallimento automatico è muto come
  prima — posizione vecchia con meteo vero batte un errore sopra numeri ancora giusti —
  e quello esplicito arriva come snackbar sulla pagina, con la stessa frase che il foglio
  Luoghi dice da sempre (`asGpsError`, una sola mappatura per tutti). `selectGps` mostra
  anche `GpsState.Acquiring`: un tocco che prende quindici secondi e non mostra niente si
  legge come una riga morta.
- **Aggiornamento al ritorno in primo piano.** Il fix si riprendeva a ogni avvio a
  freddo — quando è meno probabile che sia sbagliato — e mai al risveglio a processo
  vivo, quando è più probabile: il settle del pager non si ripete. `LifecycleEventEffect`
  su `ON_RESUME`, silenzioso e sotto la stessa soglia, quindi di solito non fa niente.
- **La soglia di adozione, 2 km.** `cacheKey` si muove su un reticolo da ~1,1 km e lo
  stato della pagina è indicizzato su quella: ogni cella attraversata ricostruiva la
  pagina dallo scheletro, spendeva due GET e ricominciava la storia del Diario. Un
  chilometro non è «movimento vero» per una previsione (i modelli risolvono fra 1 e
  11 km). `CityStore.adoptGpsFix` decide adesso per tutti, dentro un solo `edit`: sotto
  `FixAdoptionMeters` le coordinate restano dove sono e si prende solo il nome migliore
  che il geocoding ha trovato — e un geocoding fallito non sovrascrive un nome che
  funzionava, perché il suo nome sarebbe l'etichetta di coordinate che non stiamo
  adottando.
- **Il geocoding riceve le coordinate arrotondate.** Tutto il resto arrotondava a due
  decimali prima di uscire; la chiamata al `Geocoder` passava `location.latitude` e
  `location.longitude` intatti, cioè la coordinata più precisa che l'app possiede
  finiva all'unico servizio che l'app non controlla. Ora si arrotonda una volta sola,
  all'inizio.
- **Le due frasi sulla privacy dicevano il falso.** «Non lascia mai il telefono» non è
  vero: le coordinate arrotondate vanno a Open-Meteo — è l'unico modo per avere una
  previsione di dove sei — e il reverse geocoding passa dal `Geocoder` di piattaforma,
  che su quasi tutti i dispositivi è un servizio di rete. In un prodotto la cui prima
  regola è che lo schermo non deve mentire, e con la scheda Sicurezza dei dati da
  compilare in modo coerente, era il punto più esposto del percorso. Riscritte in IT e
  EN dicendo la cosa vera, che è comunque un'ottima cosa da dire.
- **Il primo disegno non aspetta più il Diario.** In `cityStates` la prima `push()`
  arrivava dopo `cachedReport()` *e* dopo `refreshChanged()` — una query su Room e dodici
  decodifiche JSON per una sezione in fondo alla pagina. Ora il report in cache esce
  subito e le revisioni arrivano con la seconda emissione.
- **`runCatching` ingoiava anche la cancellazione** nei due punti che prendevano il fix
  senza `try`/`catch` tipato: ora catturano `WeatherException`, come `enableGps` faceva
  già.
- **Manifest**: `<uses-feature android:name="android.hardware.location"
  android:required="false" />`. Dal solo permesso il Play Store deduceva l'hardware come
  necessario e filtrava l'installazione sui dispositivi che non ce l'hanno; i luoghi
  salvati sono il modo principale di usare Chiaro.
- **I test che mancavano.** `LocationProvider` aveva scritto in testa «astratta così i
  test dei ViewModel possono simularla» e non esisteva un solo test che la simulasse. Con
  la soglia spostata sotto il provider e l'adozione dentro lo store, il percorso è
  diventato la cosa più facile da testare del progetto: `CachedLocationProviderTest`
  (memo, coalescenza, due chiamanti insieme, fallimento non memorizzato), i tre casi
  dell'adozione in `CityStoreTest`, la distanza in `GpsLocationTest`. Sparisce il vecchio
  `updateGpsCity rejects a regular city`: la firma prende un `GeoFix`, quindi una città
  qualsiasi non è più esprimibile e il `require` non aveva più niente da intercettare.
- **Da riportare a monte**: `LocationProvider.kt` era identico nei due repository, quindi
  ultimo-noto-per-primo, tetto d'età e arrotondamento prima del geocoding sono difetti
  anche di tweather (`UPSTREAM.md`, e la regola delle Note trasversali: quando lo stesso
  bug va corretto due volte, si estrae `weather-core`).

---

## Il foglio di una regola (committente, 4 set 2026) — tre difetti di forma

Tre segnalazioni sullo stesso foglio, quello che si apre toccando un avviso proprio. La
terza è arrivata come domanda — «sarebbe meglio penso che compaia già tutta visibile, sei
d'accordo?» — e sì: è la prima causa, e le altre due sono in buona parte la sua coda.

- **Il foglio si apriva a metà.** `ModalBottomSheet` parte parzialmente espanso quando il
  contenuto supera metà schermo, ed è lo stato giusto per una **lista**: sotto la piega
  c'è dell'altro, e il gesto per vederlo è lo stesso gesto con cui la si scorre. L'editor
  non è una lista, è un **modulo**: nome, condizione, messaggio e prova sono una frase
  sola, e leggerne metà non serve a niente. Ora `skipPartiallyExpanded = true`, come già
  fa il foglio dei luoghi dalla Fase 3 — e siccome la colonna non chiede tutta l'altezza,
  il foglio si apre alto quanto quello che ha da dire, non a schermo pieno per principio.
- **La prova a vuoto rispondeva sotto la piega.** Con il foglio a metà, «Prova adesso»
  stampava la risposta appena fuori dalla vista: chi aveva appena fatto la domanda doveva
  trascinare il foglio per leggere la risposta, che è il contrario di quello che una
  prova a vuoto promette (VISION §5.4 — dice cosa farebbe, subito, senza mandare niente).
  Il punto sopra di solito basta; la prova porta comunque la risposta in vista da sé.
  Due frame di attesa prima di scorrere, e sono due per un motivo: la riga si **compone**
  sul primo e si **misura** sul secondo, e `maxValue` la conosce solo da misurata — con un
  frame solo si scorre alla fine di ieri. Nessun rischio quando non c'è niente da
  scorrere: `maxValue` è 0 e lo scorrimento programmatico non passa dal nested scroll,
  quindi non trascina il foglio.
- **La risposta era fuori squadra rispetto alla domanda.** Il risultato è un `Text` e
  parte dal margine del foglio (16dp); «Prova adesso» è un `TextButton` e Material gli
  mette 12dp di padding interno, quindi l'etichetta partiva da 28. Due bordi sinistri a
  12dp di distanza su due righe attaccate: si vede, ed è quello che il committente ha
  visto. I tre bottoni testuali del foglio (aggiungi condizione, prova, elimina) prendono
  ora `FlushTextButtonPadding` — orizzontale a 0, verticale agli 8dp che erano già suoi —
  così ogni elemento del foglio comincia sullo stesso bordo: il riquadro dei due campi di
  testo, i chip, i titoli, le risposte, le azioni. Il tocco resta di 48dp, che è il minimo
  di Material e non viene da questo padding.

  Non contraddice la decisione del 4 set sui `ListItem` («una lista fatta con `ListItem`
  si legge bene perché segue la piattaforma, e limare due dp per centrare un numero
  scritto in un documento sarebbe il sistema di design che discute con Material»): là il
  divario era verticale, dentro righe diverse, e nessuno poteva vederlo; qui sono due
  bordi sinistri su due righe consecutive, cioè la cosa che l'occhio misura meglio di
  ogni altra. La regola resta: si segue Material finché non si vede.

E due cose chieste subito dopo, sullo stesso foglio.

- **La risposta della prova non sopravvive alla domanda.** Restava sullo schermo mentre
  si modificavano i chip sotto, quindi un verdetto su un avviso stava sotto un altro
  avviso — «lo schermo non mente» vale anche per una risposta che invecchia. Ora sparisce
  quando cambiano le **condizioni** (`LaunchedEffect(rule.conditions)`), non quando cambia
  il messaggio: riscrivere le parole che l'avviso direbbe non cambia *se* scatta, e la
  risposta cita comunque il testo con cui è stata calcolata.
- **I valori nel messaggio si scelgono da una lista.** Domanda del committente: «ci sono
  vari campi tra graffe? Sarebbe utile un help». Sì, e non era scritto da nessuna parte:
  `RuleMessages` interpola **ogni** nome del registro (23 variabili) oltre ai due del
  trigger, e la riga di aiuto sotto il campo ne nominava due e si fermava lì. La risposta
  giusta in questo prodotto non è un elenco da copiare a mano ma la stessa mossa del
  costruttore di regole (VISION §5.4: mai una sintassi, si tocca): «Aggiungi un valore»
  apre lo **stesso vocabolario in parole** che usa la condizione, e quello che si tocca
  finisce dove sta il cursore. Da qui tre decisioni:
  - il campo diventa un `TextFieldValue`, perché l'inserimento al cursore è l'unica cosa
    che rende inutile ricordare la sintassi (e parte in fondo al testo, così un valore
    scelto prima ancora di toccare il campo accoda invece di finire in testa);
  - le righe della lista mostrano **solo le parole**, non gli id puntati: la regola di
    CLAUDE.md («never reach a screen») regge, perché l'unico posto dove il nome puntato
    compare è dentro il messaggio del lettore, che è testo suo. Se il committente li
    vuole visibili anche nella lista, è una riga di supporto per ogni voce;
  - si inserisce il nome **nelle unità del lettore** (`displayId`): l'engine risolve
    entrambe le grafie, ma `{current.temp_c}` in un messaggio di chi legge in Fahrenheit
    mentirebbe subito, mentre una grafia che invecchia se un giorno cambia unità continua
    comunque a risolvere. `RuleMessages` espone ora `TriggerValue`/`TriggerTime` e
    `placeholder()`, così le graffe le conosce un posto solo invece di due.
  - la lista offre 21 valori su 23: restano fuori i due `wmo_severe`, che si
    interpolano come `true`/`false` — né un numero né una parola che questo prodotto
    direbbe, quindi offrirli metterebbe una parola di codice dentro la frase del
    lettore. Una regola può continuare a guardarli, è solo stamparli che non ha senso.
  - La riga di aiuto sotto il campo perde i due token e rimanda alla lista, e la guida
    (§ Avvisi) dice che ogni valore guardabile è anche stampabile.

---

## La griglia dei dettagli (committente, 4 set 2026) — due icone uguali, un'etichetta a capo

Screenshot da device: «le icone delle schede dettagli non sono chiare e due sembrano
uguali (umidità e punto di rugiada)», e «Punto di rugiada» che va a capo. Due difetti
diversi con una radice sola ciascuno.

- **Le icone erano tinte piatte.** `MetricTile` passava `tint = onSurfaceVariant` a
  `Icon`, che ricolora l'intero vettore: un Meteocon diventa la sua sagoma. È l'unico
  posto dell'app che lo faceva sui disegni meteo — `HourStrip`, `DayRow` e
  `TimelineRow` passano `Color.Unspecified` da sempre — e contraddiceva DESIGN §13.1
  («the icons keep their own colors under every theme»: dipingono il mondo, come il
  canvas). Il conto della sagoma, tessera per tessera: l'umidità perde il **%** bianco
  che la rende umidità (Meteocons disegna `humidity` come `raindrop` con la percentuale
  sopra) e diventa una goccia qualunque, cioè **esattamente** la goccia del punto di
  rugiada; il barometro perde la lancetta rossa e diventa un disco; le tre particelle
  della qualità dell'aria si fondono in una macchia; la nuvola dei pollini si salda ai
  suoi trattini. Con i colori rimessi, sei tessere su otto si leggono da sole.
- **Ma due gocce restano due gocce.** Rimesso il colore, umidità e punto di rugiada
  restano lo **stesso disegno** distinto da un glifo bianco che va cercato — in due
  schede appaiate. Un punto di rugiada è una temperatura, quindi prende lo strumento che
  la legge: `dewPoint` passa da `mc_raindrop` a `mc_thermometer` (§13.1: «l'accessore
  nomina la metrica, non il disegno» serve a questo). Cade l'accessore `temperature`,
  che non era usato da nessuno ed era il secondo nome dello stesso disegno; `mc_raindrop`
  resta nella famiglia, semplicemente non lo chiede più nessuno schermo.
- **La più debole delle otto resta la qualità dell'aria.** Meteocons v2 non ha un'icona
  AQI: `smoke-particles` sono tre cerchi che nei set fill si fondono, perché
  l'importatore lascia cadere i filetti da 0.5 (departure #5) che nell'originale li
  separavano. Provata `smoke`, scartata: è una nuvola, e finirebbe **accanto** alla
  nuvola dei pollini, cioè si scambierebbe un segno debole con una coppia confondibile —
  il difetto appena corretto. Resta com'è, con la stessa scadenza già scritta per i
  pollini: la famiglia v3, quando si potrà mescolare.

E l'etichetta, misurata invece che stimata (Inter, 14sp, tracking di `labelLarge`):

- Il budget è quello che avanza accanto all'icona da 24dp: `(360 − 32 − 12) / 2 − 32 −
  32 = 94dp` su uno schermo da 360dp, 108.5dp su quello da ~389dp dello screenshot.
  «Punto di rugiada» misura **113dp** e «Qualità dell'aria» **107dp**: sul device del
  committente andava a capo la prima, su un 360dp vanno a capo tutte e due. Non era un
  caso limite di quel telefono.
- Scartate, con il loro numero: stringere icona e spaziatura (24→20dp, 8→6dp) e il
  padding della scheda (16→14dp) vale 6–10dp e non basta a 360; portare **tutte** le
  etichette a `labelMedium` (12sp) lascia «Punto di rugiada» a 97dp — ancora sopra i 94 —
  e rimpicciolisce otto etichette per una; far rimpicciolire da sola la sola etichetta
  lunga (`autoSize`) tiene il termine ma rompe l'omogeneità che il committente ha chiesto
  per nome.
- Scelta: **etichette scritte dentro il budget**. `metric_dew` → «Rugiada» (56dp),
  `metric_air` → «Qualità aria» (78dp). Ora le otto italiane e le otto inglesi stanno
  tutte su una riga anche a 360dp, con margine, e la scala tipografica resta una sola
  taglia. Il termine intero non sparisce dal prodotto: resta dove c'è spazio, nella
  variabile degli avvisi («il punto di rugiada adesso») e nella prosa della guida. Il
  budget è ora scritto come commento sopra le etichette nei due `strings.xml`, perché la
  prossima etichetta nasca dentro; e `MetricTile` dice perché la riga non si tronca mai
  con i puntini: a scala testo grande va a capo e tiene le parole, che è il modo onesto
  di rompersi.

Non toccato di proposito: la stessa tinta piatta è sulle righe del Cielo (dove
`quiet` **serve**: smorza un momento già passato), sulle voci del Diario e su un campione
della guida. Lì i disegni sono a soggetto singolo — un'alba, una luna — e la sagoma
sopravvive; se il committente li vuole a colori anche lì è una riga per punto.

---

## Il cielo impara le eclissi (committente, 4 set 2026)

Domanda del committente dopo la passata icone: «gli eventi del cielo di Chiaro sono gli
stessi di tweather? Ce ne sarebbero altri interessanti da aggiungere?». La prima risposta
è **sì alla lettera** — i due pacchetti `domain/sky` erano identici riga per riga, package
a parte — e la seconda è questa: **diciannove job nuovi** più un evento che non è
astronomia, scritti qui e riportati in tweather con la sola rinomina del package
(`UPSTREAM.md`: è la prima modifica che viaggia in quella direzione).

### Il buco trovato mentre si rispondeva

`SkyStateBuilder.moments()` prende i job `DAILY`, `events()` prendeva gli `ANNUAL`, e un
job `POLLING` **non aveva casa** sulla schermata Cielo: `moon.phase` si poteva
sottoscrivere dal catalogo, compariva nel widget e armava un promemoria, e sulla
schermata che lo possiede non compariva mai. Al suo posto la schermata sintetizzava da sé
una riga «prossima luna piena» che appariva a prescindere e non portava campanella. Ora il
calendario porta anche gli aperiodici sottoscritti — ed è lì che atterrano i quattro
quarti e le due eclissi, che sarebbero cadute nello stesso buco. La luna piena resta per
tutti (è il momento del ciclo che la gente chiede per nome) ma è il job `moon.full` vero, e
si fa da parte quando quel job è sottoscritto, perché quella riga ha la campanella.

Vincolati alla sottoscrizione, al contrario della metà annuale: una ricerca di eclissi
cammina anni di lune nuove, e farne due per chi non le ha chieste è batteria spesa per
niente sullo schermo.

### I job

- **Le quattro fasi lunari come quattro righe** (`moon.new`, `moon.first_quarter`,
  `moon.full`, `moon.last_quarter`). `moon.phase` resta per «il prossimo quarto,
  qualunque sia».
- **La luna piena più vicina dell'anno**: la lettura onesta di una parola che internet
  regala tre o quattro volte l'anno, da un numero che il motore già aveva.
- **Le due eclissi, per geometria e non per tabella.** Meeus dà alle eclissi un capitolo
  di coefficienti da cui l'istante esce dal numero di lunazione senza calcolare nessuna
  posizione; questo modulo le posizioni ce le ha, quindi un'eclissi è «dov'è l'ombra e
  quanto le passa vicino la luna». Un modello solo per tutto il modulo — la stessa
  ragione per cui `AstronomyEngine` ha una primitiva di altezza e non undici formule — e
  quindi un'eclissi non può litigare col sorgere della luna stampato sopra. Lunare:
  ombra della Terra alla distanza della Luna, ingrandimento 1/85 di Danjon (il 1/50 di
  Chauvenet dava 0.011 di magnitudine in più del catalogo). Solare: i due dischi **come
  li vede questo posto**, cioè tutta la differenza tra la totalità e un morso di sole a
  duemila chilometri. Tagliate a quello che si può guardare da qui — il 12 agosto 2026 il
  Sole tramonta su Milano con l'eclissi in corso, quindi la finestra finisce al tramonto e
  il massimo viene **rimisurato dentro la parte visibile**: dire «coperto al 93%» di un
  massimo sotto l'orizzonte sarebbe descrivere una cosa che il lettore non può vedere.
- **Due finestre di cielo buio**, un passo oltre `darkness.window`: il centro della Via
  Lattea sopra i 10° dentro la notte astronomica (mai, troppo a nord: a Milano culmina a
  15.5°, per quello la soglia è 10 e non 20), e la **luce zodiacale** nelle sere in cui
  l'eclittica sta ripida. La misura è l'altezza del punto dell'eclittica a un quarto di
  giro dal Sole, non l'angolo eclittica-orizzonte: è la stessa informazione in un'unità
  che lo schermo può stampare. Soglia 50°, calibrata sui mesi che le guide osservative
  danno (sere di gennaio-aprile, mattine di settembre-novembre alle nostre latitudini).
- **Quattro fatti annuali**: tramonto più presto e alba più tardi (che **non** sono il
  solstizio: due settimane prima e una dopo a Milano, sette settimane all'equatore —
  finestra di ricerca ±60 giorni proprio per quello), perielio e afelio, inizio e fine
  delle notti bianche sopra i ~48.5°.
- **Tre sciami** dalla stessa lista IMO che la tabella già citava (Alfa Capricornidi,
  Tauridi australi e boreali) e le Delta Aquaridi spostate a 127.0°.
- **La finestra arcobaleno**, l'unico evento non astronomico: l'arco è centrato
  sull'antisole e sale di 42°, quindi esce dall'orizzonte solo col Sole sotto i 42, e se
  in quella luce ci piove lo dicono due numeri già in casa. Sta sulla timeline di Oggi,
  con la probabilità di pioggia su cui si regge e la direzione **in parole** («a est»,
  mai «a 92°»). È una possibilità e il testo lo dice: mai una promessa.

### Il perielio, e perché VSOP87 è entrata nel file

La prima versione minimizzava la distanza a due corpi di Meeus 25 e sbagliava **fino a
cinque ore**, con due istanti su sedici nel giorno sbagliato: vicino al perielio l'orbita
è così piatta che 1e-5 UA di errore sulla distanza sono ore sull'istante. La serie del
raggio di VSOP87 (Meeus appendice III, troncata) porta tutti e sedici gli istanti
pubblicati dall'USNO **entro 43 minuti e nel giorno giusto**.

Una deviazione registrata perché è controintuitiva: la seconda versione **aggiungeva** al
baricentro la correzione lunare (il centro della Terra oscilla ±4671 km attorno al
baricentro Terra-Luna, che è proprio la scala che sposta il perielio di un giorno) e
peggiorava. Il motivo è che l'istante pubblicato **è** quello del baricentro: è quello che
un almanacco intende per «la Terra al perielio». La correzione è uscita e la KDoc lo dice,
perché è esattamente il tipo di cosa che qualcuno rimetterebbe.

### Misurato, non affermato

Le eclissi calcolate che nessuno ha verificato sono dicerie, quindi i test parlano coi
cataloghi: massimo entro 70 s dal *Five Millennium Catalog* della NASA, magnitudine
d'ombra entro 0.003, di penombra entro 0.01, durate delle fasi entro 1.5 minuti; le
circostanze locali a Milano del 12 agosto 2026 entro 15 s e 0.004 di magnitudine dal
calcolatore USNO, tramonto compreso; e la stessa eclissi esce **totale** da Burgos, che è
il test che dice che la parallasse topocentrica è giusta. Le fasi lunari entro un minuto
dalle effemeridi USNO. Tutti i riferimenti sono nei test, con l'URL da cui vengono.

### Quello che resta fuori, con il suo motivo

- **Pianeti e congiunzioni**: non sono «non ne vale la pena», sono un progetto a sé (una
  VSOP87 troncata per i pianeti e i suoi test). `VISION_SKY.md` §5 li lascia differiti;
  le eclissi erano su quella stessa lista e ne sono uscite perché servivano meno cose di
  quante ne dicesse la riga.
- **ISS e satelliti**: TLE dalla rete che scadono in giorni, e un passaggio annunciato in
  ritardo è una notifica su una cosa finita (`VISION_SKY.md` §12).
- **Aurora**: serve il Kp da un'API, fuori dalla regola «niente chiave, funziona
  offline», e alle latitudini di questo prodotto sarebbe un no permanente.
- **Aloni e pareli**: sarebbero una stima travestita da previsione — lo stesso motivo per
  cui lo ZHR di uno sciame non si stampa.

## Il nome della posizione (committente, 5 set 2026) — la provincia al posto del comune

Segnalato su tutte e due le app lo stesso giorno, con due esempi: da Cavenago di
Brianza l'intestazione diceva «Provincia di Monza e della Brianza», da Segrate diceva
«Milano». `LocationProvider.kt` era ancora identico byte per byte a quello di tweather,
quindi il difetto era di tutti e due e la correzione è la stessa in tutti e due
(`UPSTREAM.md`).

**Tre cose, e nessuna è una colpa del geocoder.** Due sono regressioni della review
della posizione del 4 settembre, una c'era da sempre e quella review l'ha esposta.

- **Al `Geocoder` andavano le coordinate arrotondate.** L'avevamo cambiato con questa
  motivazione: tutto il resto arrotonda a due decimali prima di uscire, e la chiamata
  al geocoding riceveva invece la coordinata più precisa che l'app possiede, cioè
  proprio all'unico servizio che l'app non controlla. **La premessa era falsa.** Con
  `ACCESS_COARSE_LOCATION` non esiste una coordinata precisa da proteggere: la
  piattaforma quantizza la posizione su un reticolo da ~2 km e alza l'accuratezza
  dichiarata ad almeno 2 km *prima* che l'app la veda. I due valori indicano la stessa
  cella e l'arrotondamento non comprava un grammo di privacy — che è esattamente il
  genere di frase che quella stessa review aveva riscritto altrove perché diceva il
  falso. Comprava però fino a **679 m** di spostamento a queste latitudini (555 m di
  latitudine, 390 m di longitudine a 45,5°N): su Cavenago sono 421 m verso sud-est, su
  Segrate 373 m verso ovest, abbastanza per uscire da un comune piccolo e finire nei
  campi accanto, dove un comune da rispondere non c'è. Si arrotonda di nuovo solo ciò
  che esce dall'app: il `GeoFix` — e con lui `cacheKey`, la cache, il Diario e la
  chiamata a Open-Meteo — resta a due decimali esatti come prima.
- **Si leggeva un solo indirizzo, e nell'ordine sbagliato.** `getFromLocation(..., 1)`
  più `locality ?: subAdminArea ?: subLocality`: il backend risponde a un punto con una
  **scala** di indirizzi a granularità crescente, e per un punto che non sta su una
  strada il primo gradino può non avere nessun comune sopra — in Italia torna con la
  regione e la provincia e niente in mezzo. Bastava quello perché `subAdminArea`, che
  stava *in mezzo* alla catena, vincesse a mani basse. Una provincia stampata dove va
  il nome del posto è la regola «lo schermo non deve mentire» presa in contropiede: non
  è un dato sbagliato, è un dato di un altro livello messo dove il lettore ne legge un
  altro. Ora si chiedono cinque gradini e si prende il nome più specifico che **uno
  qualsiasi** di loro conosce: comune, poi frazione o quartiere (che è comunque un posto
  in cui una persona può stare), e la provincia solo quando nessun gradino sa altro —
  dove torna a essere la risposta onesta, perché è l'unica che c'è.
- **Fra due posizioni già note vinceva la più recente.** La review ha fatto bene a
  chiedere l'ultimo noto a *tutti* i provider abilitati, ma li ordinava per
  `elapsedRealtimeNanos` e basta. I provider non rispondono con la stessa cosa: fused
  restituisce una posizione che un'altra app ha già pagato, network può restituire la
  cella a cui il telefono è attaccato, e il permesso coarse alza entrambe a 2 km senza
  migliorare la peggiore. Una cella arrivata dieci secondi fa batteva così un fix buono
  di due minuti fa, e rispondeva «Milano» a chi stava a Segrate. Ora si ordina per
  quanto il lettore può essere lontano da ciascuna *adesso*: accuratezza dichiarata più
  quello che può aver percorso da allora (10 m/s, chi attraversa una città e non
  un'autostrada). Il termine sull'età serve al ripiego delle 24 ore, dove un fix ottimo
  di ieri non deve battere uno mediocre di un'ora fa.

**Non toccato**: `maxAge` e la soglia persistita, il ripiego a 24 ore, l'ordine
fused → network → gps, `CachedLocationProvider`, `FixAdoptionMeters` e il permesso solo
coarse. La strategia era giusta; erano sbagliati i tre dettagli sopra. Nota che
l'adozione sotto i 2 km prende comunque il nome nuovo, quindi il nome corretto arriva
sulla pagina della posizione senza aspettare uno spostamento vero.

**Test**: `LocationProviderTest` in tutti e due i repository, identico. Le due decisioni
sono state estratte in due funzioni pure — `geocodedPlace(List<Address>)` e
`expectedErrorMeters(accuracy, age)` — proprio perché fossero verificabili senza un
dispositivo: sei casi sul nome (la provincia scavalcata, la frazione che batte la
provincia, la provincia che resta quando non c'è altro, i campi vuoti, la lista vuota,
regione e paese presi dal gradino che li ha) e cinque su quale posizione vince.

**Verifiche**: 627 test verdi (11 nuovi: 170 `:app`, 457 `:core`), lint 0 errori.

- [x] Verificato su device (committente, 5 set 2026): il nome del comune torna
      corretto, senza riattivare il GPS

---

## La guida agli eventi del cielo (committente, 5 set 2026)

Richiesta: «in tweather è stata aggiunta una guida per gli eventi del cielo, sia come
guida unica sia come descrizione di ogni singolo evento in fase di add. Guarda com'è
fatta l'implementazione e portala anche in Chiaro, nello stile dell'app».

In tweather è la **Fase 23**: `man 7 <job>`, una pagina per ognuno dei 51 job del
catalogo, quattro sezioni sempre nello stesso ordine (NOME, DESCRIZIONE, QUANDO, VEDERE
ANCHE), due porte — `[man]` su ogni riga del picker e `$ man sky` in fondo al file — e
tutto lo schermo, tab comprese, perché una man page è un programma che hai lanciato.

Il problema che risolveva è però **suo**: là i job hanno nomi puntati in inglese perché
il crontab li stampa, e `zodiacal.pm` a chi non sa già che cos'è non dice niente. Qui
quel problema non esiste (VISION §8: non c'è un registro «codice» da proteggere) e ogni
evento ha già un nome in parole e la riga che lo spiega. Resta l'altra metà, identica
nelle due edizioni: **una riga basta per scegliere da un elenco e non basta per capire
che cosa si è scelto**. Quindi si porta la prosa e non la forma — niente `man`, niente
sezioni urlate, niente schermo preso a forza.

### Che cosa è arrivato

- **`ui/sky/SkyGuide.kt`** — il contenuto. La mappa id → pagina (51, totale sul catalogo,
  `error(...)` per un id senza pagina, esattamente come fa `SkyText` per i nomi);
  l'adiacenza del «Vedi anche», simmetrica per costruzione; e il **«Quando succede»
  generato da `SkyJob`**: cadenza, forma, `observable`, `visibilityDependent` e
  `needsDarkness` sono campi che il motore già legge, e una frase scritta a mano su quei
  campi sarebbe una seconda copia della verità, libera di divergere alla prima modifica.
  Scritta a mano c'è solo la descrizione.
- **`ui/sky/SkyGuideScreen.kt`** — l'indice (i sei gruppi del catalogo, ogni voce con
  l'icona e la riga che già mostra il foglio «Aggiungi un momento») e la pagina: nome e
  riga di catalogo in testa, due paragrafi in `bodyLarge` come la guida di Impostazioni,
  «Quando succede» e «Vedi anche» come chip che navigano. Nessun id puntato da nessuna
  parte.
- **Le due porte.** Il pulsante info su ogni riga di «Aggiungi un momento», e l'indice
  dalla schermata Cielo (una riga sotto «Aggiungi un momento») e dal capitolo Cielo della
  guida in Impostazioni.
- **51 pagine × 2 lingue**, portate dalle `sky_man_*` di tweather come `sky_about_*`,
  accostate nel file alla coppia `sky_name_*`/`sky_expl_*` dello stesso evento.

### Le decisioni

- **La pagina si apre DENTRO il foglio del catalogo, non sopra.** A schermo intero
  avrebbe buttato via l'elenco a metà scorrimento, e il gesto indietro sarebbe tornato
  alla schermata invece che alla lista. Dentro il foglio, «indietro» torna all'elenco e
  la pagina si porta dietro il bottone che aggiunge: la domanda «che cos'è» e la risposta
  «allora lo aggiungo» diventano lo stesso gesto, che è tutto il senso di questa porta.
  L'info è un `IconButton` vero e non un secondo `clickable` sulla riga: due bersagli su
  una riga sola, o sono 48dp o è decorazione che si può premere.
- **La seconda porta non è ridondante**, anche se qui il catalogo elenca tutti i 51
  eventi (in tweather il picker offre solo quelli non ancora nel file, ed era quello
  l'argomento). Chi vuole leggere non deve passare da «Aggiungi»: è una guida, non un
  effetto collaterale dell'iscrizione.
- **Il registro, riga per riga.** Tredici pagine su 51 parlavano di righe di crontab, di
  job, del file, di `--notify` e della «tua città»: riscritte una a una in
  evento/voce/app/luogo, in tutte e due le lingue. Una pagina che dicesse «riga» sarebbe
  la biforcazione che si vede da fuori. `SkyGuideTest` lo tiene fermo: nessuna pagina
  stampa un id puntato, né in italiano né in inglese.
- **I gruppi del catalogo si spostano in `SkyGuide`.** L'indice e il foglio mostrano gli
  stessi 51 eventi: due ordini diversi sarebbero l'app che si contraddice, e un test
  verifica che l'indice sia il catalogo, una volta ciascuno.
- **Nell'indice a schermo intero non si aggiunge niente.** Lì si legge; il bottone sta
  nella pagina aperta dal catalogo, dove aggiungere è la conseguenza di aver letto.
- **La guida di Impostazioni resta un giro delle quattro schermate** e non ingoia
  cinquantuno pagine: il capitolo Cielo ci porta con un link, e il capitolo dice ora che
  ogni momento ha anche la sua pagina.

### Verifica

- 10 test nuovi (`SkyGuideTest`): totalità sul catalogo, indice = catalogo una volta
  ciascuno, due paragrafi veri e nessun segnaposto, ogni pagina davvero tradotta, nessun
  id sullo schermo, «vedi anche» che non punta a se stesso né nel vuoto, simmetria con la
  sola eccezione documentata (gli sciami puntano al buio pieno e il buio pieno non
  risponde a tredici sciami), il «quando» che segue i campi del job e un evento non
  osservabile che non incolpa mai le nuvole.
- Suite completa verde, `lintDebug` senza errori.
- **Da provare su device**: il foglio del catalogo con la pagina aperta a schermo piccolo
  (scorrimento annidato dentro il `ModalBottomSheet`), la lunghezza delle pagine a scala
  testo 200%, e il fondo dell'indice aperto dalla scheda Cielo — lì lo `Scaffold` della
  guida tiene i suoi inset di sistema mentre sotto c'è già la barra delle schede, quindi
  in fondo alla lista può avanzare un po' di aria; è la scelta che tiene corretto l'altro
  ingresso (dalla guida di Impostazioni, dove sotto non c'è niente).

---

## L'ora attuale (committente, 6 set 2026) — l'eroe era l'ultima ora, non questo minuto

Segnalata **su tutte e due le app** con la stessa frase: la situazione corrente sembra
un po' spessa, più l'ultima ora che l'ora attuale. È vero, ed è una causa sola —
condivisa, come il resto di `:core`, quindi corretta due volte con la stessa diagnosi
(`UPSTREAM.md`: un difetto del core si corregge anche a monte).

**La lettura di Open-Meteo non c'entra.** Il blocco `current` è pubblicato su una
griglia di quindici minuti: ogni risposta porta `"interval": 900` accanto ai valori e
`current.time` è l'ultimo quarto d'ora, non il minuto della richiesta (verificato sul
servizio: alle 10:53 locali la risposta diceva `10:45`). Il mapper legge quel blocco e
prende nuvole e pioggia dall'ora corrente dell'orario, che è la riga giusta.

**Il TTL della cache era `update_frequency_min`.** Quel valore è l'intervallo del job
periodico — una scelta di batteria, 15/30/60/120 con 60 di default — e usarlo come TTL
gli faceva decidere una cosa che non gli era stata chiesta: quanto possono essere
vecchi i numeri *mentre il lettore li sta guardando*. Con il default, atterrare su
Oggi entro un'ora dall'ultima sincronizzazione mostrava quella sincronizzazione, eroe
e frase compresi; con 120, due ore. E la pastiglia di freschezza taceva per
costruzione: `isStale` scatta a 2× l'intervallo, quindi un cache hit non è mai stale.

Il TTL ora è `WeatherFreshness.ProviderResolution`, quindici minuti, cioè la
risoluzione con cui il fornitore pubblica «adesso». Un valore più vecchio di così non
è vecchio: è un valore che Open-Meteo ha già sostituito, e rileggerlo costa una
richiesta che il lettore ha chiesto aprendo l'app. **Batteria è una feature** e resta
vera dov'è vera: il job periodico è dove quel costo si paga, una schermata appena
aperta no. I due intervalli tornano due numeri, e `update_frequency_min` resta
l'intervallo del worker e la base di `isStale`.

**Due strade, perché le due app arrivavano allo stesso schermo da posti diversi.**

- Qui lo stato si ricostruisce da solo quando la pagina torna in primo piano
  (`WhileSubscribed(5_000)` cancella il flusso cinque secondi dopo l'uscita e lo
  rifà al rientro), quindi il TTL è tutta la correzione per il caso «riapro l'app».
  Restava l'altro: una pagina lasciata aperta si fermava al fetch che l'aveva aperta,
  perché il tick al minuto ridisegnava — età dichiarata, verdetto di freschezza,
  taglio di recency — ma non rileggeva mai. Ora oltre i quindici minuti il tick
  rilegge, in silenzio come ogni fetch automatico (VISION §5.2: contenuto prima,
  freschezza dichiarata, aggiornamento silenzioso) e a costo zero quando la pagina
  non è a schermo, visto che quel flusso non esiste.
- In tweather non c'era **niente**: il documento si costruisce una volta, al
  caricamento, e invecchia sullo schermo. Là è nato `onResumed()`, che qui non serve
  perché il flusso lo fa già.

**Risparmio energetico** (aggiunto su richiesta del committente subito dopo): sotto
battery saver i fetch automatici — quello all'atterraggio e quello del tick — non
partono. Mai la pull, mai una pagina che non ha ancora niente da mostrare (lì
l'alternativa è uno scheletro, che non è una schermata più economica, è una vuota),
e mai il job periodico: quello lo differisce già il sistema con Doze e App Standby, e
zittirlo qui silenzierebbe un'allerta proprio sul telefono con meno carica. `push()`
continua invece a girare: età, freschezza e taglio di recency non costano né radio né
disco, e congelarli scambierebbe batteria con una pagina che mente sull'ora (§1.1).
`PowerSaveState` sta in `:core:data` accanto a `LocationProvider` — è stato di
piattaforma — ed è letto come funzione, non come valore, perché l'interruttore può
essere spostato mentre il processo è vivo.

**Non testato qui**: `:app` non ha ancora un banco di prova per i ViewModel e
costruirlo vuole sei `testImplementation` in più nel modulo (datastore, room,
retrofit e il convertitore, okhttp, serialization). La stessa guardia è coperta da due
test in tweather, dove il banco esiste già.

**Verifiche**: suite verde, lint 0 errori. `WeatherFreshnessTest` è nuovo e sta in
`:core:data` invece che accanto all'oggetto che prova, perché `UpdateFrequencies` sta
lì: il caso che conta è che l'invariante «un hit non può essere stale» regga per
**ogni** intervallo selezionabile, e una lista ricopiata a mano sarebbe esattamente la
cosa che va fuori sincrono.

- [ ] Da verificare su device (committente)

---

## Come si legge lo stato del provider (review, 6 set 2026) — e la pioggia deve esserci

Review chiesta sul repo gemello e applicata qui identica: `:core` è copiato da tweather
e `UPSTREAM.md` chiede che un difetto del core si corregga da tutte e due le parti. La
misura è la stessa — 23 città su cinque continenti, 3 864 ore, 161 giorni-città, dati
scaricati quel giorno — e il dettaglio completo sta nella Fase 26 di tweather.

**La riparazione della nebbia regge**: riscrive l'1,09% delle ore, e delle 25 servite
come `45`/`48` diciassette hanno una visibilità sopra i 1 000 m nella stessa risposta
(mediana 4 km, massimo 16,3). Il crudo era peggio.

**Il difetto trovato** è della stessa famiglia in un'altra colonna: qualsiasi ora con
codice ≥ 51 reclamava il giorno intero, e il 47% dei giorni «bagnati» lo era solo per
pioviggine — Singapore etichettava una giornata intera per un'ora di 0,1 mm all'1% di
probabilità. Ora la precipitazione deve essere materiale (≥ 1 mm sul giorno oppure
≥ 3 ore) e i codici di pericolo reclamano il giorno senza condizioni.

**La nebbia non dura un'ora**: viene scritta solo se anche l'ora accanto è sotto soglia
(le transizioni della settimana passano da 18 a 14), mentre toglierla resta una
decisione per ora.

**Le due fragilità minori** contano più qui che a monte, perché DESIGN §1.1 le vieta
esplicitamente: `visibilityKm` e `precipChancePct` sono ora nullable fino alle
superfici. Il riquadro della visibilità **non viene disegnato** quando il modello non
la porta, come già fa la qualità dell'aria; la cella dell'ora tiene i suoi 56dp e non
stampa niente invece di uno 0% mai previsto; e la **sparkline della pioggia si
interrompe** sull'ora ignota invece di disegnarla a zero — un grafico che inventa un
punto è la stessa bugia di una casella con un trattino.

### La riga di chiusura di Oggi

Chiesta dal committente. Fino a qui la pagina diceva *quando* solo quando aveva cattive
notizie: la pastiglia di freschezza compare oltre il doppio dell'intervallo e tace
altrimenti, quindi chi voleva solo sapere quanto fosse recente l'eroe non aveva dove
guardare.

`Aggiornato alle 18:45 · dati Open-Meteo`, in fondo alla pagina. **In fondo di
proposito**: un orario è riferimento, non titolo, e la testa di quella schermata è del
cielo, della temperatura e della frase del giorno (VISION §5.2, una cosa prima di
qualsiasi numero). Dà anche una fine alla pagina e mette l'attribuzione dove va un
colophon.

Le due cose non si sovrappongono e fanno lavori diversi: la pastiglia è un **avviso** e
porta un'età relativa («7 ore fa») più una via d'uscita, questa è una **constatazione**
e porta l'ora dell'orologio, che è quella che si confronta con il proprio. L'ora è
quella del LUOGO come ogni altra ora della schermata: sarebbe difendibile anche quella
del lettore (il fetch è successo sul suo orologio), ma un piè di pagina in un fuso
diverso dalla striscia che gli sta sopra è una riga da leggere due volte, e nel caso
prevalente — il posto in cui sei — le due coincidono.

**Verifiche**: suite verde, lint 0 errori. I test del mapper sono gli stessi di
tweather, allineati byte per byte.

- [ ] Da verificare su device (committente)

---

## Le icone del meteo (committente, 6 set 2026) — il tratto come default, e una scala sola

Due richieste dallo stesso screenshot di Oggi, e la seconda spiega la prima.

- [x] **Il default passa da FILL a LINE.** Non è un ripensamento sull'argomento del 3 set
      («le forme piene si leggono più in fretta a 24–32dp»): quell'argomento valeva
      *a quelle taglie*. Le taglie si sono mosse — punto sotto — e su una schermata il cui
      eroe è già un cielo dipinto il tratto tiene l'inchiostro su un peso solo, invece di
      appoggiare otto adesivi colorati sopra un dipinto. Il set line è anche l'unico dei
      tre che serve entrambi i fondi con la stessa misura (DESIGN §13.1), quindi il
      default non deve più scegliere per chi non ha ancora scelto un tema. La scelta resta
      dov'era, in Impostazioni → Aspetto, con le stesse due voci.
      Il default si sposta in quattro posti perché quattro sono i posti che lo dicono:
      `AppSettings`, la lettura dello `SettingsStore` (un preferenza assente deve leggere
      come il default, non come il vecchio default), il `LocalWeatherIcons` e il fallback
      di `MainActivity` mentre le impostazioni sono ancora nulle. Anche i due parametri
      `style` con valore di default in `ChiaroIcons` seguono: nessun chiamante li usa —
      i widget passano sempre lo stile — ma un default che contraddice il default
      dell'app è una trappola che aspetta il primo chiamante.
      **Conseguenza dichiarata**: un'installazione esistente che non ha mai aperto quella
      voce cambia aspetto con l'aggiornamento. È cosa vuol dire «default», ed è
      esattamente il gruppo di lettori per cui la modifica è stata chiesta.
- [x] **Le icone crescono di 6dp ovunque su Oggi**, e niente altro si muove: nessun
      padding, nessuno `spacedBy`, nessuna larghezza di colonna. Striscia oraria 32→38,
      riga della settimana 28→34, riga del resto della giornata 24→30, scheda dei
      dettagli 24→30. **In due passi**: 4dp, poi altri 2 chiesti dal committente sul
      confronto prima/dopo, «dove non modifica layout e spaziature attuali» — che è la
      condizione che i quattro pioli hanno dovuto superare uno per uno, non una formalità.
      Tutti e quattro la superano a 360dp, e il conto è sotto.
- [x] **I quattro numeri diventano una scala sola**, `ui/icons/WeatherIconSize`. Il
      commento di `DayRow` diceva già «tra i 32 della striscia e i 24 della timeline»:
      quando quattro punti del codice si citano a vicenda per stare in ordine, l'ordine è
      un oggetto, non un commento ripetuto quattro volte. Adesso la prossima passata è una
      riga per pioli, e l'ordine dei pioli è scritto dov'è: è l'ordine di lettura — la
      striscia si scorre di lato e porta il peso maggiore, la settimana si legge in
      colonna, una riga di prosa apre col glifo più piccolo dei tre.

### I conti che la crescita doveva pagare

Un'icona che cresce dentro un layout fermo non prende spazio dal nulla: lo prende dalle
tre misure elastiche che le stanno accanto, più la cella fissa che la contiene. Sono
quattro conti, rifatti a ogni passo — sotto ci sono i valori del secondo, con quelli del
primo fra parentesi.

- **La cella oraria non si allarga**: 38dp dentro i 56dp di cella lasciano 9dp d'aria per
  lato (erano 10), e i 4dp di passo fra le celle restano quelli. La striscia non cambia
  larghezza, quindi non cambia quante ore entrano nello schermo. È il piolo con più
  margine dei quattro, ed è per questo che è il più alto.
- **La griglia dei dettagli** è il conto che decide il tetto. Il budget dell'etichetta è
  quello che avanza accanto all'icona, `(360 − 32 − 12) / 2 − 32 − 38 = 88dp` su uno
  schermo da 360dp (90 al primo passo, 94 prima di tutto). Misurato di nuovo con la stessa
  ricetta del 4 set (Inter variabile a wght 500, 14sp, tracking di `labelLarge`, `opsz`
  14): la più larga delle sedici etichette che l'app spedisce è «Qualità aria» a
  **76,7dp**, poi «Air quality» 68,6 e «Dew point» 68,4. Restano **11,3dp** di margine:
  abbastanza per il secondo passo, ed è il numero da guardare prima di un terzo, perché
  lì si comincia a spendere il margine invece dell'aria.
  Il contratto è, ed è sempre stato, un contratto **a 360dp**: sotto quella larghezza le
  etichette vanno a capo tenendo le parole — la rottura onesta già scelta per loro — e lo
  facevano anche a 320dp con l'icona da 24 (74dp di budget contro gli stessi 76,7). Non è
  una regressione di questa passata, è il limite dichiarato che resta dov'era.
- **La riga della settimana** perde 6dp di barra della temperatura (la barra ha
  `weight(1f)` e paga lei l'allargamento): 112→106dp su 360 (108 al primo passo). La
  scala è condivisa fra i sette giorni, quindi la forma della settimana è la stessa, solo
  più stretta. Il nastro di luce sotto la riga parte ancora a 52dp — i 44 dell'etichetta
  del giorno più gli 8 accanto — e quel numero non ha mai dipeso dall'icona.
- **La riga del resto della giornata** perde gli stessi 6dp di prosa (232→226dp su 360):
  `bodyMedium` va a capo tenendo le parole, che è la rottura onesta già scelta per le
  etichette.
- **Fuori dalla scala di proposito**: i widget Glance, dove l'icona si misura sulla cella
  (Fase 8, e l'eroe del Now ha già il suo soffitto a 104dp), e le sagome della barra di
  navigazione, che sono Material a 24dp e non sono disegni meteo.

### Verifica

Suite verde (`test` + `:app:testDebugUnitTest`), lint a 0 errori, APK debug costruito, a
entrambi i passi. `SettingsStoreTest` segue il default in due punti: l'asserzione del
fresh install diventa LINE, e il round-trip scrive adesso FILL — un round-trip che scrive
il default non dimostra niente.

Nessun device qui: le taglie sono verificate per aritmetica (i quattro conti sopra) e su
un confronto prima/dopo disegnato con i drawable veri, non su una resa reale. È l'unica
parte di questa passata che resta da guardare su un telefono.

- [ ] Da verificare su device (committente)

---

## Fase 9 — Accessibilità e prestazioni, con i numeri

- [x] Passata colore (chiesta su device, 3 set; fatta il 3 set sera, alzata una
      seconda volta dopo la prova su device — «ancora un pochino» — la stessa sera):
      croma su in OKLCh — ×1.65 sul cielo diurno, a scalare fino a ×1.22 sulla
      notte; ×1.4–1.45 su verdetti, rampe e sorgenti dello schema — a **luminanza
      WCAG bloccata**,
      così ogni rapporto misurato di DESIGN.md è sopravvissuto per costruzione
      (scarto massimo documentato 0.04; tutti i numeri di §2.2–§2.3–§3.2–§3.6
      riaggiornati, `PaletteContrastTest`/`ScrimContractTest`/`SkyPaletteTest` verdi
      senza ritocchi). I neutri non si sono mossi: la carta calda è identità, il
      croma era la lamentela. Il cielo era il punto: il diurno passa da `#4E8FBF` a
      `#0090DA`, l'ora dorata guadagna oro vero (`#F49C04`/`#E58800` — quest'ultimo
      già al bordo del gamut alla prima alzata: più in là di così, a pari luminanza,
      l'sRGB non va). Due note oneste: il
      primary ambra al tono 40 era già al bordo del gamut sRGB e si muove appena
      (la vividezza dell'app in dynamic color OFF arriva da secondary/terziario,
      rampe e cielo); le ICONE meteo non sono state toccate — `FILL_REMAP` è una
      rimappatura di luminanza per contrasto, non di tinta, e una passata sulle
      icone è un lavoro a parte se dopo la prova su device servirà ancora
- [x] Passata icone meteo (3 set sera, subito dopo la passata colore): il set fill
      si sdoppia per terreno. Il vincolo «3:1 su entrambe le superfici» con un solo
      asset forza ogni luminanza in Y ∈ [0.120, 0.283] — aritmetica giusta, resa
      spenta; ora `mcf_*` (terreni chiari) tiene la banda con croma ×1.25 a luminanza
      ferma, e `mcfn_*` (terreni scuri) è la palette fill ORIGINALE di Meteocons,
      identica salvo 4 dettagli quasi-neri alzati per il 3:1 su scuro (33 colori su
      37 verbatim). `ChiaroIcons` sceglie dal terreno (l'app dal tema applicato, i
      widget dal proprio `darkGround`); `IconContrastTest` misura ogni set contro la
      SUA superficie. Anche il set line guadagna croma ×1.25 a luminanza ferma. I
      gradienti restano appiattiti al colore di faccia: a 24–32 dp una rampa a due
      stop è invisibile (motivo registrato nel tool). Nota: il marchio dell'app
      conserva i valori pre-passata (`#3589AC`/`#C27D08`) — è un drawable disegnato
      a mano, non rigenerato; da riallineare solo se si ridisegna il badge
- [x] Rampa d'inchiostro per la probabilità di pioggia (segnalata su device, 6 set):
      nella settimana lo 0% era la cifra più pesante della colonna. Cadeva su
      `onSurfaceVariant` (8.9:1) mentre il 15% accanto stampava a 1.29:1, perché una
      cifra stava usando la rampa dei SEGNI — quella pensata per sparkline, celle del
      diario e campioni, il cui estremo chiaro su carta è 1.12:1. Ora `rainInkRamp` e
      `rainInkAt`: cinque passi della stessa famiglia di tinta (la deriva verso il
      265° del fondo scala è quella della rampa di riempimento), scelti come
      inchiostro e misurati contro la superficie di §2.2 — 4.8→9.7:1 su chiaro,
      5.1→10.9:1 su scuro — **zero compreso**: una probabilità nulla è comunque una
      probabilità e sta all'estremo quieto della stessa scala, non su un altro
      colore. La rampa di riempimento non stampa più nessuna cifra e resta dei segni.
      Stessa regola nel widget, dove valeva lo stesso salto e in più i valori bassi
      sparivano sul cielo. `PaletteContrastTest` cammina da 0 a 100 e tiene ogni passo
      sopra il 4.5:1 di §10, più monotonia e il fatto che lo 0% non gridi più del
      100%; DESIGN §2.3, §8.3, §8.5, §10 e §12 riscritti con i numeri misurati.
      Le due luminanze sono libere di cambiare insieme: la cifra dello 0% ora è
      **più leggera** di prima, ed è il punto della segnalazione
- [x] La sparkline della pioggia diventa un grafico (chiesto su device, 6 set, subito
      dopo la rampa d'inchiostro): sotto le prossime ore c'era una sparkline nuda, e una
      sparkline nuda è una forma senza un posto dove stare — su una giornata inchiodata
      al 100% disegnava una riga quasi dritta in un riquadro vuoto, e più la giornata era
      piatta meno diceva. Ora `RainChart`, con le due domande che uno si fa davvero
      (*quanto in alto* e *quando*): tre linee di griglia recessive a 0 / 50 / 100% con i
      due estremi stampati nella colonna di destra, un punto per ogni ora, l'ora sotto
      l'asse ogni sei con la prima e l'ultima sempre nominate, e l'area sotto la linea
      velata con la rampa di riempimento (0.30 → 0.06) perché a colpo d'occhio una
      superficie porta un livello e un tratto porta una direzione. La linea passa alla
      rampa d'inchiostro: anche un segno ha il suo 3:1, e una giornata con picco 10%
      disegnava la sua linea in `#D0E8FA`, che non è una linea. Due regole tornano
      funzioni pure e testate (`RainChartTest`): dove la linea si interrompe (`rainRuns`
      — un'ora senza previsione non diventa uno zero) e quali ore l'asse nomina
      (`axisTicks` — l'ultima solo se la sua etichetta non finisce addosso alla
      precedente). Il punto orario è l'unica eccezione dichiarata al «marker ≥ 8dp» di
      §9.2, e sta scritta lì: è ritmo, non un valore da leggere — i 24 numeri sono nella
      striscia sopra — e sotto i 6dp di passo non viene disegnato. La giornata asciutta
      continua a non disegnare niente
- [x] «Cos'è cambiato» dopo «La settimana» (chiesto su device, 6 set — e d'accordo):
      ogni frase di quella sezione parla di un giorno più avanti («La previsione di
      Mercoledì 9 è cambiata»), quindi prima della settimana nominava giorni che il
      lettore non aveva ancora visto, e tagliava in due il blocco che parla di oggi.
      Nello stesso giro il difetto visto nello screenshot: una revisione di cui questa
      schermata non ha le parole (un codice di condizione) stampava «… è cambiata:» e
      basta — i due punti e il vuoto. Ora non diventa proprio una riga, e non spreca una
      delle tre che la sezione ha; nel Diario la riga resta, perché quello è il registro,
      ma perde il separatore appeso davanti all'ora. Le frasi si costruiscono adesso
      prima della lista, perché un `LazyListScope` non può chiamare una composable
- [x] Le ricerche recenti si cancellano, e il campo di ricerca si svuota in un colpo
      (segnalato su device, 7 set): sulle recenti non c'era **nessun** modo di togliere
      una riga — né un gesto né un comando — e il lettore l'ha letto come «non si
      cancellano», che era esatto. La strada breve era copiare lo scorrimento dei
      salvati, ma un gesto è invisibile esattamente quanto il niente che c'era prima:
      la segnalazione nasce dal non trovare, e una scorciatoia nascosta non si fa
      trovare. Quindi ogni recente prende una **crocetta** in coda alla riga e la
      sezione un **«Cancella»** accanto al titolo — due bersagli visibili, entrambi
      con l'annulla nella stessa snackbar che serve i luoghi (una cancellazione
      annullabile può permettersi di essere a un tocco solo).
      L'annulla rimette la lista **com'era, ordine compreso**:
      `SearchHistoryStore.restore(list)` scrive l'elenco che riceve invece di rigiocare
      `add(term)`, perché quella lista è ordinata per *quando* si è cercato e
      riaggiungere un termine lo dichiarerebbe il più recente — una data inventata dal
      pulsante «annulla». `remove(term)` toglie con lo stesso confronto con cui `add`
      deduplica (spazi e maiuscole: la riga che si vede è quella che se ne va, comunque
      fosse stata scritta). Sette prove nuove in `SearchHistoryStoreTest`.
      Nel campo «Cerca una città» compare una **X** in coda finché c'è qualcosa da
      cancellare, e non prima: una crocetta che non annulla niente è decorazione.
      Terza cosa, trovata guardando la stessa schermata: **i salvati non erano
      rimovibili con TalkBack**. Lo scorrimento era l'unica via, e uno scorrimento non
      ha né tastiera né screen reader; adesso la riga porta l'azione «Rimuovi» accanto
      a «Sposta su» e «Sposta giù», che stanno lì dalla Fase 3 esattamente per questo
      motivo. È materia di questa fase e costava tre righe.
      **La guida resta com'è**, di proposito: VISION §5.7 dice che non insegna un
      controllo, e una crocetta visibile non è un controllo da insegnare — lo
      scorrimento dei salvati è nella guida perché è invisibile, ed è l'eccezione che
      spiega la regola, non un precedente. Suite verde (`test` + `:app:testDebugUnitTest`),
      lint a 0 errori, APK debug costruito
- [x] Il segno della posizione arriva sui widget, e Oggi dice che ora è **là**
      (due richieste su device, 7 set — sono la stessa domanda vista dai due lati).
      Sul widget: il nome del luogo non diceva da dove veniva, quindi un «Cavenago»
      salvato e una posizione GPS ferma a Cavenago erano due cartoncini identici. Ora
      il segnaposto sta prima del nome quando quel luogo è la posizione del telefono —
      la stessa regola dell'intestazione di Oggi (§5.1, decisa il 2 set), portata sulla
      schermata home. Un widget **appuntato** non lo mostra mai e non chiede nemmeno la
      sorgente attiva: uno spillo è una città salvata per definizione, e resta quella
      mentre il lettore viaggia. Il modello guadagna `WidgetModel.fromGps`, e la riga
      del luogo diventa una sola `PlaceLine` condivisa da Now e Oggi (il widget Cielo
      non stampa il nome). Il segnaposto si misura sulla dimensione del nome **per il
      fattore di scala del testo del lettore**, come `textInkBalance`: un glifo fermo
      accanto a parole che crescono smette di far parte della stessa riga.
      Il disegno è `place` di Material Icons nel tema outlined, copiato verbatim in
      `res/drawable/ic_place_pin.xml` — Glance non sa disegnare un `ImageVector` di
      Compose, vuole un id di risorsa, quindi lo stesso glifo esiste due volte e il
      commento del file lo dice; riga aggiunta in `licenses/README.md` con il testo
      Apache-2.0 accanto agli altri due.
      Su Oggi: sotto il nome di un luogo che **non** è la posizione compaiono il giorno
      e l'ora **di lì**. È il complemento esatto del segnaposto — appare sulle pagine
      dove quello non c'è — e la ragione è la stessa: sulla pagina della posizione
      sarebbe l'orologio del telefono ristampato sotto la barra di stato che lo mostra
      già, mentre su Palermo o Reykjavík è l'unica cosa che il lettore non può guardare
      altrove. L'ora è quella del posto, come ogni altra ora della schermata, e ticchetta
      col minuto che `push()` fa girare anche in risparmio energetico. Non si confonde
      col piede della pagina: quello dice *quando sono arrivati i numeri* e lo dice con
      le sue parole («Aggiornato alle …»), nello stesso fuso.
      Una conseguenza accettata: sulla pagina GPS la riga non c'è, quindi i puntini del
      pager stanno 18dp più in alto che sulle altre. Riservare lo spazio vuoto sarebbe
      peggio (una banda vuota sotto «La mia posizione» sembra qualcosa che non ha
      caricato), e le pagine differiscono già per cielo, numeri e frase. Da guardare
      su device.
      `Formats.dayLong` è la quinta copia di «formatta e maiuscola l'iniziale» che non
      è stata scritta. Guida aggiornata (`guide_today_places_body`): dire che
      l'intestazione porta giorno e ora di lì è dire cosa fa una schermata, non
      insegnare un controllo. Suite verde, lint a 0 errori, APK debug costruito
- [x] **Verifica della passata colore** (chiesta col resto della fase, 7 set): la palette
      dell'app **è** quella finale. Non è una lettura a occhio: `PaletteDocTest` adesso
      legge DESIGN.md e misura contro il codice — i due ruoli nominati in §2.2, la tabella
      dei verdetti e le tre rampe di §2.3, gli otto banchi di §3.2, il bersaglio della luna
      di §3.4, lo scrim di §3.6 e i suoi tre rapporti — esadecimali **e** numeri stampati.
      `tools/gen_scheme.py` rigenerato produce `Scheme.kt` byte per byte, quindi anche le
      sorgenti dello schema sono quelle post-passata. Il foglio della palette è stato
      renderizzato e guardato, che è l'altra metà del lavoro: cielo diurno saturo, ora
      d'oro d'oro vero, rampe con croma.
      Due scostamenti trovati, entrambi documentali e entrambi corretti:
      **(1)** §3.4 stampava ancora `#2A3550` come bersaglio della luce lunare mentre il
      canvas mescola `#273458` dalla passata del 3 set — sei giorni di documento che
      diceva il colore vecchio, e nessun test che guardasse. È il motivo per cui
      `PaletteDocTest` esiste adesso.
      **(2)** i tre rapporti dello scrim erano tre aritmetiche diverse: `5.29` viene da un
      composito sRGB arrotondato a 8 bit con pareggio verso il basso, `3.95` dallo stesso
      con pareggio verso l'alto, e `4.58` **non si raggiunge in nessun modo** — il valore
      vero a 0.50 è 4.53. `ScrimContractTest` ne aveva una quarta, `Color.lerp`, che
      interpola in Oklab: una mescolanza percettiva, non un composito, e quindi il modello
      sbagliato per misurare una regola di compositing. Adesso una sola funzione, la stessa
      nei due test e in §3.6: `scrim × α + cielo × (1 − α)` sui valori sRGB, come fa il
      brush. La conclusione non si è mossa e anzi si è irrigidita: 0.50 supera la soglia di
      0.03, che non è margine, è fortuna.
      Terza cosa, minore: `tools/palette_sheet.py` non disegnava `rainInkRamp` (la sua
      regex conosceva due rampe su tre, ed è nata prima della terza). Adesso la disegna
      **come inchiostro** — cinque cifre di percentuale sulla superficie contro cui sono
      misurate — perché un campione di quella rampa mostrerebbe l'unica proprietà che non
      serve. Il marchio conserva i suoi valori pre-passata di proposito (§13.3), come già
      registrato
- [x] Contrasti, scala testo 200%, TalkBack, motion ridotto (7 set)
      **Motion ridotto** era la voce con più distanza fra il documento e l'APK: §7 diceva
      «reduced motion collassa tutto a una dissolvenza di 100 ms» dalla Fase 1, la costante
      `reducedMotionFadeMillis` esisteva dalla Fase 1, e **niente la leggeva** — nessuna
      animazione dell'app aveva mai chiesto nulla al lettore. Adesso `ChiaroTheme` legge
      `Settings.Global.ANIMATOR_DURATION_SCALE` (che è l'API: Android non ha un
      `prefers-reduced-motion`, e sia «Rimuovi animazioni» in Accessibilità sia la scala
      degli animator in Opzioni sviluppatore scrivono lì) e pubblica `LocalReducedMotion`.
      Con un `ContentObserver`, perché l'interruttore sta **fuori** dall'app: un valore
      letto una volta all'avvio sarebbe giusto fino al primo lettore che lo accende ad app
      aperta, cioè esattamente il lettore per cui esiste. I tre punti in cui l'app si
      muove chiedono tutti e tre: la striscia oraria della settimana si apre con
      `ChiaroMotion.enter/exit`, il pager fa `scrollToPage` invece di animare, e la
      risposta della prova a vuoto nell'editor di regole compare invece di scorrere fin
      lì. Il canvas non ha avuto bisogno di niente: è un `Brush`, non ha mai animato, e il
      «diventa un gradiente statico» di §3.5 lo mantiene per costruzione.
      **Scala testo 200%**: il rischio non era il taglio — in `ui/` non c'è **un solo**
      `maxLines`, ed è voluto — ma il fatto che una colonna misurata in dp che contiene
      testo misurato in sp si sfascia da sola. Al 200% le quattro colonne della riga
      settimanale, la cella oraria, l'orologio della timeline e la data della striscia del
      diario contenevano tutte testo largo il doppio di loro: niente veniva tagliato,
      andava a capo **in mezzo a un valore**, su una riga che non aveva l'altezza per
      ospitarlo. Due regole in `ui/theme/TextScale.kt`: una colonna che contiene testo si
      misura **in testo** (`Dp.forText()`, con tetto a 2.0 dove si ferma il cursore di
      sistema), e sopra **1.5** una riga di colonne diventa **due righe** — la settimana si
      spezza in «che giorno, che giornata» e «quanto caldo», la griglia dei dettagli passa
      a una colonna. 1.5 è misurato e non tondo: sopra quella soglia alla barra delle
      temperature restano meno di 48dp, che è una sbavatura e non una barra. Anteprime a
      `fontScale = 2f` accanto a quelle normali per i due posti che §10 nomina, più la
      striscia oraria.
      **TalkBack**: l'audit non ha trovato buchi da riempire — tutti e 35 i
      `contentDescription = null` sono righe che parlano una volta sola o glifi con la
      parola accanto, e ogni `IconButton` ha la sua descrizione.
      Sui **bersagli da 48dp** ha trovato due cose più piccole di così — la riga della
      settimana ne disegna 42 (icona 34, stacco 4, nastro 4) e il chip di freschezza 32 —
      ed è andato a correggerle scoprendo che non c'era niente da correggere: Compose
      allarga i limiti di un nodo di pointer input fino al bersaglio minimo della
      piattaforma, quindi un `clickable` è già 48dp per un dito, qualunque cosa disegni.
      Vale la pena scriverlo perché quell'allargamento ha un buco: non riserva **spazio**,
      quindi due bersagli piccoli più vicini dei loro limiti allargati si contendono i
      tocchi in mezzo. Nessuno dei due è quel caso — le righe della settimana distano 12dp
      e ne contengono uno ciascuna, il chip è solo nella sua riga.
      `minimumInteractiveComponentSize()` è stato scritto e poi **tolto**: sulla settimana
      costava 6dp per riga, cioè una pagina più alta di 42dp, per comprare al lettore
      esattamente niente. Registrato in §10 con il motivo, perché la prossima lettura di
      quei 42dp farà la stessa domanda e merita la risposta già misurata.
      **Contrasti**: nessun ritocco necessario — è la parte già coperta da
      `PaletteContrastTest`, `ScrimContractTest` e `IconContrastTest` — ma la correzione
      del modello di compositing dello scrim (sopra) è caduta qui, ed è l'unico numero di
      accessibilità che si è mosso in tutta la fase.
      Due prove nuove, pure e senza device: `MotionTest` (le tre molle sono le tre molle, e
      tutte diventano la stessa dissolvenza da 100 ms) e `TextScaleTest` (le due regole,
      più l'aritmetica che giustifica l'1.5, accanto al numero così che spostare l'uno
      voglia dire spostare l'altra)
- [x] Avvio a freddo sotto 400 ms, canvas sotto 2 ms/frame — **misurato dal committente**
      (7 set): «ben sotto i 400 ms», e il canvas sotto i 2 ms/frame. Coerente con come è
      fatto: il canvas è un `Brush.verticalGradient` con sopra un secondo brush di scrim,
      due gradienti e nessuno shader, nessuna particella, nessun ricalcolo per frame — il
      budget di §3.5 non è mai stato in discussione perché non c'è niente da spendere. E
      all'avvio non c'è spinner da attraversare (§1.1): la prima cosa composta è il
      contenuto in cache
- [x] Passata IT/EN completa (7 set): la parità c'era già ed è verde — 696 stringhe per
      lingua, zero mancanti da una parte o dall'altra, argomenti di formato uguali, e le
      sei stringhe identiche nelle due lingue sono identiche per davvero («UV», «Celsius
      (°C)», «Privacy», «no»). Quindi la passata è servita a guardare **fuori** da
      `strings.xml`, che è dove si nasconde quello che una parità non vede:
      **(1)** cinque percentuali costruite a mano (`"$pct%"`) nella settimana, nella
      striscia oraria, nel dettaglio umidità, nella tabella del diario e nel widget. In
      italiano e in inglese quel template è giusto, ed è precisamente il motivo per cui era
      sopravvissuto a cinque letture: la regola di §11 non è «l'output deve cambiare», è
      che un numero stampato appartiene alla lingua, cifre comprese. `Formats.percent`
      adesso, e `DayRow`/`HourCell` ricevono la cifra **già scritta** accanto alla
      quantità che serve alla rampa d'inchiostro — due campi per una cosa sola, sì, ma sono
      due cose: una si stampa e una si colora.
      **(2)** il valore della qualità dell'aria montava l'acronimo dentro il Kotlin: una
      parola inglese saldata nell'unico posto dove una lingua non arriva. Adesso il valore
      intero è `metric_air_value`.
      **(3)** `Formats` non aveva **nessuna** prova, dalla Fase 2 — ed è il file che porta
      tutta la regola di §11. `FormatsTest` la mette alla prova nelle due lingue in
      parallelo, che è il modo in cui un formattatore fallisce davvero: essendo giusto
      nella lingua in cui è stato scritto («9,4 km» / «9.4 km», «Lunedì 7 settembre» /
      «Monday 7 September»).
      **(4)** `StringsParityTest` guadagna tre controlli che un'aggiunta futura romperebbe
      per prima: le quantità dei plurali devono coincidere fra le due lingue
      (`getQuantityString` non si lamenta, restituisce la frase sbagliata), nessuna stringa
      vuota, e nessun `%` non raddoppiato dentro una stringa che formatta — che non è un
      difetto di stile ma un crash nel momento in cui la frase serve. Nessuna violazione
      esistente: le prove nascono verdi, ed è il punto.
      Una decisione registrata e non un'omissione: `Formats.dayLong` continua a scrivere
      `EEEE d MMMM` invece di chiedere uno schema localizzato. java.time non sa costruire
      «giorno della settimana, giorno, mese, senza anno» per locale (è il
      `DateTimePatternGenerator` di ICU, raggiungibile su Android solo via
      `getBestDateTimePattern`, che costerebbe al file la purezza e le sue prove unitarie).
      L'ordine è giusto per tutte e due le lingue spedite. Si riapre con la terza

## Il sole del widget e quello dell'app (committente, 7 set 2026)

Segnalazione: «il colore del sole nelle icone meteo nell'app è un po' scuro rispetto al
colore del sole nell'icona meteo del widget. È così by design?». Sì, ed è obbligato: i set
sono tre, scelti dal terreno (§13.1). Il sole a tratto `mc_` sta a `#C37D00` (Y 0.263), il
pieno per terreni chiari `mcf_` a `#B28500` (Y 0.262), il pieno per terreni scuri `mcfn_`
a `#FBBF24` (Y 0.579) — la palette originale di Meteocons, che disegna per fondi scuri.
Due volte e mezzo la luminanza, ed è esattamente lo scarto visto. Il verso opposto lo
spiega da solo: `#FBBF24` sulla carta chiara dell'app misura **1.59:1**, cioè illeggibile.
Nota per chi rilegge: con lo stile **Tratto** (il default) app e widget disegnano lo
stesso `#C37D00`, perché `styledRes` ignora il terreno per il set a tratto — chi vede la
differenza è sullo stile Pieno.

### Il buco vero, che la domanda ha scoperto

Misurando la risposta è saltato fuori altro. La card **Cielo** del widget non è un fondo
scuro: è il cielo scrimmato, e al suo punto più chiaro vale `#5C6E7B`, **Y 0.149**, un
mezzotono. Lì un inchiostro tiene il 3:1 solo a Y ≥ 0.546 **oppure** Y ≤ 0.016, e in mezzo
non c'è niente. Nessuno dei due set ci sta: **8 colori su 8** del set a tratto cadono
(1.03–1.57:1) e **25 su 37** del pieno-notte (1.09–2.93:1). Il sole è uno dei dodici che
passano, ed è il motivo per cui la card sembra a posto e per cui la cosa non si era mai
vista. Il sole a tratto scende sotto la soglia **da −7° di altezza solare in su**: tutte le
ore di luce.
`IconContrastTest` non lo vedeva perché misura ogni set contro le due **superfici
dell'app**; il cielo scrimmato è un terzo terreno contro cui le icone non erano mai state
misurate.

### Le tre uscite, e perché nessuna è stata presa

1. **Sdoppiare il set a tratto per terreno**, come era stato fatto col pieno — la prima
   scelta, e cade alla misurazione. La superficie scura dell'app (Y 0.008) è già servita a
   5.52:1, quindi un set del genere esisterebbe solo per il cielo e dovrebbe portare
   **ogni** colore sopra Y 0.546: una famiglia quasi bianca in cui nuvola, pioggia, sole e
   termometro smettono di essere cose diverse, per ~98 drawable nuovi. È l'uscita 3 con
   più file.
2. **Alzare lo scrim**: servirebbe alpha ≈ 0.93 per portare il fondo a Y 0.013. È una card
   nera col ricordo di un cielo dietro.
3. **Tingere il glifo nell'inchiostro della card** (bianco sopra lo scrim): completa,
   gratuita e già disponibile — il bianco lì misura 5.29:1, che è il numero di §3.6, e
   `ColorFilter.tint` è come si disegna già il segnaposto. **Respinta dal committente per
   ragioni di prodotto**: «non voglio icona monocromatica, si perderebbe molto
   dell'aspetto grafico e della bellezza per l'utente». È una decisione sul prodotto, non
   sull'aritmetica, e il prodotto è la sua.

### Cosa resta, dichiarato

Un'eccezione accettata e **limitata** al 3:1 di §10, scritta in DESIGN §13.1 con i numeri
e con le tre uscite pesate, e richiamata da §10 e dal commento in testa a
`IconContrastTest` — perché un test che non copre un terreno deve dirlo, invece di
lasciar credere una copertura che non ha. I confini: vale per il solo sfondo **Cielo**; le
card Chiaro, Scuro e Sistema sono le due superfici dell'app, già misurate, e l'app non
mette mai un'icona meteo sul canvas. E su quella card l'icona non è l'unico portatore
della condizione — accanto c'è la parola — che è la situazione che il 3:1 di §10 protegge
davvero nella striscia oraria. Se si riapre, l'uscita che tiene tutte e due le cose è una
placca scurita sotto il glifo: contrasto senza rinunciare al colore.

---

## La seconda palette, «Brillante» (committente, 7 set 2026)

Richiesta: «la palette di oggi è un po' stile colori su carta. È possibile aggiungerne
una nuova, scelta dall'utente nelle impostazioni, con colori più brillanti, in
particolare come quelli del widget» — con lo screenshot di un widget meteo altrui, blu
saturo, testo bianco, sole giallo acceso. Poi, a lavoro iniziato: «anche i colori delle
icone li voglio vivaci, per esempio un bel sole giallo vivace».

### Che cosa è arrivato

Due vestiti, non due app. `AppSettings.palette` (`AppPalette.PAPER` / `VIVID`, PAPER di
default) sceglie **insieme** lo schema Material, i token semantici di §2.3, la tabella
delle bande del cielo e — sui terreni scuri — il set delle icone. Uno solo, perché
schema, token e cielo sono stati misurati insieme: mescolare il cielo vivace con le rampe
di carta sarebbe una terza palette che nessuno ha misurato. `ui/theme/Palettes.kt` è il
posto dove i quattro pezzi stanno insieme, `ChiaroTheme(palette = …)` è dove entra, e
`LocalAppPalette` è quello che porta la scelta fin dove serve un *file* invece di un
valore (le icone).

Vale anche coi colori dallo sfondo accesi, ed è voluto: la palette semantica e il canvas
non hanno mai seguito il wallpaper (§2.3, §3), quindi la scelta del lettore decide
comunque verdetti, rampe e cielo. Le due domande sono due, e la riga di Impostazioni lo
dice.

### La regola, e perché una regola invece di una seconda scelta a mano

Dopo il color pass del 3 set la maggior parte dei token di carta **sta già sul bordo del
gamut sRGB** alla propria luminanza: alzare la saturazione li restituisce identici. Quello
che resta da spendere è la luminanza, e spenderla significa rimisurare tutto. Quindi una
regola sola (`tools/gen_vivid.py`):

> stesso tono, **luminanza WCAG tenuta ferma**, croma fino al bordo del gamut o a **×1.8**
> di quella di carta, quello che arriva prima.

Il contrasto dipende solo dalla luminanza: tenerla ferma porta di là *ogni* rapporto
stampato in DESIGN, ogni monotonia, ogni ordinamento di luminosità del cielo. Il tetto
×1.8 è quello che impedisce alla regola di diventare una caricatura: un token
deliberatamente quasi-neutro — il centro della rampa divergente, `unknown`, un cielo
notturno — sarebbe altrimenti trascinato sul bordo del gamut e smetterebbe di essere
neutro, che è l'unica cosa per cui esiste. `PaletteContrastTest` misura anche questo, non
si fida della costruzione.

Gli schemi Material **non** sono derivati così: sono generati da sorgenti proprie con lo
stesso `tools/gen_scheme.py`, perché uno schema tonale non è un insieme di token da
spostare. Le tre tinte restano i tre momenti del giorno di §2.2; cambia quale comanda —
**il cielo diurno promosso a primary** (`#2C7BF2`), l'ambra una casella sotto. A tono 40
un'ambra è un bronzo comunque, e chi chiede brillantezza non sta chiedendo un bronzo. I
neutri passano a croma 6 e 14 (da 3 e 7): è quello che trasforma un bianco caldo in uno
freddo, `#FCF9F3` → `#F6FAFF`, `#16130E` → `#0D141B`.

### Il risultato onesto: tre inchiostri non si muovono

I tre inchiostri chiari dei verdetti escono **identici** a quelli di carta. Non è una resa
del generatore: a 4,5:1 sulla carta chiara la luminanza è vincolata a `Y ≤ 0.172`, e lì
sotto sRGB non ha altro da dare. Dove la palette vivace si vede davvero è dove la
luminanza non era il vincolo — riempimenti, rampe, cielo, e sui fondi scuri le icone. È
scritto in DESIGN §2.5 così com'è, invece di lasciar credere una differenza che non c'è.

### Le icone

Il set a tratto `mc_*` serve entrambe le superfici, e per farlo è chiuso in
`Y ∈ [0.120, 0.283]`: è per questo che il sole dell'app è un bronzo, ed è la stessa
misura della segnalazione del 7 set qui sopra. Su un fondo **scuro** quel tetto non
esiste. `tools/gen_vivid_icons.py` genera `mcn_*` da `mc_*` con la regola di §2.5 e senza
il tetto: sole `#C37D00` → **`#FFA500`**, pioggia `#0085D0` → `#00A4FF`, luna e neve
`#008AB6` → `#02C3FF`; i grigi delle nuvole restano grigi, perché ×1.8 di un croma piccolo
è un croma piccolo. Lo legge solo la palette vivace, e solo su terreno scuro: carta tiene
una famiglia sola su entrambi, che è parte di cosa vuol dire «carta».

Su terreno **chiaro** la palette vivace riceve gli stessi file di carta, e questo è il
gamut che parla, non una dimenticanza: a `Y ≤ 0.284` un giallo acceso non esiste. Averlo
lì significa scambiare il 3:1 di §10, che è una decisione e non un colore — resta al
committente, dichiarata invece che presa di nascosto.

Effetto collaterale misurato, sulla card **Cielo** del widget (il buco dichiarato in
§13.1): il sole passa da **1,57:1 a 2,68:1** e il set da 1,03–1,57 a 1,03–2,68. Sempre 8
su 8 sotto il pavimento, quindi l'eccezione resta esattamente com'è per tutti e due i
vestiti — ma l'icona che il lettore guarda davvero su quella card è quasi arrivata.

### Guardarla, che nessun test fa

`tools/palette_sheet.py` disegna adesso **tutti e due** i vestiti, uno sotto l'altro: una
seconda palette che non puoi vedere accanto alla prima è una seconda palette che nessuno
ha confrontato. Cosa mostra e i numeri non dicono: fra 0° e −6° il canvas vivace vira al
**marrone** più di quello di carta, perché la miscela è percettiva e il punto medio Oklab
fra un'ambra satura e un viola saturo è un marrone saturo. Lasciato: dura sei gradi di
altezza solare e legge come un vero crepuscolo. Se un giorno smette di leggersi così, la
via d'uscita è quella che §3.2 ha già usato per l'ora d'oro — un'ancora in più, non una
tabella più spenta.

### Verifica

- `PaletteContrastTest` gira su **entrambi** i vestiti (verdetti, contenitori, ruoli
  on-color, rampe, probabilità da 0 a 100) e aggiunge le due affermazioni che rendono
  economico un secondo vestito: i token vivaci tengono le luminanze di carta, e il centro
  della rampa divergente resta un neutro. Più: ogni `AppPalette` ha un vestito.
- `PaletteDocTest` legge DESIGN **per sezione**. Serviva: il documento stampa ora due
  tabelle di verdetti, due terne di rampe e due tabelle di bande della stessa forma, e uno
  sweep dell'intero file avrebbe misurato allegramente la rampa vivace contro il Kotlin di
  carta — e sarebbe passato.
- `SkyPaletteTest` gira su entrambe le tabelle, più il §3.7: stesse altitudini, stesse
  luminanze alle ancore (deriva < 0,003), e fra le ancore al massimo **0,0112** — misurato,
  non assunto, perché la miscela è percettiva.
- `ScrimContractTest` spazza entrambe le tabelle a ogni mezzo grado. È l'unico contratto
  che la luminanza tenuta ferma **non** porta di là da sola: lo scrim compone per canale.
  Bianco sul cielo vivace più luminoso: **5,26:1** (carta 5,27:1).
- `IconContrastTest` misura quattro set contro **quattro** superfici: un set misurato
  sulla carta di un vestito e spedito sopra quella dell'altro è precisamente la cosa che
  passa la revisione e cade sul device.
- `SettingsStoreTest`: default PAPER, round-trip, e un nome sconosciuto (`"NEON"`) che
  torna al default invece di lanciare.

---

## Le icone si muovono, e i nuovi default (committente, 7 set 2026)

Richiesta, dopo la risposta sulle icone: «sul sito meteocons ci sono le icone animate:
usarle per l'app (no per il widget) sarebbe possibile? Sarebbe un bel salto per la
grafica». Sì. Ed è arrivato.

### La correzione che va detta

Nella risposta preliminare avevo consigliato «animare solo l'eroe di Oggi». Aprendo lo
schermo: **un'icona eroe non esiste**. L'eroe di Oggi è il numero della temperatura sul
canvas, e la condizione lì è una parola, non un glifo. Le icone della condizione stanno
solo in due posti — la striscia oraria e le righe della settimana — e sono esattamente le
stesse due. Quindi la regola è diventata quella onesta: **si muove la famiglia della
condizione, dovunque sia disegnata; non si muove nient'altro.**

Un tile dei dettagli porta un segno che etichetta una grandezza: un barometro che gira
per sempre è decoro, e §1.4 è dove va il decoro. `mc_not_available` non si muove perché
Meteocons non lo anima: non esiste un'animazione per «non lo sappiamo», ed è la quantità
giusta di movimento per quel caso. `ChiaroIcons.movingRes` restituisce **null** per
quelli, invece di un fermo travestito da animato.

### Come, e perché così

Le SMIL sono nei sorgenti che l'app già usa: `import_meteocons.py` le buttava via
(departure #2). Ora le porta di là come `AnimatedVectorDrawable` — **departure #7**. Non
una riscrittura del movimento: quello è dell'illustratore, e riscriverlo sarebbe stato un
secondo parere sul disegno di qualcun altro.

Quattro forme SMIL nella famiglia, tutte lineari e tutte infinite: `rotate` e `translate`
diventano un `<group>` col suo pivot che anima `rotation` o `translateX/Y`, `opacity`
diventa `fillAlpha`/`strokeAlpha` sui path del gruppo, e `gradientTransform` cade insieme
al gradiente che l'importer aveva già appiattito. Due cose che SMIL ha e AVD no:

- **`additive="sum"`** impila due trasformazioni su un elemento; AVD ne dà una a gruppo,
  quindi due trasformazioni diventano due gruppi annidati, il più esterno per primo — che
  è l'ordine in cui SMIL le moltiplica.
- **Un `begin` negativo** è una *fase*, non un ritardo: è quello che fa cadere tre gocce
  sfasate invece che in fila per uno. `startOffset` di AVD è l'opposto, quindi la fase
  finisce nei keyframe. `check_phase` ricampiona la curva emessa contro quella SMIL e si
  ferma se non combaciano — il budget è l'unico punto di aritmetica del tool che un
  lettore non può controllare guardando l'output.

Otto set adesso: quattro fermi (49 disegni) e quattro animati (18). `mca_*`, `mcaf_*`,
`mcafn_*` dall'importer; `mcan_*` da `gen_vivid_icons.py`, che ricolora `mca_*` con la
stessa tabella di `mc_*` — sono gli stessi disegni nella stessa palette, e l'animazione
non tocca un colore.

Il render è un `AndroidView` con una `ImageView`, e non un painter Compose, perché i loop
qui sono infiniti e `AnimatedImageVector` è fatto per l'altra cosa: una transizione da uno
stato all'altro, giocata una volta quando un booleano cambia. Il conto della batteria lo
paga la piattaforma, non una promessa scritta qui: un AVD si ferma quando il suo host
smette di essere visibile (`ImageView.onVisibilityAggregated` → `setVisible(false)`),
quindi una cella che esce dallo schermo o l'app in background fermano l'animazione senza
un observer nostro.

### Verifica

- **L'importer riproduce**. Prima di toccare qualsiasi cosa: rilanciato su un checkout
  v2.0.0, i 147 drawable fermi sono usciti **identici byte per byte** a quelli in repo.
  Senza quella prova ogni modifica al tool sarebbe stata alla cieca.
- `AnimatedIconTest`: un'icona animata **è** l'icona ferma (stessi path nello stesso
  ordine, stessi colori nello stesso ordine — la cosa più forte che un test JVM può dire
  su un disegno che non può rendere); ogni `<target>` punta a un elemento che il vettore
  ha davvero; ogni proprietà animata appartiene al tipo di elemento a cui è puntata; ogni
  loop è infinito, positivo e lineare, con keyframe che vanno avanti. Tutte e quattro
  falliscono in silenzio su un device: un'icona che semplicemente non si muove, scoperta
  da una persona. **Ha trovato il suo primo difetto il giorno in cui è stato scritto**:
  `fmt` arrotondava a due decimali e i due keyframe di un salto istantaneo finivano sullo
  stesso istante — un salto che non avviene.
- `IconContrastTest` spazza otto set contro quattro superfici.
- **E poi guardato**: `tools/icon_filmstrip.py` valuta ogni animator a una serie di
  istanti e scrive i fotogrammi in SVG. Un test può dire che la pioggia ha un animator
  valido; solo una persona può dire che la pioggia **scende**. Controllati tutti e
  quattro i set: gocce che scendono e sfumano, sole che gira, luna che dondola, banchi di
  nebbia che scorrono, fulmine che lampeggia.

### I nuovi default (committente)

| Voce | Prima | Ora |
|---|---|---|
| Tema | Come il telefono | **Scuro** (poi rimesso a «Come il telefono», stessa giornata: vedi sotto) |
| Palette | Carta | **Brillante** |
| Icone del meteo | A tratto | A tratto (invariata) |
| Icone animate | — | **Attivo** |
| Colori dallo sfondo | Attivo | **Spento** |
| Widget · opacità | 85% | **100%** |
| Widget · massima e minima | Attivo | **Spento** |

Due note che valgono più dei valori.

**Perché la palette sembrava non cambiare nulla.** Il device report diceva «il cambiamento
è minimo tra i due temi», e aveva ragione: con i colori dallo sfondo **accesi** lo schema
Material arriva dal wallpaper, e della palette si vedevano solo rampe e cielo. Due terzi
erano spenti. Spegnendo dynamic color — che è l'altro default chiesto qui — la differenza
diventa piena. I due valori sono stati scelti insieme e nessuno dei due dice granché senza
l'altro. Questo chiude anche il secondo punto aperto di DESIGN §13, e lo chiude dal lato
opposto a quello che ipotizzava.

**Il tema scuro contro VISION §4.** Il documento diceva «chiaro è il default, il monopolio
scuro della serie t era una posizione stilistica e qui sarebbe un problema di
accessibilità». La riga è stata riscritta, non ignorata: quello che ha spostato la
decisione è che l'eroe di quest'app è un cielo notturno dipinto per metà giornata e che la
palette Brillante è stata scelta sul suo schema scuro. La preoccupazione che la vecchia
riga proteggeva resta vera e resta scritta — un'app che ignora la modalità chiara del
telefono sembra rotta a qualcuno — quindi «Come il telefono» è a un tocco e lo scuro è un
default, non un monopolio.

I due default del widget cambiano anche i widget **già posati** che non hanno mai avuto
quella voce modificata: è quello che significa un default, ed è il motivo per cui lo si
sposta.

---

## Bordi, marchio, tema e parole (committente, 7 set 2026)

Quattro richieste in una passata, arrivate con lo screenshot del widget «Ora» sulla home.

### Il widget «Ora» respira dai due lati

Segnalazione: l'icona un po' troppo vicina al bordo sinistro, la massima e la minima
**decisamente** vicine al destro. Erano due errori diversi con una causa sola: la quarta
passata su device aveva messo `contentPaddingStart = 0.dp` sul presupposto che «un glifo
Meteocons porta circa un quarto della sua scatola come margine», e aveva lasciato il
bordo destro a `WidgetCardPaddingSnug`, i 6 dp verticali pensati per far crescere l'eroe.

Il presupposto è stato **misurato invece che stimato**: rasterizzando tutti e 160 i
disegni delle condizioni (le quattro famiglie, `mc_ mcf_ mcn_ mcfn_`), il margine
d'inchiostro a sinistra è 6,5/64 nel caso più stretto (`partly_cloudy_day`) e 9/64 alla
mediana — non 16/64. Sulla scatola da ~89 dp che la concessione a una cella lascia sono
**9-12,5 dp**, mentre il testo, che di margine laterale non ne ha, si fermava a 6.

Ora la card prende tre numeri invece di uno:

| Bordo | Prima | Ora | Perché |
|---|---|---|---|
| Sopra e sotto | 6 dp | 6 dp | l'eroe possiede l'altezza, invariato |
| Sinistra | 0 dp | **8 dp** | 8 + 9…12,5 = 17-20,5 dp d'inchiostro dal bordo, contro i 15-18,5 che il glifo ha già sopra e sotto: i due dp in più sono quello che si riprende l'angolo da 24 dp, che in verticale non c'è |
| Destra | 6 dp | **12 dp** | `WidgetCardPaddingTight`, cioè l'inset che l'altro widget da una cella già usa |

Il conto che la modifica doveva pagare è la colonna delle parole, che perde 14 dp:
«23°» più «27° / 16°» misurano circa 117 dp (metrica Inter) contro i 123 dp di colonna su
una concessione da 240 dp. Era già troppo stretto a 216 dp **prima** di questa passata e
lo resta — la coppia massima/minima è spenta di default e la sua riga di configurazione
dice che serve spazio.

**Anche gli stati vuoti** (seconda segnalazione, stessa passata). «Sto prendendo la prima
previsione…» e «Nessun luogo» stavano incastrati nell'angolo in alto a sinistra di una
card per il resto vuota: senza forma da seguire, due righe in alto sembrano contenuto che
non ha finito di caricare, non un messaggio. Ora sono **centrati verticalmente** sulla
card, su tutti e tre i widget. E sul widget «Ora» prendono l'inset del TESTO anche a
sinistra: gli 8 dp esistono perché la riga è aperta da un glifo che si porta il margine
da sé, e uno stato vuoto di glifi non ne ha.

### Il marchio passa alla palette Brillante

L'icona del launcher indossava ancora Carta, che dal 7 set non è più la palette che una
nuova installazione vede. È un disegno fatto a mano, quindi non esce da un generatore,
ma si sposta con la stessa regola di `tools/gen_scheme.py`: **ogni inchiostro tiene la
chiarezza CIELAB con cui è stato disegnato e prende tinta e croma dalla sorgente vivida
di DESIGN §2.5**.

| | Prima | Ora | |
|---|---|---|---|
| Luna, onda vicina | `#3589AC` | `#317DF5` | primario vivido `#2C7BF2` a L\* 54 |
| Stelle | `#C27D08` | `#BB8000` | secondario vivido `#FFB000` a L\* 58 |
| Onda lontana | `#6493A5` | `#6288E8` | stesso blu a L\* 58, croma allo 0,8 del bordo di gamut |
| Sfondo | `#F4F1EA` | `#E9F2FC` | neutro vivido al tono 95 |

Tenere la chiarezza tiene i contrasti che il badge aveva sul proprio fondo: stelle
2,99 → 3,00, luna 3,50 → 3,45, onda lontana 2,97 → 2,99. Due note oneste. La prima: le
**stelle non si muovono quasi**, perché a L\* 58 tutti e due gli ambra stanno già sul
bordo del gamut sRGB — è lo stesso fatto che §2.5 registra per i token che non si spostano,
non una modifica lasciata a metà. La seconda: l'onda lontana è l'unico numero scelto a
occhio invece che dalla regola. Alla croma proporzionale a quella di Carta (0,62 del
bordo) veniva un grigio-violetto; a 0,8 le due onde restano due onde e restano blu.

Il livello `<monochrome>` riusa lo stesso drawable e il sistema ne legge solo l'alfa,
quindi l'icona a tema non cambia.

### Il tema torna a «Come il telefono»

Il default scuro deciso poche ore prima è stato revocato dal committente. La ragione è
quella che la decisione precedente aveva già scritto **come proprio costo**: un'app che
ignora la modalità chiara del telefono sembra rotta, e un default è quello che la maggior
parte dei lettori vedrà per sempre. Entrambi gli schemi scuri restano a un tocco, e un
telefono in modalità scura li ottiene lo stesso senza chiedere.

Spostato anche il ripiego del **primo fotogramma**, in `MainActivity` e in
`WidgetConfigActivity`: `null` (lo store non ha ancora risposto) seguiva `DARK`, ora
segue il sistema, che è quello che lo store sta per dire.

### Le stringhe

Richiesta: in Impostazioni e nel resto dell'app, testi che descrivano **la funzione per
chi legge**, omogenei, né troppo lunghi né telegrafici; via le frasi che sembrano
giustificare qualcosa e quelle che insistono su un tema. Lo stile di riferimento è quello
delle app del Play Store, e le linee guida di Google dicono la stessa cosa: una riga di
supporto è **una** frase che dice cosa fa l'impostazione, non perché la si è costruita così.

Le tre famiglie di difetto trovate, con un esempio ciascuna:

1. **La giustificazione.** «In ogni caso si fermano quando il telefono chiede meno
   animazioni» (icone animate), «tenerli larghi è ciò che li tiene fuori dalla batteria»
   (promemoria del cielo). Vera, e non affare del lettore: è il progetto che si spiega.
2. **L'insistenza.** La privacy compariva tre volte con le stesse parole — in
   Impostazioni, nel primo avvio e nella guida — e ogni volta con una clausola in più.
   Ora è detta una volta per posto: in Informazioni resta il fatto secco, nel primo avvio
   resta solo la ragione del permesso (che Google chiede di dare), nella guida resta la
   frase su Open-Meteo senza il ritornello.
3. **La lunghezza disomogenea.** Le spiegazioni dei dialoghi andavano da 62 a 210
   caratteri. Ora stanno tutte fra 62 e 100, una frase o due corte.

Ritoccate anche: le due righe di configurazione del widget, il corpo della guida sulle
impostazioni (una catena di punti e virgola diventata quattro frasi) e la voce di
ripristino, che ora si chiama «Ripristina le impostazioni predefinite» in tutte e due le
lingue invece di essere più lunga in inglese che in italiano.

Nessuna stringa aggiunta o rimossa: `StringsParityTest` continua a valere e IT/EN restano
allineate riga per riga.

### Verifica

- `./gradlew test :app:testDebugUnitTest` verde, `:app:lintDebug` pulito,
  `:app:assembleDebug` costruito
- `SettingsStoreTest` aggiornato sul nuovo default del tema (tre asserzioni: prima
  installazione, enum sconosciuto, ripristino)
- Icona renderizzata e guardata a 432, 192, 96 e 48 px sotto la maschera circolare del
  launcher, su home chiara e scura
- Bordi del widget verificati su un mock geometrico con le metriche vere del carattere,
  prima e dopo

---

## L'anteprima nel picker, l'inset a sinistra, le icone del widget, le Informazioni (committente, 8 set 2026)

Quattro richieste in una passata, arrivate con lo screenshot della home: il widget «Ora»
di Chiaro sopra il widget meteo di un'altra app.

### Le anteprime del picker smettono di essere generiche

Segnalazione: nel selettore dei widget del launcher le tre anteprime sono generiche e non
somigliano al disegno vero. È esattamente la voce che la Fase 8 aveva rimandato («niente
preview nel picker per ora», in fondo a quella fase) e la ragione per rimandarla non
regge più: le si voleva **con** gli asset dello store, ma il picker è la prima cosa che
un lettore vede dei widget, e finché non c'è un `previewLayout` l'host mostra il layout
di caricamento di Glance — lo stesso cartoncino grigio per tutti e tre.

Come le fa tweather, e perché è l'unica strada: il picker **non esegue** il widget. Non
c'è luogo, non c'è report, non c'è composizione — quindi la preview è un layout statico
con valori d'esempio e il vestito di default cotto dentro. Tre file in `res/layout/`, uno
per widget, che ridisegnano il cartoncino vero:

| Anteprima | Che cosa disegna |
|---|---|
| `widget_now_preview` | glifo eroe, 25°, il luogo — i tre di VISION §5.9 e solo quelli, come il default |
| `widget_today_preview` | la riga eroe, la frase del giorno, cinque ore con la loro pioggia |
| `widget_sky_preview` | il momento davanti al lettore, l'ora, e la pastiglia del verdetto |

Gli hex sono **calcolati, non scelti**, e stanno in `values/colors.xml` invece che sparsi
nei tre layout: il fondo è l'ancora di mezzogiorno di `SkyPalette.Vivid` composita sotto
lo scrim §3.6 con l'aritmetica di `skyGradientBitmap` (`scrim × 0,55 + cielo × 0,45`, in
valori sRGB), gli inchiostri sono la coppia bianca di §3.6, le cifre della pioggia sono
`VividDarkColors.rainInkRamp` campionata **sui gradini** (0/25/50/75%, che cadono esatti e
non chiedono di riprodurre a mano un'interpolazione), e il verdetto è la coppia
inchiostro+contenitore di `VividDarkColors.pass` — mai una metà sola, che è la stessa
regola che la pastiglia vera segue. Le icone sono `mcn_*`: tratto, palette vivida, fondo
scuro, cioè quello che una installazione nuova disegna davvero.

`NoRawColorTest` guarda i `.kt` e questi sono XML, ma la regola non è aggirata: è che un
layout statico non ha composizione né tema da leggere — il launcher lo gonfia in un
contesto ristretto e non esegue una riga del nostro codice. Il posto unico dove i valori
stanno è il commento che li spiega.

Un'anteprima resta un **disegno del prodotto, mai una lettura**: nell'istante in cui il
launcher lega il widget, questo layout sparisce e il widget torna a non inventare nulla.

### L'inset a sinistra: 8 dp erano troppi, 4 sono la misura

Segnalazione: «avevamo riallineato l'icona che era troppo vicina al bordo; ora è un po'
troppo lontana, riavvicinala solo un poco — prendi spunto dal widget sotto il nostro».

Misurato sullo screenshot invece che a occhio (1080 px di larghezza, densità ~2,75):

| | Bordo card → inchiostro |
|---|---|
| «Ora» di Chiaro, luna a 8 dp di inset | 65 px (≈ 22 dp) |
| Il widget dell'altra app, stessa luna | 46 px (≈ 17 dp) |

La passata del 7 set aveva letto il margine del glifo sulla **famiglia** — 9/64 alla
mediana — e la mediana non è ciò che si sta guardando. `clear_night`, la falce, tiene
vuoto il **22,5%** della propria scatola dal lato d'attacco (misurato sul tracciato:
`min x` ≈ 15,9 su 64, meno 1,5 di stroke), cioè due volte e mezzo la mediana. L'inset che
corregge un glifo mediano ne sovracorregge i più marginati, e quelli sono proprio i
disegni che una home notturna mostra.

`WidgetCardPaddingLeading` passa da **8 a 4 dp**: la falce si ferma a un paio di dp dal
widget su cui la segnalazione è stata misurata, il glifo mediano resta a ~12 dp dal bordo
— più dei 6 dp di testo nudo da cui la storia era partita — e la colonna delle parole si
riprende 4 dp dei 14 che aveva perso. Il bordo destro e i 6 dp sopra/sotto non si toccano.

### La famiglia di icone diventa una proprietà del widget

Richiesta: nelle proprietà del widget, poter scegliere il tipo di icona meteo — «Segui
l'app», «Piene» o «A tratto» — per poterle avere diverse fra widget e app.

Accolta, e sta accanto a sfondo e opacità per lo stesso motivo per cui quelle sono
per-widget: **un cartoncino sulla home non è la schermata dell'app**. Sta su un wallpaper
che l'app non vede, a una dimensione che l'app non disegna, e la famiglia piena si legge
da dall'altra parte della stanza dove quella a tratto si legge bene su una pagina. Che un
lettore voglia onestamente una per posto è più che plausibile.

- `WidgetIcons { APP, FILL, LINE }` in `WidgetLookStore`, con `resolve(app)` che è tutta
  la logica; `WidgetLook.icons` di default `APP`, quindi ogni widget già piazzato continua
  a disegnare quello che disegnava e ogni cambio futuro in Impostazioni lo raggiunge.
- `WidgetModel.iconStyle` risolve **una volta** per card: il glifo eroe e la striscia
  delle ore sulla stessa scheda non possono finire di due famiglie diverse.
- La sezione è offerta a tutti e tre i widget, a differenza degli interruttori di
  contenuto sotto: tutti e tre disegnano glifi meteo.
- Le due etichette dei disegni sono le stringhe che le Impostazioni già usano
  (`settings_icons_fill`, `settings_icons_line`): un lettore incontra un controllo, non due.

### Informazioni: i dati che mancavano

Richiesta: completare «Informazioni» guardando i dati che mostra tweather. Il blocco
`about` di `settings.config` porta `app_name`, `version`, `developer`, `copyright`,
`license` e un nodo `credits` con la sorgente dei dati e il carattere; Chiaro aveva
versione, sorgente dati, codice sorgente e privacy.

Aggiunte quattro righe e completate due:

| Riga | Valore |
|---|---|
| Sviluppo | Callback Dev |
| Copyright | © 2026 Fiorenzo Brioni |
| Licenza | GPL-3.0, che apre il testo della licenza |
| Set di icone del meteo | Meteocons di Bas Milius, MIT |
| Carattere | Inter di Rasmus Andersson, SIL OFL 1.1 |
| Icone dell'interfaccia | Material Icons di Google, Apache 2.0 |
| I dati del meteo | ora dice anche **CC BY 4.0** |

I tre crediti sono esattamente ciò che `licenses/README.md` dichiara viaggiare dentro
l'APK: un carattere e una famiglia di icone incorporati sono lavoro di qualcuno, e una
schermata che nomina il fornitore dei dati e si ferma lì è onesta per due terzi. Chiaro
ne ha uno in più di tweather perché ne incorpora uno in più.

Il nome dell'app non prende una riga sua: in una lista M3 sotto un titolo «Impostazioni»
sarebbe la stessa parola detta due volte, mentre nel file JSON di tweather `app_name` è
una chiave come le altre. Sviluppo e copyright sono `translatable="false"`: un nome
proprio non è prosa.

### Verifica

- 476 test verdi (165 domain, 158 data, 5 sync, 148 app), zero failure, zero skip;
  `:app:lintDebug` pulito; `:app:assembleDebug` costruito
- Due test nuovi: `WidgetPreviewTest` (ogni provider nomina un `previewLayout` che
  esiste; nessuna anteprima usa una view che `RemoteViews` non sa gonfiare — è così che
  lo `Space` della striscia di Oggi è diventato un peso sulla frase) e `WidgetIconsTest`
  sulla tabella di `resolve`
- `StringsParityTest` continua a valere: le stringhe nuove arrivano in coppia, tranne le
  due `translatable="false"`
- Inset misurato sul tracciato vettoriale di `mcn_clear_night` e sullo screenshot del
  committente, non stimato

---

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
      `chiaro-release.password.txt`), poi il tag `v1.0.0`.
      **Prova generale eseguita** (2 set 2026, tag `v0.0.1-rc1`, poi cancellato con la
      sua release): la catena Secrets → keystore decodificato → APK firmato → GitHub
      Release ha funzionato al primo colpo — `chiaro-v0.0.1-rc1.apk` riscaricato da
      GitHub e verificato con apksigner (stessa impronta della chiave vera), mapping R8
      allegato e accoppiato al tag, corpo della release caduto correttamente sulle note
      generate con il warning previsto (nessuna sezione `0.0.1-rc1` nel CHANGELOG).
      Un difetto trovato e corretto: una release `-rc` usciva come release piena ed era
      etichettata "Latest" — ora `release.yml` marca prerelease ogni tag con trattino,
      che è la definizione SemVer di prerelease

## Il Diario, revisione della card (committente, 8 set 2026) — e il cerchio si chiude

Nove rilievi su una schermata sola, più la funzione che le mancava. In ordine di peso.

- [x] **L'asse della striscia diventa il tempo.** Era una colonna per *fetch*: alla
      cadenza predefinita di un'ora la striscia mostrava quattordici ORE sotto un
      titolo che dice «la settimana», stampava sotto «14 aggiornamenti, da Sab 5 a Sab
      5» (primo e ultimo formattati a solo giorno, quasi sempre lo stesso), e disegnava
      una notte a telefono spento esattamente come un'ora di sonno. Ora una colonna è
      una fascia di **sei ore**, quattordici fasce sono tre giorni e mezzo — la
      finestra che VISION §5.5 promette — e una fascia senza fetch è una **casella
      vuota**, non una colonna che sparisce. Le fasce vuote in testa si tagliano: la
      striscia comincia dove comincia la prova, mai prima. La didascalia stampa la
      larghezza della fascia e i due estremi con **giorno e ora**
- [x] **Le righe sono solo i giorni ancora davanti.** L'orizzonte del fetch più recente
      parte dal *suo* domani: dopo tre giorni offline la striscia intitolata «la
      settimana» mostrava giorni già passati. Ora le date si filtrano su oggi, e una
      striscia senza più giorni davanti non si disegna
- [x] **La legenda delle massime stampava estremi che la scala non ha.** Campionava
      −10…40 °C su una rampa ancorata a −5/35 con `coerceIn`: misurato, ΔE(−10, −5) =
      0,00 e ΔE(40, 35) = 0,00 — due pastiglie duplicate e due numeri falsi, contro
      DESIGN §8.10 e §9.3. Ora i sette campioni sono equispaziati fra le ancore vere e
      i numeri sotto sono quelli
- [x] **La frase della deriva partiva in minuscolo, in italiano**: «sabato 5 è
      migliorato». `JournalText` e i titoli dei giorni facevano già `titlecase`, la
      frase no; in inglese il difetto non si vedeva perché il giorno è già maiuscolo
- [x] **«Oggi» e «Ieri» seguono il fuso del LUOGO**, come il raggruppamento sopra:
      leggevano `LocalDate.now()` di sistema, e il Diario di Tokyo visto dall'Italia
      poteva intitolare «Oggi» il domani di Tokyo
- [x] **Un tocco sulla striscia apre i numeri.** C'era un `onClick = {}`: ripple che non
      faceva niente e, peggio, un'azione «tocca due volte per attivare» annunciata a
      TalkBack che non esisteva. Il suggerimento cambia di conseguenza («tocca per i
      numeri») e la striscia guadagna una descrizione unica — le caselle non hanno
      testo, quindi senza di quella un lettore di schermo sentiva sette date e nient'altro
- [x] **La pioggia non inventa più uno zero.** `journal_field_rain` stampava
      `old ?: 0`, cioè «pioggia 0% → 80%» dove il valore vecchio non c'era, mentre le
      temperature accanto stampavano già il trattino. E il difetto veniva da più in
      basso: vedi la riga sulla probabilità nullable, sotto
- [x] **Tre stati per la frase, non due.** «Finora la settimana è rimasta com'era»
      copriva sia la settimana ferma sia quella che si è mossa ed è tornata — la frase
      poteva contraddire le due revisioni elencate sotto di sé. Ora esiste «è andato su
      e giù, fra X e Y», con le stesse soglie del motore di diff (10 punti, 1 °C) così
      la frase non può mai affermare un movimento che le voci non hanno registrato
- [x] **La retention diventa per città.** `prune` era globale: con quattro luoghi
      salvati ogni Diario aveva venticinque commit, e la profondità della propria
      cronologia dipendeva da quanti altri posti si seguono. Ora cento commit **per
      città**, più un tetto globale come rete di sicurezza (togliere un luogo non
      cancella i suoi commit). Di conseguenza il Diario legge tutta la cronologia della
      città e non più 40 righe — che a cadenza oraria erano meno di due giorni, meno
      della finestra della striscia stessa
- [x] **Il tick del minuto sparisce.** Il ViewModel ricostruiva quaranta righe di JSON e
      il motore di diff una volta al minuto, anche a dati fermi e anche in risparmio
      energetico (cosa che Oggi non fa). Niente su quella schermata invecchia con
      l'orologio: gli orari sono tutti assoluti. Ora segue un Flow di Room per città,
      che riemette alla scrittura e solo alla scrittura — più un solo risveglio a
      **mezzanotte del luogo**, che è l'unico evento d'orologio che questa schermata ha
      davvero (lì «Oggi» diventa «Ieri», e lì una giornata finita diventa giudicabile).
      Il fuso lo risolve una funzione sola, `JournalStateBuilder.zoneOf`, così
      raggruppamento, etichette, verdetti e sveglia non possono discordare
- [x] **«E poi com'è andata?»** — il cerchio che mancava: `ForecastOutcome`, e una riga
      in più che chiude ogni giornata finita

### Decisioni dell'intervento

- **La probabilità giornaliera diventa nullable fino a schermo.** Il seme mappava
  `precipitation_probability_max` con `?: 0`, e uno zero è una previsione di niente
  pioggia messa in bocca a un modello che non ha parlato — esattamente ciò che §1.1
  vieta, e che Chiaro già rispettava per l'ora ma non per il giorno. Ora: la riga della
  settimana non stampa nulla e tiene la sua colonna (un trattino sarebbe un valore), lo
  snapshot omette la chiave, la striscia disegna assenza, la variabile di regola
  `today.precip_pct` non si risolve e la sua regola salta invece di far scattare
  `< 10` su un dato mancante, e il riepilogo del mattino lascia cadere la frase della
  pioggia invece di annunciare «0%». Registrato in `UPSTREAM.md`: è un bug anche
  a monte.
- **Il verdetto della giornata poggia sull'ora, non sull'istante.** `current.precipitation`
  di Open-Meteo è la somma dell'ora **precedente**, quindi con fetch orari le
  osservazioni piastrellano la giornata invece di campionarla e sperare che non abbia
  piovuto in mezzo. Due chiavi nuove nello snapshot (`current.wmo_code`,
  `current.precip_last_hour_mm`): il codice perché la domanda «pioveva quando hai
  guardato» deve avere per risposta un numero che il dominio sa leggere
  (`WeatherCodes.isPrecipitation`), non l'etichetta inglese riconosciuta a mano.
- **Le due regole del verdetto non sono simmetriche, ed è il punto.** «Ha piovuto»
  basta una osservazione bagnata: il positivo è una prova, e nessuna ora mancante può
  dis-vederla. «Non ha piovuto» pretende **16 ore delle 24 davvero coperte** (finestre
  unite, così una raffica di pull-to-refresh non compra copertura). Sotto quella soglia
  la giornata non prende nessun verdetto — non «forse», proprio nessuna riga: una
  sezione senza dati non si disegna. Le ore coperte sono stampate accanto al verdetto,
  come un run del cielo stampa il suo `obs`: un giudizio che nasconde la sua copertura
  è un giudizio che non si può pesare.
- **La massima osservata si chiama «vista».** È un massimo su campioni, non una misura
  della stazione, e la parola lo dice. Compare solo con la copertura sopra soglia,
  perché un massimo su tre letture non è un massimo.
- **Un commit scritto prima delle due chiavi è silenzio, non un'ora asciutta.** Niente
  fallback che riconosca a ritroso l'etichetta inglese: la funzione si riempie dai
  giorni successivi all'aggiornamento invece di giudicare giornate su cui non ha prove.
- **Il verdetto apre il giorno che chiude**: è datato all'ultimo istante della giornata,
  quindi l'ordinamento dal più recente lo mette in cima alla sua sezione.
- **La spunta non è un verdetto.** L'icona dice «questa giornata è stata verificata»,
  non «bene» o «male»: verde e rosso sarebbero un giudizio sul tempo, e com'è andata è
  un fatto. La parola porta l'esito, come dappertutto qui.
- **Le Minime restano fuori dalla striscia, e adesso è scritto perché.** Non sono
  escluse dal Diario — sono salvate, diffate a 1 °C e stampate nella prosa delle
  revisioni («minima 12° → 14°»): sono fuori dai *chip*. La minima non è una domanda di
  deriva ma di soglia — «gela?», «si dorme?» — e quella risposta si scrive con un
  avviso, non con una striscia di calore. E un giudizio non le si può dare: il verso si
  ribalta con la stagione, perché una minima che scende è sollievo ad agosto e brina a
  gennaio, e qui ogni numero deve portare la sua conseguenza. Conferma misurata, non
  motivo principale: sulla rampa ancorata al mondo una fascia di minime italiane
  (≈2–20 °C) occupa il 45% della rampa contro il 65% delle massime.
- **Quello che NON è stato aggiunto**, per la stessa regola: filtri per tipo di voce (le
  soglie tengono già basso il volume), ricerca, esportazione, un terzo chip qualsiasi.
  Vento e millimetri non esistono in `DailyForecast` e costerebbero modello, mapper,
  schema e migrazione: si valutano per conto loro, non si infilano qui.

### Coda dell'intervento (committente, 8 set 2026) — il sottozero

- [x] **`Formats.temperature` stampava `-0°`.** Domanda del committente sulle minime
      sottozero; la risposta è che escono senza filtri né clamp (`temperature_2m_min` è
      un `Double` non-nullable che arriva intatto a schermo, e lo slider degli avvisi va
      da −30 a +45 °C, quindi «minima sotto 0» è scrivibile). Cercando la conferma è
      saltato fuori un difetto vero: `%f` conserva il segno di un valore che ha appena
      arrotondato via, quindi −0,4 °C stampava `-0°`. Verificato sulla JVM prima e dopo:
      `-0,4 → "0°"`, `-0,5 → "-1°"`, `-3,4 → "-3°"`. Vale per ogni temperatura di ogni
      schermata, perché passano tutte da lì. Sette asserzioni nuove in `FormatsTest`,
      su entrambe le lingue, entrambe le unità e due precisioni.

### Il gelo nella striscia (committente, 8 set 2026)

- [x] **Un segno accanto al giorno, non un terzo chip.** La domanda «e d'inverno?» ha
      una risposta che la striscia poteva dare senza tradire sé stessa: il gelo è una
      soglia, e una soglia si segna, non si dipinge su una rampa. Un fiocco accanto
      all'etichetta del giorno quando la minima prevista è **a zero o sotto**, e sotto
      la striscia una riga che nomina quei giorni col loro numero — «Gelo previsto:
      sabato 5 (−2°), domenica 6 (0°)». Il dato era già su disco: `low_c` sta nello
      snapshot dalla Fase 7.

### Decisioni

- **Zero, non due gradi.** La brina da irraggiamento arriva anche con una minima a 2 °C
  perché il suolo irraggia più dell'aria a due metri, ma un segno che scatta su una
  notte che il termometro del lettore leggerà +2 è un segno che si impara a ignorare.
  La sfumatura la porta un avviso, che ha le parole per dirla; un glifo no.
- **Decide l'ultima parola, non la più fredda mai detta.** Il segno dice cosa prevede
  l'app adesso: un giorno che è tornato sopra zero lo perde, invece di tenersi un
  avvertimento che nessuno sostiene più.
- **Non è mai il glifo da solo** (DESIGN §10): la riga sotto la striscia è insieme la
  legenda del segno e l'informazione stessa, e sta **fuori** dalla descrizione unificata
  della striscia, così un lettore di schermo la riceve come frase propria invece che
  ripiegata in un paragrafo sulle colonne. Quando non gela non c'è: una riga «niente
  gelo» ogni settimana è il riempitivo che questa schermata rifiuta.
- **Lo spazio del segno si aggiunge a tutte le righe o a nessuna**, così le caselle
  restano una griglia sola; una settimana senza gelo disegna esattamente la striscia
  che disegnava prima.
- **L'accessore si chiama `frost`, non `snowflake`** (§13.1: nomina la grandezza, non il
  disegno). Il disegno è il fiocco di Meteocons, ma la domanda lì è il ghiaccio, non la
  neve.
- **L'esempio della guida resta senza segno.** Le percentuali del campione sono
  dichiaratamente inventate, ma un avviso di gelo su un giorno vero e nominato si
  leggerebbe come una previsione: il testo lo descrive a parole, il disegno non lo finge.

### Rimasto aperto

- **Il Diario del GPS si azzera se ti sposti.** `cacheKey` è lat/lon arrotondati a due
  decimali (~1,1 km), quindi «la mia posizione» cambia chiave con te e la cronologia
  riparte: la schermata dice «Ancora niente da raccontare» mentre i commit esistono
  sotto un'altra chiave. Non toccato di proposito: quella chiave governa cache, cache su
  disco, avvisi, cielo e widget, e re-inchiavarla è una decisione di prodotto con una
  migrazione dietro, non una rifinitura del Diario.
- **Sotto −5 °C il colore satura, il numero no.** `temperatureAt` è ancorata a −5/35
  con `coerceIn`, e la usano due superfici: la striscia di deriva sulle massime e — cosa
  che la review aveva mancato — la barra di intervallo della settimana
  (`WeatherCharts.kt:292`). Quindi il capo sinistro della barra a −5 e a −12 è dello
  stesso blu. La barra però non mente: il numero stampato accanto è esatto e la
  posizione è giusta, perché quella scala è il min/max della settimana e non le ancore
  della rampa. È il costo dichiarato di DESIGN §9.1 (una scala ancorata al mondo, non a
  ciò che c'è a schermo), non un difetto da correggere di nascosto: se le ancore vanno
  allargate è una decisione di design, con le sue misure di contrasto da rifare.
- **`AlertKind.PRECIPITATION` tiene il suo `?: 0`**: lì la probabilità viene dall'ora che
  ha superato la soglia, quindi è non-nulla per costruzione e il fallback non può
  scattare. Lasciato com'è per non allargare la modifica.
- **Le due `plurals` nuove non definiscono `many`** e lint le segnala, come le sei già
  presenti nel file: in italiano `many` vale da un milione in su, e qui si contano ore
  di una giornata e larghezze di fascia. Coerenti col resto del file.

### Verifica

823 test verdi in tutti i moduli (11 nuovi in `ForecastOutcomeTest`, 4 nuovi in
`JournalStateBuilderTest`, 2 in `WeatherSnapshotsTest`), zero failure, zero skip;
`:app:lintDebug` a zero errori (66 warning, gli stessi di prima più le due `many`
sopra); `:app:compileDebugKotlin` pulito.

---

## Il widget «Ora» in tre forme (committente, 8 set 2026, sera)

Richiesta, con lo screenshot della home accanto al widget meteo del launcher a quattro
misure (4×1, 3×1, 2×1, 2×2): togliere minima e massima («un utente guarda i dettagli
aprendo l'app, il widget è un'info rapida sullo stato attuale»); al posto della stringa
dello stato una descrizione della giornata come quella del vicino («Probabili temporali
stanotte»), con lo stato come ripiego; e un layout che **cambi con la dimensione** come
fa il suo, con testi allineati e visibili dove c'è spazio. Lo sfondo resta il nostro.

### Misure prese dallo screenshot (923 px di larghezza, ≈2,35 px/dp)

| | Card | Glifo (inchiostro) | Temperatura | Descrizione |
|---|---|---|---|---|
| Vicino 4×1 | ~340 × 74 dp | ~40 dp, a 17 dp dal bordo | ~27 sp | 2 righe ~16 sp, a destra, a ~25 dp dal bordo |
| Vicino 3×1 e 2×1 | ~250 / ~159 × 74 dp | come sopra | come sopra | **assente** |
| Vicino 2×2 | ~159 × 189 dp | ~64 dp, in alto a destra | ~30 sp | 2 righe sotto il numero, poi il luogo |
| Chiaro 4×1 (prima) | ~340 × 82 dp | ~64 dp | 34 sp | «Poco nuvoloso» a 20 sp accanto al numero |

Quindi una cella del launcher concede ~82 dp d'altezza al widget (i ~101 della quarta
passata erano la cella, non la concessione), due celle ~159 dp di larghezza, tre ~250,
quattro ~340.

### Le tre forme (`NowWidgetLayout.kt`, puro, con tabella in `NowWidgetLayoutTest`)

- **Stretta** (una riga, due o tre celle): glifo, temperatura, luogo. Come prima, meno
  le opzioni.
- **Larga** (una riga, quattro celle o più): la stessa riga, e la **frase del giorno**
  contro il bordo opposto, allineata a destra e centrata sulla riga. Le due colonne di
  testo si dividono lo spazio a metà (`nowSentenceColumnWidth`): Glance non misura il
  testo, e la metà è anche quello che fa il vicino — il suo blocco di descrizione è largo
  quanto quello del numero, e il vuoto cade in mezzo dove l'occhio se lo aspetta. La frase
  compare quando la sua colonna ha almeno 96 dp (≈14 caratteri a 14 sp): a quattro celle
  sono 116, a tre 71.
- **Alta** (due righe o più, `TallMinHeight` 150 dp): il glifo da solo nell'angolo in alto
  a destra, sotto temperatura, frase (due righe) e luogo impilati a sinistra — l'ordine del
  vicino e l'ordine in cui legge Oggi. Un `Box` e non una `Column`: il glifo è ancorato in
  alto, le parole pendono dal basso, e le due scatole **possono condividere la banda vuota
  sopra le maiuscole della temperatura** (`textInkBalance`, 8 dp), che il glifo riempie
  solo col proprio margine (9–12 dp di scatola vuota in basso). In orizzontale non si
  incontrano comunque: il glifo a destra, il numero a sinistra. Sul 2×2 del device il
  glifo viene ~75 dp di scatola (~54 d'inchiostro contro i 64 del vicino) dove una pila
  semplice ne avrebbe lasciati 67.

### Decisioni

- **La frase è quella di Oggi, in registro breve.** `HeadlineText.of(…, brief = true)`:
  l'ombrello tiene la sua ora e perde la coda «schiarisce dopo le…», «la pioggia dovrebbe
  smettere verso le 17:00» diventa «pioggia fino alle 17:00 circa» (due stringhe nuove,
  IT/EN; le altre erano già brevi). Stesso fatto, stessa ora, stessa cautela; vive **nello
  stesso oggetto** della frase intera perché i due registri non possano mai disaccordarsi
  su quale frase merita una previsione. Il widget Oggi tiene la frase intera: ha la
  larghezza.
- **Il ripiego è lo stato del cielo, non il vuoto.** «Poco nuvoloso» è una cosa vera sul
  cielo di adesso — è quello per cui la card esiste — e uno slot che restasse bianco ogni
  giorno tranquillo si leggerebbe come una card che non ha finito di caricare. Non è il
  riempitivo che la frase di Oggi rifiuta: lì il vuoto sta sopra un canvas che già dice
  tutto, qui starebbe accanto a un numero nudo.
- **Il contenuto segue la dimensione: rovesciata la decisione del 4 set.** Allora la
  comparsa automatica dello stato al ridimensionamento era stata proposta e respinta
  («un widget che si riscrive mentre lo si dimensiona non si può mirare»). Vissuta accanto
  a un widget che lo fa, la regola opposta è quella che il lettore si aspetta: mostrare
  quello che ci sta alla dimensione data è la grammatica dei widget del launcher. Quindi
  l'interruttore «stato accanto alla temperatura» **esce** (campo, chiave, riga di
  configurazione, due stringhe; `forget` rimuove ancora la chiave orfana di un widget
  piazzato quando c'era).
- **Minima e massima escono dal widget Ora** e restano su Oggi, dove la riga di
  configurazione compare ora **solo** per quel widget. Sul widget Ora contendevano alla
  frase lo stesso bordo, e sono a un tocco di distanza nell'app.
- **Il glifo a due celle cede un po' d'altezza alle parole** (`nowRowIconSize`): a 159 dp
  un glifo alto quanto la riga lasciava al luogo ~65 dp, cioè «Dergan…». La colonna delle
  parole tiene 84 dp dove può («−12°» a 34 sp sono ~72, un nome di dieci lettere col pin
  ~84) e il glifo prende il resto fino al proprio pavimento: 56 dp su quella card, 68 su
  una griglia da 178. Da tre celle in su comanda l'altezza, come prima.
- **Le righe della frase sulla riga larga sono quante l'altezza ne tiene, massimo tre**
  (`nowSentenceLines`): due tagliavano «Pioggia per il resto della giornata» nella colonna
  più stretta che qualifica, e la riga l'altezza la ha (tre righe ≈56 dp contro i ~70 che
  la card lascia). Con la scala font del lettore il conto scende invece di sbordare. Sulla
  forma alta sono due, come il vicino: la terza costerebbe al glifo 19 dp e a 20 caratteri
  per riga due bastano al registro breve. Glance mette l'ellissi («…»), verificato nello
  stile `Glance.AppWidget.Text` delle risorse fuse.
- **Un numero per bordo, deciso da cosa ci sta contro.** Glifo: 4 dp (`Leading`, misura
  dell'8 set mattina, ora anche il bordo destro della forma alta) e 6 sopra/sotto
  (`Snug`, anche il bordo alto della forma alta). Parole: **14 dp** (`WidgetCardPadding`,
  l'inset di Oggi) su ogni bordo che toccano — il destro della riga, sinistro e basso
  della forma alta, tutti e quattro degli stati vuoti. Il bordo destro passa da 12 a 14: i
  12 erano quelli dell'altro widget da una cella quando su quel bordo stava al più la
  coppia opzionale; un blocco di frase allineato a destra è una cosa più grande da mettere
  contro un angolo (il vicino la tiene a ~25 dp su un raggio molto maggiore), e i due dp
  escono da una colonna che la coppia non deve più pagare.
- **Corpi**: temperatura 34 sp Medium (l'eroe tarato in cinque passate, su tutte le
  forme), frase 14 sp Medium in inchiostro pieno (la veste che la stessa frase ha sul
  widget Oggi: seconda cosa che si legge dopo il numero, e un blocco multiriga accanto a un
  34 sp vuole il corpo del testo, non uno di display), luogo 15 sp attenuato, marcatore di
  età 11 sp sotto il luogo su ogni forma (e nella forma alta il budget del glifo lo paga
  quando c'è).
- **Piazzamento predefinito a 4×1** (`targetCellWidth` da 3 a 4): è la forma a una riga
  che porta la frase e la misura a cui si apre il widget del vicino; il minimo resta due
  celle. L'anteprima del picker disegna la stessa forma con gli stessi inset.
- **Glance e le metriche**: `textLineHeight` (1,32 em, la scatola top-to-bottom di Roboto
  con `includeFontPadding`) è una stima dichiarata; i budget che ci poggiano tengono la
  banda di `textInkBalance` come gioco. A scala font 1,3 sul 2×2 il testo supera lo
  spazio, il pavimento di 52 dp tiene e il margine del glifo assorbe i pochi dp di
  sovrapposizione — verificato a mano sulla geometria, non su device.

### Rimasto aperto

- **Su device.** La soglia dei 150 dp per la forma alta e i 96 dp per la colonna della
  frase sono tarate sulle concessioni misurate qui e su una griglia a cinque colonne
  (320 dp per quattro celle, che qualifica per mezzo dp); un launcher che concede meno di
  ~316 dp a quattro celle mostrerebbe la forma stretta. Se succede, il numero da toccare
  è `SentenceColumnMin`, con la tabella dietro.
- **Il nome del luogo si tronca a metà colonna** sulla forma larga («Cavenago di Bri…»):
  è il costo della divisione a metà, scelto perché l'alternativa — la colonna del numero a
  misura — lasciava alla frase quello che il nome non prendeva, e un nome lungo la
  cancellava. Se disturba, la via è pesare 2:3 invece di 1:1, non misurare.

### Verifica

Suite `:app` verde (165 test, 8 nuovi in `NowWidgetLayoutTest`: la forma per ciascuna
delle concessioni misurate, le due larghezze di colonna, il glifo della riga vincolato
dall'altezza o dalla larghezza, le righe della frase alle due scale font, il glifo della
forma alta con e senza marcatore di età, pavimento e soffitto). `:app:lintDebug` a zero
errori; i due `RtlSymmetry` che l'anteprima nuova aveva introdotto sono chiusi sul posto.
APK di debug costruito; **la verifica su device è del committente**, alle quattro misure
dello screenshot più il 2×2 con un luogo lungo.

Trovato di passaggio, e corretto: `PaletteDocTest` era rosso **solo su Windows** —
`core.autocrlf` consegna `DESIGN.md` con CRLF e le regex delle rampe cercano
«```\n» letterale. Il test normalizza i fine riga prima di leggere; CI su Linux non lo
aveva mai visto.

---

## Ora: tre ritocchi sullo screenshot; Cielo: rifatto in tre forme (committente, 8 set 2026, sera)

Screenshot del device con le tre forme di «Ora» accanto al widget del launcher: «va
benissimo», più tre ritocchi da valutare («falli solo se sei convinto») e la richiesta di
rifare da capo il widget «Cielo» con la stessa logica di adattamento.

### I tre ritocchi, tutti applicati, con le misure che li reggono

| | Prima | Ora | Perché |
|---|---|---|---|
| Frase del giorno | 14 sp Medium | **16 sp** Medium | il vicino la scrive a ~17 sp sotto un numero da ~30; a 16 sotto il nostro 34 il rapporto è lo stesso. Con Roboto misurato: ogni frase breve tiene **due righe** nella colonna da 116 dp del 4×1 e da 131 del 2×2 |
| Luogo | 15 sp | **16 sp** | stesso corpo della frase, distinta da peso e inchiostro (Regular, attenuato) come fa il vicino. «Cavenago di Brianza» misura **145 dp** a 16 sp e la colonna del 3×1 ne ha 154: ci sta. Col pin GPS (16 + 4) non ci starebbe a nessuno dei due corpi, e a 4 celle (116 dp) si tronca con l'ellissi in entrambi i casi: limite dichiarato, non introdotto |
| Pin della posizione | 0,9 × testo | **1,0 × testo** | il disegno riempie 20/24 della scatola, quindi a 0,9 su 16 sp erano 12 dp d'inchiostro (l'altezza delle maiuscole e basta); il pin del vicino misura ~13,5 dp, maiuscola più discendente. A 1,0 sono 13,3 |

Il costo dei due corpi in più è pagato dal glifo della forma alta, che scende da ~75 a
~69 dp di scatola (`nowTallIconSize`), e da **due stringhe brevi in più**: a 16 sp
«Pioggia per il resto della giornata» era l'unica frase a chiedere tre righe sul 2×2, e il
registro breve la dice «del giorno» (l'inglese era già breve; le stringhe esistono in
entrambe le lingue perché il registro è uno). L'anteprima del picker segue i corpi nuovi.

### Cielo: le tre forme (`SkyWidgetLayout.kt`, puro, con tabella in `SkyWidgetLayoutTest`)

Il numero-eroe è **l'ora del momento** a 30 sp Medium, con sotto il nome a 15 sp
attenuato e davanti il glifo del momento che riempie l'altezza (tetto 72 dp). La domanda
della card è «quando, e vale la pena uscire»: il primo pezzo lo dice un orologio leggibile
a un braccio di distanza, dove un nome a 15 sp sopra un'ora a 12 non lo diceva; il secondo
lo dice il verdetto; quale momento sia lo dice il glifo, come il glifo di «Ora» dice che
tempo fa. Le due card sulla stessa home si leggono come sorelle.

- **Stretta** (tre celle, il minimo del widget): glifo · ora / **segno** + nome. Il segno è
  il glifo del verdetto — `✓ ~ ✗ ?`, il vocabolario della serie e gli stessi caratteri con
  cui si apre il chip dell'app — da solo in un tondo da 22 dp nei colori misurati del
  verdetto. È il verdetto alla misura che una card stretta può permettersi: una forma prima
  che un colore (DESIGN §2.3), quindi leggibile in deuteranopia, e la parola sta una forma
  più su o a un tocco.
- **Larga** (quattro celle e oltre): la stessa riga, e contro il bordo opposto la colonna
  del verdetto (96 dp fissi): il chip con la **parola** sopra e il **numero** che l'ha
  deciso sotto («nuvole 10%»). Fissa e non pesata perché un chip non va a capo: «Presto per
  dirlo» a 11 sp misura 89 dp con il suo padding, e 12 sp non ci starebbe.
- **Alta** (due righe o più): quella riga in testa, col glifo a 60 dp fissi, e sotto
  l'elenco dei momenti successivi, uno per riga — glifo piccolo, nome, ora col marcatore
  del giorno, verdetto: la parola su una card larga, il segno su una stretta, così una card
  parla un registro solo dall'alto in basso. Il budget (`skyRows`) è aritmetica sulle
  altezze vere: sul 3×2 e sul 4×2 del device tre righe; la lista non si imbottisce mai.

### Decisioni

- **La forma non dipende dal contenuto.** Larga o stretta lo decide la geometria
  (`skyIsWide`: la colonna delle parole tiene almeno 120 dp una volta pagata quella del
  verdetto), mai quale momento c'è: una card che cambiasse forma con la previsione non si
  potrebbe mirare — la regola già scritta per «Ora».
- **L'ora a 30 sp, non 34 come la temperatura.** Un'ora è più lunga di una temperatura:
  «12:05 AM» sono otto glifi contro i quattro di «−12°», e a 34 misura 149 dp contro i 136
  che il 4×1 lascia alle parole; a 30 ne misura 132. Cinque glifi a 30 portano circa
  l'inchiostro di tre a 34, quindi i due numeri affiancati hanno lo stesso peso ottico.
- **Il glifo di Cielo ha un tetto a 72 dp** dove quello di «Ora» arriva a 104: sul launcher
  che concede 101 dp a riga un'alba da 89 dp schiaccerebbe l'orologio e, sottraendo alla
  colonna delle parole, riporterebbe il 4×1 alla forma stretta. Sul device (82 dp) i due
  glifi sono uguali, 70.
- **Marcatore del giorno prima del nome** («Domani · Sorge la luna»), così quando la
  colonna finisce è la coda del nome a cadere con l'ellissi, mai la parola che dice quale
  giorno. Sui nomi lunghi con marcatore a tre e quattro celle l'ellissi c'è, misurata:
  «Domani · Sorge la luna» sono 150 dp a 15 sp contro 136 di colonna. Limite dichiarato.
- **Una finestra in corso mostra l'ora in cui finisce**, con «Adesso · Ora d'oro» sotto:
  «adesso, fino alle 20:20» è la prossima cosa che succede, e stampare come eroe un'ora
  già passata sarebbe strano. Le finestre non in corso stampano il solo inizio: «19:55 –
  20:20» a 30 sp sono 200 dp, e la chiusura è a un tocco.
- **Chip con la sola parola, segno col solo glifo.** Il chip dell'app scrive glifo e
  parola insieme; qui la parola da sola perché «✓ Presto per dirlo» sforerebbe la colonna
  da 96 di un dp. Il numero sta sotto il chip sull'eroe e a un tocco nelle righe.
- **Lo stato vuoto di Cielo** («Nessun momento del cielo in arrivo») era rimasto in alto a
  sinistra quando il 7 set gli altri due erano stati centrati: ora è centrato come loro.
- **Piazzamento predefinito a 4×1** anche per Cielo (da 3), la forma con parola e numero;
  il minimo resta tre celle (`minWidth` 180 dp). L'anteprima del picker è la forma larga.
- `SkyWidgetRowsTest` esce con il budget che fissava; `SkyWidgetLayoutTest` prende il suo
  posto con forme, glifi, colonne e righe.

### Rimasto aperto

- **Su device**: le tre forme di Cielo a 3×1, 4×1, 3×2 e 4×2, con un momento di domani e
  uno in corso; la resa del segno `✓ ~ ✗ ?` col font di sistema Samsung (nell'app gli
  stessi caratteri si vedono già, ma il widget passa da RemoteViews).
- **Righe compatte a 180 dp di larghezza** (una griglia da 90 dp per cella concede il 2×2):
  «Domani · 06:47» più il segno lasciano al nome pochi dp e l'ellissi lo mangia. Sotto i
  180 il widget non è piazzabile; sopra i 250 (tre celle del device) il nome ha ≥ 78 dp.
- **Le lettere lunghe dei crepuscoli** («Crepuscolo astronomico, mattina», 236 dp a 16 sp)
  si troncano su ogni forma a una riga: nomi da 31 caratteri non stanno in nessuna colonna
  da 136, e accorciarli è una decisione di copy della schermata, non del widget.

### Verifica

Suite `:app` verde (167 test: 7 in `SkyWidgetLayoutTest` al posto dei 5 di
`SkyWidgetRowsTest`, gli 8 di `NowWidgetLayoutTest` aggiornati ai corpi nuovi).
`:app:lintDebug` a zero errori, 67 avvisi come prima (l'anteprima nuova di Cielo non ne
aggiunge). Le larghezze citate sopra sono misurate con Roboto Regular dal layoutlib di
Android Studio (Medium stimato a +3%); il font di sistema del device è un altro, e i
margini tenuti sono di quell'ordine. APK di debug costruito; **la verifica su device è del
committente**.

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
