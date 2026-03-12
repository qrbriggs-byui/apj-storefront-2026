package edu.byui.apj.storefront.apimongo.service;

import edu.byui.apj.storefront.apimongo.model.TradingCard;
import edu.byui.apj.storefront.apimongo.repository.TradingCardRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TradingCardService {

    private final TradingCardRepository repository;

    public TradingCardService(TradingCardRepository repository) {
        this.repository = repository;
    }

    public List<TradingCard> findAll(String sort, int page, int size) {
        Pageable pageable;
        if ("price".equalsIgnoreCase(sort)) {
            pageable = PageRequest.of(page, size, Sort.by("price"));
        } else if ("name".equalsIgnoreCase(sort)) {
            pageable = PageRequest.of(page, size, Sort.by("name"));
        } else {
            pageable = PageRequest.of(page, size);
        }

        return repository.findAll(pageable).getContent();
    }

    public List<TradingCard> getFeatured(String q) {
        String effective = (q == null || q.isBlank()) ? "Java" : q;
        return search(effective);
    }

    public Optional<TradingCard> getById(String id) {
        return repository.findById(id);
    }

    public List<TradingCard> filterBySpecialty(String specialty) {
        return repository.findBySpecialty(specialty);
    }

    public List<TradingCard> filterByPrice(double minPrice, double maxPrice) {
        return repository.findByPriceBetween(minPrice, maxPrice);
    }

    public List<TradingCard> search(String query) {
        return repository.findByNameContainingIgnoreCaseOrContributionContainingIgnoreCase(query, query);
    }
}
