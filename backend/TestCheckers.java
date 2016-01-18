import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

/**
 * Class for testing code.
 */
public class TestCheckers {
	public static void main(String[] args) {
	 	// EndgameDatabase db = new EndgameDatabase(Square.RED);
	 	// db.serialize("database");
	 	EndgameDatabase db = EndgameDatabase.deserialize("database.ser");
	 	System.out.println(db);

	 	for (Checkers g : db.database.keySet()) {
	 		System.out.println(db.database.get(g));
	 	}
	}
}