package com.mycompany.server;


public class RisultatoPartita {

    private final int id;
    private final String giocatore1;
    private final String giocatore2;
    private final String vincitore;    
    private final String data;         
    private final long tempoRisposta;  //millisecondi

    public RisultatoPartita(int id, String giocatore1, String giocatore2,
                            String vincitore, String data, long tempoRisposta) {
        this.id = id;
        this.giocatore1 = giocatore1;
        this.giocatore2 = giocatore2;
        this.vincitore = vincitore;
        this.data = data;
        this.tempoRisposta = tempoRisposta;
    }

    public int getId(){
        return id; 
    }
    public String getGiocatore1(){
        return giocatore1;
    }
    public String getGiocatore2(){
        return giocatore2; 
    }
    public String getVincitore(){
        return vincitore; 
    }   
    public String getData(){
        return data; 
    }
    public long getTempoRisposta(){
        return tempoRisposta; 
    }
}