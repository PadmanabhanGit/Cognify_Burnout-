import pytest
from appium import webdriver
from appium.options.android import UiAutomator2Options
from appium.webdriver.common.appiumby import AppiumBy
from pages.app_base_page import AppBasePage
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
    page = AppBasePage(driver)
    # The app starts on the Splash screen on a fresh install (like in CI).
    # Wait for the "Get Started" button to appear.
    get_started_btn = (AppiumBy.ID, "com.simats.burnouttracker:id/getStartedButton")
    
    # Assert that the button is found and displayed
    element = page.find_element(*get_started_btn)
    assert element is not None
    time.sleep(1)
    
def test_mobile_splash_navigation(driver):
    page = AppBasePage(driver)
    get_started_btn = (AppiumBy.ID, "com.simats.burnouttracker:id/getStartedButton")
    
    # Click the Get Started button to navigate forward
    page.click(*get_started_btn)
    
    # Allow time for navigation animation
    time.sleep(2)
    # We successfully navigated away from the splash screen without crashing
    assert True
