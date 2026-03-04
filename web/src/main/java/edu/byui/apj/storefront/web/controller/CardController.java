package edu.byui.apj.storefront.web.controller;

import edu.byui.apj.storefront.web.model.Card;
import edu.byui.apj.storefront.web.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "Cards", description = "Operations related to pioneer cards")
public class CardController {

    private final CardService service;

    public CardController(CardService service) {
        this.service = service;
    }

    /**
     * Returns featured cards.
     *
     * If q is omitted, defaults to "Java".
     */
    @Operation(
            summary = "Get featured cards",
            description = "Returns cards matching the query. Defaults to Java if no query provided."
    )
    @GetMapping("/featured")
    public List<Card> featured(@RequestParam(required = false) String q) {
        return service.getFeaturedCards(q);
    }

    /**
     * Returns all cards.
     */
    @Operation(summary = "Get all cards")
    @GetMapping
    public List<Card> all() {
        return service.getAll();
    }

    /**
     * Returns all cards.
     */
    @Operation(summary = "Gets a single card by ID",
               description = "Returns a card by ID. Returns a 404 error if the specified card is not found.")
    @GetMapping("/{id}")
    public ResponseEntity<Card> byId(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}