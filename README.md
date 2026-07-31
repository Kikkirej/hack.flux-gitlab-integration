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

The gitlab_username is resolved from the event as follows:

* By default (`hackflux.gitlab-username-from-oidc-ref: true`), it's derived from the local part of `hckflx_user.oidcref` (the part before `@`), since `oidcref` is a mail address, e.g. `pizza@example.com` resolves to `pizza`.
* If `hackflux.gitlab-username-from-oidc-ref` is set to `false`, it's read from `custom_fields.gitlab_user` instead.

If the user does not have a gitlab_username defined (or it couldn't be resolved), this gets directly skipped, as no GitLab interaction is expected.

1. It's checked whether the resolved gitlab_username actually corresponds to an existing GitLab user. If it doesn't, this gets skipped with a warning log.
2. The group for the topic is checked, whether it exists
    * technical name for the URL is using the technical ID as a subgroup of the configured `gitlab.parent-group-path` (i.e. `<parent-group-path>/<technical-id>`). While this is less readable, this makes it easier to identify. 
3. If it doesn't exist it gets created
4. gitlab_username from the event is added as an Owner to the topic specific group.
    * Doesn't need to check if the user is already in it, as it won't hurt to have the add function be called again.

> **Note (open question):** When the user gets edited with the GitLab username, it would be perfect if another event were triggered by hackflux for this. Until then, this could be handled by a database script run on a cron, so that all events get re-created periodically.

### Hackathon topic renamed (`topic-renamed`)

1. Checks if Hackathon Topic related group exists. 
2. If it exists the display name gets updated.
3. If it doesn't exist, this event is skipped.

### User removed from the Hackathon Topic (`user-removed-topic`)

**This doesn't delete the group, as it might delete contained source code**

The gitlab_username is resolved the same way as for `user-added` (see above). If it is not set/couldn't be resolved, this directly skips.

1. It's checked whether the resolved gitlab_username actually corresponds to an existing GitLab user. If it doesn't, this gets skipped with a warning log.
2. Identify the group from the event. If it doesn't exist, skip this event.
3. Remove gitlab_username's assignment from the group.
    * Doesn't need to check if the user is currently assigned first, as it won't hurt to call remove again.


## Configuration

| Setting | Description | Example | Default |
| ---- | ---- | ----| ---- |
| `spring.kafka.bootstrap-servers` | Endpoint(s) of the Kafka broker(s) to connect to | `localhost:9092` | `localhost:9092` |
| `spring.kafka.consumer.group-id` | Consumer group id used when reading from Kafka | `hackflux-gitlab-integration` | `hackflux-gitlab-integration` |
| `hackflux.kafka.topic` | Kafka topic to consume Hack.flux events from | `hackflux-events` | - |
| `hackflux.gitlab-username-from-oidc-ref` | When `true`, the GitLab username is derived from the local part (before `@`) of `hckflx_user.oidcref` instead of `custom_fields.gitlab_user`. Useful for Hack.flux setups without a dedicated `gitlab_user` custom field. Set to `false` to use `custom_fields.gitlab_user` instead. | `false` | `true` |
| `gitlab.url` | Base URL of the GitLab instance to integrate with | `https://gitlab.com` | `https://gitlab.com` |
| `gitlab.access-token` | Access token used to authenticate against the GitLab API (needs rights to manage groups) | `glpat-xxxxxxxxxxxxxxxxxxxx` | - |
| `gitlab.parent-group-path` | Path of the parent GitLab group under which hackathon groups are created/assigned as subgroups, i.e. `<parent-group-path>/<technical-id>` | `hackathons` | - |

## Dev Start

````
./gradlew bootRun
````

## Local Testing

`docker compose up` starts only Kafka, which is enough to run `./gradlew bootRun` day-to-day.

To test against a real GitLab instance locally:

1. `docker compose --profile gitlab up` also starts a local GitLab CE at `http://localhost:8080`. First boot takes several minutes — watch `docker compose logs -f gitlab` until it's ready.
2. Once GitLab is up, follow [docs/gitlab-init.md](docs/gitlab-init.md) to get the root password, mint an access token, and create the parent group + test users. Then set the resulting `gitlab.url`, `gitlab.access-token`, and `gitlab.parent-group-path` for `bootRun`.
3. With the app running and consuming from Kafka, follow [docs/kafka-test-events.md](docs/kafka-test-events.md) to publish `user_added` / `user_removed` events and watch the app react (group/user changes are visible in the local GitLab UI).

## Event Examples

### User added to topic

This example has a `gitlab_user` custom field set; it's only used when `hackflux.gitlab-username-from-oidc-ref` is set to `false`. By default, the GitLab username is derived from `oidcref` instead (`pizza@example.com` -> `pizza`), ignoring `gitlab_user`.

````
{
    "hckflx_eventtype": "user_added",
    "hckflx_topic": 
    {
        "uuid": "d0306161-e912-43e7-883f-6dbbaada8fb8",
        "friendly_name": "Pizza"
    },
    "hckflx_user"{
        "uuid": "not relevant",
        "oidcref": "pizza@example.com",
        "friendly_name": "test",
        "custom_fields":
            {
                "gitlab_user": "klause"
            }
    }
}
````

### User added to topic (no `gitlab_user` custom field)

Many Hack.flux setups don't expose a `gitlab_user` custom field at all. With the default `hackflux.gitlab-username-from-oidc-ref: true`, the GitLab username is still resolved, from the local part of `oidcref` (here, `pizza@example.com` -> `pizza`):

````
{
    "hckflx_eventtype": "user_added",
    "hckflx_topic": 
    {
        "uuid": "d0306161-e912-43e7-883f-6dbbaada8fb8",
        "friendly_name": "Pizza"
    },
    "hckflx_user"{
        "uuid": "not relevant",
        "oidcref": "pizza@example.com",
        "friendly_name": "test",
        "custom_fields":
            {
                "department": "cat sitting"
            }
    }
}
````

### Topic Changed

NOTE: Out of scope in this version, to reduce complexity.

### User removed

````
{
    "hckflx_eventtype": "user_removed",
    "hckflx_topic": 
    {
        "uuid": "d0306161-e912-43e7-883f-6dbbaada8fb8",
        "friendly_name": "Pizza"
    },
    "hckflx_user"{
        "uuid": "not relevant",
        "oidcref": "pizza@example.com",
        "friendly_name": "test",
        "custom_fields":
            {
                "gitlab_user": "klaus",
                "department": "cat sitting"
            }
    }
}
````

## TODO

* add name cutting for too long names
