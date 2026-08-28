====================================================
  GuessTheWord — Istruzioni di avvio
====================================================

REQUISITI
---------
- JDK 1.8 (Java 8) installato in:
  C:\Program Files\Java\jdk-1.8\

  Se non presente, scaricare da:
  https://bell-sw.com/pages/downloads/#jdk-8-lts
  (Liberica JDK 8 Standard)


STRUTTURA CARTELLA
------------------
eseguibili/
  server.jar        -> applicazione server (con pannello admin)
  client.jar        -> applicazione client (interfaccia giocatore)
  server.properties -> configurazione porta server (default: 5000)
  client.properties -> configurazione host/porta client
  analisi.ser       -> analisi del testo pre-calcolata
  readme.txt        -> questo file


AVVIO SERVER
------------
1. Aprire PowerShell nella cartella eseguibili:
   cd C:\Users\...\eseguibili

2. Avviare il server con JDK 8:
   & "C:\Program Files\Java\jdk-1.8\bin\java" -jar server.jar

3. Si apre il Pannello Admin. L'analisi viene caricata
   automaticamente da analisi.ser all'avvio.
   (Se non caricata: cliccare Seleziona Documenti ->
    scegliere il file di testo -> Avvia Analisi ->
    Salva Analisi)

4. Il server resta in ascolto sulla porta 5000.


AVVIO CLIENT (ripetere per ogni giocatore)
------------------------------------------
1. Aprire un nuovo PowerShell nella cartella eseguibili.

2. Avviare il client con JDK 8:
   & "C:\Program Files\Java\jdk-1.8\bin\java" -jar client.jar

3. Nella schermata di login:
   - Inserire username e password
   - Cliccare LOGIN (se gia registrato)
   - Cliccare REGISTRATI (primo accesso)

IMPORTANTE: il server accetta esattamente 2 client alla volta.
Avviare ENTRAMBI i client e fare login su ENTRAMBI prima
che uno dei due possa procedere.


AVVIO PARTITA
-------------
1. Entrambi i client devono aver fatto login.
2. Su entrambi: cliccare PARTITA nella barra di navigazione.
3. Scegliere la difficolta:
   - Facile    (60 secondi, shift basso)
   - Medio     (40 secondi, shift medio)
   - Difficile (20 secondi, shift alto)
4. Cliccare AVVIA PARTITA su entrambi i client.
5. Apparira il testo con la parola cifrata evidenziata.
6. Digitare la parola decifrata e cliccare OK.
7. Vince chi risponde correttamente per primo.
   Se il tempo scade senza risposta corretta: pareggio.


CREDENZIALI ADMIN (pannello server)
------------------------------------
Username: admin
Password: admin


NOTE
----
- Il database (guesstheworld.db) viene creato automaticamente
  nella cartella eseguibili al primo avvio del server.
- Storico e Classifica sono disponibili nel menu del client.
- Per una nuova sessione di gioco riavviare server e client.

====================================================
