# PLANNING, archivio 11: 2 - 10 novembre 2026: dopo la 2.0.0, le icone, l'hero di Oggi, la Fase 16 aperta

> Congelato il 10 nov 2026: un periodo chiuso del registro, spostato qui da `PLANNING.md`
> senza cambiarne una riga. Non si modifica. Il piano vivo è [`PLANNING.md`](../../PLANNING.md);
> l'indice di tutti i titoli è [`README.md`](README.md). Qui stanno le note della 2.0.0 corrette, la
> trasmissione di Gadgetbridge decisa, la prima prova sulle auto BYD, l'icona nuova (l'ora blu) e le
> icone del Cielo rilette, la revisione di Oggi e del motore del cielo con le sue code (lo scrim, il
> banco che si chiude, l'hero dopo la revisione, il cielo che non plana a ogni minuto), **la Fase 16
> con la revisione, le tavole, le decisioni, le regole comuni e la descrizione degli undici step**, e
> i suoi primi due step chiusi (16.1, 16.2).

---

## Le note della 2.0.0 corrette: la finestra e l'arco (committente, 2 nov 2026)

**Da dove nasce.** Scrivendo il commento per la PR #143 con le novità della 2.0.0, il committente
ricorda che un widget è stato sostituito da «uno più accattivante, la finestra». Il confronto dei
manifest lo conferma: la 1.1.0 dichiarava cinque widget (At a glance, Le prossime ore, Momenti del
cielo, In parole, L'arco del giorno), la 2.0.0 tre (At a glance, In parole, La finestra). Le note
della 2.0.0 (archivio 10, «La 2.0.0») non nominavano **né la finestra fra le novità né l'arco fra
le cose tolte**: la sezione `[Unreleased]` non ha mai avuto una voce per la finestra (archivio 06,
«La finestra: il widget del cielo al posto dell'arco», 20 ott 2026, dove «un arco già su una home
screen sparisce») e il riassunto ne ha ereditato il buco. Un lettore con l'arco sulla home lo
avrebbe visto sparire senza una riga che lo dicesse.

**Cosa è cambiato.** La sezione `## [2.0.0]` del CHANGELOG: la finestra prima fra le novità; l'arco
con gli altri due widget nella riga d'installazione e fra le cose tolte; il titolo dice «tre widget
più vecchi» e la finestra nuova; dalla riga «Widgets» di Changed esce l'ora della finestra, che è
della novità. La ragione del major ne esce più forte: tre widget tolti, non due.

**La pagina della release** è stata scritta da `release.yml` al tag, sul CHANGELOG di allora; il
workflow rilanciato leggerebbe ancora quel commit, e un tag già pubblicato non si sposta. La pagina
la corregge il committente a mano («Edit release»), incollando la sezione nuova.

## Il meteo di Chiaro ad altre app: la trasmissione di Gadgetbridge (committente, 2 nov 2026)

**Da dove nasce.** L'issue #72 (Humorist2601, 16 ott 2026), «Integration of Breezy Weather
Content Provider»: che Chiaro passi il suo meteo ad altre app, i launcher Kvaesitso e Grace in
testa, come fa Breezy Weather. Il testo è copiato dalla stessa richiesta fatta ad Overcast il 7 ott
2026 (sameerasw/Overcast#8), il suo «I hope you will reconsider this» compreso. Letti la discussione
di Breezy (#2089), la sua libreria `breezy-weather-data-sharing-lib`, il codice delle app che
l'issue nomina, la PR di Weather Master che cita (#1131) e la documentazione di Gadgetbridge
(«weather support»). I meccanismi in gioco sono due, e l'issue li confonde:

- **Il content provider di Breezy** (dalla v6.1.0) espone i dati *di Breezy* alle app a cui il
  lettore concede `org.breezyweather.READ_PROVIDER`. Non è un'interfaccia che un'altra app possa
  offrire: Grace lo legge da `content://org.breezyweather.provider.weather` con il pacchetto scritto
  nel codice, non c'è un modo di scoprire altri fornitori, e la libreria dei client dice ancora
  «THIS LIBRARY IS NOT YET READY TO BE USED». Chiaro non ci si può agganciare.
- **La trasmissione meteo di Gadgetbridge**,
  `nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER`, con il JSON in un extra
  `WeatherJson` (gli altri luoghi in `WeatherSecondaryJson`), è aperta: la manda qualunque app meteo.
  La ricevono Gadgetbridge e la sua versione per Bangle.js (e da lì gli orologi), Kvaesitso
  (`BreezyWeatherReceiver`, letto solo quando il lettore ha scelto «Breezy Weather» come fonte
  meteo) e Victoria Launcher (`ClockWidgetWeatherReceiver`). La PR #1131 di Weather Master fa
  questo: manda il meteo a ogni app che dichiara un ricevitore per quell'azione, invece che al solo
  Gadgetbridge. Grace non ne dichiara nessuno.

**Le strade e la scelta, pensando prima a Chiaro.** Le superfici di Chiaro restano l'app e i suoi
widget: esportare numeri nella grafica di un'altra app non porta da nessuna parte la frase, il
verdetto, il cielo. Vale dove Chiaro per scelta non arriva, gli orologi (Wear OS è fuori perimetro,
VISION §10), e a costo quasi nullo. Quindi:

1. **La trasmissione di Gadgetbridge: sì**, con i vincoli qui sotto. Un'implementazione sola
   raggiunge Gadgetbridge, Kvaesitso, Victoria e chi verrà, e il formato è di Gadgetbridge: nessuna
   interfaccia nostra da mantenere.
2. **Un plugin meteo per Kvaesitso: no, per ora.** Il suo SDK (Apache 2.0) mostrerebbe «Chiaro» per
   nome fra le fonti meteo, ma è Kvaesitso a chiedere i dati quando vuole: chiamate a Open-Meteo al
   ritmo di un'altra app, contro la batteria come funzione e il vincolo del piano gratuito; serve
   un launcher solo, e il solo guadagno è il nome.
3. **Un content provider di Chiaro: no.** Nessuno lo leggerebbe finché un launcher non scrivesse
   codice apposta, ed è un'interfaccia pubblica da documentare, versionare e non rompere più.

**I vincoli, decisi ora.**

- **Spenta di default.** L'interruttore dice cosa manda e a chi; la nota sulla privacy delle
  Impostazioni lo dice anche lei. È la prima volta che il meteo del lettore esce da Chiaro verso
  un'altra app.
- **Gli stati di Chiaro, non il `weather_code`.** La condizione inviata, dell'ora e del giorno, è lo
  stato del motore tradotto nel codice che Gadgetbridge si aspetta, con la parola di Chiaro: la
  regola «i numeri sono del modello, gli stati dell'app» vale anche fuori. La traduzione segue il
  motore come STATES: una regola che cambia lì cambia la traduzione e il suo test. L'icona la
  disegna l'app che riceve, non Chiaro.
- **Nessuna richiesta in più se il job gira già** per avvisi o widget: la trasmissione parte dalla
  stessa esecuzione, dopo il fetch. Se l'interruttore è l'unica ragione per tenere vivo il job
  (`SyncScheduler.shouldRun` avrà un motivo in più), l'interruttore lo dice, e il peso si scrive qui
  con lo step: 8,4 chiamate per aggiornamento per luogo, all'intervallo del lettore.
- **Nessun nuovo accesso alla posizione, niente coordinate.** Parte il luogo che il job già legge,
  con il suo nome; i campi delle coordinate, facoltativi nel formato, restano vuoti.
- **Un intent esplicito per ogni app che ascolta**, trovata con `queryBroadcastReceivers`: da
  Android 8 una trasmissione implicita non arriva ai ricevitori del manifest, e la documentazione di
  Gadgetbridge chiede intent espliciti da Android 14. Il manifest dichiara l'azione in `<queries>`,
  senza la quale da Android 11 Chiaro non vede chi ascolta.

**Da decidere con lo step**: quale luogo parte (quello aperto nell'app, quello del job, gli altri
in `WeatherSecondaryJson`), in quale gruppo delle Impostazioni sta l'interruttore e con quali parole,
quanto delle ore e dei giorni si manda. Con lo step si aggiornano VISION (§10, §9: il meteo che
esce verso altre app, su richiesta del lettore), DESIGN (la riga delle Impostazioni), la guida, il
CHANGELOG.

**Risposto sull'issue** (2 nov 2026): il content provider di Breezy non si può fare, la trasmissione
di Gadgetbridge sì, spenta di default e con la lettura di Chiaro; nessuna data. L'issue resta aperta
finché la funzione non esce. Nessun impegno di data: lo step sta fra gli «Aperto».

## BYD: la prima prova in auto, e quali DiLink installano Chiaro (committente, 3 nov 2026)

**Da dove nasce.** Chi ha aperto la PR #143 ha provato la 2.0.0 sull'auto, la mattina dopo
l'uscita: l'installer di DiLink risponde «解析软件包时出现问题» («There was a problem parsing the
package»), con la foto dello schermo nel commento. Su un Android più vecchio di 11 è il messaggio
con cui l'installer rifiuta un'app che chiede una versione più recente di quella del dispositivo.
L'APK della 2.0.0 è a posto: `minSdkVersion` 33, firma v2 (valida da Android 7), librerie native
per le quattro architetture. Chiaro chiede Android 13 dalla 1.0.0, e il `minSdk` non ha mai avuto
un altro valore nella storia del repository: sull'auto non si sarebbe installata nemmeno la 1.1.0.
La PR era quindi un segno copiato dal manifest di DiPlay (la voce del 2 nov 2026, archivio 10),
mai provato su Chiaro. La versione di Android dell'auto l'abbiamo chiesta nella risposta;
l'ipotesi è che sia sotto la 13.

**Quali BYD hanno Android 13.** BYD non pubblica una tabella. Le misure con una data vengono dai
progetti open source che girano sulle sue auto e ne leggono il firmware:

| Generazione | Android | Fonte |
|---|---|---|
| DiLink 5.1 | 13 (API 33) | xor777/denza-lab, «verified on the live car… Android 13 / API 33», 14 ago 2026; DiPlay, impronta `BYD-AUTO/IVI/IVI:13/…`, la stessa su Leopard 5 Ultra e Denza N9; Kiroha/byd-dashcast, un DL5.1 a API 33 |
| DiLink 100 (Atto 3 EVO) | 14 (API 34), base Android Automotive | angoikon/byd-trip-stats |
| DiLink 5 / 5.0 | 11 o 12 | byd-trip-stats (Sealion 7, Android 11); DiPlay (Tang 2024, Android 12; «BYD Android 12L, SDK 32») |
| DiLink 4 | 10, alcune ROM su base 12 | DiPlay; yash-srivastava/Overdrive-release |
| DiLink 3 | 9 o 10 | byd-trip-stats; anexplorer.io (non ufficiale) |

Chiaro si installa quindi su DiLink 5.1 e DiLink 100, non sulle unità precedenti. Che un
aggiornamento del firmware porti una DiLink più vecchia ad Android 13 è improbabile: nelle BYD la
versione di Android segue la generazione dell'unità, cioè l'hardware (byd-trip-stats: sotto DiLink
100 c'è «a different platform»). Gli aggiornamenti via rete cambiano le funzioni e l'interfaccia.

**Deciso dal committente.** Il `minSdk` resta 33: abbassarlo vorrebbe dire un secondo disegno del
cielo senza `RuntimeShader`, la lingua scelta dentro l'app, una seconda strada da collaudare per
sempre, e per DiLink 3–5.0 non basterebbe nemmeno Android 12. Il flag resta: su ogni altro
dispositivo non costa niente, e le unità che installano Chiaro sono proprio quelle con il firmware
che lo legge.

**Cosa è cambiato.** La voce «Split screen on BYD cars» del CHANGELOG `[2.0.0]` dice che serve
un'unità con Android 13 o superiore, come DiLink 5.1 o DiLink 100, e che le DiLink più vecchie non
installano Chiaro: chi ha una BYD più vecchia leggeva la voce come se valesse per la sua auto. Il
commento del manifest dice le stesse generazioni. La pagina della release v2.0.0 la corregge il
committente a mano («Edit release»), sostituendo quella voce sola. Il README resta com'è: una
funzione di nicchia e non ancora confermata non sta nella pagina principale; se ne riparla quando
qualcuno la conferma su un'auto.

## L'icona nuova: l'ora blu (committente, 9 nov 2026)

**Da dove nasce.** Il committente ha chiesto di ridisegnare da zero le icone delle quattro app
della famiglia (Chiaro, Passo, Tempo, Tetto), che erano tutte l'anello con un piccolo emblema
ambra: icone che dicano l'app, a tutto campo nello stile di Android, pulite anche in piccolo,
piatte (al più la sfumatura di un colore, la luce dallo stesso lato per tutte), con colori
brillanti ma non troppo e una palette sola per la serie. Le proposte sono state disegnate su una
tavola (tre simboli per app, due fondi, la prova sulla home, la griglia), e il committente ha
scelto. Per Chiaro: **l'ora blu**, la seconda delle tre proposte, al posto della consigliata
(il sole dietro una nuvola su un cielo dal giorno alla notte, con le stelle): meno affollata, e
dice comunque il cielo di giorno e di notte. Il prezzo, detto al committente: dice il cielo più
del meteo, perché non ha nuvole; lo regge il fatto che la schermata principale è un cielo
calcolato. Cambia due voci d'archivio: «L'icona dell'app, ridisegnata» (archivio 04, 23 set
2026, il giorno in un anello) e «Le icone della famiglia, rese omogenee» (archivio 05, 18 ott
2026, il sole piatto e l'icona della barra di stato).

**Le regole della serie**, uguali nelle quattro app:
- il fondo è il colore dell'app, schiarito in alto a sinistra e scurito in basso a destra; il
  simbolo è bianco con un solo accento ambra;
- ogni forma ha una rampa dal tono chiaro in alto a sinistra a uno più scuro in basso a destra,
  con lo stesso angolo per tutte; niente ombre (le aggiungono il launcher e lo store);
- la palette è una: Cielo `62AEFF · 2F7FE6 · 1B5CC8` (Chiaro), Notte `2A55B8 · 17398A · 0E2462`
  (il buio del cielo, solo Chiaro), Prato (Passo), Corallo (Tempo), Indaco (Tetto), Sole
  `FFD36A · FDB528 · F28C17` (l'accento di tutte), il bianco;
- **i margini** (correzione del committente dopo la prima tavola, dove i raggi del sole uscivano
  dalla zona sicura): tutto il simbolo sta entro 28 unità dal centro, un cerchio di 56 dp sui
  72 visibili, misurato sul disegno (27,7 per Chiaro). Con la maschera rotonda restano 8 dp
  d'aria, con le altre di più; arriva al bordo solo il fondo.

**Cosa è cambiato nel codice.** `tools/draw_launcher_icon.py` è riscritto e scrive tre livelli
invece di due: il fondo è ora un disegno (`ic_launcher_background.xml`, il cielo e, sotto
l'orizzonte, la terra della notte) al posto del colore `#F7F4EE`, che esce da `colors.xml`;
`mipmap-anydpi-v26/ic_launcher.xml` punta al disegno. La terra sta nel fondo e non nel primo
piano, così la parallasse del launcher muove il sole sopra di lei e non la stacca dal bordo.
L'icona a tema trasforma la terra in una barra larga quanto il cerchio dei 56 dp. L'icona della
barra di stato (`ic_stat_chiaro`, tutte e quattro le notifiche) è la stessa figura in un colore,
dentro i 2 dp d'aria delle icone di sistema: prima era il sole con una nuvola. `notification_accent`
resta `#2C7BF2`, vicinissimo al blu del cielo nuovo; cambia solo il commento. DESIGN §3.8 è
riscritto, e la riga delle notifiche in §8 lo segue.

**Come è stato controllato.** Gli XML scritti dallo script, ridisegnati e confrontati pixel per
pixel con il disegno approvato sulla tavola: lo 0,6% dei pixel differisce, tutto sui bordi
(l'antialias). `notification-summary.png` del README è rigenerata, perché mostra l'icona piccola.

## Le icone del Cielo, rilette tutte (committente, 9 nov 2026)

**Da dove nasce.** Il committente: «puoi fare una review delle icone degli eventi di Cielo e
dimmi se hai suggerimenti o se vanno bene così? Devono comunque rimanere in stile con le app
meteo ma ci sono alcune che non mi convincono, tipo stelle cadenti e ora blu. Verificale
tutte». I sessanta momenti del catalogo usano diciassette disegni: disegnati tutti nelle
quattro facce, a 72, 32 e 20 px, e letti contro la definizione di ogni evento (le pagine della
guida). Sei dicevano il contrario dell'evento o non si distinguevano da un altro; le proposte,
composte con i pezzi di Meteocons, sono state mostrate su una tavola accanto ai disegni di oggi,
e il committente le ha accettate: «Sì, implementale in una PR separata».

**Cosa cambia** (`tools/compose_sky_marks.py`, `ChiaroIcons.skyJobLineRes`, DESIGN §13.1):
- **gli sciami**: una stella che cade con tre scie dietro. La meteora del 14 ott 2026 aveva le
  stelle a meta' di una scia lunga tutta la scatola, e si leggeva come un barrato;
- **l'ora blu**: il sole sotto la linea, tratteggiato come la luna nuova, e le prime due stelle.
  Quella del 25 ott 2026 (archivio 07, «L'ora blu ha un disegno suo: il sole sotto l'orizzonte con la prima stella») era la cupola sopra la
  linea con la stella in cima: una pedina, e il sole ancora sopra l'orizzonte. Questa voce la
  cambia;
- **le notti bianche** prendono lo stesso disegno: erano la notte stellata con la luna, il
  contrario di notti che non diventano mai buie;
- **il buio pieno e il centro della Via Lattea**: tre stelle senza luna (`three-stars`). Il buio
  pieno e' la notte astronomica con la luna sotto l'orizzonte, e il disegno aveva una grande
  falce;
- **la luna piena al crepuscolo**: il sorgere della luna con il disco pieno al posto della falce
  (`full-moonrise`);
- **l'eclissi di Luna**: la luna piena con l'ombra della Terra in rosso, il rosso degli avvisi
  (`lunar-eclipse`). Era la luna piena, la stessa riga due volte nella lista;
- **la luce zodiacale**: un cono di luce storto sull'orizzonte con due stelle
  (`zodiacal-light`). Era `horizon`, un sole con i raggi. Detto al committente con la proposta:
  e' la piu' debole delle sei, il cono puo' ancora sembrare una vela; da guardare sul telefono.

Restano com'erano, perche' dicono il loro evento: alba e tramonto (anche i crepuscoli civili),
il sole (ora d'oro, mezzogiorno, perielio e afelio), la stella (crepuscolo nautico, Venere,
Giove), la notte stellata con la luna (crepuscolo astronomico, dove la luna puo' esserci), le
fasi e la luce cinerea, l'eclissi di Sole, le congiunzioni, l'orizzonte (equinozi e solstizi,
che ora l'hanno tutto per se').

**Lo strumento disegna anche, dal 9 nov 2026.** Le scie, la linea dell'orizzonte, il sole
tratteggiato, il cono e l'ombra non esistono in Meteocons, e lo strumento li disegna con due
regole: la linea della famiglia (4 unita' nella scatola di 128) e nessun colore inventato, ogni
tratto prende il suo da un disegno della famiglia nella stessa faccia (`star`, `horizon`,
`code-red`). I pezzi restano copiati alla lettera; `SkyMarksTest` controlla i pezzi come prima e,
in piu', i colori e la linea dei tratti, faccia per faccia. `moon-star` e `two-stars` escono
identici. `falling-stars` non e' piu' una sorgente, ma resta importato per
`ChiaroIcons.fallingStars`.

**Come e' stato controllato.** I disegni scritti dallo strumento, ridisegnati e confrontati pixel
per pixel con le proposte approvate: differiscono al piu' lo 0,2% dei pixel, sui bordi.
`SkyMarksTest`, `SkyMomentIconTest` (la politica id per id, aggiornata), `MeteoconsSetsTest` e le
tabelle verdi; le immagini del README che mostrano questi disegni sono rigenerate.

## Le icone nuove provate sul telefono (committente, 9 nov 2026)

Il committente ha unito le due PR (l'icona dell'app, #148; le icone del Cielo, #149) e provato
gli APK: «Ho verificato gli APK». Alla domanda sulla luce zodiacale, la piu' debole delle sei:
«Sì, la luce zodiacale va bene, segna le caselle». Le due caselle di «Aperto» sono chiuse; il
disegno della luce zodiacale resta com'e'.

## Tetto nella famiglia, e Saldo nella serie delle icone (committente, 9 nov 2026)

Il committente ha chiesto se i documenti dei vari repo nominano Tetto, la nuova app della famiglia:
«Hai allineato anche i documenti con il riferimento alla nuova app di famiglia "Tetto" nei vari
repo?». Non lo facevano: Tetto (il budget) e' la quinta app, e la nominano ora «The family» del
README e «The family» di CLAUDE.md, accanto a Passo, Saldo e Tempo. DESIGN §3.8 diceva le regole
della serie «uguali nelle quattro app»: con Saldo, entrato nella serie lo stesso giorno (il
portafoglio e la carta su un verde acqua), sono cinque. Il registro di oggi («L'icona nuova: l'ora
blu») resta com'e', perche' racconta le quattro icone ridisegnate quel giorno.

## Oggi e il motore del cielo: la revisione da app premium (committente, 9 nov 2026)

Il committente ha chiesto una review della schermata Oggi «da app premium» e del motore che
disegna il cielo, l'ha ricevuta su un canvas (prima e dopo, sei cieli, un laboratorio con lo
shader portato in WebGL accanto alle proposte) e ha risposto: «Accetto la tua proposta in tutti
i punti di revisione della schermata Oggi e del motore del Cielo. Implementa tutto». Questo è
tutto, in un intervento. Le regole restano quelle: ogni cosa disegnata è la previsione dell'ora
o l'effemeride, mai scelta per bellezza; ogni regola ha il suo test; i numeri a schermo non
cambiano.

### Il cielo: quel che la scena disegna in più (DESIGN §3.9, §3.5)

- **La nebbia.** La scena conosceva la pioggia e la neve soltanto (`SkyFall`): in un'ora il cui
  stato era nebbia il canvas disegnava la copertura, spesso un sereno, sotto la parola
  «Nebbia». Era l'unico punto in cui il cielo contraddiceva la schermata. Ora l'ora in nebbia
  (lo stato del motore, non il codice) disegna un banco che si addensa verso terra, nella luce
  delle nuvole senza il loro colore, fitto quanto dice la visibilità su cui lo stato è stato
  letto (`fogDensity`: 0,6 a un chilometro, 1 a 200 m; fra i due dove hanno deciso i testimoni
  e l'ora non ha un numero).
- **Il temporale.** Sui tre codici che lo sono (non il 1095, che è un'ora asciutta sotto un
  cielo che potrebbe) il banco è più scuro, non ha sole, e **lampeggia da dentro**: due impulsi
  a un quarto di secondo, a volte un terzo tre secondi dopo, in un ciclo di sei e mezzo. Mai
  tre lampi in un secondo (WCAG 2.3.1), sotto il tetto di luminanza come ogni pixel, niente
  fulmine disegnato: dove cade non lo sa nessuna previsione. Un cielo fermo non ha lampo.
- **Il lato del sole basso**: dal sole a 16° in giù l'orizzonte si accende e si scalda verso il
  sole e si spegne dalla parte opposta. Nessun campione.
- **Il bordo delle nuvole e la loro vita**: due ottave fini che erodono solo il bordo, una
  rampa di 0,14 invece di 0,22, e il campo che le piega cammina nel tempo, così una nuvola
  cambia forma oltre a scorrere. `deckThreshold` rimisurata sulla riga nuova, su quattrocento
  tele: si è mossa di meno di un centesimo. Il test del banco legge sessanta tele invece di
  ventiquattro: ventiquattro leggevano due centesimi sopra qualunque tabella.
- **I raggi fra le nuvole rotte**: otto campioni, solo con un banco fra il 12 e il 97% in
  un'ora asciutta. Discreti per scelta. Sono la cosa più cara e la prima a uscire (Aperto).
- **La luna con i mari**, nove macchie dove stanno, il lembo più scuro, e la faccia girata come
  la vede il luogo (il nord celeste alla luna, con la stessa proiezione del lembo illuminato).
  L'asse lunare e la librazione non sono applicati, ed è scritto.
- **Una meteora** nelle notti di picco di uno sciame forte (la regola della finestra,
  `WindowScene.peakRadiant`), una ogni 45 secondi circa, via dal radiante. Un disegno, non un
  conteggio: l'app continua a non stampare tassi.
- **La pioggia a scrosci, la neve con un piano vicino, le stelle basse che tremano di più.**
- **Il cielo fra due cieli** (`SkyTransition`, `ChiaroMotion.sky`): al cambio d'ora, a un
  aggiornamento, al nastro trascinato, il canvas scivola da un cielo all'altro in tre quarti di
  secondo invece di cambiare in un frame.

**Il costo**: il cielo peggiore passa da 25 a 28 campioni a pixel (un banco, un velo e una
pioggia; o un banco rotto, un velo e i raggi). Scritto in DESIGN §3.5 con il conto strato per
strato. La cifra della GPU va letta su un telefono (Aperto).

### Oggi: l'hero (DESIGN §3.6, §4, §5)

- **Lo scrim** ora è nel colore del cielo (lo zenit, scuro quanto lo scrim neutro), sta dove
  sta il testo (pieno dietro la cifra e dalla frase in giù, sfumato intorno; il cielo accanto
  alla cifra è cielo) ed è forte quanto serve al cielo che ha sotto (tutto di giorno, niente
  di notte, tutto in un temporale). Il contratto non cambia e ogni passo è misurato contro di
  esso: sull'aritmetica, per ogni tabella, altezza, copertura e luna, e sui pixel disegnati
  dallo shader. La cifra, che stava sulla rampa a 2,30:1, ha per la prima volta lo scrim pieno
  dietro. È la strada che il Passo 7 della revisione del cielo lasciava al committente
  (archivio 05, `05-ottobre-1-19.md`, «Passo 7: lo scrim, deciso sulla misura», 19 ott 2026),
  presa in un altro modo: gli orari restano sul canvas.
- **Il nastro si trascina**: cammina le ore intere di oggi ancora davanti, il cielo è quello
  previsto per l'ora, la cifra e la parola sono le sue, e la riga sotto dice di chi sono
  («Previsione delle 18:00»). Lasciato, torna adesso. L'ora del modello, mai una media di due.
  Per chi non può trascinare, due azioni sul nastro.
- **La cifra**: da Bold a Regular. La revisione diceva 300 «da verificare»; la verifica ha
  trovato che Google Sans parte da 400, quindi il 300 sarebbe stato un peso in Inter e un
  altro in Google Sans, che è l'argomento con cui il 20 set 2026 il Light era uscito. È 400,
  il più leggero che le due famiglie disegnano. **Cambia** «Il secondo carattere: Google Sans,
  e la temperatura in grassetto» (archivio 03, `03-settembre-18-21.md`, 20 set 2026) per la
  cifra dell'app; le card restano Bold e l'app e il suo widget tornano a stampare lo stesso
  numero in due pesi. È il prezzo, è scritto, ed è la casella che chiede conferma (Aperto).

### Oggi: il foglio (DESIGN §8.4, §8.5, §8.6)

- **L'agenda**: la fine di un intervallo sta sotto il suo inizio, nella colonna dell'ora; le
  regole del lettore che valgono adesso sono una riga sola, con i loro nomi. Il chip del
  verdetto resta sulla sua riga dall'inizio della riga: la misura della larghezza a 360dp che
  lo aveva messo lì vale ancora, e la proposta di portarlo sotto il nome non è stata fatta.
- **La settimana**: chiusa, una riga è la sua barra; aperta, mostra il suo nastro; e ogni riga
  che si apre finisce con uno chevron. **Cambia** due punti della seconda revisione di Oggi
  (archivio 05, `05-ottobre-1-19.md`, «Punto 7: la settimana leggibile», 15 ott 2026: un
  blocco solo di due barre, e lo chevron che se ne andava) e il nastro nelle righe chiuse
  dello stesso archivio («Il nastro del cielo», 12 ott 2026). Lo chevron costa 24dp al blocco:
  la riga si spezza in due dal 125% di carattere su un telefono da 360dp, era dal 145%.
- **I dettagli**: una riga è alta quanto quel che contiene (via il minimo di 160dp); il vento
  ha un quadrante da 40dp con la freccia che vola col vento. **Cambia** «Punto 4: i dettagli
  uniformi» dello stesso archivio (15 ott 2026) per l'altezza, e l'uscita della freccia
  dell'11 set 2026 (DESIGN §8.6): quella stava in una riga di testo a 16dp.
- **Il tile Nuvole** legge «cielo chiuso» sulla luce che le nuvole tolgono, come la parola
  dell'hero dal 26 ott 2026: sulla Milano del README (86% di nuvole, 66 basse) l'hero diceva
  «Nuvoloso» e il tile «Cielo chiuso, luce grigia». Ora dice «Sole a tratti».

### Dove non arriva

- La nebbia, il lampo e la meteora sono chiesti dall'hero di Oggi soltanto. La finestra e la
  card Stanotte usano lo stesso shader e ne prendono il bordo, l'evoluzione, gli scrosci, la
  neve e i mari, ma non passano nebbia, temporale né radiante: proposta in Aperto.
- Nessuna richiesta nuova al provider: il peso del fetch resta 8,4 chiamate.
- Gli screenshot del README sono rigenerati e guardati uno per uno (Oggi, la notte, il
  pieghevole, la settimana, i dettagli, i widget, le impostazioni, la guida).

## Lo scrim torna una fascia: la sagoma sul testo era una macchia (committente, 9 nov 2026)

Guardato lo screenshot dell'hero rigenerato, il committente: «ha la parte dove c'è il testo
che è più scuro». Lo scrim sagomato sul testo (sezione sopra, «Oggi: l'hero»: pieno dentro il
riquadro della cifra e dalla frase in giù, sfumato intorno) liberava il cielo accanto alla
cifra, ma su un cielo chiaro si leggeva come una macchia scura dietro il testo, con un lato
che si poteva indicare; una fascia su tutta la larghezza si legge come il canvas che scurisce
verso il piede.

- **Tolta la sagoma**: `SkyGuard`, `scrimShare`, la maschera, e il riquadro che l'hero
  riportava al canvas. Lo scrim in basso è la fascia di prima, dal 45% dell'altezza al pieno
  dove finisce il testo, la cifra sulla rampa con l'alone, come misurato il 19 ott 2026.
- **Restano** il colore del cielo (`scrimFor`) e la forza quanto serve (`scrimNeed`), con i
  loro test: di notte non c'è scrim, di giorno è quello di sempre in un altro colore.
- **Corregge la sezione sopra** in due frasi: «sta dove sta il testo» non vale più, e «la
  cifra ha per la prima volta lo scrim pieno dietro» nemmeno; la cifra Regular non ha più
  quell'argomento, e resta sulla casella che chiede conferma (Aperto).
- DESIGN §3.6 dice che è stata provata e perché è uscita, per chi ci riprova: era la forma,
  non la sfumatura. Screenshot del README rigenerati e guardati (Oggi, la notte, il
  pieghevole, i widget).

## Il banco che si chiude: tinta unita, varchi ritagliati, il sole al tramonto (committente, 10 nov 2026)

Dal tablet, Huaraz tre minuti prima del tramonto, pioviggine sotto un cielo quasi chiuso: «Il
colore in primo piano delle nuvole è corretto? Sembra che non ci siano sfumature e il colore
sembra in tinta unita: è voluto?». Ridisegnata la stessa ora con `SkyShaderPixelTest`
(`sunset-drizzle-*`), la risposta è in tre parti.

- **Voluto in parte.** Un banco chiuso è piatto per regola dal 16 ott 2026 (archivio 05,
  «un grigio con pieghe morbide, non un cielo di sbuffi illuminati»): il rilievo delle nuvole
  si riduce al 35% quando il banco si chiude. Ma da lì a una tinta sola da bordo a bordo ne
  passa: ora il banco è più chiaro dove è sottile e più scuro dove è spesso, fino a un sesto
  in più o in meno, sulle ottave grosse che ha già. Nessun campione in più.
- **Colpa della revisione del 9 nov.** Il bordo netto ed eroso dato alle nuvole (sezione «Oggi e
  il motore del cielo») è il bordo di un cumulo: su un banco al 95% ritagliava i varchi come
  carta. Dal 60 al 95% di banco le ottave fini lasciano e la rampa torna larga (da 0,14 a
  0,32). `deckThreshold` rimisurata: i valori dal 70% in su scendono fino a 0,025.
- **Sbagliato da prima.** All'ora d'oro il colore illuminato delle nuvole prende quello
  dell'orizzonte, e lo prendeva anche sotto un banco che il sole non attraversa: nuvole
  marroni sotto la pioviggine. Ora tengono la luminosità di quell'ora e perdono quattro
  quinti della tinta a opacità piena; un banco rotto al tramonto resta illuminato
  dall'orizzonte. E la chiazza arancione dove stava il sole: la macchia pallida che uno
  strato lascia passare (e la coda davanti alla nuvola) spariscono negli ultimi dodici gradi
  del sole, perché un sole basso illumina lungo il banco e non attraverso. A mezzogiorno
  resta com'era.

Quattro regole, ognuna con il suo test (`SkySceneTest`, `SkyShaderPixelTest`, `SkyDeckTest`),
scritte in DESIGN §3.9 «A deck that closes». Il gradiente sotto le nuvole non è toccato: un
cielo coperto al tramonto tiene il 30% del suo colore, come da §3.3, ed è quel che si vede nei
varchi. Screenshot del README rigenerati e guardati (cambiano di poco: il banco di Milano è
al 70%).

## L'hero dopo la revisione: quanto costa, le icone ferme sotto il dito, il cielo con un interruttore suo, il luogo che tornava indietro (committente, 10 nov 2026)

Quattro richieste in una, dopo la PR #150: leggere quanto costa l'hero rispetto a prima; fermare
le icone mentre si trascina il nastro; un'opzione per tenere fermo il cielo; e un difetto, il
luogo cambiato da Cielo o da Avvisi che su Oggi restava quello di prima, a volte con ridisegni.

### Quanto costa l'hero rispetto a prima

Letto sul codice, non misurato: su questa macchina non c'è un telefono, e la cifra della GPU
resta la casella aperta della revisione (Aperto, «La revisione di Oggi e del cielo sul
telefono»). Quel che si può dire da qui.

- **Il processore, a riposo: quasi niente in più.** Il ritmo è quello di prima, 15 frame al
  secondo per un cielo che scorre e 30 per uno in cui cade qualcosa (o lampeggia: un temporale
  ora disegna a 30 anche senza pioggia). A ogni frame ci sono nove uniform in più da passare
  allo shader e `scrimNeed`, cinque potenze: microsecondi, nessuna allocazione.
- **Il lavoro in più è della GPU**, a pixel, in campioni di rumore, e dipende dal cielo: sereno
  0 → 0; coperto 13 → 15 (il bordo del banco); pioggia sotto un banco 19 → 23 (gli scrosci);
  nebbia 3 in più; e il caso che sale davvero, **un banco rotto con il sole su un'ora asciutta,
  13 → 23** (gli otto dei raggi, più quattro tangenti), che è il «poco nuvoloso» di un
  pomeriggio qualunque. Il peggiore in assoluto passa da 25 a 28, come già scritto.
- **La planata disegna a ogni vsync, e parte ogni minuto.** `content.sky` cambia a ogni tick di
  minuto (il sole si è spostato di un quarto di grado), la scena con lui, e `SkyTransition`
  plana per un secondo circa disegnando lo shader a ogni frame dello schermo: 60 o 120 frame
  dove ce ne sarebbero 15, per una differenza che l'occhio non vede. In media il 5% di frame in
  più a 60 Hz e il 12% a 120 Hz, e soprattutto un secondo al minuto in cui la GPU lavora da
  quattro a otto volte tanto. La revisione elencava il cambio d'ora, l'aggiornamento e il
  nastro; il minuto non era nel conto. **Non toccato qui**: è una decisione (planare solo se la
  differenza si vede, o al passo del cielo), casella in Aperto.
- **Il nastro trascinato** è il momento più caro, ed è breve: a ogni ora `skyOf` gira sul thread
  dell'interfaccia (di notte con le stelle del catalogo e la loro bitmap da ridisegnare e
  ricaricare), e la planata riparte, quindi lo shader disegna a ogni vsync per tutto il
  trascinamento.

### Le icone stanno ferme mentre si trascina il nastro

Con il cielo ridisegnato a ogni frame e le icone in loop sotto, la pagina sfarfallava. Per la
durata del trascinamento le icone mostrano il disegno fermo, come per uno scorrimento
(`LocalMotionPaused`); tornano quando il dito si alza.

- **Il cielo no**: è la cosa che il dito muove. `ContentState` dà al canvas la risposta della
  pagina (`pageMoving`) e al foglio sotto quella con il nastro (`pageMoving || ribbonHeld`);
  passare dal canale degli scorrimenti (`PageScrolls`) avrebbe fermato anche il cielo e tolto la
  planata fra un'ora e l'altra.
- **Solo sotto un dito**: un'ora raggiunta con le due azioni di accessibilità resta senza
  nessuno che trascina, e lì la pagina è a riposo. Se il canvas esce con il dito ancora giù,
  la pagina viene liberata.
- DESIGN §7.1, «And while the ribbon is dragged».

### Il cielo animato ha un interruttore suo

Chiesto «se sei d'accordo, onestamente», e la risposta è sì, per due motivi. Il cielo si muoveva
sotto «Icone animate», la cui etichetta e la cui nota parlano di icone soltanto: chi voleva il
cielo fermo non poteva sapere che l'interruttore era quello, e chi spegneva le icone perdeva il
cielo senza che nessuno glielo dicesse (§1.1 a rovescio: un interruttore che fa più di quel che
dice). E i due non costano uguale: le icone sono una dozzina di loop vettoriali, il cielo è
uno shader su tutto il canvas, la sola cosa in movimento che un telefono non recente può non
reggere, e la sua cifra su un telefono è ancora da leggere.

- **Impostazioni → Aspetto → «Cielo animato»**, sotto «Icone animate», acceso di fabbrica
  (`animatedSky`, chiave `appearance_animated_sky`, `LocalAnimatedSky`). La nota dice cosa si
  muove e a chi serve spegnerlo.
- **Vale per il canvas di Oggi e per la card Stanotte di Cielo**: è lo stesso shader con lo
  stesso costo, e un «cielo» fermo in una scheda e mosso nell'altra sarebbe stato un
  interruttore che fa meno di quel che dice. La richiesta nominava l'hero di Oggi: è una scelta
  mia, da confermare.
- **Un interruttore acceso che dice «animato»**, non uno spento che dice «disattiva»: è la
  forma di quello delle icone, sopra di lui, e il valore di fabbrica è quello chiesto.
- **Spento**: un frame fermo dello stesso cielo, ridisegnato quando il cielo cambia; il nastro
  trascinato mostra ancora il cielo di ogni ora, senza planata.
- **Le due cose sono indipendenti**: chi aveva spento le icone ritrova il cielo in movimento
  (nessuna compatibilità con le installazioni di prima, com'è la regola della beta).
- **Cambia** «Punto 6: il cielo con uno shader AGSL» (archivio 05, `05-ottobre-1-19.md`, 15 ott
  2026), dove il cielo stava sotto «il meteo che si muove», lo stesso interruttore delle icone.
  DESIGN §3.5 e §7.1 («Under a switch of its own»); la guida nomina il cielo fra le cose
  dell'aspetto, IT/EN.

### Il luogo cambiato da Cielo o da Avvisi tornava quello di prima

**Il difetto.** Da Cielo o da Avvisi, aprire il foglio Luoghi e sceglierne un altro; tornando su
Oggi la pagina era ancora quella del luogo di prima, a volte dopo un mezzo viaggio verso il
luogo nuovo e ritorno, e il luogo attivo tornava quello di prima anche per le altre due schede.

**La causa.** Oggi esce dalla composizione mentre è su un'altra scheda, e il suo pager era
salvato (`rememberPagerState`): al rientro tornava sulla pagina dov'era stato lasciato. Il
settle, appena partito, riportava quella pagina come «il lettore si è fermato qui» e
`setActive` riscriveva il luogo di prima sopra quello appena scelto. Intanto il ramo che segue
la selezione aveva già fatto partire il pager verso il luogo nuovo, e la scrittura gli cambiava
il bersaglio a metà corsa: i ridisegni. In più il `PagerModel` smetteva di ascoltare lo store
cinque secondi dopo l'uscita di Oggi (`WhileSubscribed`), quindi al rientro il primo modello era
quello vecchio, d'accordo con il pager vecchio.

**La correzione.**

- **Il pager non è più salvato** (`PagedToday`): nasce ogni volta sulla pagina del luogo attivo
  dello store. Il luogo attivo lo tiene lo store; una seconda copia poteva solo non essere
  d'accordo. Vale anche dopo una rotazione: lo store è scritto a ogni settle.
- **La pagina su cui il pager nasce non è una selezione**: è dove è stato messo, non dove il
  lettore è andato, e non viene riportata; solo con niente di attivo dice allo store qualcosa
  che lo store non sa. Il fix della posizione all'arrivo resta quello della ripresa
  (`onResumed`), che un effetto entrato in una schermata già ripresa riceve subito.
- **Il `PagerModel` ascolta finché vive il ViewModel** (`SharingStarted.Lazily`): un file di
  preferenze che parla solo quando viene scritto, niente da interrogare. Al rientro il modello
  è quello giusto dal primo frame e il pager nasce sul luogo nuovo, senza viaggio.
- **Cambia** una frase di «Il pager fermo a metà dopo aver acceso la posizione» (archivio 06,
  `06-ottobre-20-22.md`, 20 ott 2026): il settle resta letto per chiave, ma il primo non è più
  riportato.

### Come è stato verificato

- `PagedTodayFollowTest`, due casi nuovi. «Tornare sulla scheda non riscrive il luogo di prima»:
  il pager dentro un `SaveableStateProvider`, tolto e rimesso come fa la shell con una scheda
  non a schermo; **sul codice di prima fallisce con `[city:2, gps]`**, il luogo vecchio scritto
  per primo. «Uno swipe è ancora una selezione». Il caso del 20 ott 2026 ora si aspetta il solo
  settle della posizione.
- `SettingsStoreTest` (il valore di fabbrica, il giro di scrittura) e `SettingsScreenTest`
  (l'interruttore del cielo c'è e non tocca le icone).
- Le icone ferme sotto il dito e il rientro senza viaggio sono cose che solo il telefono
  conferma: casella in Aperto.
- Lo screenshot delle impostazioni del README è rigenerato e guardato.

## Fase 16 — Cielo: la pagina come Oggi e il cielo da esplorare (committente, 10 nov 2026)

Chiesta dopo il lavoro sull'hero di Oggi: una revisione della schermata Cielo, coerente con
Oggi, e l'hero del cielo di notte portato a un livello premium «con feature utili e non
rumore», con più libertà di esplorare il cielo notturno. La proposta è un canvas di cinque
tavole (https://claude.ai/artifact/KeiYGuJ62PvmqxasJnSusH, privato, del committente),
**approvata per intero** il 10 nov 2026: «fai un piano per l'implementazione completa e
procedi; se è meglio in più chat dimmelo». È meglio: undici step, uno per chat, come le Fasi
14 e 15, perché ogni step è una PR che si guarda da sola e l'esploratore ha bisogno di tre
pezzi di motore che non si vedono prima di avere uno schermo.

### La revisione: cosa non va oggi

1. **Il cielo è la cosa più piccola della schermata.** Sta in una card rientrata di 16dp, alta
   224dp, e sopra di lui si contendono lo spazio il titolo, le nuvole, «Verso sud alle 01:15»,
   «Dietro le nuvole» e i nomi delle stelle; lo scrim ne copre il 60%.
2. **L'intestazione non è quella di Oggi.** Su Oggi il luogo sta sul cielo; qui c'è un
   `TabHeader` «Cielo» sopra una card.
3. **Il pulsante «Aggiungi un momento» copre quel che il lettore aspetta**: nello screenshot
   del README sta sopra l'orario e la direzione del prossimo momento.
4. **Lo stesso momento è disegnato in due modi**: `TimelineRow` su Oggi (l'ora a sinistra),
   `SkyRow` e `NextMomentCard` qui (l'ora a destra, o in grande in una card sua).
5. **Si esplora poco**: si gira solo di lato, fra 10° e 65° di altezza (`StarCameraTopDeg`,
   `StarCameraBottomDeg`), niente sopra i 70°, senza avvicinare, solo stanotte, due nomi di
   stelle al massimo, e toccare un oggetto non fa niente.

### Le cinque tavole, a parole (per la chat che non apre il canvas)

1. **Cielo, in cima alla pagina.** Il cielo a tutta larghezza fino al bordo alto, come il
   canvas di Oggi; la riga del luogo sul cielo («Milano ⌄», sotto «Stanotte, martedì 10
   novembre», l'ingranaggio a destra); sotto la riga, a destra, la pillola «Esplora»; in basso
   a sinistra, dove Oggi ha la temperatura, **il verdetto della notte** (il segno e la parola,
   40sp Regular, nel colore del verdetto), a destra sulla stessa riga «Verso sud-est · 00:30»;
   poi «Nuvole 12% · la luna è già tramontata», «Buio dalle 18:42 alle 05:24»; poi **il nastro
   della notte** nella forma del nastro di Oggi (una barra da 8dp dal crepuscolo all'alba, le
   ore di nuvole in grigio, il pallino, i due orari sotto); poi il foglio con il bordo
   arrotondato sopra il cielo. Sul foglio «I prossimi momenti» con «+ Aggiungi» nel titolo.
2. **Cielo, scorrendo.** La barra fissata porta il luogo, la data della notte e il verdetto
   in piccolo (come quella di Oggi porta la temperatura). I momenti sono le righe dell'agenda
   di Oggi: l'ora a sinistra su due righe per un intervallo, l'icona, il nome con la riga
   sotto, la campanella a destra, il chip del verdetto sotto; raggruppati «Stanotte e
   domattina», «Domani sera». «In arrivo»: le righe di oggi con «tra N giorni», e sotto un
   evento che ha una notte sua il rimando «Guarda il cielo di quella notte».
3. **Esplora il cielo.** A tutto schermo, senza la barra delle schede: dall'orizzonte allo
   zenit, si trascina in ogni direzione e si avvicina (pizzico, e due pulsanti + e −). In
   alto: chiudi, «Verso sud-est», l'elenco «Da vedere». L'orizzonte è una riga con le
   direzioni scritte sopra. Tre interruttori: «Costellazioni» (linee e nomi), «Dietro le
   nuvole», «Punta il telefono». In basso un pannello: l'ora in grande con le nuvole di
   quell'ora, «Adesso», il nastro della notte da trascinare, e la notte («‹ Stanotte,
   martedì 10 novembre ›», che apre la scelta di un'altra notte).
4. **Un oggetto toccato.** Il cielo si avvicina sull'oggetto, cerchiato; un foglio dice il
   nome, cos'è e dov'è a parole («Pianeta · a sud, a metà cielo»), il chip («Si vede bene ·
   nuvole 12% alle 22:30»), tre caselle (Sorge, Più alto, Tramonta, ognuna con l'ora e la
   direzione), una frase su fino a quando conviene e su cosa si vede a occhio nudo, il
   pulsante «Avvisami le sere in cui si vede bene» e «Cos'è, in una pagina».
5. **Da vedere, e in che notte.** Un foglio: in cima la notte (Stanotte, Domani, la notte di
   un evento, il calendario); se la previsione non arriva a quella notte, una nota lo dice
   («il cielo è disegnato senza nuvole»); poi l'elenco di quel che vale la pena, ognuno con
   quando e dove a parole; un tocco gira il cielo lì, alla sua ora migliore.

### Decisioni già prese (non si richiedono)

- Le cinque tavole come sono. **Fuori, perché rumore**: la fotocamera in realtà aumentata, i
  cataloghi di oggetti deboli, i satelliti (un'altra fonte, un altro fornitore che si muove),
  le figure mitologiche disegnate, punteggi e medaglie. Mercurio resta fuori: si vede pochi
  giorni l'anno, basso nel crepuscolo, e una riga che dice quasi sempre «non si vede» è rumore.
- **Il verdetto è la cifra dell'hero di Cielo**, il nastro della notte ha la forma del nastro
  di Oggi, «Aggiungi» sta nel titolo della sezione e il pulsante flottante se ne va.
- **L'hero resta notturno nei due temi** (lo è già: `ChiaroTheme.nightColors`); il foglio
  segue il tema. Le tavole sono disegnate nel tema scuro e in Figtree per necessità del
  canvas: l'app tiene il suo carattere.
- **Di giorno l'hero mostra la notte che arriva**, all'istante che la card usa già (adesso se
  è notte, altrimenti la metà della notte): la riga «Verso … · HH:MM» dice di che ora è.
- **Niente richieste nuove al fornitore**: tutto è astronomia calcolata sul telefono o la
  previsione che c'è già. Il peso del fetch resta 8,4 chiamate.
- **Niente compatibilità da tenere** con le installazioni di prima (la regola della beta).
- **Niente gergo**: non si stampano ascensione retta, declinazione, magnitudine né gradi;
  dove e quanto in alto si dicono a parole («a sud, a metà cielo»), come le direzioni già oggi.

### Le regole comuni

- Uno step per chat, da `origin/main` aggiornato, su un ramo `fase-16-step-N-<nome>`; alla
  fine la sezione dello step in coda a questo registro (cosa è cambiato, decisioni e
  deviazioni, come è stato verificato), la casella chiusa in «Aperto», la PR; il committente
  la unisce.
- **Lo schermo non mente**: ogni cosa disegnata sta dove il calcolo la mette; oltre la
  previsione il cielo è senza nuvole e lo dice; l'inquinamento luminoso non è modellato e la
  guida lo dice una volta.
- **La batteria**: l'esploratore disegna quando il dito lo muove, non a ogni frame; i sensori
  si accendono solo con «Punta il telefono» e si spengono quando lo schermo esce.
- Ogni regola nuova del motore ha il suo test con numeri presi da un'effemeride (JPL Horizons)
  scritti nel test con la data in cui sono stati letti.
- Gli screenshot del README: `sky-tonight.png` e `sky-ahead.png` si rigenerano negli step che
  li cambiano, e si guardano; uno nuovo per l'esploratore alla fine (16.11).
- DESIGN e la guida (IT/EN) si aggiornano nello step che cambia quel che dicono, non alla fine.
- Il registro passa le 1 000 righe durante la fase: si archivia (archivio 11) come commit a sé
  all'inizio dello step che lo trova oltre, come dice «Come è fatto».

### Gli step

**16.1 — La pagina come Oggi: l'hero.** `TonightCard` diventa il canvas in cima alla pagina:
a tutta larghezza fino al bordo alto (`SkyCanvas` e `SkySheet`, oggi usati solo da
`TodayHero`), la riga del luogo sul cielo e fissata sopra la lista come su Oggi (il
`TabHeader` resta ad Avvisi), il verdetto come cifra, il nastro della notte nella forma del
nastro (`NightStrip` ridisegnato: stesse letture, stesso trascinamento, stesse azioni di
accessibilità), la barra fissata con il verdetto in piccolo quando l'hero è uscito, la pillola
«Esplora» (che fino al 16.6 non c'è: si disegna con lo step che le dà una destinazione).
**Non cambia quel che il cielo disegna**: lo stesso `WindowScene.frame`, su un palco più alto.
Tutti gli stati di oggi restano: senza finestra, notte di luna, mai del tutto buio, buio
tutto il giorno, senza coordinate (il gradiente e il distintivo della luna), le due card dei
promemoria che non possono suonare, la pagina a due pannelli. Tocca `TonightStageTest`,
`TonightNightTest`, `WindowTurnTest`, DESIGN §8.8b e §8.12b, `sky-tonight.png`.

**16.2 — I momenti come l'agenda di Oggi.** `MomentRow`, `NextMomentCard` e `SkyRow` prendono
la forma di `TimelineRow` (l'ora a sinistra, il verdetto sotto) con la campanella che a
`TimelineRow` manca; il prossimo momento resta in testa con la sua riga «Adesso · ancora …»
o «tra …», non più una card diversa. «+ Aggiungi» nel titolo della sezione al posto del
pulsante flottante (lo `Scaffold` resta per lo snackbar di «Annulla»). «In arrivo» tiene le
sue righe; il rimando «Guarda il cielo di quella notte» arriva con il 16.9. Tocca
`SkyUndoTest`, `SkyStaleVerdictsTest`, DESIGN §8.4 e §8.8, i due screenshot del Cielo.

**16.3 — Il motore: Marte e Saturno, e quando un oggetto sorge, culmina e tramonta.** Solo
`:core:domain`, niente a schermo. `Planet` e `SkyBody` oggi sono Venere e Giove
(`PlanetMath` dice perché gli altri erano fuori); entrano Marte e Saturno con i loro test
contro l'effemeride. **Trappola**: `planetAbove` manda ogni pianeta che non è Venere su
`Body.JUPITER` (AstronomyEngine, riga 371). Una funzione pubblica per il passaggio in cielo di
una notte: sorgere, punto più alto e tramonto di un pianeta, della luna (`lunarDay` dà già
sorgere e tramonto, non il culmine; `transit()` è privata e serve solo al mezzogiorno) e di un
punto fisso del cielo (`starHorizontal` c'è già); e «fino a quando conviene», l'ultima ora in
cui sta sopra una certa altezza al buio. `StarField.planets` li disegna già tutti perché gira
su `Planet.values()`: la grandezza e il calore di Marte e Saturno si aggiungono lì, e l'hero
di Oggi, la finestra e la card li mostrano da questo step.

**16.4 — Il motore: un'altra notte, e cosa vale la pena guardare.** `Tonight` porta una notte
sola, ma `SkyScheduler.darkNight(date, …)` accetta qualunque data: `SkyStateBuilder` impara a
costruire la notte di una data (le ore di nuvole se la previsione, 7 giorni, ci arriva;
altrimenti nessuna, e lo stato lo dice). E l'elenco «Da vedere» come dato puro e testato: per
una notte e un luogo, gli oggetti che meritano (la luna con la sua fase, i pianeti su, le
costellazioni e gli ammassi che si riconoscono a occhio, la Via Lattea se il buio c'è, il
radiante di uno sciame), ognuno con la sua ora migliore, la direzione e l'altezza a parole, in
ordine d'ora. Le regole di cosa entra e cosa no sono scritte con il loro motivo, come gli
stati. Niente a schermo.

**16.5 — Le costellazioni: il dato.** Il catalogo (`BrightStars`: 5 080 stelle fino alla
magnitudine 6, solo posizione, luminosità e colore) non ha nomi né figure, e i nomi delle
stelle sono 18 in `StarNames.kt`. Lo step **comincia scegliendo la fonte delle linee con la
sua licenza** (Chiaro è GPL-3: candidate la cultura «moderna» di Stellarium e le linee di
d3-celestial, da leggere alla fonte quel giorno) e la scrive nei crediti delle Impostazioni e
del README. Uno strumento `tools/gen_constellations.py` scrive una risorsa binaria accanto a
quella delle stelle (le linee come coppie di punti, il punto dove scrivere il nome), i nomi
IT/EN delle 88, più nomi di stelle (le più luminose, una trentina), e i test della risorsa
come `StarCatalogueResourceTest`. Niente a schermo.

**16.6 — L'esploratore: la camera libera e il cielo a tutto schermo.** Una camera sua
(`SkyCamera`, pura e testata: dove guarda in direzione e in altezza, quanto è larga; da cielo
a schermo e ritorno, con una proiezione che regge lo zenit), perché quella di oggi è una
fascia fissa cablata nello shader (il banco a 45°–15°, `uBand`, la texture delle stelle in
coordinate di schermo; `SkyDeckTest` ne tiene le righe) e **l'esploratore non passa dallo
shader del meteo**. Una pagina a tutto schermo (una chiave di navigazione nuova, che copre la
barra: `showsBottomBar` oggi la nasconde solo per Impostazioni e guida) aperta dalla pillola
«Esplora» dell'hero: le stelle fino alla luminosità che lo zoom regge, i pianeti, la luna con
la sua fase, la Via Lattea, l'orizzonte con le direzioni, i nomi; trascinare in ogni
direzione, pizzicare, i due pulsanti. Disegna quando la camera cambia e basta. All'istante
dell'hero, senza ancora il pannello del tempo.

**16.7 — L'esploratore: l'ora, la notte, le nuvole, le costellazioni.** Il pannello in basso:
l'ora con le nuvole di quell'ora, «Adesso», il nastro della notte da trascinare, la notte
prima e dopo e la scelta di una data (16.4). Le nuvole dell'ora nell'esploratore **da
decidere nello step, guardando**: un velo uniforme per la copertura (le stelle si spengono
come nell'hero) è il minimo onesto; il banco dello shader su una camera libera è un lavoro a
parte. «Dietro le nuvole» è la preferenza che c'è già (`skyBareSky`). Oltre la previsione il
cielo è senza nuvole e una riga lo dice. «Costellazioni» accende linee e nomi (16.5), scelta
ricordata.

**16.8 — L'esploratore: toccare un oggetto.** Il tocco prende l'oggetto più vicino entro 24dp
(pianeti, luna, stelle con un nome, il nome di una costellazione, il radiante); la camera lo
centra e il foglio dice quel che dice la tavola 4, con i numeri del 16.3. La campanella c'è
solo dove il catalogo dei momenti ha un lavoro per quell'oggetto: per Marte e Saturno i
lavori entrano qui, con la loro pagina nella guida degli eventi; una stella non ha
campanella né pagina, e il foglio non le finge.

**16.9 — «Da vedere», e la notte di un evento.** Il foglio della tavola 5 sull'elenco del
16.4: la notte in cima, la nota quando la previsione non arriva, le righe; un tocco porta la
camera sull'oggetto alla sua ora migliore. Da «In arrivo» il rimando «Guarda il cielo di
quella notte» apre l'esploratore su quella notte, verso il radiante o l'oggetto dell'evento.

**16.10 — «Punta il telefono».** `CompassSensor` oggi dà solo la direzione
(`CompassMath.heading`, telefono piatto o dritto): serve anche l'altezza a cui punta il dorso
del telefono. Acceso, il cielo segue il telefono; il dito lo spegne. Le frasi sulla bussola
disturbata ci sono già. Sensori accesi solo con l'interruttore e la pagina a schermo. **Si
prova sul telefono prima di tenerlo**: se la bussola non regge un cielo che non balla,
l'interruttore esce e resta scritto perché.

**16.11 — La chiusura.** La guida (IT/EN) riletta per intero sul Cielo, VISION §5.3, il
README (uno screenshot dell'esploratore, il testo), il CHANGELOG, DESIGN riletto dove il
Cielo è cambiato, e la rilettura degli undici step contro questa sezione, come per la Fase 15.

### L'ordine, e cosa può andare in parallelo

Prima quel che si vede e non rischia (16.1, 16.2), poi il motore e il dato che l'esploratore
consuma (16.3, 16.4, 16.5), poi l'esploratore un pezzo alla volta (16.6–16.10), poi la
chiusura. 16.3–16.5 toccano solo `:core:domain` e `tools/` e non incrociano 16.1–16.2: chi
ha fretta può aprirne uno mentre l'altra PR è in revisione, e l'unico conflitto è la coda di
questo registro. Dal 16.6 in poi ogni step sta sul precedente.

## Il cielo non plana a ogni minuto, e un cielo fermo non si ridisegna (committente, 10 nov 2026)

Chiude la casella «La planata del cielo a ogni minuto», aperta rileggendo il costo dell'hero
(sezione «L'hero dopo la revisione», «Quanto costa l'hero rispetto a prima»). Il committente:
«puoi sistemarla in modo che sia ottimizzata e non disegni se non cambia niente?». Due cose,
la seconda trovata cercando la prima.

- **Una planata solo per un cambiamento che si vede** (`skyDistance`, `GlideFrom`,
  `SkyTransition.moveTo`). La pagina consegna al canvas un cielo nuovo a ogni minuto, il sole
  un quarto di grado più in là, e ognuno faceva partire una planata: un secondo di shader a
  ogni frame dello schermo, sessanta volte l'ora. Ora un cielo nuovo che dista meno di un
  ventesimo da quello a schermo è semplicemente lì; da un ventesimo in su è meteo, o un'ora
  trascinata, e il canvas plana come prima. La distanza è la differenza più grande fra i due
  cieli, ognuna nella sua unità: un colore per canale, una quantità per sé stessa, il posto di
  un corpo per la sua parte del canvas, un angolo per la sua parte di mezzo giro; un corpo che
  c'è in un cielo solo conta per quanto si vede. Si misura da quel che è a schermo, quindi
  una planata ancora in corso prosegue verso il cielo nuovo e non viene tagliata.
- **La soglia è delle due scelte in «Aperto» la prima** (planare solo quando si vede), non il
  passo del cielo: a 15 frame al secondo la planata di ogni minuto sarebbe costata un quarto,
  e sarebbe stata ancora lavoro per niente.
- **Misurato** (`SkyTransitionTest`, il sole da 60° a −40° a quarti di grado, cioè un minuto
  l'uno all'equatore e un minuto e mezzo a Milano): dei 400 passi ne planano **13 con il cielo
  sereno, 5 con il 40% di nuvole, 1 con il cielo chiuso**, tutti fra 6° e −5°: dove la luce
  diventa dorata, dove il sole tramonta, dove escono i pianeti. Prima planavano tutti e 400.
- **Un cielo in cui niente si muove è un frame solo** (`SkyScene.moves`, `SkyShaderLayer`). Lo
  shader legge l'orologio in otto punti, ognuno dietro una quantità: il banco, il velo, il
  tremolio delle stelle, la meteora, il lampo, la nebbia, la pioggia, la neve. Un cielo che
  non ne ha nessuna (un giorno sereno, un crepuscolo sereno con i soli pianeti) è la stessa
  immagine a ogni istante, e veniva disegnato quindici volte al secondo lo stesso: il caso
  più comune di tutti era quello che lavorava di più per niente. Ora l'orologio non gira.
- **Non cambia** quel che si vede: le planate vere (il cambio d'ora del meteo, un
  aggiornamento, il nastro trascinato) sono quelle di prima, e la card Stanotte passa dallo
  stesso `SkyShaderLayer`, quindi ne prende la seconda metà.

**Come è stato verificato.** `SkyTransitionTest`: un minuto di sole a mezzogiorno e uno di
notte stanno sotto la soglia; dieci punti di nuvole, una nebbia che arriva e il sole che
tramonta stanno sopra; e il conto della giornata qui sopra. `SkySceneTest`: cosa muove un
cielo e cosa no. `SkyShaderPixelTest`: un mezzogiorno sereno e un crepuscolo con Venere
disegnati a due istanti danno gli stessi pixel, un banco no. DESIGN §3.5 e §3.9. Sul telefono
resta da guardare con la casella dell'hero (Aperto): che al tramonto il cielo non scatti.

## 16.1 — La pagina come Oggi: l'hero (committente, 10 nov 2026)

«Esegui lo step 16.1 della Fase 16». Ramo `claude/fase-16-step-1-hero`, da `origin/main` dopo
la PR #152.

### Cosa è cambiato

- **La notte è il canvas su cui la pagina si apre** (`TonightHero`, era `TonightCard`): lo
  stesso `SkyCanvas` di Oggi con il foglio della pagina appoggiato sul fondo (`SkySheet`), a
  tutta larghezza fino al bordo alto, notturno nei due temi. Alto **la pagina meno 144dp**
  (`skyHeroHeight`: il titolo della sezione e l'inizio del primo momento restano in vista), mai
  meno del canvas di Oggi, mai più di 640dp. Sul telefono di riferimento 608dp, dove la card
  dava al cielo 224.
- **La riga del luogo è quella di Oggi** (`PlaceHeader`), fissata sopra la lista: bianca sul
  cielo a pagina ferma, la carta della pagina dal primo pixel di scorrimento, nei 300 ms di
  Oggi. Sotto il luogo, **la notte di cui parla la pagina con la data della sua sera**
  («Stanotte, martedì 10 novembre», la forma più lunga che entra): dopo mezzanotte la notte in
  corso è ancora quella cominciata ieri. Il `TabHeader` resta ad Avvisi.
- **Il verdetto è la cifra dell'hero**: il segno e la parola a 40/48 Regular (`heroVerdict`,
  un ruolo nuovo in `ChiaroType`, DESIGN §5) nell'inchiostro del verdetto, dove Oggi ha la
  temperatura; sotto, il numero che l'ha deciso con la maiuscola («Nuvole 12%»), le ore di
  buio come frase («Buio dalle 18:42 alle 05:24»), quel che la luna si è presa e il tratto più
  sereno quando ci sono. Tutte le righe nel bianco pieno del canvas con il suo alone, come su
  Oggi (la card aveva le secondarie al 72%).
- **Il nastro della notte** (`NightRibbon`, era `NightStrip`) nella forma del nastro di Oggi:
  una barra da 8dp dal crepuscolo all'alba con il pallino da 14. Stesse letture, stesso
  trascinamento (subito in orizzontale, dopo la pressione lunga da fermo), stessi scatti,
  stesse azioni per lo screen reader, e l'ora che resta quando il dito si alza.
- **La barra porta il verdetto** quando quello dell'hero è scorso sotto (`TonightInBar`, lo
  stesso conto di Oggi: `heroUnderBar`): il chip del verdetto nel tema della pagina, o nelle
  notti senza finestra la parola che stava al suo posto; un solo annuncio, «Stanotte: Bello».
- **Cielo sta sotto il rail come Oggi** (`ChiaroRoot`, `LocalRailInset`): su una finestra larga
  la pagina prende tutta la larghezza, il cielo corre sotto la barra di stato anche sopra il
  rail, e le parole dell'hero stanno sul primo pannello. `paneItems` tiene libero il rail anche
  nella colonna sola.
- **Quel che il cielo disegna non è cambiato**: lo stesso `WindowScene.frame`, la stessa
  `WindowScene.labels`, lo stesso giro col dito.
- **Tutti gli stati restano**: senza finestra («Notte di luna», «Mai del tutto buio», «Buio
  tutto il giorno», una parola al posto del verdetto, bianca e senza segno), senza coordinate
  (il gradiente della notte e il distintivo della luna, ora sotto la riga del luogo), le due
  card dei promemoria che non possono suonare (sulla carta, sotto il canvas, come i banner di
  Oggi), i due pannelli.

### Decisioni e deviazioni

1. **L'immagine si ferma dove cominciano le parole** (`SkyCanvas`, parametro `stage`), e le
   parole stanno sul colore di fondo del cielo, quello che l'ultima riga dell'immagine ha già,
   con gli ultimi 40dp dell'immagine che ci sfumano dentro. **Deviazione dalla tavola 1**, che
   ha le parole sopra le stelle come Oggi. Il motivo è la camera: quella di Oggi mette il sole
   e la luna in una fascia che la colonna tiene libera, questa li mette dove stanno davvero.
   Misurato sul telefono di riferimento: una camera alta quanto il canvas (608dp) stende i suoi
   55° a 11dp per grado su 34° di larghezza, e i 240dp di parole e di foglio coprono tutto
   quel che sta **sotto i 32°**. Lo sguardo va sulla luna ogni volta che è su, quindi una luna
   sotto i 32° (a giugno la luna piena a Milano non passa i 27° in tutta la notte), Venere
   ogni sera in cui è ancora su al buio e un radiante basso a est sarebbero finiti dietro il
   verdetto. Fermata alle parole, la camera stende i 55° su 360–410dp (6,5dp per grado, 54° di
   larghezza al minimo) e niente di quel che disegna sta sotto una parola. La riga del luogo
   ne copre i dieci gradi più alti, come faceva il titolo della card. Il prezzo: sotto le
   parole non ci sono stelle né nuvole, perché lì non ne è stata calcolata nessuna.
2. **«Guardando a … alle HH:MM» sta al piede dell'immagine, a sinistra**, non a destra del
   verdetto come nella tavola. A 40sp «Niente da fare» con il suo segno occupa 285 dei 328dp
   di un telefono da 360 (misurato sul rendering in italiano), e la riga della direzione ne
   vuole 150 e più: accanto alla parola non entra, e un posto che c'è solo con «Bello» non è
   un posto. Al piede sta sotto il cielo di cui parla, con «Dietro le nuvole» all'altro capo.
   La frase è quella che c'è (`window_looking`, la stessa del widget), non l'abbreviazione
   della tavola.
3. **L'ora tenuta è un chip sul palco**, al posto della riga della direzione, con le stesse
   parole e la ×. Sulla card stava sopra la striscia; fra le parole le avrebbe alzate di una
   riga al primo tocco, il palco si sarebbe accorciato e tutta l'immagine si sarebbe riscalata
   sotto il dito. La lettura dell'ora («02:05 · nuvole 5% · senza luna») prende la riga sotto
   il verdetto, una riga per una riga come sul nastro di Oggi. La riga «Trascina per vedere
   ogni ora» è uscita con il suo posto: il nastro di Oggi non ne ha, e la guida lo dice.
4. **Cosa dice il nastro, in 8dp.** Il colore: la notte (lo stop di mezzo della tavolozza per
   l'altezza del sole, portato del 12% verso il bianco perché su una notte del suo stesso
   colore la barra non aveva forma proprio dove la notte è migliore: visto sul rendering),
   **paglia al 28%** dove la luna è su secondo la lettura stessa (`nightReading`: la barra non
   lava mai un istante che la lettura chiama senza luna), **grigio fino al 38%** con le nuvole
   di ogni ora, lette fra le ore come le legge l'immagine. Un'ora che la previsione non copre è
   senza nuvole. Il pallino sta **sull'istante che l'immagine disegna** (adesso a notte in
   corso, la metà della notte prima, il dito quando tiene un'ora, con «adesso» come anello);
   la notte già passata è al 60%, come il giorno. Restano la riga sotto il tratto più sereno e
   i due orari della finestra sotto i suoi bordi. **Non ci stavano e sono usciti** le tacche
   delle ore, le nuvole appese, le stelle sparse, la cornice della finestra e il disco della
   luna: quel che dicevano lo dicono il colore, i due orari e le frasi.
5. **«La luna è già tramontata» non c'è.** La tavola la scrive accanto alle nuvole; lo stato
   sa quando la luna tiene l'inizio o si prende la fine della notte, non se è tramontata prima
   o sorge dopo. Restano le due frasi che c'erano, quando valgono.
6. **Senza luogo resta il `TabHeader`**, con il solo nome della scheda: non c'è un cielo su cui
   appoggiare una riga del luogo, e la pagina è lo stato vuoto delle tre schede.
7. **«Dietro le nuvole» resta dov'era**, al piede dell'immagine. La pillola «Esplora» non è
   disegnata: arriva con il 16.6, che le dà una destinazione.
8. **Il pulsante flottante c'è ancora** e copre un pezzo della prima card dei momenti: se ne
   va con il 16.2, che è il suo step. La sua etichetta ora si chiude quando il canvas esce.
9. **Il cielo sta fermo mentre la pagina scorre** (`LocalMotionPaused`), come su Oggi (DESIGN
   §7.1): la card non lo faceva.
10. **Le icone della barra di stato seguono la pagina arrivata per ultima** (`StatusBarIcons`).
    Ora due pagine hanno un cielo dietro la barra di stato, e un cambio di scheda le tiene a
    schermo tutte e due per la durata della transizione: quella che usciva riconsegnava la
    barra al tema dopo che quella che entrava l'aveva presa, e l'orologio restava scuro su un
    cielo di notte. Ogni pagina tiene una richiesta finché è a schermo.
11. **A 150% di carattere e oltre** la riga della direzione e «Dietro le nuvole» stanno una
    sopra l'altra (DESIGN §10): fianco a fianco al chip restavano tre parole di larghezza,
    una per riga (visto sul rendering a 360dp).
12. **Stringhe**: nuove `sky_hero_night` e `sky_bar_tonight`; tolte `sky_scrub_hint`,
    `sky_tonight_window_label` e `sky_tonight_dark_label`, senza più un posto. La guida
    (`guide_sky_tonight_body`, IT e EN) è riscritta su quel che c'è ora.

### Come è stato verificato

- `TonightNightTest`: i colori del nastro (la luna secondo la lettura, le nuvole dell'ora,
  nessuna nuvola dove la previsione non arriva), l'altezza del canvas fra i suoi due limiti,
  la data della notte e le sue tre forme in italiano e in inglese. Il test delle tacche è
  uscito con le tacche.
- `TonightStageTest`: i cinque di prima sul canvas nuovo (lo sguardo sulla luna e la luna dove
  la camera la mette, il dito sul nastro che muove l'ora e non la camera, l'ora che resta e la
  × che ridà la notte, «Dietro le nuvole», il giro col dito), più uno: le parole stanno sotto
  l'immagine, e l'immagine è più dei sei decimi del canvas.
- `SkyHeroPageTest`, nuovo: il luogo e la data della notte sono un solo bersaglio sul cielo e
  il titolo «Cielo» non c'è più; scorrendo la barra prende la parola della notte e il luogo
  resta; la barra di stato segue la pagina arrivata per ultima e torna al tema quando non ce
  n'è più nessuna.
- `SkyMutedTest` gira sullo schermo del telefono di riferimento: la card sta sotto il canvas,
  lì in vista, e una lista pigra compone solo quel che è in vista.
- `sky-tonight.png` e `sky-ahead.png` rigenerati e guardati.
- Guardati e non tenuti (una classe di prova in `testDebug`, cancellata): una notte serena con
  un verdetto, la luna che tiene l'inizio e un tratto più sereno; «Niente da fare» in italiano
  su 360dp nel tema chiaro; il 150% di carattere su 360 × 640; il foldable aperto nei due
  temi; un'ora tenuta; la pagina scorsa con il chip nella barra. La registrazione di Milano
  non ha una notte con un verdetto (luna piena per tutta la settimana), quindi quella notte è
  costruita: per guardarla, non per la documentazione.
- DESIGN §8.8b riscritta; §5 (`heroVerdict`), §8.1b, §8.12b, §8.12c e §3.9 allineate; README
  (i due testi alternativi) e CHANGELOG.
- La suite intera verde (`./gradlew test :app:testDebugUnitTest :app:lintDebug`: 3 013 test,
  15 saltati, nessuno fallito) e il lint verde, in locale.

### Cosa conferma solo il telefono

Casella in «Aperto». Le icone della barra di stato nel passaggio fra Oggi e Cielo, nei due
versi e nel tema chiaro; il nastro in una notte serena e senza luna, dove è una barra blu su
un cielo blu; il piede dell'immagine sotto un cielo coperto, dove le nuvole sfumano nel colore
di fondo; il dito sul nastro, che ha 8dp e una presa di 54, e sul cielo più alto, che ora gira
con una lunghezza focale più lunga.

## 16.2 — I momenti come l'agenda di Oggi (committente, 10 nov 2026)

«Esegui lo step 16.2 della Fase 16». Ramo `claude/fase-16-step-2-moments`, da `origin/main` dopo
la PR #153.

### Cosa è cambiato

- **Una riga sola per un momento, su Oggi e su Cielo** (`TimelineRow`, DESIGN §8.4): l'ora
  nella colonna a sinistra, la fine di un intervallo sotto il suo inizio, l'icona, il nome con
  la riga sotto, la campanella in fondo, il verdetto su una riga sua dal bordo della riga.
  `MomentRow` la usa per i momenti, `EventRow` per «In arrivo» senza la colonna dell'ora (il
  suo «quando» è una data, e resta nella riga sotto il nome). `NextMomentCard`, `SkyRow` e
  `SkyVerdictLine` non ci sono più; `Segment` (in `SegmentedGroup.kt`) è il segmento di un
  gruppo disegnato una riga alla volta, perché la lista del Cielo è pigra.
- **Il prossimo momento è la prima riga, non una card.** Il primo momento con un'ora
  (`momentsHead`) dice fra quanto arriva sopra il nome, nell'accento («Tra 6 h 20 min»,
  entro mezza giornata). Un momento in corso dice «Adesso» dove starebbe la sua ora, quanto
  ne resta sopra il nome («Ancora 25 min») e fino a quando nella riga sotto.
- **«Aggiungi» sta nel titolo «I prossimi momenti»**, un pulsante di testo con il suo più; lo
  screen reader sente «Aggiungi un momento». Il pulsante flottante non c'è più e la lista tiene
  16dp sotto l'ultima riga, non 88. Lo `Scaffold` resta per lo snackbar di «Annulla».
- **`TimelineRow` ha quel che la riga del Cielo aveva in più**: la campanella (`trailing`), la
  macchina fotografica dopo il nome (`mark`), cosa fa il tocco per lo screen reader
  (`onClickLabel`), la colonna dell'ora assente (`time` nullo) o vuota (stringa vuota).
- **La guida**: l'esempio di una riga del Cielo è disegnato dalla riga nuova; «I prossimi
  momenti» e «Aggiungi un momento» dicono quel che c'è ora (IT e EN).

### Decisioni e deviazioni

1. **I giorni restano «Oggi» e «Domani».** La tavola 2 intitola i gruppi «Stanotte e
   domattina» e «Domani sera»: all'ora della tavola (mezzanotte e mezza) «domani sera» è la
   sera della data di oggi. Sono parole per parti del giorno, e ne servono otto con le loro
   somme per due giorni; la data ne vuole due, sempre vere, le stesse del widget e di Oggi. Lo
   step non chiedeva di cambiare i gruppi. Se il committente vuole quelli della tavola è un
   lavoro suo, con la regola scritta.
2. **«Adesso» una volta sola.** La tavola scrive «Adesso» nella colonna e «Adesso · ancora 4 h
   54 min» sopra il nome. Su 360dp il testo ha 154dp accanto alla campanella e quella riga ne
   vuole circa 180: andava su due righe (visto sul rendering, «Adesso · ancora 25 / min»). La
   colonna dice «Adesso», la riga sopra il nome «Ancora 4 h 54 min».
3. **Le icone del Cielo scendono da 51 a 34dp**, il gradino dell'agenda, sui momenti e su «In
   arrivo». Il gradino da 51 (committente, 11 set 2026) era misurato su una riga senza colonna
   dell'ora: con l'ora a sinistra (52dp) e la campanella lasciava al nome 137dp su 360, 154 a
   34; e due misure della stessa famiglia nella stessa pagina erano due liste. Resta 51
   nell'indice della guida degli eventi, che non ha né l'ora né la campanella. **Da confermare
   sul telefono** (casella): è la decisione dello step che cambia una scelta del committente.
4. **Le righe senza ora stanno dopo le ore del loro giorno** (`dayOrder`): la fase della luna,
   che lo stato ordina al mezzogiorno del suo promemoria, e un momento che il cielo salta. Con
   l'ora in fondo alla riga non si notava; in una colonna di ore una riga senza ora non ha un
   posto, e in testa alla lista «La luna di oggi» stava dove si cerca il prossimo momento
   (visto sul rendering). La loro colonna resta vuota, così le icone del gruppo stanno in fila.
5. **«Tra …» solo sulla prima riga con un'ora, e solo se nessun momento è in corso.** Non «Il
   prossimo, oggi» come su Oggi: qui il giorno lo dice l'intestazione del gruppo.
6. **La macchina fotografica sta dopo l'ultima parola del nome**, sulla sua riga. In una
   colonna sua, accanto a un nome su due righe, stava a mezza riga da tutte e due; come
   segnaposto dentro il testo andava a capo da sola quando il nome riempiva la riga («Golden
   hour, evening» a 384dp, visto). Il nome è misurato nella larghezza meno il segno, e il
   segno è messo dopo l'ultima riga (`NameWithMark`).
7. **Oltre il 150% di carattere l'ora è una riga sopra il nome** (DESIGN §10, la regola delle
   righe a colonne), per ogni lista che usa `TimelineRow`, **quindi anche su Oggi**. Al 200%
   la colonna dell'ora da 104dp, l'icona e la campanella lasciavano al nome 90dp su 360: «La
   luna di oggi» con la sua riga era alta otto righe (visto).
8. **La colonna dell'ora è 52dp, era 48**, anche su Oggi: «Adesso» ne vuole circa 50 e a 48
   si spezzava in «Adess» e «o». Lo faceva già la riga degli avvisi che valgono adesso
   sull'agenda di Oggi, in italiano: è corretto anche lì (`today-day.png`, `today-now.png`).
9. **Il titolo con il pulsante sta dove sta un titolo senza.** Il pulsante è alto 48dp e la
   riga del titolo 24: è messo sulla riga e conta per la riga soltanto (`onTheLine`), sporge
   12dp sull'aria sopra e sotto e il dito lo trova tutto. Così su una finestra larga i primi
   titoli dei due pannelli restano sulla stessa riga.
10. **«fino al tramonto» e «dall'alba»**, che la tavola scrive sotto le ore dorate, non ci
    sono: la fine è nella colonna dell'ora, e sul Cielo il tramonto ha la sua riga subito sotto.
11. **`sky-ahead.png` parte da «Domani»**, non da «In arrivo»: senza gli 88dp del pulsante la
    pagina finisce prima che quel titolo arrivi sotto la barra, e l'immagine si apriva su mezza
    riga. Ora mostra un momento nella sua riga e poi il calendario.
12. **Il registro non è stato archiviato**: all'inizio dello step era a 908 righe, sotto le
    mille. Con questa sezione ci arriva: lo step che viene lo trova oltre e archivia (archivio
    11), come dicono le regole comuni.
13. **Stringhe**: nuove `sky_add` e `sky_left_minutes`, `sky_left_hours`,
    `sky_left_hours_minutes`; nessuna tolta (`sky_next_label` e `sky_add_moment` servono ancora).

### Come è stato verificato

- `SkyMomentsRowsTest`, nuovo, sette casi: l'ora all'inizio della riga e la fine sotto
  l'inizio, con «fino alle 19:15» per lo screen reader; la prima riga che dice fra quanto e
  nessuna card; il momento in corso con «Adesso» una volta, quanto resta e fino a quando; la
  riga senza ora dopo le ore, con il nome nella colonna degli altri; al 200% l'ora sopra il
  nome e l'intervallo su una riga; «Aggiungi» nel titolo, bersaglio da 48dp, sulla riga del
  titolo senza spostarla, che apre il catalogo; `momentsHead` e `dayOrder` come funzioni pure.
- `SkyCatalogResetTest` apre il catalogo dal pulsante nel titolo. `SkyUndoTest` e
  `SkyStaleVerdictsTest`, che lo step nominava, passano come sono: cercano il nome e l'ora
  di una riga, che ci sono ancora.
- `sky-tonight.png`, `sky-ahead.png`, `today-day.png` e `today-now.png` rigenerati e guardati.
  `widgets.png` e `widget-window.png` uscivano diversi di un migliaio di pixel senza che lo
  step tocchi i widget: lasciati come sono in `main`, da guardare a parte.
- Guardati e non tenuti (una classe di prova in `testDebug`, cancellata): la pagina in inglese
  a 384dp con i quattro momenti di default e con sei più un'eclissi seguita; in italiano a
  360dp nel tema scuro con un momento in corso; a 360dp al 200%; alle 01:30; i due pannelli a
  841dp.
- DESIGN §8.4 e §8.8 riscritte dove parlano della riga, della card e del pulsante; §10 e §13.1
  allineate; README (i due testi alternativi) e CHANGELOG.
- La suite intera (`./gradlew test :app:testDebugUnitTest :app:lintDebug`, 3 020 test, 15
  saltati) con il lint senza errori, in locale. **Un rosso nella corsa intera, non dello
  step**: `AlertsViewModelTest`, un caso che aspetta il DataStore entro dieci secondi, uno
  diverso in due corse a macchina carica; la classe da sola è verde due volte di fila. È la
  stessa famiglia dell'intermittenza del DataStore su Windows.

### Cosa conferma solo il telefono

Casella in «Aperto». Le icone a 34dp nella lista e in «In arrivo», la luce zodiacale
soprattutto, la più debole; «Aggiungi» accanto al titolo sotto il pollice; la riga con il
carattere grande del telefono.
