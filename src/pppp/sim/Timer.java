package pppp.sim;

import java.util.concurrent.*;

class Timer extends Thread {

	private boolean start = false;
	private boolean finished = false;
	private Callable <?> task = null;
	private Exception error = null;
	private Object result = null;

	public <T> T call(Callable <T> task, long timeout) throws Exception
	{
		if (!isAlive())
			throw new IllegalStateException();
		if (task == null || timeout < 0)
			throw new IllegalArgumentException();
		this.task = task;
		synchronized (this) {
			start = true;
			notify();
		}
		synchronized (this) {
			if (finished == false)
				try {
					wait(timeout);
				} catch (InterruptedException e) {}
		}
		if (finished == false)
			throw new TimeoutException();
		finished = false;
		if (error != null) throw error;
		@SuppressWarnings("unchecked")
		T result_T = (T) result;
		return result_T;
	}

	public void run()
	{
		for (;;) {
			synchronized (this) {
				if (start == false)
					try {
						wait();
					} catch (InterruptedException e) {}
			}
			start = false;
			error = null;
			try {
				result = task.call();
			} catch (Exception e) {
				error = e;
			}
			synchronized (this) {
				finished = true;
				notify();
			}
		}
	}
}
