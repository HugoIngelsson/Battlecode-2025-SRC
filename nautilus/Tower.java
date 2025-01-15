package nautilus;

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

        sortSpawnPlaces();
    }

    void sortSpawnPlaces() {
        MapLocation center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        for (int i=11; i>=0; i--) {
            for (int j=0; j<i; j++) {
                int d1 = spawnPlaces[i].distanceSquaredTo(center);
                int d2 = spawnPlaces[j].distanceSquaredTo(center);

                if (d2 < d1) {
                    MapLocation temp = spawnPlaces[i];
                    spawnPlaces[i] = spawnPlaces[j];
                    spawnPlaces[j] = temp;
                }
            }
        }
    }

    void turn1() throws GameActionException {
        home = rc.getLocation();
    }

    abstract void play() throws GameActionException;

    @Override
    void initTurn() throws GameActionException {
        decodeMessages();
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

    /**
     * Can you imagine an imaginary menagerie manager imagining managing an imaginary menagerie?
     */
    void decodeMessages() {
        Message[] messages = rc.readMessages(rc.getRoundNum());
        for (int i = 0; i < messages.length; i++) {
            int msgBytes = messages[i].getBytes();
            if ((msgBytes & 0x80000000) == 0) {
                // decode frontline info, i know these are local but it kinda depends how we wanna store this stuff
                // maybe into an array? idk
                int posY = msgBytes & 0x0000003F;
                int posX = (msgBytes & 0x00000FC0) >> 6;
                double enemyUnitRatio = Math.pow(2, ((msgBytes & 0x00007000) >> 12) - 1);
                int timestamp = rc.getRoundNum() - ((msgBytes & 0x000F8000) >> 15) * 10;
                int enemyMoppers = (msgBytes & 0x00700000) >> 20;
                int enemySoldiers = (msgBytes & 0x03800000) >> 23;
                int enemySplashers = (msgBytes & 0x1C000000) >> 26;
                double enemyTileRatio = Math.pow(2, ((msgBytes & 0x60000000) >> 29) - 1);
            } else {
                // decode tower info, same deal about how to store this
                int posY = msgBytes & 0x0000003F;
                int posX = (msgBytes & 0x00000FC0) >> 6;
                int type = (msgBytes & 0x00003000) >> 12;
                int timestamp = rc.getRoundNum() - ((msgBytes & 0x0007C000) >> 14) * 10;
            }
        }
    }
}
