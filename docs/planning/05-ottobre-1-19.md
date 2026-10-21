# PLANNING, archivio 05: 1-19 ottobre 2026: le revisioni delle schermate, via il Diario, il cielo premium

> Congelato il 21 ott 2026: un periodo chiuso del registro, spostato qui da `PLANNING.md`
> senza cambiarne una riga. Non si modifica. Il piano vivo è [`PLANNING.md`](../../PLANNING.md);
> l'indice di tutti i titoli è [`README.md`](README.md).

---

## Dopo la 1.1.0: la pagina della release e «Measure states» (committente, 1 ott 2026)

- **La pagina della release è solo la sezione del CHANGELOG.** `release.yml` non aggiunge più
  l'elenco delle pull request generato da GitHub, come già Saldo: le tre app hanno pagine uguali.
  Un tag senza la sua sezione ora fa fallire la release invece di pubblicare una pagina vuota.
- **«Measure states» non parte più sui tag.** Su un push di tag GitHub ignora il filtro
  `paths`, quindi il tag `v1.1.0` ha avviato la misurazione, che ha scaricato i dati e poi non
  ha potuto spingere il commit sul tag (`! [rejected] HEAD -> v1.1.0 (already exists)`). Il
  trigger `push` è limitato ai branch, e il passo del commit gira solo su un branch, così anche
  un avvio a mano su un tag misura senza fallire. Il tag e la release non sono stati toccati.


## La famiglia fuori dal codice (committente, 10 ott 2026)

Tempo 1.0.0 è uscito, quarta app della famiglia: il README lo nomina in «The family». Le app
sorelle (Passo, Saldo, Tempo) si nominano **solo lì e nella documentazione di progetto**, mai
in una stringa dell'app né nel codice: i commenti che dicevano «come Passo», «i valori di
Saldo» o «perché la famiglia si legga come una» sono riscritti con la loro ragione e basta
(il workflow di release, il build, la shell, le anteprime del selettore e il loro test, il
catalogo delle versioni). Le guide nell'app già non le citavano. tweather resta nominato dove
il codice ne viene: è l'origine del core (`UPSTREAM.md`), non un'app della famiglia.

## Il nastro del cielo (committente, 12 ott 2026)

Dalla revisione della schermata Oggi: i sette nastri della settimana erano sette copie (in una
settimana di fine settembre il giorno si accorcia di una ventina di minuti, e a 4dp non si
vede), e quello sul cielo diceva solo la luce. Proposta: colorare il nastro ora per ora con il
cielo previsto, con la stessa funzione che dipinge il cielo in alto. Il committente ha visto la
pagina di confronto prima/dopo (Milano del README e una settimana piovosa vera di Glasgow,
scaricata da Open-Meteo il 12 ott) e ha chiesto di implementarla: «sono ottime modifiche e
visivamente belle».

### Cosa è cambiato

- **`DaylightRibbon` riceve il cielo** (`RibbonHour`: dove cade l'ora nel giorno, copertura,
  probabilità, se è bagnata). `ribbonColorStops` campiona il giorno ogni dieci minuti e tinge
  ogni punto con `SkyPalette.gradient(fase, nuvole, probabilità).mid`; fra due nuclei di fase
  mescola in sRGB come fa il gradiente della piattaforma; i bordi dei nuclei restano fra le
  tappe. Senza ore il risultato è identico al nastro di prima (testato).
- **`DaylightPhases.sky`** costruisce le ore di un giorno dal report, chiuse dalla mezzanotte
  del giorno dopo a 1 (senza segno: è della riga di domani); **`wetRuns`** raggruppa le ore
  bagnate per le parole.
- **Il cielo in alto**: dal momento attuale in poi il cielo previsto; la parte già trascorsa
  resta luce al 60%, come prima.
- **La riga di oggi**: la parte trascorsa sfuma verso il fondo della barra quanto la notte
  (0,75). Prima prova: la riga mostrava la mattina come luce piena, cioè un mattino sereno su
  una giornata coperta (Milano del README). Primo tentativo di cura, il cielo della giornata
  intera prima del taglio di `WeatherRecency`: inutile, perché il mapper non tiene nessuna ora
  precedente a quella corrente. Annullato; la sfumatura dice «già passato» senza inventare.
- **Le ore bagnate** (stato del motore con qualcosa che cade: `RAIN_LIKELY` no) hanno un segno
  in una corsia sotto il nastro, nell'inchiostro della pioggia alla probabilità dell'ora. La
  corsia è della settimana: su tutte e sette le righe se un giorno ha un'ora bagnata (+5dp a
  riga), su nessuna in una settimana asciutta.
- **Le parole**: la descrizione della riga aggiunge «Pioggia dalle 15:00 alle 17:00, alle
  20:00» («Neve», «Pioggia e neve» secondo la fase delle ore). Stringhe IT/EN.
- DESIGN §4 e §8.5, VISION §4.3, CHANGELOG. Gli screenshot `today-now` e `today-week`
  rigenerati e guardati; `widgets.png` usciva diverso solo per antialiasing (622 pixel, nessun
  riquadro di differenza) ed è stato lasciato com'era.

### Decisioni e deviazioni

- **4dp, non 6.** La pagina di confronto suggeriva di provare 6dp: resta 4dp, la misura di §4
  e della riga a 46dp. Con il cielo l'effetto si vede ma è discreto; è la prima cosa da guardare
  sul dispositivo.
- **Bagnata = stato dell'app**, non le due soglie della pagina di confronto (≥ 0,1 mm e ≥ 20%):
  il motore degli stati le contiene già, con il resto delle sue regole.
- **Nessun segno sul cielo in alto**: la pioggia di oggi la dicono già la frase, la striscia
  delle ore e il grafico; il nastro sul cielo resta luce e nuvole.
- **Nessuna frase sulle nuvole** nella descrizione: è una raffigurazione (§4), e le nuvole ora
  per ora sono nella striscia aperta; le ore bagnate invece sono segni, e un segno ha la sua
  parola.

### Come è stato verificato

- `DaylightRibbonTest` (+8): senza ore le tappe sono quelle di prima; con le ore non tornano
  indietro e tengono i bordi dei nuclei; il pomeriggio coperto è grigio e la mattina serena no;
  prima della prima ora e di `skyFrom` niente cielo; nella riga il trascorso sfuma, sul cielo
  no; interpolazione, fine un'ora dopo l'ultima, due ore nello stesso istante; il segno
  centrato sull'ora e dentro il giorno.
- `RibbonSkyTest` (5, nuovo): dove cade ogni ora e la mezzanotte che chiude; `RAIN_LIKELY` non
  bagnata; probabilità assente = nessuna tinta; il giorno tagliato parte dall'ora corrente;
  i gruppi di ore bagnate.
- `TodayStateBuilderTest` (+1): il cielo di oggi parte dall'ora corrente, i giorni dopo interi.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` (con lo script del
  mirror nel sandbox).

## Oggi: la frase dei giorni tranquilli, gli orari sul nastro, il nastro a 6dp (committente, 12 ott 2026)

Dalla stessa revisione: «Vai con 6dp. Poi, tornando ai punti deboli della review, inizia a
implementare le soluzioni dei punti 1 e 2». Il punto 1 era la frase che mancava nei giorni
tranquilli (cioè quasi sempre: il cielo diceva «20° Coperto» e basta) e che, quando c'era,
veniva dopo il nastro; il punto 2 era il nastro sul cielo che non si leggeva senza saperlo già.

### Cosa è cambiato

- **Il nastro nelle righe a 6dp.** Il committente ha visto la settimana di Milano disegnata a
  4, 6 e 8dp affiancate. A 4dp il grigio di un pomeriggio coperto andava cercato; a 8dp il
  nastro è spesso quanto la barra delle temperature (8dp) e la riga diventa due barre dello
  stesso peso. La riga passa da 46 a 48dp.
- **`HeadlineEngine.calm`**, fuori dalla scala: `headline()` risponde ancora «c'è qualcosa da
  segnalare?», e null resta no. Il builder usa `headline() ?: calm()`. Due fatti:
  - **l'asciutto**: solo se nessuna ora rimasta oggi ha qualcosa che cade (stato del motore:
    un'ora «probabile» non conta, una bagnata al 20% sì); poi il primo giorno successivo con
    un'ora bagnata o con l'etichetta di precipitazione («Prossima pioggia domenica», «domani»,
    neve e pioggia e neve per fase), oppure, se non ce n'è, la lunghezza della previsione, da
    tre giorni in su («Asciutto per i prossimi 7 giorni»);
  - **il caldo**: la prima ora di oggi che stampa la massima, se è ancora avanti e almeno un
    grado sopra adesso («massima 23° verso le 16:00»); altrimenti la forbice di domani
    («domani tra 14° e 26°»).
- **La frase prima del nastro**, subito sotto la parola del cielo; il nastro chiude il cielo
  con **alba e tramonto** stampati sotto i punti dove cadono, tolti dalla semantica perché la
  descrizione del nastro legge già gli stessi due orari.
- La guida: «è scritto sopra ogni numero» non era vero (la frase sta sotto la temperatura) ed
  è diventato «sotto il numero grande»; «quando non c'è niente da dire la frase non compare»
  è diventato la frase dei giorni tranquilli. DESIGN §8.1 e §4, VISION §3.3, CHANGELOG.

### Decisioni e deviazioni

- **Niente «3° più caldo di ieri»**, che la revisione proponeva: il mapper non tiene le ore
  passate e il report non ha ieri; servirebbe `past_days`, un cambio del dato e non della
  schermata. Rimandato.
- **I widget non cambiano.** Il registro breve risponde null alla frase tranquilla e i widget
  tengono il loro ripiego, il cielo di adesso: tre righe di quattordici caratteri sono per il
  presente, e «Prossima pioggia domenica, massima 23°» non ci sta. `widgets.png` rigenerato
  usciva diverso solo per antialiasing ed è rimasto com'era.
- **La frase resta sotto la temperatura**, non sopra: VISION §5.1 la disegna lì, e il numero
  grande in cima al cielo è la firma della schermata. È salita sopra il nastro.
- **L'ora del picco è la prima che stampa la massima**: nel Milano del README 22,7° alle 16 e
  22,9° alle 17 sono entrambi «23°» nella striscia, e la frase diceva «verso le 17:00».

### Come è stato verificato

- `HeadlineEngineTest` (+8): la scala tace ancora nel giorno tranquillo e `calm` risponde; un'ora
  bagnata in un giorno con etichetta asciutta chiude l'asciutto; sette giorni asciutti, e
  niente sotto i tre; un'ora bagnata oggi toglie l'asciutto; la neve; la forbice di domani dopo
  il picco o con un picco non più caldo di adesso; la prima ora che stampa la massima; niente da
  dire resta niente.
- `CalmTextTest` (5, nuovo, Robolectric in italiano): le quattro combinazioni e il null del
  registro breve.
- Screenshot `today-now` e `today-week` rigenerati e guardati: «Dry for the next 7 days, high of
  23° around 16:00» sotto «Overcast», 07:13 e 19:15 sotto il nastro, la settimana a 6dp.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## Oggi: meno «0%» e la nuvola che sembrava una luna (committente, 12 ott 2026)

Punti 3 e 4 della revisione di Oggi. Il 3: sei «0%» sotto la striscia e cinque nella settimana
erano la ripetizione più rumorosa della pagina. Il 4: l'icona di «coperto» (`overcast` di
Meteocons, una nuvola con una seconda più scura dietro la spalla) alle 13 e alle 14 si leggeva
come «poco nuvoloso di notte», accanto alla vera icona notturna delle 04.

Prima, la domanda del committente sui widget: frasi tranquille «brevi» solo per loro? Valutata
e **non fatta** per ora, su parere dato al committente (che resta libero di chiederla): il widget di oggi stampa già ↑ e ↓, quindi
la metà «caldo» sarebbe un doppione; l'asciutto cambia di rado e diventerebbe tappezzeria; e la
parola del cielo («Coperto») è l'unico gemello testuale dell'icona, quella che il punto 4 aveva
trovato ambigua. I widget stampano già il registro breve di tutta la scala (allerte, temporale,
ombrello, fine della pioggia, gelo, nebbia, vento, pioggia possibile, ombrello di domani).

### Cosa è cambiato

- **La probabilità si stampa dal 20%** (`WeatherStateEngine.PROBABLE_PCT`, la soglia sotto cui
  l'app non chiama mai bagnata un'ora): nella striscia, nella striscia del giorno aperto e nella
  settimana. Sotto, la cella resta vuota e larga uguale; la descrizione parlata dice ancora la
  cifra, zero compreso. Il grafico della pioggia si disegna solo se la striscia arriva al 20%.
- **Una striscia senza nessuna cifra non ha la riga della pioggia**: la riga vuota era un buco
  di 22dp fra la striscia e la sezione dopo (visto nello screenshot rigenerato). La cella passa
  da 144 a 122dp in quel caso; lo scheletro resta sulla misura alta.
- **WMO 3 disegna `cloudy`**, una nuvola sola. Pioviggine, pioggia, nevischio e neve tengono la
  loro nuvola doppia, dove le gocce dicono cosa sono. `overcast` resta nella lista spedita perché
  i suoi gemelli animati sono generati con lei; nessuna schermata la nomina più. Non si è
  toccato nulla di generato: è la tabella `conditionLineRes`, che è codice dell'app.
- DESIGN §2.3, §8.3, §8.5, §13; VISION §5.2; CHANGELOG. Rigenerati e guardati `today-now`,
  `today-week`, `today-details`, `widgets` e `widget-day-arc` (questi tre per la nuvola).

### Decisioni e deviazioni

- **20% e non 10%**: la soglia è già dell'app, non una nuova; 10% sarebbe stata una terza
  opinione sullo stesso cielo.
- **«Zero compreso» (DESIGN §2.3, 6 set 2026) è superato solo nella stampa**: la rampa
  d'inchiostro resta quella, e colora quello che si stampa.

### Come è stato verificato

- `ConditionIconsTest`: il codice 3 prende `mc3_cloudy`; `MeteoconsSetsTest` e
  `AnimatedIconTest` verdi (le varianti di `cloudy` c'erano già).
- Screenshot rigenerati: la striscia senza «0%» e senza buco, la settimana con la colonna vuota,
  la nuvola sola nell'app e nei widget.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## Oggi: il giorno aperto all'alba, l'agenda che conta il sole (committente, 12 ott 2026)

Punti 5 e 6 della revisione di Oggi, nella numerazione della revisione (nel messaggio prima
li avevo rinumerati male; il committente ha chiesto «5 e 6» e si è seguita la revisione).

### Cosa è cambiato

- **Il giorno aperto parte dall'ora prima della prima ora di luce** (`openingHour`): sabato del
  README apriva su 00–05, tutta notte; ora su 07, con l'alba delle 07:14. Lo stato della lista
  è ricordato dentro l'espansione, quindi ogni apertura riparte da lì. Oggi apre sull'ora in
  corso, come prima (il report non ha ore precedenti).
- **Il «tra X» va alla prima riga che non è della luna** (`countdownIndex`): era sempre la
  prima, e sul Milano del README contava sei ore a un sorgere di luna un minuto prima
  dell'ora d'oro. Un'agenda di sola luna conta la luna.
- **Ora d'oro e ora blu della sera sono intervalli**: `TimelineItem.until`, dal tramonto e dal
  sole a −6° di `SolarDay`; «Ora d'oro fino alle 19:15», e così l'ora blu. Senza la
  fine (un giorno che non ce l'ha) restano il momento di prima. Stringhe IT/EN.
- DESIGN §8.4 e §8.5b, CHANGELOG; `today-now` e `today-week` rigenerati e guardati.

### Decisioni e deviazioni

- **La stella dell'ora blu resta.** La revisione la chiamava arbitraria; guardate le
  alternative della famiglia, sono peggiori: `horizon` è `sunset` con una gobba di 1,6dp (la
  stessa ragione per cui l'ora d'oro l'ha lasciato l'11 set), una luna annuncerebbe una luna.
  La stella è la prima luce del cielo della sera, che è l'ora blu.
- **«Ripete la scheda Cielo nei giorni asciutti» non è stato toccato**: è una scelta di
  struttura (la VISION §5.2.4 vuole l'agenda unita qui), non un difetto da correggere in due
  righe. Resta aperta per una decisione del committente.
- **L'arco del widget non cambia**: stampa gli stessi elementi dell'agenda ma ha la sua riga
  stretta; i campi nuovi hanno valori predefiniti e non lo toccano.

### Come è stato verificato

- `RestOfDayTest` (3, nuovo): il conto salta la luna; la prima riga conta quando non è della
  luna, un'agenda di sola luna conta la luna; il giorno aperto parte dall'ora prima della luce,
  oggi e una notte intera partono dall'inizio.
- `TodayStateBuilderTest` (+1): l'ora d'oro finisce al tramonto della stessa agenda, l'ora blu
  finisce dopo l'inizio, il tramonto non ha fine.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## Oggi: la pressione che va da qualche parte, la griglia senza buchi, la fascia vuota (committente, 12-13 ott 2026)

Punti 7 e 8 della revisione di Oggi. Il 7: la griglia dei dettagli era disomogenea (i pollini
da soli su mezza riga, accanto al vuoto) e la pressione stampava «Alta: tende al sereno» sotto
un cielo coperto all'86%. L'8: fra il bordo del foglio e «Prossime ore» c'erano 53dp di carta
vuota, misurati sullo screenshot del README.

### Cosa è cambiato

- **La pressione come tendenza** (`WeatherText.pressureTrend`): l'ora in corso contro quella tre
  ore dopo, sulle bande dei bollettini marini (sotto 1,6 hPa in 3 ore stabile, da 3,6 rapida).
  In calo, in rapido calo, in aumento: la riga di significato è la tendenza e la nota è il suo
  conto («−2,4 hPa nelle prossime 3 ore»). Stabile: resta il significato della lettura.
- **«Alta» dice *stabile*, non *sereno***: l'alta pressione è tempo stabile, e un anticiclone
  d'autunno è spesso grigio — era la contraddizione con la finestra. EN «settled weather».
- **La tessera spaiata prende tutta la riga** invece di stare accanto a un riquadro vuoto.
- **Il primo titolo sul foglio non aggiunge margine**: il bordo del foglio (24dp) e lo spazio
  della lista (12dp) bastano. Da 53 a circa 42dp fra il bordo e il testo. Con il chip della
  freschezza, un'allerta o la carta della guida in mezzo, il titolo tiene i suoi 12dp.
- La guida: «pressione e dove sta andando». DESIGN §8.6 e la spaziatura delle intestazioni,
  CHANGELOG; `today-now` e `today-details` rigenerati e guardati.

### Decisioni e deviazioni

- **Tendenza in avanti, non all'indietro.** La tendenza barometrica classica guarda le tre ore
  passate; il report non ne tiene nessuna (il mapper parte dall'ora corrente), quindi si legge
  dove il barometro *sta andando* secondo il modello. È scritto così anche nel commento.
- **La tessera delle nuvole resta.** La revisione proponeva di toglierla dove il cielo in alto
  la dice già; ma la tessera dice anche lo strato (un 90% di velo alto lascia passare il sole,
  un 90% basso no), che il cielo in alto non dice.
- Sul Milano del README la pressione cala di 1,3 hPa in 3 ore: per le bande è stabile, e la
  tessera dice «Alta: tempo stabile». Nessuna tendenza inventata sotto la soglia.

### Come è stato verificato

- `PressureTrendTest` (3, nuovo): le quattro bande e il conto; niente tendenza senza la
  pressione o senza l'ora tre dopo; la tendenza vince sulla lettura, la stabilità no.
- Screenshot rigenerati: la tessera dei pollini su tutta la riga, «High: settled weather»,
  «Next hours» più vicino al cielo.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## Ieri, la luna e il barometro: le tre proposte (committente, 13 ott 2026)

Il committente ha chiesto un parere sui due punti rimasti aperti e sulla tendenza barometrica,
con due vincoli: **niente modifiche al modello dati, niente scaricamento eccessivo dalla rete**.
Due fatti verificati prima di rispondere: la risposta di Open-Meteo parte da mezzanotte (le ore
passate di oggi sono già scaricate, il mapper le scarta), e il Journal (`weather_history`)
scrive a ogni aggiornamento la previsione da oggi a +7 giorni, massime comprese. Proposte
accettate: «implementa le tue tre proposte».

### Cosa è cambiato

- **«4° più di ieri»** nella frase tranquilla: quando la massima di oggi dista tre gradi o più da
  quella di ieri, la differenza prende il posto dell'ora («Massima 26°, 4° più di ieri»). La
  massima di ieri viene dal Journal: il commit più recente scritto nella data di ieri (nel fuso
  del luogo) e il suo `<ieri>.high_c` (`TodayStateBuilder.yesterdayHigh`, pura; il ViewModel la
  legge quando i dati si muovono, insieme a «Cosa è cambiato», filtra per data e decodifica un
  solo commit). Nessun campo nuovo, nessuna richiesta: Today legge già quella tabella.
- **La luna fuori dall'agenda di Oggi**: sorgere e tramonto restano nella scheda Cielo e
  nell'arco del widget. Il conto alla rovescia che saltava la luna (`countdownIndex`, 12 ott) era
  diventato codice morto ed è stato tolto: conta di nuovo la prima riga.
- `Formats.temperatureDifference`: una differenza di gradi, senza lo scarto dei Fahrenheit.
- DESIGN §8.1 e §8.4, VISION §3.3 e §5.2, CHANGELOG; `today-now` rigenerato e guardato.

### Decisioni e deviazioni

- **Niente `past_days=1`.** Un 12% in più a ogni aggiornamento per ogni luogo, e i giorni che
  partirebbero da ieri mentre metà dell'app assume che il primo sia oggi: un cambio di modello
  vero, con rischi ovunque.
- **Ieri è una previsione, non una misura**: la massima che il modello dava per ieri al suo
  ultimo aggiornamento della giornata. È il numero più vicino al vero che l'app abbia, e lo
  stesso tipo di numero della massima di oggi (entrambe `highC` del giorno). Senza un commit di
  ieri (installazione nuova, telefono spento) la frase tace e torna all'ora del picco.
- **La tendenza barometrica resta in avanti** (decisione esplicita, non dimenticanza). Il
  barometro classico guarda le tre ore passate perché è uno strumento e misura; le ore passate
  di questa app non sono misure ma l'analisi del modello, quindi «indietro» non è più vero di
  «avanti», e «avanti» risponde alla domanda del lettore (sta per peggiorare?). Se un giorno si
  volesse la convenzione classica, la strada è la pressione nelle `PastHour` che il mapper già
  costruisce dalla risposta scaricata: un campo in più nel modello in memoria, non nel database
  (la cache su disco tiene la risposta grezza) né nella rete; il limite sono le tre ore dopo
  mezzanotte, che la risposta non copre.
- Lo screenshot del README non mostra «più di ieri»: la registrazione non ha un Journal, e una
  massima di ieri messa a mano sarebbe un dato inventato.

### Come è stato verificato

- `HeadlineEngineTest` (+2): tre gradi da ieri prendono il posto dell'ora, in su e in giù; sotto i
  tre o senza ieri l'ora resta.
- `CalmTextTest` (+1): «Massima 23°, 4° più di ieri», e con l'asciutto davanti «…, 3° meno di ieri».
- `TodayStateBuilderTest` (+3, uno aggiornato): la massima di ieri è il commit più recente di ieri,
  non uno di oggi; senza commit di ieri o senza la massima, niente; l'agenda di Oggi senza luna, e
  l'agenda completa che arriva oltre mezzanotte resta quella del widget.
- `RestOfDayTest`: tolti i due test del conto che saltava la luna, resta quello dell'apertura.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## La guida: le due barre della settimana (committente, 13 ott 2026)

Domanda del committente: le due «etichette» della schermata, la barra delle temperature e il
nastro del giorno, sono spiegate nella guida? Per un nuovo utente non sono intuitive.

Verificato: in parte, e con **quattro frasi rese false da questa stessa PR** (non aggiornate
quando si sono cambiate le schermate, ed era da fare insieme): il nastro «dalla prima luce
all'ultima» e «dall'alba al tramonto» (va da mezzanotte a mezzanotte, e ora porta il cielo, la
pioggia, il passato attenuato e gli orari), la luna nell'agenda (tolta), la probabilità per
ogni ora (ora dal 20%). La barra delle temperature era spiegata solo nella scala comune, non in
cosa va da dove a dove, nei colori e nel pallino di adesso.

### Cosa è cambiato

- Le quattro frasi corrette, IT/EN: `guide_today_p1`, `guide_today_hours_body`,
  `guide_today_rest_body`, `guide_today_week_body`.
- **Un esempio disegnato** sotto «La settimana» (`WeekSample`): due righe vere di `DayRow`, oggi
  sereno con il pallino e la mattina passata sbiadita, domani che si copre dopo pranzo e piove
  dalle 15 alle 17 con la corsia delle ore bagnate; la didascalia dice cosa guardare in ciascuna
  barra. I giorni sono quelli veri (come la striscia del Diario nella guida), la luce è una
  giornata generica di media latitudine, i numeri sono l'esempio.
- `guide.png` rigenerato (in fondo mostra il nuovo testo sul nastro); l'esempio è stato
  guardato in uno screenshot temporaneo, poi tolto.

### Decisioni e deviazioni

- **La prima didascalia diceva «scuro di notte»**, giusto per il nastro sul cielo e sbagliato per
  le righe, dove la notte sbiadisce verso il fondo della barra: visto nello screenshot e corretto
  in «tenue alle due estremità, che sono la notte».
- **Le temperature dell'esempio sono in °C** anche per chi usa i Fahrenheit: la guida non riceve
  le unità del lettore, e la didascalia non cita numeri. Da rivedere se la guida le riceverà.
- Nessun esempio della striscia sul cielo: il paragrafo d'apertura lo descrive, e il cielo vero
  è a un tocco dalla guida.

### Come è stato verificato

- Screenshot temporaneo della sezione (inglese, tema chiaro), guardato, poi rimosso insieme al
  test che lo disegnava.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## I widget: la revisione, punti 1 e 2 (committente, 13 ott 2026)

Il committente ha chiesto una revisione dei cinque widget (layout, grafica, tipografia, numero
dei widget), l'ha accettata in quattro punti e ha chiesto di partire dai primi due: le correzioni
rapide e la scala tipografica con un solo abito per il verdetto. I punti 3 (il raggio degli
angoli preso dal sistema, la curva della luna distinguibile) e 4 (da cinque widget a tre) sono
approvati e vengono dopo. Il raggio riguarda tutta la famiglia, e si fa nelle tre app insieme.

La revisione è stata fatta disegnando ogni widget a ogni dimensione che offre (EN, IT, card
chiara, card cielo) con la registrazione di Milano del README, e misurando i testi con il font di
sistema (`measureWidgetText`) prima di decidere ogni misura qui sotto.

### Cosa è cambiato

**Punto 1, le correzioni:**
- **L'agenda dell'arco a due celle** è su due righe, il nome sopra l'orario e il segno del
  verdetto dopo l'orario («Domani ·» davanti all'orario, non al nome). Su una riga la colonna di
  131 dp lasciava al nome 42 dp, e il 2×4 stampava «Moon… / Gold… / Tom…». Ora il nome ha 103 dp,
  e il più lungo che l'agenda dice nelle due lingue, «Finisce l'ora d'oro», ne misura 99,5. Le
  righe della pioggia usano il nome breve del titolo. Il 2×3 passa da tre righe a due, il 2×4 ne
  tiene quattro.
- **Le ore della striscia di «Le prossime ore» sono orari**: «13:00» e non «13», che sopra la riga
  dei giorni («Oggi Sab Dom») si leggeva come il 13 del mese (`Formats.hourClockLabel`). Con
  l'orologio a 12 ore resta «1 PM», che lo dice già. La cella si allarga all'etichetta misurata
  (`stripCellFor`): a un font grande la striscia mostra un'ora in meno invece di tagliare «13:…».
  L'app e i tick dell'arco restano «13»: lì l'asse o il cambio di giorno dicono cosa sono.
- **Un solo nome per l'ora d'oro.** In italiano l'app diceva «Ora dorata» nel Cielo e «Ora d'oro»
  nell'agenda di Oggi e nell'arco: ora è «ora d'oro» ovunque (nomi del Cielo, guida). E il widget
  Cielo stampa il **nome breve** (`SkyText.shortNameRes`): «Ora d'oro», non «Ora d'oro, sera»,
  perché l'orario accanto dice già quale; il «, sera» era quello che l'ellissi mangiava a tre
  celle. Vale per i sette momenti che hanno un gemello mattina/sera; «Venere, stella della sera»
  resta intero, perché lì la sera è cosa il momento è.
- **Il titolo dell'arco** è il prossimo momento con un verdetto, poi il sole, poi la luna
  (`ArcSeries.nextLight`). Diceva «Sorge la luna alle 18:35» sopra un'ora d'oro di un minuto dopo
  che il lettore segue e che il Cielo giudica «Bello».
- **I nomi nel README** sono quelli del selettore dei widget («Colpo d'occhio», «Le prossime
  ore», «In parole», «Momenti del cielo», «L'arco del giorno» e gli inglesi), che la guida usava
  già.

**Punto 2, la scala e il verdetto:**
- **La scala tipografica dei widget** (`WidgetType.kt`, DESIGN §5): sei gradini, ciascuno un
  ruolo. Caption 11, Label 13, Body 16, Title 22, Display 30, Hero 34 e oltre. Cosa si è mosso:
  il nome del momento nel Cielo 15 → 16; la pillola del verdetto 11 → 13 e l'evidenza 14 → 13;
  la striscia di «Le prossime ore» 12/14 → 13/13 (il valore Medium, così etichetta e numero si
  distinguono per peso e inchiostro); la settimana dell'arco 11/12 → 13/13; la frase dell'arco
  15 → 16 e la riga sotto 12 → 13; il numero dell'arco a due celle 28 → 30, a una riga impilata
  20 → 22; le righe dell'arco a una riga 12 → 13; «Più tardi» di «In parole» 14 → 13; la pillola
  dell'allerta 11 → 13; gli stati vuoti 14/12 → 16/13. `WidgetTypeTest` fallisce su una misura
  fuori scala.
- **Il verdetto ha due abiti, per ruolo** (prima tre). Il titolo del Cielo porta la pillola,
  ora con il segno prima della parola come nella pillola dell'app; stretta, la pillola si riduce
  al segno nel cerchio. Ogni riga di elenco, nel Cielo come nell'arco, porta il segno nudo
  dell'arco in uno spazio che ogni riga tiene, così gli orari stanno in colonna (`VerdictNote`).
- **L'evidenza ha la maiuscola** («Nuvole 19%»): nell'app segue la parola sulla stessa riga, nel
  widget è una riga a sé.

### Decisioni e deviazioni

- **Sei gradini, non i cinque proposti** (11/13/16/22/34). Il Cielo stampa un orario come numero
  principale, e «7:55 PM» a 34 sp misura 128 dp, contro i 108–130 che una card a quattro celle
  lascia alle parole: il 30 che aveva resta, ed è il gradino Display, che serve anche al numero
  dell'arco accanto alla frase.
- **Una misura resta fuori scala, di proposito**: la frase di «In parole» a 18 sp. È una
  decisione del committente sul dispositivo (19 set 2026: «17 su 16 non è un grado»), e la scala
  non la scavalca senza chiederlo. È scritta in `WidgetType` e nel test.
- **La colonna del verdetto si misura nella lingua della card** (`skyVerdictColumn`,
  `skyWordsMin`). Con il segno nella pillola e la parola a 13, una colonna fissa per una lingua è
  sbagliata per l'altra: «Presto per dirlo» con il segno fa 124 dp, «Not sure yet» 102; «19:55»
  80, «7:55 PM» 116. Resta geometrica (la parola più larga delle quattro e un orario campione,
  mai il momento in mostra), così la card non cambia forma con la previsione. Le tabelle dei test
  usano il riferimento italiano a 24 ore; un 4 celle su una griglia a cinque colonne (320 dp) ora
  è largo in italiano e stretto in inglese a 12 ore, ed è giusto così.
- **Il padding verticale della pillola dell'allerta e del verdetto è 2 dp, non 3**: con la parola
  a 13 la pillola sarebbe cresciuta di 2,64 dp, e una card «Le prossime ore» 3×3 con l'intervallo
  del giorno e un'allerta perdeva la riga dei giorni. Con 2 dp cresce di 0,64 e la tiene.
- **La card dell'arco a una riga ha 9 dp sopra e sotto, non 10**: con le righe a 13 sp il nome e
  l'orario sotto il numero non stavano più su due righe in 82 dp (63,36 contro 62). Il dp che
  manca viene dal padding.
- **Il 2×3 dell'arco perde le temperature sotto le ore** (l'arco scende a 74 dp, sotto i 76 che
  servono): due righe d'agenda leggibili valgono più di una riga di numeri sul disegno.
- **Il segno del verdetto nelle righe del Cielo** prende l'inchiostro misurato per il fondo della
  card (`palette.darkGround`), come l'arco; la pillola e il cerchio del titolo restano sulla
  coppia contenitore/inchiostro, che porta il suo fondo.
- **La famiglia**: la scala nasce qui, misurata su questi widget, ed è stata portata nella
  stessa sessione nelle card di Passo e di Tempo (`WidgetType` in ciascuna, con il suo test; la
  decisione è registrata nei loro PLANNING §15 e ADR dei widget). Saldo si allinea in una
  sessione sua. Le correzioni del punto 1 e l'abito del verdetto sono solo di Chiaro: le altre
  app non hanno verdetti né l'arco.

### Come è stato verificato

- Ogni widget disegnato a ogni dimensione offerta, in italiano e in inglese, prima e dopo, con un
  test temporaneo poi rimosso; guardato ogni foglio.
- I testi misurati con `measureWidgetText` (font di sistema) per ogni larghezza citata sopra.
- Test nuovi: `WidgetTypeTest`, il titolo dell'arco in `ArcSeriesTest`, `hourClockLabel` in
  `FormatsTest`, le celle in `TodayWidgetLayoutTest`, i 320 dp in due lingue in
  `SkyWidgetLayoutTest`; le tabelle di `ArcLayoutTest`, `TodayWidgetLayoutTest`,
  `WidgetWarningTest` ricalcolate con le nuove misure, ciascuna con il conto nel commento.
- `widgets.png` e `widget-day-arc.png` rigenerati e guardati.

## I widget: la revisione, punti 3 e 4 (committente, 13 ott 2026)

Il committente ha chiesto di procedere con i due punti rimasti della revisione: l'angolo delle
card preso dal sistema con la curva della luna distinguibile (3), e da cinque widget a tre (4).

### Cosa è cambiato

- **L'angolo è quello del launcher** (`system_app_widget_background_radius`), da un 24 dp
  fisso, nella card (`WidgetCard`) e nell'anteprima del selettore (`widget_preview_card`), e
  nello stesso giorno in Passo e Tempo. Il riquadro dell'arco prende il raggio interno
  (`system_app_widget_inner_radius`), e le sue curve sono ritagliate su quell'angolo.
- **La luna dell'arco è a puntini** tondi nel pallido della luna (`ArcPalette.MoonLit`; nel
  colore quieto della card dove il riquadro non ha cielo), la corsa del sole sotto l'orizzonte
  a trattini lunghi e sottili: prima erano due tratteggi quasi uguali nello stesso inchiostro,
  e dove si incrociano si leggevano come una linea sola.
- **Tre widget nel selettore**: «Colpo d'occhio», «In parole», «L'arco del giorno».
  - «Colpo d'occhio» da tre celle per due righe disegna quello che era «Le prossime ore»
    (`nowShowsHours`): la riga in alto, le ore sotto e da tre righe i giorni. Sotto quella
    misura è quello che era. Le sue impostazioni hanno ora l'intervallo del giorno e i giorni,
    ciascuno con la misura a cui compare.
  - «L'arco del giorno» ha una scelta nuova, «La card mostra»: l'arco con la sua agenda, oppure
    «I momenti che segui, con i loro verdetti», cioè quello che era «Momenti del cielo»
    (`ArcDrawing.MOMENTS`, `showsMoments`), da tre celle in su; sotto, l'arco. Con i momenti le
    impostazioni dell'arco non sono offerte (non cambierebbero niente).
  - «Le prossime ore» e «Momenti del cielo» hanno `hide_from_picker`: chi li ha già sulla
    schermata home li tiene, funzionanti e configurabili; nessuno li trova più nel selettore.
- La guida («Sono tre»), le descrizioni del selettore, il README (la tavola dei widget ora
  mostra i tre, con «Colpo d'occhio» anche a due righe e l'arco con i momenti) e VISION §5.9.

### Decisioni e deviazioni

- **Il nome resta «L'arco del giorno», non «Cielo»** come nella proposta: è il nome che il
  committente gli ha dato e quello che la guida e il README usano, e i momenti sono una sua
  impostazione, detta nella descrizione del selettore.
- **«Momenti del cielo» non si poteva sciogliere nell'agenda dell'arco**, come la revisione
  dava per scontato: l'agenda copre le prossime ventiquattro ore del sole, della luna e della
  pioggia, mentre i momenti seguiti arrivano a eclissi, picchi di stelle cadenti e pianeti a
  giorni di distanza. Diventa quindi una scelta della card, non una perdita.
- **I momenti da tre celle**, non da due: provati a due celle, l'orario e il nome uscivano
  «18:… / Gold…». Era il minimo del widget Cielo, e resta il suo.
- **I widget ritirati non sono rimossi**: togliere il receiver cancellerebbe le card dalle
  schermate home di chi le ha. `hide_from_picker` (API 31, sotto il nostro minSdk 33) è il
  modo previsto dalla piattaforma. `WidgetPreviewTest` fissa quali due sono nascosti.
- **Su Android 16 standard l'angolo di sistema è 24 dp**, lo stesso di prima, quindi le
  immagini del README non cambiano per l'angolo: cambia sui launcher che danno un raggio loro.
  Il raggio interno invece è 16 (prima l'arco usava 8): visto nello screenshot, e da lì il
  ritaglio delle curve, che agli angoli inferiori uscivano dal riquadro.
- **Lo spazio vuoto delle card alte resta** (Colpo d'occhio 4×4 tra la riga in alto e le
  strisce, i momenti a tre e quattro righe): non era nei quattro punti, ed è segnato tra le cose
  da fare.

### Come è stato verificato

- Le tre card disegnate a tutte le dimensioni, l'arco anche con i momenti, con un test
  temporaneo poi rimosso; guardato ogni foglio.
- Test nuovi: le ore di «Colpo d'occhio» in `NowWidgetLayoutTest`, i momenti in
  `ArcLayoutTest`, la scelta nel giro di codifica di `ArcSettingsTest`, i due nascosti in
  `WidgetPreviewTest`.
- `widget-day-arc.png` rigenerato (la luna a puntini, il riquadro con il raggio interno);
  `widgets.png` disegnato con le tre card, uguale pixel per pixel al precedente (le stesse
  forme, ora di due card invece che di quattro) e quindi lasciato com'era.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## «In parole»: la frase in scala (committente, 13 ott 2026)

Il committente ha chiesto di portare in scala la frase di «In parole», l'unica misura rimasta
fuori dopo la revisione (18 sp, decisione sul dispositivo del 19 set), con un'anteprima prima e
dopo.

- `TextSentenceSp` = `WidgetType.Body`, 16 sp. Frase e fatti (luogo, intervallo, parola
  dell'allerta) hanno ora la stessa misura e si distinguono per peso e inchiostro, Medium e
  forte sopra Regular e quieto, come in «Colpo d'occhio» da sempre. Il numero sopra resta
  diverso su tutti e tre gli assi. `WidgetTypeTest` non ha più eccezioni.
- Ogni riga di frase restituisce 2,64 dp, che vanno al numero e all'icona: sul 2×2 il numero
  arriva al suo tetto di 56 sp (era 53,97), sul pannello 4×2 la fascia dell'icona passa da
  71,24 a 76,52 dp. Le tabelle di `TextWidgetLayoutTest` sono ricalcolate, con il conto nei
  commenti; «i gradi sono ordinati» ora chiede frase ≥ fatti.
- Alternativa scartata: tenere la differenza di misura scendendo i fatti a 13. Il committente
  li aveva portati a 16 sul dispositivo il 19 set («la località un pochino più grande»), e la
  card avrebbe perso proprio quello.
- Stessa modifica in Passo, la cui card «In words» aveva la stessa eccezione per la stessa
  ragione.
- Anteprima prima/dopo guardata in italiano e in inglese a ogni misura; `widgets.png`
  rigenerato.

## Le icone del cielo in scala, e la luna piena nell'agenda (committente, 13 ott 2026)

Il committente ha approvato la proposta sul punto «due disegni per stati vicini» e ha chiesto
di farla con un'anteprima, insieme alla luna piena dell'agenda dell'arco.

### Cosa è cambiato

- **Il codice 2 (poco nuvoloso) è composto** da `tools/compose_sun_cloud.py` come il codice 1:
  `sun-half-cloud-*`, lo stesso sole di `clear-day` con la nuvola di `cloudy` al 78%. Prima era
  `partly-cloudy` di Meteocons, nuvola piena con uno spicchio di sole nell'angolo: da 1 a 2 il
  sole passava da intero a spicchio e cambiava angolo, e a 32 dp lo spicchio spariva, quindi 2
  si confondeva con 3. Ora la nuvola cresce 0 → 49% → 78% → 100% e il sole intero → intero →
  coperto per un terzo → assente. `ChiaroIcons.conditionLineRes`, le anteprime del selettore e
  i test seguono.
- **Lo strumento compone ricette**, ciascuna con la sua taglia e un centro di giorno e uno di
  notte (`RECIPES`). Il poco nuvoloso: di giorno (77, 86), di notte (84, 80).
- **Tre controlli contro le schegge**: il raggio tagliato per il lungo (c'era), la punta di
  raggio rimasta nell'aria fra buco e nuvola (`tips`, nuovo) e il pezzo di falce staccato
  (`moon_fragments`, nuovo). Il primo nuovo ha trovato un difetto nel quasi sereno già
  spedito: nello stile pieno restava un puntino giallo accanto alla nuvoletta. La nuvoletta
  sale di mezza unità (y 94,5 → 94) e il puntino sparisce.
- **La luna nelle righe di elenco** (agenda dell'arco, righe del widget Cielo) usa sempre lo
  stile pieno: nello stile linea la luna piena è un anello, che a 20 dp accanto ai segni del
  verdetto sembrava un pulsante di selezione vuoto.

### Decisioni e deviazioni

- **Non `moonrise`/`moonset` per la luna dell'agenda**, che era la prima idea: erano già stati
  scartati il 9 set 2026 (a 20 dp il disco tagliato dall'orizzonte sembra «tagliato in
  basso»), e la fase reale è una scelta esplicita del committente. Resta la fase reale, nello
  stile pieno, che è anche come l'arco dipinge il disco della luna.
- **I parametri sono stati scelti con una ricerca a griglia** sui tre controlli, più la quota
  di disco coperta (circa metà), poi guardati in grande e alla misura della striscia nei
  quattro stili. A 32 dp la differenza fra 1 e 2 ora è la taglia della nuvola: più sottile del
  salto di prima, che era il difetto.
- **Gli acquazzoni restano su `partly-cloudy-*-rain`** di Meteocons: le gocce li distinguono, e
  comporre anche quelli è un lavoro a parte.
- **`partly-cloudy-day/-night` restano in `shipped_icons.py`** anche se nessuna schermata li
  nomina: il compositore ne legge la maschera.

### Come è stato verificato

- Anteprima prima/dopo della scala completa (sereno → coperto), giorno e notte, linea e pieno,
  in grande e a 40 px; anteprima prima/dopo delle zone cambiate degli screenshot del README.
- `compose_sun_cloud.py --controlla` pulito; i due nuovi controlli fatti fallire sui
  parametri vecchi (punta di raggio a y 94,5; falce in tre pezzi con il centro del giorno).
- `ComposedIconsTest` e `ConditionIconsTest` estesi al codice 2, `ArcTextTest` alla luna in
  stile pieno; README rigenerato (`today-now`, `today-week`, `settings`, `widgets`,
  `widget-day-arc`).

## Gli acquazzoni, il resto della revisione dei widget, le icone riviste tutte (committente, 13 ott 2026)

Il committente ha chiesto di sistemare gli acquazzoni, di chiudere tutti i punti rimasti della
revisione iniziale dei widget e, dopo gli acquazzoni, di rivedere tutte le icone per vedere se
sono coerenti.

### Cosa è cambiato

- **Gli acquazzoni sono il poco nuvoloso con quel che cade** (80, 81, 85, 86):
  `sun-half-cloud-rain-*` e `sun-half-cloud-snow-*`, composti da `compose_sun_cloud.py`. Lo
  stesso sole e la stessa nuvola del codice 2, con le gocce o i fiocchi di
  `partly-cloudy-*-rain/-snow` appesi al pavimento della nuvola, copiati alla lettera con le
  loro animazioni. Nello stile linea la nuvola perde il pavimento, come nei disegni di pioggia
  di Meteocons, e le gocce ne escono invece di attraversare una riga.
- **Tutto quel che cade da un cielo chiuso cade da una nuvola sola** (la revisione delle icone):
  pioggerella, pioggia, neve mista e neve prendono `drizzle`, `rain`, `sleet`, `snow` invece di
  `overcast-*`, i rovesci forti (82) `rain` invece di `extreme-rain`, il temporale
  `thunderstorms-rain` e quello possibile `thunderstorms` invece di `thunderstorms-day/night-*`.
  `overcast-*` portava la seconda nuvola scura che si leggeva come una falce (la ragione per
  cui il codice 3 l'ha lasciata il 12 ott), il temporale lo spicchio di sole che il codice 2 e
  gli acquazzoni hanno appena lasciato. Le icone sono ora due cieli e nessun terzo: sole o luna
  interi con una nuvola che cresce (1, 2, 80, 85), e una nuvola sola con quel che ne cade (3-77,
  82, 95). I disegni che nessuna schermata nomina più passano da `SHIPPED` a `PLANNED`;
  l'importatore, rieseguito su `@meteocons/svg` 3.0.0-next.10, ha riscritto solo
  `MeteoconsSets.kt` (prima, a liste invariate, il repo usciva identico al byte).
- **La banda del cielo dell'arco è continua**: dipinta colonna per colonna, con sole e luna
  dove stanno in quell'istante e il tempo raccordato fra le metà delle ore
  (`ArcSeries.weatherAt`), da una bitmap stirata con il filtro. Prima ogni campione da un quarto
  d'ora era un rettangolo con la nuvolosità della sua ora: un gradino a ogni ora e strisce di
  pochi pixel all'alba e al tramonto.
- **Le ore passate dell'arco prendono il velo del passato**: le etichette 03, 06, 09 sono
  attenuate come la banda e i percorsi, e la riga delle temperature si legge come una riga che
  comincia dove comincia la previsione.
- **Le card alte portano dati, non aria**:
  - «Colpo d'occhio» stretto da tre righe (2×3, 2×4): l'eroe del 2×2 alla sua altezza e sotto
    le prossime ore, una per riga (3 e 6); prima l'eroe stirato, metà card vuota.
  - «Colpo d'occhio» 4×4: una seconda riga di ore (le prossime 14 invece di 7), messa in conto
    per ultima e con la riga della pioggia; sul 4×3 non ci sta e la card non cambia.
  - I momenti dell'arco su una card alta: i momenti in alto e l'arco del giorno nello spazio che
    lasciano, da 72 dp; prima il blocco stava centrato in una card vuota per due terzi. La card
    Cielo nascosta tiene la sua forma.

### Decisioni e deviazioni

- **Le gocce al 95% della sorgente, non al 78% della nuvola**: rimpicciolite con la nuvola erano
  lunghe 9 unità e a misura di striscia l'acquazzone non si distingueva dal poco nuvoloso. Al
  100% la caduta animata finiva a 133,5 su una scatola di 128, oltre i 132,7 della pioggia che
  la famiglia già disegna; 0,95 è il passo tondo più grande che ci sta, e lo strumento rifiuta
  una caduta che esca più di quella (`fall_room`).
- **Il temporale non ha più la notte**: è un cielo chiuso, come la pioggia, che non l'ha mai
  avuta. L'intensità (pioggia forte, rovesci forti) resta della parola e della quantità.
- **La mattina dell'arco resta senza tempo e senza temperature**: il mapper parte dall'ora in
  corso e `WeatherRecency` toglie le ore passate, per scelta. Riempirle vorrebbe dire tenere
  nel rapporto le ore già passate di oggi (cambio del livello dati e della cache); per ora le
  etichette passate prendono il velo. Alternativa possibile: la finestra «prossime 24 ore»
  come default dell'arco.
- **L'arco sotto i momenti prende tutto lo spazio rimasto**: limitato ai 150 dp dell'arco
  sulla sua card più alta spostava solo l'aria dal fondo al centro (guardato sul 4×4).

### Come è stato verificato

- Anteprime prima/dopo: gli acquazzoni nei quattro stili, la famiglia delle condizioni intera
  (15 codici, giorno e notte, quattro stili, in grande e a 32 px), la banda dell'arco, ogni
  forma dei tre widget a ogni misura del selettore.
- `compose_sun_cloud.py` pulito con i nuovi controlli (`fall_room`, il pavimento di `cloudy`,
  la nuvola delle sorgenti uguale alla silhouette); `ComposedIconsTest` esteso (quel che cade
  alla lettera, la nuvola senza pavimento, il tratto delle gocce a 4), `ConditionIconsTest`,
  `ArcSeriesTest` (`weatherAt`), i test di layout dei tre widget (`nowTallHourRows`,
  `todayShowMoreHours`, `skyPlotHeight`).

## L'UV di un giorno di pioggia: «nelle schiarite» (committente, 13 ott 2026)

Il committente ha mandato uno screenshot della settimana di Milano: domenica 18 ottobre,
icona della pioggia, 55%, «4,3 mm di pioggia in 18 ore», e sotto «UV fino a 4 · Scotta in
circa 45 minuti». La domanda: può essere? Il numero sì, la frase no.

- **Da dove viene il 4**: è `uv_index_max`, l'ora più forte del giorno. L'UV del fornitore si
  abbassa sotto le nuvole basse e spesse, quasi per niente sotto un velo alto: nella
  registrazione di Milano del README 0,75 alle 11 del 25 set con il 100% di nuvole basse, 4,45
  alle 13 del 29 set con il 92% di nuvole quasi tutte alte, contro 4,55 alle 13 del 28 set
  sereno. A metà ottobre il sereno di Milano vale circa 3,5-4 (stima dall'altezza del sole):
  domenica il modello vede un'ora di schiarita o di nuvola sottile, in un giorno di 0,24 mm
  l'ora al 46-52% l'ora. Le ore di quella domenica non si sono potute guardare: il limite
  giornaliero dell'API, raggiunto dal container.
- **Cosa è cambiato**: `WeatherText.uvPeakMeaning(uv, giorno)` dice la conseguenza del massimo
  UV per il giorno che la riga disegna. In un giorno bagnato (la sua etichetta è
  precipitazione, che il mapper dà solo a una pioggia materiale, o un pericolo: anche il
  temporale possibile) le quattro fasce da 3 in su diventano «Nelle schiarite scotta in circa
  45 / 25 / 15 / 10 minuti…» («In sunny spells, burns in about…»). Il numero resta quello
  del modello; sotto 3 resta «Nessuna protezione necessaria». Tre lettori: i fatti del giorno
  aperto, la riga UV del riepilogo del mattino, quella di domani del riepilogo della sera.
  La tessera UV dei dettagli no: dice l'indice di adesso, che è già quello sotto le nuvole
  di adesso.

### Decisioni e deviazioni

- **Solo il giorno bagnato, non il coperto**: un coperto può essere un velo di cirri, che
  lascia passare quasi tutto l'UV (il 29 set sopra), e «nelle schiarite» lo sottovaluterebbe.
  Nemmeno la nebbia: la mattina nebbiosa di pianura ha il sole a mezzogiorno, e il massimo è
  quello.
- **Il possibile non è bagnato**: `RAIN_LIKELY` e `SNOW_LIKELY` non etichettano mai un giorno
  (il mapper li fa votare con il loro cielo); se succedesse, resterebbero la frase normale.
- **Alternativa scartata: togliere la riga nei giorni bagnati.** Nasconde un dato vero, e le
  schiarite di un giorno instabile sono proprio quelle in cui ci si scotta senza pensarci.
- **Restano le fasce larghe**: «45 minuti» vale da 3 a 5 (circa 55 e 33 minuti agli estremi
  per una pelle chiara). Non toccate qui: è un'altra domanda.
- Nessuna immagine del README cambia: la registrazione di Milano non ha giorni bagnati.

### Come è stato verificato

- `UvPeakMeaningTest`: il giorno asciutto (sereno, coperto, nebbia) tiene la frase normale;
  il giorno bagnato prende le fasce «nelle schiarite» banda per banda; sotto 3 nessuna
  protezione, bagnato o no; pioggia, rovesci, neve e temporale possibile sono bagnati, la
  pioggia probabile e un codice sconosciuto no.
- `AlertNotifierTest`: la riga di domani in un giorno di pioggia è «Tomorrow's peak UV 5 · In
  sunny spells, burns in about 45 minutes»; i test esistenti (domani coperto, UV 5) invariati.

## Cielo: la revisione della schermata, e la notte che si esplora col dito (committente, 14 ott 2026)

Il committente ha chiesto una revisione della schermata Cielo (grafica, UI/UX, aderenza a
Material 3 Expressive) e ha deciso di attuarla tutta. Il piano, nell'ordine in cui la si fa:

- [x] **La striscia della notte si esplora col dito** (il «tocco wow» della revisione).
- [x] **I segni della striscia** (punto 8): la luna distinta dalle nuvole, le tacche delle
  ore, «adesso».
- [-] ~~«Se non stanotte, quando?»~~: **scartata** dal committente (14 ott 2026): oltre due
  o tre giorni la previsione delle nuvole è troppo poco precisa per indicare una notte.
- [x] La bussola «dove guardare» per le righe che hanno una direzione.
- [x] 1. Un hero che risponde sempre (anche la notte in cui la luna c'è tutta la notte).
- [x] 2. Il prossimo momento in evidenza; gli altri in una lista a gruppi.
- [x] 3. Gli orari in colonna.
- [-] 4. Il movimento: chiavi nelle liste, `animateItem`, `MotionScheme.expressive()`.
  **Rimandato** (committente, 14 ott 2026): vedi «Material 3 Expressive, un passaggio a
  parte» sotto.
- [x] 5. «Aggiungi momento» come FAB esteso; i promemoria fuori dalla pagina.
- [x] 6. Una sola luna: la stessa percentuale e la stessa fase ovunque nella pagina.
- [x] 7. Le icone ripetute (gli sciami, la stella per cinque cose diverse).
- [x] 9. «Annulla» dopo un'aggiunta o una rimozione.
- [x] 10. Il bersaglio del selettore del luogo a 48 dp (la top app bar flessibile aspetta
  `material3` 1.5 stabile: la 1.4.0 del BOM non ha `MaterialShapes`, `ButtonGroup`,
  `FloatingToolbar` né le top app bar flessibili; ha `MotionScheme` e `ShortNavigationBar`).

### Cosa è cambiato

- **Un dito sulla striscia chiede un istante della notte.** Un trascinamento orizzontale
  parte subito (la pagina scorre in verticale, i due gesti non si contendono il dito);
  tenere fermo il dito chiede la pressione lunga, così uno scorrimento verticale che comincia
  sulla striscia non fa lampeggiare una lettura. Sotto il dito: un cursore bianco con il suo
  pomello; sopra la striscia, la riga che finché nessuno tocca dice «Trascina per vedere ogni
  ora» diventa la lettura di quell'istante, «02:05 · nuvole 5% · senza luna», con la pioggia
  dal 20% in su; e il fondo della card si ridipinge con il cielo di quell'istante: il sole e
  la luna dove stanno allora, le nuvole e la pioggia di quell'ora. La luna nell'angolo si
  attenua al 35% quando l'istante la ha sotto l'orizzonte. Alzato il dito, torna la notte
  intera.
- **I dati**: `NightHour` porta anche la probabilità di precipitazione dell'ora (nullable,
  come ovunque); `Tonight.samples` sono il sole e la luna ogni 10 minuti dal crepuscolo
  all'alba, calcolati da `SkyStateBuilder` con `AstronomyEngine`.
- **I segni**: le ore della luna in `SkyPalette.MoonWash` (paglierino) e non più nel bianco
  delle nuvole, con il disco della luna nella sua fase dentro il tratto; una tacca a ogni ora
  intera; «adesso», quando la notte è in corso.
- **Accessibilità**: la striscia non è più muta. Per TalkBack è un controllo regolabile a ore
  intere, il cui stato è la lettura: le nuvole ora per ora sono un'informazione che nessuna
  frase sopra di lei porta.
- La guida del Cielo («Stanotte») dice il gesto, IT/EN.

### Decisioni e deviazioni

- **Il dito che si alza riporta la notte intera**, come i grafici a scorrimento di sistema:
  la card parla della notte, e una lettura rimasta appesa dopo il gesto la farebbe parlare di
  un'ora senza che nessuno lo stia chiedendo. TalkBack è l'eccezione, perché lì non c'è un
  dito da tenere giù: la lettura scelta resta finché la si cambia.
- **Il tempo si posa sui cinque minuti dell'orologio**: la previsione sotto è oraria, e
  «02:07» sarebbe una precisione che non ha. Il crepuscolo e l'alba, che hanno minuti
  dispari, si raggiungono ai due capi (entro mezzo passo da un capo, il capo).
- **La luna della lettura è quella della striscia**, non l'altezza calcolata: la finestra è
  la parte della notte senza luna, e la riga non può dire «luna su» sopra un tratto che la
  banda disegna buio. L'altezza calcolata serve solo a quanto la luna schiarisce il cielo, ed
  è tenuta sopra o sotto l'orizzonte secondo quel racconto.
- **Un suggerimento fisso sopra la striscia** costa una riga alla card. Senza, il gesto non
  si scopre: niente nella striscia dice che risponde al dito.
- **La paglia e non l'oro**: l'oro si sarebbe confuso con la cornice ambra del «così così»;
  la paglia è un riempimento tenue, la cornice una linea piena.

### Come è stato verificato

- `TonightNightTest`: la lettura prende nuvole e pioggia dell'ora che contiene l'istante; un'ora
  che la previsione non copre si legge senza nuvole, non con uno zero; la luna della lettura
  è quella della striscia, e il bordo della finestra è buio; un punto della striscia si posa
  sui cinque minuti e non esce mai dalla notte; le tacche sono le ore intere strettamente
  dentro la notte; il cielo tra due campioni si legge tra i due; la notte è campionata dal
  crepuscolo all'alba, alba compresa.
- La card disegnata con Robolectric a metà gesto (ora con la luna, ora buia, notte di luna
  piena, «adesso» nella notte), guardata immagine per immagine; le immagini del README
  rigenerate e guardate.

## Cielo: la bussola «Dove guardare» (committente, 14 ott 2026)

Il secondo punto della revisione del Cielo. Le righe che dicono da che parte guardare
(«verso ovest») ora portano a una bussola che fa girare il lettore dalla parte giusta. Il
committente ha chiesto anche l'avviso di calibrazione con il gesto da fare, e lo proverà sul
suo telefono: in un container non c'è un sensore, quindi qui è verificata la matematica e il
disegno, non l'ago vero.

### Cosa è cambiato

- **La pagina di una riga con una direzione** (quella che si apre toccandola) comincia con
  «Dove guardare · Verso ovest» e il pulsante «Apri la bussola». La bussola prende il posto
  della pagina nel foglio, aperto del tutto; indietro torna alla pagina. Le righe senza
  direzione (quasi tutte, per scelta: `SkySights.bearing`) non hanno il riquadro.
- **La bussola**: il quadrante gira con il telefono, il segno dell'evento sul bordo dove
  accadrà, un indice fisso in alto che è il telefono, e una riga sola: «Gira a destra», «Gira
  a sinistra», «È alle tue spalle: voltati», «Ci sei: è davanti a te» (con una vibrazione di
  conferma). Funziona con il telefono in piano (punta il bordo alto dello schermo) e dritto
  come per una foto (punta la fotocamera).
- **La calibrazione**: quando il magnetometro si dichiara inaffidabile o poco preciso, un
  riquadro mostra l'8 disegnato con un telefonino che lo percorre girando il polso, e le
  parole del gesto; sparisce quando il sensore torna a posto. Lo stesso gesto è sempre a un
  tocco, «La direzione ti sembra sbagliata?», perché non tutti i telefoni lo segnalano.
- **Il disturbo**: un campo fuori da 20–70 µT per un secondo e mezzo (quello terrestre sta
  fra 25 e 65) è un oggetto di metallo o un magnete vicino, non una calibrazione: il riquadro
  lo dice e dice di spostarsi.
- `SkyUiState.Content.coordinates`, per la declinazione magnetica del luogo.
- La guida («Il marchio della macchina fotografica») dice la bussola, IT/EN.

### Decisioni e deviazioni

- **Dietro un tocco, non accesa sulla pagina**: la pagina si legge, e un sensore acceso mentre
  qualcuno legge della luce cinerea è batteria spesa per niente. I sensori si registrano alla
  ripresa e si rilasciano alla pausa, solo con la bussola a schermo.
- **Il nord vero**: le direzioni del cielo sono vere, quelle del magnetometro no (in Italia 2–5
  gradi, altrove decine): la bussola aggiunge la declinazione di `GeomagneticField` per il
  luogo. È il luogo della pagina, non la posizione del telefono: l'app non chiede la
  posizione, e alla scala di una regione la differenza non si vede.
- **Il vettore e non gli angoli di Eulero**: `getOrientation` è singolare proprio quando il
  telefono è tenuto dritto, e l'ago impazziva lì. La direzione è un vettore del telefono
  portato nel mondo dalla matrice di rotazione; il passaggio fra in piano e dritto ha un
  margine (0,4 / 0,6) perché al confine la direzione non salti.
- **«Ci sei» entro 12°, si esce a 18**: una mano a braccio teso copre una ventina di gradi, e
  una parola che lampeggia fra «ci sei» e «gira a destra» sembra una bussola rotta.
- **Il quadrante attenuato (45%) quando la bussola dubita**: l'ago continua a muoversi, perché
  il gesto si fa meglio guardandolo assestarsi, ma non finge una sicurezza che non ha.
- **Il limite onesto dell'avviso**: l'accuratezza che Android riporta dipende dal produttore;
  alcuni telefoni non dicono mai «bassa». Per questo l'aiuto a richiesta, e il disturbo
  misurato sull'intensità del campo, che non dipende da quel segnale.
- **Mai i gradi**, come sulle righe: il quadrante è il disegno del numero.
- **Nessuna immagine nuova nel README**: è una vista dentro un foglio, e l'insieme resta
  piccolo.

### Come è stato verificato

- `CompassMathTest`: il telefono in piano punta il bordo alto dello schermo, e la rotazione
  dello schermo sposta quel bordo; dritto punta la fotocamera, comunque sia girato lo schermo;
  la postura cambia con il margine; la svolta è la via corta, destra positiva; «ci sei» è più
  facile da tenere che da prendere, e oltre 150° è «alle spalle»; solo un campo che la Terra
  non può fare è un disturbo; inaffidabile e bassa chiedono il gesto, media e alta no; la
  media attraverso il nord resta a nord.
- La bussola disegnata con Robolectric e sensori simulati (girarsi, ci sei, da calibrare,
  disturbata, senza bussola) e la pagina con «Dove guardare», guardate immagine per immagine.
- Da provare sul telefono del committente: l'ago vero, il gesto, il telefono dritto.

## Cielo: l'hero che risponde sempre, il prossimo momento, gli orari in colonna (committente, 14 ott 2026)

I punti 1, 2 e 3 della revisione del Cielo.

### Cosa è cambiato

- **Punto 1, l'hero.** Una notte senza finestra buia apre con una parola al posto del
  verdetto: «Notte di luna», «Mai del tutto buio», «Buio tutto il giorno», con sotto le
  nuvole medie della notte. Gli orari sono grandi e tabellari, sotto la loro etichetta
  («Finestra buia», o «Cielo scuro» nella notte della luna); la prosa segue, più piccola.
  `sky_tonight_moon_all_night` perde gli orari, che ora stanno sopra di lei.
- **Punto 2, il prossimo momento** esce dal suo giorno e diventa una card in cima alla
  sezione: «Il prossimo, oggi», il nome, l'orario grande, fra quanto e da che parte, il
  verdetto, la campanella. Gli altri momenti e il calendario «In arrivo» stanno in gruppi di
  righe (segmenti `surfaceContainer` a 2 dp, angoli esterni 20 e interni 4).
- **Punto 3, gli orari in colonna**: l'inizio di ogni momento a destra del nome, prima della
  campanella; la fine di una finestra nella riga sotto, «fino alle 19:15».

### Decisioni e deviazioni

- **La parola della notte senza finestra non è un verdetto**: niente segno, niente colore.
  I verdetti giudicano una finestra e non ce n'è una; «Niente da fare» in rosso sarebbe
  falso proprio la notte in cui la luna è lo spettacolo. Era la riserva detta in revisione:
  la parola resta nella card, non va nelle notifiche né nei widget.
- **La percentuale della luna una volta sola**: il disco accanto la porta già, e la prima
  prova la stampava tre volte (disco, riga sotto la parola, prosa).
- **La card del prossimo in `surfaceContainerHigh`, non in un contenitore colorato**: la
  card della notte è già la cosa forte della pagina; due cose forti non sono una gerarchia.
- **Righe fatte a mano e non `ListItem`**, e **il verdetto sotto l'icona, non sotto il
  nome**. Con i gruppi la riga perde 32 dp. Sotto il nome il verdetto aveva 241 dp a 360,
  e il più largo non è più il «pioggia 100%» da 236 misurato il 12 set: con lo strato delle
  nuvole (24 set) «✗ Niente da fare  nuvole 86%, basse» vuole circa 262, e nella prova a
  360 dp il numero è andato sotto la parola. Dal bordo dell'icona la riga ha 304 dp a 360.
  La riga di prima era già a 1 dp dall'andare a capo (261).
- **Le date del calendario non vanno in colonna**: sono giorni, non minuti, e restano nella
  riga sotto il nome.

### Come è stato verificato

- `TonightNightTest`: le nuvole di una notte intera sono la media delle sue ore, e senza ore
  non c'è un numero.
- La pagina disegnata con Robolectric in chiaro a 384 dp e in scuro a 360 dp, e l'hero nei
  tre stati senza finestra (IT ed EN), guardati immagine per immagine; le immagini del
  README rigenerate e guardate.

## Material 3 Expressive, un passaggio a parte (committente, 14 ott 2026)

Il punto 4 della revisione del Cielo (`MotionScheme.expressive()` su `MaterialTheme`, le
chiavi e `animateItem` nelle liste) è **rimandato** a un passaggio dedicato, per due ragioni:

- **È di tutta l'app, non del Cielo.** Lo schema di movimento cambia ogni componente
  Material (interruttori, fogli, barra di navigazione, pulsanti): va guardato schermata per
  schermata, non dentro una revisione di una.
- **È della famiglia, non di Chiaro.** Chiaro, Passo, Saldo e Tempo condividono un
  linguaggio (la transizione fra le pagine è già quella di Saldo, §7 di DESIGN). Un
  aggiornamento a Material 3 Expressive fatto in una sola delle quattro le farebbe divergere;
  va deciso e fatto per tutte, con gli stessi valori.

Quando si farà, il punto di partenza misurato il 14 ott 2026:

- `material3` 1.4.0 (BOM 2026.02.01) ha `MotionScheme` (standard ed expressive) e
  `ShortNavigationBar`; **non** ha `MaterialShapes`, `ButtonGroup`, `LoadingIndicator`,
  `FloatingToolbar` né le top app bar flessibili, che sono nella 1.5 ancora alpha. Il
  passaggio aspetta la 1.5 stabile, o decide consapevolmente di non aspettarla.
- `ChiaroMotion` ha già le tre molle di §7 (spatial, spatial fast, effects) e il ripiego a
  100 ms per chi riduce le animazioni: va riconciliato con `MotionScheme`, non affiancato.
- Le liste del Cielo non hanno chiavi (`items(n)`), quindi aggiungere o togliere un momento
  salta invece di muoversi: è la prima cosa da sistemare, ed è indipendente dallo schema.
- Da guardare nello stesso passaggio: la top app bar flessibile con il luogo come
  sottotitolo (punto 10 della revisione del Cielo), le forme espressive dove dicono qualcosa.

## Cielo: il pulsante, una luna sola, i segni del cielo (committente, 14 ott 2026)

I punti 5, 6 e 7 della revisione del Cielo.

### Cosa è cambiato

- **Punto 5.** «Aggiungi un momento» è un FAB esteso in basso a destra, che resta solo icona
  quando si scorre nella lista; la lista lascia 88 dp sotto l'ultima riga. La sezione
  «Promemoria» in fondo alla pagina non c'è più: «Promemoria abituale» e «Anche a cielo
  coperto» stanno in Impostazioni, gruppo «Promemoria del cielo» (`SettingsViewModel`
  ripianifica gli allarmi come faceva il Cielo, anche dopo il ripristino); la nota «possono
  arrivare qualche minuto prima o dopo» sta nella finestra dell'anticipo, e quella di un
  momento dice dove si cambia l'abituale. `SkyActions` perde i due setter.
- **Punto 6.** `MoonOfTheNight` in `:core:domain`: la luna di una data si legge a metà della
  sua notte astronomica (a mezzanotte dove non c'è), per la card, per «La luna di oggi» e
  per il widget. Una fase che nomina un quarto porta il suo giorno e la sua ora: «Luna piena
  domani alle 18:49 · 99% illuminata». L'occorrenza di «La luna di oggi» resta a
  mezzogiorno: è l'ora del suo promemoria, e quella non cambia.
- **Punto 7.** `tools/compose_sky_marks.py` compone tre segni dai disegni di Meteocons e
  scrive `ui/icons/SkyMarks.kt`: la meteora per gli sciami, la luna con la stella per le
  congiunzioni della luna, le due stelle per Venere e Giove. CLAUDE.md, DESIGN §13 e
  `shipped_icons.py` lo dicono.
- La guida (momenti, campanella, catalogo) dice la card del prossimo, dove si sceglie il
  promemoria abituale e da dove si aggiunge un momento.

### Decisioni e deviazioni

- **I promemoria in Impostazioni e non nella finestra della campanella**: una preferenza che
  vale per tutti i momenti non è contenuto della pagina di stanotte, e la finestra di un
  momento sceglie per quel momento. La finestra però lo dice, perché chi cercava la riga in
  fondo alla pagina sappia dove è andata.
- **Il FAB primario di Material, non un colore nostro**: è il componente, con il suo
  contenitore; il FAB esteso in cima alla pagina copre il bordo di una riga, che è il
  comportamento di un FAB, e si ritira appena si scorre.
- **La luna a metà della notte, non a mezzogiorno**: è la luna che si guarda, e quella che la
  card ha sempre usato; spostare la card a mezzogiorno avrebbe messo la riga d'accordo con
  un istante in cui la luna di solito non si vede. Il widget segue, perché la home e l'app
  non stampino due fasi.
- **Il quarto con giorno e ora solo per le quattro fasi che lo nominano**: «gibbosa
  crescente» è un tratto del ciclo, non un istante, e non ha niente da precisare.
- **Tre segni e non di più.** Venere e Giove restano la stella (è il loro nome a occhio
  nudo); le ore blu e i crepuscoli nautici anche (lo stesso cielo delle prime stelle, e
  Meteocons non disegna il sole appena sotto l'orizzonte). Gli sciami restano uno per riga
  con lo stesso disegno: sono la stessa cosa in date diverse, e raggrupparli in una riga
  sola avrebbe nascosto le date e le pagine.
- **Le immagini del README**: «sky-ahead» ora parte dalla riga della guida, perché senza la
  sezione dei promemoria la lista finisce prima e lo scorrimento si fermava con quella riga
  tagliata sotto l'intestazione.

### Come è stato verificato

- `MoonOfTheNightTest`: la luna di una data si legge a metà della sua notte, a mezzanotte
  dove la notte non c'è; una fase principale porta il suo quarto, entro due giorni, ed è lo
  stesso istante del calendario; le fasi intermedie non ne hanno.
- `SkyStateBuilderTest`: la card e «La luna di oggi» stampano la stessa percentuale, e il
  quarto della luna piena del 25 set 2026 cade il 26.
- `SkyMarksTest`: le quattro facce di ogni segno, nelle tabelle, senza gemelli animati; i pezzi
  uguali alle sorgenti alla lettera, percorsi e colori; la meteora senza la luna; i contorni
  della famiglia. `SkyMomentIconTest` e `MeteoconsSetsTest` aggiornati.
- La pagina del Cielo, le Impostazioni e i segni in tutte le facce disegnati con Robolectric e
  guardati; le immagini del README rigenerate e guardate.

## Cielo: «Annulla» e il selettore del luogo (committente, 14 ott 2026)

I punti 9 e 10, gli ultimi della revisione del Cielo. Restano aperti solo il punto 4,
rimandato al passaggio Material 3 Expressive di famiglia, e «Se non stanotte, quando?»,
scartato.

### Cosa è cambiato

- **Punto 9.** Aggiungere o togliere un momento dalla sua pagina chiude il foglio e mostra una
  snackbar con «Annulla». Annullare una rimozione rimette la riga **com'era**, con il suo
  anticipo: `SkySubscriptionStore.restore`, perché `add` lo dimentica; annullare un'aggiunta
  la toglie. La schermata è uno `Scaffold`, che alza il FAB sopra la snackbar. Nel catalogo
  niente snackbar (il segno della riga cambia sul posto, e il foglio modale la coprirebbe), ma
  una riga tolta e ritoccata nello stesso foglio torna con il suo anticipo.
  `SkyUiState.Content.subscriptions` porta le righe come il negozio le tiene.
- **Punto 10.** `ui/components/TabHeader.kt` sostituisce le tre copie dell'intestazione di
  Cielo, Avvisi e Diario. Il bersaglio è il nome della scheda e il luogo insieme, 48 dp di
  altezza e almeno 48 di larghezza, un pulsante solo per TalkBack. Il testo non si sposta:
  i dati del README non cambiano.

### Decisioni e deviazioni

- **Tutte e tre le schermate, non solo il Cielo**: il difetto era lo stesso in tre copie, e
  correggerne una avrebbe lasciato le altre due a 20 dp. Un componente solo perché la
  prossima correzione arrivi a tutte.
- **Il bersaglio è il blocco intero e non una riga alta 48**: una riga del luogo alta 48
  avrebbe allungato l'intestazione di 28 dp; il nome più il luogo fanno già 48, ed è lì che
  un dito cerca «questo luogo».
- **La snackbar solo per le azioni che chiudono un foglio**: dove il segno cambia sotto il
  dito, la ricevuta c'è già e lo stesso tocco è l'annullamento.
- **`Long`**: la snackbar porta un'azione, e con TalkBack raggiungerla richiede tempo.

### Come è stato verificato

- `SkySubscriptionStoreTest`: ripristinare rimette la riga com'era, anticipo compreso, e una
  volta sola.
- `SkyUndoTest`: dalla pagina di «Tramonto» con un anticipo di 30 minuti, togli → la snackbar
  → «Annulla» → la riga torna con i suoi 30 minuti; il selettore del luogo è un bersaglio di
  almeno 48 × 48 dp, e senza un luogo il titolo non è un pulsante.
- Suite completa e lint; le immagini del README rigenerate: nessuna cambia.

## La guida dopo la revisione del Cielo (committente, 14 ott 2026)

Riletta per intero contro le cinque passate della revisione. Le parti già aggiornate lungo la
strada (la striscia da esplorare, «Dove guardare», la campanella, il catalogo, la card del
prossimo) reggono; erano rimaste indietro quattro cose.

- **«Stanotte»**: la notte senza finestra ha una parola al posto del verdetto, gli orari sono in
  grande, e la striscia ha le sue tacche, la luna color paglia e «adesso». La guida lo dice.
- **«I prossimi momenti»**: i giorni come titoli e l'orario a destra, «La luna di oggi» che è la
  luna di stanotte con il giorno e l'ora del suo quarto, e «Annulla» che rimette un momento
  tolto per sbaglio con il suo promemoria.
- **«I tuoi luoghi»**: diceva che il nome in cima a Oggi e a Cielo apre i luoghi; lo apre il
  nome in cima a ogni schermata, ed è così anche per Avvisi e Diario.
- **«Impostazioni»**: mancava il gruppo «Promemoria del cielo». La frase sul ripristino, nella
  guida e nella finestra di conferma, dice ora che tornano ai valori iniziali anche i promemoria
  del cielo, perché è vero: `SettingsStore.resetToDefaults` svuota tutto il negozio.
- **L'esempio della riga di un momento** era una copia della riga di inizio settembre (icona di
  26 dp in grigio, giorno e ora nella riga). Ora lo disegna `SkyRow` stessa, resa `internal` con
  `onOpen` facoltativo e il margine come parametro: l'esempio è la riga, non una sua imitazione.

**Il ripristino e gli avvisi** (decisione del committente, 14 ott 2026): `resetToDefaults`
svuota tutto il negozio, quindi anche gli interruttori degli avvisi pronti all'uso
(`notif_*`) e delle allerte ufficiali. **Resta così, e la frase lo dice.** È la direzione
sicura: gli avvisi valgono tutti «acceso» per difetto, quindi un ripristino può solo
riaccendere un avviso spento, mai zittirne uno. La finestra di conferma e la guida dicono
ora «tutto torna come al primo avvio… gli avvisi che avevi spento si riaccendono», e non
«si azzerano», che in italiano si legge come «si spengono».

Perché la frase sia vera subito, `SettingsViewModel` ora ripianifica il lavoro periodico
(`SyncScheduler.reconcile`) dopo il ripristino: prima lo faceva solo all'avvio del processo,
e un avviso riacceso restava senza il lavoro che lo controlla fino al riavvio dell'app. Lo
stesso valeva per il cambio di frequenza, che ora si applica subito. (La chiave dell'opacità
dei widget nello stesso negozio non è letta da nessuna schermata: le card hanno ognuna il
proprio aspetto, e la frase non la nomina.)

## Avvisi: la revisione della schermata, punti 1–6 (committente, 14 ott 2026)

Il committente ha chiesto una revisione della schermata Avvisi, grafica e UI/UX, per renderla
coerente con il Cielo appena rivisto, e ne ha fatto attuare i punti da 1 a 6. La revisione intera
è in `/mnt/project-files/review-avvisi/review-avvisi.md` del progetto; i punti:

- [x] 1. Un solo modo di raggruppare: i segmenti del Cielo, anche in Impostazioni.
- [x] 2. L'allerta in vigore come la card forte della pagina.
- [x] 3. Gli orari in colonna.
- [x] 4. Una scala tipografica sola, quella del Cielo.
- [x] 5. La regola nata da un'idea porta il disegno dell'idea.
- [x] 6. La frase della regola: gli intervalli, le soglie in grassetto.
- [x] 7. Un hero che risponde sempre (lo stato delle allerte, il prossimo avviso, la striscia).
- [x] 8. La striscia del giorno con le tacche delle ore e «adesso» del Cielo (senza la corsia delle
  ore dei tuoi avvisi: vedi «Punto 8»).
- [x] 9. «Nuovo avviso» come FAB, le idee in un foglio; la fila resta solo con «I tuoi» vuoto.
- [x] 10. «Annulla» al posto della conferma per eliminare.
- [x] 11. Lo stato di ogni regola adesso, con il suo numero.
- [x] 12. La prova dell'editor che si aggiorna a ogni chip.
- [ ] 13. «Quando scatterebbe»: la regola provata sulla settimana di previsione. **Non si fa**
  (committente, 14 ott 2026): vedi «Punto 8».

### Cosa è cambiato

- **Punto 1.** `ui/components/SegmentedGroup.kt`: `SegmentedGroup` (una lista di righe, un
  segmento `surfaceContainer` ciascuna, 2 dp fra l'una e l'altra) e `segmentShape`, che era del
  Cielo e ora è di tutti (il Cielo e la guida lo importano da lì). In Avvisi l'interruttore delle
  allerte con la sua soglia, i quattro pronti all'uso e le regole sono gruppi di segmenti, senza
  filetti; le regole erano card sciolte da 24 dp. Impostazioni passa ai segmenti nello stesso
  cambio: copiava il gruppo di Avvisi perché le due schermate sembrassero una app sola. Le card
  che stanno da sole (la striscia del giorno, la risposta del bollettino, la guida, l'anteprima
  dell'aspetto, la privacy) prendono l'angolo delle card del Cielo, 28 dp.
- **Punto 2.** Con un'allerta in vigore, Avvisi disegna `ActiveWarningCard` e non più il banner
  di Oggi: card da 28 dp nel contenitore del livello, la zona in alto con il segno, i pericoli
  in `headlineSmall`, i giorni, chi l'ha detto; apre lo stesso foglio. «Nessuna allerta» e i tre
  stati d'attesa sono la stessa card da 28 dp su `surfaceContainerHigh`.
- **Punto 3.** La fascia dei tre avvisi a orario («15–17», «6–12», «18–23») sta sopra
  l'interruttore, `titleMedium` tabellare; la frequenza chiude la descrizione. Le stringhe
  `alert_summary_when`, `alert_evening_when` e `warning_switch_when` perdono le ore, che dicono
  la colonna.
- **Punto 4.** Nome della riga in `bodyLarge`, righe sotto in `bodyMedium` `onSurfaceVariant`,
  etichetta di card in `labelLarge` `primary`, «Parti da un'idea» come i titoli dei giorni del
  Cielo.
- **Punto 5.** `ruleIcon` cerca prima l'idea con le stesse condizioni, lo stesso confronto che
  scrive «Già aggiunto».
- **Punto 6.** `RuleText.sentenceParts`: due condizioni sulla stessa grandezza sono una frase,
  «fra 5° e 26°» per almeno/al massimo, «sopra 0° e sotto 5°» per le due strette; le soglie sono
  pezzi a parte, che la card scrive in `SemiBold` e `onSurface`. La guida dice la card in cima ad
  Avvisi.

### Decisioni e deviazioni

- **Le ore sopra l'interruttore, non accanto al titolo** come nella revisione: la colonna del
  testo di una riga di Avvisi è di 180 dp a 360 (la riga ha il disegno da 36 e l'interruttore da
  52), e nella prova «Il riepilogo del mattino» con «6–12» accanto andava a capo. Sopra
  l'interruttore le ore stanno comunque in colonna, sul bordo della riga.
- **«6–12» e non «06:00–12:00»**: sono le ore della striscia del giorno subito sopra (0, 6, 12, 18,
  24), e un orario intero per capo non ci sta nella colonna dell'interruttore. Le fasce sono
  finestre, non minuti: il riepilogo arriva quando gira l'aggiornamento dentro la fascia.
- **«Fra» solo per almeno e al massimo**: sono i due limiti che la parola dice esattamente.
  Due limiti stretti tengono le loro parole («sopra 0° e sotto 5°»), con la grandezza detta una
  volta; due limiti che non lasciano niente in mezzo non si chiamano intervallo. Cambiano solo
  le parole: la regola salvata è la stessa, e le righe della notifica (una per condizione, con la
  sua lettura) non cambiano.
- **Impostazioni nello stesso cambio**: con un gruppo diverso da Avvisi e dal Cielo l'app
  avrebbe avuto due modi di raggruppare le righe, che è il difetto del punto 1.
- **L'anteprima dell'aspetto resta una card a sé** sopra il gruppo dell'aspetto, non il suo primo
  segmento: è un disegno, non una scelta.

### Come è stato verificato

- `RuleSentenceTest` (Robolectric, italiano): la corsa dice «la temperatura adesso fra 5° e 26°»
  con le tre soglie come pezzi a parte; la corsa al freddo «sopra 0° e sotto 5°»; due limiti che
  non lasciano niente in mezzo non diventano «fra»; una regola con una condizione per grandezza
  si legge come prima.
- Avvisi disegnata con Robolectric a 360 dp in italiano, in chiaro e in scuro, con due regole,
  al primo avvio e con un'allerta arancione e gialla in vigore; Impostazioni in chiaro. Guardate
  immagine per immagine; le immagini del README rigenerate e guardate.
- Suite completa e lint.

### Punti 7, 9 e 10 (committente, 14 ott 2026)

Chiesti dopo i primi sei, nella stessa PR.

**Cosa è cambiato**

- **Punto 7.** `AlertsHero`: una card da 28 dp su `surfaceContainerHigh` in testa alla pagina, dove
  c'erano «Quando arrivano» e, sotto il titolo «Allerte ufficiali», la card dello stato. In alto la
  risposta delle allerte (`WarningAnswer`, uno scheletro per ogni stato: segno e zona, la
  risposta, ciò che la regge); con un'allerta in vigore quel blocco prende il contenitore del
  livello per tutta la larghezza della card. Sotto, solo con un avviso a orario acceso, la fascia
  in cui il telefono può suonare per prima (`NextWindowRow`, sul modello della card del prossimo
  momento) e la striscia del giorno, che perde la sua card e il suo titolo (`alerts_day_title`
  tolta). `nextWindow` in `NextWindow.kt` sceglie la fascia: quella aperta adesso, altrimenti la
  prossima di oggi, altrimenti la prima di domani.
- **Punto 9.** `ExtendedFloatingActionButton` «Nuovo avviso» in uno `Scaffold`, esteso in cima e
  ridotto all'icona quando si scorre, assente al massimo delle regole. Apre `IdeasSheet`: «Da zero»
  in testa, poi «Parti da un'idea» e le idee come righe del catalogo del Cielo. La fila delle idee
  sulla pagina resta solo con «I tuoi» vuoto. L'idea vuota si chiama «Da zero» («From scratch»),
  perché «Nuovo avviso» è la parola del FAB.
- **Punto 10.** Niente più finestra di conferma (`rule_delete_confirm_*` tolte): «Elimina questo
  avviso» chiude il foglio e mostra ««Bici» eliminato · Annulla», `Long`. `RuleStore.restore`
  rimette la regola com'era e dov'era, con il suo id; `AlertsViewModel.restore` la chiama.
- Guida: «I tuoi» dice che «Nuovo avviso» apre le idee e «Da zero», e che l'eliminazione si
  annulla. VISION §5.4 allineata.

**Decisioni e deviazioni**

- **La regola torna con il suo id**, non con uno nuovo. Gli id non si riciclano perché una regola
  nuova non erediti lo stato di una tolta; qui è la stessa regola che torna, e con il suo id
  ritrova i suoi latch e le impronte di ciò che ha già mandato oggi, quindi non scatta una seconda
  volta. Torna nella sua posizione, e non torna se nel frattempo la lista è piena.
- **Torna com'era nel foglio**, nome e messaggio non ancora salvati compresi: è quello che il
  lettore aveva davanti quando ha premuto «Elimina».
- **Il prossimo è una fascia, non un avviso.** I riepiloghi arrivano una volta dentro la loro
  fascia e la pagina non sa se quello di oggi è già arrivato: dice le ore in cui il telefono può
  suonare, che sa, e non promette che suonerà. Le regole del lettore con orari propri non entrano
  nella riga: le loro ore sono sulla loro card (e il punto 8 non le porta nella striscia).
- **«Il bollettino»** come nome della fascia delle 15–17 nella riga e nella descrizione della
  striscia: «Avvisami delle allerte», il titolo dell'interruttore, è un comando e non il nome di
  qualcosa che arriva, e «Il bollettino delle allerte» andava a capo a 360 dp.
- **La fila delle idee resta come stato vuoto**, come dice la revisione: senza, «I tuoi» vuoto
  sarebbe un titolo sopra il nulla; con una regola, le idee sono nel foglio del FAB.
- **Gli stati d'attesa sotto «Allerte ufficiali»**: senza una zona da nominare l'etichetta del
  blocco dice di cosa si sta parlando, e la risposta resta in `titleMedium`, perché «Ancora nessun
  bollettino» non è una risposta da leggere da lontano.

**Come è stato verificato**

- `NextWindowTest`: prima dell'alba la fascia del mattino oggi, dentro una fascia quella aperta
  fino al suo ultimo minuto, dopo l'ultima la prima di domani, fasce spente assenti, niente
  acceso niente detto. `RuleStoreTest`: `restore` rimette la regola al suo posto con il suo id, e
  il prossimo id resta nuovo; non la raddoppia e non passa il massimo.
- Avvisi disegnata con Robolectric a 360 dp in italiano: in chiaro con due regole, in scuro con
  un'allerta arancione e «I tuoi» vuoto, in attesa del bollettino alle 22, il foglio delle idee,
  la snackbar con il FAB sollevato. Guardate immagine per immagine; le immagini del README
  rigenerate e guardate.
- Suite completa e lint.

### Punti 11 e 12 (committente, 14 ott 2026)

Chiesti dopo il 7, il 9 e il 10, nella stessa PR.

**Cosa è cambiato**

- **Il motore dice perché no.** `RuleCheck.Passes` da `data object` a `data class Passes(failed,
  value)`: la prima condizione che non tiene e ciò che ha letto. `evaluate` non cambia (il ramo è
  `is RuleCheck.Passes`); `RulePreview.WouldPass` porta gli stessi due campi.
- **Punto 11.** Ogni card di regola ha una riga di stato sotto la frase e gli orari: «Adesso sì»,
  «Adesso sì, ma fuori orario fino alle 20:00» (o «fino a domani alle 7:12»), «Adesso no · è 19,7°,
  serve sotto 5°», «Adesso non si può dire · l'indice europeo dell'aria adesso non ha un valore».
  Un punto pieno quando la regola tiene, un anello quando no o non si sa, la parola in `SemiBold`
  `onSurface`, il resto in `onSurfaceVariant`. Senza previsione per il luogo la riga non c'è.
  `AlertsViewModel` legge ogni regola contro il report in cache (`ruleReading`, la stessa funzione
  della prova) e di nuovo ogni minuto mentre la pagina è guardata.
- **Punto 12.** Il pulsante «Prova adesso» e lo scorrimento fino alla risposta non ci sono più: la
  risposta sta in testa al foglio, sopra il nome (`EditorAnswer`, la riga della card e sotto
  «Direbbe: «…»»), e si ricalcola a ogni chip, agli orari e al messaggio, quest'ultimo dopo 300 ms
  senza battute. Le stringhe `rule_preview_action`, `_fires`, `_passes`, `_unavailable` e
  `_closed*` sono tolte; la guida («È vero adesso?») dice la riga sulla card e la risposta viva.

**Decisioni e deviazioni**

- **Segno e parola, niente colore**, come chiedeva la revisione: «vera» non vuol dire «bella», e i
  colori dei verdetti del Cielo qui direbbero una cosa che la riga non dice.
- **La grandezza si nomina solo se la regola ne guarda più d'una**: su «la temperatura adesso
  sotto 5°» la riga «è 19,7°, serve sotto 5°» non ha bisogno del soggetto, che era la terza
  ripetizione di «adesso» sulla card.
- **«serve al massimo 20%» da stringhe proprie** (`rule_need_*`) e non dalle parole dei selettori:
  in inglese il selettore dice «is at most», e «it needs is at most 20%» non è una frase (visto
  nella prova in inglese).
- **Il messaggio nella chiave della risposta**, ma dopo una pausa: non cambia se la regola scatta,
  cambia ciò che la risposta cita, e la risposta deve citare quello che si sta scrivendo.
- **Ogni minuto, solo la lettura**: le regole, le impostazioni e la storia si rileggono quando
  cambiano, come prima; il minuto rifà soltanto l'aritmetica sul report già in memoria, perché
  `next_6h` scorre con l'orologio e una fascia si apre e si chiude. Niente gira a pagina chiusa.
- **Una regola spenta mostra lo stesso il suo stato**: dice se le condizioni tengono, che è ciò che
  serve per tararla prima di riaccenderla.

**Come è stato verificato**

- `RuleEngineTest`: la condizione riportata è quella che non tiene, la prima quando non ne tiene
  nessuna. `RunTemplatesTest`: la corsa sotto la pioggia è zitta per la pioggia, con il 60% letto.
  `RuleNowTest` (Robolectric, italiano): le quattro forme della riga, la virgola dei decimali, la
  forma senza soggetto, `watchesSeveral`.
- Avvisi disegnata a 360 dp in italiano in chiaro e in inglese in scuro con cinque regole (vera,
  vera fuori orario, falsa su una grandezza, falsa sulla qualità dell'aria, le due delle idee); le
  quattro risposte dell'editor disegnate a parte. Guardate immagine per immagine; l'immagine del
  README dei tuoi avvisi rigenerata e guardata.
- Suite completa e lint.

### Punto 8 (committente, 14 ott 2026)

Della proposta resta solo la parte di forma: la striscia del giorno ha le tacche delle ore intere e
«adesso» come la striscia della notte del Cielo, una linea sottile attraverso il binario con un
punto sotto (prima era il disco del nastro della luce). Le due strisce si leggono allo stesso modo.

- **Senza la seconda corsia con le ore dei tuoi avvisi.** Dopo i punti 7 e 11 ogni card dice le sue
  ore e se è fuori orario adesso: la corsia avrebbe ripetuto la stessa cosa più vaga, un avviso
  «con la luce» accende quasi tutto il giorno, e quelle ore dicono quando un avviso può parlare,
  non quando parlerà. Servirebbe solo a chi ha molti avvisi su orari diversi.
- **Senza il punto 13.** Prevedere «probabilmente domani verso le 8» è una promessa su una
  previsione che cambia, con un controllo che gira a intervalli e variabili (l'aria) senza un
  valore ora per ora; la riga del punto 11 risponde già a «perché non mi arriva».
- **Le tacche dalle 1 alle 23**, a 5 dp sul fondo del binario, `onSurface` al 30%, sopra le fasce:
  le ore 0 e 24 sono gli estremi del binario. Il punto di «adesso» sta all'altezza del tratteggio
  delle ore silenziose e si distingue da esso per forma (un disco pieno contro trattini di 2 dp).

**Come è stato verificato**

- Avvisi disegnata a 360 dp in chiaro e in scuro, di giorno e nelle ore silenziose; l'immagine del
  README degli avvisi rigenerata e guardata. Suite completa e lint.

## Diario: l'hero della settimana e le forme del Cielo (committente, 14 ott 2026)

Il committente ha chiesto una revisione del Diario (grafica e UI/UX) per renderlo coerente con il
Cielo appena rivisto, e poi di attuarne il primo passo: i punti A.1 (un hero che risponde) e A.2
(le forme). Gli altri punti della revisione (gli orari in colonna, i titoli dei giorni, le icone
delle voci, le voci che aprono la loro pagina, la striscia col dito, la memoria dei giorni chiusi)
aspettano la sua parola.

### Cosa è cambiato

- **A.1, l'hero.** La card della settimana è la testa della pagina, nella forma della card di
  stanotte: angoli 28 dp, `surfaceContainerHigh`, 20 dp di respiro. Dentro: l'etichetta «La
  settimana», **la risposta come parola** in `headlineMedium` («Sabato 17 migliora», «peggiora»,
  «si scalda», «si rinfresca», «va su e giù», «Settimana ferma»), **i suoi numeri su una riga
  loro**, tabellari, il valore di prima in inchiostro tenue e quello di adesso pieno, poi i chip,
  la striscia e il gelo. Il titolo di sezione sopra la card non c'è più: lo fa l'etichetta.
- **L'hero risponde anche nelle prime ore**: «Ancora presto», e sotto quando arriva la tendenza.
  Era una riga grigia in `bodySmall` che diceva «deriva», una parola di gergo.
- **A.2, le forme.** «Com'è andata la previsione» prende gli angoli da 28 dp e i 20 dp dell'hero,
  su `surfaceContainer`. Erano 12 dp per la settimana e 16 per la previsione, uno sotto l'altro.
- La riga del giorno che la parola nomina è evidenziata su `surfaceContainerLowest`.
- `driftStory`, la metà pura della frase: quale giorno, quale cambiamento, con quali due numeri.
  Le parole restano della schermata.
- La guida («Come si muove la settimana») dice la risposta in cima alla pagina, IT/EN.

### Decisioni e deviazioni

- **La parola al presente, «Sabato 17 migliora»**, e non «è migliorato»: è quello che il lettore
  vuole sapere del suo sabato, e il passato era della frase lunga che spiegava. I numeri sotto
  dicono da dove a dove.
- **Le massime si dicono «si scalda» e «si rinfresca», non meglio o peggio**: più caldo non è un
  giudizio (DESIGN §8.10). Una massima che va su e giù dice «va su e giù» come la pioggia.
- **Una settimana ferma non ha numeri sotto**: non c'è un giorno da nominare, e una cifra messa
  lì per riempire sarebbe la riga che la schermata rifiuta.
- **Il fondo neutro, non colorato**: la striscia sotto la parola è sulle scale della pioggia e
  della temperatura, che devono leggersi per quello che sono. Il peso viene dal tipo e dalla
  forma. La previsione resta un gradino sotto (`surfaceContainer`), perché due card dello
  stesso tono non sono una gerarchia.
- **La banda del giorno nominato passa a `surfaceContainerLowest`**: la `Highest` di prima, a un
  gradino dal nuovo fondo, nel vestito chiaro quasi spariva.
- **Nessun file di tema toccato**: le forme sono `shapes.extraLarge` e i ruoli che esistono già.
  Nello stesso momento il thread di Avvisi lavora su segmenti e scala dei tipi.

### Come è stato verificato

- `DriftStoryTest`: meno pioggia è il giorno migliore e vince lo spostamento più grande; più
  pioggia è il peggiore; un giorno che va e torna è un'oscillazione con i suoi due estremi; sotto
  la soglia del motore la settimana è ferma e non nomina un giorno; la massima è più calda o più
  fresca; una riga con una sola previsione non ha niente da confrontare.
- `JournalHeroTest` (Robolectric, 360 dp): l'etichetta e la parola ci sono, e i numeri sono un
  testo loro su **una riga sola**; nelle prime ore l'hero dice «Ancora presto»; una settimana
  ferma lo dice.
- La pagina disegnata con Robolectric in chiaro (IT, 384 dp) e in scuro (EN, 360 dp), nella
  settimana che si muove, in quella ferma e nelle prime ore, guardata immagine per immagine. Il
  README non ha un'immagine del Diario (CLAUDE.md, «Le schermate del README»), quindi nessuna da
  rigenerare.

## Diario: orari, giorni, icone, iniziali (committente, 14 ott 2026)

Il secondo passo della revisione del Diario: i punti A.3, A.4, A.5 e A.9, sulla stessa PR
dell'hero.

### Cosa è cambiato

- **A.3, gli orari in colonna**: l'ora di ogni voce in `titleMedium` tabellare, inchiostro
  pieno, sulla riga del titolo, come gli orari del Cielo. Era un'etichetta grigia piccola.
- **A.4, i giorni**: un titolo di sezione «Cosa è successo» sopra il registro, e i giorni nello
  stile dei giorni del Cielo (`labelLarge` in `primary`, la data intera accanto in inchiostro
  tenue), sempre fissi in alto mentre si scorre. Il conteggio «5 voci» non c'è più (e con lui il
  plurale `journal_day_count`).
- **A.5, le icone**: una revisione porta il disegno della grandezza che si è mossa (le gocce o
  il termometro, sul suo `primaryContainer`); un avviso scattato il disegno della sua card in
  Avvisi (`ruleIcon`, ora `internal`, cercato per nome: `JournalViewModel.rules`), con la
  campanella per un avviso che non c'è più; un aggiornamento o un bollettino mancato una nuvola
  barrata (`ic_not_reached`).
- **A.9, le iniziali**: sotto le barre di «Com'è andata la previsione» il giorno della settimana
  e il numero, su due righe.

### Decisioni e deviazioni

- **La grandezza e non il tempo del giorno, sulla revisione.** La revisione proponeva «il
  disegno del tempo del giorno di cui parla»; la voce però porta solo i numeri che si sono
  mossi, non lo stato del giorno, e dedurre pioggia o sole da una probabilità sarebbe la
  schermata che inventa quello che decide il motore degli stati. Il disegno della grandezza è
  quello dei chip della settimana, ed è vero per ogni voce.
- **La nuvola barrata disegnata qui, non `not-available` di Meteocons**: provato, quel disegno
  sono le lettere «N/A», una sigla inglese in un'app che non ha gergo. Un vettore di due tratti
  nella mano di `ic_warning`, colorato come le altre sagome tenui.
- **L'avviso per nome**: la storia registra il nome, ed è lo stesso legame che Avvisi usa per
  «scattato l'ultima volta». Un avviso rinominato o cancellato perde il disegno e torna alla
  campanella, che resta vera.
- **`ruleIcon` da `private` a `internal`**, una riga in `AlertsScreen.kt`, invece di copiarla:
  una sola funzione decide il disegno di un avviso. Il thread di Avvisi lavora sullo stesso file
  in questi giorni; la modifica è quella sola riga.
- **Giorno e numero su due righe, non «dom 11» su una**: a dieci colonne su 360 dp una colonna
  ha circa 25 dp, e «dom 11» ne vuole più di 30. Il giorno si rimpicciolisce (fino a 8 sp)
  invece di essere tagliato: provato, «Mon» e «Wed» venivano tagliati a «Mo» e «We» prima.
  Lo spazio fra le colonne passa da 6 a 4 dp.

### Come è stato verificato

- La pagina disegnata con Robolectric in chiaro (IT, 384 dp, tre giorni chiusi) e in scuro (EN,
  360 dp, dieci giorni chiusi), con ogni tipo di voce, un avviso che esiste e uno cancellato,
  guardata immagine per immagine.
- Suite completa e lint.

## Diario: le voci che aprono la loro pagina, TalkBack (14 ott 2026)

Punto 3 dell'ordine della review del Diario (`/mnt/project-files/review-diario/review-diario.md`):
A.6 e A.11, richiesti dal committente.

- [x] **A.6, le voci che aprono**: tutta la riga è il bersaglio, senza chevron, con
  `onClickLabel` come le righe del Cielo (`EntryOpens` in `JournalScreen.kt`).
  - un momento del cielo osservato apre la sua pagina (`SkyEventPage` in un foglio, i
    momenti collegati si aprono al suo posto);
  - un'allerta apre il foglio del bollettino (`WarningSheet`), se il bollettino in mano copre
    ancora il giorno della voce (`JournalViewModel.warnings`);
  - un avviso scattato apre il suo editor in Avvisi: `AlertsRoute(openRuleId)`, portato dalla
    shell (`ChiaroRoot`) con `openTabRoot(ShellTab.ALERTS)`;
  - una revisione apre «La storia di sabato 17» (`DayStorySheet`, `dayStory()`).
  - Aggiornamenti mancati, bollettini mancati e giorni chiusi restano fermi.
- [x] **A.11, TalkBack**: ogni voce è un nodo solo. Quelle che aprono lo sono perché cliccabili,
  le altre con `semantics(mergeDescendants = true)`; l'ora è letta con la frase.

### Decisioni e deviazioni

- **La storia del giorno in forma minima, non C.2 intero.** C.2 (due grafici a gradini) è nel
  punto 6 dell'ordine; una revisione però doveva aprire qualcosa ora. La forma minima è la riga
  della striscia letta in verticale: una riga per ogni colonna che ha mosso pioggia o massima,
  la più recente in alto, il numero mosso in inchiostro pieno. Gli stessi dati che la striscia
  ha già in memoria, niente di nuovo da salvare. Una colonna vuota non è un passo (una notte a
  telefono spento non è una previsione rimasta ferma), e la nota in fondo dice ogni quante ore
  è letta e da quando. I grafici di C.2 la sostituiranno o la affiancheranno.
- **Solo i giorni che la striscia tiene**: la striscia copre i giorni ancora davanti, su tre
  giorni e mezzo. Una revisione di un giorno già passato resta ferma: la sua storia non c'è più,
  e aprire un foglio vuoto sarebbe la schermata che promette qualcosa che non ha.
- **Il bollettino solo se copre ancora il giorno.** L'app tiene un bollettino solo, quello in
  mano: aprirlo da una voce di tre giorni fa mostrerebbe i gradi di un altro bollettino sotto la
  frase di quello vecchio. Se lo copre, il foglio dice da sé l'ora del suo bollettino.
- **La pagina del momento senza «Aggiungi/Togli».** È la pagina che apre il Cielo, senza il
  pulsante dell'agenda: il diario registra quello che è successo, l'agenda si tiene nel Cielo, e
  togliere un momento dal diario sarebbe un effetto lontano dalla pagina che lo mostra.
- **L'avviso in Avvisi, non un editor nel Diario.** «Apre quell'avviso in Avvisi», come diceva
  la review: l'editor vive con il view model di Avvisi, e copiarlo nel Diario vorrebbe dire due
  editor. La richiesta passa per la shell e Avvisi si apre alla sua radice (non sotto una
  pagina delle Impostazioni lasciata aperta lì); indietro chiude l'editor, poi torna a Oggi come
  da ogni scheda.
- **File condivisi toccati** (il thread di Avvisi lavora in parallelo): `AlertsScreen.kt` (due
  parametri e un `LaunchedEffect` in `AlertsRoute`), `ChiaroRoot.kt`, `ChiaroNavigation.kt`
  (`openTabRoot`). Nessun file del tema.

### Come è stato verificato

- `JournalOpensTest` (Robolectric): ogni tipo di voce apre quello che deve, con la sua etichetta
  per TalkBack; un avviso cancellato e un aggiornamento mancato non prendono il tocco; una voce
  ferma è un nodo solo con l'ora dentro. `DayStoryTest`: i passi, le colonne vuote, un giorno
  che la striscia non tiene.
- La storia del giorno e le righe disegnate con Robolectric (IT chiaro, EN scuro, 360 dp),
  guardate immagine per immagine.
- Suite completa e lint.

## Diario: la striscia della settimana col dito (14 ott 2026)

Punto 4 dell'ordine della review del Diario: C.1, richiesto dal committente. Porta con sé
A.8 (il suggerimento del gesto sopra la striscia).

- [x] Un dito sulla striscia sceglie una casella: trascinamento orizzontale subito, pressione
  lunga da fermo (`detectHorizontalDragGestures`, `detectDragGesturesAfterLongPress`, come
  `NightStrip` del Cielo). `driftCellAt()` trova la casella: le righe si dividono l'altezza, le
  colonne quello che lasciano i nomi dei giorni; oltre un bordo resta la casella del bordo.
- [x] La casella ha un anello, la sua riga e la sua colonna restano accese e il resto scende
  al 30%; il nome del giorno in grassetto. Un tocco leggero a ogni casella attraversata.
- [x] La riga sopra la striscia legge la casella (`driftReading()`): giorno, aggiornamento,
  pioggia e massima (la grandezza sullo schermo per prima); una fascia vuota e un
  aggiornamento che non arrivava a quel giorno lo dicono. Senza dito è il suggerimento.
- [x] TalkBack: ogni giorno è un controllo regolabile sugli aggiornamenti, lo stato è la
  lettura della casella (la più recente finché non lo si muove).

### Decisioni e deviazioni

- **La pressione lunga non apre più la tabella**: è il gesto che tiene una casella, come nel
  Cielo. Il tocco la apre ancora («la tabella resta per chi la vuole tutta»).
- **La riga sopra, la frase dell'hero ferma.** La review diceva «alzato il dito torna la frase
  della settimana»: la frase dell'hero non cambia mentre si esplora, cambia solo la riga sopra
  la striscia, che torna al suggerimento. Una parola grande che salta a ogni casella sarebbe
  rumore, e la riga è dove si guarda mentre il dito è sulla striscia.
- **Entrambi i numeri nella lettura**, non solo quello della grandezza scelta: costa mezza
  riga e risponde a «e la massima?» senza cambiare chip. La grandezza sullo schermo viene prima.
- **Due righe riservate** alla lettura: in italiano a 360 dp va a capo, e una riga che cresce
  sposterebbe la striscia sotto il dito.
- **Un controllo per giorno per TalkBack**, non uno per tutta la griglia: una griglia è due
  dimensioni, un controllo regolabile una sola; il giorno è la domanda («quando è cambiato
  sabato?») e gli aggiornamenti sono i passi.
- **A.8 fatto qui**: il suggerimento del gesto sta sopra la striscia; sotto resta «Una colonna
  ogni 6 ore · tocca per i numeri».

### Come è stato verificato

- `DriftScrubTest`: la casella sotto il dito, i bordi, la lettura di una casella, di una fascia
  vuota e di un aggiornamento che non arrivava al giorno. `JournalStripTest`: lo stato di ogni
  giorno per TalkBack e il suo spostamento casella per casella.
- La striscia disegnata a riposo, con una casella tenuta e con una fascia vuota, in chiaro (IT,
  384 dp) e in scuro (EN, 360 dp), guardata immagine per immagine.
- Suite completa e lint.

## Diario: il selettore segmentato, la porta della guida, e cosa non si fa (14 ott 2026)

Punto 6 dell'ordine della review del Diario, nella parte che il committente ha scelto.

- [x] **A.7**: Pioggia/Massime è un pulsante segmentato (`SingleChoiceSegmentedButtonRow`,
  `MetricChoice`), largo quanto la striscia, con il disegno della grandezza al posto della
  spunta. Erano due `FilterChip`, che in Material sono filtri attivabili insieme.
- [x] **A.8**: fatto con C.1 (il suggerimento del gesto sopra la striscia).
- [x] **A.10**: «Come si legge il diario» sotto l'hero, nella forma della riga «Che cosa sono
  questi eventi» del Cielo, apre in un foglio il capitolo del Diario della guida.

### Decisioni e deviazioni

- **Il capitolo in un foglio, non la guida intera.** La guida è una pagina sola che scorre: aprirla
  dal Diario voleva dire scorrere Oggi, Cielo e Avvisi per arrivare al capitolo, oppure dare alla
  sua chiave di navigazione un capitolo d'arrivo (un cambio in `ChiaroNavigation` e nei suoi test,
  in un file che il thread di Avvisi tocca). Il capitolo è estratto in `JournalChapter()`, disegnato
  dalla guida e dal foglio: un solo testo, due porte.
- **Non si fanno, per scelta del committente (14 ott 2026):**
  - **B** (una tabella dei giorni chiusi tenuta 90 giorni) e **C.3** (il mese della previsione):
    «Com'è andata la previsione» resta sui giorni che la storia tiene (circa quattro alla
    cadenza oraria). Statistiche in più che rischiano di non essere guardate.
  - **C.2** (la storia di un giorno con due grafici a gradini): la domanda «sabato si sta
    stabilizzando?» ha già la sua risposta nella forma minima che apre una revisione (A.6).
  Non riproporli senza una richiesta.

### Come è stato verificato

- `JournalChoiceTest`: le due grandezze sono una scelta sola; la riga apre il capitolo.
- La pagina disegnata (IT chiaro, EN scuro) con il foglio della guida aperto, guardata.
- Suite completa e lint.

## La guida e il README dopo le revisioni di Avvisi e Diario (committente, 14 ott 2026)

Il committente ha chiesto di verificare la guida nell'app e il `README.md` dopo le due revisioni
(PR #65, Diario, e #66, Avvisi). Le due PR avevano già aggiornato i paragrafi che toccavano; il
controllo, riga per riga contro le schermate, ha trovato quello che restava indietro.

- [x] **Avvisi, l'apertura del capitolo** diceva «tre gruppi»: ora la pagina apre con una card che
  risponde (le allerte, la fascia in cui il telefono può suonare per prima, la striscia del
  giorno), poi i gruppi e il pulsante «Nuovo avviso». Il paragrafo lo dice in quell'ordine.
- [x] **«È vero adesso?»** diceva «di quanto» una condizione non tiene: la riga sulla card dice il
  valore letto e quello che serve, e anche «fuori orario fino alle…», che la guida non nominava.
- [x] **Le allerte ufficiali**: l'interruttore «Avvisami delle allerte» con la sua soglia (giallo o
  arancione, una notifica per bollettino) non era nella guida, ed è ora nello stesso gruppo della
  card.
- [x] **Diario, l'apertura del capitolo** diceva che il Diario è «il registro letto come prosa»:
  dopo la revisione il registro è la terza parte della pagina. Il paragrafo dice le tre parti, e i
  tre paragrafi seguono l'ordine della pagina (`JournalChapter`): la settimana, com'è andata la
  previsione, cosa c'è scritto. «Come si muove la settimana» parte dalla risposta in cima e dice
  «Ancora presto».
- [x] **«Com'è andata la previsione»** non aveva un paragrafo (c'è dal 23 set): ora ce l'ha, con
  la ragione per cui non è un voto. La riga «Come si legge il diario» ne dice le tre parti.
- [x] Una riga di un avviso scattato apre il suo **editor** in Avvisi, non la sua card.
- [x] `README.md`: le righe delle funzioni di Avvisi e Diario dicono lo stato di ogni avviso e la
  risposta della settimana. Le didascalie e le immagini erano già giuste (#66 le aveva
  rigenerate); l'immagine della guida mostra l'inizio, che non cambia, quindi nessuna da
  rigenerare.

## Via il Diario, e la gestione dei dati (committente, 14 ott 2026)

Il committente ha chiesto una revisione della gestione dei dati (cosa si salva, per quanto, se le
tabelle si puliscono) e un parere sincero sul Diario. Il parere: come scheda non si guadagnava il
posto. Era il `git log` di tweather letto come prosa, una risposta a una domanda che il pubblico di
questa app non fa; la sua statistica («Com'è andata la previsione») poteva contare al massimo tre
giorni con la retention di allora e misurava il modello contro la sua stessa analisi; le altre voci
ripetevano cose già dette altrove (la notifica, l'età dei dati). L'unico pezzo che risponde a una
domanda vera, «Sabato è migliorato: pioggia 70% → 30%», viveva già in Oggi. Decisione del
committente: **togliere il Diario del tutto, con le sue implicazioni nel data layer, e correggere
tutti i problemi trovati nella revisione, anche i minori**.

### Cosa è cambiato

- [x] **Il Diario esce**: la scheda (tre destinazioni: Oggi, Cielo, Avvisi), `ui/journal/` con i
  suoi test, il capitolo della guida e la sua riga nella mappa delle schede, la miniatura della
  striscia, 73 stringhe per lingua, l'icona `ic_not_reached`, la porta da Avvisi
  (`openRuleId`, `openTabRoot`).
- [x] **«Cosa è cambiato» resta in Oggi**, e diventa di Oggi: `ui/today/WhatChanged.kt`
  (`WhatChangedBuilder.latestShifts`, `WhatChangedText`), stesse regole e stesse parole, stringhe
  rinominate `changed_*`. Le frasi non si toccano più: dietro non c'è niente da aprire.
- [x] **Il data layer perde quello che leggeva solo il Diario**: la tabella `warning_records`, le
  colonne `snapshot_json`, `sky_runs`, `hash`, `author`, `city_label` di `weather_history`,
  `FetchLogStore` (e il suo file, cancellato all'avvio), `ForecastOutcome`, `SkyRunRecorder`,
  `WarningDiff`, la riga «bollettino non raggiunto» e la sua chiave in `OfficialWarningStore`,
  lo snapshot delle condizioni attuali (`WeatherSnapshots.flatten`). Room passa a v6
  (`MIGRATION_5_6`): ricostruzione della tabella con `forecast_json` e `fired_rules`, l'indice
  `(city_key, timestamp_epoch_s)`, `DROP TABLE warning_records`.
- [x] **Retention per tempo, non per numero**: tre giorni per luogo (`HISTORY_RETENTION`), mai
  meno di due righe (`HISTORY_MIN_ROWS`), un tetto globale di 8 × 288 righe come sola rete di
  sicurezza. Il cento per luogo di prima faceva dipendere la profondità da quanto spesso l'app
  scaricava.
- [x] **Un commit per finestra di 15 minuti** (`COMMIT_WINDOW`, la risoluzione del provider): un
  aggiornamento dentro la finestra riscrive l'ultimo commit invece di affiancarlo. Prima ogni
  pull-to-refresh scriveva una copia identica, e la copia, senza revisioni proprie, faceva
  sparire «Cosa è cambiato» proprio al pull.
- [x] **Le regole scattate si sommano** (`recordFiredRules` legge e unisce): con il commit che
  può valere due aggiornamenti, sovrascrivere avrebbe perso la prima.
- [x] **`RuleStateStore.clearRule` finalmente chiamato**: Avvisi lo chiama quando cambiano le
  condizioni o le ore di una regola. Era scritto, testato, documentato come chiamato dalla UI, e
  nessuno lo chiamava: una regola già scattata con una soglia nuova restava zitta.
- [x] **I latch orfani se ne vanno** (`RuleStateStore.retain`, a ogni commit): quelli di un luogo
  rimosso o di una regola cancellata non venivano mai valutati, quindi mai tolti. La chiave del
  luogo per le notifiche è ora una sola definizione (`City.notificationKey`), usata dal job, dal
  refresh dei bollettini e dalla pulizia.
- [x] **La cache su disco non butta più un luogo seguito**: il tetto di 16 file era applicato
  alla scrittura, che non sa distinguere un luogo seguito da una cella GPS, e con più di 16 luoghi
  toglieva la previsione offline di qualcuno. Ora alla scrittura c'è solo l'età (l'orizzonte), e il
  tetto (8) vale solo per i file dei luoghi non più seguiti, in `forgetForeign`.
- [x] **La cache in memoria tiene solo i luoghi seguiti** (`cache.keys.retainAll` a ogni commit):
  prima cresceva con ogni cella GPS per tutta la vita del processo.
- [x] Commenti allineati: quello di `ReportDiskCache` sul backup (non più vero dal 21 set), e
  ogni riga di codice che nominava il Diario come lettore di qualcosa.
- [x] VISION (§2.2, §5.1, §5.2, §5.5, §5.7, §6, §9), DESIGN (§8.10, §8.12, tipi, accessibilità),
  README, CLAUDE.md (la regola dello storage), CHANGELOG (Removed, Fixed).

### Decisioni e deviazioni

- **Tre giorni.** Il lettore più profondo è la massima di ieri, che nel fuso del luogo può stare
  fino a 48 ore indietro; il terzo giorno è il margine per un telefono che ha dormito una mattina.
  «Ultima volta» in Avvisi legge ora tutta la retention (prima 50 righe, circa due giorni).
- **Due righe minime**: un telefono rimasto spento una settimana torna con una previsione con cui
  confrontare la prima nuova.
- **La migrazione ricostruisce la tabella** invece di `DROP COLUMN`: SQLite lo ha dalla 3.35 e
  minSdk 33 porta la 3.32. Le righe senza previsione (scritte prima della v2) non vengono copiate:
  nessun lettore le userebbe. Il file non si restringe senza un `VACUUM`, che non può girare dentro
  la transazione della migrazione: le pagine liberate vengono riusate, il file non cresce più.
- **La cancellazione di una regola non pulisce subito**: «Annulla» la riporta com'era. I latch di
  una regola che resta cancellata vanno via al commit successivo, con quelli dei luoghi rimossi.
- **Nessun «Cosa è cambiato» cliccabile**: la riga «La storia intera è nel Diario» non ha più una
  pagina dietro, e una frase che si tocca senza effetto sarebbe un controllo finto.
- **La cronologia delle allerte rientrate non si sposta altrove**: il banner e la card dicono lo
  stato del bollettino in mano, con la sua data; la storia dei cambi era una cosa del Diario.

### Come è stato verificato

- `HistoryRetentionTest` (nuovo, Robolectric con Room in memoria): tre pull in nove minuti sono
  un commit solo, con l'ora dell'ultimo, e il quarto dopo 25 minuti ne apre un secondo; le regole
  scattate su due aggiornamenti dello stesso commit restano entrambe; un luogo tiene tre giorni;
  un telefono tornato dopo una settimana tiene il nuovo commit e il più recente dei vecchi; la
  cache in memoria dimentica un luogo non seguito al commit successivo e ne rifà il fetch.
- `ChiaroDatabaseMigrationTest` riscritto: una v5 vera (con `warning_records` e le colonne del
  Diario) passa a v6 tenendo id, previsione e regole, perde la riga senza previsione e la tabella,
  e trova l'indice; una v1 attraversa tutta la catena e accetta commit nuovi.
- `RuleStateStoreTest` (+1): `retain` tiene i latch dei luoghi e delle regole vive, `gps`
  compreso, e lascia stare le impronte.
- `StoredDataSweepTest` (+1): con venti luoghi seguiti nessuno perde la sua risposta, e dei file
  stranieri ne restano otto, i più recenti.
- `WhatChangedBuilderTest` (nuovo, al posto dei casi di «Cosa è cambiato» che stavano nel test del
  Diario): pioggia che scende è migliorato, cielo che peggiora a pioggia ferma è peggiorato, la
  pioggia decide sul cielo, una massima da sola resta neutra, le etichette inglesi su disco sono la
  stessa previsione, parla solo l'ultimo aggiornamento, al massimo tre righe con la pioggia più
  mossa in testa.
- `OfficialWarningsStepTest` e `WeatherSyncWorkerTest` senza i casi del Diario; gli altri invariati.
- `./gradlew test :app:testDebugUnitTest :app:lintDebug`: 1842 test, 0 falliti (i 12 saltati sono
  le schermate del README senza `-PupdateScreenshots`), lint pulito.
- Schermate del README rigenerate e guardate una per una: la barra ha tre schede (Oggi, Cielo,
  Avvisi), la guida e Impostazioni dicono «tre schermate». Widget e notifica non cambiano.

## Avvisi: «Nuovo avviso» è l'unico ingresso (committente, 14 ott 2026)

Provando l'app il committente ha visto due ingressi alla stessa cosa: con «I tuoi» vuoto la fila
delle idee sulla pagina e il FAB «Nuovo avviso», che apre le stesse idee in un foglio; al primo
avviso creato la fila spariva e restava il FAB. Era voluto (punto 9 della revisione di Avvisi: la
fila come stato vuoto della sezione), ma letto sul telefono era un salto della pagina che
sembrava un errore. Decisione: **solo il FAB, con una riga di stato vuoto**.

- [x] La fila delle idee esce dalla pagina (`IdeaCard`, le sue misure, `tpl_add`, `tpl_create`).
  Le idee vivono solo nel foglio del FAB, con «Da zero» in testa, come già con un avviso.
- [x] «I tuoi» vuoto è una riga, `alerts_yours_empty` (IT/EN): «Gli avvisi che scrivi tu, per
  quello che fai: «Nuovo avviso» parte da un'idea o da zero.» Il titolo della sezione non sta
  sopra il nulla (DESIGN §1.1), che era la ragione della fila, e al primo avviso cambia una riga,
  non un blocco di card.
- [x] VISION §5.4, DESIGN (Avvisi), CHANGELOG. La guida diceva già che le idee si aprono da
  «Nuovo avviso»: nessuna parola da cambiare.

Scartato: tenere le card e nascondere il FAB finché la sezione è vuota. Il pulsante sarebbe
comparso dal nulla dopo il primo avviso: lo stesso salto, al contrario.

Verificato con `AlertsYoursEmptyTest` (nuovo, Robolectric): con «I tuoi» vuoto la pagina ha la
riga e nessuna idea, e «Nuovo avviso» apre il foglio con «Da zero» e «Parti da un'idea». Suite
completa e lint verdi. Le schermate del README rigenerate non cambiano: nessuna mostra «I tuoi»
vuoto.

## Oggi: la revisione della schermata (committente, 15 ott 2026)

Il committente ha chiesto una revisione della schermata Oggi (stile grafico, tipografia, UI/UX) e
della sua coerenza con Cielo e Avvisi, appena rivisti, e ne ha fatto attuare i primi cinque punti;
il resto è pianificato qui sotto, da fare tutto, con il DESIGN aggiornato dove serve. La diagnosi
in una riga: Oggi era rimasta alla generazione di prima delle revisioni del 14 ott (angoli da 12
dp, il banner piccolo, le righe in `bodyMedium`, un'intestazione sotto i 48 dp), e accanto alle
altre due tab sembrava un'altra app.

- [x] 1. L'intestazione del luogo a 48 dp, con ruolo, azione e ripple ritagliato, come `TabHeader`.
- [x] 2. Il testo secondario sul cielo in bianco pieno, distinto dalla misura; il contratto dello
  scrim esteso al testo secondario.
- [x] 3. Le righe della settimana: la freccia, lo stato per TalkBack, il giorno aperto su una card.
- [x] 4. La gerarchia del blocco principale: tre gruppi, la frase come prosa.
- [x] 5. Gli angoli 28/20 dp nominati nel tema; riquadri, banner e avvisi a quegli angoli; le righe
  della giornata in `bodyLarge`.

### Cosa è cambiato

- **Punto 1.** `PlaceHeader` (`TodayScreen.kt`): il bersaglio è il blocco nome + ora del luogo,
  28 + 20 = 48 dp (minimo 48 anche senza ora, la pagina della posizione), ritagliato a 12 dp,
  rientrato di 8 dp dai 16 della riga perché il ripple abbia aria e il testo resti sul margine; il
  ripple prende l'inchiostro della riga (bianco sul cielo). Un pulsante solo per TalkBack, «Milano,
  venerdì 25 settembre · 12:15», azione «Cambia luogo», la freccia muta. Prima il bersaglio era la
  sola riga del nome, circa 28 dp, con un ripple rettangolare sul cielo. I pallini delle pagine
  restano fuori: sono un fatto, non un ingresso. `HeaderPadding` sostituisce le tre copie del
  padding della riga.
- **Punto 2.** `SkyPalette.Ink` (bianco pieno) è l'unico inchiostro del testo sulla tela;
  `SkyPalette.InkQuiet` (bianco al 40%) solo per i pallini delle altre pagine, un segno e non un
  testo. La percepita e la nota della stima erano al 70%, gli orari del nastro all'85%: sul cielo
  più chiaro con lo scrim pieno fanno circa 3,5:1 e 4,3:1, sotto il 4,5 (DESIGN §3.6 misurava solo
  il bianco pieno). `ScrimContractTest`: due prove nuove, i bianchi più sottili cadono sotto la
  soglia, e nessun file sotto `ui/today/` nomina un bianco suo.
- **Punto 3.** `DayRow`: una freccia da 18 dp dopo la massima, che si gira (molla spaziale) quando
  il giorno è aperto; `stateDescription` «Aperto»/«Chiuso» e l'azione «Mostra le ore»/«Nascondi le
  ore» (IT/EN), solo sulle righe che si aprono. La colonna della freccia c'è su ogni riga, perché le
  sette barre finiscano allo stesso punto; la guida la disegna chiusa e dice che la freccia apre il
  giorno. Il punto in cui la riga si spezza in due è **misurato sulla larghezza** (`splitsRow`): la
  barra deve avere almeno 48 dp dopo colonne, icona, spazi e freccia. `WeekRow`: il giorno aperto è
  una card `surfaceContainer` con angolo 20 dp, 8 dp dentro il margine della pagina e 8 dp d'aria
  dentro, così le colonne non si spostano e la striscia scorre sotto il bordo della card; il fondo
  appare con la molla degli effetti, l'aria in alto cresce con quella spaziale.
- **Punto 4.** La tela in tre gruppi: l'adesso (cifra, parola del cielo, percepita, nota della
  stima) stretto sotto la cifra; la frase del giorno 12 dp sotto, in **`bodyLarge`** (era
  `titleLarge`, la stessa misura e lo stesso peso della parola sopra); il nastro 16 dp sotto, con
  gli orari a 4 dp. Prima un passo uniforme di 6 dp.
- **Punto 5.** `ui/theme/Shape.kt`: `CardCorner` (28), `GroupCorner` (20), `GroupInnerCorner` (4),
  le costanti che il Cielo e i segmenti tenevano private (`SegmentedGroup`, `SkyScreen`,
  `TonightSky` ora le leggono da lì). `MetricTile`, il banner dell'allerta e il banner d'errore a
  20 dp; la card della guida su Oggi e `NotificationsOffCard` (Cielo, Avvisi) a 28 dp con 20 dp
  d'aria. Il banner dell'allerta prende il clic da `Surface(onClick)`, così la pressione è
  ritagliata all'angolo (prima un `clickable` nel modifier disegnava un rettangolo). `TimelineRow`:
  la prosa in `bodyLarge`.

### Decisioni e deviazioni

- **La frase in `bodyLarge`, non in un 18 nuovo.** Fra la parola a 22 Medium e una prosa a 16
  Regular la differenza è di misura e di peso insieme, e la scala resta quella che è (il widget
  dice la frase a 16 anch'esso). Guardata sul Milano del README: si legge come una frase sotto il
  titolo, non come un secondo titolo.
- **Sul cielo niente opacità per gerarchia**, nemmeno a 85%: la misura basta, ed è l'unica cosa che
  il contratto dello scrim garantisce. La card del Cielo di notte (`TonightSky`) tiene i suoi bianchi
  al 70%: è su un fondo blu notte fisso, non sul cielo dipinto, e lì il contrasto regge.
- **La freccia della settimana costa alla barra 20 dp** (da 102 a 82 dp a 360): la barra resta una
  barra. Per questo la soglia di spezzatura non è più il 150% fisso di `TextScale.kt`, che resta solo
  della griglia dei dettagli: a 360 dp la riga si spezza dal 125%, a 411 dp dal 180% circa.
- **Le righe chiuse restano sulla carta**, non segmenti: sette card farebbero una settimana più
  pesante da leggere, e la freccia dice già quali si aprono. La card è del giorno aperto soltanto.
- **Il banner dell'allerta a 20 e non a 28**: è una dichiarazione di una o due righe e 28 lo farebbe
  pillola. Resta banner (DESIGN §8.13): la card grande è di Avvisi, dove c'è anche lo stato «nessuna
  allerta» contro cui misurarla; il resto dell'allineamento è nel piano qui sotto.
- **La card della guida e quella delle notifiche insieme**: sono lo stesso tipo di avviso, e
  cambiarne una sola avrebbe aperto la stessa incoerenza fra le tab.

### Come è stato verificato

- `DayRowSplitTest` (nuovo): a 360 dp la riga resta su una riga al 100% e al 120% e si spezza al
  125%; a 411 dp regge il 150% e si spezza al 200%; il limite è esattamente la barra minima.
- `ScrimContractTest`: bianco al 70% e all'85% sotto 4,5:1 su ogni tavola del cielo; nessun
  `Color.White` sotto `ui/today/`. `TextScaleTest` aggiornato (la soglia del 150% non è più della
  settimana).
- Suite completa dell'app e lint. Le immagini del README rigenerate e guardate: `today-now`
  (i tre gruppi, la timeline in `bodyLarge`), `today-week` (le frecce, sabato aperto sulla sua card),
  `today-details` (i riquadri a 20 dp); quelle di Cielo e Avvisi rigenerate escono identiche.

### Il piano per il resto (da fare tutto, committente 15 ott 2026)

Tre passi, ciascuno con il suo DESIGN, le sue prove e le immagini del README rigenerate. Le
domande segnate **(decide il committente)** vanno chiuse prima di quel passo.

**Passo A — Coerenza con Cielo e Avvisi**

- [x] A1. **Il resto della giornata e i momenti del Cielo dicono le stesse cose con le stesse
  parole.** Oggi «Ora d'oro fino alle 19:15» alle 18:36, il Cielo «Ora d'oro, sera» 18:36 – 19:15.
  Proposta: i nomi del Cielo (`sky_name_*`) anche su Oggi, l'intervallo scritto come intervallo
  («18:36 – 19:15» nella colonna dell'ora, a due righe se serve), e le righe dei momenti del cielo
  **toccabili**: aprono la pagina del momento del Cielo, con «Dove guardare» e la bussola.
  **Deciso** (committente, 15 ott 2026): solo l'apertura della pagina, niente campanella.
  DESIGN §8.4.
- [x] A2. **Una grammatica sola per «il prossimo».** Oggi il conto alla rovescia in `labelMedium`
  primary sotto la prima riga; Cielo e Avvisi un'etichetta «Prossimo, oggi» in `labelLarge` primary
  sopra il nome. Proposta: su Oggi la stessa etichetta sopra la prima riga, e «tra 6 h 20 min» nella
  riga sotto, `bodyMedium` `onSurfaceVariant`, come nella card del prossimo momento. DESIGN §8.4,
  §8.8.
- [x] A3. **L'allerta su Oggi parla come quella di Avvisi**: la freccia che dice «si apre», il
  titolo in `titleMedium` anziché `titleSmall`, la fonte in `bodyMedium`; resta banner da 20 dp.
  DESIGN §8.13.
- [x] A4. **Le card d'avviso nella scala di Avvisi**: card della guida e card delle notifiche con
  titolo in `bodyLarge`/`titleMedium` e testo in `bodyMedium` (oggi `titleSmall`/`bodySmall`); il
  banner d'errore in `bodyMedium`. DESIGN §8.11, §8.14.
- [x] A5. **La nota in fondo** («Aggiornato alle 12:10 · dati Open-Meteo») allineata a sinistra
  come le note del Cielo. DESIGN §8.1 (la nota a piè di pagina).

**Passo B — Modernizzare**

- [x] B1. **I nastri della settimana più quieti.** Sette nastri saturi pesano quanto le barre delle
  temperature, che sono il dato. Proposta: nella settimana il cielo previsto del nastro a saturazione
  e contrasto ridotti (il colore resta, il peso scende), pieno solo sul giorno aperto; misurare
  sul Milano e su una settimana di pioggia. DESIGN §4, §8.5.
- [x] B2. **La barra compatta.** Scorrendo, nel tema chiaro diventa una fascia blu-ardesia scura
  (il cielo che resta in alto, §8.1b). **Deciso** (committente, 15 ott 2026): uniformata a Cielo e
  Avvisi, `surface` con l'inchiostro del tema; si torna alla scura se sul telefono non regge.
- [x] B3. **I dettagli come gruppo.** Oggi riquadri separati da 12 dp. **Deciso** (committente, 15
  ott 2026): i segmenti del Cielo in griglia (2 dp, 20 fuori e 4 dentro). DESIGN §8.6.
- [x] B4. **«Cosa è cambiato»** come righe con un segno (la freccia su o giù della grandezza che è
  cambiata) e il giorno in testa, in un gruppo di segmenti come le righe del Cielo, invece di frasi
  `bodyMedium` sciolte. DESIGN §8 (VISION §5.2.6).
- [x] B5. **Il movimento di §7 che manca ancora**: l'elemento condiviso fra la riga della settimana e
  il suo giorno aperto (oggi l'espansione è a molle ma senza continuità della riga), e la verifica
  del ritorno predittivo sulla scheda dei luoghi e sui fogli. DESIGN §7.

**Passo C — Pulizia**

- [x] C1. `WindDirection` ha due KDoc in fila (uno inglese che parla ancora della freccia, uno in
  italiano); il KDoc di `Details` cita anch'esso la freccia del vento. Un KDoc solo, in inglese.
- [x] C2. La freccia del luogo su Oggi a 24 dp accanto a un `titleLarge`, su Cielo e Avvisi a 18
  accanto a un `titleSmall`: proporzionate ciascuna alla sua riga, da lasciare così, scritto nel
  DESIGN (§8.1b, §8.12b) perché nessuno le «allinei».

### Il resto della revisione (committente, 15 ott 2026)

Il committente ha chiuso le tre domande (solo l'apertura della pagina, la barra come Cielo e
Avvisi, i segmenti nei dettagli) e ha chiesto di fare tutto il piano.

**Cosa è cambiato**

- **A1, A2.** `RestOfDay` e `TimelineRow`: i momenti del cielo coi nomi del Cielo
  (`SkyText.nameRes`, da `timelineJob`), «fino alle 19:15» e il conto alla rovescia sulla riga
  sotto il nome in `bodyMedium` `onSurfaceVariant`, la prima riga con «Il prossimo, oggi» in
  `labelLarge` `primary` (`sky_next_label`, la stessa stringa del Cielo). Una riga del cielo apre
  `AgendaPageSheet`, la pagina del momento del Cielo, con «Dove guardare» per i momenti che hanno
  una direzione (`SkySights.bearing` all'istante della riga) e senza il pulsante per seguirlo:
  `AgendaPageSheet` prende ora un `AgendaSubscription?` al posto di `subscribed`/`onAdd`/`onRemove`,
  che il Cielo passa e Oggi no. Pioggia e arcobaleno non aprono niente. Tolte le due stringhe
  `tl_*_until` (le altre `tl_*` restano: le usa il widget dell'arco).
- **A3.** Il banner dell'allerta in `titleMedium` su `bodyMedium`, con la freccia di Avvisi.
- **A4.** La card della guida e `NotificationsOffCard` in `titleMedium`/`bodyMedium`; il banner
  d'errore in `bodyMedium`.
- **A5.** «Aggiornato alle…» sul margine di 16 dp, con 16 dp sotto.
- **B1.** `DaylightRibbon(quiet)`: il nastro di una riga chiusa piega del 40% verso la sua traccia,
  quello del giorno aperto è pieno, il passaggio sulla molla degli effetti; i segni della pioggia
  restano a inchiostro pieno.
- **B2.** La barra, appena la pagina scorre, è `surface` con l'inchiostro del tema; fondo,
  inchiostro, pallini e icone della barra di stato cambiano insieme sulla molla degli effetti.
- **B3.** `gridSegmentShape` (in `SegmentedGroup.kt`) e il parametro `shape` di `MetricTile`: la
  griglia dei dettagli è un gruppo di segmenti, 2 dp, 20 fuori e 4 dentro. La guida disegna i due
  riquadri d'esempio allo stesso modo.
- **B4.** «Cosa è cambiato» come `SegmentedGroup`: l'icona del giorno com'è adesso nella settimana,
  il giorno e il giudizio in `bodyLarge`, i numeri sotto in `bodyMedium`.
- **B5.** Il ritorno predittivo c'era già (`enableOnBackInvokedCallback`, il pop predittivo della
  navigazione, i fogli di Material). DESIGN §7 parlava ancora di un elemento condiviso verso un
  «foglio del giorno» che non esiste più: ora dice l'apertura sul posto.
- **C1.** `WindDirection` con un KDoc solo, in inglese; tolta la freccia del vento dai KDoc di
  `Details`, `Tile` e `MetricTile`.
- **C2.** Scritto in DESIGN §8.1b che le due frecce del luogo (24 e 18 dp) sono misurate ciascuna
  sulla sua riga.
- La guida dice che un momento del cielo apre la sua pagina e che «Cosa è cambiato» sono righe;
  VISION §5.2 aggiornata (agenda, settimana, cosa è cambiato, dettagli).

**Decisioni e deviazioni**

- **«Cosa è cambiato» porta l'icona del giorno, non una freccia** come diceva il piano (B4): una
  revisione sposta spesso due grandezze in versi opposti (meno pioggia, massima più bassa) e una
  freccia sola ne avrebbe detta metà. Il giudizio resta a parole.
- **«Il prossimo, oggi» anche su Oggi**, dove ogni riga è di oggi: è la stessa etichetta delle card
  di Cielo e Avvisi, e una variante solo per questa schermata sarebbe stata la terza grammatica che
  A2 voleva togliere.
- **L'ora d'oro del mattino tiene le sue parole** («Finisce l'ora d'oro»): su Oggi la riga è la
  sua fine, non il suo inizio, e il nome del Cielo avrebbe letto un orario d'inizio sbagliato.
  Apre comunque la pagina del mattino.
- **Il buio pieno apre «Buio pieno»** (`darkness.window`), non il crepuscolo astronomico: la riga
  è l'inizio del buio, il crepuscolo astronomico ne è la causa, e il nome del Cielo per la finestra
  è già quello della riga in italiano.
- **La barra chiara, a mio parere onesto, è la scelta giusta** per il tema chiaro, dove la fascia
  ardesia era l'elemento più pesante dell'app; in scuro le due sono quasi uguali. Il rischio è il
  passaggio da bianco sul cielo a scuro sulla carta, che ora è una dissolvenza di circa un decimo di
  secondo. Si torna alla scura con un colore e un inchiostro, se sul telefono non regge.
- **La riga del giorno aperto non è un elemento condiviso**: si apre sul posto, e la continuità la
  danno la card che cresce e la freccia che gira insieme.

**Come è stato verificato**

- `TodayAgendaTest` (nuovo, Robolectric, sul Milano del README): la prima riga ha «Il prossimo,
  oggi» e il nome del Cielo; toccare l'ora d'oro apre la pagina con «Dove guardare» e senza
  «Aggiungi» né «Togli»; una riga della settimana dice «Chiuso» e, aperta, «Aperto».
  `AgendaPageSheetTest` aggiornato, con la pagina senza pulsante.
- Oggi disegnata in chiaro e in scuro, a riposo, scorsa di poco (la barra sopra il cielo) e sulla
  settimana con un giorno aperto: guardate immagine per immagine. Le immagini del README rigenerate
  e guardate (`today-now`, `today-week`, `today-details`); `widgets.png` usciva diverso di qualche
  pixel su due icone animate, senza nessun cambio che lo tocchi, ed è rimasto com'era.
- Suite completa e lint.

## Oggi: il giorno che si chiude, la sua barra, la barra in alto (committente, 15 ott 2026)

Provando la revisione sul telefono il committente ha segnalato tre cose.

- [x] **Chiudere un giorno della settimana faceva crollare l'app.** L'aria in alto della scheda del
  giorno aperto seguiva la molla spaziale (smorzamento 0,8), che rimbalza: chiudendo, sulla via
  verso 0 dp scendeva sotto zero, e un `padding` negativo in Compose è un'eccezione
  (`IllegalArgumentException: Padding must be non-negative`). Riprodotto con un test prima di
  correggerlo; ora il valore è tenuto a zero o più, e il tono del nastro fra 0 e 1.
- [x] **Sulla scheda aperta la barra delle temperature era poco contrastata.** La sua traccia
  vuota è `surfaceContainerHighest`, che sulla pagina sta a 1,23:1 dal fondo e sulla scheda
  (`surfaceContainer`) a 1,11:1, misurato su entrambe le palette: la barra restava senza traccia.
  Sulla scheda la traccia diventa `outlineVariant` (1,46:1 in chiaro, 1,74 in scuro), lo stesso
  colore verso cui piega la notte del nastro, e il cerchio del «adesso» si ritaglia dal fondo della
  scheda. Il colore segue l'apertura sulla molla degli effetti.
- [x] **La barra in alto sfuma con lo scorrimento** invece di diventare chiara di colpo:
  `placeBarColors` (nuovo, `ui/today/PlaceBarColors.kt`) dà fondo, inchiostro, pallini e icone
  della barra di stato per un punto dello scorrimento. Il fondo va dal cielo in cima alla tela,
  col velo di §3.6, alla carta della pagina, opaco, e arriva alla carta quando ci arriva il bordo
  della pagina; l'inchiostro sfuma finché la sfumatura si legge a 4,5:1, altrimenti è il migliore
  fra il bianco del cielo e quello del tema, e cambia una volta sola.

**Decisioni e deviazioni**

- **Non una semplice dissolvenza da trasparente a carta**, che era la cosa ovvia: a metà corsa il
  nome sarebbe stato grigio su grigio, e col cielo che si vede attraverso un fondo semitrasparente
  (in chiaro, di notte) testo scuro su blu notte. Il fondo sfuma invece fra due colori pieni, e il
  testo sta sempre su un colore solo.
- **Il punto peggiore è 4,14:1**, nel tema chiaro col sole a −10° a metà corsa: sopra il 3:1 del
  nome a 22sp, poco sotto il 4,5 della riga dell'ora, per l'istante in cui lo scorrimento ci
  passa. Fissato da `PlaceBarColorsTest`, che percorre ogni cielo, palette, tema e punto dello
  scorrimento.
- **La traccia più forte solo sulla scheda aperta**: sulla carta la traccia di prima regge ed è
  quella delle altre righe; cambiarla ovunque avrebbe appesantito la settimana chiusa, che la
  revisione aveva appena alleggerito.

**Come è stato verificato**

- `TodayAgendaTest`: aprire e chiudere due volte un giorno, con tutte le animazioni fino in fondo.
  Prima della correzione falliva con l'eccezione del padding negativo; ora passa.
- `PlaceBarColorsTest` (nuovo): il nome si legge in ogni punto della sfumatura (4,14:1 il
  peggiore); le due estremità sono il bianco del cielo e la barra delle altre tab; in chiaro
  l'inchiostro cambia una volta sola, mai avanti e indietro.
- La pagina disegnata in chiaro a 100, 300, 450 e 600 px di scorrimento e in scuro a 300 e 600, e
  la settimana col giorno aperto in chiaro e in scuro: guardate. `today-week.png` del README
  rigenerata (la traccia della barra sulla scheda); le altre immagini non cambiano.
- Suite completa e lint.

## Oggi: la barra torna a cambiare d'un colpo, la pagina si ferma tutta (committente, 15 ott 2026)

- [x] **La barra che sfumava con lo scorrimento non va.** Sul telefono, trascinando, sembrava
  scurirsi di scatto e poi schiarirsi: per sfumare dal cielo alla carta il fondo doveva prima
  comparire come cielo velato, più scuro della tela sotto di lui. Tolti `placeBarColors` e il suo
  test. Il committente proponeva di tornare a com'era prima o di provare una barra sempre chiara
  o scura secondo il tema, come su Cielo e Avvisi, chiedendo un parere: **sconsigliata per
  estetica**, perché il cielo che arriva in cima allo schermo, sotto l'orologio, è ciò per cui
  Oggi si riconosce, e una barra sopra lo farebbe diventare un riquadro dentro una pagina (la
  tela arrotondata come card fu già scartata il 4 set per la stessa ragione). Quindi com'era
  prima — trasparente a riposo, la carta del tema dal primo pixel — con una dissolvenza di 300 ms
  al posto della molla degli effetti, che sul telefono si leggeva come uno scatto.
- [x] **Scorrendo le ore di un giorno aperto le altre icone continuavano a muoversi.**
  `LocalMotionPaused` scende soltanto: una striscia poteva fermare le proprie icone e non quelle
  della pagina. `PageScrolls` e `ReportScrolling` (accanto a `LocalMotionPaused`): ogni striscia
  dice alla pagina che si sta muovendo, e Oggi ferma tutte le sue icone finché qualcosa scorre — la
  lista, il pager o una striscia. Vale anche per le prossime ore, che avevano lo stesso difetto.

Verificato con `PageScrollsTest` (nuovo, Robolectric): la pagina è ferma finché uno qualsiasi
dei suoi scorrimenti si muove, e viene liberata anche quando una striscia esce dalla
composizione a metà corsa (un giorno chiuso mentre la sua striscia scorre ancora). Suite
completa e lint; le immagini del README rigenerate non cambiano.

## Oggi: la data del luogo, la più lunga che ci sta (committente, 15 ott 2026)

Scorrendo, quando nella barra compariva la temperatura, la data sotto il nome passava sempre da
«Venerdì 25 settembre · 12:15» a «Ven · 12:15», su ogni telefono. Il committente ha chiesto se
si potesse tenere estesa: misurata in Google Sans a `labelLarge`, accanto alla temperatura su un
360 dp restano circa 190 dp, e «Mercoledì 30 settembre · 12:15» ne vuole 197, «Wednesday 30
September · 12:15» 213, con «PM» 237; su un 411 dp ci stanno tutte. Fra il formato medio sempre e
il più lungo che ci sta il committente ha scelto il secondo.

- [x] `FittingText` (`ui/components/`): fra più forme, la prima che sta su una riga alla larghezza
  vera della riga, misurata con `TextMeasurer`; se non ne sta nessuna, l'ultima, che va a capo e
  tiene le parole (§10). La riga del luogo gli passa la forma estesa, la media («Ven 25 set ·
  12:15», `Formats.dayMedium`) e la breve. Non è più lo scorrimento a decidere: sul telefono di
  riferimento del README la forma estesa resta anche con la temperatura accanto.
- [x] TalkBack legge sempre la forma estesa, qualunque sia quella disegnata.

Verificato con `FittingTextTest` (nuovo, Robolectric con la grafica nativa, perché quella di
default misura ogni riga in pochi pixel): a 272 dp la forma estesa, a 190 la media, a 120 la
breve, a 40 la breve che va a capo; `FormatsTest` per la forma media in IT/EN. Le immagini
`today-week` e `today-details` del README rigenerate: la data estesa resta accanto alla
temperatura. Suite completa e lint.


## Oggi: la seconda revisione, da app premium (committente, 15 ott 2026)

Il committente ha chiesto se, rifacendo oggi la schermata Oggi per un'app premium da Play Store,
si terrebbe la soluzione attuale. La risposta: lo scheletro sì (l'ordine dei contenuti, il cielo
calcolato, la frase prima del numero, il rigore sui dati), la resa no. Il committente ha chiesto
di fare tutto, a partire dai punti 1 e 2, di spezzare `TodayScreen.kt`, e ha aperto due porte:
**minSdk 34 se serve alla grafica**, e **il budget dei 2 ms del canvas da rivedere** se va
rivisto; il DESIGN si riscrive per ciò che serve davvero, senza farsi limitare da ciò che dice ora.

Il piano, in ordine di effetto sulla percezione e di costo:

- [x] 0. **`TodayScreen.kt` in un file per sezione** (2 563 righe): `TodayScreen` (il pager e la
  lista), `PlaceHeader`, `TodayStates`, `TodayHero`, `TodayHours`, `TodayAgenda`, `TodayWeek`,
  `TodayChanged`, `TodayDetails`. Commit a sé, senza cambiare nulla di ciò che si disegna.
- [x] 1. **L'agenda dice se il cielo lo permette, e porta gli avvisi del lettore.** Il primo
  differenziatore del prodotto (VISION §2.2) era solo del Cielo: su Oggi l'ora d'oro non aveva
  verdetto. E VISION §5.2.4 prometteva gli avvisi del lettore nella lista dal primo giorno,
  mai disegnati. Il tramonto ripeteva la fine dell'ora d'oro. DESIGN §8.4, VISION §5.2.
- [x] 2. **Una grammatica sola per i contenitori** (DESIGN §8.0): niente sulla carta tranne il
  titolo di una sezione; le ore su una card, l'agenda e la settimana come gruppi di segmenti, come
  il Cielo. DESIGN §8.0, §8.3, §8.5b, §6.
- [x] 3. **Un hero che si trasforma**: un'intestazione collassabile su `nestedScroll`, la
  temperatura che viaggia nella barra (elemento condiviso), la temperatura più grande, massima e
  minima del giorno nell'hero.
- [x] 4. **I dettagli uniformi**: un solo stile di icone (monocromo, `onSurfaceVariant`), gli
  indicatori in una famiglia sola, le altezze pari, il riquadro notevole del giorno in evidenza
  senza riordinare la griglia.
- [x] 5. **La striscia delle ore compatta** (circa 150 dp): 7–8 colonne a 360 dp, la probabilità
  come corsia sotto le icone col linguaggio dei segni di pioggia del nastro, la prima colonna «Ora».
- [x] 6. **Il cielo con uno shader AGSL**: nuvole e pioggia dai dati (copertura, intensità, sole),
  statico col movimento ridotto. Qui si decidono minSdk 34 e il budget dei 2 ms.
- [x] 7. **La settimana leggibile**: le due barre (temperature e nastro) sulla stessa estensione
  orizzontale, via le sette frecce a favore di un'indicazione più discreta.
- [x] 8. **Gli schermi grandi**: due pannelli su tablet e pieghevoli (cielo, ore e agenda a
  sinistra; settimana e dettagli a destra).

### Cosa è cambiato (punti 1 e 2)

- **I verdetti nell'agenda.** Ogni momento del cielo del resto della giornata porta il verdetto del
  Cielo con il suo numero («Ottimo · nuvole 19%»), su una riga sua sotto la riga. È lo stesso
  verdetto della tab Cielo: `SkyVerdictEngine` sull'occorrenza del lavoro in quel giorno
  (`SkyScheduler.resolve`), sulle stesse ore, con gli stessi dati vecchi che non danno pareri.
  Un momento che il cielo non ha oggi dice la ragione del Cielo nella sua riga («La luna è su
  tutta la notte» sotto «Buio pieno 20:54»).
- **Gli avvisi del lettore nell'agenda.** Ogni regola accesa che vale sul rapporto è una riga: la
  campanella in `primary`, il nome e il messaggio della regola come li scriverebbe la sua
  notifica (parole del lettore, mai tradotte), all'ora che il soggetto della regola indica, non
  prima che le sue fasce si aprano, e «Adesso» per una regola su questo momento, in testa. Nessuna
  riga se gli avvisi del lettore sono spenti. `TodayStateBuilder.ruleRows`, letto da
  `RuleEngine.check` come la scheda della regola in Avvisi («Adesso sì»).
- **Il tramonto dentro l'ora d'oro**: «Ora d'oro, sera — fino al tramonto, alle 19:15» al posto di
  due righe con la stessa ora (`foldSunset`). Il widget dell'arco tiene entrambe.
- **«Il prossimo, oggi · tra 6 h 20 min»**: il conto alla rovescia sale nell'etichetta.
- **Le ore su una card**, `surfaceContainer` a 28 dp; la striscia scorre sotto il suo bordo e i
  punti della curva sono ritagliati dal suo fondo (`HourStrip(ground)`).
- **L'agenda e la settimana come gruppi di segmenti.** L'agenda come le righe del Cielo (il filo
  fra le icone è andato: in un gruppo l'ordine lo dice la colonna dell'ora). La settimana in sette
  segmenti; il giorno aperto cresce nel suo segmento, che sale a `surfaceContainerHigh`.
- **La lista non spazia più niente da sé**: ogni elemento dice l'aria che gli serve (titolo 24
  sopra e 12 sotto, avvisi 12 sopra), perché lo spazio uniforme di 12 dp avrebbe staccato i
  segmenti della settimana in sette card.

### Decisioni e deviazioni

- **A2 della revisione precedente rivisto**: il «tra 6 h 20 min» stava sotto il nome, accanto a
  «fino al tramonto, alle 19:15», e a 384 dp la riga andava su tre righe. Ora sta nell'etichetta,
  come lo dice la card del prossimo momento del Cielo accanto a «il prossimo».
- **Il verdetto parte dall'inizio della riga**, sotto l'ora, per la ragione della riga del Cielo:
  sotto il nome il chip avrebbe circa 194 dp a 360 e la coppia più larga ne vuole 236.
- **Nessun verdetto `UNKNOWN` su Oggi**: la pagina del momento dice perché non si sa, e su un dato
  vecchio una fila di chip «?» ripeterebbe ciò che il chip della freschezza dice una volta.
- **Una regola non è «il prossimo»**: l'etichetta va al primo momento del giorno, perché una regola
  è la lettura del meteo del lettore, non un momento.
- **Il salto tonale fra carta e contenitori resta quello dello schema** (1,11:1 nei temi chiari,
  1,13 negli scuri, misurato): riguardato sul Milano accanto al Cielo, i contenitori si leggono;
  il difetto erano le sezioni nude di Oggi, e uno schema più forte avrebbe cambiato ogni schermata.
- **I decimali nei messaggi delle regole restano** («Fuori 19,7°» accanto all'hero «20°»). Provato
  a portarli a gradi interi come ogni altra temperatura e tornato indietro: il valore di una regola
  è quello che ha confrontato con la soglia, e una regola «sotto i 5°» scattata a 4,6° scritta
  «5°» si contraddirebbe (`RuleNotifierTest` lo tiene, 23 set 2026).
- **La regola sulla scheda del README**: il Milano registrato porta le due regole che l'immagine di
  Avvisi già mostra (le idee Bici e Corsa, create con due tocchi), così l'agenda le mostra senza
  inventare niente.

### Come è stato verificato

`TodayStateBuilderTest` con i nuovi casi: il tramonto piegato nell'ora d'oro e di nuovo riga
dentro l'ora d'oro; il verdetto `PASS · nuvole 20%` sull'ora d'oro e nessun verdetto sulle svolte
del tempo; nessun parere su dati vecchi; la regola di adesso in testa, la regola fuori fascia
all'apertura della fascia, nessuna riga per una regola che non vale, spenta o con le fasce finite.
`TodayAgendaTest` (Robolectric): l'etichetta del prossimo, il verdetto disegnato, la riga della
regola, l'apertura e la chiusura del giorno senza crash. Le immagini `today-now` e `today-week`
rigenerate e guardate; `today-day` nuova (il resto della giornata con i verdetti e le regole),
nel README con didascalia e testo alternativo. Suite completa e lint.

### Punto 3: l'hero che si trasforma (committente, 15 ott 2026)

- [x] **La temperatura viaggia nella barra** (DESIGN §8.1e). Una sola cifra, disegnata sopra la
  pagina (`TravellingNumber`): scorrendo segue il cielo finché la sua testa arriva 8 dp sotto la
  barra, poi nei 96 dp di scorrimento successivi rimpicciolisce alla misura `titleLarge` della barra
  e scivola nel suo posto accanto all'ingranaggio, dove resta. La posa è una funzione pura dello
  scorrimento (`heroPose`, `dockSlot`), letta in layout e in disegno: segue il dito nei due sensi e
  non ricompone niente mentre la pagina si muove. La parte della cifra già dentro la barra ha
  l'inchiostro della barra, quella ancora sul cielo il bianco del cielo.
- [x] **La cifra a 88 sp** (era 64), con un tetto: cresce col testo del lettore fino al 130% e non
  oltre (`heroStyle`), perché a 200% «−12°» sarebbe largo circa 400 dp su un telefono da 360.
- [x] **Massima e minima del giorno sotto la parola del cielo** («Massima 23° · minima 17°»), con il
  «percepiti» dopo di loro quando dice qualcosa: prima erano solo nella prima riga della settimana.
- [x] Il pavimento del canvas da 280 a 328 dp, quanto è cresciuto l'hero, così lo scheletro finisce
  ancora dove finisce il canvas.

**Decisioni e deviazioni.** *Non un `nestedScroll` con un'intestazione che si accorcia*, come diceva
il piano: il canvas resta un elemento della lista e la barra resta fissata sopra, e a viaggiare è
solo la cifra. Un'intestazione collassabile avrebbe dovuto rifare la barra a due fondi (§8.1b), che
è stata provata e decisa sul telefono tre volte, e avrebbe spostato il cielo sotto il dito invece di
lasciarlo scorrere; il viaggio della cifra dà la continuità che il piano chiedeva senza toccare né
l'uno né l'altra. *Niente transizione d'elemento condiviso di Compose*: quelle animano fra due stati,
qui lo stato è lo scorrimento stesso, e la cifra deve stare a ogni pixel dove il dito la porta.
*Il punto d'innesco è misurato, la meta calcolata*: dove la cifra riposa nel canvas lo misura il
canvas una volta (non cambia scorrendo); dove atterra è l'aritmetica della barra (16 dp di margine,
48 dell'ingranaggio, il centro della riga), così niente si misura durante il movimento. *Col movimento
ridotto* resta il comportamento di prima: la cifra se ne va col cielo e il numero della barra entra
in dissolvenza.

**Come è stato verificato.** `HeroTravelTest` (nuovo): a riposo la cifra sta dove la tiene il
canvas, segue il cielo fino all'innesco, a metà corsa è a metà strada e alla scala di mezzo senza
salti ai due capi, oltre la corsa e con il canvas uscito resta agganciata, la meta sta prima
dell'ingranaggio al centro della riga, il tetto del 130%. `TodayAgendaTest`: scorsa la pagina fino in
fondo, la barra dice la temperatura a TalkBack. Una sequenza di scorrimento (0, 60, 100, 130, 160,
200, 300 dp) disegnata sul Milano e guardata fotogramma per fotogramma. Le immagini `today-*` del
README rigenerate (la cifra più grande, la riga delle temperature del giorno, la cifra agganciata
nelle viste scorse); le altre identiche al pixel. Suite completa e lint.

### Punto 4: i dettagli uniformi (committente, 15 ott 2026)

- [x] **Le intestazioni in un inchiostro solo.** Il glifo della grandezza alla misura della sua
  etichetta (20 dp, era un Meteocon da 34–38) e l'etichetta, tutti e due in `onSurfaceVariant`.
  I glifi sono dieci Material Icons contornate (`MetricIcons`: ombrello, fiocco, sole, aria,
  nuvola, goccia, manometro, occhio, particelle, fiore), copiate col loro tracciato invariato dal
  jar dei sorgenti di `material-icons-extended` 1.7.8 invece di dipendere dalla libreria.
- [x] **Gli indicatori in una famiglia sola**: a riposo ogni scala è lo stesso binario,
  l'inchiostro al 12%; il colore della grandezza solo fino al valore. Erano l'intera rampa al 40%
  sotto ogni riquadro, sei arcobaleni in sei tinte.
- [x] **Le altezze pari**: il valore in alto, i fatti e la conseguenza ancorati in fondo, così
  due riquadri affiancati allineano valori e conseguenze; ogni riga alta almeno 160 dp.
- [x] **Il riquadro da notare** (`notableDetail`): al più uno, quasi sempre nessuno, su
  `secondaryContainer` con la conseguenza un gradino più su e «Da notare oggi» per TalkBack; le
  soglie sono quelle in cui la frase del riquadro cambia, e la griglia non si riordina mai.
- [x] La guida dice del riquadro da notare; la riga delle Material Icons in `licenses/README.md`.

**Decisioni e deviazioni.** *Non Meteocons tinte*: il piano diceva «un solo stile monocromo», e
la strada corta era tingere i disegni che c'erano; il 4 set 2026 il telefono aveva già mostrato
che una tinta piatta riduce un Meteocon alla sua sagoma («due sembrano uguali»). Servivano glifi
disegnati per essere letti in un inchiostro solo. *Copiate e non dipendenza*: la libreria sono
migliaia di icone da compilare e dexare per dieci, e il progetto copiava già allo stesso modo il
`place` del widget. *Le tinte delle grandezze restano* (la rampa di ciascuna, §2.3): la revisione
del 23 set aveva tolto proprio il colore unico, che diceva «quanto» senza dire «di cosa»; qui il
colore resta sul valore e sparisce dal resto della scala. *Il riquadro da notare non è un colore
che dice da solo una cosa* (§10): la riga della conseguenza la dice già a parole, l'enfasi la
indica soltanto. *Le icone Meteocons delle grandezze restano* per Avvisi, il giorno aperto e i
widget; due accessori rimasti senza utenti (`cloudCover`, `visibility(km)`) sono stati tolti.

**Come è stato verificato.** `DetailsNotableTest` (nuovo): un giorno qualunque non ha niente da
notare; ogni soglia scatta dove scatta la sua frase; l'aria sulla scala del luogo; uno solo,
nell'ordine detto. Disegnati sul Milano, oltre all'immagine del README, una variante con UV 8 in
tema chiaro e scuro, guardate prima di toglierle. `today-details` rigenerata; le altre identiche
al pixel (la guida compresa: i suoi riquadri d'esempio stanno sotto la piega dell'immagine).
Suite completa e lint.

### Punto 5: la striscia delle ore compatta (committente, 15 ott 2026)

- [x] **Celle da 42 dp che si toccano**, icona a 32 dp, curva su 16 dp di corsa a 1,5 dp per grado:
  sette ore a 360 dp, otto a 384 (erano cinque, in celle da 56 dp con 4 dp fra l'una e l'altra).
- [x] **«Ora» in testa**: l'ora in cui siamo, con la temperatura e il cielo dell'hero e la
  probabilità dell'ora; la striscia partiva dall'ora piena successiva e la curva non aveva un punto
  dove sta il lettore.
- [x] **La corsia della pioggia al posto del grafico**: una barra per ora su 0–100%, alta quanto
  l'ora è probabile, nella rampa di riempimento della pioggia, le barre vicine quasi a contatto
  perché un pomeriggio bagnato sia una forma sola; la cifra sotto dal 20% nella rampa
  dell'inchiostro; una riga sotto la striscia dice di cosa è la probabilità e quando tocca il
  massimo («Probabilità di pioggia, massima 85% verso le 18:00»). Niente corsia e niente riga in
  una giornata asciutta. La card: circa 130 dp asciutta, 160 bagnata, dove era circa 310.
- [x] Tolti `RainChart` e i suoi aiuti (la finestra delle ore in vista, il tocco che scorreva la
  striscia, gli assi) con `RainChartTest`; tolti i gradini `Tile` e `TileUv` della scala delle
  icone, rimasti senza utenti dal punto 4, con il test dei distintivi che li misurava; la pagina
  del momento del Cielo ha un gradino suo, `Page` (42 dp, la misura che aveva la striscia).
- [x] La guida (le prossime ore, «Sta per piovere?»), lo scheletro (cella da 132 dp).

**Decisioni e deviazioni.** *Il grafico è andato, non spostato*: era la mappa di una striscia che
mostrava cinque ore, con una finestra che seguiva lo scorrimento e un tocco per saltare; una
striscia che ne mostra sette o otto, con la forma della pioggia dentro, è la mappa di sé stessa, e
le cento dp del grafico erano metà della card. Le ore oltre la vista restano a un gesto. *La rampa
di riempimento per le barre, quella dell'inchiostro per le cifre*: la prima è la rampa dei segni
(§2.3) e fa leggere l'improbabile come tenue; la seconda supera 4,5:1 ed è quella dei numeri. Il
nastro della settimana usa l'inchiostro per i suoi segni bagnati perché lì il segno è alto 3 dp e
il tenue sparirebbe; qui la barra ha la sua altezza per dirlo. *La corsia anche nel giorno aperto
della settimana*: la stessa striscia, quindi le stesse otto ore e le stesse barre.

**Come è stato verificato.** Disegnata sul Milano (asciutto: niente corsia, otto ore in vista, la
card a 130 dp) e su una variante con la pioggia del pomeriggio (la forma che sale, le cifre dal
20%, la riga del massimo), guardate prima di togliere la variante. `StripCurveTest` invariato.
`today-now` e `today-week` rigenerate; le altre identiche (il widget a meno di un antialiasing che
l'occhio non vede, lasciato com'era). Suite completa e lint.

### Punto 6: il cielo con uno shader AGSL (committente, 15 ott 2026)

Il committente ha lasciato a questo passo le due decisioni aperte (minSdk 34, il budget dei 2 ms) e
ha chiesto di dare precedenza alla resa, riscrivendo il DESIGN di conseguenza.

- [x] **Il canvas è uno shader AGSL** (`SkyShaderSource`, `SkyShaderLayer`): il gradiente della
  palette disegnato com'era (i tre stop, la stessa interpolazione), e sopra la scena (DESIGN §3.9):
  cumuli nella copertura dell'ora, con il bordo verso il sole (o la luna) illuminato e schiacciati
  verso l'orizzonte; cirri nella quota dello strato alto; stelle solo a notte buia e sgombra, meno
  sotto la luna; pioggia e neve solo se l'ora è bagnata per il motore degli stati, all'intensità
  del quarto d'ora misurato; le nuvole che scorrono col vento dell'ora, nel verso in cui soffia.
- [x] **Il contratto dello scrim vale anche per la scena**: ogni pixel sotto la luminanza dello stop
  più chiaro della palette, nello shader e nei colori delle nuvole calcolati in Kotlin.
- [x] **Il movimento**: 15 fotogrammi al secondo per un cielo che scorre, 30 se cade qualcosa, un
  fotogramma chiesto per passo e mai uno per vsync; fermo, come un fotogramma solo, con il «meteo che
  si muove» spento, le animazioni di sistema ridotte, il risparmio energetico, la pagina o una
  striscia in scorrimento, il canvas fuori schermo.
- [x] Le immagini del README passano dal renderer hardware (`renderWindow`), perché un canvas
  software non disegna un `RuntimeShader`; le altre immagini sono uscite identiche al pixel.
- [x] DESIGN §3.5 riscritto, §3.9 nuovo, §7.1, VISION §4.2, la guida.

**Decisioni e deviazioni.** *minSdk resta 33*: `RuntimeShader` è di Android 13, che è già il
pavimento; passare a 34 non avrebbe dato niente allo shader. *Il budget dei 2 ms per fotogramma è
sostituito* da un budget scritto in quello che un fotogramma fa (una ventina di campioni di rumore
per pixel nel cielo peggiore, quasi nessuno in un cielo sereno) e in quando un fotogramma c'è (15 o
30 al secondo, solo mentre il meteo può muoversi), con un obiettivo da misurare: **sotto i 3 ms di
GPU per fotogramma su un telefono di fascia media**. *Il rumore è ruotato a ogni ottava*: senza, la
griglia del rumore a valori disegnava «V» e strisce diagonali (visto sul campionario). *Il coperto
appiattisce l'ombreggiatura*: con il contrasto pieno un cielo al 95% era un vortice di macchie. *La
distorsione dei cumuli a due ottave*: piega le nuvole, il dettaglio lo danno le quattro di sotto, e
costa quattro campioni in meno. *Il campionario dei cieli non va nel README*: le sue condizioni sono
inventate, e la regola delle immagini lo vieta; è servito a guardare, ed è stato tolto.

**Da verificare sul telefono** (non si può in un container senza GPU): il tempo di GPU per
fotogramma del canvas (`adb shell dumpsys gfxinfo com.callbackdev.chiaro.debug framestats`, o il
profilo GPU), in un cielo coperto e in uno con la pioggia, contro l'obiettivo dei 3 ms; e la
sensazione del movimento: la velocità della deriva e lo scintillio.

**Come è stato verificato.** `SkySceneTest` (nuovo): nessuna nuvola più chiara dello stop del
contratto a ogni altitudine, copertura e pioggia, nelle due palette; il tetto tiene la tinta; stelle
solo al buio, a cielo aperto, meno con la luna; pioggia e neve solo su un'ora bagnata; la deriva nel
verso del vento, nei due emisferi, più veloce col vento più forte; la quota dei cirri.
`SkyShaderCompileTest` (nuovo, Robolectric con la grafica nativa): lo shader compila con il
compilatore AGSL vero e accetta ogni uniform che lo schermo imposta. Un campionario di nove cieli
(sereno, sparse, coperto, cirri, ora d'oro, stelle, luna piena, pioggia, neve) disegnato con il
renderer hardware e guardato a ogni ritocco; `today-now` rigenerata (il coperto di Milano ora è un
cielo coperto); le altre identiche al pixel. Suite completa e lint.

### Punto 7: la settimana leggibile (committente, 15 ott 2026)

- [x] **Le due barre in un blocco solo**: la barra delle temperature e, 4 dp sotto, il nastro della
  giornata, sulla stessa larghezza fra la minima e la massima. Il nastro partiva da sotto l'icona e
  arrivava in fondo alla riga, la barra stava fra le cifre: due barre dello stesso giorno con due
  inizi e due fini. La minima finisce sul bordo sinistro del blocco, la massima comincia sul destro.
- [x] **La probabilità sotto l'icona**, su una riga tenuta da tutti e sette i giorni quando almeno uno
  la stampa (come la corsia bagnata): la sua colonna da 36 dp è diventata larghezza del blocco.
- [x] **Via le sette frecce**: il titolo della sezione dice una volta sola «Tocca un giorno per le sue
  ore»; TalkBack sente ancora lo stato e l'azione di ogni riga, e il giorno aperto lo dice il suo
  segmento.
- [x] La riga si spezza in due quando il blocco avrebbe meno di 72 dp (`splitsRow`): da circa il 145%
  di testo a 360 dp, il 190% a 411. La guida e il suo esempio senza la freccia.

**Decisioni e deviazioni.** *Il blocco a 122 dp e non il nastro largo*: con le due barre allineate il
nastro passa da circa 250 dp a 122 a 360 dp, 5 dp un'ora; abbastanza per un pomeriggio sereno contro
uno grigio e per i segni della pioggia, e il giorno si legge come uno solo. Il nastro largo resta
quello del canvas, che è la giornata di oggi. *L'indicazione nel titolo e non una per riga*: un
segno ripetuto sette volte è il segno che si smette di vedere.

**Come è stato verificato.** `DayRowSplitTest` riscritto sulle nuove misure (una riga a 360 e 411 dp,
fino al 140% a 360, fino al 150% a 411, il minimo del blocco al dp). `TodayAgendaTest` (lo stato
aperto e chiuso per TalkBack, il giorno che si chiude senza crash). `today-week` e `today-day`
rigenerate e guardate; la settimana intera ora sta in una schermata col giorno aperto. Suite completa
e lint.

### Punto 8: gli schermi grandi (committente, 15 ott 2026)

- [x] **La rail al posto della barra** da 600 dp di finestra (`RailMinWidth`, la soglia Material fra
  compatto e medio: pieghevole aperto, tablet, telefono in orizzontale). Stesse tre tab, stesso
  contenitore `surfaceContainer` della barra, voci al centro dell'altezza dove arriva il pollice di
  chi tiene il dispositivo per i lati. Le pagine delle tab lasciano libera la larghezza della rail
  all'inizio come lasciavano libera l'altezza della barra in fondo (`ChiaroRoot`, `NavRail.kt`).
- [x] **Oggi resta a tutta finestra sotto la rail**, con `LocalRailInset` che dice di quanto tenere il
  contenuto lontano dal bordo. La rail comincia **sotto** la barra di stato: sopra di lei la fascia di
  stato è il cielo di Oggi, con le icone bianche. Una rail fin dietro all'orologio avrebbe messo cifre
  bianche su un contenitore chiaro nel tema chiaro. Il foglio di carta comincia accanto alla rail
  (`SkySheet.start`), così il suo angolo arrotondato si vede.
- [x] **Due pannelli da 640 dp di carta** (`todayColumns`, puro, `TodayColumnsTest`): sotto il cielo, a
  sinistra le ore e il resto della giornata, a destra la settimana e cosa è cambiato. 640 è la carta
  a cui ogni pannello tiene la riga della settimana su una riga sola al 100% di testo (310 dp). I due
  pannelli insieme non superano 1200 dp e stanno al centro; 8 dp fuori da ogni pannello più i 16
  delle sezioni fanno i 24 di margine Material ai bordi e 32 di canale fra i pannelli.
- [x] **L'hero sopra il primo pannello**: cifra, parola, frase e nastro hanno la larghezza del pannello
  di sinistra, il sole e la luna tutto il cielo.
- [x] **I dettagli sotto i due pannelli, a tutta larghezza**, con tante tessere per riga quante ne
  stanno alla larghezza di una tessera da telefono (160 dp), fra 2 e 4, preferendo il numero che non
  lascia una riga corta (`detailsPerRow`): nove tessere tre per tre. Sul telefono restano due.
- [x] **Cielo e Allerte centrati a 840 dp** (`TabColumn`) su una finestra larga: sono una lista sola e
  oltre quella larghezza le righe diventano più lunghe di quanto si legga in uno sguardo.
- [x] Nuovo screenshot del README, `today-foldable` (841 × 701 dp, un pieghevole aperto), con la rail
  disegnata come la disegna la shell.

**Decisioni e deviazioni.** *I dettagli non a destra*: il piano li metteva nel pannello di destra. Nella
prima resa il pannello di destra (settimana e nove tessere) era alto più del doppio di quello di
sinistra, e in fondo alla pagina restava una schermata intera di carta vuota a sinistra. A tutta
larghezza la griglia prende tre tessere per riga e i due pannelli sopra vengono alti più o meno
uguali. *Un solo scorrimento e non due colonne indipendenti*: la pagina scorre come una, l'hero
viaggia nella barra come sul telefono (§8.1e) e il tiro per aggiornare resta uno. I due pannelli sono
un solo elemento della lista; su una pagina larga stanno comunque in una o due schermate.
*La soglia dei due pannelli è la carta e non la finestra*: un pieghevole tenuto in verticale (701 dp,
621 di carta dopo la rail) resta in una colonna, perché a 300 dp per pannello la riga della settimana
si spezzerebbe in due su ogni giorno. *Impostazioni, guida e pagine del cielo* restano a tutta
larghezza: sono pagine con la loro barra in alto e la freccia indietro, e centrarle porterebbe la
freccia a metà schermo. Restano da rivedere a parte.

**Come è stato verificato.** `TodayColumnsTest` (telefono, pieghevole verticale e aperto, il minimo dei
pannelli contro la riga della settimana, il centrato oltre i 1200 dp, le tessere per riga). Rese di
prova, poi tolte, a 841 × 701 chiaro e scuro, in fondo alla pagina, a 1280 × 800 (Oggi e Cielo),
a 701 × 841 e di Allerte. Tutte le immagini del README rigenerate: quelle del telefono sono identiche
(`widgets.png` differiva per l'antialiasing di un'area, ripristinata). Suite completa e lint.

### Due difetti visti sul dispositivo (committente, 15 ott 2026)

- [x] **Una forma geometrica nel cielo**, sopra la cifra, di sera con la luna. Riprodotta portando lo
  shader in NumPy: la luce delle nuvole si calcolava pixel per pixel *verso* la luce, e attorno al
  punto della luce quella direzione fa un giro completo. Il bordo illuminato gira con lei e disegna
  anelli e archi concentrici fino a circa 150 dp dalla luna (la luna era 140 dp a destra della
  forma, alla stessa altezza). Ora la direzione è una sola per tutto il canvas, dal centro verso la
  luce; il bordo luminoso vicino alla luce resta legato al suo punto, che non ha direzione. Nella
  stessa indagine: la distorsione delle nuvole a 0,9 piegava il piano su sé stesso nello 0,02% dei
  punti (jacobiano negativo, misurato), con pieghe che viaggiano col vento; a 0,6 il minimo del
  jacobiano è 0,21 e non piega mai. `today-now` e `today-foldable` rigenerate e confrontate: stesso
  carattere, il vortice attorno al sole sparito.
- [x] **Il foglio dei luoghi che rimbalza** dopo un trascinamento verso l'alto, finché non lo si tocca.
  Una sonda Robolectric ha misurato che, al rilascio, la molla del foglio lo porta *sopra* la sua
  posizione aperta (Material 1.4 lo concede e lo maschera). Material imbottisce il contenuto del
  foglio con la parte della barra di stato che il foglio copre: al 94% dell'altezza il foglio stava a
  pochi dp sotto la barra di stato, il piccolo superamento lo portava sotto, l'imbottitura lo faceva
  crescere, la posizione aperta saliva, la molla la inseguiva, l'imbottitura spariva e la posizione
  riscendeva. Un'altalena che solo un dito interrompe; in Robolectric, senza barra di stato, il
  foglio si fermava. Ora il contenuto non prende l'inset in alto (solo quello in basso) e l'altezza è
  la finestra meno la barra di stato e 12 dp, così l'altezza non dipende più da dove sta il foglio.
  Gli altri fogli dell'app sono alti quanto il loro contenuto: stanno lontano dalla barra di stato o
  attaccati in cima, dove l'imbottitura non cambia.

**Come è stato verificato.** Lo shader portato in NumPy (le stesse funzioni, gli stessi numeri), rese
prima e dopo su otto tratti di cielo a 50% e 80% di copertura con la luce dove l'aveva il
dispositivo; il jacobiano della distorsione misurato su 4000 × 440 dp. Il rimbalzo: una sonda con
la stessa struttura del foglio, poi tolta, e la lettura del sorgente di Material 1.4
(`consumeWindowInsets(top = offset)`, `contentWindowInsets`, `animateTo` che ammette il superamento).
Resta da confermare sul dispositivo che il foglio non rimbalzi più.

**Il cuneo nel cielo, seconda parte (committente, 15 ott 2026).** Dopo la correzione della luce restava,
sopra la cifra, un piccolo cuneo a «⌐» di circa 25 dp, con due lati netti inclinati di 10°, identico
su due cieli diversi (Cavenago di sera, Bogotá di giorno con la pioggia) e che **scivola via con le
nuvole**: quindi nel rumore, non sopra il cielo. Nessuna resa su CPU lo mostrava, nemmeno lo shader
vero disegnato da Skia in Robolectric con i parametri del dispositivo: è la GPU. Escluse la mezza
precisione (rovinerebbe tutta la texture) e la contrazione in FMA (esatta, misurata). La causa,
riprodotta in fp32: ogni cella del rumore calcola i suoi angoli come `i + (1, 0)` e simili, e un
compilatore può riscrivere `(i.x + 1)·0,1031` come `i.x·0,1031 + 0,1031` con due arrotondamenti;
la cella accanto calcola lo stesso angolo dal suo `floor`, un bit diverso, e l'hash amplifica quel bit
finché il suo ultimo `fract` scavalca un intero. Simulata così, la rottura cade **proprio a (80–100,
140–170) dp, dove il dispositivo mostra il cuneo**, con un salto di 0,13 (l'ampiezza della terza
ottava, celle di 23 dp) e la stessa forma. La correzione passa ogni angolo per `floor` (`floor(i +
(1,5; 0,5))` e simili): gli stessi numeri, ma un valore che il compilatore non può aprire. Tutte le
immagini del README rigenerate e **identiche al pixel**: l'aspetto del cielo non cambia. Resta da
confermare sul dispositivo.

### La pagina dell'arcobaleno (committente, 15 ott 2026)

- [x] **La riga dell'arcobaleno apre una pagina** (`RainbowSheet`), come i momenti del cielo aprono la
  loro: era l'unica riga del resto della giornata il cui senso è una direzione, e non apriva niente.
  Nome e finestra con la pioggia, «Dove guardare» con la stessa bussola del Cielo (`LookBlock` e
  `CompassView`, riusati), cos'è un arcobaleno in due frasi (spalle al sole, pioggia davanti),
  **quanto sarà alto l'arco in parole** e, più quieta, la nota che è una possibilità.
- [x] **`RainbowWindow.sight(at, coords)`** nel dominio: la direzione opposta al sole e la cima
  dell'arco, 42° meno l'altezza del sole; null col sole giù o sopra i 42°. La finestra ricava la sua
  direzione dalla stessa funzione. `RainbowSightTest`.
- [x] **La direzione è quella del momento**: dentro la finestra, dove sta l'arco adesso (il sole si
  sposta di circa 15° l'ora); prima, quella della finestra, la stessa che stampa la riga.
- [x] L'altezza in tre parole, mai in gradi: da 28° «grande, quasi un semicerchio», da 14° «a media
  altezza», sotto «una fascia appena sopra l'orizzonte» (`rainbowHeightRes`, `RainbowSheetTest`).
- [x] La riga dice fino a quando (la fine della finestra), come l'ora d'oro.
- [x] **La guida correggeva una fisica sbagliata**: «quando il sole è basso e *davanti a lui* la
  pioggia è probabile» metteva la pioggia davanti al sole, mentre va davanti a chi guarda, col sole
  alle spalle. Ora dice «le spalle al sole» e che la riga apre la sua pagina (IT/EN).

**Come è stato verificato.** `RainbowSightTest` (la direzione opposta al sole, l'arco più alto col sole
più basso, niente arco di notte o col sole a 68°), `RainbowSheetTest` (la pagina prima e dentro la
finestra, l'altezza in parole ai suoi confini), `TodayAgendaTest`, la parità delle stringhe. La pagina
resa in chiaro e in scuro, in italiano, e guardata, poi tolta: nessuna immagine del README la mostra,
perché Milano registrata è asciutta per tutta la settimana e il README non inventa dati.

### Tre ritocchi dal dispositivo (committente, 15 ott 2026)

- [x] **«La pioggia dovrebbe smettere» sotto «Coperto»** (Manaus). Il gradino «già bagnato» della
  frase leggeva lo stato della *prima ora* (16–17, qualche decimo di mm al 39%: per il motore degli
  stati un'ora bagnata), mentre l'eroe e la cella «Ora» mostrano le condizioni *attuali* (l'ultimo
  quarto d'ora, niente che cade): la frase annunciava la fine di una pioggia che lo schermo diceva
  assente. Ora quel gradino legge lo stesso «adesso» dell'eroe (`report.current`); gelo e nebbia
  continuano a leggere la prima ora, come prima. Un'ora che sarà bagnata senza esserlo adesso è dei
  gradini successivi: a Manaus, con il massimo al 44%, sotto la soglia dell'ombrello e del «possibile»,
  la frase non parla di pioggia. Nuovo caso in `HeadlineEngineTest`.
- [x] **Le icone piene sono il predefinito** (prima a tratto, dal 6 set): le icone ora stanno sulla
  carta (ore, settimana, agenda), dove le forme piene portano i colori a colpo d'occhio. Chi non ha mai
  toccato l'impostazione cambia con l'aggiornamento; la scelta resta in Impostazioni. Le immagini del
  README rigenerate e guardate tutte (dodici cambiano).
- [x] **L'anteprima dell'aspetto in Impostazioni aggiornata**: il cielo è quello di Oggi (la scena
  con le nuvole, ferma, e il sole dove Oggi lo disegnerebbe) e non più un gradiente piatto; l'icona
  sta sulla carta accanto al verdetto, dove vivono le icone di Oggi; e la parola concorda col numero:
  il 20% di nuvole è «Quasi sereno», non il «Nuvoloso» che diceva sopra «nuvole 20%».


## Impostazioni dei widget: una sola grammatica per la famiglia (committente, 16 ott 2026)

Revisione delle schermate di impostazione dei widget delle quattro app, con l'obiettivo che layout e
stile siano gli stessi ovunque (ognuna con le sue impostazioni). Le due di Chiaro erano la fonte del
linguaggio; queste le differenze che restavano qui.

- [x] **L'ultimo colore non si schiaccia più.** La fila dei sei dischi da 36 dp chiedeva 266 dp; su un
  telefono da 360 dp il gruppo ne lascia 256, e Terracotta diventava un ovale. Ora ogni colore è un
  bersaglio da 48 dp attorno al suo disco, in una fila che va a capo (`ColorSwatches`), come era già
  in Passo e Tempo: niente più ovale e niente più bersagli sotto i 48 dp.
- [x] **«Ripristina questo widget» su tutti i widget**, non solo sull'arco: rimette com'erano
  l'aspetto e il contenuto (`WidgetLook.defaultsFor`), mai il luogo, che è da dove arrivano i dati e non
  un aspetto. Sull'arco è uno solo per la carta dell'arco e per quella dei momenti (prima erano due
  copie). Sotto il reset, «Fatto» a 12 dp invece di 24: i due pulsanti sono una coppia.
- [x] **Il titolo è «Questo widget» anche sull'arco** (era «L'arco del giorno»): l'anteprima sotto il
  titolo dice già quale widget è.
- [x] **Da bordo a bordo, e a colonna sui pieghevoli** (`ConfigPage`): le due attività chiamano
  `enableEdgeToEdge`, e la colonna non supera i 640 dp, centrata, come le pagine di Passo e Tempo.
- [x] **I titoli dei gruppi a `SectionTop` (16 dp)**, come in Impostazioni, invece di 24.

### Decisioni e deviazioni

- **La regola dell'ordine** (committente): la fonte dei dati viene sempre per prima (qui il luogo),
  poi lo sfondo, poi il contenuto e le scelte proprie del widget. Chiaro la rispettava già.
- **Niente nuovi colori di sfondo.** Proposta valutata e scartata (committente): i sei colori sono
  scuri perché reggono l'inchiostro bianco (§2.6); le stesse tinte vivaci danno 3,1–3,7:1 col bianco
  e 2,3–2,7:1 con l'inchiostro tenue, sotto il 4,5:1 del testo piccolo.

### Come è stato verificato

`./gradlew :app:compileDebugKotlin test :app:testDebugUnitTest :app:lintDebug` (con lo script del
mirror). Nessuna immagine del README mostra le impostazioni dei widget, quindi nessuna cambia.

## Cielo e Avvisi su pieghevoli e tablet (committente, 16 ott 2026)

Domanda del committente: Oggi ha il suo layout a due pannelli (punto 8, 15 ott), Cielo e Avvisi no; lasciarle
così o dare loro un layout dedicato? **Verificato prima di decidere**, con rese di prova (poi tolte) a
841 × 701 dp (pieghevole aperto) e 1280 × 800 dp (tablet in orizzontale) della lista com'era, centrata a
840 dp (`TabColumn`):

- sul pieghevole la lista prendeva tutti i 761 dp di carta: l'orario di una riga a un capo e la sua
  campanella a 600 dp di distanza, la card di stasera una fascia blu con le parole nel terzo sinistro, la
  prossima card con mezza larghezza vuota;
- sul tablet 200 dp di carta vuota per lato, e in una schermata una sezione e mezza: stasera e metà della
  prossima card in Cielo, la risposta e l'interruttore delle allerte in Avvisi;
- e sullo stesso dispositivo Oggi stava in due pannelli e le altre due tab in una colonna sola: tre tab,
  due grammatiche.

**Deciso: due pannelli, con la regola di Oggi.**

- [x] **Una regola sola per le tre tab** (`pageColumns`, ex `todayColumns`, ora in `ui/shell/PageColumns.kt`,
  `PageColumnsTest`): due pannelli da 640 dp di carta, al massimo 1200 insieme e centrati oltre, 24 dp ai
  bordi e 32 fra i pannelli. Oggi passa la rail come margine perché è disposto a tutta finestra; Cielo e
  Avvisi sono disposti accanto alla rail e passano zero. Un test verifica che la stessa finestra dia gli
  stessi pannelli a Oggi e alle altre due: un pieghevole che mostra Oggi in due pannelli mostra in due
  tutte le tab, e tenuto in verticale (621 dp di carta) le mostra tutte in una.
- [x] **Cielo**: a sinistra stasera e i momenti di oggi e domani con la riga della guida, cioè i prossimi due
  giorni, come il pannello sinistro di Oggi è oggi; a destra il calendario («In arrivo») con la nota sulla
  previsione, come la settimana di Oggi.
- [x] **Avvisi**: a sinistra la risposta e le allerte ufficiali da cui risponde (cosa è in vigore e se il
  telefono lo dirà); a destra gli avvisi letti dalla previsione, quelli pronti e poi i propri, che è la parte
  che cresce. Senza avvisi propri i due pannelli vengono alti più o meno uguali.
- [x] **La pagina detta una volta** (`PaneBlocks`, `paneItems` in `ui/shell/PaneList.kt`): ogni schermata
  elenca i suoi blocchi attraverso, a sinistra, a destra, nell'ordine di lettura. Sul telefono ogni blocco
  resta un item della lista nell'ordine di sempre; su una pagina larga i blocchi vanno nel loro pannello e i
  pannelli sono un item solo, uno scorrimento, come in Oggi. Le schede dei propri avvisi restano con la
  loro chiave (`key`), così un interruttore anima sul posto anche nel pannello.
- [x] **La card «notifiche spente»** sta attraverso i due pannelli, come i banner di Oggi.
- [x] **Il primo titolo del pannello destro** («In arrivo», «Pronti») parte dal bordo superiore della card
  a sinistra (`PaneTitleTop`, gli 8 dp d'aria della card) invece che 16 dp più in basso. L'intestazione
  della tab ha il nome sopra il primo pannello e l'ingranaggio sopra la fine del secondo.
- [x] **Il FAB** finisce dove finiscono le sezioni del pannello destro (`columns.end`), non nell'angolo della
  finestra; l'etichetta si chiude quando la prima card del pannello sinistro è uscita dallo schermo
  (`firstInView`, misurata): sul telefono resta la regola di sempre, il primo item.
- [x] Tolti `TabColumn` e `TabColumnMax`.

**Decisioni e deviazioni.** *La stessa soglia di Oggi e non una propria*: la riga più esigente di Cielo e
Avvisi (una scheda con interruttore e orario, un momento con orario e campanella) sta già a 310 dp, e una
soglia diversa avrebbe fatto stare Oggi in due pannelli e il Cielo in uno sullo stesso schermo. *Il
calendario a destra e non stasera*: stasera è la risposta della tab e sta dove si comincia a leggere. Il
pannello sinistro di Cielo viene più lungo del destro (sul pieghevole circa 1000 dp contro 550 con la
registrazione di Milano): dipende da quanti momenti segue il lettore, e un pannello destro con i momenti di
domani avrebbe spezzato la sezione dei momenti in due. *La riga della guida a sinistra*, in fondo ai momenti,
dove era sul telefono: aprire il pannello destro con lei avrebbe messo una riga d'informazione sopra il
titolo del calendario. *I fogli restano fogli* (la pagina di un evento, l'editor di una regola, il catalogo,
le idee): Material li limita a 640 dp e li centra; farne un pannello di dettaglio accanto alla lista è un
layout a sé, aggiunto alla domanda aperta 5 di DESIGN. *Nessuna nuova immagine nel README*: `today-foldable`
mostra già cosa fa l'app su uno schermo largo, e il gruppo delle immagini resta piccolo.

**Come è stato verificato.** `PageColumnsTest` (telefono, pieghevole verticale e aperto, il minimo dei
pannelli, il centrato oltre i 1200 dp, Oggi e le tab che si dividono alla stessa finestra) e
`DetailsPerRowTest` (le tessere per riga, separate dal vecchio `TodayColumnsTest`). Rese di prova, poi
tolte, di Cielo e Avvisi a 841 × 701 (in cima e in fondo alla pagina, chiaro e scuro, con e senza avvisi
propri), a 701 × 841 (una colonna) e a 1280 × 800. Tutte le immagini del README rigenerate: identiche, perché
il telefono e Oggi non cambiano. Suite completa e lint.

## Il cielo coperto: il banco basso, il velo alto, sole e luna (committente, 16 ott 2026)

Il committente ha mandato due schermate di prova, tutte e due «Coperto»: una con un banco grigio
compatto, l'altra con nuvole stirate a strisce, e ha chiesto se fosse voluto e se fosse realistico.
Le strisce erano volute (lo strato alto, DESIGN §3.9), ma guardando le due immagini sono emersi tre
difetti, da correggere per ogni luogo e solo dai dati meteo: sotto un banco basso il sole restava
luminoso come sotto un velo e la luna di giorno si vedeva ancora; il banco chiuso aveva buchi blu;
un velo alto chiuso restava a strisce, con il sole senza disco.

- [x] **La copertura si divide in banco e velo** (`cloudDecks`): lo strato basso e il medio sono il
  banco (i cumuli), l'alto il velo (i cirri), sovrapposti a caso come il totale del fornitore
  (misurato sulle quattro previsioni registrate, 672 ore: il totale è la sovrapposizione casuale degli
  strati entro l'1% in media, 10 al peggio). Nessuno dei due supera il totale; senza strati, o con
  strati tutti a zero e un totale nuvoloso, tutto il totale è banco, come prima.
- [x] **L'opacità della nuvola** (`cloudOpacity`): quanta luce prende, non quanto cielo copre. Basso
  per intero, medio 0,8, alto 0,3, combinati come la copertura e mai oltre; un'ora bagnata prende
  tutta la sua copertura. Le ragioni: l'atlante delle nubi del WMO (il cirrostrato non impedisce mai
  le ombre, l'altostrato mostra il sole «come dietro un vetro smerigliato») e l'UV del fornitore nella
  Milano registrata (0,75 sotto il 100% di nuvole basse, 4,45 sotto il 92% di alte, 4,55 col sereno).
- [x] **Sole e luna leggono l'opacità** (`SkyBodies.cloudOpacity`, dalla scena: una fonte sola).
  `sunVeil`: il disco svanisce con l'opacità e il bagliore ora si attenua anche (il nucleo da 0,55 a
  0,30) mentre si allarga: sotto uno strato il sole è un chiarore largo e tenue, sotto un velo alto
  tiene il disco nell'alone. `moonVeil` (nuovo): di giorno la luna se ne va con la luce dietro un banco
  basso, più pallida dietro un velo; di notte il disco se ne va e resta un bagliore tenue e largo.
  Il bordo luminoso delle nuvole vicino alla luce segue l'opacità.
- [x] **Il banco prende la sua quota del canvas** (`deckThreshold`, `SkyDeckTest`): la soglia dei
  cumuli è letta da una tabella misurata sul rumore dello shader (1,8 milioni di punti, sessanta canvas
  di 400 × 300 dp lungo 100 000 dp di deriva), alla quale l'alfa medio delle nuvole è il banco, ogni 5%.
  La retta da 0,80 a 0,16 di prima disegnava il 2% di nuvole per il 20% di copertura, il 26% per il
  50%, e al 100% lasciava l'11% del cielo al gradiente: i buchi blu. Al banco chiuso la soglia sta sotto
  il minimo del rumore, quindi nessun buco.
- [x] **L'ombra del banco chiuso è grigia**: verso un grigio della sua stessa luminanza, fino all'80%
  a banco chiuso, perché il sotto di uno strato è illuminato dalla nuvola e non dal blu sopra.
- [x] **Il velo alto si chiude in un velo** e sta **dietro** il banco: strisce stirate 7,5 a 1 finché
  è sparso; dal 60% al 100% lo stiramento scende a 4 a 3 e le strisce diventano un velo lattiginoso,
  appena disuguale. I cumuli non si sbiadiscono più per far vedere i cirri.
- [x] La guida (IT/EN) lo dice in una frase: sole e luna si vedono attraverso un velo alto, non dietro
  un banco basso e spesso. DESIGN §3.9 e §8.1c aggiornati.

**Decisioni e deviazioni.** *La taratura vale per tutta la scala, non solo per il coperto*: il
DESIGN diceva già che la copertura è «la quota del cielo che le nuvole prendono», e il 20% disegnato
come il 2% non lo era; ora un cielo al 25% ha nuvole visibili dove prima ne aveva appena. Un canvas
da solo copre pochi grandi riccioli di rumore e si scosta di qualche punto dalla media, come un'occhiata
al cielo vero. *Il velo dietro il banco*, non sopra: è la quota, e un velo sopra un banco chiuso avrebbe
steso una pellicola lattiginosa su nuvole che lo nascondono. *I pesi dell'opacità sono tre numeri e non
uno spessore*: il fornitore non dà lo spessore delle nuvole; la quota è quello che c'è, e l'atlante e l'UV
dicono che basta a separare i due cieli delle schermate. *Le stelle restano sulla copertura*: dietro un
cirrostrato si vede la luna e quasi nessuna stella, ed è quello che già succede. *Il gradiente non cambia*:
lo legge anche il nastro, e il velo lattiginoso lo dice già la scena. *Nessuna città nel codice*: tutto
viene dagli strati, dalla copertura e dallo stato dell'ora.

**Da verificare sul telefono**: la resa del velo chiuso e del banco chiuso sul GPU vero, di giorno e di
notte, e il tempo di GPU per fotogramma (il velo costa quanto i cirri di prima, gli stessi campioni di rumore,
e il banco quanto prima).

**Come è stato verificato.** `SkySceneTest`: banco e velo dagli strati (senza strati, strati vuoti,
sovrapposizione casuale, mai oltre il totale), l'opacità per strato e per un'ora bagnata, il bordo
luminoso sotto un velo e sotto un banco, l'ombra che perde la tinta e tiene la luminanza. `SkyBodiesTest`:
il sole con il disco sotto un velo alto e senza sotto un banco basso, la luna di giorno e di notte.
`SkyDeckTest` (nuovo): il rumore del banco rifatto in Kotlin riga per riga (con le righe dello shader
fissate da un test), la quota disegnata uguale al banco entro il 3% dal 10 al 90%, il banco chiuso senza
un pixel scoperto, la soglia sempre decrescente. `SkyShaderCompileTest`: lo shader compila e prende ogni
uniform. Un campionario di otto cieli (strato chiuso, velo chiuso, velo al 60%, misto, basso al 25 e al
50%, notte con luna piena sotto uno strato e sotto un velo), prima e dopo, disegnato con il renderer
hardware, guardato e tolto. Rigenerate `today-now` e `today-foldable` (la Milano delle 12: 66% basse, 13%
medie, 49% alte; ora un banco al 70% con il velo negli spazi, il sole un disco tenue) e `settings` (il 20%
dell'anteprima ora ha le sue nuvole); le altre identiche. Suite completa e lint.

## «Sei offline» con il Wi-Fi acceso, e la pagina vuota che non si tira (committente, 17 ott 2026)

**Il difetto, visto sul telefono.** Dalla mattina l'app non si aggiornava più: il banner diceva
«Aggiornamento non riuscito: sei offline» con il Wi-Fi acceso, una pagina senza ancora un report restava
sullo scheletro e il gesto di trascinare in basso non faceva niente. Cancellare dati e cambiare città non
cambiava nulla.

**La causa era fuori dall'app, l'etichetta dentro.** Misurato dal sandbox nello stesso pomeriggio: l'host
delle previsioni di Open-Meteo falliva una stretta di mano TLS su due (`SSL_ERROR_SYSCALL` dopo ~30 s),
e le risposte che passavano impiegavano ~4 s (`generationtime_ms` 4214); la qualità dell'aria rispondeva
normale. Il report, quando arrivava, era integro. Ma `wrapErrors` trasformava ogni `IOException` in
`NoNetwork`, e il banner diceva al lettore che il problema era il suo telefono.

**Cosa è cambiato.**

1. *Offline vuol dire offline.* Solo un nome che non si risolve (`UnknownHostException`, quello che
   Android dà senza una rete) è `NoNetwork`; una connessione rifiutata, interrotta, una stretta di mano
   ferma, un timeout sono `WeatherException.Unreachable` e a schermo il già esistente «il servizio non
   risponde» (`TodayError.SERVICE`). Il job periodico tratta `Unreachable` come `NoNetwork`:
   `Result.retry()`, il backoff di WorkManager riprova prima del periodo successivo.
2. *Un secondo tentativo per una connessione caduta*, lo stesso che i 5xx hanno dal 24 set: uno, dopo
   2 s. Non per un telefono offline (niente cambia in 2 s e il report in cache è già a schermo) e non per
   il `callTimeout` esaurito (sono già 30 s spesi; altri 30 sarebbero una pagina che non si risolve).
   Con un errore su due, la probabilità che un aggiornamento fallisca scende da 1/2 a 1/4.
3. *La pagina senza report prende il trascinamento.* Non aveva un `PullToRefreshBox`: lo stato `Empty`
   ora ce l'ha, con la colonna scorrevole perché il gesto arrivi al box.
4. *Un trascinamento durante un aggiornamento automatico si unisce a quello*: prima `fetch` tornava
   indietro in silenzio per via di `inFlight`, e con un servizio che impiegava 30 s a fallire il gesto
   non mostrava niente. Ora l'indicatore gira finché la richiesta in corso non risponde; una seconda
   richiesta non parte.

**Decisioni e deviazioni.** *Niente `ConnectivityManager`*: decidere «offline» dalla rete del sistema
avrebbe portato un `Context` nella classificazione, e l'eccezione lo dice già. Un Wi-Fi senza internet
dove il DNS risponde ma nulla passa si legge «il servizio non risponde»: meno preciso, non falso.
*La ricerca dei luoghi* mostra per `Unreachable` il suo «la ricerca non ha funzionato, riprova», non
«sei offline». *Il «tap» che non rispondeva*: lo scheletro non ha nulla da toccare; l'intestazione
(luoghi, impostazioni) resta attiva. Il gesto che mancava era il trascinamento, ora c'è.

**Come è stato verificato.** `TransientRetryTest`: una connessione caduta (TLS, rifiutata, timeout di
lettura, fine del flusso) ha un secondo tentativo e il report; un nome non risolto e il `callTimeout`
esaurito no; solo il nome non risolto dice `NoNetwork`. `WeatherSyncWorkerTest` (la porta chiusa del test
ora è `Unreachable`, con ritardo zero). Suite completa e lint.

## La pioggia nelle prossime due ore (committente, 17 ott 2026)

**La richiesta.** Dire quando arriva e quando smette la pioggia **nei prossimi minuti**, dove i dati
lo permettono, con una qualità da app premium. Il Nord Italia è il caso d'uso dichiarato.

**I dati, misurati prima del codice.** Open-Meteo risponde `minutely_15` ovunque, ma solo tre dei
suoi modelli calcolano davvero il quarto d'ora: ICON-D2 del DWD (Europa centrale), AROME di
Météo-France (Francia, 6 ore), HRRR della NOAA (Stati Uniti). Altrove la serie è **quella oraria
divisa in quattro**. Misurato quel pomeriggio, `best_match` contro ciascun modello nello stesso punto,
192 quarti per città:

| Città | `best_match` = | Ore bagnate | di cui piatte | Lettura |
|---|---|---|---|---|
| Milano, Bologna, Ancona, Firenze, Torino, Venezia, Genova, Trieste, Bolzano | ICON-D2, 0 differenze | Milano 8, Genova 10 | 0, 1 | calcolato |
| Zurigo, Monaco, Vienna, Lubiana, Praga | ICON-D2, 0 differenze | Zurigo 9, Monaco 7, Praga 2 | 0 | calcolato |
| New York | HRRR, 0 differenze | 4 | 0 | calcolato |
| Londra | né ICON-D2 (20 differenze) né AROME (16) | 3 | 3 | interpolato (UKMO) |
| Amsterdam | non ICON-D2 (54 differenze) | 9 | 9 | interpolato (KNMI) |
| Perugia, Roma, Napoli, Bari, Palermo | ICON-D2 assente dalla risposta | 0 | — | fuori griglia |
| Parigi, Nizza | ICON-D2 e AROME uguali | 0 | — | asciutto, non deciso |

Due cose decise da queste righe: **la griglia non basta** (Londra e Amsterdam ci stanno dentro e sono
interpolate), quindi conta il paese; e **l'interpolazione si riconosce** dalla forma (quattro quarti
uguali in ogni ora bagnata). E una terza, la ragione per cui la sezione non può contraddire la striscia
delle ore: a Milano i quattro quarti di ogni ora sommano esattamente l'ora (19:15–20:00 = 0,3 + 0,1 +
0,1 + 0,8 = 1,3 mm, lo slot orario delle 20:00), perché quarti e ore vengono dallo stesso modello.

**Cosa è cambiato.**

1. *La richiesta*: `minutely_15=precipitation,snowfall` e `forecast_minutely_15=24` nella stessa
   chiamata delle previsioni, nessuna chiamata in più. Ventiquattro quarti, sei ore: la sezione ne
   mostra otto, gli altri servono a leggere la forma della serie. Il primo quarto è quello appena
   finito (lo stesso del blocco `current`), e il motore lo salta.
2. *`NowcastCoverage`* (`:core:domain`): coperto è un posto il cui paese ha `best_match` = un modello
   che calcola il quarto **e** che sta nella sua griglia, meno un margine di 0,25° (la fascia dove un
   modello ad area limitata è ancora rilassato verso quello che lo contiene). ICON-D2: IT, SM, CH, LI,
   AT, DE, SI, CZ, nella griglia 43,18–58,08° N, 3,94° O–20,34° E (Firenze e Ancona sì, Siena e Perugia
   no); HRRR: US contigui. Per codice ISO come `PlaceRegion`: un posto senza codice non è coperto,
   mai indovinato. E `looksInterpolated`: se **ogni** ora bagnata della serie ha quattro quarti
   uguali, la serie è scartata. *Fallisce chiuso*: una pioviggine regolare disegnata piatta da un
   modello vero, sola nella sua serie, nasconde la sezione per quel fetch.
3. *Il mapper* tiene i quarti solo dove la copertura lo dice e la forma regge; ogni slot è il quarto
   che FINISCE alla sua etichetta, come le somme orarie. Uno slot senza valore lascia un buco, non uno
   zero, e il buco chiude la finestra.
4. *`NowcastEngine`* (`:core:domain`): la finestra parte dal quarto in cui siamo, tagliato a ora, e
   chiude sull'ultimo quarto intero entro due ore. Lo stato di un quarto è di `WeatherStateEngine`,
   sul quarto come tasso orario (×4) e con la **probabilità della sua ora**: un quarto in un'ora sotto
   il 20% è una traccia del modello, come l'ora. Così le icone delle ore e la sezione dicono la stessa
   cosa. L'intensità è la scala già del motore (AMS: debole sotto 2,5 mm/h, forte da 7,6; neve: 0,2 e
   0,8 cm/h). La pioggia che gela, che le quantità non mostrano, prende lo stato della sua ora solo
   nei quarti misurabili. Niente lettura se il fetch ha più di un'ora (una precisione al quarto d'ora
   non sopravvive a un dato vecchio) o se i quarti davanti coprono meno di un'ora.
5. *La frase in cima* (`Headline.NextRain`): un gradino dopo l'allerta e il maltempo, prima di
   «piove ora» e dell'ombrello orario, che dove la copertura c'è non parlano più della stessa ora.
   «Pioggia debole tra circa 20 minuti, per circa 30 minuti»; «La pioggia smette tra circa 25 minuti,
   poi riprende verso le 15:15»; «Pioggia moderata almeno fino alle 16:00»; «La pioggia smette a
   momenti» quando l'eroe mostra pioggia e i quarti davanti sono asciutti. Sotto l'ora si dice in
   minuti, arrotondati a cinque; sotto i dieci è «a momenti»; da un'ora in là, l'orario. I widget non
   ricevono la lettura: una scheda ridisegnata una volta l'ora non può dire «tra circa 20 minuti».
6. *La sezione* «Pioggia nelle prossime 2 ore» (DESIGN §8.3c), sopra le ore, solo quando nella
   finestra cade qualcosa: una barra per quarto d'ora su una scala fissa, le parole delle bande come
   unico asse, «Ora» e le mezz'ore sotto; la didascalia è l'aritmetica della frase (da quando a quando,
   il quarto più intenso con il suo mm/h e la sua parola) e il suo equivalente testuale. Un tocco apre
   la pagina: la finestra, ogni quarto a parole (gli asciutti consecutivi in una riga sola), da dove
   viene la previsione, la regola dello 0,1 mm e del 20%, e dove funziona.
7. *La guida* ha «Le prossime due ore».

**Decisioni e deviazioni.**

- *La stessa chiamata, non un modello esplicito.* Chiedere `models=icon_d2` a parte avrebbe dato la
  certezza del modello al prezzo di una seconda chiamata e di una seconda previsione, che avrebbe
  potuto dire un'altra cosa rispetto alla striscia delle ore dove `best_match` non è ICON-D2.
  Con `best_match` quarti e ore sono la stessa previsione per costruzione; la copertura e la forma
  dicono dove crederle.
- *Due ore, non una.* Con un passo di 15 minuti un'ora sono quattro barre: troppo poche per una forma.
  Otto lo sono, e due ore sono ancora il raggio in cui un modello rifatto ogni ora o tre ha senso al
  quarto d'ora.
- *Asciutto, niente sezione.* Una scheda che dice «niente pioggia nelle prossime due ore» ogni giorno
  asciutto sarebbe la cosa più ripetuta della pagina; la frase in cima e le ore lo dicono già.
- *La Francia resta fuori*: Parigi e Nizza erano asciutte, e un'uguaglianza tra zeri non dice quale
  modello parla. Si aggiunge quando una misura bagnata lo dice.
- *I colori delle barre* partono dal punto 80% della rampa della pioggia: il 70% misurava 2,48:1 sulla
  card, sotto il 3:1 di un segno. `PaletteContrastTest` lo ha trovato prima del telefono.
- *Lo screenshot del README non c'è*: la registrazione di Milano (25 set) non ha `minutely_15`, e una
  nuova registrazione cambia ogni immagine, quindi è una modifica a sé, da fare in un momento di
  pioggia vera su Milano («nessun dato inventato»).

**Come è stato verificato.** `NowcastCoverageTest` (le città misurate dentro e fuori, le serie
registrate di Milano, Genova e New York accettate, quelle di Londra e Amsterdam scartate);
`NowcastEngineTest` (la finestra, inizio, fine, pausa, durata oltre la finestra, soglia di probabilità,
0,1 mm, intensità, neve, misto, gelicidio, «sta per smettere», dato vecchio, buco); `WeatherReportMapperTest`
(l'inizio del quarto, fuori copertura, serie piatta, cache senza quarti, slot senza valore);
`NextRainTextTest` (le frasi in italiano e in inglese, gli arrotondamenti, il posto nella scala);
`TodayStateBuilderTest` (sezione e frase con la pioggia, niente con l'asciutto); `PaletteContrastTest`
(le barre ≥ 3:1 su `surfaceContainer` in ogni schema, più forti salendo d'intensità). La pagina è stata
disegnata su Robolectric sopra la Milano registrata con quarti veri di Milano e di Genova, chiaro e
scuro, italiano e inglese, testo al 200%, pieghevole: a 200% le parole delle bande si sovrapponevano,
e l'altezza del grafico ora è misurata in testo. Suite completa e lint.

## La frase dell'eroe tra le cifre e il nastro (committente, 17 ott 2026)

**La richiesta.** Dallo schermo: lo spazio tra «Massima 21° · minima 14°» e «Domani ombrello dalle
11:00» è giusto o è troppo? In mezzo ci finisce qualcosa?

**Misurato sullo screenshot** (1080 px di larghezza, circa 2,75 px/dp), aria tra i segni di una riga e
quelli della successiva: parola del cielo → cifre del giorno circa 17dp; cifre → frase circa **25dp**;
frase → nastro circa **26dp**; nastro → orari circa 11dp. In mezzo non c'è niente: il salto era
`HeroGroupGap` (12dp), e l'unica riga che può comparire lì, la nota della stima, sta 4dp sotto le
cifre, prima del salto. Il difetto era un altro: nel codice 12 e 16dp, sullo schermo la stessa aria,
perché una riga di testo porta la sua interlinea sopra e sotto e il nastro no. La frase stava a metà
tra le cifre e il nastro e non apparteneva a nessuno dei due; e il nastro, che deve chiudere il
canvas (DESIGN §8.1a), non si staccava dalla prosa più di quanto la prosa si staccasse da sé.

**Cosa è cambiato.** `HeroGroupGap` 12 → **8dp**, `HeroRibbonGap` 16 → **20dp**. L'aria diventa 17,
21, 30: ogni gradino più grande di quello sopra. La somma resta 28dp, quindi `CanvasBaseHeight`
(328dp), la fascia del cielo e lo scheletro non cambiano.

**Decisioni e deviazioni.**

- *Spostare i 4dp, non toglierli.* Togliere solo i 4dp sopra la frase avrebbe accorciato il canvas e
  lasciato il nastro a pari distanza; aggiungerli solo sopra il nastro avrebbe allungato il canvas di
  4dp e lasciato la frase lontana dalle cifre. Spostarli corregge entrambe le cose a costo zero.
- *8dp e non 4.* A 4 la frase sarebbe entrata nel gruppo dell'ora (le righe dentro un gruppo stanno a
  4dp); la frase parla di dopo, e il passo da 17 a 21dp di aria lo dice ancora.

**Come è stato verificato.** Suite completa e lint; gli screenshot del README (`today-now`,
`today-foldable`) rigenerati, guardati e rimisurati: 17, 18–21 (secondo la discendente della g di
«High»), 29dp d'aria, i gradini che salgono come previsto.

### I widget dicevano i minuti (committente, dal telefono, 17 ott 2026)

**Il difetto.** Il widget «In breve» di Ulma mostrava «La pioggia smette tra circa 45 minuti, poi
riprende tra circa…»: la frase al quarto d'ora su una scheda che si ridisegna a ogni
sincronizzazione, cioè una frase in minuti destinata a restare sulla schermata per un'ora. Era
esattamente quello che la voce sopra diceva di escludere («I widget non ricevono la lettura»), ed era
falso: `HeadlineEngine.headline` riceve davvero `null` di default, ma i widget non lo chiamano; riusano
lo stato di Oggi (`WidgetData` → `TodayStateBuilder.build`), e lì la lettura veniva passata.

**Cosa è cambiato.** `TodayStateBuilder.build` ha `readQuarters` (vero di default, per la pagina);
`WidgetData` passa falso, quindi lo stato dei widget non ha né la sezione né la frase al quarto d'ora,
e la loro frase torna quella della scala oraria («La pioggia dovrebbe smettere verso le…»).

**Come è stato verificato.** `TodayStateBuilderTest`: con `readQuarters = false` nessuna lettura e
nessun `Headline.NextRain` sugli stessi quarti che sulla pagina li producono. La lezione resta scritta
qui: un default su un parametro non è una garanzia, se chi lo dovrebbe rispettare passa da un'altra
porta.

## Il segno di «La mia posizione» nel foglio Luoghi (committente, dal telefono, 17 ott 2026)

**La domanda.** Dallo schermo: l'icona della riga «La mia posizione» sembra piccola e non si
riconosce; meglio più grande?

**Non era la taglia.** La riga disegnava `mc3_compass` presa per nome, la faccia *line su fondo
chiaro*, senza passare da `ChiaroIcons` che sceglie la faccia per il fondo. Il cerchio della bussola
è `#182234`, quasi il colore del foglio scuro: spariva, e restavano due aghi storti. Ingrandita, la
stessa cosa più grande. E la bussola era comunque il segno sbagliato: «la mia posizione» porta il
segnaposto ovunque altro (l'intestazione di Oggi, la guida, i widget con `ic_place_pin`).

**Cosa è cambiato.** La riga prende `Icons.Outlined.LocationOn` a 24dp (la misura dell'icona
iniziale di un `ListItem`), nel colore di contenuto della riga, che segue il tema da sé. La bussola
non la disegnava nessun altro: passa da `SHIPPED` a `PLANNED` in `tools/shipped_icons.py`, e
`MeteoconsSets.kt` è rigenerato dall'importatore (quattro voci in meno, nessun'altra differenza: lo
stesso pacchetto `@meteocons/svg@3.0.0-next.10` ha riprodotto l'albero identico prima del cambio).

**Come è stato verificato.** Suite completa e lint. Il foglio Luoghi non è in nessuno screenshot del
README, quindi non ce n'è da rigenerare.

## La nota in fondo alla pagina dei prossimi quarti d'ora (committente, dal telefono, 17 ott 2026)

**La richiesta.** La descrizione in fondo alla pagina «Pioggia nelle prossime 2 ore» è troppo
lunga per una schermata d'app, e la guida la dice già: basta un'informazione concisa.

**Cosa c'era.** Un titolo, «Da dove viene», e tre paragrafi: il modello (griglia, servizi
meteorologici, «non è un radar»), la regola del quarto bagnato (0,1 mm e un'ora al 20%), e dove la
sezione compare. Letti con la pagina davanti, uno solo serve a chi guarda le barre: che è un modello,
quindi inizio e fine possono spostarsi. La regola è aritmetica interna (la stessa delle icone delle
ore, che la guida non spiega da nessuna parte: spiegarla solo qui era incoerente); il «dove» non
serve a chi ha la pagina aperta, che è per forza in una zona coperta, e la guida lo dice già.

**Cosa è cambiato.** Titolo e tre paragrafi diventano una riga, `nowcast_sheet_note`, in
`bodySmall` e `onSurfaceVariant` sotto le righe dei quarti: «Previsione di un modello ad alta
risoluzione, non di un radar: l'inizio e la fine possono spostarsi di un quarto d'ora o due.» La
guida perde «e per sapere da dove viene la previsione», che prometteva una spiegazione non più sulla
pagina. Le tre stringhe tolte non hanno altri lettori. DESIGN §8 aggiornato.

**Come è stato verificato.** Suite completa e lint. La pagina non è in nessuno screenshot del README.

### La pagina dei quarti d'ora che rimbalza (committente, dal telefono, 17 ott 2026)

**Il difetto.** Aperta del tutto e poi tirata appena in giù, la pagina «Pioggia nelle prossime 2
ore» rimbalzava di qualche pixel finché non la si toccava: lo stesso difetto del foglio dei luoghi
(15 ott), corretto allora solo lì.

**La causa, letta nel sorgente di Material 1.4.** La posizione aperta è l'altezza della finestra
meno quella del foglio (`Expanded at fullHeight - sheetSize.height`); il contenuto prende l'inset in
alto (`BottomSheetDefaults.windowInsets` è `safeDrawing` sopra e sotto) meno la parte che il foglio
già lascia libera (`consumeWindowInsets(top = offset)`). Un foglio che si ferma dentro la fascia
della barra di stato cambia altezza di tanto quanto si muove: la posizione d'arrivo si sposta con
lui, e la molla la insegue. La pagina dei quarti d'ora, accorciata dalla nota di oggi, si ferma
proprio lì; ogni foglio dell'app alto quasi quanto lo schermo poteva farlo.

**Cosa è cambiato.** Un componente solo, `ChiaroSheet` (`ui/components/`), per tutti e sette i
fogli: il contenuto prende solo l'inset in basso, e l'altezza ha un tetto alla finestra meno la
barra di stato e 12dp, con il contenuto che scorre dentro (già lo faceva in ogni foglio). Il foglio
dei luoghi perde la sua correzione locale e prende la stessa, con `fillHeight` perché la ricerca
che risponde sotto il campo non lo faccia saltare. `OneSheetTest` fa fallire la build per un
`ModalBottomSheet` scritto fuori dal componente: il prossimo foglio sarà scritto dall'esempio di
Material, non da questo.

**Decisioni e deviazioni.**

- *Tutti i fogli, non solo questo.* Il primo rimedio era stato locale e il difetto è tornato due
  giorni dopo su un'altra pagina; la causa è nel componente, quindi il rimedio sta lì.
- *Un tetto, non un'altezza fissa.* I fogli corti restano alti quanto il loro contenuto; solo
  quello dei luoghi riempie lo spazio, per la ragione sopra.
- *Un effetto visibile:* un foglio lungo che prima arrivava sotto la barra di stato ora si ferma
  12dp sotto, con il velo a dire che sotto c'è la pagina, come quello dei luoghi dal 15 ott.

**Come è stato verificato.** Suite completa e lint, `OneSheetTest` compreso. Nessun foglio è negli
screenshot del README. Resta da confermare sul dispositivo, come allora: Robolectric non ha la barra
di stato, e senza la barra il foglio non rimbalza nemmeno prima.

## Le icone della famiglia, rese omogenee (committente, 18 ott 2026)

**La richiesta.** Una review delle icone delle quattro app (Chiaro, Passo, Tempo, Saldo): se sono
omogenee e fanno pensare a una sola suite, i punti forti e cosa correggere. Poi, dopo averne
ragionato insieme: gli emblemi non devono sembrare chiusi in un cerchio (sole, orologio e moneta
sono rotondi di natura, non racchiusi), più «premium»; le icone della barra di stato non più
l'anello ma l'oggetto dell'app; Chiaro disegnata da uno script come le altre; palette e luce
uniformate.

**La review.** La grammatica comune regge (fondo `#F7F4EE`, anello di raggio 21 e tratto 10,
emblema ambra che lo taglia, i quattro emblemi sulle quattro diagonali). Le stonature: il sole di
Chiaro sfumato a sfera, l'unico emblema in 3D; il disco senza raggi che si legge come un bollino;
l'unica icona del launcher scritta a mano.

**Cosa è cambiato in Chiaro.**

- `tools/draw_launcher_icon.py`, nuovo: scrive i due strati dell'icona e `ic_stat_chiaro.xml`. Il
  disegno dell'anello resta quello del 23 set, valore per valore.
- **Il sole**: piatto, un disco (5,6) e otto raggi corti e pieni (da 7,6 a 9,4, larghi 2,6) nell'ambra
  di famiglia `#FFC658` → `#EF8618`, luce dall'alto a sinistra, dov'era una sfera sfumata. Il vuoto
  nell'anello passa a 2,6 oltre le punte dei raggi (raggio 13,3). Scelta del committente fra le
  varianti disegnate (disco piatto, 8 raggi sottili, 12 raggi corti, 8 raggi pieni): i raggi sottili
  pesavano meno dell'anello, il disco piatto restava un bollino.
- **La barra di stato**: il sole con una nuvola davanti, non più l'anello. Il sole da solo è l'icona
  della luminosità di Android; con la nuvola dice «meteo». La prima stesura, con la nuvola fino al
  centro del sole, ne copriva troppo (parere del committente, condiviso): la nuvola è scesa e si è
  fatta piatta, un banco basso sotto un sole più grande che ne copre solo l'angolo in basso a
  sinistra. Fra le due stesure rimaste (una nuvola piccola e tonda, una piatta) il committente ha
  scelto la piatta.
- DESIGN §3.8 e §8 (la riga dell'icona piccola), CLAUDE.md (l'icona è l'output di uno script).

**Come è stato verificato.** Rese dei veri XML (cerchio, squircle, quadrato, 48 px, a tema chiaro e
scuro, su sfondo scuro, la barra di stato a 24 px su chiaro e scuro) guardate accanto alle altre tre
app; suite completa e lint; `notification-summary.png` rigenerato, perché mostra l'icona piccola.

### Da fare sul dispositivo

- [x] L'icona sulla Home con due launcher (maschere diverse) e a tema; la barra di stato con una
  notifica vera. Verificato dal committente sul dispositivo (18 ott 2026), con la nuvola piatta.

## Gli onboarding della famiglia, resi uniformi (committente, 18 ott 2026)

**La richiesta.** Uniformare gli onboarding delle quattro app nello stile e nella tipografia, senza
l'icona dell'app nella prima pagina, prendendo Chiaro come modello: niente icona, la sua tipografia,
niente indicatore di pagina, il suo modo di trattare i permessi. Scelte del committente sul piano:
pulsanti in basso, i punti di forza della prima pagina alleggeriti (nelle app che li hanno), Saldo
da cinque a quattro pagine.

**Lo standard di famiglia.** Nessuna immagine; la prima pagina è il nome (`displaySmall`) e la sua
frase (`titleMedium`); un passaggio è un titolo (`headlineMedium`) e un testo (`bodyLarge`), centrati
nello spazio sopra i pulsanti; i pulsanti stanno in basso, sempre nello stesso punto (il principale
pieno, un'alternativa vera contornata, «Non ora» di solo testo, alti almeno 52 dp), con la riga sotto
il principale in `bodySmall`. Nessun indicatore di avanzamento: il passaggio lo sente chi usa
TalkBack, sul titolo. I permessi: prima si spiega, poi si chiede; «Non ora» non costa nulla e non
mostra testi rossi; quando il dialogo non può più comparire, lo stesso pulsante apre le impostazioni;
la schermata che usa il permesso continua a offrirlo.

**L'indicatore di pagina.** Material non lo impone: è tipico dei caroselli che si sfogliano e dei
percorsi lunghi. Qui le pagine si avanzano solo con i pulsanti e sono al massimo quattro (cinque in
Passo solo sui telefoni che chiedono il consiglio sulla batteria).

**Cosa è cambiato in Chiaro** (VISION §5.8, «The frame»).

- I due passaggi in una cornice sola: il testo centrato in alto, i pulsanti in basso, alti almeno
  52 dp (erano 40, sotto il testo, e si spostavano con la sua lunghezza).
- Il titolo è il nome nudo (`first_run_title`): con `app_name` una build di prova diceva «Chiaro
  (dev)».
- Il secondo passaggio prende la scala del primo: titolo `headlineMedium` (era `headlineSmall`),
  testo `bodyLarge` (era `bodyMedium`).
- La posizione negata per sempre: il pulsante diventa «Apri le impostazioni», dove prima la
  richiesta tornava rifiutata senza dialogo e il pulsante non faceva più nulla; tornando con il
  permesso dato, la posizione si cerca da sola. Il rifiuto è detto in grigio, l'errore resta per la
  posizione spenta o il segnale che non arriva.
- Dal luogo alle notifiche il passaggio scorre ed entra in dissolvenza, come una pagina (solo
  dissolvenza con l'animazione ridotta); prima era un taglio.
- TalkBack sente «Passaggio 1 di 2» sul titolo.

**Come è stato verificato.** Rese delle due pagine (chiaro, scuro, testo al doppio) accanto a quelle
delle altre tre app; suite completa e lint. Nessuno screenshot del README mostra il primo avvio.
Provato dal committente sul dispositivo (18 ott 2026): tutto a posto.

## Revisione: fusi orari, quarti d'ora, dati che restano sul telefono (committente, 18 ott 2026)

**La richiesta.** Verificare che i dati di Open-Meteo arrivino a schermo sul fuso giusto, quarti
d'ora delle prossime due ore compresi, e che nessuna tabella o dato cresca senza limite.

**Fusi orari: cosa è stato verificato e regge.**

- *L'offset fisso.* Misurato di nuovo sull'endpoint vivo il 18 ott: Milano, `timezone=auto`, il 25 ott
  (il giorno che torna l'ora solare) risponde `utc_offset_seconds: 7200`, `GMT+2`, 24 valori. Il
  `ProviderClock` del mapper legge ogni etichetta con quell'offset e la ri-esprime sull'orologio del
  luogo: ore, `pastHours`, `quarterEndedAt` e i quarti d'ora passano tutti da `instantOf`.
- *I quarti d'ora.* `minutely_15` usa lo stesso offset della serie oraria e la sua prima etichetta è
  quella di `current.time` (16:30 alle 16:33 locali): il quarto 16:15-16:30, già chiuso, che
  `NowcastEngine.read` scarta perché la sua fine non è dopo `now`. La convenzione «somma dei 15 minuti
  precedenti» è confermata dai dati: i quattro quarti 19:15-20:00 sommano lo slot orario delle 20:00
  (0 + 0 + 0,1 + 0,4 = 0,5 mm), e così 20:15-21:00 (1,2 mm). L'età massima (un'ora) è misurata su
  `lastSync`, che una lettura dalla cache disco conserva com'era. Gli orari a schermo (didascalia,
  tacche del grafico, frase dell'eroe) passano tutti per `placeZone`, il fuso del luogo.
- *Il resto.* Avvisi e regole girano sul fuso del luogo (`report.zone()` nel job); il widget lo
  risolve dopo il report (`placeZone`); l'unico orologio del telefono è quello voluto, le ore di
  silenzio delle notifiche.

**Fusi orari: il difetto trovato.** `WeatherRecency.trim` confrontava le *etichette* locali delle ore
con l'ora corrente troncata. La notte che si torna all'ora solare due righe dicono 02:00, e durante la
seconda la prima (un'ora già finita) restava `hourly[0]`, «l'ora in cui siamo»: l'eroe e la striscia
erano un'ora indietro per quell'ora. Ora un'ora è finita quando la sua *fine* (`at` + 1 h) è passata;
identico in ogni altra ora dell'anno, e anche nei fusi a mezz'ora. I giorni restano sul calendario del
luogo. Test: `WeatherRecencyTest`, la notte del 25 ott a Roma.

**Dati: l'inventario.** Ogni cosa scritta su disco e il suo limite.

| Dove | Limite |
|---|---|
| Room `weather_history` | 3 giorni per luogo, mai meno di 2 righe; un commit per finestra di 15 min; tetto globale 2 304 righe; i luoghi non più seguiti se ne vanno dopo 7 giorni di silenzio (`StoredDataSweep`) |
| `report_cache/` | un file per luogo, via a 7 giorni (l'orizzonte); al massimo 8 file di luoghi non seguiti |
| cache in memoria del repository | ridotta ai luoghi seguiti a ogni commit |
| `alerts`, `rule_state`, `sky_alerts`, `warnings` | anelli di impronte (16, 32, 40, 120); i latch delle regole tenuti solo per luoghi e regole vivi |
| `search_history` | 5 voci |
| `cities`, `rules`, `sky`, `settings`, `workspace` | ciò che il lettore ha creato |
| `widget_cities`, `widget_look`, `widget_arc` | una voce per widget, tolta in `onDeleted` |

**Dati: il difetto trovato.** I tre store per widget sono nel backup (`data_extraction_rules.xml`), ma
il ripristino su un telefono nuovo dà ai widget id nuovi e nessun receiver sovrascriveva `onRestored`:
`WidgetCityStore.remap` esisteva, documentato come la risposta al ripristino, e non lo chiamava nessuno.
Così ogni widget ripristinato tornava ai valori di default, le sue chiavi restavano sotto id a cui
nessun `onDeleted` sarebbe mai arrivato, e un widget nuovo che riceveva uno di quei numeri ereditava
il luogo e l'aspetto di un altro. Ora `ChiaroWidgetReceiver.onRestored` sposta luogo, aspetto e
impostazioni dell'arco sui nuovi id e ridisegna; il `remap` delle città sposta anche la riga del cielo,
che lasciava indietro. Lo spostamento è uno solo per store (`WidgetKeyMoves`: prima si legge tutto,
poi si toglie ogni chiave vecchia, poi si scrive), perché vecchi e nuovi id possono sovrapporsi.

**Decisioni e deviazioni.** *Nessuna pulizia per confronto con `AppWidgetManager.getAppWidgetIds`*:
toglierebbe anche gli orfani di un `onDeleted` perso, ma eseguita tra un ripristino e il suo
`onRestored` cancellerebbe proprio le impostazioni da spostare, e il sistema consegna `onDeleted`
anche quando un launcher viene disinstallato. *Lasciati come sono, annotati:* il tetto globale della
storia (2 304 righe) può troncare chi segue più di otto luoghi aggiornati ogni quarto d'ora per tre
giorni, ed è un caso lontano; `NowcastCoverage.looksInterpolated` raggruppa i quarti per ora UTC, che
coincide con l'ora del fornitore in tutti i luoghi coperti (nessun fuso a mezz'ora tra Europa centrale
e Stati Uniti) e andrà ripensato solo se la copertura si allargasse a uno di quei fusi.

**Come è stato verificato.** `WeatherRecencyTest` (la notte del cambio d'ora), `WidgetCityStoreTest`
(spostamento con id sovrapposti, riga del cielo inclusa, niente resta sotto l'id vecchio),
`WidgetLookStoreTest` (gli aspetti seguono i widget). Suite completa.

## La temperatura non viaggia più nella barra (committente, 18 ott 2026)

**La richiesta.** «Se scorri solo poco la temperatura rimane a metà strada e sta male»: togliere il
viaggio della cifra nella barra (DESIGN §8.1e, 15 ott) e farla comparire come l'icona del cielo.

**Perché.** La posa era una funzione continua dello scorrimento senza un riposo fra i due capi, e i
96 dp della corsa cadevano dove ci si ferma a leggere (la frase, massima e minima): lo stato a metà,
la cifra rimpicciolita a cavallo del bordo in due inchiostri, era quello che si vedeva di più. Anche
l'icona della barra entrava con un'alfa proporzionale allo scorrimento, e restava a metà allo stesso
modo.

**Cosa è cambiato.**

- La cifra resta nel cielo e scorre via con lui; non c'è più una copia disegnata sopra la pagina
  (`TravellingNumber`, `heroPose`, `dockSlot`, `DockedNow` tolti).
- Una sola soglia, `heroUnderBar`: la metà della cifra passa sopra il bordo inferiore della barra.
  Il canvas misura una volta dove la cifra riposa (`HeroFigure`), il resto è aritmetica sullo
  scorrimento. Prima della misura vale la soglia di prima (`HeroReach`, 120 dp).
- Superata la soglia, icona e numero della barra (`CompactNow`) entrano insieme con la dissolvenza e
  il mezzo scorrimento della molla `effects`/`spatial`, ed escono allo stesso modo: due stati, a tempo,
  mai una via di mezzo. Lo stesso con il movimento ridotto (dove sono la dissolvenza di 100 ms di §7).
- Un solo annuncio per TalkBack nella barra, la temperatura (`clearAndSetSemantics` su `CompactNow`).

**Decisioni e deviazioni.** *La soglia a metà cifra*, non al suo primo pixel sotto la barra (il
numero piccolo comparirebbe mentre il grande è quasi tutto visibile, due copie a schermo) né
all'ultimo (un tratto senza temperatura da nessuna parte). *Nessun aggancio dello scorrimento* (far
finire la pagina a uno dei due capi): avrebbe tolto lo stato a metà spostando la pagina sotto il dito
del lettore, che è peggio. *Persa la continuità del movimento*: la tiene il momento e il posto, il
numero piccolo entra mentre il grande passa sotto, accanto all'icona dove l'occhio già lo cerca.

**Come è stato verificato.** `HeroInBarTest` (nuovo, al posto di `HeroTravelTest`): a riposo la cifra
è nel cielo, passa sotto la barra quando la sua metà ne supera il bordo e non un pixel prima, torna nel
cielo trascinando indietro, col canvas uscito è sotto la barra; il tetto del 130% del testo. Il test di
TalkBack in `TodayAgendaTest`. Una sequenza di scorrimento (60, 100, 130, 150, 170, 200, 300 dp)
disegnata sul Milano e guardata fotogramma per fotogramma: a 100 dp la barra è vuota, da 130 icona e
numero interi. Le immagini `today-day`, `today-week`, `today-details` rigenerate e guardate (il numero
della barra ora è il `titleLarge` vero, non la cifra da 88 sp rimpicciolita); `today-now` identica al
pixel. Suite completa e lint.

## Il servizio che non risponde: la review delle chiamate a Open-Meteo (committente, 19 ott 2026)

**La richiesta.** «Ultimamente l'app ha dei momenti in cui non riesce a scaricare i dati meteo, dà
un errore tipo che il server non risponde»: una review completa delle chiamate a Open-Meteo
(header, body, uso nell'app), la verifica dal vivo della raggiungibilità, i report storici
dell'ultima settimana; poi l'attuazione dei punti 1-6 della review, con due vincoli: nessun effetto
sulla batteria (altrimenti fermarsi) e nessuna riga di log in più.

**Cosa ha detto la misura.** Le chiamate sono corrette: GET senza body, liste con le virgole
letterali (il server accetta anche `%2C`), `timezone=auto`, i 24 quarti d'ora serviti dal quarto
corrente, User-Agent con versione e repo pubblico, gzip onorato (21 KB → 5 KB), HTTP/1.1 e TLS 1.3
soli in ALPN, ogni tipo del JSON uguale a quello dei DTO. Il sintomo è del server. Dal 17 ottobre il
nodo europeo dell'API gratuita (94.130.142.35, a cui `api.open-meteo.com` risolve dall'Italia; solo
un record A, nessun AAAA, mentre aria e geocoding ce l'hanno) è sovraccarico allo scoccare di ogni
ora piena, e i maintainer lo confermano nelle issue #2220 e #2219 del repo open-meteo («we only drop
API requests during peak loads at every full hour», «we will add new servers»). Misurato il 19 ott
con l'URL e lo User-Agent esatti dell'app, 72 campioni a cavallo delle 09:00: fuori picco
0,14-0,18 s con `generationtime_ms` 4-8; dalle 09:00:04 per tre minuti 8,6-11,7 s al primo byte con
`generationtime_ms` 5 700-8 200, tutti 200, l'host dell'aria invariato; normale dalle 09:04:45. La
voce del 17 ott qui sopra (una stretta di mano su due, generazione a 4,2 s) è lo stesso evento
visto dal client. Dal sorgente del server (`RateLimiter.swift`, `ConcurrencyGroupLimiter.swift`,
`ForecastApiResult.swift`): concorrenza per indirizzo IPv4 una richiesta in corso più cinque in coda,
la sesta 429 `Too many concurrent requests`; oltre 4 096 richieste in corso su un'istanza 503
`The service is overloaded`; nessun `Retry-After` in tutto il codice; il peso di una chiamata è
max(1, variabili/10 × max(1, giorni/14)), quindi la forecast di Chiaro (52 variabili) pesa 5,2
chiamate e l'aria 1,4 sui limiti per IP (600/min, 5 000/h, 10 000/giorno). La status page
(StatusCake) non ha test configurati e non mostra nulla: le issue sono l'unica cronologia.

**Cosa è cambiato.**

1. *Il tempo che il servizio ha per pensare è 20 s, non 10.* `readTimeout` esplicito a 20 s in
   `ServiceLocator`, `connectTimeout` dichiarato a 10, `callTimeout` 30 = la somma dei due. Il server
   non manda un byte finché non ha calcolato, quindi il timeout di lettura è il tempo che gli si dà:
   col default di 10 s metà delle risposte del picco moriva in timeout, e il retry a 2 s cadeva nello
   stesso picco. Venti è oltre il peggior primo byte misurato (11,7 s).
2. *Un timeout di qualsiasi tipo non si ritenta più* (`WeatherRepository.isTimeout`: ogni
   `InterruptedIOException`, cioè connect, read e call). È tempo già speso: con 20 s di lettura un
   secondo tentativo sarebbe una pagina da 42 s. Il caso peggiore di un servizio muto passa da
   10 + 2 + 10 s di radio a 20. Una connessione caduta (rifiutata, reset, fine del flusso, stretta di
   mano fallita) e un 5xx, che costano zero, tengono il loro secondo tentativo.
3. *Il retry aspetta da 2 a 6 s*: 2 s più una quota casuale di 4 (`TRANSIENT_RETRY_JITTER`,
   `nextRetryDelay`), così i telefoni che incontrano lo stesso 503 allo stesso secondo non tornano
   tutti insieme sullo stesso nodo. Il pavimento resta i 2 s di un nodo che riparte.
4. *Il 429 della coda si ritenta, i 429 dei limiti no.* Il body dell'errore viene letto una volta
   (`ApiErrorDto`, `HttpException.reason()`): `Too many concurrent requests` ha un secondo tentativo
   con lo stesso ritardo, perché la coda si libera in secondi; minuto, ora e giorno restano senza,
   come prima. La frase del server viaggia in `WeatherException.ApiError.reason` e nel suo
   messaggio; leggere il body chiude anche la connessione che prima restava appesa al garbage
   collector.
5. *Il job periodico ritenta su 503 e 429* (`ApiError.isTransient`: 5xx e 429) con
   `Result.retry()`, come già per offline e `Unreachable` dal 17 ott: prima un 503 al picco aspettava
   il periodo intero, fino a due ore di avvisi e widget sui numeri vecchi. Un altro 4xx aspetta
   ancora: fallirebbe uguale.
6. *Il tick di Oggi aspetta dopo un fallimento* (`RefreshBackoff`): un minuto, due, quattro, otto,
   poi il quarto d'ora del provider; un successo azzera; il trascinamento, l'atterraggio e il cambio
   di luogo non aspettano mai. Prima un guasto lungo erano quattro richieste al minuto per pagina
   aperta, a un servizio già in sofferenza.
7. *Un null del modello costa l'ora, l'aggregato o il blocco, mai il report.* Le ultime colonne non
   nullable dei DTO lo sono diventate: temperatura, codice, `is_day` e nuvole orarie; codice,
   massima e minima giornaliere; tutto il blocco `current`. Nel mapper: un'ora senza temperatura,
   senza stato (né codice né nuvole) o senza nuvole non è una riga, e le vicine tengono il posto
   (una riga è il suo istante, non la sua posizione); un'ora senza codice legge il cielo dalle sue
   nuvole, e un codice mancante sull'intervallo dice «non cade niente»; un giorno senza massima o
   minima le prende dalle sue ore, come già prende l'etichetta, e senza ore né etichetta non è una
   riga; un blocco `current` con un buco (temperatura, percepita, umidità, rugiada, pressione,
   vento, o niente da cui leggere uno stato) diventa la previsione del suo stesso istante dalle
   righe, con `estimated = true` e la dicitura che le superfici già hanno
   (`CurrentEstimate.estimate` sulle sole righe); l'ammontare del quarto d'ora, che le righe non
   hanno, resta del blocco ed è nullable (`Precipitation.lastQuarterHourMm`; il suo unico lettore è
   la tinta della pioggia dello shader, che senza disegna niente che cade). Se nemmeno le righe
   possono sostituire il blocco (una voce di cache di prima che portassero il resto dell'ora), il
   report non si costruisce: il fallimento di prima, l'unico rimasto.

**Decisioni e deviazioni.** *Batteria*: nessun punto aggiunge radio. Il caso peggiore (punti 1 e 2)
scende da 22 a 20 s per un servizio muto, e il caso misurato (risposta a 11 s) diventa un successo a
11 s invece di 22 s buttati più il banner. Il punto 5 aggiunge al più un tentativo per periodo
fallito, col backoff esponenziale di WorkManager (15 min) già in uso dal 17 ott. Il 6 toglie
richieste. *Il retry sui timeout tolto* è la conseguenza del punto 1, non un'aggiunta: la regola del
17 ott («sono già 30 s spesi») vale per ogni orologio. Il modo del 17 ott (stretta di mano ferma una
volta su due) guarisce ora al tick successivo invece che dopo 2 s: accettato, perché il modo
misurato oggi e descritto dai maintainer è il server lento, non quello muto. *Il punto 7 della
review (il `reason` a log) non è attuato*: l'app non scrive log propri, l'unico è
l'`HttpLoggingInterceptor` BASIC dei build di debug, due righe per chiamata della libreria a cui non
si aggiunge un campo senza riscriverle, e il vincolo era «nessuna riga nuova». La frase del server
c'è comunque in `ApiError.reason` e nel messaggio dell'eccezione. *Le ore mancanti e la nebbia*:
l'engine vede le ore presenti come una serie sola, quindi i vicini di un'ora mancante si giudicano a
vicenda per la regola della nebbia; una risposta con buchi è già un giorno che il modello non ha
intero, e questa è l'imprecisione minore. *La massima dalle ore* è aritmetica sui numeri del modello
(è quello che Open-Meteo stesso serve per la maggior parte dei modelli), non una stima: senza flag.
*Nessun `Retry-After` da onorare*: il server non lo manda. *Nessun allineamento all'ora piena da
evitare*: il job conta dall'enqueue, il tick dall'apertura della pagina; Chiaro subisce il picco,
non lo alimenta. *Il peso 5,2 della chiamata* resta: ogni variabile ha un lettore documentato.

**Come è stato verificato.** `TransientRetryTest` (riscritto: i timeout di ogni tipo e i 429 dei
limiti senza retry; il 429 della coda e le connessioni cadute con uno; il `reason` sull'errore e
`isTransient`; il ritardo in [2 s, 6 s] e non uguale per tutti). `WeatherSyncWorkerTest` (503 e 429
→ `retry()` con il repaint dei widget, 400 → `success()`). `RefreshBackoffTest` (nuovo: la scala
1-2-4-8-15, il tick che chiede solo ad attesa finita, il successo che azzera, il tetto).
`WeatherReportMapperTest` (cinque test nuovi: l'ora senza temperatura e l'ora chiusa senza, l'ora
senza codice e quella senza nuvole con le vicine al loro posto, le estreme dalle ore, il giorno
senza nulla da leggere, il blocco `current` bucato come stima del suo istante, intero mai stima,
senza righe capaci il report non si costruisce). `OpenMeteoResponseTest` (la risposta reale di
Milano con `current.temperature_2m` e `temperature_2m_max` a null mappa: stima e massima dalle ore).
Suite completa: 353 domain, 275 data, 24 sync, 733 app (14 saltati, gli screenshot del README),
lint 0 errori; build release minificata con la chiave di debug. Nessuna immagine da rigenerare:
niente a schermo è cambiato. **Da fare sul dispositivo**: un trascinamento allo scoccare di un'ora
piena (09:00-09:03) deve restituire il report invece di «il servizio non risponde»; una pagina
lasciata aperta durante un guasto deve guarire da sola entro il quarto d'ora.

## Il cielo premium: la review del motore e i sette passi (committente, 19 ott 2026)

**La richiesta.** Una review del motore grafico che disegna il cielo dell'hero: se è corretto dal
punto di vista matematico, se ha errori, se l'immagine è realistica, cosa farebbe la differenza per
un'app di qualità premium, e se vada rifatto con un'altra logica o libreria. Poi: una fase con tutto
dettagliato, e l'attuazione a partire dai punti 1 e 2 (o solo l'1, se il 2 è complesso).

**Come è stata fatta.** Letti `SkyShader.kt`, `SkyCanvas.kt`, `SkyBodies.kt`, `SkyPalette.kt`, i
test e DESIGN §3. Lo shader e la scena portati in NumPy, stesse funzioni e stessi numeri, e dodici
cieli disegnati a 400 × 352 dp (cumuli al 25 e al 50%, coperto basso, velo alto chiuso e al 60%,
ora d'oro, ora blu, luna piena, stelle, pioggia, neve, sole a est), ognuno nudo e come lo vede il
lettore con scrim e corpi celesti; `today-now.png` del README, resa dal renderer hardware, conferma
che il telefono disegna lo stesso cielo del port. Il tempo di GPU per fotogramma resta da misurare
sul dispositivo, come il passo del 15 ott già annota.

**Il verdetto.** Il motore è matematicamente corretto e **non va rifatto**: uno shader AGSL, la
scena in Kotlin puro, i test che fissano il rumore riga per riga, il tetto di luminanza sotto lo
scrim sono l'architettura giusta per Android 13 e oltre, e non c'è una libreria che faccia meglio
(Filament o Sceneform sono motori 3D da megabyte e thread propri; Lottie o Rive darebbero un cielo
disegnato a mano, non calcolato, contro §3.1). Quello che tiene la resa sotto il livello premium
sta nel modello di illuminazione dentro lo shader e nel sole disegnato fuori da esso, e si corregge
a passi dentro la struttura attuale. Corretto tutto ciò che il codice dichiara: l'hash di Hoskins e
il value noise, la normalizzazione dell'fbm (0,9375 = la somma delle ampiezze), la rotazione fra le
ottave ortonormale (36,9°), la sovrapposizione casuale degli strati, il tetto in luminanza lineare,
l'ellisse del terminatore lunare (semiasse r·|2k−1|, rigonfia dal lato giusto), lo specchio
dell'emisfero sud, la deriva nel verso del vento, lo stato dell'ora dal motore e non dal codice del
fornitore.

**Gli errori trovati** (misurati sul port, canvas di 400 × 352 dp):

1. *La luce dello shader non è dove sta il sole disegnato.* Lo shader prende la x su tutta la
   larghezza e la y fra il 12% e il 50% del canvas; `SkyBodiesLayer` disegna nella propria fascia
   (80–152 dp) con un inset dell'8%. Distanza fra i due: 19 dp a 35° di altezza, 33 a 10°, 42 a
   mezzogiorno. Il bordo luminoso delle nuvole è centrato dove il sole non c'è. → passo 2.
2. *Sole e luna stanno davanti alle nuvole e sopra lo scrim.* Il layer dei corpi è nel Box del
   contenuto di `SkyCanvas`, dopo lo scrim, e conosce solo l'opacità globale, non l'alfa della
   nuvola nel suo pixel: al 25% il disco pieno sta sopra un cumulo. Il commento di `glowRadius`
   dice che lo scrim assorbe la coda dell'alone: non può, lo scrim è sotto. → passo 2.
3. *Il cielo piovoso è al contrario.* Il colore `lit` non era attenuato dalla pioggia mentre il
   gradiente sì (§3.3): al 94% di banco e 90% di pioggia nuvole grigio chiaro (luminanza 0,45)
   sopra varchi blu scuro (0,09–0,30). Un nembostrato è più scuro del cielo attorno. → passo 1.
4. *Nessun dithering.* Il gradiente notturno va da #0D1323 a #182237: 17 livelli di rosso su una
   colonna di 1 056 px, bande larghe fino a 110 px. Su un OLED si vede a scalini. → passo 1.

**Le inesattezze fisiche.** I cirri derivavano 1,8 volte più veloci dei cumuli: essendo lontani
devono muoversi più lenti sullo schermo (parallasse rovesciata) → passo 1. Il clock avvolge ogni
3 600 s e le nuvole saltano di 3 600 × deriva dp, da 21 600 in su, in un fotogramma → accettato,
vedi le decisioni. La scelta della luce è binaria (sole se `day` > 0,5, altrimenti luna) e con il
sole sotto l'orizzonte la y della luce resta a metà canvas: al tramonto le nuvole non sono mai
illuminate da sotto, che è l'immagine stessa del tramonto → passo 3. La fusione è in spazio gamma:
al bordo di una nuvola a metà mix la luminanza è 0,537 contro 0,558 in lineare, l'alone grigio
delle nuvole composte male → passo 1.

**La resa.** Reggono: il coperto (un grigio con pieghe morbide), la notte con la luna, il cielo
stellato, i cumuli al 25%. Non reggono per un prodotto premium: il sole è un adesivo (disco piatto
di 22 dp con alone in SRC_OVER, all'ora d'oro un dischetto arancione senza il bagliore di un sole a
3°); le nuvole sono una parete e non un soffitto (la scala orizzontale del rumore è la stessa in
cima e in fondo, `persp` schiaccia solo in verticale); l'ombreggiatura pettina le nuvole (una sola
differenza finita lungo la luce, offset 0,16 e guadagno 4: striature allungate in quella direzione,
visibili al 50% con il sole a est; nessuna auto-ombra); neve e pioggia sono un pattern (un fiocco per
cella di 22 dp, mai due vicini, un piano solo; la pioggia graffi sottili a 68 dp/s); le stelle sono
tutte uguali e non sono quelle del cielo; lo scrim affumica la metà bassa (dal 45% al 93% del canvas
il nero al 55% rende l'orizzonte, la parte più luminosa di un cielo vero, la più scura dello
schermo: è il contratto di §3.6 e non un bug, ma è la cosa che più fa sembrare il cielo dietro un
vetro).

### I sette passi, in ordine di resa per costo

- [x] **1. Luce lineare e dither.** Gli uniform di colore arrivano allo shader in RGB lineare
  (convertiti in `bindSky` con la curva sRGB esatta), ogni `mix` avviene lì, il tetto è un prodotto
  scalare esatto, e il pixel torna in sRGB alla fine con la curva esatta più mezzo livello di dither
  fisso per pixel. Con lo stesso passo le tre correzioni piccole che sono solo colore: le nuvole più
  scure sotto una pioggia probabile (errore 3), il velo che deriva a metà del banco (parallasse). Un
  test di pixel (`SkyShaderPixelTest`) disegna lo shader con Skia e lo confronta con l'aritmetica
  in Kotlin.
- [x] **2. Sole e luna dentro lo shader.** Un solo sistema di coordinate (via l'errore 1),
  occlusione per pixel dall'alfa della nuvola (via l'errore 2), bagliore additivo con un lobo in
  avanti alla Henyey-Greenstein, il disco piccolo e bruciato, l'alone di 22° sotto un velo alto
  chiuso (il dato lo dice: strato alto, cirrostrato). La fase della luna resta un disegno in Kotlin
  (la geometria del terminatore è già giusta) o passa nello shader come maschera; da decidere in
  attuazione. `SkyBodiesLayer` e la sua fascia (`SkyColumn`) restano solo se serve ancora un layer
  Compose; `sunVeil` e `moonVeil` restano le regole, i loro test con loro. Immagini del README da
  rigenerare.
- [x] **3. La luce del banco.** Due campioni lungo la direzione della luce (ε e 2ε) per
  un'auto-ombra che dia spessore; gradient noise al posto del value noise nella derivata, così la
  striatura lungo la luce sparisce; il contrasto dell'ombreggiatura scalato con la luce disponibile
  (senza sole né luna le nuvole sono piatte); la y della luce libera di stare sotto il canvas al
  crepuscolo, così il sottopancia delle nuvole prende l'arancio del tramonto; la scelta sole/luna
  pesata sulla luce di ciascuno e non binaria. Qui anche il residuo dell'errore 3: lo stop alto del
  gradiente sotto nuvole e pioggia (§3.3) scende a 0,09 di luminanza, più scuro dell'ombra di
  qualunque nuvola, e un varco in cima resta un buco scuro; va rivisto nel modello del gradiente,
  che legge anche il nastro.
- [x] **4. La proiezione a piano.** Il banco come un piano a quota h visto da sotto: u = x·k/(y_h −
  y), v = k/(y_h − y) con l'orizzonte y_h sotto il bordo inferiore, al posto dello schiacciamento
  verticale di `persp`. Le nuvole rimpiccioliscono e si appiattiscono verso l'orizzonte insieme. La
  tabella di `deckThreshold` va rimisurata sul nuovo campionamento (`SkyDeckTest`).
- [x] **5. Pioggia e neve a tre piani.** Tre strati con scala, velocità e dimensione diverse (vicino
  grande e veloce, lontano piccolo e lento), ciascuno a celle con hash come ora ma più celle per
  piano e un'occupazione casuale, così la neve non è un reticolo e la pioggia ha profondità.
- [x] **6. Le stelle vere.** `AstronomyEngine` ha già l'effemeride, la fascia del canvas è già una
  proiezione azimut-altezza (`bodyAcross`, `bodyUp`), e un catalogo delle stelle sopra la terza
  magnitudine pesa pochi kilobyte: le stelle disegnate sono quelle che ci sono, con la magnitudine
  nella grandezza. Con lo stesso passo **la luna ruotata come la si vede dal luogo** (committente,
  19 ott 2026: «si può fare? lo consigli?»; sì, e consigliata): l'angolo del lembo illuminato
  rispetto alla verticale locale si calcola dalle posizioni di sole e luna che il motore ha già (la
  direzione dalla luna verso il sole proiettata sul piano tangente del cielo in quel luogo e in
  quell'ora), e nello shader è una rotazione della maschera della fase, costo nullo; ingloba
  `litOnRight`, emisfero sud compreso. Non è rumore: non aggiunge un elemento, orienta uno che c'è,
  e la falce «a barchetta» della sera di primavera o la luna «sorridente» ai tropici sono tra le
  cose della luna che un lettore nota e può verificare alzando gli occhi. Due limiti da scrivere:
  sul canvas la resa è approssimata, perché la fascia è una proiezione azimut-altezza compressa e
  non conforme (l'angolo vale nel cielo, sullo schermo «quasi»); e a 11 dp si legge sulle falci e
  sulle gibbose, non alla luna piena. Da fare sul canvas e sulla luna della scheda «Stanotte»
  (`moonLitPath`, disegnata «come la vedi»), lasciando dritte le icone della famiglia, che sono
  simboli. **Seconda parte** (committente, 20 ott 2026, dal telefono: «due o tre stelle»): la
  camera delle stelle è sua e più larga (65°–10°, ±29°), il catalogo arriva alla sesta magnitudine
  come polvere, e il README, la guida e DESIGN dicono che la luna sta sulla sua fascia (la striscia
  fra il nome del luogo e il numero grande) e
  le stelle nella finestra a sud, quindi la loro distanza sullo schermo non è quella del cielo.
  Stelle casuali rifiutate. Vedi «Passo 6, seconda parte» in fondo.
- [x] **7. Lo scrim**, deciso sulla misura (vedi sotto); era da decidere con il committente: misurare il contratto di §3.6 sul rettangolo
  del testo e non su tutta la larghezza (una scrim che sfuma anche in orizzontale), oppure un'ombra
  morbida sul testo, così la metà bassa del cielo torna chiara. Nessuna delle due è gratis: la prima
  cambia `ScrimContractTest`, la seconda il modo in cui il contratto si misura.

### Passo 1: luce lineare e dither (19 ott 2026)

**Cosa è cambiato.**

1. *Lo shader compone in luce lineare* (`SkyShaderSource`, `bindSky`). I tre stop del gradiente e
   i due colori delle nuvole arrivano come RGB lineare (`setLinearColor`, con `toLinear`, la curva
   sRGB esatta); ogni `mix` dello shader somma luce; il bianco delle stelle è scritto in lineare;
   il tetto di §3.6 è `dot(col, (0,2126; 0,7152; 0,0722))` confrontato con `uMaxLum`, esatto, al
   posto della potenza 2,2 sui valori sRGB; alla fine `encode` riporta il pixel in sRGB con la curva
   a tratti esatta (`12,92·c` sotto 0,0031308, altrimenti `1,055·c^(1/2,4) − 0,055`). Le tre
   fermate stampano gli stessi numeri di prima; cambia solo quello che sta fra loro, più chiaro
   della differenza fra i due mix.
2. *Il dither*: mezzo livello di rumore (`(hash12(fc) − 0,5)/255`) sommato al pixel codificato,
   un pattern fisso per pixel, così non luccica fra un fotogramma e l'altro. Sulla colonna della
   notte serena la corsa più lunga di un valore scende da 110 a 41 px e ai punti di mezzo fra due
   livelli lo shader stampa l'uno e l'altro a metà.
3. *Le nuvole sotto una pioggia probabile sono più scure* (`skyScene`, `WetLitDim` = 0,25): il
   colore `lit` scende di un quarto della probabilità in valore sRGB, sotto il 30% che l'ombra già
   aveva. Sulla scena della review (95% di nuvole, 90% di pioggia, sole a 40°): `lit` da 0,695 a
   0,479 di luminanza, ombra 0,13; lo stop medio del gradiente (0,174) e quello basso (0,30) stanno
   fra i due, un varco non è più né un buco né una luce. Lo stop alto (0,092) resta sotto l'ombra:
   è il modello di §3.3, rimandato al passo 3.
4. *Il velo deriva a metà del banco* (era 1,8 volte): un cirro è cinque-otto volte più alto di un
   cumulo e il suo vento due-tre volte più forte, quindi attraversa il cielo più lento.
5. DESIGN §3.5 (la luce lineare, il dither), §3.9 (il velo, le nuvole bagnate, il tetto esatto).

**Decisioni e deviazioni.** *Anche il gradiente in lineare*, non solo le nuvole: una pipeline
sola; i tre stop restano quelli di §3.2 e il contratto di §3.6 è misurato sullo stop più chiaro,
che non si muove; fra due stop un mix lineare ha luminanza fra le due (la luminanza è lineare nei
canali lineari), quindi mai sopra il tetto. Il fallback software (`Brush.verticalGradient`, dove un
`RuntimeShader` non si disegna) resta in sRGB: è uno snapshot, non lo schermo. *La curva esatta e
non la potenza 2,2*: allo stop più scuro della notte (13 su 255) la potenza stamperebbe 21, e la
notte impallidirebbe. *Il dither sta dentro il contratto*: mezzo livello sopra il tetto, contro un
margine misurato di 5,27:1 su 4,5. *La precisione `half`* basta: a 0,004 di lineare (la notte) l'ulp
di fp16 è 4·10⁻⁶, dopo la codifica 5·10⁻⁵, un ventesimo di livello. *Il salto orario del clock è
accettato*: l'offset delle nuvole deve restare sotto circa 10⁵ dp perché alla quarta ottava
(coordinate × 0,0105 × 8,4) fp32 tenga la millesima parte di cella, il rumore non è periodico (le
ottave sono ruotate), e una dissolvenza al cambio raddoppierebbe il banco per due secondi ogni ora;
il salto si vede solo tenendo Oggi aperta e ferma per un'ora intera. *L'errore 1 (la luce non dove
sta il sole) non è toccato qui*: si risolve da solo portando i corpi nello shader (passo 2), un
raccordo provvisorio delle coordinate andrebbe rifatto subito dopo. *Sull'ora d'oro* il mix lineare
tinge il sottopancia delle nuvole di malva-rosa dove prima era grigio-azzurro: è l'azzurro del
cielo alto sommato all'arancio dell'orizzonte come luce, ed è il rosa delle nuvole al tramonto;
tenuto, da guardare sul telefono.

**Da verificare sul telefono.** La grana del dither su OLED (deve essere ferma, mai un luccichio);
la resa in luce lineare di giorno (nuvole più bianche ai bordi) e all'ora d'oro (il malva); il
tempo di GPU per fotogramma, con tre `pow` in più per pixel nella codifica.

**Come è stato verificato.** `SkyShaderPixelTest` (nuovo, `testDebug` con la grafica nativa, lo
shader disegnato da Skia attraverso un `RenderNode` e un `HardwareRenderer` come le immagini del
README, perché un canvas software rifiuta un `RuntimeShader`): i tre stop stampano i numeri della
palette entro un livello; fra gli stop ogni pixel di un giorno e di una notte è il mix lineare
codificato con la curva esatta entro un livello; nelle righe in cui il valore ideale cade a metà
fra due livelli lo shader stampa l'uno e l'altro, fra il 30 e il 70%; nessun pixel del giorno più
luminoso supera lo stop del contratto. `SkySceneTest` (nuovo caso): sotto una pioggia probabile il
colore chiaro scende sotto i tre quarti di quello asciutto, lo stop medio e quello basso del
gradiente stanno fra ombra e luce, un'ora asciutta è com'era. `SkyDeckTest` fissa ancora le righe
del banco, che non sono cambiate. Il port NumPy reso prima e dopo su quattro cieli (cumuli al 25%,
ora d'oro, pioggia, sole a est) e guardato. Immagini del README rigenerate: `today-now`,
`today-foldable` (la Milano coperta, i bordi delle nuvole più puliti) e `settings` (l'ora d'oro con
il malva), guardate; `widgets` differiva per 322 px di antialiasing e nessun motivo, ripristinata.
Suite completa (738 app debug con i 14 screenshot saltati, 694 release, i moduli core invariati) e
lint con 0 errori. Il salto trovato scrivendo il test di pixel: dentro `RenderNode.apply { }` i
`width` e `height` del nodo, ancora a zero, nascondono quelli della classe e posizionano un nodo
senza dimensione, che disegna il nulla; il test li passa come parametri e lo dice.

### Passo 2: sole e luna dentro lo shader (19 ott 2026)

**Cosa è cambiato.**

1. *I corpi sono della scena* (`SunMark`, `MoonMark` in `SkyScene`, da `skyScene`, che ora legge anche
   l'elongazione della luna): dove stanno (`bodyAcross`, `bodyUp`, le regole di `SkyBodies.kt`, che
   resta il file delle regole e perde il layer), il colore del sole e il suo calore, la fase e il
   lembo illuminato della luna, e il loro velo. Il velo globale di un disco è l'opacità dello strato
   alto soltanto (tutta la copertura in un'ora bagnata), perché il banco lo nasconde **pixel per
   pixel** nello shader; i bagliori davanti alle nuvole leggono l'opacità intera, come le regole
   `sunVeil` e `moonVeil` erano state scritte. La luce delle nuvole è il posto del corpo, con la
   stessa aritmetica: via l'errore 1 (19–42 dp di distanza) per costruzione.
2. *La fascia è un dato* (`SkyBand`, dp dal bordo alto del canvas e altezza): `SkyColumn` tiene lo
   spazio fra la riga del luogo e l'hero come prima, e il suo unico figlio è uno `Spacer` che
   riferisce il proprio rettangolo nelle coordinate del canvas; l'anteprima delle Impostazioni
   dichiara la sua (8 dp dal bordo, metà del ritaglio). `SkyCanvas` e `SkyShaderLayer` la passano
   allo shader (`uBand`); senza, lo shader usa la metà alta del canvas, dove stava la luce prima.
3. *Lo shader disegna i corpi dietro le nuvole, in luce lineare*: prima la forma del banco (il suo
   colore viene dopo), poi il sole come luce aggiunta (disco a tre volte il tetto, che brucia a
   bianco; corona e coda dell'aureola), la luna come colore sopra il cielo (riflette, non brilla:
   la fase è la geometria di `moonLitPath` come maschera, il buio al 14% di terra-luce), tutti
   moltiplicati per `1 − alfa del banco`; poi il velo, il colore del banco, e davanti alle nuvole
   la quota della coda che l'aria fra noi e le nuvole diffonde, la chiazza larga e tenue che una
   nuvola lascia passare (tutto quello che uno strato chiuso mostra del sole) e il bagliore della
   luna dove un banco ne nasconde il disco. Un disco visto attraverso una nuvola sottile ha il bordo
   sfocato prima di sparire.
4. *Il tetto si apre nella fascia*: il tetto di §3.6 resta lo stop più chiaro della palette ovunque
   possa stare un testo, e la fascia non ne contiene per costruzione; dentro, a 6 dp dai bordi, sale
   a bianco. Oltre il tetto il colore viene tenuto al tetto; molto oltre (un disco a tre volte)
   brucia a un bianco della luminanza del tetto, come in un obiettivo, invece di restare un giallo
   schiacciato.
5. *Il bagliore di un corpo è un cerchio* che sfuma dal centro fino a zero 8 dp oltre il bordo più
   vicino della fascia (oltre il bordo è al più un quinto di sé, sotto il tetto), quello che
   `glowRadius` faceva per il vecchio layer; mai una finestra ritagliata dalla fascia.
6. `SkyBodiesLayer`, `SkyBodies` e il disegno in Compose tolti; `moonLitPath` resta (la scheda di
   stanotte lo disegna). DESIGN §8.1c e §3.9.

**Decisioni e deviazioni.** *La striscia*: la prima resa vera dello shader (Skia, `CHIARO_RENDER_DIR`)
aveva una striscia luminosa attraverso tutto il canvas all'altezza del sole, e sotto il velo alto il
blob tagliato in piano sul bordo della fascia: il bagliore era limitato alla fascia in altezza ma non
in larghezza, e la coda dell'aureola, larga e tenue, si vedeva come una banda. Ora è un cerchio attorno
al corpo che si spegne al bordo della fascia, e il test di pixel legge il cielo accanto al sole alla
sua stessa altezza. *L'alone di 22° provato e tolto*: a quattro dischi dal sole non sta in una fascia
di 72 dp, e stretto per starci era una seconda corona; torna se la fascia cresce (passo 7, lo scrim).
*Il disco dietro un banco è del pixel e non della regola*: `sunVeil(1).disc = 0` resta vera per lo
strato chiuso perché il velo globale di un'ora bagnata è tutta la copertura, e per uno strato asciutto
la copre l'alfa del banco, che a banco chiuso è 1 su ogni pixel (`SkyDeckTest`). *La luna di giorno
dietro uno strato*: 55% × (1 − 0) di disco, nascosto per intero dal banco; la regola di `moonVeil` per
lo strato (disco 0) si legge ora attraverso l'alfa. *Il tetto aperto non tocca il contratto*: il
test di pixel legge ogni riga sopra la fascia di un mezzogiorno sereno con il sole in cima alla
fascia e la trova sotto lo stop del contratto. *Il bagliore è grande quanto la fascia concede*: un sole in cima
alla fascia ha 19 dp di spazio sotto la riga del luogo e il suo bagliore è poco più del disco, uno a
metà ne ha 36; con una sfumatura a plateau (piena fino a metà spazio) la chiazza sotto uno strato
chiuso prendeva la forma di un disco, per cui sfuma dal centro. È il prezzo di una fascia da 72 dp,
non una scelta. *Un salto trovato scrivendo il test*: `RenderNode.apply
{ setPosition(0, 0, width, height) }` leggeva `width` e `height` del nodo, zero, e disegnava il nulla;
il test li passa come parametri.

**Da verificare sul telefono.** Il sole di mezzogiorno (disco bianco, corona, nessuna striscia), un
sole dietro un cumulo (il disco sfocato che spunta dal bordo), la luna con la fase e il suo bagliore,
il tempo di GPU (ogni termine è saltato se il corpo è sotto l'orizzonte; dentro la fascia una decina di
operazioni in più per pixel).

**Come è stato verificato.** `SkyShaderPixelTest` (quattro casi nuovi, con il renderer hardware di
Robolectric): il centro del sole di un mezzogiorno sereno è bianco (ogni canale ≥ 0,96), a sei dischi
di distanza il cielo è ancora un cielo, ogni riga sopra la fascia sta sotto lo stop del contratto, e
il cielo accanto al sole alla sua altezza è quello del gradiente (niente striscia); sotto uno strato
chiuso il centro non è bianco ed è appena più chiaro del banco attorno; la falce è illuminata dal lato
del sole, tre volte più della terra-luce, e più chiara di qualunque cielo; più cinque cieli disegnati
solo per guardarli quando `CHIARO_RENDER_DIR` è impostata (ora d'oro, velo chiuso, sole dietro un
cumulo, luna piena, luna di giorno), guardati tre volte: la prima con la striscia, la seconda con la
chiazza a forma di disco, la terza buona. `SkySceneTest` (tre casi nuovi): il sole dove lo mettono le
regole e solo sopra l'orizzonte, la luce delle nuvole uguale al suo posto, il calore all'ora d'oro; il
disco lasciato allo shader sotto un banco, attenuato sotto un velo, tolto da un'ora bagnata; la luna
con la fase, il lembo, il velo di giorno e di notte. `SkyBodiesTest` riscritto sulle regole senza il
layer. `SkyDeckTest` fissa ancora le righe del banco. Immagini del README rigenerate e guardate:
`today-now` e `today-foldable` (la Milano coperta mostra del sole un chiarore tenue, non più un disco
sopra le nuvole) e `settings` (il sole dell'ora d'oro con il suo bagliore). Suite completa (745 app
debug con i 14 screenshot saltati, 697 release, i moduli core invariati) e lint con 0 errori.

### Passo 3: la luce del banco (19 ott 2026)

**Cosa è cambiato.**

1. *Il bordo illuminato si legge sulle prime tre ottave*, a 0,22 di cella verso la luce e con
   guadagno 3 (era su tutte e quattro a 0,16 con guadagno 4): la quarta ottava, celle di 11 dp,
   metteva nella derivata il suo reticolo e pettinava le nuvole lungo la luce, la rigatura vista al
   50% con il sole a est. Le somme a due e tre ottave escono dallo stesso `fbm` del banco
   (`fbmParts`, con `out`), quindi il bordo costa i tre campioni del punto spostato e basta.
2. *L'auto-ombra*: una nuvola più spessa verso la luce di quanto sia qui, letta sulle due ottave
   grosse al doppio della distanza (`1 − exp(−7·eccesso)`, fino al 55% di buio). Mette in ombra il
   sotto e gli incavi, ed è quello che dà corpo a un cumulo. Due campioni in più; il budget di §3.5
   passa da «circa venti» a «circa venticinque».
3. *La luce è di chi ne dà di più*: il sole da 1° fino a −9° (`sunLight`, una `smoothstep`), la
   luna per illuminazione e altezza; vince il maggiore e non più il sole sopra lo 0,5 di `day`. Il
   contrasto dell'ombreggiatura è scalato da `uLightAmount`: senza sole né luna le nuvole sono
   piatte, un tono solo, com'è una nuvola che niente illumina. Anche il bordo luminoso (`glow`)
   segue la luce che c'è.
4. *Il sole sotto l'orizzonte sta sotto il canvas*: `up` scende fino a −1 a −12° e lo shader
   (`placeLight`) lo mette dal piede della fascia fino a 40 dp sotto il bordo inferiore, così quello
   che gli sta di fronte è il sotto delle nuvole. Il colore chiaro segue la luce del sole e non il
   giorno (`lerp(nightLit, warmLit, sunLight)`, con l'ora d'oro piena sotto i 2°): al tramonto le
   basi prendono l'arancio e il rosa dell'orizzonte, le cime restano grigio-azzurre.
5. *Il residuo dell'errore 3*: lo stop alto del gradiente va verso quello medio di 0,6 della
   copertura, prima della desaturazione e dello scurimento di §3.3 (`CloudZenith`). Un cielo coperto è
   piatto, non un azzurro più scuro; sotto nuvole e pioggia lo stop alto passa da 0,092 a 0,138 di
   luminanza, sopra l'ombra di un nembostrato (0,13), e il rapporto orizzonte/zenit del coperto scende
   da 2,79 (sereno) a 2,18. Lo leggono anche i widget che disegnano il gradiente intero.
6. DESIGN §3.3, §3.5, §3.9 (la luce), VISION invariata.

**Decisioni e deviazioni.** *Il gradient noise provato e lasciato*: il piano diceva «gradient noise al
posto del value noise nella derivata»; portato in NumPy con la fade quintica e le soglie rimisurate
(l'alfa medio centrava la quota del banco entro l'1%), toglieva la rigatura ma cambiava il carattere:
le masse diventavano stracciate, più fractus che cumuli, e un cielo al 25% un cielo a pecorelle. Il
value noise resta, con le forme viste e tarate sul telefono da un mese, e la derivata si leviga
leggendola sulle ottave basse: rese vecchio contro nuovo su otto cieli, la rigatura sparisce uguale.
*La tabella delle soglie non si tocca*: il campo del banco è lo stesso. *Un tramonto a −3° con una
falce al 20% è ancora illuminato dal sole* (0,65 contro 0,2): la scelta pesata e non binaria è
esattamente questo caso. *I varchi di un cielo piovoso in cima* ora sono grigio-azzurri e non più buchi
scuri; restano di una tinta diversa dal banco, perché il gradiente di §3.3 tiene il 30% del colore
anche sotto la pioggia, e questo è il modello e non un difetto.

**Da verificare sul telefono.** I cumuli al 50% con il sole di lato (niente rigatura, le basi in
ombra), un tramonto con nuvole basse (le basi accese), una notte senza luna (nuvole piatte), il tempo
di GPU con i cinque campioni in più.

**Come è stato verificato.** `SkySceneTest` (due casi nuovi): a −3° la luce sta sotto la fascia
(`up` = −0,25), le nuvole sono ancora illuminate (0,65) e calde, a −30° senza luna la luce è zero e
le nuvole piatte, il bordo luminoso segue la luce che c'è; la luce è del corpo che ne dà di più (il
sole a −3° con una falce al 20%, la luna a −12°). `SkyPaletteTest` (un caso nuovo): sotto le nuvole il
rapporto orizzonte/zenit scende sotto 0,85 di quello sereno, lo zenit di un coperto piovoso sta sopra
0,13, un cielo sereno non si muove. `SkyDeckTest` fissa le righe nuove (`fbmParts` e le sue somme
parziali) e misura ancora la quota del banco, che non è cambiata. Le rese dello shader vero
(`CHIARO_RENDER_DIR`, tre cieli in più: tramonto a −3° con 40% di basse, notte senza luna, pioggia al
95%) guardate: cumuli con le basi in ombra e senza rigatura, il tramonto con i bordi bassi accesi, la
notte piatta, i varchi del cielo piovoso non più buchi. Il prototipo NumPy (`skyport3.py`, poi la
variante B) reso vecchio contro nuovo su otto cieli. Immagini del README rigenerate e guardate:
`today-now` e `today-foldable` (la Milano coperta ha ora un banco con corpo e ombre), `settings`,
`widget-day-arc` (il cielo del widget sotto le nuvole è un filo più piatto: 22 635 px diversi, 17
livelli al massimo, invisibile; `widgets` identica). Suite completa (748 app debug con i 14 screenshot
saltati, 700 release, i moduli core invariati) e lint con 0 errori.

### Passo 7: lo scrim, deciso sulla misura (19 ott 2026)

Il committente ha lasciato la decisione a chi scrive, «in base a quello che graficamente è
migliore». Decisa sulla misura e non sul gusto: un mockup in NumPy e PIL del canvas di Oggi
(Roboto, le righe dell'hero dove le mette la pagina) sopra il cielo più chiaro, un mezzogiorno
sereno e uno al 25% di cumuli, con il contrasto letto **sui pixel subito fuori da ogni glifo**, che
è dove un testo si legge, per quattro gruppi: la cifra, la parola, le righe, gli orari del nastro.

**Cosa ha detto la misura.** Con lo scrim com'è (0,55 in fondo, rampa dal 45% al 93% del canvas),
senza alone, sul mezzogiorno sereno: cifra **2,30:1** (il pavimento del testo grande è 3), parola
2,85, righe 3,27, orari del nastro 5,28. Il contratto di §3.6 tiene dove lo scrim è pieno, cioè sugli
orari in fondo; ogni riga sopra sta sulla rampa. Con uno scrim più leggero (0,30-0,40) più un alone
sfocato sul testo (2-4 dp, nero al 60-80%): cifra 3,1 (meglio), parola e righe 2,7-2,9 (uguali o
peggio), orari 3,0-4,1 (**sotto il pavimento**). Con la rampa che parte più in basso (60% o 70%)
parola e righe peggiorano ancora. L'alone aiuta la cifra, poco i tratti sottili: sotto un tratto di
1,5 dp una sfocatura lascia un terzo della sua forza.

- [x] **Lo scrim resta com'è**, geometria e alfa: il contratto di §3.6, i suoi numeri e
  `PaletteDocTest` non si muovono. La metà bassa affumicata è il prezzo del testo piccolo in fondo, e
  la misura dice che più leggero non si leggerebbe.
- [x] **Un alone su ogni testo del canvas** (`skyInkShadow`: il colore dello scrim all'80%, sfocato
  di 3 dp, 1 dp più in basso): la cifra, la parola, le righe e gli orari dell'hero, la riga del luogo
  quando sta sul cielo, l'anteprima delle Impostazioni. Porta la cifra sopra il 3:1 del testo grande
  e stacca il bianco del testo dalle cime bianche che il banco del passo 3 ora disegna sotto di esso.
  È funzione e non decorazione, come lo scrim: aggiunge contrasto, non toglie cielo.

**Decisioni e deviazioni.** *Un'alternativa non presa*: portare gli orari del nastro fuori dal
cielo, sul foglio di carta sotto, libererebbe lo scrim in fondo fino a circa 0,3 e mostrerebbe la
metà bassa del cielo; è un cambio di layout dell'hero, da decidere con il committente e non da soli.
*Il contratto non cambia*: l'alone si somma allo scrim e non lo sostituisce, quindi il caso peggiore
misurato da `ScrimContractTest` resta quello; la misura sui glifi vive in PLANNING e nello script del
mockup, perché un test Kotlin che disegni il testo vero sul cielo vero è un lavoro a sé.

**Da verificare sul telefono.** L'alone non deve leggersi come un'ombra: a 3 dp e 80% è un velo
scuro attorno ai tratti, visibile solo dove il cielo è chiaro; e la cifra su un mezzogiorno sereno.

**Come è stato verificato.** Il mockup (`scrim_mock.py` nello scratchpad della sessione: il cielo del
port NumPy, Roboto dai font di layoutlib, le righe dove le mette Oggi, il contrasto letto sull'anello
di pixel subito fuori dai glifi) su otto varianti: scrim com'è; 0,30, 0,35 e 0,40 con alone da 2 a
4 dp; rampa dal 60% e dal 70% con alone; 0,50 dal 55% con alone. `NoRawColorTest` (l'alone è il
colore dello scrim, in `ui/theme`), `ScrimContractTest` e `PaletteDocTest` invariati e verdi,
`HeroInBarTest` e `TodayAgendaTest` con la riga del luogo che prende l'alone. Immagini del README
rigenerate e guardate: `today-now`, `today-foldable` (la cifra si stacca dalle cime delle nuvole, la
riga del luogo e gli orari con il velo) e `settings`; le altre identiche. Suite completa (748 app
debug con i 14 screenshot saltati, 700 release) e lint con 0 errori.

### Passo 4: la proiezione a piano (19 ott 2026)

**Cosa è cambiato.** Il banco è un piano a una quota, visto da sotto. Le righe del canvas sono
elevazioni, 45° in cima e 15° in fondo, e il raggio di una riga incontra il piano a 1/tan(elevazione)
della sua quota: il campo del rumore si campiona in `((x − cx)·dist/f + deriva·dist_m/f, dist)`, con
`f` la focale che fa stare quelle elevazioni nell'altezza del canvas e `dist_m` la distanza della riga
di mezzo (30°, 1,732). Sopra la testa le nuvole sono vicine e grandi, verso l'orizzonte lontane,
piccole e piatte insieme; e scorrono sullo schermo 1,7 volte più in fretta in cima che in fondo, la
parallasse di un banco vero. Prima il campo era solo schiacciato in altezza verso il fondo, della
metà, e le nuvole erano una parete: larghe uguali in cima e in fondo. La scala è quella di prima
sulla riga di mezzo, 95 dp per cella, e le elevazioni coprono il canvas qualunque sia la sua altezza
(un canvas più alto vede lo stesso cielo con celle un filo più larghe, mai un orizzonte così basso da
far scintillare il rumore). I cirri tengono il loro stiramento; pioggia, neve e stelle sono dello
schermo. DESIGN §3.9.

**Decisioni e deviazioni.** *La tabella delle soglie non si rimisura*: il piano diceva di farlo, ma
misurata sul nuovo campionamento (24 canvas, come `SkyDeckTest`) l'alfa medio centra la quota del
banco con uno scarto massimo di 0,021, lo stesso dello schiacciamento di prima, perché i valori del
campo hanno la stessa distribuzione comunque lo si campioni; cambia la varianza di un canvas solo (in
cima ci sono poche celle grandi), che è quella di un'occhiata al cielo. *45°–15° e non 55°–20°*:
resi tutti e due accanto allo schiacciamento attuale su quattro cieli, quasi uguali; il primo dà un
po' più di cielo. *Le elevazioni legate all'altezza del canvas e non a una focale fissa*: con la
focale fissa un canvas da 450 dp (testo grande) scenderebbe a 6,6° in fondo, celle di 3 dp e la
quarta ottava sotto il pixel. *La direzione della luce nel campo resta quella dello schermo*: in
coordinate del piano «su» è «più vicino», e l'ombreggiatura è una stilizzazione a due dimensioni
comunque; la rende uguale a prima.

**Da verificare sul telefono.** I cumuli al 25% e al 50%: grandi in alto, una fascia sottile in fondo;
la deriva più veloce in cima che in fondo; un coperto che recede verso l'orizzonte.

**Come è stato verificato.** Il prototipo NumPy (`skyport4.py`): la tabella delle soglie misurata sul
nuovo campionamento (24 canvas di 400 × 300 dp, un punto ogni 4 dp, 100 000 dp di deriva) per lo
schiacciamento attuale e per due coppie di elevazioni, alfa medio contro quota del banco entro 0,021,
0,021 e 0,023; quattro cieli (25%, 50% col sole a est, ora d'oro, coperto) resi nelle tre versioni e
guardati. `SkyDeckTest` fissa le cinque righe della proiezione e misura la quota del banco sul port
rifatto con la stessa aritmetica, entro il 3% dal 10 al 90% come prima; `SkyShaderCompileTest` e
`SkyShaderPixelTest` verdi (i corpi e il tetto non cambiano). Le rese dello shader vero
(`CHIARO_RENDER_DIR`) guardate: cumuli grandi in alto e una fascia schiacciata in fondo, sotto
pioggia e coperto il banco recede verso l'orizzonte, la notte con la luna. Immagini del README
rigenerate e guardate: `today-now` e `today-foldable` (la Milano coperta recede), `settings`
(l'ora d'oro su un ritaglio basso, nuvole allungate come si deve); le altre identiche. Suite
completa (748 app debug con i 14 screenshot saltati, 700 release, i moduli core invariati) e lint con
0 errori.

### Passo 5: pioggia e neve a tre piani (19 ott 2026)

**Cosa è cambiato.** Quello che cade sta su tre piani, vicino, medio e lontano, ciascuno con la sua
misura, la sua velocità e la sua forza: il vicino grande e veloce, il lontano piccolo, lento e
tenue, così la caduta ha profondità. *Pioggia*: strisce da 1,8, 1,2 e 0,8 dp a 160, 90 e 55 dp al
secondo (il piano medio è quello che c'era), ciascuna dal suo campo di rumore; si inclinano col
vento, perché una goccia va dove va l'aria mentre cade, con il segno della deriva e fino a 19°, e
scorrono lungo la propria linea (spostare il campo lungo `r.y` è spostarlo lungo la striscia). *Neve*:
celle da 34, 22 e 14 dp con un fiocco in circa due terzi di esse, spostato a caso dentro la cella e di
una misura sua, che ondeggia, cade a 22, 14 e 8 dp al secondo ed è portato di lato dal vento nella
quota del suo piano. Prima c'era un piano solo: la pioggia graffi sottili a 68 dp al secondo, la neve
un fiocco per cella di 22 dp, mai due vicini, un reticolo di punti. DESIGN §3.9.

**Decisioni e deviazioni.** *Il costo*: la pioggia passa da due a sei campioni di rumore per pixel,
la neve da tre a nove hash e tre seni; dentro il budget di §3.5, perché un'ora bagnata ha il banco
chiuso e nessuna stella. *La pioggerella di notte senza luna resta quasi invisibile*, com'era: le
gocce vestono il colore chiaro delle nuvole e di notte niente lo illumina; è vero anche fuori, e la
parola dell'hero dice che piove. *Nessun prototipo NumPy*: tre piani di particelle sono aritmetica
da leggere, e le rese dello shader vero bastavano a guardarle.

**Da verificare sul telefono.** La pioggia a 30 fotogrammi: tre velocità leggibili come profondità,
le strisce che scorrono lungo la loro inclinazione e non in verticale; la neve che ondeggia senza
reticolo, con il vento che la porta di lato.

**Come è stato verificato.** `SkyShaderCompileTest` (il ciclo sui tre piani compila e prende ogni
uniform), `SkyShaderPixelTest` (due rese in più per guardare: neve al 100% a 1,5 mm/h, pioggerella
di notte a 0,4 mm/h) e `SkyDeckTest` verdi. Le rese dello shader vero guardate al pixel: la neve in
tre taglie senza reticolo, la pioggia in tre piani inclinata col vento dell'ora. Immagini del README
lanciate e identiche, perché la Milano registrata è asciutta. Suite completa (748 app debug con i 14
screenshot saltati, 700 release, i moduli core invariati) e lint con 0 errori.

### Passo 6: le stelle vere e la luna ruotata (19 ott 2026)

**Cosa è cambiato.**

1. *Il catalogo* (`BrightStars`, in `:core:domain`, generato da `tools/gen_bright_stars.py` e mai
   scritto a mano): ogni stella del Bright Star Catalogue di Yale, quinta edizione (Hoffleit &
   Warren 1991, pubblico dominio), più luminosa della magnitudine 4,5: 904 stelle, dalla più
   luminosa, con ascensione retta e declinazione J2000 in gradi, magnitudine visuale e colore B−V.
   Lo strumento scarica il catalogo se non lo trova accanto a sé. Sirio in testa a −1,46, Polare e
   Vega dentro: `BrightStarsTest`.
2. *Dove sta una stella* (`AstronomyEngine.starHorizontal`): altezza e azimut dalle stesse funzioni
   che mettono la luna, con il tempo siderale di Greenwich e la longitudine del luogo. La precessione
   dal J2000 sposta una stella di un terzo di grado in una generazione, meno di un dp del canvas, e
   non è applicata. Verificato: la Polare sta all'altezza della latitudine di Milano, a nord, a tre
   istanti diversi; Sirio non sale mai oltre la sua altezza di transito.
3. *La finestra* (`StarField.visible`): le stelle che stanno davanti a un lettore che guarda
   l'equatore (sud nel nord, nord nel sud, la cornice di sole e luna), entro ±30° di azimut e fra 5°
   e 55° di altezza, un margine oltre la camera del banco (±16,6° e 45°–15°), così un canvas più alto
   o più largo trova le sue nella lista e la lista non dipende dal layout. Ogni stella porta la sua
   luminosità (lo span di magnitudine dal limite a Sirio, piegato a 1,3 e con un pavimento di 0,2,
   perché una stella al limite sia ancora una stella), il suo calore dal B−V (da Rigel a Betelgeuse,
   una tinta e mai un colore saturo) e una fase di scintillio sua (la sezione aurea sull'indice).
   Calcolate nel `TodayUiState` ogni minuto, solo quando il sole è sotto −6°: prima nessuna si disegna.
4. *Come si disegnano* (`StarMap`): uno shader che cicli su qualche centinaio di stelle per ogni pixel
   costerebbe più delle nuvole; il campo si disegna una volta in una bitmap della misura del canvas
   (`RadialGradient` per stella: nei canali del colore luminosità, fase e calore, nell'alfa il
   disco), si ridisegna solo quando cambiano le stelle o la misura, e lo shader la legge una volta per
   pixel (`uniform shader uStarMap`, `setInputShader`). La stella è messa dalla camera del banco
   (`starPlace`: 45° in cima, 15° in fondo, l'azimut attraverso la tangente come un foro stenopeico),
   così stella e nuvola stanno in un cielo solo; di notte la nuvola la copre. Niente di giorno: il
   test di pixel lo verifica, con una Sirio al centro e una stella al limite accanto.
5. *La luna ruotata come la vede il luogo* (`AstronomyEngine.moonBrightLimbAngle`,
   `brightLimbAngle`): la direzione del sole dalla luna sul cielo, proiettata sul piano tangente alla
   sfera nella luna e risolta sugli assi «più azimut» e «più altezza», in gradi: 0 lembo acceso a
   destra (per chi guarda l'equatore, in tutti e due gli emisferi), 90 da sopra, −90 da sotto. Nello
   shader è una rotazione della maschera della fase (`uMoonLimb`): la falce della sera punta in basso
   verso il sole appena tramontato, la «barchetta». Sostituisce `litOnRight`, che è quello che
   quest'angolo dice quando il sole sta all'altezza della luna. Il test di pixel accende una luna dal
   basso e legge che il lembo chiaro sta in fondo. La scheda «Stanotte» tiene la sua luna dritta.
6. DESIGN §3.9 (le stelle), §8.1c (la luna ruotata); `SkySnapshot` porta l'angolo e le stelle.

**Decisioni e deviazioni.** *La finestra è quella del banco, 34° × 30°, e non la fascia di sole e
luna*: la fascia comprime 300° di azimut in 400 dp e 60° di altezza in 72, e una costellazione
schiacciata nove a uno non è riconoscibile; la camera del banco è quasi conforme, e in quella finestra
Orione a gennaio è Orione. Così il cielo ha tre cornici dichiarate: la fascia per i corpi (un vincolo di
layout), la camera per nuvole e stelle. *Una bitmap e non un ciclo nello shader*: 904 stelle, o anche le
25 della finestra, per ogni pixel a 15 fotogrammi, sono più del banco intero; una lettura di texture
costa meno delle stelle a hash di prima. *Magnitudine 4,5 e non 3,5*: a 3,5 la finestra ha sette stelle
in una sera di gennaio e il cielo sembra vuoto; a 4,5 ne ha venticinque, le deboli appena visibili, che
è quello che si vede da una città. *Niente inquinamento luminoso*: nessun dato lo dice. *La luna di
«Stanotte» resta dritta*, diversamente da quanto detto a voce: quella luna sta per una notte intera e
non per un istante, e l'inclinazione cambia durante la notte; sarebbe la bugia di un istante. *Il raggio
e il pavimento ritoccati dopo la prima resa* (da 0,6 + 1,6·b a 0,8 + 2,2·b dp, pavimento da 0,12 a 0,2):
Rigel era un puntino e le deboli invisibili.

**Da verificare sul telefono.** Una notte serena: le stelle dove stanno (Orione a sud nelle sere
d'inverno, il Cigno alto in estate: controllare alzando gli occhi), lo scintillio, il loro passare dietro
le nuvole; la falce della sera inclinata verso il sole tramontato; il costo: il campo si ridisegna una
volta al minuto, lo shader legge una texture.

**Come è stato verificato.** `BrightStarsTest` (nuovo, `:core:domain`): il catalogo è 904 stelle entro
la 4,5, ordinate, Sirio in testa con i suoi numeri, Polare e Vega dentro; la Polare sta alla latitudine
di Milano a nord a tre istanti, Sirio non sale oltre il transito (entro 0,3°); il lembo punta al sole
(a destra, da sotto, da sopra, la «barchetta» della sera, lo specchio) e l'angolo ricostruisce il
vettore da cui nasce. `StarFieldTest` (nuovo): la finestra di una sera di gennaio a Milano ha fra 8 e
120 stelle, ognuna dove il motore mette la sua, la Polare mai; a Sydney la finestra guarda a nord e non
divide stelle con quella a sud; luminosità e calore dalle formule; la camera mette una stella al centro,
in cima, a destra con la focale del piano, e fuori quando è fuori. `SkyShaderPixelTest` (due casi
nuovi): una Sirio al centro e una stella al limite accanto, brillanti di notte e nulla di giorno; una
luna accesa dal basso ha il lembo chiaro in fondo; più due notti vere di Milano rese per guardarle
(gennaio con Orione, luglio), guardate due volte, la seconda dopo il ritocco di raggio e pavimento.
`SkySceneTest` porta l'angolo del lembo; `SkyShaderCompileTest` lega la texture. Immagini del README
lanciate e identiche (la Milano registrata è a mezzogiorno). Suite completa (753 app debug con i 14
screenshot saltati, 703 release, 358 domain, 275 data, 24 sync) e lint con 0 errori.

### Passo 6, seconda parte: la polvere e la finestra larga (committente, 20 ott 2026)

**Da dove nasce.** Dal telefono, la prima notte serena dopo il passo 6: «il cielo di notte guardando
a sud è quasi vuoto, due o tre stelle», a Milano e altrove. Misurato sul catalogo prima di rispondere
(le formule del motore, senza rifrazione; al bordo della finestra il conto può cambiare di qualche
unità), Milano, 19 ottobre 2026 alle 21 ora legale e, per confronto, la sera del 15 gennaio alle 22
con Orione a sud:

| finestra | limite | stelle | sotto la terza magnitudine |
|---|---|---|---|
| quella del banco, 34° × 30° (passo 6), ottobre | 4,5 | 15 | 3 (Capricorno e Acquario) |
| quella del banco, ottobre | 6,0 | 88 | 3 |
| la camera larga, 55° × 59°, ottobre | 4,5 | 45 | 5 |
| la camera larga, ottobre | 6,0 | 242 | 5 |
| quella del banco, gennaio | 4,5 | 30 | 10 |
| la camera larga, gennaio | 4,5 / 6,0 | 91 / 441 | 18 |

E la luna gibbosa crescente, nove giorni, stava proprio a sud, che per la regola della scena (§3.9)
dimezza le stelle: le quindici diventavano sette, e le deboli sparivano sotto la soglia. Il cielo non
mentiva, era davvero quello: l'autunno a sud è il tratto più vuoto dell'eclittica, e una finestra di
34° sotto una luna alta ha poco da mostrare. Ma un cielo vuoto non è un cielo premium.

**La scelta: due leve vere, e nessuna stella inventata.** Proposto al committente: lasciare com'è,
riempire con stelle casuali, solo stelle casuali, o le due leve che seguono. *Le stelle casuali sono
state rifiutate*, da sole o mescolate alle vere: una stella che il lettore può smentire alzando gli
occhi è l'unica cosa che questo canvas non deve disegnare, ed è la regola del README («nessun dato
inventato») applicata al cielo. Il committente: «Procedi con tutte e due le leve».

**Cosa è cambiato.**

1. *La camera delle stelle è sua, e più larga* (`StarCameraTopDeg = 65`, `StarCameraBottomDeg =
   10` in `StarMap.kt`, `starPlace`): 55° di altezza sul canvas invece dei 30° del banco, e con la
   stessa focale a foro stenopeico ±29,3° di azimut sul canvas base (384 × 328 dp) invece di ±17°.
   Il banco tiene la sua camera di 45°–15° (`SkyCameraTopDeg`, `SkyCameraBottomDeg` in
   `SkyShader.kt`): la tabella delle soglie misurata al passo 4 vale per quella, e una nuvola non si
   confronta con una stella. La finestra di `StarField.visible` segue con il suo margine: ±36° di
   azimut, 5°–70° di altezza.
2. *Il catalogo arriva alla magnitudine 6,0*, il limite dell'occhio sotto un cielo buio: 5 080
   stelle. A quel numero un array Kotlin di literal supera i 64 KB di un inizializzatore statico
   della JVM, quindi il catalogo è una risorsa binaria di `:core:domain`
   (`src/main/resources/com/callbackdev/chiaro/domain/sky/bright_stars.bin`, 40 648 byte): «CHST»,
   versione uint16, conteggio uint16, poi per stella quattro campi a 16 bit big-endian (ascensione
   retta uint16 su 0–360°, declinazione int16 su ±90°, magnitudine × 1000, B−V × 1000). Lo scrive
   `tools/gen_bright_stars.py` (`LIMIT_MAG = 6.0`), lo legge `BrightStars.kt`, ora un caricatore
   pigro scritto a mano che pretende la magia e la versione 1. Lo strumento resta il disegno: si
   cambia lui e si rilancia, mai il binario.
3. *La luminosità distingue la polvere dai dischi* (`StarField.brightness`, `StarMap.drawStars`):
   lo span di magnitudine va dal limite a Sirio piegato a 1,5 con un pavimento di 0,08
   (`0,08 + 0,92·t^1,5`): un decimo di Sirio al limite, un quinto alla 4,5, un terzo alla terza
   magnitudine. Sotto 0,2 (`DustBrightness`) la stella è un granello pieno al 70% del raggio e
   alfa 0xB0, non un gradiente: le stelle fra la 4,5 e la 6 sono la polvere che un cielo buio ha,
   e disegnate a disco sarebbero state una nebbia. Il raggio resta 0,8 + 2,2·b dp.
4. *Dirlo al lettore e a chi tocca lo shader*: una frase nel README e una nel paragrafo del cielo
   della guida (`guide_today_p1`, IT e EN): di notte le stelle sono quelle vere, il cielo davanti a
   chi guarda a sud (a nord sotto l'equatore), disegnate dove stanno; il sole e la luna, disegnati
   come sono, tengono la loro fascia, la striscia fra il nome del luogo e il numero grande, quindi
   la distanza fra la luna
   e una stella sullo schermo non è quella del cielo. DESIGN §3.9 e §8.1c dicono la stessa cosa a
   chi tocca lo shader, con il perché: tre cornici dichiarate, il piano del banco (45°–15°), la
   camera delle stelle (65°–10°) e la fascia dei corpi (0°–60° in 72 dp); non è un disallineamento
   da correggere, ed è scritto perché nessuno lo «corregga» per sbaglio.

**Decisioni e deviazioni.** *Due cornici per le stelle e il banco, e non una*: al passo 6 la camera
unica era la scelta («stella e nuvola in un cielo solo»); allargare anche il banco a 65°–10° avrebbe
rifatto la tabella delle soglie del passo 4 e schiacciato i cumuli, e nessuno confronta la posizione
di una nuvola con quella di una stella. La nuvola copre la stella comunque: la texture si legge sotto
il banco. *65° in cima e non lo zenit*: sul canvas base il banco dava 10,9 dp per grado d'altezza e
Orione da Betelgeuse a Rigel (18,6°) non ci stava (203 dp su 328, e ±17° di azimut); a 90°–10° la
scala scende a 4,1 dp per grado, Orione sta in 76 dp e la cintura (2,7°) in 11 dp, una costellazione
da cartolina; a 65°–10° sono 6 dp per grado, Orione in 111 dp, la cintura in 16 dp, tre stelle
distinte, e la finestra tiene tre volte le stelle di prima. *Magnitudine 6,0 e non 5,0*: a 5,0 la
sera d'ottobre ha 79 stelle e il campo è un cielo di periferia; a 6,0 ne ha 242 ed è il cielo dei
monti, che è quello che un cielo «premium» promette; la polvere sotto la 4,5 è quello che fa la
differenza fra un disegno e un cielo. *Niente Via Lattea*: idea registrata (una banda debole lungo il
piano galattico, ferma rispetto alle stelle), non decisa; non costa quasi nulla nella texture, ma è una
cosa in più da far vera.

**Da verificare sul telefono.** La prima notte serena: la ricchezza del campo (a sud in ottobre
Capricorno e Acquario sotto la luna, la Fomalhaut bassa a sinistra; Orione in inverno), la polvere che
non diventi nebbia sullo schermo del telefono, le stelle che passano dietro le nuvole, la falce della
sera inclinata. Il costo non cambia: la texture si ridisegna una volta al minuto, con 250 stelle
invece di 25, e lo shader fa la stessa lettura per pixel.

**Come è stato verificato.** `BrightStarsTest`: il binario si legge, 4 900–5 300 stelle ordinate
dalla più luminosa, Sirio in testa coi suoi numeri, Polare e Vega dentro, la posizione della Polare e
il transito di Sirio come prima. `StarFieldTest`: la finestra di gennaio a Milano tiene fra 200 e 900
stelle, mai la Polare (sta a 45° ma a nord); la luminosità vale 0,09 al limite, 0,2 alla 4,5 e il
pavimento 0,08; la camera mette al centro la stella a 37,5°, in cima quella a 65°, dentro una a ±28°
di azimut e fuori una a 31°, fuori una a 70° di altezza. `SkyShaderPixelTest`: la stella al limite
accanto a Sirio è ora polvere (0,09) e si legge ancora sopra il cielo (`faint > sky + 0.005`); le tre
notti vere di Milano (gennaio, luglio e la nuova `stars-milan-october` del 19 ottobre alle 19 UTC,
con la gibbosa a sud) rese con `CHIARO_RENDER_DIR` e guardate: ottobre ha la luna sulla fascia e la
polvere con le poche luminose del Capricorno, gennaio è denso con Orione, luglio è ricco.
`SkyShaderCompileTest`, `SkySceneTest` come prima. Immagini del README rilanciate: `guide.png` cambia
se il paragrafo è in vista, le altre sono identiche (la Milano registrata è a mezzogiorno). Suite
completa (753 app debug con i 14 screenshot saltati, 703 release, 358 domain, 275 data, 24 sync) e
lint con 0 errori.

**Lo screenshot notturno del README** (committente, 20 ott 2026: «uno screenshot della schermata
Oggi di notte per far vedere il rendering di stelle e luna reali; scegli una condizione meteo
suggestiva»). La regola del README vieta i dati inventati, quindi la notte è quella della
registrazione stessa (Milano, 25 settembre, scaricata alle 12:10), e la condizione è scelta fra le
ore notturne che la registrazione ha, non disegnata per piacere. Misurato sulla registrazione e sul
motore (luna e stelle): nessuna notte della settimana ha pioggia o nebbia, e la luna è piena il 26,
quindi sopra l'orizzonte tutta la notte, ogni notte, al 94–100%.

| ora del 25 settembre | sole | nubi (basse / alte) | luna | resa |
|---|---|---|---|---|
| 21:00 | −19° | 13% (0 / 13) | 24° a est-sudest, 99% | sereno, la luna con l'alone, le stelle che una luna piena lascia |
| 22:00 | −29° | 24% (11 / 14) | 32° a sudest | la luna fra qualche nube, metà delle stelle |
| 23:00 | −37° | 35% (35 / 0) | 39° a sud-sudest | la luna dietro un cumulo, si vede un bordo |

Scelte le 21:00: l'unica in cui si vedono tutte e due le cose chieste. Le stelle sono poche e
deboli perché la luna è piena e la scena le dimezza (§3.9): è il cielo vero di quella sera, e la
didascalia lo dice («the stars that are there»). *Il ritaglio*: la previsione è quella delle 12:10,
e alle 21:00 la pagina porta onestamente il chip «Updated 8 hours ago» come primo elemento del
foglio; non è l'età della notte ma quella della registrazione, quindi l'immagine è l'eroe da solo,
tagliata 2 dp sopra il chip (`todayNight` lo trova dal suo testo). La riga «Estimated from the
forecast» dell'eroe resta: è vera, l'osservazione è di mezzogiorno e la temperatura delle 21 è
stimata dalle ore. *Niente nuova registrazione*: registrare Milano di notte avrebbe dato una pagina
intera senza chip, ma cambia ogni immagine (regola del 25 set 2026) o richiede una seconda
registrazione con un caricatore suo; da fare se il committente vuole la pagina intera. *Tema scuro*,
come «Stanotte»: la foto notturna della famiglia. *Una parola corretta*: README, guida e DESIGN
dicevano che la luna sta «vicino alla linea dell'orizzonte»; sullo schermo la fascia è la striscia
fra il nome del luogo e il numero grande, e nell'immagine si vede; ora lo dicono.
`MilanRecording.night` porta l'istante e il perché; `ReadmeScreenshots.todayNight` il taglio. README:
l'immagine sotto la riga di «Today», larga 384 (l'eroe a 1×, le stelle grandi quanto sul telefono).

Verificato: le tre ore rese e guardate, le due scartate cancellate; `today-night.png` guardata alla
fine; `StringsParityTest`, `SkyGuideTest`; le altre immagini del README rilanciate e identiche.

**Il crash di notte sull'APK minificato** (committente, 20 ott 2026, dal telefono: «va in crash
quando scelgo una località dove è notte; su Milano, di giorno, funziona tutto»). Verificato
costruendo in locale i due APK della CI: il catalogo `bright_stars.bin` sta in tutti e due, al suo
posto (`com/callbackdev/chiaro/domain/sky/`), ma nella mappa di R8 della release `BrightStars` è
ripacchettata in `j7.o`; il caricatore cercava la risorsa con un nome relativo alla classe,
`getResourceAsStream("bright_stars.bin")`, che R8 fa diventare `j7/bright_stars.bin`: nulla,
`error(...)`, e la prima volta che il sole scende sotto −6° (le stelle si calcolano solo allora)
l'app cade. L'APK di debug non è minificato e non ne soffre, e nessun test unitario gira sotto R8:
per questo la suite era verde e la notte di Milano del README si disegnava. Corretto con il
percorso assoluto (`BrightStars.RESOURCE`, con la barra iniziale), che R8 non tocca perché non
rinomina le risorse Java; il commento sulla classe dice perché, così nessuno lo «semplifica».
Regola da tenere: **una risorsa Java si carica per percorso assoluto**, mai relativo alla classe,
in un'app che passa da R8. Nello stesso giro due cose trovate leggendo `StarMap` per cercare il
crash: `Color.rgb` mette l'alfa a 0xFF e un `or` sopra lasciava opachi tutti gli stop (la
polvere a 0xB0 e il bordo del disco a 0xA0 erano scritti e non applicati); ora i tre canali sono
mascherati e l'alfa è quello scritto, e le tre notti di Milano sono state rese di nuovo e guardate.
La documentazione di `StarField` allineata ai conteggi di DESIGN e PLANNING (45 / 242, 91 / 441).

### Venere e Giove nell'eroe (committente, 20 ott 2026)

**Da dove nasce.** Il committente, pensando al widget: «Venere e Giove, che sono due punti di
luce che un passante nota, sono disegnati oggi nell'eroe? Se no, pensi che sia buona idea?». Non
lo erano: il motore li calcola dalla Fase 28 (`PlanetMath`, Venere e Giove soltanto, a qualche
minuto d'arco), ma il canvas disegnava il solo catalogo delle stelle. Sì, è una buona idea, per
tre ragioni: un cielo che mostra Altair e non Giove accanto è smentibile alzando gli occhi; costa
due marchi nella texture; e una cosa vera in più li distingue dalle stelle, la luce ferma.

**Cosa è cambiato.** `StarField.planets(at, coords, southern)`: i due pianeti dove il motore li
mette, nella finestra delle stelle, come `StarMark` con luminosità 1 (la mappatura si ferma a
Sirio, e −4 e −2 stanno ben oltre), disco più largo (`scale` 1,4 Venere, 1,2 Giove: l'occhio li
vede come dischi dove una stella è un punto), tinta calda (0,45 e 0,55) e fase `STEADY_PHASE` =
1: lo shader legge una fase sopra 0,985 come «luce ferma» e non la fa scintillare; le fasi delle
stelle restano sotto `STAR_PHASE_CEILING` = 0,97 (il byte della texture le separa di sette
livelli), così nessuna stella è mai letta come pianeta. `markRadiusDp` in `StarMap` porta la
scala. `TodayUiState` somma stelle e pianeti nello snapshot, sotto lo stesso sole (−6°).
`StarField.visible` e `planets` hanno ora anche la forma con direzione e mezza larghezza
(`facingDeg`, `halfWidthDeg`): il widget «finestra» guarda altrove ed è più largo che alto.

**Decisioni e deviazioni.** *Sotto lo stesso sole delle stelle*: Venere si vede nel crepuscolo
chiaro, prima di −6°, ed è proprio lì che la gente la nota; disegnarla prima delle stelle
richiede una regola sua nello shader (il fattore `stars` della scena si apre con il buio); è il
limite scritto in DESIGN §3.9, da riaprire se dal telefono manca. *Luminosità 1 e non oltre*: il
canale rosso della texture è un byte, e un pianeta più luminoso di Sirio si dice con il disco,
non con un valore che non c'è. *Niente simbolo*: un pianeta è un punto di luce, e il canvas non
scrive; i nomi stanno sul widget.

**Come è stato verificato.** `StarFieldTest`: nessuna stella ha fase sopra il soffitto, il
soffitto e la fase ferma distano più di tre livelli; in un anno di sere a Milano (ogni cinque
giorni alle 22) i pianeti compaiono, e ogni marchio sta dove il motore mette il suo pianeta,
entro la finestra, con fase ferma, luminosità 1 e disco più largo di Sirio; la finestra con
direzione e larghezza sue restituisce le stesse stelle a sud, di più se più larga, e altre se
girata a sud-est. `SkyShaderCompileTest` compila il ternario; `SkyShaderPixelTest` e
`SkySceneTest` come prima.
