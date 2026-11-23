# PLANNING, archivio 17: 19-23 novembre 2026: la review della Fase 17, la cache, il grafico di «Maltempo», Milano tolta dal codice, e la Fase 18

> Congelato il 23 nov 2026: un periodo chiuso del registro, spostato qui da `PLANNING.md`
> senza cambiarne una riga. Non si modifica. Il piano vivo è [`PLANNING.md`](../../PLANNING.md);
> l'indice di tutti i titoli è [`README.md`](README.md). Qui stanno la review della Fase 17 (l'HSS
> e la serie contata una volta), la firma della cache che conta l'aria, la review finale della PR
> #185 e il grafico di «Maltempo», Milano tolta dal codice; e la **Fase 18**, più modelli nei
> numeri e nel cielo, intera: la valutazione, le decisioni, i criteri scritti prima dell'estate,
> gli step (18.1 la misura, 18.2 la temperatura che non entra, 18.3 il cielo dei tre modelli,
> 18.4 la registrazione del README, 18.6 la chiusura), e quel che il committente ha chiesto nel
> mezzo: la verifica di testo e grafica, la riga di oggi come resto della giornata, la tinta del
> cielo dalla luce, il pareggio al più leggero, «Cosa è cambiato» tolta.

---

## La review della Fase 17: l'HSS e la serie contata una volta (19 nov 2026)

Chiesto dal committente, sulla PR della Fase 17 (#185), dopo una review esterna del motore com'è
in quella PR e in main. Tre segnalazioni; la terza (tre commenti che dicevano che la CAPE serve
solo al temporale del giorno) è un commit di soli commenti, «17.8: la CAPE nei commenti legge
anche i rovesci». Le altre due:

**La misura dei rovesci con l'HSS.** Le decisioni della 17.6 stavano sul CSI, che non conta i
«no» giusti: dove l'evento è frequente, dirlo spesso alza il CSI, e dirlo sempre ne fa la sua
frequenza. D'estate un terzo o metà delle ore di pioggia sono rovesci: «Rovesci» su ogni pioggia
farebbe 0,34 in Europa e 0,47 agli aeroporti di ICON-D2, più della regola scelta (0,44).
Ricontato con l'HSS (Heidke: 0 per «sempre» e per «mai»), sugli stessi archivi e con lo stesso
strumento:

- **la scelta della 17.6 regge**: in Europa la regola è la migliore delle tre colonne, o alla
  pari, in ogni stagione (estate 0,06 → 0,34, ottobre 0,02 → 0,18, inverno 0,15 → 0,17), e la
  soglia di 10 J/kg sull'episodio vale anche per l'HSS (l'inverno europeo 0,14 senza, 0,17 con);
- **quel che il CSI nascondeva**: la pianura padana d'estate peggiora, 0,28 → 0,22, con «Rovesci»
  detto 74 volte per 45 visti; fuori dall'Europa d'inverno 0,17 → 0,07. Nessuna soglia vince
  ovunque (in pianura la forma sola fa 0,33, agli aeroporti di ICON-D2 CAPE ≥ 50 o la forma
  0,34): con un'estate sola una regola per regione sarebbe tarata sul rumore. **La regola non
  cambia**; il limite è scritto in STATES §10 e c'è una casella in «Aperto» per l'estate 2027;
- **il temporale possibile del giorno regge anche meglio**: un giorno di temporale è raro (13%
  dei giorni), il CSI lì non è gonfiato, e l'HSS cresce in ogni fetta (0,35 → 0,50 sulle tre
  stagioni, sul giorno del luogo).

`tools/analyze_against_metar.py` stampa da oggi, in `--showers` e nel temporale possibile di
`--hazards`, l'HSS, il bias e il CSI di «sempre» accanto a ogni CSI (classe `Days`). STATES §11
ha le due tavole con l'HSS, §2 e §10 il limite; i KDoc di `SHOWER_CAPE_JKG` e di `PossibleStorm`
dicono l'HSS accanto al CSI.

**L'hero e la cella contavano la serie in due modi.** Il mapper passa al motore le ore che
esistono (`statesOf`: un'ora vuota, senza codice e senza nuvole, sparisce, e le sue vicine si
giudicano fra loro, scelta del 19 ott 2026); il presente ricontava la persistenza della nebbia e,
dalla 17.6, l'episodio della pioggia sulle righe **con** i buchi, dove un'ora vuota chiude la
corsa. Accanto a un'ora vuota l'hero poteva dire «Rovesci» sopra una cella «Pioggia», o
«Coperto» sotto una cella «Nebbia». Per la nebbia il difetto c'era dal 26 ott 2026; la 17.6 l'ha
ripetuto per i rovesci.

- **Prima proposta, rivista e cambiata prima di scriverla**: contare l'episodio della serie sulle
  righe con i buchi, come l'hero. Era corretta (le due cose avrebbero detto lo stesso), ma
  sceglieva la regola opposta a quella che la serie ha dal 19 ott 2026, cambiava la striscia
  invece dell'hero, lasciava la nebbia com'era e teneva lo stesso conto in due posti.
- **Fatto**: `WeatherReportMapper.seriesOf` conta una volta sola, sulla serie che il motore
  riceve, la persistenza della nebbia e l'episodio di ogni ora, e li scrive nell'ora
  (`Hour.fogPersists`, `Hour.episodeHours`); il motore li legge per le righe, il presente li legge
  dalla riga in cui cade. La striscia e il giorno non cambiano di un'ora (sono i valori che il
  motore contava da sé); cambia solo l'hero, accanto a un'ora vuota.
- Due test in `WeatherReportMapperTest`, uno per la nebbia e uno per i rovesci, con un'ora vuota
  accanto a quella corrente: senza la correzione falliscono entrambi sull'hero (3 al posto di 45,
  80 al posto di 61) e passano sulla striscia.

Verifica: `./gradlew test :app:testDebugUnitTest` verde in locale, 3 549 test, 16 saltati (le immagini
del README);
nessuna immagine del README cambia (la registrazione di Milano non ha ore vuote né CAPE).

## La firma della cache su disco conta anche l'aria (19 nov 2026)

Chiesto dal committente, dopo una scheda dei pollini sparita a Cavenago con l'APK della PR #185 e
tornata al fetch successivo. **La causa non era nella PR**: le build debug condividono la cache, e
la voce su disco l'aveva scritta un APK di prima del 17 nov, con i pollini dentro `current` e
senza `pollenHours`; il codice nuovo la mostrava come primo disegno (`cachedReport`, che non
guarda la firma, apposta: è il ripiego offline, con l'età dichiarata) finché il fetch non l'ha
riscritta. I dati del provider erano a posto: 24 ore, le sei specie a 0,0, livello «nessuno».

**Il buco trovato guardandoci**: `ReportDiskCache.CURRENT_REQUEST`, la firma che decide se una
voce su disco vale come appena scaricata, era la richiesta del forecast più quella dei testimoni,
**senza l'aria**. Se cambiasse solo la richiesta dell'aria (com'è successo il 17 nov, quando i
pollini sono passati alle ore del giorno), una voce vecchia di meno di 15 minuti passerebbe per
fresca senza `pollenHours`, e la scheda mancherebbe fino alla scadenza. Il 17 nov non è successo
solo perché nello stesso commit era cambiata anche la richiesta del forecast.

- **Fatto**: `OpenMeteoAirQualityApi.REQUEST` (le variabili correnti, quelle orarie, i giorni) entra
  nella firma. Descrive la domanda, non la risposta: un fetch con l'aria fallita scrive ancora la
  firma corrente, com'era.
- Effetto: ogni voce già su disco smette di combaciare una volta, e ogni luogo rifà un fetch al
  primo avvio; lo fa comunque la PR, con `cape`.
- Test: `ReportDiskCacheTest`, una voce con la firma di oggi meno l'aria non è la richiesta
  corrente. `./gradlew :core:data:test` verde, 678 test.

## La review finale della PR #185 (committente, 20 nov 2026)

Chiesta dal committente prima di unire la PR: il codice, i commenti, le lunghezze dei file, le
linee guida. Una review del diff (`code-review`, livello xhigh) e i controlli meccanici sulle
regole di `CLAUDE.md`; ogni rilievo riletto nel codice prima di accettarlo.

**Pulito**: la CI sul commit di testa, i nomi delle app sorelle, TODO e log, colori a mano, emoji,
gergo a schermo (la CAPE non ci arriva), le chiavi IT/EN, le soglie del codice contro STATES e
`CLAUDE.md`, il peso delle richieste (7,9), l'indice degli archivi. Le lunghezze: il mapper passa
da 951 a 1 048 righe (45% di commenti), `AlertNotifier` a 1 101; undici file del repo stanno già
sopra le mille, e nessuna regola del progetto ne fissa una. Il mapper è il primo da dividere, in un
passo suo, se si vorrà.

**Corretto** (nella PR, su richiesta del committente):

- **L'hero nel primo quarto d'ora.** Le quantità e la probabilità del blocco sono dell'ora in cui
  la sua pioggia cade (`blockHour`, 17 nov 2026), la CAPE e l'episodio della riga che comincia
  all'etichetta: alle 13:00 il blocco 12:45-13:00 leggeva l'aria e l'episodio di 13-14, e un
  rovescio finito alle 13:00 era «Pioggia» per un quarto d'ora dopo «Rovesci». Ora la CAPE e
  l'episodio sono della riga dell'ora in cui la pioggia cade; la nebbia resta della riga
  dell'istante. Test nuovo nel mapper, che senza la correzione fallisce (61 dove va 80).
- **Il riassunto del mattino e la forma breve del temporale.** Su due parti del giorno o più la
  frase intera non sta in una riga e la breve è «Possibile temporale oggi»; il temporale contava
  come detto, e la riga con le parti e «Tieni d'occhio il cielo» spariva dalla notifica. Ora è
  detto solo dalla frase intera o dalla breve su una parte sola. Test a 384 dp con la grafica vera.
- **L'episodio al bordo della serie.** La serie parte dalla mezzanotte di oggi e finisce
  all'orizzonte: una pioggia dalle 20 all'01 era un episodio di un'ora a mezzanotte, e «Rovesci»
  a 10 J/kg. Un episodio che tocca un capo della serie ora non ha durata nota, e decide la CAPE da
  sola (STATES §3, regola 3). Negli archivi le serie sono di due mesi: i due capi non spostano la
  misura di 17.6.
- **STATES §10** diceva ancora che al temporale possibile mancava l'inverno: l'ha contato 17.6
  (§11), ora lo dice anche lì.
- **`PossibleStorm`**: una riga di commento da 138 caratteri e un link KDoc spezzato a capo.
- **`analyze_against_metar.py`**: senza `tzdata` (Windows) le tabelle sul giorno del luogo di
  `--hazards` restavano vuote in silenzio; ora la misura si ferma e lo dice. Con `tzdata` l'uscita
  è identica a prima, byte per byte.

**Non fatto, e perché**: «Chance of storms» → «Chance of thunderstorms», che avevo proposto io. La
forma breve esiste per stare in una riga: «Chance of thunderstorms in the afternoon.» misura 264 dp
a 14 sp, e una notifica su un telefono da 360 dp dà a una riga 247 dp; «Chance of storms…» 216,
l'italiano 223. Resta «storms», con il motivo scritto accanto alla stringa.

**Lasciati fuori dalla PR**, con una casella in «Aperto» ciascuno: il grafico delle allerte
«Maltempo» che non sono di pioggia (c'era già prima della PR, ed è una scelta di disegno), e il
temporale possibile di oggi accanto a un'ora che il provider lascia vuota (`PossibleStorm.ahead`
legge le righe, il mapper la risposta: con un buco la riga di oggi può nominare meno parti del
campo del giorno).

Test: `:core:domain:test` e `:core:data:testDebugUnitTest` verdi (a parte il solito
`AccessDeniedException` di DataStore su Windows, verde rilanciato), `:app:testDebugUnitTest`
verde.

## Il grafico di «Maltempo»: quello che cade (committente, 20 nov 2026)

Dalla review finale della PR #185, voce sopra: sotto un «Maltempo» il grafico della notifica
aperta era la probabilità di pioggia delle dodici ore, con il picco scritto sulla barra più alta
e la finestra dell'allerta in ambra. Il temporale suona a qualunque probabilità (17 nov 2026),
e sotto «Temporale verso le 16» barre all'8% dicevano «improbabile»: la contraddizione che 17.1
aveva tolto dal testo restava nel disegno, e TalkBack la leggeva («al massimo 8%»); sotto la neve
lo chiamava «pioggia». Il vento ha il suo grafico dal 14.7, le raffiche: la grandezza di cui parla
l'allerta. Il committente ha scelto la proposta, e per il temporale i millimetri all'ora invece di
nessun grafico.

- **Fatto**: temporale e gelicidio disegnano i millimetri di ogni ora, la neve forte i
  centimetri, su una scala del mondo da 0 al doppio della soglia forte del motore (7,6 mm/h e
  0,8 cm/h, `WeatherStateEngine.RAIN_MODERATE_MAX_MM` e `SNOW_HEAVY_CM`, ora pubbliche): la soglia è
  la riga tratteggiata a metà altezza, come la soglia del vento nel suo. Un solo inchiostro per la
  grandezza, quello tenue sotto la soglia; la finestra in ambra; il massimo scritto nella sua
  unità («4,0 mm»). Dodici ore in cui non cade niente: nessun grafico, non uno vuoto. La pioggia
  forte tiene la probabilità: lì il pericolo è la pioggia, e il testo la dice.
- TalkBack: «Grafico delle precipitazioni nelle prossime 12 ore: il massimo è 4,0 mm in un'ora,
  verso le 16», e «della neve» in centimetri.
- Il codice: `NotificationCharts.amountHours`, che con `gustHours` disegna attraverso
  `quantityHours`, un solo disegno delle barre di una grandezza; `AlertNotifier.chart` sceglie per
  il tipo di pericolo, ed è `internal` per il suo test.
- DESIGN §8.17 e il CHANGELOG lo dicono. Nessuna immagine del README cambia: quella notifica è la
  sintesi del mattino.
- Test: `NotificationChartsTest` (quello che cade, niente che cade, la neve che legge la neve) e
  `AlertNotifierTest` (la descrizione per temporale, gelicidio, neve e pioggia, e nessun grafico
  dove non cade niente). `NotificationFitRenderTest` (testDebug) dava al suo temporale delle 15
  il codice e la probabilità ma non l'acqua, e senza millimetri il grafico non c'è: ora ha i suoi
  6 mm, e nessun corpo finisce in «…» su tre telefoni, tre grandezze del carattere e due lingue.
  La suite dell'app e del dominio verde, 1 777 test.

## Milano non è più scritta nel codice (committente, 22 nov 2026)

Il committente ha chiesto perché nel codice ci fosse Milano fissa, senza `IT`. Era
`CityStore.DefaultCity`, letta in un solo punto: `migrateFirstRun`, il controllo una volta per
installazione ereditato dalla Fase 14b di tweather, che a un'installazione con una cronologia e
nessun luogo salvato scriveva Milano come luogo, per non togliere a un aggiornamento la città
che il vecchio ripiego gli mostrava. Chiaro quel ripiego non l'ha mai avuto (archivio 01,
«Fase 2», «Deviazioni e rinvii registrati»: «seminarne una finta è vietato»), e la 1.0.0 (21 set
2026) è uscita con il primo avvio e il passo delle notifiche: ogni installazione pubblicata ha
fatto il controllo al suo primo avvio, a dati vuoti, e ne ha scritto solo il segno. Il ramo che
scriveva Milano si raggiungeva soltanto da una build di sviluppo di prima della 1.0 senza luoghi,
e lì restituiva una città che il lettore aveva tolto. Il record poi era monco: senza `countryCode`
né `admin3`, quella Milano non aveva la pioggia al quarto d'ora (`NowcastCoverage`), leggeva la
qualità dell'aria fuori dalla scala europea (`PlaceRegion`) e trovava la zona d'allerta solo
dalle coordinate.

- **Tolti** `CityStore.DefaultCity`, `migrateFirstRun` con la chiave `first_run_migrated`,
  `WeatherRepository.hasAnyHistory` e `WeatherHistoryDao.any` (servivano solo a lui), e
  l'`init` di `ShellViewModel`, che ora non prende più il repository.
- **`firstRun` si legge dalle due risposte**, `init_done` e poi `notify_asked`. `FirstRun.Unknown`
  resta come valore iniziale della shell finché lo store non è stato letto: la superficie vuota
  di un fotogramma, mai il primo avvio a chi l'ha già passato. Lo store non lo emette più.
- **Chi aggiorna non vede niente**: ha già le due risposte. La chiave `first_run_migrated` resta
  sul disco, innocua; cancellarla costerebbe codice per nessun guadagno. Cambia l'archivio 01,
  «Fase 3 — Luoghi e primo avvio» (la casella di `migrateFirstRun`/`firstRun` cablati nella
  shell), e l'archivio 03, «E il primo avvio: sì, ma dopo il luogo, e a parole» (`migrateFirstRun` che
  scrive `notify_asked` per gli aggiornamenti).
- Nessun'altra città è fissa nel codice dell'app: `widget_preview_place` è il testo
  dell'anteprima del widget nel selettore del launcher, `SampleWeatherReport` (New York) è dei
  soli test, e le Milano di `tools/` e della registrazione del README non arrivano nell'APK.
- Test: `CityStoreTest` (il primo avvio senza luoghi, un'installazione con il vecchio segno
  resta risposta, il salto, l'ordine luogo-notifiche) e `WeatherSyncWorkerTest` con una Milano
  propria, completa di `IT` e del comune. Nessuna immagine del README cambia, e nemmeno il
  CHANGELOG: a schermo non cambia niente.

## Fase 18 — Più modelli nei numeri e nel cielo: i passi (committente, 22 nov 2026)

**Da dove nasce.** Il committente ha chiesto una valutazione critica del motore delle previsioni
contro le app professionali, con ricerche su come trattano i dati prima di mostrarli, e un
riesame delle decisioni del repo che oggi impediscono un motore migliore. Dalle otto proposte di
quella valutazione: «espandi gli 8 punti proposti in una fase dettagliata … rianalizza ogni punto
prima di scriverlo … tutto quello che è valore aggiunto e non rumore … step sviluppabili
singolarmente in sessioni singole … ogni passo compilabile, con un APK dalla CI da provare sul
dispositivo»; il radar «valuta bene la fattibilità», e tutto il resto indipendente da lui. Alle
quattro domande della sessione ha risposto: la regola dei numeri **si cambia** («se lo ritieni
veramente corretto cambiala anche per altro oltre la temperatura … non vorrei che se non lo
facciamo ora poi in futuro abbiamo ancora la stessa limitazione»); il **radar fuori dalla
fase**; gli step di sola misura **separati** da quelli del codice; la media dell'ensemble ECMWF
**facoltativa, in coda**. Poi, aperta la PR: «vorrei comunque fare questa fase su questo branch e
vedere alla fine come va prima di mergiarla in main».

**Come lavorano gli altri**, per quel che conta nelle scelte (le fonti in fondo alla sezione):

- **NOAA NBM**: toglie l'errore sistematico di ogni modello (una media mobile esponenziale, il
  quantile mapping) e fa la media pesata di modelli NWS e non, con i pesi dall'errore degli
  ultimi 60–120 giorni.
- **Met Office IMPROVER**: fonde gli ensemble MOGREPS con il modello UKV, calibra le probabilità,
  ricava il simbolo dell'ora da un albero di decisione sulle probabilità, e fonde all'inizio un
  nowcast estrapolato dal radar.
- **meteoblue mLM**: 10–20 modelli pesati sulle stazioni per temperatura, rugiada e vento, con un
  errore sotto quello del miglior modello grezzo in ogni continente (la verifica di luglio 2026).
- **Yr (MET Norway)**: la temperatura corretta con le stazioni Netatmo, fino a 3 °C nelle prime 12
  ore.
- **The Weather Company**: circa 100 modelli fusi (WxMix). **Google MetNet-3**: radar e stazioni,
  nessun modello numerico in ingresso, fino a 24 ore.

Il motore degli stati di Chiaro ha l'impianto dei simboli di IMPROVER (lo stato ricavato dai campi
fisici con regole in ordine, non preso dal codice del modello), e quello non si rifà. Quel che
nessuno di loro fa è mostrare un modello solo. Chiaro ne scarica già cinque, `best_match` e i
quattro testimoni della nebbia, fra cui i due globali IFS e UKMO, e i testimoni servono solo alla
nebbia.

**I numeri di questa sezione** vengono da script di lavoro, non dal repo, sui due archivi di
`tools/measurements/` con le corse per giorno di anticipo: l'inverno
(`metar-2025-12-01-2026-01-31.json.gz`) e ottobre (`metar-2026-09-26-2026-10-25.json.gz`), 26
aeroporti. Lo step 18.1 li porta in `tools/analyze_against_metar.py` e deve ritrovarli. Non
decidono: i criteri di 18.2 e 18.3 decidono sull'**estate 2026**, che nessuno ha ancora scaricato
con le corse dei testimoni.

### Cosa ho trovato

**1. La temperatura: dal secondo giorno la media di tre modelli sbaglia meno di ciascuno.**
Errore medio assoluto dell'ora contro il METAR entro 30 minuti, in °C. Il giorno 1 è l'archivio
(le prime ore di ogni corsa), il giorno N è `previous_day(N−1)`. La **rampa** è `best_match`
spostato verso la media di `best_match`, IFS e UKMO (una copia di `best_match` fuori) con il peso
del criterio qui sotto, 0 fino alle 24 ore e pieno dalle 72:

| Giorno | Inverno: best_match | IFS | UKMO | rampa | Ottobre: best_match | IFS | UKMO | rampa |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | **0,90** | 1,07 | 1,36 | 0,90 | **0,86** | 1,05 | 1,18 | 0,86 |
| 2 | 1,20 | 1,33 | 1,47 | 1,17 | 1,07 | 1,18 | 1,30 | 1,02 |
| 3 | 1,32 | 1,44 | 1,58 | 1,29 | 1,18 | 1,26 | 1,40 | 1,10 |
| 4 | 1,47 | 1,58 | 1,72 | 1,45 | 1,30 | 1,36 | 1,49 | 1,21 |
| 5 | 1,67 | 1,77 | 1,92 | 1,63 | 1,49 | 1,46 | 1,65 | 1,34 |
| 6 | 2,14 | 2,00 | 2,15 | 1,85 | 1,72 | 1,66 | 1,80 | 1,49 |
| 7 | 2,45 | 2,37 | 2,43 | 2,14 | 1,89 | 1,92 | 1,98 | 1,66 |

- **Il primo giorno la media peggiora**: piena, d'inverno fa 0,90 → 0,99. ICON-D2 da solo è il
  migliore, ed è quel che la misura dell'umidità del 27 ott 2026 aveva visto. Per questo la rampa
  parte da zero; dal secondo giorno non peggiora in nessun giorno né in nessun gruppo, e in
  pianura padana fa 1,95 → 1,64 al giorno 6 d'inverno e 1,44 → 1,21 al giorno 5 d'ottobre.
- Il salto di `best_match` d'inverno fra il giorno 5 e il 6 (1,67 → 2,14) è il cambio di modello
  dentro `best_match`: la media lo smussa (1,63 → 1,85).
- **Massima e minima del giorno** (giorno UTC con almeno 18 ore di METAR, gli estremi delle ore,
  come 14.10; con la media piena): giorno 7 d'inverno, massima 2,02 → 1,87 e minima 2,79 → 2,29;
  giorno 7 d'ottobre, 1,65 → 1,43 e 2,03 → 1,77. Al giorno 1 la massima peggiora (0,75 → 0,82):
  di nuovo la rampa.
- **Le corse cambiano meno.** Le ore in cui la previsione per la stessa ora cambia di 2 °C o più
  fra una corsa e quella del giorno dopo (con la media piena): giorno 5 contro 6, 30,1% → 12,4%
  d'inverno e 23,4% → 8,3% a ottobre; giorno 2 contro 3, 9,5% → 2,7% e 9,1% → 1,2%. È il rumore
  che «Cosa è cambiato» (soglia 1 °C) oggi racconta come una revisione.
- **Costa zero chiamate**: `OpenMeteoWitnessApi` scarica già `temperature_2m` di IFS e UKMO per
  sette giorni.

**2. Il cielo: la nuvola che l'app mostra è quella della famiglia ICON, la meno giusta dei modelli
misurati.** Contro il
satellite (indice di chiarezza, sole sopra i 15°, ore senza precipitazione né nebbia viste, come
§11 di STATES), ottobre, giorno 1, 5 112 ore di luce, il cielo dalla luce che prendono gli strati
(la regola 6, `opacity` dello strumento) per ogni modello:

| Il cielo da | Giuste | HSS | Detti: sole / un po' / niente |
|---|---:|---:|---|
| `best_match` (l'app oggi) | 66% | 0,37 | 56 / 21 / 23 |
| ICON-EU | 62% | 0,33 | 50 / 27 / 23 |
| ICON-D2 (2 738 ore) | 61% | 0,33 | 49 / 22 / 29 |
| IFS | 76% | 0,44 | 75 / 10 / 15 |
| UKMO | 76% | 0,41 | 77 / 10 / 13 |
| KNMI, DMI (3 911 ore) | 74% | 0,42 | 72 / 3 / 25 e 73 / 4 / 23 |
| **Gli strati medi di `best_match`, IFS e UKMO** | **78%** | **0,50** | 70 / 19 / 12 |
| *Visti dal satellite* | | | *72 / 17 / 11* |

- In Europa `best_match` è la famiglia ICON, la più lontana dal satellite fra i modelli misurati
  (61–62% da sola, 66% come la serve `best_match`). In pianura padana gli strati medi fanno
  67% → 81% (HSS 0,37 → 0,54), altrove 66% → 76%.
- La media non è solo più giusta: dice i tre cieli quasi nella proporzione in cui si vedono, dove
  `best_match` dice «niente sole» il doppio delle volte che succede. Batte anche IFS da solo (76%,
  0,44): non è «prendi la nuvola di un altro modello».
- Ai giorni 2–7 l'archivio delle corse non ha gli strati (14.4); sulla sola copertura totale la
  media vince a ogni giorno: ore giuste 53,0% → 59,8% al giorno 3, 47,3% → 55,4% al 5, 45,7% →
  52,5% al 7; HSS 0,16 → 0,24 al giorno 5.
- **Costa 1,2 chiamate a fetch**: gli strati dei testimoni oggi non si scaricano (7,9 → 9,1).

**3. Gli otto punti della valutazione, riletti uno per uno.**

1. **«Le API a pagamento (Ensemble, Historical, Satellite, Previous Runs)»** di `CLAUDE.md` e
   dell'archivio 07 («Fase 14 — Il motore contro i motori premium: la revisione e i passi», il
   paragrafo «Il vincolo») sono un errore di fatto: la tabella di open-meteo.com/en/pricing,
   ripetuta in /en/terms, mette la spunta nella colonna *Free* per Ensemble, Historical Forecast,
   Previous Runs, Single Runs e Satellite Radiation; mancano solo al piano a pagamento più basso,
   *Standard*. Il vincolo vero resta: uso non commerciale, 10 000
   chiamate al giorno per indirizzo, il peso di ogni richiesta. → **18.1**.
2. **La media della temperatura dal terzo giorno**: confermata, e anticipata con la rampa al
   secondo (punto 1 qui sopra). → **18.2**.
3. **L'ensemble per i giorni 3–7, intervalli e probabilità del giorno bagnato**: fuori. La
   percentuale del giorno che l'app stampa (il massimo delle orarie) segue già un giorno con
   almeno tre ore di pioggia: al giorno 1, quando stampa 20–39% piove tre ore o più nel 38% dei
   giorni d'inverno e nel 22% d'ottobre; con 60–79% nel 59 e nel 62%; da 80% nel 94 e nel 90%.
   Gli intervalli non hanno un posto nella riga della settimana, e si verificano solo con i
   membri, che Open-Meteo tiene tre giorni: servirebbero mesi di raccolta. E una richiesta con i membri pesa,
   a giudicare dall'esempio della documentazione (ICON-EPS, 40 membri, una variabile: 4,0
   chiamate), come se ogni membro fosse una variabile. Dell'ensemble resta la **media**, come
   quarto modello. → **18.5**, facoltativo.
4. **Le parti del giorno dal quarto giorno**: ci sono già. Dal 31 ott 2026 la frase del giorno dice
   le parti fino al terzo giorno e nessun «quando» negli ultimi tre (VISION §5.2, punto 5).
   Ricontato oggi su `best_match` senza probabilità: l'ora bagnata al giorno 4 ha CSI 0,33–0,34,
   la parte del giorno 0,47–0,53, il giorno 0,65–0,70. La decisione regge; niente da fare.
5. **Il radar**: fuori dalla fase (committente). La valutazione è qui sotto, «Il radar: valutato,
   da decidere», e la voce in «Da decidere: se e quando».
6. **Le correzioni statiche, un MOS calcolato dagli strumenti**: fuori. Una correzione per gruppo,
   giorno di anticipo e ora UTC, imparata sugli altri aeroporti e provata su quello lasciato fuori,
   nello stesso periodo (quindi un limite superiore), guadagna 0,1 °C solo in pianura padana
   d'inverno ai giorni 3–6 (1,33 → 1,24 al giorno 3) e niente altrove né a ottobre. Sopra la media
   guadagna 0,1–0,15 °C in pianura d'inverno, perde fino a 0,03 a ottobre, e fuori dalla pianura
   sta fra −0,04 e +0,02. E una tabella ferma invecchia a ogni aggiornamento di un modello, che
   il provider non annuncia.
7. **Altri modelli**: la **media dell'ensemble ECMWF** va in 18.5. **AIFS** fuori: per la
   documentazione di Open-Meteo ha solo passi di 6 ore, e una temperatura a 6 ore perde la massima
   e la minima. **WeatherNext 2** fuori: i termini d'uso non sono verificati e non c'è un archivio
   per anticipo. **ICON-CH1/CH2** fuori: sarebbero un cambio di modello nei giorni 1–2 al nord,
   dove `best_match` è già il migliore (0,72–0,84 °C in pianura padana), e 14.4 ha già detto no al
   caso uguale di ICON-2I.
8. **«Cosa è cambiato» sui valori della media**: non è uno step. `WeatherSnapshots.flattenForecast`
   legge il report che lo schermo mostra, e la media ci arriva da sé (punto 1, «Le corse cambiano
   meno»).

E **le altre grandezze**, che il committente ha chiesto:

- **il cielo** sì, punto 2 → **18.3**;
- **vento e punto di rugiada** no: al giorno 1 la media guadagna il 3% sul vento (3,85 → 3,73 km/h
  d'inverno, 3,79 → 3,68 a ottobre) e il 7–11% sulla rugiada (0,94 → 0,87 °C, 1,05 → 0,93), ma
  l'app li mostra solo per adesso, nella griglia dei dettagli; oltre oggi il vento è solo delle
  allerte, che leggono le raffiche, e una media abbassa i picchi. Sarebbe un numero in più che
  nessuno vede;
- **quantità e probabilità di precipitazione** mai: la media di due piogge è una pioviggine che
  nessun modello ha previsto, e la probabilità è già quella di un ensemble. Un voto dei modelli sul
  giorno bagnato contraddirebbe le ore dello stesso giorno, che restano di `best_match`.

### Decisioni prese oggi

1. **La riga sulle API a pagamento si corregge** (18.1). Le API di misura sono nel piano gratuito;
   se una entra nell'app, entra con il suo peso, come ogni richiesta.
2. **La regola dei numeri cambia ora, comunque vadano le misure** (il committente). Il testo che
   18.1 scrive in `CLAUDE.md` al posto di «the app never averages two models into a number»:

   > A number on screen is what a model forecast, or a combination of models that the app has
   > measured to be closer to what happens than the single model it replaces; never a value
   > changed to make a screen agree, never a guess. A combination is a rule like a state's:
   > written down with its reason, its measurement against observations and the slice where it
   > does not hold, tested, and named where the page says where its data come from.
   > Precipitation amounts and probabilities are never averaged: the mean of two rains is a
   > drizzle no model forecast.

   La media della temperatura e quella del cielo entrano ciascuna con il suo criterio, scritto qui
   sotto prima dei numeri dell'estate. Una che non passa resta fuori, e la regola non ferma la
   prossima. Cambia l'archivio 07, «Fase 14 — Il motore contro i motori premium: la revisione e i
   passi», il paragrafo «Il vincolo», e la regola del 27 ott 2026 in `CLAUDE.md`.
3. **I modelli della media sono tre, a pari peso**: `best_match`, IFS (`ecmwf_ifs`) e UKMO
   (`ukmo_global_deterministic_10km`), già nella richiesta dei testimoni. Un testimone che è
   `best_match` stesso (la prova delle copie di `Witnesses`) o che manca resta fuori, e la media è
   di quel che resta. KNMI e DMI restano testimoni della nebbia: coprono due giorni e mezzo, e solo
   l'Europa. Un input che manca spegne solo la media (STATES §4): senza testimoni il numero è di
   `best_match`, come oggi.
4. **La temperatura**: il peso della media cresce con le ore contate dal momento del report: zero
   fino a 24, in linea retta fino a uno a 72, pieno dopo. Gli stati leggono la temperatura del
   modello dell'ora (la fase deve restare coerente con la sua neve); tutto quel che mostra o legge
   una temperatura come numero legge la media: lo schermo, i widget, le regole del lettore, le
   allerte, «Cosa è cambiato».
5. **Il cielo**: la copertura dell'ora, il totale e i tre strati, è la media dei tre modelli **dalla
   prima ora**, e diventa la nuvola dell'ora per tutti: lo stato (la regola 6 e «Velato»), il cielo
   disegnato, il nastro, il tile, il verdetto del Cielo. La parola, il disegno e il numero restano
   una cosa sola.
6. **Il radar resta fuori dalla fase**, valutato e scritto per la decisione.
7. **Gli step di sola misura sono a sé**: 18.1 e 18.4 lasciano l'APK com'era, e lo dicono.
   **Tutta la fase sta su un ramo solo**, e si unisce a `main` solo dopo 18.6 (il committente).
   Ha un precedente, la Fase 17 sul ramo `fase-17` e la sua PR #185.
8. **La media dell'ensemble ECMWF è facoltativa, in coda** (18.5), e si chiude senza codice se non
   si può misurare.

### I criteri, scritti prima dell'estate

Scritti oggi, in questo commit, prima che l'estate 2026 (1 giu–31 ago, i 26 aeroporti) sia
scaricata con le corse dei testimoni. Gli step li applicano così come sono. I due archivi già
guardati si stampano accanto e non decidono; dopo i numeri non si cercano sottoinsiemi (un
aeroporto, un mese, un'ora) in cui una regola passi.

**La temperatura (18.2).** Per aeroporto, giorno di anticipo N da 1 a 7 e ora:
T = T_bm + w·(T_media − T_bm), con T_media come nella decisione 3,
w = min(1, max(0, (h − 24) / 48)) e h = 12 + 24·(N − 1), il centro delle ore del giorno N. La
verità è il METAR entro 30 minuti dall'ora; massima e minima sono gli estremi delle ore della serie
con la media contro quelli dei METAR, sul giorno UTC con almeno 18 ore di METAR. L'archivio non
ha gli estremi propri del modello per giorno di anticipo: la misura legge gli estremi delle ore, e
l'app sposta i propri della stessa differenza (18.2). La media entra se, sull'estate:

- (a) l'errore medio orario dei giorni 5–7 insieme scende di almeno il 5%;
- (b) in nessun giorno da 1 a 7, né in pianura padana né nel resto, sale di più di 0,02 °C;
- (c) massima e minima dei giorni 4–7 insieme non peggiorano in nessuno dei due gruppi.

Si stampano, senza decidere: i due archivi; i cambi da 1 e da 2 °C fra una corsa e la successiva;
la rampa B (zero fino a 48 ore, piena da lì) e la C (zero fino a 48, piena a 120); la media con
KNMI e DMI.

**Il cielo (18.3).** Per aeroporto e ora di luce del giorno 1 (l'archivio, che ha gli strati): la
copertura totale e i tre strati sono la media di quelli dei tre modelli (un modello conta solo con
tutti e quattro i numeri), la luce presa è `opacity` sulla media, il cielo i tre secchi di §11
(sole sotto 50, un po' da 50, niente da 80). La verità è il satellite com'è in §11. La media entra
se, sull'estate:

- (a) le ore giuste salgono di almeno 5 punti e l'HSS di almeno 0,05, su tutti gli aeroporti;
- (b) né in pianura padana né nel resto scendono;
- (c) «niente sole» detto in un'ora che il satellite dice di sole (k ≥ 0,7) non è più frequente
  di oggi: è l'errore che la regola 6 aveva ridotto (§11), e non deve tornare.

Si stampano: ottobre; i giorni 2–7 sulla copertura totale; la copertura media contro le okta dei
METAR (FEW 1–2, SCT 3–4, BKN 5–7, OVC 8) di giorno e di notte, cioè il numero del tile e del
verdetto del Cielo; la quota di «Velato»; la media a cinque con KNMI e DMI.

**La media dell'ensemble (18.5).** Entra come quarto modello della temperatura se, sull'estate e
su settembre–ottobre 2026, l'errore orario dei giorni 5–7 scende di almeno il 3% rispetto alla
media a tre, e nessun giorno né gruppo peggiora di più di 0,02 °C; nel cielo con il criterio di
18.3 contro la media a tre, se la media dell'ensemble porta gli strati.

### Le regole comuni

- **Uno step per sessione, tutti sul ramo `claude/chiaro-forecast-engine-review-xg6n57`**, con
  la PR #189 in bozza dall'inizio: nessuna PR per step. La sessione parte dal ramo aggiornato
  (`git fetch origin claude/chiaro-forecast-engine-review-xg6n57`, anche se l'ambiente le assegna
  un ramo suo), ci committa lo step (il numero dello step in testa a ogni messaggio) e ci fa push.
  Se `main` è andato avanti, lo unisce nel ramo (`git merge origin/main`), mai un rebase. Alla
  fine la sezione dello step in coda a questo registro, la casella chiusa in «Aperto» e la riga
  dello step spuntata nella descrizione della PR. Il committente unisce la PR dopo 18.6.
- **Solo rispetto a `main`** (il committente, 22 nov 2026): il punto di partenza, il codice da
  cambiare e il termine di ogni confronto sono quelli di `main`. Nessun altro ramo né PR è una
  fonte di questa fase, né per il codice né per le misure.
- **Ogni step lascia la build verde e un APK installabile dalla CI**: `./gradlew test
  :app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` (nel container con lo script del
  mirror, `CLAUDE.md`). La CI costruisce l'APK a ogni push del ramo: ogni step si prova
  dall'artifact del suo commit (la build debug si installa accanto alla release, ed è il confronto
  «prima e dopo»). La sezione dello step dice che cosa provare sul telefono, o che l'APK è quello
  di prima e non c'è niente da provare.
- **Prima la misura, nel repo**: la riga in `tools/analyze_against_metar.py`, il criterio di questa
  sezione così com'è, poi il codice. Uno step che non passa il suo criterio si chiude senza codice,
  e dice dove non passa.
- **I dati si scaricano sul runner.** Il container cloud riceve 429 da Open-Meteo (22 nov 2026:
  «Daily API request limit exceeded», l'indirizzo è condiviso). Il download e il completamento
  passano dal workflow `Measure against METAR`, che committa sul ramo; lo lancia la sessione con
  lo strumento GitHub, o il committente dal tab Actions. Mai uno script di download nella
  sessione.
- **Il codice**: la media in `:core:domain`, pura, una classe sola per le due grandezze
  (`ModelMean`, il nome è indicativo); il mapper la chiama dove la grandezza entra nel report.
  Nessun parametro tarato dopo i numeri: i pesi e la rampa sono quelli scritti qui. 18.2 e 18.3 non
  dipendono l'uno dall'altro: chi arriva prima scrive la classe.
- Ogni variabile aggiunta scrive il nuovo peso accanto alla lista, in `CLAUDE.md` e nella sonda
  (`tools/probe_provider.py`); il cambio della richiesta cambia da sé la firma della cache
  (`CURRENT_REQUEST`).
- Una regola che cambia, cambia in `docs/STATES.md`, nel codice e nel suo test nello stesso step.
  Un numero che cambia origine lo dicono, nello stesso step, la guida (IT/EN), la tabella delle
  fonti del README e il piede della pagina, senza gergo: «la media di tre modelli», non
  `best_match`.
- **Le immagini del README**: la registrazione di Milano non ha i testimoni, e senza testimoni la
  media non c'è; 18.2 e 18.3 non dovrebbero quindi cambiare nessuna immagine (se una cambia, si
  rigenera e si guarda nello step, come sempre). La registrazione si rifà una volta, dopo i due,
  in 18.4.
- Beta: nessuna compatibilità da tenere. Al primo fetch dopo l'aggiornamento «Cosa è cambiato» può
  segnare una volta un giorno cambiato dal metodo e non dal tempo; lo step che cambia un numero lo
  dice nella sua sezione.

### Gli step

**18.1 — La misura nel repo, l'estate scaricata, le due righe di `CLAUDE.md`.** Solo strumenti e
documenti: l'APK è quello di prima.

- `CLAUDE.md`, «Free and without a key is a constraint»: le API di misura sono nel piano gratuito,
  il limite resta l'uso non commerciale e il peso. Il punto «Numbers are one model's per hour»
  riscritto con il testo della decisione 2, che vale da subito anche se ancora nessuna media è nel
  codice. `docs/STATES.md`, la riga in testa («Nothing here changes an amount…»), allineata al
  principio nuovo (le medie stesse le scrivono 18.2 e 18.3). La frase del README «the numbers on
  screen are always the model's» resta fino a 18.2, perché fino a lì è vera.
- `tools/analyze_against_metar.py`: `--blend` (il punto 1: ogni modello, la media, le tre rampe,
  massima e minima, i cambi fra le corse, per gruppo) e `--sky-blend` (il punto 2: ogni modello,
  gli strati medi, i giorni 2–7 sulla copertura totale, le okta dei METAR, «Velato»). Si rilanciano
  sui due archivi e devono ridare i numeri di questa sezione, o lo step scrive quelli nuovi e
  perché sono diversi (le copie, per esempio, si riconoscono con la prova dell'app).
- `tools/measure_against_metar.py`: verificare che il download di un periodo nuovo porti tutto
  quel che i due criteri leggono (il satellite, gli strati dei tre modelli al giorno 1, le corse dei
  testimoni ai giorni 2–7); aggiungere quel che manca.
- Lanciare `Measure against METAR` sul ramo con `start=2026-06-01` e `end=2026-08-31`, poi con
  `start` vuoto per completare il file con le corse. Il workflow committa sul ramo (anche da solo,
  a ogni push che tocca `measure_against_metar.py`): prima di un push dopo il suo, `git pull`. Lo
  step non aspetta l'esito: finisce quando il workflow è partito, e 18.2 comincia controllando che
  il file ci sia.

**18.2 — La temperatura dal secondo giorno: la media dei tre modelli.**

- Prima la misura: `--blend` sull'estate, con il criterio così com'è. Se non passa, lo step finisce
  lì.
- `:core:domain`: la media (decisione 3) e la rampa (decisione 4), con i loro test: le copie, un
  testimone che manca, l'ora 0, 24, 48, 72 e 168.
- Il mapper, dopo gli stati: la temperatura di ogni ora diventa la media pesata. La massima e la
  minima del giorno restano gli estremi propri del modello (archivio 16, «Fase 17», «Cosa ho
  trovato», punto 6), spostati della differenza fra l'estremo delle ore con la media e quello delle
  ore senza. La percepita si sposta della stessa differenza della sua ora, perché resti quella che
  il modello dà rispetto alla temperatura. Gli stati del motore leggono ancora la temperatura del modello. Il
  momento del report, non quello in cui si guarda, fissa le ore: lo stesso report dice sempre gli
  stessi numeri.
- Chi la legge: il presente e le prime 24 ore non cambiano (il peso è zero), quindi l'hero, i
  dettagli, i quarti d'ora e le notifiche di oggi restano come sono. Cambiano le ore e le righe
  della settimana dal secondo giorno, i widget che le mostrano, le regole del lettore e le allerte
  che leggono quei giorni, «Cosa è cambiato».
- `docs/STATES.md`: una sezione nuova, «The temperature from the second day», con la regola, la
  rampa, la misura e il limite (il primo giorno). La guida (IT/EN): da dove vengono le temperature
  dei giorni lontani, in una frase. Il README: la tabella delle fonti e la frase «the numbers on
  screen are always the model's». Il CHANGELOG.
- Cosa provare sul telefono: le righe della settimana accanto alla versione di prima (domani poco
  o niente, dal terzo giorno massime e minime diverse di qualche decimo, a volte di un grado o due
  ai giorni 6–7); un giorno lontano
  aperto, con le ore coerenti con la sua riga; l'hero e le prime 24 ore identici; da un fetch
  all'altro, nei giorni 5–7, numeri che si muovono meno.

**18.3 — Il cielo dei tre modelli.**

- Prima la misura: `--sky-blend` sull'estate, con il criterio così com'è. Se non passa, lo step
  finisce lì.
- La richiesta dei testimoni: `cloud_cover_low`, `cloud_cover_mid`, `cloud_cover_high` (+1,2
  chiamate, il fetch 7,9 → 9,1), con il peso accanto alla lista, in `CLAUDE.md` e nella sonda.
- Il mapper, prima degli stati: la copertura di ogni ora e del blocco corrente (con i testimoni
  della riga in cui cade, come la nebbia) è la media dei tre (decisione 5). Tutto quel che legge la
  nuvola la legge da lì: `WeatherStateEngine` (la regola 6, «Velato»), `CloudLayers.lightTaken`
  per il cielo disegnato e il nastro, `WeatherText.cloudMeaning` e il tile, il verdetto del Cielo.
  La nebbia (regola 5) non cambia: vota con i suoi numeri.
- `docs/STATES.md`: la regola 6 con la media; §11 con i numeri nuovi (le ore giuste, «niente sole»
  sotto il sole, la quota di «Velato»); §4 (senza testimoni il cielo è di `best_match`); §10 (la
  notte misurata solo con le okta, l'inverno senza satellite). La guida, il README, il CHANGELOG.
- Cosa provare sul telefono: la parola dell'hero contro il cielo fuori dalla finestra, per qualche
  giorno; il cielo disegnato, il nastro della settimana e il tile delle nuvole che dicono la stessa
  cosa; una notte nel verdetto del Cielo; una mattina di nebbia che resta nebbia.

**18.4 — La registrazione del README con i tre modelli.** Solo test, risorse e immagini: l'APK è
quello di prima. Si fa se almeno uno fra 18.2 e 18.3 è entrato; altrimenti si chiude senza lavoro.

- `tools/record_readme_data.py` registra anche la risposta dei testimoni, con le variabili della
  richiesta di oggi; `ReadmeScreenshots` la legge.
- La registrazione si rifà (`python3 tools/record_readme_data.py`) in un commit suo, perché cambia
  ogni immagine. Se dal container Open-Meteo risponde 429, la lancia il committente dal PC e lo
  step riparte da lì.
- Le immagini si rigenerano e si guardano tutte; le didascalie e gli alt del README si rileggono.

**18.5 — La media dell'ensemble ECMWF come quarto modello** (facoltativo, e solo se 18.2 è
entrato).

- Prima il cancello, dal runner (nella sonda o in una riga sua): l'Ensemble Mean API dà la media di
  `ecmwf_ifs025` per giorno di anticipo nel passato (Previous Runs o Single Runs), o solo le prime
  ore di ogni corsa? Quanto pesa una richiesta? Ha gli strati delle nuvole? Senza un archivio per
  anticipo lo step si chiude senza codice, e lo dice.
- Con l'archivio: la misura con il criterio scritto qui; se passa, una richiesta in più (le sole
  variabili che servono, il peso scritto), il quarto modello nella media, i test, STATES, la guida.
- Cosa provare sul telefono, se entra: come 18.2, ai giorni 5–7.

**18.6 — La chiusura: la fase riletta, la PR rivista, l'unione.**

- La fase riletta contro i suoi criteri: che cosa è entrato, che cosa no e perché, in una tabella;
  `docs/STATES.md` riletto nelle sezioni toccate; la guida, il README e il CHANGELOG
  (`[Unreleased]`) che dicono la stessa cosa.
- Il ramo aggiornato con `main` (merge) e la CI verde sull'ultimo commit; una review dell'intero
  diff della PR (`code-review`) e i controlli meccanici di `CLAUDE.md` (colori a mano, gergo a
  schermo, le chiavi IT/EN, i pesi delle richieste, i nomi delle app sorelle).
- «Aperto» riletto: le prove dal vivo della fase restano caselle (la parola del cielo d'estate, un
  inverno con la temperatura dei giorni lontani).
- Cosa provare sul telefono: l'APK dell'ultimo commit, la fase intera. Il committente decide se
  unire.

### Il radar: valutato, da decidere

Fuori dalla fase per scelta del committente. È scritto qui perché la decisione, quando verrà, parta
dai fatti, come VISION §12.5 chiede.

**Il valore.** Il radar è l'unica fonte libera che risponde a «piove adesso, qui?» con
un'osservazione invece che con un modello, e solo in Italia. Quanto conta l'ha già contato la Fase
17 (archivio 16, «Cosa ho trovato», punto 1): il 27,9% delle ore di pioggia vista cade sotto una
cella che non mostra nulla (19,3% in pianura padana), e un'ora che l'app dice bagnata è asciutta una
volta su quattro. Sono le ore in cui si apre un'app del meteo. Il secondo valore sono le prossime
due ore: il quarto d'ora è calcolato solo dove `best_match` è ICON-D2 o HRRR (`NowcastCoverage`), a
sud di ICON-D2 è l'ora divisa in quattro (STATES §5), e un'estrapolazione del radar sarebbe lì il
primo nowcast vero.

**Che cosa c'è** (misurato il 9 set 2026, archivio 02, la tabella delle misure della Fase 11; e il
22 nov 2026):

- Radar-DPC: le tile WMTS (3,5 KB a zoom 6, solo l'ultima immagine, il parametro `time` ignorato),
  una REST per il GeoTIFF ogni 5 minuti (VMI 619 KB a 1 km), l'ultima immagine vecchia di 11
  minuti, 40 radar attivi su 50, la licenza CC BY-SA 4.0.
- **Il 22 nov 2026 l'API risponde «Access Denied» (Akamai, HTTP 403) a questo container cloud.** Il
  blocco è probabilmente per gli indirizzi non italiani o di datacenter: da verificare da una rete
  mobile italiana, da un telefono all'estero che guarda un luogo italiano e da un runner di GitHub.
  Fino ad allora la misura si fa solo da una macchina in Italia.

**Che cosa costa**, in due pezzi che si decidono uno per volta:

- **«Piove adesso»**, dalla tile ad alto zoom sul punto (lo zoom più alto servito e la tavolozza
  sono da verificare in R1): una PNG di pochi KB per controllo, decodificata con `BitmapFactory`,
  il colore riportato alla classe d'intensità della legenda. Solo
  con la pagina aperta, solo per un luogo in Italia, una cache di 5 minuti: batteria e dati
  trascurabili. I rischi: la tavolozza che cambia senza avviso (serve una sentinella nella sonda,
  come per il bollettino) e la copertura. **Un radar che non vede il luogo deve tacere, mai dire
  «asciutto»**: è il difetto di Lubiana (VISION §12.8). Il codice: una classe per la tile, il suo
  ingresso nella regola dell'hero (la precipitazione viene prima del cielo, quindi niente tocca
  18.3), il testo della sezione delle due ore quando il radar e il modello non concordano (oggi un
  hero bagnato su quarti asciutti dice «La pioggia smette a momenti», e col radar sarebbe falso),
  l'attribuzione nel piede e nella guida.
- **Le prossime due ore dal radar**, un nowcast per estrapolazione: servono le immagini passate,
  quindi la REST e il GeoTIFF (circa 620 KB l'una, almeno due alla volta), un lettore TIFF scritto
  in Kotlin (Android non ne ha uno), la stima del moto e lo spostamento delle celle, e uno
  strumento di misura suo, contro la persistenza e contro i quarti del modello a 30, 60 e 90
  minuti. Da 1,2 a 1,8 MB a ogni aggiornamento con la pioggia vicina. È il pezzo grande, e non si
  dimensiona prima della misura.

**Indipendente dal resto della fase**: tocca il blocco corrente e la sezione delle due ore; la fase
tocca le temperature dal secondo giorno e la nuvola dell'ora. Non ne dipende e non la blocca.

**LibreWXR** (segnalato dal committente il 22 nov 2026, letto e provato lo stesso giorno:
librewxr.net, i termini aggiornati al 17 nov 2026, il repository `JoshuaKimsey/LibreWXR`). È
un'API radar aperta (AGPL-3.0), compatibile con quella di RainViewer, con un'istanza pubblica
gratuita e senza chiave (`api.librewxr.net`) tenuta da una persona sola. Per l'Italia compone il
mosaico della Protezione Civile (24 radar), per il resto d'Europa OPERA (184 radar, 27 paesi), e
dove nessun radar copre mette una stima dal satellite (NOAA RRQPE, dall'infrarosso). Dà le ultime
due ore a passi di 10 minuti e **un nowcast di 60 minuti** (dichiarato sperimentale: l'estrapolazione
del radar fusa con modelli), in tile PNG fino a zoom 12. Provato da questo container: risponde
(dietro Cloudflare, cache di 5 minuti) dove l'API della Protezione Civile dava «Access Denied»; una
tile vuota pesa 334 byte, una a zoom 3 sull'Europa 4–7 KB; lo schema 0 («Black and White») è in
toni di grigio che sembrano codificare l'intensità (da verificare in R1).

Che cosa cambia nella valutazione:

- **R1 si può fare da qui**, senza una macchina in Italia. E i due pezzi costano molto meno: il
  lettore GeoTIFF, la stima del moto e lo spostamento delle celle li fa il server; l'app legge
  poche tile sul punto, l'ultima immagine e le sei del nowcast. R3 passa da «il pezzo grande» alla
  lettura di sei tile in più, più la sua misura.
- **Non solo l'Italia**: OPERA copre il resto d'Europa.
- **I rischi, che sono il prezzo**: un servizio di una persona, nato quest'anno (dopo i cambi di
  RainViewer del 1 gen 2026), senza SLA, che i suoi termini permettono di limitare o spegnere
  «at any time, with or without notice»; le tile dell'Italia sotto CC BY-SA 4.0, con
  l'attribuzione «Radar-DPC» e «Weather data via LibreWXR»; il nowcast sperimentale. E il più
  serio: **la stima dal satellite riempie i buchi dei radar nella stessa tile**, e un pixel «piove»
  che viene dall'infrarosso è una stima, non un'osservazione; dalla sola tile l'app non sa
  distinguerli (da verificare: se l'API dice la fonte o la copertura per pixel). Poi una cosa nuova
  per il lettore: la posizione approssimata del luogo (la tile che si chiede) va a un terzo che oggi
  non la riceve, e la guida lo deve dire. La licenza AGPL del server non tocca l'app, che non ne
  include il codice.
- **La regola che li rende accettabili**: il radar è un'aggiunta, mai una dipendenza. Se LibreWXR
  non risponde, se la tile non dice la fonte, o se il luogo è fuori copertura, l'app fa quel che fa
  oggi (il modello), senza dire niente di diverso.
- I termini vietano il traffico automatico sostenuto: una raccolta per la misura di R1 (13
  aeroporti ogni 10 minuti per qualche settimana) va chiesta prima all'operatore (il contatto è nei
  termini).

**I passi, se si decide**: **R1**, la misura, con LibreWXR da qualunque macchina, con l'API della
Protezione Civile solo da una macchina in Italia (il PC del committente, o una sessione collegata
con Remote Control): la raggiungibilità, il ritardo, la copertura, quanto
indietro va l'archivio, e il valore, cioè «piove adesso» del radar contro l'ora dell'app e contro i
METAR dei 13 aeroporti italiani (POD, falsi allarmi, HSS; in pianura padana e nel resto), e dove
il radar stesso sbaglia (la pioviggine che non vede, le valli alpine in ombra). **R2**, «Piove
adesso», solo se R1 lo dice. **R3**, il nowcast, solo dopo R2 e con una misura sua. Il mio parere:
R1 vale una serata; R2 è probabilmente il miglioramento più visibile per un lettore italiano nei
giorni di pioggia, a un costo piccolo; R3 solo se la sua misura mostra a 30–60 minuti un vantaggio
largo sui quarti del modello. Con LibreWXR R1 e R2 conviene deciderli presto, e il costo di R3
scende; il rischio, un servizio di una persona sola, resta, e lo regge solo la regola qui sopra.

**Le fonti** della valutazione: open-meteo.com/en/pricing e /en/terms (il piano gratuito),
/en/docs/ensemble-api e /en/docs/ensemble-mean-api (i membri, la media e la sua conservazione),
/en/docs/previous-runs-api, /en/docs/ecmwf-api (AIFS a 6 ore, la probabilità dall'ensemble IFS);
blend.mdl.nws.noaa.gov/nbm-documentation (NBM); Roberts et al. 2023, BAMS, e EGU23-5837 (IMPROVER e
i suoi simboli); content.meteoblue.com, «meteoblue Learning MultiModel (mLM)»; tnipen.github.io,
«titanlib gridpp», e la presentazione WMO di MET Norway (Netatmo in Yr); research.google,
«MetNet-3».

## 18.1 — La misura nel repo, l'estate scaricata, le due righe di `CLAUDE.md` (committente, 22 nov 2026)

Lo step della Fase 18 («Gli step», «18.1»), chiesto dal committente in una sessione sua, sul ramo
`claude/chiaro-forecast-engine-review-xg6n57` e nella PR #189. **Solo strumenti e documenti:
l'APK è quello di prima, non c'è niente da provare sul telefono.**

**`CLAUDE.md` e `docs/STATES.md`.** Il punto «Numbers are one model's per hour» diventa «Numbers
are what models forecast; states are the app's», con il testo della decisione 2 parola per parola
al posto di «the app never averages two models into a number» e la data del cambio; la frase sui
testimoni dice ora che quel che uno stato legge da più modelli arriva a schermo come numero solo
come una combinazione che ha passato la sua misura (l'umidità media no). Nel punto «Free and
without a key», al posto delle «API a pagamento»: Ensemble, Historical Forecast, Previous Runs,
Single Runs e Satellite Radiation sono nel piano gratuito (pricing e terms, letti il 22 nov
2026), il limite è l'uso non commerciale e il peso, e un'API di misura entra nell'app solo con il
suo peso scritto. In STATES la riga in testa: un numero a schermo è di un modello o di una
combinazione misurata, scritta nella sua sezione («none yet»), quantità e probabilità mai. La
frase del README resta fino a 18.2, com'era scritto.

**`tools/analyze_against_metar.py`, `--blend` e `--sky-blend`**, un rapporto per periodo, con il
criterio di 18.2 o di 18.3 stampato in fondo a ciascuno (decide solo l'estate). La media è quella
della decisione 3: `best_match` più IFS e UKMO, ciascuno fuori nell'ora in cui manca o in cui la
prova dell'app (`same_model`, sei ore per parte) lo riconosce come `best_match`; KNMI e DMI solo
nella media a cinque stampata. Il giorno 1 è l'archivio, il giorno N `previous_day(N−1)` delle
corse dei testimoni; le ore della temperatura sono quelle con il METAR entro 30 minuti e i tre
modelli tutti presenti. Rilanciati sui due archivi:

- **la temperatura oraria torna**: `best_match`, IFS, UKMO e la rampa A, d'inverno e a ottobre,
  sono i numeri della tabella di «Cosa ho trovato», punto 1, a ogni giorno, salvo l'inverno al
  giorno 5 (rampa 1,62, non 1,63) e due celle per gruppo che il terzo decimale sposta (pianura
  padana, inverno giorno 6: 1,96 → 1,64, non 1,95; ottobre giorno 5: 1,44 → 1,20, non 1,21);
- **il cielo torna**: ogni riga della tabella del punto 2 (le ore, le giuste, l'HSS, i tre detti,
  i visti dal satellite), la pianura padana 67% → 81% (HSS 0,37 → 0,54) e il resto 66% → 76%; i
  giorni 2–7 sulla copertura totale a un decimo (giorno 3: 53,0% → 59,9%, non 59,8; giorno 7:
  45,7% → 52,7%, non 52,5; HSS 0,16 → 0,24 al giorno 5);
- **le corse che cambiano** di 2 °C tornano a ottobre al giorno 2 contro 3 (9,1% → 1,2%), di
  pochi decimi altrove (inverno, 5 contro 6: 30,3% → 12,3%, non 30,1% → 12,4%; ottobre 23,5% →
  8,0%, non 23,4% → 8,3%; inverno 2 contro 3: 9,6% → 2,9%, non 9,5% → 2,7%). La differenza è la
  prova delle copie: senza, la media ridà 12,5%, 8,3% e 2,7%. Lo script di lavoro della sezione
  non la applicava qui; lo strumento la applica, come la decisione 3 chiede;
- **la massima e la minima con la media non tornano, e la differenza conta.** I numeri della
  sezione (inverno giorno 7: massima 2,02 → 1,87, minima 2,79 → 2,29, giorno 1: 0,75 → 0,82;
  ottobre giorno 7: 1,65 → 1,43 e 2,03 → 1,77) sono **la media degli estremi propri dei tre
  modelli**, senza la prova delle copie: lo strumento li ridà esatti così. Il criterio (c) e
  l'app di 18.2 leggono invece **gli estremi delle ore della serie con la media**, e la massima di
  una media di ore è più bassa della media delle massime. Letti così: inverno giorno 7, massima
  2,02 → 2,01 e minima 2,79 → 2,33; giorno 1 con la media piena 0,75 → 0,89. **Con quella
  definizione il criterio (c) non passa l'inverno**: giorni 4–7, fuori dalla pianura padana, la
  massima peggiora (1,53 → 1,61 °C; la pianura 2,00 → 1,92, le minime 1,64 → 1,46 e 2,33 → 2,07).
  Ottobre passa (a), (b) e (c). Lo strumento stampa le due definizioni, il criterio e, sotto, «media
  degli estremi propri». **Il criterio non si tocca qui**: decide sull'estate così com'è scritto,
  e l'inverno non decide. Ma se l'estate dice lo stesso, 18.2 si chiude senza codice sulla
  condizione (c), o il committente sceglie prima del verdetto se la massima e la minima della
  media siano gli estremi delle ore (com'è scritto) o la media degli estremi dei modelli (com'era
  misurato). È una domanda per lui, aperta in «Aperto».

Il resto si stampa senza decidere, come chiedono i criteri: le rampe B e C, la media a cinque, i
cambi da 1 °C. A ottobre la copertura media è più vicina alle okta dei METAR di quella di
`best_match`, di giorno (2,81 → 2,35 okta, la classe giusta 21% → 31%) e di notte (3,00 → 2,55;
21% → 28%), e d'inverno lo stesso (giorno 2,41 → 2,18, notte 2,61 → 2,38); «Velato» passa dal
7,2% al 7,6% delle ore di luce, con il sole visto in tre su quattro in entrambi i casi. L'inverno
non ha il satellite: le righe del cielo restano vuote.

**`tools/measure_against_metar.py`: niente da aggiungere.** Un periodo nuovo porta già tutto quel
che i due criteri leggono: l'archivio di ogni modello di `MODELS` con la copertura e i tre strati
(il giorno 1 della temperatura e del cielo), il satellite, i METAR con le nubi per strato (le
okta) e, al completamento, le corse dei testimoni per giorno di anticipo (`--add-witness-runs`, la
temperatura e la copertura di `best_match`, IFS, UKMO, KNMI e DMI ai giorni 1–7), con la
probabilità, le corse italiane e quelle bagnate.

**L'estate sul runner.** Lanciato `Measure against METAR` sul ramo con `start=2026-06-01` ed
`end=2026-08-31` (run 16, 22 nov 2026 alle 21:25 UTC). Il completamento con `start` vuoto parte
**dopo** che il primo ha committato, non in coda dietro di lui: un workflow lanciato a mano
prende il commit del ramo nel momento in cui parte la richiesta, e in coda non vedrebbe il file
nuovo. Per la stessa ragione la sessione non fa push mentre il workflow scarica: il suo commit
finale (`git push origin HEAD:<ramo>`) sarebbe rifiutato da un ramo andato avanti, e la misura
persa. Vale anche per 18.2: prima di un push, il workflow finito e `git pull`.

## 18.2 — La temperatura dal secondo giorno: non entra (committente, 22 nov 2026)

Lo step della Fase 18 («Gli step», «18.2»), chiesto dal committente nella stessa sessione di 18.1
(«Puoi partire qui con lo step successivo?»), sul ramo `claude/chiaro-forecast-engine-review-xg6n57`
e nella PR #189. **Si chiude senza codice, come le regole comuni chiedono a uno step che non passa
il suo criterio: l'APK è quello di prima, non c'è niente da provare sul telefono.**

**Prima della misura, la domanda di 18.1** («La massima e la minima della media, prima del
verdetto di 18.2»): il committente ha scelto **il criterio com'è scritto**, gli estremi delle ore
della serie con la media, prima che l'estate fosse letta.

**L'estate.** Il file `tools/measurements/metar-2026-06-01-2026-08-31.json.gz` è completo: 26
aeroporti, il satellite a 25 (Longyearbyen no), le corse dei testimoni a tutti e 26 (run 16 il
download, run 17 il completamento, committato alle 23:33 UTC). Ai giorni 4, 5 e 6 le ore con i tre
modelli sono un po' meno (52 224, 53 594 e 48 611 contro 57 207): corse che il provider non ha
per qualche giorno, contate come mancano. `--blend` sull'estate, con il criterio così com'è:

| | giorno 1 | 2 | 3 | 4 | 5 | 6 | 7 |
|---|---:|---:|---:|---:|---:|---:|---:|
| `best_match` | 1,02 | 1,25 | 1,23 | 1,32 | 1,47 | 1,75 | 1,96 |
| IFS | 1,15 | 1,27 | 1,36 | 1,43 | 1,57 | 1,72 | 1,97 |
| UKMO | 1,32 | 1,47 | 1,54 | 1,65 | 1,82 | 1,99 | 2,17 |
| media a tre | 0,93 | 1,09 | 1,16 | 1,24 | 1,37 | 1,52 | 1,71 |
| rampa A | 1,02 | 1,16 | 1,14 | 1,24 | 1,37 | 1,52 | 1,71 |

- **(a) passa**: l'errore orario dei giorni 5–7 scende dell'11,3% (1,734 → 1,537 °C).
- **(b) passa**: nessun giorno né gruppo sale; il peggiore è il giorno 1, dove la rampa è zero.
- **(c) non passa**: giorni 4–7, **la minima in pianura padana peggiora, 1,48 → 1,68 °C**, giorno
  per giorno (1,30 → 1,46 al giorno 4, 1,65 → 1,91 al 7). Le altre tre migliorano o restano: la
  massima in pianura 1,47 → 1,24, fuori 1,61 → 1,44, la minima fuori 1,63 → 1,61.
- **Con l'altra definizione non sarebbe cambiato niente**: la media delle minime proprie dei
  modelli, stampata e non decide, fa in pianura 1,48 → 1,60. Peggiora anche lei.

Si stampano, senza decidere: l'inverno non passa (c) per la massima fuori dalla pianura (18.1),
ottobre passa tutto; le rampe B e C e la media a cinque, dal giorno 4 uguali alla A; le corse
cambiano meno anche d'estate (2 °C o più fra il giorno 5 e il 6: 21,0% → 7,9%). E al giorno 1 la
media piena, d'estate, sarebbe meglio di `best_match` (1,02 → 0,93), non peggio come d'inverno:
non decide, perché il criterio è scritto sulla rampa.

**Quindi**: nessuna media della temperatura nel codice, la frase del README («the numbers on
screen are always the model's») resta vera e resta, la guida e il CHANGELOG non cambiano.
`docs/STATES.md`, la riga in testa: la temperatura dal secondo giorno misurata e lasciata fuori.
La regola dei numeri di `CLAUDE.md` resta com'è (decisione 2: cambiava comunque andassero le
misure). Dopo i numeri non si cerca un sottoinsieme in cui passi (i criteri): nessuna variante
senza la pianura, senza le minime o con un'altra rampa.

**Che cosa cambia per il resto della fase**: **18.5 cade**, perché lo step stesso dice «solo se
18.2 è entrato»; **18.4** dipende ora solo da 18.3; **18.3** non dipende da questo («18.2 e 18.3
non dipendono l'uno dall'altro»), e chi arriva scrive la classe della media.

## 18.3 — Il cielo dei tre modelli: entra (committente, 23 nov 2026)

Lo step della Fase 18 («Gli step», «18.3»), chiesto dal committente nella stessa sessione di 18.1 e
18.2 («Procedi con il passo successivo»), sul ramo `claude/chiaro-forecast-engine-review-xg6n57` e
nella PR #189. **Cambia l'APK.**

**Prima la misura**, `--sky-blend` sull'estate 2026 con il criterio così com'è (25 489 ore di luce
del giorno 1, 26 aeroporti): **passa tutte e tre le condizioni**.

- (a) ore giuste 76,5% → 83,6% (+7,1 punti), HSS 0,332 → 0,437 (+0,105);
- (b) pianura padana 79,3% → 88,3% (HSS 0,293 → 0,343), altrove 75,4% → 81,8% (0,343 → 0,450);
- (c) «niente sole» sotto un sole visto: 851 ore (3,3%) → 163 (0,6%).

Stampati, senza decidere: ottobre passa allo stesso modo (66,1% → 77,6%); dal secondo giorno, sulla
sola copertura, la media vince ogni giorno (65,1% → 71,1% al primo, 56,0% → 67,0% al settimo); la
copertura media è più vicina alle okta dei METAR di giorno (2,77 → 2,22 okta) e di notte (2,92 →
2,22); «Velato» scende dall'8,3% al 5,6% delle ore di luce, il sole visto in 69% di esse contro 72%;
la media a cinque con KNMI e DMI fa lo stesso (83,7%, 0,44) e non vale due modelli che coprono due
giorni e mezzo. Le tabelle sono in `docs/STATES.md` §11, «The sky of three models».

**Il codice.**

- `:core:domain`, **`ModelMean`**: la media di ogni numero del cielo (copertura e tre strati) di
  `best_match` e dei testimoni dati, a pari peso, arrotondata al percento; un modello conta solo
  con tutti e quattro i numeri; senza `best_match` niente media, senza testimoni il cielo è il suo.
  La sola grandezza è il cielo: la temperatura non è entrata (18.2). Test: `ModelMeanTest`.
- **`Witnesses`**: legge anche gli strati dei testimoni e dà `skyAt(ora)`, il cielo di IFS e UKMO
  (`MEAN_MODELS`; KNMI e DMI restano solo della nebbia, decisione 3) senza quello che è `best_match`
  stesso. La prova delle copie legge ora la risposta così com'è arrivata, tenuta da `of`, e non più
  quella passata a `at`: dopo la media la nuvola di `best_match` non è più la sua, e una copia non
  si riconoscerebbe. `at(best, instant)` diventa `at(instant)`. Test: `WitnessesTest`, tre nuovi.
- **Il mapper**, `withMeanSky`: prima di tutto il resto, la copertura e gli strati di ogni ora e del
  blocco corrente (con i testimoni della riga in cui cade, come la nebbia) diventano la media. Così
  li leggono da lì lo stato (la regola 6 e «Velato»), l'etichetta del giorno, il cielo disegnato e
  il nastro (`CloudLayers`), il tile delle nuvole, il verdetto del Cielo, «Cosa è cambiato», le
  regole del lettore e le allerte che leggono la nuvola. La nebbia non cambia: vota con i numeri di
  ogni modello. Test: tre nuovi in `WeatherReportMapperTest` (la media, senza testimoni o con uno
  strato mancante, la copia contata una volta).
- **La richiesta dei testimoni**: `cloud_cover_low`, `cloud_cover_mid`, `cloud_cover_high`. Sette
  variabili per quattro modelli: **2,8 chiamate**, il fetch **7,9 → 9,1**. Scritto accanto alla
  lista e in `CLAUDE.md`; la sonda legge la lista dal sorgente e stampa il peso da sé; la firma della
  cache (`CURRENT_REQUEST`) cambia da sé, e una voce vecchia non è un colpo fresco.
- **Il piede della pagina dice la verità solo quando è vera.** «Aggiornato alle … · dati
  Open-Meteo, nuvole da più modelli» solo se almeno un'ora o il blocco corrente ha avuto la nuvola
  di un testimone (`SystemInfo.cloudsFromModels`); senza testimoni (una richiesta fallita, una voce
  della cache di prima) il piede resta com'era, perché il cielo è allora di un modello solo. «Più
  modelli» e non «tre»: se uno dei due globali manca, sono due.
- **I testi**: la guida (IT/EN, «I dati»), una frase: i numeri sono del modello scelto per il luogo,
  tranne le nuvole, la media di tre, perché contro i satelliti è più vicina al cielo che si vede. Il
  README: una riga nella tabella delle fonti, e la frase «the numbers on screen are always the
  model's» riscritta con l'eccezione e il suo numero. Il CHANGELOG, `[Unreleased]`, «Changed».
  `docs/STATES.md`: la regola 6 con la media, «Velato» con le quote nuove, §4 (senza testimoni il
  cielo è di `best_match`), §10 (misurato al giorno 1 e di giorno; la notte solo con le okta;
  nessun inverno), §11 con la tabella dell'estate, §12 (i testimoni prestano la nuvola), la tabella
  «Where it lives» e la riga in testa.

**Le immagini del README non cambiano**: la registrazione di Milano non ha i testimoni, quindi
niente media, e il piede di `today-details.png` resta «Open-Meteo data», com'è vero per quella
registrazione. 18.4 la rifà con i testimoni.

**«Cosa è cambiato»**, al primo fetch dopo l'aggiornamento, può segnare una volta un cielo cambiato
dal metodo e non dal tempo (la regola comune della fase).

**Cosa provare sul telefono** (l'APK del commit, accanto alla versione di prima):

- la parola dell'hero contro il cielo fuori dalla finestra, per qualche giorno: meno «Coperto» sotto
  un sole che fa ombra, meno «Nuvoloso» sotto un cielo aperto;
- il cielo disegnato, il nastro della settimana e il tile delle nuvole che dicono la stessa cosa;
- il piede della pagina di Oggi, in fondo: «nuvole da più modelli»;
- una notte nel verdetto del Cielo;
- una mattina di nebbia che resta nebbia.

## 18.4 — La registrazione del README con i testimoni (committente, 23 nov 2026)

Lo step della Fase 18 («Gli step», «18.4»), chiesto dal committente nella stessa sessione
(«Procedi con il prossimo passo»), sul ramo `claude/chiaro-forecast-engine-review-xg6n57` e nella
PR #189. Si fa perché 18.3 è entrato. **Solo strumenti, test, risorse e immagini**; l'APK di questo
step cambia per la correzione dell'hero della sezione dopo, non per la registrazione.

- **`tools/record_readme_data.py`** registra anche `witnesses.json`: la richiesta dei testimoni
  com'è nell'app, i modelli e le variabili letti da `OpenMeteoApis.kt` come le altre liste.
  **`MilanRecording`** la passa al mapper quando c'è: le schermate leggono la nebbia dei due
  modelli e il cielo dei tre, come un telefono. Un commit a sé, prima della registrazione.
- **La registrazione rifatta** dal container il 23 nov 2026 alle 09:35 (Open-Meteo rispondeva; il
  429 del 22 nov era passato): Milano, un lunedì di velo alto al mattino, pioggia da domani sera.
  Cambia ogni immagine, in un commit suo.
- **La notte** (`MilanRecording.night`) è ora le 19:00 della sera registrata, la prima ora intera
  di notte piena (sole a −23°) e anche la più aperta della notte (48% di nuvola, le altre dall'81 al
  100%). L'esploratore del cielo, a quell'ora e con quella nuvola, mostra la luna quasi piena e
  poche stelle, nessuna costellazione tracciata: è quel che dice la registrazione, e l'ora non si
  sceglie per l'aspetto. Una registrazione futura in una sera serena le riporterà.
- **Le immagini rigenerate, tutte guardate**: quattordici cambiano (`guide.png` e `settings.png`
  no). Le didascalie e gli alt del README riletti e riscritti dove la giornata nuova dice altro
  (il velo invece del coperto, i verdetti, la frase di martedì, la notte alle 19:00, l'esploratore
  al 48%); `today-details.png` mostra ora il piede «clouds from several models». Senza trattini
  lunghi, come vuole `CLAUDE.md`.
- **Sei classi di test di `:app` leggono la stessa Milano** (`MilanRecording`, in `testDebug`) e
  aspettavano la giornata di settembre: `AlertsPromiseTest`, `SkyHeroPageTest`,
  `SkyMomentsRowsTest`, `SkyStaleVerdictsTest`, `TodayAgendaTest`, `WindowWeatherTest`. Riscritte
  sulla giornata nuova, con la stessa cosa da provare: le ore (l'ora d'oro 15:59–16:46, l'alba
  07:34), l'età della previsione la sera (9 ore), il verdetto che c'è («So-so», non «Great»), la
  parola della finestra («6° · High cloud»). Una sola ha dovuto cambiare forma: le due regole del
  README quella mattina non valgono insieme (5,9° sono sotto i 12 di «Bike»), e la riga che unisce
  le regole che valgono ora si prova con «Run» e la stessa regola sotto un altro nome.
  `MilanRecording.today` prende per questo le regole come parametro; le immagini non cambiano.

## La verifica: testo e grafica nell'hero e nelle fasce del giorno (committente, 23 nov 2026)

Chiesta con 18.4: «una verifica che nell'app non ci siano contraddizioni nella visualizzazione dei
dati nell'hero e nelle fasce giornaliere tra testo e grafica».

**Come.** Oltre alla lettura del codice (`TodayStateBuilder`, `DaylightPhases`, `OpenDaySentence`,
il disegno del cielo), una prova sui dati veri: previsione e testimoni di 16 città scaricati il 23
nov 2026 alle 09:30 come li chiede l'app (Milano, Cavenago, Torino, Bologna, Venezia, Bolzano,
Roma, Napoli, Palermo, Cagliari, Londra, Parigi, Oslo, Madrid, Vienna, Atene), più la registrazione
del README, ciascuna con e senza testimoni (cioè anche come `main`), passati dal costruttore di Oggi.
Per l'hero: la copertura che il disegno usa contro quella del blocco, che la parola e il tile
leggono; il secchio del cielo detto contro quello disegnato. Per ogni riga della settimana:
un'icona bagnata senza un'ora bagnata nel suo nastro; una riga senza frase; segni di pioggia nel
nastro senza parole; un cielo «tutto il giorno» nella frase diverso dall'icona; ore fuori dalla
massima e dalla minima. Una classe di prova temporanea, non committata.

**Una contraddizione, corretta** (anche su `main`): **il cielo dell'hero disegnava la copertura
dell'ora intera e gli strati del momento.** `TodayStateBuilder` passava al disegno
`currentHour.cloudCoverPct`, la riga dell'ora (l'istante dell'ora piena, fino a 59 minuti prima),
con gli strati, la parola, la nebbia e il tile del blocco corrente. Diverse in 16 città su 17:
Cavenago 82% disegnato sotto «Quasi sereno» e un tile del 41%, Milano 29% sotto un tile del 53%,
Londra 0% sotto 15%. Ora il disegno legge la copertura del blocco, e la riga solo dove il blocco
non ne ha. Rilanciata la prova: nessuna differenza. Test in `RibbonScrubTest`; il CHANGELOG, «Fixed».

**Una da decidere: la riga di oggi.** A Parigi, alle 9:30, la riga di oggi porta l'icona «Pioggia»:
la pioggia è caduta fra mezzanotte e le 6 (1,2 mm), e da lì il giorno si apre fino al sereno.
L'etichetta del giorno legge tutto il giorno del provider (STATES §6), il nastro e la frase solo le
ore che restano; così la riga dice pioggia, il nastro sole, e la frase non c'è (non ha ore che
reggano l'etichetta). Non è un errore di un numero: è che cosa vuol dire l'icona di oggi, «la
giornata» o «il resto della giornata». Lasciato com'è, in «Aperto».

**Guardate e non contraddizioni**, con la ragione:

- in nessuna città né riga una frase contraddice l'icona, un nastro ha pioggia senza parole, o le ore
  escono dalla massima e dalla minima;
- **la tinta del cielo** (lo sfondo dell'hero e il nastro) si spegne con la copertura totale, la
  parola e le nuvole disegnate con la luce che gli strati tolgono: sotto un velo alto il cielo
  diventa lattiginoso, che è quel che un velo fa, mentre la parola dice «Velato» e il disegno il
  velo (DESIGN §3.3, scelto il 19 ott 2026);
- **la frase di martedì della registrazione**, «Overcast, light rain in the evening», sopra ore del
  mattino con un po' di sole: la regola delle parti del giorno, un pareggio fra i cieli della
  mattina va al più pesante (`OpenDaySentence`, scritta il 26 set 2026).

## Le tre cose del 23 nov 2026: la riga di oggi, la tinta del cielo, il pareggio della frase (committente, 23 nov 2026)

Dopo la verifica di testo e grafica (la sezione qui sopra), il committente: per la riga di oggi è
«propenso per il resto della giornata», e chiede il mio parere, come si comportano le app premium,
e se sono d'accordo, «indipendentemente dalle regole attuali», con le due cose che avevo dato per
non contraddizioni (la tinta del cielo sotto un velo, il pareggio della frase di martedì). Al mio
parere: «Fai tutte e tre le cose», con le immagini prima e dopo della tinta, una verifica che la
pioggia e la neve restino visibili di notte e sotto un cielo scuro, e una review alla fine.

**Il criterio del pareggio, scritto prima dei numeri.** `tools/analyze_against_metar.py
--sky-ties`. Per aeroporto, giorno e parte (mattina 6–12, pomeriggio 12–18 locali), le ore di
luce del giorno 1 con il sole sopra 15° e senza precipitazione né nebbia viste; ogni ora vota il
suo cielo com'è nell'app (la media dei tre modelli, la luce presa, il velato) piegato nei tre
«look» della frase: sole (sereno, quasi sereno, velato), sole e nuvole (nuvoloso), grigio
(coperto). Solo le parti con almeno due ore e un pareggio fra due look. Il pareggio va al più
leggero se, **sull'estate 2026 e su ottobre, ciascuno**, la quota delle ore delle parti in
pareggio in cui il satellite vede il look detto è più alta che con il pareggio al più pesante.
Si stampano senza decidere: le parti in cui il look detto è quello che il satellite vede più
spesso; «grigio» detto dove il satellite vede soprattutto sole e il contrario; la pianura padana
e il resto; la mattina e il pomeriggio. La sera non si misura: d'inverno e d'autunno non ha
ore di sole sopra 15°. La misura legge le sole ore che il satellite vede; la frase dell'app vota
con tutte le ore di luce della parte.

**Il criterio del pareggio dell'etichetta del giorno, scritto prima dei numeri** (dopo la review
dei tre cambi, che l'ha chiesto: con un tratto solo la frase prende la parola dell'icona, e se
l'icona pareggia al più pesante la frase torna a dire «Overcast» sopra un cielo che le sue parti
leggevano più leggero). Stesso strumento, `--sky-ties`, la riga «giorno»: per aeroporto e giorno,
le stesse ore (di luce, sole sopra 15°, niente pioggia né nebbia viste) votano il loro codice di
cielo (sereno, quasi sereno, velato, nuvoloso, coperto), come `DayLabel` al passo 3; solo i giorni
in cui due codici pareggiano. Il pareggio di `DayLabel` va al più leggero (per `WmoCode.severity`)
se, sull'estate 2026 e su ottobre, ciascuno, la quota delle ore di quei giorni in cui il satellite
vede il look del codice detto è più alta che con il pareggio al più pesante. Se non passa, la
frase con un tratto solo continua a dire la parola dell'icona: l'icona e la frase non si
contraddicono, e il pareggio delle parti vale dove le parti sono più d'una.

**I numeri.** `--sky-ties` sui due periodi:

| | parti o giorni in pareggio | al più pesante | al più leggero |
|---|---:|---:|---:|
| Estate 2026, le parti (mattina e pomeriggio) | 191 di 4 473 | 25,6% delle ore | **60,5%** |
| Ottobre, le parti | 75 di 1 305 | 32,9% | **46,8%** |
| Estate 2026, il giorno (`DayLabel`) | 141 di 2 291 | 58,0% | **73,6%** |
| Ottobre, il giorno | 74 di 708 | 57,1% | **72,6%** |

I due criteri passano. Il pareggio al più pesante non dice mai «sole» dove il satellite vede
soprattutto grigio, ma dice «grigio» dove vede soprattutto sole (18 parti d'estate, 11 a ottobre);
il più leggero fa l'errore opposto (15 e 12), e vede giusto più del doppio delle ore d'estate.

**Le decisioni**, tutte prese dal committente su un mio parere, e i perché:

1. **La riga di oggi è il resto della giornata** (`RestOfDay`, `:core:domain`). L'icona, la
   parola, la probabilità e quello che la riga mostra aperta (la pioggia e le sue ore, la neve, la
   quota neve, la raffica, l'UV) si leggono sulle ore che restano, quando la pagina si guarda, con
   le regole del giorno; la massima e la minima restano del giorno intero, perché la barra porta
   «adesso» su quell'intervallo. Le regole stanno ora in **`DayLabel`**, una volta sola: il mapper
   le legge sugli slot del provider, Oggi sulle ore che restano. Il giorno del report resta quello
   del provider: «Cosa è cambiato», le regole del lettore e i riassunti leggono e confrontano
   quello, e un'ora che passa non è una revisione della previsione. Le app premium: i dati di Apple
   (WeatherKit) danno il giorno e il «resto di oggi» separati, e l'icona del giorno dell'app di Apple
   è criticata proprio per la pioggia della notte sotto un giorno di sole; Instant Weather documenta
   il resto della giornata per oggi; delle altre non ho trovato documentazione.
2. **La tinta del cielo legge la luce che la nuvola toglie** (`SkyPalette.gradient`, `lightPct`).
   L'appiattimento dello zenit, lo scurimento, l'ingrigimento e la luna nascosta seguono la luce; la
   copertura che la luce lascia passare, il velo, ingrigisce a metà (`VeilWhitening` 0,5, scelto
   sulle immagini fra 0 e 1). Un'ora in cui cade qualcosa tiene la copertura intera, la regola delle
   nuvole disegnate (`cloudOpacity`): sotto la pioggia e la neve il cielo e il contrasto di quel che
   cade (il pavimento di luminosità `FallLightFloor`, il ritocco della notte) sono quelli di prima.
   **Verificato pixel per pixel** su dieci scene costruite sulla Milano registrata: il coperto basso
   di giorno e di notte, la pioggia sotto nubi alte, la pioggia bassa, la pioggia e la neve di notte
   sono identici; cambiano il velo di giorno (da grigio a lattiginoso), il velo di notte (più chiaro
   sotto la luna), il cielo medio-alto e, di poco, il sereno. Le immagini prima e dopo sono state
   mostrate al committente. La leggono l'hero, la sua barra, i widget, la finestra, la pagina del
   Cielo di notte, i nastri di Oggi e quello della notifica; l'esploratore legge solo la copertura e
   la tiene.
3. **Il pareggio va al più leggero**, nella frase del giorno aperto (fra i look delle parti) e
   nell'etichetta del giorno (fra i cieli del passo 3), sui numeri qui sopra; la nebbia tiene il suo
   pareggio (non misurata, un avviso prima di un cielo). Dentro un look la parola pareggia ancora
   al più pesante (sereno contro quasi sereno: non misurato, lo stesso look).

**La review** (`code-review`, livello alto, sui commit dei tre cambi) ha trovato cinque cose, tutte
corrette nello stesso giro:

- **Il temporale possibile di oggi**: un temporale passato nella notte svuotava il campo del giorno
  (il codice aveva parlato) e zittiva la possibilità del pomeriggio sulla riga, sull'hero e nel
  riassunto del mattino. `PossibleStorm.ahead` ora tace solo senza CAPE: un temporale ancora davanti
  è fra le ore e zittisce come prima. Test in `PossibleStormTest`, STATES §7.
- **La frase con un tratto solo** prende la parola dell'icona, che pareggiava ancora al più
  pesante: da qui la seconda misura e il pareggio al più leggero anche in `DayLabel`, con il suo
  criterio scritto prima.
- **«Cosa è cambiato»** disegnava accanto a una revisione del giorno l'icona della riga, che ora è il
  resto della giornata: legge il giorno del report, quello che la revisione confronta.
- **La pagina del Cielo** disegnava il primo fotogramma, prima che la scena fosse misurata, con la
  sola copertura: ora con la luce, come la scena. DESIGN §3.3 lo diceva al contrario, corretto.
- **Il registro** non aveva i numeri e la casella era aperta: questa parte.

Le immagini del README rigenerate e guardate: l'hero velato e i nastri più azzurri, la frase di
martedì «Partly cloudy in the morning, overcast from the afternoon, light rain in the evening»
sopra un mattino con il sole nelle icone; l'alt della settimana riscritto.

**Cosa provare sul telefono**: la riga di oggi un pomeriggio dopo una notte di pioggia (sole, e
nessuna percentuale della notte); un giorno di velo alto, l'hero e il nastro lattiginosi e non grigi;
una sera di pioggia o di neve, che si vedono come prima; una frase del giorno aperto su un giorno
metà sole e metà grigio.

## «Cosa è cambiato» esce dall'app (committente, 23 nov 2026)

Il committente: «la sezione "Cosa è cambiato" è veramente utile o no? … è rumore?». Gli ho risposto
con una misura, sulle corse per giorno di anticipo degli archivi della Fase 18 (script di lavoro):
la massima di un giorno cambia di 1 °C o più da una corsa a quella del giorno dopo nel 18–35% dei
confronti ai giorni 2–5 e nel 45–56% ai giorni 6–7 (estate e inverno; la minima uguale), dentro
l'errore della previsione stessa (al giorno 6 la massima sbaglia di 1,5–1,8 °C), e tre revisioni
su dieci sopra 1 °C sono riprese per metà o più dalla corsa dopo (30–31%; 26–27% sopra 2 °C). La
sezione, con le soglie di 1 °C, 10 punti di probabilità e ogni cambio di parola, diceva soprattutto
il rumore del modello. Ho proposto di restringerla a quel che cambia un piano; il committente:
«vorrei togliere completamente "Cosa è cambiato" … in futuro magari mettere qualcosa che dia più
valore aggiunto. Così rimane anche più pulita la schermata oggi. Alla fine era solo una funzione
portata da tweather», e la rimozione su questa PR.

**Che cosa è uscito:**

- lo schermo: la sezione dopo la settimana e il suo posto nel secondo riquadro della pagina larga
  (`TodayScreen`), il componente (`TodayChanged.kt`), il costruttore e i testi
  (`WhatChanged.kt`), il campo dello stato (`TodayUiState.Content.whatChanged`) e il suo calcolo nel
  ViewModel, che ora legge dalla cronologia solo la massima di ieri;
- il motore del confronto, `ForecastDiff` e `SnapshotDiff` (`:core:data`), con i loro test;
- il peso di una parola (`ConditionWord.severity`) e la sua ricerca per id (`fromId`), che solo il
  confronto leggeva; gli id restano, il nome stabile di ogni parola, con il loro test;
- le stringhe, IT e EN: il titolo, le frasi della revisione, la voce della guida;
- nei documenti: la riga del prodotto in `CLAUDE.md` e la nota sulla cronologia, la voce del README,
  VISION §2 punto 2 e §5.2.6 (cancellato, con il perché), DESIGN §8.0 e §8.10, STATES, il
  CHANGELOG («Removed»).

**La cronologia resta, più piccola.** `weather_history` serve ancora alla frase della giornata
tranquilla (la massima di ieri) e a «ultima volta» delle regole negli avvisi. Ogni riga teneva la
settimana (parola, massima, minima e probabilità di oggi e dei sette giorni dopo), che solo il
confronto leggeva: ora tiene la sola massima di oggi (`WeatherSnapshots.flattenForecast`), la
chiave che si rilegge il mattino dopo. Le righe scritte prima tengono la loro settimana finché la
durata della cronologia (tre giorni) non le toglie, e si leggono uguali: la chiave è la stessa.
Niente migrazione del database: la colonna è la stessa, cambia quel che ci si scrive. Le etichette
inglesi di tweather che il confronto leggeva sulle righe più vecchie (`LegacyLabels`) sono uscite.

**Lo schermo che resta**: dopo la settimana vengono i dettagli. Le immagini del README non la
mostravano (la registrazione ha un aggiornamento solo, e la sezione non c'era).

Cosa provare sul telefono: Oggi scorso fino in fondo, la settimana e poi i dettagli, e nessuna
sezione fra le due; la frase della giornata tranquilla che dice ancora la massima rispetto a ieri,
dal secondo giorno d'uso; le regole che dicono ancora quando sono scattate l'ultima volta.

## 18.6 — La chiusura della Fase 18 (committente, 23 nov 2026)

Il committente, dopo la prova sul telefono: «Ho provato e tutto funziona. Prepara la PR e toglila
dalla modalità bozza così mergio tutto in main se la fase è finita».

**La fase contro i suoi criteri:**

| Step | Criterio, scritto prima dei numeri | Esito | Codice |
|---|---|---|---|
| 18.1 | ritrovare i numeri della fase; l'estate scaricata | ritrovati (le differenze e il perché in «18.1»); l'estate sul runner | strumenti e documenti |
| 18.2 | (a) −5% ai giorni 5–7, (b) nessun gruppo +0,02 °C, (c) massima e minima 4–7 | (a) −11,3% sì, (b) sì, (c) **no**: la minima in pianura padana 1,48 → 1,68 °C | nessuno |
| 18.3 | (a) +5 punti e +0,05 HSS, (b) in pianura e fuori, (c) «niente sole» col sole non più frequente | 76,5% → 83,6%, HSS 0,33 → 0,44; 79 → 88% e 75 → 82%; 3,3% → 0,6% | `ModelMean`, i testimoni con gli strati |
| 18.4 | si fa se 18.2 o 18.3 entra | fatto: Milano registrata con i testimoni | test, risorse, immagini |
| 18.5 | solo se 18.2 entra | cade | nessuno |
| fuori dagli step | il pareggio delle parti (criterio scritto prima) e dell'etichetta (idem) | 25,6% → 60,5% e 58,0% → 73,6% d'estate; 32,9% → 46,8% e 57,1% → 72,6% a ottobre | `DayLabel`, `OpenDaySentence` |

Fuori dagli step, chiesto dal committente in questo periodo e scritto qui sopra: l'hero che
disegna la copertura del momento, la riga di oggi come resto della giornata, la tinta del cielo
che legge la luce, il temporale possibile non zittito da uno passato, «Cosa è cambiato» tolta.

**I controlli di chiusura:**

- il ramo contiene `main` (`a0d422c`, nessun commit nuovo su `main` dall'apertura); la CI verde
  sull'ultimo commit; in locale `test`, `:app:testDebugUnitTest`, `:app:lintDebug`,
  `:app:assembleDebug` verdi;
- i controlli meccanici di `CLAUDE.md`: nessun colore a mano fuori da `ui/theme` nel diff, nessun
  gergo nelle stringhe nuove, le chiavi IT e EN uguali (le quattro solo inglesi sono le stesse di
  prima, non tradotte per scelta), il peso delle richieste scritto (9,1 a fetch, in `CLAUDE.md` e
  accanto alla lista), nessun nome delle app sorelle nel codice;
- la guida, il README e il CHANGELOG (`[Unreleased]`) dicono le stesse cose: il cielo di più
  modelli, la riga di oggi, il velo, il pareggio, «Cosa è cambiato» tolta;
- **la review dell'intero diff della PR** (`code-review`, livello alto): nessun difetto da
  correggere prima dell'unione; tre note minori, lasciate come sono e scritte qui:
  - il piede dice «nuvole da più modelli» se almeno un'ora ha avuto la media, anche dove il blocco
    del momento non l'ha avuta (quando la sua riga manca dalla serie): raro, e le ore della pagina
    sono comunque di più modelli;
  - la pagina del Cielo, nel solo fotogramma prima che la scena sia misurata, combina la
    copertura della lettura con gli strati dell'istante animato: un fotogramma di tinta, mai una
    parola;
  - il resto di oggi vota la luce del giorno dal sole all'inizio dell'ora, l'etichetta del giorno
    intero dall'`is_day` del provider alla fine dell'ora: l'ora del tramonto può votare diverso nelle
    due.

**«Aperto» riletto.** Le tre prove dal vivo del periodo sono chiuse dalla prova del committente
(«Ho provato e tutto funziona»); resta aperta, perché la stagione non la permette ora, la parola
del cielo dei tre modelli d'estate e in una mattina di nebbia. La Fase 18 è chiusa; il committente
unisce la PR #189.

**L'archivio.** Il registro passava le mille righe: diventa `docs/planning/17-novembre-19-23.md`,
dal 19 al 23 nov 2026 (la review della Fase 17, la cache, il grafico di «Maltempo», Milano tolta
dal codice, la Fase 18 intera), e qui resta un registro vuoto; l'indice è rifatto con
`tools/planning_index.py`.
