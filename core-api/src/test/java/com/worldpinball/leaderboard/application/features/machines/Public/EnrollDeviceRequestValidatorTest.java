package com.worldpinball.leaderboard.application.features.machines.Public;

import com.worldpinball.leaderboard.application.features.machines.Public.enroll.EnrollDeviceRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EnrollDeviceRequest Validation Tests")
class EnrollDeviceRequestValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static final EnrollDeviceRequest VALID_REQUEST = new EnrollDeviceRequest(
            UUID.randomUUID(),
            "ENROLL-SECRET-TOKEN-12345",
            "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A\n-----END PUBLIC KEY-----",
            "DEV-SN-001"
    );

    @Test
    @DisplayName("When request is valid, should have no validation errors")
    void validate_whenRequestIsValid_shouldHaveNoErrors() {
        Set<ConstraintViolation<EnrollDeviceRequest>> violations = validator.validate(VALID_REQUEST);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("When machineId is null, should have validation error on machineId")
    void validate_whenMachineIdIsNull_shouldHaveError() {
        EnrollDeviceRequest request = new EnrollDeviceRequest(
                null,
                VALID_REQUEST.plainEnrollmentToken(),
                VALID_REQUEST.publicKeyPem(),
                VALID_REQUEST.deviceSerialNumber()
        );

        Set<ConstraintViolation<EnrollDeviceRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("machineId"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("When plainEnrollmentToken is empty, should have validation error")
    void validate_whenPlainEnrollmentTokenIsEmpty_shouldHaveError(String invalidToken) {
        EnrollDeviceRequest request = new EnrollDeviceRequest(
                VALID_REQUEST.machineId(),
                invalidToken,
                VALID_REQUEST.publicKeyPem(),
                VALID_REQUEST.deviceSerialNumber()
        );

        Set<ConstraintViolation<EnrollDeviceRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("plainEnrollmentToken"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("When publicKeyPem is empty, should have validation error")
    void validate_whenPublicKeyPemIsEmpty_shouldHaveError(String invalidKey) {
        EnrollDeviceRequest request = new EnrollDeviceRequest(
                VALID_REQUEST.machineId(),
                VALID_REQUEST.plainEnrollmentToken(),
                invalidKey,
                VALID_REQUEST.deviceSerialNumber()
        );

        Set<ConstraintViolation<EnrollDeviceRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("publicKeyPem"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("When deviceSerialNumber is empty, should have validation error")
    void validate_whenDeviceSerialNumberIsEmpty_shouldHaveError(String invalidSerial) {
        EnrollDeviceRequest request = new EnrollDeviceRequest(
                VALID_REQUEST.machineId(),
                VALID_REQUEST.plainEnrollmentToken(),
                VALID_REQUEST.publicKeyPem(),
                invalidSerial
        );

        Set<ConstraintViolation<EnrollDeviceRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("deviceSerialNumber"));
    }
}
