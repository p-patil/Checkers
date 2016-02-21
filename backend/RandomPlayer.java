import java.util.Random;
import java.util.ArrayList;

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
	public int[] move(Position p) {
		ArrayList<int[]> pieces = new ArrayList<>();

		// Get all pieces.
		int k = 0;
		for (int i = 0; i < Checkers.BOARD_SIZE; i++) {
			for (int j = 0; j < Checkers.BOARD_SIZE; j++) {
				if (p.turn == Square.RED && p.board[i][j].isRed()) {
					pieces.add(new int[] {i, j});
				} else if (p.turn == Square.BLACK && p.board[i][j].isBlack()) {
					pieces.add(new int[] {i, j});
				}
			}
		}

		// Pick a random piece.
		int[] piece;
		Random r = new Random();
		do {
			piece = pieces.get(r.nextInt(pieces.size()));
		} while (Position.numValidMoves(p.board, Checkers.BOARD_SIZE, piece[0], piece[1], p.turn) == 0);


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
		} while (p.move(piece[0], piece[1], piece[0] + offset_v, piece[1] + offset_h, false) == null);

		return new int[] {piece[0], piece[1], piece[0] + offset_v, piece[1] + offset_h};
	}

	/**
	 * Performs a double jump on the given square. Assumes that there exists at least one possible capture move from the given square.
	 * @param game The checkers game to play on.
	 * @param i The vertical coordinate of the square to double jump from.
	 * @param j The horizontal coordinate of the square to double jump from.
	 * @return The coordinates array, same as the above move method.
	 */
	public int[] doubleJump(Position p, int i, int j) {
		// Make a random, legal capture.
		ArrayList<int[]> moves = new ArrayList<>();
		for (int offset_v = -2; offset_v <= 2; offset_v += 4) {
			for (int offset_h = -2; offset_h <= 2; offset_h += 4) {
				if (p.move(i, j, i + offset_v, j + offset_h, true) != null) {
					moves.add(new int[] {i + offset_v, j + offset_h});
				}
			}
		}

		int[] newPos = moves.get((new Random()).nextInt(moves.size()));
		return new int[] {i, j, newPos[0], newPos[1]};
	}
}