import java.util.ArrayList;

import model.Bonus;
import model.BonusType;
import model.Car;
import model.CarType;
import model.Direction;
import model.Game;
import model.Move;
import model.TileType;
import model.World;
import static java.lang.StrictMath.hypot;
import static java.lang.Math.pow;


public final class MyStrategy implements Strategy {
	
	public enum TileRole{
		Straight,
		Turn,
		BeforeTurn,
		AfterTurn,
		MidTurn,
		Unknown
	}
	
	public String printRole(TileRole role){
		String res;
		switch(role){
		case Straight:
			res = "↑";
			break;
			
		case Turn:
			res = "→";
			break;
			
		case BeforeTurn:
			res = "↗";
			break;
			
		case AfterTurn:
			res = "↘";
			break;
			
		case MidTurn:
			res = "↷";
			break;
			
		default:
			res = "□";
		}
		return res;
	}
	
	public String printDirection(Direction d){
		String res = "";
		if(d == Direction.UP)
			res = "↑";
		else if(d == Direction.RIGHT)
			res = "→";
		else if(d == Direction.LEFT)
			res = "←";
		else
			res = "□";
		return res;
	}
	
	/** Класс описывающий тайл через который должен проехать кодемобиль. */
	public class RoadObj{
		/** Смещение от центра необходимое для плавного прохождения поворота. */
		public final static double TurnShift = 0.3d;
		public final static double TurnShiftS = 0.25d;
		
		/** Описывают тайл карты через который необходимо проехать кодемобилю.		 */
		int tileX, tileY;
		
		/** Координаты по осям x и y внутри тайла точки через которую должен проехать кодемобиль. */
		double trajectoryX, trajectoryY;
		
		/** Индекс waypoint данного тайла */
		int waypoint;
		
		/** Тип тайла относительно прямолинейности движения */ 
		TileRole role;
		
		/** Описание изменения направления движения относительно направления в предыдущем тайле. Up - по прямойб left,right - направление поворота. */
		Direction dir;
		
		/** Описание направления движения в тайле относительно всего мира */
		//Direction moveDir;
		
		/** Если тип тайла на момент построения пути был неизвестен. */
		boolean unknown;
		
		/** Стандартный конструктор. */
		public RoadObj(int x, int y, double x1, double y1, TileRole tileRole){
			tileX = x;
			tileY = y;
			trajectoryX = x1;
			trajectoryY = y1;
			role = tileRole;
			
			waypoint =-1;
		}
		
		public RoadObj(int x, int y){
			tileX = x;
			tileY = y;
			trajectoryX = 0;
			trajectoryY = 0;
			role = TileRole.Unknown;
			
			waypoint =-1;
		}
		
		public RoadObj(int x, int y, TileRole tileRole){
			tileX = x;
			tileY = y;
			trajectoryX = 0D;
			trajectoryY = 0D;
			role = tileRole;
			
			waypoint =-1;
		}
	}
	
	
	
	private class Guardian{
		
		private static final int MIN_LENGTH = 8;
		Sector currentSector;
				
		ArrayList<RoadObj> Road;
		RoadObj last;
		Bonus targetBonus;
		
		public Guardian(){
			Road = new ArrayList<RoadObj>();	
			
			last = null;
		}
		
		private void print(){
			if(Road.size()==0)
				return;
			for(RoadObj ro : Road){
				System.out.print("[" + new Integer(ro.tileX).toString() + "," + new Integer(ro.tileY).toString());
				switch(ro.role){
				case Turn:
					System.out.print(" T");
					break;
				case Unknown:
					System.out.print(" U");
					break;
				case Straight:
					System.out.print(" S");
					break;
				case BeforeTurn:
					System.out.print(" b");
					break;
				case AfterTurn:
					System.out.print(" a");
					break;
				default:
					System.out.print(" x");
					break;	
				}
				System.out.print(printDirection(ro.dir));
				System.out.print("] ");
			}
			System.out.println("");
		}
		
		public TileRole nextRole(){
			return Road.get(0).role;			
		}
		
		
		public boolean isDoubleTurn(){		
			
			if(Road.get(0).role == TileRole.Turn)
				if(Road.get(1).role == TileRole.Turn)
					if(Road.get(0).dir == Road.get(1).dir)
						return true;
			return false;
		}
		
		public Sector getSector(){
						
			if(Road.size() < 3)
				return Sector.LINE;
			
			if(Road.get(0).role == TileRole.Turn 
					&& last != null
					&& last.role == TileRole.Turn
					&& Road.get(0).dir == last.dir){
				return Sector.U_TURN;
			}
			
			if(Road.get(1).role == TileRole.MidTurn){
				//return Sector.WIDE_TURN;
				
				if(Road.get(0).dir == Road.get(2).dir){
					currentSector = Sector.U_WIDE_TURN;
					return Sector.U_WIDE_TURN;
				}
				else{
					currentSector = Sector.S_WIDE_TURN;
					return Sector.S_WIDE_TURN;
				}	
				
			}
			
			if(Road.get(0).role == TileRole.MidTurn)
				if(currentSector != Sector.U_WIDE_TURN)
					return currentSector;
			
			
			if(Road.get(1).role == TileRole.Turn)
				if(Road.get(2).role == TileRole.Turn)
					if(Road.get(1).dir == Road.get(2).dir)
						return Sector.U_TURN;
			
			if(Road.get(0).role == TileRole.Turn)
				if(Road.get(1).role == TileRole.Turn){
					if(Road.get(0).dir == Road.get(1).dir)
						return Sector.U_TURN;
					else
						return Sector.S_TURN;
				}
			
			
			/*
			if(Road.get(0).role == TileRole.Turn)
				if(currentSector == Sector.S_WIDE_TURN)
					return currentSector;
			*/
			currentSector = Sector.LINE;
			return Sector.LINE;
		}
		
		private double calcTargetX1(Car self, World world, Game game, RoadObj tile){
			double shift = 0.5d;
			int dx,dy;
			int i = Road.indexOf(tile);
			int i1 = Road.indexOf(tile);
			if(i1==-1)
				i1 = Road.size()-1;
			else
				i1--;
			
			if(tile.role == TileRole.Turn){
				dx = tile.tileX - Road.get(i1).tileX;
				dy = tile.tileY - Road.get(i1).tileY;

				if(dx > 0){
					shift -= RoadObj.TurnShift;
				}
				else if(dx < 0){
					shift += RoadObj.TurnShift;
				}
				else if(dy > 0){				
					if(tile.dir == Direction.RIGHT){
						shift -= RoadObj.TurnShift;
					}
					else{
						shift += RoadObj.TurnShift;
					}
				}
				else{
					if(tile.dir == Direction.RIGHT){
						shift += RoadObj.TurnShift;
					}
					else{
						shift -= RoadObj.TurnShift;
					}
				}
				
				if(i>1 && i < Road.size()-2){
					if(Road.get(i-1).role == TileRole.Turn
							&& Road.get(i+1).role == TileRole.Turn
							&& Road.get(i) != Road.get(i-1)
							&& Road.get(i-1).dir == Road.get(i+1).dir)
						shift = 0.5 + RoadObj.TurnShiftS * Math.signum(shift - 0.5);
				}
			}
			else if(tile.role == TileRole.BeforeTurn
					&& Road.size() > i1 + 3){
				i1+=2;	// i1 is now turn roadobject index.
				if(Road.get(i1+1).role == TileRole.AfterTurn
						|| Road.get(i1+1).role == TileRole.MidTurn
						|| (Road.get(i1+1).role == TileRole.Turn
						&& Road.get(i1+1).dir == Road.get(i1).dir))
				{
				//i1++;
					if(true){
						dx = Road.get(i1).tileX - tile.tileX;
						dy = Road.get(i1).tileY - tile.tileY;
					
						Direction d = Road.get(i1).dir;
					
						if(d == Direction.RIGHT){
							if(dy > 0){
								shift += 0.66 * RoadObj.TurnShift;
							}
							else if(dy < 0){
								shift -= 0.66 * RoadObj.TurnShift;
							}
						}
						else if(d == Direction.LEFT){
							if(dy > 0){
								shift -= 0.66 * RoadObj.TurnShift;
							}
							else if(dy < 0){
								shift += 0.66 * RoadObj.TurnShift;
							}
						}				
					}
				}
				
			}
			
			return (tile.tileX + shift) * game.getTrackTileSize();
		}
		
		private double calcTargetY1(Car self, World world, Game game, RoadObj tile){
			double shift = 0.5d;
			int dx,dy;
			int i = Road.indexOf(tile);
			int i1 = Road.indexOf(tile);
			if(i1==-1)
				i1 = Road.size()-1;
			else
				i1--;
			
			
			if(tile.role == TileRole.Turn){
				dx = tile.tileX - Road.get(i1).tileX;
				dy = tile.tileY - Road.get(i1).tileY;
				
				if(dy > 0){
					shift -= RoadObj.TurnShift;
				}
				else if(dy < 0){
					shift += RoadObj.TurnShift;
				}
				else if(dx > 0){				
					if(tile.dir == Direction.RIGHT){
						shift += RoadObj.TurnShift;
					}
					else{
						shift -= RoadObj.TurnShift;
					}
				}
				else{
					if(tile.dir == Direction.RIGHT){
						shift -= RoadObj.TurnShift;
					}
					else{
						shift += RoadObj.TurnShift;
					}
				}
				
				if(i>1 && i < Road.size()-2){
					if(Road.get(i-1).role == TileRole.Turn
							&& Road.get(i+1).role == TileRole.Turn
							&& Road.get(i) != Road.get(i-1)
							&& Road.get(i-1).dir == Road.get(i+1).dir)
						shift = 0.5 + RoadObj.TurnShiftS * Math.signum(shift - 0.5);
				}

			}
			else if(tile.role == TileRole.BeforeTurn
					&& Road.size() > i1 + 3){
				
				i1 += 2;
				if(Road.get(i1+1).role == TileRole.AfterTurn
						|| Road.get(i1+1).role == TileRole.MidTurn
						|| (Road.get(i1+1).role == TileRole.Turn
						&& Road.get(i1+1).dir == Road.get(i1).dir))
				{
					dx = Road.get(i1).tileX - tile.tileX;
					dy = Road.get(i1).tileY - tile.tileY;
					
					Direction d = Road.get(i1).dir;
					
					if(d == Direction.RIGHT){
						if(dx > 0){
							shift -= 0.66 * RoadObj.TurnShift;
						}
						else if(dx < 0){
							shift += 0.66 * RoadObj.TurnShift;
						}
					}
					else if(d == Direction.LEFT){
						if(dx > 0){
							shift += 0.66 * RoadObj.TurnShift;
						}
						else if(dx < 0){
							shift -= 0.66 * RoadObj.TurnShift;
						}
					}
				}
				
			}
			
			return (tile.tileY + shift) * game.getTrackTileSize();
		}
				
		public void tick(Car self, World world, Game game){
			boolean changes = false;
			int xc, yc;						
			// car tile
			xc = (int)(self.getX() / game.getTrackTileSize());
			yc = (int)(self.getY() / game.getTrackTileSize());
			
			if(Road.size()==0){
				getNext(self,world,game);
				return;
			}
			
			if(Road.size()>2){
				if(Road.get(0).unknown){
					Road.clear();
					getNext(self,world,game);
					return;
				}
				else if(Road.get(1).unknown){
					
					while(Road.size()>1)
						Road.remove(1);
										
					getNext(self,world,game);
					return;
				}
				
				else if(Road.get(2).unknown){
					while(Road.size()>2)
						Road.remove(2);
					
					getNext(self,world,game);
					return;
				}
				
			}
						
			if(xc == Road.get(0).tileX && yc == Road.get(0).tileY){
				
				//return;
			}			
			else if(Road.size()>1){
				if(xc == Road.get(1).tileX && yc == Road.get(1).tileY){
					//System.out.println("Removed: "+ new Integer(Road.get(0).tileX).toString() + "," + new Integer(Road.get(0).tileY).toString());
					
					changes = true;
					last = Road.get(0);
					Road.remove(0);
					//print();
				}
				else if(Road.size() > 2 
						&& xc == Road.get(2).tileX && yc == Road.get(2).tileY
						&& Road.get(1).waypoint == -1
						){
					//System.out.println("Removed: "+ new Integer(Road.get(0).tileX).toString() + "," + new Integer(Road.get(0).tileY).toString());
					
					changes = true;
					last = Road.get(1);
					Road.remove(0);					
					Road.remove(1);
					//print();
				}
				else{
					System.out.println("Road cleared!");
					Road.clear();
				
					// current car title is not on road/
				}
			}
			
			
			if(Road.size() < MIN_LENGTH){
				//add some tiles tio road
				changes = true;
				getNext(self, world, game);
			}
			
			targetBonus = null;
			findBonuses(self, world, game);
			
			//if(changes)
			
			/*
			 * ************************************************************************************************
			 * 			Road print()
			 * ************************************************************************************************
			 */
			//	print();			
			
		}
		
		private boolean isOnTrajectory(int waypointIndex){
			boolean res = false;
			for(RoadObj obj : Road){
				if(obj.waypoint == waypointIndex)
					res = true;
			}
			return res;
		}
		
		private int indexInArray(ArrayList <int[]> line, int[] elem){
			if(line.size() < 1)
				return -1;
						
			int l = line.size();
			for(int i=0;i<l;i++){
				
				if(line.get(i).length != elem.length)
					continue;
				
				boolean match = true;
				
				for(int j=0;j<elem.length;j++){
					if(line.get(i)[j] != elem[j])
						match = false;
				}
				
				if(match==true)
					return i;
			}
			
			return -1;
		}
		
		private boolean isConnected(int x0, int y0, Direction d, World world, ArrayList <int[]> line, ArrayList <int[]> bad){
			
			TileType type = world.getTilesXY()[x0][y0];
					
			int x1 = x0, y1= y0;
			
			
			switch(d){
			case UP:
				y1--;
				break;
			case DOWN:
				y1++;
				break;
			case RIGHT:
				x1++;
				break;
			case LEFT:
				x1--;
				break;
			}
			
			for(int i=0;i<bad.size();i++){
				if(bad.get(i)[0] == x1 && bad.get(i)[1] == y1)
					return false;
			}
			
			
			for(int i=0;i<line.size();i++){
				if(line.get(i)[0] == x1 && line.get(i)[1] == y1)
					return false;
			}
			if(indexInArray(line, new int[]{x0,y0}) < 0){
				if(line.size()<1){
					if(Road.size()>1)
						if(Road.get(Road.size()-2).tileX == x1 && Road.get(Road.size()-2).tileY == y1)
							return false;					
				}
				else{
					if(Road.size()>0)
						if(Road.get(Road.size()-1).tileX == x1 && Road.get(Road.size()-1).tileY == y1)
							return false;
				}
					
			}
			else{
				int i = indexInArray(line, new int[]{x0,y0});
				if(i>0){
					if(line.get(i-1)[0] == x1 && line.get(i-1)[1] == y1)
						return false;
				}
				else{
					if(Road.size()>0)
						if(Road.get(Road.size()-1).tileX == x1 && Road.get(Road.size()-1).tileY == y1)
							return false;
				}
			}
			
			if(type == TileType.CROSSROADS)
				return true;
						
			if(d==Direction.UP){
				switch(type){
				case VERTICAL:
				case LEFT_BOTTOM_CORNER:
				case RIGHT_BOTTOM_CORNER:
				case TOP_HEADED_T:
				case LEFT_HEADED_T:
				case RIGHT_HEADED_T:
					return true;
				default:
					return false;
				}
			}
			else if(d==Direction.RIGHT){
				switch(type){
				case HORIZONTAL:
				case LEFT_BOTTOM_CORNER:
				case LEFT_TOP_CORNER:
				case TOP_HEADED_T:
				case BOTTOM_HEADED_T:
				case RIGHT_HEADED_T:
					return true;
				default:
					return false;
				}
			}
			else if(d==Direction.DOWN){
				switch(type){
				case VERTICAL:
				case LEFT_TOP_CORNER:
				case RIGHT_TOP_CORNER:
				case BOTTOM_HEADED_T:
				case LEFT_HEADED_T:
				case RIGHT_HEADED_T:
					return true;
				default:
					return false;
				}
			}
			else if(d==Direction.LEFT){
				switch(type){
				case HORIZONTAL:
				case RIGHT_BOTTOM_CORNER:
				case RIGHT_TOP_CORNER:
				case TOP_HEADED_T:
				case BOTTOM_HEADED_T:
				case LEFT_HEADED_T:
					return true;
				default:
					return false;
				}
			}
			return false;
		}
		
		/** Возможность прооехать из указанного тайла в заданном направлении */
		private boolean isPhysicallyConnected(TileType type, Direction d, World world, int x, int y){
			
			
			if(type == TileType.UNKNOWN){
				switch(d){
				case UP:
					y--;
					break;
				case DOWN:
					y++;
					break;
				case RIGHT:
					x++;
					break;
				case LEFT:
					x--;
					break;
				}
				
				if(x<0 || y<0 || x >= world.getWidth() || y >= world.getHeight())
					return false;
				
				TileType nextTile = world.getTilesXY()[x][y];
				if(nextTile == TileType.UNKNOWN)
					return true;
				
				if(nextTile == TileType.CROSSROADS)
					return true;
							
				if(d==Direction.DOWN){
					switch(nextTile){
					case VERTICAL:
					case LEFT_BOTTOM_CORNER:
					case RIGHT_BOTTOM_CORNER:
					case TOP_HEADED_T:
					case LEFT_HEADED_T:
					case RIGHT_HEADED_T:
						return true;
					default:
						return false;
					}
				}
				else if(d==Direction.LEFT){
					switch(nextTile){
					case HORIZONTAL:
					case LEFT_BOTTOM_CORNER:
					case LEFT_TOP_CORNER:
					case TOP_HEADED_T:
					case BOTTOM_HEADED_T:
					case RIGHT_HEADED_T:
						return true;
					default:
						return false;
					}
				}
				else if(d==Direction.UP){
					switch(nextTile){
					case VERTICAL:
					case LEFT_TOP_CORNER:
					case RIGHT_TOP_CORNER:
					case BOTTOM_HEADED_T:
					case LEFT_HEADED_T:
					case RIGHT_HEADED_T:
						return true;
					default:
						return false;
					}
				}
				else if(d==Direction.RIGHT){
					switch(nextTile){
					case HORIZONTAL:
					case RIGHT_BOTTOM_CORNER:
					case RIGHT_TOP_CORNER:
					case TOP_HEADED_T:
					case BOTTOM_HEADED_T:
					case LEFT_HEADED_T:
						return true;
					default:
						return false;
					}
				}
			}
			
			if(type == TileType.CROSSROADS)
				return true;
						
			if(d==Direction.UP){
				switch(type){
				case VERTICAL:
				case LEFT_BOTTOM_CORNER:
				case RIGHT_BOTTOM_CORNER:
				case TOP_HEADED_T:
				case LEFT_HEADED_T:
				case RIGHT_HEADED_T:
					return true;
				default:
					return false;
				}
			}
			else if(d==Direction.RIGHT){
				switch(type){
				case HORIZONTAL:
				case LEFT_BOTTOM_CORNER:
				case LEFT_TOP_CORNER:
				case TOP_HEADED_T:
				case BOTTOM_HEADED_T:
				case RIGHT_HEADED_T:
					return true;
				default:
					return false;
				}
			}
			else if(d==Direction.DOWN){
				switch(type){
				case VERTICAL:
				case LEFT_TOP_CORNER:
				case RIGHT_TOP_CORNER:
				case BOTTOM_HEADED_T:
				case LEFT_HEADED_T:
				case RIGHT_HEADED_T:
					return true;
				default:
					return false;
				}
			}
			else if(d==Direction.LEFT){
				switch(type){
				case HORIZONTAL:
				case RIGHT_BOTTOM_CORNER:
				case RIGHT_TOP_CORNER:
				case TOP_HEADED_T:
				case BOTTOM_HEADED_T:
				case LEFT_HEADED_T:
					return true;
				default:
					return false;
				}
			}
			return false;
		}
		
		
		/** При движении по диагонали помогает с выбором оси. 
		*	@return значение на основе которого выбирается ось для шага.
		*	1 - ось X,
		*   -1 - ось Y,
		*   0 - нет необходимости в выборк оси.
		 */
		private int selectAxis(int x, int y, int dirX,int dirY, World world){
			if(x+dirX < 0 || x+dirX >= world.getWidth())
				return 0;
			if(y+dirY < 0 || y+dirY >= world.getHeight())
				return 0;
			
			if(world.getTilesXY()[x+dirX][y+dirY] == TileType.EMPTY)
				return 0;
			
			boolean axisX, axisY;
			axisX = false;
			axisY = false;
			
			// axeX
			if(isPhysicallyConnected(world.getTilesXY()[x][y], getDirection(dirX,0),world,x,y))
				if(isPhysicallyConnected(world.getTilesXY()[x+dirX][y], getDirection(0,dirY),world,x,y))
					axisX = true;
						
			// axeY
			if(isPhysicallyConnected(world.getTilesXY()[x][y], getDirection(0, dirY),world,x,y))
				if(isPhysicallyConnected(world.getTilesXY()[x][y+dirY], getDirection(dirX, 0),world,x,y))
					axisY = true;
			
			if(axisX && ! axisY)
				return 1;
			
			if(!axisX && axisY)
				return -1;
			
			return 0;
		}
				
		public Direction getDirection(int dx, int dy){
			if(dx > 0)
				return Direction.RIGHT;
			if(dx<0)
				return Direction.LEFT;
			if(dy>0)
				return Direction.DOWN;
			if(dx<0)
				return Direction.UP;
			return Direction.UP;
		}
		/*
		public ArrayList<int[]> findAWay(Car self, World world, Game game){
			
		}
		*/
		
		/**
		 * Выводит в консоль содержимое массива.
		 * @param line
		 */
		private void printline(ArrayList<int[]> line){
			for(int [] point : line){
				System.out.printf("[%d,%d]", point[0], point[1]);
			}
			System.out.println("");
		}
		
		/** Класс описывающий представление тайла мира в дереве. */
		private class Branch{			
			int x;
			int y;
			int level;
			int id;
			int parentId;
		}
		
		/** Проверка наличия тайла с указанными координатами в двумерном списке. */
		private boolean isInTree(ArrayList<ArrayList<Branch>> Tree, int x, int y){
			for(ArrayList<Branch>level : Tree){
				if(isInLevel(level,x,y))
					return true;
				
			}
			return false;
		}
		
		/** Проверка наличия тайла с указанными координатами в списке. */
		private boolean isInLevel(ArrayList<Branch> level, int x, int y){
			for(Branch leaf : level){
				if(leaf.x == x && leaf.y == y)
					return true;
			}
			return false;
		}
		
		/** Не используется. */
		private int findParent(ArrayList<Branch> level, int x, int y){
			for(Branch leaf : level){
				if(leaf.x == x && leaf.y == y)
					return level.indexOf(leaf);
			}
			return -1;
		}
		
		/** Возможность проехать черз данный тайл. */
		private boolean isRoad(World world, int x ,int y){
			if(x < 0)	return false;
			if(y < 0)	return false;
			if(x >= world.getWidth())	return false;
			if(y >= world.getHeight())	return false;
			
			TileType tile = world.getTilesXY()[x][y];
			if(tile == TileType.EMPTY)
				return false;
			
			return true;
		}
				
		/**
		 * Рассчет дороги от одной точки до дпругой методом построения деревьев.
		 * @param self, @param world, @param game - стандартные праметры. 
		 * @return
		 */
		public boolean getWayTree(Car self, World world, Game game){
			/** Дерево связанных тайлов трассы. */
			ArrayList<ArrayList<Branch>> Tree;
			Tree = new ArrayList<ArrayList<Branch>>();			
			Tree.add(new ArrayList<Branch>());
			
			/** Индекс контрольной точки до которой мы прокладываем маршрут. */
			int next_waypoint = -1; 
			
			//System.out.println("!!!");
			
			Branch b = new Branch();
			if(Road.size()>0){
				b.x = Road.get(Road.size()-1).tileX;
				b.y = Road.get(Road.size()-1).tileY;
			}
			else{
				b.x = (int)(self.getX() / game.getTrackTileSize());
				b.y = (int)(self.getY() / game.getTrackTileSize());
			}
			b.level = 0;
			b.id = 0;
			b.parentId = -1;
			Tree.get(0).add(b);
			
			/** Координаты тайла до которого необходимо найти маршрут. */
			int targetX, targetY;
						
			if(Road.size()==0){
				targetX = self.getNextWaypointX();
				targetY = self.getNextWaypointY();
			}
			else{
				int n=-1;
				for(RoadObj ro : Road){
					if(ro.waypoint!=-1)
						n = ro.waypoint;
				}
				if(n!=-1){
					n++;
					if(n==world.getWaypoints().length)
						n=0;
					targetX = world.getWaypoints()[n][0];
					targetY = world.getWaypoints()[n][1];
				}
				else{
					n = self.getNextWaypointIndex();
					targetX = self.getNextWaypointX();
					targetY = self.getNextWaypointY();
				}
				next_waypoint = n;
			}
			
			// Построение дерева из возможных путей.
			
			do{
				ArrayList<Branch> lvl = Tree.get(Tree.size()-1);
				ArrayList<Branch> newBranch = new ArrayList<Branch>();
				for(Branch leaf : lvl){
					int newX, newY;
					newX = leaf.x;
					newY = leaf.y;
					
					// Выбор направления.
					int dx,dy;
					int dirX, dirY;
					int dirX0, dirY0, dirX1, dirY1;
					dx = targetX - newX;
					dy = targetY - newY;
					
					dirX = (int)Math.signum(dx);
					dirY = (int)Math.signum(dy);
					
					if(dirX==0){
						if(Tree.size()>2
								&& leaf.x - Tree.get(Tree.size()-2).get(leaf.parentId).x != 0){
							dirX = (int)Math.signum(leaf.x - Tree.get(Tree.size()-2).get(leaf.parentId).x);
						}
						else{
							dirX = 1;
						}
					}
					
					if(dirY==0){
						if(Tree.size()>2
								&& leaf.y - Tree.get(Tree.size()-2).get(leaf.parentId).y != 0){
							dirY = (int)Math.signum(leaf.y - Tree.get(Tree.size()-2).get(leaf.parentId).y);
						}
						else{
							dirY = 1;
						}
					}
					
					boolean axeX;
					if(Tree.size() > 2
							|| Tree.size() + Road.size() > 3){
						//boolean  lastStepX = ((leaf.y - Tree.get(Tree.size()-2).get(leaf.parentId).y) == 0);
						//boolean beforeLastStepX = ((Tree.get(Tree.size()-2).get(leaf.parentId).y - Tree.get(Tree.size()-3).get(Tree.get(Tree.size()-2).get(leaf.parentId).parentId).y) == 0);
								
						
						
						Direction nextX, nextY,last, beforeLast;
						
						/*
						last = getDirection((leaf.x - Tree.get(Tree.size()-2).get(leaf.parentId).x),(leaf.y - Tree.get(Tree.size()-2).get(leaf.parentId).y));
						beforeLast = getDirection(
								Tree.get(Tree.size()-2).get(leaf.parentId).x - Tree.get(Tree.size()-3).get(Tree.get(Tree.size()-2).get(leaf.parentId).parentId).x
								,Tree.get(Tree.size()-2).get(leaf.parentId).y - Tree.get(Tree.size()-3).get(Tree.get(Tree.size()-2).get(leaf.parentId).parentId).y);
						*/
						
						
						int x0,y0,x1,y1;
						
						if(Tree.size()==1){
							x0 = Road.get(Road.size() - 2).tileX;
							y0 = Road.get(Road.size() - 2).tileY;
							
							x1 = Road.get(Road.size() - 3).tileX;
							y1 = Road.get(Road.size() - 3).tileY;
						}						
						else if(Tree.size() == 2){
							x0 = Tree.get(Tree.size()-2).get(leaf.parentId).x;
							y0 = Tree.get(Tree.size()-2).get(leaf.parentId).y;
							
							x1 = Road.get(Road.size() - 2).tileX;
							y1 = Road.get(Road.size() - 2).tileY;
						}
						else{
							x0 = Tree.get(Tree.size()-2).get(leaf.parentId).x;
							y0 = Tree.get(Tree.size()-2).get(leaf.parentId).y;
							
							x1 = Tree.get(Tree.size()-3).get(Tree.get(Tree.size()-2).get(leaf.parentId).parentId).x;
							y1 = Tree.get(Tree.size()-3).get(Tree.get(Tree.size()-2).get(leaf.parentId).parentId).y;
						}
						
						nextX = getDirection(dirX,0);
						nextY = getDirection(0,dirY);
						last = getDirection(leaf.x - x0, leaf.y - y0);
						beforeLast = getDirection(x0 - x1, y0 - y1);
												
						/*
						if(lastStepX){
							if(beforeLastStepX)
								axeX = false;
							else
								axeX = true;
						}
						else{
							if(beforeLastStepX)
								axeX = true;
							else
								axeX = false;
						}
						*/
						
						if(last == Direction.RIGHT || last == Direction.LEFT){
							if(beforeLast == Direction.RIGHT || beforeLast == Direction.LEFT)
								axeX = false;
							else{
								if(beforeLast == nextY)
									axeX = false;
								else
									axeX = true;
							}
								
						}
						else{
							if(beforeLast == Direction.UP || beforeLast == Direction.DOWN)
								axeX = true;
							else{
								if(beforeLast == nextX)
									axeX = true;
								else
									axeX = false;
							}
						}
					}
					else
						axeX = (dx>=dy);
					if(axeX){
						dirX0 = dirX;
						dirY0 = 0;
						
						dirX1 = 0;
						dirY1 = dirY;
					}
					else{
						dirX0 = 0;
						dirY0 = dirY;
						
						dirX1 = dirX;
						dirY1 = 0;
					}
					
					for(int i=0;i<4;i++){
						switch(i){
						case 0:
							newX = leaf.x + dirX0;
							newY = leaf.y + dirY0;
							break;
						case 1:
							newX = leaf.x + dirX1;
							newY = leaf.y + dirY1;
							break;
						case 2:
							newX = leaf.x - dirX0;
							newY = leaf.y - dirY0;
							break;
						case 3:
							newX = leaf.x - dirX1;
							newY = leaf.y - dirY1;
							break;
						}
						
						
						if(isRoad(world, newX, newY))
							if(!isInTree(Tree, newX, newY))
								if(isPhysicallyConnected(world.getTilesXY()[leaf.x][leaf.y], getDirection(newX - leaf.x, newY - leaf.y),world,leaf.x,leaf.y))									
									if(!isInLevel(newBranch, newX, newY))
										if(!(Tree.size()==1
												&& Road.size() > 1
												&& (Road.get(Road.size()-2).tileX == newX && Road.get(Road.size()-2).tileY == newY)))
										{
								//if(isConnected(targetY, targetY, getDirection(newX - leaf.x, newY - leaf.y), world, null, null)){
											Branch newLeaf = new Branch();
											newLeaf.x = newX;
											newLeaf.y = newY;
											newLeaf.level = Tree.size();
											newLeaf.id = newBranch.size();
											newLeaf.parentId = lvl.indexOf(leaf);
											newBranch.add(newLeaf);
										}
					}
					
				}
				if(newBranch.size()==0)
					return false;
				else{
					/*
					for(Branch b2 : newBranch){
						System.out.printf("(%d,%d)", b2.x, b2.y);
					}
					System.out.println("");
					*/
					Tree.add(newBranch);
				}
			}while(!isInTree(Tree, targetX, targetY));
			
			ArrayList<int[]> result = new ArrayList<int[]>();
			
			int i,j;
			int parentId=0;
			
			// Создание пути в виде массива с координатами тайлов.
			
			ArrayList<Branch>leafs = Tree.get(Tree.size()-1);
			for(j=0;j<leafs.size();j++){
				if(leafs.get(j).x == targetX && leafs.get(j).y == targetY){
					parentId = leafs.get(j).parentId;
					result.add(new int[] {leafs.get(j).x, leafs.get(j).y});
				}
			}
			
			for(i = 1; i < Tree.size() - 1; i++){
				ArrayList<Branch> level = Tree.get(Tree.size() - 1 - i);
				Branch b1 = level.get(parentId);
				parentId = b1.parentId;
				result.add(0, new int[] {b1.x,b1.y});
			}
			
			// Демонстрация полученного пути.
			
			//printline(result);
			
			// Если путь пустой его не надо добавлять к маршруту.
			if(result.size() < 1)
				return false;
			
			// Если при предыдущем шаге мы не смогли рассчитать параметры последнего тайла.
			if(Road.get(Road.size()-1).role == TileRole.Unknown){
				/** Изменение координат между предыдущей и следующей точкой. */
				int dx,dy;
				/** Изменение координат между текущей и предыдущей точками. */
				int dx0, dy0;
				/** Изменение координат между текущей и следующей точками. */
				int dx1, dy1;
				/** Индекс изучаемого тайла в пути. */
				int i1;
				
				i1 = Road.size()-1;
				
				if(i1 < 1){
					Road.get(i1).role = TileRole.Straight;
					Road.get(i1).dir = Direction.UP;
				}
				else{
					dx0 = Road.get(i1).tileX - Road.get(i1-1).tileX;
					dy0 = Road.get(i1).tileY - Road.get(i1-1).tileY;
					
					dx1 = result.get(0)[0] - Road.get(i1).tileX;
					dy1 = result.get(0)[1] - Road.get(i1).tileY;
					
					dx = result.get(0)[0] - Road.get(i1 - 1).tileX;
					dy = result.get(0)[1] - Road.get(i1 - 1).tileY;
					
					if(dx == 0 || dy == 0){
						if(Road.get(i1-1).role == TileRole.Turn)
							Road.get(i1).role = TileRole.AfterTurn;
						else
							Road.get(i1).role = TileRole.Straight;
						Road.get(i1).dir = Direction.UP;
					}
					else{
						Road.get(i1).role = TileRole.Turn;
						if(Road.get(i1-1).role == TileRole.Straight)
							Road.get(i1-1).role = TileRole.BeforeTurn;
						else if(Road.get(i1-1).role == TileRole.AfterTurn)
							Road.get(i1-1).role = TileRole.MidTurn;
						
						if(Road.size()>2)
							if(Road.get(i1-1).role == TileRole.Straight)
								Road.get(i1-1).role = TileRole.BeforeTurn;
							else if (Road.get(i1-1).role == TileRole.AfterTurn)
								Road.get(i1-1).role = TileRole.MidTurn;
						
						
						if(dx0 > 0){
							if(dy1 > 0){								
								Road.get(i1).dir = Direction.RIGHT;		
							}
							else{
								Road.get(i1).dir = Direction.LEFT;
							}
						}
						else if(dx0 < 0){
							if(dy1 > 0){
								Road.get(i1).dir = Direction.LEFT;
							}
							else{
								Road.get(i1).dir = Direction.RIGHT;
							}
						}
						else if(dy0 > 0){
							if(dx1 > 0){
								Road.get(i1).dir = Direction.LEFT;
							}
							else{
								Road.get(i1).dir = Direction.RIGHT;
							}
						}
						else if(dy0 < 0){
							if(dx1 > 0){
								Road.get(i1).dir = Direction.RIGHT;
							}
							else{
								Road.get(i1).dir = Direction.LEFT;
							}
						}
							
					}
					
					Road.get(i1).trajectoryX = calcTargetX1(self, world, game, Road.get(i1));
					Road.get(i1).trajectoryY = calcTargetY1(self, world, game, Road.get(i1));
				}
			}
			
			// Представление полученного пути в объектах RoadObj;
			for(int[] point : result){
				// coordinates
				RoadObj next = new RoadObj(point[0], point[1]);
				
				// dir
				
				/** Изменение координат между предыдущей и следующей точкой. */
				int dx, dy;
				/** Изменение координат между текущей и предыдущей точками. */
				int dx0, dy0;
				/** Изменение координат между текущей и следующей точками. */
				int dx1, dy1;
				/** Индекс изучаемого тайла в пути. */
				int i1;
				i1 = result.indexOf(point);
				
				if(result.size() == 1){
					next.dir = Direction.DOWN;
					next.role = TileRole.Unknown;
					next.waypoint = next_waypoint;
				}
				else if(i1 == result.size()-1){
					next.dir = Direction.DOWN;
					next.role = TileRole.Unknown;
					next.waypoint = next_waypoint;
				}
				else{
					dx0 = point[0] - Road.get(Road.size()-1).tileX;
					dy0 = point[1] - Road.get(Road.size()-1).tileY;
					
					dx1 = result.get(i1+1)[0] - point[0];
					dy1 = result.get(i1+1)[1] - point[1];
					
					dx = result.get(i1+1)[0] - Road.get(Road.size()-1).tileX;
					dy = result.get(i1+1)[1] - Road.get(Road.size()-1).tileY;
					
					if(dx == 0 || dy == 0){
						if(Road.get(Road.size()-1).role == TileRole.Turn)
							next.role = TileRole.AfterTurn;
						else
							next.role = TileRole.Straight;
						
						next.dir = Direction.UP;
					}
					else{
						next.role = TileRole.Turn;
						if(Road.get(Road.size()-1).role == TileRole.Straight)
							Road.get(Road.size()-1).role = TileRole.BeforeTurn;
						
						else if(Road.get(Road.size()-1).role == TileRole.AfterTurn)
							Road.get(Road.size()-1).role = TileRole.MidTurn;
						
						if(dx0 > 0){
							if(dy1 > 0){								
								next.dir = Direction.RIGHT;		
							}
							else{
								next.dir = Direction.LEFT;
							}
						}
						else if(dx0 < 0){
							if(dy1 > 0){
								next.dir = Direction.LEFT;
							}
							else{
								next.dir = Direction.RIGHT;
							}
						}
						else if(dy0 > 0){
							if(dx1 > 0){
								next.dir = Direction.LEFT;
							}
							else{
								next.dir = Direction.RIGHT;
							}
						}
						else if(dy0 < 0){
							if(dx1 > 0){
								next.dir = Direction.RIGHT;
							}
							else{
								next.dir = Direction.LEFT;
							}
						}							
					}
				}
				/*
				else{
					
				}
				*/
				next.trajectoryX = calcTargetX1(self,world,game,next);
				next.trajectoryY = calcTargetY1(self,world,game,next);
				
				if(world.getTilesXY()[next.tileX][next.tileY] == TileType.UNKNOWN)
					next.unknown = true;
				else
					next.unknown = false;
				
				Road.add(next);
			
				/*
				if(Road.size() > 1
						&& Road.get(Road.size()-1).role == TileRole.Turn){
					Road.get(Road.size()-2).trajectoryX = calcTargetX1(self,world,game,Road.get(Road.size()-2));
					Road.get(Road.size()-2).trajectoryY = calcTargetY1(self,world,game,Road.get(Road.size()-2));
				}
				*/	
			}		
			//print();			
			
			for(RoadObj ro : Road){
				if(ro.role == TileRole.BeforeTurn){
					ro.trajectoryX = calcTargetX1(self,world,game,ro);
					ro.trajectoryY = calcTargetY1(self,world,game,ro);
				}
				if(ro.role == TileRole.Turn
						&& Road.indexOf(ro) > 0){
					
					ro.trajectoryX = calcTargetX1(self,world,game,ro);
					ro.trajectoryY = calcTargetY1(self,world,game,ro);
				}
			}
			
			return true;
		}
		
		public boolean getNext(Car self, World world, Game game){
			int x,y,xc,yc,xt,yt;	// x,y positions of Car and Target Tile
			int waypointIndex;		// waypoint target index 
			ArrayList<int[]> line = new ArrayList<int[]>();
			ArrayList<int[]> bad = new ArrayList<int[]>();
						
			if(Road.size()==0){
				
				xc = (int)(self.getX()  / game.getTrackTileSize());
				yc = (int)(self.getY()  / game.getTrackTileSize());
				RoadObj startObj = new RoadObj(xc, yc);
				startObj.role = TileRole.Straight;
				startObj.trajectoryX = calcTargetX1(self, world, game, startObj);
				startObj.trajectoryY = calcTargetY1(self, world, game, startObj);
				Road.add(startObj);
			}
			
			//System.out.println("Finding way function from " + new Integer(xc).toString() + ", " + new Integer(yc).toString() + " tile.");
			
			/////////////////////////////////////////////////////////////////////////////////////////
			// Somthing interesting
			if(getWayTree(self,world, game)){
				
				return true;
			}
			
			return true;
		}
		
		public double getTargetX(Game game){
			double x = 0d;			// waypoint 
			RoadObj target = null;		// target tile of track
			
			if(Road.size()==0)
				return 0d;
			
			if(Road.size()==1)
				target = Road.get(0);
			else{
				for(int i=1;i<Road.size();i++){					
					target= Road.get(i);
					if(Road.get(i).role == TileRole.BeforeTurn 
							|| target.role == TileRole.Turn
							|| target.role == TileRole.MidTurn
							|| target.role == TileRole.Unknown){
						break;
					}
				}
				//target= Road.get(1);
			}
			
			if(targetBonus != null){
				x = targetBonus.getX();
			}			
			else if(target.trajectoryX == 0d){
				// default value/ There is no calculated trajectory
				x = (target.tileX + 0.5d) * game.getTrackTileSize(); 
			}else{
				// Calculated trajectory;
				x = target.trajectoryX;
			}
			
						
			return x;
		}
		
		public double getTargetY(Game game){
			double y = 0d;			// waypoint 
			RoadObj target = null;		// target tile of track
			
			if(Road.size()==0)
				return 0d;
			
			if(Road.size()==1)
				target = Road.get(0);
			else{
				for(int i=1;i<Road.size();i++){
					target= Road.get(i);
					if(target.role == TileRole.BeforeTurn 
							|| target.role == TileRole.Turn 
							|| target.role == TileRole.MidTurn
							|| target.role == TileRole.Unknown)
						break;
				}
				
			}
			
			if(targetBonus != null){
				y = targetBonus.getY();
			}			
			else if(target.trajectoryY == 0d){
				// default value/ There is no calculated trajectory
				y = (target.tileY + 0.5d) * game.getTrackTileSize(); 
			}else{
				// Calculated trajectory;
				y = target.trajectoryY;
			}
			
			return y;
		}

		public TileRole getRole() {
			RoadObj obj = null;
			if(Road.size()==0)
				return TileRole.Unknown;
			else if(Road.size()==1)
				obj = Road.get(0);
			else
				obj = Road.get(1); 
			
			if(obj!=null)
				return obj.role;
			
			return null;
		}
		
		public TileRole getThisRole(){
			if(Road.size()==0)
				return TileRole.Unknown;
			
			return Road.get(0).role;
		}
		
		public boolean canNitro(){
			boolean r = true;
			
			int l=2;
			if(Road.size()<l){
				return false;
			}
			
			for(int i=0;i<l;i++){
				RoadObj ro = Road.get(i);
				if(ro.role == TileRole.Turn || ro.role == TileRole.AfterTurn)
					r= false;				
			}
				
			return r;
		}
		
		public boolean canNitro_S(Car self, Game game){
			/*
			int l=7;
			
			if(Road.size() < l)
				return false;
			
			if(Math.abs(self.getWheelTurn()) > Math.PI / 24
					|| Math.abs(self.getAngleTo(getTargetX(game), getTargetY(game))) > Math.PI / 24)
				return false;
			
			for(int i=1;i<l-1;i++){
				if(!(Road.get(i-1).role==TileRole.Turn
						&& Road.get(i+1).role==TileRole.Turn
						&& Road.get(i).role==TileRole.Turn
						&& Road.get(i-1).dir != Road.get(i).dir
						&& Road.get(i+1).dir != Road.get(i).dir))
					return false;				
			}
			*/
			return false;
				
		}
		
		public int roadTileIndex(int x, int y){
			int  i = -1;
			for(RoadObj ro : Road){
				if(ro.tileX == x
						&& ro.tileY == y){
					i = Road.indexOf(ro);
					break;
				}
			}
			return i;
		}
		
		public void findBonuses(Car self, World world, Game game){
			final int RepairKitPriority = 3;
			final int ScorePriority = 2;
			final int NitroPriority = 1;
			
			int priority = 0;
			Bonus selected = null;
			
			for(Bonus current : world.getBonuses()){
				// Определить принадлежность бонуса нескольким клетакам перед кодемобилем.
				
				int bonusTileX, bonusTileY;
				bonusTileX = (int)(current.getX() / game.getTrackTileSize());
				bonusTileY = (int)(current.getY() / game.getTrackTileSize());
		
				int i = roadTileIndex(bonusTileX, bonusTileY);
				
				if(i==-1)
					continue;
				
				if(i > 0 && i < 3){
					boolean line = true;
					for(int j=0;j<=i;j++)
						if(Road.get(j).role != TileRole.Straight)
							line = false;
					
					if(!line)
						continue;
					
					if(Math.tan(self.getAngleTo(current.getX(), current.getY())) * self.getDistanceTo(current) > game.getCarWidth() * 3){
					//if(Math.abs(self.getAngleTo(current.getX(), current.getY())) > Math.PI / 24){
						continue;
					}
					
				}
				else
					continue;
				
				
				if(Math.abs(self.getAngleTo(current.getX(), current.getY()) - self.getAngleTo(this.getTargetX(game), this.getTargetY(game))) > Math.PI / 6)
					continue;
				
				if(current.getType() == BonusType.NITRO_BOOST){
					if(priority < NitroPriority){
						selected = current;
						priority = NitroPriority;
					}
				}
				if(current.getType() == BonusType.PURE_SCORE){
					if(priority < ScorePriority){
						selected = current;
						priority = ScorePriority;
					}
				}
				
				if(current.getType() == BonusType.REPAIR_KIT){
					if(self.getDurability() < 0.5){
						if(priority < RepairKitPriority){
							selected = current;
							priority = RepairKitPriority;
						}
					}
				
				}
			}
			
			if(selected != null){
				targetBonus = selected;
			}
			
		}
	
		
		public int tilesUntilTurn(){
			int res = 0;
			for(RoadObj object : Road){
				if(Road.indexOf(object) == 0)
					continue;
				if(object.role != TileRole.Turn && object.role != TileRole.Unknown)
					res++;
				else
					break;
			}
			//System.out.println(res);
			return res;
		}
	} 
	
	Guardian G;
	
	static final int FRONTIER_TILES = 3;
	double FRONTIER;
	
	static final int TickStep = 40;
	int ticks;
	double lastV;
	
	static final double Velocity = 18.0D;
	static final double VelK = 0.6d;
	static final double VelK_S = 5.0d;
	static final double VelK_U = 0.5d;
	static final double VelK_WS = 0.9d;
	static final double VelK_WU = 5.0d;
	double acceleration;
	double power;
	static final double PowerStep = 0.05D;
	
	int nextTurn;
	boolean isTurn;
	boolean doubleTurn;
	boolean zoned;
	boolean u_turn_brake;
	boolean use_brake;
	
	static final int NitroRange = 4;
	
	static final int StopTime = 30;
	static final double StopSpeed = 0.5d;
	int stopTicks;
	
	static final int reverseMaxTime = 200;
	double reverseSigma;
	double reverseX, reverseY;
	int reverseTicks;
	boolean reverseStopping;
	
	Movement mode;
	Sector current;
	
	/** Определяет тип маневра выполняемого кодемобилем в текущий момент. */
	public enum Movement{
		/** Гоночная ехда до следующей контрольной точки. */
		RACE,
		/** Возвращение к рассчитанной траектории в случае отклонения от нее. */
		RETURNING,
		/** Обгон кодемобиля */
		CAR_OVERTAKE,
		/** Объезд препятствия */
		REVERSE,
		/** Сход с дистанции */
		RETREAT
	}
	
	/** Определяет особенности участка трассы преодолеваемой кодмобилем.
	 * Используются только в режиме RACE. */
	public enum Sector{
		/** Прямая. */
		LINE,
		/** Перед одиночным поворотом. */
		TURN,
		/** Серия поворотов идущие друг за другом в разных напрвавлениях. */
		S_TURN,
		/** Два последовательных поворота в одном направлении. Разварот на PI. */		
		U_TURN,
		/** Серия поворотов в одном направлении но с небольшим прямым участком между ними. */ 
		U_WIDE_TURN,
		/** Серия поворотов в разном направлении с небольшом прямым участком между ними.  */
		S_WIDE_TURN,
		/** Серия из 2-х поворотов с прямым участком между ними. */ 
		WIDE_TURN
		
	}
	
	private Sector spot(Car self, World world, Game game){
		double speed = hypot(self.getSpeedX(), self.getSpeedY());
		Sector newSector;
		
		switch(current){
		case LINE:
		}
		
		return null;
	}
	
	private void start(Car self, World world, Game game){
		FRONTIER = FRONTIER_TILES * game.getTrackTileSize();
		ticks = 0;
		lastV = 0;
		acceleration = 0;
		power = 0.5D;
		
		nextTurn = -1;
		isTurn = false;
		zoned = false;
		
		G = new Guardian();
		
		doubleTurn = false;
		//Road = new ArrayList<int[]>();
		
		mode = Movement.RACE;
		current = Sector.LINE;
		
		stopTicks=0;
		
		reverseStopping = false;
		reverseSigma = game.getCarHeight() * 2d;
		
		u_turn_brake = false;
		use_brake = false;
	}
		
	private double acceleration(double velocity){
		return 0.26d + 0.0077d * velocity;
	}
	
	private double getTurnZone(double speed){
		
		double s;
		double v0;
		
		s = 2 * speed;
		
		while(speed>Velocity){
			v0 = speed;
			double a = acceleration(speed);
			speed = v0 - a;
			s+= (speed+v0) / 2d;
		}
		
		return s;
		
		/*
		if(speed > 39)
			return 2000;
		if(speed > 35)
			return 1600;
		if(speed > 30)
			return 1300;
		if(speed > 25)
			return 1000;
		if(speed > 20)
			return 700;
		if(speed > 15)
			return 400;
		if(speed > 10)
			return 200;
		return 10;
		*/
	}
	
	private boolean brakeZone(Car self, World world){
		
		return false;
	}
	
	private boolean isTileTurn(World world, int index){
		int x,y;
		x = world.getWaypoints()[index][0];
		y = world.getWaypoints()[index][1];
		switch(world.getTilesXY()[x][y]){
		case LEFT_TOP_CORNER:    	
    	case RIGHT_TOP_CORNER:
    	case RIGHT_BOTTOM_CORNER:
    	case LEFT_BOTTOM_CORNER:
    		return true;
		default:
		}
		return false;
	}
	
    @Override
    public void move(Car self, World world, Game game, Move move) {
    	//*********************************************************
    	// Set up
    	
    	if(world.getTick() == 0)
    		start(self, world,game);
    	
    	
    	//*********************************************************
    	// measurement
    	
    	double speedModule = hypot(self.getSpeedX(), self.getSpeedY());
    	
    	
    	    	
    	//*********************************************************
    	//  finding a way
    	G.tick(self, world, game);
    	
    	Sector s= G.getSector();
    	
    	//*********************************************************
    	// Logger
    	/*
    	if(s == Sector.S_WIDE_TURN){
    		System.out.println("S_WIDE_TURN");
    	}
    	else if(s == Sector.U_WIDE_TURN){
    		System.out.println("U_WIDE_TURN");
    	}
    	*/
    	/*
    	if(world.getTick() > 180)
    		ticks++;
    	
    	if(ticks == TickStep){
    		// speed
    		//System.out.printf("v = %.3f   ", speedModule);
    		
    		if(s == Sector.LINE)
    			System.out.println("Line");
    		else if(s == Sector.WIDE_TURN)
    			System.out.println("WideTurn");
    		else if(s == Sector.S_TURN)
    			System.out.println("S_Turn");
    		else if(s == Sector.U_TURN)
    			System.out.println("U_Turn");
    		
    		System.out.printf("[%.3f , %.3f ", self.getX(), self.getY()); 
    		System.out.printf("|%.3f/%f] > ", speedModule, power);
    		//System.out.printf("[%.0f,%.0f|", G.getTargetX(game) , G.getTargetY(game));
    		System.out.print("[" + new Integer((int)(G.getTargetX(game) / game.getTrackTileSize())).toString() + "," + new Integer((int)(G.getTargetY(game) / game.getTrackTileSize())).toString() + "]");
        	System.out.print(printRole(G.getRole()) + "]");
        			 		
    		System.out.println("");
    		
    		lastV = speedModule;
    		ticks = 0;
    	}
    	*/
    	
    	
    	//*********************************************************
    	// Control
    	
    	if(self.isFinishedTrack())
    		return;
    	 	
    	// Shooting
    	
    	Car[] cars  = world.getCars();
    	int throwProjectile = 0;
    	int spillOil = 0;
    	for(Car c : cars){
    		if(!c.isTeammate()){
    			if(Math.abs(self.getAngleTo(c)) < game.getSideWasherAngle()
    					&& self.getType() == CarType.BUGGY
    					&& self.getDistanceTo(c) < 4 * game.getTrackTileSize()){
    				if(throwProjectile == 0)
    					throwProjectile = 1;
    			}
    			else if(Math.abs(self.getAngleTo(c)) < game.getSideWasherAngle()
    					&& self.getType() == CarType.JEEP
    					&& self.getDistanceTo(c) < 2 * game.getTrackTileSize()){
    				if(throwProjectile == 0)
    					throwProjectile = 1;
    			}
    			else if(Math.abs(self.getAngleTo(c)) > 11 * Math.PI / 12
    					&& self.getDistanceTo(c) < game.getTrackTileSize()
    					&& G.getRole() == TileRole.Turn){
    				if(spillOil == 0)
    					spillOil = 1;
    			}
    		}
    		else{  			
    			if (self.getX() != c.getX() && self.getY() != c.getY()) {
					if (Math.abs(self.getAngleTo(c)) < game
							.getSideWasherAngle()
							&& self.getDistanceTo(c) < 4 * game
									.getTrackTileSize())
						throwProjectile = -1;
					if (Math.abs(self.getAngleTo(c)) > 11 * Math.PI / 12
							&& self.getDistanceTo(c) < 2 * game.getTrackTileSize()
							&& G.getRole() == TileRole.Turn)
						spillOil = -1;				
    			}
    		}
    	}
    	
    	//System.out.println(new Integer(throwProjectile).toString());
    	
    	if(throwProjectile == 1)
    		move.setThrowProjectile(true);
    	
    	if(spillOil == 1)
    		move.setSpillOil(true);
    	
    	// Choosing mode
    	
    	
    	// power & braking
    	double distance = G.tilesUntilTurn() * game.getTrackTileSize();
    	
    	if(s == Sector.LINE)
    		zoned = (distance < getTurnZone(speedModule));
    	else
    		zoned = true;
    	
    	/*
    	switch(mode){
    	case RACE:
    		switch(current){
    		case LINE:
    			move.setWheelTurn(2 * self.getAngleTo(G.getTargetX(game), G.getTargetY(game)));
    			
    			if(power < 1.0D)
        			power+= PowerStep;
        		if(power > 1.0D)
        			power = 1.0D;
        		move.setEnginePower(power);
        		
        		if (world.getTick() > game.getInitialFreezeDurationTicks() 
                		&& distance > NitroRange * game.getTrackTileSize()
                		&& !move.isBrake()
                		&& !(G.getRole()  == TileRole.AfterTurn)) {
                    move.setUseNitro(true);
                }
    		}
    		break;
    	
    	}
    	*/
    	    	
    	
    	if(world.getTick() > game.getInitialFreezeDurationTicks())
    		if(speedModule < StopSpeed 
    				&& mode == Movement.RACE){
    			//System.out.println(new Integer(stopTicks).toString());
    			if(stopTicks>StopTime){
    				stopTicks = 0;
    				reverseStopping = false;
    				mode = Movement.REVERSE;
    				reverseTicks = 0;
    				reverseX = self.getX();// - game.getCarHeight() * Math.cos(self.getAngle());
    				reverseY = self.getY();// - game.getCarHeight() * Math.sin(self.getAngle());
    			}
    			else
    				stopTicks++;    		
    		}
    		else if(stopTicks > 0)
    			stopTicks = 0;
    	
    	
    	
    	
    	if(mode == Movement.RACE){
    	
    		doubleTurn = G.isDoubleTurn();
    		if(!doubleTurn)
    			move.setWheelTurn(2 * self.getAngleTo(G.getTargetX(game), G.getTargetY(game)));
    		else
    			move.setWheelTurn(4 * self.getAngleTo(G.getTargetX(game), G.getTargetY(game)));
    	
    		if(!doubleTurn){
    			doubleTurn = G.isDoubleTurn();
    		}
    		else{
    			doubleTurn = !(G.nextRole() == TileRole.Straight);
    		}
    		
    		
    	
    		if(zoned){ 
    			//move.setEnginePower(0.25d);
    			
    			switch(s){
    			case LINE:
    			
    				//move.setEnginePower(1d);
    				if(!use_brake
    						&& speedModule > 1.1D * Velocity){
    					use_brake = true;
    					move.setBrake(true);
    					move.setEnginePower(1d);
    					//System.out.println("Brake1");
    					
    				}
    				else if(use_brake
    						&& speedModule > Velocity){
    					use_brake = true;
    					move.setBrake(true);
    					move.setEnginePower(1d);
    				}
    				else{    					
    					move.setBrake(false);
    					
    					if(speedModule > 0.9D * Velocity)
    						move.setEnginePower(0.25d);
    					else{
    						if(power < 1.0D)
    	        				power+= PowerStep;
    	        			if(power > 1.0D)
    	        				power = 1.0D;
    	        			move.setEnginePower(1.0d);						//**
    					}
    				}
    				break;
    				
    			case S_TURN:
    				if(power < 1.0D)
        				power+= PowerStep;
        			if(power > 1.0D)
        				power = 1.0D;
        			move.setEnginePower(1.0d);								//**
        			/*
    				if(speedModule > 1.1D * Velocity * VelK_S){
        				
    					System.out.println("Brake3");
    					move.setBrake(true);
    				}
    				else
    					move.setBrake(false);
    				*/	
        			
        			if(G.canNitro_S(self, game))
        				move.setUseNitro(true);
        			
    				break;
    				
    			case U_TURN:
    				if(!u_turn_brake
    						&& speedModule > 1.2D * Velocity * VelK_U){        				
    					//System.out.println("Brake2");
    					u_turn_brake = true;
    					move.setBrake(true);
    				}    				
    				else if(u_turn_brake
    						&& speedModule > Velocity * VelK_U){
    					//System.out.println("New brake (2)!");
    					u_turn_brake = true;
    					move.setBrake(true);
    				}
    				else{
    					move.setBrake(false);
    					u_turn_brake = false;
    				}
    				
    				
    				move.setEnginePower(1D);
    				break;
    				
    			case WIDE_TURN:
    				move.setEnginePower(0.25D);
    				
    				if(! use_brake
    						&& speedModule > 1.1D * Velocity * VelK){
        				
    					//System.out.println("Brake2");
    					use_brake = true;
    					move.setBrake(true);
    				}
    				else if(use_brake 
    						&&  speedModule > Velocity * VelK){
    					use_brake = true;
    					move.setBrake(true);
    				}
    				else{
    					move.setBrake(false);
    					use_brake = false;
    				}
    				break;
    			case S_WIDE_TURN:
    				move.setEnginePower(1D);
    				
    				if(!use_brake
    						&& speedModule > Velocity * VelK_WS){
    					use_brake = true;
    					move.setBrake(true);
    				}
    				else if(use_brake
    						&& speedModule > Velocity * VelK_WS){
    					use_brake = true;
    					move.setBrake(true);
    				}
    				else{
    					use_brake = false;
    					move.setBrake(false);
    				}
    				break;
    			case U_WIDE_TURN:
    				move.setEnginePower(1.0D);    				
    				if(!use_brake
    						&& speedModule > 1.2D * Velocity * VelK_WU){
        				
    					//System.out.println("Brake2");
    					use_brake = true;
    					move.setBrake(true);
    				}
    				else if (use_brake
    						&& speedModule > Velocity * VelK_WU){
    					use_brake = true;
    					move.setBrake(true);
    				}
    				else{
    					use_brake = false;
    					move.setBrake(false);
    				}
    				break;
    			default:
    				move.setEnginePower(0.25d);
    				if(speedModule > 1.1D * Velocity){
    					move.setBrake(true);
    					//System.out.println("Brake1");
    					
    				}
    				else
    					move.setBrake(false);
    				
    				
    			}	
    			
    			
    		}
    		else{
    			use_brake = false;
    			if(power < 1.0D)
    				power+= PowerStep;
    			if(power > 1.0D)
    				power = 1.0D;
    			move.setEnginePower(power);    							//**
    			
    			if(speedModule * speedModule * Math.abs(self.getAngleTo(G.getTargetX(game), G.getTargetY(game))) > 6D * 6D * Math.PI
    					&& s != Sector.U_WIDE_TURN
    					&& Math.abs(self.getAngleTo(G.getTargetX(game), G.getTargetY(game))) > Math.PI / 4
    					/*&& G.getThisRole() != TileRole.Turn*/){
    				//System.out.println("new brake!");
    				move.setBrake(true);
    			}
    			
    		}
    		
			
			
    	        
    		if (world.getTick() > game.getInitialFreezeDurationTicks() 
    				&& distance >= NitroRange * game.getTrackTileSize()
    				&& !move.isBrake()
    				/*&& G.canNitro()*/) {
    			if(G.canNitro())
    				move.setUseNitro(true);
    		}
    	}
    	else if(mode == Movement.REVERSE){
    		
    		//System.out.println("Reverse mode!");
    		
    		reverseTicks++;
    		if(reverseStopping){
    			move.setBrake(true);
    			move.setEnginePower(0.0d);
    			 if(speedModule < StopSpeed){
    				 mode = Movement.RACE;
    	    			reverseX = 0;
    	    			reverseY = 0;
    	    			reverseTicks = 0;
    			 }
    		}
    		else if(reverseTicks < reverseMaxTime 
    				&& self.getDistanceTo(reverseX, reverseY) < reverseSigma){
    			if(Math.abs(self.getAngleTo(G.getTargetX(game), G.getTargetY(game))) < Math.PI / 3)
    				move.setWheelTurn(-1.5d * self.getAngleTo(G.getTargetX(game), G.getTargetY(game)));
    			else
    				move.setWheelTurn(-1.5d * self.getAngleTo(G.getTargetX(game), G.getTargetY(game)));
    			
    			move.setEnginePower(-0.5d);
    		}
    		else{
    			reverseStopping = true; 			
    			move.setBrake(true);
    			move.setEnginePower(0.0d);    			
    		}
    	}    	
    	else if(mode == Movement.RETREAT){
    		System.out.println("Halt!");
    		move.setEnginePower(1.0d);
    	}
       
    }
}
