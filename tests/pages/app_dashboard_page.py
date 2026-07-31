from appium.webdriver.common.appiumby import AppiumBy
from pages.app_base_page import AppBasePage

class AppDashboardPage(AppBasePage):
    # Locators based on Jetpack Compose testTags mapped to resource IDs
    NAV_SLEEP_MOOD = (AppiumBy.ID, "com.simats.burnouttracker:id/featureSleepMood")
    NAV_PRODUCTIVITY = (AppiumBy.ID, "com.simats.burnouttracker:id/featureProductivity")
    NAV_STUDY = (AppiumBy.ID, "com.simats.burnouttracker:id/featureStudyTracking")
    DASHBOARD_TITLE = (AppiumBy.ID, "com.simats.burnouttracker:id/burnoutAlertBox") # Using alert box as proxy for dashboard load

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
