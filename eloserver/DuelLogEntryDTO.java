class DuelLogEntryDTO {
   private String player1Username;
   private String player2Username;
   private String winnerUsername;
   private String loserUsername;
   private long timestamp;

   public DuelLogEntryDTO(String var1, String var2, String var3, String var4, long var5) {
      this.player1Username = var1;
      this.player2Username = var2;
      this.winnerUsername = var3;
      this.loserUsername = var4;
      this.timestamp = var5;
   }

   public String getPlayer1Username() {
      return this.player1Username;
   }

   public String getPlayer2Username() {
      return this.player2Username;
   }

   public String getWinnerUsername() {
      return this.winnerUsername;
   }

   public String getLoserUsername() {
      return this.loserUsername;
   }

   public long getTimestamp() {
      return this.timestamp;
   }
}
