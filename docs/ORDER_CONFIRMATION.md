# ORDER_CONFIRMATION.md

Plan for Implementing JMS-Based Order Confirmation Messaging

## Overview

This module builds on the previous plans and assumes the following are already implemented:

- cart functionality from `CART_PLAN.md`
- asynchronous order processing from `ASYNC_ORDER.md`
- multi-module storefront architecture from `APPLICATION.md`

In this module, students will learn how to use **JMS with Apache Artemis** in Spring Boot.

The goal is to publish an **order-complete event** after an order has truly finished processing, then consume that event in a separate module and generate a console-based order confirmation message.

This should model the kind of event-driven architecture commonly used for:

- email notifications
- SMS notifications
- downstream fulfillment workflows
- audit/event processing

For this exercise:

- the **web module** will act as the **JMS producer**
- the new **jms module** will act as the **JMS consumer**
- the **db module** remains the system of record for orders
- the order confirmation message must only be triggered **after the order is saved and marked complete in the db module**
- because Module 2 includes a **10 second async delay**, no JMS message should be sent until that async work has completed

High-level flow:

Browser  
↓  
web module  
↓  
db module creates and processes order asynchronously  
↓  
order reaches COMPLETED after 10 second delay  
↓  
web module detects COMPLETED status and publishes JMS message  
↓  
jms module consumes message  
↓  
jms module calls db module for full order details  
↓  
jms module logs order confirmation message to console

---

# 1. Learning Goals for This Module

This module should help students learn:

- what JMS is and why message brokers are used
- how Apache Artemis fits into a Spring Boot application
- how to configure a message producer
- how to configure a message consumer
- how to define a queue destination
- how to send a lightweight event message instead of the full object graph
- how to keep the database module as the source of truth
- how event-driven communication works alongside earlier polling-based workflows

This module should complement Module 2 rather than replace it.

Students should still be able to see the browser polling for order completion, but now a second side effect happens when completion is detected: an order confirmation event is published.

---

# 2. New Module to Add

Add a new Maven module:

`jms`

Purpose:

Consumes order confirmation events from Apache Artemis and generates a console-based confirmation message.

Suggested responsibilities of the new module:

- connect to Artemis broker
- listen on an order confirmation queue
- receive an order completion message containing an order id
- call the db module via WebClient or RestClient
- retrieve order details
- format a confirmation message
- log the confirmation message to the console

Suggested Spring Boot entry point:

`JmsApplication`

Recommended package areas:

- `config`
- `listener`
- `client`
- `dto`
- `service`

---

# 3. Architectural Decision

Use a **lightweight JMS event** rather than placing the full order payload on the queue.

Recommended JMS message contents:

- `orderId`
- optional `eventType` such as `ORDER_COMPLETED`
- optional `completedAt`

Example payload shape:

```json
{
  "eventType": "ORDER_COMPLETED",
  "orderId": 123,
  "completedAt": "2026-03-14T12:00:00"
}
```

Why this is the better teaching choice:

- keeps the message simple
- avoids duplicating the full order snapshot in multiple places
- reinforces that the db module remains the source of truth
- lets the consumer demonstrate service-to-service retrieval of fresh data
- keeps the broker payload small and easy for students to inspect

---

# 4. Order Completion Trigger Strategy

The most important rule for this module is:

**Do not publish the order confirmation JMS message until the order is actually complete in the db module.**

Because Module 2 includes asynchronous processing with a 10 second wait, the producer must not publish at checkout submission time.

## Recommended Trigger Design

Have the **web module publish the JMS message only after it observes that the order status is `COMPLETED`.**

This fits the requirement that the web module contains the JMS producer.

### Practical implementation approach

The browser already polls the web module for order status.

Current polling flow from Module 2:

Browser  
→ `GET /order-status/{orderId}` on web module  
→ web module calls db module  
→ db module returns status

Extend this flow so that when the web module sees the status transition to `COMPLETED`, it publishes a JMS event once.

## Important guard against duplicate messages

Because polling may continue briefly and the endpoint may be called multiple times, the web module needs a way to avoid publishing duplicate confirmation events.

Recommended approaches for teaching:

### Option A — simplest teaching approach
Add a boolean field in the order record such as:

- `confirmationMessageSent`

When the web module sees `COMPLETED`, it can call a db endpoint that atomically marks the order as confirmation-sent before publishing, or publish only if that flag was previously false.

### Option B — acceptable classroom simplification
If the module is meant only as a first JMS exercise, note in the plan that duplicate sends are possible in a naive polling-based producer and that production systems must make event publication idempotent.

### Recommended choice
Use **Option A** because it teaches an important real-world concept:

- publish once
- avoid duplicate messages
- keep state changes explicit

---

# 5. DB Module Changes

The db module remains responsible for order persistence and order retrieval.

It should also provide the data needed by the JMS consumer.

## 5.1 Extend the Order Model

Add the following field to the `Order` entity:

- `confirmationMessageSent` (boolean, default false)

Purpose:

Tracks whether the order completion notification event has already been published.

This allows the web module to safely determine whether it should send the JMS message.

## 5.2 Existing Order Status Support

Module 2 already introduced statuses such as:

- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `FAILED`

No new order status is required for this module.

The event should be triggered only when the order status is `COMPLETED`.

## 5.3 Order Retrieval Endpoint

The JMS consumer needs to fetch full order information after receiving the message.

Reuse or expand the existing order endpoint:

`GET /api/orders/{orderId}`

If the current endpoint only returns minimal status, add either:

### Option 1
Expand the existing endpoint so it can return complete order details.

### Option 2
Add a new detailed endpoint:

`GET /api/orders/{orderId}/details`

Recommended returned fields:

- orderId
- createdAt
- status
- totalAmount
- cartId
- items list
  - productId
  - productName
  - quantity
  - price

For teaching clarity, Option 2 is often easier because it separates:

- status polling DTOs
- full detail DTOs

## 5.4 Confirmation Sent Endpoint

Add an endpoint that supports publish-once behavior.

Recommended endpoint:

`POST /api/orders/{orderId}/confirmation-sent`

Behavior:

- if order is not `COMPLETED`, reject the request
- if `confirmationMessageSent` is already true, return a response indicating no new publish should occur
- if false, set it to true and return success

Example response:

```json
{
  "orderId": 123,
  "status": "COMPLETED",
  "confirmationMessageSent": true,
  "publishAllowed": true
}
```

If already marked:

```json
{
  "orderId": 123,
  "status": "COMPLETED",
  "confirmationMessageSent": true,
  "publishAllowed": false
}
```

This gives the web module a safe handshake before sending the JMS event.

## 5.5 DB Logging

The db module should log useful order retrieval and confirmation-state changes.

Suggested logs:

- order completed
- order details requested for confirmation message
- order confirmation flag updated

Example:

- `Order 123 marked COMPLETED`
- `Order 123 details requested by JMS confirmation workflow`
- `Order 123 confirmationMessageSent set to true`

---

# 6. WEB Module Changes

The web module will now gain **JMS producer** responsibilities while continuing to proxy HTTP requests to the db module.

## 6.1 Add Artemis JMS Dependencies

Add the Spring Boot JMS starter appropriate for Artemis.

Typical dependency direction:

- Spring JMS support
- Artemis client support

The exact artifact names can be chosen during implementation, but the module should be configured to connect to Apache Artemis using Spring Boot’s Artemis properties.

## 6.2 Producer Configuration

Add JMS-related configuration in the web module.

Suggested configuration class responsibilities:

- configure destination name constant
- configure message conversion if sending JSON
- expose `JmsTemplate` usage through a service

Create a service such as:

`OrderConfirmationProducer`

Responsibilities:

- send an order completed event to the configured queue
- serialize a simple DTO payload
- log that a message was sent

Suggested method:

`sendOrderCompleted(orderId)`

or

`sendOrderCompleted(OrderCompletedMessage message)`

## 6.3 DB Proxy Support for Publish-Once Check

The web module likely already has `OrderClientService` from Module 2.

Extend it with methods such as:

- `getOrderStatus(orderId)`
- `markConfirmationSent(orderId)`

Optional:

- `getOrderDetails(orderId)` if needed elsewhere

## 6.4 Trigger Point in the Web Module

The producer should be invoked from the web module when order completion is confirmed.

The cleanest place is likely inside the existing order-status proxy flow.

Suggested flow when `/order-status/{orderId}` is called:

1. call db module for order status
2. if status is not `COMPLETED`, return status normally
3. if status is `COMPLETED`, call db module endpoint to mark confirmation as sent
4. if response says `publishAllowed = true`, send JMS message
5. return status response to browser

This preserves the Module 2 browser workflow while introducing Module 4 messaging behind the scenes.

## 6.5 Logging in WEB Module

Suggested logs:

- `Order 123 status checked: PROCESSING`
- `Order 123 is COMPLETED; attempting to publish confirmation event`
- `Order 123 confirmation event published to queue order.confirmation`
- `Order 123 confirmation event already published; skipping`

---

# 7. JMS Module Changes

The new `jms` module will host the consumer.

## 7.1 Module Purpose

The jms module represents a downstream notification service.

It listens for completed-order events and reacts by building a human-readable confirmation message.

## 7.2 Artemis Consumer Configuration

Add Artemis JMS configuration to the jms module.

Responsibilities:

- broker connection configuration
- listener container enablement
- destination name configuration
- message converter configuration if JSON messages are used

Enable JMS listener support with:

- `@EnableJms`

## 7.3 JMS Listener

Create a component such as:

`OrderConfirmationListener`

Annotation example conceptually:

- `@JmsListener(destination = "order.confirmation")`

Responsibilities:

- receive the order completed message
- extract the order id
- call the db module to retrieve order details
- generate a confirmation message string
- log the final confirmation to the console

Suggested listener method flow:

1. receive message
2. validate payload
3. read `orderId`
4. call db module
5. build message
6. log output

## 7.4 DB Client in JMS Module

Because the consumer must reach out to the db module, add a client service using **WebClient** or **RestClient**.

Suggested service:

`OrderDetailsClient`

Method:

`getOrderDetails(orderId)`

This reinforces the architecture:

- queue message contains only the event
- full order data comes from the source-of-truth service

## 7.5 Confirmation Message Service

Create a service such as:

`OrderConfirmationMessageService`

Responsibilities:

- take order details DTO
- create a readable confirmation message
- isolate formatting logic from the listener itself

Example output:

```text
Order Confirmation
Order ID: 123
Status: COMPLETED
Total: $49.97
Items:
- Blue Eyes White Dragon x1 - $19.99
- Dark Magician x1 - $29.98
Thank you for your order!
```

This should be logged to the console.

## 7.6 Logging in JMS Module

Suggested logs:

- `Received JMS message for completed order 123`
- `Fetching order details from db module for order 123`
- `Generated confirmation message for order 123`

Then print the confirmation body.

---

# 8. Apache Artemis Configuration

Students will be installing Apache Artemis locally, so both the **web** and **jms** modules need broker configuration.

## 8.1 Properties to Add

Add Artemis-related settings to both modules’ configuration files.

Suggested properties:

- broker URL
- username
- password
- queue name
- package trust settings if object/JSON conversion is used

Recommended custom app properties:

```properties
app.jms.order-confirmation-queue=order.confirmation
```

Recommended Spring Artemis properties pattern:

```properties
spring.artemis.mode=native
spring.artemis.broker-url=tcp://localhost:61616
spring.artemis.user=admin
spring.artemis.password=admin
```

If JSON conversion or object messages are used, additional message-converter or trusted-package configuration may be needed.

## 8.2 Keep Configuration Consistent Across Modules

Both `web` and `jms` modules should use the same queue name:

`order.confirmation`

The db module does not need Artemis configuration for this plan because it does not produce or consume JMS messages.

## 8.3 Configuration for DB Service Calls from JMS Module

The jms module also needs the db base URL in its application properties.

Example:

```properties
db.service.base-url=http://localhost:8082
```

---

# 9. DTOs and Message Contracts

## 9.1 JMS Event DTO

Create a simple message DTO shared conceptually between producer and consumer.

Suggested fields:

- `orderId`
- `eventType`
- `completedAt`

This can be duplicated in each module for simplicity in teaching, or extracted later into a shared library if desired.

For classroom clarity, duplicating the small DTO in each module is acceptable at first.

## 9.2 Order Details DTO

The jms module needs a DTO that matches the db module’s detailed order response.

Suggested fields:

- `orderId`
- `status`
- `totalAmount`
- `createdAt`
- `items`

Each item:

- `productName`
- `quantity`
- `price`

## 9.3 Keep the Message Small

Do not place the full order details into the JMS event for this module.

That would weaken the lesson about:

- event payload design
- source of truth
- downstream retrieval of data

---

# 10. Full End-to-End Request Flow

## Step-by-step flow

1. user clicks **Place Order** in the browser
2. browser posts to the web module
3. web module calls db module to create the order
4. db module saves order with status `PENDING`
5. db module starts async processing
6. async method sleeps for 10 seconds
7. db module marks order `COMPLETED`
8. browser continues polling `/order-status/{orderId}` on the web module
9. web module checks db module and receives `COMPLETED`
10. web module calls db module to mark `confirmationMessageSent`
11. if publish is allowed, web module sends JMS message to Artemis queue `order.confirmation`
12. jms module consumes the message from the queue
13. jms module calls db module for order details
14. jms module formats the order confirmation message
15. jms module logs the confirmation message to the console

---

# 11. Recommended Classes by Module

## DB module

Suggested additions:

- `Order` entity update
- `OrderDetailsDTO`
- `OrderConfirmationFlagResponseDTO`
- `OrderController` additions
- `OrderService` additions

Suggested methods:

- `getOrderDetails(orderId)`
- `markConfirmationSent(orderId)`

## WEB module

Suggested additions:

- `OrderCompletedMessage`
- `OrderConfirmationProducer`
- `JmsConfig`
- `OrderClientService` enhancements
- `CheckoutController` or status controller enhancement

Suggested methods:

- `sendOrderCompleted(...)`
- `markConfirmationSent(orderId)`
- `getOrderStatus(orderId)`

## JMS module

Suggested additions:

- `JmsApplication`
- `OrderCompletedMessage`
- `JmsConfig`
- `OrderConfirmationListener`
- `OrderDetailsClient`
- `OrderConfirmationMessageService`
- DTOs for order details

---

# 12. Teaching Demonstration Ideas

This module provides a good opportunity to show students the difference between:

- synchronous HTTP request/response
- asynchronous background processing with `@Async`
- asynchronous event-driven messaging with JMS

Suggested classroom demonstrations:

## Demo 1 — show order processing still takes 10 seconds
Students place an order and watch status remain `PROCESSING` during polling.

## Demo 2 — show message only appears after completion
Students watch that no confirmation log is generated until the db module marks the order complete.

## Demo 3 — stop the JMS module temporarily
Show that the order can still complete in the db module even if the consumer is not running yet. Once the consumer starts, the broker can deliver the queued message.

## Demo 4 — explain producer vs consumer responsibilities
- web module produces event
- jms module consumes event
- db module provides source data

## Demo 5 — discuss why duplicate events matter
Explain why polling plus event production can cause duplicates if publish-once logic is not added.

---

# 13. Implementation Steps

Recommended order of implementation:

1. add new `jms` module to the parent Maven build
2. add Artemis dependencies to `web` and `jms`
3. configure Artemis properties in both modules
4. enable JMS listener support in the `jms` module
5. create JMS event DTO
6. add `confirmationMessageSent` field to the `Order` entity in the db module
7. add db endpoint for detailed order retrieval
8. add db endpoint for marking confirmation as sent
9. enhance `OrderClientService` in the web module for the new db calls
10. create `OrderConfirmationProducer` in the web module
11. update the order-status flow in the web module to publish only after completion
12. create `OrderDetailsClient` in the jms module
13. create `OrderConfirmationListener` in the jms module
14. create `OrderConfirmationMessageService` in the jms module
15. run Artemis locally and verify queue connectivity
16. test end-to-end order flow
17. verify exactly one confirmation message is logged per completed order

---

# 14. Testing Plan

## Unit tests

### DB module
Test:

- order detail DTO mapping
- confirmation flag update logic
- rejection when trying to mark confirmation sent for non-completed orders

### WEB module
Test:

- producer service sends message to correct destination
- completed orders trigger publish attempt
- already-published orders do not publish again

### JMS module
Test:

- listener receives and parses message
- client fetches order details
- confirmation message formatter produces expected output

## Integration tests

Recommended scenarios:

1. create order and confirm no JMS event is sent immediately
2. wait for async completion and verify publish occurs
3. verify consumer receives message and logs confirmation
4. verify repeated polling after completion does not create duplicate confirmation messages
5. verify consumer can fetch order details from db module

## Manual verification checklist

- Artemis broker is running
- web module connects successfully
- jms module connects successfully
- queue is created or resolved correctly
- order completes after 10 seconds
- exactly one confirmation message appears in console

---

# 15. Future Enhancements

Possible next steps after this exercise:

- send email instead of console output
- send SMS instead of console output
- add dead-letter queue handling
- add retry behavior for failed consumers
- add message correlation ids
- extract shared DTOs into a common module
- publish the event directly from the db module in a later advanced lesson
- use transactional outbox pattern for more robust delivery guarantees

These can be discussed as “what production systems do next.”

---

# 16. Summary

This module introduces **JMS messaging with Apache Artemis** into the storefront application while preserving the architecture established in earlier modules.

Responsibilities remain clearly separated:

- **db module**: owns order data and exposes order APIs
- **web module**: remains the user-facing gateway and now publishes JMS messages when orders are confirmed complete
- **jms module**: consumes completion events, fetches order details, and generates a console-based confirmation message

Most importantly, the design respects the Module 2 async workflow:

- the order is not confirmed immediately
- the db module completes the order after the 10 second async wait
- only then does the web module publish the JMS event
- the jms module reacts and logs the confirmation

This gives students a clean progression across modules:

- Module 1: cart workflow
- Module 2: async order processing
- Module 3: scheduled jobs and cleanup
- Next Module: event-driven confirmation messaging with JMS

