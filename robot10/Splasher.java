package robot10;

import battlecode.common.*;

public class Splasher extends Unit {
    private static final int MAX_PAINT = 300;
    MapInfo[] attackPos;

    public Splasher(RobotController rc) throws GameActionException {
        super(rc);
    }

    void play() throws GameActionException {
        closestRuin = getClosestRuin();

        if (rc.getPaint() < 50) {
            target = lastPainTower;
        }
        else if (target == null) {
            int x = rng.nextInt(rc.getMapWidth());
            int y = rng.nextInt(rc.getMapHeight());
            target = new MapLocation(x, y);
        }

        Direction bestDirMove = considerMoves();
        if (rc.canMove(bestDirMove)) {
            rc.move(bestDirMove);
        }

        if (target != lastPainTower && rc.getLocation().distanceSquaredTo(target) < 15) {
            target = null;
        }

        if (rc.isActionReady() && rc.getPaint() > 50) {
            attackPos = rc.senseNearbyMapInfos(4);
            nearLocations = rc.senseNearbyMapInfos();
            MapLocation paintLoc = findBestPaintLoc2();
            if (paintLoc != null) {
                rc.attack(paintLoc);
            }
        } else if (rc.isActionReady() && rc.getLocation().distanceSquaredTo(lastPainTower) <= 2) {
            RobotInfo ri = rc.senseRobotAtLocation(lastPainTower);
            if (ri == null) { // replace at some point, just temp fix
                if (semiLastPaintTower != null) {
                    lastPainTower = semiLastPaintTower;
                    semiLastPaintTower = null;
                } else {
                    lastPainTower = home;
                }
            }
            else if (ri.getTeam() == rc.getTeam()) {
                rc.transferPaint(lastPainTower, -1 * Math.min(MAX_PAINT - rc.getPaint(), ri.getPaintAmount()));
            }
        }
    }

    static int[][] deltasClose = {
            {-1,-1}, {0,-1}, {1,-1},
            {-1,0}, {0,0}, {1,0},
            {-1,1}, {0,1}, {1,1}
    };
    static int[][] deltasFar = {
            {-2, 0}, {0, 2}, {2, 0}, {0, -2}
    };
    MapLocation findBestPaintLoc2() throws GameActionException {
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        long enemyMask = 0L, neutralMask = 0L, allyMask = 0L;
        for (int j=nearLocations.length-1; j>=0; j--) {
            MapInfo m = nearLocations[j];
            int relX = x - m.getMapLocation().x + 3;
            int relY = m.getMapLocation().y - y + 3;
            if (relX < 0 || relX > 6 || relY < 0 || relY > 6) continue;
            if (m.getPaint().isEnemy()) {
                // this is what we're here for
                enemyMask |= 1L << (relX + relY*8);
            } else if (!m.getPaint().isAlly()) {
                // capturing neutral tiles is alright but not the main focus of splashers
                neutralMask |= 1L << (relX + relY*8);
            } else if (m.hasRuin()) {
                RobotInfo robotAt = rc.senseRobotAtLocation(m.getMapLocation());
                if (robotAt != null && robotAt.getTeam() != rc.getTeam()) {
                    // attacking towers is good
                    enemyMask |= 1L << (relX + relY*8);
                }
            } else {
                // repainting our own tiles is inefficient + might ruin patterns
                allyMask |= 1L << (relX + relY*8);
            }
        }

        MapLocation ret = null;
        int bestVal = Integer.MIN_VALUE;
        long fullArea = 8917394130944L, partialArea = 120730683392L;
        for (int j=attackPos.length-1; j>=0; j--) {
            MapInfo m = attackPos[j];
            if (!m.isPassable())
                continue;

            int relX = x - m.getMapLocation().x;
            int relY = m.getMapLocation().y - y;
            int shift = relX + relY*8;
            long fullMask = 0L, partialMask = 0L;
            if (shift > 0) {
                fullMask = fullArea << shift;
                partialMask = partialArea << shift;
            } else if (shift < 0) {
                fullMask = fullArea >> -shift;
                partialMask = partialArea >> -shift;
            }

            int val = 0;
            val += Long.bitCount(fullMask & neutralMask) * 3;
            val += Long.bitCount(fullMask & allyMask) * -1;
            val += Long.bitCount(partialMask & enemyMask) * 10;

            if (val > bestVal && val > 35) {
                bestVal = val;
                ret = m.getMapLocation();
            }
        }

        return ret;
    }

    MapLocation findBestPaintLoc() throws GameActionException {
        int[][] valueOfCapture = new int[9][9];
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        for (int j=nearLocations.length-1; j>=0; j--) {
            MapInfo m = nearLocations[j];
//        for (MapInfo m : nearLocations) {
            if (m.getPaint().isEnemy()) {
                // this is what we're here for
                valueOfCapture[m.getMapLocation().x-x+4][m.getMapLocation().y-y+4] = 10;
            } else if (!m.getPaint().isAlly()) {
                // capturing neutral tiles is alright but not the main focus of splashers
                valueOfCapture[m.getMapLocation().x-x+4][m.getMapLocation().y-y+4] = 3;
            } else if (m.hasRuin()) {
                RobotInfo robotAt = rc.senseRobotAtLocation(m.getMapLocation());
                if (robotAt != null && robotAt.getTeam() != rc.getTeam()) {
                    // attacking towers is very good
                    valueOfCapture[m.getMapLocation().x-x+4][m.getMapLocation().y-y+4] = 20;
                }
            } else {
                // repainting our own tiles is inefficient + might ruin patterns
                valueOfCapture[m.getMapLocation().x-x+4][m.getMapLocation().y-y+4] = -1;
            }
        }
        valueOfCapture[4][4] += 10;

        MapLocation ret = null;
        int bestVal = Integer.MIN_VALUE;
        for (int j=attackPos.length-1; j>=0; j--) {
            MapInfo m = attackPos[j];
//        }
//        for (MapInfo m : attackPos) {
            if (!m.isPassable())
                continue;

            int relX = m.getMapLocation().x - x + 4;
            int relY = m.getMapLocation().y - y + 4;
            int val = 0;
            for (int i=8; i>=0; i--) {
                val += valueOfCapture[relX+deltasClose[i][0]][relY+deltasClose[i][1]];
            }
            for (int i=3; i>=0; i--) {
                if (valueOfCapture[relX+deltasFar[i][0]][relY+deltasFar[i][1]] != 10) {
                    val += valueOfCapture[relX+deltasFar[i][0]][relY+deltasFar[i][1]];
                }
            }

            if (val > bestVal && val > 35) {
                bestVal = val;
                ret = m.getMapLocation();
            }
        }

        return ret;
    }

    @Override
    void initTurn() throws GameActionException {
        super.initTurn();
    }

    @Override
    void endTurn() throws GameActionException {
        super.endTurn();
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
                        if (nearRobots[i].getLocation().distanceSquaredTo(dest) <= 3)
                            numEnemyTowers++;
                        if (nearRobots[i].getType() == UnitType.LEVEL_ONE_DEFENSE_TOWER &&
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
                        val -= 30;
                    }
                }

                if (d != Direction.CENTER) {
                    for (int i = 6; i >= 0; i--) {
                        if (rc.getLocation().equals(lastLocations[i])) {
                            val -= 20;
                            break;
                        }
                    }
                }

                val += 5 * numAllies - numEnemyMoppers - numEnemyTowers * 3;
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
            if (ri == null && minDist > m.distanceSquaredTo(rc.getLocation())) {
                minDist = m.distanceSquaredTo(rc.getLocation());
                ret = m;
            }
            if (ri != null && ri.getTeam() == rc.getTeam()) {
                UnitType tp = ri.getType();
                if (tp == UnitType.LEVEL_ONE_PAINT_TOWER ||
                        tp == UnitType.LEVEL_THREE_PAINT_TOWER ||
                        tp == UnitType.LEVEL_TWO_PAINT_TOWER) {
                    if (ri.getLocation() != this.lastPainTower) {
                        this.semiLastPaintTower = this.lastPainTower;
                        this.lastPainTower = ri.getLocation();
                    }
                }
            }
        }

        return ret;
    }
}
