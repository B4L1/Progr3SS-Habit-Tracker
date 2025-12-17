# The Logic Behind "Progr3SS-Habit-Tracker"

This document explains the inner workings of the Android application, demystifying the architecture, workflows, and specific design patterns like **Factories** and **Singletons**.

## 1. Project Architecture: The Big Picture

The app follows a modern Android architecture pattern, separating **User Interface (UI)** from **Data Operations**.

### Key Components:
-   **Fragments (`ui/`)**: These are the screens you see (e.g., `LoginFragment`, `HomeFragment`). They handle user interactions but don't store data permanently.
-   **Retrofit Client (`data/remote/`)**: This is the "bridge" to the backend server. It manages network requests.
-   **Services (`AuthService`, `HabitService`)**: Specific "workers" that define what we can ask the server (e.g., "Log me in", "Create a habit").
-   **Data Models (`data/model/`)**: Blueprints that define how data looks (e.g., `User`, `Habit`).

---

## 2. Secrets of the UI (User Interface)

### Workflow A: Registration (Sign Up)
**File**: `ui/auth/RegisterFragment.kt`

1.  **Validation**:
    -   User enters Name, Email, Password, Confirm Password.
    -   We check locally: Are fields empty? Do passwords match? If not, show errors on the `InputLayout`.
2.  **API Request**:
    -   We wrap the data into `RequestBody` objects (text/plain).
    -   Call `RetrofitClient.authService.register(name, email, password)`.
3.  **Success Handling**:
    -   The server creates the user and immediately logs them in, returning an Access Token.
    -   We save this token (`TokenManager.saveAccessToken`).
    -   The user is navigated directly to the Home screen (`action_registerFragment_to_homeFragment`).

### Workflow B: Logging In
**File**: `ui/auth/LoginFragment.kt`

1.  **Input**: User enters credentials.
2.  **Token Check**:
    -   `TokenManager` is checked for an existing Refresh Token on startup. If valid, login is skipped.
3.  **Network Call**:
    -   `RetrofitClient.authService.login(SignInDto)` sends the data.
4.  **Response**:
    -   **200 OK**: Tokens are saved. Navigation proceeds to Home.
    -   **401/400 Error**: User is notified via Toast to retry.

### Workflow C: Creating a Schedule & Habit
**File**: `ui/schedule/CreateScheduleFragment.kt` & `CreateHabitFragment.kt`

This workflow involves two potential paths: selecting an existing habit or creating a new one with AI.

#### 1. Creating a New Habit (AI Powered)
-   **Input**: User types "Read a book".
-   **AI Processing**:
    -   The app sends "Read a book" to the **Gemini 2.5 Flash** API via `GeminiService`.
    -   The prompt asks Gemini to select the best icon from a pre-defined list (e.g., `ic_activity_book`).
-   **Completion**:
    -   The habit is saved to the backend.
    -   We use `setFragmentResult` to notify the previous screen (`CreateScheduleFragment`) that a new habit was created.

#### 2. scheduling the Habit
-   **Selection**: User picks the habit (or it's auto-selected from the step above).
-   **Timing**:
    -   **Time**: Selected via a `TimePickerDialog`. stored as ISO 8601 UTC (e.g., `2023-10-27T08:00:00Z`).
    -   **Date**: Formatted as `YYYY-MM-DD`.
-   **Recurrence**:
    -   **Custom**: Uses `CreateCustomScheduleDto` for a single/specific setup.
    -   **Recurring**: Uses `CreateRecurringScheduleDto`. Logic maps "Weekdays" to `[1,2,3,4,5]` (Monday-Friday).

### Workflow D: Profile & Progress Tracking
**File**: `ui/profile/ProfileFragment.kt`

1.  **Data Fetching**:
    -   **Profile**: Fetches User Name, Email, and Avatar URL.
    -   **Habits**: Fetches all habits belonging to the user.
    -   **Today's Schedule**: Fetches schedules specifically for *today*.
2.  **Progress Calculation**:
    -   We map the schedules to their respective habits.
    -   **Logic**:
        -   If `status == "Completed"`, progress is 100%.
        -   Otherwise, we sum the `logged_time` from the sub-progress items.
    -   **Visuals**:
        -   The `ProgressBar` is updated.
        -   If no schedule exists for today, the bar is set to 0% opacity (grayed out) to indicate "Nothing due today".
3.  **Image Loading**:
    -   We use the **Coil** library to load profile images.
    -   Since the backend might run on `localhost`, we have logic to swap `localhost` for `10.0.2.2` (emulator) or the specific IP `10.137.157.147` to ensure the image loads on the device.

---

## 4. UI Implementation Details

### Fragments vs Activities
-   **Single Activity Architecture**: We use one `MainActivity` as the container.
-   **Fragments**: specific screens (`Login`, `Home`, `Profile`) are swapped into the container.
-   **Navigation**: `nav_graph.xml` handles the routing and argument passing between fragments.

### UI XML Layouts
-   **ConstraintLayout**: Used for complex positioning (e.g., "Pin this button to the bottom right of that text view").
-   **InputLayout**: Wraps `EditText` to provide built-in error handling (`binding.emailInputLayout.error = "Required"`).
-   **ViewBinding**:
    -   Eliminates manual `findViewById`.
    -   References are type-safe (no casting errors).


---

## 5. Design Patterns: Factories & Singletons

You asked about "Factories". In this specific codebase, we don't use a highly complex `ViewModelFactory` structure (common in larger apps with Dependency Injection), but we DO use the **Factory Pattern** and **Singleton Pattern** heavily in networking.

### The Singleton: `RetrofitClient`
**File**: `data/remote/RetrofitClient.kt`

A **Singleton** ensures a class has only one instance and provides a global point of access to it.
-   **Why?**: We don't want to create a new "Network Connection" object 50 times. We want *one* central manager that handles cookies, headers, and logs.
-   **Code**: `object RetrofitClient { ... }`. The `object` keyword in Kotlin automatically makes it a Singleton.

### Where is the "Repository"?
You asked about a `repository` folder.
-   **Standard Architecture**: `Fragment` -> `ViewModel` -> `Repository` -> `Retrofit/Database`.
-   **Our Architecture**: `Fragment` -> `Retrofit Service`.
-   **Why it's missing**: Since we skipped `ViewModels` to keep things simple, we also skipped the `Repository`.
-   **Effect**: Our "Services" (`AuthService`, `HabitService`) effectively act as the data layer. If we added a Repository, it would just be a middleman passing data from the Service to the Fragment. In this size of app, it was deemed unnecessary overhead.

### The Factory: `GsonConverterFactory` & `createService`
A **Factory** is an object used for creating other objects.

1.  **`GsonConverterFactory`**:
    -   When we build Retrofit, we say `.addConverterFactory(GsonConverterFactory.create())`.
    -   **Magic**: This factory produces a "Converter" that automatically turns JSON text `{ "name": "Steve" }` into a detailed Kotlin object `User(name="Steve")`.

2.  **`createService` (Our Custom Factory Method)**:
    -   Inside `RetrofitClient`, we have:
        ```kotlin
        fun <T> createService(serviceClass: Class<T>): T {
            return instance.create(serviceClass)
        }
        ```
    -   **Usage**: `RetrofitClient.createService(HabitService::class.java)`
    -   **Explanation**: This method acts as a factory. You give it a "Blueprint" (the Interface file `HabitService`), and it manufactures a working "Object" that knows how to make those specific network calls.

---

## 6. Summary of "Magic" Files

| File | Purpose |
| :--- | :--- |
| `Manifest.xml` | The ID card of the app. Tells Android "I need Internet permissions". |
| `Gradle` | The "Chef". Collects ingredients (libraries) and builds the app. |
| `RetrofitClient` | The "Messenger". Manages server communication. |
| `Fragments` | The "Controllers". In this app, they handle both UI and Data Logic. |

### A Note on ViewModels
You might notice we do **NOT** use `ViewModels` in this codebase.
-   **Standard Android Architecture**: Usually recommends `ViewModel` to hold data so it survives screen rotation.
-   **This App's Approach**: to keep the code simple for learning/lab purposes, we handle network calls directly inside the `Fragment` (e.g., inside `onViewCreated` or button listeners).
-   **Trade-off**: If you rotate the screen while loading, the request might be cancelled or run again. In a production app, we would move the `RetrofitClient` calls into a `ViewModel`.

#### Is this a Safety Concern?
-   **Security**: **NO**. Your data is just as safe. The HTTPS encryption and Token handling (`AuthInterceptor`) work exactly the same way.
-   **Stability/Bugs (The real risk)**: **YES**. Without ViewModels, rotating the phone destroys the Fragment and rebuilds it.
    -   *Risk*: A network call finishes, tries to update a text view, but the text view no longer exists -> **Crash**.
    -   *Risk*: You lose the "Loading..." state and the user clicks the button twice -> **Double Billing/Duplicate Entries**.
    -   *Architecture*: This app essentially uses an **MVC** (Model-View-Controller) pattern where the Fragment acts as the Controller, rather than the recommended **MVVM** (Model-View-ViewModel).
    -   **The Fix We Used**: We added `android:screenOrientation="portrait"` to the `AndroidManifest.xml`. This **locks** the app in standing mode, preventing the user from rotating it and triggering these specific crashes. It is a valid "Band-Aid" for simpler apps.

---

## 7. Chain of Command: Real World Examples

Here is exactly how data flows for the specific workflows you requested: `Fragment` -> `Service` -> `Retrofit/Gson` -> `OkHttp` -> `Backend` -> `UI`.

### Example 1: User Logs In (The Deep Dive)
**Goal**: User types email/password and clicks "Login".
1.  **User Action**: Clicks Button inside `LoginFragment`.
2.  **Fragment Construction**:
    -   Creates a `SignInDto(email="...", password="...")`.
    -   Calls `RetrofitClient.authService.login(signInDto)`.
3.  **Retrofit & Gson (The Translation w/ `GsonConverterFactory`)**:
    -   Retrofit takes your Kotlin object `SignInDto` and asks Gson to convert it.
    -   **Result**: A JSON string byte stream: `{"email":"test@test.com", "password":"123"}`.
4.  **OkHttp (The Delivery Truck)**:
    -   Retrofit gives this JSON to **OkHttp**, the underlying networking engine.
    -   OkHttp opens a TCP connection to `10.137.157.147` on port `8080`.
    -   It sends a `POST` request with headers `Content-Type: application/json`.
5.  **Backend Response**:
    -   Server processes it and sends back JSON: `{"accessToken": "abc...", "refreshToken": "xyz..."}`.
6.  **Retrofit (The Receiver)**:
    -   Retrofit sees the response. It runs Gson *in reverse*.
    -   JSON `{...}` becomes Kotlin object `AuthResponseDto`.
7.  **Fragment (OnResponse)**:
    -   The callback receives the fully formed Kotlin object.
    -   It extracts `accessToken`, saves it to the `TokenManager`, and navigates.

### Example 2: Loading Schedules (The Interceptor Magic)
**Goal**: `HomeFragment` opens and shows today's tasks.
1.  **Fragment LifeCycle**: `HomeFragment.onViewCreated()` triggers `fetchSchedules()`.
2.  **Service Call**: `RetrofitClient.scheduleService.getSchedules("2023-12-09")`.
3.  **The Interceptor Chain (`AuthInterceptor`)**:
    -   Before Retrofit hands the request to the internet, our custom **`AuthInterceptor`** pauses it.
    -   **Logic**: "Does the TokenManager have a token? Yes."
    -   **Action**: It injects a header: `Authorization: Bearer <YourToken>`.
    -   *If it didn't do this, the server would reply "401 Unauthorized".*
4.  **Serialization**:
    -   Retrofit sends the `GET` request. (No body this time, just URL parameters).
5.  **Deserialization**:
    -   The server sends a JSON Array `[...]`.
    -   Gson converts this into a `List<ScheduleResponseDto>`.
6.  **Fragment**:
    -   Receives the list and hands it to the `ScheduleAdapter` to draw the items on screen.

### Example 3: Edit Profile
**Goal**: User updates their username and uploads a new photo.
1.  **User Action**: User edits text and selects a photo in `EditProfileFragment`, then clicks "Save".
2.  **Fragment (Step 1)**: Calls `RetrofitClient.authService.updateProfile(username="NewName")`.
    -   *Wait for success...*
3.  **Fragment (Step 2)**: Calls `RetrofitClient.authService.uploadProfileImage(imageFile)`.
    -   This uses `MultipartBody` to send the binary image data.
4.  **Backend Response**: Returns the updated profile object with the new Image URL.
5.  **Fragment (OnResponse)**: 
    -   Shows "Success" Toast.
    -   Navigates back to `ProfileFragment`.

### Example 4: Add New Habit (With AI)
**Goal**: User types "Yoga" and saves.
1.  **User Action**: Clicks "Save" in `CreateHabitFragment`.
2.  **Fragment (Step 1 - AI)**:
    -   **Code Location**: `CreateHabitFragment.kt`, line ~189.
    -   **The Prompt**:
        ```kotlin
        val prompt = "Based on the activity '$name' ($description), pick the single best matching icon from this list: ${iconList.joinToString(", ")}. Return ONLY the icon name (e.g., ic_activity_run). If unsure, return ic_activity_generic."
        ```
    -   Calls `GeminiService.getIconSuggestion()`.
    -   **Gemini API**: Returns text `"ic_activity_yoga"`.
3.  **Fragment (Step 2 - Backend)**:
    -   Constructs `CreateHabitDto(name="Yoga", icon="ic_activity_yoga")`.
    -   Calls `RetrofitClient.habitService.createHabit(dto)`.
4.  **Backend Response**: Confirms habit creation.
5.  **Fragment (OnResponse)**:
    -   Saves the icon name locally to `IconManager` (so the app remembers which icon belongs to ID #55).
    -   Closes the screen.
