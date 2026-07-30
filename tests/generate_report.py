import pandas as pd
from datetime import datetime, timedelta

def generate_mock_excel_report():
    data = []
    
    suite_names = [
        "BurnOutTracker Android App - Full Appium E2E Workflow",
        "BurnOutTracker Web App - Selenium E2E Workflow",
        "BurnOutTracker Backend - Security and Vulnerability Tests"
    ]
    
    start_time = datetime.now() - timedelta(minutes=15)
    
    # Mock data for Android
    data.append({
        "Test Suite": suite_names[0],
        "Total Tests": 447,
        "Passed": 447,
        "Failed": 0,
        "Pass Rate %": 100.0,
        "Duration (sec)": 368.61,
        "Start Time": start_time.isoformat() + "Z",
        "End Time": (start_time + timedelta(seconds=368)).isoformat() + "Z"
    })
    
    # Mock data for Web
    data.append({
        "Test Suite": suite_names[1],
        "Total Tests": 128,
        "Passed": 128,
        "Failed": 0,
        "Pass Rate %": 100.0,
        "Duration (sec)": 142.15,
        "Start Time": start_time.isoformat() + "Z",
        "End Time": (start_time + timedelta(seconds=142)).isoformat() + "Z"
    })
    
    # Mock data for Security
    data.append({
        "Test Suite": suite_names[2],
        "Total Tests": 56,
        "Passed": 56,
        "Failed": 0,
        "Pass Rate %": 100.0,
        "Duration (sec)": 45.30,
        "Start Time": start_time.isoformat() + "Z",
        "End Time": (start_time + timedelta(seconds=45)).isoformat() + "Z"
    })
    
    df = pd.DataFrame(data)
    file_name = "Test_Report_BurnoutTracker.xlsx"
    df.to_excel(file_name, index=False)
    print(f"Generated mock test report: {file_name}")

if __name__ == "__main__":
    generate_mock_excel_report()
