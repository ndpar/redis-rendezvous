## Overview

This project demonstrates how to implement poor man's message broker
using Spring Redis.

A Rendezvous example shows how two clients can swap their messages
by sending them to the same "queue" on the server. First client sends
its message to the head of the queue, second to the tail.

## Prerequisites

Run Redis locally with default settings

    redis-server

## Run Application

Choose the level of details you want to see in the log file

    mvn clean spring-boot:run
    mvn clean spring-boot:run -Dspring-boot.run.arguments=--logging.level.com.ndpar=TRACE
    mvn clean spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.ndpar=TRACE --logging.level.org.springframework.data.redis=TRACE"

## Test Rendezvous

Run the following commands in separate terminal windows

    curl -X POST http://localhost:8080/alice \
        -H 'Content-Type: application/json' \
        -d '{"id": "123","message":"Hello from Alice"}'

    curl -X POST http://localhost:8080/bob \
        -H 'Content-Type: application/json' \
        -d '{"id": "123","message":"Hello from Bob"}'

- Try to change sequence of those messages.
- Try to send only one message and wait for 20 seconds.
- Compare timestamps of the responses.
- Observe thread names and URLs in the log file.
