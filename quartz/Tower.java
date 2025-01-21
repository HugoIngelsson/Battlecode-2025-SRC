package quartz;

import battlecode.common.*;

public abstract class Tower extends Robot {
    // todo: maybe make messages last longer but broadcaster each others' positions so we can get rid of our own towers as "frontline"
    // todo: sometimes the indicator dot is just in a somewhat weird place lol
    MapLocation[] spawnPlaces;
    int AOE_DMG, TARGET_DMG, ATTACK_RANGE_SQ;
    // keeps track of towers (not necessarily frontline but im too lazy rn)
    MapLocation[] frontlineLocs;
    int[] frontlineTimestamp;
    int[] frontlineType;
    boolean fullFrontline;

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

        frontlineLocs = new MapLocation[5];
        frontlineTimestamp = new int[5];
        frontlineType = new int[5];
        fullFrontline = false;
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
        broadcastBirth();
    }

    abstract void play() throws GameActionException;

    @Override
    void initTurn() throws GameActionException {
        decodeMessages();


        // read from buffer
        // scan environment
    }


    /**
     * Tower-tower internet is literally 1776
     * @throws GameActionException
     */
    void broadcastBirth() throws GameActionException{
        int msg = 0;
        msg |= Math.min(rc.getLocation().y, 63);
        msg |= Math.min(rc.getLocation().x, 63) << 6;
        msg |= 0x3000; // ally
        msg |= Math.min((rc.getRoundNum()) / 10, 31) << 14; // idk

        msg |= 1 << 31;
        msg |= 1 << 30;
        rc.broadcastMessage(msg);
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
    void decodeMessages() throws GameActionException {
        sweepMessages();
        Message[] messages = rc.readMessages(rc.getRoundNum());
        for (int i = 0; i < messages.length; i++) {
            int msgBytes = messages[i].getBytes();
            MapLocation loc;
            int timestamp;
            int type;
            // double priority = 0;
            if ((msgBytes & 0x80000000) == 0) {
                break;
                // decode frontline info, i know these are local but it kinda depends how we wanna store this stuff
                // maybe into an array? idk
                /*int posY = msgBytes & 0x0000003F;
                int posX = (msgBytes & 0x00000FC0) >> 6;
                loc = new MapLocation(posX, posY);
                double enemyUnitRatio = Math.pow(2, ((msgBytes & 0x00007000) >> 12) - 1);
                timestamp = rc.getRoundNum() - ((msgBytes & 0x000F8000) >> 15) * 10;
                int enemyMoppers = (msgBytes & 0x00700000) >> 20;
                int enemySoldiers = (msgBytes & 0x03800000) >> 23;
                int enemySplashers = (msgBytes & 0x1C000000) >> 26;
                double enemyTileRatio = Math.pow(2, ((msgBytes & 0x60000000) >> 29) - 1);

                priority = 1.8 - (1 / enemyTileRatio) - (1 / enemyUnitRatio);*/
            } else {
                // decode tower info, same deal about how to store this
                int posY = msgBytes & 0x0000003F;
                int posX = (msgBytes & 0x00000FC0) >> 6;
                loc = new MapLocation(posX, posY);
                type = (msgBytes & 0x00003000) >> 12;
                int tt_msg = (msgBytes & 0x40000000) >> 30;
                timestamp = rc.getRoundNum() - ((msgBytes & 0x0007C000) >> 14) * 10;

                if((rc.getRoundNum() - timestamp) < 20 && tt_msg == 0){
                    System.out.println("Relaying");
                    msgBytes |= 0x40000000;
                    rc.broadcastMessage(msgBytes);
                }
            }

            if (!fullFrontline) {
                populateFrontline(loc, timestamp, type);
            } else {
                int index = findNearbyFrontline(loc);
                if (index == -1) {
                    for (int j = 0; j < frontlineLocs.length; j++) {
                        // not good
                        System.out.println(frontlineLocs[j]);
                    }
                }
                /*
                if (rc.getRoundNum() - frontlineTimestamp[index] >= 40) {
                    frontlineLocs[index] = loc;
                    frontlineTimestamp[index] = timestamp;
                } else if (Math.abs(timestamp - frontlineTimestamp[index]) < 20) {
                    reestimateFrontline(loc, index, timestamp, .95, type);
                } else {
                    reestimateFrontline(loc, index, timestamp, 0.3, type);
                }*/

                // potentially implement an encoding that distinguishes between ruins/enemies (esp for soldiers)
                frontlineLocs[index] = loc;
                frontlineTimestamp[index] = timestamp;
                frontlineType[index] = type;
            }

        }
    }

    /**
     * stuff
     *
     * @return the library books
     */
    int findNearbyFrontline(MapLocation newLoc) {
        int lowestDistance = 10000;
        int outIndex = -1;
        for (int i = 0; i < frontlineLocs.length; i++) {
            if (frontlineLocs[i] != null && frontlineLocs[i].distanceSquaredTo(newLoc) < lowestDistance) {
                outIndex = i;
                lowestDistance = frontlineLocs[i].distanceSquaredTo(newLoc);
            }
        }
        if(lowestDistance == 0){
            System.out.println("A baby tower was born");
        }
        return outIndex;
    }

    /**
     * yummy yummy in my tummy
     *
     * @param newLoc if you do it i'll do it
     */
    void populateFrontline(MapLocation newLoc, int roundsSince, int type) {
        for (int i = 0; i < frontlineLocs.length; i++) {
            // check if loc is alr in frontline, if so update (tower-tower comms)
            if (frontlineLocs[i] == null || frontlineLocs[i].equals(newLoc)) {
                frontlineLocs[i] = newLoc;
                frontlineTimestamp[i] = roundsSince;
                frontlineType[i] = type;
                return;
            }
        }
        fullFrontline = true;
    }

    /**
     * currently shelved
     *
     * @param newLoc
     * @param index
     * @param timestamp
     * @param priorityBenchmark
     * @param priority
     */
    void reestimateFrontline(MapLocation newLoc, int index, int timestamp, double priorityBenchmark, double priority) {
        if (priority < priorityBenchmark || frontlineTimestamp[index] > timestamp) {
            int newX = (newLoc.x + frontlineLocs[index].x) / 2;
            int newY = (newLoc.y + frontlineLocs[index].y) / 2;
            frontlineLocs[index] = new MapLocation(newX, newY);
            frontlineTimestamp[index] = (frontlineTimestamp[index] + timestamp) / 2;
        } else {
            frontlineLocs[index] = newLoc;
            frontlineTimestamp[index] = timestamp;
        }
    }

    void messageSpawnedRobot(MapLocation robotLoc) throws GameActionException {
        int lastLocIndex = 4;
        for (int i = 0; i < frontlineLocs.length; i++) {
            if (frontlineLocs[i] == null) {
                lastLocIndex = i - 1;
                break;
            }
        }
        if (rc.canSendMessage(robotLoc) && lastLocIndex >= 0) {
            int index = (int) (Math.random() * (lastLocIndex + 1));
            int msg = 0;
            msg |= frontlineLocs[index].x;
            msg |= frontlineLocs[index].y << 6;
            if (frontlineType[index] == 0) {
                msg |= (1 << 31);
            }
            rc.sendMessage(robotLoc, msg);
        }
    }

    int chooseNextSpawntype() {
        if (rc.getRoundNum() <= 5) {
            if (rc.getType() == UnitType.LEVEL_ONE_MONEY_TOWER) {
                System.out.println("HI");
                if (rc.getRoundNum() == 1)
                    return 0;
            }
            return 1;
        }

        if ((rc.getPaint() >= 300 || rc.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER)
                && Math.random() < 0.4 * rc.getRoundNum() / (10.0 + rc.getRoundNum())) {
            return 3;
        } else if (Math.random() < 0.6){
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * out with the old and in with the new (254 no way)
     */
    void sweepMessages() {
        for (int i = 0; i < frontlineLocs.length; i++) {
            if (frontlineLocs[i] != null && rc.getRoundNum() - frontlineTimestamp[i] > 100) {
                fullFrontline = false;
                for (int j = i; j < frontlineLocs.length - 1; j++) {
                    frontlineLocs[j] = frontlineLocs[j + 1];
                    frontlineTimestamp[j] = frontlineTimestamp[j + 1];
                    frontlineType[j] = frontlineType[j + 1];
                }
                frontlineLocs[4] = null;
                frontlineTimestamp[4] = 0;
                frontlineType[4] = 0;
            }
        }
    }
}
