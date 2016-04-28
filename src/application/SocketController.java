package application;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class SocketController implements SocketReceiver.OnLineReceivedListener {

	private final int INVALID_PORT = -1;
	private final String CONNECT = "Verbinden";
	private final String DISCONNECT = "Trennen";

	private Socket socket;
	private PrintWriter out;
	private SocketReceiver connectionListener;
	private boolean connected = false;
	
	@FXML
	private Button connectButton;

	@FXML
	private Button sendTelegramButton;

	@FXML
	private TextField addressTextField;

	@FXML
	private TextField portTextField;

	@FXML
	private TextArea requestTextArea;

	@FXML
	private TextArea responseTextArea;

	public void init() {
		calculateConnectionControlStates();
		responseTextArea.setEditable(false);
		sendTelegramButton.setDisable(true);
		
		addressTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			calculateConnectButtonState();
		});
	
		portTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			calculateConnectButtonState();
		});
	
		requestTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
			calculateSendButtonState();
		});
	}

	@FXML
	private void connect() throws IOException {
		if (connected) {
			stop();
			connected = false;
			emptyTextAreas();
			
		} else {
			String address = getAddress();
			int port = getPort();
			
			if (port != INVALID_PORT && !address.isEmpty()) {
				connected = connectToHost(address, port);
			}
			
		}
		
		calculateConnectionControlStates();
	}

	private void emptyTextAreas() {
		requestTextArea.setText("");
		responseTextArea.setText("");
	}

	public void stop() throws IOException {
		if (connectionListener != null)
			connectionListener.stop();
		
		if (out != null)
			out.close();
	}

	private boolean connectToHost(String hostName, int hostPort) {
		try {
			socket = new Socket(hostName, hostPort);
			out = new PrintWriter(socket.getOutputStream(), true);
	
			connectionListener = new SocketReceiver(socket, this);
			Thread connectionListenerThread = new Thread(connectionListener);
			connectionListenerThread.start();
			return true;
			
		} catch (UnknownHostException e) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Verbindungsfehler");
			alert.setContentText("Unbekannter Host: " + hostName);
			alert.show();
			System.err.println(e);
		} catch (IOException e) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Verbindungsfehler");
			alert.setContentText("Verbindung zum Host '" + hostName + "' ist fehlgeschlagen.");
			alert.show();
			System.err.println(e);
		}
		
		return false;
	}

	@FXML
	private void send() {
		String text = requestTextArea.getText();
		out.println(text);
		requestTextArea.setText(new String(""));
	}

	private String getAddress() {
		return addressTextField.getText();
	}

	private int getPort() {
		String portText = portTextField.getText();
		
		if (!portText.isEmpty()) {
			
			try {
				return Integer.parseInt(portText);
			} catch (NumberFormatException e) {
				Alert alert = new Alert(AlertType.WARNING);
				alert.setTitle("Ungültiger Port");
				alert.setContentText("Der Port '" + portText + "' ist nicht gültig.");
				alert.showAndWait();
			}
		}
		
		return INVALID_PORT;
	}
	
	private void calculateConnectionControlStates() {
		connectButton.setText(connected ? DISCONNECT : CONNECT);
		calculateConnectButtonState();
		requestTextArea.setEditable(connected);
	}

	private void calculateConnectButtonState() {
		String address = getAddress();
		int port = getPort();

		connectButton.setDisable(!connected && (address == null || address.isEmpty() || port == INVALID_PORT));
	}

	private void calculateSendButtonState() {
		String request = requestTextArea.getText();

		sendTelegramButton.setDisable(request == null || request.isEmpty() || out == null);
	}

	@Override
	public void onLineReceived(String line) {
		appendLine(line);
	}
	
	public synchronized void appendLine(String line) {
		Platform.runLater( () -> responseTextArea.appendText(line + System.lineSeparator()));
	}
}
