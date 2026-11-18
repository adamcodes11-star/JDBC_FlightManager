import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Batched flight status dispatcher for the Airport Flight Arrivals Management System.
 * <p>
 * Updates the {@code delay_status} of several arrivals at once using a single pre-compiled
 * {@link PreparedStatement} and JDBC batch processing ({@code addBatch} / {@code executeBatch}).
 * </p>
 *
 * @author Student B (Driver) &amp; Student A (Navigator)
 * @version 1.0
 */
public class AdvancedFlightDispatcher {

    /** JDBC connection URL pointing to FlightDB with public key retrieval and Jerusalem timezone enabled. */
    private static final String URL =
            "jdbc:mysql://localhost:3306/FlightDB?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";

    /** Default MySQL administrative user. */
    private static final String USER = "root";

    /**
     * Application entry point. Prompts for the MySQL root password, connects to FlightDB and
     * sets arrivals 1, 3 and 5 to {@code 'Landed - Final'} in one batch.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Please enter your MySQL root password: ");
            String password = scanner.nextLine();

            try (Connection conn = DriverManager.getConnection(URL, USER, password)) {
                System.out.println("Successfully connected to FlightDB!");
                updateMultipleDelays(conn, Arrays.asList(1, 3, 5), "Landed - Final");
            } catch (SQLException e) {
                System.err.println("Database error!");
                System.err.println("Error Code: " + e.getErrorCode());
                System.err.println("Message: " + e.getMessage());
            }
        }
    }

    /**
     * Updates {@code delay_status} for all given arrival IDs using one {@link PreparedStatement}
     * executed as a JDBC batch.
     * <p>
     * The constant {@code newStatus} parameter (index 1) is bound once, outside the loop; inside the
     * loop only the {@code id} parameter (index 2) changes and each parameter set is queued with
     * {@code addBatch()}. All updates are then sent to the server in a single round trip with
     * {@code executeBatch()}.
     * </p>
     *
     * @param conn       active JDBC {@link Connection} to {@code FlightDB}
     * @param arrivalIds IDs of the {@code Arrivals} rows to update
     * @param newStatus  the new delay status to set
     * @throws SQLException if a database access error occurs
     */
    public static void updateMultipleDelays(Connection conn, List<Integer> arrivalIds, String newStatus)
            throws SQLException {
        String sql = "UPDATE Arrivals SET delay_status = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus); // set the constant parameter ONCE, outside the loop

            for (Integer id : arrivalIds) {
                pstmt.setInt(2, id);
                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();

            int totalAffected = 0;
            for (int count : results) {
                if (count > 0) {
                    totalAffected += count;
                } else if (count == java.sql.Statement.SUCCESS_NO_INFO) {
                    totalAffected += 1; // driver executed the command but did not report a count
                }
            }

            System.out.println("Batch update completed: " + results.length + " commands executed, "
                    + totalAffected + " rows updated to '" + newStatus + "' (IDs: " + arrivalIds + ").");
        }
    }
}
