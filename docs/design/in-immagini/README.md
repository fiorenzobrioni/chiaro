# «In immagini»: i sorgenti del design

Le tavole del canvas approvato dal committente il 15 nov 2026
(https://claude.ai/artifact/VFwVjbxyAyWcWRLcRW9uhY, pagina «Chiaro»), **congelate a quel giorno**.
Sono il riferimento della Fase 17 (`PLANNING.md`, «Fase 17 — «In immagini», il widget disegnato»):
dove il piano non scrive una misura, la misura è qui.

| File | Tavola |
|---|---|
| `Anatomy.dc.html` | La card 4×2 al doppio, i dieci pezzi nell'ordine di lettura |
| `Forms.dc.html` | Le otto forme, da 2×1 a 4×4, alle misure vere in dp |
| `Config.dc.html` | La schermata di impostazioni (interattiva nel canvas) |
| `Grounds.dc.html` | La card 4×2 su dodici fondi |

**Come si leggono.** Sono pagine del formato Design Component del canvas: senza il suo runtime non
si aprono in un browser, si leggono come sorgente. Gli stili inline sono le misure (`px` = dp,
`font-size` = sp); la geometria dei disegni (la curva, le barre della pioggia, il nastro, il
quadrante) è calcolata nella classe `Component` in fondo a ogni file, da `renderVals()`: le funzioni
`chart`, `dial`, `ribbon`, `conic`. I dati (Milano, 15 nov, ore 14) sono d'esempio: il widget legge
quelli veri.

**Non si modificano.** Una misura che l'implementazione cambia si scrive in `PLANNING.md` con il
motivo (la regola della fedeltà della Fase 17), non qui.
