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

Le caselle ancora aperte dell'archivio, raccolte qui il 21 ott 2026 e rilette contro il codice
di quel giorno; il dettaglio resta dov'è nato. Una casella si chiude qui, con la sezione del
registro che la chiude.

- [ ] **Fase 12, MeteoAlarm** (le allerte ufficiali fuori dall'Italia). Ferma per scelta: il
      prototipo dell'11 set non è stato fuso, e prima di riaprirla vanno chiuse la lacuna
      dell'apertura della Fase 11 e il «fallire chiuso». `02-settembre-8-18.md`, «Fase 12».
- [ ] **Il motore degli stati, passo 6**: un giro sul telefono nelle città degli screenshot
      (Longyearbyen, Bari, Tokyo). `04-verso-1.1.0.md`, «Il motore degli stati», «I passi».
- [ ] **Le icone in stile line pesano meno delle flat**: la decisione (lasciare, passare a flat,
      ispessire gli anelli) è del committente. `02-settembre-8-18.md`, Fase 13, «Le icone
      sembrano sproporzionate fra loro».
- [ ] **Il glifo del Cielo a 51 dp**: da guardare sul dispositivo; se è troppo, 44 dp è una riga
      in `WeatherIconSize.Sky`. `02-settembre-8-18.md`, Fase 13, «Il Cielo prende un suo gradino».
- [ ] **La pausa delle icone animate mentre la pagina scorre**: tenerla è misurato, toglierla
      è una prova da fare guardando. `02-settembre-8-18.md`, Fase 13, «La prova sul dispositivo».

**Proposte non decise** (dette al committente, non fatte):

- La card del cielo non dichiara l'età del dato: con i dati vecchi i verdetti spariscono senza
  dire perché (21 ott, qui sotto, «La finestra: la frase sotto il luogo»).
- Una parola per la luna che tramonta nella frase della finestra (20 ott, qui sotto, «La
  finestra: le ore sotto l'immagine»).

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
