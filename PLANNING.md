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
- **Nessuna fase attiva** dal 16 nov 2026: quel che resta in «Aperto» sono una prova sul telefono, una misura che aspetta l'inverno e, dal 17 nov 2026, la nebbia dei due modelli da vedere dal vivo dopo il 30 novembre. Il registro fino al 16 nov 2026 è in `docs/planning/14-novembre-12-16.md`.
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
