# ASYNC_ORDER.md

Plan for Implementing Asynchronous Checkout and Order Processing

## Overview

This plan adds **checkout and order confirmation functionality** to the
storefront system.

Key goals:

-   Create orders in the **db module**
-   Provide **checkout and order confirmation pages**
-   Simulate a **slow payment gateway**
-   Demonstrate **Spring's @Async feature**
-   Allow the UI to **poll for order completion status**
-   Maintain architecture where the **UI only talks to the web module**
-   The **web module proxies requests to the db module via WebClient**

High-level flow:

Browser \| v web module \| v db module (order processing)

Order processing will be **asynchronous** to simulate real-world payment
processing delays.

------------------------------------------------------------------------

# 1. Order Data Model (DB Module)

Add entities to represent orders.

## Order

Fields:

-   id
-   cartId
-   createdAt
-   status (PENDING, PROCESSING, COMPLETED, FAILED)
-   totalAmount

Relationships:

Order 1 -\> many OrderItems

## OrderItem

Fields:

-   id
-   productId
-   productName
-   price
-   quantity

These records represent a snapshot of the cart contents at checkout.

------------------------------------------------------------------------

# 2. Order Repository

Create Spring Data repositories.

OrderRepository - save(order) - findById(id)

OrderItemRepository - saveAll(items)

------------------------------------------------------------------------

# 3. DB Module REST API

Expose endpoints to support order creation and status checks.

Controller: `OrderController`

Endpoints:

POST /api/orders

Purpose: Create a new order from the cart.

Steps:

1.  Retrieve cart
2.  Create Order entity
3.  Copy cart items into OrderItems
4.  Set status = PENDING
5.  Save order
6.  Start async processing
7.  Return orderId immediately

Response:

{ "orderId": 123, "status": "PENDING" }

------------------------------------------------------------------------

GET /api/orders/{orderId}

Purpose: Retrieve order status.

Response:

{ "orderId": 123, "status": "PROCESSING" }

Possible statuses:

-   PENDING
-   PROCESSING
-   COMPLETED
-   FAILED

------------------------------------------------------------------------

# 4. Asynchronous Order Processing

Use Spring's **@Async** feature to simulate payment processing.

## Configuration

Enable async processing in db module:

@EnableAsync

------------------------------------------------------------------------

## OrderService

Method:

processOrderAsync(orderId)

Annotation:

@Async

Flow:

1.  Load order
2.  Set status = PROCESSING
3.  Save
4.  Simulate slow payment gateway

Thread.sleep(10000)

5.  Set status = COMPLETED
6.  Save order

This demonstrates background processing using a separate thread.

------------------------------------------------------------------------

# 5. Web Module Proxy Layer

The UI will never call the db module directly.

Create:

OrderClientService

Responsibilities:

-   call db module order endpoints
-   translate responses to UI DTOs

Methods:

createOrder(cartId)

getOrderStatus(orderId)

Implementation uses **WebClient**.

Example endpoint mapping:

web module -\> db module

POST /checkout -\> POST /api/orders

GET /order-status/{orderId} -\> GET /api/orders/{orderId}

------------------------------------------------------------------------

# 6. Web Module Controllers

Controller: `CheckoutController`

Endpoints:

GET /checkout

Display checkout page.

POST /checkout

Creates order.

Steps:

1.  Retrieve cartId from session
2.  Call OrderClientService.createOrder()
3.  Receive orderId
4.  Redirect to:

/order-confirmation/{orderId}

------------------------------------------------------------------------

GET /order-confirmation/{orderId}

Displays confirmation page.

This page shows:

-   order id
-   order status
-   loading indicator

------------------------------------------------------------------------

GET /order-status/{orderId}

Proxy endpoint used by frontend polling.

Calls db module:

GET /api/orders/{orderId}

Returns status JSON.

------------------------------------------------------------------------

# 7. Checkout Page

Route:

GET /checkout

Page content:

Title: Checkout

Display:

-   cart summary
-   total cost

Button:

"Place Order"

Form submits to:

POST /checkout

------------------------------------------------------------------------

# 8. Order Confirmation Page

Route:

GET /order-confirmation/{orderId}

Initial content:

Order Confirmation

Order ID: \####

Status: Processing...

------------------------------------------------------------------------

## Polling Logic

Use JavaScript polling.

Example:

Every 2 seconds:

GET /order-status/{orderId}

Response:

{ "status": "PROCESSING" }

If status == COMPLETED:

Update page to:

"Order Complete"

Stop polling.

------------------------------------------------------------------------

# 9. Simulating a Slow Payment Gateway

Inside OrderService:

Thread.sleep(10000)

This simulates:

-   payment processing
-   fraud checks
-   external gateway delays

Because the method runs with **@Async**, the HTTP request returns
immediately.

Students will observe:

Without @Async → request blocks 10 seconds

With @Async → request returns immediately while background work
continues

------------------------------------------------------------------------

# 10. Full Request Flow

User clicks "Place Order"

Browser \| POST /checkout \| web module \| POST /api/orders \| db module

Order created (PENDING)

db module launches async processor

Background thread: \| sleep(10 seconds) \| mark order COMPLETED

Meanwhile:

Browser redirected to confirmation page

Browser polls:

GET /order-status/{orderId}

Once COMPLETED → page updates.

------------------------------------------------------------------------

# 11. Implementation Steps

Recommended order:

1.  Create Order and OrderItem entities
2.  Create repositories
3.  Implement OrderService
4.  Enable @Async
5.  Implement async order processor
6.  Add OrderController endpoints
7.  Implement WebClient OrderClientService in web module
8.  Add CheckoutController in web module
9.  Create checkout page
10. Create order confirmation page
11. Add polling JavaScript
12. Test full async flow

------------------------------------------------------------------------

# 12. Teaching Demonstration

This design helps demonstrate:

Spring @Async

Background task execution

Non-blocking user experience

Polling for asynchronous completion

Separation of services

Microservice communication via WebClient

Students will clearly see how long-running tasks should be handled
outside the main request thread.

------------------------------------------------------------------------

# Summary

This plan introduces:

-   Checkout page
-   Order creation
-   Asynchronous payment simulation
-   Order confirmation page
-   Status polling

Architecture remains consistent:

UI → web module → db module

Async processing occurs **only inside the db module**, demonstrating
proper backend task handling.
