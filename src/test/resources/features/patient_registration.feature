Feature: Patient Registration
  As a receptionist
  I want to register a new patient in the system
  So that the patient can be scheduled for lab tests

  Background:
    Given the system has the following roles: ROLE_ADMIN, ROLE_RECEPTION

  Scenario: Successfully register a patient with full details
    Given I am logged in as "reception1" with role "ROLE_RECEPTION"
    When I register a patient with the following details:
      | fullName     | Jane Tester        |
      | dob          | 1990-06-15         |
      | gender       | F                  |
      | email        | janetester@test.com|
      | mobileNumber | 9000000001         |
      | consentGiven | true               |
    Then the patient should be saved successfully
    And the patient should have a Medical Record Number assigned
    And an audit log entry should be created for "PATIENT_REGISTERED"

  Scenario: Registration fails when email is already registered
    Given a patient with email "duplicate@test.com" already exists
    When I try to register another patient with email "duplicate@test.com"
    Then I should receive a duplicate resource error
    And no new patient record should be created

  Scenario: Registration fails when consent is not given
    Given I am logged in as "reception1" with role "ROLE_RECEPTION"
    When I register a patient without giving consent
    Then the patient is saved but marked as consent_given = false
    And scheduling an appointment for this patient should be blocked

  Scenario: Register a complete patient flow
    Given I am logged in as "reception1" with role "ROLE_RECEPTION"
    When I register patient "Flow Test Patient" with mobile "9000000099"
    Then the patient count should increase by 1
    And I can find the patient by their MRN
