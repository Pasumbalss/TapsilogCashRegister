
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * TapsilogCashRegister
 * 
 * Cashier system for Tapsilog orders na may signup, login, ordering, transaction logging.
 * 
 * // Note: Pinagsama ko mga comments na casual Taglish at technical para mas madaling balikan.
 * // Lahat ng input may error handling at may option to cancel (by typing 0).
 * // File handling for transaction logging, ArrayList for all main dynamic data.
 */
public class TapsilogCashRegister {

    // --- USER DATA ---
    // Gumagamit ng ArrayList para flexible. Both username and password list dapat laging magkaparehas ng index.
    private final List<String> usernames = new ArrayList<>();
    private final List<String> passwords = new ArrayList<>();
    private final Scanner scanner = new Scanner(System.in);
    private String loggedInUser = null;

    // --- MENU DATA ---
    // Menu arrays para mabilis i-loop at i-edit. Indexing is key.
    private final String[] foodNames = {"Tapsilog", "Tosilog", "Spamsilog", "Hungariansilog"};
    private final double[] foodPrices = {80.00, 80.00, 80.00, 95.00};
    private final String[] addonNames = {"Rice", "Half Rice", "Java Rice", "None"};
    private final double[] addonPrices = {10.00, 7.00, 12.00, 0.00};

    // --- ORDER DATA (per transaction) ---
    // Gumagamit ng parallel ArrayLists (same index = same item). Pwede i-refactor to class pero for now, simple lang.
    private final List<String> orderedItems = new ArrayList<>();
    private final List<Integer> itemQuantities = new ArrayList<>();
    private final List<Double> itemPrices = new ArrayList<>();
    private final List<String> itemAddons = new ArrayList<>();

    // --- TRANSACTION ID TRACKING ---
    // Para tuloy-tuloy yung numbering, binabasa yung huling transaction ID from file each run.
    private int transactionCounter = 1;

    // Basahin transaction.txt para malaman anong ID na dapat gamitin (last + 1).
    private void loadTransactionCounter() {
        File file = new File("transactions.txt");
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lastId = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Transaction ID:")) {
                    try {
                        lastId = Integer.parseInt(line.replace("Transaction ID:", "").trim());
                    } catch (Exception e) {
                        System.out.println("Error reading transaction ID: " + e.getMessage());
                    }
                }
            }
            transactionCounter = lastId + 1;
        } catch (IOException e) {
            System.out.println("Error loading transaction counter: " + e.getMessage());
        }
    }

    // After checkout, log all details sa file (plus backup). File append mode para hindi nabubura old records.
    private void logTransactionToFile(double totalAmount) {
        StringBuilder transactionData = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dateTime = LocalDateTime.now().format(formatter);

        transactionData.append("Transaction ID: ").append(transactionCounter++).append("\n");
        transactionData.append("Date & Time: ").append(dateTime).append("\n");
        transactionData.append("Cashier: ").append(loggedInUser).append("\n");
        transactionData.append("Items Purchased:\n");
        for (int i = 0; i < orderedItems.size(); i++) {
            transactionData.append("  - ").append(orderedItems.get(i))
                    .append(" x").append(itemQuantities.get(i))
                    .append(" (").append(itemAddons.get(i)).append(")")
                    .append(" - $").append(String.format("%.2f", itemPrices.get(i))).append("\n");
        }
        transactionData.append("Total Amount: $").append(String.format("%.2f", totalAmount)).append("\n");
        transactionData.append("=============================================\n");

        // Main file logging
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("transactions.txt", true))) {
            writer.write(transactionData.toString());
        } catch (IOException e) {
            System.out.println("Problem writing transaction: " + e.getMessage());
        } finally {
            // Backup just in case
            try (BufferedWriter backupWriter = new BufferedWriter(new FileWriter("transactions_backup.txt", true))) {
                backupWriter.write(transactionData.toString());
            } catch (IOException e) {
                System.out.println("Problem with backup: " + e.getMessage());
            }
        }
    }

    // --- MAIN PROGRAM FLOW ---
    // 1. Load transaction counter
    // 2. Loop for login/signup until user is authenticated
    // 3. Order menu loop until user wants to exit
    private void start() {
        loadTransactionCounter();

        // Authentication loop
        while (loggedInUser == null) {
            showWelcomeScreen();
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("No input. Please try again.");
                continue;
            }
            char choice = input.charAt(0);
            switch (choice) {
                case '1':
                    if (!performSignup()) continue;
                    break;
                case '2':
                    if (!performLogin()) continue;
                    break;
                case '3':
                    System.out.println("\nExiting Wansilog. Thank you!");
                    return;
                default:
                    System.out.println("\nInvalid choice. Please enter 1, 2, 3, or 0 to cancel.");
                    break;
            }
        }

        // Order loop
        boolean isStillOrdering = true;
        while (isStillOrdering) {
            showOrderMenu();
            System.out.print("\nDo you want to process another order? (yes/no): ");
            String anotherOrder = scanner.nextLine();
            if (anotherOrder.equalsIgnoreCase("no")) {
                isStillOrdering = false;
            } else {
                clearCurrentOrder();
            }
        }
        System.out.println("\nThank you for using the Wansilog Cash Register, " + loggedInUser + "!");
    }

    // --- WELCOME SCREEN ---
    private void showWelcomeScreen() {
        System.out.println("\n===============================");
        System.out.println("   WELCOME TO WANSILOG!!");
        System.out.println("===============================");
        System.out.println("1. Sign Up");
        System.out.println("2. Log In");
        System.out.println("3. Exit");
        System.out.print("Enter your choice: ");
    }

    // --- SIGNUP FLOW ---
    // Username and password validation using regex. Option to cancel anytime by entering 0.
    private boolean performSignup() {
        System.out.println("\n===============================");
        System.out.println("       User Sign Up");
        System.out.println("===============================");

        String newUsername;
        String newPassword;

        // Username input/validation
        while (true) {
            System.out.print("Enter new username or 0 to cancel: ");
            newUsername = scanner.nextLine();
            if (newUsername.equals("0")) {
                System.out.println("Signup cancelled.");
                return false;
            }
            if (usernames.contains(newUsername)) {
                System.out.println("Username already taken. Please choose another.");
            } else {
                break;
            }
        }

        // Password input/validation
        while (true) {
            System.out.print("Enter new password (at least one uppercase, one number, 8-20 characters) or 0 to cancel: ");
            newPassword = scanner.nextLine();
            if (newPassword.equals("0")) {
                System.out.println("Signup cancelled.");
                return false;
            }
            if (isValidPassword(newPassword)) {
                break;
            } else {
                System.out.println("Invalid password format. Please follow the rules.");
            }
        }
        usernames.add(newUsername);
        passwords.add(newPassword);
        System.out.println("\nSign up successful! You can now log in.");
        return true;
    }

    // Regex: username = alphanumeric, 5-15 chars


    // Regex: password = at least 1 uppercase, 1 number, 8-20 chars
    private boolean isValidPassword(String password) {
        Pattern pattern = Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{8,20}$");
        return pattern.matcher(password).matches();
    }

    // --- LOGIN FLOW ---
    // Loop until valid credentials or cancel. Updates loggedInUser if successful.
    private boolean performLogin() {
        System.out.println("\n===============================");
        System.out.println("       User Login");
        System.out.println("===============================");
        while (true) {
            try {
                System.out.print("Enter username or 0 to cancel: ");
                String enteredUsername = scanner.nextLine();
                if (enteredUsername.equals("0")) {
                    System.out.println("Login cancelled.");
                    return false;
                }
                System.out.print("Enter password or 0 to cancel: ");
                String enteredPassword = scanner.nextLine();
                if (enteredPassword.equals("0")) {
                    System.out.println("Login cancelled.");
                    return false;
                }
                if (checkCredentials(enteredUsername, enteredPassword)) {
                    System.out.println("\nLogin successful! Welcome, " + loggedInUser + "!");
                    return true;
                } else {
                    System.out.println("\nInvalid username or password. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("Login error: " + e.getMessage());
            }
        }
    }

    // Check if user credentials match any pair in the list.
    private boolean checkCredentials(String username, String password) {
        for (int i = 0; i < usernames.size(); i++) {
            if (usernames.get(i).equals(username) && passwords.get(i).equals(password)) {
                loggedInUser = username;
                return true;
            }
        }
        return false;
    }

    // --- ORDER MENU LOOP ---
    // Core order processing logic. All options error handled.
    private void showOrderMenu() {
        clearCurrentOrder();
        boolean ordering = true;
        while (ordering) {
            System.out.println("\n===============================");
            System.out.println("Order Menu:");
            System.out.println("[1] Add Item");
            System.out.println("[2] Update Quantity");
            System.out.println("[3] Remove Item");
            System.out.println("[4] Display Orders");
            System.out.println("[5] Checkout");
            System.out.println("[6] Cancel Order");
            System.out.println("[0] Cancel/Back to Main Menu");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    addItemFlow();
                    break;
                case "2":
                    updateQuantityFlow();
                    break;
                case "3":
                    removeItemFlow();
                    break;
                case "4":
                    displayOrders();
                    break;
                case "5":
                    if (orderedItems.isEmpty()) {
                        System.out.println("Your order is empty.");
                    } else {
                        if (checkoutFlow()) ordering = false;
                    }
                    break;
                case "6":
                    ordering = false;
                    clearCurrentOrder();
                    System.out.println("Order cancelled.");
                    break;
                case "0":
                    ordering = false;
                    clearCurrentOrder();
                    System.out.println("Back to main menu.");
                    break;
                default:
                    System.out.println("Invalid choice. Please select from 1 to 6 or 0 to cancel.");
            }
        }
    }

    // --- ADD ITEM FLOW ---
    // Item, addon, qty - all with cancel option. All index input 1-based.
    private boolean addItemFlow() {
        try {
            System.out.println("\nAdd Item to Order:");
            for (int i = 0; i < foodNames.length; i++) {
                System.out.printf("[%d] %s - $%.2f\n", i + 1, foodNames[i], foodPrices[i]);
            }
            System.out.print("Select item number or 0 to cancel: ");
            String itemInput = scanner.nextLine();
            if (itemInput.equals("0")) {
                System.out.println("Add item cancelled.");
                return false;
            }
            int itemIdx = Integer.parseInt(itemInput) - 1;
            if (itemIdx < 0 || itemIdx >= foodNames.length) {
                System.out.println("Invalid item number.");
                return false;
            }
            for (int i = 0; i < addonNames.length; i++) {
                System.out.printf("[%d] %s - $%.2f\n", i + 1, addonNames[i], addonPrices[i]);
            }
            System.out.print("Select addon number or 0 to cancel: ");
            String addonInput = scanner.nextLine();
            if (addonInput.equals("0")) {
                System.out.println("Add item cancelled.");
                return false;
            }
            int addonIdx = Integer.parseInt(addonInput) - 1;
            if (addonIdx < 0 || addonIdx >= addonNames.length) {
                System.out.println("Invalid addon number.");
                return false;
            }
            System.out.print("Enter quantity or 0 to cancel: ");
            String qtyInput = scanner.nextLine();
            if (qtyInput.equals("0")) {
                System.out.println("Add item cancelled.");
                return false;
            }
            int qty = Integer.parseInt(qtyInput);
            if (qty < 1) {
                System.out.println("Quantity must be at least 1.");
                return false;
            }
            orderedItems.add(foodNames[itemIdx]);
            itemQuantities.add(qty);
            itemAddons.add(addonNames[addonIdx]);
            itemPrices.add((foodPrices[itemIdx] + addonPrices[addonIdx]) * qty);
            System.out.println("Item added!");
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Only numbers are allowed. " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
        return false;
    }

    // --- UPDATE QUANTITY FLOW ---
    // Lets user change quantity of an already-added item.
    private boolean updateQuantityFlow() {
        if (orderedItems.isEmpty()) {
            System.out.println("No items to update.");
            return false;
        }
        displayOrders();
        try {
            System.out.print("Enter order number to update or 0 to cancel: ");
            String idxInput = scanner.nextLine();
            if (idxInput.equals("0")) {
                System.out.println("Update cancelled.");
                return false;
            }
            int idx = Integer.parseInt(idxInput) - 1;
            if (idx < 0 || idx >= orderedItems.size()) {
                System.out.println("Invalid order number.");
                return false;
            }
            System.out.print("Enter new quantity or 0 to cancel: ");
            String qtyInput = scanner.nextLine();
            if (qtyInput.equals("0")) {
                System.out.println("Update cancelled.");
                return false;
            }
            int qty = Integer.parseInt(qtyInput);
            if (qty < 1) {
                System.out.println("Quantity must be at least 1.");
                return false;
            }
            double unitPrice = itemPrices.get(idx) / itemQuantities.get(idx);
            itemQuantities.set(idx, qty);
            itemPrices.set(idx, unitPrice * qty);
            System.out.println("Quantity updated!");
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Only numbers are allowed. " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
        return false;
    }

    // --- REMOVE ITEM FLOW ---
    // Removes chosen item from the order.
    private boolean removeItemFlow() {
        if (orderedItems.isEmpty()) {
            System.out.println("No item to remove.");
            return false;
        }
        displayOrders();
        try {
            System.out.print("Enter order number to remove or 0 to cancel: ");
            String idxInput = scanner.nextLine();
            if (idxInput.equals("0")) {
                System.out.println("Remove cancelled.");
                return false;
            }
            int idx = Integer.parseInt(idxInput) - 1;
            if (idx < 0 || idx >= orderedItems.size()) {
                System.out.println("Invalid order number.");
                return false;
            }
            orderedItems.remove(idx);
            itemQuantities.remove(idx);
            itemAddons.remove(idx);
            itemPrices.remove(idx);
            System.out.println("Item removed!");
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Only numbers are allowed. " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
        return false;
    }

    // --- DISPLAY ORDER LIST ---
    // Shows all orders before checkout or modification.
    private void displayOrders() {
        if (orderedItems.isEmpty()) {
            System.out.println("Your order is empty.");
            return;
        }
        System.out.println("\nCurrent Orders:");
        double total = 0;
        for (int i = 0; i < orderedItems.size(); i++) {
            System.out.printf("[%d] %s x%d (%s) - $%.2f\n", i + 1, orderedItems.get(i), itemQuantities.get(i), itemAddons.get(i), itemPrices.get(i));
            total += itemPrices.get(i);
        }
        System.out.println("Total: $" + String.format("%.2f", total));
        System.out.println("[0] Cancel/Back to Order Menu");
    }

    // --- CHECKOUT FLOW ---
    // Accepts payment, calculates change, and logs transaction.
    private boolean checkoutFlow() {
        displayOrders();
        double totalAmount = 0.0;
        for (double price : itemPrices) totalAmount += price;
        while (true) {
            try {
                System.out.print("Enter payment amount or 0 to cancel: $");
                String paymentInput = scanner.nextLine();
                if (paymentInput.equals("0")) {
                    System.out.println("Checkout cancelled.");
                    return false;
                }
                double payment = Double.parseDouble(paymentInput);
                if (payment < totalAmount) {
                    System.out.println("Insufficient payment. Please try again.");
                    continue;
                }
                double change = payment - totalAmount;
                System.out.println("Change: $" + String.format("%.2f", change));
                System.out.println("Thank you for your order!");
                logTransactionToFile(totalAmount);
                return true;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid amount. " + e.getMessage());
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        }
    }

    // --- CLEAR ORDER (reset all lists for fresh transaction) ---
    private void clearCurrentOrder() {
        orderedItems.clear();
        itemQuantities.clear();
        itemAddons.clear();
        itemPrices.clear();
    }

    // --- CONSTRUCTOR ---
    // Adds default users. No file-based user storage yet, but easy to add if needed.
    private TapsilogCashRegister() {
        usernames.add("karl");
        passwords.add("Lonely123");
        usernames.add("cashier");
        passwords.add("Cashier123");
    }

    // --- MAIN ENTRY POINT ---
    public static void main(String[] args) {
        TapsilogCashRegister chin = new TapsilogCashRegister();
        chin.start();
    }
}
