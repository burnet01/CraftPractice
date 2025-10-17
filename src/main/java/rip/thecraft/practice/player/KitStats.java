package rip.thecraft.practice.player;

import org.bson.Document;

public class KitStats {
    private int elo = 1000;
    private int wins = 0;
    private int losses = 0;
    
    public KitStats() {}
    
    public KitStats(int elo, int wins, int losses) {
        this.elo = elo;
        this.wins = wins;
        this.losses = losses;
    }
    
    public int getElo() {
        return elo;
    }
    
    public void setElo(int elo) {
        this.elo = elo;
    }
    
    public int getWins() {
        return wins;
    }
    
    public void setWins(int wins) {
        this.wins = wins;
    }
    
    public int getLosses() {
        return losses;
    }
    
    public void setLosses(int losses) {
        this.losses = losses;
    }
    
    public double getWinRate() {
        int total = wins + losses;
        return total == 0 ? 0 : (double) wins / total * 100;
    }
    
    public Document serialize() {
        Document document = new Document();
        document.put("elo", elo);
        document.put("wins", wins);
        document.put("losses", losses);
        return document;
    }
    
    public static KitStats deserialize(Document document) {
        return new KitStats(
            document.getInteger("elo", 1000),
            document.getInteger("wins", 0),
            document.getInteger("losses", 0)
        );
    }
}
