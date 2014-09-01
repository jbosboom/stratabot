package com.jeffreybosboom.strata;

import com.google.common.io.CharStreams;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;

/**
 * UI interaction with Strata.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/31/2014
 */
public final class Effector {
	private static final int WIDTH = 1024, HEIGHT = 768;
	private static final int[][][] CELLS_3 = {
		{{406, 327}, {460, 275}, {515, 222}},
		{{460, 379}, {515, 327}, {569, 272}},
		{{513, 435}, {571, 383}, {621, 328}}
	};
	private static final int[][] ROWS_3 = {
		{342, 390},
		{396, 444},
		{450, 498},
	};
	private static final int[][] COLS_3 = {
		{576, 498},
		{629, 444},
		{682, 390},
	};
//	private static final int[][][] CELLS_3 = cellPoints(ROWS_3, COLS_3);

	private static final int[][] COLORS_2 = {
		{476, 740},
		{550, 740}
	};
	private static final int[][] COLORS_3 = {
		{440, 740},
		{510, 740},
		{585, 740}
	};
	private static final int[][][] COLORS = {
		null, null, COLORS_2, COLORS_3
	};
	private static final Color BACKGROUND_COLOR = new Color(208, 202, 183);

//	private static int[][][] cellPoints(int[][] rowSelectors, int[][] colSelectors) {
//		int[][][] retval = new int[rowSelectors.length][colSelectors.length][2];
//		for (int row = 0; row < rowSelectors.length; ++row)
//			for (int col = 0; col < colSelectors.length; ++col) {
//				int[] r = rowSelectors[row], c = colSelectors[col];
//				//row line has slope -1, col line has slope 1 (y grows downward)
//				Line2D.Double rowLine = new Line2D.Double(r[0], r[1], r[0]+1, r[1]-1);
//				Line2D.Double colLine = new Line2D.Double(c[0], c[1], c[0]+1, c[1]+1);
//				rowLine.
//				retval[row][col][0] = (rowSelectors[row][0] + colSelectors[col][0])/2;
//				retval[row][col][1] = rowSelectors[row][1] - (rowSelectors[row][0] - retval[row][col][0]);
//			}
//		return retval;
//	}

	private final Robot robot;
	private final Rectangle strataRect;
	public Effector() throws AWTException, IOException, InterruptedException {
		this.robot = new Robot();
		robot.setAutoDelay(300);
		ProcessBuilder pb = new ProcessBuilder("cmdow.exe strata /B /P".split(" "));
		Process p = pb.start();
		p.waitFor();
		Reader r = new InputStreamReader(p.getInputStream());
		List<String> readLines = CharStreams.readLines(r);
		if (readLines.size() != 1)
			throw new RuntimeException(readLines.toString());
		String[] fields = readLines.get(0).trim().split("\\h+");
		System.out.println(Arrays.toString(fields));
		//These include window decorations, whose size varies by computer.
		int windowLeft = Integer.parseInt(fields[fields.length-6]);
		int windowTop = Integer.parseInt(fields[fields.length-5]);
		int windowWidth = Integer.parseInt(fields[fields.length-4]);
		int windowHeight = Integer.parseInt(fields[fields.length-3]);
		int borderWidth = (windowWidth - WIDTH)/2;
		int titleBarHeight = windowHeight - HEIGHT - borderWidth;
		this.strataRect = new Rectangle(windowLeft + borderWidth, windowTop + titleBarHeight, WIDTH, HEIGHT);
	}

	public void playPuzzle(int sideLength, int numColors) {
		Color[] colors = new Color[numColors];
		//Initially the first color is selected (saturated), so we get the other
		//colors first, then click another.
		for (int i = 1; i < numColors; ++i)
			colors[i] = getPixelColor(COLORS[numColors][i]);
		click(COLORS[numColors][1]);
		colors[0] = getPixelColor(COLORS[numColors][0]);

		byte[][] puzzleBytes = new byte[sideLength][sideLength];
		for (int row = 0; row < sideLength; ++row)
			for (int col = 0; col < sideLength; ++col) {
				Color c = getPixelColor(CELLS_3[row][col]);
				puzzleBytes[row][col] = indexOfClosestColor(c, colors);
			}

		Puzzle puzzle = Puzzle.fromArray(puzzleBytes);
		System.out.println(puzzle);
		Solver solver = new Solver();
		Solution solution = solver.solve(puzzle).get().complete(puzzle);
		System.out.println(solution);
		System.out.format("%d states examined, %d backtracks%n", solver.puzzlesExamined(), solver.backtracks());

		for (int i = 0; i < solution.size(); ++i) {
			click(COLORS[numColors][solution.color(i)]);
			click((solution.isRow(i) ? ROWS_3 : COLS_3)[solution.ribbonIndex(i)]);
		}
	}

	private Color getPixelColor(int[] xy) {
		assert xy.length == 2;
		return robot.getPixelColor(xy[0] + strataRect.getLocation().x, xy[1] + strataRect.getLocation().y);
	}

	/**
	 * Left-clicks at the given coordinates in the Strata window.
	 * @param xy
	 */
	private void click(int[] xy) {
		assert xy.length == 2;
		System.out.println("click "+Arrays.toString(xy));
		robot.mouseMove(xy[0] + strataRect.getLocation().x, xy[1] + strataRect.getLocation().y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	/**
	 * Returns the index of the color in haystack with the smallest absolute
	 * difference from needle, or -1 if BACKGROUND_COLOR is closer to needle.
	 * @param needle
	 * @param haystack
	 * @return
	 */
	private static byte indexOfClosestColor(Color needle, Color[] haystack) {
		byte bestIdx = 0;
		int bestDiff = absoluteDifference(needle, haystack[0]);
		for (byte i = 1; i < haystack.length; ++i) {
			int d = absoluteDifference(needle, haystack[i]);
			if (d < bestDiff) {
				bestIdx = i;
				bestDiff = d;
			}
		}
		if (absoluteDifference(needle, BACKGROUND_COLOR) < bestDiff)
			return -1;
		return bestIdx;
	}

	private static int absoluteDifference(Color a, Color b) {
		return Math.abs(a.getRed() - b.getRed()) +
				Math.abs(a.getGreen() - b.getGreen()) +
				Math.abs(a.getBlue() - b.getBlue());
	}

	public static void main(String[] args) throws AWTException, IOException, InterruptedException {
		Effector e = new Effector();
		e.playPuzzle(3, 3);
	}
}
