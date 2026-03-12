package edu.byui.apj.storefront.apimongo.repository;

import edu.byui.apj.storefront.apimongo.model.TradingCard;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TradingCardRepository extends MongoRepository<TradingCard, String> {

    List<TradingCard> findBySpecialty(String specialty);

    List<TradingCard> findByPriceBetween(double min, double max);

    List<TradingCard> findByNameContainingIgnoreCaseOrContributionContainingIgnoreCase(
            String name, String contribution);
}