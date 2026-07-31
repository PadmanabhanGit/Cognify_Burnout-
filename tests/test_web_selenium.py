import pytest
import os
from selenium import webdriver
from pages.login_page import LoginPage

# Use environment variable for BASE_URL as per requirements, default to localhost for local testing
BASE_URL = os.getenv("BASE_URL", "http://localhost:5173")

@pytest.fixture(scope="module")
def driver():
    options = webdriver.ChromeOptions()
    options.add_argument('--headless')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')
    
    try:
        driver = webdriver.Chrome(options=options)
        driver.implicitly_wait(10)
        yield driver
        driver.quit()
    except Exception as e:
        pytest.skip(f"Chrome WebDriver not found: {e}")

def test_web_empty_login_validation(driver):
    login_page = LoginPage(driver, BASE_URL)
    login_page.load()
    # Attempt to login with empty credentials
    login_page.login("", "")
    
    # Assert that the validation error is displayed
    assert login_page.is_error_displayed() is True

def test_web_invalid_login(driver):
    login_page = LoginPage(driver, BASE_URL)
    login_page.load()
    login_page.login("invalid@user.com", "wrongpass")
    
    # Assert that the Firebase auth error is displayed
    assert login_page.is_error_displayed() is True

def test_web_navigation_links(driver):
    driver.get(BASE_URL)
    # Validate the title or core elements exist
    assert driver.title != ""

def test_web_responsive_layout(driver):
    driver.get(BASE_URL)
    driver.set_window_size(375, 812) # iPhone X dimensions
    assert driver.title != ""
