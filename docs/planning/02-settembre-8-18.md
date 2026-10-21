# PLANNING, archivio 02: 8-18 settembre 2026: le review, le allerte ufficiali, le icone (Fasi 11-13)

> Congelato il 21 ott 2026: un periodo chiuso del registro, spostato qui da `PLANNING.md`
> senza cambiarne una riga. Non si modifica. Il piano vivo è [`PLANNING.md`](../../PLANNING.md);
> l'indice di tutti i titoli è [`README.md`](README.md).

---

## Il Diario, revisione della card (committente, 8 set 2026) — e il cerchio si chiude

Nove rilievi su una schermata sola, più la funzione che le mancava. In ordine di peso.

- [x] **L'asse della striscia diventa il tempo.** Era una colonna per *fetch*: alla
      cadenza predefinita di un'ora la striscia mostrava quattordici ORE sotto un
      titolo che dice «la settimana», stampava sotto «14 aggiornamenti, da Sab 5 a Sab
      5» (primo e ultimo formattati a solo giorno, quasi sempre lo stesso), e disegnava
      una notte a telefono spento esattamente come un'ora di sonno. Ora una colonna è
      una fascia di **sei ore**, quattordici fasce sono tre giorni e mezzo — la
      finestra che VISION §5.5 promette — e una fascia senza fetch è una **casella
      vuota**, non una colonna che sparisce. Le fasce vuote in testa si tagliano: la
      striscia comincia dove comincia la prova, mai prima. La didascalia stampa la
      larghezza della fascia e i due estremi con **giorno e ora**
- [x] **Le righe sono solo i giorni ancora davanti.** L'orizzonte del fetch più recente
      parte dal *suo* domani: dopo tre giorni offline la striscia intitolata «la
      settimana» mostrava giorni già passati. Ora le date si filtrano su oggi, e una
      striscia senza più giorni davanti non si disegna
- [x] **La legenda delle massime stampava estremi che la scala non ha.** Campionava
      −10…40 °C su una rampa ancorata a −5/35 con `coerceIn`: misurato, ΔE(−10, −5) =
      0,00 e ΔE(40, 35) = 0,00 — due pastiglie duplicate e due numeri falsi, contro
      DESIGN §8.10 e §9.3. Ora i sette campioni sono equispaziati fra le ancore vere e
      i numeri sotto sono quelli
- [x] **La frase della deriva partiva in minuscolo, in italiano**: «sabato 5 è
      migliorato». `JournalText` e i titoli dei giorni facevano già `titlecase`, la
      frase no; in inglese il difetto non si vedeva perché il giorno è già maiuscolo
- [x] **«Oggi» e «Ieri» seguono il fuso del LUOGO**, come il raggruppamento sopra:
      leggevano `LocalDate.now()` di sistema, e il Diario di Tokyo visto dall'Italia
      poteva intitolare «Oggi» il domani di Tokyo
- [x] **Un tocco sulla striscia apre i numeri.** C'era un `onClick = {}`: ripple che non
      faceva niente e, peggio, un'azione «tocca due volte per attivare» annunciata a
      TalkBack che non esisteva. Il suggerimento cambia di conseguenza («tocca per i
      numeri») e la striscia guadagna una descrizione unica — le caselle non hanno
      testo, quindi senza di quella un lettore di schermo sentiva sette date e nient'altro
- [x] **La pioggia non inventa più uno zero.** `journal_field_rain` stampava
      `old ?: 0`, cioè «pioggia 0% → 80%» dove il valore vecchio non c'era, mentre le
      temperature accanto stampavano già il trattino. E il difetto veniva da più in
      basso: vedi la riga sulla probabilità nullable, sotto
- [x] **Tre stati per la frase, non due.** «Finora la settimana è rimasta com'era»
      copriva sia la settimana ferma sia quella che si è mossa ed è tornata — la frase
      poteva contraddire le due revisioni elencate sotto di sé. Ora esiste «è andato su
      e giù, fra X e Y», con le stesse soglie del motore di diff (10 punti, 1 °C) così
      la frase non può mai affermare un movimento che le voci non hanno registrato
- [x] **La retention diventa per città.** `prune` era globale: con quattro luoghi
      salvati ogni Diario aveva venticinque commit, e la profondità della propria
      cronologia dipendeva da quanti altri posti si seguono. Ora cento commit **per
      città**, più un tetto globale come rete di sicurezza (togliere un luogo non
      cancella i suoi commit). Di conseguenza il Diario legge tutta la cronologia della
      città e non più 40 righe — che a cadenza oraria erano meno di due giorni, meno
      della finestra della striscia stessa
- [x] **Il tick del minuto sparisce.** Il ViewModel ricostruiva quaranta righe di JSON e
      il motore di diff una volta al minuto, anche a dati fermi e anche in risparmio
      energetico (cosa che Oggi non fa). Niente su quella schermata invecchia con
      l'orologio: gli orari sono tutti assoluti. Ora segue un Flow di Room per città,
      che riemette alla scrittura e solo alla scrittura — più un solo risveglio a
      **mezzanotte del luogo**, che è l'unico evento d'orologio che questa schermata ha
      davvero (lì «Oggi» diventa «Ieri», e lì una giornata finita diventa giudicabile).
      Il fuso lo risolve una funzione sola, `JournalStateBuilder.zoneOf`, così
      raggruppamento, etichette, verdetti e sveglia non possono discordare
- [x] **«E poi com'è andata?»** — il cerchio che mancava: `ForecastOutcome`, e una riga
      in più che chiude ogni giornata finita

### Decisioni dell'intervento

- **La probabilità giornaliera diventa nullable fino a schermo.** Il seme mappava
  `precipitation_probability_max` con `?: 0`, e uno zero è una previsione di niente
  pioggia messa in bocca a un modello che non ha parlato — esattamente ciò che §1.1
  vieta, e che Chiaro già rispettava per l'ora ma non per il giorno. Ora: la riga della
  settimana non stampa nulla e tiene la sua colonna (un trattino sarebbe un valore), lo
  snapshot omette la chiave, la striscia disegna assenza, la variabile di regola
  `today.precip_pct` non si risolve e la sua regola salta invece di far scattare
  `< 10` su un dato mancante, e il riepilogo del mattino lascia cadere la frase della
  pioggia invece di annunciare «0%». Registrato in `UPSTREAM.md`: è un bug anche
  a monte.
- **Il verdetto della giornata poggia sull'ora, non sull'istante.** `current.precipitation`
  di Open-Meteo è la somma dell'ora **precedente**, quindi con fetch orari le
  osservazioni piastrellano la giornata invece di campionarla e sperare che non abbia
  piovuto in mezzo. Due chiavi nuove nello snapshot (`current.wmo_code`,
  `current.precip_last_hour_mm`): il codice perché la domanda «pioveva quando hai
  guardato» deve avere per risposta un numero che il dominio sa leggere
  (`WeatherCodes.isPrecipitation`), non l'etichetta inglese riconosciuta a mano.
- **Le due regole del verdetto non sono simmetriche, ed è il punto.** «Ha piovuto»
  basta una osservazione bagnata: il positivo è una prova, e nessuna ora mancante può
  dis-vederla. «Non ha piovuto» pretende **16 ore delle 24 davvero coperte** (finestre
  unite, così una raffica di pull-to-refresh non compra copertura). Sotto quella soglia
  la giornata non prende nessun verdetto — non «forse», proprio nessuna riga: una
  sezione senza dati non si disegna. Le ore coperte sono stampate accanto al verdetto,
  come un run del cielo stampa il suo `obs`: un giudizio che nasconde la sua copertura
  è un giudizio che non si può pesare.
- **La massima osservata si chiama «vista».** È un massimo su campioni, non una misura
  della stazione, e la parola lo dice. Compare solo con la copertura sopra soglia,
  perché un massimo su tre letture non è un massimo.
- **Un commit scritto prima delle due chiavi è silenzio, non un'ora asciutta.** Niente
  fallback che riconosca a ritroso l'etichetta inglese: la funzione si riempie dai
  giorni successivi all'aggiornamento invece di giudicare giornate su cui non ha prove.
- **Il verdetto apre il giorno che chiude**: è datato all'ultimo istante della giornata,
  quindi l'ordinamento dal più recente lo mette in cima alla sua sezione.
- **La spunta non è un verdetto.** L'icona dice «questa giornata è stata verificata»,
  non «bene» o «male»: verde e rosso sarebbero un giudizio sul tempo, e com'è andata è
  un fatto. La parola porta l'esito, come dappertutto qui.
- **Le Minime restano fuori dalla striscia, e adesso è scritto perché.** Non sono
  escluse dal Diario — sono salvate, diffate a 1 °C e stampate nella prosa delle
  revisioni («minima 12° → 14°»): sono fuori dai *chip*. La minima non è una domanda di
  deriva ma di soglia — «gela?», «si dorme?» — e quella risposta si scrive con un
  avviso, non con una striscia di calore. E un giudizio non le si può dare: il verso si
  ribalta con la stagione, perché una minima che scende è sollievo ad agosto e brina a
  gennaio, e qui ogni numero deve portare la sua conseguenza. Conferma misurata, non
  motivo principale: sulla rampa ancorata al mondo una fascia di minime italiane
  (≈2–20 °C) occupa il 45% della rampa contro il 65% delle massime.
- **Quello che NON è stato aggiunto**, per la stessa regola: filtri per tipo di voce (le
  soglie tengono già basso il volume), ricerca, esportazione, un terzo chip qualsiasi.
  Vento e millimetri non esistono in `DailyForecast` e costerebbero modello, mapper,
  schema e migrazione: si valutano per conto loro, non si infilano qui.

### Coda dell'intervento (committente, 8 set 2026) — il sottozero

- [x] **`Formats.temperature` stampava `-0°`.** Domanda del committente sulle minime
      sottozero; la risposta è che escono senza filtri né clamp (`temperature_2m_min` è
      un `Double` non-nullable che arriva intatto a schermo, e lo slider degli avvisi va
      da −30 a +45 °C, quindi «minima sotto 0» è scrivibile). Cercando la conferma è
      saltato fuori un difetto vero: `%f` conserva il segno di un valore che ha appena
      arrotondato via, quindi −0,4 °C stampava `-0°`. Verificato sulla JVM prima e dopo:
      `-0,4 → "0°"`, `-0,5 → "-1°"`, `-3,4 → "-3°"`. Vale per ogni temperatura di ogni
      schermata, perché passano tutte da lì. Sette asserzioni nuove in `FormatsTest`,
      su entrambe le lingue, entrambe le unità e due precisioni.

### Il gelo nella striscia (committente, 8 set 2026)

- [x] **Un segno accanto al giorno, non un terzo chip.** La domanda «e d'inverno?» ha
      una risposta che la striscia poteva dare senza tradire sé stessa: il gelo è una
      soglia, e una soglia si segna, non si dipinge su una rampa. Un fiocco accanto
      all'etichetta del giorno quando la minima prevista è **a zero o sotto**, e sotto
      la striscia una riga che nomina quei giorni col loro numero — «Gelo previsto:
      sabato 5 (−2°), domenica 6 (0°)». Il dato era già su disco: `low_c` sta nello
      snapshot dalla Fase 7.

### Decisioni

- **Zero, non due gradi.** La brina da irraggiamento arriva anche con una minima a 2 °C
  perché il suolo irraggia più dell'aria a due metri, ma un segno che scatta su una
  notte che il termometro del lettore leggerà +2 è un segno che si impara a ignorare.
  La sfumatura la porta un avviso, che ha le parole per dirla; un glifo no.
- **Decide l'ultima parola, non la più fredda mai detta.** Il segno dice cosa prevede
  l'app adesso: un giorno che è tornato sopra zero lo perde, invece di tenersi un
  avvertimento che nessuno sostiene più.
- **Non è mai il glifo da solo** (DESIGN §10): la riga sotto la striscia è insieme la
  legenda del segno e l'informazione stessa, e sta **fuori** dalla descrizione unificata
  della striscia, così un lettore di schermo la riceve come frase propria invece che
  ripiegata in un paragrafo sulle colonne. Quando non gela non c'è: una riga «niente
  gelo» ogni settimana è il riempitivo che questa schermata rifiuta.
- **Lo spazio del segno si aggiunge a tutte le righe o a nessuna**, così le caselle
  restano una griglia sola; una settimana senza gelo disegna esattamente la striscia
  che disegnava prima.
- **L'accessore si chiama `frost`, non `snowflake`** (§13.1: nomina la grandezza, non il
  disegno). Il disegno è il fiocco di Meteocons, ma la domanda lì è il ghiaccio, non la
  neve.
- **L'esempio della guida resta senza segno.** Le percentuali del campione sono
  dichiaratamente inventate, ma un avviso di gelo su un giorno vero e nominato si
  leggerebbe come una previsione: il testo lo descrive a parole, il disegno non lo finge.

### Rimasto aperto

- **Il Diario del GPS si azzera se ti sposti.** `cacheKey` è lat/lon arrotondati a due
  decimali (~1,1 km), quindi «la mia posizione» cambia chiave con te e la cronologia
  riparte: la schermata dice «Ancora niente da raccontare» mentre i commit esistono
  sotto un'altra chiave. Non toccato di proposito: quella chiave governa cache, cache su
  disco, avvisi, cielo e widget, e re-inchiavarla è una decisione di prodotto con una
  migrazione dietro, non una rifinitura del Diario.
- **Sotto −5 °C il colore satura, il numero no.** `temperatureAt` è ancorata a −5/35
  con `coerceIn`, e la usano due superfici: la striscia di deriva sulle massime e — cosa
  che la review aveva mancato — la barra di intervallo della settimana
  (`WeatherCharts.kt:292`). Quindi il capo sinistro della barra a −5 e a −12 è dello
  stesso blu. La barra però non mente: il numero stampato accanto è esatto e la
  posizione è giusta, perché quella scala è il min/max della settimana e non le ancore
  della rampa. È il costo dichiarato di DESIGN §9.1 (una scala ancorata al mondo, non a
  ciò che c'è a schermo), non un difetto da correggere di nascosto: se le ancore vanno
  allargate è una decisione di design, con le sue misure di contrasto da rifare.
- **`AlertKind.PRECIPITATION` tiene il suo `?: 0`**: lì la probabilità viene dall'ora che
  ha superato la soglia, quindi è non-nulla per costruzione e il fallback non può
  scattare. Lasciato com'è per non allargare la modifica.
- **Le due `plurals` nuove non definiscono `many`** e lint le segnala, come le sei già
  presenti nel file: in italiano `many` vale da un milione in su, e qui si contano ore
  di una giornata e larghezze di fascia. Coerenti col resto del file.

### Verifica

823 test verdi in tutti i moduli (11 nuovi in `ForecastOutcomeTest`, 4 nuovi in
`JournalStateBuilderTest`, 2 in `WeatherSnapshotsTest`), zero failure, zero skip;
`:app:lintDebug` a zero errori (66 warning, gli stessi di prima più le due `many`
sopra); `:app:compileDebugKotlin` pulito.

---

## Il widget «Ora» in tre forme (committente, 8 set 2026, sera)

Richiesta, con lo screenshot della home accanto al widget meteo del launcher a quattro
misure (4×1, 3×1, 2×1, 2×2): togliere minima e massima («un utente guarda i dettagli
aprendo l'app, il widget è un'info rapida sullo stato attuale»); al posto della stringa
dello stato una descrizione della giornata come quella del vicino («Probabili temporali
stanotte»), con lo stato come ripiego; e un layout che **cambi con la dimensione** come
fa il suo, con testi allineati e visibili dove c'è spazio. Lo sfondo resta il nostro.

### Misure prese dallo screenshot (923 px di larghezza, ≈2,35 px/dp)

| | Card | Glifo (inchiostro) | Temperatura | Descrizione |
|---|---|---|---|---|
| Vicino 4×1 | ~340 × 74 dp | ~40 dp, a 17 dp dal bordo | ~27 sp | 2 righe ~16 sp, a destra, a ~25 dp dal bordo |
| Vicino 3×1 e 2×1 | ~250 / ~159 × 74 dp | come sopra | come sopra | **assente** |
| Vicino 2×2 | ~159 × 189 dp | ~64 dp, in alto a destra | ~30 sp | 2 righe sotto il numero, poi il luogo |
| Chiaro 4×1 (prima) | ~340 × 82 dp | ~64 dp | 34 sp | «Poco nuvoloso» a 20 sp accanto al numero |

Quindi una cella del launcher concede ~82 dp d'altezza al widget (i ~101 della quarta
passata erano la cella, non la concessione), due celle ~159 dp di larghezza, tre ~250,
quattro ~340.

### Le tre forme (`NowWidgetLayout.kt`, puro, con tabella in `NowWidgetLayoutTest`)

- **Stretta** (una riga, due o tre celle): glifo, temperatura, luogo. Come prima, meno
  le opzioni.
- **Larga** (una riga, quattro celle o più): la stessa riga, e la **frase del giorno**
  contro il bordo opposto, allineata a destra e centrata sulla riga. Le due colonne di
  testo si dividono lo spazio a metà (`nowSentenceColumnWidth`): Glance non misura il
  testo, e la metà è anche quello che fa il vicino — il suo blocco di descrizione è largo
  quanto quello del numero, e il vuoto cade in mezzo dove l'occhio se lo aspetta. La frase
  compare quando la sua colonna ha almeno 96 dp (≈14 caratteri a 14 sp): a quattro celle
  sono 116, a tre 71.
- **Alta** (due righe o più, `TallMinHeight` 150 dp): il glifo da solo nell'angolo in alto
  a destra, sotto temperatura, frase (due righe) e luogo impilati a sinistra — l'ordine del
  vicino e l'ordine in cui legge Oggi. Un `Box` e non una `Column`: il glifo è ancorato in
  alto, le parole pendono dal basso, e le due scatole **possono condividere la banda vuota
  sopra le maiuscole della temperatura** (`textInkBalance`, 8 dp), che il glifo riempie
  solo col proprio margine (9–12 dp di scatola vuota in basso). In orizzontale non si
  incontrano comunque: il glifo a destra, il numero a sinistra. Sul 2×2 del device il
  glifo viene ~75 dp di scatola (~54 d'inchiostro contro i 64 del vicino) dove una pila
  semplice ne avrebbe lasciati 67.

### Decisioni

- **La frase è quella di Oggi, in registro breve.** `HeadlineText.of(…, brief = true)`:
  l'ombrello tiene la sua ora e perde la coda «schiarisce dopo le…», «la pioggia dovrebbe
  smettere verso le 17:00» diventa «pioggia fino alle 17:00 circa» (due stringhe nuove,
  IT/EN; le altre erano già brevi). Stesso fatto, stessa ora, stessa cautela; vive **nello
  stesso oggetto** della frase intera perché i due registri non possano mai disaccordarsi
  su quale frase merita una previsione. Il widget Oggi tiene la frase intera: ha la
  larghezza.
- **Il ripiego è lo stato del cielo, non il vuoto.** «Poco nuvoloso» è una cosa vera sul
  cielo di adesso — è quello per cui la card esiste — e uno slot che restasse bianco ogni
  giorno tranquillo si leggerebbe come una card che non ha finito di caricare. Non è il
  riempitivo che la frase di Oggi rifiuta: lì il vuoto sta sopra un canvas che già dice
  tutto, qui starebbe accanto a un numero nudo.
- **Il contenuto segue la dimensione: rovesciata la decisione del 4 set.** Allora la
  comparsa automatica dello stato al ridimensionamento era stata proposta e respinta
  («un widget che si riscrive mentre lo si dimensiona non si può mirare»). Vissuta accanto
  a un widget che lo fa, la regola opposta è quella che il lettore si aspetta: mostrare
  quello che ci sta alla dimensione data è la grammatica dei widget del launcher. Quindi
  l'interruttore «stato accanto alla temperatura» **esce** (campo, chiave, riga di
  configurazione, due stringhe; `forget` rimuove ancora la chiave orfana di un widget
  piazzato quando c'era).
- **Minima e massima escono dal widget Ora** e restano su Oggi, dove la riga di
  configurazione compare ora **solo** per quel widget. Sul widget Ora contendevano alla
  frase lo stesso bordo, e sono a un tocco di distanza nell'app.
- **Il glifo a due celle cede un po' d'altezza alle parole** (`nowRowIconSize`): a 159 dp
  un glifo alto quanto la riga lasciava al luogo ~65 dp, cioè «Dergan…». La colonna delle
  parole tiene 84 dp dove può («−12°» a 34 sp sono ~72, un nome di dieci lettere col pin
  ~84) e il glifo prende il resto fino al proprio pavimento: 56 dp su quella card, 68 su
  una griglia da 178. Da tre celle in su comanda l'altezza, come prima.
- **Le righe della frase sulla riga larga sono quante l'altezza ne tiene, massimo tre**
  (`nowSentenceLines`): due tagliavano «Pioggia per il resto della giornata» nella colonna
  più stretta che qualifica, e la riga l'altezza la ha (tre righe ≈56 dp contro i ~70 che
  la card lascia). Con la scala font del lettore il conto scende invece di sbordare. Sulla
  forma alta sono due, come il vicino: la terza costerebbe al glifo 19 dp e a 20 caratteri
  per riga due bastano al registro breve. Glance mette l'ellissi («…»), verificato nello
  stile `Glance.AppWidget.Text` delle risorse fuse.
- **Un numero per bordo, deciso da cosa ci sta contro.** Glifo: 4 dp (`Leading`, misura
  dell'8 set mattina, ora anche il bordo destro della forma alta) e 6 sopra/sotto
  (`Snug`, anche il bordo alto della forma alta). Parole: **14 dp** (`WidgetCardPadding`,
  l'inset di Oggi) su ogni bordo che toccano — il destro della riga, sinistro e basso
  della forma alta, tutti e quattro degli stati vuoti. Il bordo destro passa da 12 a 14: i
  12 erano quelli dell'altro widget da una cella quando su quel bordo stava al più la
  coppia opzionale; un blocco di frase allineato a destra è una cosa più grande da mettere
  contro un angolo (il vicino la tiene a ~25 dp su un raggio molto maggiore), e i due dp
  escono da una colonna che la coppia non deve più pagare.
- **Corpi**: temperatura 34 sp Medium (l'eroe tarato in cinque passate, su tutte le
  forme), frase 14 sp Medium in inchiostro pieno (la veste che la stessa frase ha sul
  widget Oggi: seconda cosa che si legge dopo il numero, e un blocco multiriga accanto a un
  34 sp vuole il corpo del testo, non uno di display), luogo 15 sp attenuato, marcatore di
  età 11 sp sotto il luogo su ogni forma (e nella forma alta il budget del glifo lo paga
  quando c'è).
- **Piazzamento predefinito a 4×1** (`targetCellWidth` da 3 a 4): è la forma a una riga
  che porta la frase e la misura a cui si apre il widget del vicino; il minimo resta due
  celle. L'anteprima del picker disegna la stessa forma con gli stessi inset.
- **Glance e le metriche**: `textLineHeight` (1,32 em, la scatola top-to-bottom di Roboto
  con `includeFontPadding`) è una stima dichiarata; i budget che ci poggiano tengono la
  banda di `textInkBalance` come gioco. A scala font 1,3 sul 2×2 il testo supera lo
  spazio, il pavimento di 52 dp tiene e il margine del glifo assorbe i pochi dp di
  sovrapposizione — verificato a mano sulla geometria, non su device.

### Rimasto aperto

- **Su device.** La soglia dei 150 dp per la forma alta e i 96 dp per la colonna della
  frase sono tarate sulle concessioni misurate qui e su una griglia a cinque colonne
  (320 dp per quattro celle, che qualifica per mezzo dp); un launcher che concede meno di
  ~316 dp a quattro celle mostrerebbe la forma stretta. Se succede, il numero da toccare
  è `SentenceColumnMin`, con la tabella dietro.
- **Il nome del luogo si tronca a metà colonna** sulla forma larga («Cavenago di Bri…»):
  è il costo della divisione a metà, scelto perché l'alternativa — la colonna del numero a
  misura — lasciava alla frase quello che il nome non prendeva, e un nome lungo la
  cancellava. Se disturba, la via è pesare 2:3 invece di 1:1, non misurare.

### Verifica

Suite `:app` verde (165 test, 8 nuovi in `NowWidgetLayoutTest`: la forma per ciascuna
delle concessioni misurate, le due larghezze di colonna, il glifo della riga vincolato
dall'altezza o dalla larghezza, le righe della frase alle due scale font, il glifo della
forma alta con e senza marcatore di età, pavimento e soffitto). `:app:lintDebug` a zero
errori; i due `RtlSymmetry` che l'anteprima nuova aveva introdotto sono chiusi sul posto.
APK di debug costruito; **la verifica su device è del committente**, alle quattro misure
dello screenshot più il 2×2 con un luogo lungo.

Trovato di passaggio, e corretto: `PaletteDocTest` era rosso **solo su Windows** —
`core.autocrlf` consegna `DESIGN.md` con CRLF e le regex delle rampe cercano
«```\n» letterale. Il test normalizza i fine riga prima di leggere; CI su Linux non lo
aveva mai visto.

---

## Ora: tre ritocchi sullo screenshot; Cielo: rifatto in tre forme (committente, 8 set 2026, sera)

Screenshot del device con le tre forme di «Ora» accanto al widget del launcher: «va
benissimo», più tre ritocchi da valutare («falli solo se sei convinto») e la richiesta di
rifare da capo il widget «Cielo» con la stessa logica di adattamento.

### I tre ritocchi, tutti applicati, con le misure che li reggono

| | Prima | Ora | Perché |
|---|---|---|---|
| Frase del giorno | 14 sp Medium | **16 sp** Medium | il vicino la scrive a ~17 sp sotto un numero da ~30; a 16 sotto il nostro 34 il rapporto è lo stesso. Con Roboto misurato: ogni frase breve tiene **due righe** nella colonna da 116 dp del 4×1 e da 131 del 2×2 |
| Luogo | 15 sp | **16 sp** | stesso corpo della frase, distinta da peso e inchiostro (Regular, attenuato) come fa il vicino. «Cavenago di Brianza» misura **145 dp** a 16 sp e la colonna del 3×1 ne ha 154: ci sta. Col pin GPS (16 + 4) non ci starebbe a nessuno dei due corpi, e a 4 celle (116 dp) si tronca con l'ellissi in entrambi i casi: limite dichiarato, non introdotto |
| Pin della posizione | 0,9 × testo | **1,0 × testo** | il disegno riempie 20/24 della scatola, quindi a 0,9 su 16 sp erano 12 dp d'inchiostro (l'altezza delle maiuscole e basta); il pin del vicino misura ~13,5 dp, maiuscola più discendente. A 1,0 sono 13,3 |

Il costo dei due corpi in più è pagato dal glifo della forma alta, che scende da ~75 a
~69 dp di scatola (`nowTallIconSize`), e da **due stringhe brevi in più**: a 16 sp
«Pioggia per il resto della giornata» era l'unica frase a chiedere tre righe sul 2×2, e il
registro breve la dice «del giorno» (l'inglese era già breve; le stringhe esistono in
entrambe le lingue perché il registro è uno). L'anteprima del picker segue i corpi nuovi.

### Cielo: le tre forme (`SkyWidgetLayout.kt`, puro, con tabella in `SkyWidgetLayoutTest`)

Il numero-eroe è **l'ora del momento** a 30 sp Medium, con sotto il nome a 15 sp
attenuato e davanti il glifo del momento che riempie l'altezza (tetto 72 dp). La domanda
della card è «quando, e vale la pena uscire»: il primo pezzo lo dice un orologio leggibile
a un braccio di distanza, dove un nome a 15 sp sopra un'ora a 12 non lo diceva; il secondo
lo dice il verdetto; quale momento sia lo dice il glifo, come il glifo di «Ora» dice che
tempo fa. Le due card sulla stessa home si leggono come sorelle.

- **Stretta** (tre celle, il minimo del widget): glifo · ora / **segno** + nome. Il segno è
  il glifo del verdetto — `✓ ~ ✗ ?`, il vocabolario della serie e gli stessi caratteri con
  cui si apre il chip dell'app — da solo in un tondo da 22 dp nei colori misurati del
  verdetto. È il verdetto alla misura che una card stretta può permettersi: una forma prima
  che un colore (DESIGN §2.3), quindi leggibile in deuteranopia, e la parola sta una forma
  più su o a un tocco.
- **Larga** (quattro celle e oltre): la stessa riga, e contro il bordo opposto la colonna
  del verdetto (96 dp fissi): il chip con la **parola** sopra e il **numero** che l'ha
  deciso sotto («nuvole 10%»). Fissa e non pesata perché un chip non va a capo: «Presto per
  dirlo» a 11 sp misura 89 dp con il suo padding, e 12 sp non ci starebbe.
- **Alta** (due righe o più): quella riga in testa, col glifo a 60 dp fissi, e sotto
  l'elenco dei momenti successivi, uno per riga — glifo piccolo, nome, ora col marcatore
  del giorno, verdetto: la parola su una card larga, il segno su una stretta, così una card
  parla un registro solo dall'alto in basso. Il budget (`skyRows`) è aritmetica sulle
  altezze vere: sul 3×2 e sul 4×2 del device tre righe; la lista non si imbottisce mai.

### Decisioni

- **La forma non dipende dal contenuto.** Larga o stretta lo decide la geometria
  (`skyIsWide`: la colonna delle parole tiene almeno 120 dp una volta pagata quella del
  verdetto), mai quale momento c'è: una card che cambiasse forma con la previsione non si
  potrebbe mirare — la regola già scritta per «Ora».
- **L'ora a 30 sp, non 34 come la temperatura.** Un'ora è più lunga di una temperatura:
  «12:05 AM» sono otto glifi contro i quattro di «−12°», e a 34 misura 149 dp contro i 136
  che il 4×1 lascia alle parole; a 30 ne misura 132. Cinque glifi a 30 portano circa
  l'inchiostro di tre a 34, quindi i due numeri affiancati hanno lo stesso peso ottico.
- **Il glifo di Cielo ha un tetto a 72 dp** dove quello di «Ora» arriva a 104: sul launcher
  che concede 101 dp a riga un'alba da 89 dp schiaccerebbe l'orologio e, sottraendo alla
  colonna delle parole, riporterebbe il 4×1 alla forma stretta. Sul device (82 dp) i due
  glifi sono uguali, 70.
- **Marcatore del giorno prima del nome** («Domani · Sorge la luna»), così quando la
  colonna finisce è la coda del nome a cadere con l'ellissi, mai la parola che dice quale
  giorno. Sui nomi lunghi con marcatore a tre e quattro celle l'ellissi c'è, misurata:
  «Domani · Sorge la luna» sono 150 dp a 15 sp contro 136 di colonna. Limite dichiarato.
- **Una finestra in corso mostra l'ora in cui finisce**, con «Adesso · Ora d'oro» sotto:
  «adesso, fino alle 20:20» è la prossima cosa che succede, e stampare come eroe un'ora
  già passata sarebbe strano. Le finestre non in corso stampano il solo inizio: «19:55 –
  20:20» a 30 sp sono 200 dp, e la chiusura è a un tocco.
- **Chip con la sola parola, segno col solo glifo.** Il chip dell'app scrive glifo e
  parola insieme; qui la parola da sola perché «✓ Presto per dirlo» sforerebbe la colonna
  da 96 di un dp. Il numero sta sotto il chip sull'eroe e a un tocco nelle righe.
- **Lo stato vuoto di Cielo** («Nessun momento del cielo in arrivo») era rimasto in alto a
  sinistra quando il 7 set gli altri due erano stati centrati: ora è centrato come loro.
- **Piazzamento predefinito a 4×1** anche per Cielo (da 3), la forma con parola e numero;
  il minimo resta tre celle (`minWidth` 180 dp). L'anteprima del picker è la forma larga.
- `SkyWidgetRowsTest` esce con il budget che fissava; `SkyWidgetLayoutTest` prende il suo
  posto con forme, glifi, colonne e righe.

### Rimasto aperto

- **Su device**: le tre forme di Cielo a 3×1, 4×1, 3×2 e 4×2, con un momento di domani e
  uno in corso; la resa del segno `✓ ~ ✗ ?` col font di sistema Samsung (nell'app gli
  stessi caratteri si vedono già, ma il widget passa da RemoteViews).
- **Righe compatte a 180 dp di larghezza** (una griglia da 90 dp per cella concede il 2×2):
  «Domani · 06:47» più il segno lasciano al nome pochi dp e l'ellissi lo mangia. Sotto i
  180 il widget non è piazzabile; sopra i 250 (tre celle del device) il nome ha ≥ 78 dp.
- **Le lettere lunghe dei crepuscoli** («Crepuscolo astronomico, mattina», 236 dp a 16 sp)
  si troncano su ogni forma a una riga: nomi da 31 caratteri non stanno in nessuna colonna
  da 136, e accorciarli è una decisione di copy della schermata, non del widget.

### Verifica

Suite `:app` verde (167 test: 7 in `SkyWidgetLayoutTest` al posto dei 5 di
`SkyWidgetRowsTest`, gli 8 di `NowWidgetLayoutTest` aggiornati ai corpi nuovi).
`:app:lintDebug` a zero errori, 67 avvisi come prima (l'anteprima nuova di Cielo non ne
aggiunge). Le larghezze citate sopra sono misurate con Roboto Regular dal layoutlib di
Android Studio (Medium stimato a +3%); il font di sistema del device è un altro, e i
margini tenuti sono di quell'ordine. APK di debug costruito; **la verifica su device è del
committente**.

---

## Interruttore per la frase, la disposizione al contrario, Oggi rifatto, i nomi (committente, 8 set 2026, pomeriggio)

«Entrambi i widget sono ok.» Quattro richieste: la frase visibile o no dalle impostazioni
di Ora; una disposizione alternativa per Ora su una riga («l'icona a destra, e nella prima
riga il testo che lì è nella seconda» — il 4×2 schiacciato in 4×1); una revisione completa
di Oggi, con lo stesso interruttore se ci sta; nomi nuovi nel picker, senza «Chiaro ·».

### La frase, un interruttore (Ora e Oggi)

`WidgetLook.showSentence`, **acceso di default**: lo slot esiste per quello, e chi vuole
il numero nudo lo spegne per singolo widget. Decide solo il **contenuto**: se c'è
**spazio** lo decide ancora la concessione (`NowLayout`, `todayIsWide`), quindi
l'interruttore può togliere la frase, mai forzarla su una card troppo stretta. È il
contrario, un giorno dopo, dell'interruttore «stato accanto alla temperatura» uscito la
mattina: quello era spento di default e decideva il layout. Con la frase spenta la forma
alta di Ora dà le sue due righe al glifo (`nowTallIconSize(withSentence = false)`: 104 dp
sul 2×2, il soffitto).

### La disposizione al contrario (`WidgetArrangement`, solo Ora)

Due valori, per widget: `ICON_START` (la grammatica del launcher, default) e `ICON_END`:
il glifo nell'angolo di coda, e sul lato d'attacco il numero con la frase **alla sua
spalla** — due righe al massimo, centrate sull'altezza del numero, come la descrizione del
vicino sta accanto al suo — e il luogo sotto entrambi. È la composizione della card alta
premuta in una riga, per chi vuole le due sulla stessa home senza che litighino. I due
inset si scambiano di bordo e restano gli stessi numeri: 14 alle parole, 4 al glifo. La
frase compare quando il suo spazio (`nowMirroredSentenceWidth`: la riga meno bordi, glifo,
gap e la colonna del numero, 66 dp) supera lo stesso minimo della colonna standard, 96:
166 dp sul 4×1 del device, 146 su una griglia a cinque colonne, 76 su tre celle — dove
resta a casa, esattamente come dall'altro verso. La card alta ignora la scelta: ha già il
glifo a destra.

### Oggi, rifatto sulla grammatica di Ora (`TodayWidgetLayout.kt`, puro, con tabella)

«Oggi = adesso più le prossime ore» è la definizione di VISION §5.9, e ora la card lo dice
con una grammatica sola: **la testa è la riga larga di Ora** — glifo che riempie la fascia,
temperatura (34 sp, era 36) sopra il luogo, frase contro il bordo opposto, minima e massima
**sotto la frase** quando richieste — e sotto la striscia delle ore.

- **La frase lascia la sua riga** sotto l'eroe e va al bordo opposto, dove Ora la stampa e
  dove chi ha le due card sulla stessa home la cerca già. La riga restituita va al glifo:
  sul 4×2 del device (~340 × 189) passa da **~45 a ~76 dp**, la misura che Ora disegna
  accanto. Registro breve anche qui: la colonna è la stessa di Ora (~113 dp).
- **Il budget** (`todayHeroIconSize`): altezza meno 6 sopra, 14 sotto, la striscia
  (84,84 dp con la riga della pioggia, 70,32 senza) e 8 di stacco; le parole dell'eroe
  (`todayHeroTextHeight`: la colonna più alta fra sinistra — temperatura, luogo, età,
  banda di `textInkBalance` — e destra — frase su due righe, escursione) sono 74 dp e
  stanno nella fascia.
- **La riga della pioggia deve anche starci** (`todayShowRain`), oltre ad avere qualcosa
  da dire: sul 4×2 ci sta (86,8 dp di spazio contro 84,8), ma un marcatore di età prende la
  terza riga alle parole e lo spazio scende a 72: allora la riga resta a casa invece di
  essere tagliata al bordo — la sezione che non ci sta non si disegna.
- **Stretta** (250 dp, il minimo del provider): niente colonna a destra, quindi né frase né
  escursione — la coppia aveva lasciato il posto sotto il numero alla terza passata proprio
  perché lì affollava. La nota dell'interruttore dice ora «al bordo opposto, sotto la
  frase».
- Inset come Ora: 4 al glifo (attacco e alto), 14 alle parole (coda e basso); la striscia
  paga da sé i 10 dp che la portano all'inset delle parole. Anteprima del picker rifatta
  sulla stessa struttura (il vuoto fra riga e striscia è un `FrameLayout`: `Space` non è
  fra le view che RemoteViews gonfia).

### I nomi

| | Prima | Ora |
|---|---|---|
| Ora | «Chiaro · Ora» / «Chiaro · Now» | **«Colpo d'occhio»** / **«At a glance»** — il glance nel senso vecchio della parola, che è la definizione del widget in VISION |
| Oggi | «Chiaro · Oggi» / «Chiaro · Today» | **«Le prossime ore»** / **«The hours ahead»** — quello che la striscia aggiunge all'adesso |
| Cielo | «Chiaro · Cielo» / «Chiaro · Sky» | **«Momenti del cielo»** / **«Sky moments»** — la sezione della schermata Cielo, che è la lista che il widget legge |

Il nome dell'app sta già sopra la lista nel picker, quindi ripeterlo davanti a ognuno non
diceva niente. La guida in app usa gli stessi nomi.

### Decisioni

- **Un solo registro per la frase su tutti i widget**: breve. Oggi stampava la frase intera
  su una riga tutta sua; nella colonna di 113 dp la frase intera («Ombrello verso le
  17:00, schiarisce dopo le 19:00», 330 dp a 16 sp) chiederebbe tre righe. La frase intera è
  nella schermata.
- **`fontScale`, `sentence` e `sentenceStyle`** escono da `NowWidget.kt` come `internal`
  e li usano anche Oggi (tutti e tre) e Cielo (`fontScale`): una frase, un vestito, una
  scala.
- **`DayRange` a 16 sp** (era 15), il corpo del luogo: stesso ordine di fatto, stesso corpo.

### Rimasto aperto

- **Su device**: Ora al contrario a 3×1 e 4×1 (frase alla spalla del numero su due righe,
  luogo sotto); Oggi a 3×2 e 4×2 con e senza pioggia, con l'escursione accesa; la frase
  spenta su entrambi; i tre nomi nel picker.
- **Oggi a 250 dp** perde frase ed escursione insieme: se il committente le vuole anche lì,
  la via è la frase sotto l'eroe a tutta larghezza come prima, pagata dal glifo.

### Verifica

Suite `:app` verde (175 test: 6 nuovi in `TodayWidgetLayoutTest`, 2 in
`NowWidgetLayoutTest` per il glifo senza frase e la disposizione al contrario).
`:app:lintDebug` a zero errori e 60 avvisi, sette meno di prima: l'anteprima di Oggi
rifatta chiude i suoi `RtlSymmetry`. APK di debug costruito; **la verifica su device è
del committente**.

---

## La revisione di Oggi: il terzo gradino delle icone, l'eroe che cresce, lo scroll (committente, 8 set 2026, sera)

Richiesta: una review della schermata principale — layout, disposizione delle sezioni,
icone del meteo «leggermente più grandi ma senza aumentare le spaziature» — e lo scroll
della pagina e della striscia oraria, «leggermente scattoso» da quando le icone si
muovono, da migliorare **senza toccare le animazioni**. Poi: applicare tutto, correggere
la documentazione dove sbagliava, e dire se il bordo del canvas sta meglio dritto o
arrotondato. Nessun device collegato: tutto quello che segue è misurato nel codice, nei
sorgenti di piattaforma e nei file delle icone, e la verifica sul telefono è del
committente.

### Le misure che hanno deciso

- **L'inchiostro nel riquadro.** Prima di ingrandire le icone ho misurato quanto del
  viewport 64×64 dei Meteocons occupa il disegno, statico e lungo tutto il loop
  dell'animazione (campionando i path, stroke incluso, e allargando ogni gruppo animato
  dell'escursione dei suoi keyframe di traslazione; la rotazione è limitata dal punto
  più lontano dal pivot). Le 18 icone della condizione usano il riquadro quando si
  muovono: unione x **6.0–61.6**, y **8.0–60.0**. Le gocce cadono fino a y 60, il banco
  dell'overcast arriva a x 61.6, il sole parte da y 8. Un ritaglio uniforme del viewport
  potrebbe prendere 2.4 unità (sotto il 4%) e sposterebbe i centri ottici: **no**,
  misurato. La nuvola ferma è quello che fa sembrare piccola la famiglia: 30 unità di
  altezza in un riquadro da 64, cioè 20dp di disegno in un'icona da 42.
- **L'eroe a testo grande.** Il canvas era 280dp fissi più status bar; dentro, tutto in
  sp. Spazio libero a 100% di testo: 30dp con la frase su una riga, **2dp su due**,
  −26dp su tre. La frase più lunga in italiano fa 49 caratteri con le due ore e su 360dp
  va su due righe: la pagina era al limite già a 100%, e a 115% eroe e riga del luogo si
  sovrapponevano di 30dp.
- **Dove va il tempo dello scroll**, verificato nei sorgenti e non supposto:
  - AOSP `AnimatedVectorDrawable`: il costruttore privato copia già in profondità
    l'albero del vettore per ogni istanza (`AnimatedVectorDrawableState(copy, …)` fa
    `newDrawable` + `mutate` sul `VectorDrawable` interno), quindi il `mutate()` in
    `ConditionIcon` era una **seconda** copia buttata via; l'animatore è
    `VectorDrawableAnimatorRT` e gira sul RenderThread finché il canvas è accelerato;
    `setVisible(false)` mette in pausa i loop infiniti.
  - Compose `AndroidViewHolder`: `onGloballyPositioned` richiama `View.layout()` a ogni
    spostamento, cioè a ogni frame di scroll, per ogni icona interop.
    `LazyListMeasuredItem` piazza ogni item su un layer, quindi senza interop uno scroll
    sposta i layer senza ridisegnarli.
  - Compose `animation-graphics`: il parser legge `repeatCount="infinite"` e costruisce
    un `repeatable` infinito. **La motivazione scritta il 7 set era sbagliata**: non è
    che `AnimatedImageVector` non sappia fare un loop, è che lo fa ricomponendo il
    vettore a ogni frame e rasterizzando su CPU, sul thread che serve allo scroll. La
    scelta dell'`ImageView` resta giusta per questo motivo, ed è quello che DESIGN §7.1
    e il KDoc dicono adesso.

### Decisioni

1. **Icone: terzo gradino, +4dp su ogni piolo** — striscia **42**, settimana **38**,
   timeline **34**, tile **34**. Nessuna spaziatura, nessuna colonna, nessun
   arrangement cambia. Cosa paga ciascuno, a 360dp: la striscia **niente** (l'icona si
   prende i 2dp di padding verticale in cui stava, la cella resta 112dp e l'aria laterale
   passa da 9 a 7dp); la settimana 4dp di altezza per riga e la barra 106→102dp; la
   timeline 4dp per riga e la prosa 226→222dp; il tile 4dp di testata e il budget
   dell'etichetta 88→84dp contro i 76.7 di «Qualità aria» — 7.3dp di margine, **fine
   della scala** per quel piolo. Scartata la scala a tempo di disegno (`scale(1.1)`
   nello stesso riquadro): per la striscia è la stessa cosa scritta peggio, per le altre
   righe fa sbordare l'inchiostro di 1–2dp nei gap.
2. **Il canvas è un pavimento, non una misura**: almeno 280dp più status bar, e cresce
   col testo. Riga del luogo ed eroe sono i due capi di una `Column` con `SpaceBetween`
   su `heightIn(min)`, non due figli allineati ai bordi opposti di una scatola fissa. Il
   flip delle icone della status bar si misura sull'altezza reale dell'item del canvas
   (`layoutInfo`), perché la fascia di scrim è una frazione e la frazione ora si muove.
3. **Bordo dritto, confermato.** Il committente ha chiesto un parere: gli angoli
   arrotondati facevano leggere il cielo come una card che galleggia sullo scroll, ed è
   il motivo per cui erano stati tolti il 4 set. Sulla pagina ogni altra superficie è
   rientrata di 16dp e arrotondata; l'unica che non lo è dev'essere il terreno su cui la
   pagina si apre, non un'altra card. Dritto.
4. **Skeleton rimisurato sul layout vero**: il blocco del cielo aveva ancora gli angoli
   da 28dp persi il 4 set e i 240dp di prima che il canvas arrivasse alla status bar; le
   celle erano 96dp contro i 112 della striscia. Ora il blocco è `280 − (8 + 48 + 8)` =
   216dp a bordo dritto, così riga del luogo più blocco finiscono dove finisce il canvas
   vero; sei celle da 56×112 a 4dp l'una dall'altra.
5. **La riga del luogo a 8dp ovunque**: era 8 sul cielo e 12 sugli stati piatti, quindi
   sfogliando da un luogo con report a uno senza il titolo saltava di 4dp.
6. **La striscia scorre a filo schermo**: il margine di pagina entra come
   `contentPadding` della `LazyRow` invece di stare sulla colonna che la contiene, così
   la prima cella parte sui 16dp come tutto il resto e le altre scivolano sotto il bordo
   dello schermo invece di essere tagliate su una linea 16dp più dentro, che faceva
   leggere la striscia come una scatola. Vale anche per la striscia del giorno aperto.
7. **Lo scroll, quattro interventi che non toccano un disegno né un animatore:**
   - via il `mutate()` (una copia dell'albero per cella invece di due);
   - la cella riciclata **tiene il drawable**: `onReset` ferma solo il loop, `update` lo
     riavvia se la risorsa è la stessa, `onRelease` lo rilascia. Vive in una
     `MovingIconView` con il proprio flag `resting`, perché il RenderThread riferisce la
     fine del loop al thread UI uno o due frame dopo lo `stop()`, e un riuso arrivato in
     quella finestra avrebbe trovato `isRunning` vero e lasciato l'icona ferma;
   - le celle hanno una **chiave** (`HourCell.key`, l'ora): allo scoccare dell'ora esce
     solo la prima cella, invece di rigonfiare tutte le icone visibili nello stesso
     frame;
   - la settimana è **sette item** invece di uno: stesso ritmo (la colonna spaziava di
     12dp e la lista spazia di 12dp), ma il prefetch prende una riga alla volta invece
     di sette icone nel frame in cui la sezione entra. Lo stato del giorno aperto è
     salito in `ContentState`.
   Quello che resta è l'animazione stessa: ~14 cache vettoriali rirasterizzate sul
   RenderThread a ogni vsync. È il prezzo della funzione e l'unica leva è quante icone si
   muovono insieme; si misura con un A/B, `adb shell settings put global
   animator_duration_scale 0` spegne le icone (già leggono quel valore) e `dumpsys
   gfxinfo` confronta i frame persi.
8. **Dettagli in fondo**, per decisione del committente. La revisione grafica delle card
   è un parere dato a parte, non un intervento.

### Rimasto aperto (su device)

- Striscia e pagina: gli scatti prima e dopo, con l'A/B sopra.
- L'eroe a 130% di testo e con una frase su due righe: la riga del luogo non deve più
  essere raggiunta.
- Skeleton contro canvas vero: nessun salto quando arriva il report.
- La settimana: aprire e chiudere un giorno ora che ogni riga è un item, e il ritmo dei
  12dp tra le righe.
- Le icone a 42/38/34: se «leggermente più grandi» è questo.

### Verifica

Suite `:app` verde (175 test, invariati: nessuna regola pura nuova, i cambi sono di
layout e di interop). `:app:lintDebug` a zero errori e 60 avvisi, come prima. APK di
debug costruito; **la verifica su device è del committente**.

---

## Le card dei dettagli rifatte, e la review di Cielo (committente, 8 set 2026, notte)

Richiesta, subito dopo: applicare tutti i consigli dati sulle card dei dettagli; fare la
stessa review a **Cielo** — layout, dimensione delle icone «o comunque vedi tu», logica,
dati e messaggi — e pushare su un branch nuovo perché parta la CI.

### Le card, com'erano e come sono

Il problema era di gerarchia: valore a 16sp contro un'etichetta a 14 e un'icona da 34dp,
l'occhio andava sull'icona e non sulla lettura. Da qui:

- **Il valore è una lettura**: `ReadingValue` in `Type.kt`, Inter Light 24sp su 32 di
  interlinea, tabulare — la voce dell'eroe alla scala di una card, con lo stesso
  argomento del 64sp: un numero in peso da corpo legge come un titolo, non come una
  lettura. Costa 8dp per riga di card.
- **Una traccia dove la scala è del mondo** (`QuantityTrack`): 4dp, `outlineVariant`
  con il tratto in `primary` fino al valore, mai più stretto di quanto è alto. Solo per
  UV (0–11: sopra il tracciato è pieno e parla la frase), Umidità (0–100) e Qualità aria
  (0–300, la soglia «pericolosa» dell'AQI statunitense che il provider serve). Niente per
  pressione e visibilità: una è una banda stretta intorno a 1013, l'altra è logaritmica,
  e una traccia lì sarebbe una forma senza niente da dire.
- **Vento**: il valore è la sola velocità. Sotto, una freccia disegnata (`WindArrow`,
  non un'icona Material: l'unica freccia del set base è auto-specchiata in RTL e una
  direzione della bussola non deve esserlo) che indica **dove va l'aria**, e le parole
  «da nord-est» per **da dove viene** — le stesse otto parole che Cielo usa per un
  arcobaleno (`SkyText.bearingRes`), perché un pubblico generale legge «da nord-est» e
  decodifica «NNE». La convenzione è quella delle mappe: la freccia è movimento, la
  didascalia è la sorgente. La sigla a sedici punti del modello, che in italiano
  stampava «W», resta nei dati. **Le raffiche** hanno una riga quando contano
  (`WeatherText.gustsMaterial`: almeno 25 km/h e almeno una volta e mezza il vento) e in
  quei giorni la frase si legge sulla raffica, perché «si sente, non dà fastidio» sopra
  «raffiche a 45 km/h» è la card che litiga con sé stessa.
- **Umidità e Rugiada sono una card.** Dicevano la stessa cosa una sotto l'altra
  («Confortevole» / «Gradevole»), e «Rugiada» è la parola meno capita dello schermo. La
  card Umidità tiene la percentuale e la traccia, prende la frase dal punto di rugiada
  (il predittore migliore di come si sta) e stampa «Rugiada 12°» come nota: il dato non
  si perde, il titolo sì. Via `metric_dew` e le quattro `humidity_meaning_*`; il campione
  della Guida usa la frase della rugiada.
- **Qualità aria** senza «AQI»: era l'unica sigla di uno schermo fatto per non averne, e
  l'etichetta dice già cos'è il numero. Via `metric_air_value`.
- **Pollini**: la nota dice **quali** («Graminacee e alberi»), dalle tre famiglie del
  modello al livello peggiore, in ordine di catalogo; niente riga quando sono assenti.
  Le famiglie stanno in risorsa minuscole per entrare in un elenco (`list_two`,
  `list_three`, localizzati) e la card mette la maiuscola alla prima.

**Le larghezze, misurate a 360dp**: l'interno di una card è `(360 − 32 − 12) / 2 − 32` =
126dp. «↙ da nord-ovest» ≈ 120dp, sta. «Raffiche a 45 km/h» ≈ 131dp può andare a capo a
360 e sta su una riga da 393dp in su (142dp), che è la larghezza dei telefoni di oggi;
«Raffiche fino a» era scartata per questo. «Graminacee, alberi e erbe» va a capo, ed è il
caso raro in cui tutte e tre le famiglie sono allo stesso livello. Una card più alta
allarga la sua compagna di riga, com'era già (`IntrinsicSize.Max`).

### Cielo: la review

**Layout.** La struttura regge — l'eroe, i momenti, l'agenda, i promemoria — e il chip
del verdetto sotto il nome (device, 3 set) resta la scelta giusta. Due cose cambiate:

- **Le icone erano silhouette**: 26dp tinte `onSurfaceVariant` nei momenti e negli
  eventi, nell'indice della guida e nella testata della pagina (36dp), 18dp con la tinta
  del chip nei correlati. Era l'ultimo posto in cui la famiglia era tinta piatta, e
  tinta piatta la luna piena e la luna nuova sono lo stesso disco, alba e tramonto lo
  stesso orizzonte. Ora colori propri (§13.1) e la scala di Oggi: **34dp** (il piolo
  della timeline) nelle righe di momenti, eventi e indice, **42dp** (il piolo della
  striscia) come unico glifo della pagina di un evento, **24dp** nei chip correlati, dove
  a 18 un Meteocon erano 13dp di inchiostro.
- **I segni di spunta del catalogo** venivano ricostruiti dalle righe a schermo
  (momenti più eventi con campanella): un job iscritto senza riga — una ricerca di
  eclissi che non trova niente davanti — risultava non iscritto e si offriva di essere
  aggiunto di nuovo. `Content.subscribedIds` arriva dallo store.

**Logica e messaggi, verificati e lasciati com'erano:**

- La card Stanotte stampa la riga della luna al posto del numero delle nuvole solo
  quando `moonPct` c'è, e il motore lo mette solo quando la luna ha deciso il verdetto
  (`SkyVerdictNote.MOONLIGHT`): la riga della luna **è** l'aritmetica, non la nasconde.
- «Domani» sui momenti giornalieri è sempre esatto: `SkyScheduler.next` riporta i giorni
  `∅` invece di saltarli, quindi il prossimo è sempre quello di domani, con o senza
  motivo.
- Il formato degli eventi («12 agosto · La previsione non arriva ancora così lontano»)
  va a capo sotto il nome e legge bene; il giorno della settimana non serve a mesi di
  distanza.
- Cielo non aggiunge l'inset della barra di navigazione in fondo alla lista, Oggi sì:
  la `NavigationBar` sta sotto entrambe e consuma l'inset da sé, quindi Oggi ha 24–48dp
  di respiro in più prima della barra. Lasciato: non è un errore in nessuno dei due.

**Trovato e non corretto, perché sta nel core** (UPSTREAM: si corregge a monte):
`SkyScheduler.darkness()` risponde `NO_DARKNESS` anche quando il sole resta sotto i −18°
per tutto il giorno — notte polare, crepuscolo astronomico senza inizio né fine — e la
card Stanotte direbbe «il cielo non diventa mai del tutto buio» a Longyearbyen in
dicembre, che è il contrario. Serve un motivo distinto («buio tutto il giorno») nel
motore e la card che lo stampa. Fuori dalle latitudini del pubblico dell'app; registrato.

### Verifica

Suite `:app` verde (175 test). `:app:lintDebug` a zero errori e 60 avvisi, come prima.
APK di debug costruito; il branch `claude/today-details-sky-review-k4p7wz` porta tutto
alla CI; **la verifica su device è del committente**: le card con e senza raffiche, la
striscia a filo, l'eroe a testo grande, Cielo con le icone a colori.

---

## La notte polare corretta, e la review di Avvisi e Diario (committente, 8 set 2026, tarda sera)

APK della CI provato: «ottimo». Richiesta: correggere la notte polare trovata nella
review di Cielo; poi la stessa passata, funzionale ed estetica, sulle ultime due
schermate — Avvisi e Diario — con particolare attenzione al funzionamento del Diario e
alle correzioni da proporre.

### La notte polare: corretta nel core, e registrata in UPSTREAM

`SkyScheduler.darkness()` rispondeva `NO_DARKNESS` per due cieli opposti: il sole che non
scende mai 18° sotto l'orizzonte (la notte bianca) e il sole che non **risale** mai fino a
18° sotto (la notte polare profonda, dove è buio a mezzogiorno). La card Stanotte diceva
«il cielo non diventa mai del tutto buio» anche sul secondo, che è il contrario. Prima di
correggere, il conto di dove capita: il sole resta sotto i −18° tutto il giorno solo se a
mezzogiorno solare `90 − |φ − δ| < −18`, cioè oltre **84,6°** di latitudine al solstizio
— nessun paese abitato, le stazioni polari sì. Corretto comunque, perché è il motore che
chiama una notte polare con il nome di una notte bianca:

- nuovo motivo `SkyNotScheduled.DARK_ALL_DAY`; `darkness()` lo restituisce quando il
  giorno è senza sole (`sunDownAllDay`) **e** l'altezza del sole al mezzogiorno solare è
  sotto il crepuscolo astronomico. Il giorno di confine con un solo estremo nullo resta
  `NO_DARKNESS`, che è la risposta giusta anche lì;
- la Via Lattea propaga il motivo della finestra invece di riscriverlo `NO_DARKNESS`;
- `Tonight.reason` porta il motivo alla card, che stampa la frase giusta
  (`sky_tonight_dark_all_day`); le righe `∅` hanno la loro (`sky_none_dark_all_day`);
- `SkySchedulerTest`: a −89,5° al solstizio di giugno la finestra è `DARK_ALL_DAY`,
  Copenaghen lo stesso giorno resta `NO_DARKNESS`;
- **UPSTREAM.md**: è un bug anche a monte — `sky.crontab` stampa lo stesso `∅` — e la
  voce dice che la correzione va portata di là (motivo, ramo, test).

### Avvisi: la review

**Funziona.** Interruttori pronti con la frase di cosa mandano e quando; regole del
lettore come frase di chip con picker e mai un campo libero per un valore con un
intervallo; «Prova adesso» che risponde senza notificare e si azzera quando una
condizione cambia; permesso chiesto al primo interruttore acceso; cancellazione
confermata; il template crea una regola vera e apre subito l'editor. Tutto coerente con
VISION §5.4. Da correggere o migliorare, in ordine:

1. **«Scattato l'ultima volta» è nel fuso del telefono** (`ZoneId.systemDefault()`),
   l'unica ora dell'app che non è del luogo. Per il proprio luogo coincidono; per
   Palermo letta da Reykjavík no. Portare `zone` in `AlertsUiState.Content`.
2. **Il picker degli operatori offre «uguale a» e «diverso da» sulle grandezze
   continue**: «temperatura uguale a 20°» non scatta quasi mai ed è la soglia senza
   senso che i picker esistono per rendere non scrivibile. Nascondere i due per
   `NUMBER`, `TEMPERATURE`, `SPEED`; restano per i sì/no.
3. **Un template si può aggiungere due volte** e produce due regole «Bici» identiche.
   Un template le cui condizioni esistono già tra le regole va segnato come aggiunto
   (o nascosto), come fa il catalogo di Cielo con la spunta.
4. **Al massimo delle regole i template spariscono senza una parola**: una riga «Hai già
   il massimo di N avvisi: togline uno per aggiungerne un altro» dove stavano.
5. **Estetica**: le regole del lettore sono `ListItem` identici agli interruttori pronti,
   e il titolo di gruppo è l'unica cosa che le distingue. VISION le chiama *card*: una
   `Surface` `surfaceContainer` come le card dei dettagli — nome, frase, ultimo scatto,
   interruttore — separa i due gruppi a colpo d'occhio e dice che la card si apre, cosa
   che una riga con interruttore non dice.
6. **La risposta di «Prova adesso»** è tutta in `onSurfaceVariant`: quando scatterebbe è
   la risposta che il lettore cercava e merita l'inchiostro pieno; «resterebbe zitto»
   può restare quieto.

### Diario: la review

**Verificato leggendo il costruttore e i due motori** (`ForecastDiff`, `ForecastOutcome`):
le revisioni diventano righe solo con una baseline (un giorno che entra nell'orizzonte
non è una revisione); il giudizio lo dà la pioggia e la temperatura resta neutra; l'esito
di una giornata finita si dichiara «ha piovuto» con una sola osservazione bagnata e «non
ha piovuto» solo con 16 ore coperte; la deriva ha l'asse del tempo a fasce di 6 ore, il
buco resta buco, la testata parte dove parte l'evidenza e il gelo segna il giorno
sull'ultima previsione; i giorni si raggruppano nel fuso del luogo e la schermata si
sveglia solo alla sua mezzanotte. Nessun errore di logica trovato. Le proposte, in
ordine di resa:

1. **Comprimere le revisioni per giorno bersaglio.** Ogni fetch che sposta la massima di
   un giorno di 1 °C o la pioggia di 10 punti fa una riga; a cadenza oraria e su sette
   giorni bersaglio questo può essere una decina o più di righe al giorno dello stesso
   tenore («la massima di venerdì è passata da 24° a 25°»), e il diario smette di essere
   leggibile. Proposta: nel costruttore, per ogni coppia (giorno del diario, giorno
   bersaglio) una riga sola con primo → ultimo valore e «in N aggiornamenti», e nessuna
   riga quando il valore è tornato dov'era — la frase della deriva già ragiona così
   («è andato su e giù»), la prosa no. Il Diario di Oggi («Cosa è cambiato») resta
   sull'ultimo fetch com'è.
2. **Oggi nella deriva.** Le righe sono i giorni «ancora avanti» e oggi è escluso, ma «la
   pioggia di oggi è salita nelle ultime ore?» è la domanda del mattino. Includere oggi
   come prima riga finché la giornata corre.
3. **La copertura dell'esito.** Ogni osservazione copre l'ora precedente, fissa: a
   cadenza di 2 ore (il massimo dell'intervallo) una giornata copre al più 12 ore e il
   verdetto «non ha piovuto» non arriva **mai**; a cadenza oraria basta una notte in
   Doze per scendere sotto le 16. Proposta, in `ForecastOutcome`: ogni osservazione copre
   il tempo dal fetch precedente, con un tetto (3 ore), così la copertura misura quanto
   l'app ha davvero guardato e non quante volte. Da verificare su device prima: se «Ieri»
   riceve la sua riga di esito alla cadenza del committente.
4. **La tabella dei numeri** unisce quattordici valori con frecce in una riga di dialogo
   che va a capo male. Una griglia con l'ora della fascia in testa, o le sole fasce con
   un valore.
5. **Le icone delle righe**: nuvola e stella sono Meteocons tinti in grigio a 24dp, la
   stessa silhouette tolta a Cielo. Qui però il glifo è la **categoria** della riga, non
   il tempo: la scelta coerente è un set Material monocromo per tutte e cinque le
   categorie (revisione, cielo osservato, avviso scattato, esito, aggiornamento
   mancato), come campanella, spunta e triangolo già sono. La regola §13.1 riguarda le
   icone del meteo, e queste non lo sarebbero più.
6. **L'ora come etichetta in coda** alla riga (`trailingContent`, `labelSmall`) invece
   che «· alle 21:54» in fondo alla frase: un registro si scorre per ora.
7. **La deriva in una card** `surfaceContainer` con i chip dentro: cinque elementi
   impilati (titolo, chip, striscia, gelo, frase) diventano un oggetto.

Nessuna di queste è implementata: sono proposte, con il numero 1 e il numero 3 come le
due che cambiano quello che il Diario dice.

### Verifica

`:core:domain` verde (166 test, uno nuovo), `:core:data` verde (171), `:app` verde (175).
`:app:lintDebug` a zero errori e 60 avvisi. APK di debug costruito. La correzione è nel
working tree del branch della CI, **non ancora pushata**: la CI riparte quando il
committente lo dice.

---

## Avvisi e Diario: le proposte applicate (committente, 9 set 2026, notte)

Richiesta: applicare tutte le correzioni e le proposte della review di Avvisi e Diario;
sullo scatto residuo dello scroll di Oggi solo una risposta, senza codice; poi push
perché riparta la CI.

### Avvisi

1. **«Scattato l'ultima volta» nel fuso del luogo**: `AlertsUiState.Content.zone`, letto
   dalla città attiva con il fallback del telefono, e la card lo usa.
2. **Niente «uguale a» / «diverso da» sulle grandezze continue**: il picker li offre
   solo ai sì/no. Una regola che già li porta li tiene; il picker smette di proporli.
3. **Un template già presente è segnato** con la spunta in `primary` e non è più
   toccabile: il confronto è sulle condizioni (`RuleCondition` è una data class), così
   vale anche se il lettore ha rinominato la regola. Stringa `tpl_already_added`.
4. **Al massimo delle regole** una riga lo dice (`alerts_max_reached`, con `MaxRules`),
   dove prima i template sparivano e basta.
5. **Le regole sono card**: `Surface` `surfaceContainer` con angolo `medium`, nome in
   `titleMedium`, frase, ultimo scatto in `bodySmall` grigio, interruttore a destra;
   tutta la card apre l'editor, l'interruttore resta il suo bersaglio. Item con chiave
   sull'id della regola, così un toggle anima la riga invece di ricostruirla.
6. **La risposta di «Prova adesso»** è in `onSurface` quando scatterebbe, quieta negli
   altri tre casi.

### Diario

1. **Le revisioni si piegano** per coppia (giorno del diario, giorno bersaglio):
   `JournalStateBuilder.forecastShifts(rows, zone)` raggruppa le revisioni per fetch
   (`revisions`, che resta quello che legge «Cosa è cambiato» su Oggi) e per ogni campo
   tiene il primo valore vecchio e l'ultimo nuovo; un campo tornato dov'era non è un
   cambiamento (confronto numerico dove i valori sono numeri), un giorno i cui campi
   sono tutti tornati non ha riga. `ForecastShift.revisions` dice quante ne raccoglie e
   la riga lo stampa oltre uno (`journal_shift_revisions`, plurale). Due test nuovi in
   `JournalStateBuilderTest`: la piega su tre fetch, e il ritorno che cancella.
2. **Oggi nella deriva.** Le righe sono `!isBefore(today)`. Ma il giorno corrente non
   era su disco: `WeatherSnapshots.flattenForecast` salvava da domani a sette giorni,
   ora salva **da oggi** (`0L..7L`). È una divergenza in più dal core di tweather ed è
   in UPSTREAM con la sua cucitura: `ForecastDiff.dayLabel` chiama «tomorrow» la prima
   data salvata, e quell'etichetta è dei Logs di tweather, niente qui la legge.
   `WeatherSnapshotsTest` aggiornato. Effetto collaterale voluto: anche «Cosa è
   cambiato» su Oggi può ora dire che la previsione di oggi si è mossa.
3. **La copertura dell'esito** (`ForecastOutcome`): una lettura copre il tempo dalla
   lettura precedente, con un tetto di **due ore** — la cadenza massima dell'app — e la
   prima lettura copre la sua ora. Prima ogni lettura copriva un'ora fissa, quindi a
   cadenza di due ore una giornata guardata da cima a fondo copriva dodici ore e il
   verdetto «non ha piovuto» non arrivava mai. La pioggia resta sull'ora che i
   millimetri descrivono, non sulla finestra di copertura: una lettura bagnata all'1:30
   che copre fino a ieri non bagna ieri. Tre test nuovi in `ForecastOutcomeTest` e uno
   adattato (tre letture a 9, 10 e 16: quattro ore coperte, non tre). Il tetto è una
   scelta: due ore è quanto l'app stessa promette di guardare, sei ore di sonno ne
   coprono due. In UPSTREAM.
4. **La tabella dei numeri è una griglia**: intestazione a due righe (giorno e ora
   dello slot, del fetch se c'è, nominale se lo slot è vuoto), una riga per giorno
   bersaglio, cifre tabulari, scorrimento laterale quando è più larga del dialogo.
5. **Icone monocrome Material** per le cinque categorie: matita per la revisione,
   stella per il cielo osservato, campanella, spunta, triangolo. Nuvola e stella
   Meteocons tinte grigie erano l'ultima silhouette rimasta; qui il glifo è la categoria
   e non il tempo, quindi §13.1 non lo raggiunge.
6. **L'ora in coda alla riga** come `labelSmall` tabulare; l'esito, che è del giorno,
   non ne ha. Via «· alle 21:54» dalle frasi e via la stringa `journal_at_time`.
7. **La deriva in una card** `surfaceContainer`: chip, striscia, gelo e frase in una
   `Column` con 16dp di margine e 12 di passo; la striscia, il gelo e la frase perdono i
   margini propri. Il titolo di sezione resta fuori.

DESIGN §8.10 e la guida («una riga per oggi e per ogni giorno davanti») aggiornati.

### Lo scatto residuo di Oggi, solo una risposta

Quello che resta è del RenderThread: ~14 vettori animati rirasterizzati a ogni vsync
mentre lo stesso thread muove i layer dello scroll. L'unica leva che non tocca né i
disegni né i loro animatori è **fermare i loop mentre la lista è in movimento**
(`isScrollInProgress` → `setVisible(false)` sul drawable, che mette in pausa un AVD
infinito; ripresa da dove era a lista ferma). Costa zero da ferma e toglie tutto il
lavoro vettoriale dai frame dello scroll; il prezzo è che le icone si congelano durante
un trascinamento lento. Da misurare prima con l'A/B `animator_duration_scale 0`. Non
applicato: il committente ha chiesto solo la risposta.

### Verifica

`:core:domain` verde (166), `:core:data` verde (174, tre nuovi), `:app` verde (177, due
nuovi). `:app:lintDebug` a zero errori e 61 avvisi (uno in più). APK di debug costruito e
branch pushato: la CI produce quello da provare.

---

## Il meteo sta fermo mentre la pagina si muove (committente, 9 set 2026)

APK provato: «tutto ok». Richiesta: provare la soluzione proposta per lo scatto residuo
di Oggi, pushare e aprire la PR.

### Quello che AOSP ha detto prima di scrivere

La proposta era `setVisible(false)` sul drawable durante lo scroll, perché
`AnimatedVectorDrawable.setVisible` mette in pausa un loop infinito. Letto il sorgente
prima di fidarsi: `VectorDrawableAnimatorRT.pause()` e `resume()` sono **due TODO** in
AOSP. Sul RenderThread la pausa non esiste, e con essa cade anche una frase che DESIGN
§7.1 e il KDoc ripetevano dal 7 set — «la piattaforma mette in pausa l'animatore quando
la view non è più visibile». Non è vero: quello che ferma il lavoro è **non essere
disegnati**, perché hwui non prepara un nodo fuori dalla display list e non ne fa girare
gli animatori. Il risultato pratico era comunque giusto (una cella fuori schermo non
costa); la spiegazione no, ed è corretta in entrambi i posti.

Neanche `stop()` va bene: porta il disegno al fotogramma finale del loop, che per la
pioggia è quello **senza gocce**, e `start()` alla ripresa rifarebbe partire tutto dalla
nuvola.

### Cosa fa adesso

- `LocalMotionPaused`, in `ConditionIcon.kt`: vero finché uno scroll è in corso. Lo
  forniscono la lista di Oggi (`listState.isScrollInProgress`), la riga della striscia
  (`rowState`, in OR con quello che la pagina già dice) e il pager tra i luoghi
  (`pagerState.isScrollInProgress`).
- Un'icona animata è **due cose in una scatola**: il disegno fermo e il gemello animato
  sopra. Sono composti sempre entrambi — comporre quattordici painter al primo frame di
  uno scroll sarebbe lo scatto che si vuole togliere — e il locale decide chi si vede: il
  fermo ad alpha 1 con la `ImageView` `INVISIBLE` mentre la pagina si muove, il gemello
  sopra un fermo trasparente da ferma.
- Una View `INVISIBLE` il padre non la disegna, il suo nodo esce dalla display list e
  hwui non ne tocca gli animatori: il RenderThread spende i frame dello scroll sui layer
  e non sui vettori. Alla ripresa il loop è dove lo mette l'orologio — gli animatori
  vanno a tempo di frame — non dove era rimasto: niente replay.
- Il prezzo visibile: un cambio di posa alle due estremità di uno scroll, il disegno
  animato scatta nella posa ferma quando il dito si muove e torna quando si ferma. È il
  compromesso già detto nella risposta di ieri, con «fermo nella posa da fermo» al posto
  di «congelato».

DESIGN §7.1 e il KDoc di `ConditionIcon` corretti sulla pausa e aggiornati sul nuovo
comportamento.

### Rimasto aperto (su device)

- Lo scatto è sparito, o ridotto? Se resta identico, il collo non era il RenderThread e
  l'A/B `animator_duration_scale 0` resta il modo di dirlo.
- Il cambio di posa a inizio e fine scroll: se disturba più dello scatto, si può limitare
  al solo fling (drag in corso → `interactionSource` della lista) o togliere del tutto.

### Verifica

Suite verdi; lint invariato; APK di debug costruito; PR aperta sul branch.

---

## La frase anticipa (committente, 9 set 2026)

Osservazione dal device, sul widget: «Pioggia fino alle 18:00 circa» va bene, ma se non
sta piovendo e potrebbe piovere tra qualche ora, o oggi, o domani, la frase non lo dice;
e in generale sarebbe bello che anticipasse quello che sta per accadere e merita
attenzione, non solo la pioggia. Chiesto di verificare, e nel caso implementare.

### Verificato: era così

`HeadlineEngine` aveva tre gradini — un codice severo entro 12 ore, pioggia adesso,
pioggia **entro 6 ore** sopra la soglia della notifica (70%) — e poi silenzio. Quindi un
mattino asciutto prima di una sera bagnata, un giorno asciutto prima di un domani
bagnato, un pomeriggio al 60%, una notte di gelo: niente. La frase era nata come sorella
della notifica (stesse soglie, stesso orizzonte), e la notifica ha ragione a guardare
sei ore; la frase in cima alla pagina no.

### La scala, primo che combacia vince

1. Codice severo entro 12 ore (com'era).
2. Pioggia adesso, con quando smette (com'era).
3. **Pioggia probabile oggi** (≥ 70%): l'orizzonte è il resto del giorno locale, e mai
   meno delle sei ore del notificatore — alle 22:00 la pioggia delle 03:00 è ancora di
   stanotte, e la frase la dice come «Ombrello verso le 03:00».
4. **Gelo entro domattina**: l'ora più fredda da adesso alle 10:00 di domani, se è a zero
   o sotto (la soglia della striscia di deriva) e **adesso non gela**. Solo
   anticipazione: un pomeriggio a −2° è già il numero dell'eroe, ripeterlo sarebbe la
   pagina che dice due volte la stessa cosa.
5. **Nebbia in arrivo** entro 12 ore, solo se adesso non c'è: «Nebbia» adesso è già la
   parola della condizione sotto il numero.
6. **Vento forte adesso**: sostenuto ≥ 39 km/h (la fascia «tieni il cappello» del tile)
   o raffiche ≥ 60. L'unico «adesso» che la frase porta, perché l'eroe non ha il vento e
   il modello non ha il vento orario: qui non può guardare avanti come gli altri.
7. **Pioggia possibile oggi**: almeno il 50%, sotto la soglia dell'ombrello. Il 50 è la
   soglia che il motore già aveva per «il cielo ha smesso di promettere pioggia», letta
   al contrario. Registro onesto: «Possibile pioggia verso le 17:00».
8. **Pioggia probabile domani**: la prima ora di domani sopra il 70%. «Domani ombrello
   dalle 09:00».
9. Niente: resta la risposta di un giorno senza niente di tutto questo.

Cosa NON c'è, e perché: il caldo e l'UV alto sparerebbero ogni giorno d'estate e la
frase smetterebbe di significare qualcosa; il vento di domani non è nel modello.

### Il testo

Nuove parole in entrambe le lingue, nel registro pieno e in quello breve del widget:
possibile pioggia/neve, domani ombrello/neve, gelo con la minima (breve senza), nebbia,
vento forte fino a X (breve «Vento fino a X»). Gelo e vento stampano un numero con la
sua unità, quindi `HeadlineText.of` prende `UnitSettings`; i due widget lo passano dal
loro modello. La guida («La frase») elenca i nuovi esempi e dice che la frase anticipa.

### Verifica

`HeadlineEngineTest` riscritto come scala: 19 casi (erano 10) — ogni gradino con la sua
frase e con chi vince su chi, il test «oltre sei ore non è ancora notizia» diventato
«più tardi oggi è l'ombrello», la mezzanotte con e senza le sei ore, il gelo che tace se
gela già o se arriva dopo le dieci, la nebbia che tace se c'è già, le tre soglie del
vento. Suite `:app` verde (186). Lint a zero errori e 61 avvisi, come prima. APK di debug
costruito; branch `claude/headline-looks-ahead-p2x7mn` e PR per la CI: **la verifica su
device è del committente**, in particolare il widget «Ora» con le frasi nuove nel suo
registro breve.

---

## La palette che non si vedeva: la nota che mentiva, la notte smorta (committente, 9 set 2026)

Due screenshot dello stesso tramonto di Londra, uno con Carta e uno con Brillante:
canvas identico, icone identiche. La domanda era se la palette funzionasse.

### Verificato: funziona, ed è la prova che serviva

Misurati i pixel dei due screenshot contro le tabelle in `Scheme.kt` e `SkyPalette.kt`.
Superficie `#0D141C` contro `#16130E`, pill della navigazione `#624000` contro
`#004C70`, testo `#B9C8DF` contro `#CCC6BA`: sono `VividDarkScheme` e `ChiaroDarkScheme`
esatti. La ribbon cambia dove deve: banda del giorno `#2BB8FF` contro `#54B7F0`, ora blu
`#2241B5` contro `#354A89`. La palette arriva ovunque. Nessun bug di wiring.

Il canvas era identico **per aritmetica, non per un guasto**. Londra alle 19:33, sole a
circa 0°, copertura 100%: agli anchor di 4° e 0° i due stop centrale e basso sono gli
stessi esadecimali in entrambe le tabelle (`#F49C04`, `#FFD083`, `#E58800`, `#FFC268`),
perché `gen_vivid.py` tocca il bordo del gamut prima di arrivare a ×1.8 — lo stesso
fatto che §2.5 registra già per gli ambra. Resta solo lo stop alto, e il 100% di nuvole
gli toglie il 70% di quel che gli era rimasto (`CloudDesaturation`), sotto lo scrim
superiore. Calcolato: paper `#535F6E #917A57 #B8A992`, vivid `#395571 #917A57 #B8A992`.
Due stop su tre byte per byte.

### Le icone: la nota mentiva davvero

`ChiaroIcons.styledRes` legge la palette solo sul ramo `style != FILL`. Il set pieno è
la tavolozza di Meteocons e non ha un fratello vivid (§13.1, ed è voluto). Lo screenshot
era sul set pieno — luna campionata `#86C2DA`, cioè `mcfn_clear_night`. Quindi la nota
delle impostazioni, «con icone del meteo più vivaci», prometteva a quel lettore una cosa
che non sarebbe successa. Contro la regola che lo schermo non mente.

`settings_palette_note` ora dice solo quello che vale sempre, e `paletteNote()` gli
appende la frase giusta per il set icone attivo: a linea, «Brillante le schiarisce anche
sul tema scuro»; piene, «tengono i colori loro, quindi la palette non le tocca».

### Il pavimento del cielo: perché Brillante era smorto di notte

Misurata ogni banda come frazione della cromia che sRGB regge alla sua luminanza:

| | giorno | 8° | ora d'oro | ora blu | −12° | −18° | notte |
|---|---|---|---|---|---|---|---|
| paper | 1.00 | 0.81 | 0.59/1.00 | 0.59 | 0.36 | 0.34 | 0.25 |
| vivid ×1.8 | 1.00 | 1.00 | 1.00 | 1.00/0.78 | 0.65 | 0.63 | **0.44** |

Il vestito era al massimo esattamente sul cielo che è già luminoso e al minimo su quello
sotto cui si apre l'app la sera. Non è una scelta: è la forma della regola. ×1.8 è un
**multiplo della cromia di paper**, giusto per un token — è quello che tiene neutro un
token deliberatamente neutro — e paper la notte l'ha disegnata con la cromia più bassa
di tutte. Il moltiplicatore dava il meno dove il gamut aveva il più.

`SKY_FLOOR = 0.65` in `gen_vivid.py`: nessuna banda sotto quella frazione del gamut alla
propria luminanza. Essendo una frazione del gamut e non un multiplo di paper, può solo
alzare le bande che il moltiplicatore aveva lasciato piatte. Sposta **tre righe e nient'
altro**: giorno, sole basso, i due anchor dorati, orizzonte e ora blu erano già sopra e
escono dal generatore identici — quindi la misura dello scrim di §3.7 non si muove, e
nemmeno il crepuscolo bruno che quella sezione registra. I token semantici non lo
ricevono: `VividLightColors` e `VividDarkColors` escono invariati.

0.65 scelto come fu scelto 1.8: renderizzando il foglio e guardandolo
(`tools/palette_sheet.py`, chromium headless). A 0.75 la mezzanotte comincia a leggersi
come un blu reale invece che come una notte.

### Considerato e scartato

**Un anchor in più all'ora d'oro**, il rimedio che §3.7 stessa indica. Misurato: agli
stop dorati la cromia disponibile alla luminanza tenuta è 1.00–1.01 volte quella di
paper. Non c'è un secondo ambra da avere, quindi un anchor in più interpolerebbe tra due
colori identici. Il rimedio di §3.7 vale per il crepuscolo bruno, che questo cambiamento
non tocca; qui non ci sarebbe niente da separare.

### Verifica

`SkyPaletteTest` guadagna il limite inferiore che gli mancava. C'era solo
`Paper.gradient(-6.0) != Vivid.gradient(-6.0)`: una singola altitudine, che qualunque
collasso parziale supera indisturbato. Adesso ogni stop che **può** differire deve
differire, e ogni stop che coincide deve esibire il gamut come scusa (cromia di paper
entro il 5% del tetto) — più un tetto sul numero di coincidenze, perché il controllo
stop-per-stop passerebbe anche se i due vestiti diventassero uno. Il secondo test
rimisura la regola di §3.7 sugli esadecimali emessi, che è l'unico modo di accorgersi
di `SKY_FLOOR` modificato e tabella non rigenerata. L'aritmetica OKLCh e il bordo del
gamut sono riscritti in Kotlin apposta perché possano dissentire da `color_math.py`.

DESIGN.md §3.7: tabella aggiornata sulle tre righe, la clausola stampata, il perché
misurato — e `PaletteDocTest`, che rilegge quella sezione e la confronta con quello che
il canvas disegna davvero, è il test che tiene le due cose insieme.

Suite intera verde: `:app` 188 (erano 186: i due test nuovi), `:core:domain` 166,
`:core:data` 174, `:core:sync` 5. `ScrimContractTest` continua a passare senza che sia
stato toccato, ed era il rischio vero del pavimento: spazza entrambe le tabelle a ogni
mezzo grado, e lo stop più luminoso — `#AFE0FF`, quello contro cui la misura è fatta —
è fra quelli che il pavimento non muove. Lint a zero errori e 61 avvisi, come prima. CI
verde su `claude/palette-settings-bug-343c6z`.

**La verifica su device è del committente**: il cielo della sera è la cosa che cambia, e
un foglio renderizzato non è un telefono in mano.

---

## Il quarto widget: «L'arco del giorno» (committente, 9 set 2026)

Richiesta: un widget nuovo, «moderno, in stile professionale, con qualcosa di speciale che
lo distingua dai classici widget meteo», ridimensionabile da 1×1 a 4×4 e che si riconfiguri
per informazioni e per layout a ogni misura, con una pagina di impostazioni completa. Libertà
creativa dichiarata («dimentica le regole»); la sola regola tenuta è quella che tiene verde la
CI, perché l'APK da provare esce da lì.

### Cos'è

Il percorso del sole sopra il luogo del lettore, **calcolato** dallo stesso motore
astronomico che dipinge il canvas: non un'icona di alba e tramonto, la traiettoria vera,
campionata ogni quarto d'ora (97 campioni, pochi millisecondi). Sotto, il cielo di ogni ora
come bande: la stessa `SkyPalette.gradient()` del canvas, con la copertura nuvolosa e la
pioggia dell'ora prevista dove il report ha l'ora, sotto lo stesso scrim §3.6 della card. La
luna: il suo percorso punteggiato mentre è alta e il disco nella **fase vera** (frazione
illuminata ed elongazione da `AstronomyEngine.moonIllumination`, terminatore come semiellisse,
lato illuminato a destra nell'emisfero nord e a sinistra in quello sud). La pioggia: la
probabilità di ogni ora come barra che sale dal suolo verso l'orizzonte (100% tocca
l'orizzonte; rampa `rainAt`). Il presente segnato, il passato velato. Le ore sotto, e sotto le
ore la temperatura prevista dove il report ce l'ha: il passato stampa l'ora e nessun numero.

In parole: il **prossimo momento della luce** con la sua ora («Tramonto alle 19:42») e il
conto alla rovescia («tra 2 h 10 min», contato sull'orologio, giusto anche con dati vecchi);
l'agenda delle prossime ventiquattro ore, riga per riga, con il **verdetto** del widget Cielo
accanto alle righe che sono anche momenti seguiti; sulla card a quattro righe, la settimana.

### Le cinque forme (`ArcLayout.kt`, puro, tabella in `ArcLayoutTest`)

Colonne dalla larghezza (soglie 120/210/300 dp), righe dall'altezza (150/245/340): stanno
negli spazi vuoti tra le griglie misurate (una cella ~70-101 dp, due ~150-180, tre ~230-260,
quattro ~320-360; una riga ~82-101, due ~189, tre ~290, quattro ~390).

- **DIAL** (una colonna): l'arco e un numero sotto, temperatura o ora del prossimo momento a
  scelta. 85×82 → arco 65×28,96 dp (62 − 29,04 la riga a 22 sp − 4). Con dati vecchi la riga
  «stale» toglie i suoi 14,52 all'arco: il segno che i dati sono vecchi vince sul disegno, e
  il pavimento dell'arco è 12 dp.
- **STRIP** (una riga): a due celle le parole **sopra** l'arco (139×31,6: 47 dp di larghezza
  non sono un giorno); a tre e quattro accanto, colonna di parole 100 dp («Tramonto · 19:42»
  a 12 sp è ~95), arco 122×62 e 212×62 con le etichette delle ore (≥ 52 dp) senza temperature
  (< 76).
- **CARD** (due colonne, due o più righe): numero 28 sp, luogo, frase su due righe a 13 sp,
  arco, agenda. 159×189: header 88,44, arco 66,56, nessuna riga; 159×290: 3 righe, arco
  84,08 con le temperature.
- **PANEL** (tre o quattro colonne, due righe): una riga sola in testa, il numero a 30 sp e
  accanto la frase a 15 sp con luogo · conto alla rovescia (· stale) a 12 sp sotto; la frase
  prende due righe sotto i 190 dp di colonna («Golden hour ends at 09:15» a 15 sp Medium è
  ~165). 340×189: header 39,6, arco 59,08, **2 righe di agenda** (il pannello preferisce le
  righe: 56 dp di arco preferiti); 250×189: frase su due righe, header 55,44, arco 70,4, 1 riga.
- **BOARD** (tre o quattro colonne, tre o quattro righe): l'arco preferisce 110 dp e prende
  fino a 150. 340×290: arco 132,92 e 3 righe; 340×390: settimana in fondo (70,88 dp: nome,
  glifo 22, massima, minima), arco 128,88 e 4 righe.

Il budget è sempre `righe = ⌊(spazio − arco preferito − gap + gapRiga) / (riga + gapRiga)⌋`,
mai più delle righe che esistono; l'arco prende il resto fino al suo tetto. Riga agenda =
13 sp × 1,32 + 6 = 23,16 dp; a font scale 1,3 il conteggio scende (1 riga sul 4×2) invece di
sforare. Densità compatta = testi al 90%: riga 21,44, e sul 4×3 quattro righe invece di tre.

### Le impostazioni (`ArcSettings`, DataStore `widget_arc`, per istanza)

Finestra (oggi da mezzanotte a mezzanotte, o le prossime 24 ore); suolo (bande, nastro
della luce, niente); strati (sole, luna, pioggia, presente, passato velato, ore,
temperature); parole (prossimo momento / frase del giorno / solo il numero; la cifra della
card 1×1; massima e minima dal look condiviso); agenda (sole, luna, pioggia, verdetti);
settimana; densità. Diciassette chiavi, codec puro (`ArcSettingsCodec`, tabella in
`ArcSettingsTest`): un valore di un'altra versione torna al suo default e non porta giù la
card. Più il look condiviso (sfondo, opacità, icone) e il luogo, come gli altri tre.

La pagina è sua (`ArcConfigActivity`): si apre su un'**anteprima viva** della card a otto
misure (1×1, 2×1, 4×1, 2×2, 4×2, 4×3, 2×4, 4×4, le misure del device di riferimento),
disegnata dallo stesso `ArcPainter` e dallo stesso `arcPlan` del launcher, con una frase che
dice cosa mostra quella misura; ogni tocco salva, ridisegna il widget e aggiorna l'anteprima;
un tasto riporta ai default.

### Decisioni

- **La grafica è un bitmap** (`ArcPainter`, `android.graphics`): RemoteViews non disegna una
  curva in nessun altro modo, ed è quello che permette all'anteprima di mostrare la stessa
  immagine del launcher. Le parole restano `Text` di Glance (localizzazione, font scale,
  accessibilità); nel bitmap solo le etichette delle ore e le temperature, perché devono
  stare a una x precisa, con le misure del testo fatte davvero (`Paint.measureText`: il passo
  si allarga finché «12 PM» non collide). La `contentDescription` dell'immagine dice il
  prossimo momento e quando: l'equivalente testuale (§9.3) anche sulla card da una cella.
- **Sole e luna con esadecimali** in `ui/theme/ArcPalette.kt`, il solo file della feature
  che ne ha (`NoRawColorTest`); tutto il resto sono ruoli e tabelle: percorso ed etichette
  negli inchiostri della card, bande dalle tabelle del cielo, pioggia dalla rampa §2.3.
- **Niente curva della temperatura.** Due scale su un grafico sono un doppio asse (§9.1): i
  numeri sotto le ore fanno il lavoro, e la card «Le prossime ore» ha già la striscia.
- **Finestra «oggi» di default**: è l'arco che tutti riconoscono; «le prossime 24 ore» in
  opzione parte 30 minuti prima di adesso, perché il disco del sole stia nel riquadro e
  l'ultima mezz'ora si legga come appena passata.
- **L'agenda guarda sempre 24 ore avanti**, qualunque sia la finestra: la regola è una,
  `TodayStateBuilder.agenda`, resa pubblica e finestrata; il `timeline` di Oggi è la stessa
  regola tagliata a mezzanotte (test in `TodayStateBuilderTest`). Il widget e la schermata
  hanno già stampato due albe diverse una volta, e la cura è una regola in un posto solo.
- **Verdetto sulla riga**: match per id del job (`sun.set`, `golden_hour.am` per la fine
  dell'ora d'oro del mattino, `twilight.astronomical.pm` o `darkness.window` per il buio
  pieno…) e ±20 minuti sull'inizio o sulla fine del momento. Le svolte della pioggia e
  l'arcobaleno non hanno job: nessuno si abbona a un rovescio.
- **Sotto i 40 dp il disegno si asciuga** (dial, strip a due celle): bande, arco, disco,
  presente; niente luna, pioggia, sole sotto l'orizzonte. Visto ai pixel: a 29 dp erano
  rumore.
- **Il velo del passato copre solo il suolo** (bande o nastro), mai la card nuda: con il
  nastro copriva un blocco grigio (visto ai pixel), ora è alto quanto il nastro; il percorso
  del sole nel passato si attenua da solo (alpha × 0,45).
- **Refactor condiviso, piccolo**: `resolveWidgetPalette`, `effectiveWidgetBackground` e
  `widgetCardFill` estratti da `WidgetCard` (l'anteprima non può scegliere un inchiostro che
  il launcher non sceglierebbe); `VerdictMark`/`VerdictChip` e le righe della pagina di
  configurazione (`SectionLabel`, `SwitchRow`, `ChoiceRow`) da private a internal.
- La guida dice «quattro» e aggiunge una frase sull'arco; README e CHANGELOG aggiornati.

### Rimasto aperto (su device)

- Le misure sul launcher vero: il 1×1 su griglie da 70 dp, il 4×4 (bitmap 858×~355 px), le
  font scale grandi, le etichette in 12 ore. L'anteprima Compose approssima la tipografia di
  Glance (line height diverse di qualche dp): è un'anteprima, non uno screenshot.
- L'icona del picker e la voce nel menu «widget» del launcher con la quarta card.
- VISION §5.9/§6 e DESIGN registrano tre widget: da aggiornare quando la card avrà passato la
  prova su device, come per gli altri tre.

### Verifica

Suite intera verde: `:app` 228 (erano 188: +16 `ArcLayoutTest`, +12 `ArcSeriesTest`, +6
`ArcSettingsTest`, +5 `ArcPainterTest` — Robolectric in grafica nativa, che dipinge il bitmap
davvero e ne legge i pixel, e con `CHIARO_RENDER_DIR` salva i fotogrammi in PNG per
guardarli, che è come sono stati trovati il blocco grigio del nastro e il rumore del dial —
+1 `TodayStateBuilderTest`; `WidgetPreviewTest` sa che i widget sono quattro), `:core:domain`
166, `:core:data` 174, `:core:sync` 5. Lint a zero errori. APK debug costruito in locale;
branch `claude/day-arc-widget-7k2m9q` e PR per la CI: **la verifica su device è del
committente**.

---

## L'arco su device: la colonna da dieci figli, la luna tagliata, la review (committente, 9 set 2026)

Primo screenshot del 4×4 (One UI, 923 px, ~2,35 px/dp), due appunti e una richiesta di
review. «Ottimo widget» a parte, quello che c'era da correggere.

### Il 4×4 senza settimana (e senza la quarta riga)

Lo screenshot mostrava tre righe di agenda, niente settimana e mezza card vuota, mentre
l'anteprima nelle impostazioni mostrava tutto. La misura della card (800×950 px ≈ 340×404 dp)
e l'altezza del grafico (~129 dp) dicono che il piano era quello giusto, BOARD a quattro righe
con settimana: quello che mancava era stato **scartato in disegno**. Glance disegna al massimo
**dieci figli per contenitore** e i successivi li lascia cadere senza una parola. La colonna
della card ne aveva tredici: intestazione, spaziatore, grafico, poi per l'agenda uno
spaziatore in testa e uno tra ogni riga (1 + 4 righe + 3 spaziatori = 8), lo spaziatore a peso
e la settimana. I primi dieci finiscono esattamente alla terza riga più il suo spaziatore:
la quarta riga, il peso e la settimana erano l'undicesimo, il dodicesimo e il tredicesimo.
Compose non ha quel limite, ed è per questo che l'anteprima mentiva.

Ora l'agenda è **un figlio solo** (`Column`), con gli spazi tra le righe come padding e non
come spaziatori: la colonna della card ne ha sei, quella dell'agenda al massimo sei. Nessun
altro contenitore del widget supera i dieci (settimana: sette colonne; riga di agenda:
cinque figli). L'anteprima ora dice la verità per costruzione: stesso piano, stessi limiti.

### La luna tagliata in basso

`mc_moonset` è, per disegno, un disco **ritagliato dall'orizzonte** (`clip-path` a 39,5/64)
con sotto una linea di 2 unità e una freccia. Nella schermata Oggi sta a 34 dp e la linea è
un dp: si legge «luna che tramonta». Nella riga del widget stava a 18 dp: la linea era 0,56 dp,
invisibile, e restava un disco con il fondo mancante — esattamente l'appunto. Le righe della
luna ora disegnano la **luna nella sua fase vera** all'istante della riga
(`ChiaroIcons.moonPhaseRes(MoonPhase.at(at))`): intera, la stessa luna che il grafico dipinge,
e la parola accanto dice se sorge o tramonta. I glifi di alba e tramonto restano i loro: i
raggi li fanno leggere anche a 20 dp. Il glifo di riga passa da 18 a 20 dp, quello del widget
Cielo, così le due liste sono sorelle.

### La review: cosa ho trovato e cosa ho cambiato

- **Il conto alla rovescia mentiva con il passare del tempo.** «tra 7 h 43 min» era esatto
  al minuto al momento del disegno, e il widget si ridisegna a ogni sync — ogni ora per
  default, mai al minuto: un'ora dopo diceva ancora 7 h 43. Ora è **grossolano di proposito**:
  ore arrotondate («tra 8 h»), «entro un'ora» sotto i sessanta minuti, «a momenti» sotto il
  minuto. L'ora esatta accanto («alle 19:42») è il fatto che non invecchia. Le due stringhe
  al minuto sono uscite da entrambe le lingue.
- **L'eroe era l'arcobaleno.** «Forse un arcobaleno alle 17:00» in testa alla card: la
  probabilità di un fenomeno meteo, non un momento della luce, che è quello che l'impostazione
  promette. `ArcSeries.nextLight`: il primo evento con un job del cielo dietro (sole o luna);
  l'agenda tiene tutto; se non c'è niente della luce davanti, l'eroe prende quello che c'è.
  Test in `ArcSeriesTest`.
- **La riga dell'arcobaleno era troncata** («…un arcobaleno stareb…»): la frase della
  schermata è prosa da riga intera. La riga ora stampa i due fatti, «Forse un arcobaleno a
  ovest (88%)» (`arc_rainbow_row`, con `SkyText.bearingRes`).
- **Gli orari dell'agenda non stavano in colonna**: «19:07» slittava a sinistra dei «19:00»
  sopra, spinto dal suo segno di verdetto. Ora, appena una riga ha un verdetto, ogni riga
  riserva il posto del segno (22 dp + 6): gli orari sono una colonna.
- **La strip a 3-4 celle troncava la seconda riga**: «Tramonta la luna · 19:00» a 12 sp è
  ~130 dp, la colonna ne aveva 100. Colonna a 110, numero a 22 sp (da 24) e **tre righe**
  dove l'altezza le regge (62 − 29,04 = 32,96 ≥ 2 × 15,84): il nome del momento a 12 sp Medium,
  sotto «19:00 · tra 8 h» in inchiostro secondario; a font scale 1,3 le due si fondono in
  «Tramonto · 19:00». Il grafico perde 10 dp (112 e 202). `ArcLayoutTest` aggiornato.
- Guardato e lasciato: le barre della pioggia sopra il tratto del sole sotto l'orizzonte
  (si sovrappongono a destra nello screenshot, ma il tratteggio resta leggibile e la barra è
  il dato); il segno di verdetto coi colori del tema chiaro su card scura (è una coppia
  misurata, come nel widget Cielo); il velo del passato che al mattino copre un terzo della
  card (è il disegno: il giorno è passato per un terzo).

### Verifica

`:app` 233 (+1 `ArcSeriesTest`, +4 `ArcTextTest` con Robolectric per le frasi), il resto
invariato. Lint a zero errori. `ArcPainter` non è cambiato, quindi i fotogrammi valgono
ancora. Stesso branch, CI. **La verifica su device è del committente**: il 4×4 con la
settimana in fondo e quattro righe, la luna intera nelle righe, la strip a 4×1 con tre righe.

---

## L'arco su device, secondo giro: la luna nuova, il segno di verdetto, l'audit dei glifi (committente, 9 set 2026)

Secondo screenshot: la settimana c'è e le quattro righe pure. Due domande: la luna della riga
«è corretta così?», e il segno di verdetto — la ✗ nel cerchio rosa — «non mi piace, troppo
forte, deve restare un avviso ma tenue».

### La luna: corretta per l'astronomia, e il committente la tiene così

Il glifo era quello di Meteocons per la **luna nuova**: un cerchio tratteggiato sottile. Il
classificatore aveva ragione — la luna nuova è l'11 settembre, la sera del 9 mancavano meno
di due giorni, ed è dentro l'ottavo di ciclo che `MoonPhase.at` chiama NEW_MOON. Il dubbio
era che in una riga un cerchietto tratteggiato si legga «qui non c'è niente»; l'alternativa
provata è stata la falce della notte serena (`mc_clear_night`) come simbolo fisso, il modo in
cui le righe del sole mostrano un sole e non lo stato del sole. Spiegato il perché del
cerchietto, **il committente ha scelto la fase vera**: «si vedrebbe lo spicchio o la luna
piena nel momento giusto? allora meglio lasciare così». Sì: ogni nome copre 45° di
elongazione centrati sul proprio istante, quindi il mezzo disco compare intorno ai quarti e
il disco pieno intorno al plenilunio, ciascuno entro circa un giorno e tre quarti
dall'istante esatto; il cerchietto tratteggiato resta i due-tre giorni al mese in cui la
luna non si vede, che è quello che disegna. La stessa fase che il grafico dipinge.

### L'audit dei glifi

Ogni glifo che il widget può mostrare, verificato nel drawable e nelle tre tabelle di
`ChiaroIcons` (piene, piene su fondo scuro, a linea su fondo scuro):

| riga | glifo | a 20 dp |
|---|---|---|
| alba, tramonto | `mc_sunrise`, `mc_sunset` | mezzo sole (clip a 39,5/64), riga di 2 unità = 1,7 px, freccia di 4,5 unità: si vede appena; il verso lo dice la parola |
| ora d'oro | `mc_horizon` | mezzo sole con la riga: leggibile (nello screenshot si vede) |
| ora blu | `mc_star` | stella a tratto 3: leggibile |
| buio pieno | `mc_starry_night` | falce e tre stelline: le stelline sono punti, la falce regge |
| sorge/tramonta la luna | `moonPhaseRes(MoonPhase.at(istante))` | otto glifi a tratto 2-3: falci, quarti, gibbose e disco pieno leggibili; la luna nuova è il cerchietto tratteggiato, scelto |
| arcobaleno | `mc_partly_cloudy_day_rain` | sole, nube, pioggia: leggibile |
| pioggia probabile / smette | `mc_raindrops`, `mc_cloudy` | leggibili |
| settimana | `conditionRes(codice, notte = false)` | 22 dp, la stessa mappa della card Oggi |

Tutte le famiglie di glifi hanno le quattro varianti (`mc_`, `mcn_`, `mcf_`, `mcfn_`), le
otto fasi della luna comprese, e sono nelle tabelle: un glifo senza fratello sarebbe stato
un crash al primo lettore che sceglie le icone piene (`getValue` lancia). `ArcTextTest` ora
lo pinza: ogni tipo di riga, in ogni famiglia, su entrambi i fondi, in entrambe le palette,
su trenta giorni di luna.

### Il segno di verdetto

Era la pillola del widget Cielo, riusata com'era: cerchio pieno di 22 dp nel colore del
contenitore e la ✗ nell'inchiostro del verdetto. Due cose la rendevano un pugno: **la coppia
di colori era quella del tema chiaro** — scelta con `isNight`, e il telefono era in tema
chiaro — su una card che è scura qualunque cosa faccia il telefono (il cielo sotto lo scrim,
o la card scura), quindi un rosa pallido pieno sul fondo più scuro dello schermo; e **la
pillola è l'eroe della card Cielo**, mentre qui è una nota accanto a un orario. Ora la riga
porta il solo glifo della serie — `✓ ~ ✗ ?`, una forma prima che un colore (§2.3), che è la
parte che regge la deuteranopia — nell'inchiostro del verdetto **scelto per il fondo della
card** (`palette.darkGround`), a 13 sp Medium come le parole della riga, in una colonna di
14 dp riservata per tutte le righe appena una ha un verdetto. Gli inchiostri del set scuro
sono quelli misurati contro una superficie scura: il glifo si legge dove sta, ed è la cosa
che mancava al «verde nudo» che il 4 set aveva fatto crescere il contenitore sul widget
Cielo. Il widget Cielo resta com'è: lì il verdetto è il soggetto.

### Verifica

`:app` 234 (+1 in `ArcTextTest`, l'audit dei glifi), lint a zero errori. Stesso branch.
**Su device**: la ✗ tenue accanto a «Ora d'oro»; la luna delle righe cambia da sola con la
fase, e la prima falce si vedrà dal 13 settembre.

---

## Il segno di verdetto, terza volta: un disegno, non un carattere (committente, 9 set 2026)

«Ora va bene perché è tenue, però la X scritta così, come un font in stile scrittura a mano,
non mi piace». Aveva ragione, e il motivo è preciso: i glifi della serie `✓ ✗` sono U+2713 e
U+2717, che **Roboto non ha**. Android li prende da un font di fallback per i simboli — su
One UI di taglio calligrafico, su un Pixel un altro — quindi il segno non era mai stato nella
mano del widget, né nel widget Cielo né qui. L'alternativa a costo zero, il segno di
moltiplicazione «×» di Roboto per il FAIL, avrebbe lasciato la ✓ al font di fallback: due
segni della stessa famiglia in due mani diverse.

Quattro vettori (`ic_verdict_pass`, `_unstable`, `_fail`, `_unknown`): un peso di linea solo,
2,4 su 24 — 1,2 dp ai 12 dp a cui stanno, il tratto di un 13 sp Medium — punte arrotondate,
nessun colore proprio: tinti con l'inchiostro del verdetto dove compaiono (`ColorFilter.tint`),
scelto per il fondo della card come prima. 12 dp è l'altezza delle minuscole delle parole
accanto, così la croce sta nella riga come una lettera e non come un'icona. La regola resta
quella di §2.3: una forma prima che un colore, e un disegno per verdetto, mai due verdetti su
una forma (test in `ArcTextTest`). La `contentDescription` del segno è la parola del verdetto,
così la riga si legge «Ora d'oro, 19:07, no».

Il widget Cielo usa ancora il carattere, dentro il suo contenitore: stessa mano calligrafica.
Non toccato — è il disegno che il committente ha approvato il 4 set — ma i quattro vettori sono
pronti anche per lui, se lo vorrà: una riga in `VerdictMark`.

### Verifica

`:app` 235, lint a zero errori. Stesso branch. **Su device**: la croce nella riga «Ora d'oro».

---

## I quattro segni ovunque: widget Cielo, chip dell'app, tabella in `ChiaroIcons` (committente, 9 set 2026)

«Usa i 4 vettori anche nel widget Cielo e poi verifica in tutta l'app se ci sono altri
punti in cui si potrebbero utilizzare.»

Cercati tutti i punti che stampano i glifi del verdetto come caratteri (`.glyph`, `✓`,
`✗`, `"~"`): erano **due**, e ora sono zero.

- **`VerdictMark` del widget Cielo**: il carattere `verdict.kind.glyph` a 12 sp dentro il
  contenitore rotondo di 22 dp. Ora il disegno a 12 dp (`SkyMarkGlyph`, che sostituisce
  `SkyMarkSp`), tinto con l'inchiostro del verdetto come prima, con la parola del verdetto
  come `contentDescription`. Il contenitore resta: lì il verdetto è il soggetto della card,
  ed è il disegno approvato il 4 set.
- **`VerdictChip` dell'app** (`ui/components`), cioè la schermata Cielo (le card dei
  momenti e il calendario), la guida e ogni riga che apre con `✓ Ottimo · 12% nuvole`: il
  `Text(glyph)` a `labelLarge` diventa una `Icon` di 14 dp scalata con il font del lettore
  (`forText()`), tinta con l'inchiostro del chip. La semantica non cambia: il chip
  annunciava già solo «ottimo, 12% nuvole» e mai il segno.

La tabella sta in un posto solo, `ChiaroIcons.verdictMarkRes`, in due forme — per il
`VerdictKind` della UI e per lo `SkyVerdictKind` del dominio — perché i widget hanno in
mano verdetti e il chip ha in mano il suo enum; `ArcText.markRes` sparisce e i due widget e
il chip leggono la stessa riga. Il test in `ArcTextTest` verifica che le due forme diano gli
stessi quattro disegni e che nessun verdetto ne condivida uno con un altro.

Dove i vettori **non** servono, verificato: le notifiche (solo testo, il segno non c'è),
il Diario (parole, nessun glifo), le stringhe (nessuna contiene `✓` o `✗`). Il dominio
tiene il suo `glyph` in `SkyVerdictKind`: è il vocabolario di tweather, che li stampa come
codice, e nessuno in Chiaro lo legge più.

DESIGN.md §8.7 registra che il glifo è un disegno e perché.

### Verifica

`:app` 235, lint a zero errori. Stesso branch. **Su device**: il segno nel widget Cielo e
i chip della schermata Cielo.

---

## Il riallineamento dei due repo (committente, 9 set 2026)

Richiesta: «controlla i file `UPSTREAM.md` dei due repo e, prima di toccare il codice,
fammi un elenco delle implementazioni da fare per riallineare i due repo»; poi
«esegui i punti che hai proposto nell'ordine proposto».

### Il confronto

tweather non ha un `UPSTREAM.md`: il suo registro sono le fasi che citano Chiaro nel
suo `PLANNING.md`. Il confronto è stato quindi meccanico — la riscrittura di
`tools/seed_core.py` applicata a tweather HEAD e diffata file per file con `:core` —
e poi ogni differenza letta contro quello che i due registri dichiarano. Prima: 55
file identici su 86 condivisi, 30 diversi, più 2 solo a monte e 10 solo qui. Dopo: 66
identici, e ogni differenza rimasta ha la sua riga in `UPSTREAM.md`: la classificazione
file per file sta nell'elenco «What is NOT the same as upstream» e la passata è
raccontata nella sezione «The second pass». Il confronto si ripete con la riscrittura
di `tools/seed_core.py` applicata a un checkout di tweather e un diff dei due alberi.

### Portato a monte (in tweather), e la base che era vecchia

Due correzioni che `UPSTREAM.md` segnava come «bug anche a monte» e che a monte
mancavano ancora: `DailyForecast.precipPct` nullable (con `WeatherCodes.FIRST_PRECIP_CODE`
e `isPrecipitation`, così il mapper torna identico e non diverso per una riga) e
`SkyNotScheduled.DARK_ALL_DAY`. Ogni file toccato in `:core` è di nuovo identico; le
superfici a valle in tweather (`WeatherReadme`, `WeatherJson`, `SkyDocument` e le sue
note) sono state adattate nel suo registro. Lì è la **Fase 29**.

La terza, la stringa `"null"` che `WeatherSnapshots.flatten` scriveva in
`current.precip_chance_pct`, è stata portata e poi **ritirata**: il confronto era stato
fatto su un `main` locale di tweather fermo al 6 set, e il rebase della PR gemella ha
trovato le Fasi 27–28c a monte, fra cui la 28 che sullo stesso punto aveva deciso il
contrario e di proposito — `history.diff` è un diff di `weather_data.json`, un valore
assente è una riga che dice `null`, con `NullValue` nominato e insieme di chiavi fisso.
Qui il Diario è prosa e le sue «shift» accoppiano le chiavi, quindi la chiave assente
resta omessa, ora anche per `sunrise` e `sunset` che scrivevano ancora la parola:
stessa realtà, due registri, due file, e `UPSTREAM.md` lo dice. Dalla stessa Fase 28
sono invece tornati a valle i fatti: `Duration.hhMm()` nel dominio, `ForecastDiff` senza
`dayLabel` (con il suo test), il `location` che ripiega sul paese come `City.label`, la
chiave `astronomical.daylight_duration`. La lezione è di procedura, e sta in memoria:
`git fetch` e confronto con `origin/main` in tutti e due i repo prima di misurare.

### Fatto qui

- **`UPSTREAM.md`** registra le divergenze che erano nate senza una riga
  (`SearchHistoryStore.remove/restore`, `WeatherHistoryDao.observeFor` e
  `historyFlowFor`, la collocazione di `WeatherFreshnessTest` in `:core:data`, il nome
  del file di `PowerSaveState`, il `fetchLogStore` in `overrideForTests`), segna come
  portate le tre correzioni, e ha una sezione nuova sulla passata.
- **I tre test che tweather aveva e Chiaro no.** `SkyAlarmSchedulerTest` è un porting
  diretto in `:app` (Robolectric, `ShadowAlarmManager`: inesatto, `RTC_WAKEUP`, una
  sola sveglia, il boot la riarma, un promemoria non consegnabile riarma comunque).
  `WeatherSyncWorkerTest` è un porting in `:core:sync`, che per questo ha ora un banco
  Robolectric (`isIncludeAndroidResources`, `work-testing`, e le librerie del layer dati
  come `testImplementation`, perché arrivano a `main` attraverso `:core:data` come
  `implementation` e il classpath dei test non le vede): i notificatori e i widget sono
  fake dietro `SyncDependencies`, e due casi sono di Chiaro — un fetch fallito scrive
  nel `FetchLogStore` con la sua ragione, e ridisegna i widget perché il marcatore di
  vecchiaia possa apparire. Per il primo `ServiceLocator.overrideForTests` ha imparato
  il `fetchLogStore`. `SkyNotifierTest` tiene la struttura del test di tweather e
  nessuna parola: i due corpi, la localizzazione, e il controllo che nessun id puntato
  raggiunga la notifica. **L'orologio è asserito per forma e non per valore**:
  `SkyNotifier` segue l'impostazione 12/24 ore del dispositivo, che è del lettore e non
  del test.
- **`WeatherSnapshotsTest`** ha il caso della chance assente (identico a quello portato a
  monte).
- **I commenti dei file condivisi**, una sola formulazione datata in entrambi i repo:
  `LocationProvider` (i tre «Fase 3b» diventano «4 set 2026», e il paragrafo di testa
  dice che il file è tenuto identico e perché), `WeatherFreshness`, `PowerSaveState`, il
  doc di `getWeather` in `WeatherRepository`, e i cinque punti del porting (`precipPct`,
  `FIRST_PRECIP_CODE`, il mapper, le due chiavi dello snapshot, il test del mapper) dove
  la formulazione di Chiaro citava `ForecastOutcome`, «il seed» o il §1.1. Decisione:
  **nei file condivisi si data, non si numera** — i due PLANNING numerano lo stesso
  lavoro in modo diverso e lo faranno sempre. I commenti che nominano una superficie di
  una sola app (`RuleEngine`, `NotificationRule`) restano diversi di proposito.

### Deciso e non fatto

- `adoptGpsFix` con `gps_fixed_at` e `CachedLocationProvider` restano di Chiaro:
  tweather li ha rifiutati esplicitamente nella sua Fase 20 e la decisione è sua.
- L'estrazione di `weather-core`: il registro dice che il grilletto è scattato il 5 set.
  È una decisione, non un'implementazione, e resta al committente.

### Verifica

`:core` 354 test, `:app` 251, lint 0 errori. tweather, sul branch ribasato: 726 test e
lint 0 errori, con tre casi che su questa macchina falliscono anche su `origin/main`
pulito (la CI di `main` è verde) e sono dell'ambiente, non del riallineamento. Due PR,
una per repo, sullo stesso
branch `claude/core-realignment-9set-a4k7m2`: il committente le fonde e cancella i
branch.

---

## Fase 11 — Allerte ufficiali, primo strato: la Protezione Civile

Pianificata il 9 set 2026 (committente), dopo v1.0.0. Il perimetro sta in VISION §5.2, §5.4,
§5.5, §5.9 e §12.8; questa è la fase che lo realizza per i luoghi in Italia. La regola che la
governa: **in Italia fa fede la Protezione Civile**. L'ordine — prima questa, poi MeteoAlarm
(Fase 12) — è del committente e inverte la prima proposta, con tre motivi che VISION §12.8
registra per esteso: il CAP italiano di MeteoAlarm dichiara da solo di non essere l'allerta
ufficiale del Servizio Nazionale di Protezione Civile; il bollettino si verifica il pomeriggio
stesso contro il cielo e contro il bollettino regionale, cioè dove l'app viene collaudata; e «in
Italia una sola voce» è una regola di precedenza per paese, non una fusione di fonti.

Le parole, prima del codice. **«Allerta»** è quella che emette l'autorità (EN *warning*);
**«Avvisi»** restano quelli che il lettore scrive o accende (EN *alerts*). In codice la famiglia
si chiama `warnings/` (`OfficialWarning`, `WarningLevel`, `WarningHazard`), perché `Alert` è già
degli avvisi integrati. Nessun codice di zona (`Abru-A`), nessun identificativo di bollettino,
nessuna sigla CAP raggiunge lo schermo: c'è un test per questo, come per gli id del cielo.

- [x] `:core:domain/warnings/`: il modello (`WarningBulletin`, `ZoneWarning`, `PlaceWarnings`,
      `WarningLevel { NONE, YELLOW, ORANGE, RED }`, `WarningHazard { HYDRAULIC, HYDROGEOLOGICAL,
      THUNDERSTORM }` — i quattordici tipi di MeteoAlarm si aggiungono in Fase 12), l'indice
      delle zone (`WarningZoneIndex`: punto-nel-poligono sulle 156 zone, con il comune come
      ripiego) e il motore (`OfficialWarningEngine.forPlace` → `PlaceWarnings?`, `null` quando
      non c'è nulla da dire; `notificationFor(...)` con la sua impronta). Tutto puro, con
      tabella di test
- [x] `tools/build_warning_zones.py`, l'importatore di riferimento (come `import_meteocons.py`):
      dallo shapefile del bollettino (`Zona_all`, `Nome_zona`, geometria) e dal TopoJSON
      (`Comuni`) produce `core/data/src/main/assets/warning_zones_it.json` — codice, nome,
      regione, poligoni semplificati, comuni normalizzati. **Misurare**: obiettivo ≤ 400 KB
      grezzi; il numero finisce qui. `WarningZoneIndexTest` colloca venti comuni noti (coordinate
      dal geocoding, fissate nel test) nella loro zona, e tre punti in mare in nessuna
- [x] `City` cresce di `countryCode` e `admin3` (nullable, con default), riempiti su entrambe le
      strade: `GeoResultDto`, che li riceve già e li scarta per `ignoreUnknownKeys`, e `GeoFix`
      della posizione. Registrato in `UPSTREAM.md`
- [x] `:core:data/warnings/`: `WarningSource` (interfaccia) e `DpcBulletinSource` — scoperta,
      download, parser. Zip con `java.util.zip`, CAP con un pull-parser sottile che produce un
      intermedio piatto; la lettura di livelli e rischi dalle etichette («ORDINARIA CRITICITA'
      PER RISCHIO IDRAULICO / ALLERTA GIALLA») sta nel dominio, per parole chiave, con test su un
      CAP vero salvato come fixture (il primo file non Kotlin sotto `src/test`, e va bene così: un
      CAP inventato non proverebbe niente)
- [x] La scoperta del bollettino di criticità: Atom dei commit (18 KB) → `.diff` dell'ultimo
      commit (≈200 B) → `AAAAMMGG_HHMM` → `files/xml/<stamp>.zip` (7 KB). Ripiego
      `files/all/latest_all.zip` (4,7 MB) solo su rete non a consumo. La vigilanza non ha bisogno
      di scoperta: `files/<AAAAMMGG>.json` e `files/xml/<AAAAMMGG>.zip`
- [x] Persistenza: `OfficialWarningStore` (DataStore `warnings`: il bollettino corrente per fonte
      come JSON, più l'anello delle impronte notificate, mai dentro `settings`) e la tabella Room
      `warning_records` (migrazione 4→5, additiva) che il Diario legge
- [x] Il passo nel job: `OfficialWarningsStep`, chiamato da `WeatherSyncWorker` dopo il fetch,
      dentro `runCatching`, con la sua cadenza (sotto) e il suo motivo in `alertsWanted` e
      `shouldRun`, così un lettore che vuole solo le allerte tiene vivo il job
- [x] `SyncNotifiers.notifyOfficialWarning(...): Boolean`, `OfficialWarningNotifier` in `:app`,
      due canali, impronta bruciata solo su `true`; il fake in `WeatherSyncWorkerTest` cresce
- [x] Oggi: `WarningBanner` tra il chip di freschezza e le prossime ore; `WarningSheet`; il
      gradino in cima alla scala di `HeadlineEngine` per arancione e rosso, con la sua riga in
      `HeadlineText` nei due registri
- [x] Avvisi: il gruppo «Allerte ufficiali» in testa, con la card, i tre stati onesti,
      l'interruttore delle notifiche e il livello di partenza
- [x] Diario: la riga quando il livello cambia tra due bollettini, con il suo glifo di categoria;
      la riga «bollettino non raggiunto», una al giorno al massimo
- [x] Widget: il chip su Ora, Oggi e Arco, l'interruttore per istanza (`showWarning`, acceso),
      le regole di layout pure con tabella, il conteggio dei figli di Glance
- [x] DESIGN.md: §2.3 i tre token di livello misurati (giallo, arancione, rosso: inchiostro e
      contenitore, chiaro e scuro, ΔE fra loro sotto deuteranopia — il rosso può essere la
      coppia `fail`, il giallo NON è `unstable`: l'ambra della freschezza e il giallo di
      un'allerta sono due affermazioni diverse); §8.13 `WarningBanner`, `WarningSheet`, il chip;
      `ic_warning` disegnato con il peso dei segni di verdetto, mai il carattere ⚠
- [x] Guida: un paragrafo nel capitolo Avvisi (cos'è, chi la emette, quando arriva);
      attribuzione «Dipartimento della Protezione Civile, CC BY 4.0» nel foglio e in Informazioni
- [x] Stringhe IT/EN (`warning_*`, `notif_warning_*`, `notif_channel_warning_*`),
      `StringsParityTest` verde; `OfficialWarningNotifierTest` sul modello di `SkyNotifierTest`
- [x] Verifica su device, in Italia: chiusa dal committente l'11 set 2026 su varie località, due
      giorni dopo il merge invece dei sette previsti; il default del livello di notifica resta
      **gialla**. Quel che quei due giorni non hanno attraversato è sotto, in «Verifica»
- [x] **La lacuna dell'apertura**, trovata e chiusa l'11 set 2026: il passo non girava mai
      fuori dal job periodico. `WarningRefresh` è l'ingresso che mancava (sotto)

### Le misure che hanno deciso (9 set 2026)

Tutto verificato dal vivo quel giorno, non letto in una documentazione.

| Cosa | Misura |
|---|---|
| Bollettino di criticità | Entro le 16 (quello dell'8 set: emesso 15:19, su GitHub alle 15:43), oggi e domani, 156 zone di allerta (187 poligoni), tre rischi (idraulico, idrogeologico, temporali), quattro livelli — i temporali non hanno il rosso |
| Il suo CAP | `files/xml/<stamp>.zip` 7 KB → `Cap_<stamp>.xml` 45 KB; 8 blocchi `info` (rischio × livello × giorno), 209 aree con geocodice `Zona di Allerta` (es. `Cala-5`, `Abru-A`), **nessun poligono**: le zone assenti sono verdi; `<note>` con i rinvii ai bollettini regionali (Lombardia, Campania, Liguria quel giorno) |
| Il join | I 187 nomi del TopoJSON sono unici e contengono tutti i 209 `areaDesc` del CAP; il DBF dello shapefile porta il codice (`Zona_all`) accanto al nome: è da lì che l'indice prende codice, nome e geometria |
| I comuni | Il TopoJSON elenca i comuni di ogni zona; encoding sporco («Citt� Sant'Angelo») e nomi troncati nella vigilanza («Belv», «Bidon», «Buddus») |
| Geocoding | `admin3` = «Comune di Segrate» anche per la frazione (Redecesio → Segrate, Bovisa → Milano); `country_code` ricevuto e oggi scartato da `toCity` |
| Scoperta | Atom dei commit 18 KB con titoli generici («Published by DPC Github Pipeline»), ~12 commit per bollettino, l'ultimo è la preview PNG; `.diff` del commit ≈200 B col nome del file; `files/all/latest_all.zip` è stabile ma pesa 4,7 MB (shapefile e PDF dentro); `files/xml/latest*.zip` non esiste |
| Vigilanza | Entro le 15, tre giorni, 71 zone con comuni, CAP 5 KB con la sola categoria «Precipitazioni previste» a quattro gradini (assenti, deboli, moderati, elevati); temperature, venti, visibilità e mare sono **prosa nazionale**, più un livello di 42 punti con `id_fenomeno` e 25 icone senza legenda testuale |
| GitHub dai telefoni | L'API REST ha 60 richieste l'ora per IP e gli operatori mobili condividono un IP fra migliaia di utenti: **non si usa**. Il raw e l'Atom dei commit sono il sito, non l'API |
| Il sito delle mappe | Nessun endpoint «ultimo bollettino» (cercato nel markup: niente) |
| Licenza | CC BY 4.0 per entrambi i repository; i README dicono «in fase di caricamento» dal 2020 |
| Radar (per memoria, VISION §12.5) | Radar-DPC: tile WMTS 3,5 KB a zoom 6, GeoTIFF VMI 619 KB a 1 km, ultimo frame vecchio di 11 minuti, 40 radar su 50 attivi, il parametro `time` ignorato dalle tile (tre richieste, tre immagini identiche byte per byte). Non si fa niente, per scelta del committente |

### La cadenza del passo

Il passo vive nel job condiviso e spende rete solo quando è dovuto; non ha un orologio suo.

- **Criticità**: dalle 15:30 locali finché non c'è un bollettino di oggi; poi un ricontrollo l'ora
  fino alle 21 (l'Atom: 18 KB) per cogliere un «Aggiornamento»; dopo le 21 niente fino a domani.
  Prima delle 15:30 il «domani» del bollettino di ieri **è** oggi: non c'è nulla da scaricare.
- **Vigilanza**: dalle 14:30, stesso schema; il nome è la data, un 404 vuol dire «non ancora».
- **All'apertura** di Oggi, se è passata l'ora e il bollettino di oggi non c'è: in silenzio, con
  lo stesso cancello del risparmio energetico della rilettura silenziosa; un trascinamento per
  aggiornare lo forza. Il passo è uno per dispositivo, non per luogo: un bollettino copre tutti.
- **Con l'intervallo a 120 minuti** il caso peggiore è vedere il bollettino due ore dopo: accettato
  e dichiarato, perché il banner stampa l'ora del bollettino, non l'ora della lettura.
- **Il passo è inerte** se nessun luogo salvato cade in una zona italiana (indice: prima il
  riquadro, poi il poligono).
- **Un fallimento** tiene il bollettino precedente e dice la sua data; una riga nel Diario al
  giorno, non a tentativo. Mai un errore che il lettore debba leggere in Oggi.

### Dove e quando si vede

**Oggi.** Il `WarningBanner` sta dopo il chip di freschezza e prima della card della guida: il
chip qualifica l'eroe (parla dei dati), il banner parla del mondo, quindi apre il contenuto.
`Surface` a tutta larghezza dentro `PagePadding`, angolo `medium`, contenitore del livello più
alto, altezza minima 56dp; a sinistra `ic_warning` 24dp nell'inchiostro del livello; prima riga
`titleSmall` «Allerta arancione per temporali» (più rischi: «Allerta arancione per temporali,
gialla per rischio idrogeologico», in ordine di livello); seconda riga `bodySmall` «Oggi fino a
mezzanotte · Protezione Civile, bollettino delle 15:19» — oppure «Domani · …», «Oggi e domani · …».
Un solo annuncio TalkBack. Tocco → il foglio. Si disegna solo se per oggi o domani il livello
massimo è almeno giallo e il bollettino non è scaduto (scade a mezzanotte del suo «domani»);
altrimenti non esiste, e il verde non si annuncia qui. Arancione e rosso prendono anche la frase
in cima — gradino zero della scala di `HeadlineEngine`, oggi prima di domani — «Allerta arancione
per temporali, oggi» / «Domani allerta rossa per rischio idraulico»; il giallo no, perché in
autunno il giallo è uno stato frequente e una frase uguale un giorno su tre smette di essere
letta. Le righe della settimana non portano segni: il budget della `DayRow` a 360dp è già speso
(DESIGN §8.5) e il banner nomina il giorno.

**Il foglio** (`WarningSheet`, un `ModalBottomSheet`). Titolo «Allerta per *Nodo idraulico di
Milano*» con la regione sotto. Una griglia: una riga per rischio, due colonne (Oggi, Domani), in
ogni cella la **parola** del livello nel suo contenitore — «nessuna» in grigio `unknown`, mai una
cella vuota, mai un colore da solo. Poi «Cosa vuol dire», il significato del livello più alto con
le parole del Dipartimento (la fonte citata nel commento della stringa: gialla, fenomeni
localizzati e possibili disagi; arancione, fenomeni diffusi o intensi e possibili danni; rossa,
fenomeni molto intensi e pericolo per le persone) — è la riga «ogni numero dice cosa farne» di
questa superficie. Poi la `<note>` del bollettino, solo se nomina la regione del luogo,
etichettata «Nota del bollettino» e citata com'è. Poi, quando c'è, la vigilanza: «Precipitazioni
previste: elevate domani» per la zona di vigilanza del luogo. In coda la fonte con la data e
l'ora («Dipartimento della Protezione Civile · bollettino dell'8 settembre, 15:19 · CC BY 4.0») e
«Apri il bollettino», che porta alla pagina del bollettino sul sito del Dipartimento.

**Avvisi.** Un terzo gruppo, in testa, «Allerte ufficiali»: la stessa card del banner, che apre lo
stesso foglio, oppure uno dei tre stati che il banner non deve mai disegnare perché qui l'assenza
è la risposta cercata: «Nessuna allerta per *Nodo idraulico di Milano* · bollettino delle 15:19»;
«Bollettino di ieri: il prossimo esce di solito entro le 16»; «Le allerte ufficiali sono
disponibili per i luoghi in Italia» (la riga che la Fase 12 cambia). Sotto, l'interruttore delle
notifiche con la descrizione esatta di cosa manda e quando — «Quando la Protezione Civile emette
un'allerta per questa zona, appena esce il bollettino, di solito tra le 15 e le 17. Mai più di
una per bollettino» — e la riga «Avvisami da: gialla / arancione». Vive qui e non nelle
Impostazioni per il motivo della Fase 6: accanto a ciò che governa. Il permesso si chiede al
primo interruttore che si accende, come per gli altri tre.

**Diario e guida.** Una riga quando, per il luogo attivo, un livello compare, sale o scende tra due
bollettini consecutivi — «Allerta arancione per temporali, domani (Protezione Civile, 15:19)»,
«Allerta rientrata: nessuna criticità per domani» — con un glifo di categoria suo, silhouette
Material come le altre cinque; e la riga del bollettino non raggiunto. La guida aggiunge un
paragrafo al capitolo Avvisi che dice cosa è un'allerta, chi la emette e quando arriva; non
giustifica l'assenza fuori dall'Italia, dice cosa fa in Italia.

### Le notifiche

**Quando.** All'ingestione di un bollettino — nuovo identificativo, oppure stesso identificativo
con un livello massimo più alto (l'«Aggiornamento») — se per il luogo attivo il massimo tra oggi
e domani è almeno il livello scelto e l'impronta `"$cityKey:warn:$bulletinId:$maxLevel"` non è
ancora bruciata. Un livello che scende non notifica: lo scrive il Diario. Il luogo attivo e basta,
come tutto il resto del job; le città appuntate ai widget restano fuori, registrato sotto.

**Due canali**: `warning_high` («Allerte arancioni e rosse», `IMPORTANCE_HIGH`) e
`warning_yellow` («Allerte gialle», `IMPORTANCE_DEFAULT`). Due perché Android permette al lettore
di zittire il giallo dal sistema senza perdere l'arancione, e la riga «Avvisami da» fa lo stesso
dall'interno: due strade oneste per la stessa scelta. Id fisso `3000 + (cityId % 1000)`, `3000`
per la posizione: una notifica per luogo alla volta, un bollettino nuovo sostituisce il vecchio.
Categoria `STATUS`, icona piccola dell'app, nessun colore e nessuna azione, come le altre.

**Chiusa.** Titolo «Allerta arancione · Milano». Testo, una riga: «Temporali, oggi fino a
mezzanotte · bollettino delle 15:19»; con due rischi «Temporali e rischio idrogeologico, oggi ·
…»; per domani «Temporali, domani · …».

**Espansa** (`BigTextStyle`). La prima riga è la frase chiusa — il test del non-scostamento di
`SkyNotifierTest` vale anche qui — poi una riga vuota, poi un fatto per riga, ciascuna presente
solo se ha i suoi dati, in quest'ordine:
- «Oggi: temporali arancione · idrogeologico giallo»
- «Domani: temporali giallo»
- «Zona di allerta: Nodo idraulico di Milano»
- «Cosa vuol dire: …» — il significato del livello più alto, le parole del Dipartimento
- «Nota del bollettino: …» — solo se la nota nomina la regione del luogo
- «Dipartimento della Protezione Civile · bollettino delle 15:19» — sempre ultima

Il tocco apre l'app su Oggi, dove il banner è la prima cosa sotto la tela: nessun deep link, che
oggi non esiste e non serve qui. `OfficialWarningNotifierTest` come `SkyNotifierTest`: la forma
della chiusa, le righe dell'espansa, `@Config(qualifiers = "it")`, la chiusa uguale alla prima
riga dell'espansa, e nessun codice di zona o identificativo nel testo.

### I widget

I widget del sistema accanto ai quali Chiaro vive mostrano una riga quando il servizio nazionale
emette un'allerta: è un'abitudine del lettore, non un'invenzione. La regola segue quella della
frase, e si scrive come funzioni pure con tabella (`*LayoutTest`), come tutto il resto:

- **Arancione e rosso**: la frase (`HeadlineText`, registro breve: «Allerta arancione · temporali»)
  già lo dice. Con la frase accesa nessun chip — sarebbe la stessa cosa detta due volte; con la
  frase spenta il chip prende lo slot della frase, così l'allerta arriva sul widget comunque.
- **Giallo**: la frase non lo porta mai, quindi il chip ha una riga sua dove la forma ha spazio
  per una riga in più — Ora largo e alto, la riga eroe di Oggi (sotto la frase), il pannello e la
  card dell'Arco — e mai sulle forme a una riga, né sul quadrante e sulle strisce dell'Arco.
- **Il chip**: `ic_warning` 12dp nell'inchiostro del livello più la parola («Allerta gialla»)
  a 11sp sul contenitore del livello, angolo 10dp, padding 7/3 — la grammatica del `VerdictChip`
  del widget Cielo. `contentDescription` in parole. I colori passano da
  `palette.dress.colors(palette.darkGround)`: il fondo della card, non il tema del telefono (la
  trappola già documentata nel widget Cielo). Contare i figli di ogni contenitore Glance toccato.
- **L'interruttore per istanza**: `WidgetLook.showWarning` (Ora, Oggi) e `ArcSettings.warning`
  (Arco), **accesi** di default: in un giorno senza allerta non cambiano nulla, e in un giorno con
  l'allerta sono la riga che un lettore meno vorrebbe che il widget tacesse. Cielo non lo offre:
  quella card parla dei momenti del cielo, e un chip sul suolo sarebbe un secondo soggetto. Le
  anteprime del picker non cambiano: l'allerta non è lo stato che si pubblicizza.
- **I dati**: `WidgetModel.warning: PlaceWarnings?` caricato in `WidgetData.load` dallo store,
  mai dalla rete al momento del disegno; il passo che scrive lo store chiama `repaintAll`, e lo
  store entra nel collettore di processo di `ChiaroApplication`.

### Decisioni della fase (prese in pianificazione, 9 set 2026)

- **Geometria prima dei nomi.** I nomi dei comuni nei file sono sporchi (encoding, troncature) e
  una frazione non è un comune; il punto-nel-poligono sulle zone semplificate funziona anche per
  la posizione GPS e non dipende da nessuna stringa. Il comune resta il ripiego per un punto sul
  confine. Il costo è un asset da misurare (≤ 400 KB) e un algoritmo puro con la sua tabella.
- **La scoperta passa dal sito, non dall'API**, per il tetto per IP condiviso dagli operatori
  mobili. Atom più diff: due richieste piccole al giorno per dispositivo. `latest_all.zip` è il
  ripiego, non la via, per i suoi 4,7 MB.
- **Store più tabella**, non una colonna su `weather_history`: un bollettino è un documento con
  una validità, non un'osservazione di un fetch (l'argomento di `sky_runs` non si applica), e
  deve comparire in Oggi anche nei giorni in cui nessun fetch è riuscito. La tabella è per il
  Diario e per «cosa è cambiato tra due bollettini».
- **La frase la prendono arancione e rosso, non il giallo.** Motivo sopra: frequenza. Il banner
  copre il giallo.
- **Il verde si dice una volta, in Avvisi.** In Oggi l'assenza non si disegna (DESIGN §1.1);
  in Avvisi è la risposta a chi è venuto a controllare.
- **Default del livello di notifica: gialla.** È il livello che il Dipartimento stesso
  definisce «prestare attenzione» ed è quello che i lettori italiani sentono nominare; il canale
  a importanza normale evita l'heads-up. Si tiene o si alza dopo la settimana di collaudo, con i
  bollettini veri contati.
- **Il luogo attivo e basta**, come il resto del job. Le città appuntate ai widget non notificano
  (registrato in «Rimasto fuori»).
- **Nessun deep link**: Oggi mostra il banner per primo. Quando una superficie lo pretenderà, il
  costo è `MainActivity` più `ShellTab`, e sarà quella fase a pagarlo.
- **Vigilanza: solo la classe delle precipitazioni, solo nel foglio, senza notifica.** Non è
  un'allerta ed è l'unica parte strutturata; il livello dei fenomeni ha 25 icone senza una
  legenda testuale documentata, e inventarne una sarebbe una schermata che mente.
- **Le parole dell'autorità si citano, non si traducono** (VISION §8). Nota, significati e
  descrizioni restano in italiano anche per il lettore inglese, etichettate come della fonte;
  livello, rischio, giorno e zona localizzano.
- **Il giallo non è l'ambra.** `unstable` dice «dato vecchio» e «cielo incerto»; un'allerta gialla
  è un'affermazione di un'autorità. Tre coppie nuove in `ChiaroColors`, misurate; il rosso può
  coincidere con `fail` se la misura lo permette, e si scrive se lo fa.
- **Attribuzione** CC BY 4.0 nel foglio, in Informazioni e nella guida: è un obbligo della
  licenza, ed è anche l'unico posto in cui il lettore scopre da dove viene l'allerta.
- **Confermate dal committente il 9 set 2026** le tre scelte che il piano dava per provvisorie:
  il default dalla gialla, il solo luogo attivo, il chip dei widget acceso.

### Rimasto fuori, con il motivo

- Le notifiche per le città appuntate ai widget: il job notifica il luogo attivo, e cambiare
  questo per le allerte sole sarebbe una regola in più da spiegare.
- I bollettini regionali (ogni Regione ha il suo sito e il suo formato) e gli orari di validità
  che il nazionale rimanda a loro: il foglio mostra la nota e si ferma.
- Il livello dei fenomeni della vigilanza (punti e icone) e la prosa nazionale su venti e
  temperature: senza legenda e senza un luogo, non c'è modo onesto di legarli a una città.
- IT-alert: è cell broadcast del sistema, non un feed.
- Una mappa delle zone: Chiaro non ha una mappa, e la decisione sul radar (VISION §12.5) è
  aperta apposta.

### Riferimenti per chi apre la fase (raccolti il 9 set 2026)

**Gli URL.** Base raw `https://raw.githubusercontent.com/pcm-dpc/<repo>/master/files/`, con
`<repo>` = `DPC-Bollettini-Criticita-Idrogeologica-Idraulica` oppure
`DPC-Bollettini-Vigilanza-Meteorologica`. Lo User-Agent dell'app passa dall'interceptor
condiviso: su `raw.githubusercontent.com` è l'unico host dove conta davvero.

- Criticità: `xml/<stamp>.zip` (dentro `Cap_<stamp>.xml`, `BCR_testo_DCAT_AP_IT.xml`,
  `README.txt`); `shp/<stamp>_shp.zip` (DBF con `Zona_all`, `Nome_zona`, `Criticita`, `Idrogeo`,
  `Temporali`, `Idraulico`; 187 record; è la sorgente dell'importatore); `topojson/<stamp>_today.json`
  e `_tomorrow.json` (proprietà `Nome zona`, `Comuni`, `Per rischio idraulico|temporali|idrogeologico`,
  `Rappresentata nella mappa`; 1,2 MB); `<stamp>.json` l'indice; `all/latest_all.zip` il ripiego.
  `<stamp>` = `AAAAMMGG_HHMM`; **`20260908_1519` è la fixture di test**, riscaricabile.
- Scoperta: `https://github.com/pcm-dpc/<repo>/commits/master.atom`, poi
  `https://github.com/pcm-dpc/<repo>/commit/<sha>.diff` del primo `<entry>`: la prima riga
  (`diff --git a/files/preview/<stamp>_domani.png …`) porta lo stamp. Entrambi dal sito, non
  dall'API.
- Vigilanza: `<AAAAMMGG>.json` (chiavi `today`, `tomorrow`, `aftertomorrow`, ciascuna con
  `attachment[]`, `topo_json[]`, `html_description`); `xml/<AAAAMMGG>.zip` (dentro
  `Cap_<AAAAMMGG>.xml`); `topojson/<AAAAMMGG>_oggi.json` (proprietà `Nome_Zona`, `comuni`,
  `id_classificazione`, `Quantitativi_previsti`; 71 zone).

**Il CAP della criticità.** `<identifier>DPC_BULLETIN_2026_09_08_6471</identifier>`,
`<sender>2.49.0.0.380.1</sender>`, `<sent>` con offset locale, un `<note>` nazionale con i
rinvii regionali; 8 `<info>`, ognuno con `<event>` nella forma «ORDINARIA CRITICITA' PER RISCHIO
IDRAULICO / ALLERTA GIALLA:» (parole chiave: IDRAULICO, IDROGEOLOGICO, TEMPORALI; GIALLA,
ARANCIONE, ROSSA), `<onset>`/`<expires>` che dicono il giorno, `<severity>` Moderate o Severe, e
le `<area>` con `<areaDesc>` = nome della zona e `<geocode>` `Zona di Allerta` = codice. Nei
file le etichette dei livelli sono «Assenza di fenomeni significativi prevedibili / NESSUNA
ALLERTA», «Ordinaria / ALLERTA GIALLA», «Moderata / ALLERTA ARANCIONE», «Elevata / ALLERTA
ROSSA».

**Il CAP della vigilanza.** 10 `<info>`, `<category>Precipitazioni previste</category>`,
`<event>` fra «Assenti o non rilevanti», «Deboli», «Moderati», «Elevati», `<geocode>` `id_zona`
numerico, `<onset>`/`<expires>` per il giorno.

**Punti d'aggancio nel codice** (stato al 9 set 2026, da rileggere prima di toccarli):

- `core/sync`: `SyncNotifiers.kt` (l'interfaccia e `SyncDependencies`); `WeatherSyncWorker.kt`
  (i passi: fetch → fuso e `cityKey` → `AlertEngine` → `RuleEngine` → città appuntate → cielo;
  il nuovo passo va dopo `AlertEngine`); `SyncScheduler.alertsWanted` e `shouldRun`.
- `app/notifications`: `AlertNotifier.kt` (canali creati pigramente, id fissi 1001-1003,
  `BigTextStyle`, `openApp`); `SkyNotifierTest.kt` (il modello del test); `ChiaroNotifiers.kt`.
- `core/data`: `ServiceLocator.build()` (l'OkHttp condiviso è una variabile locale: va issato in
  un campo per il quarto host; `overrideForTests`); `remote/dto/GeocodingDto.kt` e
  `WeatherRepository.toCity` (dove `country_code` si perde); `domain/model/GpsLocation.kt` e
  `CityStore.adoptGpsFix` (la seconda strada di `City`); `local/WeatherHistory.kt` (migrazioni
  1→4 e `ChiaroDatabaseMigrationTest`, che fissa a mano gli schemi vecchi); `AlertStateStore.kt`
  (l'idioma dell'anello di impronte in un DataStore suo).
- `app/ui/today`: `TodayUiState.kt` (`Content.whatChanged`, il modello per un campo riempito dal
  ViewModel e non dal builder); `TodayScreen.kt` `ContentState` (la `LazyColumn`: il banner dopo
  `FreshnessChip`, prima di `GuideCard`); `HeadlineEngine.kt` (la scala, primo che combacia
  vince) e `HeadlineText.kt` (i due registri).
- `app/ui/alerts`: `AlertsScreen.kt` (`ReadySwitch`, `somethingTurnedOn()` per il permesso);
  `AlertsViewModel.mutate` (riconcilia il job dopo ogni modifica).
- `app/ui/theme`: `ChiaroColors.kt` (quattro palette, le due Brillante generate da
  `tools/gen_vivid.py`: non si ritoccano a mano); `PaletteContrastTest`; `PaletteDocTest` (legge
  i rapporti stampati in DESIGN §2.3); `NoRawColorTest`; `ui/StringsParityTest`; `ui/format/Formats`.
- `app/widget`: `WidgetLookStore.kt` (`WidgetLook`, chiavi `*_$id`, `forget`);
  `arc/ArcSettings.kt` (`ArcSettingsCodec`, il test che il codec scrive esattamente le chiavi che
  legge); `WidgetData.kt` (`WidgetModel`, `load`); `WidgetUi.kt` (`verdictInk`/`verdictContainer`
  con il fondo della card, non il tema); `SkyWidget.kt` `VerdictChip` (la grammatica del chip);
  i quattro `*LayoutTest`.

**Sequenza suggerita, un PR per passo**, ognuno verde da solo: (1) dominio, importatore, asset,
i due campi di `City`, la voce in `UPSTREAM.md`; (2) sorgente, store, tabella e migrazione, passo
nel job, notifier con il suo test; (3) Oggi, foglio, Avvisi, Diario, guida, token in DESIGN,
stringhe; (4) widget, con il giro di screenshot su device. La settimana di collaudo parte al
merge del (3).

### Il primo PR: dominio, importatore, asset, i due campi di `City` (9 set 2026)

Branch `claude/warnings-domain-9set-k3p8q1`. Tutto misurato sui file veri del bollettino
`20260908_1519`, scaricati quel giorno.

**Le zone sono 187, non 156.** Il DBF dello shapefile ha 187 record, 187 codici `Zona_all`
distinti e 187 nomi distinti, un record (multi-parte) per zona; i 187 nomi del TopoJSON sono
gli stessi. Il «156 zone (187 poligoni)» della tabella delle misure era un conteggio sbagliato:
niente cambia nel modello, cambia il numero che si legge qui. Le altre misure di quel giorno
reggono, con due precisazioni sui file che questo PR legge: nei 8 182 nomi di comune del
TopoJSON della criticità **non c'è nessun U+FFFD** (il «Citt� Sant'Angelo» sta nella vigilanza,
che legge il secondo PR), e l'unico danno di codepage nei nomi di zona è un apostrofo perso in
due nomi della Valle d'Aosta (`Valle d?Aosta`), che l'importatore ripara perché un `?` fra due
lettere in un toponimo non può essere altro. **Tredici zone hanno per nome il loro codice**
(sette della Basilicata, sei delle Marche: «Basi-A1», «Marc-4»): la Regione non ha mai dato
loro un nome, e «nessun codice di zona raggiunge lo schermo» dovrà fare i conti con loro prima
che il foglio esista — decisione del terzo PR, registrata qui perché la misura è di questo.
Altre due misure che servono a chi scrive il foglio: 273 comuni stanno in più di una zona
(Roma in cinque), e per Reggio Calabria il Dipartimento scrive «Reggio di Calabria» dove
Open-Meteo dice «Reggio Calabria» — il ripiego per nome fallisce e la geometria vince, che è
la prova di «geometria prima dei nomi» su un capoluogo.

**L'importatore** (`tools/build_warning_zones.py`, solo libreria standard: DBF e SHP letti a
mano, come `import_meteocons.py` non chiede pacchetti). La regione viene dal prefisso del
codice (venti prefissi, tabella nel file; un prefisso ignoto ferma la build). La geometria:
Douglas-Peucker per anello in un riferimento metrico (la longitudine scalata a 42°N), poi
quantizzazione a 10⁻⁴ gradi (11 m di latitudine, 8 di longitudine) con codifica a differenze
lungo l'anello; gli anelli che collassano sotto il triangolo si scartano (633 → 457: scogli e
isolotti). La tolleranza è scritta nell'asset e l'indice la conosce.

| Tolleranza | Punti | Asset (con 8 295 chiavi di comune, ~130 KB) |
|---|---|---|
| grezzo | 136 562 | — |
| 100 m | 73 067 | 630 KB |
| 250 m | 34 260 | 389 KB |
| **500 m** | **18 723** | **284 KB** |
| 1000 m | 10 130 | 219 KB |

**Scelti 500 m**: 250 m spende tutto il budget dei 400 KB; la strada della posizione arrotonda
già il fix su una griglia di ~1,1 km (`City.cacheKey`, fino a 680 m di spostamento) e un
comune geocodificato sta a chilometri dal bordo della sua zona, quindi niente che l'app
localizzi è più fino di mezzo chilometro. La fascia di bordo che l'indice cede al comune è
larga quanto la tolleranza in ogni caso. Il JSON usa chiavi leggibili (`code`, `name`,
`region`, `rings`, `comuni`): abbreviarle avrebbe reso ~8 KB. I nomi dei comuni sono
normalizzati una volta, nell'importatore, e allo stesso modo nel dominio
(`WarningZoneIndex.normalizeComune`, con il commento «cambiare entrambi»): NFD senza segni
combinanti, minuscolo, apostrofi raddrizzati, spazi compressi; i nomi bilingui
(`Bolzano/Bozen`) si indicizzano per metà (8 182 voci → 8 295 chiavi); solo il lato della
ricerca toglie un «Comune di » iniziale, che è come Open-Meteo scrive `admin3` (ma «Roma» e
«Genova» arrivano senza).

**L'indice** (`WarningZoneIndex`, puro, decodifica il JSON con kotlinx.serialization in
0,12 s nel test compresi 10 casi). `locate(punto, comune?)` decide in tre passi, ognuno solo
se il precedente non ha deciso: (1) una zona che contiene il punto a più della tolleranza dal
suo bordo è certa (due zone certe sarebbero una sovrapposizione della sorgente: il comune, poi
la più profonda); (2) nella fascia di bordo — dentro un poligono o entro la tolleranza da uno
— decide il comune se nomina esattamente una delle zone lì, altrimenti la zona in cui il punto
è più dentro, altrimenti la più vicina (una spiaggia che la costa semplificata ha spostato a
terra); (3) fuori da ogni poligono il comune da solo, se appartiene a una zona sola. Il
contenimento è pari-impari su tutti gli anelli della zona, così i buchi si contano da soli e
l'orientamento non serve; il filtro sul riquadro (allargato della tolleranza) viene prima.
`WarningZoneIndexTest` legge **il file che l'app spedisce** (la cartella degli asset di
`:core:data` è una risorsa di test di `:core:domain`, dichiarata in `build.gradle.kts` con il
motivo), e fissa 22 capoluoghi e comuni con le coordinate del geocoding del 9 set: tutti per
sola geometria, tra cui Perugia nella fascia di bordo (decide la profondità, 172 m) e Palermo
sulla costa (decide il comune); tre punti in mare e Lugano in nessuna zona; il confine
Piemonte/Lombardia a 45.0353 N 8.8225 E dove il comune sposta il punto da una parte o
dall'altra e un comune di un'altra zona non lo sposta; Genova 3 km al largo per nome; Roma per
nome da sola in nessuna zona perché è in cinque; `Forlì`/`FORLI`/`Comune di Forlì` e `Bozen`.

**Il modello** (`warnings/WarningModel.kt`): `WarningLevel` in ordine di gravità così `maxOf` e
`>=` dicono quel che dicono; `WarningHazard` con `displayOrder` = la precedenza del Dipartimento
a pari livello (Idraulico, Temporali, Idrogeologico, dal README dello shapefile), che tutte le
superfici useranno; `ZoneWarning` è la riga piatta (zona, giorno, rischio, livello) e
`WarningBulletin` elenca solo le righe sopra NESSUNA — quel che non nomina è verde, come le zone
assenti dal CAP; `PlaceWarnings` porta la zona, i giorni ancora davanti con tutti e tre i
rischi per giorno, e la nota. **Il verde è un valore**: `forPlace` torna `null` solo senza zona
o con tutti i giorni passati; una zona verde torna con tutto a NESSUNA, che è la riga «Nessuna
allerta» di Avvisi. I giorni passati si tolgono (prima delle 15:30 il «domani» di ieri è
l'unico giorno). La nota viaggia solo se nomina la regione della zona **o la zona stessa** (le
due zone trentine si chiamano come la loro provincia autonoma), confrontata normalizzata e
senza trattini («Friuli Venezia Giulia» nella nota, «Friuli-Venezia Giulia» nell'indice).
`notificationFor` costruisce l'impronta `"$cityKey:warn:$bulletinId:$maxLevel"` e legge le
impronte bruciate **per prefisso**: stesso bollettino a un livello uguale o più alto già
notificato → silenzio, così l'«Aggiornamento» che sale è notizia e la correzione che scende è
del Diario. `OfficialWarningEngineTest`, 18 casi in tabella.

**`City` e le due strade.** `countryCode` e `admin3`, nullable con default (le liste salvate
prima decodificano identiche; `encodeDefaults` è falso, quindi i null non si scrivono).
`GeoResultDto` ha `admin3` (il `country_code` arrivava già), `toCity` passa entrambi;
`GeoFix` ha gli stessi due campi, `toGpsCity` li porta, `adoptGpsFix` li tratta come il nome
(li aggiorna se il fix li sa, li tiene se no), `geocodedPlace` legge `Address.countryCode` e
la prima `locality` della scala — il comune anche quando il nome è un quartiere. **È la prima
divergenza voluta di `LocationProvider.kt` da tweather**, quattro righe additive, registrata
in `UPSTREAM.md` con la raccomandazione di portarle a monte.

**`WarningZoneAssets`** in `:core:data/warnings/` è l'unica riga Android: apre l'asset e lo
passa al dominio; il suo test Robolectric prova che l'asset è nella libreria e si apre.

Verifica del PR: `:core:domain` 195 test, `:core:data` 182, tutti verdi al primo giro; la suite
completa come la CI (`test :app:testDebugUnitTest :app:lintDebug`) 639 test — dominio 195, dati 182,
sync 11, app 251 — e lint 0 errori, nessun avviso sui file nuovi.

### Il secondo PR: sorgente, store, tabella, passo nel job, notifier (9 set 2026)

Branch `claude/warnings-source-9set-m7r2v4`, impilato sul primo (il #24 non era ancora fuso).
Quattro caselle in più della lista: `:core:data/warnings/`, la scoperta, la persistenza con la
migrazione, il passo nel job, `SyncNotifiers` e il notifier. Le stringhe del notifier entrano
qui (`notif_warning_*`, `notif_channel_warning_*`, `warning_*`), `StringsParityTest` verde.

**Misure nuove, dal vivo il 9 set.** Il bollettino del giorno è uscito alle 15:46
(`20260909_1546`, su GitHub alle 15:56): CAP di 79 KB, 10 blocchi, 385 aree, nota «Regioni
Lombardia e Liguria … ai rispettivi bollettini regionali». **L'Atom dei commit onora
`If-None-Match`**: la richiesta condizionale torna 304 con zero byte, quindi il ricontrollo
orario di un bollettino già in mano non costa niente (`WarningFetchState.feedTag`, l'ETag
salvato nello store). **Il `.diff` di un commit non è sempre 200 B**: quello di un commit di
metà pipeline (il GeoJSON) pesa 5,5 MB. La sorgente legge la prima riga e chiude la
connessione — lo stamp sta nel nome del file, qualunque file sia — e prova in ordine i tre
commit più recenti, saltando quelli senza stamp (un commit al README). `latest_all.zip`
contiene il CAP accanto a shapefile e PDF, quindi il ripiego su rete non a consumo funziona e
lo stamp si legge dal nome dell'entry. Un 404 su `xml/<stamp>.zip` vuol dire «pipeline non
arrivata»: si tiene il bollettino precedente e si dimentica l'ETag, così il giro dopo rilegge
l'Atom per intero.

**La sorgente** (`WarningSource`, `DpcBulletinSource`): scoperta dal sito, mai dall'API;
`CapParser` è un pull-parser sottile (`android.util.Xml`) che produce il `CapAlert` piatto e
non sa nulla di livelli; `DpcBulletinReader`, nel dominio, legge rischio e livello dalle
etichette per parole chiave (IDRAULIC, IDROGEOLOGIC, TEMPORAL; GIALLA, ARANCIONE, ROSSA,
NESSUNA ALLERTA) e **salta un blocco che non sa leggere invece di indovinarlo**. I giorni del
bollettino sono gli onset dei blocchi uniti al giorno d'emissione e al successivo, perché un
giorno tutto verde non ha blocchi in cui trovarsi. Le fixture sono file veri: il CAP dell'8 set
(45 KB), l'Atom del 9 (18 KB) e il diff del primo commit (216 B), i primi file non Kotlin sotto
`src/test`; il test della sorgente usa un interceptor OkHttp come trasporto e **asserisce le
richieste fatte**, non solo il risultato (tre per un bollettino nuovo, una per il 304).

**La cadenza è pura** (`WarningFetchPolicy`, tabella): niente in mano → ogni giro chiede;
bollettino di ieri prima delle 15:30 → no; dalle 15:30 finché non c'è quello di oggi → sì;
quello di oggi in mano → un'occhiata l'ora fino alle 21. **Il confronto fra due bollettini è
puro** (`WarningDiff`): il primo bollettino che un luogo vede è il suo stato, non una notizia
— nessuna riga nel Diario; un giorno che il vecchio non copriva parte da verde; un cambio di
zona non si confronta.

**Il passo** (`OfficialWarningsStep`, in `:core:sync`) gira **prima del cancello
`alertsWanted`**, subito dopo il fetch riuscito, dentro `runCatching`: il bollettino è contenuto
per Oggi e per i widget, non solo una notifica, e se parlare lo decide il suo interruttore.
`alertsWanted` include `officialWarnings`, così chi vuole solo le allerte tiene vivo il job.
Inerte se nessun luogo salvato cade in una zona. La rete a consumo si legge da
`ConnectivityManager.isActiveNetworkMetered` (permesso dichiarato nel manifest di `:core:sync`,
già fuso da WorkManager). La riga «bollettino non raggiunto» si scrive solo dopo le 15:30, se
quello di oggi manca, una volta al giorno (`failureLoggedOn` nello store). **Accettato**: un
fetch meteo fallito salta anche il passo (offline lo sono entrambi; un `ApiError` di Open-Meteo
fa uscire il worker prima) e il giro successivo recupera.

**Le impostazioni**: `NotificationSettings.officialWarnings` (acceso) e `officialWarningsFrom`
(gialla), chiavi `notif_official_warnings` e `notif_official_warnings_from`, additive; i loro
interruttori in Avvisi sono del terzo PR. **Lo store** (`OfficialWarningStore`, DataStore
`warnings`): il bollettino corrente per sorgente come JSON con le date ISO, l'ETag, l'ultimo
tentativo, il giorno dell'ultima riga di fallimento, l'anello delle impronte (40). Una riga con
un rischio o un livello che questa build non conosce si scarta in lettura, non si indovina.
**La tabella** `warning_records` (migrazione 4→5, additiva; `ChiaroDatabase.MIGRATIONS` è ora
l'unica lista, condivisa da builder e test) porta `kind` (`LEVEL_CHANGE` o `BULLETIN_MISSED`),
il bollettino, la zona, il giorno, il rischio e i due livelli, tutti come nomi e stringhe ISO:
il Diario li rende nella lingua del lettore al momento della lettura.

**Il notifier** (`OfficialWarningNotifier`): due canali (`warning_high` alta, `warning_yellow`
normale), id `3000 + id % 1000` e `3000` per la posizione, titolo «Allerta arancione · Milano»,
chiusa «Temporali, oggi fino a mezzanotte · bollettino delle 15:46», espansa con la chiusa in
testa e poi un fatto per riga (i livelli per giorno, la zona, «Cosa vuol dire», la nota se c'è,
la fonte per ultima). Due grammatiche per il livello: la frase è «Allerta gialla», il colore
nelle righe dei giorni è «temporali giallo». **I significati dei livelli sono il riassunto che
il piano dava, in italiano nelle due lingue** (`warning_meaning_*`): non una citazione
letterale; se il committente vuole le parole esatte del Dipartimento con la loro fonte, il
terzo PR sostituisce tre stringhe. `OfficialWarningNotifierTest` sul modello di
`SkyNotifierTest`, con il controllo che né codice di zona né identificativo raggiungano il
testo.

**Rinviato al terzo PR, con il motivo: la vigilanza.** Vuole un secondo asset (le 71 zone di
vigilanza dal TopoJSON `_oggi.json`, con i loro comuni) che il primo PR non ha costruito, un
secondo documento nello store e la sua cadenza dalle 14:30; la sua unica superficie è una riga
del foglio. Entra con il foglio, non prima.

**`ServiceLocator`** issa OkHttp e il database in campi (la sorgente condivide il client per lo
User-Agent, il DAO condivide il database) e cresce di quattro accessori e di quattro parametri
in `overrideForTests`.

Verifica del PR: suite completa come la CI, **695 test** — dominio 213 (+18), dati 201 (+19),
sync 21 (+10), app 260 (+9) — e lint 0 errori. Un solo giro rosso, per due sviste del test e
non del codice: il CAP dell'8 set dava il Versante Jonico Settentrionale giallo su tutti e tre i
rischi, non sul solo idraulico come il test supponeva (il file ha ragione, il test è stato
corretto e dice perché); e `cancelAll` va chiamato sul gestore vero, non sulla shadow.

### Il terzo PR: Oggi, il foglio, Avvisi, il Diario, la guida, i token, le stringhe (9 set 2026)

Branch `claude/adoring-bell-viiqyl`, impilato sui primi due. Sei caselle della lista: le
superfici. Da qui il bollettino si vede, e da qui parte la settimana di collaudo.

**I tre token di livello, misurati.** Una regola sola per tutti e tre, così l'unica cosa
che cambia fra loro è la tinta: l'inchiostro a **8,5:1** sul fondo chiaro e a **11,0:1** su
quello scuro, il contenitore al bordo del gamut sRGB per la sua tinta a luminanza tenuta.
I contenitori stanno **più in basso di quelli dei verdetti** (luminanza .62 contro .78) e
il motivo è una misura: a .78 sRGB tiene .099, .046 e .038 di croma per queste tre tinte e
arancione e rosso escono lo stesso rosa pallido; a .62 i tre tetti sono .175, .088 e .075 e
il giallo è un giallo. L'inchiostro rosso chiaro cade a due unità da `fail` (`#990003`
contro `#950700`) — è quello che fanno una tinta condivisa e un contrasto condiviso — e
resta un token suo, perché le due affermazioni non sono la stessa. Tutti e sei escono dal
generatore identici: `gen_vivid.py` cresce a sette coppie (`PAIRS`) e non ha niente da
prendere su valori già al bordo.

**La deuteranopia si misura, non si cita.** `Deuteranopia.kt` nei sorgenti di test è il
validatore: simulazione Viénot–Brettel–Mollon 1999 su sRGB lineare, distanza in ΔE\*ab
(CIE76), soglia percettiva 2,3. Il risultato in tabella in DESIGN §2.3: **in ogni schema uno
dei due portanti tiene circa nove decimi della sua distanza e l'altro ne tiene meno di un
decimo** — l'inchiostro collassa su carta, il contenitore collassa sullo scuro, e
arancione↔rosso è la coppia debole in entrambi i casi. `PaletteContrastTest` asserisce il
collasso come *tetto* (l'unica asserzione al contrario del file) e `PaletteDocTest` legge i
numeri stampati. Il ΔE 0,7 che §2.3 cita per `unstable`↔`fail` viene dal validatore di
tweather su una scala che qui non si riproduce (questo ne misura 6,4): la frase resta com'è
e il documento dice che le due cifre non sono confrontabili, invece di riscrivere una
misura che non si può rifare.

**Le tredici zone senza nome** (le sette della Basilicata, le sei delle Marche) sono la
decisione che il primo PR ha rinviato qui. In una frase si nomina la regione — «Allerta per
**una zona della regione Marche**» — e nella notifica la riga «Zona di allerta:», che dopo i
due punti non avrebbe altro che un codice, **non si disegna**: è la regola che quella
notifica già segue per ogni riga. Il sostantivo sta dentro la stringa apposta, perché
l'italiano vuole «in Basilicata» e «nelle Marche» e quelle due Regioni sono esattamente le
tredici.

**Il gradino zero della frase** prende arancione e rosso, mai il giallo, e il giorno lo
sceglie oggi prima di domani; i rischi nominati sono quelli **al picco del giorno
nominato**, così «arancione per temporali» non diventa «per temporali e rischio
idrogeologico» perché il secondo era giallo. Le due forme sono parallele («… , oggi» / «… ,
domani») invece della «Domani allerta rossa …» che il piano dava: stessa frase in due
lingue senza far girare le maiuscole, e il registro breve dei widget resta «Allerta
arancione · temporali».

**Il verde è un valore, e vale in un posto solo.** `TodayStateBuilder` scarta un bollettino
tutto NESSUNA prima di costruire: niente banner e niente frase (§1.1). Avvisi disegna tutti
e quattro gli stati di `PlaceWarningState` — non in zona, nessun bollettino ancora,
bollettino non più valido per oggi, e quello corrente anche se verde — perché lì l'assenza
**è** la risposta cercata. È la stessa lettura in due screen con due regole opposte, ed è
il motivo per cui lo stato è un tipo (`OfficialWarningReader.state`, puro, sei casi in
tabella) e non un `PlaceWarnings?`.

**Il glifo del Diario si è mosso.** Il triangolo di Material passa alla riga dell'autorità —
è il segno dell'allerta in tutto il resto dell'app — e le due righe «un aggiornamento non è
arrivato» (il fetch fallito, il bollettino non raggiunto) prendono `Refresh` e condividono
una categoria sola. La regola del Diario non cambia: il glifo nomina la categoria, quindi
due categorie non possono averne uno.

**Il vocabolario è uno**, `ui/warnings/WarningText`, e la notifica del secondo PR ci si è
appoggiata invece di tenersi le sue mappature: due copie di «Allerta arancione per
temporali» divergono, e la divergenza si vede come una notifica e un banner che parlano di
bollettini diversi. Da lì `PlaceWarnings.ranked` e `peakDays` sono saliti nel dominio, dove
il widget del quarto PR li troverà già scritti.

**La lettura non chiede mai rete.** `OfficialWarningReader` segue lo store; l'indice delle
zone è un *fornitore* e non un campo, così un lettore con tutti i luoghi all'estero non
decodifica mai i 290 KB dell'asset. Il giorno con cui si legge è quello dell'**emittente**,
e il minuto di Oggi lo rivaluta: una pagina lasciata aperta oltre la mezzanotte smette di
mostrare i livelli di ieri.

**Rinviata al quarto passo, con la misura: la vigilanza.** Il secondo PR l'aveva mandata
qui «col foglio». Misurato il 9 set: il TopoJSON delle 71 zone pesa **807 KB grezzi**, è in
TopoJSON (archi delta-codificati con `transform`, 1 893 archi) e non in shapefile, quindi
l'importatore della criticità non lo legge — gli manca un decodificatore di archi che non
ha mai avuto — e le sue proprietà sono `Nome_Zona`, `comuni`, `id_classificazione`,
`Quantitativi_previsti`: **non c'è `id_zona`**, che è il geocodice con cui il CAP della
vigilanza nomina le zone, quindi il join va rimisurato prima di scriverlo. Sono un asset
nuovo da costruire e da pesare, un secondo documento nello store, una seconda cadenza e una
seconda sorgente, per una riga del foglio: è un PR suo, e ci va con la sua ricerca, non
appoggiato a una funzione finita.

Verifica del PR: suite completa come la CI, **731 test** — dominio 213 (invariato), dati
207 (+6), sync 21 (invariato), app 290 (+30) — e lint 0 errori, nessun avviso sui file nuovi (l'unico che era comparso,
`UseKtx` su `WarningSheet`, è stato tolto passando a `String.toUri()`).

### Il quarto PR: il chip sui widget, e due contenitori Glance che perdevano figli (9 set 2026)

Branch `claude/adoring-bell-viiqyl`, sopra il terzo. L'ultima casella di codice della
fase: l'allerta sulla schermata home.

**La regola è una tabella, e sta in un posto solo** (`WidgetWarning.kt`, `warningSlot`).
Quattro card dovevano essere d'accordo su quando la frase sta già dicendo l'allerta, e
quattro copie di una regola sono quattro occasioni per non esserlo. Decide quello che la
card **sta già dicendo**: la frase del giorno nel registro breve *è* l'arancione e il
rosso, quindi dove quella frase c'è la pastiglia non si disegna; dove non c'è — il lettore
l'ha spenta, o l'eroe dell'Arco sta mostrando il prossimo momento di luce — la pastiglia
prende il suo posto e la card non cresce di niente. Il giallo la frase non lo porta mai,
quindi vuole una riga sua e compare solo dove la forma ne ha una in più.

**La frase del widget ora porta davvero l'allerta.** `WidgetData.load` costruiva il
contenuto senza passare il bollettino a `TodayStateBuilder`, quindi il gradino zero della
frase non arrivava mai su una card: la pastiglia si sarebbe disegnata accanto a una frase
che dell'allerta non diceva niente — il widget e l'app che raccontano due pomeriggi
diversi, che è la cosa che quell'oggetto esiste per impedire. Il bollettino si legge ora
**prima** del contenuto, e il contenuto si costruisce con quello.

**Il costo è un numero solo** (`warningChipHeight`, 20,52 dp alla dimensione di default,
più 4 di aria) e ogni budget lo sottrae. Dove la riga non c'è, la pastiglia non si
disegna: sul 2×2 di riferimento del widget Ora con la frase accesa il glifo scenderebbe a
44,4 dp contro il pavimento di 52 della famiglia, quindi il giallo resta a casa; con la
frase spenta le sue due righe la pagano due volte. Su Oggi la riga della pioggia cede per
prima, come già cede al marcatore di dato vecchio. Sull'Arco l'agenda molla una riga prima
che il disegno molli un pixel (pannello di riferimento: intestazione 39,6 → 64,1, agenda
2 → 1, arco fermo ai suoi 56 preferiti).

**I colori vengono dal fondo della card** (`WidgetPalette.colors`), mai dal tema del
telefono: una card chiara sotto un tema scuro porterebbe l'inchiostro del set scuro sul
contenitore di quello chiaro, e la coppia smetterebbe di essere quella misurata. È la
stessa correzione che il segno dell'agenda dell'Arco ha già avuto il 9 set.

**Contando i figli di Glance sono venuti fuori due contenitori che ne perdevano.** Il
limite è dieci per contenitore e oltre quello Glance lascia cadere senza dire niente (la
scoperta del 9 set sull'Arco). Misurati tutti quelli che questa fase tocca, e due erano
già oltre:

| Contenitore | Prima | Ora |
|---|---|---|
| Striscia delle ore di Oggi | 7 celle + 6 spaziatori = **13** | 7 |
| Colonna della card Cielo (forma alta) | 3 + 5 righe + 4 spaziatori + 1 = **13** | 9 |
| Colonna della frase di Ora (largo) | 1 | 2 |
| Colonna delle parole di Ora (alto) | 4 | 5 |
| Colonna di coda di Oggi | 2 | 3 |
| Colonna della card dell'Arco | 6 | 7 |
| Colonna del pannello dell'Arco | 6 | 7 |

Sulla striscia di Oggi il conto è **esattamente la card su cui il widget è disegnato**:
quattro celle sul dispositivo di riferimento danno sette ore, e le ultime due erano
scartate in silenzio. Sul Cielo servono cinque righe sotto l'eroe, che una card da tre
righe con sei sottoscrizioni ha. La cura è quella che l'Arco ha già usato: gli spazi come
**padding dentro una scatola più alta**, non come spaziatori — la geometria non cambia di
un dp e il contenitore ha un figlio per riga invece di due. Le celle della striscia
prendono metà spazio per lato invece dell'intero tra l'una e l'altra, così restano una
griglia sola: l'inchiostro della striscia cede 3 dp per capo, ed è tutta la differenza
visibile su una card da 340. La pastiglia è **un figlio solo** per la stessa ragione
(`WarningChipRow`: l'aria sopra è il padding del contenitore, non uno spaziatore).

**L'interruttore per istanza è acceso** su tutte e tre: `WidgetLook.showWarning` (Ora,
Oggi) e `ArcSettings.warning` (Arco, diciottesima chiave del codec). Cielo non lo offre —
quella card parla dei momenti del cielo, e una pastiglia sul suolo sarebbe un secondo
soggetto. Le anteprime del picker non cambiano. L'anteprima dell'Arco sì, e deve: disegna
dallo stesso piano e dallo stesso slot, perché una schermata di impostazioni che mostra
una pastiglia che la home non disegnerebbe è esattamente il bug che quel file esiste per
non avere.

**Il ridisegno.** Il passo che scrive il bollettino chiama `repaintAll` — un bollettino può
arrivare in un giro il cui meteo veniva dalla cache, e allora il gancio del commit del
repository non scatta mai — e lo store entra nel collettore di processo di
`ChiaroApplication`, accanto al luogo attivo e alle impostazioni.

**Il lettore non paga l'asset se non deve.** `OfficialWarningReader.graded` chiede prima
allo store e solo dopo all'indice: il passo è inerte se nessun luogo salvato cade in una
zona, quindi chi ha tutti i luoghi all'estero non ha nessun bollettino in memoria e non
decodifica mai i 290 KB su un ridisegno della home. Il test lo prova con un fornitore
dell'indice che lancia.

Verifica del PR: suite completa come la CI, **754 test** — dominio 213, dati 208 (+1),
sync 21, app 312 (+22) — e lint 0 errori, nessun avviso sui file nuovi. Resta il giro di
screenshot su device, che è del committente.

### Verifica

Suite come la CI: **754 test** — dominio 213, dati 208, sync 21, app 312 — e lint 0 errori
(quarto PR, 9 set 2026).

**Il collaudo, chiuso dal committente l'11 set 2026.** Doveva durare una settimana ed è durato
due giorni: varie località verificate contro i bollettini veri. Quel che questo conferma e quel
che non conferma, perché la differenza conta:

- **Confermato**: la zona che l'indice trova per un luogo, il banner e il foglio su bollettini
  veri, il livello di notifica di partenza (**gialla**, non si alza).
- **Non attraversato**, perché due giorni di settembre non li contengono: un'allerta rossa, un
  «Aggiornamento» che alza il livello a bollettino invariato, la riga «bollettino non raggiunto»,
  e il passaggio di mezzanotte con la pagina aperta. Restano coperti dai test e non dal campo,
  ed è giusto che il documento lo dica invece di far passare due giorni per una settimana.

### La lacuna dell'apertura (trovata l'11 set 2026, ancora aperta)

Il piano della fase diceva, in «La cadenza del passo»: «**All'apertura** di Oggi, se è passata
l'ora e il bollettino di oggi non c'è: in silenzio… un trascinamento per aggiornare lo forza».
**Non è mai stato cablato.** Verificato l'11 set cercando tutti i chiamanti:

- `OfficialWarningsStep` è invocato da **un posto solo**, `WeatherSyncWorker`;
- `SyncScheduler.reconcile` accoda solo lavoro **periodico** (`enqueueUniquePeriodicWork`), mai
  un colpo singolo;
- il trascinamento di Oggi chiama `repository.getWeather(forceRefresh = true)` e **non tocca il
  passo**.

Conseguenza: su un'installazione fresca non compare **nessuna** allerta — italiana o no — finché
WorkManager non fa scattare il job, e con l'intervallo di default (`DefaultUpdateFrequencyMin`
= **60**) può essere un'ora. Il collaudo italiano non l'ha vista perché l'app era in uso da
giorni e il job aveva già girato; si è vista solo installando l'APK e guardando subito.

Non era un difetto di correttezza — il dato arrivava, tardi — ma rendeva la funzione
impossibile da provare in trenta secondi.

**Chiusa lo stesso giorno** con `sync/WarningRefresh`, l'unico ingresso che permette a una
schermata di far girare la gamba delle allerte del job. Oggi lo chiama all'apertura e sul
trascinamento; il parametro `force` del passo esisteva già ed era proprio per questo.

Tre decisioni, ognuna piccola e ognuna con il suo motivo:

- **Scarica, non parla.** Il passo accetta ora `notifiers: SyncNotifiers?` e **null vuol dire
  «questa esecuzione non parla»** — un concetto solo invece di un flag più un oggetto finto.
  È la stessa divisione che il resto dell'app segue già: aprire Oggi scarica il meteo e non
  ha mai mandato una notifica; parlare è del job. L'impronta resta **non bruciata**, quindi
  se il lettore chiude l'app prima di accorgersene il job glielo dice lo stesso.
- **Una alla volta.** Oggi è un pager: atterrarci sottoscrive un flusso per luogo salvato e
  ognuno chiede. La cadenza **non** li ferma su un'installazione fresca, dove `shouldFetch`
  risponde sì finché non c'è un bollettino in mano — cinque luoghi sarebbero stati cinque
  download dello stesso documento. Il primo vince, gli altri tornano subito: volevano tutti
  lo stesso documento, e il collettore dello store lo consegna a ogni pagina comunque.
- **Il risparmio energetico vale qui come per il meteo**: l'esecuzione che nessuno ha chiesto
  si salta sotto risparmio, mai il trascinamento e mai una pagina che non ha ancora nessun
  bollettino.

Verifica: **757 test** (sync 21 → 24), lint 0 errori. I tre test nuovi fissano quel che conta
— un'esecuzione muta tiene il contenuto, non parla, non brucia l'impronta, e scrive lo stesso
nel Diario, perché il diario è contenuto e non parola. **Non testato**: il cablaggio dentro
`TodayViewModel`, che non ha test perché quella classe non è mai stata costruita in un test;
le tre righe stanno alla stessa copertura del `fetch` che hanno accanto.

## Fase 12 — Allerte ufficiali, secondo strato: MeteoAlarm

Pianificata il 9 set 2026 insieme alla Fase 11; parte quando la 11 ha passato la sua settimana.
Aggiunge un adattatore e cambia una riga: il modello, le superfici, le notifiche e i widget sono
quelli della Fase 11. **In Italia continua a fare fede la Protezione Civile**: il feed italiano di
MeteoAlarm non si legge mai.

- [x] Spike, fatto l'11 set 2026 (sotto). L'EDR è ancora chiuso, il ripiego per nome che questa
      casella dava non regge alla misura, e le geometrie esistono pubblicate altrove
- [ ] **Prima di riaprire la fase**: la lacuna dell'apertura della Fase 11 (sopra), e il
      «fallire chiuso» che il prototipo ha dimostrato mancante (sotto). Le caselle che seguono
      sono state tutte scritte e misurate su un branch l'11 set 2026, e **non sono state fuse**:
      il motivo è in «Il prototipo, e perché non è stato fuso»
- [ ] `MeteoAlarmSource`: un Atom per ogni paese dei luoghi salvati fuori dall'Italia
      (`meteoalarm-legacy-atom-<paese>`, ~80 KB per l'Italia oggi), con richieste condizionali
      (ETag / If-Modified-Since — da verificare nello spike: ogni giro del job che scarica 80 KB
      per paese è 2 MB al giorno, un 304 costa niente); il CAP solo per le voci dell'area del
      luogo; lo stesso pull-parser della Fase 11, perché il CAP è lo stesso CAP 1.2
- [ ] La mappatura: `awareness_level` 2/3/4 → giallo/arancione/rosso; `awareness_type` →
      `WarningHazard`, che cresce dei quattordici tipi (vento, neve e ghiaccio, temporali,
      nebbia, caldo, freddo, eventi costieri, incendi, valanghe, pioggia, alluvione,
      pioggia-alluvione, pericolo marino, siccità) con le loro parole in IT e EN
- [ ] I testi: i blocchi `info` arrivano in inglese e nella lingua nazionale; si sceglie quella
      del lettore se c'è, altrimenti l'inglese, e si cita (VISION §8). `onset` ed `expires` sono
      ore precise: la riga «dalle 11 alle 23» sostituisce «oggi» nel banner e nella notifica
- [ ] Precedenza per `countryCode`: `IT` → solo DPC; altrove MeteoAlarm; la riga «disponibili per
      i luoghi in Italia» di Avvisi diventa «in Italia e in 32 paesi europei», e fuori da entrambi
      resta onesta
- [ ] Attribuzione come i termini chiedono: il servizio nazionale (`senderName`) e l'ora di
      emissione nel banner, nel foglio e nella notifica; «EUMETNET – MeteoAlarm» solo se mai si
      mostrassero più paesi insieme, cosa che un luogo alla volta non fa
- [ ] Impronte con l'identificativo CAP e il livello; stesse due canali, stessi id
- [ ] Widget e Diario: nessun lavoro, stesso modello — ed è il test che la Fase 11 ha disegnato
      bene

### Lo spike, e le tre cose che ha cambiato (11 set 2026)

Misurato dal vivo quel giorno, non letto in una documentazione. Lo spike doveva sciogliere una
domanda (l'EDR è aperto?) e ne ha sciolte tre, due delle quali cambiano il piano.

| Cosa | Misura |
|---|---|
| EDR | Ancora **chiuso**: `/edr/v1/collections?f=json` risponde 200 e descrive i filtri, `/edr/v1/collections/warnings/locations` risponde **401 `{"error":"Unauthorized"}`**. Invariato dal 9 set |
| Richieste condizionali | **Non onorate.** Nessun `ETag` e nessun `Last-Modified` su **nessuno** dei 39 feed; `If-Modified-Since` torna 200 col corpo intero; `Range: bytes=0-1023` torna 200 col corpo intero; `Accept-Encoding: gzip` non comprime (85 251 B chiesti, 85 251 B serviti). `cache-control: max-age=0, private, must-revalidate`, e nessun validatore da rivalidare |
| `HEAD` | Funziona e dà `content-length`. **Scartato come validatore**: «Yellow» e «Orange» hanno le stesse sei lettere, quindi il salto di livello — il caso che più conta — non muove la lunghezza |
| Il feed `<updated>` | **È un timestamp di contenuto, non di generazione**: 5 letture in 2 minuti su IT, AT e FR danno lo stesso `<updated>` e corpi identici byte per byte. Sta a un offset stabile di **1 381–1 385 B**, cioè dentro la prima `recv` |
| I 39 feed | 24 h di misura: 22 attivi, 17 vuoti (986 B). Totale 1 295 KB, media 33 KB, massimo **200 KB** (Israele), poi Austria 182, Slovenia 170 |
| `cap:event` | **60 forme distinte** fra i 22 feed attivi: «Yellow Thunderstorm Warning», «Thunderstormwarning», «Thunderstorms Level 1», «EXTREME HIGH TEMP», «near gale», e un produttore che perde il suo template (**`awareness_type=5, awareness_level=2`**, 47 voci). Inutilizzabile |
| `cap:severity` | Moderate 592, Severe 87, **Minor 28**: tre valori per quattro livelli, e il Minor non è nessuno dei tre colori. Inutilizzabile |
| I geocodici del feed | `EMMA_ID` in 14 paesi, **`NUTS3`** in FR/RO/MK, **`NUTS2`** in HU, **nessun geocodice** in EE/IL/NO/SI e in parte LV/NL. L'unico campo che c'è sempre è `cap:areaDesc` |
| `areaDesc` = la regione? | **No.** È admin1 solo in IT, RO e PT; altrove sono distretti (AT: 86 Bezirke), dipartimenti (FR), zone meteo (ES: «Costa - Ibiza y Formentera»), **bacini fluviali** (SI: «Gradaščica», «Ljubljanica») o **aree di mare** (NO, FI, EE, LV) |
| Le geometrie, dove sono | `MeteoAlarm_Geocodes_2026_07_31.json` nel progetto GitLab `meteoalarm-pm-group/documents` che i riferimenti già citavano per il profilo CAP: **33 MB di GeoJSON, 2 006 poligoni veri, 30 paesi, 935 750 punti**, proprietà `code`/`country`/`name`/`type`, tutti `EMMA_ID`, CRS84, CC BY 4.0. Quattro versioni datate nel repo, la più recente del 31 lug 2026 |
| La copertura | Incrociando i 707 avvisi dei 22 feed attivi con quel file: **413 per codice, 68 per nome, 226 non risolti = 68,0%**. Ma per paese: **17 paesi su 22 risolvono il 100%**; i non risolti sono EE (0/57), IL (0/77), NO (0/8), SI (0/73) e LV (9/20), e i loro nomi sono «Port of Tallinn», «Sea - Center», «Northern Baltic Sea», «Morje» — **mare e bacini fluviali, non luoghi in cui si abita** |

**Le tre cose che cambiano.**

1. **Le geometrie ci sono, e non dall'EDR.** La casella diceva «se l'EDR è aperto, di là;
   altrimenti una tabella di nomi». L'EDR è chiuso e la tabella di nomi non regge (riga
   `areaDesc` sopra): funzionerebbe per tre paesi su ventidue. La terza strada — un file
   ufficiale, pubblicato, versionato per data, con i poligoni veri e la stessa licenza dei feed —
   fa quello che l'EDR avrebbe fatto e lo fa offline. È la stessa forma della Fase 11: un
   importatore, un asset, un indice puro punto-nel-poligono.
2. **Il livello e il rischio si leggono solo dal CAP.** Il piano supponeva che `cap:event`
   («Orange Wind Warning») bastasse a evitare di scaricare il CAP per ogni voce. Con 60 forme e
   un produttore che stampa il proprio template, leggere il colore dall'`event` vuol dire
   sbagliarlo in silenzio: `awareness_level` e `awareness_type` nei `<parameter>` del CAP sono
   gli unici campi codificati, e sono quelli che si leggono. Il CAP si scarica sempre, per le sole
   voci dell'area del luogo (2,9 KB l'uno, misurato su AT).
3. **La cadenza non scende a un'ora.** «Da sciogliere» prevedeva che senza richieste condizionali
   il passo scendesse a una lettura l'ora per paese. Non serve: il `<updated>` del feed è un
   timestamp di contenuto e sta nei primi 1 385 byte, quindi la sorgente legge il prefisso,
   confronta e **chiude la connessione** — lo stesso idioma con cui `DpcBulletinSource` legge
   la prima riga di un `.diff` da 5,5 MB. Una lettura invariata costa una `recv`, non 33 KB.

**Quel che lo spike non ha potuto misurare**, e va detto: 17 dei 39 feed erano vuoti quel giorno
(fra loro CH, SE, UK, UA, LU, AD), quindi la loro forma di geocodice non è osservata, solo
supposta dal file delle geometrie. E EE, IL e NO non sono **proprio** nel file delle geometrie:
per loro la fase non promette niente, e lo dice.

### Il prototipo dell'11 set 2026, e perché non è stato fuso

La fase è stata scritta per intero su `claude/meteoalarm-domain-11set-t4k9w2` — dominio,
importatore, 28 asset, sorgente, store, passo, superfici, 847 test verdi, lint 0 errori, CI
verde, APK installato — e **il committente ha deciso di non fonderla**. Il branch è stato
cancellato. Quel che segue è il motivo, perché è la parte che vale più del codice.

**Il difetto non era la Slovenia, era come falliva il join.** Quando l'area di una voce non si
collocava, lo schermo non taceva: **affermava**. Lubiana, provata su device, aveva 73 allerte
attive — una *Severe Rain Warning* — e l'app diceva «Nessuna allerta». Non un buco, una frase
falsa su un'informazione di sicurezza. E il guasto era **invisibile**: nessun errore, nessuna
riga nel Diario, niente che distinguesse «qui è tranquillo» da «non ho saputo collocare
nessuna delle 73 voci».

Vale per ogni paese e per sempre: se un servizio nazionale rinomina le sue aree, o MeteoAlarm
ripubblica il file dei geocodici con un'altra numerazione, ogni lettore di quel paese passa in
silenzio a «nessuna allerta».

**La misura che ha deciso.** Un audit che replica la logica di join contro i feed dal vivo,
28 paesi, 11 set 2026:

| Esito | Paesi |
|---|---|
| join verificato funzionante | 10 — AT, BA, BG, FR, GR, HU, MK, PT, RO, RS |
| **rotto** | 1 — **SI** |
| parziale, e **corretto**: perde solo le aree marine scartate di proposito | 6 — DE, ES, FI, HR, LT, PL |
| feed vuoto quel giorno, quindi mai osservato | 11 — BE, CY, CZ, DK, IE, IS, MD, ME, MT, NL, SK |

**Diciassette paesi su 28 erano una promessa non verificata**, mentre la schermata Avvisi
stampava con sicurezza «in Italia e in 28 paesi europei». Il numero era più sicuro di sé dei
dati dietro.

**Il caso sloveno, per chi riaprirà.** Il feed nomina le sue aree `Slovenia / Central`,
`Slovenia / North-East`; l'asset le chiama `Central`, `Northeast`. Il confronto falliva su due
cose insieme: il prefisso `<paese> / ` e il trattino. Riparabile — non serviva escludere la
Slovenia — ma il commento di `MeteoAlarmCountries` **dichiarava la Slovenia esclusa** e il
codice la spediva lo stesso, e nessun test se n'è accorto perché le assenze erano un elenco
scritto a mano che non la conteneva. È l'errore da non ripetere: la tabella dei paesi non va
verificata contro un elenco, va verificata **contro i feed veri**.

**Le due cose che renderebbero la fase fondibile**, e nessuna riguarda la Slovenia:

1. **Fallire chiuso.** Se un feed ha voci e **nessuna** si colloca, quello non è «nessuna
   allerta», è «non coperto». Si calcola con i dati già in mano, e trasforma una bugia
   silenziosa in un'assenza onesta. È la correzione che conta.
2. **L'audit come strumento ripetibile**, da lanciare prima di un rilascio e quando MeteoAlarm
   ripubblica il file dei geocodici. Fuori dalla CI: dipende dalla rete e dal meteo del giorno.

Con quelle due, la frase a schermo smette di essere «28 paesi» e diventa quello che è davvero:
i paesi in cui *adesso* sappiamo collocare un'allerta. La fragilità dei dati resta — è nella
natura di trenta produttori indipendenti — ma smette di essere silenziosa, che è l'unica cosa
che la rende accettabile per una funzione di sicurezza.

**Quel che il prototipo ha dimostrato che funziona**, e che non va rifatto da zero: il modello
a due emittenti in `:core:domain` (l'Italia isolata da `WarningIssuer.of(countryCode)` e mai
toccata), la richiesta condizionale costruita sul `<updated>` del feed, l'importatore con il
blocco marino, e il fatto che le superfici della Fase 11 reggono un secondo emittente senza
essere riscritte. Le misure sono tutte qui sotto e nello spike.

### Decisioni già prese

- **MeteoAlarm e non gli aggregatori**: il WMO Severe Weather Information Centre è un sito senza
  API (il suo JSON interno è piatto, senza geometrie, e rilancia i feed nazionali); l'Alert-Hub
  filtra per rettangolo e il feed Italia conteneva cento allerte slovene e svizzere e zero
  italiane il giorno della misura; OpenWeatherMap chiede una chiave, contro la promessa del
  prodotto. Misure del 9 set 2026, VISION §12.8.
- **Il feed italiano di MeteoAlarm non si legge**, nemmeno come complemento per vento e caldo che
  il bollettino di criticità non copre: la regola «una sola voce» vale più di due rischi in più, e
  il CAP stesso dice di non essere l'allerta ufficiale.

### Da sciogliere all'inizio della fase

- L'accesso all'EDR (sopra). Cambia il costo della fase, non il suo perimetro.
- Se le richieste condizionali non sono onorate dai feed: un Atom per paese per ogni giro del job
  è troppo a 15 minuti; in quel caso la cadenza del passo scende a una lettura l'ora per paese,
  e si dichiara.

### Riferimenti (raccolti il 9 set 2026)

- Feed: `https://feeds.meteoalarm.org/feeds/meteoalarm-legacy-atom-<paese>` (l'italiano,
  `-italy`, serve solo a leggere il formato: in Italia non si usa). Ogni `<entry>` porta
  `cap:geocode` `EMMA_ID` (es. `IT019`), `cap:areaDesc`, `cap:event` («Orange Wind Warning»),
  `cap:sent`/`effective`/`onset`/`expires`, `cap:severity`, `cap:message_type` (Alert, Update) e
  il link `application/cap+xml` a `https://feeds.meteoalarm.org/api/v1/warnings/feeds-<paese>/<uuid>`;
  il feed dichiara i termini in `<rights>` e un hub PubSubHubbub che non ci riguarda.
- CAP: un `<info>` per lingua (`en-GB` più la nazionale), `<parameter>` `awareness_level`
  («3; orange; Severe») e `awareness_type` («1; Wind»), `<senderName>`, `<description>`,
  `<instruction>`, `<web>`, `<area>` con `EMMA_ID` e nessun poligono.
- Livelli: 2 yellow Moderate, 3 orange Severe, 4 red Extreme. Tipi: 1 Wind, 2 snow-ice,
  3 Thunderstorm, 4 Fog, 5 high-temperature, 6 low-temperature, 7 coastalevent, 8 forest-fire,
  9 avalanches, 10 Rain, 12 flooding, 13 rain-flood, 14 Marine-Hazard, 15 Drought (l'11 non
  esiste).
- API: `https://api.meteoalarm.org/edr/v1/collections?f=json` risponde senza chiave e descrive
  i filtri; `…/collections/warnings/locations` risponde 401; `…/metadata/v1` è il portale.
  Stato del servizio `https://status.meteoalarm.org`; termini
  `https://meteoalarm.org/en/page/terms-and-conditions`; profilo CAP v2.0 nel progetto GitLab
  `meteoalarm-pm-group/documents`.

---

## Fase 13 — Le icone, da zero: Meteocons v3

Aperta l'11 set 2026 da una segnalazione del committente: dove Open-Meteo e le altre app
mostrano il sole, Chiaro mostra il sole dietro una nuvola. La verifica ha dato ragione alla
segnalazione e ha trovato sotto una causa più grande di quel singolo glifo — **il repo è
fermo a Meteocons v2.0.0, e la famiglia nel frattempo è stata rifatta**. Questa fase non
ripara una riga: rifà l'importazione.

### Il difetto che l'ha aperta

`ChiaroIcons.conditionLineRes` (riga 373) dà **lo stesso disegno** ai codici WMO 1 e 2:

```kotlin
1, 2 -> if (night) mc_partly_cloudy_night else mc_partly_cloudy_day
```

Non sono vicini: sono i due estremi della metà serena del cielo. La soglia di Open-Meteo è
già scritta in questo repo, in `WeatherReportMapper.skyCode` (riga 369) — 0 sotto il 20% di
copertura, 1 fra 20 e 49, 2 fra 50 e 79, 3 da 80 in su. Misurato dal vivo l'11 set 2026 su
10 città × 7 giorni = **1 680 ore** (`weather_code` + `cloud_cover`):

| codice | quota ore | copertura p10 / p50 / p90 |
|---|---|---|
| 0 | 28% | 0 / 0 / 10 |
| 1 | **17,2%** | **10 / 25 / 46** |
| 2 | 20% | 47 / 64 / 80 |
| 3 | 31% | 82 / 98 / 100 |

I due secchi non si toccano nemmeno fra il decimo e il novantesimo percentile. **Un'ora su
sei** viene disegnata al 64% di cielo chiuso quando ne ha il 25%. E la parola accanto dice
già la cosa giusta (`WeatherText.condition`: 1 → `cond_mostly_clear`, «Quasi sereno»),
quindi sull'hero il disegno **contraddice una parola visibile**; nella striscia oraria e
nella riga della settimana la parola non è visibile affatto (sta solo nella
`contentDescription`: `HourStrip.kt:97`, `TodayScreen.kt:1315`), quindi lì il glifo sbagliato
è l'unico portatore che il lettore ha.

Due difetti minori nella stessa tabella: il fallback `else -> mc_cloudy` disegna una nuvola
mentre la parola dice «Condizioni sconosciute» (`mc_not_available` è importato in tutti e
quattro i set e la mappatura dei codici non lo usa mai); e l'82 (rovesci violenti) va su
`rain` mentre 80 e 81 vanno su `partly_cloudy_*_rain`. E **la tabella non ha un solo test**:
`AnimatedIconTest` cammina `movingOf`, `IconContrastTest` misura i colori, `WidgetIconsTest`
copre la scelta dello stile, nessuno chiede mai che disegno prenda un codice. È per questo
che 1 e 2 hanno potuto fondersi senza che niente diventasse rosso.

### Perché si riparte da zero e non si tocca la riga

Meteocons v2.0.0 **non ha un disegno per «quasi sereno»**: `production/line/all` sono 122
icone e nella famiglia del cielo ci sono solo clear, partly-cloudy, overcast (+ day/night) e
cloudy. Misurato sugli SVG originali, in una scatola di 64: il sole di `clear-day` ha r 10.5
al centro; quello di `partly-cloudy-day` ha **r 4.5** (−82% di area del disco) sotto una
nuvola che occupa x 16→53.5, y 23.5→45.5; `overcast-day` è la stessa nuvola con un secondo
strato sopra, cioè **più** nuvoloso, non meno. Non c'è un gradino intermedio da scegliere.

**In v3 c'è.** La categoria si chiama `Mostly Clear` ed esiste in 18 varianti. Il resto della
famiglia è cresciuto nello stesso modo: `pollen-grass/tree/weed` nei quattro livelli,
`uv-index-1…11`, `barometer-low…extreme`, `windsock-calm/weak/moderate`,
`wind-beaufort-0…12`, `compass-n…nw`, `code-yellow/orange/red` e `weather-alert` — cioè
esattamente le bande che le schede Dettagli, il tile pollini e le allerte della Fase 11
calcolano già e disegnano con un glifo generico.

| | v2.0.0 (oggi) | v3 |
|---|---|---|
| Icone | 122 (line) | **519**, in 16 categorie |
| Stili | line, fill | **fill, flat, line, monochrome** |
| Formati | SVG con SMIL | SVG con SMIL (`@meteocons/svg`), SVG statico (`@meteocons/svg-static`), **Lottie** (`@meteocons/lottie`) |
| Scatola | 64 × 64 | 128 × 128 |
| Distribuzione | clone del repo al tag | npm + CDN `cdn.meteocons.com/{versione}/{formato}/{stile}/{icona}.{est}`, con `manifest.json` |
| Licenza | MIT | MIT |
| Importate in Chiaro | 49 su 122 | — |

Chiaro ne spedisce oggi 268 file XML, 1,7 MB: 49 icone × 4 set statici (`mc_`, `mcn_`,
`mcf_`, `mcfn_`) + 18 × 4 set animati (`mca_`, `mcan_`, `mcaf_`, `mcafn_`).

### Lo spike, prima di aprire le caselle

Tre domande, e finché non hanno risposta la fase non parte. La prima è l'unica che può
fermarla.

- [x] **Le maschere.** v3 usa `<mask>`, che VectorDrawable non ha. Sul sottoinsieme delle 70
      icone che Chiaro metterebbe a schermo: **line 31, flat 31, monochrome 31, fill 12**. Per
      `line` sono quasi tutti i cieli composti — `mostly-clear-day/night`,
      `partly-cloudy-day/night`, `overcast`, `fog-day/night`, gli `overcast-*`, i
      `partly-cloudy-*-rain/snow`, `extreme-rain`, i `thunderstorms-day/night` — quindi
      **il convertitore di maschere è obbligatorio**, non una comodità, e lo è qualunque sia
      il secondo stile.
      Il fatto che salva: sono **tutte `mask-type:alpha` con riempimenti binari bianco/nero,
      nessuna maschera in gradiente**, e la stragrande maggioranza usa `evenodd`. Una maschera
      «tutta la tela meno questa forma» si riscrive come `<clip-path>` se si **inverte il
      verso** della sottoforma (il parser Android applica NON-ZERO e `<clip-path>` non ha
      `fillType`). La maschera però **si muove** (porta la sua `animateTransform`: è la nuvola
      che va su e giù) mentre il sole sotto sta fermo, e in VectorDrawable la trasformazione di
      un gruppo si applica **sia al clip sia ai figli**. Serve quindi la coppia annidata:

      ```xml
      <group android:translateY="@anim">        <!-- muove il clip -->
          <clip-path android:pathData="tela meno nuvola"/>
          <group android:translateY="@anim-inverso">   <!-- rimette fermo il sole -->
              … il sole …
          </group>
      </group>
      ```

      **Fatto l'11 set 2026, e la geometria regge: 1 345 maschere su 1 345 identiche.**
      `tools/spike_mask_clip.py` non guarda un disegno, rasterizza: appiattisce ogni maschera
      dei quattro stili (bezier, archi e abbreviazioni comprese), calcola con una scanline
      l'area **evenodd dell'originale** e l'area **nonzero della versione convertita**, e le
      confronta su una griglia di 128 × 128. **Zero pixel di differenza**, su tutte e 519 le
      icone × 4 stili: 1 236 maschere a due sottopercorsi (tela + forma, quelle che hanno
      bisogno dell'inversione) e 109 a uno solo (dove le due regole già coincidono). 12,7
      secondi in tutto.
      **Anche la maschera che si muove regge, e si è guardata** (stesso giorno).
      `tools/spike_v3_convert.py` è il prototipo del convertitore: converte davvero un SVG
      v3 in `<vector>` + `<animated-vector>`, con l'inversione fatta **sui comandi** e non
      sulla polilinea (una cubica girata è `P1,C2,C1,P0`; un arco tiene i raggi e
      **inverte `sweep`**), e ri-rasterizza ogni conversione prima di scriverla. Quattro
      icone `line` — `clear-day`, `mostly-clear-day`, `partly-cloudy-day`, `overcast` —
      più `partly-cloudy-day-rain` e `thunderstorms-day`.
      **`./gradlew :app:assembleDebug` passa**: aapt accetta il viewport 128, il
      `clip-path`, i gruppi annidati, `fillType="evenOdd"` e i `pathInterpolator` generati
      dai `keySplines`.
      E `tools/icon_filmstrip.py` — insegnato a leggere il viewport invece dei 64 fissi —
      le ha disegnate a nove istanti del ciclo su **entrambe** le superfici: la nuvola
      va su e giù, **il sole sotto resta fermo**, i raggi sono tagliati dove passa la
      nuvola. La coppia di gruppi annidati fa quello che doveva fare.
      Due cose che il filmstrip ha reso visibili e che nessun numero aveva reso ovvie:
      la scala 0 → 1 → 2 → 3 finalmente **si legge come una scala** (sole pieno, sole con
      nuvoletta, sole dietro la nuvola, nuvola sola); e i colori originali sulla carta
      sono **fantasmi**, mentre sul fondo scuro sono nitidi. La misura del 73% / 13%
      guardata invece che calcolata.
      Quel che resta al telefono: che hwui renda la coppia annidata come la rende il
      filmstrip. Il rischio è sceso da «la fase può cadere» a «una verifica di resa».
- [x] **I colori originali su carta.** La misura sotto. Lo spike produce il filmstrip delle
      due ipotesi (originali / riancorati) e si decide guardando.
- [x] **La prerelease.** `@meteocons/svg` su npm ha `latest` = **0.1.0** e `next` =
      **3.0.0-next.10**: v3 non è ancora stabile. Verificare se esce una `3.0.0` prima di
      aprire le caselle; se non esce, si appunta la versione esatta come si è appuntato il tag
      v2.0.0, e `UPSTREAM.md` dice che è una prerelease e perché.
      **Sciolto l'11 set 2026**: la 3.0.0 non e' uscita, quindi si appunta la
      prerelease esatta — `@meteocons/svg@3.0.0-next.10` — come si appuntava il tag
      v2.0.0, e `licenses/Meteocons-MIT.txt` dice da quale pacchetto viene il testo.

### Il commento sui colori originali, con i numeri

«Lasciare i colori originali» semplificherebbe moltissimo: farebbe cadere il motivo per cui
oggi esistono quattro set statici invece di due. Ma non regge sulla superficie chiara.

Misurato sulle 70 icone che Chiaro metterebbe su uno schermo, contro le due superfici di
DESIGN §2.2 (carta `#FCF9F3`, scura `#16130E`), soglia 3:1 di §10. **I riempimenti delle
maschere sono esclusi**: `#fff` e `#000` lì dentro non sono inchiostro, e contarli gonfiava
il conto nella prima stesura di questa fase. «Inchiostro sotto soglia» pesa ogni colore per
quante volte è usato, che è la domanda vera:

| | fill | flat | line | monochrome |
|---|---|---|---|---|
| colori distinti | 44 | 35 | 31 | **1** |
| passano 3:1 su **entrambe** | 10 | 10 | 7 | 0 |
| inchiostro sotto soglia su **carta** | **77%** | **73%** | **73%** | 100% |
| inchiostro sotto soglia su **scura** | 7% | 5% | **13%** | 0% |

I più usati sono corpi e contorni di nuvola: `#e6effc` (26 usi in line) **1,10:1** su carta,
`#e2e8f0` (15 usi) **1,17:1**, `#86c3db` (27 usi) **1,84:1**, `#f8af18` — il sole — (33 usi)
**1,79:1**. Non sono «un po' chiari»: sono invisibili, ed è lo stesso difetto che
`import_meteocons.py` documenta per v2 (`#E5E7EB`, 1,18:1) — v3 non l'ha risolto perché
Meteocons è disegnato per un fondo neutro, non per la carta.

Una differenza fra gli stili che conta: **`line` è l'unico che fallisce in modo materiale
anche sul fondo scuro** (13% dell'inchiostro contro il 5–7% degli altri), per via di
`#1e293b` (29 usi, **1,27:1** su scuro) con cui disegna i contorni scuri. Quindi line va
riancorato su **entrambe** le superfici, fill e flat praticamente solo su carta. Che è
esattamente la forma dei quattro set che ci sono già.

E `monochrome` è la via di fuga che nessuno ha chiesto ma che va scritta: **un solo colore**,
quindi tingibile con un ruolo e conforme per costruzione. Non si prende, e il motivo è già in
DESIGN §13.1 — «una tinta piatta trasforma la famiglia in sagome». Resta lì come opzione se
un giorno servisse un set che non costa nulla in contrasto.

Quindi la risposta alla domanda «i colori originali possono causare problemi»: **sì, su carta
sempre, e su fondo scuro anche per line**. La proposta, che non blocca il lavoro e non butta
la regola:

- [x] Il tool importa **con i colori originali** come uscita di riferimento (`--original`), e il
      rimappaggio è un **passo successivo** sullo stesso albero: si generano entrambi e si sceglie
      davanti al filmstrip, anziché decidere prima
- [x] L'architettura «due set scelti dal fondo» (`styledRes(darkGround)`) **resta**: è già
      scritta, ha superato una revisione di design, e con i numeri qui sopra è esattamente la
      forma giusta. Ma la regola non è «originali sullo scuro, riancorati sulla carta» per
      tutti: **flat** può quasi tenere gli originali sullo scuro (5% di inchiostro sotto
      soglia), **line no** (13%, per i contorni `#1e293b`). Quindi quattro set come oggi, e
      quanto rimappaggio serva a ciascuno lo dice la misura, non la simmetria
- [~] **Non presa**, e si scrive perché era la terza strada: dare all'icona un
      **fondo suo** (una pastiglia tenue sotto il glifo nella striscia e nella settimana): fa
      passare il 3:1 senza toccare un colore. È una decisione di design, non di conversione —
      va in DESIGN §13.1, non qui

### I due stili: **flat + line**, e perché non fill

Il committente aveva detto «fill e flat», poi si è corretto in «fill e line», poi ha chiesto
se non fosse meglio «flat e line». La misura dà ragione alla terza versione, e per un motivo
più forte del conteggio delle maschere.

**v3 `flat` è quello che Chiaro già spedisce.** L'attuale set `mcf_*` è lo stile fill di v2
con **ogni gradiente appiattito sul colore di faccia** dal tool (dipartenza n. 4 nell'intestazione
di `import_meteocons.py`). Lo stile `flat` di v3 è esattamente quel disegno, ma **disegnato
così a monte** invece che derivato da noi. Scegliere `fill` vorrebbe dire o tenere i gradienti
— una capacità nuova, un rischio nuovo e un cambio visivo rispetto a oggi — o riappiattirli,
cioè rifare a valle un lavoro che l'illustratore pubblica già fatto. È il punto 5 della
richiesta («riusare le originali senza ricrearle») portato al massimo.

Misurato sulle 70 icone del sottoinsieme:

| | fill | flat | line |
|---|---|---|---|
| maschere da convertire | 12 | 31 | 31 |
| filtri da lasciar cadere | 1 (`compass`) | **0** | **0** |
| gradienti da appiattire o portare | 65 | 3 | **0** |
| tratteggi da ridisegnare | 2 | 2 | 2 |
| animazioni | identiche | identiche | identiche |

**La coppia flat + line è quella che chiede al tool il minor numero di riscritture**: zero
filtri, zero o quasi gradienti, e le maschere che servirebbero comunque perché `line` — lo
stile predefinito, quello che il lettore vede appena installa — ne ha 31 su 70. Una volta
costruito il convertitore per line, flat non costa una riga in più. Il conteggio delle
maschere, che nella prima stesura di questa fase faceva preferire fill, **smette di essere un
argomento** nel momento in cui line è uno dei due: va costruito comunque.

Va anche detto cos'è il `line` di v3, perché non è il `line` di v2: non è un contorno
tracciato, è **una forma riempita con `fill-rule="evenodd"`** — una ciambella. Per Android è
una buona notizia (`android:fillType="evenOdd"` esiste dall'API 24, e non c'è nessun tratto
da convertire); per il contrasto è la stessa notizia di sempre, perché quella ciambella è
riempita di `#e6effc`.

E la simmetria con oggi si conserva: **line resta il predefinito, flat è l'alternativa in
Impostazioni → Aspetto**, esattamente come line/fill adesso. Nessuna stringa da riscrivere,
nessuna scelta da spiegare di nuovo al lettore.

### Il commento sulla «reimportazione totale»

519 icone × 2 stili × (statica + animata) non stanno nell'app. Misurato sul repo: un
drawable statico pesa **~1,7×** il suo SVG, un animato **~3,5×**; l'SVG v3 medio è 4,1 KB,
quindi ~7 KB statico e ~14 KB animato.

Vale anche la regola di DESIGN §7.1, che dimezza il conto: **muove solo la famiglia delle
condizioni**, perché «il marchio di un tile etichetta una quantità, e un barometro che gira
per sempre è decorazione». Quindi 24 icone hanno il gemello animato e le altre 119 no.

| scenario | file | XML su disco |
|---|---|---|
| oggi (v2, 49 icone, 4 set statici + 4 animati) | 268 | 1,7 MB |
| **le 143 della lista, line + flat, 2 set per fondo** | **668** | **~5,2 MB** |
| le 143, line + flat, 1 solo set di colori | 334 | ~2,6 MB |
| tutte e 519, 2 stili, 1 set | 2 076 | ~21,8 MB |
| tutte e 519, 2 stili, 2 set per fondo | 4 152 | ~43,6 MB |

Tre volte il peso di oggi per tre volte le icone.

**Ma il peso su disco non è il peso nell'APK, e cambia la conclusione.** Misurato dentro
`app-debug.apk`: i 736 drawable della famiglia pesano 972 KB non compressi e **371 KB
compressi**, cioè **516 B l'uno** — l'XML binario più il deflate valgono un fattore 4,6
contro il testo su disco. Rifatto il conto in quella moneta: la lista da 143 aggiunge
**~1,2 MB** all'APK di release, il set completo da 519 ne aggiungerebbe **~3,5 MB**.

E c'è la terza strada, che è quella giusta e che il progetto ha già configurata
(`isShrinkResources = true`, `app/build.gradle.kts:63`):

- [x] **Si converte il set completo (519 × 2 stili), si generano le tabelle Kotlin solo per
      la lista di spedizione.** Un drawable che nessuna tabella nomina non è referenziato, e
      `shrinkResources` lo toglie dalla release: **zero byte** di costo per le icone non
      ancora usate, ~15 MB nel repo, e promuoverne una a «spedita» è **una riga**, senza
      reimportare niente e senza rete. Il repo diventa il set completo, l'APK resta la lista.
      **Provato l'11 set 2026 sul set intero, e «zero byte» era sbagliato.** Con tutte le
      1 974 icone v3 in `res/drawable/` e nessuna riga di Kotlin che le nomini, l'A/B di
      due build di release dà **6 676 125 B contro 6 548 541 B: +127 583 B**, cioè
      **65 byte a drawable**. E stanno *tutti* in `resources.arsc` (+127 464 B): i file in
      `res/` sono **1 565 in entrambe**, identici — AGP butta il disegno e tiene la voce
      in tabella. Quindi il set completo costa **125 KB, il 2% dell'APK**, non zero.
      L'argomento regge lo stesso e il ripiego previsto non serve, ma il numero è questo
      e non quello che avevo scritto

Quindi «totale» va inteso sul **convertitore**, non sull'APK: il tool deve saper convertire
qualunque delle 519 e dimostrarlo, e la tabella `ICONS` resta la lista di spedizione.
Crescerla è una riga.


### Blocco C — il colore, fatto (11 set 2026)

`tools/reanchor.py`. La regola della v2 era una **tabella a mano** (`REMAP`, `FILL_REMAP`,
`FILL_NIGHT`): una riga per ogni hex. La v3 ha il doppio dei colori e ne avrà altri,
quindi qui la regola è scritta come funzione e la tabella è il suo risultato.

Si tiene la tinta e si sposta la **luminanza**, perché il contrasto WCAG dipende solo da
quella: è l'unica leva che cambia il rapporto, ed è quella che si vede di meno. Due cose
però cambiano rispetto alla v2, e la seconda è un guadagno:

1. **Non si sposta un colore, si comprime una famiglia.** Una nuvola è tre grigi, e una
   regola applicata a ciascuno per conto suo può scambiarli. I colori si raggruppano per
   tinta (i neutri, sotto 0,03 di croma, stanno insieme) e la famiglia si comprime
   intera, tenendo fermo **l'estremo che già va bene**: contro un tetto resta fermo il più
   scuro e scende il più chiaro, contro un pavimento resta fermo il più chiaro e sale il
   più scuro. Ordine e spaziatura in L di Oklab si conservano.
2. **Ogni set deve una sola superficie.** v2 chiedeva a un set di reggere entrambe, il che
   lo inchiodava nella banda Y ∈ [0,120, 0,283] — ed è il motivo per cui il suo sole è un
   bronzo: a quella luminanza in sRGB il giallo non c'è. Qui il vincolo è un tetto
   (carta) **oppure** un pavimento (scuro), mai tutti e due, e sul fondo scuro il sole
   resta un oro vero (`#f8af18` → `#ffc25e`).

| | colori | identici all'originale | sotto 3:1 dopo |
|---|---|---|---|
| line, carta | 57 | 2 | **0** |
| line, scuro | 57 | 10 | **0** |
| flat, carta | 62 | 2 | **0** |
| flat, scuro | 62 | 12 | **0** |

Un colore che già reggeva e che la compressione lascia dov'era esce **identico**: il giro
per Oklch e ritorno sposterebbe l'ultima cifra, e cambiare un hex che non ne aveva bisogno
è rumore in un diff che qualcuno dovrà leggere. E la soglia non si dà per raggiunta: dopo
aver costruito il colore si **misura** il rapporto e si corregge di un passo alla volta
finché passa, che è la stessa regola con cui `PaletteContrastTest` asserisce l'esito e non
la ricetta.

I quattro set escono con lo schema della v2 — `mc3_`/`mc3n_` (line, carta e scuro),
`mc3f_`/`mc3fn_` (flat) più i loro gemelli animati — e il riancoraggio si applica **una
volta per stile**, non per icona, perché una famiglia si può spostare solo se la regola
vede tutti i suoi grigi insieme. Il flag `--original` emette anche i set non riancorati,
per il confronto al filmstrip; non si spediscono.

**Il costo, misurato:** 4 004 drawable, 39 MB nel repo, e nell'APK di release
**+257 996 B** (64 byte l'uno, lo stesso numero della misura precedente), cioè il 4%.

- [x] **39 MB nel repo**: **decisi l'11 set 2026 dal committente — non pesano.** La
      famiglia intera resta in git, e la lista di spedizione resta il filtro verso l'APK.
      Se un giorno pesassero, la stessa lista può diventare anche lista di *conversione*


### Blocco B — la cucitura Kotlin, fatta (11 set 2026)

L'app disegna la v3. `tools/import_meteocons.py` e i 268 drawable della v2 escono di
scena; il tool v2 resta in `tools/` perché `import_meteocons_v3.py` ne importa
`loop_keyframes` — la rotazione dei keyframe per una fase negativa, scritta per la v2 e
valida qui identica.

- [x] **`ChiaroIcons` ora è politica, non tabelle.** Le quattro mappe stanno in
      `ui/icons/MeteoconsSets.kt`, **generato** dal tool su `tools/shipped_icons.py`: 143
      icone per quattro set sono 595 righe in cui un refuso non si vede, e il tool sa già
      quali file ha scritto. Quel che resta in `ChiaroIcons` è la parte su cui si discute:
      quale codice prende quale disegno, quale metrica quale marchio
- [x] **Il vestito non sceglie più un'icona.** `styledRes` prendeva `AppPalette` perché il
      set line della v2 doveva **entrambe** le superfici (banda Y ∈ [0,120, 0,283], da cui
      il sole bronzo) e serviva un secondo set perché il vestito vivid potesse scappare da
      quel tetto sui fondi scuri. Con quattro set scelti da stile e fondo il parametro non
      decideva più niente: è stato tolto da `styledRes`, `conditionRes`, `movingRes`,
      `moonPhaseRes`, `ArcText.rowIconRes` e `SkyWidget.skyJobIconRes`
- [x] **`WeatherIcons.FILL` ora è il `flat` di v3, e l'enum tiene il suo nome**: `flat` è
      quello che l'app già spediva come set pieno (il fill v2 coi gradienti appiattiti dal
      tool). Rinominare la costante avrebbe migrato una preferenza salvata e riscritto una
      stringa delle impostazioni per descrivere lo stesso disegno
- [x] **La mappatura WMO** è quella della tabella sopra, `not-available` compreso
- [x] **Il test che non c'era**: `ConditionIconsTest`. Ogni codice che il provider può
      servire → il suo disegno, di giorno e di notte; il fallback; e una riga che dice
      esplicitamente che 0, 1 e 2 sono tre disegni diversi — il difetto che ha aperto la
      fase, in un'asserzione
- [x] **`IconContrastTest` e `AnimatedIconTest`** guardano i set v3. Il primo ha perso la
      frase «il set line deve entrambe le superfici», che dalla Fase 13 non è più vera; il
      secondo ha imparato due cose: che un `keySplines` è un `<pathInterpolator>` e non un
      difetto (pretendeva che ogni animatore fosse lineare, vero solo per la v2), e che
      un'icona il cui tratteggio è diventato una finestra **non** ha lo stesso `pathData`
      del suo gemello fermo — eccezione dichiarata, riconosciuta dalla presenza di
      `trimPath` e da nient'altro, così non può allargarsi in silenzio
- [x] **`MeteoconsSetsTest`**, la guardia del confine. Aggiungere un `R.drawable.mc3_…` a
      una schermata senza aggiungerlo alla lista compila benissimo e poi lancia in faccia
      al lettore, perché `styledRes` usa `getValue`: il test legge i sorgenti e lo impedisce

**E la lista di spedizione si è ristretta, dopo averlo misurato.** Era di 143; le icone
che una schermata disegna davvero oggi sono **53**. Spedire anche le altre 90 — i pollini
graduati, l'UV, le bande del barometro, la Beaufort, i momenti del giorno, i tipi di
allerta — costava **570 KB su 6,8 MB, l'8% dell'APK, per disegni che nessun lettore
vedeva**. Stanno in `PLANNED` nello stesso file, e ognuna si sposta in `SHIPPED` nella
stessa modifica che le dà una schermata.

| | APK di release |
|---|---|
| prima della fase (v2, 49 icone) | 6 548 541 B |
| v3 con 53 spedite | **7 141 284 B** (+593 KB) |
| v3 con 143 spedite | 7 724 737 B (+1,12 MB) |

Il mezzo mega in più compra 24 cieli invece di 18, i disegni di v3 che sono più
dettagliati, e i quattro set riancorati.

- [x] **Le schede Dettagli**: pollini per pianta e livello, UV per unità (il tile stampa
      l'intero), le **tre** bande del barometro che `pressureMeaning` dice e non le cinque
      disegnate, e `mist`/`haze`/`fog` per la visibilità. Il vento **non** è graduato, per la
      stessa regola: `windMeaning` ha cinque bande, i windsock sono tre e la Beaufort non è
      detta da nessuna parte a schermo
- [x] **L'ago del vento**: `wind-direction-n` col solo gruppo `Pointer`, ritagliato su una
      finestra di 48 unità centrata sul **mozzo** (senza ritaglio è tredici unità
      d'inchiostro su centoventotto, e a 16 dp una scheggia — guardato, non supposto), ruotato
      come `WindArrow` ruotava già. Resta a 16 dp, così nessuna misura del tile si muove


### Blocco D — i documenti, e un numero che è migliorato (11 set 2026)

`DESIGN.md` §13.1 era la sezione più lunga del documento e citava v2.0.0, i 64 unit, le
tre dipartenze del tool e il quarto set del vestito vivid: riscritta. Con lei §7.1, la
nota dei generatori e `README.md` (che non prende trattini lunghi, ed è stato ricontrollato
riga per riga).

**L'eccezione dichiarata del widget Cielo è stata rimisurata, e si è mossa parecchio.**
Quel fondo è il cielo scurito, al più chiaro `#5C6E7B`, Y 0,149: un mezzotono, dove un
inchiostro passa il 3:1 solo sopra Y 0,546 o sotto Y 0,016, e in mezzo non c'è niente.

| | sotto 3:1 su `#5C6E7B` | il sole |
|---|---|---|
| v2 line (`mc_*`) | 8 su 8 | 1,57:1 |
| v2 line vivid (`mcn_*`) | 8 su 8 | 2,68:1 |
| **v3 line scuro (`mc3n_*`)** | **17 su 34** | **3,31:1** |
| **v3 flat scuro (`mc3fn_*`)** | **20 su 38** | **3,33:1** |

**L'icona che il lettore guarda davvero adesso passa**, e non era mai successo. Metà dei
colori resta sotto, quindi l'eccezione resta un'eccezione — ma il caso che conta si è
chiuso per aritmetica e non per concessione: un set che deve una superficie sola ha spazio
che un set che ne deve due non ha. È il guadagno della decisione del Blocco C, arrivato
dove non lo si cercava.


### La prova sul dispositivo, e le due cose che ha rovesciato (11 set 2026)

Il committente ha installato l'APK e guardato striscia oraria, fasce dei giorni, Cielo e
widget: **vanno**. Le schede Dettagli no, e in due punti aveva ragione lui.

**Il vento sembrava rotto perché lo era.** Il marchio usciva come mezza riga e un pezzo di
ricciolo. La causa è una mia scelta sbagliata del blocco sui tratteggi: avevo deciso che un
tratteggio fermo si ridisegna a segmenti *sempre*, anche quando il tratteggio è la finestra
di una spazzolata. Ma i marchi delle schede **non si animano mai** (§7.1), quindi quel che
il lettore vede per sempre è il primo fotogramma di un'animazione che non parte. L'importatore
v2 lo aveva già capito e messo per iscritto — «il tratteggio lì esisteva solo per essere
animato» — e io l'ho scavalcato con una regola generale. Ora il fermo esce **pieno** e solo
il gemello animato porta la finestra di `trimPath`.

Rimettendolo pieno è venuto fuori un difetto vero che il tratteggio nascondeva:
`emit_path` chiudeva **ogni** sottopercorso con uno `Z`, e su un tratto aperto quella
chiusura è una riga che torna indietro. Sui segmenti spezzati era invisibile (ogni tratto
tornava su se stesso), sul ricciolo intero sarebbe stata una diagonale in mezzo all'icona.
`parse_segments` adesso si ricorda se un sottopercorso era chiuso.

**La regola sulle icone graduate aveva una metà in meno.** Diceva: un glifo può dire solo
un livello che il tile già calcola e già dice a parole. Il barometro la passava — Meteocons
ne disegna cinque, l'app ha tre bande — ma quel che distingue i cinque quadranti è un ago
**largo 2 unità su 128**, cioè mezzo dp ai 34 del tile. Aritmeticamente giusto e otticamente
assente: un quadrante che sembra dover indicare qualcosa e non indica niente, ed è esattamente
come l'ha descritto il committente («non ha indicazione»). La regola ora ha la sua seconda
metà — **e che un lettore possa vederlo** — e la pressione spedisce il quadrante generico.

**La freccia della direzione del vento è stata tolta** (committente). Non perché dicesse
troppo poco: diceva *più* dell'etichetta a sedici punte accanto. Perché a 16 dp non era
bella, e in un tile fatto di righe di testo era l'unica cosa che si notava per il motivo
sbagliato. La macchina che prende metà disegno (`PARTIALS`) resta, documentata e inutilizzata:
è un problema che tornerà.

**E una domanda a cui la misura risponde di no.** Il committente ha chiesto se, con la v3, si
possa togliere la pausa delle animazioni durante lo scroll (§7.1, messa l'8 set perché lo
scroll era scattoso). Misurati i nodi di percorso delle icone che si animano davvero:

| | icone | nodi, mediana | nodi, totale |
|---|---|---|---|
| v2 (`mca_*`) | 18 | 24 | 561 |
| **v3 (`mc3a_*`, spedite)** | 23 | **71** | **2 079** |

Il costo per fotogramma sul RenderThread è la rasterizzazione di quei percorsi, e la v3 ne
ha **circa il triplo**. La ragione per cui la pausa esiste non è venuta meno: è cresciuta. Il
conteggio dei nodi è un indizio e non un tempo di fotogramma, quindi la risposta certa resta
una prova sul dispositivo — ma la misura punta dalla parte opposta a quella sperata.

- [ ] Se si vuole comunque provare: `LocalMotionPaused` è un punto solo, e un APK con la
      pausa tolta si costruisce in un minuto. Da fare guardando, non discutendo


### Il secondo giro sulle schede (11 set 2026)

Tre domande del committente dopo la prima prova, e tre risposte misurate.

**Qualità aria: `smoke` invece di `smoke-particles`.** D'accordo, e c'è un numero dietro:
le particelle da sole riempiono **0,33** della loro scatola, il marchio più piccolo della
griglia; `smoke` è le stesse particelle con l'aria in cui stanno, **0,62**. Il costo si
dichiara: quando la visibilità scende nella sua banda di foschia il tile accanto disegna
una nuvola con le righe e questo una nuvola con i puntini, e a 34 dp si somigliano. Succede
sotto i 10 km, dove le parole comunque differiscono.

**Ora d'oro e Tramonto: erano lo stesso disegno, ed è colpa del set.** `sunrise`,
`horizon` e `sunset` differiscono per una gobba in mezzo alla linea dell'orizzonte, **6
unità su 128**, cioè 1,6 dp alla misura della riga. Invisibile. Meteocons v3 non ha un
disegno per l'ora d'oro — cercati e scartati `sun-hot` (legge «caldo»), `haze-day` (legge
«foschia»), i `time-*` (portano un intervallo di ore stampato, che contraddirebbe l'ora
della riga) — ma **il sole pieno dice la cosa giusta**: nell'ora d'oro il sole è ancora
sopra l'orizzonte, all'alba e al tramonto lo sta attraversando. Chi attraversa tiene la
linea.

L'ora blu resta la stella vuota e **si dichiara debole**: legge più come un «preferito»
che come la prima stella della sera, e in v3 non c'è di meglio senza rubare un disegno a
un'altra riga.

**Le dimensioni: la scala non si è mossa, e il motivo è aritmetico.** Meteocons non disegna
tutte le icone alla stessa taglia dentro la scatola: misurata la frazione occupata dai
marchi delle schede, va da **0,33** a **1,00**. Non è una svista, è come sono disegnate, e
appiattirla vorrebbe dire una goccia grande come un sole.

| | riempimento della scatola |
|---|---|
| `uv-index` | 1,00 |
| `barometer` | 0,80 |
| `wind` | 0,63 |
| **`pollen-*`** | **0,53** (era 0,76 con `dust`) |
| `mist`, `raindrops` | 0,47 |
| `humidity` | 0,42 |
| `smoke-particles` | 0,33 |

Un solo caso si corregge, ed è una **regressione misurata**: i pollini riempiono 0,53
dove `dust` — il ripiego che sostituiscono, il disegno con cui la griglia era stata messa
a punto — ne riempiva 0,76. Si importano in una finestra di 89 unità invece di 128, **una
sola per tutte e tredici**, calcolata sull'unione dei loro inchiostri così che piante e
livelli non ballino fra loro.

**Il badge del numero non si salva ingrandendo.** In `uv-index-*` e `pollen-*-*` il badge è
**30 × 30 unità su 128** in entrambi: 8 dp ai 34 del tile, con dentro una cifra di circa
**3,2 dp**. Il budget dell'etichetta concede esattamente un gradino (a 38 dp restano 80 dp
contro i 76,7 che «Qualità aria» chiede; a 42 ne restano 76 e va a capo), e quel gradino
comprerebbe alla cifra **0,4 dp**. Non basta a renderla leggibile e spende più di metà del
margine. Il badge resta quello che a questa misura è onestamente: un segno di colore, non
una cifra. Il valore è stampato sotto in 32 sp, che è dove si legge.


### Il terzo giro: l'ago che non c'era, e un gradino (11 set 2026)

**«All'icona della pressione manca l'indicatore» era un difetto di conversione, non una
scelta.** Tutti i barometri di Meteocons hanno il loro ago; il `<clipPath>` che li
accompagna però è un rettangolo con un `transform="rotate(45 …)"`, e il convertitore
**ignorava quel transform**. Il ritaglio finiva nell'angolo in alto a sinistra e cancellava
l'ago. `<clip-path>` in VectorDrawable non ha un transform suo, e appoggiarlo al gruppo lo
applicherebbe anche ai figli — cioè muoverebbe il disegno insieme al ritaglio — quindi la
matrice si cuoce nelle coordinate (un arco tiene i raggi sotto una trasformazione rigida e
ruota l'inclinazione; una scala non uniforme viene rifiutata invece che disegnata male).

Venti disegni toccati, tutti `barometer*` e `compass*`, due dei quali si spediscono. E
qui sta la lezione: **il giro precedente aveva concluso che il barometro «non si può
graduare perché l'ago è mezzo dp»**. L'ago era invece assente del tutto, e la conclusione
poggiava su un difetto scambiato per una proprietà del disegno. La decisione di non
graduarlo resta — quattro quadranti che differiscono per l'angolo di un ago da 1 dp non
sono quattro cose diverse per un lettore — ma ora è presa sul disegno vero.

**La bussola ha perso le sue lettere.** Riparando il ritaglio sono diventate visibili le
**N E S W disegnate come path** che la `compass` di v3 porta, e in italiano l'ovest è O:
è lo stesso testo inglese dentro un'immagine per cui erano state scartate le otto
`wind-direction-*`. Il tool sa ora lasciare indietro un gruppo (`DROP_GROUPS`), e togliere
`Letters` riporta la bussola al disegno della v2 — cerchio e ago — con cui la riga dei
luoghi era stata messa a punto.

**E il gradino c'era davvero.** Il budget dell'etichetta è `118 − icona` su 360 dp contro i
76,7 dp che «Qualità aria» chiede, quindi il tetto è **41,3 dp**: 38 lascia 3,3 dp di
margine, 40 ne lascia 1,3, 42 manda a capo. La nota che stava lì diceva che un quarto
gradino avrebbe mandato a capo l'etichetta; l'aritmetica dice che un gradino ci sta, e
l'aritmetica ha ragione. Il tile passa a **38 dp**. Costa alla scala il suo ordine stretto
— il tile adesso pareggia la riga della settimana invece di starle sotto — e un'etichetta
di scheda accanto a una riga della settimana non è un confronto che un lettore fa mai.


### «Il ritaglio può aver rotto altro?» — cercato, e no (11 set 2026)

Domanda del committente dopo il difetto del barometro. La risposta non è un'opinione:
sono due verifiche, una sulla forma del convertitore e una sul risultato.

**Primo: l'inventario.** Enumerati tutti gli elementi e tutti gli attributi che compaiono
nelle 519 × 4 sorgenti, e confrontati con quel che l'importatore legge davvero. Cinque
sospetti, e quattro erano fantasmi:

| sospetto | occorrenze | esito |
|---|---|---|
| `filter` come **attributo** di un gruppo (io rifiuto solo l'elemento) | **0** | non esiste |
| `clip-rule` dentro un `<clipPath>` | **0** | non esiste |
| forme diverse da `<rect>` dentro un `<clipPath>` | **0** | sono tutti rettangoli |
| `stop-opacity`, primo stop con offset ≠ 0 | **0** | non esistono |
| `additive="sum"` su `animateTransform` | 84 | **corretto per costruzione** |

L'ultimo meritava la verifica vera, fatta attraversando l'albero e non con una regex: in
tutte e 519 non c'è **un solo** elemento con un `transform` statico più un'animazione che
lo sostituisca, né due animazioni di cui la seconda non additiva. L'annidamento di gruppi
che l'importatore fa è sempre la composizione giusta.

**Secondo: il risultato, non le intenzioni.** `tools/diff_against_source.py` sovrappone
ogni icona convertita alla sua sorgente con `mix-blend-mode: difference` in una griglia a
posizione nota, fa scattare Chrome, decodifica il PNG e conta i pixel accesi cella per
cella. Due accortezze senza le quali il numero non vuol dire niente: l'antialiasing va
spento (un pixel a metà trasparenza non dà zero sotto `difference`, e senza
`crispEdges` brillano tutte e 519 le celle), e la **linea di base** va misurata e
sottratta confrontando la sorgente con se stessa.

Il primo giro diceva 443 celle diverse su 519. Non erano icone rotte: era il filmstrip.
**`icon_filmstrip.py` ignorava `fillType="evenOdd"`**, e nella famiglia `line` di v3 un
contorno *è* un anello disegnato con quella regola — quindi lo strumento riempiva piene
tutte le nuvole. Difetto suo, corretto, e vale anche per ogni figura che questa fase ha
mostrato finora.

Corretto quello, le icone con una **forma intera** di differenza (picco ≥ 200, cioè non un
bordo) sono **35 su 519**, e sono due gruppi soli, entrambi voluti e già scritti:

- i **16 pollini**, che sono ritagliati apposta in una finestra di 89 unità;
- le **19 del vento**, dove il tratteggio spazzolato esce pieno nel disegno fermo.

Nient'altro. Il `transform` sul `<clipPath>` era l'unica classe di perdita silenziosa, e
non ne restano.


### «Le icone sembrano sproporzionate fra loro» — non e' la taglia, e' il tratto (11 set 2026)

Segnalazione dal dispositivo, su «Il resto della giornata» e «La settimana»: il sole e la
luna sembrano grandi, le nuvole piccole. Cercata la causa in tre misure, e le prime due
dicono di no.

**Non e' la dimensione della scatola.** Il lato dell'inchiostro in frazione di scatola
sulla famiglia dei cieli: v2 andava da 0,32 a 0,70 (escursione **2,17×**), v3 va da 0,47 a
0,73 (**1,58×**). La v3 è *più* uniforme di quella che spediva prima, e le masse sono quasi
tutte più grandi. L'unica che si è rimpicciolita è `fog-day` (0,70 → 0,49).

**Non è l'inchiostro totale.** Rasterizzate e contate le coperture: `partly-cloudy-day` ne
mette a schermo **più** di `clear-day` (8,3% contro 7,7% della cella). Eppure sembra più
leggera, quindi non è quello che l'occhio pesa.

**È lo spessore del tratto.** In v2 la nuvola era un tratto dichiarato: **3 unità su una
scatola di 64, il 4,69%**. In v3 la nuvola della famiglia `line` è un **anello riempito
evenodd**, e l'anello è spesso **4 unità su 128, il 3,13%**. Cioè un terzo più sottile in
proporzione: a 42 dp sono **1,3 dp di linea contro i 2,0 di prima**. Accanto a un sole che
è un disco pieno con i raggi pieni, un anello di 1,3 dp pesa meno — ed è esattamente quel
che si vede.

Va aggiunto che una parte di quello che si nota è il disegno che fa il suo mestiere:
`mostly-clear` **deve** pesare meno di `clear`, ed è la distinzione per cui questa fase è
stata aperta.

- [ ] **La decisione è del committente**, perché le tre strade portano a lavori diversi:
      lasciare così (è il disegno dell'illustratore, e un contorno pesa meno di un pieno);
      passare allo stile **flat**, dove tutto è pieno e la riga si pareggia — un tocco in
      Impostazioni, zero codice; oppure **ispessire gli anelli** della famiglia line
      aggiungendo un tratto dello stesso colore al path riempito, che è meccanico ed esatto
      (4 → 6 unità riporterebbe il peso relativo a quello della v2) ma è una dipartenza vera
      dal «si usa l'originale»


### La regola sulla scatola: quella dell'illustratore, senza eccezioni (11 set 2026)

Decisione del committente, e sostituisce quella che avevo preso io il giorno prima: **ogni
icona si importa nella finestra in cui è disegnata a monte, "a dimensione 1"**, senza
ritagli e senza scale per-icona. Le differenze di taglia che ne risultano sono
dell'illustratore, e si tengono.

Quindi il ritaglio dei pollini è stato **tolto** — tornano a 0,52 della loro scatola, cioè
più piccoli di come li avevo messi — e con lui è sparita la macchina che lo permetteva
(`CROPS`, e `PARTIALS` che era l'unica altra cosa a ritagliare e non aveva più utenti da
quando la freccia del vento è uscita). Resta `DROP_GROUPS`, che non è una questione di
taglia ma di lingua: toglie le lettere inglesi dalla bussola.

Il conto di quel che si perde, perché è giusto averlo scritto: i pollini erano il caso in
cui il ripiego della v2 (`dust`) riempiva 0,76 della scatola e il disegno vero ne riempie
0,53. Il pareggio che il ritaglio comprava valeva meno di una regola senza eccezioni, e
un'eccezione è una cosa da ricordare.

**La verifica dice che la fedeltà è ora completa.** Rifatto il confronto con le sorgenti
dopo la rimozione, le icone con una forma intera di differenza sono **19 su 519**, e sono
17 del vento (il tratteggio spazzolato reso pieno nel disegno fermo, inevitabile perché
VectorDrawable non ha `stroke-dasharray`) più `moonrise` e `moonset` a 4 e 5 pixel, che
guardati al grande sono identici alla sorgente: è rumore di soglia. La famiglia dei pollini
è sparita dall'elenco, che è esattamente quel che doveva succedere.


### Il codice 1 prende il sole pieno (11 set 2026)

Decisione del committente, contro il mio consiglio, e con una ragione che si e' rivelata
migliore dell'argomento con cui l'avevo difesa io.

Io dicevo: abbiamo il disegno del «quasi sereno», usiamolo. Lui diceva: quel disegno **dà
l'idea del nuvoloso** anche quando significa il contrario. Misurato nella sorgente, ha
ragione con un margine netto:

| | nuvola | disco del sole |
|---|---|---|
| `clear-day` | — | 36 |
| **`mostly-clear-day`** | **56** | **23** |
| `partly-cloudy-day` | 80 | 18 |

La nuvola del «quasi sereno» è il **70%** di quella del «poco nuvoloso**, per un cielo che
ne ha il 39% della copertura (25% mediana contro 64%). E il sole rimpicciolisce da 36 a 23
unità, un terzo in meno, mentre a un quarto di cielo coperto il sole è fuori tutto. Il
disegno è più nuvoloso del suo nome: usato per il codice 1 rifaceva il difetto originale in
forma mite, sovrastimando la nuvola invece di sottostimare il sole.

**Il costo, scritto e non aggirato:** la striscia oraria e la riga della settimana non hanno
parole, quindi lì 0 e 1 ora si somigliano, e sono il 17,2% delle ore disegnate come cielo
terso su un cielo coperto per un quarto. Nella scheda in alto le parole li distinguono
ancora.

Quasi tutte le app fanno lo stesso collasso, ma per una ragione più povera: non hanno il
disegno. L'integrazione Open-Meteo di Home Assistant mappa 0 e 1 entrambi su `sunny`, e la
sorgente di Open-Meteo mette il codice 1 a 20–49% di copertura. Qui è una scelta, non un
limite di vocabolario: **il disegno resta nel repo e in `PLANNED`**, pronto se un giorno la
scelta si rivede.

`ConditionIconsTest` fissa tutte e due le metà: il codice 1 **deve** essere uguale allo 0, e
**deve** restare diverso dal 2 — che è la confusione da cui è nata la fase e che non deve
poter tornare per distrazione.

### Il quasi sereno diventa un disegno suo (12 set 2026)

Il committente torna sulla decisione del giorno prima, e la riapre dalla parte giusta: il
sole pieno per il codice 1 è onesto sulla parola e muto sul disegno, «mi spiace però non
visualizzare questo stato meteo correttamente». La domanda è se si possa **comporre**
un'icona partendo da quella di `clear` e aggiungendo la nuvola rimpicciolita in basso a
destra, invece di scegliere fra due disegni che sbagliano in due direzioni opposte.

Si può, e non serve disegnare niente a mano: tutti i pezzi sono già importati.
`tools/compose_sun_cloud.py` li mette insieme, e la regola che lo rende sicuro è che **i
percorsi restano identici alla lettera ai loro originali** — il sole è `clear-day` con i
suoi path e il suo gruppo di scala, la nuvola è la silhouette di `cloudy`. L'unico
percorso calcolato è il buco della maschera, ed è anche l'unico misurato riga per riga.

**Tre cose sono state decise guardando, e una quarta misurando.**

1. **La taglia della nuvola.** Tre misure renderizzate a 42 dp (la striscia oraria) e
   confrontate con il poco nuvoloso: 37,3 / 43,1 / 48,8 unità, cioè il 38% / 43% / 49%
   della sua nuvola. Scelta la mediana. Per riferimento, la nuvola dello scartato
   `mostly-clear` ne vale il **72%**: è quella la ragione per cui il disegno della
   libreria non si usa.
2. **La nuvola è ri-tracciata, non rimpicciolita.** Scalare l'anello di `cloudy` scala
   anche il suo contorno: a metà taglia sarebbe largo 2,0 contro i 3,7 del sole, e la
   nuvoletta si legge come uno sbaffo. Il gruppo porta `strokeWidth` = 4 / scala, così il
   tratto torna esattamente a 4. `ComposedIconsTest` tiene fermo quel prodotto.
3. **Due composizioni, e ha vinto quella che cambia meno.** A: sole all'85%, spostato in
   alto a sinistra, inchiostro esattamente sullo 0,690 di famiglia. B: `clear-day`
   **intatto** — stessa taglia, stesso posto — e la nuvola nell'angolo. Scelta B dal
   committente: fra sereno e quasi sereno cambia una cosa sola, ed è la cosa che cambia
   nel cielo. Si paga con un inchiostro a **0,711** invece di 0,690 (il 3% sopra, ben
   sotto il tetto di 0,88) e con il raggio di sud-est che resta dietro la nuvola.
4. **La notte porta la stessa nuvola, e non lo faceva da sola.** Segnalata dal
   committente guardando i provini: nel disegno la nuvola era già nello stesso punto, ma
   la normalizzazione della taglia la spostava, perché una luna occupa meno di un sole
   con i raggi (`mc3scale` 1,138 contro 0,980) e la stessa nuvola usciva 50,1 unità
   invece di 43,1. Nella composizione A la correzione era dare alla coppia una scala sola
   e alla luna la taglia che porta anche la notte a 0,69 — veniva l'85% di `clear-night`,
   cioè **la stessa percentuale a cui sta il sole**. In B il problema non si pone: non
   c'è normalizzazione, la luna è `clear-night` intatta e la nuvola cade sullo stesso
   pixel in tutte e due.

**L'aria fra la nuvola e i raggi** è l'unica misura che la composizione deve inventare,
perché la maschera della sorgente è dilatata di 4 e scalata con la nuvola varrebbe 1,96.
Si riparte da quella e si scosta lungo le normali: misurata dove il sole arriva davvero,
esce **1,80–2,45** (mediana 2,29), contro i 2,48 che Meteocons lascia nel poco nuvoloso.

**Il difetto trovato dallo strumento, non dall'occhio.** Al primo piazzamento (centro
della nuvola a 86,7) il buco tagliava il raggio di sud **per il lungo** e lasciava una
scheggia larga mezzo tratto accanto alla nuvola. `ray_report()` la misura raggio per
raggio — quanto ne resta e quanto è largo quel che resta — e `slivers()` si rifiuta di
scrivere i file finché c'è. Spostata la nuvola di 3,2 unità a destra: sette raggi interi,
il sud-est dietro la nuvola, nessuna scheggia.

**Il difetto che si vedeva solo sul telefono (12 set 2026).** Con le icone animate accese
la nuvoletta partiva per la tangente; ferma era al suo posto. La causa è una regola di
`AnimatedVectorDrawable` che non avevo in mente: un `objectAnimator` **sostituisce** la
proprietà del gruppo, non ci si somma. Il gruppo della nuvola portava
`translateY="30.5"` (il posto) e un `translateY` animato da 0 a −3 (il dondolio): appena
l'animazione parte, il 30,5 sparisce e la nuvola sale di trenta unità, mentre il buco
della maschera resta dov'era. Ora i gruppi sono due — un guscio vuoto che porta solo
l'animazione, e dentro quello che porta taglia e posto — che è poi la forma che
l'importatore dà a tutte le sue, e per questo nessuna icona importata aveva mai mostrato
il problema.

**Perché non l'aveva visto il filmstrip**, che esiste apposta per guardare le animate:
`tools/icon_filmstrip.py` **sommava** la trasformazione statica e quella animata. Sui file
dell'importatore le due non convivono mai, quindi la somma dava sempre la risposta giusta
e il difetto è passato. Corretto lì (sostituisce, non somma) e fissato in
`AnimatedIconTest`: nessun gruppo anima una proprietà che porta anche scritta, su tutta la
famiglia. La misura di quanto fosse cieco il controllo: 8 file su 519 × 4 set la
violavano, ed erano tutti e soli i composti.

**Cosa resta dov'era:** il `mostly-clear` di Meteocons resta nel repo e in `PLANNED`,
inutilizzato; le parole sullo schermo non cambiano; `MeteoconsSets.kt` non è stato
toccato, perché il composto vive in `ComposedIcons.kt`, che l'importatore non riscrive.
`ConditionIconsTest` ora fissa tre distanze invece di due (0 ≠ 1, 1 ≠ 2, 0 ≠ 2) e
`ComposedIconsTest` confronta il composto con le sue sorgenti percorso per percorso: se
una ri-importazione cambia i disegni da cui viene, il test dice di ri-eseguire lo
strumento invece di lasciare il composto indietro in silenzio.

### «È l'importazione che ridimensiona?» — no, ed è misurato sulle 519 (11 set 2026)

Segnalazione dal dispositivo, sul Cielo → **In arrivo**: la luna piena e la luna delle
stelle cadenti hanno taglie molto diverse, e la domanda è se sia l'importatore a fare un
resize dell'originale.

**La risposta è no, e non è un'opinione.** `tools/icon_ink.py --confronto` misura il
riquadro dell'inchiostro sulla sorgente SVG e sul vector drawable generato e mette le due
misure accanto: **519 icone × 2 stili, 0 fuori tolleranza** (un centesimo di unità su 128,
che è il rumore dell'appiattimento delle curve). L'importatore copia il `viewBox` e le
coordinate così come sono — `convert()` scrive `viewportWidth` dal `viewBox` e basta — e
dal 964e01a non esiste più nemmeno la macchina che poteva ritagliare (`CROPS`, `PARTIALS`).

La coppia segnalata, vista da vicino, è la prova più corta:

| | sorgente Meteocons | drawable spedito |
|---|---|---|
| `moon-full` | `<circle cx=64 cy=64 r=30.5>` | `M33.5,64 A30.5,30.5,…` |
| `falling-stars`, la luna | `M59.997 43.821 c-2.266 14.257…` | `M59.997,43.821 C57.731,58.078,…` |

Stessi numeri. L'unica cosa che cambia è il colore (`#72b9d5` → `#88cfec`), che è il
riancoraggio al 3:1 del fondo scuro e non tocca la geometria.

**Quel che succede davvero è che l'illustratore rimpicciolisce la luna quando il disegno
ne contiene anche altro**, e lo fa a scalini netti. Misurato con `--dentro moon`, il lato
della sola luna in frazione di scatola:

| la luna sta | lato | su 34 dp |
|---|---|---|
| da sola (`moon-full`, `clear-night`, `moonrise`, `moonset`) | 0,49–0,51 | ~17,0 dp |
| con due o tre compagni (`falling-stars`, `starry-night`, `fog-night`) | **0,34** | **11,6 dp** |
| dietro una nuvola (`partly-cloudy-night`, `thunderstorms-night`) | 0,20 | 6,9 dp |

Quindi la luna piena ha **1,47×** il diametro della luna dei Draconidi, e in copertura
d'inchiostro il disegno intero ne ha 2,27× (19,7% della scatola contro 8,7%). È
esattamente quel che l'occhio vede, ed è dell'illustratore.

**Dove si nota, e perché proprio lì.** «In arrivo» è l'unico posto del prodotto dove
icone di **famiglie diverse** stanno incolonnate una sotto l'altra a 34 dp, quindi il
lettore le confronta davvero. In quella colonna il lato dell'inchiostro va da 0,75
(`sunrise`, `horizon` — gli equinozi e i solstizi) a 0,37 (`starry-night`), cioè **da 25,5
a 12,6 dp, un'escursione di 2,02×**. Sull'intera lista di spedizione va da 0,33
(`smoke-particles`) a 0,81 (`uv-index-11-plus`): **2,48×**.

Vale la pena scriverlo perché ribalta l'istinto: `falling-stars` **non** è un'icona
piccola. Il suo riquadro è 0,63 largo, più largo della luna piena; è alto 0,36 perché la
composizione è una diagonale in una scatola quadrata. È piccola la **luna dentro**, ed è
alto poco il disegno: due cose diverse, e la prima è quella che si è notata.

- [x] **Deciso lo stesso giorno**: si normalizza, su tutte le icone — sezione qui sotto,
      dove c'è anche il motivo per cui la strada scelta NON è quella che consigliavo qui.
      I numeri delle tre strade restano scritti perché sono la ragione della scelta:
      **lasciare così** — è il disegno dell'illustratore, e la regola senza eccezioni del
      964e01a vale ancora;
      **normalizzare sul lato** (la regola da manuale) **peggiora proprio questa coppia**:
      `falling-stars` è già 0,63 e crescerebbe meno della luna piena a 0,50, portando le
      due lune da 1,47× a 1,85×;
      **normalizzare sull'inchiostro** (una scala per icona, `k = √(copertura obiettivo /
      copertura)`, applicata al disegno dentro una scatola che non si muove, quindi zero
      effetti sul layout) le pareggia. Alla copertura mediana della lista, 14,8%, e sullo
      stile flat: la luna piena va da 17,0 a **14,7 dp**, quella dei Draconidi da 11,4 a
      **14,9 dp**. Costa una tabella generata dall'importatore e un fattore di scala ai
      quattro punti di disegno; tocca ogni icona dell'app (`smoke-particles` × 2,02,
      `clear-day` × 1,05), e tre icone arriverebbero al bordo della scatola

**Un difetto trovato costruendo la misura**, che vale oltre lo strumento:
`spike_mask_clip.flatten` **scarta i sottopercorsi di due punti**, ed è giusto là dove
nasce (deve provare delle maschere, che sono forme chiuse) ma qui faceva sparire la scia
di `falling-stars` — 0,25 di scatola — e rendeva `mist` «senza inchiostro». `icon_ink.py`
ha quindi il suo `flatten`, che tiene i segmenti e passa gli archi per la
parametrizzazione centrale del modulo delle maschere. Chi riusa quella funzione altrove
lo tenga presente.

### La taglia si pareggia: ogni disegno a una misura comune (11 set 2026)

> La prima versione di questa regola normalizzava il **lato più lungo** a 0,75 ed è
> durata mezza giornata: la sezione che segue la racconta com'è stata presa, e quella
> dopo dice perché il lato non bastava e cosa la sostituisce.

Decisione del committente, e **rovescia quella sua del mattino** («la scatola è quella
dell'illustratore, senza eccezioni»): le differenze di taglia fra i disegni di Meteocons,
misurate poche ore prima, si leggono come un difetto dell'app e vanno tolte, su tutte le
icone.

**La regola che avevo proposto io non regge, e l'ho scoperta provandola.** Avevo suggerito
di normalizzare sull'**inchiostro** (`k = √(copertura obiettivo / copertura)`) perché
pareggiava la coppia segnalata a 0,2 dp. Applicata a tutte le famiglie si rompe subito: le
otto fasi lunari sono **una sola geometria** — tutte 0,50 di scatola — con undici volte la
massa fra gli estremi, `moon-new` al **1,71%** della scatola e `moon-full` al **19,70%**.
Normalizzare sulla massa spedirebbe una luna nuova **3,4×** la luna piena. Qualunque
formula che contenga un termine di massa muore sullo stesso scoglio, quindi la massa non
entra.

**Quel che si spedisce è la normalizzazione sul lato**, l'unica che sta in piedi su tutte
le famiglie (le fasi lunari restano insieme a 1,50; la scala UV resta insieme a 1,00):

| | prima | dopo |
|---|---|---|
| lato dell'inchiostro, lista di spedizione | 0,33 – 0,81 (**2,48×**) | 0,727 – 0,750 (**1,03×**) |
| `moon-full` a 34 dp | 17,0 dp | 25,5 dp |
| `falling-stars` | 21,3 dp | 25,5 dp |
| `starry-night` | 12,6 dp | 25,5 dp |
| `clear-day`, `sunrise` | 25,5 dp | 25,5 dp (fermi: sono il riferimento) |

`tools/import_meteocons_v3.py` avvolge ogni drawable in un gruppo `mc3scale`, su misure di
`tools/icon_ink.py`. **502 delle 517** ne hanno preso uno; le altre erano già a misura. La
finestra non si tocca ancora: il `viewBox` è quello dell'illustratore e non c'è nessun
ritaglio. `--confronto` continua a provarlo, ora al netto della scala dichiarata: **519
icone × 2 stili, 0 fuori tolleranza**.

Tre cose sono decisioni, non dettagli:

- **Una scala per disegno, non per stile**, misurata sull'unione di `line` e `flat`: un'icona
  che cambia taglia quando il lettore cambia stile in Impostazioni sarebbe l'opposto di quel
  che la scala serve a fare.
- **Il perno è il centro della scatola, non quello dell'inchiostro.** Ricentrare sposterebbe
  le composizioni volutamente fuori centro (il sole di `sunrise` sta basso perché sorge da
  una linea) e cambierebbe il centraggio ottico su cui la striscia oraria è stata messa a
  punto. Il prezzo: un disegno fuori centro tocca il bordo prima di arrivare a 0,75 e lì si
  ferma — `sunrise`, `sunset`, `horizon` e `humidity` si fermano a **0,727**.
- **Il pennello si scala col disegno.** Metà della famiglia `line` disegna i contorni come
  anelli riempiti `evenOdd` e non come tratti, e un anello non si può riassottigliare;
  compensare gli uni e non gli altri renderebbe disuniforme la famiglia invece delle icone.
  Il costo, scritto: alla scala massima della lista (`smoke-particles`, **2,29×**) la linea è
  più del doppio di quella di `clear-day`, che resta a 1,00.

**Quel che la normalizzazione NON risolve**, ed è la metà della segnalazione che va detta
chiara: Meteocons rimpicciolisce la luna quando il disegno contiene anche altro — 0,50 da
sola, **0,34** con due o tre compagni, **0,20** dietro una nuvola — e quella è una scelta di
composizione, non di taglia. Dopo la scala la luna piena porta 25,5 dp e la luna dei
Draconidi 14,0 dp: il rapporto passa da 1,47× a 1,82×, perché `falling-stars` era già più
larga della luna piena e quindi cresce meno. Le due **icone** ora pareggiano; le due lune no,
e nessuna regola di taglia può farlo senza rompere le fasi lunari. L'unica leva che resta è
quale disegno una schermata chiede.

`MeteoconScaleTest` tiene le tre cose che si rompono per distrazione: le quattro facce di un
disegno portano la stessa scala, il gemello animato porta quella del fermo (altrimenti
l'icona salta di taglia quando parte l'animazione) e il perno è il centro della scatola.

### Il Cielo prende un suo gradino: 51 dp (11 set 2026)

Richiesta del committente subito dopo, «anche 1,5× se c'è spazio»: le icone del Cielo
crescono da `WeatherIconSize.Timeline` (34 dp) a un gradino nuovo, `Sky` = **51 dp**, nelle
righe dei momenti, in quelle di «In arrivo» e nell'indice della guida.

Lo spazio c'è, misurato a 360 dp prima di prenderlo. Il budget di testo di una riga è
`360 − 16 − icona − 16 − 48 (la campanella) − 16`, quindi passa da **230 a 213 dp**; una riga
di «In arrivo» senza campanella passa da 278 a 261. L'altezza non si muove: le righe sono già
a tre righe di testo (nome, ora, pastiglia), e 51 dp stanno dentro gli 88 dp di una riga
Material a tre righe con 18 dp d'aria per lato, dentro i 72 dp di una a due righe con 10,5.

**Il mio parere, scritto perché è stato chiesto e perché il numero non lo dice da solo.** Le
due modifiche si moltiplicano, e la seconda è stata chiesta guardando l'app *prima* della
prima: la normalizzazione da sola porta già la luna del Cielo da 17,0 a 25,5 dp e i
Draconidi da 12,6 a 25,5, cioè l'ingrandimento di 1,5× che la richiesta chiedeva, per le
icone che sembravano piccole. Sommandoci il gradino, a 51 dp l'inchiostro diventa **35,2 dp**
su un disegno quadrato (i numeri sono aggiornati alla media geometrica della sezione
seguente; con la regola sul lato erano 38,3), contro i **29** della striscia oraria: il Cielo
resta la superficie con i disegni più grandi dell'app, e rovescia l'ordine di lettura per cui
i gradini di `WeatherIconSize` sono ordinati — la striscia porta il peso maggiore e dovrebbe
restare davanti.

- [ ] **Da guardare sul dispositivo e decidere.** Spedito a 51 dp come chiesto, e la prova
      del committente non l'ha contestato. Se un giorno risulta troppo, **44 dp** (30 dp
      d'inchiostro) rimette la striscia davanti tenendo il Cielo sopra il resto della
      giornata, ed è una riga in `WeatherIconSize.Sky`. L'altra cosa da guardare nello stesso giro sono le righe con la
      frase lunga («8 Ottobre · La previsione non arriva ancora così lontano»): a 213 dp una
      come quella può passare da due righe a tre

### La media geometrica, e perché il lato più lungo non bastava (11 set 2026)

Prova sul dispositivo, subito dopo la normalizzazione. Due segnalazioni, e sono la stessa:
**le lune senza nuvole sembrano grandi** e **le stelle cadenti un po' piccole**.

Hanno ragione, ed è un difetto della regola, non delle icone. Normalizzare `max(w, h)` a
0,75 è la regola da manuale e ha un vizio che si vede solo su uno schermo: **in una scatola
quadrata un disegno quadrato arriva alla misura anche in altezza, uno piatto no**. Misurato
sulle icone della striscia:

| | dopo la regola sul lato | dopo la media geometrica |
|---|---|---|
| `clear-night`, `moon-full` | 0,75 × **0,75** | 0,69 × **0,69** |
| `overcast` | 0,75 × **0,45** | 0,88 × **0,52** |
| `cloudy` | 0,75 × 0,47 | 0,87 × 0,55 |
| `falling-stars` | 0,75 × **0,43** | 0,88 × **0,50** |
| `rainbow` | 0,75 × 0,38 | 0,88 × 0,44 |
| `clear-day`, `sunrise` | 0,75 × 0,75 | 0,69 × 0,69 |

La luna riempiva la scatola in tutte e due le direzioni e la nuvola in una sola: da lì
«grande» e «piccola», che erano la stessa cosa vista da due parti. La media geometrica
legge i due lati insieme, tira giù i quadrati e su i piatti, e li fa incontrare.

**Perché la media e non qualcos'altro**, con i tentativi scartati scritti perché costano
tempo a rifarli:

- la **massa** (copertura) è morta di nuovo sulle fasi lunari, come la prima volta;
- la **sagoma** (l'unione dei sottopercorsi riempiti, che avrebbe dovuto salvare la luna
  nuova rendendola un disco) non la salva: in `flat` Meteocons disegna **solo la parte
  illuminata**, quindi la sagoma della luna nuova è 1,76% della scatola contro il 19,65%
  della piena — è coperura sotto un altro nome;
- **l'altezza da sola** non si può: una nuvola alta quanto la luna sarebbe larga 1,27 volte
  la scatola, cioè tagliata.

La regola finale è il minore di tre vincoli: **media geometrica a 0,69**, **ingombro
massimo 0,88** (senza il tetto `rainbow` arrivava a 0,97 di larghezza, a filo di scatola) e
**il bordo**. Sulla lista di spedizione la media geometrica sta fra **0,49 e 0,69** (1,41×)
dove la sorgente spaziava di 2,48×; 72 icone su 79 le decide la media, 7 il tetto, il bordo
nessuna. `--confronto` continua a dire 519 × 2, 0 fuori tolleranza.

Il gradino del Cielo resta a 51 dp: l'inchiostro di un disegno quadrato passa da 38,3 a
**35,2 dp**, contro i 29 della striscia oraria. La riserva scritta nella sezione precedente
si attenua ma non sparisce.


### L'ora del bollettino senza il suo giorno (committente, 11 set 2026)

Da uno screenshot: alle **12:33** il banner diceva «Protezione Civile, bollettino delle
15:07» per un bollettino emesso il **pomeriggio prima**. Un'ora che quel giorno non era
ancora arrivata, quindi l'unica lettura che si poteva escludere era quella vera.

**Non è un caso di bordo, è il caso normale.** Il Dipartimento pubblica nel pomeriggio e il
bollettino copre *oggi e domani*: chi apre l'app la mattina sta guardando il bollettino di
ieri, tutte le mattine. L'ora da sola non poteva dirlo.

La regola è quella proposta dal committente: **il giorno si dice solo quando non è oggi.**
Una data accanto a ogni ora sarebbe rumore sulla superficie che porta l'allerta, e l'ora di
oggi è già inequivocabile quando il giorno è quello di chi legge.

Una frase sola, `WarningText.issued`, con dentro la sua preposizione — «delle 15:07», «di
ieri alle 15:07», «del 10 set 2026 alle 15:07» — perché un `%1$s` che a volte è un'ora e a
volte una data con l'ora rompe la grammatica di chi la ospita.

**Dove c'era il difetto, verificato superficie per superficie** (la seconda metà della
domanda era proprio «guarda anche in Avvisi»):

| superficie | prima | adesso |
|---|---|---|
| banner su Oggi | solo l'ora | la frase |
| Avvisi, card con allerta (riusa il banner) | solo l'ora | la frase |
| Avvisi, card «nessuna allerta» | solo l'ora | la frase |
| notifica, riga compatta e riga della fonte | solo l'ora | la frase |
| Diario, riga dell'allerta | solo l'ora | la frase, confrontata con **il giorno della voce**, non con quello di chi legge |
| Avvisi, card «nessun bollettino per oggi» | solo la **data** | invariata: il difetto non ce l'aveva |
| scheda dell'allerta | data **e** ora, sempre | invariata, ed è voluto |

La scheda resta l'eccezione perché è la superficie della provenienza: l'attribuzione che la
licenza chiede non deve dipendere da quando la si legge.

**Il giorno di confronto è quello dell'emittente**, `LocalDate.now(zone del luogo)` — lo
stesso che `WarningText.days` già usa — così un telefono all'estero non trasforma in «di
ieri» un bollettino italiano di oggi. Un bollettino datato **avanti** (un orologio che non
torna) prende la data invece di passare per quello di oggi: quattro test in
`WarningTextTest` fissano i quattro rami.

### La scheda Rugiada: era voluta, e sta scritto (8 set 2026)

Domanda del committente nello stesso giro. Non è un errore: è la review delle card dell'**8
set 2026**, sezione «Le card, com'erano e come sono» più su in questo file. Umidità e
Rugiada dicevano la stessa cosa una sotto l'altra («Confortevole» / «Gradevole»), e
«Rugiada» è la parola meno capita dello schermo. La card Umidità tiene la percentuale e la
traccia, **prende la frase dal punto di rugiada** — che è il predittore migliore di come si
sta — e stampa «Rugiada 12°» come nota: il dato non si è perso, il titolo sì. Via
`metric_dew` e le quattro `humidity_meaning_*`.

### Le caselle

### Blocco A — il convertitore, fatto (11 set 2026)

`tools/import_meteocons_v3.py`, con la geometria dei percorsi estratta in
`tools/svg_paths.py` perché serve anche alla prova (`spike_mask_clip.py`). Legge il
tarball di `@meteocons/svg`, non un clone.

| | line | flat |
|---|---|---|
| statiche convertite | **517 / 519** | **517 / 519** |
| animate | 470 | 470 |
| saltate | 2 | 2 |

Le due che saltano sono `pressure-high-alt` e `pressure-low-alt`: una maschera di contorno
alla Figma (un `<mask>` applicato a una forma sola, non a un gruppo), e non sono nella
lista di spedizione. **1 974 file, 20 MB nel repo**, `:app:assembleDebug` verde.

Quel che il rapporto del tool dichiara invece di tacere, ed è debito vero:

- **112 icone con il tratteggio reso solido.** VectorDrawable non ha `stroke-dasharray`.
  L'importatore v2 li **ridisegnava** come segmenti veri (la sua dipartenza n. 3) e qui
  quel lavoro non è stato rifatto: per ora la riga tratteggiata esce piena, che è un
  disegno diverso. Nella lista di spedizione tocca `wind` e `wind-beaufort-*`
- **42 animazioni del tratteggio perse** (`stroke-dashoffset`, le formiche in marcia). Si
  perde l'animazione, non l'icona, ed è la conseguenza del punto sopra
- **15 gradienti appiattiti** sul colore di faccia: è voluto, ed è la dipartenza n. 4 di v2
  per lo stesso motivo. Con line e flat sono un caso di bordo, non lo stile

Cose che il porting ha richiesto e che v2 non aveva: i rettangoli ad angoli arrotondati
(149 icone, fra cui tutti i `barometer*`) come quattro archi veri; le trasformazioni
statiche (`rotate(45 cx cy)` è `rotation` più il pivot, alla lettera) come gruppi
annidati; i `clip-path` interni, saltando quello grande quanto la tela che Figma mette
addosso a quasi ogni icona. La fase negativa, i `keyTimes` e i keyframe **non** sono stati
riscritti: sono le funzioni di `import_meteocons.py`, importate e usate tali e quali.

- [x] **Il tool v2 resta, i suoi drawable no.** `import_meteocons.py` non produce più niente
      di spedito, ma `import_meteocons_v3.py` ne importa `loop_keyframes`: la rotazione dei
      keyframe per una fase negativa è scritta lì ed è valida identica sulla v3
- [x] **Il tratteggio**: ridisegnare i 112 come fece v2, o dichiarare per iscritto quali
      restano pieni. Non si spedisce `wind` con una riga che il disegnatore aveva tratteggiato
      senza dirlo
      **Fatto**, e in due modi diversi perche' erano due problemi: vedi sopra.
- [x] **Il tool**: `import_meteocons.py` legge da `@meteocons/svg` / `@meteocons/svg-static`
      (versione appuntata, tarball con checksum, o CDN versionato) invece che da un clone del
      repo. Oggi il tool accetta `g`, `circle`, `path`, `defs` ed esce su tutto il resto; gli
      elementi nuovi da gestire, misurati su tutte e 519, sono `<rect>` (598, quasi sempre il
      rettangolo del `clipPath` di tela), `<mask>`, `<line>` (2) e — solo nello stile fill —
      `<filter>` + `<fe*>` (17). Va gestito anche `fill-rule="evenodd"`, che in `line` è il
      modo stesso in cui è disegnato il contorno: `android:fillType="evenOdd"`, dall'API 24
- [x] **Le maschere**: la conversione dello spike, estesa e verificata icona per icona. È il
      punto 5 della richiesta: dove *non* si può riusare l'originale, il tool lo dice e il
      motivo finisce in `UPSTREAM.md`
- [x] **I filtri**: nel sottoinsieme, con flat + line, sono **zero** — confermato sull'importazione intera, nessuna icona line o flat ne usa. Restano nello stile fill
      (17 icone su 519: tutte `compass*` e `wind-direction-*`, un'ombra portata fatta di
      `feFlood` + `feOffset` + `feComposite` + `feBlend`). Se un giorno si importasse fill, si
      **lasciano cadere**: l'ombra non porta informazione e VectorDrawable non ha filtri
- [x] **I tratteggi**, chiusi l'11 set 2026 — ed erano **due problemi diversi**, non uno:

      | | quanti | cos'è | come si dice in Android |
      |---|---|---|---|
      | `stroke-dasharray="12 9"` | 112 | tratteggio fermo, tutte **rette** (foschia, nebbia, fumo) | si **ridisegna** a segmenti veri |
      | `stroke-dasharray="50"` + `stroke-dashoffset` animato | 34 | non è un tratteggio: è una **finestra che corre** lungo la riga del vento | `trimPathStart/End` + `trimPathOffset` animato |

      Il primo caso è esatto e non approssimato: una retta di 48 unità spezzata 12 acceso /
      9 spento dà `M40,95 L52,95 M61,95 L73,95 M82,95 L88,95`, che è quel che l'SVG
      disegnerebbe. Il secondo lo è quasi: `trimPath` ha **una** finestra, quindi dove il
      tratto è più lungo del periodo (100 unità; il più lungo misura 111) l'SVG ne
      mostrerebbe due. Le due grandezze non hanno né unità né verso in comune — l'offset
      SVG è in unità di disegno e crescendo sposta il motivo **all'indietro**,
      `trimPathOffset` è una frazione e crescendo lo sposta **in avanti** — quindi la
      corsa si converte in giri di percorso e si anima da 1 a 0.
      Le icone che animano salgono da 470 a **484 per stile**: le quattordici del vento
      avevano solo quella. E `icon_filmstrip.py` ha imparato a ridisegnare `trimPath*`
      come il tratteggio da cui viene (con `pathLength="1"` le frazioni di Android **sono**
      le unità del dasharray), quindi la spazzolata si è potuta guardare: scorre
- [x] **I gradienti**: con flat + line il problema **sparisce** — sulle 519 sono **15 icone**, appiattite sul colore di faccia e dichiarate nel rapporto. Era il
      motivo della dipartenza n. 4 del tool, che a questo punto si può togliere anziché
      riscrivere. Resta da decidere solo per le tre di flat: appiattire come oggi, o portarle
      davvero (VectorDrawable **sa fare i gradienti** con `aapt:attr` su `android:fillColor`);
      se si portano, va verificato che reggano dentro i widget Glance, che caricano il
      drawable nel processo del launcher
- [x] **Le animazioni restano quelle di Meteocons** (punto 6 della richiesta — 470 icone animate per stile):
      `@meteocons/svg` è ancora **SMIL**, quindi la strada SMIL → `AnimatedVectorDrawable` che
      il tool percorre già regge. Il Lottie di v3 sarebbe l'altra strada e **non si prende**:
      vorrebbe `lottie-android` come dipendenza, non gira in un widget Glance, e si porterebbe
      dietro un runtime per un'icona da 34 dp.
      **Le animazioni sono identiche nei quattro stili** — stessi tipi, stessi conteggi, stessi
      tempi; cambia solo che negli stili mascherati la maschera porta il proprio `translate`.
      Quindi «le line animate vanno bene?» ha la stessa risposta di qualunque altro stile, e la
      risposta è sì: animano **485 icone su 519**, e **68 su 70** nel sottoinsieme. Tipi da
      coprire nel sottoinsieme: `translate` 151, `opacity` 106, `rotate` 42, `scale` 4,
      `stroke-dashoffset` 4. I primi tre il tool li fa già, `scale` è un attributo di
      `<group>`, gli ultimi quattro sono le due icone del tratteggio
- [x] **La mappatura WMO**, finalmente 1:1 con quello che il provider dice (punto 4):

| WMO | parola già a schermo | icona v3 (giorno / notte) |
|---|---|---|
| 0 | Sereno | `clear-day` / `clear-night` |
| 1 | Quasi sereno | `clear-day` / `clear-night` (deciso l'11 set 2026, sotto) |
| 2 | Poco nuvoloso | `partly-cloudy-day` / `partly-cloudy-night` |
| 3 | Coperto | `overcast` |
| 45, 48 | Nebbia | `fog-day` / `fog-night` |
| 51, 53, 55 | Pioviggine | `overcast-drizzle` |
| 56, 57 | Pioviggine gelata | `overcast-sleet` |
| 61, 63, 65 | Pioggia | `overcast-rain` |
| 66, 67 | Pioggia gelata | `overcast-sleet` |
| 71, 73, 75, 77 | Neve | `overcast-snow` |
| 80, 81 | Rovesci | `partly-cloudy-day-rain` / `-night-rain` |
| 82 | Rovesci violenti | `extreme-rain` |
| 85, 86 | Rovesci di neve | `partly-cloudy-day-snow` / `-night-snow` |
| 95 | Temporale | `thunderstorms-day` / `thunderstorms-night` |
| 96, 99 | Temporale con grandine | `thunderstorms-day-hail` / `-night-hail` |
| altro | Condizioni sconosciute | **`not-available`**, non una nuvola |

  Le righe `overcast-*` invece delle piatte `drizzle`/`rain`/`snow` hanno una misura dietro,
  dalle stesse 1 680 ore: quando piove il cielo **è** chiuso (codice 51: copertura minima 88,
  p50 100; codice 61: minima 87, p50 100; codice 80: minima 71, p50 100). Disegnare la
  pioggia senza la sua nuvola sarebbe sottrarre un fatto, non semplificare
- [x] **Un test sulla tabella**, che oggi non c'è: ogni codice WMO che il provider può
      servire → il suo drawable, per giorno e per notte, e il fallback su `not-available`.
      È la casella che impedisce alla prossima fusione silenziosa di ripetersi
- [x] **La lista di spedizione.** Era di 143, verificate una per una presenti in entrambi
      gli stili; misurando si è divisa in **83 spedite e 61 pronte** (`SHIPPED` e `PLANNED`
      in `tools/shipped_icons.py`), perché spedire un disegno che nessuna schermata nomina
      costa e non si vede. Il gruppo qui sotto è la scelta di partenza (punti 2 e 3 della
      richiesta); la colonna che conta è se una schermata la disegna:

| gruppo | n | note |
|---|---|---|
| Cieli, la mappatura WMO | 24 | la tabella sopra, `not-available` compreso |
| Dettagli, i marchi di oggi | 13 | `wind`, `humidity`, `uv-index`, `thermometer`, `barometer`, `raindrop(s)`, `mist`, `umbrella`, `snowflake`, `dust`, `smoke-particles`, `compass` |
| **Pollini** | 16 | `pollen`, `pollen-grass/tree/weed` e i loro `-low/-moderate/-high/-very-high` |
| **UV graduato** | 12 | `uv-index-1…11` e `-11-plus` |
| **Pressione graduata** | 5 | `barometer-low/moderate/high/very-high/extreme` |
| **Vento graduato** | 18 | `windsock(-calm/-weak/-moderate)`, `wind-beaufort-0…12`, `umbrella-wind` |
| **Visibilità e temperatura** | 5 | `haze`, `fog`, `smoke`, `thermometer-warmer/-colder` |
| **Direzione del vento** | 8 | `wind-direction-n…nw` — vedi la riserva sotto |
| Cielo e agenda, di oggi | 17 | alba/tramonto, luna e sue fasi, orizzonte, stelle, eclissi |
| **Arcobaleno e momenti del giorno** | 11 | `rainbow`, `rainbow-clear`, `rainbow-cloud`, `time-morning…late-night` |
| **Allerte, per tipo di rischio** | 14 | `weather-alert(-day/-night)`, `wind-alert`, `thermometer-alert`, `uv-index-alert`, `water-alert`, `fire-alert`, `avalanche-danger-alert`, `tornado`, `hurricane`, `cyclone`, `waterspout`, `falling-rocks-alert` |

- [x] **Due debiti che questa lista salda**, e sono scritti nel codice, non dedotti:
      `ChiaroIcons.pollen` oggi disegna `mc_dust` e il suo commento dice «Meteocons v2 non ha
      un'icona per i pollini (**v3 sì**) … serve finché la famiglia v3 non si stabilizza»;
      `ChiaroIcons.rainbow` disegna `mc_partly_cloudy_day_rain` e dice «Meteocons v2 non ha un
      arcobaleno». Sono le due caselle che la Fase 2 ha lasciato aperte e che qui si chiudono
- [x] **La regola sulle icone graduate**: si spedisce un glifo per banda **solo dove la banda
      è già calcolata e già detta a parole** (`WeatherText.uvMeaning`, `pressureMeaning`,
      `windMeaning`, `pollenLevel`). Se il glifo dicesse un livello che la riga accanto non
      dice, sarebbe un secondo verdetto senza la sua aritmetica (DESIGN §1.2)
- [x] **La direzione del vento: una sola icona, ruotata** (deciso l'11 set 2026, dopo che il
      committente aveva chiesto se usare le otto). Non si spediscono le otto, e le ragioni
      sono tre, tutte lette nel codice e nel disegno, non dedotte:
      1. **sarebbe un passo indietro, non un'approssimazione.** `ui/components/WindArrow.kt`
         disegna già la direzione **esatta**, ruotando di `fromDegrees + 180` in continuo:
         non sedici punti, infiniti. Otto glifi fissi vorrebbero dire secchi da 45°;
      2. i glifi hanno **le lettere N/E/S/W disegnate come path**. In italiano l'ovest è
         **O**, non W: è testo inglese dentro un'immagine, non traducibile, contro la regola
         che in questo prodotto tutto ciò che sta a schermo si localizza;
      3. l'ago di Meteocons punta da **dove il vento viene**; Chiaro punta **dove l'aria
         va**, con la sua motivazione scritta (revisione delle schede, 8 set 2026).
         Importarlo così rovescerebbe in silenzio una decisione presa.

      Quel che si fa invece, e che dà lo stesso guadagno visivo: si importa
      `wind-direction-n` **tenendo solo il gruppo `Pointer`** — l'ago è un path solo, bbox
      x 57,5–70,5 e y 40,5–84,0 in una scatola di 128, simmetrico sul centro — si butta il
      gruppo `Letters`, e lo si ruota come `WindArrow` ruota già. Risultato: l'ago di
      Meteocons, nella mano della famiglia, esatto al grado, senza lettere inglesi e con la
      convenzione di Chiaro intatta. Da guardare al filmstrip: l'ago è più dettagliato della
      freccia disegnata a mano e il tile lo mostra a **16 dp**
- [ ] **Riserva sulle allerte**: le `Alarms` entrano per il **tipo** di rischio (i quattordici
      `WarningHazard` della Fase 12), **non per il livello**. Giallo/arancione/rosso restano
      `ic_warning` tinto: DESIGN §8.13 ha scelto un disegno al peso dei segni di verdetto
      apposta, e `code-yellow/orange/red` di Meteocons sono icone a colori pieni che in quello
      slot non ci stanno. Importarle sarebbe disfare una decisione, non aggiungere un'opzione
- [~] **Le dimensioni non si toccano** (punto 7): la scatola passa da 64 a 128, ma è
      `viewportWidth`/`viewportHeight` nell'XML e i dp della scala restano quelli
      (`WeatherIconSize`: striscia 42, settimana 38, riga e tile 34). **Da verificare sul
      dispositivo**, non solo sulla carta: v3 è un disegno nuovo e potrebbe riempire la sua
      scatola diversamente, e DESIGN §13.1 dice che è la nuvola semplice a decidere come si
      legge la famiglia in piccolo. Se il peso ottico cambia, si dichiara e si rimisura la
      scala — non si cambia un padding
- [x] **Lo stile**: **line + flat**, per gli argomenti della sezione sopra, con **line
      predefinito** come oggi. Si costruiscono insieme, perché condividono il convertitore di
      maschere e non hanno nient'altro da convertire. Se lo spike delle maschere fallisce non
      cade un secondo stile: **cade la fase**, perché senza maschere il line non ha i cieli
      composti. È per questo che lo spike sta prima di tutto
- [x] **La licenza**: `licenses/Meteocons-MIT.txt` porta il testo della v3 (il copyright
      passa da «2020-2021» a «2020-present») e dice da quale pacchetto viene. `UPSTREAM.md`
      **non** andava toccato: parla solo di `:core`, e le icone sono `:app`
- [x] **DESIGN §13.1 riscritta**, e con lei §7.1 (l'importatore ha un nome nuovo), la nota
      dei generatori (erano tre, `gen_vivid_icons.py` è stato cancellato: un set per fondo gli
      ha tolto il lavoro) e **l'eccezione dichiarata del widget Cielo, rimisurata** — vedi
      sotto. `CLAUDE.md` dice che niente sotto `res/drawable/mc3*` e `MeteoconsSets.kt` si
      modifica a mano, e `README.md` conta 765 test invece di 754

### Quel che non si è potuto misurare

Le maschere non sono state **convertite**, solo lette: che la riscrittura clip-path + verso
invertito, con la coppia di gruppi annidati che tiene fermo il sole, renda identica
l'originale **non è dimostrato**, ed è il rischio numero uno della fase — per questo è uno
spike e non una casella, e per questo ora può far cadere la fase intera invece che un solo
stile. I gradienti dentro un widget Glance non sono stati provati.

E le 519 non sono state **guardate**: sono state contate, misurate sul colore, sull'elemento
e sull'animazione, mai messe su uno schermo. Vale in particolare per la scelta line + flat,
che è argomentata sulle conversioni risparmiate e **non** su come i due disegni stanno
accanto nella striscia a 42 dp. `tools/icon_filmstrip.py` esiste già ed è quello che chiude
ognuna di queste caselle.

Una correzione a questa stessa fase, registrata perché è la regola della serie: la prima
stesura contava `#fff` e `#000` fra i colori del disegno e ne ricavava che fill fosse il più
sicuro. Erano i riempimenti delle **maschere**, non inchiostro. Rimisurato a maschere escluse,
sul sottoinsieme giusto, la conclusione si è rovesciata.

---

## Note trasversali

- **Il fork non si dimentica**: quando un bug del core va corretto due volte, si estrae
  `weather-core` (VISION §7.3). `UPSTREAM.md` è quello che rende l'estrazione un
  pomeriggio invece che uno scavo.
- **Batteria**: un solo job periodico per tutto, allarmi inesatti, nessun servizio in
  foreground, nessuna posizione in background. Vale già da adesso, non da una fase di
  ottimizzazione.
- **Niente radar**: il provider non ha immagini. È una posizione dichiarata, non una
  mancanza da nascondere. Riesaminata il 9 set 2026 (Radar-DPC esiste, è libero, copre l'Italia:
  le misure sono nella Fase 11) e lasciata così per scelta del committente.
- **Le allerte ufficiali si citano, non si traducono** (VISION §8, dal 9 set 2026): livello,
  rischio, giorno e zona localizzano; le parole di un'autorità restano sue, nella sua lingua,
  etichettate come tali. E in Italia fa fede la Protezione Civile, sempre.

### Tre taglie, guardate sul dispositivo (committente, 12 set 2026)

Tre richieste da tre screenshot, e la densità del dispositivo di riferimento rimisurata
per strada. **È 2,8125 px/dp, non 2,75**: sullo screenshot di Oggi il margine da 16 dp dei
tile misura 45 px e la gronda da 12 ne misura 34, quindi lo schermo è largo **384 dp** e un
tile è `(384 − 32 − 12) / 2 = 170` dp, cioè i 478 px misurati. Tutti i numeri sotto sono
letti a quella scala.

**1. L'icona del widget è «un po' grande».** Vero, ed era il modo in cui la misura era
scritta: il glifo di una scheda a una riga prendeva tutta l'altezza concessa meno i
paddings, e basta. Sulla scheda a quattro celle del dispositivo la riga è ~85 dp, quindi il
glifo usciva a **73 dp** — con accanto un blocco di parole alto 66, cioè la riga della
temperatura (34 sp × 1,32) più quella del posto (16 × 1,32). Il tetto nuovo,
`RowIconMax = 66 dp`, è quel blocco: **il glifo non supera le parole che gli stanno
accanto**. Sulla scheda di riferimento toglie il 10% (50,5 → 45,5 dp d'inchiostro sulla
media geometrica 0,69 della famiglia) e, come effetto secondario che vale da solo, rende
uguale il glifo di ogni riga da ~78 dp in su: prima un launcher che concede righe da 101 dp
lo portava a 89 senza che nulla accanto crescesse. È lo stesso tetto che il widget Cielo si
era dato l'8 set (`SkyHeroIconMax`, 72 dp contro un orologio da 30 sp), letto sull'ancora di
questa scheda invece che su quella. Le schede strette non si muovono: a due celle lega la
larghezza, sotto il tetto. La colonna della frase guadagna i 4 dp che il glifo lascia
(116 → 118 a quattro celle).

**2. La pastiglia del Cielo va a capo troppo facilmente.** La misura dice perché:
`supportingContent` è la colonna di testo della riga Material, e una colonna di testo
finisce dove comincia lo slot in coda. Material spende **163 dp** di una riga con
campanella in margini e slot fissi: `16 (bordo) + 51 (il glifo) + 16 + 16 + 48 (la
campanella) + 16`. **Il quarto 16 è vero e mancava ai conti scritti l'11 set**: sullo
screenshot del dispositivo la pastiglia finisce a 303,6 dp e la scatola da 48 della
campanella è centrata a 344, quindi fra le due ci sono 16,4 dp. La nota su
`WeatherIconSize.Sky` è corretta di conseguenza — il budget di una riga con campanella
passava da 214 a 197 dp, non da 230 a 213; quello di una riga senza («In arrivo» senza
promemoria) non ha quel gap ed era giusto, 278 → 261.

Restano quindi **221 dp** a 384 e 197 a 360. «✗ Niente da fare  nuvole 66%» ne misura
**223**: 24 di padding, 14 di segno, due gap da 6, 92,4 di parola e 77 di numero, più 3,8
di margini laterali letti sulla pastiglia «✓ Bello  nuvole 0%» che le sta sotto e che su
una riga ci sta, a 154. **Ha sbagliato di due dp**, e quel che è andato a capo è il numero
sotto la parola: esattamente la coppia che DESIGN §8.7 non lascia separare. Due dp non sono
un margine, ed è questo che vuol dire «non va a capo facilmente»: la coppia più larga che
l'app possa stampare, «Niente da fare» con «pioggia 100%», ne vuole ~236.

La pastiglia scende quindi su una riga sua, sotto tutta la riga della lista, e siccome
della campanella lì non arriva niente **si prende anche la sua colonna**: `schermo − 99`,
cioè 285 dp a 384 e 261 a 360, sopra i 236 del caso peggiore in tutt'e due. Le alternative
sono state misurate prima di scartarle, e nessuna compra un margine: una campanella da
40 dp vale 8 dp (il bersaglio da 48 resta, `IconButton` lo estende oltre i propri limiti),
una pastiglia più stretta ne vale 4, e i 51 dp del glifo sono una decisione dell'11 set,
non del gioco. Il prezzo è verticale e va scritto: senza la pastiglia dentro, la riga
diventa a due righe di testo, quindi Material centra i suoi 44 dp nei 72 di minimo e
**l'aria sopra la pastiglia passa da 6 a ~14 dp**, con la riga che cresce di ~6. Vale per
i momenti e per «In arrivo».

**3. Il numero dell'UV deve leggersi come quello dei Pollini.** Le due icone portano lo
**stesso** distintivo — un quadrato stondato di 30 unità su 128, col valore dentro — e
escono diverse per via della normalizzazione, non del disegno: il sole dell'UV arriva già
agli angoli e prende scala **0,92**, la spiga dei pollini è compatta e prende **1,3382**.
A parità di scatola il distintivo dei pollini è quindi 1,45× quello dell'UV, ed è quel che
si misura sullo screenshot: 33 px contro 40.

La sola leva rimasta al punto di chiamata è la scatola, e la scatola è quel che si muove:
`WeatherIconSize.TileUv = 55 dp` (`38 × 1,3382 / 0,92 = 55,3`) mette i due distintivi a
**11,85 contro 11,92 dp**, lo 0,6% di distanza. I pollini degli alberi sono scalati 1,3109,
quindi il loro distintivo è 11,68 e l'UV gli sta l'1,5% sopra invece che sotto; il
confronto chiesto era con l'erba, che è quel che lo screenshot mostrava.

Quel che costa, scritto perché è una deroga vera alla scala di §13.1: **questo gradino sta
il 45% sopra `Tile`**, quindi il glifo dell'UV è l'oggetto più grande della griglia dei
dettagli, e la sua riga d'intestazione cresce di 17 dp — che paga anche il tile accanto,
perché una coppia condivide un'altezza. Quel che **non** costa è il budget dell'etichetta,
che era la ragione per cui il gradino dei tile si era fermato a 38: `118 − 55` lascia 63 dp
a 360 dp per le due lettere «UV», la più corta che l'app spedisca.

- [ ] **Da guardare sul dispositivo.** Le tre sono aritmetica, non disegno: il widget a 66
      dp, l'aria sopra la pastiglia del Cielo, e soprattutto se l'UV a 55 dp accanto a un
      Vento a 38 si legge come una gerarchia o come un errore. Se è la seconda, la strada
      che resta non è un numero intermedio (non pareggerebbe niente) ma chiedere
      all'importatore una scala per famiglia — cioè un'eccezione dichiarata dentro
      `icon_ink.scale_of`, non al punto di chiamata

`MeteoconScaleTest` tiene il pareggio del punto 3 misurando le due scale **nei drawable
spediti**, non nei numeri di questa pagina: se l'importatore rigirasse con un `TARGET`
diverso, o Meteocons ridisegnasse una delle due, il rapporto cambierebbe e il commento no.
Misura anche il distintivo prima di confrontarlo, perché la premessa «è lo stesso quadrato»
è la metà che si rompe in silenzio. `NowWidgetLayoutTest` fissa il tetto del punto 1 a tre
altezze di riga (70, 82, 85, 130 dp) e la densità rimisurata sta nella sua intestazione.

---

## La review della base dati: cosa cresce, cosa non se ne va mai (committente, 12 set 2026)

La domanda era tre domande: c'è una logica che cancella i dati vecchi dopo una certa
data, il database cresce e basta giorno dopo giorno, e ci sono isole di dati che non
vengono mai cancellate. Le risposte, nell'ordine: **no e va bene così**, **no**, **sì, ed
erano quattro**.

### Quel che c'era già, e perché una scadenza a tempo sarebbe stata sbagliata

Nessuna retention dell'app è a tempo, e non è una dimenticanza. `weather_history` tiene
gli ultimi 100 commit **per luogo** (`pruneCity`) con un tetto globale a 800 righe
(`prune`), `warning_records` teneva 100 righe per luogo, `ReportDiskCache` tiene 16 file e
butta quelli oltre l'orizzonte della previsione, e i cinque store di impronte
(`AlertStateStore`, `RuleStateStore`, `SkyAlertStateStore`, `FetchLogStore`,
`OfficialWarningStore`) sono anelli limitati a 16-40 voci. Un conteggio, non una data: un
telefono spento per una settimana deve ritrovare il suo Diario, e «cancella tutto quel che
ha più di N giorni» glielo svuoterebbe proprio nel caso in cui l'utente ne ha più bisogno.
Il volume, del resto, non è il problema — 800 commit da ~1,6 KB sono ~1,3 MB, e ci si
arriva solo con otto luoghi attivi.

Il problema è un altro, e il conteggio per luogo non lo vede: **le righe di un luogo che
non esiste più**. Non sono vecchie, sono orfane, e nessuna lettura futura le toccherà mai.

### Le quattro isole

1. **`warning_records` non aveva nessun tetto globale.** `pruneCity` limita il luogo che
   gli viene passato e nessun altro, quindi ogni chiave che smetteva di essergli passata
   — un luogo tolto dalla lista, e soprattutto ognuna delle celle da ~1,1 km che la
   pseudo-città GPS conia — si teneva le sue righe per la vita dell'installazione. Era
   l'unica tabella dell'app senza soffitto.
2. **Il ramo «bollettino non raggiunto» scriveva senza potare.** `pruneCity` stava solo
   nel ramo in cui un bollettino si muove davvero, e un emittente irraggiungibile per una
   stagione è esattamente quando non si muove. Una riga al giorno è lenta, ma lenta e
   illimitata è comunque illimitata.
3. **Nessuna delle due tabelle si chiedeva *di chi* fossero le righe.** Il backstop
   globale della cronologia sfratta per età globale, quindi le righe di una cella
   abbandonata venivano evicted alla stessa velocità di quelle di una città seguita: la
   domanda sbagliata. Stessa cosa per i file di `ReportDiskCache`, che è limitato per
   numero e pota solo in scrittura.
4. **`WidgetCityStore.forget` dimenticava la chiave `widget_sky_`.** La riga del cielo è
   arrivata dopo (Fase 16e) e non è mai stata aggiunta a `forget`: ogni widget rimosso
   lasciava il suo flag nel file per sempre, e un widget nuovo a cui il sistema avesse
   dato quell'id si ritrovava una riga che nessuno aveva chiesto.

### Quel che si è fatto

`StoredDataSweep` (`:core:data`, `local/`) è la terza retention, e la sola che guarda la
chiave invece del conteggio: **una chiave che l'app non segue più, ferma da `Grace`, se ne
va tutta intera**. Le regole, e il perché di ognuna:

- *Non segue più* è la lista dei luoghi salvati più il fix GPS corrente, niente altro. I
  pin dei widget si risolvono contro quella stessa lista (o contro il sentinella GPS),
  quindi non hanno voce in capitolo; le celle attraversate ieri **non** ci sono, ed è
  tutto il punto.
- *Ferma da `Grace`* si misura dalla riga **più recente** della chiave, e la cancellazione
  è tutto-o-niente per chiave. Lo swipe di rimozione ha un undo e ri-aggiungere un luogo è
  un tap: un Diario che tornasse con le pagine di mezzo strappate sarebbe peggio di uno
  che torna vuoto.
- `Grace` è **sette giorni**, la stessa settimana che la previsione raggiunge: abbastanza
  perché un undo, un ripensamento o un telefono in un cassetto non costino il Diario a
  nessuno, abbastanza poco perché due settimane di pendolarismo non lascino due settimane
  di celle morte.
- Un insieme di chiavi vive **vuoto** è uno stato vero (l'ultimo luogo tolto, GPS spento)
  e vuol dire «qui non è vivo niente», non «stai fermo». Room espande una lista vuota in
  `NOT IN ()`, che SQLite non parsa, quindi passa un sentinella (`""`) che nessuna
  `cacheKey` — sempre `<int>:<int>` — può valere.

Gira **dove già girano le altre due**, cioè nel punto unico in cui atterrano dati nuovi
(`WeatherRepository.recordHistory`), dietro un `onHousekeeping` iniettato da
`ServiceLocator` come già faceva `onHistoryCommitted`: la spazzata ha bisogno della lista
dei luoghi salvati, che il repository per scelta non conosce. È `bestEffort` per conto suo
e non dentro il blocco dell'altro hook, perché una pulizia che fallisce deve comunque
lasciare al widget il suo repaint.

Accanto, tre riparazioni puntuali: il backstop globale di `warning_records`
(`RETENTION * PLACES` = 800, gemello di quello della cronologia e generoso per la stessa
ragione), la potatura anche sul ramo del bollettino mancato, e `widget_sky_` dentro
`forget`.

### Quel che si è visto e NON si è toccato

- **`WidgetCityStore.remap` è codice morto.** Nessuno lo chiama: il receiver non ha un
  `onRestored`, e `allowBackup` è `true` senza `dataExtractionRules`. Dopo un ripristino
  da backup ogni widget prende un id nuovo, quindi i pin restano appesi ai vecchi id (che
  non se ne vanno mai) e i widget ripristinati perdono la loro città. È un bug vero, ma
  ripararlo davvero vuol dire decidere anche per `WidgetLookStore` e `ArcSettingsStore`,
  che un `remap` non ce l'hanno affatto: è una scelta di prodotto, non una pulizia, e sta
  qui in attesa.
- **Nessun indice su `city_key`** in nessuna delle due tabelle: ogni `historyFor`,
  `observeFor` e `pruneCity` è una scansione completa. A 800 righe non si misura, e
  comprarlo costa una versione di schema: si rivaluta se i tetti crescono.
- **`ReportDiskCache.prune()` gira solo in scrittura.** La spazzata ora copre il caso che
  contava (i file orfani); quello che resta — un'app che non fetcha più e tiene i suoi
  file — è un'app che non sta girando.
- **I tetti a conteggio restano conteggi.** `HISTORY_CITIES = 8` è generoso apposta, e con
  le chiavi morte tolte di mezzo la pressione sul backstop globale sparisce quasi del
  tutto.

Tre test nuovi in `:core:data`: `StoredDataSweepTest` (sei casi, compreso il tutto-o-niente
per chiave e l'insieme vivo vuoto), `WarningRecordDaoTest` per il backstop che mancava, e
un caso in più in `WidgetCityStoreTest` per la chiave del cielo. Suite a **793 verdi**.
