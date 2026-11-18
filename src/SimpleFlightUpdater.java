import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Secure flight status updater for the Airport Flight Arrivals Management System.
 * <p>
 * Updates the {@code delay_status} of flight {@code LX354} in the {@code Arrivals} table of the
 * normalized {@code FlightDB} schema, using a value typed by the user.
 * </p>
 *
 * <h2>Why the previous version was vulnerable (SQL Injection)</h2>
 * <p>
 * The original implementation used a plain {@link java.sql.Statement} and built the SQL command
 * by raw string concatenation:
 * </p>
 * <pre>
 * "UPDATE Arrivals SET delay_status = '" + userInput + "' WHERE flight_number = 'LX354'"
 * </pre>
 * <p>
 * Because the user input became part of the SQL <em>syntax</em>, the payload {@code Landed' #}
 * produced the command:
 * </p>
 * <pre>
 * UPDATE Arrivals SET delay_status = 'Landed' #' WHERE flight_number = 'LX354'
 * </pre>
 * <ul>
 *   <li>The single quote ({@code '}) prematurely closed the string literal {@code 'Landed'}.</li>
 *   <li>The hash sign ({@code #}) started a MySQL single-line comment, so the database engine
 *       ignored the rest of the line - including the entire {@code WHERE} clause.</li>
 *   <li>The result was an unconditional {@code UPDATE} that overwrote all 299 rows of
 *       {@code Arrivals} with {@code 'Landed'} instead of updating a single flight.</li>
 * </ul>
 *
 * <h2>How PreparedStatement prevents SQL Injection</h2>
 * <ul>
 *   <li>The SQL text {@code UPDATE Arrivals SET delay_status = ? WHERE flight_number = ?} is fixed
 *       and sent to the driver/server <em>before</em> any user data is supplied, so its structure
 *       (the {@code WHERE} clause) can never be changed by input.</li>
 *   <li>Values are bound through {@link PreparedStatement#setString(int, String)}; the driver
 *       transmits them strictly as data literals and escapes special characters such as
 *       {@code '} and {@code #}. They are never parsed as SQL keywords, operators or comments.</li>
 *   <li>The same payload {@code Landed' #} is therefore stored literally as the text
 *       {@code Landed' #} in exactly one row (LX354), and the other 298 flights stay intact.</li>
 * </ul>
 * <p>
 * Both the {@link Connection} and the {@link PreparedStatement} are opened with
 * try-with-resources, guaranteeing they are closed automatically and preventing connection leaks.
 * </p>
 *
 * @author Student A (Driver) &amp; Student B (Navigator)
 * @version 2.0 (secure refactoring - Part 5.2)
 */
public class SimpleFlightUpdater {

    /** JDBC connection URL pointing to FlightDB with public key retrieval and Jerusalem timezone enabled. */
    private static final String URL =
            "jdbc:mysql://localhost:3306/FlightDB?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";

    /** Default MySQL administrative user. */
    private static final String USER = "root";

    /** The flight whose delay status is updated. */
    private static final String TARGET_FLIGHT = "LX354";

    /** Parameterized (pre-compiled) UPDATE command - user input can never alter its structure. */
    private static final String UPDATE_SQL =
            "UPDATE Arrivals SET delay_status = ? WHERE flight_number = ?";

    /**
     * Application entry point. Prompts for the MySQL root password and for a new delay status,
     * then securely updates flight {@code LX354} using a {@link PreparedStatement} and prints
     * the number of affected rows.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Please enter your MySQL root password: ");
            String password = scanner.nextLine();

            System.out.print("Enter a new delay status for flight '" + TARGET_FLIGHT + "': ");
            String newStatus = scanner.nextLine();

            try (Connection conn = DriverManager.getConnection(URL, USER, password);
                 PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {

                // User input is bound as a pure data literal - never interpreted as SQL syntax.
                pstmt.setString(1, newStatus);
                pstmt.setString(2, TARGET_FLIGHT);

                int rowsAffected = pstmt.executeUpdate();
                System.out.println("Rows affected: " + rowsAffected);

            } catch (SQLException e) {
                System.err.println("Database error!");
                System.err.println("Error Code: " + e.getErrorCode());
                System.err.println("Message: " + e.getMessage());
            }
        }
    }
}
