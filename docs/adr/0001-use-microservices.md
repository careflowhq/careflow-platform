# ADR 0001 - Use Pragmatic Microservices Architecture

## Status
Accepted

## Context

CareFlow AI is being designed as a scalable multi-tenant healthcare SaaS platform.

The platform will contain:
- authentication
- patient management
- follow-up workflows
- notifications
- future AI services

These domains have different scaling and operational concerns.

## Decision

Use a pragmatic microservices architecture with:
- Spring Boot services
- REST communication
- RabbitMQ for async notifications
- database per service

## Consequences

### Positive
- service isolation
- scalability
- domain separation
- future extensibility

### Negative
- increased operational complexity
- distributed debugging
- more deployment overhead

## Notes

Avoid overengineering:
- no Kubernetes initially
- no Kafka initially
- no service mesh