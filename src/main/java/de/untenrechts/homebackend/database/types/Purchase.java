package de.untenrechts.homebackend.database.types;

import de.untenrechts.homebackend.controller.types.RestPurchase;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;

@Getter
@Setter
@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "buyer", nullable = false)
    private String buyer;
    @Column(name = "market")
    private String market;
    @Column(name = "date_bought", nullable = false)
    private Date dateBought;
    @Column(name = "product_category", nullable = false)
    private ProductCategory productCategory;
    @Column(name = "product_name", nullable = false)
    private String productName;
    @Column(name = "price", nullable = false)
    private BigDecimal price;

    public Purchase() {
    }

    public Purchase(RestPurchase restPurchase) {
        this.buyer = restPurchase.getBuyer();
        this.market = restPurchase.getMarket();
        this.dateBought = restPurchase.getDateBought();
        this.productCategory = restPurchase.getProductCategory();
        this.productName = restPurchase.getProductName();
        this.price = restPurchase.getPrice();
    }

}
