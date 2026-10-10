package com.krishilink.repository;

import com.krishilink.entity.Lot;
import com.krishilink.entity.Offer;
import com.krishilink.entity.User;
import com.krishilink.enums.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    List<Offer> findByBuyer(User buyer);
    List<Offer> findByLot(Lot lot);
    List<Offer> findByLotOwner(User owner);
    List<Offer> findByStatus(OfferStatus status);
}
