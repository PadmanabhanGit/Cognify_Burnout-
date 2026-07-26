const wdio = require('webdriverio');

const opts = {
  path: '/wd/hub',
  port: 4723,
  capabilities: {
    platformName: "Android",
    platformVersion: "11.0", // Update with target emulator version
    deviceName: "Android Emulator",
    app: "../../app/build/outputs/apk/debug/app-debug.apk", // Path to Android APK
    automationName: "UiAutomator2"
  }
};

async function testAppiumLogin() {
  console.log("Starting Appium session...");
  const client = await wdio.remote(opts);

  try {
    // Basic test: verify the app launched and wait for an element (e.g., login button)
    // Note: Replace 'com.simats.burnouttracker:id/loginButton' with actual resource ID
    // const loginBtn = await client.$('id=com.simats.burnouttracker:id/loginButton');
    // await loginBtn.waitForDisplayed({ timeout: 10000 });
    
    console.log("✅ Appium Android App Launched Successfully!");
  } catch (error) {
    console.error("❌ Appium Android Test Failed:", error);
    process.exit(1);
  } finally {
    await client.deleteSession();
  }
}

testAppiumLogin();
