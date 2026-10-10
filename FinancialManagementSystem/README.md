# StackUp — Redesigned UI Edition

A Java Swing interface rebuilt from the green-and-black StackUp reference screens: soft grey frame, a floating brand pill and account pill on top, a slim icon rail on the left (hover an icon for the page name), large rounded white cards, pill buttons and inputs, striped revenue/profit bars, doughnut charts with amount legends, status chips and avatar initials. The dashboard chart stacks expenses (black) with profit (striped) per month. Financial data starts empty. The backend preserves validation, permissions, accounts, persistence, reports and payments. Executable screenshots (empty data) are in previews.

Reference elements with no backing feature were left out rather than faked: the light/dark theme switch, the notes field on Quick Entry, the sign-in entries and "last active" times in the activity log, and the monthly expense limit (replaced by expenses vs. revenue for the month). Export buttons write CSV; Download PDF on the report writes a simple text PDF with amounts as "PHP".
## Run in VS Code

1. Extract the ZIP and open StackUpMockupEdition as the project folder.
2. Use JDK 17+ and Extension Pack for Java.
3. Open src/Main.java and click Run, or run .\run.bat in the terminal.
4. On macOS/Linux use bash run.sh.
5. First launch creates your Admin account. Create Employee accounts in Accounts. Financial records and payments start empty.

## Roles

Admin: Dashboard, Reminders, Transaction History (filter/edit/delete), Financial Report, Financial Insights, Activity Log, Accounts, Logout.
Employee: Dashboard, Reminders, Financial Record, Transaction History (filter only), Payments (schedule/mark paid), Logout.

Only one menu page appears at a time. Reopening the current page shows an already-open warning. Passwords are hashed; the reset panel resets an employee password rather than revealing the original. All charts read actual records, with zero totals and no fabricated bars before data entry.

## Source layout

src/frontend holds one file per screen (the page classes), PageSupport (shared table and card helpers), ChartPanel, DatePicker, Pager and Dialogs. src/frontend/style holds only the look: Theme (colors, fonts, rounded panels, chips), Icons and the three Skin classes that round the buttons, drop-downs and scroll bars; none of it changes what the app does. src/backend contains data classes and FinancialService. Main.java starts the app. FRONTEND_CODE.md and BACKEND_CODE.md collect the complete code for review.


## Validation

Java 17 compilation passed; all 20 frontend workflow checks, backend tests, chart-data tests and account reset tests passed. Tests use temporary data; no sample records or accounts are bundled.
