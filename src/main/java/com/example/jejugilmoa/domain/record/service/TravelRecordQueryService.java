package com.example.jejugilmoa.domain.record.service;

import com.example.jejugilmoa.domain.imageupload.service.ImageUrlResolver;
import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.converter.TravelRecordConverter;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCardResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordDetailResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordImageResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordMapPlaceResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordMapResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordPlaceResponse;
import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.entity.TravelRecordImage;
import com.example.jejugilmoa.domain.record.entity.TravelRecordPlace;
import com.example.jejugilmoa.domain.record.enums.ReactionType;
import com.example.jejugilmoa.domain.record.enums.RecordView;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.repository.TravelRecordImageRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordPlaceRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordReactionRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelRecordQueryService {

    private final TravelRecordRepository travelRecordRepository;
    private final TravelRecordPlaceRepository travelRecordPlaceRepository;
    private final TravelRecordImageRepository travelRecordImageRepository;
    private final TravelRecordReactionRepository travelRecordReactionRepository;
    private final ImageUrlResolver imageUrlResolver;

    public PageResponse<?> getRecords(Long userId, RecordView view, boolean mine, Pageable pageable) {
        Page<TravelRecord> records = mine
                ? travelRecordRepository.findActiveByUserId(userId, pageable)
                : travelRecordRepository.findActivePublic(pageable);
        return view == RecordView.MAP ? toMapPage(records) : toCardPage(records, userId);
    }

    public TravelRecordDetailResponse getDetail(Long recordId, Long userId) {
        TravelRecord record = travelRecordRepository.findActiveByIdWithUserAndPlan(recordId)
                .filter(found -> found.getUser().getId().equals(userId)
                        || found.getVisibility() == Visibility.PUBLIC)
                .orElseThrow(() -> new GeneralException(RecordErrorCode.RECORD_NOT_FOUND));

        List<TravelRecordPlace> places = travelRecordPlaceRepository
                .findAllByRecordIdInSnapshotOrder(recordId);
        List<TravelRecordImage> images = travelRecordImageRepository
                .findAllByRecordIdOrderBySequence(recordId);

        Map<Long, List<TravelRecordImageResponse>> placeImages = new HashMap<>();
        List<TravelRecordImageResponse> recordImages = new ArrayList<>();
        List<TravelRecordImageResponse> allImages = new ArrayList<>();
        for (TravelRecordImage image : images) {
            TravelRecordImageResponse response = TravelRecordConverter.toImageResponse(
                    image, imageUrlResolver.resolve(image.getObjectKey()));
            allImages.add(response);
            if (image.getTravelRecordPlace() == null) {
                recordImages.add(response);
            } else {
                placeImages.computeIfAbsent(image.getTravelRecordPlace().getId(), ignored -> new ArrayList<>())
                        .add(response);
            }
        }
        allImages.sort(Comparator.comparingInt(TravelRecordImageResponse::sequenceOrder));

        List<TravelRecordPlaceResponse> placeResponses = places.stream()
                .map(place -> TravelRecordConverter.toPlaceResponse(
                        place, placeImages.getOrDefault(place.getId(), List.of())))
                .toList();
        ReactionSummary reactions = loadReactionSummaries(List.of(recordId)).getOrDefault(
                recordId, ReactionSummary.EMPTY);
        ReactionType myReaction = loadMyReactions(List.of(recordId), userId).get(recordId);

        return TravelRecordConverter.toDetailResponse(
                record, recordImages, allImages, placeResponses,
                reactions.likeCount(), reactions.dislikeCount(), myReaction);
    }

    private PageResponse<TravelRecordCardResponse> toCardPage(Page<TravelRecord> records, Long userId) {
        List<Long> recordIds = records.getContent().stream().map(TravelRecord::getId).toList();
        if (recordIds.isEmpty()) {
            return emptyPage(records);
        }

        Map<Long, TravelRecordImage> thumbnails = travelRecordImageRepository
                .findThumbnailImagesByRecordIds(recordIds).stream()
                .collect(Collectors.toMap(image -> image.getTravelRecord().getId(), Function.identity()));
        Map<Long, Long> visitedPlaceCounts = travelRecordPlaceRepository.countVisitedByRecordIds(recordIds)
                .stream().collect(Collectors.toMap(
                        TravelRecordPlaceRepository.VisitedPlaceCount::getRecordId,
                        TravelRecordPlaceRepository.VisitedPlaceCount::getCount));
        Map<Long, Long> photoCounts = travelRecordImageRepository.countByRecordIds(recordIds)
                .stream().collect(Collectors.toMap(
                        TravelRecordImageRepository.RecordImageCount::getRecordId,
                        TravelRecordImageRepository.RecordImageCount::getCount));
        Map<Long, ReactionSummary> reactions = loadReactionSummaries(recordIds);
        Map<Long, ReactionType> myReactions = loadMyReactions(recordIds, userId);

        return PageResponse.of(records.map(record -> {
            TravelRecordImage thumbnail = thumbnails.get(record.getId());
            String thumbnailUrl = thumbnail == null ? null : imageUrlResolver.resolve(thumbnail.getObjectKey());
            ReactionSummary reaction = reactions.getOrDefault(record.getId(), ReactionSummary.EMPTY);
            return TravelRecordConverter.toCardResponse(
                    record,
                    thumbnailUrl,
                    visitedPlaceCounts.getOrDefault(record.getId(), 0L),
                    photoCounts.getOrDefault(record.getId(), 0L),
                    reaction.likeCount(),
                    reaction.dislikeCount(),
                    myReactions.get(record.getId()));
        }));
    }

    private PageResponse<TravelRecordMapResponse> toMapPage(Page<TravelRecord> records) {
        List<Long> recordIds = records.getContent().stream().map(TravelRecord::getId).toList();
        if (recordIds.isEmpty()) {
            return emptyPage(records);
        }

        Map<Long, List<TravelRecordMapPlaceResponse>> placesByRecord = travelRecordPlaceRepository
                .findAllByRecordIdsInSnapshotOrder(recordIds).stream()
                .collect(Collectors.groupingBy(
                        place -> place.getTravelRecord().getId(),
                        Collectors.mapping(TravelRecordConverter::toMapPlaceResponse, Collectors.toList())));

        return PageResponse.of(records.map(record -> TravelRecordConverter.toMapResponse(
                record, placesByRecord.getOrDefault(record.getId(), List.of()))));
    }

    private Map<Long, ReactionSummary> loadReactionSummaries(List<Long> recordIds) {
        Map<Long, EnumMap<ReactionType, Long>> grouped = new HashMap<>();
        travelRecordReactionRepository.countByRecordIdsAndType(recordIds).forEach(row ->
                grouped.computeIfAbsent(row.getRecordId(), ignored -> new EnumMap<>(ReactionType.class))
                        .put(row.getReactionType(), row.getCount()));
        return grouped.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> new ReactionSummary(
                        entry.getValue().getOrDefault(ReactionType.LIKE, 0L),
                        entry.getValue().getOrDefault(ReactionType.DISLIKE, 0L))));
    }

    private <T> PageResponse<T> emptyPage(Page<TravelRecord> records) {
        return new PageResponse<>(
                List.of(), records.getNumber(), records.getSize(), records.getTotalElements(),
                records.getTotalPages(), records.isLast());
    }

    private Map<Long, ReactionType> loadMyReactions(List<Long> recordIds, Long userId) {
        return travelRecordReactionRepository.findMineByRecordIds(recordIds, userId).stream()
                .collect(Collectors.toMap(
                        reaction -> reaction.getTravelRecord().getId(),
                        reaction -> reaction.getReactionType()));
    }

    private record ReactionSummary(long likeCount, long dislikeCount) {
        private static final ReactionSummary EMPTY = new ReactionSummary(0, 0);
    }
}
