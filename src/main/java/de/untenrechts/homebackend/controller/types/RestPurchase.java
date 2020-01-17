package de.untenrechts.homebackend.controller.types;

import de.untenrechts.homebackend.database.types.ProductCategory;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Date;

@Getter
@Setter
public class RestPurchase {

    private String buyer;
    private String market;
    private Date dateBought;
    private ProductCategory productCategory;
    private String productName;
    private BigDecimal price;

    public RestPurchase() {
    }

}
