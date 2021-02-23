package webserver;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;


import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;


//redirect sys.out.print to gui
class CustomOutputStream extends OutputStream {
	private JTextArea textArea;

	// constructor
	public CustomOutputStream(JTextArea textArea) {
		this.textArea = textArea;
	}

	// writes to textarea with input "b" (char as int) and refocuses textarea to
	// bottom
	@Override
	public void write(int b) throws IOException {
		// redirects data to the text area
		textArea.append(String.valueOf((char) b));
		// scrolls the text area to the end of data
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

}

@SuppressWarnings("serial")
public class webserver extends JFrame {
	private static ServerSocket serverSocket;
	private static final int PORT = 1234;


	private JTextArea textArea;
	private JButton buttonClear = new JButton("Clear");
	private static String path;

	// constructor
	public webserver() {

		super("Server Log"); 

		textArea = new JTextArea(50, 10);
		textArea.setEditable(false);
		PrintStream printStream = new PrintStream(new CustomOutputStream(textArea));


		// re-assigns standard output stream and error output stream
		System.setOut(printStream);
		System.setErr(printStream);


		//clear button
		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(10, 10, 10, 10);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridx = 1;
		add(buttonClear, constraints);

		//text area
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridwidth = 2;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		add(new JScrollPane(textArea), constraints);

		//adds event handler for button Clear
		buttonClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				// clears the text area
				try {
					textArea.getDocument().remove(0, textArea.getDocument().getLength());
					 System.out.println("Text area cleared");
				} catch (BadLocationException ex) {
					ex.printStackTrace();
				}
			}
		});

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(500, 400);
		setLocationRelativeTo(null);
	}

	public static void main(String[] args) throws IOException {

		// Runs the gui program.
		webserver mainFrame = new webserver();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
			
				mainFrame.setVisible(true);
			
				
			}
		});

		boolean isValidPath = false;
		boolean serverConnected = false; // boolean checks if server connected successfully

		while (!isValidPath) {
			// ask user for server directory path
			path = (String) JOptionPane.showInputDialog(mainFrame,
					"Enter Server Directory Path (enter -1 to exit Server): \n", "Input Dialog",
					JOptionPane.PLAIN_MESSAGE, null, // no icon
					null, // no drop down
					null // no prompt text
			);

			// If a string was entered by user
			if ((path != null) && (path.length() > 0)) {
				if (path.equals("-1")) {
					System.exit(0);
				}

				isValidPath = checkPath(path); // check if path valid
				if (!isValidPath) // display error msg if invalid
				{
					System.out.println(" !! \"" + path + "\" is not a valid directory path. Try Again.");
				}
			} else // user didnt enter anything
			{
				System.out.println(" !! You must enter a file path. Try Again.");
			}
		}

		System.out.println("\nCurrent Server Directory: " + path);
		System.out.println("--------------------------------------------------------------------------");

		// Establish server socket listening to port 1234
		try {
			serverSocket = new ServerSocket(PORT);
			serverConnected = true; 
		} catch (IOException ioEx) {
			System.out.println("\nUnable to set up port!");
			System.exit(1);
		}
		if (serverConnected) {
			System.out.println("Server connected. Listening for requests at port " + PORT);

			do {
				// Wait for client...
				Socket client = serverSocket.accept();

				System.out.println("\nNew client accepted.\n");

				// Create a thread to handle communication with
				// this client and pass the constructor for this
				// thread a reference to the relevant socket...
				ClientHandler handler = new ClientHandler(client, path);

				handler.start();// As usual, this method calls run.
			} while (true);
		}
	}// end of the main

	// takes file path as input and returns true if path leads to valid directory
	private static boolean checkPath(String path) {
		boolean isValid = false;
		try {
			File test = new File(path); // throws exception if invalid path
			if (test.isFile()) // false if it's file, not folder
				isValid = false;
			else if (test.isDirectory()) // return true only if path leads to valid directory
				isValid = true;
		} catch (Exception e) {
			isValid = false;
			e.printStackTrace();
		}
		return isValid;
	}

}

class ClientHandler extends Thread {


	private Socket client;
	private Scanner input;
	private PrintWriter Dataoutput;
	private String clientName;
	final 	String CRLF = "\r\n"; 

	public ClientHandler(Socket socket, String p) {
		// Set up reference to associated socket...
		client = socket;
	}

	public void run() {
		do {
			try {
				input = new Scanner(client.getInputStream());
				Dataoutput = new PrintWriter(client.getOutputStream(), true);
			} catch (IOException ioEx) {
				ioEx.printStackTrace();
			}

			String actionType = "";
			actionType = input.next();
			System.out.println("actionType======"+actionType);

			if (actionType.equals("sendClientPath")) {
				String clientPath = input.nextLine();
				System.out.println("clientPath==========="+clientPath);
				String[] tokens = clientPath.split(" "); // split at whiteaspaces
				String requestType = tokens[0]; // GET or POST
				String requestedItem = tokens[1]; // NAME OF FILE (or filelist)

				// collect client headerlines
				String headerLine = "";
				String temp = "";
				while ((temp = input.nextLine()).length() != 0) {
					headerLine += temp + "\n";
				}

				if (requestType == "GET") {
					if (requestedItem.equals("/File-List")) // get file list and send to client
					{
						System.out.println("\n!------ " + this.clientName + " REQUESTED TO VIEW Content Of Directory LIST ------!");
						System.out.println(clientPath);
						System.out.println(headerLine);
						try {
							sendFileList(Dataoutput);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else // CLIENT requests a file
					{
						System.out.println("\n******* " + this.clientName + " REQUESTED TO DOWNLOAD A FILE *******");
						System.out.println(clientPath);
						System.out.println(headerLine);
						try {
							sendFile(Dataoutput, requestedItem);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

			} else if (actionType.equals("viewFile")) {
				String requestLine; // reads client requests
				String headerLines; // reads client requests

				// Accept message from client on
				// the socket's input stream...
				// received is reference for client path
				requestLine = input.next();
				headerLines = input.next();
				
				// Echo message back to client on
				// the socket's output stream...
				Dataoutput.println("viewFile");
				Dataoutput.println(requestLine);
				Dataoutput.println(headerLines);
			} else if (actionType.equals("downloadFile")) {
//				To be implemented later
				Dataoutput.println("downloadFile");

			}
			
		} while (true); // Repeat above until 'QUIT' sent by client...
	}



	private void sendFileList(PrintWriter dataoutput2) {
		// TODO later
		
	}

	private void sendFile(PrintWriter dataoutput2, String requestedItem) {
		// TODO later
		
	}

}
