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
- **Aperta il 22 nov 2026**: **Fase 18**, più modelli nei numeri e nel cielo: sei step, uno per sessione, tutti sul ramo `claude/chiaro-forecast-engine-review-xg6n57` con la PR #189 in bozza, che si unisce a `main` solo alla fine (18.1 la misura nel repo, l'estate scaricata e le due righe di `CLAUDE.md`; 18.2 la temperatura dal secondo giorno; 18.3 il cielo dei tre modelli; 18.4 la registrazione del README; 18.5, facoltativo, la media dell'ensemble ECMWF; 18.6 la chiusura e l'unione). Quel che ho trovato, le decisioni, i criteri scritti prima dei dati dell'estate, le regole comuni e ogni step sono in questo registro, «Fase 18 — Più modelli nei numeri e nel cielo: i passi». Il radar ne è fuori, valutato nella stessa sezione («Il radar: valutato, da decidere») e in «Da decidere: se e quando».
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
- [ ] **I rovesci in pianura padana, ricontati con l'HSS** (questo registro, «La review della Fase 17: l'HSS e la serie contata una volta», 19 nov 2026): dopo l'estate 2027, `--showers` sui suoi archivi. Con l'HSS l'estate 2026 in pianura padana peggiora (0,28 → 0,22, «Rovesci» detto 74 volte per 45 visti, su 131 ore): se l'estate dopo dice lo stesso, si decide lì una soglia diversa per la pianura (la forma sola faceva 0,33), non prima.
- [x] **Il grafico di «Maltempo» quando il pericolo non è la pioggia** (questo registro, «La review
  finale della PR #185», 20 nov 2026): per temporale, gelicidio e neve il testo non dice più la
  probabilità di pioggia (17.1), il grafico la disegna ancora con il suo picco, e TalkBack lo
  legge. Fatto il 20 nov 2026 nella stessa PR (questo registro, «Il grafico di «Maltempo»:
  quello che cade»): millimetri all'ora per temporale e gelicidio, centimetri per la neve.
- [ ] **Il temporale possibile accanto a un'ora vuota** (questo registro, «La review finale della
  PR #185», 20 nov 2026): `PossibleStorm.ahead` legge le righe del report, il mapper la risposta;
  dove il provider lascia un'ora senza temperatura, stato o copertura, la riga di oggi può
  nominare meno parti del campo del giorno. Raro: la cura è portare la CAPE dei due capi
  dell'ora nella riga.
- [x] **18.1** La misura nel repo, l'estate scaricata, le due righe di `CLAUDE.md`: le API di misura nel piano gratuito, la regola dei numeri riscritta; `--blend` e `--sky-blend` che ritrovano i numeri della fase; il download dell'estate 2026 lanciato sul runner (Fase 18, questo registro). Solo strumenti e documenti: l'APK è quello di prima. Fatto il 22 nov 2026 (questo registro, «18.1»): i numeri tornano, salvo la massima e la minima con la media, che la sezione aveva misurato come media degli estremi dei modelli; l'estate lanciata sul runner, il completamento dopo.
- [x] **La massima e la minima della media, prima del verdetto di 18.2** (questo registro, «18.1»): il criterio (c) legge gli estremi delle ore della serie con la media, la sezione aveva misurato la media degli estremi dei modelli; con la prima definizione l'inverno non passa (fuori dalla pianura padana, la massima dei giorni 4–7 1,53 → 1,61 °C). Il committente sceglie quale delle due è la massima dell'app prima che 18.2 legga l'estate; senza una scelta, vale il criterio com'è scritto. Chiusa il 22 nov 2026 (questo registro, «18.2»): il committente ha scelto il criterio com'è scritto, e l'estate non passa con nessuna delle due.
- [x] **18.2** La temperatura dal secondo giorno: la media di `best_match`, IFS e UKMO con il peso che cresce dalle 24 alle 72 ore, se passa il suo criterio sull'estate (Fase 18). **Non entra**, 22 nov 2026 (questo registro, «18.2»): l'errore orario dei giorni 5–7 scende dell'11%, ma la minima dei giorni 4–7 in pianura padana peggiora (1,48 → 1,68 °C), e la condizione (c) non passa. Chiuso senza codice.
- [ ] **18.3** Il cielo dei tre modelli: la copertura e gli strati medi dalla prima ora, per lo stato, il disegno, il nastro, il tile e il verdetto del Cielo; gli strati dei testimoni nella richiesta (fetch 7,9 → 9,1), se passa il suo criterio sull'estate (Fase 18).
- [ ] **18.4** La registrazione del README con i testimoni, e le immagini rigenerate e guardate, dopo 18.2 e 18.3 (Fase 18). Solo test, risorse e immagini: l'APK è quello di prima.
- [x] **18.5** (facoltativo) La media dell'ensemble ECMWF come quarto modello: prima il cancello (un archivio per giorno di anticipo, il peso), poi il criterio; senza archivio si chiude senza codice (Fase 18). **Non fatto**: lo step era «solo se 18.2 è entrato», e 18.2 non è entrato (questo registro, «18.2», 22 nov 2026).
- [ ] **18.6** La chiusura: la fase riletta contro i suoi criteri, la PR #189 rivista per intero, l'APK dell'ultimo commit provato dal committente, l'unione a `main` (Fase 18).

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
  l'indipendenza dal resto sono in questo registro, «Fase 18 — Più modelli nei numeri e nel
  cielo: i passi», «Il radar: valutato, da decidere». Il primo passo, se si fa, è una misura: con
  LibreWXR (segnalato dal committente lo stesso giorno, valutato nella stessa sezione) si fa da
  qualunque macchina; con l'API della Protezione Civile solo dall'Italia, perché da un container
  cloud risponde «Access Denied».

Uscito da questa lista il 19 nov 2026, perché fatto: **il temporale dedotto dalla CAPE** (archivio
15, «La review del motore, punto 4»), partito lo stesso giorno come step 17.4 e 17.5 della Fase
17, una possibilità del giorno e non un pericolo dell'ora (archivio 16, «17.4», «17.5» e «17.8»).

Riletto il 19 nov 2026, all'archivio 16, a Fase 17 chiusa (il registro aveva passato le 1 000 righe): le caselle nate dal 17 nov 2026 rimandano all'archivio 16 (`docs/planning/16-novembre-17-19.md`); nessuna fase è attiva; restano aperte due prove sul telefono (l'icona di «A colpo d'occhio» più dentro, le stelle cadenti), sei prove dal vivo (la nebbia dei due modelli dopo il 30 novembre, l'hero nelle ore da 0,1 mm, «Velato», e la prossima estate il temporale senza la soglia, il temporale possibile e i rovesci) e il cielo d'inverno contro il satellite, che aspetta febbraio 2027; nessuna proposta non decisa; «Da decidere: se e quando» tiene la trasmissione di Gadgetbridge e gli avvisi ufficiali fuori dall'Italia, e il temporale dedotto dalla CAPE ne è uscito, fatto. Riletto il 17 nov 2026, all'archivio 15, dopo il merge della review del motore (PR #181): le caselle nate dal 16 nov 2026 rimandano all'archivio 15 (`docs/planning/15-novembre-16-17.md`); nessuna fase è attiva; restano aperte una prova sul telefono (l'icona di «A colpo d'occhio» più dentro), tre prove dal vivo (la nebbia dei due modelli dopo il 30 novembre, l'hero nelle ore da 0,1 mm, il temporale senza la soglia la prossima estate), il cielo d'inverno contro il satellite, che aspetta febbraio 2027, e «Velato», misurato e rimandato a una sessione sua, con il passo scritto nell'archivio 15; nessuna proposta non decisa; «Da decidere: se e quando» tiene la trasmissione di Gadgetbridge, gli avvisi ufficiali fuori dall'Italia e, dal 17 nov 2026, il temporale dedotto dalla CAPE. Riletto il 16 nov 2026, all'archivio 14, a registro chiuso: le caselle nate dal 12 nov 2026 rimandano all'archivio 14 (`docs/planning/14-novembre-12-16.md`); la Fase 16 e la Fase 14 sono chiuse e nessuna fase è attiva; restano aperte una prova sul telefono (quel che cade nell'hero del Cielo) e il cielo d'inverno contro il satellite, che aspetta febbraio 2027; nessuna proposta non decisa; «Da decidere: se e quando» tiene la trasmissione di Gadgetbridge e gli avvisi ufficiali fuori dall'Italia, che non sono lavoro finché il committente non lo dice. Riletto il 12 nov 2026, all'archivio 13, all'inizio del 16.11: le caselle nate dal 12 nov 2026 rimandano all'archivio 13 (`docs/planning/13-novembre-12.md`), la descrizione degli step della Fase 16 resta nell'archivio 11 (16.11 aperto: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.8, 16.9, e 16.10 che decide se «Punta il telefono» si tiene), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; due proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, e il fuso della macchina nelle immagini dei widget del README. Riletto il 12 nov 2026, all'archivio 12, all'inizio del 16.8: le caselle nate dal 10 nov 2026 rimandano all'archivio 12 (`docs/planning/12-novembre-10-11.md`), la descrizione degli step della Fase 16 resta nell'archivio 11 (16.8–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, le due caselle del 16.4, 16.5, 16.6, 16.7, la notte bianca), il peso della cifra e «Costellazioni» accesa da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; due proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, e il fuso della macchina nelle immagini dei widget del README. Riletto il 10 nov 2026, all'archivio 11, all'inizio del 16.3: le caselle nate dal 2 nov 2026 rimandano all'archivio 11 (`docs/planning/11-novembre-2-10.md`), dove sta anche la descrizione degli step della Fase 16 (16.3–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.1, 16.2), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; una proposta non decisa, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo. Riletto il 2 nov 2026, all'archivio 10, con la 2.0.0: le caselle chiuse dal 29 ott 2026 (15.13, 15.14) rimandano all'archivio 10 (`docs/planning/10-verso-2.0.0.md`); restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, dopo la rilettura della Fase 15: aperte 15.13 e 15.14, la coda; e come prima 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, all'archivio 09: le caselle della Fase 15 sono tutte chiuse (in `docs/planning/09-ottobre-28-29.md`), restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 28 ott 2026, all'archivio 08, lo stesso giorno dell'apertura della Fase 15: le caselle sopra sono della Fase 14 (14.12 in attesa) e della Fase 15 (in `docs/planning/08-ottobre-27-28.md`), nessuna proposta non decisa. Riletto il 27 ott 2026, all'archivio 07: le caselle della Fase 14, nessuna proposta non decisa. Fino al 26 ott 2026 nessuna casella aperta (riletto il 22 ott 2026, all'archivio 06):
le ultime chiuse quel giorno stanno in `docs/planning/06-ottobre-20-22.md`, «Cielo: la finestra
reale nella card Stanotte», «Il widget «Momenti del cielo» va via», «Le notifiche: ogni riga nella
forma che sta in una riga», «Il widget «Le prossime ore» va via».

---

## Registro dal 19 nov 2026, dopo l'archivio 16

## La review della Fase 17: l'HSS e la serie contata una volta (19 nov 2026)

Chiesto dal committente, sulla PR della Fase 17 (#185), dopo una review esterna del motore com'è
in quella PR e in main. Tre segnalazioni; la terza (tre commenti che dicevano che la CAPE serve
solo al temporale del giorno) è un commit di soli commenti, «17.8: la CAPE nei commenti legge
anche i rovesci». Le altre due:

**La misura dei rovesci con l'HSS.** Le decisioni della 17.6 stavano sul CSI, che non conta i
«no» giusti: dove l'evento è frequente, dirlo spesso alza il CSI, e dirlo sempre ne fa la sua
frequenza. D'estate un terzo o metà delle ore di pioggia sono rovesci: «Rovesci» su ogni pioggia
farebbe 0,34 in Europa e 0,47 agli aeroporti di ICON-D2, più della regola scelta (0,44).
Ricontato con l'HSS (Heidke: 0 per «sempre» e per «mai»), sugli stessi archivi e con lo stesso
strumento:

- **la scelta della 17.6 regge**: in Europa la regola è la migliore delle tre colonne, o alla
  pari, in ogni stagione (estate 0,06 → 0,34, ottobre 0,02 → 0,18, inverno 0,15 → 0,17), e la
  soglia di 10 J/kg sull'episodio vale anche per l'HSS (l'inverno europeo 0,14 senza, 0,17 con);
- **quel che il CSI nascondeva**: la pianura padana d'estate peggiora, 0,28 → 0,22, con «Rovesci»
  detto 74 volte per 45 visti; fuori dall'Europa d'inverno 0,17 → 0,07. Nessuna soglia vince
  ovunque (in pianura la forma sola fa 0,33, agli aeroporti di ICON-D2 CAPE ≥ 50 o la forma
  0,34): con un'estate sola una regola per regione sarebbe tarata sul rumore. **La regola non
  cambia**; il limite è scritto in STATES §10 e c'è una casella in «Aperto» per l'estate 2027;
- **il temporale possibile del giorno regge anche meglio**: un giorno di temporale è raro (13%
  dei giorni), il CSI lì non è gonfiato, e l'HSS cresce in ogni fetta (0,35 → 0,50 sulle tre
  stagioni, sul giorno del luogo).

`tools/analyze_against_metar.py` stampa da oggi, in `--showers` e nel temporale possibile di
`--hazards`, l'HSS, il bias e il CSI di «sempre» accanto a ogni CSI (classe `Days`). STATES §11
ha le due tavole con l'HSS, §2 e §10 il limite; i KDoc di `SHOWER_CAPE_JKG` e di `PossibleStorm`
dicono l'HSS accanto al CSI.

**L'hero e la cella contavano la serie in due modi.** Il mapper passa al motore le ore che
esistono (`statesOf`: un'ora vuota, senza codice e senza nuvole, sparisce, e le sue vicine si
giudicano fra loro, scelta del 19 ott 2026); il presente ricontava la persistenza della nebbia e,
dalla 17.6, l'episodio della pioggia sulle righe **con** i buchi, dove un'ora vuota chiude la
corsa. Accanto a un'ora vuota l'hero poteva dire «Rovesci» sopra una cella «Pioggia», o
«Coperto» sotto una cella «Nebbia». Per la nebbia il difetto c'era dal 26 ott 2026; la 17.6 l'ha
ripetuto per i rovesci.

- **Prima proposta, rivista e cambiata prima di scriverla**: contare l'episodio della serie sulle
  righe con i buchi, come l'hero. Era corretta (le due cose avrebbero detto lo stesso), ma
  sceglieva la regola opposta a quella che la serie ha dal 19 ott 2026, cambiava la striscia
  invece dell'hero, lasciava la nebbia com'era e teneva lo stesso conto in due posti.
- **Fatto**: `WeatherReportMapper.seriesOf` conta una volta sola, sulla serie che il motore
  riceve, la persistenza della nebbia e l'episodio di ogni ora, e li scrive nell'ora
  (`Hour.fogPersists`, `Hour.episodeHours`); il motore li legge per le righe, il presente li legge
  dalla riga in cui cade. La striscia e il giorno non cambiano di un'ora (sono i valori che il
  motore contava da sé); cambia solo l'hero, accanto a un'ora vuota.
- Due test in `WeatherReportMapperTest`, uno per la nebbia e uno per i rovesci, con un'ora vuota
  accanto a quella corrente: senza la correzione falliscono entrambi sull'hero (3 al posto di 45,
  80 al posto di 61) e passano sulla striscia.

Verifica: `./gradlew test :app:testDebugUnitTest` verde in locale, 3 549 test, 16 saltati (le immagini
del README);
nessuna immagine del README cambia (la registrazione di Milano non ha ore vuote né CAPE).

## La firma della cache su disco conta anche l'aria (19 nov 2026)

Chiesto dal committente, dopo una scheda dei pollini sparita a Cavenago con l'APK della PR #185 e
tornata al fetch successivo. **La causa non era nella PR**: le build debug condividono la cache, e
la voce su disco l'aveva scritta un APK di prima del 17 nov, con i pollini dentro `current` e
senza `pollenHours`; il codice nuovo la mostrava come primo disegno (`cachedReport`, che non
guarda la firma, apposta: è il ripiego offline, con l'età dichiarata) finché il fetch non l'ha
riscritta. I dati del provider erano a posto: 24 ore, le sei specie a 0,0, livello «nessuno».

**Il buco trovato guardandoci**: `ReportDiskCache.CURRENT_REQUEST`, la firma che decide se una
voce su disco vale come appena scaricata, era la richiesta del forecast più quella dei testimoni,
**senza l'aria**. Se cambiasse solo la richiesta dell'aria (com'è successo il 17 nov, quando i
pollini sono passati alle ore del giorno), una voce vecchia di meno di 15 minuti passerebbe per
fresca senza `pollenHours`, e la scheda mancherebbe fino alla scadenza. Il 17 nov non è successo
solo perché nello stesso commit era cambiata anche la richiesta del forecast.

- **Fatto**: `OpenMeteoAirQualityApi.REQUEST` (le variabili correnti, quelle orarie, i giorni) entra
  nella firma. Descrive la domanda, non la risposta: un fetch con l'aria fallita scrive ancora la
  firma corrente, com'era.
- Effetto: ogni voce già su disco smette di combaciare una volta, e ogni luogo rifà un fetch al
  primo avvio; lo fa comunque la PR, con `cape`.
- Test: `ReportDiskCacheTest`, una voce con la firma di oggi meno l'aria non è la richiesta
  corrente. `./gradlew :core:data:test` verde, 678 test.

## La review finale della PR #185 (committente, 20 nov 2026)

Chiesta dal committente prima di unire la PR: il codice, i commenti, le lunghezze dei file, le
linee guida. Una review del diff (`code-review`, livello xhigh) e i controlli meccanici sulle
regole di `CLAUDE.md`; ogni rilievo riletto nel codice prima di accettarlo.

**Pulito**: la CI sul commit di testa, i nomi delle app sorelle, TODO e log, colori a mano, emoji,
gergo a schermo (la CAPE non ci arriva), le chiavi IT/EN, le soglie del codice contro STATES e
`CLAUDE.md`, il peso delle richieste (7,9), l'indice degli archivi. Le lunghezze: il mapper passa
da 951 a 1 048 righe (45% di commenti), `AlertNotifier` a 1 101; undici file del repo stanno già
sopra le mille, e nessuna regola del progetto ne fissa una. Il mapper è il primo da dividere, in un
passo suo, se si vorrà.

**Corretto** (nella PR, su richiesta del committente):

- **L'hero nel primo quarto d'ora.** Le quantità e la probabilità del blocco sono dell'ora in cui
  la sua pioggia cade (`blockHour`, 17 nov 2026), la CAPE e l'episodio della riga che comincia
  all'etichetta: alle 13:00 il blocco 12:45-13:00 leggeva l'aria e l'episodio di 13-14, e un
  rovescio finito alle 13:00 era «Pioggia» per un quarto d'ora dopo «Rovesci». Ora la CAPE e
  l'episodio sono della riga dell'ora in cui la pioggia cade; la nebbia resta della riga
  dell'istante. Test nuovo nel mapper, che senza la correzione fallisce (61 dove va 80).
- **Il riassunto del mattino e la forma breve del temporale.** Su due parti del giorno o più la
  frase intera non sta in una riga e la breve è «Possibile temporale oggi»; il temporale contava
  come detto, e la riga con le parti e «Tieni d'occhio il cielo» spariva dalla notifica. Ora è
  detto solo dalla frase intera o dalla breve su una parte sola. Test a 384 dp con la grafica vera.
- **L'episodio al bordo della serie.** La serie parte dalla mezzanotte di oggi e finisce
  all'orizzonte: una pioggia dalle 20 all'01 era un episodio di un'ora a mezzanotte, e «Rovesci»
  a 10 J/kg. Un episodio che tocca un capo della serie ora non ha durata nota, e decide la CAPE da
  sola (STATES §3, regola 3). Negli archivi le serie sono di due mesi: i due capi non spostano la
  misura di 17.6.
- **STATES §10** diceva ancora che al temporale possibile mancava l'inverno: l'ha contato 17.6
  (§11), ora lo dice anche lì.
- **`PossibleStorm`**: una riga di commento da 138 caratteri e un link KDoc spezzato a capo.
- **`analyze_against_metar.py`**: senza `tzdata` (Windows) le tabelle sul giorno del luogo di
  `--hazards` restavano vuote in silenzio; ora la misura si ferma e lo dice. Con `tzdata` l'uscita
  è identica a prima, byte per byte.

**Non fatto, e perché**: «Chance of storms» → «Chance of thunderstorms», che avevo proposto io. La
forma breve esiste per stare in una riga: «Chance of thunderstorms in the afternoon.» misura 264 dp
a 14 sp, e una notifica su un telefono da 360 dp dà a una riga 247 dp; «Chance of storms…» 216,
l'italiano 223. Resta «storms», con il motivo scritto accanto alla stringa.

**Lasciati fuori dalla PR**, con una casella in «Aperto» ciascuno: il grafico delle allerte
«Maltempo» che non sono di pioggia (c'era già prima della PR, ed è una scelta di disegno), e il
temporale possibile di oggi accanto a un'ora che il provider lascia vuota (`PossibleStorm.ahead`
legge le righe, il mapper la risposta: con un buco la riga di oggi può nominare meno parti del
campo del giorno).

Test: `:core:domain:test` e `:core:data:testDebugUnitTest` verdi (a parte il solito
`AccessDeniedException` di DataStore su Windows, verde rilanciato), `:app:testDebugUnitTest`
verde.

## Il grafico di «Maltempo»: quello che cade (committente, 20 nov 2026)

Dalla review finale della PR #185, voce sopra: sotto un «Maltempo» il grafico della notifica
aperta era la probabilità di pioggia delle dodici ore, con il picco scritto sulla barra più alta
e la finestra dell'allerta in ambra. Il temporale suona a qualunque probabilità (17 nov 2026),
e sotto «Temporale verso le 16» barre all'8% dicevano «improbabile»: la contraddizione che 17.1
aveva tolto dal testo restava nel disegno, e TalkBack la leggeva («al massimo 8%»); sotto la neve
lo chiamava «pioggia». Il vento ha il suo grafico dal 14.7, le raffiche: la grandezza di cui parla
l'allerta. Il committente ha scelto la proposta, e per il temporale i millimetri all'ora invece di
nessun grafico.

- **Fatto**: temporale e gelicidio disegnano i millimetri di ogni ora, la neve forte i
  centimetri, su una scala del mondo da 0 al doppio della soglia forte del motore (7,6 mm/h e
  0,8 cm/h, `WeatherStateEngine.RAIN_MODERATE_MAX_MM` e `SNOW_HEAVY_CM`, ora pubbliche): la soglia è
  la riga tratteggiata a metà altezza, come la soglia del vento nel suo. Un solo inchiostro per la
  grandezza, quello tenue sotto la soglia; la finestra in ambra; il massimo scritto nella sua
  unità («4,0 mm»). Dodici ore in cui non cade niente: nessun grafico, non uno vuoto. La pioggia
  forte tiene la probabilità: lì il pericolo è la pioggia, e il testo la dice.
- TalkBack: «Grafico delle precipitazioni nelle prossime 12 ore: il massimo è 4,0 mm in un'ora,
  verso le 16», e «della neve» in centimetri.
- Il codice: `NotificationCharts.amountHours`, che con `gustHours` disegna attraverso
  `quantityHours`, un solo disegno delle barre di una grandezza; `AlertNotifier.chart` sceglie per
  il tipo di pericolo, ed è `internal` per il suo test.
- DESIGN §8.17 e il CHANGELOG lo dicono. Nessuna immagine del README cambia: quella notifica è la
  sintesi del mattino.
- Test: `NotificationChartsTest` (quello che cade, niente che cade, la neve che legge la neve) e
  `AlertNotifierTest` (la descrizione per temporale, gelicidio, neve e pioggia, e nessun grafico
  dove non cade niente). `NotificationFitRenderTest` (testDebug) dava al suo temporale delle 15
  il codice e la probabilità ma non l'acqua, e senza millimetri il grafico non c'è: ora ha i suoi
  6 mm, e nessun corpo finisce in «…» su tre telefoni, tre grandezze del carattere e due lingue.
  La suite dell'app e del dominio verde, 1 777 test.

## Milano non è più scritta nel codice (committente, 22 nov 2026)

Il committente ha chiesto perché nel codice ci fosse Milano fissa, senza `IT`. Era
`CityStore.DefaultCity`, letta in un solo punto: `migrateFirstRun`, il controllo una volta per
installazione ereditato dalla Fase 14b di tweather, che a un'installazione con una cronologia e
nessun luogo salvato scriveva Milano come luogo, per non togliere a un aggiornamento la città
che il vecchio ripiego gli mostrava. Chiaro quel ripiego non l'ha mai avuto (archivio 01,
«Fase 2», «Deviazioni e rinvii registrati»: «seminarne una finta è vietato»), e la 1.0.0 (21 set
2026) è uscita con il primo avvio e il passo delle notifiche: ogni installazione pubblicata ha
fatto il controllo al suo primo avvio, a dati vuoti, e ne ha scritto solo il segno. Il ramo che
scriveva Milano si raggiungeva soltanto da una build di sviluppo di prima della 1.0 senza luoghi,
e lì restituiva una città che il lettore aveva tolto. Il record poi era monco: senza `countryCode`
né `admin3`, quella Milano non aveva la pioggia al quarto d'ora (`NowcastCoverage`), leggeva la
qualità dell'aria fuori dalla scala europea (`PlaceRegion`) e trovava la zona d'allerta solo
dalle coordinate.

- **Tolti** `CityStore.DefaultCity`, `migrateFirstRun` con la chiave `first_run_migrated`,
  `WeatherRepository.hasAnyHistory` e `WeatherHistoryDao.any` (servivano solo a lui), e
  l'`init` di `ShellViewModel`, che ora non prende più il repository.
- **`firstRun` si legge dalle due risposte**, `init_done` e poi `notify_asked`. `FirstRun.Unknown`
  resta come valore iniziale della shell finché lo store non è stato letto: la superficie vuota
  di un fotogramma, mai il primo avvio a chi l'ha già passato. Lo store non lo emette più.
- **Chi aggiorna non vede niente**: ha già le due risposte. La chiave `first_run_migrated` resta
  sul disco, innocua; cancellarla costerebbe codice per nessun guadagno. Cambia l'archivio 01,
  «Fase 3 — Luoghi e primo avvio» (la casella di `migrateFirstRun`/`firstRun` cablati nella
  shell), e l'archivio 03, «E il primo avvio: sì, ma dopo il luogo, e a parole» (`migrateFirstRun` che
  scrive `notify_asked` per gli aggiornamenti).
- Nessun'altra città è fissa nel codice dell'app: `widget_preview_place` è il testo
  dell'anteprima del widget nel selettore del launcher, `SampleWeatherReport` (New York) è dei
  soli test, e le Milano di `tools/` e della registrazione del README non arrivano nell'APK.
- Test: `CityStoreTest` (il primo avvio senza luoghi, un'installazione con il vecchio segno
  resta risposta, il salto, l'ordine luogo-notifiche) e `WeatherSyncWorkerTest` con una Milano
  propria, completa di `IT` e del comune. Nessuna immagine del README cambia, e nemmeno il
  CHANGELOG: a schermo non cambia niente.

## Fase 18 — Più modelli nei numeri e nel cielo: i passi (committente, 22 nov 2026)

**Da dove nasce.** Il committente ha chiesto una valutazione critica del motore delle previsioni
contro le app professionali, con ricerche su come trattano i dati prima di mostrarli, e un
riesame delle decisioni del repo che oggi impediscono un motore migliore. Dalle otto proposte di
quella valutazione: «espandi gli 8 punti proposti in una fase dettagliata … rianalizza ogni punto
prima di scriverlo … tutto quello che è valore aggiunto e non rumore … step sviluppabili
singolarmente in sessioni singole … ogni passo compilabile, con un APK dalla CI da provare sul
dispositivo»; il radar «valuta bene la fattibilità», e tutto il resto indipendente da lui. Alle
quattro domande della sessione ha risposto: la regola dei numeri **si cambia** («se lo ritieni
veramente corretto cambiala anche per altro oltre la temperatura … non vorrei che se non lo
facciamo ora poi in futuro abbiamo ancora la stessa limitazione»); il **radar fuori dalla
fase**; gli step di sola misura **separati** da quelli del codice; la media dell'ensemble ECMWF
**facoltativa, in coda**. Poi, aperta la PR: «vorrei comunque fare questa fase su questo branch e
vedere alla fine come va prima di mergiarla in main».

**Come lavorano gli altri**, per quel che conta nelle scelte (le fonti in fondo alla sezione):

- **NOAA NBM**: toglie l'errore sistematico di ogni modello (una media mobile esponenziale, il
  quantile mapping) e fa la media pesata di modelli NWS e non, con i pesi dall'errore degli
  ultimi 60–120 giorni.
- **Met Office IMPROVER**: fonde gli ensemble MOGREPS con il modello UKV, calibra le probabilità,
  ricava il simbolo dell'ora da un albero di decisione sulle probabilità, e fonde all'inizio un
  nowcast estrapolato dal radar.
- **meteoblue mLM**: 10–20 modelli pesati sulle stazioni per temperatura, rugiada e vento, con un
  errore sotto quello del miglior modello grezzo in ogni continente (la verifica di luglio 2026).
- **Yr (MET Norway)**: la temperatura corretta con le stazioni Netatmo, fino a 3 °C nelle prime 12
  ore.
- **The Weather Company**: circa 100 modelli fusi (WxMix). **Google MetNet-3**: radar e stazioni,
  nessun modello numerico in ingresso, fino a 24 ore.

Il motore degli stati di Chiaro ha l'impianto dei simboli di IMPROVER (lo stato ricavato dai campi
fisici con regole in ordine, non preso dal codice del modello), e quello non si rifà. Quel che
nessuno di loro fa è mostrare un modello solo. Chiaro ne scarica già cinque, `best_match` e i
quattro testimoni della nebbia, fra cui i due globali IFS e UKMO, e i testimoni servono solo alla
nebbia.

**I numeri di questa sezione** vengono da script di lavoro, non dal repo, sui due archivi di
`tools/measurements/` con le corse per giorno di anticipo: l'inverno
(`metar-2025-12-01-2026-01-31.json.gz`) e ottobre (`metar-2026-09-26-2026-10-25.json.gz`), 26
aeroporti. Lo step 18.1 li porta in `tools/analyze_against_metar.py` e deve ritrovarli. Non
decidono: i criteri di 18.2 e 18.3 decidono sull'**estate 2026**, che nessuno ha ancora scaricato
con le corse dei testimoni.

### Cosa ho trovato

**1. La temperatura: dal secondo giorno la media di tre modelli sbaglia meno di ciascuno.**
Errore medio assoluto dell'ora contro il METAR entro 30 minuti, in °C. Il giorno 1 è l'archivio
(le prime ore di ogni corsa), il giorno N è `previous_day(N−1)`. La **rampa** è `best_match`
spostato verso la media di `best_match`, IFS e UKMO (una copia di `best_match` fuori) con il peso
del criterio qui sotto, 0 fino alle 24 ore e pieno dalle 72:

| Giorno | Inverno: best_match | IFS | UKMO | rampa | Ottobre: best_match | IFS | UKMO | rampa |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | **0,90** | 1,07 | 1,36 | 0,90 | **0,86** | 1,05 | 1,18 | 0,86 |
| 2 | 1,20 | 1,33 | 1,47 | 1,17 | 1,07 | 1,18 | 1,30 | 1,02 |
| 3 | 1,32 | 1,44 | 1,58 | 1,29 | 1,18 | 1,26 | 1,40 | 1,10 |
| 4 | 1,47 | 1,58 | 1,72 | 1,45 | 1,30 | 1,36 | 1,49 | 1,21 |
| 5 | 1,67 | 1,77 | 1,92 | 1,63 | 1,49 | 1,46 | 1,65 | 1,34 |
| 6 | 2,14 | 2,00 | 2,15 | 1,85 | 1,72 | 1,66 | 1,80 | 1,49 |
| 7 | 2,45 | 2,37 | 2,43 | 2,14 | 1,89 | 1,92 | 1,98 | 1,66 |

- **Il primo giorno la media peggiora**: piena, d'inverno fa 0,90 → 0,99. ICON-D2 da solo è il
  migliore, ed è quel che la misura dell'umidità del 27 ott 2026 aveva visto. Per questo la rampa
  parte da zero; dal secondo giorno non peggiora in nessun giorno né in nessun gruppo, e in
  pianura padana fa 1,95 → 1,64 al giorno 6 d'inverno e 1,44 → 1,21 al giorno 5 d'ottobre.
- Il salto di `best_match` d'inverno fra il giorno 5 e il 6 (1,67 → 2,14) è il cambio di modello
  dentro `best_match`: la media lo smussa (1,63 → 1,85).
- **Massima e minima del giorno** (giorno UTC con almeno 18 ore di METAR, gli estremi delle ore,
  come 14.10; con la media piena): giorno 7 d'inverno, massima 2,02 → 1,87 e minima 2,79 → 2,29;
  giorno 7 d'ottobre, 1,65 → 1,43 e 2,03 → 1,77. Al giorno 1 la massima peggiora (0,75 → 0,82):
  di nuovo la rampa.
- **Le corse cambiano meno.** Le ore in cui la previsione per la stessa ora cambia di 2 °C o più
  fra una corsa e quella del giorno dopo (con la media piena): giorno 5 contro 6, 30,1% → 12,4%
  d'inverno e 23,4% → 8,3% a ottobre; giorno 2 contro 3, 9,5% → 2,7% e 9,1% → 1,2%. È il rumore
  che «Cosa è cambiato» (soglia 1 °C) oggi racconta come una revisione.
- **Costa zero chiamate**: `OpenMeteoWitnessApi` scarica già `temperature_2m` di IFS e UKMO per
  sette giorni.

**2. Il cielo: la nuvola che l'app mostra è quella della famiglia ICON, la meno giusta dei modelli
misurati.** Contro il
satellite (indice di chiarezza, sole sopra i 15°, ore senza precipitazione né nebbia viste, come
§11 di STATES), ottobre, giorno 1, 5 112 ore di luce, il cielo dalla luce che prendono gli strati
(la regola 6, `opacity` dello strumento) per ogni modello:

| Il cielo da | Giuste | HSS | Detti: sole / un po' / niente |
|---|---:|---:|---|
| `best_match` (l'app oggi) | 66% | 0,37 | 56 / 21 / 23 |
| ICON-EU | 62% | 0,33 | 50 / 27 / 23 |
| ICON-D2 (2 738 ore) | 61% | 0,33 | 49 / 22 / 29 |
| IFS | 76% | 0,44 | 75 / 10 / 15 |
| UKMO | 76% | 0,41 | 77 / 10 / 13 |
| KNMI, DMI (3 911 ore) | 74% | 0,42 | 72 / 3 / 25 e 73 / 4 / 23 |
| **Gli strati medi di `best_match`, IFS e UKMO** | **78%** | **0,50** | 70 / 19 / 12 |
| *Visti dal satellite* | | | *72 / 17 / 11* |

- In Europa `best_match` è la famiglia ICON, la più lontana dal satellite fra i modelli misurati
  (61–62% da sola, 66% come la serve `best_match`). In pianura padana gli strati medi fanno
  67% → 81% (HSS 0,37 → 0,54), altrove 66% → 76%.
- La media non è solo più giusta: dice i tre cieli quasi nella proporzione in cui si vedono, dove
  `best_match` dice «niente sole» il doppio delle volte che succede. Batte anche IFS da solo (76%,
  0,44): non è «prendi la nuvola di un altro modello».
- Ai giorni 2–7 l'archivio delle corse non ha gli strati (14.4); sulla sola copertura totale la
  media vince a ogni giorno: ore giuste 53,0% → 59,8% al giorno 3, 47,3% → 55,4% al 5, 45,7% →
  52,5% al 7; HSS 0,16 → 0,24 al giorno 5.
- **Costa 1,2 chiamate a fetch**: gli strati dei testimoni oggi non si scaricano (7,9 → 9,1).

**3. Gli otto punti della valutazione, riletti uno per uno.**

1. **«Le API a pagamento (Ensemble, Historical, Satellite, Previous Runs)»** di `CLAUDE.md` e
   dell'archivio 07 («Fase 14 — Il motore contro i motori premium: la revisione e i passi», il
   paragrafo «Il vincolo») sono un errore di fatto: la tabella di open-meteo.com/en/pricing,
   ripetuta in /en/terms, mette la spunta nella colonna *Free* per Ensemble, Historical Forecast,
   Previous Runs, Single Runs e Satellite Radiation; mancano solo al piano a pagamento più basso,
   *Standard*. Il vincolo vero resta: uso non commerciale, 10 000
   chiamate al giorno per indirizzo, il peso di ogni richiesta. → **18.1**.
2. **La media della temperatura dal terzo giorno**: confermata, e anticipata con la rampa al
   secondo (punto 1 qui sopra). → **18.2**.
3. **L'ensemble per i giorni 3–7, intervalli e probabilità del giorno bagnato**: fuori. La
   percentuale del giorno che l'app stampa (il massimo delle orarie) segue già un giorno con
   almeno tre ore di pioggia: al giorno 1, quando stampa 20–39% piove tre ore o più nel 38% dei
   giorni d'inverno e nel 22% d'ottobre; con 60–79% nel 59 e nel 62%; da 80% nel 94 e nel 90%.
   Gli intervalli non hanno un posto nella riga della settimana, e si verificano solo con i
   membri, che Open-Meteo tiene tre giorni: servirebbero mesi di raccolta. E una richiesta con i membri pesa,
   a giudicare dall'esempio della documentazione (ICON-EPS, 40 membri, una variabile: 4,0
   chiamate), come se ogni membro fosse una variabile. Dell'ensemble resta la **media**, come
   quarto modello. → **18.5**, facoltativo.
4. **Le parti del giorno dal quarto giorno**: ci sono già. Dal 31 ott 2026 la frase del giorno dice
   le parti fino al terzo giorno e nessun «quando» negli ultimi tre (VISION §5.2, punto 5).
   Ricontato oggi su `best_match` senza probabilità: l'ora bagnata al giorno 4 ha CSI 0,33–0,34,
   la parte del giorno 0,47–0,53, il giorno 0,65–0,70. La decisione regge; niente da fare.
5. **Il radar**: fuori dalla fase (committente). La valutazione è qui sotto, «Il radar: valutato,
   da decidere», e la voce in «Da decidere: se e quando».
6. **Le correzioni statiche, un MOS calcolato dagli strumenti**: fuori. Una correzione per gruppo,
   giorno di anticipo e ora UTC, imparata sugli altri aeroporti e provata su quello lasciato fuori,
   nello stesso periodo (quindi un limite superiore), guadagna 0,1 °C solo in pianura padana
   d'inverno ai giorni 3–6 (1,33 → 1,24 al giorno 3) e niente altrove né a ottobre. Sopra la media
   guadagna 0,1–0,15 °C in pianura d'inverno, perde fino a 0,03 a ottobre, e fuori dalla pianura
   sta fra −0,04 e +0,02. E una tabella ferma invecchia a ogni aggiornamento di un modello, che
   il provider non annuncia.
7. **Altri modelli**: la **media dell'ensemble ECMWF** va in 18.5. **AIFS** fuori: per la
   documentazione di Open-Meteo ha solo passi di 6 ore, e una temperatura a 6 ore perde la massima
   e la minima. **WeatherNext 2** fuori: i termini d'uso non sono verificati e non c'è un archivio
   per anticipo. **ICON-CH1/CH2** fuori: sarebbero un cambio di modello nei giorni 1–2 al nord,
   dove `best_match` è già il migliore (0,72–0,84 °C in pianura padana), e 14.4 ha già detto no al
   caso uguale di ICON-2I.
8. **«Cosa è cambiato» sui valori della media**: non è uno step. `WeatherSnapshots.flattenForecast`
   legge il report che lo schermo mostra, e la media ci arriva da sé (punto 1, «Le corse cambiano
   meno»).

E **le altre grandezze**, che il committente ha chiesto:

- **il cielo** sì, punto 2 → **18.3**;
- **vento e punto di rugiada** no: al giorno 1 la media guadagna il 3% sul vento (3,85 → 3,73 km/h
  d'inverno, 3,79 → 3,68 a ottobre) e il 7–11% sulla rugiada (0,94 → 0,87 °C, 1,05 → 0,93), ma
  l'app li mostra solo per adesso, nella griglia dei dettagli; oltre oggi il vento è solo delle
  allerte, che leggono le raffiche, e una media abbassa i picchi. Sarebbe un numero in più che
  nessuno vede;
- **quantità e probabilità di precipitazione** mai: la media di due piogge è una pioviggine che
  nessun modello ha previsto, e la probabilità è già quella di un ensemble. Un voto dei modelli sul
  giorno bagnato contraddirebbe le ore dello stesso giorno, che restano di `best_match`.

### Decisioni prese oggi

1. **La riga sulle API a pagamento si corregge** (18.1). Le API di misura sono nel piano gratuito;
   se una entra nell'app, entra con il suo peso, come ogni richiesta.
2. **La regola dei numeri cambia ora, comunque vadano le misure** (il committente). Il testo che
   18.1 scrive in `CLAUDE.md` al posto di «the app never averages two models into a number»:

   > A number on screen is what a model forecast, or a combination of models that the app has
   > measured to be closer to what happens than the single model it replaces; never a value
   > changed to make a screen agree, never a guess. A combination is a rule like a state's:
   > written down with its reason, its measurement against observations and the slice where it
   > does not hold, tested, and named where the page says where its data come from.
   > Precipitation amounts and probabilities are never averaged: the mean of two rains is a
   > drizzle no model forecast.

   La media della temperatura e quella del cielo entrano ciascuna con il suo criterio, scritto qui
   sotto prima dei numeri dell'estate. Una che non passa resta fuori, e la regola non ferma la
   prossima. Cambia l'archivio 07, «Fase 14 — Il motore contro i motori premium: la revisione e i
   passi», il paragrafo «Il vincolo», e la regola del 27 ott 2026 in `CLAUDE.md`.
3. **I modelli della media sono tre, a pari peso**: `best_match`, IFS (`ecmwf_ifs`) e UKMO
   (`ukmo_global_deterministic_10km`), già nella richiesta dei testimoni. Un testimone che è
   `best_match` stesso (la prova delle copie di `Witnesses`) o che manca resta fuori, e la media è
   di quel che resta. KNMI e DMI restano testimoni della nebbia: coprono due giorni e mezzo, e solo
   l'Europa. Un input che manca spegne solo la media (STATES §4): senza testimoni il numero è di
   `best_match`, come oggi.
4. **La temperatura**: il peso della media cresce con le ore contate dal momento del report: zero
   fino a 24, in linea retta fino a uno a 72, pieno dopo. Gli stati leggono la temperatura del
   modello dell'ora (la fase deve restare coerente con la sua neve); tutto quel che mostra o legge
   una temperatura come numero legge la media: lo schermo, i widget, le regole del lettore, le
   allerte, «Cosa è cambiato».
5. **Il cielo**: la copertura dell'ora, il totale e i tre strati, è la media dei tre modelli **dalla
   prima ora**, e diventa la nuvola dell'ora per tutti: lo stato (la regola 6 e «Velato»), il cielo
   disegnato, il nastro, il tile, il verdetto del Cielo. La parola, il disegno e il numero restano
   una cosa sola.
6. **Il radar resta fuori dalla fase**, valutato e scritto per la decisione.
7. **Gli step di sola misura sono a sé**: 18.1 e 18.4 lasciano l'APK com'era, e lo dicono.
   **Tutta la fase sta su un ramo solo**, e si unisce a `main` solo dopo 18.6 (il committente).
   Ha un precedente, la Fase 17 sul ramo `fase-17` e la sua PR #185.
8. **La media dell'ensemble ECMWF è facoltativa, in coda** (18.5), e si chiude senza codice se non
   si può misurare.

### I criteri, scritti prima dell'estate

Scritti oggi, in questo commit, prima che l'estate 2026 (1 giu–31 ago, i 26 aeroporti) sia
scaricata con le corse dei testimoni. Gli step li applicano così come sono. I due archivi già
guardati si stampano accanto e non decidono; dopo i numeri non si cercano sottoinsiemi (un
aeroporto, un mese, un'ora) in cui una regola passi.

**La temperatura (18.2).** Per aeroporto, giorno di anticipo N da 1 a 7 e ora:
T = T_bm + w·(T_media − T_bm), con T_media come nella decisione 3,
w = min(1, max(0, (h − 24) / 48)) e h = 12 + 24·(N − 1), il centro delle ore del giorno N. La
verità è il METAR entro 30 minuti dall'ora; massima e minima sono gli estremi delle ore della serie
con la media contro quelli dei METAR, sul giorno UTC con almeno 18 ore di METAR. L'archivio non
ha gli estremi propri del modello per giorno di anticipo: la misura legge gli estremi delle ore, e
l'app sposta i propri della stessa differenza (18.2). La media entra se, sull'estate:

- (a) l'errore medio orario dei giorni 5–7 insieme scende di almeno il 5%;
- (b) in nessun giorno da 1 a 7, né in pianura padana né nel resto, sale di più di 0,02 °C;
- (c) massima e minima dei giorni 4–7 insieme non peggiorano in nessuno dei due gruppi.

Si stampano, senza decidere: i due archivi; i cambi da 1 e da 2 °C fra una corsa e la successiva;
la rampa B (zero fino a 48 ore, piena da lì) e la C (zero fino a 48, piena a 120); la media con
KNMI e DMI.

**Il cielo (18.3).** Per aeroporto e ora di luce del giorno 1 (l'archivio, che ha gli strati): la
copertura totale e i tre strati sono la media di quelli dei tre modelli (un modello conta solo con
tutti e quattro i numeri), la luce presa è `opacity` sulla media, il cielo i tre secchi di §11
(sole sotto 50, un po' da 50, niente da 80). La verità è il satellite com'è in §11. La media entra
se, sull'estate:

- (a) le ore giuste salgono di almeno 5 punti e l'HSS di almeno 0,05, su tutti gli aeroporti;
- (b) né in pianura padana né nel resto scendono;
- (c) «niente sole» detto in un'ora che il satellite dice di sole (k ≥ 0,7) non è più frequente
  di oggi: è l'errore che la regola 6 aveva ridotto (§11), e non deve tornare.

Si stampano: ottobre; i giorni 2–7 sulla copertura totale; la copertura media contro le okta dei
METAR (FEW 1–2, SCT 3–4, BKN 5–7, OVC 8) di giorno e di notte, cioè il numero del tile e del
verdetto del Cielo; la quota di «Velato»; la media a cinque con KNMI e DMI.

**La media dell'ensemble (18.5).** Entra come quarto modello della temperatura se, sull'estate e
su settembre–ottobre 2026, l'errore orario dei giorni 5–7 scende di almeno il 3% rispetto alla
media a tre, e nessun giorno né gruppo peggiora di più di 0,02 °C; nel cielo con il criterio di
18.3 contro la media a tre, se la media dell'ensemble porta gli strati.

### Le regole comuni

- **Uno step per sessione, tutti sul ramo `claude/chiaro-forecast-engine-review-xg6n57`**, con
  la PR #189 in bozza dall'inizio: nessuna PR per step. La sessione parte dal ramo aggiornato
  (`git fetch origin claude/chiaro-forecast-engine-review-xg6n57`, anche se l'ambiente le assegna
  un ramo suo), ci committa lo step (il numero dello step in testa a ogni messaggio) e ci fa push.
  Se `main` è andato avanti, lo unisce nel ramo (`git merge origin/main`), mai un rebase. Alla
  fine la sezione dello step in coda a questo registro, la casella chiusa in «Aperto» e la riga
  dello step spuntata nella descrizione della PR. Il committente unisce la PR dopo 18.6.
- **Solo rispetto a `main`** (il committente, 22 nov 2026): il punto di partenza, il codice da
  cambiare e il termine di ogni confronto sono quelli di `main`. Nessun altro ramo né PR è una
  fonte di questa fase, né per il codice né per le misure.
- **Ogni step lascia la build verde e un APK installabile dalla CI**: `./gradlew test
  :app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` (nel container con lo script del
  mirror, `CLAUDE.md`). La CI costruisce l'APK a ogni push del ramo: ogni step si prova
  dall'artifact del suo commit (la build debug si installa accanto alla release, ed è il confronto
  «prima e dopo»). La sezione dello step dice che cosa provare sul telefono, o che l'APK è quello
  di prima e non c'è niente da provare.
- **Prima la misura, nel repo**: la riga in `tools/analyze_against_metar.py`, il criterio di questa
  sezione così com'è, poi il codice. Uno step che non passa il suo criterio si chiude senza codice,
  e dice dove non passa.
- **I dati si scaricano sul runner.** Il container cloud riceve 429 da Open-Meteo (22 nov 2026:
  «Daily API request limit exceeded», l'indirizzo è condiviso). Il download e il completamento
  passano dal workflow `Measure against METAR`, che committa sul ramo; lo lancia la sessione con
  lo strumento GitHub, o il committente dal tab Actions. Mai uno script di download nella
  sessione.
- **Il codice**: la media in `:core:domain`, pura, una classe sola per le due grandezze
  (`ModelMean`, il nome è indicativo); il mapper la chiama dove la grandezza entra nel report.
  Nessun parametro tarato dopo i numeri: i pesi e la rampa sono quelli scritti qui. 18.2 e 18.3 non
  dipendono l'uno dall'altro: chi arriva prima scrive la classe.
- Ogni variabile aggiunta scrive il nuovo peso accanto alla lista, in `CLAUDE.md` e nella sonda
  (`tools/probe_provider.py`); il cambio della richiesta cambia da sé la firma della cache
  (`CURRENT_REQUEST`).
- Una regola che cambia, cambia in `docs/STATES.md`, nel codice e nel suo test nello stesso step.
  Un numero che cambia origine lo dicono, nello stesso step, la guida (IT/EN), la tabella delle
  fonti del README e il piede della pagina, senza gergo: «la media di tre modelli», non
  `best_match`.
- **Le immagini del README**: la registrazione di Milano non ha i testimoni, e senza testimoni la
  media non c'è; 18.2 e 18.3 non dovrebbero quindi cambiare nessuna immagine (se una cambia, si
  rigenera e si guarda nello step, come sempre). La registrazione si rifà una volta, dopo i due,
  in 18.4.
- Beta: nessuna compatibilità da tenere. Al primo fetch dopo l'aggiornamento «Cosa è cambiato» può
  segnare una volta un giorno cambiato dal metodo e non dal tempo; lo step che cambia un numero lo
  dice nella sua sezione.

### Gli step

**18.1 — La misura nel repo, l'estate scaricata, le due righe di `CLAUDE.md`.** Solo strumenti e
documenti: l'APK è quello di prima.

- `CLAUDE.md`, «Free and without a key is a constraint»: le API di misura sono nel piano gratuito,
  il limite resta l'uso non commerciale e il peso. Il punto «Numbers are one model's per hour»
  riscritto con il testo della decisione 2, che vale da subito anche se ancora nessuna media è nel
  codice. `docs/STATES.md`, la riga in testa («Nothing here changes an amount…»), allineata al
  principio nuovo (le medie stesse le scrivono 18.2 e 18.3). La frase del README «the numbers on
  screen are always the model's» resta fino a 18.2, perché fino a lì è vera.
- `tools/analyze_against_metar.py`: `--blend` (il punto 1: ogni modello, la media, le tre rampe,
  massima e minima, i cambi fra le corse, per gruppo) e `--sky-blend` (il punto 2: ogni modello,
  gli strati medi, i giorni 2–7 sulla copertura totale, le okta dei METAR, «Velato»). Si rilanciano
  sui due archivi e devono ridare i numeri di questa sezione, o lo step scrive quelli nuovi e
  perché sono diversi (le copie, per esempio, si riconoscono con la prova dell'app).
- `tools/measure_against_metar.py`: verificare che il download di un periodo nuovo porti tutto
  quel che i due criteri leggono (il satellite, gli strati dei tre modelli al giorno 1, le corse dei
  testimoni ai giorni 2–7); aggiungere quel che manca.
- Lanciare `Measure against METAR` sul ramo con `start=2026-06-01` e `end=2026-08-31`, poi con
  `start` vuoto per completare il file con le corse. Il workflow committa sul ramo (anche da solo,
  a ogni push che tocca `measure_against_metar.py`): prima di un push dopo il suo, `git pull`. Lo
  step non aspetta l'esito: finisce quando il workflow è partito, e 18.2 comincia controllando che
  il file ci sia.

**18.2 — La temperatura dal secondo giorno: la media dei tre modelli.**

- Prima la misura: `--blend` sull'estate, con il criterio così com'è. Se non passa, lo step finisce
  lì.
- `:core:domain`: la media (decisione 3) e la rampa (decisione 4), con i loro test: le copie, un
  testimone che manca, l'ora 0, 24, 48, 72 e 168.
- Il mapper, dopo gli stati: la temperatura di ogni ora diventa la media pesata. La massima e la
  minima del giorno restano gli estremi propri del modello (archivio 16, «Fase 17», «Cosa ho
  trovato», punto 6), spostati della differenza fra l'estremo delle ore con la media e quello delle
  ore senza. La percepita si sposta della stessa differenza della sua ora, perché resti quella che
  il modello dà rispetto alla temperatura. Gli stati del motore leggono ancora la temperatura del modello. Il
  momento del report, non quello in cui si guarda, fissa le ore: lo stesso report dice sempre gli
  stessi numeri.
- Chi la legge: il presente e le prime 24 ore non cambiano (il peso è zero), quindi l'hero, i
  dettagli, i quarti d'ora e le notifiche di oggi restano come sono. Cambiano le ore e le righe
  della settimana dal secondo giorno, i widget che le mostrano, le regole del lettore e le allerte
  che leggono quei giorni, «Cosa è cambiato».
- `docs/STATES.md`: una sezione nuova, «The temperature from the second day», con la regola, la
  rampa, la misura e il limite (il primo giorno). La guida (IT/EN): da dove vengono le temperature
  dei giorni lontani, in una frase. Il README: la tabella delle fonti e la frase «the numbers on
  screen are always the model's». Il CHANGELOG.
- Cosa provare sul telefono: le righe della settimana accanto alla versione di prima (domani poco
  o niente, dal terzo giorno massime e minime diverse di qualche decimo, a volte di un grado o due
  ai giorni 6–7); un giorno lontano
  aperto, con le ore coerenti con la sua riga; l'hero e le prime 24 ore identici; da un fetch
  all'altro, nei giorni 5–7, numeri che si muovono meno.

**18.3 — Il cielo dei tre modelli.**

- Prima la misura: `--sky-blend` sull'estate, con il criterio così com'è. Se non passa, lo step
  finisce lì.
- La richiesta dei testimoni: `cloud_cover_low`, `cloud_cover_mid`, `cloud_cover_high` (+1,2
  chiamate, il fetch 7,9 → 9,1), con il peso accanto alla lista, in `CLAUDE.md` e nella sonda.
- Il mapper, prima degli stati: la copertura di ogni ora e del blocco corrente (con i testimoni
  della riga in cui cade, come la nebbia) è la media dei tre (decisione 5). Tutto quel che legge la
  nuvola la legge da lì: `WeatherStateEngine` (la regola 6, «Velato»), `CloudLayers.lightTaken`
  per il cielo disegnato e il nastro, `WeatherText.cloudMeaning` e il tile, il verdetto del Cielo.
  La nebbia (regola 5) non cambia: vota con i suoi numeri.
- `docs/STATES.md`: la regola 6 con la media; §11 con i numeri nuovi (le ore giuste, «niente sole»
  sotto il sole, la quota di «Velato»); §4 (senza testimoni il cielo è di `best_match`); §10 (la
  notte misurata solo con le okta, l'inverno senza satellite). La guida, il README, il CHANGELOG.
- Cosa provare sul telefono: la parola dell'hero contro il cielo fuori dalla finestra, per qualche
  giorno; il cielo disegnato, il nastro della settimana e il tile delle nuvole che dicono la stessa
  cosa; una notte nel verdetto del Cielo; una mattina di nebbia che resta nebbia.

**18.4 — La registrazione del README con i tre modelli.** Solo test, risorse e immagini: l'APK è
quello di prima. Si fa se almeno uno fra 18.2 e 18.3 è entrato; altrimenti si chiude senza lavoro.

- `tools/record_readme_data.py` registra anche la risposta dei testimoni, con le variabili della
  richiesta di oggi; `ReadmeScreenshots` la legge.
- La registrazione si rifà (`python3 tools/record_readme_data.py`) in un commit suo, perché cambia
  ogni immagine. Se dal container Open-Meteo risponde 429, la lancia il committente dal PC e lo
  step riparte da lì.
- Le immagini si rigenerano e si guardano tutte; le didascalie e gli alt del README si rileggono.

**18.5 — La media dell'ensemble ECMWF come quarto modello** (facoltativo, e solo se 18.2 è
entrato).

- Prima il cancello, dal runner (nella sonda o in una riga sua): l'Ensemble Mean API dà la media di
  `ecmwf_ifs025` per giorno di anticipo nel passato (Previous Runs o Single Runs), o solo le prime
  ore di ogni corsa? Quanto pesa una richiesta? Ha gli strati delle nuvole? Senza un archivio per
  anticipo lo step si chiude senza codice, e lo dice.
- Con l'archivio: la misura con il criterio scritto qui; se passa, una richiesta in più (le sole
  variabili che servono, il peso scritto), il quarto modello nella media, i test, STATES, la guida.
- Cosa provare sul telefono, se entra: come 18.2, ai giorni 5–7.

**18.6 — La chiusura: la fase riletta, la PR rivista, l'unione.**

- La fase riletta contro i suoi criteri: che cosa è entrato, che cosa no e perché, in una tabella;
  `docs/STATES.md` riletto nelle sezioni toccate; la guida, il README e il CHANGELOG
  (`[Unreleased]`) che dicono la stessa cosa.
- Il ramo aggiornato con `main` (merge) e la CI verde sull'ultimo commit; una review dell'intero
  diff della PR (`code-review`) e i controlli meccanici di `CLAUDE.md` (colori a mano, gergo a
  schermo, le chiavi IT/EN, i pesi delle richieste, i nomi delle app sorelle).
- «Aperto» riletto: le prove dal vivo della fase restano caselle (la parola del cielo d'estate, un
  inverno con la temperatura dei giorni lontani).
- Cosa provare sul telefono: l'APK dell'ultimo commit, la fase intera. Il committente decide se
  unire.

### Il radar: valutato, da decidere

Fuori dalla fase per scelta del committente. È scritto qui perché la decisione, quando verrà, parta
dai fatti, come VISION §12.5 chiede.

**Il valore.** Il radar è l'unica fonte libera che risponde a «piove adesso, qui?» con
un'osservazione invece che con un modello, e solo in Italia. Quanto conta l'ha già contato la Fase
17 (archivio 16, «Cosa ho trovato», punto 1): il 27,9% delle ore di pioggia vista cade sotto una
cella che non mostra nulla (19,3% in pianura padana), e un'ora che l'app dice bagnata è asciutta una
volta su quattro. Sono le ore in cui si apre un'app del meteo. Il secondo valore sono le prossime
due ore: il quarto d'ora è calcolato solo dove `best_match` è ICON-D2 o HRRR (`NowcastCoverage`), a
sud di ICON-D2 è l'ora divisa in quattro (STATES §5), e un'estrapolazione del radar sarebbe lì il
primo nowcast vero.

**Che cosa c'è** (misurato il 9 set 2026, archivio 02, la tabella delle misure della Fase 11; e il
22 nov 2026):

- Radar-DPC: le tile WMTS (3,5 KB a zoom 6, solo l'ultima immagine, il parametro `time` ignorato),
  una REST per il GeoTIFF ogni 5 minuti (VMI 619 KB a 1 km), l'ultima immagine vecchia di 11
  minuti, 40 radar attivi su 50, la licenza CC BY-SA 4.0.
- **Il 22 nov 2026 l'API risponde «Access Denied» (Akamai, HTTP 403) a questo container cloud.** Il
  blocco è probabilmente per gli indirizzi non italiani o di datacenter: da verificare da una rete
  mobile italiana, da un telefono all'estero che guarda un luogo italiano e da un runner di GitHub.
  Fino ad allora la misura si fa solo da una macchina in Italia.

**Che cosa costa**, in due pezzi che si decidono uno per volta:

- **«Piove adesso»**, dalla tile ad alto zoom sul punto (lo zoom più alto servito e la tavolozza
  sono da verificare in R1): una PNG di pochi KB per controllo, decodificata con `BitmapFactory`,
  il colore riportato alla classe d'intensità della legenda. Solo
  con la pagina aperta, solo per un luogo in Italia, una cache di 5 minuti: batteria e dati
  trascurabili. I rischi: la tavolozza che cambia senza avviso (serve una sentinella nella sonda,
  come per il bollettino) e la copertura. **Un radar che non vede il luogo deve tacere, mai dire
  «asciutto»**: è il difetto di Lubiana (VISION §12.8). Il codice: una classe per la tile, il suo
  ingresso nella regola dell'hero (la precipitazione viene prima del cielo, quindi niente tocca
  18.3), il testo della sezione delle due ore quando il radar e il modello non concordano (oggi un
  hero bagnato su quarti asciutti dice «La pioggia smette a momenti», e col radar sarebbe falso),
  l'attribuzione nel piede e nella guida.
- **Le prossime due ore dal radar**, un nowcast per estrapolazione: servono le immagini passate,
  quindi la REST e il GeoTIFF (circa 620 KB l'una, almeno due alla volta), un lettore TIFF scritto
  in Kotlin (Android non ne ha uno), la stima del moto e lo spostamento delle celle, e uno
  strumento di misura suo, contro la persistenza e contro i quarti del modello a 30, 60 e 90
  minuti. Da 1,2 a 1,8 MB a ogni aggiornamento con la pioggia vicina. È il pezzo grande, e non si
  dimensiona prima della misura.

**Indipendente dal resto della fase**: tocca il blocco corrente e la sezione delle due ore; la fase
tocca le temperature dal secondo giorno e la nuvola dell'ora. Non ne dipende e non la blocca.

**LibreWXR** (segnalato dal committente il 22 nov 2026, letto e provato lo stesso giorno:
librewxr.net, i termini aggiornati al 17 nov 2026, il repository `JoshuaKimsey/LibreWXR`). È
un'API radar aperta (AGPL-3.0), compatibile con quella di RainViewer, con un'istanza pubblica
gratuita e senza chiave (`api.librewxr.net`) tenuta da una persona sola. Per l'Italia compone il
mosaico della Protezione Civile (24 radar), per il resto d'Europa OPERA (184 radar, 27 paesi), e
dove nessun radar copre mette una stima dal satellite (NOAA RRQPE, dall'infrarosso). Dà le ultime
due ore a passi di 10 minuti e **un nowcast di 60 minuti** (dichiarato sperimentale: l'estrapolazione
del radar fusa con modelli), in tile PNG fino a zoom 12. Provato da questo container: risponde
(dietro Cloudflare, cache di 5 minuti) dove l'API della Protezione Civile dava «Access Denied»; una
tile vuota pesa 334 byte, una a zoom 3 sull'Europa 4–7 KB; lo schema 0 («Black and White») è in
toni di grigio che sembrano codificare l'intensità (da verificare in R1).

Che cosa cambia nella valutazione:

- **R1 si può fare da qui**, senza una macchina in Italia. E i due pezzi costano molto meno: il
  lettore GeoTIFF, la stima del moto e lo spostamento delle celle li fa il server; l'app legge
  poche tile sul punto, l'ultima immagine e le sei del nowcast. R3 passa da «il pezzo grande» alla
  lettura di sei tile in più, più la sua misura.
- **Non solo l'Italia**: OPERA copre il resto d'Europa.
- **I rischi, che sono il prezzo**: un servizio di una persona, nato quest'anno (dopo i cambi di
  RainViewer del 1 gen 2026), senza SLA, che i suoi termini permettono di limitare o spegnere
  «at any time, with or without notice»; le tile dell'Italia sotto CC BY-SA 4.0, con
  l'attribuzione «Radar-DPC» e «Weather data via LibreWXR»; il nowcast sperimentale. E il più
  serio: **la stima dal satellite riempie i buchi dei radar nella stessa tile**, e un pixel «piove»
  che viene dall'infrarosso è una stima, non un'osservazione; dalla sola tile l'app non sa
  distinguerli (da verificare: se l'API dice la fonte o la copertura per pixel). Poi una cosa nuova
  per il lettore: la posizione approssimata del luogo (la tile che si chiede) va a un terzo che oggi
  non la riceve, e la guida lo deve dire. La licenza AGPL del server non tocca l'app, che non ne
  include il codice.
- **La regola che li rende accettabili**: il radar è un'aggiunta, mai una dipendenza. Se LibreWXR
  non risponde, se la tile non dice la fonte, o se il luogo è fuori copertura, l'app fa quel che fa
  oggi (il modello), senza dire niente di diverso.
- I termini vietano il traffico automatico sostenuto: una raccolta per la misura di R1 (13
  aeroporti ogni 10 minuti per qualche settimana) va chiesta prima all'operatore (il contatto è nei
  termini).

**I passi, se si decide**: **R1**, la misura, con LibreWXR da qualunque macchina, con l'API della
Protezione Civile solo da una macchina in Italia (il PC del committente, o una sessione collegata
con Remote Control): la raggiungibilità, il ritardo, la copertura, quanto
indietro va l'archivio, e il valore, cioè «piove adesso» del radar contro l'ora dell'app e contro i
METAR dei 13 aeroporti italiani (POD, falsi allarmi, HSS; in pianura padana e nel resto), e dove
il radar stesso sbaglia (la pioviggine che non vede, le valli alpine in ombra). **R2**, «Piove
adesso», solo se R1 lo dice. **R3**, il nowcast, solo dopo R2 e con una misura sua. Il mio parere:
R1 vale una serata; R2 è probabilmente il miglioramento più visibile per un lettore italiano nei
giorni di pioggia, a un costo piccolo; R3 solo se la sua misura mostra a 30–60 minuti un vantaggio
largo sui quarti del modello. Con LibreWXR R1 e R2 conviene deciderli presto, e il costo di R3
scende; il rischio, un servizio di una persona sola, resta, e lo regge solo la regola qui sopra.

**Le fonti** della valutazione: open-meteo.com/en/pricing e /en/terms (il piano gratuito),
/en/docs/ensemble-api e /en/docs/ensemble-mean-api (i membri, la media e la sua conservazione),
/en/docs/previous-runs-api, /en/docs/ecmwf-api (AIFS a 6 ore, la probabilità dall'ensemble IFS);
blend.mdl.nws.noaa.gov/nbm-documentation (NBM); Roberts et al. 2023, BAMS, e EGU23-5837 (IMPROVER e
i suoi simboli); content.meteoblue.com, «meteoblue Learning MultiModel (mLM)»; tnipen.github.io,
«titanlib gridpp», e la presentazione WMO di MET Norway (Netatmo in Yr); research.google,
«MetNet-3».

## 18.1 — La misura nel repo, l'estate scaricata, le due righe di `CLAUDE.md` (committente, 22 nov 2026)

Lo step della Fase 18 («Gli step», «18.1»), chiesto dal committente in una sessione sua, sul ramo
`claude/chiaro-forecast-engine-review-xg6n57` e nella PR #189. **Solo strumenti e documenti:
l'APK è quello di prima, non c'è niente da provare sul telefono.**

**`CLAUDE.md` e `docs/STATES.md`.** Il punto «Numbers are one model's per hour» diventa «Numbers
are what models forecast; states are the app's», con il testo della decisione 2 parola per parola
al posto di «the app never averages two models into a number» e la data del cambio; la frase sui
testimoni dice ora che quel che uno stato legge da più modelli arriva a schermo come numero solo
come una combinazione che ha passato la sua misura (l'umidità media no). Nel punto «Free and
without a key», al posto delle «API a pagamento»: Ensemble, Historical Forecast, Previous Runs,
Single Runs e Satellite Radiation sono nel piano gratuito (pricing e terms, letti il 22 nov
2026), il limite è l'uso non commerciale e il peso, e un'API di misura entra nell'app solo con il
suo peso scritto. In STATES la riga in testa: un numero a schermo è di un modello o di una
combinazione misurata, scritta nella sua sezione («none yet»), quantità e probabilità mai. La
frase del README resta fino a 18.2, com'era scritto.

**`tools/analyze_against_metar.py`, `--blend` e `--sky-blend`**, un rapporto per periodo, con il
criterio di 18.2 o di 18.3 stampato in fondo a ciascuno (decide solo l'estate). La media è quella
della decisione 3: `best_match` più IFS e UKMO, ciascuno fuori nell'ora in cui manca o in cui la
prova dell'app (`same_model`, sei ore per parte) lo riconosce come `best_match`; KNMI e DMI solo
nella media a cinque stampata. Il giorno 1 è l'archivio, il giorno N `previous_day(N−1)` delle
corse dei testimoni; le ore della temperatura sono quelle con il METAR entro 30 minuti e i tre
modelli tutti presenti. Rilanciati sui due archivi:

- **la temperatura oraria torna**: `best_match`, IFS, UKMO e la rampa A, d'inverno e a ottobre,
  sono i numeri della tabella di «Cosa ho trovato», punto 1, a ogni giorno, salvo l'inverno al
  giorno 5 (rampa 1,62, non 1,63) e due celle per gruppo che il terzo decimale sposta (pianura
  padana, inverno giorno 6: 1,96 → 1,64, non 1,95; ottobre giorno 5: 1,44 → 1,20, non 1,21);
- **il cielo torna**: ogni riga della tabella del punto 2 (le ore, le giuste, l'HSS, i tre detti,
  i visti dal satellite), la pianura padana 67% → 81% (HSS 0,37 → 0,54) e il resto 66% → 76%; i
  giorni 2–7 sulla copertura totale a un decimo (giorno 3: 53,0% → 59,9%, non 59,8; giorno 7:
  45,7% → 52,7%, non 52,5; HSS 0,16 → 0,24 al giorno 5);
- **le corse che cambiano** di 2 °C tornano a ottobre al giorno 2 contro 3 (9,1% → 1,2%), di
  pochi decimi altrove (inverno, 5 contro 6: 30,3% → 12,3%, non 30,1% → 12,4%; ottobre 23,5% →
  8,0%, non 23,4% → 8,3%; inverno 2 contro 3: 9,6% → 2,9%, non 9,5% → 2,7%). La differenza è la
  prova delle copie: senza, la media ridà 12,5%, 8,3% e 2,7%. Lo script di lavoro della sezione
  non la applicava qui; lo strumento la applica, come la decisione 3 chiede;
- **la massima e la minima con la media non tornano, e la differenza conta.** I numeri della
  sezione (inverno giorno 7: massima 2,02 → 1,87, minima 2,79 → 2,29, giorno 1: 0,75 → 0,82;
  ottobre giorno 7: 1,65 → 1,43 e 2,03 → 1,77) sono **la media degli estremi propri dei tre
  modelli**, senza la prova delle copie: lo strumento li ridà esatti così. Il criterio (c) e
  l'app di 18.2 leggono invece **gli estremi delle ore della serie con la media**, e la massima di
  una media di ore è più bassa della media delle massime. Letti così: inverno giorno 7, massima
  2,02 → 2,01 e minima 2,79 → 2,33; giorno 1 con la media piena 0,75 → 0,89. **Con quella
  definizione il criterio (c) non passa l'inverno**: giorni 4–7, fuori dalla pianura padana, la
  massima peggiora (1,53 → 1,61 °C; la pianura 2,00 → 1,92, le minime 1,64 → 1,46 e 2,33 → 2,07).
  Ottobre passa (a), (b) e (c). Lo strumento stampa le due definizioni, il criterio e, sotto, «media
  degli estremi propri». **Il criterio non si tocca qui**: decide sull'estate così com'è scritto,
  e l'inverno non decide. Ma se l'estate dice lo stesso, 18.2 si chiude senza codice sulla
  condizione (c), o il committente sceglie prima del verdetto se la massima e la minima della
  media siano gli estremi delle ore (com'è scritto) o la media degli estremi dei modelli (com'era
  misurato). È una domanda per lui, aperta in «Aperto».

Il resto si stampa senza decidere, come chiedono i criteri: le rampe B e C, la media a cinque, i
cambi da 1 °C. A ottobre la copertura media è più vicina alle okta dei METAR di quella di
`best_match`, di giorno (2,81 → 2,35 okta, la classe giusta 21% → 31%) e di notte (3,00 → 2,55;
21% → 28%), e d'inverno lo stesso (giorno 2,41 → 2,18, notte 2,61 → 2,38); «Velato» passa dal
7,2% al 7,6% delle ore di luce, con il sole visto in tre su quattro in entrambi i casi. L'inverno
non ha il satellite: le righe del cielo restano vuote.

**`tools/measure_against_metar.py`: niente da aggiungere.** Un periodo nuovo porta già tutto quel
che i due criteri leggono: l'archivio di ogni modello di `MODELS` con la copertura e i tre strati
(il giorno 1 della temperatura e del cielo), il satellite, i METAR con le nubi per strato (le
okta) e, al completamento, le corse dei testimoni per giorno di anticipo (`--add-witness-runs`, la
temperatura e la copertura di `best_match`, IFS, UKMO, KNMI e DMI ai giorni 1–7), con la
probabilità, le corse italiane e quelle bagnate.

**L'estate sul runner.** Lanciato `Measure against METAR` sul ramo con `start=2026-06-01` ed
`end=2026-08-31` (run 16, 22 nov 2026 alle 21:25 UTC). Il completamento con `start` vuoto parte
**dopo** che il primo ha committato, non in coda dietro di lui: un workflow lanciato a mano
prende il commit del ramo nel momento in cui parte la richiesta, e in coda non vedrebbe il file
nuovo. Per la stessa ragione la sessione non fa push mentre il workflow scarica: il suo commit
finale (`git push origin HEAD:<ramo>`) sarebbe rifiutato da un ramo andato avanti, e la misura
persa. Vale anche per 18.2: prima di un push, il workflow finito e `git pull`.

## 18.2 — La temperatura dal secondo giorno: non entra (committente, 22 nov 2026)

Lo step della Fase 18 («Gli step», «18.2»), chiesto dal committente nella stessa sessione di 18.1
(«Puoi partire qui con lo step successivo?»), sul ramo `claude/chiaro-forecast-engine-review-xg6n57`
e nella PR #189. **Si chiude senza codice, come le regole comuni chiedono a uno step che non passa
il suo criterio: l'APK è quello di prima, non c'è niente da provare sul telefono.**

**Prima della misura, la domanda di 18.1** («La massima e la minima della media, prima del
verdetto di 18.2»): il committente ha scelto **il criterio com'è scritto**, gli estremi delle ore
della serie con la media, prima che l'estate fosse letta.

**L'estate.** Il file `tools/measurements/metar-2026-06-01-2026-08-31.json.gz` è completo: 26
aeroporti, il satellite a 25 (Longyearbyen no), le corse dei testimoni a tutti e 26 (run 16 il
download, run 17 il completamento, committato alle 23:33 UTC). Ai giorni 4, 5 e 6 le ore con i tre
modelli sono un po' meno (52 224, 53 594 e 48 611 contro 57 207): corse che il provider non ha
per qualche giorno, contate come mancano. `--blend` sull'estate, con il criterio così com'è:

| | giorno 1 | 2 | 3 | 4 | 5 | 6 | 7 |
|---|---:|---:|---:|---:|---:|---:|---:|
| `best_match` | 1,02 | 1,25 | 1,23 | 1,32 | 1,47 | 1,75 | 1,96 |
| IFS | 1,15 | 1,27 | 1,36 | 1,43 | 1,57 | 1,72 | 1,97 |
| UKMO | 1,32 | 1,47 | 1,54 | 1,65 | 1,82 | 1,99 | 2,17 |
| media a tre | 0,93 | 1,09 | 1,16 | 1,24 | 1,37 | 1,52 | 1,71 |
| rampa A | 1,02 | 1,16 | 1,14 | 1,24 | 1,37 | 1,52 | 1,71 |

- **(a) passa**: l'errore orario dei giorni 5–7 scende dell'11,3% (1,734 → 1,537 °C).
- **(b) passa**: nessun giorno né gruppo sale; il peggiore è il giorno 1, dove la rampa è zero.
- **(c) non passa**: giorni 4–7, **la minima in pianura padana peggiora, 1,48 → 1,68 °C**, giorno
  per giorno (1,30 → 1,46 al giorno 4, 1,65 → 1,91 al 7). Le altre tre migliorano o restano: la
  massima in pianura 1,47 → 1,24, fuori 1,61 → 1,44, la minima fuori 1,63 → 1,61.
- **Con l'altra definizione non sarebbe cambiato niente**: la media delle minime proprie dei
  modelli, stampata e non decide, fa in pianura 1,48 → 1,60. Peggiora anche lei.

Si stampano, senza decidere: l'inverno non passa (c) per la massima fuori dalla pianura (18.1),
ottobre passa tutto; le rampe B e C e la media a cinque, dal giorno 4 uguali alla A; le corse
cambiano meno anche d'estate (2 °C o più fra il giorno 5 e il 6: 21,0% → 7,9%). E al giorno 1 la
media piena, d'estate, sarebbe meglio di `best_match` (1,02 → 0,93), non peggio come d'inverno:
non decide, perché il criterio è scritto sulla rampa.

**Quindi**: nessuna media della temperatura nel codice, la frase del README («the numbers on
screen are always the model's») resta vera e resta, la guida e il CHANGELOG non cambiano.
`docs/STATES.md`, la riga in testa: la temperatura dal secondo giorno misurata e lasciata fuori.
La regola dei numeri di `CLAUDE.md` resta com'è (decisione 2: cambiava comunque andassero le
misure). Dopo i numeri non si cerca un sottoinsieme in cui passi (i criteri): nessuna variante
senza la pianura, senza le minime o con un'altra rampa.

**Che cosa cambia per il resto della fase**: **18.5 cade**, perché lo step stesso dice «solo se
18.2 è entrato»; **18.4** dipende ora solo da 18.3; **18.3** non dipende da questo («18.2 e 18.3
non dipendono l'uno dall'altro»), e chi arriva scrive la classe della media.
