import pandas as pd

def generate_excel_report(test_results):
    data = []
    
    suite_names = {
        "mobile": "BurnOutTracker Android App - Full Appium E2E Workflow",
        "web": "BurnOutTracker Web App - Selenium E2E Workflow",
        "security": "BurnOutTracker Backend - Security and Vulnerability Tests"
    }
    
    for key, result in test_results.items():
        if result["total"] == 0:
            continue
            
        pass_rate = (result["passed"] / result["total"]) * 100 if result["total"] > 0 else 0
        
        data.append({
            "Test Suite": suite_names[key],
            "Total Tests": result["total"],
            "Passed": result["passed"],
            "Failed": result["failed"],
            "Pass Rate %": round(pass_rate, 2),
            "Duration (sec)": round(result["duration"], 2),
            "Start Time": result["start_time"],
            "End Time": result["end_time"]
        })
    
    if data:
        df = pd.DataFrame(data)
        file_name = "Test_Report_BurnoutTracker.xlsx"
        df.to_excel(file_name, index=False)
        print(f"\nGenerated test report: {file_name}")
    else:
        print("\nNo tests ran, skipping report generation.")
