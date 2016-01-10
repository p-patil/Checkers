import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Class for building an endgame database.
 */
public class EndgameDatabase implements Serializable {
	HashMap<BoardWrapper, Position> boardToPositions;
	HashSet<BoardWrapper> roots;

	/**
	 * Basic constructor. Builds all possible positions with at most n pieces, and creates the game graph that results from them by initializing
	 * each Position object's successor attribute, using retrograde analysis. There are ((BOARD_SIZE^2 / 2) choose n) total such games. For
	 * BOARD_SIZE = 8 and n = 5, this is about 202k games.
	 * @param n The maximum number of pieces in any position.
	 */
	public EndgameDatabase(int n) {
		boardToPositions = new HashMap<>();
		roots = new HashSet<>();

	}

	/**
	 * Wrapper class for arrays which overrides the Object class's equals and hashCode methods, so arrays can be used as keys in a HashMap.
	 */
	public class BoardWrapper {
		public Square[][] board;

		public BoardWrapper(Square[][] board) {
			this.board = new Square[board.length][board[0].length];
			for (int i = 0; i < this.board.length; i++) {
				for (int j = 0; j < this.board[i].length; j++) {
					this.board[i][j] = new Square(board[i][j]);
				}
			}
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.board);
		}

		@Override
		public boolean equals(Object obj) {
			if (this.board.length != ((BoardWrapper) obj).board.length) {
				return false;
			}

			for (int i = 0; i < this.board.length; i++) {
				if (this.board[i].length != ((BoardWrapper) obj).board[i].length) {
					return false;
				}

				for (int j = 0; j < this.board[i].length; j++) {
					if (!this.board[i][j].equals(((BoardWrapper) obj).board[i][j])) {
						return false;
					}
				}
			}

			return true;
		}
	}

	/**
	 * Serializes this object.
	 * @param filepath The path to the file in which to store the contents of this object.
	 * @return Whether or not the serialization succeeded.
	 */
	public boolean serialize(String filepath) {
		boolean success = true;
		try {
			FileOutputStream fileOut = new FileOutputStream(filepath + ".ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this);
			out.close();
			fileOut.close();
		} catch (IOException e) {
			success = false;
		} finally {
			return success;
		}
	}

	/**
	 * Deserializes the endgame database located at the given filepath.
	 * @param filepath The path to the .ser file to deserialize.
	 * @return Returns the deserialized object, or null if there was an error.
	 */
	public static EndgameDatabase deserialize(String filepath) {
		try {
			FileInputStream fileIn = new FileInputStream(filepath);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			EndgameDatabase database = (EndgameDatabase) in.readObject();
			in.close();
			fileIn.close();
			return database;
		} catch (IOException e) {
			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
}