import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    public Connection connectToDatabase() {
        try {
            Class.forName("org.postgresql.Driver");

            String url = "jdbc:postgresql://xhgrid4:5432/votaciones";
            String user = "postgres";
            String password = "postgres";
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error al cargar el driver de la base de datos: " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException("Error al conectar con la base de datos: " + e.getMessage(), e);
        }
    }
}
