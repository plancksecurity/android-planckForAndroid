Feature: Symmetric key generation generates unique keys with a key size of 512 bit
  Background:
    Given I created an account

    #Summary:
    # Description:
  Scenario: Cucumber Symmetric key generation generates unique keys
    And I send 1 messages to bot1 with subject SymmetricKey and body UniqueKeys
    And I save session_key from JSON of 26 messages




