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
- **Nessuna fase attiva** dal 19 nov 2026, a Fase 17 chiusa, e prima dal 16 al 19 nov 2026: quel che resta in «Aperto» sono una prova sul telefono, una misura che aspetta l'inverno e, dal 17 nov 2026, la nebbia dei due modelli da vedere dal vivo dopo il 30 novembre. Dal 17 nov 2026 anche le caselle della review del motore: due prove dal vivo e «Velato», fatto lo stesso giorno (archivio 16, ««Velato»: fatto») con la sua prova dal vivo; il temporale dedotto fuori dalla famiglia ICON, che era fra le cose «Da decidere: se e quando», è stato fatto dalla Fase 17. Il registro dal 17 al 19 nov 2026 è in `docs/planning/16-novembre-17-19.md` (la sonda, «Velato», le stelle cadenti, la nebbia vista dal vivo, la Fase 17 intera), quello dal 16 al 17 nov 2026 in `docs/planning/15-novembre-16-17.md` (la nebbia dei due modelli, la review del motore, il passo di «Velato»), quello fino al 16 nov 2026 in `docs/planning/14-novembre-12-16.md`.
- **Aperta il 21 nov 2026**: **Fase 18**, il modello verso i motori professionali (committente, sul ramo `ccr-3f909142-whm9fe`, partito da `main`): la correzione statistica e la fusione dei modelli, misurate, al posto del modello di oggi su questo ramo, da confrontare con `main` sul telefono. Il via del committente il 21 nov 2026, con le regole nuove scritte (18.0); il registro in «Fase 18 — Il modello verso i motori professionali».
- **Da decidere: se e quando** (dal 16 nov 2026). Non è una fase attiva e non è lavoro da fare: sono le funzioni che il committente non ha ancora deciso se fare, tutte o alcune, né quando. Stanno in fondo ad «Aperto», sotto il loro titolo; nessuna si comincia senza il suo via, e una che parte torna fra le caselle con la sua fase o il suo step.

## Aperto

Quel che resta da fare o da decidere, in un posto solo: una casella si apre qui, con il rimando
alla sezione del registro da cui nasce, e si chiude qui, con quella che la chiude.

- [x] **Fase 18: il testo nuovo delle regole** (registro, «Fase 18», «Le regole: il testo proposto»): il committente lo approva, lo cambia o lo respinge, prima di ogni riga di codice. Approvato il 21 nov 2026 e
  scritto (registro, «18.0 — Le regole»).
- [x] **Fase 18: le cinque decisioni** (registro, «Fase 18», «Le decisioni del committente»): le due APK affiancate, le osservazioni alpine per gli strumenti, l'ordine di ICON-CH, il ramo `prova-nuovo-modello`, il bilancio delle chiamate; dal 21 nov 2026 anche D6, il nowcast dal radar (registro, «Le domande del committente»).
  Confermate tutte e sei dal committente il 21 nov 2026 (registro, «18.0 — Le regole»); D1 cambiata lo stesso
  giorno: una sola APK con lo switch (registro, «Una sola APK con lo switch»). D2 e D6
  chiedono una verifica prima del lavoro, nelle due caselle sotto.
- [ ] **Fase 18, D2: le osservazioni alpine per gli strumenti** — licenza, archivio e accesso senza chiave
  dei dati delle stazioni di MeteoSvizzera e delle ARPA alpine (registro, «Fase 18», «Le decisioni del committente»).
- [ ] **Fase 18, D6: le immagini passate di Radar-DPC** — senza almeno due immagini non c'è moto, e 18.9 non
  esiste (registro, «Le domande del committente»).
- [x] **18.1 — La misura di partenza** (registro, «Fase 18», «Il piano: gli step»). Fatto il 21 nov 2026: la
  fusione passa ai giorni 6–7 in Italia, la correzione per zona no (registro, «18.1 — La misura di partenza: fatto»).
- [x] **18.4 — La fusione della temperatura ai giorni 6–7 in Italia** (registro, «18.1 — La misura di partenza: fatto»). Fatto il
  22 nov 2026 (registro, «18.4 — … fatto»).
- [ ] **18.5 — La probabilità della pioggia, calibrata** (registro, «Fase 18», «Il piano: gli step»).

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

## Fase 18 — Il modello verso i motori professionali (committente, 21 nov 2026)

**La richiesta.** Dopo il confronto fra `main` e il ramo `prova-nuovo-modello` (fatto in chat lo
stesso giorno, non nel repo), il committente ha chiesto di scegliere un modello e portarlo al
livello dei motori professionali, rivedendo i vincoli che lo impediscono se sono sbagliati. Si
lavora su questo ramo (`ccr-3f909142-whm9fe`, partito da `main` a `56d8693`): **qui il modello
nuovo prende il posto di quello di oggi**, e il committente lo confronta con `main` installando
le due APK. Questa sezione è la proposta: nessuna regola e nessuna riga di codice cambiano
prima del suo via («Aperto»).

**Il modello scelto: quello di `main`.** Ha già quel che i servizi professionali hanno come
metodo: la verifica contro le osservazioni (26 aeroporti, satellite, due periodi), i criteri
scritti prima dei numeri, il quarto d'ora calcolato, più modelli letti insieme sugli stati, la
sonda che si accorge quando il fornitore cambia. Il ramo `prova-nuovo-modello` ha gli
ingredienti giusti (ICON-CH più ECMWF è la base di MeteoSvizzera), ma senza la correzione che i
professionisti mettono sopra, senza misura, e con ICON-2I che il 14.4 ha già misurato peggiore
(archivio 08, «14.4»: al nord, giorno 1, 1,21 °C di errore medio contro 0,80; al sud nessuna
cella vinta). Da quel ramo si prendono pezzi, ciascuno dopo la sua misura: ICON-CH, i codici di
ICON-D2 dove `best_match` non è ICON-D2, `ForecastComposer` come forma della cucitura. Il ramo
resta com'è (decisione D4).

### Che cosa fanno i motori professionali

Verificato il 21 nov 2026 sulle pagine e gli abstract dei gestori:

- **MeteoSvizzera**: le previsioni locali dell'app (6 000 punti, ogni 10 minuti) sono una
  correzione statistica che fonde più modelli in un consenso, EMOS e machine learning; ora su
  ICON-CH1/CH2 con ECMWF. Contro il modello locale grezzo, fino al 30% di CRPS in meno; le valli
  guadagnano di più dalla fusione (meteoswiss.admin.ch, «Generation of high quality localised
  forecasts» e «Statistical postprocessing»; EMS2021-288; ECMWF, «Application and verification of
  ECMWF products 2024 — Switzerland»).
- **DWD, MOSMIX**: ICON e IFS corretti ciascuno con la sua regressione (MOS) e poi fusi, fino a
  14 giorni, sui punti delle stazioni (EMS2017-378; EMS2026-326, che annuncia il successore).
- **Met Office, IMPROVER**: nowcast, UKV, MOGREPS-UK e MOGREPS-G, ed ECMWF, fusi con pesi che
  cambiano con l'anticipo, in probabilità (EMS2025-60; i dati «UK Spot Probabilities»).

Tre cose comuni: **nessuno mostra un modello grezzo**; **il numero corretto o fuso sbaglia meno
di ogni modello da solo**, ed è per questo che lo mostrano; **la pioggia si fonde in probabilità
e quantili, mai come media delle quantità**.

### Perché c'era il vincolo

«Mai la media di due modelli» non viene da tweather. È nato il 22–27 ott 2026, nella revisione
della Fase 14 (archivio 07, «Il motore dopo la revisione», le due decisioni del committente, e
«Le decisioni prese oggi»), con tre ragioni scritte:

1. «Una media di modelli è un numero che nessun modello ha previsto: per CLAUDE.md andrebbe
   dichiarata stima ovunque» (da «lo schermo non mente», «le stime lo dicono»).
2. La coerenza: un cielo votato accanto alla piastrella delle nuvole di `best_match` direbbe
   «poco nuvoloso» sopra un 98%.
3. I testimoni entravano allora per la nebbia, e si decise che votano sugli stati e non si
   stampano.

E una misura che lo sostiene (STATES §11, 17 nov 2026): **la media delle umidità senza la più
alta e la più bassa è più lontana dai METAR dell'umidità del solo modello dell'ora** (5,58 punti
contro 5,40; nella pianura padana 6,06 contro 5,33), perché i testimoni sono più grossolani
(9–10 km contro i 2 di ICON-D2).

**La lettura di oggi.** Le ragioni 2 e 3 e la misura sono giuste e restano: i campi devono stare
insieme, e una media **non pesata e non calibrata** di un modello fine con tre grossolani
peggiora. La ragione 1 è sbagliata come assoluto: confonde «un numero che nessun modello ha
previsto» con «un numero inventato». Ogni previsione è una stima; quel che conta per lo schermo
che non mente è che il metodo sia scritto, misurato e dichiarato. Il repo lo aveva già visto
senza trarne la conseguenza: nel 14.10 (archivio 08) l'errore della massima raddoppia quando i
modelli divergono (MAE 3,1 °C con uno scarto ≥ 4 °C, 1,4 sotto), cioè la divergenza dice
quanto fidarsi, ed è l'informazione su cui lavora una fusione.

Due premesse cadono con la stessa decisione:

- **STATES §1**: «with no observations and no history it cannot calibrate». Vero nell'app,
  falso per il prodotto: gli strumenti hanno storia e osservazioni (archivio delle previsioni,
  corse precedenti, METAR, satellite) e una correzione allenata lì entra nell'app come tabella.
- **«Nessuna osservazione entra nell'app»** (archivio 07, proposta 5 esclusa): resta vero per le
  osservazioni dal vivo; i coefficienti allenati sulle osservazioni non sono osservazioni.

### Le regole: il testo proposto

**Restano come sono**: «lo schermo non mente»; «ogni numero dice cosa farne»; un verdetto con la
sua aritmetica; la nebbia di due modelli (STATES §3, regola 5) e la media delle umidità come
filtro, mai come numero; **VISION §12, decisione 11** (un modello si misura prima di usarlo),
che è la regola più professionale del repo e si estende alla correzione; gratuito e senza
chiave, con il peso in chiamate di ogni richiesta; le API a pagamento solo negli strumenti.

**CLAUDE.md, «Numbers are one model's per hour; states are the app's»** diventa:

> **A number is one model's, or the app's measured correction of the models; states are the
> app's.** A temperature, a wind or a probability on screen is either the hour's model's own
> or a correction the app applies by a written method: trained in `tools/` on the forecast
> archive against what was observed, shipped as a table with its date, and allowed only where
> it beat the best single model at that lead day and in that region on a period it was not
> trained on, with the criterion written before the numbers. **The amounts are never averaged**:
> precipitation and snow are one model's per hour; what several models say about them is a
> probability. Fields that belong together move together (temperature, dew point, humidity,
> feels-like). Where no correction applies (outside the regions measured, a model missing) the
> number is the model's, and the app can always say which of the two it is.

**CLAUDE.md, «The screen must not lie»**: «Estimates say so» resta, con l'aggiunta: «a measured
correction is a forecast, not an estimate: it is named once, in the data credits and in the
guide, not on every number».

**VISION §12, decisione 13, nuova**: «La correzione dei modelli — decisa il <data del via>. Il
numero a schermo può essere la correzione misurata dei modelli (CLAUDE.md); cambia la decisione
del 26–27 ott 2026 (archivio 07, «Il motore dopo la revisione» e «Le decisioni prese oggi»):
i testimoni possono entrare nei numeri, con pesi misurati, e mai nelle quantità». La decisione
11 riceve una riga: «lo stesso vale per ogni correzione».

### L'inventario verificato: dove sta il vincolo, e che cosa cambia

Cercato in tutto il repo il 21 nov 2026, esclusi l'archivio (`docs/planning/NN-*.md`, congelato)
e il `CHANGELOG.md` (storia: la voce nuova va in `[Unreleased]`).

| Dove | Che cosa dice oggi | Che cosa cambia |
|---|---|---|
| `CLAUDE.md`, «Design constraints», «Numbers are one model's per hour» | mai una media di due modelli in un numero | il testo sopra |
| `CLAUDE.md`, «The screen must not lie» | «Estimates say so» | l'aggiunta sopra |
| `CLAUDE.md`, «Domain notes», «Free and without a key» | 7,9 chiamate per aggiornamento | la somma nuova, a ogni step che aggiunge variabili |
| `VISION.md` §1.2 (riga 41) | «Estimates are labeled» | niente: vale con la lettura sopra |
| `VISION.md` §12, decisione 11 (riga 1283) | un modello più fine si misura prima | una riga: anche la correzione |
| `VISION.md` §12 | — | decisione 13, nuova |
| `DESIGN.md` §1.1 (righe 31 e 44) | «a probability, an amount or a temperature on screen is the model's, or an estimate that says so» | «the model's, the app's measured correction (named in the credits), or an estimate that says so»; la tabella resta |
| `docs/STATES.md` §1 (righe 63–66) | «cannot calibrate» | la correzione è degli strumenti, l'app la applica |
| `docs/STATES.md` §12 (riga 996) | «they are never printed: the numbers on screen stay `best_match`'s» | i testimoni entrano nei numeri dove la correzione è misurata; sulla nebbia votano come oggi |
| `docs/STATES.md`, sezione nuova | — | §13, la correzione: metodo, tabelle, misure, dove vale |
| `docs/STATES.md` §3, regola 5 (riga 163) e §11 (riga 754) | la media delle umidità è un filtro, mai un numero | niente: la misura resta vera e la regola anche |
| `README.md` (righe 116 e 124) | «never shown as numbers»; «the numbers on screen are always the model's» | riscritte quando la correzione entra, senza lineette (regola del README) |
| `OpenMeteoApis.kt`, `OpenMeteoWitnessApi` (righe 233–244 e 276–289) | «Their numbers never reach a screen»; quattro variabili, 1,6 chiamate | il KDoc e la lista, con il peso nuovo, nello step che chiede variabili in più |
| `WeatherStateEngine.kt` (righe 187, 208, 235) | i testimoni «never printed» | niente: parla della nebbia, e lì resta vero |
| `TodayUiState.kt` (riga 440) | «The model's hour, not a blend of two» | il commento cita la regola nuova: il cielo trascinato resta dell'ora, corretta o no |
| `SnowLine.kt` (riga 13) | «The number is one model's, like every other» | il commento: la quota neve resta di un modello |
| `res/values*/strings.xml`, `settings_data_source_note`, la guida «I dati» | Open-Meteo come fonte | una riga: «previsioni corrette da Chiaro su più modelli», dove la correzione c'è |

Nessun test oggi verifica «una sola fonte per numero»: la regola stava solo nei documenti. I
test nuovi la scrivono per la correzione (i campi insieme, nessuna quantità fusa, il modello
grezzo dove la tabella non vale).

### Il piano: gli step

Un commit per step, come la Fase 17; ogni step che cambia un numero ha il suo criterio scritto e
committato **prima** di guardare i numeri, ed è misurato su un periodo che non lo ha allenato.

- **18.0 — Le regole.** Il testo sopra, approvato o cambiato dal committente, scritto in
  CLAUDE.md, VISION, DESIGN, STATES. Nessun codice.
- **18.1 — La misura di partenza, con i dati che ci sono.** Gli archivi del 14.10 hanno già,
  per 26 aeroporti e due periodi (inverno 2025-26, ottobre 2026), la temperatura di `best_match`
  e dei quattro testimoni per giorno di anticipo da 1 a 7, contro i METAR. Si confrontano:
  `best_match` grezzo, `best_match` con la correzione dell'errore sistematico (per zona, giorno
  di anticipo, ora, stagione), la media semplice, la fusione con pesi per anticipo. Allenato
  su un periodo, provato sull'altro, e viceversa. Dice se la strada vale prima di scrivere
  codice dell'app; se nessuna variante batte `best_match` di almeno 0,2 °C in entrambi i
  periodi, la fase si ferma qui e lo dice.
- **18.2 — ICON-CH, misurato.** Come il 14.4: gli aeroporti nel dominio di MeteoSvizzera
  (Malpensa, Linate, Orio, Caselle, Villafranca, Zurigo, e quel che il dominio prende), giorni
  1–5, temperatura e ora bagnata, contro ICON-D2 e ICON-EU. Se vince, è un membro della fusione;
  se no, resta fuori come ICON-2I.
- **18.3 — La correzione della temperatura nell'app.** Lo strumento (`tools/train_correction.py`)
  scrive la tabella; `:core:domain` la applica (puro Kotlin, nessun `Context`); la rugiada si
  sposta con la temperatura così che l'umidità resti del modello, e il percepito si ricalcola.
  La tabella ha la sua data e il suo limite (la sonda dice quando è vecchia: il fornitore cambia
  modelli, e una correzione di un modello che non c'è più è sbagliata).
- **18.4 — La fusione per anticipo.** Solo se 18.1 dice che batte la correzione da sola: i pesi
  per giorno di anticipo (il modello fine pesa nelle prime ore, IFS cresce dopo), al posto della
  cucitura a giorno intero di `prova-nuovo-modello`. I testimoni chiedono temperatura, rugiada e
  vento in più: il peso in chiamate nuovo si scrive qui.
- **18.5 — La probabilità della pioggia, calibrata.** La quantità resta del modello dell'ora; la
  probabilità si calibra sui METAR (affidabilità e Brier) da quella di `best_match` e da quanti
  modelli vedono ≥ 0,1 mm. Risolve anche le ore senza probabilità. La soglia del 20% di
  «Maltempo» (STATES §2) si rimisura sulla probabilità nuova.
- **18.6 — Vento e raffiche.** La stessa correzione della temperatura, per classe di quota, solo
  se 18.1 rifatto sul vento lo dice.
- **18.7 — La neve calda.** La correzione di Jennings del ramo di prova, misurata contro la fase
  dei METAR (`best_match` è giusto al 94%): entra solo se alza quel numero, e non toglie mai neve
  dove un'osservazione la vedeva.
- **18.8 — Sul telefono.** Le due APK affiancate (D1), i crediti, la guida, il README e le sue
  immagini se cambia quel che mostrano; la prova del committente contro `main`.

### Le regole comuni della fase

- Niente quantità mediate; i campi insieme; il modello grezzo dove la correzione non vale.
- Ogni tabella ha la data, i periodi su cui è allenata e quelli su cui è provata, e un test che
  la confronta con lo strumento che l'ha scritta (come i composer delle icone): non si modifica a
  mano.
- Ogni richiesta nuova ha il suo peso in chiamate accanto alla lista delle variabili.
- **Il limite dei dati, detto subito**: 26 aeroporti, quasi tutti in pianura, due periodi. Una
  correzione per zona e quota allenata su poche stazioni può imparare il rumore: la prova su un
  periodo non usato è il freno, e dove i dati non bastano (la montagna) la correzione non vale e
  lo schermo resta del modello.

### Le decisioni del committente

- **D1 — Le due APK affiancate.** Oggi il debug di ogni ramo è `com.callbackdev.chiaro.debug`:
  l'APK di questo ramo sostituirebbe quella di `main`. Proposta: su questo ramo il debug prende
  `applicationIdSuffix ".prova"` e un nome di lancio che lo dice, così le due stanno sul telefono
  insieme; il suffisso torna `.debug` se il ramo entra.
- **D2 — Le osservazioni alpine, per gli strumenti soltanto.** I METAR non bastano per la
  montagna. I dati aperti delle stazioni di MeteoSvizzera e delle ARPA (Lombardia, Piemonte,
  Veneto, Trentino-Alto Adige) sono da verificare: licenza, archivio, accesso senza chiave. Nessuno
  entra nell'app; servirebbero host nuovi nella rete del runner.
- **D3 — L'ordine.** ICON-CH (18.2) prima o dopo la correzione (18.3). Proposta: dopo, perché la
  correzione vale su tutta Italia e ICON-CH solo dove copre.
- **D4 — Il ramo `prova-nuovo-modello`.** Resta com'è, come termine di confronto; non si unisce a
  questo. Proposta: sì.
- **D5 — Il bilancio delle chiamate.** Oggi 7,9; la fusione ne porterebbe circa 3 in più (più
  variabili ai testimoni, ICON-CH dove copre). Proposta: un tetto di 12 per aggiornamento,
  scritto qui, sotto il quale ogni step resta.

### Le domande del committente: radar, testimoni, Jennings, ECMWF (21 nov 2026)

**Il nowcast da radar** non era nella proposta: c'era solo il rimando a VISION §12, decisione 5,
lasciata aperta il 9 set 2026. I servizi professionali per le prime due ore non usano un modello:
spostano in avanti l'eco del radar osservato secondo il moto delle ultime immagini (MeteoSvizzera
INCA-CH/NowPal, DWD RADVOR, ZAMG INCA, il Met Office con il nowcast che IMPROVER pesa al 100% in
partenza e a zero verso le quattro ore). «Prossime due ore» di Chiaro è un'altra cosa: i quarti
d'ora calcolati da ICON-D2, un modello e non un radar, e la guida lo dice. La fonte per l'Italia
c'è (Radar-DPC: GeoTIFF ogni 5 minuti, 1 km, 619 KB, CC BY-SA, senza chiave; VISION §12, 5).
Proposta: **18.9 — Il nowcast dal radar, da decidere (D6)**, solo il testo nell'app: nessuna
mappa, il radar spostato in avanti sul punto del lettore e fuso con i quarti di ICON-D2 per
anticipo. Prima si verifica che l'API dia le immagini passate (il servizio delle tessere dà solo
l'ultima, VISION §12, 5): senza almeno due immagini non c'è moto. Costo: circa 1,2–1,8 MB per
aggiornamento se l'API non ritaglia, la batteria del calcolo, e solo l'Italia.

**I testimoni e `best_match`.** Per la nebbia restano come sono: sono stati scelti su una misura
(STATES §11; GFS fuori perché giusto nel 5% delle nebbie). Per la fusione il numero conta meno
della varietà: MOSMIX ne fonde due (ICON e IFS), IMPROVER quattro. Oggi le famiglie sono tre, non
cinque: KNMI e DMI sono lo stesso modello (HARMONIE-AROME Europe), con tre giorni di orizzonte.
`best_match` come modello principale resta: ICON-D2 nelle prime 48 ore al nord è un modello a
2 km con i quarti e la correzione di quota. Il punto debole è il medio termine (ICON-EU, poi ICON
globale ai giorni 6–7). Candidati da misurare in 18.1 e 18.2, non da aggiungere a occhio: **ECMWF
IFS per i giorni 3–7** (c'è già negli archivi, per giorno di anticipo); **AIFS** (il modello di
ECMWF a intelligenza artificiale: non è negli archivi, va scaricato); **ICON-CH** sulle Alpi.
Il criterio di 18.1 si scrive prima di guardare: per questo i numeri IFS contro `best_match` per
giorno di anticipo non sono stati calcolati oggi, anche se i dati ci sono.

**Jennings et al. 2018** è una fonte seria (17,8 milioni di osservazioni, Nature
Communications), ma è una soglia per quando un modello non dice la fase, e i professionisti la
fase la prendono dai profili verticali: il bulbo umido, la quota dello zero (IMPROVER, MET
Norway). `main` non ne ha bisogno: la fase di `best_match` viene dalla microfisica di ICON ed è
giusta nel 94% delle ore (STATES §10), e la quota neve c'è già (14.8). Il difetto che Jennings
corregge, la neve «tutta o niente» di ICON-CH su Open-Meteo, esiste solo se ICON-CH entra. Resta
quindi **18.7, legato a 18.2**: si misura solo se ICON-CH entra, e con la quota dello zero
accanto all'umidità; non tocca mai le ore di `best_match`.

**ECMWF IFS** è in `main` come testimone (9 km): vota sulla nebbia e dà l'umidità al filtro, e
nessun suo numero va a schermo. È il riferimento mondiale del medio termine e la base di MOSMIX,
IMPROVER e MeteoSvizzera: entra nella revisione come membro della fusione, soprattutto dal giorno
3, se 18.1 lo dice. Il suo ensemble (ENS) sarebbe la strada professionale per la probabilità, ma
l'API Ensemble è fra quelle che l'app non chiama (CLAUDE.md, «Free and without a key»): la
probabilità resta quella di 18.5.

- **D6 — Il nowcast dal radar** (18.9): verificare le immagini passate di Radar-DPC e poi
  decidere; riapre VISION §12, decisione 5, solo per il testo, senza mappa.

### 18.0 — Le regole (committente, 21 nov 2026)

**Il via.** Il committente conferma le scelte della proposta e le sei decisioni come proposte:
D1 le due APK affiancate, D2 le osservazioni alpine da cercare per gli strumenti, D3 ICON-CH
dopo la correzione, D4 `prova-nuovo-modello` lasciato com'è, D5 il tetto di 12 chiamate, D6 le
immagini passate di Radar-DPC da verificare.

**Fatto.**

- `CLAUDE.md`: la regola «A number is one model's, or the app's measured correction of the
  models» al posto di «Numbers are one model's per hour» (il testo di «Le regole: il testo
  proposto», con la media delle umidità detta come la ragione per cui una correzione è pesata e
  misurata); «The screen must not lie» con la correzione nominata una volta; le API d'archivio.
- `VISION.md` §12: la decisione 13, e una riga alla 11.
- `DESIGN.md` §1.1: «what stays forbidden is inventing a number», con la correzione fra le tre
  forme di un numero onesto.
- `docs/STATES.md` §1 (la calibrazione è degli strumenti) e §12 (i testimoni possono entrare
  nella correzione, mai in una quantità).
- `TodayUiState.kt`: il commento cita la regola con il nome nuovo.
- **D1**: `applicationIdSuffix ".prova"` e l'etichetta «Chiaro (prova)» su questo ramo, e la riga
  in CLAUDE.md; nessun file dell'app dipende dal nome del pacchetto (cercato: né `authorities` né
  `APPLICATION_ID`).
- Non toccati, come l'inventario diceva: il `README.md` e le stringhe dei crediti, che cambiano
  quando una correzione entra davvero; i KDoc dei testimoni e di `SnowLine`, che parlano della
  nebbia e della quota neve e lì restano veri.

**Una correzione a CLAUDE.md** (deviazione, trovata rispondendo al committente). La regola
«Free and without a key» chiamava Ensemble, Historical, Satellite e Previous Runs «the paid
tiers». Non è vero: la pagina dei prezzi di Open-Meteo, letta il 21 nov 2026, li dà gratuiti per
l'uso non commerciale e senza chiave (assenti solo dal piano Standard a pagamento). Quel che li
tiene fuori dall'app è il peso: l'esempio della pagina Ensemble, una variabile di un modello per
7 giorni, vale 4,0 chiamate, perché ogni membro conta. La regola resta (fuori dall'app), il
motivo è riscritto; uno step che ne volesse uno nell'app ne misura prima il peso.
`precipitation_probability` di `best_match` è già la quota di membri di un ensemble (STATES §1):
la probabilità che l'app stampa viene già da lì.

### 18.1 — La misura di partenza: il criterio, scritto prima dei numeri (21 nov 2026)

Committato prima che lo strumento calcoli un solo errore, come per il 14.4.

**I dati.** I due archivi in `tools/measurements/` (inverno: 1 dic 2025–31 gen 2026; ottobre:
26 set–25 ott 2026), 26 aeroporti, blocco `previous_runs`: la temperatura a 2 m di `best_match`,
IFS e UKMO per i giorni 1–7 dell'app, e di KNMI e DMI per i giorni 1–2 (dal 3 sono vuoti:
guardato prima, il numero delle ore piene, non gli errori). Contro il METAR entro 30 minuti.
Un testimone che è una copia di `best_match` in quel giorno (`day_copy`, come nel 14.10) è
escluso da quel giorno.

**Le varianti**, ora per ora:

- **A** `best_match` com'è: il riferimento.
- **B** IFS e UKMO da soli: si stampano per sapere, non decidono.
- **C** la media semplice dei modelli presenti: il controllo (CLAUDE.md: una media non è una
  correzione), non si può scegliere.
- **D** `best_match` meno il suo errore medio, per zona, giorno di anticipo e fascia di sei ore
  (UTC 0–6, 6–12, 12–18, 18–24).
- **E** la fusione: pesi per zona e giorno di anticipo su `best_match`, IFS e UKMO (e KNMI e DMI
  ai giorni 1–2), a passi di 0,1, non negativi, somma 1, scelti per il minimo errore medio
  assoluto.
- **F** la fusione E, poi la correzione dell'errore medio come in D.

**Le zone**: la pianura padana (Linate, Malpensa, Orio, Caselle, Villafranca, Bologna, Tessera),
il centro-sud e le isole (Fiumicino, Capodichino, Bari, Punta Raisi, Catania, Elmas), il resto
d'Europa (le tredici altre). **I gruppi di anticipo**: giorni 1–2, 3–5, 6–7.

**Come si allena e come si prova** (il freno contro il rumore). Ogni aeroporto è provato con una
tabella allenata **senza di lui e senza i suoi giorni**: gli altri aeroporti della sua zona,
sull'altra metà del periodo (le due metà contigue, non i giorni alterni, che si somigliano), e
viceversa. Così la tabella non ha mai visto né il posto né i giorni su cui è giudicata. Poi,
a parte, **fra i periodi**: allenata sull'inverno e provata su ottobre, e viceversa.

**La misura**: l'errore medio assoluto (MAE) della temperatura dell'ora, per zona, gruppo di
anticipo e periodo; accanto, la massima e la minima del giorno, che non decidono.

**La decisione**, dichiarata ora:

- Una variante fra D, E, F **passa** in una cella (zona, gruppo di anticipo) se il suo MAE è
  **almeno 0,2 °C sotto quello di A in entrambi i periodi** (il margine del 14.4) e **nessun
  aeroporto italiano della zona** peggiora di più di 0,1 °C in uno dei due periodi.
- Fra quelle che passano vince la più semplice (D, poi E, poi F), salvo che una più complessa
  sia più bassa di almeno 0,05 °C in entrambi i periodi.
- **Se nessuna variante passa in nessuna cella della pianura padana e del centro-sud**, la
  correzione della temperatura non si fa: 18.3 e 18.4 non esistono, e la fase va avanti solo con
  la probabilità (18.5) e quel che resta.
- **Fra i periodi** non decide: se una variante che passa peggiora A fra i periodi, la correzione
  è stagionale, e 18.3 aspetta un archivio della stessa stagione dell'anno prima (si scarica sul
  runner) prima di entrare nell'app.
- Non si cercano dopo i numeri sottoinsiemi (un aeroporto, un'ora, un mese) in cui una variante
  vinca, e i passi dei pesi e le fasce non si cambiano dopo aver visto i risultati.

### 18.1 — La misura di partenza: fatto (21 nov 2026)

`python tools/analyze_correction.py tools/measurements/metar-*.json.gz` (strumento nuovo, 50
secondi). **Il verdetto: la fusione E passa ai giorni 6–7 in Italia, e da nessun'altra parte.**

**La tabella decisiva** (MAE della temperatura dell'ora in °C contro il METAR, ogni aeroporto
provato senza di lui e senza i suoi giorni; A `best_match`, C la media semplice, D la correzione
dell'errore medio, E la fusione, F la fusione corretta):

| Zona | Giorni | Periodo | A | IFS | UKMO | C | D | E | F |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|
| Pianura padana | 1–2 | inverno | 0,98 | 1,33 | 1,30 | 1,02 | 0,98 | 0,96 | 0,96 |
| Pianura padana | 1–2 | ottobre | 0,82 | 1,10 | 1,41 | 0,89 | 0,85 | 0,79 | 0,80 |
| Pianura padana | 3–5 | inverno | 1,46 | 1,71 | 1,56 | 1,39 | 1,39 | 1,39 | 1,32 |
| Pianura padana | 3–5 | ottobre | 1,26 | 1,29 | 1,55 | 1,13 | 1,33 | 1,14 | 1,16 |
| **Pianura padana** | **6–7** | inverno | 2,05 | 2,06 | 1,87 | 1,72 | 1,95 | **1,72** | 1,67 |
| **Pianura padana** | **6–7** | ottobre | 1,65 | 1,72 | 1,89 | 1,43 | 1,73 | **1,43** | 1,42 |
| Centro-sud e isole | 1–2 | inverno | 1,06 | 1,10 | 1,40 | 1,03 | 1,09 | 1,02 | 1,04 |
| Centro-sud e isole | 1–2 | ottobre | 1,02 | 1,15 | 1,21 | 0,97 | 1,07 | 0,97 | 1,02 |
| Centro-sud e isole | 3–5 | inverno | 1,33 | 1,36 | 1,55 | 1,26 | 1,36 | 1,26 | 1,26 |
| Centro-sud e isole | 3–5 | ottobre | 1,23 | 1,32 | 1,40 | 1,12 | 1,29 | 1,15 | 1,21 |
| **Centro-sud e isole** | **6–7** | inverno | 2,39 | 1,83 | 1,92 | 1,70 | 2,26 | **1,72** | 1,72 |
| **Centro-sud e isole** | **6–7** | ottobre | 1,60 | 1,58 | 1,64 | 1,35 | 1,63 | **1,39** | 1,43 |
| Resto d'Europa | 6–7 | inverno | 2,39 | 2,11 | 2,74 | 2,28 | 2,39 | 2,28 | 2,25 |
| Resto d'Europa | 6–7 | ottobre | 1,98 | 1,86 | 2,04 | 1,75 | 2,01 | 1,78 | 1,79 |

(Il resto d'Europa ai giorni 1–5, e le tabelle fra i periodi, della massima e della minima e
dei pesi, sono nell'uscita dello strumento; nessuna cambia il verdetto.)

**Il verdetto del criterio**, cella per cella:

- **Pianura padana, giorni 6–7: E** (−0,33 °C d'inverno, −0,22 a ottobre; passa anche F, che
  non è più bassa di 0,05 in entrambi i periodi, quindi vince la più semplice).
- **Centro-sud e isole, giorni 6–7: E** (−0,67 d'inverno, −0,21 a ottobre).
- **Ogni altra cella: nessuna**. Giorni 3–5: E guadagna 0,07–0,12, sotto il margine di 0,2.
  Giorni 1–2: 0,02–0,05. Resto d'Europa, giorni 6–7: −0,11 d'inverno, fuori.
- **Fra i periodi** E tiene in tutte e due le celle (pianura padana 2,05 → 1,77 e 1,65 → 1,48;
  centro-sud 2,39 → 1,73 e 1,60 → 1,37): la fusione **non** è stagionale, e non aspetta un
  archivio dell'anno prima.

**Che cosa dicono i numeri, oltre al verdetto.**

- **La correzione dell'errore medio (D) non funziona su un posto che non ha visto**: è pari o
  peggio di `best_match` quasi ovunque (fra i periodi, centro-sud giorni 6–7, 2,04 contro 1,60).
  L'errore sistematico di un modello è del posto (la quota, la costa, la valle), non della zona:
  è quel che fanno MOSMIX e MeteoSvizzera, una correzione **per stazione**, e un'app che serve
  qualunque punto senza le sue osservazioni non può averla. **18.3 così com'era non esiste**:
  una correzione per posto servirebbe le osservazioni del posto, che l'app non legge.
- **Il punto debole è il medio termine, come detto rispondendo al committente**: ai giorni 6–7
  `best_match` in Italia è ICON globale, e al centro-sud d'inverno sbaglia la minima di 3,49 °C
  (E 2,06). IFS da solo lì vale già 1,83 contro 2,39.
- **Ai giorni 1–2 `best_match` è già il migliore**: ICON-D2 con la correzione di quota.
  Nessun testimone da solo gli si avvicina in Italia; la fusione guadagna centesimi.
- **La media semplice C vale quanto la fusione E** in quasi ogni cella: i pesi imparati
  (pianura padana, giorno 6: 0,4 `best_match`, 0,4 IFS, 0,2 UKMO) sono vicini all'uguale. Non è
  una contraddizione con la media delle umidità (STATES §11): lì i testimoni erano più
  grossolani del modello dell'ora, qui ai giorni 6–7 lo sono meno (IFS a 9 km contro ICON a 13).

**Che cosa cambia nel piano.**

- **18.3** esce: nessuna correzione per zona che non sia per posto.
- **18.4 diventa il primo step di codice: la fusione della temperatura ai giorni 6–7 in Italia**,
  con i pesi per giorno di anticipo allenati su tutti e due i periodi e tutti gli aeroporti della
  zona. **Nessuna chiamata in più**: i testimoni portano già la temperatura di IFS e UKMO per
  sette giorni. Da decidere nello step, prima dei numeri: quale zona ha un posto (le due zone
  sono misurate su aeroporti di pianura e di costa: la montagna resta del modello, come la regola
  dice); come si muovono insieme la rugiada, l'umidità e il percepito; la massima e la minima del
  giorno rifatte dalle ore fuse.
- 18.5 (la probabilità), 18.2 (ICON-CH) e il resto restano come sono.

**Deviazioni.** Lo strumento usa numpy (la ricerca dei pesi in Python puro durava decine di
minuti), l'unico del repo; i giorni della massima e della minima sono di 24 ore UTC, non locali.
Nessuna soglia, fascia o passo è cambiato dopo i numeri.

### Una sola APK con lo switch, al posto di due affiancate (committente, 21 nov 2026)

**La richiesta.** Il committente cambia D1: non due APK affiancate, ma la stessa app con uno
switch nelle Impostazioni, il modello di `main` da una parte e il `main` corretto dall'altra,
per vedere le differenze senza due installazioni. E chiede che fine fa il modello del ramo di
prova.

**Le due risposte.**

- **Su questo ramo si lavora sul modello di `main`**: il ramo è partito da `main` a `56d8693`, e
  la Fase 18 ci scrive sopra le correzioni misurate. Il modello di `prova-nuovo-modello`
  (ICON-CH, ICON-2I, ECMWF a giorni interi) **non è qui**: resta sul suo ramo, intatto (D4), da
  dove si prenderà ICON-CH solo dopo la misura di 18.2.
- **D1 cambia**: `applicationIdSuffix` torna `.debug` e l'etichetta «Chiaro (dev)», com'erano.
  L'APK di questo ramo **sostituisce** quella di `main` (e quella di `prova-nuovo-modello`, che
  ha lo stesso pacchetto): «Attuale» nello switch è `main`.

**Fatto: lo switch.** Impostazioni, Aggiornamenti, «Modello delle previsioni»: «Corretto: più
modelli, misurati» (il default) e «Attuale: scelta automatica di Open-Meteo». La forma è quella
di `prova-nuovo-modello` (il committente l'ha già provata), con una differenza che la semplifica:

- **le due strade leggono lo stesso download**. La fusione di 18.4 usa la temperatura di IFS e
  UKMO che i testimoni portano già: lo switch cambia solo quel che il mapper calcola
  (`WeatherReportMapper.map(corrected = …)`, `SystemInfo.corrected`), mai una richiesta. Per
  questo **la cache su disco è una sola** e un cambio di switch rimappa quel che c'è senza
  scaricare; la cache in memoria sa con quale strada è stata mappata una voce e non serve mai
  quella dell'altra (`WeatherRepository`, `CacheEntry.source`).
- «Cos'è cambiato» riparte dal momento dello switch, dal primo aggiornamento dopo
  l'installazione sopra `main` e dopo un reset che cambia la strada
  (`SettingsStore.forecastSourceForFetch`, `forecastSourceSinceEpochS`), come nel ramo di prova.
  Le chiavi delle preferenze hanno un nome loro (`forecast_source_fase18`): il `NEW` che il ramo
  di prova ha scritto sul telefono non viene letto come un valore di questa.
- Il testo del dialogo non promette niente che non ci sia: «corregge solo dove il confronto con
  quel che è stato osservato ha mostrato che sbaglia meno; dove non c'è una correzione misurata,
  i numeri sono gli stessi». **Fino a 18.4 le due strade danno gli stessi numeri**: lo switch c'è
  prima della correzione perché la prova sul telefono parta dalla stessa app.
- I widget si ridisegnano al prossimo aggiornamento, non al cambio di switch: un cambio non
  scrive nella cronologia, e il ridisegno è appeso a quella.

Test: `ForecastSourceTest` (la strada nel report, la cache in memoria che non serve l'altra, il
disco rimappato senza scaricare, la data dello switch e del primo aggiornamento).

La suite intera verde dopo lo switch (`test :app:testDebugUnitTest`): 3 564 test, 16 saltati
(le immagini del README, solo su richiesta), nessuno fallito.

### 18.4 — La fusione della temperatura ai giorni 6–7: le scelte, scritte prima della tabella (22 nov 2026)

Il committente: «vai in ordine consigliato e parti con il prossimo punto». La misura (18.1) ha
deciso **che cosa** si fonde; qui si decide **dove, quando e con che cosa intorno**, prima di
scrivere la tabella dei pesi e il codice.

- **I posti.** Solo l'Italia (`IT`, e San Marino e Vaticano, che le stanno dentro) e solo fino a
  **350 m** di quota (`elevation` della risposta di Open-Meteo, la quota del punto con cui il
  fornitore corregge la temperatura): gli aeroporti misurati vanno da 2 a 301 m (Caselle), e
  la montagna resta del modello, come la regola chiede dove non c'è misura. **Due tabelle**:
  quella della pianura padana a nord di **44,0° N**, quella del centro-sud sotto. Il centro fra
  Roma (41,8°) e Bologna (44,5°) non ha un aeroporto suo: prende la tabella del centro-sud, la
  zona che lo nomina, e dove le due zone si toccano i pesi misurati sono vicini (18.1). Le
  valli alpine sotto i 350 m (Trento, Bolzano) prendono quella della pianura padana: non sono
  misurate, e lo si dice qui; un archivio di stazioni alpine (D2) le misurerà.
- **I giorni: le ultime due righe della settimana, intere.** La misura è per giorno di
  anticipo (le ore fra 120 e 168 dal momento della corsa); l'app non sa quando il modello ha
  girato, e un confine che si sposta ogni ora taglierebbe la sesta riga in due e la farebbe
  cambiare a ogni aggiornamento. Quindi si fonde **per giorno di calendario**: il sesto giorno
  della risposta (indice 5) con i pesi del giorno 6, il settimo (indice 6) con quelli del 7.
  Il mattino del sesto giorno, letto presto, è ancora al giorno 5 di anticipo: lì la fusione
  era più bassa di `best_match` di 0,07–0,12 °C (18.1, giorni 3–5), mai più alta, quindi non
  peggiora, e una riga è una sola risposta.
- **I campi insieme.** La temperatura dell'ora è la fusione; **l'umidità resta del modello**
  e la rugiada si ricalcola da quella e dalla temperatura nuova (Magnus, coefficienti di
  Alduchov e Eskridge 1996: 17,625 e 243,04 °C); il **percepito si sposta della stessa
  differenza** della temperatura (in Steadman, la forma di Open-Meteo, a umidità fissa la
  derivata del percepito rispetto alla temperatura è vicina a 1); **la massima e la minima del
  giorno** si rifanno dalle ore fuse. Pioggia, neve, nubi, vento, zero termico e quota neve
  restano del modello: la regola vieta di fondere le quantità, e gli altri campi non sono
  stati misurati.
- **I testimoni.** IFS e UKMO dalla risposta dei testimoni che l'app scarica già (temperatura a
  2 m, sette giorni): **nessuna chiamata in più**. Un testimone che è `best_match` intorno a
  quell'ora (`Witnesses.copies`) è escluso, come nella misura (`day_copy`); un membro che
  manca rinormalizza gli altri; senza testimoni l'ora resta del modello.
- **«Cos'è cambiato».** Un giorno porta nella cronologia il segno della tabella che lo ha
  fuso (`.correction`, 6 o 7); se il segno cambia fra due aggiornamenti — a mezzanotte il
  settimo giorno diventa il sesto, il sesto diventa il quinto e torna del modello — quel
  giorno non ha una base di confronto, come un giorno che entra nell'orizzonte: la differenza
  fra la fusione e il modello non è una revisione della previsione.
- **La tabella** si scrive con `tools/analyze_correction.py --write-table`, allenata su
  tutti gli aeroporti della zona e su tutti e due i periodi, in un file Kotlin di `:core:domain`
  che dice chi lo scrive e non si modifica a mano; `--check` dice se il file è indietro.
- **A schermo**: la riga in fondo a Oggi e i crediti nominano la fusione solo dove c'è
  (`systemInfo.corrected` e almeno un giorno fuso), con ECMWF e Met Office, i modelli di cui
  la fusione legge i numeri (la licenza CC BY 4.0 di Open-Meteo chiede di nominare la fonte;
  lo chiedono anche ECMWF e il Met Office per i loro dati aperti).

### 18.4 — La fusione della temperatura ai giorni 6–7: fatto (22 nov 2026)

Come le scelte sopra, senza deviazioni nelle regole.

- **La tabella** (`tools/analyze_correction.py --write-table`, `TemperatureBlendTable.kt`;
  `--check` dice «aggiornato»), pesi `best_match` / IFS / UKMO: pianura padana 0,3 / 0,3 / 0,4
  ai giorni 6 e 7; centro-sud 0,2 / 0,5 / 0,3 al giorno 6 e 0,3 / 0,4 / 0,3 al 7. Allenati sui
  due periodi insieme, sono un po' diversi da quelli di ciascun periodo letti in 18.1 (la
  pianura padana a ottobre era 0,4 / 0,4 / 0,2): la ricerca sceglie il minimo di un errore
  piatto vicino all'uguale, e la media semplice era già quasi pari alla fusione.
- **Il codice**: `TemperatureBlend` in `:core:domain` (le zone, i pesi, la fusione, la rugiada);
  `TemperatureCorrection` in `:core:data` (la fusione posata sulla risposta prima del mapper);
  `Witnesses.temperatureOf` (un testimone che è `best_match` intorno all'ora non conta);
  `WeatherReportMapper.map` corregge solo con `corrected`; `DailyForecast.correction` porta la
  tabella del giorno fino alla cronologia, e `ForecastDiff` non confronta un giorno il cui segno
  è cambiato.
- **A schermo**: in fondo a Oggi, solo dove un giorno è fuso, la riga «Ultimi due giorni:
  temperatura corretta con ECMWF e Met Office»; nelle Impostazioni il credito dei dati la dice
  con lo switch su «Corretto»; la guida ha un paragrafo in «I dati». `README.md` (le fonti e la
  frase sui numeri) e STATES §13.
- **Le immagini del README**: rigenerate tutte e sedici, nessuna cambia (la registrazione di
  Milano non ha testimoni, e le Impostazioni ritratte sono il gruppo «Aspetto»); guardata
  `settings.png`.
- **Dal vivo, Milano, 22 nov 2026** (una richiesta, solo per vedere la grandezza): giovedì 27,
  `best_match` 1,8 / 6,1 °C, IFS 2,8 / 8,1, UKMO 6,1 / 8,2, la fusione **4,0 / 7,4**; venerdì 28
  UKMO ha 14 ore su 24 (è al bordo del suo orizzonte) e la fusione rinormalizza da sola:
  0,7 / 5,9 → 1,1 / 6,7. Quando ICON globale si stacca dagli altri due, la riga si sposta di
  gradi, non di decimi: è lì che la misura ha trovato il guadagno.

Test: `TemperatureBlendTest` (dove vale, la tabella che somma a uno, i membri che mancano, la
rugiada andata e ritorno), `TemperatureCorrectionTest` (sulla Milano registrata: i due giorni
con i pesi giusti e gli altri del modello, i campi insieme e le quantità ferme, la copia di
`best_match` esclusa, fuori dall'Italia e sopra i 350 m niente, il segno nella cronologia),
`ForecastDiffTest` (un giorno il cui segno cambia non ha base, uno con lo stesso segno si
confronta come sempre).

La suite intera verde dopo 18.4 (`test :app:testDebugUnitTest :app:lintDebug`): 3 580 test, 16
saltati, nessuno fallito; lint senza errori, e nessun avviso nuovo nei file toccati (i tre che
cadono in quelle schermate sono su righe del 30 ott 2026).
