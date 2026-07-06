package dev.lumen.application.assessment;

import dev.lumen.domain.crisis.CrisisResourceType;

public record CrisisResourceResponse(String name, CrisisResourceType type, String contact, String availability) {
}
