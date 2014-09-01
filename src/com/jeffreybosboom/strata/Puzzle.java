package com.jeffreybosboom.strata;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import java.util.Arrays;

/**
 * A Strata puzzle board. Puzzle boards are rotated 45 degrees clockwise, so the
 * lower-left-to-upper-right set of ribbons are the rows and the
 * lower-right-to-upper-left set are the columns.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/30/2014
 */
public final class Puzzle {
	/**
	 * The board itself.  Colors are integers from 0 to N-1, while unconstrained
	 * cells are -1.  These arrays may be shared with other Puzzle objects, so
	 * should not be modified.
	 */
	private final byte[][] board;
	private Puzzle(byte[][] board) {
		checkArgument(Arrays.stream(board).map(x -> x.length).distinct().count() <= 1, "board not rectangular");
		//TODO: check colors are packed into 0..N-1.
		this.board = board;
	}

	public static Puzzle fromArray(byte[][] board) {
		byte[][] a = new byte[board.length][];
		for (int i = 0; i < board.length; ++i)
			a[i] = board[i].clone();
		return new Puzzle(a);
	}

	public static Puzzle fromString(String str) {
		//TODO: assuming 10 color max
		String[] rows = str.split("\n");
		byte[][] board = new byte[rows.length][rows[0].length()];
		for (int i = 0; i < rows.length; ++i)
			for (int j = 0; j < rows[0].length(); ++j)
				board[i][j] = (byte)(rows[i].charAt(j) == ' ' ? -1 : Character.digit(rows[i].charAt(j), 10));
		return new Puzzle(board);
	}

	public byte rows() {
		return (byte)board.length;
	}

	public byte cols() {
		return rows() == 0 ? 0 : (byte)board[0].length;
	}

	public boolean isEmpty() {
		return rows() == 0 || cols() == 0;
	}

	public byte color(int row, int col) {
		checkElementIndex(row, rows());
		checkElementIndex(col, cols());
		return board[row][col];
	}

	public Puzzle withoutRow(int row) {
		checkElementIndex(row, rows());
		//we can share the common rows
		byte[][] newBoard = new byte[rows()-1][];
		for (int idx = 0, i = 0; i < rows(); ++i)
			if (i != row)
				newBoard[idx++] = board[i];
		Puzzle puzzle = new Puzzle(newBoard);
		assert puzzle.rows() == rows() - 1;
		return puzzle;
	}

	public Puzzle withoutCol(int col) {
		checkElementIndex(col, cols());
		if (cols()-1 == 0)
			return new Puzzle(new byte[0][0]);
		byte[][] newBoard = new byte[rows()][cols()-1];
		for (int i = 0; i < rows(); ++i)
			for (int idx = 0, j = 0; j < cols(); ++j)
				if (j != col)
					newBoard[i][idx++] = color(i, j);
		Puzzle puzzle = new Puzzle(newBoard);
		assert puzzle.cols() == cols() - 1;
		return puzzle;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Puzzle other = (Puzzle)obj;
		if (!Arrays.deepEquals(this.board, other.board))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 13 * hash + Arrays.deepHashCode(this.board);
		return hash;
	}

	@Override
	public String toString() {
		return Arrays.deepToString(board);
	}
}
