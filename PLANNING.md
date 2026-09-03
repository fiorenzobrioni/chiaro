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
      prosa IT/EN, illustrata coi componenti veri (i quattro `VerdictChip`)
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
  compare come un fatto su come funzionano le cose (il radar è un'immagine
  distribuita come tale; i dati di Chiaro sono numeri, e la stessa domanda trova
  risposta nella frase e nelle probabilità orarie), mai come un giudizio di valore, e
  senza paragoni con altre app. La guida inoltre non spiega mai un elemento
  dell'interfaccia: un elemento che ha bisogno di spiegazione è un bug (VISION §5.7).
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
- **Nota onesta sull'opacità bassa**: gradiente e scrim scalano insieme, l'inchiostro
  resta pieno; sotto ~40% col vestito Cielo la leggibilità dipende dallo sfondo del
  lettore — è la sua scelta, per quel widget.
- **L'icona dell'app** stringe le distanze: falce giù, onde su, composizione centrata
  nel badge (il primo taglio abbracciava i bordi e lasciava un golfo in mezzo).

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
