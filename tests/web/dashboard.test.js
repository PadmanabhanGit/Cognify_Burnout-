const { Builder, By, until } = require('selenium-webdriver');

(async function testDashboard() {
  let driver = await new Builder().forBrowser('chrome').build();
  try {
    // Navigate to the local React app
    await driver.get('http://localhost:5173'); // Default Vite port

    // Wait for the dashboard header to load
    await driver.wait(until.elementLocated(By.xpath("//h1[contains(text(), 'BurnOutTracker')]")), 10000);
    
    // Check if stats cards are visible
    const statsCards = await driver.findElements(By.className('stats-card'));
    if (statsCards.length === 0) {
      throw new Error("Stats cards not found on the dashboard!");
    }
    
    console.log("✅ Web App Dashboard Test Passed!");
  } catch (error) {
    console.error("❌ Web App Dashboard Test Failed:", error);
    process.exit(1);
  } finally {
    await driver.quit();
  }
})();
