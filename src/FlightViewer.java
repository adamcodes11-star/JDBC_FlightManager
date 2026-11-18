import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Terminal-based arrivals viewer for the Airport Flight Arrivals Management System.
 * <p>
 * Prompts for a terminal number and prints all arrivals landing at that terminal, joined with
 * their departure city from {@code FlightRoutes} and sorted chronologically by scheduled time.
 * The terminal value is bound through a secure {@link PreparedStatement} placeholder.
 * </p>
 *
 * @author Student A &amp; Student B (Part 4 - Independent AI Prompting Challenge)
 * @version 1.0
 */
public class FlightViewer {

    /** JDBC connection URL pointing to FlightDB with public key retrieval and Jerusalem timezone enabled. */
    private static final String URL =
            "jdbc:mysql://localhost:3306/FlightDB?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";

    /** Default MySQL administrative user. */
    private static final String USER = "root";

    /** Parameterized INNER JOIN filtered by terminal and sorted by scheduled time. */
    private static final String SELECT_BY_TERMINAL_SQL =
            "SELECT a.id, a.flight_number, r.from_city, a.scheduled, a.delay_status, a.terminal " +
            "FROM Arrivals a " +
            "INNER JOIN FlightRoutes r ON a.flight_number = r.flight_number " +
            "WHERE a.terminal = ? " +
            "ORDER BY a.scheduled ASC";

    /**
     * Application entry point. Prompts for the MySQL root password and a terminal number
     * (2, 3, 4 or 5), then prints the matching arrivals.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Please enter your MySQL root password: ");
            String password = scanner.nextLine();

            System.out.print("Enter target terminal number (2, 3, 4, 5): ");
            String terminal = scanner.nextLine().trim();

            if (!terminal.matches("[2-5]")) {
                System.out.println("Invalid terminal '" + terminal + "'. Valid terminals are 2, 3, 4, 5.");
                return;
            }

            try (Connection conn = DriverManager.getConnection(URL, USER, password);
                 PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_TERMINAL_SQL)) {

                pstmt.setString(1, terminal); // terminal is VARCHAR(10) -> setString, not setInt

                try (ResultSet rs = pstmt.executeQuery()) {
                    System.out.println("\n --- Arrivals at Terminal " + terminal + " (sorted by scheduled time) ---");
                    System.out.printf("%-5s | %-10s | %-15s | %-10s | %-20s | %-10s%n",
                            "ID", "Flight", "From", "Scheduled", "Status", "Terminal");
                    System.out.println("--------------------------------------------------------------------------------------");

                    int rowCount = 0;
                    while (rs.next()) {
                        System.out.printf("%-5d | %-10s | %-15s | %-10s | %-20s | %-10s%n",
                                rs.getInt("id"),
                                rs.getString("flight_number"),
                                rs.getString("from_city"),
                                rs.getString("scheduled"),
                                rs.getString("delay_status"),
                                rs.getString("terminal"));
                        rowCount++;
                    }
                    System.out.println("--------------------------------------------------------------------------------------");
                    System.out.println("Total flights at terminal " + terminal + ": " + rowCount);
                }

            } catch (SQLException e) {
                System.err.println("Database error!");
                System.err.println("Error Code: " + e.getErrorCode());
                System.err.println("Message: " + e.getMessage());
            }
        }
    }
}
