package main.player;

import org.msgpack.annotation.Index;

import java.io.Serializable;

public class SignUpData implements Serializable {
    @Index(0)
    public String email;
    @Index(1)
    public String password;
    @Index(2)
    public String confirmationCode;

    public SignUpData(String email, String password, String  confirmationCode){
        this.email = email;
        this.password = password;
        this.confirmationCode = confirmationCode;
    }

    public SignUpData(){

    }
}
