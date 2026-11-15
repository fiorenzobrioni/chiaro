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
  lasciato e i gradi a schermo sono chiusi lo stesso giorno, archivio 14, «La coda della Fase 16,
  i gradi, e le prove del 16.11»). La revisione, le tavole approvate, le decisioni, le regole comuni e la
  descrizione di ogni step sono in `docs/planning/11-novembre-2-10.md`, «Fase 16 — Cielo: la pagina
  come Oggi e il cielo da esplorare», con i primi due step chiusi (16.1, 16.2); gli step dal 16.3
  al 16.7 sono in `docs/planning/12-novembre-10-11.md`, quelli dal 16.8 al 16.10 in
  `docs/planning/13-novembre-12.md`; la chiusura (16.11) è nell'archivio 14 (`docs/planning/14-novembre-12-14.md`).
- **Aperta il 15 nov 2026**: **Fase 17**, «In immagini», il widget disegnato: quattro step, uno per chat, sul canvas approvato e la regola della fedeltà (registro, «Fase 17 — «In immagini», il widget disegnato»).
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
  committente sul telefono il 12 nov 2026 (archivio 14, «Le prove sul telefono dell'hero e di
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
  al foglio, il carattere grande, la luna toccata. Fatto: dato per provato dal committente il 12 nov 2026 (archivio 14, «16.11», «Le conferme, e le prove sul telefono date per fatte»).
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
  riga), la pagina che si apre sulla notte di un evento. Fatto: dato per provato dal committente il 12 nov 2026 (archivio 14, «16.11», «Le conferme, e le prove sul telefono date per fatte»).
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
  le decisioni 2, 3 e 6 confermate alla PR #162 (archivio 14, «16.11», «Le conferme, e le prove
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
- [x] **16.11 sul telefono** (archivio 14, «16.11», «Cosa conferma solo il telefono»): il pizzico con
  «Punta il telefono» acceso alzando un dito prima dell'altro, un pianeta basso toccato che sta
  sopra il suo foglio, la riga di uno sciame nella sua notte, il capitolo del Cielo della guida
  con il carattere grande. Fatto: provato dal committente sul telefono il 12 nov 2026 (archivio 14, «La coda della Fase 16, i gradi, e le prove del 16.11»).
- [x] **La coda della Fase 16** (archivio 14, «16.11», «Trovato e non toccato»): quel che la
  rilettura ha trovato e non ha toccato. I tocchi e i trascinamenti sotto le fasce dei comandi
  dell'esploratore; una riga dei momenti con «?» e senza il motivo; la percentuale della luna
  nel distintivo al 72%; una notte passata senza previsione che riceve «Non si sa ancora»;
  codice senza chi lo chiama (`sampleAt`, `SkyBody.target`); il vocabolario di prima nei
  commenti del motore (`sky.crontab`, `VISION_SKY.md`, «Fase 16b/16d»). Proposta, non decisa:
  uno step di coda, come il 15.13. Fatto il 12 nov 2026, su richiesta del committente nella
  stessa chat (archivio 14, «La coda della Fase 16, i gradi, e le prove del 16.11»): **scritto
  senza compilare e senza lanciare i test, la CI è la prima a provarlo.**
- [x] **La coda della Fase 16 sul telefono** (archivio 14, «La coda della Fase 16, i gradi, e le
  prove del 16.11», «Cosa conferma solo il telefono»): un tocco accanto ai due pulsanti con un
  foglio aperto, un dito che si muove sul pannello dell'ora, la riga di una congiunzione in
  lune piene, la luna nel distintivo di un luogo senza coordinate. Fatto: provato dal
  committente sul telefono il 12 nov 2026 (archivio 14, «Le prove sul telefono della coda della
  Fase 16»).
- [x] **I gradi che restano a schermo nel Cielo** (archivio 14, «16.11», «Trovato e non toccato»).
  La Fase 16 ha deciso «niente gradi» e le sue superfici non ne hanno; restano da prima «N° di
  distanza» sulle righe delle congiunzioni in «In arrivo» e i gradi nelle pagine degli eventi.
  Da decidere: estendere la regola (le frasi a parole) o scrivere in DESIGN che lì restano.
  Chiusa il 12 nov 2026 con tutte e due, ognuna dove è vera: la riga dice la distanza in lune
  piene, le pagine tengono i gradi con cui definiscono un crepuscolo, e DESIGN §8.8 lo scrive
  (archivio 14, «La coda della Fase 16, i gradi, e le prove del 16.11»).
- [x] **La revisione di Avvisi sul telefono** (archivio 14, «Avvisi come Oggi e Cielo: la revisione
  della schermata», «Cosa conferma solo il telefono»): l'intestazione nel passaggio fra le tre
  schede, «Aggiungi» accanto a «I tuoi» anche con il carattere grande, le righe con la loro
  aria nuova, «I tuoi» vuoto nel suo segmento, TalkBack sugli interruttori di Avvisi e di
  Impostazioni, la striscia del giorno al 200%, il foglio «Un valore nel messaggio». E, dalla
  sezione dopo («Avvisi: le tre scelte confermate…», «Cosa conferma solo il telefono»): la
  pagina e la guida con l'orologio a 12 ore, il selettore dell'ora in piedi e di lato. Fatto:
  provato dal committente sul telefono il 12 nov 2026 (archivio 14, «Le prove sul telefono
  dell'hero e di Avvisi»).
- [ ] **Il ritmo delle sezioni del Cielo** (stessa sezione, «Trovato e non toccato»): 17dp sopra
  un titolo e 5 sotto, dove Oggi e Avvisi hanno i 24 e 12 di DESIGN §8.0. Proposta, non decisa:
  `SkySectionTitle` sui numeri di Oggi, e le due immagini del Cielo rigenerate.
- [ ] **La barra di Avvisi quando la risposta è scorsa via** (stessa sezione, «Proposte, non
  decise»; DESIGN §13, voce 6): il segno e la parola del livello mentre un'allerta è in vigore.
  Proposta, non decisa.
- [x] **Le fasce di Avvisi con l'orologio a 12 ore** (stessa sezione, «Trovato e non toccato»):
  «18–23» accanto a «alle 2:34 PM». Fatto il 12 nov 2026, nella stessa PR, su richiesta del
  committente, con il selettore dell'ora di una fascia (archivio 14, «Avvisi: le tre scelte
  confermate, le ore sull'orologio del telefono, il selettore dell'ora»).
- [ ] **Il primo avvio sul telefono** (archivio 14, «Il primo avvio, rivisto con quelli della
  famiglia», 12 nov 2026; la casella era rimasta nel registro, sotto «Avvisi: le tre scelte
  confermate…», «Cosa conferma solo il telefono», e passa qui all'archivio). Su un'installazione
  nuova, o dopo aver cancellato i dati dell'app: il benvenuto e «Iniziamo»; il luogo e il gesto
  indietro che torna al benvenuto; la posizione negata una volta e poi per sempre; le notifiche;
  gli a capo in italiano e in inglese con il carattere grande; lo scorrimento avanti e indietro, e
  la sola dissolvenza con le animazioni tolte.
- [ ] **I widget del 14 nov 2026 sul telefono** (archivio 14, «I widget: la massima e la minima a una
  riga, la nuvola del coperto, la posizione fissata»): la 4×1 con «Massima e minima di oggi» accesa,
  la frase ancora leggibile a due righe sul launcher del committente; la nuvola del coperto accanto
  alle altre; un widget fissato a «La mia posizione» con l'app su un'altra località, anche dopo uno
  spostamento.
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
- [x] **17.1** Le misure e il motore di «In immagini» (registro, «Fase 17 — «In immagini», il widget disegnato»). Fatto il 15 nov 2026 (registro, «17.1»): la geometria in `domain/pictures`, le otto forme in `InPicturesLayout`, i test contro i numeri del canvas; nove deviazioni scritte, tre domande.
- [x] **Le tre domande del 17.1** (registro, «17.1», «Domande al committente»): la scala della curva (3,6 dp per grado si stringe nel 55% delle giornate d'autunno misurate; proposta 2,5), la 4×3 a 115 e 130% di testo (il luogo o le sei ore), il posto della parola dell'avviso ufficiale (proposta: quello del luogo). Chiuse il 15 nov 2026 (registro, «Le tre domande del 17.1, chiuse»): 2,5 dp per grado, l'ordine del canvas, il chip nella riga del luogo.
- [ ] **17.2** Il disegno: la lastra, il quadrante, la card in ogni forma (stessa sezione).
- [ ] **17.3** Il widget: il receiver, il provider, le impostazioni (stessa sezione).
- [ ] **17.4** Il confronto con il canvas, forma per forma, e la chiusura (stessa sezione).
- [ ] **«In immagini» sul telefono** (stessa sezione): le otto forme ridimensionando sul launcher del
  committente, accanto a «In parole»; il cielo di giorno, all'ora d'oro e di notte; la pioggia in
  arrivo; i sei colori; il 130% di testo; TalkBack sulla lastra.

Riletto il 15 nov 2026, all'archivio 14, all'inizio del 17.1: le caselle nate dal 12 nov 2026 rimandano all'archivio 14 (`docs/planning/14-novembre-12-14.md`), e quella del primo avvio sul telefono, rimasta dentro il registro, è passata qui; la descrizione della Fase 17 resta nel registro (17.1–17.4 aperti); restano aperte le prove sul telefono (la revisione di Oggi, il primo avvio, i widget del 14 nov 2026, «In immagini»), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; quattro proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, il fuso della macchina nelle immagini dei widget del README, il ritmo delle sezioni del Cielo e la barra di Avvisi quando la risposta è scorsa via. Riletto il 12 nov 2026, all'archivio 13, all'inizio del 16.11: le caselle nate dal 12 nov 2026 rimandano all'archivio 13 (`docs/planning/13-novembre-12.md`), la descrizione degli step della Fase 16 resta nell'archivio 11 (16.11 aperto: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.8, 16.9, e 16.10 che decide se «Punta il telefono» si tiene), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; due proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, e il fuso della macchina nelle immagini dei widget del README. Riletto il 12 nov 2026, all'archivio 12, all'inizio del 16.8: le caselle nate dal 10 nov 2026 rimandano all'archivio 12 (`docs/planning/12-novembre-10-11.md`), la descrizione degli step della Fase 16 resta nell'archivio 11 (16.8–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, le due caselle del 16.4, 16.5, 16.6, 16.7, la notte bianca), il peso della cifra e «Costellazioni» accesa da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; due proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, e il fuso della macchina nelle immagini dei widget del README. Riletto il 10 nov 2026, all'archivio 11, all'inizio del 16.3: le caselle nate dal 2 nov 2026 rimandano all'archivio 11 (`docs/planning/11-novembre-2-10.md`), dove sta anche la descrizione degli step della Fase 16 (16.3–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.1, 16.2), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; una proposta non decisa, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo. Riletto il 2 nov 2026, all'archivio 10, con la 2.0.0: le caselle chiuse dal 29 ott 2026 (15.13, 15.14) rimandano all'archivio 10 (`docs/planning/10-verso-2.0.0.md`); restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, dopo la rilettura della Fase 15: aperte 15.13 e 15.14, la coda; e come prima 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, all'archivio 09: le caselle della Fase 15 sono tutte chiuse (in `docs/planning/09-ottobre-28-29.md`), restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 28 ott 2026, all'archivio 08, lo stesso giorno dell'apertura della Fase 15: le caselle sopra sono della Fase 14 (14.12 in attesa) e della Fase 15 (in `docs/planning/08-ottobre-27-28.md`), nessuna proposta non decisa. Riletto il 27 ott 2026, all'archivio 07: le caselle della Fase 14, nessuna proposta non decisa. Fino al 26 ott 2026 nessuna casella aperta (riletto il 22 ott 2026, all'archivio 06):
le ultime chiuse quel giorno stanno in `docs/planning/06-ottobre-20-22.md`, «Cielo: la finestra
reale nella card Stanotte», «Il widget «Momenti del cielo» va via», «Le notifiche: ogni riga nella
forma che sta in una riga», «Il widget «Le prossime ore» va via».

---

## Registro dal 15 nov 2026, dopo l'archivio 14

## Fase 17 — «In immagini», il widget disegnato (committente, 15 nov 2026)

**La richiesta.** Una serie nuova di widget per la famiglia, da affiancare a «In parole»: un widget
grafico che commenta quello testuale, «accattivante ma pulito, di livello premium, con info utili,
layout e tipografia curata, colori brillanti e vivi, senza funzionalità che sono solo rumore»;
ridimensionabile, con layout adattivo e una schermata di configurazione. Prima Chiaro, poi Passo,
Tempo e Tetto sullo stesso stile.

**Il design approvato** è il canvas https://claude.ai/artifact/VFwVjbxyAyWcWRLcRW9uhY, pagina
«Chiaro» (le altre tre pagine sono le sorelle): cinque tavole (la serie, l'anatomia della 4×2, le
otto forme, le impostazioni interattive, i dodici fondi). I sorgenti delle quattro che servono al
codice sono in `docs/design/in-immagini/`, congelati a oggi, con un README che dice come si leggono.
Nome: **«In immagini»** / **«In pictures»**, il gemello disegnato di «In parole»: dove l'altro scrive
la giornata, questo la disegna (la temperatura come una curva, la pioggia come barre, la luce come
un nastro), con gli stessi fatti e la stessa frase.

### La regola della fedeltà (committente, 15 nov 2026)

«Vorrei che sia specificato che i layout e la grafica dei nuovi widget siano fedeli (il più
possibile) a quelli dei canvas creati qui»: in passato un'implementazione partita da un design se
n'è allontanata, e graficamente non è venuta bene. Quindi la fedeltà è un **criterio di
accettazione**, non un'intenzione:

1. **I numeri qui sotto sono il contratto**: misure in dp, corpi in sp, inchiostri, raggi,
   posizioni. Le funzioni pure del layout li riproducono e i test li fissano alle grant di
   riferimento (85/189/293/397 dp d'altezza, 159/250/340 di larghezza). Dove il piano non scrive una
   misura, vale quella del sorgente in `docs/design/in-immagini/`.
2. **Una deviazione è ammessa solo se la impone la piattaforma** (Glance e `RemoteViews`: niente
   maschere né tracciati, la faccia di sistema del launcher, undici figli per contenitore, la
   memoria delle bitmap) **o una regola misurata** di questo repository (il contrasto, l'inchiostro
   della famiglia, lo scrim di §3.6). Ognuna si scrive in questo registro, nello step che la fa,
   con il motivo e la soluzione più vicina al canvas. «Sembrava meglio così» non è un motivo: si
   chiede prima al committente.
3. **L'ultimo step confronta a vista**: la galleria del widget accanto alla tavola del canvas, forma
   per forma e alla stessa scala, e l'elenco di quello che differisce, ciascuna differenza con la sua
   riga di registro o corretta. Lo step non si chiude con una differenza non scritta.

Due deviazioni sono già decise, perché il canvas le ha prese da regole che il codice ha più
precise: **l'inchiostro quieto** è il 75% di §2.6 (il canvas usa 78% su alcune righe) e **il fondo
cielo** è il cielo vero di quest'ora con lo scrim di §3.6 (il canvas ha un gradiente fisso delle 14).

### Le decisioni del committente (15 nov 2026)

Le cinque proposte della revisione del canvas, accettate tutte:

1. **Il fondo predefinito è il colore dell'app**: per Chiaro il cielo di quest'ora (come la card
   «Colpo d'occhio»), per le sorelle il loro colore d'icona scurito finché l'inchiostro tiene.
2. (Tetto) Le colonne dei tetti non sono un grafico: solo il periodo aperto, adesso.
3. (Passo) «In immagini» e «In parole» non si doppiano: la curva contro il solito da una parte, il
   cerchio con le barre dall'altra.
4. (Passo, Tempo) Due contenuti nuovi delle sorelle (la frase corta della riga di Passo, i prossimi
   giorni di Tempo): approvati.
5. (Tetto) Sui fondi chiaro e scuro l'avanti sui giorni lo dice la forma, non il colore.

Per Chiaro contano la 1 e la regola che le sta sotto: il widget si legge prima per forma e parola,
poi per colore (CLAUDE.md, «A verdict ships with its arithmetic»).

### Le otto forme

Margini della card: 14 dp (12 sulle forme strette 2×2 e 2×3; 10/12 sulla 2×1; 12/16 sulla 4×1);
angolo del launcher (24 dp); la **lastra** è un riquadro dentro la card con l'angolo interno del
launcher (16 dp), un velo `rgba(6,24,52,0.30)` sul cielo (`rgba(0,0,0,0.18)` sui sei colori, sulla
carta e sull'inchiostro il loro scuro al 5%) e un filo interno bianco al 10%.

| Forma | Dimensioni | Cosa porta |
|---|---|---|
| 2×1 | 159 × 85 | Il numero (30 sp Bold) e la frase corta (13 sp Medium) a sinistra; il **quadrante** a destra, 52 dp, anello 6 dp |
| 4×1 | 340 × 85 | Il numero (36 sp); la frase (16 Medium) e il luogo (13, quieto) sulla stessa riga; sotto, **dodici ore di nastro** (6 dp) con la pioggia segnata sopra, il disco di adesso (14 dp) e le ore (11 sp: «ora», 17, 20, 23, 2) |
| 2×2 | 159 × 189 | Il quadrante, 124 dp, anello 9 dp, con il numero dentro (38 sp) e «↑12° ↓6°» (13); sotto la frase corta (13 Medium) e il luogo (11) |
| 3×2 | 250 × 189 | Testata da 54 dp: numero (44 sp) e frase (16 Medium); la lastra 226 × 100, le ore ogni sei |
| 4×2 | 340 × 189 | **La forma di riferimento** (l'anatomia del canvas): icona dell'ora 46 dp, numero 46 sp, frase 16 Medium a capo e mai troncata, «Milano · ↑12° ↓6°» 13 quieto, testata 54 dp, 8 dp, poi la lastra 312 × 100 (sotto) |
| 2×3 | 159 × 293 | Il quadrante, la frase intera (16), e «Più tardi» nella lastra: tre righe da 24 dp, tre ore l'una dall'altra (ora 13 quieto, icona 22, temperatura 13 Medium, probabilità 11 Semibold) |
| 4×3 | 340 × 293 | La 4×2, e sotto sei ore in griglia (ora 13, icona 28, temperatura 13 Medium, probabilità 11) |
| 4×4 | 340 × 397 | La 4×2, e cinque giorni, oggi primo, righe da 38 dp con filo: nome 40 dp, icona 26, probabilità 30, minima 24, barra dell'escursione (6 dp) su una **scala fissa da −5° a 30°** colorata da `temperatureAt` della minima a quello della massima, massima 26 |

**La lastra della 4×2**, dall'alto (y dentro la lastra, in dp):

- **La curva**, 6–50: bianca 2,4 dp, giunzioni rotonde, un'area sotto che sfuma dal 28% allo 0;
  **3,6 dp per grado, sempre** (la scala che si ancora al mondo, non allo schermo: una giornata
  piatta resta piatta); un'escursione che non ci sta stringe la scala fino a starci, e lo dice il
  registro dello step. Massima e minima sul loro punto, 13 sp Semibold (la massima 19 dp sopra, la
  minima 5 dp sotto), mai in una legenda. Il **disco di adesso** sulla curva, 12 dp bianco con un
  anello di 3 dp del velo al 35%.
- **La pioggia**, 50–68 (18 dp = 100%), filo al 14% sul fondo: una barra per ora **solo dal 20%**
  (`PROBABLE_PCT`), larga il 62% del passo, angoli 3/1 dp, alta la probabilità. Il **tono** dice
  l'intensità con le soglie del motore (STATES, 0,5 / 2,5 / 7,6 mm/h): quattro toni dello stesso blu,
  più chiaro più forte (sul cielo `#1E97CF`, `#3FB0E6`, `#6CCBF5`, `#A9E2FF` del canvas, da
  verificare contro la rampa di §2.3 e il velo, con il loro test di contrasto a 3:1 come segni).
  Le soglie del canvas (0,4 / 0,8 / 1,5) erano segnaposto: valgono quelle del motore.
- **Il nastro della luce**, 73–79 (6 dp, angoli 3): le fasi di §4 sulle 24 ore da adesso, con
  `ribbonStops` e i colori di mezzo di §3.2, le stesse funzioni dell'hero.
- **Le ore**, 83, ogni tre ore da «ora», 11 sp quiete.

**Il quadrante** (2×1, 2×2, 2×3): le 24 ore, mezzanotte in basso e mezzogiorno in alto; l'anello è
il nastro di oggi girato in cerchio (gli stessi colori, alba e tramonto veri del luogo); la parte
già passata, da mezzanotte ad adesso, sotto un velo del 42%; il disco di adesso sull'anello (14 dp,
anello 2,5); la pioggia delle prossime 24 ore come puntini di 5 dp, 16 dp dentro l'anello, opachi da
0,45 a 1 con la probabilità, solo dal 20%.

**L'icona dell'ora** è quella dell'app (Meteocons, dallo stato del motore, `shipped_icons.py`),
mai il disegno segnaposto del canvas.

### Le impostazioni

Dal kit di sempre (`WidgetConfigKit.kt`, DESIGN §8.16), nell'ordine del canvas: la card vera in
cima sul fondo `primaryContainer`→`tertiaryContainer`, i chip delle misure (4×2 «com'è ora», 2×2,
4×1, 4×4) con una riga per forma e il suggerimento del ridimensionamento; poi **Il disegno**
(segmenti: «Prossime 24 ore» / «La giornata», da mezzanotte, con la mattina passata più chiara),
**Sulla card** (il disegno dell'ora, la pioggia, il nastro della luce, i prossimi giorni, gli
avvisi ufficiali, ciascuno con la sua nota), **Sfondo** (`BackgroundSection`: il cielo di adesso
predefinito, chiaro, scuro, come il telefono, un colore), **Trasparenza**, il ripristino.

### Gli step, uno per chat

- **17.1 — Le misure e il motore.** Le funzioni pure delle otto forme (`InPicturesLayout`, sul modello
  di `TextWidgetLayout`), la geometria della lastra e del quadrante (scala, barre, nastro, ore, punti)
  in `:core:domain` dove non tocca risorse; i test alle grant di riferimento e a 100, 115 e 130% di
  testo. Il canvas e i suoi numeri letti prima di scrivere una riga.
- **17.2 — Il disegno.** Il pittore della lastra e del quadrante (bitmap alla misura vera, testi
  della lastra disegnati con la faccia di sistema), la card Glance in ogni forma, l'icona dell'ora,
  i fondi e l'inchiostro (`widgetInk`), la descrizione per TalkBack (la frase e la lettura della
  lastra: un'immagine con il suo equivalente in testo, §9.3).
- **17.3 — Il widget.** Il receiver, il `appwidget-provider` (default 4×2, minimo una cella, nessun
  massimo, come le altre card), il refresh del job, l'anteprima nel selettore, le impostazioni.
- **17.4 — Il confronto e la chiusura.** La galleria di ogni forma su ogni fondo accanto al canvas,
  l'elenco delle differenze (la regola della fedeltà, punto 3); le stringhe IT/EN, la guida dei
  widget, `CHANGELOG.md` (Unreleased), il README (una figura nuova, `widget-in-pictures.png`) e le
  immagini rigenerate e guardate.

## 17.1 — Le misure e il motore di «In immagini» (committente, 15 nov 2026)

Il primo step della Fase 17 (registro, «Fase 17 — «In immagini», il widget disegnato», «Gli step»).
Chiesto così: «Lavora sullo step 17.1 della Fase 17 in PLANNING.md («In immagini»). Leggi prima la
sezione della Fase 17, la regola della fedeltà e i sorgenti in docs/design/in-immagini/, poi il
canvas». Prima dello step, come commit a sé, l'archivio 14 (il registro dal 12 al 14 nov 2026 aveva
passato le ~1 000 righe; la Fase 17 resta qui).

### Come è stato letto il canvas

Le quattro tavole del canvas sono **identiche al byte** ai sorgenti congelati in
`docs/design/in-immagini/` (stesse dimensioni, letti dal canvas il 15 nov 2026); la quinta, la serie
(`Main.dc.html`), non è nel repo e non aggiunge misure: ripete la 4×2, la 2×2 e la 4×1 su un
telefono, con «In parole» 2×2 accanto. Il JS delle tavole (`renderVals()`: `chart`, `dial`,
`ribbon`, `conic`, la striscia) è stato **eseguito sotto node** sull'esempio del canvas (Milano, 15
nov, ore 14, i suoi `T`, `P`, `A`): i numeri che ne escono (punti, controlli delle cubiche, etichette,
barre, ore, dischi, puntini) sono quelli che i test confrontano, al decimo di dp a cui il canvas li
stampa. Un test verde è un disegno che cade dove cade quello approvato.

### Cosa è cambiato

1. **La geometria, in `:core:domain`** (pacchetto `domain/pictures`, Kotlin puro, nessuna risorsa):
   - `PictureWindow` e `PictureSpan`: la finestra del disegno, «Prossime 24 ore» (dall'ora in corso
     alla stessa domani) o «La giornata» (da mezzanotte a mezzanotte, 23 o 25 ore la notte del
     cambio d'ora). **L'asse è il tempo, mai l'indice**: il canvas mette l'ora *i* a `6 + i × passo`
     perché il suo esempio ha 25 ore piene; qui ogni x è la frazione del tempo vero, così un'ora che
     manca è un buco, il disco di adesso sta alle 14:23 quando sono le 14:23, e sull'esempio del
     canvas i numeri sono gli stessi. `PictureHour` legge da `HourlyForecast` temperatura,
     probabilità e millimetri; `strideHours` le righe di «Più tardi» e della griglia della 4×3.
   - `Plate.kt`, la lastra: `PlateMetrics` (ogni costante del canvas con il suo nome: 100 dp, angolo
     16, asse da 6 a 6, curva 2,4, area dal 28% a 50, disco 12/3/35%, etichette 13 sp in 28 × 14 a
     19 sopra e 5 sotto, pioggia 50–68 con filo al 14%, barre al 62% dell'ora e mai sotto 4, angoli
     3/1, nastro 73–79, ore a 83 in 24 dp), `plateDrawing` (i tratti della curva, le cubiche di
     `smooth()` — Catmull-Rom a un sesto, gli estremi doppi —, il disco su quella curva e non sulla
     corda, massima e minima, barre, ore), `RainTone` sulla scala del motore.
   - `Dial.kt`, il quadrante: `DialSpec.LARGE` (124, anello 9, disco 14/2,5, puntini 5 a 16) e
     `SMALL` (52, anello 6, disco 10/2, puntini 3 a 11: misure che il piano non scrive e il canvas
     sì), mezzanotte in basso e mezzogiorno in alto in senso orario, il velo del 42% sul passato, i
     puntini dal 20% con opacità da 0,45 a 1.
   - `Strip.kt`, la striscia della 4×1: dodici ore, i segni della pioggia sul nastro, il disco a 3 dp
     dall'inizio, le cinque ore ancorate inizio, centro, fine (quel che fa il `space-between` del
     canvas, scritto in modo che una bitmap possa disegnarlo).
   - `WeekRange.kt`, i cinque giorni della 4×4 sulla scala fissa da −5° a 30°.
2. **`WeatherStateEngine`**: le tre soglie della pioggia (0,5 / 2,5 / 7,6 mm/h) da `private` a
   `internal`, perché le barre si tonano sulla stessa scala; nessuna regola cambia. `docs/STATES.md`
   §7 dice il nuovo lettore.
3. **Le forme, in `:app`** (`widget/InPicturesLayout.kt`, sul modello di `TextWidgetLayout`):
   `picturesForm` sceglie dalla concessione, `picturesPlan` mette sulla card i rettangoli del glifo,
   del quadrante, della lastra, della striscia, di «Più tardi», e dice quali parole stanno, in
   quante righe, con il numero a che corpo, e cosa compra l'altezza (ore, giorni). Il testo lo misura
   il chiamante con la faccia del sistema (`PicturesMeasure`, come il widget di testo misura la
   frase): Glance non sa misurare. Ogni dp e sp è una costante del canvas con il suo nome.
4. **I test**: `PlateTest` (15) e `DialStripWeekTest` (9) in `:core:domain` contro i numeri del
   canvas; `InPicturesLayoutTest` (18) in `:app`, le otto forme alle grant di riferimento e a 100, 115
   e 130% di testo, con e senza il dato vecchio, misurate da una tabella delle larghezze di Roboto.

### Le otto forme, misurate

A 100%, con l'esempio del canvas (frase in due righe, «Milano · ↑12° ↓6°» in una):

| Forma | Il piano dice | Esce |
|---|---|---|
| 2×1 | numero 30, frase 13, quadrante 52 a destra | quadrante a (95, 16,5), la frase corta in due righe nella colonna di 75 dp |
| 4×1 | numero 36, frase e luogo su una riga, dodici ore | la striscia a y 47 (colonna di 45 dp centrata in 61), larga quanto resta dopo il numero |
| 2×2 | quadrante 124, numero 38 dentro, frase 13, luogo 11 | quadrante a (17,5, 16), 124; l'altezza è spesa al dp (165 su 165) |
| 3×2 | testata 54, numero 44, frase 16, lastra 226 × 100 | lastra a (12, 76), senza luogo |
| 4×2 | icona 46, numero 46, frase a capo, luogo e escursione 13, lastra 312 × 100 | icona a (14, 15); lastra a (14, **77**): vedi la deviazione 1 |
| 2×3 | quadrante, frase intera 16, «Più tardi» tre righe da 24 | «Più tardi» da 190 a 274, tre righe |
| 4×3 | la 4×2 e sei ore in griglia | le ore da 189, sei colonne |
| 4×4 | la 4×2 e cinque giorni da 38 | i giorni da 191, cinque righe, la barra 122 dp |

A 115 e 130% nessun pezzo esce dalla card e il dato vecchio non cede mai (i due test che girano
tutte le forme). Cosa cede, nell'ordine del canvas («la larghezza compra parole»: prima il glifo,
poi il luogo; la frase va a capo e diventa la corta solo dopo):

- **4×2**: a 115 e 130% il luogo va via, la frase intera resta in due righe e l'icona resta (togliere
  l'icona non accorciava la frase, e allora si riprova con l'icona: vedi le decisioni). Il numero
  scende a 45 e 39,8 sp, perché la sua scatola non superi i 54 dp della testata.
- **3×2**: a 115% tutto come a 100 (numero 43); a 130% la frase intera vuole tre righe e la testata
  non le regge: la corta, in due.
- **2×2**: il quadrante scende a 119,6 (115%) e 115,3 dp (130%) prima che una parola ceda; a 130% il
  numero dentro passa a 30,5 sp, il più grande che la corda dell'anello regge.
- **2×3**: a 115% tre righe di «Più tardi»; a 130% la frase intera vuole tre righe e le righe non ci
  stanno più: resta la frase intera con il luogo sotto, senza «Più tardi».
- **4×3**: a 115 e 130% la testata cresce (il luogo sta) e le sei ore non ci stanno più (mancano 3
  dp a 115%): una domanda al committente, sotto.
- **4×4**: quattro giorni a 115 e 130% (righe intere, almeno tre).
- **4×1**: a 130% tutto resta; con il dato vecchio a 115% la frase cede all'«Aggiornato alle».
- **2×1**: a 115 e 130% il quadrante va via, e la frase corta sta in una riga.

### Decisioni e deviazioni

Le deviazioni dal canvas (la regola della fedeltà, punto 2), ognuna con la ragione e la soluzione
più vicina:

1. **La lastra della 4×2 sta a 77, non a 76, quando la frase prende due righe** (piattaforma). Il
   canvas dà alla testata 54 dp più 8 di respiro, e le sue parole (4 + 2 × 20 + 2 + 17 = 63 dp)
   sforano di un dp nella lastra: la scatola del luogo, non le lettere. In RemoteViews testata e
   lastra sono fratelli e non si sovrappongono; la lastra scende del dp che le parole prendono oltre i
   62, e la card lo regge al dp (14 + 63 + 100 + 12 = 189). Con una frase di una riga è a 76.
2. **Il numero della testata si ferma dove la sua scatola è la testata** (regola del canvas,
   generalizzata): 46 sp fino al 112% di testo, poi il corpo la cui scatola fa 54 dp (45 a 115%,
   39,8 a 130%; sulla 3×2, 43 e 38,1). A 130% un 46 pieno farebbe uscire la lastra dalla card.
3. **La curva si ancora in alto a 21 dp** (il canvas: 21,08). Il canvas mette la sua massima a
   `14 + (12,4 − 12,1) × 3,6` dentro un svg spostato di 6: un'àncora di 12,4° scelta a mano per
   quel giorno. La regola che quel numero voleva dire è «l'etichetta della massima a 2 dp dal bordo»;
   ogni punto sta 0,08 dp più in alto del canvas. Il fondo è 49 dp, dove l'etichetta della minima
   finisce sul filo della pioggia.
4. **Il disco di adesso è centrato sul punto** (incoerenza del canvas): la tavola delle forme lo
   sposta di un dp a destra (`x − 5` per un disco di 12), quella delle impostazioni lo centra (`− 6`).
   Vale la seconda: il disco sta sulla curva.
5. **Le barre si tonano con le soglie del motore** (deciso nel piano): 0,5 / 2,5 / 7,6 mm/h sul
   totale d'acqua dell'ora, neve compresa. Sull'esempio del canvas i toni cambiano (il canvas usava
   0,4 / 0,8 / 1,5): dove il canvas aveva quattro toni ne restano due, la pioviggine e la pioggia
   leggera, che è quel che 1,8 mm/h sono. Un'ora senza millimetri (cache di prima del 15.10) prende il
   tono più quieto: la barra dice la probabilità, che è nota, e nessuna intensità.
6. **La striscia ha dodici segni, non tredici** (artefatto del canvas): il suo ciclo arriva all'ora
   12 inclusa e il tredicesimo segno parte dove il nastro finisce, 18,6 dp fuori sul margine. Il nastro
   è dodici ore; l'ora dopo non c'è.
7. **Il quadrante porta 24 ore di pioggia, non 25** (artefatto del canvas): la venticinquesima cade
   sull'angolo della prima, e due puntini lì disegnerebbero la pioggia di domani sopra quella di oggi.
8. **La striscia è larga quanto resta dopo il numero misurato** (piattaforma e canvas): il canvas
   scrive 223 dp a mano per i suoi segni mentre il suo nastro è la colonna flessibile; qui la colonna
   è `340 − 32 − numero − 12` con il numero misurato dal chiamante, e segni e nastro sono la stessa.
9. **Le etichette di massima e minima restano dentro la lastra**, e un punto solo ha un'etichetta
   sola: il canvas non arriva mai al bordo, una massima all'ultima ora metterebbe mezza etichetta sul
   margine; in una giornata piatta massima e minima sarebbero lo stesso punto due volte.

E le decisioni dove il canvas non arriva (le forme fuori dalle otto, i dati che mancano):

- **Le soglie delle forme sono quelle di casa**: una riga sotto 150 dp (`TallMinHeight`), tre celle
  da 210 (`HoursMinWidth`), quattro da 300 (`TextPanelMinWidth`). Una card che non regge il suo
  disegno passa alla forma più piccola che lo regge, mai a una tagliata: la lastra alla striscia (una
  4×2 di 170 dp è una 4×1), la striscia e il quadrante alla 2×1, e la 2×1 risponde sempre (a una cella
  senza quadrante). Il quadrante non scende sotto 96 dp.
- **L'ordine di quel che cede**: il glifo, il luogo, la frase intera per la corta, la frase; mai il
  dato vecchio, che prende il posto del luogo in Caption (11 sp su 13) e, dove la forma non ha luogo
  (2×1, 3×2, 2×3), si aggiunge sotto la frase. Il glifo cede solo se toglierlo accorcia la frase:
  altrimenti il tentativo che sta si riprova con il glifo e lo tiene.
- **L'altezza compra le cose dopo le parole**: «Più tardi» da due a tre righe solo con il quadrante
  pieno a 124 e la frase intera pagati; i giorni della 4×4 da tre a cinque righe intere, altrimenti le
  sei ore; spenti «I prossimi giorni», le ore. La 3×2 compra solo le ore (una barra di giorni a 226 dp
  sarebbe lunga 36 dp, un dp per grado). Una card stretta da 245 dp in su stampa la frase intera come
  la 2×3, con o senza righe.
- **«Più tardi» e la griglia sono le ore della lastra**: dall'ora in corso più tre, a tre ore l'una
  dall'altra (17, 20, 23 alle 14), non il primo multiplo di tre a novanta minuti del widget di testo:
  sotto la lastra le righe nominano le ore che la lastra segna.
- **La minima e la massima**: in «Prossime 24 ore» dalla seconda ora in poi (il canvas: sulla prima
  c'è il disco), in «La giornata» dalla prima, come la tavola delle impostazioni.
- **3,6 dp per grado Celsius, qualunque unità stampi il lettore**: la forma della giornata non cambia
  con le etichette.
- **I dati che mancano**: una temperatura che manca spezza la curva, una probabilità che manca non
  disegna la barra né il puntino; un giorno fuori scala si taglia al bordo e non si stira, con una
  scheggia dell'1% dove la barra sparirebbe; i numeri accanto dicono la verità.
- **Il disegno sporge dalla lastra come nel canvas**: il disco della prima ora finisce 3 dp fuori,
  «ora» 2,5. La bitmap del pittore deve tenere `PlateMetrics.OVERHANG` (6 dp) attorno alla lastra.

### La scala della curva, misurata

Il piano: «un'escursione che non ci sta stringe la scala fino a starci, e lo dice il registro dello
step». Fra 21 e 49 dp ci sono 28 dp, **7,8° a 3,6 dp per grado**. Misurato sulle 26 città di
`tools/measurements/metar-*.json.gz` (la temperatura di `best_match`, ogni finestra di 24 ore che
parte da un'ora):

| Periodo | finestre | mediana | 90° perc. | la scala si stringe |
|---|---|---|---|---|
| 26 set – 25 ott 2026 | 18 096 | 8,3° | 12,7° | **55,2%** |
| 1 dic 2025 – 31 gen 2026 | 38 064 | 5,3° | 9,7° | **25,2%** |

Con una scala fissa più bassa la curva si stringerebbe il 39,9 / 12,6% delle volte a 3,0 dp per
grado, il 20,6 / 3,1% a 2,5, il 5,6 / 0,2% a 2,0 (autunno / inverno). L'estate, che non è nei dati,
ha escursioni più larghe. Il codice fa quel che il piano dice (`plateDegree`); la scelta è del
committente, sotto.

### Domande al committente (proposte, non decise)

1. **La scala della curva.** Con 3,6 dp per grado «sempre» resta vero solo per metà delle giornate
   d'autunno: le altre si stringono, e due giornate diverse si disegnano alla stessa altezza. Una
   scala fissa che quasi mai si stringe è 2,0 dp per grado (14°): una giornata come quella del canvas
   (6,2°) farebbe 12 dp di curva invece di 22. Proposta: 2,5 dp per grado (11,2°), che si stringe un
   giorno d'autunno su cinque e quasi mai d'inverno. La costante è una (`PlateMetrics.DEGREE`).
2. **La 4×3 a 115 e 130% di testo.** La testata cresce, il luogo e l'escursione stanno, e le sei ore
   no (a 115% mancano 3 dp): la card è una 4×2 con 95 dp vuoti sotto. L'ordine del canvas tiene il
   luogo (è il quarto pezzo letto, la griglia viene dopo); l'alternativa è togliere il luogo e tenere
   le ore.
3. **Gli avvisi ufficiali.** Le impostazioni del canvas hanno l'interruttore («il livello in parola e
   in colore, solo quando c'è»), le tavole non disegnano dove va la parola. Il 17.2 ha bisogno di un
   posto: proposta, quello del luogo, come la riga dei fatti di «In parole».

### Trovato e non toccato, per gli step dopo

- **Il numero in un `TextView`.** Il canvas mette 46 sp in una scatola di 48 dp con `letter-spacing:
  −0,03em`; un `TextView` tiene 61 dp (`includeFontPadding`) e Glance non ha la spaziatura delle
  lettere. Il 17.2 sceglie fra il numero disegnato nella bitmap e un `TextView` di altezza fissa
  con l'inchiostro dove il canvas lo mette; il layout conta sulla scatola del canvas.
- **«La giornata» vuole le ore del mattino già passate**, e il report non le ha: `WeatherRecency`
  le toglie. Il 17.3 deve dire da dove vengono (lo storico, o il report prima del taglio); senza, la
  curva parte da adesso e la mattina è vuota, che è vero ma non è il disegno.
- **L'etichetta della minima può cadere su una barra** in un'ora fredda e piovosa (il canvas non ci
  arriva: la sua minima è in un'ora asciutta). Il pittore decide l'ordine dei livelli.
- **Gli inchiostri della pioggia per fondo** (la tavola dei fondi ha una rampa per il cielo, una per
  il chiaro, una per lo scuro) e il loro contrasto a 3:1: 17.2, come il piano dice.

### Come è stato verificato

I 24 test della geometria contro i numeri che il JS del canvas calcola, i 18 delle forme. La suite
di `:core:domain` e di `:app` (1 734 test, 16 saltati, le immagini del README che girano solo a
richiesta) e il lint di `:app`: verdi. Nessuna immagine del README cambia: nessuna schermata è
toccata. Niente da provare sul telefono in questo step: è geometria, e il disegno arriva con il
17.2.

## Le tre domande del 17.1, chiuse (committente, 15 nov 2026)

«Per le tre tue domande per chiudere procedi con le tue proposte che mi vanno bene» (registro,
«17.1», «Domande al committente»). Chiuse prima del 17.2, nella stessa chat:

1. **La scala della curva è 2,5 dp per grado** (`PlateMetrics.DEGREE`, da 3,6): 11,2° fra 21 e 49 dp,
   la curva si stringe il 20,6% delle finestre d'autunno misurate e il 3,1% d'inverno, contro il 55,2 e
   il 25,2. Cambia una decisione del piano («3,6 dp per grado, sempre», registro, «Fase 17», «La
   lastra») e un numero del canvas: è una deviazione decisa dal committente. Sull'esempio del canvas
   la curva di Milano (6,2°) è alta 15,5 dp invece di 22,3; le x, le barre, le ore, i dischi sono gli
   stessi. `PlateTest` confronta ora le y con la regola nuova e le x con il canvas.
2. **La 4×3 a 115 e 130% di testo segue l'ordine del canvas**: il luogo e l'escursione restano, le sei
   ore no. Per questa domanda il 17.1 non aveva fatto una proposta (aveva chiesto quale delle due);
   resta quel che il codice già faceva, l'ordine di lettura dell'anatomia, che è la regola della
   fedeltà. Si cambia in una riga se il committente, vista la card sul telefono, preferisce le ore.
3. **L'avviso ufficiale sta nella riga del luogo**, come il chip della famiglia (segno, parola,
   colore: `WarningChip`, `PicturesPiece.Warning`): prende il posto del luogo e non cede mai, come il
   dato vecchio; con un dato vecchio la riga è del dato vecchio, perché un avviso letto da un archivio
   che il job non ha rinfrescato è vecchio quanto la previsione. Sulle forme senza luogo (2×1, 3×2,
   2×3) si aggiunge sotto la frase. Sulla 4×2 di riferimento un giorno d'avviso costa 4 dp (il chip è
   21,2 contro 17): cede il glifo e la frase intera sta in una riga. `InPicturesLayoutTest` gira tutte
   le forme a tutte le scale con l'avviso, e con l'avviso e il dato vecchio insieme.

## 17.2 — Il disegno: la lastra, il quadrante, la card in ogni forma (committente, 15 nov 2026)

Il secondo step della Fase 17 (registro, «Fase 17 — «In immagini», il widget disegnato», «Gli
step»), nella stessa chat del 17.1, chiesto così: «Poi parti con lo step 17.2». Il canvas è lo
stesso letto nel 17.1 (le quattro tavole identiche al byte ai sorgenti congelati); il widget non ha
ancora un receiver (17.3), e la card si guarda nella galleria qui sotto.

### Cosa è cambiato

1. **Gli inchiostri** (`ui/theme/PicturesInks.kt`): per ogni fondo del canvas (il cielo con lo
   scrim di §3.6, i sei colori, la carta, lo scuro) l'inchiostro, il quieto, il velo e il filo della
   lastra, il disco di adesso e i suoi anelli, le quattro tinte della pioggia, i puntini, il velo del
   passato sul quadrante, i segni della striscia. Qui e non nel pittore perché un hex fuori da
   `ui/theme/` è un hex fuori da `ui/theme/` (`NoRawColorTest`). `PicturesInksTest` li misura.
2. **Il pittore** (`widget/pictures/PicturesPainter.kt`): la lastra, il quadrante, la striscia della
   4×1, il fondo di «Più tardi» e la barra di un giorno, bitmap alla misura vera e alla densità dello
   schermo, dalla geometria di `domain/pictures`. Le etichette della massima e della minima e le ore
   sono disegnate nella bitmap con la faccia del sistema (`Typeface.DEFAULT`, Semibold a 600), come il
   launcher le scriverebbe. In «La giornata» la curva e le barre prima di adesso sono al 45%. Il
   nastro della lastra e della striscia sono le fasi di ogni giorno che la finestra attraversa, con il
   cielo previsto, mappate sulla finestra (`windowRibbon`: `ribbonColorStops` dell'hero e
   `SkyPalette.gradient(…).mid`); l'anello del quadrante è il nastro di oggi girato in senso orario
   da mezzanotte in basso.
3. **La card** (`widget/pictures/InPicturesContent.kt`): il piano di `InPicturesLayout` disegnato in
   Glance, in ogni forma. **Ogni pezzo sta dove il piano lo mette**: la card è un `Box` (un
   `FrameLayout` sul launcher) e ogni pezzo è un figlio spostato al suo rettangolo dal suo padding,
   così le posizioni assolute del canvas sono quelle della card e nessuna riga o colonna le rimescola.
   Un testo è spostato anche di `textShift`, perché la sua prima linea di base cada dove la mette la
   scatola del canvas: il canvas mette un 46 sp in una scatola di 48, un `TextView` lo mette in una di
   61 con la base 9 dp più in basso. Il testo lo misura il dispositivo (`DevicePicturesMeasure`:
   `StaticLayout` sulla faccia del sistema, Bold il numero, Medium la frase e l'avviso). La frase
   intera è quella di Oggi (`HeadlineText`, registro pieno), la corta quella di «In parole» (il
   breve). L'icona dell'ora è quella della famiglia (`ChiaroIcons.conditionRes`, lo stato del motore),
   mai il disegno segnaposto del canvas; l'avviso è il chip della famiglia; il luogo porta lo spillo
   quando è la posizione (`PlaceLine`), come in ogni card.
4. **TalkBack** (§9.3, un'immagine con il suo equivalente in testo): la lastra, il quadrante e la
   striscia hanno ciascuno una descrizione che dice quel che disegnano, nell'ordine in cui si leggono
   — adesso, la massima e la minima con la loro ora, da quando a quando la pioggia è probabile e la
   sua ora più probabile, o che non lo è (`pictureDescription`, IT/EN). Le barre dei giorni no: le
   due cifre accanto le dicono.
5. **Le stringhe** IT/EN (`pictures_*`), poche: «ora», la coppia dell'escursione, le descrizioni.
6. **La galleria** (`InPicturesGallery`, `testDebug`): le otto forme alle grant di riferimento, come
   le dispone la tavola «Le otto forme», sul cielo, sulla carta, sullo scuro, sul blu, di notte, in
   «La giornata», a 115 e 130% di testo, disegnate come le card del README (Glance in `RemoteViews`,
   applicate a una vista, passate per la GPU), solo a richiesta:
   `./gradlew :app:testDebugUnitTest --tests "*InPicturesGallery" -PpicturesGallery=<cartella>`.
   È il materiale del confronto del 17.4. **Guardata, tavola per tavola**, e corretta tre volte (sotto).
7. **I test**: `PicturesInksTest` (4), `PicturesPainterTest` (4: la misura della bitmap con il suo
   margine, una barra nella sua tinta, il disco bianco sul suo punto, il velo del passato sul
   quadrante, i segni della striscia, le parole della lastra), `InPicturesLayoutTest` aggiornato.

### Decisioni e deviazioni

Dal canvas (la regola della fedeltà, punto 2):

1. **Le tinte della pioggia, dove non passavano 3:1 come segni** (regola misurata, il contrasto). Sul
   velo della lastra sopra il cielo più chiaro che lo scrim lascia (luminanza 0,086) la tinta più
   scura del canvas, #1E97CF, fa 2,35:1; sulla carta le due più chiare, #76BCEC e #2E97DE, 1,78 e 2,75;
   sullo scuro la più scura, #006C98, 2,81. Ognuna si è spostata solo fin dove il minimo chiede, verso
   la tinta vicina, nello stesso blu e nello stesso ordine: sul cielo e sui colori #3FB0E6, #62C1EE,
   #86D1F7, #A9E2FF (le due di mezzo divise in parti uguali fra la prima e l'ultima del canvas); sulla
   carta #2988C8, #006FAC, #005A8E, #004470; sullo scuro #0078A8, #0092C8, #55BCEC, #A9E2FF. Quattro
   tinte restano quattro, ciascuna almeno 1,1 volte più lontana dal fondo della precedente.
2. **I puntini del quadrante sulla carta sono #006FAC**, non #2E97DE (2,75:1 a piena opacità).
3. **Il velo della lastra**: sul cielo `rgba(6,24,52,0,30)` e sui colori nero al 18%, del canvas e del
   piano; sulla carta l'inchiostro al 5% (il piano; il canvas aveva un ambra al 6%); sullo scuro bianco
   al 5%, come il canvas, dove il piano scrive «il loro scuro»: un velo più scuro sul fondo più scuro
   non si vede.
4. **L'area sotto la curva sfuma dal 28% allo zero su ogni fondo** (il piano), dove la tavola dei fondi
   la riempiva al 10% piatto.
5. **Le righe della frase sono quelle del `TextView`** (piattaforma: Glance non ha l'altezza di
   riga): 18,75 dp fra due righe da 16 sp invece di 20. La prima riga cade dove la mette il canvas
   (`textShift`); due righe in tutto misurano 40 dp in entrambi.
6. **Il numero senza `letter-spacing: −0,03em`** (piattaforma: Glance non ha la spaziatura delle
   lettere): «20°» è 1 dp più largo che nel canvas.
7. **Il fondo di «Più tardi» è una bitmap** (piattaforma: Glance non ha un bordo), per avere il filo
   bianco al 10% della lastra.

E le decisioni prese guardando la galleria:

- **Il glifo cede anche alle ore e ai giorni**: sulla 4×3 una frase di tre righe accanto al glifo
  toglieva le sei ore, sulla 4×4 un giorno. Il glifo è il primo pezzo che una card dà via, quindi
  dove la stessa testata senza glifo tiene più ore o più giorni, il glifo va
  (`InPicturesLayout.extraRank`).
- **Le colonne dei giorni crescono con il testo** e la barra si accorcia (122 dp a 100%, 86 a 130%):
  a 130% la minima usciva «1…», e il canvas vieta i «…» su un numero. La colonna dell'ora di «Più
  tardi» è la più larga delle sue etichette, mai meno dei 20 dp del canvas («5 PM» non ci sta).
- **La 2×1 posiziona le sue righe come le altre forme**: in colonna i `TextView` impilavano le loro
  scatole, più alte di quelle del canvas, e «Aggiornato 8 ore fa» usciva in fondo alla card.
- **La griglia delle ore e i giorni scrivono la probabilità nel colore dei puntini**, che sul fondo
  della card passa 4,5:1 come testo su ogni fondo (`PicturesInksTest`).

### Trovato e non toccato, per gli step dopo

- **«La giornata» disegna solo da adesso in poi**: il report non ha le ore del mattino già passate
  (`WeatherRecency`), e la curva parte dal disco. È vero ma non è il disegno della tavola delle
  impostazioni (la mattina più chiara): il 17.3 deve dire da dove vengono quelle ore.
- **Il fondo predefinito di questo widget è il cielo** (la decisione 1 del committente), mentre
  `WidgetLook.defaultsFor` dà il colore alle card della famiglia: la galleria lo imposta a mano, il
  17.3 lo scrive nelle impostazioni.
- **I dati della galleria sono la registrazione del README** (Milano, 25 set 2026): una settimana
  asciutta, quindi niente barre a schermo. Le barre, i puntini e i segni sono provati da
  `PicturesPainterTest` sull'esempio piovoso del canvas, non guardati in una card.

### Come è stato verificato

La galleria, otto tavole, guardate una per una (sopra, i difetti trovati e corretti). I test nuovi
e quelli del 17.1; la suite di `:core:domain` e di `:app` e il lint (sotto, con l'esito).
