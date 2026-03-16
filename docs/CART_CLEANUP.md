# CART_CLEANUP.md

Plan for Implementing Scheduled Cart Cleanup in the Spring Boot Storefront

## Overview

This plan adds a new **Module 3** feature focused on **scheduled jobs in Spring Boot**.

The goal is to introduce a new module named **`work`** that runs a scheduled cart cleanup job. The cleanup job will call REST endpoints in the **db module** to find and remove carts that are older than a configurable number of minutes. That age threshold will be stored in `application.properties`.

At the end of each run, the cleanup process should generate a simple report showing how many carts were removed. In addition, the **db module** should log each individual cart that gets deleted.

This design keeps the same architecture introduced in earlier modules:

Browser / UI -> web module -> db module for user-facing flows

For this background workflow:

work module -> db module

This module is intended to teach:

- Spring scheduled jobs using `@Scheduled`
- externalized configuration with `application.properties`
- service-to-service communication between Spring Boot modules
- separation between background workers and persistence APIs
- simple reporting and operational logging

---

# 1. Module Goal

Module 3 should demonstrate that not all application behavior begins with a browser request.

Unlike Module 1 (cart interactions) and Module 2 (async checkout), this module introduces a **background worker** that runs automatically on a schedule.

Responsibilities should be split as follows:

## work Module

- owns the scheduled cleanup job
- reads cleanup configuration from `application.properties`
- calls db module cleanup endpoints
- prints or logs a report at the end of the run

## db Module

- determines which carts are expired
- removes expired carts from the database
- logs each cart as it is deleted
- returns a cleanup summary to the work module

---

# 2. Why a Separate `work` Module

Create a new module called **`work`** rather than placing the scheduled job inside the db module.

Reasons:

- It reinforces the multi-module architecture already used in the storefront.
- It keeps persistence concerns inside the db module.
- It teaches that scheduled jobs can live in a dedicated worker service.
- It mirrors real systems where background jobs are separated from user-facing services.

Add the new module to the parent Maven build:

- `work`

The new module should be a Spring Boot application with its own entry point, for example:

- `WorkApplication`

---

# 3. Cleanup Configuration

The cleanup age threshold should live in `application.properties`.

Recommended properties in the **work module**:

```properties
cart.cleanup.max-age-minutes=30
cart.cleanup.fixed-delay-ms=60000
```

Meaning:

- `cart.cleanup.max-age-minutes` = carts older than this many minutes should be deleted
- `cart.cleanup.fixed-delay-ms` = how often the scheduled job should run

You may also add the db service base URL if it is not already shared through configuration:

```properties
db.service.base-url=http://localhost:8082
```

This gives students a good example of externalized configuration instead of hardcoding values.

---

# 4. High-Level Flow

Each scheduled run should follow this sequence:

1. The `work` module scheduler starts automatically.
2. The cleanup job reads `cart.cleanup.max-age-minutes`.
3. The cleanup job calls a db module endpoint.
4. The db module finds carts older than the configured age.
5. The db module deletes those carts.
6. The db module logs each cart that is removed.
7. The db module returns a summary response.
8. The `work` module generates a final report for the run.

Example flow:

Scheduler fires -> work module -> db module cleanup API -> expired carts deleted -> summary returned -> report logged

---

# 5. DB Module Changes

The db module already owns cart persistence, so all expiration logic should be implemented there.

## 5.1 Cart Entity Expectations

The existing cart design already includes a `createdAt` field from Module 1. That field will now become important for cleanup.

Ensure the `Cart` entity contains:

- `id`
- `createdAt`
- cart items / relationships already used by the cart feature

If `createdAt` is not automatically set yet, add logic so it is populated when a cart is created.

---

## 5.2 Repository Updates

Extend `CartRepository` with methods needed for cleanup.

Recommended queries:

- find carts with `createdAt` before a cutoff timestamp
- optionally delete all carts returned by that query

Possible repository methods:

```java
List<Cart> findByCreatedAtBefore(LocalDateTime cutoff);
```

You may choose either:

- load carts first, log each one, then delete them one by one, or
- load carts, log them, then delete in batch

For teaching clarity, deleting after loading them individually is often easier to explain because students can clearly see each deleted cart in the logs.

---

## 5.3 Service Layer

Add cleanup logic to the db module service layer.

Recommended service:

- `CartCleanupService`

Suggested method:

```java
CartCleanupResponse cleanupExpiredCarts(int maxAgeMinutes)
```

Flow inside the service:

1. Compute cutoff time:
   - `now minus maxAgeMinutes`
2. Query for expired carts.
3. For each expired cart:
   - log cart id
   - optionally log createdAt
   - delete cart
4. Count how many carts were removed.
5. Return a summary response.

Example logging in db module:

```java
log.info("Removing expired cart id={} createdAt={}", cart.getId(), cart.getCreatedAt());
```

This satisfies the requirement that the db module logs each cart that is removed.

---

## 5.4 Response DTO

Return a summary object from the db module rather than a plain number.

Example DTO:

```java
public class CartCleanupResponse {
    private int removedCount;
    private int maxAgeMinutes;
    private LocalDateTime cutoffTime;
    private LocalDateTime runAt;
}
```

This gives the work module enough information to produce a nice end-of-run report.

---

## 5.5 DB Module REST API

Expose a cleanup endpoint in the db module.

Controller:

- `CartCleanupController`

Recommended endpoint:

```http
POST /api/internal/cart-cleanup
```

Request options:

### Option A: Query Parameter

```http
POST /api/internal/cart-cleanup?maxAgeMinutes=30
```

### Option B: Request Body

```json
{ "maxAgeMinutes": 30 }
```

For teaching simplicity, a query parameter is easiest.

Response example:

```json
{
  "removedCount": 4,
  "maxAgeMinutes": 30,
  "cutoffTime": "2026-03-14T11:30:00",
  "runAt": "2026-03-14T12:00:00"
}
```

Why use an internal endpoint:

- this is an operational action, not a browser-facing feature
- it makes it clear the work module is acting like a backend worker

---

# 6. Work Module Implementation

The new `work` module should own the scheduled job and the reporting behavior.

## 6.1 Spring Boot Setup

Create:

- `WorkApplication`

Enable scheduling:

```java
@EnableScheduling
```

This module can be started independently from the other services.

---

## 6.2 Configuration Properties

Bind cleanup properties into a typed configuration class.

Example:

```java
@ConfigurationProperties(prefix = "cart.cleanup")
public class CartCleanupProperties {
    private int maxAgeMinutes;
    private long fixedDelayMs;
}
```

This helps teach a clean Spring Boot configuration pattern.

---

## 6.3 DB Client Service

Create a client inside the `work` module for calling the db module.

Recommended class:

- `CartCleanupClientService`

Responsibilities:

- call the db cleanup endpoint
- pass the configured max age
- deserialize the cleanup summary response

Suggested method:

```java
CartCleanupResponse triggerCleanup(int maxAgeMinutes)
```

Use **WebClient** to stay consistent with the architecture used in the earlier modules.

---

## 6.4 Scheduled Job

Create the actual scheduled component.

Recommended class:

- `CartCleanupJob`

Suggested method:

```java
@Scheduled(fixedDelayString = "${cart.cleanup.fixed-delay-ms}")
public void runCartCleanup()
```

Flow:

1. Read `maxAgeMinutes` from configuration.
2. Log that the job is starting.
3. Call `CartCleanupClientService.triggerCleanup(maxAgeMinutes)`.
4. Receive `CartCleanupResponse`.
5. Generate the final report.
6. Log completion.

Example start log:

```java
log.info("Starting cart cleanup job with maxAgeMinutes={}", properties.getMaxAgeMinutes());
```

---

## 6.5 Report Generation

The requirement says a report should be generated at the end of the run detailing how many carts were removed.

For Module 3, the simplest report is an application log summary.

Example report output:

```text
Cart cleanup run complete.
Run time: 2026-03-14T12:00:00
Expiration threshold: 30 minutes
Removed carts: 4
Cutoff used: 2026-03-14T11:30:00
```

You could place this in a dedicated helper such as:

- `CartCleanupReportService`

But for teaching simplicity, it can also be logged directly in the scheduled job.

If you want one extra teaching step, you could also write the report to a text file later, but logging is enough for the initial implementation plan.

---

# 7. Suggested Class Layout

## work Module

- `WorkApplication`
- `config/CartCleanupProperties`
- `config/WorkWebClientConfig`
- `client/CartCleanupClientService`
- `job/CartCleanupJob`
- `dto/CartCleanupResponse`

## db Module

- `controller/CartCleanupController`
- `service/CartCleanupService`
- `dto/CartCleanupResponse`
- `repository/CartRepository` updates

If desired, the response DTO can be duplicated in both modules, or extracted to a shared module later. For teaching, duplicating the DTO is acceptable and often simpler.

---

# 8. Endpoint Design Recommendation

Recommended endpoint in db module:

```http
POST /api/internal/cart-cleanup?maxAgeMinutes=30
```

Why POST instead of DELETE:

- the operation is a cleanup process, not deletion of one known resource
- it is action-oriented and may delete multiple records
- it returns a process summary

This makes the API intention clear for students.

---

# 9. Error Handling

Add a small amount of resilience so the scheduled job is safe to demo.

## In the work module

Wrap the remote call in `try/catch`.

If the db module is unavailable:

- log an error
- do not crash the application
- allow the next scheduled run to try again

Example:

```java
try {
    CartCleanupResponse response = client.triggerCleanup(properties.getMaxAgeMinutes());
    // generate report
} catch (Exception ex) {
    log.error("Cart cleanup job failed", ex);
}
```

## In the db module

Validate the incoming parameter:

- reject `maxAgeMinutes <= 0`
- return a `400 Bad Request` if invalid

This teaches students that background jobs still need normal API validation.

---

# 10. Logging Expectations

There are two different logging responsibilities.

## DB Module Logs

The db module should log each removed cart.

Example:

```text
Removing expired cart id=17 createdAt=2026-03-14T10:55:00
Removing expired cart id=21 createdAt=2026-03-14T11:02:00
```

## Work Module Logs

The work module should log the final run summary.

Example:

```text
Cart cleanup completed. maxAgeMinutes=30, removedCount=2, cutoffTime=2026-03-14T11:30:00
```

This separation is useful pedagogically:

- db module logs the record-by-record work
- work module logs the job-level report

---

# 11. Implementation Steps

Recommended order:

1. Add the new `work` module to the parent project.
2. Create `WorkApplication` and enable scheduling.
3. Add cleanup properties to `application.properties`.
4. Ensure `Cart.createdAt` is populated correctly in the db module.
5. Extend `CartRepository` with a query for expired carts.
6. Implement `CartCleanupService` in the db module.
7. Add the db cleanup response DTO.
8. Add `CartCleanupController` endpoint in the db module.
9. Test the db cleanup endpoint manually with Postman or curl.
10. Create `CartCleanupClientService` in the work module.
11. Create `CartCleanupJob` with `@Scheduled`.
12. Add end-of-run report logging.
13. Start db + work modules together and verify scheduled cleanup.
14. Create test carts with older timestamps and confirm removal.
15. Verify each deleted cart appears in db logs and the summary appears in work logs.

---

# 12. Demo Scenario for Teaching

A good classroom demonstration would be:

1. Start the db module.
2. Start the work module.
3. Insert several carts into the database:
   - some recent
   - some older than the threshold
4. Set:

```properties
cart.cleanup.max-age-minutes=30
cart.cleanup.fixed-delay-ms=15000
```

5. Wait for the job to run.
6. Show students:
   - the scheduler firing automatically
   - the work module calling the db module
   - each expired cart being logged and deleted in the db module
   - the final report showing how many carts were removed

This makes the concept of scheduled jobs concrete and visible.

---

# 13. Testing Ideas

## Unit Tests

### db module

- cleanup finds only carts older than the cutoff
- cleanup deletes the right carts
- cleanup returns the correct removed count

### work module

- scheduled job calls the client with configured `maxAgeMinutes`
- report logging uses returned response data
- exceptions are handled without crashing the app

## Integration Tests

- create carts with old and recent timestamps
- invoke cleanup endpoint
- verify only old carts are removed
- verify response count matches deleted rows

---

# 14. Concepts This Module Teaches

Module 3 should help students understand:

- `@Scheduled` for recurring jobs
- `@EnableScheduling`
- configuration from `application.properties`
- worker-service design in a multi-module system
- backend-to-backend REST communication
- operational logging vs business logging
- time-based cleanup logic

It also builds naturally on the first two modules:

- Module 1 taught cart persistence and WebClient-based service boundaries
- Module 2 taught asynchronous backend processing
- Module 3 now teaches recurring automated background work

---

# 15. Summary

This module introduces a dedicated **`work`** module that runs a scheduled cart cleanup job.

The job reads a configurable cart age threshold from `application.properties`, calls the **db module** to remove expired carts, and generates a summary report at the end of the run.

Responsibilities remain cleanly separated:

- **work module** = scheduling, configuration, reporting
- **db module** = finding expired carts, deleting them, logging each removed cart

This is a strong teaching example because it shows students how Spring Boot handles recurring background work in a service-oriented architecture.
