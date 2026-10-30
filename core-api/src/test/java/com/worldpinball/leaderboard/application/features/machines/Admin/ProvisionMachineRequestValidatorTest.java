package com.worldpinball.leaderboard.application.features.machines.Admin;

import com.worldpinball.leaderboard.application.features.machines.Admin.provision.dtos.ProvisionMachineRequest;
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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProvisionMachineRequest Validation Tests")
class ProvisionMachineRequestValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("When request is valid, should have no validation errors")
    void validate_whenRequestIsValid_shouldHaveNoErrors() {
        ProvisionMachineRequest request = new ProvisionMachineRequest(
                "WP-K7M4-9X2P-R8HD",
                "resident-evil",
                "Resident Evil"
        );

        Set<ConstraintViolation<ProvisionMachineRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("When serialNumber is empty, should have validation error on worldPinballSerialNumber")
    void validate_whenSerialNumberIsEmpty_shouldHaveSerialNumberError(String invalidSerialNumber) {
        ProvisionMachineRequest request = new ProvisionMachineRequest(
                invalidSerialNumber,
                "resident-evil",
                "Resident Evil"
        );

        Set<ConstraintViolation<ProvisionMachineRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("worldPinballSerialNumber"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("When ipSlug is empty, should have validation error on ipSlug")
    void validate_whenIpSlugIsEmpty_shouldHaveIpSlugError(String invalidIpSlug) {
        ProvisionMachineRequest request = new ProvisionMachineRequest(
                "WP-K7M4-9X2P-R8HD",
                invalidIpSlug,
                "Resident Evil"
        );

        Set<ConstraintViolation<ProvisionMachineRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("ipSlug"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("When displayName is empty, should have validation error on displayName")
    void validate_whenDisplayNameIsEmpty_shouldHaveDisplayNameError(String invalidDisplayName) {
        ProvisionMachineRequest request = new ProvisionMachineRequest(
                "WP-K7M4-9X2P-R8HD",
                "resident-evil",
                invalidDisplayName
        );

        Set<ConstraintViolation<ProvisionMachineRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("displayName"));
    }
}
