Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test15 StoreDraftEncryptedOnTrustedServersWhenNeverUnprotected
    When I open menu
    Then I select from screen preferences_action
    And I select from screen account_settings_action
    And I select from screen app_name
    Then I set checkbox pep_mistrust_server_and_store_mails_encrypted to true
    And I go back to message compose
    And I click message compose
    Then I fill messageTo field with username@email.com
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    Then I click view pEp_indicator
    And I check status is pEpRatingUnencrypted
    Then I press back
    Then I select from message menu is_always_secure
    Then I go back and save as draft
    And I open menu
    And I select from screen account_settings_folders
    And I select from screen special_mailbox_name_drafts
    And I click first message
    Then I open menu
    And I compare texts on screen: is_not_always_secure and is_not_always_secure
    Then I remove account



