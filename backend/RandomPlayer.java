import java.util.Random;

/**
 * Checkers player that makes random checkers moves. Useful for testing purposes.
 */
public class RandomPlayer {
	/**
	 * Basic constructor.
	 */
	public void RandomPlayer() {

	}

	/**
	 * Makes a random move on a given Checkers game for a given side.
	 * @param game The game to move on.
	 * @return Returns an array with four integers - the first pair is the source coordinates, the next is the destination coordinates.
	 */
	public int[] move(Checkers game) {
		Square[] pieces; // Array to hold all pieces on our side.
		if (game.getCurrentTurn() == Square.RED) {
			pieces = new Square[game.getRedCount()];
		} else {
			pieces = new Square[game.getBlackCount()];
		}

		// Get all pieces.
		int k = 0;
		for (int i = 0; i < Checkers.BOARD_SIZE; i++) {
			for (int j = 0; j < Checkers.BOARD_SIZE; j++) {
				if (game.getCurrentTurn() == Square.RED) {
					if (game.getPiece(i, j).isRed()) {
						pieces[k++] = game.getPiece(i, j);						
					}
				} else {
					if (game.getPiece(i, j).isBlack()) {
						pieces[k++] = game.getPiece(i, j);
					}
				}
			}
		}

		// Pick a random piece.
		Square piece;
		Random r = new Random();
		do {
			piece = pieces[r.nextInt(pieces.length)];
		} while (!game.hasValidMoves(piece.getVerticalCoord(), piece.getHorizontalCoord(), game.getCurrentTurn()));

		// Make a random, legal move.
		int offset_v, offset_h;
		do {
			if (r.nextInt(2) == 1) { // Randomly capture, with probability 0.5
				offset_v = (r.nextInt(2) == 0) ? 2 : -2;
				offset_h = (r.nextInt(2) == 0) ? 2 : -2;
			} else { // Otherwise, randomly move.
				offset_v = (r.nextInt(2) == 0) ? 1 : -1;
				offset_h = (r.nextInt(2) == 0) ? 1 : -1;
			}
		} while (!game.move(piece.getVerticalCoord(), piece.getHorizontalCoord(), piece.getVerticalCoord() + offset_v, 
						    piece.getHorizontalCoord() + offset_h));

		int[] coordinates = new int[4];
		coordinates[0] = piece.getVerticalCoord();
		coordinates[1] = piece.getHorizontalCoord();
		coordinates[2] = piece.getVerticalCoord() + offset_v;
		coordinates[3] = piece.getHorizontalCoord() + offset_h;
		return coordinates;
	}

	/**
	 * Performs a double jump on the given square. Assumes that there exists at least one possible capture move from the given square.
	 * @param game The checkers game to play on.
	 * @param piece The square to double jump from.
	 * @return The coordinates array, same as the above move method.
	 */
	public int[] doubleJump(Checkers game, Square piece) {
		// Make a random, legal capture.
		Random r = new Random();
		int offset_v, offset_h;
		do {
			offset_v = (r.nextInt(2) == 0) ? 2 : -2;
			offset_h = (r.nextInt(2) == 0) ? 2 : -2;
		} while (!game.move(piece.getVerticalCoord(), piece.getHorizontalCoord(), piece.getVerticalCoord() + offset_v, 
						    piece.getHorizontalCoord() + offset_h));

		int[] coordinates = new int[4];
		coordinates[0] = piece.getVerticalCoord();
		coordinates[1] = piece.getHorizontalCoord();
		coordinates[2] = piece.getVerticalCoord() + offset_v;
		coordinates[3] = piece.getHorizontalCoord() + offset_h;
		return coordinates;
	}
}