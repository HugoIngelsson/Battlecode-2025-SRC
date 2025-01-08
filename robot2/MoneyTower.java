package robot2;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.UnitType;

public class MoneyTower extends Tower {
    int nextSpawn = 1;
    public MoneyTower(RobotController rc) throws GameActionException {
        super(rc);
    }

    void play() throws GameActionException {
        if (nextSpawn == 0 && rc.getPaint() >= 100 && rc.getMoney() >= 300) {
            for (int i=11; i>=0; i--) {
                if (rc.canBuildRobot(UnitType.MOPPER, spawnPlaces[i])) {
                    rc.buildRobot(UnitType.MOPPER, spawnPlaces[i]);
                    break;
                }
            }
            nextSpawn++;
        }
        if (nextSpawn == 1 && rc.getPaint() >= 200 && rc.getMoney() >= 250) {
            for (int i=11; i>=0; i--) {
                if (rc.canBuildRobot(UnitType.SOLDIER, spawnPlaces[i])) {
                    rc.buildRobot(UnitType.SOLDIER, spawnPlaces[i]);
                    break;
                }
            }
            nextSpawn = 0;
        }
    }

    @Override
    void endTurn() throws GameActionException {

    }
}
