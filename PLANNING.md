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
- [ ] **Un verdetto su una finestra che la previsione copre a metà** (registro, «16.4», decisione 4).
  `SkyVerdictEngine` giudica le ore che trova: se la previsione finisce dentro la finestra, il
  verdetto è della parte che c'è, per ogni riga dell'app (uno sciame o la notte al settimo
  giorno). Proposta, non decisa: «non si sa ancora», con il motivo «oltre la previsione»,
  quando l'ultima ora della previsione finisce prima della fine della finestra; cambia quel
  che si legge a schermo sulle righe al bordo della settimana.
- [ ] **La finestra di uno sciame che si apre al picco** (registro, «16.4», decisione 10). Quando
  il picco cade di notte la finestra del lavoro va dal picco all'alba: per le Geminidi del
  2026 sono sedici minuti (05:51–06:07), in una notte in cui il radiante è su dal crepuscolo
  all'alba. Uno sciame è largo un giorno o più. Proposta, non decisa: la notte intera del picco, dal
  crepuscolo all'alba, per la riga, il promemoria e l'elenco «Da vedere».
- [ ] **16.5** Le costellazioni: la fonte e la licenza, la risorsa, i nomi (Fase 16).
- [ ] **16.6** L'esploratore: la camera libera e il cielo a tutto schermo (Fase 16).
- [ ] **16.7** L'esploratore: l'ora, la notte, le nuvole, le costellazioni (Fase 16).
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
