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
- **Chiusa il 19 nov 2026**: **Fase 17**, il motore dopo la review esterna (aperta lo stesso giorno, sul ramo `fase-17`, un commit per step): 17.1 (due testi che si contraddicono), 17.4 (la CAPE nel report e il temporale possibile del giorno), 17.5 (il temporale possibile a schermo), 17.6 (i rovesci dalla CAPE e dalla forma) e 17.8 (la chiusura); 17.2, 17.3 e 17.7 sono usciti dalla fase. Nell'archivio 16 (`docs/planning/16-novembre-17-19.md`): «Fase 17 — Il motore dopo la review esterna» (la review, le misure, le regole comuni, gli step), poi «La Fase 17 riordinata» e «La Fase 17 accorciata», che valgono, l'ultima per prima, dove non concordano; la chiusura in «17.8». Restano due prove dal vivo d'estate, il temporale possibile e i rovesci.
- **Chiusa il 23 nov 2026**: **Fase 18**, più modelli nei numeri e nel cielo (dal 22 nov 2026, sul ramo `claude/chiaro-forecast-engine-review-xg6n57`, PR #189): 18.1 (la misura e la regola dei numeri), 18.3 (il cielo dei tre modelli, entrato), 18.4 (la registrazione del README) e 18.6 (la chiusura); 18.2 (la temperatura dal secondo giorno) non è entrato, 18.5 è caduto con lui. Nello stesso periodo, chiesti dal committente: la verifica di testo e grafica, la riga di oggi come resto della giornata, la tinta del cielo dalla luce, il pareggio al più leggero, «Cosa è cambiato» tolta. Tutto nell'archivio 17 (`docs/planning/17-novembre-19-23.md`): «Fase 18 — Più modelli nei numeri e nel cielo: i passi» e le sezioni che la seguono, fino a «18.6». Il radar ne è fuori, valutato in «Il radar: valutato, da decidere» e in «Da decidere: se e quando».
- **Nessuna fase attiva** dal 19 al 22 nov 2026, a Fase 17 chiusa, e prima dal 16 al 19 nov 2026: quel che resta in «Aperto» sono una prova sul telefono, una misura che aspetta l'inverno e, dal 17 nov 2026, la nebbia dei due modelli da vedere dal vivo dopo il 30 novembre. Dal 17 nov 2026 anche le caselle della review del motore: due prove dal vivo e «Velato», fatto lo stesso giorno (archivio 16, ««Velato»: fatto») con la sua prova dal vivo; il temporale dedotto fuori dalla famiglia ICON, che era fra le cose «Da decidere: se e quando», è stato fatto dalla Fase 17. Il registro dal 17 al 19 nov 2026 è in `docs/planning/16-novembre-17-19.md` (la sonda, «Velato», le stelle cadenti, la nebbia vista dal vivo, la Fase 17 intera), quello dal 16 al 17 nov 2026 in `docs/planning/15-novembre-16-17.md` (la nebbia dei due modelli, la review del motore, il passo di «Velato»), quello fino al 16 nov 2026 in `docs/planning/14-novembre-12-16.md`.
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
  Dal 16 nov 2026 (archivio 15, «Quel che cade di notte: una luce sua»): provati dal committente
  la pioggia di notte, il nastro dentro e fuori dalle ore bagnate, il temporale con la sua
  pioggia e «Dietro le nuvole» che la toglie (Buenos Aires). **Resta la neve**, che in una
  notte senza luna non si vedeva: da provare sul telefono con la luce nuova. Il valore
  della luce, 0,10, è confermato dal committente sulla tavola resa (stessa sezione, «La
  decisione»).
  Chiusa il 16 nov 2026: la neve di notte vista dal committente sul telefono con la luce
  nuova (archivio 15, «Quel che cade di notte: una luce sua», «Sul telefono»).
- [ ] **L'icona di «A colpo d'occhio» più dentro, sul telefono** (archivio 15, ««A colpo d'occhio»:
  l'icona cinque dp più dentro», 16 nov 2026): il 4×1 accanto a «In parole» e al widget di
  Passo, con il coperto e con il sole; il 2×1; la scheda delle ore.
- [ ] **La nebbia dei due modelli, dal vivo dopo il 30 novembre** (archivio 15, «La nebbia: due
  modelli», 17 nov 2026): la sonda di dicembre (`Probe provider`) che trova vuota la visibilità
  di ICON in Italia, e l'app che non cambia nulla quel giorno; un mattino di nebbia padana in cui
  la pagina dice «Nebbia» dove due modelli la vedono, e il riquadro della visibilità con la
  parola dove il numero manca o supera il chilometro.
- [ ] **L'hero nelle ore da 0,1 mm, dal vivo** (archivio 15, «La review del motore, punto 1», 17
  nov 2026): un luogo fuori dal quarto d'ora calcolato (Roma, Napoli, una città fuori
  dall'Europa) in un'ora di pioggia debolissima, con l'hero che dice «Pioviggine» come la cella
  «Ora».
- [x] **Il bordo sud di ICON-D2 nella sonda** (stessa sezione, «La misura»). La casella, com'era
  scritta, era sbagliata in due punti: diceva che la sonda non ha una città fra 43,4° e 44° N, e
  ha Firenze (43,77°) e Ancona (43,62°), che con il bordo nuovo (43,6°) sta proprio sul confine
  interno e lo sorveglia già; e chiedeva una città sull'Appennino per una differenza fra
  `best_match` e ICON-D2 che, rimisurata, non c'è nelle ore che l'app legge. Mancava una città
  appena fuori. Fatto il 17 nov 2026: **Livorno** (43,55°) fra le città della sonda, che ora
  sono 46 (archivio 15, «La review del motore: le quattro proposte rilette»).
- [x] **Le parole dell'interruttore della pioggia** (archivio 15, «La review del motore, punto 3»,
  «Trovato e non toccato»): la notifica ora dice anche «Neve in arrivo», la riga di Avvisi, il
  canale e la guida dicevano solo «pioggia», e la vecchia regola. Fatto il 17 nov 2026, nella
  stessa PR: «Pioggia o neve nelle prossime ore» (archivio 15, «La review del motore: le quattro
  proposte rilette»).
- [ ] **Il temporale senza la soglia, dal vivo** (archivio 15, «La review del motore, punto 4»): la
  prossima estate, un pomeriggio di temporali isolati in pianura con la probabilità sotto il
  20%, e la pagina che dice «Temporale».
- [x] **«Velato»: lo stato e il suo disegno** (archivio 15, «La review del motore, punto 5»).
  Fatto il 17 nov 2026 (archivio 16, ««Velato»: fatto»): il disegno scelto su una tavola, le altre
  quattro decisioni confermate, il passo dell'archivio 15 eseguito.
- [ ] **«Velato» dal vivo**: un giorno di cirri fitti, la pagina che dice «Velato» sotto un cielo
  lattiginoso con il sole che fa ombra; la stessa parola di notte, con le stelle che si vedono
  appena; il disegno nella striscia oraria accanto a «Quasi sereno» e a «Nuvoloso», che non si
  confonda con nessuno dei due, fermo e in movimento. E sull'inverno, con «Il cielo d'inverno
  contro il satellite»: `tools/analyze_against_metar.py --veiled` sull'archivio che avrà il
  satellite di dicembre (archivio 16, ««Velato»: fatto», «Cosa conferma solo il vivo»).
- [ ] **Le stelle cadenti, sul telefono** (archivio 16, «Le stelle cadenti si vedono», 17 nov 2026):
  una notte di picco (le Geminidi, 13–14 dicembre, o le Leonidi se si fa in tempo) con il
  «Cielo animato» acceso, l'hero del Cielo e quello di Oggi: la scia sotto la riga del luogo e
  sopra le parole, lunga abbastanza da seguirla con l'occhio, fluida; una ogni tre quarti di
  minuto circa. E se la frequenza va bene così.
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
- [x] **17.1** Due testi che si contraddicono: «gli ultimi dati sono di Aggiornato 2 ore fa», e «pioggia N%» sotto un temporale, un gelicidio o una neve nella notifica «Maltempo» (Fase 17). Fatto il 19 nov 2026 (archivio 16, «17.1»): la probabilità solo sotto la pioggia forte, la frase della stima in due frasi.
- [x] **17.2** «Possibile» anche dove il modello piove e l'ensemble no (Fase 17). **Non fatto**: fuori dall'Europa d'estate non tiene; tolto il 19 nov 2026, e non è fra le cose da decidere (archivio 16, «La Fase 17 accorciata» e «17.2, 17.3 e 17.7: tolti del tutto»).
- [x] **17.3** Il blocco `daily` ricavato dalle ore: sette variabili in meno, 0,7 chiamate a fetch (Fase 17). **Non fatto**: tolto il 19 nov 2026, e non è fra le cose da decidere (archivio 16, «La Fase 17 riordinata» e «17.2, 17.3 e 17.7: tolti del tutto»).
- [x] **17.4** La CAPE nel report e il temporale possibile del giorno: la variabile, il modello, il mapper, la misura nello strumento (Fase 17). Fatto il 19 nov 2026 (archivio 16, «17.4»): `cape` nell'orario (fetch 7,9), `DailyForecast.possibleStorm` con le parti del giorno, la tabella del punto 3 ritrovata esatta in `--hazards`; nessuna schermata la legge ancora.
- [x] **17.5** Il temporale possibile del giorno a schermo: il giorno aperto, la frase, le due sintesi; mai un'icona d'ora, mai una notifica (Fase 17). Fatto il 19 nov 2026 (archivio 16, «17.5»): «Possibile qualche temporale nel pomeriggio» nei fatti e nella frase del giorno aperto, nella frase di Oggi, nelle due sintesi; oggi solo le parti ancora davanti.
- [ ] **Il temporale possibile, dal vivo** (archivio 16, «17.5», «Cosa conferma solo il vivo»): un giorno d'estate in cui la riga c'è, sul telefono, la frase, i fatti, le due sintesi; e quanto spesso la dice a Singapore e nei tropici, dove la CAPE è alta quasi ogni giorno.
- [x] **17.6** «Rovesci» dalla CAPE e dalla forma, non da `showers`: prima la misura d'inverno, poi la regola (Fase 17). Fatto il 19 nov 2026 (archivio 16, «17.6»): la regola com'era scritta non passava l'inverno per un soffio; il committente ha scelto la variante con l'episodio solo in aria con CAPE ≥ 10.
- [x] **17.7** La nebbia dal quarto giorno: la misura all'anticipo vero (Fase 17). **Non fatto**: tolto dalla fase il 19 nov 2026, una misura che non migliorava nulla; il limite va in STATES §11 con il 17.8 (archivio 16, «La Fase 17 accorciata»).
- [x] **17.8** La chiusura: la fase riletta, STATES riletto intero, la guida, il CHANGELOG (Fase 17). Fatto il 19 nov 2026 (archivio 16, «17.8»): la Fase 17 è chiusa.
- [ ] **I rovesci, dal vivo** (archivio 16, «17.6» e «17.8»): d'estate in pianura padana, un pomeriggio convettivo che la striscia chiama «Rovesci» il primo giorno e il terzo allo stesso modo (prima era «Pioggia» nei giorni di ICON-D2 e «Rovesci» da quelli di ICON-EU), il presente che dice quel che dice la sua cella; d'inverno una pioggia di fronte che resta «Pioggia», e nessun «Rovesci» su una pioviggine breve.
- [ ] **I rovesci in pianura padana, ricontati con l'HSS** (archivio 17, «La review della Fase 17: l'HSS e la serie contata una volta», 19 nov 2026): dopo l'estate 2027, `--showers` sui suoi archivi. Con l'HSS l'estate 2026 in pianura padana peggiora (0,28 → 0,22, «Rovesci» detto 74 volte per 45 visti, su 131 ore): se l'estate dopo dice lo stesso, si decide lì una soglia diversa per la pianura (la forma sola faceva 0,33), non prima.
- [x] **Il grafico di «Maltempo» quando il pericolo non è la pioggia** (archivio 17, «La review
  finale della PR #185», 20 nov 2026): per temporale, gelicidio e neve il testo non dice più la
  probabilità di pioggia (17.1), il grafico la disegna ancora con il suo picco, e TalkBack lo
  legge. Fatto il 20 nov 2026 nella stessa PR (archivio 17, «Il grafico di «Maltempo»:
  quello che cade»): millimetri all'ora per temporale e gelicidio, centimetri per la neve.
- [ ] **Il temporale possibile accanto a un'ora vuota** (archivio 17, «La review finale della
  PR #185», 20 nov 2026): `PossibleStorm.ahead` legge le righe del report, il mapper la risposta;
  dove il provider lascia un'ora senza temperatura, stato o copertura, la riga di oggi può
  nominare meno parti del campo del giorno. Raro: la cura è portare la CAPE dei due capi
  dell'ora nella riga.
- [x] **18.1** La misura nel repo, l'estate scaricata, le due righe di `CLAUDE.md`: le API di misura nel piano gratuito, la regola dei numeri riscritta; `--blend` e `--sky-blend` che ritrovano i numeri della fase; il download dell'estate 2026 lanciato sul runner (Fase 18, archivio 17). Solo strumenti e documenti: l'APK è quello di prima. Fatto il 22 nov 2026 (archivio 17, «18.1»): i numeri tornano, salvo la massima e la minima con la media, che la sezione aveva misurato come media degli estremi dei modelli; l'estate lanciata sul runner, il completamento dopo.
- [x] **La massima e la minima della media, prima del verdetto di 18.2** (archivio 17, «18.1»): il criterio (c) legge gli estremi delle ore della serie con la media, la sezione aveva misurato la media degli estremi dei modelli; con la prima definizione l'inverno non passa (fuori dalla pianura padana, la massima dei giorni 4–7 1,53 → 1,61 °C). Il committente sceglie quale delle due è la massima dell'app prima che 18.2 legga l'estate; senza una scelta, vale il criterio com'è scritto. Chiusa il 22 nov 2026 (archivio 17, «18.2»): il committente ha scelto il criterio com'è scritto, e l'estate non passa con nessuna delle due.
- [x] **18.2** La temperatura dal secondo giorno: la media di `best_match`, IFS e UKMO con il peso che cresce dalle 24 alle 72 ore, se passa il suo criterio sull'estate (Fase 18). **Non entra**, 22 nov 2026 (archivio 17, «18.2»): l'errore orario dei giorni 5–7 scende dell'11%, ma la minima dei giorni 4–7 in pianura padana peggiora (1,48 → 1,68 °C), e la condizione (c) non passa. Chiuso senza codice.
- [x] **18.3** Il cielo dei tre modelli: la copertura e gli strati medi dalla prima ora, per lo stato, il disegno, il nastro, il tile e il verdetto del Cielo; gli strati dei testimoni nella richiesta (fetch 7,9 → 9,1), se passa il suo criterio sull'estate (Fase 18). **Entra**, 23 nov 2026 (archivio 17, «18.3»): sull'estate le ore giuste 76,5% → 83,6%, «niente sole» sotto il sole 3,3% → 0,6%.
- [x] **Il cielo dei tre modelli, dal vivo** (archivio 17, «18.3»): qualche giorno con l'APK del commit, la parola dell'hero contro il cielo fuori dalla finestra; il cielo disegnato, il nastro e il tile che dicono la stessa cosa; il piede di Oggi con «nuvole da più modelli»; una notte nel verdetto del Cielo; una mattina di nebbia che resta nebbia. Provato dal committente il 23 nov 2026: «Ho provato e tutto funziona» (archivio 17, «18.6»).
- [x] **18.4** La registrazione del README con i testimoni, e le immagini rigenerate e guardate, dopo 18.2 e 18.3 (Fase 18). Solo test, risorse e immagini: l'APK è quello di prima. Fatto il 23 nov 2026 (archivio 17, «18.4»): Milano registrata di nuovo con i testimoni, quattordici immagini rigenerate e guardate, le didascalie riscritte.
- [x] **L'icona della riga di oggi: la giornata o il resto della giornata?** (archivio 17, «La verifica: testo e grafica nell'hero e nelle fasce del giorno», 23 nov 2026): a Parigi, alle 9:30, la riga di oggi dice «Pioggia» per la pioggia della notte, mentre il nastro e la frase guardano le ore che restano e dicono sole. Il committente decide se l'etichetta di oggi legga solo le ore che restano. Deciso il 23 nov 2026: il resto della giornata (archivio 17, «Le tre cose del 23 nov 2026»).
- [x] **Le tre cose del 23 nov 2026, dal vivo** (archivio 17, stessa sezione): la riga di oggi dopo una notte di pioggia, un giorno di velo alto sull'hero e sul nastro, una sera di pioggia o di neve che si vede come prima, una frase del giorno aperto su un giorno metà sole e metà grigio. Provato dal committente il 23 nov 2026: «Ho provato e tutto funziona» (archivio 17, «18.6»).
- [x] **Oggi senza «Cosa è cambiato», dal vivo** (archivio 17, «Cosa è cambiato» esce dall'app», 23 nov 2026): Oggi scorso fino in fondo, la settimana e poi i dettagli, nessuna sezione fra le due; la frase della giornata tranquilla che dice ancora la massima rispetto a ieri, dal secondo giorno; le regole che dicono ancora quando sono scattate l'ultima volta. Provato dal committente il 23 nov 2026: «Ho provato e tutto funziona» (archivio 17, «18.6»).
- [x] **18.5** (facoltativo) La media dell'ensemble ECMWF come quarto modello: prima il cancello (un archivio per giorno di anticipo, il peso), poi il criterio; senza archivio si chiude senza codice (Fase 18). **Non fatto**: lo step era «solo se 18.2 è entrato», e 18.2 non è entrato (archivio 17, «18.2», 22 nov 2026).
- [x] **18.6** La chiusura: la fase riletta contro i suoi criteri, la PR #189 rivista per intero, l'APK dell'ultimo commit provato dal committente, l'unione a `main` (Fase 18). Fatto il 23 nov 2026 (archivio 17, «18.6»): la Fase 18 è chiusa.
- [ ] **Il cielo dei tre modelli d'estate e nella nebbia, dal vivo** (archivio 17, «18.3» e «18.6»): un giorno d'estate di cumuli e di sole, la parola dell'hero contro il cielo fuori; una mattina di nebbia che resta nebbia.
- [ ] **Il nastro trascinato indietro, dal vivo** («Il nastro di Oggi si trascina anche indietro», 23 nov 2026): la sera, il dito dal pallino verso sinistra fino a mezzanotte; la cifra, la parola e il cielo di un'ora passata, con «Stima delle …» sotto; la pioggia o la neve di una mattina bagnata che cadono sul canvas; il tratto passato del nastro con il suo cielo, più tenue; le due azioni di TalkBack che passano da adesso alle ore indietro e tornano.
- [ ] **Il sole e la luna dell'hero, sul telefono** («Il sole e la luna dell'hero: la review e tre correzioni» e «La review di controllo del sole e della luna», 24 nov 2026): Huaraz verso mezzogiorno sul tablet, il sole che passa dal centro del canvas e non salta da un bordo all'altro (fino a fine febbraio guarda a sud), e nell'hero e nella finestra dalla stessa parte; a Huaraz una notte di luna quasi piena, la luna che verso il suo punto più alto riattraversa il canvas in un'ora o due e non salta; una falce sottile al crepuscolo con la luce cinerea, e una mezza luna senza la metà grigia; la luna di giorno senza alone e senza disco chiaro, bianca anche bassa; la luna appena sorta al buio, ambra, su uno schermo vero (l'OLED satura più del render): se è troppo, è `SkyPalette.MoonLow`. Il committente ha provato l'apk il 24 nov 2026 («mi sembra ok»); restano da vedere quando capitano la luna di Huaraz, la falce e la luna bassa.
- [ ] **Il piede di Oggi, sul telefono** («La review finale delle correzioni, e il piede di Oggi», 24 nov 2026): in fondo alla pagina, sotto «Aggiornato alle…», il margine della pagina e non più la fascia vuota; lo stesso margine che c'è in fondo a Cielo e ad Avvisi.
- [ ] **Il piede delle tre pagine sul tablet** («Lo snackbar di Oggi e il piede di Cielo: una regola per i tre tab», 24 nov 2026): con il rail, in fondo a Oggi, Cielo e Avvisi l'ultima riga sopra la barra di sistema e non sotto, con i gesti e con i tre tasti; e uno snackbar (annullare un promemoria in Cielo) che sul telefono sta appena sopra la barra dei tab.
- [ ] **Il nastro di Cielo trascinato indietro, dal vivo** («Il nastro di Cielo indietro: le prime ore della notte erano sereno», 24 nov 2026): la sera, a notte cominciata, il dito sul nastro dell'hero verso il crepuscolo; sotto un cielo coperto il canvas resta coperto, e la riga sotto il verdetto dice «Stima delle …»; dopo mezzanotte, il dito sulle ore della sera prima dice «disegnato senza nuvole»; in Esplora la stessa ora dice «Stima: nuvole …».

### Da decidere: se e quando

Fuori dalle fasi attive dal 16 nov 2026: il committente non ha ancora deciso se farle, tutte o
alcune, né quando. Niente qui si comincia senza il suo via.

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
- **Il radar della Protezione Civile: «piove adesso» e le prossime due ore, in Italia** (VISION
  §12.5, aperta dal 9 set 2026). Valutato il 22 nov 2026 con la Fase 18 e lasciato fuori dalla
  fase per scelta del committente, perché si decida a parte sul costo in codice e in risorse:
  il valore, quel che c'è, quel che costa in due pezzi separabili, i passi R1–R3 e
  l'indipendenza dal resto sono nell'archivio 17, «Fase 18 — Più modelli nei numeri e nel
  cielo: i passi», «Il radar: valutato, da decidere». Il primo passo, se si fa, è una misura: con
  LibreWXR (segnalato dal committente lo stesso giorno, valutato nella stessa sezione) si fa da
  qualunque macchina; con l'API della Protezione Civile solo dall'Italia, perché da un container
  cloud risponde «Access Denied».

Uscito da questa lista il 19 nov 2026, perché fatto: **il temporale dedotto dalla CAPE** (archivio
15, «La review del motore, punto 4»), partito lo stesso giorno come step 17.4 e 17.5 della Fase
17, una possibilità del giorno e non un pericolo dell'ora (archivio 16, «17.4», «17.5» e «17.8»).

Riletto il 23 nov 2026, all'archivio 17, a Fase 18 chiusa (il registro aveva passato le 1 000 righe): le caselle nate dal 19 nov 2026 rimandano all'archivio 17 (`docs/planning/17-novembre-19-23.md`); nessuna fase è attiva; restano aperte due prove sul telefono (l'icona di «A colpo d'occhio» più dentro, le stelle cadenti), le prove dal vivo delle fasi passate (la nebbia dei due modelli dopo il 30 novembre, l'hero nelle ore da 0,1 mm, «Velato», e la prossima estate il temporale senza la soglia, il temporale possibile e i rovesci) e una della Fase 18 (il cielo dei tre modelli d'estate e nella nebbia), i rovesci in pianura padana da ricontare dopo l'estate 2027, il temporale possibile accanto a un'ora vuota, e il cielo d'inverno contro il satellite, che aspetta febbraio 2027; nessuna proposta non decisa; «Da decidere: se e quando» tiene la trasmissione di Gadgetbridge, gli avvisi ufficiali fuori dall'Italia e il radar. Riletto il 19 nov 2026, all'archivio 16, a Fase 17 chiusa (il registro aveva passato le 1 000 righe): le caselle nate dal 17 nov 2026 rimandano all'archivio 16 (`docs/planning/16-novembre-17-19.md`); nessuna fase è attiva; restano aperte due prove sul telefono (l'icona di «A colpo d'occhio» più dentro, le stelle cadenti), sei prove dal vivo (la nebbia dei due modelli dopo il 30 novembre, l'hero nelle ore da 0,1 mm, «Velato», e la prossima estate il temporale senza la soglia, il temporale possibile e i rovesci) e il cielo d'inverno contro il satellite, che aspetta febbraio 2027; nessuna proposta non decisa; «Da decidere: se e quando» tiene la trasmissione di Gadgetbridge e gli avvisi ufficiali fuori dall'Italia, e il temporale dedotto dalla CAPE ne è uscito, fatto. Riletto il 17 nov 2026, all'archivio 15, dopo il merge della review del motore (PR #181): le caselle nate dal 16 nov 2026 rimandano all'archivio 15 (`docs/planning/15-novembre-16-17.md`); nessuna fase è attiva; restano aperte una prova sul telefono (l'icona di «A colpo d'occhio» più dentro), tre prove dal vivo (la nebbia dei due modelli dopo il 30 novembre, l'hero nelle ore da 0,1 mm, il temporale senza la soglia la prossima estate), il cielo d'inverno contro il satellite, che aspetta febbraio 2027, e «Velato», misurato e rimandato a una sessione sua, con il passo scritto nell'archivio 15; nessuna proposta non decisa; «Da decidere: se e quando» tiene la trasmissione di Gadgetbridge, gli avvisi ufficiali fuori dall'Italia e, dal 17 nov 2026, il temporale dedotto dalla CAPE. Riletto il 16 nov 2026, all'archivio 14, a registro chiuso: le caselle nate dal 12 nov 2026 rimandano all'archivio 14 (`docs/planning/14-novembre-12-16.md`); la Fase 16 e la Fase 14 sono chiuse e nessuna fase è attiva; restano aperte una prova sul telefono (quel che cade nell'hero del Cielo) e il cielo d'inverno contro il satellite, che aspetta febbraio 2027; nessuna proposta non decisa; «Da decidere: se e quando» tiene la trasmissione di Gadgetbridge e gli avvisi ufficiali fuori dall'Italia, che non sono lavoro finché il committente non lo dice. Riletto il 12 nov 2026, all'archivio 13, all'inizio del 16.11: le caselle nate dal 12 nov 2026 rimandano all'archivio 13 (`docs/planning/13-novembre-12.md`), la descrizione degli step della Fase 16 resta nell'archivio 11 (16.11 aperto: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.8, 16.9, e 16.10 che decide se «Punta il telefono» si tiene), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; due proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, e il fuso della macchina nelle immagini dei widget del README. Riletto il 12 nov 2026, all'archivio 12, all'inizio del 16.8: le caselle nate dal 10 nov 2026 rimandano all'archivio 12 (`docs/planning/12-novembre-10-11.md`), la descrizione degli step della Fase 16 resta nell'archivio 11 (16.8–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, le due caselle del 16.4, 16.5, 16.6, 16.7, la notte bianca), il peso della cifra e «Costellazioni» accesa da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; due proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, e il fuso della macchina nelle immagini dei widget del README. Riletto il 10 nov 2026, all'archivio 11, all'inizio del 16.3: le caselle nate dal 2 nov 2026 rimandano all'archivio 11 (`docs/planning/11-novembre-2-10.md`), dove sta anche la descrizione degli step della Fase 16 (16.3–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.1, 16.2), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; una proposta non decisa, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo. Riletto il 2 nov 2026, all'archivio 10, con la 2.0.0: le caselle chiuse dal 29 ott 2026 (15.13, 15.14) rimandano all'archivio 10 (`docs/planning/10-verso-2.0.0.md`); restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, dopo la rilettura della Fase 15: aperte 15.13 e 15.14, la coda; e come prima 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, all'archivio 09: le caselle della Fase 15 sono tutte chiuse (in `docs/planning/09-ottobre-28-29.md`), restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 28 ott 2026, all'archivio 08, lo stesso giorno dell'apertura della Fase 15: le caselle sopra sono della Fase 14 (14.12 in attesa) e della Fase 15 (in `docs/planning/08-ottobre-27-28.md`), nessuna proposta non decisa. Riletto il 27 ott 2026, all'archivio 07: le caselle della Fase 14, nessuna proposta non decisa. Fino al 26 ott 2026 nessuna casella aperta (riletto il 22 ott 2026, all'archivio 06):
le ultime chiuse quel giorno stanno in `docs/planning/06-ottobre-20-22.md`, «Cielo: la finestra
reale nella card Stanotte», «Il widget «Momenti del cielo» va via», «Le notifiche: ogni riga nella
forma che sta in una riga», «Il widget «Le prossime ore» va via».

---

## Registro dal 23 nov 2026, dopo l'archivio 17

## Il nastro di Oggi si trascina anche indietro (committente, 23 nov 2026)

Il committente ha chiesto perché il nastro dell'hero si trascina solo verso le ore che vengono e
non verso quelle già passate, se farlo è semplice e se aggiunge valore o solo rumore («non
sarebbe bello vedere la sera com'è andata la giornata?»). La risposta: nessun limite tecnico, una
scelta scritta in DESIGN §4 il 9 nov 2026 (archivio 11, «Oggi e il motore del cielo: la revisione
da app premium») sulla premessa che il report non tenesse nessuna ora prima di quella in corso.
La premessa era vera del report e non della risposta: Open-Meteo, senza `past_days`, risponde
dalla mezzanotte del luogo, il motore degli stati legge già tutta la serie, e il mapper scartava
le ore prima di quella in corso all'ultimo passo (ne teneva tre per «Nell'ultima ora»). La cache
su disco tiene la risposta grezza, quindi le ore ci sono anche offline. Il parere: valore, purché
la riga sotto la cifra non prometta «com'è andata», perché quelle ore sono la stima dei modelli
e non una misura, e la sera chi legge era lì.

Il committente ha deciso: farlo, con il meteo completo all'indietro (pioggia, neve e il resto),
«comunque una stima», sempre secondo le regole del motore dell'app, senza complessità eccessiva
nel codice; e ha chiesto se servisse una misura prima o se bastasse applicare le regole del
motore ai dati passati.

### Serve una misura? No, e perché

- **Nessuna regola nuova.** Le ore passate sono le stesse righe delle ore future, con gli stessi
  campi, lette dallo stesso `WeatherStateEngine` con le stesse soglie (STATES), la stessa nebbia
  a due voti, lo stesso cielo dei tre modelli (18.3). La regola di `CLAUDE.md` chiede una misura
  per ogni regola che deriva uno stato e per ogni combinazione che diventa un numero: qui non
  nasce né l'una né l'altra.
- **Il dato è il migliore che l'app ha.** Un'ora passata è servita dalla corsa più recente che
  l'ha coperta, poche ore prima al massimo: un anticipo più corto di qualunque ora futura che
  l'app mostra già senza un cancello di misura.
- **Quel che una misura direbbe non cambia il codice.** Contro i METAR direbbe quanto spesso la
  pioggia di un'ora passata c'era davvero, cioè quanto vale la parola «stima»; ma la parola resta
  «stima» comunque, e il committente vuole il meteo completo comunque. Il parere del 23 nov
  («farei quella misura prima di scegliere la parola») è superato dalla decisione: la parola è
  scelta, ed è onesta per costruzione.
- **Dove una misura servirebbe**: se l'app dicesse «è piovuto» invece di «stima», o se un'ora
  passata diventasse un numero altrove (un avviso, un totale del giorno). Nessuna delle due cose
  è fatta.

### Cosa è cambiato

- [x] **`WeatherReport.hoursSoFar`**: le ore di oggi già chiuse, dalla mezzanotte del luogo
  all'ora in corso esclusa, con lo stato del motore come ogni ora. Un campo a parte e non dentro
  `hourly`, perché `hourly[0]` è l'ora in corso per contratto e lo leggono i motori, gli avvisi,
  le regole e i widget: nessuno di loro cambia.
- [x] **Il mapper** le costruisce con lo stesso `mapHourly` delle ore future (un limite
  superiore in più), dal primo slot del giorno del luogo, controllato e non presunto.
- [x] **`WeatherRecency.trim`** sposta in `hoursSoFar` un'ora che finisce invece di buttarla, e
  toglie quelle che non sono di oggi: la lista non supera mai un giorno, e niente di nuovo va su
  disco. Un report fresco esce ancora identico, la stessa istanza.
- [x] **L'hero**: il nastro cammina le ore piene di oggi da entrambe le parti di adesso
  (`TodayStateBuilder.hoursBehind`, `scrubTarget`, `ribbonStep` per le due azioni di TalkBack,
  che passano dalle ore indietro ad adesso e alle ore avanti); l'ora in corso resta di adesso. Un'ora passata
  disegna il suo cielo con `skyOf`, che lo sapeva già fare (il sole e la luna dove stavano, la
  pioggia, la neve, la nebbia, il temporale dello stato), e sotto la cifra dice «Stima delle
  9:00» dove un'ora futura dice «Previsione delle 18:00».
- [x] **Il tratto passato del nastro ha il suo cielo**, ancora al 60%: il canvas riceve le ore
  di oggi intere. La riga di oggi nella settimana resta il resto della giornata (deciso il 23 nov
  2026, archivio 17, «Le tre cose del 23 nov 2026») e non cambia.
- [x] La guida (IT/EN), DESIGN §4 (due punti), il KDoc del nastro, il CHANGELOG (la voce
  «Drag the day» non ancora rilasciata, ampliata).

### Decisioni e deviazioni

- **Cambia** DESIGN §4 del 9 nov 2026 (archivio 11, «Oggi e il motore del cielo: la revisione da
  app premium», «Il nastro si trascina»: «cammina le ore intere di oggi ancora davanti»), per il
  motivo detto sopra: la premessa non era del dato ma del report.
- **Cambia** il punto «Nothing is invented» di DESIGN §4 (archivio 05, «Il nastro del cielo»,
  12 ott 2026) per il canvas: il tratto passato era luce sola perché l'app non aveva il cielo di
  quelle ore; ora lo ha, e la luce sola disegnava sereno una mattina coperta.
- **«Stima», non «com'è andata»**, né un'etichetta diversa per la pioggia: il committente vuole
  il meteo completo, e una parola sola per l'ora intera dice di chi è tutto quel che c'è sopra.
- **Un telefono offline dal giorno prima** cammina all'indietro le ore di stamattina del fetch
  di ieri: previsioni a più di un giorno, non stime a poche ore. Non c'è un ramo per loro: il
  chip dell'età sopra dice già quanto è vecchio il dato, e «stima» resta vera.
- **«Pioggia possibile» su un'ora passata** (lo stato `RAIN_LIKELY`: ≥ 60% senza un millimetro
  misurabile) resta com'è: è la parola del motore per quell'ora, e sotto «Stima delle …» si legge
  per quel che è.
- **Il costo**: zero chiamate in più (le ore erano già nella risposta, la somma resta 9,1 a
  fetch), nessuna migrazione, al più ventiquattro righe in memoria per luogo.


### Come è stato verificato

- `WeatherReportMapperTest` (+1): le ore di oggi prima di quella in corso sono a parte, dalle
  00:00 alle 13:00, con la pioggia della 13-14 letta come pioggia dal motore; a mezzanotte non ce
  n'è nessuna.
- `WeatherRecencyTest` (+2): un'ora che finisce entra nelle ore di oggi dopo quelle del mapper;
  un telefono offline dal giorno prima tiene le ore di oggi già passate e perde quelle di ieri.
  Un report fresco esce ancora identico (`WeatherNowTest`).
- `RibbonScrubTest` (+4, uno riscritto): le ore indietro sono quelle piene di oggi prima
  dell'ora in corso; il dito nel tratto già speso si ferma sull'ora sotto di sé, sull'etichetta
  dell'ora in corso su adesso; le due azioni vanno indietro, passano da adesso e proseguono, e si
  fermano agli estremi; un'ora passata disegna la sua nebbia, la sua pioggia con i suoi
  millimetri e la sua neve, con il sole dove stava; il nastro del canvas parte da mezzanotte con
  la pioggia del mattino, e la riga di oggi della settimana resta il temporale del pomeriggio.
- Tutti i test di unità (dominio 528, dati 327, sync 34, app 1 262) e `:app:lintDebug` verdi.
- Le immagini del README rigenerate e guardate: cambiano `today-now.png`, `today-night.png` e
  `today-foldable.png`, solo nel tratto passato del nastro, che a Milano è il mattino coperto e
  nebbioso della registrazione invece di un'alba limpida. `widgets.png` usciva diversa in 850
  pixel dell'icona del sole velato, identica a occhio, da un codice che questa modifica non tocca:
  rumore del disegno, lasciata com'era. Le didascalie restano vere.

## Il piede di Oggi va a capo sul punto (committente, 23 nov 2026)

Il committente ha chiesto di sistemare la riga in fondo a Oggi, «Aggiornato alle …», che va a
capo in modo indesiderato. La causa: da 18.3 (archivio 17, «18.3», 22-23 nov 2026) la riga dice
quasi sempre «Aggiornato alle 09:40 · dati Open-Meteo, nuvole da più modelli», che a 12sp misura
362 dp in Google Sans e 381 in Inter (in inglese 379 e 398), contro i 328-380 dp che un telefono
lascia dentro i margini; il paragrafo andava a capo dove capitava e lasciava «modelli» o «più
modelli» soli sotto. L'immagine `today-details.png` del README lo mostrava («models» da solo).

### Cosa è cambiato

- [x] **Una riga dove ci sta, altrimenti due spezzate sul punto**: `DataFooter` passa a
  `FittingText` (il componente del 15 ott 2026 che sceglie la prima variante che sta su una riga,
  misurata alla larghezza vera, nella lingua e nella dimensione del carattere del lettore) due
  varianti: la riga intera, e «Aggiornato alle 09:40» con sotto «Dati Open-Meteo, nuvole da più
  modelli». Alla dimensione di carattere più grande, dove nemmeno l'ora sta su una riga, la
  seconda va a capo fra le parole, come prima.
- [x] **Le stringhe in due pezzi**: `today_updated_at`, `today_data_source`,
  `today_data_source_clouds` (IT/EN) al posto di `today_updated_footer` e
  `today_updated_footer_clouds`; il « · » fra i due è del codice, come fra le cifre dell'hero.
- [x] DESIGN §8 («The page's last line»), l'immagine `today-details.png` rigenerata e guardata.

### Decisioni e deviazioni

- **Spezzata sul punto e non accorciata**: «nuvole da più modelli» dice da dove viene il cielo
  (18.3), e toglierlo sui telefoni stretti nasconderebbe la fonte proprio dove è più usata.
- **La seconda riga con la maiuscola**: è l'inizio di una riga, come la prima cifra del
  sottotitolo dell'hero.
- **Nessuna voce nel CHANGELOG**: la variante con le nuvole è di 18.3, non ancora rilasciata, e
  quella di prima stava su una riga (214-245 dp); il difetto non è mai uscito.


### Come è stato verificato

- `today-details.png` rigenerata e guardata: «Updated at 09:35» e sotto «Open-Meteo data, clouds
  from several models», dove prima «models» restava solo sulla seconda riga. Le altre immagini
  non cambiano (`widgets.png` compresa: la differenza di pochi pixel della sezione sopra era
  davvero rumore).
- I test dell'app (1 262) e `:app:lintDebug` verdi; le stringhe nuove sono in tutte e due le
  lingue.

## La review del nastro a due versi, e le correzioni (committente, 23 nov 2026)

Il committente ha chiesto una review di controllo delle due modifiche della giornata (il nastro
che si trascina indietro e il piede di Oggi), e poi di sistemare tutto sullo stesso branch. Sei
punti; il piede non ne aveva.

### Cosa è cambiato

- [x] **Il giorno del cambio d'ora** (il difetto vero). L'ora trascinata era tenuta per la sua
  etichetta: il 25 ottobre due ore si chiamano 02:00, e dalla prima il passo successivo trovava
  la seconda uguale e non si muoveva più, così con TalkBack le ore dalle 03:00 in poi non si
  raggiungevano andando avanti. Con le ore passate il caso durava tutto il giorno, non più solo
  la notte. Ora l'ora è tenuta per il suo istante (`HourlyForecast.at`), come fanno già
  `WeatherRecency` e la striscia; `ribbonStep` è generico. Con il dito le due 02:00 stanno nello
  stesso punto del nastro e si tocca la prima: un giorno di 25 ore su un nastro di 24 non ha
  posto per entrambe, e le azioni le raggiungono tutte e due.
- [x] **`TodayUiState.Content.nowAt`**, l'istante di adesso accanto all'ora del luogo: le ore
  davanti e dietro si leggono sugli istanti. Dietro è un'ora la cui FINE è passata per
  l'orologio del telefono, la regola di `WeatherRecency`: le ore del report sono tagliate all'ora
  del provider, che un telefono con l'orologio indietro non ha raggiunto, e l'ora in cui il
  telefono si trova restava fra le passate come «Stima». Davanti, la notte del cambio d'ora, la
  seconda 02:00 alle 02:30 della prima volta mancava: per l'etichetta non era «dopo».
- [x] **«Stima dalla previsione» sotto un'ora trascinata** (c'era già, per le ore davanti):
  la riga parla dei numeri di adesso, che con un'ora trascinata non sono sul canvas, e sotto
  «Stima delle 09:00» erano due stime di due ore. Si fa da parte tenendo la sua riga (alfa 0,
  niente semantica): nulla si muove quando il dito si posa (DESIGN §4, una riga per una riga), e
  lo screen reader non la legge.
- [x] **Le azioni di TalkBack sono solo quelle che muovono**: alle 23:30 non c'è «Ora
  successiva», a mezzanotte e mezza non c'è «Ora precedente» (prima c'era, e non faceva niente),
  e un'azione risponde se si è mossa.
- [x] **Quattro commenti** che dicevano ancora la regola di prima: sopra il nastro in
  `TodayHero`, `skyOf`, `hoursAhead`, `ribbonColorStops`. La nota «Offline» del `CLAUDE.md`
  resta vera (le ore passate escono da `hourly`) e non si tocca.

### Decisioni e deviazioni

- **Il cielo passato del nastro non porta la parola «stima»**, ed è una scelta: il nastro è
  «una raffigurazione e non una codifica» (DESIGN §4), e nemmeno il tratto futuro dice
  «previsione»; quel che è stima lo dice l'hero per ogni ora chiesta («Stima delle 9:00»), lo
  dice la guida, e il tratto passato resta al 60%. Una parola sul nastro, o nella sua descrizione
  vocale, direbbe una cosa sola su ventiquattro ore che il dito legge una per una.
- **Un test Compose e non solo funzioni pure** per le azioni e la riga della stima
  (`TodayRibbonTest`, sulla Milano registrata del README): quel che si verifica è quel che
  TalkBack trova sul nodo del nastro, e quel che resta scritto sotto la cifra.

### Come è stato verificato

- `RibbonScrubTest` (+2): le due 02:00 del 25 ottobre sono due passi; un'ora che il telefono non
  ha finito non è dietro, qualunque cosa dica l'ora del provider (13:00 contro le 12:30 del
  telefono), e la prossima è davanti; da adesso, senza ore davanti, il passo avanti resta fermo.
- `TodayRibbonTest` (nuovo, 3): due ore dopo il fetch «Stima dalla previsione» c'è; un passo
  indietro porta «Estimate for …» e la riga non si trova più, né scritta né letta; tornati ad
  adesso ricompare. Alle 09:40 si fanno nove passi indietro, fino a mezzanotte, e lì resta
  offerta solo «Ora successiva»; alle 23:30 solo «Ora precedente».

## Il sole e la luna dell'hero: la review e tre correzioni (committente, 24 nov 2026)

Il committente ha chiesto una review di come sono disegnati il sole e la luna nell'hero di Oggi
(posizione nelle ore della giornata, disegno, colori, realismo, texture), la spiegazione di come
funziona oggi, e proposte solo dove aggiungono valore e non rumore, «l'app a un livello premium
dal design pulito». Letti `SkyBodies.kt`, lo shader, `SkyColumn` e DESIGN §8.1c, la review è
stata fatta sulle immagini dello shader vero (un test usa-e-getta sul modello di
`SkyShaderPixelTest`, cancellato alla fine: il sole a otto altezze nei due abiti e sotto cinque
cieli, la luna a sette fasi di notte, di giorno, al crepuscolo, bassa, dietro velo e banco) e
sui numeri del motore (la traccia del sole ogni venti minuti a Milano d'inverno e d'estate, a
Huaraz, Singapore, Mumbai, Tromsø e Sydney). Tre proposte, e il committente le ha approvate
tutte e tre: «implementa 1, 2 e 3».

### Cosa ha trovato la review

- **Il disegno regge**: il sole a media altezza con il suo alone, dietro un cumulo, sotto il
  velo e sotto il coperto; la luna piena con i mari, la falce girata come la vede il luogo, la
  luna dietro le nuvole. Nessuna texture: tutto aritmetica, i mari sono nove gaussiane.
- **Ai tropici il sole saltava da un bordo all'altro a mezzogiorno** (il difetto). La regola
  metteva chi legge di fronte all'equatore; fra i tropici, nei mesi in cui il sole culmina dal
  lato del polo, il sole gli stava alle spalle. A Huaraz il 24 nov 2026, col sole a 79°: 0,87
  della larghezza tutta la mattina, 1,00 alle 11:40, 0,00 alle 12:00, 0,13 tutto il pomeriggio.
  A Huaraz è così da metà ottobre a fine febbraio; a Singapore da marzo a settembre.
- **La parte in ombra della luna si vedeva sempre**: al 14% del volto a ogni fase e a ogni
  ora. Al 3% di fase una palla grigia con un filo di luce, a metà una moneta a due toni, di
  giorno un disco chiaro sull'azzurro, e una luna nuova accanto al sole un fantasma che nel
  cielo non c'è. La pagina Cielo dice già quando la luce cinerea si vede: una falce fra l'1% e
  il 18%, al crepuscolo o prima dell'alba (archivio 03, la voce «Luce cinerea»).
- **La luna bassa non si scaldava**: il sole arrossisce negli ultimi 12°, la luna restava
  bianco-azzurra fino all'orizzonte. VISION chiama la luna piena che sorge «a huge orange disc».
- **Due cose minori, sistemate con la seconda**: la luna di giorno aveva un alone (0,55), e il
  suo disco passava da 0,95 a 0,55 nel minuto dell'alba, uno scatto che il canvas copriva con
  una planata.

### Cosa è cambiato

- [x] **Chi legge guarda il sole di mezzogiorno** (`facesNorth`, `SkyBodies.kt`): a nord se il
  luogo è a sud della latitudine su cui sta il sole (`AstronomyEngine.sunDeclination`, nuova,
  letta al mezzogiorno del giorno: un giorno, un verso). Fuori dai tropici è l'emisfero, tutti
  i giorni dell'anno, e nessuna immagine si muove. `SkySnapshot.southern` diventa
  `facingNorth`; le stelle, il lato del vento e la luce delle nuvole leggono lo stesso valore e
  girano con lui. Il passo più grande in venti minuti: Huaraz da 1,00 a 0,08, Singapore il 21
  giugno da 1,00 a 0,04, Mumbai il 10 giugno da 0,95 a 0,18 (un sole a 86°, la cui direzione
  gira davvero così in fretta).
- [x] **La parte in ombra della luna è il cielo** (`earthshineShare`): la luce cinerea tutta su
  una falce dell'8% o meno, niente dal 35%, e solo al buio, dal sole sull'orizzonte (niente) a
  −6° (tutta); a metà della forza di prima, 0,07 in luce lineare (`EarthshineLight`,
  `MoonMark.earthshine`, l'uniform `uEarthshine`). **La luna di giorno non ha alone**, e il suo
  disco impallidisce con la luce del giorno, da 0,95 alla fine del crepuscolo civile a 0,55 col
  sole a sei gradi (`moonVeil` legge il `day` della scena, non più un sì o no sull'orizzonte).
- [x] **La luna bassa si scalda** (`horizonWarmth`, una regola per i due corpi: niente dai 12°
  in su, tutto sull'orizzonte): il volto va da `MoonFace` a `SkyPalette.MoonLow`, `#F4C98A`, e
  l'alone con lui (`MoonMark.color`).
- [x] **L'esploratore disegna la stessa luna** (`SkyExploreScreen`): la luce cinerea con la
  stessa regola e il volto scaldato vicino all'orizzonte. La sua forza resta 0,14: quel disegno
  mescola colori e lo shader luce, e la stessa resa vuole due numeri.
- [x] **DESIGN** §8.1c (la regola nuova di «across», la parte in ombra, i quattro colori, e il
  paragrafo «The review of 24 nov 2026» con le misure, le prove scartate e il limite che
  resta), §3.9 (la camera delle stelle e il lato del vento, che seguono chi legge) e §8.8d
  (la luna dell'esploratore); **CHANGELOG** (due voci in «Fixed», una in «Added»).
- [x] **README**: tre immagini rigenerate e guardate una per una, tutte e tre nel disco della
  luna e basta. `today-night.png` (96 pixel, 7 livelli al più) e `widget-window.png` (164
  pixel, 8 livelli): la luna quasi piena del 23 novembre perde l'orlo in ombra. `sky-explore.png`
  (1 568 pixel, 13 livelli al più): la luna dell'esploratore sotto la nuvola non ha più sotto
  di sé il disco della luce cinerea, ed è appena meno chiara. `widgets.png` usciva diversa in
  due icone della striscia delle ore, che questo lavoro non tocca: rumore, come il 23 nov 2026,
  e non è stata toccata.

### Decisioni e deviazioni

- **Cambiano due scelte dell'archivio**: «il sole e la luna nel cielo», di traverso «per
  azimut visto guardando l'equatore» (archivio 04, `docs/planning/04-verso-1.1.0.md`, «La
  schermata Oggi, review grafica, blocchi 4–7 e il resto», 23 set 2026), e la parte in ombra
  «al 14% sopra il cielo» dell'esploratore (archivio 12, `docs/planning/12-novembre-10-11.md`,
  «16.6 — L'esploratore: la camera libera e il cielo a tutto schermo»), che riprendeva il 14%
  dell'hero. Il 14% a ogni fase era una semplificazione, non una misura; la regola nuova è
  quella dell'evento del Cielo.
- **Il verso di chi legge è dell'hero di Oggi soltanto**, in questo primo taglio: la finestra
  e la pagina Cielo guardano dove scelgono loro (la luna, il radiante) e per il resto tengono
  l'emisfero; `WindowSky.of` leggeva il valore dall'hero e ora lo legge dalla latitudine,
  com'era. La review di controllo ha portato il verso nuovo anche nella finestra di giorno
  (sezione qui sotto).
- **Il nome `southern` resta** nei parametri di `skyScene`, `bodyAcross` e `StarField`: è il
  quadro di chi sta a sud e guarda a nord, e lo passano anche la finestra e il Cielo con il loro
  emisfero. Rinominarlo erano 78 occorrenze in 22 file per una parola; il commento di `bodyAcross`
  dice che cosa passa Oggi.
- **Due limiti scritti**, e il primo era sbagliato: «la luna ai tropici può passare dal lato
  che chi legge non guarda e restare a un bordo, come prima» diceva questa riga, e al
  committente era stato detto «un caso raro». Non era come prima e non era raro: la review di
  controllo l'ha contato (sezione qui sotto) e corretto. Resta vero il secondo: nei due giorni
  l'anno in cui il sole passa allo zenit il quadro si gira, a mezzanotte.
- **Provato e scartato**, su un'immagine accanto a quella in uso: il disco del sole basso meno
  sovraesposto (da 3× a 1× o 0,6× sull'orizzonte: un disco giallo piatto, senza più la
  bruciatura che lo fa luce) e l'alone limitato da ogni bordo della fascia invece che dal più
  vicino (una cupola tagliata dritta). Il sole non è stato toccato.
- **Un limite che resta**: in cima alla fascia, un mezzogiorno d'estate, il sole ha poco più
  del suo disco, e il tetto di luminanza che si chiude al bordo porta l'orlo alto del disco da
  255 a 239. Lo cambia solo una fascia più alta, cioè l'altezza dell'hero: non proposto.
- **Nessuna richiesta nuova al provider**: il peso di un fetch resta 9,1 chiamate.

### Come è stato verificato

- `SolarEventsTest` (+1): la declinazione ai solstizi e all'equinozio, e a Huaraz il sole di
  mezzogiorno alto quanto la distanza dalla sua latitudine lascia.
- `SkyBodiesTest` (+5): la luna impallidisce senza scatti; la luce cinerea è di una falce
  sottile e della notte; il calore dell'orizzonte negli ultimi 12°; chi legge guarda il sole di
  mezzogiorno (Milano e Sydney tutto l'anno, Huaraz, Singapore e Mumbai secondo la stagione);
  ai tropici il sole attraversa il canvas una volta, minuto per minuto, mai più di un ventesimo
  della larghezza per minuto, al centro a mezzogiorno e mai su un bordo, e con la regola di
  prima saltava (il test lo misura, più di 0,8).
- `SkySceneTest` (+1) e `SkyTransitionTest`: la scena porta la luce cinerea e il colore, e fra
  due cieli si mescolano come il resto.
- `SkyShaderPixelTest` (+2), sui pixel dello shader: la parte in ombra di una mezza luna e di
  una luna di giorno è il cielo accanto (entro 0,015 di luminanza; era circa 0,12 sopra), quella di
  una falce del 5% no; il volto a 1° è caldo, a 6° a metà, a 20° il bianco di prima.
- Tutti i test (domain 529, data 327, sync 34, app 1 275) e `:app:lintDebug` verdi, in
  locale. Sul telefono resta la casella in «Aperto».

## La review di controllo del sole e della luna, e le correzioni (committente, 24 nov 2026)

Aperta la PR #191, il committente ha chiesto una review di controllo di quel che era stato
fatto. La review ha riletto il diff riga per riga (nessun errore di codice in shader,
transizione, esploratore e finestra) e ha rifatto due conti che la prima non aveva fatto: la
luna ai tropici minuto per minuto su un mese, e la luna bassa disegnata alla luce. Ne sono
usciti due difetti, il primo introdotto dal lavoro stesso e descritto male al committente («un
caso raro»). Il committente ha deciso: correggere tutti e due, correggere le cose minori (i
documenti, `sky-explore.png`), e sul simbolo della luna della pagina Cielo «decidi tu».

### Cosa ha trovato

- **Ai tropici ora saltava la luna** (la regressione). La luna piena sta di fronte al sole,
  quindi ha la declinazione opposta: nei mesi in cui il canvas si è girato verso il sole di
  mezzogiorno è la luna luminosa a passare alle spalle di chi legge. Contate, su trenta giorni,
  le notti in cui il disco salta da un bordo all'altro in un minuto:

  | Luogo, periodo | sole, prima | sole, dopo il primo taglio | luna di notte, prima | luna di notte, dopo |
  |---|---|---|---|---|
  | Huaraz, 10 nov – 9 dic 2026 | 30 su 30 | 0 | 0 su 27 | 11 su 27 |
  | Singapore, giugno 2026 | 30 su 30 | 0 | 0 su 28 | 14 su 28 |
  | Mumbai, giugno 2026 | 30 su 30 | 0 | 0 su 28 | 13 su 28 |
  | Milano, giugno e dicembre | 0 | 0 | 0 | 0 |

  A Huaraz le notti dal 18 al 28 novembre, dal 67% alla luna piena e ritorno; la notte del 24
  il disco sta su un bordo 188 minuti su 639. Nell'altra metà dell'anno il difetto della luna
  c'era già, per geometria e non contato (a Huaraz, a giugno, la luna piena culmina a sud, alle
  spalle di chi guarda a nord). E il sole di
  mezzanotte a Tromsø saltava ogni notte, con l'una e con l'altra regola: la prima review
  l'aveva visto nella traccia e non l'aveva detto.
- **La luna bassa ambra alla luce**: disegnata dallo shader col sole a +3° e a +0,5°, quasi
  spariva nel cielo arancio in cui sorge (bianca si vedeva); di giorno, col sole a 30° e la
  luna a 3°, era un disco grigio-beige sull'azzurro. Al buio e nel crepuscolo scuro era quella
  approvata.
- **`sky-explore.png`**: la parte illuminata della luna dell'esploratore, sotto la nuvola,
  era fino a 13 livelli meno chiara. Effetto non voluto: il disco della luce cinerea stava
  sotto tutto il disco, anche sotto la parte illuminata.
- **Mentre si correggeva, un terzo caso**: con le due regole a posto, un anno intero contato
  minuto per minuto mostrava ancora passi grandi quando un corpo passa quasi allo zenit, dove
  la direzione gira di mezzo giro in due minuti. La luna in 22–28 giorni l'anno ai tropici,
  fino a 0,74 della larghezza in un minuto (Mumbai, 12 giugno, a 89,9°); il sole nei suoi
  10–15 giorni di zenit, fino a 0,38. Al committente era stato detto «zero salti nel
  campione», vero sopra la soglia di 0,5 nei mesi contati e non nell'anno.

### Cosa è cambiato

- [x] **Alle spalle di chi legge un corpo riattraversa il canvas** (`bodyAcross`): nei 60° che
  la scala non tiene, 30° da una parte e dall'altra del punto alle spalle, va dal suo bordo al
  centro e all'altro bordo, dove prima stava fermo sul bordo e al punto alle spalle saltava.
  Fino a 150° da dove si guarda non cambia nulla.
- [x] **Allo zenit un corpo sta verso il centro** (`bodyAcross` con l'altezza): dai 75° in su
  la distanza dal centro si accorcia come la distanza dallo zenit, `cos(altezza)`, fino a
  niente sulla verticale. Sotto i 75° è il posto della direzione, identico. Ricontato l'anno
  2026 intero a Huaraz, Singapore e Mumbai: nessun minuto sopra un ventesimo della larghezza,
  il più grande 0,018 per la luna e 0,005 per il sole. A Milano non muove niente (nessun corpo
  arriva a 75°); a Roma la luna di 0,004 al più, a Palermo di 0,026, il sole in nessuna delle
  tre.
- [x] **Il calore della luna solo al buio** (`moonWarmth` = `horizonWarmth` per quel che resta
  della notte, `daylight`): tutto dalla fine del crepuscolo civile, niente col sole a sei
  gradi. Di giorno la luna è il bianco che era.
- [x] **La finestra di giorno guarda come l'hero** (`WindowScene.bandNorth`): di giorno il sole
  e la luna della finestra stanno nella stessa fascia dell'hero, e ai tropici le due avrebbero
  disegnato un sole solo da due parti, con le nuvole contro. Di notte la finestra e il Cielo
  guardano dove sceglie la loro camera e tengono l'emisfero, com'era. Non era stato chiesto: è
  una conseguenza del primo taglio trovata correggendo.
- [x] **La luna dell'esploratore**: la parte illuminata tiene sotto di sé lo strato che aveva
  tutto il disco, così sotto un velo è chiara com'era; in `sky-explore.png` la parte illuminata
  torna quella di `main`, e resta diverso solo l'orlo in ombra, come nelle altre due.
- [x] **I documenti**: DESIGN §8.1c (le tre regole di posizione con i conti, il calore al
  buio, la finestra di giorno) e §8.8d; CHANGELOG; la riga «come prima» e quella sulla finestra
  della sezione qui sopra, corrette dove stavano con il rimando a questa; la casella in
  «Aperto» con la luna di Huaraz e la finestra.

### Decisioni e deviazioni

- **Il simbolo della luna della pagina Cielo resta com'è** (`MoonBadge`, il disco intero al
  16% sotto la fase e la percentuale): è lo schema di una fase, non il cielo, e senza il disco
  intero una falce del 4% con «4%» sotto non dice di che cosa è il quattro per cento.
- **Lo zenit dai 75° e non dai 60°**, dove finisce la fascia: da 60° la regola spostava i corpi
  a Milano fino a 0,037 della larghezza nelle ore centrali d'estate, per un difetto che a
  Milano non c'è. Da 75° il Nord Italia è intatto e i tropici sono a posto lo stesso (da 60°:
  0,009 il passo più grande; da 75°: 0,018).
- **Un solo verso per il giorno, non uno di giorno e uno di notte.** Il verso migliore per la
  notte sarebbe l'opposto (la parte notturna dell'eclittica culmina dal lato opposto al sole),
  ma girare il quadro al crepuscolo specchierebbe la luna già sorta, il lato del vento e la luce
  dell'orizzonte in un colpo solo. Con le due regole di `bodyAcross` un corpo alle spalle
  riattraversa in un'ora o due, senza salti: basta.
- **Quel che resta**: nei due giorni l'anno in cui il sole passa allo zenit di un luogo il
  quadro si gira, a mezzanotte, e la luna e le stelle con lui.

### Come è stato verificato

- `SkyBodiesTest` (+3, e due riscritti): la piega alle spalle, continua su tutta la bussola nei
  due versi; il corpo allo zenit verso il centro e, sotto i 75°, identico al posto della
  direzione; il calore della luna al buio; un mese ai tropici minuto per minuto per sole e
  luna, i giorni del 2026 in cui passano più vicini allo zenit, il sole di mezzanotte e la luna
  che non tramonta a Tromsø: nessun minuto sopra un ventesimo della larghezza.
- `SkySceneTest`: di giorno la luna bassa è `MoonFace`, al tramonto a metà.
- Le misure della review (il mese ai tropici con le tre regole, l'anno intero prima e dopo lo
  zenit, la luna bassa alla luce nei due abiti) sono state fatte con test usa-e-getta sul
  motore e sullo shader veri, cancellati alla fine.
- Tutti i test (domain 529, data 327, sync 34, app 1 278) e `:app:lintDebug` verdi, in
  locale. Le immagini del README rigenerate: `sky-explore.png` ora diversa da quella di `main` solo nell'orlo in ombra della luna (120 pixel, 12 livelli al più; erano 1 568 e 13), `today-night.png` e `widget-window.png` identiche a quelle del primo commit, `widgets.png` questa volta identica a `main`.

## La review finale delle correzioni, e il piede di Oggi (committente, 24 nov 2026)

Provato l'apk della PR #191 («mi sembra ok»), il committente ha chiesto una review finale delle
correzioni e una verifica in più: in fondo alla pagina Oggi, sotto la riga «Aggiornato alle…»
(su due righe sul suo telefono), «sembra che c'è uno spazio di troppo, o una riga vuota».

### La review finale

- Riletto il secondo commit riga per riga (la piega alle spalle, lo zenit, il calore al buio,
  la finestra di giorno, la luna dell'esploratore): nessun errore. La CI della PR è verde.
- **Tre commenti dicevano ancora «guarda l'equatore»** dove ora non è più tutta la regola
  (`TodayUiState`, il radiante; `StarField.facing`; `WindowScene.facing`): corretti. Gli altri
  che lo dicono parlano del parametro, che resta l'emisfero per chi lo passa così.
- **Una frase della guida non era più vera ai tropici** (`guide_today_p1`, IT/EN): le stelle
  di Oggi erano «il cielo che hai davanti guardando a sud (a nord, sotto l'equatore)». A Huaraz,
  sotto l'equatore, da metà ottobre a fine febbraio il canvas guarda a sud. Ora: «guardando a
  sud (a nord, dove a mezzogiorno il sole sta a nord)». Era l'unico testo a schermo che dice da
  che parte guarda il cielo.

### Il piede di Oggi: lo spazio c'era

- **La causa**: la lista di Oggi teneva sotto l'ultima riga anche l'altezza della barra di
  navigazione del sistema (`contentPadding = WindowInsets.navigationBars`), scritta quando la
  pagina arrivava fino al piede della finestra. Da quando la shell lascia libera sotto le
  pagine l'altezza della sua barra, quella del sistema compresa (`MainScreens`, `barHeight`),
  quell'altezza era contata due volte: sul telefono, sotto l'ultima riga, i 16 dp di DESIGN
  §8 più una fascia vuota alta quanto la barra di sistema (48 dp con i tre tasti, meno con i
  gesti, secondo il telefono), cioè una riga e più. Cielo e Avvisi finiscono sui loro 16 dp.
- **Non si vedeva nelle immagini del README** perché lì la barra di sistema non ha altezza.
- [x] **Corretto**: la lista tiene l'altezza della barra di sistema solo accanto al rail di una
  finestra larga (§8.12c), dove la pagina arriva davvero al piede della finestra; sopra la
  barra della shell finisce sui suoi 16 dp.
- [x] **`TodayFootTest`** (nuovo, 3): dà un'altezza alla barra di sistema sulla Milano
  registrata e misura quel che resta sotto l'ultima riga. Prima della correzione, con 48 px di
  barra: 64 px invece di 16. Dopo: 16 sopra la barra della shell, 16 + 48 accanto al rail.
- **Non toccato**: lo snackbar di Oggi tiene lo stesso margine di sistema e sul telefono sta
  più in alto di quanto servirebbe; è un avviso che passa, non una riga della pagina.

### Come è stato verificato

- Tutti i test (domain 529, data 327, sync 34, app 1 281) e `:app:lintDebug` verdi, in
  locale: nella corsa intera due test di Avvisi sono caduti per il noto `AccessDeniedException`
  di DataStore su Windows (un file temporaneo che non si rinomina) e rilanciati da soli sono
  verdi. Nessuna immagine del README cambia: `guide.png` non mostra quella frase, e
  `widgets.png` è uscita di nuovo diversa in due icone della striscia delle ore, il rumore
  già visto il 23 nov e nella prima corsa di oggi, e non è stata toccata. Sul telefono: il piede
  di Oggi è da riguardare con l'apk della PR, nella sua casella in «Aperto».

## Lo snackbar di Oggi e il piede di Cielo: una regola per i tre tab (committente, 24 nov 2026)

Il committente: «se lo ritieni giusto correggi anche la snackbar di Oggi», e la verifica del
piede di Cielo «così chiudiamo il controllo» (Avvisi a vista gli sembra giusto).

- **Lo snackbar di Oggi: giusto correggerlo.** Stesso doppio conto della lista: teneva l'altezza
  della barra di sistema sopra la barra della shell, e sul telefono stava più in alto di
  quella misura rispetto agli snackbar di Cielo e Avvisi.
- **Il piede di Cielo sul telefono è giusto**: sotto l'ultima riga (l'ingresso alla guida) ci
  sono i 16 dp della pagina e gli 8 della riga, 24, e la pagina non legge la barra di sistema.
  Lo stesso per Avvisi, dal codice e dal test.
- **Trovato misurando: su una finestra larga è il contrario.** Accanto al rail la pagina arriva
  al piede della finestra, e Cielo e Avvisi non tenevano niente per la barra di sistema:
  l'ultima riga ci finiva sotto (48 dp con i tre tasti). Oggi lì era già giusto.
- [x] **Una regola sola, detta dalla shell** (`LocalPageFoot`, `pageFoot(rail)` in `NavRail.kt`,
  data da `MainScreens` ai tre tab): sotto l'ultima riga di una pagina e sotto il suo snackbar
  c'è l'altezza della barra di sistema accanto al rail, niente sopra la barra in basso. Oggi,
  Cielo e Avvisi la leggono; nessuno dei tre tab legge più da sé la barra di sistema.
- [x] **Test**: `TodayFootTest` (3), `SkyFootTest` (2) e `AlertsFootTest` (1) misurano quel che
  resta sotto l'ultima riga sopra la barra e accanto al rail. In quello di Oggi la barra di
  sistema riceve davvero un'altezza, e la pagina non la conta; in Cielo e Avvisi l'altezza
  data alla vista non arriva alla pagina del test (qualcosa la richiede di nuovo al sistema,
  che in Robolectric risponde zero), e il valore è dato come lo dà la shell.
- **Non verificabile qui**: quanto è alta la barra di sistema su un tablet vero, con i gesti e
  con i tre tasti. La regola usa l'altezza che il sistema dichiara, come fa il rail.
- DESIGN §8 («The page's last line»), CHANGELOG, la casella in «Aperto».
- Tutti i test (domain 529, data 327, sync 34, app 1 284) e `:app:lintDebug` verdi, in
  locale; nessuna immagine del README cambia.

## Il nastro di Cielo indietro: le prime ore della notte erano sereno (committente, 24 nov 2026)

Il committente: «Nell'hero della schermata Cielo ogni tanto quando trascino sulla fascia per
scorrere le ore, quando vado nelle ore indietro mi visualizza cielo sereno anche se le
previsioni sono diverse. Ad esempio come ora a Milano» (alle 19:20), «verifica attentamente e
correggi».

- **La causa**: le ore della notte (`Tonight.hours`, `SkyStateBuilder.nightHours`) erano lette
  solo da `report.hourly`, che comincia all'ora del fetch. Su una notte in corso le prime ore,
  dal crepuscolo all'ora del fetch, non c'erano, e un dito tornato lì non trovava un'ora:
  `betweenHours` dava null, il canvas leggeva `?: 0f`, copertura zero, un campo di stelle sotto
  un coperto. La riga sotto il verdetto non diceva niente delle nuvole (giusto, non le aveva),
  e il nastro stesso disegnava quel tratto senza grigio. Stasera a Milano la notte astronomica
  comincia prima delle 19 (il test lo verifica) e il report delle 19:10 comincia alle 19: il
  tratto dal crepuscolo alle 19 era sereno. «Ogni tanto» perché succede solo a notte in corso e solo nel tratto
  prima del fetch.
- **Il dato c'era**: dal 23 nov 2026 il report tiene le ore di oggi già chiuse
  (`WeatherReport.hoursSoFar`, «Il nastro di Oggi si trascina anche indietro»), che il nastro di
  Oggi percorre all'indietro come stima. Il Cielo non le leggeva.
- [x] **`nightHours` prende anche `hoursSoFar`**: le ore della notte già passate, un'ora per
  riga (dove le due liste si sovrapponessero vince quella avanti), ognuna segnata
  `NightHour.estimate`. Il canvas, il nastro e la lettura sotto il dito le hanno; l'hero, la
  notte di un'altra data, Esplora la stessa notte.
- [x] **Sono la stima, e lo dicono**, come su Oggi: la riga sotto il verdetto apre con «Stima
  delle 18:40» (la stringa di Oggi, `hero_estimate_at`); in Esplora la riga dell'ora dice
  «Stima: nuvole 95%» (`explore_estimate_cloud`) e la scheda di un oggetto la stessa frase
  al posto del verdetto.
- [x] **Non decidono niente**: il verdetto della notte e il tratto più sereno leggono ancora
  `hourly` soltanto (sono una previsione, come per una notte passata, che non ha verdetto); la
  nuvolosità media sotto una notte senza finestra (`meanCloud`) lascia fuori le ore stimate, e il
  widget della finestra ora legge la stessa funzione invece di una sua copia; in Esplora un'ora
  stimata non ha il verdetto dell'ora.
- **Il resto del buco, dopo mezzanotte**: `hoursSoFar` è di oggi (`WeatherRecency.trim` toglie
  quelle di ieri, e la risposta di Open-Meteo comincia alla mezzanotte del luogo), quindi alle
  01:00 le ore della sera prima non ci sono più dopo il primo fetch. Per quelle ore non c'è
  niente di onesto da disegnare: il canvas resta senza nuvole come prima, ma **la riga sotto il
  dito dice «disegnato senza nuvole»** (`sky_scrub_no_clouds`), la regola che Esplora ha già
  per un'ora che la previsione non raggiunge (DESIGN §8.8b, 16.7). Non tenere il dito fuori da
  quel tratto: il nastro resta dal crepuscolo all'alba sui due schermi, e il silenzio sul tratto
  era la bugia, non il tratto.
- **Non fatto, e perché**: chiedere a Open-Meteo `past_days=1` coprirebbe anche la sera prima
  dopo mezzanotte senza costo in chiamate (8 giorni restano sotto le due settimane), ma sposta
  l'inizio di ogni serie oraria e giornaliera della risposta, il mapper, i testimoni e la
  sonda: un cambiamento del livello dati per sei ore di nastro dopo mezzanotte, da decidere a
  parte se il committente lo vuole.
- **Test**: `TonightNightTest` (le ore della notte con quelle passate, la stima, la media senza
  di loro) e `SkyStateBuilderTest` (tre: la sera di Milano del 24 nov alle 19:20 con il
  crepuscolo coperto e stimato; il verdetto, il tratto più sereno e la media uguali con o senza
  le ore passate; dopo mezzanotte le ore della sera non inventate).
- La guida (IT/EN, Stanotte ed Esplora), DESIGN §8.8b, il CHANGELOG, la casella in «Aperto».
- Tutti i test (domain 529, data 327, sync 34, app 1 288) e `:app:lintDebug` verdi, nel
  container. Nessuna immagine del README cambia: sono disegnate la mattina, a notte non ancora
  cominciata, e la notte di Esplora è il suo mezzo, tutto ore avanti.

## La review di controllo del nastro di Cielo indietro (committente, 24 nov 2026)

Il committente: «fai una review di controllo dell'implementazione fatta in questa sezione».
Riletto il percorso intero (`nightHours`, il canvas, il nastro, la riga sotto il dito, Esplora
e la sua scheda, il widget della finestra, chi altro legge `Tonight.hours`) contro il report
come Cielo lo riceve: `ReportKeeper` lo dà **non tagliato** e ricostruisce la pagina a ogni
minuto. Due cose da correggere e un test che passava per il motivo sbagliato.

- [x] **«Stima» seguiva la provenienza, non l'orologio.** Era stima solo un'ora di
  `hoursSoFar`. Con un report che il telefono non ha potuto aggiornare (offline, un refresh
  fallito) le ore già passate stanno ancora in `hourly`: sotto il dito dicevano «19:30 · nuvole
  95%» senza «Stima», e in Esplora avevano il verdetto dell'ora, mentre Oggi, che taglia il
  report sull'orologio (`WeatherRecency.trim`), alla stessa ora dice «Stima delle 19:00».
  Corretto: `nightHours` riceve `now` e un'ora di `hourly` finita entro `now` è stima anche lei.
  La pagina si ricostruisce a ogni minuto (il tick del `ReportKeeper`), quindi la parola cambia
  entro il minuto in cui l'ora finisce. La media delle nuvole sotto una notte senza finestra
  lascia fuori anche quelle; il verdetto della notte resta com'era (legge `hourly`, e su un report
  oltre la soglia di vecchiaia si rifiuta già).
- [x] **La scheda di Esplora perdeva l'ora.** Su un'ora stimata la scheda di un oggetto metteva
  «Stima: nuvole 12%» al posto del verdetto, il cui chip diceva anche l'ora («Ottimo, nuvole 12%
  alle 21:35»); la scheda prende il posto del pannello con l'orario, e l'ora non era scritta da
  nessuna parte. Ora la scheda dice «Stima delle 21:35 · nuvole 12%», come la riga dell'hero.
- [x] **Il test della riga di Saturno in Esplora** (`SkyExploreScreenTest`, «a row takes the sky
  to the thing's hour…») si aspettava il verdetto alle 21:35 con la pagina alle 23:00: passava
  perché il report del test è di mezzogiorno e quell'ora stava in `hourly`. Nell'app, con un
  report fresco, le 21:35 sono già una stima, e prima di questa sezione erano sereno senza
  nuvole. Il test ora chiede la riga della stima con la sua ora, e che il verdetto non ci sia.
- **Verificato e giusto**: `hoursSoFar` e `hourly` sono disgiunti per costruzione (il mapper
  taglia all'indice dell'ora in corso) e la fusione li tiene in ordine comunque; un report di
  prima del 23 nov non ha `hoursSoFar` e la notte resta com'era; TalkBack legge la stessa riga
  («Stima delle …» compreso); il nastro grigia il tratto passato al 60% come il resto; la pioggia,
  la neve, la nebbia e il temporale di un'ora stimata cadono sul canvas come su Oggi; le immagini
  del README restano quelle (mattina, notte non cominciata).
- **Lasciato com'è**: «Stima delle 0:40» dopo mezzanotte è la stringa di Oggi, che ha la stessa
  forma per le sue ore dalla mezzanotte; se va cambiata, va cambiata per tutte e due.
- **Test**: `TonightNightTest` (le ore di un report non aggiornato stimate dall'orologio),
  `SkyStateBuilderTest` (nuovo: il report delle 19:10 letto alle 21:20, le 19 e le 20 stima, le
  21 no), `SkyExploreScreenTest` (la riga di Saturno). DESIGN §8.8b (una riga).
- Tutti i test (domain 529, data 327, sync 34, app 1 289) e `:app:lintDebug` verdi, nel
  container.

## «Stanotte» è stanotte: l'hero del Cielo resta sulla notte di fuori (committente, 27 nov 2026)

Il committente, venerdì 27 nov 2026 alle 20:20: l'hero del Cielo diceva «Stanotte, sabato 28
novembre». Era la notte dopo. A Milano, tre giorni dopo la luna piena, la finestra di buio
della sera (la notte astronomica meno la luna) andava dalle 18:29 alle 19:33, misurata con
`SkyScheduler.darkNight`; l'hero prendeva la notte da `SkyUpcoming`, che fa passare ogni momento
finito al successivo, e alle 20:20 la finestra era finita: data, cielo e verdetto erano quelli
del 28 (18:28–20:56), sotto la parola della notte di fuori. Succedeva ogni sera dopo la luna
piena, dal sorgere della luna a mezzanotte, e dopo mezzanotte fino all'alba.

- [x] **La notte dell'hero si sceglie sulla notte astronomica**, dal crepuscolo all'alba, non
  sulla sua finestra (`SkyStateBuilder.tonight`): quella di ieri finché la sua alba non è
  passata, quella di oggi altrimenti. Una notte senza crepuscolo (notte bianca, notte polare)
  non copre nulla, e resta la risposta di oggi con la sua ragione, come prima. Cambia il KDoc
  di `tonight` che chiedeva all'hero e alla riga della finestra di dire la stessa notte: la riga
  della finestra seguita continua a passare a domani (risponde «quando è la prossima»), l'hero
  dice «com'è stanotte». Il widget della finestra legge lo stesso `Tonight`.
- [x] **Una finestra finita in una notte in corso non ha verdetto** (`Tonight.windowPassed`):
  sarebbe un giudizio su ore passate, come per una notte finita. Al suo posto «Il buio è finito»
  (bianco, senza segno, come le notti senza finestra), sotto le nuvole medie delle ore che
  restano (`meanCloud`, che non legge le stime), poi «Buio dalle 18:29 alle 19:33» e «La luna è
  sorta alle 19:33», con la sua percentuale. Il nastro resta, e il dito ci scorre come prima. La barra
  dice la stessa parola, semplice; il widget della finestra «Buio finito · luna … · nuvole …» e non «Chiaro di luna tutta la notte» di una notte la cui prima ora era buia.
- **Lasciato com'è**: dove la luna sale e poi tramonta prima dell'alba resta un secondo tratto
  buio, più corto della finestra (che è il più lungo); la pagina non lo annuncia, come già non
  lo disegnava il nastro (`moonUpRanges` tiene la luna su dalla fine della finestra all'alba).
  La frase della luna è al passato e non dice «fino all'alba», così resta vera anche lì.
- **Il meteo delle ore passate sotto il dito** (domanda del committente lo stesso giorno):
  prima di mezzanotte c'è già, come stima, da «Il nastro di Cielo indietro» (24 nov 2026), e
  questa correzione lo rende visibile tutta la sera, perché la notte di fuori ora resta
  sull'hero. Dopo mezzanotte no: la risposta del provider comincia dalla mezzanotte del luogo
  (`hoursSoFar` è di oggi), e le ore della sera prima sono disegnate senza nuvole, e lo dicono
  (lasciato com'era: chiederle vuol dire `past_days=1` nella richiesta). **Deciso con il
  committente lo stesso giorno: resta così.** Dopo mezzanotte conta la notte che resta, che
  c'è tutta; le ore della sera sono passate e sarebbero una stima, e la pagina dice che non le
  ha invece di inventarle. Il costo (il mapper, i giorni, il quarto d'ora, i testimoni della
  nebbia, il peso misurato) non vale sei ore di stima. Si riapre solo se l'uso mostra che
  quelle ore servono, come lavoro a parte con la sua misura.
- Test: `SkyStateBuilderTest` (tre nuovi: Milano 27 nov 2026 alle 20:20, alle 18:50 e il
  mattino dopo, prima e dopo l'alba). La guida (IT/EN, «Stanotte»), DESIGN §8.8b, il CHANGELOG.
