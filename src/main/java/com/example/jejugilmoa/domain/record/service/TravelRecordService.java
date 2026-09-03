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
                .flatMap(placeInput -> nullSafe(placeInput.imageObjectKeys()).stream())
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
            if (placeInput != null) {
                for (String objectKey : nullSafe(placeInput.imageObjectKeys())) {
                    images.add(TravelRecordConverter.toImage(record, recordPlacesByCourseId.get(course.getId()),
                            objectKey, imageSequence++));
                }
            }
        }
        travelRecordImageRepository.saveAll(images);
        record.changeThumbnailImage(images.isEmpty() ? null : images.getFirst());

        return TravelRecordConverter.toCreateResponse(record, request.tripId());
    }

    @Transactional
    public TravelRecordUpdateResponse update(Long userId, Long recordId, TravelRecordUpdateRequest request) {
        TravelRecord record = getOwnedRecordForUpdate(userId, recordId);
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
        List<TravelRecordImage> finalImages = currentImages;
        if (changesImages) {
            finalImages = updateImages(
                    record, recordPlaces, placeUpdates, currentImages, request.imageObjectKeys(), userId);
        }
        updateThumbnail(record, finalImages, changesImages, request.thumbnailImageObjectKey());
        travelRecordRepository.flush();
        return TravelRecordConverter.toUpdateResponse(record);
    }

    @Transactional
    public void delete(Long userId, Long recordId) {
        TravelRecord record = getOwnedRecord(userId, recordId);
        record.delete();
    }

    private TravelRecord getOwnedRecord(Long userId, Long recordId) {
        return travelRecordRepository.findActiveOwnedRecord(recordId, userId)
                .orElseThrow(() -> new GeneralException(travelRecordRepository.existsActiveById(recordId)
                        ? RecordErrorCode.RECORD_ACCESS_DENIED
                        : RecordErrorCode.RECORD_NOT_FOUND));
    }

    private TravelRecord getOwnedRecordForUpdate(Long userId, Long recordId) {
        return travelRecordRepository.findActiveOwnedRecordForUpdate(recordId, userId)
                .orElseThrow(() -> new GeneralException(travelRecordRepository.existsActiveById(recordId)
                        ? RecordErrorCode.RECORD_ACCESS_DENIED
                        : RecordErrorCode.RECORD_NOT_FOUND));
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
                        && (update.image().objectKeys() == null || update.image().objectKeys().isEmpty());
                boolean invalidRemove = update.image().action()
                        == com.example.jejugilmoa.domain.record.enums.RecordPlaceImageAction.REMOVE
                        && update.image().objectKeys() != null;
                if (invalidReplace || invalidRemove) {
                    throw new GeneralException(RecordErrorCode.RECORD_INVALID_OBJECT_KEY);
                }
            }
        }
        return updates;
    }

    private List<TravelRecordImage> updateImages(
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
        Map<Long, List<TravelRecordImage>> currentPlaceById = currentImages.stream()
                .filter(image -> image.getTravelRecordPlace() != null)
                .collect(Collectors.groupingBy(image -> image.getTravelRecordPlace().getId()));

        List<String> finalRecordKeys = requestedRecordKeys == null
                ? currentRecordImages.stream().map(TravelRecordImage::getObjectKey).toList()
                : requestedRecordKeys;
        Map<Long, List<String>> finalPlaceKeys = new HashMap<>();
        currentPlaceById.forEach((placeId, images) -> finalPlaceKeys.put(placeId,
                images.stream().map(TravelRecordImage::getObjectKey).toList()));
        placeUpdates.forEach((placeId, update) -> {
            if (update.image() == null) {
                return;
            }
            if (update.image().action()
                    == com.example.jejugilmoa.domain.record.enums.RecordPlaceImageAction.REMOVE) {
                finalPlaceKeys.remove(placeId);
            } else {
                finalPlaceKeys.put(placeId, update.image().objectKeys());
            }
        });

        List<String> finalKeys = java.util.stream.Stream.concat(
                finalRecordKeys.stream(), finalPlaceKeys.values().stream().flatMap(List::stream)).toList();
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

        Map<Long, List<TravelRecordImage>> finalPlaceImages = new HashMap<>();
        Map<Long, TravelRecordPlace> recordPlacesById = placesById(recordPlaces);
        finalPlaceKeys.forEach((placeId, keys) -> {
            Map<String, TravelRecordImage> currentByKey = currentPlaceById
                    .getOrDefault(placeId, List.of()).stream()
                    .collect(Collectors.toMap(TravelRecordImage::getObjectKey, Function.identity()));
            List<TravelRecordImage> images = new ArrayList<>();
            for (String key : keys) {
                TravelRecordImage image = currentByKey.get(key);
                if (image == null) {
                    image = TravelRecordConverter.toImage(record, recordPlacesById.get(placeId), key, 0);
                } else {
                    retained.add(image);
                }
                images.add(image);
            }
            finalPlaceImages.put(placeId, images);
        });

        List<TravelRecordImage> removed = currentImages.stream()
                .filter(image -> !retained.contains(image)).toList();
        if (containsImage(removed, record.getThumbnailImage())) {
            record.changeThumbnailImage(null);
            travelRecordRepository.flush();
        }
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
            for (TravelRecordImage image : finalPlaceImages.getOrDefault(place.getId(), List.of())) {
                image.changeSequenceOrder(sequence++);
                finalImages.add(image);
            }
        }
        travelRecordImageRepository.saveAll(finalImages);
        travelRecordImageRepository.flush();
        return finalImages;
    }

    private void updateThumbnail(
            TravelRecord record,
            List<TravelRecordImage> finalImages,
            boolean changesImages,
            String requestedThumbnailKey) {
        if (requestedThumbnailKey != null) {
            TravelRecordImage requestedThumbnail = finalImages.stream()
                    .filter(image -> image.getObjectKey().equals(requestedThumbnailKey))
                    .findFirst()
                    .orElseThrow(() -> new GeneralException(
                            RecordErrorCode.RECORD_THUMBNAIL_TARGET_MISMATCH));
            record.changeThumbnailImage(requestedThumbnail);
            return;
        }

        if (changesImages && !containsImage(finalImages, record.getThumbnailImage())) {
            record.changeThumbnailImage(finalImages.isEmpty() ? null : finalImages.getFirst());
        }
    }

    private boolean containsImage(List<TravelRecordImage> images, TravelRecordImage target) {
        if (target == null) {
            return false;
        }
        return images.stream().anyMatch(image -> image == target
                || image.getId() != null && image.getId().equals(target.getId()));
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
