import java.util.concurrent.ConcurrentLinkedQueue;

public class EscapeRoom
{
    public static ConcurrentLinkedQueue<String> inputQueue = new ConcurrentLinkedQueue<>();
    public static void enqueueCommand(String cmd) { if (cmd != null) inputQueue.add(cmd); }

    public static void main(String[] args) 
    {      
        System.out.println("Welcome to EscapeRoom!");
        System.out.println("Get to the other side of the room, avoiding walls and invisible traps,");
        System.out.println("pick up all the prizes.\n");

        GameGUI game = new GameGUI();
        game.createBoard();
        System.out.println("Traps on this board: " + game.getTotalTraps());

        int m = 60; // move size
        int movesCount = 0;
        int score = 0;

        String[] validCommands = { "right","left","up","down","r","l","u","d","t","space",
                "jump","jr","jumpleft","jl","jumpup","ju","jumpdown","jd",
                "pickup","p","find","springr","springl","springu","springd","sr","sl","su","sd",
                "removetrap","rt","yes","no","y","n",
                "score","status","quit","q","replay","help","?","h","c","check","restart"};

        boolean play = true;
        boolean springMode = false;
        boolean jumpMode = false;

        while (play)
        {
            System.out.print("> ");
            String cmd = UserInput.getValidInput(validCommands);
            movesCount++;

            // Mode activation
            if (cmd.equals("t")) { springMode = true; jumpMode = false; System.out.println("Spring mode: choose direction"); }
            else if (cmd.equals("space")) { jumpMode = true; springMode = false; System.out.println("Jump mode: choose direction"); }

            // Move commands
            int delta = 0;
            switch(cmd)
            {
                case "right": case "r":
                    if (springMode) { delta = game.springTrap(m,0); springMode=false; }
                    else if (jumpMode) { delta = jump(game, m,0); jumpMode=false; }
                    else delta = game.movePlayer(m,0);
                    break;
                case "left": case "l":
                    if (springMode) { delta = game.springTrap(-m,0); springMode=false; }
                    else if (jumpMode) { delta = jump(game,-m,0); jumpMode=false; }
                    else delta = game.movePlayer(-m,0);
                    break;
                case "up": case "u":
                    if (springMode) { delta = game.springTrap(0,-m); springMode=false; }
                    else if (jumpMode) { delta = jump(game,0,-m); jumpMode=false; }
                    else delta = game.movePlayer(0,-m);
                    break;
                case "down": case "d":
                    if (springMode) { delta = game.springTrap(0,m); springMode=false; }
                    else if (jumpMode) { delta = jump(game,0,m); jumpMode=false; }
                    else delta = game.movePlayer(0,m);
                    break;
                case "pickup": case "p": delta = game.pickupPrize(); break;
                case "find": delta = findTraps(game,m); break;
                case "check": case "c": delta = checkTraps(game,m); break;
                case "replay": delta = game.replay(); break;
                case "restart": 
                    delta = game.restart(); 
                    score = 0; // reset score to 0
                    System.out.println("Your Score and steps have reset");
                    break;
                case "score": case "status":
                    System.out.println("Score: " + score + ", Steps: " + game.getSteps() +
                            ", Trap Collisions: " + game.getTrapCollisions() + "/6, Trap Removals: " + game.getTrapRemovals());
                    break;
                case "quit": case "q": play=false; break;
                case "help": case "?": case "h": printHelp(); break;
                default: break;
            }

            score += delta;

            if (game.hasPendingTrapCollision())
            {
                System.out.print("Detrap for 5 points? (y/n): ");
                String[] yesNo = {"y","n","yes","no"};
                String resp = UserInput.getValidInput(yesNo);
                if (resp.equals("y") || resp.equals("yes"))
                {
                    score += game.removeTrap();
                }
                else
                {
                    // decline detrap: apply -1 now, but do NOT consume a removal chance
                    score -= 1;
                }
                game.clearPendingTrapCollision();
            }

            if (game.isAtFinish()) { System.out.println("Finish: reached the exit"); break; }
            System.out.println("Score now: " + score);
        }

        score += game.endGame();
        System.out.println("Final score: " + score);
        System.out.println("Total steps: " + game.getSteps());
    }

    private static int jump(GameGUI game, int dx, int dy)
    {
        int first = game.movePlayer(dx,dy);
        int second = 0;
        if (first == 0) second = game.movePlayer(dx,dy);
        return first + second;
    }

    private static int findTraps(GameGUI game,int m)
    {
        boolean any;
        boolean rTrap = game.isTrap(m,0);
        boolean lTrap = game.isTrap(-m,0);
        boolean uTrap = game.isTrap(0,-m);
        boolean dTrap = game.isTrap(0,m);
        any = rTrap || lTrap || uTrap || dTrap;
        if (any)
        {
            System.out.print("Find Trap: ");
            if (rTrap) System.out.print("right ");
            if (lTrap) System.out.print("left ");
            if (uTrap) System.out.print("up ");
            if (dTrap) System.out.print("down ");
            System.out.println();
        }
        else System.out.println("Find Trap: no traps adjacent");
        return 0;
    }

    private static int checkTraps(GameGUI game,int m)
    {
        boolean any;
        boolean rTrap = game.isTrap(m,0);
        boolean lTrap = game.isTrap(-m,0);
        boolean uTrap = game.isTrap(0,-m);
        boolean dTrap = game.isTrap(0,m);
        any = rTrap || lTrap || uTrap || dTrap;
        if (any)
        {
            System.out.print("Check: ");
            if (rTrap) System.out.print("right ");
            if (lTrap) System.out.print("left ");
            if (uTrap) System.out.print("up ");
            if (dTrap) System.out.print("down ");
            System.out.println();
        }
        else System.out.println("Check: no traps adjacent");
        return -1; // check costs 1 point
    }

    private static void printHelp()
    {
        System.out.println("Commands: right/left/up/down (r/l/u/d). Keyboard: WASD/Arrows for movement");
        System.out.println("jump (jr/jl/ju/jd or space + direction), pickup (p), find, check (c), spring (t + direction)");
        System.out.println("removetrap (rt), score, replay, restart (r), quit (q)");
    }
}
