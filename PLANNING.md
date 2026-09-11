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

## Interruttore per la frase, la disposizione al contrario, Oggi rifatto, i nomi (committente, 8 set 2026, pomeriggio)

«Entrambi i widget sono ok.» Quattro richieste: la frase visibile o no dalle impostazioni
di Ora; una disposizione alternativa per Ora su una riga («l'icona a destra, e nella prima
riga il testo che lì è nella seconda» — il 4×2 schiacciato in 4×1); una revisione completa
di Oggi, con lo stesso interruttore se ci sta; nomi nuovi nel picker, senza «Chiaro ·».

### La frase, un interruttore (Ora e Oggi)

`WidgetLook.showSentence`, **acceso di default**: lo slot esiste per quello, e chi vuole
il numero nudo lo spegne per singolo widget. Decide solo il **contenuto**: se c'è
**spazio** lo decide ancora la concessione (`NowLayout`, `todayIsWide`), quindi
l'interruttore può togliere la frase, mai forzarla su una card troppo stretta. È il
contrario, un giorno dopo, dell'interruttore «stato accanto alla temperatura» uscito la
mattina: quello era spento di default e decideva il layout. Con la frase spenta la forma
alta di Ora dà le sue due righe al glifo (`nowTallIconSize(withSentence = false)`: 104 dp
sul 2×2, il soffitto).

### La disposizione al contrario (`WidgetArrangement`, solo Ora)

Due valori, per widget: `ICON_START` (la grammatica del launcher, default) e `ICON_END`:
il glifo nell'angolo di coda, e sul lato d'attacco il numero con la frase **alla sua
spalla** — due righe al massimo, centrate sull'altezza del numero, come la descrizione del
vicino sta accanto al suo — e il luogo sotto entrambi. È la composizione della card alta
premuta in una riga, per chi vuole le due sulla stessa home senza che litighino. I due
inset si scambiano di bordo e restano gli stessi numeri: 14 alle parole, 4 al glifo. La
frase compare quando il suo spazio (`nowMirroredSentenceWidth`: la riga meno bordi, glifo,
gap e la colonna del numero, 66 dp) supera lo stesso minimo della colonna standard, 96:
166 dp sul 4×1 del device, 146 su una griglia a cinque colonne, 76 su tre celle — dove
resta a casa, esattamente come dall'altro verso. La card alta ignora la scelta: ha già il
glifo a destra.

### Oggi, rifatto sulla grammatica di Ora (`TodayWidgetLayout.kt`, puro, con tabella)

«Oggi = adesso più le prossime ore» è la definizione di VISION §5.9, e ora la card lo dice
con una grammatica sola: **la testa è la riga larga di Ora** — glifo che riempie la fascia,
temperatura (34 sp, era 36) sopra il luogo, frase contro il bordo opposto, minima e massima
**sotto la frase** quando richieste — e sotto la striscia delle ore.

- **La frase lascia la sua riga** sotto l'eroe e va al bordo opposto, dove Ora la stampa e
  dove chi ha le due card sulla stessa home la cerca già. La riga restituita va al glifo:
  sul 4×2 del device (~340 × 189) passa da **~45 a ~76 dp**, la misura che Ora disegna
  accanto. Registro breve anche qui: la colonna è la stessa di Ora (~113 dp).
- **Il budget** (`todayHeroIconSize`): altezza meno 6 sopra, 14 sotto, la striscia
  (84,84 dp con la riga della pioggia, 70,32 senza) e 8 di stacco; le parole dell'eroe
  (`todayHeroTextHeight`: la colonna più alta fra sinistra — temperatura, luogo, età,
  banda di `textInkBalance` — e destra — frase su due righe, escursione) sono 74 dp e
  stanno nella fascia.
- **La riga della pioggia deve anche starci** (`todayShowRain`), oltre ad avere qualcosa
  da dire: sul 4×2 ci sta (86,8 dp di spazio contro 84,8), ma un marcatore di età prende la
  terza riga alle parole e lo spazio scende a 72: allora la riga resta a casa invece di
  essere tagliata al bordo — la sezione che non ci sta non si disegna.
- **Stretta** (250 dp, il minimo del provider): niente colonna a destra, quindi né frase né
  escursione — la coppia aveva lasciato il posto sotto il numero alla terza passata proprio
  perché lì affollava. La nota dell'interruttore dice ora «al bordo opposto, sotto la
  frase».
- Inset come Ora: 4 al glifo (attacco e alto), 14 alle parole (coda e basso); la striscia
  paga da sé i 10 dp che la portano all'inset delle parole. Anteprima del picker rifatta
  sulla stessa struttura (il vuoto fra riga e striscia è un `FrameLayout`: `Space` non è
  fra le view che RemoteViews gonfia).

### I nomi

| | Prima | Ora |
|---|---|---|
| Ora | «Chiaro · Ora» / «Chiaro · Now» | **«Colpo d'occhio»** / **«At a glance»** — il glance nel senso vecchio della parola, che è la definizione del widget in VISION |
| Oggi | «Chiaro · Oggi» / «Chiaro · Today» | **«Le prossime ore»** / **«The hours ahead»** — quello che la striscia aggiunge all'adesso |
| Cielo | «Chiaro · Cielo» / «Chiaro · Sky» | **«Momenti del cielo»** / **«Sky moments»** — la sezione della schermata Cielo, che è la lista che il widget legge |

Il nome dell'app sta già sopra la lista nel picker, quindi ripeterlo davanti a ognuno non
diceva niente. La guida in app usa gli stessi nomi.

### Decisioni

- **Un solo registro per la frase su tutti i widget**: breve. Oggi stampava la frase intera
  su una riga tutta sua; nella colonna di 113 dp la frase intera («Ombrello verso le
  17:00, schiarisce dopo le 19:00», 330 dp a 16 sp) chiederebbe tre righe. La frase intera è
  nella schermata.
- **`fontScale`, `sentence` e `sentenceStyle`** escono da `NowWidget.kt` come `internal`
  e li usano anche Oggi (tutti e tre) e Cielo (`fontScale`): una frase, un vestito, una
  scala.
- **`DayRange` a 16 sp** (era 15), il corpo del luogo: stesso ordine di fatto, stesso corpo.

### Rimasto aperto

- **Su device**: Ora al contrario a 3×1 e 4×1 (frase alla spalla del numero su due righe,
  luogo sotto); Oggi a 3×2 e 4×2 con e senza pioggia, con l'escursione accesa; la frase
  spenta su entrambi; i tre nomi nel picker.
- **Oggi a 250 dp** perde frase ed escursione insieme: se il committente le vuole anche lì,
  la via è la frase sotto l'eroe a tutta larghezza come prima, pagata dal glifo.

### Verifica

Suite `:app` verde (175 test: 6 nuovi in `TodayWidgetLayoutTest`, 2 in
`NowWidgetLayoutTest` per il glifo senza frase e la disposizione al contrario).
`:app:lintDebug` a zero errori e 60 avvisi, sette meno di prima: l'anteprima di Oggi
rifatta chiude i suoi `RtlSymmetry`. APK di debug costruito; **la verifica su device è
del committente**.

---

## La revisione di Oggi: il terzo gradino delle icone, l'eroe che cresce, lo scroll (committente, 8 set 2026, sera)

Richiesta: una review della schermata principale — layout, disposizione delle sezioni,
icone del meteo «leggermente più grandi ma senza aumentare le spaziature» — e lo scroll
della pagina e della striscia oraria, «leggermente scattoso» da quando le icone si
muovono, da migliorare **senza toccare le animazioni**. Poi: applicare tutto, correggere
la documentazione dove sbagliava, e dire se il bordo del canvas sta meglio dritto o
arrotondato. Nessun device collegato: tutto quello che segue è misurato nel codice, nei
sorgenti di piattaforma e nei file delle icone, e la verifica sul telefono è del
committente.

### Le misure che hanno deciso

- **L'inchiostro nel riquadro.** Prima di ingrandire le icone ho misurato quanto del
  viewport 64×64 dei Meteocons occupa il disegno, statico e lungo tutto il loop
  dell'animazione (campionando i path, stroke incluso, e allargando ogni gruppo animato
  dell'escursione dei suoi keyframe di traslazione; la rotazione è limitata dal punto
  più lontano dal pivot). Le 18 icone della condizione usano il riquadro quando si
  muovono: unione x **6.0–61.6**, y **8.0–60.0**. Le gocce cadono fino a y 60, il banco
  dell'overcast arriva a x 61.6, il sole parte da y 8. Un ritaglio uniforme del viewport
  potrebbe prendere 2.4 unità (sotto il 4%) e sposterebbe i centri ottici: **no**,
  misurato. La nuvola ferma è quello che fa sembrare piccola la famiglia: 30 unità di
  altezza in un riquadro da 64, cioè 20dp di disegno in un'icona da 42.
- **L'eroe a testo grande.** Il canvas era 280dp fissi più status bar; dentro, tutto in
  sp. Spazio libero a 100% di testo: 30dp con la frase su una riga, **2dp su due**,
  −26dp su tre. La frase più lunga in italiano fa 49 caratteri con le due ore e su 360dp
  va su due righe: la pagina era al limite già a 100%, e a 115% eroe e riga del luogo si
  sovrapponevano di 30dp.
- **Dove va il tempo dello scroll**, verificato nei sorgenti e non supposto:
  - AOSP `AnimatedVectorDrawable`: il costruttore privato copia già in profondità
    l'albero del vettore per ogni istanza (`AnimatedVectorDrawableState(copy, …)` fa
    `newDrawable` + `mutate` sul `VectorDrawable` interno), quindi il `mutate()` in
    `ConditionIcon` era una **seconda** copia buttata via; l'animatore è
    `VectorDrawableAnimatorRT` e gira sul RenderThread finché il canvas è accelerato;
    `setVisible(false)` mette in pausa i loop infiniti.
  - Compose `AndroidViewHolder`: `onGloballyPositioned` richiama `View.layout()` a ogni
    spostamento, cioè a ogni frame di scroll, per ogni icona interop.
    `LazyListMeasuredItem` piazza ogni item su un layer, quindi senza interop uno scroll
    sposta i layer senza ridisegnarli.
  - Compose `animation-graphics`: il parser legge `repeatCount="infinite"` e costruisce
    un `repeatable` infinito. **La motivazione scritta il 7 set era sbagliata**: non è
    che `AnimatedImageVector` non sappia fare un loop, è che lo fa ricomponendo il
    vettore a ogni frame e rasterizzando su CPU, sul thread che serve allo scroll. La
    scelta dell'`ImageView` resta giusta per questo motivo, ed è quello che DESIGN §7.1
    e il KDoc dicono adesso.

### Decisioni

1. **Icone: terzo gradino, +4dp su ogni piolo** — striscia **42**, settimana **38**,
   timeline **34**, tile **34**. Nessuna spaziatura, nessuna colonna, nessun
   arrangement cambia. Cosa paga ciascuno, a 360dp: la striscia **niente** (l'icona si
   prende i 2dp di padding verticale in cui stava, la cella resta 112dp e l'aria laterale
   passa da 9 a 7dp); la settimana 4dp di altezza per riga e la barra 106→102dp; la
   timeline 4dp per riga e la prosa 226→222dp; il tile 4dp di testata e il budget
   dell'etichetta 88→84dp contro i 76.7 di «Qualità aria» — 7.3dp di margine, **fine
   della scala** per quel piolo. Scartata la scala a tempo di disegno (`scale(1.1)`
   nello stesso riquadro): per la striscia è la stessa cosa scritta peggio, per le altre
   righe fa sbordare l'inchiostro di 1–2dp nei gap.
2. **Il canvas è un pavimento, non una misura**: almeno 280dp più status bar, e cresce
   col testo. Riga del luogo ed eroe sono i due capi di una `Column` con `SpaceBetween`
   su `heightIn(min)`, non due figli allineati ai bordi opposti di una scatola fissa. Il
   flip delle icone della status bar si misura sull'altezza reale dell'item del canvas
   (`layoutInfo`), perché la fascia di scrim è una frazione e la frazione ora si muove.
3. **Bordo dritto, confermato.** Il committente ha chiesto un parere: gli angoli
   arrotondati facevano leggere il cielo come una card che galleggia sullo scroll, ed è
   il motivo per cui erano stati tolti il 4 set. Sulla pagina ogni altra superficie è
   rientrata di 16dp e arrotondata; l'unica che non lo è dev'essere il terreno su cui la
   pagina si apre, non un'altra card. Dritto.
4. **Skeleton rimisurato sul layout vero**: il blocco del cielo aveva ancora gli angoli
   da 28dp persi il 4 set e i 240dp di prima che il canvas arrivasse alla status bar; le
   celle erano 96dp contro i 112 della striscia. Ora il blocco è `280 − (8 + 48 + 8)` =
   216dp a bordo dritto, così riga del luogo più blocco finiscono dove finisce il canvas
   vero; sei celle da 56×112 a 4dp l'una dall'altra.
5. **La riga del luogo a 8dp ovunque**: era 8 sul cielo e 12 sugli stati piatti, quindi
   sfogliando da un luogo con report a uno senza il titolo saltava di 4dp.
6. **La striscia scorre a filo schermo**: il margine di pagina entra come
   `contentPadding` della `LazyRow` invece di stare sulla colonna che la contiene, così
   la prima cella parte sui 16dp come tutto il resto e le altre scivolano sotto il bordo
   dello schermo invece di essere tagliate su una linea 16dp più dentro, che faceva
   leggere la striscia come una scatola. Vale anche per la striscia del giorno aperto.
7. **Lo scroll, quattro interventi che non toccano un disegno né un animatore:**
   - via il `mutate()` (una copia dell'albero per cella invece di due);
   - la cella riciclata **tiene il drawable**: `onReset` ferma solo il loop, `update` lo
     riavvia se la risorsa è la stessa, `onRelease` lo rilascia. Vive in una
     `MovingIconView` con il proprio flag `resting`, perché il RenderThread riferisce la
     fine del loop al thread UI uno o due frame dopo lo `stop()`, e un riuso arrivato in
     quella finestra avrebbe trovato `isRunning` vero e lasciato l'icona ferma;
   - le celle hanno una **chiave** (`HourCell.key`, l'ora): allo scoccare dell'ora esce
     solo la prima cella, invece di rigonfiare tutte le icone visibili nello stesso
     frame;
   - la settimana è **sette item** invece di uno: stesso ritmo (la colonna spaziava di
     12dp e la lista spazia di 12dp), ma il prefetch prende una riga alla volta invece
     di sette icone nel frame in cui la sezione entra. Lo stato del giorno aperto è
     salito in `ContentState`.
   Quello che resta è l'animazione stessa: ~14 cache vettoriali rirasterizzate sul
   RenderThread a ogni vsync. È il prezzo della funzione e l'unica leva è quante icone si
   muovono insieme; si misura con un A/B, `adb shell settings put global
   animator_duration_scale 0` spegne le icone (già leggono quel valore) e `dumpsys
   gfxinfo` confronta i frame persi.
8. **Dettagli in fondo**, per decisione del committente. La revisione grafica delle card
   è un parere dato a parte, non un intervento.

### Rimasto aperto (su device)

- Striscia e pagina: gli scatti prima e dopo, con l'A/B sopra.
- L'eroe a 130% di testo e con una frase su due righe: la riga del luogo non deve più
  essere raggiunta.
- Skeleton contro canvas vero: nessun salto quando arriva il report.
- La settimana: aprire e chiudere un giorno ora che ogni riga è un item, e il ritmo dei
  12dp tra le righe.
- Le icone a 42/38/34: se «leggermente più grandi» è questo.

### Verifica

Suite `:app` verde (175 test, invariati: nessuna regola pura nuova, i cambi sono di
layout e di interop). `:app:lintDebug` a zero errori e 60 avvisi, come prima. APK di
debug costruito; **la verifica su device è del committente**.

---

## Le card dei dettagli rifatte, e la review di Cielo (committente, 8 set 2026, notte)

Richiesta, subito dopo: applicare tutti i consigli dati sulle card dei dettagli; fare la
stessa review a **Cielo** — layout, dimensione delle icone «o comunque vedi tu», logica,
dati e messaggi — e pushare su un branch nuovo perché parta la CI.

### Le card, com'erano e come sono

Il problema era di gerarchia: valore a 16sp contro un'etichetta a 14 e un'icona da 34dp,
l'occhio andava sull'icona e non sulla lettura. Da qui:

- **Il valore è una lettura**: `ReadingValue` in `Type.kt`, Inter Light 24sp su 32 di
  interlinea, tabulare — la voce dell'eroe alla scala di una card, con lo stesso
  argomento del 64sp: un numero in peso da corpo legge come un titolo, non come una
  lettura. Costa 8dp per riga di card.
- **Una traccia dove la scala è del mondo** (`QuantityTrack`): 4dp, `outlineVariant`
  con il tratto in `primary` fino al valore, mai più stretto di quanto è alto. Solo per
  UV (0–11: sopra il tracciato è pieno e parla la frase), Umidità (0–100) e Qualità aria
  (0–300, la soglia «pericolosa» dell'AQI statunitense che il provider serve). Niente per
  pressione e visibilità: una è una banda stretta intorno a 1013, l'altra è logaritmica,
  e una traccia lì sarebbe una forma senza niente da dire.
- **Vento**: il valore è la sola velocità. Sotto, una freccia disegnata (`WindArrow`,
  non un'icona Material: l'unica freccia del set base è auto-specchiata in RTL e una
  direzione della bussola non deve esserlo) che indica **dove va l'aria**, e le parole
  «da nord-est» per **da dove viene** — le stesse otto parole che Cielo usa per un
  arcobaleno (`SkyText.bearingRes`), perché un pubblico generale legge «da nord-est» e
  decodifica «NNE». La convenzione è quella delle mappe: la freccia è movimento, la
  didascalia è la sorgente. La sigla a sedici punti del modello, che in italiano
  stampava «W», resta nei dati. **Le raffiche** hanno una riga quando contano
  (`WeatherText.gustsMaterial`: almeno 25 km/h e almeno una volta e mezza il vento) e in
  quei giorni la frase si legge sulla raffica, perché «si sente, non dà fastidio» sopra
  «raffiche a 45 km/h» è la card che litiga con sé stessa.
- **Umidità e Rugiada sono una card.** Dicevano la stessa cosa una sotto l'altra
  («Confortevole» / «Gradevole»), e «Rugiada» è la parola meno capita dello schermo. La
  card Umidità tiene la percentuale e la traccia, prende la frase dal punto di rugiada
  (il predittore migliore di come si sta) e stampa «Rugiada 12°» come nota: il dato non
  si perde, il titolo sì. Via `metric_dew` e le quattro `humidity_meaning_*`; il campione
  della Guida usa la frase della rugiada.
- **Qualità aria** senza «AQI»: era l'unica sigla di uno schermo fatto per non averne, e
  l'etichetta dice già cos'è il numero. Via `metric_air_value`.
- **Pollini**: la nota dice **quali** («Graminacee e alberi»), dalle tre famiglie del
  modello al livello peggiore, in ordine di catalogo; niente riga quando sono assenti.
  Le famiglie stanno in risorsa minuscole per entrare in un elenco (`list_two`,
  `list_three`, localizzati) e la card mette la maiuscola alla prima.

**Le larghezze, misurate a 360dp**: l'interno di una card è `(360 − 32 − 12) / 2 − 32` =
126dp. «↙ da nord-ovest» ≈ 120dp, sta. «Raffiche a 45 km/h» ≈ 131dp può andare a capo a
360 e sta su una riga da 393dp in su (142dp), che è la larghezza dei telefoni di oggi;
«Raffiche fino a» era scartata per questo. «Graminacee, alberi e erbe» va a capo, ed è il
caso raro in cui tutte e tre le famiglie sono allo stesso livello. Una card più alta
allarga la sua compagna di riga, com'era già (`IntrinsicSize.Max`).

### Cielo: la review

**Layout.** La struttura regge — l'eroe, i momenti, l'agenda, i promemoria — e il chip
del verdetto sotto il nome (device, 3 set) resta la scelta giusta. Due cose cambiate:

- **Le icone erano silhouette**: 26dp tinte `onSurfaceVariant` nei momenti e negli
  eventi, nell'indice della guida e nella testata della pagina (36dp), 18dp con la tinta
  del chip nei correlati. Era l'ultimo posto in cui la famiglia era tinta piatta, e
  tinta piatta la luna piena e la luna nuova sono lo stesso disco, alba e tramonto lo
  stesso orizzonte. Ora colori propri (§13.1) e la scala di Oggi: **34dp** (il piolo
  della timeline) nelle righe di momenti, eventi e indice, **42dp** (il piolo della
  striscia) come unico glifo della pagina di un evento, **24dp** nei chip correlati, dove
  a 18 un Meteocon erano 13dp di inchiostro.
- **I segni di spunta del catalogo** venivano ricostruiti dalle righe a schermo
  (momenti più eventi con campanella): un job iscritto senza riga — una ricerca di
  eclissi che non trova niente davanti — risultava non iscritto e si offriva di essere
  aggiunto di nuovo. `Content.subscribedIds` arriva dallo store.

**Logica e messaggi, verificati e lasciati com'erano:**

- La card Stanotte stampa la riga della luna al posto del numero delle nuvole solo
  quando `moonPct` c'è, e il motore lo mette solo quando la luna ha deciso il verdetto
  (`SkyVerdictNote.MOONLIGHT`): la riga della luna **è** l'aritmetica, non la nasconde.
- «Domani» sui momenti giornalieri è sempre esatto: `SkyScheduler.next` riporta i giorni
  `∅` invece di saltarli, quindi il prossimo è sempre quello di domani, con o senza
  motivo.
- Il formato degli eventi («12 agosto · La previsione non arriva ancora così lontano»)
  va a capo sotto il nome e legge bene; il giorno della settimana non serve a mesi di
  distanza.
- Cielo non aggiunge l'inset della barra di navigazione in fondo alla lista, Oggi sì:
  la `NavigationBar` sta sotto entrambe e consuma l'inset da sé, quindi Oggi ha 24–48dp
  di respiro in più prima della barra. Lasciato: non è un errore in nessuno dei due.

**Trovato e non corretto, perché sta nel core** (UPSTREAM: si corregge a monte):
`SkyScheduler.darkness()` risponde `NO_DARKNESS` anche quando il sole resta sotto i −18°
per tutto il giorno — notte polare, crepuscolo astronomico senza inizio né fine — e la
card Stanotte direbbe «il cielo non diventa mai del tutto buio» a Longyearbyen in
dicembre, che è il contrario. Serve un motivo distinto («buio tutto il giorno») nel
motore e la card che lo stampa. Fuori dalle latitudini del pubblico dell'app; registrato.

### Verifica

Suite `:app` verde (175 test). `:app:lintDebug` a zero errori e 60 avvisi, come prima.
APK di debug costruito; il branch `claude/today-details-sky-review-k4p7wz` porta tutto
alla CI; **la verifica su device è del committente**: le card con e senza raffiche, la
striscia a filo, l'eroe a testo grande, Cielo con le icone a colori.

---

## La notte polare corretta, e la review di Avvisi e Diario (committente, 8 set 2026, tarda sera)

APK della CI provato: «ottimo». Richiesta: correggere la notte polare trovata nella
review di Cielo; poi la stessa passata, funzionale ed estetica, sulle ultime due
schermate — Avvisi e Diario — con particolare attenzione al funzionamento del Diario e
alle correzioni da proporre.

### La notte polare: corretta nel core, e registrata in UPSTREAM

`SkyScheduler.darkness()` rispondeva `NO_DARKNESS` per due cieli opposti: il sole che non
scende mai 18° sotto l'orizzonte (la notte bianca) e il sole che non **risale** mai fino a
18° sotto (la notte polare profonda, dove è buio a mezzogiorno). La card Stanotte diceva
«il cielo non diventa mai del tutto buio» anche sul secondo, che è il contrario. Prima di
correggere, il conto di dove capita: il sole resta sotto i −18° tutto il giorno solo se a
mezzogiorno solare `90 − |φ − δ| < −18`, cioè oltre **84,6°** di latitudine al solstizio
— nessun paese abitato, le stazioni polari sì. Corretto comunque, perché è il motore che
chiama una notte polare con il nome di una notte bianca:

- nuovo motivo `SkyNotScheduled.DARK_ALL_DAY`; `darkness()` lo restituisce quando il
  giorno è senza sole (`sunDownAllDay`) **e** l'altezza del sole al mezzogiorno solare è
  sotto il crepuscolo astronomico. Il giorno di confine con un solo estremo nullo resta
  `NO_DARKNESS`, che è la risposta giusta anche lì;
- la Via Lattea propaga il motivo della finestra invece di riscriverlo `NO_DARKNESS`;
- `Tonight.reason` porta il motivo alla card, che stampa la frase giusta
  (`sky_tonight_dark_all_day`); le righe `∅` hanno la loro (`sky_none_dark_all_day`);
- `SkySchedulerTest`: a −89,5° al solstizio di giugno la finestra è `DARK_ALL_DAY`,
  Copenaghen lo stesso giorno resta `NO_DARKNESS`;
- **UPSTREAM.md**: è un bug anche a monte — `sky.crontab` stampa lo stesso `∅` — e la
  voce dice che la correzione va portata di là (motivo, ramo, test).

### Avvisi: la review

**Funziona.** Interruttori pronti con la frase di cosa mandano e quando; regole del
lettore come frase di chip con picker e mai un campo libero per un valore con un
intervallo; «Prova adesso» che risponde senza notificare e si azzera quando una
condizione cambia; permesso chiesto al primo interruttore acceso; cancellazione
confermata; il template crea una regola vera e apre subito l'editor. Tutto coerente con
VISION §5.4. Da correggere o migliorare, in ordine:

1. **«Scattato l'ultima volta» è nel fuso del telefono** (`ZoneId.systemDefault()`),
   l'unica ora dell'app che non è del luogo. Per il proprio luogo coincidono; per
   Palermo letta da Reykjavík no. Portare `zone` in `AlertsUiState.Content`.
2. **Il picker degli operatori offre «uguale a» e «diverso da» sulle grandezze
   continue**: «temperatura uguale a 20°» non scatta quasi mai ed è la soglia senza
   senso che i picker esistono per rendere non scrivibile. Nascondere i due per
   `NUMBER`, `TEMPERATURE`, `SPEED`; restano per i sì/no.
3. **Un template si può aggiungere due volte** e produce due regole «Bici» identiche.
   Un template le cui condizioni esistono già tra le regole va segnato come aggiunto
   (o nascosto), come fa il catalogo di Cielo con la spunta.
4. **Al massimo delle regole i template spariscono senza una parola**: una riga «Hai già
   il massimo di N avvisi: togline uno per aggiungerne un altro» dove stavano.
5. **Estetica**: le regole del lettore sono `ListItem` identici agli interruttori pronti,
   e il titolo di gruppo è l'unica cosa che le distingue. VISION le chiama *card*: una
   `Surface` `surfaceContainer` come le card dei dettagli — nome, frase, ultimo scatto,
   interruttore — separa i due gruppi a colpo d'occhio e dice che la card si apre, cosa
   che una riga con interruttore non dice.
6. **La risposta di «Prova adesso»** è tutta in `onSurfaceVariant`: quando scatterebbe è
   la risposta che il lettore cercava e merita l'inchiostro pieno; «resterebbe zitto»
   può restare quieto.

### Diario: la review

**Verificato leggendo il costruttore e i due motori** (`ForecastDiff`, `ForecastOutcome`):
le revisioni diventano righe solo con una baseline (un giorno che entra nell'orizzonte
non è una revisione); il giudizio lo dà la pioggia e la temperatura resta neutra; l'esito
di una giornata finita si dichiara «ha piovuto» con una sola osservazione bagnata e «non
ha piovuto» solo con 16 ore coperte; la deriva ha l'asse del tempo a fasce di 6 ore, il
buco resta buco, la testata parte dove parte l'evidenza e il gelo segna il giorno
sull'ultima previsione; i giorni si raggruppano nel fuso del luogo e la schermata si
sveglia solo alla sua mezzanotte. Nessun errore di logica trovato. Le proposte, in
ordine di resa:

1. **Comprimere le revisioni per giorno bersaglio.** Ogni fetch che sposta la massima di
   un giorno di 1 °C o la pioggia di 10 punti fa una riga; a cadenza oraria e su sette
   giorni bersaglio questo può essere una decina o più di righe al giorno dello stesso
   tenore («la massima di venerdì è passata da 24° a 25°»), e il diario smette di essere
   leggibile. Proposta: nel costruttore, per ogni coppia (giorno del diario, giorno
   bersaglio) una riga sola con primo → ultimo valore e «in N aggiornamenti», e nessuna
   riga quando il valore è tornato dov'era — la frase della deriva già ragiona così
   («è andato su e giù»), la prosa no. Il Diario di Oggi («Cosa è cambiato») resta
   sull'ultimo fetch com'è.
2. **Oggi nella deriva.** Le righe sono i giorni «ancora avanti» e oggi è escluso, ma «la
   pioggia di oggi è salita nelle ultime ore?» è la domanda del mattino. Includere oggi
   come prima riga finché la giornata corre.
3. **La copertura dell'esito.** Ogni osservazione copre l'ora precedente, fissa: a
   cadenza di 2 ore (il massimo dell'intervallo) una giornata copre al più 12 ore e il
   verdetto «non ha piovuto» non arriva **mai**; a cadenza oraria basta una notte in
   Doze per scendere sotto le 16. Proposta, in `ForecastOutcome`: ogni osservazione copre
   il tempo dal fetch precedente, con un tetto (3 ore), così la copertura misura quanto
   l'app ha davvero guardato e non quante volte. Da verificare su device prima: se «Ieri»
   riceve la sua riga di esito alla cadenza del committente.
4. **La tabella dei numeri** unisce quattordici valori con frecce in una riga di dialogo
   che va a capo male. Una griglia con l'ora della fascia in testa, o le sole fasce con
   un valore.
5. **Le icone delle righe**: nuvola e stella sono Meteocons tinti in grigio a 24dp, la
   stessa silhouette tolta a Cielo. Qui però il glifo è la **categoria** della riga, non
   il tempo: la scelta coerente è un set Material monocromo per tutte e cinque le
   categorie (revisione, cielo osservato, avviso scattato, esito, aggiornamento
   mancato), come campanella, spunta e triangolo già sono. La regola §13.1 riguarda le
   icone del meteo, e queste non lo sarebbero più.
6. **L'ora come etichetta in coda** alla riga (`trailingContent`, `labelSmall`) invece
   che «· alle 21:54» in fondo alla frase: un registro si scorre per ora.
7. **La deriva in una card** `surfaceContainer` con i chip dentro: cinque elementi
   impilati (titolo, chip, striscia, gelo, frase) diventano un oggetto.

Nessuna di queste è implementata: sono proposte, con il numero 1 e il numero 3 come le
due che cambiano quello che il Diario dice.

### Verifica

`:core:domain` verde (166 test, uno nuovo), `:core:data` verde (171), `:app` verde (175).
`:app:lintDebug` a zero errori e 60 avvisi. APK di debug costruito. La correzione è nel
working tree del branch della CI, **non ancora pushata**: la CI riparte quando il
committente lo dice.

---

## Avvisi e Diario: le proposte applicate (committente, 9 set 2026, notte)

Richiesta: applicare tutte le correzioni e le proposte della review di Avvisi e Diario;
sullo scatto residuo dello scroll di Oggi solo una risposta, senza codice; poi push
perché riparta la CI.

### Avvisi

1. **«Scattato l'ultima volta» nel fuso del luogo**: `AlertsUiState.Content.zone`, letto
   dalla città attiva con il fallback del telefono, e la card lo usa.
2. **Niente «uguale a» / «diverso da» sulle grandezze continue**: il picker li offre
   solo ai sì/no. Una regola che già li porta li tiene; il picker smette di proporli.
3. **Un template già presente è segnato** con la spunta in `primary` e non è più
   toccabile: il confronto è sulle condizioni (`RuleCondition` è una data class), così
   vale anche se il lettore ha rinominato la regola. Stringa `tpl_already_added`.
4. **Al massimo delle regole** una riga lo dice (`alerts_max_reached`, con `MaxRules`),
   dove prima i template sparivano e basta.
5. **Le regole sono card**: `Surface` `surfaceContainer` con angolo `medium`, nome in
   `titleMedium`, frase, ultimo scatto in `bodySmall` grigio, interruttore a destra;
   tutta la card apre l'editor, l'interruttore resta il suo bersaglio. Item con chiave
   sull'id della regola, così un toggle anima la riga invece di ricostruirla.
6. **La risposta di «Prova adesso»** è in `onSurface` quando scatterebbe, quieta negli
   altri tre casi.

### Diario

1. **Le revisioni si piegano** per coppia (giorno del diario, giorno bersaglio):
   `JournalStateBuilder.forecastShifts(rows, zone)` raggruppa le revisioni per fetch
   (`revisions`, che resta quello che legge «Cosa è cambiato» su Oggi) e per ogni campo
   tiene il primo valore vecchio e l'ultimo nuovo; un campo tornato dov'era non è un
   cambiamento (confronto numerico dove i valori sono numeri), un giorno i cui campi
   sono tutti tornati non ha riga. `ForecastShift.revisions` dice quante ne raccoglie e
   la riga lo stampa oltre uno (`journal_shift_revisions`, plurale). Due test nuovi in
   `JournalStateBuilderTest`: la piega su tre fetch, e il ritorno che cancella.
2. **Oggi nella deriva.** Le righe sono `!isBefore(today)`. Ma il giorno corrente non
   era su disco: `WeatherSnapshots.flattenForecast` salvava da domani a sette giorni,
   ora salva **da oggi** (`0L..7L`). È una divergenza in più dal core di tweather ed è
   in UPSTREAM con la sua cucitura: `ForecastDiff.dayLabel` chiama «tomorrow» la prima
   data salvata, e quell'etichetta è dei Logs di tweather, niente qui la legge.
   `WeatherSnapshotsTest` aggiornato. Effetto collaterale voluto: anche «Cosa è
   cambiato» su Oggi può ora dire che la previsione di oggi si è mossa.
3. **La copertura dell'esito** (`ForecastOutcome`): una lettura copre il tempo dalla
   lettura precedente, con un tetto di **due ore** — la cadenza massima dell'app — e la
   prima lettura copre la sua ora. Prima ogni lettura copriva un'ora fissa, quindi a
   cadenza di due ore una giornata guardata da cima a fondo copriva dodici ore e il
   verdetto «non ha piovuto» non arrivava mai. La pioggia resta sull'ora che i
   millimetri descrivono, non sulla finestra di copertura: una lettura bagnata all'1:30
   che copre fino a ieri non bagna ieri. Tre test nuovi in `ForecastOutcomeTest` e uno
   adattato (tre letture a 9, 10 e 16: quattro ore coperte, non tre). Il tetto è una
   scelta: due ore è quanto l'app stessa promette di guardare, sei ore di sonno ne
   coprono due. In UPSTREAM.
4. **La tabella dei numeri è una griglia**: intestazione a due righe (giorno e ora
   dello slot, del fetch se c'è, nominale se lo slot è vuoto), una riga per giorno
   bersaglio, cifre tabulari, scorrimento laterale quando è più larga del dialogo.
5. **Icone monocrome Material** per le cinque categorie: matita per la revisione,
   stella per il cielo osservato, campanella, spunta, triangolo. Nuvola e stella
   Meteocons tinte grigie erano l'ultima silhouette rimasta; qui il glifo è la categoria
   e non il tempo, quindi §13.1 non lo raggiunge.
6. **L'ora in coda alla riga** come `labelSmall` tabulare; l'esito, che è del giorno,
   non ne ha. Via «· alle 21:54» dalle frasi e via la stringa `journal_at_time`.
7. **La deriva in una card** `surfaceContainer`: chip, striscia, gelo e frase in una
   `Column` con 16dp di margine e 12 di passo; la striscia, il gelo e la frase perdono i
   margini propri. Il titolo di sezione resta fuori.

DESIGN §8.10 e la guida («una riga per oggi e per ogni giorno davanti») aggiornati.

### Lo scatto residuo di Oggi, solo una risposta

Quello che resta è del RenderThread: ~14 vettori animati rirasterizzati a ogni vsync
mentre lo stesso thread muove i layer dello scroll. L'unica leva che non tocca né i
disegni né i loro animatori è **fermare i loop mentre la lista è in movimento**
(`isScrollInProgress` → `setVisible(false)` sul drawable, che mette in pausa un AVD
infinito; ripresa da dove era a lista ferma). Costa zero da ferma e toglie tutto il
lavoro vettoriale dai frame dello scroll; il prezzo è che le icone si congelano durante
un trascinamento lento. Da misurare prima con l'A/B `animator_duration_scale 0`. Non
applicato: il committente ha chiesto solo la risposta.

### Verifica

`:core:domain` verde (166), `:core:data` verde (174, tre nuovi), `:app` verde (177, due
nuovi). `:app:lintDebug` a zero errori e 61 avvisi (uno in più). APK di debug costruito e
branch pushato: la CI produce quello da provare.

---

## Il meteo sta fermo mentre la pagina si muove (committente, 9 set 2026)

APK provato: «tutto ok». Richiesta: provare la soluzione proposta per lo scatto residuo
di Oggi, pushare e aprire la PR.

### Quello che AOSP ha detto prima di scrivere

La proposta era `setVisible(false)` sul drawable durante lo scroll, perché
`AnimatedVectorDrawable.setVisible` mette in pausa un loop infinito. Letto il sorgente
prima di fidarsi: `VectorDrawableAnimatorRT.pause()` e `resume()` sono **due TODO** in
AOSP. Sul RenderThread la pausa non esiste, e con essa cade anche una frase che DESIGN
§7.1 e il KDoc ripetevano dal 7 set — «la piattaforma mette in pausa l'animatore quando
la view non è più visibile». Non è vero: quello che ferma il lavoro è **non essere
disegnati**, perché hwui non prepara un nodo fuori dalla display list e non ne fa girare
gli animatori. Il risultato pratico era comunque giusto (una cella fuori schermo non
costa); la spiegazione no, ed è corretta in entrambi i posti.

Neanche `stop()` va bene: porta il disegno al fotogramma finale del loop, che per la
pioggia è quello **senza gocce**, e `start()` alla ripresa rifarebbe partire tutto dalla
nuvola.

### Cosa fa adesso

- `LocalMotionPaused`, in `ConditionIcon.kt`: vero finché uno scroll è in corso. Lo
  forniscono la lista di Oggi (`listState.isScrollInProgress`), la riga della striscia
  (`rowState`, in OR con quello che la pagina già dice) e il pager tra i luoghi
  (`pagerState.isScrollInProgress`).
- Un'icona animata è **due cose in una scatola**: il disegno fermo e il gemello animato
  sopra. Sono composti sempre entrambi — comporre quattordici painter al primo frame di
  uno scroll sarebbe lo scatto che si vuole togliere — e il locale decide chi si vede: il
  fermo ad alpha 1 con la `ImageView` `INVISIBLE` mentre la pagina si muove, il gemello
  sopra un fermo trasparente da ferma.
- Una View `INVISIBLE` il padre non la disegna, il suo nodo esce dalla display list e
  hwui non ne tocca gli animatori: il RenderThread spende i frame dello scroll sui layer
  e non sui vettori. Alla ripresa il loop è dove lo mette l'orologio — gli animatori
  vanno a tempo di frame — non dove era rimasto: niente replay.
- Il prezzo visibile: un cambio di posa alle due estremità di uno scroll, il disegno
  animato scatta nella posa ferma quando il dito si muove e torna quando si ferma. È il
  compromesso già detto nella risposta di ieri, con «fermo nella posa da fermo» al posto
  di «congelato».

DESIGN §7.1 e il KDoc di `ConditionIcon` corretti sulla pausa e aggiornati sul nuovo
comportamento.

### Rimasto aperto (su device)

- Lo scatto è sparito, o ridotto? Se resta identico, il collo non era il RenderThread e
  l'A/B `animator_duration_scale 0` resta il modo di dirlo.
- Il cambio di posa a inizio e fine scroll: se disturba più dello scatto, si può limitare
  al solo fling (drag in corso → `interactionSource` della lista) o togliere del tutto.

### Verifica

Suite verdi; lint invariato; APK di debug costruito; PR aperta sul branch.

---

## La frase anticipa (committente, 9 set 2026)

Osservazione dal device, sul widget: «Pioggia fino alle 18:00 circa» va bene, ma se non
sta piovendo e potrebbe piovere tra qualche ora, o oggi, o domani, la frase non lo dice;
e in generale sarebbe bello che anticipasse quello che sta per accadere e merita
attenzione, non solo la pioggia. Chiesto di verificare, e nel caso implementare.

### Verificato: era così

`HeadlineEngine` aveva tre gradini — un codice severo entro 12 ore, pioggia adesso,
pioggia **entro 6 ore** sopra la soglia della notifica (70%) — e poi silenzio. Quindi un
mattino asciutto prima di una sera bagnata, un giorno asciutto prima di un domani
bagnato, un pomeriggio al 60%, una notte di gelo: niente. La frase era nata come sorella
della notifica (stesse soglie, stesso orizzonte), e la notifica ha ragione a guardare
sei ore; la frase in cima alla pagina no.

### La scala, primo che combacia vince

1. Codice severo entro 12 ore (com'era).
2. Pioggia adesso, con quando smette (com'era).
3. **Pioggia probabile oggi** (≥ 70%): l'orizzonte è il resto del giorno locale, e mai
   meno delle sei ore del notificatore — alle 22:00 la pioggia delle 03:00 è ancora di
   stanotte, e la frase la dice come «Ombrello verso le 03:00».
4. **Gelo entro domattina**: l'ora più fredda da adesso alle 10:00 di domani, se è a zero
   o sotto (la soglia della striscia di deriva) e **adesso non gela**. Solo
   anticipazione: un pomeriggio a −2° è già il numero dell'eroe, ripeterlo sarebbe la
   pagina che dice due volte la stessa cosa.
5. **Nebbia in arrivo** entro 12 ore, solo se adesso non c'è: «Nebbia» adesso è già la
   parola della condizione sotto il numero.
6. **Vento forte adesso**: sostenuto ≥ 39 km/h (la fascia «tieni il cappello» del tile)
   o raffiche ≥ 60. L'unico «adesso» che la frase porta, perché l'eroe non ha il vento e
   il modello non ha il vento orario: qui non può guardare avanti come gli altri.
7. **Pioggia possibile oggi**: almeno il 50%, sotto la soglia dell'ombrello. Il 50 è la
   soglia che il motore già aveva per «il cielo ha smesso di promettere pioggia», letta
   al contrario. Registro onesto: «Possibile pioggia verso le 17:00».
8. **Pioggia probabile domani**: la prima ora di domani sopra il 70%. «Domani ombrello
   dalle 09:00».
9. Niente: resta la risposta di un giorno senza niente di tutto questo.

Cosa NON c'è, e perché: il caldo e l'UV alto sparerebbero ogni giorno d'estate e la
frase smetterebbe di significare qualcosa; il vento di domani non è nel modello.

### Il testo

Nuove parole in entrambe le lingue, nel registro pieno e in quello breve del widget:
possibile pioggia/neve, domani ombrello/neve, gelo con la minima (breve senza), nebbia,
vento forte fino a X (breve «Vento fino a X»). Gelo e vento stampano un numero con la
sua unità, quindi `HeadlineText.of` prende `UnitSettings`; i due widget lo passano dal
loro modello. La guida («La frase») elenca i nuovi esempi e dice che la frase anticipa.

### Verifica

`HeadlineEngineTest` riscritto come scala: 19 casi (erano 10) — ogni gradino con la sua
frase e con chi vince su chi, il test «oltre sei ore non è ancora notizia» diventato
«più tardi oggi è l'ombrello», la mezzanotte con e senza le sei ore, il gelo che tace se
gela già o se arriva dopo le dieci, la nebbia che tace se c'è già, le tre soglie del
vento. Suite `:app` verde (186). Lint a zero errori e 61 avvisi, come prima. APK di debug
costruito; branch `claude/headline-looks-ahead-p2x7mn` e PR per la CI: **la verifica su
device è del committente**, in particolare il widget «Ora» con le frasi nuove nel suo
registro breve.

---

## La palette che non si vedeva: la nota che mentiva, la notte smorta (committente, 9 set 2026)

Due screenshot dello stesso tramonto di Londra, uno con Carta e uno con Brillante:
canvas identico, icone identiche. La domanda era se la palette funzionasse.

### Verificato: funziona, ed è la prova che serviva

Misurati i pixel dei due screenshot contro le tabelle in `Scheme.kt` e `SkyPalette.kt`.
Superficie `#0D141C` contro `#16130E`, pill della navigazione `#624000` contro
`#004C70`, testo `#B9C8DF` contro `#CCC6BA`: sono `VividDarkScheme` e `ChiaroDarkScheme`
esatti. La ribbon cambia dove deve: banda del giorno `#2BB8FF` contro `#54B7F0`, ora blu
`#2241B5` contro `#354A89`. La palette arriva ovunque. Nessun bug di wiring.

Il canvas era identico **per aritmetica, non per un guasto**. Londra alle 19:33, sole a
circa 0°, copertura 100%: agli anchor di 4° e 0° i due stop centrale e basso sono gli
stessi esadecimali in entrambe le tabelle (`#F49C04`, `#FFD083`, `#E58800`, `#FFC268`),
perché `gen_vivid.py` tocca il bordo del gamut prima di arrivare a ×1.8 — lo stesso
fatto che §2.5 registra già per gli ambra. Resta solo lo stop alto, e il 100% di nuvole
gli toglie il 70% di quel che gli era rimasto (`CloudDesaturation`), sotto lo scrim
superiore. Calcolato: paper `#535F6E #917A57 #B8A992`, vivid `#395571 #917A57 #B8A992`.
Due stop su tre byte per byte.

### Le icone: la nota mentiva davvero

`ChiaroIcons.styledRes` legge la palette solo sul ramo `style != FILL`. Il set pieno è
la tavolozza di Meteocons e non ha un fratello vivid (§13.1, ed è voluto). Lo screenshot
era sul set pieno — luna campionata `#86C2DA`, cioè `mcfn_clear_night`. Quindi la nota
delle impostazioni, «con icone del meteo più vivaci», prometteva a quel lettore una cosa
che non sarebbe successa. Contro la regola che lo schermo non mente.

`settings_palette_note` ora dice solo quello che vale sempre, e `paletteNote()` gli
appende la frase giusta per il set icone attivo: a linea, «Brillante le schiarisce anche
sul tema scuro»; piene, «tengono i colori loro, quindi la palette non le tocca».

### Il pavimento del cielo: perché Brillante era smorto di notte

Misurata ogni banda come frazione della cromia che sRGB regge alla sua luminanza:

| | giorno | 8° | ora d'oro | ora blu | −12° | −18° | notte |
|---|---|---|---|---|---|---|---|
| paper | 1.00 | 0.81 | 0.59/1.00 | 0.59 | 0.36 | 0.34 | 0.25 |
| vivid ×1.8 | 1.00 | 1.00 | 1.00 | 1.00/0.78 | 0.65 | 0.63 | **0.44** |

Il vestito era al massimo esattamente sul cielo che è già luminoso e al minimo su quello
sotto cui si apre l'app la sera. Non è una scelta: è la forma della regola. ×1.8 è un
**multiplo della cromia di paper**, giusto per un token — è quello che tiene neutro un
token deliberatamente neutro — e paper la notte l'ha disegnata con la cromia più bassa
di tutte. Il moltiplicatore dava il meno dove il gamut aveva il più.

`SKY_FLOOR = 0.65` in `gen_vivid.py`: nessuna banda sotto quella frazione del gamut alla
propria luminanza. Essendo una frazione del gamut e non un multiplo di paper, può solo
alzare le bande che il moltiplicatore aveva lasciato piatte. Sposta **tre righe e nient'
altro**: giorno, sole basso, i due anchor dorati, orizzonte e ora blu erano già sopra e
escono dal generatore identici — quindi la misura dello scrim di §3.7 non si muove, e
nemmeno il crepuscolo bruno che quella sezione registra. I token semantici non lo
ricevono: `VividLightColors` e `VividDarkColors` escono invariati.

0.65 scelto come fu scelto 1.8: renderizzando il foglio e guardandolo
(`tools/palette_sheet.py`, chromium headless). A 0.75 la mezzanotte comincia a leggersi
come un blu reale invece che come una notte.

### Considerato e scartato

**Un anchor in più all'ora d'oro**, il rimedio che §3.7 stessa indica. Misurato: agli
stop dorati la cromia disponibile alla luminanza tenuta è 1.00–1.01 volte quella di
paper. Non c'è un secondo ambra da avere, quindi un anchor in più interpolerebbe tra due
colori identici. Il rimedio di §3.7 vale per il crepuscolo bruno, che questo cambiamento
non tocca; qui non ci sarebbe niente da separare.

### Verifica

`SkyPaletteTest` guadagna il limite inferiore che gli mancava. C'era solo
`Paper.gradient(-6.0) != Vivid.gradient(-6.0)`: una singola altitudine, che qualunque
collasso parziale supera indisturbato. Adesso ogni stop che **può** differire deve
differire, e ogni stop che coincide deve esibire il gamut come scusa (cromia di paper
entro il 5% del tetto) — più un tetto sul numero di coincidenze, perché il controllo
stop-per-stop passerebbe anche se i due vestiti diventassero uno. Il secondo test
rimisura la regola di §3.7 sugli esadecimali emessi, che è l'unico modo di accorgersi
di `SKY_FLOOR` modificato e tabella non rigenerata. L'aritmetica OKLCh e il bordo del
gamut sono riscritti in Kotlin apposta perché possano dissentire da `color_math.py`.

DESIGN.md §3.7: tabella aggiornata sulle tre righe, la clausola stampata, il perché
misurato — e `PaletteDocTest`, che rilegge quella sezione e la confronta con quello che
il canvas disegna davvero, è il test che tiene le due cose insieme.

Suite intera verde: `:app` 188 (erano 186: i due test nuovi), `:core:domain` 166,
`:core:data` 174, `:core:sync` 5. `ScrimContractTest` continua a passare senza che sia
stato toccato, ed era il rischio vero del pavimento: spazza entrambe le tabelle a ogni
mezzo grado, e lo stop più luminoso — `#AFE0FF`, quello contro cui la misura è fatta —
è fra quelli che il pavimento non muove. Lint a zero errori e 61 avvisi, come prima. CI
verde su `claude/palette-settings-bug-343c6z`.

**La verifica su device è del committente**: il cielo della sera è la cosa che cambia, e
un foglio renderizzato non è un telefono in mano.

---

## Il quarto widget: «L'arco del giorno» (committente, 9 set 2026)

Richiesta: un widget nuovo, «moderno, in stile professionale, con qualcosa di speciale che
lo distingua dai classici widget meteo», ridimensionabile da 1×1 a 4×4 e che si riconfiguri
per informazioni e per layout a ogni misura, con una pagina di impostazioni completa. Libertà
creativa dichiarata («dimentica le regole»); la sola regola tenuta è quella che tiene verde la
CI, perché l'APK da provare esce da lì.

### Cos'è

Il percorso del sole sopra il luogo del lettore, **calcolato** dallo stesso motore
astronomico che dipinge il canvas: non un'icona di alba e tramonto, la traiettoria vera,
campionata ogni quarto d'ora (97 campioni, pochi millisecondi). Sotto, il cielo di ogni ora
come bande: la stessa `SkyPalette.gradient()` del canvas, con la copertura nuvolosa e la
pioggia dell'ora prevista dove il report ha l'ora, sotto lo stesso scrim §3.6 della card. La
luna: il suo percorso punteggiato mentre è alta e il disco nella **fase vera** (frazione
illuminata ed elongazione da `AstronomyEngine.moonIllumination`, terminatore come semiellisse,
lato illuminato a destra nell'emisfero nord e a sinistra in quello sud). La pioggia: la
probabilità di ogni ora come barra che sale dal suolo verso l'orizzonte (100% tocca
l'orizzonte; rampa `rainAt`). Il presente segnato, il passato velato. Le ore sotto, e sotto le
ore la temperatura prevista dove il report ce l'ha: il passato stampa l'ora e nessun numero.

In parole: il **prossimo momento della luce** con la sua ora («Tramonto alle 19:42») e il
conto alla rovescia («tra 2 h 10 min», contato sull'orologio, giusto anche con dati vecchi);
l'agenda delle prossime ventiquattro ore, riga per riga, con il **verdetto** del widget Cielo
accanto alle righe che sono anche momenti seguiti; sulla card a quattro righe, la settimana.

### Le cinque forme (`ArcLayout.kt`, puro, tabella in `ArcLayoutTest`)

Colonne dalla larghezza (soglie 120/210/300 dp), righe dall'altezza (150/245/340): stanno
negli spazi vuoti tra le griglie misurate (una cella ~70-101 dp, due ~150-180, tre ~230-260,
quattro ~320-360; una riga ~82-101, due ~189, tre ~290, quattro ~390).

- **DIAL** (una colonna): l'arco e un numero sotto, temperatura o ora del prossimo momento a
  scelta. 85×82 → arco 65×28,96 dp (62 − 29,04 la riga a 22 sp − 4). Con dati vecchi la riga
  «stale» toglie i suoi 14,52 all'arco: il segno che i dati sono vecchi vince sul disegno, e
  il pavimento dell'arco è 12 dp.
- **STRIP** (una riga): a due celle le parole **sopra** l'arco (139×31,6: 47 dp di larghezza
  non sono un giorno); a tre e quattro accanto, colonna di parole 100 dp («Tramonto · 19:42»
  a 12 sp è ~95), arco 122×62 e 212×62 con le etichette delle ore (≥ 52 dp) senza temperature
  (< 76).
- **CARD** (due colonne, due o più righe): numero 28 sp, luogo, frase su due righe a 13 sp,
  arco, agenda. 159×189: header 88,44, arco 66,56, nessuna riga; 159×290: 3 righe, arco
  84,08 con le temperature.
- **PANEL** (tre o quattro colonne, due righe): una riga sola in testa, il numero a 30 sp e
  accanto la frase a 15 sp con luogo · conto alla rovescia (· stale) a 12 sp sotto; la frase
  prende due righe sotto i 190 dp di colonna («Golden hour ends at 09:15» a 15 sp Medium è
  ~165). 340×189: header 39,6, arco 59,08, **2 righe di agenda** (il pannello preferisce le
  righe: 56 dp di arco preferiti); 250×189: frase su due righe, header 55,44, arco 70,4, 1 riga.
- **BOARD** (tre o quattro colonne, tre o quattro righe): l'arco preferisce 110 dp e prende
  fino a 150. 340×290: arco 132,92 e 3 righe; 340×390: settimana in fondo (70,88 dp: nome,
  glifo 22, massima, minima), arco 128,88 e 4 righe.

Il budget è sempre `righe = ⌊(spazio − arco preferito − gap + gapRiga) / (riga + gapRiga)⌋`,
mai più delle righe che esistono; l'arco prende il resto fino al suo tetto. Riga agenda =
13 sp × 1,32 + 6 = 23,16 dp; a font scale 1,3 il conteggio scende (1 riga sul 4×2) invece di
sforare. Densità compatta = testi al 90%: riga 21,44, e sul 4×3 quattro righe invece di tre.

### Le impostazioni (`ArcSettings`, DataStore `widget_arc`, per istanza)

Finestra (oggi da mezzanotte a mezzanotte, o le prossime 24 ore); suolo (bande, nastro
della luce, niente); strati (sole, luna, pioggia, presente, passato velato, ore,
temperature); parole (prossimo momento / frase del giorno / solo il numero; la cifra della
card 1×1; massima e minima dal look condiviso); agenda (sole, luna, pioggia, verdetti);
settimana; densità. Diciassette chiavi, codec puro (`ArcSettingsCodec`, tabella in
`ArcSettingsTest`): un valore di un'altra versione torna al suo default e non porta giù la
card. Più il look condiviso (sfondo, opacità, icone) e il luogo, come gli altri tre.

La pagina è sua (`ArcConfigActivity`): si apre su un'**anteprima viva** della card a otto
misure (1×1, 2×1, 4×1, 2×2, 4×2, 4×3, 2×4, 4×4, le misure del device di riferimento),
disegnata dallo stesso `ArcPainter` e dallo stesso `arcPlan` del launcher, con una frase che
dice cosa mostra quella misura; ogni tocco salva, ridisegna il widget e aggiorna l'anteprima;
un tasto riporta ai default.

### Decisioni

- **La grafica è un bitmap** (`ArcPainter`, `android.graphics`): RemoteViews non disegna una
  curva in nessun altro modo, ed è quello che permette all'anteprima di mostrare la stessa
  immagine del launcher. Le parole restano `Text` di Glance (localizzazione, font scale,
  accessibilità); nel bitmap solo le etichette delle ore e le temperature, perché devono
  stare a una x precisa, con le misure del testo fatte davvero (`Paint.measureText`: il passo
  si allarga finché «12 PM» non collide). La `contentDescription` dell'immagine dice il
  prossimo momento e quando: l'equivalente testuale (§9.3) anche sulla card da una cella.
- **Sole e luna con esadecimali** in `ui/theme/ArcPalette.kt`, il solo file della feature
  che ne ha (`NoRawColorTest`); tutto il resto sono ruoli e tabelle: percorso ed etichette
  negli inchiostri della card, bande dalle tabelle del cielo, pioggia dalla rampa §2.3.
- **Niente curva della temperatura.** Due scale su un grafico sono un doppio asse (§9.1): i
  numeri sotto le ore fanno il lavoro, e la card «Le prossime ore» ha già la striscia.
- **Finestra «oggi» di default**: è l'arco che tutti riconoscono; «le prossime 24 ore» in
  opzione parte 30 minuti prima di adesso, perché il disco del sole stia nel riquadro e
  l'ultima mezz'ora si legga come appena passata.
- **L'agenda guarda sempre 24 ore avanti**, qualunque sia la finestra: la regola è una,
  `TodayStateBuilder.agenda`, resa pubblica e finestrata; il `timeline` di Oggi è la stessa
  regola tagliata a mezzanotte (test in `TodayStateBuilderTest`). Il widget e la schermata
  hanno già stampato due albe diverse una volta, e la cura è una regola in un posto solo.
- **Verdetto sulla riga**: match per id del job (`sun.set`, `golden_hour.am` per la fine
  dell'ora d'oro del mattino, `twilight.astronomical.pm` o `darkness.window` per il buio
  pieno…) e ±20 minuti sull'inizio o sulla fine del momento. Le svolte della pioggia e
  l'arcobaleno non hanno job: nessuno si abbona a un rovescio.
- **Sotto i 40 dp il disegno si asciuga** (dial, strip a due celle): bande, arco, disco,
  presente; niente luna, pioggia, sole sotto l'orizzonte. Visto ai pixel: a 29 dp erano
  rumore.
- **Il velo del passato copre solo il suolo** (bande o nastro), mai la card nuda: con il
  nastro copriva un blocco grigio (visto ai pixel), ora è alto quanto il nastro; il percorso
  del sole nel passato si attenua da solo (alpha × 0,45).
- **Refactor condiviso, piccolo**: `resolveWidgetPalette`, `effectiveWidgetBackground` e
  `widgetCardFill` estratti da `WidgetCard` (l'anteprima non può scegliere un inchiostro che
  il launcher non sceglierebbe); `VerdictMark`/`VerdictChip` e le righe della pagina di
  configurazione (`SectionLabel`, `SwitchRow`, `ChoiceRow`) da private a internal.
- La guida dice «quattro» e aggiunge una frase sull'arco; README e CHANGELOG aggiornati.

### Rimasto aperto (su device)

- Le misure sul launcher vero: il 1×1 su griglie da 70 dp, il 4×4 (bitmap 858×~355 px), le
  font scale grandi, le etichette in 12 ore. L'anteprima Compose approssima la tipografia di
  Glance (line height diverse di qualche dp): è un'anteprima, non uno screenshot.
- L'icona del picker e la voce nel menu «widget» del launcher con la quarta card.
- VISION §5.9/§6 e DESIGN registrano tre widget: da aggiornare quando la card avrà passato la
  prova su device, come per gli altri tre.

### Verifica

Suite intera verde: `:app` 228 (erano 188: +16 `ArcLayoutTest`, +12 `ArcSeriesTest`, +6
`ArcSettingsTest`, +5 `ArcPainterTest` — Robolectric in grafica nativa, che dipinge il bitmap
davvero e ne legge i pixel, e con `CHIARO_RENDER_DIR` salva i fotogrammi in PNG per
guardarli, che è come sono stati trovati il blocco grigio del nastro e il rumore del dial —
+1 `TodayStateBuilderTest`; `WidgetPreviewTest` sa che i widget sono quattro), `:core:domain`
166, `:core:data` 174, `:core:sync` 5. Lint a zero errori. APK debug costruito in locale;
branch `claude/day-arc-widget-7k2m9q` e PR per la CI: **la verifica su device è del
committente**.

---

## L'arco su device: la colonna da dieci figli, la luna tagliata, la review (committente, 9 set 2026)

Primo screenshot del 4×4 (One UI, 923 px, ~2,35 px/dp), due appunti e una richiesta di
review. «Ottimo widget» a parte, quello che c'era da correggere.

### Il 4×4 senza settimana (e senza la quarta riga)

Lo screenshot mostrava tre righe di agenda, niente settimana e mezza card vuota, mentre
l'anteprima nelle impostazioni mostrava tutto. La misura della card (800×950 px ≈ 340×404 dp)
e l'altezza del grafico (~129 dp) dicono che il piano era quello giusto, BOARD a quattro righe
con settimana: quello che mancava era stato **scartato in disegno**. Glance disegna al massimo
**dieci figli per contenitore** e i successivi li lascia cadere senza una parola. La colonna
della card ne aveva tredici: intestazione, spaziatore, grafico, poi per l'agenda uno
spaziatore in testa e uno tra ogni riga (1 + 4 righe + 3 spaziatori = 8), lo spaziatore a peso
e la settimana. I primi dieci finiscono esattamente alla terza riga più il suo spaziatore:
la quarta riga, il peso e la settimana erano l'undicesimo, il dodicesimo e il tredicesimo.
Compose non ha quel limite, ed è per questo che l'anteprima mentiva.

Ora l'agenda è **un figlio solo** (`Column`), con gli spazi tra le righe come padding e non
come spaziatori: la colonna della card ne ha sei, quella dell'agenda al massimo sei. Nessun
altro contenitore del widget supera i dieci (settimana: sette colonne; riga di agenda:
cinque figli). L'anteprima ora dice la verità per costruzione: stesso piano, stessi limiti.

### La luna tagliata in basso

`mc_moonset` è, per disegno, un disco **ritagliato dall'orizzonte** (`clip-path` a 39,5/64)
con sotto una linea di 2 unità e una freccia. Nella schermata Oggi sta a 34 dp e la linea è
un dp: si legge «luna che tramonta». Nella riga del widget stava a 18 dp: la linea era 0,56 dp,
invisibile, e restava un disco con il fondo mancante — esattamente l'appunto. Le righe della
luna ora disegnano la **luna nella sua fase vera** all'istante della riga
(`ChiaroIcons.moonPhaseRes(MoonPhase.at(at))`): intera, la stessa luna che il grafico dipinge,
e la parola accanto dice se sorge o tramonta. I glifi di alba e tramonto restano i loro: i
raggi li fanno leggere anche a 20 dp. Il glifo di riga passa da 18 a 20 dp, quello del widget
Cielo, così le due liste sono sorelle.

### La review: cosa ho trovato e cosa ho cambiato

- **Il conto alla rovescia mentiva con il passare del tempo.** «tra 7 h 43 min» era esatto
  al minuto al momento del disegno, e il widget si ridisegna a ogni sync — ogni ora per
  default, mai al minuto: un'ora dopo diceva ancora 7 h 43. Ora è **grossolano di proposito**:
  ore arrotondate («tra 8 h»), «entro un'ora» sotto i sessanta minuti, «a momenti» sotto il
  minuto. L'ora esatta accanto («alle 19:42») è il fatto che non invecchia. Le due stringhe
  al minuto sono uscite da entrambe le lingue.
- **L'eroe era l'arcobaleno.** «Forse un arcobaleno alle 17:00» in testa alla card: la
  probabilità di un fenomeno meteo, non un momento della luce, che è quello che l'impostazione
  promette. `ArcSeries.nextLight`: il primo evento con un job del cielo dietro (sole o luna);
  l'agenda tiene tutto; se non c'è niente della luce davanti, l'eroe prende quello che c'è.
  Test in `ArcSeriesTest`.
- **La riga dell'arcobaleno era troncata** («…un arcobaleno stareb…»): la frase della
  schermata è prosa da riga intera. La riga ora stampa i due fatti, «Forse un arcobaleno a
  ovest (88%)» (`arc_rainbow_row`, con `SkyText.bearingRes`).
- **Gli orari dell'agenda non stavano in colonna**: «19:07» slittava a sinistra dei «19:00»
  sopra, spinto dal suo segno di verdetto. Ora, appena una riga ha un verdetto, ogni riga
  riserva il posto del segno (22 dp + 6): gli orari sono una colonna.
- **La strip a 3-4 celle troncava la seconda riga**: «Tramonta la luna · 19:00» a 12 sp è
  ~130 dp, la colonna ne aveva 100. Colonna a 110, numero a 22 sp (da 24) e **tre righe**
  dove l'altezza le regge (62 − 29,04 = 32,96 ≥ 2 × 15,84): il nome del momento a 12 sp Medium,
  sotto «19:00 · tra 8 h» in inchiostro secondario; a font scale 1,3 le due si fondono in
  «Tramonto · 19:00». Il grafico perde 10 dp (112 e 202). `ArcLayoutTest` aggiornato.
- Guardato e lasciato: le barre della pioggia sopra il tratto del sole sotto l'orizzonte
  (si sovrappongono a destra nello screenshot, ma il tratteggio resta leggibile e la barra è
  il dato); il segno di verdetto coi colori del tema chiaro su card scura (è una coppia
  misurata, come nel widget Cielo); il velo del passato che al mattino copre un terzo della
  card (è il disegno: il giorno è passato per un terzo).

### Verifica

`:app` 233 (+1 `ArcSeriesTest`, +4 `ArcTextTest` con Robolectric per le frasi), il resto
invariato. Lint a zero errori. `ArcPainter` non è cambiato, quindi i fotogrammi valgono
ancora. Stesso branch, CI. **La verifica su device è del committente**: il 4×4 con la
settimana in fondo e quattro righe, la luna intera nelle righe, la strip a 4×1 con tre righe.

---

## L'arco su device, secondo giro: la luna nuova, il segno di verdetto, l'audit dei glifi (committente, 9 set 2026)

Secondo screenshot: la settimana c'è e le quattro righe pure. Due domande: la luna della riga
«è corretta così?», e il segno di verdetto — la ✗ nel cerchio rosa — «non mi piace, troppo
forte, deve restare un avviso ma tenue».

### La luna: corretta per l'astronomia, e il committente la tiene così

Il glifo era quello di Meteocons per la **luna nuova**: un cerchio tratteggiato sottile. Il
classificatore aveva ragione — la luna nuova è l'11 settembre, la sera del 9 mancavano meno
di due giorni, ed è dentro l'ottavo di ciclo che `MoonPhase.at` chiama NEW_MOON. Il dubbio
era che in una riga un cerchietto tratteggiato si legga «qui non c'è niente»; l'alternativa
provata è stata la falce della notte serena (`mc_clear_night`) come simbolo fisso, il modo in
cui le righe del sole mostrano un sole e non lo stato del sole. Spiegato il perché del
cerchietto, **il committente ha scelto la fase vera**: «si vedrebbe lo spicchio o la luna
piena nel momento giusto? allora meglio lasciare così». Sì: ogni nome copre 45° di
elongazione centrati sul proprio istante, quindi il mezzo disco compare intorno ai quarti e
il disco pieno intorno al plenilunio, ciascuno entro circa un giorno e tre quarti
dall'istante esatto; il cerchietto tratteggiato resta i due-tre giorni al mese in cui la
luna non si vede, che è quello che disegna. La stessa fase che il grafico dipinge.

### L'audit dei glifi

Ogni glifo che il widget può mostrare, verificato nel drawable e nelle tre tabelle di
`ChiaroIcons` (piene, piene su fondo scuro, a linea su fondo scuro):

| riga | glifo | a 20 dp |
|---|---|---|
| alba, tramonto | `mc_sunrise`, `mc_sunset` | mezzo sole (clip a 39,5/64), riga di 2 unità = 1,7 px, freccia di 4,5 unità: si vede appena; il verso lo dice la parola |
| ora d'oro | `mc_horizon` | mezzo sole con la riga: leggibile (nello screenshot si vede) |
| ora blu | `mc_star` | stella a tratto 3: leggibile |
| buio pieno | `mc_starry_night` | falce e tre stelline: le stelline sono punti, la falce regge |
| sorge/tramonta la luna | `moonPhaseRes(MoonPhase.at(istante))` | otto glifi a tratto 2-3: falci, quarti, gibbose e disco pieno leggibili; la luna nuova è il cerchietto tratteggiato, scelto |
| arcobaleno | `mc_partly_cloudy_day_rain` | sole, nube, pioggia: leggibile |
| pioggia probabile / smette | `mc_raindrops`, `mc_cloudy` | leggibili |
| settimana | `conditionRes(codice, notte = false)` | 22 dp, la stessa mappa della card Oggi |

Tutte le famiglie di glifi hanno le quattro varianti (`mc_`, `mcn_`, `mcf_`, `mcfn_`), le
otto fasi della luna comprese, e sono nelle tabelle: un glifo senza fratello sarebbe stato
un crash al primo lettore che sceglie le icone piene (`getValue` lancia). `ArcTextTest` ora
lo pinza: ogni tipo di riga, in ogni famiglia, su entrambi i fondi, in entrambe le palette,
su trenta giorni di luna.

### Il segno di verdetto

Era la pillola del widget Cielo, riusata com'era: cerchio pieno di 22 dp nel colore del
contenitore e la ✗ nell'inchiostro del verdetto. Due cose la rendevano un pugno: **la coppia
di colori era quella del tema chiaro** — scelta con `isNight`, e il telefono era in tema
chiaro — su una card che è scura qualunque cosa faccia il telefono (il cielo sotto lo scrim,
o la card scura), quindi un rosa pallido pieno sul fondo più scuro dello schermo; e **la
pillola è l'eroe della card Cielo**, mentre qui è una nota accanto a un orario. Ora la riga
porta il solo glifo della serie — `✓ ~ ✗ ?`, una forma prima che un colore (§2.3), che è la
parte che regge la deuteranopia — nell'inchiostro del verdetto **scelto per il fondo della
card** (`palette.darkGround`), a 13 sp Medium come le parole della riga, in una colonna di
14 dp riservata per tutte le righe appena una ha un verdetto. Gli inchiostri del set scuro
sono quelli misurati contro una superficie scura: il glifo si legge dove sta, ed è la cosa
che mancava al «verde nudo» che il 4 set aveva fatto crescere il contenitore sul widget
Cielo. Il widget Cielo resta com'è: lì il verdetto è il soggetto.

### Verifica

`:app` 234 (+1 in `ArcTextTest`, l'audit dei glifi), lint a zero errori. Stesso branch.
**Su device**: la ✗ tenue accanto a «Ora d'oro»; la luna delle righe cambia da sola con la
fase, e la prima falce si vedrà dal 13 settembre.

---

## Il segno di verdetto, terza volta: un disegno, non un carattere (committente, 9 set 2026)

«Ora va bene perché è tenue, però la X scritta così, come un font in stile scrittura a mano,
non mi piace». Aveva ragione, e il motivo è preciso: i glifi della serie `✓ ✗` sono U+2713 e
U+2717, che **Roboto non ha**. Android li prende da un font di fallback per i simboli — su
One UI di taglio calligrafico, su un Pixel un altro — quindi il segno non era mai stato nella
mano del widget, né nel widget Cielo né qui. L'alternativa a costo zero, il segno di
moltiplicazione «×» di Roboto per il FAIL, avrebbe lasciato la ✓ al font di fallback: due
segni della stessa famiglia in due mani diverse.

Quattro vettori (`ic_verdict_pass`, `_unstable`, `_fail`, `_unknown`): un peso di linea solo,
2,4 su 24 — 1,2 dp ai 12 dp a cui stanno, il tratto di un 13 sp Medium — punte arrotondate,
nessun colore proprio: tinti con l'inchiostro del verdetto dove compaiono (`ColorFilter.tint`),
scelto per il fondo della card come prima. 12 dp è l'altezza delle minuscole delle parole
accanto, così la croce sta nella riga come una lettera e non come un'icona. La regola resta
quella di §2.3: una forma prima che un colore, e un disegno per verdetto, mai due verdetti su
una forma (test in `ArcTextTest`). La `contentDescription` del segno è la parola del verdetto,
così la riga si legge «Ora d'oro, 19:07, no».

Il widget Cielo usa ancora il carattere, dentro il suo contenitore: stessa mano calligrafica.
Non toccato — è il disegno che il committente ha approvato il 4 set — ma i quattro vettori sono
pronti anche per lui, se lo vorrà: una riga in `VerdictMark`.

### Verifica

`:app` 235, lint a zero errori. Stesso branch. **Su device**: la croce nella riga «Ora d'oro».

---

## I quattro segni ovunque: widget Cielo, chip dell'app, tabella in `ChiaroIcons` (committente, 9 set 2026)

«Usa i 4 vettori anche nel widget Cielo e poi verifica in tutta l'app se ci sono altri
punti in cui si potrebbero utilizzare.»

Cercati tutti i punti che stampano i glifi del verdetto come caratteri (`.glyph`, `✓`,
`✗`, `"~"`): erano **due**, e ora sono zero.

- **`VerdictMark` del widget Cielo**: il carattere `verdict.kind.glyph` a 12 sp dentro il
  contenitore rotondo di 22 dp. Ora il disegno a 12 dp (`SkyMarkGlyph`, che sostituisce
  `SkyMarkSp`), tinto con l'inchiostro del verdetto come prima, con la parola del verdetto
  come `contentDescription`. Il contenitore resta: lì il verdetto è il soggetto della card,
  ed è il disegno approvato il 4 set.
- **`VerdictChip` dell'app** (`ui/components`), cioè la schermata Cielo (le card dei
  momenti e il calendario), la guida e ogni riga che apre con `✓ Ottimo · 12% nuvole`: il
  `Text(glyph)` a `labelLarge` diventa una `Icon` di 14 dp scalata con il font del lettore
  (`forText()`), tinta con l'inchiostro del chip. La semantica non cambia: il chip
  annunciava già solo «ottimo, 12% nuvole» e mai il segno.

La tabella sta in un posto solo, `ChiaroIcons.verdictMarkRes`, in due forme — per il
`VerdictKind` della UI e per lo `SkyVerdictKind` del dominio — perché i widget hanno in
mano verdetti e il chip ha in mano il suo enum; `ArcText.markRes` sparisce e i due widget e
il chip leggono la stessa riga. Il test in `ArcTextTest` verifica che le due forme diano gli
stessi quattro disegni e che nessun verdetto ne condivida uno con un altro.

Dove i vettori **non** servono, verificato: le notifiche (solo testo, il segno non c'è),
il Diario (parole, nessun glifo), le stringhe (nessuna contiene `✓` o `✗`). Il dominio
tiene il suo `glyph` in `SkyVerdictKind`: è il vocabolario di tweather, che li stampa come
codice, e nessuno in Chiaro lo legge più.

DESIGN.md §8.7 registra che il glifo è un disegno e perché.

### Verifica

`:app` 235, lint a zero errori. Stesso branch. **Su device**: il segno nel widget Cielo e
i chip della schermata Cielo.

---

## Il riallineamento dei due repo (committente, 9 set 2026)

Richiesta: «controlla i file `UPSTREAM.md` dei due repo e, prima di toccare il codice,
fammi un elenco delle implementazioni da fare per riallineare i due repo»; poi
«esegui i punti che hai proposto nell'ordine proposto».

### Il confronto

tweather non ha un `UPSTREAM.md`: il suo registro sono le fasi che citano Chiaro nel
suo `PLANNING.md`. Il confronto è stato quindi meccanico — la riscrittura di
`tools/seed_core.py` applicata a tweather HEAD e diffata file per file con `:core` —
e poi ogni differenza letta contro quello che i due registri dichiarano. Prima: 55
file identici su 86 condivisi, 30 diversi, più 2 solo a monte e 10 solo qui. Dopo: 66
identici, e ogni differenza rimasta ha la sua riga in `UPSTREAM.md`: la classificazione
file per file sta nell'elenco «What is NOT the same as upstream» e la passata è
raccontata nella sezione «The second pass». Il confronto si ripete con la riscrittura
di `tools/seed_core.py` applicata a un checkout di tweather e un diff dei due alberi.

### Portato a monte (in tweather), e la base che era vecchia

Due correzioni che `UPSTREAM.md` segnava come «bug anche a monte» e che a monte
mancavano ancora: `DailyForecast.precipPct` nullable (con `WeatherCodes.FIRST_PRECIP_CODE`
e `isPrecipitation`, così il mapper torna identico e non diverso per una riga) e
`SkyNotScheduled.DARK_ALL_DAY`. Ogni file toccato in `:core` è di nuovo identico; le
superfici a valle in tweather (`WeatherReadme`, `WeatherJson`, `SkyDocument` e le sue
note) sono state adattate nel suo registro. Lì è la **Fase 29**.

La terza, la stringa `"null"` che `WeatherSnapshots.flatten` scriveva in
`current.precip_chance_pct`, è stata portata e poi **ritirata**: il confronto era stato
fatto su un `main` locale di tweather fermo al 6 set, e il rebase della PR gemella ha
trovato le Fasi 27–28c a monte, fra cui la 28 che sullo stesso punto aveva deciso il
contrario e di proposito — `history.diff` è un diff di `weather_data.json`, un valore
assente è una riga che dice `null`, con `NullValue` nominato e insieme di chiavi fisso.
Qui il Diario è prosa e le sue «shift» accoppiano le chiavi, quindi la chiave assente
resta omessa, ora anche per `sunrise` e `sunset` che scrivevano ancora la parola:
stessa realtà, due registri, due file, e `UPSTREAM.md` lo dice. Dalla stessa Fase 28
sono invece tornati a valle i fatti: `Duration.hhMm()` nel dominio, `ForecastDiff` senza
`dayLabel` (con il suo test), il `location` che ripiega sul paese come `City.label`, la
chiave `astronomical.daylight_duration`. La lezione è di procedura, e sta in memoria:
`git fetch` e confronto con `origin/main` in tutti e due i repo prima di misurare.

### Fatto qui

- **`UPSTREAM.md`** registra le divergenze che erano nate senza una riga
  (`SearchHistoryStore.remove/restore`, `WeatherHistoryDao.observeFor` e
  `historyFlowFor`, la collocazione di `WeatherFreshnessTest` in `:core:data`, il nome
  del file di `PowerSaveState`, il `fetchLogStore` in `overrideForTests`), segna come
  portate le tre correzioni, e ha una sezione nuova sulla passata.
- **I tre test che tweather aveva e Chiaro no.** `SkyAlarmSchedulerTest` è un porting
  diretto in `:app` (Robolectric, `ShadowAlarmManager`: inesatto, `RTC_WAKEUP`, una
  sola sveglia, il boot la riarma, un promemoria non consegnabile riarma comunque).
  `WeatherSyncWorkerTest` è un porting in `:core:sync`, che per questo ha ora un banco
  Robolectric (`isIncludeAndroidResources`, `work-testing`, e le librerie del layer dati
  come `testImplementation`, perché arrivano a `main` attraverso `:core:data` come
  `implementation` e il classpath dei test non le vede): i notificatori e i widget sono
  fake dietro `SyncDependencies`, e due casi sono di Chiaro — un fetch fallito scrive
  nel `FetchLogStore` con la sua ragione, e ridisegna i widget perché il marcatore di
  vecchiaia possa apparire. Per il primo `ServiceLocator.overrideForTests` ha imparato
  il `fetchLogStore`. `SkyNotifierTest` tiene la struttura del test di tweather e
  nessuna parola: i due corpi, la localizzazione, e il controllo che nessun id puntato
  raggiunga la notifica. **L'orologio è asserito per forma e non per valore**:
  `SkyNotifier` segue l'impostazione 12/24 ore del dispositivo, che è del lettore e non
  del test.
- **`WeatherSnapshotsTest`** ha il caso della chance assente (identico a quello portato a
  monte).
- **I commenti dei file condivisi**, una sola formulazione datata in entrambi i repo:
  `LocationProvider` (i tre «Fase 3b» diventano «4 set 2026», e il paragrafo di testa
  dice che il file è tenuto identico e perché), `WeatherFreshness`, `PowerSaveState`, il
  doc di `getWeather` in `WeatherRepository`, e i cinque punti del porting (`precipPct`,
  `FIRST_PRECIP_CODE`, il mapper, le due chiavi dello snapshot, il test del mapper) dove
  la formulazione di Chiaro citava `ForecastOutcome`, «il seed» o il §1.1. Decisione:
  **nei file condivisi si data, non si numera** — i due PLANNING numerano lo stesso
  lavoro in modo diverso e lo faranno sempre. I commenti che nominano una superficie di
  una sola app (`RuleEngine`, `NotificationRule`) restano diversi di proposito.

### Deciso e non fatto

- `adoptGpsFix` con `gps_fixed_at` e `CachedLocationProvider` restano di Chiaro:
  tweather li ha rifiutati esplicitamente nella sua Fase 20 e la decisione è sua.
- L'estrazione di `weather-core`: il registro dice che il grilletto è scattato il 5 set.
  È una decisione, non un'implementazione, e resta al committente.

### Verifica

`:core` 354 test, `:app` 251, lint 0 errori. tweather, sul branch ribasato: 726 test e
lint 0 errori, con tre casi che su questa macchina falliscono anche su `origin/main`
pulito (la CI di `main` è verde) e sono dell'ambiente, non del riallineamento. Due PR,
una per repo, sullo stesso
branch `claude/core-realignment-9set-a4k7m2`: il committente le fonde e cancella i
branch.

---

## Fase 11 — Allerte ufficiali, primo strato: la Protezione Civile

Pianificata il 9 set 2026 (committente), dopo v1.0.0. Il perimetro sta in VISION §5.2, §5.4,
§5.5, §5.9 e §12.8; questa è la fase che lo realizza per i luoghi in Italia. La regola che la
governa: **in Italia fa fede la Protezione Civile**. L'ordine — prima questa, poi MeteoAlarm
(Fase 12) — è del committente e inverte la prima proposta, con tre motivi che VISION §12.8
registra per esteso: il CAP italiano di MeteoAlarm dichiara da solo di non essere l'allerta
ufficiale del Servizio Nazionale di Protezione Civile; il bollettino si verifica il pomeriggio
stesso contro il cielo e contro il bollettino regionale, cioè dove l'app viene collaudata; e «in
Italia una sola voce» è una regola di precedenza per paese, non una fusione di fonti.

Le parole, prima del codice. **«Allerta»** è quella che emette l'autorità (EN *warning*);
**«Avvisi»** restano quelli che il lettore scrive o accende (EN *alerts*). In codice la famiglia
si chiama `warnings/` (`OfficialWarning`, `WarningLevel`, `WarningHazard`), perché `Alert` è già
degli avvisi integrati. Nessun codice di zona (`Abru-A`), nessun identificativo di bollettino,
nessuna sigla CAP raggiunge lo schermo: c'è un test per questo, come per gli id del cielo.

- [x] `:core:domain/warnings/`: il modello (`WarningBulletin`, `ZoneWarning`, `PlaceWarnings`,
      `WarningLevel { NONE, YELLOW, ORANGE, RED }`, `WarningHazard { HYDRAULIC, HYDROGEOLOGICAL,
      THUNDERSTORM }` — i quattordici tipi di MeteoAlarm si aggiungono in Fase 12), l'indice
      delle zone (`WarningZoneIndex`: punto-nel-poligono sulle 156 zone, con il comune come
      ripiego) e il motore (`OfficialWarningEngine.forPlace` → `PlaceWarnings?`, `null` quando
      non c'è nulla da dire; `notificationFor(...)` con la sua impronta). Tutto puro, con
      tabella di test
- [x] `tools/build_warning_zones.py`, l'importatore di riferimento (come `import_meteocons.py`):
      dallo shapefile del bollettino (`Zona_all`, `Nome_zona`, geometria) e dal TopoJSON
      (`Comuni`) produce `core/data/src/main/assets/warning_zones_it.json` — codice, nome,
      regione, poligoni semplificati, comuni normalizzati. **Misurare**: obiettivo ≤ 400 KB
      grezzi; il numero finisce qui. `WarningZoneIndexTest` colloca venti comuni noti (coordinate
      dal geocoding, fissate nel test) nella loro zona, e tre punti in mare in nessuna
- [x] `City` cresce di `countryCode` e `admin3` (nullable, con default), riempiti su entrambe le
      strade: `GeoResultDto`, che li riceve già e li scarta per `ignoreUnknownKeys`, e `GeoFix`
      della posizione. Registrato in `UPSTREAM.md`
- [x] `:core:data/warnings/`: `WarningSource` (interfaccia) e `DpcBulletinSource` — scoperta,
      download, parser. Zip con `java.util.zip`, CAP con un pull-parser sottile che produce un
      intermedio piatto; la lettura di livelli e rischi dalle etichette («ORDINARIA CRITICITA'
      PER RISCHIO IDRAULICO / ALLERTA GIALLA») sta nel dominio, per parole chiave, con test su un
      CAP vero salvato come fixture (il primo file non Kotlin sotto `src/test`, e va bene così: un
      CAP inventato non proverebbe niente)
- [x] La scoperta del bollettino di criticità: Atom dei commit (18 KB) → `.diff` dell'ultimo
      commit (≈200 B) → `AAAAMMGG_HHMM` → `files/xml/<stamp>.zip` (7 KB). Ripiego
      `files/all/latest_all.zip` (4,7 MB) solo su rete non a consumo. La vigilanza non ha bisogno
      di scoperta: `files/<AAAAMMGG>.json` e `files/xml/<AAAAMMGG>.zip`
- [x] Persistenza: `OfficialWarningStore` (DataStore `warnings`: il bollettino corrente per fonte
      come JSON, più l'anello delle impronte notificate, mai dentro `settings`) e la tabella Room
      `warning_records` (migrazione 4→5, additiva) che il Diario legge
- [x] Il passo nel job: `OfficialWarningsStep`, chiamato da `WeatherSyncWorker` dopo il fetch,
      dentro `runCatching`, con la sua cadenza (sotto) e il suo motivo in `alertsWanted` e
      `shouldRun`, così un lettore che vuole solo le allerte tiene vivo il job
- [x] `SyncNotifiers.notifyOfficialWarning(...): Boolean`, `OfficialWarningNotifier` in `:app`,
      due canali, impronta bruciata solo su `true`; il fake in `WeatherSyncWorkerTest` cresce
- [x] Oggi: `WarningBanner` tra il chip di freschezza e le prossime ore; `WarningSheet`; il
      gradino in cima alla scala di `HeadlineEngine` per arancione e rosso, con la sua riga in
      `HeadlineText` nei due registri
- [x] Avvisi: il gruppo «Allerte ufficiali» in testa, con la card, i tre stati onesti,
      l'interruttore delle notifiche e il livello di partenza
- [x] Diario: la riga quando il livello cambia tra due bollettini, con il suo glifo di categoria;
      la riga «bollettino non raggiunto», una al giorno al massimo
- [x] Widget: il chip su Ora, Oggi e Arco, l'interruttore per istanza (`showWarning`, acceso),
      le regole di layout pure con tabella, il conteggio dei figli di Glance
- [x] DESIGN.md: §2.3 i tre token di livello misurati (giallo, arancione, rosso: inchiostro e
      contenitore, chiaro e scuro, ΔE fra loro sotto deuteranopia — il rosso può essere la
      coppia `fail`, il giallo NON è `unstable`: l'ambra della freschezza e il giallo di
      un'allerta sono due affermazioni diverse); §8.13 `WarningBanner`, `WarningSheet`, il chip;
      `ic_warning` disegnato con il peso dei segni di verdetto, mai il carattere ⚠
- [x] Guida: un paragrafo nel capitolo Avvisi (cos'è, chi la emette, quando arriva);
      attribuzione «Dipartimento della Protezione Civile, CC BY 4.0» nel foglio e in Informazioni
- [x] Stringhe IT/EN (`warning_*`, `notif_warning_*`, `notif_channel_warning_*`),
      `StringsParityTest` verde; `OfficialWarningNotifierTest` sul modello di `SkyNotifierTest`
- [x] Verifica su device, in Italia: chiusa dal committente l'11 set 2026 su varie località, due
      giorni dopo il merge invece dei sette previsti; il default del livello di notifica resta
      **gialla**. Quel che quei due giorni non hanno attraversato è sotto, in «Verifica»
- [x] **La lacuna dell'apertura**, trovata e chiusa l'11 set 2026: il passo non girava mai
      fuori dal job periodico. `WarningRefresh` è l'ingresso che mancava (sotto)

### Le misure che hanno deciso (9 set 2026)

Tutto verificato dal vivo quel giorno, non letto in una documentazione.

| Cosa | Misura |
|---|---|
| Bollettino di criticità | Entro le 16 (quello dell'8 set: emesso 15:19, su GitHub alle 15:43), oggi e domani, 156 zone di allerta (187 poligoni), tre rischi (idraulico, idrogeologico, temporali), quattro livelli — i temporali non hanno il rosso |
| Il suo CAP | `files/xml/<stamp>.zip` 7 KB → `Cap_<stamp>.xml` 45 KB; 8 blocchi `info` (rischio × livello × giorno), 209 aree con geocodice `Zona di Allerta` (es. `Cala-5`, `Abru-A`), **nessun poligono**: le zone assenti sono verdi; `<note>` con i rinvii ai bollettini regionali (Lombardia, Campania, Liguria quel giorno) |
| Il join | I 187 nomi del TopoJSON sono unici e contengono tutti i 209 `areaDesc` del CAP; il DBF dello shapefile porta il codice (`Zona_all`) accanto al nome: è da lì che l'indice prende codice, nome e geometria |
| I comuni | Il TopoJSON elenca i comuni di ogni zona; encoding sporco («Citt� Sant'Angelo») e nomi troncati nella vigilanza («Belv», «Bidon», «Buddus») |
| Geocoding | `admin3` = «Comune di Segrate» anche per la frazione (Redecesio → Segrate, Bovisa → Milano); `country_code` ricevuto e oggi scartato da `toCity` |
| Scoperta | Atom dei commit 18 KB con titoli generici («Published by DPC Github Pipeline»), ~12 commit per bollettino, l'ultimo è la preview PNG; `.diff` del commit ≈200 B col nome del file; `files/all/latest_all.zip` è stabile ma pesa 4,7 MB (shapefile e PDF dentro); `files/xml/latest*.zip` non esiste |
| Vigilanza | Entro le 15, tre giorni, 71 zone con comuni, CAP 5 KB con la sola categoria «Precipitazioni previste» a quattro gradini (assenti, deboli, moderati, elevati); temperature, venti, visibilità e mare sono **prosa nazionale**, più un livello di 42 punti con `id_fenomeno` e 25 icone senza legenda testuale |
| GitHub dai telefoni | L'API REST ha 60 richieste l'ora per IP e gli operatori mobili condividono un IP fra migliaia di utenti: **non si usa**. Il raw e l'Atom dei commit sono il sito, non l'API |
| Il sito delle mappe | Nessun endpoint «ultimo bollettino» (cercato nel markup: niente) |
| Licenza | CC BY 4.0 per entrambi i repository; i README dicono «in fase di caricamento» dal 2020 |
| Radar (per memoria, VISION §12.5) | Radar-DPC: tile WMTS 3,5 KB a zoom 6, GeoTIFF VMI 619 KB a 1 km, ultimo frame vecchio di 11 minuti, 40 radar su 50 attivi, il parametro `time` ignorato dalle tile (tre richieste, tre immagini identiche byte per byte). Non si fa niente, per scelta del committente |

### La cadenza del passo

Il passo vive nel job condiviso e spende rete solo quando è dovuto; non ha un orologio suo.

- **Criticità**: dalle 15:30 locali finché non c'è un bollettino di oggi; poi un ricontrollo l'ora
  fino alle 21 (l'Atom: 18 KB) per cogliere un «Aggiornamento»; dopo le 21 niente fino a domani.
  Prima delle 15:30 il «domani» del bollettino di ieri **è** oggi: non c'è nulla da scaricare.
- **Vigilanza**: dalle 14:30, stesso schema; il nome è la data, un 404 vuol dire «non ancora».
- **All'apertura** di Oggi, se è passata l'ora e il bollettino di oggi non c'è: in silenzio, con
  lo stesso cancello del risparmio energetico della rilettura silenziosa; un trascinamento per
  aggiornare lo forza. Il passo è uno per dispositivo, non per luogo: un bollettino copre tutti.
- **Con l'intervallo a 120 minuti** il caso peggiore è vedere il bollettino due ore dopo: accettato
  e dichiarato, perché il banner stampa l'ora del bollettino, non l'ora della lettura.
- **Il passo è inerte** se nessun luogo salvato cade in una zona italiana (indice: prima il
  riquadro, poi il poligono).
- **Un fallimento** tiene il bollettino precedente e dice la sua data; una riga nel Diario al
  giorno, non a tentativo. Mai un errore che il lettore debba leggere in Oggi.

### Dove e quando si vede

**Oggi.** Il `WarningBanner` sta dopo il chip di freschezza e prima della card della guida: il
chip qualifica l'eroe (parla dei dati), il banner parla del mondo, quindi apre il contenuto.
`Surface` a tutta larghezza dentro `PagePadding`, angolo `medium`, contenitore del livello più
alto, altezza minima 56dp; a sinistra `ic_warning` 24dp nell'inchiostro del livello; prima riga
`titleSmall` «Allerta arancione per temporali» (più rischi: «Allerta arancione per temporali,
gialla per rischio idrogeologico», in ordine di livello); seconda riga `bodySmall` «Oggi fino a
mezzanotte · Protezione Civile, bollettino delle 15:19» — oppure «Domani · …», «Oggi e domani · …».
Un solo annuncio TalkBack. Tocco → il foglio. Si disegna solo se per oggi o domani il livello
massimo è almeno giallo e il bollettino non è scaduto (scade a mezzanotte del suo «domani»);
altrimenti non esiste, e il verde non si annuncia qui. Arancione e rosso prendono anche la frase
in cima — gradino zero della scala di `HeadlineEngine`, oggi prima di domani — «Allerta arancione
per temporali, oggi» / «Domani allerta rossa per rischio idraulico»; il giallo no, perché in
autunno il giallo è uno stato frequente e una frase uguale un giorno su tre smette di essere
letta. Le righe della settimana non portano segni: il budget della `DayRow` a 360dp è già speso
(DESIGN §8.5) e il banner nomina il giorno.

**Il foglio** (`WarningSheet`, un `ModalBottomSheet`). Titolo «Allerta per *Nodo idraulico di
Milano*» con la regione sotto. Una griglia: una riga per rischio, due colonne (Oggi, Domani), in
ogni cella la **parola** del livello nel suo contenitore — «nessuna» in grigio `unknown`, mai una
cella vuota, mai un colore da solo. Poi «Cosa vuol dire», il significato del livello più alto con
le parole del Dipartimento (la fonte citata nel commento della stringa: gialla, fenomeni
localizzati e possibili disagi; arancione, fenomeni diffusi o intensi e possibili danni; rossa,
fenomeni molto intensi e pericolo per le persone) — è la riga «ogni numero dice cosa farne» di
questa superficie. Poi la `<note>` del bollettino, solo se nomina la regione del luogo,
etichettata «Nota del bollettino» e citata com'è. Poi, quando c'è, la vigilanza: «Precipitazioni
previste: elevate domani» per la zona di vigilanza del luogo. In coda la fonte con la data e
l'ora («Dipartimento della Protezione Civile · bollettino dell'8 settembre, 15:19 · CC BY 4.0») e
«Apri il bollettino», che porta alla pagina del bollettino sul sito del Dipartimento.

**Avvisi.** Un terzo gruppo, in testa, «Allerte ufficiali»: la stessa card del banner, che apre lo
stesso foglio, oppure uno dei tre stati che il banner non deve mai disegnare perché qui l'assenza
è la risposta cercata: «Nessuna allerta per *Nodo idraulico di Milano* · bollettino delle 15:19»;
«Bollettino di ieri: il prossimo esce di solito entro le 16»; «Le allerte ufficiali sono
disponibili per i luoghi in Italia» (la riga che la Fase 12 cambia). Sotto, l'interruttore delle
notifiche con la descrizione esatta di cosa manda e quando — «Quando la Protezione Civile emette
un'allerta per questa zona, appena esce il bollettino, di solito tra le 15 e le 17. Mai più di
una per bollettino» — e la riga «Avvisami da: gialla / arancione». Vive qui e non nelle
Impostazioni per il motivo della Fase 6: accanto a ciò che governa. Il permesso si chiede al
primo interruttore che si accende, come per gli altri tre.

**Diario e guida.** Una riga quando, per il luogo attivo, un livello compare, sale o scende tra due
bollettini consecutivi — «Allerta arancione per temporali, domani (Protezione Civile, 15:19)»,
«Allerta rientrata: nessuna criticità per domani» — con un glifo di categoria suo, silhouette
Material come le altre cinque; e la riga del bollettino non raggiunto. La guida aggiunge un
paragrafo al capitolo Avvisi che dice cosa è un'allerta, chi la emette e quando arriva; non
giustifica l'assenza fuori dall'Italia, dice cosa fa in Italia.

### Le notifiche

**Quando.** All'ingestione di un bollettino — nuovo identificativo, oppure stesso identificativo
con un livello massimo più alto (l'«Aggiornamento») — se per il luogo attivo il massimo tra oggi
e domani è almeno il livello scelto e l'impronta `"$cityKey:warn:$bulletinId:$maxLevel"` non è
ancora bruciata. Un livello che scende non notifica: lo scrive il Diario. Il luogo attivo e basta,
come tutto il resto del job; le città appuntate ai widget restano fuori, registrato sotto.

**Due canali**: `warning_high` («Allerte arancioni e rosse», `IMPORTANCE_HIGH`) e
`warning_yellow` («Allerte gialle», `IMPORTANCE_DEFAULT`). Due perché Android permette al lettore
di zittire il giallo dal sistema senza perdere l'arancione, e la riga «Avvisami da» fa lo stesso
dall'interno: due strade oneste per la stessa scelta. Id fisso `3000 + (cityId % 1000)`, `3000`
per la posizione: una notifica per luogo alla volta, un bollettino nuovo sostituisce il vecchio.
Categoria `STATUS`, icona piccola dell'app, nessun colore e nessuna azione, come le altre.

**Chiusa.** Titolo «Allerta arancione · Milano». Testo, una riga: «Temporali, oggi fino a
mezzanotte · bollettino delle 15:19»; con due rischi «Temporali e rischio idrogeologico, oggi ·
…»; per domani «Temporali, domani · …».

**Espansa** (`BigTextStyle`). La prima riga è la frase chiusa — il test del non-scostamento di
`SkyNotifierTest` vale anche qui — poi una riga vuota, poi un fatto per riga, ciascuna presente
solo se ha i suoi dati, in quest'ordine:
- «Oggi: temporali arancione · idrogeologico giallo»
- «Domani: temporali giallo»
- «Zona di allerta: Nodo idraulico di Milano»
- «Cosa vuol dire: …» — il significato del livello più alto, le parole del Dipartimento
- «Nota del bollettino: …» — solo se la nota nomina la regione del luogo
- «Dipartimento della Protezione Civile · bollettino delle 15:19» — sempre ultima

Il tocco apre l'app su Oggi, dove il banner è la prima cosa sotto la tela: nessun deep link, che
oggi non esiste e non serve qui. `OfficialWarningNotifierTest` come `SkyNotifierTest`: la forma
della chiusa, le righe dell'espansa, `@Config(qualifiers = "it")`, la chiusa uguale alla prima
riga dell'espansa, e nessun codice di zona o identificativo nel testo.

### I widget

I widget del sistema accanto ai quali Chiaro vive mostrano una riga quando il servizio nazionale
emette un'allerta: è un'abitudine del lettore, non un'invenzione. La regola segue quella della
frase, e si scrive come funzioni pure con tabella (`*LayoutTest`), come tutto il resto:

- **Arancione e rosso**: la frase (`HeadlineText`, registro breve: «Allerta arancione · temporali»)
  già lo dice. Con la frase accesa nessun chip — sarebbe la stessa cosa detta due volte; con la
  frase spenta il chip prende lo slot della frase, così l'allerta arriva sul widget comunque.
- **Giallo**: la frase non lo porta mai, quindi il chip ha una riga sua dove la forma ha spazio
  per una riga in più — Ora largo e alto, la riga eroe di Oggi (sotto la frase), il pannello e la
  card dell'Arco — e mai sulle forme a una riga, né sul quadrante e sulle strisce dell'Arco.
- **Il chip**: `ic_warning` 12dp nell'inchiostro del livello più la parola («Allerta gialla»)
  a 11sp sul contenitore del livello, angolo 10dp, padding 7/3 — la grammatica del `VerdictChip`
  del widget Cielo. `contentDescription` in parole. I colori passano da
  `palette.dress.colors(palette.darkGround)`: il fondo della card, non il tema del telefono (la
  trappola già documentata nel widget Cielo). Contare i figli di ogni contenitore Glance toccato.
- **L'interruttore per istanza**: `WidgetLook.showWarning` (Ora, Oggi) e `ArcSettings.warning`
  (Arco), **accesi** di default: in un giorno senza allerta non cambiano nulla, e in un giorno con
  l'allerta sono la riga che un lettore meno vorrebbe che il widget tacesse. Cielo non lo offre:
  quella card parla dei momenti del cielo, e un chip sul suolo sarebbe un secondo soggetto. Le
  anteprime del picker non cambiano: l'allerta non è lo stato che si pubblicizza.
- **I dati**: `WidgetModel.warning: PlaceWarnings?` caricato in `WidgetData.load` dallo store,
  mai dalla rete al momento del disegno; il passo che scrive lo store chiama `repaintAll`, e lo
  store entra nel collettore di processo di `ChiaroApplication`.

### Decisioni della fase (prese in pianificazione, 9 set 2026)

- **Geometria prima dei nomi.** I nomi dei comuni nei file sono sporchi (encoding, troncature) e
  una frazione non è un comune; il punto-nel-poligono sulle zone semplificate funziona anche per
  la posizione GPS e non dipende da nessuna stringa. Il comune resta il ripiego per un punto sul
  confine. Il costo è un asset da misurare (≤ 400 KB) e un algoritmo puro con la sua tabella.
- **La scoperta passa dal sito, non dall'API**, per il tetto per IP condiviso dagli operatori
  mobili. Atom più diff: due richieste piccole al giorno per dispositivo. `latest_all.zip` è il
  ripiego, non la via, per i suoi 4,7 MB.
- **Store più tabella**, non una colonna su `weather_history`: un bollettino è un documento con
  una validità, non un'osservazione di un fetch (l'argomento di `sky_runs` non si applica), e
  deve comparire in Oggi anche nei giorni in cui nessun fetch è riuscito. La tabella è per il
  Diario e per «cosa è cambiato tra due bollettini».
- **La frase la prendono arancione e rosso, non il giallo.** Motivo sopra: frequenza. Il banner
  copre il giallo.
- **Il verde si dice una volta, in Avvisi.** In Oggi l'assenza non si disegna (DESIGN §1.1);
  in Avvisi è la risposta a chi è venuto a controllare.
- **Default del livello di notifica: gialla.** È il livello che il Dipartimento stesso
  definisce «prestare attenzione» ed è quello che i lettori italiani sentono nominare; il canale
  a importanza normale evita l'heads-up. Si tiene o si alza dopo la settimana di collaudo, con i
  bollettini veri contati.
- **Il luogo attivo e basta**, come il resto del job. Le città appuntate ai widget non notificano
  (registrato in «Rimasto fuori»).
- **Nessun deep link**: Oggi mostra il banner per primo. Quando una superficie lo pretenderà, il
  costo è `MainActivity` più `ShellTab`, e sarà quella fase a pagarlo.
- **Vigilanza: solo la classe delle precipitazioni, solo nel foglio, senza notifica.** Non è
  un'allerta ed è l'unica parte strutturata; il livello dei fenomeni ha 25 icone senza una
  legenda testuale documentata, e inventarne una sarebbe una schermata che mente.
- **Le parole dell'autorità si citano, non si traducono** (VISION §8). Nota, significati e
  descrizioni restano in italiano anche per il lettore inglese, etichettate come della fonte;
  livello, rischio, giorno e zona localizzano.
- **Il giallo non è l'ambra.** `unstable` dice «dato vecchio» e «cielo incerto»; un'allerta gialla
  è un'affermazione di un'autorità. Tre coppie nuove in `ChiaroColors`, misurate; il rosso può
  coincidere con `fail` se la misura lo permette, e si scrive se lo fa.
- **Attribuzione** CC BY 4.0 nel foglio, in Informazioni e nella guida: è un obbligo della
  licenza, ed è anche l'unico posto in cui il lettore scopre da dove viene l'allerta.
- **Confermate dal committente il 9 set 2026** le tre scelte che il piano dava per provvisorie:
  il default dalla gialla, il solo luogo attivo, il chip dei widget acceso.

### Rimasto fuori, con il motivo

- Le notifiche per le città appuntate ai widget: il job notifica il luogo attivo, e cambiare
  questo per le allerte sole sarebbe una regola in più da spiegare.
- I bollettini regionali (ogni Regione ha il suo sito e il suo formato) e gli orari di validità
  che il nazionale rimanda a loro: il foglio mostra la nota e si ferma.
- Il livello dei fenomeni della vigilanza (punti e icone) e la prosa nazionale su venti e
  temperature: senza legenda e senza un luogo, non c'è modo onesto di legarli a una città.
- IT-alert: è cell broadcast del sistema, non un feed.
- Una mappa delle zone: Chiaro non ha una mappa, e la decisione sul radar (VISION §12.5) è
  aperta apposta.

### Riferimenti per chi apre la fase (raccolti il 9 set 2026)

**Gli URL.** Base raw `https://raw.githubusercontent.com/pcm-dpc/<repo>/master/files/`, con
`<repo>` = `DPC-Bollettini-Criticita-Idrogeologica-Idraulica` oppure
`DPC-Bollettini-Vigilanza-Meteorologica`. Lo User-Agent dell'app passa dall'interceptor
condiviso: su `raw.githubusercontent.com` è l'unico host dove conta davvero.

- Criticità: `xml/<stamp>.zip` (dentro `Cap_<stamp>.xml`, `BCR_testo_DCAT_AP_IT.xml`,
  `README.txt`); `shp/<stamp>_shp.zip` (DBF con `Zona_all`, `Nome_zona`, `Criticita`, `Idrogeo`,
  `Temporali`, `Idraulico`; 187 record; è la sorgente dell'importatore); `topojson/<stamp>_today.json`
  e `_tomorrow.json` (proprietà `Nome zona`, `Comuni`, `Per rischio idraulico|temporali|idrogeologico`,
  `Rappresentata nella mappa`; 1,2 MB); `<stamp>.json` l'indice; `all/latest_all.zip` il ripiego.
  `<stamp>` = `AAAAMMGG_HHMM`; **`20260908_1519` è la fixture di test**, riscaricabile.
- Scoperta: `https://github.com/pcm-dpc/<repo>/commits/master.atom`, poi
  `https://github.com/pcm-dpc/<repo>/commit/<sha>.diff` del primo `<entry>`: la prima riga
  (`diff --git a/files/preview/<stamp>_domani.png …`) porta lo stamp. Entrambi dal sito, non
  dall'API.
- Vigilanza: `<AAAAMMGG>.json` (chiavi `today`, `tomorrow`, `aftertomorrow`, ciascuna con
  `attachment[]`, `topo_json[]`, `html_description`); `xml/<AAAAMMGG>.zip` (dentro
  `Cap_<AAAAMMGG>.xml`); `topojson/<AAAAMMGG>_oggi.json` (proprietà `Nome_Zona`, `comuni`,
  `id_classificazione`, `Quantitativi_previsti`; 71 zone).

**Il CAP della criticità.** `<identifier>DPC_BULLETIN_2026_09_08_6471</identifier>`,
`<sender>2.49.0.0.380.1</sender>`, `<sent>` con offset locale, un `<note>` nazionale con i
rinvii regionali; 8 `<info>`, ognuno con `<event>` nella forma «ORDINARIA CRITICITA' PER RISCHIO
IDRAULICO / ALLERTA GIALLA:» (parole chiave: IDRAULICO, IDROGEOLOGICO, TEMPORALI; GIALLA,
ARANCIONE, ROSSA), `<onset>`/`<expires>` che dicono il giorno, `<severity>` Moderate o Severe, e
le `<area>` con `<areaDesc>` = nome della zona e `<geocode>` `Zona di Allerta` = codice. Nei
file le etichette dei livelli sono «Assenza di fenomeni significativi prevedibili / NESSUNA
ALLERTA», «Ordinaria / ALLERTA GIALLA», «Moderata / ALLERTA ARANCIONE», «Elevata / ALLERTA
ROSSA».

**Il CAP della vigilanza.** 10 `<info>`, `<category>Precipitazioni previste</category>`,
`<event>` fra «Assenti o non rilevanti», «Deboli», «Moderati», «Elevati», `<geocode>` `id_zona`
numerico, `<onset>`/`<expires>` per il giorno.

**Punti d'aggancio nel codice** (stato al 9 set 2026, da rileggere prima di toccarli):

- `core/sync`: `SyncNotifiers.kt` (l'interfaccia e `SyncDependencies`); `WeatherSyncWorker.kt`
  (i passi: fetch → fuso e `cityKey` → `AlertEngine` → `RuleEngine` → città appuntate → cielo;
  il nuovo passo va dopo `AlertEngine`); `SyncScheduler.alertsWanted` e `shouldRun`.
- `app/notifications`: `AlertNotifier.kt` (canali creati pigramente, id fissi 1001-1003,
  `BigTextStyle`, `openApp`); `SkyNotifierTest.kt` (il modello del test); `ChiaroNotifiers.kt`.
- `core/data`: `ServiceLocator.build()` (l'OkHttp condiviso è una variabile locale: va issato in
  un campo per il quarto host; `overrideForTests`); `remote/dto/GeocodingDto.kt` e
  `WeatherRepository.toCity` (dove `country_code` si perde); `domain/model/GpsLocation.kt` e
  `CityStore.adoptGpsFix` (la seconda strada di `City`); `local/WeatherHistory.kt` (migrazioni
  1→4 e `ChiaroDatabaseMigrationTest`, che fissa a mano gli schemi vecchi); `AlertStateStore.kt`
  (l'idioma dell'anello di impronte in un DataStore suo).
- `app/ui/today`: `TodayUiState.kt` (`Content.whatChanged`, il modello per un campo riempito dal
  ViewModel e non dal builder); `TodayScreen.kt` `ContentState` (la `LazyColumn`: il banner dopo
  `FreshnessChip`, prima di `GuideCard`); `HeadlineEngine.kt` (la scala, primo che combacia
  vince) e `HeadlineText.kt` (i due registri).
- `app/ui/alerts`: `AlertsScreen.kt` (`ReadySwitch`, `somethingTurnedOn()` per il permesso);
  `AlertsViewModel.mutate` (riconcilia il job dopo ogni modifica).
- `app/ui/theme`: `ChiaroColors.kt` (quattro palette, le due Brillante generate da
  `tools/gen_vivid.py`: non si ritoccano a mano); `PaletteContrastTest`; `PaletteDocTest` (legge
  i rapporti stampati in DESIGN §2.3); `NoRawColorTest`; `ui/StringsParityTest`; `ui/format/Formats`.
- `app/widget`: `WidgetLookStore.kt` (`WidgetLook`, chiavi `*_$id`, `forget`);
  `arc/ArcSettings.kt` (`ArcSettingsCodec`, il test che il codec scrive esattamente le chiavi che
  legge); `WidgetData.kt` (`WidgetModel`, `load`); `WidgetUi.kt` (`verdictInk`/`verdictContainer`
  con il fondo della card, non il tema); `SkyWidget.kt` `VerdictChip` (la grammatica del chip);
  i quattro `*LayoutTest`.

**Sequenza suggerita, un PR per passo**, ognuno verde da solo: (1) dominio, importatore, asset,
i due campi di `City`, la voce in `UPSTREAM.md`; (2) sorgente, store, tabella e migrazione, passo
nel job, notifier con il suo test; (3) Oggi, foglio, Avvisi, Diario, guida, token in DESIGN,
stringhe; (4) widget, con il giro di screenshot su device. La settimana di collaudo parte al
merge del (3).

### Il primo PR: dominio, importatore, asset, i due campi di `City` (9 set 2026)

Branch `claude/warnings-domain-9set-k3p8q1`. Tutto misurato sui file veri del bollettino
`20260908_1519`, scaricati quel giorno.

**Le zone sono 187, non 156.** Il DBF dello shapefile ha 187 record, 187 codici `Zona_all`
distinti e 187 nomi distinti, un record (multi-parte) per zona; i 187 nomi del TopoJSON sono
gli stessi. Il «156 zone (187 poligoni)» della tabella delle misure era un conteggio sbagliato:
niente cambia nel modello, cambia il numero che si legge qui. Le altre misure di quel giorno
reggono, con due precisazioni sui file che questo PR legge: nei 8 182 nomi di comune del
TopoJSON della criticità **non c'è nessun U+FFFD** (il «Citt� Sant'Angelo» sta nella vigilanza,
che legge il secondo PR), e l'unico danno di codepage nei nomi di zona è un apostrofo perso in
due nomi della Valle d'Aosta (`Valle d?Aosta`), che l'importatore ripara perché un `?` fra due
lettere in un toponimo non può essere altro. **Tredici zone hanno per nome il loro codice**
(sette della Basilicata, sei delle Marche: «Basi-A1», «Marc-4»): la Regione non ha mai dato
loro un nome, e «nessun codice di zona raggiunge lo schermo» dovrà fare i conti con loro prima
che il foglio esista — decisione del terzo PR, registrata qui perché la misura è di questo.
Altre due misure che servono a chi scrive il foglio: 273 comuni stanno in più di una zona
(Roma in cinque), e per Reggio Calabria il Dipartimento scrive «Reggio di Calabria» dove
Open-Meteo dice «Reggio Calabria» — il ripiego per nome fallisce e la geometria vince, che è
la prova di «geometria prima dei nomi» su un capoluogo.

**L'importatore** (`tools/build_warning_zones.py`, solo libreria standard: DBF e SHP letti a
mano, come `import_meteocons.py` non chiede pacchetti). La regione viene dal prefisso del
codice (venti prefissi, tabella nel file; un prefisso ignoto ferma la build). La geometria:
Douglas-Peucker per anello in un riferimento metrico (la longitudine scalata a 42°N), poi
quantizzazione a 10⁻⁴ gradi (11 m di latitudine, 8 di longitudine) con codifica a differenze
lungo l'anello; gli anelli che collassano sotto il triangolo si scartano (633 → 457: scogli e
isolotti). La tolleranza è scritta nell'asset e l'indice la conosce.

| Tolleranza | Punti | Asset (con 8 295 chiavi di comune, ~130 KB) |
|---|---|---|
| grezzo | 136 562 | — |
| 100 m | 73 067 | 630 KB |
| 250 m | 34 260 | 389 KB |
| **500 m** | **18 723** | **284 KB** |
| 1000 m | 10 130 | 219 KB |

**Scelti 500 m**: 250 m spende tutto il budget dei 400 KB; la strada della posizione arrotonda
già il fix su una griglia di ~1,1 km (`City.cacheKey`, fino a 680 m di spostamento) e un
comune geocodificato sta a chilometri dal bordo della sua zona, quindi niente che l'app
localizzi è più fino di mezzo chilometro. La fascia di bordo che l'indice cede al comune è
larga quanto la tolleranza in ogni caso. Il JSON usa chiavi leggibili (`code`, `name`,
`region`, `rings`, `comuni`): abbreviarle avrebbe reso ~8 KB. I nomi dei comuni sono
normalizzati una volta, nell'importatore, e allo stesso modo nel dominio
(`WarningZoneIndex.normalizeComune`, con il commento «cambiare entrambi»): NFD senza segni
combinanti, minuscolo, apostrofi raddrizzati, spazi compressi; i nomi bilingui
(`Bolzano/Bozen`) si indicizzano per metà (8 182 voci → 8 295 chiavi); solo il lato della
ricerca toglie un «Comune di » iniziale, che è come Open-Meteo scrive `admin3` (ma «Roma» e
«Genova» arrivano senza).

**L'indice** (`WarningZoneIndex`, puro, decodifica il JSON con kotlinx.serialization in
0,12 s nel test compresi 10 casi). `locate(punto, comune?)` decide in tre passi, ognuno solo
se il precedente non ha deciso: (1) una zona che contiene il punto a più della tolleranza dal
suo bordo è certa (due zone certe sarebbero una sovrapposizione della sorgente: il comune, poi
la più profonda); (2) nella fascia di bordo — dentro un poligono o entro la tolleranza da uno
— decide il comune se nomina esattamente una delle zone lì, altrimenti la zona in cui il punto
è più dentro, altrimenti la più vicina (una spiaggia che la costa semplificata ha spostato a
terra); (3) fuori da ogni poligono il comune da solo, se appartiene a una zona sola. Il
contenimento è pari-impari su tutti gli anelli della zona, così i buchi si contano da soli e
l'orientamento non serve; il filtro sul riquadro (allargato della tolleranza) viene prima.
`WarningZoneIndexTest` legge **il file che l'app spedisce** (la cartella degli asset di
`:core:data` è una risorsa di test di `:core:domain`, dichiarata in `build.gradle.kts` con il
motivo), e fissa 22 capoluoghi e comuni con le coordinate del geocoding del 9 set: tutti per
sola geometria, tra cui Perugia nella fascia di bordo (decide la profondità, 172 m) e Palermo
sulla costa (decide il comune); tre punti in mare e Lugano in nessuna zona; il confine
Piemonte/Lombardia a 45.0353 N 8.8225 E dove il comune sposta il punto da una parte o
dall'altra e un comune di un'altra zona non lo sposta; Genova 3 km al largo per nome; Roma per
nome da sola in nessuna zona perché è in cinque; `Forlì`/`FORLI`/`Comune di Forlì` e `Bozen`.

**Il modello** (`warnings/WarningModel.kt`): `WarningLevel` in ordine di gravità così `maxOf` e
`>=` dicono quel che dicono; `WarningHazard` con `displayOrder` = la precedenza del Dipartimento
a pari livello (Idraulico, Temporali, Idrogeologico, dal README dello shapefile), che tutte le
superfici useranno; `ZoneWarning` è la riga piatta (zona, giorno, rischio, livello) e
`WarningBulletin` elenca solo le righe sopra NESSUNA — quel che non nomina è verde, come le zone
assenti dal CAP; `PlaceWarnings` porta la zona, i giorni ancora davanti con tutti e tre i
rischi per giorno, e la nota. **Il verde è un valore**: `forPlace` torna `null` solo senza zona
o con tutti i giorni passati; una zona verde torna con tutto a NESSUNA, che è la riga «Nessuna
allerta» di Avvisi. I giorni passati si tolgono (prima delle 15:30 il «domani» di ieri è
l'unico giorno). La nota viaggia solo se nomina la regione della zona **o la zona stessa** (le
due zone trentine si chiamano come la loro provincia autonoma), confrontata normalizzata e
senza trattini («Friuli Venezia Giulia» nella nota, «Friuli-Venezia Giulia» nell'indice).
`notificationFor` costruisce l'impronta `"$cityKey:warn:$bulletinId:$maxLevel"` e legge le
impronte bruciate **per prefisso**: stesso bollettino a un livello uguale o più alto già
notificato → silenzio, così l'«Aggiornamento» che sale è notizia e la correzione che scende è
del Diario. `OfficialWarningEngineTest`, 18 casi in tabella.

**`City` e le due strade.** `countryCode` e `admin3`, nullable con default (le liste salvate
prima decodificano identiche; `encodeDefaults` è falso, quindi i null non si scrivono).
`GeoResultDto` ha `admin3` (il `country_code` arrivava già), `toCity` passa entrambi;
`GeoFix` ha gli stessi due campi, `toGpsCity` li porta, `adoptGpsFix` li tratta come il nome
(li aggiorna se il fix li sa, li tiene se no), `geocodedPlace` legge `Address.countryCode` e
la prima `locality` della scala — il comune anche quando il nome è un quartiere. **È la prima
divergenza voluta di `LocationProvider.kt` da tweather**, quattro righe additive, registrata
in `UPSTREAM.md` con la raccomandazione di portarle a monte.

**`WarningZoneAssets`** in `:core:data/warnings/` è l'unica riga Android: apre l'asset e lo
passa al dominio; il suo test Robolectric prova che l'asset è nella libreria e si apre.

Verifica del PR: `:core:domain` 195 test, `:core:data` 182, tutti verdi al primo giro; la suite
completa come la CI (`test :app:testDebugUnitTest :app:lintDebug`) 639 test — dominio 195, dati 182,
sync 11, app 251 — e lint 0 errori, nessun avviso sui file nuovi.

### Il secondo PR: sorgente, store, tabella, passo nel job, notifier (9 set 2026)

Branch `claude/warnings-source-9set-m7r2v4`, impilato sul primo (il #24 non era ancora fuso).
Quattro caselle in più della lista: `:core:data/warnings/`, la scoperta, la persistenza con la
migrazione, il passo nel job, `SyncNotifiers` e il notifier. Le stringhe del notifier entrano
qui (`notif_warning_*`, `notif_channel_warning_*`, `warning_*`), `StringsParityTest` verde.

**Misure nuove, dal vivo il 9 set.** Il bollettino del giorno è uscito alle 15:46
(`20260909_1546`, su GitHub alle 15:56): CAP di 79 KB, 10 blocchi, 385 aree, nota «Regioni
Lombardia e Liguria … ai rispettivi bollettini regionali». **L'Atom dei commit onora
`If-None-Match`**: la richiesta condizionale torna 304 con zero byte, quindi il ricontrollo
orario di un bollettino già in mano non costa niente (`WarningFetchState.feedTag`, l'ETag
salvato nello store). **Il `.diff` di un commit non è sempre 200 B**: quello di un commit di
metà pipeline (il GeoJSON) pesa 5,5 MB. La sorgente legge la prima riga e chiude la
connessione — lo stamp sta nel nome del file, qualunque file sia — e prova in ordine i tre
commit più recenti, saltando quelli senza stamp (un commit al README). `latest_all.zip`
contiene il CAP accanto a shapefile e PDF, quindi il ripiego su rete non a consumo funziona e
lo stamp si legge dal nome dell'entry. Un 404 su `xml/<stamp>.zip` vuol dire «pipeline non
arrivata»: si tiene il bollettino precedente e si dimentica l'ETag, così il giro dopo rilegge
l'Atom per intero.

**La sorgente** (`WarningSource`, `DpcBulletinSource`): scoperta dal sito, mai dall'API;
`CapParser` è un pull-parser sottile (`android.util.Xml`) che produce il `CapAlert` piatto e
non sa nulla di livelli; `DpcBulletinReader`, nel dominio, legge rischio e livello dalle
etichette per parole chiave (IDRAULIC, IDROGEOLOGIC, TEMPORAL; GIALLA, ARANCIONE, ROSSA,
NESSUNA ALLERTA) e **salta un blocco che non sa leggere invece di indovinarlo**. I giorni del
bollettino sono gli onset dei blocchi uniti al giorno d'emissione e al successivo, perché un
giorno tutto verde non ha blocchi in cui trovarsi. Le fixture sono file veri: il CAP dell'8 set
(45 KB), l'Atom del 9 (18 KB) e il diff del primo commit (216 B), i primi file non Kotlin sotto
`src/test`; il test della sorgente usa un interceptor OkHttp come trasporto e **asserisce le
richieste fatte**, non solo il risultato (tre per un bollettino nuovo, una per il 304).

**La cadenza è pura** (`WarningFetchPolicy`, tabella): niente in mano → ogni giro chiede;
bollettino di ieri prima delle 15:30 → no; dalle 15:30 finché non c'è quello di oggi → sì;
quello di oggi in mano → un'occhiata l'ora fino alle 21. **Il confronto fra due bollettini è
puro** (`WarningDiff`): il primo bollettino che un luogo vede è il suo stato, non una notizia
— nessuna riga nel Diario; un giorno che il vecchio non copriva parte da verde; un cambio di
zona non si confronta.

**Il passo** (`OfficialWarningsStep`, in `:core:sync`) gira **prima del cancello
`alertsWanted`**, subito dopo il fetch riuscito, dentro `runCatching`: il bollettino è contenuto
per Oggi e per i widget, non solo una notifica, e se parlare lo decide il suo interruttore.
`alertsWanted` include `officialWarnings`, così chi vuole solo le allerte tiene vivo il job.
Inerte se nessun luogo salvato cade in una zona. La rete a consumo si legge da
`ConnectivityManager.isActiveNetworkMetered` (permesso dichiarato nel manifest di `:core:sync`,
già fuso da WorkManager). La riga «bollettino non raggiunto» si scrive solo dopo le 15:30, se
quello di oggi manca, una volta al giorno (`failureLoggedOn` nello store). **Accettato**: un
fetch meteo fallito salta anche il passo (offline lo sono entrambi; un `ApiError` di Open-Meteo
fa uscire il worker prima) e il giro successivo recupera.

**Le impostazioni**: `NotificationSettings.officialWarnings` (acceso) e `officialWarningsFrom`
(gialla), chiavi `notif_official_warnings` e `notif_official_warnings_from`, additive; i loro
interruttori in Avvisi sono del terzo PR. **Lo store** (`OfficialWarningStore`, DataStore
`warnings`): il bollettino corrente per sorgente come JSON con le date ISO, l'ETag, l'ultimo
tentativo, il giorno dell'ultima riga di fallimento, l'anello delle impronte (40). Una riga con
un rischio o un livello che questa build non conosce si scarta in lettura, non si indovina.
**La tabella** `warning_records` (migrazione 4→5, additiva; `ChiaroDatabase.MIGRATIONS` è ora
l'unica lista, condivisa da builder e test) porta `kind` (`LEVEL_CHANGE` o `BULLETIN_MISSED`),
il bollettino, la zona, il giorno, il rischio e i due livelli, tutti come nomi e stringhe ISO:
il Diario li rende nella lingua del lettore al momento della lettura.

**Il notifier** (`OfficialWarningNotifier`): due canali (`warning_high` alta, `warning_yellow`
normale), id `3000 + id % 1000` e `3000` per la posizione, titolo «Allerta arancione · Milano»,
chiusa «Temporali, oggi fino a mezzanotte · bollettino delle 15:46», espansa con la chiusa in
testa e poi un fatto per riga (i livelli per giorno, la zona, «Cosa vuol dire», la nota se c'è,
la fonte per ultima). Due grammatiche per il livello: la frase è «Allerta gialla», il colore
nelle righe dei giorni è «temporali giallo». **I significati dei livelli sono il riassunto che
il piano dava, in italiano nelle due lingue** (`warning_meaning_*`): non una citazione
letterale; se il committente vuole le parole esatte del Dipartimento con la loro fonte, il
terzo PR sostituisce tre stringhe. `OfficialWarningNotifierTest` sul modello di
`SkyNotifierTest`, con il controllo che né codice di zona né identificativo raggiungano il
testo.

**Rinviato al terzo PR, con il motivo: la vigilanza.** Vuole un secondo asset (le 71 zone di
vigilanza dal TopoJSON `_oggi.json`, con i loro comuni) che il primo PR non ha costruito, un
secondo documento nello store e la sua cadenza dalle 14:30; la sua unica superficie è una riga
del foglio. Entra con il foglio, non prima.

**`ServiceLocator`** issa OkHttp e il database in campi (la sorgente condivide il client per lo
User-Agent, il DAO condivide il database) e cresce di quattro accessori e di quattro parametri
in `overrideForTests`.

Verifica del PR: suite completa come la CI, **695 test** — dominio 213 (+18), dati 201 (+19),
sync 21 (+10), app 260 (+9) — e lint 0 errori. Un solo giro rosso, per due sviste del test e
non del codice: il CAP dell'8 set dava il Versante Jonico Settentrionale giallo su tutti e tre i
rischi, non sul solo idraulico come il test supponeva (il file ha ragione, il test è stato
corretto e dice perché); e `cancelAll` va chiamato sul gestore vero, non sulla shadow.

### Il terzo PR: Oggi, il foglio, Avvisi, il Diario, la guida, i token, le stringhe (9 set 2026)

Branch `claude/adoring-bell-viiqyl`, impilato sui primi due. Sei caselle della lista: le
superfici. Da qui il bollettino si vede, e da qui parte la settimana di collaudo.

**I tre token di livello, misurati.** Una regola sola per tutti e tre, così l'unica cosa
che cambia fra loro è la tinta: l'inchiostro a **8,5:1** sul fondo chiaro e a **11,0:1** su
quello scuro, il contenitore al bordo del gamut sRGB per la sua tinta a luminanza tenuta.
I contenitori stanno **più in basso di quelli dei verdetti** (luminanza .62 contro .78) e
il motivo è una misura: a .78 sRGB tiene .099, .046 e .038 di croma per queste tre tinte e
arancione e rosso escono lo stesso rosa pallido; a .62 i tre tetti sono .175, .088 e .075 e
il giallo è un giallo. L'inchiostro rosso chiaro cade a due unità da `fail` (`#990003`
contro `#950700`) — è quello che fanno una tinta condivisa e un contrasto condiviso — e
resta un token suo, perché le due affermazioni non sono la stessa. Tutti e sei escono dal
generatore identici: `gen_vivid.py` cresce a sette coppie (`PAIRS`) e non ha niente da
prendere su valori già al bordo.

**La deuteranopia si misura, non si cita.** `Deuteranopia.kt` nei sorgenti di test è il
validatore: simulazione Viénot–Brettel–Mollon 1999 su sRGB lineare, distanza in ΔE\*ab
(CIE76), soglia percettiva 2,3. Il risultato in tabella in DESIGN §2.3: **in ogni schema uno
dei due portanti tiene circa nove decimi della sua distanza e l'altro ne tiene meno di un
decimo** — l'inchiostro collassa su carta, il contenitore collassa sullo scuro, e
arancione↔rosso è la coppia debole in entrambi i casi. `PaletteContrastTest` asserisce il
collasso come *tetto* (l'unica asserzione al contrario del file) e `PaletteDocTest` legge i
numeri stampati. Il ΔE 0,7 che §2.3 cita per `unstable`↔`fail` viene dal validatore di
tweather su una scala che qui non si riproduce (questo ne misura 6,4): la frase resta com'è
e il documento dice che le due cifre non sono confrontabili, invece di riscrivere una
misura che non si può rifare.

**Le tredici zone senza nome** (le sette della Basilicata, le sei delle Marche) sono la
decisione che il primo PR ha rinviato qui. In una frase si nomina la regione — «Allerta per
**una zona della regione Marche**» — e nella notifica la riga «Zona di allerta:», che dopo i
due punti non avrebbe altro che un codice, **non si disegna**: è la regola che quella
notifica già segue per ogni riga. Il sostantivo sta dentro la stringa apposta, perché
l'italiano vuole «in Basilicata» e «nelle Marche» e quelle due Regioni sono esattamente le
tredici.

**Il gradino zero della frase** prende arancione e rosso, mai il giallo, e il giorno lo
sceglie oggi prima di domani; i rischi nominati sono quelli **al picco del giorno
nominato**, così «arancione per temporali» non diventa «per temporali e rischio
idrogeologico» perché il secondo era giallo. Le due forme sono parallele («… , oggi» / «… ,
domani») invece della «Domani allerta rossa …» che il piano dava: stessa frase in due
lingue senza far girare le maiuscole, e il registro breve dei widget resta «Allerta
arancione · temporali».

**Il verde è un valore, e vale in un posto solo.** `TodayStateBuilder` scarta un bollettino
tutto NESSUNA prima di costruire: niente banner e niente frase (§1.1). Avvisi disegna tutti
e quattro gli stati di `PlaceWarningState` — non in zona, nessun bollettino ancora,
bollettino non più valido per oggi, e quello corrente anche se verde — perché lì l'assenza
**è** la risposta cercata. È la stessa lettura in due screen con due regole opposte, ed è
il motivo per cui lo stato è un tipo (`OfficialWarningReader.state`, puro, sei casi in
tabella) e non un `PlaceWarnings?`.

**Il glifo del Diario si è mosso.** Il triangolo di Material passa alla riga dell'autorità —
è il segno dell'allerta in tutto il resto dell'app — e le due righe «un aggiornamento non è
arrivato» (il fetch fallito, il bollettino non raggiunto) prendono `Refresh` e condividono
una categoria sola. La regola del Diario non cambia: il glifo nomina la categoria, quindi
due categorie non possono averne uno.

**Il vocabolario è uno**, `ui/warnings/WarningText`, e la notifica del secondo PR ci si è
appoggiata invece di tenersi le sue mappature: due copie di «Allerta arancione per
temporali» divergono, e la divergenza si vede come una notifica e un banner che parlano di
bollettini diversi. Da lì `PlaceWarnings.ranked` e `peakDays` sono saliti nel dominio, dove
il widget del quarto PR li troverà già scritti.

**La lettura non chiede mai rete.** `OfficialWarningReader` segue lo store; l'indice delle
zone è un *fornitore* e non un campo, così un lettore con tutti i luoghi all'estero non
decodifica mai i 290 KB dell'asset. Il giorno con cui si legge è quello dell'**emittente**,
e il minuto di Oggi lo rivaluta: una pagina lasciata aperta oltre la mezzanotte smette di
mostrare i livelli di ieri.

**Rinviata al quarto passo, con la misura: la vigilanza.** Il secondo PR l'aveva mandata
qui «col foglio». Misurato il 9 set: il TopoJSON delle 71 zone pesa **807 KB grezzi**, è in
TopoJSON (archi delta-codificati con `transform`, 1 893 archi) e non in shapefile, quindi
l'importatore della criticità non lo legge — gli manca un decodificatore di archi che non
ha mai avuto — e le sue proprietà sono `Nome_Zona`, `comuni`, `id_classificazione`,
`Quantitativi_previsti`: **non c'è `id_zona`**, che è il geocodice con cui il CAP della
vigilanza nomina le zone, quindi il join va rimisurato prima di scriverlo. Sono un asset
nuovo da costruire e da pesare, un secondo documento nello store, una seconda cadenza e una
seconda sorgente, per una riga del foglio: è un PR suo, e ci va con la sua ricerca, non
appoggiato a una funzione finita.

Verifica del PR: suite completa come la CI, **731 test** — dominio 213 (invariato), dati
207 (+6), sync 21 (invariato), app 290 (+30) — e lint 0 errori, nessun avviso sui file nuovi (l'unico che era comparso,
`UseKtx` su `WarningSheet`, è stato tolto passando a `String.toUri()`).

### Il quarto PR: il chip sui widget, e due contenitori Glance che perdevano figli (9 set 2026)

Branch `claude/adoring-bell-viiqyl`, sopra il terzo. L'ultima casella di codice della
fase: l'allerta sulla schermata home.

**La regola è una tabella, e sta in un posto solo** (`WidgetWarning.kt`, `warningSlot`).
Quattro card dovevano essere d'accordo su quando la frase sta già dicendo l'allerta, e
quattro copie di una regola sono quattro occasioni per non esserlo. Decide quello che la
card **sta già dicendo**: la frase del giorno nel registro breve *è* l'arancione e il
rosso, quindi dove quella frase c'è la pastiglia non si disegna; dove non c'è — il lettore
l'ha spenta, o l'eroe dell'Arco sta mostrando il prossimo momento di luce — la pastiglia
prende il suo posto e la card non cresce di niente. Il giallo la frase non lo porta mai,
quindi vuole una riga sua e compare solo dove la forma ne ha una in più.

**La frase del widget ora porta davvero l'allerta.** `WidgetData.load` costruiva il
contenuto senza passare il bollettino a `TodayStateBuilder`, quindi il gradino zero della
frase non arrivava mai su una card: la pastiglia si sarebbe disegnata accanto a una frase
che dell'allerta non diceva niente — il widget e l'app che raccontano due pomeriggi
diversi, che è la cosa che quell'oggetto esiste per impedire. Il bollettino si legge ora
**prima** del contenuto, e il contenuto si costruisce con quello.

**Il costo è un numero solo** (`warningChipHeight`, 20,52 dp alla dimensione di default,
più 4 di aria) e ogni budget lo sottrae. Dove la riga non c'è, la pastiglia non si
disegna: sul 2×2 di riferimento del widget Ora con la frase accesa il glifo scenderebbe a
44,4 dp contro il pavimento di 52 della famiglia, quindi il giallo resta a casa; con la
frase spenta le sue due righe la pagano due volte. Su Oggi la riga della pioggia cede per
prima, come già cede al marcatore di dato vecchio. Sull'Arco l'agenda molla una riga prima
che il disegno molli un pixel (pannello di riferimento: intestazione 39,6 → 64,1, agenda
2 → 1, arco fermo ai suoi 56 preferiti).

**I colori vengono dal fondo della card** (`WidgetPalette.colors`), mai dal tema del
telefono: una card chiara sotto un tema scuro porterebbe l'inchiostro del set scuro sul
contenitore di quello chiaro, e la coppia smetterebbe di essere quella misurata. È la
stessa correzione che il segno dell'agenda dell'Arco ha già avuto il 9 set.

**Contando i figli di Glance sono venuti fuori due contenitori che ne perdevano.** Il
limite è dieci per contenitore e oltre quello Glance lascia cadere senza dire niente (la
scoperta del 9 set sull'Arco). Misurati tutti quelli che questa fase tocca, e due erano
già oltre:

| Contenitore | Prima | Ora |
|---|---|---|
| Striscia delle ore di Oggi | 7 celle + 6 spaziatori = **13** | 7 |
| Colonna della card Cielo (forma alta) | 3 + 5 righe + 4 spaziatori + 1 = **13** | 9 |
| Colonna della frase di Ora (largo) | 1 | 2 |
| Colonna delle parole di Ora (alto) | 4 | 5 |
| Colonna di coda di Oggi | 2 | 3 |
| Colonna della card dell'Arco | 6 | 7 |
| Colonna del pannello dell'Arco | 6 | 7 |

Sulla striscia di Oggi il conto è **esattamente la card su cui il widget è disegnato**:
quattro celle sul dispositivo di riferimento danno sette ore, e le ultime due erano
scartate in silenzio. Sul Cielo servono cinque righe sotto l'eroe, che una card da tre
righe con sei sottoscrizioni ha. La cura è quella che l'Arco ha già usato: gli spazi come
**padding dentro una scatola più alta**, non come spaziatori — la geometria non cambia di
un dp e il contenitore ha un figlio per riga invece di due. Le celle della striscia
prendono metà spazio per lato invece dell'intero tra l'una e l'altra, così restano una
griglia sola: l'inchiostro della striscia cede 3 dp per capo, ed è tutta la differenza
visibile su una card da 340. La pastiglia è **un figlio solo** per la stessa ragione
(`WarningChipRow`: l'aria sopra è il padding del contenitore, non uno spaziatore).

**L'interruttore per istanza è acceso** su tutte e tre: `WidgetLook.showWarning` (Ora,
Oggi) e `ArcSettings.warning` (Arco, diciottesima chiave del codec). Cielo non lo offre —
quella card parla dei momenti del cielo, e una pastiglia sul suolo sarebbe un secondo
soggetto. Le anteprime del picker non cambiano. L'anteprima dell'Arco sì, e deve: disegna
dallo stesso piano e dallo stesso slot, perché una schermata di impostazioni che mostra
una pastiglia che la home non disegnerebbe è esattamente il bug che quel file esiste per
non avere.

**Il ridisegno.** Il passo che scrive il bollettino chiama `repaintAll` — un bollettino può
arrivare in un giro il cui meteo veniva dalla cache, e allora il gancio del commit del
repository non scatta mai — e lo store entra nel collettore di processo di
`ChiaroApplication`, accanto al luogo attivo e alle impostazioni.

**Il lettore non paga l'asset se non deve.** `OfficialWarningReader.graded` chiede prima
allo store e solo dopo all'indice: il passo è inerte se nessun luogo salvato cade in una
zona, quindi chi ha tutti i luoghi all'estero non ha nessun bollettino in memoria e non
decodifica mai i 290 KB su un ridisegno della home. Il test lo prova con un fornitore
dell'indice che lancia.

Verifica del PR: suite completa come la CI, **754 test** — dominio 213, dati 208 (+1),
sync 21, app 312 (+22) — e lint 0 errori, nessun avviso sui file nuovi. Resta il giro di
screenshot su device, che è del committente.

### Verifica

Suite come la CI: **754 test** — dominio 213, dati 208, sync 21, app 312 — e lint 0 errori
(quarto PR, 9 set 2026).

**Il collaudo, chiuso dal committente l'11 set 2026.** Doveva durare una settimana ed è durato
due giorni: varie località verificate contro i bollettini veri. Quel che questo conferma e quel
che non conferma, perché la differenza conta:

- **Confermato**: la zona che l'indice trova per un luogo, il banner e il foglio su bollettini
  veri, il livello di notifica di partenza (**gialla**, non si alza).
- **Non attraversato**, perché due giorni di settembre non li contengono: un'allerta rossa, un
  «Aggiornamento» che alza il livello a bollettino invariato, la riga «bollettino non raggiunto»,
  e il passaggio di mezzanotte con la pagina aperta. Restano coperti dai test e non dal campo,
  ed è giusto che il documento lo dica invece di far passare due giorni per una settimana.

### La lacuna dell'apertura (trovata l'11 set 2026, ancora aperta)

Il piano della fase diceva, in «La cadenza del passo»: «**All'apertura** di Oggi, se è passata
l'ora e il bollettino di oggi non c'è: in silenzio… un trascinamento per aggiornare lo forza».
**Non è mai stato cablato.** Verificato l'11 set cercando tutti i chiamanti:

- `OfficialWarningsStep` è invocato da **un posto solo**, `WeatherSyncWorker`;
- `SyncScheduler.reconcile` accoda solo lavoro **periodico** (`enqueueUniquePeriodicWork`), mai
  un colpo singolo;
- il trascinamento di Oggi chiama `repository.getWeather(forceRefresh = true)` e **non tocca il
  passo**.

Conseguenza: su un'installazione fresca non compare **nessuna** allerta — italiana o no — finché
WorkManager non fa scattare il job, e con l'intervallo di default (`DefaultUpdateFrequencyMin`
= **60**) può essere un'ora. Il collaudo italiano non l'ha vista perché l'app era in uso da
giorni e il job aveva già girato; si è vista solo installando l'APK e guardando subito.

Non era un difetto di correttezza — il dato arrivava, tardi — ma rendeva la funzione
impossibile da provare in trenta secondi.

**Chiusa lo stesso giorno** con `sync/WarningRefresh`, l'unico ingresso che permette a una
schermata di far girare la gamba delle allerte del job. Oggi lo chiama all'apertura e sul
trascinamento; il parametro `force` del passo esisteva già ed era proprio per questo.

Tre decisioni, ognuna piccola e ognuna con il suo motivo:

- **Scarica, non parla.** Il passo accetta ora `notifiers: SyncNotifiers?` e **null vuol dire
  «questa esecuzione non parla»** — un concetto solo invece di un flag più un oggetto finto.
  È la stessa divisione che il resto dell'app segue già: aprire Oggi scarica il meteo e non
  ha mai mandato una notifica; parlare è del job. L'impronta resta **non bruciata**, quindi
  se il lettore chiude l'app prima di accorgersene il job glielo dice lo stesso.
- **Una alla volta.** Oggi è un pager: atterrarci sottoscrive un flusso per luogo salvato e
  ognuno chiede. La cadenza **non** li ferma su un'installazione fresca, dove `shouldFetch`
  risponde sì finché non c'è un bollettino in mano — cinque luoghi sarebbero stati cinque
  download dello stesso documento. Il primo vince, gli altri tornano subito: volevano tutti
  lo stesso documento, e il collettore dello store lo consegna a ogni pagina comunque.
- **Il risparmio energetico vale qui come per il meteo**: l'esecuzione che nessuno ha chiesto
  si salta sotto risparmio, mai il trascinamento e mai una pagina che non ha ancora nessun
  bollettino.

Verifica: **757 test** (sync 21 → 24), lint 0 errori. I tre test nuovi fissano quel che conta
— un'esecuzione muta tiene il contenuto, non parla, non brucia l'impronta, e scrive lo stesso
nel Diario, perché il diario è contenuto e non parola. **Non testato**: il cablaggio dentro
`TodayViewModel`, che non ha test perché quella classe non è mai stata costruita in un test;
le tre righe stanno alla stessa copertura del `fetch` che hanno accanto.

## Fase 12 — Allerte ufficiali, secondo strato: MeteoAlarm

Pianificata il 9 set 2026 insieme alla Fase 11; parte quando la 11 ha passato la sua settimana.
Aggiunge un adattatore e cambia una riga: il modello, le superfici, le notifiche e i widget sono
quelli della Fase 11. **In Italia continua a fare fede la Protezione Civile**: il feed italiano di
MeteoAlarm non si legge mai.

- [x] Spike, fatto l'11 set 2026 (sotto). L'EDR è ancora chiuso, il ripiego per nome che questa
      casella dava non regge alla misura, e le geometrie esistono pubblicate altrove
- [ ] **Prima di riaprire la fase**: la lacuna dell'apertura della Fase 11 (sopra), e il
      «fallire chiuso» che il prototipo ha dimostrato mancante (sotto). Le caselle che seguono
      sono state tutte scritte e misurate su un branch l'11 set 2026, e **non sono state fuse**:
      il motivo è in «Il prototipo, e perché non è stato fuso»
- [ ] `MeteoAlarmSource`: un Atom per ogni paese dei luoghi salvati fuori dall'Italia
      (`meteoalarm-legacy-atom-<paese>`, ~80 KB per l'Italia oggi), con richieste condizionali
      (ETag / If-Modified-Since — da verificare nello spike: ogni giro del job che scarica 80 KB
      per paese è 2 MB al giorno, un 304 costa niente); il CAP solo per le voci dell'area del
      luogo; lo stesso pull-parser della Fase 11, perché il CAP è lo stesso CAP 1.2
- [ ] La mappatura: `awareness_level` 2/3/4 → giallo/arancione/rosso; `awareness_type` →
      `WarningHazard`, che cresce dei quattordici tipi (vento, neve e ghiaccio, temporali,
      nebbia, caldo, freddo, eventi costieri, incendi, valanghe, pioggia, alluvione,
      pioggia-alluvione, pericolo marino, siccità) con le loro parole in IT e EN
- [ ] I testi: i blocchi `info` arrivano in inglese e nella lingua nazionale; si sceglie quella
      del lettore se c'è, altrimenti l'inglese, e si cita (VISION §8). `onset` ed `expires` sono
      ore precise: la riga «dalle 11 alle 23» sostituisce «oggi» nel banner e nella notifica
- [ ] Precedenza per `countryCode`: `IT` → solo DPC; altrove MeteoAlarm; la riga «disponibili per
      i luoghi in Italia» di Avvisi diventa «in Italia e in 32 paesi europei», e fuori da entrambi
      resta onesta
- [ ] Attribuzione come i termini chiedono: il servizio nazionale (`senderName`) e l'ora di
      emissione nel banner, nel foglio e nella notifica; «EUMETNET – MeteoAlarm» solo se mai si
      mostrassero più paesi insieme, cosa che un luogo alla volta non fa
- [ ] Impronte con l'identificativo CAP e il livello; stesse due canali, stessi id
- [ ] Widget e Diario: nessun lavoro, stesso modello — ed è il test che la Fase 11 ha disegnato
      bene

### Lo spike, e le tre cose che ha cambiato (11 set 2026)

Misurato dal vivo quel giorno, non letto in una documentazione. Lo spike doveva sciogliere una
domanda (l'EDR è aperto?) e ne ha sciolte tre, due delle quali cambiano il piano.

| Cosa | Misura |
|---|---|
| EDR | Ancora **chiuso**: `/edr/v1/collections?f=json` risponde 200 e descrive i filtri, `/edr/v1/collections/warnings/locations` risponde **401 `{"error":"Unauthorized"}`**. Invariato dal 9 set |
| Richieste condizionali | **Non onorate.** Nessun `ETag` e nessun `Last-Modified` su **nessuno** dei 39 feed; `If-Modified-Since` torna 200 col corpo intero; `Range: bytes=0-1023` torna 200 col corpo intero; `Accept-Encoding: gzip` non comprime (85 251 B chiesti, 85 251 B serviti). `cache-control: max-age=0, private, must-revalidate`, e nessun validatore da rivalidare |
| `HEAD` | Funziona e dà `content-length`. **Scartato come validatore**: «Yellow» e «Orange» hanno le stesse sei lettere, quindi il salto di livello — il caso che più conta — non muove la lunghezza |
| Il feed `<updated>` | **È un timestamp di contenuto, non di generazione**: 5 letture in 2 minuti su IT, AT e FR danno lo stesso `<updated>` e corpi identici byte per byte. Sta a un offset stabile di **1 381–1 385 B**, cioè dentro la prima `recv` |
| I 39 feed | 24 h di misura: 22 attivi, 17 vuoti (986 B). Totale 1 295 KB, media 33 KB, massimo **200 KB** (Israele), poi Austria 182, Slovenia 170 |
| `cap:event` | **60 forme distinte** fra i 22 feed attivi: «Yellow Thunderstorm Warning», «Thunderstormwarning», «Thunderstorms Level 1», «EXTREME HIGH TEMP», «near gale», e un produttore che perde il suo template (**`awareness_type=5, awareness_level=2`**, 47 voci). Inutilizzabile |
| `cap:severity` | Moderate 592, Severe 87, **Minor 28**: tre valori per quattro livelli, e il Minor non è nessuno dei tre colori. Inutilizzabile |
| I geocodici del feed | `EMMA_ID` in 14 paesi, **`NUTS3`** in FR/RO/MK, **`NUTS2`** in HU, **nessun geocodice** in EE/IL/NO/SI e in parte LV/NL. L'unico campo che c'è sempre è `cap:areaDesc` |
| `areaDesc` = la regione? | **No.** È admin1 solo in IT, RO e PT; altrove sono distretti (AT: 86 Bezirke), dipartimenti (FR), zone meteo (ES: «Costa - Ibiza y Formentera»), **bacini fluviali** (SI: «Gradaščica», «Ljubljanica») o **aree di mare** (NO, FI, EE, LV) |
| Le geometrie, dove sono | `MeteoAlarm_Geocodes_2026_07_31.json` nel progetto GitLab `meteoalarm-pm-group/documents` che i riferimenti già citavano per il profilo CAP: **33 MB di GeoJSON, 2 006 poligoni veri, 30 paesi, 935 750 punti**, proprietà `code`/`country`/`name`/`type`, tutti `EMMA_ID`, CRS84, CC BY 4.0. Quattro versioni datate nel repo, la più recente del 31 lug 2026 |
| La copertura | Incrociando i 707 avvisi dei 22 feed attivi con quel file: **413 per codice, 68 per nome, 226 non risolti = 68,0%**. Ma per paese: **17 paesi su 22 risolvono il 100%**; i non risolti sono EE (0/57), IL (0/77), NO (0/8), SI (0/73) e LV (9/20), e i loro nomi sono «Port of Tallinn», «Sea - Center», «Northern Baltic Sea», «Morje» — **mare e bacini fluviali, non luoghi in cui si abita** |

**Le tre cose che cambiano.**

1. **Le geometrie ci sono, e non dall'EDR.** La casella diceva «se l'EDR è aperto, di là;
   altrimenti una tabella di nomi». L'EDR è chiuso e la tabella di nomi non regge (riga
   `areaDesc` sopra): funzionerebbe per tre paesi su ventidue. La terza strada — un file
   ufficiale, pubblicato, versionato per data, con i poligoni veri e la stessa licenza dei feed —
   fa quello che l'EDR avrebbe fatto e lo fa offline. È la stessa forma della Fase 11: un
   importatore, un asset, un indice puro punto-nel-poligono.
2. **Il livello e il rischio si leggono solo dal CAP.** Il piano supponeva che `cap:event`
   («Orange Wind Warning») bastasse a evitare di scaricare il CAP per ogni voce. Con 60 forme e
   un produttore che stampa il proprio template, leggere il colore dall'`event` vuol dire
   sbagliarlo in silenzio: `awareness_level` e `awareness_type` nei `<parameter>` del CAP sono
   gli unici campi codificati, e sono quelli che si leggono. Il CAP si scarica sempre, per le sole
   voci dell'area del luogo (2,9 KB l'uno, misurato su AT).
3. **La cadenza non scende a un'ora.** «Da sciogliere» prevedeva che senza richieste condizionali
   il passo scendesse a una lettura l'ora per paese. Non serve: il `<updated>` del feed è un
   timestamp di contenuto e sta nei primi 1 385 byte, quindi la sorgente legge il prefisso,
   confronta e **chiude la connessione** — lo stesso idioma con cui `DpcBulletinSource` legge
   la prima riga di un `.diff` da 5,5 MB. Una lettura invariata costa una `recv`, non 33 KB.

**Quel che lo spike non ha potuto misurare**, e va detto: 17 dei 39 feed erano vuoti quel giorno
(fra loro CH, SE, UK, UA, LU, AD), quindi la loro forma di geocodice non è osservata, solo
supposta dal file delle geometrie. E EE, IL e NO non sono **proprio** nel file delle geometrie:
per loro la fase non promette niente, e lo dice.

### Il prototipo dell'11 set 2026, e perché non è stato fuso

La fase è stata scritta per intero su `claude/meteoalarm-domain-11set-t4k9w2` — dominio,
importatore, 28 asset, sorgente, store, passo, superfici, 847 test verdi, lint 0 errori, CI
verde, APK installato — e **il committente ha deciso di non fonderla**. Il branch è stato
cancellato. Quel che segue è il motivo, perché è la parte che vale più del codice.

**Il difetto non era la Slovenia, era come falliva il join.** Quando l'area di una voce non si
collocava, lo schermo non taceva: **affermava**. Lubiana, provata su device, aveva 73 allerte
attive — una *Severe Rain Warning* — e l'app diceva «Nessuna allerta». Non un buco, una frase
falsa su un'informazione di sicurezza. E il guasto era **invisibile**: nessun errore, nessuna
riga nel Diario, niente che distinguesse «qui è tranquillo» da «non ho saputo collocare
nessuna delle 73 voci».

Vale per ogni paese e per sempre: se un servizio nazionale rinomina le sue aree, o MeteoAlarm
ripubblica il file dei geocodici con un'altra numerazione, ogni lettore di quel paese passa in
silenzio a «nessuna allerta».

**La misura che ha deciso.** Un audit che replica la logica di join contro i feed dal vivo,
28 paesi, 11 set 2026:

| Esito | Paesi |
|---|---|
| join verificato funzionante | 10 — AT, BA, BG, FR, GR, HU, MK, PT, RO, RS |
| **rotto** | 1 — **SI** |
| parziale, e **corretto**: perde solo le aree marine scartate di proposito | 6 — DE, ES, FI, HR, LT, PL |
| feed vuoto quel giorno, quindi mai osservato | 11 — BE, CY, CZ, DK, IE, IS, MD, ME, MT, NL, SK |

**Diciassette paesi su 28 erano una promessa non verificata**, mentre la schermata Avvisi
stampava con sicurezza «in Italia e in 28 paesi europei». Il numero era più sicuro di sé dei
dati dietro.

**Il caso sloveno, per chi riaprirà.** Il feed nomina le sue aree `Slovenia / Central`,
`Slovenia / North-East`; l'asset le chiama `Central`, `Northeast`. Il confronto falliva su due
cose insieme: il prefisso `<paese> / ` e il trattino. Riparabile — non serviva escludere la
Slovenia — ma il commento di `MeteoAlarmCountries` **dichiarava la Slovenia esclusa** e il
codice la spediva lo stesso, e nessun test se n'è accorto perché le assenze erano un elenco
scritto a mano che non la conteneva. È l'errore da non ripetere: la tabella dei paesi non va
verificata contro un elenco, va verificata **contro i feed veri**.

**Le due cose che renderebbero la fase fondibile**, e nessuna riguarda la Slovenia:

1. **Fallire chiuso.** Se un feed ha voci e **nessuna** si colloca, quello non è «nessuna
   allerta», è «non coperto». Si calcola con i dati già in mano, e trasforma una bugia
   silenziosa in un'assenza onesta. È la correzione che conta.
2. **L'audit come strumento ripetibile**, da lanciare prima di un rilascio e quando MeteoAlarm
   ripubblica il file dei geocodici. Fuori dalla CI: dipende dalla rete e dal meteo del giorno.

Con quelle due, la frase a schermo smette di essere «28 paesi» e diventa quello che è davvero:
i paesi in cui *adesso* sappiamo collocare un'allerta. La fragilità dei dati resta — è nella
natura di trenta produttori indipendenti — ma smette di essere silenziosa, che è l'unica cosa
che la rende accettabile per una funzione di sicurezza.

**Quel che il prototipo ha dimostrato che funziona**, e che non va rifatto da zero: il modello
a due emittenti in `:core:domain` (l'Italia isolata da `WarningIssuer.of(countryCode)` e mai
toccata), la richiesta condizionale costruita sul `<updated>` del feed, l'importatore con il
blocco marino, e il fatto che le superfici della Fase 11 reggono un secondo emittente senza
essere riscritte. Le misure sono tutte qui sotto e nello spike.

### Decisioni già prese

- **MeteoAlarm e non gli aggregatori**: il WMO Severe Weather Information Centre è un sito senza
  API (il suo JSON interno è piatto, senza geometrie, e rilancia i feed nazionali); l'Alert-Hub
  filtra per rettangolo e il feed Italia conteneva cento allerte slovene e svizzere e zero
  italiane il giorno della misura; OpenWeatherMap chiede una chiave, contro la promessa del
  prodotto. Misure del 9 set 2026, VISION §12.8.
- **Il feed italiano di MeteoAlarm non si legge**, nemmeno come complemento per vento e caldo che
  il bollettino di criticità non copre: la regola «una sola voce» vale più di due rischi in più, e
  il CAP stesso dice di non essere l'allerta ufficiale.

### Da sciogliere all'inizio della fase

- L'accesso all'EDR (sopra). Cambia il costo della fase, non il suo perimetro.
- Se le richieste condizionali non sono onorate dai feed: un Atom per paese per ogni giro del job
  è troppo a 15 minuti; in quel caso la cadenza del passo scende a una lettura l'ora per paese,
  e si dichiara.

### Riferimenti (raccolti il 9 set 2026)

- Feed: `https://feeds.meteoalarm.org/feeds/meteoalarm-legacy-atom-<paese>` (l'italiano,
  `-italy`, serve solo a leggere il formato: in Italia non si usa). Ogni `<entry>` porta
  `cap:geocode` `EMMA_ID` (es. `IT019`), `cap:areaDesc`, `cap:event` («Orange Wind Warning»),
  `cap:sent`/`effective`/`onset`/`expires`, `cap:severity`, `cap:message_type` (Alert, Update) e
  il link `application/cap+xml` a `https://feeds.meteoalarm.org/api/v1/warnings/feeds-<paese>/<uuid>`;
  il feed dichiara i termini in `<rights>` e un hub PubSubHubbub che non ci riguarda.
- CAP: un `<info>` per lingua (`en-GB` più la nazionale), `<parameter>` `awareness_level`
  («3; orange; Severe») e `awareness_type` («1; Wind»), `<senderName>`, `<description>`,
  `<instruction>`, `<web>`, `<area>` con `EMMA_ID` e nessun poligono.
- Livelli: 2 yellow Moderate, 3 orange Severe, 4 red Extreme. Tipi: 1 Wind, 2 snow-ice,
  3 Thunderstorm, 4 Fog, 5 high-temperature, 6 low-temperature, 7 coastalevent, 8 forest-fire,
  9 avalanches, 10 Rain, 12 flooding, 13 rain-flood, 14 Marine-Hazard, 15 Drought (l'11 non
  esiste).
- API: `https://api.meteoalarm.org/edr/v1/collections?f=json` risponde senza chiave e descrive
  i filtri; `…/collections/warnings/locations` risponde 401; `…/metadata/v1` è il portale.
  Stato del servizio `https://status.meteoalarm.org`; termini
  `https://meteoalarm.org/en/page/terms-and-conditions`; profilo CAP v2.0 nel progetto GitLab
  `meteoalarm-pm-group/documents`.

---

## Fase 13 — Le icone, da zero: Meteocons v3

Aperta l'11 set 2026 da una segnalazione del committente: dove Open-Meteo e le altre app
mostrano il sole, Chiaro mostra il sole dietro una nuvola. La verifica ha dato ragione alla
segnalazione e ha trovato sotto una causa più grande di quel singolo glifo — **il repo è
fermo a Meteocons v2.0.0, e la famiglia nel frattempo è stata rifatta**. Questa fase non
ripara una riga: rifà l'importazione.

### Il difetto che l'ha aperta

`ChiaroIcons.conditionLineRes` (riga 373) dà **lo stesso disegno** ai codici WMO 1 e 2:

```kotlin
1, 2 -> if (night) mc_partly_cloudy_night else mc_partly_cloudy_day
```

Non sono vicini: sono i due estremi della metà serena del cielo. La soglia di Open-Meteo è
già scritta in questo repo, in `WeatherReportMapper.skyCode` (riga 369) — 0 sotto il 20% di
copertura, 1 fra 20 e 49, 2 fra 50 e 79, 3 da 80 in su. Misurato dal vivo l'11 set 2026 su
10 città × 7 giorni = **1 680 ore** (`weather_code` + `cloud_cover`):

| codice | quota ore | copertura p10 / p50 / p90 |
|---|---|---|
| 0 | 28% | 0 / 0 / 10 |
| 1 | **17,2%** | **10 / 25 / 46** |
| 2 | 20% | 47 / 64 / 80 |
| 3 | 31% | 82 / 98 / 100 |

I due secchi non si toccano nemmeno fra il decimo e il novantesimo percentile. **Un'ora su
sei** viene disegnata al 64% di cielo chiuso quando ne ha il 25%. E la parola accanto dice
già la cosa giusta (`WeatherText.condition`: 1 → `cond_mostly_clear`, «Quasi sereno»),
quindi sull'hero il disegno **contraddice una parola visibile**; nella striscia oraria e
nella riga della settimana la parola non è visibile affatto (sta solo nella
`contentDescription`: `HourStrip.kt:97`, `TodayScreen.kt:1315`), quindi lì il glifo sbagliato
è l'unico portatore che il lettore ha.

Due difetti minori nella stessa tabella: il fallback `else -> mc_cloudy` disegna una nuvola
mentre la parola dice «Condizioni sconosciute» (`mc_not_available` è importato in tutti e
quattro i set e la mappatura dei codici non lo usa mai); e l'82 (rovesci violenti) va su
`rain` mentre 80 e 81 vanno su `partly_cloudy_*_rain`. E **la tabella non ha un solo test**:
`AnimatedIconTest` cammina `movingOf`, `IconContrastTest` misura i colori, `WidgetIconsTest`
copre la scelta dello stile, nessuno chiede mai che disegno prenda un codice. È per questo
che 1 e 2 hanno potuto fondersi senza che niente diventasse rosso.

### Perché si riparte da zero e non si tocca la riga

Meteocons v2.0.0 **non ha un disegno per «quasi sereno»**: `production/line/all` sono 122
icone e nella famiglia del cielo ci sono solo clear, partly-cloudy, overcast (+ day/night) e
cloudy. Misurato sugli SVG originali, in una scatola di 64: il sole di `clear-day` ha r 10.5
al centro; quello di `partly-cloudy-day` ha **r 4.5** (−82% di area del disco) sotto una
nuvola che occupa x 16→53.5, y 23.5→45.5; `overcast-day` è la stessa nuvola con un secondo
strato sopra, cioè **più** nuvoloso, non meno. Non c'è un gradino intermedio da scegliere.

**In v3 c'è.** La categoria si chiama `Mostly Clear` ed esiste in 18 varianti. Il resto della
famiglia è cresciuto nello stesso modo: `pollen-grass/tree/weed` nei quattro livelli,
`uv-index-1…11`, `barometer-low…extreme`, `windsock-calm/weak/moderate`,
`wind-beaufort-0…12`, `compass-n…nw`, `code-yellow/orange/red` e `weather-alert` — cioè
esattamente le bande che le schede Dettagli, il tile pollini e le allerte della Fase 11
calcolano già e disegnano con un glifo generico.

| | v2.0.0 (oggi) | v3 |
|---|---|---|
| Icone | 122 (line) | **519**, in 16 categorie |
| Stili | line, fill | **fill, flat, line, monochrome** |
| Formati | SVG con SMIL | SVG con SMIL (`@meteocons/svg`), SVG statico (`@meteocons/svg-static`), **Lottie** (`@meteocons/lottie`) |
| Scatola | 64 × 64 | 128 × 128 |
| Distribuzione | clone del repo al tag | npm + CDN `cdn.meteocons.com/{versione}/{formato}/{stile}/{icona}.{est}`, con `manifest.json` |
| Licenza | MIT | MIT |
| Importate in Chiaro | 49 su 122 | — |

Chiaro ne spedisce oggi 268 file XML, 1,7 MB: 49 icone × 4 set statici (`mc_`, `mcn_`,
`mcf_`, `mcfn_`) + 18 × 4 set animati (`mca_`, `mcan_`, `mcaf_`, `mcafn_`).

### Lo spike, prima di aprire le caselle

Tre domande, e finché non hanno risposta la fase non parte. La prima è l'unica che può
fermarla.

- [x] **Le maschere.** v3 usa `<mask>`, che VectorDrawable non ha. Sul sottoinsieme delle 70
      icone che Chiaro metterebbe a schermo: **line 31, flat 31, monochrome 31, fill 12**. Per
      `line` sono quasi tutti i cieli composti — `mostly-clear-day/night`,
      `partly-cloudy-day/night`, `overcast`, `fog-day/night`, gli `overcast-*`, i
      `partly-cloudy-*-rain/snow`, `extreme-rain`, i `thunderstorms-day/night` — quindi
      **il convertitore di maschere è obbligatorio**, non una comodità, e lo è qualunque sia
      il secondo stile.
      Il fatto che salva: sono **tutte `mask-type:alpha` con riempimenti binari bianco/nero,
      nessuna maschera in gradiente**, e la stragrande maggioranza usa `evenodd`. Una maschera
      «tutta la tela meno questa forma» si riscrive come `<clip-path>` se si **inverte il
      verso** della sottoforma (il parser Android applica NON-ZERO e `<clip-path>` non ha
      `fillType`). La maschera però **si muove** (porta la sua `animateTransform`: è la nuvola
      che va su e giù) mentre il sole sotto sta fermo, e in VectorDrawable la trasformazione di
      un gruppo si applica **sia al clip sia ai figli**. Serve quindi la coppia annidata:

      ```xml
      <group android:translateY="@anim">        <!-- muove il clip -->
          <clip-path android:pathData="tela meno nuvola"/>
          <group android:translateY="@anim-inverso">   <!-- rimette fermo il sole -->
              … il sole …
          </group>
      </group>
      ```

      **Fatto l'11 set 2026, e la geometria regge: 1 345 maschere su 1 345 identiche.**
      `tools/spike_mask_clip.py` non guarda un disegno, rasterizza: appiattisce ogni maschera
      dei quattro stili (bezier, archi e abbreviazioni comprese), calcola con una scanline
      l'area **evenodd dell'originale** e l'area **nonzero della versione convertita**, e le
      confronta su una griglia di 128 × 128. **Zero pixel di differenza**, su tutte e 519 le
      icone × 4 stili: 1 236 maschere a due sottopercorsi (tela + forma, quelle che hanno
      bisogno dell'inversione) e 109 a uno solo (dove le due regole già coincidono). 12,7
      secondi in tutto.
      **Anche la maschera che si muove regge, e si è guardata** (stesso giorno).
      `tools/spike_v3_convert.py` è il prototipo del convertitore: converte davvero un SVG
      v3 in `<vector>` + `<animated-vector>`, con l'inversione fatta **sui comandi** e non
      sulla polilinea (una cubica girata è `P1,C2,C1,P0`; un arco tiene i raggi e
      **inverte `sweep`**), e ri-rasterizza ogni conversione prima di scriverla. Quattro
      icone `line` — `clear-day`, `mostly-clear-day`, `partly-cloudy-day`, `overcast` —
      più `partly-cloudy-day-rain` e `thunderstorms-day`.
      **`./gradlew :app:assembleDebug` passa**: aapt accetta il viewport 128, il
      `clip-path`, i gruppi annidati, `fillType="evenOdd"` e i `pathInterpolator` generati
      dai `keySplines`.
      E `tools/icon_filmstrip.py` — insegnato a leggere il viewport invece dei 64 fissi —
      le ha disegnate a nove istanti del ciclo su **entrambe** le superfici: la nuvola
      va su e giù, **il sole sotto resta fermo**, i raggi sono tagliati dove passa la
      nuvola. La coppia di gruppi annidati fa quello che doveva fare.
      Due cose che il filmstrip ha reso visibili e che nessun numero aveva reso ovvie:
      la scala 0 → 1 → 2 → 3 finalmente **si legge come una scala** (sole pieno, sole con
      nuvoletta, sole dietro la nuvola, nuvola sola); e i colori originali sulla carta
      sono **fantasmi**, mentre sul fondo scuro sono nitidi. La misura del 73% / 13%
      guardata invece che calcolata.
      Quel che resta al telefono: che hwui renda la coppia annidata come la rende il
      filmstrip. Il rischio è sceso da «la fase può cadere» a «una verifica di resa».
- [x] **I colori originali su carta.** La misura sotto. Lo spike produce il filmstrip delle
      due ipotesi (originali / riancorati) e si decide guardando.
- [x] **La prerelease.** `@meteocons/svg` su npm ha `latest` = **0.1.0** e `next` =
      **3.0.0-next.10**: v3 non è ancora stabile. Verificare se esce una `3.0.0` prima di
      aprire le caselle; se non esce, si appunta la versione esatta come si è appuntato il tag
      v2.0.0, e `UPSTREAM.md` dice che è una prerelease e perché.
      **Sciolto l'11 set 2026**: la 3.0.0 non e' uscita, quindi si appunta la
      prerelease esatta — `@meteocons/svg@3.0.0-next.10` — come si appuntava il tag
      v2.0.0, e `licenses/Meteocons-MIT.txt` dice da quale pacchetto viene il testo.

### Il commento sui colori originali, con i numeri

«Lasciare i colori originali» semplificherebbe moltissimo: farebbe cadere il motivo per cui
oggi esistono quattro set statici invece di due. Ma non regge sulla superficie chiara.

Misurato sulle 70 icone che Chiaro metterebbe su uno schermo, contro le due superfici di
DESIGN §2.2 (carta `#FCF9F3`, scura `#16130E`), soglia 3:1 di §10. **I riempimenti delle
maschere sono esclusi**: `#fff` e `#000` lì dentro non sono inchiostro, e contarli gonfiava
il conto nella prima stesura di questa fase. «Inchiostro sotto soglia» pesa ogni colore per
quante volte è usato, che è la domanda vera:

| | fill | flat | line | monochrome |
|---|---|---|---|---|
| colori distinti | 44 | 35 | 31 | **1** |
| passano 3:1 su **entrambe** | 10 | 10 | 7 | 0 |
| inchiostro sotto soglia su **carta** | **77%** | **73%** | **73%** | 100% |
| inchiostro sotto soglia su **scura** | 7% | 5% | **13%** | 0% |

I più usati sono corpi e contorni di nuvola: `#e6effc` (26 usi in line) **1,10:1** su carta,
`#e2e8f0` (15 usi) **1,17:1**, `#86c3db` (27 usi) **1,84:1**, `#f8af18` — il sole — (33 usi)
**1,79:1**. Non sono «un po' chiari»: sono invisibili, ed è lo stesso difetto che
`import_meteocons.py` documenta per v2 (`#E5E7EB`, 1,18:1) — v3 non l'ha risolto perché
Meteocons è disegnato per un fondo neutro, non per la carta.

Una differenza fra gli stili che conta: **`line` è l'unico che fallisce in modo materiale
anche sul fondo scuro** (13% dell'inchiostro contro il 5–7% degli altri), per via di
`#1e293b` (29 usi, **1,27:1** su scuro) con cui disegna i contorni scuri. Quindi line va
riancorato su **entrambe** le superfici, fill e flat praticamente solo su carta. Che è
esattamente la forma dei quattro set che ci sono già.

E `monochrome` è la via di fuga che nessuno ha chiesto ma che va scritta: **un solo colore**,
quindi tingibile con un ruolo e conforme per costruzione. Non si prende, e il motivo è già in
DESIGN §13.1 — «una tinta piatta trasforma la famiglia in sagome». Resta lì come opzione se
un giorno servisse un set che non costa nulla in contrasto.

Quindi la risposta alla domanda «i colori originali possono causare problemi»: **sì, su carta
sempre, e su fondo scuro anche per line**. La proposta, che non blocca il lavoro e non butta
la regola:

- [x] Il tool importa **con i colori originali** come uscita di riferimento (`--original`), e il
      rimappaggio è un **passo successivo** sullo stesso albero: si generano entrambi e si sceglie
      davanti al filmstrip, anziché decidere prima
- [x] L'architettura «due set scelti dal fondo» (`styledRes(darkGround)`) **resta**: è già
      scritta, ha superato una revisione di design, e con i numeri qui sopra è esattamente la
      forma giusta. Ma la regola non è «originali sullo scuro, riancorati sulla carta» per
      tutti: **flat** può quasi tenere gli originali sullo scuro (5% di inchiostro sotto
      soglia), **line no** (13%, per i contorni `#1e293b`). Quindi quattro set come oggi, e
      quanto rimappaggio serva a ciascuno lo dice la misura, non la simmetria
- [~] **Non presa**, e si scrive perché era la terza strada: dare all'icona un
      **fondo suo** (una pastiglia tenue sotto il glifo nella striscia e nella settimana): fa
      passare il 3:1 senza toccare un colore. È una decisione di design, non di conversione —
      va in DESIGN §13.1, non qui

### I due stili: **flat + line**, e perché non fill

Il committente aveva detto «fill e flat», poi si è corretto in «fill e line», poi ha chiesto
se non fosse meglio «flat e line». La misura dà ragione alla terza versione, e per un motivo
più forte del conteggio delle maschere.

**v3 `flat` è quello che Chiaro già spedisce.** L'attuale set `mcf_*` è lo stile fill di v2
con **ogni gradiente appiattito sul colore di faccia** dal tool (dipartenza n. 4 nell'intestazione
di `import_meteocons.py`). Lo stile `flat` di v3 è esattamente quel disegno, ma **disegnato
così a monte** invece che derivato da noi. Scegliere `fill` vorrebbe dire o tenere i gradienti
— una capacità nuova, un rischio nuovo e un cambio visivo rispetto a oggi — o riappiattirli,
cioè rifare a valle un lavoro che l'illustratore pubblica già fatto. È il punto 5 della
richiesta («riusare le originali senza ricrearle») portato al massimo.

Misurato sulle 70 icone del sottoinsieme:

| | fill | flat | line |
|---|---|---|---|
| maschere da convertire | 12 | 31 | 31 |
| filtri da lasciar cadere | 1 (`compass`) | **0** | **0** |
| gradienti da appiattire o portare | 65 | 3 | **0** |
| tratteggi da ridisegnare | 2 | 2 | 2 |
| animazioni | identiche | identiche | identiche |

**La coppia flat + line è quella che chiede al tool il minor numero di riscritture**: zero
filtri, zero o quasi gradienti, e le maschere che servirebbero comunque perché `line` — lo
stile predefinito, quello che il lettore vede appena installa — ne ha 31 su 70. Una volta
costruito il convertitore per line, flat non costa una riga in più. Il conteggio delle
maschere, che nella prima stesura di questa fase faceva preferire fill, **smette di essere un
argomento** nel momento in cui line è uno dei due: va costruito comunque.

Va anche detto cos'è il `line` di v3, perché non è il `line` di v2: non è un contorno
tracciato, è **una forma riempita con `fill-rule="evenodd"`** — una ciambella. Per Android è
una buona notizia (`android:fillType="evenOdd"` esiste dall'API 24, e non c'è nessun tratto
da convertire); per il contrasto è la stessa notizia di sempre, perché quella ciambella è
riempita di `#e6effc`.

E la simmetria con oggi si conserva: **line resta il predefinito, flat è l'alternativa in
Impostazioni → Aspetto**, esattamente come line/fill adesso. Nessuna stringa da riscrivere,
nessuna scelta da spiegare di nuovo al lettore.

### Il commento sulla «reimportazione totale»

519 icone × 2 stili × (statica + animata) non stanno nell'app. Misurato sul repo: un
drawable statico pesa **~1,7×** il suo SVG, un animato **~3,5×**; l'SVG v3 medio è 4,1 KB,
quindi ~7 KB statico e ~14 KB animato.

Vale anche la regola di DESIGN §7.1, che dimezza il conto: **muove solo la famiglia delle
condizioni**, perché «il marchio di un tile etichetta una quantità, e un barometro che gira
per sempre è decorazione». Quindi 24 icone hanno il gemello animato e le altre 119 no.

| scenario | file | XML su disco |
|---|---|---|
| oggi (v2, 49 icone, 4 set statici + 4 animati) | 268 | 1,7 MB |
| **le 143 della lista, line + flat, 2 set per fondo** | **668** | **~5,2 MB** |
| le 143, line + flat, 1 solo set di colori | 334 | ~2,6 MB |
| tutte e 519, 2 stili, 1 set | 2 076 | ~21,8 MB |
| tutte e 519, 2 stili, 2 set per fondo | 4 152 | ~43,6 MB |

Tre volte il peso di oggi per tre volte le icone.

**Ma il peso su disco non è il peso nell'APK, e cambia la conclusione.** Misurato dentro
`app-debug.apk`: i 736 drawable della famiglia pesano 972 KB non compressi e **371 KB
compressi**, cioè **516 B l'uno** — l'XML binario più il deflate valgono un fattore 4,6
contro il testo su disco. Rifatto il conto in quella moneta: la lista da 143 aggiunge
**~1,2 MB** all'APK di release, il set completo da 519 ne aggiungerebbe **~3,5 MB**.

E c'è la terza strada, che è quella giusta e che il progetto ha già configurata
(`isShrinkResources = true`, `app/build.gradle.kts:63`):

- [x] **Si converte il set completo (519 × 2 stili), si generano le tabelle Kotlin solo per
      la lista di spedizione.** Un drawable che nessuna tabella nomina non è referenziato, e
      `shrinkResources` lo toglie dalla release: **zero byte** di costo per le icone non
      ancora usate, ~15 MB nel repo, e promuoverne una a «spedita» è **una riga**, senza
      reimportare niente e senza rete. Il repo diventa il set completo, l'APK resta la lista.
      **Provato l'11 set 2026 sul set intero, e «zero byte» era sbagliato.** Con tutte le
      1 974 icone v3 in `res/drawable/` e nessuna riga di Kotlin che le nomini, l'A/B di
      due build di release dà **6 676 125 B contro 6 548 541 B: +127 583 B**, cioè
      **65 byte a drawable**. E stanno *tutti* in `resources.arsc` (+127 464 B): i file in
      `res/` sono **1 565 in entrambe**, identici — AGP butta il disegno e tiene la voce
      in tabella. Quindi il set completo costa **125 KB, il 2% dell'APK**, non zero.
      L'argomento regge lo stesso e il ripiego previsto non serve, ma il numero è questo
      e non quello che avevo scritto

Quindi «totale» va inteso sul **convertitore**, non sull'APK: il tool deve saper convertire
qualunque delle 519 e dimostrarlo, e la tabella `ICONS` resta la lista di spedizione.
Crescerla è una riga.


### Blocco C — il colore, fatto (11 set 2026)

`tools/reanchor.py`. La regola della v2 era una **tabella a mano** (`REMAP`, `FILL_REMAP`,
`FILL_NIGHT`): una riga per ogni hex. La v3 ha il doppio dei colori e ne avrà altri,
quindi qui la regola è scritta come funzione e la tabella è il suo risultato.

Si tiene la tinta e si sposta la **luminanza**, perché il contrasto WCAG dipende solo da
quella: è l'unica leva che cambia il rapporto, ed è quella che si vede di meno. Due cose
però cambiano rispetto alla v2, e la seconda è un guadagno:

1. **Non si sposta un colore, si comprime una famiglia.** Una nuvola è tre grigi, e una
   regola applicata a ciascuno per conto suo può scambiarli. I colori si raggruppano per
   tinta (i neutri, sotto 0,03 di croma, stanno insieme) e la famiglia si comprime
   intera, tenendo fermo **l'estremo che già va bene**: contro un tetto resta fermo il più
   scuro e scende il più chiaro, contro un pavimento resta fermo il più chiaro e sale il
   più scuro. Ordine e spaziatura in L di Oklab si conservano.
2. **Ogni set deve una sola superficie.** v2 chiedeva a un set di reggere entrambe, il che
   lo inchiodava nella banda Y ∈ [0,120, 0,283] — ed è il motivo per cui il suo sole è un
   bronzo: a quella luminanza in sRGB il giallo non c'è. Qui il vincolo è un tetto
   (carta) **oppure** un pavimento (scuro), mai tutti e due, e sul fondo scuro il sole
   resta un oro vero (`#f8af18` → `#ffc25e`).

| | colori | identici all'originale | sotto 3:1 dopo |
|---|---|---|---|
| line, carta | 57 | 2 | **0** |
| line, scuro | 57 | 10 | **0** |
| flat, carta | 62 | 2 | **0** |
| flat, scuro | 62 | 12 | **0** |

Un colore che già reggeva e che la compressione lascia dov'era esce **identico**: il giro
per Oklch e ritorno sposterebbe l'ultima cifra, e cambiare un hex che non ne aveva bisogno
è rumore in un diff che qualcuno dovrà leggere. E la soglia non si dà per raggiunta: dopo
aver costruito il colore si **misura** il rapporto e si corregge di un passo alla volta
finché passa, che è la stessa regola con cui `PaletteContrastTest` asserisce l'esito e non
la ricetta.

I quattro set escono con lo schema della v2 — `mc3_`/`mc3n_` (line, carta e scuro),
`mc3f_`/`mc3fn_` (flat) più i loro gemelli animati — e il riancoraggio si applica **una
volta per stile**, non per icona, perché una famiglia si può spostare solo se la regola
vede tutti i suoi grigi insieme. Il flag `--original` emette anche i set non riancorati,
per il confronto al filmstrip; non si spediscono.

**Il costo, misurato:** 4 004 drawable, 39 MB nel repo, e nell'APK di release
**+257 996 B** (64 byte l'uno, lo stesso numero della misura precedente), cioè il 4%.

- [x] **39 MB nel repo**: **decisi l'11 set 2026 dal committente — non pesano.** La
      famiglia intera resta in git, e la lista di spedizione resta il filtro verso l'APK.
      Se un giorno pesassero, la stessa lista può diventare anche lista di *conversione*


### Blocco B — la cucitura Kotlin, fatta (11 set 2026)

L'app disegna la v3. `tools/import_meteocons.py` e i 268 drawable della v2 escono di
scena; il tool v2 resta in `tools/` perché `import_meteocons_v3.py` ne importa
`loop_keyframes` — la rotazione dei keyframe per una fase negativa, scritta per la v2 e
valida qui identica.

- [x] **`ChiaroIcons` ora è politica, non tabelle.** Le quattro mappe stanno in
      `ui/icons/MeteoconsSets.kt`, **generato** dal tool su `tools/shipped_icons.py`: 143
      icone per quattro set sono 595 righe in cui un refuso non si vede, e il tool sa già
      quali file ha scritto. Quel che resta in `ChiaroIcons` è la parte su cui si discute:
      quale codice prende quale disegno, quale metrica quale marchio
- [x] **Il vestito non sceglie più un'icona.** `styledRes` prendeva `AppPalette` perché il
      set line della v2 doveva **entrambe** le superfici (banda Y ∈ [0,120, 0,283], da cui
      il sole bronzo) e serviva un secondo set perché il vestito vivid potesse scappare da
      quel tetto sui fondi scuri. Con quattro set scelti da stile e fondo il parametro non
      decideva più niente: è stato tolto da `styledRes`, `conditionRes`, `movingRes`,
      `moonPhaseRes`, `ArcText.rowIconRes` e `SkyWidget.skyJobIconRes`
- [x] **`WeatherIcons.FILL` ora è il `flat` di v3, e l'enum tiene il suo nome**: `flat` è
      quello che l'app già spediva come set pieno (il fill v2 coi gradienti appiattiti dal
      tool). Rinominare la costante avrebbe migrato una preferenza salvata e riscritto una
      stringa delle impostazioni per descrivere lo stesso disegno
- [x] **La mappatura WMO** è quella della tabella sopra, `not-available` compreso
- [x] **Il test che non c'era**: `ConditionIconsTest`. Ogni codice che il provider può
      servire → il suo disegno, di giorno e di notte; il fallback; e una riga che dice
      esplicitamente che 0, 1 e 2 sono tre disegni diversi — il difetto che ha aperto la
      fase, in un'asserzione
- [x] **`IconContrastTest` e `AnimatedIconTest`** guardano i set v3. Il primo ha perso la
      frase «il set line deve entrambe le superfici», che dalla Fase 13 non è più vera; il
      secondo ha imparato due cose: che un `keySplines` è un `<pathInterpolator>` e non un
      difetto (pretendeva che ogni animatore fosse lineare, vero solo per la v2), e che
      un'icona il cui tratteggio è diventato una finestra **non** ha lo stesso `pathData`
      del suo gemello fermo — eccezione dichiarata, riconosciuta dalla presenza di
      `trimPath` e da nient'altro, così non può allargarsi in silenzio
- [x] **`MeteoconsSetsTest`**, la guardia del confine. Aggiungere un `R.drawable.mc3_…` a
      una schermata senza aggiungerlo alla lista compila benissimo e poi lancia in faccia
      al lettore, perché `styledRes` usa `getValue`: il test legge i sorgenti e lo impedisce

**E la lista di spedizione si è ristretta, dopo averlo misurato.** Era di 143; le icone
che una schermata disegna davvero oggi sono **53**. Spedire anche le altre 90 — i pollini
graduati, l'UV, le bande del barometro, la Beaufort, i momenti del giorno, i tipi di
allerta — costava **570 KB su 6,8 MB, l'8% dell'APK, per disegni che nessun lettore
vedeva**. Stanno in `PLANNED` nello stesso file, e ognuna si sposta in `SHIPPED` nella
stessa modifica che le dà una schermata.

| | APK di release |
|---|---|
| prima della fase (v2, 49 icone) | 6 548 541 B |
| v3 con 53 spedite | **7 141 284 B** (+593 KB) |
| v3 con 143 spedite | 7 724 737 B (+1,12 MB) |

Il mezzo mega in più compra 24 cieli invece di 18, i disegni di v3 che sono più
dettagliati, e i quattro set riancorati.

- [x] **Le schede Dettagli**: pollini per pianta e livello, UV per unità (il tile stampa
      l'intero), le **tre** bande del barometro che `pressureMeaning` dice e non le cinque
      disegnate, e `mist`/`haze`/`fog` per la visibilità. Il vento **non** è graduato, per la
      stessa regola: `windMeaning` ha cinque bande, i windsock sono tre e la Beaufort non è
      detta da nessuna parte a schermo
- [x] **L'ago del vento**: `wind-direction-n` col solo gruppo `Pointer`, ritagliato su una
      finestra di 48 unità centrata sul **mozzo** (senza ritaglio è tredici unità
      d'inchiostro su centoventotto, e a 16 dp una scheggia — guardato, non supposto), ruotato
      come `WindArrow` ruotava già. Resta a 16 dp, così nessuna misura del tile si muove


### Blocco D — i documenti, e un numero che è migliorato (11 set 2026)

`DESIGN.md` §13.1 era la sezione più lunga del documento e citava v2.0.0, i 64 unit, le
tre dipartenze del tool e il quarto set del vestito vivid: riscritta. Con lei §7.1, la
nota dei generatori e `README.md` (che non prende trattini lunghi, ed è stato ricontrollato
riga per riga).

**L'eccezione dichiarata del widget Cielo è stata rimisurata, e si è mossa parecchio.**
Quel fondo è il cielo scurito, al più chiaro `#5C6E7B`, Y 0,149: un mezzotono, dove un
inchiostro passa il 3:1 solo sopra Y 0,546 o sotto Y 0,016, e in mezzo non c'è niente.

| | sotto 3:1 su `#5C6E7B` | il sole |
|---|---|---|
| v2 line (`mc_*`) | 8 su 8 | 1,57:1 |
| v2 line vivid (`mcn_*`) | 8 su 8 | 2,68:1 |
| **v3 line scuro (`mc3n_*`)** | **17 su 34** | **3,31:1** |
| **v3 flat scuro (`mc3fn_*`)** | **20 su 38** | **3,33:1** |

**L'icona che il lettore guarda davvero adesso passa**, e non era mai successo. Metà dei
colori resta sotto, quindi l'eccezione resta un'eccezione — ma il caso che conta si è
chiuso per aritmetica e non per concessione: un set che deve una superficie sola ha spazio
che un set che ne deve due non ha. È il guadagno della decisione del Blocco C, arrivato
dove non lo si cercava.

### Le caselle

### Blocco A — il convertitore, fatto (11 set 2026)

`tools/import_meteocons_v3.py`, con la geometria dei percorsi estratta in
`tools/svg_paths.py` perché serve anche alla prova (`spike_mask_clip.py`). Legge il
tarball di `@meteocons/svg`, non un clone.

| | line | flat |
|---|---|---|
| statiche convertite | **517 / 519** | **517 / 519** |
| animate | 470 | 470 |
| saltate | 2 | 2 |

Le due che saltano sono `pressure-high-alt` e `pressure-low-alt`: una maschera di contorno
alla Figma (un `<mask>` applicato a una forma sola, non a un gruppo), e non sono nella
lista di spedizione. **1 974 file, 20 MB nel repo**, `:app:assembleDebug` verde.

Quel che il rapporto del tool dichiara invece di tacere, ed è debito vero:

- **112 icone con il tratteggio reso solido.** VectorDrawable non ha `stroke-dasharray`.
  L'importatore v2 li **ridisegnava** come segmenti veri (la sua dipartenza n. 3) e qui
  quel lavoro non è stato rifatto: per ora la riga tratteggiata esce piena, che è un
  disegno diverso. Nella lista di spedizione tocca `wind` e `wind-beaufort-*`
- **42 animazioni del tratteggio perse** (`stroke-dashoffset`, le formiche in marcia). Si
  perde l'animazione, non l'icona, ed è la conseguenza del punto sopra
- **15 gradienti appiattiti** sul colore di faccia: è voluto, ed è la dipartenza n. 4 di v2
  per lo stesso motivo. Con line e flat sono un caso di bordo, non lo stile

Cose che il porting ha richiesto e che v2 non aveva: i rettangoli ad angoli arrotondati
(149 icone, fra cui tutti i `barometer*`) come quattro archi veri; le trasformazioni
statiche (`rotate(45 cx cy)` è `rotation` più il pivot, alla lettera) come gruppi
annidati; i `clip-path` interni, saltando quello grande quanto la tela che Figma mette
addosso a quasi ogni icona. La fase negativa, i `keyTimes` e i keyframe **non** sono stati
riscritti: sono le funzioni di `import_meteocons.py`, importate e usate tali e quali.

- [x] **Il tool v2 resta, i suoi drawable no.** `import_meteocons.py` non produce più niente
      di spedito, ma `import_meteocons_v3.py` ne importa `loop_keyframes`: la rotazione dei
      keyframe per una fase negativa è scritta lì ed è valida identica sulla v3
- [x] **Il tratteggio**: ridisegnare i 112 come fece v2, o dichiarare per iscritto quali
      restano pieni. Non si spedisce `wind` con una riga che il disegnatore aveva tratteggiato
      senza dirlo
      **Fatto**, e in due modi diversi perche' erano due problemi: vedi sopra.
- [x] **Il tool**: `import_meteocons.py` legge da `@meteocons/svg` / `@meteocons/svg-static`
      (versione appuntata, tarball con checksum, o CDN versionato) invece che da un clone del
      repo. Oggi il tool accetta `g`, `circle`, `path`, `defs` ed esce su tutto il resto; gli
      elementi nuovi da gestire, misurati su tutte e 519, sono `<rect>` (598, quasi sempre il
      rettangolo del `clipPath` di tela), `<mask>`, `<line>` (2) e — solo nello stile fill —
      `<filter>` + `<fe*>` (17). Va gestito anche `fill-rule="evenodd"`, che in `line` è il
      modo stesso in cui è disegnato il contorno: `android:fillType="evenOdd"`, dall'API 24
- [x] **Le maschere**: la conversione dello spike, estesa e verificata icona per icona. È il
      punto 5 della richiesta: dove *non* si può riusare l'originale, il tool lo dice e il
      motivo finisce in `UPSTREAM.md`
- [x] **I filtri**: nel sottoinsieme, con flat + line, sono **zero** — confermato sull'importazione intera, nessuna icona line o flat ne usa. Restano nello stile fill
      (17 icone su 519: tutte `compass*` e `wind-direction-*`, un'ombra portata fatta di
      `feFlood` + `feOffset` + `feComposite` + `feBlend`). Se un giorno si importasse fill, si
      **lasciano cadere**: l'ombra non porta informazione e VectorDrawable non ha filtri
- [x] **I tratteggi**, chiusi l'11 set 2026 — ed erano **due problemi diversi**, non uno:

      | | quanti | cos'è | come si dice in Android |
      |---|---|---|---|
      | `stroke-dasharray="12 9"` | 112 | tratteggio fermo, tutte **rette** (foschia, nebbia, fumo) | si **ridisegna** a segmenti veri |
      | `stroke-dasharray="50"` + `stroke-dashoffset` animato | 34 | non è un tratteggio: è una **finestra che corre** lungo la riga del vento | `trimPathStart/End` + `trimPathOffset` animato |

      Il primo caso è esatto e non approssimato: una retta di 48 unità spezzata 12 acceso /
      9 spento dà `M40,95 L52,95 M61,95 L73,95 M82,95 L88,95`, che è quel che l'SVG
      disegnerebbe. Il secondo lo è quasi: `trimPath` ha **una** finestra, quindi dove il
      tratto è più lungo del periodo (100 unità; il più lungo misura 111) l'SVG ne
      mostrerebbe due. Le due grandezze non hanno né unità né verso in comune — l'offset
      SVG è in unità di disegno e crescendo sposta il motivo **all'indietro**,
      `trimPathOffset` è una frazione e crescendo lo sposta **in avanti** — quindi la
      corsa si converte in giri di percorso e si anima da 1 a 0.
      Le icone che animano salgono da 470 a **484 per stile**: le quattordici del vento
      avevano solo quella. E `icon_filmstrip.py` ha imparato a ridisegnare `trimPath*`
      come il tratteggio da cui viene (con `pathLength="1"` le frazioni di Android **sono**
      le unità del dasharray), quindi la spazzolata si è potuta guardare: scorre
- [x] **I gradienti**: con flat + line il problema **sparisce** — sulle 519 sono **15 icone**, appiattite sul colore di faccia e dichiarate nel rapporto. Era il
      motivo della dipartenza n. 4 del tool, che a questo punto si può togliere anziché
      riscrivere. Resta da decidere solo per le tre di flat: appiattire come oggi, o portarle
      davvero (VectorDrawable **sa fare i gradienti** con `aapt:attr` su `android:fillColor`);
      se si portano, va verificato che reggano dentro i widget Glance, che caricano il
      drawable nel processo del launcher
- [x] **Le animazioni restano quelle di Meteocons** (punto 6 della richiesta — 470 icone animate per stile):
      `@meteocons/svg` è ancora **SMIL**, quindi la strada SMIL → `AnimatedVectorDrawable` che
      il tool percorre già regge. Il Lottie di v3 sarebbe l'altra strada e **non si prende**:
      vorrebbe `lottie-android` come dipendenza, non gira in un widget Glance, e si porterebbe
      dietro un runtime per un'icona da 34 dp.
      **Le animazioni sono identiche nei quattro stili** — stessi tipi, stessi conteggi, stessi
      tempi; cambia solo che negli stili mascherati la maschera porta il proprio `translate`.
      Quindi «le line animate vanno bene?» ha la stessa risposta di qualunque altro stile, e la
      risposta è sì: animano **485 icone su 519**, e **68 su 70** nel sottoinsieme. Tipi da
      coprire nel sottoinsieme: `translate` 151, `opacity` 106, `rotate` 42, `scale` 4,
      `stroke-dashoffset` 4. I primi tre il tool li fa già, `scale` è un attributo di
      `<group>`, gli ultimi quattro sono le due icone del tratteggio
- [x] **La mappatura WMO**, finalmente 1:1 con quello che il provider dice (punto 4):

| WMO | parola già a schermo | icona v3 (giorno / notte) |
|---|---|---|
| 0 | Sereno | `clear-day` / `clear-night` |
| 1 | Quasi sereno | **`mostly-clear-day` / `mostly-clear-night`** |
| 2 | Poco nuvoloso | `partly-cloudy-day` / `partly-cloudy-night` |
| 3 | Coperto | `overcast` |
| 45, 48 | Nebbia | `fog-day` / `fog-night` |
| 51, 53, 55 | Pioviggine | `overcast-drizzle` |
| 56, 57 | Pioviggine gelata | `overcast-sleet` |
| 61, 63, 65 | Pioggia | `overcast-rain` |
| 66, 67 | Pioggia gelata | `overcast-sleet` |
| 71, 73, 75, 77 | Neve | `overcast-snow` |
| 80, 81 | Rovesci | `partly-cloudy-day-rain` / `-night-rain` |
| 82 | Rovesci violenti | `extreme-rain` |
| 85, 86 | Rovesci di neve | `partly-cloudy-day-snow` / `-night-snow` |
| 95 | Temporale | `thunderstorms-day` / `thunderstorms-night` |
| 96, 99 | Temporale con grandine | `thunderstorms-day-hail` / `-night-hail` |
| altro | Condizioni sconosciute | **`not-available`**, non una nuvola |

  Le righe `overcast-*` invece delle piatte `drizzle`/`rain`/`snow` hanno una misura dietro,
  dalle stesse 1 680 ore: quando piove il cielo **è** chiuso (codice 51: copertura minima 88,
  p50 100; codice 61: minima 87, p50 100; codice 80: minima 71, p50 100). Disegnare la
  pioggia senza la sua nuvola sarebbe sottrarre un fatto, non semplificare
- [x] **Un test sulla tabella**, che oggi non c'è: ogni codice WMO che il provider può
      servire → il suo drawable, per giorno e per notte, e il fallback su `not-available`.
      È la casella che impedisce alla prossima fusione silenziosa di ripetersi
- [x] **La lista di spedizione.** Era di 143, verificate una per una presenti in entrambi
      gli stili; misurando si è divisa in **83 spedite e 61 pronte** (`SHIPPED` e `PLANNED`
      in `tools/shipped_icons.py`), perché spedire un disegno che nessuna schermata nomina
      costa e non si vede. Il gruppo qui sotto è la scelta di partenza (punti 2 e 3 della
      richiesta); la colonna che conta è se una schermata la disegna:

| gruppo | n | note |
|---|---|---|
| Cieli, la mappatura WMO | 24 | la tabella sopra, `not-available` compreso |
| Dettagli, i marchi di oggi | 13 | `wind`, `humidity`, `uv-index`, `thermometer`, `barometer`, `raindrop(s)`, `mist`, `umbrella`, `snowflake`, `dust`, `smoke-particles`, `compass` |
| **Pollini** | 16 | `pollen`, `pollen-grass/tree/weed` e i loro `-low/-moderate/-high/-very-high` |
| **UV graduato** | 12 | `uv-index-1…11` e `-11-plus` |
| **Pressione graduata** | 5 | `barometer-low/moderate/high/very-high/extreme` |
| **Vento graduato** | 18 | `windsock(-calm/-weak/-moderate)`, `wind-beaufort-0…12`, `umbrella-wind` |
| **Visibilità e temperatura** | 5 | `haze`, `fog`, `smoke`, `thermometer-warmer/-colder` |
| **Direzione del vento** | 8 | `wind-direction-n…nw` — vedi la riserva sotto |
| Cielo e agenda, di oggi | 17 | alba/tramonto, luna e sue fasi, orizzonte, stelle, eclissi |
| **Arcobaleno e momenti del giorno** | 11 | `rainbow`, `rainbow-clear`, `rainbow-cloud`, `time-morning…late-night` |
| **Allerte, per tipo di rischio** | 14 | `weather-alert(-day/-night)`, `wind-alert`, `thermometer-alert`, `uv-index-alert`, `water-alert`, `fire-alert`, `avalanche-danger-alert`, `tornado`, `hurricane`, `cyclone`, `waterspout`, `falling-rocks-alert` |

- [x] **Due debiti che questa lista salda**, e sono scritti nel codice, non dedotti:
      `ChiaroIcons.pollen` oggi disegna `mc_dust` e il suo commento dice «Meteocons v2 non ha
      un'icona per i pollini (**v3 sì**) … serve finché la famiglia v3 non si stabilizza»;
      `ChiaroIcons.rainbow` disegna `mc_partly_cloudy_day_rain` e dice «Meteocons v2 non ha un
      arcobaleno». Sono le due caselle che la Fase 2 ha lasciato aperte e che qui si chiudono
- [x] **La regola sulle icone graduate**: si spedisce un glifo per banda **solo dove la banda
      è già calcolata e già detta a parole** (`WeatherText.uvMeaning`, `pressureMeaning`,
      `windMeaning`, `pollenLevel`). Se il glifo dicesse un livello che la riga accanto non
      dice, sarebbe un secondo verdetto senza la sua aritmetica (DESIGN §1.2)
- [x] **La direzione del vento: una sola icona, ruotata** (deciso l'11 set 2026, dopo che il
      committente aveva chiesto se usare le otto). Non si spediscono le otto, e le ragioni
      sono tre, tutte lette nel codice e nel disegno, non dedotte:
      1. **sarebbe un passo indietro, non un'approssimazione.** `ui/components/WindArrow.kt`
         disegna già la direzione **esatta**, ruotando di `fromDegrees + 180` in continuo:
         non sedici punti, infiniti. Otto glifi fissi vorrebbero dire secchi da 45°;
      2. i glifi hanno **le lettere N/E/S/W disegnate come path**. In italiano l'ovest è
         **O**, non W: è testo inglese dentro un'immagine, non traducibile, contro la regola
         che in questo prodotto tutto ciò che sta a schermo si localizza;
      3. l'ago di Meteocons punta da **dove il vento viene**; Chiaro punta **dove l'aria
         va**, con la sua motivazione scritta (revisione delle schede, 8 set 2026).
         Importarlo così rovescerebbe in silenzio una decisione presa.

      Quel che si fa invece, e che dà lo stesso guadagno visivo: si importa
      `wind-direction-n` **tenendo solo il gruppo `Pointer`** — l'ago è un path solo, bbox
      x 57,5–70,5 e y 40,5–84,0 in una scatola di 128, simmetrico sul centro — si butta il
      gruppo `Letters`, e lo si ruota come `WindArrow` ruota già. Risultato: l'ago di
      Meteocons, nella mano della famiglia, esatto al grado, senza lettere inglesi e con la
      convenzione di Chiaro intatta. Da guardare al filmstrip: l'ago è più dettagliato della
      freccia disegnata a mano e il tile lo mostra a **16 dp**
- [ ] **Riserva sulle allerte**: le `Alarms` entrano per il **tipo** di rischio (i quattordici
      `WarningHazard` della Fase 12), **non per il livello**. Giallo/arancione/rosso restano
      `ic_warning` tinto: DESIGN §8.13 ha scelto un disegno al peso dei segni di verdetto
      apposta, e `code-yellow/orange/red` di Meteocons sono icone a colori pieni che in quello
      slot non ci stanno. Importarle sarebbe disfare una decisione, non aggiungere un'opzione
- [~] **Le dimensioni non si toccano** (punto 7): la scatola passa da 64 a 128, ma è
      `viewportWidth`/`viewportHeight` nell'XML e i dp della scala restano quelli
      (`WeatherIconSize`: striscia 42, settimana 38, riga e tile 34). **Da verificare sul
      dispositivo**, non solo sulla carta: v3 è un disegno nuovo e potrebbe riempire la sua
      scatola diversamente, e DESIGN §13.1 dice che è la nuvola semplice a decidere come si
      legge la famiglia in piccolo. Se il peso ottico cambia, si dichiara e si rimisura la
      scala — non si cambia un padding
- [x] **Lo stile**: **line + flat**, per gli argomenti della sezione sopra, con **line
      predefinito** come oggi. Si costruiscono insieme, perché condividono il convertitore di
      maschere e non hanno nient'altro da convertire. Se lo spike delle maschere fallisce non
      cade un secondo stile: **cade la fase**, perché senza maschere il line non ha i cieli
      composti. È per questo che lo spike sta prima di tutto
- [x] **La licenza**: `licenses/Meteocons-MIT.txt` porta il testo della v3 (il copyright
      passa da «2020-2021» a «2020-present») e dice da quale pacchetto viene. `UPSTREAM.md`
      **non** andava toccato: parla solo di `:core`, e le icone sono `:app`
- [x] **DESIGN §13.1 riscritta**, e con lei §7.1 (l'importatore ha un nome nuovo), la nota
      dei generatori (erano tre, `gen_vivid_icons.py` è stato cancellato: un set per fondo gli
      ha tolto il lavoro) e **l'eccezione dichiarata del widget Cielo, rimisurata** — vedi
      sotto. `CLAUDE.md` dice che niente sotto `res/drawable/mc3*` e `MeteoconsSets.kt` si
      modifica a mano, e `README.md` conta 765 test invece di 754

### Quel che non si è potuto misurare

Le maschere non sono state **convertite**, solo lette: che la riscrittura clip-path + verso
invertito, con la coppia di gruppi annidati che tiene fermo il sole, renda identica
l'originale **non è dimostrato**, ed è il rischio numero uno della fase — per questo è uno
spike e non una casella, e per questo ora può far cadere la fase intera invece che un solo
stile. I gradienti dentro un widget Glance non sono stati provati.

E le 519 non sono state **guardate**: sono state contate, misurate sul colore, sull'elemento
e sull'animazione, mai messe su uno schermo. Vale in particolare per la scelta line + flat,
che è argomentata sulle conversioni risparmiate e **non** su come i due disegni stanno
accanto nella striscia a 42 dp. `tools/icon_filmstrip.py` esiste già ed è quello che chiude
ognuna di queste caselle.

Una correzione a questa stessa fase, registrata perché è la regola della serie: la prima
stesura contava `#fff` e `#000` fra i colori del disegno e ne ricavava che fill fosse il più
sicuro. Erano i riempimenti delle **maschere**, non inchiostro. Rimisurato a maschere escluse,
sul sottoinsieme giusto, la conclusione si è rovesciata.

---

## Note trasversali

- **Il fork non si dimentica**: quando un bug del core va corretto due volte, si estrae
  `weather-core` (VISION §7.3). `UPSTREAM.md` è quello che rende l'estrazione un
  pomeriggio invece che uno scavo.
- **Batteria**: un solo job periodico per tutto, allarmi inesatti, nessun servizio in
  foreground, nessuna posizione in background. Vale già da adesso, non da una fase di
  ottimizzazione.
- **Niente radar**: il provider non ha immagini. È una posizione dichiarata, non una
  mancanza da nascondere. Riesaminata il 9 set 2026 (Radar-DPC esiste, è libero, copre l'Italia:
  le misure sono nella Fase 11) e lasciata così per scelta del committente.
- **Le allerte ufficiali si citano, non si traducono** (VISION §8, dal 9 set 2026): livello,
  rischio, giorno e zona localizzano; le parole di un'autorità restano sue, nella sua lingua,
  etichettate come tali. E in Italia fa fede la Protezione Civile, sempre.
