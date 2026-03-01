package edu.byui.apj.storefront.web.controller;

import edu.byui.apj.storefront.web.service.CardService;
import edu.byui.apj.storefront.web.model.Card;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/featured")
    public List<Card> featuredCards() {
        return cardService.getFeaturedCards();
    }

    @GetMapping("/whoami")
    public String whoAmI(@RequestHeader("User-Agent") String userAgent) {
        return "You are using: " + userAgent;
    }
}