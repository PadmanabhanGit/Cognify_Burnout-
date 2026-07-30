const XLSX = require('xlsx');

function generateMockExcelReport() {
    const data = [];
    const suiteNames = [
        "BurnOutTracker Android App - Full Appium E2E Workflow",
        "BurnOutTracker Web App - Selenium E2E Workflow",
        "BurnOutTracker Backend - Security and Vulnerability Tests"
    ];

    const startTime = new Date();
    startTime.setMinutes(startTime.getMinutes() - 15);

    // Mock data for Android
    data.push({
        "Test Suite": suiteNames[0],
        "Total Tests": 447,
        "Passed": 447,
        "Failed": 0,
        "Pass Rate %": 100.0,
        "Duration (sec)": 368.61,
        "Start Time": startTime.toISOString(),
        "End Time": new Date(startTime.getTime() + 368610).toISOString()
    });

    // Mock data for Web
    data.push({
        "Test Suite": suiteNames[1],
        "Total Tests": 128,
        "Passed": 128,
        "Failed": 0,
        "Pass Rate %": 100.0,
        "Duration (sec)": 142.15,
        "Start Time": startTime.toISOString(),
        "End Time": new Date(startTime.getTime() + 142150).toISOString()
    });

    // Mock data for Security
    data.push({
        "Test Suite": suiteNames[2],
        "Total Tests": 56,
        "Passed": 56,
        "Failed": 0,
        "Pass Rate %": 100.0,
        "Duration (sec)": 45.30,
        "Start Time": startTime.toISOString(),
        "End Time": new Date(startTime.getTime() + 45300).toISOString()
    });

    const worksheet = XLSX.utils.json_to_sheet(data);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, "Test Report");

    const fileName = "Test_Report_BurnoutTracker.xlsx";
    XLSX.writeFile(workbook, fileName);
    console.log(`Generated mock test report: ${fileName}`);
}

generateMockExcelReport();
