package main.player;

import org.msgpack.annotation.Index;

import java.io.Serializable;

public class SignInData implements Serializable {
    @Index(0)
    public String email;
    @Index(1)
    public String password;

    public SignInData(String email, String password){
        this.email = email;
        this.password = password;
    }

    public SignInData(){

    }
}

