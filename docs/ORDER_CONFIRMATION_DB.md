# ORDER_CONFIRMATION_DB.md

Plan for Implementing JMS-Based Order Confirmation Messaging (**DB-Triggered Producer**)

This document is an alternate version of `ORDER_CONFIRMATION.md`. Compare the two and implement whichever fits your course better.

---

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

**This variant (DB-triggered):**

- the **db module** will act as the **JMS producer** (publish when async processing finishes)
- the new **jms module** will act as the **JMS consumer**
- the **web module** stays an HTTP gateway only—it does **not** connect to Artemis or publish messages
- the order confirmation message must only be triggered **after the order is saved and marked complete in the db module**
- because Module 2 includes a **10 second async delay**, no JMS message is sent until that async work completes—publication happens **in the same service layer** that transitions the order to `COMPLETED`

High-level flow:

Browser  
↓  
web module  
↓  
db module creates and processes order asynchronously  
↓  
order reaches COMPLETED after 10 second delay **inside the db module**  
↓  
**db module publishes JMS message immediately after marking COMPLETED**  
↓  
jms module consumes message  
↓  
jms module calls db module for full order details  
↓  
jms module logs order confirmation message to console  

**Contrast with `ORDER_CONFIRMATION.md`:** In the original plan, the web module polls status and publishes when it first sees `COMPLETED`. Here, publication is **tied to completion in the db module**, so no browser poll is required for messaging (though polling can still drive the UI).

---

# 1. Learning Goals for This Module

This module should help students learn:

- what JMS is and why message brokers are used
- how Apache Artemis fits into a Spring Boot application
- how to configure a message producer **in a service that owns the business event**
- how to configure a message consumer in a separate process
- how to define a queue destination
- how to send a lightweight event message instead of the full object graph
- how to keep the database module as the source of truth
- how event-driven communication differs from polling-based side effects

This module should complement Module 2 rather than replace it.

Students should still be able to see the browser polling for order completion; the JMS confirmation is triggered **when the db module completes processing**, not when the browser happens to poll.

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

*(Same as `ORDER_CONFIRMATION.md`.)*

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
- reinforces that the db module remains the source of truth for order data
- lets the consumer demonstrate service-to-service retrieval of fresh data
- keeps the broker payload small and easy for students to inspect

---

# 4. Order Completion Trigger Strategy

The most important rule for this module is:

**Do not publish the order confirmation JMS message until the order is actually complete in the db module.**

Because Module 2 includes asynchronous processing with a 10 second wait, the producer must not publish at checkout submission time.

## Recommended Trigger Design (DB Module)

Have the **db module publish the JMS message in the same execution path that marks the order `COMPLETED`**—e.g. at the end of the `@Async` (or equivalent) order-processing method, **after** persistence shows status `COMPLETED`.

### Why this differs from the web-triggered plan

- **Single source of truth for “completion happened”:** The db module is the only place that knows the async work finished.
- **No coupling to polling:** Confirmation messaging does not depend on a client calling `/order-status` again.
- **Natural “exactly once” per completion:** If the completion logic runs once per order lifecycle, the publish runs once. (See duplicate discussion below.)

### Practical implementation approach

After Module 2’s async step (e.g. sleep) completes:

1. Update the order entity to `COMPLETED` and persist (flush/commit as appropriate).
2. Call a small producer service (e.g. `OrderConfirmationProducer.sendOrderCompleted(orderId)`) **from that same flow**.

Order of operations to consider for teaching:

- **Simplest:** Persist `COMPLETED`, then send JMS. If JMS send fails after commit, you can discuss at-least-once delivery and retries (good classroom topic).
- **Advanced (optional):** Introduce transactional outbox later—listed under future enhancements.

## Duplicate messages

Unlike the web plan, you do **not** get duplicates from repeated status polls. Duplicates could still occur if:

- the completion path were invoked more than once for the same order (bug or retry), or
- the broker redelivers and the consumer logs again (consumer idempotency is a separate topic).

For teaching, **one clear completion method** that runs once per order is usually enough. Optional hardening:

- Set a boolean `confirmationMessageSent` (or `jmsEventPublished`) on the order **in the same transaction** as `COMPLETED`, and only call `JmsTemplate` if transitioning from not-sent to sent; or
- Document that production systems often use outbox + idempotent consumers.

**Recommended choice for classroom:** Start with **publish once at end of completion method**. Add the flag only if you want to mirror the idempotency lesson from the web plan in the db layer.

---

# 5. DB Module Changes

The db module remains the system of record and **now also hosts the JMS producer**.

## 5.1 Order Model (Optional Field)

If you use publish-once idempotency in the db layer, add:

- `confirmationMessageSent` or `orderCompletedEventPublished` (boolean, default false)

If you rely on a single completion invocation only, this field is optional.

## 5.2 Existing Order Status Support

Module 2 already introduced statuses such as:

- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `FAILED`

No new order status is required. Publish only when status becomes `COMPLETED` (and not on `FAILED` unless you design a different event).

## 5.3 Order Retrieval Endpoint

The JMS consumer still needs to fetch full order information after receiving the message.

Reuse or expand:

`GET /api/orders/{orderId}`

Or add:

`GET /api/orders/{orderId}/details`

Recommended returned fields (same as original plan):

- orderId, createdAt, status, totalAmount, cartId
- items: productId, productName, quantity, price

## 5.4 Confirmation-Sent Endpoint (Optional in This Variant)

In `ORDER_CONFIRMATION.md`, `POST /api/orders/{orderId}/confirmation-sent` supports the web module’s publish-once handshake.

**In the DB-triggered variant, that endpoint is usually unnecessary** unless you want the db to expose “event published” state for debugging or admin tools. You may omit it to simplify the lab.

## 5.5 Artemis / JMS in the DB Module

Add Spring Boot JMS + Artemis client dependencies to the **db** module.

Configure:

- broker URL, user, password
- queue name (e.g. `order.confirmation`)
- `JmsTemplate` and a producer service such as `OrderConfirmationProducer` with `sendOrderCompleted(orderId)` (or DTO-based send)

Invoke that producer from the order-completion service **after** the order is persisted as `COMPLETED`.

## 5.6 DB Logging

Suggested logs:

- order completed
- JMS order-completed event published (with order id)
- order details requested by JMS consumer (when jms calls back)

Example:

- `Order 123 marked COMPLETED`
- `Order 123 ORDER_COMPLETED event sent to queue order.confirmation`
- `Order 123 details requested by JMS confirmation workflow`

---

# 6. WEB Module Changes

The **web module does not produce JMS messages** in this plan.

## 6.1 No Artemis Dependencies on Web

Keep the web module free of JMS/Artemis unless you have another use case. It continues to:

- proxy checkout and order-status requests to the db module
- support browser polling for UI feedback

## 6.2 Order Status Flow

`/order-status/{orderId}` (or equivalent) remains:

1. web calls db for status
2. return status to browser

**Do not** add JMS publishing here—that happens in the db module when processing completes.

## 6.3 Logging in WEB Module

Unchanged from Module 2 style, e.g.:

- `Order 123 status checked: PROCESSING`

No requirement for “attempting to publish confirmation event” logs on web.

---

# 7. JMS Module Changes

Same consumer story as `ORDER_CONFIRMATION.md`.

## 7.1 Module Purpose

Downstream notification service: listen for completed-order events, fetch details from db, log confirmation.

## 7.2 Artemis Consumer Configuration

Broker connection, `@EnableJms`, listener container, JSON converter if used.

## 7.3 JMS Listener

`OrderConfirmationListener` on `order.confirmation`: receive message → validate → `orderId` → call db → format → log.

## 7.4 DB Client in JMS Module

`OrderDetailsClient.getOrderDetails(orderId)` via WebClient/RestClient.

## 7.5 Confirmation Message Service

`OrderConfirmationMessageService`—same example console output as the original plan.

## 7.6 Logging in JMS Module

Same as original plan.

---

# 8. Apache Artemis Configuration

## 8.1 Which Modules Need Broker Access

| Module | Role |
|--------|------|
| **db** | JMS **producer** |
| **jms** | JMS **consumer** |
| **web** | No Artemis configuration |

## 8.2 Properties to Add

**db module** and **jms module**:

```properties
app.jms.order-confirmation-queue=order.confirmation
```

```properties
spring.artemis.mode=native
spring.artemis.broker-url=tcp://localhost:61616
spring.artemis.user=admin
spring.artemis.password=admin
```

**jms module** also needs:

```properties
db.service.base-url=http://localhost:8082
```

(Adjust port to match your db service.)

## 8.3 Keep Queue Name Consistent

Both **db** and **jms** use the same destination: `order.confirmation`.

---

# 9. DTOs and Message Contracts

Same as `ORDER_CONFIRMATION.md`:

- Small shared-style DTO: `orderId`, `eventType`, `completedAt` (may be duplicated across db + jms modules for teaching)
- Order details DTO in jms module matching db API response
- Do not put full order on the queue

---

# 10. Full End-to-End Request Flow

## Step-by-step flow

1. User clicks **Place Order** in the browser
2. Browser posts to the web module
3. Web module calls db module to create the order
4. Db module saves order with status `PENDING` and starts async processing
5. Async method waits (e.g. 10 seconds)
6. Db module marks order `COMPLETED` and persists
7. **Db module sends JMS message** to `order.confirmation`
8. (In parallel) Browser may poll web → db for status and eventually see `COMPLETED`
9. Jms module consumes the message
10. Jms module calls db module for order details
11. Jms module formats and logs the confirmation message

**Note:** Step 7 does not depend on step 8—the message can be on the queue before or after the user’s next poll.

---

# 11. Recommended Classes by Module

## DB module

Suggested additions:

- Artemis/JMS dependencies and properties
- `JmsConfig` (template, destination, converter if needed)
- `OrderCompletedMessage` (DTO for queue payload)
- `OrderConfirmationProducer`
- `Order` entity update **if** using a published flag
- `OrderDetailsDTO` / detail endpoint for the consumer
- **Order service / async completion method:** after `COMPLETED`, call `OrderConfirmationProducer`

Suggested methods:

- `sendOrderCompleted(orderId)` (or message DTO)
- `getOrderDetails(orderId)` (HTTP API for jms module)

## WEB module

- **No** `OrderConfirmationProducer`, **no** JmsConfig for this feature
- Existing order proxy / status endpoints only

## JMS module

Same as `ORDER_CONFIRMATION.md`:

- `JmsApplication`, `OrderCompletedMessage`, `JmsConfig`, `OrderConfirmationListener`, `OrderDetailsClient`, `OrderConfirmationMessageService`, order-details DTOs

---

# 12. Teaching Demonstration Ideas

## Demo 1 — order processing still takes 10 seconds

Same as original.

## Demo 2 — message when db completes, not when user refreshes

Show that the console log can appear **as soon as** the db finishes the async step—even if no one polls the browser yet (optional: delay opening the status page).

## Demo 3 — stop the JMS module temporarily

Order still completes in db; messages queue in Artemis. Start jms later and show delivery.

## Demo 4 — producer vs consumer

- **db module** produces the event at completion
- **jms module** consumes and formats output
- **web module** is not part of the messaging path

## Demo 5 — why publish from the owning service?

Contrast with web-triggered plan: completion and notification originate in the same bounded context (order processing), which matches many production “domain event” styles.

---

# 13. Implementation Steps

Recommended order:

1. Add `jms` module to the parent Maven build
2. Add Artemis JMS dependencies to **db** and **jms** (not web)
3. Configure Artemis in **db** and **jms** `application.properties`
4. In db module: configure `JmsTemplate` and `OrderConfirmationProducer`
5. In db async completion flow: after setting `COMPLETED`, call producer
6. Enable `@EnableJms` and listener in **jms** module
7. Create JMS event DTO in db + jms (duplicate OK)
8. Add or extend `GET` order details endpoint for jms consumer
9. Implement `OrderDetailsClient` and `OrderConfirmationListener` in jms
10. Implement `OrderConfirmationMessageService` in jms
11. Run Artemis; run db, web, jms; place order; verify one console confirmation after ~10 seconds
12. (Optional) Add idempotency flag on order if teaching duplicate prevention in db

---

# 14. Testing Plan

## Unit tests

### DB module

- After completion logic, producer is invoked (mock `JmsTemplate`)
- Order detail DTO mapping
- If using flag: only one publish when completion runs twice incorrectly (edge case)

### WEB module

- No JMS tests required for this feature

### JMS module

- Same as original: listener, client, formatter

## Integration tests

1. Create order; assert no JMS message before async completion (or use test listener)
2. After completion delay, assert message appears on queue / consumer runs
3. Repeated status polls from web must not affect number of messages (still one per order)
4. Consumer fetches details from db

## Manual checklist

- Artemis running
- **db** connects as producer
- **jms** connects as consumer
- One confirmation log per completed order
- Confirmation can appear without hitting order-status again (optional check)

---

# 15. Future Enhancements

- Email/SMS instead of console
- Dead-letter queues, retries, correlation ids
- Shared DTO module
- **Transactional outbox** in db module so “saved COMPLETED” and “message must be sent” are reliable together
- Web-triggered publish (`ORDER_CONFIRMATION.md`) as alternate lab for comparing architectures

---

# 16. Summary

This variant introduces **JMS with Apache Artemis** with the **db module as the producer** when async order processing finishes.

| Responsibility | Module |
|----------------|--------|
| Order data, async completion, **publish ORDER_COMPLETED** | **db** |
| HTTP/UI, polling | **web** |
| Consume event, fetch details, console confirmation | **jms** |

Compared to `ORDER_CONFIRMATION.md`:

| Topic | ORDER_CONFIRMATION.md | ORDER_CONFIRMATION_DB.md (this doc) |
|-------|------------------------|-------------------------------------|
| JMS producer | web | db |
| Trigger | First poll sees COMPLETED | End of db async completion |
| Web + Artemis | Yes | No |
| DB + Artemis | No | Yes |
| `confirmation-sent` HTTP handshake | Recommended | Usually omitted |
| Duplicate risk from polling | Mitigate with flag + endpoint | Not applicable to producer |

Both plans satisfy: **no JMS until the order is truly complete after the async delay**; lightweight queue payload; jms consumer pulls full details from db.

---

*End of ORDER_CONFIRMATION_DB.md*
