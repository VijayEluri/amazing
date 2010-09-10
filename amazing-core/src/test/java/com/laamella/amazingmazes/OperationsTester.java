package com.laamella.amazingmazes;

import static com.laamella.amazingmazes.mazemodel.MazeDefinitionState.*;

import org.grlea.log.SimpleLogger;
import org.junit.Before;
import org.junit.Test;

import com.laamella.amazingmazes.generators.Randomizer;
import com.laamella.amazingmazes.generators.daedalus.PrimMazeGenerator;
import com.laamella.amazingmazes.mazemodel.*;
import com.laamella.amazingmazes.mazemodel.grid.Grid;
import com.laamella.amazingmazes.mazemodel.grid.implementation.GridMatrixStorage;
import com.laamella.amazingmazes.mazemodel.grid.implementation.GridWithDecoupledState;
import com.laamella.amazingmazes.mazemodel.matrix.implementation.StateMatrix;
import com.laamella.amazingmazes.operations.*;

public class OperationsTester {
	private static final SimpleLogger log = new SimpleLogger(OperationsTester.class);

	private StateMatrix mazeStateMatrix;
	private Grid.UtilityWrapper grid;
	private GridMatrixStorage stateStorage;

	private Randomizer.Default randomGenerator;

	private PrimMazeGenerator mazeGenerator;

	private final StateMatrixPrettyPrinter defaultStateMatrixPrettyPrinter = new StateMatrixPrettyPrinter();

	@Before
	public void before() {
		mazeStateMatrix = new StateMatrix(new Size(149, 41));
		stateStorage = new GridMatrixStorage(mazeStateMatrix);
		grid = new Grid.UtilityWrapper(new GridWithDecoupledState(stateStorage));
		randomGenerator = new Randomizer.Default();
		mazeGenerator = new PrimMazeGenerator(randomGenerator);
		grid.getTopLeftSquare().setState(MazeDefinitionState.ENTRANCE, true);
		mazeGenerator.generateMaze(grid);
		mazeStateMatrix.addObserver(new PrettyPrintObserver(mazeStateMatrix));
	}

	@Test
	public void testDistanceMarkingOperation() {
		new VertexDistanceMarker().mark(grid.getSquare(new Position(5, 5)));

		final StateMatrixPrettyPrinter stateMatrixPrettyPrinter = new StateMatrixPrettyPrinter('#');
		stateMatrixPrettyPrinter.map(VertexDistanceMarker.DISTANCE);
		stateMatrixPrettyPrinter.map(PASSAGE, ' ');
		log.debug(stateMatrixPrettyPrinter.getPrintableMaze(mazeStateMatrix));
	}

	@Test
	public void testMostDistanceExitMarkingOperation() {
		new MostDistantExitMarker().findMostDistantExit(grid);

		final StateMatrixPrettyPrinter stateMatrixPrettyPrinter = new StateMatrixPrettyPrinter('#');
		stateMatrixPrettyPrinter.map(VertexDistanceMarker.DISTANCE);
		stateMatrixPrettyPrinter.map(PASSAGE, ' ');
		log.debug(stateMatrixPrettyPrinter.getPrintableMaze(mazeStateMatrix));
		log.debug(defaultStateMatrixPrettyPrinter.getPrintableMaze(mazeStateMatrix));
	}

	@Test
	public void testDistanceFromDeadEndMarkerOperation() {
		final DistanceFromDeadEndMarker longestPathFinderOperation = new DistanceFromDeadEndMarker();

		final StateMatrixPrettyPrinter stateMatrixPrettyPrinter = new StateMatrixPrettyPrinter('#');
		stateMatrixPrettyPrinter.map(DistanceFromDeadEndMarker.DISTANCE_FROM_DEAD_END);
		stateMatrixPrettyPrinter.map(PASSAGE, ' ');
		
		longestPathFinderOperation.addObserver(new PrettyPrintObserver(mazeStateMatrix, stateMatrixPrettyPrinter));

		longestPathFinderOperation.execute(grid);
		
		log.debug(stateMatrixPrettyPrinter.getPrintableMaze(mazeStateMatrix));
	}

	@Test
	public void testMostDistantEntranceAndExitFinder() {
		grid.clearState(ENTRANCE);
		grid.clearState(EXIT);
		new DistanceFromDeadEndMarker().execute(grid);
		new MostDistantEntranceAndExitFinder().execute(grid);
	}


}