Feature: Test
  Background:
    Given I created an account


  @QTR-2230
  Scenario Outline: Cucumber Normal use of synced devices
    When Normal use of sync for devices A and B for 7 days
    #When I disable sync on device A
    #Then I check devices A and B are not sync
    #When I disable protection on device A
    #And I disable protection on device B
    #Then I check account devices A and B are not protected
    #When I enable sync on device A
    #And I enable protection on device A
    #And I enable protection on device B
    #And I remove all messages
    Examples:
      |account|
      |  0    |


