package uk.org.taverna.server.client;

public enum Property {
	STDOUT("stdout"), STDERR("stderr"), EXIT_CODE("exitcode"), READY_TO_NOTIFY(
			"readyToNotify"), EMAIL("notificationAddress"), USAGE(
			"usageRecord");

	private String s;

	private Property(String s) {
		this.s = s;
	}

	@Override
	public String toString() {
		return s;
	}
}