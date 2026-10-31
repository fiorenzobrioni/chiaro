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
- **Chiusa il 29 ott 2026**: **Fase 15**, la schermata Avvisi dopo la revisione (dal 28 ott 2026): dodici step,
  uno per chat, in ordine di urgenza (quel che mente, poi il rumore, poi le migliorie, poi il valore
  nuovo); la revisione, le decisioni, le regole comuni e la descrizione di ogni step sono in
  `docs/planning/08-ottobre-27-28.md`, «Fase 15 — La schermata Avvisi: la revisione e i passi»;
  gli step, chiusi tutti e dodici, sono in `docs/planning/09-ottobre-28-29.md`. La rilettura
  dei dodici (qui sotto, «La Fase 15 riletta», 29 ott 2026) ha lasciato due step di coda, 15.13 e
  15.14, chiusi lo stesso giorno (qui sotto, «15.13» e «15.14»).
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
- [x] **15.13** La coda della Fase 15 nel codice: il job legge i luoghi legati anche quando il luogo attivo non c'è o non risponde; l'orfana si spegne anche senza il job; il canale del Cielo silenziato è detto a schermo («La Fase 15 riletta», 29 ott 2026). Fatto il 29 ott 2026 («15.13»).
- [x] **15.14** La coda della Fase 15 nei documenti: la riga «Alerts» del README, «four groups» nel CHANGELOG, due frasi della guida («La Fase 15 riletta», 29 ott 2026). Fatto il 29 ott 2026 («15.14»): la Fase 15 è chiusa del tutto.

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

## 15.13 — La coda nel codice: il job, l'orfana, il canale del Cielo (committente, 29 ott 2026)

**Fatto.** I tre punti della rilettura («La Fase 15 riletta», qui sopra) sono nel codice, con i
loro test; resta il 15.14, i documenti.

- **(a) Il job non si ferma più al luogo attivo.** In `WeatherSyncWorker.doWork` il luogo attivo
  è ora `City?`: nullo con la posizione accesa senza fix salvato (appena accesa, servizio spento,
  permesso tolto) e con nessun luogo scelto. Il suo tratto (fetch, bollettino, avvisi pronti,
  regole senza luogo e legate a lui) gira solo se il luogo c'è e la previsione arriva; i luoghi
  legati (`RulePlaces.elsewhere`, che accetta `active` nullo: le regole senza luogo non hanno
  niente da seguire e non si leggono, ognuna delle legate sul suo) e le città dei widget girano
  comunque. Un errore che passa (rete, servizio, 503, 429) sul luogo attivo chiude ancora la corsa
  con il riprovo, come prima e come dice lo step: il riprovo rifà tutto il giro, e leggere i luoghi
  legati prima sarebbe leggerli due volte. Un 4xx proprio della richiesta, o un'altra
  `WeatherException`, ripaga i widget per il segno di stantio e prosegue sugli altri luoghi. Il
  riarmo dei promemoria del Cielo resta legato al fetch riuscito del luogo attivo, il cui cielo i
  promemoria raccontano: senza luogo non si riarma, come prima. **Nessuna chiamata in più**: con
  la posizione senza fix il giro costa i soli luoghi legati e dei widget (`RecordedApi` nel test:
  Sydney 1, Reykjavík 1 per il widget, nessuna per la posizione).
- **(b) L'orfana si spegne anche senza il job.** `SyncScheduler.reconcile` chiama
  `RuleStore.switchOffOrphans` con gli id dei luoghi salvati prima di contare le regole accese: è
  il punto da cui passano già ogni modifica di Avvisi (`mutate`), i widget, le impostazioni e
  l'avvio. La rimozione di un luogo ci passa adesso: `PlacesViewModel.settleRemoval`, chiamata dal
  foglio quando la snackbar dell'«Annulla» si chiude senza l'azione, lancia la riconciliazione
  (`reconcileSync`, iniettata dalla Factory: `SyncScheduler.reconcile(app)`); alla rimozione in sé
  no, perché un luogo rimesso dall'«Annulla» non deve mai aver perso le sue regole. Il foglio chiuso
  prima che la snackbar finisca lascia l'orfana alla prossima riconciliazione o al job, cioè il
  primo gesto o il primo avvio, com'era la promessa dello step.
- **(c) Il Cielo dice il canale silenziato.** `SkyContent` legge `rememberMutedChannels` per il solo
  `sky_reminders` e disegna la seconda voce della `NotificationsOffCard` quando il permesso c'è, una
  campanella è armata (`remindersArmed`) e il canale è muto: le `notifications_muted_*` al
  singolare con il nome del canale (`sky_channel_name`, quello che il sistema mostra), il tasto apre
  la sua pagina (`NotificationPermission.openChannelSettings`). Mai con le notifiche spente del
  tutto (parla la prima voce) né su un canale mai creato.

**Chiusura.** `WeatherSyncWorkerTest`: posizione accesa senza fix (la regola legata a Sydney parla,
la città del widget è fetchata, la regola senza luogo tace), 400 sul luogo attivo (i luoghi legati e
dei widget girano, i widget ripagano), 503 sul luogo attivo (riprovo, Sydney non letta), e l'orfana
spenta da `reconcile` senza job; `RulePlacesTest` per il luogo attivo assente; `SkyMutedTest` in
`testDebug` (muto nominato e il tasto apre la sua pagina; a importanza normale nessuna card; con le
notifiche spente la prima voce). DESIGN §8.14 e VISION §5.8 con il Cielo; `guide_sky_bell_body` IT
ed EN; CHANGELOG `[Unreleased]`: due Fixed per (a) e (b), l'Added del canale silenziato esteso al
Cielo. Nessuno screenshot: gli stati non sono nella registrazione.

**Deviazioni.**

- *Il test dell'orfana spenta da `reconcile` sta in `WeatherSyncWorkerTest`, non in
  `SyncSchedulerTest`* come scriveva lo step: `SyncSchedulerTest` è la tabella pura della decisione
  (nessun Robolectric, nessuno store), e `reconcile` ha bisogno del grafo installato — store, città,
  WorkManager di prova — che il test del worker ha già e usa per le altre riconciliazioni.
- *Il riprovo su un errore che passa del luogo attivo chiude ancora la corsa prima dei luoghi
  legati*: lo step lo dice («rifà tutto il giro»), e il test lo fissa perché un futuro «leggiamo
  intanto gli altri» raddoppierebbe le chiamate a ogni backoff.

## 15.14 — La coda nei documenti (committente, 29 ott 2026)

**Fatto.** I ritocchi ai documenti della rilettura; con questo la coda della Fase 15 è chiusa e
la fase con lei.

- **README, la riga «Alerts» di Features** dice anche cosa si guarda (adesso, le prossime ore, oggi
  o domani, millimetri compresi), dove vale un avviso (il luogo aperto, o un luogo salvato
  qualunque luogo si guardi) e se è vero adesso; senza trattini lunghi, la regola del file.
- **CHANGELOG, la voce Changed del 15.7**: «a sheet in five groups, now, the next 6 hours, the
  next 12 hours, today and tomorrow», e via «of thirty-one», che era il conto prima del 15.9 e
  del 15.10.
- **La guida**: in IT «il luogo che hai aperto, che lo segue» al posto di «che cambia con lui»
  (la card dice «segue»); `guide_alerts_preview_body` in IT e in EN dice che un avviso su oggi,
  vero prima delle sei del mattino, dice da che ora può suonare, come quello su domani lo dice da
  mezzogiorno (`rule_now_from`, «può suonare dalle»).

**Chiusura.** `guide.png` inquadra la testa della guida (le tre schermate e l'inizio di Oggi), non
il capitolo di Avvisi: guardato, non rigenerato. Nessun test, nessuna chiamata; le risorse
compilate per sicurezza sugli apostrofi.

## Luoghi: il tocco su «La mia posizione» come lo swipe (committente, 30 ott 2026)

**Da dove nasce.** La domanda del committente: con Oggi su un luogo salvato, alla posizione si
arriva in due modi. Lo swipe fino alla sua pagina va benissimo; il tocco sulla riga «La mia
posizione» del foglio Luoghi invece «fa ripartire la ricerca della posizione, che può durare vari
secondi». Si può scegliere la posizione lì come con lo swipe, usando quella che c'è già? Letto il
codice, la selezione era già istantanea (`selectGps` scriveva la scelta prima del fix, e Oggi
cambiava pagina dietro il foglio), ma il foglio restava aperto su un fix nuovo chiesto con
`maxAge = Now`, fino a 15 secondi, anche quando l'ultimo aveva un minuto. Lo swipe rifà il fix
solo oltre i 5 minuti, accetta una posizione che il telefono ha già da meno di 5 minuti, in
silenzio e senza far aspettare nessuno. Il commento su `setActive` diceva che lo swipe rifà il fix
«exactly as» il tocco; dal 4 set (archivio 01, «Verifica su device (committente, 4 set 2026)») le
due strade avevano ciascuna la sua soglia. Le app meteo di fascia alta (Meteo di Apple, Google
Meteo, Samsung, AccuWeather, Carrot) trattano la posizione nella lista come un luogo qualunque:
si apre subito e la posizione si aggiorna dietro. Proposto di cambiare il tocco della riga e non
di aggiungere una seconda voce (due righe per un posto, diverse in una cosa che non si vede), e
senza un'icona «localizza ora», perché il pull fa già quel lavoro: il committente ha detto sì,
senza l'icona.

**Cosa è cambiato.**

- **`GpsRefix`** (`:core:data`) è la strada silenziosa alla posizione, una sola per il pager che
  si assesta sulla pagina, per il ritorno in primo piano e ora per il tocco sulla riga. Le regole
  sono quelle che stavano in `TodayViewModel`, spostate e non cambiate: un fix solo se l'ultimo ha
  5 minuti o più (`LocationProvider.SilentMaxAge`), con quei 5 minuti come `maxAge` e il
  `SilentTimeout` di 8 secondi; con nessun fix dovuto, il meteo di un arrivo in attesa si chiede
  di nuovo (`GpsArrival.retry`); un fallimento è muto. Un `Mutex` fa aspettare il secondo
  chiamante dello stesso gesto (il tocco seleziona, poi il pager si assesta: due chiamate), che
  trova il fix fresco e non chiede niente; prima, quando il fix spostava il lettore, i due
  prendevano lo stesso fix e scaricavano due volte il meteo del posto nuovo. Per questo l'app ne
  ha uno solo (`ServiceLocator.gpsRefix`), passato dalle Factory ai due ViewModel. In più di
  prima: con la posizione spenta mentre un giro aspettava il lock, non si chiede niente.
- **`PlacesViewModel.selectGps`** seleziona, chiude il foglio subito e lascia il fix a
  `GpsRefix`. Due casi tengono il foglio aperto, quelli per cui la Fase 3b lo teneva aperto e la
  cui ragione vale ancora: il permesso tolto o la localizzazione del telefono spenta, letti senza
  fix da `LocationProvider.obstacle()` (nuovo: gli stessi controlli con cui comincia
  `currentFix`, in un `manager()` condiviso, così non possono dire due cose diverse), con la
  frase di sempre sotto la riga; e nessun posto ancora trovato, dove il tocco cerca ad alta voce
  come prima (`locate`: la barra, `Now`, il foglio che si chiude alla risposta). La selezione
  avviene in tutti i casi: posto vecchio, meteo vero.
- **`TodayViewModel`**: `setActive` e `resumed` chiamano `GpsRefix.run()`; il pull (`relocate`)
  è l'unica strada esplicita rimasta sulla pagina, `Now` con la rotella e l'errore detto. Via
  `fixIsDue`, `GpsRefixInterval` e il parametro `explicit`.
- Restano richieste ad alta voce, cioè `Now` e il motivo detto quando fallisce: l'interruttore
  che si accende e il pull sulla pagina della posizione.

**Decisioni e deviazioni.**

- *Il tocco e lo swipe sono lo stesso codice, non due copie della stessa regola*: la promessa del
  commento su `setActive` si era rotta proprio perché ciascuna strada aveva la sua.
- *Cambia una decisione della Fase 3b* (archivio 01, «Verifica su device (committente, 4 set
  2026)», `selectGps` con `GpsState.Acquiring`): il foglio non aspetta più il fix per chiudersi.
  La ragione di allora era doppia, «chi tocca la mia posizione intende dove è adesso» e «la riga è
  l'unico posto che dice perché fallisce». La prima la tiene la regola dell'arrivo del 20 ott (la
  pagina si sposta solo con il meteo del posto nuovo in mano), senza far aspettare nessuno; la
  seconda la tiene `obstacle()` per i due motivi che si leggono gratis, e il pull per gli altri
  (nessun segnale, tempo scaduto).
- *Il paragrafo in testa a `LocationProvider.kt` va via*: diceva il file identico byte per byte
  alla sua copia nell'app da cui Chiaro è nata, da tenere in passo; non lo è più dal 23 set 2026
  (CLAUDE.md), e questo cambio lo avrebbe reso falso una volta di più.

**Come è stato verificato.** `GpsRefixTest`, nuovo (7: un fix giovane non chiede niente; uno
vecchio si riprende con `SilentMaxAge` e `SilentTimeout`; un fallimento è muto; uno spostamento
arriva con il meteo in mano; un arrivo in attesa si richiede senza fix; con la posizione spenta
niente; i due chiamanti di un gesto prendono un fix e scaricano una volta, e senza il lock il caso
è rosso, provato); `PlacesViewModelTest`, nuovo, il primo test di quel ViewModel (5: con un posto
trovato il foglio si chiude subito e non chiede la posizione; con il fix dovuto si chiude prima e
il fix arriva dietro con `SilentMaxAge`; il permesso tolto e la localizzazione spenta tengono il
foglio con il motivo, e la scelta avviene; senza un posto il tocco cerca con `Now` e chiude alla
risposta); `CachedLocationProviderTest` e `GpsArrivalTest` com'erano; suite completa e lint.
CHANGELOG `[Unreleased]`, una voce Changed; VISION §5.6, un paragrafo. Nessuno screenshot: il
foglio non cambia aspetto. Nessuna chiamata in più: il fix silenzioso costa quel che costava lo
swipe, e il gesto che poteva scaricare due volte il meteo del posto nuovo lo scarica una.
Provato sul telefono dal committente lo stesso giorno: funziona.

## Impostazioni: la revisione (committente, 30 ott 2026)

**Da dove nasce.** Il committente ha chiesto una review della schermata Impostazioni, in vista
della release: errori, impostazioni da aggiungere o togliere o spostare, la dicitura del credito
Meteocons (e se la licenza copre le icone modificate), layout e grafica, la coerenza con Passo e
Tempo (repo allegati come esempio). La review ha reso la pagina intera con un test Robolectric
usa e getta (384 × 3 400 dp, chiaro e scuro, IT ed EN) e l'ha confrontata con le Impostazioni
delle due sorelle; poi ha chiesto di implementare i punti nell'ordine di priorità indicato, in
uno step solo.

**Quel che la pagina diceva male, e ora dice.**

- *La nota privacy taceva GitHub.* Il bollettino delle allerte ufficiali, acceso di default, si
  scarica da `raw.githubusercontent.com` (`DpcBulletinSource`): «Chiaro non ha un server suo… A
  Open-Meteo va solo il luogo» era vera e incompleta. Ora: «il bollettino delle allerte ufficiali
  arriva dal repository GitHub della Protezione Civile, senza alcun luogo».
- *La guida diceva «unità di temperatura e vento»*: la pagina ne ha tre. Il capitolo segue ora
  l'ordine della pagina e nomina i widget.
- *«Colori dallo sfondo» prometteva «invece che dalla palette»*: in Chiaro la palette dipinge
  comunque il cielo e le scale (per questo la riga Palette non si spegne, a differenza delle
  sorelle). La nota dice ora esattamente questo.
- *La riga Lingua usciva dall'app senza il chevron* che ogni altra riga che esce porta.
- *Il dialogo aperto non sopravviveva alla rotazione*: `remember` → `rememberSaveable`.
- *La licenza*: «GPL-3.0: libera di usare, studiare, modificare e condividere, senza garanzia»,
  la frase delle sorelle; il «senza garanzia» è quel che la GPL chiede di dire.
- *`licenses/README.md`* elencava come composti solo i `sun-one-cloud-*` e rimandava a «Settings →
  About»: ora elenca le tre composizioni e il gruppo giusto.

**Il credito Meteocons.** La MIT permette modifica, fusione e redistribuzione e chiede solo che il
copyright e la nota di permesso viaggino con le copie; non chiede di segnalare le modifiche
(Apache 2.0 sì, MIT no). Riancoraggio dei colori, riscalatura, maschere in clip, lettere della
bussola tolte e le dodici composizioni rientrano tutte. La nota viaggia nel repo
(`licenses/Meteocons-MIT.txt`) e nel sorgente GPL pubblico; nell'APK c'è la riga con il link,
la prassi comune. La dicitura diventa «Meteocons di Bas Milius, MIT, adattate per Chiaro»: non per
la licenza, per onestà, e per non attribuire all'illustratore disegni composti qui.

**Cosa cambia nella pagina.**

- *L'ordine delle sorelle*: prima quel che l'app fa (Unità, Aggiornamenti, Promemoria del cielo,
  Widget), poi l'Aspetto, poi Lingua, Privacy, Informazioni, Dati e riconoscimenti, Ripristina.
  L'Aspetto stava secondo, prima di ogni funzione.
- *Un gruppo «Widget»*, due righe informative come in Tempo: come se ne aggiunge uno (con i tre
  nomi, presi dalle etichette dei widget) e che ognuno tiene le sue impostazioni dalla porta del
  launcher. Niente pin: l'app non chiede `requestPinAppWidget`, e una riga che non fa niente
  sarebbe una bugia (DESIGN §1.1). VISION §5.7 li elencava qui, la guida ha il capitolo, la pagina
  non li nominava.
- *La card della guida* usa `titleSmall` e icona 24 dp come la card privacy e come le sorelle
  (era `titleMedium` e 28).
- *La top bar si tinge allo scorrimento* (`pinnedScrollBehavior`), come nelle sorelle.
- *Il credito dei caratteri* si unisce con « · » come nelle sorelle, non con il trattino lungo.
- *`SettingsScreenTest`* (testDebug), il primo della pagina: il selettore della palette apre e
  restituisce la scelta; il gruppo Widget nomina i tre widget; l'ordine dei gruppi su una
  finestra alta 4 000 dp; il ripristino chiede prima e ripristina una volta; il credito adattato
  e la riga Lingua esistono.

**Due chiavi morte tolte dallo store** (beta: nessuna compatibilità da tenere).

- `skyEnabled` (`sky_enabled`): nessuna schermata la cambiava, i tre lettori (`SkyAlarmScheduler`,
  `SkyAlarmReceiver`, `WeatherSyncWorker`) la trovavano sempre vera. *Cambia una decisione
  archiviata* (archivio 01, «Fase 5 — Cielo», «`skyEnabled` resta plumbing senza interruttore»):
  allora si teneva «per il giorno in cui una superficie la reclamasse»; quel giorno non è venuto in
  sei settimane e il Cielo è metà del prodotto, non un modulo da spegnere. Via con i due test che
  la provavano.
- `widgetOpacityPct` con `WidgetOpacities` e `DefaultWidgetOpacityPct` (`widget_bg_opacity_pct`):
  l'opacità è per widget da `WidgetLookStore` e questa non la leggeva più nessuno.
- I due valori restano nel file di chi li ha scritti, non letti: lo dice il commento accanto alle
  chiavi, come per `notif_user_rules`.

**Non fatto, di proposito.** I gruppi restano segmenti M3 Expressive (`surfaceContainer`, angoli
20/4 dp) dove le sorelle hanno un fondo unico a 24 dp con righe sottili: Chiaro l'ha scelto il
14 ott 2026 per allinearsi a Cielo e Avvisi, e la coerenza dentro l'app vale più di quella fra
app; se le sorelle adotteranno i segmenti è una decisione loro. Le notifiche restano su Avvisi
(VISION). L'a-capo «CC BY / 4.0» del credito Protezione Civile a 384 dp resta: il nome intero è
l'attribuzione.

**Screenshot.** `settings.png` inquadra ora l'Aspetto (lo scroll porta il suo titolo sotto la
barra, `scrollUnderBar`), perché la cima della pagina è la card della guida e le unità e la
didascalia del README dice «Appearance, with a live preview»; alt text aggiornato. Solo quello
rigenerato, con `--tests "*ReadmeScreenshots.settings"`, e guardato.

**Come è stato verificato.** Suite completa e lint, tutto verde: `:core:domain` 416,
`:core:data` 650, `:core:sync` 68, `:app` 1 706 su due varianti (15 saltati), lint 0 errori. La
pagina rivista resa per intero un'altra volta (IT chiaro, EN scuro, 384 × 3 600 dp) e guardata:
il gruppo Widget tiene i tre nomi su due righe, la nota privacy in italiano sta in nove righe
nella card, il credito adattato va a capo su «Chiaro». Nessuna chiamata in più al provider.
Documenti: CHANGELOG `[Unreleased]` una voce Changed; DESIGN §8.15 un punto sull'ordine e due
righe su privacy e credito; `licenses/README.md`; README alt text. Provato sul telefono dal
committente lo stesso giorno, con l'APK della CI: va tutto bene.

## La sonda guarda anche il bollettino della Protezione Civile (committente, 30 ott 2026)

**Da dove nasce.** Dalla domanda sulla fonte delle allerte, chiusa nella review delle Impostazioni:
il repository GitHub `pcm-dpc` è il canale di pubblicazione del Dipartimento stesso (CC BY 4.0,
commit «Published by DPC Github Pipeline»), il sito delle mappe non ha un endpoint «ultimo
bollettino» (misurato il 9 set 2026, archivio 02), quindi la fonte resta quella. Il rischio che
resta è un canale che si spegne o cambia forma: l'app lo regge in modo onesto (niente trovato,
niente tenuto, la corsa dopo richiede), ma quel silenzio è identico a «nessuna allerta oggi» e un
lettore lo scoprirebbe il giorno di un'allerta vera non arrivata. Il committente ha chiesto di
mettere il controllo nella sonda mensile, nello stesso workflow.

**Cosa fa.** `tools/probe_dpc.py`, importato da `probe_provider.py` e lanciabile da solo, rifà la
scoperta dell'app alla lettera, con lo stesso `User-Agent` nominato: l'Atom dei commit (il sito,
mai l'API REST e il suo tetto di 60 l'ora per IP), i primi tre commit, lo stamp sulla prima riga
del `.diff` letta e basta; poi `files/xml/<stamp>.zip`, il `Cap_<stamp>.xml` dentro, contati i
blocchi `info`, le aree e quelle con il geocodice «Zona di Allerta» (`DpcBulletinReader.ZONE_GEOCODE`);
e una HEAD su `files/all/latest_all.zip`, il ripiego. Quattro richieste e una HEAD al mese, da un
indirizzo che non è di nessun lettore. Il risultato entra nel JSON della sonda (campo `bulletin`)
e nella sua tabella, con una riga sola.

**La sentinella scatta** (e la sonda è rossa, con il commit del JSON come sempre) se: non c'è uno
stamp; l'ultimo bollettino ha più di **48 ore** (quotidiano entro le 16, la sonda gira alle 5:23
del giorno dopo, un giorno saltato dalla pipeline non è ancora un canale spento); lo zip non
risponde 200; il CAP non si legge, non ha blocchi `info` o nessuna area porta la zona; il ripiego
non c'è. La regola è assoluta, non un confronto con la sonda precedente, quindi vale anche con
`--no-sentinel`. Un guasto di rete del runner è un `error` e scatta anch'esso: un falso allarme al
mese si rilancia a mano, un canale spento non visto costa di più.

**Quel che non vede, detto.** Un guasto più breve del mese fra due sonde (al dubbio, la sonda si
lancia a mano da `workflow_dispatch`); un cambio di contenuto che il lettore accetta ma interpreta
male, un livello nuovo o una zona rinominata: quello è un confronto fra il CAP e
`warning_zones_it.json`, un'altra misura, non fatta.

**Verificato dal vivo** (30 ott 2026, mattina): bollettino `20261029_1426`, 22 ore, CAP di 3 KB
con 2 `info` e 12 aree, tutte con la zona (una giornata quieta: quello dell'8 set 2026 pesava
45 KB con 8 `info` e 209 aree); ripiego 200, 4,6 MB. La sonda intera provata su Milano sola, con
`--out` fuori dal repo: la riga del bollettino in coda alla tabella, il campo nel JSON, uscita 0.
Documenti: la testata di `probe_provider.py` e del workflow, CLAUDE.md (la nota sul provider).
Nessuna chiamata in più dall'app; nessuno screenshot.

## La nebbia di Cavenago: una verifica sul campo (committente, 31 ott 2026)

**Da dove nasce.** Il committente ha chiesto un controllo, solo osservazione, della mattina a
Cavenago di Brianza (45,583 N, 9,413 E, 176 m), guidando fra le 6 e le 7 verso Melzo e ritorno,
con una foto alle 7:42. Qui quel che si è visto, contro i modelli e contro quello che l'app ne fa.
Nessun codice cambiato.

**Il visto.** Fra le 6 e le 7: cielo sereno, nebbia leggera nei campi, qualche banco sulle strade
solo dove passano accanto ai campi. Alle 7:30 nebbiolina rasoterra, quasi finita. Alle 7:42 niente,
cielo sereno, le Prealpi visibili nella foschia (visibilità di decine di chilometri). I METAR
dicono lo stesso: Linate 2–3 km di foschia con banchi (BCFG, RVR fino a 350–500 m), Orio 5–6 km di
foschia, Malpensa 10 km con banchi e RVR 400–500 m su qualche pista, NSC ovunque, temperatura uguale
alla rugiada (6–8 °C). Linate, vicino all'Idroscalo, non rappresenta Cavenago: la prima lettura che
ne prendeva i 2–3 km come veri anche qui era sbagliata, e la strada l'ha corretta.

**I modelli** (`best_match` è ICON-D2 qui, coincide in ogni ora):

| Ora | ICON-D2 | ECMWF IFS | UKMO, KNMI, DMI |
|---|---|---|---|
| 05–07 | 11–14 km, umidità 95–96%, codice 45 | 160–220 m, 99–100% | 10–50 km, 81–86% |
| 07:45 | 1 700 m, 98%, nube bassa 42% | — | — |
| 08:00 | 1 080 m, 98%, nube bassa 56% | 220 m, 99% | 11–12 km, 81–84% |
| 09:00 | 14 km, 93% | 9 km, 96% | 13–14 km |

ICON-D2 mette il picco della sua nebbia alle 08, un'ora dopo che sul posto era finita; IFS vede
nebbia fitta tutta la notte che non c'era; i tre restanti dicono «niente nebbia», giusto, con
un'aria troppo secca. Nessun modello rappresenta la nebbia rasoterra dei campi, alta pochi metri.

**Cosa ha fatto il motore.** Ha detto **«Sereno»**, quello che il committente ha visto sul telefono
ed era vero. La nebbia l'ha fermata la regola 5 al primo controllo (STATES §3): il codice 45 delle
05–08 contraddetto dalla visibilità del modello stesso, 11–14 km, scartato prima di arrivare al voto
dei testimoni. La nebbiolina dei campi non ha una parola, e non deve averla: nessun numero la vede.

**Quel che resta da guardare, non deciso.** Due cose viste stamattina, nessuna casella aperta finché
il committente non lo chiede:

- **Il margine alle 08 era di 80 m.** Con la visibilità del modello stesso basta un testimone
  (`FOG_WITNESSES_OWN` = 1), e l'unico disposto a confermare era IFS, quello che sbagliava tutta la
  notte; l'umidità propria (98%) c'era. Una corsa a 950 m avrebbe scritto «Nebbia» sotto un cielo
  sereno. Un caso, non una misura: la regola è stata scelta su 24 aeroporti (STATES §11).
- **La nebbia del modello torna come nube.** ICON-D2 porta il suo strato di nebbia anche nella nube
  bassa (42% alle 7:45, 56% alle 8); la regola 5 la scarta come nebbia, la regola 6 la legge come
  cielo: sulla carta «Quasi sereno» alle 7:45 e «Nuvoloso» alle 8 sotto un cielo vuoto. Sul telefono
  si leggeva «Sereno», probabilmente un report preso fra le 7 e le 7:15 (0–14% di nube) e ancora nei
  suoi 15 minuti: non verificato. La domanda, se un giorno si misura contro METAR e satellite: nube
  bassa con umidità ≥ 95% in un'ora la cui nebbia la regola 5 ha appena scartato è cielo o nebbia
  del modello?

**Un dettaglio per la sonda.** KNMI e DMI riportano esattamente 50 000 m nelle stesse ore (04, 07,
10, 11, 15, 17, 18) e 10–20 km nelle altre: un tetto o un riempimento di Open-Meteo, non una
previsione. Non cambia il voto sulla nebbia (50 km non vedono nebbia), ma è un valore da
riconoscere se un giorno la visibilità prestata la legge come numero.
