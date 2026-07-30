from appium.webdriver.common.appiumby import AppiumBy
from pages.app_base_page import AppBasePage

class AppDashboardPage(AppBasePage):
    # Locators based on Jetpack Compose content descriptions (testTags)
    NAV_SLEEP_MOOD = (AppiumBy.ACCESSIBILITY_ID, "nav_sleep_mood")
    NAV_PRODUCTIVITY = (AppiumBy.ACCESSIBILITY_ID, "nav_productivity")
    NAV_STUDY = (AppiumBy.ACCESSIBILITY_ID, "nav_study")
    DASHBOARD_TITLE = (AppiumBy.ACCESSIBILITY_ID, "dashboard_title")

    def __init__(self, driver):
        super().__init__(driver)

    def is_dashboard_loaded(self):
        try:
            return self.find_element(*self.DASHBOARD_TITLE).is_displayed()
        except:
            return False

    def navigate_to_sleep_mood(self):
        self.click(*self.NAV_SLEEP_MOOD)

    def navigate_to_productivity(self):
        self.click(*self.NAV_PRODUCTIVITY)

    def navigate_to_study(self):
        self.click(*self.NAV_STUDY)
