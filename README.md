====================================================
  GuessTheWord — Istruzioni di avvio
====================================================

REQUISITI
---------
- JDK 1.8 (Java 8) installato:

!!Se non presente, scaricare da:
  https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html
!!Scaricare una versione con ancora integrata la libreria di JavaFX

CREDENZIALI ADMIN
username:admin
password:admin

CREDENZIALI GIOCATORI
giocatore 1 -> username:giulius password:123
giocatore 2 -> username:caius password:123

STRUTTURA CARTELLA
------------------
eseguibili/
  server.jar                	-> applicazione server (con pannello admin)
  client.jar                 	-> applicazione client (interfaccia giocatore)
  server.properties   		-> configurazione porta server (default: 5000)
  client.properties    		-> configurazione host/porta client
  data/               		
	/analisi		-> cartella contenente le analisi dei vari documenti in formato serializzato
	/documenti		-> cartella contenente una copia dei documenti originali delle analisi
  database/		
	guesstheword.db		-> database del progetto
  documento_testing.txt		-> file .txt da usare per il testing dell'analisi
  readme.txt             	-> questo file


AVVIO SERVER
------------
1. Aprire PowerShell/Terminale nella cartella eseguibili:
   (WINDOWS) cd C:\Users\...\eseguibili
   (MACOS) cd ~/.../eseguibili

2. Avviare il server con JDK 8:
   (WINDOWS) & "C:\Program Files\Java\jdk-1.8\bin\java" -jar server.jar
   (MACOS) java -jar server.jar

3. Nella schermata di login inserire
	username: admin
	password: admin

4. Cliccare LOGIN

5. Si apre il Pannello Admin. Le analisi vengono
   caricate all'avvio, successivamente al login.
   (Se è il primo avvio allora: 
	-> cliccare Seleziona Documenti
	-> scegliere il file di testo fornito nella cartella degli eseguibili o uno o più file di testo a scelta
	-> Avvia Analisi
	-> Salva Analisi
	-> Carica Analisi
    )

6. Il server resta in ascolto sulla porta 5000.


AVVIO CLIENT (ripetere per ogni giocatore)
------------------------------------------
1. Aprire un nuovo PowerShell/Terminale nella cartella eseguibili.

2. Avviare il client con JDK 8:
   (WINDOWS) & "C:\Program Files\Java\jdk-1.8\bin\java" -jar client.jar
   (MACOS) java -jar client.jar

3. Nella schermata di login:
   - Inserire username e password (
	username e password nuovi, o account preregistrati di test:

	username: giulius
	password: 123

	username: caius
	password: 123
      )
	
   - Cliccare LOGIN (se gia registrato) o cliccare REGISTRATI (se si vuole creare un nuovo account)

IMPORTANTE: il server accetta massimo 2 client connessi alla volta.


AVVIO PARTITA
-------------
1. Entrambi i client devono aver fatto login o comunque la partita non incomincia prima che entrambi siano loggati.
2. Su uno cliccare NUOVA PARTITA.
3. Scegliere la difficoltà
4. Cliccare AVVIA PARTITA su entrambi i client.
5. Apparira il testo con la parola cifrata evidenziata.
6. Digitare la parola decifrata e cliccare Invia.
7. Vince chi risponde correttamente per primo.
   Se il tempo scade senza risposta corretta: pareggio.


NOTE
----
- Il database (guesstheworld.db) è già presente nella cartella
  nella cartella /database degli eseguibili è popolato con dei 
  giocatori preesistenti per il testing.
- Storico e Classifica sono disponibili nel menu del client.

====================================================
