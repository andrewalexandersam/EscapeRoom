# Escape Room Game
You're stuck in a room and have to get out by moving around and avoiding traps.

## How to Play
Move around with WASD keys or type l/r/u/d in the terminal. Collect coins for points and try to reach the exit on the right side. Watch out for invisible traps!

### Controls
- WASD or arrow keys to move
- Space + direction to jump
- T + direction to spring over traps
- P to pick up coins
- C to check for nearby traps (costs 1 point)
- R to restart
- Q to quit
- H for help
- Y to remove trap
- N to not remove trap

### Traps
- You can remove 2 traps for 5 points each
- After that, every step costs 1 point
- If you hit more than 6 traps, you lose 5 points when finishing

### Scoring
- +10 for coins
- +5 for correctly springing a trap
- -5 to remove traps
- -1 for declining trap removal
- -1 per step after using 2 removals
- -5 if you finish with too many trap hits
- +10 for finishing

## Running It
```bash
javac *.java
java EscapeRoom
```

## Made By
Andrew Alexander Sam and Atharv Sharma

For Dr. Schick's AP Computer Science A class
