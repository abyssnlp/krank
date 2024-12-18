# Krank

Queue based auto-scaling

## Overview

Krank is a queue-based auto-scaling solution designed to dynamically adjust the number of replicas for your deployments
based on the metrics collected from various message brokers. Currently, it supports Kafka, with plans to support
RabbitMQ and SQS in the future.

## Features

- Auto-scaling based on queue metrics
- Supports Kafka (RabbitMQ and SQS support coming soon)
- Configurable scaling strategies
- Metrics collection and monitoring
- In-memory state management (with support for persistent state coming soon)

## Getting Started

### Prerequisites

- Java 11
- Kotlin 1.9.20 or higher
- Gradle 8.5 or higher
- Docker (for building and running the Docker image)
- Helm (for deploying using Helm charts)

### Building the Project

1. Clone the repository:
    ```sh
    git clone https://github.com/abyssnlp/krank.git
    cd krank
    ```

2. Build the project using Gradle:
    ```sh
    ./gradlew build
    ```

### Running Tests

To run the tests, use the following command:

```sh
./gradlew test
```

### Running the Application




