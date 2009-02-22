package com.laamella.amazing.mazemodel.orthogonal.implementation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.laamella.amazing.mazemodel.Position;
import com.laamella.amazing.mazemodel.Size;
import com.laamella.amazing.mazemodel.graph.Edge;
import com.laamella.amazing.mazemodel.graph.Vertex;
import com.laamella.amazing.mazemodel.matrix.ArrayUtilities;
import com.laamella.amazing.mazemodel.orthogonal.Direction;
import com.laamella.amazing.mazemodel.orthogonal.Grid;
import com.laamella.amazing.mazemodel.orthogonal.Square;
import com.laamella.amazing.mazemodel.orthogonal.Wall;

import static com.laamella.amazing.mazemodel.orthogonal.Direction.*;

/**
 * Grid knows about relationships between squares and walls, but knows nothing
 * about their state. That is delegated to objects returned from the
 * storageFactory.
 */
public class GridWithDecoupledStorage implements Grid {
	private final SquareDefault[][] squares;
	private final Size size;
	private final WallDefault[][] horizontalWalls;
	private final WallDefault[][] verticalWalls;
	private Square exit;
	private Square entrance;
	private Set<Edge> edges = new HashSet<Edge>();
	private Set<Vertex> vertices = new HashSet<Vertex>();

	public GridWithDecoupledStorage(final GridStorageFactory storageFactory) {
		this.size = storageFactory.getSize();
		squares = new SquareDefault[size.width][size.height];

		// TODO this is becoming a little dramatic

		horizontalWalls = new WallDefault[size.width][size.height + 1];
		verticalWalls = new WallDefault[size.width + 1][size.height];
		ArrayUtilities.visit2dArray(horizontalWalls, new ArrayUtilities.Visitor2dArray() {
			public void visit(Position position) {
				final WallStorage horizontalWallStorage = storageFactory.createHorizontalWallStorage(position);
				final WallDefault wall = new WallDefault(horizontalWallStorage);
				horizontalWalls[position.x][position.y] = wall;
				edges.add(wall);
			}
		});
		ArrayUtilities.visit2dArray(verticalWalls, new ArrayUtilities.Visitor2dArray() {
			public void visit(Position position) {
				final WallStorage verticalWallStorage = storageFactory.createVerticalWallStorage(position);
				final WallDefault wall = new WallDefault(verticalWallStorage);
				verticalWalls[position.x][position.y] = wall;
				edges.add(wall);
			}
		});
		ArrayUtilities.visit2dArray(squares, new ArrayUtilities.Visitor2dArray() {
			public void visit(Position position) {
				final SquareStorage squareStorage = storageFactory.createSquareStorage(position);
				final SquareDefault square = new SquareDefault(squareStorage, position);
				squares[position.x][position.y] = square;
				vertices.add(square);
			}
		});

		ArrayUtilities.visit2dArray(horizontalWalls, new ArrayUtilities.Visitor2dArray() {
			public void visit(Position position) {
				if (position.y == 0 || position.y == getSize().height) {
					return;
				}
				final Square squareAbove = getSquare(position.move(UP.getMove()));
				final Square squareBelow = getSquare(position);
				horizontalWalls[position.x][position.y].connect(squareAbove, squareBelow);
			}
		});
		ArrayUtilities.visit2dArray(verticalWalls, new ArrayUtilities.Visitor2dArray() {
			public void visit(Position position) {
				if (position.x == 0 || position.x == getSize().width) {
					return;
				}
				final Square squareLeft = getSquare(position.move(LEFT.getMove()));
				final Square squareRight = getSquare(position);
				verticalWalls[position.x][position.y].connect(squareLeft, squareRight);
			}
		});
		ArrayUtilities.visit2dArray(squares, new ArrayUtilities.Visitor2dArray() {
			public void visit(Position position) {
				squares[position.x][position.y].connect(GridWithDecoupledStorage.this);
			}
		});
	}

	public Size getSize() {
		return size;
	}

	public Square getSquare(Position position) {
		return squares[position.x][position.y];
	}

	public Wall getHorizontalWall(int x, int y) {
		return horizontalWalls[x][y];
	}

	public Wall getVerticalWall(int x, int y) {
		return verticalWalls[x][y];
	}

	public Square getEntrance() {
		return entrance;
	}

	public Square getExit() {
		return exit;
	}

	public void setEntrance(Square entrance) {
		this.entrance = entrance;
	}

	public void setExit(Square exit) {
		this.exit = exit;
	}

	@Override
	public Set<Edge> getEdges() {
		return Collections.unmodifiableSet(edges);
	}

	@Override
	public Set<Vertex> getVertices() {
		return Collections.unmodifiableSet(vertices);
	}

	public static class SquareDefault implements Square {
		private final SquareStorage storage;
		private final Position position;
		private DirectionMap<Wall> wallMap;
		private DirectionMap<Square> squareMap;
		private Set<Edge> edges;

		public SquareDefault(final SquareStorage squareStorage, final Position position) {
			this.storage = squareStorage;
			this.position = position;
		}

		void connect(final GridWithDecoupledStorage grid) {
			if (wallMap == null) {
				wallMap = new DirectionMap<Wall>(grid.getHorizontalWall(position.x, position.y), grid.getVerticalWall(position.x + 1, position.y), grid
						.getHorizontalWall(position.x, position.y + 1), grid.getVerticalWall(position.x, position.y));
			}
			if (squareMap == null) {
				squareMap = new DirectionMap<Square>();
				if (position.y > 0) {
					squareMap.up = grid.getSquare(position.move(UP.getMove()));
				}
				if (position.x < grid.getSize().width - 1) {
					squareMap.right = grid.getSquare(position.move(RIGHT.getMove()));
				}
				if (position.y < grid.getSize().height - 1) {
					squareMap.down = grid.getSquare(position.move(DOWN.getMove()));
				}
				if (position.x > 0) {
					squareMap.left = grid.getSquare(position.move(LEFT.getMove()));
				}
			}
			if (edges == null) {
				edges = new HashSet<Edge>();
				final Square leftSquare = getSquare(LEFT);
				if (leftSquare != null) {
					edges.add(getWall(LEFT));
				}
				final Square rightSquare = getSquare(RIGHT);
				if (rightSquare != null) {
					edges.add(getWall(RIGHT));
				}
				final Square upSquare = getSquare(UP);
				if (upSquare != null) {
					edges.add(getWall(UP));
				}
				final Square downSquare = getSquare(DOWN);
				if (downSquare != null) {
					edges.add(getWall(DOWN));
				}
			}

		}

		public Wall getWall(Direction wall) {
			return wallMap.get(wall);
		}

		public Square getSquare(Direction direction) {
			return squareMap.get(direction);
		}

		public Position getPosition() {
			return position;
		}

		public boolean hasState(Object state) {
			return storage.hasState(state);
		}

		public void setState(Object newState, boolean hasOrNot) {
			storage.setState(newState, hasOrNot);
		}

		@Override
		public Set<Edge> getEdges() {
			return edges;
		}
	}

	public static class WallDefault implements Wall {
		private final WallStorage storage;
		private Square squareA;
		private Square squareB;

		public WallDefault(final WallStorage storage) {
			this.storage = storage;
		}

		void connect(Square squareA, Square squareB) {
			this.squareA = squareA;
			this.squareB = squareB;
		}

		public boolean isOpen() {
			return storage.hasState(OPEN);
		}

		public void setOpened(boolean open) {
			storage.setState(OPEN, open);
		}

		public void close() {
			setOpened(false);
		}

		public void open() {
			setOpened(true);
		}

		public boolean hasState(Object state) {
			return storage.hasState(state);
		}

		public void setState(Object newState, boolean hasOrNot) {
			storage.setState(newState, hasOrNot);
		}

		@Override
		public Vertex getVertexA() {
			return squareA;
		}

		@Override
		public Vertex getVertexB() {
			return squareB;
		}

		@Override
		public Vertex travel(final Vertex sourceVertex) {
			if (sourceVertex.equals(squareA)) {
				return squareB;
			}
			if (sourceVertex.equals(squareB)) {
				return squareA;
			}
			throw new IllegalArgumentException("Can't travel, edge does not belong to vertex.");
		}

	}
}
