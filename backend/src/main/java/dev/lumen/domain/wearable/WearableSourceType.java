package dev.lumen.domain.wearable;

/**
 * SIMULATOR is the only implementation today. FITBIT/GARMIN/APPLE_HEALTH/
 * HEALTH_CONNECT are documented future adapters (ADR-0008) — adding one only means
 * implementing WearableSource, nothing in the domain or ingestion pipeline changes.
 */
public enum WearableSourceType {
    SIMULATOR
}
