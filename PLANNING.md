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

- [x] **La finestra reale in Cielo** (committente, 22 ott 2026, qui sotto, «La finestra: l'ora
  del cielo accanto alla bussola»): nella card «Stanotte», dopo la review di Cielo, il cielo
  dell'ora sotto il dito disegnato dal painter della finestra con bussola e nomi, e il
  trascinamento orizzontale che gira la direzione; il sensore della bussola non è il primo
  passo (magnetometro a 10–20° senza calibrazione). Fatta il 22 ott (qui sotto, «Cielo: la finestra reale nella card
  Stanotte»); il sensore della bussola e lo schermo intero non si fanno.

**Proposte non decise** (dette al committente, non fatte):

- Il widget «Le prossime ore», ritirato dal picker il 13 ott e tenuto dichiarato per le home
  che lo avevano: lo stesso caso di «Momenti del cielo», andato il 22 ott (qui sotto, «Il widget
  «Momenti del cielo» va via»). Da decidere se toglierlo allo stesso modo.
- Le righe italiane delle notifiche più corte («Qualità aria» come il riquadro di Oggi, «Vento»
  senza «adesso» nei riepiloghi), da decidere sullo screenshot del telefono: solo se il riepilogo
  del mattino mostra quattro righe e non cinque (21 ott, qui sotto, «Le notifiche: la misura
  prima di spedire»).

---

## Registro dal 20 ott 2026

## La finestra: il widget del cielo al posto dell'arco (committente, 20 ott 2026)

**Da dove nasce.** Dopo la review del motore grafico, il committente sul widget dell'arco: «una
vecchia idea; non la trovo molto interessante né graficamente di qualità premium. Un'idea più
fresca, utile, con grafica accattivante, con un occhio alle modifiche del motore (stelle, pianeti,
Via Lattea)». Il giudizio condiviso: l'arco era un cruscotto, non un'immagine; un grafico
dell'altezza del sole, una riga di temperature, quattro momenti e la settimana, tre liste e un
diagramma da imparare; la curva del sole è la cosa che interessa meno a chi vive di momenti, e i
momenti stavano già nelle righe sotto; ore e settimana doppioni di «At a glance» e dell'app.
L'unica cosa da salvare: l'elenco «i momenti che segui» con i verdetti, ereditato dal widget Sky.

**La proposta, approvata su maquette.** «La finestra»: una card che è il cielo di quest'ora,
disegnato dalla stessa scena dell'eroe (gradiente, nubi, sole, luna con fase e inclinazione vere,
stelle che ci sono, Venere e Giove fra loro), con il luogo e una frase in basso (il verdetto di
stanotte con i numeri che l'hanno deciso, o il prossimo momento di luce con il suo verdetto) e i
nomi delle due o tre cose più luminose scritti dove stanno. Più alta, le righe dei momenti seguiti.
Prima di una riga di codice, sei card rese dal motore vero sulla Milano registrata (il test
`WindowWidgetMockup`, tenuto fuori dal repo, nello scratchpad della sessione), guardate e
approvate: «bellissime». Tre cose decise disegnando:

- *La finestra si gira.* Alle 21 la luna stava a sud-est, fuori dai ±41° di una finestra a sud:
  o si perdeva, o tornava sulla fascia compressa dell'eroe. Terza via: di notte, con la luna su
  (sopra 5°), la finestra guarda la luna; altrimenti l'equatore. La didascalia lo dice («Looking
  SE», `SkyText.bearingRes`). Luna e stelle in una camera sola, quella delle stelle: in un widget
  non c'è il testo dell'eroe da cui tenere una fascia, e la distanza luna-stella è quella del
  cielo, l'unico posto del prodotto in cui lo è.
- *Le parole in basso*, su uno scrim che sale dal fondo: le stelle luminose stanno in alto e le
  etichette finivano sotto la frase.
- *Senza finestra buia* (luna al 99%) il builder non dà verdetto, e la frase dice i fatti:
  «Moonlit all night · moon 99% · cloud 61%».

**Cosa è cambiato.**

1. *Il pacchetto `widget/window`* (nuovo): `WindowWidget` (receiver, widget, `WindowWidgetContent`
   in Glance: l'immagine a tutta card, la didascalia su due righe, la bussola o l'età del dato a
   destra, il chip dell'allerta in alto a sinistra, le righe dei momenti sotto con la riga della
   card Sky, `CompactRow`, resa `internal`); `WindowLayout` (quattro forme dal grant: STRIP a una
   riga, NARROW a due celle, WINDOW a tre o quattro per due, TALL da tre righe; il cielo mai meno
   di 189 dp, le righe quante ne stanno e mai più dei momenti disponibili, `WindowLayoutTest`);
   `WindowPainter` (la scena con `skyScene` sulle stelle della finestra, `StarField.visible` e
   `planets` con direzione e mezza larghezza dal rapporto del box; la luna rimessa nella camera
   delle stelle attraverso l'aritmetica della fascia con la fascia pari all'immagine; lo shader
   reso fuori schermo con `HardwareRenderer` in un `ImageReader`, come le immagini del README; se
   fallisce, il disegno piano: gradiente, stelle a punti, luna a disco, niente nubi, e
   `WindowPicture.gpu` lo dice; gli scrim e le etichette disegnati sul bitmap, 11 sp per la scala
   del font, con l'alone di §3.6; nessuna etichetta con copertura ≥ 60%, nessuna nella striscia
   della didascalia o sotto il chip); `WindowText` (le frasi, da `SkyText` e dalle stringhe nuove);
   `StarNames` (le diciotto stelle più luminose con un nome, J2000, le stringhe localizzate:
   Sirio, Procione, Arturo, Polluce, Regolo, Castore, Canopo).
2. *Il modello*: `WidgetModel.tonight` (`Tonight` del builder della scheda Cielo, letto in
   `WidgetData.load` sulle stesse sottoscrizioni dei momenti), per la frase di notte.
3. *Il registro e la configurazione*: `WidgetKind.WINDOW` al posto di `ARC`; il manifest dichiara
   `WindowWidgetReceiver` con `widget_window_info.xml` (minimo due celle per una, quattro per due
   di default) e la schermata condivisa; l'anteprima del picker `widget_window_preview.xml` su un
   cielo notturno suo (`widget_preview_window`, l'unico fondo ammesso oltre la card, e
   `WidgetPreviewTest` lo sa); nelle impostazioni la finestra non offre sfondo né opacità (il suo
   fondo è l'immagine, e un interruttore che non cambia nulla non si offre) e del gruppo «La card
   mostra» offre solo l'allerta; le note delle forme (`window_form_*`).
4. *L'arco è cancellato*: il pacchetto `widget/arc` (2 970 righe), i suoi cinque test, la sua
   schermata di configurazione, l'info, l'anteprima, le 71 stringhe per lingua (`arc_config_reset`
   sopravvive come `widget_config_reset`, perché il pulsante è di tutti), i colori dell'anteprima.
   `ChiaroWidgets` non ha più il terzo store da dimenticare e rimappare.
5. *README, guida, VISION, DESIGN*: le immagini `widgets.png` (la finestra di giorno al posto
   dell'arco) e `widget-window.png` (la finestra di notte a quattro per due, la sera della
   registrazione, e la card alta di giorno con tre momenti) al posto di `widget-day-arc.png`; il
   paragrafo dei widget nella guida (IT e EN) e la riga del README; VISION §5.9 (la finestra, e
   perché l'arco è andato via); DESIGN dove nominava l'arco.

**Decisioni e deviazioni.** *Il codice dell'arco va via, non resta nascosto*: i due provider
ritirati il 13 ottobre restano dichiarati perché il loro contenuto vive dentro altre card; qui non
vive da nessuna parte, e tremila righe per una card che il picker non offre più sono un peso che la
serie rifiuta. Un arco già su una home screen sparisce; git lo conserva. *Tre forme sulla carta e
non cinque*: la cella singola non ha spazio per un cielo, due celle di larghezza sono il minimo
(`minWidth` 110 dp). *Il rendering sul telefono è la cosa da misurare per prima*:
`HardwareRenderer` fuori da una finestra è API pubblica dalla 29 e il README lo usa in Robolectric,
ma il widget lo chiama dal processo in background; se cade, il disegno piano è onesto e povero.
*La cadenza resta quella della famiglia* (ogni sync e ogni ora): le stelle si spostano di 15° l'ora,
un quarto della finestra; l'ora dell'immagine è quella del rapporto, scritta sull'eroe e non sulla
card; una ripittura ogni quarto d'ora è la misura successiva, se dal telefono serve. *L'immagine del
README di notte dice «Updated 8 hours ago»*: la registrazione è delle 12:10 e alle 21 il widget è
vecchio; la famiglia dice l'età del dato al posto della bussola, e la foto non la nasconde (la
schermata Oggi di notte, invece, è stata tagliata sopra il chip: lì l'età è un elemento del foglio,
qui una riga della card). *La luna bassa sta bassa*: alle 21 sta a 24° e nella camera 65°–10° finisce
nella striscia della didascalia, senza etichetta; è la fisica, non un difetto. *Venere e Giove* sono
etichettati ma quella sera non erano in finestra; i nomi delle stelle neppure (Altair a sud, fuori
dalla finestra girata a sud-est): la maquette senza luna del 1° ottobre li mostra.

**Da verificare sul telefono.** La resa GPU nel processo del widget (`WindowPicture.gpu`: se il
disegno piano compare, nubi assenti e luna a disco, lo si vede); la finestra di giorno e di notte,
le etichette, la bussola; la card alta con le righe; il peso di una ripittura (un fotogramma GPU
per sync); il picker con l'anteprima e le tre card; le impostazioni della finestra (luogo, allerta,
icone, azzera).

**Come è stato verificato.** `WindowLayoutTest` (forme dal grant, righe dal cielo che resta e mai
una riga inesistente, un font più grande toglie righe, cosa porta la didascalia per forma),
`WindowTextTest` (stanotte col verdetto e i numeri, i fatti della luna senza finestra buia, il
prossimo momento con orologio e verdetto e «domani», la bussola, i nomi), `StarNamesTest` (diciotto,
dalla più luminosa, sulla sfera), `WindowPainterTest` (testDebug, pipeline GPU: di notte la finestra
si gira sulla luna e la disegna dove la mette la camera delle stelle, un disco chiaro sopra il
cielo; di giorno guarda l'equatore e non disegna stelle; il disegno piano senza shader ha il
gradiente e il disco; la card alta tiene il cielo sopra il fondo delle righe), le sei maquette e le
tre immagini del painter rese e guardate; `WidgetPreviewTest`, `WidgetPreviewSizesTest`,
`WidgetTypeTest`, `WidgetConfigChoicesTest`, `WidgetLookDefaultsTest`, `ManifestTasksTest`
aggiornati; le immagini del README rigenerate e guardate (`widgets.png`, `widget-window.png`,
`today-night.png` per la maschera dell'alfa); suite completa (715 app debug con i 15 screenshot saltati, 660 release, 360 domain, 275 data, 24 sync: meno di prima, perché i cinque test dell'arco sono andati con lui) e lint con 0 errori.

**Il tap apre l'app, non Cielo** (committente, 20 ott 2026, dal telefono, dopo la prova: «preferirei
che apra la schermata principale come avviene negli altri widget»). D'accordo: l'arco atterrava su
Cielo perché era materiale di Cielo (momenti e verdetti); l'immagine della finestra è l'eroe di Oggi,
e un tocco sul cielo porta al cielo, più grande, con i numeri sotto. `destination = null` come Now,
Today e In parole; VISION §5.9 lo dice. Nello stesso giro `WidgetData.load` costruisce lo stato di
Cielo per «stanotte» solo quando la card è la finestra: le altre non lo stampano e non devono
pagarlo a ogni ripittura.

### La Via Lattea (committente, 20 ott 2026)

**Da dove nasce.** Registrata al passo 6 come idea e rifiutata lì per non far troppo in un giro;
proposta di nuovo come prima cosa da aggiungere dopo la finestra, perché cambia una notte senza
luna nell'eroe e nel widget insieme. Il committente: «parti con il punto 1».

**Cosa è cambiato.** `StarField.galacticFrame(at, coords, facingDeg)`: il polo nord galattico e il
centro galattico (J2000, IAU 1958) come due vettori unitari nel sistema della camera delle stelle
(x a destra, y in alto, z verso dove guarda), dalla stessa aritmetica che mette una stella
(`SkyFrame`). Lo snapshot li calcola con le stelle (sole sotto −6°), la scena li porta
(`SkyScene.galactic`) con la quantità `milkyWay` = buio × (1 − copertura)³ × (1 − luce lunare)²:
alla Via Lattea serve più buio che a una stella, un velo che lascia Vega la toglie, e una mezza
luna la toglie due volte. Nello shader, sotto le stelle: la direzione del pixel nella camera
(65°–10°, `uStarCam`), la latitudine galattica dal polo e la longitudine dal centro, una
gaussiana trasversale larga 6° verso l'anticentro e 10° verso il centro, più luminosa verso il
Sagittario (0,3 + 0,7·cos^1,5), screziata da un rumore fisso al cielo (`fbm2` su longitudine e
latitudine, 0,8 + 0,4: a 0,7 + 0,6 la banda si leggeva come chiazze di nubi, visto sulla resa di luglio), più sottile verso l'orizzonte, tinta fredda (0,74, 0,80, 0,92) a un ventesimo della
luce lineare al massimo. L'eroe e la finestra la ricevono dallo stesso `skyScene`; la finestra con
la sua direzione. La guida (IT, EN), il README e DESIGN §3.9 la nominano.

**Decisioni e deviazioni.** *Un modello dichiarato, non una mappa*: il profilo lungo il piano è
una funzione della longitudine, non una misura; la fenditura da Cigno a Scorpione e le nubi vere
della banda non ci sono, e DESIGN lo scrive, come scrive che l'inquinamento luminoso non è
modellato. Una mappa di brillanza misurata (per esempio per gradi di longitudine) è il passo dopo,
se la banda disegnata risulta troppo liscia sul telefono. *Sotto le stelle*: la banda è il fondo
su cui le stelle si posano. *Un ventesimo di luce lineare*: misurato sulle rese di luglio (la
banda da Aquila a Sagittario attraversa la finestra a sud), gennaio (la Via Lattea invernale, a
est di Orione, debole per profilo) e ottobre (fuori finestra sotto la luna piena: nulla, com'è
giusto); da ritoccare sul telefono.

**Come è stato verificato.** `StarFieldTest`: i due vettori sono unitari e perpendicolari, l'altezza
del centro è quella del motore (`galacticCoreAltitude`, entro un decimo di grado), girare la camera
cambia x e z e non y. `SkySceneTest`: la quantità è 0 di giorno, 0 con cielo coperto, 0 senza
cornice; sopra 0,9 in una notte buia e sgombra; meno di un ottavo con metà cielo coperto e sotto le
stelle; quasi nulla con la luna piena. `SkyShaderPixelTest`: una cornice col piano che taglia il
canvas in verticale contro una che lo manca: il centro è più chiaro di oltre 0,01 di luminanza con
la banda, i bordi uguali (è una banda, non una tinta), e di giorno la quantità è 0; le tre notti di
Milano rese di nuovo e guardate. `SkyShaderCompileTest` compila `atan(y, x)`, `cross` e `asin`.

### Il radiante nelle notti di picco (committente, 20 ott 2026)

**Da dove nasce.** Il secondo dei punti suggeriti dopo la finestra: nella notte del picco di uno
sciame la finestra sa dove guarda, e un'etichetta «Perseidi, da qui» nel punto da cui le meteore
sembrano partire è una cosa che nessun widget meteo fa. Il committente: «implementa il radiante».

**Cosa è cambiato.** `MeteorShowerTable.MeteorShower` porta il radiante al massimo (ascensione retta e
declinazione J2000, al grado, dalla working list IMO: Quadrantidi 230° +49°, Liridi 271° +34°, Eta
Aquaridi 338° −1°, delta Aquaridi 340° −16°, alfa Capricornidi 307° −10°, Perseidi 48° +58°,
Draconidi 262° +54°, Tauridi sud 32° +9°, Orionidi 95° +16°, Tauridi nord 58° +22°, Leonidi 152°
+22°, Geminidi 112° +33°, Ursidi 217° +76°); il radiante deriva di un grado al giorno lungo lo
sciame, quindi vale nella notte del picco e non si chiede in altre. `WindowPainter.peakRadiant(at,
zone, coords)`: gli sciami del catalogo risolti per l'istante come li risolve la scheda Cielo
(`SkyUpcoming.allAt`), tenuti se la finestra della notte del picco è aperta o apre entro 14 ore
(la sera prima di un picco alle 3 è la sua notte: lo sciame è largo giorni); due sciami nella stessa
notte (delta Aquaridi e alfa Capricornidi) danno la finestra che apre prima; il radiante messo dal
motore come una stella. La finestra si gira: luna se è su; altrimenti il radiante se è sopra i 3°;
altrimenti l'equatore. Nel disegno, un anello sottile di 5 dp nel punto e l'etichetta «<sciame>, da
qui» (`window_label_radiant`, con il nome breve di `SkyText`), con le regole delle altre: niente
con copertura ≥ 60%, niente sulla didascalia o sotto il chip. Solo sul widget: l'eroe non scrive.

**Decisioni e deviazioni.** *Un anello e non una luce*: il radiante è un punto del cielo, non un
oggetto; disegnarci una stella sarebbe inventarla. *Nessun tasso*: la tabella non ha lo ZHR per la
ragione scritta in Fase 16b, e l'etichetta non lo promette. *La luna prima del radiante*: una luna
alta è anche quella che ammazza lo sciame, e la finestra lo mostra. *Il radiante sotto l'orizzonte
non si nomina* (Leonidi e Eta Aquaridi sorgono dopo mezzanotte: prima, niente, com'è giusto).

**Come è stato verificato.** `SeasonsAndShowersTest`: ogni radiante sulla sfera, le Perseidi a
48°/+58°, le Geminidi a due gradi da Castore, le Ursidi vicino al polo. `WindowRadiantTest`: nella
sera del picco delle Geminidi 2026 il radiante è il loro, dove il motore lo mette, su, a est; in
una notte senza picco nulla; sei ore prima del picco delle Perseidi il radiante c'è e una settimana
prima no. `WindowTextTest`: «Perseids, from here». Immagini del README identiche (il 25 settembre
non è una notte di picco). Suite completa (721 app debug con i 15 screenshot saltati, 665 release, 361 domain, 275 data, 24 sync) e lint con 0 errori.

**Forti sempre, deboli solo se seguiti; e la via del ritorno ai predefiniti** (committente, 20 ott
2026). La domanda: un'installazione nuova, che non segue nessuno sciame e si affida alla schermata
Cielo, cosa vedrebbe? Misurato sul catalogo: i predefiniti sono alba, tramonto, ora d'oro della
sera e luna di oggi, nessuno sciame; con «solo i seguiti» quell'utente non avrebbe mai visto un
radiante, neanche alle Perseidi; con «tutti» avrebbe visto «Tauridi, da qui» per cinque meteore
l'ora. La regola scelta sta in mezzo: `MeteorShower.strong`, vero per gli otto sciami con un tasso
da quindici l'ora in su nella working list IMO (Quadrantidi 110, Perseidi 100, Geminidi 150, Eta
Aquaridi 50, delta Aquaridi 25, Orionidi 20, Liridi 18, Leonidi 15), falso per i cinque da cinque
o dieci (alfa Capricornidi, Draconidi, le due Tauridi, Ursidi); un giudizio con la soglia scritta
e la fonte citata, non un numero stampato. `peakRadiant(at, zone, coords, followed)` tiene i forti
sempre e i deboli solo se il loro id è fra i momenti della card (`model.moments`, i seguiti). E la
via del ritorno: chi ha toccato la lista dei momenti seguiti trova in fondo al catalogo «Torna ai
quattro predefiniti», con la riga che dice cosa sono e che i promemoria messi sulle righe se ne
vanno; offerta solo quando la lista è diversa da quella di un'installazione nuova (altre righe, o
una riga con un promemoria suo o commentata), perché una riga che non cambia nulla non si offre;
e annullabile sul posto finché il foglio è aperto, con il file precedente rimesso intero, promemoria
compresi (`SkySubscriptionStore.replaceAll`, `SkyViewModel.setMoments`, `SkyActions.setMoments`).
Il ripristino generale delle impostazioni non la tocca: il suo testo nomina unità, aspetto,
frequenza, promemoria e interruttori, e i momenti seguiti sono contenuto del lettore, come i luoghi.
Verificato: `SeasonsAndShowersTest` (otto forti, cinque deboli), `WindowRadiantTest` (le Tauridi sud
nella loro notte: nulla senza seguirle, il radiante se seguite; le Geminidi sono forti),
`SkySubscriptionStoreTest` (il file intero ai predefiniti e indietro, una riga ignota cade),
`SkyCatalogResetTest` (la riga c'è solo con una lista toccata, il tocco manda i quattro predefiniti,
l'annulla rimette la lista com'era con il promemoria; su una lista nuova la riga non c'è).

**La metà dell'ora** (committente, 20 ott 2026). Indeciso sulla ripittura ogni quarto d'ora della
finestra, ha chiesto un parere onesto: non farla. Un risveglio suo per una card guardata di
sfuggita, che Doze rimanderebbe comunque proprio quando il telefono dorme; l'errore di un'ora è 15°
a fine ora, una spanna a braccio teso, e la manopola esiste già (chi mette il sync a 15 minuti ha
anche la finestra a 15 minuti, con la rete attaccata). Fatto invece il mezzo trucco a costo zero:
a notte piena (sole sotto −12°, `FullNightSunDeg`, quando le stelle ci sono e il gradiente ha
smesso di muoversi) la finestra calcola l'astronomia per trenta minuti avanti (`drawAt`, in
`WindowPicture.drawnAt`): stelle, pianeti, luna con fase e lembo, Via Lattea, radiante; sette gradi
avanti da fresca, sette indietro alla fine, invece di zero e quindici. Di giorno e nel crepuscolo
(fra −6° e −12°) si disegna per adesso, perché lì il cielo cambia in minuti e un sole tramontato un
quarto d'ora prima sarebbe una bugia. Le nubi restano quelle dell'ora prevista: si muove solo
l'astronomia. Verificato in `WindowPainterTest`: la notte della registrazione è disegnata per
21:30 (la luna dove il motore la mette a quell'ora, la finestra girata sul suo azimut di allora),
il giorno per adesso, il crepuscolo delle 20:00 (sole a −8,7°) per adesso con le stelle fuori.
`widget-window.png` rigenerata (la luna un poco più alta) e guardata; VISION §5.9 lo dice.

**Un numero solo per la luna** (committente, 20 ott 2026, da uno screenshot di Taipei: «Luna 68%»
sul disco e «luna 69%» nella frase). Due istanti onesti su una card sola, letti come un errore:
l'etichetta dice la fase di adesso (della metà dell'ora, da PR #86), la frase riporta il numero
della scheda «Stanotte», la fase a metà notte, quella che ha deciso il verdetto e che la schermata
Cielo stampa. Regola: un numero per card. Quando la luna è disegnata e nominata (`WindowPicture.
moonNamed`, vero solo se l'etichetta è stata davvero scritta, non nascosta dalla didascalia o dal
chip), la frase lascia il numero al disco («Bello stanotte · nuvole 3%», «Chiaro di luna tutta la
notte · nuvole 61%»); quando la luna non è in finestra, la frase tiene il numero di metà notte,
uguale a quello di Cielo. Verificato in `WindowTextTest` (le frasi senza luna) e `WindowPainterTest`
(la notte della registrazione nomina la luna, il giorno no); `widget-window.png` rigenerata.

**L'orologio vivo del luogo** (committente, 20 ott 2026, dallo screenshot di Taipei: «l'ora, non
essendo quella del fuso del dispositivo, si potrebbe indicare»). Sì, ma non dipinta: un'ora nella
bitmap è vecchia di un minuto subito e di un'ora a fine ciclo, e lo schermo non mente. La via
onesta è la `TextClock` della piattaforma, che il launcher aggiorna da solo ogni minuto nel fuso
che le si dice, senza ripitture: sta dentro la card Glance come `AndroidRemoteViews`
(`layout/widget_window_clock.xml`, il fuso e l'inchiostro messi dalla card, dimensione Caption,
il punto mediano come separatore). Accanto al nome del luogo, «Taipei · 21:18», e solo quando il
luogo tiene un'ora diversa dal telefono, confrontando gli offset e non i nomi dei fusi
(`clockDiffers`): Parigi non ha l'orologio su un telefono a Roma, e il luogo del telefono non ce
l'ha mai, perché il suo orologio è quello del launcher. Verificato in `WindowClockTest` (la card
composta come la riceve il launcher e applicata a una vista: Milano su un telefono a Roma senza
orologio, Taipei con una `TextClock` nel fuso Asia/Taipei; l'offset e non il nome). Da provare sul
launcher: una vista classica dentro una card Glance.

**Venere nel crepuscolo** (committente, 20 ott 2026: «se lo ritieni utile e un'aggiunta grafica
premium»). Sì: la stella della sera sopra il rosso del tramonto è la vista classica, e il limite
era scritto in DESIGN. I pianeti escono dal sole a −3° (`StarField.PLANETS_SUN_DEG`), pieni a −5° (a −8° il punto era ancora debole sul crepuscolo, visto sulla resa),
con un fattore loro nella scena (`SkyScene.planets`, `uPlanets`: copertura al quadrato, la luna ne
toglie un terzo dove a una stella ne toglie due); lo shader riconosce il pianeta dalla fase ferma
e lo accende con la sua quantità, mentre le stelle aspettano −10°. Lo snapshot dell'eroe calcola i
pianeti da −3° e le stelle da −6°; la finestra lo stesso, con le etichette «Venere»/«Giove» anche
nel crepuscolo. Verificato in `SkySceneTest` (nulla di giorno e a −1°, a −4° i pianeti sì e le
stelle no, pieni a −10°, nulla sotto il cielo coperto, la luna piena ne lascia più di due terzi) e
`SkyShaderPixelTest` (a −4° Venere al centro è più chiara del cielo di oltre 0,02 di luminanza, una
stella nello stesso punto non si distingue dal cielo). DESIGN §3.9 chiude il limite.

**Dal secondo screenshot di Taipei** (committente, 20 ott 2026, cinquanta minuti dopo il primo).
Tre cose. *Capella sulla luna*: Capella a quell'ora stava a nord-est, dietro le spalle di chi guarda
a sud-ovest, a 177° dall'asse della camera; la tangente di 177° è quella di −3°, e `starPlace` la
metteva a nove dp dal centro, all'altezza della luna. Le stelle disegnate passano dal filtro della
finestra prima della camera; un nome messo da solo (stelle con nome, pianeti, radiante) no. Ora la
camera rifiuta ogni direzione oltre 89° dal suo asse (`BehindCameraDeg`), e `StarFieldTest` lo
pinna a 177°, −150° e 92°. È quello che aveva rubato il posto a Fomalhaut, che a 29° dal centro
era in finestra. *Due nomi uno sull'altro*: `finish` tiene le scatole dei nomi scritti e non scrive
un nome che ne tocchi un altro, nell'ordine che è la priorità (luna, radiante, pianeti, stelle per
luminosità). *L'ora sopra la base del nome*: la vista che avvolge la `TextClock` non ha una base da
allineare, quindi la riga allinea in alto e l'orologio porta un'aria sopra calcolata dal font
(`clockPaddingTop`: la prima base di un `Text` di Glance sta al `top` del font, quella di una
TextClock senza padding al suo `ascent`, misurati sul font del sistema come fa
`widgetTextDescentEm`); `WindowClockTest` misura la card impaginata e pretende le due basi entro un
dp e mezzo. Controllato il resto del disegno: la luna al 68% a 29° a sud-ovest alle 22:26 (la metà
dell'ora), la sua falce accesa verso il basso a destra dove sta il sole sotto l'orizzonte a
nord-ovest, la frase senza il numero della luna, le righe con i verdetti, «21:56» uguale a 15:56
più sei ore.

### La finestra: il meteo dell'ora, e dietro le nuvole (committente, 20 ott 2026)

**Da dove nasce.** Due idee del committente davanti alla finestra. La prima: una card uguale che
racconti il meteo di Oggi invece del cielo, come opzione del widget e non come widget nuovo; poi
precisata a voce: a quattro per due la card resta quella del cielo e porta solo la temperatura
e lo stato dell'ora, in alto a sinistra, senza frase né massima e minima, «così il disegno rimane
protagonista». La seconda: un'opzione che disegni il cielo senza nuvole, per vedere di notte lo
stato vero del cielo anche quando il tempo lo nasconde, e di giorno il sole e il colore del
sereno, con una nota cortissima sulla card. Il parere dato: la prima è coerente e non un
doppione (l'immagine è già l'eroe di Oggi, cambiano le parole); la seconda va contro «lo schermo
non mente» e diventa onesta solo perché la accende il lettore e la card lo scrive sempre; il
valore c'è ed è la domanda della schermata Cielo, il rumore è il lettore che se ne dimentica,
tenuto a bada dalla nota e dalle parole che restano quelle del meteo.

**Cosa è cambiato.** Due interruttori nel look per widget (`WidgetLook.windowWeather`,
`windowBareSky`, chiavi `window_weather_<id>` e `window_bare_<id>`, tenuti, spostati e
dimenticati come gli altri), offerti nella schermata condivisa sotto «La card mostra» per la sola
finestra, spenti di default. *Il meteo dell'ora*: `WindowPlan.weather` (non sulla striscia a una
riga); in alto a sinistra «20° · Coperto» (`WindowText.weatherLine`: `Formats.temperature` e la
parola di `WeatherText`, le stesse della card «Colpo d'occhio»), a 16 sp medio come il nome del
luogo, con uno scrim suo nell'immagine (metà di quello della didascalia, su 56 dp); sotto, il chip
dell'allerta se c'è. *Dietro le nuvole*: il pittore disegna con copertura 0, strati nulli, niente
pioggia né neve, per il gradiente, la scena e il tetto delle etichette; la luce della luna resta
(è vera); la nota «Dietro le nuvole» / «Behind the clouds» in alto a destra dell'immagine, a
Caption con l'alone, con una scatola che le etichette non toccano; `WindowPicture.bare` lo dice.
Sulla prima riga, a meno che la riga del meteo in alto a sinistra la raggiunga (la card stretta
a due per due, un carattere grande; il pittore misura la riga del meteo): allora sulla riga sotto,
e sotto il chip dell'allerta se c'è, perché niente di quello che la card scrive stia su altro.
Le parole della card (frase, verdetti, righe, numeri) restano quelle della previsione.

**Decisioni e deviazioni.** *Nessuna seconda card*: un'opzione sulla finestra, come chiesto, e con
il meteo ridotto a due dati perché il disegno resti il soggetto. *La nota nell'immagine e non nel
testo Glance*: è parte del disegno che qualifica, e con l'alone sta su ogni cielo senza scrim.
*Le parole non cambiano con «dietro le nuvole»*: «nuvole 90%» sotto un cielo stellato è il patto
con il lettore, non una contraddizione.

**Come è stato verificato.** `WidgetLookStoreTest` (i due interruttori persistono, si spostano col
look, si dimenticano), `WindowLayoutTest` (la riga del meteo c'è dove c'è un alto, non sulla
striscia), `WindowWeatherTest` (la card composta come la riceve il launcher: «20° · Overcast» solo
con l'interruttore acceso), `WindowPainterTest` (il mezzogiorno coperto della registrazione, dietro
le nuvole, è un cielo blu: più blu del grigio di oltre 0,1; l'immagine lo sa), `StringsParityTest`.
Immagini del README identiche (interruttori spenti).

### La finestra: le ore sotto l'immagine, l'orologio sulla linea di base, la luna bassa (committente, 20 ott 2026)

**Da dove nasce.** Dallo screenshot con le due card in modo meteo, quattro per tre e quattro per
due, e da quello di Taipei. Tre cose.

**Le prossime ore sulla card alta.** La proposta del committente: in modo meteo la card quattro
per tre potrebbe mostrare, al posto dei prossimi momenti del cielo, le prossime ore meteo, le
righe che «In parole» aggiunge quando si allunga, «così differenziamo meglio le due modalità».
Il parere dato, e la condizione: sì, perché è l'unico punto in cui le due modalità possono
differire nella sostanza (il resto della card era e resta del cielo), e chi vuole il meteo sotto
l'immagine vuole le ore, non i momenti; a patto che le righe siano la tabella di «In parole»
(`laterHours`, `LaterTable`: ora, temperatura, pioggia quando è almeno il 30%, parola, a tre ore
di distanza), così le ore si leggono in un modo solo su ogni card, e non un terzo disegno delle
ore. Il contro-argomento, detto: la quattro per tre in modo meteo si avvicina a «In parole»
quattro per tre e a «Colpo d'occhio» quattro per due; la differenza che resta è l'immagine come
soggetto e la frase del cielo. Fatto così: `WindowPlan.hourRows` (solo sulla forma alta, con
l'interruttore acceso): tante righe della tabella quante ne tiene lo spazio sotto un cielo di
almeno `WindowSkyMin` (tre a quattro per tre, le quattro della tabella a quattro per quattro),
la banda tagliata all'altezza della tabella (`textLaterHeight`, più l'aria `WindowHoursPadding`)
e il cielo che prende il resto, i momenti a zero; con l'interruttore acceso la banda c'è anche per chi
non segue momenti, perché è del meteo; alla fine della previsione, con meno di due ore, i momenti
che ci stanno. La nota dell'interruttore e la guida lo dicono.

**L'orologio di Taipei, seconda volta.** Dallo screenshot: «· 23:22» ancora un paio di dp sotto
la linea di base di «Taipei». In Robolectric le due linee coincidevano al pixel (sondato: posto
41 px, orologio nella sua cornice, linee di base 32 e 32), quindi l'aritmetica delle discese dei
due corpi teneva nel Roboto di Robolectric e non nel carattere di One UI. Tolta l'aritmetica:
nome e orologio sono un solo `LinearLayout` della piattaforma con `baselineAligned`
(`widget_window_name.xml`), il nome con l'aspetto che Glance dà al suo `Text` medio (l'aspetto
predefinito del dispositivo a peso 500, 16 sp), l'orologio a 11 sp; la card ne imposta il
testo, la zona e gli inchiostri. Solo quando l'orologio c'è: il nome degli altri luoghi resta il
`Text` di Glance, e le immagini del README non cambiano. `WindowClockTest` chiede ora le due
linee di base uguali, non entro un dp e mezzo.

**La luna bassa.** Chiesto se l'inquadratura dovesse scendere. Misurato con il motore dell'app
(Taipei, 20 ott 2026): la luna alle 23:52 locali (il mezzo dell'ora disegnata) sta a 13,2° di
altezza, azimut 245°, al 69%; a 00:30 a 5,6°, a 01:00 sotto l'orizzonte. Sta bassa perché
tramonta entro l'ora. Il bordo basso della finestra è a 10° (la camera delle stelle, 65° in
cima e 10° in fondo, condivisa con l'eroe): il disco a 13° sta sul bordo, com'è nello
screenshot. Non si scende: con il fondo all'orizzonte (0°) la luna a 13° cadrebbe a 45 dp dal
fondo, dietro la riga del nome; per portarla sopra la didascalia (60 dp più il raggio, 21° di
una campata di 55°) il fondo dovrebbe stare a −10°, cielo disegnato sotto l'orizzonte, e la
luna che tramonta sembrerebbe a metà cielo. Regola che resta: la luna sta dove la camera la
mette, e una luna sotto i 31° circa su una quattro per due sta dietro le parole o sul bordo,
perché le parole stanno in basso e la luna anche. Un'eventuale parola nella frase («la luna
tramonta alle 00:55») è proposta, non fatta.

**Come è stato verificato.** `WindowLayoutTest` (le ore sulla card alta con l'interruttore, la
banda della tabella, il cielo che prende il resto, quattro righe al massimo, nessuna riga dove
non c'è banda), `WindowWeatherTest` (la card quattro per tre composta: le ore e la parola della
prima, non «Golden hour»; spenta, i momenti), `WindowClockTest` (linee di base uguali),
`StringsParityTest`; card renderizzate e guardate: Taipei con l'orologio, le ore di giorno e di
notte a tre e a quattro righe, con l'allerta. Immagini del README identiche.

## Il pager fermo a metà dopo aver acceso la posizione (committente, dal telefono, 20 ott 2026)

**Il difetto.** Con «La mia posizione» spenta e un luogo scelto attivo, accenderla dal foglio
Luoghi e tornare su Oggi lasciava il pager fermo a metà tra la posizione e il luogo di prima,
con il luogo di prima ancora selezionato (screenshot: Tokyo, e la pagina a sinistra a metà).

**La causa.** `setUseGps(true)` è una scrittura sola, ma il `PagerModel` nasce da tre flussi
dello stesso store e può arrivare in due passi: prima la pagina posizione inserita in testa con
il luogo ancora attivo, poi la selezione della posizione. Al primo passo il pager tiene il luogo
per chiave e il suo indice scala di uno; il settle era letto per **indice**, quindi quello
scalino contava come «il lettore si è fermato qui» e `setActive(luogo)` riscriveva il luogo
sopra la posizione appena scelta. Quella scrittura arrivava mentre il pager scorreva verso la
posizione, gli cambiava il bersaglio a metà corsa, e il ramo che segue la selezione non partiva:
il bersaglio era già la pagina più vicina (`currentPage`) e lo scorrimento interrotto poteva
contare ancora come in corso.

**La correzione** (`PagedToday`). Il settle si legge per **chiave** (`distinctUntilChangedBy`):
una pagina che cambia indice perché un'altra le è entrata davanti non è un posto dove il lettore
si è fermato. Il ramo che segue la selezione considera «a riposo» solo bersaglio uguale a
`currentPage` **e** offset zero, e lo trattiene solo il dito del lettore
(`collectIsDraggedAsState`), non qualunque scorrimento: uno scorrimento nostro interrotto si
riprende.

**Come è stato verificato.** `PagedTodayFollowTest`: accendere la posizione non riporta il
luogo spostato come settle (fallisce sul codice di prima con `[city:2, city:2, gps]`) e la
posizione arriva a riposo; una selezione che torna indietro a un terzo di corsa viene portata a
casa. Il secondo passa anche sul codice di prima: in Robolectric lo scorrimento interrotto è già
finito quando il pager segue di nuovo, quindi il vecchio vincolo su «uno scorrimento in corso»
non scatta; è la parte della correzione che solo il telefono conferma.

## La posizione resta dove ha il meteo (committente, dal telefono, 20 ott 2026)

**Il difetto.** Offline, un luogo salvato restava sui suoi ultimi dati con l'età dichiarata;
«La mia posizione» no. Spostandosi oltre i 2 km l'app adottava subito il fix nuovo
(`adoptGpsFix`), cioè un posto senza cache: pagina vuota, banner offline, e per titolo le
coordinate, perché offline fallisce anche il nome. Le app premium fanno il contrario: restano
sull'ultimo posto di cui hanno dati e si spostano, posto e meteo insieme, appena raggiungono la
fonte. Il widget faceva lo stesso, dal momento in cui l'app prendeva il fix: «Sto prendendo la
prima previsione…», che offline non era nemmeno vero.

**La regola** (`GpsArrival`, in `:core:data`, la passano tutte e tre le strade del fix: il
toggle e la riga del foglio Luoghi, il refix di Oggi). Sotto i 2 km niente cambia. Oltre, si
chiede PRIMA la previsione del posto nuovo: arriva, e la pagina si sposta con i dati già in
cache; non arriva, e la pagina resta sul posto di cui ha il meteo, mentre il posto raggiunto
aspetta in `LocationSettings.arrival` (`holdGpsFix`; l'istante del fix si scrive comunque, un fix
è stato preso). Se il posto di prima non ha meteo da mostrare si adotta come prima: una pagina
vuota del posto nuovo è tutto quello che c'è. Un fix nuovo risponde all'attesa in ogni caso
(`adoptGpsFix` la cancella), anche tornando vicino al posto mostrato; spegnere la posizione la
dimentica.

**Quando si riprova**, sempre senza un fix nuovo, perché le coordinate ci sono già: all'atterrare
sulla pagina e al ritorno in primo piano (se il fix non è ancora dovuto; se lo è, il fix nuovo
riprova da sé), al tick del minuto della pagina posizione con lo stesso `RefreshBackoff` di un
fetch fallito, mai sotto risparmio energetico, e nel job periodico prima di scegliere il posto.
Quest'ultimo è il motivo per cui la regola sta in `:core:data` e non nel ViewModel: il job non
prende mai la posizione in background, ma il fix lo ha già preso l'app, e così lo spostamento
avviene alla prima esecuzione di nuovo in rete e non solo alla prossima apertura. In ogni caso
la scrittura avviene solo se il posto in attesa è ancora quello (`adoptArrival`): un fix più
recente non deve vedersi riportare indietro.

**Cosa dice la pagina.** Sotto il chip dell'età, «Sei a 17 km da Cavenago di Brianza: questo è
ancora il suo meteo, finché non arriva quello di dove sei.» Il chip parla dell'età del meteo, non
del posto, e il pin accanto al nome si legge «sei qui»: senza questa riga la pagina direbbe una
cosa falsa con numeri veri. La distanza è in chilometri interi e mai sotto 2, perché sotto i 2
km il fix non è un arrivo. Il campo è `TodayUiState.Content.elsewhereKm`, riempito dal
ViewModel e dai widget, mai dal builder, che dei fix non sa niente.

**Cosa dicono i widget.** Leggono la stessa posizione salvata, quindi restano sul posto vecchio
senza modifiche al loro codice. Il segnale va nella riga dell'età (`marked`, `markerText`), che
ogni forma riserva già: «Sei altrove» con dati freschi, che altrimenti non stamperebbero niente,
e «Altrove · 8 h fa» con dati vecchi. La forma lunga, «You've moved · 8 hours ago», sulle card
4×2 veniva troncata a «8 hou…», cioè perdeva proprio il numero per cui la riga esiste. Il
collettore dei widget in `ChiaroApplication` ascolta anche l'attesa, che non cambia il posto
attivo ma cambia cosa le card devono dire.

**Il limite, che c'era già.** «Colpo d'occhio» a 4×1 non ha altezza per la riga: l'età dei dati
vecchi non c'era nemmeno prima, e così ora lo spostamento. Va risolto nel layout di quella forma,
per entrambe le cose; non è in questa modifica.

**Come è stato verificato.** `GpsArrivalTest` (offline resta e aspetta; online si sposta con il
meteo già preso; sotto i 2 km non chiede niente; senza meteo da tenere adotta come prima; l'attesa
si adotta quando il meteo arriva; tornare vicino la cancella; spegnere la posizione la dimentica;
la distanza), `MarkerTextTest` (fresco: niente finché non si è altrove; vecchio: entrambe le cose,
forma corta), suite intera verde, lint. Pagina e card renderizzate e guardate in IT e EN, con dati
freschi e vecchi di 8 ore. Le immagini del README non cambiano: la registrazione non ha nessuno
spostamento.

## «Colpo d'occhio» a una riga: la riga dell'età dove c'è posto (committente, 21 ott 2026)

**Il difetto.** Sulle forme a una riga di «Colpo d'occhio» (4×1, 3×1, standard e rovesciata)
la riga dell'età stava sotto il luogo, e lì non c'era altezza: numero 44,9 dp, luogo 21,1, riga
14,5, contro i ~70 che la card lascia. Veniva tagliata fuori, quindi i dati vecchi non dicevano
mai la loro età su quella card, e dal 20 ott nemmeno lo spostamento («Sei altrove»). Era la card
più comune ed era muta proprio dove doveva parlare.

**Dove va adesso.**
- **4×1 standard** (c'è la colonna della frase): al bordo opposto, sotto la frase, che cede una
  delle sue righe (`markerBlock`, `nowSentenceLines(withMarker)`). Con il chip dell'allerta il
  chip tiene la sua riga accanto al marcatore alla dimensione di default, non a 1,3
  (`nowRowHasWarningRow(withMarker)`).
- **Disposizione rovesciata con la frase**: alla spalla del numero, la frase scende a una riga e
  la riga dell'età va sotto di lei; con il chip, sotto il chip.
- **Senza colonna** (3×1, o la frase spenta): accanto al numero, in piccolo, su due righe al
  massimo: l'altezza del numero ne contiene due con margine.

**La soglia.** Accanto al numero solo dove ci sta (`nowRowMarkerAsideFits`): la larghezza delle
parole, meno la colonna del numero (66 dp) e lo spazio (12), deve tenere «Sei altrove» a 11 sp,
~55 dp misurati; le forme più lunghe vanno a capo sulla seconda riga. Tre celle passano con
margine; due no, e lì la card resta il numero e il luogo come prima. Visto a 130 dp: senza la
soglia la riga rubava la larghezza al numero, che diventava «1…».

**Due celle: l'icona lascia il posto** (committente, stesso giorno: «sistemalo in questa PR»).
A 130 dp il numero era già troncato su `main`, «1…» sopra «M…»: la colonna delle parole aveva
48 dp, contro i 66 del numero e gli 84 che le parole chiedono, e la card dichiara
`minWidth="110dp"`. Sotto 166 dp (bordo 4 + icona al suo minimo 56 + spazio 8 + bordo 14 +
parole 84) la riga non tiene tutte e tre le cose, e l'icona è quella che va
(`nowRowShowsIcon`); il bordo dove stava diventa un bordo di parole (14 dp). La strada del
numero più piccolo è stata misurata e scartata: «−12°» in 48 dp è 22 sp, poco più del luogo a
16, e il luogo resta tagliato comunque. Il numero è ciò per cui la card esiste, il luogo è ciò
che rende onesta una card sulla posizione (il pin e «Sei altrove»); lo stato del cielo, a due
celle, è la cosa che si può lasciare all'app. A due celle «Sei altrove» non trova posto accanto
al numero nemmeno senza icona (102 dp di parole contro i 134 che chiederebbe): resta il pin.

**Come è stato verificato.** `NowWidgetLayoutTest` (il marcatore toglie una riga alla frase e mai
l'ultima; il chip accanto al marcatore a 1 sì, a 1,3 no; accanto al numero a quattro e tre celle
sì, a due no; l'icona c'è da 166 dp, non a 165 né a 110), suite intera verde, lint. Card renderizzate e guardate in italiano a 340 e 250 dp,
standard, rovesciata e senza frase, nei quattro stati (fresco, vecchio, altrove, vecchio e
altrove), e a 130 e 110 dp contro `main`: numero e luogo interi, l'icona di nuovo a 170. Le immagini del README non cambiano: sul codice di `main` e su
questo ramo `widgets.png` esce identica pixel per pixel (la differenza con quella committata è la
deriva di render di questa macchina sulla card della finestra, uguale nei due casi).

## La finestra: la frase sotto il luogo, con i dati vecchi e in modo meteo (committente, dal telefono, 21 ott 2026)

**Da dove nasce.** Uno screenshot delle 05:49, sei ore senza rete, la finestra in modo meteo:
«16° · Pioggia leggera» in alto, e sotto «Ornago» la frase «Presto per dirlo stanotte». La
proposta del committente: in modo meteo, sotto il luogo, la frase di «In parole».

**Il difetto che c'era sotto, in tutti e due i modi.** Il «?» non era della notte ma del dato:
`SkyVerdictEngine.evaluate` rifiuta di giudicare oltre `staleAfter` (`STALE_DATA`), e giusto
così; la card ne stampava la parola, «Presto per dirlo», che dice «è troppo presto» quando la
ragione è «i dati hanno sei ore». La causa sbagliata è una bugia piccola ma è una bugia, e alle
05:49 «stanotte» è quasi finita. Fatto: un verdetto `UNKNOWN` per `STALE_DATA` o `NO_DATA`
(`SkyVerdict.isRefusedForData`, nel dominio) non arriva su nessuna card: `WidgetData.momentsFor`
lo toglie ai momenti, e così la card del cielo (il chip dell'eroe, il «?» delle righe) e le righe
della finestra alta non lo stampano più; `WindowText.tonight` lo salta per stanotte, che viene dal
`SkyStateBuilder` condiviso con la scheda Cielo, dove la frase ha spazio per dire la ragione vera
e resta com'è. Stanotte tace, e la frase ricade sul prossimo momento di luce
senza verdetto («Alba alle 07:41»), che il telefono calcola da solo ed è vero comunque; l'età
resta nell'angolo. Il «?» di un momento oltre la fine della previsione (`BEYOND_HORIZON`) resta:
lì «presto per dirlo» è la ragione vera. Scelto il momento e non la luna, che il committente
lasciava aperta: il numero della luna non si può dire quando il disco è già nominato
nell'immagine (un numero solo per la luna, 20 ott), il momento sì sempre; e a cielo vuoto di
dati il prossimo appuntamento con la luce è la cosa più utile che la card sa.

**Il modo meteo.** Parere dato: sì, perché la riga in alto dice l'ora e quella sotto il luogo
può dire il seguito della giornata, come la tabella delle ore sostituisce già i momenti sulla
card alta. Con una condizione: nei giorni quieti `sentence()` ricade sulla parola del cielo,
che la riga in alto ha già detto, e la card la ripeterebbe. Fatto: `headline()` (la frase del
giorno nel registro breve, null nei giorni quieti; `sentence()` è ora lei più il ripiego) e
`WindowText.caption` che, con l'interruttore acceso, stampa quella e, se non c'è, la frase del
cielo di prima.

**Le righe, nella stessa PR** (committente, stesso giorno). Le righe dei momenti sulla
finestra alta e sulla card del cielo segnavano ancora «?» con i dati vecchi, e il chip dell'eroe
della card del cielo diceva «Presto per dirlo» a lettere grandi. Tolto alla fonte invece che in
ogni disegno, perché la regola è una: un verdetto si mostra solo da dati freschi, altrimenti
restano il momento e la sua ora, che il telefono calcola da solo. Il contro-argomento, detto: la
card del cielo non dichiara l'età del dato, quindi con i dati vecchi i verdetti spariscono senza
una parola che dica perché; ma una card che non afferma niente non mente, e un «?» con la ragione
sbagliata sì. Dire l'età anche lì è una proposta, non fatta.

**Come è stato verificato.** `SkyVerdictEngineTest` (solo `NO_DATA` e `STALE_DATA` sono rifiuti per
il dato, non `BEYOND_HORIZON`), `WidgetMomentsTest` (Milano registrata: dati freschi con verdetti,
sei ore dopo nessun verdetto e i momenti ancora lì), `WindowTextTest` (verdetto vecchio o assente:
niente stanotte; oltre la previsione il «?» resta), `WindowWeatherTest` (in modo meteo la
frase del giorno, composta sulla card; nei giorni quieti la stessa frase del modo cielo), suite
intera verde, lint. Le immagini del README non cambiano: la finestra vi è in modo cielo con dati
freschi.

## PLANNING diviso: il piano vivo e l'archivio (committente, 21 ott 2026)

**Da dove nasce.** «Planning è diventato troppo lungo»: 16 000 righe, 1,15 MB, circa 300 000
token, più di quel che una sessione tiene; nessuno, né il committente né Claude Code, lo leggeva
più per intero, e le 18 caselle aperte stavano sparse fra sezioni chiuse da settimane.

**Il parere dato, e la scelta.** Spezzare, non tagliare. Le sezioni sono il perché di soglie e
scelte che il codice cita per nome (43 rimandi); scegliere in 16 000 righe che cosa «non serve»
costa molto e rischia di buttare la misura che servirà, mentre un archivio che nessuno carica
non costa niente. Le pratiche a cui si guarda: i registri delle decisioni (ADR), dove una
decisione passata non si riscrive ma se ne scrive una nuova che la sostituisce; il registro
chiuso separato da quel che è aperto; l'archivio per periodo; e, per un agente, il contesto
caricato sempre piccolo con un indice verso il resto. Il precedente nel repo è `UPSTREAM.md`,
congelato il 23 set.

**Fatto.**

- `docs/planning/01`-`05`: le righe 1-15441 del file di prima, divise ai titoli `##` in cinque
  periodi da 2 300 a 4 000 righe, in ordine cronologico, ciascuno con una testata che lo dice
  congelato. Per periodo e non per argomento: molte sezioni toccano cielo, widget e avvisi
  insieme, e i rimandi «Fase N» sono cronologici. Le righe sono identiche: lo ha verificato il
  confronto del file di prima con gli archivi più il registro qui sopra, a meno dei separatori
  `---` e delle righe vuote ai confini.
- `docs/planning/README.md` e `tools/planning_index.py`: l'indice di ogni titolo `##` e `###`
  dell'archivio con la riga, e `--check` per sapere se è al passo. Ogni nome di sezione citato
  dal codice e dai documenti si trova nell'indice o in questo file, tranne «Fase 13b» e «Fase
  19», che sono numeri di fase di tweather e non erano titoli di questo file nemmeno prima.
- Questo file: le regole, «Come è fatto», lo stato, gli aperti, e il registro dal 20 ott («La
  finestra» e quel che segue).
- I rimandi nel codice restano com'erano: «PLANNING» è ora il nome dell'insieme, e l'indice
  risolve i titoli. Riscriverli avrebbe toccato una ventina di sorgenti per niente. Aggiornati i
  documenti che dicono dove stanno le cose: `CLAUDE.md`, `VISION.md` §1, `README.md`.

**Le caselle rilette.** Delle 18 aperte, cinque restano aperte e sono in «Aperto». Chiuse nel
trasloco, perché superate: la lista della Fase 10 (la 1.0.0 e la 1.1.0 sono uscite); l'UV a
55 dp «da guardare sul dispositivo» (`WeatherIconSize.TileUv` non esiste più); la «riserva sulle
allerte» della Fase 13, che è una regola e non un compito; il punto 13 della revisione di Avvisi
(«Quando scatterebbe»), che il committente ha chiuso con «non si fa» il 14 ott. Le altre caselle
aperte erano i passi di quelle cinque.

## Il motore degli stati diventa `docs/STATES.md` (committente, 21 ott 2026)

**Da dove nasce.** Proposto nel piano della divisione e approvato: «Il motore degli stati» (25
set) non è una cronaca ma la specifica viva del motore, e `CLAUDE.md` la citava come il posto in
cui stanno «ogni soglia e la sua ragione». In un archivio congelato non si potrebbe più tenere
al passo.

**Fatto.** `docs/STATES.md`, in inglese come `DESIGN.md`: perché l'app decide (i fatti del
provider, misurati), le soglie in una tabella con la ragione di ciascuna, le sei regole
dell'ora in ordine, i ripieghi per campo mancante, il blocco `current` e il quarto d'ora, il
giorno (pericolo, 1 mm o 3 ore, il cielo della luce), come le schermate usano gli stati, i
disegni, la misura del 25 set, i limiti e quel che resta aperto. Ogni soglia è stata riletta sul
codice di oggi (`WeatherStateEngine`, `WmoCode`, `WeatherReportMapper.dailyCode`,
`NowcastEngine`), non copiata dalla proposta del 25 set: dove la proposta e il codice
divergevano (la revisione dello stesso giorno, la probabilità stampata dal 20% del 12 ott, il
quarto d'ora del 17 ott) vale il codice. La sezione d'archivio resta com'è, come storia.

**La regola**: una regola del motore cambia in `docs/STATES.md`, nel codice e nel test nella
stessa modifica, e la decisione si scrive qui con la misura. I rimandi che puntavano a «Il
motore degli stati» per le soglie (`CLAUDE.md`, `DESIGN.md`, il KDoc di `WeatherStateEngine`, i
commenti delle stringhe, gli strumenti della misura e il loro workflow) puntano ora a
`docs/STATES.md`; `README.md` lo elenca fra i documenti.

## Gli aperti chiusi (committente, 21 ott 2026)

Le cinque caselle raccolte in «Aperto» dalla divisione del file, chiuse dal committente: «alcuni
sono vecchi e già provati da tempo».

- [x] **Il motore degli stati, passo 6** (il giro sul telefono): il motore è in uso dal 25 set.
- [x] **Le icone in stile line che pesano meno delle flat**: restano come sono.
- [x] **Il glifo del Cielo a 51 dp**: resta 51 dp.
- [x] **La pausa delle icone animate mentre la pagina scorre**: resta.
- [x] **Fase 12, MeteoAlarm**: chiusa come casella del piano, non fatta. Non è una verifica ma una
      fase mai aperta: le allerte fuori dall'Italia restano nella roadmap di `VISION.md` (Fase 12),
      e se si riaprono hanno una sezione nuova qui, che parte da `02-settembre-8-18.md`, «Fase 12»
      (il prototipo dell'11 set, la lacuna dell'apertura della Fase 11, il «fallire chiuso»).

In `docs/STATES.md` §10 il giro sul telefono non è più fra le cose aperte; la misura d'inverno sì,
perché è un limite del motore e non una casella.


## Le notifiche: la misura prima di spedire, la frase del giorno, il nastro e la scena (committente, 21 ott 2026)

**Da dove nasce.** Dal telefono (Samsung S24 Ultra): una review delle notifiche «per renderle più
premium e pulite, sia a livello grafico che di informazioni testuali», a fronte degli ultimi
aggiornamenti di Oggi e della finestra, e un difetto da verificare: il messaggio della notifica
espansa «viene troncato e esce con i puntini finali, mancano giusto gli ultimi caratteri». Il
committente ha voluto prima il piano con le ragioni, poi «Vai così», lasciando le scelte a chi
scrive con un vincolo solo: premium e coerenti con il resto dell'app.

**Il difetto, misurato.** Il corpo espanso con il grafico mette le righe dei dettagli in un solo
`TextView` con `maxLines="5"` e `ellipsize="end"`: il tetto conta le righe *visive* dopo l'a capo,
e una `RemoteViews` non misura; sforato il tetto, Android butta via quello che segue e mette i
puntini in coda alla quinta riga visiva. Sonda in sandbox (Robolectric, grafica nativa, il
template di sistema via `recoverBuilder`): le sette notifiche con immagine in italiano, dalla
Milano registrata, a 360, 384 e 411 dp per carattere 1,0, 1,15 e 1,3. A 384 dp, la larghezza
dell'S24 Ultra, il template lascia 292 dp al testo e le righe italiane misurano 250–270 dp a
14 sp. Esito: il mattino asciutto intero solo a carattere 1,0; il mattino con pioggia (sei righe)
sempre tagliato, con l'aria sparita e «Alba 07:13, tramonto 19:15…»; la sera tagliata già a 1,0
(due righe lunghe due righe), con la pioggia di domani perde l'UV intero; l'allerta con nota
sempre tagliata (la nota da sola fa cinque righe) e la fonte, l'attribuzione che la licenza
chiede, mai mostrata. Un secondo difetto, latente, dal conto del tetto: a carattere 1,3 cinque
righe più grafico fanno ~290 dp contro i 256 del tetto, e il sistema taglia il fondo senza
puntini.

**Cosa è cambiato.**

1. *La misura prima di spedire* (`NotificationFit`, nuovo): il notificatore conosce la larghezza
   minore dello schermo e la scala del carattere; stima la larghezza utile (schermo meno 92 dp
   di margini del template, stock Android 12–16, per il 92%, un margine per i caratteri dei
   produttori), scorre le righe candidate con uno `StaticLayout` a 14 sp e tiene righe intere,
   nell'ordine di valore, finché stanno nel budget; la prima che non sta chiude la lista. Il
   budget è quello che il tetto dei 256 dp lascia sotto *questo* grafico e *questo* titolo alla
   scala del lettore (`detailBudget`), non cinque fisso; `setMaxLines` del `TextView` è messo allo
   stesso numero e il `maxLines` del layout resta solo come rete. Sotto il grafico va solo quello
   che il grafico non dice: la riga del picco della pioggia (etichettato sulle barre), la riga
   «Minima, massima» del riepilogo (le etichette del grafico), la nota dell'allerta (un paragrafo)
   restano nel testo lungo di `BigTextStyle`, che tiene ogni riga per orologio, auto e schermata
   di blocco; la fonte dell'allerta resta sempre, per ultima.
2. *La frase del giorno*: i due riepiloghi partono dalla frase di `HeadlineEngine`, quella
   dell'eroe di Oggi e dei widget (allerta arancione, maltempo, l'ora dell'ombrello, gelo
   domattina, vento, o nel giorno quieto la sua asciutta e il suo caldo), nella forma più lunga
   che sta in una riga compressa (`firstThatFits`: la frase intera, poi quella breve dei widget,
   poi nel giorno quieto le due metà una alla volta), con il punto fermo, perché una riga della
   tendina è una frase dove quella dell'eroe è una riga. I numeri seguono: nel corpo con il
   grafico li etichetta il grafico dove accadono, nel testo lungo stanno in prima riga («Minima
   17°, massima 23°.»). Il worker legge le allerte graduate del luogo
   (`ServiceLocator.warningReader(context).graded`) e le passa al notificatore
   (`SyncNotifiers.notifyAlert` ha il parametro), così il primo gradino della scala è lo stesso
   della pagina. Il rapporto si legge come lo legge la pagina (`CurrentEstimate`, `WeatherRecency`);
   senza copertura dell'adesso, nessuna frase e la riga dei fatti di prima. La riga dei fatti
   stampa la clausola della pioggia solo dal pavimento del «possibile» (`CLEAR_BELOW_PCT`, 50):
   «pioggia 8%» e «pioggia 0%» sotto un eroe che dice «asciutto» erano due opinioni su un cielo.
   La riga della pioggia di oggi non si ripete sotto il grafico quando la frase l'ha già detta.
   Il sole del mattino: a sole alto «Tramonto 19:15 · 3 minuti di luce in meno di ieri», oggi
   contro ieri con `EveningDetails.sun`, la riga che aveva solo la sera; prima dell'alba la coppia.
3. *Il grafico della giornata*: la notte non è più il riquadro lilla sui due estremi della curva
   (l'unico elemento della notifica senza un fratello su qualche schermata) ma il nastro della
   luce di Oggi sotto le ore, `DaylightPhases.phases` e `ribbonColorStops` con il cielo della
   previsione, nei colori della `SkyPalette` del lettore, come linea di base del grafico; la cifra
   del minimo cambia lato quando a destra toccherebbe l'etichetta della pioggia o il bordo
   (`lowLabelRight`). Altezze 272/222 px (prima 264/212), con la curva un po' più stretta.
4. *Il promemoria del cielo con la scena* (`SkyPictures`): il quadro della finestra per l'ora del
   momento, non per adesso, dipinto da `WindowPainter.paintSky` con i nomi delle cose luminose e
   senza lo scrim della didascalia; un bitmap solo (la scena non ha tema); solo da un'ora che la
   previsione ha e mai da un rapporto che il verdetto ha rifiutato come vecchio; se la GPU non
   risponde, il disegno piano del pittore. Per farlo il pittore è stato staccato dal
   `WidgetModel`: `WindowSky` (il luogo, il meteo dell'ora, la veste; `of(model)` per la card,
   `forHour(report, …)` per una notifica), `WindowOverlay` (cosa la card scrive sopra l'immagine,
   niente per una notifica), `paintSky(…)`; `paint(model, …)` resta e vi delega. Il ricevitore
   dell'allarme disegna e passa l'immagine; il job stesso conta come seguito, così lo sciame
   debole ha il suo radiante nel suo promemoria.
5. *Documenti e immagini*: DESIGN §8.17 (misura, nastro, scena, frase), VISION §5 (la voce dei
   gemelli), CHANGELOG `[Unreleased]`, `notification-summary.png` rigenerata e guardata (frase,
   nastro, cinque righe intere), il suo alt text.

**Decisioni e deviazioni.**

- *Il 92% è un'assunzione, detta*: il carattere di One UI è più largo di Roboto e i suoi margini
  non sono più stretti; la stima deve sbagliare per difetto (una riga lasciata fuori) e non per
  eccesso (una riga tagliata). Il costo misurato: a 384 dp in italiano a carattere 1,0 la riga
  «Vento adesso 12 km/h · Una brezza leggera» (270 dp) conta due righe nella stima e una sul
  telefono stock. Lo screenshot del committente è la verifica.
- *La prima riga che non sta chiude la lista*, invece di saltare avanti a una più corta: l'ordine
  è di valore, e un corpo che salta una riga racconta la storia in un altro ordine.
- *La riga «Minima, massima» tolta dal corpo con il grafico*, decisa guardando la prima resa: sotto
  la frase ripeteva i due numeri che il grafico etichetta esattamente dove accadono, e portava il
  titolo a due righe, cioè una riga di dettagli in meno; in italiano a 384 dp è la differenza fra
  tre righe mostrate e quattro. Resta in prima riga del testo lungo, che non ha grafico.
- *Il budget dal tetto e non cinque fisso*: il conto ha fatto emergere il taglio a carattere 1,3,
  che nessuno aveva visto perché nessuno aveva un carattere grande sul telefono di prova.
- *La scena solo nel promemoria del cielo*, non nel riepilogo della sera, pur essendo quest'ultimo
  quello che parla di stanotte: ogni notifica porta l'immagine della schermata che apre, i
  riepiloghi aprono Oggi e portano la sua giornata, il promemoria apre Cielo e porta il suo cielo;
  il riepilogo della sera è intitolato «Domani» e i suoi fatti sono di domani. **Cambia una
  decisione archiviata**: `docs/planning/04-verso-1.1.0.md`, «Le notifiche, review dei messaggi e
  grafica nelle espanse (committente, 23 set 2026)», «Niente grafica per il cielo e per gli avvisi
  personali: non aggiungerebbe niente»: era vero dei grafici di quantità che l'app aveva allora;
  dal 20 ott l'app ha il proprio disegno del cielo, ed è la cosa stessa di cui il verdetto parla,
  calcolata dalla stessa previsione. L'avviso personale resta senza immagine.
- *Il pavimento della clausola è il 50 di `HeadlineEngine`* e non il 30 dell'etichetta del
  grafico: è il «possibile» della frase, lo stesso che il riepilogo usava già per la riga «pioggia
  possibile», e l'app non tiene un terzo parere sulla stessa pioggia.
- *Il punto fermo alla frase*: l'eroe non lo mette, la notifica sì, perché lì la riga è una frase
  tra frasi (le altre notifiche lo hanno) e il test «ogni frase finisce come una frase» lo chiede.
- *Il nastro sotto le ore e la curva com'era*: allineare anche la curva alla linea sottile della
  striscia delle ore era una strada; in un'immagine senza i numeri delle ore la curva con l'area
  colorata sul mondo dice la giornata meglio. Il grafico della pioggia non prende il nastro: le
  sue dodici ore possono scavalcare la mezzanotte e la sua domanda non è la luce.
- *Niente icona grande*, misurato: toglierebbe 48 dp a una riga compressa che usa già 252 su 292.
- *Il refactor del pittore è meccanico*: `paint(model, …)` costruisce `WindowSky` e delega; i test
  del pittore passano invariati e le rese della card su questo ramo sono confrontate con quelle di
  `main` (sotto). Lo scrim della didascalia si disegna solo quando c'è una didascalia
  (`captionDp > 0`), com'era di fatto per la card.
- *Niente nuova immagine nel README*: una sola notifica, come deciso il 30 set; il promemoria del
  cielo con la scena resta da vedere sul telefono.

**Proposta non decisa** (detta al committente, non fatta): «Qualità dell'aria» allineata a
«Qualità aria» del riquadro di Oggi, e «Vento adesso» senza «adesso» nei riepiloghi, dove non c'è
una finestra da cui distinguere la lettura di ora, per far stare in una riga a 384 dp le due righe
italiane che misurano 270 dp contro i 268 della stima. Si decide sullo screenshot del telefono: se
il riepilogo del mattino mostra le cinque righe, non serve.

**Da verificare sul telefono.** Il riepilogo del mattino aperto in italiano, carattere di
default: quante righe mostra e se la quinta c'è (la stima del 92% contro la larghezza vera di One
UI); lo stesso a un carattere più grande. Un promemoria del cielo aperto: la scena o il disegno
piano (luna a disco, niente nubi: allora la GPU nel ricevitore dell'allarme non risponde e lo si
sa). Il nastro sul fondo grigio di Samsung, chiaro e scuro.

**Come è stato verificato.** `NotificationFitTest` (la larghezza, le righe intere come testa dei
candidati, la chiusura alla prima che non sta, il budget dal tetto che cala con il carattere e
non supera il tetto, la scala misurata), `NotificationFitRenderTest` (testDebug, Milano registrata:
otto corpi in italiano e inglese a tre larghezze per tre scale, spediti e gonfiati nel template di
sistema con la stessa configurazione, zero puntini e mai una riga nascosta), `AlertNotifierTest`
(la frase guida e i numeri seguono, la riga dei fatti senza clausola sotto il pavimento, la frase
sola sopra il grafico, il tramonto con la luce di ieri e la coppia prima dell'alba, la pioggia non
ripetuta sotto il grafico, il picco non ripetuto, il corpo con il grafico come testa della storia),
`OfficialWarningNotifierTest` (sotto la griglia la fonte sì e la nota no), `NotificationChartsTest`
(il nastro più chiaro di giorno che di notte su entrambi i fondi, il lato della cifra del minimo),
`SkyNotifierTest` (con l'immagine il corpo si apre sulla scena e le parole restano),
`SkyPicturesTest` (testDebug, GPU: la notte della registrazione alla misura dell'immagine e più
scura del suo mezzogiorno, il ripiego piano, nessuna immagine fuori dalla previsione),
`WindowPainterTest` e `WindowWeatherTest` invariati e verdi dopo il refactor, `StringsParityTest`,
i finti notificatori di `:core:sync` aggiornati. Suite completa verde (768 app debug con i 15 screenshot saltati, 691 release, 363 domain, 284 data, 24 sync) e lint con 0 errori; le quattro rese della card della finestra su questo ramo e su `main` confrontate pixel per pixel, identiche. Le immagini della sonda e della scena
rese e guardate; `notification-summary.png` rigenerata e guardata, chiara e scura.

## La finestra: l'ora del cielo accanto alla bussola, e il sole sopra le parole (committente, 22 ott 2026)

**Da dove nasce.** Una proposta del committente sul widget, in tre parti: lasciare il motore
com'è ma inquadrare come l'eroe di Oggi, togliere la bussola e i nomi, e portare la vista reale
in Cielo più avanti. La premessa: nella finestra «la luna viene resa visibile ma non nella corretta
posizione rispetto alle stelle», e sole e luna «spesso in basso quasi a filo del bordo».

**La verifica, prima della proposta.** La premessa era dell'eroe, non del widget. Nella finestra
di notte la luna sta nella camera delle stelle (`WindowPainter`, `starPlace`), e la finestra si
gira verso di lei: la distanza luna-stella è quella del cielo, l'unico posto del prodotto in cui
lo è (VISION §5.9, DESIGN §3.9). È l'eroe che mette luna e sole sulla fascia compressa e le stelle
in una camera verso sud, «tre cornici di proposito». Di giorno, però, anche la finestra usa la
fascia per il sole, com'è nell'eroe, e la bussola non compare. Le cause vere della perplessità:
la bussola invita a verificare, e una card ridipinta ogni ora sta fino a 7° indietro o avanti a
notte piena (la metà dell'ora, 20 ott) e fino a 15° al crepuscolo; sotto i 5° la luna non è
disegnata. E «a filo del bordo» era vero per due ragioni diverse: di notte la camera 65°–10° su
189 dp mette ogni corpo sotto 31° circa dietro le parole, misurato e accettato il 20 ott («La luna
bassa»); di giorno la fascia era l'intera immagine, striscia della didascalia compresa, e un sole
sotto 25° ci finiva dentro. Stima per Milano il 22 ottobre, mezzogiorno a 33°: sole sopra 25° solo
fra le 10:45 e le 15:15 circa; a dicembre, mezzogiorno a 21°, mai.

**La proposta cambiata, e il parere.** Saputo che la notte è fedele, il committente ha scelto di
tenerla e di aggiungere l'ora per cui il cielo è disegnato, «l'app è onesta e non mente, anzi è
ancora più onesta». D'accordo: rende vera la bussola invece di toglierla. Due aggiunte dal parere:
il sole di giorno sopra la striscia, che la nuova proposta non toccava e che non costa fedeltà
perché la fascia è una convenzione; e nessuna opzione «cielo reale o simulato» nelle impostazioni
del widget, su cui il committente era indeciso: due parole che nessun lettore sa giudicare, la
regola di famiglia dell'interruttore che non si offre, e il doppio delle rese da provare.

**Cosa è cambiato.**

1. *L'ora nella riga della bussola*, di notte: «Guardando a sud-est alle 21:30», «Looking SE at
   21:30» (`WindowText.looking` con `WindowPicture.drawnAt` e il fuso del luogo, `window_looking`
   con due segnaposto). L'istante com'è calcolato, non arrotondato: una card dipinta da un sync alle
   21:37 e disegnata per le 22:07 dice «22:07». Nel testo della card e non nel bitmap: scala con il
   carattere, lo legge lo screen reader, nessuna resa in più. Solo con la bussola, quindi di notte
   nelle forme che la portano: di giorno il sole è sulla fascia e un'ora gli darebbe una precisione
   che la fascia non ha; con i dati vecchi resta l'età al posto della bussola, come prima.
2. *La fascia dei corpi di giorno* è l'immagine sopra la striscia della didascalia (70 dp) e sotto
   le parole in alto a sinistra quando ci sono (`dayBandTopDp`: la riga del meteo, il chip
   dell'allerta), com'è la fascia dell'eroe sopra il suo testo; su un'immagine senza parole, la
   notifica, resta tutta l'immagine. Di notte resta tutta l'immagine, perché la luna ci passa per
   finire dove la mette la camera delle stelle, e una fascia che finisse sopra le parole alzerebbe
   una luna che tramonta. Un effetto in più: il tetto di luminanza, che si apre solo nella fascia,
   torna a coprire la didascalia.
3. *La guida* (IT, EN), VISION §5.9, DESIGN §3.9, il README (il testo alternativo dell'immagine di
   notte) e il CHANGELOG.

**Decisioni e deviazioni.** *L'anteprima del picker resta «Guardando a sud», senza ora*: è un
disegno fisso, e un'ora lì sarebbe inventata. *Due ore su una card per un luogo in un altro fuso*:
l'orologio vivo accanto al nome e l'ora del cielo nella bussola, entrambe nel fuso del luogo; la
preposizione le distingue, e la si guarda su Taipei dal telefono. *Il CHANGELOG non aveva la
finestra*: la sostituzione dell'arco del 20 ott non è in `[Unreleased]`, e la voce di oggi la
nomina come cosa nota; la voce dell'arco che diventa finestra è da scrivere.

**Come è stato verificato.** `WindowTextTest` (la bussola con l'ora a Roma e a Taipei),
`WindowPainterTest` (nuovo: alle 17:30 della registrazione, sole a meno di 25°, il disco sta sopra
la striscia sulla card e più in basso di almeno 20 dp sull'immagine senza didascalia, con la
luminanza del disco contro il cielo accanto; le sei rese guardate), `WindowLayoutTest`,
`WindowWeatherTest`, `StringsParityTest`, `NoRawColorTest`; le immagini del README rigenerate e
guardate; suite `:app` debug e lint.

## Cielo, la review del 22 ott: i verdetti vecchi senza punto interrogativo (committente, 22 ott 2026)

**Da dove nasce.** La review della schermata Cielo chiesta dal committente dopo la finestra, in
ottica premium. Fatta sulla pagina intera resa in un'immagine sola (384 × 2600 dp, Robolectric)
in quattro stati: la sera della registrazione, una sera con dati vecchi di cinque giorni, il
giorno in tema chiaro, l'italiano; più la striscia a metà gesto. Due limiti detti: i fogli
(catalogo, pagina di un evento, anticipo) sono finestre a parte e il render non li cattura, letti
nel codice e nelle decisioni del 14 ott; e la settimana registrata è tutta di notti con la luna,
quindi lo stato con verdetto della card Stanotte non si riproduce dalla registrazione.

**I rilievi, in ordine di peso.** (1) La card Stanotte è l'unico cielo del prodotto ancora a
gradiente: Oggi e il widget disegnano la scena intera. È la casa della finestra reale, casella
aperta sopra. (2) I verdetti rifiutati per i dati stampavano «? Presto per dirlo» sulla card del
prossimo e sulle righe senza la ragione, che la card Stanotte invece dice: la schermata era
rimasta indietro rispetto alla regola del 21 ott («un verdetto si mostra solo da dati freschi»),
applicata a widget e notifiche. Con il sync a un'ora i dati sono vecchi dopo due, un telefono in
Doze di notte: stato frequente. (3) «L'ultima previsione è troppo vecchia per giudicare» ripetuta
su ogni riga di «In arrivo», sei volte a capo, il difetto tolto il 23 set per «troppo lontano».
(4) La riga «Che cosa sono questi eventi» in mezzo alla pagina, fra l'agenda e il calendario.
(5) La luna piena due volte il giorno della registrazione: «La luna di oggi · Luna piena domani»
e la riga di «In arrivo». Sta bene: gerarchia card-prossimo-righe, orari in colonna, verdetti con
i numeri a dati freschi, lunghezze italiane, i segni dei verdetti, la striscia a metà gesto.

**Cosa è cambiato, il punto 1 della proposta (rilievi 2 e 3).** Nessun chip sulla card del
prossimo e sulle righe dei momenti quando il verdetto è rifiutato per i dati
(`SkyVerdict.isRefusedForData`: `STALE_DATA` o `NO_DATA`); la riga del calendario non appende più
quella ragione (resta `NO_COVERAGE`, che è della riga). Una nota per sezione, sotto i momenti e
sotto «In arrivo» (`DataNote`): «Aggiornato 9 ore fa: i verdetti aspettano una previsione fresca»
con l'età nelle parole del chip di Oggi (`freshnessAge`), o «Ancora nessuna previsione: i verdetti
arrivano con la prima». `SkyUiState.Content.lastSync` porta l'istante della previsione. Il «?»
oltre la previsione resta, con la sua nota di sezione: lì «presto per dirlo» è la ragione vera.
La guida («I prossimi momenti», IT/EN), VISION §5.3 e il CHANGELOG lo dicono.

**Decisioni e deviazioni.** *La nota e non il chip con la ragione*: un chip «? Aggiornato 9 ore
fa» su cinque righe è la stessa frase cinque volte, e la regola della serie è dirla una volta per
sezione (23 set). *L'età nelle parole di Oggi*: un'età detta in due modi in due schermate sarebbe
una terza cosa da imparare. *I rilievi 4 e 5 e la finestra reale* vanno nel giro successivo,
perché cambiano gli screenshot insieme.

**Come è stato verificato.** `SkyStaleVerdictsTest` (nuovo, finestra alta per comporre tutta la
pagina: la sera della registrazione, dati di 8 ore, nessun «Presto per dirlo» e nessun «troppo
vecchia» sulle righe, la nota con «Aggiornato 8 ore fa» due volte, l'alba e il suo orario ancora
lì; a mezzogiorno i chip ci sono e la nota no), `SkyUndoTest`, `TonightNightTest`,
`StringsParityTest`; le immagini del README rigenerate: nessuna cambia, perché «sky-tonight» è il
mezzogiorno della registrazione in tema scuro, con i dati freschi; suite `:app` debug e lint.

## Cielo: la finestra reale nella card Stanotte, la guida in fondo, una luna piena sola (committente, 22 ott 2026)

**Da dove nasce.** Il punto 2 della proposta della review di Cielo, con i rilievi 4 e 5 nello
stesso giro. Il committente ha chiesto se nella card ci sono i nomi come nel widget e se, essendo
una card dedicata, ce ne stiano di più; e se oltre alla card ci sia uno schermo intero. Parere
dato e accolto: i nomi con la regola del widget (la luna con la fase, Venere e Giove, due stelle
al massimo, il radiante nelle notti di picco, niente sotto il 60% di copertura e niente sopra le
parole), perché un nome serve solo se risponde a «cos'è quella luce» e dieci nomi su una card
larga come un widget sono una mappa stellare che si accavalla; nessuno schermo intero, perché i
due gesti che contano stanno nella card e una vista a tutto schermo porta aspettative da app di
mappe stellari. Il committente: «abbandoniamo e non teniamone traccia».

**Cosa è cambiato.**

1. *`WindowScene`* (nuovo, in `widget/window`): il calcolo della finestra estratto dal painter del
   widget e condiviso: `frame` (dove guarda, la scena dello shader, la luna nella camera delle
   stelle, il radiante), `facing` (la scelta della direzione: luna se su, radiante, equatore),
   `labels` (i nomi, non piazzati) e `place` (le regole di piazzamento: niente nella striscia in
   fondo, niente nei riquadri delle parole, niente sopra un altro nome, a destra o a sinistra del
   punto). `WindowPainter` chiama queste e disegna; `WindowLabel`, `PlacedLabel`, `Radiant` sono
   suoi tipi. Una regola più stretta che prima: i riquadri delle parole in alto a sinistra
   (riga del meteo, chip) escludono un nome anche per il testo e non solo per il punto.
2. *La card Stanotte* (`TonightSky.kt`): in cima un palco di 200 dp, la finestra disegnata dal vivo
   da `SkyShaderLayer` con la fascia uguale al palco; i nomi disegnati sul canvas con le regole di
   `place`, con il blocco del titolo e la riga della bussola come riquadri da evitare; «Stanotte»
   e, in fondo alla sua riga, «Guardando a sud-est alle 21:00» (`WindowText.looking`, la stessa
   frase del widget); il titolo su uno scrim dall'alto con l'alone di §3.6, a tutta larghezza
   (accanto alla bussola «Notte di luna» andava a capo, visto sulla resa); sotto il palco le righe
   di prima (orari, prosa, striscia) su un fondo che è l'ultimo colore del cielo con il 45% di
   nero, il fondo della finestra alta. Il badge della luna se ne va: la luna è nell'immagine, con
   il numero della notte (`tonight.moonIlluminationPct`, quello che le righe stampano): un numero
   per card. Senza coordinate (uno stato costruito a mano) la card resta com'era, gradiente e
   badge.
3. *L'istante dell'immagine* (`tonightInstant`): quello sotto il dito sulla striscia; altrimenti
   adesso se la notte è in corso; altrimenti la metà della notte, l'ora in cui la card legge la
   luna. *La direzione* si sceglie una volta per notte (`WindowScene.facing` sull'istante di
   apertura) e non si muove con il dito sulla striscia: il cielo scorre e l'inquadratura resta.
   *Il trascinamento orizzontale sul palco* la gira (`turnedFacing`: la lunghezza focale della
   camera delle stelle, un decimo di grado per px a 200 dp), con la stessa precedenza della
   striscia (l'orizzontale parte subito, il verticale resta alla pagina); il giro resta finché la
   pagina è aperta, keyed sulla notte. Il palco è un nodo con la riga della bussola come
   descrizione; il gesto non ha un equivalente per TalkBack, la riga sì.
4. *Rilievo 4*: la riga «Che cosa sono questi eventi» in fondo alla pagina, dopo «In arrivo» e le
   sue note; «sky-ahead» del README riparte da «In arrivo», che ora arriva sotto l'intestazione.
5. *Rilievo 5*: la riga «Luna piena» di «In arrivo» cade quando l'agenda nomina lo stesso
   istante («La luna di oggi · Luna piena domani alle 18:49»), entro un minuto, perché le due
   ricerche del quarto convergono da partenze diverse e differiscono di secondi (visto: a
   mezzogiorno la riga restava, alle 21 cadeva). Resta per chi non segue la luna.
6. La guida («Stanotte», IT/EN), VISION §5.3, DESIGN §3.9, il CHANGELOG; le immagini
   `sky-tonight.png` e `sky-ahead.png` rigenerate e guardate.

**Decisioni e deviazioni.** *La metà della notte e non l'ora più serena*: a mezzogiorno la card
mostra la notte che arriva alla sua metà, che nella registrazione è coperta al 100%, e l'immagine
del README è un muro di nubi; l'alternativa, l'ora più serena della notte, è stata considerata e
scartata perché sceglierebbe il cielo migliore e non quello che decide il verdetto, e la luna è
letta alla metà: la striscia esiste per cercare l'ora. Proposta, non fatta: se dal telefono la
card di giorno risulta troppo spesso un muro grigio, si riapre con quel dato. *Un trascinamento
che riparte dallo stato*: i primi passi del gesto ripartivano tutti dalla direzione composta prima
del gesto, perché gli eventi arrivano più fitti dei fotogrammi, e la card girava di un passo
solo; la lambda legge `turn` dallo stato. *Robolectric perde la prima sequenza di tocchi* dopo
`setContent` (nessun inizio di trascinamento per la prima, la seconda arriva): il test fa un
trascinamento di riscaldamento e misura il secondo. *Nessun `moving` nuovo*: il palco si muove
con le stesse condizioni dell'eroe (icone animate, riduzione del movimento, pausa, risparmio
energetico).

**Come è stato verificato.** `TonightStageTest` (nuovo, GPU: la card alle 21 della registrazione
guarda la luna e dice l'ora, il badge non c'è, il disco è chiaro dove la camera delle stelle lo
mette; la striscia cambia l'ora e non la direzione; il trascinamento cambia la direzione e non
l'ora), `TonightNightTest` (l'istante dell'immagine dentro e fuori la notte; il giro per focale,
con il nord che si avvolge), `SkyStateBuilderTest` (la luna piena cede all'agenda e resta senza),
`SkyStaleVerdictsTest` (la riga della guida sotto il calendario), `WindowPainterTest`,
`WindowRadiantTest`, `WindowTextTest`, `NotificationFitTest` sul painter rifattorizzato con le
immagini dei widget invariate; la pagina resa intera di notte, di giorno, in italiano e a metà
gesto e guardata; suite `:app` debug e lint.

## Cielo, la card Stanotte dal telefono: l'ora che resta, il palco a 224, «Dietro le nuvole», le nubi che scorrono (committente, 22 ott 2026)

**Da dove nasce.** Dal telefono, dopo la prima build della finestra reale («il cielo si disegna
correttamente»), quattro richieste con il parere chiesto prima di fare: l'ora torna indietro
appena si alza il dito, e non si può fissare un'ora e poi girarsi; il palco più grande senza
alzare la card; «Dietro le nuvole» anche qui, persistente; lo stacco netto fra sereno e
nuvoloso scorrendo il tempo. Parere dato e approvato punto per punto.

**Cosa è cambiato.**

1. *L'ora resta.* La regola del 14 ott («il dito che si alza riporta la notte intera») era per
   una lettura di testo; con l'immagine si è rovesciata, perché fissare un'ora e girarsi è
   l'uso vero. Alzato il dito l'ora resta: cursore, lettura, immagine. La lettura diventa un
   chip di input con la × in coda («02:05 · nuvole 5% · senza luna ×», `sky_scrub_whole_night`
   come descrizione della ×), e la × o un tocco sul chip rimettono la notte intera. La parola e
   i numeri in cima restano della notte; la riga della bussola dice l'ora dell'immagine. L'ora e
   il giro vivono quanto la pagina.
2. *Il palco a 224 dp a card uguale*: aria sotto il palco 12→8, spaziatura 6→4, sopra la
   striscia 10→6, fondo 20→16, i 24 dp misurati sulla resa. Il committente ha scelto questa e
   non 240 con 16 dp in più.
3. *«Dietro le nuvole»* come chip di filtro in basso a destra del palco, con la spunta quando è
   acceso: la scelta è nelle impostazioni dell'app (`AppSettings.skyBareSky`, chiave
   `sky_bare_sky`, `SettingsStore.setSkyBareSky`, `SkyViewModel.setBareSky`, `SkyActions.setBareSky`,
   `Content.bareSky`) e sopravvive alla chiusura; il ripristino generale la azzera con le altre.
   Acceso, l'immagine è senza copertura, velo e pioggia dell'ora; le parole restano della
   previsione, la regola del widget. Separato dall'interruttore del widget, che è della sua
   card. I nomi evitano il chip come evitano la bussola e il titolo.
4. *Le nubi scorrono.* La card di prima animava la copertura con una molla; la nuova la
   calcolava dal numero intero dell'ora e saltava a ogni cambio d'ora. Ora `betweenHours` legge
   la copertura e la pioggia fra l'ora che tiene l'istante e la successiva, per i minuti
   dentro l'ora, e una molla (`ChiaroMotion.effects`) addolcisce anche un dito che salta di
   ore. Solo per il disegno: la lettura stampa ancora il numero dell'ora (`cloudAt`).
5. La guida («Stanotte», IT/EN), VISION §5.3, DESIGN §3.9, CHANGELOG; `sky-tonight.png`
   rigenerata e guardata.

**Decisioni e deviazioni.** *Il chip e non un gesto nascosto* per «Dietro le nuvole»: chi guarda
un palco coperto vuole sbirciare lì e non in Impostazioni, e un interruttore visibile con la sua
parola non si deve scoprire. *L'interpolazione è del disegno*: una copertura fra due ore è una
stima e il numero stampato resta quello della previsione, come il nastro di Oggi con la
temperatura. *Il chip dell'ora costa 12 dp* quando c'è un'ora tenuta (32 dp contro la riga di
20): accettati, perché solo mentre c'è un'ora scelta. *Il chip «Dietro le nuvole» non si
rimpicciolisce* (chiesto dal telefono): Material lo fissa a 32 dp e l'etichetta è già la taglia
più piccola; il vuoto sotto, letto più grande di quello a destra, era il bersaglio di tocco di
48 dp che aggiunge 8 dp invisibili sopra e sotto: il margine in fondo passa da 8 a 4 e l'occhio
vede 12 come a destra.

**Come è stato verificato.** `TonightStageTest` (l'ora tenuta resta dopo il sollevamento, con la
direzione ferma, e la × riporta la notte; il chip «Dietro le nuvole» riporta la scelta),
`TonightNightTest` (la copertura fra le ore: a metà strada è la media, senza ora successiva è
l'ora, fuori dalla notte niente; il numero dell'ora resta), `SettingsStoreTest` (predefinito
spento, andata e ritorno), `SkyStaleVerdictsTest`, `SkyUndoTest`, `StringsParityTest`; la
pagina resa e guardata; suite `:core:data` e `:app` debug e lint.

## La posizione raggiunta offline prende il nome al primo momento online (committente, dal telefono, 22 ott 2026)

**Da dove nasce.** Uno screenshot del riepilogo della sera: «Domani · 45.48N 9.30E» al posto del
nome. Dal 20 ott («Position: stay on the place with weather until the new one has its own») uno
spostamento fatto offline tiene la pagina sul luogo che ha il meteo e mette in attesa il luogo
raggiunto; tornati online la pagina si sposta, ma con le coordinate come titolo, e il nome arriva
solo con il fix successivo, a mano o a tempo.

**La causa.** Il fix è preso quando il geocoder non può rispondere (offline), quindi
`GeoFix.placeName` è nullo e `toGpsCity()` mette come nome l'etichetta delle coordinate. L'arrivo
è salvato così; `GpsArrival.retry()` chiedeva solo il meteo e adottava il luogo com'era. Lo
stesso succede al luogo adottato subito, quando non c'è un posto con meteo su cui restare.

**Cosa è cambiato.** `City.isUnnamed` (dominio): la pagina della posizione con le coordinate per
nome. `LocationProvider.placeOf(coordinates)`: il geocoder da solo, senza prendere una posizione,
null se assente, se fallisce o se non ha un nome; `AndroidLocationProvider` lo fa,
`CachedLocationProvider` lo passa, i finti dei test hanno il predefinito. `GpsArrival` riceve il
provider: `retry()` dà il nome all'arrivo prima di chiedere il meteo e lo adotta con il nome
(`CityStore.adoptArrival` salva la copia passata e non quella in attesa, stesso `cacheKey`);
`nameShown()` dà il nome alla pagina mostrata se è ancora senza (`CityStore.renameGpsCity`, solo
se è ancora lo stesso luogo). Chiamato da Oggi dopo un fetch che è andato a buon fine, il segno
di essere online, e dal worker periodico dopo il retry. `GeocodedPlace` diventa pubblico.

**Decisioni e deviazioni.** *Il nome dal geocoder, mai inventato*: se il geocoder non risponde le
coordinate restano, e il test lo dice. *Il momento è il fetch riuscito*, non un ascolto della
rete: l'app non osserva la connettività e non comincia qui. *Solo lo stesso luogo*: un fix preso
nel frattempo non viene rinominato con il nome di un altro punto.

**Come è stato verificato.** `GpsArrivalTest` (nuovi: un luogo raggiunto offline prende il nome
al retry, con regione e `cacheKey` uguale, e l'attesa si svuota; senza risposta del geocoder le
coordinate restano; una pagina adottata con le coordinate prende il nome al primo fetch, una
volta sola, e una pagina già con il nome non si tocca), `CityStoreTest`,
`CachedLocationProviderTest`; suite `:core:data`, `:core:sync`, `:app` e lint. Da provare sul
telefono: lo spostamento in modalità aereo e il ritorno online, il titolo della pagina e del
riepilogo della sera.

## Il widget «Momenti del cielo» va via, e due proposte chiuse (committente, 22 ott 2026)

**Da dove nasce.** Le tre proposte non decise rilette una per una con il committente. La prima
(l'età del dato sulla card del cielo, 21 ott) ha svelato che la card era un widget ritirato dal
picker il 13 ott e tenuto dichiarato per le home che lo avevano: «non voglio tenere questa
implementazione solo per compatibilità con versioni passate». La seconda (una parola per la luna
che tramonta nella frase della finestra, 20 ott): «chiudila come non fatta».

**Cosa è cambiato.** Via `SkyWidget.kt`, `SkyWidgetLayout.kt`, `SkyWidgetLayoutTest`,
`widget_sky_info.xml`, `widget_sky_preview.xml`, il receiver nel manifest, `WidgetKind.SKY` con
i suoi rami nel registro e nella configurazione, le stringhe del widget e delle sue forme
(`widget_sky_*`, `sky_form_*`, `widget_preview_evidence`), il drawable e i colori dell'anteprima.
Quel che la finestra usa resta in `SkyRows.kt` (nuovo): `CompactRow`, `VerdictNote`, l'orologio
con il giorno, il disegno del momento e le costanti delle righe. `WidgetTypeTest` tiene la sola
misura sopravvissuta; `WidgetPreviewTest` vuole un solo provider nascosto, «Le prossime ore».
VISION §5.9 dice che la card è andata e perché, DESIGN §8.7 non la nomina più, il CHANGELOG ha
la voce «Removed» e corregge la frase del 13 ott che prometteva che le card restavano.

**Decisioni e deviazioni.** *Una card ancora sulla home sparisce con l'aggiornamento*, come
l'arco il 20 ott: Android toglie i widget di un provider che non c'è più; il CHANGELOG lo dice e
indica la finestra al suo posto. *«Le prossime ore» resta* nascosto e dichiarato: è lo stesso
caso, e va deciso a parte (proposta sotto). *La proposta 1 si chiude senza farla*: l'età del
dato su una card che non esiste più. *La proposta 2 si chiude come non fatta*: la frase della
finestra è una riga, da ieri la bussola dice l'ora dell'immagine, e il tramonto della luna è un
momento del catalogo in Cielo.

**Come è stato verificato.** `WidgetPreviewTest` (un solo provider nascosto), `WidgetPreviewSizesTest`,
`WidgetLookDefaultsTest`, `WidgetTypeTest`, `WidgetMomentsTest` (sulla finestra), i test della
finestra, `ManifestTasksTest`, `StringsParityTest`, `NoRawColorTest`, `MeteoconsSetsTest`; le
immagini del README dei widget invariate; suite `:app` e lint.
