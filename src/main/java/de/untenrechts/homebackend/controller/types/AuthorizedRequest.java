package de.untenrechts.homebackend.controller.types;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorizedRequest<T> {

    private AuthType authType;
    private T requestObject;

    public AuthorizedRequest() {
    }

}
