package dev.lumen.domain.recommendation;

import java.util.UUID;

public interface RecommendationNotifier {

    void notify(UUID userId, RecommendationNotification notification);
}
