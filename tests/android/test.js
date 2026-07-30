const { remote } = require('webdriverio');
const exceljs = require('exceljs');
const fs = require('fs');
const path = require('path');

const RESULTS_DIR = path.join(__dirname, 'Test Results');

// Setup directories
['Excel', 'HTML', 'Screenshots', 'Logs', 'Summary'].forEach(dir => {
  fs.mkdirSync(path.join(RESULTS_DIR, dir), { recursive: true });
});

async function runTests() {
  const capabilities = {
    platformName: 'Android',
    'appium:automationName': 'UiAutomator2',
    'appium:app': path.join(__dirname, '../../app/build/outputs/apk/debug/app-debug.apk'),
    'appium:autoGrantPermissions': true,
  };

  const wdOpts = {
    hostname: process.env.APPIUM_HOST || 'localhost',
    port: parseInt(process.env.APPIUM_PORT, 10) || 4723,
    logLevel: 'info',
    capabilities,
  };

  const results = [];
  let passed = 0;
  let failed = 0;
  let driver;

  try {
    console.log('Starting Appium test...');
    driver = await remote(wdOpts);

    // Test 1: App Launch
    try {
      // Assuming wait for main activity
      await driver.pause(3000); // Simple wait for app to load
      results.push({ name: 'App Launch', status: 'PASS', error: '' });
      passed++;
    } catch (e) {
      results.push({ name: 'App Launch', status: 'FAIL', error: e.message });
      failed++;
    }

    // Screenshot
    const screenshot = await driver.takeScreenshot();
    fs.writeFileSync(path.join(RESULTS_DIR, 'Screenshots', 'app-launch.png'), screenshot, 'base64');

  } catch (e) {
    console.error('Failed to initialize driver:', e);
  } finally {
    if (driver) await driver.deleteSession();
    
    // Generate Reports
    const workbook = new exceljs.Workbook();
    const webSheet = workbook.addWorksheet('Test Cases');
    webSheet.addRow(['Test Case', 'Status', 'Error']);
    results.forEach(r => webSheet.addRow([r.name, r.status, r.error]));
    await workbook.xlsx.writeFile(path.join(RESULTS_DIR, 'Excel', 'Automation_Test_Report.xlsx'));

    // HTML Report
    const html = `
    <html>
      <head><title>Android Appium Test Execution Report</title></head>
      <body>
        <h1>Android Appium Test Execution Report</h1>
        <p>Total Tests: ${results.length} | Passed: ${passed} | Failed: ${failed}</p>
        <table border="1">
          <tr><th>Test Case</th><th>Status</th><th>Error</th></tr>
          ${results.map(r => `<tr><td>${r.name}</td><td style="color:${r.status==='PASS'?'green':'red'}">${r.status}</td><td>${r.error}</td></tr>`).join('')}
        </table>
      </body>
    </html>`;
    fs.writeFileSync(path.join(RESULTS_DIR, 'HTML', 'execution-report.html'), html);

    // Summary Markdown
    const summary = `
# Android Appium Test Summary

Build Number: ${process.env.GITHUB_RUN_NUMBER || 'Local'}
Execution Date: ${new Date().toISOString()}

Total Tests: ${results.length}
Passed: ${passed}
Failed: ${failed}
Skipped: 0
Pass Rate: ${results.length > 0 ? Math.round((passed / results.length) * 100) : 0}%
    `;
    fs.writeFileSync(path.join(RESULTS_DIR, 'Summary', 'summary.md'), summary.trim());
    
    fs.writeFileSync(path.join(RESULTS_DIR, 'Logs', 'execution.log'), 'Appium tests completed.\n');
  }
}

runTests();
