import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Set;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.IOException;

// IMPLEMENT DOUBLE JUMP FUNCTIONALITY.

/**
 * Class for building an endgame database.
 */
public class EndgameTablebase implements Serializable {
	public static final int ENDGAME_LIMIT = 4; // The threshold number of pieces before an endgame database is used.

	public HashMap<Checkers, Position> database; // Maps checkers games to the corresponding position object, whose successors and successorScores
												 // attributes will be fully set upon execution of the constructor.

	/**
	 * Basic constructor. Builds all possible positions with at most n pieces, and creates the game graph that results from them by initializing
	 * each Position object's successor attribute, using retrograde analysis. There are ((BOARD_SIZE^2 / 2) choose ENDGAME_LIMIT) total such 
	 * games. For BOARD_SIZE = 8 and ENDGAME_LIMIT = 5, this is about 202k games.
	 * @param turn The side (red or black) to build the database for.
	 */
	public EndgameTablebase(int turn) {
		this.database = new HashMap<>();
		for (Position p : generateWinningPositions(generateAllPositions(ENDGAME_LIMIT), turn)) {
			this.database.put(new Checkers(p.board, p.turn), p);
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
	public static EndgameTablebase deserialize(String filepath) {
		try {
			FileInputStream fileIn = new FileInputStream(filepath);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			EndgameTablebase database = (EndgameTablebase) in.readObject();
			in.close();
			fileIn.close();
			return database;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	// Helper methods below this line.

	/**
	 * Given a set of seed positions and a player's side to be on, returns the set of all positions that can be guaranteed to win for the 
	 * given side, assuming perfect play by that side.
	 * @param seedPositions The initial positions to use.
	 * @param player Which side to be on.
	 * @return Returns a set of all winnable (assuming perfect play by both sides) positions.
	 */
	public static HashSet<Position> generateWinningPositions(Set<Position> seedPositions, int player) {
		HashSet<Position> curr = new HashSet<>(), next = new HashSet<>(), allPositions = new HashSet<>();
		// Initialize the base case with all positions in which red has won.
		for (Position p : seedPositions) {
			if ((new Checkers(p.board, p.turn)).isGameOver() == 1) { // Position is terminal with red win.
				curr.add(p);
				allPositions.add(p);
			}
		}

		int currentTurn, prevTurn, totalValidMoves, maxPieces = (Checkers.BOARD_SIZE / 2 - 1) * (Checkers.BOARD_SIZE / 2), lastSize = -1;
		currentTurn = prevTurn = (player == Square.RED) ? Square.BLACK : Square.RED;
		// Keep iterating until all positions have been seen (ie no new positions are added)
		for (int k = 1; allPositions.size() != lastSize; lastSize = allPositions.size(), k++) {
			next.clear();

			// If curr contains positions in which it's black to move and no matter what move black makes, red can force a win in at most k moves.
			if (currentTurn == prevTurn) {
				for (Position p : curr) {

					// Add all positions that can reach a position in curr within a move.
					for (Position q : unMove(p)) {
						if (!allPositions.contains(q)) {
							if (!q.isLeaf()) {
								q.generateSuccessors();
								q.successorScores.put(p, k);
							}

							next.add(q);
							allPositions.add(q);
						}
					}

					// Add all positions that can reach a position in curr within a capture, if adding another piece doesn't exceed the limit.
					if ((currentTurn == Square.BLACK && p.redPieceCount() + 1 <= maxPieces) || (currentTurn == Square.RED && p.blackPieceCount() + 1 <= maxPieces)) {
						if (p.pieceCount() + 1 <= ENDGAME_LIMIT) {
							for (Position q : unCapture(p)) {
								if (!allPositions.contains(q)) {
									if (!q.isLeaf()) {
										q.generateSuccessors();
										q.successorScores.put(p, k);
									}

									next.add(q);
									allPositions.add(q);
								}
							}
					}
					}
				}
			} else { // curr contains positions in which it's red to move and there exists a move red can make that will put red in a position
					 // to be able to force a win in at most k - 1 moves (so red can force a win in k moves).
				// Add all positions in which it's black to move and no matter what move black makes, the result will always be some position
				// in curr (so that no matter what black does, red can force a win in at most k moves). Compute such positions by comparing the
				// number of times some position in curr references (in unMove or unCapture) a given position to the number of legal moves that
				// can be made in that position - if they match, then every legal move in that position leads to some position in curr.
				HashMap<Position, Integer> moveCounts = new HashMap<>(); // Stores the reference counts.
				for (Position p : curr) {
					for (Position q : unMove(p)) {
						if (!q.isLeaf()) {
							q.generateSuccessors();
							q.successorScores.put(p, k);
						}
						if (!moveCounts.containsKey(q)) {
							moveCounts.put(q, 0);
						}

						moveCounts.put(q, moveCounts.get(q) + 1);
					}

					// Only check uncaptures if the piece limit is not exceeded.
					if ((currentTurn == Square.BLACK && p.redPieceCount() + 1 <= maxPieces) || (currentTurn == Square.RED && p.blackPieceCount() + 1 <= maxPieces)) {	
						if (p.pieceCount() + 1 <= ENDGAME_LIMIT) {
							for (Position q : unCapture(p)) {
								q.generateSuccessors();
								q.successorScores.put(p, k);

								if (!moveCounts.containsKey(q)) {
									moveCounts.put(q, 0);
								}

								moveCounts.put(q, moveCounts.get(q) + 1);
							}
						}
					}
				}

				// Compare each computed position's reference count to the number of legal moves it has.
				for (Position p : moveCounts.keySet()) {
					totalValidMoves = 0;
					for (int i = 0; i < p.board.length; i++) {
						for (int j = 0; j < p.board[i].length; j++) {
							if (!p.board[i][j].isEmpty()) {
								totalValidMoves += Checkers.numValidMoves(p.board, i, j, p.turn);
							}
						}
					}

					// If the move counts match, add the position.
					if (moveCounts.get(p) == totalValidMoves) {
						next.add(p);
						allPositions.add(p);
					}
				}
			}

			currentTurn = (currentTurn == Square.RED) ? Square.BLACK : Square.RED;
			curr = new HashSet<>(next);
		}

		return allPositions;
	}

	/**
	 * Given a position and turn, computes all positions which reach the given one in exactly one move (excluding captures) made by the side 
	 * whose turn it is.
	 * @param p The position.
	 * @param turn Whose turn it is.
	 * @return Returns the set of all positions one move (not capture) away from p.
	 */
	public static HashSet<Position> unMove(Position p) {
		HashSet<Position> unmovePositions = new HashSet<>();

		Square[][] temp;
		int prevTurn = (p.turn == Square.RED) ? Square.BLACK : Square.RED;
		for (int i = 0; i < p.board.length; i++) {
			for (int j = 0; j < p.board[i].length; j++) {
				if (prevTurn == Square.RED && p.board[i][j].isRed()) {
					if (i + 1 < p.board[i].length) {
						if (j - 1 >= 0 && p.board[i + 1][j - 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i + 1][j - 1] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							unmovePositions.add(new Position(temp, prevTurn));
						}
						if (j + 1 < p.board[i].length && p.board[i + 1][j + 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i + 1][j + 1] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							unmovePositions.add(new Position(temp, prevTurn));
						}
					}

					if (p.board[i][j].isKing()) {
						if (i - 1 >= 0) {
							if (j - 1 >= 0 && p.board[i - 1][j - 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i - 1][j - 1].setRedKing();
								temp[i][j].setEmpty();
								unmovePositions.add(new Position(temp, prevTurn));
							}
							if (j + 1 < p.board[i].length && p.board[i - 1][j + 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i - 1][j + 1].setRedKing();
								temp[i][j].setEmpty();
								unmovePositions.add(new Position(temp, prevTurn));
							}
						}
					}
				} else if (prevTurn == Square.BLACK && p.board[i][j].isBlack()) {
					if (i - 1 >= 0) {
						if (j - 1 >= 0 && p.board[i - 1][j - 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i - 1][j - 1] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							unmovePositions.add(new Position(temp, prevTurn));
						}
						if (j + 1 < p.board[i].length && p.board[i - 1][j + 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i - 1][j + 1] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							unmovePositions.add(new Position(temp, prevTurn));
						}
					}

					if (p.board[i][j].isKing()) {
						if (i + 1 < p.board.length) {
							if (j - 1 >= 0 && p.board[i + 1][j - 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i + 1][j - 1].setBlackKing();
								temp[i][j].setEmpty();
								unmovePositions.add(new Position(temp, prevTurn));
							}
							if (j + 1 < p.board[i].length && p.board[i + 1][j + 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i + 1][j + 1].setBlackKing();
								temp[i][j].setEmpty();
								unmovePositions.add(new Position(temp, prevTurn));
							}
						}
					}
				}
			}
		}

		return unmovePositions;
	}

	// IMPLEMENT DOUBLE JUMP LOGIC
	/**
	 * Given a position and a turn, computes all positions which reach the given on in exactly one capture sequence (can include double jumps),
	 * made by the side whose turn it is.
	 * @param p The position.
	 * @param turn Whose turn it is.
	 * @return Returns the set of all positions one capture sequence away from p.
	 */
	public static HashSet<Position> unCapture(Position p) {
		HashSet<Position> uncapturePositions = new HashSet<>();
		Square[][] temp;
		int prevTurn = (p.turn == Square.RED) ? Square.BLACK : Square.RED;
		for (int i = 0; i < p.board.length; i++) {
			for (int j = 0; j < p.board[i].length; j++) {
				if (prevTurn == Square.RED && p.board[i][j].isRed()) {
					if (i + 2 < p.board.length) {
						if (j - 2 >= 0 && p.board[i + 2][j - 2].isEmpty() && p.board[i + 1][j - 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i + 2][j - 2] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							temp[i + 1][j - 1].setBlack();
							uncapturePositions.add(new Position(temp, prevTurn));

							temp = Position.deepSquareCopy(temp);
							temp[i + 1][j - 1].setBlackKing();
							uncapturePositions.add(new Position(temp, prevTurn));
						}
						if (j + 2 < p.board[i].length && p.board[i + 2][j + 2].isEmpty() && p.board[i + 1][j + 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i + 2][j + 2] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							temp[i + 1][j + 1].setBlack();
							uncapturePositions.add(new Position(temp, prevTurn));

							temp = Position.deepSquareCopy(temp);
							temp[i + 1][j + 1].setBlackKing();
							uncapturePositions.add(new Position(temp, prevTurn));
						}
					}

					if (p.board[i][j].isKing()) {
						if (i - 2 >= 0) {
							if (j - 2 >= 0 && p.board[i - 2][j - 2].isEmpty() && p.board[i - 1][j - 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i - 2][j - 2].setRedKing();
								temp[i][j].setEmpty();
								temp[i - 1][j - 1].setBlack();
								uncapturePositions.add(new Position(temp, prevTurn));

								temp = Position.deepSquareCopy(temp);
								temp[i - 1][j - 1].setBlackKing();
								uncapturePositions.add(new Position(temp, prevTurn));
							}
							if (j + 2 < p.board[i].length && p.board[i - 2][j + 2].isEmpty() && p.board[i - 1][j + 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i - 2][j + 2].setRedKing();
								temp[i][j].setEmpty();
								temp[i - 1][j + 1].setBlack();
								uncapturePositions.add(new Position(temp, prevTurn));

								temp = Position.deepSquareCopy(temp);
								temp[i - 1][j + 1].setBlackKing();
								uncapturePositions.add(new Position(temp, prevTurn));
							}
						}
					}
				} else if (prevTurn == Square.BLACK && p.board[i][j].isBlack()) {
					if (i - 2 >= 0) {
						if (j - 2 >= 0 && p.board[i - 2][j - 2].isEmpty() && p.board[i - 1][j - 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i - 2][j - 2] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							temp[i - 1][j - 1].setRed();
							uncapturePositions.add(new Position(temp, prevTurn));

							temp = Position.deepSquareCopy(temp);
							temp[i - 1][j - 1].setRedKing();
							uncapturePositions.add(new Position(temp, prevTurn));
						}
						if (j + 2 < p.board[i].length && p.board[i - 2][j + 2].isEmpty() && p.board[i - 1][j + 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i - 2][j + 2] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							temp[i - 1][j + 1].setRed();
							uncapturePositions.add(new Position(temp, prevTurn));

							temp = Position.deepSquareCopy(temp);
							temp[i - 1][j + 1].setRedKing();
							uncapturePositions.add(new Position(temp, prevTurn));
						}
					}

					if (p.board[i][j].isKing()) {
						if (i + 2 < p.board.length) {
							if (j - 2 >= 0 && p.board[i + 2][j - 2].isEmpty() && p.board[i + 1][j - 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i + 2][j - 2].setBlackKing();
								temp[i][j].setEmpty();
								temp[i + 1][j - 1].setRed();
								uncapturePositions.add(new Position(temp, prevTurn));

								temp = Position.deepSquareCopy(temp);
								temp[i + 1][j - 1].setRedKing();
								uncapturePositions.add(new Position(temp, prevTurn));
							}
							if (j + 2 < p.board[i].length && p.board[i + 2][j + 2].isEmpty() && p.board[i + 1][j + 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i + 2][j + 2].setBlackKing();
								temp[i][j].setEmpty();
								temp[i + 1][j + 1].setRed();
								uncapturePositions.add(new Position(temp, prevTurn));

								temp = Position.deepSquareCopy(temp);
								temp[i + 1][j + 1].setRedKing();
								uncapturePositions.add(new Position(temp, prevTurn));
							}
						}
					}
				}
			}
		}

		return uncapturePositions;
	}

	/**
	 * Generatess all legal board configurations with at most n pieces.
	 * NOTE: Still buggy - includes subtle unreachable positions.
	 * @param n The maximum number of red pieces allowed on the table.
	 * @return Returns the set of all legal positions.
	 */
	public static Set<Position> generateAllPositions(int n) {
		// We can use dynamic programming - for every position with k pieces, there is a position with (k - 1) pieces that is one piece away.
		// For each position with (k - 1) pieces, add another piece at every legal location, in all four possible states.
		Square[][] emptyBoard = new Square[Checkers.BOARD_SIZE][Checkers.BOARD_SIZE], temp; // Initialize empty board.
		for (int i = 0; i < emptyBoard.length; i++) {
			for (int j = 0; j < emptyBoard[i].length; j++) {
				emptyBoard[i][j] = new Square(Square.EMPTY, i, j);
			}
		}		

		// Base case, k = 1. Simply put a single piece, in all four possible states, in every legal place in an otherwise empty board.
		HashMap<Position, Integer> curr = new HashMap<>(), allPositions = new HashMap<>();
		Position tempPos1, tempPos2;
		for (int i = 0; i < emptyBoard.length; i++) { // Only iterate over dark squares of the checkers board.
			for (int j = 0; j < emptyBoard[i].length; j++) {
				if ((i + j) % 2 != 1) {
					continue;
				}

				// Cover all possible states. Note that for boards that are completely dominated by one side, it is impossible to reach the
				// position in which it is not the turn of the side with no pieces left (eg the board contains all black pieces and it's 
				// black to move), so these are omitted. Such positions are impossible because one move prior to the position it would have
				// been the turn of a side with no pieces remaining (since there is no move the side could have made to kill his own pieces)
				// and the game would have ended there instead of proceeding to the position in question. To make sure such positions do not
				// leak through for boards with more than one piece, keep track of boards that are dominated by a single color by mapping them
				// to 0 if they're mixed and to the color if not.

				// Skip boards that have non-kinged pieces on the enemy home rank or that are empty other than non-kinged pieces on the friendly home rank.
				if (i != 0 && i != emptyBoard.length - 1) {
					temp = Position.deepSquareCopy(emptyBoard);
					temp[i][j].setRed(); // Place piece at [i, j]. Since the only piece on the board is red, it must be black's turn.
					tempPos1 = new Position(temp, Square.BLACK);
					curr.put(tempPos1, Square.RED);
					allPositions.put(tempPos1, Square.RED);

					temp = Position.deepSquareCopy(emptyBoard);
					temp[i][j].setBlack(); // Here, the only piece on the board is black, so it must be red's turn.
					tempPos1 = new Position(temp, Square.RED);
					curr.put(tempPos1, Square.BLACK);
					allPositions.put(tempPos1, Square.BLACK);
				}

				temp = Position.deepSquareCopy(emptyBoard);
				temp[i][j].setRedKing();
				tempPos1 = new Position(temp, Square.BLACK);
				curr.put(tempPos1, Square.RED);
				allPositions.put(tempPos1, Square.RED);

				temp = Position.deepSquareCopy(emptyBoard);
				temp[i][j].setBlackKing();
				tempPos1 = new Position(temp, Square.RED);
				curr.put(tempPos1, Square.BLACK);
				allPositions.put(tempPos1, Square.BLACK);
			}
		}

		// Dynamic programming part - use level k set to construct level (k + 1) set.
		HashMap<Position, Integer> next = new HashMap<>(); // Used to store boards one recursive level up.
		int maxPieces = (Checkers.BOARD_SIZE / 2 - 1) * (Checkers.BOARD_SIZE / 2); // Maximum number of pieces for either side.
		// for (int k = 2; k <= n; k++) {
		for (int k = 2; k <= 2; k++) {
			next.clear();
			for (Position p : curr.keySet()) { // Iterate over all board configurations with (k - 1) pieces to construct level k boards.

				// Add the k-th piece, in all states, in every legal location.
				for (int i = 0; i < p.board.length; i++) {
					for (int j = 0; j < p.board[i].length; j++) {
						if (((i + j) % 2 != 1) || !p.board[i][j].isEmpty()) { // Must be an empty dark square to place piece.
							continue;
						}
						
						// Cover all possible states.
						
						// Add another red piece this iteration if the number of red pieces isn't already saturated.
						if (p.redPieceCount() + 1 <= maxPieces) {
							if (i != 0) {
								temp = Position.deepSquareCopy(p.board);
								temp[i][j].setRed(); // Place piece at [i, j]
								// If the position is marked red, it contains all red pieces, so don't put any additional red pieces with red to move.
								if (curr.get(p) == Square.RED) {
									tempPos1 = new Position(temp, Square.BLACK);
									next.put(tempPos1, Square.RED);
									allPositions.put(tempPos1, Square.RED);
								} else {
									tempPos1 = new Position(temp, Square.RED);
									next.put(tempPos1, 0);
									allPositions.put(tempPos1, 0);

									tempPos2 = new Position(temp, Square.BLACK);
									next.put(tempPos2, 0);
									allPositions.put(tempPos2, 0);
								}
							}

							temp = Position.deepSquareCopy(p.board);
							temp[i][j].setRedKing();

							if (curr.get(p) == Square.RED) {
								tempPos1 = new Position(temp, Square.BLACK);

								next.put(tempPos1, Square.RED);
								allPositions.put(tempPos1, Square.RED);
							} else {
								tempPos1 = new Position(temp, Square.RED);
								next.put(tempPos1, 0);
								allPositions.put(tempPos1, 0);

								tempPos2 = new Position(temp, Square.BLACK);
								next.put(tempPos2, 0);
								allPositions.put(tempPos2, 0);
							}
						}

						// Add another black piece this iteration if the number of black pieces isn't already saturated.
						if (p.blackPieceCount() + 1 <= maxPieces) {
							if (i != p.board.length - 1) {
								temp = Position.deepSquareCopy(p.board);
								temp[i][j].setBlack();
								if (curr.get(p) == Square.BLACK) {
									tempPos1 = new Position(temp, Square.RED);
									next.put(tempPos1, Square.BLACK);
									allPositions.put(tempPos1, Square.BLACK);
								} else {
									tempPos1 = new Position(temp, Square.RED);
									next.put(tempPos1, 0);
									allPositions.put(tempPos1, 0);

									tempPos2 = new Position(temp, Square.BLACK);
									next.put(tempPos2, 0);
									allPositions.put(tempPos2, 0);
								}
							}

							temp = Position.deepSquareCopy(p.board);
							temp[i][j].setBlackKing();
							if (curr.get(p) == Square.BLACK) {
								tempPos1 = new Position(temp, Square.RED);
								next.put(tempPos1, Square.BLACK);
								allPositions.put(tempPos1, Square.BLACK);
							} else {
								tempPos1 = new Position(temp, Square.RED);
								next.put(tempPos1, 0);
								allPositions.put(tempPos1, 0);

								tempPos2 = new Position(temp, Square.BLACK);
								next.put(tempPos2, 0);
								allPositions.put(tempPos2, 0);
							}
						}
					}

				}
			}

			curr = new HashMap<>(next); // Update.
		}

		return allPositions.keySet();
	}
}