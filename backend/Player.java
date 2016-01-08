public abstract class Player {
	public abstract int[] move(Checkers game); // Method for making a move.
	public abstract int[] doubleJump(Checkers game, Square piece); // Method that handles capture sequences.
}