package warspite_old2;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import java.util.Random;

public class RobotPlayer {
    static int turnCount = 0;
    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static Robot r;
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        PathFinder.init(rc);
        switch (rc.getType().getBaseType()) {
            case SOLDIER: r = new Soldier(rc); break;
            case SPLASHER: r = new Splasher(rc); break;
            case MOPPER: r = new Mopper(rc); break;
            case LEVEL_ONE_DEFENSE_TOWER: r = new DefenseTower(rc); break;
            case LEVEL_ONE_MONEY_TOWER: r = new MoneyTower(rc); break;
            case LEVEL_ONE_PAINT_TOWER: r = new PaintTower(rc); break;
            default: // PANIC! Shouldn't reach
                r = null;
                return;
        }

        r.turn1();
        turnCount = rc.getRoundNum()-1;

        while (true) {
            try {
                turnCount += 1;

                r.initTurn();
                r.play();
                r.endTurn();
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                if (rc.getRoundNum() != turnCount) {
                    turnCount = rc.getRoundNum();
                    System.out.println("Overflowed on bytecode " + rc.getType());
                }
                Clock.yield();
            }
        }
    }
}
