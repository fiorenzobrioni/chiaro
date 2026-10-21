# PLANNING, archivio 04: 21 settembre - 1 ottobre 2026: le review grafiche, il motore degli stati, la 1.1.0

> Congelato il 21 ott 2026: un periodo chiuso del registro, spostato qui da `PLANNING.md`
> senza cambiarne una riga. Non si modifica. Il piano vivo è [`PLANNING.md`](../../PLANNING.md);
> l'indice di tutti i titoli è [`README.md`](README.md).

---

## Il backup diventa una decisione, e la riga della privacy dice quel che succede (committente, 21 set 2026)

Dal lint della vigilia: `DataExtractionRules`, un avviso solo, che chiedeva perché
`allowBackup="true"` stesse lì senza regole accanto. La risposta era «perché è il
default», che in questo repo non è una risposta.

### Perché non `allowBackup="false"`

Sembrava l'opzione pulita e non lo è. La documentazione Android dice che per le app che
targettano API 31 e oltre — noi siamo a 36 — quel valore «su dispositivi di alcuni
produttori disabilita il backup cloud **ma non disabilita i trasferimenti
dispositivo-a-dispositivo**». Un'istruzione il cui effetto dipende da chi ha fatto il
telefono non è una decisione: sarebbe stata l'unica riga non deterministica del progetto.

L'altra cosa che cambia il quadro, e che va detta perché ridimensiona il problema: il
backup cloud è **cifrato end-to-end col PIN del dispositivo da Android 9**. Google tiene
un blob che non può leggere. Con minSdk 33 vale sempre.

### La regola, e il motivo che non è la privacy

`res/xml/data_extraction_rules.xml`, una allowlist di una riga per ciascuno dei due
flussi: solo `files/datastore`, che è dove scrivono tutti i `preferencesDataStore` di
`:core:data` — `cities` (i luoghi salvati), `settings`, `rules`, `sky`, `sky_alerts`, i
quattro store dei widget e il resto. Quel che non è nominato resta fuori, e quel che
resta fuori è il database Room.

Il Room esce **prima di tutto per correttezza**. Dentro ci sono le previsioni in cache e
il Diario: roba con un timestamp sopra. Ripristinarlo su un telefono configurato una
settimana dopo consegna all'app ore già passate, che `WeatherRecency` butta e
`WeatherFreshness` marca stale a vista: peso trasportato per scartarlo al primo avvio.
Poi, sì, c'è anche l'altra faccia: `weather_history` è una riga per fetch per luogo, con
la data, e letta di fila è il registro di quali posti il lettore ha guardato e quando. È
la cosa che un backup ha meno motivo di copiare, e non vale niente dall'altra parte.

Due flussi identici di proposito: `<cloud-backup>` e `<device-transfer>` si configurano
indipendentemente, e non c'è ragione di concedere a uno quel che si nega all'altro. Chi
cambia telefono si ritrova luoghi, avvisi e impostazioni; nessuno si ritrova la cache.

### La riga della privacy diceva cinque parole

«Nessun account, nessuna pubblicità, nessun tracciamento.» Vero, e muto sulla sola
domanda che uno va a cercare in quella riga: un'app che chiede la posizione, la posizione
dove la manda? La guida lo diceva già, e bene (`guide_places_gps_body`,
`guide_data_p1`): era Impostazioni a essere l'unico posto dove non si diceva. Adesso la
riga porta i tre fatti — una previsione deve pur essere di un posto, quindi il posto va a
Open-Meteo arrotondato al chilometro e senza account né identificativo del dispositivo
attaccato; non esce altro; e il backup porta luoghi e impostazioni ma mai il diario.

Nella stessa passata una precisazione in `guide_data_p1`, che diceva «la richiesta del
meteo è l'unica cosa che esce dal telefono». Con la regola di backup appena scritta non è
più esatto alla lettera: un backup di sistema esce dal telefono, solo che a mandarlo è
Android e non Chiaro. Adesso dice «l'unica cosa che **Chiaro** manda da qualche parte»,
che è la stessa promessa detta con precisione.

**Il posizionamento privacy-first resta**, ed è la risposta alla domanda del committente.
Non vuol dire «niente esce dal dispositivo»: vuol dire che dove una scelta c'era è stata
presa dal lato privato, e le scelte c'erano sette volte (account, analytics, advertising
id, crash reporting, posizione in background, fine invece di coarse, obbligatoria invece
di opzionale). Sulla coordinata verso Open-Meteo una scelta non c'era: una previsione per
«qui» richiede di dire dov'è «qui». Quel che si poteva fare era minimizzarla, ed è fatto.

### La revisione di «Informazioni»

Il blocco è completo e non mancava niente: versione (da `BuildConfig`, quindi 1.0.0 senza
altro da toccare), sviluppatore, copyright, licenza, Open-Meteo, Protezione Civile,
Meteocons, i caratteri con «in uso: X», le icone di interfaccia, il codice sorgente, la
privacy. Undici righe, ognuna col suo link. Due cose però non tornavano:

- **Il link di Meteocons puntava a `basmilius/weather-icons`**, mentre il README, i tre
  tool e il file di licenza usano tutti `basmilius/meteocons`. Funziona, ma solo per il
  redirect che GitHub tiene sui repo rinominati: un giorno che qualcuno registra quel
  nome, l'attribuzione di una licenza punta altrove. Portato allo slug canonico.
- **`licenses/README.md` era fermo a due giri fa.** Dava Meteocons **v2.0.0** convertito
  in `res/drawable/mc_*.xml` da `tools/import_meteocons.py`: di `mc_*` nel repo ce ne sono
  **zero**, di `mc3_*` **519**, e il file di licenza accanto dichiara già
  `@meteocons/svg 3.0.0-next.10`. La tabella contraddiceva il file che le sta a fianco.
  Corretta, e aggiunta la riga che mancava del tutto: la **geometria delle 187 zone di
  allerta** (`warning_zones_it.json`, 284 KB, CC BY 4.0), che è dato di terzi che viaggia
  dentro l'APK, cioè esattamente quel che quella tabella dichiara di elencare. Corretta
  anche la chiusa, che diceva che l'attribuzione «appartiene alla guida»: sta in
  Impostazioni → Informazioni, una riga per ciascuna, con il link.

`./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` verdi,
**1661 test**, lint a zero errori e l'avviso `DataExtractionRules` sparito. Verificato
sull'APK vero: `versionName="1.0.0"`, `dataExtractionRules` presente, e l'XML delle regole
dentro il pacchetto con le due allowlist come scritte.

## «In parole» su una riga: la riga dell'aggiornamento tagliata in basso (committente, 22 set 2026)

Segnalato con una schermata home: sulla card 4×1 con la frase, la riga «Aggiornato 2 ore fa»
è disegnata e poi **tagliata sotto la linea di base** — le pance delle due «g» non ci sono, e
sotto il taglio restano ancora quattro o cinque dp di card azzurra. La richiesta era esplicita:
sistemarla **senza toccare nient'altro**, né posizione né dimensione del testo e dell'eventuale
glifo.

**La causa è che il budget di questa forma spende tutta la card.** `textRowHeroSp` dà al numero
**tutto quello che resta** dopo la riga del luogo e quella del marcatore, quindi la colonna di
sinistra misura *esattamente* `altezza − 6 × 2` a ogni misura e a ogni scala del carattere: zero
gioco, per costruzione. E `textLineHeight` è una stima — 1,32 em è la scatola di Roboto — che il
file che la definisce dichiara tale, aggiungendo che i budget che ci si appoggiano «tengono una
banda di gioco (`textInkBalance`) che assorbe un carattere la cui scatola è di qualche centesimo
più alta». Questo non ne teneva nessuna. Su un telefono il cui carattere di sistema sta
sui ~1,37 em la colonna vuole ~3 dp più di quanti la card ne conceda, il `LinearLayout` verticale
in cui si compila misura l'ultimo figlio con quel che avanza, e un `TextView` misurato sotto la
propria altezza di riga disegna la riga e la taglia. **L'ultimo figlio è il marcatore**, che è
anche l'unica riga che un budget non ha mai il diritto di lasciar cadere (VISION §5.9): ecco
perché l'errore si vedeva solo lì.

Misurato sulla schermata: card 940 × 235 px su un 1080 × 2340, cioè la card di riferimento di
340 × 85 dp; la base della riga cade a ~381 px e il taglio a 383, cioè esattamente sulla linea di
base. Sopra scala 1,15 la stessa colonna sfora per una seconda ragione — il numero è già sul
proprio pavimento (`TextHeroFloor`) e non ha più niente con cui pagare il marcatore — e finisce
sulla stessa vittima.

**La correzione è che i due numeri non sono lo stesso numero.** Il budget continua a *spendere*
i 6 dp snelli su ciascun bordo, quindi ogni dimensione e ogni posizione della card restano quelle
di prima; la card smette di *riservarli* (`textCardPaddingVertical`), così il blocco può crescere
dentro la propria aria invece di tagliare l'ultima riga. E non si muove niente quando non serve,
perché una card a una riga **centra** quel che tiene — la `Row` di `TextForm.ROW` e la `Column` di
`TextForm.LINE` lo fanno entrambe — e un blocco centrato alto *h* sta a `(altezza − h) / 2`
qualunque sia il margine simmetrico intorno: su ogni misura in cui la stima regge la card disegna
al dp quello che disegnava prima, e dove non regge ha 12 dp (`TextRowHeadroom`, il ~16% del budget
della riga di riferimento) da sforare prima che qualcosa venga tagliato.

Una sola eccezione, ed è l'unica cosa in questa forma ancorata alla **card** invece che al blocco
di parole: il glifo del meteo della forma stretta, che sta in fondo a destra del riquadro. Tiene
adesso 6 dp suoi, scritti fuori dal modificatore di dimensione perché in Glance il padding di una
`Image` dimensionata si mangia il disegno invece di stargli sotto. Così resta al dp dov'era.

Le altre due forme non si toccano: stack e pannello appendono l'occhiello in alto e il blocco in
basso, quindi lì il margine è una posizione e non solo aria, e una card senza ancora un bollettino
non ha nemmeno una forma.

**Quel che la correzione non copre, a verbale.** A scala 1,3 la colonna di una riga vuole ~91 dp
su una card da 85: dodici dp di sforo non ci arrivano, e gli unici dp rimasti da prendere sono
quelli del numero, che è già sul pavimento dei 26 sp. Abbassare il pavimento cambierebbe la
dimensione della temperatura su ogni card che ci cade sopra — una decisione diversa da questa, e
non quella che era stata chiesta — quindi è scritta qui e pinnata da un test
(`the largest font on the shortest row is past what any inset can carry`) invece di essere presa
di nascosto.

Quattro test nuovi: che il budget di una riga spenda la card intera (il meccanismo, non un numero
di contorno), che il margine restituito porti una scatola di carattere del 4% più alta della
stima, che solo le due forme a una riga lo restituiscano, e il limite qui sopra.

`./gradlew test :app:testDebugUnitTest :app:lintDebug` verdi, **1669 test**, lint a zero errori.

## `SkyAlarmSchedulerTest`: il test che correva contro l'avvio dell'app (22 set 2026)

Emerso lavorando sul widget, e per questo vale la pena scriverlo: durante le prove della
correzione precedente la suite e' fallita **una volta** su `the alarm is inexact and wakes the
device`, con `Required value was null` — cioe' nessuna sveglia armata — e alla riesecuzione era
verde. Un messaggio che non dice niente, su un test che non c'entrava con quel che stavo
toccando: il candidato perfetto per essere archiviato come «flaky» e lasciato li'. La regola di
casa dice il contrario, e aveva ragione.

**La riproduzione prima della diagnosi.** In isolamento, 8 esecuzioni su 8 verdi; la suite
`:app` intera, 5 su 5 verdi. Poi ho strumentato le asserzioni perche' stampassero *perche'* non
c'era una sveglia (luogo attivo, iscrizioni, impostazioni, piano, identita' dei negozi) e ho
stressato la classe: **2 fallimenti su 30**, poi un terzo su 20. Circa il 6%, cioe' abbastanza
raro da passare inosservato in CI per settimane e abbastanza frequente da far perdere un
pomeriggio a qualcuno.

**Quel che la strumentazione ha detto**, e che nessuna rilettura del codice avrebbe provato:

```
city=Success(null)  ownCities=[Milan]  ownActive=Saved(Milan)
sameStore=false     locatorCities=[]   settings=skyEnabled=true default=30
```

Il negozio del test aveva Milano; quello che lo `ServiceLocator` restituiva era **un altro
oggetto**, reale e vuoto. E nella stessa chiamata a `overrideForTests` il negozio delle
impostazioni era rimasto quello del test: un solo blocco di assegnazioni, due esiti diversi.
Questo esclude la scrittura persa e indica una corsa su un singleton.

**La causa e' `ChiaroApplication.onCreate`.** Robolectric costruisce l'applicazione dichiarata
nel manifest **una volta per test**, e quel metodo lancia due coroutine su uno scope
`Dispatchers.Default` di cui il test non ha alcun appiglio. Tutt'e due toccano esattamente cio'
che questa classe misura:

- la prima chiama `SkyAlarmScheduler.reschedule`, la rete di sicurezza a ogni avvio di processo.
  Legge quel che il locator contiene in quell'istante — di norma i negozi veri e vuoti, perche'
  sta correndo contro la preparazione del test — non trova niente da armare e chiama
  `AlarmManager.cancel` sull'unico request code fisso: **la sveglia sparisce da sotto
  l'asserzione**;
- la seconda risolve `ServiceLocator.cityStore(app)` per il collettore dei widget. Quel getter e'
  un singleton pigro, quindi cadere **in mezzo** alle assegnazioni di `overrideForTests` costruisce
  il negozio vero e lo memorizza **sopra** il finto. E' esattamente la firma misurata.

**Nessuna delle due e' un difetto dell'app**: riarmare all'avvio del processo e seguire il luogo
attivo sono decisioni prese e documentate. Semplicemente non sono l'oggetto di questo test, e un
test unitario che installa i propri finti dentro un singleton di processo non puo' allo stesso
tempo correre contro il processo che quel singleton lo legge.

**La correzione e' una riga, e sta solo nel test**: `@Config(application = Application::class)`.
Senza `ChiaroApplication` non c'e' nessuna coroutine di avvio, quindi niente corsa e niente
sveglia cancellata da sotto. Lo scheduler qui viene pilotato direttamente, quindi
un'`Application` nuda e' tutta l'impalcatura che gli serve.

Insieme, due asserzioni di precondizione in `setUpStores`: il luogo viene riletto **attraverso il
locator**, com'e' letto dallo scheduler. Se un giorno la preparazione non dovesse attecchire, il
test fallira' dicendo che non c'e' il luogo, invece di fallire tre righe dopo su una sveglia
mancante — che e' il modo in cui questa classe falliva, senza dire niente.

**La prova**: 60 esecuzioni consecutive dopo la correzione, zero fallimenti, contro i 3 su 50 di
prima (a quel tasso, 60 esecuzioni pulite per caso sarebbero circa il 2%).
`./gradlew test :app:testDebugUnitTest :app:lintDebug` verdi, **1669 test**, lint a zero errori.

---

## Navigation 3 e il back di Saldo (committente, 22 set 2026)

«Implementa Navigation 3 in Chiaro e fai le stesse animazioni che ci sono in Saldo (il back,
e mi sembra che in Saldo sia animato anche il cambio fra tab, verifica).»

È la seconda volta che la domanda arriva: il 18 set la dissolvenza di Saldo era stata rifatta
a mano (`ScreenSwap.kt`, un `AnimatedContent` su un `SeekableTransitionState`) e ritirata lo
stesso giorno (sezione «L'intestazione di Oggi che non scorre via»). Questa volta non si imita
Saldo: si adotta **lo stesso meccanismo**, Navigation 3, con le stesse transizioni.

### La verifica: in Saldo il cambio tab è animato, sì

Letto nel sorgente di `navigation3-ui` 1.1.4 (la versione di Saldo), non a memoria.
`NavDisplay` riceve la lista `[pila della Dashboard] + [pila del tab scelto]` e decide la
direzione con `isPop(vecchia, nuova)`: è un pop solo se la nuova lista è un prefisso stretto
della vecchia. Quindi:

- Dashboard → un altro tab: `[D]` → `[D, S]`, **push** (la pagina entra da destra);
- un tab → Dashboard: `[D, S]` → `[D]`, **pop** (la Dashboard rientra da sinistra);
- fra due tab che non sono la Dashboard: `[D, S]` → `[D, M]`, stessa lunghezza ma divergente,
  **push**.

Il cambio tab usa quindi la stessa transizione di ogni altra pagina, e in Chiaro lo fa allo
stesso modo, con Oggi al posto della Dashboard.

### Cosa è cambiato

- **`ui/shell/ChiaroNavigation.kt`** (nuovo): le chiavi `NavKey` serializzabili (i quattro tab,
  `SettingsKey`, `GuideKey`, `SkyGuideKey`, `SkyEventKey`) e `ChiaroNavigationState`, la ricetta
  «multiple back stacks» con la forma di `SaldoNavigationState`: una pila per tab, sullo schermo
  `[pila di Oggi] + [pila del tab scelto]`, back dalla radice di un tab porta a Oggi, back dalla
  radice di Oggi è del sistema. Lo stato salvabile di ogni pila vive in un decoratore suo, quindi
  un tab ritrova scroll e pagina aperta quando ci si torna (prima lo scroll di Oggi si perdeva a
  ogni cambio tab: il ramo del `when` usciva dalla composizione e il suo `rememberSaveable` con
  lui).
- **`ChiaroRoot.kt`**: `MainScreens` diventa un `NavDisplay`. Via i due enum di stato
  (`ShellOverlay`, `guideFromSettings`) e il `BackHandler`: la guida torna dalla porta da cui è
  entrata perché la porta è la pagina sotto di lei nella pila. La barra in basso è disegnata
  **sopra** il display, come in Saldo, e scivola via sotto Impostazioni e la guida; le pagine
  dei tab lasciano libera in basso l'altezza della barra (80 dp + l'inset di sistema), cioè
  esattamente l'area che il vecchio `Column` dava loro, con un `padding` semplice e non un
  modificatore di inset, così nessuna pagina vede cambiare l'inset che leggeva.
- **La guida degli eventi del cielo** (`SkyGuideScreen.kt`) diventa due route, `SkyGuideIndexRoute`
  e `SkyEventRoute`: prima era un `openId` dentro un composable con un suo `BackHandler`, e il
  back da una pagina all'indice era un cambio secco. Ora è un pop predittivo come gli altri. Un
  evento correlato aperto da una pagina **prende il posto** di quella pagina (`replaceTop`), che
  è la promessa di prima: back riporta all'indice da cui si era partiti. `SkyRoute` e
  `GuideRoute` perdono il loro stato «guida aperta» e ricevono una callback.
- **Le transizioni** sono quelle di Saldo, valore per valore: 300 ms, `FastOutSlowInEasing`,
  scorrimento di un sesto della larghezza più dissolvenza, in un senso per il push e nell'altro
  per il pop e per il back predittivo; la barra entra ed esce in verticale negli stessi 300 ms.
- **Manifest**: `enableOnBackInvokedCallback="true"`, per il gesto di sistema alla radice di Oggi
  su Android 14 e 15 (su 16 con targetSdk 36 è già acceso).
- **Un widget o una notifica** (`openFromOutside`) riporta ogni pila alla radice e seleziona il
  tab: è la regola di prima («il tocco da fuori chiude gli overlay»), estesa alla pila di Oggi
  che ora sta sotto ogni altro tab, altrimenti un back dal Cielo aperto da un widget avrebbe
  mostrato una pagina di Impostazioni lasciata aperta ore prima.

### Decisioni e deviazioni

- **Una tween e non una molla**, contro DESIGN §7 che vuole molle ovunque: il back predittivo
  *cerca* la transizione col dito (`seekTo`), e per mettere il progresso del gesto su una curva
  la curva deve avere una durata. È anche la richiesta letterale (le animazioni di Saldo). Il
  movimento ridotto la collassa ai 100 ms di dissolvenza come tutto il resto. Scritto in
  DESIGN §7.
- **Nessun decoratore dei ViewModel**, a differenza di Saldo: i ViewModel restano legati
  all'activity com'erano. I quattro tab condividono un solo `PlacesViewModel`, e un
  `ViewModelStore` per voce lo avrebbe diviso in quattro. Nessun cambiamento di comportamento
  per i dati.
- **Il `BackHandler` del catalogo nel Cielo resta**: vive dentro un `ModalBottomSheet`, che ha la
  sua finestra e il suo back; non è navigazione dello shell.
- **Dipendenze**: `navigation3-runtime`/`-ui` 1.1.4 chiedono activity 1.12, lifecycle 2.10 e
  Compose 1.10, quindi il catalogo le dichiara a quel che si risolve (BOM Compose `2026.02.01`,
  activity `1.12.4`, lifecycle `2.10.0`: lo stesso insieme di Saldo) invece di lasciarle salire
  in silenzio sotto un numero vecchio. Material 3 1.4, che arriva con il BOM, non porta più le
  icone Material con sé: `material-icons-core` è ora dichiarato, alla 1.7.8 che si risolveva già.
  Tolto `navigation-compose`, che era nel catalogo e nel build ma non era usato da nessun file.
  Plugin `kotlin.serialization` su `:app` per le chiavi. Un warning nuovo di Material 1.4
  (`confirmValueChange` deprecato in `PlacesSheet`) resta un warning: non cambia niente oggi.

### Quel che il salto di versione ha rotto, e trovato prima del dispositivo

- **Una famiglia tipografica sfuggita.** Material 3 1.4 aggiunge quindici ruoli «emphasized»
  (interni nell'API stabile) e `Typography().copy(...)`, che era il modo in cui `typographyFor`
  costruiva la scala, li lasciava con la famiglia di Material, cioè il sans di piattaforma.
  `TypographyFamilyTest` esiste esattamente per questo («un ruolo aggiunto da una release
  futura di Material fallisce qui invece di andare in APK») e lo ha preso. La scala ora si
  costruisce col costruttore pubblico, che deriva ogni ruolo enfatizzato da quello che riceve.
- **Due regole di lint nuove di Compose 1.10**, 26 errori su codice che non era cambiato:
  `NonObservableLocale` (24, `Locale.getDefault()` letto dentro un composable) e
  `LocalContextGetResourceValueCall` (2, `context.getString` in `ArcPreview`). Corrette nel
  codice come in Saldo, non soppresse: `currentLocale()` (`ui/format/CurrentLocale.kt`) legge
  `LocalConfiguration`, `glanceLocale()` (`widget/WidgetUi.kt`) la configurazione del contesto
  di Glance, che non ha `LocalConfiguration`; le due stringhe passano per `stringResource`.
  Stessa risposta di prima (con le lingue per app un cambio di lingua riavvia comunque
  l'activity), letta nel modo che Compose può osservare.

### Come è stato verificato

- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verde, **1689 test**
  (i 1669 di prima più i 10 nuovi, in debug e release), lint a zero errori;
  `:app:assembleRelease -PsignReleaseWithDebugKey` verde, quindi R8 passa con le chiavi
  serializzabili.
- `ChiaroNavigationStateTest` (nuovo, 10 test): la pagina su cui atterra ogni back, il tab in cui
  lascia, la guida che torna dalla sua porta, la barra coperta da Impostazioni e non dalla guida
  del cielo nel tab, l'evento correlato che rimpiazza la pagina, il tab che conserva la pagina
  aperta, il tocco da fuori che ripulisce le pile, e ogni chiave che fa andata e ritorno
  attraverso kotlinx.serialization (una chiave senza serializzatore cadrebbe alla prima
  rotazione, non in compilazione).
- **Da fare sul dispositivo**, perché qui non c'è: il gesto che segue il dito (fra pagine, fra
  un tab e Oggi, alla radice di Oggi verso la home), la barra che scivola, e un giro sulle
  schermate per il passaggio a Material 3 1.4 / Compose 1.10.

## La pagina dell'evento dalla riga dell'agenda (committente, 23 set 2026)

Richiesta: «per vedere le info degli eventi visualizzati bisogna andare in "Che cosa sono
questi eventi" o nella schermata per aggiungerli/rimuoverli e cercare l'evento. Sarebbe più
comodo avere accesso alle info direttamente dalla riga». Valutato prima di farlo, e condiviso:
la domanda «che cos'è» arriva sulla riga, e la maggior parte di quel che l'agenda mostra il
lettore non l'ha mai scelto dal catalogo — i momenti di default e soprattutto il calendario
«per tutti» (picchi degli sciami, perielio, equinozi) — quindi per quelle righe non c'era una
porta che non passasse dal sapere già il nome. E un tocco sulla riga, finora, non faceva niente.
Chiesto insieme: togliere un momento dalla stessa pagina.

### Cosa è cambiato

- **Ogni riga di «I prossimi momenti» e di «In arrivo» apre la pagina del suo evento**
  (`Modifier.agendaRowOpens`), l'intera riga compresa la linea del verdetto. La campanella
  resta un `IconButton` a sé e prende i suoi tocchi.
- **La pagina si apre in un foglio sopra l'agenda** (`AgendaPageSheet`), la stessa
  `SkyEventPage` del catalogo con lo stesso bottone (`CatalogAction`): «Togli dai miei
  momenti» se l'evento è seguito, «Aggiungi ai miei momenti» se no (una riga del calendario
  per tutti, o una pagina raggiunta da «Vedi anche»). Agire chiude il foglio: la ricevuta è
  l'agenda sotto, una riga sparita o una riga arrivata.
- **La guida di Impostazioni** (capitolo Cielo, «I prossimi momenti») dice che ogni riga porta
  alla pagina del suo evento e che è anche il posto da cui toglierlo. VISION §5.3: le porte
  della guida agli eventi diventano tre.

### Decisioni e deviazioni

- **Un foglio e non la pagina a tutto schermo della guida** (in discussione era stata proposta
  `SkyEventKey`, che esiste già). Il bottone che toglie ha bisogno di sapere se l'evento è
  seguito e di passare per `SkyViewModel`, che rimette la sveglia dopo ogni modifica: dentro
  la schermata ci sono entrambi, nella rotta della guida nessuno dei due, e un secondo
  view model solo per un bottone sarebbe una seconda copia della verità. Il foglio è anche la
  forma che la pagina ha già dal catalogo, per le stesse ragioni (la lista sotto resta dov'era).
- **Nessuna icona info, nessuna freccia.** La riga non ha 48 dp da dare: `SkyVerdictLine` è il
  conto di quanto costa quella colonna, e una seconda icona accanto alla campanella avrebbe
  rimesso a capo il verdetto a 360 dp. Il tocco sull'intera riga è il gesto che una lista
  suggerisce già; a TalkBack lo dice `onClickLabel` («leggere che cos'è»), perché l'increspatura
  lo dice solo all'occhio.
- **Il bottone si chiama come nel catalogo** («Togli dai miei momenti», non «Rimuovi
  dall'agenda» come l'aveva chiamato la discussione): un'azione, un nome, da qualunque porta.
- **Una riga con due sciami apre il primo**, che è il job della riga; l'altro è a un tocco nel
  «Vedi anche» (i due sciami che condividono il corpo progenitore si puntano già a vicenda).
- **«Vedi anche» rimpiazza la pagina** invece di impilarla, come nella guida a tutto schermo:
  indietro chiude sempre il foglio sull'agenda.
- **La card «Stanotte» non apre niente**: è un verdetto sulla notte, non un evento, e la
  finestra di buio ha già la sua riga fra i momenti.

### Come è stato verificato

- `AgendaPageSheetTest` (nuovo, Robolectric + Compose, primo test Compose dell'app) vive in
  `src/testDebug`: l'activity che la regola Compose lancia la dichiara `ui-test-manifest`, che è
  `debugImplementation` per non finire in un APK di release, e in `src/test` falliva nella
  variante release («Unable to resolve activity»). Controllato che morda: scambiando aggiungi e
  togli nel foglio, due test su tre diventano rossi.
  Cosa verifica: una pagina seguita toglie l'evento e
  chiude, una non seguita lo aggiunge e chiude, un id che il catalogo non ha più chiude invece
  di disegnare una pagina vuota.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verde, **1692 test**
  (i 1689 di prima più i 3 nuovi), lint a zero.
- **Da fare sul dispositivo**: il tocco sulle righe (anche sulla linea del chip), la campanella
  che resta un bersaglio a sé, TalkBack che annuncia l'azione.

## La schermata Oggi, review grafica: nastro, foglio sul cielo, grafico pioggia (committente, 23 set 2026)

Richiesta: una review completa della schermata principale, con cinque osservazioni del
committente (la linea della pioggia non sincronizzata con «Prossime ore», la stessa linea
animata all'apertura, lo stacco secco fra l'area della temperatura e il resto, le barre
colorate nei dettagli, le linee del giorno più sfumate) più le proposte della review. Scelto il
blocco 1–3 della proposta, con un mandato esplicito: **dove una scelta migliora la grafica e
viola DESIGN.md, si cambia DESIGN.md e vince l'estetica**.

### Cosa è cambiato

- **Il nastro della luce è luce, non una tabella** (`DaylightRibbon`, DESIGN §4). Un solo
  gradiente con estremi arrotondati al posto dei rettangoli a spigolo vivo, che sul dispositivo
  leggevano come un codice a barre. Ogni fase tiene il suo colore al centro e sfuma nella vicina
  sul bordo che condividono (il 30% della sua larghezza per lato, al massimo l'1,5% del giorno:
  `ribbonStops`). Sul canvas passa da 6 a 8 dp; l'«adesso» è un **disco bianco di 14 dp** con un
  anello dell'inchiostro dello scrim, e la parte di giornata già passata è al 60%.
- **Nelle righe della settimana la notte si ritira** verso la traccia della barra min/max
  (`nightFade`): niente dal crepuscolo civile in su, il 75% dalla notte astronomica in giù.
  Sette barre blu notte erano l'inchiostro più pesante della sezione, e dicevano la cosa che
  cambia meno; ora ogni riga mostra la sua pillola di luce.
- **La pagina è un foglio appoggiato sul cielo** (`SkySheet`, DESIGN §8.1). I primi 24 dp della
  pagina coprono il fondo del canvas con angoli di 28 dp: il cielo si vede attorno agli angoli e
  la fascia più scura dello scrim finisce sotto la carta. Il foglio **prende la luce** del cielo:
  i suoi primi 120 dp portano il colore del fondo del canvas al 16%, con una sfumatura a curva
  fino alla superficie — caldo al tramonto, azzurro a mezzogiorno, quasi niente di notte.
- **Il grafico pioggia è la mappa della fascia oraria** (DESIGN §8.3b). Tiene la vista delle
  24 ore e ci segna le ore che la fascia sopra ha in vista (una finestra `surfaceContainerHigh`
  che segue lo scroll); un tocco o un trascinamento sul grafico porta la fascia su quell'ora.
  `HourStrip` accetta il suo `LazyListState` da fuori per questo.
- **Il grafico si disegna** la prima volta che compare: linea e area rivelate da sinistra a
  destra in 900 ms, una volta per pagina, mai con il movimento ridotto.

### Decisioni e deviazioni

- **Mappa e non grafico «spalmato» sulle 24 celle.** Allungarlo sotto la fascia avrebbe dato
  l'allineamento ora per ora, e tolto l'unica cosa per cui il grafico c'è: la giornata intera in
  un colpo d'occhio (VISION §5.2.3). La finestra dà la sincronia senza perdere la panoramica, e
  il tocco la rende una scorciatoia invece che una decorazione.
- **La rivelazione va lungo il tempo, mai dal basso.** Una linea che cresce dallo zero mostra
  per mezzo secondo una giornata asciutta che nessuno ha previsto: §1.1 non ha eccezioni per le
  animazioni. Il flag è `rememberSaveable`, così scorrere via il grafico e tornarci non la
  ripete.
- **Il bordo dritto del canvas resta dritto.** Gli angoli tolti il 4 set facevano del cielo una
  card; qui il cielo resta il fondo e la card è la pagina. La linguetta è dentro il pavimento dei
  280 dp, così lo scheletro finisce ancora dove finisce il canvas.
- **Lo scrim del fondo arriva pieno dove comincia il foglio**, non sul bordo del canvas: il testo
  ha lo stesso scrim di prima, e §3.6 vale com'era.
- **Il bagliore è disegnato dall'item del canvas oltre i suoi bordi**, sotto gli item trasparenti
  che lo seguono. Si esaurisce entro l'altezza della riga del luogo fissata in alto, così quando
  l'item del canvas esce dalla lista la parte che se ne va con lui è già sotto la superficie di
  quella riga.
- **DESIGN §4 prometteva un pallino dal primo giorno**; il codice disegnava una riga di 2 dp. Il
  documento ora dice il disco che c'è.
- **Il gesto sul grafico non passa al pager fra i luoghi**: un trascinamento orizzontale sul
  grafico scorre la fascia, come già faceva il trascinamento sulla fascia stessa.

### Come è stato verificato

- `DaylightRibbonTest` (nuovo): stop monotoni anche con fasi sovrapposte, il nucleo pieno delle
  fasi lunghe, la dissolvenza della notte che lascia stare il giorno. `RainChartTest`: la
  finestra delle ore in vista (a riposo e a metà scroll, celle tagliate comprese) e l'ora sotto
  il dito, mai fuori dai due capi.
- Resa verificata con uno screenshot Robolectric temporaneo (grafica nativa, `View.draw` su una
  bitmap: `captureToImage` va in timeout sotto Robolectric) al tramonto chiaro, all'ora d'oro,
  a notte in tema scuro e a metà rivelazione; il test non è nel repo. Due ritocchi dopo averle
  guardate: la finestra da `surfaceContainerHighest` a `surfaceContainerHigh` (troppo pesante),
  il passato del nastro dal 50 al 60% (spento).
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verde, 8 test nuovi
  (481 in `:app` debug), lint a zero errori e nessun avviso sui file toccati. Maven Central ha
  risposto 429 a più riprese in questa sessione: i giri rossi erano download (compreso il jar di
  Robolectric per `:core:data`), rifatti finché verdi.
- **Da fare sul dispositivo**: il bagliore quando il canvas esce dalla lista (non deve vedersi il
  taglio), il trascinamento sul grafico dentro il pager, l'animazione alla prima apertura.

## La schermata Oggi, review grafica, blocchi 4–7 e il resto (committente, 23 set 2026)

Richiesta: «Fai il blocco 4-7. Così è tutto completato? Anche gli effetti sono suggeriti da
te?». Risposta data prima di cominciare: il 4–7 della tabella copriva tre dei quattro effetti
proposti (sole e luna, barra compatta, curva della temperatura) e lasciava fuori il pallino
«adesso» sulla barra di Oggi, il giorno a mezzanotte nella fascia e il filo con «tra N min»
nell'agenda. Fatti anche quelli, così tutta la review è implementata. Mandato invariato:
dove l'estetica e DESIGN.md litigano, si cambia DESIGN.md.

### Cosa è cambiato

- **La temperatura principale** (DESIGN §8.1c): gradi interi e «°» a 64sp, i decimi al 55%
  sulla stessa linea di base (`heroTemperatureText`); la condizione a `titleLarge`; «Percepiti»
  solo quando differisce di almeno 1°.
- **Le barre dei Dettagli** (§8.6, §2.3): ogni grandezza ha la sua scala e la sua tinta — UV
  caldo, umidità nell'acqua della pioggia, aria viola, pollini verdi, pressione divergente
  attorno a 1013 — disegnata intera al 40%, piena fino al valore, con le soglie come tagli e un
  disco sul valore. Nuove anche le barre di **pressione** e **pollini** (quattro gradini).
  Quattro rampe nuove in `ChiaroColors`, carta e vivid; le vivid generate da
  `tools/gen_vivid.py`, che ora le conosce.
- **Il sole e la luna nel cielo** (§8.1c, `SkyBodiesLayer`): nella loro posizione vera, di
  traverso per azimut visto guardando l'equatore e in su per altezza; la luna con la sua fase.
  Il canvas tiene per loro una fascia di almeno 56 dp fra la riga del luogo e la temperatura
  (`SkyColumn`).
- **La barra fissata in alto resta cielo** (§8.1b): scorrendo prende il colore del bordo alto
  del canvas sotto lo scrim invece della superficie, inchiostro bianco sempre; quando la
  temperatura è sparita porta icona e gradi, e la data si accorcia al giorno breve.
- **La curva della temperatura nella fascia oraria** (§8.3, `StripCurve`): ogni cella disegna
  il suo tratto di una curva continua, 2 dp per grado; la cella passa da 112 a 144 dp.
- **Mezzanotte ha un nome** nella fascia (il giorno breve in `primary` al posto di «00»).
- **Il pallino «adesso» sulla barra di Oggi** nella settimana (§8.5).
- **Il resto della giornata** ha un filo fra le icone e «tra N min» sotto la prima riga (§8.4).

### Decisioni e deviazioni

- **I decimi sulla linea di base, non in alto.** La prima resa li alzava all'altezza delle
  maiuscole: la virgola diventava un apostrofo («20’8»). Visto sullo screenshot e cambiato.
- **La fascia del cielo ha un minimo, e il canvas cresce per pagarlo.** Con la data del luogo
  e la condizione più grande restavano 14 dp fra la riga e l'eroe: nessuno spazio per un sole.
  `SkyColumn` misura l'eroe e garantisce 56 dp; il canvas si allunga di circa 25 dp sul
  telefono, di più quando c'è la frase. Il sole è l'effetto, e un effetto che non ha posto non
  c'è; lo scheletro resta sul pavimento dei 280 dp e quindi può essere più corto del canvas
  vero di quella fascia.
- **L'alone del sole può uscire di 14 dp dalla sua fascia**, non di più: a quella distanza è
  pochi punti percentuali di bianco sotto lo scrim, e non tocca il contrasto del testo.
- **UV caldo, non la scala OMS.** Verde-giallo-arancio-rosso-viola è un arcobaleno (§9.1) e le
  sue fasce si confondono sotto deuteranopia come quelle dei verdetti; la parola sotto il numero
  porta la fascia.
- **La barra non passa più alla superficie.** La decisione del 18 set (superficie e inchiostro
  del tema) era una risposta alle icone bianche sul contenuto chiaro; un fondo di cielo
  scrimmato risolve lo stesso problema senza il salto da bianco su cielo a nero su carta.
- **Nella barra compatta i gradi interi**, come ogni temperatura sotto l'eroe: con i decimi il
  nome del luogo perdeva metà del suo spazio.
- **La curva a scala fissa**, non adattata a minimo e massimo delle 24 ore (§9.1): comprime solo
  una giornata che non ci sta.
- **Il pallino di Oggi sulla scala della settimana**, anche fuori dal tratto min–max del giorno:
  una mattina più fredda della minima prevista sta onestamente a sinistra.

### Come è stato verificato

- Test nuovi: `SkyBodiesTest` (lati per emisfero, altezza, lato illuminato della luna, forma della
  falce; Robolectric per il `Path`), `StripCurveTest` (scala fissa, compressione, punto sulla
  curva), `HeroTemperatureTextTest` (solo i decimi sono piccoli, anche col segno meno), e in
  `PaletteContrastTest` / `PaletteDocTest` le quattro rampe nuove: monotone quelle sequenziali,
  col centro più chiaro in chiaro e più scuro in scuro quella della pressione, stampate in
  DESIGN §2.3 e §2.5 byte per byte.
- `tools/gen_vivid.py` rigenerato: i valori vivid già esistenti escono identici, le quattro rampe
  nuove sono quelle stampate.
- Resa della schermata **vera** (`ContentState` con uno stato costruito da `TodayStateBuilder`)
  con Robolectric: mattina, tramonto, notte in tema scuro, pagina scorsa con la barra compatta. Il
  test temporaneo non è nel repo. Tre ritocchi dopo averle guardate: i decimi (sopra), la fascia
  minima del cielo (sopra), la barra compatta che mandava a capo la data (ora giorno breve e gradi
  interi); in più le tracce a riposo dal 30 al 40% e il filo dell'agenda meno spezzato.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verde, 497 test in
  `:app` debug (16 nuovi), lint a zero errori e nessun avviso sui file toccati.
- **Da fare sul dispositivo**: la barra compatta durante uno scroll veloce, il sole all'alba e al
  tramonto (azimut agli estremi), la luna di giorno, la curva nella fascia aperta di un giorno
  della settimana.

## La schermata Cielo, review grafica (committente, 23 set 2026)

Richiesta: «fai un review anche della schermata Cielo sempre con lo stesso obiettivo di
migliorare e rendere anche wow in qualche aspetto la schermata. Ottimizza e migliora layout e
testi dove necessario». Stesso mandato della schermata Oggi: dove l'estetica e DESIGN.md
litigano, si cambia DESIGN.md.

### La review

Resa della schermata vera (Robolectric, pomeriggio sereno, sera mista con luna al 92%, notte di
luna nuova in tema scuro) prima di toccare niente. Tre cose:

1. **L'eroe non era notturno.** «Stanotte» era una card nel colore del verdetto con cinque righe
   di testo: corretta, e la cosa meno notturna di una schermata sulla notte. Il dato per
   disegnarla c'era tutto (la notte, la finestra, la luna, le nuvole ora per ora).
2. **«Domani ·» su ogni riga.** La sera tutte le righe dei momenti cominciavano con la stessa
   parola, e l'ora — quello che si cerca — finiva a metà riga.
3. **«In arrivo» ripeteva la stessa frase.** Cinque righe di sciami, ognuna chiusa da «La
   previsione non arriva ancora così lontano», ognuna andata a capo.

### Cosa è cambiato

- **`TonightCard` è la notte** (DESIGN §8.8b, `TonightSky.kt`): fondo della banda notturna del
  canvas sollevata dalla luna, verdetto con segno e parola nella coppia scura, la luna nella sua
  fase nell'angolo, e la **striscia della notte** — ore di luna argentate, nuvole ora per ora
  appese dall'alto, stelle dove è sereno, finestra incorniciata nell'inchiostro del verdetto con
  i suoi orari, tratto più sereno sottolineato.
- **`Tonight` porta le ore della notte** (`NightHour`, dalla previsione) e l'elongazione della
  luna; `SkyStateBuilder.nightHours` le ritaglia.
- **I momenti sotto «Oggi» e «Domani»**, senza la parola ripetuta sulle righe; il prossimo dice
  fra quanto.
- **Gli eventi contano i giorni** («tra 15 giorni»), e «troppo lontano» è una nota sola sotto la
  sezione.
- **`SkyContent` prende funzioni, non il view model** (`SkyActions`): la schermata si può
  disegnare in un test o in un'anteprima.
- `ChiaroTheme.nightColors`: la palette semantica scura nel vestito del lettore, per la card.

### Decisioni e deviazioni

- **Il verdetto nella coppia scura anche in tema chiaro.** La card è sempre notte, come il canvas
  (§3.2): i colori seguono il fondo, non il tema. È la stessa eccezione che il cielo ha sempre avuto.
- **La striscia non dice niente di nuovo**, di proposito: ogni segno è una frase della card, e le
  frasi restano. Per questo è muta per TalkBack.
- **Stelle fisse, non animate**: nessuna particella (§3.5); la loro luminosità segue le nuvole
  dell'ora e la luna, quindi anche le stelle sono il dato.
- **La luna nel badge non ha un'ombra disegnata a mano**: è lo stesso `moonLitPath` del canvas di
  Oggi, così le due lune non possono disegnare fasi diverse.
- **«Troppo lontano» resta detto**, una volta: le righe senza verdetto non portano un chip, e la
  nota sotto la sezione dice perché.

### Come è stato verificato

- `TonightNightTest` (nuovo): ore di luna lette dalla finestra contro la notte, frazione sulla
  striscia, nuvola dell'istante, ore della notte che includono quella a cavallo del crepuscolo.
- Resa Robolectric prima e dopo, sui tre scenari; dopo averla guardata: nuvole sfumate sotto (erano
  blocchi), striscia ritagliata sugli angoli arrotondati.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verde, 503 test in
  `:app` debug, lint a zero errori.
- **Da fare sul dispositivo**: la card in una notte di luna che sorge a metà (cornice e argento
  affiancati), il «tra N» sul prossimo momento, il conteggio dei giorni a cavallo di mezzanotte.

## La schermata Avvisi, review grafica (committente, 23 set 2026)

Richiesta: «Ora fai una review con gli stessi obiettivi anche per la schermata Avvisi». Stesso
mandato: dove l'estetica e DESIGN.md litigano, si cambia DESIGN.md.

### La review

Resa della schermata vera prima di toccarla (Robolectric: nessuna allerta con due regole, allerta
arancione senza regole, allerta gialla in tema scuro). Quel che mostrava:

1. **Un muro di testo.** Quattro interruttori «pronti» con descrizioni di tre e quattro righe, in
   cui il «quando» e il «quanto spesso» erano la coda della frase; nessun segno per distinguerli
   prima di leggerli.
2. **«Allerte ufficiali» due volte**: titolo del gruppo e titolo dell'interruttore, con la soglia
   «Avvisami da» come terza riga sciolta.
3. **L'assenza detta come una nota**: «Nessuna allerta per Nodo Idraulico di Milano» in
   `titleSmall` su un riquadro piccolo, in una schermata dove quella è la risposta che si cercava.
4. **Le idee come altre cinque righe** in fondo, indistinguibili da impostazioni.
5. **La guida era rimasta indietro**: «Qui vivono due specie» e «Tre interruttori», quando i gruppi
   sono tre e gli interruttori quattro (il riepilogo della sera, 21 set, non era mai entrato).

### Cosa è cambiato

- **«Quando arrivano»** (DESIGN §8.9b): le 24 ore con le finestre degli avvisi a orario, nel
  colore del cielo della loro ora, i loro disegni sopra, l'«adesso» e le ore sotto; e in parole
  quelli che arrivano a qualunque ora.
- **Gruppi arrotondati** per l'interruttore delle allerte (con la sua soglia dentro) e per i
  quattro pronti; ogni riga con il suo disegno (sbiadito quando è spenta), una frase su cosa manda
  e, a parte, nell'accento, quando e quanto spesso. Testi riscritti: `alert_*_desc` accorciati,
  `alert_*_when` e `warning_switch_when` nuovi, l'interruttore si chiama «Avvisami delle allerte».
- **Il «tutto tranquillo»** è una card con segno, «Nessuna allerta», la zona e l'ora del bollettino.
- **Le regole** hanno il disegno di quel che guardano; **le idee** sono card in una riga orizzontale.
- **La guida** dice tre gruppi e quattro interruttori; «Pronti» diventa «Pronti all'uso», come nella
  guida.
- `AlertsContent` prende funzioni (`AlertsActions`), non il view model, come Cielo.

### Decisioni e deviazioni

- **Le finestre sono disegnate, non gli avvisi «a qualunque ora»**: maltempo, pioggia e regole non
  hanno una finestra, e disegnarli su tutta la striscia sarebbe una barra piena che non dice niente.
  Sono nominati in una riga sotto.
- **Il giallo del bollettino è l'inchiostro in tema scuro**: il contenitore giallo scuro spariva sulla
  traccia. È la stessa coppia di §2.3, solo scelta per il fondo.
- **Nessun colore per l'assenza**: il segno del «tutto tranquillo» è in `primary`, non nel verde di un
  verdetto; il fondo resta neutro (§8.13).
- **Il chip «Arancione» andava a capo** dentro sé stesso al rientro del testo delle righe: la soglia è
  un `FlowRow` dal margine del gruppo.
- **`warning_card_none` tolta**: la frase con la zona dentro è diventata titolo e riga separati.

### Come è stato verificato

- Resa Robolectric prima e dopo, tre scenari, chiaro e scuro; dopo averla guardata: soglia in flow,
  disegno della sera distinto da quello del mattino (erano due orizzonti quasi uguali), descrizioni
  delle idee su quattro righe, giallo del bollettino in tema scuro.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verde, 503 test in
  `:app` debug (nessuno nuovo: il giro cambia disegno e testi, non logica), lint a zero errori.
- **Da fare sul dispositivo**: la striscia «Quando arrivano» con l'ora vera, il tocco sulle card
  delle idee e sul «tutto tranquillo», TalkBack sulla striscia.

## Il Diario, review grafica (committente, 23 set 2026)

Richiesta: «Passa a Diario. Questa è molto anonima come schermata, fai un bel review come hai fatto
per le altre». Stesso mandato: dove l'estetica e DESIGN.md litigano, si cambia DESIGN.md.

### La review

Resa della schermata vera (Robolectric, due giorni di voci di ogni categoria, deriva di sette giorni
con un gelo). Anonima davvero, e per ragioni precise:

1. **Tutte le voci uguali**: silhouette grigie da 24dp, testo grigio, nessuna gerarchia fra un'allerta
   arancione e un aggiornamento mancato.
2. **I numeri non dicevano dove sta la previsione adesso**: «pioggia 70% → 30%» tutto nello stesso
   inchiostro.
3. **La card della deriva teneva la risposta in fondo**: la frase che dice se la settimana migliora
   era l'ultima riga, sotto un grafico che non diceva da che parte si legge il tempo.
4. **Il cielo osservato era una frase** («Bello, nuvole 8%») dove il Cielo usa un chip.
5. **Il motivo di un aggiornamento fallito ripeteva il titolo**: «Un aggiornamento non è arrivato» /
   «Aggiornamento non riuscito: sei offline».

### Cosa è cambiato

- **Voci su un filo** (DESIGN §8.10): badge da 40dp nel tono della categoria o con il disegno meteo
  per il cielo e la giornata chiusa, filo fra i badge dello stesso giorno, testo a `bodyLarge`.
- **I numeri**: il valore di prima quieto, quello di adesso in grassetto (la pioggia sulla sua rampa
  d'inchiostro); stesse stringhe del «cosa è cambiato» di Oggi, con segnaposto.
- **Il verdetto del cielo osservato è il `VerdictChip`**.
- **Intestazioni dei giorni fisse** con la data intera accanto a «Oggi»/«Ieri» e il numero di voci.
- **Deriva**: frase in testa, chip con il disegno della grandezza, asse del tempo ai due capi, celle
  arrotondate, la riga di cui parla la frase evidenziata.
- **«Com'è andata la previsione»** (nuova): scarto medio delle massime, pioggia data nei giorni di
  pioggia contro gli altri, e una colonna per giorno con la pioggia data e il disegno di com'è andata.
- **Testi**: «Il telefono era offline» / «Il servizio meteo non ha risposto» / «Il motivo non è
  chiaro» per i fallimenti; il bollettino rimasto valido senza l'anno quando è di quest'anno.

### Decisioni e deviazioni

- **Colore per categoria, non per giudizio.** §8.10 voleva le silhouette tutte in `onSurfaceVariant`;
  il colore ora distingue la categoria (il glifo resta), e una revisione è dello stesso colore che sia
  migliorata o peggiorata: il giudizio sta nella frase, come la deriva ha sempre voluto.
- **Medie, non un punteggio.** Una probabilità non è giusta o sbagliata in un giorno solo; separare la
  pioggia data nei giorni bagnati da quella nei giorni asciutti è la misura onesta che due settimane di
  dati possono dare. Da tre giornate chiuse in su, le ultime dieci.
- **Scarto delle massime in °F convertito come differenza** (× 9/5, senza il +32).

### Come è stato verificato

- `OutcomeStatsTest` (nuovo): scarto medio in valore assoluto, medie separate per giorni di pioggia e
  asciutti, nessuna media «bagnata» in una settimana senza pioggia.
- Resa Robolectric prima e dopo, chiaro e scuro.
- Il test ha preso un dettaglio: `kotlin.math.round` arrotonda la metà al pari (70,5 → 70), mentre
  ogni percentuale dell'app arrotonda la metà in su; ora `roundToInt`.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verde, 506 test in `:app`
  debug, lint a zero errori e nessun avviso sui file toccati.
- **Da fare sul dispositivo**: le intestazioni fisse durante lo scroll, il tocco sulla deriva che apre
  la tabella, la card della previsione con dati veri di più giorni.

## Le Impostazioni, review grafica e la nota privacy (committente, 23 set 2026)

Richiesta: «Fai review anche della schermata Settings. Oltre ai soliti obiettivi volevo anche
cambiare la sezione privacy: mi sembra eccessivamente verbosa, vorrei una nota semplice e veritiera
della filosofia di privacy dell'app».

### La review

Resa della schermata (Robolectric, paper chiaro e vivid scuro). Corretta e piatta: righe Material
sotto intestazioni blu, la guida come prima riga fra le altre, undici righe di «Informazioni» di cui
la privacy era una, con un paragrafo di cinque righe.

### Cosa è cambiato (DESIGN §8.15)

- La guida è una card in testa; ogni gruppo sta su un fondo arrotondato come in Avvisi.
- **L'anteprima dell'aspetto**: una fetta di cielo nella palette scelta, all'ora d'oro, con temperatura
  nel carattere scelto, condizione nel set di icone scelto, e sotto un verdetto, una pioggia e una barra
  di temperatura nei colori della palette. Cambia a ogni scelta.
- **Privacy**: gruppo a sé, card con lucchetto, una riga («Niente account, niente pubblicità, niente
  tracciamento.») e tre fatti in due frasi.
- I riconoscimenti in un gruppo loro, «Dati e riconoscimenti»; il ripristino un bottone in fondo.
- `SettingsList` prende funzioni (`SettingsActions`) invece del view model.

### La nota privacy: cosa dice e perché

Verificato sul codice prima di scriverla: le uniche chiamate di rete sono Open-Meteo (meteo, aria,
ricerca dei luoghi) e il download del bollettino della Protezione Civile dal mirror su GitHub, che non
manda niente dell'utente; la posizione esce da `LocationProvider` già arrotondata a due decimali; non
c'è account, SDK di analytics né identificativo pubblicitario.

- **«Chiaro non ha un server suo»** è la frase che regge le altre: tutto quel che l'app tiene è sul
  telefono per costruzione, non per promessa.
- **Il backup è fuori dalla nota**, di proposito: lo fa Android se il lettore l'ha acceso, cifrato col
  suo PIN, e copre luoghi e impostazioni ma non il diario (21 set). Metterlo in una nota di due frasi la
  rendeva una nota sul backup; resta spiegato nella guida.
- **«Nessun identificativo», non «anonimo»**: ogni richiesta di rete porta un indirizzo IP, e una nota
  veritiera non promette più di quel che l'app controlla.
- **Il geocoder di Android** (che trova il nome del posto della posizione) resta nella guida, dove è già
  detto: è il sistema, non Chiaro, a decidere dove chiedere.

### Come è stato verificato

- Resa Robolectric prima e dopo; dopo averla guardata, l'anteprima spostata a 3° (a 5° il cielo era un
  marroncino) e la nota accorciata di una riga.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` verde, 506 test in `:app`
  debug, lint a zero errori (l'unico avviso sul file, `UseKtx` su `Uri.parse`, c'era già).
- **Da fare sul dispositivo**: l'anteprima mentre si cambiano palette, carattere e icone.

## Il widget «In parole» e le sue impostazioni, review grafica (committente, 23 set 2026)

Richiesta: «Ora fai una review con gli stessi obiettivi per il widget "In parole" e le relative
impostazioni. Tieni in considerazione il ridimensionamento e il re-layout».

### La review

Resa del widget con la composizione Glance vera (`GlanceRemoteViews.compose` → `RemoteViews.apply`
in Robolectric) su una matrice di concessioni del launcher: 2×1, 3×1, 4×1, 4×1 alta, 5×1, 2×2, 3×2,
4×2, 5×2, 2×3, 3×3, 4×3, fresca, vecchia, con allerta e con l'icona. Le quattro forme reggono e le
misure dei due passaggi sul dispositivo tengono; il difetto è in altezza: **da tre righe in su la
card non aveva niente da fare con lo spazio**. Il numero è al suo tetto (56 e 64sp, per ragioni già
scritte) e l'aria fra luogo e blocco cresceva e basta: un 3×3 era metà vuoto. La descrizione del
provider prometteva ancora «the next hours as figures under it», mai fatte.

La schermata delle impostazioni: una lista piatta di radio sotto etichette blu, senza vedere la
card, con due note sbagliate per questa card (l'allerta «come pastiglia», che qui è una parola; la
massima e minima «al bordo opposto»).

### Cosa è cambiato

- **«Più tardi»** (DESIGN §5): da tre righe, le prossime ore in righe di testo — ora, temperatura,
  pioggia se ≥ 30%, il cielo in una parola — ogni tre ore sull'orologio, dalla prima almeno 90
  minuti avanti. Pagata per ultima, a righe intere, da 2 a 4; il pannello tiene 16dp d'aria; la
  parola cade dove resterebbe sotto 84dp (2 celle). Interruttore per widget (`showLater`, acceso):
  cambia qualcosa solo dove c'era aria vuota.
- **`TextWidgetContent`** estratta da `provideGlance`: la stessa funzione disegna la card del
  launcher e l'anteprima.
- **Impostazioni** (DESIGN §8.16): anteprima vera con le dimensioni in chip e una riga per forma
  più il suggerimento di ridimensionare; gruppi arrotondati come in Impostazioni; sfondi con il
  loro campione, colori in una striscia di campioni; opacità in una riga; «Più tardi» e l'icona in
  fondo ai contenuti, la famiglia di icone subito dopo.
- Testi: nota dell'icona accorciata; note dedicate per allerta e massima/minima sulla card
  testuale; descrizione del selettore che dice le prossime ore; commento del provider corretto.

### Decisioni e deviazioni

- **Parole e numeri, non glifi, nella tabella.** Il widget «Le prossime ore» ha già la striscia
  disegnata; qui la stessa informazione è detta: è la premessa della card.
- **Ore sul passo dell'orologio (18, 21, 00), non «fra 3, 6, 9 ore»**: si leggono ad alta voce. Il
  passo è sull'istante, non sull'etichetta, così la notte del cambio d'ora non sposta una riga.
- **Descrizione del cielo per ora, non per fascia («Stasera»)**: una fascia vuole un'aggregazione
  dei codici che il mapper fa solo per il giorno, con regole misurate (Fase 13b/26); rifarla qui
  in piccolo sarebbe stato un modo nuovo di sbagliare il cielo. L'ora ha il suo codice già
  riparato.
- **L'anteprima è la composizione vera, non un rifacimento in Compose** come quella dell'Arco:
  `GlanceRemoteViews` (API sperimentale di Glance 1.1) compone per qualsiasi dimensione. Se
  un giorno smettesse di funzionare, l'anteprima semplicemente non si disegna (`runCatching`).
- **`BackgroundSection` resta una sola** per i due schermi (`WidgetConfigChoicesTest`): la nuova
  versione con i campioni serve anche l'Arco, che mantiene il suo impianto piatto.

### Come è stato verificato

- Resa Robolectric del widget (matrice sopra) e della schermata impostazioni, chiara e scura, prima
  e dopo; dopo averla guardata: la parola della tabella tolta sotto 84dp (stampava «Poc…») e le
  colonne ristrette di pochi dp perché a 3 celle, con pioggia e icona, la parola resti.
- `TextLaterTest`, `LaterHoursTest`: nessuna tabella sulle card di riferimento a una e due righe,
  almeno due righe su tutte quelle a tre, il resto del piano identico con l'interruttore acceso o
  spento, la tabella dentro la card, righe intere, ore sull'orologio, un'ora mancante è una riga
  mancante.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- **Da fare sul dispositivo**: l'anteprima nella schermata del launcher (tocco inghiottito,
  angoli), la card a 3×3 e 4×3 sulla schermata Home.

## Gli altri widget e le loro impostazioni, review grafica (committente, 23 set 2026)

Richiesta: «Fai una review anche degli altri widget e uniforma le relative schermate di
impostazione come questa appena fatta».

### La review

Ogni ricevitore ha ora la sua `*WidgetContent` (come «In parole»), e con quella la resa vera di
Adesso, Le prossime ore, Momenti del cielo e Arco a 13 concessioni, da 1×1 a 4×4. Cosa è emerso:

- **Adesso**, forma alta: da tre righe l'icona si ferma a 104dp e il numero resta a 34sp; un 4×3
  era un campo blu con un'icona in un angolo e tre righe in fondo.
- **Le prossime ore**: da tre righe l'icona cresceva fino a 104dp nella riga accanto alle parole e
  la frase, stretta a 99dp, diventava «Ombrello / verso le 20:…» con cento dp vuoti sotto. Anche
  a 4×2 senza pioggia l'icona era 91dp.
- **Momenti del cielo** e **Arco** usano già l'altezza (più momenti, più agenda, la settimana):
  invariati. Il blocco del Cielo centrato nelle card alte è una scelta del committente (4 set).
- Le forme sotto i minimi dei provider (Adesso 1×1, Le prossime ore a una riga, Cielo 2×1) sono
  irraggiungibili e non sono state toccate.

### Cosa è cambiato

- **Adesso**: `nowTallTemperatureSp` — il numero prende quello che l'icona non può usare, da 34
  a 56sp, con il limite di larghezza. Il 2×2 di riferimento non cambia.
- **Le prossime ore**: tetto dell'icona a 80dp (`TodayHeroIconMax`); **i prossimi giorni** sotto
  le ore da tre righe (`todayShowDays`), stessa griglia delle ore, oggi per primo; interruttore
  «I prossimi giorni» (lo stesso campo `showLater` di «Più tardi»).
- **Impostazioni**: `WidgetConfigKit.kt` con anteprima vera per tutti i tipi, chip per tipo
  (`previewSizes`, legati ai minimi dei provider), riga per forma (`formNote`), gruppi, righe,
  opacità, «Fatto». La schermata dell'Arco rifatta con gli stessi gruppi e l'anteprima vera;
  `ArcPreview.kt` (536 righe di copia in Compose) eliminato, come `TextWidgetPreview.kt`,
  assorbito nel kit.
- Testi: note dell'allerta accorciate (Adesso/Le prossime ore, Arco), «Ripristina questo widget»,
  una riga per forma per ogni widget.

### Decisioni e deviazioni

- **80dp e non 76** per l'icona di Le prossime ore: 76 è il 4×2 con la pioggia e non si muove;
  80 è il blocco di parole accanto (74dp) più il margine proprio del disegno. Il test che fissava
  90,68 e 104 ora fissa 80, con la ragione.
- **I giorni partono da oggi**, contro la prima idea (da domani, perché la massima e minima di oggi
  sono dell'eroe): con sei giorni sotto sette ore le due righe non erano più una griglia.
- **Un interruttore per due cose diverse** (`showLater`): è «il rango in più di una card alta» su
  entrambe le card, e ogni schermata lo nomina per quello che fa lì.
- **Nessun cambio a Cielo e Arco**, oltre alla schermata: la resa non ha trovato difetti che
  non fossero scelte già registrate.

### Come è stato verificato

- Resa Robolectric dei quattro widget (13 dimensioni ciascuno) prima e dopo, e delle due schermate
  (chiara e scura) con l'anteprima di ogni tipo.
- `NowWidgetLayoutTest` (il numero cresce solo col surplus, l'icona resta al tetto, limite di
  larghezza), `TodayWidgetLayoutTest` (tetto 80, giorni assenti a due righe e presenti a tre, tutto
  dentro la card), `WidgetPreviewSizesTest` (ogni chip rispetta i minimi del provider).
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- **Da fare sul dispositivo**: le anteprime nelle due schermate, Adesso e Le prossime ore a 3×3 e
  4×3 sulla Home.

## L'icona dell'app, ridisegnata (committente, 23 set 2026)

Richiesta: «prova a ridisegnare da zero l'icona dell'app. Prima di renderla definitiva fammi un
preview che la valuto»; scelta: «Vai con la D che è anche la tua consigliata».

### Il percorso

- **Diagnosi**: l'icona era una falce stellata sopra due onde, cioè la notte, per un'app che si
  chiama Chiaro ed è l'edizione diurna; a 36px le stelle erano puntini.
- **Candidati disegnati come veri vector drawable** (sfondo, primo piano, monocromo) e resi da
  Robolectric, poi mascherati (cerchio, squircle, quadrato arrotondato), a tema chiaro e scuro, a
  64/48/36px su sfondo chiaro e scuro. Sei tentativi, due scartati da me prima dell'anteprima:
  il tramonto sul mare (generico) e il nastro a pillola (in monocromo un interruttore); la prima
  curva del sole era una cupola, la seconda una «A».
- **Scelta: D, «il giorno in un anello»** (DESIGN §3.8).

### Cosa è cambiato

- `ic_launcher_foreground.xml` ridisegnato; nuovo `ic_launcher_monochrome.xml` per le icone a
  tema (prima il monocromo riusava il primo piano); fondo `#F7F4EE`.
- Il rendering finale coincide pixel per pixel con l'anteprima approvata.

### Da fare sul dispositivo

- L'icona sulla Home con due o tre launcher (maschere diverse) e a tema; la schermata di avvio di
  Android 12+, che usa l'icona sul fondo della finestra.

## Le notifiche, review dei messaggi e grafica nelle espanse (committente, 23 set 2026)

Richiesta: «Fai una review finale anche dei messaggi delle notifiche (nelle due varianti chiuse
ed espanse). … se pensi sia un'aggiunta che dà valore all'app e se è possibile, verifica se
aggiungere anche della grafica nei messaggi delle notifiche estese (solo dove veramente aggiunge
valore)».

### La review

Resa delle sette notifiche con i template del sistema (Robolectric: `recoverBuilder` →
`createContentView`/`createBigContentView` → `apply`), chiare e scure. I testi reggono; tre
difetti veri:

- il **riepilogo del mattino** diceva quanto è probabile la pioggia e mai quando;
- la **notte** del riepilogo serale era «Stanotte fino a 9°», che si legge male;
- l'**avviso personale** stampava «— valore 21.4»: numero nudo, punto decimale del codice, e la
  condizione in minuscolo come riga a sé.

### Cosa è cambiato (DESIGN §8.17)

- **Grafica** nelle espanse di: pioggia e maltempo (le prossime 12 ore di pioggia, la finestra
  dell'avviso illuminata), riepiloghi (la giornata: temperatura come curva sulla scala del mondo,
  pioggia sotto, notte in tinta), allerta (la griglia dei livelli). Niente grafica per il cielo
  e per gli avvisi personali: non aggiungerebbe niente.
- Riga «Pioggia dalle … alle …, fino a …%» nel riepilogo del mattino, subito dopo «Adesso».
- «Stanotte minima …», lettura degli avvisi personali con unità e virgola, frase maiuscola.
- Colore d'accento del marchio e icona di stato ridisegnata come l'anello dell'icona.

### Decisioni e deviazioni

- **Vista espansa personalizzata + BigTextStyle insieme**: la vista personalizzata mostra il
  grafico e cinque righe; il testo lungo resta negli extra per orologio, auto, schermata di
  blocco e test. Su Android 12+ il sistema la decora comunque (intestazione, icona, ora).
- **Il grafico è dipinto per il tema del sistema al momento dell'invio**: se il lettore cambia
  tema dopo, l'immagine resta quella; il testo intorno segue il tema. Un compromesso accettato:
  una notifica vive ore, non giorni, e i colori scelti reggono su entrambi i fondi.
  **Superato il 30 set 2026** («Il grafico delle notifiche sul fondo sbagliato»): non reggevano.
- **Il messaggio dell'avviso personale non è toccato**: è testo del lettore, e i segnaposto sono
  interpolati in `:core:domain`, puro Kotlin senza locale (debito ereditato da tweather,
  UPSTREAM.md). Corretta solo la riga «Perché è scattata», che è nostra.

### Come è stato verificato

- Resa prima e dopo, chiara e scura; dopo averla guardata: la curva non si vedeva (colore base
  trasparente sotto lo shader), il riempimento era slavato (ora sfuma verso il basso), la notte
  grigia sembrava «dati mancanti» (ora tinta del primario), altezze ridotte per il tetto di 256dp.
- `NotificationChartsTest` (quando si disegna e quando no, ore passate escluse, riga della
  pioggia solo se piove, tetto dei 256dp), `AlertNotifierTest` (immagine su tutti i tipi con
  testo completo, pioggia di oggi dopo «Adesso», accento), `OfficialWarningNotifierTest`
  (griglia), `RuleNotifierTest` (unità e virgola).
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- **Da fare sul dispositivo**: le espanse su due o tre launcher/ROM, chiare e scure; l'icona di
  stato nella barra.

## Le notifiche: quali, e quando (committente, 23 set 2026)

Richiesta: «fai una review anche su queste e verifica che quelle che ci sono sono corrette e se
ne andrebbero aggiunte o tolte altre».

### La review (motori, worker, promemoria, allerte, regole, idee)

Difetti trovati, tutti riprodotti in un test prima della correzione:

1. **Pioggia doppia dopo il temporale**: il temporale zittiva la pioggia solo nel giro in cui
   veniva notificato; un'ora dopo, con l'impronta bruciata, il suo 90% mandava «Ombrello verso
   le 16» sullo stesso evento.
2. **Una pioggia a cavallo di mezzogiorno erano due avvisi** (impronta per mezza giornata
   dell'ora trovata, non dell'inizio della pioggia), e la pioggia già in corso veniva annunciata
   «in arrivo».
3. **Un temporale a cavallo di mezzanotte veniva riannunciato alle 00:30** (l'ora dopo
   mezzanotte ha la data di domani), con importanza alta: sveglia.
4. **Allerta ufficiale ripetuta ogni pomeriggio**: ogni bollettino ha un id nuovo, quindi
   un'arancione di due giorni arrivava due volte senza nulla di nuovo.
5. **Regole sui fatti del giorno due volte al giorno, la prima a mezzanotte** («Oggi UV fino a
   8» alle 00:05 e alle 12).
6. **Nessun rispetto della notte**: un avviso di pioggia alle 03:00 per le 07:00 suonava.
7. **Idea «Una notte senza pioggia»** scattava anche alle 9 del mattino dicendo «Stanotte…».
8. **Numeri nei messaggi delle regole col punto decimale** anche in italiano («21.4°»).
9. Riepilogo del mattino: la riga «oggi» presa come prima del report e non per data (difensivo).

Aggiunte e rimozioni: **nessuna notifica da togliere** — ognuna risponde a una domanda che le
altre non fanno. **Un'idea in più**, «Caldo forte», il gemello estivo di «Ghiaccio domattina».
Valutate e scartate: vento forte (l'orario del fornitore non ha vento, solo il valore di
adesso), «smette di piovere» (lo coprono le idee «Bici» e «Corsa», che cercano finestre
asciutte), avvisi sul cambio di previsione (il Diario li registra; come notifica sarebbero
rumore).

### Cosa è cambiato (DESIGN §8.17b, UPSTREAM.md)

- `AlertEngine.firstArrival` e `nearSevere`; riepilogo del mattino per data.
- `OfficialWarningEngine.toldTokens`: le celle dette bruciate con l'impronta; anello a 120.
- `RuleEngine.dayShaped`: una volta al giorno dalle 6 per le regole su `today.*`.
- Ore di quiete 22–7 (`NotificationViews.quietAtNight`), tranne allerte rosse e promemoria del
  cielo; tratteggio nella striscia «Quando arrivano» e una frase che lo dice.
- `RuleMessages.interpolate(format = …)` additivo; il notificatore e l'anteprima di Avvisi
  scrivono la virgola del lettore.
- Idee: «Caldo forte» nuova, «Dodici ore asciutte» al posto di «Una notte senza pioggia» (le
  regole già create tengono nome e messaggio: sono testo del lettore).

### Decisioni e deviazioni

- **Nessuna soglia cambiata**: la review voleva misurare quanto spesso i codici di maltempo
  arrivano con probabilità bassa, ma Open-Meteo oggi ha rifiutato le richieste da questa rete
  (limite giornaliero). Resta da misurare, come le fasi 13b/26: se i temporali al 10–20%
  risultano frequenti, il maltempo dovrebbe chiedere anche una probabilità minima.
- **Un'ora di pausa non spezza una pioggia**: «70, 65, 80» è un solo episodio.
- **Le ore di quiete sono dell'orologio del telefono**, non del luogo: dorme il lettore.

### Come è stato verificato

- Test nuovi in `AlertEngineTest` (7), `OfficialWarningEngineTest` (3), `RuleEngineTest` (2),
  `RuleNotifierTest`, `QuietHoursTest`; aggiornati `OfficialWarningStoreTest` (anello) e
  `OfficialWarningsStepTest` (celle dette).
- Resa di Avvisi con il tratteggio delle ore di quiete.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## I segnaposto con la loro unità, e la separazione da tweather (committente, 23 set 2026)

Richiesta: sistemare i segnaposto dei messaggi degli avvisi personali («`{current.temp_c}` esce
ancora come 21.4, senza unità e con il punto»); e: «Intendo poi scollegarmi a questo punto da
tweather che seguirà la sua strada ma per ora è ferma».

### Cosa è cambiato

- `RuleMessages.interpolate` prende un `Writer` (valore, variabile, testo che segue; orario).
  `Canonical` è la scrittura di prima; `RuleText.MessageWriter` è quella dell'app: il valore con
  la sua unità e la virgola del lettore («21,4°», «20%», «35 km/h»), sì/no in parole, l'ora
  nel formato 12/24 del telefono. Usato dalla notifica e dall'anteprima di Avvisi.
- **Nessun doppione di unità**: se chi scrive ha già messo l'unità subito dopo il segnaposto
  («{current.temp_c}°», com'erano tutti i messaggi e le idee finora), non viene aggiunta. I
  messaggi già salvati si leggono uguali.
- Le idee non scrivono più le unità a mano; la guida del selettore dice che il valore arriva
  con la sua unità.
- **Separazione da tweather**: CLAUDE.md, VISION §7.3 e UPSTREAM.md dicono che dal 23 set 2026
  Chiaro è un prodotto indipendente; UPSTREAM.md è storia congelata, le correzioni al core non
  si riportano indietro, niente estrazione di un core condiviso.

### Come è stato verificato

- `RuleNotifierTest` (unità una volta sola, virgola), `RuleMessagesTest` (il `Writer` riceve
  variabile, valore e testo che segue).
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## La soglia del maltempo (committente, 24 set 2026)

Richiesta: «prova a controllare le soglie cosi questo round di implementazione è veramente
completo», sull'ipotesi lasciata aperta il 23 set: un temporale con probabilità di pioggia bassa
(10–20%) fa scattare un «Maltempo» a banner.

### La misura

- **Fonte**: Open-Meteo, 31 luoghi (15 italiani, 16 nel mondo in climi diversi), 21 lug –
  18 set 2026. Previsione: l'API forecast con `past_days` (codice e probabilità per ora, come
  li riceve l'app; è la previsione a breve scadenza, quindi più brava di una a 12 ore, e la
  misura è prudente). Verifica: pioggia ERA5 (archive API) nella finestra della serie ±2 ore.
  L'archivio delle corse precedenti (`previous-runs`) non serve la probabilità
  (`precipitation_probability_previous_day1` è tutta null), per questo non è stato usato.
- **Eventi**: 148 serie di ore severe (una serie = un avviso, stessa regola di `firstArrival`,
  un'ora di pausa tollerata): 131 THUNDER, 17 RAIN; nessun ghiaccio né neve d'estate.
- **Risultato** (probabilità massima della serie → serie con ≥ 1 mm osservato, con ≥ 5 mm):

  | probabilità | serie | ≥ 1 mm | ≥ 5 mm | pioggia mediana |
  |---|---|---|---|---|
  | 0–9% | 21 | 24% | 5% | 0,2 mm |
  | 10–19% | 15 | 47% | 7% | 0,7 mm |
  | 20–29% | 8 | 75% | 12% | 2,2 mm |
  | 30–49% | 16 | 100% | 38% | 3,6 mm |
  | 50–69% | 23 | 87% | 48% | 3,8 mm |
  | 70–100% | 65 | 98% | 74% | 6,9 mm |

  Base: cinque ore qualsiasi vedono 1 mm il 12% delle volte, 5 mm il 3%.
- **Soglie provate**: 10% → toglie 21 avvisi (24% verificati), ne perde 5 veri; **20% → toglie
  36 avvisi su 148 (un quarto), di cui un terzo verificati e quasi sempre con pioggia leggera;
  delle 68 serie con 5 mm ne perde 2**; 30% → toglie 44 e perde 18 verificati, 3 con 5 mm.

### Cosa è cambiato

- `AlertEngine.SEVERE_MIN_CHANCE_PCT = 20`, `severeBucket(hour)` / `isSevere(hour)`: **una sola
  definizione** di «maltempo», letta dall'avviso, dal suo «arriva», dal silenzio dell'ombrello
  vicino a un temporale (`nearSevere`), dalla frase di Oggi e dei widget
  (`HeadlineEngine`), dalla finestra della notifica (`AlertDetails`) e da `next_Nh.wmo_severe`
  degli avvisi personali. Nessuna può chiamare temporale ciò che le altre hanno scartato.

### Decisioni e deviazioni

- **Solo THUNDER e RAIN.** Ghiaccio e neve non erano misurabili (estate) e la pioviggine che
  gela è pericolosa in quantità che l'ensemble può contare appena: tengono il codice da solo.
- **Probabilità assente = il codice vale.** È un dato che dipende dal modello (§1.1): un numero
  mancante non è prova che il temporale non verrà.
- **L'icona resta.** L'ora dice ancora temporale (è il cielo del modello, con accanto il suo
  10%); è il verdetto, il banner, che non lo dice più.
- **Un temporale improbabile non zittisce più l'ombrello**: la pioggia all'80% un'ora dopo un
  95 al 10% è la notizia, e nessun altro l'avrebbe detta.

### Come è stato verificato

- `AlertEngineTest` (sotto la soglia nessun avviso, alla soglia sì; ghiaccio, neve e
  probabilità assente invariati; l'arrivo è la prima ora probabile; l'ombrello non più
  zittito), `HeadlineEngineTest`, `RuleVariablesTest` (`wmo_severe` con la stessa soglia);
  i fixture di prova danno ai codici severi una probabilità credibile (40%), come fa il fornitore.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.


## Il motore dei dati: review di Open-Meteo e dei codici WMO (committente, 24 set 2026)

Richiesta: una review del motore che interroga Open-Meteo e interpreta i dati, con attenzione al
`weather_code`; poi «inizia con P1-P6 e tutti i suggerimenti», il suggerimento 1 (un modello WMO
unico) nel momento in cui conviene, riverificando tutto: «è il cuore dell'app».

### La review (le verifiche fatte quel giorno)

- **API live** su 13 luoghi e **sorgente di Open-Meteo** (`WeatherCode.swift`, `calculate`):
  - `current` è il blocco **a 15 minuti** (`"interval": 900`; la tabella 15-minutely della
    documentazione dice «Preceding 15 minutes sum»). Reykjavík: `current.precipitation` 0,4 mm =
    il quarto delle 08:45, contro 2,7 mm dell'ora; Singapore 0,0 dopo un'ora da 0,3 mm. L'app lo
    chiamava `lastHourMm` e `ForecastOutcome` ci basava un'ora di «asciutto».
  - I valori orari di pioggia e probabilità sono «of the preceding hour»: lo slot etichettato
    con l'ora corrente descrive l'ora già passata.
  - Fuori dalla famiglia ICON Open-Meteo **deriva** il codice: pioviggine = sotto 1,3 mm/h,
    82 = ≥ 7,6 mm/h (la soglia di 65), 96 = temporale con punteggio > 85, **99 e la grandine
    mai**. «Hail is only available in Central Europe».
  - `models=icon_seamless` serve `current.uv_index: null`: il DTO non nullable avrebbe fatto
    fallire l'intero report (lo stesso rischio chiuso il 20 set per `uv_index_max`).
- **Il codice**: il giorno prendeva il `max()` del numero WMO fra le ore bagnate, e il numero non
  è un ordine di gravità (80 > 75, 85 > 82, 71 > 65); la riga della settimana diceva «Temporale»
  per i temporali che `AlertEngine` scartava sotto il 20%; il Journal non poteva stampare un
  cambio di cielo (etichetta inglese) e non lo giudicava; cinque tabelle dei codici tenute
  d'accordo a mano.

### Cosa è cambiato

- **`WmoCode` (domain)**: la tabella unica dei 28 codici, con parola (`ConditionWord`, id stabili
  scritti nello storico), fase (NONE/LIQUID/FREEZING/FROZEN), **gravità** esplicita e classe di
  maltempo. La leggono il mapper, `AlertEngine.SevereCodes` (ora derivata), `HeadlineEngine`,
  `WeatherText` (per parola) e `ChiaroIcons` (per voce della tabella). `WeatherCondition` è il
  solo codice: descrizione inglese ed emoji di tweather tolte, con `uvDescription` e
  `AirQuality.status` che nessuno leggeva.
- **Il codice del giorno (P2, P5)**: 1) un pericolo che l'ensemble sostiene
  (`AlertEngine.severeBucket(code, chance)`, la stessa soglia del 20% del banner) prende il
  giorno; un'ora di temporale o nubifragio improbabile è ignorata, millimetri compresi;
  2) la pioggia materiale (1 mm o 3 ore, invariato) sceglie per **gravità dentro la fase
  dominante** (più acqua; ore se mancano i millimetri), a parità di gravità la forma con più
  ore; 3) altrimenti il cielo della luce, dove un'ora di pioggia che non ha guadagnato il giorno
  vota col cielo della sua nuvolosità.
- **La pioggia intorno ad adesso (P1)**: `Precipitation` porta il quarto d'ora con la sua fine
  (`current.time`), le **tre ore chiuse** dalla serie oraria (fine, mm, temperatura) e la
  probabilità dell'**ora in corso** (P7a: lo slot che finisce dopo adesso). Istantanea:
  `current.precip_quarter_mm`/`_end`, `current.past_hours_end`/`_mm`/`_temp_c`.
  `ForecastOutcome`: copertura = esattamente gli intervalli per cui un commit porta una
  quantità, unione, mai allungata; la massima «vista» conta anche le ore chiuse.
- **UV attuale nullabile (P6)**: DTO, dominio, tile di Oggi (non disegnato senza dato), variabile
  delle regole (`optional`), istantanea (chiave assente).
- **Parole e icone (P3, P4)**: 96/99 «Temporale forte» / «Strong thunderstorm» col temporale
  semplice; 82 «Rovesci forti» / «Heavy showers» (disegno invariato); codice 2 «Nuvoloso» in
  italiano. `tools/shipped_icons.py`: la grandine passa in PLANNED; `MeteoconsSets.kt`
  rigenerato con `write_kotlin` dell'importatore (verificato prima che, a lista invariata,
  riproducesse il file identico).
- **Journal (P8)**: lo stato è l'id della parola, e `WeatherSnapshots.conditionWord` legge anche le
  etichette inglesi già su disco, così il primo aggiornamento non segna la settimana come
  cambiata. Il dettaglio stampa «sereno → temporale»; se la pioggia non decide, decide la gravità.
- **Minori**: inquinanti mancanti null e non 0; il verdetto del cielo porta `precipPct` null
  quando nessuna ora ha una probabilità; commento contraddittorio di `repairFog` corretto; un
  codice fuori tabella non viene mai «riparato».
- **Rete (suggerimento 4)**: `WeatherRepository` riprova **una** volta, dopo 2 s, un 500/502/503/504
  del forecast.

### Decisioni e deviazioni

- **La gravità, non il numero, e la fase prima della gravità**: una giornata di neve con un
  rovescio è neve; il pericolo resta davanti a tutto, perché la regola può togliere una
  distorsione, mai un avviso (13b).
- **Un pericolo improbabile non bagna il giorno**: è la decisione che il resto dell'app aveva già
  preso il 24 set; tenere i suoi millimetri avrebbe rimesso dalla finestra la pioggia che il
  banner aveva tolto dalla porta.
- **«Temporale forte», non «con grandine»**: vera con entrambe le letture del codice. La grandine
  tornerà quando la risposta dirà quale modello ha scritto il codice.
- **«Pioviggine» resta** per 51-55: per il lettore è la pioggia fine e leggera, che è ciò che il
  codice promette in entrambe le letture.
- **82 tiene `extreme-rain`**: ≥ 7,6 mm/h è davvero un acquazzone; esagerava la parola, non il
  disegno.
- **I commit vecchi restano ciò che erano**: la loro unica quantità era un quarto d'ora, e come tale
  è letta. I giorni già chiusi con soli commit vecchi perdono l'«è rimasto asciutto» che non
  potevano sostenere; la pioggia vista resta prova.
- **Nessun retry su 429** (i limiti sono per IP e per minuto, «riprova tra un minuto»: su un NAT
  dell'operatore riprovare consuma il limite di altri) **né su errore di rete** (offline è il caso
  comune, e il report in cache è già a schermo).
- **Il `?: 0` della tinta del cielo di Oggi resta**: è un colore, non un numero stampato.

### Rimandato, con la ragione

- **Suggerimento 2 (nuove variabili: `rain`/`showers`/`snowfall`, totali giornalieri, raffiche
  massime, nuvole basse/medie/alte)** e **suggerimento 3 (le condizioni «attuali» ricavate
  dall'ora quando sono vecchie)**: il 3 ha bisogno delle variabili orarie del 2 (percepita,
  umidità, vento), altrimenti l'eroe mescolerebbe ore diverse; entrambi portano numeri nuovi su
  superfici nuove, cioè design (§1.2) e misure proprie. Passata dedicata.
- **P7b (le righe orarie etichettate con l'inizio dell'intervallo)**: sposta di un'ora pioggia,
  probabilità e codice di ogni riga per ogni motore (avvisi, frase, cielo, regole): va misurata a
  parte, con i suoi test di confine.
- **Suggerimento 6 (AQI europeo, soglie dei pollini per specie, nowcast a 15 minuti)**:
  funzioni nuove, non correzioni.

### Come è stato verificato

- Base prima di toccare il codice: 767 test del core e 520 dell'app, verdi.
- `WmoCodeTest` (la tabella contro i codici documentati e contro ogni insieme tenuto a mano
  prima), `OpenMeteoResponseTest` (tre **risposte reali** di quella mattina, stesse variabili
  dell'app, deserializzate con lo stesso `Json`), `WeatherReportMapperTest` (fase, gravità,
  soglia, voto del cielo, ore chiuse, ora in corso, UV assente), `ForecastOutcomeTest`
  (riscritto sulla nuova semantica, con i commit vecchi), `ForecastDiffTest`,
  `WeatherSnapshotsTest`, `TransientRetryTest`, `JournalStateBuilderTest`, `ConditionWordsTest`,
  `ConditionIconsTest`.
- **Controprova**: con il vecchio `dailyCode` rimesso al suo posto, gli 8 test nuovi sul codice
  del giorno falliscono e tutti i precedenti passano: la correzione è reale e non cambia i casi
  che erano già giusti.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## I dati nuovi a schermo, e la chiusura della review (committente, 24 set 2026)

Richiesta: «prepara la proposta, io vorrei visualizzare tutti i dati, o almeno quelli che tu mi
proponi, quindi decidi come è meglio visualizzarli e dove. E poi procedi.» Chiude i punti rimasti
della review del motore: suggerimenti 2, 3 e 6, P7b.

### La proposta (decisa, con le sue ragioni)

**Cosa si chiede in più a Open-Meteo** (verificato il 24 set su 12 luoghi dall'Artide
all'Everest con `best_match`: nessun valore nullo; tutti i campi restano nullabili lo stesso):

- orari: `apparent_temperature`, `relative_humidity_2m`, `dew_point_2m`, `pressure_msl`,
  `wind_speed_10m`, `wind_direction_10m`, `wind_gusts_10m`, `uv_index`,
  `cloud_cover_low`/`_mid`/`_high`;
- attuali: `cloud_cover_low`/`_mid`/`_high`;
- giornalieri: `precipitation_sum`, `precipitation_hours`, `snowfall_sum`, `wind_gusts_10m_max`;
- qualità dell'aria: `european_aqi`.
- **Non** `rain`/`showers`/`snowfall` orari, proposti nella review: la fase del giorno la decide
  già il codice pesato coi millimetri (P2), e nessuna superficie li userebbe. Un dato che non
  ha un lettore è quello che questo repo ha già tolto due volte.

**Dove va ogni dato** (DESIGN §1.3: frase, riquadro, scheda; §1.2: ogni numero con la sua
conseguenza; §1.1: senza dato non si disegna):

| Dato | Dove | Come |
|---|---|---|
| pioggia del giorno (mm, ore) | **Oggi → Dettagli**, riquadro nuovo «Pioggia»; **Settimana → giorno aperto** | valore «12 mm», nota «in 5 ore» e, oggi, «nell'ultima ora 1,1 mm» (le ore chiuse di P1); conseguenza a fasce: <0,2 niente, <2 «qualche goccia», <10 «serve l'ombrello», <30 «giornata bagnata», ≥30 «pioggia abbondante» |
| neve del giorno (cm) | Dettagli (solo se oggi nevica); giorno aperto | «8 cm», fasce: <1 spolverata, <5 strade scivolose, <20 neve da spalare, ≥20 nevicata forte |
| raffica massima del giorno | nota del riquadro Vento; giorno aperto quando conta | «fino a 60 km/h oggi», con la stessa soglia di `gustsMaterial` |
| UV massimo dei giorni | giorno aperto (oggi c'è già come nota) | «UV fino a 7», con la fascia di `uvMeaning` |
| nuvole per strato | **Dettagli**, riquadro nuovo «Nuvole»; **Cielo**, evidenza del verdetto | valore = copertura totale; nota = lo strato che domina («alte e sottili», «basse»); conseguenza per il sole: aperto / velato / a tratti / chiuso |
| vento, umidità, percepita, pressione, UV orari | la **stima delle condizioni attuali** (sugg. 3) e la **frase del giorno** (raffiche in arrivo) | nessun numero nuovo a schermo: rendono veri quelli che ci sono |
| indice europeo dell'aria | riquadro Aria, **per i luoghi in Europa** | fasce EEA 2024 (0-20 buona … >100 pessima), con la sua traccia; fuori Europa resta l'indice USA |
| pollini per specie | riquadro Pollini | soglie **MeteoSwiss** per specie (erba 20/50/150, betulla 11/70/300, ontano 11/70/250, ambrosia 6/11/40, artemisia 6/15/50; olivo come il frassino, stessa famiglia: 11/100/350) e il **quinto livello** «molto alto», che la scala ufficiale ha e l'app comprimeva in «alto» |

**Il giorno aperto nella settimana**: sopra le sue ore, un blocco «fatti del giorno» — una riga
per fatto (icona, valore, conseguenza), solo i fatti che dicono qualcosa quel giorno. La riga
chiusa resta com'è: è già piena, e il principio §1.3 vuole i numeri al secondo livello.

**La stima delle condizioni attuali (sugg. 3)**: quando il blocco `current` ha più di **60
minuti** (è un'istantanea di un quarto d'ora; dopo un'ora la previsione per adesso è più vicina
al vero), Oggi e i widget mostrano i valori **dell'ora corrente** della previsione — temperatura,
percepita, umidità e punto di rugiada, pressione e UV interpolati fra le due ore che la
contengono; direzione del vento dell'ora più vicina; raffica, codice e probabilità dell'ora in
corso — e l'eroe dice **«Stima dalla previsione»** (§1.1: un valore calcolato lo dichiara).
L'aria e i pollini non si stimano: restano quelli misurati, e la loro età la dice già il chip.

**P7b, le righe orarie per l'ora che comincia**: la riga delle 15 porta temperatura, nuvole e
visibilità **alle 15** e pioggia, probabilità, codice e raffica **dalle 15 alle 16** (in
Open-Meteo sono «dell'ora precedente», quindi dello slot delle 16). È ciò che un lettore capisce
da «15 · 60%», e toglie un'ora di ritardo a ogni «pioggia verso le 15». Si fa in un punto solo,
il mapper; l'ultima ora della settimana, che non ha la successiva, si perde.

**Non si fa, con la ragione**: la previsione a 15 minuti («smette tra 20 minuti»). Misurato il
24 set: a Milano, Bari, Palermo e perfino Monaco circa l'80% dei valori a 15 minuti coincide con
l'interpolazione lineare dei valori orari (New York, modello nativo: 36%), e la risposta non dice
quando il dato è vero. In Italia sarebbe precisione finta.

**Le regole degli avvisi personali** ricevono le stesse quantità: `today.precip_mm`,
`today.snow_cm`, `today.gust_max_kph`, `next_6h.gust_max_kph` / `next_12h.gust_max_kph`,
`current.aqi_eu_index`.

### Cosa è cambiato

- **Richiesta e modello**: le variabili della proposta, tutte nullabili e con default (la cache
  scritta prima si legge ancora). `HourlyForecast` porta il resto dell'ora e le nuvole per
  strato; `DailyForecast` pioggia, ore, neve e raffica massima; `CurrentConditions` nuvole,
  strati ed `estimated`; `AirQuality` l'indice europeo e la scala del luogo (`PlaceRegion`,
  per codice di paese); `PollenLevel.VERY_HIGH`; `CloudLayers.dominant()`.
- **P7b** nel mapper: le righe per l'ora che comincia; la pioggia che cade **durante** l'ora
  (codice, probabilità, raffica dallo slot dopo) e il cielo con cui l'ora **comincia** (codice
  di cielo o nebbia dello slot stesso, o quello che la sua nuvolosità fa quando lo slot è
  pioggia dell'ora prima). L'ultima ora della risposta non è più una riga.
- **Stima di adesso** (`CurrentEstimate`, sugg. 3): oltre 60 minuti, in `TodayStateBuilder`,
  quindi anche nei widget; «Stima dalla previsione» sotto l'eroe e una riga sopra i Dettagli.
- **Frase del giorno**: il vento forte più tardi oggi, con la sua ora.
- **Cielo**: lo strato che fa le nuvole nell'evidenza del verdetto (non nel widget).
- **Oggi → Dettagli**: «Pioggia oggi», «Neve oggi» (solo se nevica), «Nuvole», la raffica
  massima nella nota del Vento, l'aria sulla scala europea in Europa, i pollini a cinque livelli.
- **Settimana**: i fatti del giorno aperto (DESIGN §8.5b).
- **Regole**: `today.precip_mm`, `today.snow_cm`, `today.gust_max_kph`,
  `next_6h/12h.gust_max_kph`, `current.aqi_eu_index`; `current.aqi_index` si chiama ora
  «l'indice statunitense dell'aria» perché accanto ce n'è un altro.
- **Pollini**: soglie MeteoSwiss per specie in `WeatherCodes`; i disegni `very-high` spediti
  (`shipped_icons.py`, `MeteoconsSets.kt` rigenerato da `write_kotlin`).

### Decisioni e deviazioni

- **Il codice di un'ora è metà intervallo e metà istante.** La prima stesura di P7b spostava
  tutto il codice di uno slot: sulla risposta reale di Sydney la riga delle 06:00, con 320 m di
  visibilità, diventava sereno perché alle 07 la nebbia era andata. Precipitazione dall'ora
  che comincia, cielo dall'istante con cui comincia.
- **Le fasce si decidono sul numero stampato**: 19,81 cm si legge «20 cm» ed è «nevicata forte»,
  non la fascia sotto; trovato dal test sulla risposta dell'Everest.
- **Lo strato non cambia i verdetti del cielo**: le soglie sono tarate sul totale e l'app non ha
  un riscontro (un tramonto «bello») su cui ritararle; dirlo come evidenza è gratuito e onesto.
- **Nessuna traccia per le nuvole**: la conseguenza dipende dallo strato quanto dalla
  percentuale. Per pioggia e neve **gradini**, non una linea: i millimetri non sono lineari per
  chi legge.
- **L'olivo con le soglie del frassino** (stessa famiglia, allergeni crociati): la tabella
  svizzera non lo ha. **Le soglie sono per medie giornaliere** e il valore è quello dell'ora,
  la stessa approssimazione della scala di prima, sui numeri giusti.
- **Indice europeo solo per codice di paese**, mai per riquadro geografico (Tunisi); un luogo
  senza codice tiene la scala di sempre. Le regole su `current.aqi_index` non cambiano significato.
- **La stima non tocca aria e pollini** e non si fa se le righe non portano il resto dell'ora
  (una cache vecchia): un «adesso» fatto di due ore diverse sarebbe peggio di uno vecchio.
- **Niente previsione a 15 minuti** (misura sopra) e niente `rain`/`showers`/`snowfall` orari.

### Come è stato verificato

- Risposte reali della stessa mattina con tutte le variabili (Reykjavík: 10,5 mm in 16 ore e
  raffiche a 112 km/h; Milano: cielo 100% di nubi alte; Everest: 19,81 cm di neve), più le tre
  di prima per la compatibilità della cache. `CurrentEstimateTest`, `CloudAndRegionTest`,
  `WeatherCodesTest` (pollini), `SkyVerdictEngineTest` (strato), `RuleVariablesTest` (nuove
  variabili), `WeatherReportMapperTest` (P7b, campi, aria, pollini), `HeadlineEngineTest`
  (vento più tardi), `NewQuantitiesTextTest` (fasce e parole).
- **Controprova P7b**: con le righe rimesse sullo slot proprio falliscono i quattro test che
  parlano dell'ora in cui cade la pioggia, e passano quelli sul cielo, come deve essere.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## La temperatura in gradi interi (committente, 24 set 2026)

Domanda: «la temperatura attuale è visualizzata con un decimale: va bene così o meglio
arrotondare?». Risposta data: arrotondare. Il decimale era la risoluzione del modello, non la
sua precisione (una temperatura a 2 m prevista vale uno o due gradi), da oggi l'eroe può essere
una stima interpolata fra due ore, e le app per il grande pubblico stampano gradi interi: il
decimale è delle app che leggono il termometro di una stazione. Decisione del committente: sì.

- **Cosa è cambiato**: la temperatura principale e la percepita in gradi interi (`TodayScreen`);
  tolti `heroTemperatureText` e il suo test (i decimi piccoli del 23 set non hanno più niente da
  rimpicciolire); DESIGN §5 (regola dell'arrotondamento) e §8.1 aggiornati.
- **Resta col decimale** la lettura di un avviso personale («ora 21,4°»): lì il decimo è il
  motivo per cui una soglia di 21 è scattata, ed è un valore letto, non una previsione mostrata.
- **La percepita resta legata a un grado di differenza**: con i gradi interi due valori a un
  grado di distanza si stampano sempre diversi, quindi non compare mai «21°, percepita 21°».
- **Verificato**: `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## La guida allineata al motore (committente, 24 set 2026)

Review della guida dopo le tre passate: non diceva niente di quello che era arrivato. Aggiornati,
in italiano e in inglese, i paragrafi della frase (il vento forte in arrivo), della pioggia (i
millimetri e le ore), dei dati vecchi (la stima dalla previsione), delle prossime ore (ogni
colonna è l'ora che comincia: «15 · 60%» è la pioggia fra le 15 e le 16), della settimana (i
fatti del giorno aperto), dei dettagli (pioggia, neve, nuvole per strato, aria sull'indice
europeo, pollini per specie), dei verdetti (lo strato nelle prove) e degli avvisi personali (le
grandezze nuove). Gli esempi disegnati (UV e umidità, i verdetti, l'età del dato) erano già
giusti. `guide_today_hours_body` porta `formatted="false"`: il «60%» nel testo non è un formato.

## Il sole coperto sembrava la luna (committente, 24 set 2026)

Da uno screenshot delle 15:50 a Cavenago, cielo «Coperto»: nel cielo di Oggi un disco grigio
chiaro dai bordi netti, «dal colore sembra più la luna». Era il sole (alto 35°; la luna sorgeva
alle 18:16 e sotto l'orizzonte non si disegna). La nuvola sbiadiva disco e alone dello stesso
70%: a copertura piena restava un disco crema al 30% con l'alone quasi spento, cioè un cerchio
grigio col bordo, esattamente come si disegna una luna.

- **Cosa è cambiato** (`SkyBodies.sunVeil`): il disco svanisce con la nuvola fino a zero,
  l'alone tiene gran parte della sua forza e si allarga di un terzo. Coperto: una zona di cielo
  più luminosa e morbida, senza bordo; 80%: un disco tenue in un alone largo; metà: il disco
  con il suo alone; sereno: invariato. La luna resta un disco netto con la sua fase.
- **Come è stato verificato**: simulato prima di scriverlo, sul colore di cielo dello
  screenshot, prima/dopo a 100, 80, 50 e 0% accanto alla luna di giorno (lo «prima» riproduce
  lo screenshot); `SkyBodiesTest` fissa i parametri e i loro estremi. DESIGN §8.1c aggiornato.

## «In parole»: il numero calava nei giorni d'allerta, e la linea di base (committente, 24 set 2026)

Due screenshot del pannello 4×2 alla stessa ora: Cavenago («Coperto», tre ore di «Più tardi»)
composto bene; Catania («Nuvoloso», «Allerta gialla») con il numero una riga più in basso e
un'ora in meno sotto. Più la proposta: numero e massima/minima allineati in basso.

- **La causa**: il piano riservava sempre **due** righe di frase (Glance non misura il
  testo), e con l'allerta la colonna di destra valeva 2 × 23,76 + 2 × 21,12 = 89,8 dp contro
  gli 84,5 del numero. Il blocco risultava 5 dp più alto di come si disegna, «Più tardi»
  perdeva una riga per quei 5 dp, e il numero, inchiodato al fondo, scendeva di una riga
  intera. Solo nei giorni con un'allerta e una frase corta, cioè proprio quelli visti.
- **La correzione**: la frase si misura (`measureWidgetText`, la stessa misura che le schede
  Ora e Oggi usano da 20 set per le loro righe, con `RowFitSlack`) contro la sua colonna;
  se sta su una riga ne riserva una (`textPanelPlan(sentenceFitsOneLine)`). Una frase che ne
  vuole due le ha ancora, e lì il blocco cresce davvero: è onesto.
- **La linea di base**: il terzo giro (19 set) voleva «la linea di base del numero e quella
  della coppia cadano insieme», ma le colonne erano allineate per le **scatole** di riga, e
  la scatola tiene sotto le cifre una discesa proporzionale alla taglia: ~17 dp a 64 sp, ~4 a
  16. Il numero stava 13 dp sopra la coppia. Ora la colonna di destra si alza della
  differenza (`textPanelBaselineLift`, 13,0 dp sul pannello di riferimento; misurato sullo
  screenshot: ~13,6), e le cifre dei due stanno su una riga. La discesa è quella del font
  del telefono (`widgetTextDescentEm`, dal suo `fontMetrics.bottom`), Roboto (0,271 em) come
  riserva e nei test.
- **Decisioni**: l'allineamento è aria e si paga solo con quello che la colonna lascia: non
  toglie mai una riga di parole (test su tutte le combinazioni, anche a 300×150 e scala 1,3).
  Con i dati vecchi l'ultima riga a sinistra è la marca «aggiornato…», più piccola della
  coppia: niente sollevamento, le colonne condividono la sua linea di base come prima. Solo il
  pannello: la riga 4×1 centra le colonne in verticale e non ha un fondo comune da allineare.
  Il glifo, quando è acceso, perde i 13 dp dell'allineamento (58,2 invece di 71,2 sul 4×2 con
  due righe riservate) e riprende la riga che la frase corta non usa (82,0).
- **Come è stato verificato**: `TextLaterTest` riproduce Catania (376×225: 2 righe con la
  riserva, 3 misurando, uguali a Cavenago), fissa la linea di base comune per numero/coppia e
  numero/frase, lo zero da vecchio, e che il sollevamento non costi mai una riga; aggiornati i
  valori del glifo. Suite completa, lint e build.

## Neve a 0%, «pioggia» che era neve (committente, 24 set 2026)

Due screenshot di Longyearbyen alle 22:37, con tre domande: un fiocco di neve sopra «0%» in
due punti (la striscia delle ore e le ore del venerdì aperto) è normale? Il venerdì aperto
dice «0,6 mm di pioggia in 6 ore» e «0,4 cm di neve»: sono davvero due cose? «Probabilità di
pioggia» comprende la neve?

- **Il fiocco su 0%**: per Open-Meteo è normale. `weather_code` e le quantità vengono dal
  modello deterministico; `precipitation_probability` dall'ensemble, ed è la quota dei suoi
  membri che vede almeno 0,1 mm nell'ora. Al margine i due non concordano: una traccia di
  neve nell'uno, nessun membro sopra la soglia nell'altro. **Decisione del committente:
  l'icona resta quella che arriva**, `weather_code` non si modifica per questo. Una prima
  versione (stesso giorno) disegnava il cielo in un'ora a 0% e non la contava per
  l'etichetta del giorno; è stata tolta prima del merge: si mostra il dato del provider.
- **La «pioggia» che era neve**: `precipitation_sum` è pioggia, rovesci **e l'acqua della
  neve** insieme (i 19,81 cm dell'Everest nella fixture del 24 set arrivano come 28,4 mm). Gli
  0,6 mm del venerdì erano quasi tutti gli 0,4 cm di neve. **Cosa è cambiato**: si chiedono
  anche `rain_sum` e `showers_sum`; `DailyForecast.rainMm` è la loro somma, e la stampano la
  riga «di pioggia» del giorno aperto, la tile «Pioggia oggi» e la variabile delle regole
  `today.precip_mm` (l'id resta per le regole salvate; «la pioggia di oggi» ora è la pioggia).
  `precipMm` resta il totale, per chi un giorno dovesse volere proprio quello.
- **Decisioni**: una risposta senza la divisione (cache di prima, modello che non la fa) usa il
  totale solo in un giorno senza neve; con neve dentro la pioggia è sconosciuta, non il totale.
  `precipitation_hours` conta ogni tipo d'ora: sta sulla riga che è l'unica del giorno (anche
  quella della neve, ora: «0,4 cm di neve in 6 ore»), su nessuna quando pioggia e neve si
  dividono il giorno. Per la stessa ragione, in un giorno con neve la tile della pioggia non
  porta le ore né «Nell'ultima ora», che contano anche la neve.
- **«Probabilità di pioggia»**: la probabilità è di qualunque precipitazione. La didascalia del
  grafico (e la sua descrizione a voce) ora la nomina dai codici della striscia, come la frase
  in cima nomina pioggia o neve dal codice della sua ora: «di neve» quando ogni ora che cade è
  neve, «di pioggia o neve» quando cadono entrambe, «di pioggia» altrimenti e quando nessuna
  ora disegna un codice (la parola di prima).
- **Rimandato**: le descrizioni a voce della cella oraria e della riga del giorno dicono ancora
  «pioggia N%»; cambiarle vuol dire dare a ogni cella la sua parola, e va fatto con quelle.
- **Come è stato verificato**: `WeatherReportMapperTest` (la pioggia sola da `rain_sum` + `showers_sum`, il ripiego senza neve, lo
  sconosciuto con neve), `OpenMeteoResponseTest` (Everest: 28,4 mm di totale, pioggia
  sconosciuta), `RuleVariablesTest` (la regola legge la pioggia, non il totale; il campione
  `sampleWeatherReport` porta ora `rainMm`, senza neve uguale al totale). Suite completa e
  lint. La risposta live di Longyearbyen non è stata riletta: dal container Open-Meteo non
  era raggiungibile.

### Il seguito (committente, 25 set 2026)

- **Icona e percentuale, la regola**: dopo il fiocco su 0% il committente ha visto il caso
  speculare, una nuvola su 70%. Correggere l'uno vorrebbe dire correggere anche l'altro, e lì
  un codice andrebbe inventato (quale pioggia? o neve?). Decisione: **un codice si corregge
  solo quando lo smentisce un altro dato dello stesso modello** (la nebbia con la
  visibilità); tra modelli diversi l'app non fa da arbitro. L'eccezione che resta, un
  pericolo sotto il 20% che non fa l'avviso né l'etichetta del giorno, riguarda gli avvisi,
  non le icone delle ore. La guida delle prossime ore ora lo dice in una frase.
- **La pioggia che non compariva**: a Longyearbyen, subito dopo l'aggiornamento, il lunedì
  (3,7 mm di `rain_sum` + 0,1 di `showers_sum`, 0,07 cm di neve) non diceva niente, e la
  domenica mostrava solo la neve con le ore. Era la risposta scritta su disco dalla
  versione di prima, riletta come fresca per i suoi 15 minuti: `ReportDiskCache` la
  riconosceva solo dalle coordinate. **Cosa è cambiato**: l'entry registra la richiesta
  che l'ha prodotta (`OpenMeteoForecastApi.REQUEST`), e una richiesta diversa non è mai un
  HIT; resta però il ripiego di un telefono offline. E il ripiego per le risposte senza
  divisione usa il totale anche quando la neve è sotto la soglia che lo schermo chiama neve
  (`TRACE_SNOW_CM`, 0,1 cm): l'acqua di quella neve è sotto 0,15 mm, meno di quanto la riga
  stampa, e lo zero esatto nascondeva 3,9 mm dietro 0,07 cm.
- **Come è stato verificato**: la risposta live di Longyearbyen del 25 set (via WebFetch)
  per i numeri; `ReportDiskCacheTest` (la richiesta scritta e riletta, un'entry senza
  campo non è la richiesta corrente); `WeatherReportMapperTest` (la neve in traccia non
  blocca il ripiego). Suite completa e lint.

## Il motore degli stati: dal codice del provider allo stato dell'app (committente, 25 set 2026)

Il committente torna sulla decisione del 24 set («l'icona resta quella che arriva») con
un'analisi propria, «Weather Data Sanitization Pipeline», e una direzione: **filtrare e
rielaborare i dati dove serve**, come fanno le app che ha studiato, invece di fare da
passacarte tra l'API e lo schermo. Chiede una revisione precisa, adattata all'app, con il
parere su se sia la scelta giusta. Qui sotto: i fatti verificati, la valutazione punto per
punto, il motore rivisto fenomeno per fenomeno, i passi. **Stato: proposta, da approvare
prima di scrivere codice.** Supera la regola del giorno prima («tra modelli diversi l'app non
fa da arbitro»), e la frase della guida sulle icone e le percentuali che non coincidono è
stata tolta: la guida delle prossime ore è tornata al testo di prima.

### I fatti, verificati il 25 set

- **Come Open-Meteo scrive `weather_code`** (`WeatherCode.calculate`, letto nel sorgente):
  temporale da una probabilità calcolata su CAPE e lifted index (> 60 → 95, > 85 → 96),
  **prima e indipendentemente dalla precipitazione**; gelicidio solo con
  `categoricalFreezingRain >= 1`, che solo alcuni modelli forniscono; rovesci solo con
  precipitazione convettiva > 0 o CAPE ≥ 800, e per la pioggia **solo da 1,3 mm/h** (sotto
  cade nel ramo della pioviggine); neve **da 0,01 cm/h** (71: 0,01–0,2; 73: 0,2–0,8; 75:
  ≥ 0,8); pioggia **da 0,01 mm/h** (pioviggine 51/53/55 fino a 1,3 mm/h, poi 61/63/65 a
  2,5 e 7,6); nebbia se visibilità ≤ 1000 m; cielo sulle soglie 20/50/80 della nuvolosità.
  Per la famiglia ICON il codice è invece il `ww` del DWD, con la sua logica.
- **La probabilità** (`precipitation_probability`) è la quota dei membri di un ensemble a
  0,25° che vede **più di 0,1 mm** nell'ora (documentazione). Il codice parte da 0,01: fra
  0,01 e 0,1 mm il codice dice «precipita» per definizione di una quantità che la
  probabilità, per definizione, non conta. Questa è la causa strutturale del fiocco su 0%.
- **Le quantità orarie** escono arrotondate a 0,1 mm (`snowfall` a 0,01 cm): la più piccola
  precipitazione che la risposta sa dire è 0,1 mm, la stessa soglia della probabilità.
- **`freezing_rain` non esiste** come variabile: la richiesta risponde HTTP 400. Il
  gelicidio si conosce solo dal codice (56/57/66/67) o si deduce da fase e temperatura.
- **`showers` è la precipitazione convettiva di qualunque fase**, non la pioggia a
  rovesci: nella risposta di Longyearbyen del 25 set le ore codificate 85 (rovesci di neve)
  portano la stessa acqua in `showers` e in `snowfall`, e il 25 somma 0,8 mm contro un
  totale di 0,6. Negli altri sei giorni della stessa risposta vale esattamente
  **totale = rain + showers + snowfall / 0,7** (7 cm di neve = 10 mm d'acqua, la regola
  della documentazione). Quindi **liquida = min(rain + showers, totale − snowfall / 0,7)**.
  → Corretto in questo stesso giro in `WeatherReportMapper.rainOf`: la «pioggia» del giorno
  aperto del 24 set sommava anche i rovesci di neve.
- **Un campione** (6 città, 72 ore, 432 ore, `best_match` del 25 set alle 00:40: Milano,
  Bari, Londra, Reykjavik, Longyearbyen, Tokyo; 70 ore con ≥ 0,1 mm), tre disaccordi:
  - A. codice di precipitazione con probabilità < 20%: **6 ore** (Longyearbyen, 6–18%);
  - B. codice di cielo con ≥ 0,1 mm **nella stessa risposta**: **4 ore** (Bari 08–11, fino
    a 0,5 mm sotto «coperto»): il codice smentisce la quantità dello stesso modello;
  - C. modello asciutto con probabilità ≥ 50%: **16 ore** (Bari 2, Tokyo 14), 7 delle quali
    ≥ 70% sotto un'icona di cielo; tutte a ridosso di un periodo piovoso: è la tempistica
    del singolo modello contro trenta membri.
  Un campione di un giorno è un'indicazione, non una misura: la misura vera è il passo 1.

### La valutazione dell'analisi

**La direzione è giusta.** Le app citate non mostrano il dato grezzo, ma va detto con
precisione chi filtra: il grosso del lavoro lo fanno i **fornitori di dati** (The Weather
Company/IBM, AccuWeather con i suoi meteorologi, il backend di Apple Weather, il National
Blend of Models del NWS) con **post-elaborazione statistica** (MOS, fusione di modelli,
calibrazione sulle osservazioni) e con la derivazione del «tempo sensibile» dai campi fisici.
Carrot Weather è un client che lascia scegliere il fornitore: filtra poco di suo. «Coherence
engine» e «data sanitization pipeline» non sono termini del settore; quelli in uso sono
*post-processing*, *MOS*, *model blending*, *consistency checks* (il Graphical Forecast
Editor del NWS ha strumenti che tengono coerenti probabilità, quantità, tipo di tempo e
nuvolosità). Open-Meteo invece è vicino all'uscita dei modelli: un'app costruita su di lui
ha una ragione in più per avere uno strato suo. Detto onestamente: senza osservazioni e
senza storico l'app non può **calibrare** (migliorare l'accuratezza); può rendere
**coerente e leggibile** quello che mostra, ed è già molto.

Punto per punto:

1. **Floor e capping della probabilità — respinti.** Portare a 0% un 14% cancella
   un'informazione vera (quattro membri su trenta vedono precipitazione), e portare a 50%
   una probabilità sotto il 40% scrive un numero che nessun modello ha prodotto. In più la
   probabilità alimenta gli avvisi (70% in 6 ore), la frase in cima e le regole del
   lettore: cambiarla cambia tutti e tre. La condizione sulla neve del floor (> 0,5 cm) è
   anche ridondante: 0,5 cm sono 0,7 mm d'acqua, già dentro `precipitation > 0,5`. La
   coerenza si ottiene altrove: **il numero resta quello del modello, è lo stato (icona e
   parola) che si decide guardando anche la probabilità.**
2. **Le fasce di intensità — tenute, rifatte.** Avevano buchi (0,4–0,5 mm; sotto 0,1 non
   definito) e una soglia di temperatura (2 °C) per scegliere fra goccia e fiocco, quando il
   modello dice già la fase: `snowfall` contro il resto. Le fasce standard per la pioggia
   (AMS Glossary) sono debole < 2,5 mm/h, moderata 2,5–7,6, forte > 7,6: le stesse soglie
   alte di Open-Meteo. La differenza vera è in basso, dove Open-Meteo chiama «pioviggine
   fitta» 1 mm/h (Tokyo, 26 set, al 90%): per un lettore è pioggia debole.
3. **L'albero — tenuto nella forma, corretto nel contenuto.** Mancavano nebbia (che l'app
   già ripara), rovesci, pioggia e neve insieme come stato vero, le ore probabili senza
   quantità (caso C), il gelicidio senza `freezing_rain`; `precipitation > 0` va sostituito
   da ≥ 0,1 mm; «sleet» è ambiguo (pioggia e neve in UK, granuli di ghiaccio in USA) e va
   detto «pioggia e neve»; le soglie di cielo 25/70 a tre stati cancellerebbero il «quasi
   sereno», e l'app usa 20/50/80 come il provider.
4. **Il ripiego — tenuto, reso per campo e per ora**: non tutto o niente sull'intera
   risposta, ma per ciascun input mancante il pezzo di albero che lo usa torna al codice.
5. **Dove vive — non nel ViewModel.** Frase in cima, avvisi, regole, widget, diario e riga
   del giorno leggono tutti `HourlyForecast.condition`: un filtro nella UI lascerebbe loro
   il codice grezzo e l'app si contraddirebbe fra schermate. Il motore sta in `:core:domain`
   (Kotlin puro, testabile a tabella) e lo chiama il mapper, al posto di oggi.

### Il motore rivisto

Un'ora entra con: codice, quantità totale, neve, `showers`, probabilità, temperatura,
visibilità, nuvolosità (le quantità e la probabilità dello slot successivo, come oggi). Esce
con uno stato; **nessun numero cambia**. Soglie comuni: **misurabile = ≥ 0,1 mm** (la stessa
della probabilità e della risoluzione della risposta); **probabile = probabilità ≥ 20% o
assente** (la soglia del maltempo di `AlertEngine`, e quella sotto cui i bollettini del NWS di
norma non nominano la precipitazione). Acqua della neve = `snowfall / 0,7`; liquida = totale
− acqua della neve.

In ordine; la prima regola che risponde decide:

1. **Temporale** — codice 95/96/99 del provider (l'app non ha CAPE: non lo inventa né lo
   toglie) e probabile. Con ≥ 0,1 mm: temporale con pioggia; senza: il disegno del
   temporale senza gocce, parola «Temporali possibili» (Open-Meteo lo scrive anche in ore
   asciutte, perché nasce dal CAPE). 96/99 restano «forte», senza grandine.
2. **Gelicidio** — codice 56/57/66/67 del provider, **oppure** (da misurare, passo 1)
   liquida misurabile con temperatura ≤ 0 °C: pioggia che gela al suolo. Pioviggine gelata
   sotto 1,3 mm/h, pioggia gelata sopra (le soglie del provider). Nessuna soglia di
   probabilità, come oggi: il ghiaccio è pericoloso in qualunque quantità.
3. **Precipitazione misurabile e probabile** (≥ 0,1 mm e probabile):
   - **Pioggia e neve** se liquida e acqua della neve sono **entrambe** ≥ 0,1 mm: WMO 68
     (debole, < 2,5 mm/h in tutto) o 69 (moderata o forte). Codici WMO validi che Open-Meteo
     non scrive mai; disegno `overcast-sleet` (oggi usato per il gelicidio: da decidere).
   - **Neve** se la parte che cade è neve: **rovesci di neve** (85, forti 86 da 0,8 cm/h)
     quando `showers` ≥ metà dell'acqua dell'ora, altrimenti neve 71/73/75 a 0,2 e 0,8 cm/h
     (le soglie del provider; 75 e 86 restano «maltempo»).
   - **Pioggia** altrimenti: **rovesci** (80, 81 da 2,5, 82 da 7,6 mm/h) quando la parte
     convettiva liquida — `min(showers, liquida)` — è almeno metà, **a qualunque
     intensità** (Open-Meteo li chiama pioviggine sotto 1,3 mm/h); altrimenti **pioviggine**
     (51) sotto 0,5 mm/h, **pioggia debole** (61) fino a 2,5, **pioggia** (63) fino a 7,6,
     **pioggia forte** (65) oltre. Spariscono 53/55: 1 mm/h è pioggia debole, non
     «pioviggine fitta».
4. **Precipitazione probabile senza quantità** (caso C: < 0,1 mm, probabilità ≥ 60%, la
   fascia che i bollettini NWS chiamano «probabile») — **decisione aperta**: mostrare la
   precipitazione come «probabile» (disegno della debole, parola «Pioggia probabile» /
   «Neve probabile»), con la fase dell'ora bagnata del modello più vicina entro 3 ore,
   altrimenti neve a ≤ 1 °C (la soglia media pioggia/neve dell'emisfero nord, Jennings et al.
   2018). Oppure lasciare il cielo e far parlare la percentuale, come oggi.
5. **Nebbia** — la riparazione di oggi, dentro il motore: visibilità ≤ 1000 m, inventata
   solo se persiste in un'ora vicina, tolta quando la visibilità la smentisce. **Nebbia
   gelata** (48) con temperatura ≤ 0 °C: la nebbia che deposita brina, il caso che conta su
   strada; oggi 48 non arriva mai perché solo ICON lo scrive. La precipitazione viene prima
   della nebbia: nella pioggia la visibilità bassa è la pioggia.
6. **Cielo** — dalla nuvolosità sulle soglie del provider (20/50/80, quattro stati), non dal
   codice: il cielo disegnato in cima è già calcolato dalla nuvolosità, e l'icona deve dire
   la stessa cosa. Misurato il 22 ago sul Po: la nuvolosità sposta il 23% delle ore rispetto
   al codice (per la famiglia ICON, che non la usa per il `ww`).

Le ore di traccia (< 0,1 mm) o improbabili (< 20%) finiscono in 5 o 6: è la correzione del
fiocco su 0% (caso A), e un codice «coperto» con 0,5 mm diventa pioggia (caso B).

**Il resto dell'app eredita lo stato**: la riga del giorno (le regole della Fase 26 e del 24
set restano, applicate agli stati), la frase in cima, gli avvisi, le regole, i widget, il
diario. **Il blocco `current`** passa dallo stesso motore con i suoi campi. **Ripiego**: un
input mancante (quantità, neve, `showers`, nuvolosità, visibilità, temperatura) spegne solo
la regola che lo usa, e quell'ora torna al codice del provider per quel pezzo; una cache
vecchia senza i campi nuovi funziona come oggi.

**Fuori, per ora**: la foschia (visibilità 1–5 km, disegni `mist`/`haze` già spediti, ma non
è un codice WMO fra quelli che l'app parla: stato nuovo, da proporre a parte); la grandine
(nessun modello di Open-Meteo la prevede); il ghiaccio al suolo dopo la pioggia (è una frase
o un avviso, non un'icona).

### Le decisioni aperte, per il committente

1. Caso C (probabile senza quantità): stato «probabile» dal 60%, oppure cielo come oggi.
2. Gelicidio dedotto (liquida a ≤ 0 °C): sì dopo la misura, o solo dal codice.
3. «Pioggia e neve» e il gelicidio oggi condividono `overcast-sleet`: separarli (Meteocons ha
   anche `sleet`) o tenerli insieme e distinguerli con la parola.
4. «Temporali possibili» per un 95 senza pioggia nell'ora.

### Le decisioni (committente, 25 set 2026, con i vincoli proposti)

1. **Caso C: sì, stato «Pioggia probabile» / «Neve probabile»**, con quattro vincoli: soglia
   60% da confermare con la misura; disegno della precipitazione debole (`overcast-drizzle`
   o l'equivalente della neve), mai quello pieno; è uno stato di **possibilità**, quindi non
   fa dire alla frase in cima «piove adesso», non conta come ora bagnata per l'etichetta
   del giorno e non porta millimetri; fase dall'ora bagnata del modello più vicina entro 3
   ore, altrimenti neve a ≤ 1 °C.
2. **Gelicidio dedotto: dopo la misura**, e in più con la soglia di probabilità (≥ 20%) che
   il gelicidio scritto dal provider non ha: uno stato dedotto che fa scattare un avviso di
   maltempo senza probabilità alcuna sarebbe il falso allarme più caro dell'app.
3. **Separati.** «Pioggia e neve» prende `overcast-sleet`, che disegna proprio gocce e
   fiocchi. Il gelicidio non ha un disegno onesto in Meteocons (nessuna icona di pioggia che
   gela): si compone, come il «quasi sereno» con `tools/compose_sun_cloud.py`, oppure si
   sceglie un disegno esistente; da decidere al passo 2 con una prova a schermo.
4. **«Temporali possibili» per un 95 senza pioggia nell'ora: sì.** Il limite noto: la
   probabilità è di precipitazione, non di fulmini (Open-Meteo non ne dà una), quindi il
   filtro al 20% è il migliore disponibile, non uno esatto.

### I passi

- [x] 1. **Misura** (25 set 2026, 08:42 UTC): 26 città scelte per fenomeno, 7 giorni, 4342
      ore come le mostra l'app. `tools/measure_states.py` scarica (gira in locale o nel
      workflow `Measure states`, che committa il file), `tools/analyze_states.py` prototipa
      il motore e conta; i dati grezzi sono in `tools/measurements/states-2026-09-25.json`.
      Vedi «La misura» qui sotto.
- [x] 2. `WmoCode`: 68/69 («Pioggia e neve», fase `MIXED`), gli stati dell'app sopra il
      1000 (1061 «Pioggia probabile», 1071 «Neve probabile», 1095 «Temporali possibili»,
      ancora un pericolo), 48 con la sua parola («Nebbia gelata»); `likely` tiene gli stati
      probabili fuori da `isPrecipitation`. Parole IT/EN, disegni (vedi sotto).
- [x] 3. `WeatherStateEngine` in `:core:domain`, puro; `WeatherStateEngineTest` regola per
      regola, con i confini delle soglie, i ripieghi e i casi della misura per nome.
- [x] 4. Il mapper costruisce le ore delle righe (intervallo dallo slot dopo, istanti dal
      proprio) e le passa al motore; il giorno lavora sugli stati riportati agli slot; il
      blocco `current` passa dallo stesso motore. Chiede `snowfall` e `showers` orari e
      correnti; spariscono `repairedCodes`, `skyAt` e `repairFog`, entrati nel motore.
- [x] 5. Verifica: le fixture reali passano invariate (Reykjavik, Milano, Everest, Sydney);
      il motore Kotlin rigirato sulle 4342 ore della misura dà lo stesso stato del
      prototipo Python in 4339; le 3 differenze sono tutte l'arrotondamento voluto (0,2 mm
      con 0,07 cm: 0,1 di liquida e 0,1 d'acqua di neve, «pioggia e neve» in Kotlin, dove
      il prototipo senza tolleranza leggeva 0,0999… di liquida). Suite completa e lint.
- [ ] 6. Un giro sul telefono nelle città degli screenshot (Longyearbyen, Bari, Tokyo).

### Come è stato scritto (25 set 2026)

- **I disegni.** Il gelicidio prende il disegno di quel che cade — pioviggine per 56/57,
  pioggia per 66/67 — perché Meteocons non ha un disegno onesto della pioggia che gela, e
  `overcast-sleet` (gocce e fiocchi, lo si è guardato) è andato a pioggia e neve, che è
  esattamente quel che disegna. Il pericolo resta della parola e del banner «Maltempo»,
  che per il ghiaccio scatta a qualunque probabilità. Un disegno composto per il gelicidio
  (come il «quasi sereno») resta un seguito possibile. Il temporale che piove prende
  `thunderstorms-*-rain`, importato con `tools/import_meteocons_v3.py` (la ri-esecuzione a
  lista invariata ha dato zero differenze; con le due voci nuove ha aggiunto solo quelle
  a `MeteoconsSets.kt`); «Temporali possibili» tiene il fulmine senza gocce. La pioggia
  probabile prende la pioviggine, la neve probabile l'unica neve sotto una nuvola.
- **Il blocco `current`** somma 15 minuti (`interval` 900 in tutte le 26 città): le
  quantità si portano all'ora prima delle soglie. Non dice mai «probabile» — descrive
  adesso — e può inventare nebbia senza vicini, come prima.
- **La neve forte** vale come avviso a qualunque probabilità, come in `AlertEngine`:
  la regola della probabilità non la toglie.
- **Il gelicidio dedotto** non è scritto: nessuna ora della misura l'avrebbe acceso, e
  resta per la misura d'inverno (decisione 2).
- **La frase in cima** diceva «pioggia» per pioggia e neve insieme (`isSnow` è la sola
  neve), mentre icona, parola e didascalia dicevano entrambe. Chiuso lo stesso giorno su
  richiesta del committente: `Headline.Falling` (pioggia, neve, entrambe) al posto del
  booleano, e le otto frasi «pioggia e neve» in IT/EN. La stessa frase la leggono il
  widget «Ora» e le notifiche, che la ereditano.

### La revisione del codice (25 set 2026, chiesta dal committente)

Una revisione del commit del motore ha trovato nove punti; verificati sul codice uno per uno:

- **Corretti**: (1) pioggia e neve nascondeva l'avviso di una parte forte — ora una neve
  forte (≥ 0,8 cm/h) o una pioggia forte (≥ 7,6 mm/h) dentro l'ora mista le dà il nome e
  l'avviso, anche sotto il 20% per la neve; (2) il giorno votava il cielo con gli istanti
  dello slot prima — ora lo slot k è l'intervallo e l'istante di k, come leggeva
  `dailyCode` prima del motore; (3) senza `showers` (cache vecchia) i rovesci diventavano
  pioggia — ora ripiegano sui codici di rovescio del provider; (6) «probabile» ignorava la
  fase dell'ora stessa — ora viene prima la sua (traccia di neve o codice), poi l'ora
  vicina, poi la temperatura; (8) la probabilità dell'ora in corso si calcolava due volte —
  ora una; (9) la persistenza della nebbia viveva nel mapper — ora è del motore, che ha la
  serie (il blocco `current` la forza con `fogPersists = true`).
- **Chiarito, non cambiato**: (5) il blocco `current` può dire «Temporali possibili»:
  `likely = false` spegne la pioggia o neve probabile, non l'avviso di un temporale che il
  provider vede adesso.
- **Limiti noti, detti al committente**: (4) la frase in cima per pioggia e neve (chiuso
  subito dopo, sopra); (7) il gelicidio disegna la pioggia che è, e la pioggia probabile la pioviggine: la
  differenza è della parola, del banner e della percentuale.
- Dopo le correzioni il motore Kotlin, rigirato sulle 4342 ore della misura, resta uguale
  al prototipo in 4339 ore, con le stesse 3 differenze volute.

### La regola «lo schermo non mente»

Il committente vuole toglierla perché bloccherebbe le elaborazioni. Letta per quello che
dice (DESIGN §1.1: niente sezioni vuote, età vera dei dati vecchi, stime dichiarate, niente
segnaposto spacciati per valori), non vieta nessuna elaborazione: a bloccare è stata la mia
lettura del 24 set, che ne ha fatto un «non toccare il codice». Quindi la regola resta per
quello che dice, e le si affianca il principio che mancava — **i numeri sono del modello,
gli stati sono dell'app** — in `CLAUDE.md` e in DESIGN §1.1. Rimuoverla del tutto
toglierebbe anche le sue parti che nessuno vuole perdere (lo scheletro che sembra uno zero,
l'età dei dati che tace).

### La misura (passo 1, 25 set 2026)

26 città, 7 giorni, 4342 ore; 585 con ≥ 0,1 mm (13,5%). I disaccordi fra i dati grezzi:

| | ore | % |
|---|---:|---:|
| A. codice di precipitazione con probabilità < 20% | 88 | 2,0 |
| B. codice di cielo o nebbia con ≥ 0,1 mm nella stessa risposta | 19 | 0,4 |
| C. asciutta (< 0,1 mm) con probabilità ≥ 50 / 60 / 70% | 103 / 56 / 20 | 2,4 / 1,3 / 0,5 |

Il motore proposto cambia la parola di **795 ore su 4342 (18,3%)**:

- **Cielo, 472 ore (10,9%)**, ed è un fatto europeo: rispetto alle soglie 20/50/80 sulla
  nuvolosità dello stesso slot il codice di cielo diverge nel 10–29% delle ore in Italia,
  Francia e Germania (Cagliari 29%, Trento 27%, Milano 17%) e nello 0–1% altrove (Londra,
  Oslo, Reykjavik, Longyearbyen, le Americhe, Tokyo, Singapore, Mumbai, Sydney). È il `ww`
  della famiglia ICON, che Open-Meteo inoltra senza ricalcolarlo; fuori dall'Europa il
  codice è già la nuvolosità. I passaggi più frequenti: quasi sereno → sereno 161, poco
  nuvoloso → quasi sereno 104, poco nuvoloso ↔ coperto 64.
- **Pioggia e rovesci, 244 ore**: pioviggine → rovesci 120 (Reykjavik, 0,4 mm/h
  convettivi: Open-Meteo chiama pioviggine ogni rovescio sotto 1,3 mm/h), pioviggine →
  pioggia debole 95 (Londra, 0,9 mm/h: la «pioviggine fitta»), pioviggine → cielo 71 e
  pioggia debole/rovesci/neve → cielo 13 (probabilità < 20%: Londra 0,3 mm al 16–19%,
  Montreal 2,2 mm al 17%, Longyearbyen 0,1 mm al 10–18%), cielo → pioggia 9 (il caso B:
  Napoli 0,2 mm al 98% sotto «coperto»).
- **Probabile, 56 ore** (caso C dal 60%): Bari 60–85%, Oslo 88%, Tokyo 71–90%, Singapore;
  due di neve a Longyearbyen, dalla fase dell'ora bagnata vicina.
- **Nebbia, 23 ore**: 18 diventano nebbia gelata (Longyearbyen, −4 °C), 5 ore di
  pioviggine al < 20% con visibilità ≤ 1 km diventano nebbia (Singapore).
- **Temporale (14 ore) e neve (25 ore)**: nessun cambiamento.
- **Mai viste in questo campione**: gelicidio dedotto (0 ore di pioggia a ≤ 0 °C), pioggia e
  neve insieme (0), temporale senza pioggia (0). Fine settembre non ha inverno: **queste
  regole restano da misurare**, rilanciando lo script fra dicembre e febbraio. Fino ad
  allora il gelicidio dedotto resta spento (decisione 2), le altre due si scrivono con i
  test ma senza una misura dietro, e lo si dice.

Due fatti sui dati, che correggono l'analisi del 25 mattina:

- **`rain` e `showers` cambiano significato col modello.** A Longyearbyen `showers`
  contiene la neve convettiva (lo stesso 0,1 mm in `showers` e in `snowfall`, codice 85); a
  Londra `rain` contiene già i rovesci: il 27 set rain 1,5, showers 0,6, totale 1,5; il 30
  rain 7,5, showers 0,9, totale 7,5. L'unica liquida affidabile è **totale − neve / 0,7**,
  che vale in tutte le 585 ore bagnate. `rainOf` (il minimo fra le due) dà la risposta
  giusta in entrambi i casi: la versione mergiata il 24 avrebbe scritto 2,1 mm per i 1,5 di
  Londra. Il motore userà solo la differenza.
- **Il cielo dalla nuvolosità è la scelta con più effetto a schermo**, e riguarda soprattutto
  il pubblico italiano. La ragione per farlo resta: l'icona dirà la stessa cosa della tile
  delle nuvole e del cielo disegnato in cima, che sono già calcolati dalla nuvolosità. Il
  limite: il `ww` del DWD pesa meno le nubi alte e sottili, la nuvolosità totale no; l'app
  ha già le nubi per strato, e un «velato» per il cielo di sole nubi alte è un seguito
  possibile, non una premessa.

## Le schermate del README disegnate dai test (committente, 25 set 2026)

Richiesta: «Fatico a tenere aggiornati gli screenshot […] quelli attuali sono fermi alla
release». Le otto immagini delle schermate erano foto del telefono del 21 set (v1.0.0, in
italiano, su Cortina d'Ampezzo) e da allora l'app era cambiata sotto di loro (il motore
degli stati, le icone, la griglia dei dettagli, Avvisi). Il modello è quello di Passo: le
schermate le disegnano dei test, a richiesta, e una regola del repo dice quando.

### Cosa è cambiato

- **`ReadmeScreenshots`** (`app/src/testDebug/.../readme/`): otto test Robolectric con la
  grafica nativa che disegnano Oggi (in cima, la settimana, i dettagli), Cielo (stanotte, il
  calendario), Avvisi, Impostazioni e la guida in `docs/screenshots/*.png`. Partono solo
  con `-PupdateScreenshots` (`app/build.gradle.kts` passa la cartella come proprietà di
  sistema); senza, `assumeTrue` li salta, quindi la CI e un `test` qualsiasi non riscrivono
  mai un'immagine.
- **Dati veri, non inventati**: `tools/record_readme_data.py` registra Milano in
  `app/src/test/resources/readme/` (il geocoder in inglese, la previsione e la qualità
  dell'aria con **le stesse variabili dell'app**, lette da `OpenMeteoApis.kt` e non
  copiate, l'ultimo bollettino della Protezione Civile e il momento della registrazione).
  `MilanRecording` le passa per la pipeline dell'app: i DTO, `WeatherReportMapper`,
  `TodayStateBuilder`, `SkyStateBuilder`, l'indice delle zone d'allerta e
  `OfficialWarningEngine`. Le schermate sono disegnate cinque minuti dopo la
  registrazione, con le impostazioni di un'installazione nuova (`AppSettings()`).
- **Le schermate prendono lo stato, non il view model**: `SkyScreen`, `AlertsScreen` e
  `SettingsScreen` (internal) sono estratte dalle rispettive `*Route`, che ora le chiamano
  e basta; `TodayPage`, `ChiaroBottomBar` e `BottomBarHeight` passano da private a internal.
  Nessun cambiamento di comportamento.
- **`LocalClock`** (`ui/format/LocalClock.kt`): i punti in cui un composable leggeva l'ora
  da solo al momento di disegnare la leggono da qui: l'età nel chip di freschezza, il «tra
  20 min» e il «tra 13 giorni» del Cielo, il disco di «adesso» nella striscia di Avvisi e il
  giorno del suo bollettino, «oggi» e «ieri» del Diario, i giorni d'esempio della guida. Sul
  telefono è l'orologio di sistema, come prima; nei test è fermo al momento della
  registrazione. I view model restano sul loro `Clock`: non disegnano.
- README: le otto immagini sono `.png` generate; il testo dice da dove vengono e come si
  rigenerano. `widget-day-arc.jpg` e `widgets-home.jpg` restano foto della v1.0.0 e il
  README lo dice. Tolta la frase «the screenshots above are the shipping build», che non
  sarà più vera né falsa per costruzione: le immagini seguono il codice.
- CLAUDE.md: la regola (sotto).

### Decisioni e deviazioni

- **Milano, in inglese** (committente). Inglese britannico (`en-rGB`), non americano: il
  README è in inglese ma la città è italiana, e l'orologio a 24 ore e «Friday 25 September»
  sono quello che vede un lettore europeo. Telefono 384 × 832 dp (il device di riferimento
  delle misure dei widget) a xhdpi.
- **Icone ferme** (committente: «puoi fare gli screenshot con le icone non animate»):
  `LocalAnimatedIcons` a false, e comunque il test di Compose non fa girare le animazioni
  infinite.
- **Cielo in tema scuro**, le altre in chiaro: il README deve mostrare il secondo vestito da
  qualche parte, e il Cielo è la scheda che si apre di sera. Stessa scelta di Passo.
- **Non `captureToImage`**: con questo Compose (BOM 2026.02.01) e Robolectric 4.16 aspetta
  un ridisegno sul thread che sta bloccando e va in timeout dopo 2 s (anche con
  `robolectric.pixelCopyRenderMode=hardware`). `View.draw` della finestra sotto la grafica
  nativa disegna gli stessi pixel senza aggiornare nessuna dipendenza (Passo, su Robolectric
  4.17 e BOM 2026.09, usa `captureToImage`).
- **Niente Diario, niente widget**: il Diario ha bisogno di giorni di storia registrata, e
  una storia inventata sarebbe l'unica immagine finta del README; i widget sono Glance e un
  test non disegna un launcher. Restano fuori finché una registrazione onesta non li rende
  possibili (per il Diario: l'API «previous runs» di Open-Meteo è la strada da misurare).
- **Nessun controllo in CI sulle immagini**: il rendering di Robolectric cambia di un pixel
  fra versioni e font, e un test che fallisce per quello verrebbe ignorato. La garanzia è
  la regola in CLAUDE.md, come in Passo.

### Come è stato verificato

- `./gradlew :app:testDebugUnitTest --tests "*ReadmeScreenshots" -PupdateScreenshots`
  (con lo script del mirror): otto immagini, guardate una per una; e senza la proprietà gli
  otto test risultano saltati. **Due esecuzioni di fila danno gli stessi byte** per tutte e
  otto: la prima prova l'aveva mancata per Avvisi, il cui disco di «adesso» leggeva ancora
  l'orologio vero (`LocalTime.now(zone)`, sfuggito alla prima ricerca perché aveva un
  argomento), ed è così che sono stati trovati anche Cielo e Diario.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

### Il seguito: cosa mostrano (committente, 25 set 2026)

Richiesta: rivedere se le schermate coprono tutto quello che c'è da mostrare, e decidere.
Confrontate con le cinque cose che dicono cos'è l'app (il cielo calcolato, una frase prima dei
numeri, l'agenda del cielo col verdetto, il diario della previsione, gli avvisi scritti da chi
legge) e con l'elenco delle funzioni del README:

- **Aggiunta `alerts-yours.png`**: la sezione «Yours» di Avvisi con due regole fatte dalle
  idee dell'app (Bike e Run, create come le crea `addFromTemplate`: nome, messaggio e
  condizioni dell'idea, mai scattate) e la fila delle idee, con «Already added» su quella
  usata. Gli avvisi scritti da chi legge erano l'unico dei cinque tratti senza immagine.
  Nove immagini generate, non più otto.
- **`today-week.png` con domani aperto**: la settimana e, sotto la riga di sabato, le sue
  ore e i suoi fatti (qui l'UV, perché è un giorno asciutto). Prima mostrava solo le righe.
- **Il Diario resta fuori, misurato**: la Single Runs API di Open-Meteo ridà una corsa
  passata nella stessa forma della risposta viva, ma rifiuta `daily` se la corsa non parte
  alla mezzanotte locale («only supported … if 'run' starts at 00:00 in the requested
  timezone») e le corse delle 22Z non esistono per nessun modello provato (GFS, ICON); la
  Previous Runs API ha solo ritardi di giorni interi. Un diario ricostruito avrebbe un
  aggiornamento al giorno con i buchi in mezzo, che dice il falso sul comportamento
  dell'app. Il README lo dice. La strada onesta è registrare Milano a intervalli per qualche
  giorno e ricostruire le righe con `WeatherSnapshots.flatten`/`flattenForecast`.
- **Fuori anche l'editor delle regole**: è un bottom sheet in una finestra sua, che il
  disegno della finestra dell'attività non contiene, e vuole il view model vero.
- README: le immagini in quattro gruppi (Oggi; Cielo; Avvisi e Impostazioni; la schermata
  home, fotografata), il paragrafo iniziale dice cosa non è disegnato da un test e perché.

**Un difetto trovato dall'immagine, non corretto qui**: in inglese la frase della regola è
«When the temperature now at least 12°», senza verbo (`rule_sentence_fragment` è
`%1$s %2$s %3$s` e gli operatori sono «at least», «above»…; il README promette «*when*
rain in the next 6 hours *is* above 70%»). In italiano «la temperatura adesso almeno 12°»
regge come stile telegrafico. Non toccato in questa modifica, che riguarda le immagini:
è una decisione di testo del committente (aggiungere «is» agli operatori numerici inglesi,
o un frammento con il verbo, tenendo «is»/«is not» dei sì/no).

### Il seguito: «is» negli operatori e i widget disegnati (committente, 25 set 2026)

Richieste: aggiungere «is» agli operatori inglesi e rigenerare l'immagine; e «non hai
rigenerato gli screenshot dei widget e vedo ancora i miei».

- **Gli operatori inglesi portano il verbo**: «is above», «is at least», «is below», «is at
  most», «is exactly», «is anything but» (`values/strings.xml`); «is» e «is not» dei sì/no
  restano come sono, l'italiano non cambia. La frase della card e i chip dell'editor ora
  leggono «When the temperature now is at least 12°», come il README la promette. Nessun
  test fissava le parole vecchie. `alerts-yours.png` rigenerata.
- **I widget sono disegnati, non più fotografati.** La premessa di prima («un test non
  disegna un launcher») era vera del launcher e non dei widget: `GlanceRemoteViews().compose`
  (Glance 1.1.1, sperimentale) compone il contenuto di un widget nei `RemoteViews` che un
  launcher riceve, senza id né launcher, e `RemoteViews.apply` lo applica a una view vera.
  I contenuti erano già funzioni `internal` di un `WidgetModel` (`NowWidgetContent`,
  `TextWidgetContent`, `SkyWidgetContent`, `TodayWidgetContent`, `ArcWidgetContent`), e il
  modello si costruisce come fa `WidgetData.load`: lo stato di Oggi sul report, l'aspetto
  di un widget appena messo (`WidgetLook.defaultsFor`), i momenti giudicati. Per questi
  ultimi `WidgetData.moments` è diviso in due: la lettura delle iscrizioni e
  `momentsFor`, puro e `internal`, che il test chiama invece di copiarlo.
- **Gli angoli**: disegnati su un canvas software uscivano quadrati, card e chip dei
  verdetti, perché Glance li arrotonda col contorno della view (`clipToOutline`, API 31+) e
  solo un render node rispetta un contorno. La view è attaccata alla finestra (accelerata)
  dell'attività del test e registrata in un `RenderNode`, reso da un `HardwareRenderer` su
  un `ImageReader`: gli angoli escono come sul telefono.
- **Il testo è Roboto**, non Google Sans come nell'app: è giusto così, un widget è
  disegnato nel processo del launcher col carattere di sistema, come nella foto del device.
- **L'orologio anche nei widget**: `staleText(…, Instant.now())` in cinque punti, il «domani»
  del widget Cielo (`dayMark`, che ora riceve il giorno invece di leggerlo) e il `now` con cui
  l'arco costruisce la sua serie leggono `LocalClock`: Glance compone con lo stesso runtime
  di Compose e i `CompositionLocal` funzionano uguali. Sul telefono resta l'orologio di
  sistema. Resta `Instant.now()` solo in `WidgetData.load`, che costruisce il modello e che
  il test non attraversa.
- `widgets.png` (Now e «In words» a 4×1, Sky e Today a 4×2) e `widget-day-arc.png` (4×4)
  sostituiscono le due foto della v1.0.0, su un fondo scuro semplice al posto dello sfondo
  del telefono; le misure delle celle sono quelle dei test dei widget (340 × 82, 340 × 189)
  e dell'arco nella foto (340 × 397). Undici immagini generate; nessuna foto nel README.

## Le anteprime del selettore sulla card blu, come Passo (committente, 25 set 2026)

Richiesta: «Riscrivi le anteprime dei widget di Chiaro nel picker prendendo come esempio lo stile
di quelle di Passo, così le rendiamo omogenee tra le due app e si vede che sono dello stesso
ecosistema».

La differenza era una sola, e di sostanza: le anteprime di Passo stanno sulla **card blu piatta**
(`#0F3B6B`, angolo 24 dp, inchiostri bianchi), che è la card predefinita di Chiaro dal 21 set; le
cinque di Chiaro stavano ancora sul **gradiente del cielo** di prima del 21 set. Il selettore
pubblicizzava quindi un vestito che una card appena messa non indossa più, e accanto alle due di
Passo sembrava un'altra famiglia. La struttura (riga larga del 4×1, pannello del 4×2, ranghi e
margini della card vera, valori d'esempio) era già la stessa: Passo l'aveva presa da qui.

- **`widget_preview_sky` → `widget_preview_card`**: stesso nome, stessa forma e stesso colore del
  drawable di Passo, un `solid` di `@color/widget_preview_card` con l'angolo di 24 dp. Le cinque
  anteprime (Colpo d'occhio, In parole, Momenti del cielo, Le prossime ore, L'arco del giorno) ci
  stanno sopra; i commenti dicono la card blu al posto del cielo sotto lo scrim.
- **`colors.xml`**: via le tre fermate del cielo; `widget_preview_card` è
  `widgetCardContainer(WidgetCardColor.BLUE)`. La fermata di mezzogiorno resta, rinominata
  `widget_preview_arc_noon`, perché le bande dell'arco sono il cielo dell'arco e non il fondo della
  card: la dipinge `ArcPainter` su qualunque card. Inchiostri, rampa della pioggia e coppia del
  verdetto non cambiano: una card colorata scrive con la stessa coppia bianca del cielo
  (`WidgetInk.OVER_SKY`).
- **La pastiglia del verdetto** a 10 dp di angolo invece di 12: è quello che `VerdictChip`
  disegna, e un'anteprima che mostra un'altra forma pubblicizza una card che non esiste.
- **La frase fino a tre righe** su Colpo d'occhio e In parole, come la card a una riga la concede
  (`RowSentenceMaxLines`, `TextRowSentenceMaxLines`) e come l'anteprima di Passo: con due,
  «Pioggia in arrivo verso le cinque.» usciva troncata in un selettore dove la card vera la
  stampa intera. Le prossime ore resta a due, che è quello che la sua riga eroe concede davvero
  (`TallSentenceMaxLines`).
- **Un test** (`WidgetPreviewTest`, «the previews draw the default card»), il gemello di quello di
  Passo: il predefinito di `WidgetLook` è `COLOR`, l'hex cotto in `colors.xml` è quello della
  tabella Kotlin del suo colore, e ogni anteprima sta su `widget_preview_card`. Un predefinito
  cambiato o un colore rimisurato fallisce qui e non nel selettore di qualcuno.

**Rimandato, con la ragione**: le anteprime *generate* (Android 15+, `providePreview` e
`setWidgetPreviews`), che Passo pubblica una volta per versione. Chiedono Glance 1.2 (allora 1.1.1,
aggiornato subito dopo: sezione seguente) e un modello d'esempio nel codice di produzione per
ciascuna delle cinque card (un report meteo completo, non una giornata di passi), che è un lavoro a
sé; sui launcher che non le chiedono il selettore mostra comunque il `previewLayout`, ed è quello
che questa modifica allinea.

### Glance 1.2.0, senza anteprime generate (committente, 25 set 2026)

Domanda: aggiornare Glance alla 1.2 è complesso o rischia di rompere qualcosa? Provato in una copia
di lavoro prima di rispondere, poi deciso: «fai l'aggiornamento di Glance e lasciamo così», senza
le anteprime generate, così nelle modifiche future non c'è un modello d'esempio da tenere al passo.

- **Una riga**: `glance = "1.2.0"` in `libs.versions.toml` (era 1.1.1), la stessa versione di Passo.
  Nessun file Kotlin cambia; nessun avviso di deprecazione tocca il codice dei widget.
- **Le dipendenze risolte non cambiano** fuori da Glance (confronto di `releaseRuntimeClasspath`
  prima e dopo): Compose resta alla 1.11.2 del BOM, WorkManager alla 2.10.5 che il catalogo già
  fissa, quella che in Passo aveva tolto le card dallo spinner di caricamento (ADR 0005 di Passo).
- **I widget disegnati sono identici**: `widgets.png` e `widget-day-arc.png` rigenerati con 1.2
  (`GlanceRemoteViews`, sempre sperimentale, stesso comportamento) coincidono **pixel per pixel**
  con quelli fatti con 1.1.1; nessuna immagine del README cambia.
- **Le anteprime del selettore restano i `previewLayout` statici.** Senza `providePreview` il
  launcher mostra quelli su ogni versione di Android, com'era. Il costo di manutenzione che
  resta è quello di sempre: quando una modifica cambia l'aspetto di una card a 4×1 o 4×2 (il
  fondo predefinito, i ranghi del tipo, i margini), il suo `widget_*_preview.xml` si ridisegna a
  mano nella stessa modifica. Cambiare dati, testi o logica delle card non lo tocca.
  `WidgetPreviewTest` ferma in build i due errori silenziosi (anteprima mancante o non
  inflatabile) e il fondo diverso dalla card predefinita.
- Da verificare sul telefono, perché nessun test lo raggiunge: aggiornamento periodico, tap e
  riconfigurazione delle cinque card su One UI.

### La finestra socchiusa: una fascia in più per la notte (committente, 26 set 2026)

Domanda, con la notifica sotto gli occhi: «Stanotte minima 14° verso le 07:00 · Notte mite, si
può lasciare la finestra aperta. Concordi, o le temperature sono un po' bassine per tenere la
finestra aperta?» (Cavenago di Brianza, 16° a mezzanotte, 14° dalle 3 alle 7.)

**Il numero era giusto, la frase no.** A fine settembre in Brianza 14° di minima sono nella
norma, e «mite» si difende; «si può lasciare la finestra aperta» no: una camera aperta tutta la
notte su 14-15° si sveglia verso i 16°, che per molti è fredda. Due difetti nelle fasce del
21 set:

1. **13-21° era una fascia sola**, e una notte a 13,1° e una a 20,9° non chiedono la stessa cosa.
2. **I 13° erano la soglia della felpa** («sotto i 13 il freddo arriva a un corpo che esce»),
   riusata per la finestra: a 14° l'app diceva di dormire con la finestra aperta, quando chi
   esce alla stessa ora la felpa la vuole ancora.

**La decisione**: una fascia nuova, **13-16°**, «Notte fresca: finestra socchiusa, felpa se esci»
(`night_meaning_ajar`); «finestra aperta» resta alle notti tra 16 e 21°; il resto non cambia
(≤ 0 gelo, < 5 fredda, < 13 felpa, ≥ 21 calda, vicina alla «notte tropicale» dei climatologi,
minima sopra i 20°). La fascia si legge sul valore del modello, non su quello stampato, come le
altre della notte. `NightMeaningTest` fissa i bordi e il caso di stanotte: le fasce non avevano
un test.

### La corsa in due fasce, e tre condizioni per avviso (committente, 26 set 2026)

Dalla revisione delle frasi legate alla temperatura nelle notifiche (le altre stampano il
numero nudo; i modelli degli avvisi hanno soglie fisse, e quella della Corsa non aveva un
minimo: poteva dire «Buona finestra per correre» a −5°). Proposta del committente: il
messaggio attuale da 5° in su, uno con le precauzioni fra 0 e 5°, perché è il limite pratico
di molti corridori amatoriali; sotto zero l'aria gelida dà fastidio ai polmoni e soprattutto
cresce il rischio di lastre di ghiaccio sull'asfalto.

**D'accordo su 5° e su 0-5°; sotto zero, niente.** Il modello è un invito («vai a
correre»), e sotto zero sarebbe l'app che invita al rischio che non sa misurare: il ghiaccio
sull'asfalto è dell'asfalto bagnato (pioggia o neve delle ore prima), non dell'aria. A −2°
su asciutto si corre bene, a +1° dopo una pioggia no. Il pericolo lo dice già «Ghiaccio
domattina», alla stessa soglia di 0°.

**Tre condizioni per avviso** (`MaxConditions` da 2 a 3): una regola è condizioni in «e» più
un messaggio solo, quindi le fasce sono due modelli; e una temperatura fra due valori più la
pioggia sono tre condizioni. Con due, la regola più naturale che un lettore scrive («tra 5 e
26° e non piove») non si poteva dire. L'editor offre la terza con lo stesso pulsante; la
notifica elenca già le condizioni una per riga.

| Modello | Condizioni | Messaggio |
|---|---|---|
| Una finestra per correre («Corsa») | pioggia 6 h ≤ 20%, adesso ≥ 5°, adesso ≤ 26° | «Buona finestra per correre: {current.temp_c} e pioggia al {next_6h.precip_chance_max}.» |
| Corsa al freddo (nuovo) | pioggia 6 h ≤ 20%, adesso > 0°, adesso < 5° | «Si corre, ma fa freddo: {current.temp_c}. Strati, guanti e riscaldamento più lungo.» |

- I bordi si toccano senza buchi né sovrapposizioni: 5,0° è della Corsa, 4,9° di quella al
  freddo, 0,0° di nessuna delle due (è del Ghiaccio). Il messaggio della Corsa dice ora anche
  la temperatura, visto che è lei a decidere la fascia.
- Chi aveva già aggiunto «Corsa» tiene la sua regola, senza minimo: un avviso salvato è testo
  del lettore. Cambia solo quel che si aggiunge da ora.
- Il disegno dell'idea nuova è `thermometer-colder`, spostato da PLANNED a SHIPPED in
  `tools/shipped_icons.py` e importato con `tools/import_meteocons_v3.py` (dal pacchetto
  `@meteocons/svg@3.0.0-next.10`): nessun drawable cambia, `MeteoconsSets.kt` guadagna le sue
  quattro righe; `compose_sun_cloud.py` rieseguito, zero differenze.
- `RunTemplatesTest` passa le fasce dal motore vero (−3, 0, 0,5, 4,9, 5, 18, 26, 26,5°), la
  finestra bagnata che le spegne entrambe, e che ogni idea stia nel limite dell'editor.
- `alerts-yours.png` rigenerato: la card «Run» dice ora la sua fascia. Il testo alternativo
  resta vero. La frase della card ripete «la temperatura adesso» due volte («almeno 5° e …
  al massimo 26°»): leggere due condizioni sulla stessa quantità come «tra 5° e 26°» è un
  seguito possibile, non fatto qui.
- VISION §5.4 e la guida degli avvisi dicono «fino a due condizioni in più».

## Gli orari di un avviso: sempre, con la luce, o una o due fasce (committente, 26 set 2026)

Alle 21:47 di sabato «Bici» ha scritto «Si pedala: 18,8° e pioggia al 0%» a Cavenago di
Brianza. Il tempo lo permetteva, l'ora no. Il motore aveva fatto il suo: una regola che
legge le previsioni parla al massimo una volta al mattino e una al pomeriggio, e le due
condizioni sono diventate vere insieme dopo le 21. Quello che mancava era un modo per
dirgli *quando* il lettore farebbe davvero la cosa. Proposta discussa e accettata prima di
scrivere codice: due fasce e «Con la luce».

### Cosa è cambiato

- **`RuleWindow`** (`:core:domain`, `rules/RuleWindow.kt`), un campo nuovo di
  `NotificationRule` con tre valori: `ALWAYS` (quello che ogni regola ha sempre fatto,
  e il default), `DAYLIGHT` e `HOURS` con una o due `RuleBand` in minuti dalla mezzanotte
  sull'orologio del posto (inizio incluso, fine esclusa; una fascia la cui fine viene prima
  dell'inizio passa la mezzanotte).
- **«Con la luce» è il sole sopra l'orizzonte adesso**, all'altezza dell'alba degli almanacchi
  (−0,833°), da `AstronomyEngine.sunAltitude`. Niente alba da cercare: il giorno polare è
  aperto e la notte polare chiusa senza un caso speciale. Segue la stagione da solo: a Milano
  chiude alle 16:40 a dicembre e alle 21:15 a giugno, dove un «7-21» fisso manderebbe in bici
  al buio tutto l'inverno.
- **Nel motore** (`RuleEngine.evaluate`): fuori dagli orari la regola tace e **non registra
  niente**, così una regola ancora vera quando la finestra si apre parla allora («si pedala»
  alle 7:00 dopo una notte mite). Dentro, **una voce per occorrenza della finestra**: una per
  fascia, una per giorno di luce. L'impronta porta il giorno in cui l'occorrenza è cominciata e
  la fascia stessa (`…:2026-09-26:H420-540`, `…:LIGHT`). Una regola sui fatti del giorno
  (`today.*`) tiene la sua voce per data, nella prima fascia che si apre, anche prima delle 6
  se il lettore ha scelto le 5. Le regole `ALWAYS` non cambiano di un byte.
- **L'editor** ha la sezione «In che orari»: tre FilterChip (Sempre · Con la luce · Fasce
  orarie), come la scelta del livello delle allerte. Per le fasce, «dalle [07:00] alle
  [21:00]», ogni orario un chip che apre il `TimePicker` di Material, «e un'altra fascia»
  fino a due, la × per toglierne una. Sotto, una riga che dice cosa fa la scelta; per «Con la
  luce» dice l'alba e il tramonto di oggi al posto attivo.
- **La scheda** dell'avviso dice i suoi orari sotto la frase («Solo con la luce», «Solo dalle
  07:00 alle 09:00 e dalle 17:00 alle 19:30»); per «Sempre» nessuna riga, com'era.
- **«Prova adesso»** fuori orario non dice più «Adesso scatterebbe»: «Scatterebbe, ma non a
  quest'ora: potrà dalle 07:13» (o «da domani alle»), e sotto il messaggio che manderebbe.
- **Le idee Bici, Corsa e Corsa al freddo nascono «Con la luce»**, e la loro descrizione lo
  dice. Ghiaccio, UV, Caldo e Asciutto restano «Sempre»: «Ghiaccio domattina» serve proprio
  la sera prima.
- **«Quando arrivano»**: «i tuoi avvisi» compare tra quelli «a qualsiasi ora» solo se almeno
  uno acceso non ha orari suoi.
- Guida degli avvisi (IT/EN) e VISION §5.4 dicono gli orari.

### Decisioni e deviazioni

- **Una voce per fascia, non il mezzo giorno.** Con le fasce la chiave del mezzo giorno
  (`AM`/`PM`) avrebbe fatto parlare due volte una fascia 11-14, e il latch delle regole su
  `current.*` avrebbe tenuto zitta la fascia di stasera per una condizione vera da stamattina.
  La promessa scritta sotto le fasce è «al massimo una volta per fascia al giorno», e il
  motore fa esattamente quella.
- **Il latch si riarma lo stesso**: una regola su `current.*` con orari che torna falsa libera
  il suo vecchio latch, così tornare a «Sempre» non la trova bloccata.
- **Le fasce restano salvate quando si passa a un altro tipo**: chi prova «Con la luce» e
  torna indietro le ritrova. Una prima visita a «Fasce orarie» parte da 07:00-21:00, la
  seconda fascia da 17:00-19:00.
- **Una fascia che comincia dove finisce non è scrivibile**: il pulsante OK del selettore si
  spegne e lo dice. Un file che ne avesse una (o `HOURS` senza fasce) si legge come «Sempre»,
  mai come una regola zittita per sempre senza una parola.
- **I tuoi avvisi già salvati non cambiano**: il campo manca dal JSON e si legge come
  `ALWAYS`. Il «Bici» del committente resta «Sempre» finché non lo apre e sceglie; un avviso
  salvato è del lettore.
- **Il ritardo**: il controllo gira col job periodico, quindi una regola vera all'apertura
  della fascia può arrivare fino a un intervallo di sincronizzazione dopo. È il ritardo che
  c'era già; nessun allarme nuovo, per la regola della batteria.
- **La striscia «Quando arrivano» non disegna le fasce dei tuoi avvisi**: con dieci regole
  diventerebbe un orario ferroviario. Ogni scheda dice le sue.

### Come è stato verificato

- `RuleWindowTest` (nuovo, 10 casi): fasce e loro bordi, la fascia oltre mezzanotte come una
  sola occorrenza, la luce a Milano a settembre, dicembre e giugno, giorno e notte polare a
  Tromsø, la prossima apertura (fascia, alba, notte polare senza alba da promettere), un
  avviso salvato prima che si legge «Sempre» e il giro completo del JSON.
- `RuleEngineTest` (+6): fuori orario non scatta e non registra, una voce per fascia anche
  per le regole su `current.*`, una fascia a cavallo di mezzogiorno parla una volta, «Con la
  luce» a New York spegne le 21:47, un fatto del giorno nella prima fascia del lettore, il
  latch che si riarma.
- `RunTemplatesTest` (+1): Bici, Corsa e Corsa al freddo nascono «Con la luce», le altre
  idee «Sempre».
- `alerts-yours.png` rigenerato e guardato: le schede Bike e Run dicono «Only in daylight»,
  l'idea della bici dice «In daylight, …». Il testo alternativo lo dice. `widgets.png` usciva
  diverso di 622 pixel di antialiasing, invisibili: rimesso com'era.
- README: la frase sugli avvisi dice gli orari, e già che c'era «fino a due condizioni in
  più» (era «una seconda», ferma a prima del 26 set) e sette idee (erano sei).
- **Provato sul dispositivo dal committente** (30 set 2026), con «Nuovo avviso» qui sotto,
  uscito nella stessa PR (#56): tutto a posto.

## «Nuovo avviso»: la carta per partire da zero (committente, 26 set 2026)

La guida lo prometteva già («oppure costruisci la frase da te») ma l'app no: l'unico modo di
creare un avviso era scegliere un'idea e cambiarle tutto, e un'idea usata restava «Già
aggiunto», quindi anche due «Bici» con soglie diverse erano un giro non evidente.
`RuleStore.add()` senza argomenti c'era, ereditata da tweather, ma nessun pulsante la usava
e scrive nome e messaggio in inglese fisso: non adatta.

- **Una carta «Nuovo avviso» in testa alla fila delle idee** (scelta del committente fra la
  carta e un pulsante sotto «I tuoi»): disegno a matita in `primary` (il «+» c'è già sulla
  riga dell'azione), «Da zero: scegli tu cosa guardare, la soglia, gli orari e il messaggio»,
  azione «Crea». Mai «Già aggiunto». In testa e non in fondo: in una fila da scorrere di lato
  l'ultima carta è quella che nessuno raggiunge.
- **Non parte vuota**: l'editor non tiene una regola senza condizioni, e una frase che
  funziona da cambiare batte uno scheletro da riempire. `RuleText.blank`: «la temperatura
  adesso almeno 20°» (la stessa condizione che aggiunge «e un'altra condizione»), nome «Il mio
  avviso», messaggio «Il tuo avviso è scattato: {trigger.value}.», che resta vero qualunque
  cosa il lettore ne faccia, orari «Sempre». Si apre subito l'editor, come dopo un'idea.
- **Fuori da `RuleText.templates`**: non è mai «già aggiunta», e l'ordine delle idee è quello
  che la foto del README e i test nominano.
- **Il nome è numerato se già preso** (`RuleText.freeName`: «Il mio avviso 2», «Bici 2»),
  per ogni creazione da idea: «ultima volta» si legge dalla cronologia per nome, e due avvisi
  con lo stesso nome condividerebbero un passato.
- Al tetto di dieci sparisce con le idee, sotto la frase che c'è già.
- Guida IT/EN («oppure apri «Nuovo avviso» e costruisci la frase da te»), VISION §5.4, README
  (frase, testo alternativo e didascalia) e `alerts-yours.png` rigenerato.
- `RunTemplatesTest` (+2): l'idea vuota è una regola che il motore valuta, a qualsiasi ora,
  fuori dalle idee; la numerazione dei nomi.
- **Provato sul dispositivo dal committente** (30 set 2026): tutto a posto.

## Il grafico delle notifiche sul fondo sbagliato (committente, 30 set 2026)

Segnalazione dal dispositivo (Samsung, tema chiaro, riepilogo del mattino delle 09:59): «Il
grafico sulle notifiche dell'app non è visibile in maniera ottimale […] verifica anche in tema
scuro». Nella foto le ore e le cifre erano quasi bianche sul fondo grigio chiaro, la curva
marrone, la notte un riquadro lilla: esattamente il grafico dipinto con gli inchiostri della
**notte**, rifatto nel sandbox pixel per pixel. Il testo intorno, che segue il tema da sé, era
giusto. In più «17°» stava sopra «09».

La causa è la decisione del 23 set (sopra, «Il grafico è dipinto per il tema del sistema al
momento dell'invio»): il tema al momento dell'invio non è quello al momento della lettura (una
programmazione notturna che finisce, un cambio a mano, un processo in background con una
configurazione vecchia), e una notifica, a differenza di un widget, l'app non la ridipinge mai.
Il compromesso «i colori reggono su entrambi i fondi» era falso: gli inchiostri di un tema
sono illeggibili sull'altro.

### Cosa è cambiato

- **Due immagini, sceglie il sistema**: `NotificationCharts.themed` dipinge ogni grafico due
  volte (schema del giorno e della notte, stessa veste del lettore letta una volta) e
  `notification_expanded` porta due `ImageView`, con la visibilità presa da
  `values/notification.xml` e `values-night/notification.xml`. Il sistema rigonfia la vista
  quando cambia il tema (è il meccanismo delle risorse `-night` dei widget), ed è l'unico
  momento che sa su che fondo si legge. Vale per tutti e tre i grafici: giornata, ore di
  pioggia, livelli di allerta.
- **Il minimo non cade più sull'etichetta dell'ora**: la cifra sta sotto e di lato al punto
  (a destra, a sinistra vicino al bordo destro), dove la curva non può essere, perché è il
  minimo; il riepilogo del mattino ha quasi sempre il minimo alla prima ora. Grafico asciutto
  200 → 212 px e 8 px in più fra curva e ore; con la pioggia 10 px in più fra curva e barre.
- **Un bordo tenue sotto la curva** nell'inchiostro del fondo (onSurface al 16% di giorno, 24%
  di notte): l'estremo mite della scala (pesca chiaro di giorno, marrone scuro di notte) si
  perdeva nel grigio traslucido di Samsung. Il colore della linea resta quello del mondo.
- **La notte come fascia arrotondata** per ogni tratto di ore notturne, la forma della finestra
  del grafico della pioggia, invece del rettangolo a spigoli vivi nell'angolo.

### Decisioni e deviazioni

- **Due immagini e non una sola «neutra»**: nessun inchiostro è leggibile sia sul fondo chiaro
  sia su quello scuro, e colorare solo le etichette con una tinta di sistema lascerebbe curva,
  barre e bande con i colori di un tema solo. Il costo è la memoria: la giornata con pioggia
  pesa 2 × 1,06 MB, appena sopra la soglia di *avviso* di Android per le viste remote (2 MB,
  solo un log), lontana da quella che le toglie (5 MB).
- **La visibilità è l'indice dell'enum** (`0` visibile, `2` gone), non `View.GONE` (8): il
  test di inflazione l'ha trovato, 8 mandava in crash la vista.

### Come è stato verificato

- Resa nel sandbox (Robolectric, grafica nativa) prima e dopo, tema chiaro e scuro, su quattro
  fondi (grigio Samsung chiaro e scuro, Pixel chiaro e scuro): la resa «notte su fondo
  chiaro» coincide con la foto; dopo, ore e cifre leggibili su tutti e quattro, «17°»
  staccato da «09», curva distinta anche a 17°.
- `NotificationChartsTest` (+3): un'immagine per fondo o nessuna; il layout gonfiato con
  `notnight` mostra quella del giorno e nasconde l'altra, con `night` il contrario.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- **Provato sul dispositivo dal committente** (30 set 2026): «Ho provato ed è tutto ok». Il
  riepilogo del mattino in tema chiaro e in tema scuro, il grafico che cambia con il resto.

## La notifica nel README (committente, 30 set 2026)

Richiesta: «si potrebbe mettere almeno uno screenshot delle notifiche nel readme? Solo se da
valore aggiunto e non solo rumore». Valutazione data prima di farlo: una sola, il riepilogo
del mattino aperto, perché è la cosa che il README non mostrava e che si usa ogni giorno (la
giornata in una frase e un grafico senza aprire l'app); pioggia e allerta sarebbero la stessa
forma con meno da vedere. Il committente ha detto sì.

- **`notification-summary.png`**, terza colonna della tabella dei widget, che diventa
  «Widgets and notifications» (la colonna era vuota). Didascalia e testo alternativo.
- **Nessun dato inventato**: l'avviso è quello che `AlertEngine.evaluate` trova nella
  registrazione di Milano al momento del report stesso (le 12:00 del `current`, l'ultimo
  minuto della finestra 6-12; le 12:10 dell'orologio della registrazione ne sarebbero
  fuori), con le impostazioni di una installazione nuova; lo pubblica `AlertNotifier`, lo
  stesso codice del telefono, e il corpo è la `RemoteViews` che l'app consegna.
- **La tendina non è dell'app**: la cornice (icona, nome, «now», freccia) è il modello del
  sistema stesso, `Notification.Builder.recoverBuilder(...).createBigContentView()`, cioè
  quello con cui Android decora un corpo personalizzato; il fondo è quello di Android
  stock (`system_neutral1_50` / `system_neutral1_800`), non quello di un produttore. Due
  card, tendina chiara e scura, ciascuna con il grafico che il sistema sceglie per lei: è
  la correzione del giorno stesso, e fa vedere che il riepilogo si legge su entrambe.
- Tre aggiustamenti del solo test, perché l'intestazione dicesse quello che vede un lettore:
  il nome «Chiaro» e non «Chiaro (dev)» della build di debug; l'orologio del test portato
  al momento dell'invio (diceva «in 56 y», dal 1970 di Robolectric); tolto dagli extra
  `android.appInfo`, altrimenti il builder recuperato usa un contesto nuovo con la
  configurazione del telefono e colora l'intestazione della card scura per il giorno.
- Le altre undici immagini, rigenerate nello stesso giro, sono uscite identiche byte per byte.

## Il README della famiglia (committente, 1 ott 2026)

Prima della 1.1.0 i README di Chiaro, Passo e Saldo sono stati riscritti su una struttura
comune: intestazione con gli stessi badge e il link al download, cos'è l'app, le schermate in
una tabella a tre colonne con didascalie di una riga, le funzioni in punti di una riga, i
principi, l'installazione (checksum, impronta del certificato, aggiornamenti), la roadmap, la
build, lo stack, la struttura, la documentazione, la famiglia, la licenza. Il committente ha
chiesto punti brevi: le descrizioni lunghe scoraggiavano la lettura, e il dettaglio vive già in
`VISION.md`, `DESIGN.md` e nella guida dell'app. Sono usciti il riquadro delle tre schermate
nell'intestazione, la sezione «Design» (rimanda a `DESIGN.md`) e i conteggi dei test, che
invecchiavano a ogni commit. La «Roadmap» non racconta più la 1.0.0 come imminente.

`release.yml` pubblica ora anche `chiaro-vX.Y.Z.apk.sha256`, come Passo, e il README stampa
l'impronta SHA-256 del certificato di firma, letta dall'APK della 1.0.0
(`apksigner verify --print-certs`).

## La 1.1.0 (committente, 1 ott 2026)

Il numero è **1.1.0**, non la 1.5.0 proposta all'inizio: dalla 1.0.0 ci sono funzioni nuove e
nessuna rottura di compatibilità, cioè un salto di minor per SemVer, ed è il numero che il
CHANGELOG annunciava. `versionName` passa a `1.1.0` e `versionCode` a **2**: la 1.0.0 ha
pubblicato l'1, e un aggiornamento in place deve salire. Il database è alla versione 5 come
nella 1.0.0 (nessuna migrazione nuova), quindi la 1.1.0 si installa sopra la 1.0.0 senza
perdere luoghi, avvisi e impostazioni.

La sezione `## [1.1.0]` del CHANGELOG è scritta corta, come il committente ha chiesto per i
README: le voci lunghe dell'`[Unreleased]` sono riassunte in punti di una riga. Il dettaglio di
ogni modifica resta in questo documento e nei commit.
