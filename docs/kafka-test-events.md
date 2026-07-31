# Kafka test events

Commands to publish the event payloads documented in the [README](../README.md#event-examples) to the local Kafka broker started by `docker compose up`, so the flows described there can be triggered without writing a producer.

Topic name matches `hackflux.kafka.topic` (`hackflux-events` is used below — adjust if you configured a different topic).

## User added to topic

Corresponds to [User added to hackathon topic](../README.md#user-added-to-hackathon-topic-user-added). JSON below is corrected to be valid (the README snippet is missing a colon after `hckflx_user`).

```
echo '{"hckflx_eventtype":"user_added","hckflx_topic":{"uuid":"d0306161-e912-43e7-883f-6dbbaada8fb8","friendly_name":"Pizza"},"hckflx_user":{"uuid":"not relevant","oidcref":"pizza@example.com","friendly_name":"test","custom_fields":{"gitlab_user":"user1"}}}' \
  | docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server localhost:9092 --topic hackflux-events
```

## User removed from topic

Corresponds to [User removed from the Hackathon Topic](../README.md#user-removed-from-the-hackathon-topic-user-removed-topic).

```
echo '{"hckflx_eventtype":"user_removed","hckflx_topic":{"uuid":"d0306161-e912-43e7-883f-6dbbaada8fb8","friendly_name":"Pizza"},"hckflx_user":{"uuid":"not relevant","oidcref":"pizza@example.com","friendly_name":"test","custom_fields":{"gitlab_user":"user2","department":"cat sitting"}}}' \
  | docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server localhost:9092 --topic hackflux-events
```

`topic-renamed` is called out in the README as out of scope for this version, so no example is included for it.

## Verify messages landed on the topic

```
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic hackflux-events --from-beginning
```
