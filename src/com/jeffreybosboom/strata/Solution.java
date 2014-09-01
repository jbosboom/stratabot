package com.jeffreybosboom.strata;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.primitives.Bytes;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A Solution represents a (potential) solution to a Strata puzzle: an order of
 * row and column ribbons plus a color assignment for each ribbon.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/30/2014
 */
public final class Solution {
	/**
	 * The ribbon order, bottom-to-top. If bit 7 is clear, the lower bits are a
	 * row number; if bit 7 is set, the lower bits are a column number.
	 */
	private final byte[] order;
	/**
	 * The color assignments, in the same order as {@link #order}.
	 */
	private final byte[] colors;
	private Solution(byte[] order, byte[] colors) {
		checkArgument(Bytes.asList(order).stream().distinct().count() == order.length, "contains duplicates: %s", Arrays.toString(order));
		this.order = order;
		this.colors = colors;
	}

	public static Solution empty() {
		return new Solution(new byte[0], new byte[0]);
	}

	public Solution appendRow(byte row, byte color) {
		byte[] newOrder = Arrays.copyOf(order, size()+1);
		byte[] newColors = Arrays.copyOf(colors, size()+1);
		for (int i = 0; i < size(); ++i)
			if (isRow(i) && ribbonIndex(i) >= row)
				++newOrder[i];
		newOrder[size()] = row;
		newColors[size()] = color;
		return new Solution(newOrder, newColors);
	}

	public Solution appendCol(byte col, byte color) {
		byte[] newOrder = Arrays.copyOf(order, size()+1);
		byte[] newColors = Arrays.copyOf(colors, size()+1);
		for (int i = 0; i < size(); ++i)
			if (isCol(i) && ribbonIndex(i) >= col)
				++newOrder[i];
		newOrder[size()] = (byte)(col | 128);
		newColors[size()] = color;
		return new Solution(newOrder, newColors);
	}

	/**
	 * Returns a complete solution for the given puzzle; that is, a solution
	 * containing all rows and columns in the given puzzle, even those not
	 * required to meet constraints.
	 * @param p a puzzle
	 * @return a complete solution to the given puzzle
	 */
	public Solution complete(Puzzle p) {
		byte[] newOrder = new byte[p.rows()+p.cols()];
		byte[] newColors = new byte[p.rows()+p.cols()];
		int idx = 0;
		//add anything missing
		for (byte row = 0; row < p.rows(); ++row)
			if (!Bytes.contains(order, row)) {
				newOrder[idx] = row;
				newColors[idx++] = 0;
			}
		for (byte col = 0; col < p.cols(); ++col)
			if (!Bytes.contains(order, (byte)(col | 128))) {
				newOrder[idx] = (byte)(col | 128);
				newColors[idx++] = 0;
			}
		//replay our contents in order
		System.arraycopy(order, 0, newOrder, idx, order.length);
		System.arraycopy(colors, 0, newColors, idx, colors.length);
		return new Solution(newOrder, newColors);
	}

	public int size() {
		return order.length;
	}

	public byte ribbonIndex(int pos) {
		return (byte)(order[pos] & ~128);
	}

	public boolean isRow(int pos) {
		return !isCol(pos);
	}

	public boolean isCol(int pos) {
		return (order[pos] & 128) != 0;
	}

	public byte color(int pos) {
		return colors[pos];
	}

	@Override
	public String toString() {
		return IntStream.range(0, size())
				.mapToObj(i -> (isRow(i) ? "row " : "col ") + ribbonIndex(i) + " = " + color(i))
				.collect(Collectors.joining(", "));
	}
}
