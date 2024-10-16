#Author: Abdul Qadir Khan
#Date: 06/12/21
#Description: Demonstrate Cucumber Java framework with BS Automate product
@e2e @regression
Feature: E2E Flow
  Scenario Outline: TC-1856 Signed in User makes a Purchase for a product with Price and Vendor filters
    Given User is on home page
    When User clicks on sign in link
    And User enters <username> and <password> and clicks on sign in
    And <username> is signed in to the App
    And User clicks on Apple in Vendor filter
    And User sorts the search result by Price: Low to High
    And User can see the list of Apple products with Price in ascending order
    And User clicks on Add to cart for the iPhone XR and clicks on CHECKOUT
    And User enters <first>,<last>,<address>,<state>,<postal> and clicks SUBMIT
    Then User can see the order id for the product
    Examples:
      |username|password|first|last|address|state|postal|
      |"demouser"|"testingisfun99"|"first"|"last"|"test"|"test state"|1234|