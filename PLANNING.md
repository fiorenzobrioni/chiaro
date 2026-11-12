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
- **Chiusa il 12 nov 2026**: **Fase 16**, la schermata Cielo come Oggi e il cielo da esplorare (dal
  10 nov 2026): undici step, uno per chat (16.1–16.2 la pagina, 16.3–16.5 il motore e il dato,
  16.6–16.10 l'esploratore, 16.11 la chiusura con la rilettura; la coda che la rilettura aveva
  lasciato e i gradi a schermo sono chiusi lo stesso giorno, registro, «La coda della Fase 16,
  i gradi, e le prove del 16.11»). La revisione, le tavole approvate, le decisioni, le regole comuni e la
  descrizione di ogni step sono in `docs/planning/11-novembre-2-10.md`, «Fase 16 — Cielo: la pagina
  come Oggi e il cielo da esplorare», con i primi due step chiusi (16.1, 16.2); gli step dal 16.3
  al 16.7 sono in `docs/planning/12-novembre-10-11.md`, quelli dal 16.8 al 16.10 in
  `docs/planning/13-novembre-12.md`; la chiusura (16.11) è nel registro qui sotto.
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
- [x] **L'hero dopo la revisione, sul telefono** (archivio 11, «L'hero dopo la revisione: quanto costa, le
  icone ferme sotto il dito, il cielo con un interruttore suo, il luogo che tornava indietro», 10 nov
  2026). Quel che i test non dicono: le icone ferme mentre si trascina il nastro e lo sfarfallio
  che se ne va; «Cielo animato» spento, su Oggi e sulla card Stanotte; il luogo cambiato da Cielo e
  da Avvisi che su Oggi è quello nuovo dal primo frame, senza viaggio. Fatto: provato dal
  committente sul telefono il 12 nov 2026 (registro, «Le prove sul telefono dell'hero e di
  Avvisi»).
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
  promemoria all'imbrunire, la notte al bordo della previsione che dice «presto per dirlo». Fatto: dato per provato dal committente il 12 nov 2026 (archivio 13, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
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
  delle costellazioni» in Impostazioni → Dati e riconoscimenti, che apre la pagina della fonte. Fatto: dato per provato dal committente il 12 nov 2026 (archivio 13, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **16.6** L'esploratore: la camera libera e il cielo a tutto schermo (Fase 16). Chiuso l'11 nov 2026 (archivio 12, «16.6 — L'esploratore: la camera libera e il cielo a tutto schermo»).
- [x] **16.6 sul telefono** (archivio 12, «16.6», «Cosa conferma solo il telefono»): il dito sul cielo
  (trascinare, pizzicare, il giro vicino allo zenit), la fluidità con le stelle a schermo, la Via
  Lattea disegnata dalla GPU del telefono, la pillola «Esplora» con il carattere grande, le icone
  chiare della barra di stato, il gesto indietro. Fatto: dato per provato dal committente il 12 nov 2026 (archivio 13, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **16.7** L'esploratore: l'ora, la notte, le nuvole, le costellazioni (Fase 16). Chiuso l'11 nov 2026 (archivio 12, «16.7 — L'esploratore: l'ora, la notte, le nuvole, le costellazioni»).
- [x] **16.7 sul telefono** (archivio 12, «16.7», «Cosa conferma solo il telefono»): il dito sul nastro
  dentro l'esploratore, le linee delle costellazioni su uno schermo vero, i nomi in maiuscolo con
  il carattere grande, il calendario, il velo delle nuvole in una notte che le ha, «Adesso» che
  avanza da solo, il telefono di lato. Fatto: dato per provato dal committente il 12 nov 2026 (archivio 13, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **«Costellazioni» accesa finché non la si spegne** (archivio 12, «16.7», decisione 10). Lo step
  diceva «scelta ricordata» e non da dove parte; parte accesa. Confermato dal committente il
  12 nov 2026 (archivio 13, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **Una notte bianca non ha un nastro** (archivio 12, «16.7», «Trovato e non toccato»). Dove non
  c'è la notte astronomica (Stoccolma da maggio ad agosto, Londra fra fine maggio e metà luglio)
  l'esploratore ha un'ora sola, come l'hero. Fatto l'11 nov 2026 con la proposta com'era, per
  l'esploratore soltanto: lì il nastro va dal tramonto all'alba (archivio 12, «La notte bianca ha un
  nastro»). **Scritto senza compilare e senza lanciare i test: la CI è la prima a provarlo.**
- [x] **La notte bianca sul telefono** (archivio 12, «La notte bianca ha un nastro»): il nastro di un
  luogo a nord in una data d'estate (Stoccolma, 23 giugno, dal calendario), i suoi colori, la
  frase sotto, l'ora che si muove. Fatto: dato per provato dal committente il 12 nov 2026 (archivio 13, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **16.8** L'esploratore: toccare un oggetto (Fase 16). Chiuso il 12 nov 2026 (archivio 13, «16.8 — L'esploratore: toccare un oggetto»).
- [x] **16.8 sul telefono** (archivio 13, «16.8», «Cosa conferma solo il telefono»): il dito sugli
  oggetti (i 24dp, due cose vicine), la planata, il gesto indietro che chiude il foglio, la
  campanella con il permesso delle notifiche e il promemoria che arriva, la pagina e il ritorno
  al foglio, il carattere grande, la luna toccata. Fatto: dato per provato dal committente il 12 nov 2026 (registro, «16.11», «Le conferme, e le prove sul telefono date per fatte»).
- [x] **Il foglio dell'oggetto al posto del pannello dell'ora** (archivio 13, «16.8», decisione 1).
  La tavola 4 lo disegna come un foglio sopra il cielo; è nella pagina, ai piedi, e mentre è
  aperto l'ora non si muove. Confermato dal committente il 12 nov 2026 (archivio 13, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **La campanella del foglio** (archivio 13, «16.8», decisioni 3 e 4): mette il promemoria
  abituale, o trenta minuti dove l'abituale è «mai»; spenta lascia il momento fra i propri; la
  luna non ne ha; il canale silenziato dal telefono non è detto sul foglio. Confermato dal committente il 12 nov 2026 (archivio 13, «Le scelte del 16.8 confermate, e le prove sul telefono dal 16.4 al 16.7»).
- [x] **La riga di Giove sulla regola dell'elenco «Da vedere»** (archivio 13, «16.8», «Trovato e non
  toccato»), come Marte e Saturno dal 16.8. Stesso buio (dal crepuscolo civile) e stesso
  pavimento (10°); la riga non ha il quarto d'ora minimo dell'elenco, e in una notte con due
  tratti prende il primo dove l'elenco prende quello con il punto più alto. Misurato il 12 nov
  2026 (archivio 13, «Giove: la riga contro l'elenco, la misura»): in sei anni le finestre
  coincidono in ogni notte che ne ha due, e la riga ne dà in più da 5 a 11 l'anno, tutte sotto
  il quarto d'ora. Fatto il 12 nov 2026, nella stessa PR, sul sì del committente alla misura
  (archivio 13, «Giove sulla regola dell'elenco»).
- [x] **16.9** «Da vedere», e la notte di un evento (Fase 16). Chiuso il 12 nov 2026 (archivio 13, «16.9 — «Da vedere», e la notte di un evento»).
- [x] **16.9 sul telefono** (archivio 13, «16.9», «Cosa conferma solo il telefono»): il foglio
  «Da vedere» sopra il cielo (aprirlo, trascinarlo, chiuderlo), i chip della notte con il
  carattere grande, il calendario sopra il foglio, la planata dopo una riga, l'anello intorno
  a una figura, il rimando sotto le righe di «In arrivo» sotto il dito (due bersagli in una
  riga), la pagina che si apre sulla notte di un evento. Fatto: dato per provato dal committente il 12 nov 2026 (registro, «16.11», «Le conferme, e le prove sul telefono date per fatte»).
- [x] **«Luna nuova, 3% illuminata» nell'elenco «Da vedere»** (archivio 13, «16.9», «Trovato e non
  toccato»). L'elenco ha la luna dall'1% illuminato e il nome della fase è quello del giorno:
  la falce sottile all'alba usciva come «Luna nuova, 3% illuminata». Fatto il 12 nov 2026,
  nella stessa PR, con un'altra strada da quella proposta: dall'1% è «Falce sottile», nella
  riga e nel foglio della luna (archivio 13, «Le scelte del 16.9 confermate, e i due «trovati»
  corretti»).
- [x] **16.10** «Punta il telefono», da provare sul telefono prima di tenerlo (Fase 16). Scritto il 12 nov 2026 (archivio 13, «16.10 — «Punta il telefono»»); se si tiene lo dice la casella qui sotto.
- [x] **16.10 sul telefono: si tiene o esce** (archivio 13, «16.10», «Cosa conferma solo il telefono: se
  si tiene»). Lo step chiede di provarlo prima di tenerlo, e non è stato visto su nessun
  telefono: il cielo fermo a telefono fermo, il cielo dietro a un braccio che gira, il nord
  dov'è davvero, fra 60° e lo zenit, il telefono basso sull'orizzonte (il cielo si ferma sul
  suo pavimento), il telefono di lato, il pizzico e il tocco mentre punta, lo schermo che
  resta acceso, il riquadro della bussola. Se la bussola non regge un cielo che non balla,
  l'interruttore esce e resta scritto perché. Da confermare con lui le decisioni 2, 3 e 6.
  Con questa, «Dietro le nuvole» dopo il secondo commit (archivio 13, ««Dietro le nuvole» non si
  salva più», «Cosa conferma solo il telefono»): accesa nell'hero resta attraverso le schede
  e passa all'esploratore, spenta lì non torna indietro, e alla riapertura dell'app è spenta.
  Chiusa il 12 nov 2026: data per provata dal committente, e **«Punta il telefono» si tiene**;
  le decisioni 2, 3 e 6 confermate alla PR #162 (registro, «16.11», «Le conferme, e le prove
  sul telefono date per fatte»).
- [x] **«Dietro le nuvole» ricordata fra un avvio e l'altro, e la stessa per l'hero e per
  l'esploratore** (archivio 13, «16.10», «Il parere chiesto su «Dietro le nuvole»»). Fatto il 12
  nov 2026, nella stessa PR, sul sì del committente alla proposta (archivio 13, ««Dietro le
  nuvole» non si salva più»): legata alla notte, in più di quel che la proposta diceva. Oggi è una
  preferenza sola, spenta di default e salvata (`skyBareSky`, chiesta «persistente» il 22 ott
  2026 per la card): accesa nell'esploratore, apre la scheda Cielo ogni sera su un cielo
  stellato sotto il verdetto di una notte coperta. Proposta, non decisa: il default resta
  spento e la scelta non si salva più; l'hero la tiene finché la pagina vive, l'esploratore si
  apre com'era l'hero e poi ha la sua. Il widget non cambia.
- [x] **16.11** La chiusura: guida, VISION, README, CHANGELOG, la rilettura (Fase 16). Chiuso il 12 nov 2026, «16.11 — La chiusura: i documenti, e la Fase 16 riletta»: la Fase 16 è chiusa.
- [x] **16.11 sul telefono** (registro, «16.11», «Cosa conferma solo il telefono»): il pizzico con
  «Punta il telefono» acceso alzando un dito prima dell'altro, un pianeta basso toccato che sta
  sopra il suo foglio, la riga di uno sciame nella sua notte, il capitolo del Cielo della guida
  con il carattere grande. Fatto: provato dal committente sul telefono il 12 nov 2026 (registro, «La coda della Fase 16, i gradi, e le prove del 16.11»).
- [x] **La coda della Fase 16** (registro, «16.11», «Trovato e non toccato»): quel che la
  rilettura ha trovato e non ha toccato. I tocchi e i trascinamenti sotto le fasce dei comandi
  dell'esploratore; una riga dei momenti con «?» e senza il motivo; la percentuale della luna
  nel distintivo al 72%; una notte passata senza previsione che riceve «Non si sa ancora»;
  codice senza chi lo chiama (`sampleAt`, `SkyBody.target`); il vocabolario di prima nei
  commenti del motore (`sky.crontab`, `VISION_SKY.md`, «Fase 16b/16d»). Proposta, non decisa:
  uno step di coda, come il 15.13. Fatto il 12 nov 2026, su richiesta del committente nella
  stessa chat (registro, «La coda della Fase 16, i gradi, e le prove del 16.11»): **scritto
  senza compilare e senza lanciare i test, la CI è la prima a provarlo.**
- [x] **La coda della Fase 16 sul telefono** (registro, «La coda della Fase 16, i gradi, e le
  prove del 16.11», «Cosa conferma solo il telefono»): un tocco accanto ai due pulsanti con un
  foglio aperto, un dito che si muove sul pannello dell'ora, la riga di una congiunzione in
  lune piene, la luna nel distintivo di un luogo senza coordinate. Fatto: provato dal
  committente sul telefono il 12 nov 2026 (registro, «Le prove sul telefono della coda della
  Fase 16»).
- [x] **I gradi che restano a schermo nel Cielo** (registro, «16.11», «Trovato e non toccato»).
  La Fase 16 ha deciso «niente gradi» e le sue superfici non ne hanno; restano da prima «N° di
  distanza» sulle righe delle congiunzioni in «In arrivo» e i gradi nelle pagine degli eventi.
  Da decidere: estendere la regola (le frasi a parole) o scrivere in DESIGN che lì restano.
  Chiusa il 12 nov 2026 con tutte e due, ognuna dove è vera: la riga dice la distanza in lune
  piene, le pagine tengono i gradi con cui definiscono un crepuscolo, e DESIGN §8.8 lo scrive
  (registro, «La coda della Fase 16, i gradi, e le prove del 16.11»).
- [x] **La revisione di Avvisi sul telefono** (registro, «Avvisi come Oggi e Cielo: la revisione
  della schermata», «Cosa conferma solo il telefono»): l'intestazione nel passaggio fra le tre
  schede, «Aggiungi» accanto a «I tuoi» anche con il carattere grande, le righe con la loro
  aria nuova, «I tuoi» vuoto nel suo segmento, TalkBack sugli interruttori di Avvisi e di
  Impostazioni, la striscia del giorno al 200%, il foglio «Un valore nel messaggio». E, dalla
  sezione dopo («Avvisi: le tre scelte confermate…», «Cosa conferma solo il telefono»): la
  pagina e la guida con l'orologio a 12 ore, il selettore dell'ora in piedi e di lato. Fatto:
  provato dal committente sul telefono il 12 nov 2026 (registro, «Le prove sul telefono
  dell'hero e di Avvisi»).
- [ ] **Il ritmo delle sezioni del Cielo** (stessa sezione, «Trovato e non toccato»): 17dp sopra
  un titolo e 5 sotto, dove Oggi e Avvisi hanno i 24 e 12 di DESIGN §8.0. Proposta, non decisa:
  `SkySectionTitle` sui numeri di Oggi, e le due immagini del Cielo rigenerate.
- [ ] **La barra di Avvisi quando la risposta è scorsa via** (stessa sezione, «Proposte, non
  decise»; DESIGN §13, voce 6): il segno e la parola del livello mentre un'allerta è in vigore.
  Proposta, non decisa.
- [x] **Le fasce di Avvisi con l'orologio a 12 ore** (stessa sezione, «Trovato e non toccato»):
  «18–23» accanto a «alle 2:34 PM». Fatto il 12 nov 2026, nella stessa PR, su richiesta del
  committente, con il selettore dell'ora di una fascia (registro, «Avvisi: le tre scelte
  confermate, le ore sull'orologio del telefono, il selettore dell'ora»).
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

Riletto il 12 nov 2026, all'archivio 13, all'inizio del 16.11: le caselle nate dal 12 nov 2026 rimandano all'archivio 13 (`docs/planning/13-novembre-12.md`), la descrizione degli step della Fase 16 resta nell'archivio 11 (16.11 aperto: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.8, 16.9, e 16.10 che decide se «Punta il telefono» si tiene), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; due proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, e il fuso della macchina nelle immagini dei widget del README. Riletto il 12 nov 2026, all'archivio 12, all'inizio del 16.8: le caselle nate dal 10 nov 2026 rimandano all'archivio 12 (`docs/planning/12-novembre-10-11.md`), la descrizione degli step della Fase 16 resta nell'archivio 11 (16.8–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, le due caselle del 16.4, 16.5, 16.6, 16.7, la notte bianca), il peso della cifra e «Costellazioni» accesa da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; due proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, e il fuso della macchina nelle immagini dei widget del README. Riletto il 10 nov 2026, all'archivio 11, all'inizio del 16.3: le caselle nate dal 2 nov 2026 rimandano all'archivio 11 (`docs/planning/11-novembre-2-10.md`), dove sta anche la descrizione degli step della Fase 16 (16.3–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.1, 16.2), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; una proposta non decisa, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo. Riletto il 2 nov 2026, all'archivio 10, con la 2.0.0: le caselle chiuse dal 29 ott 2026 (15.13, 15.14) rimandano all'archivio 10 (`docs/planning/10-verso-2.0.0.md`); restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, dopo la rilettura della Fase 15: aperte 15.13 e 15.14, la coda; e come prima 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, all'archivio 09: le caselle della Fase 15 sono tutte chiuse (in `docs/planning/09-ottobre-28-29.md`), restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 28 ott 2026, all'archivio 08, lo stesso giorno dell'apertura della Fase 15: le caselle sopra sono della Fase 14 (14.12 in attesa) e della Fase 15 (in `docs/planning/08-ottobre-27-28.md`), nessuna proposta non decisa. Riletto il 27 ott 2026, all'archivio 07: le caselle della Fase 14, nessuna proposta non decisa. Fino al 26 ott 2026 nessuna casella aperta (riletto il 22 ott 2026, all'archivio 06):
le ultime chiuse quel giorno stanno in `docs/planning/06-ottobre-20-22.md`, «Cielo: la finestra
reale nella card Stanotte», «Il widget «Momenti del cielo» va via», «Le notifiche: ogni riga nella
forma che sta in una riga», «Il widget «Le prossime ore» va via».

---

## Registro dal 12 nov 2026, dopo l'archivio 13

## 16.11 — La chiusura: i documenti, e la Fase 16 riletta (committente, 12 nov 2026)

L'undicesimo e ultimo step della Fase 16 (archivio 11, «Fase 16 — Cielo: la pagina come Oggi e il
cielo da esplorare», «Gli step»): «la guida (IT/EN) riletta per intero sul Cielo, VISION §5.3, il
README (uno screenshot dell'esploratore, il testo), il CHANGELOG, DESIGN riletto dove il Cielo è
cambiato, e la rilettura degli undici step contro questa sezione, come per la Fase 15». Chiesto
con la formula della fase («Esegui lo step 16.11 della Fase 16 in PLANNING.md, da origin/main
aggiornato; alla fine pusha e apri la PR»), e con una riga in più: «Segna come chiusi i test
aperti da fare per i punti precedenti di questa fase». Prima dello step, come commit a sé,
**l'archivio 13**: il registro era a 810 righe e questa sezione l'avrebbe portato oltre le
1 000; la regola della fase archivia all'inizio dello step che lo trova così.

### Le conferme, e le prove sul telefono date per fatte

Due cose dette dal committente e non ancora scritte qui, con le sue parole e con la mia
lettura, che è una lettura.

- **Alla PR #162** (16.10), unendola il 12 nov 2026: «Confermo le tue scelte, ho unito la PR».
  Le scelte che gli erano state sottoposte erano tre (archivio 13, «16.10», decisioni 2, 3 e
  6): è il dito che trascina a spegnere «Punta il telefono», non il pizzico né i due pulsanti;
  un tocco su un oggetto non lo spegne; lo schermo resta acceso finché punta. Confermate, con
  il secondo commit di quella PR («Dietro le nuvole» che non si salva più).
- **Con questo step**: «Segna come chiusi i test aperti da fare per i punti precedenti di questa
  fase». Le caselle aperte di quel genere erano tre: «16.8 sul telefono», «16.9 sul telefono» e
  «16.10 sul telefono: si tiene o esce». Chiuse tutte e tre come le precedenti (archivio 12,
  «Le prove sul telefono della Fase 16, da 16.1 a 16.3»; archivio 13, «Le scelte del 16.8
  confermate, e le prove sul telefono dal 16.4 al 16.7»): **date per provate**. Per il 16.10 la
  casella era anche una decisione, e la lettura è che **«Punta il telefono» si tiene**: DESIGN
  §8.8d e VISION §5.3 lo dicono ora al presente. Non ha scritto di averlo provato su un
  telefono, e i numeri del filtro restano aritmetica (archivio 13, «16.10», decisione 11): se
  la bussola non regge un cielo che non balla, quel che esce è scritto lì («Cosa conferma solo
  il telefono: se si tiene») e si toglie in un commit.

### La rilettura: come è stata fatta, e il verdetto

Come per la Fase 15 (archivio 10, «La Fase 15 riletta»): gli undici step contro la sezione che
li descrive, le regole comuni e le decisioni prese, e i documenti contro il codice. Tre letture
indipendenti del codice, una per gruppo di step (la pagina, 16.1 e 16.2; il motore e il dato,
da 16.3 a 16.5; l'esploratore, da 16.6 a 16.10), ognuna con la descrizione dello step, quel che
lo step ha scritto di aver fatto e deviato, il codice e i suoi test; la guida in IT e in EN,
VISION §5.3, DESIGN §8.8, §8.8b, §8.8d e §8.12b, il README e il CHANGELOG `[Unreleased]` letti
per intero da chi scrive; ogni riscontro verificato sul codice prima di toccare qualcosa. Un
difetto in più è uscito disegnando lo screenshot dell'esploratore, non leggendo.

**Il verdetto, in breve.** Quel che gli step promettevano c'è: nessuna promessa mancata che non
sia scritta come deviazione, le regole del motore con i numeri di Horizons e la data in cui
sono stati letti, la fonte delle costellazioni con la sua licenza nelle Impostazioni, nel
README e in `licenses/`, l'esploratore che disegna solo quando la camera o l'ora cambiano, i
sensori accesi solo con l'interruttore e la pagina davanti, nessun gergo sulle superfici della
fase, nessuna app sorella nominata. Non reggevano: **i documenti** (VISION §5.3 descriveva
ancora la card e negava lo schermo intero; la guida aveva tutto l'esploratore in un solo
capoverso sotto «Stanotte»; il README non lo nominava), **una regola comune** (l'inquinamento
luminoso che «la guida dice una volta» non era detto da nessuna parte), **cinque difetti nel
codice**, e una ventina di commenti rimasti a prima della fase.

### Cosa è cambiato: i documenti

- **La guida, il capitolo del Cielo** (IT ed EN). «Stanotte» era diventato un capoverso di
  novecento parole, cresciuto di una frase a ogni step, con «Scorrendo, il luogo resta in cima»
  finito in mezzo all'esploratore. Ora sono quattro voci: **«Stanotte»** (l'hero: il verdetto,
  il nastro, l'immagine, «Dietro le nuvole», la barra fissata), poi dopo i verdetti **«Esplora
  il cielo»** (lo schermo intero, il pannello dell'ora e della notte, le nuvole come un velo,
  «Costellazioni», «Punta il telefono»), **«Cosa stai guardando»** (il tocco e il foglio) e
  **«Da vedere»** (l'elenco); la frase su «Guarda il cielo di quella notte» sta in «In arrivo»,
  dove sta il rimando. Corrette nel passaggio: la campanella di uno sciame dice «la notte del
  picco» e non «le sere»; la luna ha la sua pagina; lo schermo resta acceso mentre il telefono
  punta e il foglio si apre senza che il cielo smetta di seguirlo; «Adesso» sta accanto ai due
  pulsanti e prima dell'imbrunire dice «Stanotte»; il radiante è spiegato una volta («il punto
  da cui le sue meteore sembrano partire»); «la stessa della card in cima» è «quella nel cielo
  in cima alla pagina»; «il widget Cielo», che non esiste più, è «La finestra»; uno spazio
  mancante in italiano («lo dice.Scorrendo»); in inglese le virgolette sono di nuovo quelle
  della lingua. **E la frase che la regola comune voleva**: «Le stelle sono quelle che si vedono
  da un posto buio: quante ne tolgono le luci intorno a te, Chiaro non può saperlo». Le parole
  «esploratore» ed «explorer» non sono sullo schermo: la guida dice «il cielo a tutto schermo».
- **VISION §5.3.** «Stanotte» è l'hero della pagina e non una card; **«Explore the sky»** è una
  voce nuova, che scrive che la fase **rovescia la decisione del 22 ott 2026** («no full screen
  and no more names than the widget writes: a sky map is another app's expectation») e perché,
  i suoi cinque pezzi, quel che resta fuori perché rumore, e che resta il cielo a occhio nudo
  di §3.2; i momenti sono le righe dell'agenda; il catalogo è di 62 e si apre da «Aggiungi» nel
  titolo; Marte e Saturno accanto a Venere e Giove; la notte dello sciame e il rimando alla
  sua notte in «Next events».
- **DESIGN.** §8.8b, «The hero always answers», aveva ancora le misure della card (la parola a
  `headlineMedium`, gli orari grandi sotto un'etichetta): ora dice l'hero. «Le altre ragioni
  di un verdetto che manca restano sulla riga» era falso dal 22 ott 2026: si dicono una volta
  sotto la sezione. §8.8d: il titolo copre il 16.10; «da provare sul telefono prima di tenerlo»
  è al passato; i trentuno nomi di stelle rimandano a `NamedStars` e non a un conto che §3.9 non
  fa; il pavimento di un ammasso è 20° e non 10°; e le cinque correzioni qui sotto. Le due
  frasi della sezione Avvisi che citavano `SkyRow` e il pulsante flottante del Cielo sono
  datate e restano, con una nota su cosa è cambiato dopo.
- **README.** Una frase nel primo capoverso, la riga «Sky» riscritta e una riga **«Explore the
  sky»** in Features, pianeti e stelle nella tabella dei dati calcolati sul telefono, e **lo
  screenshot dell'esploratore** (`sky-explore.png`, sotto). Senza trattini lunghi.
- **CHANGELOG `[Unreleased]`.** La voce dell'oggetto toccato dice le due campanelle e la pagina
  della luna; la notte dello sciame dice che la riga resta fino all'alba; e due voci in Fixed
  che mancavano dal 10 nov 2026 (archivio 12, «Le due correzioni trovate con le caselle», fatte
  in dieci minuti e mai scritte lì): il promemoria giudicato sulla finestra intera, e le
  Quadrantidi a gennaio.
- **I commenti nel codice** rimasti a prima della fase: «Venus and Jupiter, and no others» e
  «the two planets» dove i pianeti sono quattro (`SkyJobCatalog`, `SkySights`, `PlanetMath`,
  `WindowPainter`, `WindowWidget`, `SkyPictures`); «all fifty» e «fifty-one events» dove il
  catalogo è di 62; «Castor, the faintest star the app names», vero per la finestra e non più
  per l'esploratore; la finestra di uno sciame «some nine hours wide»; «only a daily window can
  cross midnight»; un commento di `SkyPassageTest` che diceva cinque gradi dove il test ne usa
  due. E la **regola 2 di `WorthSeeing`**, che metteva il radiante fra le cose che aspettano il
  crepuscolo nautico: il codice e il suo test lo tengono nella notte astronomica del picco, che
  è la finestra dello sciame, e la regola ora lo dice con il motivo (uno sciame che il
  calendario dice senza notte buia non sta nemmeno nell'elenco). Via cinque import che
  `SkyScreen` non usava dal 16.2 e un'intestazione vuota.

### Cosa è cambiato: cinque correzioni nel codice

Ognuna con il suo test. Sono difetti certi e piccoli, del tipo che il committente ha già fatto
correggere nella stessa PR quando gli sono stati mostrati (archivio 13, «Le scelte del 16.9
confermate, e i due «trovati» corretti»); quel che è una scelta sua è in «Trovato e non toccato».

1. **La riga di uno sciame resta in «In arrivo» per tutta la sua notte.** Dal 10 nov 2026 la
   finestra di uno sciame è la notte intera del picco, dal crepuscolo all'alba, e il calendario
   chiedeva a `SkyScheduler.next` un inizio ancora davanti: dall'imbrunire della notte delle
   Geminidi la riga era quella del 2027, e «Guarda il cielo di quella notte» apriva un cielo a
   un anno da lì. `SkyStateBuilder` chiede ora a `SkyUpcoming.of` anche i momenti annuali, la
   regola che i momenti e il widget hanno già («una finestra in corso vince»). Dopo mezzanotte
   la riga tiene la sua data, senza conto dei giorni. `SkyStateBuilderTest`.
2. **Un oggetto basso non finisce dietro il suo foglio.** Trovato guardando una variante dello
   screenshot: Saturno a 13° un'ora dopo il sorgere, toccato, stava sotto le parole che lo
   descrivono, anello compreso. La camera veniva puntata con l'orizzonte del pannello (0,72
   dell'altezza sul telefono di riferimento); il foglio che ne prende il posto è più alto e
   l'orizzonte sale con lui. Ora `select` punta con l'orizzonte più alto che un piede può
   chiedere (`SkyCamera.HighestHorizon`, 0,56); dove il foglio è più basso la camera resta
   comunque sulla sua riga. Sul telefono di riferimento, per aritmetica, una luce da circa 8°
   in su sta sopra il foglio più alto con il suo anello; più in basso resta dietro, e il foglio
   dice che è bassa e che i tetti sono di mezzo. `SkyExploreScreenTest`, con Saturno a 12°.
3. **Il foglio dice perché il cielo è senza nuvole.** La regola comune («oltre la previsione il
   cielo è senza nuvole e lo dice») era tenuta dalla riga del pannello, e il foglio di un
   oggetto copre il pannello: una notte aperta da «In arrivo» su uno sciame oltre la previsione
   era un cielo stellato senza una parola. Il 16.8 (decisione 2) contava su quella riga. Dove
   la previsione non ha quell'ora, al posto del chip il foglio porta la frase del pannello.
4. **Il dito rimasto giù alla fine di un pizzico non riprende il cielo al telefono.** Con
   «Punta il telefono» acceso il pizzico doveva solo avvicinare (scelta confermata). Ma due
   dita non lasciano uno schermo nello stesso istante, e il movimento di quello rimasto
   arrivava come un trascinamento: su un telefono vero il pizzico spegneva l'interruttore. Il
   test lo alzava insieme all'altro e non lo vedeva. Un tocco che è stato un pizzico lo resta
   fino alla fine.
5. **La campanella di Venere all'alba dice «le mattine».** Prima dell'alba il foglio di Venere
   mette il promemoria sulla riga della stella del mattino, e il pulsante diceva «Avvisami le
   sere in cui si vede bene». Due stringhe nuove (IT/EN).

### Decisioni e deviazioni

1. **Le correzioni al codice in uno step di documenti.** Lo step è «la chiusura» e per la Fase
   15 la rilettura aveva aperto due step di coda. Qui i cinque difetti sono corretti nello
   step: sono piccoli, hanno un test ciascuno, e due dei cinque smentivano una frase che la
   guida stava per ripetere. Quel che chiede una scelta non è stato toccato.
2. **«Punta il telefono» e il «Dietro le nuvole» dell'esploratore tornano com'erano se il
   sistema riporta la pagina.** Sono `rememberSaveable`, e DESIGN diceva «kept across a rotation
   only». Non cambia il codice, cambia la frase: lo stato salvato di una pagina la riporta come
   il lettore l'ha lasciata, davanti a lui, ed è un'altra cosa da una preferenza che accende i
   sensori a ogni apertura (quella che il 16.10 escludeva). DESIGN §8.8d lo dice ora com'è.
3. **Uno screenshot solo dell'esploratore, e la sera della registrazione.** `sky-explore.png` è
   la pagina come la apre «Esplora» alle 21:00 della sera registrata (`MilanRecording.night`,
   l'ora della foto notturna di Oggi: nuvole 13%), con le nuvole della previsione e
   «Costellazioni» accesa com'è finché non la si spegne: la luna al 99% con il suo alone,
   Saturno, Pegaso e l'Acquario. Guardate e scartate: la metà della notte (01:15, nuvole
   100%: un cielo piatto, vero e muto), la stessa con «Dietro le nuvole», e un oggetto toccato,
   che è la variante da cui è uscito il difetto 2. Niente è scelto per bellezza: con la luna
   quasi piena le stelle sono poche, e sono quelle.
4. **La tabella del README.** Con dieci immagini da tre per riga ne avanzava una: la riga del
   Cielo è ora cielo, esploratore, calendario; la guida passa accanto agli Avvisi; il riepilogo
   del mattino, che non è una schermata dell'app ma la tendina delle notifiche, sta da solo
   sotto la tabella come la foto notturna e il pieghevole.
5. **«Esploratore» non è una parola dello schermo.** L'app dice «Esplora» sul pulsante e
   nient'altro; la guida diceva «nell'esploratore» senza che nessuno l'avesse chiamato così.
   Resta il nome nel codice e in questi documenti.
6. **`WindowTurnTest`** era fra i test che il 16.1 diceva di toccare e il suo registro non dice
   che passa com'è: passa com'è, nella suite di oggi.

### Trovato e non toccato

In «Aperto», in due caselle. La prima, **«La coda della Fase 16»**, sono cose piccole che
chiedono una scelta o un'occhiata sul telefono:

- **I tocchi sotto le fasce dei comandi.** La fascia fra «Adesso» e i due pulsanti è larga
  quanto lo schermo, e quella degli interruttori comprende il vuoto in fondo alla seconda riga:
  con la camera alzata una stella con un nome lì sotto non si tocca, e un tocco lì non chiude
  il foglio. E un trascinamento che parte dal fondo nudo del pannello dell'ora gira il cielo (e
  spegne «Punta il telefono»), dove un tocco è già fermato.
- **Una riga dei momenti con «?» e senza il motivo.** Fra «Oggi» e «Domani» un verdetto «Non si
  sa ancora» per una previsione che non copre quell'ora esce senza numero e senza frase; «In
  arrivo» lo dice una volta sotto la sezione. C'era prima del 16.2 ed è raro su due giorni.
- **La percentuale della luna nel distintivo** (un luogo senza coordinate) è ancora al 72% e
  senza alone, dove il 16.1 ha portato ogni riga dell'hero al bianco pieno.
- **Una notte passata senza previsione** riceve «Non si sa ancora» invece di nessun verdetto
  (`nightForecast` risponde `NONE` prima di provare `PAST`).
- **Codice senza chi lo chiama**: `sampleAt` in `TonightSky` e `SkyBody.target` hanno solo un
  test; una variabile locale non usata in `TonightHero`.
- **Il vocabolario di prima nei commenti del motore**: `sky.crontab`, «the file»,
  `VISION_SKY.md`, e una «Fase 16b/16d» che è di un'altra numerazione e ora si legge come la
  Fase 16 di Chiaro (`SkyScheduler`, `SkyJobCatalog`, `MeteorShowerTable`, `SkyVerdictEngine`).

La seconda è una scelta di prodotto: **i gradi che restano a schermo nel Cielo**. La fase ha
deciso «niente gradi» e le sue superfici non ne hanno; restano, da prima, «N° di distanza»
sulle righe delle congiunzioni in «In arrivo» (Fase 28: una separazione, non una posizione) e
i gradi nelle pagine degli eventi (`sky_about_*`: «più di 10° sopra l'orizzonte»). Estendere la
regola vuol dire riscrivere quelle frasi a parole («larga quanto un pugno a braccio teso»); non
estenderla vuol dire scriverlo in DESIGN.

### Come è stato verificato

- **I test nuovi**, uno per correzione: `SkyStateBuilderTest` (le Geminidi seguite restano la
  riga del 13 dicembre alle 22:00 e alle 03:00 del 14, e a mezzogiorno del 14 sono quelle del
  2027, con l'anno); `SkyExploreScreenTest`: Saturno fra 10° e 13° toccato sta sopra il foglio
  con tutto l'anello; senza previsione il foglio dice «Drawn without clouds: there is no
  forecast», e nella notte di un evento oltre la previsione «the forecast does not reach this
  night»; un pizzico in cui un dito si alza prima e l'altro continua a muoversi lascia
  l'interruttore acceso, il cielo più vicino e dove il telefono punta; Venere alle 06:00 del
  10 dicembre ha «Tell me the mornings», mette il promemoria su `venus.morning` e diventa «You
  will be told the mornings».
- **Guardato**: `sky-explore.png` e le tre varianti scartate; il foglio di Saturno basso
  (`CHIARO_RENDER_DIR`), con il pianeta e il suo anello sopra il foglio, dove nella variante
  di prima stavano sotto il chip del verdetto; le due lingue della guida rilette per intero
  dopo il taglio, voce per voce.
- **La suite**: `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
  3 367 test nelle cartelle dei risultati, 16 saltati (gli screenshot, a richiesta), **uno
  fallito**: `AlertsViewModelTest`, «choosing a place binds, re-arms, and brings an orphan back
  on», nella variante release, per un timeout delle coroutine. È il test che sotto una suite
  carica scade su DataStore (già visto al 16.2), non tocca niente di questo step, e la stessa
  classe nella variante debug della stessa corsa è verde. Rilanciata da
  sola nelle due varianti: 9 test su 9. Nella stessa seconda corsa, dopo l'ultimo ritocco a una
  frase della guida, `ItalianArticleTest`, `StringsParityTest`, i test della guida,
  `:app:lintDebug` (0 errori) e `:app:assembleDebug`, che il fallimento aveva fermato: verdi.
- **Gli screenshot del README**: `sky-explore.png` è nuovo, disegnato con
  `--tests "*ReadmeScreenshots.skyExplore" -PupdateScreenshots`; gli altri non cambiano
  (nessuna schermata che disegnano è stata toccata: la guida è inquadrata in testa, non nel
  capitolo del Cielo) e non sono stati rigenerati.
- **L'indice dell'archivio**: `python tools/planning_index.py --check` passa, dopo l'archivio 13.
- **Non verificato**: niente di questo step è stato visto su un telefono (la casella «16.11 sul
  telefono»); il pizzico è provato con i puntatori del test, che sono quel che un telefono
  manda e non un telefono.

### Cosa conferma solo il telefono

Casella «16.11 sul telefono»: il pizzico con «Punta il telefono» acceso, alzando un dito prima
dell'altro; un pianeta basso toccato (Venere una sera qualsiasi), che deve stare sopra il suo
foglio; la riga di uno sciame nella sua notte (le Geminidi, la sera del 13 dicembre); il
capitolo del Cielo della guida, quattro voci, con il carattere grande.

## La coda della Fase 16, i gradi, e le prove del 16.11 (committente, 12 nov 2026)

Unita la PR #163, il committente nella stessa chat: «Ho fatto i test sul telefono e puoi
segnarli come fatti nel planning. Puoi chiudere anche qui in questa chat le caselle aperte in
planning che mi hai detto qui sopra? In una nuova PR o se sono semplici mergia direttamente in
main e poi aspetto la CI che compili tutto, non compilare qui altrimenti mi blocchi la
macchina per 30 minuti». Le caselle erano tre: «16.11 sul telefono», «La coda della Fase 16»,
«I gradi che restano a schermo nel Cielo».

- **«16.11 sul telefono» è fatta**, e questa volta provata: lo dice lui.
- **Una PR, non `main`.** Sono modifiche al codice mai compilate: su un ramo la CI le prova
  prima che arrivino dove si costruisce l'APK.
- **Niente di quel che segue è stato compilato, lanciato o disegnato in locale, per
  richiesta.** Ogni modifica è stata letta contro il codice che tocca e i suoi chiamanti; i
  test nuovi sono scritti e non lanciati. La CI è la prima a provarli.

### Cosa è cambiato

1. **I tocchi accanto ai comandi dell'esploratore.** Un tocco sul cielo era scartato dentro
   ogni fascia che sta sopra il cielo tranne quella in alto; le fasce sono larghe quanto lo
   schermo e i pulsanti prendono già i loro tocchi. Ora è scartato solo sul pannello dell'ora e
   sul foglio, che hanno un fondo loro: accanto ai due pulsanti e agli interruttori il cielo è
   cielo, una stella con un nome lì risponde, e un tocco lì chiude il foglio.
2. **Un dito che si muove sul pannello dell'ora non gira il cielo.** Il fondo del pannello non
   prendeva i tocchi, e un trascinamento partito da lì arrivava al cielo sotto (e spegneva
   «Punta il telefono»). Il pannello ora li tiene.
3. **Una riga dei momenti con «?» dice perché.** Dove la previsione non arriva all'ora di un
   momento, la riga porta il motivo nella sua frase, come le righe di «In arrivo»; l'età o
   l'assenza del dato restano dette una volta sotto la sezione.
4. **La percentuale della luna nel distintivo** (un luogo senza coordinate) è bianca piena con
   l'alone, come ogni riga dell'hero dal 16.1.
5. **Una notte passata è passata, con o senza previsione.** `nightForecast` rispondeva «nessuna
   previsione» prima di guardare se la notte era finita, e una notte già spesa riceveva «Non
   si sa ancora». Nell'esploratore, senza previsione, una notte passata dice ora «questa notte è
   passata».
6. **Via il codice senza chi lo chiama**: `sampleAt` con il suo test, `SkyBody.target` con le
   sue due asserzioni, una variabile locale in `TonightHero`.
7. **Le fasi dell'altra app, dette dell'altra app.** Nei commenti del motore «Fase 16b» …
   «16f» sono ora «upstream Fase 16b» (36 righe in 26 file), e in testa ad `AstronomyEngine`
   una nota dice come leggere i commenti del pacchetto: `sky.crontab`, «the file» e
   `VISION_SKY.md` sono lo schermo e il documento dell'app da cui il motore viene.
8. **I gradi.** La riga di una congiunzione dice la distanza **in lune piene**: «a circa 6
   lune piene di distanza» dove diceva «3° di distanza», e «a meno di due lune piene» sotto il
   grado. Le pagine degli eventi tengono i loro gradi.

### Decisioni e deviazioni

1. **I gradi: tutte e due le strade, ognuna dove è vera.** La casella chiedeva di scegliere fra
   estendere la regola e scrivere che lì i gradi restano; il committente ha chiesto di
   chiuderla, non ha detto come. Sulla riga il grado era un numero senza figura, e la luna
   (mezzo grado) è la larghezza che in cielo hanno visto tutti: i gradi raddoppiati e
   arrotondati, da 2 a 10. Nelle pagine no: un crepuscolo *è* il sole a 6, 12 o 18 gradi
   sotto l'orizzonte, la pagina è dove si viene a farsi spiegare la parola, e tre crepuscoli
   distinti da «poco», «di più» e «molto» non spiegherebbero niente. DESIGN §8.8 scrive dove
   la regola si ferma. Tornare indietro sulla riga è una stringa e una costante.
2. **Il vocabolario di prima non è riscritto.** Sono una quarantina di commenti, la voce stessa
   del motore, e `UPSTREAM.md` dice da dove viene. Quel che ingannava era una cosa sola, i nomi
   delle fasi che si leggevano come la Fase 16 di Chiaro: quella è tolta, e una nota dice il
   resto una volta.
3. **Il motivo sulla riga, e il chip resta.** «In arrivo» toglie il chip a un verdetto che non
   c'è; fra i momenti il «?» resta con il motivo accanto, perché togliere un chip da una riga
   che ieri lo aveva è un cambiamento più grande di quello chiesto, e non si è potuto guardare.
4. **Il tocco è scartato solo sul pannello e sul foglio.** La strada più corta fra le due
   possibili: i pulsanti non hanno bisogno di essere protetti, il fondo sì.

### Come è stato verificato

- **Non in locale**, come chiesto: né la compilazione, né i test, né lint.
- **Letto**: ogni simbolo tolto cercato in tutto il repository (`sampleAt` e `SkyBody.target`
  avevano solo i loro test; `sky_conjunction_apart` un solo chiamante); gli import che le
  modifiche usano sono già nei file; nessuno screenshot del README mostra una riga cambiata
  (`sky-ahead.png` non ha congiunzioni né «?»), quindi nessuno si rigenera.
- **I test scritti e non lanciati**: `SkyStateBuilderTest` (una notte passata senza previsione
  è `PAST` e senza verdetto); `SkyMomentsRowsTest` (con la previsione tagliata alle sue prime due
  ore il motivo è su almeno due righe: corretto dopo la prima CI, qui sotto); `SkyExploreScreenTest` (un tocco accanto ai due
  pulsanti con il foglio di Saturno aperto lo chiude; un dito che si muove sull'ora del
  pannello lascia la camera dov'era).
- **Se la CI è rossa, i primi sospetti**: il test del tocco accanto ai pulsanti (una geometria
  mai disegnata); `SkyMomentsRowsTest` (quale delle due frasi il motore dà a un momento oltre
  l'ultima ora); un test esistente che fissava il «?» senza motivo o il tocco scartato nelle
  fasce.

### La prima CI: rossa per un test, non per il codice

La CI della PR #164 ha compilato tutto e ha fallito su un test solo, uno dei quattro nuovi:
`SkyMomentsRowsTest`, «a moment the forecast does not reach says so on its row», che voleva il
motivo almeno due volte e l'ha trovato una. Il committente: «Scopri perché, correggila ed
esegui il push». **Sbagliata era la premessa del test, non la correzione.** Il test tagliava
la previsione alle prime 13 ore credendo che partissero da mezzanotte; le ore di un report
partono dall'ora in cui è stato scaricato (il mapper tiene a parte quelle passate), quindi 13
ore dalle 12:00 arrivano a mezzanotte e coprono l'ora d'oro e il tramonto della sera: fuori
restava solo l'alba di domani, una riga, ed era giusto che il motivo fosse lì una volta. In
più l'hero non lo dice: la notte registrata è una notte di luna, senza finestra. Il test
taglia ora a due ore (12:00 e 13:00), e chiede il motivo su almeno due righe. Gli altri tre
test nuovi e i 52 dell'esploratore erano verdi nella stessa corsa (dal rapporto della CI);
lint e le due build non erano partiti, e partono con questo push. Di nuovo niente compilato in
locale.

### Cosa conferma solo il telefono

Casella «La coda della Fase 16 sul telefono»: un tocco accanto ai due pulsanti con un foglio
aperto; un dito che si muove sul pannello dell'ora; la riga di una congiunzione in lune piene,
quando il calendario ne ha una; la luna nel distintivo di un luogo senza coordinate.

## Avvisi come Oggi e Cielo: la revisione della schermata (committente, 12 nov 2026)

Unita la PR #164, il committente in una nuova chat: «Fai una review della schermata Avvisi.
Allinea grafica, tipografia e layout alle schermate Oggi e Cielo già sottoposte a review in
modo da mantenere omogeneità e coerenza tra le schermate. Segui le linee guida Google Material3
riguardo a UI/UX, layout, grafica e tipografia. Verifica anche se ci sono errori, se ci sono
suggerimenti che vorresti applicare o nuove implementazioni da suggerire. Se puoi, e non vedi
controindicazioni, alla fine non compilare qui perché mi tiene la macchina impegnata per circa
30 minuti: lascia partire la CI su GitHub e verifica da lì se è verde o rossa».

Una PR sola, dal `main` aggiornato (3b6f11e5), ramo `claude/alerts-review`. **Niente è stato
compilato, lanciato o disegnato in locale, per richiesta**: la CI ha compilato e lanciato i
test, e le immagini del README le ha disegnate un runner (qui sotto, punto 12).

### Come è stata fatta

- **Letta tutta**: `AlertsScreen.kt` (2 680 righe), `AlertsViewModel`, `NextWindow`,
  `AlertChannels`, le parti di `RuleText` che la pagina chiama; e di Oggi e del Cielo quel che
  le loro revisioni hanno lasciato (`PlaceHeader`, `SectionTitle`, `TimelineRow`,
  `SkySectionTitle`, il pulsante «Aggiungi»), con DESIGN §6, §8.0, §8.1b, §8.9b, §8.12.
- **Misurata sulle immagini del README**, che sono a 384dp e due pixel per dp (le distanze
  lette sull'immagine, al dp): `alerts.png` e
  `alerts-yours.png` accanto a `today-day.png`, `sky-tonight.png` e `sky-ahead.png`.

| | Oggi | Cielo | Avvisi prima | Avvisi ora |
|---|---|---|---|---|
| La prima riga dell'intestazione | il luogo, `titleLarge` | il luogo, `titleLarge` | «Avvisi», `titleLarge` | il luogo, `titleLarge` |
| La seconda | il giorno e l'ora | la notte | il luogo, `titleSmall` | «Avvisi», `labelLarge` |
| Sopra un titolo di sezione | 24 | 17 | 20 (24 sotto l'hero) | 24 |
| Sotto un titolo di sezione | 12 | 5 | 8 | 12 |
| Aria sopra e sotto in una riga | 12 | 12 | 8 | 12 |
| Il disegno di una riga | 34 | 34 | 36 | 34 |
| L'azione della pagina | — | «+ Aggiungi» nel titolo | pulsante flottante | «+ Aggiungi» nel titolo |

### Cosa è cambiato

1. **Il luogo apre l'intestazione**, come su Oggi e sul Cielo: la riga del luogo di Oggi
   (`PlaceHeader`) sulla carta, «Milano ⌄» e sotto «Avvisi», un solo pulsante per TalkBack
   («Milano, Avvisi», azione «Cambia luogo»). Il blocco è alto gli stessi 48dp: sotto non si
   sposta niente. Senza luogo resta il `TabHeader` con il solo nome della scheda, come sul Cielo.
2. **«Aggiungi» nel titolo di «I tuoi»**, al posto del pulsante flottante: lo stesso pulsante
   del Cielo (`TitleAction`, ora in `ui/components` e usato da tutti e due), la stessa parola,
   «Nuovo avviso» per chi ascolta. Il foglio che apre porta «Nuovo avviso» in testa. Il
   margine sotto l'ultima riga torna 16dp (era 88, per il pulsante).
3. **Un ritmo, quello di Oggi** (DESIGN §8.0): 24 sopra un titolo e 12 sotto. I titoli sono
   il `SectionTitle` di Oggi, che ora accetta un'azione; `GroupTitle`, la copia di Avvisi con
   i suoi numeri, non c'è più.
4. **Le righe come quelle dell'agenda**: 12dp sopra e sotto (`ListRow.Vertical`, era 8) e il
   disegno al gradino dell'agenda (`WeatherIconSize.Timeline`, 34dp; era un 36 solo suo).
   `TimelineRow` legge ora il 12 da `ListRow`: un numero solo per le tre schede.
5. **«I tuoi» vuoto è un segmento**: la frase stava nuda sulla carta sotto il titolo, l'unico
   contenuto di una sezione fuori da un contenitore (§8.0). Dice ora «…: «Aggiungi» parte da
   un'idea o da zero».
6. **Una riga con l'interruttore dice se è accesa** (errore): `toggleable` al posto di
   `clickable` con il ruolo dell'interruttore, sulle cinque righe di Avvisi e sulle righe di
   Impostazioni. L'interruttore disegnato non ha stato (`onCheckedChange = null`), e la riga
   era per TalkBack un interruttore che non diceva mai se era acceso.
7. **L'interruttore di un avviso tuo porta il suo nome** (errore): è un bersaglio a parte
   dalla card, e da solo era «acceso, interruttore» una volta per avviso.
8. **La striscia del giorno è alta quanto le sue ore** (errore): il disegno teneva 14dp per
   le ore sotto la traccia qualunque fosse il carattere; a 200% sono 32dp, disegnate 8dp dentro
   la frase delle ore silenziose.
9. **«Un valore nel messaggio» è un foglio a gruppi**, quelli di «Cosa guardare», dopo i due
   valori dell'avviso stesso: era una finestra di una quarantina di righe nell'ordine del
   registro, la lista che il 15.7 aveva tolto al selettore della variabile e lasciato qui.
10. **Via il codice senza chi lo chiama**: `PaneBlocks.firstInView` e la misura del primo
    blocco di sinistra servivano solo all'etichetta del pulsante flottante.
11. **I documenti**: DESIGN §6, §8.1b, §8.9b («In Today's shape»), §8.12b, §8.12c e due voci
    in §13; VISION §5.4; la guida (due frasi che dicevano «Nuovo avviso, il pulsante in
    basso»); CHANGELOG; i due testi alternativi del README.
12. **Le immagini del README disegnate su un runner**, e le due di Avvisi rigenerate così:
    `.github/workflows/readme-screenshots.yml` lancia `ReadmeScreenshots` con
    `-PupdateScreenshots` e lascia `docs/screenshots/*.png` come artefatto; non committa
    niente. Parte a mano dalla scheda Actions, su qualunque ramo, e a un push che tocca il
    file stesso (mai su `main`), perché un workflow si può lanciare a mano solo quando il
    ramo di default lo ha già.

### Decisioni e deviazioni

1. **L'intestazione cambia una decisione della Fase 16.** Archivio 11, «Fase 16 — Cielo: la
   pagina come Oggi e il cielo da esplorare», «Gli step», 16.1: «il `TabHeader` resta ad
   Avvisi». Lì era una nota a margine del lavoro sul Cielo, non una scelta su Avvisi; ora che
   la richiesta è l'omogeneità delle tre schede, l'intestazione è la differenza che si vede per
   prima: il luogo cambiava riga e misura alla terza scheda. Avvisi non ha un cielo su cui
   stendere la riga, quindi sta sulla carta, nell'inchiostro della pagina. **La seconda riga è
   il nome della scheda**: Oggi lì dice il giorno, il Cielo la notte, cioè di che cosa parla la
   pagina. Tornare indietro è un blocco in `AlertsScreen`.
2. **Il pulsante cambia forma, non la decisione del 14 ott 2026** (archivio 05, «Avvisi:
   «Nuovo avviso» è l'unico ingresso»): resta l'unico ingresso, e al tetto sparisce con la
   frase che dice perché. Era flottante «come quello del Cielo», che dal 16.2 è nel titolo per
   una ragione che qui vale di più: il pulsante stava sopra la colonna degli interruttori.
3. **«Aggiungi», non «Nuovo avviso», sul pulsante.** Una parola sola regge anche al 200% di
   carattere accanto a «I tuoi» (con «Nuovo avviso», a 360dp, al titolo non resterebbe una riga intera: un conto sulle larghezze, non una misura), ed è la
   stessa del Cielo nello stesso posto. Il nome intero resta a TalkBack e in testa al foglio.
4. **La nota dell'età sotto il titolo, quando c'è il pulsante.** «Aggiornato 3 ore fa» stava
   in fondo alla riga del titolo; con il pulsante accanto, a un carattere grande al titolo
   restavano pochi dp. Solo nel titolo con un'azione: i titoli di Oggi non cambiano.
5. **12 e 34 cambiano il 15.7** (archivio 09, «15.7 — Le righe come il Cielo, l'ora relativa,
   «Cosa guardare»»): lì 8dp e 36dp erano le misure del Cielo di allora e un gradino scelto
   contro un Cielo a 51. Il Cielo ha preso la riga dell'agenda con il 16.2.
6. **Impostazioni toccata per un rigo**: lo stesso difetto dell'interruttore, nella stessa
   forma. Lasciarlo lì sapendolo non era una scelta.
7. **Quel che di Material 3 è stato guardato e lasciato com'è**: i due chip di «Avvisami da»
   (una scelta fra due valori; i chip filtro a scelta singola sono ammessi e sono la forma di
   «In che orari» e «Dove» nell'editor); lo slider della soglia e i pulsanti radio del
   confronto in una finestra; lo snackbar lungo con «Annulla»; i bersagli da 48dp. L'hero resta
   una card: la pagina non ha un cielo da stendere fino al bordo.
8. **Le ore sopra l'interruttore restano `titleMedium`.** Sul Cielo e su Oggi l'ora è ora in
   una colonna all'inizio della riga, `labelLarge` e quieta; qui è una fascia («6–12») e sta
   sopra il suo interruttore. Spostarla è un disegno nuovo, non un allineamento.
9. **Un workflow che nessuno aveva chiesto.** La richiesta era di non compilare sulla
   macchina del committente; la regola del README vuole le immagini rigenerate nella stessa
   modifica e guardate prima del commit, e una revisione grafica mai disegnata resta una
   lettura del codice. Le due cose stavano insieme solo disegnando altrove. Il workflow è un
   file, non tocca la CI che c'è, non scrive nel repository e non parte su `main`; se non
   serve, si toglie il file. Come la misura degli stati parte a un push che lo tocca; quella
   committa sul ramo, questo no.

### Trovato e non toccato

- **`widgets.png` e `widget-window.png` escono diverse sul runner** da quelle committate, senza
  che i widget siano stati toccati: è la casella già aperta («Le immagini dei widget nel README
  dipendono dal fuso della macchina»). Lasciate com'erano su `main`.
- **Il ritmo del Cielo non è quello di §8.0.** Sopra «In arrivo» ci sono 17dp e sotto 5,
  dove Oggi (e ora Avvisi) ha 24 e 12: `SkySectionTitle` spende ancora 16 e 4. Non è la
  schermata di questa revisione, e cambia due immagini del README. Proposta, casella aperta.
- **Le ore delle fasce con l'orologio a 12 ore.** «6–12», «15–17», «18–23», le ore sotto la
  striscia e «Dalle 22 alle 7» sono ore intere di un giorno di ventiquattro, qualunque sia
  l'orologio del telefono; ogni altra ora della pagina lo segue. DESIGN §13, voce 7.
- **DESIGN §6 dice ancora `ListItem`** per i momenti del Cielo e le righe di Impostazioni:
  è vero dei fogli, non più di quelle righe. Corretta la parte di Avvisi soltanto.
- **Il selettore dell'ora di una fascia** è un `TimePicker` dentro un `AlertDialog`: su uno
  schermo basso o di lato il quadrante può non starci. Material 3 ha un `TimePickerDialog`
  che sceglie da sé fra quadrante e campi; non provato, non toccato.
- **`TabHeader` non riceve più un luogo da nessuna schermata**: la riga del luogo e il suo
  bersaglio restano nel componente, e li disegna solo `SkyUndoTest`.

### Proposte, non decise

1. **La barra di Avvisi quando la risposta è scorsa via**: su Oggi la barra prende la
   temperatura, sul Cielo la parola della notte; qui il segno e la parola del livello mentre
   un'allerta è in vigore, niente in un giorno quieto. DESIGN §13, voce 6.
2. **Il ritmo del Cielo a 24 e 12**, qui sopra.
3. **Le fasce nell'orologio del telefono**, qui sopra.
4. **Lo storico di un avviso**: la card dice solo l'ultima volta che è scattato, e lo storico
   tiene tre giorni; nell'editor potrebbero stare le ultime volte, con l'ora e il numero.

### Come è stato verificato

- **Non in locale**, come chiesto: né la compilazione, né i test, né lint, né un disegno.
- **Letto**: il diff riga per riga contro le firme che chiama (`PlaceHeader`, `Segment`,
  `SectionTitle`, `toggleable`, `RuleVariables.displayId`); ogni simbolo tolto cercato in tutto
  il repository (`firstInView` e `measureFirstLeft` avevano un solo chiamante, `GroupTitle` e
  `PlaceholderPickerDialog` solo questo file); i test esistenti che toccano la pagina riletti
  (`AlertsPlaceTest` prende l'ultimo nodo `isToggleable`, che resta l'interruttore dell'avviso;
  `ReadmeScreenshots` trova l'intestazione dall'ingranaggio, che `PlaceHeader` ha).
- **I test, scritti senza lanciarli qui**: `AlertsLikeTodayTest`, nuovo (il luogo apre
  l'intestazione; «Aggiungi» è nel titolo, sulla sua riga, un bersaglio da 48dp, e apre le
  idee; 24 sopra e 12 sotto un titolo; una riga pronta è un interruttore acceso e il tocco la
  spegne; l'interruttore di un avviso porta il suo nome); `AlertsYoursEmptyTest`, adattato al
  pulsante nel titolo.
- **La prima CI: rossa per un test, non per il codice.** Ha compilato tutto e, dei 1 188 test
  dell'app, ne ha fallito uno, dei miei nuovi: «the place leads the header», che voleva la
  parola «Alerts» assente e la cercava nell'albero non unito, dove resta quel che la
  descrizione di un blocco sostituisce. L'albero che legge TalkBack è quello unito: lì la
  cerca ora. Le due misure (24 e 13dp) erano verdi alla prima. **La seconda CI è verde**
  (8ad3d944): i test di tutti i moduli, lint, l'APK di debug e quello di release.
- **Le immagini, guardate**: le sedici del README disegnate sul runner dal ramo. Le dodici di
  Oggi, del Cielo, delle Impostazioni, della guida e della notifica sono **identiche al byte**
  a quelle committate, quindi quel che è condiviso (`TitleAction`, `SectionTitle`, `ListRow`,
  `TimelineRow`, `PaneList`) non ha spostato niente fuori da Avvisi. Le due di Avvisi
  mostrano quel che doveva cambiare: «Milan ⌄» e «Alerts» sotto, «+ Add» sulla riga di
  «Yours», e sull'immagine 24dp sopra «Official warnings» e «Yours», 12 e 13 sotto.
- **Mai visto**: il carattere grande, il tema scuro, la pagina larga a due colonne, i fogli,
  TalkBack. Stanno nella casella del telefono.

### Cosa conferma solo il telefono

Casella «La revisione di Avvisi sul telefono»: l'intestazione nel passaggio fra le tre schede
(il luogo fermo, la seconda riga che cambia); «Aggiungi» accanto a «I tuoi», anche con il
carattere grande e con la nota dell'età; le righe con 12dp d'aria e il disegno a 34; «I tuoi»
vuoto nel suo segmento; TalkBack sulle righe pronte, sugli interruttori degli avvisi e su
quelli di Impostazioni; la striscia del giorno al 200% di carattere; il foglio «Un valore nel
messaggio» aperto sopra l'editor, e il valore che finisce dove sta il cursore.

## Avvisi: le tre scelte confermate, le ore sull'orologio del telefono, il selettore dell'ora (committente, 12 nov 2026)

Stessa chat, la PR #165 ancora aperta e verde. Il committente: «Confermo le tue tre scelte.
Sistema le due cose indicate in "Trovato e non toccato" ma prima spiegami meglio "Il selettore
dell'ora di una fascia è un quadrante dentro una finestra: su uno schermo basso potrebbe non
starci. Non provato."».

- **Confermate le tre scelte** della sezione qui sopra («Decisioni e deviazioni», 1, 3 e 9):
  l'intestazione con il luogo in testa, «Aggiungi» sul pulsante, il workflow che disegna le
  immagini del README.
- **Le due cose** sono le ore delle fasce con l'orologio a 12 ore e il selettore dell'ora di
  una fascia, fatte nella stessa PR. Il ritmo del Cielo e la barra di Avvisi, che erano una
  domanda e una proposta, restano caselle aperte: non ne ha parlato.
- **Di nuovo niente compilato in locale**: la CI e il runner delle immagini.

### La spiegazione chiesta, in breve

Il selettore era il quadrante di Material 3 dentro una finestra generica. Con i numeri della
libreria (Material 3 1.4, letti dal suo archivio): il quadrante vuole fra 316 e 384dp di
altezza, titolo, margini e pulsanti della finestra circa 160 in più; un telefono di lato o una
finestra a metà schermo ne ha 360–410 in tutto, e la finestra generica non scorre. In più il
quadrante era l'unico modo per dire un'ora, dove Material chiede accanto i due campi da
tastiera. Era un conto sulle misure, mai visto disegnato.

### Cosa è cambiato

1. **Le ore delle fasce seguono l'orologio del telefono** (`AlertHours`, nuovo, puro): sopra
   gli interruttori e nella fascia in testa «3–5 PM» dove un telefono a 12 ore leggeva
   «15–17»; le ore sotto la striscia; la frase delle ore silenziose; «Di solito tra le…» sotto
   l'interruttore delle allerte; «il prossimo esce di solito entro le…» nella card con un
   bollettino vecchio; quel che TalkBack legge della striscia; il capitolo Avvisi della guida.
2. **Il selettore dell'ora è la finestra di Material per un'ora** (`TimePickerDialog`): il
   quadrante con il pulsante che lo cambia in due campi, e solo i campi sotto la soglia della
   libreria per il quadrante (300dp di finestra). Titolo, orologio del telefono e «OK» rifiutato
   sull'ora dell'altro capo restano com'erano.
3. **I documenti**: DESIGN §8.9b (due punti in «In Today's shape»), §13 senza la voce 7;
   CHANGELOG; il workflow delle immagini dice come leggere il suo artefatto.

### Decisioni e deviazioni

1. **«3–5 PM», la metà del giorno detta una volta** quando i due capi la condividono, e «6
   AM–12 PM» quando no. È la forma più corta che non si può leggere male, e sopra
   l'interruttore sta ancora a `titleMedium`; la più larga è sugli 86dp (un conto, non una
   misura), tolti alla descrizione di quella riga sola. Il segno è quello della lingua del
   telefono, non una stringa dell'app.
2. **Tre ore sotto la striscia dove cinque si toccherebbero**: «12 AM», «6 AM», «12 PM» sono
   più larghe di «0», «6», «12», e con un carattere grande cinque si sovrappongono. Si misura
   mentre si disegna, con 4dp almeno fra una e l'altra.
3. **A 24 ore non cambia niente, tranne due frasi italiane**: «tra le 15:00 e le 17:00» e
   «entro le 16:00», che dicevano «le 15 e le 17» e «le 16» dove l'inglese diceva già «15:00» e
   «16:00». Una stringa sola per le due lingue prende l'ora come ora d'orologio. Le quattordici
   immagini delle schermate del README sono uscite identiche al byte.
4. **La guida toccata**, anche se la casella parlava della pagina: il suo capitolo diceva
   «tra le 18 e le 23» a chi sul telefono legge 6 PM. Due stringhe hanno ora argomenti
   (`formatted="false"` tolto, il «70%» raddoppiato).
5. **La soglia del quadrante è della libreria**, non un numero nostro: se Material la sposta,
   la finestra la segue.
6. **`BandTimeDialog` è `internal`**, per poterlo provare da solo: l'editor tiene il view
   model e non si disegna in un test.
7. **Le ore silenziose e l'ora del bollettino sono costanti della pagina** (`QuietFrom`,
   `QuietTo`, `BulletinBy`): erano scritte nelle frasi e nel disegno.

### Trovato e non toccato

- **Le notifiche e i widget non sono stati guardati per l'orologio a 12 ore**: questa
  revisione ha cercato le ore intere scritte nelle stringhe della pagina e della guida, e non
  ne restano; il testo delle notifiche e delle card non è stato riletto con questa domanda.

### Come è stato verificato

- **La CI, tre corse.** La prima ha compilato l'app e si è fermata su una riga di un test mio
  (un aiutante a un argomento chiamato con tre). La seconda (cb0581f9) è verde: i test nuovi
  compresi, lint, le due build. La terza è quella di questo commit.
- **I test nuovi**: `AlertHoursTest` (la forma delle ore sui due orologi);
  `BandTimeDialogTest` (in una finestra alta il quadrante, e il pulsante lo cambia in campi; in
  una finestra di 280dp solo i campi, con i due pulsanti a schermo; «OK» spento sull'ora
  dell'altro capo); in `AlertsLikeTodayTest`, la pagina con l'orologio a 12 ore;
  `AlertsPromiseTest` cerca le ore del bollettino nella forma che la pagina stampa.
- **Le immagini**: ridisegnate sul runner dopo le due correzioni, le quattordici delle
  schermate identiche al byte a quelle committate.
- **Mai visti**: il selettore disegnato (né il quadrante né i campi, né di lato), la pagina con
  l'orologio a 12 ore, le tre ore sotto la striscia. Stanno nella casella del telefono.

### Cosa conferma solo il telefono

- [ ] **Il primo avvio sul telefono** (registro, «Il primo avvio, rivisto con quelli della
  famiglia», 12 nov 2026). Su un'installazione nuova, o dopo aver cancellato i dati dell'app: il
  benvenuto e «Iniziamo»; il luogo e il gesto indietro che torna al benvenuto; la posizione negata
  una volta e poi per sempre (la riga sotto le parole, «Apri le impostazioni»); le notifiche; gli a
  capo in italiano e in inglese con il carattere grande; lo scorrimento avanti e indietro, e la sola
  dissolvenza con le animazioni tolte.
Nella casella «La revisione di Avvisi sul telefono», in più: con l'orologio a 12 ore, le ore
sopra gli interruttori e nella card in testa, le ore sotto la striscia (cinque, e tre con il
carattere grande), la frase delle ore silenziose, il capitolo Avvisi della guida; il selettore
dell'ora di una fascia in piedi e con il telefono di lato, il pulsante che passa ai due campi,
l'ora scritta da tastiera.

## Le prove sul telefono della coda della Fase 16 (committente, 12 nov 2026)

Stessa chat, prima di unire la PR #165: «Ho fatto tutti i test sul telefono. Chiudi eventuali
punti aperti su test del telefono relativi alla fase 16 conclusa in precedenza».

- **Chiusa «La coda della Fase 16 sul telefono»**, l'unica casella del telefono della Fase 16
  rimasta aperta: provata, lo dice lui. Con questa la Fase 16 non ha più niente di aperto sul
  telefono; le sue altre caselle erano già chiuse (16.1–16.11).
- **Lasciate aperte le altre tre caselle del telefono**, perché la richiesta nominava la Fase
  16 e queste non lo sono: «La revisione di Oggi e del cielo sul telefono» e «L'hero dopo la
  revisione, sul telefono» (9 e 10 nov 2026, prima della Fase 16; la prima chiede anche una
  cifra della GPU e un giorno di nebbia o di temporale), e «La revisione di Avvisi sul
  telefono», di questa PR. Chiesto al committente se «tutti i test» le comprende.
- Solo questo file: nessun codice toccato.

## Le prove sul telefono dell'hero e di Avvisi (committente, 12 nov 2026)

Alla domanda se «tutti i test» comprendeva le tre caselle del telefono rimaste, il committente:
«Lascia aperta solo «La revisione di Oggi e del cielo sul telefono»».

- **Chiuse «L'hero dopo la revisione, sul telefono» e «La revisione di Avvisi sul telefono»**:
  provate, lo dice lui. La seconda comprende le prove aggiunte dalle due correzioni dello
  stesso giorno (l'orologio a 12 ore, il selettore dell'ora).
- **Resta aperta «La revisione di Oggi e del cielo sul telefono»**, per sua scelta: è quella
  che chiede la cifra della GPU, il nastro sotto il dito, e la nebbia e il lampo in un giorno
  che li ha.
- Solo questo file: nessun codice toccato.

## Il primo avvio, rivisto con quelli della famiglia (committente, 12 nov 2026)

**La richiesta.** «Fai una review delle schermate di onboarding di Chiaro»: i testi coerenti con
l'app di oggi; le schermate che mancano per presentarla a chi la installa, nei limiti delle linee
guida (quante pagine, cosa dicono); grafica, layout, tipografia, righe senza parole sole e
animazione tra le pagine allineate a Passo e Tempo, come app dello stesso ecosistema.

**Cosa ho trovato.**

- **La cornice era già quella della famiglia** (archivio 05, «Gli onboarding della famiglia, resi
  uniformi», 18 ott 2026): le parole centrate sopra i pulsanti, i pulsanti in basso a 52 dp, la
  stessa scala di tipi, lo scorrimento di 300 ms. Da allora Passo e Tempo sono andati avanti (12 nov
  2026): si aprono su un benvenuto (il nome, la frase, tre promesse) e solo dopo chiedono; i pulsanti
  dicono «Iniziamo», «Continua», «Più tardi»; ogni riga centrata è bilanciata (`LineBreak.Heading`),
  mai una parola sola in fondo; si muovono solo le parole, nei due versi. Chiaro si apriva sulla
  domanda (il nome e la frase sopra «Usa la mia posizione», niente che dicesse cosa fa l'app prima di
  chiedere un permesso), diceva «Non ora», andava a capo come capitava («…prima che un'app possa
  mostrare una / notifica.»), e muoveva l'intera pagina, pulsanti compresi, solo in avanti.
- **Un testo non era vero.** Le notifiche dicevano «Maltempo in arrivo, pioggia nelle prossime ore,
  allerte ufficiali e i tuoi avvisi sono già attivi»: un'installazione nuova non ha avvisi suoi, e i
  due riepiloghi, accesi di serie dal 21 set 2026 (`NotificationSettings`), la frase li taceva. Le
  altre righe tengono: la posizione arrotondata a circa un chilometro (la guida, «La mia posizione»),
  «Puoi cambiare idea quando vuoi, dalla schermata Avvisi» (la card di DESIGN §8.14).

**Cosa è cambiato** (VISION §5.8, riscritto).

1. **Il benvenuto, una pagina sua** (passaggio 1 di 3), che non chiede niente: il nome
   (`displaySmall`), la frase (`titleMedium`) e tre promesse, titolo `titleMedium` e testo
   `bodyMedium`, a 36 dp dalla frase e a 20 tra loro come nelle due app. «Prima una frase, poi i
   numeri» (Oggi); «I momenti del cielo» (l'ora d'oro, la luna, i pianeti, le stelle cadenti, ognuno
   con il suo verdetto, e il cielo della notte da esplorare con un dito); «Gratis, senza account»
   (niente pubblicità, nessun tracciamento, il meteo da Open-Meteo, il cielo calcolato sul telefono
   anche senza connessione). Non «resta sul telefono» come in Passo: i luoghi vanno nel backup di
   Android (`data_extraction_rules.xml`) e le coordinate a Open-Meteo, e una promessa deve essere
   vera alla lettera. Gli avvisi non sono una promessa del benvenuto perché hanno la loro pagina due
   passi dopo. Il benvenuto non si salva: chi riapre l'app dopo che il processo è morto riparte da
   lì, e costa un tocco.
2. **Perché tre pagine e non quattro.** Le linee guida (Material: pochi schermi, il valore prima, i
   permessi chiesti nel contesto e dopo averli spiegati; la famiglia: al massimo quattro pagine, solo
   parole) vogliono il minimo che mette l'app in mano. Il luogo serve a tutto; le notifiche servono ai
   cinque interruttori accesi di serie; il resto (il Cielo, i widget, la guida) si trova dove sta, e
   Oggi offre la guida con la sua card una tantum. Una pagina dei widget come quella di Tempo non
   c'è: lì il widget è il prodotto, qui un'aggiunta, che la guida racconta nel suo capitolo.
3. **Il luogo, una pagina come le altre** (2 di 3): il titolo «Il tuo luogo» e un testo che dice
   perché serve un luogo e da dove se ne aggiungono altri; i pulsanti di prima. Il rifiuto della
   posizione non sta più tra i pulsanti, dove li spingeva in su, ma sotto le parole e nel loro
   inchiostro quieto, con la strada (`first_run_location_refused`): «Senza il permesso, Chiaro non sa
   dove sei. Puoi cercare una città, o consentirlo più tardi dai tuoi luoghi.» I guasti (posizione
   spenta, nessun segnale) restano nel colore dell'errore, con le parole di sempre. Il pulsante che
   aspetta la posizione lo dice a TalkBack («Sto cercando la tua posizione…»): era un cerchio muto.
4. **Le notifiche** (3 di 3) nominano quel che è acceso davvero: «Il riepilogo del mattino e quello
   della sera, il maltempo, la pioggia in arrivo e, in Italia, le allerte ufficiali sono già accesi.»
5. **I pulsanti come nelle due app**: «Iniziamo» / «Get started» sul benvenuto; «Più tardi» /
   «Later» al posto di «Non ora» / «Not now», sul luogo e sulle notifiche (`first_run_later`, uno per
   i due). I pulsanti che chiedono restano quelli di Chiaro, che dicono cosa fanno («Usa la mia
   posizione», «Consenti le notifiche»), dove Passo e Tempo dicono «Consenti».
6. **Le righe bilanciate**: `balanced()` in `ui/theme/Type.kt` (`LineBreak.Heading`), su ogni testo
   centrato del primo avvio: i titoli, la frase, le promesse, i testi, la riga sotto il pulsante, il
   rifiuto. Solo il primo avvio, come nelle altre app: il resto tiene i suoi a capo finché una
   revisione sua non lo guarda.
7. **Il movimento**: si muovono solo le parole, i pulsanti cambiano sul posto; avanti la pagina
   scorre di un sesto da destra, indietro da sinistra, 300 ms con la dissolvenza; 100 ms di sola
   dissolvenza con l'animazione ridotta. Il ritorno è quello del sistema, dal luogo al benvenuto; le
   notifiche non tornano al luogo, che è già risposto e salvato e ha la sua strada nel foglio dei
   luoghi (indietro, lì, lascia l'app come prima). In `ChiaroRoot` le due fasi del negozio
   (`Pending`, `Notifications`) sono un ramo solo, perché la pagina non si ridisegni intera; il resto
   (la prima risposta del negozio, la fine del percorso) è un taglio, come prima.

**Come è stato verificato.** `FirstRunScreenTest` (nuovo, in `testDebug`): le tre pagine con il
passaggio sul titolo, i pulsanti e a chi rispondono, il ritorno dal luogo al benvenuto e nessun
ritorno dalle notifiche, il rifiuto con il pulsante delle impostazioni, il pulsante che aspetta, e
nessuna parola sola in fondo a una riga a 360 dp, misurata con il carattere dell'app, in italiano e
in inglese (con `CHIARO_RENDER_DIR` salva le pagine). Le pagine rese (chiare e scure, IT e EN)
guardate accanto ai benvenuti di Passo e Tempo: stessa gerarchia, stessi spazi, stessi pulsanti, il
colore di Chiaro. Suite completa (3 395 test, nessuno rosso) e lint, nessun avviso sui file toccati. Nessuno screenshot del README mostra il primo avvio, e non ne
aggiungo uno: un benvenuto di sole parole non dice di Chiaro più di quanto il README dica già.
`CHANGELOG.md` [Unreleased], DESIGN §5 (la riga di `displaySmall`). Da provare sul telefono: la
casella in «Aperto».
