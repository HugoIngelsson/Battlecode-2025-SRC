package robot1;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.UnitType;

public class PaintTower extends Tower {
    public PaintTower(RobotController rc) throws GameActionException {
        super(rc);
    }

    void play() throws GameActionException {
        if (rc.getPaint() >= 200 && rc.getMoney() >= 250) {
            for (int i=11; i>=0; i--) {
                if (rc.canBuildRobot(UnitType.SOLDIER, spawnPlaces[i])) {
                    rc.buildRobot(UnitType.SOLDIER, spawnPlaces[i]);
                    break;
                }
            }
        }
    }

    @Override
    void endTurn() throws GameActionException {

    }
}
