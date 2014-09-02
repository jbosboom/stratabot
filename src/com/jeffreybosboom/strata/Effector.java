/*
 * Copyright 2014 Jeffrey Bosboom.
 * This file is part of stratabot.
 *
 * stratabot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * stratabot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with stratabot.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jeffreybosboom.strata;

import com.google.common.io.CharStreams;
import com.google.common.math.IntMath;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * UI interaction with Strata.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/31/2014
 */
public final class Effector {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	private static final int WIDTH = 1024, HEIGHT = 768;
	private static final int[][] ROWS_3 = subdivide(342, 390, 450, 498, 3);
	private static final int[][] COLS_3 = subdivide(576, 498, 682, 390, 3);
	private static final int[][][] CELLS_3 = cellPoints(ROWS_3, COLS_3);

	private static final int[][] ROWS_4 = subdivide(334, 378, 462, 507, 4);
	private static final int[][] COLS_4 = subdivide(560, 506, 690, 378, 4);
	private static final int[][][] CELLS_4 = cellPoints(ROWS_4, COLS_4);

	private static final int[][] ROWS_5 = subdivide(328, 368, 470, 512, 5);
	private static final int[][] COLS_5 = subdivide(552, 512, 696, 368, 5);
	private static final int[][][] CELLS_5 = cellPoints(ROWS_5, COLS_5);

	private static final int[][] ROWS_6 = subdivide(325, 362, 477, 514, 6);
	private static final int[][] COLS_6 = subdivide(547, 514, 699, 362, 6);
	private static final int[][][] CELLS_6 = cellPoints(ROWS_6, COLS_6);

	private static final int[][][] ROWS = {
		null, null, null, ROWS_3, ROWS_4, ROWS_5, ROWS_6
	};
	private static final int[][][] COLS = {
		null, null, null, COLS_3, COLS_4, COLS_5, COLS_6
	};
	private static final int[][][][] CELLS = {
		null, null, null, CELLS_3, CELLS_4, CELLS_5, CELLS_6
	};

	private static final int[][] COLORS_2 = {
		{476, 740},
		{550, 740}
	};
	private static final int[][] COLORS_3 = subdivide(440, 740, 585, 740, 3);
	private static final int[][] COLORS_4 = subdivide(400, 740, 620, 740, 4);
	private static final int[][] COLORS_5 = subdivide(366, 740, 658, 740, 5);
	private static final int[][] COLORS_6 = subdivide(330, 740, 695, 740, 6);
	private static final int[][][] COLORS = {
		null, null, COLORS_2, COLORS_3, COLORS_4, COLORS_5, COLORS_6
	};
	private static final Color BACKGROUND_COLOR = new Color(208, 202, 183);

	/**
	 * Divides the line segment between (xa, ya) and (xb, yb) into points-1
	 * line segments of approximately equal length, returning the points
	 * defining these segments.
	 * @param xa the x coordinate of the first point
	 * @param ya the y coordinate of the first point
	 * @param xb the x coordinate of the second point
	 * @param yb the y coordinate of the second point
	 * @param points the number of points to return
	 * @return an array of points defining the divided line segments
	 */
	private static int[][] subdivide(int xa, int ya, int xb, int yb, int points) {
		double x = xa, y = ya;
		double xIncr = (xb - xa)/(points-1), yIncr = (yb - ya)/(points-1);
		int[][] retval = new int[points][];
		for (int i = 0; i < retval.length; ++i, x += xIncr, y += yIncr)
			retval[i] = new int[]{(int)Math.round(x), (int)Math.round(y)};
		return retval;
	}

	private static int[][][] cellPoints(int[][] rowSelectors, int[][] colSelectors) {
		int[][][] retval = new int[rowSelectors.length][colSelectors.length][2];
		for (int row = 0; row < rowSelectors.length; ++row)
			for (int col = 0; col < colSelectors.length; ++col) {
				int[] r = rowSelectors[row], c = colSelectors[col];
				//row line has slope -1, col line has slope 1 (y grows downward)
				int br = r[0] + r[1], bc = -c[0] + c[1];
				retval[row][col][0] = (br - bc)/2;
				retval[row][col][1] = retval[row][col][0] + bc;
			}
		return retval;
	}
	//</editor-fold>

	private final Robot robot;
	private final Rectangle strataRect;
	public Effector() throws AWTException, IOException, InterruptedException {
		this.robot = new Robot();
		robot.setAutoDelay(200);
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

	/**
	 * Plays a puzzle with the given side length and number of colors.  Assumes
	 * Strata is at the puzzle screen with all animations (including the subtle
	 * fade on the color selectors) completed.
	 * @param sideLength the puzzle side length
	 * @param numColors the number of colors in the puzzle
	 */
	public void playPuzzle(int sideLength, int numColors) {
		playPuzzle(sideLength, getPuzzleColors(numColors));
	}

	private void playPuzzle(int sideLength, Color[] colors) {
		BufferedImage screenshot = robot.createScreenCapture(strataRect);
		byte[][] puzzleBytes = new byte[sideLength][sideLength];
		for (int row = 0; row < sideLength; ++row)
			for (int col = 0; col < sideLength; ++col) {
				Color c = getPixelColor(CELLS[sideLength][row][col], screenshot);
				puzzleBytes[row][col] = indexOfClosestColor(c, colors);
			}

		Puzzle puzzle = Puzzle.fromArray(puzzleBytes);
		System.out.println(puzzle);
		Solver solver = new Solver();
		Solution solution = solver.solve(puzzle).get().complete(puzzle);
		System.out.println(solution);
		System.out.format("%d states examined, %d backtracks%n", solver.puzzlesExamined(), solver.backtracks());

		byte currentColor = 0;
		for (int i = 0; i < solution.size(); ++i) {
			if (solution.color(i) != currentColor)
				click(COLORS[colors.length][(currentColor = solution.color(i))]);
			click((solution.isRow(i) ? ROWS[sideLength] : COLS[sideLength])[solution.ribbonIndex(i)]);
		}
	}

	/**
	 * Plays a "wave" (set) of puzzles.  Assumes Strata is at the first puzzle
	 * screen with all animations (including the subtle	fade on the color
	 * selectors) completed.
	 * @param sideLength the puzzles' side length
	 * @param numColors the number of colors in the puzzles
	 */
	public void playWave(int sideLength, int numColors) {
		//assumes beginning at first puzzle
		//We could share the screenshot between getting colors and playing the
		//first puzzle, but that's probably not worth it.
		int puzzles = sideLength*sideLength;
		Color[] colors = getPuzzleColors(numColors);
		while (puzzles-- > 0) {
			playPuzzle(sideLength, colors);
			sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
			click(new int[]{510, 622});
			sleepUninterruptibly(3000, TimeUnit.MILLISECONDS);
		}
	}

	private Color[] getPuzzleColors(int numColors) {
		BufferedImage screenshot = robot.createScreenCapture(strataRect);
		Color[] colors = new Color[numColors];
		//Initially the first color is selected (saturated), so we get the other
		//colors first, then click another.
		for (int i = 1; i < numColors; ++i)
			colors[i] = getPixelColor(COLORS[numColors][i], screenshot);
		click(COLORS[numColors][1]);
		colors[0] = getPixelColor(COLORS[numColors][0]);
		click(COLORS[numColors][0]);
		return colors;
	}

	private static int[][] mooreNeighborhood(int n) {
		int[][] retval = new int[IntMath.pow(2*n+1, 2)][];
		int idx = 0;
		for (int x = -n; x < n+1; ++x)
			for (int y = -n; y < n+1; ++y)
				retval[idx++] = new int[]{x, y};
		return retval;
	}
	private static final int[][] NEIGHBORHOOD = mooreNeighborhood(2);
	private Color getPixelColor(int[] xy) {
		assert xy.length == 2;
		int r = 0, g = 0, b = 0;
		for (int[] adj : NEIGHBORHOOD) {
			Color c = robot.getPixelColor(xy[0] + strataRect.getLocation().x + adj[0],
					xy[1] + strataRect.getLocation().y + adj[1]);
			r += c.getRed();
			g += c.getGreen();
			b += c.getBlue();
		}
		return new Color(r/NEIGHBORHOOD.length, g/NEIGHBORHOOD.length, b/NEIGHBORHOOD.length);
	}

	private Color getPixelColor(int[] xy, BufferedImage screenshot) {
		assert xy.length == 2;
		int r = 0, g = 0, b = 0;
		for (int[] adj : NEIGHBORHOOD) {
			Color c = new Color(screenshot.getRGB(xy[0] + adj[0], xy[1] + adj[1]));
			r += c.getRed();
			g += c.getGreen();
			b += c.getBlue();
		}
		return new Color(r/NEIGHBORHOOD.length, g/NEIGHBORHOOD.length, b/NEIGHBORHOOD.length);
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
		double bestDiff = perceptualDifference(needle, haystack[0]);
		for (byte i = 1; i < haystack.length; ++i) {
			double d = perceptualDifference(needle, haystack[i]);
			if (d < bestDiff) {
				bestIdx = i;
				bestDiff = d;
			}
		}
		if (perceptualDifference(needle, BACKGROUND_COLOR) < bestDiff)
			return -1;
		return bestIdx;
	}

	/**
	 * Returns a measure of the perceptual difference between the given colors
	 * (lower values are less different).  The metric used is from
	 * http://www.compuphase.com/cmetric.htm.
	 * @param a a color
	 * @param b a color
	 * @return the perceptual difference between the two colors (lower is less
	 * different)
	 */
	private static double perceptualDifference(Color a, Color b) {
		double meanRed = ((double)a.getRed() + b.getRed())/2;
		int dR = a.getRed() - b.getRed();
		int dG = a.getGreen() - b.getGreen();
		int dB = a.getBlue() - b.getBlue();
		return Math.sqrt((2 + meanRed/256)*dR*dR + 4*dG*dG + (2 + (255 - meanRed)/256)*dB*dB);
	}

	public static void main(String[] args) throws AWTException, IOException, InterruptedException {
		Effector e = new Effector();
		e.playPuzzle(6, 6);
	}
}
