/**
 * Simple class representing a square on the board.
 */
public class Square {
	public static final int EMPTY = 0; // Represents an empty square.
	public static final int RED = 1; // Represents a square occupied by a red piece.
	public static final int BLACK = -1; // Represents a square occipied by a black piece.
	public static final int RED_KING = 2; // Represents a square occupied by a kinged red piece.
	public static final int BLACK_KING = -2; // Represents a square occupied by a kinged black piece.

	private int state; // Which piece is occupying this square, if any.

	/**
	 * Basic constructor.
	 */
	public Square(int state) {
		this.state = state;
	}

	/**
 	 * Constructor that initializes this square to be a copy of s.
 	 * @param s A square to clone.
 	 */
	public Square(Square s) {
		if (s.isEmpty()) {
			this.state = Square.EMPTY;
		} else if (s.isRed()) {
			if (s.isKing()) {
				this.state = Square.RED_KING;
			} else {
				this.state = Square.RED;
			}
		} else {
			if (s.isKing()) {
				this.state = Square.BLACK_KING;
			} else {
				this.state = Square.BLACK;
			}
		}
	}

	/**
	 * Returns if a red piece is on this square.
	 * @return True if a red piece occupies this square, false otherwise.
	 */
	public boolean isRed() {
		return this.state == Square.RED || this.state == Square.RED_KING;
	}

	/**
	 * Returns if no piece is on this square.
	 * @return True if no piece occupies this square, false otherwise.
	 */
	public boolean isEmpty() {
		return this.state == Square.EMPTY;
	}

	/**
	 * Returns if a kinged piece is on this square.
	 * @return True if a king occupies this square, false otherwise.
	 */
	public boolean isKing() {
		return this.state == Square.RED_KING || this.state == Square.BLACK_KING;
	}

	public void setEmpty() {
		this.state = Square.EMPTY;
	}

	public void makeKing() {
		if (this.state == Square.RED) {
			this.state = Square.RED_KING;
		} else if (this.state == Square.BLACK) {
			this.state = Square.BLACK_KING;
		}
	}
}