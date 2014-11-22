stratabot
=========

Purpose
-------

stratabot solves puzzles from the puzzle game
[Strata](http://store.steampowered.com/app/286380), including limited automation
to parse Strata's graphics and submit solutions with mouse clicks.

Building
--------

`ant fetch; ant compile`

Running
-------

To solve a puzzle you've typed in, run `Solver.main`.

To interact with Strata, open a puzzle and call `Effector.playPuzzle` or
`Effector.playWave`, passing in the side length (3 for 3x3 puzzles, etc.) and
number of colors, and stratabot will solve that puzzle or all the puzzles in the
wave.

TODO
----

With just a bit more effort, the automation could be extended to the set/wave
select screens as well, allowing the bot to play though all the puzzles without
assistance.

License
-------

stratabot is licensed under GPLv3+.