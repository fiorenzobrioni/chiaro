# PLANNING, archivio 09: 28-29 ottobre 2026: la Fase 15 chiusa

> Congelato il 29 ott 2026: un periodo chiuso del registro, spostato qui da `PLANNING.md`
> senza cambiarne una riga. Non si modifica. Il piano vivo è [`PLANNING.md`](../../PLANNING.md);
> l'indice di tutti i titoli è [`README.md`](README.md). Qui stanno i dodici step della Fase 15,
> la schermata Avvisi, chiusi fra il 28 e il 29 ott 2026 (la revisione, le decisioni, le regole
> comuni e la descrizione degli step sono nell'archivio 08, «Fase 15 — La schermata Avvisi: la
> revisione e i passi»), e due interventi a margine sul Cielo, «Il Cielo rinfresca da sé» e «Il
> Cielo senza luogo è lo stato vuoto delle altre due schede».

---

## 15.1 — Il «vero adesso» legge il report di Oggi (committente, 28 ott 2026)

**Fatto.** La card di ogni regola e l'anteprima dell'editor leggono il report **preparato come
Oggi lo prepara**, e la pagina lo tiene fresco da sé. Tre pezzi.

- **`WeatherNow.prepare`** (`:core:domain`, nuovo): `CurrentEstimate.apply` poi
  `WeatherRecency.trim`, null quando del report non resta niente sul presente. È la riga che
  `TodayUiState.build` faceva in casa dal 24 set 2026, tirata fuori perché due lettori ne facciano
  una sola; Oggi (e i widget, che costruiscono da lì) passano da qui, e il controllo `coversNow` separato di Oggi se
  ne va (la stessa domanda: resta qualcosa dopo il taglio).
- **`today.*` legge la riga datata oggi** (`RuleVariables.today`, `now.toLocalDate()`), null se
  non c'è, come `AlertEngine.findDailySummary` fa dal 23 set 2026. Con una cache di ieri la prima
  riga è ieri, e «la massima di oggi» era quella di ieri: sul report preparato le due letture
  coincidono (il taglio toglie ieri), sul report grezzo del worker pure (un fetch appena fatto
  comincia da oggi), e la lettura per data chiude l'unico caso in cui divergevano.
- **La pagina rinfresca in silenzio** (`AlertsViewModel`): un flusso per luogo attivo, vivo solo
  finché la pagina è guardata, che legge la cache prima di tutto (niente spinner, mai), poi chiede
  al repository per la strada di Oggi — `getWeather` senza forza, il cui TTL è
  `WeatherFreshness.ProviderResolution`: sotto i 15 minuti è un hit e non costa niente — all'apertura
  e a ogni minuto oltre il quarto d'ora, con il `RefreshBackoff` di Oggi dopo un errore, e mai
  sotto il risparmio batteria a meno che non ci sia nessun report (le eccezioni di Oggi: una
  richiesta che nessuno ha fatto ad alta voce si rinvia, una pagina vuota non è una pagina più
  economica). Il tick del minuto, che stava in `state`, vive lì. Il report tenuto è grezzo e porta
  la chiave del luogo, così un cambio di luogo non legge mai il report di quello di prima; le card
  lo leggono preparato al minuto della lettura. L'errore di rete è muto: la pagina tiene quel che
  ha e ne dice l'età.
- **L'età a schermo**: `Content` porta `lastSync` e `isStale` (la soglia di `WeatherFreshness`,
  la stessa del chip); oltre la soglia il titolo «I tuoi» porta a fine riga le parole del chip
  («Aggiornato 3 ore fa», `freshnessAge` di Oggi, `bodySmall` in `onSurfaceVariant` sulla base del
  titolo, come il `hint` di `SectionTitle`), e solo quando ci sono card: un'età sopra un gruppo
  vuoto non qualifica niente. Nessuna stringa nuova: i plurali del chip ci sono già in IT e EN.
- **L'anteprima dell'editor** si ricalcola anche al minuto e a ogni report che arriva:
  il `LaunchedEffect` prende `lastSync` fra le chiavi e cicla ogni minuto; i 300 ms di attesa dopo
  un tasto restano.

**Il peso in chiamate.** Nessuna richiesta nuova e nessuna variabile nuova: è la cache di Oggi,
lo stesso fetch da 8,4 chiamate. Avvisi tenuta aperta pesa come Oggi tenuta aperta, un fetch ogni
quarto d'ora al più; chi arriva da Oggi la trova fresca e non spende niente. Prima pesava zero e
mentiva.

**Decisioni e deviazioni.**

- *La verifica «a mano» che la card e la riga dell'agenda dicano la stessa cosa è un test*
  (`RuleReadingTest`, Robolectric): una cache di Milano presa ieri alle 10 letta oggi alle 10:30,
  con il blocco `current` a 25° e le righe di oggi a 12°, la massima di ieri 30 e quella di oggi
  20. Una regola «massima di oggi ≥ 25» dice «Adesso no · è 20°» sulla card e non ha riga
  nell'agenda; «temperatura adesso ≥ 20» dice no con 12° (stimato) e l'agenda non la porta; le
  soglie che oggi supera dicono sì su entrambe. Prima la card diceva sì su ieri e sul mattino di
  ieri.
- *Il report campione non si tocca*: i suoi giorni partono tre giorni dopo la sua data, e lo
  usano decine di test. I tre che leggono `today.*` (`RuleVariablesTest`, `RuleEngineTest`,
  `RuleTextTest`) ridatano i giorni perché il primo sia quello di `now`; `RuleEngineTest` non
  cambia nella sostanza, come lo step chiede.
- *Gli screenshot del README non cambiano*: le immagini di Avvisi sono disegnate cinque minuti
  dopo il fetch che è la registrazione stessa, nella stessa ora, quindi la preparazione è
  l'identità (niente stima sotto i 60 minuti, niente da tagliare) e il report non è stantio;
  `MilanRecording` passa comunque dalla stessa preparazione, così la foto legge come la pagina.
  Resi di nuovo e confrontati con i file committati: identici byte per byte, git non li vede cambiati.
- *Il Cielo non rinfresca ancora*: legge la cache come Avvisi faceva. Il suo verdetto dice l'età
  del dato da sé (`staleAfter`), e lo step riguarda Avvisi; se un giorno servirà, il flusso del
  custode è scritto perché si possa spostare.

**Come è stato verificato.** `WeatherNowTest` (nuovo: una cache di ieri porta oggi in testa e il
`current` stimato; un report dell'ora è restituito com'è; un report oltre l'orizzonte è null),
`RuleVariablesTest` (la riga datata oggi, mai la prima; la cache di ieri; nessun oggi),
`RuleReadingTest` (nuovo, sopra), `RuleEngineTest`, `RuleTextTest`, `RuleNowTest`,
`TodayStateBuilderTest`; suite completa verde (`./gradlew test :app:testDebugUnitTest`,
2 627 test, 15 saltati: gli screenshot, che girano solo su richiesta); lint verde (`:app:lintDebug`). DESIGN §8.9 («Is it true now», la riga e l'editor),
VISION §5.4, CHANGELOG `[Unreleased]`.

## 15.2 — Lo schermo non promette un suono che non può arrivare (committente, 28 ott 2026)

**Fatto.** Quattro pezzi, tutti nella pagina Avvisi; il motore non è toccato.

- **La finestra del bollettino è del luogo, non dello switch.** `alertWindows` la disegna solo
  quando `content.warnings` non è `Unavailable` (`bulletinPossible`: una zona in attesa del primo
  bollettino, o con quello di ieri, può ancora suonare; un luogo fuori da ogni zona mai, qualunque
  cosa dica lo switch), e l'ora «15–17» sopra lo switch segue la stessa regola. Lo switch resta
  dov'è: è un'impostazione, e la card sopra dice già se il luogo ha una zona. Prima, per Lione, la
  card diceva «disponibili per i luoghi in Italia» e sotto «Il prossimo, oggi · Il bollettino ·
  15–17».
- **Il bollettino già uscito oggi non è il prossimo a suonare.** `nextWindow` prende un insieme
  di finestre `spent`: una finestra consumata è saltata, aperta o no, e torna domani con le altre.
  È consumata la finestra del bollettino quando `warnings` è `Current` e `issuedAt` è datato oggi
  (`AlertWindow.spentOn`, con l'oggi del luogo, che per un luogo in Italia è quello dell'emittente).
  Le finestre dei riepiloghi non si consumano mai: lo schermo non sa se il riepilogo è arrivato, e
  la regola del 14 ott resta (una finestra, non una promessa). Alle 16 con il bollettino delle
  14:34 la riga dice «Il prossimo, oggi · Il riepilogo della sera», dove diceva «Adesso · Il
  bollettino · 15–17».
- **Nessun luogo è lo stato vuoto.** `AlertsUiState.NoPlace`, emesso dal view model quando
  l'attivo è `ActiveSource.None`; la pagina disegna il `NoPlaceState` di Oggi (lo stesso
  composable, che prende ora la frase della pagina: «Aggiungi una città: gli avvisi e le allerte
  parlano di un luogo»), con l'azione che apre i luoghi, e niente altro. Prima disegnava la pagina
  intera con «disponibili per i luoghi in Italia» in testa.
- **Le parole dello switch.** «Di solito in quella fascia» rimandava a una colonna che TalkBack non
  legge insieme alla riga: ora «Di solito tra le 15 e le 17, mai più di una per bollettino» («Usually
  between 15:00 and 17:00» in EN), con i numeri che la riga già riceve; le due costanti
  `BulletinFrom`/`BulletinTo` sono le stesse della striscia, dell'ora sopra lo switch e della frase.

**Il peso in chiamate.** Nessuna richiesta nuova, nessuna variabile nuova: 8,4 chiamate il fetch,
come prima.

**Decisioni e deviazioni.**

- *Lo stato «nessun luogo» è uno stato del view model, non una `Content` con il nome vuoto*: la
  pagina non ha niente da dire senza un luogo (nessuna zona per cui rispondere, nessuna previsione
  su cui leggere una regola), e un `Content` senza luogo avrebbe lasciato alla pagina il compito di
  non disegnare pezzo per pezzo. L'editor non si apre perché non c'è `Content`; le regole restano
  salvate e tornano con il primo luogo.
- *Lo stato vuoto è il composable di Oggi, non quello del Cielo*: §8.11 lo nomina come lo stato
  vuoto dell'app; il Cielo ha il suo (allineato a sinistra, `titleMedium`) e non è di questo step.
- *La finestra consumata resta disegnata sulla striscia*: la striscia è la forma della giornata,
  non la lista di quel che deve ancora arrivare; è la riga sopra a dire il prossimo.
- *Lo screenshot `alerts.png` cambia*, contro la previsione dello step («a Milano non dovrebbe»):
  la riga dello switch è in foto, e la nuova frase è una riga più lunga. La card in testa non
  cambia (il bollettino della registrazione è di ieri, le 12:15 del 25 set: «Next, today · The
  bulletin · 15–17» resta). Rigenerato e guardato; `alerts-yours.png` non cambia.

**Come è stato verificato.** `NextWindowTest` (tre casi nuovi: la finestra consumata saltata prima
di aprirsi e mentre è aperta, da sola torna domani, non sposta quelle prima di lei),
`AlertsPromiseTest` (nuovo, `testDebug`, Robolectric: Lione senza «15–17» né «Il bollettino» e con lo
switch ancora in pagina; il bollettino di oggi alle 16 non dice «Adesso» e il prossimo è di oggi;
quello di ieri alle 16 dice «Adesso · Il bollettino»; nessun luogo è il titolo, la frase e
l'azione, senza i gruppi), `AlertsYoursEmptyTest`, `NotificationsPromisedTest`; suite completa
verde (`./gradlew test :app:testDebugUnitTest`, 2 637 test, 15 saltati: gli screenshot, che girano solo su richiesta); lint verde (`:app:lintDebug`). DESIGN
§8.9b (la finestra del luogo, il bollettino consumato, lo stato «nessun luogo»), VISION §5.4,
CHANGELOG `[Unreleased]`, `alerts.png`.

## 15.3 — Un canale silenziato è detto a schermo (committente, 28 ott 2026)

**Fatto.** La terza via per perdere una notifica, dopo il permesso negato e lo switch di sistema
spento: il permesso c'è, l'app può notificare, ma un canale è a importanza «nessuna» (dalle
impostazioni, o da una pressione lunga su una notifica). `allowed` resta vero, ogni `notify` su
quel canale torna falso e la card della regola diceva «Non è ancora mai scattato» a vita (la
revisione, punto 4: «non mi avvisa mai»). Quattro pezzi.

- **La lettura per canale** (`NotificationPermission.mutedChannels`): fra gli id chiesti, quelli
  il cui canale esiste con `IMPORTANCE_NONE`. Un canale è creato dal primo `notify`: uno mai
  creato non è muto, e la card non si disegna sopra un canale che il lettore non ha mai visto.
  Ricreare un canale ne abbassa l'importanza e mai la alza (regola di Android, e di Robolectric,
  letta nel bytecode della shadow 4.16): il `createNotificationChannel` dei notifier non
  smuta niente, e il test lo pinna. `openChannelSettings` apre la pagina del canale
  (`ACTION_CHANNEL_NOTIFICATION_SETTINGS` con package e id), e la pagina dell'app dove un
  dispositivo non la ha. `AlertNotifier` espone `channelId(kind)` e `channelNameRes(kind)`,
  che erano privati.
- **La mappa promessa → canali** (`AlertChannels.kt`, `promisedChannels`): è
  `notificationsPromised` detto canale per canale, nell'ordine della pagina. Lo switch del
  maltempo porta due canali, il temporale e il caldo (14.6: il caldo corre sullo stesso switch
  ma su un canale suo, il telefono può silenziarne uno e tenere l'altro, e la card deve poter
  nominare l'uno o l'altro); le allerte portano «arancioni e rosse» sempre e «gialle» solo
  finché lo switch parte dal giallo (dall'arancione in su l'app non manda mai una gialla, e un
  canale giallo muto non silenzia niente di promesso); il canale del lettore solo con una
  regola accesa. Il test chiude il cerchio: una promessa ha sempre un canale, un canale sempre
  una promessa.
- **La seconda voce della card** (`NotificationsOffCard` prende il `title`; DESIGN §8.14):
  disegnata quando il permesso c'è e un canale promesso è muto — «Un avviso è silenziato» /
  «Il telefono ha silenziato «I tuoi avvisi»: non può arrivare», con il nome **che il sistema
  mostra per il canale** (la parola che il lettore cercherà nella pagina delle impostazioni),
  tra virgolette («» in IT, “” in EN, una stringa per quotarlo), e il tasto sempre «Apri le
  impostazioni» (`settingsOnly = true`: nessun dialogo smuta un canale) che apre la pagina di
  quel canale. Più canali muti: titolo e corpo al plurale, la lista nella lingua del lettore
  (`android.icu.text.ListFormatter`: «Maltempo» e «I tuoi avvisi», “Severe weather” and “Your
  alerts”), e il tasto apre il primo. Riletta a ogni `ON_RESUME` come la prima voce
  (`rememberMutedChannels`), perché il rimedio sta fuori dall'app.
- **La riga del canale muto sfuma l'icona** come una riga spenta (`OffAlpha`, 0,4): `AlertRow`
  e `RuleCard` prendono `muted`. Una riga sfuma solo quando **tutti** i canali su cui corre sono
  muti (`rowMuted`, `warningsRowMuted`): con il solo caldo muto il temporale arriva ancora, la
  riga resta accesa ed è la card a nominare il canale che non arriverà.

**Il peso in chiamate.** Nessuna richiesta nuova, nessuna variabile nuova: 8,4 chiamate il fetch,
come prima. La lettura dei canali è una chiamata al `NotificationManager` all'apertura e al
ritorno in primo piano.

**Decisioni e deviazioni.**

- *La card muta non si disegna con le notifiche spente del tutto*: la prima voce dice già che
  niente può arrivare, e due card per un silenzio solo sono rumore. Lo step non lo diceva; il test
  lo pinna.
- *La riga sfuma solo con tutti i suoi canali muti*, non con uno: lo step dice «la riga del canale
  muto», e per il maltempo e le allerte i canali sono due. Sfumare la riga del maltempo con il
  solo caldo muto avrebbe detto spenta una riga che per il temporale suona ancora.
- *DESIGN: la seconda voce è scritta in §8.14*, la sezione della `NotificationsOffCard`, non in
  §8.13 come lo step dice: §8.13 è il banner delle allerte, e la card è §8.14 dal 21 set 2026.
- *Il canale del Cielo (`sky_reminders`) non è di questo step*: lo step elenca i canali di Avvisi,
  e la card del Cielo è del Cielo. `mutedChannels` e la seconda voce sono scritte perché il Cielo
  possa usarle quando servirà, con i suoi promemoria come promessa.
- *Nessuno screenshot cambia*, come lo step prevede: la registrazione non ha canali muti, e il
  parametro `title` della card ha per default il titolo di prima.

**Come è stato verificato.** `NotificationPermissionTest` (nuovo, Robolectric: il canale a
«nessuna» è muto; mai creato e a importanza normale non lo sono; più canali rispondono con i muti
nell'ordine chiesto; ricreato a `DEFAULT` resta muto; la strada è l'intent del canale con package e
id), `PromisedChannelsTest` (nuovo: tutto spento nessun canale; il maltempo porta temporale e caldo;
le gialle solo dal giallo; il canale del lettore solo con una regola accesa; promessa e canale si
implicano; il nome è quello del notifier; la riga sfuma solo con tutti i canali muti),
`AlertsMutedTest` (nuovo, `testDebug`, Robolectric: il canale muto è nominato e il tasto apre la sua
pagina; due muti sono una card che li elenca e apre il primo; a importanza normale nessuna card; il
canale muto di una regola spenta non è nominato; con le notifiche spente parla la prima voce);
`NotificationsPromisedTest`, `AlertsPromiseTest` invariati e verdi; suite completa verde
(`./gradlew test :app:testDebugUnitTest`, 2 670 test, 15 saltati: gli screenshot, che
girano solo su richiesta); lint verde (`:app:lintDebug`). DESIGN §8.14 (la seconda voce), VISION
§5.4, CHANGELOG `[Unreleased]`, stringhe IT ed EN.

## 15.4 — Il motore: il riarmo e il nome (committente, 28 ott 2026)

**Fatto.** I punti 5 e 6 della revisione, e la prima delle decisioni prese il 28 ott 2026 (il
nome unico, lo storico con l'id). Il motore non cambia: `RuleEngine` e `RuleEngineTest` sono
intatti, come lo step prevedeva. Tre pezzi.

- **Il riarmo alla riaccensione** (`AlertsViewModel.update`, `rearms` nel companion): la regola
  si pulisce (`RuleStateStore.clearRule`: il latch e le impronte) quando cambiano le condizioni o
  gli orari, come dal 14 ott 2026, **e quando `enabled` passa da falso a vero**. Il motivo, nel
  commento: il worker valuta solo le regole accese (`filter { it.enabled }`), una regola spenta non
  è mai letta falsa e il suo latch non si scarica; «Temperatura adesso ≥ 20» vera e scattata,
  spenta, scesa e risalita, riaccesa: muta fino al prossimo falso. Lo spegnimento da solo non
  pulisce niente: una regola ferma non ha niente da riarmare, e lo stato se ne va nel momento in
  cui conta, quando la regola torna a essere letta. La rinomina lascia lo stato com'è.
- **Il nome unico** (`RuleText.isTaken`, `RuleText.keptName`; l'editor in `AlertsScreen`): il campo
  del nome riceve i nomi delle altre regole e, mentre si scrive uno che è già di un'altra, passa in
  errore con la riga «Un altro avviso ha già questo nome.» sotto; alla chiusura dello sheet un nome
  preso, come uno vuoto, lascia alla regola il nome di prima, e lo stesso vale per la regola che
  «Annulla» rimette a posto dopo l'eliminazione. Il confronto ignora le maiuscole e gli spazi
  intorno: «bici» dopo «Bici» è la stessa parola su due card. `freeName` resta per le idee e usa lo
  stesso confronto: «Bici» con una «bici» già in lista diventa «Bici 2».
- **Lo storico porta l'id** (`FiredRule` in `:core:data`): `recordFiredRules` riceve le regole e
  scrive `id:nome` nel commit (`["1:Bici","2:Ombrello"]`); `firedRules` decodifica una riga in
  `FiredRule(id, nome)`, con l'id nullo per le righe scritte prima; la lettura della pagina
  (`lastFired`, per id di regola, al posto di `lastFiredByName`) prova l'id dove la riga lo ha e il
  nome dove non lo ha (`FiredRule.isFor`), così nessun passato si perde all'aggiornamento. Il
  worker passa le regole scattate, non più i nomi.

**Il peso in chiamate.** Nessuna richiesta nuova, nessuna variabile nuova: 8,4 chiamate il fetch,
come prima.

**Decisioni e deviazioni.**

- *Il confronto dei nomi ignora maiuscole e spazi*: lo step dice «un nome già di un'altra regola»;
  due card «Bici» e «bici» confondono quanto due «Bici», e il campo non deve lasciar passare per
  una maiuscola quel che rifiuta per una lettera. Vale anche per `freeName`, che prima era
  sensibile alle maiuscole.
- *La pulizia alla riaccensione toglie anche le impronte del giorno*, non solo il latch, perché
  `clearRule` è una sola, la stessa del cambio di soglia: una regola giornaliera spenta e riaccesa
  nello stesso giorno può parlare una seconda volta, come già faceva con una soglia nuova. Una
  pulizia del solo latch sarebbe una seconda funzione per un caso che il cambio di soglia già
  accetta.
- *L'ambiguità delle righe vecchie*: una riga scritta prima dello step che è un nome solo e
  comincia con cifre e due punti («12:00 corsa») si legge come `id:nome` e non si ritrova. Dura
  quanto la retention di `weather_history`, tre giorni, poi ogni riga è nella forma nuova; non
  vale una seconda forma. Scritto nel KDoc di `FiredRule`.
- *Il test del view model è vero* (`AlertsViewModelTest`, Robolectric): store veri su file
  temporanei, la cronologia in un Room in memoria, la rete uno stub che fallisce. Per questo `:app`
  guadagna `room-runtime` fra le dipendenze di test, come ha già DataStore per lo stesso motivo.
  Il `reconcile` dello scheduler dopo ogni modifica legge gli store dal `ServiceLocator`
  sovrascritto per il test e fallisce su WorkManager dentro il suo `runCatching`, come in
  produzione senza il worker.
- *Nessuno screenshot cambia*: l'editor è uno sheet, che le immagini del README non catturano, e
  la registrazione non ha storico di regole scattate.

**Come è stato verificato.** `FiredRuleTest` (nuovo, `:core:data`: `id:nome` scritto e riletto; un
nome con i due punti suoi sopravvive al giro; una riga nuova è della regola per id qualunque sia il
nome di adesso; una riga vecchia porta il nome solo ed è letta per nome), `HistoryRetentionTest`
(aggiornato: il commit scrive `["1:bici","2:ombrello"]` e li somma senza sovrascrivere),
`AlertsViewModelTest` (nuovo: spenta il latch resta, riaccesa se ne va; la rinomina lo lascia;
`rearms` dice sì a condizioni, orari e riaccensione e no allo spegnimento e alla rinomina; «scattato
l'ultima volta» per id sulla riga nuova anche dopo la rinomina e per nome su quella vecchia, nulla
per chi non è mai scattata; due regole non condividono un passato e vince la riga più recente),
`RuleTextTest` (tre test nuovi: il nome preso qualunque siano maiuscole e spazi; alla chiusura il
nome preso o vuoto è quello di prima, il proprio nome in un'altra maiuscola no; l'idea numerata
oltre una regola che ha il nome in un'altra maiuscola), `RunTemplatesTest` e `RuleEngineTest`
invariati e verdi; suite completa verde (`./gradlew test :app:testDebugUnitTest`, 2 694 test, 15
saltati: gli screenshot, che girano solo su richiesta); lint verde (`:app:lintDebug`). DESIGN §8.9
(il campo del nome) e §8.9b (il rimando di «Annulla»), VISION §5.4, CHANGELOG `[Unreleased]`,
stringhe IT ed EN.

## Il Cielo rinfresca da sé (committente, 28 ott 2026)

**Da dove nasce.** Chiudendo 15.1 avevo scritto che il Cielo «non rinfresca ancora»: legge la
cache e la rilegge al minuto, mai un fetch. Il committente ha chiesto se anche il Cielo andasse
toccato, e di farlo subito, bene. Letto `SkyViewModel`, `SkyStateBuilder.build`, le ore della
notte (`nightHours`), `SkyVerdictEngine.evaluate`, `DataNote` in `SkyScreen`.

**Quel che c'era.** Il Cielo non mentiva: oltre la soglia di stantio il verdetto risponde «non si
può dire · dati vecchi» e la sezione dice l'età una volta, con le parole del chip (22 ott 2026).
Ma non chiedeva mai dati freschi: chi apriva il Cielo senza passare da Oggi trovava l'ultimo
report del job, fino a due ore, e una notte di «?» che un fetch avrebbe fatto parlare — lo stesso
fetch che Oggi avrebbe fatto a quel minuto, nessuna chiamata nuova. Il blocco `current` non c'entra:
il Cielo legge solo le righe orarie, per finestra (il verdetto, il tratto sereno, gli strati della
card «Stanotte» sono delle ore della notte), quindi la stima di `WeatherNow` non lo tocca e il
taglio non cambierebbe una riga di quel che disegna.

**Fatto.**

- **`ReportKeeper`** (`ui/today`, nuovo): il custode scritto in `AlertsViewModel` per 15.1, tirato
  fuori com'era perché lo usino due pagine. Prende due funzioni (`cached`, `fetch`) e non il
  repository, così un test gli passa un registratore e un orologio; il costruttore secondario lega
  il repository. `keep(city)` emette la cache subito, poi ogni minuto, e chiede in silenzio oltre il
  quarto d'ora del provider con il `RefreshBackoff` di Oggi dopo un errore, mai sotto il risparmio
  batteria se un report c'è; `follow(cities)` segue il luogo attivo e risponde null senza luogo.
  Ogni emissione porta il luogo, il report grezzo e il minuto della consegna.
- **`SkyViewModel`** combina il custode, le sottoscrizioni e le impostazioni: il tick del minuto e
  `cachedReport` sono suoi. `PowerSaveState` entra dalla factory. Il report resta grezzo, per il
  motivo sopra.
- **`AlertsViewModel`** usa lo stesso custode: `page` è dei soli store, e tutto quel che viene dal
  report (fuso, alba e tramonto, `lastSync`, `isStale`, le letture) si calcola in `state` a ogni
  consegna; l'ultimo report consegnato resta in un campo per l'anteprima dell'editor.

**Decisioni.**

- *Oggi tiene il suo flusso*: il pull, gli errori a schermo, l'arrivo del GPS, il bollettino non
  sono del custode, che è la strada di Oggi per le pagine che non hanno queste cose. Non si
  duplica una riga: il custode riusa `RefreshBackoff` e la soglia del repository.
- *Nessun segno nuovo sul Cielo*: la pagina diceva già l'età una volta per sezione, e continua;
  quel che cambia è che l'età smette di crescere da sola.
- *Nessuna richiesta nuova*: la stessa cache di Oggi, lo stesso fetch da 8,4 chiamate, al più uno
  ogni quarto d'ora mentre la pagina è guardata.

**Come è stato verificato.** `ReportKeeperTest` (nuovo, `runTest` a tempo virtuale, l'orologio è
quello dello scheduler): la cache prima e il report fresco dopo quando ha più di un quarto d'ora;
un report giovane non si chiede fino al quarto d'ora, e poi una volta sola; sotto il risparmio
batteria mai con un report, una volta senza; un errore è muto e aspetta il backoff (1, 2, 4
minuti) con i tick che continuano; `follow` cambia luogo e risponde null senza luogo. Suite
completa verde (2 704 test, 15 saltati: gli screenshot, che girano solo su richiesta); lint verde. Gli screenshot del README non cambiano: le immagini del
Cielo e di Avvisi nascono da `SkyStateBuilder` e da `AlertsUiState` costruiti a mano, non dai view
model. VISION §5.3, CHANGELOG `[Unreleased]`.

## 15.5 — Le soglie nelle unità del lettore (committente, 28 ott 2026)

**Fatto.** Il punto 7 della revisione e la tacca dello slider fra le piccole migliorie. Lo
slider della soglia ragiona nell'unità che il lettore legge e la regola resta in °C, km/h e hPa:
cambiare unità non cambia nessun avviso. Tre pezzi, tutti in `RuleText` (`ui/alerts`) e nel
`ValuePickerDialog` dell'editor; il motore e `RuleVariables` non cambiano.

- **La griglia è dell'unità** (`RuleText.valueSpec(variableId, units)`): temperatura °F −22…113
  passo 1; vento mph 0–75 passo 5, m/s 0–35 passo 1, nodi 0–65 passo 5, Beaufort 0–12 passo 1;
  raffiche mph 0–95, m/s 0–40, nodi 0–80, Beaufort 0–12; pressione inHg 28,0–31,0 passo 0,1, mmHg
  710–790 passo 5. Le metriche sono quelle di prima (°C −30…45, km/h 0–120 e 0–150, hPa 950–1050),
  e chi non ha toccato le impostazioni non vede niente di diverso. Prima la griglia era canonica e
  solo l'etichetta convertiva: in Fahrenheit un passo era 1 °C e l'etichetta diceva «53,6°»,
  «55,4°»; in Beaufort cinque km/h a passo davano più posizioni con la stessa forza.
- **Il salvataggio resta canonico**: la posizione passa da `RuleVariables.canonicalValue` verso
  la regola e da `displayValue` verso lo slider, le due funzioni di 14.9. Una forza Beaufort
  salva il limite inferiore della fascia, come già faceva la parola («almeno 6 Bft» è 39 km/h).
  La soglia salvata fuori dalla griglia del lettore (scritta in km/h e letta in miglia: 72 km/h
  sono 44,7 mph) si mostra com'è finché il cursore non si muove: lo stato dello slider è nullo
  all'apertura, l'etichetta legge la soglia della regola, e «Fatto» senza movimento riconsegna
  la soglia identica. Il chip che cambia la variabile tiene la soglia com'è quando la griglia
  nuova la contiene, e la porta al bordo quando no (`RuleText.inRange`), dove prima la tagliava
  sempre sulla griglia canonica.
- **Le tacche solo fino a venti passi** (`RuleText.ticks`): l'umidità (20), l'UV (12), le forze
  Beaufort (12) le tengono; la temperatura (75 passi in °C, 135 in °F), i millimetri (100), i
  pollici (30) no: `steps = 0`, lo slider è continuo e lo scatto al passo è tenuto a mano
  (`RuleText.snap`, relativo al minimo della griglia, così 28,0 + k·0,1 cade sui decimi anche in
  virgola mobile). L'etichetta non mostra mai un valore su cui il cursore non può tornare.

**Il peso in chiamate.** Nessuna richiesta nuova, nessuna variabile nuova: 8,4 chiamate il fetch,
come prima.

**Decisioni e deviazioni.**

- *`valueSpec` vuole le unità, senza default*: un chiamante che le dimentica si vedrebbe la
  griglia metrica in silenzio, e la prova (`RuleTextTest`) chiama ogni unità per nome.
- *`current.wind.gust_kph` prende la griglia del vento, non delle raffiche*, perché l'id contiene
  «wind» e la lettura è nell'ordine di prima: era così dal 23 set 2026 e lo step non lo tocca;
  cambia solo la scala per unità.
- *Le tre funzioni pure dello slider stanno in `RuleText`* (`positions`, `ticks`, `snap`,
  `inRange`) e non nel composable, così la regola delle venti tacche e lo scatto al passo hanno
  un test senza Compose.
- *Nessuna stringa nuova*, nessuna in IT ed EN: l'etichetta è quella di `RuleText.value`, che
  già diceva l'unità dal 14.9.
- *Nessuno screenshot cambia*: lo slider sta in un dialogo sopra lo sheet dell'editor, che le
  immagini del README non catturano, e i default sono metrici.

**Come è stato verificato.** `RuleTextTest` (quattro test nuovi): per ognuna delle sedici coppie
variabile-unità la griglia attesa, e per ogni posizione della griglia l'etichetta salvata in
canonico e riletta è intera dove l'unità è intera (°F, mph, m/s, nodi, Bft, mmHg), al decimo nei
pollici, e due posizioni non hanno mai la stessa etichetta; le tacche (19 l'umidità, 11 l'UV e il
Beaufort, 0 la temperatura, i millimetri, i pollici); lo scatto al passo dentro il bordo (29,94 →
29,9; 29,96 → 30,0; 40 → 31,0 inHg; 54,6 → 55 °F); la soglia dentro la griglia tenuta tale e quale
(72 km/h in miglia, 40 km/h in Beaufort, 45 °C in °F) e fuori portata riportata al bordo (200 km/h
→ 75 mph, −40 °C → −22 °F). I due test di prima che chiamavano `valueSpec` passano le unità
metriche. Suite completa verde (`./gradlew test :app:testDebugUnitTest`, 2 712 test, 15 saltati:
gli screenshot, che girano solo su richiesta); lint verde (`:app:lintDebug`). DESIGN §8.9 (il
paragrafo dello slider), VISION §5.4 (una riga), CHANGELOG
`[Unreleased]`.

## 15.6 — Il rumore che va via (committente, 28 ott 2026)

**Fatto.** Le quattro cose che la revisione aveva chiamato rumore escono insieme, in uno step
solo perché cambiano la stessa immagine del README una volta.

- **La riga «In qualunque ora, se serve: …»** sotto la striscia del giorno non c'è più, con le sue
  due stringhe (`alerts_day_anytime`, `alerts_day_yours`): ripeteva i titoli di due switch poco
  sotto, e «i tuoi avvisi» era vero solo delle regole «Sempre». `AlertDayStrip` non riceve più le
  impostazioni né il flag delle regole: solo le finestre e l'ora.
- **La legenda** dice «Dalle 22 alle 7 arrivano senza suono, tranne le allerte rosse e i
  promemoria del cielo», senza «(tratteggio)»: il tratteggio resta, sotto le ore che vuol dire,
  e nessuna parola lo spiega (VISION §5.7).
- **«Dodici ore asciutte» esce dalle idee**: via il `Template` da `RuleText.templates`, le sue
  quattro stringhe in IT ed EN e la riga di `templateIcon`. Le idee sono sei, nell'ordine di
  prima; la guida («una finestra per correre, ghiaccio domattina, una notte serena») cita ora
  «caldo forte». Le regole già create dall'idea restano, sono testo del lettore, e `ruleIcon` dà
  loro il disegno della pioggia come a ogni regola sulla probabilità.
- **Il flag `userRules` va via** da `NotificationSettings`, da `SettingsStore` (lettura, setter e
  chiave; la preferenza `notif_user_rules` già scritta resta nel file e nessuno la legge), da
  `SyncScheduler.alertsWanted` (una regola accesa tiene vivo il job da sola), dal worker, da
  `notificationsPromised`, da `promisedChannels` e dal flusso delle regole di Oggi, che ora
  filtra solo per lo switch di ogni regola. Nessuna schermata cambia: il flag non aveva mai avuto
  un interruttore.

**Il peso in chiamate.** Nessuna richiesta nuova, nessuna variabile nuova: 8,4 chiamate il fetch,
come prima.

**Decisioni e deviazioni.**

- *La preferenza salvata si ignora, non si cancella*: una chiave in più nel DataStore non costa
  niente, e una migrazione per toglierla sarebbe codice per un valore che nessuno legge.
- *La prova delle idee* (`RunTemplatesTest`) non può nominare una stringa che non c'è più: il
  test nuovo fissa le sei idee nell'ordine dello sheet e che nessuna sia sulla sola probabilità
  delle prossime dodici ore.
- *I due test del flag spento* (in `NotificationsPromisedTest` e `SyncSchedulerTest`) non hanno
  più un flag da spegnere: la regola spenta resta l'unico modo per cui una regola non promette.

**Come è stato verificato.** Suite completa verde (`./gradlew test :app:testDebugUnitTest`,
2 714 test, nessuno saltato perché gli screenshot sono girati nello stesso giro); lint verde
(`:app:lintDebug`). `alerts.png` rigenerato e guardato: sotto la striscia resta la sola frase
delle ore silenziose, la riga «At any hour, when needed» non c'è più; `alerts-yours.png` e le
altre immagini non cambiano. DESIGN §8.9b (la riga e la legenda, le idee), VISION §5.4 (le idee),
CHANGELOG `[Unreleased]`.

## Il Cielo senza luogo è lo stato vuoto delle altre due schede (committente, 28 ott 2026)

**Fatto.** Con nessun luogo attivo il Cielo disegnava una copia sua di «Nessun luogo»
(`NoPlaceForSky`): titolo a `titleMedium`, testo e azione al bordo iniziale e in alto, dove Oggi
e Avvisi disegnano `NoPlaceState` (ui/today), centrato, con il titolo a `titleLarge`. Verificato
nel codice: tre schede, due allineamenti di uno stato solo. Il Cielo passa allo stesso composable
con la sua frase (`sky_no_place_body`, che resta), e la copia privata va via; la nota di
`NoPlaceState` dice che è di tre schede. DESIGN §8.11 lo scrive; CHANGELOG `[Unreleased]`,
«Fixed». Nessuno screenshot: lo stato non è nella registrazione. Nessuna stringa nuova.

## 15.7 — Le righe come il Cielo, l'ora relativa, «Cosa guardare» (committente, 28 ott 2026)

**Fatto.** Tre cose, tutte sulla pagina Avvisi e sul suo editor.

- **Le righe prendono le misure del Cielo.** `ListRow` (`ui/components`, nuovo): inizio 12,
  spazio 12 fra disegno e testo, 8 sopra e sotto, altezza minima 72. `SkyRow` le legge da lì
  (le sue tre costanti private se ne vanno), e `AlertRow` e `RuleCard` pure, dove spendevano 16,
  14 e 14 senza un'altezza minima. L'icona resta a 36 dp (`RowIcon`, decisione dell'apertura:
  un termometro a 51 è un manifesto); lo switch tiene i suoi 16 dp in coda, perché a differenza
  della campanella del Cielo non ha una scatola d'aria sua. Anche la riga del prossimo momento
  nella card di testa prende lo spazio 12.
- **«Scattato l'ultima volta» dice il giorno come il bollettino** (`RuleText.firedAt`): «oggi
  alle 7:12», «ieri alle 7:12», «il giorno 25 ott alle 7:12» oltre ieri, la stessa regola di
  `WarningText.issued` (il giorno solo quando non è oggi). La data è giorno e mese: lo storico
  tiene tre giorni, l'anno non è mai in dubbio. Prima la riga diceva «27 ott, 07:12» sotto una
  testa che diceva «di ieri alle 14:34». Tre stringhe nuove in IT ed EN (`rule_fired_today`,
  `_yesterday`, `_dated`); in italiano «il giorno», non «il», perché l'articolo non tocchi mai
  la cifra (la regola di `ItalianArticleTest`, già usata da `warning_issued_dated`).
- **«Cosa guardare» è uno sheet** (`VariablePickerSheet`, `ChiaroSheet`) con quattro titoli:
  «Adesso», «Prossime 6 ore», «Prossime 12 ore», «Oggi», la prima parola di ogni variabile
  (`RuleText.variableGroups`, dal prefisso dell'id, nell'ordine del registro dentro ogni gruppo);
  i titoli a `labelLarge` in `primary` come «Parti da un'idea», le righe `ListItem` sul fondo
  dello sheet, la voce scelta con la spunta in `primary` del catalogo e `selected` per TalkBack.
  Era un dialogo con 31 radio in fila. Quattro stringhe nuove per i titoli. La domanda resta in
  testa allo sheet a `titleMedium`: il chip che lo apre dice la variabile, non a cosa serve la
  lista (lo sheet delle idee non ne ha perché il FAB la dice, qui nessuno la dice).

**Il peso in chiamate.** Nessuna richiesta nuova, nessuna variabile nuova: 8,4 chiamate il fetch,
come prima.

**Reso e guardato** (la classe di prova a pagina intera, cancellata prima del commit): Avvisi
con due regole accanto al Cielo, 384 dp, inglese. A 12 dp d'inizio l'icona da 36 non resta
sospesa: il suo inchiostro sta dove sta quello del Cielo, le colonne di testo partono a 76 dp su
Avvisi e a 91 sul Cielo. Le misure restano come lo step le ha scritte, senza tornare a 16. Lo
sheet non si cattura (finestra sua).

**Decisioni e deviazioni.**

- *Le costanti sono un oggetto in `ui/components`* (`ListRow`), non tre `val` copiate: il Cielo
  le legge da lì, così le due schede non possono più scostarsi.
- *Il verticale è 8 come nel Cielo*, non nominato dallo step: con l'altezza minima a 72 una riga
  corta si centra, e una da tre righe resta più bassa di prima di 12 dp.
- *La data senza anno* («d MMM» del locale) dove la forma del bollettino la mette per intero: lo
  storico delle regole è di tre giorni, l'anno sarebbe rumore.
- *Il titolo in testa allo sheet*, che lo sheet delle idee non ha, per il motivo sopra.

**Come è stato verificato.** `RuleFiredTest` (nuovo, Robolectric in italiano): oggi, ieri, tre
giorni fa («il giorno 25 ott … alle 7:12»), e la riga intera «Scattato l'ultima volta ieri alle
7:12»; `VariableGroupsTest` (nuovo, puro): ogni variabile del registro in un gruppo e uno solo,
nell'ordine del registro, i quattro titoli nell'ordine adesso/6/12/oggi. Suite completa verde
(`./gradlew test :app:testDebugUnitTest`, 2 726 test, nessuno saltato perché gli screenshot sono
girati nello stesso giro); lint verde (`:app:lintDebug`). `alerts.png` e `alerts-yours.png` rigenerati e guardati: le righe più strette d'inizio e più basse, le altre immagini non cambiano. DESIGN §8.9b
(un punto nuovo con le misure, l'ora, lo sheet), VISION §5.4, CHANGELOG `[Unreleased]`.

## 15.8 — Le graffe diventano parole (committente, 28 ott 2026)

**Fatto.** Il campo del messaggio dipinge ogni `{segnaposto}` che il registro conosce come la sua
frase, su un chip in linea; il testo salvato tiene l'id.

- **`PlaceholderTransformation`** (`ui/alerts/PlaceholderWords.kt`, nuovo): una
  `VisualTransformation` del campo (`OutlinedTextField` su `TextFieldValue`, l'API di prima: non
  serve passare alla nuova). Ogni `{nome}` che `RuleText.placeholderPhrase` conosce — i due del
  trigger («quello che ha fatto scattare l'avviso», «l'ora in cui è scattato») e ogni variabile
  del registro in entrambe le grafie (`current.temp_f` è `current.temp_c` nell'unità del lettore,
  `RuleVariables.canonicalId`) — diventa la sua frase fra due spazi sottili su un chip di
  `secondaryContainer` con inchiostro `onSecondaryContainer`. Un nome sconosciuto resta com'è:
  un refuso non si nasconde.
- **La mappa del cursore** (`PlaceholderWords.mapping`): prima di un chip i due testi differiscono
  di quanto i chip precedenti sono cresciuti; un offset salvato dentro un id è la fine del chip, un
  offset dipinto dentro un chip è l'offset salvato dopo la sua `}`: l'id non ha un dentro a schermo.
- **Il tasto indietro su un chip lo toglie intero** (`PlaceholderWords.wholeDeletion`): il campo
  cancella un carattere salvato, la `}` dopo cui stava il cursore; quando l'edit è tutto lì e la
  graffa chiudeva un segnaposto noto, `onValueChange` toglie il segnaposto intero e mette il
  cursore dove cominciava. Ogni altro edit resta come il campo l'ha fatto. La mappa lo permette,
  quindi niente cancellazione carattere per carattere.
- **«Aggiungi un valore» inserisce come oggi** (l'id nella grafia dell'unità), e il chip compare
  dove atterra. La riga «Direbbe: …» in testa all'editor resta.
- **Le parole che dicevano «graffe»** cambiano: `rule_message_help` e il paragrafo della guida
  (`guide_alerts_message_body`, che stampava `{trigger.value}` e `{trigger.time}` in chiaro)
  dicono i valori come parole su un chip. Nessuna stringa nuova.

**Il peso in chiamate.** Nessuna richiesta nuova, nessuna variabile nuova: 8,4 chiamate il fetch,
come prima.

**Reso e guardato** (classe di prova in `testDebug`, cancellata prima del commit): il campo con
il messaggio della revisione («Si pedala: {current.temp_c} e pioggia al
{next_6h.precip_chance_max} {refuso») e quello dell'idea del ghiaccio, in italiano, chiaro e
scuro, a 384 dp. Le frasi stanno in linea sul fondo di `secondaryContainer`, vanno a capo con le parole del messaggio, il refuso resta «{refuso» com'è, in entrambi i temi. L'editor è uno sheet e le immagini del README non lo catturano: nessuno
screenshot cambia. La resa su un telefono vero resta da vedere al committente: se non regge, la
decisione dell'apertura (la sola riga «Direbbe: …»).

**Decisioni e deviazioni.**

- *`VisualTransformation`, non `OutputTransformation`*: il campo è su `TextFieldValue` e la
  vecchia API basta; passare alla nuova per questo sarebbe un cambio a parte.
- *Il chip è uno `SpanStyle` con sfondo*, non un composable: dentro un campo di testo una
  trasformazione può solo colorare, e un fondo con due spazi sottili è quanto di chip ci sta in
  una riga che si scrive. Niente bordo tondo: non si può.
- *Un offset dentro un id va alla fine del chip*, non all'inizio: così un tocco sul chip mette il
  cursore dopo, e il tasto indietro lo toglie intero.

**Come è stato verificato.** `PlaceholderWordsTest` (nuovo, puro): il testo dipinto su un
messaggio con due graffe e un refuso; gli offset avanti (prima, dentro e dopo ogni chip, il
refuso spostato della crescita) e indietro (dentro il chip → dopo la `}`), e per ogni offset
dipinto il ritorno non arretra mai; la cancellazione intera dopo un chip e i quattro edit che
non lo sono (una lettera scritta, una lettera del refuso, la graffa di un nome ignoto, una
selezione). Suite completa verde (`./gradlew test :app:testDebugUnitTest`, 2 736 test, nessuno
saltato perché gli screenshot sono girati nello stesso giro, nessuna immagine cambiata); lint
verde (`:app:lintDebug`). DESIGN §8.9b (un punto nuovo), VISION §5.4, CHANGELOG `[Unreleased]`.

## 15.9 — Le variabili «domani» e l'idea «Neve domani» (committente, 29 ott 2026)

**Fatto.** Le regole leggono domani, e la prima idea che lo usa è nelle idee.

- **Sette variabili `tomorrow.*`** (`RuleVariables`): `tomorrow.high_c`, `tomorrow.low_c`,
  `tomorrow.precip_pct`, `tomorrow.uv_max`, `tomorrow.precip_mm`, `tomorrow.snow_cm`,
  `tomorrow.gust_max_kph`, gli stessi sette fatti di `today.*` letti dalla riga **datata domani**
  (`now.toLocalDate().plusDays(1)`), null se manca, come `today.*` legge la riga datata oggi dal
  15.1: mai la seconda riga, che su una cache di ieri è oggi. Il registro dei due giorni è un
  ciclo solo (`for ((day, offset) in …)`, come quello delle 6 e 12 ore), così i due giorni non
  divergono; `precip_mm` è la sola pioggia, come oggi. Temperatura e raffiche seguono le unità
  del lettore come quelle di oggi (`tomorrow.high_f`, `tomorrow.gust_max_kn`).
- **Nel motore una regola su domani è a forma di giorno** (`RuleEngine`): `dayShaped` riconosce
  `today.*` e `tomorrow.*` (con le `current.*` accanto); l'impronta è sulla **data di cui parla**
  (`…:<domani>:DAY`), e `DayRulesFrom` (le 6) ha la sorella `TomorrowRulesFrom`, **mezzogiorno**,
  con il motivo delle decisioni nel commento: al mattino il riepilogo dice oggi, dal pomeriggio si
  pianifica domani, alle 18 il riepilogo della sera lo chiude. Una voce per data: scattata a
  mezzogiorno, alle 18 col riepilogo e alle 23:30 tace; dopo mezzanotte «domani» è la data dopo,
  e aspetta di nuovo mezzogiorno. Una regola su domani vera solo dalle 19 parla alle 19, una volta.
- **`RuleEngine.speaksFrom(rule)`**, pubblica: l'ora da cui una regola senza orari propri, se è
  un fatto di un giorno, può parlare (6:00 oggi, 12:00 domani), null per ogni altra. Il motore la
  usa per il cancello, la card per dirlo (sotto).
- **Le parole** IT/EN: «la massima di domani», «la minima di domani», «la pioggia di domani», «l'UV
  massimo di domani», «i millimetri di pioggia di domani», «la neve di domani», «le raffiche più
  forti di domani»; il quinto gruppo **«Domani»** nello sheet «Cosa guardare» dopo «Oggi», nello
  stesso ordine (`VariableGroupsTest` lo tiene riga per riga).
- **L'idea «Neve domani»** (`tomorrow.snow_cm ≥ 5`, a qualsiasi ora, «Domani {trigger.value} di
  neve: gomme invernali e partenza in anticipo.» / «{trigger.value} of snow tomorrow: winter
  tyres, and an early start.») entra dopo «Ghiaccio domattina», che resta sulle 12 ore: il
  ghiaccio è una domanda su stanotte, la neve su una data. Sette idee.
- **L'icona della regola.** L'idea porta la nuvola che nevica (`ChiaroIcons.snowfall`, nuovo
  accessor su `mc3_snow`, già nell'APK per la quota neve: nessun disegno nuovo,
  `shipped_icons.py` non cambia), non il fiocco del ghiaccio che le sta accanto nello sheet; la
  prende anche ogni regola sulla neve, così una regola nata dall'idea tiene il disegno quando la
  soglia si sposta (prima la neve prendeva il fiocco del gelo). E `ruleIcon` riconosce una
  temperatura dal **tipo** della variabile, non da «temp» nell'id: la massima e la minima di un
  giorno (`today.high_c`, `tomorrow.low_c`) cadevano sulla nuvola.
- **La guida** (`guide_alerts_yours_body`, IT/EN): «neve domani» fra le idee, e «i numeri di
  domani: un avviso su domani suona una volta sola, da mezzogiorno del giorno prima».

**Il peso in chiamate.** Nessuna richiesta nuova, nessuna variabile nuova: le righe giornaliere
c'erano già, sette giorni, domani compreso. 8,4 chiamate il fetch, come prima.

**Decisioni e deviazioni.**

- *L'ordine delle variabili di domani è quello di oggi* (massima, minima, pioggia, UV,
  millimetri, neve, raffiche), non quello in cui lo step le elenca: due gruppi che fanno le stesse
  sette domande si leggono riga per riga.
- *Una regola su domani con orari propri parla nella sua prima fascia, anche prima di
  mezzogiorno*, come una su oggi con la fascia delle 5 vince sulle 6 (`RuleEngineTest` lo aveva già
  per oggi): sotto un cancello a mezzogiorno una regola «7–9» su domani non potrebbe parlare mai.
  Il mezzogiorno vale per le regole «Sempre», che sono quelle che il lettore non ha messo in orario.
- *Oggi e domani nella stessa regola* («massima di oggi e di domani ≥ 30», due giorni di caldo)
  parlano una volta, quando parla domani: da mezzogiorno, con l'impronta su domani. *Domani accanto
  a una finestra* (`next_6h.*`) tiene le mezze giornate, come oggi accanto a una finestra.
- *L'impronta di domani porta lo stesso `:DAY` di quella di oggi*, sulla data di domani: una
  regola che cambia condizioni ha l'impronta pulita da `clearRule` (14 ott 2026), quindi le due
  forme della stessa regola non si incontrano mai sulla stessa data.
- *Deviazione, non nello step: la card dice quando può suonare.* Alle 9 «Neve domani» è vera e
  la card diceva «Adesso sì», con il suono che arriva a mezzogiorno: la promessa che il 15.2 ha
  tolto al bollettino. Ora `ruleReading` risponde `RulePreview.NotYet` quando la finestra è aperta
  ma `speaksFrom` non è ancora arrivato, e la riga dice «Adesso sì, può suonare dalle 12:00»
  (`rule_now_from`, IT/EN), sulla card e in testa all'editor, con «Direbbe: …» sotto. Vale anche
  per una regola su oggi prima delle 6, che aveva lo stesso silenzio non detto dal 23 set 2026.
- *L'agenda di Oggi non cambia*: una regola su domani vera sta fra le righe «adesso», con il suo
  messaggio («Domani 8 cm di neve: …»). La riga è una lettura, non la promessa che il telefono
  suonerà (15 ott 2026), e la lettura è vera.
- *Le idee si trovano per titolo, non per posto*, nei test (`RuleSentenceTest`, `RuleNowTest`) e
  nella registrazione del README (`MilanRecording.yourRules`): «Neve domani» al terzo posto
  spostava la Corsa, e `templates[2]` sarebbe diventata la neve.

**Come è stato verificato.** `RuleVariablesTest` (la riga datata domani, mai la seconda; la cache
di ieri; un report che finisce oggi non ha domani; la neve che il modello non porta è null; le
unità); `RuleEngineTest` (una voce per data, non prima di mezzogiorno, non due volte alle 18 col
riepilogo né alle 23:30, il giorno dopo di nuovo da mezzogiorno con l'impronta della data dopo;
vera solo la sera parla una volta; oggi e domani insieme; domani accanto a una finestra; la fascia
del lettore prima di mezzogiorno; `speaksFrom` per ogni forma); `RunTemplatesTest` (sette idee
nell'ordine dello sheet; «Neve domani» scatta su 5 cm di domani, non su 4,9 né sui 12 di oggi, e
salta senza la neve); `VariableGroupsTest` (cinque gruppi, domani come oggi riga per riga);
`RuleNowTest` («Adesso sì, può suonare dalle 12:00»); `RuleReadingTest` (alle 10:30 la card di una
regola su domani è `NotYet` alle 12, una soglia mancata legge i 18 di domani, non i 20 di oggi né
i 30 di ieri). Suite completa verde (`./gradlew test :app:testDebugUnitTest`, 2 752 test, 15 saltati: gli
screenshot, che girano solo su richiesta); lint verde (`:app:lintDebug`). Gli screenshot che leggono
le regole (`alerts.png`, `alerts-yours.png`, `today-day.png`) resi di nuovo: identici byte per byte,
git non li vede cambiati; lo sheet delle idee e quello di «Cosa guardare», dove la vista cambia,
non sono nelle immagini del README. DESIGN §8.9b (un punto nuovo, e la riga in «Is it true
now»), §8.17b (la regola di domani e l'idea), VISION §5.4, CHANGELOG `[Unreleased]`.

## 15.10 — Le quantità nelle prossime ore (committente, 29 ott 2026)

**Fatto.** Le regole leggono quanto cade nelle prossime 6 e 12 ore, non solo quanto è probabile,
e «le prossime N ore» sono una finestra sola per ogni variabile che le nomina.

- **Le righe orarie portano le quantità** (`HourlyForecast.precipMm`, `snowCm`, nuovi e
  nullable): il totale dell'ora in mm (pioggia, rovesci e l'acqua della neve) e la neve in cm,
  valori d'intervallo presi dallo slot dopo la riga come la probabilità e la raffica (l'ora che
  comincia alla riga). Il mapper li aveva già, per il motore degli stati, e non li passava. La
  cache su disco tiene il DTO e lo rimappa, quindi anche un report in cache li porta.
- **Quattro variabili** (`RuleVariables`, nel ciclo delle 6 e 12 ore, subito dopo la
  probabilità): `next_6h.precip_mm`, `next_12h.precip_mm`, `next_6h.snow_cm`, `next_12h.snow_cm`.
  La pioggia è **totale − neve / 0,7**, mai sotto zero, il riparto che STATES §1 e la regola 3
  leggono per lo stato dell'ora (`rain` e `showers` cambiano senso con il modello, il totale e la
  neve no); la neve è la somma della neve. `{trigger.time}` è l'ora di punta di ciascuna, nessuna
  quando non cade niente (allora il messaggio dice l'ora in cui scatta, come sempre).
- **Null, mai uno zero che nessuno ha previsto**: un'ora della finestra senza totale o senza
  neve, una riga che manca, un report che finisce dentro la finestra lasciano la somma senza
  valore, e la regola salta. Un picco resta sulle ore che ci sono, come prima (un'ora senza
  raffica è lasciata fuori, mai letta come calma): il massimo delle ore note è un fatto su di
  loro, una somma corta di un'ora è un numero sbagliato.
- **Una finestra sola per ogni `next_Nh.*`** (`WeatherReport.hoursAhead`, `:core:domain`,
  `HoursAhead.kt`): **l'ora in cui sei e le N−1 dopo**, N in tutto, le prime N caselle della striscia di Oggi con «Ora» in testa; per
  istanti, così la notte in cui l'ora torna indietro (le 01:00 due volte) sei ore sono sei righe.
  Vale per le somme nuove e per le variabili di prima (la probabilità, la minima e la massima, le
  raffiche, la percepita, il maltempo). Un valore che cade nell'ora in corso dice la sua ora come
  adesso (`hourOf`): «verso le 14:00» alle 14:30 era un'ora già cominciata.
- **Il «Caldo intenso» conta sulla stessa finestra** (`AlertEngine.findHeat`): le due ore sopra
  i 35° percepiti fra le prossime dodici sono l'ora in corso e le undici dopo. Prima le righe
  con l'etichetta in `[adesso, adesso + 12]` lasciavano fuori l'ora in corso (due ore calde,
  questa e la prossima, contavano per una) e allo scoccare dell'ora ne contavano tredici. Il
  riferimento dell'avviso resta la prima ora calda, un'etichetta della serie, perché la
  notifica ritrova da lì le ore del caldo (`AlertDetails.heatWindow`); la frase invece dice il
  picco, e quando il picco è l'ora in corso dice «Percepiti fino a 39°, adesso.»
  (`notif_heat_body_now`, IT/EN; `isUnderWay`, accanto alla finestra), dove «verso le 14:00»
  alle 14:30 nominava un'ora già cominciata.
- **Le parole** IT/EN («i millimetri di pioggia nelle prossime 6 ore», «la neve nelle prossime
  12 ore», …), che lo sheet «Cosa guardare» mette nei gruppi delle 6 e delle 12 ore dopo la
  probabilità; **`valueSpec`** 0–50 mm passo 1 sulle 6 ore, 0–100 sulle 12, neve 0–50 cm su
  entrambe (nessuna tacca: più di venti posizioni). La riga della guida su cosa si può guardare
  nomina le prossime ore.

**Il peso in chiamate.** Nessuna richiesta nuova e nessuna variabile nuova: `precipitation` e
`snowfall` orari si chiedono dal 25 set 2026 per il motore degli stati. 8,4 chiamate il fetch,
come prima.

**Decisioni e deviazioni.**

- *La finestra delle variabili di prima cambia* (committente, 29 ott 2026, sulla PR: l'app è in
  beta e non c'è compatibilità da tenere con le regole già salvate; «fai la cosa corretta, da app
  premium»). Era la finestra delle righe con l'etichetta in `[adesso, adesso + N]`: alle 14:30
  lasciava fuori l'ora in corso — «la pioggia nelle prossime 6 ore» della Bici non vedeva il 90%
  che sta cadendo adesso — e alle 14:00 in punto ne contava sette. La prima stesura di questo step
  dava alle somme una finestra loro (le righe che cominciano in `[adesso, adesso + N)`) e lasciava
  le altre com'erano: due finestre per «le prossime 6 ore», e quella delle somme saltava ancora
  l'ora in corso. Ora una sola, quella che il lettore vede sulla striscia. Il prezzo: alle 14:30
  l'ultima mezz'ora delle sei (20:00-20:30) è fuori, come lo è dalla striscia; a un'ora intera per
  casella non c'è modo di tenerla senza contare mezz'ora che nessuno ha previsto a parte.
- *Il caldo nello stesso step* (committente, 29 ott 2026, sulla PR, dopo aver chiesto se le
  finestre degli avvisi pronti fossero da sistemare): solo il caldo conta ore. «Pioggia in
  arrivo» e «Maltempo» cercano l'ora in cui una serie **comincia** (`firstArrival`) e lasciano
  fuori l'ora in corso apposta (23 set 2026: «Ombrello verso le 12» su una pioggia già in
  corso dalle 10, il temporale annunciato di nuovo dopo mezzanotte); con la finestra delle
  regole quei difetti tornerebbero, e la fine inclusiva va bene per «entro 6 ore». Fatto qui e
  non in uno step a parte: è la stessa definizione, e uno step costava più della modifica.
  Nel test di `AlertEngine` il report di Roma porta ora il fuso di Roma: la finestra legge gli
  istanti, e quello del campione (New York) la spostava di sei ore.
- *Senza la neve di un'ora, nessuna delle due somme*: STATES §4 per lo **stato** prende la fase
  dal codice del provider quando `snowfall` manca, ma un numero di una regola non è uno stato, e
  un codice non dà millimetri. Misurato: nella sonda del 27 ott 2026 (45 città) `precipitation`,
  `snowfall` e `showers` orari non arrivano mai nulli, quindi la strada è per un modello che
  domani cambi, ed è scritta in STATES §7.
- *Arrotondata al centesimo*, la precisione del provider: sei ore di 0,2, 0,2, 0,3, 0,2, 0,4 e
  0,7 mm sommate in virgola mobile fanno 1,9999999999999998, e «almeno 2» non scattava (il caso
  è nel test).
- *Il test «ogni variabile risolve sul campione»* ora gli dà dodici ore con le quantità: il
  campione ne ha cinque, senza, e una somma vuole la finestra intera. Il campione resta com'è.
  Due casi di `RuleEngineTest` che valutavano alle 9 sul campione (le cui ore partono alle 15)
  valutano alle 10: con la finestra nuova le sei ore delle 9 finiscono alle 14, e non c'era più
  niente da leggere; la sostanza dei test (la mezza giornata, il secchio del mattino) non cambia.

**Come è stato verificato.** `RuleVariablesTest`: la finestra (alle 13 le sei ore sono 13-18 e
la riga delle 19 resta fuori, alle 14 entra, alle 15:30 l'ora in corso è la prima e dice adesso);
una finestra mista pioggia e neve — 2 mm nell'ora in corso, la punta di 3,5 alle 15, un'ora di
sola neve, un'ora di pioggia e neve, un'ora asciutta, un'ora dove l'acqua della neve supera il
totale, come a Svalbard — con le somme e le ore di punta a 6 e 12 ore, la punta nell'ora in corso
detta adesso, la finestra asciutta a zero senza ora; un'ora senza neve, una senza totale, una
mancante dentro la finestra e una fuori, un report che finisce prima; la pioggia di adesso che
conta alle 14:30, sei ore alle 15:00 in punto e nella notte del cambio d'ora a New York; la somma
in virgola mobile. `WeatherReportMapperTest` (la riga porta il totale e la neve dello slot dopo;
liste vuote leggono null); `RuleTextTest` (le parole e le gamme, nessuna tacca); `RuleEngineTest`
(i due casi spostati alle 10); `AlertEngineTest` (il caldo: alle 19:30 un'ora sola, quella in
corso; alle 23 l'orizzonte arriva alle 10 del 31, un'ora calda, a mezzanotte anche alle 11, e
il caldo è del 31; alle 18:30 l'ora in corso è una delle due, e l'avviso si ancora alle 18);
`AlertNotifierTest` (il picco nell'ora in corso detto «adesso», ancora davanti con la sua
ora). Suite completa verde (`./gradlew test :app:testDebugUnitTest`,
2 762 test, 15 saltati: gli screenshot, che girano solo su richiesta); lint verde
(`:app:lintDebug`). Gli screenshot che leggono le regole resi di nuovo: `alerts.png` e
`alerts-yours.png` identici byte per byte; **`today-day.png` cambia in un punto solo**, la riga
della Corsa nel resto della giornata, che diceva «13:00» e dice «Adesso», come la Bici sopra: la
regola vale adesso, e «13:00» era la prima riga dopo l'ora in corso, scelta come «punta» di una
pioggia allo 0% in ogni ora. Guardata e tenuta; il testo alternativo del README resta vero.
Nessuna immagine del README mostra il caldo. DESIGN §8.9b (un punto nuovo) e §8.17b (il caldo),
VISION §5.4, STATES §7 (le regole sommano sullo stesso riparto, e la finestra), CHANGELOG
`[Unreleased]` (Added, e due voci in Changed).

## 15.11 — La regola legata a un luogo: il modello, il motore, il worker (committente, 29 ott 2026)

**Fatto.** Una regola può guardare un luogo salvato invece del luogo attivo, e il job la legge
lì, sulla previsione e nell'ora di quel luogo. Niente ancora a schermo: l'editor che la lega e la
card che lo dice sono di 15.12, e finché non ci sono nessuna regola ha un luogo, quindi l'app oggi
si comporta come prima (con un'eccezione, l'ultimo punto delle deviazioni).

- **Il modello** (`NotificationRule.place: RulePlace?`, `:core:domain`): null è il luogo attivo,
  quale che sia, ed è ogni regola salvata prima; `RulePlace` porta l'id del luogo salvato
  (`City.id`) e il suo nome. Mai il GPS: la posizione è il luogo attivo per definizione.
- **Dove si legge ogni regola** (`RulePlaces`, puro, accanto al motore): il luogo attivo legge le
  regole senza luogo e quelle legate a lui (`readsActive`); ogni altro luogo salvato le sue
  (`elsewhere`, nell'ordine della lista); una regola legata a un luogo tolto non si legge da
  nessuna parte (`isOrphan`). **Il motore non cambia**: la chiave del luogo è già nel latch e
  nelle impronte (`cityKey`), e quelle di una regola legata sono del suo luogo.
- **Il worker** (`WeatherSyncWorker`): dopo il luogo attivo, che fa quel che faceva, fetcha una
  volta per periodo ogni luogo salvato a cui è legata una regola accesa e legge su quel report le
  sole regole di quel luogo, nel suo fuso; la notifica porta il nome del luogo, il latch la sua
  chiave, e quel che è scattato va nel commit della **sua** cronologia, da cui Avvisi leggerà
  «scattato l'ultima volta». La lettura delle regole è una funzione sola (`readRules`) per il
  luogo attivo e per gli altri; gli avvisi pronti restano del solo luogo attivo (`postAlerts`).
- **Il luogo tolto**: al primo passaggio del job dopo la rimozione la regola si spegne
  (`RuleStore.switchOffOrphans`, una sola transazione) e tiene il suo luogo; il view model di
  Avvisi la segna per la card (`RuleCardModel.placeGone`) dal momento in cui il luogo non c'è
  più, letto dalla lista dei salvati, non dal passaggio del job, e da quel momento la pagina non
  la conta fra le sue promesse (`canRing`: la card «notifiche spente» e quella dei canali
  silenziati), perché non può suonare.
- **`SyncScheduler.alertsWanted`** conta già ogni regola accesa, legata o no; una regola orfana
  accesa tiene vivo il job fino al passaggio che la spegne, e quel passaggio, se non resta
  nient'altro da fare, lascia andare il job da sé. **`RuleStateStore.retain`** non cambia: i
  luoghi vivi sono già tutti i salvati, e il latch di una regola legata vive quanto il suo luogo.

**Il peso in chiamate.** Nessuna richiesta nuova: **8,4 chiamate per luogo legato per periodo**
(5,4 la previsione, 1,6 i testimoni, 1,4 l'aria), solo mentre una regola accesa è legata a quel
luogo, e un luogo che è anche la città di un widget si fetcha una volta (la seconda lettura è
dalla cache). Con il periodo di default (un'ora) e tre luoghi legati: **25 chiamate in più l'ora,
605 al giorno**, il 6% dei 10 000 giornalieri di un indirizzo (circa un sedicesimo), con il luogo attivo
che resta a 202 al giorno. Il caso peggiore che l'app permette: dieci regole su dieci luoghi
diversi (`MaxRules`) al periodo di 15 minuti, 336 l'ora e 8 064 al giorno, più gli 806 del luogo
attivo, **8 870 al giorno**: sotto il tetto, e 92 chiamate in un passaggio contro le 600 del
minuto. Il caso è possibile e raro; se un giorno la sonda o un lettore dicesse che pesa, la
strada è il periodo dei luoghi legati, non un tetto al numero di luoghi.

**Decisioni e deviazioni.**

- *Il luogo è `RulePlace(id, nome)`, non `placeId: Long?`* come scriveva lo step: la card di
  15.12 dice «Bormio non c'è più», e quando il luogo è tolto la lista dei salvati il nome non ce
  l'ha più. Il nome tenuto sulla regola si usa solo allora; finché il luogo è salvato lo nomina il
  suo record.
- *La regola orfana tiene il luogo* (lo step diceva `enabled = false`, `placeId` null): con il
  luogo a null la regola diventerebbe «il luogo attivo», e riaccenderla le farebbe cambiare
  soggetto in silenzio, proprio quel che la decisione della Fase 15 vieta; e un luogo riaggiunto,
  o rimesso dall'«Annulla» della rimozione, ritrova le sue regole legate a lui, spente finché il
  lettore non le riaccende. Il segno per la card è calcolato (luogo non fra i salvati), quindi è
  giusto anche prima che il job passi e anche se l'«Annulla» arriva prima di lui.
- *Spenta dal job, prima del cancello degli avvisi*, in una transazione dello store e non con
  `update`, che riscriverebbe una copia letta prima sopra una modifica fatta intanto dall'editor.
- *Un fallimento passeggero di un luogo legato chiede il riprovo* (lo step diceva «come le città
  dei widget», che aspettano il periodo): un widget vecchio di un'ora dice la sua età, una regola
  in ritardo di un'ora è un avviso arrivato tardi. Lo stesso criterio del luogo attivo: rete,
  servizio irraggiungibile, 5xx e 429 riprovano, un altro 4xx aspetta. Il riprovo rifà il giro del
  luogo attivo, che costa una lettura dalla cache entro i 15 minuti e un fetch dopo; le impronte e
  i latch tengono ogni avviso a una volta.
- *La notifica nomina il luogo per cui la regola è letta* (`city.name`, prima
  `report.location.city`): sono lo stesso nome tranne quando la cache restituisce il report di
  un altro record della stessa cella di 1 km (la posizione accanto a un luogo salvato).
- *Le card non leggono sul luogo attivo una regola legata altrove* (`now` null, la card tace
  invece di indovinare): la lettura sulla cache del suo luogo è di 15.12. Oggi non cambia niente a
  schermo, nessuna regola ha un luogo.
- *Un widget su un'altra città si aggiorna anche quando il job non deve avvisare di niente*
  (bug trovato riscrivendo il worker, presente dalla Fase 8): il ritorno anticipato del cancello
  degli avvisi veniva prima del fetch delle città dei widget, e con le notifiche spente o negate,
  o nessun avviso acceso, la città di un widget diversa dal luogo attivo non era mai aggiornata dal
  job e il widget invecchiava fino al segno di dato vecchio. Ora il fetch delle città dei widget
  viene dopo il cancello; il richiamo dei promemoria del cielo resta dietro di esso, com'era.
  **Cambia il comportamento**: nel CHANGELOG, in Fixed.

**Lasciato a 15.12** (l'editor, la card, la notifica): il «Dove» nell'editor; la riga «A Bormio»
e «Adesso sì/no» sulla cache di quel luogo; «scattato l'ultima volta» letto dalla cronologia del
luogo della regola (oggi `lastFired` legge quella del luogo attivo); l'anteprima dell'editor sul
luogo della regola; la card orfana e cosa fa il suo interruttore; l'agenda di Oggi con le sole
regole del luogo in pagina; la voce Added del CHANGELOG, DESIGN §8.9 e VISION §5.4.

**Come è stato verificato.** `RulePlacesTest` (nuovo, `:core:domain`): le regole del luogo attivo,
gli altri luoghi nell'ordine della lista, la posizione attiva, l'orfana letta da nessuna parte e,
tolto l'ultimo luogo, ogni regola legata orfana. `RuleStoreTest`: una regola scritta a mano come
la scriveva una build di prima legge `place` null; il luogo sopravvive a un riavvio;
`switchOffOrphans` spegne solo le accese orfane, tiene il luogo, non tocca le altre e non scrive
quando non c'è niente. `WeatherSyncWorkerTest`, sui report registrati di Reykjavík e Sydney (due
luoghi che nessun bollettino gradua, così il passo delle allerte resta inerte; `:core:sync` legge
ora le risposte registrate dei test di `:core:data`): con Reykjavík attivo e Sydney salvato, la
regola senza luogo e quella legata a Reykjavík parlano con «Reykjavík», quella legata a Sydney con
«Sydney», un fetch per luogo, i latch per luogo, e quel che è scattato nella cronologia di
ciascuno; la regola legata a un luogo tolto si spegne, tiene il luogo e non è letta altrove;
un'orfana sola lascia andare il job nello stesso passaggio, senza fetch; un 503 su Sydney chiede il
riprovo e le regole di Reykjavík parlano lo stesso; un 400 aspetta; un job senza avvisi con un
widget su Sydney fetcha Sydney. `AlertsViewModelTest`, sulla Milano registrata del README: la
regola senza luogo è letta, quella legata a Torino e quella legata a Bormio (tolto) no, e solo la
seconda è segnata; `NotificationsPromisedTest`: una regola accesa il cui luogo non c'è più non è
una promessa. `RuleEngineTest` non cambia. Suite completa verde (`./gradlew test :app:testDebugUnitTest`, 2 788 test, 15 saltati: gli screenshot, che girano solo su richiesta); lint verde (`:app:lintDebug`). Nessuna vista cambia, nessuno
screenshot rigenerato; DESIGN e VISION restano a 15.12, che porta la superficie.

## 15.12 — La regola legata a un luogo: l'editor, la card, la notifica (committente, 29 ott 2026)

**Fatto.** Il lettore può dire dove vale un suo avviso, e la pagina dice dove e risponde da lì.
Con questo step la Fase 15 è chiusa: i dodici step sono fatti.

- **«Dove» nell'editor**, sotto «In che orari» e nella sua grammatica: due chip, «Il luogo
  attivo» e un luogo salvato (che dice «Un luogo salvato» finché non se ne sceglie uno, poi il suo
  nome), e la riga che dice cosa fa la scelta: «Segue il luogo che hai aperto, ora Milano.», «Vale
  sempre per Torino, qualunque luogo tu abbia aperto.». Il chip apre «Dove vale», un foglio nella
  grammatica di «Cosa guardare»: «Il luogo attivo» in testa con il luogo che vuol dire adesso, poi
  i luoghi salvati nell'ordine del lettore, con la regione, e la spunta su quello scelto. Il GPS non c'è.
  La sezione non si disegna quando non c'è niente da scegliere (nessun luogo salvato e la regola
  sul luogo attivo). La scelta passa da `AlertsViewModel.choosePlace`, che legge la regola dallo
  store (niente di non salvato dell'editor viene sovrascritto) e la riarma: `rearms` guarda ora
  anche il luogo, perché un latch archiviato sotto il vecchio luogo la terrebbe muta sotto il nuovo.
- **La card dice dove**, su una riga sua fra la frase e gli orari, solo quando la regola è legata:
  «A Torino», e «Ad Aosta» davanti a una a (la d eufonica, `RuleText.placeAt`; l'inglese dice «In»
  comunque). «Adesso sì/no» si legge **sulla cache di quel luogo**, nel suo fuso (`readElsewhere`),
  mai con un fetch: il job tiene fresco un luogo legato finché la regola è accesa. La riga porta
  l'età di quella previsione quando passa la soglia di stantio, «A Torino · aggiornato 3 ore fa»,
  perché il titolo della pagina dice quella del luogo attivo e non questa, o «nessuna previsione
  ancora» quando il telefono non ne ha. «Scattato l'ultima volta» si legge dalla cronologia del
  luogo della regola (`lastFired` per luogo), quella che il job annota dal 15.11.
- **La regola orfana** dice «Bormio non c'è più: scegli dove vale» in pieno inchiostro, l'icona
  sbiadita e l'interruttore spento (`canRing`, anche nei pochi minuti prima che il job la spenga
  davvero); il suo interruttore apre «Dove vale» invece di accendere qualcosa, e il luogo scelto la
  lega e la riaccende. Lo stesso nell'editor: un'orfana mostra la riga «Bormio non è più fra i tuoi
  luoghi: scegli dove vale.», nessun chip scelto, e la risposta in cima dice lo stesso invece di
  «nessuna previsione ancora per questo luogo», che manderebbe ad aspettare.
- **L'anteprima dell'editor** legge il luogo della regola (`preview`): la sua cache, e un fetch
  silenzioso quando manca o ha più del quarto d'ora del provider, alle condizioni del keeper (mai
  sotto il risparmio batteria se c'è già qualcosa, `RefreshBackoff` fra un errore e l'altro, perché
  l'editor chiede ogni minuto); un fetch arrivato fa rileggere subito le card (`boundFetched`).
- **Oggi** mette fra le righe della giornata solo le regole lette sul luogo in pagina: quelle senza
  luogo e quelle legate a lui (`TodayUiState.build`, filtro `RulePlaces.readsActive`). Lo stesso
  vale per i widget, che costruiscono per la loro città.
- **La notifica** porta già il nome del luogo nel titolo («Neve · Bormio») dal 15.11.
- **La guida**: il paragrafo «I tuoi» dice il dove e cosa succede se il luogo si toglie; «Quanto
  costa» dice che un avviso acceso su un altro luogo aggiunge allo stesso giro l'aggiornamento di
  quel luogo (prima diceva «Niente in più», non più vero per intero).

**Il peso in chiamate.** Nessuna richiesta nuova oltre a quella del 15.11 (8,4 chiamate per luogo
legato per periodo, solo con la regola accesa). In più, l'editor: al più un fetch del luogo
legato ogni quarto d'ora mentre l'editor di quella regola è aperto, e nessuno se la cache è più
giovane; di solito uno, il giorno in cui si sceglie un luogo che il telefono non ha mai visto. Le
card non chiedono mai niente.

**Decisioni e deviazioni.**

- *Scegliere un luogo per un'orfana la riaccende*, dall'interruttore e dall'editor: il job l'ha
  spenta per il luogo, e il luogo è proprio quel che il lettore ha appena ridato. Una regola
  spenta dal lettore e legata altrove resta spenta (il test lo dice). Il caso raro di una regola
  spenta dal lettore *e poi* rimasta orfana si riaccende alla scelta: la card lo mostra subito.
- *La riga del luogo porta lo stato della previsione di quel luogo* (età, nessuna previsione):
  lo step diceva «nessuna previsione per Bormio»; detto sulla riga del luogo, in coda, è una riga
  sola per tutto quel che riguarda il dove.
- *L'editor fa il fetch, le card no*: lo step diceva che «Adesso sì/no» legge la cache. Le card
  lo fanno; l'editor, dove qualcuno sta chiedendo di un luogo appena scelto, avrebbe detto
  «nessuna previsione» fino al passaggio del job, anche un'ora.
- *Il luogo legato al luogo attivo dice comunque «A Milano»*: è fisso, e non seguirà il lettore
  quando cambia luogo; la riga lo distingue da una regola che segue.
- *I tipi del view model*: `RuleCardModel.place: RuleCardPlace?` (`Saved` con età e assenza di
  previsione, `Gone`) al posto del `placeGone: Boolean` del 15.11, che diventa derivato.
  `AlertsUiState.Content.savedPlaces` porta la lista per l'editor.

**Come è stato verificato.** `AlertsViewModelTest`: una regola legata a Torino si legge sulla
cache di Torino (la Milano registrata del README, mappata per Torino), dice «Torino» e legge
«scattato l'ultima volta» dalla cronologia di Torino; una legata a Genova senza cache dice
«nessuna previsione»; l'orfana è segnata e non letta; la previsione di Torino vecchia di tre ore
porta l'età; l'anteprima di una regola su Genova fa il fetch e la cache resta; un luogo non più
salvato non ha anteprima; `choosePlace` lega, riarma, riaccende un'orfana e lascia spenta una
regola spenta dal lettore; `rearms` guarda il luogo per id, non il nome. `AlertsPlaceTest`
(nuovo, `testDebug`): la riga del luogo solo sulla regola legata, l'età e «nessuna previsione»
sulla riga, l'orfana con la sua frase, l'interruttore spento che apre «Dove vale» e il luogo
scelto passato a `choosePlace`. `RulePlaceTextTest` (nuovo): «A Bormio», «Ad Aosta», «Ad Àrbatax»,
«A Empoli», e «In» in inglese. `TodayStateBuilderTest`: una regola legata a un altro luogo non è
una riga di questa giornata, una legata a questo sì. Rese a pagina intera in italiano, chiaro e
scuro, 384 dp: le card dei cinque stati e la sezione «Dove» nei tre (la sezione resa fuori dal
foglio, che la resa non cattura; la classe di prova è stata tolta). Suite completa verde (`./gradlew test :app:testDebugUnitTest`, 2 803 test, 15 saltati: gli screenshot, che girano solo su richiesta; il primo giro aveva perso un test al flake noto di DataStore su Windows, un file temporaneo che non si lascia rinominare, e il test di `choosePlace` aspetta ora che ogni sua scrittura sia arrivata); lint verde (`:app:lintDebug`). Gli screenshot del
README rigenerati: tutti identici byte per byte, `alerts.png` e `alerts-yours.png` compresi (le
regole di Milano restano senza luogo fisso). DESIGN §8.9 («Dove») e §8.9b (la riga del luogo),
VISION §5.4, la guida, CHANGELOG `[Unreleased]` (Added).
