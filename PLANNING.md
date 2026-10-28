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

- **Rilasciata**: 1.1.0 (1 ott 2026, `v1.1.0`). Quel che è entrato dopo sta nella sezione
  `[Unreleased]` del `CHANGELOG.md`.
- **In corso**: **Fase 14**, il motore contro i motori premium (dal 27 ott 2026): tredici step, uno per chat; i primi quattro (14.1–14.3 con la scadenza del 30 novembre, la visibilità ICON che sparisce; 14.4 la misura di ICON-2I, che ha detto no), il 14.6 (il caldo), il 14.7 (il vento), il 14.8 (la quota neve), il 14.9 (le unità), il 14.10 (la concordanza dei modelli, che ha detto no) e il 14.13 (le icone, prima dell'inverno) sono chiusi; 14.5 e 14.11 non esistono. La revisione, le decisioni, le regole comuni e la descrizione di ogni step sono in `docs/planning/07-ottobre-22-27.md`, «Fase 14 — Il motore contro i motori premium»; uno step chiuso scrive la sua sezione qui.

## Aperto

Quel che resta da fare o da decidere, in un posto solo: una casella si apre qui, con il rimando
alla sezione del registro da cui nasce, e si chiude qui, con quella che la chiude.

- [ ] **Il cielo d'inverno contro il satellite.** La regola 6 è misurata su ottobre soltanto:
  l'archivio del satellite di Open-Meteo parte da febbraio 2026. Rilanciare
  `tools/measure_against_metar.py` su dicembre–gennaio 2026-27 e rileggere STATES §11 (archivio 07,
  «Il motore dopo la revisione», 26 ott 2026).
- [x] **14.1** La sonda del provider, il quarto d'ora dove è calcolato, le regole (Fase 14). Fatto il 27 ott 2026 (archivio 07, «14.1»).
- [x] **14.2** La nebbia senza la visibilità di ICON: la misura. Fatto il 27 ott 2026 (archivio 07, «14.2»): la regola scelta entra con 14.3, subito.
- [x] **14.3** La nebbia senza la visibilità di ICON: il codice, e il tile. Fatto il 27 ott 2026 (archivio 07, «14.3»): la regola di 14.2 è nel motore e vale da oggi, un mese prima di DWD.
- [x] **14.4** ICON-2I contro ICON-EU e ICON-D2: la misura. Fatto il 27 ott 2026 (qui sotto, «14.4»): ICON-2I non vince in nessuna cella decisiva, non entra.
- [x] **14.5** ICON-2I nel report, dove ha vinto (solo se 14.4 lo dice). Chiusa il 27 ott 2026 senza lavoro: 14.4 ha detto no (qui sotto, «14.4»).
- [x] **14.6** Il caldo: il gradino della frase e il pericolo. Fatto il 27 ott 2026 (qui sotto, «14.6»).
- [x] **14.7** Il vento nel «Maltempo», con la quota del luogo. Fatto il 27 ott 2026 (qui sotto, «14.7»): le soglie sono quelle della tabella MeteoSwiss letta dal documento, non quelle scritte nello step.
- [x] **14.8** La quota neve. Fatto il 28 ott 2026 (qui sotto, «14.8»): la riga nel giorno aperto e nella sera, due variabili in più (5,4 chiamate la previsione, 8,4 il fetch).
- [x] **14.9** Le unità: m/s, nodi, Beaufort, inHg, mmHg. Fatto il 28 ott 2026 (qui sotto, «14.9»): i default restano km/h e hPa, gli id canonici delle regole restano `_kph` e `_mb`.
- [x] **14.10** La concordanza dei modelli: la misura. Fatto il 28 ott 2026 (qui sotto, «14.10»): lo scarto fra i modelli dice quanto si sbaglia in media, non se il giorno è sbagliato; «non concordano» non è vera due volte su tre a nessuna soglia che scatti.
- [x] **14.11** La concordanza a schermo (solo se 14.10 lo dice). Chiusa il 28 ott 2026 senza lavoro: 14.10 ha detto no (qui sotto, «14.10»).
- [ ] **14.12** Gli avvisi ufficiali fuori dall'Italia, a chiusura garantita: **in attesa** dal 28 ott 2026, su via libera del committente, quando ci sarà una base di lettori in Europa (qui sotto, «14.12 resta in attesa»).
- [x] **14.13** Le icone che mancano: pioggia e pioviggine gelata, pioggia e neve possibile, composte. Fatto il 27 ott 2026 (qui sotto, «14.13»).

Riletto il 27 ott 2026, all'archivio 07: le caselle sopra sono tutte della Fase 14, nessuna proposta non decisa. Fino al 26 ott 2026 nessuna casella aperta (riletto il 22 ott 2026, all'archivio 06):
le ultime chiuse quel giorno stanno in `docs/planning/06-ottobre-20-22.md`, «Cielo: la finestra
reale nella card Stanotte», «Il widget «Momenti del cielo» va via», «Le notifiche: ogni riga nella
forma che sta in una riga», «Il widget «Le prossime ore» va via».

---

## Registro dal 27 ott 2026, dopo l'archivio 07

## 14.13 — Le icone che mancano (committente, 27 ott 2026)

**Fatto.** Quattro disegni composti dai pezzi di Meteocons, con uno strumento nuovo accanto a
`compose_sky_marks.py`, lo stesso contratto: `tools/compose_falls.py` scrive 32 drawable (quattro
facce ferme e quattro animate per disegno) e `ui/icons/ComposedFalls.kt`, e niente è disegnato a
mano. La pioggia gelata e la pioviggine gelata hanno un glifo loro; la pioggia e la neve
possibile non sono più la pioviggine e la neve reali.

**Le ricette.**

- **`freezing-rain`, `freezing-drizzle`** (66/67, 56/57): `rain` e `drizzle` **intatti** (stessi
  percorsi, stessa scala, stesso posto, le gocce che cadono come nella sorgente) più il cristallo
  di `snowflake` al **28%** sotto la nuvola, in basso a destra, con il centro dell'inchiostro a
  (108, 113): dove il ghiaccio si forma, a terra. Il posto è misurato: a x 108 il lobo destro della
  nuvola finisce a y 93,5 e il cristallo, contorno compreso, comincia a 98,4; la terza goccia
  finisce a x 82,2 e il cristallo comincia a 93,4, quindi la caduta animata non lo attraversa mai
  (lo strumento rifiuta una ricetta in cui si toccano o che esce dalla scatola). Al 32% toccava il
  lobo; a sinistra delle gocce si leggeva come una quarta goccia; sotto le gocce ci cadevano
  dentro. Largo **26,8 unità su 128** con il contorno: 7 px nella striscia a 32 dp, 8 nella
  settimana a 38, 11 nell'eroe del widget a 52. Tiene la linea della famiglia (`strokeWidth` /
  0,28, la regola dei segni del cielo) e dondola di 6° in 3 s come `snowflake` fa da solo, il
  passo della nuvola.
- **`rain-possible`, `snow-possible`** (1061, 1071): la nuvola di `drizzle` e di `snow` con **una
  goccia e un fiocco in meno** (va via quella di mezzo, restano la prima e la terza, simmetriche)
  **e più chiari**: `strokeAlpha` / `fillAlpha` su quel che cade, al **60%, o a quanto il 3:1 sul
  fondo permette se è di più** (DESIGN §10, misurato sulle superfici delle due vesti, §2.5). Le
  animazioni della sorgente sono copiate alla lettera e i fotogrammi dell'alfa moltiplicati per la
  stessa chiarezza, perché un `objectAnimator` sostituisce la proprietà: una goccia che tornasse
  piena appena si muove direbbe pioggia, non possibilità.

**Le misure della chiarezza** (contrasto sul fondo peggiore della faccia; «pieno» è il colore
di Meteocons com'è):

| Faccia | Gocce (`drizzle`) | Fiocchi (`snow`) |
|---|---|---|
| line chiaro (`mc3_`) | pieno 9,33:1; soglia 0,55; **scritto 0,60** (3,40:1) | pieno 4,19:1; soglia 0,81; **scritto 0,85** (3,25:1) |
| line scuro (`mc3n_`) | pieno 6,10:1; soglia 0,62; **scritto 0,65** (3,25:1) | pieno 11,53:1; soglia 0,43; **scritto 0,60** (4,79:1) |
| flat chiaro (`mc3f_`) | pieno 9,36:1; soglia 0,55; **scritto 0,60** (3,41:1) | pieno 4,24:1; soglia 0,80; **scritto 0,80** (3,01:1) |
| flat scuro (`mc3fn_`) | pieno 6,16:1; soglia 0,61; **scritto 0,65** (3,26:1) | pieno 11,63:1; soglia 0,43; **scritto 0,60** (4,84:1) |

Sul fondo chiaro i fiocchi possono schiarirsi di poco (il loro blu-verde sta a 4,2:1 già pieno),
e lì la possibilità della neve la dice il conto dei fiocchi più del colore. È il limite onesto:
un segno sotto il 3:1 non si legge, e DESIGN §10 vale più della ricetta. `ComposedFallsTest`
rimisura l'alfa scritto contro gli schemi veri (non contro la tabella dello strumento) e
pretende che un passo più chiaro non reggerebbe.

**Il foglio** (`tools/icon_filmstrip.py --foglio 32,38,52`, nuovo modo dello strumento: i disegni
nominati, fermi, nelle quattro facce sul loro fondo, alle tre taglie, e sotto le strisce
animate), reso con Edge headless e guardato: i quattro nuovi accanto a `drizzle`, `rain`,
`sleet`, `snow`. Allegato: [`docs/planning/14-13-foglio-icone.png`](docs/planning/14-13-foglio-icone.png).
A 32 px il cristallo è un asterisco leggibile sotto la nuvola e non si confonde con una goccia;
«pioggia possibile» si distingue dalla pioviggine per le due gocce rade e più chiare; «neve
possibile» sul fondo chiaro per i due fiocchi, sul fondo scuro anche per il colore. Lo strumento
legge ora l'alfa scritto nell'XML come punto di partenza (prima disegnava piene le gocce dei
disegni fermi).

**Il resto del lavoro.** `ChiaroIcons.conditionLineRes` assegna i quattro; `styledRes` e
`movingRes` interrogano la quarta tabella (`ComposedFalls`, fra `ComposedIcons` e `SkyMarks`);
widget e notifiche ereditano dalla risorsa. `ConditionIconsTest` (la tabella dei codici),
`MeteoconsSetsTest` (la lista di spedizione conta anche `ComposedFalls`), `ComposedFallsTest`
nuovo: pezzi alla lettera meno il gruppo tolto, base ferma alla sua taglia, linea della famiglia,
chiarezza e 3:1, fotogrammi dell'alfa, il dondolio del cristallo uguale a quello di `snowflake`,
i sei codici per tutte le strade, nessun disegno in due tabelle. `shipped_icons.py` cambia solo
nei commenti: `rain`, `drizzle`, `snow` e `snowflake` erano già spediti. DESIGN §13.1 (la regola
delle quattro composizioni, con le misure), STATES §8, CLAUDE.md (il quarto strumento e il
quarto file generato), CHANGELOG `[Unreleased]`, le stringhe non cambiano (le parole c'erano). Gli screenshot del
README rigenerati con `-PupdateScreenshots` e guardati con `git status`: **nessuna immagine
cambia**, la Milano registrata (23 set 2026) non ha nessuno dei quattro stati, nemmeno «pioggia
possibile» che lo step dava per probabile.

**Deviazioni.** (1) Lo step chiedeva `ComposedIconsTest` per il confronto percorso per percorso:
quel test è costruito sul sole e sulla nuvola (buco della maschera, `g3sun`, `g6cloudplace`) e i
quattro disegni nuovi non ne hanno nessuno; il confronto sta in `ComposedFallsTest`, con la
stessa promessa. (2) Un composer nuovo e un file generato nuovo, non `compose_sky_marks.py`: i
segni del cielo non hanno gemelli animati per regola (`SkyMarksTest` lo pretende) e queste
quattro sono condizioni, che si muovono (DESIGN §7.1). (3) Il cristallo si muove: lo step lo
immaginava fermo, ma `snowflake` in Meteocons dondola e la regola dello step («le animazioni
della fonte dove la parte le porta») vale più dell'immaginazione; `ComposedFallsTest` pretende
che il dondolio sia quello della sorgente, alla lettera.


## 14.4 — ICON-2I contro ICON-EU e ICON-D2: la misura (committente, 27 ott 2026)

**Il criterio, scritto prima di guardare i numeri** (lo step lo chiede così; questa parte è
stata committata prima che lo strumento scaricasse una riga di ICON-2I).

*Che cosa si confronta.* Per i tredici aeroporti italiani — gli undici dell'archivio più Bari
(LIBD) e Catania (LICC), aggiunti con questo step — quattro serie contro le osservazioni:
`best_match` **come è servito** (è quel che il lettore vede oggi), `icon_eu`, `icon_d2` dove
arriva, `italia_meteo_arpae_icon_2i`. Due gruppi: il **sud** sono gli aeroporti dove la sonda
del 27 ott non trova ICON-D2 in `best_match` — Roma, Napoli, Bari, Palermo, Cagliari — più
Catania, a sud-est di Palermo e quindi fuori dal dominio di ICON-D2 come lei; il **nord** è la
pianura padana (Linate, Malpensa, Orio, Caselle, Villafranca, Bologna, Tessera), dove
`best_match` è ICON-D2 nei giorni 1–2 e ICON-EU dal giorno 3.

*I giorni di anticipo.* Il **giorno 1** è l'archivio (`historical-forecast-api`, le prime ore di
ogni corsa: a Roma il 10 dic 2025 è uguale ora per ora a `previous_day0` della Previous Runs
API, 24 ore su 24). Il **giorno 2** è `previous_day1` della Previous Runs API (il valore che la
corsa di 24 ore prima dava per quell'ora), il **giorno 3** è `previous_day2` (48 ore prima).
ICON-2I esce ogni 12 ore con 72 ore di orizzonte, ICON-EU ogni 3 con 120, ICON-D2 ogni 3 con
48: ICON-2I al giorno 2 porta una corsa più vecchia di quella di ICON-EU, ed è il suo handicap
vero anche nell'app; ICON-D2 non ha un giorno 3 e ICON-2I non ha un giorno 4, e dove un modello
non arriva la cella è vuota, non uno zero. La Previous Runs API porta per giorno di anticipo
temperatura, umidità, rugiada, copertura totale, precipitazione, neve, rovesci, vento e codice,
**non** la visibilità né gli strati (verificato a Roma il 27 ott): il cielo dei giorni 2–3 si
misura sulla copertura totale, per tutti i modelli allo stesso modo.

*Le quattro misure*, per modello, giorno, periodo (ottobre 2026 = 26 set–25 ott; inverno =
1 dic 2025–31 gen 2026), gruppo e aeroporto, sulle ore con un METAR entro 30 minuti:

- **temperatura**: errore medio assoluto fra la temperatura dell'ora del modello e quella del
  METAR (`tmpf`, portata in °C);
- **ora bagnata**: CSI di «≥ 0,1 mm nell'ora i..i+1» contro un METAR con un fenomeno in caduta
  sulla stazione nella stessa ora (`wet_seen`), **senza la probabilità** per nessuno: ICON-2I
  non ce l'ha, e il filtro del 20% andrebbe a favore di chi ce l'ha;
- **fase**: giusta (pioggia, neve, mista; il liquido è il totale meno la neve / 0,7, come nel
  motore) sulle ore in cui METAR e modello hanno precipitazione;
- **cielo**: giusto contro il satellite (sole sopra 15°, ore asciutte, senza nebbia vista né
  detta), al giorno 1 con la regola 6 di STATES (la luce del disegno, strati 1 / 0,8 / 0,3:
  l'archivio ha gli strati di ICON-2I, verificato), ai giorni 2–3 con la copertura totale.

*La decisione*, dichiarata ora:

- **Sud, giorni 1 e 2.** ICON-2I entra se, **in ciascuno dei due giorni e in ciascuno dei due
  periodi**, sull'insieme degli aeroporti del sud, CSI(ICON-2I) ≥ CSI(`best_match`) + 0,02
  **e** MAE(ICON-2I) ≤ MAE(`best_match`) − 0,2 °C. L'avversario è `best_match` come servito
  (ICON-EU con la correzione di quota di Open-Meteo al giorno 1, ICON-EU tale e quale ai giorni
  2–3: a Roma il 10 dic 2025 i due `previous_day1` coincidono); la colonna `icon_eu` si stampa
  accanto per leggere quanto vale la correzione, non per decidere.
- **Nord, giorno 3.** ICON-2I entra per il giorno 3 se batte `best_match` (lì ICON-EU) con gli
  stessi margini, in entrambi i periodi. Nei giorni 1–2 al nord ICON-D2 resta, come lo step
  dice: quelle celle si stampano per sapere, non per decidere.
- Una vittoria su una sola delle due misure, o in un solo periodo, o in un solo giorno, **non
  basta**: 14.5 non esiste e la sua casella si chiude con il perché. Non si cercano, dopo i
  numeri, sottoinsiemi (un aeroporto, un mese, una fascia oraria) in cui ICON-2I vinca.
- **Fase e cielo non decidono** se ICON-2I entra: dicono quali variabili 14.5 sovrapporrebbe
  (dove la fase o il cielo di ICON-2I perdono contro `best_match`, restano di `best_match`
  anche dove ICON-2I entra per temperatura e pioggia).
- Una cella decisiva con meno di **30 ore bagnate osservate** si stampa ma non regge una
  decisione: se succede, lo step chiude «non deciso» e dice che cosa manca (un altro periodo).

**Fatto.** La misura dice **no**: ICON-2I non batte `best_match` in nessuna cella decisiva, su
nessuna delle due misure, e non entra. **14.5 non esiste.** Nessun codice dell'app cambia.

**I dati.** I due archivi hanno ora 26 aeroporti (Bari e Catania scaricati per intero: previsioni
dei dieci modelli, METAR, satellite, probabilità), le colonne di ICON-D2 (presente a nord di
Firenze, a Zurigo, Monaco, Vienna, Parigi, Amsterdam, Londra; 400 altrove) e di ICON-2I (le
undici italiane più Bari e Catania, e Zurigo, Monaco, Vienna: il dominio prende le Alpi), e per i
tredici italiani il blocco `previous_runs` con i giorni 2 e 3 dei quattro modelli (ICON-D2 al sud
400, al nord solo il giorno 2: 48 ore di orizzonte). Ore con un METAR e una temperatura:
13 139 al sud, 15 342 al nord, nei due periodi; ore bagnate viste 1 551 al sud (1 293 d'inverno,
258 a ottobre) e 1 847 al nord (1 456 e 391).

**La tabella** (MAE della temperatura in °C contro il METAR; ora bagnata: dette / prese / false
e CSI; `best_match` come servito, ICON-2I; il Δ è ICON-2I meno `best_match` sul CSI e
`best_match` meno ICON-2I sul MAE: positivo è ICON-2I meglio; le soglie erano +0,02 e +0,2):

| Cella | `best_match` | ICON-2I | ΔCSI | ΔMAE |
|---|---|---|---:|---:|
| **Sud, giorno 1, inverno** | 1,00 °C; 1 780 / 928 / 852; **0,43** | 1,04 °C; 1 220 / 767 / 453; **0,44** | +0,007 | −0,04 |
| **Sud, giorno 1, ottobre** | 0,99 °C; 374 / 140 / 234; **0,28** | 0,94 °C; 166 / 84 / 82; **0,25** | −0,037 | +0,04 |
| **Sud, giorno 2, inverno** | 1,11 °C; 1 631 / 822 / 809; **0,39** | 1,08 °C; 1 185 / 682 / 503; **0,38** | −0,011 | +0,03 |
| **Sud, giorno 2, ottobre** | 1,06 °C; 315 / 117 / 198; **0,26** | 1,04 °C; 146 / 70 / 76; **0,21** | −0,047 | +0,02 |
| **Nord, giorno 3, inverno** | 1,33 °C; 1 540 / 887 / 653; **0,42** | 1,38 °C; 1 379 / 839 / 540; **0,42** | 0,000 | −0,05 |
| **Nord, giorno 3, ottobre** | 1,12 °C; 411 / 201 / 210; **0,33** | 1,19 °C; 409 / 229 / 180; **0,40** | +0,067 | −0,07 |
| Sud, giorno 3, entrambi | 1,17 °C; 1 954 / 855 / 1 099; 0,32 | 1,12 °C; 1 396 / 708 / 688; 0,32 | +0,002 | +0,05 |
| Nord, giorno 1, entrambi (`best_match` è ICON-D2) | 0,80 °C; 1 802 / 1 348 / 454; 0,59 | 1,21 °C; 1 691 / 1 218 / 473; 0,53 | −0,061 | −0,41 |
| Nord, giorno 2, entrambi (`best_match` è ICON-D2) | 1,05 °C; 1 990 / 1 265 / 725; 0,49 | 1,24 °C; 1 866 / 1 189 / 677; 0,47 | −0,025 | −0,19 |

Le colonne `icon_eu` e `icon_d2`, stampate accanto dallo strumento: al sud `best_match` e ICON-EU
coincidono d'inverno in ogni numero (giorno 1 compreso: la correzione di quota di 14.1 non c'era
in quelle ore) e a ottobre differiscono di 0,04 °C sul MAE (0,95 ICON-EU, 0,99 `best_match`) e di
nulla sul CSI; al nord, giorno 1, ICON-D2 è 0,82 °C e 0,57 contro gli 0,80 e 0,59 di
`best_match` (che lì ha i quarti e la correzione), ICON-EU 1,00 e 0,53, ICON-2I 1,21 e 0,53.

**Che cosa dicono i numeri, oltre al verdetto.**

- **Sulla temperatura ICON-2I non è mai più vicino di 0,05 °C**, in nessuna cella decisiva; al nord
  è **0,4 °C più lontano** di ICON-D2 al giorno 1 (peggio a Malpensa d'inverno, 1,59 contro 0,91,
  e a Tessera a ottobre, 1,23 contro 0,64) e 0,2 al giorno 2. La griglia a 2,2 km non vale la
  correzione di quota e il nowcast che `best_match` ha già.
- **Sull'ora bagnata ICON-2I è un modello più asciutto con lo stesso CSI**: al sud, giorno 1, dice
  1 386 ore bagnate contro 2 154, con metà delle false (535 contro 1 086) ma 700 ore di pioggia
  mancate contro 483 (POD 55% contro 69%). Il punteggio non cambia, l'errore cambia faccia: per
  il lettore che riceve «Maltempo» sarebbero meno falsi allarmi e più pioggia non annunciata.
- **L'unica cella con un margine vero è il nord al giorno 3 di ottobre** (CSI 0,40 contro 0,33,
  391 ore bagnate viste): non si ripete d'inverno (0,42 contro 0,42, 1 455 ore) e la temperatura
  perde in entrambi. Il criterio chiedeva entrambi i periodi ed entrambe le misure, ed è scritto
  sopra: non si va a cercare perché ottobre sia andato così.
- **La fase è giusta al 99–100% per tutti** (al sud è sempre pioggia) e **il cielo** di ICON-2I
  perde: al giorno 1, con la regola 6, 71% contro 73% al sud e 56% contro 67% al nord (ICON-D2
  67%); solo ottobre, perché l'archivio del satellite non ha l'inverno (casella aperta).
  Nessuna delle due avrebbe deciso; nessuna avrebbe cambiato il no.
- Per aeroporto (tabella dello strumento, `best_match` → ICON-2I, MAE / CSI): ICON-2I prende un
  punto o due di CSI a Bari, Catania e Cagliari d'inverno e li perde a Palermo e Fiumicino
  (0,44 → 0,23 a ottobre); sul MAE le differenze stanno entro 0,2 °C in quasi ogni cella del
  sud, e le più grandi sono a Fiumicino nei giorni 2 e 3 (fino a 0,25 °C a ottobre, 1,34 → 1,09),
  un aeroporto solo.

**Deviazioni.**

- **I giorni 2 e 3 vengono dalla Previous Runs API** (`previous-runs-api.open-meteo.com`,
  `<variabile>_previous_day1` e `_day2`), non dall'archivio cucito per giorno di anticipo che lo
  step 14.10 immagina: l'archivio delle previsioni è solo la prima parte di ogni corsa. L'API è
  fra quelle che CLAUDE.md lascia agli strumenti di misura, risponde sul passato (dicembre 2025
  compreso) e porta per giorno di anticipo nove delle tredici variabili: senza visibilità e
  senza strati, da cui il cielo dei giorni 2–3 sulla copertura totale. Lo strumento ha il modo
  `--add-lead-days`; il workflow lo esegue nel passo di completamento; i giorni 2–3 esistono
  per i tredici aeroporti italiani soltanto (14.10 chiederà i suoi).
- **Un modello per richiesta nei completamenti.** Quando dei modelli chiesti uno solo copre il
  punto, Open-Meteo risponde con le colonne **senza** il suffisso `_<modello>` (a Roma, ICON-D2
  assente, le colonne di ICON-2I arrivavano nude e il file non le trovava: visto nella prova su
  due giorni, prima del download vero). `--add-models` e `--add-lead-days` chiedono ora un
  modello alla volta e mettono il suffisso da sé (`suffixed`). Lo stesso peso in chiamate.
- **Scaricato in locale, non sul runner**: ~1 050 chiamate pesate (la formula del provider,
  `variabili / 10 × giorni / 14`) in 26 minuti, contro le ~3 ore del runner a 45 s a richiesta
  (registro 14.2). Il workflow gira lo stesso al push, perché lo script è cambiato, e non deve
  commettere nulla: ogni modo è idempotente e riscrive un file solo quando gli aggiunge qualcosa.
- **Due cose imparate dal download**, nello strumento: Windows ha negato per un attimo la
  rinomina del file appena scritto (`PermissionError` dopo 18 colonne; `write` ritenta cinque
  volte), e tre risposte sono arrivate con il corpo vuoto (`Expecting value`, Zurigo e Monaco
  d'inverno, Napoli nelle corse precedenti): lo strumento non le ritenta, un secondo passaggio
  dello stesso modo le ha prese, ed è quel che il workflow fa a ogni push.
- **I numeri di STATES §11 restano su 24 aeroporti.** Con Bari e Catania nei file, chi rilancia
  lo strumento legge 45 605 ore asciutte e 1 442 nebbie (erano 41 873 e 1 410): la regola di oggi
  dice 2 093 / 759 / 1 334, CSI 0,27, la regola scelta in 14.2 2 019 / 764 / 1 255, CSI 0,28.
  Nulla cambia nell'ordine delle varianti; una nota in §11 lo dice, le tabelle non si riscrivono.
- **Niente CHANGELOG, niente screenshot, niente stringhe**: nulla raggiunge lo schermo. VISION
  §12, decisione 11, riceve una riga con l'esito.

**Come è stato verificato.** Il criterio sta nel commit precedente a ogni dato di ICON-2I; la
prova dello strumento su un file di due giorni a Fiumicino (sei chiamate) prima del download;
`analyze_against_metar.py` sui due file completati, con le sezioni di 14.2 rilette (sopra) e il
verdetto calcolato dallo strumento con i margini scritti, non a mano; suite completa verde
(`./gradlew test :app:testDebugUnitTest`: nessun file Kotlin toccato, Gradle la dà up-to-date
dall'ultima corsa verde).

## 14.6 — Il caldo (committente, 27 ott 2026)

**Fatto.** Il caldo è un pericolo dell'app: nella frase in cima a Oggi, nella notifica, nei
due riassunti, nelle regole del lettore. Nessun codice del provider lo dice: lo dice la
percepita delle righe (nel modello dal 24 set 2026), e una riga senza percepita non è mai
calda (§1.1).

**Le soglie e le fonti.**

- **DWD, Hitzewarnung**: livello 1 «starke Wärmebelastung» a temperatura percepita
  (Gefühlte Temperatur, modello Klima-Michel) ≥ 32 °C nel primo pomeriggio; livello 2
  «extreme Wärmebelastung» ≥ 38 °C. La percepita di Open-Meteo è `apparent_temperature`
  (vento, umidità, radiazione): non la stessa misura, la stessa idea e la stessa scala (Roma,
  30 lug 2026: aria 37,6, percepita 41,3). I **bollettini del Ministero della Salute** (ondate
  di calore, 27 città, livelli 0–3), il riferimento italiano, restano fuori: nessun feed
  aperto per città.
- **La frase** (`HeadlineEngine`, gradino 3b: dopo l'ombrello, prima del gelo): percepita
  ≥ 32 in un'ora di oggi ancora davanti → «Caldo: percepiti 41° verso le 13:00», il picco e
  la sua ora; già in corso (la percepita del blocco `current`, il numero dell'eroe, ≥ 32) →
  «Caldo fino alle 23:00», la prima ora sotto i 32 entro 24 ore, con «picco 41° verso le
  13:00» finché il picco deve ancora venire ed è almeno un grado sopra adesso (il margine
  della frase calma); senza un'ora sotto i 32 → «Caldo per il resto della giornata».
  «Domani caldo: percepiti 41° verso le 13:00» è l'ultimo gradino (9), dopo l'ombrello di
  domani. Registro breve dei widget: «Caldo, 41° verso le 13:00», «Caldo fino alle 23:00»,
  «Caldo tutto il giorno», «Domani caldo, 41°».
- **La notifica** (`AlertEngine.HEAT`): percepita ≥ 35 per almeno 2 ore nelle prossime 12,
  fra i due livelli DWD, perché la pagina può dire un pomeriggio caldo e deve suonare solo
  un pomeriggio pericoloso. Titolo «Caldo intenso · Roma», corpo «Percepiti fino a 41° verso
  le 13:00.» (il picco e la sua ora dalla finestra letta sulle ore del report, `HeatWindow`;
  il numero dell'avviso quando le ore sono passate; «Caldo intenso in arrivo.» senza né
  l'uno né l'altro). Sull'interruttore «Maltempo in arrivo», la cui riga lo nomina; canale
  suo «Caldo intenso», importanza alta come il temporale, id 1005; impronta
  `<città>:heat:<data della prima ora sopra la soglia>` nel set `heatFingerprints` (chiave
  `alert_fp_heat`, limitata come le altre): una per giornata di caldo e per città, e un
  sondaggio a mezzogiorno dentro il caldo non la ripete. Non silenzia la pioggia: un giorno
  caldo con un temporale alle cinque sono due cose. La quiete 22–7 vale come per tutte.
- **Il corpo aperto**: il disegno del riassunto del mattino (la temperatura del resto del
  giorno sulla sua luce, la forma del caldo), poi «Dalle 10:00 alle 19:00 · Acqua e ombra
  nelle ore centrali» (da 38: «Fuori solo la mattina e la sera, acqua spesso»: le due fasce
  DWD dette come decisioni, `WeatherText.heatMeaning`), «Temperatura da 33° a 38°»,
  «Adesso 20° · Sereno», «UV massimo 8 · …». Il picco non si ripete sotto il disegno: è la
  riga chiusa.
- **I riassunti**: la frase del giorno è quella di Oggi, quindi dice il caldo da sola; quando
  non lo dice (l'ombrello vince), una riga «Percepiti fino a 41° verso le 13:00 · …» al
  mattino per oggi e «Domani percepiti fino a 41° verso le 13:00 · …» la sera, dalla soglia
  della frase (32), mai da una terza.
- **Le regole del lettore**: `next_6h.feels_like_max_c` e `next_12h.feels_like_max_c` («la
  massima percepita delle prossime 6 / 12 ore»), temperatura, in °F con `_f`; l'ora senza
  percepita non conta. Trovato per strada: il picker di `current.feels_like_c` correva su
  0–100 come una probabilità (`RuleText.valueSpec` non conosceva «feels_like»); ora −30…45
  come le temperature.

**La fixture d'estate.** Roma, 30–31 lug 2026, dall'archivio di Open-Meteo
(`historical-forecast-api`, `best_match`, 41,9028 N 12,4964 E, Europe/Rome, letto il 27 ott
2026): i due giorni più caldi del mese per percepita, 41,3 °C alle 13 del 30 (aria 37,6) e
41,0 alle 13 del 31; sopra i 35 dalle 10 alle 19, sopra i 32 dalle 9 alle 22 e nelle prime
due ore della notte. `RomeHeatWave` in `:core:domain` e in `:app` (i test di un modulo non
leggono quelli dell'altro). Sul 30 luglio: alle 7 la frase dice «Caldo: percepiti 41° verso
le 13:00» e la notifica parte ancorata alle 10 con il picco 41,3 e l'impronta del 30; alle 10
«Caldo fino alle 23:00, picco 41° verso le 13:00»; alle 15 «Caldo fino alle 23:00» (le ore
davanti sono più fresche di adesso); alle 23 «Domani caldo: percepiti 41° verso le 13:00»;
alle 18:30 la notifica tace (un'ora sola sopra i 35 nelle dodici) e alle 23 annuncia il 31
con la sua impronta.

**Deviazioni.**

- La frase in corso dice anche il picco finché deve venire (lo step dava solo «caldo fino
  alle 19»): a chi legge alle 10 il picco delle 13 è la notizia; sparisce quando le ore
  davanti sono più fresche dell'eroe, perché «picco 38° verso le 16» sotto un eroe a 39°
  sarebbe la pagina che annuncia il proprio passato.
- Nessun quinto interruttore: il caldo sta sotto «Maltempo in arrivo», che lo nomina nella
  descrizione («… caldo intenso nelle prossime 12 ore»; «al massimo uno per perturbazione,
  uno per giornata di caldo»); la guida di Avvisi e quella di Oggi lo dicono. Un canale suo,
  così si può tacere da solo.
- Il disegno: lo step non ne chiedeva uno; la notifica prende quello del riassunto del
  mattino perché la temperatura del resto del giorno è la forma del caldo, e il fit lo misura
  (`NotificationFitRenderTest`, caso «heat»: IT ed EN, 360/384/411 dp, 1,0/1,15/1,3).
- La Milano registrata non ha caldo (settembre): il caso del fit e la resa alzano le sue ore
  11–18 a una percepita 36–41; solo per misurare la resa, mai per una figura del README.
- La resa: tre corpi (il caldo, il riassunto del mattino e quello della sera con il caldo)
  resi in Robolectric con la configurazione del post e guardati, come nel registro delle
  notifiche (test di prova non committato): la curva, le righe, nessun taglio. Il riassunto
  della sera in inglese sceglie la forma breve «Heat tomorrow, 36°.» perché la lunga non sta
  nella riga chiusa; in italiano sta.
- STATES non cambia: il caldo non è uno stato dell'ora. DESIGN §8.17b, VISION §5.4 e §12
  (decisione 12), CHANGELOG `[Unreleased]`, le stringhe IT/EN.
- Screenshot del README rigenerati con `-PupdateScreenshots` e guardati con `git status`:
  **nessuna immagine cambia**; la riga dell'interruttore «Maltempo», il cui testo cambia, non è
  in nessuna figura.

**Come è stato verificato.** `AlertEngineTest` (la giornata di caldo annunciata una volta,
ancorata alla prima ora, con la percepita peggiore; un'ora sola non basta; la riga senza
percepita; l'interruttore; non silenzia la pioggia), `HeadlineEngineTest` (davanti, in corso
con e senza picco, tutto il giorno, l'ordine dei gradini, domani, la riga senza percepita),
`RuleVariablesTest`, `RuleTextTest`, `AlertDetailsTest` (la finestra del caldo),
`AlertStateStoreTest` (lo slot), `NotificationFitRenderTest` (caso «heat»); le tre rese
guardate; suite completa verde (`./gradlew test :app:testDebugUnitTest`, 2 557 test, 15
saltati: gli screenshot); lint verde (`:app:lintDebug`).

## 14.7 — Il vento nel «Maltempo» (committente, 27 ott 2026)

**Fatto.** Una raffica che tocca la soglia del luogo è «Maltempo»: nella frase in cima a Oggi,
nella notifica, nel banner e nei widget che leggono la stessa frase, nella variabile
`wmo_severe` delle regole del lettore. Nessun codice del provider lo dice: lo dice la raffica
delle righe (`gustKph`, nel modello dal 24 set 2026), letta da `AlertEngine.severeBucket`
accanto al codice, così le quattro superfici lo vedono insieme e nessuna può chiamare burrasca
un'ora che le altre tacciono. Il giorno non cambia parola per il vento: non è uno stato, è un
fatto del giorno, e la riga delle raffiche c'è già.

**Le soglie e le fonti.** Lo step dava «70 km/h sotto i 1 000 m, 90 fra 1 000 e 1 600, 110
oltre», da scrivere qui come tabella MeteoSwiss. La tabella, letta il 27 ott 2026 dal documento
ufficiale «Beschreibung zu den Gefahrenstufen» (MeteoSchweiz, maggio 2021, tabella 3,
«Warnlimiten für Wind mit den entsprechenden Übergangshöhen»), è un'altra:

| Regione | Quota | Livello 2 | Livello 3 | Livello 4 | Livello 5 |
|---|---|---:|---:|---:|---:|
| Altipiano e Giura | < 1 000 m | 70 km/h | 90 | 110 | 140 |
| Altipiano e Giura | ≥ 1 000 m | 100 km/h | 130 | 160 | 200 |
| Prealpi, Alpi e Sud delle Alpi | < 1 600 m | 70 km/h | 90 | 110 | 140 |
| Prealpi, Alpi e Sud delle Alpi | ≥ 1 600 m | 100 km/h | 130 | 160 | 200 |

Due sole soglie per il livello 2 (il primo che avverte il pubblico, «stürmischer Wind»): **70
km/h sotto la linea, 100 sopra**; i 90 e i 110 dello step sono i livelli 3 e 4 della riga di
pianura, non gradini di quota. La linea è una, e si sposta con la regione: 1 000 m in pianura e
nel Giura, dove quella quota è già un crinale; 1 600 m nelle Alpi, dove è ancora un fondovalle.
L'app non sa distinguere un crinale da un fondovalle, e fra le due quote tiene la lettura
alpina: **70 km/h sotto i 1 600 m, 100 da 1 600 m in su** (`WIND_GUST_KPH`,
`WIND_GUST_MOUNTAIN_KPH`, `WIND_MOUNTAIN_FROM_M`, `windGustBar`). Il perché: i paesi fra 1 000
e 1 600 m sono per lo più fondivalle di montagna (Cortina, Bormio, Chamonix, Davos), la soglia
che suona di più è l'errore più sicuro per un pericolo che costa una notifica a giornata di
burrasca, e una città d'altipiano (Ávila, Salt Lake City) riceve un avviso che la riga di
pianura avrebbe taciuto, non un silenzio. DWD avverte di «Sturmböen» (arancione, Bft 8–9) da 65
km/h: i 70 svizzeri sono la più severa delle due soglie nazionali, scelta perché a chi vive al
mare (Palermo, Cagliari) non si dica ogni giorno di vento. **Deviazione dallo step, con il
motivo**: una soglia è un numero con la sua fonte (regola del repo), e la fonte citata dallo
step non contiene i gradini che lo step le attribuiva. VISION §12, decisione 12, corretta.

**La quota del luogo** è quella della risposta (`elevation` di Open-Meteo: la cella della
griglia, o la quota a cui il downscaling ha corretto la temperatura), nel DTO e in
`Location.elevationM`, nullabile, mai indovinata: una voce di cache scritta prima, o un
provider che smette di darla, lasciano la soglia di pianura, quella che suona di più. Nessuna
richiesta e nessuna variabile in più: il campo è nel corpo di ogni risposta già oggi.

**Le parole.** La frase (gradino 1 della scala, con temporale, ghiaccio, pioggia e neve forte):
«Raffiche fino a 112 km/h verso le 00:00», il picco della serie di ore consecutive sopra la
soglia e l'ora in cui comincia; nell'unità del lettore («70 mph»); la stessa frase nel registro
breve dei widget, perché una burrasca non ha una forma più corta. Quando l'ora ha codice di
temporale e raffica insieme, è il temporale: il codice prima, la raffica dopo (`severeBucket`).
La notifica: titolo «Maltempo in arrivo · Trieste», corpo «Raffiche fino a 112 km/h verso le
08:00.» (il picco e la sua ora dalla finestra letta sulle ore del report, `WindWindow`; il
numero dell'avviso quando le ore sono passate; «Raffiche forti in arrivo.» senza né l'uno né
l'altro); canale e importanza del temporale, impronta `<città>:sev:WIND:<data della prima ora>`
nello stesso insieme delle altre: una per burrasca e per città, e una burrasca che dura due
giorni è una notifica. **Il disegno**: le raffiche delle prossime dodici ore come barre
(`NotificationCharts.gustHours`), la scala del mondo 0–140 km/h (il livello 5 di pianura, così
la soglia sta a metà altezza e un uragano riempie il grafico), la soglia del luogo tratteggiata
dove il grafico della pioggia ha il suo 50%, la finestra accesa nell'ambra del temporale, l'ora
più forte etichettata nell'unità del lettore; un inchiostro solo per la quantità, le ore sotto
la soglia in quello quieto. Il corpo aperto: «Dalle 13:00 alle 17:00», «Pioggia fino a 98%, il
peggio verso le 11:00» quando la finestra ha un'ora dal 50% in su (la notifica della burrasca
tace l'ombrello della sua corsa, come quella del temporale: una per perturbazione, quindi porta
il fatto dell'ombrello), «Temperatura da 21° a 23°», «Adesso 20° · Nuvoloso», il vento di adesso.
Le regole: `next_6h.wmo_severe` e `next_12h.wmo_severe` vedono la raffica alla soglia del luogo
(la variabile ora legge il report intero, per la quota). L'interruttore resta «Maltempo in
arrivo», la cui descrizione nomina le raffiche di burrasca.

**Le fixture** (`WinterGales`, in `:core:domain` e in `:app`), dall'archivio di Open-Meteo
(`historical-forecast-api`, `best_match`, letto il 27 ott 2026):

- **Trieste, 23–25 dic 2025** (45,6495 N 13,7768 E, 18 m): una bora, ENE per tre giorni
  (56–67°), 65,2 km/h alle 23 del 23, 73,4 a mezzanotte, **112,0 alle 8 del 24**, ancora oltre
  i 100 alla fine del 25. Alle 18 del 23 l'avviso parte ancorato a mezzanotte, con il 112 e
  l'impronta del 24; alle 8 del 24, già detto, tace; a mezzanotte del 25 tace senza impronta,
  perché la burrasca è una. A 1 700 m suona alle 5 del 24 (103,7), e alle 14 del 23 non suona.
  La frase alle 18 è la burrasca e non l'ombrello delle 19 (73%); a 1 700 m alle 16 è
  l'ombrello.
- **Genova, 22–23 mar 2025** (44,4056 N 8,9463 E, 11 m): il libeccio più forte di due inverni
  d'archivio (ott 2024–apr 2026), 71,6 km/h all'1 del 23 da 203°, un'ora sola sopra la soglia fra
  un 59,8 e un 52,2, tre ore dopo il temporale delle 22 (codice 96, 98%). Alle 20 del 22 l'avviso
  è il temporale; detto quello, alle 23 è il libeccio, con il 71,6 e l'impronta del 23. Gli
  otto giorni più ventosi di Genova nell'archivio sono tutti di tramontana (81,7 il 16 dic 2025):
  il libeccio da 70 è raro, e la fixture lo dice.

**La misura: quante notifiche porta.** Le raffiche di `best_match` ai 26 aeroporti dell'archivio
della nebbia, dic 2025–gen 2026 (scaricate il 27 ott 2026, 26 richieste da 1 variabile e 9
settimane, ≈ 4,5 chiamate l'una), alla soglia di pianura (nessuno dei 26 è sopra i 600 m):

| Aeroporto | Quota | Dic: ore ≥ 70 · giorni · avvisi | Gen: ore · giorni · avvisi | Raffica max |
|---|---:|---|---|---:|
| Milano Linate, Malpensa, Bergamo, Torino, Bologna, Verona, Napoli, Bari | 35–292 m | 0 · 0 · 0 | 0 · 0 · 0 | 25,9–69,5 |
| Venezia Tessera | 1 m | 2 · 1 · 1 | 0 · 0 · 0 | 72,7 |
| Roma Fiumicino | 1 m | 0 · 0 · 0 | 3 · 1 · 1 | 82,8 |
| Palermo Punta Raisi | 10 m | 1 · 1 · 1 | 36 · 6 · 5 | 101,9 |
| Cagliari Elmas | 2 m | 0 · 0 · 0 | 13 · 3 · 3 | 91,4 |
| Catania Fontanarossa | 4 m | 0 · 0 · 0 | 37 · 5 · 3 | 102,2 |
| Zurigo, Monaco, Madrid, Oslo, Vienna, Longyearbyen | 25–593 m | 0 · 0 · 0 | 0 · 0 · 0 | 48,6–68,8 |
| Parigi CDG | 106 m | 0 · 0 · 0 | 8 · 1 · 1 | 96,1 |
| Amsterdam | 5 m | 0 · 0 · 0 | 1 · 1 · 1 | 73,1 |
| Londra Heathrow | 26 m | 2 · 2 · 2 | 0 · 0 · 0 | 71,6 |
| Atene | 79 m | 0 · 0 · 0 | 1 · 1 · 1 | 70,9 |
| Stoccolma Arlanda | 28 m | 7 · 1 · 1 | 0 · 0 · 0 | 74,9 |
| Helsinki | 48 m | 7 · 2 · 1 | 0 · 0 · 0 | 76,7 |
| Keflavik | 48 m | 86 · 11 · 9 | 52 · 7 · 5 | 99,7 |
| **Totale, 26 aeroporti** | | **105 · 18 · 15** | **151 · 25 · 20** | |

«Avvisi» è il numero di impronte: le partenze di corsa (una corsa sopravvive a un'ora quieta,
come per il temporale), una per data. Trentacinque notifiche in due mesi su 26 città, 20 delle
quali a Keflavik, Palermo e Catania; nelle città italiane di pianura e nelle capitali
continentali nessuna. Quattro delle 256 ore sopra soglia (tutte a Palermo) hanno già un codice
di pericolo: la raffica aggiunge, non duplica. Con la soglia DWD (65) le ore sarebbero di più
dove già suona, non altrove: la scelta dei 70 è confermata dalla frequenza.

**Deviazioni.**

- Le soglie: sopra, con il motivo. La scala a tre gradini dello step non ha fonte; quella a due
  della tabella sì.
- Il disegno: lo step non ne chiedeva uno; la notifica senza figura era l'unica del catalogo dei
  pericoli, e le raffiche ora per ora sono la forma della burrasca (quando sale, quando cala)
  come la curva della temperatura è quella del caldo. Il grafico è la geometria di quello della
  pioggia con un'altra quantità, le regole di DESIGN §9 (una tinta, scala del mondo).
- `Alert.bucket`: la classe del pericolo, che l'impronta già contiene, portata in chiaro perché
  il notifier distingua la burrasca dal temporale senza rileggere una chiave; `Alert.gustKph`
  il picco, come `highC` per il caldo.
- `severeBucket(hour)` e `isSevere(hour)` prendono la quota: ogni lettore passa
  `report.location.elevationM`; la versione su codice e probabilità, quella del mapper per
  l'etichetta del giorno, resta com'era, perché il vento non etichetta il giorno.
- Il test «a strong wind later today» di `HeadlineEngineTest` usava 70 km/h come vento forte:
  da oggi 70 è la burrasca, e il test scende a 68 (il gradino 6 della scala, da 60, resta per il
  vento forte sotto la soglia).
- STATES non cambia: la burrasca non è uno stato dell'ora. DESIGN §8.17 e §8.17b, VISION §12
  (decisione 12), CHANGELOG `[Unreleased]`, le stringhe IT/EN.
- Screenshot del README: rigenerati con `-PupdateScreenshots` e guardati con `git status`: **nessuna immagine cambia**; la riga dell'interruttore «Maltempo», il cui testo nomina le raffiche, non è in nessuna figura.

**Come è stato verificato.** `AlertEngineTest` (la soglia per quota; la bora annunciata una
volta, ancorata alla prima ora, con il picco; in montagna da 100; la riga senza raffica; il
codice prima della raffica; il libeccio dopo il temporale; l'interruttore e la pioggia della
corsa), `HeadlineEngineTest` (la burrasca davanti all'ombrello, la soglia alta, la raffica del
temporale), `SevereTextTest` (la frase in km/h e mph, senza numero, il temporale com'era),
`RuleVariablesTest` (`wmo_severe` alla soglia del luogo), `WeatherReportMapperTest` (la quota,
e nulla senza), `AlertDetailsTest` (la finestra del vento), `NotificationFitRenderTest` (caso
«wind»: IT ed EN, 360/384/411 dp, 1,0/1,15/1,3); le quattro rese (IT ed EN, giorno e notte)
guardate in Robolectric come nel registro delle notifiche (probe non committato): la banda, la
soglia tratteggiata a metà, il picco «101 km/h», le quattro righe, nessun taglio; suite completa verde (`./gradlew test :app:testDebugUnitTest`); lint verde (`:app:lintDebug`).

## 14.8 — La quota neve (committente, 28 ott 2026)

**Fatto.** Il giorno aperto della settimana, oggi compreso, ha un fatto in più fra la neve
in centimetri e le raffiche: «Neve oltre i 1 200 m», con la conseguenza per il luogo sotto
(«Qui pioggia, neve sulle strade più in alto» quando la quota sta sopra il luogo, «Neve fin
qui, pioggia più in basso» quando è alla sua quota o sotto); il riassunto della sera porta la
stessa riga per domani («Domani neve oltre i 1 200 m circa · Qui pioggia, neve sulle strade
più in alto»), dopo la pioggia di domani e prima del caldo. Il glifo è la nuvola che nevica
(`mc3_snow`), non il fiocco dei centimetri che le sta sopra.

**Le variabili e il peso.** `freezing_level_height` e `snowfall_height` entrano nelle
variabili orarie (`OpenMeteoForecastApi.HOURLY_VARIABLES`), nel DTO e in `HourlyForecast`
(`freezingLevelM`, `snowfallHeightM`, nullabili, con default per la cache di prima): **54
variabili, 5,4 chiamate la previsione, 8,4 il fetch** (5,2 e 8,2 fino a ieri), scritto accanto
alla lista e in CLAUDE.md. Nessuna richiesta in più. `REQUEST` cambia con la lista, quindi
una voce di cache scritta prima si rimappa senza la riga fino al fetch successivo, mai con un
numero inventato.

**La regola della quota** (`SnowLine`, in `:core:domain`): la quota dell'ora è
`snowfall_height` quando c'è, altrimenti lo zero termico meno **300 m**, e allora la riga
dice «circa»: MeteoSwiss, «Schneefallgrenze», «in der Regel 200 bis 400 Meter unterhalb der
Nullgradgrenze», preso il mezzo della fascia. Una quota sotto il mare è 0 m. La quota del
giorno è la minima delle sue ore bagnate (lo stato del motore, non il codice del provider) e
il giorno la porta solo quando la sua etichetta è di precipitazione, cioè quando
`dailyCode` ha giudicato la precipitazione materiale (≥ 1 mm o ≥ 3 ore, pericoli esenti): una
quota sopra un'ora da 0,1 mm che l'app non crede è un fatto di niente. La riga si disegna
quando la quota sta **entro 1 500 m sopra il luogo o sotto di esso** (`SnowLine.matters`, la
quota del luogo è l'`elevation` della risposta, da 14.7); senza quota del luogo si legge il
mare, e la riga compare solo per una quota entro i 1 500 m, mai per una troppo lontana.
**A schermo la quota è al centinaio di metri** («1.200 m» in italiano, «1,200 m» in inglese,
`Formats.altitude`): la quota neve è una fascia di qualche centinaio di metri, non una curva di
livello, e il 1 720 del modello stampato al metro sarebbe una precisione che la previsione
non ha; il centinaio è quel che i servizi svizzero e italiano stampano. Non è una media né un
numero cambiato: è l'arrotondamento di stampa, come il grado della temperatura.

**La misura dal vivo** (28 ott 2026, `best_match`, Milano 45,4642 N 9,19 E e Bormio 46,4667 N
10,3706 E, 72 ore ciascuna): le due colonne arrivano piene (0 null su 144) in Italia, dove
`best_match` è la famiglia ICON; `elevation` 147 m e 1 204 m. Lo scarto fra lo zero termico e
la quota del modello:

| Luogo | Ore bagnate (≥ 0,1 mm) | Scarto nelle ore bagnate | Scarto su tutte le 72 ore (min · mediana · max) |
|---|---|---|---|
| Milano | 2 (30 ott, 03 e 04) | 220 m, 230 m | 200 · 507 · 870 |
| Bormio | 0 | — | 420 · 970 · 1 870 |

Nelle ore bagnate, le sole che la quota del giorno legge, il modello sta dentro la fascia
200–400 di MeteoSwiss; nelle ore asciutte la sua quota scende molto più in basso (l'aria secca
raffredda il fiocco che sublima), ed è il motivo per cui la stima vale solo dove il modello
non porta la sua quota e si legge solo sulle ore che bagnano. Dove il modello la porta
(l'Italia di ICON) la riga è la sua, senza «circa».

**Deviazioni e scelte non scritte nello step.**

- «Entro 1 500 m sopra il luogo o sotto di esso»: letto come una condizione sola, quota ≤
  luogo + 1 500, senza limite in basso. Sotto il luogo la neve è la sua e lo stato lo dice
  già, ma la riga dice dove torna pioggia, che lo stato non dice: a Bormio «Neve oltre gli
  800 m» è un fatto della sua valle.
- La seconda riga: lo step non la dava; «Every number says what to do with it» la vuole, e
  sono due frasi, sopra e sotto la quota del luogo (`WeatherText.snowLineMeaning`), le stesse
  nel giorno aperto e nella sera.
- Il centinaio di metri a schermo: sopra, con il motivo.
- La quota del giorno la calcola il mapper, non la schermata: ha le ore con lo stato del motore
  e il giudizio di materialità già fatto per l'etichetta, e la sera la legge dallo stesso campo
  (`DailyForecast.snowLine`), così le due superfici non possono leggere due quote.
- STATES non cambia: la quota neve non è uno stato dell'ora né del giorno, e il motore non la
  legge. DESIGN §8.5b, CHANGELOG `[Unreleased]`, le stringhe IT/EN.
- Screenshot del README: **nessuno rigenerato**. La Milano registrata (`forecast.json` del 25
  set) non ha le due colonne, quindi nessuna immagine può mostrare la riga, e per regola si
  rigenera solo se un'immagine cambia; si rigenererà quando la registrazione sarà rifatta per
  un altro motivo.

**Come è stato verificato.** `SnowLineTest` (la quota propria vince e non è stima; lo zero
termico meno 300 è «circa»; niente senza nulla, il mare a zero; la minima tiene la sua
provenienza; i 1 500 m sopra e il sotto; il raggiungere), `WeatherReportMapperTest` (la riga
porta zero termico e quota del suo slot, la stima e la propria; la quota del giorno è la
minima delle ore bagnate, non dell'ora asciutta più bassa; un giorno che la pioggia non
etichetta non la porta; la risposta senza colonne la lascia nulla), `EveningDetailsTest` (la
quota di domani e non di oggi, quando conta al luogo; troppo alta o giorno asciutto, niente;
a Bormio la stessa quota conta), `DayFactsTest` (Compose, in `testDebug`: «roughly» e la
pioggia qui; la propria al centinaio senza «roughly»; sotto il luogo «Snow down to here»;
troppo alta non si disegna e gli altri fatti restano); suite completa verde
(`./gradlew test :app:testDebugUnitTest`); lint verde (`:app:lintDebug`).

## 14.9 — Le unità (committente, 28 ott 2026)

**Fatto.** Il vento si stampa in cinque unità e la pressione in tre, a scelta nelle
impostazioni («Unità», una riga «Pressione» nuova sotto «Vento»): km/h, mph, m/s, nodi e la
scala Beaufort; hPa, pollici di mercurio, millimetri di mercurio. I default restano km/h e hPa,
e chi non tocca niente non vede niente di diverso. `WindSpeedUnit` guadagna `M_S`, `KNOTS`,
`BEAUFORT`; `PressureUnit` è nuovo (`HPA`, `INHG`, `MMHG`), con la sua chiave
`units_pressure` nello store; `UnitSettings.pressure`. I motori non cambiano: tengono km/h e
hPa, e `Units` (`:core:domain`, nuovo) è l'aritmetica in un posto solo, letta da `Formats` e dal
registro delle regole, così le due superfici non possono convertire in due modi.

**Le forme.** `Formats.wind`: «18 km/h», «11 mph», «5 m/s», «10 kn», interi (§5, «wind to whole
units»); Beaufort è **la forza più la parola della scala**, «6 Bft (strong breeze)», «6 Bft
(vento fresco)», il numero essendo la fascia in cui cadono i km/h e la parola quella del Met
Office in inglese e della scala italiana in italiano; `short = true` dà la forza sola, «6 Bft»,
per il valore del tile e l'etichetta del grafico. `Formats.pressure`: «1013 hPa», «29.92 inHg»
(al centesimo: è la precisione a cui quell'unità si legge, un decimo sono 3,4 hPa), «760 mmHg».
`Formats.pressureChange`, nuovo, per la tendenza del barometro, firmata e nell'unità («+2,4 hPa»,
«−0,07 inHg», «+1,8 mmHg»): la stringa `pressure_trend_note` perde il suo «hPa» scritto, l'unità
viaggia con il numero.

**La scala Beaufort.** I limiti inferiori in km/h delle tredici forze sono quelli della scala
WMO come i servizi la stampano in km/h: 0, 1, 6, 12, 20, 29, 39, 50, 62, 75, 89, 103, 118. Due
di quei bordi erano già nella scala del tile senza saperlo: «tieni il cappello» dai 39 km/h è
la forza 6, la raffica del giorno che vale una riga dai 62 è la forza 8. Le parole inglesi sono
i «descriptive terms» della pagina del Met Office (letta il 28 ott 2026: calm, light air, light
breeze, gentle breeze, moderate breeze, fresh breeze, strong breeze, near gale, gale, strong
gale, storm, violent storm, hurricane); per la forza 9 la pagina stessa dice che il termine
ufficiale è «strong gale» e «severe gale» è l'uso di casa del Met Office, e l'app scrive quello
ufficiale. Le italiane sono quelle della scala italiana (calma, bava di vento, brezza leggera,
brezza tesa, vento moderato, vento teso, vento fresco, vento forte, burrasca, burrasca forte,
tempesta, fortunale, uragano): per la 11 «fortunale», la parola sola, non «tempesta violenta».

**Le regole del lettore.** Gli id mostrati si generalizzano: `_kph` ↔ `_mph`, `_ms`, `_kn`,
`_bft`; `_mb` ↔ `_inhg`, `_mmhg` (`RuleVariableKind.PRESSURE` nuovo, per `current.pressure_mb`,
che era un numero senza unità). **I canonici restano `_kph` e `_mb`**: `canonicalId` riporta
ogni ortografia a quelli, una regola salvata in nodi è la stessa regola in km/h, e un messaggio
scritto con `{today.gust_max_kn}` si risolve come prima. La soglia e la lettura dicono l'unità
del lettore con `RuleText.unitSuffix` (« kn», « Bft», « inHg»...), e in Beaufort la parola della
scala dopo la forza, come l'eroe; una forza di cui l'autore ha già scritto «Bft» nel messaggio
resta la sua riga, senza parola. `formatValue` scrive una forza intera e i pollici al centesimo
(`Units.speedDecimals`, `pressureDecimals`). `canonicalValue` di una forza è il limite inferiore
della fascia: «almeno 6 Bft» è «almeno 39 km/h», la soglia che la parola vuol dire.

**Le superfici.** Il tile del vento stampa la forza sola come valore (il tipo `readingValue`
a 24 sp non ha posto per «6 Bft (strong breeze)» in mezza colonna) e la parola apre la nota,
«Strong breeze · Gusts up to 8 Bft (gale)», unita con il punto mediano come la riga del vento
delle notifiche; il tile della pressione e la sua nota, l'eroe («Raffiche fino a 11 Bft
(fortunale) verso le 00:00»), il fatto del giorno, le notifiche (la riga del vento, il corpo del
maltempo, la descrizione del grafico) passano tutti da `Formats` e seguono l'unità da soli;
l'etichetta sopra la barra del grafico delle raffiche usa la forma corta. **I widget non
stampano né vento né pressione**: la frase del widget è l'eroe (`HeadlineText`), che eredita.

**Scelte non scritte nello step.**

- Le parole Beaufort stanno in `Formats`, in Kotlin, non in `strings.xml`: sono parte del
  formato del numero, e `Formats` stampa senza dispositivo, con la lingua come parametro, che
  è quel che lo step chiede di provare («test di Formats per ogni unità e locale»). Le due
  lingue sono le due che l'app spedisce; un'altra legge l'inglese, come ogni stringa.
- Il simbolo del nodo è «kn» (ISO 80000) in entrambe le lingue, non «kts» né «nodi» scritto:
  l'impostazione dice «Nodi (kn)» e da lì in poi il numero porta il simbolo.
- Lo slider della soglia tiene il suo passo canonico (5 km/h, 5 hPa) e l'etichetta converte:
  in Beaufort l'etichetta cambia ai bordi delle fasce e fra un bordo e l'altro resta ferma; in
  pollici si muove di 0,15 per passo. È il comportamento che mph aveva già (3,1 mph per passo)
  e la soglia salvata è sempre in km/h e hPa.
- `pressure_trend_note` cambia di testo in IT ed EN (l'unità esce dalla stringa).

**Come è stato verificato.** `FormatsTest`: ogni unità del vento e della pressione in
inglese e in italiano (interi, il segno, «kn», i pollici al centesimo con la virgola italiana,
760 e 29,92 per l'atmosfera standard), le tredici fasce Beaufort ai loro bordi (0,9 → 0; 1 → 1;
38,9 → 5; 39 → 6; 62 → 8; 118 → 12; 250 → 12), le parole nelle due lingue e il tedesco che legge
l'inglese, la forma corta, la tendenza firmata nelle tre unità. `RuleVariablesTest`: gli id
mostrati e il ritorno al canonico per le cinque unità del vento e le tre della pressione, senza
che l'una tocchi i nomi dell'altra; conversione e andata-ritorno in ogni unità; la forza come
limite inferiore (6 → 39, 12 e 14 → 118); i decimali per unità. `RuleTextTest`: ogni ortografia
di un segnaposto si risolve in un messaggio. `SevereTextTest`: l'eroe in m/s, nodi e Beaufort
con la parola italiana. `SettingsStoreTest`: il default hPa e la scrittura. Suite completa verde
(`./gradlew test :app:testDebugUnitTest`); lint verde (`:app:lintDebug`). Screenshot del README
rigenerato e guardato: `settings.png` (la riga «Pressure · Hectopascals (hPa)» sotto «Wind»);
nessun'altra immagine cambia, i default sono quelli di prima. DESIGN §5 (la riga degli
arrotondamenti), CHANGELOG `[Unreleased]`, le stringhe IT/EN.

## 14.10 — La concordanza dei modelli: la misura (committente, 28 ott 2026)

**Il criterio, scritto prima di guardare i numeri** (come in 14.4: questa parte è committata
prima che lo strumento scaricasse una corsa dei testimoni).

*La domanda.* Lo step la scrive: uno scarto grande fra i modelli predice un errore grande di
`best_match`? Con quale soglia la parola «i modelli non concordano» è vera più di due volte su
tre? Se la risposta è no, 14.11 non esiste.

*Le corse.* Il committente chiede le corse intere per giorno di anticipo, non le prime ore:
l'archivio delle previsioni (`historical-forecast-api`) è la prima parte di ogni corsa, cioè il
giorno 1 soltanto (14.4). Lo strumento chiede quindi alla **Previous Runs API**
(`previous-runs-api.open-meteo.com`, fra quelle che CLAUDE.md lascia agli strumenti di misura)
`temperature_2m` e `cloud_cover` in `previous_day0` … `previous_day6` per `best_match` e per i
quattro testimoni dell'app (IFS, UKMO, KNMI e DMI HARMONIE Europe: quelli che 14.11 leggerebbe
senza una richiesta in più), per i 26 aeroporti e i due periodi (ottobre 2026 = 26 set–25 ott;
inverno = 1 dic 2025–31 gen 2026). Il **giorno N** dell'app è `previous_day(N−1)`: quel che la
corsa di N−1 giorni prima diceva per le ore di quel giorno (14.4: a Roma il 10 dic 2025
`previous_day0` di `best_match` è uguale all'archivio ora per ora; qui si verifica su ogni
aeroporto). Sondato il 28 ott a Linate: `best_match` e IFS rispondono fino a `previous_day7`,
UKMO fino a `previous_day6` (giorno 7), ICON-EU fino a `previous_day4`, **KNMI e DMI solo
`previous_day1`** (giorno 2): dal giorno 3 i testimoni sono IFS e UKMO, e lo sono anche
nell'app (le due HARMONIE corrono tre giorni). Gli strati non ci sono in nessun giorno
(14.4): il cielo si misura sulla copertura totale per tutti, come la regola 6 fa dove gli strati
mancano («with no layers it is the total»). Una variabile per modello: `best_match` al giorno N
è `best_match` com'era servito N−1 giorni prima, con dentro il modello che Open-Meteo aveva
scelto allora, niente altro.

*Un giorno, una riga.* Per aeroporto, periodo, giorno di calendario (UTC: la massima d'Europa
sta nel pomeriggio, dentro il giorno UTC a ogni longitudine da Keflavik a Helsinki) e giorno di
anticipo 1–7:

- **la massima** di ogni modello è il massimo delle sue 24 ore; quella **osservata** è il
  massimo dei METAR del giorno (`tmpf` in °C, interi), con almeno 18 ore su 24 coperte da un
  METAR entro 30 minuti, altrimenti il giorno non conta. Lo **scarto** è la massima più alta meno
  la più bassa fra `best_match` e i testimoni di quel giorno di anticipo; l'**errore** è
  |massima di `best_match` − massima osservata|.
- **il cielo**: sulle ore del giorno in cui il sole sta sopra i 15° e il satellite ha la
  radiazione, senza precipitazione né nebbia nel METAR (il filtro di 14.4), almeno tre; il
  cielo del giorno di ogni modello è la **media della copertura totale** su quelle ore; lo
  **scarto** è la media più alta meno la più bassa, in punti; l'**errore** di `best_match` è la
  quota di quelle ore in cui il suo cielo (20/50/80 sulla copertura, come la regola 6 senza
  strati) non è quello del satellite (sole / un po' di sole / niente sole, 0,7 e 0,4
  dell'indice di chiarezza, §11).
- **I doppioni restano fuori**, come nell'app: un testimone che in quel giorno di anticipo ha la
  temperatura entro 0,05 °C e la copertura entro 0,5 punti di `best_match` in ogni ora in cui
  entrambi ne hanno una, con almeno 12 ore, è `best_match` stesso e non allarga lo scarto (il
  test di `Witnesses.copies`, sulle ore del giorno invece che su ±6). Un giorno senza almeno un
  testimone non doppione non ha scarto e non conta.

*La parola e la verità.* «I modelli non concordano» si dice quando lo scarto è ≥ S. È **vera**
quando l'errore poi osservato è **grande**: sulla massima **≥ 2 °C**, il doppio della soglia con
cui «Che cosa è cambiato» già racconta una revisione (`ForecastDiff`, 1 °C) e la differenza che
un lettore sente addosso; sul cielo, `best_match` sbagliato in **più di metà** delle ore
misurabili del giorno (la parola del giorno sbagliata più spesso che giusta). Per S si provano i
gradi interi da 1 a 6 sulla massima e 20, 30, 40, 50, 60 punti sul cielo. Per ogni giorno di
anticipo, periodo e insieme si stampano: i giorni, il tasso di base (quanto spesso l'errore è
grande senza dire nulla), quanti giorni direbbero la parola a quella S, la **precisione** (quanto
spesso la parola detta è vera), quanto spesso l'errore è grande quando la parola non si dice, il
MAE della massima nei due gruppi, e la correlazione di Spearman fra scarto ed errore. Una tabella
con l'errore ≥ 3 °C si stampa accanto per leggere, non per decidere.

*La decisione*, dichiarata ora. 14.11 esiste se, sui **giorni 3–7 messi insieme** (la riga che
14.11 stamperebbe parte dal terzo giorno), su tutti gli aeroporti, c'è una soglia S sulla
massima tale che:

- la precisione supera **i due terzi**, nei due periodi presi uno per uno e nell'insieme (come
  14.4 chiedeva entrambi i periodi);
- la precisione supera il tasso di base di **almeno 15 punti**: una parola vera due volte su tre
  dove l'errore è grande due volte su tre anche in silenzio non dice niente;
- la parola si dice in **almeno il 5% dei giorni**: una soglia che non scatta mai è una riga
  che non esiste.

La frase complementare, «I modelli concordano», entra in 14.11 solo se a quella S anche lei è
vera più di due volte su tre (errore < 2 °C quando lo scarto è sotto la soglia), in entrambi i
periodi; altrimenti 14.11 dice solo la divergenza. Il cielo passa o fallisce da solo con le
stesse tre condizioni sulle sue soglie, e la sua parola entra in 14.11 solo se passa; 14.11
esiste se passa almeno la massima. I giorni 1 e 2 si stampano per sapere, non per decidere, e
dopo i numeri non si cercano sottoinsiemi (un aeroporto, un mese, un giorno solo) in cui la
parola funzioni. Non si decide sulla ≥ 3 °C.

*Il peso.* Una richiesta per modello (le colonne senza suffisso quando risponde un modello solo,
14.4): 14 variabili, 6,2 chiamate pesate d'inverno e 3,0 a ottobre, 5 modelli × 26 aeroporti,
**~1 200 chiamate pesate** in tutto, in locale (il runner a 45 s a richiesta, registro 14.2).
Nessuna richiesta dell'app cambia: la misura legge quel che l'app scarica già.

**Fatto.** La misura dice **no**: a nessuna soglia che scatti in più dell'1% dei giorni la parola
«i modelli non concordano» è vera più di due volte su tre, né sulla massima né sul cielo.
**14.11 non esiste.** Nessun codice dell'app cambia.

**I dati.** I due archivi portano ora, per i 26 aeroporti, le corse di `best_match`, IFS,
UKMO, KNMI e DMI per i giorni 1–7 (`--add-witness-runs`, 14 colonne a modello sotto
`previous_runs`, accanto ai giorni 2–3 italiani di 14.4): 1 196 chiamate pesate in 30 minuti
in locale, due corpi vuoti (Heathrow e Madrid d'inverno) presi al secondo passaggio come in
14.4. KNMI e DMI non coprono Palermo, Cagliari, Catania, Madrid, Atene e Longyearbyen (400, segnati).
`previous_day0` di `best_match` è uguale all'archivio ora per ora **in ogni aeroporto**: il
giorno 1 delle corse è il giorno 1 della misura. Giorni con la massima: **2 382 per giorno di
anticipo** (1 611 d'inverno, 771 a ottobre); con il cielo **680**, ottobre soltanto, perché
l'archivio del satellite parte da febbraio 2026 (la casella aperta «Il cielo d'inverno»).
Testimoni al giorno 1: quattro in 19 aeroporti, due fuori dal dominio HARMONIE, tre a Keflavik
(DMI è `best_match`), uno a Longyearbyen (IFS lo è); **dal giorno 3 sono IFS e UKMO ovunque, e
uno solo a Keflavik, Helsinki, Heathrow, Amsterdam, Oslo, Longyearbyen e Stoccolma**, dove
`best_match` è uno dei due: lì lo scarto è fra due modelli. Lo scarto mediano sulla massima nei
giorni 3–7 è 1,4 °C, il novantesimo percentile 3,3.

**La tabella della massima**, giorni 3–7 messi insieme, 11 910 giorni, errore grande = la
massima di `best_match` a ≥ 2 °C dal METAR (il tasso di base è 26%: 28 d'inverno, 21 a
ottobre; ρ di Spearman fra scarto ed errore 0,27):

| Soglia S | Detta | Vera (inverno / ottobre) | Sopra la base | «Concordano» vera |
|---|---:|---:|---:|---:|
| ≥ 1 °C | 67% | 31% (34 / 26) | +5 | 85% |
| ≥ 2 °C | 30% | 44% (46 / 38) | +18 | 82% |
| ≥ 3 °C | 13% | 56% (58 / 50) | +30 | 78% |
| ≥ 4 °C | 6% | 61% (63 / 58) | +36 | 76% |
| ≥ 5 °C | 3% | 64% (63 / 66) | +38 | 75% |
| ≥ 6 °C | 1% | 68% (65 / 94) | +43 | 75% |

Per giorno di anticipo (entrambi i periodi): tasso di base 15, 20, 26, 31, 38% dal giorno 3 al
7; a S ≥ 3 °C la parola è vera 48, 59, 55, 56, 56%; ρ fra 0,20 e 0,26. Con l'errore a ≥ 3 °C
(letta, non decisa) la parola non supera il 49%. Per aeroporto, giorni 3–7, a S ≥ 3 °C: vera
dal 20% di Catania al 76% di Helsinki, sopra i due terzi solo a Helsinki, Malpensa, Caselle e
Bari, e non è un sottoinsieme che si va a cercare (il criterio lo vieta).

**La tabella del cielo**, giorni 3–7, ottobre, 3 400 giorni, errore grande = `best_match`
sbagliato in più di metà delle ore di luce misurabili (tasso di base **51%**: sulla copertura
totale il cielo di un giorno lontano è una moneta; ρ 0,24):

| Soglia S | Detta | Vera | Sopra la base | «Concordano» vera |
|---|---:|---:|---:|---:|
| ≥ 20 punti | 62% | 57% | +5 | 57% |
| ≥ 30 punti | 46% | 59% | +8 | 56% |
| ≥ 40 punti | 32% | 63% | +11 | 54% |
| ≥ 50 punti | 20% | 66% | +15 | 52% |
| ≥ 60 punti | 12% | 68% | +17 | 51% |

**Perché no, oltre il verdetto.** Lo scarto **sa qualcosa**: l'errore medio della massima
raddoppia quando i modelli divergono (giorni 3–7: MAE 3,1 °C con uno scarto ≥ 4 °C contro 1,4
sotto; 2,3–2,8 contro 1,0–1,7 a ≥ 3 °C, giorno per giorno), e la correlazione c'è in ogni
giorno e in ogni periodo. Ma dice **quanto si sbaglia in media**, non **se questo giorno è
sbagliato**: a 3 °C di scarto, la soglia che una frase stamperebbe, la massima poi è entro i
2 °C quasi una volta su due; a 6 °C, dove la parola diventa vera due volte su tre, scatta un
giorno su cento e d'inverno resta al 65%. Una riga nel giorno aperto è un'affermazione su quel
giorno, e sarebbe falsa più spesso di quanto l'app accetta per un verdetto. La frase inversa,
«i modelli concordano», è vera dal 75 all'85%, ma è il tasso di base rovesciato (74%): dirla non
aggiunge niente. Sul cielo il tasso di base del 51% toglie il terreno sotto a ogni soglia:
a 60 punti la parola è vera al 68% su ottobre solo, e il criterio chiedeva entrambi i periodi;
in ogni caso la parola del cielo entrava solo con la massima.

**Deviazioni.**

- **La Previous Runs API chiama il giorno 0 con il nome nudo** (`temperature_2m`, non
  `temperature_2m_previous_day0`): nel blocco `previous_runs` il giorno 1 è `<variabile>_<modello>`
  e i giorni 2–7 `<variabile>_previous_day<N−1>_<modello>`; l'analisi lo sa (`run_series`).
- **Il cielo ha un periodo solo**, ottobre: la condizione «in ogni periodo» non era verificabile
  per il cielo, e la riga sta nel registro com'è. Non cambia l'esito.
- **Il cielo dei giorni 1–2 è sulla copertura totale** anche dove l'archivio ha gli strati, per
  non misurare il giorno 1 con un metro diverso dai giorni 3–7 (la riga di 14.11 partiva dal
  terzo).
- **Niente CHANGELOG, niente stringhe, niente screenshot**: nulla raggiunge lo schermo. STATES
  §12, l'ultima voce («What they may say on screen»), riceve l'esito; il workflow «Measure
  against METAR» esegue il modo nuovo nel passo di completamento, idempotente come gli altri.

**Come è stato verificato.** Il criterio sta nel commit precedente a ogni corsa scaricata;
`previous_day0` uguale all'archivio in 26 aeroporti su 26; `analyze_against_metar.py` sui due
file completati, con le sezioni di 14.2 e 14.4 rilette identiche; suite completa verde
(`./gradlew test :app:testDebugUnitTest`, nessun file Kotlin toccato).

## 14.12 resta in attesa (committente, 28 ott 2026)

**Deciso.** Lo step 14.12, gli avvisi ufficiali fuori dall'Italia, **non si fa ora** e resta
nella lista «Aperto» in attesa del via libera, che arriverà se ci sarà una maggiore esigenza:
una base di lettori in Europa, o un posizionamento sullo store che voglia dire «Europa».

**Perché.** Riletta la Fase 12 (archivio 02, «Fase 12 — Allerte ufficiali, secondo strato»): la
copertura di MeteoAlarm è europea, non mondiale, e fuori dall'Europa non esiste una fonte
gratuita senza chiave; la strada è nota (il prototipo dell'11 set 2026 era completo e fu
scartato per il «fallire aperto», non per la difficoltà) e le due regole che la renderebbero
fondibile sono scritte nello step; l'Italia, dove sono i lettori, non passerebbe da nessun
codice nuovo. Quel che pesa è la manutenzione che dura quanto l'app: trenta servizi nazionali
indipendenti, 17 feed su 39 mai osservati, tre paesi senza geometrie, un audit a ogni sonda.
Rimandare non costa nulla: la schermata Avvisi dice già onestamente dove non copre.

**Che cosa cambia nel piano.** Niente: la casella resta aperta, con la data e il motivo; la
Fase 14 non ha altri step da fare. Nessun codice, nessuna stringa, nessun CHANGELOG.

## Cielo: gli strati delle nuvole nella card «Stanotte» (committente, 28 ott 2026)

**Da dove nasce.** Una domanda del committente: il motore che disegna il cielo nell'eroe di
«Oggi» è lo stesso che disegna lo stage di «Stanotte», e le nuvole sono rese allo stesso modo?
Il motore sì: tutte e due le superfici passano per `SkyShaderLayer` e la scena di `skyScene`
(lo stage attraverso `WindowScene.frame`, il costruttore del widget finestra). Le nuvole no:
l'eroe riceve gli strati dell'ora (`cloudLayers`, da `TodayUiState`) e il widget finestra pure
(`WindowPainter`, dall'ora della serie), mentre la card passava `layers = null`, perché
`NightHour` portava solo copertura e probabilità. Con `null` `cloudDecks` fa di tutta la
copertura il ponte basso: una notte di cirri al 70% era disegnata come un banco di cumuli al
70%, mentre la parola sotto l'eroe, dal 26 ott 2026, legge la stessa ora dagli strati
(`CloudLayers.lightTaken`). Due letture dello stesso cielo, su una card che risponde alla sola
domanda «si vedranno le stelle?», dove un velo alto e un ponte basso sono risposte diverse:
stelle, pianeti e Via Lattea in `skyScene` pesano la copertura proprio con gli strati.

**Deciso.** Entrano **solo gli strati**: la serie oraria li ha già, scaricati e in cache per
tutta la settimana, e la card li scartava. `NightHour` porta `layers: CloudLayers?`,
`nightHours` lo riempie dall'ora, la card interpola i tre strati tra le due ore che contengono
l'istante come già fa con la copertura (`betweenHours`, una molla per ciascuno) e li passa a
`WindowScene.frame`. Un'ora che il modello non ha diviso (strati nulli o tutti a zero) si legge
come tutta copertura bassa (`layerShares`): è esattamente quel che lo shader fa di un'ora senza
strati (`cloudDecks`: ponte = copertura, velo zero; `lightTaken`: la copertura), così tra un'ora
divisa e una non divisa il ponte scorre e non si abbassa a metà strada. «Behind the clouds»
azzera anche gli strati, come copertura e probabilità. Niente cambia nei numeri, nella striscia
(che resta sulla copertura totale) e nel verdetto.

**Fuori, con il motivo.** La pioggia: l'ora futura non ha la quantità (la serie oraria non porta
i millimetri; l'eroe usa il quarto d'ora di `current`), quindi si disegnerebbe sempre
l'intensità minima, una tinta che non dice niente; se piove la copertura è già piena e il
verdetto già no; la probabilità è già nelle righe e nella striscia. Il vento: i dati ci sono e
`frame` li accetta, ma nuvole che corrono a velocità diverse su una card che si gira col dito
non sono un'informazione che il lettore veda. Restano fuori finché non c'è una ragione visibile.

**Che cosa non si tocca.** L'eroe di «Oggi» (`TodayHero`, `TodayUiState`), lo shader,
`skyScene`, `SkyCanvas` e il widget finestra: nessuna riga. I file cambiati sono `SkyUiState.kt`
(il modello e `nightHours`) e `TonightSky.kt`; DESIGN §3.9 (il paragrafo dello stage) e il
CHANGELOG lo registrano.

**Verifica.** `TonightNightTest`: le quote di un'ora divisa sono quelle del modello, quelle di
un'ora non divisa tutta copertura bassa, e lette da `cloudDecks` e `lightTaken` danno il
risultato di «nessuno strato»; tra un'ora di velo al 70% e un'ora non divisa al 70% a metà ora il
ponte e il velo sono 0,35 e 0,35, agli estremi (0; 0,7) e (0,7; 0); `nightHours` porta gli
strati dell'ora. Suite completa verde (2 620 test). Screenshot `sky-tonight.png` reso sul branch e su
`main` (worktree) e confrontato con PIL: zero pixel diversi. La registrazione di Milano è del
mattino, la card disegna il centro della notte (01:15 del 26 set 2026), dove l'ora ha nuvole
basse al 100% e strati alti a zero: la lettura a strati e quella senza coincidono, l'immagine
non cambia e non viene rigenerata.
