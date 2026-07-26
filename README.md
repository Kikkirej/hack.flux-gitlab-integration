# Hack.flux Integration - GitLab

[Hack.flux](https://sr.ht/~chirpcel/hack.flux/) is a tool for organizing hackathons. To integrate in existing infrastructure it enables integrating by reacting to events. This integration is meant for integrating in GitLab.

Features:

* GitLab Group Creation
* GitLab Group Update
* GitLab Group Assignment
* GitLab Group Deassignment

## Events

Each feature above is implemented by one of the three flows below: *User added* covers Creation and Assignment, *Hackathon topic renamed* covers Update, and *User removed* covers Deassignment.

Needed information in events: 

* eventtype (`user-added`, `topic-renamed`, `user-removed-topic`)
* technical ID topic
* topic displayname
* affected users' gitlab username 
    * (should be part of the configurable fields which are sent in the event as well)
    * This of course only with the events which included users.

These are described conceptually above; the actual JSON key names are not yet fixed by a schema.

### User added to hackathon topic (`user-added`)

If the user does not have a gitlab_username defined, this gets directly skipped, as no GitLab interaction is expected.

1. The group for the topic is checked, whether it exists
    * technical name for the URL is using the technical ID as a subgroup of the configured `gitlab.parent-group-path` (i.e. `<parent-group-path>/<technical-id>`). While this is less readable, this makes it easier to identify. 
2. If it doesn't exist it gets created
3. gitlab_username from the event is added as an Owner to the topic specific group.
    * Doesn't need to check if the user is already in it, as it won't hurt to have the add function be called again.

> **Note (open question):** When the user gets edited with the GitLab username, it would be perfect if another event were triggered by hackflux for this. Until then, this could be handled by a database script run on a cron, so that all events get re-created periodically.

### Hackathon topic renamed (`topic-renamed`)

1. Checks if Hackathon Topic related group exists. 
2. If it exists the display name gets updated.
3. If it doesn't exist, this event is skipped.

### User removed from the Hackathon Topic (`user-removed-topic`)

**This doesn't delete the group, as it might delete contained source code**

If gitlab_username is not set this directly skips.

1. Identify the group from the event. If it doesn't exist, skip this event.
2. Remove gitlab_username's assignment from the group.
    * Doesn't need to check if the user is currently assigned first, as it won't hurt to call remove again.


## Configuration

| Setting | Description | Example | Default |
| ---- | ---- | ----| ---- |
| `spring.kafka.bootstrap-servers` | Endpoint(s) of the Kafka broker(s) to connect to | `localhost:9092` | `localhost:9092` |
| `spring.kafka.consumer.group-id` | Consumer group id used when reading from Kafka | `hackflux-gitlab-integration` | `hackflux-gitlab-integration` |
| `hackflux.kafka.topic` | Kafka topic to consume Hack.flux events from | `hackflux-events` | - |
| `gitlab.url` | Base URL of the GitLab instance to integrate with | `https://gitlab.com` | `https://gitlab.com` |
| `gitlab.access-token` | Access token used to authenticate against the GitLab API (needs rights to manage groups) | `glpat-xxxxxxxxxxxxxxxxxxxx` | - |
| `gitlab.parent-group-path` | Path of the parent GitLab group under which hackathon groups are created/assigned as subgroups, i.e. `<parent-group-path>/<technical-id>` | `hackathons` | - |


## Dev Start

````
./gradlew bootRun
````