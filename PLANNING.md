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
- **Aperta il 10 nov 2026**: **Fase 16**, la schermata Cielo come Oggi e il cielo da esplorare: undici
  step, uno per chat (16.1–16.2 la pagina, 16.3–16.5 il motore e il dato, 16.6–16.10 l'esploratore,
  16.11 la chiusura). La revisione, le tavole approvate, le decisioni, le regole comuni e la
  descrizione di ogni step sono nel registro qui sotto, «Fase 16 — Cielo: la pagina come Oggi e il
  cielo da esplorare»; ogni step scrive la sua sezione in coda.
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
- [x] **L'icona nuova sul telefono** (registro, «L'icona nuova: l'ora blu», 9 nov 2026): l'icona
  nella maschera del telefono, l'icona a tema, l'icona piccola delle notifiche alla sua misura.
  Fatto dal committente il 9 nov 2026 (registro, «Le icone nuove provate sul telefono»).
- [x] **Le icone nuove del Cielo sul telefono** (registro, «Le icone del Cielo, rilette tutte», 9 nov
  2026): la lista del Cielo e il catalogo nelle due famiglie d'icone e nei due temi, e la
  finestra; la luce zodiacale soprattutto, la piu' debole delle sei. Fatto dal committente il
  9 nov 2026, la luce zodiacale compresa (registro, «Le icone nuove provate sul telefono»).
- [ ] **La revisione di Oggi e del cielo sul telefono** (registro, «Oggi e il motore del cielo: la
  revisione da app premium», 9 nov 2026). Quel che un container non può dire: **la cifra della
  GPU** con il cielo peggiore salito da 25 a 28 campioni (`adb shell dumpsys gfxinfo`, sotto 3 ms
  a frame su un telefono della classe del Pixel 6a, DESIGN §3.5; se non tiene, i raggi del sole
  sono i primi a uscire: `rays` a zero in `skyScene`); il nastro trascinato sotto il dito (il
  passo d'ora, il tocco aptico, il ritorno ad adesso, lo scorrimento della pagina che parte da
  lì); la nebbia e il lampo in un
  giorno che li ha; la riga della settimana con lo chevron a 360dp e a 130% di carattere.
- [ ] **L'hero dopo la revisione, sul telefono** (registro, «L'hero dopo la revisione: quanto costa, le
  icone ferme sotto il dito, il cielo con un interruttore suo, il luogo che tornava indietro», 10 nov
  2026). Quel che i test non dicono: le icone ferme mentre si trascina il nastro e lo sfarfallio
  che se ne va; «Cielo animato» spento, su Oggi e sulla card Stanotte; il luogo cambiato da Cielo e
  da Avvisi che su Oggi è quello nuovo dal primo frame, senza viaggio.
- [ ] **La planata del cielo a ogni minuto** (stessa sezione, «Quanto costa l'hero rispetto a prima»):
  `SkyTransition` plana a ogni tick di minuto, un secondo di shader a ogni vsync per uno
  spostamento del sole che non si vede. Da decidere: planare solo quando la differenza si vede
  (la copertura, lo stato, l'ora trascinata), o al passo del cielo (15 o 30 frame al secondo).
- [ ] **Il peso della cifra: Regular nell'app, Bold nelle card** (stesso registro, «La cifra»).
  La revisione accettata chiedeva 300; è 400, il più leggero che le due famiglie disegnano, e
  le card restano Bold: la regola del 20 set 2026 («Una temperatura eroe è Bold», DESIGN §5)
  vale ora per le card soltanto. Da confermare dal committente guardandole fianco a fianco,
  come allora; tornare indietro è una riga (`HeroWeight` in `Type.kt`) e il suo test.
- [ ] **La nebbia, il temporale e la meteora nella finestra e nella card Stanotte** (stesso
  registro, «Dove non arriva»): lo shader li disegna, `WindowScene` e `TonightSky` non li
  chiedono ancora. Proposta, non decisa.
- [ ] **16.1** La pagina come Oggi: l'hero a tutta larghezza, il luogo sul cielo, il verdetto come cifra, il nastro della notte (Fase 16).
- [ ] **16.2** I momenti come l'agenda di Oggi; «Aggiungi» nel titolo al posto del pulsante flottante (Fase 16).
- [ ] **16.3** Il motore: Marte e Saturno; sorgere, culmine e tramonto di un oggetto (Fase 16).
- [ ] **16.4** Il motore: la notte di un'altra data; l'elenco «Da vedere» come dato (Fase 16).
- [ ] **16.5** Le costellazioni: la fonte e la licenza, la risorsa, i nomi (Fase 16).
- [ ] **16.6** L'esploratore: la camera libera e il cielo a tutto schermo (Fase 16).
- [ ] **16.7** L'esploratore: l'ora, la notte, le nuvole, le costellazioni (Fase 16).
- [ ] **16.8** L'esploratore: toccare un oggetto (Fase 16).
- [ ] **16.9** «Da vedere», e la notte di un evento (Fase 16).
- [ ] **16.10** «Punta il telefono», da provare sul telefono prima di tenerlo (Fase 16).
- [ ] **16.11** La chiusura: guida, VISION, README, CHANGELOG, la rilettura (Fase 16).
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
