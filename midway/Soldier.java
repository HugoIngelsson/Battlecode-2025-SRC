package midway;

import battlecode.common.*;

public class Soldier extends Unit {
    private static final int MAX_PAINT = 200;

    MapInfo[] attackPos;
    int srpToFix = Integer.MAX_VALUE;
    boolean formingSRP = false;
    int SRPSearch = -1;
    MapLocation saveSRPLoc = null;

    int lastOccupiedRuinTurn = -11;
    MapLocation lastOccupiedRuin = null;
    public Soldier(RobotController rc) throws GameActionException {
        super(rc);
    }

    void play() throws GameActionException {
        if (formingSRP)
            rc.setIndicatorString("SRPing");

        closestRuin = getClosestRuin();
        if (closestRuin != null) {
            lastRuinSeen = closestRuin;
            ruinTurn = rc.getRoundNum();
        }

        if (formingSRP || rc.getRoundNum() - building < 50){
            closestRuin = null;
        }

        rc.setIndicatorDot(lastPainTower, 0, 0, 255);
        if (rc.getPaint() < Math.max(Math.sqrt(rc.getLocation().distanceSquaredTo(lastPainTower)) / 2, 15)) {
            target = lastPainTower;
            targetIsRuin = false;
            if (!retreating) {
                storeMessage();

                if (formingSRP) {
                    saveSRPLoc = rc.getLocation();
                    formingSRP = false;
                }
            }

            if (rc.getLocation().distanceSquaredTo(target) < 2) {
                RobotInfo tower = rc.senseRobotAtLocation(target);
                if (tower != null) {
                    if (tower.getPaintAmount() < 20 && rc.senseMapInfo(rc.getLocation()).getPaint().isAlly()){
                        target = rc.getLocation();
                    } else if (rc.isActionReady()) {
                        rc.transferPaint(target, -1 * Math.min(MAX_PAINT*2/3 - rc.getPaint(), tower.getPaintAmount()));
                    }
                } else {
                    if (semiLastPaintTower != null) {
                        lastPainTower = semiLastPaintTower;
                        semiLastPaintTower = null;
                    } else lastPainTower = home;

                    target = lastPainTower;
                }
            }
        }
        else if (closestRuin != null && SRPSearch < 0) {
            target = closestRuin;
            targetIsRuin = true;

            ruinPattern = decodeMarkings(target);
            if (ruinPattern == -1) {
                hasRuinPattern = false;
                ruinPattern = GameConstants.MONEY_TOWER_PATTERN;
            } else {
                hasRuinPattern = true;
            }
            retreating = false;
        }
        else if (((srpToFix = neededForSRP()) <= rc.getPaint() * 10 ||
                rc.getRoundNum() < 50 && building > -51 && srpToFix <= 25) &&
                srpToFix > 0) {
            target = rc.getLocation();
            formingSRP = true;

            if (rc.senseMapInfo(target).getMark() == PaintType.EMPTY)
                rc.mark(rc.getLocation(), false);
            retreating = false;

        }
        else if (saveSRPLoc != null) { // go back to finish what you started
            target = saveSRPLoc;
        }
        else if (!blockNewTarget) { // roam randomly
            int x = rng.nextInt(rc.getMapWidth());
            int y = rng.nextInt(rc.getMapHeight());
            target = new MapLocation(x, y);
            blockNewTarget = true;
            targetIsRuin = false;
            retreating = false;
        } else {
            retreating = false;
        }

        if (srpToFix == Integer.MAX_VALUE)
            formingSRP = false;

        if (targetIsRuin && rc.isMovementReady() &&
                rc.getLocation().distanceSquaredTo(target) == 1) {
            if (rc.senseMapInfo(target.add(Direction.NORTH)).getMark() == PaintType.EMPTY) {

                if (rng.nextInt(100) < 50 || rc.getRoundNum() < 30) {
                    markRuin(target, UnitType.LEVEL_ONE_MONEY_TOWER);
                    System.out.println("Marked a ruin for a money tower");
                } else {
                    markRuin(target, UnitType.LEVEL_ONE_PAINT_TOWER);
                    System.out.println("Marked a ruin for a paint tower");
                }
            }
        }

        if (blockNewTarget && rc.getLocation().distanceSquaredTo(target) < 15 &&
                SRPSearch-- < 0) {
            blockNewTarget = false;
        }


        if (saveSRPLoc != null && rc.onTheMap(saveSRPLoc))
            rc.setIndicatorDot(target, 255, 255, 0);
        if (saveSRPLoc != null && rc.getLocation().equals(saveSRPLoc) &&
                rc.getPaint() > Math.max(Math.sqrt(rc.getLocation().distanceSquaredTo(lastPainTower)) / 2, 15)) {
            saveSRPLoc = null;
        }

        if (rc.onTheMap(target))
            rc.setIndicatorDot(target, 255, 0, 0);

        if (rc.isMovementReady()) {
            Direction bestDir = considerMoves();
            rc.setIndicatorDot(rc.getLocation().add(bestDir), 255, 0, 0);

            if (bestDir != Direction.CENTER && !formingSRP)
                rc.move(bestDir);
        }

        attackPos = rc.senseNearbyMapInfos(9);
        if (rc.isActionReady() &&
                rc.getPaint() >= Math.max(Math.sqrt(rc.getLocation().distanceSquaredTo(lastPainTower)) / 2, 15)) {
            MapLocation paintLoc = findBestPaintLoc();

            if (paintLoc != null) {
                boolean secondary;
                if (closestRuin != null &&
                        Math.abs(paintLoc.x - closestRuin.x) <= 2 &&
                        Math.abs(paintLoc.y - closestRuin.y) <= 2) {
                    int relX = paintLoc.x - closestRuin.x + 2;
                    int relY = paintLoc.y - closestRuin.y + 2;

                    secondary = (ruinPattern & (1 << (relX + 5*relY))) > 0;
                } else if (formingSRP &&
                        Math.abs(paintLoc.x - target.x) <= 2 &&
                        Math.abs(paintLoc.y - target.y) <= 2) {
                    int relX = paintLoc.x - target.x + 2;
                    int relY = paintLoc.y - target.y + 2;

                    secondary = (GameConstants.RESOURCE_PATTERN & (1 << (relX + 5*relY))) > 0;
                } else {
                    secondary = (paintLoc.x + paintLoc.y) % 2 == 0;
                    int x = paintLoc.x, y = paintLoc.y;
                    while (x > 0) {
                        x -= 3;
                        y -= 1;
                    }

                    y %= 10;
                    y += 10;
                    y %= 10;
                    if (y % 3 == 0 && x + y / 3 == 0)
                        secondary = false;
                }

                rc.attack(paintLoc, secondary);
                if (formingSRP) {
                    try {
                        rc.completeResourcePattern(rc.getLocation());
                        rc.mark(rc.getLocation(), true);

                        SRPSearch = 10;
                        blockNewTarget = true;
                        target = new MapLocation(target.x-1, target.y+3);
                        saveSRPLoc = null;
                        formingSRP = false;
                    }
                    catch (GameActionException e) { ; }
                }
            }
        }

        try {
            // sensing whether it's possible is just as expensive
            // so no reason not to try
            switch (ruinPattern) {
                case GameConstants.PAINT_TOWER_PATTERN:
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, target);
                    break;
                case GameConstants.DEFENSE_TOWER_PATTERN:
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, target);
                    break;
                case GameConstants.MONEY_TOWER_PATTERN:
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, target);
                    break;
            }

            unmarkRuin(target);
        } catch (GameActionException e) {
            if (rc.getNumberTowers() >= GameConstants.MAX_NUMBER_OF_TOWERS) { // apparently a thing
                building = rc.getRoundNum();
            }
        }

        unstuckify();
    }

    MapLocation findBestPaintLoc() throws GameActionException {
        MapLocation ret = null;
        int bestVal = Integer.MIN_VALUE;

        for (MapInfo m : attackPos) {
            if (!m.isPassable()) {
                RobotInfo robotAt = rc.senseRobotAtLocation(m.getMapLocation());
                if (m.hasRuin() && robotAt != null && robotAt.getTeam() != rc.getTeam()) {
                    return m.getMapLocation();
                } else continue;
            }

            rc.setIndicatorDot(m.getMapLocation(), 0, 255, 0);
            MapLocation loc = m.getMapLocation();
            if (m.getPaint() == PaintType.EMPTY) {
                if (closestRuin != null && hasRuinPattern &&
                        (Math.abs(loc.x - closestRuin.x) > 2 ||
                                Math.abs(loc.y - closestRuin.y) > 2))
                    continue;

                int val = -5 * loc.distanceSquaredTo(target);
                val -= loc.distanceSquaredTo(home);
                if (Math.abs(loc.x - target.x) <= 2 &&
                        Math.abs(loc.y - target.y) <= 2) {
                    val += 200;
                }
                if (loc.equals(rc.getLocation()))
                    val += 135;

                if (val > bestVal) {
                    bestVal = val;
                    ret = loc;
                }
            } else if (m.getPaint() == PaintType.ALLY_PRIMARY || m.getPaint() == PaintType.ALLY_SECONDARY) {
                if (closestRuin != null && hasRuinPattern &&
                        Math.abs(loc.x - closestRuin.x) <= 2 &&
                        Math.abs(loc.y - closestRuin.y) <= 2) {

                    int relX = loc.x - closestRuin.x + 2;
                    int relY = loc.y - closestRuin.y + 2;

                    boolean secondary = (ruinPattern & (1 << (relX + 5*relY))) > 0;

                    if ((m.getPaint() == PaintType.ALLY_SECONDARY) ^ secondary) {
                        if (200 > bestVal) {
                            bestVal = 200;
                            ret = loc;
                        }
                    }
                } else if (formingSRP && Math.abs(loc.x - target.x) <= 2 &&
                        Math.abs(loc.y - target.y) <= 2) {
                    int relX = loc.x - target.x + 2;
                    int relY = loc.y - target.y + 2;

                    boolean secondary = (GameConstants.RESOURCE_PATTERN & (1 << (relX + 5*relY))) > 0;

                    if (!m.getPaint().isAlly()) {
                        if (2000 > bestVal) {
                            bestVal = 2000;
                            ret = loc;
                        }
                    }
                    else if (m.getPaint() == PaintType.ALLY_SECONDARY ^ secondary) {
                        if (1000 > bestVal) {
                            bestVal = 1000;
                            ret = loc;
                        }
                    }
                }
            }
        }

        return ret;
    }

    Direction considerMoves() throws GameActionException {
        int maxVal = Integer.MIN_VALUE;
        Direction ret = Direction.CENTER;

        for (Direction d : RobotPlayer.directions) {
            int numEnemyTowers = 0;
            int numEnemyMoppers = 0;
            int numAllies = 0;
            MapLocation dest = rc.getLocation().add(d);
            if (rc.canMove(d)) {
                for (int i=nearRobots.length-1; i>=0; i--) {
                    if (nearRobots[i].team == rc.getTeam()) {
                        if (nearRobots[i].getLocation().distanceSquaredTo(dest) <= 10)
                            numAllies++;
                    } else if (nearRobots[i].getType() == UnitType.MOPPER) {
                        if (nearRobots[i].getLocation().distanceSquaredTo(dest) <= 4)
                            numEnemyMoppers++;
                    } else if (nearRobots[i].getType() != UnitType.SOLDIER &&
                            nearRobots[i].getType() != UnitType.SPLASHER) {
                        if (nearRobots[i].getLocation().distanceSquaredTo(dest) <= 9)
                            numEnemyTowers++;
                        else if (nearRobots[i].getType() == UnitType.LEVEL_ONE_DEFENSE_TOWER &&
                                nearRobots[i].getLocation().distanceSquaredTo(dest) <= 20) {
                            numEnemyTowers += 2;
                        }
                    }
                }

                MapInfo destInfo = rc.senseMapInfo(dest);
                int val = dest.distanceSquaredTo(target) * -3;
                if (!destInfo.getPaint().isAlly()) {
                    if (destInfo.getPaint() == PaintType.EMPTY) {
                        if (!rc.isActionReady())
                            val -= 5;
                    } else {
                        val -= 10;
                    }
                } else if (rc.getPaint() < 20) {
                    val += 50;
                }

                for (int i = 6; i >= 0; i--) {
                    if (dest.equals(lastLocations[i])) {
                        val -= 20;
                        break;
                    }
                }

                val += numAllies - numEnemyMoppers;
                if (!rc.isActionReady())
                    val -= numEnemyTowers * 30;

                if (val > maxVal) {
                    maxVal = val;
                    ret = d;
                }
            }
        }

        return ret;
    }

    MapLocation getClosestRuin() throws GameActionException {
        int minDist = Integer.MAX_VALUE;
        MapLocation ret = null;

        for (MapLocation m : nearRuins) {
            RobotInfo ri = rc.senseRobotAtLocation(m);
            if (ri == null && minDist > m.distanceSquaredTo(rc.getLocation()) &&
                    (m != lastOccupiedRuin || lastOccupiedRuinTurn + 10 > rc.getRoundNum())) {
                if (rc.getLocation().distanceSquaredTo(m) > 2 && rc.getRoundNum() > 30 && (
                        rc.canSenseRobotAtLocation(m.add(Direction.NORTH)) && rc.senseRobotAtLocation(m.add(Direction.NORTH)).getTeam() == rc.getTeam() ||
                        rc.canSenseRobotAtLocation(m.add(Direction.SOUTH)) && rc.senseRobotAtLocation(m.add(Direction.SOUTH)).getTeam() == rc.getTeam() ||
                        rc.canSenseRobotAtLocation(m.add(Direction.WEST)) && rc.senseRobotAtLocation(m.add(Direction.WEST)).getTeam() == rc.getTeam() ||
                        rc.canSenseRobotAtLocation(m.add(Direction.EAST)) && rc.senseRobotAtLocation(m.add(Direction.EAST)).getTeam() == rc.getTeam())
                ) {
                    lastOccupiedRuinTurn = rc.getRoundNum();
                    lastOccupiedRuin = m;
                } else {
                    minDist = m.distanceSquaredTo(rc.getLocation());
                    ret = m;
                }
            }
            else if (ri != null) {
                if (rc.canSendMessage(ri.getLocation()) && retreating) {
                    rc.sendMessage(ri.getLocation(), constructMessage(rc.getRoundNum() % 2 == 0));
                }

                // मैं दुनिया पर राज करता था। जब मैं वचन देता तो आंखें उठ जातीं!!

                UnitType tp = ri.getType();
                if (tp == UnitType.LEVEL_ONE_PAINT_TOWER ||
                        tp == UnitType.LEVEL_THREE_PAINT_TOWER ||
                        tp == UnitType.LEVEL_TWO_PAINT_TOWER) {
                    if (ri.getLocation() != this.lastPainTower && ri.getTeam() == rc.getTeam()) {
                        this.semiLastPaintTower = this.lastPainTower;
                        this.lastPainTower = ri.getLocation();
                    }
                }

                if (ri.getTeam() != rc.getTeam()){
                    lastEnemyTower = ri.getLocation();
                    lastEnemyTowerType = tp;
                    enemyTowerTurn = rc.getRoundNum();
                }
            }
        }

        return ret;
    }

    int[][] SRPDeltas = {
            { 0, 3}, { 1, 4}, { 2, 3}, { 3, 2}, { 4, 1},
            { 3, 0}, { 4,-1}, { 3,-2}, { 2,-3}, { 1,-4},
            { 0,-3}, {-1,-4}, {-2,-3}, {-3,-2}, {-4,-1},
            {-3, 0}, {-4, 1}, {-3, 2}, {-2, 3}, {-1, 4},
    };

    int[][] extraGoodFittings = {
            { 1, 3}, { 3, 1},
            {-1, 3}, {-3, 1},
            {-1,-3}, {-3,-1},
            { 1,-3}, { 3,-1},
    };

    // returns the number of squares needed to change for SRP
    // will return INTEGER.MAX_VALUE if this would ruin another SRP (eventually)
    // costs: ~4000 bytecode
    int neededForSRP() throws GameActionException {
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        int cnt = 0;

        for (int i=19; i>=0; i--) {
            MapLocation loc = new MapLocation(x+SRPDeltas[i][0], y+SRPDeltas[i][1]);
            if (!rc.onTheMap(loc))
                continue;

            MapInfo mi = rc.senseMapInfo(loc);
            if (mi.getMark() == PaintType.ALLY_SECONDARY ||
                    mi.getMark() == PaintType.ALLY_PRIMARY && rc.canSenseLocation(loc))
                return Integer.MAX_VALUE;
        }

        for (int i=-2; i<=2; i++) {
            for (int j=-2; j<=2; j++) {
                MapLocation loc = new MapLocation(x+i, y+j);
                if (!rc.onTheMap(loc)) return Integer.MAX_VALUE;

                MapInfo mi = rc.senseMapInfo(loc);
                if (mi.getPaint().isEnemy() || !mi.isPassable() ||
                        mi.getMark() == PaintType.ALLY_PRIMARY && rc.canSenseRobotAtLocation(loc) &&
                        (i != 0 || j != 0))
                    return Integer.MAX_VALUE;

                if (mi.getMark() == PaintType.ALLY_SECONDARY) {
                    if (rc.canSenseRobotAtLocation(loc.add(Direction.NORTH)) && rc.senseMapInfo(loc.add(Direction.NORTH)).hasRuin() ||
                            rc.canSenseRobotAtLocation(loc.add(Direction.SOUTH)) && rc.senseMapInfo(loc.add(Direction.SOUTH)).hasRuin() ||
                            rc.canSenseRobotAtLocation(loc.add(Direction.EAST)) && rc.senseMapInfo(loc.add(Direction.EAST)).hasRuin() ||
                            rc.canSenseRobotAtLocation(loc.add(Direction.WEST)) && rc.senseMapInfo(loc.add(Direction.WEST)).hasRuin()) {
                        // do nothing
                    } else return Integer.MAX_VALUE;
                }

                int relX = i + 2;
                int relY = j + 2;

                boolean secondary = (GameConstants.RESOURCE_PATTERN & (1 << (relX + 5*relY))) > 0;
                if (!mi.getPaint().isAlly() ||
                        (mi.getPaint() == PaintType.ALLY_SECONDARY ^ secondary))
                    cnt++;
            }
        }

        if (cnt > 0) {
            // good to complete a mark
            if (rc.senseMapInfo(rc.getLocation()).getMark() == PaintType.ALLY_PRIMARY)
                cnt -= 2;

            // these patterns fit well (compact)
            for (int i=7; i>=0; i--) {
                MapLocation loc = new MapLocation(x+extraGoodFittings[i][0], y+extraGoodFittings[i][1]);
                if (!rc.onTheMap(loc))
                    continue;

                if (rc.senseMapInfo(loc).getMark() == PaintType.ALLY_SECONDARY)
                    cnt -= 5;
            }

            cnt = Math.max(1, cnt);
        }

        return cnt;
    }

    @Override
    void initTurn() throws GameActionException {
        super.initTurn();
    }

    @Override
    void endTurn() throws GameActionException {
        super.endTurn();
    }
}
