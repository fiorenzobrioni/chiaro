# PLANNING.md — Chiaro

Il registro del lavoro: **ogni decisione e ogni deviazione si annotano con il motivo**
(regola della serie). Il perimetro del prodotto sta in `VISION.md`, il sistema di design in
`DESIGN.md`, la storia del core in `UPSTREAM.md`.

Quel che **non** si annota, dal 21 set 2026 su richiesta del committente: le valutazioni
finite con «non si fa niente». Una strada scartata prima di imboccarla non è una deviazione
dal piano, è la conversazione che ha portato a non deviare, e resta dove è nata. Un file che
cresce di chiacchiere smette di essere un piano e diventa un archivio in cui il piano non si
trova più.

## Come è fatto (21 ott 2026)

- **Questo file** è il piano vivo: lo stato, quel che è aperto, e il registro del periodo
  corrente. Le nuove decisioni si scrivono **in coda al registro**, una sezione `##` per
  intervento, con la data e chi l'ha chiesto, come sempre.
- **`docs/planning/NN-<periodo>.md`** sono i periodi chiusi del registro, dalla Fase 0 in poi,
  **congelati**: non si modificano. Una decisione che ne cambia una vecchia si scrive qui, con
  il rimando a quella che cambia (file e titolo).
- **`docs/planning/README.md`** è l'indice di ogni titolo `##` e `###` dell'archivio, con la
  riga: un rimando per nome nel codice («PLANNING, Fase 11», «I dati nuovi a schermo») che non
  è in questo file si trova lì. È scritto da `python tools/planning_index.py`, mai a mano, e
  cambia solo quando si archivia.
- **Si archivia** quando il registro qui sotto passa le ~1 000 righe, o a una release, la
  prima delle due: il registro diventa il file d'archivio successivo (la stessa testata dei
  precedenti), qui resta un registro vuoto con la sua data d'inizio, «Aperto» si rilegge, e si
  rilancia lo script dell'indice.

## Stato

- **Rilasciata**: 1.1.0 (1 ott 2026, `v1.1.0`). Quel che è entrato dopo sta nella sezione
  `[Unreleased]` del `CHANGELOG.md`.
- **In corso**: **Fase 15**, la schermata Avvisi dopo la revisione (dal 28 ott 2026): dodici step,
  uno per chat, in ordine di urgenza (quel che mente, poi il rumore, poi le migliorie, poi il valore
  nuovo); la revisione, le decisioni, le regole comuni e la descrizione di ogni step sono in
  `docs/planning/08-ottobre-27-28.md`, «Fase 15 — La schermata Avvisi: la revisione e i passi»;
  uno step chiuso scrive la sua sezione qui; 15.1 è chiuso. **Fase 14**, il motore contro i motori premium (dal 27 ott 2026): tredici step, uno per chat; i primi quattro (14.1–14.3 con la scadenza del 30 novembre, la visibilità ICON che sparisce; 14.4 la misura di ICON-2I, che ha detto no), il 14.6 (il caldo), il 14.7 (il vento), il 14.8 (la quota neve), il 14.9 (le unità), il 14.10 (la concordanza dei modelli, che ha detto no) e il 14.13 (le icone, prima dell'inverno) sono chiusi; 14.5 e 14.11 non esistono. La revisione, le decisioni, le regole comuni e la descrizione di ogni step sono in `docs/planning/07-ottobre-22-27.md`, «Fase 14 — Il motore contro i motori premium»; gli step chiusi dal 27 ott 2026 e l'attesa di 14.12 sono in `docs/planning/08-ottobre-27-28.md`; uno step chiuso da qui in poi scrive la sua sezione qui.

## Aperto

Quel che resta da fare o da decidere, in un posto solo: una casella si apre qui, con il rimando
alla sezione del registro da cui nasce, e si chiude qui, con quella che la chiude.

- [ ] **Il cielo d'inverno contro il satellite.** La regola 6 è misurata su ottobre soltanto:
  l'archivio del satellite di Open-Meteo parte da febbraio 2026. Rilanciare
  `tools/measure_against_metar.py` su dicembre–gennaio 2026-27 e rileggere STATES §11 (archivio 07,
  «Il motore dopo la revisione», 26 ott 2026).
- [x] **14.1** La sonda del provider, il quarto d'ora dove è calcolato, le regole (Fase 14). Fatto il 27 ott 2026 (archivio 07, «14.1»).
- [x] **14.2** La nebbia senza la visibilità di ICON: la misura. Fatto il 27 ott 2026 (archivio 07, «14.2»): la regola scelta entra con 14.3, subito.
- [x] **14.3** La nebbia senza la visibilità di ICON: il codice, e il tile. Fatto il 27 ott 2026 (archivio 07, «14.3»): la regola di 14.2 è nel motore e vale da oggi, un mese prima di DWD.
- [x] **14.4** ICON-2I contro ICON-EU e ICON-D2: la misura. Fatto il 27 ott 2026 (archivio 08, «14.4»): ICON-2I non vince in nessuna cella decisiva, non entra.
- [x] **14.5** ICON-2I nel report, dove ha vinto (solo se 14.4 lo dice). Chiusa il 27 ott 2026 senza lavoro: 14.4 ha detto no (archivio 08, «14.4»).
- [x] **14.6** Il caldo: il gradino della frase e il pericolo. Fatto il 27 ott 2026 (archivio 08, «14.6»).
- [x] **14.7** Il vento nel «Maltempo», con la quota del luogo. Fatto il 27 ott 2026 (archivio 08, «14.7»): le soglie sono quelle della tabella MeteoSwiss letta dal documento, non quelle scritte nello step.
- [x] **14.8** La quota neve. Fatto il 28 ott 2026 (archivio 08, «14.8»): la riga nel giorno aperto e nella sera, due variabili in più (5,4 chiamate la previsione, 8,4 il fetch).
- [x] **14.9** Le unità: m/s, nodi, Beaufort, inHg, mmHg. Fatto il 28 ott 2026 (archivio 08, «14.9»): i default restano km/h e hPa, gli id canonici delle regole restano `_kph` e `_mb`.
- [x] **14.10** La concordanza dei modelli: la misura. Fatto il 28 ott 2026 (archivio 08, «14.10»): lo scarto fra i modelli dice quanto si sbaglia in media, non se il giorno è sbagliato; «non concordano» non è vera due volte su tre a nessuna soglia che scatti.
- [x] **14.11** La concordanza a schermo (solo se 14.10 lo dice). Chiusa il 28 ott 2026 senza lavoro: 14.10 ha detto no (archivio 08, «14.10»).
- [ ] **14.12** Gli avvisi ufficiali fuori dall'Italia, a chiusura garantita: **in attesa** dal 28 ott 2026, su via libera del committente, quando ci sarà una base di lettori in Europa (archivio 08, «14.12 resta in attesa»).
- [x] **14.13** Le icone che mancano: pioggia e pioviggine gelata, pioggia e neve possibile, composte. Fatto il 27 ott 2026 (archivio 08, «14.13»).
- [x] **15.1** Il «vero adesso» delle regole legge il report di Oggi, dice l'età, e l'anteprima dell'editor resta viva (Fase 15, archivio 08). Fatto il 28 ott 2026 (qui sotto, «15.1»): una funzione sola prepara il report per Oggi e per Avvisi, `today.*` legge la riga datata oggi, la pagina rinfresca da sé.
- [x] **15.2** Lo schermo non promette un suono che non può arrivare: la finestra del bollettino per luogo, lo stato «nessun luogo», il bollettino già uscito, le parole dello switch (Fase 15). Fatto il 28 ott 2026 (qui sotto, «15.2»).
- [ ] **15.3** Un canale silenziato dal telefono è detto a schermo, con la strada per riaprirlo (Fase 15).
- [ ] **15.4** Il motore delle regole: il riarmo alla riaccensione; il nome unico e l'id nello storico (Fase 15).
- [ ] **15.5** Le soglie nelle unità del lettore; lo slider senza la fila di tacche (Fase 15).
- [ ] **15.6** Il rumore che va via: la riga «in qualunque ora», la legenda del tratteggio, l'idea «Dodici ore asciutte», il flag `userRules` (Fase 15).
- [ ] **15.7** Le righe di Avvisi con le misure del Cielo; l'ora relativa; «Cosa guardare» in uno sheet a quattro gruppi (Fase 15).
- [ ] **15.8** Le graffe del messaggio diventano parole (Fase 15).
- [ ] **15.9** Le variabili «domani» e l'idea «Neve domani» (Fase 15).
- [ ] **15.10** Le quantità nelle prossime ore: millimetri e centimetri (Fase 15).
- [ ] **15.11** La regola legata a un luogo: il modello, il motore, il worker e il peso in chiamate (Fase 15).
- [ ] **15.12** La regola legata a un luogo: l'editor, la card, la notifica (Fase 15).

Riletto il 28 ott 2026, all'archivio 08, lo stesso giorno dell'apertura della Fase 15: le caselle sopra sono della Fase 14 (14.12 in attesa) e della Fase 15 (in `docs/planning/08-ottobre-27-28.md`), nessuna proposta non decisa. Riletto il 27 ott 2026, all'archivio 07: le caselle della Fase 14, nessuna proposta non decisa. Fino al 26 ott 2026 nessuna casella aperta (riletto il 22 ott 2026, all'archivio 06):
le ultime chiuse quel giorno stanno in `docs/planning/06-ottobre-20-22.md`, «Cielo: la finestra
reale nella card Stanotte», «Il widget «Momenti del cielo» va via», «Le notifiche: ogni riga nella
forma che sta in una riga», «Il widget «Le prossime ore» va via».

---

## Registro dal 28 ott 2026, dopo l'archivio 08

## 15.1 — Il «vero adesso» legge il report di Oggi (committente, 28 ott 2026)

**Fatto.** La card di ogni regola e l'anteprima dell'editor leggono il report **preparato come
Oggi lo prepara**, e la pagina lo tiene fresco da sé. Tre pezzi.

- **`WeatherNow.prepare`** (`:core:domain`, nuovo): `CurrentEstimate.apply` poi
  `WeatherRecency.trim`, null quando del report non resta niente sul presente. È la riga che
  `TodayUiState.build` faceva in casa dal 24 set 2026, tirata fuori perché due lettori ne facciano
  una sola; Oggi (e i widget, che costruiscono da lì) passano da qui, e il controllo `coversNow` separato di Oggi se
  ne va (la stessa domanda: resta qualcosa dopo il taglio).
- **`today.*` legge la riga datata oggi** (`RuleVariables.today`, `now.toLocalDate()`), null se
  non c'è, come `AlertEngine.findDailySummary` fa dal 23 set 2026. Con una cache di ieri la prima
  riga è ieri, e «la massima di oggi» era quella di ieri: sul report preparato le due letture
  coincidono (il taglio toglie ieri), sul report grezzo del worker pure (un fetch appena fatto
  comincia da oggi), e la lettura per data chiude l'unico caso in cui divergevano.
- **La pagina rinfresca in silenzio** (`AlertsViewModel`): un flusso per luogo attivo, vivo solo
  finché la pagina è guardata, che legge la cache prima di tutto (niente spinner, mai), poi chiede
  al repository per la strada di Oggi — `getWeather` senza forza, il cui TTL è
  `WeatherFreshness.ProviderResolution`: sotto i 15 minuti è un hit e non costa niente — all'apertura
  e a ogni minuto oltre il quarto d'ora, con il `RefreshBackoff` di Oggi dopo un errore, e mai
  sotto il risparmio batteria a meno che non ci sia nessun report (le eccezioni di Oggi: una
  richiesta che nessuno ha fatto ad alta voce si rinvia, una pagina vuota non è una pagina più
  economica). Il tick del minuto, che stava in `state`, vive lì. Il report tenuto è grezzo e porta
  la chiave del luogo, così un cambio di luogo non legge mai il report di quello di prima; le card
  lo leggono preparato al minuto della lettura. L'errore di rete è muto: la pagina tiene quel che
  ha e ne dice l'età.
- **L'età a schermo**: `Content` porta `lastSync` e `isStale` (la soglia di `WeatherFreshness`,
  la stessa del chip); oltre la soglia il titolo «I tuoi» porta a fine riga le parole del chip
  («Aggiornato 3 ore fa», `freshnessAge` di Oggi, `bodySmall` in `onSurfaceVariant` sulla base del
  titolo, come il `hint` di `SectionTitle`), e solo quando ci sono card: un'età sopra un gruppo
  vuoto non qualifica niente. Nessuna stringa nuova: i plurali del chip ci sono già in IT e EN.
- **L'anteprima dell'editor** si ricalcola anche al minuto e a ogni report che arriva:
  il `LaunchedEffect` prende `lastSync` fra le chiavi e cicla ogni minuto; i 300 ms di attesa dopo
  un tasto restano.

**Il peso in chiamate.** Nessuna richiesta nuova e nessuna variabile nuova: è la cache di Oggi,
lo stesso fetch da 8,4 chiamate. Avvisi tenuta aperta pesa come Oggi tenuta aperta, un fetch ogni
quarto d'ora al più; chi arriva da Oggi la trova fresca e non spende niente. Prima pesava zero e
mentiva.

**Decisioni e deviazioni.**

- *La verifica «a mano» che la card e la riga dell'agenda dicano la stessa cosa è un test*
  (`RuleReadingTest`, Robolectric): una cache di Milano presa ieri alle 10 letta oggi alle 10:30,
  con il blocco `current` a 25° e le righe di oggi a 12°, la massima di ieri 30 e quella di oggi
  20. Una regola «massima di oggi ≥ 25» dice «Adesso no · è 20°» sulla card e non ha riga
  nell'agenda; «temperatura adesso ≥ 20» dice no con 12° (stimato) e l'agenda non la porta; le
  soglie che oggi supera dicono sì su entrambe. Prima la card diceva sì su ieri e sul mattino di
  ieri.
- *Il report campione non si tocca*: i suoi giorni partono tre giorni dopo la sua data, e lo
  usano decine di test. I tre che leggono `today.*` (`RuleVariablesTest`, `RuleEngineTest`,
  `RuleTextTest`) ridatano i giorni perché il primo sia quello di `now`; `RuleEngineTest` non
  cambia nella sostanza, come lo step chiede.
- *Gli screenshot del README non cambiano*: le immagini di Avvisi sono disegnate cinque minuti
  dopo il fetch che è la registrazione stessa, nella stessa ora, quindi la preparazione è
  l'identità (niente stima sotto i 60 minuti, niente da tagliare) e il report non è stantio;
  `MilanRecording` passa comunque dalla stessa preparazione, così la foto legge come la pagina.
  Resi di nuovo e confrontati con i file committati: identici byte per byte, git non li vede cambiati.
- *Il Cielo non rinfresca ancora*: legge la cache come Avvisi faceva. Il suo verdetto dice l'età
  del dato da sé (`staleAfter`), e lo step riguarda Avvisi; se un giorno servirà, il flusso del
  custode è scritto perché si possa spostare.

**Come è stato verificato.** `WeatherNowTest` (nuovo: una cache di ieri porta oggi in testa e il
`current` stimato; un report dell'ora è restituito com'è; un report oltre l'orizzonte è null),
`RuleVariablesTest` (la riga datata oggi, mai la prima; la cache di ieri; nessun oggi),
`RuleReadingTest` (nuovo, sopra), `RuleEngineTest`, `RuleTextTest`, `RuleNowTest`,
`TodayStateBuilderTest`; suite completa verde (`./gradlew test :app:testDebugUnitTest`,
2 627 test, 15 saltati: gli screenshot, che girano solo su richiesta); lint verde (`:app:lintDebug`). DESIGN §8.9 («Is it true now», la riga e l'editor),
VISION §5.4, CHANGELOG `[Unreleased]`.

## 15.2 — Lo schermo non promette un suono che non può arrivare (committente, 28 ott 2026)

**Fatto.** Quattro pezzi, tutti nella pagina Avvisi; il motore non è toccato.

- **La finestra del bollettino è del luogo, non dello switch.** `alertWindows` la disegna solo
  quando `content.warnings` non è `Unavailable` (`bulletinPossible`: una zona in attesa del primo
  bollettino, o con quello di ieri, può ancora suonare; un luogo fuori da ogni zona mai, qualunque
  cosa dica lo switch), e l'ora «15–17» sopra lo switch segue la stessa regola. Lo switch resta
  dov'è: è un'impostazione, e la card sopra dice già se il luogo ha una zona. Prima, per Lione, la
  card diceva «disponibili per i luoghi in Italia» e sotto «Il prossimo, oggi · Il bollettino ·
  15–17».
- **Il bollettino già uscito oggi non è il prossimo a suonare.** `nextWindow` prende un insieme
  di finestre `spent`: una finestra consumata è saltata, aperta o no, e torna domani con le altre.
  È consumata la finestra del bollettino quando `warnings` è `Current` e `issuedAt` è datato oggi
  (`AlertWindow.spentOn`, con l'oggi del luogo, che per un luogo in Italia è quello dell'emittente).
  Le finestre dei riepiloghi non si consumano mai: lo schermo non sa se il riepilogo è arrivato, e
  la regola del 14 ott resta (una finestra, non una promessa). Alle 16 con il bollettino delle
  14:34 la riga dice «Il prossimo, oggi · Il riepilogo della sera», dove diceva «Adesso · Il
  bollettino · 15–17».
- **Nessun luogo è lo stato vuoto.** `AlertsUiState.NoPlace`, emesso dal view model quando
  l'attivo è `ActiveSource.None`; la pagina disegna il `NoPlaceState` di Oggi (lo stesso
  composable, che prende ora la frase della pagina: «Aggiungi una città: gli avvisi e le allerte
  parlano di un luogo»), con l'azione che apre i luoghi, e niente altro. Prima disegnava la pagina
  intera con «disponibili per i luoghi in Italia» in testa.
- **Le parole dello switch.** «Di solito in quella fascia» rimandava a una colonna che TalkBack non
  legge insieme alla riga: ora «Di solito tra le 15 e le 17, mai più di una per bollettino» («Usually
  between 15:00 and 17:00» in EN), con i numeri che la riga già riceve; le due costanti
  `BulletinFrom`/`BulletinTo` sono le stesse della striscia, dell'ora sopra lo switch e della frase.

**Il peso in chiamate.** Nessuna richiesta nuova, nessuna variabile nuova: 8,4 chiamate il fetch,
come prima.

**Decisioni e deviazioni.**

- *Lo stato «nessun luogo» è uno stato del view model, non una `Content` con il nome vuoto*: la
  pagina non ha niente da dire senza un luogo (nessuna zona per cui rispondere, nessuna previsione
  su cui leggere una regola), e un `Content` senza luogo avrebbe lasciato alla pagina il compito di
  non disegnare pezzo per pezzo. L'editor non si apre perché non c'è `Content`; le regole restano
  salvate e tornano con il primo luogo.
- *Lo stato vuoto è il composable di Oggi, non quello del Cielo*: §8.11 lo nomina come lo stato
  vuoto dell'app; il Cielo ha il suo (allineato a sinistra, `titleMedium`) e non è di questo step.
- *La finestra consumata resta disegnata sulla striscia*: la striscia è la forma della giornata,
  non la lista di quel che deve ancora arrivare; è la riga sopra a dire il prossimo.
- *Lo screenshot `alerts.png` cambia*, contro la previsione dello step («a Milano non dovrebbe»):
  la riga dello switch è in foto, e la nuova frase è una riga più lunga. La card in testa non
  cambia (il bollettino della registrazione è di ieri, le 12:15 del 25 set: «Next, today · The
  bulletin · 15–17» resta). Rigenerato e guardato; `alerts-yours.png` non cambia.

**Come è stato verificato.** `NextWindowTest` (tre casi nuovi: la finestra consumata saltata prima
di aprirsi e mentre è aperta, da sola torna domani, non sposta quelle prima di lei),
`AlertsPromiseTest` (nuovo, `testDebug`, Robolectric: Lione senza «15–17» né «Il bollettino» e con lo
switch ancora in pagina; il bollettino di oggi alle 16 non dice «Adesso» e il prossimo è di oggi;
quello di ieri alle 16 dice «Adesso · Il bollettino»; nessun luogo è il titolo, la frase e
l'azione, senza i gruppi), `AlertsYoursEmptyTest`, `NotificationsPromisedTest`; suite completa
verde (`./gradlew test :app:testDebugUnitTest`, 2 637 test, 15 saltati: gli screenshot, che girano solo su richiesta); lint verde (`:app:lintDebug`). DESIGN
§8.9b (la finestra del luogo, il bollettino consumato, lo stato «nessun luogo»), VISION §5.4,
CHANGELOG `[Unreleased]`, `alerts.png`.
