Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test12 assertRemoveTwoDifferentColorContactsAndKeepOriginalToolbarColor
    Then I click message compose
    And I fill messageTo field with self
    And I fill messageTo field with unknown@user.is
    And I fill messageSubject field with Subject
    And I fill messageBody field with TheBody
    Then I check toolBar color is pep_no_color
    Then I press back
    And I discard message
    Then I check toolBar color is pep_green
    Then I click message compose
    And I fill messageTo field with random@test.pep-security.net
    And I fill messageTo field with unknown@user.is
    And I fill messageSubject field with Subject
    And I fill messageBody field with TheBody
    Then I check toolBar color is pep_no_color
    Then I press back
    And I discard message
    Then I check toolBar color is pep_green
    Then I remove account




