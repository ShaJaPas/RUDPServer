package main.player;

public class ConfCode {
    public String code;
    public long timestamp;
    private static final long TIMEOUT = 1000 * 60 * 5;
    public ConfCode(String code, long timestamp){
        this.code = code;
        this.timestamp = timestamp;
    }

    public boolean update(long time){
        return time - timestamp < TIMEOUT;
    }
}
