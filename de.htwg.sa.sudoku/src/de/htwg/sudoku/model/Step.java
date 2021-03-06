package de.htwg.sudoku.model;

public class Step {
	
	private int row;
	private int column;
	private int value;
	private String reason;

	public Step(int row, int column, int value, String reason) {
		this.row = row;
		this.column = column;
		this.value = value;
		this.reason = reason;
	}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}

	public int getColumn() {
		return column;
	}

	public void setColumn(int column) {
		this.column = column;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

    public String getReason() {
        return reason;
    }

}
