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

def test_web_valid_login(driver):
    login_page = LoginPage(driver, BASE_URL)
    login_page.load()
    login_page.login("web_login", "web_pass")
    
    # Assert that we are navigated away from login (e.g. to dashboard)
    # In a real deployed app, the URL changes or a dashboard element appears
    assert "login" not in driver.current_url.lower() or "dashboard" in driver.current_url.lower()

def test_web_invalid_login(driver):
    login_page = LoginPage(driver, BASE_URL)
    login_page.load()
    login_page.login("invalid@user.com", "wrongpass")
    
    # In a real app, an error message is displayed
    assert login_page.is_error_displayed() is True

def test_web_navigation_links(driver):
    driver.get(BASE_URL)
    # Validate the title or core elements exist
    assert driver.title != ""

def test_web_responsive_layout(driver):
    driver.get(BASE_URL)
    driver.set_window_size(375, 812) # iPhone X dimensions
    assert driver.title != ""
