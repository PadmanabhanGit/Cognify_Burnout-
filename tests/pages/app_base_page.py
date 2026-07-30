from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

class AppBasePage:
    def __init__(self, driver):
        self.driver = driver
        self.timeout = 10

    def find_element(self, locator_type, locator_value):
        return WebDriverWait(self.driver, self.timeout).until(
            EC.presence_of_element_located((locator_type, locator_value))
        )

    def click(self, locator_type, locator_value):
        element = WebDriverWait(self.driver, self.timeout).until(
            EC.element_to_be_clickable((locator_type, locator_value))
        )
        element.click()

    def enter_text(self, locator_type, locator_value, text):
        element = self.find_element(locator_type, locator_value)
        element.clear()
        element.send_keys(text)
