package webserver;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;

@SuppressWarnings("serial")
public class webclient extends JFrame implements ActionListener {
	private static InetAddress host;
	private static final int PORT = 1234;

	private JTextArea logDisplay = new JTextArea();
	private JButton CloseButton = new JButton();
	private JButton ViewButton = new JButton();
	private JButton DownloadButton = new JButton();
	private static webclient frame;
	private static PrintWriter networkOutput;
	private static Scanner networkInput;
	private ChatThread chatThread;
	private static Socket socket;
	private final String CRLF = "" + "\r\n"; // terminate output stream (http), carriage return and a line feed

	public webclient() {

		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();

		logDisplay = new JTextArea(10, 15);
		logDisplay.setWrapStyleWord(true);
		logDisplay.setLineWrap(true);
		logDisplay.setEditable(false);

		CloseButton = new JButton("Close");
		CloseButton.addActionListener(this);
		constraints.gridx = 2;
		constraints.gridy = 0;
		constraints.insets = new Insets(10, 10, 10, 10);
		add(CloseButton, constraints);

		ViewButton = new JButton("View");
		ViewButton.addActionListener(this);
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(10, 10, 10, 10);
		add(ViewButton, constraints);

		DownloadButton = new JButton("Download");
		DownloadButton.addActionListener(this);
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.insets = new Insets(10, 10, 10, 10);
		add(DownloadButton, constraints);

		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridwidth = 2;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		add(new JScrollPane(logDisplay), constraints);

		chatThread = new ChatThread();
		chatThread.start();

	}

	public static void main(String[] args) {
		connectToServer();
		frame = new webclient();
		frame.setTitle("New Client");
		frame.setSize(700, 500);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setLocationRelativeTo(null);

	}

	public static void connectToServer() {
		try {
			host = InetAddress.getLocalHost();
		} catch (UnknownHostException uhEx) {
			System.out.println("\nHost ID not found!\n");
			System.exit(1);
		}
		try {
			socket = new Socket(host, PORT);

			networkInput = new Scanner(socket.getInputStream());
			networkOutput = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException ioEx) {
			ioEx.printStackTrace();
			System.exit(1);
		}
	}

	public void actionPerformed(ActionEvent event) {

		if (event.getSource() == ViewButton) {
			logDisplay.append("\n********** View Contents Of Directory **********");
			try {
				sendClientRequest(networkOutput);
				return;
			} catch (Exception e) {
				e.printStackTrace();
			} // send http-get request to server

		}

		if (event.getSource() == DownloadButton) {

			logDisplay.append("\n********** FILE DOWNLOAD **********\n");
			String filename = "";
			filename = JOptionPane.showInputDialog("Enter name of file to download (with extensions)");
			if (filename.length() != 0) // user input isnt empty
			{
				logDisplay.append("> You requested to download file '" + filename + "'\n");
				try {
					sendClientRequest(networkOutput);
					return;
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}

		}
		if (event.getSource() == CloseButton) {

			try {
				// on the server terminal
				System.out.println("Closing Connection");
				JOptionPane.showMessageDialog(frame, "Closing Connection");
				// on the GUI
				logDisplay.append("The connection is closed");
				if (socket != null) {
					socket.close();
				}
				setVisible(false);

			} catch (IOException ioEx) {
				System.out.println("Unable to disconnect!");
				System.exit(1);
			}
		}

	}

	// sends http client requests to server via outputstream with the Get method
	private void sendClientRequest(PrintWriter out) throws Exception {

		String requestLine = "GET/List Of Files In Directory HTTP/1.1";
		// to get the files and directories in specified path
		File folder = new File("C:\\Users\\alienware\\Desktop");
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				logDisplay.append("\n File " + listOfFiles[i].getName());
			} else if (listOfFiles[i].isDirectory()) {
				logDisplay.append("\n Directory " + listOfFiles[i].getName());
			}
		}

		String contentLength = "";

		String headerLines = "Host:LocalHost Port:1234" + CRLF + "User-Agent:Eclipse 19-20" + CRLF + contentLength + CRLF
				+ "Date:" + getHTTPTime();
		// send request to server
		out.println("viewFile");
//		out.println(requestLine);
		out.println(headerLines);
		System.out.println("requestLine ===" + requestLine);
		System.out.println("headerlines ===" + headerLines);
	}

	// returns time as string in http-format
	private static String getHTTPTime() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(calendar.getTime());
	}

	class ChatThread extends Thread {
		public void run() {
			do {

				if (networkInput.hasNext()) {
					String actionType = networkInput.next();
					if (actionType.equals("viewFile")) {

						String requestLine = networkInput.next();
						String headerLines = networkInput.next();

						logDisplay.append("\n" + requestLine);
						logDisplay.append("\n" + headerLines);
					}

					if (actionType.equals("downloadFile")) {

						String requestLine = networkInput.next();

						logDisplay.append("\n" + requestLine);

					}
				}

			} while (true);
		}
	}
}
