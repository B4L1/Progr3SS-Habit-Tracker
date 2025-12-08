# Work In Progress Status (As of 2025-12-08)

The following features have been paused to focus on core specifications.

## 1. AI Icon Generation (Gemini Integration)
- **Status:** Partially Implemented / Unreliable.
- **Current Behavior:** 
    - API keys and calls to Gemini are functioning (logs show success).
    - Icons are not consistently persisting to `SharedPreferences` or are failing to load into the `RecyclerView`.
    - Workaround using `habit.description` to stash icon names is in place but not fully verified.
- **Action Item:** Resume debugging `IconManager` persistence and adapter binding logic later.

## 2. Create Schedule
- **Status:** Broken (API Error 400).
- **Current Behavior:**
    - Submitting a new schedule returns a `400 Bad Request`.
    - Suspected causes: Invalid date/time format, incorrect field naming (camelCase vs snake_case) for `daysOfWeek` or `habitId`, or validation rules on `notes`.
- **Action Item:** Requires strict audit of JSON payload against backend expectation.

## 3. Habit Management
- **Status:** Limitation Identified.
- **Current Behavior:** Backend `DELETE /habit/{id}` returns 404, implying deletion is not supported or the endpoint is different.
- **Action Item:** Confirm backend API specification for habit deletion.
