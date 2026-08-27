package com.example.jejugilmoa.domain.record;

import com.example.jejugilmoa.domain.imageupload.service.ImageObjectVerifier;
import com.example.jejugilmoa.domain.record.dto.TravelRecordUpdateRequest;
import com.example.jejugilmoa.domain.record.repository.TravelRecordImageRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.record.service.TravelRecordService;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class TravelRecordMutationIntegrationTest {

    @Autowired TravelRecordService travelRecordService;
    @Autowired TravelRecordRepository travelRecordRepository;
    @Autowired TravelRecordImageRepository travelRecordImageRepository;
    @Autowired UserRepository userRepository;
    @Autowired JdbcClient jdbcClient;
    @MockitoBean ImageObjectVerifier imageObjectVerifier;

    @Test
    void updateReordersImagesAcrossDatabaseUniqueConstraint() {
        User owner = userRepository.saveAndFlush(User.builder().nickname("수정 통합 테스트").build());
        Long recordId = insertRecord(owner.getId());
        insertImage(recordId, "records/%d/A.jpg".formatted(owner.getId()), 1);
        insertImage(recordId, "records/%d/B.jpg".formatted(owner.getId()), 2);
        insertImage(recordId, "records/%d/C.jpg".formatted(owner.getId()), 3);

        String newKey = "records/%d/D.jpg".formatted(owner.getId());
        travelRecordService.update(owner.getId(), recordId, new TravelRecordUpdateRequest(
                null, null, null, null,
                List.of("records/%d/B.jpg".formatted(owner.getId()),
                        "records/%d/C.jpg".formatted(owner.getId()), newKey)));

        assertThat(travelRecordImageRepository.findAllByRecordIdOrderBySequence(recordId))
                .extracting(image -> image.getObjectKey() + ":" + image.getSequenceOrder())
                .containsExactly(
                        "records/%d/B.jpg:1".formatted(owner.getId()),
                        "records/%d/C.jpg:2".formatted(owner.getId()),
                        "records/%d/D.jpg:3".formatted(owner.getId()));
        verify(imageObjectVerifier).verify(newKey);
    }

    @Test
    void deleteKeepsDatabaseRowButExcludesAllActiveQueries() {
        User owner = userRepository.saveAndFlush(User.builder().nickname("삭제 통합 테스트").build());
        Long recordId = insertRecord(owner.getId());

        travelRecordService.delete(owner.getId(), recordId);
        travelRecordRepository.flush();

        Integer rowCount = jdbcClient.sql("SELECT COUNT(*) FROM travel_record WHERE id = :recordId")
                .param("recordId", recordId).query(Integer.class).single();
        assertThat(rowCount).isOne();
        assertThat(travelRecordRepository.findById(recordId)).get()
                .extracting(record -> record.getDeletedAt()).isNotNull();
        assertThat(travelRecordRepository.findActiveByIdWithUserAndPlan(recordId)).isEmpty();
        assertThat(travelRecordRepository.findActiveByUserId(
                owner.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent()).isEmpty();
    }

    private Long insertRecord(Long userId) {
        return jdbcClient.sql("""
                        INSERT INTO travel_record
                            (created_at, updated_at, user_id, title, visibility, like_count, dislike_count)
                        VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :userId, '통합 테스트 기록',
                                'PRIVATE', 0, 0)
                        RETURNING id
                        """)
                .param("userId", userId).query(Long.class).single();
    }

    private void insertImage(Long recordId, String objectKey, int sequenceOrder) {
        jdbcClient.sql("""
                        INSERT INTO travel_record_image
                            (created_at, updated_at, travel_record_id, object_key, sequence_order)
                        VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :recordId, :objectKey, :sequenceOrder)
                        """)
                .param("recordId", recordId).param("objectKey", objectKey)
                .param("sequenceOrder", sequenceOrder).update();
    }
}
