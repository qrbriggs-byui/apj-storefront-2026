package edu.byui.apj.storefront.web.service;

import edu.byui.apj.storefront.web.model.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Calls the API module using WebClient.
 *
 * Teaching notes:
 * - We use a reactive chain (bodyToFlux) then .collectList().block() to integrate with MVC controllers.
 * - Article 6 explains that blocking removes some scalability advantages; in a fully reactive web app you'd return Flux/Mono.
 */
@Service
public class WebCardService {

    private static final Logger logger = LoggerFactory.getLogger(WebCardService.class);

    private final WebClient client;

    public WebCardService(WebClient cardApiClient) {
        this.client = cardApiClient;
    }

    public List<Card> getFeatured(String q) {
        String uri = (q == null || q.isBlank()) ? "/api/cards/featured" : "/api/cards/featured?q=" + q;
        logger.debug("WebClient GET {}", uri);

        Flux<Card> flux = client.get()
                .uri(uri)
                .retrieve()
                .bodyToFlux(Card.class)
                .timeout(Duration.ofSeconds(5)); // avoid hanging forever

        // NOTE: .block() here for MVC compatibility — tradeoff discussed in class
        return flux.collectList().block();
    }

    public List<Card> search(String q) {
        String uri = (q == null || q.isBlank()) ? "/api/cards" : "/api/cards?q=" + q;
        logger.debug("WebClient GET {}", uri);

        Flux<Card> flux = client.get()
                .uri(uri)
                .retrieve()
                .bodyToFlux(Card.class)
                .timeout(Duration.ofSeconds(5));

        return flux.collectList().block();
    }

    public Optional<Card> getCardById(Long id) {
        logger.debug("WebClient GET by id {}", id);
        Mono<Card> cardMono = client.get()
                .uri("/api/cards/" + id)
                .retrieve()
                .bodyToMono(Card.class);
        return cardMono.blockOptional();
    }
}