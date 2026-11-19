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
- **Aperta il 19 nov 2026**: **Fase 17**, il motore dopo la review esterna: sul ramo `fase-17`, un commit per step. Chiuso il 17.1 (due testi che si contraddicono); restano, in quest'ordine, 17.4–17.5 la CAPE e il temporale possibile del giorno, 17.2 «possibile» anche dove il modello piove e l'ensemble no, in aria non convettiva, 17.6 i rovesci dalla CAPE e dalla forma, 17.7 la nebbia dal quarto giorno all'anticipo vero, 17.8 la chiusura; il 17.3 è uscito dalla fase. La review, le misure, le decisioni, le regole comuni e la descrizione di ogni step sono nel registro qui sotto, «Fase 17 — Il motore dopo la review esterna»; l'ordine e gli step cambiati in «La Fase 17 riordinata», che vale dove le due non concordano.
- **Nessuna fase attiva** dal 16 al 19 nov 2026: quel che resta in «Aperto» sono una prova sul telefono, una misura che aspetta l'inverno e, dal 17 nov 2026, la nebbia dei due modelli da vedere dal vivo dopo il 30 novembre. Dal 17 nov 2026 anche le caselle della review del motore: due prove dal vivo e «Velato», fatto lo stesso giorno (registro, «Velato: fatto») con la sua prova dal vivo; il temporale dedotto fuori dalla famiglia ICON è fra le cose «Da decidere: se e quando». Il registro dal 16 al 17 nov 2026 è in `docs/planning/15-novembre-16-17.md` (la nebbia dei due modelli, la review del motore, il passo di «Velato»), quello fino al 16 nov 2026 in `docs/planning/14-novembre-12-16.md`.
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
  Fatto il 17 nov 2026 (registro, «Velato: fatto»): il disegno scelto su una tavola, le altre
  quattro decisioni confermate, il passo dell'archivio 15 eseguito.
- [ ] **«Velato» dal vivo**: un giorno di cirri fitti, la pagina che dice «Velato» sotto un cielo
  lattiginoso con il sole che fa ombra; la stessa parola di notte, con le stelle che si vedono
  appena; il disegno nella striscia oraria accanto a «Quasi sereno» e a «Nuvoloso», che non si
  confonda con nessuno dei due, fermo e in movimento. E sull'inverno, con «Il cielo d'inverno
  contro il satellite»: `tools/analyze_against_metar.py --veiled` sull'archivio che avrà il
  satellite di dicembre (registro, «Velato: fatto», «Cosa conferma solo il vivo»).
- [ ] **Le stelle cadenti, sul telefono** (registro, «Le stelle cadenti si vedono», 17 nov 2026):
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
- [x] **17.1** Due testi che si contraddicono: «gli ultimi dati sono di Aggiornato 2 ore fa», e «pioggia N%» sotto un temporale, un gelicidio o una neve nella notifica «Maltempo» (Fase 17). Fatto il 19 nov 2026 (registro, «17.1»): la probabilità solo sotto la pioggia forte, la frase della stima in due frasi.
- [ ] **17.2** «Possibile» anche dove il modello piove e l'ensemble no, solo in aria non convettiva (CAPE sotto 100 J/kg); dopo il 17.4 (Fase 17; registro, «La Fase 17 riordinata»).
- [x] **17.3** Il blocco `daily` ricavato dalle ore: sette variabili in meno, 0,7 chiamate a fetch (Fase 17). **Non fatto**: uscito dalla fase il 19 nov 2026 e passato a «Da decidere: se e quando» (registro, «La Fase 17 riordinata»).
- [ ] **17.4** La CAPE nel report e il temporale possibile del giorno: la variabile, il modello, il mapper, la misura nello strumento (Fase 17).
- [ ] **17.5** Il temporale possibile del giorno a schermo: il giorno aperto, la frase, le due sintesi; mai un'icona d'ora, mai una notifica (Fase 17).
- [ ] **17.6** «Rovesci» dalla CAPE e dalla forma, non da `showers`: prima la misura d'inverno, poi la regola (Fase 17).
- [ ] **17.7** La nebbia dal quarto giorno: la misura all'anticipo vero, poi la decisione (Fase 17).
- [ ] **17.8** La chiusura: la fase riletta, STATES riletto intero, la guida, il CHANGELOG (Fase 17).

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
- **Un temporale dedotto dalla CAPE dove il provider non scrive il codice** (archivio 15, «La
  review del motore, punto 4», «Trovato, e non toccato», 17 nov 2026). Fuori dalla famiglia ICON
  (Stati Uniti, Regno Unito, Olanda, paesi nordici, Giappone, Singapore) l'app non dice mai
  «Temporale». Dedotto dalla CAPE vedrebbe tre volte i giorni di temporale con la stessa
  precisione sul giorno (54% contro 57%) e sbaglierebbe l'ora nove volte su dieci. Non ora:
  costa 0,1 chiamate a ogni fetch ovunque, anche in Italia dove il codice c'è e dove la stessa
  regola fa peggio del codice; è un pericolo che l'app inventerebbe, contro la regola 1; e
  chiede un concetto nuovo, un «Temporali possibili» della giornata, su ogni superficie
  (archivio 15, «La review del motore: le quattro proposte rilette»).
  **Partito il 19 nov 2026** (committente): è gli step 17.4 e 17.5 della Fase 17, come
  possibilità del giorno e non come pericolo dell'ora, con la misura per giorno e per zona che
  il 17 nov non c'era (registro, «Fase 17 — Il motore dopo la review esterna», «Il temporale
  del giorno»).
- **Il blocco `daily` ricavato dalle ore** (era lo step 17.3; registro, «Fase 17», punto 6 e
  step 17.3, e «La Fase 17 riordinata», 19 nov 2026). Sette variabili su dieci coincidono con
  l'aggregato delle ore su 630 giorni-città su 630: toglierle vale 0,7 chiamate a fetch e
  nessun calcolo migliore, e cambia la pioggia stampata del giorno in un giorno su otto.

Riletto il 17 nov 2026, all'archivio 15, dopo il merge della review del motore (PR #181): le caselle nate dal 16 nov 2026 rimandano all'archivio 15 (`docs/planning/15-novembre-16-17.md`); nessuna fase è attiva; restano aperte una prova sul telefono (l'icona di «A colpo d'occhio» più dentro), tre prove dal vivo (la nebbia dei due modelli dopo il 30 novembre, l'hero nelle ore da 0,1 mm, il temporale senza la soglia la prossima estate), il cielo d'inverno contro il satellite, che aspetta febbraio 2027, e «Velato», misurato e rimandato a una sessione sua, con il passo scritto nell'archivio 15; nessuna proposta non decisa; «Da decidere: se e quando» tiene la trasmissione di Gadgetbridge, gli avvisi ufficiali fuori dall'Italia e, dal 17 nov 2026, il temporale dedotto dalla CAPE. Riletto il 16 nov 2026, all'archivio 14, a registro chiuso: le caselle nate dal 12 nov 2026 rimandano all'archivio 14 (`docs/planning/14-novembre-12-16.md`); la Fase 16 e la Fase 14 sono chiuse e nessuna fase è attiva; restano aperte una prova sul telefono (quel che cade nell'hero del Cielo) e il cielo d'inverno contro il satellite, che aspetta febbraio 2027; nessuna proposta non decisa; «Da decidere: se e quando» tiene la trasmissione di Gadgetbridge e gli avvisi ufficiali fuori dall'Italia, che non sono lavoro finché il committente non lo dice. Riletto il 12 nov 2026, all'archivio 13, all'inizio del 16.11: le caselle nate dal 12 nov 2026 rimandano all'archivio 13 (`docs/planning/13-novembre-12.md`), la descrizione degli step della Fase 16 resta nell'archivio 11 (16.11 aperto: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.8, 16.9, e 16.10 che decide se «Punta il telefono» si tiene), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; due proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, e il fuso della macchina nelle immagini dei widget del README. Riletto il 12 nov 2026, all'archivio 12, all'inizio del 16.8: le caselle nate dal 10 nov 2026 rimandano all'archivio 12 (`docs/planning/12-novembre-10-11.md`), la descrizione degli step della Fase 16 resta nell'archivio 11 (16.8–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, le due caselle del 16.4, 16.5, 16.6, 16.7, la notte bianca), il peso della cifra e «Costellazioni» accesa da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; due proposte non decise, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo, e il fuso della macchina nelle immagini dei widget del README. Riletto il 10 nov 2026, all'archivio 11, all'inizio del 16.3: le caselle nate dal 2 nov 2026 rimandano all'archivio 11 (`docs/planning/11-novembre-2-10.md`), dove sta anche la descrizione degli step della Fase 16 (16.3–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.1, 16.2), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; una proposta non decisa, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo. Riletto il 2 nov 2026, all'archivio 10, con la 2.0.0: le caselle chiuse dal 29 ott 2026 (15.13, 15.14) rimandano all'archivio 10 (`docs/planning/10-verso-2.0.0.md`); restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, dopo la rilettura della Fase 15: aperte 15.13 e 15.14, la coda; e come prima 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, all'archivio 09: le caselle della Fase 15 sono tutte chiuse (in `docs/planning/09-ottobre-28-29.md`), restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 28 ott 2026, all'archivio 08, lo stesso giorno dell'apertura della Fase 15: le caselle sopra sono della Fase 14 (14.12 in attesa) e della Fase 15 (in `docs/planning/08-ottobre-27-28.md`), nessuna proposta non decisa. Riletto il 27 ott 2026, all'archivio 07: le caselle della Fase 14, nessuna proposta non decisa. Fino al 26 ott 2026 nessuna casella aperta (riletto il 22 ott 2026, all'archivio 06):
le ultime chiuse quel giorno stanno in `docs/planning/06-ottobre-20-22.md`, «Cielo: la finestra
reale nella card Stanotte», «Il widget «Momenti del cielo» va via», «Le notifiche: ogni riga nella
forma che sta in una riga», «Il widget «Le prossime ore» va via».

---

## Registro dal 17 nov 2026, dopo l'archivio 15

## La sonda tiene nel repo le ultime dodici (committente, 17 nov 2026)

**Da dove nasce.** Il committente, dopo l'archivio 15: la sonda gira ogni mese e scrive un file
nel repo; oggi non è un problema, ma invece di crearne all'infinito si tengano le ultime
dodici, e sia il workflow a cancellare le vecchie quando scrive la nuova. Ha chiesto un parere
onesto e se è fattibile; se sì, di farlo e di portarlo direttamente su `main`.

### Cosa ho trovato

- **È come dice.** `Probe provider` gira il 2 di ogni mese alle 05:23 UTC e a richiesta, e
  committa `tools/measurements/probe-<data>.json` sul ramo su cui gira. I file sono due, del 27
  ott e del 2 nov 2026, 281 e 285 kB.
- **La sentinella legge solo la sonda precedente** (`previous_probe`: l'ultimo file per nome
  che non è quello di oggi). Dodici sono undici più di quel che le serve.
- **Quel che si limita è l'albero, non la storia.** Una sonda pesa 285 kB nell'albero di
  lavoro, 3,4 MB l'anno, e circa 8 kB compressa nella storia di git, dove resta comunque,
  cancellata o no. Il guadagno è quindi piccolo, e sta nell'albero e nei checkout a profondità
  1 della CI: il repo clonato intero non dimagrisce. Non c'era urgenza e non c'è un costo: è la
  regola che l'app ha per il suo disco (una cosa scritta arriva con il suo limite), applicata
  al repo.
- **Un solo rimando per nome a una sonda**, nel codice: il commento di `IconD2Countries` in
  `NowcastCoverage.kt` cita `probe-2026-10-27.json`. Uscirà dall'albero con la tredicesima
  sonda (il 2 ott 2027, se prima non ne gira nessuna a richiesta) e resta nella storia. Il
  commento non cambia: la data è il rimando.

### Cosa è cambiato

- `.github/workflows/probe-provider.yml`: nello step che committa, prima del `git add`, i
  `probe-*.json` oltre gli ultimi dodici per nome (la data è nel nome) si cancellano, e la
  cancellazione sta nel commit della sonda nuova. Una riga. Lo step gira anche quando la
  sentinella è scattata, come prima, e le altre misure della cartella non si toccano.
- **Nel workflow e non nello script**: `probe_provider.py` si lancia anche a mano (`--cities`,
  `--no-sentinel`), e non deve cancellare file nella copia di chi lo prova.
- `CLAUDE.md` e la docstring dello script dicono dove sta una sonda che non è più nell'albero:
  `git log -- tools/measurements/probe-<data>.json`.
- Una sonda a richiesta conta come una mensile: sono dodici file, non dodici mesi. Due corse
  nello stesso giorno riscrivono lo stesso file.

### Come è stato verificato

Le righe dello step, con la shell che GitHub usa (`bash -eo pipefail`), in un repo di prova.
Tredici sonde committate e la quattordicesima appena scritta, con accanto file `metar-`,
`hazards-` e `states-`: restano le ultime dodici, le due più vecchie escono nello stesso
commit, il resto è intatto. Due sonde e la terza: nessuna cancellazione. La stessa sonda una
seconda volta: «Nothing new to commit». Nessuna sonda (lo script morto prima di scrivere):
nessun errore. Dodici, con quella di oggi riscritta: restano dodici. Il workflow si legge
ancora come YAML. **Non è stato lanciato**: una corsa scrive una sonda su `main`, e non è stata
chiesta.

### Cosa conferma solo il vivo

La corsa del 2 dic 2026 è la prima a passare dalle righe nuove, con tre file e niente da
cancellare: è la stessa che la casella «La nebbia dei due modelli, dal vivo dopo il 30
novembre» aspetta, e lo step gira rossa o verde che sia. La prima cancellazione vera è alla
tredicesima sonda.

## «Velato»: fatto (committente, 17 nov 2026)

**Da dove nasce.** Il committente: «Voglio implementare il punto aperto "Velature" in planning.
Partiamo dal design delle nuove icone meteo […] poi implementiamo tutto il punto una volta
decise le icone». Il passo era scritto per intero nell'archivio 15 (`docs/planning/15-novembre-16-17.md`,
««Velato»: il passo, pronto da eseguire»); questa sezione dice cosa è stato deciso e cosa è
cambiato rispetto a quel passo, che resta il riferimento per il perché.

### La tavola e il disegno

Una tavola dei candidati (un artefatto Design, «Velato, le icone»: non è nel repository, come
quelle di prima), fatta come chiedeva l'archivio 15: i candidati accanto a `clear-day`,
`sun-one-cloud`, `sun-half-cloud` e `cloudy`, alla striscia (32 dp, ritoccabile a 38 e 52) e
all'hero (80 dp), nelle quattro facce, giorno e notte, con la nebbia in fondo alla scala, il
vicino da cui non deve confondersi. Composti dai disegni spediti, rasterizzati con lo stesso
convertitore di `tools/icon_filmstrip.py`.

- **A · Velo**: il sereno intatto, due tratti orizzontali di `haze` attraverso il centro del
  disco. Proposto.
- **B · Cirri**: gli stessi tratti, inclinati. Riconoscibile, ma un segno obliquo che la famiglia
  non usa altrove.
- **C · Foschia**: `haze-day` e `haze-night` di Meteocons (la strada 1 dell'archivio): il sole
  basso dietro tratti a terra, che dice foschia o alba e accanto alla nebbia quasi non si
  distingue. Sconsigliato.
- Scartati guardandoli, prima della tavola: i tratti sulla metà bassa (un tramonto), un tratto
  obliquo solo (un sole barrato), tre tratti tratteggiati (affollati a 32 dp), una nuvola
  trasparente davanti al sole (torbida, e nella faccia a linea i contorni si incrociano).

Il committente ha preso A e ha chiesto una variante con il velo più in alto. Provate tre
altezze, poi misurate con lo strumento (sotto): **A alto**, i tratti a 47 e 58 su 128, è la
scelta («Va bene A alto»). La strada è la 2 dell'archivio, una composizione: niente pacchetto
npm, niente tratti nuovi oltre a quelli di `haze`.

### Le cinque decisioni

1. **Il disegno**: A alto (sopra).
2. **La parola inglese**: «High cloud», confermata. «Hazy sun» non regge di notte.
3. **Il posto fra i cieli**: fra «Quasi sereno» e «Nuvoloso», le severità rinumerate (sereno 0,
   quasi sereno 1, velato 2, nuvoloso 3, coperto 4; la nebbia resta a 10), confermato.
4. **La frase del giorno aperto**: «Velato» sta con il sole (`Look.SUN`), confermato. Non serve
   codice: dentro un tratto di sole la parola è la più frequente, e nel pareggio la più pesante.
5. **Le soglie**: 80% e 50%, confermate.

### Cosa è cambiato

- **Le icone**: `tools/compose_sun_cloud.py` ha la quarta composizione, `sun-veiled-*` (`VEILS`):
  il sereno intatto, due tratti di `haze` (il grigio di ogni faccia, la linea a schermo 3,66,
  la punta tonda) a 47 e 58 di giorno e a 48 e 59 di notte, sfalsati di 12 unità, un buco largo
  2,2 per lato (`VEIL_GAP`), lo scorrere delle righe di `fog-day` (3 unità in 3 s, il buco con
  loro, il sole sotto fermo). Lo strumento rifiuta una briciola di sole o di falce più piccola
  di un punto della linea (`veil_specks`), due buchi che si toccano (`veil_bands`), i raggi
  tagliati per il lungo e le punte come per le nuvole. 16 drawable nuovi; i 64 composti di prima
  sono usciti identici al byte. `ComposedIcons.kt` ha `veiledDay`/`veiledNight`.
- **L'altezza misurata, non a occhio** (`ray_report`, `veil_pieces`): a 60/71 (la prima A) del
  raggio di sinistra resta il 54%; a 50/60 di quello di destra il 61%, e fra i tratti resta una
  briciola d'anello di 1,8 unità quadre, che lo strumento rifiuterebbe; a 43/54 dei raggi in
  diagonale il 60–69%; a 47/58 i raggi orizzontali sono interi e dei due in diagonale in alto
  resta il 97%, la punta interna tolta di traverso. Il pezzo più piccolo fuori dal velo è di
  15,8 unità quadre (l'anello fra i tratti, nella linea), sopra i 10,6 di un punto.
- **Il dominio**: `WmoCode.VEILED` (1003, fra gli stati dell'app sopra 1000), `ConditionWord.VEILED`
  in fondo all'elenco (gli `id` sono su disco), le severità come alla decisione 3.
  `WeatherStateEngine.skyOf`: copertura ≥ `VEILED_COVER_PCT` (80) e luce tolta <
  `VEILED_LIGHT_PCT` (50) danno «Velato», prima dei gradini; senza strati mai.
- **Il pareggio dell'etichetta del giorno** (`WeatherReportMapper.dailyCode`, passo 3) va alla
  severità, poi al numero. Era il difetto latente che l'archivio 15 segnalava: con 1003 il velo
  avrebbe vinto ogni pareggio, anche contro la nebbia.
- **L'app**: `cond_veiled` «Velato» / «High cloud»; `WeatherText`; `ChiaroIcons.conditionLineRes`
  (giorno e notte). Niente da toccare, come previsto, in `OpenDaySentence`, `WhatChanged`, i
  widget, gli avvisi, le notifiche, il cielo disegnato, il verdetto del Cielo.
- **La misura ripetibile**: `tools/analyze_against_metar.py --veiled`, sulle ore della regola 6
  (luce, asciutte, il sole sopra 15°, niente nebbia vista né detta con la regola 5 di oggi).
  Sull'archivio di ottobre: 4 845 ore, **368 velate (7,6%)**, il sole domina nel 73%, un po' di
  sole nel 23%, niente nel 4%, indice mediano 0,86; in pianura padana 84 su 1 527 (5,5%), 80% /
  17% / 4%; le coperte che dicono «Nuvoloso» 58% / 32% / 10%, 0,77; sotto il 50% di copertura
  98% e 0,97; con la soglia al 90% 308 ore, 70%. **Cambia i numeri dell'archivio 15** (punto 5:
  373 ore, 7,7%, indice 0,85, 346 con lo strato alto): lo script provvisorio leggeva la nebbia
  detta in un altro modo; la differenza è di cinque ore e nessuna conclusione cambia. I numeri
  nel codice e in STATES sono quelli ripetibili.
- **I documenti**: `docs/STATES.md` (la tabella in testa, §2, §3 regola 6, §6 il pareggio, §8,
  §10 dove la voce della proposta diventa «Velato non ha inverno», la storia); `DESIGN.md` §13.1;
  `CLAUDE.md` (cosa compone `compose_sun_cloud.py`); `tools/shipped_icons.py` (`haze` e
  `fog-day` sorgenti anche del velo); `CHANGELOG.md`, «Added».

### I test

- `WmoCodeTest`: 1003 fra gli stati del motore, il gruppo di parole, l'`id` «veiled» in fondo, e
  la scala dei cieli 0–4 con il velato senza fase, senza pericolo, mai bagnato, sotto la nebbia.
- `WeatherStateEngineTest`: il velo intero; i bordi (80% e 49%); i veli sottili sotto; un buco,
  uno strato basso, metà della luce non sono un velo; senza strati no; la pioggia, il temporale
  possibile e la nebbia vengono prima. Il test che fissava 92% di nubi alte come «Quasi sereno»
  ora dice «Velato», e la stessa luce sotto un velo rotto resta «Quasi sereno».
- `WeatherReportMapperTest`: un giorno di velo è «Velato»; il pareggio velato/coperto va al
  coperto (con il numero avrebbe vinto il velo), velato/quasi sereno al velato.
- `ConditionWordsTest`: la parola, in due lingue, senza sole né luna. `ConditionIconsTest`: 1003
  di giorno e di notte, e diverso da ognuno dei quattro cieli. `OpenDaySentenceTest`: il velo sta
  con il sole, un pomeriggio grigio fa due tratti, il pareggio con il sereno va al velo.
- `ComposedIconsTest`: il sole del velato è quello di sereno alla lettera in ogni faccia; il velo
  è nel grigio e nella linea di `haze`, due tratti orizzontali sopra i raggi orizzontali; il
  buco è lo stesso nelle quattro facce; il velo scorre e il sole sotto resta fermo; 1003 si
  trova nelle quattro facce, fermo e animato.

### Come è stato verificato

Sull'ultimo stato del ramo, in locale: `./gradlew test :app:testDebugUnitTest :app:lintDebug`.

| | Test | Falliti |
|---|---:|---:|
| `:core:domain` | 491 | 0 |
| `:core:data` | 328 | 0 |
| `:core:sync` | 34 | 0 |
| `:app`, debug | 1 231 (16 saltati: le immagini del README, che girano a richiesta) | 0 |
| `:app`, release | 1 005 | 0 |

Lint: 0 errori, 3 737 avvisi; nessuno sulle righe che questo lavoro tocca (l'archivio 15 ne
contava 3 736: la differenza sta nei controlli di versione delle dipendenze, che dipendono dalla
rete). `python3 tools/compose_sun_cloud.py --controlla` passa.

**Guardato, non solo provato.** Le sedici facce nuove rese con `tools/icon_filmstrip.py --foglio`
a 32 e 80 px accanto al sereno, al quasi sereno, al poco nuvoloso e alla nebbia, ferme e in
movimento; a 160 px le quattro facce di giorno e di notte e un fotogramma a velo spostato: i
tratti alla linea del sole, l'aria uguale nelle quattro facce, il buco che scorre con il velo.

**Le immagini del README** rigenerate (`-PupdateScreenshots`) e guardate: cambia solo
`today-week.png`, l'icona di martedì, il giorno della Milano registrata con il 92% di nubi alte,
che passa dal quasi sereno al velato; a 38 dp si distingue dal sereno di lunedì e dal nuvoloso
di mercoledì. Le altre quindici sono uscite identiche al byte, e la didascalia e il testo
alternativo non nominano quel giorno: niente da cambiare nel README.

### Cosa conferma solo il vivo

Un giorno di cirri fitti: la pagina che dice «Velato» sotto un cielo lattiginoso con il sole che
fa ombra; la stessa parola di notte; il disegno nella striscia oraria accanto a «Quasi sereno» e
a «Nuvoloso», fermo e in movimento, su un telefono. E l'inverno: la misura di ottobre non dice
cosa fa un velo con il sole basso di dicembre, e `--veiled` si rilancia con la casella «Il cielo
d'inverno contro il satellite». Casella in «Aperto».

## Le stelle cadenti si vedono (committente, 17 nov 2026)

**Da dove nasce.** Il committente, la sera del picco delle Leonidi: nell'hero di Cielo gli
sembra di vedere ogni tanto una stella cadente, ma fa fatica: l'animazione pare velocissima e
solo nella parte molto alta della finestra. «Prova a verificare. Magari sto vedendo male io.»

### Cosa ho trovato

Vedeva bene. Lo shader (archivio 11, «Oggi e il motore del cielo: la revisione da app
premium», 9 nov 2026) metteva l'inizio della scia nell'8–43% alto del riquadro e la faceva correre via dal radiante. Una notte di picco il
radiante è basso, o sotto il piede della camera (le Leonidi sorgono verso le 23 a Milano), e
la finestra lo guarda: **ogni** scia sale, verso il bordo alto. E dal 10 nov 2026 (Fase 16.1)
il disegno del Cielo arriva fino al bordo dello schermo, sotto la barra di stato e la riga del
luogo. Sull'aritmetica dello shader, in un riquadro di 411 × 420 dp con 96 dp coperti in alto:

| Il radiante | Le scie che salgono | La testa coperta quando è più luminosa |
|---|---:|---:|
| Davanti, 5–25° di altezza | 100% | 93–99% |
| 60–100° di lato | 100% | 50–60% |

E la scia durava 0,8 s con un inviluppo a campana, cioè circa mezzo secondo visibile: sette
o otto dei quindici fotogrammi al secondo del cielo di notte, larga 1,4 dp.

### Le decisioni (committente: «procedi così; una modifica precisa e da app premium»)

- **Dove e quando li decide Kotlin** (`MeteorTrail`, puro e testato), non più lo shader: lo
  shader riceve la scia di quell'istante (coda, testa, forza, larghezza, alone) e la disegna.
- **Tutta la scia nel cielo libero**: sotto la riga del luogo e, sul Cielo, sotto «Esplora»
  (40 dp); sopra le parole o il piede dello stadio (48 + 12 dp); 20 dp dai lati e 10 dalla
  fascia, oltre l'alone. Su Oggi il cielo libero è la fascia del sole e della luna; dove
  nessuno lo dice, la stessa fascia (la finestra widget non anima). **Corre esattamente via dal
  radiante**: la scia sta sulla retta dal radiante per il suo punto medio, ed è solo accorciata
  o spostata per starci, mai girata (il test chiede meno di 0,05°).
- **Ogni scia ha le sue caratteristiche, sotto una regola sola**: lunga 90–180 dp ±30%, più
  corta vicino al radiante (vista di scorcio); 0,9–1,6 s di volo a circa 110 dp/s ±10%, le
  corte più lente (vicino al radiante una meteora vera è anche più lenta); la luminosità
  sbilanciata verso il medio, una su dieci luminosa, più larga (1,5–2,4 dp) e con un alone più
  grande. Prima variavano solo il posto e la lunghezza.
- **Si accende e si spegne senza scatti**: sale in un quinto del volo, tiene, scende al 35%
  nell'ultimo quarto, e lascia per 0,4 s una scia che si ritira nel punto in cui si è spenta;
  nessun passo di 1/240 s cambia la luce più del 5%. La coda è il 40% del percorso, più sottile
  e più debole verso il fondo.
- **Fluida**: mentre una meteora è in volo il cielo chiede 60 fotogrammi al secondo
  (`MeteorFps`), due secondi ogni quarantacinque; a quindici una testa a 110 dp/s salta 7 dp
  per fotogramma. Il resto della notte resta a quindici.
- **Una ogni 45 secondi circa, come prima**: è quello che si vede di uno sciame forte da una
  città, e il committente non ha chiesto di cambiarla. Il seme della sequenza è il radiante in
  gradi interi: letto ai decimali, cambiava a ogni nuova scena e una scia poteva saltare o
  sparire in volo.

### Cosa è cambiato

`ui/components/MeteorTrail.kt` (nuovo); `SkyShader.kt` (le uniform `uMeteorTail`,
`uMeteorHead`, `uMeteorLook` al posto di `uMeteor` e `uMeteorSeed`, il disegno, il ritmo dei
fotogrammi, `bindSky` con il cielo libero); `SkyCanvas.kt` (`open`); `TonightSky.kt` (il cielo
libero dell'hero); `DESIGN.md` §3.9 (la meteora, e i fotogrammi); `CHANGELOG.md`, la voce della
meteora in «Added», che non è ancora uscita.

### I test

- `MeteorTrailTest`: in sei posizioni del radiante e duemila periodi, ogni scia intera nel cielo
  libero e ai suoi margini; la direzione via dal radiante; sotto un radiante basso salgono tutte
  e il loro punto medio sta a metà del cielo libero; ogni scia diversa (lunghezze, durate,
  luminosità, momenti, posti); più corte e più lente vicino al radiante; per duecento meteore
  più di 0,6 s forti, nessun salto, la testa che va in un verso solo; niente fra una e l'altra,
  e nessun fotogramma in più; un cielo libero troppo piccolo non ne ha; un'altra notte, un'altra
  sequenza.
- `SkyShaderPixelTest`, la meteora: disegnata da Skia, vista nel cielo libero, niente sopra e
  sotto, nessuna in una notte che non è di picco, e più di un campione e meno della sua vita.

### Come è stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug`, sull'ultimo stato del ramo:

| | Test | Falliti |
|---|---:|---:|
| `:core:domain` | 491 | 0 |
| `:core:data` | 328 | 0 |
| `:core:sync` | 34 | 0 |
| `:app`, debug | 1 240 (16 saltati: le immagini del README, che girano a richiesta) | 0 |
| `:app`, release | 1 014 | 0 |

Lint: 0 errori, 3 736 avvisi, nessuno sulle righe che questo lavoro tocca. **Guardato**: sei
fotogrammi di quattro meteore resi dallo shader nella geometria dell'hero del Cielo (411 × 420 dp,
densità 3, il radiante basso davanti), con le linee della riga del luogo e del cielo libero
sopra: la scia nasce in basso e sale dentro la fascia, la testa con un alone piccolo, la coda che
si assottiglia, la scia residua corta e debole.

### Cosa conferma solo il vivo

La fluidità a 60 fotogrammi e la frequenza, su un telefono, una notte di picco: casella in
«Aperto».

## La nebbia a Cavenago, vista la sera stessa (committente, 17 nov 2026)

Il committente, alle 23 circa, sul posto: l'app dice «Nebbia» a Cavenago di Brianza, e la
nebbia c'è; la pagina la dà per tutta la notte. È il luogo del falso allarme da cui la regola
dei due modelli è nata la mattina stessa (archivio 15, «La nebbia: due modelli»), e la prima
volta che la regola nuova dice nebbia davanti a un testimone.

**Cosa vale e cosa no.** Una sera, un luogo, un'ora: un vero positivo, non una misura (la CSI
della regola resta lo 0,299 dei 26 aeroporti). Non ho letto i voti dei quattro testimoni di
quell'ora: Open-Meteo rifiutava l'indirizzo del container (limite giornaliero esaurito), e
non ho chiesto altro. Se la nebbia sia durata quanto la pagina diceva, lo sa solo il mattino.

**La casella** «La nebbia dei due modelli, dal vivo dopo il 30 novembre» resta aperta: chiede
la sonda di dicembre e un mattino di nebbia dopo che ICON ha perso la visibilità, e stasera la
visibilità di ICON-D2 c'è ancora (la regola non la legge, ma il giorno che conta è quello).

## La nebbia della notte, contro i METAR (committente, 18 nov 2026)

Il committente, il mattino dopo la voce sopra: a Cavenago di Brianza nebbia dopo le 23, alle 5
e alle 6, e l'app diceva «Nebbia»; a Milano nebbia alle 7, come l'app; alle 8:30 nebbia un po'
più rada, e l'app la dà fino alle 10. Mi chiede di verificare.

**Come.** I METAR di Orio al Serio (LIME, ~22 km da Cavenago), Linate (LIML) e Malpensa (LIMC)
dalle 17 del 17 alle 8:20 del 18 (archivio IEM e aviationweather.gov), e la regola del 17 nov
(`WeatherStateEngine.fogVotes`, `fogOrSky`: due voti, persistenza o codice 45/48, media delle
umidità senza estremi ≥ 93%) rifatta in Python sulle due località con `past_days=1`: i
testimoni della richiesta dell'app e best_match per umidità, vento e codice. Orari locali.

**Cavenago.** La regola: nebbia dalle 22 alle 03 (quattro voti su cinque alle 23), un buco
alle 04 (un voto: IFS soltanto), poi nebbia dalle 05 alle 10 compresa, niente alle 11. Orio:
foschia a 3000–1500 m fino all'01:20, nebbia a 50–100 m dalle 02:50 alle 05:50, foschia e poi
sereno dalle 06:20 alle 07:20, di nuovo nebbia a 100 m alle 07:50 e alle 08:20. Alle 23 Orio
non vedeva nebbia e il committente sì: a chiazze, e 22 km sono tanti; a Cavenago vale il
testimone sul posto.

**Milano.** La regola: nebbia alle 23 e a mezzanotte, niente all'01 e alle 02 (un voto), poi
nebbia dalle 03 alle 10 compresa, niente alle 11. Linate: foschia a 2–4 km tutta la notte, e
la nebbia fitta stava a Malpensa, 40 km a ovest; nebbia a 800 e 600 m alle 05:50 e alle 06:20,
banchi alle 06:50, nebbia a 800 m alle 07:20 e a 200 m alle 07:50 e alle 08:20. Il buco
dell'01–02 è giusto. Alle 08 best_match dava pioviggine (51) con 0 mm allo 0%: la regola delle
precipitazioni non scatta e l'ora resta nebbia, come deve.

**Cosa vale e cosa no.** I dati sono quelli di adesso, le corse più recenti che Open-Meteo
cuce per le ore passate, non la cache che il telefono aveva quando il committente guardava:
vicini, non gli stessi. Una notte, due luoghi: un segnale buono, non una misura (la CSI resta
lo 0,299 dei 26 aeroporti). Il buco delle 04 a Cavenago, se l'app aveva gli stessi numeri, era
un'ora di cielo dentro la nebbia; nessuno guardava a quell'ora, e una sola notte non basta a
toccare la persistenza.

**La casella** «La nebbia dei due modelli, dal vivo dopo il 30 novembre» resta aperta: stanotte
ICON-D2 aveva ancora la sua visibilità (la regola non la legge); chiede un mattino di nebbia
dopo quel giorno.

## Fase 17 — Il motore dopo la review esterna: i passi (committente, 19 nov 2026)

**Da dove nasce.** Il committente chiede una review del motore «da persona esterna al progetto»:
il motore che legge Open-Meteo, le regole, come arrivano a schermo, la scelta dei testimoni e dei
campi scaricati; e poi se un motore proposto da zero sarebbe in linea con questo. Letti per intero
`WeatherStateEngine`, `WmoCode`, `WeatherReportMapper`, `Witnesses`, `AlertEngine`, `NowcastEngine`,
`NowcastCoverage`, `CurrentEstimate`, `SnowLine`, `HeadlineEngine`, `OpenMeteoApis` e
`docs/STATES.md`; le schermate (hero, ore, tile, settimana, due ore, notifiche) lette da un agente e
ricontrollate dove ne nasce un difetto. Niente è stato preso dai documenti senza rifare il conto:
gli archivi di `tools/measurements/` riletti con script fuori dal repo, e l'API dal vivo il 19 nov.
Il committente, vista la review: «scrivi una fase con gli step per implementare tutto quello che dà
valore aggiunto da app premium»; sul temporale dalla CAPE, «possiamo tornare indietro e modificare la
decisione presa»; su KNMI e DMI, «fai quello che ritieni migliore per l'app».

**Il verdetto.** L'impianto è quello che rifarei: lo stato dai campi fisici, i numeri mai toccati,
la nebbia a due modelli, ogni soglia con la sua misura. Regge alla riprova: `--hazards` ridà le
cifre di STATES §11 e la regola 5 rifatta le sue (1 807 / 747 / 1 060, CSI 0,299); l'allineamento
delle ore (intervalli dallo slot dopo) è giusto; «la pioggia viene prima della nebbia» pure (su 111
ore in cui la pioggia nasconde una nebbia che la regola 5 direbbe, l'aeroporto era in nebbia in 13).
Dove un motore mio sarebbe diverso sono quattro punti, e sono gli step.

**I numeri di questa sezione** vengono da script di lavoro, non dal repo. Ogni step che ne usa uno
porta **prima** la sua misura in `tools/analyze_against_metar.py`, la rilancia, e parte solo se
ritrova il numero scritto qui (o scrive quello nuovo, e perché è diverso).

### Cosa ho trovato

**1. Un'ora con pioggia misurabile sotto il 20% è un cielo asciutto, e piove 4 volte su 10.** I due
archivi METAR (ottobre 2026 e inverno, 26 aeroporti, 57 058 ore con METAR e probabilità, 9 546
bagnate):

| L'ora | Come la mostra l'app | Ore | Bagnata | Pianura padana |
|---|---|---:|---:|---:|
| ≥ 0,1 mm e ≥ 20% | pioggia | 7 395 | 74% | 83% di 1 504 |
| ≥ 0,1 mm e 10–19% | **cielo, nessuna cifra** | 479 | **45%** | 50% di 111 |
| ≥ 0,1 mm e 0–9% | **cielo, nessuna cifra** | 865 | **39%** | 27% di 187 |
| niente di misurabile, ≥ 60% | «Pioggia possibile» | 1 247 | 49% | 41% di 201 |
| niente di misurabile, 20–59% | cielo e la percentuale | 3 067 | 25% | 26% di 649 |
| niente di misurabile, < 20% | cielo | 44 005 | 5% | 2% di 12 669 |

Quando l'ensemble dice sì e il modello no, l'app dice «possibile»; quando il modello dice sì e
l'ensemble no, dice cielo: lo stesso disaccordo, bagnato quasi altrettanto spesso (41% contro 49%),
in due disegni diversi. STATES §11 lo aveva già nei suoi numeri: il pavimento del 20% porta la CSI
da 0,50 a 0,48, toglie falsi allarmi al prezzo di «diceva nuvoloso e pioveva». Oggi il 27,9% delle
ore di pioggia vista cade sotto una cella che non mostra nulla, né stato né percentuale (pianura
padana 19,3%).

**2. «Rovesci» dipende dal modello, non dal tempo.** ICON-D2 risolve la convezione e il suo
`showers` è quasi vuoto; ICON-EU la parametrizza. Dal vivo, ultimi 92 giorni, stesso punto:
`showers` sul totale 1,5% contro 25% a Milano, 1,1% contro 20% a Verona, 2,1% contro 11% a Monaco;
le 7 ore da 2,5 mm/h in su di ICON-D2 a Milano tutte «non convettive» per la regola 3. Nel nord
Italia lo stesso evento è «Pioggia» nei giorni 1–2 e «Rovesci» dal terzo. Contro i METAR (SH o TS
nell'ora, sulle ore dette bagnate e viste bagnate, Europa: le stazioni americane non scrivono SH):

| Estate 2026 | Dette | Giuste | Prese | CSI |
|---|---:|---:|---:|---:|
| Oggi (`showers` ≥ metà), Europa, 1 807 ore, SH/TS in 662 | 145 | 46% | 10% | 0,09 |
| CAPE ≥ 100 J/kg | 499 | 62% | 47% | 0,37 |
| CAPE ≥ 100 o un episodio di 1–2 ore | 655 | 57% | 57% | 0,40 |
| Oggi, aeroporti di ICON-D2, 514 ore, SH/TS in 273 | 50 | 64% | 12% | 0,11 |
| CAPE ≥ 100 | 199 | 66% | 48% | 0,39 |
| CAPE ≥ 100 o un episodio di 1–2 ore | 260 | 65% | 62% | 0,46 |

D'inverno e in ottobre (4 853 ore, SH/TS nel 10%) nessuna regola vale molto: oggi 0,13, la forma
sola 0,09–0,12, e negli aeroporti di ICON-D2 la parola esce in 19 ore su 1 610. La CAPE d'inverno
non è negli archivi: è la misura che lo step 17.6 fa prima del codice. In questa tabella le ore con
il codice di temporale non sono tolte, e nell'app sono della regola 1: lo step le toglie.

**3. Il temporale del giorno.** Per ora il codice del provider è quel che STATES §11 dice (e la
misura si riproduce). Per **giorno** e per zona, estate e ottobre 2026, 39 aeroporti, 4 368 giorni,
temporale visto in 787, non era mai stato contato: il codice da solo, e il codice più un secondo
livello nei giorni in cui tace, «un'ora con CAPE ≥ 500 J/kg, almeno 0,1 mm e ≥ 20%»:

| Zona (giorni con temporale) | Il codice: detti, giusti, presi, CSI | Il secondo livello: giorni in più, giusti | Insieme: giusti, presi, CSI |
|---|---|---|---|
| Pianura padana (157) | 109, 78%, 54%, 0,47 | 30, 60% | 74%, 66%, **0,53** |
| Resto d'Italia (58) | 29, 52%, 26%, 0,21 | 39, 41% | 46%, 53%, **0,33** |
| Resto d'Europa (125) | 57, 65%, 30%, 0,26 | 52, 56% | 61%, 53%, **0,39** |
| Fuori dall'Europa (447) | 195, 57%, 25%, 0,21 | 390, 53% | 54%, 71%, **0,45** |
| Tutti (787) | 390, 64%, 32%, 0,27 | 511, 53% | 57%, 66%, **0,44** |

Con CAPE ≥ 300 la CSI è la stessa (0,44) e in Europa il livello è giusto meno spesso (43% contro
56%); con 1 000 scende a 0,40: 500 sta sul pianoro. Il livello aggiunto è giusto un giorno su due:
è una possibilità, e la parola lo dice. Nell'ora che nomina resta giusto una volta su dieci
(STATES §10): per questo non è mai un'ora.

**4. La nebbia dal quarto giorno è misurata su dati che l'app non riceve.** La visibilità di IFS e
UKMO oltre i primi giorni arriva a passi di 3 e poi 6 ore, interpolata (dal vivo a Milano: nei
giorni 5–6 30 punti su 46 sono la media dei vicini, nei primi due 10 e 2), e su una serie
interpolata la persistenza è vera per costruzione. La riga «giorni 4–7» di STATES §11 (0,267) toglie
KNMI e DMI dalle ore del primo giorno, a passo d'ora. Rifatta campionando i due testimoni ogni 3 e
ogni 6 ore: CSI 0,249 e 0,225 (pianura padana 0,227 → 0,211 → 0,188), le nebbie prese dal 46% al
39% al 32%, e quel che è detto giusto come prima (39–44%). Resta fuori l'errore dell'anticipo, che
nessun archivio qui ha.

**5. Due testi.** `TodayDetails.kt:341` passa a `estimate_details` la frase intera di
`freshnessAge`: «gli ultimi dati sono di Aggiornato 2 ore fa.» (e in inglese). `AlertNotifier.kt:235`
stampa «pioggia N%» in ogni «Maltempo» che non sia vento: dal 17 nov il temporale non ha più la
soglia, ed esce «Temporale verso le 16:00, pioggia 8%»; e sopra una neve forte dice «pioggia»,
l'errore tolto dall'ombrello quello stesso giorno (STATES §7).

**6. Il blocco `daily`.** Sette variabili su dieci sono l'aggregato delle ore della stessa risposta:
su 630 giorni-città (le 45 città della sonda, una settimana indietro e una avanti, 19 nov 2026)
`precipitation_probability_max`, `uv_index_max`, `precipitation_sum`, `snowfall_sum`,
`wind_gusts_10m_max`, `rain_sum`, `showers_sum` coincidono 630 su 630. Massima e minima no (476 e
338 su 630, fino a 2,4 °C): sono gli estremi propri del modello, e restano.

### Misurato e lasciato com'è

- **KNMI e DMI restano due voti.** Sono lo stesso modello (HARMONIE-AROME; a Milano la copertura è
  identica in 50 ore su 70, la visibilità in 14, e nelle altre uno dà 100 m e l'altro 50 km): la
  review proponeva un voto per famiglia. La misura dice di no, sulle 45 605 ore di §11:

  | Regola 5 con | Dette | Giuste | False (aria limpida) | CSI | Pianura padana | Altrove |
  |---|---:|---:|---:|---:|---:|---:|
  | **Oggi: quattro testimoni, un voto l'uno** | 1 807 | 747 | 1 060 (579) | **0,299** | **0,254** | 0,336 |
  | HARMONIE un voto, basta uno dei due | 1 720 | 727 | 993 (545) | 0,299 | 0,250 | 0,338 |
  | HARMONIE un voto, solo se concordano | 1 575 | 677 | 898 (501) | 0,289 | 0,238 | 0,330 |
  | Solo KNMI | 1 870 | 733 | 1 137 (658) | 0,284 | 0,234 | 0,323 |
  | Solo DMI | 1 866 | 748 | 1 118 (639) | 0,292 | 0,253 | 0,323 |

  Un voto per famiglia è lo stesso punteggio, e in pianura padana un filo sotto; tenerne uno solo è
  peggio ovunque. Le due corse partono a ore diverse e portano un'informazione che una sola non ha.
- **Nessun testimone nuovo.** Dal vivo a Milano la visibilità la portano solo ICON-D2 e ICON-EU
  (fino al 30 nov), IFS 9 km, UKMO, KNMI, DMI, GFS e CMA GRAPES; non AROME, ARPEGE, ICON-2I, ICON-CH,
  ICON globale, GEM, JMA, IFS 0,25°. GFS è fuori con la sua misura (§11); CMA nella notte di nebbia
  fra il 18 e il 19 nov dava 30 km fissi con Linate a 200–600 m, come GFS a 24 km. Il 2 km del Met
  Office risponde a Milano e a Torino e non a Venezia (HTTP 400): non è un testimone di una regola
  che deve valere per tutti. I quattro sono la scelta giusta, per esaurimento.
- **Il «Maltempo» a soglia oraria non perde gli accumuli.** Su 70 giorni con almeno 30 mm (i tre
  archivi) ne restano muti 5, uno su 30 da 50 mm in su (Bologna, 24 dic 2025: 50,2 mm, l'ora più
  intensa 5,9), e in Italia quel giorno è del bollettino della Protezione Civile. Non toccato.
- **«Ora» è il modello e non lo dice**, se non quando è una stima. Come ogni app del settore; la
  nota a piè di pagina dice la fonte e l'ora. Non toccato.

### Decisioni prese oggi

1. **Il temporale dalla CAPE si fa**, e cambia la decisione del 17 nov 2026 (archivio 15, «La review
   del motore, punto 4», «Trovato, e non toccato», e «La review del motore: le quattro proposte
   rilette»). Le tre ragioni di allora, una per una: *«in Italia la stessa regola fa peggio del
   codice»* era contata per ora e al posto del codice; per giorno e sotto il codice aggiunge anche in
   pianura padana (0,47 → 0,53). *«Un pericolo che l'app inventerebbe»*: non è un pericolo, non
   entra in `SevereBucket`, non suona, non è l'icona di nessuna ora; è una possibilità del giorno,
   detta come tale, giusta un giorno su due come «Pioggia possibile» lo è un'ora su due. *«Costa
   0,1 chiamate ovunque»*: lo step 17.3 ne toglie 0,7 prima.
2. **«Possibile» vale nei due versi.** Una quantità misurabile che l'ensemble non sostiene è una
   possibilità, non un cielo. La regola 3 non «toglie una distorsione»: la dice per quel che è.
3. **Il carattere convettivo si legge dall'aria (CAPE) e dalla forma dell'episodio**, che non
   cambiano con il modello che serve l'ora; `showers` resta alla neve finché non è misurata.
4. **La pioggia del giorno è la somma del liquido delle ore** (totale − neve / 0,7), la stessa
   delle regole del lettore (15.10) e del motore. Oggi è `rain_sum + showers_sum` con il tetto, che
   su 75 giorni dei 630 sta sotto il totale del modello senza che ci sia neve (Trieste, 12 nov
   2026: 30,9 contro 31,7 mm): un numero del modello al posto di un altro, il più coerente dei due.

### Le regole comuni

- Uno step per chat, da `origin/main` aggiornato, su un ramo `fase-17-step-N-<nome>`; alla fine la
  sezione dello step in coda a questo registro, la casella chiusa in «Aperto», la PR; il committente
  la unisce.
- **Prima la misura, nel repo**: la riga nuova in `tools/analyze_against_metar.py`, il numero di
  questa sezione ritrovato, poi il codice. **Il nord Italia per primo**, poi l'Europa e il mondo:
  una regola entra se tiene in tutte le fette, e lo step scrive le fette.
- Una regola che cambia, cambia in `docs/STATES.md`, nel codice e nel suo test nello stesso step.
- Ogni variabile aggiunta o tolta scrive il nuovo peso accanto alla lista, in `CLAUDE.md` e nella
  sonda (`tools/probe_provider.py`).
- Nessuna compatibilità con le installazioni vecchie da tenere: è una beta; una voce di cache
  scaricata con la richiesta vecchia non è un colpo fresco, come sempre (`CURRENT_REQUEST`).
- Gli screenshot del README si rigenerano nello step che cambia quel che mostrano, e si guardano.
- La guida e le stringhe (IT/EN) nello step che cambia quel che dicono, non alla fine.

### Gli step

**17.1 — Due testi che si contraddicono.** `estimate_details` prende l'età senza «Aggiornato»
(una risorsa sua: «gli ultimi dati sono di 2 ore fa»), IT e EN. Il corpo del «Maltempo»: la
probabilità si stampa solo per `SevereBucket.RAIN`, l'unico pericolo che ha quella soglia; temporale,
ghiaccio e neve dicono il pericolo e l'ora, e le righe lunghe (`AlertDetails`) non dicono «pioggia»
sopra una neve. Test sulle stringhe rese. *Fatto quando*: nessuna notifica «Maltempo» stampa una
percentuale di pioggia sotto un temporale o la parola «pioggia» sopra una neve.

**17.2 — «Possibile» anche dove il modello piove e l'ensemble no.** Regola 3: una quantità
misurabile sotto `PROBABLE_PCT` non cade più al cielo ma diventa `RAIN_LIKELY` o `SNOW_LIKELY`, con
la fase dell'ora stessa (`falling`); la neve forte resta il pericolo che è. Non cambia nient'altro:
«possibile» non è `isPrecipitation`, quindi non etichetta il giorno, non fa l'ombrello, non entra
nelle ore di «X mm in N ore», non è «piove ora» (il blocco `current` e i quarti restano
`likely = false`: descrivono quel che cade). La cella resta senza cifra sotto il 20%; la
descrizione parlata la dice. I disegni ci sono già (14.13). *Da decidere nello step, con la
misura*: se da 0% o da 10%. Da 0% la classe «possibile» passa da 1 247 a 2 591 ore, bagnate 45%
(pianura padana 38%); da 10% a 1 726, bagnate 48% (44%). Propongo da 0%, una frase sola («una
quantità misurabile non è mai un cielo asciutto»), **se** la classe resta bagnata almeno una volta
su tre in ogni fetta, estate compresa (l'archivio dei pericoli ha quantità, probabilità e METAR: va
contata lì prima del codice); altrimenti 10%. *Fatto quando*: STATES §3 e §11 con la tabella
sopra rifatta dallo strumento, `WeatherStateEngineTest`, e il README se un'immagine cambia.

**17.3 — Il blocco `daily` ricavato dalle ore.** Escono da `DAILY_VARIABLES` le sette variabili
del punto 6; il mapper le conta dalle ore del giorno del provider, come già `precipHours`, e la
pioggia del giorno diventa la somma del liquido delle ore (decisione 4). Restano
`temperature_2m_max`, `temperature_2m_min` e `weather_code` (la strada di STATES §6 per un giorno
senza ore). 52 variabili → 45: la previsione **4,5 chiamate**, il fetch **7,1**. Prima del codice la
misura rifatta nello strumento su almeno i 616 giorni di `precipitation_hours`, e i giorni in cui
la pioggia stampata cambia contati e scritti. *Fatto quando*: `WeatherReportMapperTest` sulle
risposte registrate dà gli stessi cinque numeri di oggi (probabilità, UV, totale, neve, raffica) e
la pioggia del giorno con la sua differenza scritta, la sonda stampa 7,1, `CLAUDE.md` pure.

**17.4 — La CAPE nel report e il temporale possibile del giorno.** `cape` nell'orario (**+0,1**:
previsione 4,6, fetch **7,2**), sulla riga come istante. Sul giorno un campo nuovo, non uno stato:
la parte del giorno (mattina, pomeriggio, sera, notte) in cui un'ora ha CAPE ≥ 500 J/kg (il più
alto dei due estremi dell'ora), almeno 0,1 mm e probabilità ≥ 20% o ignota, **solo se nessuna ora
del giorno ha il codice di temporale**: dove il codice c'è, parla il codice. Non tocca
`severeBucket`, l'etichetta del giorno, le regole del lettore. La tabella per giorno e per zona
entra in `--hazards`. *Fatto quando*: lo strumento ridà la tabella del punto 3, il campo è nullo su
una risposta senza CAPE (una voce di cache vecchia), i test del mapper lo coprono.

**17.5 — Il temporale possibile del giorno a schermo.** Dove: una riga nei fatti del giorno aperto
(con la sua seconda riga, «tieni d'occhio il cielo» o quel che il committente sceglie), la frase del
giorno aperto, un gradino della frase di Oggi dopo l'ombrello e prima del caldo, la riga del
mattino e della sera. Mai: l'icona o la parola di un'ora, l'icona del giorno, una notifica sua, il
«Maltempo». La parola non può essere quella dello stato 1095 («Temporali possibili», che è un
pericolo dell'ora con il codice): propongo «Possibile qualche temporale nel pomeriggio», da
confermare su una tavola prima del codice. Guida IT/EN, una riga. *Fatto quando*: i test della
frase e delle sintesi, le immagini del README che cambiano rigenerate e guardate.

**17.6 — «Rovesci» dalla CAPE e dalla forma.** Prima la misura che manca: la CAPE scaricata per
l'inverno e per ottobre sugli archivi METAR (strumento, sul runner), e la tabella del punto 2
rifatta per stagione e per fetta, le ore con il codice di temporale tolte (sono della regola 1).
Poi la regola 3: una pioggia è un rovescio quando l'ora ha CAPE ≥ 100 J/kg o il suo episodio dura
una o due ore, a ogni intensità come oggi; `showers` non si legge più per la pioggia, resta per i
rovesci di neve (mai misurati: una riga in STATES §10). *Entra se* raddoppia almeno la CSI di oggi
d'estate negli aeroporti di ICON-D2 (da 0,11; la misura di oggi dice 0,39–0,46) senza scendere
sotto quella di oggi in nessuna fetta d'inverno. *Se non entra*, la proposta al committente è
togliere la distinzione per la pioggia (debole, pioggia, forte) piuttosto che tenere una parola che
cambia con il giorno di anticipo. Dipende dal 17.4.

**17.7 — La nebbia dal quarto giorno: la misura all'anticipo vero.** Uno step di misura, come 14.4
e 14.10, che può dire «niente da fare». Si guarda se la Previous Runs API porta la visibilità di IFS
e di UKMO per giorno di anticipo (per la famiglia ICON non la portava, STATES §11); se sì, la regola
5 sui 26 aeroporti ai giorni 4–7 veri. **La soglia è scritta ora**: se quel che la regola dice ai
giorni 4–7 è giusto meno della metà di quanto lo è al primo giorno (41%), la nebbia dal quarto
giorno non è più lo stato di un'ora (né icona, né frase) e resta fuori finché una misura non la
riporta; altrimenti resta com'è, con il numero vero in STATES al posto dell'emulazione. Se l'API non
la porta, in STATES §11 va la riga del punto 4 (0,249 e 0,225, un tetto) e lo step si chiude.

**17.8 — La chiusura.** La fase riletta step per step contro questa sezione; STATES riletto
intero (le regole 3 e 4, §7, §10, §11, §12 con «Misurato e lasciato com'è»); `CLAUDE.md` dove dice
7,8 chiamate e dove descrive lo stato di un'ora; il CHANGELOG; le prove dal vivo che restano, come
caselle (il temporale possibile e i rovesci chiedono un'estate, come «Il temporale senza la soglia»).

### L'ordine

17.1 e 17.2 sono quel che oggi mente e non dipendono da nulla. 17.3 paga in chiamate il 17.4, e va
prima. 17.4 → 17.5 e 17.4 → 17.6. 17.7 è indipendente e può andare in qualunque momento, anche dopo
il 30 novembre insieme alla casella della nebbia dal vivo. 17.8 per ultimo.

### Cosa ci si aspetta, in numeri

Sugli archivi di oggi (il primo giorno di ogni corsa, aeroporti; estate e ottobre per il temporale e
i rovesci, ottobre e inverno per la pioggia): sono misure di partenza, non promesse, e ogni step
scrive la sua.

| | Oggi | Dopo |
|---|---:|---:|
| Ore di pioggia vista sotto una cella che non mostra nulla (17.2, da 0%) | 27,9% | 22,1% |
| …in pianura padana | 19,3% | 13,5% |
| Ore «possibile» che poi sono bagnate | 49% | 45% |
| Giorni di temporale detti in anticipo, tutti gli aeroporti (17.4) | 32% | 66% |
| …in pianura padana / resto d'Italia / fuori dall'Europa | 54 / 26 / 25% | 66 / 53 / 71% |
| Giorni detti che hanno il temporale | 64% | 57% |
| CSI del temporale per giorno | 0,27 | 0,44 |
| Rovesci presi d'estate, Europa (17.6) | 10% | 47–57% |
| …e «Rovesci» giusto quando è detto | 46% | 57–62% |
| Chiamate a fetch (17.3 e 17.4) | 7,8 | 7,2 |

La nebbia non si muove: la regola del 17 nov resta, e il 17.7 misura, non migliora.

## 17.1 — Due testi che si contraddicono (committente, 19 nov 2026)

Il committente, letta la fase: «se la fase puoi implementarla ora in questa sessione senza
controindicazioni, procedi». **Deviazione dalle regole comuni**, detta a lui prima: gli step si
fanno nella stessa sessione, un commit per step su un solo ramo `fase-17`, senza push; 17.6 e 17.7
si fermano alla misura se la misura non c'è o dice no.

### Cosa è cambiato

- `estimate_details` (IT/EN): «Valori stimati dalla previsione per quest'ora. Aggiornato 2 ore fa.»
  La frase prendeva già l'età intera di `freshnessAge` e la metteva dopo «gli ultimi dati sono di».
  Due frasi invece di una risorsa nuova per l'età: nessun plurale in più da tenere in due lingue.
- `AlertNotifier`: nel corpo del «Maltempo» la probabilità si stampa solo per `SevereBucket.RAIN`.
  La riga lunga «Pioggia fino a N%, il peggio verso le…» (`forecastDetails`, parametro
  `rainChance`) si scrive solo dove quel che l'avviso annuncia è pioggia: il «Maltempo» di pioggia
  forte, e l'ombrello quando non è neve né pioggia e neve.

### Decisioni e deviazioni

- La riga del vento (`windDetails`) tiene la sua «Pioggia fino a»: lì la pioggia è un fatto in più
  dentro una burrasca, e la soglia è già il 50%.
- Il grafico del «Maltempo» resta la probabilità delle ore: è la finestra, non una frase.

### Come è stato verificato

`AlertNotifierTest`, due test nuovi (il temporale all'8% senza cifra, la neve forte senza «rain»,
il gelicidio senza cifra, la pioggia forte con «rain at 70%»; l'ombrello di neve senza «Rain up
to»): `./gradlew :app:testDebugUnitTest --tests "*AlertNotifierTest"`, verde in locale.

## 17.2 — La misura d'estate, prima del codice: la regola com'era scritta non tiene (19 nov 2026)

Lo step chiedeva di contare la classe «quantità misurabile sotto il 20%» sull'archivio dei
pericoli prima di scrivere codice, con una soglia: bagnata almeno una volta su tre in ogni fetta,
estate compresa. Contata (script di lavoro, estate 2026, 39 aeroporti, le ore con il codice di
temporale tolte): **non la passa**, e la casella resta aperta in attesa del committente.

| Estate 2026 | Ore | Bagnata | «Possibile» di oggi (≥ 60%, niente di misurabile) | Cielo con la percentuale |
|---|---:|---:|---:|---:|
| Tutti | 1 560 | 18% | 30% di 2 508 | 13% |
| Pianura padana | 189 | 28% | 16% di 245 | 7% |
| Resto d'Italia | 67 | 10% | 0% di 4 | 13% |
| Resto d'Europa | 502 | 32% | 33% di 1 123 | 18% |
| Fuori dall'Europa | 802 | 7% | 30% di 1 136 | 12% |

In ottobre e d'inverno la classe è bagnata nel 41% delle ore (registro, «Fase 17», punto 1);
d'estate nel 18%, e fuori dall'Europa nel 7%: lì una quantità sotto il 20% è la cella di un
temporale che il modello mette sul punto e che non ci passa. I numeri «27,9% → 22,1%» di «Cosa ci
si aspetta» valgono per ottobre e l'inverno soltanto.

**Quel che la separa è l'aria**, la CAPE dell'ora (il più alto dei due estremi), estate e ottobre:

| Quantità misurabile sotto il 20% | CAPE < 100 J/kg | 100–499 | ≥ 500 |
|---|---:|---:|---:|
| Ottobre, tutti | 49% di 211 | 30% di 37 | 12% di 25 |
| Estate, pianura padana | 42% di 52 | 33% di 42 | 18% di 95 |
| Estate, resto d'Europa | 32% di 423 | 30% di 61 | 33% di 18 |
| Estate, fuori dall'Europa | 10% di 205 | 9% di 201 | 5% di 396 |
| Estate, tutti | 26% di 686 | 17% di 306 | 8% di 568 |

In aria non convettiva la classe è una possibilità vera in Europa in ogni stagione (32–51%); fuori
dall'Europa d'estate no nemmeno così (10%), su 205 ore di 13 aeroporti in tre mesi.

**Proposta al committente, non decisa**: «possibile» per una quantità misurabile sotto il 20% solo
dove la CAPE dell'ora è sotto 100 J/kg, e quindi dopo il 17.4, che porta la variabile; oppure non
farlo. Da 0% ovunque, com'era scritto, no.

Trovato contando, e non toccato: d'estate anche la «Pioggia possibile» di oggi è bagnata nel 30%
delle ore (16% in pianura padana), e un'ora detta di pioggia nel 44%.

## La Fase 17 riordinata: solo quel che migliora il calcolo (committente, 19 nov 2026)

Il committente, letta la misura d'estate del 17.2: «ok con le tue proposte», «mettiamo solo quello
che pensi onestamente che crea valore e un miglior calcolo meteo», e la fase continua in una
sessione nuova su questo ramo (`fase-17`, un commit per step, senza push finché non lo chiede).
Questa sezione **cambia l'ordine e due step** di «Fase 17 — Il motore dopo la review esterna»; dove
le due non concordano vale questa.

**L'ordine nuovo**: 17.4 → 17.5 → 17.2 → 17.6 → 17.7 → 17.8. Il 17.1 è chiuso. Il 17.3 esce dalla
fase.

- **17.4 e 17.5 per primi**, com'erano scritti. È il guadagno più grande e il più sicuro della
  fase (i giorni di temporale detti in anticipo dal 32% al 66%, misurati su estate e ottobre), e
  porta la CAPE da cui dipendono il 17.2 e il 17.6. Senza il 17.3 prima, il peso è 53 variabili:
  la previsione **5,3 chiamate**, il fetch **7,9** (da 7,8); va scritto accanto alla lista, in
  `CLAUDE.md` e nella sonda. Le due parole sono decise: «Possibile qualche temporale nel
  pomeriggio» per il livello del giorno, e «Temporali possibili» resta dello stato 1095.
- **17.2 si fa, cambiato**: una quantità misurabile sotto il 20% diventa «Pioggia possibile» o
  «Neve possibile» **solo dove la CAPE dell'ora (il più alto dei due estremi) è sotto 100 J/kg**;
  in aria convettiva resta il cielo, com'è oggi. Senza CAPE nella risposta (una voce di cache
  vecchia) la regola è spenta. Prima del codice la misura nello strumento, con le tabelle di
  «17.2 — La misura d'estate» ritrovate; *entra se* la classe è bagnata almeno una volta su tre in
  ogni fetta d'Europa, pianura padana per prima, in ogni stagione che gli archivi hanno. **Il
  limite noto, da scrivere in STATES §10**: fuori dall'Europa d'estate resta sbagliata nove volte
  su dieci (10% di 205 ore in tre mesi su 13 aeroporti); e la CAPE d'inverno non è negli archivi
  (la scarica il 17.6: se il 17.2 viene prima, l'inverno è contato senza il filtro, 41%, e
  ricontato lì). Il resto dello step è quello scritto: non è `isPrecipitation`, non etichetta il
  giorno, non fa l'ombrello, il blocco `current` e i quarti restano fuori.
- **17.6** (i rovesci) com'era scritto.
- **17.7** (la nebbia dal quarto giorno) resta uno step di sola misura, con la sua soglia già
  scritta: non migliora nulla da solo, dice se una cosa che l'app mostra è vera.
- **17.3 esce dalla fase** e passa a «Da decidere: se e quando». Toglie 0,7 chiamate a fetch e non
  migliora nessun calcolo; cambia la pioggia stampata del giorno in un giorno su otto e tocca una
  ventina di punti nei test del mapper. Con 7,9 chiamate a fetch l'app è lontana da ogni limite
  (10 000 al giorno per indirizzo): è igiene dei costi, da fare se e quando le chiamate contano.
- **17.8** (la chiusura) per ultimo, senza la riga sulle 7,2 chiamate.

**Cosa ci si aspetta, riletto.** Restano le righe del temporale e dei rovesci della tabella di
«Fase 17». Quella della pioggia taciuta («27,9% → 22,1%») valeva per ottobre e l'inverno e per la
regola da 0%: con il filtro della CAPE il guadagno è lo stesso nella stagione fredda, dove l'aria
non è convettiva, e minore d'estate; il numero vero lo scrive lo step. Le chiamate: 7,8 → 7,9.
