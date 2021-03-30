Feature: Test
  Background:
    Given I created an account

  @QA-sync
  Scenario Outline: Cucumber KeySync

    When I sync devices A and B
    Then I check devices A and B are sync
    When I disable sync on device A
    Then I check devices A and B are not sync
    When I enable sync on device A
    And I sync devices A and B
    Then I check devices A and B are sync
    When I create an account for sync on device C
    Then I check devices C and A are not sync
    When I enable sync on device C
    And I sync devices C and A
    Then I check devices C and A are sync
    When I disable sync on device C
    Then I check devices A and B are sync



    #And I setup second account for devices A and B
    Examples:
      |account|
      |  0    |


