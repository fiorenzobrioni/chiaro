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
- **In corso**: **Fase 14**, il motore contro i motori premium (dal 27 ott 2026): tredici step, uno per chat; i primi quattro (14.1–14.3 con la scadenza del 30 novembre, la visibilità ICON che sparisce; 14.4 la misura di ICON-2I, che ha detto no) e il 14.13 (le icone, prima dell'inverno) sono chiusi; 14.5 non esiste. La revisione, le decisioni, le regole comuni e la descrizione di ogni step sono in `docs/planning/07-ottobre-22-27.md`, «Fase 14 — Il motore contro i motori premium»; uno step chiuso scrive la sua sezione qui.

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
- [ ] **14.6** Il caldo: il gradino della frase e il pericolo.
- [ ] **14.7** Il vento nel «Maltempo», con la quota del luogo.
- [ ] **14.8** La quota neve.
- [ ] **14.9** Le unità: m/s, nodi, Beaufort, inHg, mmHg.
- [ ] **14.10** La concordanza dei modelli: la misura.
- [ ] **14.11** La concordanza a schermo (solo se 14.10 lo dice).
- [ ] **14.12** Gli avvisi ufficiali fuori dall'Italia, a chiusura garantita: su via libera del committente.
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
