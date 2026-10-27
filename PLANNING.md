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
- **In corso**: **Fase 14**, il motore contro i motori premium (dal 27 ott 2026): tredici step, uno per chat, con il 30 novembre come scadenza dei primi tre (la visibilità ICON che sparisce).

## Aperto

Quel che resta da fare o da decidere, in un posto solo: una casella si apre qui, con il rimando
alla sezione del registro da cui nasce, e si chiude qui, con quella che la chiude.

- [ ] **Il cielo d'inverno contro il satellite.** La regola 6 è misurata su ottobre soltanto:
  l'archivio del satellite di Open-Meteo parte da febbraio 2026. Rilanciare
  `tools/measure_against_metar.py` su dicembre–gennaio 2026-27 e rileggere STATES §11 (registro,
  «Il motore dopo la revisione», 26 ott 2026).
- [x] **14.1** La sonda del provider, il quarto d'ora dove è calcolato, le regole (Fase 14). Fatto il 27 ott 2026 (registro, «14.1»).
- [ ] **14.2** La nebbia senza la visibilità di ICON: la misura. Entro metà novembre.
- [ ] **14.3** La nebbia senza la visibilità di ICON: il codice, e il tile. Entro il 25 novembre.
- [ ] **14.4** ICON-2I contro ICON-EU e ICON-D2: la misura.
- [ ] **14.5** ICON-2I nel report, dove ha vinto (solo se 14.4 lo dice).
- [ ] **14.6** Il caldo: il gradino della frase e il pericolo.
- [ ] **14.7** Il vento nel «Maltempo», con la quota del luogo.
- [ ] **14.8** La quota neve.
- [ ] **14.9** Le unità: m/s, nodi, Beaufort, inHg, mmHg.
- [ ] **14.10** La concordanza dei modelli: la misura.
- [ ] **14.11** La concordanza a schermo (solo se 14.10 lo dice).
- [ ] **14.12** Gli avvisi ufficiali fuori dall'Italia, a chiusura garantita: su via libera del committente.
- [ ] **14.13** Le icone che mancano: pioggia e pioviggine gelata, pioggia e neve possibile, composte. Dopo 14.3.

Fino al 26 ott 2026 nessuna casella aperta e nessuna proposta non decisa (riletto il 22 ott 2026, all'archivio 06):
le ultime chiuse quel giorno stanno in `docs/planning/06-ottobre-20-22.md`, «Cielo: la finestra
reale nella card Stanotte», «Il widget «Momenti del cielo» va via», «Le notifiche: ogni riga nella
forma che sta in una riga», «Il widget «Le prossime ore» va via».

---

## Registro dal 22 ott 2026, dopo l'archivio 06

## La finestra: la fascia della didascalia nel colore del cielo (committente, dal telefono, 22 ott 2026)

**Da dove nasce.** Tre screenshot della finestra (Los Angeles coperto, Londra «dietro le
nuvole», Cavenago nuvoloso) e la domanda: la fascia in basso, grigia con il sereno, è voluta?
Sì: lo scrim della didascalia del 20 ott, gli ultimi 70 dp dal trasparente al nero al 50% sul
bordo. Sotto un coperto si confonde con le nubi; sopra il sereno il nero al 50% su un
orizzonte pallido dà un grigio neutro, ed è quello che si stacca dal disegno.

**La misura che ha cambiato la domanda.** Il contrasto del bianco alle due righe della
didascalia, misurato sulle rese della Milano registrata (95° percentile della luminanza su
tutta la larghezza delle righe): di giorno **2,2–3,1:1**, sotto il 4,5 di §3.6 che il resto
dell'app tiene; di notte 9,4 e 14,6. La fascia non era troppo forte: era troppo debole per
il contratto. Rese e numeri per tre varianti oltre a quella di oggi:

| Cielo | Oggi | A | B | C |
|---|---|---|---|---|
| Sereno | 2,2 / 3,0 | 5,1 / 4,9 | 5,0 / 4,8 | 2,2 / 2,9 |
| Poco nuvoloso | 2,4 / 3,1 | 5,0 / 4,9 | 4,6 / 4,5 | 2,4 / 3,1 |
| Coperto | 2,2 / 3,0 | 4,8 / 4,9 | 4,5 / 4,6 | 2,2 / 3,0 |
| Notte | 9,4 / 14,6 | 7,6 / 11,5 | 7,6 / 11,5 | 9,6 / 13,9 |

A: l'inchiostro dell'eroe (`#101216`) su una rampa lunga dal 45% dell'altezza, piena dove
cominciano le parole, misurata fino a 4,5. B: il colore del cielo (lo zenit a metà verso
l'inchiostro), sulla stessa rampa, alla forza che le parole chiedono sul cielo disegnato,
nulla di notte. C: la fascia di oggi (70 dp, lineare, buia come il nero al 50% sullo stesso
fondo) nel colore del cielo. Il committente, guardando: A e B scuriscono di più la fascia di
giorno e accentuano lo stacco fra cielo e banda; «oggi» è meglio di loro. Il parere dato: d'accordo
su A e B (su un sereno B scurisce da metà card), e C è meglio di oggi senza cambiare altro: sul
sereno il fondo diventa un blu più profondo invece di un grigio, sul coperto è quasi uguale,
di notte il blu della notte. Scelta: C.

**Cosa è cambiato.** `WindowPainter`: il velo della didascalia è `captionVeil(gradient)` (lo
zenit del cielo a metà verso `SkyPalette.ScrimColor`), sulla stessa fascia di 70 dp, alla forza
`todaysDarknessIn(ground, veil)`: la minima, a passi di un quarantesimo, che lo fa buio quanto
il nero al 50% sul fondo che c'è davvero sotto le parole (`captionGround`, il 95° percentile
della striscia della didascalia, campionato sul cielo disegnato prima che il bitmap vada), con
un tetto a 0,85. Lo scrim in alto della riga del meteo è quello di prima. `WindowScrimTest`
(nuovo): il velo è blu sotto un sereno; di giorno è buio quanto il nero di prima, ed è il
minimo che lo fa; di notte si ferma al tetto, un poco più chiaro, con il bianco sopra 9:1.
DESIGN §3.6 (l'eccezione dichiarata con le tre misure), §3.9 (il velo), VISION §5.9, CHANGELOG;
`widgets.png` e `widget-window.png` rigenerate e guardate.

**Decisioni e deviazioni.** *Un'eccezione dichiarata al contratto di §3.6*, scelta dal
committente guardando le rese: la didascalia della finestra di giorno resta a 2–3:1, perché le
due vie che arrivano a 4,5 scuriscono metà della card e l'immagine è il soggetto. Scritta in
DESIGN con i numeri, perché chi passa dopo non la prenda per una svista. *La didascalia è un
testo di Glance*, che non può portare l'alone di §3.6; dipingerla nel bitmap per averlo
perderebbe la scala del carattere e l'orologio vivo dei luoghi in un altro fuso. *Di notte il
velo resta*, come oggi (A e B lo toglievano perché non serviva): il fondo della card di notte
non cambia aspetto.

**Come è stato verificato.** `WindowScrimTest`, `WindowPainterTest`, `SkyPicturesTest`,
`NoRawColorTest`; le rese di oggi, A, B e C guardate affiancate e misurate; suite `:app` e
lint.

## Il cielo della card Stanotte gira tutto insieme (committente, dal telefono, 25 ott 2026)

**Da dove nasce.** Trascinando il cielo della card «Stanotte» per cambiare orientamento, le
stelle, la luna e la Via Lattea giravano e le nuvole restavano ferme: scorreva solo il cielo
dietro di esse, un fondale dietro uno stencil. Il committente chiede che le nuvole si muovano
in sincrono con il cielo, perché sembri davvero un cambio di visuale.

**Perché succedeva.** La scena dello shader (DESIGN §3.9) legge il deck, il velo e quel che
cade in coordinate dello schermo, con il solo vento a spostarli; l'orientamento della finestra
(`facingDeg`) entrava nelle stelle, nella luna e nel telaio galattico, mai nelle nuvole.

**Cosa è cambiato.** `SkyScene.turn` (gradi a destra, non avvolti) e l'uniform `uTurn`: il
deck, il velo, la pioggia e la neve sono letti `turn` più in là, convertito in dp con la
focale della camera delle stelle (`starPlace`, 65°→10° sull'altezza), cioè gli stessi pixel
che una stella percorre per la stessa rotazione: una camera che gira non ha parallasse,
vicino o lontano, e la parallasse del deck resta quella del vento. `skyScene` prende
`facingDeg` e `turnDeg`; con `facingDeg` la direzione del vento sullo schermo è vista da dove
guarda la finestra (`sin(verso − orientamento)`; verso l'equatore è la formula di prima, e i
test di prima non cambiano). `WindowScene.frame(turnDeg)` li passa; e mentre è la luna a
illuminare le nuvole, la luce sta dove la camera disegna la luna (`cameraLight`, tenuta entro
±75° dall'asse quando la luna è di fianco o dietro), non dove la metteva la fascia dei corpi:
i bordi illuminati guardano la luna che il lettore vede e girano con lei. `TonightCard`: lo
stato è la rotazione accumulata (`turned`, gradi, mai avvolta: il campo da cui si leggono le
nuvole non ha periodo, e un avvolgimento a nord le farebbe saltare) e l'orientamento è
`turnedFacing(auto, turned)`; `turnDegrees(dx, skyH)` è il passo di un trascinamento. Il
widget non gira (`turn` 0): la sua resa cambia solo per la luce della luna.

**Decisioni e deviazioni.** *I pixel delle stelle, non una rotazione del piano del deck*:
ruotare davvero il piano delle nuvole le sposterebbe di 1,8 volte i pixel delle stelle (la
camera del deck è 45°→15°, quella delle stelle 65°→10°), e sotto il dito le nuvole
correrebbero più del cielo; «in sincrono» è lo stesso spostamento, che è anche quello di una
camera che gira. *Il vento visto da dove guarda la finestra* è venuto con la rotazione: nuvole
che girano con la camera e un vento nel telaio dell'equatore si contraddicono. *La luce della
luna nella camera* corregge quel che c'era (la luna disegnata dalla camera, i bordi illuminati
dalla fascia), reso visibile dalle nuvole che ora girano; nel widget sposta un poco il lato
illuminato verso la luna che si vede.

**Come è stato verificato.** `SkyShaderPixelTest` (caso nuovo): una rotazione che vale 60 px
si legge come il cielo fermo 60 px più in là, entro 0,01 di luminanza su tre righe, deck, velo
e pioggia insieme, e il quadro si è mosso davvero (di giorno, con disco e alone del sole
tolti: di notte senza luna le nuvole distano un centesimo dal cielo e non mostrano nulla).
`WindowTurnTest` (nuovo): la rotazione arriva alla scena intera oltre il giro, il vento è
visto da dove guarda la finestra, la luce sta sulla luna disegnata e gira con lei, e con la
luna fuori dalla finestra resta oltre il bordo. `SkySceneTest` e `TonightNightTest`
aggiornati; le due rese guardate affiancate. Suite `:app` e lint. Le immagini del README
rese su main e sul branch e confrontate pixel per pixel: nessuna cambia (`sky-tonight.png` al
più di 6 livelli su 255 per la luce della luna, invisibile), e nessuna è stata rigenerata. DESIGN §3.9 (deriva e
rotazione), VISION §5.3, CHANGELOG.

## Le nuvole della card Stanotte si stiravano in righe girando il cielo (committente, dal telefono, 25 ott 2026)

**Da dove nasce.** Appena mergiata la PR #104 (le nuvole che girano con il cielo, sezione
precedente), il committente trascina un po' il cielo di Edimburgo e manda lo screenshot: il
deck è diventato righe orizzontali, «si stirano sempre di più» con il trascinamento, e chiede
di verificare, anche per quel che cade.

**Perché succedeva.** Il deck è un piano sotto una camera (DESIGN §3.9): la riga dello schermo
dà la distanza `dist` (1 in alto, 3,7 in basso) e la colonna l'ascissa a terra
`(x − cx) · dist / focale`. La PR #104 sommava `uTurn` (il giro in dp) alla colonna prima della
divisione prospettica: «la stessa immagine ferma letta `uTurn` più in là». Per le stelle vale,
perché sono all'infinito; per un piano no: l'ascissa a terra del giro è `uTurn · dist / focale`,
diversa riga per riga, e con un giro grande la coordinata del rumore dipende quasi solo dalla
riga. È un piano visto a 60-80° fuori dall'asse di un pinhole, e lì un pinhole stira tutto
lungo le righe. Il velo, la pioggia e la neve non hanno prospettiva (fogli piatti in coordinate
schermo): il loro scorrimento è una traslazione e non si stira.

**Cosa è cambiato.** Lo shader gira davvero la camera del deck, di `theta = uTurn / focale`:
l'angolo che sposta il *centro* del deck degli stessi pixel di una stella (la focale del deck,
30° sull'altezza, è 1,8 volte quella delle stelle, 55°, perciò l'angolo è 1,8 volte più
piccolo: resta la decisione della sezione precedente, «in sincrono» è lo stesso spostamento al
centro, e non una rotazione dello stesso angolo che farebbe correre le nuvole 1,8 volte il
cielo). Il piede del raggio sul piano (`cam`) è ruotato di `theta` (`deckC`, `deckS`): una
rotazione è un'isometria, non stira niente e torna su se stessa ogni 1,8 giri. La direzione
della luce dei bordi (`toward`) è ruotata nello stesso telaio, altrimenti un deck girato era
illuminato di lato. La deriva del vento è sommata nel telaio in cui il campo è stato steso e
gira con il piano; per questo `WindowScene.frame` passa a `skyScene` `facing − turnDeg`,
l'orientamento di *prima* del dito: con quello di adesso il vento girava due volte (nello
scalare e nella rotazione) e la somma `uTime · uDrift` saltava sotto il dito a ogni cambio di
`across`, un difetto della PR #104 che nessuno aveva ancora visto. I fogli (velo, pioggia,
neve) restano letti `uTurn` dp più in là, come prima: non si stirano. `turned` resta non
avvolto: i fogli non hanno periodo. A giro zero la formula è identica a prima (`cos 0`,
`sin 0`) e `facing − 0` è `facing`: nessuna immagine del README cambia, nessuna rigenerata.

**Decisioni e deviazioni.** *Girare la camera del deck e non quella delle stelle*: la focale
del deck è una misura (95 dp a cella sulla riga di mezzo, la tabella di `deckThreshold` su 24
canvas) e non si tocca per un gesto. *Lo stesso pixel al centro, la tangente ai bordi*: ai
bordi dello schermo il deck si sposta di più del centro (`focale · Δtan`, 77 px per 60 al
bordo di un canvas 2:1), le stelle con la loro focale un po' meno: è il pinhole di ciascuno,
e nessuno confronta una nuvola e una stella al bordo durante un trascinamento.

**Come è stato verificato.** `SkyShaderPixelTest`: il caso di ieri è diviso in due. *I fogli*
(velo e pioggia, deck 0): girati di 60 px si leggono come il cielo fermo 60 px più in là entro
0,01 di luminanza su tre righe, su tutta la larghezza. *Il deck* (deck 0,6, niente velo): al
centro (±60 px) entro 0,03, e dopo **cinque giri** il grano del deck (passo medio di luminanza
a 4 px, in orizzontale su verticale) è quello del cielo fermo entro un terzo e la luminanza
media entro 0,03 (misurato: centro a 0,0067, grano 0,328 contro 0,354, media 0,451 contro
0,459). Prima della correzione, sulla stessa scena: grano orizzontale 0,054 contro 0,354 del
cielo fermo, 6,6 volte più piatto, la figura dello screenshot. `SkyDeckTest`
aggiornato alle righe nuove dello shader (il port legge la camera a giro zero).
`WindowTurnTest`: la deriva di una finestra girata di 90° e di 380° è quella di dove guardava
prima. Le tre rese (fermo, 60 px, cinque giri) guardate affiancate: nuvole in tutte e tre.
Suite `:app`, lint, APK di debug. DESIGN §3.9 (deriva e rotazione, con le misure).

## L'ora blu ha un disegno suo: il sole sotto l'orizzonte con la prima stella (committente, 25 ott 2026)

**Da dove nasce.** Il committente chiede se la stella per gli eventi dell'ora blu va bene o
se serve un'icona piu' adatta, e in caso di vedere una o due alternative.

**Il parere.** La stella da sola non va: non per il disegno ma per quante cose dice. Dal 14 ott
2026 e' l'ora blu, il crepuscolo nautico, Venere e Giove insieme («la stella dice cinque
cose», punto 7 della revisione del Cielo, che aveva tolto solo le congiunzioni), e per l'ora blu
non dice la cosa che la definisce: il sole appena sotto l'orizzonte, da −4° a −6°, con le
prime stelle. Meteocons non ha quel disegno ma ha i pezzi.

**Le alternative.** Composte come i marchi del 14 ott (`tools/compose_sky_marks.py`: pezzi di
disegni importati, scala e posizione, linea della famiglia), rese con il convertitore di
`tools/icon_filmstrip.py` e Edge headless a 51, 32 e 20 px, su fondo chiaro e scuro, accanto ai
vicini della scala del giorno (ora d'oro, tramonto, candidata, notte). Cinque composizioni:
la cupola di `horizon` senza i raggi con la stella sopra, centrata; la stessa con la stella in
alto a destra (sbilanciata); il sole con i raggi piu' la stella (troppo vicino al tramonto); la
cupola senza la linea (un arco sotto una stella); la sola linea con la stella (una stella
sottolineata). Mostrate al committente la prima e l'ultima; scelta la prima.

**Cosa e' cambiato.** `horizon-star`, quarto marchio del cielo: `horizon` senza `g1rays` al
120% con il centro dell'inchiostro a (64, 100), la linea a 111 su 128, e `star` al 62% a
(64, 40). `Piece.drop` toglie ora i nomi a qualunque profondita' (`_prune`): i raggi stanno
dentro il gruppo del ritaglio, e il filtro di prima guardava solo i figli diretti. Lo strumento
scrive e confronta in UTF-8 con i fine riga indifferenti: su Windows la codifica di sistema
scriveva il trattino lungo del commento come un byte che rende l'XML non valido, e il CRLF
della copia di lavoro diceva «da rigenerare» di ogni file uguale. `skyJobLineRes`:
`blue_hour.am/pm` prendono `mc3_horizon_star`; il crepuscolo nautico, Venere e Giove tengono
la stella. La scala del giorno sale a gradini leggibili: il sole, il sole sulla linea, il sole
sotto la linea con la stella, la stella sola, le stelle e la luna. `horizon` e `star` erano
gia' fra i disegni spediti: `shipped_icons.py` cambia solo nel commento. **Una seconda
tabella** e' venuta fuori rigenerando le immagini del README: `today-day.png` (la riga
dell'ora blu) non cambiava, perche' la lista di Oggi (`TodayAgenda.timelineIcon`) sceglieva le
icone per conto suo, kind per kind, la stessa deriva che il 21 set 2026 aveva fatto stampare al
widget una luna sbagliata. Ora i momenti che hanno una pagina sul Cielo (`TimelineKind.skyJob`)
prendono il disegno dalla tabella unica (`ChiaroIcons.skyJob`), e la `when` resta per i turni
del tempo, l'arcobaleno e le regole, che una pagina non hanno; per gli altri sette kind il
disegno e' lo stesso di prima.

**Come e' stato verificato.** `SkyMarksTest` (ricetta nuova, i pezzi alla lettera con
l'orizzonte senza i raggi, il caso «non porta i raggi, tiene la linea e la stella»),
`SkyMomentIconTest`, `MeteoconsSetsTest`; `compose_sky_marks.py --controlla` pulito. Le quattro
facce rese e guardate accanto ai vicini. Suite `:app`, lint, APK di debug. Immagini del README
rigenerate: cambia `today-day.png`, l'unica con una riga dell'ora blu, guardata; `sky-tonight.png`
e `today-night.png` uscivano diverse di 6 e 3 livelli su 255 al massimo (il residuo della luce
della luna della PR #104, lasciato apposta, e il rumore della GPU), non per questo cambiamento,
e sono state rimesse come erano. DESIGN §13.1 e §8 (la riga dell'ora blu), CLAUDE.md (i
marchi), CHANGELOG.

## La luce delle nuvole non salta più dietro la luna (committente, dal telefono, 25 ott 2026)

**Da dove nasce.** Provata la PR #105, il committente gira la finestra della card Stanotte: tutto
fluido, ma passando per ovest, in entrambi i sensi, «l'immagine cambia di colpo, come se si
ridisegnasse con un angolo diverso»; il cielo resta coerente, scatta il disegno.

**Perché succedeva.** La luce delle nuvole della PR #104 sta dove la camera disegna la luna,
con l'angolo fuori asse **bloccato a ±75°** (`cameraLight`, `LightEdgeDeg`). Guardando dalla
parte opposta alla luna l'angolo (`acrossOf`) passa da +180° a −180°, e la luce salta da 75° a
destra a 75° a sinistra: 3,7 focali da una parte a 3,7 dall'altra, cinque larghezze in un grado.
Lo shader usa una sola direzione per tutto il canvas (dal centro verso la luce) per i bordi
illuminati e l'autoombra del deck, quindi ogni bordo cambia lato nello stesso istante: il
disegno ridisegnato con un altro angolo. Con la luna a est il salto cade a ovest.

**Cosa è cambiato.** Fino a 75° la luce è dove era (il pinhole della camera delle stelle).
Oltre, **gira intorno al canvas**: dal punto in cui stava al bordo, alla stessa distanza, la
sua direzione dal centro ruota verso il basso man mano che l'angolo va a 180°, dove le due
vie si incontrano: una luna alle spalle del lettore illumina le nuvole dal piede del quadro,
il lato vicino, quello che la guarda. Nessun salto in nessun punto del giro. Il widget non
cambia: guarda la luna quando è su.

**Decisioni e deviazioni.** *L'angolo al bordo, a sinistra, è tenuto dal lato di π* (`atan2`
lo dà vicino a −π): il primo tentativo girava dalla parte lunga, passando per destra, e il
test lo ha preso. *Il limite del test è un quinto del quadro*, non un decimo: al bordo dei 75°
la tangente del pinhole corre 0,16 del quadro per grado, continua ma veloce, e lì la direzione
dal centro, che è quel che lo shader usa, gira appena.

**Come è stato verificato.** `WindowTurnTest` (caso nuovo): giro completo un grado alla volta,
il passo più grande fra due gradi è 0,16 del quadro (prima: cinque larghezze); dietro al
lettore la luce sta sotto il centro del piede, uguale da una parte e dall'altra. Quattro
fotogrammi della finestra (Milano, 26 ott 22:00, nuvole 55%) a 170°, 179°, 181° e 190° dalla
luna, resi con lo shader vero prima e dopo (classe di scratch, non committata): prima, fra 179°
e 181°, i bordi illuminati cambiano lato tutti insieme; dopo, i due fotogrammi sono quasi uguali
e la luce gira piano da 170° a 190°. Suite `:app`, lint, APK di debug. DESIGN §3.9.

**Dal telefono, dopo la correzione (committente, 26 ott 2026): un movimento residuo, lasciato.**
Fra est e sud-est le nuvole cambiano ancora un poco, in modo continuo. È la finestra che passa
sulla luna stessa: a Edimburgo quella sera la luna stava a 100° alle 19:00 e a 128° alle 21:00
(motore dell'app), alta 24°–40°, cioè vicino alla metà verticale del canvas. La luce sta sul
disco, e la direzione unica «dal centro del canvas verso la luce» (15 ott 2026: per pixel
disegnava anelli intorno alla luna) ruota da destra a sinistra passando dalla verticale mentre
la luna attraversa la colonna centrale: con la luna a 32° (55 px sotto il centro) la direzione
gira di 84° in 10° di rotazione, 20° per grado vicino al centro. Continuo, nessun salto, ma si
vede. Non si corregge con sicurezza: è il modello della luce di ogni cielo dell'app, non della
finestra, e prendere la direzione dal piede del quadro invece che dal centro (che renderebbe il
passaggio lento) cambierebbe l'ombreggiatura di tutti i cieli e andrebbe riguardato per intero.
Il committente chiede di intervenire solo se risolvibile con sicurezza: lasciato così.

## «Adesso» non inventa una nebbia che la sua ora non ha (committente, sul posto, 26 ott 2026)

**Da dove nasce.** Il committente è a Lissone alle 9:45: parzialmente soleggiato, visibilità
ottima, e l'app dice «Nebbia».

**La causa, misurata sulla risposta di quel momento** (Open-Meteo, 45.612 N 9.240 E). Il blocco
`current` dà codice 3 (coperto), nuvole 98% tutte basse, umidità 94%, **visibilità 380 m**. Le
ore attorno: 08:00 4 060 m, 09:00 140 m, 10:00 1 200 m, poi 25 km dalle 11. Il modello vede uno
strato basso di un'ora che sul posto non c'è. La striscia oraria non lo disegnava: la riga delle
9 è sotto il chilometro da sola, e la regola della Fase 26 inventa la nebbia solo se persiste in
un'ora vicina (STATES §3, regola 5), quindi «Coperto». Il blocco `current` invece passava
`fogPersists = true`, scritto come se la sua visibilità fosse un'osservazione («se adesso sono
300 m, adesso c'è nebbia»): è il numero del modello, interpolato come quello delle righe. Così il
titolo diceva «Nebbia» sopra una striscia che diceva «Coperto», contro il codice del provider
stesso.

**Cosa è cambiato.** La nebbia del presente è quella della sua riga: il mapper chiede a
`WeatherStateEngine.fogPersists` (ora pubblica, sulle sole visibilità) se la riga in cui cade
adesso è una nebbia che persiste, e passa quel verdetto al blocco. La visibilità del blocco deve
comunque stare sotto il chilometro, e un codice di nebbia del provider confermato dalla
visibilità resta nebbia come prima. Nessun numero cambia: la piastrella della visibilità dice
ancora 380 m, che è quel che il modello prevede. STATES §5 aggiornato.

**Quel che resta, detto chiaro.** A Lissone l'app ora dice «Coperto», non «Parzialmente
soleggiato»: le nuvole al 98% sono il numero del modello, e l'app non lo cambia (CLAUDE.md, «i
numeri sono del modello»). L'errore che si toglie è quello dell'app, la nebbia aggiunta sopra.

**Come è stato verificato.** `WeatherReportMapperTest`: il caso di Lissone (blocco a 380 m, riga
a 140 m fra 4 km e 1,2 km: «Coperto» sopra e sotto, 0,38 km sulla piastrella), una nebbia che
persiste nella riga resta nebbia anche sopra, e la visibilità del blocco ha l'ultima parola se
la riga è in nebbia e il blocco no. `WeatherStateEngineTest`: la prova di persistenza sulle
sole visibilità dà lo stesso verdetto della serie. Suite completa e lint.

## Il motore dopo la revisione: più modelli per gli stati, strati e foschia (committente, 26 ott 2026)

**Da dove nasce.** Dopo la nebbia di Lissone il committente chiede una revisione completa del
motore e un confronto con i motori più noti (Apple, Google, AccuWeather, IBM/The Weather Company,
NWS National Blend, Met Office IMPROVER, Yr, meteoblue). La revisione, data in conversazione, ha
trovato che quasi tutti fanno tre cose che Chiaro non fa: combinano più modelli, ragionano in
probabilità su ogni fenomeno, correggono il presente con osservazioni. A Lissone, alle 9, il solo
ICON-D2 (che `best_match` usa lì) vedeva la nebbia: ICON-EU 14 km, UKMO 11 km, GFS 24 km, i 31
membri dell'ensemble GFS tutti sopra il chilometro.

**Le proposte e la scelta.** Cinque proposte; il committente prende le prime quattro e lascia la
quinta:

1. la nebbia inventata solo con una seconda prova fisica (umidità o punto di rugiada);
2. il cielo dagli strati, pesando meno le nubi alte (IMPROVER guarda solo basse e medie);
3. modelli testimoni che votano sugli stati;
4. la foschia (1–5 km) come stato suo, già in STATES §10;
5. ~~osservazioni vere per «adesso» (METAR, ARPA)~~: **esclusa**, rappresentano male un punto e
   portano l'app fuori da Open-Meteo; se torna, torna come decisione di prodotto in VISION.

**Le due decisioni del committente.**

- **I numeri restano di `best_match`, i testimoni votano solo sugli stati**, e solo su nebbia,
  foschia e fase (pioggia, neve, miste). **Il cielo resta di `best_match`**, letto per strati: un
  cielo votato accanto alla piastrella delle nuvole di `best_match` direbbe «poco nuvoloso» sopra
  un 98%, la contraddizione che la regola 6 esiste per evitare. Una media di modelli è un numero
  che nessun modello ha previsto: per CLAUDE.md andrebbe dichiarata stima ovunque.
- **L'ordine**: prima la misura, poi i punti 1, 2 e 4 (nessun dato nuovo), poi i testimoni. Le
  soglie si scrivono sui numeri misurati, come vuole STATES.

**Quali testimoni.** Non ICON-EU: misurato su 15 città, `best_match` è la famiglia ICON in tutta
Italia per tutta la settimana (ICON-D2 i primi due giorni dove arriva, poi ICON-EU, poi ICON
globale; a Roma, Palermo, Madrid, Atene ICON-EU dal primo giorno), e ICON-D2 gira dentro
ICON-EU. Tre famiglie, ECMWF (`ecmwf_ifs`, 9 km, tutte le variabili del motore), UKMO globale
10 km (senza `showers`) e GFS, e a ogni risposta si scarta quella identica a `best_match`,
riconosciuta dai numeri e non da una tabella (fuori dall'Europa `best_match` è spesso ECMWF:
Sydney tutta la settimana, il nord Europa dal quarto giorno). Lo stesso principio di
`NowcastCoverage.looksInterpolated`. Senza modelli fissi al posto di `best_match`: si
perderebbero i modelli regionali (ICON-D2, AROME, HRRR, UKMO 2 km, MET Nordic) e la sezione del
quarto d'ora, che esiste grazie a ICON-D2 e HRRR.

### La misura: le previsioni passate contro il cielo osservato

**Lo strumento.** `tools/measure_against_metar.py` scarica, per 24 aeroporti scelti per fenomeno
(la pianura padana prima di tutto) e per due periodi, ottobre 2026 (26 set – 25 ott) e l'inverno
(1 dic 2025 – 31 gen 2026): le previsioni che ogni modello aveva fatto per quelle ore (l'archivio
`historical-forecast-api` di Open-Meteo, per `best_match` e i candidati testimoni), i METAR degli
aeroporti (archivio IEM, gratuito: visibilità e fenomeni) e la radiazione solare vista dal
satellite (archivio di Open-Meteo: se il sole si vedeva). `tools/analyze_against_metar.py` dà i
punteggi delle regole e delle loro varianti sulle righe dell'app. I dati grezzi stanno in
`tools/measurements/metar-*.json.gz`. Le osservazioni servono solo a misurare: nell'app non entrano
(il punto 5 resta escluso). STATES §11 ha le tabelle.

**Deviazione: il periodo.** Il piano diceva «la misura»; ne sono venute due. Ottobre ha poca nebbia
padana (Linate: 0 ore di nebbia osservate, 28 dette dal motore) e nessuna neve, e una soglia della
nebbia scelta su ottobre avrebbe preferito la regola sbagliata: su ottobre vinceva «la maggioranza
dei testimoni», sull'inverno la stessa regola perdeva più nebbie vere di quanti falsi allarmi
toglieva (sotto). Il satellite di
Open-Meteo non ha archivio prima di febbraio 2026: il cielo è misurato su ottobre soltanto, e STATES
§10 dice di rimisurarlo d'inverno.

**Deviazione: la rete.** Open-Meteo risponde 429 a richieste con molte variabili e molti modelli
(contano come molte chiamate): lo scaricamento ha richiesto ore, con una pausa di un minuto a ogni
429 e un `--resume` per riprendere gli aeroporti persi.

### Le regole scelte sui numeri

- **Punto 2, il cielo per strati: fatto.** La regola 6 legge la parte di luce che le nubi tolgono
  (`CloudLayers.lightTaken`: basse 1, medie 0,8, alte 0,3), la stessa formula con cui il disegno
  del cielo illumina le sue nuvole dal 16 ott, spostata dal disegno al dominio. Contro il satellite,
  4 380 ore di luce: giusto il 66% contro il 55% della copertura totale, «coperto» col sole visibile
  il 5,8% delle ore contro il 17,5%. `max(basse, medie)` faceva due punti meglio (68%) ed è scartato
  perché la parola e il disegno devono venire da un'aritmetica sola. La tile delle nuvole continua a
  mostrare il totale e il suo strato, che spiega la differenza.
- **Punti 1 e 3, la nebbia: fatti insieme.** Non uno o l'altro: l'umidità propria ≥ 95% **e**
  almeno un testimone che la vede (sotto 1 km, o umidità ≥ 97%); senza testimoni basta l'umidità.
  Sui due periodi, 41 873 ore asciutte e 1 410 ore di nebbia: falsi allarmi −41% (2 257 → 1 323),
  nebbie inventate su aria limpida −51%, l'85% delle nebbie vere che il motore prendeva è ancora
  preso; CSI da 0,24 a 0,28. La «maggioranza dei testimoni» (la proposta di metà giornata, buona su
  ottobre) è scartata per l'inverno: 607 nebbie prese su 1 257 contro 671.
- **Punto 4, la foschia: misurata e non fatta.** La visibilità del modello tra 1 e 5 km ha ragione
  nel 22% delle ore in cui la dice (CSI 0,15) e né l'umidità né un testimone la aggiustano: uno stato
  sbagliato il 78% delle volte è lo schermo che mente. STATES §10.
- **Il voto sulla fase: misurato e non fatto.** `best_match` indovina pioggia, neve o miste nel 94%
  di 4 995 ore, il voto nel 95%. Non vale il codice: i testimoni chiedono solo temperatura, nuvole,
  visibilità e umidità.

**I testimoni nel codice.** `OpenMeteoWitnessApi` (una richiesta a parte, perché una risposta
multi-modello rinomina ogni colonna e cambierebbe la forma del report), `WitnessResponseDto` letto
per nome di colonna, `Witnesses` nel mapper (allinea per etichetta oraria e scarta, ora per ora, il
modello identico a `best_match`: temperatura entro 0,05 °C e nuvole entro 0,5% per sei ore di qua e
di là), `WeatherStateEngine.Witness`. Best-effort come l'aria; salvati nella cache su disco accanto
alla previsione, così un report riletto dal disco vota come quello appena scaricato. «Adesso» usa i
testimoni della sua riga e la propria umidità.

**Lissone, alla fine.** Alle 9:45 del 26 ott: umidità 94% (nessuna nebbia per la regola 1) e i tre
testimoni a 24–28 km con umidità tra 58% e 77% (nessuna per la regola 3). La parola diventa
«Coperto»: il 98% di nubi basse di quell'ora è il numero del modello, e l'app non lo cambia.

**Le immagini del README** sono rigenerate: il Milano registrato a mezzogiorno del 25 set passa da
«Overcast» a «Partly cloudy» (86% di copertura, 66% bassa, 49% alta; il satellite vedeva il sole,
150–320 W/m² di luce diretta), e con lui alcune ore e due giorni della settimana. Le differenze
invisibili nella card Stanotte e nella notte di Oggi (al massimo 8 livelli di colore su 255) vengono
dal lavoro del 25 ott sulle nuvole della card, che non aveva rigenerato `sky-tonight.png` (fermo al
22 ott). **Le immagini vanno generate con il fuso di Roma** (`TZ=Europe/Rome` davanti a Gradle): in
un contenitore in UTC il fuso del telefono non è quello di Milano, e il widget finestra mette l'ora
del luogo accanto al nome, come deve fare quando i due orologi differiscono.

**Come è stato verificato.** `WeatherStateEngineTest` (la luce degli strati, la nebbia con e senza
testimoni, il caso di Lissone), `WitnessesTest` (allineamento, doppione, modello mancante),
`WeatherReportMapperTest` (la nebbia che gli altri modelli non vedono, sopra e sotto),
`ReportDiskCacheTest` (i testimoni nella cache), `WindowWeatherTest` aggiornato al nuovo cielo.
Suite completa e lint.

### Le verifiche rimaste, e la misura della pioggia (committente, 26 ott 2026)

**Da dove nasce.** Il committente chiede se il piano era finito del tutto. Non lo era nelle
verifiche, e sono state chiuse:

- **CI** verde su entrambi i push del branch (Android CI, run 429 e 430).
- **La chiamata vera**: un test di prova, non committato, con il Retrofit e il `Json` dell'app contro
  il servizio dal vivo: 168 ore, le dodici colonne, e su Lissone i testimoni cambiano lo stato di 2
  ore su 151.
- **La release minificata** (`assembleRelease -PsignReleaseWithDebugKey`) si costruisce, e R8 tiene
  `OpenMeteoWitnessApi` e `WitnessResponseDto` (con il suo `$$serializer`) come tiene la previsione.
  Non provata su un telefono: qui non c'è un emulatore.
- **Il costo**: 3,3 KB compressi accanto ai 4,5 KB della previsione; 12 variabili contro 52 nel
  conteggio di Open-Meteo, circa un quarto in più. STATES §12.

**Dopo il confronto con IMPROVER** il committente chiede una valutazione onesta dei quattro passi
proposti: soglia della pioggia, pioviggine come IMPROVER, fulmini, calibrazione. Fatti ora i primi
due, misurati insieme; i fulmini a giugno, quando c'è un'estate da misurare; la calibrazione no
(serve osservazione continua, ed è il punto 5 escluso; correggere i numeri va contro «i numeri sono
del modello»).

**Deviazione: il limite giornaliero.** Le misure del mattino hanno esaurito le chiamate gratuite di
Open-Meteo per l'indirizzo di questo ambiente («Daily API request limit exceeded»). La probabilità di
precipitazione, che mancava nei file, la scarica un workflow nuovo, `Measure against METAR`, su un
runner di GitHub, che ha un indirizzo suo, come già fa `Measure states`;
`measure_against_metar.py --add-probability` completa i file esistenti una stazione alla volta.

**I numeri della pioggia** (il workflow ha scaricato la probabilità per 47 dei 48 aeroporti-periodo,
run 1 di `Measure against METAR`): la regola del motore, ≥ 0,1 mm e ≥ 20%, ha un CSI di 0,48, lo
stesso del 30% e meglio di ogni soglia più alta; decidere con la sola probabilità, come IMPROVER, al
loro 50% fa 0,47 e al meglio (40%) 0,49. **La soglia resta 20%**, ora misurata contro la pioggia vista
e non più solo contro gli avvisi. **La pioviggine alla IMPROVER non si fa**: le 807 ore che toglierebbe
erano bagnate nel 61% dei casi, e il CSI scende a 0,45. Nessun codice cambia; cambia STATES §2 e §11.

Una cosa trovata e poi decisa (sotto): «Pioggia probabile» (niente di misurabile, ≥ 60%) è stata
bagnata nel 49% delle 1 134 ore. La regola 4 la disegna già come una possibilità e non come pioggia, e la
parola resta del committente: se «probabile» debba diventare «possibile» è una scelta di prodotto, da
proporre a parte.

### «Pioggia possibile» al posto di «Pioggia probabile» (committente, 26 ott 2026)

Il committente lascia la scelta: farlo se è meglio. **Fatto**, sui numeri divisi per fascia: niente
di misurabile e probabilità 60–69%, bagnate il 37% delle ore; 70–79%, il 42%; dall'80%, il 61%.
«Probabile» prometteva troppo in due fasce su tre; «possibile» è vero in tutte, e dall'80% sottostima,
che per lo schermo è l'errore minore. Una terza parola per la fascia alta è stata valutata e non fatta:
un'ora al 61% resta un'ora in cui piove poco più di una volta su due, e la differenza non vale uno
stato in più. Cambiano solo `cond_rain_likely` e `cond_snow_likely` (IT e EN); gli identificatori
(`RAIN_LIKELY`, `rain_likely` nelle regole del lettore) restano, come vuole CLAUDE.md. La notifica
«Pioggia probabile nelle prossime ore» resta: è un'altra cosa, scatta dal 70% entro sei ore.

## Fase 14 — Il motore contro i motori premium: la revisione e i passi (committente, 27 ott 2026)

**Da dove nasce.** Il committente chiede una revisione completa del motore di interpretazione e
della sua resa a schermo, confrontata con come i motori premium leggono gli stessi campi (verifica
in rete, dati aggiornati), per un'app di livello premium, mondiale, con riguardo all'Europa e
all'Italia. Letti per intero `WeatherStateEngine`, `WmoCode`, `WeatherReportMapper`, `AlertEngine`,
`HeadlineEngine`, `WeatherText`, i tile dei dettagli, `NowcastCoverage`, `Witnesses` e
`docs/STATES.md`; in rete, i sorgenti e le pagine di Open-Meteo (`ForecastapiController.swift` per
l'ordine di `best_match`, la pagina DWD, le issue #2220 e #2219), le regole dei simboli di MET
Norway (`weather_symbol/Factory.cpp`), l'albero IMPROVER del Met Office, WeatherKit, MeteoSwiss,
meteoblue, Yr, DWD (criteri di allerta), l'EEA 2024, 3BMeteo. Tre fatti verificati dal vivo il 27 ott:

| Verifica | Esito |
|---|---|
| Parigi e Lione, `minutely_15` di `best_match` contro ICON-D2 e AROME HD | identica a ICON-D2 in ogni quarto; AROME diversa |
| Roma, `best_match` contro ICON-EU e ICON-2I | ICON-EU nelle prime ore; ICON-2I non entra, e non porta né visibilità né probabilità |
| Roma, `snowfall_height` e `freezing_level_height` in `best_match`; KNMI/DMI HARMONIE Europe a Milano e Roma | presenti (1 720 m, 3 300 m); KNMI Europe risponde con la visibilità a 5,5 km su entrambe |

**Il verdetto, in breve.** Il motore è coerente con i motori premium che derivano l'icona dai campi
fisici (MET Norway: nubi 13/38/86, pioggia da 0,1 mm, fase dal bulbo umido; IMPROVER: nubi 19/81,
pioggia da 0,1, nebbia 1 km, foschia 5 km) e in più è misurato contro osservazioni, cosa che nessuno
di loro pubblica. Le soglie di pioggia 2,5/7,6 mm/h sono la scala italiana (debole < 2, moderata
2–6, forte > 6), non quella norvegese (forte da 1 mm/h): giuste per il pubblico. Percepito da 1°
di scarto come iOS 18, UV sulle bande WHO, aria sull'EEA 2024, polline MeteoSwiss. Il quarto d'ora
solo dove calcolato è un vantaggio in Italia: Apple e Google non hanno il nowcast qui. Scelte
diverse dagli altri ma difendibili e già motivate: la probabilità nascosta sotto il 20% (il Met
Office stampa «<5%»); la percentuale del giorno come massimo orario (sottostima «piove oggi»,
MeteoSwiss lo scrive; meteoblue usa la media).

**I problemi trovati, in ordine di urgenza.**

1. **Il 30 nov 2026 la visibilità ICON sparisce.** DWD migra i feed alla griglia nativa e toglie
   `VIS` da ICON, ICON-EU e ICON-D2 (annuncio DWD di settembre 2026; la pagina DWD di Open-Meteo:
   «Visibility is only available until November 2026»; PR #2160 già unita). In Italia `best_match`
   è la famiglia ICON tutta la settimana, quindi da dicembre `visibility` arriva nulla, o riempita
   da IFS HRES a 9 km (Open-Meteo riempie per variabile col modello più grosso che ce l'ha). Nel
   codice di oggi: con visibilità nulla la regola 4 di STATES fa restare «il verdetto di nebbia del
   provider», cioè il `ww` 45/48 di DWD nudo, misurato in contraddizione con la sua stessa
   visibilità nel 68% dei casi; e se Open-Meteo riempie con IFS, il testimone `ecmwf_ifs` ha la
   stessa visibilità dell'ora propria, `Witnesses` non lo riconosce (confronta temperatura e nubi,
   che restano ICON) e il voto è un modello che conferma sé stesso.
2. **Il tile «Visibilità» contraddice il motore.** Stampa «Foschia fitta» a 1–4 km e «Nebbia:
   prudenza in strada» sotto 1 km dalla sola visibilità del modello, e `notableDetail` lo evidenzia
   sotto 1 km senza umidità né testimoni: STATES §10 ha escluso la foschia perché quel numero è
   giusto nel 22% delle ore, e l'eroe non dice «Nebbia» senza doppia conferma. Il caso Lissone del
   26 ott vive ancora in quel tile.
3. **Il quarto d'ora è nascosto dove `best_match` è ICON-D2.** `NowcastCoverage` esclude la Francia
   «perché va ad AROME»; nel sorgente di Open-Meteo la regola «ovunque ICON-D2 abbia dati» precede
   quella AROME (Francia nord), e lo stesso ordine vale per Danimarca sotto 54,9° N, Lussemburgo,
   Polonia e Ungheria occidentali, Croazia del nord, Slovacchia. La Francia a sud del box ha i 15
   minuti nativi di AROME (orizzonte 6 h, aggiornamento orario), mai misurati.
4. **A sud di Firenze l'Italia corre su ICON-EU a 7 km.** ICON-2I di ARPAE (2,2 km, 72 h,
   aggiornato ogni 12 h, in archivio storico dal 13 apr 2025) non è in `best_match` da nessuna
   parte. Non è un difetto: non ha visibilità né probabilità, e nessuna misura dice che batta ICON-EU
   a Roma, Napoli, Palermo, Cagliari. Si misura prima di decidere.
5. **Il caldo manca dove c'è il gelo.** La scala della frase ha «Gelo entro mattina» e nessun
   gradino per il caldo; le notifiche non hanno un pericolo «caldo»; c'è solo il modello di regola a
   33°. DWD avverte a percepiti 32 (livello 1) e 38 (livello 2).
6. **Il vento non è mai «Maltempo».** `SevereBucket` ha temporale, ghiaccio, pioggia, neve; una
   raffica a 90 km/h non notifica nulla, e il bollettino della Protezione Civile non porta il vento.
   DWD arancione (Sturmböen) da 65 km/h, MeteoSwiss livello 2 da 70 in pianura.

**I suggerimenti con valore reale** (gli altri, scartati, restano nella conversazione): quota
neve (`snowfall_height`, o zero termico meno 200–400 m come MeteoSwiss), concordanza dei modelli sui
giorni 3–7 (3BMeteo stampa l'attendibilità, meteoblue la predictability, Yr gli intervalli) letta
dai testimoni già scaricati, unità del vento e della pressione per chi non è in Italia (m/s, nodi,
Beaufort, inHg), e il divario noto fuori dall'Italia: nessun avviso ufficiale (MeteoAlarm, Fase 12,
sospesa perché non chiudeva al sicuro).

**Il vincolo.** Chiaro resta gratuita e senza chiave: Open-Meteo per uso non commerciale conta le
chiamate per indirizzo (600/min, 5 000/h, 10 000/giorno; una chiamata vale `variabili / 10` oltre
le dieci variabili, e per settimane oltre le due), senza SLA; l'autenticazione a chiave è stata
rifiutata dal manutentore il 18 ott 2026 (#2219) e la risposta al sovraccarico di ottobre (#2220)
è stata «server nuovi» più il consiglio di non chiedere allo scoccare dell'ora. Ogni richiesta
nuova di questa fase scrive il suo peso in chiamate accanto a sé, e nulla nell'app chiama le API a
pagamento (Ensemble, Historical, Satellite): quelle sono degli strumenti di misura, che girano su un
runner con il suo indirizzo.

### Le decisioni prese oggi

- **La nebbia dopo DWD.** La regola 5 cambia struttura, non principio: la visibilità che il
  motore legge è *quella che la risposta ha*, la propria quando c'è, altrimenti quella dei testimoni
  (IFS HRES 9 km, UKMO 10 km, e da misurare KNMI HARMONIE Europe 5,5 km, che risponde sull'Italia
  con la visibilità); la persistenza si giudica sulla serie che si sta leggendo; l'umidità propria
  resta obbligatoria dove c'è; **il codice del provider da solo non fa mai nebbia**, né oggi né
  dopo: la regola 4 «senza visibilità resta il verdetto del provider» è abrogata. Le soglie le
  fissa la misura (14.2) sull'inverno già scaricato, togliendo la visibilità di `best_match`
  dall'analisi come succederà da dicembre. Se la regola nuova regge il CSI di quella di oggi
  (0,28), entra subito e non il 30 novembre: meglio un passaggio misurato che un gradino.
- **Il tile «Visibilità» segue lo stato.** Si disegna solo quando lo stato del motore è nebbia o
  nebbia gelata, con il numero del modello quando c'è e la riga «prudenza in strada»; sparisce
  altrimenti, come ogni tile che non ha una seconda riga onesta (CLAUDE.md, «Every number says what
  to do with it»). `notableDetail` legge lo stato, non il chilometraggio. Le righe «foschia» vanno
  via con la misura che le ha smentite. La variabile delle regole `current.visibility_km` resta.
- **ICON-2I si misura prima di usarsi**, con lo stesso strumento e gli stessi aeroporti della nebbia,
  per giorno di anticipo e contro ICON-EU al sud e ICON-D2 al nord. Entra solo se vince.
- **Una sola regola nuova sui numeri**: «i numeri sono di un modello, uno per ora» — l'app può
  sovrapporre per ora un modello a un altro (è quel che `best_match` fa già), mai farne una media.
  La formula di CLAUDE.md «Numbers are the model's» si riscrive così (14.1).
- **I testimoni restano non stampati come previsione**; quel che dicono insieme (il voto sulla
  nebbia, la concordanza) può diventare una parola a schermo. STATES §12 si riscrive in 14.3 e in
  14.11.

### Le regole del repo che cambiano

Scritte in 14.1 salvo dove indicato, perché le decisioni sono prese oggi e il codice le raggiunge
step per step:

- **CLAUDE.md, «Domain notes»**: il paragrafo sulla nebbia (visibilità della stessa ora, 68%) si
  riscrive secondo la decisione sopra, con la data DWD; si aggiunge **«The provider is a moving
  target»**: Open-Meteo cambia feed e composizione di `best_match` senza preavviso, ogni variabile
  che il motore legge ha un modello d'origine e una strada quando manca, e la sonda mensile (14.1)
  è quel che se ne accorge prima dei lettori. Si aggiunge **«Free and without a key is a
  constraint»**: il peso in chiamate di ogni richiesta sta scritto accanto a essa e la somma per
  fetch nel registro; nessuna richiesta pianificata cade allo scoccare dell'ora; le API a pagamento
  sono degli strumenti. «Numbers are the model's» diventa «one model's per hour, never an average».
- **CLAUDE.md, «Design constraints»**: alla riga «Every number says what to do with it» si aggiunge
  l'esempio del tile della visibilità (14.3).
- **STATES**: §2 (soglie nuove dalla misura), §3 regola 5, §4 (la voce «no visibility»), §12 (i
  testimoni: quali, e che cosa possono dire a schermo). In 14.3 e 14.11.
- **VISION §12**: tre decisioni nuove, la 10 (la nebbia dopo DWD), la 11 (ICON-2I misurato prima di
  usato), la 12 (il caldo e il vento come pericoli dell'app). In 14.1.
- **`NowcastCoverage`** e la frase del CHANGELOG sulla copertura: in 14.1.

### Regole comuni a ogni step

Ogni step è una chat: apre con la sezione del registro che lo descrive, chiude con i test verdi
(`./gradlew test :app:testDebugUnitTest`), la sua sezione `##` in coda a questo registro con le
misure e le deviazioni, la casella in «Aperto» spuntata, STATES/DESIGN/CLAUDE aggiornati dove la
regola o la superficie cambia, le stringhe IT ed EN, la riga nel CHANGELOG `[Unreleased]`, e gli
screenshot del README rigenerati e guardati se una vista cambia. Niente nell'app chiama un'API a
pagamento e niente richiede una chiave. Una misura è un numero nel registro, non un'impressione.

### Gli step

**14.1 — La sonda del provider, il quarto d'ora dove è calcolato, le regole.** *Scopo*: accorgersi
dei cambi di Open-Meteo prima dei lettori, e togliere il quarto d'ora da sotto il tappeto dove
`best_match` lo calcola. *Lavoro*: (a) `tools/probe_provider.py` + workflow `Probe provider`
(mensile e a mano): per 20 città di riferimento (le 15 della misura dei testimoni più Parigi,
Marsiglia, Copenaghen, Varsavia, Budapest) chiede `best_match` con le variabili dell'app e ogni
modello regionale candidato, scrive in `tools/measurements/probe-<data>.json` quale modello è
`best_match` per variabile e orizzonte (serie identiche, il criterio di `Witnesses`), quali
variabili arrivano nulle, e il peso in chiamate di ogni richiesta dell'app; fallisce se una
variabile che il motore legge diventa nulla dove prima non lo era. (b) `NowcastCoverage` riscritto
come box ICON-D2 meno le eccezioni misurate (NL, BE → KNMI; GB, IE → UKMO; ≥ 54,9° N → MET Nordic
dove ha dati) più il box AROME per la Francia a sud, se la sonda mostra i quarti di AROME in
`best_match` a Marsiglia, Tolosa, Bordeaux, Nizza; `looksInterpolated` resta la chiusura di
sicurezza; test con le serie misurate. (c) le modifiche a CLAUDE.md e VISION §12 elencate sopra.
*Chiusura*: la tabella «dove è calcolato il quarto d'ora» nel registro, città per città, con la
prova; il CHANGELOG corretto. *Dimensione*: una chat. *Prima di tutti*: è la rete che prende il
30 novembre.

**14.2 — La nebbia senza la visibilità di ICON: la misura.** *Scopo*: scegliere con i numeri la
regola 5 di dicembre. *Lavoro*: `measure_against_metar.py` aggiunge ai candidati
`knmi_harmonie_arome_europe` (e `dmi_harmonie_arome_europe` se la sonda li distingue) e, per i
24 aeroporti, riscarica ottobre e l'inverno 2025-26 con la nuova lista; `analyze_against_metar.py`
aggiunge le varianti **senza la visibilità di `best_match`**: (i) il codice da solo (quel che la
regola 4 farebbe oggi), (ii) umidità propria ≥ 95 più un testimone (visibilità ≤ 1 km o RH ≥ 97),
(iii) persistenza sulla visibilità del testimone migliore più umidità propria a 95 e a 97,
(iv) la visibilità di IFS al posto della propria con la regola di oggi, (v) le stesse con KNMI
Europe come testimone in più o al posto di GFS, che visibilità non ne ha. Per ciascuna: dette,
giuste, mancate, false (e false su aria limpida a 5 km), CSI, per periodo e per aeroporto; la
pianura padana a parte. *Chiusura*: la tabella in STATES §11 e la regola scelta scritta nel
registro con la soglia; il doppione IFS/`best_match` studiato sui dati (quante ore la visibilità
propria coincide con quella di un testimone) per decidere il riconoscimento per variabile.
*Dimensione*: una chat (il workflow gira sul runner; il resto è analisi). *Dipende da*: niente;
14.1 aiuta. *Entro*: metà novembre.

**14.3 — La nebbia senza la visibilità di ICON: il codice, e il tile.** *Lavoro*:
`WeatherStateEngine.Hour` legge la visibilità come «la propria, altrimenti quella del voto»;
`persists` e `fogPersists` sulla serie letta; `fogConfirmed` con le soglie di 14.2; il codice non
fa più nebbia da solo (rule 4 abrogata, `fogOrSky` riscritta); `Witnesses` riconosce il doppione
per variabile (una visibilità uguale a quella propria per ±6 ore non vota, la sua umidità sì se la
temperatura è un'altra); `OpenMeteoWitnessApi.MODELS` secondo 14.2 (GFS esce se non serve a
nulla; `REQUEST` cambia e la cache lo sa); il mapper passa al blocco `current` la stessa serie.
Il tile «Visibilità» come deciso: solo in stato di nebbia, `notableDetail` sullo stato,
`visibilityMeaning` ridotta a due righe (nebbia, nebbia gelata) e i test di `TodayDetails`.
*Chiusura*: `WeatherStateEngineTest` e `WitnessesTest` con i casi «visibilità nulla» e «visibilità
uguale a un testimone»; il motore Kotlin rieseguito sui dati di 14.2 coincide con il prototipo
Python (come in §9); STATES §2–§4, §12; CLAUDE.md «Domain notes» e la riga sul tile; DESIGN §8.6;
screenshot dei dettagli se la Milano registrata li cambia. *Dimensione*: una chat, piena. *Dipende
da*: 14.2. *Entro*: 25 novembre, con cinque giorni di margine su DWD.

**14.4 — ICON-2I contro ICON-EU e ICON-D2: la misura.** *Lavoro*: `measure_against_metar.py`
aggiunge `italia_meteo_arpae_icon_2i` ai modelli e Bari (LIBD) e Catania (LICC) agli aeroporti;
`analyze_against_metar.py` dà, per aeroporto italiano e per giorno di anticipo (1, 2, 3), contro il
METAR: errore medio assoluto della temperatura, CSI dell'ora bagnata (≥ 0,1 mm, senza la
probabilità, che ICON-2I non ha: la stessa colonna per tutti), fase giusta, cielo giusto contro il
satellite con la regola 6 (ICON-2I ha le nubi per strato: da verificare nel download), per
`best_match`, `icon_eu`, `icon_d2` dove c'è, `icon_2i`. Due periodi: ottobre e l'inverno.
*Chiusura*: la tabella nel registro e la decisione scritta con il criterio dichiarato prima di
guardare i numeri: ICON-2I entra solo se vince sull'ora bagnata **e** sulla temperatura negli
aeroporti del sud per i giorni 1–2, di un margine che non sia rumore (almeno due punti di CSI e
0,2 °C su entrambi i periodi); al nord entra per il giorno 3 alle stesse condizioni contro ICON-EU.
*Dimensione*: una chat. *Dipende da*: 14.1 (il peso in chiamate del download).

**14.5 — ICON-2I nel report, dove ha vinto (condizionale a 14.4).** *Lavoro*: una terza richiesta
per i luoghi con `countryCode` IT, SM, VA, `models=italia_meteo_arpae_icon_2i`, `forecast_days=3`,
le variabili orarie dell'app meno quelle che non ha (peso stimato: 48 variabili su 3 giorni ≈ 2
chiamate, scritto accanto); il mapper sovrappone **per ora e per variabile** i valori di ICON-2I
alle ore e ai luoghi che 14.4 ha detto, lasciando a `best_match` probabilità, visibilità, UV e
tutto il resto; il blocco `current` segue le stesse ore; la cache disco porta la risposta accanto
(`REQUEST`); `Witnesses` non vede ICON-2I come testimone (è la previsione, non un voto); l'about e
la guida dicono da dove vengono i numeri in Italia senza nominare modelli a schermo (CLAUDE.md,
niente gergo: «il modello italiano a 2 km» è una frase, `icon_2i` no). *Chiusura*: test del
mapper con una risposta registrata su Roma; la misura del costo (byte, chiamate) nel registro;
CLAUDE.md «Domain notes» e VISION §9 aggiornati. *Dimensione*: una chat, piena. *Dipende da*:
14.4 con esito positivo; altrimenti lo step non esiste e la casella si chiude con il perché.

**14.6 — Il caldo.** *Lavoro*: `HeadlineEngine` guadagna il gradino **Caldo** dopo l'ombrello e
prima del gelo: percepita (`feelsLikeC`) ≥ 32 °C in un'ora di oggi ancora davanti («Caldo: percepiti
36° verso le 15»), o già in corso con l'ora in cui scende sotto («caldo fino alle 19»); il riassunto
della sera lo dice per domani. `AlertEngine` guadagna il pericolo **HEAT** (percepita ≥ 35 per
almeno due ore nelle prossime 12, una notifica al giorno per città, titolo suo «Caldo intenso»,
non «Maltempo»; `nearSevere` e le impronte come gli altri); le regole del lettore ottengono
`next_Nh.feels_like_max_c`. Le soglie e le fonti (DWD 32/38 percepiti; i bollettini del Ministero
della Salute a tre livelli, non integrabili senza una fonte aperta per 27 città, restano fuori) si
scrivono nel registro; le stringhe IT/EN; il widget e la notifica ereditano. *Chiusura*: test di
`HeadlineEngineTest` e `AlertEngineTest` sulle fixture d'estate (Roma luglio 2026 dall'archivio);
resa della notifica controllata come nel registro delle notifiche. *Dimensione*: una chat. *Dipende
da*: niente.

**14.7 — Il vento nel «Maltempo».** *Lavoro*: `SevereBucket.WIND` da `gustKph` dell'ora ≥ 70 km/h
sotto i 1 000 m, 90 fra 1 000 e 1 600, 110 oltre (la tabella delle soglie MeteoSwiss per
altitudine, scritta nel registro); l'`elevation` della risposta entra nel DTO e in `Location`
(nullabile, mai indovinata: senza quota vale la soglia di pianura); `severeBucket(hour)` lo legge
accanto al codice, così banner, frase, `wmo_severe` e notifica lo vedono insieme; parole «Raffiche
fino a 85 km/h verso le 14»; il giorno non cambia etichetta per il vento (non è uno stato, è un
fatto del giorno, e la riga «raffiche» c'è già). *Chiusura*: test su una bora a Trieste e un
libeccio a Genova dall'archivio; la frequenza mensile del pericolo ai 24 aeroporti dell'inverno
misurato nel registro, per sapere quante notifiche porta. *Dimensione*: una chat. *Dipende da*:
niente; dopo 14.6 per le stringhe condivise.

**14.8 — La quota neve.** *Lavoro*: `freezing_level_height` e `snowfall_height` entrano nelle
variabili orarie (peso: +2 variabili), nel DTO, in `HourlyForecast` (nullabili); la quota neve
dell'ora è `snowfall_height` quando c'è, altrimenti lo zero termico meno 300 m, detta «circa» (la
fascia 200–400 m di MeteoSwiss, scritta nel registro); un fatto del giorno aperto e di oggi, «Neve
oltre i 1 200 m circa», quando il giorno ha precipitazione materiale e la quota minima nelle ore
bagnate sta entro 1 500 m sopra il luogo o sotto di esso (allora la neve è la sua, e lo stato lo
dice già); il riassunto della sera lo porta per domani. *Chiusura*: test del mapper e di
`DayFacts`; DESIGN §8.x con la riga nuova; screenshot del giorno aperto se la Milano registrata ha
un giorno che lo mostra (probabilmente no: si rigenera solo se cambia). *Dimensione*: una chat.

**14.9 — Le unità.** *Lavoro*: `WindSpeedUnit` guadagna M_S, KNOTS, BEAUFORT (numero più parola della
scala, le parole del Met Office in EN e della scala italiana in IT); `PressureUnit` nuovo con HPA,
INHG, MMHG; `Formats`, le impostazioni, i widget, le notifiche, il tile del vento e della
pressione, le regole del lettore (gli id mostrati `_kph`/`_mph` si generalizzano, i canonici
restano km/h e hPa); il default resta km/h e hPa. *Chiusura*: test di `Formats` per ogni unità e
locale; screenshot delle impostazioni rigenerato. *Dimensione*: una chat.

**14.10 — La concordanza dei modelli: la misura.** *Lavoro*: con l'archivio di 14.2 (temperatura e
nubi di `best_match` e dei testimoni per i giorni 1–7, cucito per giorno di anticipo dal
`historical-forecast-api` con `forecast_days` pieno: lo strumento deve chiedere le corse intere, non
le prime ore), per ogni aeroporto e giorno: lo scarto fra i modelli sulla massima e sul cielo (la
luce tolta, regola 6) contro l'errore poi osservato di `best_match` (METAR per la temperatura,
satellite per il cielo). La domanda: uno scarto grande predice un errore grande? Con quale soglia
la parola «i modelli non concordano» è vera più di due volte su tre? *Chiusura*: la tabella nel
registro; se la risposta è no, 14.11 non esiste e si scrive il perché. *Dimensione*: una chat.

**14.11 — La concordanza a schermo (condizionale a 14.10).** *Lavoro*: una riga nel giorno aperto
dal terzo giorno in poi, «I modelli concordano» / «Previsione incerta: i modelli divergono di 4°
sulla massima», letta dai testimoni già scaricati (nessuna richiesta in più); la frase in cima non
la dice (è un giorno lontano); la settimana chiusa no (§1.3, la profondità è opzionale). STATES §12
si riscrive: i numeri dei testimoni non sono la previsione, la loro distanza dalla previsione è una
parola. *Chiusura*: test, DESIGN, screenshot del giorno aperto. *Dimensione*: una chat.

**14.12 — Gli avvisi ufficiali fuori dall'Italia, a chiusura garantita (su via libera del
committente).** La Fase 12 ritorna con la regola che le mancava: un feed con voci e nessuna
corrispondenza è «luogo non coperto» a schermo, mai «nessuna allerta»; la tabella dei paesi si
verifica contro i feed vivi nella sonda di 14.1 (una colonna in più: quante voci, quante
localizzate) e non a mano. Il resto del lavoro è nel ramo del 11 set 2026 e nel registro della
Fase 12. *Dimensione*: due chat (la sonda e la chiusura; poi le superfici). *Dipende da*: 14.1 e
dal committente, perché fu lui a fermarla.

**14.13 — Le icone che mancano (committente, 27 ott 2026).** *Da dove nasce*: ogni stato del motore
ha un disegno, ma tre coppie di stati ne condividono uno in modo che l'occhio non distingue, e
DESIGN vuole che un verdetto sia un glifo e una parola prima che un colore. **Pioggia gelata e
pioviggine gelata** (56/57, 66/67) disegnano la pioggia e la pioviggine normali: l'unico pericolo
della tabella senza un glifo suo, e Meteocons non ce l'ha (STATES §8 lo lascia «possibile»).
**Pioggia possibile e neve possibile** (1061, 1071) disegnano la pioviggine e la neve reali: una
possibilità al 60% e una pioviggine che cade sono lo stesso disegno nella striscia. *Lavoro*:
`tools/compose_sky_marks.py` (o un composer nuovo accanto, con lo stesso contratto) compone quattro
disegni dalle parti della famiglia — `freezing-rain` e `freezing-drizzle`: le gocce di `rain` e
`drizzle` con il cristallo di `snowflake` in basso, dove il ghiaccio si forma; `rain-possible` e
`snow-possible`: la nuvola di `drizzle` e di `snow` con una goccia e un fiocco in meno e più
chiari, la possibilità disegnata come tale — facce giorno e notte dove la fonte le ha, con le
animazioni della fonte dove la parte le porta; `ComposedIconsTest` li confronta con le parti
percorso per percorso; `shipped_icons.py` li spedisce; `ChiaroIcons.conditionLineRes` li
assegna; le notifiche e i widget ereditano. Niente XML a mano (CLAUDE.md). *Chiusura*: il
foglio delle icone (`tools/icon_filmstrip.py`) con i quattro nuovi accanto ai vecchi, guardato
alle tre taglie della striscia, della settimana e dell'eroe, e allegato al registro; gli
screenshot del README rigenerati se la Milano registrata ha uno di questi stati (probabilmente
«pioggia possibile»); DESIGN §13.1 con la regola delle quattro composizioni nuove. *Dimensione*:
una chat. *Dipende da*: niente; le sue icone non toccano il motore. *Quando*: dopo 14.3 e prima
delle misure, perché la pioggia gelata è d'inverno e dicembre è vicino; non alla fine.

**L'ordine.** 14.1, 14.2, 14.3 subito, in quest'ordine: hanno la scadenza del 30 novembre. Poi
14.13 (una chat, prima dell'inverno), poi 14.4 → 14.5, 14.6, 14.7, 14.8, 14.9, 14.10 → 14.11 in
qualsiasi ordine, un passo per chat. 14.12 quando il committente dice sì.

## 14.1 — La sonda del provider, il quarto d'ora dove è calcolato, le regole (committente, 27 ott 2026)

**Fatto.** Tre cose, come lo step le descrive.

**(a) La sonda.** `tools/probe_provider.py` e il workflow `Probe provider` (mensile, il 2 del mese alle
05:23 UTC, fuori dallo scoccare dell'ora come Open-Meteo chiede; e a mano). Per 45 città (dodici
italiane, da Milano a Cagliari, perché il bordo di ICON-D2 taglia l'Italia in due; poi ogni paese
che Open-Meteo serve con un modello suo; tre fuori dall'Europa) chiede due cose: **la richiesta
dell'app** tale e quale, con le liste di variabili lette da `OpenMeteoApis.kt` (la sonda non può
restare indietro rispetto al codice), e conta i nulli per blocco e variabile; e **i venti modelli
candidati in una richiesta sola** (`models=`: quelli che non coprono il punto restano fuori dalla
risposta senza errore), per riconoscere chi è `best_match` per variabile e giorno di anticipo con
il criterio di `Witnesses` (serie uguali entro la tolleranza, 90% delle ore), e lo stesso sui
quarti d'ora. Scrive `tools/measurements/probe-<data>.json` e stampa la tabella. **La sentinella**:
esce con 1 quando una variabile che era piena nella sonda precedente arriva vuota, per la richiesta
dell'app e per i testimoni; il workflow fallisce e commette lo stesso la sonda che ha visto il
cambio. Una corsa vale ~700 chiamate, distanziate; a Milano e Roma la prova è costata 4 minuti.

Due cose imparate scrivendola, che cambiano come si legge il provider:

- **La temperatura di `best_match` non coincide con nessun modello** a sud del bordo di ICON-D2
  (Roma, Perugia, Napoli, Bari, Palermo, Cagliari: 4–25% delle ore uguali a ICON-EU), mentre la
  **visibilità** di quelle stesse ore è ICON-EU al 100% nei giorni 1–5 e IFS HRES nei giorni 6–7, e
  la pioggia, quando c'è, è ICON-EU. Open-Meteo corregge la temperatura per la quota del punto, e
  il riconoscimento «dai numeri» di `Witnesses` (temperatura entro 0,05 °C) **non può riconoscere
  un doppione per variabile**: va rifatto per variabile in 14.3, come la fase già prevede. La
  sonda riconosce con la pioggia quando la serie non è piatta, con la visibilità e le nubi
  altrimenti, e con la temperatura dove un modello intero coincide.
- **La visibilità riempita da IFS esiste già**, non è un'ipotesi per dicembre: a Marsiglia, dove
  `best_match` è AROME (che visibilità non ne ha), la visibilità dei giorni 1–7 è `ecmwf_ifs` al
  100%, con la temperatura di AROME HD accanto. È la forma esatta che l'Italia prenderà dal
  30 novembre, e il caso di prova per 14.2 e 14.3.

E una correzione al registro del 26 ott («Quali testimoni»): in Italia `best_match` non è «la
famiglia ICON per tutta la settimana»: lo è nei giorni 1–5 (ICON-D2 dove arriva, poi ICON-EU), e nei
giorni 6–7 è ICON globale dove la temperatura coincide (Trieste, Bologna, Napoli, Bari, Palermo,
Cagliari, Zurigo, Vienna, Monaco, Lione), IFS HRES ad Ancona e Marsiglia, nessuno dei due a Milano
e Roma. Il voto sulla nebbia non ne soffre (un doppione si riconosce dai numeri, e lo si farà per
variabile), ma la frase era un ricordo della documentazione, e CLAUDE.md da oggi lo vieta.

**La tabella** (27 ott 2026; «G» è il giorno di anticipo, il modello è quello della pioggia, o
della temperatura dove la pioggia è piatta; `—` è una miscela che non coincide con nessuno al 90%;
«Quarti» è chi calcola i quarti d'ora e se sono calcolati):

| Città | G1 | G2 | G3 | G5 | G7 | Quarti | Visibilità (nulli) |
|---|---|---|---|---|---|---|---|
| Milano (IT) | icon_d2 | — | — | — | — | icon_d2, calcolati | 0% |
| Torino (IT) | icon_d2 | — | — | — | — | icon_d2, calcolati | 0% |
| Trieste (IT) | icon_d2 | icon_d2 | — | — | icon_global | icon_d2, calcolati | 0% |
| Bologna (IT) | icon_d2 | icon_d2 | — | — | icon_global | icon_d2, calcolati | 0% |
| Firenze (IT) | icon_d2 | — | — | — | — | icon_d2, calcolati | 0% |
| Ancona (IT) | icon_d2 | icon_d2 | — | — | ecmwf_ifs | icon_d2, calcolati | 0% |
| Perugia (IT) | — | — | — | — | icon_global | —, interpolati | 0% |
| Roma (IT) | — | — | — | — | — | —, interpolati | 0% |
| Napoli (IT) | — | — | — | — | icon_global | —, interpolati | 0% |
| Bari (IT) | — | — | — | — | icon_global | —, interpolati | 0% |
| Palermo (IT) | — | — | — | — | icon_global | —, interpolati | 0% |
| Cagliari (IT) | — | — | — | — | icon_global | —, interpolati | 0% |
| Zurigo (CH) | icon_d2 | — | — | — | icon_global | icon_d2, calcolati | 0% |
| Vienna (AT) | icon_d2 | icon_d2 | icon_eu | — | icon_global | icon_d2, calcolati | 0% |
| Monaco (DE) | icon_d2 | icon_d2 | — | — | icon_global | icon_d2, calcolati | 0% |
| Berlino (DE) | icon_d2 | icon_d2 | — | — | — | icon_d2, calcolati | 0% |
| Praga (CZ) | icon_d2 | icon_d2 | — | — | — | icon_d2, calcolati | 0% |
| Lubiana (SI) | icon_d2 | — | — | — | — | icon_d2, calcolati | 0% |
| Lussemburgo (LU) | knmi_netherlands | knmi_netherlands | — | ecmwf_ifs | ecmwf_ifs | knmi_netherlands, interpolati | 0% |
| Parigi (FR) | icon_d2 | icon_d2 | — | — | — | icon_d2, calcolati | 0% |
| Lione (FR) | icon_d2 | icon_d2 | — | — | icon_global | icon_d2, calcolati | 0% |
| Marsiglia (FR) | — | mf_arome_france_hd | — | — | ecmwf_ifs | —, interpolati | 0% |
| Tolosa (FR) | icon_d2 | — | — | — | — | icon_d2, calcolati | 0% |
| Bordeaux (FR) | icon_d2 | icon_d2 | — | — | icon_global | icon_d2, calcolati | 0% |
| Nizza (FR) | icon_d2 | icon_d2 | — | — | icon_global | icon_d2, calcolati | 0% |
| Amsterdam (NL) | knmi_netherlands | knmi_netherlands | — | ecmwf_ifs | ecmwf_ifs | ecmwf_ifs, interpolati | 0% |
| Bruxelles (BE) | knmi_netherlands | knmi_netherlands | — | ecmwf_ifs | ecmwf_ifs | knmi_netherlands, interpolati | 0% |
| Londra (GB) | ukmo_2km | ukmo_2km | — | ukmo_10km | — | gfs_global, interpolati | 0% |
| Dublino (IE) | ukmo_2km | ukmo_2km | — | ukmo_10km | — | ukmo_2km, interpolati | 0% |
| Copenaghen (DK) | metno_nordic | metno_nordic | ecmwf_ifs | ecmwf_ifs | ecmwf_ifs | metno_nordic, interpolati | 0% |
| Odense (DK) | metno_nordic | metno_nordic | icon_eu | ecmwf_ifs | ecmwf_ifs | metno_nordic, interpolati | 0% |
| Oslo (NO) | metno_nordic | icon_eu/metno_nordic | — | ecmwf_ifs | ecmwf_ifs | metno_nordic, interpolati | 0% |
| Stoccolma (SE) | metno_nordic | metno_nordic | icon_eu | ecmwf_ifs | ecmwf_ifs | metno_nordic, interpolati | 0% |
| Helsinki (FI) | metno_nordic | metno_nordic | — | ecmwf_ifs | ecmwf_ifs | metno_nordic, interpolati | 0% |
| Varsavia (PL) | dmi_europe | dmi_europe | — | ecmwf_ifs | ecmwf_ifs | dmi_europe, interpolati | 0% |
| Breslavia (PL) | icon_d2 | icon_d2 | — | icon_eu | — | icon_d2, calcolati | 0% |
| Budapest (HU) | dmi_europe | dmi_europe | ecmwf_ifs | ecmwf_ifs | ecmwf_ifs | dmi_europe, interpolati | 0% |
| Zagabria (HR) | icon_d2 | icon_d2 | icon_eu | icon_eu | icon_global | icon_d2, calcolati | 0% |
| Bratislava (SK) | icon_d2 | icon_d2 | — | icon_global | — | icon_d2, calcolati | 0% |
| Madrid (ES) | — | — | — | — | — | —, interpolati | 0% |
| Lisbona (PT) | — | — | — | — | — | —, interpolati | 0% |
| Atene (GR) | — | — | icon_global | — | icon_global | —, interpolati | 0% |
| New York (US) | gfs_hrrr | gfs_hrrr | gfs_global | gfs_global | gfs_global | gfs_hrrr, calcolati | 0% |
| Sydney (AU) | ecmwf_ifs | ecmwf_ifs | ecmwf_ifs | ecmwf_ifs | ecmwf_ifs | icon_global, interpolati | 0% |
| Tokyo (JP) | jma_msm | jma_msm | jma_msm | ecmwf_ifs | ecmwf_ifs | jma_msm, interpolati | 0% |

**Il peso in chiamate per fetch**: previsione 5,2 (52 variabili, 7 giorni), testimoni 1,2, aria 1,4:
**7,8** dei 10 000 al giorno per indirizzo. Scritto accanto a ogni lista in `OpenMeteoApis.kt`.

**(b) Il quarto d'ora.** `NowcastCoverage.IconD2Countries` guadagna **FR, HR, SK, PL**: Parigi,
Lione, Tolosa, Bordeaux, Nizza, Zagabria, Bratislava, Breslavia hanno i quarti di `best_match`
identici a ICON-D2, temperatura e pioggia. La lista del 17 ott era sbagliata per la Francia
«perché va ad AROME»: nel sorgente di Open-Meteo la regola «ovunque ICON-D2 abbia dati» viene
prima. Restano fuori, misurate: Lussemburgo (KNMI), Varsavia e **Budapest** (DMI HARMONIE Europe:
ICON-D2 manca dalla risposta, il box regolare è più largo della griglia ruotata al bordo est, e
l'Ungheria non entra), Marsiglia (nella fascia di rilassamento, il margine di 0,25° la esclude già),
i nordici, le isole britanniche, l'Olanda e il Belgio. La forma della regola non cambia — paese
misurato **e** box meno il margine, `looksInterpolated` a chiudere — perché Budapest dimostra che il
box da solo non basta: una nazione entra nella lista solo con una città misurata dentro. La Francia
a sud del box (Marsiglia) non ha i quarti di AROME in `best_match` (temperatura di nessuno al 90%,
pioggia piatta): niente box AROME, finché una sonda bagnata non lo mostri. `NowcastCoverageTest`
porta le città nuove nei due sensi; il CHANGELOG dice la copertura nuova.

**(c) Le regole.** CLAUDE.md: «Numbers are one model's per hour» con la frase sulla media
vietata; la voce nuova sulla nebbia («Fog is never the code's alone», con la data DWD e il tile);
«The provider is a moving target»; «Free and without a key is a constraint, not a default», con
l'aritmetica delle chiamate, il divieto dello scoccare dell'ora e le API a pagamento lasciate agli
strumenti. VISION §12: le decisioni 10 (la nebbia dopo DWD), 11 (un modello più fine si misura prima
di usarlo), 12 (caldo e vento pericoli dell'app). I pesi in chiamate accanto alle tre richieste.

**Deviazioni.** La sonda era pensata per riconoscere il modello dalla temperatura, come
`Witnesses`: non funziona a sud del bordo (sopra), e riconosce dalla pioggia o dalla visibilità;
il JSON porta tutte e cinque le variabili per giorno, così 14.3 ha i numeri per il riconoscimento
per variabile. Il box AROME per la Francia del sud, previsto «se la sonda lo mostra», non si fa:
la sonda non lo mostra.
