package oakbot.task;

import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import oakbot.bot.Bot;
import oakbot.bot.PostMessage;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;

/**
 * Regularly posts messages depending on how many security updates are
 * available.
 * @author Michael Angstadt
 */
public abstract class HealthMonitor implements ScheduledTask {
	private static final Logger logger = Logger.getLogger(HealthMonitor.class.getName());

	private final String[] responses = { "coughs", "sneezes", "clears throat", "expectorates", "sniffles", "wheezes", "groans", "moans" };
	private final List<Integer> roomIds;
	private boolean first = true;
	private int securityUpdates;

	/**
	 * @param roomIds the rooms to post the messages to
	 */
	public HealthMonitor(List<Integer> roomIds) {
		this.roomIds = roomIds;
	}

	@Override
	public long nextRun() {
		if (first) {
			first = false;

			logger.info("Starting health monitor...");

			/*
			 * Do not start the health monitor if there is a problem checking
			 * for security updates.
			 */
			try {
				securityUpdates = getNumSecurityUpdates();
			} catch (Exception e) {
				logger.log(Level.INFO, "Could not query system for security updates.  Health monitor disabled.", e);
				return 0;
			}

			logger.info("Health monitor started.  There are " + securityUpdates + " security updates available.");
		}

		/*
		 * Don't post anything if there are less than 10 updates. But check
		 * again tomorrow.
		 */
		if (securityUpdates < 10) {
			return Duration.ofDays(1).toMillis();
		}

		double timesToPostPerDay = securityUpdates / 30.0;
		if (timesToPostPerDay > 8) {
			timesToPostPerDay = 8;
		}
		return (long) ((24 / timesToPostPerDay) * 60 * 60 * 1000);
	}

	@Override
	public void run(Bot bot) {
		try {
			securityUpdates = getNumSecurityUpdates();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Problem querying for security updates.", e);
			securityUpdates = 0;
		}

		if (securityUpdates >= 10) {
			for (int roomId : roomIds) {
				ChatBuilder cb = new ChatBuilder();
				cb.italic(Command.random(responses));
				PostMessage response = new PostMessage(cb).bypassFilters(true);
				try {
					bot.sendMessage(roomId, response);
				} catch (Exception e) {
					logger.log(Level.WARNING, "Problem sending cough message [room=" + roomId + "].", e);
				}
			}
		}
	}

	/**
	 * Queries the local operating system for the number of security updates
	 * that are available.
	 * @return the number of security updates or null if they couldn't be
	 * retrieved
	 * @throws Exception if there is any sort of problem
	 */
	public abstract int getNumSecurityUpdates() throws Exception;
}
