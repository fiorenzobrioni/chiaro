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
- **In corso**: **Fase 14**, il motore contro i motori premium (dal 27 ott 2026): tredici step, uno per chat; i primi tre, con la scadenza del 30 novembre (la visibilità ICON che sparisce), e il 14.13 (le icone, prima dell'inverno) sono chiusi. La revisione, le decisioni, le regole comuni e la descrizione di ogni step sono in `docs/planning/07-ottobre-22-27.md`, «Fase 14 — Il motore contro i motori premium»; uno step chiuso scrive la sua sezione qui.

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
- [ ] **14.4** ICON-2I contro ICON-EU e ICON-D2: la misura.
- [ ] **14.5** ICON-2I nel report, dove ha vinto (solo se 14.4 lo dice).
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

