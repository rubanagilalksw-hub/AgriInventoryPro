# Agri Inventory Pro
Desktop inventory system for a seed, fertilizer and pesticide store (Java Swing + SQLite/JDBC).
DSA used: **Queue (array-based, circular)**: group C requirement.

## Run (Windows)
1. Install a JDK 11 or newer (e.g. Temurin from adoptium.net).
2. Double-click **run.bat**. Wait a few seconds: the first run creates the database and sample data.

Mac/Linux: `./run.sh`

Login: `admin / admin123` (full access) or `staff / staff123` (limited).

## Requirements checklist
| Requirement | Where |
|---|---|
| Inventory system | Products, Farmers, Suppliers, Stock, Seasons |
| Queue (enqueue, dequeue, peek, rear, size, isEmpty, isFull) | `OrderQueue.java`, page **Order Queue** |
| CRUD | Products / Farmers / Suppliers pages (Create, Read, Update, Delete) + Users in Settings |
| Data storage | Saved automatically in a local data file (`data/agri.db`, created on first run) |

## Unique features
- **Smart Reorder Advisor**: predicts stock-outs from sales history (SQL) and suggests order quantities.
- **Farmer Credit Risk score**.
- **Array Inspector**: shows the real circular array with FRONT/REAR pointers.
- **Rush Hour Demo**: adds orders one by one to show priority insertion and Queue Overflow.
- **Queue Operations Log**: every queue operation is recorded.
- CSV export, bill printing, backup/restore, Light/Dark mode.

## Project structure
`src/agri/*.java` source, `lib/` SQLite JDBC driver, `run.bat` / `run.sh` launchers.
