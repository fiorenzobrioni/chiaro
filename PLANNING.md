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

- **Rilasciata**: 2.0.0 (2 nov 2026, `v2.0.0`; archivio 10, «La 2.0.0»), dopo la 1.1.0
  (1 ott 2026, `v1.1.0`). Quel che entra dopo sta nella sezione `[Unreleased]` del `CHANGELOG.md`.
- **Chiusa il 29 ott 2026**: **Fase 15**, la schermata Avvisi dopo la revisione (dal 28 ott 2026): dodici step,
  uno per chat, in ordine di urgenza (quel che mente, poi il rumore, poi le migliorie, poi il valore
  nuovo); la revisione, le decisioni, le regole comuni e la descrizione di ogni step sono in
  `docs/planning/08-ottobre-27-28.md`, «Fase 15 — La schermata Avvisi: la revisione e i passi»;
  gli step, chiusi tutti e dodici, sono in `docs/planning/09-ottobre-28-29.md`. La rilettura
  dei dodici (archivio 10, «La Fase 15 riletta», 29 ott 2026) ha lasciato due step di coda, 15.13 e
  15.14, chiusi lo stesso giorno (archivio 10, «15.13» e «15.14»).
- **Aperta il 10 nov 2026**: **Fase 16**, la schermata Cielo come Oggi e il cielo da esplorare: undici
  step, uno per chat (16.1–16.2 la pagina, 16.3–16.5 il motore e il dato, 16.6–16.10 l'esploratore,
  16.11 la chiusura). La revisione, le tavole approvate, le decisioni, le regole comuni e la
  descrizione di ogni step sono in `docs/planning/11-novembre-2-10.md`, «Fase 16 — Cielo: la pagina
  come Oggi e il cielo da esplorare», con i primi due step chiusi (16.1, 16.2); gli step dal 16.3
  al 16.7 sono in `docs/planning/12-novembre-10-11.md`; uno step chiuso da qui in poi scrive la
  sua sezione nel registro qui sotto.
- **Ancora aperta**: **Fase 14**, il motore contro i motori premium (dal 27 ott 2026): tredici step, uno per chat; i primi quattro (14.1–14.3 con la scadenza del 30 novembre, la visibilità ICON che sparisce; 14.4 la misura di ICON-2I, che ha detto no), il 14.6 (il caldo), il 14.7 (il vento), il 14.8 (la quota neve), il 14.9 (le unità), il 14.10 (la concordanza dei modelli, che ha detto no) e il 14.13 (le icone, prima dell'inverno) sono chiusi; 14.5 e 14.11 non esistono. La revisione, le decisioni, le regole comuni e la descrizione di ogni step sono in `docs/planning/07-ottobre-22-27.md`, «Fase 14 — Il motore contro i motori premium»; gli step chiusi dal 27 ott 2026 e l'attesa di 14.12 sono in `docs/planning/08-ottobre-27-28.md`; uno step chiuso da qui in poi scrive la sua sezione qui.

## Aperto

Quel che resta da fare o da decidere, in un posto solo: una casella si apre qui, con il rimando
alla sezione del registro da cui nasce, e si chiude qui, con quella che la chiude.

- [ ] **Il cielo d'inverno contro il satellite.** La regola 6 è misurata su ottobre soltanto:
  l'archivio del satellite di Open-Meteo parte da febbraio 2026. Rilanciare
  `tools/measure_against_metar.py` su dicembre–gennaio 2026-27 e rileggere STATES §11 (archivio 07,
  «Il motore dopo la revisione», 26 ott 2026).
- [ ] **Il meteo di Chiaro ad altre app con la trasmissione di Gadgetbridge** (issue #72). Decisa,
  da fare quando c'è spazio: `ACTION_GENERIC_WEATHER`, spenta di default, gli stati di Chiaro e non
  il `weather_code`, nessuna richiesta in più se il job gira già, niente coordinate, un intent
  esplicito per app (archivio 11, «Il meteo di Chiaro ad altre app: la trasmissione di Gadgetbridge»,
  2 nov 2026). Quando esce, si scrive sull'issue e la si chiude.
- [x] **L'icona nuova sul telefono** (archivio 11, «L'icona nuova: l'ora blu», 9 nov 2026): l'icona
  nella maschera del telefono, l'icona a tema, l'icona piccola delle notifiche alla sua misura.
  Fatto dal committente il 9 nov 2026 (archivio 11, «Le icone nuove provate sul telefono»).
- [x] **Le icone nuove del Cielo sul telefono** (archivio 11, «Le icone del Cielo, rilette tutte», 9 nov
  2026): la lista del Cielo e il catalogo nelle due famiglie d'icone e nei due temi, e la
  finestra; la luce zodiacale soprattutto, la piu' debole delle sei. Fatto dal committente il
  9 nov 2026, la luce zodiacale compresa (archivio 11, «Le icone nuove provate sul telefono»).
- [ ] **La revisione di Oggi e del cielo sul telefono** (archivio 11, «Oggi e il motore del cielo: la
  revisione da app premium», 9 nov 2026). Quel che un container non può dire: **la cifra della
  GPU** con il cielo peggiore salito da 25 a 28 campioni (`adb shell dumpsys gfxinfo`, sotto 3 ms
  a frame su un telefono della classe del Pixel 6a, DESIGN §3.5; se non tiene, i raggi del sole
  sono i primi a uscire: `rays` a zero in `skyScene`); il nastro trascinato sotto il dito (il
  passo d'ora, il tocco aptico, il ritorno ad adesso, lo scorrimento della pagina che parte da
  lì); la nebbia e il lampo in un
  giorno che li ha; la riga della settimana con lo chevron a 360dp e a 130% di carattere.
- [ ] **L'hero dopo la revisione, sul telefono** (archivio 11, «L'hero dopo la revisione: quanto costa, le
  icone ferme sotto il dito, il cielo con un interruttore suo, il luogo che tornava indietro», 10 nov
  2026). Quel che i test non dicono: le icone ferme mentre si trascina il nastro e lo sfarfallio
  che se ne va; «Cielo animato» spento, su Oggi e sulla card Stanotte; il luogo cambiato da Cielo e
  da Avvisi che su Oggi è quello nuovo dal primo frame, senza viaggio.
- [x] **La planata del cielo a ogni minuto** (archivio 11, «L'hero dopo la revisione», «Quanto costa
  l'hero rispetto a prima»). Fatto il 10 nov 2026: plana solo un cambiamento che si vede, e un
  cielo in cui niente si muove non si ridisegna (archivio 11, «Il cielo non plana a ogni minuto, e un
  cielo fermo non si ridisegna»).
- [ ] **Il peso della cifra: Regular nell'app, Bold nelle card** (archivio 11, «Oggi e il motore del cielo: la revisione da app premium», «La cifra»).
  La revisione accettata chiedeva 300; è 400, il più leggero che le due famiglie disegnano, e
  le card restano Bold: la regola del 20 set 2026 («Una temperatura eroe è Bold», DESIGN §5)
  vale ora per le card soltanto. Da confermare dal committente guardandole fianco a fianco,
  come allora; tornare indietro è una riga (`HeroWeight` in `Type.kt`) e il suo test.
- [ ] **La nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo** (la card
  Stanotte fino al 16.1; archivio 11, stessa sezione, «Dove non arriva»): lo shader li disegna, `WindowScene` e `TonightSky` non li
  chiedono ancora. Proposta, non decisa.
- [x] **16.1** La pagina come Oggi: l'hero a tutta larghezza, il luogo sul cielo, il verdetto come cifra, il nastro della notte (Fase 16). Chiuso il 10 nov 2026 (archivio 11, «16.1 — La pagina come Oggi: l'hero»).
- [x] **16.1 sul telefono** (archivio 11, «16.1 — La pagina come Oggi: l'hero», «Cosa conferma solo il telefono»): le icone della
  barra di stato nel passaggio fra Oggi e Cielo, il nastro in una notte serena e senza luna,
  il piede dell'immagine sotto un cielo coperto, il dito sul nastro e sul cielo. Fatto: dato per provato dal committente il 10 nov 2026 (archivio 12, «Le prove sul
  telefono della Fase 16, da 16.1 a 16.3»).
- [x] **16.2** I momenti come l'agenda di Oggi; «Aggiungi» nel titolo al posto del pulsante flottante (Fase 16). Chiuso il 10 nov 2026 (archivio 11, «16.2 — I momenti come l'agenda di Oggi»).
- [x] **16.2 sul telefono** (archivio 11, «16.2 — I momenti come l'agenda di Oggi», «Cosa conferma solo il telefono»): le icone del
  Cielo a 34dp, che erano a 51 per scelta del committente (decisione 3: da confermare o da
  riportare indietro), la luce zodiacale soprattutto; «Aggiungi» accanto al titolo; la riga con
  il carattere grande. Fatto: dato per provato dal committente il 10 nov 2026 (archivio 12, «Le prove sul
  telefono della Fase 16, da 16.1 a 16.3»).
- [x] **16.3** Il motore: Marte e Saturno; sorgere, culmine e tramonto di un oggetto (Fase 16). Chiuso il 10 nov 2026 (archivio 12, «16.3 — Il motore: Marte e Saturno, e quando un oggetto sorge, culmina e tramonta»).
- [x] **16.3 sul telefono** (archivio 12, «16.3», «Cosa conferma solo il telefono»): Marte e Saturno
  nell'hero di Oggi e nella finestra in una notte in cui ci sono; la tinta di Marte accanto a
  Giove; i due nomi sulla finestra. Fatto: dato per provato dal committente il 10 nov 2026 (archivio 12, «Le prove sul
  telefono della Fase 16, da 16.1 a 16.3»).
- [ ] **Le immagini dei widget nel README dipendono dal fuso della macchina** (archivio 12, «16.3»,
  «Come è stato verificato»). La finestra scrive l'ora del luogo accanto al nome quando
  l'orologio del luogo non è quello del telefono; nel test il telefono è la macchina, e su una
  con un altro fuso esce «Milan · 01:00» (nelle immagini committate), su una col fuso di Milano no.
  Proposta, non decisa: `ReadmeScreenshots` fissa il fuso della JVM a quello del luogo, e le due
  immagini si rigenerano senza l'ora.
- [x] **16.4** Il motore: la notte di un'altra data; l'elenco «Da vedere» come dato (Fase 16). Chiuso il 10 nov 2026 (archivio 12, «16.4 — Il motore: la notte di un'altra data, e cosa vale la pena guardare»).
- [x] **Un verdetto su una finestra che la previsione copre a metà** (archivio 12, «16.4», decisione 4).
  `SkyVerdictEngine` giudica le ore che trova: se la previsione finisce dentro la finestra, il
  verdetto è della parte che c'è, per ogni riga dell'app (uno sciame o la notte al settimo
  giorno). Fatto il 10 nov 2026 con la proposta com'era: «Presto per dirlo», con il motivo
  «la previsione non arriva ancora così lontano» (archivio 12, «Le due caselle del 16.4», «La finestra coperta a metà»).
- [x] **La finestra di uno sciame che si apre al picco** (archivio 12, «16.4», decisione 10). Quando
  il picco cade di notte la finestra del lavoro va dal picco all'alba: per le Geminidi del
  2026 sono sedici minuti (05:51–06:07), in una notte in cui il radiante è su dal crepuscolo
  all'alba. Fatto il 10 nov 2026 con la proposta com'era: la notte intera del picco (archivio 12,
  «Le due caselle del 16.4», «La notte dello sciame»).
- [x] **Le due caselle del 16.4 sul telefono** (archivio 12, «Le due caselle del 16.4», «Cosa conferma
  solo il telefono»): la riga di uno sciame in «In arrivo» con la data della sera, il suo
  promemoria all'imbrunire, la notte al bordo della previsione che dice «presto per dirlo». Fatto: dato per provato dal committente il 12 nov 2026 (registro, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **Il verdetto del promemoria di una finestra è della sua prima ora** (fatto il 10 nov 2026, archivio 12, «Le due correzioni trovate con le caselle»; nato in «Le due
  caselle del 16.4», «Trovato e non toccato»). `SkyAlarmReceiver` giudica l'istante in cui la
  finestra si apre (`end = null`), la riga del Cielo tutta la finestra: per la notte buia e,
  dal 10 nov 2026, per uno sciame, il promemoria può dire «Bello» di una notte che la riga dice
  «Così così». Proposta, non decisa: il promemoria risolve la finestra del lavoro e la giudica
  intera, come la riga.
- [x] **Dal 1° gennaio lo sciame «prossimo» è quello dell'anno dopo** (fatto il 10 nov 2026, archivio 12, «Le due correzioni trovate con le caselle»; nato in «Le due caselle
  del 16.4», «Trovato e non toccato»). `SkyUpcoming.of` risolve un lavoro annuale nell'anno di
  oggi, e le Quadrantidi «del 2027» cadono nel gennaio 2028: dal 1° gennaio 2027 la riga di
  chi le segue dice l'anno dopo, e nella notte del picco (3–4 gennaio 2027) il cielo disegnato
  non ne nomina il radiante. Da verificare per gli altri lavori annuali la cui risposta cade
  nel gennaio dopo (l'alba più tarda). Proposta, non decisa: `SkyUpcoming.of` chiede anche
  l'anno prima, come `SkyScheduler.next` e l'elenco «Da vedere». **Da fare prima del 1°
  gennaio 2027.**
- [x] **16.5** Le costellazioni: la fonte e la licenza, la risorsa, i nomi (Fase 16). Chiuso il 10 nov 2026 (archivio 12, «16.5 — Le costellazioni: il dato»).
- [x] **16.5 sul telefono** (archivio 12, «16.5», «Cosa conferma solo il telefono»): la riga «Linee
  delle costellazioni» in Impostazioni → Dati e riconoscimenti, che apre la pagina della fonte. Fatto: dato per provato dal committente il 12 nov 2026 (registro, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **16.6** L'esploratore: la camera libera e il cielo a tutto schermo (Fase 16). Chiuso l'11 nov 2026 (archivio 12, «16.6 — L'esploratore: la camera libera e il cielo a tutto schermo»).
- [x] **16.6 sul telefono** (archivio 12, «16.6», «Cosa conferma solo il telefono»): il dito sul cielo
  (trascinare, pizzicare, il giro vicino allo zenit), la fluidità con le stelle a schermo, la Via
  Lattea disegnata dalla GPU del telefono, la pillola «Esplora» con il carattere grande, le icone
  chiare della barra di stato, il gesto indietro. Fatto: dato per provato dal committente il 12 nov 2026 (registro, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **16.7** L'esploratore: l'ora, la notte, le nuvole, le costellazioni (Fase 16). Chiuso l'11 nov 2026 (archivio 12, «16.7 — L'esploratore: l'ora, la notte, le nuvole, le costellazioni»).
- [x] **16.7 sul telefono** (archivio 12, «16.7», «Cosa conferma solo il telefono»): il dito sul nastro
  dentro l'esploratore, le linee delle costellazioni su uno schermo vero, i nomi in maiuscolo con
  il carattere grande, il calendario, il velo delle nuvole in una notte che le ha, «Adesso» che
  avanza da solo, il telefono di lato. Fatto: dato per provato dal committente il 12 nov 2026 (registro, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **«Costellazioni» accesa finché non la si spegne** (archivio 12, «16.7», decisione 10). Lo step
  diceva «scelta ricordata» e non da dove parte; parte accesa. Confermato dal committente il
  12 nov 2026 (registro, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **Una notte bianca non ha un nastro** (archivio 12, «16.7», «Trovato e non toccato»). Dove non
  c'è la notte astronomica (Stoccolma da maggio ad agosto, Londra fra fine maggio e metà luglio)
  l'esploratore ha un'ora sola, come l'hero. Fatto l'11 nov 2026 con la proposta com'era, per
  l'esploratore soltanto: lì il nastro va dal tramonto all'alba (archivio 12, «La notte bianca ha un
  nastro»). **Scritto senza compilare e senza lanciare i test: la CI è la prima a provarlo.**
- [x] **La notte bianca sul telefono** (archivio 12, «La notte bianca ha un nastro»): il nastro di un
  luogo a nord in una data d'estate (Stoccolma, 23 giugno, dal calendario), i suoi colori, la
  frase sotto, l'ora che si muove. Fatto: dato per provato dal committente il 12 nov 2026 (registro, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **16.8** L'esploratore: toccare un oggetto (Fase 16). Chiuso il 12 nov 2026, «16.8 — L'esploratore: toccare un oggetto».
- [ ] **16.8 sul telefono** (registro, «16.8», «Cosa conferma solo il telefono»): il dito sugli
  oggetti (i 24dp, due cose vicine), la planata, il gesto indietro che chiude il foglio, la
  campanella con il permesso delle notifiche e il promemoria che arriva, la pagina e il ritorno
  al foglio, il carattere grande, la luna toccata.
- [x] **Il foglio dell'oggetto al posto del pannello dell'ora** (registro, «16.8», decisione 1).
  La tavola 4 lo disegna come un foglio sopra il cielo; è nella pagina, ai piedi, e mentre è
  aperto l'ora non si muove. Confermato dal committente il 12 nov 2026 (registro, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **La campanella del foglio** (registro, «16.8», decisioni 3 e 4): mette il promemoria
  abituale, o trenta minuti dove l'abituale è «mai»; spenta lascia il momento fra i propri; la
  luna non ne ha; il canale silenziato dal telefono non è detto sul foglio. Confermato dal committente il 12 nov 2026 (registro, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **La riga di Giove sulla regola dell'elenco «Da vedere»** (registro, «16.8», «Trovato e non
  toccato»), come Marte e Saturno dal 16.8. Stesso buio (dal crepuscolo civile) e stesso
  pavimento (10°); la riga non ha il quarto d'ora minimo dell'elenco, e in una notte con due
  tratti prende il primo dove l'elenco prende quello con il punto più alto. Misurato il 12 nov
  2026 (registro, «Giove: la riga contro l'elenco, la misura»): in sei anni le finestre
  coincidono in ogni notte che ne ha due, e la riga ne dà in più da 5 a 11 l'anno, tutte sotto
  il quarto d'ora. Fatto il 12 nov 2026, nella stessa PR, sul sì del committente alla misura
  (registro, «Giove sulla regola dell'elenco»).
- [x] **16.9** «Da vedere», e la notte di un evento (Fase 16). Chiuso il 12 nov 2026, «16.9 — «Da vedere», e la notte di un evento».
- [ ] **16.9 sul telefono** (registro, «16.9», «Cosa conferma solo il telefono»): il foglio
  «Da vedere» sopra il cielo (aprirlo, trascinarlo, chiuderlo), i chip della notte con il
  carattere grande, il calendario sopra il foglio, la planata dopo una riga, l'anello intorno
  a una figura, il rimando sotto le righe di «In arrivo» sotto il dito (due bersagli in una
  riga), la pagina che si apre sulla notte di un evento.
- [x] **«Luna nuova, 3% illuminata» nell'elenco «Da vedere»** (registro, «16.9», «Trovato e non
  toccato»). L'elenco ha la luna dall'1% illuminato e il nome della fase è quello del giorno:
  la falce sottile all'alba usciva come «Luna nuova, 3% illuminata». Fatto il 12 nov 2026,
  nella stessa PR, con un'altra strada da quella proposta: dall'1% è «Falce sottile», nella
  riga e nel foglio della luna (registro, «Le scelte del 16.9 confermate, e i due «trovati»
  corretti»).
- [x] **16.10** «Punta il telefono», da provare sul telefono prima di tenerlo (Fase 16). Scritto il 12 nov 2026, «16.10 — «Punta il telefono»»; se si tiene lo dice la casella qui sotto.
- [ ] **16.10 sul telefono: si tiene o esce** (registro, «16.10», «Cosa conferma solo il telefono: se
  si tiene»). Lo step chiede di provarlo prima di tenerlo, e non è stato visto su nessun
  telefono: il cielo fermo a telefono fermo, il cielo dietro a un braccio che gira, il nord
  dov'è davvero, fra 60° e lo zenit, il telefono basso sull'orizzonte (il cielo si ferma sul
  suo pavimento), il telefono di lato, il pizzico e il tocco mentre punta, lo schermo che
  resta acceso, il riquadro della bussola. Se la bussola non regge un cielo che non balla,
  l'interruttore esce e resta scritto perché. Da confermare con lui le decisioni 2, 3 e 6.
- [ ] **«Dietro le nuvole» ricordata fra un avvio e l'altro, e la stessa per l'hero e per
  l'esploratore** (registro, «16.10», «Il parere chiesto su «Dietro le nuvole»»). Oggi è una
  preferenza sola, spenta di default e salvata (`skyBareSky`, chiesta «persistente» il 22 ott
  2026 per la card): accesa nell'esploratore, apre la scheda Cielo ogni sera su un cielo
  stellato sotto il verdetto di una notte coperta. Proposta, non decisa: il default resta
  spento e la scelta non si salva più; l'hero la tiene finché la pagina vive, l'esploratore si
  apre com'era l'hero e poi ha la sua. Il widget non cambia.
- [ ] **16.11** La chiusura: guida, VISION, README, CHANGELOG, la rilettura (Fase 16).
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
- [x] **15.1** Il «vero adesso» delle regole legge il report di Oggi, dice l'età, e l'anteprima dell'editor resta viva (Fase 15, archivio 08). Fatto il 28 ott 2026 (archivio 09, «15.1»): una funzione sola prepara il report per Oggi e per Avvisi, `today.*` legge la riga datata oggi, la pagina rinfresca da sé.
- [x] **15.2** Lo schermo non promette un suono che non può arrivare: la finestra del bollettino per luogo, lo stato «nessun luogo», il bollettino già uscito, le parole dello switch (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.2»).
- [x] **15.3** Un canale silenziato dal telefono è detto a schermo, con la strada per riaprirlo (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.3»).
- [x] **15.4** Il motore delle regole: il riarmo alla riaccensione; il nome unico e l'id nello storico (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.4»): il motore non cambia, è la pagina che riarma; lo storico scrive `id:nome` e legge per id, per nome le righe vecchie.
- [x] **15.5** Le soglie nelle unità del lettore; lo slider senza la fila di tacche (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.5»): la griglia è dell'unità, il salvataggio resta canonico, le tacche solo fino a venti passi.
- [x] **15.6** Il rumore che va via: la riga «in qualunque ora», la legenda del tratteggio, l'idea «Dodici ore asciutte», il flag `userRules` (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.6»): tutto via in uno step, le idee sono sei, una regola accesa tiene vivo il job da sola.
- [x] **15.7** Le righe di Avvisi con le misure del Cielo; l'ora relativa; «Cosa guardare» in uno sheet a quattro gruppi (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.7»): `ListRow` condiviso con il Cielo, l'icona resta a 36, «oggi alle», «ieri alle», lo sheet a quattro titoli.
- [x] **15.8** Le graffe del messaggio diventano parole (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.8»): una `VisualTransformation` dipinge la frase su un chip, il testo salvato tiene l'id, il tasto indietro toglie il chip intero; la resa su telefono, che restava da vedere al committente, è stata vista e tiene (29 ott 2026, archivio 10, «La Fase 15 riletta»).
- [x] **15.9** Le variabili «domani» e l'idea «Neve domani» (Fase 15). Fatto il 29 ott 2026 (archivio 09, «15.9»): sette `tomorrow.*` sulla riga datata domani, una voce per data da mezzogiorno, «Neve domani» dopo «Ghiaccio domattina», e la card che prima di mezzogiorno dice da quando può suonare.
- [x] **15.10** Le quantità nelle prossime ore: millimetri e centimetri (Fase 15). Fatto il 29 ott 2026 (archivio 09, «15.10»): le righe portano il totale e la neve dell'ora, quattro somme `next_Nh.*`, la pioggia senza l'acqua della neve, null se manca un'ora; una finestra sola per ogni `next_Nh.*`, l'ora in corso e le N−1 dopo, e il «Caldo intenso» conta sulla stessa.
- [x] **15.11** La regola legata a un luogo: il modello, il motore, il worker e il peso in chiamate (Fase 15). Fatto il 29 ott 2026 (archivio 09, «15.11»): `NotificationRule.place`, il job legge ogni luogo legato sulla sua previsione e nel suo fuso, 8,4 chiamate per luogo per periodo; la regola orfana si spegne e tiene il luogo; a schermo niente fino a 15.12.
- [x] **15.12** La regola legata a un luogo: l'editor, la card, la notifica (Fase 15). Fatto il 29 ott 2026 (archivio 09, «15.12»), con i punti che 15.11 gli lasciava: «Dove» nell'editor, la riga «A Bormio» con l'età di quella previsione, «Adesso sì/no» e «scattato l'ultima volta» dal luogo della regola, l'anteprima che fa il fetch, l'orfana il cui interruttore chiede dove, Oggi con le sole regole del luogo in pagina. La Fase 15 è chiusa.
- [x] **15.13** La coda della Fase 15 nel codice: il job legge i luoghi legati anche quando il luogo attivo non c'è o non risponde; l'orfana si spegne anche senza il job; il canale del Cielo silenziato è detto a schermo (archivio 10, «La Fase 15 riletta», 29 ott 2026). Fatto il 29 ott 2026 (archivio 10, «15.13»).
- [x] **15.14** La coda della Fase 15 nei documenti: la riga «Alerts» del README, «four groups» nel CHANGELOG, due frasi della guida (archivio 10, «La Fase 15 riletta», 29 ott 2026). Fatto il 29 ott 2026 (archivio 10, «15.14»): la Fase 15 è chiusa del tutto.

Riletto il 12 nov 2026, all'archivio 12, all'inizio del 16.8: le caselle nate dal 10 nov 2026 rimandano all'archivio 12 (`docs/planning/12-novembre-10-11.md`), la descrizione degli step della Fase 16 resta nell'archivio 11 (16.8–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, le due caselle del 16.4, 16.5, 16.6, 16.7, la notte bianca), il peso della cifra e «Costellazioni» accesa da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; due proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, e il fuso della macchina nelle immagini dei widget del README. Riletto il 10 nov 2026, all'archivio 11, all'inizio del 16.3: le caselle nate dal 2 nov 2026 rimandano all'archivio 11 (`docs/planning/11-novembre-2-10.md`), dove sta anche la descrizione degli step della Fase 16 (16.3–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.1, 16.2), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; una proposta non decisa, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo. Riletto il 2 nov 2026, all'archivio 10, con la 2.0.0: le caselle chiuse dal 29 ott 2026 (15.13, 15.14) rimandano all'archivio 10 (`docs/planning/10-verso-2.0.0.md`); restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, dopo la rilettura della Fase 15: aperte 15.13 e 15.14, la coda; e come prima 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, all'archivio 09: le caselle della Fase 15 sono tutte chiuse (in `docs/planning/09-ottobre-28-29.md`), restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 28 ott 2026, all'archivio 08, lo stesso giorno dell'apertura della Fase 15: le caselle sopra sono della Fase 14 (14.12 in attesa) e della Fase 15 (in `docs/planning/08-ottobre-27-28.md`), nessuna proposta non decisa. Riletto il 27 ott 2026, all'archivio 07: le caselle della Fase 14, nessuna proposta non decisa. Fino al 26 ott 2026 nessuna casella aperta (riletto il 22 ott 2026, all'archivio 06):
le ultime chiuse quel giorno stanno in `docs/planning/06-ottobre-20-22.md`, «Cielo: la finestra
reale nella card Stanotte», «Il widget «Momenti del cielo» va via», «Le notifiche: ogni riga nella
forma che sta in una riga», «Il widget «Le prossime ore» va via».

---

## Registro dal 12 nov 2026, dopo l'archivio 12

## 16.8 — L'esploratore: toccare un oggetto (committente, 12 nov 2026)

L'ottavo step della Fase 16 (archivio 11, «Fase 16 — Cielo: la pagina come Oggi e il cielo da
esplorare», «Gli step»; tavola 4, «Un oggetto toccato»). Chiesto con la formula della fase:
«Esegui lo step 16.8 della Fase 16 in PLANNING.md, da origin/main aggiornato; alla fine pusha e
apri la PR». Prima dello step, come commit a sé, l'archivio 12: il registro aveva passato le
1 000 righe (regola comune della fase).

### Cosa è cambiato

- **Il tocco** (`ExplorePick`, `pickSpots`). Un tocco sul cielo prende la cosa con un nome più
  vicina entro 24dp, misurati dal bordo di quel che è disegnato: la luna dal suo lembo, un
  pianeta, una delle trentuno stelle con un nome, il radiante, e il nome di una costellazione
  dalla sua scritta. Un dito su Vega è Vega, un dito su «LIRA» lì accanto è la Lira. Una luce
  risponde finché si vede abbastanza da avere un nome (`DomeLook.NamedFrom`); un nome di
  costellazione risponde solo se il frame l'ha scritto (`ExplorePainter.writtenFigures`).
- **La camera** (`SkyCamera.aimed`, `SkyCamera.towards`). Va sull'oggetto con una planata di
  450 ms (subito con il movimento ridotto; un dito sul cielo la ferma dov'è), e si avvicina: a
  35° di campo per una luce, mai più larga dei 60° di riposo per una costellazione, mai più
  lontana di dove il lettore era già. L'oggetto finisce al centro del cielo che il foglio
  lascia: a 0,34 dell'altezza sul telefono, di fianco alla card su una finestra larga.
- **L'anello**: 13dp, l'inchiostro del canvas a 0,85, una riga da 1,5dp e niente di pieno;
  quello della luna sta a 7dp dal lembo. Il nome dell'oggetto è scritto fuori dall'anello. Una
  costellazione non ne ha: il suo nome è il segno.
- **Il foglio** (`ObjectCard`, `ObjectFacts`, `ExploreObjects`), ai piedi della pagina **al
  posto del pannello dell'ora**: il nome; cos'è e dov'è a parole («Pianeta · a sud-ovest, a
  metà cielo»); il cielo dell'ora come chip di verdetto con il suo numero e la sua ora («Bello ·
  nuvole 12% alle 23:00»); le tre caselle «Sorge», «Più alto», «Tramonta» con l'ora e la
  direzione (i numeri del 16.3, `AstronomyEngine.passage`); fino a quando conviene
  (`worthUntil`, sui pavimenti dell'elenco «Da vedere») e cosa si vede a occhio nudo; la
  campanella e «Cos'è, in una pagina». Si chiude con la sua ×, con un tocco sul cielo vuoto e
  con il gesto indietro, che chiude il foglio prima della pagina.
- **Marte e Saturno nel catalogo dei momenti**: `mars.night` e `saturn.night`
  (`SkyJobCatalog.MarsNight`, `SaturnNight`), con il nome, la riga, la pagina nella guida degli
  eventi (IT/EN), l'icona, la direzione e i rimandi fra le pagine. Il catalogo passa da
  sessanta a **sessantadue** momenti (la guida, il README, VISION e i commenti che lo contavano).
- **La finestra delle due righe è quella dell'elenco «Da vedere»** (`WorthSeeing.planetNight`,
  `SkySights.planetNight`): dal sole a −12° (sono stelle fra le stelle), a 10° di altezza, per
  almeno un quarto d'ora, e mai mentre il pianeta è più debole di 1,5. Un motivo nuovo,
  `SkyNotScheduled.PLANET_TOO_FAINT` («Troppo debole in questi mesi: si confonde con le
  stelle»), per i mesi in cui Marte è dall'altra parte del sole.
- **Il verdetto di un'ora** (`SkyVerdictEngine.ofWeather`): le soglie di `evaluate` in un posto
  solo, che `evaluate` ora legge e che il foglio usa per l'ora guardata.
- **La campanella** (`SkyViewModel.remind`, `remindedJobs`): aggiunge il momento ai propri con
  un promemoria che può suonare; spenta, lascia il momento e toglie il promemoria.
- **La pagina della guida** si apre sopra l'esploratore (`SkyExploreRoute.onOpenPage`,
  `SkyEventKey(id, inTab = false)`), e indietro torna al cielo e al foglio da cui è partita.
- **Per chi non può puntare**: la luna, i pianeti e il radiante a schermo sono azioni del cielo
  per un lettore di schermo («Apri: Saturno»), accanto alle quattro che lo girano.
- **Documenti**: DESIGN §8.8d («A thing pointed at»), la guida («Stanotte», IT/EN), il
  CHANGELOG, il README (62), VISION §5.3 (la conta).

### Decisioni e deviazioni

1. **Il foglio sta nella pagina, al posto del pannello, e non è un foglio modale.** La tavola 4
   lo disegna come un foglio sopra il cielo. Un `ModalBottomSheet` è un'altra finestra con il
   suo velo: il cielo sotto non risponde più al dito, e l'anello resta dietro. Qui il cielo
   resta vivo: si trascina, si avvicina, si tocca un'altra cosa e il foglio cambia. Il costo:
   mentre il foglio è aperto l'ora non si muove (il pannello non c'è), e il chip dice di che ora
   è il cielo. Chi vuole un'altra ora chiude il foglio.
2. **Il chip dice «Bello», non «Si vede bene».** Sono le quattro parole dei verdetti dell'app
   («Bello», «Così così», «Niente da fare»), con le stesse soglie delle righe: un oggetto che
   avesse un vocabolario suo direbbe con due parole la stessa cosa. Giudica **l'ora guardata**,
   non una finestra, e porta l'ora nel numero («nuvole 12% alle 23:00»). Dove la previsione non
   ha quell'ora il chip non c'è (lo dice già la riga del pannello: «Disegnato senza nuvole…»).
3. **La campanella dove il catalogo ha una riga, e quale.** Giove, Marte, Saturno: la loro
   notte. Venere ne ha due, e vale il lato della notte in cui il lettore la sta guardando: il
   sole a ovest del meridiano è la sera, a est il mattino. Il radiante: il picco del suo sciame
   («Avvisami la notte del picco»). **La luna non ha campanella**: le sue righe sono il sorgere,
   il tramonto e le fasi, e nessuna è «le sere in cui si vede bene»; ha la pagina («La luna di
   oggi»). Una stella e una costellazione non hanno né l'una né l'altra, come dice lo step.
4. **«Avvisami» promette un suono, quindi lo mette.** Aggiunge il momento con il promemoria
   abituale; dove l'abituale è «mai» gli dà i trenta minuti del catalogo (`SkyLead.Default`),
   perché il pulsante ha promesso un avviso e «mai» non lo è. La campanella è disegnata accesa
   solo se il promemoria c'è davvero (`remindedJobs`): un momento seguito con il promemoria
   spento non è «ti avviso». Spegnerla mette «mai» su quel momento e lo lascia fra i propri: si
   toglie dalla pagina Cielo, dove si tolgono gli altri. Se il telefono non permette le
   notifiche, il tocco le chiede e sotto il pulsante c'è la frase della pagina Cielo
   («…nessuno di loro può arrivare»). **Non coperto**: il canale silenziato dal telefono, che
   la pagina Cielo dice e il foglio no.
5. **Marte e Saturno con la regola dell'elenco, non con quella di Giove.** La riga di Giove
   resta com'è (dal crepuscolo civile, `planetAbove`). Le due nuove leggono la stessa funzione
   dell'elenco «Da vedere», così il promemoria e la riga dell'elenco parlano delle stesse ore,
   e la riga dice **perché** una notte non ha finestra: troppo basso, o troppo debole. Senza
   il secondo motivo, per mesi un lettore che segue Marte avrebbe ricevuto l'invito a uscire
   per un punto che non si distingue dalle stelle intorno.
6. **Né `needsDarkness` né `photographic`** sulle due righe, come su Giove e Venere: Saturno al
   suo minimo è ancora una stella di prima grandezza, e la luna piena non le nasconde.
7. **Una stella non ha una frase sua.** Il foglio di una stella è corto: il nome, «Stella · a
   est, a metà cielo», il chip, le caselle, fino a quando. Trentuno frasi scritte a mano
   sarebbero trentuno affermazioni da verificare, e il catalogo delle stelle porta solo
   posizione, luminosità e colore. La luna, i quattro pianeti e il radiante hanno la loro.
8. **Le caselle che ci sono sono quelle vere.** Una cosa che da qui non tramonta (Capella e la
   Polare a Milano) ha solo «Più alto» e la frase «Non tramonta: da qui resta sopra
   l'orizzonte tutta la notte e tutto il giorno»; mai una casella con un trattino.
9. **Dove e quanto in alto a parole**: la direzione all'ottavo di bussola e l'altezza a terzi
   (`SkyHeight` del 16.4: «in basso», «a metà cielo», «in alto»; «sopra la testa» senza
   direzione). Le forme sono senza genere apposta («sta in basso», non «è basso»): il soggetto
   è la Luna, Marte, una stella.
10. **La planata è l'unico momento in cui la pagina disegna senza il dito**: meno di mezzo
    secondo, una volta per tocco. La regola della batteria tiene (disegna quando la camera
    cambia): è la camera che cambia.
11. **La fascia in alto è cielo.** Le scritte in alto tengono lontani i nomi, ma la riga del
    titolo è larga quanto lo schermo e contiene una × e una frase: un dito sul cielo accanto
    alle parole è un dito sul cielo. Trovato con il telefono di lato, dove Saturno stava lì.
12. **Di notte e a schermo**: una cosa sotto l'orizzonte, o un radiante in una notte che non è
    del suo picco, non ha foglio; se l'orologio la porta sotto mentre il foglio è aperto, il
    foglio si chiude.

### Trovato e non toccato

- **Le stelle e le costellazioni non si aprono senza puntare.** Le azioni per il lettore di
  schermo coprono la luna, i pianeti e il radiante; trentuno stelle e ottantotto nomi in un
  menu di azioni sarebbero un elenco, e l'elenco è il foglio «Da vedere» del 16.9.
- **Con il telefono di lato il foglio scorre**: in 384dp di altezza ne restano 238 e la
  campanella sta sotto la piega. Su un tablet ci sta intero.
- **La riga di Giove e l'elenco «Da vedere» hanno due regole** (decisione 5). Il buio è lo
  stesso (dal crepuscolo civile all'alba civile, Giove è uno dei due «brillanti») e il
  pavimento anche (10°): dove tutte e due hanno una finestra, gli orari coincidono. Differiscono
  in due casi. **Una finestra più corta di un quarto d'ora** (Giove che tocca i 10° per pochi
  minuti al crepuscolo, alla fine di un'apparizione): la riga la dà, con il suo promemoria, e
  l'elenco non ha la riga (regola 8 dell'elenco). **Una notte con due tratti** (basso la sera,
  di nuovo su prima dell'alba): la riga prende il primo (`planetAbove`), l'elenco quello con il
  punto più alto. Portare Giove sulla regola dell'elenco è una riga di codice; toglie il
  promemoria di quelle poche sere e allinea il secondo caso. Non in questo step: è un momento
  che esiste dalla Fase 28. Quante sere l'anno tocca è stato misurato lo stesso giorno, su
  richiesta (registro, «Giove: la riga contro l'elenco, la misura»).

### Come è stato verificato

- **Il motore contro un'effemeride** (`SkyPlanetLinesTest`). Le due finestre della notte del
  10 nov 2026 a Milano contro **JPL Horizons, letto il 12 nov 2026** (tabella dell'osservatore,
  un minuto a riga, altezza senza atmosfera, con il segno del crepuscolo di Horizons): Saturno
  dal crepuscolo nautico (17:06 UTC) a quando scende a 10° (01:45–01:46 UTC), Marte da quando
  sale a 10° (23:43–23:44 UTC) all'alba nautica (05:10 UTC), entro due minuti. La riga è la
  riga dell'elenco, stessa finestra. Tre anni di Milano, una notte a settimana: Marte è su,
  troppo basso o troppo debole, e la riga dice quale; Saturno non è mai troppo debole. Le
  soglie di un'ora sono quelle di una finestra.
- **La scelta al tocco, la camera, i fatti** (`ExploreObjectTest`, `SkyCameraTest`): il più
  vicino entro il raggio e niente oltre; la luna dal lembo, un nome dalla sua scritta; la
  stella contro il nome accanto; una scelta ritrovata dalla sua chiave; i punti toccabili dove
  la camera li mette, e nessuno sotto un cielo chiuso; i fatti di Saturno uguali a quelli del
  motore; la Polare che non tramonta; nessuna campanella e nessuna pagina che non sia una riga
  del catalogo; la mira da qualunque camera, il pavimento tenuto, la planata per la via corta.
- **La pagina** (`SkyExploreScreenTest`, nove casi nuovi, resi dalla GPU): il tocco a 20dp da
  Saturno apre il foglio al posto del pannello, con le ore del motore nelle tre caselle, la
  camera a 35° e il pianeta al centro sopra il foglio, l'anello disegnato; la campanella mette
  e toglie il promemoria su `saturn.night` e il rimando chiede la sua pagina; Capella ha nome,
  posto e passaggio, e né campanella né pagina; il nome di Orione apre la costellazione senza
  avvicinare; il foglio si chiude con la ×, con il cielo vuoto e con indietro (che non chiude
  la pagina); senza previsione non c'è chip; un pianeta si apre senza dito; con il telefono di
  lato il foglio è la card nell'angolo e il pianeta sta di fianco.
- **Guardate**: il foglio di Saturno e quello di Capella sul telefono (384 × 832dp), e Saturno
  con il telefono di lato. La prima resa aveva il nome scritto sopra l'anello: corretto.
- **Tutta la suite e il lint** (`./gradlew test :app:testDebugUnitTest :app:lintDebug`, 20
  minuti): 2 917 test (app debug 1 120, di cui 15 saltati, le immagini del README; app release
  954; domain 484; data 325; sync 34) e **un fallimento, dello step**: `ItalianArticleTest`
  rifiutava «%1$s, illuminata al %2$d%%» nella riga della luna, perché con 80 sarebbe
  «all'80%». Ora è «Gibbosa crescente, 97% illuminata». Dopo la correzione (e dopo una frase
  di Marte riscritta e una lettura di risorsa spostata fuori da un composable) sono stati
  rilanciati i test di domain interi, gli 89 test dell'app che lo step tocca
  (`ItalianArticleTest`, `ExploreObjectTest`, `SkyCameraTest`, `SkyExploreScreenTest`,
  `SkyGuideTest`, `SkyTextTest`, `SkyMomentIconTest`, `SkySearchTest`), `ItalianArticleTest`
  anche nella variante release, e il lint: verdi, 0 errori di lint. **La suite intera dell'app
  non è stata rilanciata dopo la correzione**: la esegue la CI sulla PR.

### Cosa conferma solo il telefono

Casella in «Aperto»: il dito (24dp bastano? troppi tocchi a vuoto, o la cosa sbagliata fra due
vicine?), la planata, il gesto indietro che chiude il foglio, la campanella che chiede il
permesso delle notifiche e il promemoria che arriva, la pagina che si apre e il ritorno al
foglio, il foglio con il carattere grande, la luna toccata.

## Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7 (committente, 12 nov 2026)

Nella stessa chat del 16.8, prima della PR, a suite ancora in corso. Lette le quattro scelte
riassunte a fine step (il foglio nella pagina e non modale, con l'ora ferma mentre è aperto; il
chip con la parola dei verdetti, «Bello», e non «Si vede bene»; la campanella che mette il
promemoria abituale o trenta minuti dove l'abituale è «mai», e la luna con la pagina e senza
campanella; Marte che dice «troppo debole» nei mesi in cui non si distingue dalle stelle), il
committente: «Confermo le tue scelte. Segna anche come già fatti i test su telefono dei passi
precedenti della fase 16 ancora aperti».

- **Le scelte.** Chiuse in «Aperto» le due caselle che le aspettavano («Il foglio dell'oggetto
  al posto del pannello dell'ora», «La campanella del foglio»); il chip e Marte troppo debole
  erano decisioni dello step senza una casella (registro, «16.8», decisioni 2 e 5) e restano
  come sono scritte.
- **Le prove sul telefono.** Segnate come fatte le cinque ancora aperte dei passi prima del
  16.8: le due caselle del 16.4, 16.5, 16.6, 16.7 e la notte bianca. Come il 10 nov 2026
  (archivio 12, «Le prove sul telefono della Fase 16, da 16.1 a 16.3»), è la parola del
  committente e non una prova fatta qui: nessuna resa nuova, nessun test in più.
- **«Costellazioni» accesa finché non la si spegne** (16.7, decisione 10) non era né una delle
  quattro scelte né una prova, ed era rimasta aperta; chiesto, il committente poco dopo: «Sì,
  chiudi anche costellazioni con la tua scelta». Chiusa: parte accesa, com'è nel codice.
- **Resta aperto** della Fase 16: «16.8 sul telefono», che è di questo step, e la riga di
  Giove sulla regola dell'elenco, proposta non decisa. Fuori dalla Fase 16 restano le due
  prove di Oggi (la revisione, l'hero).

## Giove: la riga contro l'elenco, la misura (committente, 12 nov 2026)

Chiesta nella stessa chat, dopo aver riletto la proposta («Rinfrescami sulla proposta di
Giove», poi «Sì, misura»). La descrizione scritta nello step era sbagliata in un punto
(«differiscono di minuti ai bordi») ed è stata corretta nel registro prima della misura: il
buio e il pavimento sono gli stessi, e la differenza è la finestra corta e la notte con due
tratti.

**Come.** Ogni notte dal 1° gennaio 2026 al 31 dicembre 2031 (2 191), in quattro luoghi, la riga
`jupiter.night` (`SkyScheduler.resolve`) contro la riga dell'elenco
(`WorthSeeing.planetNight`). Una prova scritta per la misura e non committata; «stessa
finestra» è entro 90 secondi ai due capi.

| Luogo | Notti con la riga | Notti con l'elenco | Stessa finestra | Solo la riga | Due tratti diversi |
|---|---|---|---|---|---|
| Milano | 1 790 | 1 726 | 1 726 | 64 | 0 |
| Palermo | 1 863 | 1 803 | 1 803 | 60 | 0 |
| Stoccolma | 1 155 | 1 125 | 1 125 | 30 | 0 |
| Sydney | 1 879 | 1 827 | 1 827 | 52 | 0 |

**Cosa dice.**

- **Dove tutte e due hanno una finestra, è la stessa**, in ogni notte dei sei anni e in ogni
  luogo. Nessuna notte ha la riga dell'elenco senza la riga del catalogo.
- **«Solo la riga» sono le finestre sotto il quarto d'ora**, da 0 a 14 minuti, tutte: a Milano
  64 in sei anni, undici l'anno; a Stoccolma cinque. Vengono in due gruppi l'anno: le ultime
  sere di un'apparizione (da quattro a dieci di fila, la finestra che si accorcia di due
  minuti a sera fino a zero, al crepuscolo civile) e le prime mattine della successiva
  (quattro o sei, pochi minuti prima dell'alba civile). A Milano nel 2026: 17–20 giugno la
  sera (13, 10, 6 e 3 minuti), 22–25 agosto la mattina (1, 5, 10 e 14 minuti).
- **La notte con due tratti non è capitata mai**, in nessuno dei quattro luoghi. Resta una
  differenza del codice e non una che un lettore incontra a queste latitudini.

**Cosa cambierebbe.** Con Giove sulla regola dell'elenco chi lo segue non riceve più, una
decina di volte l'anno a Milano, il promemoria di una finestra di qualche minuto in un cielo
ancora chiaro; in quelle sere la riga direbbe «Troppo basso per vederlo, così vicino al
sole». Tutte le altre notti restano identiche. Alla misura non era stato fatto: la decisione
era del committente, che l'ha presa subito dopo (la sezione che segue).

## Giove sulla regola dell'elenco (committente, 12 nov 2026)

Letti i numeri della misura e la raccomandazione di farlo, il committente: «Sì, in questa PR».
Un terzo commit sulla PR #160.

### Cosa è cambiato

- **`jupiter.night` legge `SkySights.planetNight`**, come Marte e Saturno: la riga dell'elenco
  «Da vedere» (`WorthSeeing.planetNight`), dal crepuscolo civile all'alba civile (Giove resta
  uno dei due «brillanti»), a 10°, per almeno un quarto d'ora. Una sera in cui Giove sta sopra
  i 10° per meno di un quarto d'ora non ha più finestra né promemoria: la riga dice «Troppo
  basso per vederlo, così vicino al sole» (`PLANET_TOO_LOW`), il motivo che dà già nelle
  settimane in cui Giove è dietro il sole.
- **`SkySights.planetWindow` perde `wholeNight`**: lo usava solo Giove. Resta la finestra di
  Venere, della sera e del mattino.
- Il CHANGELOG lo dice fra i cambiamenti.

### Decisioni e deviazioni

1. **Nessuna compatibilità da tenere** (la regola della beta): chi segue Giove trova la riga
   nuova dal primo calcolo, senza migrazione; il promemoria già armato per una di quelle sere
   viene ricalcolato al prossimo riarmo, come ogni promemoria.
2. **Venere resta com'è.** Le sue due righe sono finestre di quattro ore dal tramonto o prima
   dell'alba, a 5°, e l'elenco la tratta come Giove: le due regole non sono state confrontate
   notte per notte, e non era nella proposta.
3. **La notte con due tratti** ora prende quello con il punto più alto, come l'elenco. La
   misura non ne ha trovata una in sei anni e in quattro luoghi: è un allineamento del codice.

### Come è stato verificato

- `SkyPlanetLinesTest`: Giove entra nei due test che avevano Marte e Saturno (la riga è la riga
  dell'elenco, stessa finestra; tre anni di Milano, una notte a settimana: su o troppo basso,
  mai troppo debole), e un test nuovo sulle date della misura: a Milano il 16 giugno 2026 la
  riga ha ancora una finestra, di almeno un quarto d'ora e meno di venti minuti; dal 17 al 20
  giugno no, «troppo basso»; così il 20 luglio (dietro il sole) e dal 22 al 25 agosto; dal 26
  agosto la finestra del mattino. E il tratto corto del 18 giugno c'è davvero, dal crepuscolo
  civile, fra otto e dodici minuti: è la durata a tenerlo fuori.
- `SkySightsTest`: il test che chiedeva la finestra di Giove a `planetWindow` la chiede a
  `planetNight`, e la trova sopra il suo pavimento.
- I test di domain interi (485), i test dell'app del Cielo e delle notifiche (322) e quelli di
  sync (34): verdi. La suite intera dell'app e il lint non sono stati rilanciati per questo
  commit, che tocca solo `:core:domain`, il CHANGELOG e questo file: li esegue la CI.

## 16.9 — «Da vedere», e la notte di un evento (committente, 12 nov 2026)

Il nono step della Fase 16 (archivio 11, «Fase 16 — Cielo: la pagina come Oggi e il cielo da
esplorare», «Gli step»; tavola 5, «Da vedere, e in che notte», e la riga di «In arrivo» della
tavola 2). Chiesto con la formula della fase: «Esegui lo step 16.9 della Fase 16 in
PLANNING.md, da origin/main aggiornato; alla fine pusha e apri la PR». Da `origin/main` dopo
l'unione del 16.8 (PR #160, `7fe488d7`); il registro è a 280 righe, niente da archiviare.

### Cosa è cambiato

- **Il foglio «Da vedere»** (`WorthSeeingSheet`, `WorthSeeingList`, in `ExploreSights.kt`),
  aperto da un chip in fondo alla riga in alto dell'esploratore: l'elenco del 16.4
  (`WorthSeeing.of`) per la notte su cui la pagina sta. In cima **la notte**, come chip che
  vanno a capo: «Stanotte», la notte dopo, le notti dei prossimi tre eventi che ne hanno una,
  ognuna con il suo nome e la data («Geminidi · 13 dic»), e il calendario («Un'altra notte»,
  o la data quando la notte è una scelta dal lettore). Sotto, la data della notte per esteso;
  **la nota quando la previsione non ha le nuvole di quella notte** («La previsione non
  arriva a questa notte: il cielo è disegnato senza nuvole», e le altre due: la notte passata,
  nessuna previsione); una riga che dice cos'è l'ora («Ognuna all'ora in cui sta più in alto.
  Tocca una riga e il cielo gira lì»); poi le righe.
- **Le righe sono quelle dell'agenda** (`TimelineRow` in un gruppo): l'ora in cui la cosa sta
  più in alto nella colonna dell'orologio, il disegno, il nome, e sotto due righe: **dove sta
  a quell'ora, a parole** («A sud, a metà cielo»; prima, per la luna la sua fase di quella
  notte e per un ammasso «Ammasso di stelle») e **il tratto in cui conviene** («Dalle 18:03
  alle 02:37»). Le parole del posto sono quelle del foglio dell'oggetto (`ObjectWords.place`).
- **Un tocco su una riga** chiude il foglio, porta la pagina **all'ora della riga** e la
  camera **sull'oggetto**, con la planata del 16.8 e con il foglio dell'oggetto aperto.
- **Due cose che l'elenco conosce e un dito no** (`SkyPick.Shape`, `SkyPick.MilkyWay`): una
  figura dell'elenco (il Grande Carro, il Triangolo estivo, le Pleiadi: stelle, non un nome
  scritto) e il nucleo della Via Lattea. Hanno il foglio dell'oggetto come le altre: il nome,
  cos'è e dov'è («Figura di stelle · a nord-est, a metà cielo», «Ammasso di stelle», «Il
  nucleo luminoso della galassia»), il chip dell'ora, le tre caselle, fino a quando. La
  figura ha **un anello solo intorno a tutte le sue stelle** (`extentRingDp`: dal centro
  alla più lontana, più 14dp d'aria); la Via Lattea non ha anello, ha la campanella e la
  pagina del suo momento (`milky_way.core`) e una frase su cosa si vede.
- **«Guarda il cielo di quella notte»** sotto le righe di «In arrivo» il cui evento ha una
  notte sua (`SkyEventNights`, `EventRow.onNight`): apre l'esploratore su quella notte,
  all'ora dell'evento, girato sull'oggetto con il suo foglio aperto.
- **La navigazione**: `SkyExploreKey` porta la sera in cui la notte comincia
  (`nightEpochDay`) e l'oggetto (`pick`, la chiave di un `SkyPick`); `SkyExploreScreen` li
  riceve come `openDate` e `openPick`, e le notti degli eventi come `eventNights`.
- **Le parole**: i nomi di cinque figure (Grande Carro, Quadrato di Pegaso, Triangolo estivo,
  Pleiadi, Iadi; le altre sei sono costellazioni e hanno il nome del 16.5), IT/EN; la guida
  («Stanotte», due capoversi in coda, IT/EN); DESIGN §8.8d («Da vedere», «From the calendar
  ahead») e §8.8; il CHANGELOG.
- **Lo screenshot del README** `sky-ahead.png`, rigenerato: le righe di «In arrivo» con il
  rimando sotto.

### Decisioni e deviazioni

1. **Il foglio dell'elenco è un foglio vero, nel tema dell'app; quello dell'oggetto resta
   nella pagina.** Sono due cose diverse: l'elenco è una lista da cui scegliere, e mentre è
   aperto il cielo sotto non ha niente da rispondere; il foglio dell'oggetto sta nella pagina
   perché lì il cielo resta in mano al lettore (16.8, decisione 1). Nel tema dell'app come il
   calendario, non in quello della notte: le righe sono quelle dell'agenda, con i loro colori.
2. **La notte del foglio è la notte della pagina.** Scegliere un'altra notte in cima
   all'elenco gira il cielo dietro il foglio. La tavola 5 mette la notte in cima al foglio e
   non dice di chi è: una sola, perché un elenco su una notte che il cielo non mostra
   sarebbero due notti in una pagina, e la riga toccata dovrebbe poi cambiare anche quella.
3. **«Domani» fino a mezzanotte, «La notte dopo» dopo.** Alle due di notte, con stanotte in
   corso, la notte che viene comincia stasera: chiamarla «domani» sarebbe sbagliato di un
   giorno. Sono due stringhe che c'erano già.
4. **Quali eventi hanno una notte sua** (`SkyEventNights`, con il motivo nel KDoc): uno
   sciame, un'eclissi di luna, la luna accanto a un pianeta o due pianeti vicini, una luna
   piena (la riga del calendario, `moon.full`, la più vicina dell'anno, la luna piena al
   crepuscolo). **Fuori**: solstizi, equinozi, perielio e afelio, il tramonto più precoce e
   l'alba più tarda, le notti bianche (sono date), l'eclissi di sole (è del giorno), la luna
   nuova e i quarti (un cielo che non ha niente di suo da mostrare), `moon.phase` (la riga non
   dice quale fase è). Un rimando sotto ogni riga sarebbe la pagina che riempie spazio.
5. **La sera a cui una notte appartiene è la data di dodici ore prima dell'evento**: un
   istante prima di mezzogiorno è della notte cominciata la sera prima. Per uno sciame è la
   sera della sua finestra (il test lo tiene per i tredici della tabella).
6. **L'ora a cui il cielo si apre**: quella dell'evento dove l'ora è lo spettacolo (la metà di
   un'eclissi, l'istante di una congiunzione, la luna piena che sorge al crepuscolo);
   altrimenti quella che l'elenco ha per l'oggetto in quella notte, quando sta più in alto
   (uno sciame è una notte intera, l'istante di una luna piena è geometria). Per le Geminidi
   del 2026 è verso le due del 14 dicembre, con il radiante oltre i 70°.
7. **Di una coppia si cerchia il pianeta**, non la luna: dove sta lui, la luna è lì accanto e
   non ha bisogno di essere trovata. Venere e Giove: Venere.
8. **Uno sciame debole che nessuno segue è disegnato per chi arriva dalla sua riga.** La
   regola dell'hero è «uno sciame debole solo per chi lo segue», e «In arrivo» porta anche le
   Ursidi di tutti: chi tocca il rimando sotto quella riga le ha chieste per nome, e in quella
   pagina il radiante c'è, cerchiato e in elenco. Senza, il rimando avrebbe aperto un cielo
   girato verso un punto senza niente.
9. **Una riga apre anche il foglio dell'oggetto.** Lo step dice «un tocco porta la camera
   sull'oggetto alla sua ora migliore»; il foglio si apre come con un dito, perché una camera
   girata verso un punto senza nome lascia il lettore a chiedersi quale sia. L'ora resta
   tenuta quando il foglio si chiude, e «Adesso» riporta al cielo di fuori.
10. **Le figure e la Via Lattea non rispondono al dito**, solo all'elenco: non sono una luce
    né una scritta, e un bersaglio grande come Orione sotto ogni tocco avrebbe rubato le
    stelle che contiene. Il loro «fino a quando» è la fine del tratto della riga, non un
    calcolo sul centro: una figura conviene finché ogni sua stella è sopra i tetti, la Via
    Lattea finché la luna è giù. Così il foglio e la riga dicono la stessa ora.
11. **Le figure non hanno una frase su cosa si vede**, come le stelle (16.8, decisione 7):
    undici frasi scritte a mano sarebbero undici affermazioni da verificare. La Via Lattea
    ne ha una, e dice che vuole un cielo buio lontano dalle luci: è l'unico posto, con la
    guida, dove l'app lo dice.
12. **Nessun verdetto sulla riga dell'elenco.** L'elenco è quel che c'è lassù dietro le
    nuvole (16.4); il verdetto dell'ora è sul foglio dell'oggetto, un tocco dopo. Dodici chip
    «Bello» uno sotto l'altro avrebbero detto dodici volte la stessa notte.
13. **I disegni delle righe sono quelli che ci sono**: la fase della luna di quella notte, la
    stella dei pianeti, la stella cadente degli sciami, le tre stelle del buio per le figure e
    la Via Lattea. Nessun disegno nuovo.
14. **I nomi delle sei figure che sono costellazioni sono quelli del 16.5** (in italiano
    «Gemelli», «Leone»; in inglese i nomi latini, «Gemini», «Leo»); in inglese il Grande Carro
    è «Big Dipper».
15. **Tre notti di eventi al massimo fra i chip**, le più vicine, senza quelle che sono già
    stanotte o la notte dopo: di più sarebbero quattro righe di chip sopra l'elenco.
16. **La pagina aperta su un oggetto è già lì**, senza planata: la planata serve a seguire
    dove è andato il cielo, e chi arriva da «In arrivo» non l'ha visto prima.
17. **Il rimando non c'è oltre il calendario dell'esploratore** (cinque anni): la ricerca di
    un'eclissi ne guarda sei.
18. **VISION §5.3 e il testo del README restano al 16.11**, che li rilegge per intero. Del
    README cambia solo il testo alternativo di `sky-ahead.png`, che descriveva una riga (la
    porta della guida) che nell'immagine non c'è più.
19. **Il rimando sta sotto il nome, vicino alle sue parole.** La prima resa lo metteva
    all'inizio della riga, sotto l'icona, con 37dp di vuoto sopra (l'aria della riga più
    quella del pulsante): quattro righe su uno schermo dove ne stavano sei, e il rimando
    sembrava di nessuno. Ora parte dalla colonna del nome ed è sollevato dei 12dp del piede
    della riga. La riga cresce comunque di 36dp: è il costo di un rimando che la tavola 2
    vuole sotto ogni evento con una notte, e nel calendario di tutti quasi ogni riga è uno
    sciame.
20. **Le righe dell'elenco su due righe**: dove, poi quando. Su una sola, un telefono
    spezzava «dalle 18:03 | alle 02:37» a metà della coppia (visto alla prima resa).

### Trovato e non toccato

- **«Luna nuova, 3% illuminata» in elenco.** L'elenco ha la luna dall'1% illuminato (16.4,
  regola 3) e il nome della fase è quello che l'app dà a tutto il giorno: la falce sottile
  all'alba, due giorni prima della luna nuova, esce come «Luna · Luna nuova, 3% illuminata».
  È vero e suona storto. Proposta, non decisa: la riga della luna dice la fase solo dal 5%
  in su, o la soglia dell'elenco sale.
- **Un'ora fuori dal nastro.** La luna, Venere e Giove hanno il loro buio dal crepuscolo
  civile, e la loro ora migliore può cadere prima che la notte astronomica cominci: chiuso il
  foglio dell'oggetto, il pannello scrive quell'ora e il pallino del nastro sta fermo al suo
  inizio. L'ora scritta è vera; il nastro è la notte astronomica e quell'ora ne è fuori.
- **La riga apre sull'ora migliore anche se è già passata** (stanotte alle tre, Saturno alle
  21:31): il cielo è quello delle 21:31, senza le nuvole perché la previsione non tiene le
  ore passate, e la pagina lo dice («Disegnato senza nuvole»).

### Come è stato verificato

- **La notte di un evento** (`SkyEventNightsTest`, sette casi, sul motore e senza schermo): le
  Geminidi del 2026 da Milano hanno la notte del 13 dicembre, l'ora che l'elenco dà al
  radiante (fra l'una e le tre, oltre i 70°, dentro la finestra dello sciame) e il radiante
  come oggetto; le Ursidi, deboli, hanno la loro ora anche per chi non le segue; una luna piena
  alle 03:14 è della notte prima e una alle 15:53 di quella sera, e si apre sulla luna quando
  è più alta; un'eclissi di luna alla sua metà, una congiunzione al suo istante sul pianeta;
  dieci righe senza notte (solstizio, equinozio, perielio, tramonto più precoce, alba più
  tarda, notti bianche, eclissi di sole, luna nuova, primo quarto, `moon.phase`) e una riga
  senza data non hanno rimando; ognuno dei tredici sciami ha la notte della sera in cui la
  sua finestra si apre.
- **Le scelte dell'elenco** (`ExploreObjectTest`, quattro casi nuovi): ogni riga di tre notti
  (11 novembre, 12 luglio, 13 dicembre, che insieme hanno i cinque tipi) è una scelta che si
  ritrova dalla sua chiave e gira la camera verso quel che l'elenco ha misurato; il foglio di
  Orione sta sul suo centro, tiene le sue sette stelle dove il motore le mette e finisce
  quando finisce la riga; quello della Via Lattea ha la campanella e la pagina di
  `milky_way.core`; l'anello arriva alla stella più lontana più 14dp, e le Pleiadi, che nel
  catalogo sono una stella, tengono l'anello di una luce.
- **La pagina** (`SkyExploreScreenTest`, sette casi nuovi, resi dalla GPU): il chip apre
  l'elenco della notte con una riga per ogni cosa del motore, l'ora, il posto e il tratto; la
  riga di Saturno chiude il foglio, porta la pagina alle 21:35, apre il foglio dell'oggetto
  con il chip di quell'ora e mette il pianeta al centro sopra il foglio a 35° di campo, e
  chiuso il foglio l'ora resta; la riga di Orione apre la figura senza campanella né pagina,
  a 60°, con ogni stella dentro l'anello; i chip cambiano la notte della pagina («Domani», la
  notte delle Geminidi con la nota sulla previsione, di nuovo «Stanotte», il calendario);
  aperta sulla notte delle Geminidi la pagina è su quella notte, sul radiante, con la
  campanella del picco e senza verdetto; uno sciame debole non seguito è disegnato e in
  elenco per chi arriva dalla sua riga; sotto il sole di mezzanotte l'elenco dice che non ha
  niente.
- **Il rimando** (`SkyEventNightLinkTest`, sul Milano registrato): sta sotto le righe che
  hanno una notte e sotto nessun'altra, e chiede l'esploratore sulla notte di quell'evento
  con il suo oggetto. `ChiaroNavigationStateTest`: la chiave con la notte e l'oggetto passa
  dallo stato salvato.
- **Guardate**: l'elenco in italiano nel tema chiaro (stanotte) e in inglese nel tema scuro
  (la notte delle Geminidi, con la nota), una notte di luglio con la Via Lattea; il foglio di
  Saturno dopo la riga; Orione con il suo anello; la pagina aperta sulle Geminidi; lo
  screenshot `sky-ahead.png` prima e dopo aver stretto il rimando. Da queste: le due righe
  sotto il nome, il rimando sotto il nome, la riga della luna nuova annotata sopra.
- **Tutta la suite e il lint** (`./gradlew test :app:testDebugUnitTest :app:lintDebug`, 22
  minuti e 39 secondi): app debug 1 140 test (15 saltati, le immagini del README), app
  release 965, il lint con 0 errori. **Due fallimenti**: il test della figura, che toccava
  una riga sotto la piega del foglio a riposo (il clic cadeva fuori dalla finestra; ora usa
  l'azione della riga, e il codice dell'app non è cambiato), e `AlertsViewModelTest`, il
  timeout di DataStore sotto carico già visto al 16.2, verde da solo. Dopo la correzione
  sono stati rilanciati `SkyExploreScreenTest` (34), `ExploreObjectTest` (19),
  `SkyEventNightsTest` (7), `SkyEventNightLinkTest` (2), `SkyMomentsRowsTest` (7),
  `SkyGuideTest` (10) e `AlertsViewModelTest` (9): verdi. **La suite intera non è stata
  rilanciata dopo quella correzione**: la esegue la CI sulla PR.

### Cosa conferma solo il telefono

Casella in «Aperto»: il foglio sopra il cielo (aprirlo, trascinarlo fino in cima, chiuderlo
con il gesto), i chip della notte con il carattere grande, il calendario aperto sopra il
foglio, la planata dopo una riga e l'ora che cambia insieme, l'anello intorno a una figura
quando si avvicina e si allontana, il rimando sotto le righe di «In arrivo» sotto il dito
(due bersagli in una riga: la riga apre la pagina, il rimando il cielo), la pagina che si
apre sulla notte di un evento e il ritorno indietro.

## Le scelte del 16.9 confermate, e i due «trovati» corretti (committente, 12 nov 2026)

Nella stessa chat del 16.9, a PR #161 aperta. Lette le sette scelte riassunte a fine step (il
foglio dell'elenco modale e nel tema dell'app; la notte del foglio che è la notte della
pagina; la riga che apre anche il foglio dell'oggetto; le figure e la Via Lattea con il foglio
dell'oggetto, raggiunte solo dall'elenco, e l'anello intorno alle stelle di una figura;
nessun verdetto sulle righe dell'elenco; il rimando di «In arrivo» sotto il nome, con la riga
36dp più alta; lo sciame debole disegnato per chi arriva dalla sua riga), il committente:
«Confermo le tue scelte. Riguardo alla sezione "Trovato e non toccato" sono interventi da
fare? Se sì li puoi fare qui in questa PR?». Risposto: due sì e uno no. Un secondo commit
sulla PR #161.

### Cosa è cambiato

- **Una luna che la fase chiama nuova e che si vede è una falce sottile**
  (`ObjectWords.moonKind`, `isThinCrescent`, `moonDrawn`). Il nome della fase è del giorno, e
  «Luna nuova» tiene un giorno e tre quarti da ogni parte dell'istante (`MoonPhase.at`:
  ±22,5° di elongazione); dentro quel tratto la falce nel crepuscolo o all'alba è una cosa che
  si vede, e l'elenco la porta dall'1% illuminato (16.4, regola 3). Da quell'1% la riga
  dell'elenco **e il foglio dell'oggetto della luna** (16.8, che aveva le stesse parole) dicono
  «Falce sottile, 3% illuminata», e la riga disegna la falce del suo lato, crescente dopo la
  luna nuova e calante prima (dall'elongazione all'ora della riga), non il cerchio vuoto della
  luna nuova. Sotto l'1% resta «Luna nuova».
- **Il nastro non ha pallino quando l'ora non è sua** (`ribbonHolds`, in `NightRibbon`). Una
  riga di «Da vedere», o la notte di una congiunzione, può portare la pagina a un'ora fuori
  dalla notte astronomica (Venere al crepuscolo civile): il pallino stava fermo al capo più
  vicino del nastro, su un'ora che la pagina non mostrava. Ora il nastro non lo disegna;
  l'ora è scritta sopra, in grande, e l'anello di «adesso» resta dov'è. Vale per il nastro
  dell'hero come per quello dell'esploratore, ma nell'hero l'ora è sempre dentro la notte.

### Decisioni e deviazioni

1. **La proposta scritta nel 16.9 non è quella fatta.** Lì era «la riga dice la fase solo dal
   5% in su, o la soglia dell'elenco sale». Nessuna delle due: togliere la fase lasciava la
   riga senza dire cos'è, e alzare la soglia toglieva dall'elenco proprio la falce sottile,
   che è una delle cose più belle da andare a cercare. Il difetto era il nome, e il nome è
   stato corretto dove si legge.
2. **`MoonPhase.at` non cambia.** La riga «La luna di oggi», l'hero e i widget chiamano il
   giorno «Luna nuova» per quel tratto, ed è il nome che una persona dà alla luna di quei
   giorni; lì la frase è sul giorno, non su una cosa da guardare a un'ora.
3. **Nessuna parola nuova sul nastro** («prima del buio»): l'ora in grande sopra il nastro e i
   due orari sotto (18:39, 05:35) dicono già che le 17:45 vengono prima.
4. **La riga che apre su un'ora già passata resta com'è.** Il cielo è quello di quell'ora, e
   la pagina dice che è disegnato senza nuvole e che c'è «Adesso» per tornare.
5. **Al secondo, non all'istante.** La pagina tiene l'ora in secondi interi: il primo istante
   della notte, tenuto, tornava una frazione di secondo prima dell'inizio e il pallino
   spariva proprio al capo del nastro. Trovato dal test, prima del commit.

### Come è stato verificato

- `SkyExploreScreenTest` (due casi nuovi, 36 in tutto): la luna del 13 luglio 2026 all'alba,
  che il motore dà come `NEW_MOON` al 3%, è in elenco come «Thin crescent, 3% lit», disegnata
  calante, e una luna allo 0% resta «New moon»; alle 17:45 dell'11 novembre (la notte comincia
  alle 18:39) il nastro reso dalla GPU non ha niente di chiaro al suo inizio, e con la prima
  ora della notte tenuta sì.
- `ExploreObjectTest` (due casi nuovi, 21): la sera dopo la luna nuova del 9 novembre 2026 la
  falce è disegnata crescente e la mattina prima calante; il nastro tiene i suoi due capi, non
  un secondo oltre, e il primo istante tenuto in secondi interi.
- Rilanciati verdi anche `TonightNightTest` (21), `TonightStageTest` (9), `SkyHeroPageTest`
  (3), `ExploreTimeTest` (12), `ItalianArticleTest` (3): il nastro è quello dell'hero.
- **Guardata** la pagina alle 17:45: il cielo del crepuscolo, l'ora, il nastro senza pallino.
- **Gli screenshot del README** rigenerati per controllo: nessuno cambia (il nastro di
  `sky-tonight.png` ha il suo pallino). Il lint: 0 errori.
- **La suite intera non è stata rilanciata** per questo commit: la esegue la CI sulla PR.

## 16.10 — «Punta il telefono» (committente, 12 nov 2026)

Il decimo step della Fase 16 (archivio 11, «Fase 16 — Cielo: la pagina come Oggi e il cielo da
esplorare», «Gli step»; tavola 3, il terzo interruttore). Chiesto con la formula della fase:
«Esegui lo step 16.10 della Fase 16 in PLANNING.md, da origin/main aggiornato», e con una
domanda in più sul default di «Dietro le nuvole», a cui risponde l'ultima parte di questa
sezione. Il registro era a 780 righe: nessun archivio prima dello step.

### Cosa è cambiato

- **La bussola dà anche l'altezza** (`CompassMath.aim`, `Aim`, `CompassState.altitude`).
  `CompassMath.heading` dava la direzione di un telefono in piano o dritto; `aim` dà dove punta
  il dorso a qualunque inclinazione: **l'altezza** è la componente verso l'alto del dorso, letta
  come angolo, esatta sempre; **la direzione** è quella del dorso fino a 60° di altezza (un
  telefono inclinato di lato lì non cambia niente), e da 60° allo zenit scivola su quella del
  bordo alto dello schermo, girata: dritto in alto il dorso non ha una direzione (un grado di
  tremito la farebbe girare per tutta la bussola), e lì il cielo sta sullo schermo come lo
  schermo sta sotto il cielo. Sotto l'orizzonte lo stesso, allo specchio: un telefono in piano
  punta dove punta il suo bordo alto, come lo legge la bussola di «Dove guardare». Oltre lo
  zenit le due direzioni si oppongono e per un grado circa non ce n'è una: la lettura non
  arriva e il cielo resta dov'era.
- **Un cielo che non balla** (`SteadyAim`). Due cose sulle letture, una cinquantina al secondo:
  un passa-basso il cui peso cresce con quanto il telefono si è mosso (un venticinquesimo per
  lettura entro mezzo grado da dove sta il cielo, cioè mezzo secondo di costante di tempo; un
  terzo da cinque gradi in su, un sedicesimo di secondo; in proporzione fra i due), e un passo
  di un ventesimo di grado sotto il quale non si dice niente (un pixel al campo di riposo). Un
  peso fisso avrebbe dovuto scegliere fra un cielo che trema e uno che nuota dietro al
  telefono.
- **`rememberCompass(coordinates, enabled, aim)`**. `enabled = false` è una bussola che nessuno
  legge: niente è registrato, e lo stato dice comunque se il telefono ne ha una. `aim = true` è
  la bussola dell'esploratore: `heading` è la direzione del dorso e `altitude` la sua altezza,
  già ferme. Al rilascio (pausa, interruttore spento, pagina uscita) la lettura si azzera: una
  direzione di prima non è dove punta il telefono adesso.
- **L'interruttore** (`SkyExploreScreen`, parametro `phone`). Il terzo chip in alto, «Punta il
  telefono» / «Point the phone», **solo su un telefono che ha una bussola**. Acceso, la camera
  è del telefono: guarda dove punta il dorso e alla sua altezza, il campo resta quello del
  lettore, e la pagina disegna quando il telefono si muove. La prima lettura non butta il
  cielo: la camera ci va con la planata di 450 ms di un oggetto toccato (subito con il
  movimento ridotto), poi è la lettura. Sotto il pavimento della camera il telefono punta a
  terra e il cielo si ferma sul suo orizzonte.
- **Il dito.** Un dito che trascina il cielo se lo riprende e l'interruttore si spegne
  (`byHand`), come le quattro azioni del cielo per un lettore di schermo. Due dita che
  pizzicano e i due pulsanti cambiano quanto è vicino e lasciano la direzione al telefono. Un
  tocco su un oggetto apre il suo foglio e la camera resta del telefono, né più vicina né
  girata; una riga di «Da vedere» ha chiesto di essere portata lì, e l'interruttore si spegne.
- **Il riquadro della bussola** (`CompassNote`). Mentre il cielo è appeso alla bussola e
  quella dice di sé che non è affidabile, o che il campo non è quello della terra, un riquadro
  sotto gli interruttori lo scrive con le frasi che c'erano già («La bussola va calibrata…»,
  «Qualcosa qui vicino disturba la bussola…») e sparisce quando sparisce il motivo. I nomi
  delle stelle non gli passano sotto.
- **Lo schermo resta acceso** mentre l'interruttore è acceso (`keepScreenOn`), e solo allora.
- **Documenti**: DESIGN §8.8d («Punta il telefono», e gli interruttori in alto), la guida
  («Stanotte», IT/EN), il CHANGELOG. VISION §5.3 e il testo del README restano al 16.11, come
  per il 16.9.

### Decisioni e deviazioni

1. **Un modo della pagina, non una preferenza.** «Costellazioni» e «Dietro le nuvole» sono
   ricordate; questo no: è spento ogni volta che la pagina si apre, e tenuto solo attraverso
   una rotazione. Ricordato, la pagina si aprirebbe con il cielo dove capita che punti il
   telefono (in mano, quasi in piano: il pavimento) e con i sensori accesi senza un tocco. Lo
   step dice «sensori accesi solo con l'interruttore», e un interruttore acceso da una memoria
   non è un tocco.
2. **«Il dito lo spegne» è il dito che trascina.** Lo step dice «il dito lo spegne». Trascinare
   è dire dove guardare, e toglie il cielo al telefono. Pizzicare è dire quanto vicino, e non
   lo toglie: chi punta il telefono verso una luce e vuole vederla più grande non ha chiesto di
   smettere di puntare. Lo zoom allora è intorno al centro dello schermo, che è dove punta il
   telefono, e non fra le dita. Da confermare sul telefono.
3. **Un tocco su un oggetto non spegne.** Puntare e chiedere «cos'è quella» è l'uso: il foglio
   si apre, l'anello resta intorno all'oggetto mentre il telefono si muove, e la camera non
   plana né si avvicina (lo farebbe contro il telefono). Una riga di «Da vedere» è un'altra
   domanda («portami lì, alla sua ora»), e lì il telefono lascia il cielo.
4. **Allo zenit la direzione è quella dello schermo.** Una camera senza rollio (16.6) ha
   bisogno di una direzione anche guardando dritto in alto: è quella che decide da che parte
   sta ogni orizzonte sullo schermo. Presa dal bordo alto dello schermo, il cielo sopra la
   testa è disegnato per come il telefono è girato, ed è l'unica lettura che non gira su se
   stessa a ogni tremito. Fra 60° e lo zenit, con il telefono inclinato di lato, il centro
   dello schermo è fuori di poco da dove punta il dorso (al più la distanza dallo zenit per il
   seno di metà dell'inclinazione): il prezzo di un cielo dritto.
5. **Sotto il pavimento il cielo non segue il telefono verso terra.** La camera non scende
   sotto l'altezza che tiene l'orizzonte sopra il pannello (16.6, 16.7): con il telefono
   puntato sull'orizzonte il centro dello schermo sta più in alto di dove punta il dorso (circa
   venticinque gradi sul telefono in verticale al campo di riposo), e quel che è basso
   sull'orizzonte è a schermo, sotto il centro, sopra la riga con le direzioni. Tenere il
   centro esatto voleva dire riempire mezzo schermo di terra dietro al pannello.
6. **Lo schermo acceso non era nello step.** Chi tiene il telefono verso il cielo guarda e non
   tocca, e a trenta secondi lo schermo si spegnerebbe. Vale solo con l'interruttore acceso.
   Da confermare.
7. **Il nord vero è quello del luogo della pagina**, come nella bussola di «Dove guardare»
   (`GeomagneticField` sulle coordinate del luogo), non di dove sta il telefono: l'app non sa
   dov'è il telefono se il luogo non è «qui». Chi esplora il cielo di un'altra città puntando
   il telefono vede quel cielo, girato con la declinazione di quella città. Non è detto a
   schermo.
8. **Il riquadro non ha il disegno dell'otto.** La pagina della bussola lo anima; l'esploratore
   disegna quando la camera cambia e basta, e un'animazione continua sopra il cielo sarebbe la
   prima. Le parole dicono il gesto. Il «disturbata» ha la precedenza sul «da calibrare»,
   perché calibrare non lo risolve.
9. **Sotto un foglio i sensori riposano.** Con l'elenco «Da vedere» o il calendario aperti il
   cielo non è quello che il lettore guarda: la bussola non è letta, e riparte quando il
   foglio si chiude.
10. **La lettura azzerata al rilascio vale anche per la pagina della bussola**: tornando
    dall'app in secondo piano riparte da «Cerco il nord…» per il tempo della prima lettura,
    invece di mostrare la direzione di prima della pausa.
11. **I numeri del filtro sono aritmetica, non ancora un telefono.** Scelti perché a 50 letture
    al secondo un braccio a 60° al secondo resti a meno di sei gradi dal telefono e una mano
    ferma non muova il cielo più di qualche volta al secondo; i test li tengono lì. Se sul
    telefono il cielo balla o resta indietro, sono cinque costanti in `SteadyAim`.

### Come è stato verificato

- `CompassMathTest` (15 casi nuovi, 24): l'aiuto che costruisce le matrici rifà le due di cui
  il file si fidava già; un telefono dritto punta l'orizzonte che ha davanti; inclinato
  all'indietro l'altezza sale di quanto è inclinato e la direzione non cambia, fino sopra la
  testa; inclinato di lato fino a 60° punta dove punta il dorso; dritto in alto la direzione è
  quella dello schermo, anche con lo schermo ruotato; un grado di qua e di là dallo zenit non
  la fa girare; molto oltre lo zenit è il cielo alle spalle; in piano punta a terra, dove punta
  il bordo alto; dove le due direzioni si annullano non ce n'è una. Il filtro: la prima
  lettura è il cielo; dieci secondi di una mano che trema di un terzo di grado tengono il
  cielo entro due decimi e lo muovono meno di cinquanta volte (le letture grezze cinquecento);
  un telefono sul tavolo non muove niente; un braccio a 60° al secondo è seguito a meno di sei
  gradi e due secondi dopo il cielo è arrivato; attraverso il nord si passa dal nord.
- `CompassSensorTest` (nuovo, 4 casi, sui sensori di Robolectric): senza magnetometro non c'è
  bussola; **registrata solo mentre qualcuno la legge e l'app è davanti** (nessun ascoltatore da
  spenta, uno da accesa, nessuno con l'app in secondo piano, di nuovo uno al ritorno, nessuno
  a interruttore spento); un evento del vettore di rotazione di un telefono inclinato di 30°
  verso nord dà nord e 30°, e al rilascio la lettura si azzera; senza `aim` è la bussola di
  prima, una direzione e nessuna altezza.
- `SkyExploreScreenTest` (11 casi nuovi, 47): senza bussola non c'è l'interruttore; è spento
  all'apertura e la bussola è letta solo da acceso; acceso, il cielo è dove punta il telefono
  e va dove va, fino a «dritto in alto»; puntato a terra il cielo resta sul suo orizzonte; **un
  telefono fermo per mezzo minuto non chiede un disegno, e un grado di giro ne chiede uno**;
  un dito che trascina lo spegne e le letture dopo non muovono più niente; le azioni del cielo
  lo spengono; i due pulsanti e un pizzico avvicinano e lasciano direzione e interruttore; un
  tocco su Saturno apre il foglio con il campo e la direzione del telefono, e il telefono
  continua a muovere il cielo; sotto l'elenco la bussola non è letta, e una riga porta la
  camera sull'oggetto con l'interruttore spento; il riquadro c'è solo da acceso, con la frase
  giusta, e va via.
- **Guardate** due rese: il cielo puntato allo zenit con i tre chip (il terzo va a capo a 384dp,
  in inglese e in italiano), e il riquadro «La bussola va calibrata» sotto gli interruttori,
  con i nomi delle stelle che gli stanno fuori.
- **Gli screenshot del README**: nessuno cambia (l'esploratore non ne ha ancora uno, 16.11; e
  l'interruttore non si disegna senza una bussola).
- **La suite intera e il lint** girano una volta sola, sull'albero dei due commit di questa PR
  (lo step, e «Dietro le nuvole» qui sotto): i numeri sono nella sezione che segue, «Come è
  stato verificato». Per questo commit da solo sono girate le tre classi qui sopra, 75 casi.

### Cosa conferma solo il telefono: se si tiene

Lo step lo dice: **si prova sul telefono prima di tenerlo**. Niente di quel che segue è stato
visto, perché qui non c'è né un telefono né un emulatore con una bussola: la casella «16.10
sul telefono» in «Aperto» è la decisione. Da guardare, in ordine: il cielo fermo con il
telefono tenuto fermo (e il passo di un ventesimo di grado che non si vede come uno scatto);
il cielo che sta dietro a un braccio che gira e arriva quando si ferma; il nord dov'è davvero
(una stella o la luna dietro al telefono, al suo posto sullo schermo); il passaggio fra 60° e
lo zenit, con il telefono dritto e con il telefono un po' storto; il telefono di lato; il
pizzico mentre punta; il tocco su un oggetto mentre punta; il telefono puntato basso
sull'orizzonte, dove il cielo si ferma (decisione 5); lo schermo che non si spegne; il
riquadro vicino a una ringhiera o a una cover con la calamita. **Se la bussola non regge un
cielo che non balla, l'interruttore esce** e qui resta scritto perché: il codice che va via
con lui è `SteadyAim`, `CompassMath.aim`, il parametro `aim` e il terzo chip; `enabled` resta
comunque.

### Il parere chiesto su «Dietro le nuvole»

La domanda del committente, con lo step: il default di «Dietro le nuvole» va bene così ed è
corretto, o meglio un default nuovo e non tenere salvata l'ultima scelta?

**Com'è oggi.** Una preferenza sola (`AppSettings.skyBareSky`, chiave `sky_bare_sky`), spenta
di default, salvata fra un avvio e l'altro, e **la stessa** per l'hero del Cielo e per
l'esploratore: accesa in uno, è accesa nell'altro. Il committente la chiese «persistente» il
22 ott 2026, per la card Stanotte (archivio 06, «Cielo, la card Stanotte dal telefono»). Il
widget ha la sua, per card, ed è un'altra cosa.

**Il parere.** Il default spento è giusto e non va cambiato: l'immagine dell'hero è la
previsione, e un cielo stellato sopra «nuvole 90%» è onesto solo perché l'ha chiesto il
lettore un momento prima. È la memoria che non regge più, per due cose cambiate da allora:
la card è diventata l'hero della pagina, con il verdetto come cifra (16.1), e l'esploratore
legge la stessa preferenza (16.7). Chi la accende nell'esploratore, dove è la cosa naturale
da fare in una notte coperta, da quel momento apre la scheda Cielo ogni sera su un cielo
stellato sotto un verdetto «Niente da fare», finché non nota il chip: è il lettore che se ne
dimentica, il rumore già scritto il 20 ott 2026 (archivio 06, «La finestra: il meteo dell'ora,
e dietro le nuvole»), e proprio nelle sere in cui l'immagine conta. Il chip acceso lo dice, ma
è piccolo, e la cifra accanto dice il contrario. «Dietro le nuvole» è uno sguardo, come l'ora
tenuta e il giro («vivono quanto la pagina», stessa sezione del 22 ott), non un'impostazione;
«Costellazioni» invece è un modo di disegnare la carta e non dice niente del meteo: resta
ricordata.

**La proposta, non fatta**: spenta ogni volta, non salvata. L'hero la tiene finché la pagina
vive; l'esploratore si apre com'era l'hero in quel momento (come già fa con l'ora e la
direzione) e poi ha la sua, che non torna indietro. `skyBareSky` e la sua chiave escono dalle
impostazioni; il widget non cambia. Il costo: chi vive una settimana di cielo coperto e apre
l'esploratore ogni sera fa un tocco ogni volta. Rovescia una richiesta del committente («anche
qui, persistente»), quindi è una casella in «Aperto» e non un cambiamento di questo step.
