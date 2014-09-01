package com.jeffreybosboom.strata;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Solves Puzzles.  Solver is not thread-safe.  Solvers may be reused, but doing
 * so will retain memory.
 *
 * It turns out that the solver will not backtrack iff the puzzle has no
 * solution, so the cache is not necessary. (TODO)
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/30/2014
 */
public final class Solver {
	public Solver() {}

	private final Map<Puzzle, Optional<Solution>> cache = new HashMap<>();
	public Optional<Solution> solve(Puzzle puzzle) {
		if (puzzle.isEmpty()) return Optional.of(Solution.empty());
		return cache.computeIfAbsent(puzzle, this::solve_uncached);
	}

	private Optional<Solution> solve_uncached(Puzzle puzzle) {
		for (byte row = 0; row < puzzle.rows(); ++row) {
			final byte row_ = row;
			Set<Byte> colors = IntStream.range(0, puzzle.cols())
					.mapToObj(col -> puzzle.color(row_, col))
					.filter(b -> b != -1)
					.collect(Collectors.toSet());
			if (colors.size() > 1) continue;
			byte color = colors.isEmpty() ? 0 : colors.iterator().next();
			Optional<Solution> solution = solve(puzzle.withoutRow(row));
			if (solution.isPresent())
				return Optional.of(solution.get().appendRow(row, color));
		}
		//TODO: implement transposition to remove this duplication
		for (byte col = 0; col < puzzle.cols(); ++col) {
			final byte col_ = col;
			Set<Byte> colors = IntStream.range(0, puzzle.rows())
					.mapToObj(row -> puzzle.color(row, col_))
					.filter(b -> b != -1)
					.collect(Collectors.toSet());
			if (colors.size() > 1) continue;
			byte color = colors.isEmpty() ? 0 : colors.iterator().next();
			Optional<Solution> solution = solve(puzzle.withoutCol(col));
			if (solution.isPresent())
				return Optional.of(solution.get().appendCol(col, color));
		}
		return Optional.empty();
	}

	public int puzzlesExamined() {
		return cache.size();
	}

	public int backtracks() {
		return (int)cache.values().stream().filter(o -> !o.isPresent()).count();
	}

	public static void main(String[] args) {
		String string = "0 00";
		Puzzle puzzle = Puzzle.fromString(string);
		Optional<Solution> solution = new Solver().solve(puzzle);
		System.out.println(solution.get());
	}
}
