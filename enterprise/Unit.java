package enterprise;

import battlecode.common.*;

public abstract class Unit extends Robot {
    MapLocation target;
    boolean blockNewTarget = false;
    boolean targetIsRuin = false;

    MapLocation[] lastLocations;
    int lastLocationsID;

    boolean hasRuinPattern;
    int ruinPattern;

    MapLocation[] nearRuins;
    MapInfo[] nearLocations;
    RobotInfo[] nearRobots;
    MapLocation closestRuin;

    public Unit(RobotController rc) throws GameActionException {
        super(rc);
    }

    void turn1() throws GameActionException {
        // find spawn tower
        MapLocation[] ruins = rc.senseNearbyRuins(-1);
        int minDist = 10000;
        for (MapLocation m : ruins) {
            if (m.distanceSquaredTo(rc.getLocation()) < minDist) {
                minDist = m.distanceSquaredTo(rc.getLocation());
                home = m;
            }
        }

        lastLocations = new MapLocation[7];
        for (int i=6; i>=0; i--) {
            lastLocations[i] = rc.getLocation();
        }
        lastLocationsID = 0;
    }

    void initTurn() throws GameActionException {
        nearRuins = rc.senseNearbyRuins(-1);
        nearLocations = rc.senseNearbyMapInfos();
        nearRobots = rc.senseNearbyRobots();
    }

    void endTurn() throws GameActionException {
        lastLocations[lastLocationsID++] = rc.getLocation();
        lastLocationsID %= lastLocations.length;
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

    abstract void play() throws GameActionException;
}
