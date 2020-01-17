package de.untenrechts.homebackend.controller.types;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class AuthType {

    private String username;
    private String password;

    public AuthType() {
    }

}
