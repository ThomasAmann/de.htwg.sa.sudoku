package de.htwg.sudoku.persistence.couchdb;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.Revision;
import org.ektorp.ViewQuery;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import de.htwg.sudoku.model.ICell;
import de.htwg.sudoku.model.IGrid;
import de.htwg.sudoku.model.impl.Grid;
import de.htwg.sudoku.persistence.IGridDAO;

public class GridCouchdbDAO implements IGridDAO {

	private CouchDbConnector db = null;
	private Logger logger = Logger.getLogger("de.htwg.sudoku.persistence.couchdb");
	
	public GridCouchdbDAO() {
		HttpClient client = null;
		try {
			client = new StdHttpClient.Builder().url(
					"http://lenny2.in.htwg-konstanz.de:5984").build();

		} catch (MalformedURLException e) {
		    logger.error("Malformed URL", e);
		}
		CouchDbInstance dbInstance = new StdCouchDbInstance(client);
		db = dbInstance.createConnector("sudoku_db", true);
		db.createDatabaseIfNotExists();
	}

	private IGrid copyGrid(PersistentGrid pgrid) {
		if (pgrid == null) {
			return null;
		}
		IGrid grid = new Grid(pgrid.getBlocksPerEdge());
		grid.setId(pgrid.getId());
		grid.setName(pgrid.getName());

		for (PersistentCell cellBase : pgrid.getCells()) {
			int column = cellBase.getColumn();
			int row = cellBase.getRow();
			int value = cellBase.getValue();
			boolean given = cellBase.getGiven();

			ICell cell = grid.getICell(column, row);
			cell.setValue(value);
			cell.setGiven(given);
		}

		return grid;
	}

	private PersistentGrid copyGrid(IGrid grid) {
		if (grid == null) {
			return null;
		}

		String gridId = grid.getId();
		PersistentGrid pgrid;
		if (containsGridById(gridId)) {
			// The Object already exists within the session
			pgrid = (PersistentGrid) db.find(PersistentGrid.class, gridId);

			// Synchronize values
			Set<PersistentCell> cells = pgrid.getCells();
			for (PersistentCell c : cells) {
				int col = c.getColumn();
				int row = c.getRow();
				c.setValue(grid.getICell(col, row).getValue());
			}

		} else {
			// A new database entry
			pgrid = new PersistentGrid();

			Set<PersistentCell> cells = new HashSet<PersistentCell>();

			for (int column = 0; column < Math.pow(grid.getBlockSize(), 2); column++) {
				for (int row = 0; row < Math.pow(grid.getBlockSize(), 2); row++) {
					ICell cell = grid.getICell(column, row);

					PersistentCell pcell = new PersistentCell(column, row);
					pcell.setGiven(cell.isGiven());
					pcell.setValue(cell.getValue());

					cells.add(pcell);
				}
			}
			pgrid.setCells(cells);
		}

		pgrid.setId(grid.getId());
		pgrid.setName(grid.getName());
		pgrid.setBlocksPerEdge(grid.getBlockSize());
		pgrid.setNumberSetCells(grid.getNumberSetCells());

		return pgrid;
	}

	@Override
	public void saveGrid(IGrid grid) {
		if (containsGridById(grid.getId())) {
			db.update(copyGrid(grid));
		} else {
			db.create(grid.getId(), copyGrid(grid));
		}
	}

	@Override
	public boolean containsGridById(String id) {
		if (getGridById(id) == null) {
			return false;
		}
		return true;
	}

	@Override
	public List<IGrid> getGridsByDifficulty(int max, int min) {
		List<IGrid> newL = new ArrayList<IGrid>();
		for (IGrid g : getAllGrids()) {
			int diff = g.getNumberSetCells();
			if (diff < max && diff > min) {
				newL.add(g);
			}
		}
		return newL;
	}

	@Override
	public IGrid getGridById(String id) {
		PersistentGrid g = db.find(PersistentGrid.class, id);
		if (g == null) {
			return null;
		}
		return copyGrid(g);
	}

	@Override
	public void deleteGridById(String id) {
		db.delete(copyGrid(getGridById(id)));
	}

	@Override
	public List<IGrid> getAllGrids() {
		ViewQuery q = new ViewQuery().allDocs().includeDocs(true);
				
		List<IGrid> lst = new ArrayList<IGrid>();
		for (PersistentGrid pGrid : db.queryView(q, PersistentGrid.class)) {
			lst.add(copyGrid(pGrid));
		}

		return lst;
	}

	@Override
	public void generateGrids(int number, int gridSize) {
		List<PersistentGrid> pgridList = new ArrayList<PersistentGrid>();
		
		for (int i = 0; i < number; i++) {
			IGrid grid = new Grid(gridSize);
			grid.create();
			pgridList.add(copyGrid(grid));
		}
		
		db.executeBulk(pgridList);
	}

	@SuppressWarnings("unused")
	private void deleteAll() {
		List<String> l = db.getAllDocIds();
		for (String id : l) {
			List<Revision> r = db.getRevisions(id);
			for (int i = 0; i < r.size(); i++) {
				try {
					db.delete(id, r.get(i).getRev());
				} catch (Exception e) {
				    logger.error("An Error occured: ", e);
				}
			}

		}
	}
}
