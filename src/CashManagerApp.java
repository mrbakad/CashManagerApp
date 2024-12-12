import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CashManagerApp extends Application {
    private Connection connection;
    private int currentUserId = -1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        connectToDatabase();

        primaryStage.setTitle("Cash Manager App");

        // Login Screen
        VBox loginBox = new VBox(10);
        loginBox.setPadding(new Insets(20));
        Label loginLabel = new Label("Login or Register");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Button loginButton = new Button("Login");
        Button registerButton = new Button("Register");
        Label messageLabel = new Label();

        loginBox.getChildren().addAll(loginLabel, usernameField, passwordField, loginButton, registerButton, messageLabel);

        Scene loginScene = new Scene(loginBox, 400, 300);

        // Main Screen
        VBox mainBox = new VBox(10);
        mainBox.setPadding(new Insets(20));
        Label balanceLabel = new Label("Balance: $0.00");
        Button addIncomeButton = new Button("Add Income");
        Button recordExpenseButton = new Button("Record Expense");
        Button viewTransactionsButton = new Button("View Transactions");
        Button viewMonthlySummaryButton = new Button("View Monthly Summary");
        Button logoutButton = new Button("Logout");

        mainBox.getChildren().addAll(balanceLabel, addIncomeButton, recordExpenseButton, viewTransactionsButton, viewMonthlySummaryButton, logoutButton);

        Scene mainScene = new Scene(mainBox, 600, 400);

        // Login Button Action
        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (authenticateUser(username, password)) {
                messageLabel.setText("");
                updateBalanceLabel(balanceLabel);
                primaryStage.setScene(mainScene);
            } else {
                messageLabel.setText("Invalid username or password.");
            }
        });

        // Register Button Action
        registerButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (registerUser(username, password)) {
                messageLabel.setText("Registration successful. Please login.");
            } else {
                messageLabel.setText("Username already exists.");
            }
        });

        // Logout Button Action
        logoutButton.setOnAction(e -> {
            currentUserId = -1;
            usernameField.clear();
            passwordField.clear();
            primaryStage.setScene(loginScene);
        });

        // Add Income Button Action
        addIncomeButton.setOnAction(e -> {
            showTransactionDialog(primaryStage, "Add Income", balanceLabel, true);
        });

        // Record Expense Button Action
        recordExpenseButton.setOnAction(e -> {
            showTransactionDialog(primaryStage, "Record Expense", balanceLabel, false);
        });

        // Set initial scene
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    private void connectToDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:cash_manager.db");
            Statement statement = connection.createStatement();
            // Create users table
            statement.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE, " +
                    "password TEXT)");
            // Create transactions table
            statement.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "date TEXT, " +
                    "amount REAL, " +
                    "category TEXT, " +
                    "FOREIGN KEY(user_id) REFERENCES users(id))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean authenticateUser(String username, String password) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM users WHERE username = ? AND password = ?");
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                currentUserId = resultSet.getInt("id");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean registerUser(String username, String password) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO users (username, password) VALUES (?, ?)");
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private void updateBalanceLabel(Label balanceLabel) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT SUM(amount) AS balance FROM transactions WHERE user_id = ?");
            statement.setInt(1, currentUserId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                double balance = resultSet.getDouble("balance");
                balanceLabel.setText(String.format("Balance: $%.2f", balance));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showTransactionDialog(Stage primaryStage, String title, Label balanceLabel, boolean isIncome) {
        Stage dialog = new Stage();
        dialog.setTitle(title);

        VBox dialogBox = new VBox(10);
        dialogBox.setPadding(new Insets(20));

        Label amountLabel = new Label("Enter amount:");
        TextField amountField = new TextField();
        Label categoryLabel = new Label("Enter category:");
        TextField categoryField = new TextField();
        Button saveButton = new Button("Save");

        dialogBox.getChildren().addAll(amountLabel, amountField, categoryLabel, categoryField, saveButton);

        saveButton.setOnAction(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                if (!isIncome) amount = -amount;

                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO transactions (user_id, date, amount, category) VALUES (?, ?, ?, ?)");
                statement.setInt(1, currentUserId);
                statement.setString(2, LocalDate.now().toString());
                statement.setDouble(3, amount);
                statement.setString(4, categoryField.getText().trim());
                statement.executeUpdate();

                updateBalanceLabel(balanceLabel);
                dialog.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Scene dialogScene = new Scene(dialogBox, 300, 200);
        dialog.setScene(dialogScene);
        dialog.show();
    }
}