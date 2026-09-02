# 💰 FinFlow — Personal Finance App

**FinFlow** is a modern native Android application designed to help users manage their personal finances, track income and expenses, organize transactions, monitor scheduled payments, and understand their financial activity through clear statistics and visual summaries.

The application is built with **Kotlin and Jetpack Compose** and follows a layered architecture that separates UI, domain, and data responsibilities.

> 🚧 **Project status:** Active development
> The current version provides the core personal-finance management experience, with additional functionality planned for future releases.

---

## ✨ Features

### 🏠 Financial Dashboard

The Home screen provides a quick overview of the user's financial situation.

* Current available balance
* Total income
* Total expenses
* Net financial change
* Account selection
* Recent transactions
* Upcoming scheduled payments
* Dynamic time-based greeting
* Quick access to transaction details

The greeting is automatically determined from the device's local time:

| Time          | Greeting       |
| ------------- | -------------- |
| 05:00 – 11:59 | Good morning   |
| 12:00 – 17:59 | Good afternoon |
| 18:00 – 04:59 | Good night     |

---

### 💳 Account Management

FinFlow supports multiple financial accounts/wallets.

Users can:

* Create accounts
* Select the active account
* Define account name
* Select account currency
* Set an initial balance
* Assign a custom account color
* Manage existing accounts
* View account-specific financial information

The current account model is primarily designed around **cash/wallet accounts**, with the architecture allowing additional account types to be introduced later.

---

### 💸 Transaction Management

The application provides a complete workflow for recording and managing financial transactions.

Supported transaction types:

* **Income**
* **Expense**

Each transaction can contain:

* Transaction type
* Amount
* Currency
* Category
* Account
* Date
* Note
* Optional attachment URI
* Creation timestamp
* Last update timestamp

Transactions are linked to their corresponding account and category, allowing the application to present richer transaction information throughout the UI.

---

### 🔎 Transaction Search & Filtering

The Transactions screen provides tools for finding and reviewing financial activity.

Available functionality includes:

* Search transactions
* Filter by transaction type
* View all transactions
* View income transactions
* View expense transactions
* Open transaction details
* Edit transactions
* Delete transactions

Transaction filters are represented by the following categories:

* All
* Income
* Expense

---

### 📊 Financial Statistics

FinFlow includes a dedicated Statistics screen for analyzing financial activity.

The statistics layer supports:

* Income vs. expense analysis
* Spending by category
* Transaction counts
* Period-based financial activity
* Financial summaries
* Visual data representation

Supported time periods include:

* Week
* Month
* Year
* Custom

Date filtering also supports:

* This month
* This week
* This year
* All time

This makes the application suitable not only for recording transactions but also for understanding spending behavior over time.

---

### 🗓️ Scheduled Payments

Users can create recurring or scheduled financial obligations.

Supported frequencies include:

* Daily
* Weekly
* Monthly
* Yearly

Scheduled payments contain information such as:

* Name
* Amount
* Currency
* Category
* Account
* Frequency
* Next payment date
* Note
* Active/inactive state

The Home screen surfaces upcoming payments so users can quickly see what financial obligations are approaching.

---

### 🗂️ Categories

Transactions can be organized using customizable categories.

Categories contain:

* Name
* Transaction type
* Icon
* Color
* Default/custom state

The project already defines visual category concepts such as:

* Food
* Transport
* Shopping
* Bills
* Health
* Entertainment
* Salary
* Freelance
* Business
* Gift
* Other

Categories are also used by the statistics system to calculate category-level spending.

---

### 🎨 Modern Material 3 UI

The interface is built entirely with **Jetpack Compose** and **Material 3**.

The UI uses:

* Material 3 components
* Custom reusable Compose components
* Rounded cards
* Financial summary cards
* Account selector sheets
* Modal dialogs
* Bottom navigation
* Custom category icons
* Dynamic colors and theming
* Responsive layouts
* Visual income/expense indicators

The Home screen uses a prominent financial overview card with balance, income and expense information presented together for quick comprehension.

---

### 🌍 Localization

The application includes localized Android resources for:

* English
* Russian
* Uzbek

Resource directories are separated using Android's standard localization mechanism.

---

## 🏗️ Architecture

FinFlow follows a layered architecture with clear separation between presentation, domain, and persistence responsibilities.

```text
┌───────────────────────────────────────────┐
│                  UI Layer                 │
│                                           │
│  Screens • Components • ViewModels • Theme│
└──────────────────────┬────────────────────┘
                       │
                       ▼
┌───────────────────────────────────────────┐
│               Domain Layer                │
│                                           │
│  Models • Business Logic • Helpers        │
└──────────────────────┬────────────────────┘
                       │
                       ▼
┌───────────────────────────────────────────┐
│                 Data Layer                │
│                                           │
│ Repository • DAO • Entities • Room DB     │
└───────────────────────────────────────────┘
```

### UI Layer

Located under:

```text
app/src/main/java/dev/egamberganov/finflow/ui/
```

The UI layer contains:

```text
ui/
├── components/
├── screens/
├── theme/
└── viewmodel/
```

#### Screens

```text
HomeScreen.kt
MenuScreen.kt
OnboardingScreen.kt
StatisticsScreen.kt
TransactionsScreen.kt
```

#### Reusable Components

```text
AccountSelectorSheet.kt
AddTransactionSheet.kt
AppBottomNavBar.kt
CategoryIcons.kt
ManageAccountsDialog.kt
ManageCategoriesDialog.kt
ManageScheduledPaymentsDialog.kt
SettingsDialogs.kt
TransactionDetailDialog.kt
```

This structure keeps large UI flows separated from reusable presentation components.

---

## 🧠 Domain Layer

The domain layer is located under:

```text
app/src/main/java/dev/egamberganov/finflow/domain/
```

It contains domain-level models and reusable business logic.

```text
domain/
├── model/
│   └── Models.kt
└── GreetingProvider.kt
```

### Financial Models

The domain layer provides higher-level models such as:

* `AccountWithBalance`
* `FinancialSummary`
* `CategorySpend`
* `PeriodActivity`

It also defines reusable enums for:

* Time periods
* Transaction filters
* Date ranges

A dedicated `CurrencyFormatter` is used to format monetary values and compact financial figures.

---

## 💾 Data Layer

The persistence layer is implemented using **Room Database**.

```text
data/
├── dao/
├── database/
├── entity/
└── repository/
```

### Database

The application uses a local Room database:

```text
personal_finance.db
```

The database currently contains five main entities:

```text
AccountEntity
CategoryEntity
TransactionEntity
ScheduledPaymentEntity
AppSettingsEntity
```

---

### Database Relationships

Transactions are connected to both accounts and categories.

```text
Account
   │
   ├───────────────┐
   │               │
   ▼               ▼
Transaction ──── Category
```

Scheduled payments use a similar relationship:

```text
Account
   │
   └──── ScheduledPayment ──── Category
```

Foreign keys are used to maintain referential integrity.

Deleting an account cascades to its related transactions/scheduled payments, while categories are protected from deletion when referenced.

---

## 🗃️ Repository

The main data-access abstraction is:

```text
FinanceRepository.kt
```

The repository coordinates interaction between the ViewModel and the Room DAOs.

Available DAO areas include:

```text
AccountDao.kt
CategoryDao.kt
TransactionDao.kt
ScheduledPaymentDao.kt
AppSettingsDao.kt
```

This provides a single higher-level entry point for the application's financial data operations.

---

## 🧩 ViewModel

The primary presentation/business-state coordinator is:

```text
FinanceViewModel.kt
```

The ViewModel works with:

* Accounts
* Categories
* Transactions
* Scheduled payments
* Application settings
* Financial summaries
* Statistics
* Filtering
* Account selection
* UI state

It uses Android's lifecycle-aware `ViewModel` and Kotlin coroutines for asynchronous operations.

---

## 🛠️ Tech Stack

### Core

| Technology      | Purpose                         |
| --------------- | ------------------------------- |
| Kotlin          | Primary programming language    |
| Android         | Application platform            |
| Jetpack Compose | Declarative UI                  |
| Material 3      | UI components and design system |
| AndroidX        | Android application framework   |

### Architecture & State

| Technology         | Purpose                      |
| ------------------ | ---------------------------- |
| ViewModel          | UI/business state management |
| Kotlin Coroutines  | Asynchronous operations      |
| Navigation Compose | Screen navigation            |

### Persistence

| Technology | Purpose                  |
| ---------- | ------------------------ |
| Room       | Local database           |
| SQLite     | Underlying local storage |

### Networking & Serialization

| Technology | Purpose                |
| ---------- | ---------------------- |
| Retrofit   | HTTP API communication |
| OkHttp     | Networking             |
| Moshi      | JSON serialization     |
| Coil       | Image loading          |

### AI & Google Services

The project includes Firebase AI integration and is prepared for Gemini API usage.

The repository contains an `.env.example` configuration describing the `GEMINI_API_KEY` environment variable.

Firebase App Check support is also included.

---

## 🧪 Testing

The project includes a dedicated testing setup for both local and instrumentation testing.

Testing technologies include:

* JUnit
* AndroidX Test
* Compose UI testing
* Espresso
* Robolectric
* Roborazzi
* Kotlin Coroutines Test

Roborazzi is also configured for screenshot-based UI testing.

The project includes:

```text
app/src/test/
app/src/androidTest/
```

This provides a foundation for testing both application logic and Compose UI behavior.

---

## 📁 Project Structure

```text
Personal-Finance-App/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── dev/
│   │   │   │       └── egamberganov/
│   │   │   │           └── finflow/
│   │   │   │               │
│   │   │   │               ├── data/
│   │   │   │               │   ├── dao/
│   │   │   │               │   ├── database/
│   │   │   │               │   ├── entity/
│   │   │   │               │   └── repository/
│   │   │   │               │
│   │   │   │               ├── domain/
│   │   │   │               │   ├── model/
│   │   │   │               │   └── GreetingProvider.kt
│   │   │   │               │
│   │   │   │               ├── ui/
│   │   │   │               │   ├── components/
│   │   │   │               │   ├── screens/
│   │   │   │               │   ├── theme/
│   │   │   │               │   └── viewmodel/
│   │   │   │               │
│   │   │   │               └── MainActivity.kt
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   ├── values/
│   │   │   │   ├── values-ru/
│   │   │   │   ├── values-uz/
│   │   │   │   └── xml/
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   ├── test/
│   │   └── androidTest/
│   │
│   └── build.gradle.kts
│
├── gradle/
├── .env.example
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── metadata.json
```

---

## ⚙️ Requirements

To build the project locally, you need:

* Android Studio with a recent Android SDK
* JDK 11+
* Android SDK 36
* Gradle wrapper included in the project
* An Android device or emulator running Android 7.0/API 24 or higher

The application is configured with:

```text
minSdk    = 24
targetSdk = 36
compileSdk = 36
```

---

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/egamberganovdev/Personal-Finance-App.git
```

```bash
cd Personal-Finance-App
```

### 2. Open the project

Open the project in Android Studio and allow Gradle to synchronize the project dependencies.

### 3. Configure environment variables

The repository contains:

```text
.env.example
```

Copy it to:

```text
.env
```

If Gemini API functionality is enabled, configure:

```env
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
```

Do not commit API keys or other secrets to the repository.

### 4. Build the project

Using Gradle:

```bash
./gradlew assembleDebug
```

On Windows:

```bash
gradlew.bat assembleDebug
```

### 5. Run the application

Run the `app` configuration from Android Studio on:

* A physical Android device
* An Android Emulator

---

## 🔐 Security

FinFlow is primarily designed around local financial-data storage using Room.

The project also integrates Google/Firebase services and Gemini AI functionality, therefore API credentials and service configuration should be handled through environment variables or secure secret-management mechanisms.

### Important

Never commit:

```text
.env
google-services.json
private API keys
keystores
signing credentials
```

to a public repository.

For production deployments, release signing credentials must be supplied securely through environment variables or CI/CD secret management.

---

## 💱 Supported Currencies

The application's currency formatter has explicit support for:

* USD — `$`
* EUR — `€`
* GBP — `£`
* RUB — `₽`
* KZT — `₸`

Other currency codes can also be displayed using a generic currency-code format.

The default account currency is:

```text
UZS
```

---

## 🎯 Design Goals

FinFlow is designed around several principles:

### Simplicity

Financial information should be understandable at a glance.

### Local-first data

Core financial records are persisted locally using Room.

### Clear financial context

Income, expenses, balances, transactions and upcoming payments are presented together.

### Reusable UI

Common interactions are implemented as reusable Compose components instead of duplicating UI logic.

### Separation of concerns

Data access, domain models and UI responsibilities are kept in separate layers.

### Extensibility

The current architecture provides a foundation for adding additional financial-management functionality without restructuring the entire application.

---

## 🔮 Future Improvements

Potential areas for future development include:

* Advanced budgeting
* Monthly spending limits
* Savings goals
* More advanced financial analytics
* More account types
* Improved recurring-payment automation
* Cloud synchronization
* Secure authentication
* Data export/import
* Backup and restore
* Expanded currency support
* More comprehensive financial reports
* Improved AI-powered financial insights
* Enhanced accessibility
* More extensive automated UI testing

---

## 📌 Current Limitations

The current implementation should be considered an evolving personal-finance application rather than a production banking system.

In particular:

* The application primarily uses local persistence.
* Account functionality currently focuses on cash/wallet-style accounts.
* Cloud synchronization is not the core storage mechanism.
* Financial calculations should not be treated as professional financial advice.
* Additional production-hardening and security review would be required before handling highly sensitive financial information at scale.

---

## 🤝 Contributing

Contributions are welcome.

If you would like to improve FinFlow:

1. Fork the repository.
2. Create a feature branch.

```bash
git checkout -b feature/my-feature
```

3. Make your changes.
4. Run the test suite.
5. Commit your changes.

```bash
git commit -m "Add my feature"
```

6. Push the branch.

```bash
git push origin feature/my-feature
```

7. Open a Pull Request.

When contributing, please try to preserve the existing separation between:

```text
UI
↓
ViewModel
↓
Repository
↓
DAO
↓
Room
```

---

## 📄 License

This project does not currently declare a dedicated open-source license in the repository.

If this project is intended for public reuse, distribution or external contributions, adding an explicit license is recommended.

---

## 👨‍💻 Author

Developed by **egamberganovdev**.

---

## 🔗 Repository

**GitHub:**
https://github.com/egamberganovdev/Personal-Finance-App

---

## ⭐ Project Overview

FinFlow is a Kotlin-based Android personal finance application focused on making everyday financial tracking simple and understandable.

It combines:

**Accounts + Transactions + Categories + Scheduled Payments + Statistics + Local Storage + Modern Compose UI**

into a single application designed to give users a clear picture of their personal finances.
