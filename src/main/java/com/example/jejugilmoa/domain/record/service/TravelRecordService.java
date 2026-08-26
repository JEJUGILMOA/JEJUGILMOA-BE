package com.example.jejugilmoa.domain.record.service;

import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.imageupload.service.ImageObjectVerifier;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.domain.record.converter.TravelRecordConverter;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordPlaceMemoRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordPlaceUpdateRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordUpdateRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordUpdateResponse;
import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.entity.TravelRecordImage;
import com.example.jejugilmoa.domain.record.entity.TravelRecordPlace;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.repository.TravelRecordImageRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordPlaceRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TravelRecordService {

    private static final String RECORD_PLAN_UNIQUE_CONSTRAINT = "uk_travel_record_active_plan";

    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final TravelCourseRepository travelCourseRepository;
    private final TravelRecordRepository travelRecordRepository;
    private final TravelRecordPlaceRepository travelRecordPlaceRepository;
    private final TravelRecordImageRepository travelRecordImageRepository;
    private final ImageObjectVerifier imageObjectVerifier;

    @Transactional
    public TravelRecordCreateResponse create(Long userId, TravelRecordCreateRequest request) {
        var user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
        var plan = travelPlanRepository.findByIdForUpdate(request.tripId())
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));

        if (!plan.getUser().getId().equals(userId)) {
            throw new GeneralException(RecordErrorCode.RECORD_TRIP_ACCESS_DENIED);
        }
        if (plan.getStatus() != TravelPlanStatus.COMPLETED) {
            throw new GeneralException(RecordErrorCode.RECORD_TRIP_NOT_COMPLETED);
        }
        if (travelRecordRepository.existsByTravelPlanIdAndDeletedAtIsNull(request.tripId())) {
            throw new GeneralException(RecordErrorCode.RECORD_ALREADY_EXISTS);
        }

        List<TravelCourse> courses = travelCourseRepository
                .findAllByTravelPlanIdWithPlaceOrderByVisitDateAscSequenceOrderAsc(request.tripId());
        Map<Long, TravelRecordPlaceMemoRequest> placeInputs = validateAndIndexPlaceInputs(
                request.placeMemos(), courses);
        List<String> recordImageObjectKeys = nullSafe(request.imageObjectKeys());
        List<String> placeImageObjectKeys = placeInputs.values().stream()
                .map(TravelRecordPlaceMemoRequest::imageObjectKey)
                .filter(java.util.Objects::nonNull)
                .toList();
        validateObjectKeys(java.util.stream.Stream.concat(
                recordImageObjectKeys.stream(), placeImageObjectKeys.stream()).toList(), userId);

        var record = TravelRecordConverter.toEntity(request, plan, user);
        try {
            record = travelRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, RECORD_PLAN_UNIQUE_CONSTRAINT)) {
                throw new GeneralException(RecordErrorCode.RECORD_ALREADY_EXISTS);
            }
            throw exception;
        }

        var recordPlaces = new ArrayList<TravelRecordPlace>();
        Map<Long, TravelRecordPlace> recordPlacesByCourseId = new HashMap<>();
        for (int index = 0; index < courses.size(); index++) {
            TravelCourse course = courses.get(index);
            TravelRecordPlaceMemoRequest placeInput = placeInputs.get(course.getId());
            TravelRecordPlace recordPlace = TravelRecordConverter.toPlace(
                    record, course, index + 1, placeInput == null ? null : placeInput.memo());
            recordPlaces.add(recordPlace);
            recordPlacesByCourseId.put(course.getId(), recordPlace);
        }
        travelRecordPlaceRepository.saveAll(recordPlaces);

        var images = new ArrayList<TravelRecordImage>();
        int imageSequence = 1;
        for (String objectKey : recordImageObjectKeys) {
            images.add(TravelRecordConverter.toImage(record, objectKey, imageSequence++));
        }
        for (TravelCourse course : courses) {
            TravelRecordPlaceMemoRequest placeInput = placeInputs.get(course.getId());
            if (placeInput != null && placeInput.imageObjectKey() != null) {
                images.add(TravelRecordConverter.toImage(record, recordPlacesByCourseId.get(course.getId()),
                        placeInput.imageObjectKey(), imageSequence++));
            }
        }
        travelRecordImageRepository.saveAll(images);

        return TravelRecordConverter.toCreateResponse(record, request.tripId());
    }

    @Transactional
    public TravelRecordUpdateResponse update(Long userId, Long recordId, TravelRecordUpdateRequest request) {
        TravelRecord record = getOwnedRecord(userId, recordId);
        List<TravelRecordPlace> recordPlaces = travelRecordPlaceRepository
                .findAllByRecordIdInSnapshotOrder(recordId);
        Map<Long, TravelRecordPlace> placesById = recordPlaces.stream()
                .collect(Collectors.toMap(TravelRecordPlace::getId, Function.identity()));
        Map<Long, TravelRecordPlaceUpdateRequest> placeUpdates = indexPlaceUpdates(request.places(), placesById);
        List<TravelRecordImage> currentImages = travelRecordImageRepository
                .findAllByRecordIdOrderBySequence(recordId);

        record.updateContent(request.title(), request.description(), request.visibility());
        placeUpdates.forEach((placeId, update) -> {
            if (update.memo() != null) {
                placesById.get(placeId).updateMemo(update.memo());
            }
        });
        boolean changesImages = request.imageObjectKeys() != null
                || placeUpdates.values().stream().anyMatch(update -> update.image() != null);
        if (changesImages) {
            updateImages(record, recordPlaces, placeUpdates, currentImages, request.imageObjectKeys(), userId);
        }
        travelRecordRepository.flush();
        return TravelRecordConverter.toUpdateResponse(record);
    }

    @Transactional
    public void delete(Long userId, Long recordId) {
        TravelRecord record = getOwnedRecord(userId, recordId);
        record.delete();
    }

    private TravelRecord getOwnedRecord(Long userId, Long recordId) {
        TravelRecord record = travelRecordRepository.findById(recordId)
                .orElseThrow(() -> new GeneralException(RecordErrorCode.RECORD_NOT_FOUND));
        if (record.getDeletedAt() != null) {
            throw new GeneralException(RecordErrorCode.RECORD_ALREADY_DELETED);
        }
        if (!record.getUser().getId().equals(userId)) {
            throw new GeneralException(RecordErrorCode.RECORD_ACCESS_DENIED);
        }
        return record;
    }

    private Map<Long, TravelRecordPlaceUpdateRequest> indexPlaceUpdates(
            List<TravelRecordPlaceUpdateRequest> requested, Map<Long, TravelRecordPlace> placesById) {
        Map<Long, TravelRecordPlaceUpdateRequest> updates = new HashMap<>();
        for (TravelRecordPlaceUpdateRequest update : nullSafe(requested)) {
            if (!placesById.containsKey(update.recordPlaceId())
                    || updates.putIfAbsent(update.recordPlaceId(), update) != null) {
                throw new GeneralException(RecordErrorCode.RECORD_PLACE_TARGET_MISMATCH);
            }
            if (update.image() != null) {
                boolean invalidReplace = update.image().action()
                        == com.example.jejugilmoa.domain.record.enums.RecordPlaceImageAction.REPLACE
                        && (update.image().objectKey() == null || update.image().objectKey().isBlank());
                boolean invalidRemove = update.image().action()
                        == com.example.jejugilmoa.domain.record.enums.RecordPlaceImageAction.REMOVE
                        && update.image().objectKey() != null;
                if (invalidReplace || invalidRemove) {
                    throw new GeneralException(RecordErrorCode.RECORD_INVALID_OBJECT_KEY);
                }
            }
        }
        return updates;
    }

    private void updateImages(
            TravelRecord record,
            List<TravelRecordPlace> recordPlaces,
            Map<Long, TravelRecordPlaceUpdateRequest> placeUpdates,
            List<TravelRecordImage> currentImages,
            List<String> requestedRecordKeys,
            Long userId) {
        List<TravelRecordImage> currentRecordImages = currentImages.stream()
                .filter(image -> image.getTravelRecordPlace() == null).toList();
        Map<String, TravelRecordImage> currentRecordByKey = currentRecordImages.stream()
                .collect(Collectors.toMap(TravelRecordImage::getObjectKey, Function.identity()));
        Map<Long, TravelRecordImage> currentPlaceById = currentImages.stream()
                .filter(image -> image.getTravelRecordPlace() != null)
                .collect(Collectors.toMap(image -> image.getTravelRecordPlace().getId(), Function.identity()));

        List<String> finalRecordKeys = requestedRecordKeys == null
                ? currentRecordImages.stream().map(TravelRecordImage::getObjectKey).toList()
                : requestedRecordKeys;
        Map<Long, String> finalPlaceKeys = new HashMap<>();
        currentPlaceById.forEach((placeId, image) -> finalPlaceKeys.put(placeId, image.getObjectKey()));
        placeUpdates.forEach((placeId, update) -> {
            if (update.image() == null) {
                return;
            }
            if (update.image().action()
                    == com.example.jejugilmoa.domain.record.enums.RecordPlaceImageAction.REMOVE) {
                finalPlaceKeys.remove(placeId);
            } else {
                finalPlaceKeys.put(placeId, update.image().objectKey());
            }
        });

        List<String> finalKeys = java.util.stream.Stream.concat(
                finalRecordKeys.stream(), finalPlaceKeys.values().stream()).toList();
        if (new HashSet<>(finalKeys).size() != finalKeys.size()) {
            throw new GeneralException(RecordErrorCode.RECORD_INVALID_OBJECT_KEY);
        }
        Set<String> currentKeys = currentImages.stream().map(TravelRecordImage::getObjectKey)
                .collect(Collectors.toSet());
        validateObjectKeys(finalKeys.stream().filter(key -> !currentKeys.contains(key)).toList(), userId);

        Set<TravelRecordImage> retained = new HashSet<>();
        List<TravelRecordImage> finalRecordImages = new ArrayList<>();
        for (String key : finalRecordKeys) {
            TravelRecordImage image = currentRecordByKey.get(key);
            if (image == null) {
                image = TravelRecordConverter.toImage(record, key, 0);
            } else {
                retained.add(image);
            }
            finalRecordImages.add(image);
        }

        Map<Long, TravelRecordImage> finalPlaceImages = new HashMap<>();
        finalPlaceKeys.forEach((placeId, key) -> {
            TravelRecordImage image = currentPlaceById.get(placeId);
            if (image == null) {
                image = TravelRecordConverter.toImage(record, placesById(recordPlaces).get(placeId), key, 0);
            } else {
                retained.add(image);
                if (!image.getObjectKey().equals(key)) {
                    image.replaceObjectKey(key);
                }
            }
            finalPlaceImages.put(placeId, image);
        });

        List<TravelRecordImage> removed = currentImages.stream()
                .filter(image -> !retained.contains(image)).toList();
        travelRecordImageRepository.deleteAll(removed);
        travelRecordImageRepository.flush();

        int temporarySequence = -1;
        for (TravelRecordImage image : retained) {
            image.changeSequenceOrder(temporarySequence--);
        }
        travelRecordImageRepository.flush();

        List<TravelRecordImage> finalImages = new ArrayList<>();
        int sequence = 1;
        for (TravelRecordImage image : finalRecordImages) {
            image.changeSequenceOrder(sequence++);
            finalImages.add(image);
        }
        for (TravelRecordPlace place : recordPlaces) {
            TravelRecordImage image = finalPlaceImages.get(place.getId());
            if (image != null) {
                image.changeSequenceOrder(sequence++);
                finalImages.add(image);
            }
        }
        travelRecordImageRepository.saveAll(finalImages);
    }

    private Map<Long, TravelRecordPlace> placesById(List<TravelRecordPlace> places) {
        return places.stream().collect(Collectors.toMap(TravelRecordPlace::getId, Function.identity()));
    }

    private Map<Long, TravelRecordPlaceMemoRequest> validateAndIndexPlaceInputs(
            List<TravelRecordPlaceMemoRequest> requestedMemos, List<TravelCourse> courses) {
        Set<Long> courseIds = courses.stream().map(TravelCourse::getId).collect(Collectors.toSet());
        Set<Long> memoCourseIds = new HashSet<>();
        Map<Long, TravelRecordPlaceMemoRequest> placeInputs = new HashMap<>();
        for (TravelRecordPlaceMemoRequest requestedMemo : nullSafe(requestedMemos)) {
            if (!courseIds.contains(requestedMemo.travelCourseId())
                    || !memoCourseIds.add(requestedMemo.travelCourseId())) {
                throw new GeneralException(RecordErrorCode.RECORD_MEMO_TARGET_MISMATCH);
            }
            placeInputs.put(requestedMemo.travelCourseId(), requestedMemo);
        }
        return placeInputs;
    }

    private void validateObjectKeys(List<String> requestedObjectKeys, Long userId) {
        List<String> objectKeys = nullSafe(requestedObjectKeys);
        String expectedPrefix = "records/%d/".formatted(userId);
        Set<String> uniqueKeys = new HashSet<>();
        if (objectKeys.stream().anyMatch(key -> !key.startsWith(expectedPrefix)
                || key.length() == expectedPrefix.length() || !uniqueKeys.add(key))) {
            throw new GeneralException(RecordErrorCode.RECORD_INVALID_OBJECT_KEY);
        }
        objectKeys.forEach(imageObjectVerifier::verify);
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private boolean hasConstraint(Throwable throwable, String constraintName) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && constraintName.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            if (current.getMessage() != null && current.getMessage().contains(constraintName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
