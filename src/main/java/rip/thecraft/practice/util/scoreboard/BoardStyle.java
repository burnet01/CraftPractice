package rip.thecraft.practice.util.scoreboard;

/**
 * Created by: ThatKawaiiSam
 * Project: Assemble
 */
public enum BoardStyle {

	MODERN(false, 1);

	private int start;
	private boolean descending;

	BoardStyle(boolean descending, int start) {
		this.descending = descending;
		this.start = start;
	}

	public boolean isDescending() {
		return this.descending;
	}

	public int getStart() {
		return this.start;
	}
}
