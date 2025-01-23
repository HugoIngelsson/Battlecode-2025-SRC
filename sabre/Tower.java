package sabre;

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
    int lastBroadcasted = 0;

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

        frontlineLocs = new MapLocation[50];
        frontlineTimestamp = new int[50];
        frontlineType = new int[50];
        fullFrontline = false;

        for (int i=0; i<50; i++) {
            frontlineTimestamp[i] = Integer.MAX_VALUE / 2;
        }
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
        determineBestDestinations();

        // read from buffer
        // scan environment
    }

    @Override
    void endTurn() throws GameActionException {
        for (int i=0; i<frontlineLocs.length; i++) {
            if (frontlineLocs[i] != null)
                frontlineTimestamp[i]++;
            else
                break;
        }

        if (rc.getRoundNum() > 50) // send the HORSE
            endOfTurnMessageUnits();
        endOfTurnRelay();
    }


    /**
     * Tower-tower internet is literally 1776
     * @throws GameActionException
     */
    void broadcastBirth() throws GameActionException {
        int msg = 0;
        msg |= Math.min(rc.getLocation().y, 63);
        msg |= Math.min(rc.getLocation().x, 63) << 6;
        msg |= 3 << 12; // ally
//        msg |= Math.min((rc.getRoundNum()) / 10, 31) << 14; // idk

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
        Message[] messages = rc.readMessages(rc.getRoundNum()-1);
        for (int i = 0; i < messages.length && Clock.getBytecodesLeft() > 7000; i++) {
            int msgBytes = messages[i].getBytes();
            MapLocation loc;
            int timestamp;
            int type;
            // double priority = 0;
            if ((msgBytes & 0x80000000) == 0) {
                continue;
            } else {
                // decode tower info, same deal about how to store this
                int posY = msgBytes & 0x0000003F;
                int posX = (msgBytes & 0x00000FC0) >> 6;
                loc = new MapLocation(posX, posY);
                type = (msgBytes & 0x00003000) >> 12;
                timestamp = ((msgBytes & 0x0007C000) >> 14) * 10;
            }

            populateFrontline(loc, timestamp, type);
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
                if (roundsSince > frontlineTimestamp[i]) {
                    return;
                }

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

    void messageRobot(MapLocation robotLoc) throws GameActionException {
        switch (rc.senseRobotAtLocation(robotLoc).type) {
            case MOPPER:
                messageBotWithLocation(robotLoc, bestMopperLoc);
                break;
            case SOLDIER:
                messageBotWithLocation(robotLoc, bestSoldierLoc);
                break;
            case SPLASHER:
                messageBotWithLocation(robotLoc, bestSplasherLoc);
                break;
        }
    }

    MapLocation bestSplasherLoc, bestMopperLoc, bestSoldierLoc;
    void determineBestDestinations() {
        int bestSplasherScore = Integer.MIN_VALUE;
        int bestMopperScore = Integer.MIN_VALUE;
        int bestSoldierScore = Integer.MIN_VALUE;

        for (int i=0; i<frontlineLocs.length; i++) {
            if (frontlineLocs[i] == null)
                break;

            int splasherVal = -frontlineLocs[i].distanceSquaredTo(rc.getLocation());
            int mopperVal = splasherVal;
            int soldierVal = mopperVal;

            if (frontlineType[i] == 0) {
                mopperVal += 500;
            }
            else if (frontlineType[i] == 1) {
                splasherVal += 500;
                mopperVal = Integer.MIN_VALUE;
            }
            else if (frontlineType[i] == 2) {
                mopperVal = Integer.MIN_VALUE;
                splasherVal -= 500;
                soldierVal -= 500;
            }
            else if (frontlineType[i] == 3) {
                continue;
            }

            if (splasherVal > bestSplasherScore) {
                bestSplasherScore = splasherVal;
                bestSplasherLoc = frontlineLocs[i];
            }
            if (mopperVal > bestMopperScore) {
                bestMopperScore = mopperVal;
                bestMopperLoc = frontlineLocs[i];
            }
            if (soldierVal > bestSoldierScore) {
                bestSoldierScore = soldierVal;
                bestSoldierLoc = frontlineLocs[i];
            }
        }
    }

    void messageBotWithFrontlineID(MapLocation bot, int i) throws GameActionException {
        int msg = 0;
        msg |= frontlineLocs[i].y;
        msg |= (frontlineLocs[i].x << 6);
        if (frontlineType[i] == 0) {
            msg |= (1 << 31);
        }

        if (rc.canSendMessage(bot))
            rc.sendMessage(bot, msg);
    }

    void messageBotWithLocation(MapLocation bot, MapLocation dest) throws GameActionException {
        if (dest == null || !rc.canSendMessage(bot)) return;
        int msg = 0;
        msg |= dest.y;
        msg |= dest.x << 6;

        rc.sendMessage(bot, msg);
    }

    int chooseNextSpawntype() {
        if (rc.getRoundNum() <= 5) {
            if (rc.getRoundNum() == 1)
                    return 0;
            return 1;
        }

        if ((rc.getPaint() >= 300 || rc.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER)
                && Math.random() < 0.4 * rc.getRoundNum() / (10.0 + rc.getRoundNum())) {
            return 3;
        } else if ((rc.getPaint() >= 200 || rc.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER) &&
                Math.random() < 0.6){
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
                frontlineLocs[49] = null;
                frontlineTimestamp[49] = 0;
                frontlineType[49] = 0;
            }
        }
    }

    void indicateKnown() throws GameActionException {
        for (int i=frontlineLocs.length-1; i>=0; i--) {
            if (frontlineLocs[i] != null) {
                int type = frontlineType[i];
                switch (type) {
                    case 0:
                        rc.setIndicatorDot(frontlineLocs[i], 255, 0, 0);
                        break;
                    case 1:
                        rc.setIndicatorDot(frontlineLocs[i], 255, 255, 0);
                        break;
                    case 2:
                        rc.setIndicatorDot(frontlineLocs[i], 0, 255, 0);
                        break;
                    case 3:
                        rc.setIndicatorDot(frontlineLocs[i], 0, 255, 255);
                        break;
                }
            }
        }
    }

    void endOfTurnRelay() throws GameActionException {
        if (rc.canBroadcastMessage())
            broadcastBirth();

        if (frontlineLocs[0] == null) {
            return;
        }

        int firstBroadcast = lastBroadcasted;
        int broadcasted = 0;
        while (broadcasted++ < 5 && rc.canBroadcastMessage() && Clock.getBytecodesLeft() > 400) {
            lastBroadcasted++;
            if (lastBroadcasted == frontlineLocs.length || frontlineLocs[lastBroadcasted] == null)
                lastBroadcasted = 0;

            if (frontlineLocs[lastBroadcasted].equals(rc.getLocation()))
                continue;

            int msg = 0xC0000000;
            msg |= Math.min(63, frontlineLocs[lastBroadcasted].y);
            msg |= Math.min(63, frontlineLocs[lastBroadcasted].x) << 6;
            msg |= Math.min(3, frontlineType[lastBroadcasted]) << 12;
            msg |= Math.min(31, (frontlineTimestamp[lastBroadcasted] / 10) + 1) << 14;

            rc.broadcastMessage(msg);

            if (lastBroadcasted == firstBroadcast)
                break;
        }
    }

    void endOfTurnMessageUnits() throws GameActionException {
        RobotInfo[] nearRobots = rc.senseNearbyRobots(-1);
        for (int i=nearRobots.length-1; i>=0; i--) {
            if (Math.random() < 0.2)
                messageRobot(nearRobots[i].location);
        }
    }
}
