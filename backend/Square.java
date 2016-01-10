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
	private int i; // Vertical coordinate
	private int j; // Horizontal coordinate

	/**
	 * Basic constructor.
	 *
	 */
	public Square(int state, int i, int j) {
		this.state = state;
		this.i = i;
		this.j = j;
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

		this.i = s.getVerticalCoord();
		this.j = s.getHorizontalCoord();
	}

	/**
	 * @return Returns this Square's vertical coordinate.
	 */
	public int getVerticalCoord() {
		return this.i;
	}

	/**
	 * @return Returns this Square's horizontal coordinate.
	 */
	public int getHorizontalCoord() {
		return this.j;
	}

	/**
	 * Returns if a red piece is on this square.
	 * @return True if a red piece occupies this square, false otherwise.
	 */
	public boolean isRed() {
		return this.state == Square.RED || this.state == Square.RED_KING;
	}

	/**
	 * Returns if a black piece is on this square.
	 * @return True if a black piece occupies this square, false otherwise.
	 */
	public boolean isBlack() {
		return this.state == Square.BLACK || this.state == Square.BLACK_KING;
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

	/**
	 * Sets this square to be empty.
	 */
	public void setEmpty() {
		this.state = Square.EMPTY;
	}

	/**
	 * Makes the piece on this square a king.
	 */
	public void makeKing() {
		if (this.state == Square.RED) {
			this.state = Square.RED_KING;
		} else if (this.state == Square.BLACK) {
			this.state = Square.BLACK_KING;
		}
	}

	@Override
	public int hashCode() {
		return this.state + BLACK_KING;
	}

	@Override
	public boolean equals(Object obj) {
		Square s = (Square) obj;
		int state;
		if (s.isRed()) {
			if (s.isKing()) {
				state = Square.RED_KING;
			} else {
				state = Square.RED;
			}
		} else if (s.isBlack()) {
			if (s.isKing()) {
				state = Square.BLACK_KING;
			} {
				state = Square.BLACK;
			}
		} else {
			state = Square.EMPTY;
		}

		return (this.state == state) && (this.i == s.getVerticalCoord()) && (this.j == s.getHorizontalCoord());
	}
}