package org.example.handlers;

import java.time.Duration;
import java.util.Objects;

import org.example.metrics.EventType;
import org.example.metrics.MetricsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;

import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.eventbus.EventBus;

import lombok.extern.slf4j.Slf4j;

/**
 * Tries to remove the duplicate events that are already processed.
 *
 * Theoretically, this is done by using the "unique id representation" of the incoming message.
 * These unique Ids could be stored "somewhere" so that it can be retrieved quickly.
 * If the same event comes again, and if its already present in the "storage", then the event can be marked duplicate.
 *
 * How do to check for unique id of the event?
 * In a more robust event processing service, the message protocol will have a contract and will be well defined.
 * Most of the time, message protocol will describe a field to attach a unique ID for that message. This is for the
 * server to know, what the client thinks as the unique ID. Now the event processing service could use that id for
 * validating, if it's a duplicate event. If we are processing chunk by chunk, we could have hashed that chunk and then
 * validate against previous hashes saved. But here we have to process, event by event and the event itself is a
 * simple 9 digit number. We could use the hash of its {@link String} representation, or run a hashing algorithm to
 * find the hash. As you know, the default {@link String#hashCode()} could give same value for two different strings.
 * For simplicity, we will use the String value itself as a "unique Id" of the event.
 *
 * Now in reality, when events are being streamed, it is not feasible to hold all the unique IDs in some distributed
 * storage. Even if we have high performant IO, in-memory, distributed storage, that will keep growing and it will be a
 * nightmare to handle that storage itself.
 *
 * The possibility of the same event sent more than once is, when there is some sort of disconnect between the publisher
 * of the message and the broker of that message. The sender might think the message was not delivered because of some
 * network disruption, but the message broker might have received that message. The sender might retry to send the same
 * message again. Hence, it's often sensible to assume duplicated messages will be close to each other.
 *
 * Hence typical event handling systems, introduce time-to-live for each ID and remove it from memory when the
 * time-to-live expires.
 *
 * With that in mind, for simplicity, we are using a in memory, non-distributed, cache provided by {@link CacheLoader}.
 * Each entry has a time to live specified by {@link #deDupExpiryTimeIntervalInSecs}. It is by default 5 mins.
 * This number is based on how other messaging systems handle deduplication (eg: AWS SQS). Override this number as
 * appropriate to the business case. After the time to live, the event id will be discarded from the cache.
 *
 * If during the time interval, if the same event arrives, it is marked as duplicate and is not sent for further
 * processing.
 *
 * In addition, it publishes two events to {@link MetricsCollector} via {@link EventBus}.
 * One event if its a unique message. And the other, if its a duplicate event. Both these events are pubished
 * asynchronously for further processing.
 */
@Slf4j
@Component
public class DeduplicationHandler {

    private final int deDupExpiryTimeIntervalInSecs;
    private final EventBus eventBus;
    private final LoadingCache<String, String> cacheLoader;

    @Autowired
    public DeduplicationHandler(
            final EventBus eventBus,
            @Value("${deduplication.expiry.time.interval.secs:300}") final int deDupExpiryTimeIntervalInSecs) {

        this.eventBus = eventBus;
        this.deDupExpiryTimeIntervalInSecs = deDupExpiryTimeIntervalInSecs;
        final CacheLoader<String, String> cache = new CacheLoader<String, String>() {
            @Override
            public String load(final String key) {
                return key;
            }
        };

        this.cacheLoader = CacheBuilder.<String, String>newBuilder()
            .expireAfterWrite(Duration.ofSeconds(this.deDupExpiryTimeIntervalInSecs))
            .removalListener(this::checkRemovedEntries)
            .build(cache);
    }

    private void checkRemovedEntries(final RemovalNotification<Object, Object> notification) {
        log.debug("Event {} was removed post expiry.", notification);
    }

    /**
     * Tries to remove the duplicate events that are already processed.
     *
     * @param event An instance of {@link Buffer}.
     */
    public Buffer handle(final Buffer event) {
        Preconditions.checkArgument(Objects.nonNull(event), "Message event cannot be null");
        Preconditions.checkArgument(event.length() > 0, "Message event cannot be empty");

        final String evenHash = event.toString();

        if (ObjectUtils.isEmpty(this.cacheLoader.getIfPresent(evenHash))) {
            this.cacheLoader.put(evenHash, evenHash);

            // Publish an event stating a unique event was found
            //this.eventBus.publish(MetricsCollector.name(), JsonUtils.toJson(new EventMetadata(EventType.NEW)));
            this.eventBus.publish(MetricsCollector.name(), EventType.NEW.name());
            return event;
        }

        // Publish an event stating a duplicate event was found
        this.eventBus.publish(MetricsCollector.name(), EventType.DUPLICATE.name());
        log.debug("Duplicate event found with hash {}", evenHash);
        return Buffer.buffer();
    }
}
