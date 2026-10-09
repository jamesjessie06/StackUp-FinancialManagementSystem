# StackUp — Redesigned UI Edition

A Java Swing interface adapted from the approved teal reference: gray outer frame, slim white icon rail, top navigation tabs, rounded white cards, a dashboard card grid and teal highlights. StackUp menus replace the reference's wallet and banking features. The monthly cash-flow chart stacks actual revenue and expenses; their combined bar height represents transaction activity, not profit. Financial data starts empty. The backend preserves validation, permissions, accounts, persistence, reports and payments. Actual executable screenshots are included in previews.
## Run in VS Code

1. Extract the ZIP and open StackUpMockupEdition as the project folder.
2. Use JDK 17+ and Extension Pack for Java.
3. Open src/Main.java and click Run, or run .\run.bat in the terminal.
4. On macOS/Linux use bash run.sh.
5. First launch creates your Admin account. Create Employee accounts in Accounts. Financial records and payments start empty.

## Roles

Admin: Dashboard, Transaction History (filter/edit/delete), Financial Report, Financial Insights, Activity Log, Accounts, Logout.
Employee: Dashboard, Financial Record, Transaction History (filter only), Payments (schedule/mark paid), Logout.

Only one menu page appears at a time. Reopening the current page shows an already-open warning. Passwords are hashed; the reset panel resets an employee password rather than revealing the original. All charts read actual records, with zero totals and no fabricated bars before data entry.

## Source layout

src/frontend contains newly written Swing screens, Theme, DatePicker, ChartPanel and dialogs. src/backend contains data classes and FinancialService. Main.java starts the app. FRONTEND_CODE.md and BACKEND_CODE.md collect the complete code for review.


## Validation

Java 17 compilation passed; all 20 frontend workflow checks, backend tests, chart-data tests and account reset tests passed. Tests use temporary data; no sample records or accounts are bundled.
