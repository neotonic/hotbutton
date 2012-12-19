/**
 * HotButton Server by Dima Max and Tim Daniel Evert
 */
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class hotbutton
{
	/**
	 * main server function 
	 */
	public static void main(String[] args) throws IOException
	{
		System.out.println("Starting HotButton Server....");
		
		// listen server port 31337
		Selector selector = Selector.open();
		InetSocketAddress addrServer = new InetSocketAddress(31337);
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		server.socket().bind(addrServer);
		SelectionKey serverkey = server.register(selector, SelectionKey.OP_ACCEPT);
		
		// Buffer
		ByteBuffer buffer = ByteBuffer.allocate(512);
		
		// main loop
		System.out.println("Server started...");
		for(;;)
		{
			// do select
			int readyChannels = selector.select();
			if(readyChannels < 0)
				break;
				
			// iterate through selection
			Set<SelectionKey> readyKeys = selector.selectedKeys();
			Iterator iterator = readyKeys.iterator();
			while (iterator.hasNext())
			{
				// fetch a key
				SelectionKey key = (SelectionKey)iterator.next();
				
				// new client connected
				if (key == serverkey && key.isAcceptable())
				{
					SocketChannel client = server.accept();
					client.configureBlocking(false);
					client.register(selector, SelectionKey.OP_READ);
					System.out.println("Accepted connection from " + client);
				}
				
				// message from client
				else if(key.isReadable())
				{
					// read data to buffer
					SocketChannel client = (SocketChannel) key.channel();
					int bytesread = client.read(buffer);
					if (bytesread == -1) {
						key.cancel();
						client.close();
					}
					
					// dump message from buffer
					//System.out.println("--------------------");
					System.out.print(client.socket().getInetAddress() + " >> : ");
					buffer.flip();
					while(buffer.hasRemaining()) {
						System.out.print((char) buffer.get()); // read 1 byte at a time
					}
					buffer.clear();
					//System.out.println("--------------------");
				}
				
				
				// remove it
				iterator.remove();
			}
		}

	}
}

/*
 * 
void test() throws IOException {
	int port = 11111;
	java.net.ServerSocket serverSocket = new java.net.ServerSocket(port);
	java.net.Socket client = warteAufAnmeldung(serverSocket);
	String nachricht = leseNachricht(client);
	System.out.println(nachricht);
	schreibeNachricht(client, nachricht);
}
java.net.Socket warteAufAnmeldung(java.net.ServerSocket serverSocket) throws IOException {
	java.net.Socket socket = serverSocket.accept(); // blockiert, bis sich ein Client angemeldet hat
	return socket;
}
String leseNachricht(java.net.Socket socket) throws IOException {
	BufferedReader bufferedReader = 
		new BufferedReader(
			new InputStreamReader(
				socket.getInputStream()));
	char[] buffer = new char[200];
	int anzahlZeichen = bufferedReader.read(buffer, 0, 200); // blockiert bis Nachricht empfangen
	String nachricht = new String(buffer, 0, anzahlZeichen);
	return nachricht;
}
void schreibeNachricht(java.net.Socket socket, String nachricht) throws IOException {
	PrintWriter printWriter =
		new PrintWriter(
			new OutputStreamWriter(
				socket.getOutputStream()));
	printWriter.print(nachricht);
	printWriter.flush();
}
}
*/
