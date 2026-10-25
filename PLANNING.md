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

Nessuna casella aperta e nessuna proposta non decisa (riletto il 22 ott 2026, all'archivio 06):
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
