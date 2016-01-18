import java.io.Serializable;

/**
 * Simple class representing a square on the board.
 */
public class Square implements Serializable {
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
		this.state = s.getState();
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
	 * @return Returns this Square's state.
	 */
	public int getState() {
		return this.state;
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
	 * Sets this square to be red.
	 */
	public void setRed() {
		this.state = Square.RED;
	}

	/**
	 * Sets this square to be a red king.
	 */
	public void setRedKing() {
		this.state = Square.RED_KING;
	}

	/**
	 * Sets this square to be black.
	 */
	public void setBlack() {
		this.state = Square.BLACK;
	}

	/**
	 * Sets this square to be a black king.
	 */
	public void setBlackKing() {
		this.state = Square.BLACK_KING;
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
	public boolean equals(Object obj) {
		Square s = (Square) obj;
		return (this.state == s.getState()) && (this.i == s.getVerticalCoord()) && (this.j == s.getHorizontalCoord());	
	}

	@Override
	public int hashCode() {
		return Integer.parseInt(Integer.toString(this.state - BLACK_KING + 1) + Integer.toString(this.i) + Integer.toString(this.j));
	}
}