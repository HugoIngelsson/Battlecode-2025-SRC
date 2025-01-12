package fulcrum;

import battlecode.common.*;

public abstract class Tower extends Robot {
    MapLocation[] spawnPlaces;
    int AOE_DMG, TARGET_DMG, ATTACK_RANGE_SQ;

    public Tower(RobotController rc) throws GameActionException {
        super(rc);

        spawnPlaces = new MapLocation[12];
        spawnPlaces[0] = rc.getLocation().add(Direction.NORTH);
        spawnPlaces[1] = rc.getLocation().add(Direction.NORTHEAST);
        spawnPlaces[2] = rc.getLocation().add(Direction.EAST);
        spawnPlaces[3] = rc.getLocation().add(Direction.SOUTHEAST);
        spawnPlaces[4] = rc.getLocation().add(Direction.SOUTH);
        spawnPlaces[5] = rc.getLocation().add(Direction.SOUTHWEST);
        spawnPlaces[6] = rc.getLocation().add(Direction.WEST);
        spawnPlaces[7] = rc.getLocation().add(Direction.NORTHWEST);
        spawnPlaces[8] = rc.getLocation().add(Direction.NORTH).add(Direction.NORTH);
        spawnPlaces[9] = rc.getLocation().add(Direction.EAST).add(Direction.EAST);
        spawnPlaces[10] = rc.getLocation().add(Direction.SOUTH).add(Direction.SOUTH);
        spawnPlaces[11] = rc.getLocation().add(Direction.WEST).add(Direction.WEST);
    }

    void turn1() throws GameActionException {
        home = rc.getLocation();
    }

    abstract void play() throws GameActionException;

    @Override
    void initTurn() throws GameActionException {
        // read from buffer
        // scan environment
    }

    MapLocation bestAttackTarget() throws GameActionException {
        MapLocation bestTarget = null;
        int distToTarget = -1;
        UnitType type = null;
        int targetHealth = Integer.MAX_VALUE;

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(ATTACK_RANGE_SQ);
        for (RobotInfo ri : nearbyRobots) {
            if (ri.getTeam() == rc.getTeam())
                continue;

            if (targetHealth > 0) {
                // prioritize kills
                if (ri.getHealth() <= TARGET_DMG) {
                    bestTarget = ri.getLocation();
                    distToTarget = rc.getLocation().distanceSquaredTo(bestTarget);
                    type = ri.getType();
                    targetHealth = ri.getHealth() - TARGET_DMG;
                }
                // otherwise, prioritize lower health
                else if (ri.getHealth() - TARGET_DMG < targetHealth) {
                    bestTarget = ri.getLocation();
                    distToTarget = rc.getLocation().distanceSquaredTo(bestTarget);
                    type = ri.getType();
                    targetHealth = ri.getHealth() - TARGET_DMG;
                }
            }
            // now we only replace if both are kills
            else if (ri.getHealth() <= TARGET_DMG) {
                // same type
                if (ri.getType() == type) {
                    // prioritize further away enemies, might escape sooner
                    if (ri.getLocation().distanceSquaredTo(rc.getLocation()) > distToTarget) {
                        bestTarget = ri.getLocation();
                        distToTarget = rc.getLocation().distanceSquaredTo(bestTarget);
                        type = ri.getType();
                        targetHealth = ri.getHealth() - TARGET_DMG;
                    }
                }
                // splashers are scarier than moppers
                else if (type == UnitType.MOPPER) {
                    if (ri.getType() == UnitType.SPLASHER) {
                        bestTarget = ri.getLocation();
                        distToTarget = rc.getLocation().distanceSquaredTo(bestTarget);
                        type = ri.getType();
                        targetHealth = ri.getHealth() - TARGET_DMG;
                    }
                }
                // splashers and moppers are scarier than soldiers
                else if (type == UnitType.SOLDIER) {
                    if (ri.getType() == UnitType.SPLASHER ||
                            ri.getType() == UnitType.MOPPER) {
                        bestTarget = ri.getLocation();
                        distToTarget = rc.getLocation().distanceSquaredTo(bestTarget);
                        type = ri.getType();
                        targetHealth = ri.getHealth() - TARGET_DMG;
                    }
                }
            }
        }

        return bestTarget;
    }
}
