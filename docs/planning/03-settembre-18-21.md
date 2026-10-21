# PLANNING, archivio 03: 18-21 settembre 2026: widget, caratteri, Open-Meteo, Cielo, la 1.0.0

> Congelato il 21 ott 2026: un periodo chiuso del registro, spostato qui da `PLANNING.md`
> senza cambiarne una riga. Non si modifica. Il piano vivo è [`PLANNING.md`](../../PLANNING.md);
> l'indice di tutti i titoli è [`README.md`](README.md).

---

## L'intestazione di Oggi che non scorre via (committente, 18 set 2026)

Due richieste dal dispositivo, una per schermata e una per tutta l'app. La prima:
«l'intestazione con il nome della città su Oggi deve restare ferma, come nelle altre
schermate». La seconda: «sulla gesture back voglio la stessa dissolvenza fra le schermate
che c'è in Saldo» — fatta, guardata sul dispositivo e **ritirata dal committente stesso**
poche ore dopo, quindi di quel lavoro non resta niente in APK (sotto, cosa era e cos'è
tornato indietro).

### La riga del luogo, appuntata

Cielo, Allerte e Diario tengono la loro intestazione **fuori** dalla lista, in una `Column`
sopra di essa, quindi non scorre. Oggi no: la riga del luogo era il primo pezzo del
`CanvasHeader`, cioè del primo item della `LazyColumn`, e se ne andava in alto con il cielo.
Il modo più corto di pareggiare le altre tre — una riga sopra la lista, su `surface` — però
costava il cielo a filo dello schermo: il canvas possiede il bordo superiore da Fase 3
(DESIGN §3.6) e la fascia di scrim in cima esiste proprio per la riga del luogo e le icone
di stato.

Si è fatta invece la cosa che DESIGN §8.1 aveva già scritto («collapses on scroll into the
app bar, keeping place and temperature»): la riga è **appuntata sopra la lista**, dentro il
`PullToRefreshBox` così che l'indicatore del pull le scenda davanti, e ha **due terreni**.

- In cima alla pagina non ha terreno: bianco sulla fascia di scrim, che è dove §3.6 misura
  i suoi 5,27:1.
- Dal **primo pixel** di scroll prende `surface` e l'inchiostro del tema, con la molla
  `effects` di §7 a fare il passaggio.

Niente di intermedio, e questa è la decisione da ricordare: la soglia misurata che c'era
prima (`SkyCanvasTopScrimEnd` sull'altezza reale del canvas) teneva il bianco finché la
fascia stava dietro la **status bar**, e la riga del luogo scende 48dp più in basso della
status bar — con quella soglia, per qualche decina di dp, il nome della città sarebbe stato
bianco su cielo non scrimmato. Ora la soglia è una sola, sta in `atTop`, e le icone della
status bar la seguono perché quel che sta dietro la status bar **è** questa barra.

Il canvas tiene il posto della riga con uno `Spacer` alto quanto la barra **misurata**
(`onSizeChanged`), non quanto una costante: la riga cresce con la scala del testo, con i
pallini del pager e con l'ora del luogo, e `PlainHeaderHeight` (48 + 8 + 8) è solo il
pavimento su cui sta prima del primo layout. Con lo `SpaceBetween` che c'era già, l'eroe
atterra dove atterrava prima e il bordo inferiore del canvas resta a `status + 280dp`: lo
scheletro, che quella somma la quotava già (`CanvasBaseHeight - PlainHeaderHeight`), non è
stato toccato e continua a combaciare.

Gli stati senza report (scheletro, nessun luogo, vuoto) avevano già l'intestazione ferma
sopra il contenuto e sono rimasti come erano.

### La dissolvenza fra le schermate: fatta, e tolta lo stesso giorno

La seconda richiesta è stata implementata e poi **rimossa su richiesta del committente**,
che ha preferito lo scambio istantaneo di prima. Sta qui perché il tentativo è un dato,
non per rimpianto: se la domanda torna, questo è quel che c'era.

Chiaro non ha un nav graph (due enum di stato e un `BackHandler`, scelta di Fase 4), quindi
la forma di Saldo — fade + slide di un sesto di schermo — era stata ottenuta con un
`AnimatedContent` su un `SeekableTransitionState` in un solo composable riusabile
(`ui/shell/ScreenSwap.kt`), con un `PredictiveBackHandler` che faceva `seekTo` a ogni evento
del gesto: è l'unico modo di far seguire il dito alla transizione invece di farla partire a
gesto concluso. Tre chiamanti: lo shell (i quattro tab, Impostazioni, la guida), il tab
Cielo e la guida (tonight/tour ↔ documento degli eventi). Aveva richiesto anche di stringere
il `BackHandler` di `SkyGuideRoute` al solo livello pagina → indice, perché un handler che
chiude se stesso consuma il gesto e non anima nulla.

Il ritorno è pulito e verificato come tale: i quattro file toccati (`ChiaroRoot.kt`,
`SkyScreen.kt`, `SkyGuideScreen.kt`, `GuideScreen.kt`) sono **identici byte per byte** alla
versione precedente, `ScreenSwap.kt` non esiste, e DESIGN §7 è tornato alle sue quattro
animazioni. Di questa richiesta resta solo la riga del luogo appuntata, che è l'altra.

### Come è stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verde, 793 test.
Il resto — che il nome della città non scorra più via e che la barra cambi terreno quando
deve — è una prova da dispositivo: qui non c'è.

---

## Il widget senza niente di disegnato (committente, 19 set 2026)

«Crea un nuovo widget 4x1 (ridimensionabile) ma con informazioni solo testuali. Niente
icona meteo o altra grafica. Decidi tu, per ogni tipologia di informazione da visualizzare,
il modo migliore per: layout, dimensione testo, testo grassetto, ecc. Deve risultare una
tipografia ordinata e leggibile con info principali in primo piano.»

È il quinto widget della casa e il primo che non disegna niente. Si chiama **«In parole»**
(`In words` in inglese), `TextWidget` nel codice.

### La cosa da capire prima di scrivere una riga

Il primo istinto è «il widget Ora senza l'icona», e sarebbe sbagliato. Le altre quattro
schede prendono la gerarchia **dal disegno**: il glifo Meteocons riempie l'altezza che il
launcher concede ed è la prima cosa che si legge a distanza di braccio — sta scritto in
VISION §5.9 e la taglia del glifo è aritmetica in tre file di layout. Togli il disegno e la
scheda resta senza primo piano: un 34 sp e un 16 sp uno sopra l'altro non sono una
gerarchia, sono due righe.

Quindi la gerarchia si è **ricostruita con il carattere**, e la regola che tiene tutto in
piedi è una sola: **quattro ranghi che si distinguono per taglia E peso E inchiostro,** mai
per uno solo dei tre. Un rango che si distinguesse solo per taglia si sfalda quando il
lettore alza il font di sistema; uno che si distinguesse solo per inchiostro sparisce su una
carta semitrasparente.

| rango | cosa | taglia | peso | inchiostro |
|---|---|---|---|---|
| 1 | la temperatura | 30…56 sp, scalata alla concessione | **Bold** | forte |
| 2 | la frase del giorno | 17 sp | Medium | forte |
| 3 | il luogo, massima/minima, la parola dell'allerta, le temperature delle ore | 14 sp | Regular il luogo, Medium le cifre | quieto il luogo, forte le cifre |
| 4 | il marcatore di vecchiaia, le etichette delle ore | 11 sp | Regular | freschezza il marcatore, quieto le etichette |

**Il numero è il disegno, adesso.** Bold dove tutta la casa scrive Medium — il peso è uno
dei tre assi, e il rango in cima è l'unico che può permetterseli tutti e tre — e soprattutto
**scalato alla concessione**, esattamente come le altre quattro scalano il glifo. Una
taglia fissa può essere giusta su una sola dimensione di cella: `heroIconSize` risolve il
problema per un disegno, `textSizeForLine` (nuovo in `WidgetUi.kt`, l'inverso di
`textLineHeight`) lo risolve per una cifra. Sulla riga di riferimento da 85 dp il numero
esce a **41,3 sp** contro i 34 che il widget Ora stampa accanto al suo glifo; su una scheda
alta arriva al soffitto di 56.

I 17 sp della frase sono un punto sopra il 16 della casa, e il punto è pagato dall'assenza
del disegno: su questa scheda la frase **è** la seconda cosa da leggere, non la didascalia
di un'immagine. Due punti no: a 18 la colonna da 174 dp della scheda a quattro celle smette
di tenere «Pioggia gelata verso le 15:00» in due righe.

### Le tre forme, e la cella che si guadagna

Stessa grammatica di `NowLayout`, perché le schede stanno sullo stesso schermo:

- **`LINE`** — una riga troppo stretta per due colonne: luogo, numero, vecchiaia. Basta.
- **`ROW`** — una riga con la seconda colonna, che è la collocazione 4×1 di default: il
  luogo sopra il numero a sinistra, e al bordo opposto la frase con sotto l'allerta e la
  massima/minima.
- **`STACK`** — due righe in su: una colonna sola, centrata sull'altezza che non riempie,
  con le prossime ore stampate come cifre dove l'altezza le regge.

**A tre celle questa scheda porta già la frase, il widget Ora no** (`NowLayout.NARROW` a
250 dp). Non è una scelta di gusto, è la stessa aritmetica con un addendo in meno: i 66 dp
di glifo più gli 8 di distacco che quella scheda spende prima della prima lettera, questa
non li ha. Il test lo mette una accanto all'altra, perché è la differenza che giustifica
l'esistenza di due widget.

La colonna di sinistra è una **quota** (42%) e non la metà esatta che usa il widget Ora: le
due colonne non fanno lo stesso lavoro, una tiene un numero e un nome — entrambi corti — e
l'altra tiene prosa, che è la cosa che ha bisogno di misura. Sulla scheda di riferimento a
quattro celle fa 126 dp contro 174; sotto c'è un pavimento di 96 dp, che è quel che serve a
«−12°» al piano del numero e a un nome di dieci lettere.

### L'ordine in cui si spende, che è la gerarchia scritta

Su scheda alta il budget compra in quest'ordine, e l'ordine **è** l'argomento di layout:

1. il numero, prenotato subito a 40 sp — su una scheda alta è il titolo della pagina, e un
   titolo che si rimpicciolisce perché ci stia una nota a piè di pagina è la scheda che
   litiga con sé stessa;
2. le due righe di frase;
3. la parola dell'allerta;
4. massima e minima;
5. le prossime ore;
6. quel che avanza torna al numero, fino a 56 sp.

Il luogo e il marcatore di vecchiaia non sono nell'ordine perché non sono mai facoltativi:
una scheda senza luogo è un numero su nessun posto, e una senza età mente su quanto è
vecchia (VISION §5.9). Sulla riga da 85 dp il marcatore si paga con il numero, che scende a
30,3 sp — il pavimento, centrato lì apposta.

**Due righe di frase e mai tre** su scheda alta: è il numero della scheda alta del widget
Ora (`TallSentenceMaxLines`) e lo stesso argomento — lì la terza riga costava 21 dp al
glifo, qui costa 13 sp al numero. Sulla 4×2 di riferimento è la differenza fra un 56 sp e un
43 sp, per una riga che nel registro breve a 340 dp di misura non serve quasi mai. Sulla
riga singola invece la terza riga resta, perché lì non c'è nessun numero che la paghi: la
colonna la usa solo se non c'è altro da dire.

### Le due cose che un disegno diceva e ora dicono le parole

- **La posizione del telefono** era il segnaposto (`ic_place_pin`) su tutte le altre schede.
  Qui è `widget_text_place_gps`, «Milano · la mia posizione»: da dove viene un numero fa
  parte della sua verità (la regola dell'intestazione di Oggi, §5.1), e una scheda senza
  glifi deve dirlo invece di marcarlo.
- **L'allerta ufficiale** era una pastiglia. La pastiglia esiste per una ragione misurata,
  non decorativa: il terreno di un widget è un cielo con lo scrim o lo sfondo di qualcun
  altro, e **una parola colorata nuda su uno di quei terreni è risultata illeggibile** (4
  set, sui verdetti della scheda Cielo — «un verde nudo era difficile da leggere su una
  scheda scura»). La pastiglia risponde portandosi dietro il proprio terreno misurato.
  Una scheda il cui presupposto è che non si disegna niente **non può portarselo**, quindi
  rinuncia al colore e non alla leggibilità: stampa «Allerta gialla» nell'inchiostro forte
  della scheda, al rango 3. Non è uno sconto sulla regola ma il suo pavimento — DESIGN §2.3
  fa della parola il vettore e del colore il rinforzo, e qui c'è solo il vettore. Per il
  resto decide la stessa tabella `warningSlot` delle altre quattro: arancione e rosso stanno
  già nella frase, il giallo prende una riga sua dove ce n'è una in più.

### Cosa si è deciso di NON mettere

- **La riga della pioggia sotto le ore.** Le ore stampano etichetta e temperatura, non la
  probabilità. La pioggia, quando conta, è già nella frase del giorno («Pioggia verso le
  cinque»): una seconda affermazione sulla pioggia in cifre, a un rango più basso, sarebbe
  la stessa cosa detta due volte — che è l'argomento con cui la tabella dell'allerta tiene
  la pastiglia fuori dalle schede arancioni.
- **Il maiuscoletto per il nome del luogo.** Tipograficamente un'etichetta tutta maiuscola
  sopra un numero grande funziona, ma TalkBack su alcune versioni la compita lettera per
  lettera, e un nome proprio accentato in maiuscolo è una decisione sulla lingua che questo
  file non ha titolo di prendere. Il rango basta a farla leggere come etichetta.
- **Lo sfondo.** Resta quello della casa (il cielo calcolato, di default, più chiaro/scuro/
  sistema e l'opacità), perché è il vestito della scheda e non il suo contenuto, ed è
  misurato: lo scrim §3.6 esiste perché l'inchiostro bianco tenga il suo pavimento. Chi
  vuole la tipografia pura su carta piatta la sceglie dalle impostazioni della scheda, che
  sono un tocco lungo di distanza.
- **La famiglia di icone** non viene più offerta nella schermata di configurazione quando la
  scheda è questa: un interruttore che non cambia niente non si offre (la riga che c'era
  già per la massima/minima del widget Oggi). Massima/minima invece **viene** offerta anche
  qui, e per la stessa ragione per cui la offre Oggi: c'è una colonna di fatti in cui
  metterla, dove sul widget Ora c'era solo il bordo della frase da affollare.

### I contenitori di Glance, contati di nuovo

Glance disegna al massimo dieci figli per contenitore e **scarta il resto senza dire
niente** (Fase 11, dove aveva già mangiato le ultime due ore della striscia di Oggi). La
colonna della forma `STACK` ne ha sette al massimo, le due colonne della `ROW` tre ciascuna,
la riga delle ore sei — e sei è il soffitto di `textHourCells` proprio per quello. Ogni
distacco è **padding**, mai uno `Spacer`: una riga che costa due figli costa il doppio di
quel che sembra.

### Quel che è cambiato fuori dal widget

- `WidgetUi.kt`: `textSizeForLine`, l'inverso di `textLineHeight`, e un parametro `size` su
  `DayRange` (era fissa a 16 sp; qui la coppia sta al rango 3, a 14).
- `ChiaroWidgets`: `WidgetKind.TEXT`, il ricevitore, la famiglia. `WidgetConfigActivity`:
  niente icone per questa scheda, sì frase/allerta/massima e minima.
- `WidgetPreviewTest` conta cinque provider e cinque anteprime invece di quattro. È un
  conteggio asserito e non derivato apposta: un provider aggiunto senza anteprima deve
  fallire lì e non sullo schermo di casa di qualcuno.

### Come è stato verificato

`./gradlew :app:compileDebugKotlin`, `test :app:testDebugUnitTest`, `:app:lintDebug` e
`:app:assembleDebug` verdi, **808 test** (i 793 di prima più i 15 di
`TextWidgetLayoutTest`), lint senza un rilievo sui file nuovi, e l'APK contiene
`res/xml/widget_text_info.xml` e `res/layout/widget_text_preview.xml`. Il resto — come legge
davvero la scheda su uno schermo di casa, a quali taglie il launcher la concede, se 41 sp
sono i 41 sp giusti — è una prova da dispositivo: qui non c'è.

### Il secondo giro, dal dispositivo (committente, 19 set 2026)

Quattro richieste dopo la prima prova su schermo di casa, con screenshot: la card 4×1 sul
cielo notturno, «Ornago · la mia posizi…» troncato, «24°», «Quasi sereno», «25° / 16°».

**1. Il segnaposto torna.** «Invece della scritta "la mia posizione" metti il simbolo del
GPS davanti alla località.» La prima versione l'aveva messa a parole apposta — una scheda
che non disegna niente dice quel che le altre marcano — e lo screenshot ha mostrato il costo
esatto di quella coerenza: la riga era **tutta** la nota, e il nome del posto ci finiva
dentro troncato. Il segno costa 16 dp di inchiostro, dice la stessa cosa e lascia il nome
intero. È lo stesso `ic_place_pin` della schermata e delle altre quattro schede, quindi le
cinque non possono litigare su che aspetto ha «la mia posizione».

Da qui una regola che vale anche per il punto 2 e che adesso sta nell'intestazione di
`TextWidget.kt`: **un segno alla misura della riga cui appartiene, tinto con l'inchiostro di
quella riga, è punteggiatura, non grafica.** «Niente icona meteo o altra grafica» resta
intero: sulla scheda non c'è nessun disegno del meteo, nessuna pastiglia, nessuna
illustrazione.

**2. Le frecce su e giù.** «Temperatura max e min un po' più grandi e con le frecce su e giù
ad indicare massima e minima.» Sono due `vector` nuovi (`ic_range_high`, `ic_range_low`) e
non i caratteri ↑ e ↓, per la ragione già misurata il 9 set sui marchi dei verdetti: un
carattere che il font di sistema non ha lo disegna un font di ripiego, con la sua mano e il
suo peso, e il committente lesse la ✗ come scrittura a mano. Stessa costruzione dei marchi:
un tracciato a 2.4 di 24, cuspidi tonde, ≈1.6 dp alla taglia a cui si mostrano.

La coppia resta **un solo composable**, `DayRange`, con un parametro `marks`. Non due
grammatiche per la stessa cosa: è la stessa frase a due budget. I segni costano ~25 dp, che
la colonna da 174 dp del widget testuale porta e quella da 113 dp del widget Oggi no — quindi
Oggi tiene la barra e la scheda che ha lo spazio lo dice per esteso. L'enfasi non cambia in
nessuno dei due casi: la massima prima e forte, la minima dopo e smorzata, e ogni segno
prende l'inchiostro della cifra davanti a cui sta.

**3. Il rango 3 sale, e il pavimento dell'eroe scende.** «La località un pochino più
grande», e le due temperature pure. Il rango 3 va da 14 a **16 sp**, che è il numero con cui
tutte le altre schede stampano un luogo e una massima/minima: le cinque adesso sono d'accordo
su quanto è grande un fatto. Ma 17 sopra 16 non è un rango, è un arrotondamento, quindi la
frase del giorno sale a **18**. Due punti, con Medium contro Regular e forte contro quieto,
è il salto più piccolo che si ordina ancora a distanza di braccio; a 19 la colonna da 174 dp
smette di tenere «Pioggia gelata verso le 15:00» in due righe, ed è la misura che ha fermato
il numero lì.

Il seguito è aritmetico e va detto perché è il genere di cosa che si rompe in silenzio: la
riga del luogo è cresciuta di 2,6 dp, quindi su una riga da 85 dp con il marcatore di
vecchiaia il numero resta con 37,4 dp, cioè 28,3 sp — **sotto il vecchio pavimento di 30**.
Un pavimento che non si può pagare non è un pavimento, è una riga tagliata. È sceso a **26**,
e così torna a essere quel che deve essere: il punto sotto il quale una scheda strizzata
smette di rimpicciolire la cifra, non una taglia che una concessione misurata raggiunge. Con
dati freschi quella stessa riga legge 39,3 sp.

**4. Lo sfondo colorato.** «Possibilità di mettere uno sfondo colorato: blu, blu chiaro,
verde…» Sei colori — blu, blu chiaro, verde, verde acqua, viola, terracotta — accanto alle
quattro scelte che c'erano già, e **su tutti e cinque i widget**, non solo su questo: il
colore è una proprietà della scheda, non di quel che ci sta stampato sopra.

Due decisioni dentro la decisione.

- **Nessun inchiostro nuovo.** Inchiostro e terreno sono una coppia (§2.3), e quanto costa
  spezzarla è stato misurato il 4 set sui verdetti della scheda Cielo. Quindi invece di sei
  terne di inchiostri, ogni colore è scelto **abbastanza scuro da portare la coppia che
  l'app ha già e ha già misurato**: il bianco §3.6 del cielo con lo scrim, pieno per
  l'inchiostro, 75% per quello quieto, 85% per quello della freschezza. Diciotto misure in
  DESIGN §2.6, asserite da `PaletteContrastTest` e confrontate con il documento da
  `PaletteDocTest`. Il pavimento che conta è il quieto, perché porta un'etichetta d'ora da
  11 sp: il peggiore dei sei dà 5,2:1 contro i 4,5 che servono.
- **Sei colori devono essere sei colori.** Nessuna coppia sotto **13 ΔE**, altrimenti a
  scegliere è il nome e non il colore. Il blu del primo tentativo (`#14477A`) stava a 9,4 dal
  blu chiaro accanto: è sceso a `#0F3B6B`, che è anche il blu che un lettore si aspetta di
  vedere quando l'altro si chiama «blu chiaro».

`WidgetBackground` prende **un** valore nuovo, `COLOR`, e non sei: quale colore è una seconda
domanda, e la si fa solo a chi ha scelto COLOR (`WidgetLook.cardColor`), così ogni `when` su
quell'enum resta di quattro righe. Sotto `InkTrustFloorPct` una scheda colorata passa la
domanda dell'inchiostro alla carta da parati come fanno il cielo e la scheda di sistema:
scegliere un colore è scegliere un **terreno**, non nominare un inchiostro, e al 20% di
solidità quel terreno in gran parte non c'è. Chiaro e scuro continuano a decidere a qualsiasi
solidità, perché quelli sì sono il lettore che nomina un inchiostro.

Le due schermate di configurazione stampavano già le stesse quattro righe da due copie della
stessa lista, ed è esattamente il posto in cui un quinto tipo lascia indietro una delle due
copie: adesso c'è un `BackgroundSection` solo, usato da entrambe, con le righe dei colori
annidate sotto la scelta «Un colore» e una pastiglia del colore in fondo a ognuna — con il
nome davanti, perché una pastiglia non è un'etichetta (§10).

### Come è stato verificato (secondo giro)

`./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verdi, **812 test**
(i 15 del widget più 4 nuovi: le diciotto misure dei colori, la distanza fra i sei, il fatto
che nessuna scheda solida interroghi la carta da parati, e il confronto fra la tabella di
§2.6 e il codice). Lint senza rilievi. Come legge la scheda colorata su uno schermo di casa
resta una prova da dispositivo.

### Il terzo giro: le ore se ne vanno, il pannello, e il nome intero (committente, 19 set 2026)

Screenshot con le due schede affiancate — 4×1 e 4×2, sfondo blu — e tre richieste.

**1. Le temperature orarie via, e sempre.** «Praticamente facciamo sempre senza temperature
orarie.» Erano l'unica cosa che la scheda alta guadagnava in più, ed è la prima volta che
una feature di questa serie viene tolta per come **legge** e non per un difetto: sullo
schermo di casa le sei colonne sotto il blocco di testo si leggevano come un secondo widget
pinzato sotto il primo. La scheda che esiste per le ore è il widget Oggi, e ce l'ha con i
glifi. Via `textHourCells`, `textHoursHeight`, `TextHourTempSp`, i quattro numeri della
cella e il campo `showHours` del piano.

**2. Allineato in basso, e un pannello da quattro colonne in su.** «Allinea in basso testo e
(se impostata) temperatura minima e massima. Se la dimensione poi è su 2 righe e le colonne
sono almeno 4 valuta di cambiare il layout con la temperatura allineata in basso con font un
po' più grande e testo e temperature max e min allineate a destra in basso.»

Le due righe adesso **appuntano il luogo in cima** e tutto il resto **in fondo**, con l'aria
in mezzo. È quel che l'altezza compra al posto delle ore, ed è la differenza fra una
composizione e un elenco che è finito: `Spacer` pesato fra l'occhiello e il blocco, un figlio
solo (Glance ne disegna dieci per contenitore e scarta il resto senza dirlo).

Da quattro celle in su c'è una forma nuova, `TextForm.PANEL`: l'occhiello a tutta larghezza
in cima, e lungo il fondo il numero a sinistra con la frase e la massima/minima allineate a
destra, le due colonne **allineate in basso** così che la linea di base del numero e quella
della coppia cadano insieme — che è tutto il senso di «allineate a destra in basso».

E qui il numero può essere più grande, con una ragione e non a occhio: nelle altre forme sta
**sotto** le parole, qui sta **accanto**. Vale la regola che il widget Ora scrive per il suo
glifo (`RowIconMax`: l'eroe non supera mai il blocco che gli sta a fianco), letta su un
blocco diverso — due righe di frase più la parola dell'allerta più la coppia fanno 89,8 dp, e
64 sp hanno una scatola di riga di 84,5. Quindi tre soffitti, uno per forma: **44** dove sta
sotto il luogo con una colonna di prosa a fianco, **56** sulla scheda alta stretta dove sta
sopra la sua frase, **64** sul pannello.

La soglia «almeno 4 colonne» è 300 dp (`TextPanelMinWidth`), misurata come `TallMinHeight`:
quattro celle sono ~340 dp sul dispositivo di riferimento e ~320 su una griglia da cinque
colonne, tre sono ~250. Un widget non sa quante celle ha avuto.

La colonna di sinistra del pannello è **fissa** a 140 dp e non è `textLeadingColumn`: lì
dentro ci sta una cifra, non un nome — il nome è l'occhiello sopra entrambe le colonne — e
140 è «−12°» al soffitto (134,4) più un paio di dp, così a decidere la taglia del numero è il
soffitto e non la larghezza.

**3. Il nome lungo, intero.** «Quando è su una riga vorrei che si riesca a vedere
completamente una località lunga come "Cavenago di Brianza" lasciando così il layout e le
dimensioni dei vari testi.»

La colonna di sinistra era il 42% dello slack della riga: 126 dp, contro i 165 che
«⌖ Cavenago di Brianza» vuole (145 misurati di nome a 16 sp, più la scatola del segnaposto e
la sua aria). Il risultato era «Cavenago di Bri…» su una riga che aveva 174 dp di bianco
nell'altra colonna. **Una quota non può risolverlo**, perché le due colonne non vogliono la
stessa cosa: quella di sinistra vuole esattamente quanto le serve al nome che tiene, quella
di destra vuole una misura.

Quindi: ogni dp oltre il minimo della frase va al nome finché il nome è soddisfatto.

```
lead = (slack − 104).coerceAtMost(168).coerceAtLeast(96)
```

Quattro celle: 168 contro 132 (prima 126 contro 174). Tre celle: 106, e la frase resta
esattamente sul suo minimo — la forma che questa scheda guadagna a tre celle non viene
restituita. Due celle: il pavimento, e 23 dp di avanzo, che non è una colonna. Niente
cambia nelle taglie dei testi né nella disposizione, che era la condizione.

Un controllo che valeva la pena fare: la coppia con le frecce a 16 sp misura ~95 dp con due
numeri a due cifre e ~113 con due «−12°», quindi ci sta nei 132 della colonna nuova senza
tagli.

### Come è stato verificato (terzo giro)

`./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verdi, **814 test**,
di cui 17 sul layout di questa scheda — fra questi uno che, per ogni concessione che è un
pannello, con e senza marcatore di vecchiaia e a due scale di font, ricalcola le due colonne
e verifica che la più alta stia dentro la card. Lint senza rilievi. Come legge davvero il
pannello su uno schermo di casa resta una prova da dispositivo.

## L'icona del meteo sul widget testuale, disegnata nell'aria (committente, 20 set 2026)

«Per ogni layout/dimensione del widget senza cambiare assolutamente niente del testo che c'è
adesso, dove si potrebbe aggiungere anche l'icona del meteo attuale, attivabile da
un'impostazione.» E, in coda: se viene bene si potrebbe togliere «A colpo d'occhio».

### La condizione detta bene, perché è tutto il progetto

«Senza cambiare niente del testo» non vuol dire «con le stesse taglie». Vuol dire che
**nessun budget può cambiare**: se il glifo fosse una sezione per cui la card fa spazio,
qualcuno pagherebbe — una riga di frase, la parola dell'allerta, la coppia max/min, o dp di
numero. Quindi l'impianto è rovesciato rispetto alle altre quattro schede.

Le altre quattro **prenotano il disegno per primo** e sistemano le parole intorno. Questa
calcola tutti e quattro i suoi piani **come se il glifo non esistesse**, e solo dopo chiede
se quel che avanza basta a tenerne uno. Le quattro funzioni nuove in `TextWidgetLayout`
(`textIconSize`, `textRowIconSize`, `textStackIconSize`, `textPanelIconSize`) restituiscono
**una taglia o niente**, e nessuna di loro è input di un piano. Dove non c'è aria non c'è
disegno: è la regola «una sezione che non ci sta non si disegna», applicata alla cosa che
questa card era nata per non avere.

### I due posti dove l'aria c'è già

**Accanto al numero** (`LINE`, `ROW`, `STACK`). Una temperatura è larga al massimo
[TempEmWidth] = 2,1 em — «−12°» — e il resto della sua riga è aria. Misurata contro il
numero **più largo che l'app possa stampare**, mai contro quello che sta stampando: la
regola che il widget Cielo scrive per le sue forme, perché una card che cambia forma con la
previsione non si può mirare.

**Sopra le parole** (`PANEL`). Quella forma appunta l'occhiello in cima e il blocco in fondo,
quindi la fascia in mezzo è vuota per costruzione: è quel che l'altezza compra da quando le
ore se ne sono andate. Il glifo ci si appende.

Su una riga il glifo è **sovrapposto** (`Box`) e non affiancato (`Row`): in una `Row` si
prenderebbe la sua larghezza dalla colonna, e il nome del posto — la riga che questa card ha
combattuto per stampare intera due giri fa — tornerebbe a troncarsi. Sovrapposto, le parole
tengono ogni dp e il glifo sta nell'angolo che il numero lascia vuoto.

Sul panel invece il glifo è il **primo figlio della colonna di destra**, non una riga fra lo
spacer e il blocco. La prima versione l'ho scritta come riga a sé e **era sbagliata**: in una
`Column` l'altezza si somma, quindi occhiello + glifo + riga bassa sforava di 15,8 dp sulla
4×2. Dentro la colonna di destra invece la colonna cresce **verso l'alto** dentro la fascia
che lo spacer teneva, l'allineamento in basso della riga lascia il numero esattamente dov'era
(verificato: 76,5..161 con e senza glifo, e 62,1..161 con il marcatore di vecchiaia), e
`textPanelIconSize` è per costruzione l'altezza che quella fascia aveva.

### Quel che ne esce, misurato

| forma | grant | glifo |
|---|---|---|
| LINE 1 cella | 110×85 | niente (0 dp d'aria) |
| LINE 2 celle | 159×85 | **51,9 dp** |
| ROW 3 celle | 250×85 | niente (23,5 dp) |
| ROW 3,5 celle | 300×85 | **51,9 dp** |
| ROW 4 celle | 340×85 | **51,9 dp** |
| ROW 4 celle, riga alta | 340×101 | **58,1 dp** |
| STACK 2 celle | 159×189 | niente (27,7 dp) |
| STACK 2,5 celle | 205×189 | **71,2 dp** |
| PANEL 4×2 | 340×189 | **71,2 dp** |
| PANEL 4×2 con allerta | 340×189 | **50,1 dp** |
| PANEL 4×3 | 340×293 | **104 dp** (soffitto) |

Due cose sono cambiate rispetto alla valutazione che avevo dato in chat, e vanno dette:
il pavimento unico a 48 dp fa sì che **nella giornata con allerta il panel il glifo lo
tiene** (50,1 dp) invece di perderlo, e il marcatore di vecchiaia **non costa niente al
glifo del panel**, perché vive nella colonna di sinistra mentre la fascia si misura su
quella di destra.

Il pavimento è 48 e non i 52 della famiglia, di un passo: sulle altre quattro schede il glifo
**è** l'eroe e 52 è dove smette di reggere la card a distanza di braccio; qui l'eroe è il
numero e il glifo è il suo accompagnatore, alla taglia ottica del numero — 48 dp di scatola
sono ~33 di inchiostro, che è quel che una cifra da 39 sp mette sulla stessa riga.

### Il bordo, che sono dieci dp e una regola vecchia

Un bordo con un glifo prende 4 dp, uno con parole 14 (`WidgetCardPaddingLeading`, misurato
sul widget Ora il 7 set: i disegni portano 9–12,5 dp di margine loro). Sulle tre forme in cui
è il disegno a toccare il bordo della card — tutte tranne `ROW`, dove il glifo è interno alla
colonna del nome — la card gli dà il bordo del glifo e **il testo restituisce i dieci dp**
(`TextIconEdgeGive`). Così ogni riga si misura contro esattamente la larghezza di prima: è la
promessa «non si muove niente» scritta come un numero solo. Senza, il glifo si sarebbe
fermato ~10 dp più dentro della frase sopra cui sta, che è il difetto che il repo ha già
registrato una volta.

### L'interruttore

`WidgetLook.showIcon`, **spento di default**, nella schermata della singola card (pressione
lunga → matita), in cima alla sezione «Contenuto» perché decide che **tipo** di card è e non
cosa la card dice. Spento di default è il nome della scheda che mantiene la parola: «In
parole» deve essere vero nel momento in cui la si posa. Per widget e non nelle impostazioni
dell'app, come tutti gli altri interruttori di contenuto, così lo stesso schermo può portare
la stessa card due volte, una con il glifo e una senza — che è metà del perché l'opzione vale.

Acceso, riappare anche la scelta della famiglia di icone per questa scheda, nascosta finché
non disegnava niente. E il glifo prende la parola della condizione per chi legge con TalkBack
**solo quando la frase è spenta**: con la frase accesa lo direbbe due volte, senza frase
sarebbe un disegno senza nome.

### «Togliamo A colpo d'occhio?» — no, e non per estetica

Il fattore che decide non è il design: togliere `NowWidgetReceiver` dal manifest
**orfanizza ogni widget già posato**, perché Android non ha migrazione fra provider. Oltre a
quello si perderebbero la disposizione speculare, il glifo a 2 e 3 celle su una riga, e i
66 dp contro 52 sulla riga a quattro celle. Decisione del committente: si tiene.

### Una stringa che mentiva

`widget_text_desc` prometteva ancora «e, su una card più alta, le prossime ore», tolte il
19 set. Corretta nello stesso giro, in entrambe le lingue.

### Come è stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verdi, **821 test**
(sette nuovi). Due di quei sette non controllano dove va il glifo ma **quanto costa**: per
ogni forma, a tre scale di font e con e senza marcatore di vecchiaia, rifanno l'aritmetica
della card e verificano che il glifo stia sotto la riga del luogo, non copra il numero al suo
più largo, lasci sei em al marcatore, e che il panel non cresca per farlo entrare. È il
genere di rottura che nessuno segnala come bug: una riga di frase tagliata il giorno in cui
si accende un interruttore.

---

## Il carattere dell'app, che ora si sceglie (committente, 20 set 2026)

«L'app usa il font Inter mentre il widget penso usi il font di sistema: è corretto? Mi piace
il font usato dal widget "In parole" e anche il bold usato per la temperatura. Vorrei che
anche l'app utilizzi questo font… magari si potrebbe mettere nelle opzioni un parametro per
impostare quale font utilizzare.»

### La diagnosi era giusta, e una metà non è il font

L'app è in Inter, incluso nell'APK come font variabile (`Type.kt`, `res/font/inter_variable.ttf`,
880 KB, OFL). Le card sono nel carattere di sistema **e non per scelta**: Glance disegna
attraverso `RemoteViews`, che non ha un'API di famiglia, quindi `androidx.glance.text.TextStyle`
ha colore, dimensione e peso e basta. Il codice lo dava già per scontato in due punti —
`TextWidgetLayout` misura «−12°» su Roboto Bold (`TempEmWidth`), `ArcPainter` dipinge con
`Typeface.DEFAULT`.

La parte che va detta perché non si perda: **fra la temperatura del widget e quella dell'app
la differenza più grande non è la famiglia, è il peso.** La card la stampa in Bold, la
schermata in Light 300 a 64 sp, e il Light è una decisione scritta (§5: «un numero in un peso
da testo si legge come un titolo, non come una lettura»). Questo giro non la tocca: l'opzione
sposta la famiglia e **nient'altro** (`TypographyFamilyTest` verifica che dimensioni, pesi e
interlinee siano identici nelle due risposte), così se dopo il peso dell'eroe va cambiato lo
si cambia da solo, sapendo che cosa si sta cambiando. Resta aperto.

### La coerenza si può ottenere solo spostando l'app

Non esiste il verso opposto. Portare Inter dentro i quattro widget Glance si potrebbe solo con
`AndroidRemoteViews` e una `SpannableString` con `TypefaceSpan`, cioè rifacendo a mano ogni
forma di ogni card e rinunciando a quel che Glance dà: fragile, e per di più andrebbe contro
le larghezze in em già misurate. Il quinto, l'arco, dipinge su Canvas e tecnicamente potrebbe
(`Typeface` da `ResourcesCompat.getFont`), ma sarebbe l'unica card diversa dalle altre quattro,
che è peggio dell'incoerenza che risolve. Quindi: la si ottiene muovendo l'app, o non la si
ottiene.

### Quel che costa il carattere di sistema, tutto in silenzio

Tre cose, ed è per queste che il predefinito resta Inter e non diventa «di sistema»:

1. **I pesi sono quelli che il dispositivo ha.** Dove manca, Android lo sintetizza spalmando
   gli outline — esattamente il difetto che il commento in `Type.kt` dice di aver evitato
   includendo il variabile. Il candidato è proprio il 300 dell'eroe.
2. **`tnum` può non fare niente.** Una famiglia senza cifre tabulari ignora la feature senza
   errore: nessun crash, solo colonne di numeri che smettono di essere colonne. Inter ce l'ha;
   di un font OEM non si sa, e **non è verificabile da qui**: va guardato sul dispositivo.
3. **Le colonne in dp erano misurate su Inter** (`TextScale.kt`: i 44/36/34/34 della riga
   della settimana, la cella dell'ora, l'orologio della timeline). Una faccia più larga le fa
   riflettere un passo prima. Non taglia niente — in `ui/` non c'è un solo `maxLines`, ed è
   deliberato — ma un valore può andare a capo prima di quanto dica la misura.

E una quarta che non è un costo ma una conseguenza: l'app smette di avere un aspetto suo. È lo
stesso scambio del colore dinamico, fatto sul carattere invece che sul colore, e si risolve
allo stesso modo: la cosa dell'app di default, la cosa del telefono a richiesta.

### Come è fatto

`AppFont { INTER, SYSTEM }` accanto a `ThemeMode` e `AppPalette` in `:core:data` — solo UI,
nessun motore la legge — con la chiave `appearance_font` e il solito ritorno al default per un
valore che questa versione non conosce. `ChiaroTheme` prende un parametro `font` e da lì
scendono entrambe le metà della scala: i quindici ruoli Material (`chiaroTypography`) e i due
che Material non ha (`LocalChiaroType`, come `LocalChiaroColors`). Quei due erano `val` di
primo livello che nominavano `InterFamily`: costruiti all'init della classe, avrebbero
continuato a stampare Inter sotto un lettore che aveva chiesto il sistema — è la rottura che il
test copre per seconda. Le tre activity che montano il tema (`MainActivity` e le due di
configurazione dei widget, che sono schermate dell'app) passano la scelta.

La riga sta in Aspetto, subito dopo Palette, perché è la stessa domanda dell'abito. Il foglio
di scelta porta la spiegazione: che Inter viene con l'app ed è uguale su ogni telefono, che «di
sistema» è il carattere del telefono ed è anche quello con cui sono scritte le card.

Un dettaglio che non è cosmetico: **la riga dei crediti non può dire Inter a chi non lo sta
leggendo.** Inter resta incluso comunque (è il predefinito ed è il fallback), quindi
l'attribuzione resta; a chi ha scelto il sistema la riga aggiunge «incluso, ma non in uso». È
§1.1 applicata ai crediti, la stessa lezione della nota della palette.

### Come è stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verdi, **826 test**
(cinque nuovi) e lint a zero errori. I cinque sono `TypographyFamilyTest`; `SettingsStoreTest`
guadagna tre verifiche dentro i test che già aveva (il default, il giro completo, il valore
sconosciuto). Il test dei ruoli li chiede a Material per
riflessione invece di elencarli: un ruolo aggiunto da una versione futura di Material 3 e non
copiato fallisce lì, invece di stampare una riga dell'app in un secondo carattere senza che
nessuno se ne accorga.

**Quel che resta da guardare sul dispositivo**, perché una JVM non lo può dire: se il carattere
di sistema del telefono porta le cifre tabulari (le colonne del Diario e della settimana sono
il posto dove si vede), e se il 300 dell'eroe è disegnato o sintetizzato.

---

## Il secondo carattere: Google Sans, e la temperatura in grassetto (committente, 20 set 2026)

«Invece di avere un font di sistema che cambia di marca in marca forse meglio provare un
font fisso oltre Inter. Cosa ne dici di provare Google Sans? Dovrebbe essere free e con
licenza valida per utilizzo, controlla. […] Possiamo anche provare a mettere in un bel bold
la temperatura attuale dandogli un bel look.»

### La licenza, controllata alla fonte e non sulla scheda

Google Sans sta in `google/fonts` nella cartella **`ofl/googlesans`**: `license: "OFL"`,
`OFL.txt` con «Copyright 2025 The Google Sans Project Authors», e **nessun Reserved Font
Name dichiarato** nella riga di copyright. Quindi si puo' impacchettare, ridurre e
ridistribuire dentro un'app GPL tenendo la licenza accanto — che e' quel che il repo fa
gia' per Inter. Il testo e' in `licenses/GoogleSans-OFL.txt` e la riga in
`licenses/README.md`.

Va detto anche quel che la licenza **non** dice: Google Sans e' il carattere con cui sono
scritti Android e le app di Google, e un'app di terzi che lo indossa si fa somigliare a
loro. E' legittimo e non e' un problema legale; e' una scelta di prodotto, ed e' del
committente.

### Cinque mega non entrano in un APK

Il file a monte pesa **4 974 940 byte** — 8 311 glifi e una ventina di scritture che questa
app non stampa. `tools/import_google_sans.py` lo riduce a **306 664 byte**, cioe' **un terzo
di Inter** (880 KB, che e' il file come lo pubblica il suo autore). Come per i disegni del
meteo, il file sotto `res/font/` non si modifica a mano: rifare girare lo strumento E'
l'importazione, e `--controlla` rifa' la riduzione e confronta i byte (per questo lo
strumento scrive `recalcTimestamp=False`: senza, due esecuzioni identiche darebbero due file
diversi per via dell'ora dentro `head`).

Cosa viene fissato e perche' sta nell'intestazione dello strumento. Le tre cose da ricordare
qui:

- **`GRAD` a 0 e `opsz` a 18.** Il primo e' un asse di compensazione ottica che l'app non ha
  un posto dove decidere; il secondo, in questo file, va da 17 a 18: un punto.
- **`wght` resta variabile, ma parte da 400.** In Google Sans **il 300 non esiste**. Inter
  arriva a 100, questo no, quindi la famiglia dichiara quattro pesi invece di cinque e chi
  chiede Light prende il 400. Non si dichiara un peso che il file non sa disegnare: sarebbe
  la sbavatura per cui Inter era stato impacchettato variabile.
- **`tnum` c'e'**, verificato sul file e non sulla scheda del sito. Era la condizione: DESIGN
  §5 dice che ogni cifra in colonna e' tabulare, e una famiglia senza cifre tabulari fa
  ballare il Diario e la settimana **senza dare un errore**. Se non l'avesse avuto, Google
  Sans non sarebbe entrato.

Le scritture tenute sono quelle che Inter porta gia' (latino esteso, greco, cirillico): i
nomi dei luoghi arrivano dal geocoder e non sono tutti italiani. Quel che resta fuori cade
sul carattere di sistema glifo per glifo, che e' come Android gestisce da sempre un buco.

### Il grassetto, che e' il ribaltamento di una regola scritta

`heroTemperature` passa da **300 a 700**, e la regola vecchia va riscritta invece che
aggirata. Diceva: «un numero in un peso da testo si legge come un titolo, non come una
lettura». A 24 sp regge ancora, e infatti `readingValue` **non si tocca**. A 64 sp no: quel
numero non e' una lettura fra le altre, e' la cosa per cui la schermata esiste, e un filo
d'inchiostro steso sopra un cielo dipinto si legge come ornamento. La card sulla schermata
principale lo stampa in Bold dal giorno in cui e' nata: l'app che dava un peso diverso allo
stesso numero era l'osservazione da cui e' partito tutto.

Grassetto e basta sarebbe stato meta' del lavoro. A quella misura la spaziatura predefinita
e' disegnata per un paragrafo, e le cifre si mettono in mezzo fra loro: **−0,02 em** (−1,28
sp a 64) e' quel che rimette il peso dentro un numero. La formula di Inter si assesta
intorno a −0,022 a questa taglia; ci si ferma appena prima perche' lo stesso numero deve
stare anche in Google Sans, che e' piu' tondo e si chiude prima. **Da guardare sul
dispositivo**: se a 64 sp il numero sembra ancora largo, il passo successivo e' −0,022, non
un'altra taglia.

### Le tre risposte, e perche' «di sistema» resta

Inter (predefinito), Google Sans, di sistema. La terza si poteva togliere — il committente
l'aveva messa in discussione — e si tiene per una ragione sola: **e' l'unica che fa leggere
l'app esattamente come le card**, che e' la domanda da cui e' nata la settimana. Le altre due
sono lo stesso disegno su ogni telefono, che e' la ragione per cui una delle due e' il
default e la terza non lo sara' mai.

La riga dei crediti adesso nomina **tutti e due** i font inclusi, sempre, perche' tutti e due
viaggiano nell'APK qualunque cosa dica l'impostazione, e poi dice quale dei due e' sullo
schermo. Il tocco porta alla pagina del font in uso, o — quando il carattere e' quello del
telefono e non c'e' nessuno da accreditare — alla licenza che i due inclusi condividono.

### Come e' stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verdi, **832 test**
(undici fra i due file di tipografia, sei nuovi in questo giro) e lint a zero errori.

`FontAssetTest` e' il test che conta, e legge i **file**, non il codice: si apre il `.ttf`,
si cammina la tabella delle tabelle e si chiede all'asse `wght` se copre davvero ogni peso
che la famiglia dichiara, alla `GSUB` se `tnum` c'e', e al file quanto pesa. Niente nella
catena di build lo controlla: chiedere un peso fuori dall'asse non e' un errore, e' un
rendering diverso, ed e' il modo in cui un difetto di tipografia arriva a chi legge senza
che nessuno se ne accorga.

### Il predefinito passa a Google Sans, e una promessa da correggere (committente, dal dispositivo)

«Fai Google Sans il predefinito. Va benissimo, vedi screenshot.»

Fatto, e il costo va scritto perche' e' reale: **l'argomento che Inter aveva era la misura.**
Le colonne fisse di `TextScale.kt` — i 44/36/34/34 della riga della settimana, la cella
dell'ora, l'orologio della timeline — sono larghezze di Inter. Google Sans e' un filo piu'
largo e piu' tondo, quindi quelle colonne incontrano il loro punto di riflusso un passo prima
di quanto dica il commento accanto. Non taglia niente (in `ui/` non c'e' un `maxLines`), ma i
numeri da rimisurare, quando qualcuno lo fara', sono quelli. L'argomento che ha vinto e'
quello che ha deciso ogni altro default di questa app: che cosa si vede quando la si apre.

Un'installazione che non ha mai aperto l'impostazione cambia carattere all'aggiornamento. E'
quel che e' un default, ed e' esattamente la stessa riga scritta il 6 set per le icone a
tratto: spostarlo per quei lettori e' il punto del cambio.

### «Se seleziono il font di sistema i numeri non sembrano uguali a quelli del widget»

Domanda giusta, e la risposta e' **no, non e' garantito che coincidano** — la promessa era
mia e la stringa la faceva a schermo, quindi la stringa e' stata corretta (§1.1: lo schermo
non mente, e questa era una frase che prometteva una cosa che il dispositivo ha smentito).

Tre meccanismi, indipendenti, e bastano il primo o il secondo da soli:

1. **La card non la disegna l'app.** Le `RemoteViews` di un widget vengono gonfiate dal
   **launcher**, nel suo processo e sotto il suo tema; `FontFamily.Default` invece risolve il
   typeface predefinito **dentro il processo dell'app**. Su molte ROM il carattere
   dell'interfaccia di sistema e quello che ricevono le app non sono lo stesso file, e in quel
   caso nessuna impostazione dell'app puo' farli coincidere.
2. **L'app chiede le cifre tabulari, la card non puo' chiedere niente.** `tnum` seleziona una
   serie di cifre diversa da quella predefinita, e in molte famiglie la cifra che cambia di
   piu' e' proprio l'1. Stesso font, due disegni della stessa cifra: e' il meccanismo che
   spiega perche' a non somigliarsi siano **i numeri** e non le lettere.
3. **Il peso e la spaziatura.** L'eroe dell'app e' Bold con −0,02 em; la card e' Bold senza
   spaziatura, ed e' un altro corpo. Non cambia il disegno del glifo, cambia come si legge.

**Come distinguere 1 da 2 in dieci secondi, sul telefono**: con «di sistema» attivo, confronta
le **lettere** invece delle cifre — il nome del luogo nell'intestazione dell'app e quello
sulla card. Se le lettere coincidono e solo le cifre no, e' `tnum` (caso 2) e si risolve
togliendo le tabulari all'eroe, che e' un numero solo e non una colonna. Se non coincidono
nemmeno le lettere, sono due font diversi (caso 1) e non c'e' niente da togliere: e' il
sistema operativo.

Non e' stato toccato niente su questo se non la stringa: quale dei due casi sia, lo dice il
dispositivo, e il rimedio del caso 2 (l'eroe senza `tnum`) ha un costo suo — l'eroe e' il
numero che si aggiorna sul posto, e senza cifre tabulari oscilla quando 19,4 diventa 19,5.

### Come e' stato verificato (secondo giro)

`./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verdi, 832 test,
lint a zero errori. Il giro di `SettingsStoreTest` e' stato cambiato apposta: con Google Sans
come default, un test che salvava Google Sans e lo rileggeva non provava piu' niente, quindi
ora salva Inter.

## Il riepilogo della sera, e una revisione di tutti i messaggi (committente, 21 set 2026)

«Vorrei aggiungere un nuovo avviso dal nome simile a "Il riepilogo della sera", gemello di
"Il riepilogo del mattino". Cosa ne pensi onestamente? Deve dare info sulla serata/notte e
una panoramica su domani. Meglio spezzarlo in due avvisi? È un'aggiunta che serve davvero?»

### La risposta onesta, prima del codice

**Serve, ma non per la ragione della domanda.** Le due metà dell'idea non valgono uguale.

La panoramica su domani è **l'avviso con l'argomento più forte di tutta la sezione Pronti**,
più forte del riepilogo del mattino che già c'è. Alle otto del mattino «oggi massima 18°»
arriva mentre la giornata è già cominciata: le decisioni che quel numero governa (come
vestirsi, se portare l'ombrello, a che ora suona la sveglia) sono già state prese. Alle otto
di sera le stesse decisioni sono **ancora tutte da prendere**, ed è l'unico momento della
giornata in cui un'app meteo può cambiarne una senza che il lettore la apra.

La serata e la notte, da sole, sono l'esatto contrario: **non valgono un avviso.** Alle 21 la
serata è fuori dalla finestra, e la notte è un dato con un solo consumatore vero — quanto
scende, perché decide il ghiaccio sul parabrezza, le piante sul balcone e la finestra aperta.
Un avviso solo per quello sarebbe una notifica al giorno per dire una cosa sola, e il gelo ha
già la sua strada in questa app: «Ghiaccio domattina» è uno dei cinque modelli di regola.

Quindi: **un avviso solo, non due**, e le ragioni sono scritte perché sono la decisione.

1. Due notifiche nella stessa ora sono esattamente il rumore contro cui è scritta ogni altra
   regola di questa sezione («al massimo uno per perturbazione», «al massimo due volte al
   giorno»). Spezzare in due significherebbe che chi li accende entrambi ne riceve due a
   venti minuti di distanza, sullo stesso luogo, con metà del contenuto in comune.
2. **La notte e domani sono una decisione sola presa in un momento solo.** Non si decide la
   notte e poi separatamente domani: si guarda fuori una volta e si decide la sera.
3. La sezione Pronti passerebbe da quattro interruttori a cinque, due dei quali quasi
   identici a leggersi. Una lista di interruttori smette di essere scorribile molto prima di
   quanto si creda.
4. Le due metà hanno **pesi diversi**, e un avviso solo lo può dire: domani sta nella riga
   chiusa, la notte sta sotto, dove va a finire quel che non decide da solo se aprire la
   notifica. Due avvisi pari le dichiarerebbero equivalenti, che non sono.

### Il nome

Resta **«Il riepilogo della sera»**, gemello di «Il riepilogo del mattino», anche se il suo
argomento è domani e non la sera. L'alternativa onesta era «Come sarà domani», più informativa
e più precisa — ed è stata scartata: nella lista i due interruttori si leggono uno sotto
l'altro, e la coppia *è* l'informazione. Chi ha appena letto «tra le 6 e le 12» capisce al
volo cosa sia «tra le 18 e le 23»; «Come sarà domani» sarebbe una terza cosa da imparare, e
nasconderebbe la notte che l'avviso porta davvero. La descrizione dice la verità intera:
«Una volta al giorno, tra le 18 e le 23: il cielo di domani, minima, massima e pioggia.
Aprendolo, anche la notte che c'è in mezzo.»

### Cosa mostra, e da dove viene

La riga chiusa è **la frase del gemello, identica**: `Coperto. Minima −1°, massima 9°,
pioggia 80%.` A separarli c'è il titolo, «Domani · Milano» contro «Oggi · Milano», ed è
l'unica cosa che deve separarli. Il giorno si dice una volta, dove va detto.

Aperta, cinque righe possibili, per valore decrescente, ognuna disegnata solo se ha il suo
dato (§1.1 — mai un trattino al posto di un valore, mai uno zero inventato):

| riga | quando compare | perché è lì |
| --- | --- | --- |
| `Stanotte fino a −1° verso le 03:00 · Gelo: ghiaccio sui vetri al mattino` | sempre, se il report ha ore dentro la notte | è l'unica temperatura di cui l'app parla prima che qualcuno ci stia dentro, e la fascia è la decisione, non il termometro |
| `Pioggia stanotte fino al 60%, il peggio verso le 03:00` | picco ≥ 50% | sotto metà la riga direbbe «probabilmente no», che non è una notizia |
| `Domani pioggia dalle 14:00 alle 16:00, fino al 80%` | domani tocca la soglia ombrello (70%) | è la decisione dell'ombrello, cioè il motivo per cui si legge un riepilogo la sera |
| `Domani alba 07:58, tramonto 17:07 · 1 minuto di luce in più di oggi` | alba e tramonto esistono entrambi | è l'edizione della luce del giorno: nessun'altra app stampa quel delta |
| `Domani UV massimo 5 · Scotta in circa 45 minuti` | UV ≥ 3 | alle 21 un «nessuna protezione necessaria» non risponde a nessuna domanda |

Tre cose vanno messe a verbale perché sono deviazioni consapevoli.

**Nessuna soglia nuova per la pioggia.** L'app ne ha due — 70% (l'ombrello, `AlertEngine`) e
50% (il «possibile», `HeadlineEngine.CLEAR_BELOW_PCT`) — e questo avviso usa quelle. La
seconda è diventata `internal` apposta: una terza soglia sarebbe una terza opinione della
stessa app sullo stesso cielo.

**La finestra della pioggia di domani è calcolata sulle sole ore di domani.** Una pioggia che
comincia alle 23 di stasera è la pioggia di stasera, e la riga che la stampa dice «domani».
Una che scavalca la mezzanotte successiva torna quindi aperta («dalle 21:00 in poi»), che è
quel che dicono i dati quando l'unità è il giorno.

**L'UV è condizionale qui e incondizionato nel gemello del mattino.** È una divergenza fra i
due, voluta: `WeatherText`'s KDoc dice che «non c'è niente da fare» è anch'essa una risposta,
ed è vera alle 8, quando il lettore ha in mano la domanda. Alle 21 nessuno la sta facendo.

**La finestra 18–23** ha un vincolo duro dentro: il tetto delle 23 è quel che tiene onesta la
parola «domani». Passata la mezzanotte «domani» è il giorno dopo quello per cui i numeri sono
stati letti, e un riepilogo che arriva alle 00:10 sbaglia di un giorno. Cinque ore di
larghezza perché il job periodico ci cade dentro anche all'intervallo più lento consentito
dalle impostazioni (120 minuti).

**Non parte se il report non ha domani.** Non è una guardia difensiva: una cache può
sopravvivere alla propria settimana, e un riepilogo la cui frase chiusa sarebbe fatta tutta
di trattini è lo schermo che mente nell'unico posto in cui il lettore non può verificarlo.

### Quel che è costato altrove

- `AlertState` ha una **casella di dedup separata** per i due riepiloghi. Condividerla
  avrebbe significato un riepilogo al giorno, il primo che arriva: alle 20 quello del mattino
  ha già scritto la data di oggi.
- `Alert.forDate` è nuovo: il notificatore deve sapere di che giorno parla e **non** può
  ricavarlo dall'orologio del dispositivo, che può essere un giorno più in là del calendario
  della città.
- `EveningDetails` è puro come `AlertDetails`, e prende `from` come parametro invece di
  leggere `hours.first().time`: **il report del worker non è tagliato da `WeatherRecency`**
  (il taglio vive nel livello UI, in `TodayUiState`), quindi senza quel parametro la «notte»
  sarebbe partita dall'ora più fredda di stamattina. È il tipo di errore che sarebbe passato
  in produzione dicendo un numero plausibile e sbagliato.
- L'alba e il tramonto di domani li calcola `AstronomyEngine`, non il provider: il blocco
  `astronomical` del report è del giorno del report, e alle 20 l'alba di oggi è l'unico dato
  che non serve a nessuno. Due `sunCrossing` una volta al giorno, e funziona offline.

### La revisione di tutti i messaggi, chiesta insieme

Letti tutti e sei i notificatori, chiuso e aperto. Due difetti veri, entrambi corretti.

**1. L'avviso di maltempo era l'unico senza il punto finale**, e per un motivo strutturale:
era un tronco (`%1$s verso le %2$s`) più un frammento opzionale (`, pioggia al 90%`). Un
frammento incollato in inglese non è un frammento che ogni lingua mette lì, ed è anche quel
che gli toglieva la punteggiatura. Ora è **una frase intera per forma**.

**2. Lo stesso pezzo di codice stampava `time ?: ""` e `precipPct ?: 0`.** Con l'ora assente
usciva «Temporale verso le» — una frase rotta — e con la probabilità assente «pioggia al 0%»,
che è uno zero inventato, cioè esattamente ciò contro cui è scritto §1.1. Irraggiungibile
oggi, perché il motore àncora entrambi gli avvisi su un'ora che ha letto davvero; ora
irraggiungibile anche nel testo, che sceglie una frase che di quel dato non ha bisogno.

Il resto della revisione, per completezza, non ha prodotto cambi:

- **Chiuso ≠ aperto** vale per tutti e sei. Maltempo e pioggia aggiungono la finestra vera,
  l'ora peggiore e l'escursione; il riepilogo del mattino aggiunge i fatti del giorno; le
  regole aggiungono l'aritmetica che le ha fatte scattare; il cielo aggiunge la frase del
  catalogo su cosa sia quel momento; le allerte ufficiali aggiungono i livelli per giorno, la
  zona, il significato e la fonte. `AlertNotifierTest` ora lo **impone** per ogni tipo
  incorporato, così un quinto non può uscire come titolo senza niente sotto.
- Il riepilogo del mattino apre con «Adesso», mentre maltempo e pioggia mettono «Adesso»
  penultimo. È una differenza d'ordine e non un difetto: alle 8 «adesso» è la cosa più
  azionabile che ci sia; sotto una finestra di temporale sarebbe la riga sbagliata in cima.
- Il promemoria del cielo aggiunge **una** riga sola quando lo si apre. È poco, ma è la riga
  giusta: chi si è iscritto all'«ora blu» una volta e non ricorda cosa sia, lì lo ritrova.
- L'avviso pioggia e il riepilogo della sera possono coincidere: alle 20 la finestra a sei ore
  del primo arriva alle 2 di notte, che è la notte di cui parla il secondo. Non si sopprimono
  a vicenda, di proposito — uno è un allarme, l'altro è un riassunto, stanno su due canali e
  due id diversi, e chi vuole solo uno dei due lo spegne.

**Una cosa segnalata e rimandata al committente**: in italiano «fino al 80%» andrebbe elisa
in «fino all'80%». È diventata il lavoro del giorno dopo, qui sotto.

### Come è stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug` verdi, lint a zero errori. Tre file di
test nuovi o cresciuti:

- `AlertEngineTest` — la finestra 18–23 ai bordi, il dedup separato dai due riepiloghi, il
  fatto che il mattino non cada mai nella finestra della sera e viceversa, e che senza domani
  nel report non parta niente.
- `EveningDetailsTest` — la notte è l'ora più fredda fra adesso e l'alba (con la prova che
  l'ora più fredda di *stamattina*, che nel report del worker c'è ancora, non viene contata),
  la finestra di pioggia di domani è di domani sola, l'alba di domani nella zona della città,
  il ripiego alle 6 dentro la notte polare, e il delta di luce che coincide con la sottrazione
  delle due durate.
- `AlertNotifierTest` — **nuovo**, e la parte di questo lavoro che resterà utile più a lungo:
  rende la regola «aperto dice più di chiuso» una cosa che la build controlla, per ogni tipo,
  invece di una cosa scritta in un KDoc.


## L'elisione, e perché non è un helper (committente, 22 set 2026)

«Sistema anche le stringhe che necessitano l'elisione. Applica la soluzione migliore dal punto
di vista del codice. Dove dici che "marcisce" non mi sembra la soluzione migliore, giusto?»

Giusto. E il dubbio era ben riposto: la strada dell'helper è quella sbagliata, ma non per la
ragione che avevo scritto ieri.

### Quanto è grande davvero

Prima di decidere, contate. Non erano tre stringhe né cinque: sono **diciotto**, e stanno in
tre posti diversi dell'app.

- **Dodici con `%d`**, cioè un numero che mette l'app: otto nelle notifiche e **quattro nella
  schermata Cielo**, che ieri non avevo nemmeno guardato («illuminata al 80%», «il 80% della
  Luna nell'ombra»).
- **Due con `%s` che portano una percentuale già formattata**: gli esiti del Diario, «Pioggia
  data al 80%».
- **Quattro con `%s` che portano una data**: «bollettino del 8 set 2026», che vuole «dell'8».
  Questa classe non l'avevo vista affatto, ed è quella che rende il problema una regola e non
  una svista: non riguarda le percentuali, riguarda **qualunque valore che comincia per cifra**.

Dentro 0–100 i numeri che cominciano per vocale sono esattamente 1, 8, 11 e 80–89. Cioè:
«pioggia al %d%%» era giusta nove volte su dieci e sbagliata la decima, in silenzio, in una
notifica che nessuno può correggere. È il difetto peggiore da avere, perché non si manifesta
mai quando lo cerchi.

### Le tre strade, e perché la terza

**1. Una funzione in `Formats` che sa quali numeri elidono.** È quella che ieri ho chiamato
«marcisce», e il committente aveva ragione a non fidarsi della motivazione: «marcisce» non è
un argomento, è un'etichetta. L'argomento vero è che sarebbe **un fatto sulla lingua italiana
scritto in Kotlin**, applicato a — o saltato per — ogni altra lingua in cui l'app finirà. Chi
traduce apre `values-xx/strings.xml`, non `Formats.kt`: la regola sarebbe in un posto in cui
nessuno di quelli che ne hanno bisogno la troverà.

**2. Due varianti per frase, scelte da una lista di numeri elidenti tenuta nelle risorse.**
Sembra la versione buona della 1 — la grammatica torna dentro il file della lingua, e un
traduttore catalano ci mette la sua lista. È stata scartata per una ragione più netta:
comporre una frase da un frammento («al 70%») più il resto **è esattamente la forma da cui
l'avviso di maltempo è stato riscritto via un commit fa**, perché una subordinata incollata in
inglese non è una subordinata che ogni lingua mette lì. Comprare l'elisione con quella forma
sarebbe disfare la decisione di ieri il giorno dopo averla presa. Il costo secondario, 18 × 2
stringhe italiane più altrettante inglesi identiche a coppie per via di `StringsParityTest`,
da solo non avrebbe deciso niente; questo sì.

**3. Scrivere la frase in modo che nessun articolo tocchi il numero.** Scelta. Non ha codice,
**non può essere sbagliata in nessuna lingua**, presente o futura, e non è nemmeno una nuova
idea in questa app: `notif_summary_body` dice «pioggia 80%» dal giorno in cui è stata scritta.
Erano le altre a essere l'eccezione, non questa.

Le uscite usate, tutte e tre invariabili:

- **Via l'articolo** dove la frase regge senza: «pioggia 80%», «Parziale: 80% della Luna».
- **Preposizione scempia**, che non elide mai: «fino a 80%», «coperto per 80%». Questo è il
  punto che rende la soluzione una regola e non un trucco: `a`, `di`, `da`, `in`, `su` non
  chiedono apostrofo davanti a nessun numero, e nemmeno i plurali (`alle 8`, `delle 8`, `dei`).
  Solo il singolare `il/lo/la` e le sue contrazioni elidono.
- **L'articolo agganciato a una parola**, per le date: «bollettino **del giorno** 8 set 2026».
  «del» ora concorda con *giorno*, che non cambia mai. Una sola frase è stata girata invece
  che allungata, perché ci guadagnava: «L'ultimo è del 8 set» è diventata «L'ultimo **risale a**
  8 set».

Il Cielo ha preso la forma che l'inglese aveva già: «%d%% illuminata» contro «%d%% lit»,
invece di «illuminata al %d%%». Due lingue che dicono la stessa cosa nello stesso ordine sono
anche due stringhe che non divergono alla prossima modifica.

### La regola è un test, non una convenzione

`ItalianArticleTest` fallisce la build se una stringa italiana rimette un articolo elidibile
subito prima di un `%d`, negli `<string>` e dentro gli `<item>` dei plurali. **Il test è stato
verificato rompendo la regola apposta** e guardandolo fallire: un test di questo tipo che non
sia stato visto fallire non sorveglia niente.

Ed è il posto giusto per l'unico pezzo di grammatica italiana che resta nel repository: un
test non entra in nessun APK, non raggiunge nessun'altra lingua, e sta dove chi aggiunge una
stringa lo incontra. È la differenza fra sapere l'italiano in produzione — la strada 1 — e
saperlo nella build.

Quel che il test **non** può controllare è scritto nel suo KDoc: un argomento `%s` porta una
stringa che ha formattato qualcun altro, e se cominci per cifra lo decide il chiamante, non la
risorsa. Le sei di quella classe sono corrette a mano e nominate lì, così chi ne aggiunge una
settima legge perché esistono.

### Come è stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verdi, 1509 test,
lint a zero errori. Nessun file Kotlin di produzione toccato: la correzione è diciotto stringhe
e un test, che è esattamente la dimensione che questo problema doveva avere.

---

## Le tre note dallo schermo di casa: il peso del numero (committente, 20 set 2026)

Uno screenshot con due card una sopra l'altra — «In parole» su fondo blu e «Colpo d'occhio»
sul cielo — e tre righe:

> «widget "In parole": la temperatura minima e la relativa freccia sono più piccole della
> temperatura massima. È una cosa voluta? […] widget "Colpo d'occhio" e "Le prossime ore":
> la temperatura sarebbe bello se visualizzata in bold come nel widget "In parole". […] in
> tutti i widget vorrei nelle impostazioni la voce "Un colore".»

### La minima non era più piccola: era più debole, ed è lo stesso difetto

Misurata, la coppia era già alla stessa taglia: stesso `size` per i due numeri, e i due
vettori `ic_range_high`/`ic_range_low` sono la stessa geometria specchiata, 14 unità di
asta su 24 tutt'e due. Quel che cambiava erano **peso e inchiostro**: massima in Medium
sull'inchiostro forte, minima in Regular su quello quieto, e la freccia tinta come il numero
che accompagna. Sul dispositivo quella differenza non si legge come «questa è la secondaria»:
si legge come **un numero più piccolo**, che è precisamente la segnalazione.

Da qui la decisione, che non è «togliere la gerarchia» ma **chiedersi chi la porta**.
`DayRange` ha due abiti e adesso non condividono più un'enfasi:

- **Con le frecce** (il widget testuale, che ha i 174 dp di colonna per portarle) le due metà
  sono vestite uguali: stessa taglia, stesso peso, stesso inchiostro, frecce tinte con lui.
  ↑ e ↓ dicono già quale è quale, quindi l'attenuazione lo diceva una seconda volta e la
  faceva pagare alla minima in presenza. È §2.3 letta a questa scala: **il segno porta il
  significato, il colore al massimo lo asseconda.**
- **Con la barra** (il widget Oggi, la cui colonna da 113 dp le frecce non le prende) non c'è
  nessun segno a portarlo, e lì l'inchiostro resta la cosa che ordina la coppia: massima
  prima e forte, minima dopo e attenuata — l'enfasi delle righe della settimana.

Una sola `DayRange` con un parametro, come prima: è sempre una frase a due budget, e adesso
anche a due grammatiche.

### Il grassetto sale da una card a una regola

`heroTemperature` sull'app era passata in Bold il 20 set; il widget testuale ci stava dal
giorno in cui è nato. Le card «Colpo d'occhio» e «Le prossime ore» stampavano ancora 34 sp
Medium, che è **il peso con cui su quelle card è scritto ogni fatto** — la frase, il luogo,
massima e minima. Accanto al Bold del widget testuale il numero non leggeva come l'eroe della
card: leggeva come un fatto un po' più grande degli altri.

La regola scritta in DESIGN §5 nomina l'**eroe**, non la grandezza: le tre card in cui la
temperatura *è* l'eroe la stampano in Bold, la card dell'arco no, perché lì l'eroe è il
disegno e il numero è una riga della striscia accanto. E le ore in fondo al widget Oggi
restano Regular per la stessa ragione: sette cifre in grassetto sotto un eroe in grassetto
sono due eroi.

**Rimisurato, perché il Bold è più largo del Medium e un budget ci stava appoggiato.**
`TemperatureColumnMin` (66 dp) riserva la colonna del numero quando accanto c'è una frase, sui
~62 dp di «−12°» a 34 sp col carattere di sistema. Sui due file inclusi il Bold costa **+2,3%**
(Google Sans) e **+2,0%** (Inter) sull'avanzamento del Medium per quella stringa: ~1,5 dp, cioè
~63 dp, e la colonna resta con 3 dp di margine. Il numero non si tocca; il commento accanto sì,
perché diceva «Medium».

Le due anteprime del selettore (`widget_now_preview.xml`, `widget_today_preview.xml`) passano
in `textStyle="bold"`: un'anteprima che mostra un peso diverso da quello della card sta
pubblicizzando un prodotto che non esiste.

### «Un colore»: c'era già, e adesso è un test

La terza riga chiedeva una cosa **già vera dal 19 set**: `BackgroundSection` è un composable
solo, `WidgetConfigActivity` (che configura Colpo d'occhio, Le prossime ore, Momenti del cielo
e In parole) e `ArcConfigActivity` lo chiamano tutt'e due senza nessuna condizione sul tipo di
card, e la riga «Un colore» con i suoi sei colori sta lì dentro. Si raggiunge tenendo premuta
la card sulla schermata di casa e toccando l'ingranaggio, poi **Sfondo → Un colore**.

Quel che mancava non era la voce: era qualcosa che tenesse la promessa. Le due liste diventano
dati (`WidgetBackgroundChoices`, `WidgetCardColorChoices`) e `WidgetConfigChoicesTest` controlla
tre cose: che ogni valore di `WidgetBackground` abbia la sua riga e nell'ordine giusto, che ogni
`WidgetCardColor` ce l'abbia, e che le due schermate passino davvero dalla sezione condivisa
invece di stamparsi ciascuna la propria lista — che è esattamente il modo in cui «Un colore»
finirebbe su quattro widget su cinque senza che nessuno se ne accorga.

### Come è stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verdi, **1515 test**
(tre nuovi), lint a zero errori. Le larghezze del Bold contro il Medium sono misurate sui due
`.ttf` inclusi con `fontTools`, non stimate.

*(Il messaggio di commit dice 1512: la variante `release` non aveva ancora rigirato i tre test
nuovi quando è stato contato. Il numero giusto è 1515.)*

---

## Il nome del luogo, e la colonna che se lo prendeva a metà (committente, 20 set 2026)

> «Nello screenshot "Colpo d'occhio" vedi che il nome della località è troncato? Sì che questo
> esempio è lungo, ma si riuscirebbe a visualizzarlo per completo senza penalizzare il testo a
> destra nel caso la frase da visualizzare sia lunga? Modifica solo se non rovina il layout.»

La condizione è la parte interessante, ed è quella che ha deciso la forma della soluzione.

### Che cosa faceva davvero la riga larga

Le due colonne di testo di una card a una riga si dividevano **la metà esatta** della luce che
resta dopo il glifo (`nowSentenceColumnWidth`). Sulla card di riferimento da quattro celle sono
236 dp, cioè 118 per una. «Cavenago di Brianza» col segnaposto è ~167 dp: troncato. E accanto,
nei suoi 118 dp, c'era **«Sereno»**, che ne occupa 47.

Metà era la prima risposta giusta e la ragione regge ancora: è quel che fa il widget di
riferimento, il suo blocco di descrizione è largo quanto il suo blocco del numero, e lo spazio
vuoto cade in mezzo dove l'occhio se lo aspetta. Quel che metà non sa fare è **accorgersi che
una delle due colonne non è piena**.

### Perché non è una quota fissa più grande

La soluzione ovvia — dare alle parole 140 dp invece di 118 — è quella che il committente ha
escluso nella stessa frase: quei 22 dp li pagherebbe **ogni** frase lunga, anche sulle card
dove il nome del luogo è «Rho». E non basterebbe comunque: 140 − 20 di segnaposto = 120 dp di
nome, e «Cavenago di Brianza» ne vuole 147.

Quindi la domanda va girata: non «quanto do alle parole», ma **quanto chiedono i due blocchi**.

### Misurare, non stimare

Glance non sa misurare il testo, ed è la ragione per cui questa riga si divideva a metà. Ma il
testo si può misurare **dove si può**: `measureWidgetText` apre un `Paint` in questo processo,
alla taglia e al peso che il `Text` avrà, e chiede la larghezza. La faccia è
`Typeface.DEFAULT` — **il carattere di sistema, che è esattamente quello con cui una card viene
disegnata**: un widget lo gonfia il launcher da `RemoteViews` e non vede mai il font incluso
nell'APK, che è tutto il motivo per cui in Impostazioni «il carattere del telefono» è la scelta
che avvicina di più l'app alle sue card. Non è una stima della larghezza: è la larghezza, nello
stesso font e alla stessa taglia.

Quel che non può promettere sta nel suo KDoc: su un telefono la cui interfaccia di sistema gira
una faccia diversa da quella che ricevono le app, il launcher misura qualche punto percentuale
diverso da noi. Per questo ogni chiamante tiene `RowFitSlack` (4 dp) e **niente qui è un
vincolo**: un nome che viene fuori più largo di quel che abbiamo misurato va in ellissi
esattamente come faceva prima, che è lo stato da cui siamo partiti.

Una trappola trovata dai test e vale la pena scriverla: il `Typeface` era un `val` di primo
livello, e i test puri del layout leggono altre proprietà di primo livello dello stesso file —
l'inizializzatore della classe partiva sotto JUnit e `Typeface.create` sull'`android.jar`
stubbato esplode. È `by lazy`: niente si costruisce finché qualcosa non misura davvero.

### Le tre regole, e l'ordine è la promessa

`nowWordsColumnWidth` è aritmetica pura (quindi `NowWidgetLayoutTest` la fissa a tavolino):

1. **La frase tiene quel che chiede, e mai meno di quel che le dava la metà.** Quel che chiede è
   la sua larghezza misurata su **una riga**, tagliata alla metà: così una frase lunga non può
   essere stretta, viene tagliata prima di poterlo essere, e la card ricade esattamente sul
   layout che ha oggi. È la condizione del committente, scritta come un `coerceAtMost`.
2. **Le parole prendono quel che serve alla riga del luogo**, mai meno di `WordsColumnMin`,
   perché in quella colonna ci vive anche la temperatura e troncare un numero è un guasto
   peggiore che troncare un nome.
3. **Le parole non portano mai la frase sotto il suo pavimento**: quel che resta dopo la 1.

Il risultato non è mai più piccolo della metà, quindi **nessuna card perde un dp di quelli che
ha oggi**; l'unica cosa che si muove è lo spazio che la frase teneva senza usarlo.

Simulato sulle facce incluse (che sono più larghe del Roboto con cui il launcher disegna
davvero, quindi è il caso peggiore), card da quattro celle:

| luogo | frase | parole | frase | esito |
|---|---|---:|---:|---|
| Cavenago di Brianza | Sereno | 171 | 65 | nome **intero**, frase su una riga |
| Cavenago di Brianza | Poco nuvoloso | 123 | 113 | nome quasi intero, frase su una riga |
| Cavenago di Brianza | Pioggia gelata verso le 15:00 | 118 | 118 | **identico a oggi** |
| Milano | Pioggia in arrivo verso le cinque. | 118 | 118 | **identico a oggi** |

Con la pastiglia dell'allerta nella colonna di destra non si misura niente e si torna alla
metà: una pastiglia è un blocco di inchiostro fisso che non sa andare a capo né in ellissi, e
il giorno di un'allerta non è il giorno per cercare il bordo di un'aritmetica.

### La regressione che ① si portava dietro, trovata rileggendo il proprio diff

Da quando la finestra di buio è la notte meno la luna, è `∅` ogni notte in cui la luna è su dal
crepuscolo all'alba — e quelle notti vengono **in fila**. `SkyReminderPlanner` chiedeva
`next(limit = 3)` e filtrava gli `At`: tre `∅` di fila e il promemoria del buio spariva senza dire
niente. Misurato sul 2026, corsa più lunga di notti piene: **Milano 9, Palermo 6, Copenaghen 10,
Edimburgo 11**. Un promemoria muto per una settimana e mezza sarebbe stato un guasto peggiore di
quello che questa fase ripara.

La correzione non alza e basta la costante: cerca **prima a tre e poi a sedici**, così la camminata
profonda si paga solo quando quella corta non ha trovato niente — che è il caso della finestra di
buio e di nient'altro. Alzarla per tutti avrebbe voluto dire sedici giorni di almanacco per ogni
sottoscrizione a ogni re-arm, per un caso su cinquantuno.

Sedici copre la corsa peggiore misurata più un preavviso da un giorno, con un margine che non è
una coincidenza da rimisurare ogni anno.

### Quel che NON è stato toccato

- **La disposizione a glifo destro** (`MirroredRowContent`), che il committente dice già legge
  il nome per intero: lì la colonna delle parole è tutta la riga meno il glifo, e non c'è
  niente da dividere.
- **Il widget «Le prossime ore»**, che ha la stessa divisione a metà nella sua riga dell'eroe.
  Non era nella richiesta e la sua colonna di destra porta anche massima/minima, quindi «quel
  che la frase chiede» lì è una domanda con due risposte. Si estende quando lo si chiede.
- **Le anteprime del selettore**, che restano corrette: il loro luogo di esempio è «Milano» e la
  loro frase è lunga, cioè esattamente il caso in cui la nuova regola ricade sulla metà.

### Come è stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verdi, **1523 test**
(quattro nuovi in questo giro, sette in tutto sul ramo), lint a zero errori. Le larghezze della
tabella qui sopra sono calcolate dai due `.ttf` inclusi con `fontTools`, non stimate a occhio.

---

## La freccia era davvero più piccola, e non era l'inchiostro (committente, 20 set 2026)

> «Sul widget "In parole" la freccia temperatura minima è ancora piccola e anche il testo mi
> sembra più piccolo: mi sono perso qualcosa?»

No: si era perso qualcosa chi ha guardato. Il giro precedente aveva trovato una differenza
vera — massima in Medium sull'inchiostro forte, minima in Regular su quello quieto — l'ha
corretta, e si è fermato lì perché la geometria *sul file* era identica: `ic_range_high` e
`ic_range_low` sono la stessa asta da 14 unità su 24, specchiata. Misurare il disegno e non
quel che arriva sullo schermo è esattamente l'errore che quella segnalazione ha scoperto.

### Che cosa succedeva davvero

`padding` di Glance è `RemoteViews.setViewPadding` **sulla stessa view** su cui atterra la
misura (verificato nel bytecode di `ApplyModifiersKt`), e un `Image` scala il disegno per
entrare in quel che il padding lascia (`ContentScale.Fit`, il default — verificato in
`ImageKt`). Quindi:

```kotlin
GlanceModifier.padding(start = 8.dp, end = 2.dp).size(16.dp)
```

non è un segno da 16 dp con dell'aria intorno: è **un segno da 6 dp**. `RangeMark` chiedeva
proprio quello, e l'aria fra le due metà della coppia — gli 8 dp di `RangeMarkGap` — la
pagava il segno della minima, di tasca sua. Risultato: freccia in giù disegnata a 6 dp,
freccia in su (che paga solo i 2 dp di coda) a 14. **Il 43% dell'inchiostro, alla stessa
misura nominale.** Il committente lo ha visto due volte e aveva ragione due volte.

Il codice attorno lo sapeva già e lo dice a parole sue in due punti: il segnaposto di
`PlaceLine` si distanzia con uno `Spacer`, e `WarningChipRow` ha il commento «il padding sta
sulla scatola e non sulla pastiglia, perché il padding di una pastiglia sta DENTRO il suo
sfondo, e un'aria colorata non è un'aria». `RangeMark` era l'unico posto che non lo seguiva,
e il commento accanto spiegava perché: evitare uno `Spacer` per tenere la riga a quattro
figli. Adesso l'aria sta su un `Box` che avvolge il segno — quattro figli lo stesso, e il
disegno alla misura che chiede.

La coppia con le frecce cresce di 12 dp (da ~75 a ~87 a 16 sp, ~100 sulla coppia più larga
della scala): la colonna del widget testuale che la porta ne ha 132, quindi restano 30 dp di
margine anche nel caso peggiore.

**`WidgetGlyphBoxTest`** legge i sorgenti e fallisce se una catena di `GlanceModifier` mette
insieme `.size(` e `.padding(`. `width` e `height` non ci sono apposta: un contenitore a
larghezza fissa con del padding è un'altra cosa ed è giusta — il padding di un layout lo
pagano i suoi figli, non una bitmap. **Verificato rompendolo**: rimettendo il padding
sull'`Image` di `RangeMark` il test fallisce e nomina il file e la riga.

### E «anche il testo mi sembra più piccolo»

Quello era il giro precedente e ora è a posto: con le frecce le due metà sono vestite uguali,
stessa taglia, stesso peso, stesso inchiostro. Se lo screenshot è di una build anteriore a
`4dd8221` la differenza c'è ancora; se è posteriore, resta solo la freccia, che è questo giro.

## Le due colonne dell'eroe, anche su «Le prossime ore»

> «Sì, fallo anche su "Le prossime ore".»

La grammatica era scritta due volte — `todayIsWide` si ricalcolava in casa la stessa divisione
a metà di `nowSentenceColumnWidth` — e una grammatica scritta due volte è una grammatica che
diverge. Adesso c'è una sola aritmetica, `heroRowWordsWidth` / `heroRowEvenColumn` /
`heroWordsColumnWidth`, e le due card la chiamano.

Le tre regole e il loro ordine sono quelle del giro precedente. Quel che cambia è **che cosa
deve coprire «quel che la colonna di destra chiede»**: sulla card «Colpo d'occhio» quella
colonna tiene della prosa, che va a capo; qui può tenere anche massima e minima del giorno,
che **non** vanno a capo — una coppia più stretta di quel che misura è una coppia con una
cifra tagliata via. Quindi si misura il più largo dei due inquilini invece di riservare per
entrambi, e con la pastiglia dell'allerta non si misura niente e resta la metà.

### Come è stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verdi, **1529 test**
(sei nuovi in questo giro, tredici sul ramo), lint a zero errori.

## La review delle chiamate a Open-Meteo: l'offset fisso, e tre bugie minori (committente, 20 set 2026)

> «Review accurata delle chiamate alle API di OpenMeteo e all'interpretazione/visualizzazione
> dei dati. Dati chiamata, UTC località passata, interpretazione dati (weathercode, ecc.),
> logica di visualizzazione.»

La review è stata fatta **contro l'API vera**, non solo leggendo il codice: gli stessi
identici parametri che manda l'app, su 15 località (Tokyo, Svalbard, McMurdo, medio Pacifico,
Everest, Reykjavík, Nuuk, Ushuaia, Malé, Lhasa, Jakutsk, Sydney, Lagos, Quito, Milano). Le 13
che hanno risposto danno tutte 168 ore, 7 giorni, **tutti i campi presenti**, e unità conformi
a quel che i DTO assumono (`°C`, `km/h`, `hPa`, `mm`, `m` per la visibilità, `%`,
`grains/m³`, `µg/m³` — la conversione CO in mg/m³ è giusta, verificata su `current_units`).
La forma della richiesta è sana. Quel che non lo era è l'assunzione sotto i timestamp.

### 1. `utc_offset_seconds`: Open-Meteo non manda l'ora di parete

`ForecastResponseDto` leggeva `timezone` e ignorava `utc_offset_seconds`; il mapper faceva
`LocalDateTime.parse` e tutti i lettori interpretavano quei valori come ora locale nelle
regole di `ZoneId.of(timezone)`. **È falso.** Con `timezone=auto` il provider prende l'offset
in vigore al momento della richiesta e ci costruisce sopra tutta la settimana: la serie è
`UTC + quell'offset`, costante.

Misurato il 20 set 2026 su una previsione a 16 giorni di Sydney, che attraversa il passaggio
all'ora legale del 4 ottobre:

- la risposta porta `utc_offset_seconds: 36000` e `timezone_abbreviation: "GMT+10"` per
  **tutti** i giorni, anche quelli in AEDT;
- ogni giorno di calendario ha esattamente 24 valori, `2026-10-04T02:00` compreso — un'ora
  che in `Australia/Sydney` non esiste;
- confronto ora per ora con la stessa richiesta su `timezone=UTC`: **0 discrepanze su 384**
  con offset fisso `+10`, **53 su 69** leggendo le etichette come ora di parete vera;
- il `daily.sunrise` del 4 ottobre dichiara `05:28`, mentre l'orologio a Sydney dice `06:28`.

Conseguenza per Chiaro: nei ~7 giorni in cui l'orizzonte di un fetch scavalca un cambio d'ora
(in Europa dal 18 al 25 ottobre, e a marzo) **ogni riga successiva alla transizione stava
un'ora indietro**. La temperatura sotto «08:00» era quella delle 07:00; `stripHour` decideva
giorno/notte dall'etichetta invece che dal dato; `SkyVerdictEngine.window()` agganciava un
picco di meteore delle 02:00 alle nuvole delle 03:00 — proprio i verdetti a 3-7 giorni della
Fase 16a; `WeatherRecency.trim` teneva un'ora già passata. L'alba dell'app era giusta in tutto
questo, perché `AstronomyEngine` le ore del provider non le ha mai viste: è quello che rendeva
il disaccordo **visibile sullo schermo** invece che soltanto sbagliato sotto.

Nessuna traccia di `utc_offset_seconds` o dell'ora legale in `PLANNING.md` o `UPSTREAM.md`
prima d'ora, e nessun test del mapper lo copriva: era un punto cieco, non una deviazione
registrata.

**La correzione.** Il campo entra nel DTO nullable e con default — `ReportDiskCache` serializza
il DTO grezzo, quindi una voce scritta prima deve continuare a decodificare, e un null si
risponde col comportamento vecchio e non con una stima. Nel mapper c'è `ProviderClock`, che
tiene le due cornici separate e dice a parole quale vale dove:

- **dentro** `map()` i confronti fra valori della risposta restano nel frame del provider,
  dove condividono un offset solo e l'aritmetica è esatta (`currentHourIndex` e il
  raggruppamento di `mapDaily`);
- **in uscita** ogni valore è ri-espresso sull'orologio della città.

`mapDaily` raggruppa apposta sulle etichette del provider e non sull'ora di parete: una riga
di `daily` è l'aggregato del provider sul **suo** giorno, e `dailyCode` ri-deriva l'etichetta
dalle ore che quell'aggregato ha visto. Raggruppare sull'orologio della città regalerebbe al
giorno una venticinquesima ora che il suo massimo e il suo minimo non hanno mai visto. Quel
che resta dell'offset fisso è quindi **un'ora a ciascun capo dei due giorni l'anno** in cui
una zona cambia: la finestra del provider, che è quel che `daily.weather_code` avrebbe detto
comunque, e due ordini di grandezza meno dell'errore che sostituisce.

### `HourlyForecast.at`, e l'insidia che ha aperto

Ri-esprimere le ore ha una conseguenza che va detta prima di leggerla in un crash report: **il
giorno in cui una zona torna all'ora solare, le 02:00 ci sono davvero due volte**. Fino a ieri
non succedeva, ed è l'offset fisso che lo impediva. `HourStrip` chiavava le celle con
`hour.time.toString()`, e una `LazyRow` con due chiavi uguali non disegna una riga doppia:
**lancia**. La striscia e la riga espansa della settimana sarebbero andate in crash il 25
ottobre.

Quindi `HourlyForecast` guadagna `at: Instant` — l'identità, dove `time` è soltanto
l'etichetta — e la chiave della striscia è quella. Il campo si ripaga subito: quattro lettori
ri-derivavano l'istante a mano con `time.atZone(zone).toInstant()`, che su un'ora ambigua deve
*scegliere* uno dei due offset. Adesso lo leggono: il flag giorno/notte della striscia, la
serie del widget arco, e i due capi di `RainbowWindow`. Quest'ultima perde il parametro `zone`,
che non le serve più — ed è la versione onesta comunque: la geometria del sole è un fatto su un
momento e un luogo, mai su quale calendario il momento è scritto.

Quattro test nuovi sul mapper, e **verificati rompendo il codice**: con `localOf` rimessa a
identità, `hours after a spring forward land on the city's clock` e `the day a zone falls back
holds one label twice and two moments` falliscono entrambi.

### 2. Il tile UV diceva il picco del giorno con la frase del momento

`Details` stampava `uv_index_max` — il **massimo** della giornata — sotto l'etichetta secca
«UV» e sotto una riga di significato scritta per il presente. Alle 23:00 di un giorno di luglio
si leggeva «UV 8 — Scotta in circa 25 minuti, copriti»: un consiglio su un sole tramontato da
quattro ore, in una griglia dove tutto il resto (vento, umidità, pressione, visibilità, aria) è
un'osservazione di adesso. Il KDoc di `DailyForecast.uvIndexMax` difende il picco «sotto
un'intestazione Oggi», e fa bene; questa griglia non è quell'intestazione.

Il valore è adesso `current.uvIndex` — preso dall'API dal primo commit e **renderizzato da
nessuna parte** fino a oggi — e il picco resta come `note`, come il tile del vento porta le sue
raffiche, solo finché è ancora notizia (`peak > now`: un picco uguale alla lettura è il tile che
scrive due volte lo stesso numero). Nuova stringa `uv_peak_today`.

Il primo tentativo nascondeva il tile a sole sotto l'orizzonte. È stato scartato rileggendo
DESIGN §1.2: *«una metrica il cui valore non ha conseguenze oggi riceve lo stesso la riga della
sua banda — "non c'è niente da fare" è anch'essa una risposta»*. «UV 0, nessuna protezione
necessaria» a mezzanotte **è** quella risposta; la vecchia riga era una risposta diversa, su
un'ora diversa. Il tile resta sempre.

### 3. La cella oraria annunciava «pioggia 0%» dove non stampa niente

In `toCell` il commento diceva la cosa giusta — *«letto come 0 solo quando la previsione dice
0»* — e il codice faceva l'opposto: `hour.precipChancePct ?: 0` dentro `hour_cell_desc`, che
non ha una seconda forma. Chi usa TalkBack sentiva «pioggia 0%» su un'ora per cui il provider
non ha previsto niente: lo zero inventato che tutta la Fase 26 ha estirpato, sopravvissuto
nell'unico posto che non si legge con gli occhi. La riga della settimana lo risolveva già nel
modo giusto due schermate sotto (`week_day_desc` / `week_day_desc_no_rain`); ora la cella ha la
stessa coppia, con `hour_cell_desc_no_rain`.

### 4. Il fuso veniva dalla città dove poteva venire dal report

`City.timezone` è `null` per il luogo GPS, e apposta: `timezone=auto` lo risolve dalle
coordinate (`toGpsCity`). Solo che **sei superfici** risolvevano il fuso dalla città e non dal
report, quindi per la posizione cadevano tutte su `ZoneId.systemDefault()`. In `WidgetData` si
vedeva nel modello stesso: `zone` era calcolato **prima** di caricare il report, mentre il
`content` costruito subito dopo — da `TodayStateBuilder`, che il report lo legge — usava quello
del luogo. Una card, due orologi, che è esattamente la cosa che il commento lì accanto dice di
esistere per impedire.

Adesso c'è una risoluzione sola, `placeZone(report, city)` in `:core:domain`, con l'ordine
scritto come ordine di quanto ciascuna fonte sa: il fuso del report (la risposta del provider
sul punto per cui la previsione è stata chiesta, mai assente in una risposta che ha fatto
parse) → quello della città (il geocoder; assente per la posizione) → quello del device, che è
una stima. Un id che `ZoneId` non sa leggere cade alla fonte dopo invece di lanciare, che è il
`runCatching` che ogni chiamante si scriveva a mano.

Adottata da `SkyUiState`, `AlertsViewModel` (sia la preview che il flusso di contenuto, che
legge il report in cache — un hit in memoria nel caso ordinario, e quel blocco va già alla
tabella della cronologia una riga sopra), `WidgetData` (con il calcolo spostato **dopo** il
report), `JournalStateBuilder` e il suo ViewModel, `SkyAlarmScheduler`, `SkyAlarmReceiver` e
`TodayStateBuilder`. E dalle quattro che erano già giuste ma tenevano la loro copia del
`runCatching` — `WeatherRecency`, `WeatherSyncWorker` (due), `AlertNotifier` — via
`WeatherReport.zone()`.

La fixture di `SkyStateBuilderTest` ha pagato il cambio, e aveva ragione lei a rompersi: dava a
una città di Milano il `sampleWeatherReport()`, che è di New York, e lasciava stare
l'incoerenza perché il builder il fuso lo prendeva dalla città. È un accoppiamento che il
repository non può produrre — `map()` scrive la `Location` dalla città stessa che le è stata
chiesta — quindi la fixture adesso allinea la `location` al luogo, e il test
(`every time on the screen is the city's, not the phone's`) continua ad avere i denti: il fuso
del device in CI non è né Roma né New York.

### Come è stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug` verdi, **1537 test** (quattro nuovi sul
mapper, tutti e quattro verificati rompendo il codice), lint a zero errori.

### Quel che è stato guardato e lasciato stare

- **Il weather_code, la nebbia e il codice del giorno**: niente da correggere. La riparazione
  della nebbia contro la visibilità, la regola di persistenza a una sola direzione, il cancello
  di materialità e la clausola `HazardCodes` che viene prima sono misurati, documentati e
  coperti da 38 test. Verificato che `skyCode()` riproduce i bucket di Open-Meteo
  (`<20/50/80`) e che la soglia nebbia a 1000 m è la loro.
- **`is_day` contro `AstronomyEngine`**: sembravano due definizioni di giorno/notte in
  disaccordo, e non lo sono. Su Milano `is_day` gira alle 08:00 (alba 07:07) e alle 20:00
  (tramonto 19:24), cioè «sole sopra l'orizzonte all'inizio dell'ora» — identico a quel che
  calcola l'engine. Ridondanza, non difetto.
- **`sunrise`/`sunset`/`daylight_duration`**: chiesti, deserializzati, scritti nella cache e
  letti da nessuno in produzione dalla Fase 16e. Toglierli ridurrebbe la risposta e tre
  superfici di parse-failure a valore zero, ma è un giro suo.
- **Liste DTO a elementi non-nullable per campi model-dependent** (`uvIndexMax` e compagnia):
  fragilità reale — un null lì non degrada un tile, fa fallire l'intero report — ma **non un
  bug vivo. Verificato**: `uv_index_max` torna `[null, null, null]` con
  `models=icon_seamless` e `jma_seamless`, e per date passate anche con `best_match` (Milano
  1-3 luglio), ma l'app non passa né `models=` né `past_days`, e su `best_match` a 7 giorni
  non si è mai visto. Collegato: il `?: 0` alla riga di `uvMax` è lo stesso pattern che due
  righe sotto il file rifiuta per `precipPct`.
- **`?: 0` su cieli sconosciuti**: `ArcPainter` dipinge cielo sereno per un'ora fuori
  dall'orizzonte del report (`hourCovering` torna null), e `SkySnapshot.precipPct` fa lo stesso
  per una probabilità assente. Si vede solo con un telefono offline da giorni, ed è su una
  tela e non su un numero.
- **`mapNotNull` morto in `SkyVerdictEngine`**: `window.mapNotNull { it.cloudCoverPct }` su un
  `Int` non-nullable non filtra niente, e il ramo `NO_COVERAGE` che segue è irraggiungibile
  perché `window()` torna già null se la lista è vuota.
- **Soglia neve**: `WET_DAY_MM = 1.0` è equivalente in acqua, e i codici neve 71/73/77/85 non
  sono in `HazardCodes`. Due ore di neve moderata con 0,9 mm w.e. — circa un centimetro a
  terra — perdono l'etichetta. La valvola delle 3 ore copre il caso lungo, non questo: da
  misurare a parte, con lo stesso metodo delle 161 giornate-città della Fase 26.
- **Nessun `callTimeout` su OkHttp**: restano i 10s di default per connect/read/write, e due
  GET in `coroutineScope` possono sommarsi oltre quel che uno schermo appena aperto sostiene.

## Secondo giro sulla review: i punti 5-9, e uno che era sbagliato io (20 set 2026)

> «I punti dal 5 al 9 pensi sia il caso di farli ora? Se sì parti. Decidi per ogni punto cosa
> è meglio fare (o non fare).»

Quattro fatti, uno no. Quello che non è stato fatto è quello la cui premessa, letta bene, era
sbagliata.

### 5. `uv_index_max` nullable fino in fondo — **fatto**

La fragilità era vera e vale la pena dirla per esteso, perché il modo in cui fallisce non è
quello che sembra: `DailyDto.uvIndexMax` era `List<Double>`, e un `null` lì dentro non
degrada un tile, **fa lanciare il deserializzatore e si porta via l'intero report** — la
settimana di previsione, il blocco corrente, le ore. Verificato sull'endpoint vero:
`uv_index_max` torna `[null, null, null]` con `models=icon_seamless` e `models=jma_seamless`,
e per date passate anche con `best_match` (Milano, 1-3 luglio). Chiaro non passa né `models=`
né `past_days`, quindi non l'ha mai incontrato; ma `best_match` sceglie il modello per regione
e Open-Meteo cambia quelle scelte, e il prezzo di sbagliarsi è l'app che non mostra niente.

Il campo prende elementi nullable, come `precipitation_probability_max` che gli sta accanto e
`visibility` nel blocco orario: è il terzo campo model-dependent scoperto, e il primo scoperto
**prima** che rompesse qualcosa. Il `?: 0` nel mapper se ne va per la stessa frase che il file
scrive due righe sotto a proposito della probabilità: uno zero UV non è un'assenza, è la
previsione di un sole che non scotta, stampata sotto «Nessuna protezione necessaria».
`DailyForecast.uvIndexMax` diventa `Int?`, e i quattro lettori lo gestiscono nel modo ovvio —
le due righe UV delle notifiche semplicemente non si dicono, e `today.uv_max` non si risolve,
che è quel che una regola fa col silenzio.

**`DailyForecast.uvDescription` è stato tolto.** Era vocabolario JSON di tweather — inglese,
mai localizzato, letto da niente in quest'app tranne il test che verificava che il mapper lo
avesse scritto, esattamente come `WeatherCondition.description` (lo dice già il KDoc di
`WeatherText`). Un indice nullable ha costretto la domanda «e la sua etichetta cosa dice
quando l'indice non c'è», e la risposta onesta era che non la stava chiedendo nessuno.
`CurrentConditions.uvDescription` resta, morto allo stesso modo: lì non c'è niente che
costringa, e toglierlo sarebbe scope creep. Resta scritto qui.

Due test nuovi, e il primo dice a parole la modalità di fallimento che sostituisce: il report
mappa tutti e sette i giorni, il giorno dice di non sapere, e corrente e ore sono intatte.

### 6. `sunrise`, `sunset`, `daylight_duration` fuori dalla richiesta — **fatto**

Chiesti al provider, deserializzati, scritti in ogni voce di `ReportDiskCache` e letti da
nessuno in produzione da quando la Fase 16e ha dato l'astronomia ad `AstronomyEngine`. Erano
rimasti perché «non costano niente», che è vero dei byte e falso di tutto il resto: sono tre
campi non-nullable in più che una risposta deve soddisfare perché l'intero report faccia
parse, e — questa è la ragione che ha deciso — **sono una trappola da ieri**. Il provider li
scrive sull'offset fisso che `utc_offset_seconds` nomina, quindi dopo un cambio d'ora
derivano di un'ora dall'orologio: chi trovasse `daily.sunrise` lì nel DTO e lo usasse
reintrodurrebbe esattamente l'ora che il giro precedente ha tolto. La risposta per Sydney del
4 ottobre dichiara un'alba alle 05:28 dove l'orologio lì dice 06:28.

Nessuna migrazione: `ignoreUnknownKeys` fa decodificare le voci di cache già scritte, che
quelle chiavi ce l'hanno ancora. Verificata la richiesta ridotta contro l'API vera — 168 ore,
7 giorni, le cinque variabili giornaliere che servono e nient'altro.

### 7. L'arco che dipinge sereno dove non sa — **non fatto, e la segnalazione era sbagliata**

`ArcPainter` passa `cloudPct = hour?.hour?.cloudCoverPct ?: 0` per un campione che il report
non copre, e sembrava «sereno disegnato dove non si sa». Leggendo `SkyPalette.gradient`: il
mix nuvole è `lerp(stop, grigio, CloudDesaturation * cloudPct/100)` più un `dim` che vale
`1 - CloudDarkening * cloudPct/100`. **A zero il lerp non fa niente e il dim vale 1**: zero non
è «sereno», è *nessuna modulazione* — la banda di altitudine esattamente come il sole l'ha
disegnata.

Che è la risposta onesta, non una svista: per un'ora oltre l'orizzonte della previsione l'arco
conosce la luce e non dice niente del tempo. Qualunque altro valore sarebbe un cielo che non
gli è stato detto (50 % non è «non so», è «mezzo nuvoloso»). Resta un commento al call site,
perché il prossimo lettore inciampi meno di quanto ci sia inciampato io.

### 8. Il `mapNotNull` morto in `SkyVerdictEngine` — **fatto**

`window.mapNotNull { it.cloudCoverPct }` su un `Int` non-nullable non filtrava niente, e il
ramo `NO_COVERAGE` sotto era irraggiungibile perché `window()` rifiuta già la lista vuota. Era
una nullabilità che il tipo non ha, scritta dentro codice che suggerisce di averla — e il KDoc
di `HourlyForecast.cloudCoverPct` spiega per intero perché quella nullabilità è stata tolta
apposta. Il valore d'enum resta: `horizonNote` è dove la domanda trova davvero risposta.

### 9. `callTimeout` su OkHttp — **fatto, 30 secondi**

I default di OkHttp limitano ogni **fase** — 10s per connettere, 10s fra un byte e l'altro — e
niente limita la chiamata. Una connessione che sgocciola un byte prima di ogni read timeout non
ne fa scattare mai uno: su uno schermo appena aperto è un refresh che non si risolve, e dentro
`WeatherSyncWorker` è un worker tenuto aperto sulla batteria di qualcuno. Trenta secondi stanno
oltre il caso peggiore onesto (connessione più lettura lenta di 10 KB) e dentro quel che l'app
assorbe: ogni chiamante ha già il report in cache come ripiego, e il pull to refresh è un
gesto.

### Come è stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verdi, **1541 test**
(quattro nuovi nel giro precedente, due in questo), lint a zero errori. La richiesta ridotta
ricontrollata contro l'endpoint vero, e il caso «modello senza UV» ricontrollato con
`models=jma_seamless` perché il test riproduca l'API e non la mia idea dell'API.

### Punto 10, la soglia della neve: cosa sarebbe, e perché non è in questo giro

`WET_DAY_MM = 1.0` è **millimetri di equivalente in acqua**, e `precipitation` di Open-Meteo
somma pioggia e neve in quell'unità. Un millimetro d'acqua è circa un centimetro di neve
fresca: la stessa soglia pesa dieci volte di più quando quel che cade resta per terra. E i
codici neve 71 (debole), 73 (moderata), 77 (granelli) e 85 (rovesci deboli) **non** sono in
`HazardCodes` — ci sono solo 75 e 86, i «forte». Quindi due ore di neve moderata con 0,9 mm
w.e. non superano né il millimetro né le tre ore, e la giornata viene etichettata dalla
nuvolosità: «Coperto» su un giorno con un centimetro a terra.

La valvola delle tre ore copre il caso lungo e non questo, ed è il motivo per cui non è una
correzione ovvia: la soglia com'è è **misurata** (161 giornate-città, 23 città, Fase 26) e ha
risolto un difetto vero, quindi cambiarla a intuito vale meno di lasciarla. La domanda giusta
non è «alzare o abbassare» ma *quante giornate di neve vere l'attuale ne manca*, e si risponde
con lo stesso metodo: un mese d'inverno su una decina di città nevose (Mosca, Helsinki, Oslo,
Sapporo, Québec, Innsbruck, Denver, Harbin, Tromsø), `dailyCode` ricalcolato su ogni
giornata-città, contate quelle in cui i codici neve ci sono ma l'etichetta va al cielo, e
incrociate con `snowfall` in centimetri — che è il campo che dice se per terra c'era qualcosa.
Se sono poche, la soglia resta e questa nota è la risposta; se sono molte, la correzione
candidata è una soglia in acqua più bassa quando i codici della giornata sono di neve, non un
numero diverso per tutti.

**È una misura che si può fare, ma richiede l'archivio** (`archive-api.open-meteo.com`), perché
l'endpoint di previsione accetta `start_date` solo per circa tre mesi indietro e a settembre
non arriva a gennaio. Tentata oggi: `HTTP 429, Daily API request limit exceeded` — la quota
gratuita dell'IP di questo ambiente era già esaurita dalle quindici località della review. Da
riprendere con la quota libera; è un giro suo, non una riga di codice.

---

## Gli avvisi che non potevano suonare, e due widget che aprivano la schermata sbagliata (committente, 21 set 2026)

> «Quando si installa l'app alcuni Avvisi sono già attivi ma il permesso per le notifiche non è
> mai stato chiesto e quindi questi avvisi non scattano mai. [...] Ora l'unico modo per fare il
> consenso al permesso è disattivare e poi riattivare un avviso.»

La segnalazione è esatta e il difetto è più vecchio della schermata: `NotificationSettings` nasce
con `severeWeatherAlerts`, `precipitationWarning`, `userRules` e `officialWarnings` a **true**, e
`POST_NOTIFICATIONS` veniva chiesto solo dentro `somethingTurnedOn()` — cioè dall'**atto** di
accendere un interruttore. Un'installazione nuova non accende niente: trova tutto già acceso. Quindi
quattro promesse fatte e nessuna richiesta fatta, e l'unica strada al dialogo di sistema era
spegnere e riaccendere, che non è una strada che qualcuno trova.

VISION §5.8 diceva «le notifiche sono richieste la prima volta che il lettore accende qualcosa che
ne ha bisogno, mai all'avvio». Era onesta sul *quando* e sbagliata sul *se*: la regola vale solo se
i predefiniti sono spenti, e non lo sono. Due modi di chiuderla, e ci vogliono **entrambi**.

### Perché il dialogo di sistema, da solo, non è la riparazione

Android 13 mostra quel dialogo **al massimo due volte per installazione**. Dopo, `launch()` torna
`false` senza disegnare niente. E c'è un secondo vicolo cieco che si legge uguale da uno schermo e
si ripara in un posto opposto: permesso **concesso** e notifiche spente dalle impostazioni di
sistema — lì `launch()` torna `true` immediatamente, sempre senza disegnare niente. In tutti e due
i casi un pulsante cablato al dialogo è un pulsante morto, che è esattamente lo stato che stiamo
cercando di finire.

Quindi la domanda giusta non è «ho il permesso» ma `NotificationManagerCompat.areNotificationsEnabled()`:
*una notifica postata adesso arriverebbe?*. È quella che ogni schermata chiede
(`NotificationPermission.allowed`). Le due strade morte sono chiuse così:

- permesso già concesso e notifiche spente → si va **dritti** alle impostazioni di sistema, e il
  pulsante lo dice («Apri le impostazioni»);
- rifiuto che torna indietro con `shouldShowRequestPermissionRationale` **falso** → il sistema non
  ha più un dialogo da mostrare, e si consegna la pagina delle impostazioni **nello stesso tap**.

Quel `shouldShowRequestPermissionRationale` non distingue «mai chiesto» da «rifiutato per sempre»:
è falso per tutti e due. Per questo si legge **dopo** il rifiuto e non prima della richiesta — che
è anche il motivo per cui non serve persistere nessun flag «gliel'ho già chiesto». Dopo un primo
rifiuto è vero, quindi un «no» è preso per un no e nessuno viene buttato nelle impostazioni.

### La riparazione che resta: la card (DESIGN §8.14)

Su Avvisi, sopra tutto, e **solo mentre la schermata starebbe mentendo**: qualcosa è acceso e il
telefono non lo lascia passare. È la metà che conta, perché a differenza del dialogo può essere
rioffertà ogni volta, dice cosa non va prima di offrire il rimedio, e sparisce da sola quando il
permesso arriva. Si rilegge a ogni `ON_RESUME`, perché la riparazione avviene **fuori** dall'app e
una card ancora lì dopo il consenso è la stessa bugia al contrario.

Con tutto spento non è disegnata: non c'è nessuna promessa da rompere, e una card che rimprovera
per un permesso che non serve a niente sta inventando un problema — lo stesso difetto dall'altra
parte. `notificationsPromised()` è quella domanda, ed è un test: se i predefiniti si muovono,
`NotificationsPromisedTest` è dove si vede.

**Anche Cielo**, per la stessa ragione e con la soglia sua: `remindersArmed()`, cioè una campanella
davvero armata. Lì nessun promemoria nasce acceso, quindi un'installazione nuova non promette niente
e la card non c'è; ma dal momento che una campanella c'è e il permesso manca, la riga mostra una
campanella per qualcosa che non può suonare, che è la stessa bugia di Avvisi.

### E il primo avvio: sì, ma dopo il luogo, e a parole

`FirstRun` ha un terzo stato, `Notifications`, fra `Pending` e `Done`. È un passo **suo** e non una
riga sulla schermata del luogo, perché una notifica è una promessa **su un luogo** e lì il luogo
non c'è ancora. Ed è una schermata di parole con un «Consenti» esplicito, mai il dialogo di sistema
all'arrivo: quel dialogo si ha due volte in tutta la vita dell'installazione, quindi si spende su un
tap che l'ha chiesto. «Non ora» è una risposta vera e non costa niente — non tocca il dialogo, e
Avvisi tiene la stessa offerta finché è vera.

Tre regole al contorno, tutte e tre nel `CityStore` e tutte e tre con un test:

- **saltare il luogo salta anche questo**, in una sola `edit` (`markFirstRunSkipped`): due edit
  emetterebbero `Notifications` per un frame lungo la strada, e chi ha appena rifiutato di dare un
  luogo non ha niente su cui ricevere un avviso. «Salta» non può voler dire «salta uno dei due».
- **un aggiornamento non viene mai fermato**: `migrateFirstRun` scrive `notify_asked` insieme a
  `init_done` per le installazioni usate, e — il caso che il `return@edit` in cima si sarebbe perso
  — lo **riempie** anche per un'installazione già migrata dalla build precedente, che altrimenti
  avrebbe trovato una schermata intera alla prima apertura. È esattamente quello che quel controllo
  è stato scritto per evitare.
- **se le notifiche sono già permesse il passo non si vede**: non c'è niente da chiedere, quindi non
  si chiede niente.

### I due widget che aprivano la schermata sbagliata

> «Nei widget "Momenti del cielo" e "Arco del giorno" al tap aprire l'app direttamente nella
> schermata Cielo, che dovrebbe essere più corretto rispetto alle informazioni che i widget
> visualizzano.»

Giusto, ed è il principio a essere giusto: quei due disegnano materiale della schermata Cielo — i
momenti, i verdetti, il prossimo momento della luce, l'agenda — e atterrare su Oggi chiedeva al
lettore di andare a ritrovare quello che aveva appena letto sullo schermo di casa.

`WidgetCard` prende un `destination: ShellTab?` — `null` resta «apri l'app», che atterra dove il
lettore l'ha lasciata, ed è quello che fanno ancora le altre tre card. La destinazione viaggia
nell'**azione** dell'intent oltre che in un extra: gli extra non entrano in `Intent.filterEquals`,
che è la chiave della cache dei `PendingIntent`, e l'azione è quella che resta leggibile in un
intent a cui gli extra non sono sopravvissuti. (Glance timbra già un `data` unico per widget, quindi
le due cose sono cintura e bretelle; verificato leggendo il bytecode di `ApplyActionKt`, che usa
`PendingIntent.getActivity` con il **nostro** intent, azione e flag compresi.)

I flag sono la ricetta di un deep link in un'app a un task solo: `CLEAR_TOP or SINGLE_TOP` consegna
l'intent all'istanza già viva via `onNewIntent` invece di finirla e ricostruirla. Senza, il **primo**
tap avrebbe funzionato e tutti quelli dopo no — l'app sarebbe tornata avanti sulla scheda di prima,
che è il caso che un lettore incontra ogni volta tranne una. Il tab è stato, non una rotta, quindi
`MainActivity` tiene la richiesta e la passa alla shell, che la consuma e **chiude gli overlay**:
atterrare su Cielo sotto una pagina Impostazioni aperta sarebbe rispondere al tap e nascondere la
risposta. Una richiesta arrivata prima che il primo avvio abbia risposto non si perde: resta lì
finché le schede esistono.

Le altre tre card sono rimaste come stavano. Lo stesso principio le manderebbe su Oggi, ma non è
stato chiesto e non è un difetto: «apri l'app» è una risposta onesta per una card che mostra il
tempo di adesso.

### Come è stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug` verdi, **1587 test** (18 nuovi nell'app,
4+2 nel `CityStore`), lint a zero errori. `:app:assembleDebug` e `:app:assembleRelease
-PsignReleaseWithDebugKey` costruiscono, così l'R8 è passato anche lui. `ShellDestinationTest`
costruisce davvero il `PendingIntent` sopra l'intent, perché un intent implicito sarebbe rifiutato
in faccia su questo minSdk.

Quel che **non** è coperto da un test e va guardato su un dispositivo: il dialogo di sistema non
esiste in Robolectric, quindi il percorso «rifiuta due volte → il tap seguente apre le impostazioni»
è ragionato sul contratto di `ActivityResultContracts.RequestPermission` e non misurato. Da provare
su un telefono: installare, arrivare al passo due, dire di no due volte, e controllare che il
pulsante su Avvisi porti nelle impostazioni di sistema e non da nessuna parte.

---

## Quattro predefiniti, e due task che erano tre (committente, 21 set 2026, dal dispositivo)

Installazione da zero, provata: il permesso notifiche viene chiesto e la card su Avvisi fa il resto.
Da lì, quattro predefiniti da spostare e due difetti veri trovati usando l'app.

### I predefiniti

**I due riepiloghi, accesi.** `dailySummary` e `eveningSummary` nascevano a `false` su un argomento
scritto per intero nel KDoc: «un digest che nessuno ha chiesto è l'unica notifica per cui un'app
meteo viene disinstallata». È l'argomento giusto su *un digest* e quello sbagliato su questi due.
Sono l'app: una frase prima di qualunque numero, detta all'ora in cui la giornata è ancora una
decisione. Un'installazione che non apre mai Avvisi prendeva le allerte e niente della lettura. Il
tetto è una notifica ciascuno, a ore fisse, e si spengono in due tap.

**La card blu al posto del cielo.** Il gradiente resta l'eroe dell'app ed è a una riga di distanza.
Quel che non è, è la cosa giusta da incontrare su una schermata di casa che non ha mai visto: è una
fotografia del tempo dietro una card di fatti, e su uno sfondo carico i due terreni litigano — che
è esattamente quel che lo scrim e il cursore dell'opacità servono a governare, e un predefinito non
dovrebbe aver bisogno di essere governato. Una card piatta colorata è la grammatica del launcher,
si legge a distanza di braccio su qualunque wallpaper, e il colore è una riga sotto.

**Massima e minima su «In parole».** Questo è l'unico campo che risponde **diverso per card**, e per
questo `WidgetLook.defaultsFor(kind)` esiste invece di cinque costanti: un predefinito che varia è
precisamente quello che finisce scritto due volte — nello store e nella schermata impostazioni — e
poi divergono. «In parole» non ha un disegno da proteggere: la sua gerarchia è costruita di tipo, in
ranghi di fatti, e la massima e minima sono un rango già progettato. Lasciarlo vuoto è l'unica card
che ci perde. Sulle altre vale il KDoc di sempre: il numero nudo è l'eroe.

Due fixture si appoggiavano ai vecchi predefiniti e adesso nominano tutto quel che dichiarano —
`SyncSchedulerTest.allOff`, `AlertEngineTest.allOn`/`none` e `WeatherSyncWorkerTest.allTogglesOff`,
che si chiamava «tutti spenti» e ne lasciava uno acceso. Una fixture che dipende da un predefinito
smette di dire la verità il giorno che il predefinito si muove, in silenzio.

### I due task che erano tre

> «Se apro le proprietà di un widget e poi la chiudo con lo swipe dal basso e poi apro l'app, con
> la gesture back invece di tornare alla home mi ricompare la schermata proprietà.»
> «Alcune volte mi trovo ad avere due schermate principali dell'app.»

Stessa famiglia, e la diagnosi è una riga: **Android identifica un task dall'intent che lo ha
creato**, e questa app entrava da tre porte diverse con tre intent scritti a mano.

`WidgetConfigActivity` e `ArcConfigActivity` avevano l'affinità del pacchetto, quindi stavano nel
**task dell'app**. Chiuderle con lo swipe le lasciava lì; il lancio successivo dell'app metteva la
schermata principale sopra, e il back usciva su una schermata che il lettore aveva già congedato.
Ora hanno `taskAffinity=""` — nessuna affinità, mai quel task — e `noHistory="true"`, così una
schermata scacciata è finita e non un task che nessuno vede e nessuno può chiudere. Non si perde
niente: ogni scelta lì è scritta nell'istante in cui è toccata, e «Fatto» chiude solo la porta. Ed è
sicuro verso l'host perché tutti e cinque i provider dichiarano `configuration_optional`: quella
schermata non gira mai al momento del piazzamento, quindi nessun widget aspetta il suo risultato.
C'è un test che tiene ferma quella parola, perché è la premessa del `noHistory`.

Le due schermate principali sono l'altra metà, ed è **colpa del giro precedente**. Avevo messo la
destinazione nell'**azione** dell'intent, ragionando che gli extra non entrano in `filterEquals` e
quindi non distinguono due `PendingIntent`. Il ragionamento era giusto e la conclusione rovesciata:
non entrare in `filterEquals` è **esattamente** il motivo per cui l'extra è l'unico posto dove la
destinazione può viaggiare senza che il task smetta di somigliare a quello del launcher. Con
un'azione nostra, il tap sull'icona non riconosceva più il task aperto dal widget e ne apriva un
secondo — la sovrapposizione nello screenshot.

Quindi: **una porta sola**, e **è l'intent del launcher** — `ACTION_MAIN`, `CATEGORY_LAUNCHER`, il
nostro componente, più l'extra quando c'è una destinazione. Ci passano tutti e cinque i widget
(anche i tre che non chiedono una scheda) e tutti e quattro i notificatori, che avevano ciascuno il
proprio `Intent(context, MainActivity)`. E `MainActivity` è `singleTask`: è quel che l'app **è** —
una sola activity, con Impostazioni e la guida che sono stato al suo interno — e senza, la
piattaforma ne faceva un secondo task ogni volta che l'intent non era quello del launcher. I flag
scritti a mano nel giro precedente (`CLEAR_TOP`, `SINGLE_TOP`) facevano metà di quel lavoro a mano e
non potevano fare l'altra metà, che è garantire che ci sia **una** istanza a cui consegnare. Resta
`NEW_TASK`, che la piattaforma pretende da un `PendingIntent`.

### Gli angoli bianchi: la stessa causa, e come verificarlo

> «Quando stacco il dito e l'app fa l'animazione completa di chiusura vedo gli angoli arrotondati
> bianchi e non più trasparenti. Sembra che lo fa solo quando l'app è stata aperta dal widget.»

«Solo quando è stata aperta dal widget» è la diagnosi dentro la segnalazione, ed è la ragione per
cui non è un problema di `windowBackground`: quello è lo stesso da qualunque porta si entri. Quel
che cambia con la porta è **quale animazione di chiusura sceglie il sistema**. Il back predittivo,
con il dito giù, anima la superficie viva dell'activity e la ritaglia: fuori dal ritaglio non c'è
niente, quindi trasparente. Al rilascio parte la transizione vera, e su un task che il launcher
riconosce come una delle sue icone è il launcher a disegnarla — quella che rientra nell'icona. Su un
task che non riconosce, il sistema usa la chiusura generica, che dipinge il colore di fondo del task
(preso da `windowBackground`, bianco in chiaro) dietro la superficie che si rimpicciolisce, angoli
arrotondati compresi.

Con una porta sola che è l'intent del launcher, e `singleTask` sopra, quel task non esiste più. **È
la correzione che mi aspetto risolva anche questo**, e va detto chiaramente che è l'unico dei
quattro punti che non ho potuto misurare: l'animazione di chiusura non esiste in Robolectric.

Da provare sul telefono, nell'ordine: (1) riavvio pulito, tap sul widget con app chiusa, back →
gli angoli devono essere trasparenti come da icona; (2) apri da icona, poi da widget, poi back due
volte → deve uscire alla home, non su una seconda schermata principale; (3) matita del widget,
swipe in alto, apri l'app, back → home. Se il punto (1) resta bianco mentre (2) e (3) sono a posto,
la causa non è il task e la leva successiva è `android:windowBackground`, che oggi è quello di
`Theme.DeviceDefault.DayNight` — con la nota che renderlo trasparente si paga con qualche frame di
launcher visibile all'avvio a freddo, che è precisamente il motivo per cui quel tema è lì.

### Come è stato verificato

`./gradlew test :app:testDebugUnitTest :app:lintDebug` verdi, **1599 test** (12 nuovi: la porta
riscritta, i predefiniti delle card, e tre attributi di manifest che nessun test sugli intent può
vedere), lint a zero errori. `:app:assembleDebug` e `:app:assembleRelease -PsignReleaseWithDebugKey`
costruiscono.

### Le notifiche atterrano dove devono (committente, stessa giornata)

Chiuso il giro, restava la riga che la porta unica aveva reso gratis: la stessa regola dei widget,
**la schermata di cui la notifica parla**.

- Il promemoria del cielo → **Cielo**: è la riga di quel momento, la sua campanella, il verdetto con
  il numero che l'ha deciso e la frase del catalogo su cosa sia quel momento.
- Una regola che è scattata → **Avvisi**: la card di quella regola è lì, con l'ora dell'ultimo
  scatto, che questa notifica ha appena cambiato.
- Un'allerta ufficiale → **Avvisi**, non Oggi. Il banner su Oggi c'è, ma solo da gialla in su e solo
  finché il bollettino è vivo, e di una zona verde non dice niente (DESIGN §8.13). Avvisi è la
  schermata dove uno è andato a chiedere, e da lì si apre il foglio con l'aritmetica: la griglia
  rischio per giorno, cosa vuol dire quel livello, l'attribuzione.
- I quattro avvisi predefiniti → **Oggi**. Sono il tempo: le ore, la pioggia, la frase. Avvisi è
  dove sta l'**interruttore** che li ha mandati, che non è quel che cerca chi ha toccato «Pioggia
  alle 17».

C'è un costo nascosto nell'aver fatto una porta sola, e va scritto perché è invisibile: adesso i
quattro intent sono `filterEquals`-identici, e `PendingIntent` chiave la cache **su quello**. Gli
extra non li vede. Quindi il **request code** è l'unica cosa che tiene separate due destinazioni, e
con `FLAG_UPDATE_CURRENT` due chiamanti che ne condividono uno fanno sì che il secondo riscriva la
destinazione del primo — sotto una notifica già sulla tendina. I quattro passano il proprio id di
notifica, e quei quattro intervalli erano già disgiunti e documentati dove sono calcolati
(1001-1004 gli avvisi predefiniti, 2000-2999 le regole, 3000-3999 le allerte, 7000+ il cielo).
`SkyNotifier` era l'unico che non lo faceva: passava `0` fisso, che prima non voleva dire niente e
adesso sarebbe stato il buco.

`NotificationDestinationTest` posta **tutte e quattro sullo stesso manager** e rilegge le
destinazioni dopo, non una alla volta: una collisione si vede solo quando le quattro coesistono.
Verificato che il test serva davvero, rompendolo apposta — messo `1002` come request code del cielo,
il test combinato fallisce con «the sky reminder must be the only one on Cielo, expected 1 but was
null» (l'avviso pioggia, postato dopo, gli aveva riscritto gli extra), mentre i quattro test singoli
restavano verdi. È esattamente il motivo per cui il primo test esiste nella forma che ha.

`./gradlew test :app:testDebugUnitTest :app:lintDebug` verdi, **1609 test**, lint a zero errori.

## Revisione della schermata Cielo: sette difetti, e uno era la promessa della guida (committente, 20 set 2026)

Chiesta una revisione delle feature e degli eventi del cielo: che cosa è sbagliato, come sono
resi, che cosa manca. Ne sono usciti sette punti, affrontati nell'ordine che il committente ha
dato — ① ③ ④, poi ② ⑤ ⑥ ⑦ — e il primo non era un difetto di calcolo: era il codice che diceva
una cosa diversa dalla propria guida.

### ① La finestra di buio non era l'intersezione che la guida descrive

`sky_about_darkness` promette, in tutte e due le lingue, «la notte astronomica — il sole a più di
18° sotto l'orizzonte — **con in più la luna sotto l'orizzonte**», la chiama «l'unico evento qui
dentro che sia un'intersezione invece di un attraversamento», e chiude con la frase che ha deciso
la correzione: «certe notti la finestra è tutta la notte, certe notti è novanta minuti, e certe
notti non c'è affatto». L'aiuto della card Stanotte dice la stessa cosa: «le ore in cui il cielo è
davvero scuro **e la luna è fuori dai piedi**».

`SkyScheduler.darkness()` restituiva crepuscolo astronomico → alba astronomica, e basta. La luna
non entrava nella finestra: rientrava all'altro capo come declassamento del verdetto. Quindi la
finestra non era **mai** di novanta minuti e non mancava **mai** per la luna, e la card stampava
«Buio dalle 21:47 alle 05:12» anche con la luna piena alta per sei di quelle ore. Non è una
sfumatura: è la riga che la guida indica come la ragione per cui questa parte dell'app esiste.

La correzione è un'intersezione, non un motore. `AstronomyEngine` guadagna `moonDownRuns`, che è il
complemento di `aboveAltitude` già in casa — una **lista** di tratti e non il primo, perché una
notte può averne due (luna che sorge alle dieci e tramonta alle tre lascia la scheggia prima e le
ore dopo, e dare la scheggia perché viene prima sarebbe la risposta sbagliata una volta su due).
La soglia è quella di sorgere della luna, parallasse compresa, la stessa che attraversano
`moon.rise` e `moon.set`: una finestra che dicesse la luna giù a un'ora che la riga sopra chiama
moonrise sarebbe lo schermo che litiga con se stesso.

`SkyScheduler.darkNight(date)` torna **due** risposte — la notte e la finestra — perché lo schermo
ha bisogno di tutte e due: la card stampa la finestra, e la riga sotto può dire *perché* è corta
solo se sa ancora quanto era lunga la notte. `darkness()` è la seconda; `milkyWayCore` passa da lì,
quindi il nucleo galattico eredita il filtro lunare, che è come doveva essere (una luna gibbosa
cancella il centro della Via Lattea esattamente come il crepuscolo).

**Nessun pavimento sulla durata.** Una finestra di dodici minuti è una cosa strana da sentirsi dire
ed è vera; inventare una soglia sotto la quale l'app la chiama `∅` sarebbe barattare un fatto per
uno schermo più ordinato, cioè il baratto che questo modulo esiste per rifiutare.

Misurato a Milano, agosto 2026, ed è la frase della guida diventata tabella:

| notte | illuminata | finestra |
|---|---|---|
| 12 ago | 0% | 22:33 → 04:23, tutta la notte |
| 23 ago | 82% | 01:11 → 04:42, 152 minuti |
| 24 ago | 89% | 03:17 → 04:45, **88 minuti** |
| 26-30 ago | 98% | `∅` — la luna è su dal crepuscolo all'alba |

Il terzo stato di card vuoto è nuovo (`MOON_ALL_NIGHT`) e ha la sua frase, che dice anche la notte
che la luna **non** ha cancellato: il cielo è comunque scuro dalle 22:01 alle 04:49, e una card che
dicesse solo «niente finestra di buio» farebbe credere che a sbagliare sia il sole.

Effetto collaterale, e riparazione di un secondo rilievo della revisione: la card mostrava **un
numero solo** (`moonLine ?: evidence ?: reason`), quindi quando decideva la luna la percentuale di
nuvole spariva. Adesso la luna è un fatto sui bordi della finestra e non un declassamento, e sta
su una riga sua; l'aritmetica del verdetto sta sotto, sempre.

### ③ e ④ «In arrivo» buttava via i `∅`, e non garantiva una riga a chi si era iscritto

Sono due facce della stessa lista, quindi una correzione sola.

`events()` faceva `filterIsInstance<SkyOccurrence.At>()`: un `∅` spariva. Ma `SkyScheduler` ha una
dottrina scritta e opposta («un giorno il cui risultato è `∅` è **riportato**, non saltato») e la
lista `moments` accanto la rispetta. Misurato: le **Perseidi a Stoccolma** danno `NO_DARKNESS` nel
2026 e nel 2027, e `next(limit = 1)` restituisce quel `∅`, quindi non si guardava nemmeno l'anno
dopo. Lo sciame più famoso dell'anno semplicemente non esisteva, senza una parola.

E le sei righe erano scelte **per data su tutto il catalogo**. Misurato a Milano il 20 settembre
2026: equinozio, luna piena, Draconidi, Tauridi australi, Orionidi, Tauridi boreali. Sette
settimane, tre sciami minori su sei righe, e fuori le Geminidi (14 dicembre), il solstizio, il
tramonto più presto, la luna piena più vicina dell'anno e tutte e due le eclissi — la solare da
Milano è il **2 agosto 2027**, diciottesima per data. Chi si iscriveva a «Eclissi di Sole» aveva
la spunta nel catalogo, **un promemoria che funzionava davvero** (`SkyAlarmScheduler` pesca tutte
le sottoscrizioni) e nessuna riga da nessuna parte. La campanella e lo schermo non erano d'accordo
su che cosa il lettore stesse seguendo, che è esattamente il difetto per cui `SkyUpcoming` era
stato scritto.

Adesso la lista ha **due strati**. Il primo sono le righe del lettore: ogni sottoscrizione non
giornaliera, sempre, `∅` compresi con la loro ragione. Il secondo è il calendario di tutti, i
prossimi eventi annuali che non sono già nel primo, a riempire fino a sei — con un pavimento di
tre, perché uno schermo che cancellasse il prossimo sciame per far posto a sette iscrizioni
avrebbe scambiato un buco con un altro. Nel secondo strato i `∅` non entrano: un `∅` che nessuno
ha chiesto è un fatto di qualcun altro, e stamparne tredici a una latitudine da notti bianche
seppellirebbe la lista che deve riempire.

La metà **aperiodica** resta legata alle sottoscrizioni come sempre: una ricerca di eclissi cammina
anni di lune nuove, e farne due per chi non ha chiesto niente sarebbe batteria spesa per niente.

### ⑤ La riga non diceva né l'anno né l'ora

`d MMMM`, su una lista che arriva anni avanti: `EclipseEngine` cerca sei anni una solare, e
«2 agosto» era una data che il lettore non poteva collocare. L'anno lo decide il builder e non la
riga (`UpcomingEvent.showYear`), perché l'orologio sta lì: il builder è puro e prende `now`, un
composable dovrebbe farselo passare.

L'ora la stampano **le sole eclissi**, e la regola è scritta dov'è: la riga di uno sciame è una
notte larga nove ore e un solstizio è una data, un'eclissi è novanta minuti per cui uno esce di
casa o li perde.

### ⑥ Due righe per un evento solo

Tre collisioni, una regola. Le **Delta Aquaridi e le Alfa Capricornidi** hanno tutte e due
longitudine solare 127.0° — la lista IMO le mette davvero lì — quindi stesso istante, stessa
notte, stessa icona: misurato, `2027-07-29 23:05` per entrambe. Quelle si **fondono** e tengono
tutti e due i nomi. Le altre due sono lo stesso evento chiesto due volte (`moon.phase` che risolve
su una luna piena che il lettore segue anche per nome; la luna piena più vicina dell'anno che
cade sulla riga della luna piena semplice): lì vince la riga più specifica, e una riga con la
campanella batte una senza — togliere di mezzo quella a cui il lettore si è iscritto gli
porterebbe via la campanella dallo schermo.

### ② `withMoon` non faceva quel che diceva la sua KDoc

«Sampled across the window, not at its start: a moon che tramonta un'ora dopo lascia usabile gran
parte della notte». Il codice prendeva cinque campioni, filtrava quelli con la luna su e poi
leggeva solo la loro **illuminazione massima**: quanti campioni fossero su veniva buttato. Una luna
su per un campione su cinque declassava quanto una su per cinque su cinque.

L'illuminazione è la variabile sbagliata da misurare lungo una finestra, perché in una notte non si
muove; la **quota di finestra** con la luna sopra l'orizzonte è quella che si muove, ed è quella che
decide adesso. Soglia alla metà — una notte la cui metà più buia è senza luna è una notte in cui si
esce — e campioni da cinque a nove, perché cinque quantizzavano la quota a un quinto.

Nota d'ordine: dopo ① la finestra di buio e il nucleo galattico sono senza luna per costruzione,
quindi `withMoon` non scatta più per loro. Resta dov'era sempre servito: sciami e luce zodiacale.

### ⑦ VISION §5.3 era ferma a Fase 5

Diceva «the 32-job catalog» e cinque gruppi: sono 51 in sei (mancava Eclissi), e §6 dice 51 da
sempre. Era il conto giusto alla Fase 5, e la Fase 19 ha aggiunto la coppia di cielo buio, i
quarti per nome, le due eclissi, i fatti annuali e tre sciami senza che questa riga seguisse. La
riga di Fase 5 qui sotto resta com'è: è un log, e alla Fase 5 trentadue era vero.

### Quel che NON è stato toccato

Dalla stessa revisione erano uscite tre proposte che non sono difetti e non entrano in questo giro:
la media delle nuvole su otto-dieci ore come riassunto della notte (buttare via il tratto buono che
l'app ha, ora per ora), il catalogo da 51 voci senza ricerca, e quattro eventi nuovi (finestra
senza luna — che ① adesso rende possibile —, luna piena che sorge al crepuscolo, luce cinerea,
azimut del tramonto). Restano da decidere.

### Come è stato verificato

Le misure di questa voce non sono ragionate: `:core:domain` compilato e interrogato con test
usa-e-getta, poi rimossi, che stampano la lista «In arrivo» come la costruisce lo schermo, la
tabella delle finestre di agosto e l'altezza della luna ora per ora sulla notte usata dalle
fixture.

Test nuovi, tutti su fatti misurati e non su numeri scelti: la notte astronomica resta dusk →
dawn di domani (`darkNight().night`), la finestra è la notte meno la luna e si apre al **moonset
al minuto** della riga che la stampa, la luna piena può prendersela tutta, a luna nuova è la notte
intera; una sottoscrizione lontana ha comunque la sua riga con l'anno, una che il cielo salta tiene
riga e ragione e sta in fondo, due sciami di una notte stanno su una riga; e la luna che occupa
parte della finestra non la rovina tutta.

Un'asserzione è stata **riscritta dopo averla vista fallire per il motivo giusto**: dentro la
finestra la luna si misura contro l'altezza del bordo della finestra, non contro zero, perché la
luna tramonta col centro un filo **sopra** l'orizzonte geometrico (la parallasse batte rifrazione e
semidiametro messi insieme, Meeus 15.1) e lo zero non è la linea su cui quella finestra è tagliata.

Dalla rilettura avversariale del proprio diff sono usciti altri due punti, piccoli e veri: la riga
fusa dei due sciami teneva la **prima** del gruppo e non quella con la campanella (con un solo
sciame dei due sottoscritto si sarebbe portata via il promemoria del lettore dallo schermo), e la
lista degli `∅` sommava una sorgente che per costruzione non può averne.

`ItalianArticleTest` ha bocciato la prima stesura delle stringhe nuove («illuminata al %d%%»):
riscritte nella forma che l'app già usa, «%d%% illuminata». È la regola del 22 settembre che fa il
suo lavoro su chi non la conosceva.

`./gradlew test :app:testDebugUnitTest :app:lintDebug` verdi, **1620 test**, lint a zero errori.

## Fase 28 — Nove eventi nuovi, due pianeti, e un marchio che non è un'opinione (committente, 20 set 2026)

Chiesto di implementare le tre proposte rimaste della revisione, i quattro eventi nuovi, e —
riaprendo esplicitamente una decisione a verbale — Venere e Giove. In più, la domanda diretta:
segnalare che un evento si presta alla fotografia è utile o è rumore?

### La risposta sul marchio fotografico, prima del codice

**Sì, ma a due condizioni**, e senza di quelle sarebbe esattamente il rumore che il committente
temeva.

Le ragioni per cui non lo è: il catalogo ha già proprietà leggibili dalla macchina
(`observable`, `visibilityDependent`, `needsDarkness`) che generano da sole le righe «quando
capita» nella guida, e un flag viaggia sullo stesso binario senza poter divergere dalla prosa;
l'ora d'oro e l'ora blu **sono già nel catalogo solo per la fotografia**, quindi l'app ha già
quel lettore e semplicemente non lo nomina; e non aggiunge righe, quindi per chi non fotografa
costa zero.

Le due condizioni:

1. **Scarso.** Su venti eventi su sessanta non direbbe niente. Sono **nove**: le quattro ore del
   fotografo, la luna piena al crepuscolo, le due finestre di luce cinerea, il nucleo della Via
   Lattea e l'eclissi **lunare**. Il conto è un test (`SkyJobCatalogTest`), non una buona
   intenzione di chi aggiunge il prossimo job, e i casi che non devono mai prenderlo sono
   nominati: mezzogiorno solare, equinozio, perielio, fase lunare.
2. **Deve portare un'informazione, non un'etichetta.** «Bello da fotografare» è un'opinione e
   quest'app non ne stampa. Quel che serve è **dove puntare**, e quindi il flag e l'azimut sono
   una cosa sola: il marchio dice *porta la macchina*, la riga accanto dice *verso ovest-nordovest*.
   `SkySights.bearing` risponde `null` sulla maggior parte delle righe apposta — un equinozio non
   capita in una direzione, e «il sole sorge verso est» non insegna niente a nessuno — ed è un
   test che ogni evento fotografico ne abbia invece una.

**L'eclissi solare è l'unica esclusione decisa sulla sicurezza e non sulla vista.** La riga che
l'app già stampa su di essa è un avvertimento — non guardare mai il sole senza un filtro adatto —
e «porta la macchina fotografica» accanto a un avvertimento si legge come un permesso. Non lo è:
un obiettivo puntato al sole senza filtro solare distrugge il sensore dietro, e attraverso un
mirino ottico distrugge l'occhio dietro quello. Il marchio serve a mandare qualcuno fuori con una
macchina, quindi l'unico evento in cui questo richiede attrezzatura che l'app non può verificare
non lo prende.

Il disegno è nostro (`ic_photographic.xml`), nella stessa mano di `ic_verdict_*` e `ic_range_*`:
tratti da 2.4 su 24, capi tondi, ~1.8 dp ai 18 a cui la riga lo mostra. Non un'icona Material,
per due motivi che valgono entrambi: il progetto non porta `material-icons-extended` e non
aggiunge un megabyte di glifi per un marchio, e ogni pezzo di grafica di servizio che quest'app
disegna da sé vive già in quella cartella in una mano sola.

### I due pianeti, e la decisione riaperta

`PLANNING.md` aveva messo a verbale alla Fase 19: «pianeti e congiunzioni = un progetto a sé (una
VSOP87 troncata e i suoi test)». Vero della **categoria**, falso di questi due, ed è l'argomento
con cui il committente l'ha riaperta: Venere e Giove sono gli unici punti di luce che un passante
distingue senza che glielo spieghi nessuno, sono la risposta a «cos'è quella stella luminosa», e
per arrivarci a qualche primo d'arco servono elementi orbitali, non una serie per pianeta. Gli
altri sei restano fuori per il motivo che non è cambiato: Marte chiede del suo colore, Mercurio
non è mai cielo scuro, e Saturno, Urano e Nettuno sono roba da telescopio, che §3.2 mette fuori.

Il modello sono **elementi kepleriani con derive secolari** — la tabella JPL/Caltech degli
elementi approssimati dei pianeti maggiori, valida 1800-2050 — risolti attraverso l'equazione di
Keplero, riferiti all'eclittica media J2000, precessati all'eclittica della data, corretti per il
tempo-luce una volta e poi passati alle stesse primitive di obliquità, altezza e azimut che usa
ogni altro corpo del modulo.

**Buono a qualche primo d'arco**, che è la taglia giusta per le due domande che l'app fa: se un
pianeta è su e abbastanza alto da essere la cosa che qualcuno sta indicando, e quanto dista dalla
luna quando si incrociano — una separazione che questa app stampa in gradi interi e mai in primi.
Non è un'effemeride: niente occultazioni, niente transiti, e niente qui va reso al secondo.

### Misurato, non affermato — e la catena conta

Una tabella di coefficienti che nessuno ha verificato è una diceria (la regola a cui è stato
tenuto `EclipseEngine`). Ma anche una riga di effemeride **ricordata** è una diceria, quindi la
verifica è una catena di invarianti e non un elenco di date:

1. Gli elementi della **Terra** rispondono alla longitudine solare di questo stesso modulo, che è
   a sua volta misurata contro l'alba di Open-Meteo a trenta secondi. Il Sole visto da qui **è**
   la longitudine eliocentrica della Terra girata di mezzo giro, quindi quell'unica asserzione
   copre elementi, Keplero, la precessione e il cambio di sistema in un colpo solo. Misurato:
   **meno di tre primi** su sette date fra il 2000 e il 2030.
2. I pianeti rispondono agli invarianti che una tabella sbagliata non può soddisfare: la massima
   elongazione di Venere (che non lascia mai il crepuscolo, ed è il motivo per cui è solo stella
   della sera o del mattino) fra 44° e 48°; Giove che arriva davvero all'opposizione; i due
   periodi **sinodici** entro tre giorni dai pubblicati; i due periodi siderali; e l'intervallo di
   distanza dal Sole che ciascuna orbita consente.

Due asserzioni sono state **riscritte dopo averle viste fallire per il motivo giusto**, ed è il
tipo di cosa che vale più del test:

- Il periodo sinodico di Venere è uscito **292,28 giorni**. Non è un difetto del modello: è
  583,92 ÷ 2, perché Venere passa davanti al Sole **due volte** per ciclo — congiunzione inferiore
  e superiore — e contare ogni minimo di elongazione misura mezzo periodo con grande sicurezza.
  Le due metà si distinguono da quanto è lontana.
- Quello di Giove è uscito **381,6** contro 398,88. Anche qui il rilevatore: la separazione è un
  angolo di cerchio massimo, quindi il vagare in latitudine eclittica ci mette dentro minimi
  locali. Passato alla definizione vera — l'attraversamento dello zero della differenza segnata in
  ascensione retta, con i tripli passaggi della retrogradazione deduplicati a cento giorni.

Una conferma indipendente arrivata gratis dalle misure: le finestre di Venere fanno passare il
pianeta da stella della sera a stella del mattino fra fine ottobre e novembre 2026, che è
esattamente attorno alla congiunzione inferiore del 26 ottobre 2026 (la precedente più 583,92
giorni). Nessun test la chiede; è il modello che si comporta come il sistema solare.

### I nove eventi

- **Luna piena al crepuscolo** (`moon.full_at_dusk`). L'intersezione: disco già abbastanza tondo
  E sorgere dentro il crepuscolo. Misurato a Milano dal 20 settembre 2026: **25 settembre, 18:35**,
  cioè la sera **prima** della luna piena (26 settembre 18:49) — che è spesso la migliore, ed è il
  motivo per cui la ricerca è una finestra e non l'istante della geometria.
- **Luce cinerea**, sera e mattina (`earthshine.pm/am`). Falce fra l'1% e il 18%: sotto è ancora
  dentro il bagliore del Sole, sopra il bordo illuminato annega la luce di cenere, che è tutta la
  vista. Misurato: **12, 13 e 14 ottobre 2026** a Milano, con la finestra che cresce da 22 a 64
  minuti mentre la luna tramonta più tardi.
- **Venere sera / Venere mattina / Giove stanotte**. Soglie di altezza (5° per Venere, 10° per
  Giove) perché un pianeta a tre gradi è dietro le case, e dire «c'è Venere» a qualcuno in una via
  sarebbe inventare una vista. Deliberatamente **non** `needsDarkness`: Giove a magnitudine −2 è
  ovvio in un cielo di periferia e Venere è ovvio di giorno, quindi una luna piena non li rovina —
  e dire che lo facesse sarebbe prendere in prestito una regola delle cose deboli per le due più
  luminose.
- **Tre congiunzioni**. La seconda metà della definizione non è un vezzo: la luna passa accanto a
  Venere ogni mese e **circa la metà** di quelle volte capita con tutti e due dietro il Sole, dove
  l'evento è reale, calcolabile e invisibile — e una riga che lo annunciasse manderebbe qualcuno
  fuori a guardare la luce del giorno. Quindi un candidato torna solo quando esiste un tratto di
  notte o crepuscolo con tutti e due davvero su. Misurato da Milano: luna-Giove a **0,2°** il 6
  ottobre 2026, luna-Venere a 1,0° il 7 novembre, e Venere-Giove a **0,6° il 10 novembre 2028** —
  che è la distanza fra «ogni mese» e «vale la pena segnarsela».

Nessuna icona nuova da importare, e non è una scorciatoia: **un pianeta a occhio nudo è un punto
luminoso**, quindi `star` — già spedita — è il disegno onesto. Meteocons non ha pianeti e
inventarne uno sarebbe un dischetto che nessuno vede.

### Le due proposte rimaste

- **Il tratto più sereno** (`SkyVerdictEngine.clearStretch`). Il verdetto è una media su otto-dieci
  ore, che è il numero giusto per una parola sola e un pessimo riassunto di una notte: un cielo
  sereno fino all'una e chiuso dopo esce «così così», e la metà buona — che l'app ha, ora per ora,
  e ha già scaricato — veniva buttata. Adesso la card la nomina. Derivato e non inventato: una
  corsa di ore consecutive che passerebbero ognuna da sola. `null` quasi sempre, apposta — quando
  nessuna ora passa, quando la corsa è tutta la finestra (il verdetto l'ha già detto), e quando sta
  sotto l'ora, perché «sereno dalle 02:10 alle 02:40» è una promessa che questa previsione non può
  mantenere.
- **La ricerca nel catalogo**. Sessanta voci in sei gruppi erano uno scroll per chi cercava
  «Perseidi» per nome. Filtra sulle due stringhe che la riga **già stampa** — nome e riga di
  spiegazione — senza accenti e senza maiuscole, e mai sull'id puntato, che su questo schermo non
  compare e non comparirà (VISION §5.3). Non un `SearchBar`: dentro un bottom sheet che già tiene
  una lista sarebbe una seconda superficie che scorre sopra la prima.

### Come è stato verificato

Ogni numero di questa voce è uscito da un test usa-e-getta poi rimosso, non da una stima: la
tabella delle finestre di luce cinerea, le congiunzioni con la loro separazione e la loro finestra
di visibilità, le finestre di Venere e Giove nell'arco di duecento giorni.

I test nuovi verificano **quel che il codice dichiara**, non il suo output: la luna piena al
crepuscolo è davvero quasi piena e sorge davvero dentro il crepuscolo, al minuto del sorgere che
la riga accanto stampa; la luce cinerea capita solo su una falce sottile e la finestra sta dentro
il crepuscolo; Venere non è mai stella della sera e del mattino lo stesso giorno, e in 400 giorni è
entrambe; una congiunzione è chiusa (un giorno prima e uno dopo sono più larghi) e tutti e due i
corpi sono davvero su, col Sole giù, dentro la sua finestra.

I test della guida non hanno avuto bisogno di modifiche e sono passati al primo colpo sulle nove
pagine nuove, il che è il loro scopo: iterano il catalogo, quindi «ogni evento ha una pagina»,
«ogni pagina è due paragrafi veri», «ogni pagina è davvero tradotta» e «nessuna pagina dice un id
a voce alta» valgono anche per quel che non esisteva quando sono stati scritti.

Dalla rilettura avversariale del proprio diff sono usciti due punti, e nessuno dei due sarebbe
stato trovato da un test:

- La riga di una congiunzione prendeva la separazione da una **seconda ricerca** («la prossima
  congiunzione da oggi») mentre la sua data veniva da `SkyUpcoming`, che scavalca un evento
  finito. Due domande, due risposte, una riga sola: è lo stesso difetto per cui `SkyUpcoming`
  esiste, alla terza occorrenza. Adesso la separazione si misura **sull'istante della riga**, dove
  di risposte ce n'è una.
- La camminata aperiodica chiamava la ricerca diretta invece dell'almanacco: tre anni a passi di
  sei ore con una posizione della Luna a ogni passo, a ogni ricostruzione della schermata, nel
  giorno dopo una congiunzione. Passa dal memo, chiesta per giorno locale — che è esatto, perché
  due congiunzioni della stessa coppia non cadono mai lo stesso giorno.

Una sbadataggine da registrare perché costa tempo a chiunque la rifaccia: le trentasei stringhe
nuove sono state generate con un `unicode_escape` di Python, che si è mangiato i `\'` degli
apostrofi italiani e ha trasformato i `\n\n` dei capoversi in a capo veri. AAPT lo segnala come
«Invalid unicode escape sequence», che non è la causa. Riparate e ricontrollate contando apostrofi
nudi e capoversi.

`./gradlew test :app:testDebugUnitTest :app:lintDebug` verdi, **1639 test**, lint a zero errori.

## L'aiuto in-app, riletto contro quel che l'app fa davvero (committente, 20 set 2026)

Chiesto di verificare che la guida fosse aggiornata. Sette punti, e **due non venivano dalle Fasi
27 e 28**: erano già lì.

### Quel che era rimasto indietro sul Cielo

- **«Stanotte»** diceva «le ore in cui il cielo è davvero scuro e la luna è fuori dai piedi», che
  fino alla Fase 27 era una bugia e adesso è vero. Ma la card nel frattempo ha imparato altre tre
  cose che la guida non nominava: quel che la luna si è presa, lo stato in cui non c'è finestra
  affatto, e il tratto più sereno della Fase 28.
- **«In arrivo»** descriveva la lista a uno strato. Ne ha due dalla Fase 27, tiene i `∅` con la
  loro ragione, stampa anno e ore sulle righe lontane e fonde due sciami di una notte: la parte
  più cambiata della schermata, e la guida non ne diceva niente.
- **«Aggiungi un momento»** aveva il conto e i gruppi già corretti (aggiornati nella Fase 28
  stessa) e non nominava **il campo di ricerca**, che è nuovo.
- **Il marchio della macchina fotografica non era spiegato da nessuna parte.** È il difetto più
  serio dei quattro, perché l'ora dorata della sera è una delle quattro sottoscrizioni
  predefinite: un lettore lo incontra **su un'installazione appena fatta**, e un marchio che
  nessuno ha spiegato è un marchio di cui nessuno si fida. Ha un blocco suo, subito dopo l'esempio
  di riga, e dice anche perché l'eclissi di Sole non ce l'ha.

### I due che erano già lì, e non li aveva messi la Fase 28

- **Il widget «In parole» non esisteva nella guida.** VISION §5.9 dice «cinque card», il manifest
  lo chiama in un commento «the fifth, and the one with nothing drawn on it», la schermata delle
  impostazioni lo offre — e il capitolo dei widget ne descriveva quattro. Il widget del 19
  settembre non è mai entrato nell'aiuto. Adesso c'è, con la frase che lo distingue davvero (il
  disegno è la temperatura, e il nome del luogo è stampato per intero).
- **La riga dell'arcobaleno su Oggi non era nominata.** `TimelineKind.RAINBOW` è nella linea del
  tempo con il suo azimut, ed è una delle righe più caratteristiche dell'app — l'unica di quella
  schermata che arriva con una direzione. La guida elencava alba, ore dorate, buio, luna e pioggia,
  e si fermava lì.
- E **due impostazioni non erano elencate**: il carattere e l'animazione delle icone, mentre nella
  stessa frase erano nominate la palette e i due disegni delle icone.

### Una chiave che mentiva da due giri

`guide_widgets_three_title` conteneva già «Sono quattro»: il testo era stato aggiornato quando il
quarto widget è arrivato, la chiave no. Con il quinto sarebbe mentita una terza volta, quindi è
diventata `guide_widgets_all_title` (e `_body`). Non è un dettaglio di stile: una chiave che dice
un numero è una chiave che invecchia, e chi la rilegge fra sei mesi crede al nome.

### Come è stato verificato

Non rileggendo la guida e annuendo, ma confrontando ogni affermazione verificabile con la sua
sorgente: i widget contro i `receiver` del manifest e le etichette in `strings.xml`, le voci delle
impostazioni contro le `settings_*` che la schermata risolve davvero, la linea del tempo contro
`TimelineKind`, il conto del catalogo contro `SkyJobCatalog.all`. È così che sono saltati fuori
l'arcobaleno e «In parole», che nessuna rilettura del solo testo avrebbe trovato.

`MomentSample` non è stato toccato e non ne aveva bisogno: costruisce a mano una riga di `sun.rise`,
che non è fotografica e non ha una direzione da stampare, quindi l'esempio resta vero.

`./gradlew test :app:testDebugUnitTest :app:lintDebug` verdi, 1639 test, lint a zero errori.

## Fase 28b — «Foto» non trovava niente, e il marchio non c'era dove si sceglie (committente, 20 set 2026)

Chiesto se «luna piena al crepuscolo» non dovesse dire nella descrizione breve che è un evento
fotografabile, così che un fotografo amatoriale lo trovi senza aprire la pagina, e così che una
ricerca per «foto» lo peschi. Chiesto esplicitamente di rispondere solo se d'accordo.

**L'obiettivo era giusto e ha scoperto due buchi della Fase 28, ma il meccanismo proposto no.**

### I due buchi

- **Il marchio non era sulla riga del catalogo.** `SkyHeadline` era finito su `MomentRow` e su
  `EventRow` e non sulla riga della tendina «Aggiungi un momento» — cioè sull'unica schermata dove
  uno **sfoglia per scegliere**. Un marchio che compare solo dopo che la riga è già sottoscritta
  arriva dopo la decisione che doveva aiutare. Adesso c'è.
- **La ricerca non sapeva niente del flag.** `matchesQuery` confrontava nome e spiegazione, quindi
  cercare «foto» non restituiva niente mentre nove righe portavano la macchina fotografica. Una
  ricerca e un flag costruiti nella stessa fase e mai presentati.

### Perché non la parola nella descrizione

La riga breve del catalogo esiste per dire **che cos'è** una cosa. «La sera in cui la luna piena
sorge mentre il cielo è ancora colorato» è una definizione; «, da fotografare» attaccato in fondo è
prosa peggiore che ripete il glifo seduto sulla stessa riga.

E soprattutto: la parola diventerebbe una **seconda verità** per qualcosa che `SkyJob.photographic`
già sa, libera di divergerne la prima volta che qualcuno tocca una delle nove stringhe. È
esattamente la ragione per cui le righe «quando capita» della guida si leggono dal job invece di
essere scritte due volte.

Quindi la ricerca interroga il flag. `sky_catalog_search_photo_terms` è una lista di parole per
lingua («foto fotografia fotografare macchina obiettivo scatto…»), e un termine risponde **per
prefisso da tre lettere in su**: «fot» li trova, «fo» no. La soglia guarda i TERMINI e non il
testo — «m» continua a pescare la riga perché sta dentro «mentre», che è la regola della
sottostringa che fa il suo lavoro.

### Come è stato verificato

`matchesSearch` è stata tirata fuori dal composable come funzione pura apposta per poterla provare:
`SkySearchTest` fissa che «foto» arriva solo a una riga marcata, che la prosa non porta la parola,
che un prefisso di due lettere non tira dentro tutto, e che accenti e maiuscole non contano.

Due test sono falliti al primo giro e solo **uno** era un difetto: una query di soli spazi non
veniva ripulita, e la funzione la trattava come una sottostringa da cercare (in pratica non ci
arrivava mai, perché il chiamante filtra già su `isBlank`, ma il contratto è il contratto).
L'altro era il test a sbagliare: asseriva che «m» non trovasse una riga marcata, mentre «m» sta
dentro «mentre» e trovarla è il comportamento giusto. Corretto il test, non il codice.

### Nota sulla sezione «In arrivo», chiesta nella stessa occasione

Il criterio del secondo strato, messo a verbale perché non era scritto da nessuna parte in
italiano: sono **solo i job ANNUALI del catalogo** (le quattro stagioni, perielio e afelio,
tramonto più presto e alba più tardi, le due notti bianche, la luna piena più vicina, i tredici
sciami: ventiquattro in tutto), non sottoscritti, risolti alla prossima occorrenza, **i più vicini
per data**, a riempire fino a sei righe con un pavimento di tre. Più la prossima luna piena, che
c'è per tutti.

La conseguenza da sapere: **eclissi, congiunzioni, luce cinerea, Venere e Giove non compaiono mai
se non li segui.** Per gli aperiodici è voluto e costoso da cambiare (una ricerca di congiunzioni
cammina tre anni: farla per chi non ha chiesto niente sarebbe batteria spesa per nessuno); per i
giornalieri è giusto, perché il loro posto è «I prossimi momenti» e non il calendario. Resta che
una Venere-Giove a 0,6° è invisibile a chi non sapeva di doverla cercare, ed è un candidato onesto
per un giro futuro.

`./gradlew test :app:testDebugUnitTest :app:lintDebug` verdi, **1649 test**, lint a zero errori.

## I testi: la promessa, il README, e il marchio della fotocamera (committente, 21 set 2026)

**La tagline.** «Il meteo che ti dice cosa farne» era §1.2 detta in sei parole: giusta, e
al posto sbagliato. Sulla prima schermata il lettore non ha ancora visto niente, e una frase
che descrive il *modo* gli chiede di fidarsi di una compressione che si scioglie solo dopo
l'uso. Scelta fra quattro, la versione concreta che tiene la cadenza di quella vecchia:
**«Il meteo che ti dice se conviene uscire, e quando.»** («Weather that tells you whether it
is worth going out, and when.») Nei due `strings.xml`, in VISION §2.3 e nel sottotitolo del
README. La regola §1.2 resta dov'è sempre stata, nella griglia dei dettagli e nella guida:
è una regola di prodotto, non uno slogan.

**Il README non nomina più nessuno, nemmeno in astratto.** Tolti i paragoni generici
(«Most weather apps answer "how many degrees"», «three things it does that a weather app
normally does not», «the widget nobody else ships»), la sezione «Where it comes from» e ogni
menzione del repo a monte, che restava anche nella tabella dei documenti. Una pagina che si
definisce per differenza chiede al lettore di conoscere il termine di paragone; adesso apre
su quel che l'app risponde e mette in fila quel che c'è oltre le previsioni.

**E il marchio della macchina fotografica ci entra.** Il README non ne diceva niente, né dei
nove momenti che lo portano né della finestra dell'arcobaleno, e la riga del catalogo era
ferma a «32 moments» contro i sessanta in sette gruppi che l'app dichiara. Aggiunto dentro i
punti che c'erano già, non come voce nuova: una coda alla frase del catalogo nel punto Cielo
(i nove, la direzione in cui guardare, e l'eclissi di Sole che di proposito non lo porta
perché va guardata col filtro) e una parentesi nella frase della timeline nel punto Oggi.

Due cose che **non** si fanno, ed è la ragione per cui le frasi stanno dove stanno:

- **Niente voce a sé per la fotografia.** Un punto elenco tutto suo trasformerebbe il marchio
  nella promessa di un pianificatore fotografico, che quest'app non è: niente bussola, niente
  realtà aumentata, niente tempi di posa. È la misura che VISION §3.2 tiene sull'astronomia.
- **Mai «ti dice quando ci sarà un arcobaleno».** `RainbowWindow` calcola la finestra in cui
  il cielo è disposto per farne uno, e il suo commento dice «never a promise that there will
  be a rainbow». Il README dice «the windows where the geometry and the forecast line up for
  a rainbow»: la pagina che elenca la regola dell'onestà non è il posto dove romperla.

Corrette nella stessa passata due cose diventate false: il paragrafo della roadmap dava la
Fase 11 come futura mentre l'elenco delle funzioni la descrive al presente, e i numeri dei
test erano fermi a 775 su quattro moduli (oggi 946, contati per modulo e non per compito:
`:app`, `:core:data` e `:core:sync` girano la stessa suite in debug e in release, che è il
motivo per cui la riga finale di Gradle ne annuncia 1649).

## Il widget «In parole»: la colonna che si era allineata a destra (committente, 21 set 2026)

Segnalato con due schermate della stessa card 4×1, quella con la frase: «sembra che testo e
temperatura siano allineati a destra e non a sinistra. Con Manchester ad esempio si vede che
l'allineamento è a destra mentre con Cavenago di Brianza che è lungo non si nota». È esattamente
quello che succedeva, e la seconda metà della frase è anche la ragione per cui era passato: la
colonna di sinistra è larga `textLeadingColumn`, 168 dp sulla card di riferimento, misurati su
«⌖ Cavenago di Brianza». Un nome che la riempie non lascia vedere niente; «Manchester» sono
~90 dp, e gli altri ~78 restavano vuoti a sinistra col nome e il numero appoggiati al bordo
interno della colonna, in mezzo alla card.

**La causa.** Il `Box` di Glance non è quello di Compose: non esiste un `Modifier.align` per
figlio, quindi `contentAlignment` vale per TUTTI i figli. Il `BottomEnd` era scritto per il glifo
del meteo, che in questa forma sta in fondo alla colonna del nome; la colonna delle parole, che
non aveva una larghezza propria, se lo prendeva anche lei. Le altre tre forme non lo mostravano
perché lì la colonna ha già `fillMaxSize` (la forma stretta) o non sta dentro un Box (stack e
pannello) — e il `TallContent` di «Colpo d'occhio», che risolve lo stesso problema, scrive
`fillMaxSize()` e `horizontalAlignment = Alignment.Start` per esteso da sempre. Anche l'anteprima
del selettore (`widget_text_preview.xml`) disegna il layout giusto: la card in esecuzione
contraddiceva la propria anteprima, che è il modo più chiaro di dire che nessuno l'aveva voluto.

**La correzione è una larghezza**: `fillMaxWidth()` sulla colonna delle parole, con
l'allineamento scritto per esteso perché si legga a colpo d'occhio che non è quello del Box. Con
essa rientra un secondo difetto che nessuno aveva ancora visto perché l'icona è spenta di
default: `textRowIconSize` dimensiona il glifo su quello che il numero più largo lascia libero
**partendo dal bordo interno**, quindi con un nome corto e l'icona accesa il disegno finiva
esattamente sopra la temperatura.

### Il bordo che la card regalava anche dove non disegnava niente

Controllando il resto del file, come chiesto nella stessa occasione, ne è uscito un secondo
difetto indipendente. Il bordo destro passa da 14 dp a 4 quando è il glifo a incontrarlo, e ogni
testo che arriva fin lì restituisce i 10 dp di differenza: è la promessa «non si muove niente»
scritta come un numero solo (`TextIconEdgeGive`). Solo che le due metà erano decise da due
condizioni diverse — la card regalava il bordo sull'interruttore del lettore, i testi
restituivano solo dove `textIconSize` aveva davvero prodotto un disegno. E quella funzione
risponde 0 dp su ogni misura troppo piccola per il glifo: lo stack di due celle a qualunque scala
del carattere, il pannello di riferimento a scala 1,3, la card stretta a una riga a 1,3. Su
quelle, esattamente, accendere l'icona spostava ogni riga 10 dp verso un angolo di raggio 24 e
non ci disegnava niente.

Adesso le due metà sono una condizione sola letta due volte (`textCardPaddingEnd` e
`textEdgeGive`), e due test le tengono insieme: la somma fa `WidgetCardPadding` su ogni forma e
da entrambe le parti dell'interruttore, e le due misure che chiedono il glifo senza averne lo
spazio scrivono comunque a 14. Nello stesso passaggio la riga del luogo entra fra i testi che
pagano il resto: sulle tre forme col glifo al bordo era l'unica a non farlo, e su stack e
pannello è l'occhiello a tutta larghezza, cioè proprio la riga che quel bordo lo raggiunge — un
nome tagliato a 4 dp dall'angolo, 10 più in là della frase sotto di lui.

`./gradlew test :app:testDebugUnitTest` e `:app:lintDebug` verdi, **1653 test**, lint a zero
errori.

## Il cielo sul widget: una tabella sola per le icone (committente, 21 set 2026)

Segnalato da una schermata home: «l'icona dell'evento "Luna piena al crepuscolo" non sembra
corretta ed è diversa da quella che compare nell'app». Vero, e il modo in cui è successo conta
più del singolo disegno.

La schermata Cielo (`SkyScreen.jobIcon`) e il widget «Momenti del cielo»
(`SkyWidget.skyJobIconRes`) avevano ciascuno il proprio `when` sugli id dei job. Il catalogo è
cresciuto due volte — Fase 19 (le eclissi, i quattro quarti di luna, tramonto più presto e alba
più tardi, perielio e afelio) e Fase 28 (Venere, Giove, le tre congiunzioni, la luce cinerea, la
luna piena al crepuscolo, la Via Lattea, le notti bianche, la luce zodiacale) — e solo la tabella
della schermata è stata tenuta al passo. **Venticinque id su quarantasette** cadevano nell'`else`
del widget, che è il disegno dello sciame meteorico: la stessa sottoscrizione mostrava due
immagini diverse sulla home e dentro l'app.

Ed è passato per una ragione precisa, che vale la pena mettere a verbale: **un id non elencato è
un `when` perfettamente legale.** Non c'è niente che diventi rosso, il compilatore è contento, e
la sola prova possibile era guardare la card.

**La correzione è strutturale, non una riga.** La politica — quale disegno tocca a quale momento
— sta adesso in `ChiaroIcons.skyJobLineRes`, cioè nell'unico oggetto che in questo repo decide
che disegno prende una cosa, e la schermata e il widget la leggono entrambi: `skyJob` per un
`ImageVector`, `skyJobRes` per il res id che serve a Glance. `SkyMomentIconTest` cammina
`SkyJobCatalog.all` e fallisce se un job ci finisce senza icona: l'unico arm che può prendere il
`falling-stars` sono i tredici sciami, che è il disegno di cui portano il nome. Un momento nuovo
va adesso aggiunto in due posti, e il secondo è il test.

**Nella stessa passata, la luna di oggi.** Il widget disegnava una luna piena su «La luna oggi»
qualunque fosse la fase, mentre la schermata ha sempre disegnato quella vera (`Moment.moonPhase`).
`NextMoment` porta adesso la fase, dallo stesso classificatore e con la stessa regola (solo il
job del giorno, null su tutti gli altri), e il commento del widget — «The moon's day-moment gets
its real phase» — smette di essere falso.

### La seconda domanda: «L'arco del giorno» e gli eventi seguiti

«Nel widget "L'arco del giorno" non compare l'evento personalizzato "Luna piena al crepuscolo": è
corretto così?» **Sì, ed è di proposito.** L'agenda dell'arco è la giornata di luce del posto —
`TodayStateBuilder.agenda` sulle prossime 24 ore (`AgendaReach`), filtrata dai tre interruttori
del lettore — non l'elenco dei momenti seguiti. La card che porta le sottoscrizioni è «Momenti
del cielo», ed è la ragione per cui quella esiste. Quell'evento poi cadeva il 25 settembre,
quattro giorni oltre la portata dell'agenda: nessun layout lo avrebbe mostrato.

Quello che invece non andava, ed è stato corretto, è **il verdetto**. `jobIdsFor` dice quali job
del Cielo «nominano lo stesso momento» di una riga dell'agenda, e per il sorgere della luna
elencava solo `moon.rise`. Ma `SkySights.nextFullMoonAtDusk` **restituisce un sorgere di luna** —
quello della sera in cui la luna piena viene su dentro il crepuscolo — quindi i due sono lo
stesso istante, calcolato dallo stesso motore. Il lettore che seguiva proprio quel momento
trovava la riga nuda nella sola sera di cui parlava. Adesso il verdetto ci arriva; chi segue
entrambi i momenti vede quello che la lista incontra per primo, e sono due risposte oneste alla
stessa domanda.

`./gradlew test :app:testDebugUnitTest` e `:app:lintDebug` verdi, **1661 test**, lint a zero
errori.

## La 1.0.0: le schermate nel README, e una review prima del tag (committente, 21 set 2026)

Tre richieste in una: mettere nel README le schermate appena aggiunte in `docs/screenshots`,
rileggere l'app prima di pubblicare — «soprattutto nelle stringhe e nei glifi e
l'onboarding» — e poi portare la release il più avanti possibile.

### Le schermate

Dieci file, `Screenshot_20260921_*.jpg`, 1080x2340, con lo spazio nel nome (quelle dello
schermo di casa si chiamavano `... One UI Home.jpg`): rinominati con `git mv` in nomi che
dicono cosa mostrano, perché un `src=` con uno spazio dentro è una riga che si rompe al
primo che la tocca. `today-now`, `today-week`, `today-details`, `sky-tonight`, `sky-ahead`,
`alerts`, `settings`, `guide`, `widgets-home`, `widget-day-arc`.

Due posti nel README, non uno. Un **trittico** dentro il blocco centrato dell'intestazione,
sotto i badge: Oggi, Cielo e l'arco del giorno sullo schermo di casa, che è la gamma del
prodotto in tre immagini prima di quattrocento righe di prosa. E una sezione
**`## Screenshots`** fra «What Chiaro is» e «Features», tre tabelle da tre con una didascalia
per immagine: la lettura di Oggi (il cielo, la settimana, la griglia dei numeri), il Cielo
con la guida accanto, e la riga di Avvisi, Impostazioni e lo schermo di casa. Le didascalie
stanno in `<sub>`, sotto la loro colonna, e ogni immagine porta un `alt` che descrive la
schermata e non il file.

La sezione apre dicendo che le schermate sono in italiano: l'app spedisce in due lingue e
una pagina in inglese che mostra dieci schermate in italiano senza dirlo lascia il lettore a
indovinare. La regola della punteggiatura del README (niente lineette) vale anche qui, ed è
stata verificata sul file intero dopo ogni inserimento.

**Il Diario non c'è, ed è una scelta del committente**: senza dati passati la schermata è
vuota, e una schermata vuota in vetrina non dice cosa fa il Diario, dice che non fa niente.
È la regola §1.1 applicata al README invece che a un layout: una sezione senza dati non si
disegna. La sezione non promette le quattro schermate, quindi non mente nominandone tre; il
punto elenco del Diario resta dov'è, nelle funzioni, a raccontarlo a parole.

### La review

**Le stringhe.** 1016 in inglese contro 1013 in italiano: le tre di scarto sono le sole
`translatable="false"` (`app_name`, e le due note dei crediti), quindi la parità è piena.
Zero orfane dall'altra parte. Gli argomenti di formato confrontati uno a uno sui due file
con un matcher stretto — il primo giro, scritto largo, dava 22 falsi positivi leggendo
«70% chance» come una specifica `%c`, che è esattamente l'errore che il flag spazio fa fare
— e sono **zero** divergenze reali: nessun `%1$s` perso, nessuna quantità in più o in meno.
Zero doppi spazi, zero spazi di bordo. Nessun accento mancante e nessun apostrofo tipografico
nell'italiano. Nessun `TODO`, nessun segnaposto, nessun testo di prova.

Va detto che questo giro **non ha trovato niente che i test non guardassero già**:
`StringsParityTest` controlla parità, nomi doppi e argomenti di formato, ed è verde. La
review ha confermato il guardiano dall'esterno, il che è il massimo che potesse dare.

**I glifi.** La tabella è una sola e sta in `ChiaroIcons.skyJobLineRes`, con
`SkyMomentIconTest` che cammina `SkyJobCatalog` e fallisce su un job senza disegno: è la
correzione del 21 set, e regge. `MeteoconsSetsTest` e `ComposedIconsTest` verdi, quindi
`shipped_icons.py` e il compositore non sono andati alla deriva. Controllate a occhio sulle
schermate le scelte che sembrano strane e non lo sono: la stella dell'ora blu e dei pianeti
(la famiglia non ha pianeti, e un punto di luce è quel che si vede davvero), l'orizzonte
degli equinozi e dei solstizi, il sorgere di luna prestato alla luna piena al crepuscolo.

Una cosa che sembrava un difetto e non lo è: sulla schermata Cielo la luna di stanotte è
al 78% e «La luna di oggi» al 73%. Sono due istanti diversi — adesso, e la finestra di buio
sei ore più tardi — su una luna che cresce verso il plenilunio del 26. Ognuna porta il suo
contesto scritto sopra. È precisione, non incoerenza.

**L'onboarding.** Due passi e nessun carosello. Il primo chiede il luogo con la frase del
perché già sullo schermo prima che il dito tocchi il pulsante; il secondo chiede le
notifiche a parole prima che il sistema le chieda col dialogo, e «Non ora» è una risposta
vera che non spende il dialogo. Le stringhe dei due passi sono piene e giuste in entrambe le
lingue. Niente da correggere.

**Lint: zero errori.** Dei 3669 avvisi, 3606 sono `UnusedResources` sui 519 disegni che il
repo porta apposta e che solo `shipped_icons.py` fa arrivare nell'APK — è la politica, non
un difetto. Dei restanti nessuno tocca la release: i cinque `ContentDescription` sono
immagini decorative del `widget_today_preview` (l'anteprima del selettore, non una schermata),
`UnsafeProtectedBroadcastReceiver` è il receiver del boot che è già `exported="false"`, e
gli undici `MissingQuantity` chiedono la categoria `many` italiana, che in CLDR scatta sui
numeri compatti (i milioni) e che Android risolve comunque su `other`, cioè sulla frase
giusta. Restano tre avvisi di dipendenze più nuove, che non si toccano alla vigilia di un tag.

### Quel che serviva davvero al tag, e che mancava

Due cose, e la review le ha trovate perché le cercava:

- **`versionName` era ancora `0.1.0`.** L'APK della 1.0.0 si sarebbe presentato come 0.1.0
  nella schermata Info e nello `User-Agent` che il data layer manda a Open-Meteo, perché
  entrambi leggono `BuildConfig.VERSION_NAME`: un posto solo da cambiare, ed era quello.
  `versionCode` resta **1**: non c'è mai stato un tag pubblicato (la prova end-to-end era su
  un tag usa e getta, poi cancellato), quindi la prima release è davvero la prima.
- **Il `## [1.0.0]` del changelog non esisteva.** `release.yml` legge la sezione col nome del
  tag e la usa come corpo della release; senza, il workflow non fallisce — scrive un warning
  e ripiega sulle note generate — ma la pagina della 1.0.0 sarebbe uscita con un elenco di
  commit al posto di quel che l'app è. `## [Unreleased]` è diventato `## [1.0.0] - 2026-09-21`,
  con sopra un `## [Unreleased]` vuoto e, in testa alla sezione, i tre paragrafi che dicono
  cos'è Chiaro e cosa c'è dentro: chi arriva dalla pagina della release non ha un README
  davanti. L'estrazione è stata provata girando l'`awk` del workflow sul file vero: 1063
  righe, 83 KB, sotto il limite di 125 000 caratteri che GitHub mette al corpo di una release.

Nella stessa passata il README ha smesso di dire tre cose diventate false: il badge di stato
(«v1.0.0 in preparation», scritto a mano, che sarebbe invecchiato il giorno del tag) è
adesso il badge dinamico di `shields.io` che legge le release del repo e non può mentire in
nessuna delle due direzioni; il paragrafo della roadmap dava per mancanti l'icona e le
schermate, che ci sono (l'icona adattiva col layer monocromatico è disegnata e misurata da
settembre); e «Install» diceva «v1.0.0 is not tagged yet». Corretto anche il numero dei test,
fermo a 946 con 450 in `:app`: sono **952** con **456** in `:app`.

### Dove si ferma

Il tag no. `v1.0.0` fa partire `release.yml`, che pubblica una release vera e visibile a
tutti, e va messo su `main` dopo il merge, non su un ramo di lavoro: è la decisione del
committente, non una cosa da prendersi. Da verificare prima i quattro secret
(`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), che dal repo non si
leggono.

`./gradlew test :app:testDebugUnitTest` e `:app:lintDebug` verdi, **1661 test**, lint a zero
errori; `:app:assembleRelease` minificato verde, quindi R8 regge il bump.
