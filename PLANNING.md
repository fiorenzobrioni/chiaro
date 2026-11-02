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

- **Rilasciata**: 2.0.0 (2 nov 2026, `v2.0.0`; archivio 10, «La 2.0.0»), dopo la 1.1.0
  (1 ott 2026, `v1.1.0`). Quel che entra dopo sta nella sezione `[Unreleased]` del `CHANGELOG.md`.
- **Chiusa il 29 ott 2026**: **Fase 15**, la schermata Avvisi dopo la revisione (dal 28 ott 2026): dodici step,
  uno per chat, in ordine di urgenza (quel che mente, poi il rumore, poi le migliorie, poi il valore
  nuovo); la revisione, le decisioni, le regole comuni e la descrizione di ogni step sono in
  `docs/planning/08-ottobre-27-28.md`, «Fase 15 — La schermata Avvisi: la revisione e i passi»;
  gli step, chiusi tutti e dodici, sono in `docs/planning/09-ottobre-28-29.md`. La rilettura
  dei dodici (archivio 10, «La Fase 15 riletta», 29 ott 2026) ha lasciato due step di coda, 15.13 e
  15.14, chiusi lo stesso giorno (archivio 10, «15.13» e «15.14»).
- **Ancora aperta**: **Fase 14**, il motore contro i motori premium (dal 27 ott 2026): tredici step, uno per chat; i primi quattro (14.1–14.3 con la scadenza del 30 novembre, la visibilità ICON che sparisce; 14.4 la misura di ICON-2I, che ha detto no), il 14.6 (il caldo), il 14.7 (il vento), il 14.8 (la quota neve), il 14.9 (le unità), il 14.10 (la concordanza dei modelli, che ha detto no) e il 14.13 (le icone, prima dell'inverno) sono chiusi; 14.5 e 14.11 non esistono. La revisione, le decisioni, le regole comuni e la descrizione di ogni step sono in `docs/planning/07-ottobre-22-27.md`, «Fase 14 — Il motore contro i motori premium»; gli step chiusi dal 27 ott 2026 e l'attesa di 14.12 sono in `docs/planning/08-ottobre-27-28.md`; uno step chiuso da qui in poi scrive la sua sezione qui.

## Aperto

Quel che resta da fare o da decidere, in un posto solo: una casella si apre qui, con il rimando
alla sezione del registro da cui nasce, e si chiude qui, con quella che la chiude.

- [ ] **Il cielo d'inverno contro il satellite.** La regola 6 è misurata su ottobre soltanto:
  l'archivio del satellite di Open-Meteo parte da febbraio 2026. Rilanciare
  `tools/measure_against_metar.py` su dicembre–gennaio 2026-27 e rileggere STATES §11 (archivio 07,
  «Il motore dopo la revisione», 26 ott 2026).
- [ ] **Il meteo di Chiaro ad altre app con la trasmissione di Gadgetbridge** (issue #72). Decisa,
  da fare quando c'è spazio: `ACTION_GENERIC_WEATHER`, spenta di default, gli stati di Chiaro e non
  il `weather_code`, nessuna richiesta in più se il job gira già, niente coordinate, un intent
  esplicito per app (registro, «Il meteo di Chiaro ad altre app: la trasmissione di Gadgetbridge»,
  2 nov 2026). Quando esce, si scrive sull'issue e la si chiude.
- [x] **14.1** La sonda del provider, il quarto d'ora dove è calcolato, le regole (Fase 14). Fatto il 27 ott 2026 (archivio 07, «14.1»).
- [x] **14.2** La nebbia senza la visibilità di ICON: la misura. Fatto il 27 ott 2026 (archivio 07, «14.2»): la regola scelta entra con 14.3, subito.
- [x] **14.3** La nebbia senza la visibilità di ICON: il codice, e il tile. Fatto il 27 ott 2026 (archivio 07, «14.3»): la regola di 14.2 è nel motore e vale da oggi, un mese prima di DWD.
- [x] **14.4** ICON-2I contro ICON-EU e ICON-D2: la misura. Fatto il 27 ott 2026 (archivio 08, «14.4»): ICON-2I non vince in nessuna cella decisiva, non entra.
- [x] **14.5** ICON-2I nel report, dove ha vinto (solo se 14.4 lo dice). Chiusa il 27 ott 2026 senza lavoro: 14.4 ha detto no (archivio 08, «14.4»).
- [x] **14.6** Il caldo: il gradino della frase e il pericolo. Fatto il 27 ott 2026 (archivio 08, «14.6»).
- [x] **14.7** Il vento nel «Maltempo», con la quota del luogo. Fatto il 27 ott 2026 (archivio 08, «14.7»): le soglie sono quelle della tabella MeteoSwiss letta dal documento, non quelle scritte nello step.
- [x] **14.8** La quota neve. Fatto il 28 ott 2026 (archivio 08, «14.8»): la riga nel giorno aperto e nella sera, due variabili in più (5,4 chiamate la previsione, 8,4 il fetch).
- [x] **14.9** Le unità: m/s, nodi, Beaufort, inHg, mmHg. Fatto il 28 ott 2026 (archivio 08, «14.9»): i default restano km/h e hPa, gli id canonici delle regole restano `_kph` e `_mb`.
- [x] **14.10** La concordanza dei modelli: la misura. Fatto il 28 ott 2026 (archivio 08, «14.10»): lo scarto fra i modelli dice quanto si sbaglia in media, non se il giorno è sbagliato; «non concordano» non è vera due volte su tre a nessuna soglia che scatti.
- [x] **14.11** La concordanza a schermo (solo se 14.10 lo dice). Chiusa il 28 ott 2026 senza lavoro: 14.10 ha detto no (archivio 08, «14.10»).
- [ ] **14.12** Gli avvisi ufficiali fuori dall'Italia, a chiusura garantita: **in attesa** dal 28 ott 2026, su via libera del committente, quando ci sarà una base di lettori in Europa (archivio 08, «14.12 resta in attesa»).
- [x] **14.13** Le icone che mancano: pioggia e pioviggine gelata, pioggia e neve possibile, composte. Fatto il 27 ott 2026 (archivio 08, «14.13»).
- [x] **15.1** Il «vero adesso» delle regole legge il report di Oggi, dice l'età, e l'anteprima dell'editor resta viva (Fase 15, archivio 08). Fatto il 28 ott 2026 (archivio 09, «15.1»): una funzione sola prepara il report per Oggi e per Avvisi, `today.*` legge la riga datata oggi, la pagina rinfresca da sé.
- [x] **15.2** Lo schermo non promette un suono che non può arrivare: la finestra del bollettino per luogo, lo stato «nessun luogo», il bollettino già uscito, le parole dello switch (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.2»).
- [x] **15.3** Un canale silenziato dal telefono è detto a schermo, con la strada per riaprirlo (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.3»).
- [x] **15.4** Il motore delle regole: il riarmo alla riaccensione; il nome unico e l'id nello storico (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.4»): il motore non cambia, è la pagina che riarma; lo storico scrive `id:nome` e legge per id, per nome le righe vecchie.
- [x] **15.5** Le soglie nelle unità del lettore; lo slider senza la fila di tacche (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.5»): la griglia è dell'unità, il salvataggio resta canonico, le tacche solo fino a venti passi.
- [x] **15.6** Il rumore che va via: la riga «in qualunque ora», la legenda del tratteggio, l'idea «Dodici ore asciutte», il flag `userRules` (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.6»): tutto via in uno step, le idee sono sei, una regola accesa tiene vivo il job da sola.
- [x] **15.7** Le righe di Avvisi con le misure del Cielo; l'ora relativa; «Cosa guardare» in uno sheet a quattro gruppi (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.7»): `ListRow` condiviso con il Cielo, l'icona resta a 36, «oggi alle», «ieri alle», lo sheet a quattro titoli.
- [x] **15.8** Le graffe del messaggio diventano parole (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.8»): una `VisualTransformation` dipinge la frase su un chip, il testo salvato tiene l'id, il tasto indietro toglie il chip intero; la resa su telefono, che restava da vedere al committente, è stata vista e tiene (29 ott 2026, archivio 10, «La Fase 15 riletta»).
- [x] **15.9** Le variabili «domani» e l'idea «Neve domani» (Fase 15). Fatto il 29 ott 2026 (archivio 09, «15.9»): sette `tomorrow.*` sulla riga datata domani, una voce per data da mezzogiorno, «Neve domani» dopo «Ghiaccio domattina», e la card che prima di mezzogiorno dice da quando può suonare.
- [x] **15.10** Le quantità nelle prossime ore: millimetri e centimetri (Fase 15). Fatto il 29 ott 2026 (archivio 09, «15.10»): le righe portano il totale e la neve dell'ora, quattro somme `next_Nh.*`, la pioggia senza l'acqua della neve, null se manca un'ora; una finestra sola per ogni `next_Nh.*`, l'ora in corso e le N−1 dopo, e il «Caldo intenso» conta sulla stessa.
- [x] **15.11** La regola legata a un luogo: il modello, il motore, il worker e il peso in chiamate (Fase 15). Fatto il 29 ott 2026 (archivio 09, «15.11»): `NotificationRule.place`, il job legge ogni luogo legato sulla sua previsione e nel suo fuso, 8,4 chiamate per luogo per periodo; la regola orfana si spegne e tiene il luogo; a schermo niente fino a 15.12.
- [x] **15.12** La regola legata a un luogo: l'editor, la card, la notifica (Fase 15). Fatto il 29 ott 2026 (archivio 09, «15.12»), con i punti che 15.11 gli lasciava: «Dove» nell'editor, la riga «A Bormio» con l'età di quella previsione, «Adesso sì/no» e «scattato l'ultima volta» dal luogo della regola, l'anteprima che fa il fetch, l'orfana il cui interruttore chiede dove, Oggi con le sole regole del luogo in pagina. La Fase 15 è chiusa.
- [x] **15.13** La coda della Fase 15 nel codice: il job legge i luoghi legati anche quando il luogo attivo non c'è o non risponde; l'orfana si spegne anche senza il job; il canale del Cielo silenziato è detto a schermo (archivio 10, «La Fase 15 riletta», 29 ott 2026). Fatto il 29 ott 2026 (archivio 10, «15.13»).
- [x] **15.14** La coda della Fase 15 nei documenti: la riga «Alerts» del README, «four groups» nel CHANGELOG, due frasi della guida (archivio 10, «La Fase 15 riletta», 29 ott 2026). Fatto il 29 ott 2026 (archivio 10, «15.14»): la Fase 15 è chiusa del tutto.

Riletto il 2 nov 2026, all'archivio 10, con la 2.0.0: le caselle chiuse dal 29 ott 2026 (15.13, 15.14) rimandano all'archivio 10 (`docs/planning/10-verso-2.0.0.md`); restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, dopo la rilettura della Fase 15: aperte 15.13 e 15.14, la coda; e come prima 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, all'archivio 09: le caselle della Fase 15 sono tutte chiuse (in `docs/planning/09-ottobre-28-29.md`), restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 28 ott 2026, all'archivio 08, lo stesso giorno dell'apertura della Fase 15: le caselle sopra sono della Fase 14 (14.12 in attesa) e della Fase 15 (in `docs/planning/08-ottobre-27-28.md`), nessuna proposta non decisa. Riletto il 27 ott 2026, all'archivio 07: le caselle della Fase 14, nessuna proposta non decisa. Fino al 26 ott 2026 nessuna casella aperta (riletto il 22 ott 2026, all'archivio 06):
le ultime chiuse quel giorno stanno in `docs/planning/06-ottobre-20-22.md`, «Cielo: la finestra
reale nella card Stanotte», «Il widget «Momenti del cielo» va via», «Le notifiche: ogni riga nella
forma che sta in una riga», «Il widget «Le prossime ore» va via».

---

## Registro dal 2 nov 2026, dopo l'archivio 10

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
