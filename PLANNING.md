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
- **Chiusa il 29 ott 2026, con una coda**: **Fase 15**, la schermata Avvisi dopo la revisione (dal 28 ott 2026): dodici step,
  uno per chat, in ordine di urgenza (quel che mente, poi il rumore, poi le migliorie, poi il valore
  nuovo); la revisione, le decisioni, le regole comuni e la descrizione di ogni step sono in
  `docs/planning/08-ottobre-27-28.md`, «Fase 15 — La schermata Avvisi: la revisione e i passi»;
  gli step, chiusi tutti e dodici, sono in `docs/planning/09-ottobre-28-29.md`. La rilettura
  dei dodici (qui sotto, «La Fase 15 riletta», 29 ott 2026) lascia due step di coda, 15.13 e
  15.14, uno per chat come gli altri.
- **Ancora aperta**: **Fase 14**, il motore contro i motori premium (dal 27 ott 2026): tredici step, uno per chat; i primi quattro (14.1–14.3 con la scadenza del 30 novembre, la visibilità ICON che sparisce; 14.4 la misura di ICON-2I, che ha detto no), il 14.6 (il caldo), il 14.7 (il vento), il 14.8 (la quota neve), il 14.9 (le unità), il 14.10 (la concordanza dei modelli, che ha detto no) e il 14.13 (le icone, prima dell'inverno) sono chiusi; 14.5 e 14.11 non esistono. La revisione, le decisioni, le regole comuni e la descrizione di ogni step sono in `docs/planning/07-ottobre-22-27.md`, «Fase 14 — Il motore contro i motori premium»; gli step chiusi dal 27 ott 2026 e l'attesa di 14.12 sono in `docs/planning/08-ottobre-27-28.md`; uno step chiuso da qui in poi scrive la sua sezione qui.

## Aperto

Quel che resta da fare o da decidere, in un posto solo: una casella si apre qui, con il rimando
alla sezione del registro da cui nasce, e si chiude qui, con quella che la chiude.

- [ ] **Il cielo d'inverno contro il satellite.** La regola 6 è misurata su ottobre soltanto:
  l'archivio del satellite di Open-Meteo parte da febbraio 2026. Rilanciare
  `tools/measure_against_metar.py` su dicembre–gennaio 2026-27 e rileggere STATES §11 (archivio 07,
  «Il motore dopo la revisione», 26 ott 2026).
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
- [x] **15.8** Le graffe del messaggio diventano parole (Fase 15). Fatto il 28 ott 2026 (archivio 09, «15.8»): una `VisualTransformation` dipinge la frase su un chip, il testo salvato tiene l'id, il tasto indietro toglie il chip intero; la resa su telefono, che restava da vedere al committente, è stata vista e tiene (29 ott 2026, «La Fase 15 riletta»).
- [x] **15.9** Le variabili «domani» e l'idea «Neve domani» (Fase 15). Fatto il 29 ott 2026 (archivio 09, «15.9»): sette `tomorrow.*` sulla riga datata domani, una voce per data da mezzogiorno, «Neve domani» dopo «Ghiaccio domattina», e la card che prima di mezzogiorno dice da quando può suonare.
- [x] **15.10** Le quantità nelle prossime ore: millimetri e centimetri (Fase 15). Fatto il 29 ott 2026 (archivio 09, «15.10»): le righe portano il totale e la neve dell'ora, quattro somme `next_Nh.*`, la pioggia senza l'acqua della neve, null se manca un'ora; una finestra sola per ogni `next_Nh.*`, l'ora in corso e le N−1 dopo, e il «Caldo intenso» conta sulla stessa.
- [x] **15.11** La regola legata a un luogo: il modello, il motore, il worker e il peso in chiamate (Fase 15). Fatto il 29 ott 2026 (archivio 09, «15.11»): `NotificationRule.place`, il job legge ogni luogo legato sulla sua previsione e nel suo fuso, 8,4 chiamate per luogo per periodo; la regola orfana si spegne e tiene il luogo; a schermo niente fino a 15.12.
- [x] **15.12** La regola legata a un luogo: l'editor, la card, la notifica (Fase 15). Fatto il 29 ott 2026 (archivio 09, «15.12»), con i punti che 15.11 gli lasciava: «Dove» nell'editor, la riga «A Bormio» con l'età di quella previsione, «Adesso sì/no» e «scattato l'ultima volta» dal luogo della regola, l'anteprima che fa il fetch, l'orfana il cui interruttore chiede dove, Oggi con le sole regole del luogo in pagina. La Fase 15 è chiusa.
- [ ] **15.13** La coda della Fase 15 nel codice: il job legge i luoghi legati anche quando il luogo attivo non c'è o non risponde; l'orfana si spegne anche senza il job; il canale del Cielo silenziato è detto a schermo («La Fase 15 riletta», 29 ott 2026).
- [ ] **15.14** La coda della Fase 15 nei documenti: la riga «Alerts» del README, «four groups» nel CHANGELOG, due frasi della guida («La Fase 15 riletta», 29 ott 2026).

Riletto il 29 ott 2026, dopo la rilettura della Fase 15: aperte 15.13 e 15.14, la coda; e come prima 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 29 ott 2026, all'archivio 09: le caselle della Fase 15 sono tutte chiuse (in `docs/planning/09-ottobre-28-29.md`), restano aperte 14.12, in attesa, e il cielo d'inverno contro il satellite, che aspetta dicembre; nessuna proposta non decisa. Riletto il 28 ott 2026, all'archivio 08, lo stesso giorno dell'apertura della Fase 15: le caselle sopra sono della Fase 14 (14.12 in attesa) e della Fase 15 (in `docs/planning/08-ottobre-27-28.md`), nessuna proposta non decisa. Riletto il 27 ott 2026, all'archivio 07: le caselle della Fase 14, nessuna proposta non decisa. Fino al 26 ott 2026 nessuna casella aperta (riletto il 22 ott 2026, all'archivio 06):
le ultime chiuse quel giorno stanno in `docs/planning/06-ottobre-20-22.md`, «Cielo: la finestra
reale nella card Stanotte», «Il widget «Momenti del cielo» va via», «Le notifiche: ogni riga nella
forma che sta in una riga», «Il widget «Le prossime ore» va via».

---

## Registro dal 29 ott 2026, dopo l'archivio 09

## La Fase 15 riletta: due step di coda (committente, 29 ott 2026)

**Da dove nasce.** Chiusi i dodici step, il committente chiede una rilettura delle
implementazioni della Fase 15 — errori, cose non complete, quel che manca e non era nella
fase — e una verifica della guida in app e del README. Letti per intero `RuleEngine`,
`RuleVariables`, `RulePlaces`, `NotificationRule`, `HoursAhead`, `WeatherNow`,
`WeatherSyncWorker`, `AlertsViewModel`, `AlertsScreen`, `RuleText`, `PlaceholderWords`,
`AlertChannels`, `ReportKeeper`, i diff di `:core:data` (`FiredRule`, `RuleStore`,
`RuleStateStore`, `WeatherRepository`, il mapper), di `SyncScheduler`, di `AlertEngine`, di
Oggi e delle notifiche, le stringhe della guida in IT e in EN, il README, il CHANGELOG
`[Unreleased]`, VISION §5.4 e DESIGN §8.9/§8.9b; suite verde a HEAD (2 803 test, 15 saltati:
gli screenshot).

**Il verdetto, in breve.** Il motore, il worker e la pagina fanno quel che il registro dice:
una finestra sola per «le prossime N ore», le somme che restano nulle su un'ora senza
quantità, l'impronta per data e il cancello di mezzogiorno, il riarmo per luogo, lo storico
per id con il ripiego per nome, i canali promessi, il nome unico. VISION, DESIGN, STATES e
CHANGELOG sono allineati. Restano due buchi nel codice, una cosa che manca e non era nella
fase, e tre ritocchi ai documenti.

**I problemi trovati.**

1. **Il job salta i luoghi legati quando il luogo attivo non c'è o non risponde.** In
   `WeatherSyncWorker.doWork` il ramo della posizione ritorna se non c'è un `gpsCity`
   (nessun fix ancora salvato: la posizione appena accesa, il servizio spento, il permesso
   tolto), prima di leggere le regole legate ai luoghi salvati e prima del fetch delle città
   dei widget; lo stesso con un 4xx non transitorio sul luogo attivo. Una regola «Neve a
   Bormio» resta muta per tutto il tempo in cui la posizione non arriva, e nessuna card lo
   dice. `WeatherSyncWorkerTest` non ha un caso con la posizione attiva. È l'unico punto in
   cui il 15.11 non mantiene la promessa.
2. **La regola orfana si spegne solo se il job gira.** `switchOffOrphans` vive nel worker;
   con le notifiche negate o spente e nessun widget il job è cancellato, e la regola resta
   accesa nello store. A schermo `canRing` copre il caso, ma lo scheduler la conta fra le
   accese e, rimesso il luogo, la regola torna a suonare da sola, dove il 15.11 dice «spente
   finché il lettore non le riaccende».
3. **Il canale dei promemoria del Cielo silenziato non è detto da nessuna parte.** Il 15.3
   lo lasciava fuori apposta; `mutedChannels` e la seconda voce della `NotificationsOffCard`
   sono scritte perché il Cielo le usi, e la pagina ha già la prima voce (`remindersArmed`).
   Il committente lo mette nella coda.
4. **I documenti.** La riga «Alerts» in Features del README non nomina niente della Fase 15
   (il luogo proprio, domani, i millimetri); la voce Changed del 15.7 nel CHANGELOG dice
   ancora «a sheet in four groups» mentre l'Added del 15.9 aggiunge «Tomorrow» (VISION dice
   «four, and a fifth»); nella guida in italiano «il luogo che hai aperto, che cambia con lui»
   suona storto, la card dice «segue»; e «può suonare dalle 6:00», che una regola su oggi dice
   prima delle sei, non è nella guida, mentre il mezzogiorno di domani sì. La guida è per il
   resto aggiornata in IT e in EN: le idee senza «Dodici ore asciutte», i valori come parole su
   un chip, i numeri di domani, i millimetri delle prossime ore, il «dove» con il luogo tolto,
   il costo di un luogo in più, il canale silenziato in forma generica. Gli screenshot e i
   testi alternativi del README sono quelli rigenerati e restano veri.

**Le decisioni prese oggi.**

- **La resa dei chip del 15.8 su telefono è vista e tiene** (committente): la casella del
  15.8 non ha più niente in sospeso.
- **Due step di coda, uno per chat come i dodici**: 15.13 per i punti 1, 2 e 3 (il 3 perché i
  pezzi ci sono già e lo step è piccolo), 15.14 per i documenti. Le regole comuni sono quelle
  della Fase 15 (archivio 08): test verdi, la sezione `##` qui in coda, la casella spuntata,
  DESIGN e VISION dove la superficie cambia, le stringhe IT ed EN, il CHANGELOG, gli
  screenshot se una vista cambia, un branch e una PR, il committente unisce.

### Gli step

**15.13 — La coda nel codice: il job, l'orfana, il canale del Cielo.** *Lavoro*: (a) in
`WeatherSyncWorker` il tratto del luogo attivo (fetch, bollettino, avvisi pronti, regole
senza luogo e legate a lui) non chiude più la corsa quando il luogo attivo manca o risponde
con un errore che aspetta: con la posizione senza fix e con un 4xx non transitorio la corsa
prosegue sui luoghi legati e sulle città dei widget, che sono di altri luoghi; un errore che
passa (rete, servizio, 503, 429) chiede il riprovo come oggi, perché rifà tutto il giro.
`RulePlaces.elsewhere` accetta un luogo attivo assente (le regole senza luogo non si leggono da
nessuna parte, quelle legate ognuna sul suo). (b) `switchOffOrphans` è chiamata anche fuori
dal job, in `SyncScheduler.reconcile` prima di contare le regole accese — è il punto che
ogni modifica di Avvisi, i widget, le impostazioni e l'avvio chiamano già — e `PlacesViewModel`
chiama `reconcile` alla rimozione di un luogo, dopo la finestra dell'«Annulla» (un luogo
rimesso dall'«Annulla» non ha mai perso le sue regole); così un'orfana è spenta entro il
primo gesto o il primo avvio, job o non job. (c) Il Cielo disegna la seconda voce della
`NotificationsOffCard` (15.3) quando il permesso c'è, un promemoria è armato
(`remindersArmed`) e `sky_reminders` è muto (`rememberMutedChannels`), con il nome che il
sistema mostra per il canale e il tasto che apre la sua pagina; le stringhe al plurale di
`notifications_muted_*` servono com'è, al singolare. *Chiusura*: `WeatherSyncWorkerTest`
con la posizione attiva senza fix (la regola legata a Sydney parla, la città del widget è
fetchata, le regole senza luogo tacciono) e con un 400 sul luogo attivo; `RulePlacesTest`
per il luogo attivo assente; `SyncSchedulerTest` per l'orfana spenta da `reconcile`;
`SkyMutedTest` in `testDebug` come `AlertsMutedTest` (muto nominato e il tasto apre la sua
pagina; a importanza normale nessuna card; con le notifiche spente parla la prima voce);
DESIGN §8.14 (il Cielo) e la riga della guida sulla campanella (`guide_sky_bell_body`, IT ed
EN: se il telefono silenzia i promemoria, il Cielo lo dice); CHANGELOG `[Unreleased]`, Fixed
per (a) e (b), la voce Added del canale silenziato estesa al Cielo per (c). Nessuna chiamata
in più: con la posizione senza fix il giro costa i soli luoghi legati e dei widget.
Nessuno screenshot (gli stati non sono nella registrazione). *Dimensione*: una chat.

**15.14 — La coda nei documenti.** *Lavoro*: nel README la riga «Alerts» di Features dice
anche dove un avviso vale (un luogo salvato, qualunque luogo tu guardi), domani e quanto
cade (senza trattini lunghi, la regola del file); nel CHANGELOG la voce Changed del 15.7 dice
cinque gruppi, con domani; nella guida IT «che cambia con lui» diventa «che lo segue», e
`guide_alerts_preview_body` in IT e in EN dice che un avviso su oggi, vero prima delle sei,
dice da che ora può suonare, come quello su domani da mezzogiorno. *Chiusura*: le stringhe
in IT e in EN; `guide.png` inquadra la testa della guida, non il capitolo di Avvisi:
verificare, e rigenerare solo se il capitolo è in cornice. Nessun test, nessuna chiamata.
*Dimensione*: una chat, dopo 15.13 così il CHANGELOG si tocca una volta.
