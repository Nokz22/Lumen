package dev.lumen.infrastructure.persistence.exercise;

import dev.lumen.domain.exercise.ExerciseCompletion;
import dev.lumen.domain.exercise.ExerciseCompletionRepository;
import dev.lumen.domain.shared.PageQuery;
import dev.lumen.domain.shared.PagedResult;
import dev.lumen.infrastructure.persistence.shared.SpringDataPaging;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class ExerciseCompletionRepositoryImpl implements ExerciseCompletionRepository {

    private final SpringDataExerciseCompletionJpaRepository jpaRepository;

    ExerciseCompletionRepositoryImpl(SpringDataExerciseCompletionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ExerciseCompletion save(ExerciseCompletion completion) {
        return jpaRepository.save(completion);
    }

    @Override
    public List<ExerciseCompletion> findByUserIdOrderByCompletedAtDesc(UUID userId) {
        return jpaRepository.findByUserIdOrderByCompletedAtDesc(userId);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    public PagedResult<ExerciseCompletion> findPageByUserIdOrderByCompletedAtDesc(UUID userId, PageQuery pageQuery) {
        return SpringDataPaging.toPagedResult(
                jpaRepository.findPageByUserIdOrderByCompletedAtDesc(userId, SpringDataPaging.toPageable(pageQuery)));
    }
}
