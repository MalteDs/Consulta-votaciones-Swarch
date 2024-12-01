import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    public Connection connectToDatabase() {
        try {
            String url = "jdbc:postgresql://xhgrid2:5432/votaciones";
            String user = "postgres";
            String password = "postgres";
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException("Error al conectar con la base de datos: " + e.getMessage(), e);
        }
    }
}
