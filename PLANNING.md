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
- [ ] **La riga di Giove sulla regola dell'elenco «Da vedere»** (registro, «16.8», «Trovato e non
  toccato»), come Marte e Saturno dal 16.8. Stesso buio (dal crepuscolo civile) e stesso
  pavimento (10°); la riga non ha il quarto d'ora minimo dell'elenco, e in una notte con due
  tratti prende il primo dove l'elenco prende quello con il punto più alto. Misurato il 12 nov
  2026 (registro, «Giove: la riga contro l'elenco, la misura»): in sei anni le finestre
  coincidono in ogni notte che ne ha due, e la riga ne dà in più da 5 a 11 l'anno, tutte sotto
  il quarto d'ora. Proposta, non decisa: toglie il promemoria di quelle sere, e non cambia altro.
- [ ] **16.9** «Da vedere», e la notte di un evento (Fase 16).
- [ ] **16.10** «Punta il telefono», da provare sul telefono prima di tenerlo (Fase 16).
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
sole». Tutte le altre notti restano identiche. Non è stato fatto: la casella resta aperta in
«Aperto», con questi numeri, e la decisione è del committente.
