# Hack.flux Integration - GitLab

[Hack.flux](https://sr.ht/~chirpcel/hack.flux/) is a tool for organizing hackathons. To integrate in existing infrastructure it enables integrating by reacting to events. This integration is meant for integrating in GitLab.

Features:

* GitLab Group Creation
* GitLab Group Assignment
* GitLab Group Deassignment

## Events

### User added to hackathn topic

The information 

## Configuration

| Setting | Description | Example | Default |
| ---- | ---- | ----| ---- |
| `spring.kafka.bootstrap-servers` | Endpoint(s) of the Kafka broker(s) to connect to | `localhost:9092` | `localhost:9092` |
| `hackflux.kafka.topic` | Name of the Kafka topic that hackathon events are consumed from | `hackflux.hackathon-events` | - |
| `spring.kafka.consumer.group-id` | Consumer group id used when reading from Kafka | `hackflux-gitlab-integration` | `hackflux-gitlab-integration` |
| `gitlab.url` | Base URL of the GitLab instance to integrate with | `https://gitlab.com` | `https://gitlab.com` |
| `gitlab.access-token` | Access token used to authenticate against the GitLab API (needs rights to manage groups) | `glpat-xxxxxxxxxxxxxxxxxxxx` | - |
| `gitlab.parent-group-path` | Path of the parent GitLab group under which hackathon groups are created/assigned | `hackathons` | - |


## Dev Start

````
./gradlew bootRun
````