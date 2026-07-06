package dev.lumen.application.moodcheckin;

import dev.lumen.domain.moodcheckin.MoodCheckIn;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface MoodCheckInMapper {

    MoodCheckInResponse toResponse(MoodCheckIn moodCheckIn);
}
