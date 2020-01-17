package de.untenrechts.homebackend.controller;

import de.untenrechts.homebackend.controller.types.AuthType;
import de.untenrechts.homebackend.controller.types.AuthorizedRequest;
import de.untenrechts.homebackend.controller.types.RestPurchase;
import de.untenrechts.homebackend.database.repositories.PurchaseRepository;
import de.untenrechts.homebackend.database.repositories.UserRepository;
import de.untenrechts.homebackend.database.types.Purchase;
import de.untenrechts.homebackend.database.types.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/purchase")
@Slf4j
public class PurchaseController {

    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;

    @Autowired
    public PurchaseController(PurchaseRepository purchaseRepository, UserRepository userRepository) {
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
    }

    @RequestMapping(
            path = "/create",
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<List<Purchase>> createPurchase(@Valid @RequestBody AuthorizedRequest<List<RestPurchase>> createRequest) {
        if (isAuthorized(createRequest.getAuthType())) {
            List<Purchase> requestedPurchases = createRequest.getRequestObject().stream()
                    .map(Purchase::new)
                    .collect(Collectors.toList());
            List<Purchase> persistedPurchases = purchaseRepository.saveAll(requestedPurchases);
            return new ResponseEntity<>(persistedPurchases, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @RequestMapping(
            path = "/list",
            method = RequestMethod.POST,
            produces = "application/json"
    )
    public ResponseEntity<List<Purchase>> getAllPurchases(@Valid @RequestBody AuthorizedRequest<?> listRequest) {
        if (isAuthorized(listRequest.getAuthType())) {
            List<Purchase> purchaseList = purchaseRepository.findAll();
            return new ResponseEntity<>(purchaseList, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    private boolean isAuthorized(AuthType authObject) {
        String username = authObject.getUsername();
        String providedPasswordHexString = authObject.getPassword();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            String knownPasswordHexString = userOpt.get().getPassword();
            return providedPasswordHexString.equals(knownPasswordHexString);
        }
        return false;
    }

}
