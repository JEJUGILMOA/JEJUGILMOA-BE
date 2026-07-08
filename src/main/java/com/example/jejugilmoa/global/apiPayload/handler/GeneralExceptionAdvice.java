package com.example.jejugilmoa.global.apiPayload.handler;

import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import com.example.jejugilmoa.global.apiPayload.code.GeneralErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GeneralExceptionAdvice {
    // 서비스 로직에서 의도적으로 발생시키는 예외
    // GeneralException을 상속한 모든 커스텀 예외 처리
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(
            GeneralException ex
    ) {
        log.warn("GeneralException occurred: code={}, message={}",
                ex.getCode().getCode(), ex.getCode().getMessage(), ex);
        return ResponseEntity.status(ex.getCode().getStatus())
                .body(ApiResponse.onFailure(
                                ex.getCode(),
                                null
                        )
                );
    }

    // 예상하지 못한 서버 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        BaseCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(
                        code,
                        null
                ));
    }

    // 유저 입력값 검증 예외 처리
    // DTO 필드 단위 오류 메시지 반환 (@Valid, @Validated 실패)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        log.warn("Validation exception occurred", ex);

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        BaseCode code = GeneralErrorCode.BAD_REQUEST;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(
                        code,
                        errors
                ));
    }

    // JSON 파싱 실패, 잘못된 enum 값 등 요청 본문을 읽을 수 없는 경우
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex
    ) {
        log.warn("HttpMessageNotReadable: {}", ex.getMessage());
        BaseCode code = GeneralErrorCode.BAD_REQUEST;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, null));
    }

    // 경로 변수 또는 쿼리 파라미터 타입 불일치
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {
        log.warn("MethodArgumentTypeMismatch: param={}, value={}", ex.getName(), ex.getValue());
        BaseCode code = GeneralErrorCode.BAD_REQUEST;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, null));
    }

    // 필수 쿼리 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex
    ) {
        log.warn("MissingServletRequestParameter: param={}", ex.getParameterName());
        BaseCode code = GeneralErrorCode.BAD_REQUEST;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, null));
    }

    // 폼 데이터 또는 모델 어트리뷰트 바인딩 실패
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleBindException(
            BindException ex
    ) {
        log.warn("BindException occurred", ex);
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        BaseCode code = GeneralErrorCode.BAD_REQUEST;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, errors));
    }

    // @Validated 클래스 레벨 또는 메서드 파라미터 제약 조건 위반
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleConstraintViolation(
            ConstraintViolationException ex
    ) {
        log.warn("ConstraintViolationException occurred", ex);
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        BaseCode code = GeneralErrorCode.BAD_REQUEST;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, errors));
    }

    // DB 무결성 제약 위반
    // 중복 키(PostgreSQL SQLState 23505)처럼 클라이언트 요청 기준으로 예상 가능한 충돌만 400
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        Throwable cause = ex.getMostSpecificCause();
        String message = cause.getMessage();

        boolean isDuplicateEntry =
                (cause instanceof SQLException sqlEx && "23505".equals(sqlEx.getSQLState()))
                        || (message != null && message.contains("duplicate key"));
        if (isDuplicateEntry) {
            log.warn("DataIntegrityViolationException (duplicate key): {}", message);
            BaseCode code = GeneralErrorCode.BAD_REQUEST;
            return ResponseEntity.status(code.getStatus())
                    .body(ApiResponse.onFailure(code, null));
        }

        log.error("DataIntegrityViolationException (unexpected): {}", message, ex);
        BaseCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, null));
    }

    // JPA flush/commit 시점 Bean Validation 실패 (서비스 로직에서 처리 못하고 트랜잭션까지 넘어온 경우)
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionSystemException(
            TransactionSystemException ex,
            HttpServletRequest request
    ) {
        Throwable cause = ex.getRootCause();
        if (cause instanceof ConstraintViolationException) {
            log.warn("요청 데이터가 유효성 검증을 통과하지 못했습니다: {}", cause.getMessage());
            BaseCode code = GeneralErrorCode.BAD_REQUEST;
            return ResponseEntity.status(code.getStatus())
                    .body(ApiResponse.onFailure(code, null));
        }
        log.error("TransactionSystemException: {}", ex.getMessage(), ex);
        BaseCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, null));
    }

    // 존재하지 않는 리소스 요청은 DEBUG 레벨로 로깅
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
    }

}
