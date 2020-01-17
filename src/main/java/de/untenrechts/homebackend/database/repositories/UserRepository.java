package de.untenrechts.homebackend.database.repositories;

import de.untenrechts.homebackend.database.types.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u where u.username= :username")
    Optional<User> findByUsername(@Param("username") @NonNull String username);

}
