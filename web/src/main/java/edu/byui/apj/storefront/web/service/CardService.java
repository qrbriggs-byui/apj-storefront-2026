package edu.byui.apj.storefront.web.service;

import edu.byui.apj.storefront.web.data.CsvCardRepository;
import edu.byui.apj.storefront.web.model.Card;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CardService {

    private final CsvCardRepository repo;

    public CardService(CsvCardRepository repo) {
        this.repo = repo;
    }

    public List<Card> getFeaturedCards(String q) {
        // if q is null or empty, default to "Java" for featured cards
        String effective = (q == null || q.isBlank()) ? "Java " : q;
        return repo.search(effective);
    }

    public List<Card> getAll() {
        return repo.findAll();
    }

    public Optional<Card> getById(Long id) {
        return repo.findById(id);
    }
}