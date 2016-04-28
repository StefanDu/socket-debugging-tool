package application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Class for asynchronous receiving messages from a socket. Informs the listener about incoming message lines.
 */
public class SocketReceiver implements Runnable {
	
    /**
     * Interface to be implemented by an input listener.
     */
    public interface OnLineReceivedListener {
        public void onLineReceived(String line);
    }
    
	private Socket socket;
	private OnLineReceivedListener listener;
	private boolean closing = false;

	public SocketReceiver(Socket socket, OnLineReceivedListener listener) {
		this.socket = socket;
		this.listener = listener;
	}

	public void run() {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			System.out.println("in or out failed");
			System.exit(-1);
		}

		while (!Thread.currentThread().isInterrupted()) {
			try {
				listener.onLineReceived(in.readLine());
			} catch (IOException e) {
				if (closing)
					break;
				
				System.out.println("Read failed");
				System.exit(-1);
			}
		}
	}
	
	public synchronized void stop() {
		closing = true;
		Thread.currentThread().interrupt();
	}

}
