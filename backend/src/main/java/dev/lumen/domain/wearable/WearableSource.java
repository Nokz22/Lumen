package dev.lumen.domain.wearable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A data source, not a persistence port — fetch() returns readings to be ingested,
 * it does not save them. A future Fitbit/Garmin adapter implements this exact
 * interface by calling an external API instead of generating synthetic data; nothing
 * downstream of WearableSource needs to change.
 */
public interface WearableSource {

    WearableSourceType getSourceType();

    List<WearableReading> fetch(UUID userId, Instant since, Instant until);
}
