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
- **In corso**: nessuna fase aperta; si lavora per interventi, su richiesta del committente.

## Aperto

Quel che resta da fare o da decidere, in un posto solo: una casella si apre qui, con il rimando
alla sezione del registro da cui nasce, e si chiude qui, con quella che la chiude.

- [ ] **Il cielo d'inverno contro il satellite.** La regola 6 è misurata su ottobre soltanto:
  l'archivio del satellite di Open-Meteo parte da febbraio 2026. Rilanciare
  `tools/measure_against_metar.py` su dicembre–gennaio 2026-27 e rileggere STATES §11 (registro,
  «Il motore dopo la revisione», 26 ott 2026).

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
