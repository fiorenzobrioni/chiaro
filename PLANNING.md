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
  `docs/planning/13-novembre-12.md`; la chiusura (16.11) e la coda in
  `docs/planning/14-novembre-12-16.md`.
- **Chiusa il 16 nov 2026**: **Fase 14**, il motore contro i motori premium (dal 27 ott 2026): tredici step, uno per chat; i primi quattro (14.1–14.3 con la scadenza del 30 novembre, la visibilità ICON che sparisce; 14.4 la misura di ICON-2I, che ha detto no), il 14.6 (il caldo), il 14.7 (il vento), il 14.8 (la quota neve), il 14.9 (le unità), il 14.10 (la concordanza dei modelli, che ha detto no) e il 14.13 (le icone, prima dell'inverno) sono chiusi; 14.5 e 14.11 non esistono. La revisione, le decisioni, le regole comuni e la descrizione di ogni step sono in `docs/planning/07-ottobre-22-27.md`, «Fase 14 — Il motore contro i motori premium»; gli step chiusi dal 27 ott 2026 e l'attesa di 14.12 sono in `docs/planning/08-ottobre-27-28.md`; il 14.12, l'unico rimasto, è uscito dalla fase il 16 nov 2026 senza essere fatto, ed è in «Da decidere: se e quando» (archivio 14, «Gadgetbridge e gli avvisi fuori dall'Italia escono dalle fasi attive»).
- **Nessuna fase attiva** dal 16 nov 2026: quel che resta in «Aperto» sono una prova sul telefono, una misura che aspetta l'inverno e, dal 17 nov 2026, la nebbia dei due modelli da vedere dal vivo dopo il 30 novembre. Dal 17 nov 2026 anche le caselle della review del motore: due prove dal vivo e «Velato», una proposta misurata che aspetta la scelta del disegno; il temporale dedotto fuori dalla famiglia ICON è fra le cose «Da decidere: se e quando». Il registro fino al 16 nov 2026 è in `docs/planning/14-novembre-12-16.md`.
- **Da decidere: se e quando** (dal 16 nov 2026). Non è una fase attiva e non è lavoro da fare: sono le funzioni che il committente non ha ancora deciso se fare, tutte o alcune, né quando. Stanno in fondo ad «Aperto», sotto il loro titolo; nessuna si comincia senza il suo via, e una che parte torna fra le caselle con la sua fase o il suo step.

## Aperto

Quel che resta da fare o da decidere, in un posto solo: una casella si apre qui, con il rimando
alla sezione del registro da cui nasce, e si chiude qui, con quella che la chiude.

- [ ] **Il cielo d'inverno contro il satellite.** La regola 6 è misurata su ottobre soltanto:
  l'archivio del satellite di Open-Meteo parte da febbraio 2026. Rilanciare
  `tools/measure_against_metar.py` su dicembre–gennaio 2026-27 e rileggere STATES §11 (archivio 07,
  «Il motore dopo la revisione», 26 ott 2026).
- [x] **L'icona nuova sul telefono** (archivio 11, «L'icona nuova: l'ora blu», 9 nov 2026): l'icona
  nella maschera del telefono, l'icona a tema, l'icona piccola delle notifiche alla sua misura.
  Fatto dal committente il 9 nov 2026 (archivio 11, «Le icone nuove provate sul telefono»).
- [x] **Le icone nuove del Cielo sul telefono** (archivio 11, «Le icone del Cielo, rilette tutte», 9 nov
  2026): la lista del Cielo e il catalogo nelle due famiglie d'icone e nei due temi, e la
  finestra; la luce zodiacale soprattutto, la piu' debole delle sei. Fatto dal committente il
  9 nov 2026, la luce zodiacale compresa (archivio 11, «Le icone nuove provate sul telefono»).
- [x] **«A colpo d'occhio» come «In parole», sul telefono** (archivio 14, «"A colpo d'occhio" con il
  layout di "In parole"», 15 nov 2026): il 4×1 accanto a «In parole» sul launcher del
  committente, il nome lungo intero, l'icona alla misura del numero; il 2×1 e il 3×1; la scheda
  delle ore a 4×2; un giorno con l'allerta (il chip al bordo opposto) e uno con i dati vecchi.
  Fatto: provato dal committente sul telefono il 16 nov 2026 (archivio 14, «Le prove sul telefono del 16 nov 2026»).
- [x] **La revisione di Oggi e del cielo sul telefono** (archivio 11, «Oggi e il motore del cielo: la
  revisione da app premium», 9 nov 2026). Quel che un container non può dire: **la cifra della
  GPU** con il cielo peggiore salito da 25 a 28 campioni (`adb shell dumpsys gfxinfo`, sotto 3 ms
  a frame su un telefono della classe del Pixel 6a, DESIGN §3.5; se non tiene, i raggi del sole
  sono i primi a uscire: `rays` a zero in `skyScene`); il nastro trascinato sotto il dito (il
  passo d'ora, il tocco aptico, il ritorno ad adesso, lo scorrimento della pagina che parte da
  lì); la nebbia e il lampo in un
  giorno che li ha; la riga della settimana con lo chevron a 360dp e a 130% di carattere.
  Dal 16 nov 2026 (archivio 14, «Le prove sul telefono del 16 nov 2026»): provati il nastro sotto il dito e la riga della settimana; la
  cifra della GPU non è stata letta (niente cavo) e il committente la dà per buona a occhio, i
  raggi restano. restavano la nebbia e il lampo.
  Chiusa il 16 nov 2026: la nebbia e il lampo visti dal committente lo stesso giorno, il lampo
  anche di notte (stessa sezione).
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
- [x] **Il peso della cifra: Regular nell'app, Bold nelle card** (archivio 11, «Oggi e il motore del cielo: la revisione da app premium», «La cifra»).
  La revisione accettata chiedeva 300; è 400, il più leggero che le due famiglie disegnano, e
  le card restano Bold: la regola del 20 set 2026 («Una temperatura eroe è Bold», DESIGN §5)
  vale ora per le card soltanto. Da confermare dal committente guardandole fianco a fianco,
  come allora; tornare indietro è una riga (`HeroWeight` in `Type.kt`) e il suo test.
  Confermato dal committente il 16 nov 2026, guardate fianco a fianco sul telefono (archivio 14, «Le prove sul telefono del 16 nov 2026»).
- [x] **La nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo** (la card
  Stanotte fino al 16.1; archivio 11, stessa sezione, «Dove non arriva»): lo shader li disegna, `WindowScene` e `TonightSky` non li
  chiedono ancora. Proposta, non decisa.
  Fatto il 16 nov 2026 (archivio 14, «Le quattro proposte»): tutte e tre nell'hero del Cielo; nella finestra la nebbia e il
  banco scuro del temporale, né lampo né scia, perché è un'immagine ferma.
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
- [x] **Le immagini dei widget nel README dipendono dal fuso della macchina** (archivio 12, «16.3»,
  «Come è stato verificato»). La finestra scrive l'ora del luogo accanto al nome quando
  l'orologio del luogo non è quello del telefono; nel test il telefono è la macchina, e su una
  con un altro fuso esce «Milan · 01:00» (nelle immagini committate), su una col fuso di Milano no.
  Proposta, non decisa: `ReadmeScreenshots` fissa il fuso della JVM a quello del luogo, e le due
  immagini si rigenerano senza l'ora.
  Fatto il 16 nov 2026 con la proposta com'era (archivio 14, «Le quattro proposte»): le due immagini rigenerate, senza l'ora.
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
- [x] **Il ritmo delle sezioni del Cielo** (stessa sezione, «Trovato e non toccato»): 17dp sopra
  un titolo e 5 sotto, dove Oggi e Avvisi hanno i 24 e 12 di DESIGN §8.0. Proposta, non decisa:
  `SkySectionTitle` sui numeri di Oggi, e le due immagini del Cielo rigenerate.
  Fatto il 16 nov 2026 con la proposta com'era (archivio 14, «Le quattro proposte»): cambia `sky-ahead.png` soltanto.
- [x] **La barra di Avvisi quando la risposta è scorsa via** (stessa sezione, «Proposte, non
  decise»; DESIGN §13, voce 6): il segno e la parola del livello mentre un'allerta è in vigore.
  Proposta, non decisa.
  Fatto il 16 nov 2026 con la proposta com'era (archivio 14, «Le quattro proposte»).
- [x] **Le fasce di Avvisi con l'orologio a 12 ore** (stessa sezione, «Trovato e non toccato»):
  «18–23» accanto a «alle 2:34 PM». Fatto il 12 nov 2026, nella stessa PR, su richiesta del
  committente, con il selettore dell'ora di una fascia (archivio 14, «Avvisi: le tre scelte
  confermate, le ore sull'orologio del telefono, il selettore dell'ora»).
- [x] **I widget del 14 nov 2026 sul telefono** (archivio 14, «I widget: la massima e la minima a una
  riga, la nuvola del coperto, la posizione fissata»): la 4×1 con «Massima e minima di oggi» accesa,
  la frase ancora leggibile a due righe sul launcher del committente; la nuvola del coperto accanto
  alle altre; un widget fissato a «La mia posizione» con l'app su un'altra località, anche dopo uno
  spostamento.
  Fatto: provato dal committente sul telefono il 16 nov 2026 (archivio 14, «Le prove sul telefono del 16 nov 2026»).
- [x] **Il primo avvio sul telefono** (archivio 14, «Il primo avvio, rivisto con quelli della
  famiglia», 12 nov 2026). Su un'installazione nuova, o dopo aver cancellato i dati dell'app: il
  benvenuto e «Iniziamo»; il luogo e il gesto indietro che torna al benvenuto; la posizione negata
  una volta e poi per sempre (la riga sotto le parole, «Apri le impostazioni»); le notifiche; gli a
  capo in italiano e in inglese con il carattere grande; lo scorrimento avanti e indietro, e la sola
  dissolvenza con le animazioni tolte.
  Fatto: provato dal committente sul telefono il 16 nov 2026 (archivio 14, «Le prove sul telefono del 16 nov 2026»).
- [x] **Le quattro proposte sul telefono** (archivio 14, «Le quattro proposte», «Cosa conferma solo
  il telefono», 16 nov 2026): l'hero del Cielo in una notte di nebbia (il nastro che ci passa
  dentro, «Dietro le nuvole» che la toglie) e in un temporale (il lampo); la finestra in
  un'ora di nebbia, di giorno e di notte; la barra di Avvisi con un'allerta in vigore, il chip
  che entra e che esce, con il carattere grande accanto a un nome lungo.
  Fatto: provato dal committente sul telefono il 16 nov 2026, sull'APK della PR #175 (archivio 14,
  «Le quattro proposte sul telefono: provate»).
- [x] **Quel che cade, nell'hero del Cielo** (stessa sezione, «Trovato e non toccato»): la sua
  scena non chiede né pioggia né neve (`SkyFall.NONE`), e dal 16 nov 2026 un temporale vi
  lampeggia sopra un'immagine asciutta. Proposta, non decisa: le ore della notte portano lo
  stato e i millimetri, come quelle di Oggi.
  Fatto il 16 nov 2026, senza il vento (archivio 14, «Quel che cade nell'hero del Cielo, e la
  nebbia contro il verdetto: la misura»).
- [x] **Le parole dell'hero del Cielo non sanno della nebbia** (stessa sezione, «Trovato e non
  toccato»): sopra una nebbia disegnata il verdetto dice «Notte di luna», «Nuvole 20%».
  Il verdetto legge la copertura, non lo stato. Proposta, non decisa, e non piccola: è una
  regola del motore dei verdetti, con la sua misura.
  **Misurata il 16 nov 2026** (stessa sezione, «La misura: la nebbia contro la copertura»): dove l'ora ha una
  visibilità sua la copertura è oltre il 65% nel 98,2% delle ore di nebbia, e il verdetto dice
  già «niente da fare»; con la visibilità prestata, nel 93,0%, e nelle 148 ore che restano
  l'aeroporto ha visto aria limpida in 84 e nebbia in 39. Proposta: **nessuna regola**.
  Chiusa il 16 nov 2026 **senza regola**, sul sì del committente (archivio 14, «Nessuna regola
  per la nebbia nel verdetto del Cielo»).
- [x] **Quel che cade nell'hero del Cielo, sul telefono** (archivio 14, «Quel che cade nell'hero
  del Cielo, e la nebbia contro il verdetto: la misura», 16 nov 2026): una notte di pioggia e
  una di neve nell'hero del Cielo, il nastro che entra e esce dalle ore bagnate, il temporale
  con la sua pioggia, «Dietro le nuvole» che la toglie.
  Dal 16 nov 2026 (registro, «Quel che cade di notte: una luce sua»): provati dal committente
  la pioggia di notte, il nastro dentro e fuori dalle ore bagnate, il temporale con la sua
  pioggia e «Dietro le nuvole» che la toglie (Buenos Aires). **Resta la neve**, che in una
  notte senza luna non si vedeva: da provare sul telefono con la luce nuova. Il valore
  della luce, 0,10, è confermato dal committente sulla tavola resa (stessa sezione, «La
  decisione»).
  Chiusa il 16 nov 2026: la neve di notte vista dal committente sul telefono con la luce
  nuova (registro, «Quel che cade di notte: una luce sua», «Sul telefono»).
- [ ] **L'icona di «A colpo d'occhio» più dentro, sul telefono** (registro, ««A colpo d'occhio»:
  l'icona cinque dp più dentro», 16 nov 2026): il 4×1 accanto a «In parole» e al widget di
  Passo, con il coperto e con il sole; il 2×1; la scheda delle ore.
- [ ] **La nebbia dei due modelli, dal vivo dopo il 30 novembre** (registro, «La nebbia: due
  modelli», 17 nov 2026): la sonda di dicembre (`Probe provider`) che trova vuota la visibilità
  di ICON in Italia, e l'app che non cambia nulla quel giorno; un mattino di nebbia padana in cui
  la pagina dice «Nebbia» dove due modelli la vedono, e il riquadro della visibilità con la
  parola dove il numero manca o supera il chilometro.
- [ ] **L'hero nelle ore da 0,1 mm, dal vivo** (registro, «La review del motore, punto 1», 17
  nov 2026): un luogo fuori dal quarto d'ora calcolato (Roma, Napoli, una città fuori
  dall'Europa) in un'ora di pioggia debolissima, con l'hero che dice «Pioviggine» come la cella
  «Ora».
- [x] **Il bordo sud di ICON-D2 nella sonda** (stessa sezione, «La misura»). La casella, com'era
  scritta, era sbagliata in due punti: diceva che la sonda non ha una città fra 43,4° e 44° N, e
  ha Firenze (43,77°) e Ancona (43,62°), che con il bordo nuovo (43,6°) sta proprio sul confine
  interno e lo sorveglia già; e chiedeva una città sull'Appennino per una differenza fra
  `best_match` e ICON-D2 che, rimisurata, non c'è nelle ore che l'app legge. Mancava una città
  appena fuori. Fatto il 17 nov 2026: **Livorno** (43,55°) fra le città della sonda, che ora
  sono 46 (registro, «La review del motore: le quattro proposte rilette»).
- [x] **Le parole dell'interruttore della pioggia** (registro, «La review del motore, punto 3»,
  «Trovato e non toccato»): la notifica ora dice anche «Neve in arrivo», la riga di Avvisi, il
  canale e la guida dicevano solo «pioggia», e la vecchia regola. Fatto il 17 nov 2026, nella
  stessa PR: «Pioggia o neve nelle prossime ore» (registro, «La review del motore: le quattro
  proposte rilette»).
- [ ] **Il temporale senza la soglia, dal vivo** (registro, «La review del motore, punto 4»): la
  prossima estate, un pomeriggio di temporali isolati in pianura con la probabilità sotto il
  20%, e la pagina che dice «Temporale».
- [ ] **«Velato»: lo stato e il suo disegno** (registro, «La review del motore, punto 5»):
  misurato (7,7% delle ore di giorno, 5,6% in pianura padana) e non fatto. Rimandato a una
  prossima sessione dal committente il 17 nov 2026. **Il passo intero è scritto** (registro,
  ««Velato»: il passo, pronto da eseguire»): le cinque decisioni da prendere con lui, la prima
  delle quali è il disegno, da una tavola dei candidati; la regola e dove va; i punti del
  codice uno per uno; le tre strade per le icone; i test, i documenti, le immagini; la misura
  da rendere ripetibile.
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
- [x] **14.12** Gli avvisi ufficiali fuori dall'Italia, a chiusura garantita. **Non fatto**: in attesa dal 28 ott 2026 (archivio 08, «14.12 resta in attesa»), uscito dalla Fase 14 il 16 nov 2026 e passato a «Da decidere: se e quando», qui sotto (archivio 14, «Gadgetbridge e gli avvisi fuori dall'Italia escono dalle fasi attive»).
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

### Da decidere: se e quando

Fuori dalle fasi attive dal 16 nov 2026: il committente non ha ancora deciso se farle, tutte e
due o una, né quando. Niente qui si comincia senza il suo via.

- **Il meteo di Chiaro ad altre app con la trasmissione di Gadgetbridge** (issue #72). Era
  «decisa, da fare quando c'è spazio» (archivio 11, «Il meteo di Chiaro ad altre app: la
  trasmissione di Gadgetbridge», 2 nov 2026); dal 16 nov 2026 il se e il quando tornano da
  decidere. Il come resta quello scritto allora, se si fa: `ACTION_GENERIC_WEATHER`, spenta di
  default, gli stati di Chiaro e non il `weather_code`, nessuna richiesta in più se il job gira
  già, niente coordinate, un intent esplicito per app. L'issue resta aperta: lì è scritto che
  si farà, senza data.
- **Gli avvisi ufficiali fuori dall'Italia, a chiusura garantita** (era lo step 14.12 della
  Fase 14; archivio 07, «Fase 14 — Il motore contro i motori premium», e archivio 08, «14.12
  resta in attesa», 28 ott 2026). Aspettava una base di lettori in Europa; dal 16 nov 2026 non
  è più uno step di una fase.
- **Un temporale dedotto dalla CAPE dove il provider non scrive il codice** (registro, «La
  review del motore, punto 4», «Trovato, e non toccato», 17 nov 2026). Fuori dalla famiglia ICON
  (Stati Uniti, Regno Unito, Olanda, paesi nordici, Giappone, Singapore) l'app non dice mai
  «Temporale». Dedotto dalla CAPE vedrebbe tre volte i giorni di temporale con la stessa
  precisione sul giorno (54% contro 57%) e sbaglierebbe l'ora nove volte su dieci. Non ora:
  costa 0,1 chiamate a ogni fetch ovunque, anche in Italia dove il codice c'è e dove la stessa
  regola fa peggio del codice; è un pericolo che l'app inventerebbe, contro la regola 1; e
  chiede un concetto nuovo, un «Temporali possibili» della giornata, su ogni superficie
  (registro, «La review del motore: le quattro proposte rilette»).

Riletto il 16 nov 2026, all'archivio 14, a registro chiuso: le caselle nate dal 12 nov 2026 rimandano all'archivio 14 (`docs/planning/14-novembre-12-16.md`); la Fase 16 e la Fase 14 sono chiuse e nessuna fase è attiva; restano aperte una prova sul telefono (quel che cade nell'hero del Cielo) e il cielo d'inverno contro il satellite, che aspetta febbraio 2027; nessuna proposta non decisa; «Da decidere: se e quando» tiene la trasmissione di Gadgetbridge e gli avvisi ufficiali fuori dall'Italia, che non sono lavoro finché il committente non lo dice. Riletto il 12 nov 2026, all'archivio 13, all'inizio del 16.11: le caselle nate dal 12 nov 2026 rimandano all'archivio 13 (`docs/planning/13-novembre-12.md`), la descrizione degli step della Fase 16 resta nell'archivio 11 (16.11 aperto: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.8, 16.9, e 16.10 che decide se «Punta il telefono» si tiene), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; due proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, e il fuso della macchina nelle immagini dei widget del README. Riletto il 12 nov 2026, all'archivio 12, all'inizio del 16.8: le caselle nate dal 10 nov 2026 rimandano all'archivio 12 (`docs/planning/12-novembre-10-11.md`), la descrizione degli step della Fase 16 resta nell'archivio 11 (16.8–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, le due caselle del 16.4, 16.5, 16.6, 16.7, la notte bianca), il peso della cifra e «Costellazioni» accesa da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; due proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, e il fuso della macchina nelle immagini dei widget del README. Riletto il 10 nov 2026, all'archivio 11, all'inizio del 16.3: le caselle nate dal 2 nov 2026 rimandano all'archivio 11 (`docs/planning/11-novembre-2-10.md`), dove sta anche la descrizione degli step della Fase 16 (16.3–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.1, 16.2), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; una proposta non decisa, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo. Riletto il 2 nov 2026, all'archivio 10, con la 2.0.0: le caselle chiuse dal 29 ott 2026 (15.13, 15.14) rimandano all'archivio 10 (`docs/planning/10-verso-2.0.0.md`); restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, dopo la rilettura della Fase 15: aperte 15.13 e 15.14, la coda; e come prima 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, all'archivio 09: le caselle della Fase 15 sono tutte chiuse (in `docs/planning/09-ottobre-28-29.md`), restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 28 ott 2026, all'archivio 08, lo stesso giorno dell'apertura della Fase 15: le caselle sopra sono della Fase 14 (14.12 in attesa) e della Fase 15 (in `docs/planning/08-ottobre-27-28.md`), nessuna proposta non decisa. Riletto il 27 ott 2026, all'archivio 07: le caselle della Fase 14, nessuna proposta non decisa. Fino al 26 ott 2026 nessuna casella aperta (riletto il 22 ott 2026, all'archivio 06):
le ultime chiuse quel giorno stanno in `docs/planning/06-ottobre-20-22.md`, «Cielo: la finestra
reale nella card Stanotte», «Il widget «Momenti del cielo» va via», «Le notifiche: ogni riga nella
forma che sta in una riga», «Il widget «Le prossime ore» va via».

---

## Registro dal 16 nov 2026, dopo l'archivio 14

## Quel che cade di notte: una luce sua (committente, 16 nov 2026)

**Da dove nasce.** Provando sul telefono quel che cade nell'hero del Cielo, il committente: la
pioggia di notte a Buenos Aires funziona, il nastro, il temporale con la pioggia e «Dietro le
nuvole» pure; per la neve gli ho dato Krasnojarsk (neve prevista dalle 00 alle 08 locali, 0,3-0,6
mm l'ora), e: «Non riesco a vedere la neve nell'hero in Cielo. Dammi indicazioni precise».

### Cosa ho trovato

- **Non poteva vederla.** Resa con lo shader vero la notte di Krasnojarsk (banco chiuso, e a
  quell'ora nessuna luna che lo illumini): fra il cielo asciutto e quello con la neve a 0,4 mm l'ora la differenza è di 6
  livelli su 255 al massimo, e a 4 mm l'ora, il pieno, di 10 livelli sullo 0,29% dei pixel. La
  pioggia uguale. Con la luna piena e di giorno si vedono.
- **Perché.** Le gocce e i fiocchi sono disegnati nel colore chiaro delle nuvole (`uLit`), che di
  notte è la luce della luna: luminanza 0,093 con la luna piena alta, 0,023 senza luna, quasi
  quella del banco dietro.
- **Era una decisione, non una svista.** `docs/planning/05-ottobre-1-19.md`, «Il cielo premium:
  la review del motore e i sette passi», «Passo 5: pioggia e neve a tre piani» (19 ott 2026): «La
  pioggerella di notte senza luna resta quasi invisibile, com'era: le gocce vestono il colore
  chiaro delle nuvole e di notte niente lo illumina; è vero anche fuori, e la parola dell'hero
  dice che piove». DESIGN §3.9 lo ripeteva.

### Cosa è cambiato, e cosa cambia di quella decisione

- **Quel che cade ha una luce sua** (`SkyScene.fallLit`, `fallLight`, `FallLightFloor`,
  `floorLuminance`; lo shader la legge in `uFallLit`): il colore chiaro delle nuvole, mai sotto
  una luminanza di 0,10. Di giorno e sotto una luna alta quel colore è già sopra, e non cambia
  niente; sotto il tetto di luminanza di §3.6 come ogni pixel. Vale dove c'è lo shader: l'hero
  di Oggi, quello del Cielo, la finestra.
- **Cambia la decisione del 19 ott 2026**, per tre motivi che allora non c'erano o non erano
  misurati: (1) non è la pioggerella: è invisibile anche la neve a 4 mm l'ora; (2) «la parola
  dell'hero dice che piove» non vale per l'hero del Cielo, che dallo stesso giorno disegna quel
  che cade e in una notte senza finestra buia dice «Nuvole 100%»; (3) un disegno che l'app
  promette non c'era per metà di ogni mese.
- **Il contro-argomento, scritto perché resti**: al buio la pioggia non si vede davvero, e una
  luce che la mostra è una convenzione del disegno. È vero. È la stessa convenzione dei bordi
  accesi delle nuvole: non tocca nessun numero, e senza di lei sotto la parola «Neve» il cielo
  disegnato è un nero.
- **Il valore, 0,10, è una proposta** guardata accanto a 0,06 (la neve leggera resta quasi
  invisibile) e a 0,16 (più chiara dei fiocchi sotto la luna piena, che non deve superare):
  è la luce che le nuvole hanno sotto la luna piena. Il committente decide guardando, sulla
  tavola resa e sul telefono; cambiarlo è una costante.

### Come è stato verificato

`SkySceneTest`: la luce di quel che cade in una notte senza luna (al pavimento), sotto la luna
piena e di giorno (quella delle nuvole), sotto il tetto; `floorLuminance` (tiene un colore che
ha la luce, alza uno che non l'ha tenendone la tinta, fa un grigio del nero). `SkyTransitionTest`,
i test dello shader, `WindowStateSkyTest` e `WindowPainterTest` rilanciati. Una tavola resa con
lo shader vero, quattro valori per sei cieli (neve e pioggia leggere e fitte senza luna, neve
con la luna piena, neve di giorno), guardata. Le immagini del README ridisegnate: nessuna
cambia, perché la registrazione non ha un'ora bagnata di notte. La suite intera la lancia la CI.

### Cosa conferma solo il telefono

La casella «Quel che cade nell'hero del Cielo, sul telefono», in «Aperto», con quel che resta:
la neve di notte, e il valore della luce.

### La decisione

Vista la tavola e letto che si cambia una decisione del 19 ott 2026, il committente, lo stesso
giorno: «La tua proposta 0,10 mi convince e sono d'accordo. Cambiamo pure la decisione presa in
passato. Meglio la scelta che mi proponi oggi. Infatti nelle notti di pioggia, anche sul widget La
finestra, non si vedeva quasi mai la pioggia (e la neve se selezionavo altre località).»

- **La decisione del 19 ott 2026 è cambiata** (`docs/planning/05-ottobre-1-19.md`, «Il cielo
  premium: la review del motore e i sette passi», «Passo 5: pioggia e neve a tre piani»): quel
  che cade si vede anche in una notte senza luna.
- **Il valore è 0,10**, confermato sulla tavola resa. Sul telefono resta da vedere la neve di
  notte: la casella in «Aperto».
- **Una conferma che viene dall'uso**: la pioggia che di notte non si vedeva quasi mai nella
  finestra l'aveva già notata lui. Il difetto non era dell'hero del Cielo, che l'ha solo reso
  evidente.

### Sul telefono

Con l'APK della CI della PR #177 (`43944b92`, verde), il committente: «Sto provando e mi sembra
OK. A Ulan Bator si vede la neve nell'hero di Cielo per questa notte. Anche Krasnojarsk. A
Sapporo prima neve e poi pioggia. Direi che ora si vedono con un bel colore e una sfumatura
ottima.»

- **Chiusa «Quel che cade nell'hero del Cielo, sul telefono»**: la neve di notte in due luoghi,
  e a Sapporo la neve che lungo la notte diventa pioggia. La pioggia, il nastro, il temporale
  con la sua pioggia e «Dietro le nuvole» li aveva provati prima, con la pioggia di Buenos
  Aires; «Dietro le nuvole» sopra la neve non è nominato.
- **0,10 resta**: visto sul telefono dopo la tavola.
- In «Aperto» resta una casella sola, il cielo d'inverno contro il satellite, che aspetta
  febbraio 2027.
- **«Dietro le nuvole» anche sulla neve**, più tardi nella stessa chat: «Dietro le nuvole l'ho
  riprovato e funziona sia con neve che con pioggia.» Della casella non resta niente di non
  detto.

## «A colpo d'occhio»: l'icona cinque dp più dentro (committente, 16 nov 2026)

**La richiesta.** «Vorrei che l'icona meteo sia spostata solo pochi punti più a destra, insieme
alla temperatura attuale alla sua destra. Ora mi sembra un po' troppo vicina al bordo di sinistra
del widget», con tre domande prima di fare: se è possibile, se rovina il layout o lo spazio dei
testi e della frase, se sono d'accordo. E poi, dato l'accesso al repo di Passo: «verifica lì
l'anello a che distanza è dal bordo sinistro, ad esempio in 4×1. Forse il widget di Chiaro è già
allineato». Sulla risposta, con le misure: «Ok, procedi».

### Cosa ho trovato

- **In Passo l'anello sta a 14 dp** dal bordo della card (`WidgetCardPadding`), e il suo
  inchiostro arriva al bordo del riquadro (`RingPainter`: il tratto è inscritto).
- **In Chiaro il riquadro dell'icona parte a 4 dp** (`WidgetCardPaddingLeading`) e conta sul
  margine che ogni disegno tiene dentro di sé. Quel margine è una quota del riquadro: misurato
  sui disegni spediti, resi a 400 px, 7,5% la pioviggine, 8,3% la neve, 9,2% la nuvola e la
  pioggia, 13,3% il temporale, 15,5% il sole. Sull'icona di 52 dp che la riga ha dal 15 nov 2026
  l'inchiostro stava a 7,9-8,8 dp dal bordo per le nuvole e a 12 per il sole, sotto un nome del
  luogo che parte a 14; con i dati vecchi (icona di 37 dp) a 6,8-9,8. Il commento del 15 nov
  («l'inchiostro dell'icona e la prima lettera del luogo stanno sullo stesso bordo a 14 dp»)
  non era vero: valeva, e solo per il sole, sull'icona di 66 dp di prima.
- **Non era allineato a Passo**, quindi: fra 2 e 6 dp più vicino al bordo.

### Cosa è cambiato

- **L'icona della riga sta 5 dp più dentro** (`NowGlyphShift`, `NowHeroRow`), e il numero con
  lei: l'inchiostro di una nuvola a 13,8 dp, della neve a 13,3, della pioviggine a 12,9, del
  temporale a 15,9, del sole a 17. Le nuvole, i disegni più larghi e quelli che sembravano
  toccare il bordo, cadono sul bordo delle parole e dell'anello della famiglia.
- **Una misura sola per ogni disegno**, e non il margine di ciascuno: la temperatura non deve
  fare un passo di lato quando cambia il tempo.
- **Vale dove c'è la riga**: il 4×1, il 3×1, il 2×1 e la testa della scheda delle ore. La forma
  alta (due righe) non cambia: lì l'icona è grande e il suo margine pure.
- **L'anteprima nel selettore dei widget** (`widget_now_preview.xml`) ha gli stessi 5 dp.

### Cosa costa

- **Ai testi niente**: il nome del luogo, la frase, la massima e la minima, il chip dell'allerta
  e la riga dei dati vecchi non si muovono. I 5 dp escono dalla riga del numero, dentro la
  colonna del luogo (168 dp): icona, spazio e numero più largo fanno 148 dp invece di 143.
- **Al numero niente, sul launcher del committente**: dove è l'altezza a decidere il numero
  (riga da 82 dp: 39,3 sp a quattro celle sulla riga di riferimento, 37,0 a due) la larghezza
  ne avanza ancora. Avevo detto a lui che il 2×1 avrebbe perso un sp e mezzo: era il conto
  sulla riga di riferimento da 85 dp, non sulla sua; lo perde solo una card a due celle su
  una riga più alta di 82,5 dp.

### Come è stato verificato

`NowWidgetLayoutTest`: il caso nuovo (i 5 dp, l'inchiostro della nuvola sul bordo delle parole a
un quarto di dp, il sole a 17, i numeri di quattro, tre e due celle fermi, la riga alta a due
celle che paga), e «l'icona, lo spazio e il numero più largo stanno nella riga» con i 5 dp
dentro, a 1, 1,15 e 1,3 di carattere; tutti i test dei widget. Una tavola resa, quattro
spostamenti (0, 4, 5, 6 dp) per sei tempi e la scheda delle ore, con la linea dei 14 dp:
guardata, a 5 la nuvola sta sulla linea. `widgets.png` del README rigenerata e guardata; le
altre quindici identiche. La suite intera la lancia la CI.

### Cosa conferma solo il telefono

La casella «L'icona di "A colpo d'occhio" più dentro, sul telefono», in «Aperto».

### La scelta

Vista la tavola dei quattro spostamenti, il committente: «Va bene +5». Il valore resta 5 dp;
sul telefono la casella in «Aperto» è ancora da provare.

## La nebbia: due modelli (committente, 17 nov 2026)

**Da dove nasce.** Alle 10 del 17 nov 2026 l'app diceva «Nebbia 7°» a Cavenago di Brianza. Il
committente ha chiesto un controllo incrociato dei testimoni, senza toccare il repo; poi,
uscito di casa: «si vede lontano, niente nebbia [...] vedo tranquillamente il paese vicino»,
e l'umidità di casa sua intorno al 90% contro il 98% dell'app. Sulle misure che seguono:
ragionare già per una regola dopo il 30 novembre, «anche da zero», con la media
dell'umidità dei testimoni senza i due estremi come sua idea da valutare; poi «procedi con
l'implementazione [...] deve essere già pronta per il dopo 30 novembre, [...] non dobbiamo
toccare l'app e la regola funziona ora nello stesso modo».

### Cosa ho trovato

- **Le 10:00 a Cavenago** (richieste dell'app, scaricate alle 10:10): `best_match` era ICON-D2
  (numeri identici a `icon_d2` ora per ora), 240 m di visibilità (80, 220, 160 m dalle 07 alle
  09), 98% di umidità, vento 3 km/h, codice 45. I testimoni: IFS 1,4 km al 92%, UKMO 18 km al
  94%, KNMI 5 km al 95%, DMI 2,4 km al 97%. La regola del 27 ott (archivio 07, «14.3») chiedeva
  un testimone che vedesse la nebbia, sotto il chilometro **o al 97% di umidità**: l'unico era
  DMI, esattamente sulla soglia, con una visibilità sua di 2,4 km. Alle 10:28 una corsa nuova
  di DMI dava 95%, e la stessa ora diventava «Coperto».
- **Quello che si vedeva**: i SYNOP via Ogimet alle 10:00 (temperatura e rugiada al decimo):
  Orio al Serio 7,3/6,0 °C, 91%, 2 km in foschia; Malpensa 7,2/5,8 °C, 91%, 7 km; Linate 6 km,
  5 km in foschia alle 10:20, con rugiada uguale alla temperatura tutta la mattina (un 100% che
  con 6 km di visibilità non è credibile). Nessuna nebbia. Gli altri modelli, fuori dai
  testimoni: ICON-EU 93%, ICON-2I 85%, ICON-CH1 e CH2 94%, AROME 97%, ARPEGE 100%, AIFS 93%,
  GEM 90%, GFS 67%.
- **ICON-2I**, che il committente aveva visto indicato come il migliore per l'Italia: la misura
  14.4 (archivio 08) rifatta con lo strumento dice ancora no; aggiunte l'umidità e il vento, che
  14.4 non misurava, sugli aeroporti italiani al giorno 1: al nord ICON-2I sbaglia la
  temperatura di 1,21 °C contro 0,80 di `best_match` e l'umidità di 7,4 punti contro 5,3 (5,7
  contro 4,4 in aria quasi satura); al sud la temperatura è pari (1,01 contro 1,00), il vento un
  poco meglio (4,2 contro 4,4 km/h), l'umidità è sistematicamente bassa (−1,7) e in aria quasi
  satura sbaglia di 8,1 punti contro 5,5. Scegliere il modello principale per località resta
  quel che `best_match` già fa; nessuna cella misurata dove cambiarlo convenga.

### La misura

Sui due archivi di §11 (26 aeroporti, 45 605 ore asciutte, 1 442 nebbie viste), con lo strumento
nuovo `tools/analyze_against_metar.py --fog-votes`. In ordine di come la si è cercata:

- **La prima proposta, due testimoni sempre** (la regola del 27 ott con `FOG_WITNESSES_OWN` a 2):
  CSI 0,289 contro 0,285 sul totale, ma 0,234 contro 0,249 in pianura padana (56 nebbie vere
  perse per 167 false tolte). Ritirata.
- **La media dell'umidità senza gli estremi (l'idea del committente)**: al posto delle
  conferme di oggi peggiora (0,259); come filtro sotto due modelli che vedono sotto il
  chilometro è fra le migliori. Come stima dell'aria è peggio del modello principale da solo:
  5,58 punti dal METAR contro 5,40, in pianura padana 6,06 contro 5,33, in aria quasi satura
  5,74 contro 4,39. Filtro sì, numero a schermo no: la regola di CLAUDE.md sulle medie regge sui
  dati, e resta.
- **La maggioranza dei testimoni sotto il chilometro**: precisa (59%), ma coglie una nebbia su
  cinque (18%). Troppo prudente.
- **Come decidono i servizi che lo fanno di mestiere**: il Met Office (IMPROVER) decide l'icona
  da probabilità su più modelli; NCEP (Zhou e Du 2010, ensemble multi-modello) fa dire sì o no a
  ogni membro, con aria satura e vento debole dove non c'è una nube al suolo, e conta i sì; il
  National Blend of Models pesa la visibilità a mano, non la media. Da qui il voto per modello.

| Regola | Dette | Giuste | False (su aria limpida) | CSI | Pianura padana | Altrove |
|---|---:|---:|---:|---:|---:|---:|
| Del 27 ott, come girava (visibilità di ICON letta) | 2 205 | 808 | 1 397 (789) | 0,285 | 0,249 | 0,308 |
| Del 27 ott, da dicembre (senza ICON) | 2 019 | 764 | 1 255 (701) | 0,283 | 0,248 | 0,310 |
| **Due modelli, 98% / 12 km/h / media 93%** | **1 807** | **747** | **1 060 (579)** | **0,299** | **0,254** | **0,336** |
| Due modelli, 99% / 12 km/h / media 94% (proposta prima del ricontrollo) | 1 562 | 682 | 880 (461) | 0,294 | 0,248 | 0,332 |

Avanti in inverno (0,303 contro 0,288) e a ottobre (0,255 contro 0,241) rispetto alla regola del
27 ott come sarebbe andata da dicembre; contro quella con la visibilità di ICON, indietro solo a
ottobre (0,255 contro 0,283, 153 nebbie), e quella visibilità se ne va comunque. Le soglie stanno
su un piano (98-100%, 8-16 km/h, 93-95%: 0,285-0,299), e 98/12/93 batte la proposta 99/12/94 in
ogni fetta: totale, pianura padana, altrove, inverno, ottobre. I giorni 4-7, simulati togliendo
KNMI e DMI: 0,267 contro 0,253. Le tabelle intere sono in STATES §11, «Two models».

### La regola, da oggi

- **Ogni modello vota.** Ogni testimone con la sua visibilità a 1 km o meno; il modello
  principale con l'aria al 98% o più in un vento di 12 km/h o meno. La visibilità di
  `best_match` non si legge: prima del 30 novembre come dopo.
- **Due voti fanno la nebbia**, tenuta dal codice di nebbia del provider o persistente in
  un'ora vicina con due voti anche lei.
- **Sotto un'aria che i modelli dicono quasi satura**: la media delle umidità di tutti, senza la
  più alta e la più bassa, al 93% o più. Un filtro, mai un voto, mai un numero a schermo.
- **Nebbia gelata** a 0 °C o meno, come prima.
- **Senza testimoni non c'è nebbia**: la parola di un modello solo, visibilità, aria satura o
  codice che sia.

### Cosa è cambiato

- `WeatherStateEngine`: `fogVotes`, `fogMeanHumidityPct`, `fogPersists` sui voti, le costanti
  `FOG_SATURATED_PCT`, `FOG_CALM_KPH`, `FOG_VOTES`, `FOG_MEAN_HUMIDITY_PCT`; `Hour` perde la
  visibilità e prende il vento; via `visibilityOf`, `fogConfirmed` e le costanti della regola
  del 27 ott.
- `Witnesses`: via il riconoscimento per variabile della visibilità copiata (archivio 07, «14.3»):
  la visibilità di `best_match` non si legge, e una copia non può contare due volte.
- `WeatherReportMapper`: le righe portano umidità e vento dell'istante; il blocco `current`
  vota con la propria umidità e il proprio vento e con i testimoni della sua riga, e persiste
  sui voti delle righe.
- **Il riquadro della visibilità** (`TodayDetails`, `fogTileKm`): in nebbia stampa i chilometri
  solo se sono quelli di una nebbia, 1 km o meno; altrimenti la parola dello stato. Con due
  modelli che vedono la nebbia e il principale a 4 km, «4 km — prudenza in strada» sarebbe stato
  il riquadro che si contraddice.
- **Questo cambia una decisione d'archivio**: archivio 07, «14.2 — La nebbia senza la
  visibilità di ICON: la misura» e «14.3 — La nebbia senza la visibilità di ICON: il codice, e
  il tile», 27 ott 2026. La loro regola (la visibilità propria o la più bassa dei testimoni,
  uno o due testimoni secondo da dove viene, il voto per umidità al 97%) è sostituita da questa.
- **I documenti**: STATES §2, §3 (regola 5), §4, §5, §10 (il riquadro, e la nebbia contro la
  copertura rifatta sulla regola nuova: 155 ore su 1 898 sotto il 65% di copertura, l'8%
  contro il 7%, la decisione di allora regge), §11 «Two models», §12; CLAUDE.md, la nota sulla
  nebbia, e una riga sotto «Numbers are one model's»: uno stato può leggere più modelli, quel
  che ne legge non arriva a schermo come numero; i commenti di `OpenMeteoApis`.
- **Lo strumento**: `--fog-votes` (questa misura) e `--fog-cover` con la regola nuova come
  primo passaggio.

### Cosa costa

- **In chiamate, niente**: il vento e l'umidità del modello principale sono già nella richiesta
  (5,4), visibilità e umidità dei testimoni anche (1,6). Il totale resta 8,4.
- **Nella cache, niente**: la richiesta non cambia, e una voce letta dal disco passa dal mapper
  con la regola nuova.
- **Le nebbie che si perdono**: 2% delle vere colte contro la regola del 27 ott da dicembre;
  dove manca la risposta dei testimoni (una richiesta fallita) la nebbia non c'è.

### Come è stato verificato

- **Il motore Kotlin rieseguito sulle stesse ore** (il metodo del 14.3: un test di passaggio,
  cancellato prima del commit, che costruisce `HourlyDto` e `WitnessResponseDto` dagli archivi e
  passa per `Witnesses.of` e `WeatherStateEngine.states`): 45 605 ore, 1 807 dette, 747 giuste,
  1 060 false, **0 differenze** dal prototipo.
- I test: `WeatherStateEngineTest` riscritto per la regola 5 (Cavenago alle 10 e alle 07,
  Lissone, i voti, la media, la persistenza, il codice, la nebbia gelata);
  `WeatherReportMapperTest` (le righe e il blocco `current` con i testimoni, la visibilità di
  `best_match` letta o no che non cambia nulla, il blocco che perde il suo voto nel vento);
  `OpenMeteoResponseTest` (Sydney: senza testimoni l'alba è il suo cielo, con due la nebbia);
  `WitnessesTest`; `DetailsNotableTest` (il riquadro). `:core:domain:test` e
  `:core:data:testDebugUnitTest` verdi, salvo l'`AccessDeniedException` di DataStore su questa
  macchina, che non tocca questo lavoro. La suite intera e il lint li lancia la CI, su richiesta
  del committente: l'APK da provare è il suo.
- Le immagini del README non cambiano: la registrazione di Milano non ha un'ora sotto il
  chilometro né testimoni, prima come dopo.

### Cosa conferma solo il vivo

La casella «La nebbia dei due modelli, dal vivo dopo il 30 novembre», in «Aperto».

## Adesso e poi: l'hero decide se piove ora (committente, 17 nov 2026)

**Da dove nasce.** Cavenago di Brianza, 17 nov 2026, 12:32, pioggia debole sul posto: la pagina
diceva «Coperto» in cima e sotto «La pioggia smette tra circa 30 minuti». Il committente, sulla
proposta: «procedi con la correzione del punto 1 e apri la PR», dopo aver chiesto se il quarto
d'ora sia più preciso dell'ora (risposta sotto).

### Cosa ho trovato

- **Due quarti d'ora diversi, non un'ora e un quarto.** L'hero è il blocco `current`, il quarto
  appena finito (`interval` 900: alle 12:32 quello delle 12:15-12:30, 0,0 mm); la sezione
  «Pioggia nelle prossime 2 ore» parte dal quarto in corso (12:30-12:45, 0,1 mm, 0,4 mm/h al
  38%). Una regola c'era già in un verso (17 ott 2026, `endingNow`: hero bagnato su quarti
  asciutti, «La pioggia smette a momenti»); nell'altro verso il quarto in corso bagnato faceva
  una pioggia «in corso» sotto un hero asciutto.
- **La sola cosa oraria è la probabilità**, che vale per l'hero e per i quarti (la probabilità
  della loro ora). Il quarto d'ora ha più dettaglio dell'ora, non più precisione: i modelli
  sbagliano l'inizio e la fine di 15-60 minuti (oggi il rinforzo previsto alle 13, sul posto
  alle 12:10), e sommare su un'ora ne compensa una parte. La guida lo dice già («l'inizio e
  la fine possono spostarsi di un quarto d'ora o due»).

### Cosa è cambiato

- `NowcastEngine`: un tratto bagnato nel quarto in corso è `ongoing` solo se anche l'hero è
  bagnato; sotto un hero asciutto comincia al bordo di quel quarto. La frase dice «Pioggia
  debole a momenti, per circa 30 minuti» (e non «smette tra circa 30 minuti»), la scheda
  «dalle 12:30 alle 13:00» (e non «fino alle 13:00»), il picco con la sua ora e non «più intensa
  ora» (`NextRain.kt`). Nessuna stringa nuova: le frasi esistevano.
- Non cambia: le quantità, le probabilità, gli stati delle ore e dei quarti, l'hero, i widget,
  le notifiche, le variabili degli avvisi. Quando hero e quarto in corso sono d'accordo, nulla.
- STATES §5, «The present and the quarter of an hour»; CHANGELOG, «Fixed».

### Come è stato verificato

`NowcastEngineTest` (il caso di Cavenago: il tratto non è in corso, comincia alle 14:00 del
quarto, `endingNow` falso; i casi «in corso» con l'hero bagnato), `NextRainTextTest` (la frase
in italiano e in inglese); `:core:domain:test` e i test di Oggi verdi. La suite intera la lancia
la CI.

### Visto, e cosa ne è stato

- **La probabilità dell'hero al cambio d'ora.** Alle 13:12 l'hero è il quarto 12:45-13:00
  (0,4 mm/h), ma lo giudica con la probabilità dell'ora in corso, 13-14 (5%), non della sua,
  12-13 (38%): «Coperto» mentre pioviggina. La sezione invece giudica ogni quarto con la sua
  ora. Succede solo nel primo quarto d'ora di ogni ora. **Fatto lo stesso giorno** (il
  committente: «Fai la modifica per il punto 1»): `WeatherReportMapper` legge lo stato
  dell'hero con la probabilità dell'ora del suo quarto (lo slot che finisce alla fine del
  quarto o dopo); la probabilità detta agli avvisi (`current.precipitation.chancePct`) resta
  quella dell'ora in corso, l'unico posto dove compare. Test: `WeatherReportMapperTest`, «the
  hero's quarter is read with the chance of its own hour»; STATES §5; CHANGELOG, «Fixed».
  Sul posto, alle 13:30 circa, la pioggia aveva smesso: «Coperto» e la sezione senza pioggia
  erano giusti.
- **Una PR sola** (il committente: «Se puoi metti tutto dentro una sola PR»): questo lavoro e
  «La nebbia: due modelli» stanno nella #179; la #180, che portava solo questo, è chiusa.
- **La soglia del 20% sotto quantità vere.** Alle 13 l'ora 13-14 ha 1,6 mm al 5%: la pagina
  dice «Coperto» e non mostra la sezione, e pioviggina. Sull'archivio (§11) le ore sotto il 20%
  con almeno 0,1 mm sono bagnate al 40-50% qualunque sia la quantità; in pianura padana quelle
  da 0,5 mm in su lo sono 18 volte su 25, altrove 22 su 72: troppo poche per una regola.
  L'inverno 2026-27 ne darà di più.

## La review del motore, punto 1: l'hero dove il quarto d'ora non è calcolato, e una colonna che poteva far cadere il report (committente, 17 nov 2026)

**Da dove nasce.** Il committente ha chiesto una review completa del motore, dalla lettura di
Open-Meteo alle regole («deve restare pulito e curato e non over engineered»), e sulla lista
dei cinque punti che ne è uscita: «Direi di fare tutti i punti da 1 a 5 nell'ordine
consigliato», con una precedenza: l'app si usa soprattutto in Italia, e nel nord, poi in Europa
e nel mondo; le verifiche più accurate vanno fatte lì, e le regole devono restare valide
ovunque. Questo è il primo punto: i due difetti.

### Cosa ho trovato

- **L'hero è asciutto nelle ore da 0,1 mm, dove il quarto d'ora non è di un modello.** Fuori da
  ICON-D2 e HRRR Open-Meteo serve il quarto come l'ora divisa per quattro, e arrotonda ogni
  quarto a 0,1 mm: un'ora da 0,1 mm sono quattro quarti da 0,0. Il blocco `current` è uno di
  quei quarti (`interval` 900), e l'hero diceva «Coperto» sopra la cella «Ora» che diceva
  «Pioviggine».
- **La tabella di chi calcola il quarto arrivava più a sud dei dati.** `NowcastCoverage` dava
  per coperta l'Italia fino a 43,43° N (la griglia regolare di DWD da 43,18°, meno il margine);
  ICON-D2 su Open-Meteo risponde più a nord. A Livorno (43,55°) la chiave `icon_d2` non ha
  dati e tutte le ore bagnate di `best_match` erano piatte.
- **`hourly.precipitation` era l'unica colonna con elementi non annullabili** (`List<Double>`):
  un `null` lì faceva fallire la decodifica dell'intero report, il guasto tolto alle altre
  colonne il 19 ott 2026. La sonda non l'ha mai vista vuota (45 città, 27 ott e 2 nov): un
  difetto latente, senza un test.

### La misura (17 nov 2026, dal vivo)

- **Quanto pesa.** 24 città fuori da ICON-D2 e HRRR, 7 giorni: delle 819 ore che il motore
  chiama bagnate (≥ 0,1 mm e ≥ 20%), **184 (22%) hanno esattamente 0,1 mm**. Visto in diretta
  alle 14:00 UTC: Singapore e Bogotá con il blocco a 0,0 dentro un'ora da 0,1 mm al 47% e al
  39%; Tokyo, Kuala Lumpur e Glasgow con il blocco uguale al quarto della serie (1,2, 0,8 e
  0,4 mm), cioè `current.precipitation` è il quarto.
- **Il nord Italia non è toccato.** 20 città del nord, 3 giorni: le ore bagnate hanno una
  forma (Milano 7 su 7, Bergamo 10 su 10, Brescia 14 su 14, Bologna 11 su 11, Parma 13 su 13,
  Novara 8 su 8; un'ora da 0,1 mm è un quarto da 0,1 e tre da 0,0), e i quarti sommano
  all'ora in quasi tutte.
  Le poche piatte (Aosta 13 su 41, Bolzano 5 su 9, Trieste 6 su 24) non sono state separate
  fra il terzo giorno, oltre le 48 ore di ICON-D2, e il resto: il blocco `current` non è mai
  lì. A sud dei dati di ICON-D2 tutte le ore bagnate sono piatte: Livorno 18 su 18, Siena 18
  su 18, Perugia 22 su 22, Roma 19 su 19, Napoli 8 su 8, Bari 5 su 5.
- **Il bordo sud di ICON-D2.** `models=icon_d2` lungo sei meridiani da 7,6° a 13,4° E: a 43,7° N
  risponde su tutti, a 43,6° su due (11,8° e 13,4°), a 43,5° e più giù su nessuno. Ancona
  (43,62°) e Tolosa (43,60°), misurate calcolate dalla sonda del 27 ott, restano dentro.
- **Trovato, e poi spiegato.** A 44,0° N / 10,3° E e 11,8° E, e a 43,8° N / 7,6° E e 11,8° E,
  ICON-D2 ha dati e `best_match` ne differisce in 3–20 quarti su 180. Scritto qui come una
  domanda aperta («un altro modello in quota?»); rimisurato lo stesso giorno, non lo è: i
  quarti diversi sono tutti nel terzo giorno, da 32 ore avanti alla fine della corsa di ICON-D2
  (il 19 nov dalle 00:30 alle 12:00, misurando alle 16 del 17), per un decimo di millimetro
  (1,8 contro 1,9), dove `best_match` passa la mano. Nelle 48 ore dei due giorni di calendario
  non ne differisce uno, in nessuno dei punti e nemmeno a Castelnuovo di Garfagnana, Barga,
  Massa, Viareggio, Forlì, Firenze, Ancona e Bologna. L'hero legge un quarto e la sezione sei
  ore: non li riguarda. La mia prima scansione confrontava una finestra che arrivava a 45 ore.

### Cosa è cambiato

- **La regola** (`WeatherReportMapper.cutInFour`, STATES §5): la richiesta chiede anche i tre
  quarti prima di quello del blocco (`past_minutely_15=3`, `PAST_QUARTERS`), così i quattro
  quarti dell'ora del blocco ci sono sempre. **Tutti e quattro uguali, e nessun nowcast nel
  luogo**: lo stato dell'hero legge precipitazione, neve e rovesci di quell'ora, i numeri della
  striscia, con la stessa probabilità. Altrimenti il blocco tiene il suo quarto, com'era.
  Letta dalla serie e non dalla tabella: vale ovunque nel mondo senza sapere quale modello c'è.
- **Dove l'app legge i quarti l'hero non cambia**, nemmeno in un'ora i cui quarti arrotondano
  tutti a zero: lì le frasi della sezione sono scritte contro il quarto del blocco (17 nov
  2026, «Adesso e poi»), e un hero bagnato sopra quarti asciutti direbbe «La pioggia smette a
  momenti» per un'ora. Resta quindi, nei luoghi con il nowcast, il caso raro dell'ora da
  0,1 mm senza un quarto bagnato: l'hero asciutto sopra la cella, com'era.
- **La tabella**: `NowcastCoverage.IconD2` parte da 43,35° N, che con il margine fa 43,6°.
  Livorno e Arezzo escono; Ancona, Firenze e Tolosa restano.
- **`HourlyDto.precipitationMm` è `List<Double?>`.** Un'ora senza quantità legge cosa cade dal
  codice del provider (STATES §4, «no amount») e non aggiunge millimetri al suo giorno.
- **Non cambia**: la quantità stampata per l'ultimo quarto (`lastQuarterHourMm`, del blocco),
  la probabilità detta agli avvisi, gli stati delle ore e dei quarti, la sezione delle
  prossime due ore dove c'era.

### Cosa costa

Nessuna variabile in più: tre quarti d'ora di due variabili nella risposta, e il peso della
richiesta resta 5,4 chiamate: letto sul contatore di Open-Meteo (`X-RateLimit-Cost`) con la
richiesta dell'app, con e senza i tre quarti. `REQUEST`
cambia, e le voci del disco scaricate prima non sono un HIT fresco: alla prima apertura si
riscarica. Sette finti client dei test prendono il parametro nuovo, e la sonda
(`tools/probe_provider.py`) lo manda come l'app.

### Come è stato verificato

`WeatherReportMapperTest`: l'ora piatta fuori copertura (hero e cella «Ora» tutti e due
pioviggine, la quantità del quarto resta 0,0), l'ora con una forma (il quarto asciutto è
asciutto, quello bagnato è 0,4 mm/h), il luogo con il nowcast (l'hero tiene il suo), l'ora di
cui la serie non ha i quattro quarti, e la colonna con un `null` passata per il decodificatore
e tornata. `NowcastCoverageTest`: Livorno e Arezzo fuori, le città già misurate dove erano.
Lanciati qui: `:core:data` (i 122 test del mapper, della cache e del repository) e
`:core:domain` (`NowcastCoverageTest`), verdi.

### Cosa conferma solo il vivo

Un luogo fuori dal nord (Roma, Napoli, una città fuori dall'Europa) in un'ora di pioggia
debolissima: l'hero che dice «Pioviggine» come la cella «Ora». La trovo io con Open-Meteo il
giorno della prova.

## La review del motore, punto 2: quel che si scarica e nessuno legge, e le ore di pioggia del giorno (committente, 17 nov 2026)

Il secondo dei cinque punti della review (sopra, «punto 1», da dove nasce).

### Cosa ho trovato

- **Sei variabili dell'aria scaricate, mappate e mai lette.** `pm2_5`, `pm10`, `ozone`,
  `nitrogen_dioxide`, `sulphur_dioxide`, `carbon_monoxide`: finivano in `Pollutants`, che
  nessuna schermata, nessun avviso e nessuna regola leggeva (solo i test e il report di
  esempio). Lo diceva il suo stesso commento dal 24 set 2026: «No screen reads these yet».
- **`current.is_day` mai letto**: il giorno e la notte sono del motore astronomico; quello
  orario resta, lo legge l'etichetta del giorno.
- **Tre campi di dominio calcolati per ogni report e mai letti**: `Wind.directionCompass` (le
  schermate disegnano la direzione dai gradi), `SystemInfo.responseTimeMs` (misurato, scritto
  anche su disco), `Pollutants`. Eredità di tweather, che li stampava nel suo JSON.
- **`precipitation_hours` è un conteggio che l'app può fare da sé, e che non è quello della
  striscia.** È il numero delle ore con una quantità, a qualunque probabilità (su tutte le
  giornate misurate coincide con le ore a più di 0 mm); l'app lo stampava accanto ai
  millimetri del giorno («6 mm in 5 ore») sopra una striscia le cui ore bagnate sono quelle
  del motore (≥ 0,1 mm e ≥ 20%), anche nei giorni in cui la striscia non ne ha nessuna.

### La misura (17 nov 2026, dal vivo, una settimana di previsione)

| | Giorni con un'ora bagnata | Stesso conteggio | Diversi di ≥ 1 ora | Di ≥ 2 ore |
|---|---:|---:|---:|---:|
| Nord Italia, 20 città | 58 | 36 (62%) | 22 (38%) | 6 (10%) |
| Resto d'Italia e mondo, 24 città | 90 | 51 (57%) | 39 (43%) | 21 (23%) |

Il provider conta sempre di più, mai di meno. I casi peggiori al nord: Torino il 22 nov, 9
ore del provider e nessuna del motore su 2,4 mm (tutte sotto il 20%: la striscia non ha
un'ora bagnata, l'etichetta è un cielo, e la riga diceva «2,4 mm in 9 ore»); Cuneo lo stesso
giorno 6 e 0; Venezia e Padova oggi 8 e 5. Fuori: Atene 9 e 0, Londra 11 e 4, New York 16 e 10.
Il motore è approssimato in Python (quantità e probabilità; non i codici di temporale e di
gelicidio, che non cambiano il conto di un'ora già bagnata).

### Cosa è cambiato

- **Le richieste.** Aria: 8 variabili al posto di 14 (i due indici e i sei pollini).
  Previsione: 52 al posto di 54 (via `current.is_day` e `daily.precipitation_hours`).
- **Le ore di pioggia del giorno le conta l'app, e sono quelle del modello**
  (`WeatherReportMapper.precipHours`, `DailyForecast.precipHours`, ora un intero): le ore del
  giorno a cui il modello dà una quantità, le stesse di cui i millimetri accanto sono la
  somma, senza chiedere la variabile. **Non sono dette nei giorni senza un'ora bagnata**: lì
  restano i millimetri soli. Le due superfici che le stampano (il riquadro della pioggia nei
  dettagli, il giorno aperto nella settimana) non cambiano regola: da un'ora in su, sulla
  riga della pioggia o della neve quando è l'unica. Sotto, «Riletto con il committente»: la
  prima versione contava le ore bagnate del motore, ed era sbagliata.
- **Via dal dominio**: `Pollutants` e `AirQuality.pollutants`, `Wind.directionCompass` con
  `WeatherCodes.windCompass`, `SystemInfo.responseTimeMs` (anche da `ReportDiskCache.Entry` e
  dal repository, che non cronometra più la richiesta).
- **Non cambia**: nessuna schermata perde un dato che mostrava. L'indice dell'aria e i pollini
  sono quelli di prima.

### Cosa costa: la somma nuova

| Richiesta | Prima | Ora |
|---|---:|---:|
| Previsione | 54 variabili, 5,4 | 52 variabili, **5,2** |
| Testimoni | 16, 1,6 | 16, **1,6** |
| Aria | 14, 1,4 | 8, **1,0** |
| **Un fetch** | **8,4** | **7,8** |

Previsione e testimoni letti sul contatore di Open-Meteo (`X-RateLimit-Cost`: 5,2 e 1,6);
l'host dell'aria non espone il contatore, e il suo 1,0 è l'aritmetica della pagina dei prezzi
(una chiamata fino a dieci variabili). Sono 0,6 chiamate in meno a ogni fetch, per ogni luogo
e per ogni regola legata a un luogo (15.11). Aggiornati CLAUDE.md, VISION §5.4, STATES §12 e i
commenti accanto alle liste. `REQUEST` cambia: le voci del disco di prima non sono un HIT
fresco, e restano leggibili offline (`ignoreUnknownKeys`).

### Come è stato verificato

`WeatherReportMapperTest`: «the day's rain hours are the hours with an amount, as its
millimetres are» (tre ore sotto il 20%, un'ora di pioggia e una senza probabilità: due ore
bagnate, cinque ore con una quantità, e la nota dice cinque; nove ore tutte sotto il 20%, il
caso di Torino: nessuna nota; un giorno asciutto e uno che le ore non raggiungono: nessuna).
`OpenMeteoResponseTest`, Reykjavik del 24 set: 16 ore, come il provider. Tolti i
due test degli inquinanti e quello della rosa dei venti, con i campi che affermavano.
Lanciati qui: `:core:domain` (485), `:core:data` (325), `:core:sync` (34), verdi, e i test
dell'app compilano. Le immagini del README non cambiano: la Milano registrata ha zero ore di
pioggia in tutta la settimana, per il provider e per il motore, e nessun quarto d'ora.

### Riletto con il committente lo stesso giorno: la nota «X mm in N ore»

**L'obiezione.** Prima della PR il committente ha chiesto di rivedere il conteggio: nella
prima versione di questo punto le ore erano quelle che il motore chiama bagnate, e la frase
metteva insieme due cose diverse, i millimetri di tutto il giorno e le ore delle sole ore
sopra il 20%. «Prima la frase era coerente al suo interno ma non con la striscia. Adesso
combacia con la striscia ma non è più coerente dentro di sé»: se di nove ore ne resta una, la
frase dice «2,4 mm in 1 ora», e chi legge capisce un acquazzone.

**La mia opinione: ha ragione, e l'errore era mio.** L'avevo visto e l'avevo liquidato
scrivendo «i millimetri accanto restano il totale del modello: un numero non è mai dell'app».
Proteggeva il numero e non la frase: accanto a una quantità, il conto delle ore fa parte
dell'affermazione sulla quantità, è un numero e non uno stato. Avevo applicato «bagnato ha una
definizione sola» a una cosa che non è uno stato.

**La misura**, sullo stesso campione (20 città del nord, 24 nel resto del mondo), una
settimana di previsione, giorni con la riga della pioggia:

| | Nord: nota in | cambia da prima | i mm cambiano | intensità oltre l'ora più forte del giorno | ore a ≥ 2 dalla striscia |
|---|---:|---:|---:|---:|---:|
| Prima (ore del provider) | 59 di 59 | – | – | 0 | 3 |
| Ore bagnate del motore (la prima versione) | 51 | 24 | 0 | 2 | 0 |
| Strada 1: millimetri e ore delle ore bagnate | 51 | 33 | 30 | 0 | 0 |
| **Strada 2: ore del modello, niente nota senza ore bagnate** | 51 | 8 | 0 | 0 | 3 |

| | Resto: nota in | cambia da prima | i mm cambiano | intensità oltre l'ora più forte del giorno | ore a ≥ 2 dalla striscia |
|---|---:|---:|---:|---:|---:|
| Prima (ore del provider) | 90 di 90 | – | – | 0 | 16 |
| Ore bagnate del motore (la prima versione) | 84 | 37 | 0 | 3 | 0 |
| Strada 1: millimetri e ore delle ore bagnate | 84 | 45 | 44 | 0 | 0 |
| **Strada 2: ore del modello, niente nota senza ore bagnate** | 84 | 6 | 0 | 0 | 16 |

«Oltre l'ora più forte del giorno» è il criterio più severo che ho trovato per «un'intensità
che non c'è»: millimetri diviso ore dà più di quanto il modello mette in qualunque ora di quel
giorno. Rifatta su due settimane (una indietro e una avanti, 616 giornate, 320 con la riga
della pioggia) per avere più casi: la prima versione lo fa in 7 note su 129 al nord e 8 su
170 altrove (e supera di una volta e mezza l'intensità delle ore bagnate in 6 e 5); la strada
1 cambia i millimetri del giorno in 71 giorni su 141 e 78 su 179; la strada 2 toglie la nota
in 12 e 9 giorni e non sbaglia mai l'intensità. I casi della prima versione: Cuneo, 10 nov,
10,0 mm in sei ore di cui due bagnate, «10,0 mm in 2 ore» con l'ora più forte a 3,1 mm;
Udine, 11 nov, «2,3 mm in 3 ore» con 0,8 mm nelle tre ore bagnate; Milano, oggi, «1,0 mm in
1 ora» con 0,3 mm nell'ora bagnata; Londra, 23 nov, «3,7 mm in 4 ore» con nessuna ora sopra
0,5 mm. Il motore nella misura è approssimato in Python, come nella prima.

**La scelta: la strada 2, senza la chiamata in più.**

- **Non la strada 1**: è l'unica coerente con la striscia e dentro la frase, ma cambia la
  pioggia del giorno in un giorno bagnato su due. Il totale del giorno è un numero del
  modello, e la regola che non si rompe mai è che l'app non cambia un numero.
- **La strada 2** tiene la frase com'era, vera dentro di sé (millimetri e ore sulle stesse
  ore), e toglie il solo caso che la striscia smentiva del tutto: le ore dette sopra un
  giorno senza un'ora bagnata.
- **Senza richiedere `precipitation_hours`**: su 616 giornate su 616 è il numero delle ore con
  una quantità maggiore di zero nella serie oraria, che l'app ha già. Lo conta
  `WeatherReportMapper.precipHours`: stesso numero, nessuna variabile, e la previsione resta
  a 52 variabili e 5,2 chiamate. Se un giorno Open-Meteo cambiasse la sua definizione, la
  nostra resta quella scritta: le ore di cui i millimetri sono la somma.
- **Cosa resta, e va detto**: il conto è del modello e le celle bagnate della striscia sono
  dell'app. Dove il motore ha almeno un'ora bagnata ma ne scarta altre sotto il 20%, la frase
  dice più ore di quante la striscia ne disegni: due o più in 3 note su 51 al nord e 16 su 84
  altrove (su due settimane 16 su 129 e 26 su 170). Sono due affermazioni vere su due cose
  diverse, i numeri del modello e gli stati dell'app; la prima versione ne faceva una falsa.

### Trovato e non toccato

- **`daily.weather_code`** serve solo ai giorni che le ore non raggiungono, e con sette giorni
  di ore e sette di giorni non ce ne sono: 0,1 chiamate per un ripiego che non scatta. Non
  l'ho tolto: è la rete sotto un giorno le cui ore arrivano tutte senza stato.
- **`tools/measure_states.py`** chiede ancora `precipitation_hours` nella sua lista, che è sua:
  non toccato, perché una modifica a quel file lancia la misura e ne scrive il risultato sul
  ramo e su main.

## La review del motore, punto 3: l'ombrello è un'ora bagnata, non una probabilità (committente, 17 nov 2026)

Il terzo dei cinque punti della review (sopra, «punto 1», da dove nasce).

### Cosa ho trovato

- **«Pioggia in arrivo» leggeva solo la probabilità** (≥ 70% entro sei ore), non lo stato
  dell'ora: suonava anche sulle ore che la striscia disegna «Pioggia possibile», niente di
  misurabile sotto una probabilità alta.
- **La stessa soglia aveva quattro lettori**, e «l'app non deve avere due opinioni su cosa
  conta come pioggia in arrivo» (`EveningDetails`): la notifica, la sua finestra
  (`AlertDetails.rainWindow`), la riga di domani nel riepilogo della sera
  (`EveningDetails.tomorrowRain`) e la frase di Oggi «Ombrello verso le…»
  (`HeadlineEngine`). Oggi diceva «Ombrello verso le 16» sopra una cella che diceva
  «Pioggia possibile».
- **La notifica diceva «pioggia» anche sulla neve.** «Pioggia in arrivo · Bormio», «Ombrello
  verso le 8: pioggia 80%», sopra un'ora di neve debole o moderata (la neve forte è del
  «Maltempo»). Nell'archivio d'inverno, fuori dall'Italia, sono neve o pioggia mista a neve 454
  delle 1 720 ore da ombrello (26%); negli aeroporti della pianura padana 9 su 893, ma i
  luoghi di montagna non sono nell'archivio.

### La misura (i due archivi di STATES §11, 52 aeroporti-periodo con la probabilità)

Per ora, contro il METAR dell'ora:

| Un'ora con probabilità ≥ 70% | Ore | Bagnata nell'ora | Entro un'ora prima o dopo |
|---|---:|---:|---:|
| Tutte (la regola fino a oggi) | 5 715 | 80% | 92% |
| Bagnate per il motore (la regola nuova) | 4 859 | 84% | 94% |
| Niente di misurabile, 70–79% | 301 | 42% | 72% |
| Niente di misurabile, ≥ 80% | 555 | 61% | 83% |
| Pianura padana: bagnate per il motore | 1 062 | 91% | 98% |
| Pianura padana: niente di misurabile, ≥ 80% | 86 | 45% | 71% |
| Pianura padana: niente di misurabile, ≥ 90% | 47 | 51% | 74% |

Dove l'avviso suona, cioè sulla prima ora di un tratto (`firstArrival`, un'ora di pausa non
lo spezza):

| | Arrivi | Bagnata l'ora detta | Entro un'ora prima o dopo | Pioggia entro sei ore |
|---|---:|---:|---:|---:|
| Fino a oggi, tutti | 698 | 68% | 87% | 93% |
| **Regola nuova, tutti** | 694 | **74%** | **91%** | **95%** |
| Fino a oggi, pianura padana | 123 | 66% | 86% | 93% |
| **Regola nuova, pianura padana** | 119 | **79%** | **94%** | **96%** |
| Fino a oggi, resto d'Italia | 141 | 55% | 77% | 90% |
| Regola nuova, resto d'Italia | 147 | 55% | 78% | 90% |
| Fino a oggi, fuori dall'Italia | 434 | 73% | 90% | 94% |
| Regola nuova, fuori dall'Italia | 428 | 80% | 94% | 97% |

Gli avvisi restano quasi gli stessi di numero, perché il tratto suona comunque, sulla sua
prima ora bagnata: cambia l'ora che dicono, giusta più spesso. I tratti di oggi che tacciono
del tutto sono 63 su 698 (11 in pianura padana): durante quei tratti ha piovuto nel 49% dei
casi (3 degli 11 padani). Un secondo gradino («possibile» dall'80% o dal 90%) non regge in
pianura padana, dove quelle ore sono bagnate una volta su due a qualunque probabilità: la
regola è una sola, senza eccezioni.

### Cosa è cambiato

- **`AlertEngine.isUmbrella`**: un'ora il cui stato è una precipitazione, con probabilità
  ≥ 70%. La leggono `findPrecipitation`, `AlertDetails.rainWindow`,
  `EveningDetails.tomorrowRain` e le due frasi di Oggi («Ombrello verso le…», «Domani ombrello
  dalle…»). Il parametro `thresholdPct` delle due finestre, che nessuno passava, va via.
- **Un'ora «possibile» al 70% o più** non è più l'ombrello: su Oggi scende al gradino che
  c'era già, «Possibile pioggia verso le…» (dal 50%), e la notifica non suona.
- **La notifica dice cosa cade**, dallo stato dell'ora: «Neve in arrivo · Bormio», «Neve verso
  le 8:00: probabilità 80%.»; «Pioggia e neve in arrivo»; la pioggia com'era. Sei stringhe
  nuove per lingua, in italiano senza un articolo davanti al numero (`ItalianArticleTest`:
  «all'80%» e «al 70%» non stanno in una stringa sola; la suite l'ha trovato).
- **Non cambia**: la soglia (70%), l'orizzonte (sei ore), le due volte al giorno, il silenzio
  accanto al «Maltempo», il verdetto del Cielo (che legge la probabilità per un'altra domanda:
  se il cielo si vedrà).
- STATES §7; CHANGELOG, «Changed» e «Fixed».

### Come è stato verificato

`AlertEngineTest`: un'ora «possibile» al 90% e un cielo al 90% non suonano, il tratto suona
sulla sua prima ora bagnata, la neve suona con il suo stato; i test che volevano dire «pioggia
in arrivo» ora lo dicono con un'ora di pioggia (`rain(ora, probabilità)`). `HeadlineEngineTest`:
«l'ombrello è un'ora che il motore chiama bagnata», e la neve «possibile» all'80% è un «forse».
`AlertDetailsTest`: la finestra finisce dove finiscono le ore bagnate. `AlertNotifierTest`: i
tre titoli e le tre frasi, e la frase senza ora. Lanciati qui: `AlertEngineTest` (44),
`HeadlineEngineTest`, `AlertDetailsTest`, `EveningDetailsTest`, `AlertNotifierTest` (22), verdi.
L'immagine del riepilogo nel README non cambia: la Milano registrata è asciutta.

### Trovato e non toccato

- **Le parole dell'interruttore.** Su Avvisi la riga dice «Pioggia nelle prossime ore · Pioggia
  oltre il 70% di probabilità entro 6 ore», il canale «Pioggia in arrivo», la guida «pioggia
  oltre il 70% entro 6 ore»; da oggi la notifica dice anche «Neve in arrivo». Lasciate qui come
  proposta, e **fatte lo stesso giorno nella stessa PR** su richiesta del committente: un
  comportamento cambiato sotto le parole di prima è la schermata che non dice il vero
  (sotto, «Le quattro proposte rilette»).
- **Nel sud d'Italia l'ora detta è giusta una volta su due** (55%, e 77% entro un'ora), prima
  e dopo: lì la pioggia d'autunno è a rovesci, e l'ora di un rovescio è la cosa che un modello
  sbaglia di più. Nessuna regola: è il limite del dato.

## La review del motore, punto 4: temporale e gelicidio contro quel che si è visto (committente, 17 nov 2026)

Il quarto dei cinque punti della review (sopra, «punto 1», da dove nasce): le regole 1 e 2 del
motore passano il codice del provider ed erano le uniche due regole di pericolo mai misurate
contro l'osservato. La soglia del 20% del temporale (24 set 2026) era stata misurata contro la
pioggia di una rianalisi, non contro i fulmini.

### La misura

- **Lo strumento**: `tools/measure_hazards.py` scarica per aeroporto e periodo quel che
  `best_match` aveva previsto (codice, quantità, temperatura, probabilità, CAPE, lifted index,
  inibizione, potenziale di fulmini) e dei METAR solo l'ora, i fenomeni e la temperatura;
  `tools/analyze_against_metar.py --hazards` fa i conti, e legge anche gli archivi `metar-*`.
  I file si chiamano `hazards-*` di proposito: il workflow `Measure against METAR` completa
  ogni `metar-*.json.gz` con tutti i modelli e il satellite, che qui non servono.
- **I dati**: estate 2026 (1 giu – 31 ago) ai 26 aeroporti e a 13 fuori dall'Europa (Miami,
  Atlanta, Houston, Denver, Chicago, Città del Messico, Bogotá, Singapore, Kuala Lumpur,
  Bangkok, Hong Kong, Tokyo, Delhi), ottobre 2026, e l'inverno dell'archivio che c'era.
  142 838 ore con un METAR. Scaricati da qui: 256 chiamate per l'estate, 56 per ottobre.
- **La verità**: TS alla stazione nell'ora («giusta»); TS o VCTS in quell'ora, in quella
  prima o in quella dopo («entro un'ora»); FZRA o FZDZ per il gelicidio.

### Il temporale: cosa ho trovato

| Pianura padana, ora per ora | Dette | Giuste | Entro un'ora | Viste | CSI |
|---|---:|---:|---:|---:|---:|
| Il codice con la soglia del 20% (fino a oggi) | 148 | 60 (41%) | 104 (70%) | 250 | 0,18 |
| **Il codice a qualunque probabilità** | 220 | 101 (46%) | 163 (74%) | 250 | 0,27 |

| Giorni di temporale | Detti | Giusti | Visti | CSI |
|---|---:|---:|---:|---:|
| Pianura padana, con la soglia | 70 | 52 (74%) | 157 | 0,30 |
| **Pianura padana, senza** | 109 | 85 (78%) | 157 | 0,47 |
| Tutti i 39 aeroporti, con la soglia | 379 | 225 (59%) | 824 | 0,23 |
| **Tutti i 39 aeroporti, senza** | 436 | 265 (61%) | 824 | 0,27 |

- **La soglia toglieva temporali veri.** In pianura padana 39 giorni di temporale su 109, e in
  33 di quei 39 il temporale c'è stato (85%): più spesso che nei giorni che teneva (74%).
- **La probabilità di pioggia non è una probabilità di fulmini.** Per probabilità dell'ora con
  il codice di temporale, tuoni entro un'ora: sotto il 5%, 48%; 5–9%, 71%; 10–19%, 64%;
  20–39%, 44%; 40–69%, 29%; dal 70%, 34%. Scende mentre la probabilità sale: una probabilità
  bassa è il temporale d'estate che un modello a scala di nube mette sul punto e un ensemble
  non può confermare; una alta è un fronte con dentro un codice di temporale.
- **Dove la soglia agiva**: 115 ore, 90 ai dieci aeroporti serviti da ICON-D2 (pianura padana,
  Monaco, Zurigo, Vienna), con tuoni entro un'ora nel 71%, come le ore che teneva (70%).
  Altrove 25 ore, 4 con tuoni vicini: toglierla lì non cambia quasi niente.
- **Il sud d'Italia**: 230 ore dette, 24 giuste (10%), 60 entro un'ora (26%), quasi tutte
  d'autunno e d'inverno e ad alta probabilità. È il punto debole del codice, e la soglia non
  lo toccava (6 ore).
- **CAPE e potenziale di fulmini, misurati e non letti.** Una soglia di CAPE sotto il codice
  toglie solo temporali. Il potenziale di fulmini di ICON-D2 da 5 J/kg è il segno più sicuro
  che ci sia (19 giuste su 26 in pianura padana, 24 entro un'ora) e vede un'ora di temporale
  su tredici; aggiunto al codice porta 15 ore, 12 giuste, in un'estate su sette aeroporti.
  Una variabile in più per quello: no.

### Trovato, e non toccato: fuori dalla famiglia ICON il provider non dice mai «temporale»

Ore dette dal codice / ore con tuoni alla stazione, estate 2026: Miami 0 / 142, Atlanta 1 / 78,
Chicago 0 / 70, Houston 0 / 64, Denver 0 / 57, Tokyo 0 / 31, Kuala Lumpur 20 / 164, Singapore
3 / 67, Oslo 0 / 23, Amsterdam 0 / 18, Helsinki 0 / 12, Stoccolma 0 / 8, Londra 0 / 3. Dove
`best_match` è HRRR, GFS, il Met Office, KNMI, MET Nordic o JMA, Chiaro non dice mai
«Temporale»: non lo scrivevo nella review perché non lo sapevo, e STATES §1 diceva il
contrario («il codice di temporale viene da CAPE e lifted index»: è nel sorgente di
Open-Meteo, non nei dati). Dove il codice c'è fuori dall'Europa (Hong Kong 278 / 95, Delhi
111 / 77, Bangkok 90 / 55) è ICON globale.

Un temporale **dedotto** (CAPE ≥ 500 J/kg sopra una quantità misurabile e probabile), fuori
dall'Europa: 585 giorni detti, 318 giusti (54%), su 447 visti, dove il codice ne dice 190 e ne
prende 109 (57%); ora per ora 4 480 dette, 465 giuste (10%), 914 entro un'ora (20%). Vede tre
volte i giorni di temporale con la stessa precisione sul giorno, e sbaglia l'ora nove volte su
dieci. **Proposta, non decisa**: è una variabile in più (`cape`, +0,1 chiamate), un pericolo
che l'app inventerebbe (la regola 1 dice «né inventa né toglie»), e va deciso prima come si
dice: un «Temporali possibili» della giornata, senza l'ora e senza la notifica di «Maltempo»,
è quel che i numeri reggono. Dal 17 nov 2026 è fra le cose «Da decidere: se e quando», con
il no di adesso e i suoi motivi (sotto, «Le quattro proposte rilette»).

### Il gelicidio: cosa ho trovato

Inverno (dic 2025 – gen 2026), 26 aeroporti, 121 ore di gelicidio viste:

| | Dette | Giuste | Entro un'ora |
|---|---:|---:|---:|
| **Regola 2: il codice, a qualunque probabilità** | 34 | 12 (35%) | 16 (47%) |
| Il codice con una soglia del 20% | 19 | 7 (37%) | 8 (42%) |
| Il codice con almeno 0,1 mm | 21 | 8 (38%) | 9 (43%) |
| Dedotto: liquido ≥ 0,1 mm a 0 °C o meno | 145 | 22 (15%) | 34 (23%) |
| Dedotto, con la soglia del 20% | 111 | 17 (15%) | 23 (21%) |

- **La regola 2 resta com'è.** Una soglia toglie ore giuste e non aggiunge precisione.
- **Il gelicidio dedotto resta spento**, ed era la domanda aperta in STATES §10 dal 25 set
  2026: ha ragione un'ora su sei, e accenderebbe «Maltempo» 145 volte per 22.
- **Lo stesso limite del temporale**: il codice è della famiglia ICON. Vienna 15 dette, 13
  giuste entro un'ora, 50 viste; Monaco 16 / 3 / 8; Helsinki 0 dette su 45 viste, Longyearbyen
  0 su 13. In pianura padana quell'inverno nessuna ora di gelicidio, né detta né vista.

### Cosa è cambiato

- **Regola 1**: il codice di temporale vale a qualunque probabilità di pioggia
  (`WeatherStateEngine`); sopra un'ora senza niente di misurabile resta «Temporali possibili».
- **«Maltempo»**: `AlertEngine.severeBucket` tiene la soglia del 20% solo per il nubifragio
  (65, 82), l'unica classe che la misura del 24 set regge ancora; temporale, ghiaccio e neve
  forte a qualunque probabilità. La leggono il banner, la frase di Oggi, la notifica,
  l'etichetta del giorno e `wmo_severe` delle regole del lettore.
- **Non cambia**: la regola 2, il gelicidio dedotto (spento), le richieste (nessuna variabile
  in più: resta 7,8 a fetch).
- STATES §3 (regola 1), §6 (passo 1), §10, §11 («The provider's hazards»); CHANGELOG, «Changed».

### Cosa cambia per chi usa l'app, e il contro-argomento

D'estate, nel nord Italia, la settimana mostra «Temporale» in più giorni (in pianura padana
109 contro 70 su un'estate di sette aeroporti) e la notifica di «Maltempo» suona di
conseguenza. Sono giusti 78 volte su 100 al giorno, e 74 su 100 entro un'ora dall'ora detta.
**Contro**: un giorno su cinque di quelli è un temporale che sul posto non arriva, e chi
aveva trovato la notifica discreta la troverà più frequente; è la stessa proporzione dei
temporali che l'app già diceva, non peggiore. La misura è un'estate sola e aeroporti di
pianura: le valli alpine non ci sono.

### Come è stato verificato

`WeatherStateEngineTest` («a storm code is a storm at any chance of rain»), `AlertEngineTest`
(il nubifragio improbabile non è severo, il temporale sì; arriva alla sua prima ora),
`WmoCodeTest`, `RuleVariablesTest` (`wmo_severe`), `WeatherReportMapperTest` («a storm labels
its day at any chance of rain»), `HeadlineEngineTest` («a storm at 10 percent is the headline
too»). Lanciati qui: `:core:domain` (487), verdi, e i test del mapper e di Oggi.

### Cosa conferma solo il vivo

La prossima estate: un pomeriggio di temporali isolati in pianura con la probabilità sotto il
20%, e la pagina che dice «Temporale» dove prima diceva il cielo.

## La review del motore, punto 5: il polline sulla media del giorno; «Velato» misurato e proposto (committente, 17 nov 2026)

L'ultimo dei cinque punti della review (sopra, «punto 1», da dove nasce). Dei due lavori uno
è fatto, il polline; l'altro, «Velato», è misurato e proposto, e **non è fatto**: il perché
è sotto.

### Il polline: cosa ho trovato, e la misura

Le classi di MeteoSwiss sono scritte per la **concentrazione media del giorno**; l'app le
applicava al conteggio dell'ora corrente (lo diceva il commento di `PollenSpecies` dal 24 set
2026: «the same approximation the old scale made»). La richiesta dell'aria era solo `current`.

Misurato sulla stagione 2026 (1 feb – 30 set), ora per ora, dall'API dell'aria di Open-Meteo
(CAMS Europa), la classe dell'ora contro la classe della media del giorno, per famiglia:

| | Nord Italia, 12 città | Resto d'Italia ed Europa, 12 città |
|---|---:|---:|
| Giorni-famiglia in stagione | 4 764 | 3 851 |
| Giorni le cui ore attraversano due classi o più | 78% | 78% |
| Giorni le cui ore ne attraversano tre o più | 19% | 19% |
| Ore che mostrano una classe diversa da quella del giorno | 26% | 26% |
| …più bassa / più alta | 15% / 11% | 16% / 10% |
| In un giorno «alto» o «molto alto», ore mostrate «moderato» o meno | 33% | 30% |
| Per famiglia: alberi / graminacee / erbe | 29% / 18% / 41% | 33% / 16% / 35% |

Un esempio, Milano, 2 aprile 2026, betulla: 8 granuli alle 16, 104 alle 19; l'app diceva
«Bassi» dopo pranzo e «Alti» la sera, e la media del giorno è 28, «Moderati». Le erbe, cioè
l'ambrosia della pianura padana, sono la famiglia che cambia di più.

### Il polline: cosa è cambiato

- **La richiesta dell'aria**: i due indici restano `current`; i sei pollini sono `hourly` per
  oggi (`forecast_days=1`, il giorno del luogo). Otto variabili, **1 chiamata**, come dopo il
  punto 2: il fetch resta 7,8.
- **Il livello è della media del giorno** (`WeatherReportMapper.toPollenReport`): per specie
  la media delle ore, almeno dodici o la specie non ha un livello; poi il peggiore per
  famiglia, com'era. `AirQualityHourlyDto`; `ReportDiskCache.Entry.pollenHours`.
- **Le parole non cambiano**, e dicevano già il giorno («Giornata dura per chi è allergico»).
- **La registrazione del README** aveva il polline della sola ora (25 set 2026, 12:00): le ho
  aggiunto le ore di quel giorno, dalla stessa API e per lo stesso punto. L'artemisia passa da
  2,5 granuli dell'ora (bassi) a 6,2 di media (moderati): l'immagine dei dettagli cambia, ed
  è rigenerata. `tools/record_readme_data.py` chiede l'aria come l'app.

### «Velato»: la misura

Dal 26 ott 2026 la regola 6 legge la luce che le nuvole tolgono, non la copertura: un cielo
tutto velato di nubi alte toglie il 30% della luce ed è «Quasi sereno». Sull'archivio di
ottobre con il satellite, 4 851 ore di giorno asciutte:

| Copertura ≥ 80% | Ore | Il satellite: sole / un po' di sole / niente sole | Indice di serenità mediano |
|---|---:|---:|---:|
| e la parola è «Sereno» o «Quasi sereno» | 373 (7,7%) | 73% / 23% / 4% | 0,85 |
| …in pianura padana | 85 (5,6%) | 80% / 16% / 4% | 0,87 |
| e la parola è «Nuvoloso» | 506 (10,4%) | 58% / 32% / 10% | 0,76 |
| e la parola è «Coperto» | 967 (19,9%) | 28% / 38% / 34% | 0,51 |
| Per confronto, copertura < 50% | 2 232 (46,0%) | 98% / 2% / 0% | 0,97 |

In 346 di quelle 373 ore lo strato alto fa il cielo. La parola ha ragione sulla luce (il sole
c'è tre volte su quattro, ed è la misura del 26 ott) e torto sul cielo: è coperto da un velo,
e la sua luce è l'85% di un cielo aperto, non il 97%. **La parola giusta è «Velato»**: uno
stato suo, copertura ≥ 80% con meno di metà della luce tolta, di giorno e di notte.

### «Velato»: perché non è fatto, e cosa serve

- **È un disegno prima che una regola.** La famiglia non ha un sole velato: `haze-day` e
  `haze-night` di Meteocons sono un sole e una luna sopra tre tratti di foschia a terra, e
  non sono fra le icone spedite. Serve sceglierne uno o comporlo, nelle quattro famiglie, e
  il committente lo sceglie guardando una tavola dei candidati.
- **È il cambiamento più visibile dei cinque**: parola e icona di un'ora di giorno su tredici,
  su Oggi, sulla settimana, sui widget, nelle notifiche e nelle immagini del README (la
  Milano registrata ha un giorno al 92% di nubi alte).
- **Quel che è pronto**: la regola (sopra), la misura, l'elenco dei punti da toccare
  (`WmoCode` con uno stato 1003, `ConditionWord`, le parole in due lingue, `ChiaroIcons`,
  l'aspetto del cielo in `OpenDaySentence`, il pareggio fra i cieli nell'etichetta del giorno,
  che oggi va al numero di codice più alto e deve andare al più pesante). Casella in «Aperto».

### Come è stato verificato

`WeatherReportMapperTest`: «the pollen's level is the day's mean, not the hour the page is
opened in» (la giornata di betulla di Milano; dodici ore fanno un giorno, undici no), le
classi per specie e la famiglia sulla media, il luogo senza pollini. `:core:data` verde (un
test del DataStore è caduto una volta per il rename su Windows, quello già noto, e non
c'entra). La suite intera e le immagini: sotto, «La chiusura».

## La review del motore: le quattro proposte rilette con un parere esterno, e la chiusura (committente, 17 nov 2026)

**Da dove nasce.** Sulle quattro proposte che la review aveva lasciato in «Aperto» il
committente ha portato un parere esterno, da verificare prima di applicarlo: «Dammi una tua
opinione onesta e verifica attentamente se quello che ti scrivo è corretto e lo condividi».
Qui le quattro risposte, dove sono d'accordo e dove no, con le verifiche.

### 1. Il bordo sud di ICON-D2 nella sonda: d'accordo sulla correzione, non sulla seconda città

- **La casella era sbagliata, e il parere ha ragione.** Diceva che la sonda non ha una città fra
  43,4° e 44° N: ha Firenze (43,7696°) e Ancona (43,6158°). Con il bordo nuovo (43,35° più il
  margine di 0,25° fa 43,6°) Ancona sta 0,016° dentro il confine, e la sonda la legge ogni mese:
  il confine interno era già sorvegliato. L'avevo scritta senza rileggere l'elenco delle città.
- **Livorno, sì.** Mancava una città appena fuori, che dica se i dati di ICON-D2 arrivano più a
  sud: Livorno (43,5485° N, 10,3106° E) è fra le città della sonda, che ora sono 46. Lanciata la
  sonda sulle due: Ancona «icon_d2, calcolati», Livorno «interpolati», e `icon_eu` al secondo
  giorno.
- **La città sull'Appennino, no: la differenza che doveva sorvegliare non c'è.** L'avevo
  scritta io («a 44,0° N / 10,3° E `best_match` differisce da ICON-D2») come una domanda aperta.
  Rimisurata punto per punto: nei due giorni di calendario `best_match` non differisce da
  ICON-D2 in nessun quarto di pioggia, né nei tre punti della prima scansione né a Castelnuovo
  di Garfagnana (44,1097° N, 10,4117° E, 290 m), Barga, Massa, Viareggio, Forlì, Firenze, Ancona
  e Bologna. I quarti diversi della prima scansione stanno tutti nel terzo giorno, da 32 ore
  avanti alla fine della corsa di ICON-D2, per un decimo di millimetro: è dove `best_match`
  passa la mano, non un altro modello in quota. La temperatura invece differisce in 2–26
  quarti su 192 un po' ovunque (Firenze 26, Massa 22), ed è la correzione di quota che la sonda
  conosce dal 14.1. Corretta la sezione del punto 1 («Trovato, e poi spiegato»).
- **I workflow.** `Probe provider` parte solo a calendario (il 2 del mese) e a mano: modificare
  `tools/probe_provider.py` non lancia niente. La sentinella salta una città che la sonda
  precedente non aveva (`if old is None: continue`). Una città in più sono circa 16 chiamate al
  mese sul runner, non sull'app.
- **Con l'occasione**: la sonda manda `past_minutely_15` come l'app (punto 1) e conta nell'aria
  anche i sei pollini orari (punto 5): 8 variabili, 1 chiamata; stampa 5,2 + 1,6 + 1,0 = 7,8.
  CLAUDE.md dice 46 città.

### 2. Le parole dell'interruttore della pioggia: d'accordo, e fatto in questa PR

L'argomento del parere è giusto e l'avevo sbagliato io a rimandarlo: dopo il punto 3 la riga
diceva «Pioggia oltre il 70% di probabilità entro 6 ore» di un avviso che non suona più su
una probabilità sola, e «Pioggia» di una notifica che può dire «Neve in arrivo». Un
comportamento cambiato sotto le parole di prima è la schermata che non dice il vero.

| Dove | Prima | Ora |
|---|---|---|
| La riga di Avvisi, il titolo | Pioggia nelle prossime ore | Pioggia o neve nelle prossime ore |
| La riga di Avvisi, la descrizione | Pioggia oltre il 70% di probabilità entro 6 ore. | Un'ora di pioggia o di neve entro 6 ore, probabile almeno al 70%. |
| Il canale delle notifiche | Pioggia in arrivo | Pioggia o neve in arrivo |
| La guida, «Pronti all'uso» | pioggia oltre il 70% entro 6 ore | un'ora di pioggia o di neve probabile almeno al 70% entro 6 ore |
| Il primo avvio, le notifiche | la pioggia in arrivo | la pioggia o la neve in arrivo |

In inglese: «Rain or snow in the next hours», «An hour of rain or snow within 6 hours, at
least 70% likely.», «Incoming rain or snow», e le due frasi della guida e del primo avvio.
«Un'ora di pioggia» e non «pioggia probabile»: è l'ora che il motore chiama bagnata, quella che
la striscia disegna; un'ora «possibile» al 90% non suona.

- **Tutti i punti dove comparivano**: le cinque stringhe sopra, in due lingue, e due frasi di
  VISION (§5.4 e il primo avvio). Le anteprime dei widget non ne parlano (dicono «le prossime
  ore» della previsione). DESIGN cita «Pioggia nelle prossime ore» una volta, dentro la frase
  di una riga tolta il 28 ott: è storia, resta. Il titolo e il testo della notifica erano già
  per fase dal punto 3.
- **Il canale si rinomina senza ricrearlo.** `AlertNotifier.ensureChannel` chiama
  `createNotificationChannel` con lo stesso id a ogni notifica: per Android è un cambio di
  nome, e l'id, con quel che il lettore ha scelto sul canale (silenziato, suono), resta.
  Verificato nel codice e con un test (`AlertNotifierTest`, «a channel made under its old name
  takes the new one at the next alert»); **non su un telefono**. Un limite, detto: il nome
  cambia alla prossima notifica di quel canale, non all'aggiornamento dell'app; fino ad allora
  le impostazioni del telefono dicono ancora «Pioggia in arrivo». Riguarda solo chi ha il
  canale silenziato e apre Avvisi prima che arrivi la prossima pioggia: la scheda gli nomina il
  canale con le parole nuove.
- **Le immagini.** Rigenerate: cambia `alerts-yours.png` e non `alerts.png`, dove la riga della
  pioggia non c'è. Nell'immagine la riga sta sopra il taglio e se ne vede solo la coda («most
  twice a day.»), una riga più in basso perché il titolo va su due; il resto è identico. Per
  vedere le parole ho reso la pagina sul gruppo «Pronti all'uso», in italiano e in inglese, con
  due test provvisori poi tolti: la riga tiene. Due cose viste lì e non toccate: in inglese il
  titolo va a capo lasciando «hours» da solo sulla seconda riga (in italiano l'a capo è
  bilanciato); e l'icona della riga sono ancora due gocce.

### 3. Il temporale dedotto dalla CAPE: d'accordo sul no, per ora

Non ho argomenti misurati per farlo adesso, e ne ho uno in più contro. I tre motivi del parere
reggono: 0,1 chiamate a ogni fetch in tutto il mondo; un pericolo inventato, contro la regola
1; un concetto nuovo da progettare su ogni superficie. Il quarto è nei numeri del punto 4: **in
pianura padana la stessa regola fa peggio del codice** (138 ore dette, 30 giuste, 22%, e 69
entro un'ora, 50%, contro il 46% e il 74% del codice), quindi dove l'app si usa di più non
darebbe niente, e fuori dall'Europa direbbe un'ora giusta su dieci. Quel che resta vero, e sta
scritto in STATES §10: fuori dalla famiglia ICON l'app non dice mai «Temporale». Spostato da
«Aperto» a «Da decidere: se e quando».

### 4. «Velato»: d'accordo, come passo a sé

La casella resta in «Aperto» com'è, con la regola (copertura ≥ 80% e meno di metà della luce
tolta), la misura e l'elenco dei punti da toccare. Una precisazione sulla frase del parere,
«il sole c'è 3 volte su 4»: è il 73% su tutti gli aeroporti e l'80% in pianura padana, e il 23%
che resta è «un po' di sole», non un cielo chiuso (4%).

**Rimandato a una prossima sessione** (il committente, lo stesso giorno, a lavoro chiuso:
«Lasciamo il velato per una prossima sessione, cancella pure le tavole che rivediamo l'icona
più avanti»). Una prima tavola dei candidati era stata resa e non è tenuta: va rifatta allora.
Un'avvertenza per chi la rifà: `haze-day` e `haze-night` non sono fra le icone spedite, e rese
dai drawable del repository escono tagliate; vanno passate dall'importatore prima di
confrontarle con le altre.

### La chiusura: cosa è stato lanciato

Sull'ultimo stato del ramo, in locale: `./gradlew test :app:testDebugUnitTest :app:lintDebug`.

| | Test | Falliti |
|---|---:|---:|
| `:core:domain` | 487 | 0 |
| `:core:data` | 326 | 0 |
| `:core:sync` | 34 | 0 |
| `:app`, debug | 1 224 (16 saltati: le immagini del README, che girano a richiesta) | 0 |
| `:app`, release | 998 | 0 |

Lint: 0 errori, 3 736 avvisi, gli stessi di prima. Due cose che la suite ha trovato lungo la
strada, e che dicono che serve lanciarla: le due stringhe italiane della neve con «al %d%%»
(`ItalianArticleTest`, corrette in «probabilità %d%%»), e un test del DataStore caduto una
volta per il rename su Windows, quello già noto.

Le immagini del README rigenerate e guardate: `today-details.png` (il riquadro dei pollini,
da «Low» a «Moderate · Weeds») e `alerts-yours.png` (la riga della pioggia più alta di una
riga). Le altre quattordici sono uscite identiche al byte.

Non verificato qui, e sta nelle caselle di «Aperto»: niente è stato visto su un telefono. Il
canale rinominato, l'hero nelle ore da 0,1 mm fuori dal quarto calcolato e il temporale senza
la soglia aspettano il vivo.

## «Velato»: il passo, pronto da eseguire (committente, 17 nov 2026)

**Da dove nasce.** A lavoro chiuso il committente ha chiesto se le indicazioni per fare
«Velato» fossero complete, cioè se una sessione successiva avesse già tutto: «Naturalmente
bisognerà creare anche le nuove icone». Non lo erano: nel punto 5 c'erano la misura, la regola
e un elenco di una riga. Questa sezione è il passo intero, scritto dopo aver riletto il
codice: chi lo esegue parte da qui e dalla misura del punto 5, senza rifare l'inventario.

### Cosa è, in una riga

Uno stato del cielo in più, dell'app: **«Velato»**, quando il cielo è coperto (copertura
totale ≥ 80%) e le nuvole tolgono meno di metà della luce del sole (`CloudLayers.lightTaken`
< 50%), di giorno e di notte. Oggi quelle ore dicono «Quasi sereno» (di rado «Sereno») sopra
un cielo tutto velato di nubi alte: 373 ore di giorno su 4 851 nell'archivio di ottobre
(7,7%; 5,6% in pianura padana), in 346 delle quali è lo strato alto a fare il cielo.

### Le decisioni da prendere con il committente, prima di scrivere codice

1. **Il disegno**, guardando una tavola (sotto, «Le icone»). È la decisione che ha fermato il
   passo.
2. **La parola inglese.** In italiano è «Velato». In inglese «Hazy sun» non regge di notte;
   la mia proposta è «High cloud», vera di giorno e di notte; l'alternativa è «Veiled».
3. **Il posto fra i cieli** (`WmoCode.severity`, che ordina i pareggi dell'etichetta del
   giorno e «Cosa è cambiato»). Proposta: fra «Quasi sereno» e «Nuvoloso», perché al satellite
   il sole c'è nel 73% delle ore velate e nel 58% delle ore coperte che dicono «Nuvoloso». Le severità dei cieli
   sono 0, 1, 2, 3 e non c'è posto: si rinumerano (sereno 0, quasi sereno 1, velato 2,
   nuvoloso 3, coperto 4; la nebbia resta a 10). La severità non è scritta su disco: sul disco
   va l'`id` della parola.
4. **Nella frase del giorno aperto** (`OpenDaySentence`): un tratto velato sta con il sole
   (proposta: sì, è `Look.SUN` senza toccare niente, e il tratto prende la parola più
   frequente) o è un aspetto suo, con una sua parola nella frase.
5. **Le soglie**, 80% e 50%: sono i due gradini che Open-Meteo e la regola 6 usano già (il
   «coperto» della copertura, la fine delle parole di sole nella luce). Misurata anche la
   copertura ≥ 90%: 311 ore (6,4%), sole nel 70%. Da confermare o cambiare sulla tabella del
   punto 5.

### La regola, dove va

- `WeatherStateEngine.skyOf(cloudCoverPct, layers)`, la regola 6: prima dei gradini,
  copertura ≥ 80 e luce tolta < 50 danno lo stato nuovo. Due costanti con la loro ragione
  (`VEILED_COVER_PCT`, `VEILED_LIGHT_PCT`).
- Senza gli strati (`layers` nullo) la luce tolta è la copertura, e a 80% di copertura non è
  mai sotto 50: nessun «Velato» senza strati, e non serve un caso a parte.
- Vale per ogni chiamante di `skyOf`: le righe, gli slot dell'etichetta del giorno, il blocco
  `current`, il cielo di un'ora di pioggia che non ha meritato il giorno (`dailyCode`, passo
  3), `OpenDaySentence.skyOf`.
- **Non tocca** i numeri, le richieste (nessuna variabile), il cielo disegnato (legge già gli
  strati e disegna il velo), il verdetto del Cielo (legge la copertura), gli avvisi.

### I punti del codice, uno per uno

| Dove | Cosa |
|---|---|
| `core/domain/.../WmoCode.kt` | Una voce `VEILED(1003, ConditionWord.VEILED, Phase.NONE, …, null)`, fra gli stati dell'app sopra 1000 (1061, 1071, 1095); la severità secondo la decisione 3; il KDoc della tabella. |
| `ConditionWord` (stesso file) | `VEILED("veiled")` **in fondo**: gli `id` sono su disco nello storico, e l'ordine delle voci non si tocca. |
| `core/domain/.../WeatherStateEngine.kt` | La regola (sopra) e il KDoc della regola 6. |
| `core/data/.../mapper/WeatherReportMapper.kt`, `dailyCode` | Il pareggio fra i cieli del passo 3 oggi va al **numero di codice** più alto (`compareBy({ it.value }, { it.key })`): con 1003 «Velato» vincerebbe ogni pareggio, anche contro la nebbia. Va alla severità. Difetto latente da correggere comunque. |
| `app/.../ui/today/WeatherText.kt` | `ConditionWord.VEILED -> R.string.cond_veiled` (il `when` è esaustivo: non compila senza). |
| `app/src/main/res/values{,-it}/strings.xml` | `cond_veiled`: «Velato», e la parola inglese decisa. Accanto: `cond_clear` «Sereno», `cond_mostly_clear` «Quasi sereno», `cond_partly_cloudy` «Nuvoloso», `cond_overcast` «Coperto». |
| `app/.../ui/icons/ChiaroIcons.kt`, `conditionLineRes` | Il ramo `WmoCode.VEILED`, giorno e notte (il `when` è sulle voci della tabella: non compila senza). |
| `app/.../ui/today/OpenDaySentence.kt`, `look()` | Solo se la decisione 4 dice «un aspetto suo»: oggi tutto quel che non è nebbia, coperto o nuvoloso è `Look.SUN`. |
| `app/.../ui/today/WhatChanged.kt` | Niente codice: legge `ConditionWord.severity`. Cambia cosa conta come «migliorato» fra due giorni, secondo la decisione 3. |
| `core/data/.../local/WeatherSnapshots.kt` | Niente: la mappa è il vocabolario vecchio, congelato; lo stato nuovo scrive il suo `id`. |

Non leggono i cieli per nome, e non vanno toccati: `HeadlineEngine`, `AlertEngine`,
`NowcastEngine`, `RuleVariables`, i widget (prendono icona e parola da `ChiaroIcons` e
`WeatherText`), `TodayUiState` (`fogOf`, `thunderOf`, `fallOf`).

### Le icone

Servono **due disegni** (giorno e notte) in **quattro facce** ciascuno: linea e pieno, su fondo
chiaro e su fondo scuro (`lineDarkOf`, `flatOf`, `flatDarkOf`), più l'animazione se la faccia
ne ha una (`movingOf`). Tre strade, da mettere sulla tavola una accanto all'altra:

1. **Meteocons `haze-day` e `haze-night`**: il sole, o la luna, sopra tre tratti di foschia.
   Sono fra i 519 disegni del repository e **non** fra quelli spediti: si aggiungono a
   `tools/shipped_icons.py` e si rilancia `tools/import_meteocons_v3.py`, che li porta alla
   taglia e ai colori della famiglia. Il pacchetto non è su questa macchina: `npm pack
   @meteocons/svg@3.0.0-next.10`, poi `tar -xzf`, come dice l'intestazione dello strumento
   (`npm` c'è). Dicono «foschia a terra» più che «velo in cielo»: è il loro limite.
2. **Una composizione**, come il «quasi sereno»: `tools/compose_sun_cloud.py` ha già le
   ricette (`sun-one-cloud`, `sun-half-cloud`) e i pezzi restano identici alla lettera ai loro
   originali. Una ricetta nuova: il sole intatto di `clear-day` (la luna di `clear-night`) con
   i tratti di `haze`, che è già spedito, o con la sagoma larga e bassa di `cloudy`. Non serve
   il pacchetto npm. Il nome è del repository (in Meteocons non esiste), e va scritto fra
   quelli che lo strumento possiede in CLAUDE.md e in DESIGN §13.1.
3. **Qualche tratto disegnato**, nella linea della famiglia (4 unità) e nei colori letti da
   un disegno della stessa faccia, come fa `tools/compose_sky_marks.py` dal 9 nov 2026.

**La tavola**: i candidati accanto a `clear-day`, `sun-one-cloud`, `sun-half-cloud` e
`cloudy`, alle due taglie che contano (la striscia oraria, 34 dp, e l'hero), nelle quattro
facce, giorno e notte. Un'avvertenza: i disegni non spediti (`mc3_haze_day.xml` e simili)
resi così come sono escono **tagliati**, perché non sono passati dal riancoraggio; vanno
importati prima di confrontarli. Una prima tavola, fatta il 17 nov 2026 senza questo passo,
non è stata tenuta. Niente sotto `res/drawable/mc3*` e `ui/icons/*Sets.kt` si scrive a mano:
rilanciare lo strumento è il disegno, e dopo un'importazione si rilanciano i tre compositori.

### I test da aggiornare o aggiungere

- `WmoCodeTest`: `engineOnly` (68, 69, 1061, 1071, 1095) prende 1003; i gruppi di parole; «i
  word id sono stabili»; la severità dei cieli.
- `WeatherStateEngineTest`: copertura 95% di sole nubi alte è «Velato»; con uno strato basso
  no; senza strati no; a 79% di copertura no; di notte uguale; la nebbia e la pioggia vengono
  prima.
- `WeatherReportMapperTest`: un giorno le cui ore di luce sono velate ha l'etichetta
  «Velato»; il pareggio fra due cieli va al più pesante, non al numero più alto.
- `ConditionWordsTest` («ogni parola ha la sua stringa») e `ConditionIconsTest` («ogni
  disegno esiste nelle quattro facce», «la notte cambia il disegno solo dove il cielo
  cambia»).
- `OpenDaySentenceTest`, secondo la decisione 4.
- `MeteoconsSetsTest`, e `ComposedIconsTest` se il disegno è composto: falliscono da soli se
  un'icona nominata da uno schermo non è spedita o se una composizione si scosta dai pezzi.

### I documenti e le immagini

- `docs/STATES.md`: §2 (le due soglie), §3 regola 6, §8 (il disegno), la tabella in testa
  («The states the app adds»), §10 (la voce «A covered sky the word calls sunny» diventa
  storia), §9 se cambia il conto delle ore che il motore riscrive.
- `DESIGN.md` §13.1 (la famiglia delle icone) e CLAUDE.md (l'elenco di cosa compone ogni
  strumento), se il disegno è composto; CHANGELOG, «Added».
- **Le immagini del README** vanno rigenerate tutte e guardate: la settimana registrata di
  Milano ha un giorno al 92% di nubi alte, e non so dire prima quali cambiano. I widget e il
  riepilogo sono disegnati dalle stesse ore.
- La guida non elenca le parole del cielo: niente da cambiare lì (una sola stringa in tutto
  il file dice «Quasi sereno», ed è la parola stessa).

### La misura, da rendere ripetibile

I numeri del punto 5 vengono da uno script provvisorio che non è nel repository. Il metodo,
per rifarlo dentro `tools/analyze_against_metar.py` (la sezione «CIELO contro il satellite»
ha già tutto: `clearness`, `seen_sky`, `opacity`):

- le ore di giorno asciutte con il satellite, il sole sopra 15°, senza nebbia detta o vista:
  le stesse della regola 6;
- luce = `min(opacity(basse, medie, alte), copertura)`; la parola dai gradini 20, 50, 80;
- i candidati sono le ore con copertura ≥ 80% e luce < 50%: quante sono, e sotto di loro le
  tre classi del satellite (sole da 0,7, un po' di sole da 0,4, niente sole) e l'indice
  mediano; a parte la pianura padana;
- per confronto le ore con copertura < 50% (sole 98%, indice 0,97).

Va aggiunto come rapporto a sé (`--veiled`), e **rilanciato sull'inverno** con la casella
già aperta «Il cielo d'inverno contro il satellite» (febbraio 2027): la misura di ottobre non
dice cosa fa un velo con il sole basso di dicembre.

### Cosa conferma solo il vivo

Un giorno di cirri fitti: la pagina che dice «Velato» sotto un cielo lattiginoso con il sole
che fa ombra; la stessa parola di notte, con le stelle che si vedono appena; il disegno nella
striscia oraria accanto a «Quasi sereno» e a «Nuvoloso», che non si confonda con nessuno dei
due.
