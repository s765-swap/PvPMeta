    class DuelEntry {
   private String player1Uuid;
   private String player2Uuid;
   private String winnerUuid;
   private String loserUuid;
   private long timestamp;

   public DuelEntry() {
   }

   public DuelEntry(String var1, String var2, String var3, String var4, long var5) {
      this.player1Uuid = var1;
      this.player2Uuid = var2;
      this.winnerUuid = var3;
      this.loserUuid = var4;
      this.timestamp = var5;
   }

   public String getPlayer1Uuid() {
      return this.player1Uuid;
   }

   public String getPlayer2Uuid() {
      return this.player2Uuid;
   }

   public String getWinnerUuid() {
      return this.winnerUuid;
   }

   public String getLoserUuid() {
      return this.loserUuid;
   }

   public long getTimestamp() {
      return this.timestamp;
   }

   public void setPlayer1Uuid(String var1) {
      this.player1Uuid = var1;
   }

   public void setPlayer2Uuid(String var1) {
      this.player2Uuid = var1;
   }

   public void setWinnerUuid(String var1) {
      this.winnerUuid = var1;
   }

   public void setLoserUuid(String var1) {
      this.loserUuid = var1;
   }

   public void setTimestamp(long var1) {
      this.timestamp = var1;
   }

   public String toString() {
      return "DuelEntry{player1Uuid='" + this.player1Uuid + "', player2Uuid='" + this.player2Uuid + "', winnerUuid='" + this.winnerUuid + "', loserUuid='" + this.loserUuid + "', timestamp=" + this.timestamp + "}";
   }
}
