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
  come Oggi e il cielo da esplorare», con i primi due step chiusi (16.1, 16.2); uno step chiuso da
  qui in poi scrive la sua sezione nel registro qui sotto.
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
  il piede dell'immagine sotto un cielo coperto, il dito sul nastro e sul cielo. Fatto: dato per provato dal committente il 10 nov 2026 (registro, «Le prove sul
  telefono della Fase 16, da 16.1 a 16.3»).
- [x] **16.2** I momenti come l'agenda di Oggi; «Aggiungi» nel titolo al posto del pulsante flottante (Fase 16). Chiuso il 10 nov 2026 (archivio 11, «16.2 — I momenti come l'agenda di Oggi»).
- [x] **16.2 sul telefono** (archivio 11, «16.2 — I momenti come l'agenda di Oggi», «Cosa conferma solo il telefono»): le icone del
  Cielo a 34dp, che erano a 51 per scelta del committente (decisione 3: da confermare o da
  riportare indietro), la luce zodiacale soprattutto; «Aggiungi» accanto al titolo; la riga con
  il carattere grande. Fatto: dato per provato dal committente il 10 nov 2026 (registro, «Le prove sul
  telefono della Fase 16, da 16.1 a 16.3»).
- [x] **16.3** Il motore: Marte e Saturno; sorgere, culmine e tramonto di un oggetto (Fase 16). Chiuso il 10 nov 2026, «16.3 — Il motore: Marte e Saturno, e quando un oggetto sorge, culmina e tramonta».
- [x] **16.3 sul telefono** (stesso registro, «Cosa conferma solo il telefono»): Marte e Saturno
  nell'hero di Oggi e nella finestra in una notte in cui ci sono; la tinta di Marte accanto a
  Giove; i due nomi sulla finestra. Fatto: dato per provato dal committente il 10 nov 2026 (registro, «Le prove sul
  telefono della Fase 16, da 16.1 a 16.3»).
- [ ] **Le immagini dei widget nel README dipendono dal fuso della macchina** (registro, «16.3»,
  «Come è stato verificato»). La finestra scrive l'ora del luogo accanto al nome quando
  l'orologio del luogo non è quello del telefono; nel test il telefono è la macchina, e su una
  con un altro fuso esce «Milan · 01:00» (nelle immagini committate), su una col fuso di Milano no.
  Proposta, non decisa: `ReadmeScreenshots` fissa il fuso della JVM a quello del luogo, e le due
  immagini si rigenerano senza l'ora.
- [x] **16.4** Il motore: la notte di un'altra data; l'elenco «Da vedere» come dato (Fase 16). Chiuso il 10 nov 2026, «16.4 — Il motore: la notte di un'altra data, e cosa vale la pena guardare».
- [x] **Un verdetto su una finestra che la previsione copre a metà** (registro, «16.4», decisione 4).
  `SkyVerdictEngine` giudica le ore che trova: se la previsione finisce dentro la finestra, il
  verdetto è della parte che c'è, per ogni riga dell'app (uno sciame o la notte al settimo
  giorno). Fatto il 10 nov 2026 con la proposta com'era: «Presto per dirlo», con il motivo
  «la previsione non arriva ancora così lontano» (registro, «Le due caselle del 16.4», «La finestra coperta a metà»).
- [x] **La finestra di uno sciame che si apre al picco** (registro, «16.4», decisione 10). Quando
  il picco cade di notte la finestra del lavoro va dal picco all'alba: per le Geminidi del
  2026 sono sedici minuti (05:51–06:07), in una notte in cui il radiante è su dal crepuscolo
  all'alba. Fatto il 10 nov 2026 con la proposta com'era: la notte intera del picco (registro,
  «Le due caselle del 16.4», «La notte dello sciame»).
- [ ] **Le due caselle del 16.4 sul telefono** (registro, «Le due caselle del 16.4», «Cosa conferma
  solo il telefono»): la riga di uno sciame in «In arrivo» con la data della sera, il suo
  promemoria all'imbrunire, la notte al bordo della previsione che dice «presto per dirlo».
- [x] **Il verdetto del promemoria di una finestra è della sua prima ora** (fatto il 10 nov 2026, registro, «Le due correzioni trovate con le caselle»; nato in «Le due
  caselle del 16.4», «Trovato e non toccato»). `SkyAlarmReceiver` giudica l'istante in cui la
  finestra si apre (`end = null`), la riga del Cielo tutta la finestra: per la notte buia e,
  dal 10 nov 2026, per uno sciame, il promemoria può dire «Bello» di una notte che la riga dice
  «Così così». Proposta, non decisa: il promemoria risolve la finestra del lavoro e la giudica
  intera, come la riga.
- [x] **Dal 1° gennaio lo sciame «prossimo» è quello dell'anno dopo** (fatto il 10 nov 2026, registro, «Le due correzioni trovate con le caselle»; nato in «Le due caselle
  del 16.4», «Trovato e non toccato»). `SkyUpcoming.of` risolve un lavoro annuale nell'anno di
  oggi, e le Quadrantidi «del 2027» cadono nel gennaio 2028: dal 1° gennaio 2027 la riga di
  chi le segue dice l'anno dopo, e nella notte del picco (3–4 gennaio 2027) il cielo disegnato
  non ne nomina il radiante. Da verificare per gli altri lavori annuali la cui risposta cade
  nel gennaio dopo (l'alba più tarda). Proposta, non decisa: `SkyUpcoming.of` chiede anche
  l'anno prima, come `SkyScheduler.next` e l'elenco «Da vedere». **Da fare prima del 1°
  gennaio 2027.**
- [x] **16.5** Le costellazioni: la fonte e la licenza, la risorsa, i nomi (Fase 16). Chiuso il 10 nov 2026, «16.5 — Le costellazioni: il dato».
- [ ] **16.5 sul telefono** (registro, «16.5», «Cosa conferma solo il telefono»): la riga «Linee
  delle costellazioni» in Impostazioni → Dati e riconoscimenti, che apre la pagina della fonte.
- [x] **16.6** L'esploratore: la camera libera e il cielo a tutto schermo (Fase 16). Chiuso l'11 nov 2026, «16.6 — L'esploratore: la camera libera e il cielo a tutto schermo».
- [ ] **16.6 sul telefono** (registro, «16.6», «Cosa conferma solo il telefono»): il dito sul cielo
  (trascinare, pizzicare, il giro vicino allo zenit), la fluidità con le stelle a schermo, la Via
  Lattea disegnata dalla GPU del telefono, la pillola «Esplora» con il carattere grande, le icone
  chiare della barra di stato, il gesto indietro.
- [x] **16.7** L'esploratore: l'ora, la notte, le nuvole, le costellazioni (Fase 16). Chiuso l'11 nov 2026, «16.7 — L'esploratore: l'ora, la notte, le nuvole, le costellazioni».
- [ ] **16.7 sul telefono** (registro, «16.7», «Cosa conferma solo il telefono»): il dito sul nastro
  dentro l'esploratore, le linee delle costellazioni su uno schermo vero, i nomi in maiuscolo con
  il carattere grande, il calendario, il velo delle nuvole in una notte che le ha, «Adesso» che
  avanza da solo, il telefono di lato.
- [ ] **«Costellazioni» accesa finché non la si spegne** (registro, «16.7», decisione 10). Lo step
  diceva «scelta ricordata» e non da dove parte; parte accesa. Da confermare dal committente
  guardando la pagina; tornare indietro è una riga (`AppSettings.skyConstellations`) e il suo test.
- [x] **Una notte bianca non ha un nastro** (registro, «16.7», «Trovato e non toccato»). Dove non
  c'è la notte astronomica (Stoccolma da maggio ad agosto, Londra fra fine maggio e metà luglio)
  l'esploratore ha un'ora sola, come l'hero. Fatto l'11 nov 2026 con la proposta com'era, per
  l'esploratore soltanto: lì il nastro va dal tramonto all'alba (registro, «La notte bianca ha un
  nastro»). **Scritto senza compilare e senza lanciare i test: la CI è la prima a provarlo.**
- [ ] **La notte bianca sul telefono** (registro, «La notte bianca ha un nastro»): il nastro di un
  luogo a nord in una data d'estate (Stoccolma, 23 giugno, dal calendario), i suoi colori, la
  frase sotto, l'ora che si muove.
- [ ] **16.8** L'esploratore: toccare un oggetto (Fase 16).
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

Riletto il 10 nov 2026, all'archivio 11, all'inizio del 16.3: le caselle nate dal 2 nov 2026 rimandano all'archivio 11 (`docs/planning/11-novembre-2-10.md`), dove sta anche la descrizione degli step della Fase 16 (16.3–16.11 aperti: «Fase 16», «Gli step»); restano aperte le prove sul telefono (la revisione di Oggi, l'hero, 16.1, 16.2), il peso della cifra da confermare, la trasmissione di Gadgetbridge decisa e da fare, 14.12 in attesa e il cielo d'inverno contro il satellite; una proposta non decisa, la nebbia, il temporale e la meteora nella finestra e nell'hero del Cielo. Riletto il 2 nov 2026, all'archivio 10, con la 2.0.0: le caselle chiuse dal 29 ott 2026 (15.13, 15.14) rimandano all'archivio 10 (`docs/planning/10-verso-2.0.0.md`); restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, dopo la rilettura della Fase 15: aperte 15.13 e 15.14, la coda; e come prima 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, all'archivio 09: le caselle della Fase 15 sono tutte chiuse (in `docs/planning/09-ottobre-28-29.md`), restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 28 ott 2026, all'archivio 08, lo stesso giorno dell'apertura della Fase 15: le caselle sopra sono della Fase 14 (14.12 in attesa) e della Fase 15 (in `docs/planning/08-ottobre-27-28.md`), nessuna proposta non decisa. Riletto il 27 ott 2026, all'archivio 07: le caselle della Fase 14, nessuna proposta non decisa. Fino al 26 ott 2026 nessuna casella aperta (riletto il 22 ott 2026, all'archivio 06):
le ultime chiuse quel giorno stanno in `docs/planning/06-ottobre-20-22.md`, «Cielo: la finestra
reale nella card Stanotte», «Il widget «Momenti del cielo» va via», «Le notifiche: ogni riga nella
forma che sta in una riga», «Il widget «Le prossime ore» va via».

---

## Registro dal 10 nov 2026, dopo l'archivio 11

## 16.3 — Il motore: Marte e Saturno, e quando un oggetto sorge, culmina e tramonta (committente, 10 nov 2026)

Il terzo step della Fase 16 (archivio 11, «Fase 16 — Cielo: la pagina come Oggi e il cielo da
esplorare», «Gli step»), da `origin/main` dopo l'unione del 16.2 (PR #154). Prima dello step,
come commit a sé, **l'archivio 11**: il registro era a 1 018 righe e la regola della fase lo
archivia all'inizio dello step che lo trova oltre le mille.

### Cosa è cambiato

- **I pianeti sono quattro.** `Planet` è `VENUS, JUPITER, MARS, SATURN`, nell'ordine in cui
  l'occhio li trova (i due che nessuno deve farsi insegnare, poi i due che si cercano): i
  primi due tengono il posto che avevano, e l'ordine è anche la priorità dei nomi sulla
  finestra. Gli elementi di Marte e Saturno sono la Tabella 1 del JPL («Approximate Positions
  of the Planets», 1800–2050), la stessa da cui vengono Venere e Giove, **riletta alla fonte
  il 10 nov 2026** cifra per cifra. `PlanetMath` dice perché sono entrati e perché Mercurio
  resta fuori.
- **La trappola è tolta alla radice**, non con un ramo in più: l'enum privato `Body` di
  `AstronomyEngine` (sole, luna, centro galattico, Venere, Giove) è un tipo sigillato in cui un
  pianeta porta con sé quale pianeta è (`Body.Wanderer(planet)`) e un punto fisso le sue
  coordinate (`Body.Fixed`): non c'è più una tabella da `Planet` a `Body` che possa restare
  indietro. `planetAbove` mandava su Giove ogni pianeta che non era Venere; un test lo tiene
  (la notte del 10 novembre a Milano Saturno è su dal crepuscolo e Giove sorge a mezzanotte).
- **`SkyBody`** ha `MARS` e `SATURN`, e ogni voce dice che pianeta è (`planet`, nullo per la
  luna): `separation`, `bodyAltitude` e `bodyAzimuth` non hanno più un ramo per corpo. Le
  coppie con una riga restano le tre di `SkyScheduler.ConjunctionJobs`.
- **Il passaggio di un oggetto in una notte**: `AstronomyEngine.passage(target, night, coords)`
  dà `SkyPassage`, sorgere, punto più alto e tramonto, ognuno con l'ora, l'altezza e la
  direzione (`SkyPassagePoint`). `SkyTarget` è la luna, un pianeta o un punto fisso del cielo
  (una stella, un radiante, il centro di una costellazione, per ascensione retta e
  declinazione J2000 come `starHorizontal`). `horizontal(target, at, coords)` dà dove sta a
  un'ora.
- **«Fino a quando conviene»**: `watchRuns(target, dark, coords, minAltitudeDeg)` dà i tratti
  del buio in cui l'oggetto sta sopra una certa altezza, in ordine; `worthUntil` è la fine
  dell'ultimo, nulla se non ci arriva mai, la fine del buio se l'oggetto dura più della notte.
- **La luminosità di un pianeta**: `planetMagnitude(planet, at)`, le formule dell'Astronomical
  Almanac come le stampa Meeus (cap. 41), con i due termini degli anelli per Saturno (cap. 45).
  Serve a disegnare, non si stampa.
- **`SkySights.minAltitude(planet)`** è pubblica e ha un ramo per pianeta: 5° Venere, 10° gli
  altri tre. Era un `if` con Giove nel ramo «altrimenti».
- **Nell'app**: `StarField.planets` disegna i quattro (girava già su `Planet.values()`): Marte
  e Saturno con il disco a 1,1 volte quello di una stella, Marte caldo 0,95, Saturno 0,7, e
  **luminosi quanto lo sono quella notte** (`planetBrightness`); la finestra li nomina
  («Marte», «Saturno»; `window_label_mars`, `window_label_saturn`). La guida del Cielo dice
  «Venere, Giove, Marte e Saturno» dove diceva «Venere e Giove», e la pagina «La luna accanto
  a Giove» non dice più «l'altro pianeta che quest'app conosce per nome». DESIGN §3.9, VISION
  §5.9 (la finestra), CHANGELOG.

### Decisioni e deviazioni

1. **Quale passaggio è «quello della notte».** Un oggetto passa in cielo una volta al giorno
   e una notte non è un giorno: una stella della sera è sorta la mattina, una del mattino
   tramonta il pomeriggio dopo. Il passaggio è quello il cui **punto più alto è il più vicino
   alla metà della notte**, con il sorgere prima e il tramonto dopo, dovunque cadano
   sull'orologio, anche di giorno. Saturno stasera a Milano è sorto alle 15:34, di pomeriggio,
   ed è la risposta vera a «quando è sorto». Si cerca entro tredici ore dalla metà della notte
   (il giro più lungo è quello della luna, 25 h 10 min) ed entro tredici ore dal culmine.
2. **L'orizzonte è quello dell'almanacco.** Un punto di luce sorge quando la rifrazione lo ha
   già alzato: 34' sotto l'orizzonte (`POINT_RISE_ALTITUDE`). `riseThreshold` dava 0° ai
   pianeti e al centro galattico, e nessuno lo leggeva (i loro intervalli hanno sempre
   un'altezza esplicita): il numero è corretto senza che niente a schermo cambi. La luna tiene
   la sua soglia, quella di `lunarDay`.
3. **Il culmine è il punto più alto, non il passaggio al meridiano.** Per la luna differiscono
   di due o tre minuti, perché si sposta in declinazione mentre passa; la tavola 4 dice «Più
   alto». L'altezza della luna è quella del motore, dal centro della Terra, fino a un grado
   sopra quella che si vede da un luogo: la stessa a cui ogni luna dell'app è disegnata.
4. **Totale come il resto del motore.** Un oggetto che non sorge o non tramonta entro mezza
   giornata dal culmine ha un nullo lì; con tutti e due nulli è sempre su (`alwaysUp`) o, se
   anche il culmine è sotto l'orizzonte, mai (`neverUp`). Ai poli, dove una stella non ha
   culmine, la risposta è comunque un istante.
5. **Deviazione: la luminosità segue la magnitudine.** Lo step diceva «la grandezza e il
   calore di Marte e Saturno si aggiungono lì» e la luminosità di un pianeta era fissa in cima
   alla scala. Per Venere e Giove è vero ogni notte (la scala finisce a Sirio, −1,5; il minimo
   di Giove è −1,6); Marte va da sopra Giove (−2,9) a un punto come le stelle del Carro (+1,8)
   e Saturno da −0,5 a +1,2 con gli anelli di taglio: disegnati in cima alla scala sarebbero,
   quasi ogni notte, una luce che il lettore alza gli occhi e non trova. Da qui
   `planetMagnitude`. **Non è gergo a schermo**: il numero non si stampa, sceglie quanto è
   grande un punto.
6. **Deviazione: lo step tocca `:app`.** Scriveva «solo `:core:domain`, niente a schermo» e
   nella stessa descrizione «l'hero di Oggi, la finestra e la card li mostrano da questo step»:
   vale la seconda. I `when` su `Planet` di `StarField` e `WindowText` sono esaustivi e non
   compilano senza i due rami; con i rami servono due stringhe e, perché la guida non menta,
   due frasi. Niente lavori nuovi nel catalogo (arrivano con il 16.8), niente schermata nuova.
7. **Un tipo solo per «una cosa in cielo».** `SkyTarget` invece di una funzione per la luna,
   una per i pianeti e una per le stelle: l'elenco «Da vedere» del 16.4 e il tocco del 16.8
   hanno oggetti di tutti e tre i tipi nella stessa lista.
8. **Sette primi d'arco su Saturno sono accettati.** Gli elementi medi non portano la grande
   disuguaglianza fra Giove e Saturno: per questa tabella il JPL dichiara 600″ in longitudine
   su Saturno e 400″ su Giove (misurati qui 407″ e 149″), e l'altra sua tabella, quella lunga
   con i termini in più (3000 a.C.–3000 d.C.), ne dichiara 1000″ e 600″. Sette primi sono meno
   di un dp di qualunque tela e mezzo minuto sull'ora del sorgere, che contro l'effemeride sta
   comunque nel minuto.
9. **`aboveAltitude` passa da `runsAtOrAbove`**: la stessa caccia, tenuta fino in fondo
   all'intervallo; il primo tratto è la risposta di prima, con gli stessi confronti.
   `belowThresholdRuns` (la luna sotto l'orizzonte) non è toccata.

### Come è stato verificato

- **Contro JPL Horizons, letto il 10 nov 2026** (`ssd.jpl.nasa.gov/api/horizons.api`), con i
  numeri scritti nei test e la data di lettura, come vuole la regola della fase.
  `PlanetEphemerisTest`: i quattro pianeti a undici istanti dal 2026 al 2039, posizione
  apparente geocentrica e magnitudine. Lo scarto peggiore sul cielo: **Venere 0,015°, Marte
  0,019°, Giove 0,042°, Saturno 0,113°**; sulla magnitudine Venere 0,24, Saturno 0,15, Marte
  0,12, Giove 0,03 (le righe di Saturno vanno dagli anelli di taglio del 2026 e del 2039 a
  quelli aperti del 2032). `SkyPassageTest`: sorgere, culmine e tramonto di Venere, Marte,
  Giove, Saturno e della luna a Milano nella notte del 10 novembre 2026, e di Marte (in
  opposizione), Saturno e della luna a Sydney in quella del 19 febbraio 2027, dove tutto
  culmina a nord: **ogni orario entro un minuto** da Horizons, che stampa il minuto, la luna
  compresa; le direzioni entro 0,6°, le altezze entro 0,3° (la luna entro 1,2°, la parallasse).
- **Un punto fisso è geometria**: Sirio da Milano culmina a 90° − latitudine + declinazione,
  sorge e tramonta alla stessa distanza dal culmine, e quella distanza è l'angolo orario
  dell'orizzonte dell'almanacco al passo delle stelle, entro 5 secondi; la Polare è sempre su,
  Canopo mai, la Croce del Sud sempre su da Sydney; al polo la funzione risponde lo stesso.
- **Gli invarianti di `PlanetMathTest` estesi ai due nuovi**: l'opposizione, il periodo
  sinodico (779,94 e 378,09 giorni; Marte camminato fino al 2041, perché da una congiunzione
  alla successiva va da 764 a 811 giorni), l'anno (686,98 e 10 759 giorni, con i tre giorni e
  mezzo che la tabella toglie a Saturno detti nel test), l'orbita, le distanze dalla Terra.
- `StarFieldTest`: ogni pianeta dove il motore lo mette, fermo e più largo di una stella della
  sua luminosità; Marte in cima alla scala nell'opposizione del 2035 e sotto la metà nel 2032;
  Saturno dell'11 novembre a 0,5 (Horizons), fra Procione e Aldebaran; Venere e Giove a 1 in
  dodici anni di campioni. `WindowTextTest`: i due nomi.
- **Guardati**, perché un colore e una grandezza si decidono guardando: lo shader vero
  (`SkyShaderPixelTest`, «the skies a reader meets», che ora mette anche i pianeti nelle sue
  notti vere) ha disegnato Giove e Marte a una spanna l'uno dall'altro prima dell'alba del 15
  dicembre 2026 e Saturno da solo al meridiano la sera del 10 novembre. Giove è il disco
  bianco di prima; Marte, quella notte a +0,2, è un disco più piccolo di una tinta sabbia
  calda, la stessa che la scala dà alle stelle più rosse («una tinta, mai un colore saturo»,
  DESIGN §3.9); Saturno un punto fermo appena più largo delle stelle del suo rango.
- **Gli screenshot del README non cambiano.** Rigenerati tutti e quindici: tredici escono
  identici byte per byte. La sera della registrazione (25 set 2026, 21:00) Saturno è a 13° a
  est, fuori dalla finestra a sud di Oggi, e nel widget, basso sotto un cielo coperto al 61%,
  non sposta un pixel; Marte e Giove sono sotto l'orizzonte, Venere è tramontata. Le due immagini dei widget escono
  diverse **per un motivo che non è di questo step**, ed è finalmente spiegato (il 16.2 lo
  aveva lasciato senza): la finestra scrive l'ora del luogo accanto al nome quando l'orologio
  del luogo non è quello del telefono (`clockDiffers`), e il «telefono» del test è la macchina
  che lo lancia. Su una macchina con un altro fuso esce «Milan · 01:00», che è nelle immagini
  committate; su questa, che ha il fuso di Milano, no. Lasciate come su `main`; casella in «Aperto».
- La suite intera (`./gradlew test :app:testDebugUnitTest :app:lintDebug`, 3 044 test, 15
  saltati: gli screenshot) verde in locale, il lint senza errori.

### Cosa conferma solo il telefono

Casella in «Aperto». Marte e Saturno nell'hero di Oggi e nella finestra in una notte in cui ci
sono (Saturno la sera verso sud fino a gennaio, Marte e Giove insieme nella seconda metà della
notte in novembre e dicembre): la tinta di Marte accanto a Giove, se si legge «il pianeta rosso» o
chiede più colore di quanto la scala delle stelle dia (sarebbe una modifica dello shader, non
di questo step); i due nomi sulla finestra.

## 16.4 — Il motore: la notte di un'altra data, e cosa vale la pena guardare (committente, 10 nov 2026)

Il quarto step della Fase 16 (archivio 11, «Fase 16 — Cielo: la pagina come Oggi e il cielo da
esplorare», «Gli step»), da `origin/main` dopo l'unione del 16.3 (PR #155). Niente a schermo:
due pezzi di dato che l'esploratore consuma dal 16.7 e dal 16.9.

### Cosa è cambiato

- **La notte di una data.** `SkyStateBuilder.night(city, report, settings, date, now)` dà per
  la notte che comincia la sera di `date` lo stesso `Tonight` da cui è fatto l'hero: la notte
  astronomica, la finestra buia senza la luna, la luna di quella notte, il sole e la luna
  campionati ogni dieci minuti, il verdetto. L'astronomia si calcola per qualunque data; le
  nuvole sono della previsione, che è lunga una settimana.
- **Lo stato dice se la previsione ci arriva.** `Tonight` porta `forecast`, un `NightForecast`:
  `REACHES` (le ore di nuvole ci sono, fino all'alba), `BEYOND` (la notte finisce oltre
  l'ultima ora della previsione: nessuna ora, il cielo si disegna senza nuvole e lo schermo lo
  dirà), `PAST` (la notte è finita: nessuna ora e nessun verdetto), `NONE` (non c'è previsione).
  E porta `date`, la sera a cui la notte appartiene: una notte bianca non ha un crepuscolo da
  cui leggerla, e `nightDate` ora la legge da lì.
- **Un posto solo fa un `Tonight`** (`nightOf`): la notte di fuori e una notte scelta dal
  calendario sono la stessa cosa, e il test lo tiene (`build(...).tonight` è uguale a
  `night(...)` della sua data).
- **«Da vedere» come dato**, in `:core:domain`: `WorthSeeing.of(date, zone, coords, followed)`
  dà per una notte e un luogo le cose che meritano, ognuna un `Sight`: cos'è (`SightSubject`:
  la luna con la fase e la percentuale della notte, un pianeta, una figura, la Via Lattea, il
  radiante di uno sciame), l'ora migliore, dove sta a quell'ora (direzione e altezza in gradi,
  e `height`, la fascia che lo schermo dirà a parole), il tratto in cui vale la pena, e il
  `SkyTarget` verso cui girare la camera. In ordine d'ora.
- **Le figure** (`SkyFigures`): undici, ognuna con le stelle che la disegnano, prese dal
  catalogo che la tela disegna già. Cassiopea, le Pleiadi, le Iadi, Orione, i Gemelli, il
  Leone, il Grande Carro, la Croce del Sud, lo Scorpione, il Triangolo estivo, il Quadrato di
  Pegaso; ognuna con le tre lettere IAU della costellazione sotto cui il 16.5 archivierà linee e
  nomi.
- **L'altezza a parole** (`SkyHeight`): bassa sotto i 30°, a metà cielo fino a 60°, alta fino
  a 80°, sopra la testa oltre. Le stringhe arrivano con lo schermo (16.8, 16.9).
- `AstronomyEngine.galacticCore` è pubblico come `SkyTarget`: il nucleo della galassia è una
  cosa verso cui si gira la camera come le altre.

### Le regole di cosa entra, con il loro motivo

Scritte per esteso nel KDoc di `WorthSeeing`, come gli stati hanno le loro; qui in breve.

1. Una cosa è in elenco **finché il cielo è buio abbastanza per quel che è e lei sta sopra i
   tetti**; l'ora migliore è l'istante di quel tratto in cui è più alta.
2. **Quanto buio dipende da quanto è luminosa**: la luna, Venere e Giove dal sole a −6° (ognuno
   supera ogni stella, ogni notte); quel che è fatto di stelle dal sole a −12° (Marte e
   Saturno, le figure, un radiante); la Via Lattea a −18° e senza luna.
3. **La luna** a 5° o più e illuminata almeno per l'1% (sotto è nel bagliore del sole: la soglia
   della luce cinerea).
4. **Un pianeta** alla sua altezza minima (5° Venere, 10° gli altri) e **non più debole della
   magnitudine 1,5**: tocca solo Marte, che dall'altra parte dell'orbita è una stella come quelle
   del Carro. Il numero sceglie, non si stampa.
5. **Una figura** finché **ognuna delle sue stelle** è a 10°; **un ammasso** a 20°.
6. **La Via Lattea** quando la riga del catalogo ha una finestra quella notte (il nucleo a 10°
   nel buio senza luna).
7. **Un radiante** solo nella notte del picco, a 10° dentro la finestra dello sciame; uno
   sciame forte sempre, uno debole solo per chi lo segue (la regola della finestra).
8. **Un tratto sotto il quarto d'ora non è una riga.**

### Decisioni e deviazioni

1. **Le ore di nuvole: tutte o nessuna.** La previsione lascia la sua settima notte a
   mezzanotte. Lo step diceva «le ore di nuvole se la previsione ci arriva; altrimenti
   nessuna»: una notte in cui la previsione si ferma a metà **non è raggiunta** e non ha ore.
   Un nastro ingrigito per mezza notte e lasciato com'è per l'altra disegnerebbe la metà che
   nessuno ha previsto esattamente come un cielo sereno. Vale anche per l'hero di stanotte, che
   passa dallo stesso posto: gli succede solo con un report vecchio di sei giorni, che il
   verdetto rifiuta già per l'età. Tornare a «le ore che ci sono» è una riga (`nightOf`), da
   decidere guardando lo schermo del 16.7 se serve.
2. **Una notte in corso è raggiunta.** Il report non porta più le ore già passate
   (`WeatherRecency`): alle tre di notte le ore dalle 21 alle 2 mancano, e quel che resta
   della notte c'è tutto. Conta la fine, non l'inizio.
3. **Una notte finita non ha verdetto.** Un verdetto è una previsione e l'app non ha mai visto
   il cielo: per ieri notte la finestra buia resta (è un fatto), il verdetto e il tratto più
   sereno no. Serve alla «notte prima» del 16.7.
4. **Il verdetto resta del motore, e su una finestra coperta a metà giudica la metà che ha.**
   Trovato scrivendo i test: `SkyVerdictEngine` fa la media delle ore che trova nella finestra,
   e se la previsione finisce dentro la finestra giudica la parte che c'è. Per la settima
   notte questo mette un verdetto giudicato accanto a `forecast = BEYOND`. È la regola di ogni
   riga dell'app da sempre (uno sciame al sesto giorno), cambiarla cambia quel che si legge a
   schermo, e questo step non ha schermo: **non toccato**, casella in «Aperto».
5. **Le figure sono definite dalle loro stelle, non da un centro e un raggio.** «Tutta la
   figura sopra i tetti» si chiede alle stelle una per una, e il centro è la media delle loro
   direzioni (non delle coordinate: il Quadrato di Pegaso sta a cavallo delle 0h, e la media
   semplice di 346° e 2° cade nel Leone). È questa regola, non una tabella di latitudini, a
   tenere lo Scorpione fuori dall'elenco di Milano (il pungiglione culmina a 7°) e dentro
   quello di Roma, basso, per meno di tre ore di una notte d'estate.
6. **Lo Scorpione senza la curva della coda.** Con Sargas (43° sud) la figura sarebbe stata
   intera solo sotto i 37° di latitudine, cioè mai in Italia; la figura è «chele, cuore e
   pungiglione», e il pungiglione (Shaula) basta a dire che c'è tutto quel che si riconosce.
7. **Undici figure, e perché queste.** Una forma con un nome che la gente ha sentito, fatta di
   stelle più luminose della magnitudine 3,5 (quel che il cielo di una città mostra ancora), o
   un ammasso che si legge come una cosa sola; scelte perché ogni stagione dei due emisferi ne
   abbia. Fuori: il Cigno e la Lira (il Triangolo estivo è le loro stelle più luminose, e due
   righe per un pezzo di cielo sono una di troppo), l'Orsa Minore (in città ne resta la
   Polare), la Teiera del Sagittario (lì l'elenco ha già il nucleo della Via Lattea), le
   stelle singole (le nomina l'esploratore). Le 88 sono del 16.5, da disegnare; un elenco che
   mandasse fuori per la Lince riempirebbe righe perché può.
8. **La Via Lattea dell'elenco è il nucleo**, la riga che il catalogo ha già: così l'elenco e
   il momento «Via Lattea» di un lettore non possono dire due cose. Il resto della fascia è
   la metà debole, l'app non sa quanto è buio il cielo del lettore, e l'esploratore la disegna
   per chi vuole cercarla.
9. **L'anno di uno sciame comincia all'equinozio di marzo.** Trovato dal test: `resolve` per
   le Quadrantidi «del 2026» dà gennaio 2027, perché la longitudine del sole parte da zero a
   marzo. L'elenco chiede anche l'anno prima, come fa già `SkyScheduler.next`.
10. **Il radiante usa la finestra dello sciame com'è**, perché l'elenco e il promemoria del
    picco parlino delle stesse ore. Quella finestra si apre al picco quando il picco cade di
    notte: per le Geminidi del 2026 (picco alle 05:51 del 14 dicembre) è di sedici minuti,
    05:51–06:07, con il radiante già a ovest, mentre verso le due era a 77°. È la regola del
    lavoro dalla Fase 19 e non di questo step: casella in «Aperto».
11. **La soglia di Marte morde davvero**: su 362 notti campionate in dieci anni in cui Marte
    sta sopra i 10° al buio, in 53 è più debole di 1,5 (fra il luglio 2028 e il gennaio 2035).
    Venere, Giove e Saturno non ci arrivano mai (il test lo tiene su dieci anni).
12. **Il quarto d'ora è nato guardando l'elenco.** Lo step non lo chiedeva e la regola del
    modulo per la finestra buia è «nessuna soglia di durata». Ma il 15 aprile 2027 da Milano
    le Pleiadi stanno sopra i loro 20° per un secondo, mentre escono le stelle, e la riga
    c'era. La finestra buia resta senza soglia: quella è un fatto sulla notte, questo è un
    invito.
13. **Nessuna regola sulla luna che lava le altre.** Una figura di stelle di seconda
    grandezza regge la luna piena, e la sola vista che non la regge, la Via Lattea, aspetta
    già che tramonti.
14. **Dove la notte non è buia abbastanza le righe che vogliono il buio mancano**, senza una
    riga che lo dica (lo dice l'hero, con il motivo che ha già): a Londra in giugno ci sono le
    figure e non la Via Lattea, a Stoccolma solo la luna e i due pianeti brillanti, sotto il
    sole di mezzanotte niente. **Nella notte polare** il tratto va da un mezzogiorno
    all'altro: la notte che è tutto il giorno.
15. **L'elenco non entra nello stato della pagina.** Si calcola a richiesta, quando il foglio
    del 16.9 si apre: da 2 a 12 ms su questa macchina, e nessuno lo guarda finché non lo apre.
16. **`Tonight.date` è nullo solo in un valore fatto a mano** (i test ne costruiscono una
    ventina): il costruttore lo mette sempre.

### Come è stato verificato

- **Contro JPL Horizons, letto il 10 nov 2026** (`ssd.jpl.nasa.gov/api/horizons.api`, tabella
  dell'osservatore a Milano, rifratta, quantità 4, passi di un minuto), con i numeri scritti in
  `WorthSeeingTest`. Saturno, che culmina al buio, ha l'ora migliore al culmine (20:40 UTC,
  180,2° a 45,6°: la riga di `SkyPassageTest`). Marte, Giove e Venere, che all'alba dell'11
  novembre salgono ancora, hanno l'ora migliore alla fine del loro buio, e i bui sono due:
  Horizons segna il minuto in cui il sole passa i −12° (fra le 05:09 e le 05:10 UTC, dove
  l'elenco mette Marte, 161,7° a 58,8°) e i −6° (fra le 05:44 e le 05:45, dove mette Giove,
  175,9° a 58,2°, e Venere, 124,0° a 15,1°): ogni posizione entro un decimo di grado. La luna
  del 19 novembre: più alta alle 19:07 UTC per Horizons, alle 19:06 per l'elenco, 181,6° contro
  181,4°, 43,8° dal suolo contro 44,4° dal centro della Terra (la parallasse, la tolleranza che
  `SkyPassageTest` dà alla luna).
- **Le regole si richiedono al motore, non si ricordano**: la finestra del Grande Carro si apre
  quando la sua stella più bassa (Alkaid) arriva a 10°, e il test lo chiede alla stella; le
  Pleiadi a 20°; Cassiopea, che da Milano non scende mai così in basso, ha tutto il buio delle
  stelle. Lo Scorpione mai da Milano in un anno, da Roma basso per meno di tre ore, da Sydney
  sopra la testa. La Croce del Sud da Sydney e mai il Carro né Cassiopea. Il radiante di ogni
  sciame forte nella notte del suo picco e in nessun'altra delle sei intorno; quello delle
  Draconidi solo per chi le segue. La Via Lattea uguale alla riga del catalogo, e assente sotto
  la luna.
- **Quel che vale sempre**, su 28 notti del 2026 da Milano, Sydney, Quito e Tromsø: in ordine
  d'ora, una riga per cosa, l'ora migliore dentro il suo tratto, il tratto di almeno un quarto
  d'ora, la cosa sopra la sua soglia, e nessun istante del tratto in cui stia più in alto che
  all'ora migliore.
- `SkyFiguresTest`: ogni stella di ogni figura è una stella di `bright_stars.bin` entro tre
  centesimi di grado e più luminosa della soglia della tabella; il centro di ogni figura sta
  fra le sue stelle, quello del Quadrato a 354° e non a 174°.
- `SkyStateBuilderTest`: la notte dell'hero è la notte della sua data; domani ha la sua
  finestra, le sue ore e il suo verdetto; oltre la previsione ha il cielo e nessuna nuvola, e
  dice `BEYOND`; una notte in cui la previsione si ferma dentro non ha ore; una notte finita
  non ha ore né verdetto; senza report `NONE`; una notte bianca a Stoccolma tiene la data
  chiesta.
- **Guardato prima di fissare i test**: l'elenco stampato per sedici notti (sette da Milano
  in tutte le stagioni, Palermo, due da Sydney, Stoccolma e Londra al solstizio, Longyearbyen
  al sole di mezzanotte e nella notte polare, Tromsø, Quito). Da lì sono venuti il quarto d'ora, lo
  Scorpione senza Sargas e l'anno degli sciami.
- La suite intera (`./gradlew test :app:testDebugUnitTest :app:lintDebug`, 3 080 test, 15 saltati: gli screenshot) verde in locale, il lint senza errori.

### Cosa conferma solo il telefono

Niente: lo step non ha schermo. Gli screenshot del README non cambiano (nessuna immagine legge
quel che è stato toccato).

## Le prove sul telefono della Fase 16, da 16.1 a 16.3 (committente, 10 nov 2026)

Chiesto insieme al 16.4: «segna i punti da controllare sul telefono, della fase 16 già
implementati, come fatti». Chiuse in «Aperto» le tre caselle «16.1 sul telefono», «16.2 sul
telefono» e «16.3 sul telefono».

- **Niente da cambiare nel codice**: il committente non ha chiesto modifiche a nessuno dei tre
  step.
- **Le deviazioni che aspettavano la sua conferma restano come sono.** È la lettura di chi
  scrive, non una frase del committente: chiudere la casella senza chiedere di tornare indietro
  è la conferma, e se non lo era la casella si riapre. Sono: nel 16.1 le parole dell'hero sul
  colore di fondo del cielo e non sopra le stelle (la deviazione dalla tavola 1, misurata
  nella sua sezione); nel 16.2 le icone del Cielo a 34dp, che erano a 51 per scelta del
  committente (decisione 3), i gruppi «Oggi» e «Domani» al posto di «Stanotte e domattina»,
  «Adesso» una volta sola; nel 16.3 la luminosità di Marte e Saturno che segue la magnitudine
  (decisione 5) e la tinta sabbia di Marte, senza una modifica dello shader.
- **Restano aperte** le due prove nate prima della fase, che non sono della Fase 16: «La
  revisione di Oggi e del cielo sul telefono» (la cifra della GPU, il nastro sotto il dito, la
  nebbia e il lampo) e «L'hero dopo la revisione, sul telefono».

## 16.5 — Le costellazioni: il dato (committente, 10 nov 2026)

Il quinto step della Fase 16 (archivio 11, «Fase 16 — Cielo: la pagina come Oggi e il cielo da
esplorare», «Gli step»), da `origin/main` dopo l'unione del 16.4 (PR #156). Niente nel cielo
disegnato: il dato che l'esploratore consuma dal 16.6 (i nomi delle stelle), dal 16.7 (le
linee e i nomi delle costellazioni) e dal 16.8 (il nome da toccare). Nella stessa PR, chieste
insieme, le due caselle nate dal 16.4: la sezione dopo questa.

### La fonte delle linee, e la sua licenza

Lette alla fonte il 10 nov 2026, le due candidate dello step.

- **d3-celestial** di Olaf Frohn (`github.com/ofrohn/d3-celestial`): **BSD a 3 clausole**
  («Copyright (c) 2015, Olaf Frohn», il file `LICENSE`; il readme chiude con «Released under BSD
  License»). `data/constellations.lines.json` sono le linee come coordinate J2000, una
  `MultiLineString` per costellazione; `data/constellations.json` il punto dove scrivere ogni
  nome, i nomi in venti lingue e un rango da 1 a 3. Il readme dichiara la provenienza: la
  pagina delle costellazioni dell'IAU, «name positions and some line modifications by me».
- **Stellarium, cultura «moderna»** (`github.com/Stellarium/stellarium-skycultures`,
  `western/`): la riga della licenza in `description.md` è «Text and data: CC BY-SA», **senza
  versione** (le illustrazioni: Free Art License). Le linee di `index.json` sono numeri
  Hipparcos, non coordinate.

**Scelta d3-celestial**, per due motivi. La licenza: la BSD a 3 clausole entra in un'app
GPL-3 senza niente da decidere (si tiene la nota); della CC BY-SA solo la 4.0 può essere
portata in GPL-3, e una riga che non dice quale versione non è una licenza su cui costruire.
Il dato: le linee di Stellarium vogliono un secondo catalogo (Hipparcos) per diventare
posizioni, quelle di d3-celestial sono già posizioni, e ognuna cade su una stella del catalogo
che la tela disegna (decisione 2).

### Cosa è cambiato

- **`tools/gen_constellations.py`** scrive
  `core/domain/src/main/resources/.../sky/constellations.bin` accanto a `bright_stars.bin`:
  88 costellazioni, 743 segmenti, 89 punti dei nomi, 6 924 byte. La fonte è fissata al commit
  `7e720a3` (il `master` di quel giorno, fermo dal 5 lug 2022). Il formato è nel docstring:
  «CHCN», la versione, il conteggio, e per costellazione la sigla IAU, il rango, i punti del
  nome, i segmenti come coppie di punti; un punto ha i due campi a 16 bit di una stella di
  `bright_stars.bin`.
- **`Constellations`** (`:core:domain`, Kotlin puro) la rilegge, una volta, al primo uso:
  `all` (le 88 nell'ordine dei nomi latini), `byId` (la sigla IAU con le sue maiuscole), e per
  ognuna `Constellation`: `id`, `rank`, `labels` (dove scrivere il nome, come `SkyTarget.Fixed`:
  il 16.8 ci gira la camera), `lines` (quattro numeri a segmento). Cercata per percorso
  assoluto, come il catalogo delle stelle (R8 sposta la classe e non la risorsa).
- **I nomi delle 88**, IT/EN, sono stringhe dell'app (`constellation_and` … `constellation_vul`)
  e `ConstellationNames` (`:app`, `ui/sky`) le dà per sigla.
- **Le stelle con un nome sono 31** (`NamedStars`): alle diciotto si aggiungono Alfa Centauri,
  Hadar, Acrux, Mimosa, Adhara, Shaula, Gacrux, Bellatrix, Elnath, Miaplacidus, Alnilam,
  Alnair e la Stella Polare. Sono le trenta più luminose di tutto il cielo, che finiscono ad
  Alnair, più la Polare. **La finestra resta alle sue diciotto** (`WindowStars`).
- **I crediti**: una riga in Impostazioni → Dati e riconoscimenti («Linee delle costellazioni
  · d3-celestial di Olaf Frohn, BSD a 3 clausole, adattate per Chiaro», che apre la fonte), la
  nota della licenza in `licenses/d3-celestial-BSD-3-Clause.txt`, la riga in
  `licenses/README.md` e una frase nel README.
- `tools/gen_bright_stars.py` espone le due formule con cui scrive una posizione (`ra_field`,
  `dec_field`), perché le linee le usino tali e quali; rilanciato, il binario delle stelle
  esce identico byte per byte.

### Decisioni e deviazioni

1. **I nomi non vengono dalla fonte.** d3-celestial ha «Big Dipper» per l'Orsa Maggiore e
   «Little Dipper» per la Minore, «Compass» sia per il Compasso sia per la Bussola, «Cavallo»
   per il Cavallino, «Ofiucho», «Idra femmina», «Bulino dello Incisore». Dalla fonte si prendono
   le linee e i punti; i nomi sono dell'app, come quelli delle stelle.
2. **Ogni estremo di una linea è una stella che la tela disegna, bit per bit.** Misurato prima
   di scrivere lo strumento: tutti i 767 vertici della fonte stanno entro 0,0025° da una stella
   del catalogo di Yale. Lo strumento sposta ogni vertice sulla stella su cui sta (la più
   luminosa entro un centesimo di grado fra quelle di `bright_stars.bin`) e la scrive con gli
   stessi campi: una linea finisce su un punto disegnato, non accanto. Due vertici stavano
   sulla metà debole di una doppia (κ Bootis, ι Cancri, magnitudine 6,7 e 6,6, fuori dal
   catalogo dell'app) e passano alla metà che si vede, a 0,004° e 0,008°. Lo strumento si ferma
   se un vertice non trova una stella.
3. **In inglese il nome è quello latino, in italiano quello italiano.** Ogni carta in inglese
   stampa Ursa Major, Cygnus, Gemini, e sono i nomi che un lettore inglese ha sentito;
   l'italiano ha un nome suo per ognuna (Orsa Maggiore, Cigno, Gemelli). Non è gergo: è il
   nome. Rivedibile senza toccare il dato: sono 88 stringhe.
4. **Norma è «Squadra», non «Regolo».** In italiano ha tutti e due i nomi, e Regolo in questo
   cielo è già una stella del Leone. Idra e Idra Maschio restano i due nomi che hanno.
5. **Il Serpente è una costellazione con due punti del nome.** La fonte lo dà in due voci
   (testa e coda, ai lati dell'Ofiuco); qui è una, come nell'elenco dell'IAU, con le linee di
   tutti e due i pezzi e il nome scritto su ognuno. Le altre 87 hanno un punto.
6. **Deviazione: il rango entra nella risorsa.** Lo step chiedeva le linee e il punto del
   nome. La fonte dà anche un rango (22 al primo, 24 al secondo, 42 al terzo): un cielo largo
   tutto lo schermo non regge 88 nomi, e il 16.7 avrebbe dovuto inventarne uno. Costa un byte
   a costellazione; ordina, non si stampa.
7. **Le linee sono le 743 della fonte, tutte.** Nessun tratto doppio e nessuno fra le due metà
   di una doppia (lo strumento li toglierebbe, e non ne ha trovati). La Croce del Sud sono due
   linee che si incrociano senza una stella in comune; la Gru è in due pezzi nella fonte e
   resta così. Il tratto più lungo è nella Carena, 25,6°.
8. **Trentuno stelle, e perché queste.** «Le più luminose, una trentina»: le trenta più
   luminose a occhio nudo (le due metà di una doppia insieme: Alfa Centauri, Acrux, Castore),
   che si fermano ad Alnair (1,74; la successiva è Alioth, 1,77), più la Polare, che è circa
   la cinquantesima ed è la sola che un lettore cerca per nome. Le posizioni sono quelle del
   catalogo di Yale, e il test le trova lì.
9. **Deviazione: la finestra non usa i nomi nuovi.** Scrive due nomi al massimo, «i più
   luminosi con un nome che un lettore ha sentito» (20 ott 2026), e lo step non ha schermo:
   con la lista lunga avrebbe scritto «Bellatrix» o «Elnath» dove oggi non scrive niente. I
   tredici sono dell'esploratore. La Polare e Alfa Centauri sulla finestra sarebbero una
   scelta del committente, non di questo step.
10. **I nomi in `:app`, il dato in `:core:domain`.** Il dominio è Kotlin puro e non ha
    stringhe: come `SkyFigure`, una costellazione porta la sigla e il nome sta dove c'è lo
    schermo. Le sigle di `SkyFigure.constellation` (16.4) sono le stesse, e un test lo tiene.
11. **La riga dei crediti c'è da questo step, prima delle linee.** Il file entra nell'APK da
    ora, e la BSD chiede la nota dove si distribuisce il binario. «Adattate per Chiaro» per lo
    stesso motivo delle icone: gli estremi sono stati spostati sulle stelle.
12. **Il catalogo delle stelle non ha una riga in Impostazioni.** È di pubblico dominio e non
    chiede niente; `licenses/README.md` ora lo dice, dove prima non era nominato.
13. **DESIGN, la guida e il CHANGELOG non cambiano per questo step.** Il cielo disegna quel che
    disegnava; DESIGN §3.9 e la guida dicono le costellazioni nello step che le disegna (16.7).

### Come è stato verificato

- `ConstellationsTest` (`:core:domain`): l'elenco è quello dell'IAU, scritto nel test e non
  letto dal file, le 88 una volta ciascuna; **ogni estremo di ogni linea è una posizione di
  `BrightStars`, bit per bit** (1 486 estremi); nessuna linea di lunghezza zero, nessuna due
  volte, nessuna oltre i 26°; il rango fra 1 e 3 con Orione, l'Orsa Maggiore e lo Scorpione al
  primo; un punto del nome a costellazione, due per il Serpente, ognuno entro 8° dalle sue
  linee (il più lontano è quello della Poppa, 7,9°); le forme che un lettore conosce unite come
  le conosce (il Grande Carro stella per stella con la coppa chiusa, la W, la cintura di
  Orione, i due bracci della Croce, la coda dell'Orsa Minore sulla Polare, Alpheratz estremo
  sia di Andromeda sia di Pegaso); ogni figura di «Da vedere» archiviata sotto una
  costellazione che esiste, e le stelle di una figura che è una costellazione intera sono
  estremi delle sue linee.
- `ConstellationResourceTest`: il file si trova per percorso assoluto da un altro pacchetto e
  non per nome relativo (la trappola di R8 del 20 ott 2026), sta accanto alle stelle e ha la
  sua firma.
- `ConstellationNamesTest` (`:app`, nelle due lingue): un nome per ognuna delle 88 del motore
  e per nient'altro, 88 nomi diversi in inglese e 88 in italiano, «Squadra» accanto a «Regolo»
  la stella, e i nomi che la guida e il 16.4 dicono già (Cassiopea, Orione, Leone, Scorpione).
- `StarNamesTest`: 31 nomi, in ordine di luminosità, ognuno su una stella del catalogo; ogni
  stella del catalogo più luminosa di 1,75 ha un nome; la finestra ha le sue diciotto.
  `SettingsScreenTest`: la riga dei crediti c'è.
- **Lo strumento rilanciato dà lo stesso file**, e `gen_bright_stars.py` dopo la modifica dà
  lo stesso `bright_stars.bin`.
- La suite intera, il lint e la build minificata: in fondo alla sezione dopo questa, una
  volta sola per tutta la PR.

### Cosa conferma solo il telefono

Casella in «Aperto». La riga «Linee delle costellazioni» in Impostazioni → Dati e
riconoscimenti, e che apra la pagina della fonte. Nient'altro: lo step non disegna niente.
Gli screenshot del README non cambiano per questo step (quello delle Impostazioni mostra
l'Aspetto, non i crediti).

## Le due caselle del 16.4: la finestra coperta a metà, e la notte dello sciame (committente, 10 nov 2026)

Chiesto insieme al 16.5: «guarda queste caselle aperte in planning e implementa con la tua
proposta se lo ritieni fattibile». Fattibili tutte e due, con la proposta scritta nella
casella: sono due regole del motore con il loro test, e tutti gli schermi le leggono dallo
stesso posto. Cambiano quel che si legge a schermo, a differenza del 16.5.

### La finestra coperta a metà

- **La regola.** `SkyVerdictEngine.evaluate` risponde «Presto per dirlo», con il motivo «la
  previsione non arriva ancora così lontano» (`UNKNOWN`, `BEYOND_HORIZON`), quando l'ultima
  ora della previsione finisce prima della fine della finestra. Prima faceva la media delle
  ore che trovava e la stampava come verdetto dell'intera finestra: la previsione lascia la
  sua settima notte a mezzanotte, e «Bello, nuvole 10%» su quella notte era il numero della
  sera sotto il nome di una notte di cui nessuno aveva previsto le ore piccole.
- **Conta solo la fine.** Una finestra in corso ha perso le prime ore perché sono passate
  (`WeatherRecency` le toglie), e quel che ne resta c'è tutto: resta giudicata. È la regola
  con cui `SkyStateBuilder.nightForecast` legge una notte (16.4, decisione 2), sulle stesse
  ore: l'ultima ora è un secchio lungo un'ora, e una finestra che si chiude dentro, o sul suo
  bordo, è coperta. Ora `forecast = BEYOND` e il verdetto dicono la stessa cosa.
- **Il tratto più sereno segue.** `clearStretch` è nullo quando la previsione si ferma dentro
  la finestra: un tratto che finisce dove finisce la previsione stamperebbe il bordo della
  previsione come l'ora in cui il cielo si chiude.
- **Un buco in mezzo alla previsione** resta com'era (`NO_COVERAGE` solo se la finestra non ha
  nessuna ora): la casella era sul bordo, e un buco in mezzo non è mai stato visto dal vivo.
- **Chi lo legge**: ogni posto che giudica passa da lì (le righe e l'hero del Cielo, l'agenda
  di Oggi, il widget, il promemoria). A schermo cambia la riga al bordo della settimana: la
  notte buia del settimo giorno, uno sciame la cui notte la previsione lascia a metà.
  «Presto per dirlo» in «In arrivo» è la nota in fondo alla sezione, come già per quel che è
  oltre la previsione. La guida diceva già «quando la previsione non arriva così lontano…
  un grigio "presto per dirlo"»: ora è vero anche sul bordo.

### La notte dello sciame

- **La regola.** La finestra di uno sciame è la notte del suo picco, intera, dal crepuscolo
  astronomico all'alba (`SkyScheduler.meteorPeak`). Prima, con il picco dentro la notte, si
  apriva al picco: «è quando il tasso è più alto, e non c'è motivo di mandare fuori nessuno
  prima». Un motivo c'è: l'istante è il centro di un massimo largo un giorno, le ore prima
  valgono quanto quelle dopo, e quel che cambia dentro una notte è quanto è alto il radiante.
  `MeteorShowerTable` lo diceva già nella sua prima regola: un picco è una notte, non un
  istante.
- **Le Geminidi del 2026**, il caso della casella: picco alle 05:51 del 14 dicembre da
  Milano. Era una finestra di sedici minuti, 05:51–06:07, datata 14 dicembre, con il radiante
  già a ovest. È la notte che comincia la sera del 13, e nell'elenco «Da vedere» il radiante
  ha l'ora migliore verso le due, a 77,5° (90° − latitudine + declinazione: un punto fisso è
  geometria), con un tratto di più di dieci ore.
- **La data della riga è la sera.** «In arrivo» scrive la data in cui la finestra comincia:
  per un picco nelle ore piccole era la mattina dopo, ora è la sera in cui si esce.
- **Il promemoria** arriva all'imbrunire della notte del picco, con l'anticipo scelto, e non
  più prima dell'alba («Tra 30 minuti, alle 05:51»).
- **Due sciami nella stessa notte condividono la riga** anche quando i picchi non sono lo
  stesso istante: la riga si unisce per inizio della finestra, che ora è il crepuscolo. È
  quel che la guida dice («due sciami che culminano la stessa notte ne condividono una»).
- **Il radiante sul cielo disegnato** (`WindowScene.peakRadiant`: l'hero di Oggi, la
  finestra, il widget) è nominato dal crepuscolo della notte del picco alla sua alba.
  `PeakAhead` scende da 14 a 4 ore: serviva a far arrivare la sera prima a un picco nelle ore
  piccole, cosa che ora fa la finestra; con la notte intera, quattordici ore avrebbero
  nominato il radiante anche alla fine della notte prima. Quattro ore sono il crepuscolo: il
  cielo si disegna buio dalla fine di quello civile, fino a tre ore prima di quello
  astronomico in una sera d'estate a 55°.
- **Un picco di giorno** resta com'era: la notte più vicina, intera (la regola delle ore 12,
  `NIGHT_ENDS_HOUR`).

### Trovato e non toccato

- **Il verdetto del promemoria di una finestra è della sua prima ora.** `SkyAlarmReceiver`
  giudica l'istante in cui la finestra si apre (`end = null`), la riga del Cielo la finestra
  intera. Vale da sempre per la notte buia e per le ore d'oro; con la notte intera vale anche
  per uno sciame, dove prima l'istante giudicato era il picco. Casella in «Aperto», con la
  proposta.
- **Dal 1° gennaio lo sciame «prossimo» è quello dell'anno dopo.** Trovato controllando la
  notte intera sulle Quadrantidi, e c'era già: `SkyUpcoming.of` chiede a `SkyScheduler.resolve`
  l'anno di oggi, e l'«anno» di uno sciame comincia all'equinozio di marzo (16.4, decisione
  9). Misurato: il 2 gennaio 2027 a mezzogiorno le Quadrantidi «prossime» sono quelle del 3
  gennaio 2028, e nella notte del picco (alle 23 del 3 e alle 3 del 4 gennaio 2027) il cielo
  disegnato non ne nomina il radiante. `SkyScheduler.next` risponde giusto finché la finestra
  non è cominciata (parte da un anno prima), e «Da vedere» chiede già l'anno prima. Non è di
  questa PR; casella in «Aperto», con la proposta e la scadenza: **prima del 1° gennaio 2027**.

### Come è stato verificato

- `SkyVerdictEngineTest`: una finestra dalle 21 alle 5 con la previsione che finisce a
  mezzanotte è `UNKNOWN`/`BEYOND_HORIZON` e non porta nessun numero della metà che c'era; la
  stessa finestra chiusa a mezzanotte in punto, o alle 23:40, è giudicata; chiusa alle 00:01
  no; il tratto sereno che finirebbe sul bordo è nullo; una finestra in corso senza le prime
  ore è giudicata su quel che resta. `SkyStateBuilderTest`: la notte in cui la previsione si
  ferma dentro ha `BEYOND`, nessuna ora, il verdetto «oltre la previsione» e nessun tratto.
- `SkyNewJobsTest`: le Geminidi del 2026 vanno dal crepuscolo del 13 all'alba del 14, con il
  picco dentro, a meno di venti minuti dalla fine; ogni sciame, in tre anni (2026, 2027,
  2031), va dal crepuscolo di una sera all'alba dopo, e il picco è dentro o a meno di tredici
  ore. `WorthSeeingTest`: il radiante delle Geminidi è in elenco dalla sera, entro un'ora
  dall'uscita delle stelle, quando arriva a 10° (chiesto al motore), fino all'alba; l'ora
  migliore è alle due, a 77,5°, a sud. `WindowRadiantTest`: il radiante non c'è la mattina del
  13, c'è dalle 17:20 del 13 alle due del 14 (sopra i 70°), non c'è la sera del 14.
- **Gli screenshot del README non cambiano.** Rigenerati tutti e quindici: tredici identici
  byte per byte, i due dei widget diversi solo per l'ora «· 01:00» accanto al luogo, che
  dipende dal fuso della macchina (la casella aperta dal 16.3) e sono lasciati come su `main`.
  Nella sera della registrazione (25 set 2026) nessuno sciame ha il picco dentro la notte e
  nessuna riga sta sul bordo della previsione.
- **Quali sciami cambiano nel 2026** (stampato dal motore, da Milano): tre su tredici, quelli
  con il picco dentro la notte. Le Quadrantidi (picco alle 20:42 del 3 gennaio 2027: la
  finestra si apre alle 18:39), le Eta Aquaridi (01:58 del 6 maggio: dalle 22:37 del 5, e la
  riga è datata 5), le Geminidi (05:51 del 14 dicembre: dalle 18:27 del 13, datata 13). Gli
  altri dieci hanno il picco di giorno e avevano già la notte intera.
- **La suite intera, per tutta la PR** (`./gradlew test :app:testDebugUnitTest :app:lintDebug`,
  3 107 test, 15 saltati: gli screenshot) verde in locale, il lint senza errori. Con la stessa
  corsa la build minificata (`:app:assembleRelease -PsignReleaseWithDebugKey`): l'APK porta
  `com/callbackdev/chiaro/domain/sky/constellations.bin` accanto a `bright_stars.bin`, al
  percorso assoluto con cui `Constellations` lo cerca.

### Cosa conferma solo il telefono

Casella in «Aperto». La riga di uno sciame in «In arrivo» con la data della sera e, nella
notte del picco, in corso dal crepuscolo; il suo promemoria all'imbrunire; la riga della
notte al bordo della previsione senza verdetto e con la nota in fondo alla sezione.

## Le due correzioni trovate con le caselle (committente, 10 nov 2026)

Chiesto dopo la PR #157, con dieci minuti a disposizione: «se la correzione è rapida, non
eseguire la build dell'app e non eseguire i test; li esegue la CI». Nella stessa PR, un
commit a sé.

- **Un lavoro annuale la cui risposta cade nel gennaio dopo.** `SkyUpcoming.of` chiede prima
  l'anno scorso per i lavori annuali, e lo tiene finché non è finito: le Quadrantidi «del
  2026» (3 gennaio 2027) restano quelle davanti al lettore dal 1° gennaio fino alla loro
  alba, in corso durante la notte, e il cielo disegnato ne nomina il radiante. Vale per ogni
  lavoro annuale, l'alba più tarda compresa.
- **Il promemoria giudica la finestra intera.** `SkyScheduler.occurrenceAt(job, start, …)`
  ritrova la finestra che comincia all'istante che l'allarme porta (il giorno, il giorno
  prima, un anno prima; al minuto), e `SkyAlarmReceiver` passa la sua fine al verdetto: lo
  stesso che dà la riga del Cielo. Un istante non ha fine e resta com'era.
- **Non verificato in locale, per richiesta**: né la compilazione né i test
  (`SkyUpcomingAnnualTest`, `SkyOccurrenceAtTest`, scritti e non lanciati). I numeri del
  primo test sono quelli misurati prima della correzione con una sonda (registro, «Le due
  caselle del 16.4», «Trovato e non toccato»). Se la CI è rossa si corregge da lì; un test
  esistente che fissava il comportamento di prima su un lavoro annuale a gennaio sarebbe il
  primo sospetto.

## 16.6 — L'esploratore: la camera libera e il cielo a tutto schermo (committente, 11 nov 2026)

Il sesto step della Fase 16 (archivio 11, «Fase 16 — Cielo: la pagina come Oggi e il cielo da
esplorare», «Gli step»), da `origin/main` dopo l'unione del 16.5 (PR #157). Il primo
dell'esploratore: da qui il cielo si guarda tutto, dall'orizzonte allo zenit, girandolo con il
dito. È la tavola 3 senza il pannello del tempo, gli interruttori e l'elenco, che arrivano
con i loro step.

### Cosa è cambiato

- **`SkyCamera`** (`:app`, `ui/sky/explore`, Kotlin puro): dove guarda in direzione e in
  altezza, e quanto è larga (l'angolo che prende il **lato corto** della finestra, così
  girare il telefono non avvicina). Da cielo a schermo e ritorno (`SkyLens`), il
  trascinamento, il pizzico, il passo di una direzione, i due limiti, la camera con cui la
  pagina si apre, la magnitudine più debole che un campo disegna e quanto ingrandisce un
  punto di luce.
- **`SkyDome`**: il cielo intero di un istante da un luogo, ogni cosa come direzione est,
  nord, su: le 5 080 stelle del catalogo, i quattro pianeti, la luna, il sole, la griglia
  della Via Lattea, il radiante nella notte di picco, le stelle con un nome che sono sopra
  l'orizzonte. Calcolato una volta per l'istante; la camera poi ci gira sopra.
- **`SkyExploreScreen`** (`SkyExploreRoute`): la pagina a tutto schermo. Dal fondo in su: i
  colori del cielo, la Via Lattea, le stelle che il campo regge, i pianeti, la luna con la
  sua fase, il sole se è su, il suolo, la riga dell'orizzonte con le otto direzioni scritte
  sopra, i nomi. In alto la croce e «Verso sud-est»; in basso «Il cielo delle 23:00, senza
  le nuvole» e i due pulsanti.
- **La navigazione**: `SkyExploreKey(atEpochSecond, facingDeg)`, e `showsBottomBar` nasconde
  la barra (e il rail) anche per lei. Indietro la chiude.
- **La pillola «Esplora»** sull'hero del Cielo, sotto la riga del luogo, a destra (la tavola
  1, che il 16.1 aveva lasciato a questo step): consegna l'ora tenuta sul nastro e la
  direzione in cui guarda il palco.
- **Tre regole dello shader hanno un nome** (`starsShown`, `planetsShown`, `milkyWayShown`,
  con `moonLightOf` e `skyDarkness`, in `SkyShader.kt`): quanto si vedono le stelle, i
  pianeti e la Via Lattea. `skyScene` le chiama dove prima le scriveva, e l'esploratore
  chiama le stesse.
- **Tre ruoli di colore** in `SkyPalette` (`StarCool`, `StarWarm`, `MilkyWayGlow`): le tinte
  che lo shader mescola per le stesse cose, per una tela che non è uno shader.
- La guida (IT/EN) dice «Esplora» nel paragrafo di «Stanotte»; il CHANGELOG ha la sua voce;
  DESIGN §8.8d è la sezione dell'esploratore (con le righe in §3.9, §8.8b, §8.12 e §10).

### Decisioni e deviazioni

1. **La proiezione è stereografica**, centrata dove la camera guarda: una direzione a θ dal
   centro cade a `2·f·tan(θ/2)` dal mezzo dello schermo. Tre motivi, nell'ordine in cui
   hanno deciso. **Regge lo zenit**: non c'è niente in lei che sia un'altezza o una
   direzione, solo l'angolo da dove si guarda, e dritto in alto è una direzione come le
   altre (la camera delle stelle di oggi è un foro stenopeico le cui righe sono altezze, e
   non ha una riga per lo zenit). **Tiene le forme**: è conforme, e il Grande Carro al bordo
   di un campo largo è il Grande Carro, più grande; un foro stenopeico a 45° dall'asse
   raddoppia le lunghezze che puntano via dal centro. **Va oltre l'angolo retto**: un
   telefono dritto con 60° di larghezza ne ha più di 120 d'altezza, dall'orizzonte allo
   zenit in uno schermo.
2. **Niente rollio.** L'orizzonte è in piano nel mezzo dello schermo e lo zenit dritto
   sopra, sempre. È quel che definisce «su» anche allo zenit: l'asse destro della camera
   dipende solo dalla direzione, così guardando dritto in alto dopo aver guardato a sud il
   sud è ai piedi dello schermo e il nord in cima, come per chi si è piegato all'indietro.
3. **Il campo va da 15° a 90°, e si apre a 60°.** Sotto i 15° lo schermo sarebbe una dozzina
   di punti: il catalogo si ferma alla sesta magnitudine e l'app non ha niente di più debole
   su cui avvicinarsi (fuori i cataloghi di oggetti deboli, decisione della fase). Sopra i
   90° gli angoli dello schermo sono a più di 90° dal centro e la proiezione li stira più
   del doppio. 60° è la scala dell'hero: le stesse stelle per dp.
4. **Le stelle che un campo disegna**: tutte fino a 60°, poi `6 − 4·log10(campo / 60)`, 5,3
   al campo più largo. Il conto delle stelle triplica a ogni magnitudine e un campo largo il
   doppio tiene quattro volte il cielo: la regola tiene circa le stesse stelle per dp. Uno
   zoom che infittisse soltanto la polvere nasconderebbe le figure. Un punto di luce è
   disegnato più grande della radice quarta dello zoom (1,4 a 15°, 0,9 a 90°).
5. **Il trascinamento porta il cielo**: la direzione sotto il dito resta sotto il dito,
   esatta. Per una camera senza rollio c'è una forma chiusa: di traverso il posto di una
   direzione dipende solo dalla direzione della camera (`cos h · sin(A − az)`), quindi quella
   viene prima; l'altezza è il giro che resta. Dove non può (una stella vicino allo zenit
   non si porta di lato più della sua distanza dallo zenit; il pavimento; dritto in alto)
   il dito scivola sul cielo.
6. **Il pavimento: l'orizzonte a 0,82 dell'altezza**, che è anche come la pagina si apre.
   Dipende dal campo: 10° al più stretto, 60° al più largo, così **allargare il campo non fa
   entrare il suolo**. Deviazione dal primo calcolo, vista nella prima resa: con il limite a
   sette decimi il campo più largo aveva un terzo dello schermo di suolo, una forma scura
   con niente sopra. **Il 16.7 lo rivede**: il pannello in basso è più alto della riga e dei
   due pulsanti di oggi, e l'orizzonte deve stare sopra di lui.
7. **Ogni posto è del motore, e il tempo siderale è uno solo.** Le stelle sono girate dai
   loro posti di catalogo con una rotazione, e la rotazione è letta da
   `AstronomyEngine.starHorizontal` nei tre assi del sistema del catalogo: l'altezza e la
   direzione di un punto fisso sono, nel motore, un giro della sfera e nient'altro (niente
   rifrazione, niente precessione), quindi tre risposte danno tutto il giro. Misurato su
   tutte le 5 080 stelle, nei due emisferi e a sei mesi di distanza: lo scarto peggiore dal
   motore è sotto un decimillesimo di grado.
8. **Deviazione: la camera sta in `:app`, non in `:core:domain`.** È da cielo a schermo, e
   lo schermo è di `:app`; non ha Compose né Android, e i suoi test sono JUnit semplice.
9. **Deviazione: `SkyShader.kt` è toccato**, e lo step diceva che l'esploratore non passa
   dallo shader. Non ci passa: sono uscite tre regole con un nome, perché due schermi che
   decidono quante stelle si vedono con due copie della stessa formula smettono di essere
   d'accordo al primo ritocco. Quel che lo shader disegna non cambia (`SkySceneTest`, e le
   immagini del README, identiche).
10. **Le nuvole non ci sono, e la riga in fondo lo dice.** Lo step non le chiedeva (sono del
    16.7, «da decidere guardando») e non chiedeva la riga: ma un cielo sereno disegnato sopra
    una notte coperta, senza dirlo, è lo schermo che mente. «Il cielo delle 23:00, senza le
    nuvole» dice l'ora per cui è disegnato e cosa manca. Il pannello del 16.7 la sostituisce.
11. **L'istante è preso una volta, quando la pagina si apre**: l'ora tenuta sul nastro
    dell'hero se ce n'è una, altrimenti quella dell'hero (adesso se è notte, la metà della
    notte altrimenti). Non segue l'orologio: questo step non ha un tempo suo, e la riga dice
    di che ora è. La chiave di navigazione porta l'ora e la direzione, così una rotazione o
    il ritorno dopo che il sistema ha chiuso l'app ritrovano lo stesso cielo.
12. **Si disegna su una tela semplice.** La Via Lattea è il modello dello shader (una
    gaussiana lungo il piano galattico, più larga e più chiara verso il centro) come griglia
    di 120 × 9 direzioni riempita con una chiamata sola; le stelle sono dischi con il
    raggio, la luminosità e la tinta dell'hero, e dalla seconda magnitudine in su portano
    un alone di tre dischi tenui (un disco netto di 3dp si legge come un pulsante: visto
    nella prima resa, dove l'alone era un disco solo); la luna ha la fase girata verso il
    sole come lo vede il luogo, e la parte in ombra al 14% **sopra il cielo**, mai più scura
    del cielo (nella prima resa era un buco nero nel crepuscolo). Restano dello shader e non
    sono qui: i mari della luna, lo scintillio, la meteora.
13. **Disegna quando la camera cambia e basta**, come chiedeva lo step. Niente in questo
    cielo si muove da sé: per questo niente scintillio. Il costo è del dito: a ogni frame di
    un trascinamento, un prodotto scalare per stella e un cerchio per quelle a schermo
    (misurato in una notte buia di novembre da Milano su un telefono dritto: 812 stelle a
    schermo al campo d'apertura, 642 al più largo, 66 al più stretto; mai le migliaia del
    catalogo, e `SkyDomeTest` lo tiene).
14. **I nomi**: la luna con la sua fase, il radiante, i pianeti, e le stelle con un nome fino
    alla prima magnitudine al campo più largo (quattordici in tutto il cielo, dove una
    dozzina di nomi sarebbe un elenco e non un cielo) e tutte e trentuno da 45° in giù; la
    Polare a ogni campo. Sono i trentuno del 16.5, usati qui per la prima volta. Si posano
    con le regole della finestra (`WindowScene.place`): nessuno su un altro, né sulla parola
    di una direzione, né sotto quel che sta sopra il cielo. Un nome si scrive solo se il
    suo punto è disegnato abbastanza da essere quel che il nome indica.
15. **«Dritto sopra la testa» da 80° in su**, al posto di «Verso sud-est»: lì la direzione è
    il lato dei piedi dello schermo, non dove qualcuno sta guardando. 80° è dove comincia
    `SkyHeight.OVERHEAD` (16.4).
16. **Il sole è disegnato se è su.** L'istante dell'hero è di notte, tranne dove la notte
    non c'è (il sole di mezzanotte): lì il cielo ha il colore dell'ora, le stelle non ci
    sono perché non si vedono, e un disco dice perché.
17. **Quattro azioni per chi non può trascinare** (gira a sinistra, a destra, più in alto,
    più in basso), sul cielo, che al lettore di schermo dice la stessa riga scritta in alto.
    I due pulsanti sono il pizzico per una mano sola.
18. **Quel che la tavola 3 ha e questo step non disegna**: l'elenco «Da vedere» in alto a
    destra, i tre interruttori, il pannello. Arrivano con lo step che dà loro qualcosa da
    fare (16.7, 16.9, 16.10), come la pillola è arrivata con questo.
19. **La pillola c'è solo dove c'è un cielo da aprire**: un luogo con le coordinate. Senza,
    al suo posto c'è il distintivo della luna, e i due non si incontrano.
20. **VISION §5.3 e lo screenshot dell'esploratore restano al 16.11**, come dicono le regole
    comuni: fino al 16.7 la pagina cambia ancora faccia.

### Come è stato verificato

- `SkyCameraTest` (17, JUnit semplice): dove la camera guarda è il mezzo dello schermo,
  zenit compreso; schermo e cielo si rispondono; lo zenit è dritto sopra il mezzo e
  l'orizzonte davanti dritto sotto; a nord l'est è a destra, a sud a sinistra; dritto in
  alto il cielo è un anello intorno al mezzo con l'orizzonte guardato ai piedi; il lato
  corto prende il campo, in piedi e di lato; una forma al bordo di un campo largo resta la
  sua forma; quel che è dietro non ha posto; **il cielo sotto il dito resta sotto il dito**
  (mezzo pixel), anche vicino allo zenit; un trascinamento si ferma dritto in alto e al
  pavimento, e non gira la camera se è dritto; si apre con l'orizzonte a 0,82; un pizzico
  tiene fermo il cielo fra le dita; lo zoom si ferma ai due capi e allargando il suolo non
  entra; un passo gira, fa il giro a nord e si ferma ai capi; l'orizzonte è un cerchio con
  il cielo dentro; le due regole dello zoom.
- `SkyDomeTest` (8): **ogni stella del catalogo è dove la mette il motore** (20 320 confronti);
  la luna, i pianeti e il sole sono i suoi; hanno un nome solo le stelle sopra l'orizzonte
  (la Polare sempre da Milano, mai da Sydney); una notte buia mostra tutto, una luna piena
  toglie più di metà delle stelle e la Via Lattea, il giorno tutto; il radiante c'è nella
  notte delle Geminidi e non in un'altra; la griglia della Via Lattea sta sul piano
  galattico, è più chiara verso il centro, si chiude e finisce nel cielo; più stretto il
  campo, più deboli le stelle con il nome; un frame disegna qualche centinaio di stelle, e
  un campo più largo non ne disegna di più.
- `SkyExploreScreenTest` (8, attraverso la GPU come il palco dell'hero): si apre dove
  guardava l'hero, all'ora detta, e dice che le nuvole non ci sono; **la luna è disegnata
  dove la mette la camera**; **un falcetto della sera è illuminato dal lato del sole
  tramontato** (14 nov 2026, 25%: chiaro verso il sole, scuro dall'altra parte); un
  trascinamento gira il cielo, e tirandolo giù si guarda «dritto sopra la testa»; i due
  pulsanti avvicinano e allontanano e ognuno si ferma al suo capo; **disegna quando la
  camera cambia e in nessun altro momento** (mezzo minuto a guardare non chiede nessun
  ridisegno, un giro ne chiede uno, poi più niente); la croce la chiude; il cielo si gira
  senza dito.
- `TonightStageTest`: la pillola consegna la direzione del palco e l'ora tenuta sul nastro
  (nessuna quando è la notte intera), non c'è senza una destinazione né senza coordinate.
  `ChiaroNavigationStateTest`: l'esploratore copre la barra, indietro la rende, non la
  toglie a un'altra scheda, un tocco da fuori lo chiude, la chiave passa dallo stato salvato.
- **Guardato**, non solo provato: sei rese della pagina (una notte buia di novembre con
  Orione che sorge, la luna piena della registrazione, il falcetto, lo zenit, il campo più
  stretto e il più largo). Le tre correzioni scritte nelle decisioni 6 e 12 vengono da lì; e
  un difetto che nessun test avrebbe detto così in fretta: la prima resa non aveva né stelle
  né luna, perché un tracciato svuotato torna alla regola di riempimento di default e il
  suolo (il rettangolo meno il cerchio dell'orizzonte) copriva tutto il cielo.
- **`sky-tonight.png` rigenerato e guardato** (la pillola sotto l'ingranaggio); gli altri
  dodici identici byte per byte, i due dei widget lasciati come su `main` (la casella del
  fuso, aperta dal 16.3).
- **La suite intera** (`./gradlew test :app:testDebugUnitTest :app:lintDebug`): 3 173 test
  verdi, 15 saltati (gli screenshot), e il lint con **un errore**, mio: una stringa letta dal
  contesto dentro un composable (le parole delle direzioni), che a un cambio di lingua
  sarebbe rimasta quella di prima. Corretto con `stringResource`. Dopo la correzione e gli
  ultimi ritocchi (la regola dei nomi come funzione pura, il conto delle stelle a frame, la
  frase della guida) sono stati rilanciati i test dell'esploratore, del palco, della guida,
  della navigazione, `SkySceneTest` e `NoRawColorTest` (95, verdi), il lint (0 errori) e la
  build minificata (`:app:assembleRelease -PsignReleaseWithDebugKey`), che riesce. La suite
  intera non è stata rilanciata dopo quei ritocchi: la fa la CI.

### Cosa conferma solo il telefono

Casella in «Aperto». Quel che una resa ferma non dice: **il dito** (il cielo che resta sotto
il dito, il pizzico, il giro vicino allo zenit, dove la camera gira in fretta perché non può
fare altro); **la fluidità** del trascinamento con duemila stelle a schermo; la Via Lattea
disegnata dalla GPU del telefono (una griglia a colori per vertice: qui è stata disegnata
dalla pipeline di prova); la pillola sull'hero con il carattere grande; la barra di stato
con le icone chiare; il gesto indietro.

## 16.7 — L'esploratore: l'ora, la notte, le nuvole, le costellazioni (committente, 11 nov 2026)

Il settimo step della Fase 16 (archivio 11, «Fase 16 — Cielo: la pagina come Oggi e il cielo da
esplorare», «Gli step»), da `origin/main` dopo l'unione del 16.6 (PR #158). L'esploratore aveva
un cielo e un dito; da qui ha un tempo suo: l'ora, la notte e la data si cambiano dalla pagina,
le nuvole dell'ora ci sono, e le costellazioni del 16.5 si disegnano per la prima volta. È la
tavola 3 senza l'elenco «Da vedere» (16.9) e senza «Punta il telefono» (16.10).

### Cosa è cambiato

- **Il pannello in basso** (`ExplorePanel`): l'ora in grande con le nuvole di quell'ora accanto,
  il nastro della notte, la notte fra due frecce. Sostituisce la riga «Il cielo delle 23:00,
  senza le nuvole» del 16.6, che se ne va con la sua stringa.
- **Il nastro è quello dell'hero** (`NightRibbon`, da privato a interno): la stessa barra, lo
  stesso dito, la stessa lettura per il lettore di schermo. Il pallino sta sull'ora disegnata,
  «adesso» resta un anello mentre se ne tiene un'altra.
- **La notte prima e dopo, e una data**: le due frecce, e il nome della notte che è il
  pulsante del calendario di sistema. La notte di un'altra data è quella del 16.4
  (`SkyStateBuilder.night`), chiesta attraverso **`SkyNights`**, un valore nuovo nello stato
  del Cielo (il luogo, la sua previsione, le impostazioni che il verdetto legge).
- **Le nuvole dell'ora** come un velo uniforme: spengono le stelle, i pianeti e la Via Lattea
  con la regola dell'hero (`SkyDome.starsUnder` e le due sorelle, sulle stesse funzioni di
  `SkyShader.kt`), ingrigiscono i colori del cielo con la tavolozza, impallidiscono la luna.
  **«Dietro le nuvole»** è la preferenza che c'è già (`skyBareSky`), come chip in alto.
- **Oltre la previsione** il cielo è disegnato senza nuvole e la riga accanto all'ora dice
  perché: la previsione non arriva a quella notte, la notte è passata, non c'è una previsione.
- **«Costellazioni»**: le 743 linee e gli 89 nomi del 16.5, girati dalla stessa rotazione delle
  stelle (`ConstellationUnits`, `SkyDome.constellationLines`); una preferenza nuova,
  `skyConstellations`, ricordata.
- **«Adesso»** e **«Stanotte»**, la via del ritorno, sul suolo accanto ai due pulsanti dello
  zoom, che ora stanno affiancati.
- **Il pavimento della camera è della pagina**: `SkyCamera` prende da chi la usa dove sta
  l'orizzonte (`horizonAt`), e la pagina lo tiene 12dp sopra quel che sta sul suolo.
- La guida (IT/EN) dice il pannello, le nuvole e le costellazioni nel paragrafo di «Stanotte»;
  il CHANGELOG allunga la voce dell'esploratore; DESIGN §8.8d è riscritta dove cambia.

### Decisioni e deviazioni

1. **Le nuvole: il velo uniforme, deciso guardando.** Lo step lasciava la scelta allo schermo.
   Viste le rese a 50%, 80% (sulla luna piena) e 100%: il velo dice quanta parte del cielo è
   presa, che è quel che la previsione sa, e non finge di sapere dove. Le stelle e i pianeti
   perdono il cielo aperto al quadrato, la Via Lattea al cubo (la regola dell'hero, la stessa
   funzione); i colori sono quelli che la tavolozza dà a quella copertura; la luna resta dov'è
   e impallidisce (un quinto del disco sotto una copertura intera, l'alone via per primo: la
   macchia chiara che è una luna coperta). A 100% il cielo è piatto, senza una stella, e il
   numero accanto all'ora dice perché. Il banco dello shader su una camera libera resta un
   lavoro a parte, come diceva lo step, e non è stato aperto.
2. **Le nuvole non planano.** Sull'hero la copertura scivola da un valore all'altro con una
   molla; qui no, per la regola della batteria: la copertura è letta fra le ore, quindi sotto
   il dito scorre da sé, e un interruttore la toglie in un frame.
3. **«Dietro le nuvole» c'è solo dove c'è una copertura da togliere.** In una notte che la
   previsione non raggiunge sarebbe un interruttore che non fa niente.
4. **La riga accanto all'ora non dice mai «sereno» dove nessuno l'ha previsto**: «Disegnato
   senza nuvole», con il motivo quando c'è (`NightForecast` del 16.4). Stampa le nuvole e non
   la pioggia, che l'hero aggiunge dal 20%: qui la riga è quanta parte del cielo si vede.
5. **Deviazione: il tempo della pagina segue l'orologio.** Il 16.6 prendeva l'istante una
   volta e lo diceva. Ora, senza un'ora tenuta e con la notte in corso, il cielo è quello di
   adesso e avanza un minuto alla volta: un «adesso» vecchio di venti minuti è un cielo
   girato di cinque gradi, e la riga con l'ora non basta più a scusarlo quando accanto c'è
   un pulsante che si chiama «Adesso». Costa un frame al minuto. Di giorno, o su un'altra
   notte, l'istante è la metà della notte e non si muove.
6. **L'ora tenuta passa all'altra notte**, alla stessa ora dell'orologio, se quella notte ce
   l'ha (`carriedHour`): è così che si guarda la luna spostarsi da una sera all'altra. Se non
   ce l'ha (le 22:00 portate a giugno) la notte si apre sulla sua metà.
7. **La via del ritorno c'è solo dove c'è un posto a cui tornare** (`exploreReturn`): «Adesso»
   con la notte in corso, da un'altra notte o da un'altra ora; «Stanotte» prima del
   crepuscolo, da un'altra notte. Prima del crepuscolo, con un'ora di stanotte tenuta, niente:
   il nastro sotto il dito è già tutto. La tavola 3 aveva «Adesso» nel pannello; sta sul
   suolo, accanto allo zoom, perché compare e scompare e nel pannello spostava l'ora.
8. **Dove stanno le cose.** La tavola 3 elenca gli interruttori senza dire dove. Stanno **in
   alto**, sotto la riga della direzione, e il tempo **in basso**: quel che la pagina disegna
   si sceglie una volta ed è ricordato, per quando lo disegna c'è il pollice. I due pulsanti
   dello zoom passano da incolonnati ad affiancati: in colonna alzavano il suolo di 100dp.
9. **Il pavimento: l'orizzonte 12dp sopra quel che sta sul suolo**, come il 16.6 chiedeva di
   rivedere (decisione 6 di quello step). Letto da dove comincia il piede della pagina
   (`horizonShare`), mai più basso di 0,82 dell'altezza e mai più alto di 0,56 (un pavimento
   a metà schermo è una camera sull'orizzonte). Su un telefono dritto di 832dp esce a 0,73.
   Una camera rimasta sotto un pavimento che è salito viene rialzata (`SkyCamera.within`): il
   pannello è più alto in una notte con il nastro che in una senza.
10. **Deviazione: «Costellazioni» è accesa finché qualcuno non la spegne.** Lo step diceva
    «scelta ricordata» e non il valore di partenza. Cinquemila punti senza una figura sono
    polvere per chi non ne sa nominare una, e la pagina si chiama «Esplora»: guardate le due
    rese fianco a fianco, quella con le linee è la pagina e l'altra il suo fondo. Rivedibile
    in una riga (`AppSettings.skyConstellations`).
11. **Le linee si fermano prima delle stelle** (5dp), sotto di loro, sottili (1dp, il bianco
    della tela a 0,3): una linea porta a una stella, non la attraversa. Sono segmenti dritti:
    su questa proiezione la linea vera è un arco di cerchio, e sui 25,6° del tratto più lungo
    (16.5) i due differiscono meno del punto della stella.
12. **I nomi delle figure sono di una regione e non di una luce**: maiuscole piccole e
    spaziate, più tenui, centrate sul punto della fonte, come una carta scrive un mare. Quanti
    se ne scrivono lo dice il rango del 16.5: il primo a ogni campo, il secondo dal campo
    d'apertura in giù, tutti sotto i 45° (`ExploreNames.figureNamed`).
13. **Una figura viene prima di una stella**, dopo la luna e i pianeti: chi ha acceso le
    figure ne ha chiesto i nomi, e «Lira» scartata per «Vega» lascerebbe la figura tracciata
    e senza nome.
14. **Linee e nomi se ne vanno con le stelle** (di giorno, sotto un cielo chiuso) e
    sbiadiscono con loro sotto la luna: una figura tracciata su un cielo senza stelle sarebbe
    una carta, e questa pagina è il cielo.
15. **Deviazione: lo stato del Cielo porta la previsione** (`SkyNights` in
    `SkyUiState.Content`). Il costruttore resta puro; l'alternativa era una funzione nel view
    model, e uno stato con dentro una funzione non si confronta più. Un valore sì.
16. **Il calendario è quello di sistema**, nel tema dell'app e non in quello della notte (è
    una finestra sopra la pagina). Un giorno è la sera in cui la notte comincia. **Da un anno
    indietro a cinque avanti: un limite scelto, non misurato.** I test del motore contro
    l'effemeride coprono il 2026 e il 2027; più in là niente dice che i pianeti sbaglino, e
    niente dice che non lo facciano.
17. **Su una finestra larga (da 600dp) il pannello è una card nell'angolo** e il cielo tiene
    il pavimento di prima: un telefono di lato con il pannello per tutta la larghezza avrebbe
    avuto un cielo alto cento dp.
18. **Il pannello è scuro quasi del tutto** (0,9). A 0,6, nella prima resa, più vicino di
    dove arriva l'orizzonte le stelle e le linee stavano dentro le cifre dell'ora.
19. **La camera non si muove quando si muove l'ora**: è il cielo che gira sotto. La direzione
    con cui la pagina si apre è scelta una volta.
20. **Quel che la tavola 3 ha e questo step non disegna**: l'elenco «Da vedere» (16.9),
    «Punta il telefono» (16.10), il tocco su un oggetto (16.8). VISION §5.3 e lo screenshot
    dell'esploratore restano al 16.11.

### Trovato e non toccato

Una data senza notte astronomica (le notti bianche: Stoccolma da maggio ad agosto, Londra fra
fine maggio e metà luglio) **non ha un nastro**, qui come sull'hero: l'esploratore mostra il
cielo di adesso, o della mezzanotte per un'altra data, e l'ora non si cambia. Il pannello dice
quale cielo è («Mai del tutto buio»), ma chi sta a Londra in giugno ha un esploratore con
un'ora sola. Casella in «Aperto», proposta e non decisa: dove non c'è la notte astronomica, il
nastro va dal tramonto all'alba.

### Come è stato verificato

- `ExploreTimeTest` (10, JUnit semplice): l'istante del cielo (l'ora tenuta, adesso a notte
  in corso, la metà altrimenti; adesso o la mezzanotte dove la notte non c'è, da Stoccolma il
  23 giugno); l'ora portata a un'altra notte, anche attraverso il cambio dell'ora del 25
  ottobre, e non portata dove la notte non ce l'ha; la via del ritorno nei suoi otto casi;
  dove sta l'orizzonte; il nome di un'altra notte, con l'anno quando serve; quali figure
  nomina un campo; `SkyNights` è il costruttore, e lo stato la porta.
- `SkyCameraTest` (20, tre nuovi): con quattro pavimenti diversi la camera si apre con
  l'orizzonte lì, un trascinamento e un passo si fermano lì, e allargando il campo
  l'orizzonte non si muove; una camera sotto un pavimento salito viene rialzata; il pavimento
  non arriva a metà schermo.
- `SkyDomeTest` (12, quattro nuovi): **ogni estremo delle 743 linee è dove il motore mette la
  sua stella** (2 972 confronti, nei due emisferi, sotto un millesimo di grado); una linea
  dell'Orsa Maggiore finisce sulle stelle della cupola; ogni nome è al suo punto, i due del
  Serpente compresi; la copertura toglie le stelle come dice la regola dell'hero, e le figure
  con loro.
- `SkyExploreScreenTest` (18, dieci nuovi, attraverso la GPU): l'orizzonte sta sopra i pulsanti
  e i pulsanti sopra il pannello; **un'ora coperta toglie le stelle e «Dietro le nuvole» le
  rende**, con le parole che restano della previsione; mezza copertura le attenua senza
  toglierle; la luna dietro il velo è più debole di quella scoperta e più chiara del suo
  cielo; **le linee di Orione sono disegnate fra le sue stelle**, e l'interruttore le toglie;
  il nastro tiene un'altra ora e «Adesso» rende l'orologio; le frecce passano di notte in
  notte e ogni notte dice quel che la previsione sa; di giorno la via del ritorno è
  «Stanotte»; il nome della notte apre il calendario e un giorno scelto è una notte; su una
  finestra larga il pannello è nell'angolo; disegna quando cambia la camera o l'ora e in
  nessun altro momento (un minuto e mezzo a guardare, con l'orologio fermo, non chiede
  niente).
- **Guardato**, non solo provato: quindici rese (la notte buia con le figure, verso est con
  Orione, il campo più largo e il più stretto, lo zenit, la luna piena senza previsione e
  dietro l'80% di nuvole, 50% e 100% di copertura, «Dietro le nuvole», un'ora tenuta, la
  notte oltre la previsione, il telefono di lato). Da lì vengono le decisioni 1, 10 e 18.
- `sky-tonight.png` e gli altri screenshot del README **non sono stati rigenerati**: l'hero
  cambia solo nella visibilità di tre nomi (`NightRibbon`, `ChipFill`, `ChipScrim`).
- **La suite intera** (`./gradlew test :app:testDebugUnitTest :app:lintDebug`): 3 220 test
  verdi, 15 saltati (gli screenshot), e il lint con 0 errori e nessun avviso sui file
  toccati. Le due frasi della guida sono cambiate a suite già partita: dopo, sono stati
  rilanciati i test della guida, delle stringhe, dell'esploratore, del palco dell'hero e
  `NoRawColorTest` (107, verdi), il lint (0 errori) e la build minificata
  (`:app:assembleRelease -PsignReleaseWithDebugKey`), che riesce.

### Cosa conferma solo il telefono

Casella in «Aperto». **Il dito sul nastro dentro l'esploratore** (il cielo che gira sotto la
camera a ogni passo di cinque minuti: qui ogni passo ricalcola la cupola, e solo il telefono
dice se scorre); le linee delle costellazioni disegnate dalla GPU del telefono, e se a 0,3
sono troppo o troppo poco su uno schermo vero; i nomi in maiuscolo con il carattere grande;
il calendario; il velo delle nuvole in una notte che le ha; «Adesso» lasciato aperto qualche
minuto, che avanza da solo; il telefono di lato.

## La notte bianca ha un nastro: dal tramonto all'alba (committente, 11 nov 2026)

Chiesto nella stessa chat del 16.7, dopo la spiegazione della casella che quello step aveva
lasciato aperta («se non è una cosa lunga implementa ora la tua proposta e mettila nella stessa
PR»), con un quarto d'ora a disposizione e l'indicazione di **non lanciare la compilazione, i
test e il lint: li esegue la CI al push**. È nella PR #159, come secondo commit.

### Cosa è cambiato

- **`SkyStateBuilder.exploredNight`**: la notte di una data per l'esploratore. Dove la data ha
  una notte astronomica è `night`, identica. Dove il cielo non diventa mai del tutto buio
  (`NO_DARKNESS`) la notte prende **un intervallo dal tramonto all'alba**, con il posto del sole
  lungo l'intervallo (i colori della barra) e le ore di nuvole della previsione; il resto
  resta com'era: nessuna finestra di buio, nessun verdetto, lo stesso motivo.
- **`SkyNights.of`** chiede quella. È l'unica strada da cui l'esploratore prende una notte.
- **Il pannello** scrive «Mai del tutto buio» anche sotto il nastro, non più solo al suo posto.
- DESIGN §8.8d dice il nastro della notte bianca.

### Decisioni e deviazioni

1. **Solo l'esploratore.** L'hero legge la notte di `build`, e una notte bianca lì resta una
   frase senza nastro: è un colpo d'occhio, e la sua frase dice già tutto. Nell'esploratore il
   nastro è l'unica maniglia sull'ora, e senza di lui chi apre la pagina a Londra alle tre di
   un pomeriggio di giugno vede il cielo del pomeriggio e non può chiedere le undici e mezza.
2. **Il cielo non è detto buio: gli sono date delle ore.** La barra ha i colori del
   crepuscolo, i due orari sotto sono quelli del sole, e la frase resta. Niente verdetto: i
   quattro verdetti giudicano una finestra di buio, e non c'è.
3. **Dal tramonto all'alba, non dal crepuscolo civile o nautico.** Sono i due orari che un
   lettore conosce, e a 60° di latitudine in giugno il crepuscolo nautico non finisce nemmeno
   lui: la scelta più stretta avrebbe lasciato di nuovo senza nastro chi ne ha più bisogno.
4. **Restano senza nastro** una data senza tramonto (il sole di mezzanotte) e la notte polare
   profonda: lì non c'è un intervallo da prendere, e il pannello dice quale cielo è.
5. **Di giorno la pagina si apre sulla metà dell'intervallo**, che in una notte bianca è il
   momento più scuro; dopo il tramonto su adesso. È la regola delle altre notti
   (`tonightInstant`), senza un caso in più.

### Come è stato verificato

**Non è stato verificato in locale.** Il codice è scritto e riletto, non compilato; i due test
nuovi di `ExploreTimeTest` (una notte bianca a Stoccolma il 23 giugno 2026 ha un nastro dal
tramonto all'alba e resta una notte non buia; una notte che diventa buia è la stessa per
l'esploratore, a Milano in novembre e in giugno) sono scritti e mai lanciati. Il caso del sole
di mezzanotte è letto nel codice e non ha un test. Li esegue la CI sulla PR #159: se è rossa,
il primo sospetto è un'aspettativa di quei due test.

### Cosa conferma solo il telefono

Casella in «Aperto»: il nastro di una notte bianca non è mai stato disegnato, nemmeno in una
resa.
