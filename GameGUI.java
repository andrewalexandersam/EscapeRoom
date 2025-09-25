import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * A Game board on which to place and move players.
 * 
 * @author PLTW
 * @version 1.0
 */
public class GameGUI extends JComponent
{
  static final long serialVersionUID = 141L; // problem 1.4.1

  private static final int WIDTH = 510;
  private static final int HEIGHT = 360;
  private static final int SPACE_SIZE = 60;
  private static final int GRID_W = 8;
  private static final int GRID_H = 5;
  private static final int START_LOC_X = 15;
  private static final int START_LOC_Y = 15;
  
  // initial placement of player
  int x = START_LOC_X; 
  int y = START_LOC_Y;

  // grid image to show in background
  private Image bgImage;

  // player image and info
  private Image player;
  private Point playerLoc;
  private int playerSteps;

  // walls, prizes, traps
  private int totalWalls;
  private Rectangle[] walls; 
  private Image prizeImage;
  private int totalPrizes;
  private Rectangle[] prizes;
  private int totalTraps;
  private Rectangle[] traps;

  // scores, sometimes awarded as (negative) penalties
  private int prizeVal = 10;
  private int trapVal = 5;
  private int endVal = 10;
  private int offGridVal = 5; // penalty only
  private int hitWallVal = 5;  // penalty only
  private int trapRemovalCost = 5;
  private int collisionLimit = 6;
  private int stepPenalty = 1;
  
  // trap collision tracking
  private int trapCollisions = 0;
  private int trapRemovals = 0;
  private int removalChancesUsed = 0; // counts YES detrap uses only (max 2)
  private boolean finishLocationTop = false; // false = bottom-right, true = top-right
  private boolean pendingTrapCollision = false;
  private boolean onTrapAfterRemovals = false; // true when standing on trap after 2 removals used
  private boolean stepPenaltyActive = false; // true after 2 removals used and next trap hit
  

  // game frame
  private JFrame frame;

  /**
   * Constructor for the GameGUI class.
   * Creates a frame with a background image and a player that will move around the board.
   */
  public GameGUI()
  {
    
    try {
      bgImage = ImageIO.read(new File("grid.png"));      
    } catch (Exception e) {
      System.err.println("Could not open file grid.png");
    }      
    try {
      prizeImage = ImageIO.read(new File("coin.png"));      
    } catch (Exception e) {
      System.err.println("Could not open file coin.png");
    }
  
    // player image, student can customize this image by changing file on disk
    try {
      player = ImageIO.read(new File("player.png"));      
    } catch (Exception e) {
     System.err.println("Could not open file player.png");
    }
    // save player location
    playerLoc = new Point(x,y);

    // create the game frame
    frame = new JFrame();
    frame.setTitle("EscapeRoom");
    frame.setSize(WIDTH, HEIGHT);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(this);
    frame.setResizable(false); 
    // Use KeyListener to capture real keyboard input
    installKeyListener();
    this.setFocusable(true);
    frame.setVisible(true);
    this.requestFocusInWindow();
    frame.requestFocus();

    // set default config
    totalWalls = 20;
    totalPrizes = 3;
    totalTraps = 8;
    
    // randomize finish location (top-right or bottom-right)
    Random rand = new Random();
    finishLocationTop = rand.nextBoolean();
  }

  /** Use a KeyListener to enqueue commands directly from keyboard events */
  private void installKeyListener()
  {
    KeyAdapter adapter = new KeyAdapter()
    {
      @Override
      public void keyPressed(KeyEvent e)
      {
        int code = e.getKeyCode();
        switch (code)
        {
          case KeyEvent.VK_W:
          case KeyEvent.VK_UP:
            EscapeRoom.enqueueCommand("u");
            break;
          case KeyEvent.VK_A:
          case KeyEvent.VK_LEFT:
            EscapeRoom.enqueueCommand("l");
            break;
          case KeyEvent.VK_D:
          case KeyEvent.VK_RIGHT:
            EscapeRoom.enqueueCommand("r");
            break;
          case KeyEvent.VK_S:
          case KeyEvent.VK_DOWN:
            EscapeRoom.enqueueCommand("d");
            break;
          case KeyEvent.VK_T:
            EscapeRoom.enqueueCommand("t");
            break;
          case KeyEvent.VK_SPACE:
            EscapeRoom.enqueueCommand("space");
            break;
          case KeyEvent.VK_H:
            EscapeRoom.enqueueCommand("h");
            break;
          case KeyEvent.VK_Q:
            EscapeRoom.enqueueCommand("q");
            break;
          case KeyEvent.VK_Y:
            EscapeRoom.enqueueCommand("y");
            break;
          case KeyEvent.VK_N:
            EscapeRoom.enqueueCommand("n");
            break;
          case KeyEvent.VK_P:
            EscapeRoom.enqueueCommand("p");
            break;
          case KeyEvent.VK_C:
            EscapeRoom.enqueueCommand("c");
            break;
          case KeyEvent.VK_R:
            EscapeRoom.enqueueCommand("restart");
            break;
          default:
            break;
        }
      }
    };
    // Attach to both the component and the frame to maximize chances of focus
    this.addKeyListener(adapter);
    frame.addKeyListener(adapter);
  }

 /**
  * After a GameGUI object is created, this method adds the walls, prizes, and traps to the gameboard.
  * Note that traps and prizes may occupy the same location.
  */
  public void createBoard()
  {
    traps = new Rectangle[totalTraps];
    createTraps();
    
    prizes = new Rectangle[totalPrizes];
    createPrizes();

    walls = new Rectangle[totalWalls];
    createWalls();
  }

  /**
   * Increment/decrement the player location by the amount designated.
   * This method checks for bumping into walls, going off the grid, and trap collisions.
   * <P>
   * precondition: amount to move is not larger than the board, otherwise player may appear to disappear
   * postcondition: increases number of steps even if the player did not actually move (e.g. bumping into a wall)
   * <P>
   * @param incrx amount to move player in x direction
   * @param incry amount to move player in y direction
   * @return penalty score for hitting a wall, going off grid, or trap collision, 0 otherwise
   */
  public int movePlayer(int incrx, int incry)
  {
      int perStepStickyPenalty = 0;
      // Apply continuous -1 per step while currently standing on a trap after removals
      if (trapRemovals >= 2 && isOnTrap())
      {
        perStepStickyPenalty -= stepPenalty;
      }
      int newX = x + incrx;
      int newY = y + incry;
      
      // increment regardless of whether player really moves
      playerSteps++;

      // check if off grid horizontally and vertically
      if ( (newX < 0 || newX > WIDTH-SPACE_SIZE) || (newY < 0 || newY > HEIGHT-SPACE_SIZE) )
      {
        System.out.println ("OFF THE GRID!");
        // apply trap step penalty if standing on a trap and removals are exhausted
        if (trapRemovals >= 2 && isOnTrap()) return perStepStickyPenalty;
        return perStepStickyPenalty; // otherwise no score penalty
      }

      // determine if a wall is in the way
      for (Rectangle r: walls)
      {
        // this rect. location
        int startX =  (int)r.getX();
        int endX  =  (int)r.getX() + (int)r.getWidth();
        int startY =  (int)r.getY();
        int endY = (int) r.getY() + (int)r.getHeight();

        // (Note: the following if statements could be written as huge conditional but who wants to look at that!?)
        // moving RIGHT, check to the right
        if ((incrx > 0) && (x <= startX) && (startX <= newX) && (y >= startY) && (y <= endY))
        {
          System.out.println("A WALL IS IN THE WAY");
          // apply trap step penalty if standing on a trap and removals are exhausted
          if (trapRemovals >= 2 && isOnTrap()) return perStepStickyPenalty;
          return perStepStickyPenalty; // otherwise no score penalty for hitting a wall
        }
        // moving LEFT, check to the left
        else if ((incrx < 0) && (x >= startX) && (startX >= newX) && (y >= startY) && (y <= endY))
        {
          System.out.println("A WALL IS IN THE WAY");
          if (trapRemovals >= 2 && isOnTrap()) return perStepStickyPenalty;
          return perStepStickyPenalty;
        }
        // moving DOWN check below
        else if ((incry > 0) && (y <= startY && startY <= newY && x >= startX && x <= endX))
        {
          System.out.println("A WALL IS IN THE WAY");
          if (trapRemovals >= 2 && isOnTrap()) return perStepStickyPenalty;
          return perStepStickyPenalty;
        }
        // moving UP check above
        else if ((incry < 0) && (y >= startY) && (startY >= newY) && (x >= startX) && (x <= endX))
        {
          System.out.println("A WALL IS IN THE WAY");
          if (trapRemovals >= 2 && isOnTrap()) return -stepPenalty;
          return 0;
        }     
      }

      // all is well, move player
      x += incrx;
      y += incry;
      
      // Check for trap collision at new location
      int trapPenalty = checkTrapCollision();

      // After both removals are used and next trap is hit, apply -1 on every step
      if (stepPenaltyActive)
      {
        trapPenalty -= stepPenalty;
      }
      
      repaint();   
      return perStepStickyPenalty + trapPenalty;   
  }

  /**
   * Check the space adjacent to the player for a trap. The adjacent location is one space away from the player, 
   * designated by newx, newy.
   * <P>
   * precondition: newx and newy must be the amount a player regularly moves, otherwise an existing trap may go undetected
   * <P>
   * @param newx a location indicating the space to the right or left of the player
   * @param newy a location indicating the space above or below the player
   * @return true if the new location has a trap that has not been sprung, false otherwise
   */
  public boolean isTrap(int newx, int newy)
  {
    double px = x + newx;
    double py = y + newy;


    for (Rectangle r: traps)
    {
      // DEBUG: System.out.println("trapx:" + r.getX() + " trapy:" + r.getY() + "\npx: " + px + " py:" + py);
      // zero size traps have already been sprung, ignore
      if (r.getWidth() > 0)
      {
        // if new location of player has a trap, return true
        if (r.contains(px, py))
        {
          System.out.println("A TRAP IS AHEAD");
          return true;
        }
      }
    }
    // there is no trap where player wants to go
    return false;
  }

  /**
   * Spring the trap. Traps can only be sprung once and attempts to spring
   * a sprung task results in a penalty.
   * <P>
   * precondition: newx and newy must be the amount a player regularly moves, otherwise an existing trap may go unsprung
   * <P>
   * @param newx a location indicating the space to the right or left of the player
   * @param newy a location indicating the space above or below the player
   * @return a positive score if a trap is sprung, otherwise a negative penalty for trying to spring a non-existent trap
   */
  public int springTrap(int newx, int newy)
  {
    double px = x + newx;
    double py = y + newy;

    // check all traps, some of which may be already sprung
    for (Rectangle r: traps)
    {
      // DEBUG: System.out.println("trapx:" + r.getX() + " trapy:" + r.getY() + "\npx: " + px + " py:" + py);
      if (r.contains(px, py))
      {
        // zero size traps indicate it has been sprung, cannot spring again, so ignore
        if (r.getWidth() > 0)
        {
          r.setSize(0,0);
          System.out.println("TRAP IS SPRUNG!");
          return trapVal;
        }
      }
    }
    // no trap here, penalty
    System.out.println("THERE IS NO TRAP HERE TO SPRING");
    return -trapVal;
  }

  /**
   * Pickup a prize and score points. If no prize is in that location, this results in a penalty.
   * <P>
   * @return positive score if a location had a prize to be picked up, otherwise a negative penalty
   */
  public int pickupPrize()
  {
    double px = x;
    double py = y;

    for (Rectangle p: prizes)
    {
      // DEBUG: System.out.println("prizex:" + p.getX() + " prizey:" + p.getY() + "\npx: " + px + " py:" + py);
      // if location has a prize, pick it up
      if (p.getWidth() > 0 && p.contains(px, py))
      {
        System.out.println("YOU PICKED UP A PRIZE!");
        p.setSize(0,0);
        repaint();
        return prizeVal;
      }
    }
    System.out.println("OOPS, NO PRIZE HERE");
    return 0; // no penalty for trying to pick up when no prize is present  
  }

  /**
   * Return the numbers of steps the player has taken.
   * <P>
   * @return the number of steps
   */
  public int getSteps()
  {
    return playerSteps;
  }
  
  /**
   * Check if player is currently on a trap and handle collision
   * @return penalty score for trap collision, 0 if no collision
   */
  public int checkTrapCollision()
  {
    double px = x;
    double py = y;
    
    for (Rectangle t: traps)
    {
      if (t.getWidth() > 0 && t.contains(px, py))
      {
        trapCollisions++;
        System.out.println("TRAP COLLISION! (" + trapCollisions + "/" + collisionLimit + ")");
        if (removalChancesUsed >= 2)
        {
          // After both removals are used, activate step penalty on next trap hit
          stepPenaltyActive = true;
          return -stepPenalty;
        }
        else
        {
          // still have removals left: prompt caller whether to detrap
          pendingTrapCollision = true;
          return 0;
        }
      }
    }
    return 0;
  }

  /**
   * Whether the last move resulted in a trap collision that requires prompting.
   */
  public boolean hasPendingTrapCollision()
  {
    return pendingTrapCollision;
  }

  /**
   * Clear the pending trap collision flag after it has been handled by the caller.
   */
  public void clearPendingTrapCollision()
  {
    pendingTrapCollision = false;
  }
  
  /**
   * Check if player is currently on a trap
   * @return true if player is on a trap, false otherwise
   */
  public boolean isOnTrap()
  {
    double px = x;
    double py = y;
    
    for (Rectangle t: traps)
    {
      if (t.getWidth() > 0 && t.contains(px, py))
      {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Remove trap at player's current location
   * @return cost of removal (negative score), or penalty if no trap or limit exceeded
   */
  public int removeTrap()
  {
    if (trapRemovals >= 2)
    {
      System.out.println("TRAP REMOVAL LIMIT REACHED! Traps are now permanent.");
      return -trapRemovalCost;
    }
    
    double px = x;
    double py = y;
    
    for (Rectangle t: traps)
    {
      if (t.getWidth() > 0 && t.contains(px, py))
      {
        t.setSize(0, 0); // remove trap
        trapRemovals++;
        removalChancesUsed++;
        System.out.println("TRAP REMOVED! (" + trapRemovals + "/2 removals used)");
        return -trapRemovalCost;
      }
    }
    
    System.out.println("NO TRAP HERE TO REMOVE!");
    return -trapRemovalCost;
  }
  
  /**
   * Get trap collision count
   * @return number of trap collisions
   */
  public int getTrapCollisions()
  {
    return trapCollisions;
  }
  
  /**
   * Get trap removal count
   * @return number of trap removals used
   */
  public int getTrapRemovals()
  {
    return trapRemovals;
  }

  /**
   * Return total number of traps configured on the board.
   */
  public int getTotalTraps()
  {
    return totalTraps;
  }

  /**
   * Consume a removal chance without removing a trap (user declined).
   */
  public void consumeRemovalChance()
  {
    if (removalChancesUsed < 2) removalChancesUsed++;
  }
  
  /**
   * Set the designated number of prizes in the game.  This can be used to customize the gameboard configuration.
   * <P>
   * precondition p must be a positive, non-zero integer
   * <P>
   * @param p number of prizes to create
   */
  public void setPrizes(int p) 
  {
    totalPrizes = p;
  }
  
  /**
   * Set the designated number of traps in the game. This can be used to customize the gameboard configuration.
   * <P>
   * precondition t must be a positive, non-zero integer
   * <P>
   * @param t number of traps to create
   */
  public void setTraps(int t) 
  {
    totalTraps = t;
  }
  
  /**
   * Set the designated number of walls in the game. This can be used to customize the gameboard configuration.
   * <P>
   * precondition t must be a positive, non-zero integer
   * <P>
   * @param w number of walls to create
   */
  public void setWalls(int w) 
  {
    totalWalls = w;
  }

  /**
   * Reset the board to replay existing game. The method can be called at any time but results in a penalty if called
   * before the player reaches the finish location.
   * <P>
   * @return positive score for reaching the finish location, penalty otherwise
   */
  public int replay()
  {
    int win = playerAtEnd();
  
    // resize prizes and traps to "reactivate" them
    for (Rectangle p: prizes)
      p.setSize(SPACE_SIZE/3, SPACE_SIZE/3);
    for (Rectangle t: traps)
      t.setSize(SPACE_SIZE/3, SPACE_SIZE/3);

    // move player to start of board and reset counters
    x = START_LOC_X;
    y = START_LOC_Y;
    playerSteps = 0;
    trapCollisions = 0;
    trapRemovals = 0;
    removalChancesUsed = 0;
    onTrapAfterRemovals = false;
    stepPenaltyActive = false;
    
    // randomize finish location for replay
    Random rand = new Random();
    finishLocationTop = rand.nextBoolean();
    
    repaint();
    return win;
  }

  /**
   * Create a completely new board layout (for restart when player spawns on trap)
   * @return 0 (no score change for restart)
   */
  public int restart()
  {
    // create new board with different layout
    createBoard();
    
    // move player to start of board and reset counters
    x = START_LOC_X;
    y = START_LOC_Y;
    playerSteps = 0;
    trapCollisions = 0;
    trapRemovals = 0;
    removalChancesUsed = 0;
    onTrapAfterRemovals = false;
    stepPenaltyActive = false;
    stepPenaltyActive = false;
    
    // randomize finish location for restart
    Random rand = new Random();
    finishLocationTop = rand.nextBoolean();
    
    repaint();
    return 0; // no score change for restart
  }

 /**
  * End the game, checking if the player made it to the far right wall.
  * <P>
  * @return positive score for reaching the far right wall, penalty otherwise
  */
  public int endGame() 
  {
    int win = playerAtEnd();
  
    setVisible(false);
    frame.dispose();
    return win;
  }

  /*------------------- public methods not to be called as part of API -------------------*/

  /** 
   * For internal use and should not be called directly: Users graphics buffer to paint board elements.
   */
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D)g;

    // draw grid
    g.drawImage(bgImage, 0, 0, null);

    // add (invisible) traps
    for (Rectangle t : traps)
    {
      g2.setPaint(Color.WHITE); 
      g2.fill(t);
    }

    // add prizes
    for (Rectangle p : prizes)
    {
      // picked up prizes are 0 size so don't render
      if (p.getWidth() > 0) 
      {
      int px = (int)p.getX();
      int py = (int)p.getY();
      g.drawImage(prizeImage, px, py, null);
      }
    }

    // add walls
    for (Rectangle r : walls) 
    {
      g2.setPaint(Color.BLACK);
      g2.fill(r);
    }
   
    // draw player, saving its location
    g.drawImage(player, x, y, 40,40, null);
    playerLoc.setLocation(x,y);
  }

  /*------------------- private methods -------------------*/

  /*
   * Add randomly placed prizes to be picked up.
   * Note:  prizes and traps may occupy the same location, with traps hiding prizes
   */
  private void createPrizes()
  {
    int s = SPACE_SIZE; 
    Random rand = new Random();
     for (int numPrizes = 0; numPrizes < totalPrizes; numPrizes++)
     {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);

      Rectangle r;
      r = new Rectangle((w*s + 15),(h*s + 15), 15, 15);
      prizes[numPrizes] = r;
     }
  }

  /*
   * Add randomly placed traps to the board. They will be painted white and appear invisible.
   * Note:  prizes and traps may occupy the same location, with traps hiding prizes
   */
  private void createTraps()
  {
    int s = SPACE_SIZE; 
    Random rand = new Random();
     for (int numTraps = 0; numTraps < totalTraps; numTraps++)
     {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);

      Rectangle r;
      r = new Rectangle((w*s + 15),(h*s + 15), 15, 15);
      traps[numTraps] = r;
     }
  }

  /*
   * Add walls to the board in random locations 
   */
  private void createWalls()
  {
     int s = SPACE_SIZE; 

     Random rand = new Random();
     for (int numWalls = 0; numWalls < totalWalls; numWalls++)
     {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);

      Rectangle r;
       if (rand.nextInt(2) == 0) 
       {
         // vertical wall
         r = new Rectangle((w*s + s - 5),h*s, 8,s);
       }
       else
       {
         /// horizontal
         r = new Rectangle(w*s,(h*s + s - 5), s, 8);
       }
       walls[numWalls] = r;
     }
  }

  /**
   * Checks if player is at the finish location (randomized top-right or bottom-right)
   * @return positive score for reaching finish, penalty otherwise
   */
  private int playerAtEnd() 
  {
    int score;
    double px = x;
    double py = y;
    
    // Check if at far right
    boolean atRight = px > (WIDTH - 2*SPACE_SIZE);
    
    // Check if at correct vertical position based on finish location
    boolean atCorrectY;
    if (finishLocationTop) {
      atCorrectY = py < SPACE_SIZE; // top area
    } else {
      atCorrectY = py > (HEIGHT - 2*SPACE_SIZE); // bottom area
    }
    
    if (atRight && atCorrectY)
    {
      // Check collision limit
      if (trapCollisions > collisionLimit) {
        System.out.println("TOO MANY TRAP COLLISIONS! Pay 5 points to finish anyway.");
        score = -5; // penalty for too many collisions
      } else {
        System.out.println("YOU MADE IT!");
        score = endVal;
      }
    }
    else
    {
      System.out.println("OOPS, YOU QUIT TOO SOON!");
      score = -endVal;
    }
    return score;
  }

  /**
   * Check if the player is currently at the randomized finish location.
   * @return true if at finish, false otherwise
   */
  public boolean isAtFinish()
  {
    double px = x;
    double py = y;
    boolean atRight = px > (WIDTH - 2*SPACE_SIZE);
    boolean atCorrectY = finishLocationTop ? (py < SPACE_SIZE) : (py > (HEIGHT - 2*SPACE_SIZE));
    return atRight && atCorrectY;
  }
}