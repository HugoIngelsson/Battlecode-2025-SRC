package sabre;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.UnitType;

public class MoneyTower extends Tower {
    int nextSpawn = 1;
    public MoneyTower(RobotController rc) throws GameActionException {
        super(rc);
        this.AOE_DMG = 10;
        this.TARGET_DMG = 20;
        this.ATTACK_RANGE_SQ = 9;
    }

    void play() throws GameActionException {
        indicateKnown();

        rc.attack(null);
        MapLocation target = bestAttackTarget();
        if (target != null) {
            rc.attack(target);
        }

        if (nextSpawn == 0 && rc.getPaint() >= 100 &&
                (rc.getMoney() >= 1030 && rc.getRoundNum() > 30 || rc.getRoundNum() <= 3 && rc.getMoney() >= 300)) {
            for (int i=11; i>=0; i--) {
                int id = (int)(Math.random() * 12);
                if (rc.canBuildRobot(UnitType.MOPPER, spawnPlaces[id])) {
                    rc.buildRobot(UnitType.MOPPER, spawnPlaces[id]);
                    messageRobot(spawnPlaces[id]);
                    break;
                }
            }

            nextSpawn = chooseNextSpawntype();
        }
        if (nextSpawn == 1 && rc.getPaint() >= 200 &&
                (rc.getMoney() >= 1030 && rc.getRoundNum() > 30 || rc.getRoundNum() <= 3 && rc.getMoney() >= 250)) {
            for (int i=11; i>=0; i--) {
                int id = (int)(Math.random() * 12);
                if (rc.canBuildRobot(UnitType.SOLDIER, spawnPlaces[id])) {
                    rc.buildRobot(UnitType.SOLDIER, spawnPlaces[id]);
                    messageRobot(spawnPlaces[id]);
                    break;
                }
            }

            nextSpawn = chooseNextSpawntype();
        }
        else if (nextSpawn == 3 && rc.getPaint() >= 300 && rc.getMoney() >= 1030) {
            for (int i=11; i>=0; i--) {
                int id = (int)(Math.random() * 12);
                if (rc.canBuildRobot(UnitType.SPLASHER, spawnPlaces[id])) {
                    rc.buildRobot(UnitType.SPLASHER, spawnPlaces[id]);
                    messageRobot(spawnPlaces[id]);
                    break;
                }
            }

            nextSpawn = chooseNextSpawntype();
        }
    }

    @Override
    void endTurn() throws GameActionException {
        super.endTurn();
    }
}
