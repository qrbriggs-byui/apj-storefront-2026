# CART_PLAN.md

Plan for Adding Cart Functionality to the Spring Boot Storefront

## Overview

This document outlines a plan to implement a **shopping cart feature**
using the existing **db module** for persistence and the **web module**
as the frontend gateway using **WebClient proxies**.

The goal is to support:

-   Add product to cart from the product detail page
-   View cart on a dedicated cart page
-   Persist cart information in the **db module**
-   Retrieve cart information through **WebClient proxy calls**
-   Provide a checkout button that navigates to a **placeholder checkout
    page**

Architecture pattern:

Client (browser) \| v web module \| v db module (cart persistence)

The web module will call the db module via **WebClient**.

------------------------------------------------------------------------

# 1. Data Model (db Module)

The db module already contains a `Cart` entity and `CartRepository`. If
not fully implemented, extend them as follows.

## Entities

Cart - id - userId (optional for now) - createdAt -
List`<CartItem>`{=html}

CartItem - id - productId - productName - quantity - price

Relationships

Cart 1 -\> many CartItems

------------------------------------------------------------------------

# 2. DB Module REST API

Expose REST endpoints in the **db module** to support cart operations.

Controller: `CartController`

Endpoints:

GET /api/cart/{cartId} - Retrieve the current cart

POST /api/cart - Create a new cart

POST /api/cart/{cartId}/items - Add item to cart

PUT /api/cart/{cartId}/items/{itemId} - Update quantity

DELETE /api/cart/{cartId}/items/{itemId} - Remove item

DELETE /api/cart/{cartId} - Clear cart

Response DTOs:

CartDTO CartItemDTO

These DTOs should prevent leaking internal JPA entities.

------------------------------------------------------------------------

# 3. Web Module Proxy Layer

The **web module** will not talk directly to the database. Instead it
will call the db module using **WebClient**.

Create:

CartClientService

Responsibilities:

-   call db service endpoints
-   convert responses to web DTOs

Methods:

getCart(cartId)

createCart()

addItem(cartId, productId, quantity)

updateItem(cartId, itemId, quantity)

removeItem(cartId, itemId)

clearCart(cartId)

Configuration:

Add a base URL for the db service in `application.yml`

Example:

db.service.base-url: http://localhost:8082

------------------------------------------------------------------------

# 4. Web Module Controllers

Create a controller responsible for cart views.

Controller: `CartController`

Routes:

GET /cart - Render the cart page

POST /cart/add - Add product to cart

POST /cart/remove - Remove item

POST /cart/update - Update quantity

GET /checkout - Navigate to checkout page

The controller will call `CartClientService` which communicates with the
db module.

------------------------------------------------------------------------

# 5. Product Detail Page Changes

Modify the **product detail page** to include an **Add to Cart button**.

Button behavior:

POST /cart/add

Parameters:

productId quantity

Flow:

User clicks Add to Cart \| v web controller \| v CartClientService \| v
db module API \| v cart updated

------------------------------------------------------------------------

# 6. Cart Page

Create a dedicated cart page.

Route: GET /cart

Page features:

Display:

-   product name
-   price
-   quantity
-   subtotal
-   cart total

Actions:

-   update quantity
-   remove item
-   checkout button

Layout:

## Cart Items Table

## Product \| Price \| Qty \| Total

Checkout Button

------------------------------------------------------------------------

# 7. Checkout Page (Placeholder)

Create a page for:

GET /checkout

Content:

Title: Checkout

Text:

"Checkout functionality has not been implemented yet."

This page will eventually support:

-   shipping information
-   payment
-   order confirmation

------------------------------------------------------------------------

# 8. Session or Cart Identification

Decide how carts are identified.

Simple implementation:

Store cartId in HTTP session.

Flow:

User visits site \| Create cart if none exists \| Store cartId in
session

This avoids needing authentication initially.

------------------------------------------------------------------------

# 9. Implementation Steps

Recommended order:

1.  Implement db module cart API
2.  Test endpoints using Postman
3.  Implement CartClientService in web module
4.  Implement web CartController
5.  Add Add-To-Cart button to product page
6.  Build cart page
7.  Build checkout placeholder page
8.  Test full workflow

------------------------------------------------------------------------

# 10. Future Enhancements

Later improvements could include:

User Accounts - associate carts with users

Persistent carts - restore carts when users log in

Inventory validation

Order service integration

Payment processing

Cart expiration logic

------------------------------------------------------------------------

# Summary

This plan introduces a cart feature with a clear separation of
responsibilities:

web module - UI - controllers - WebClient proxy

db module - cart persistence - REST API

The implementation allows the system to evolve toward a **true
microservice storefront architecture**.
