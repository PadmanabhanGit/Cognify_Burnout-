import pytest
from datetime import datetime

# Global dictionary to hold test results
test_results = {
    "mobile": {"total": 0, "passed": 0, "failed": 0, "duration": 0.0, "start_time": None, "end_time": None},
    "web": {"total": 0, "passed": 0, "failed": 0, "duration": 0.0, "start_time": None, "end_time": None},
    "security": {"total": 0, "passed": 0, "failed": 0, "duration": 0.0, "start_time": None, "end_time": None},
}

@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    # Execute all other hooks to obtain the report object
    outcome = yield
    rep = outcome.get_result()
    
    # We only look at actual test calls, not setup/teardown
    if rep.when == "call":
        suite = "mobile" if "mobile" in item.nodeid else "web" if "web" in item.nodeid else "security"
        
        test_results[suite]["total"] += 1
        if rep.passed:
            test_results[suite]["passed"] += 1
        else:
            test_results[suite]["failed"] += 1
        
        test_results[suite]["duration"] += rep.duration

def pytest_sessionstart(session):
    for suite in test_results:
        test_results[suite]["start_time"] = datetime.now().isoformat() + "Z"

def pytest_sessionfinish(session, exitstatus):
    for suite in test_results:
        test_results[suite]["end_time"] = datetime.now().isoformat() + "Z"
    
    # Generate excel report
    from reporter import generate_excel_report
    generate_excel_report(test_results)
