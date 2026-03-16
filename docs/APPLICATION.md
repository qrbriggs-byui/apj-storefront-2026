# APPLICATION.md
Project: apj-storefront

This document summarizes the current implementation of the Spring Boot multi-module project.

## High Level Architecture

This project is a **multi‑module Maven Spring Boot application** built with **Java 21** and **Spring Boot 3.5.x**.

Parent module:
- parent (packaging: pom)

Modules:
- web
- api
- db
- api-mongo
- demo (example sandbox module)

The design appears to follow a **microservice-style architecture** where multiple services handle different parts of the application.

## Module Overview

### 1. web Module
Purpose:
Acts as the **web gateway / client layer**. It proxies requests to backend services.

Key Components:
- WebApplication – Spring Boot entry point.
- ProxyCardController – forwards card requests to the API service.
- ProxyTradingCardController – forwards trading card requests to the Mongo API service.
- WebCardService – service layer for card operations.
- MongoTradingCardClientService – client service used to call the Mongo trading card API.
- WebClientConfig – configuration for Spring WebClient.
- DTOs / Models
  - Card
  - TradingCardDTO

Responsibilities:
- Receives HTTP requests from users
- Calls downstream services using WebClient
- Returns aggregated responses to clients

### 2. api Module
Purpose:
Provides the **primary REST API for card data**.

Key Components:
- ApiApplication – Spring Boot entry point.
- CardController – REST controller exposing card endpoints.
- CardService – business logic for card operations.
- CsvCardRepository – repository that loads card data from a CSV file.
- Card – domain model representing a card.

Responsibilities:
- Provide REST endpoints for card retrieval
- Load card data from CSV source
- Serve card data to other services (like the web module)

### 3. db Module
Purpose:
Implements the **database-backed ecommerce functionality**.

Key Components:

Entities / Models:
- Cart
- Category
- Item
- Product
- User
- UserProfile

Repositories (Spring Data):
- CartRepository
- CategoryRepository
- ItemRepository
- ProductRepository
- UserRepository
- UserProfileRepository

Services:
- CartService

Controllers:
- CartController

Other:
- DataSeeder – initializes sample data at startup.

Responsibilities:
- Persist and manage store data
- Manage carts and product relationships
- Provide database-backed APIs

### 4. api-mongo Module
Purpose:
Provides a **MongoDB-based API for trading cards**.

Key Components:
- ApiMongoApplication – Spring Boot entry point.
- TradingCardController – REST controller for trading cards.
- TradingCardService – business logic layer.
- TradingCardRepository – Mongo repository interface.
- TradingCard – Mongo document model.

Responsibilities:
- Persist and retrieve trading cards using MongoDB
- Expose trading card REST endpoints

### 5. demo Module
Purpose:
Appears to be a **sample or teaching module** demonstrating basic Spring Data usage.

Components:
- ProductController
- ProductRepository
- Entities such as Product, Order, Payment, PriceChange

Responsibilities:
- Demonstration of CRUD patterns
- Not clearly integrated with the main storefront system

## Cross Module Interaction

Typical request flow:

Client
   |
   v
web module
   |
   |----> api module (card data from CSV)
   |
   |----> api-mongo module (trading card data)
   |
   |----> db module (cart, users, products)

The web module acts as an **aggregator / proxy gateway** to backend services.

## Technologies Used

Core:
- Java 21
- Spring Boot 3.5.x
- Maven multi-module build

Spring Components:
- Spring Web / REST Controllers
- Spring Data JPA
- Spring Data MongoDB
- WebClient (for service-to-service communication)

Persistence:
- Relational database (JPA entities in db module)
- MongoDB (api-mongo module)

Testing:
- Spring Boot Test framework

## What Has Been Implemented So Far

The following capabilities currently exist:

✔ Multi-module Maven architecture  
✔ Separate microservice-like modules  
✔ REST APIs for cards and trading cards  
✔ CSV-backed card repository  
✔ MongoDB-backed trading card repository  
✔ Database entities for an ecommerce store  
✔ Shopping cart service and controller  
✔ Web proxy service using WebClient  
✔ Data seeding for initial data  

The foundation for a **distributed storefront system** is in place.

## What Appears Not Yet Implemented

Based on the current codebase, the following features appear incomplete or missing:

- Authentication / authorization
- Order checkout workflow
- Payment processing integration
- Inventory management
- Front-end UI (only backend services exist)
- API gateway or service discovery
- Event-driven messaging between services

These may be planned future features.

## Summary

The repository currently contains the early stages of a **Spring Boot distributed storefront system**.

Architecture includes:
- Gateway web service
- REST card API
- Mongo trading card API
- Relational database service
- Example demo module

The project already demonstrates many enterprise patterns including:
- multi-module builds
- service layering
- repository pattern
- service-to-service communication
- polyglot persistence (SQL + Mongo).
