package com.pluralsight;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FinancialTracker {
    private static ArrayList<Transaction> transactions = new ArrayList<>();
    private static final String FILE_NAME = "transactions.csv";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        loadTransactions();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\nWelcome to TransactionApp");
            System.out.println("Choose an option:");
            System.out.println("D) Add Deposit");
            System.out.println("P) Make Payment (Debit)");
            System.out.println("L) Ledger");
            System.out.println("X) Exit");
            System.out.print("> ");

            String input = scanner.nextLine().trim().toUpperCase();
            switch (input) {
                case "D": addTransaction(scanner, true); break;
                case "P": addTransaction(scanner, false); break;
                case "L": ledgerMenu(scanner); break;
                case "X": running = false; break;
                default: System.out.println("Invalid option."); break;
            }
        }

        scanner.close();
    }

    private static void loadTransactions() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) {
                System.out.println("Error creating transactions file.");
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            transactions.clear();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 5) {
                    LocalDateTime dateTime = LocalDateTime.parse(parts[0] + " " + parts[1], DATE_TIME_FORMATTER);
                    transactions.add(new Transaction(dateTime, parts[2], parts[3], Double.parseDouble(parts[4])));
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to read transactions.");
        }
    }

    private static void saveTransactions() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Transaction t : transactions) {
                writer.write(t.toCSVLine());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving transactions.");
        }
    }

    private static void addTransaction(Scanner scanner, boolean isDeposit) {
        try {
            System.out.print("Enter date and time (yyyy-MM-dd HH:mm:ss): ");
            LocalDateTime dateTime = LocalDateTime.parse(scanner.nextLine(), DATE_TIME_FORMATTER);
            System.out.print("Description: ");
            String description = scanner.nextLine();
            System.out.print("Vendor: ");
            String vendor = scanner.nextLine();
            System.out.print("Amount: ");
            double amount = Double.parseDouble(scanner.nextLine());
            if (!isDeposit) amount *= -1;

            transactions.add(new Transaction(dateTime, description, vendor, amount));
            saveTransactions();
            System.out.println((isDeposit ? "Deposit" : "Payment") + " added.");

        } catch (Exception e) {
            System.out.println("Invalid input. Transaction not added.");
        }
    }

    private static void ledgerMenu(Scanner scanner) {
        boolean running = true;
        while (running) {
            System.out.println("\nLedger Menu");
            System.out.println("A) All");
            System.out.println("D) Deposits");
            System.out.println("P) Payments");
            System.out.println("R) Reports");
            System.out.println("H) Home");
            System.out.print("> ");

            String input = scanner.nextLine().trim().toUpperCase();
            switch (input) {
                case "A": displayTransactions(t -> true); break;
                case "D": displayTransactions(t -> t.amount > 0); break;
                case "P": displayTransactions(t -> t.amount < 0); break;
                case "R": reportsMenu(scanner); break;
                case "H": running = false; break;
                default: System.out.println("Invalid option."); break;
            }
        }
    }

    private static void displayTransactions(java.util.function.Predicate<Transaction> filter) {
        transactions.stream()
                .filter(filter)
                .sorted(Comparator.comparing((Transaction t) -> t.dateTime).reversed())
                .forEach(System.out::println);
    }

    private static void reportsMenu(Scanner scanner) {
        boolean running = true;
        while (running) {
            System.out.println("\nReports");
            System.out.println("1) Month To Date");
            System.out.println("2) Previous Month");
            System.out.println("3) Year To Date");
            System.out.println("4) Previous Year");
            System.out.println("5) Search by Vendor");
            System.out.println("0) Back");
            System.out.print("> ");

            switch (scanner.nextLine()) {
                case "1": filterByDate(LocalDate.now().withDayOfMonth(1), LocalDate.now()); break;
                case "2": {
                    LocalDate first = LocalDate.now().minusMonths(1).withDayOfMonth(1);
                    LocalDate last = first.withDayOfMonth(first.lengthOfMonth());
                    filterByDate(first, last);
                    break;
                }
                case "3": filterByDate(LocalDate.now().withDayOfYear(1), LocalDate.now()); break;
                case "4": {
                    LocalDate first = LocalDate.now().minusYears(1).withDayOfYear(1);
                    LocalDate last = first.withDayOfYear(first.lengthOfYear());
                    filterByDate(first, last);
                    break;
                }
                case "5":
                    System.out.print("Enter vendor name: ");
                    String vendor = scanner.nextLine().trim().toLowerCase();
                    displayTransactions(t -> t.vendor.toLowerCase().contains(vendor));
                    break;
                case "0": running = false; break;
                default: System.out.println("Invalid option."); break;
            }
        }
    }

    private static void filterByDate(LocalDate start, LocalDate end) {
        displayTransactions(t -> {
            LocalDate date = t.dateTime.toLocalDate();
            return !date.isBefore(start) && !date.isAfter(end);
        });
    }

    static class Transaction {
        LocalDateTime dateTime;
        String description, vendor;
        double amount;

        Transaction(LocalDateTime dt, String desc, String vend, double amt) {
            this.dateTime = dt;
            this.description = desc;
            this.vendor = vend;
            this.amount = amt;
        }

        public String toCSVLine() {
            return String.format("%s|%s|%s|%s|%.2f",
                    dateTime.toLocalDate(), dateTime.toLocalTime(), description, vendor, amount);
        }

        public String toString() {
            return String.format("%s %s | %-20s | %-10s | %10.2f",
                    dateTime.toLocalDate(), dateTime.toLocalTime(), description, vendor, amount);
        }
    }
}
