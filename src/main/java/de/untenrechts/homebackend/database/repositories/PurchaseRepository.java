package de.untenrechts.homebackend.database.repositories;

import de.untenrechts.homebackend.database.types.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {


}
