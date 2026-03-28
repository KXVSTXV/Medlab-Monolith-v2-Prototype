package com.cognizant.medlab.web.dto;

import com.cognizant.medlab.domain.patient.Patient;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Request/response DTOs for the Patient API. */
public final class PatientDto {

    private PatientDto() {}

    public record CreateRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dob,

        @NotNull(message = "Gender is required")
        Patient.Gender gender,

        @Email(message = "Valid email required")
        String email,

        @Pattern(regexp = "^[0-9]{10,15}$", message = "Mobile must be 10-15 digits")
        String mobileNumber,

        String address,
        String bloodGroup,
        String allergies,
        String medicalHistory,
        boolean consentGiven
    ) {}

    public record UpdateRequest(
        @NotBlank String fullName,
        @Past LocalDate dob,
        Patient.Gender gender,
        String address,
        String bloodGroup,
        String allergies,
        String medicalHistory,
        boolean consentGiven
    ) {}

    public record Response(
        Long id,
        String medicalRecordNumber,
        String fullName,
        LocalDate dob,
        Patient.Gender gender,
        String email,
        String mobileNumber,
        String address,
        String bloodGroup,
        String allergies,
        boolean consentGiven,
        Patient.PatientStatus status,
        String createdAt,
        String createdBy
    ) {
        public static Response from(Patient p) {
            return new Response(
                p.getId(), p.getMedicalRecordNumber(), p.getFullName(),
                p.getDob(), p.getGender(), p.getEmail(), p.getMobileNumber(),
                p.getAddress(), p.getBloodGroup(), p.getAllergies(),
                p.isConsentGiven(), p.getStatus(),
                p.getCreatedAt() != null ? p.getCreatedAt().toString() : null,
                p.getCreatedBy()
            );
        }
    }
}
