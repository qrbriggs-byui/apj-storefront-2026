# APJ Storefront — Project Summary

## Overview

**apj-storefront** is a multi-module Spring Boot 3 (Java 21) application that implements a small storefront for “Fantasy CardWerx”—a fictional store that displays trading-style cards for computer science pioneers. The project demonstrates a **backend API** plus a **web front-end** that consumes that API via a proxy, with static HTML/CSS/JS and optional OpenAPI/Swagger documentation.

## Architecture

- **Parent POM** (`pom.xml`): Maven aggregator with modules `api` and `web`.
- **api**: REST service that loads card data from a CSV file and exposes JSON endpoints.
- **web**: Spring Boot web app that serves static pages and **proxies** card requests to the API using Spring WebClient (reactive HTTP client). The browser talks only to the web app; the web app calls the API.

```
Browser  →  Web (port 8080)  →  API (e.g. 8081)
                ↓                      ↓
         Static HTML/JS         data/pioneers.csv
         ProxyCardController    CardController
         WebCardService         CsvCardRepository
```

## Modules

### API (`api/`)

- **Purpose**: Backend service for “pioneer” cards (ID, name, specialty, contribution, price, image URL).
- **Data**: Cards are loaded at startup from `api/src/main/resources/data/pioneers.csv` (OpenCSV). In-memory, read-only repository.
- **Endpoints** (base path `/api/cards`):
  - `GET /api/cards/featured?q=...` — Featured cards; optional query `q` (defaults to “Java” when omitted).
  - `GET /api/cards` — All cards.
  - `GET /api/cards/{id}` — Single card by ID (404 if not found).
- **Tech**: Spring Boot Web, Actuator, Springdoc OpenAPI (Swagger UI), Lombok, OpenCSV.
- **Docs**: Swagger UI available when the API is running (Springdoc path typically `/docs` or similar per `springdoc` config).

### Web (`web/`)

- **Purpose**: Storefront UI and single backend-for-frontend that the browser uses.
- **Static assets**: `web/src/main/resources/static/` — `index.html`, `product-listing.html`, `product.html`, and JS/CSS (`index.js`, `product.js`, `product-listing.js`, `index.css`, `product.css`).
- **Proxy endpoints**: Same paths as API so the front end can call `/api/cards`, `/api/cards/featured`, `/api/cards/{id}` on the same origin. Implemented by `ProxyCardController` and `WebCardService` (WebClient → API).
- **Config**: `card.api.base-url` (default `http://localhost:8081`) in `application.properties`; WebClient bean in `WebClientConfig`.
- **Tech**: Spring Boot Web, WebFlux (WebClient), Actuator, Springdoc, Lombok.

## Data Model

**Card** (same logical shape in API and web, in respective `model` packages):

- `id` (Long), `name`, `specialty`, `contribution`, `price` (BigDecimal), `imageUrl`.

CSV columns: `ID`, `Name`, `Specialty`, `Contribution`, `Price`, `ImageUrl`.

## Front-End (Static)

- **Brand**: “Fantasy CardWerx” — “APJ Store — Curated trading cards & collectibles.”
- **Pages**:
  - **Home** (`index.html`): Featured cards; header search redirects to product listing with query.
  - **Browse** (`product-listing.html`): All/searchable cards and search form.
  - **Product detail** (`product.html`): Single card by ID (e.g. from URL query).
- **Behavior**: Fetch from `/api/cards/featured`, `/api/cards`, `/api/cards/{id}` (all go to web app, which proxies to API). Uses `fetch()` and renders cards in a grid; error and empty states handled.

## Running the Project

1. **Build**: From repo root, `mvn clean install` (or build `api` and `web` modules).
2. **API**: Run `api` module (e.g. `ApiApplication`). Configure `server.port=8081` if using default web config that points to `http://localhost:8081`.
3. **Web**: Run `web` module (e.g. `WebApplication`). Default port is 8080; `application-dev` can override (e.g. port 8081 and debug logging).
4. **Usage**: Open `http://localhost:8080` in a browser; ensure the API is reachable at the URL set by `card.api.base-url` so proxy requests succeed.

## Technologies (Summary)

| Area        | Choices                                      |
|------------|-----------------------------------------------|
| Runtime    | Java 21                                       |
| Framework  | Spring Boot 3.5.x                             |
| API docs   | Springdoc OpenAPI (Swagger UI)                |
| HTTP client (web → api) | Spring WebClient (reactive)          |
| Data       | In-memory CSV (OpenCSV), no database         |
| Front-end  | Vanilla HTML, CSS, JavaScript                 |

## Course / Educational Context

The codebase references “Article 6” and teaching notes (e.g. blocking WebClient in MVC, proxy pattern). It is well-suited for BYU-Idaho CSE 290R as a compact example of a multi-module Spring Boot app, REST API, CSV-backed repository, reactive client usage, and a static storefront that consumes the API through a proxy.
