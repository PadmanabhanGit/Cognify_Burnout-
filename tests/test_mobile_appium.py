import pytest
from appium import webdriver
from appium.options.android import UiAutomator2Options
from appium.webdriver.common.appiumby import AppiumBy
from pages.app_dashboard_page import AppDashboardPage
import time

@pytest.fixture(scope="module")
def driver():
    import os
    apk_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '../app/build/outputs/apk/debug/app-debug.apk'))
    
    options = UiAutomator2Options()
    options.platform_name = 'Android'
    options.device_name = 'Android Emulator'
    options.app = apk_path
    options.app_package = 'com.simats.burnouttracker'
    options.app_activity = '.MainActivity'
    options.automation_name = 'UiAutomator2'
    options.no_reset = True
    
    try:
        # Appium 2.x defaults to / base path, but our previous tests used /wd/hub or just /
        # If your Appium server starts on / specifically, use http://127.0.0.1:4723
        driver = webdriver.Remote('http://127.0.0.1:4723', options=options)
        yield driver
        driver.quit()
    except Exception as e:
        pytest.skip(f"Appium server not running or Emulator not found: {e}")

def test_mobile_app_launch(driver):
    dashboard = AppDashboardPage(driver)
    # Verify that the dashboard loads by asserting the presence of the dashboard title element
    assert dashboard.is_dashboard_loaded() is True
    
def test_mobile_navigation_to_sleep_mood(driver):
    dashboard = AppDashboardPage(driver)
    # Perform actual click on the Sleep & Mood feature card
    dashboard.navigate_to_sleep_mood()
    # Adding a brief sleep to allow Compose navigation animations to finish before the next test
    time.sleep(1)

def test_mobile_navigation_to_productivity(driver):
    # Navigate back to dashboard (since previous test navigated away)
    # Appium provides a standard back button action for Android
    driver.back()
    time.sleep(1)
    
    dashboard = AppDashboardPage(driver)
    # Perform actual click on the Productivity feature card
    dashboard.navigate_to_productivity()
    time.sleep(1)

def test_mobile_navigation_to_study_tracker(driver):
    # Navigate back to dashboard
    driver.back()
    time.sleep(1)
    
    dashboard = AppDashboardPage(driver)
    # Perform actual click on the Study Tracking feature card
    dashboard.navigate_to_study()
    time.sleep(1)
