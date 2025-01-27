package xianyang;

import battlecode.common.*;

public abstract class Unit extends Robot {
    MapLocation target;
    boolean blockNewTarget = false;
    boolean targetIsRuin = false;
    boolean stuck = false;
    Team team;
    MapLocation currentLocation;
    MapLocation attackTarget;

    MapLocation[] lastLocations;
    int lastLocationsID;

    boolean hasRuinPattern;
    int ruinPattern;
    int building = -51;

    MapLocation saveSRPLoc = null;
    MapLocation saveRuinLoc = null;
    MapLocation directive = null;

    MapLocation[] nearRuins;
    MapInfo[] nearLocations;
    RobotInfo[] nearRobots;
    MapLocation closestRuin;
    MapLocation lastRuinSeen;


    boolean retreating = false;
    int enemySplashers = 0;
    int enemyMoppers = 0;
    int enemySoldiers = 0;
    MapLocation locForMsg;
    double tileRatio = 0;
    int msgTurn = 0;
    int friendlyUnits = 0;
    int ruinTurn = 0;
    int enemyTowerTurn = 0;
    MapLocation lastFriendlyTower = null;
    int friendlyTowerTurn = 0;

    boolean broadcastSwitch = false;
    int spawnTurn;

    public Unit(RobotController rc) throws GameActionException {
        super(rc);
    }

    void turn1() throws GameActionException {
        spawnTurn = (int)(0.5 * rc.getRoundNum());

        // find spawn tower
        MapLocation[] ruins = rc.senseNearbyRuins(-1);
        int minDist = Integer.MAX_VALUE;
        for (MapLocation m : ruins) {
            if (m.distanceSquaredTo(rc.getLocation()) < minDist) {
                minDist = m.distanceSquaredTo(rc.getLocation());
                home = m;
            }
        }

        lastLocations = new MapLocation[7];
        for (int i = 6; i >= 0; i--) {
            lastLocations[i] = rc.getLocation();
        }
        lastLocationsID = 0;

        lastPainTower = home;

        Message[] messages = rc.readMessages(rc.getRoundNum()-1);
        if (messages.length > 0) {
            int msg = messages[0].getBytes();
            directive = new MapLocation((msg & 0xFC0) >> 6, msg & 0x3F);
            blockNewTarget = true;
        }

        if (directive == null) {
            MapLocation ideal = target = rc.getLocation().translate(
                    home.directionTo(rc.getLocation()).dx * 10,
                    home.directionTo(rc.getLocation()).dy * 10);
            if (rc.onTheMap(home)) {
                target = ideal;
                blockNewTarget = true;
            } else {
                MapLocation center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
                target = rc.getLocation().translate(
                        rc.getLocation().directionTo(center).dx * 10,
                        rc.getLocation().directionTo(center).dy * 10);
                blockNewTarget = true;
            }
        }

        team = rc.getTeam();
    }

    void initTurn() throws GameActionException {
        nearRuins = rc.senseNearbyRuins(-1);
        nearLocations = rc.senseNearbyMapInfos();
        nearRobots = rc.senseNearbyRobots();
        currentLocation = rc.getLocation();
        readMessages();

        if (target != null && !rc.onTheMap(target)) {
            target = null;
            blockNewTarget = false;
        }
    }

    void endTurn() throws GameActionException {
        lastLocations[lastLocationsID++] = rc.getLocation();
        lastLocationsID %= lastLocations.length;

        if (rc.getPaint() == 0)
            rc.disintegrate();

        if (lastEnemyTower != null)
            rc.setIndicatorDot(lastEnemyTower, 0, 255, 255);
    }

    void markRuin(MapLocation loc, UnitType type) throws GameActionException {
        boolean mark1, mark2;
        switch (type) {
            case LEVEL_ONE_DEFENSE_TOWER:
            case LEVEL_TWO_DEFENSE_TOWER:
            case LEVEL_THREE_DEFENSE_TOWER:
                mark1 = true;
                mark2 = false;
                break;
            case LEVEL_ONE_MONEY_TOWER:
            case LEVEL_TWO_MONEY_TOWER:
            case LEVEL_THREE_MONEY_TOWER:
                mark1 = true;
                mark2 = true;
                break;
            case LEVEL_ONE_PAINT_TOWER:
            case LEVEL_TWO_PAINT_TOWER:
            case LEVEL_THREE_PAINT_TOWER:
                mark1 = false;
                mark2 = false;
                break;
            default: // shouldn't happen
                return;
        }

        switch (rc.getLocation().directionTo(loc)) {
            case EAST:
                rc.mark(loc.add(Direction.NORTH), mark1);
                rc.mark(loc.add(Direction.WEST), mark2);
                rc.mark(loc.add(Direction.SOUTH), mark1);

                if (rc.canMove(Direction.SOUTHEAST))
                    rc.move(Direction.SOUTHEAST);
                else if (rc.canMove(Direction.NORTHEAST))
                    rc.move(Direction.NORTHEAST);
                else return;

                rc.mark(loc.add(Direction.EAST), mark2);

                break;
            case WEST:
                rc.mark(loc.add(Direction.NORTH), mark1);
                rc.mark(loc.add(Direction.EAST), mark2);
                rc.mark(loc.add(Direction.SOUTH), mark1);

                if (rc.canMove(Direction.SOUTHWEST))
                    rc.move(Direction.SOUTHWEST);
                else if (rc.canMove(Direction.NORTHWEST))
                    rc.move(Direction.NORTHWEST);
                else return;

                rc.mark(loc.add(Direction.WEST), mark2);
                break;
            case NORTH:
                rc.mark(loc.add(Direction.EAST), mark2);
                rc.mark(loc.add(Direction.WEST), mark2);
                rc.mark(loc.add(Direction.SOUTH), mark1);

                if (rc.canMove(Direction.NORTHEAST))
                    rc.move(Direction.NORTHEAST);
                else if (rc.canMove(Direction.NORTHWEST))
                    rc.move(Direction.NORTHWEST);
                else return;

                rc.mark(loc.add(Direction.NORTH), mark1);
                break;
            case SOUTH:
                rc.mark(loc.add(Direction.NORTH), mark1);
                rc.mark(loc.add(Direction.EAST), mark2);
                rc.mark(loc.add(Direction.WEST), mark2);

                if (rc.canMove(Direction.SOUTHEAST))
                    rc.move(Direction.SOUTHEAST);
                else if (rc.canMove(Direction.SOUTHWEST))
                    rc.move(Direction.SOUTHWEST);
                else return;

                rc.mark(loc.add(Direction.SOUTH), mark1);
                break;
        }
    }

    void unmarkRuin(MapLocation loc) throws GameActionException {
        switch (rc.getLocation().directionTo(loc)) {
            case EAST:
                rc.removeMark(loc.add(Direction.NORTH));
                rc.removeMark(loc.add(Direction.WEST));
                rc.removeMark(loc.add(Direction.SOUTH));

                if (rc.canMove(Direction.SOUTHEAST))
                    rc.move(Direction.SOUTHEAST);
                else if (rc.canMove(Direction.NORTHEAST))
                    rc.move(Direction.NORTHEAST);
                else return;

                rc.removeMark(loc.add(Direction.EAST));
                break;
            case WEST:
                rc.removeMark(loc.add(Direction.NORTH));
                rc.removeMark(loc.add(Direction.SOUTH));
                rc.removeMark(loc.add(Direction.EAST));

                if (rc.canMove(Direction.SOUTHWEST))
                    rc.move(Direction.SOUTHWEST);
                else if (rc.canMove(Direction.NORTHWEST))
                    rc.move(Direction.NORTHWEST);
                else return;

                rc.removeMark(loc.add(Direction.WEST));
                break;
            case NORTH:
                rc.removeMark(loc.add(Direction.WEST));
                rc.removeMark(loc.add(Direction.SOUTH));
                rc.removeMark(loc.add(Direction.EAST));

                if (rc.canMove(Direction.NORTHEAST))
                    rc.move(Direction.NORTHEAST);
                else if (rc.canMove(Direction.NORTHWEST))
                    rc.move(Direction.NORTHWEST);
                else return;

                rc.removeMark(loc.add(Direction.NORTH));
                break;
            case SOUTH:
                rc.removeMark(loc.add(Direction.NORTH));
                rc.removeMark(loc.add(Direction.WEST));
                rc.removeMark(loc.add(Direction.EAST));

                if (rc.canMove(Direction.SOUTHEAST))
                    rc.move(Direction.SOUTHEAST);
                else if (rc.canMove(Direction.SOUTHWEST))
                    rc.move(Direction.SOUTHWEST);
                else return;

                rc.removeMark(loc.add(Direction.SOUTH));
                break;
        }
    }

    void remarkAsDefense(MapLocation ml) throws GameActionException {
        if (rc.getLocation().distanceSquaredTo(ml) > 1)
            return;

        if (rc.getLocation().directionTo(ml) == Direction.WEST || rc.getLocation().directionTo(ml) == Direction.EAST) {
            boolean secondary = rc.senseMapInfo(ml.add(Direction.NORTH)).getMark() == PaintType.ALLY_PRIMARY;
            if ((rc.senseMapInfo(ml.add(Direction.WEST)).getMark() == PaintType.ALLY_PRIMARY) != secondary)
                return;

            rc.mark(ml.add(Direction.NORTH), secondary);
            rc.mark(ml.add(Direction.SOUTH), secondary);
        } else {
            boolean secondary = rc.senseMapInfo(ml.add(Direction.EAST)).getMark() == PaintType.ALLY_PRIMARY;
            if ((rc.senseMapInfo(ml.add(Direction.SOUTH)).getMark() == PaintType.ALLY_PRIMARY) != secondary)
                return;

            rc.mark(ml.add(Direction.EAST), secondary);
            rc.mark(ml.add(Direction.WEST), secondary);
        }
    }

    int decodeMarkings(MapLocation loc) throws GameActionException {
        boolean northSouth, eastWest, unmarked;
        if (rc.canSenseLocation(loc.add(Direction.NORTH))) {
            northSouth = rc.senseMapInfo(loc.add(Direction.NORTH)).getMark() == PaintType.ALLY_SECONDARY;
            unmarked = rc.senseMapInfo(loc.add(Direction.NORTH)).getMark() == PaintType.EMPTY;
        } else {
            northSouth = rc.senseMapInfo(loc.add(Direction.SOUTH)).getMark() == PaintType.ALLY_SECONDARY;
            unmarked = rc.senseMapInfo(loc.add(Direction.SOUTH)).getMark() == PaintType.EMPTY;
        }

        if (rc.canSenseLocation(loc.add(Direction.EAST))) {
            eastWest = rc.senseMapInfo(loc.add(Direction.EAST)).getMark() == PaintType.ALLY_SECONDARY;
        } else {
            eastWest = rc.senseMapInfo(loc.add(Direction.WEST)).getMark() == PaintType.ALLY_SECONDARY;
        }

        if (unmarked) return -1;
        else if (northSouth && eastWest) return GameConstants.MONEY_TOWER_PATTERN;
        else if (northSouth || eastWest) return GameConstants.DEFENSE_TOWER_PATTERN;
        else return GameConstants.PAINT_TOWER_PATTERN;
    }

    /**
     * Store da message
     * input: nada
     * output: सोमेरत
     */
    void storeMessage() {
        retreating = true;
        for(RobotInfo ri: nearRobots){
            if(ri.getTeam() == team){
                friendlyUnits++;
            } else {
                switch (ri.getType()) {
                    case UnitType.SOLDIER:
                        enemySoldiers++;
                        break;
                    case UnitType.SPLASHER:
                        enemySplashers++;
                        break;
                    case UnitType.MOPPER:
                        enemyMoppers++;
                        break;
                    default:
                        break;
                }
            }
        }
        locForMsg = currentLocation;
        int friendly = 0;
        int unfriendly = 0;
        for (MapInfo ti: nearLocations) {
            if (ti.getPaint().isAlly()) {
                friendly++;
            } else if (ti.getPaint().isEnemy()) {
                unfriendly++;
            }
        }
        tileRatio = (double) unfriendly / friendly;
        msgTurn = rc.getRoundNum();
    }

    /**
     * Change da world
     * My Final Message
     * Goodb ye
     *
     * @return emotional music and they were singin' bye bye miss american pie drove the chevy to the levy but the
     */
    int constructMessage(boolean towerMsg) throws GameActionException {
//        int msgs[] = new int[2];
        int msg = 0;
        if(!towerMsg) {
            if (lastPainTower != null && broadcastSwitch) {
                // imitate being the tower
                msg |= Math.min(lastPainTower.y, 63);
                msg |= Math.min(lastPainTower.x, 63) << 6;
                msg |= 3 << 12; // ally
                msg |= 31 << 14;

                msg |= 1 << 31;
                msg |= 1 << 30;
            } else {
                msg |= Math.min(home.y, 63);
                msg |= Math.min(home.x, 63) << 6;
                msg |= 3 << 12; // ally
                msg |= 31 << 14; // make it have happened a long time ago

                msg |= 1 << 31;
                msg |= 1 << 30;
            }

            broadcastSwitch = !broadcastSwitch;
        } else {
            if (broadcastSwitch && lastFriendlyTower != null) {
                msg |= Math.min(lastFriendlyTower.y, 63);
                msg |= Math.min(lastFriendlyTower.x, 63) << 6;
                msg |= 3 << 12; // ally
                msg |= Math.min((rc.getRoundNum() - friendlyTowerTurn) / 10, 31) << 14;

                msg |= 1 << 31;
                msg |= 1 << 30;
            }
            // there's something going on here
            else if (ruinTurn > enemyTowerTurn) {
                msg |= Math.min(lastRuinSeen.y, 63);
                msg |= Math.min(lastRuinSeen.x, 63) << 6;
                msg |= Math.min((rc.getRoundNum() - ruinTurn) / 10, 31) << 14;
            } else if (lastEnemyTower != null) {
                // Jag får en kyss
                int typ;
                switch (lastEnemyTowerType) {
                    case LEVEL_ONE_PAINT_TOWER:
                    case LEVEL_TWO_PAINT_TOWER:
                    case LEVEL_THREE_PAINT_TOWER:
                    case LEVEL_ONE_MONEY_TOWER:
                    case LEVEL_TWO_MONEY_TOWER:
                    case LEVEL_THREE_MONEY_TOWER:
                        typ = 1;
                        break;
                    case LEVEL_ONE_DEFENSE_TOWER:
                    case LEVEL_TWO_DEFENSE_TOWER:
                    case LEVEL_THREE_DEFENSE_TOWER:
                        typ = 2;
                        break;
                    default:
                        typ = -1;
                        break;
                }
                rc.setIndicatorDot(lastEnemyTower, 0, 255, 255);
                msg |= Math.min(lastEnemyTower.y, 63);
                msg |= Math.min(lastEnemyTower.x, 63) << 6;
                msg |= typ << 12;
                msg |= Math.min((rc.getRoundNum() - enemyTowerTurn) / 10, 31) << 14;
            }

            msg |= 1 << 31;
        }

        return msg;
    }

    /**
     * ଦୟାକରି ମୋତେ ବଞ୍ଚାଅ (^_^)
     *
     * @throws GameActionException
     */
    void unstuckify() throws GameActionException {
        int dupLocations = 0;
        for (int i=6; i>=0; i--) {
            if (rc.getLocation().equals(lastLocations[i]))
                dupLocations++;
        }

        if (dupLocations > 0 && target != null && rc.getLocation().distanceSquaredTo(target) > 5) {
            stuck = true;
            PathFinder.target = target;
        }
    }

    abstract void play() throws GameActionException;

    void readMessages() {
        if (directive != null) return;

        Message[] messages = rc.readMessages(rc.getRoundNum()-1);
        if (messages.length > 0) {
            int msg = messages[0].getBytes();
            directive = new MapLocation((msg & 0xFC0) >> 6, msg & 0x3F);
            if (directive.x == 0 && directive.y == 0) directive = null;
            else blockNewTarget = true;
        }
    }

    MapLocation randomTarget() throws GameActionException {
        Direction dir = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
        if (dir == Direction.CENTER) dir = Direction.NORTH;
        double rng = Math.random();
        if (rng < 0.2) {
            dir = dir.rotateRight().rotateRight();
        } else if (rng < 0.4) {
            dir = dir.rotateRight();
        } else if (rng < 0.6) {
            dir = dir.rotateLeft();
        } else if (rng < 0.8) {
            dir = dir.rotateLeft().rotateLeft();
        }

        return rc.getLocation().translate(dir.dx * 40, dir.dy * 40);
    }

    boolean closeToBorder() throws GameActionException {
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        return (x <= 2 || y <= 2 || x >= rc.getMapWidth()-3 || y >= rc.getMapHeight()-3);
    }
}
