/**
 * HotButton Server by Dima Max and Tim Daniel Evert
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import java.lang.InterruptedException;
import java.lang.StringBuilder;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

/**
 * admin interface
 */
class AdminInterface implements HttpHandler
{
	public void handle(HttpExchange exchange) throws IOException
	{
		// send headers
		Headers responseHeaders = exchange.getResponseHeaders();
		responseHeaders.set("Content-Type", "text/html");
		exchange.sendResponseHeaders(200, 0);
		
		// send body
		OutputStream responseBody = exchange.getResponseBody();
		OutputStreamWriter out = new OutputStreamWriter(responseBody);
		
		// write header
		writeHeader(out);
		
		// get status of all clients
		Set<SelectionKey> allKeys = hotbutton.selector.keys();
		for(SelectionKey key : allKeys)
		{
			if(key == hotbutton.serverkey)
				continue;
				
			String username = (String)key.attachment();
			SocketChannel client = (SocketChannel)key.channel();
			out.write("<li>" + username + " [" + client.socket().getRemoteSocketAddress() + "]</li>");
		}
		
		// write footer and end
		writeFooter(out);
		out.close();
		responseBody.close();
	}
	
	/**
	 * writeHeader
	 */
	private void writeHeader(OutputStreamWriter out) throws IOException
	{
		out.write("<html>" +
		"<head>" +
		"<style>" +
		"hr {color:sienna;}" +
		"p {margin-left:20px;}" +
		"h2 { color: rgb(81, 137, 81); font-size: 140%; font-family: arial,helvetica,sans-serif; }" +
		"body { background-color: rgb(228, 238, 228); }" +
		"#users { position: absolute; right: 20px; top: 20px; bottom: 20px; padding: 10px; background-color: rgb(216, 228, 216); list-style-image:url('http://www.ticketcreator.com/android16.png'); }" +
		"#users li { margin-left: 20px; }" +
		"</style>" +
		"</head>" +
		"<body>" +
		"<a href=unlock>Unlock</a>" +
		"<ul id=users>" +
		"<h2>Users</h2>");
	}
	
	/**
	 * writeFooter
	 */
	public void writeFooter(OutputStreamWriter out) throws IOException
	{
		out.write("</ul>" +
		"</body>" +
		"</html>");
	}
}

/**
 * lock / unlock button
 */
class LockUnlockInerface implements HttpHandler
{
	public void handle(HttpExchange exchange) throws IOException
	{
		// send headers
		Headers responseHeaders = exchange.getResponseHeaders();
		responseHeaders.set("Content-Type", "text/html");
		exchange.sendResponseHeaders(200, 0);
		
		
		ByteBuffer buffer = ByteBuffer.allocate(512);
		buffer.putChar('u');
		buffer.putChar('\r');
		buffer.putChar('\n');
		
		Set<SelectionKey> allKeys = hotbutton.selector.keys();
		for(SelectionKey key : allKeys)
		{
			if(key == hotbutton.serverkey)
				continue;
			
			SocketChannel client = (SocketChannel)key.channel();
			buffer.flip();
			client.write(buffer);
		}
		
		OutputStream responseBody = exchange.getResponseBody();
		
		
		responseBody.close();
	}
}

/**
 * hotbutton main class
 */
public class hotbutton
{
	/**
	 * all clients
	 */
	static SelectionKey serverkey;
	static Selector selector;

	/**
	 * main server function 
	 */
	public static void main(String[] args) throws IOException
	{
		System.out.println("Starting HotButton Server....");
		
		// create admin interface
		InetSocketAddress address = new InetSocketAddress(8080);
		HttpServer httpServer = HttpServer.create(address, 0);
		AdminInterface admin = new AdminInterface();
		LockUnlockInerface lockunlock = new LockUnlockInerface();
		httpServer.createContext("/", admin);
		httpServer.createContext("/lock", lockunlock);
		httpServer.createContext("/unlock", lockunlock);
		httpServer.start();
		
		// listen server port 31337
		InetSocketAddress addrServer = new InetSocketAddress(31337);
		ServerSocketChannel server = ServerSocketChannel.open();
		selector = Selector.open();
		server.configureBlocking(false);
		server.socket().bind(addrServer);
		serverkey = server.register(selector, SelectionKey.OP_ACCEPT);
		serverkey.attach("Server");
		
		// initialisation stuff
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
				iterator.remove();
				if (!key.isValid())
					continue;
				
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
					SocketChannel client = (SocketChannel)key.channel();
					int bytesread = client.read(buffer);
					if (bytesread == -1) {
						key.cancel();
						client.close();
					}
					
					// generate request
					ArrayList<String> command = new ArrayList<String>();
					StringBuilder s = new StringBuilder();
					buffer.flip();
					while(buffer.hasRemaining()) {
						char c = (char)buffer.get();
						if(c == '-') {
							command.add(s.toString());
							s = new StringBuilder();
						} else {
							s.append(c);
						}
					}
					command.add(s.toString());
					buffer.clear();
					
					// process Command
					List<String> response = processCommand(key, command);
					
					// send response
					for(String message : response)
					{
						buffer.put(message.getBytes());
						buffer.putChar('-');
					}
					buffer.putChar('\r');
					buffer.putChar('\n');
					buffer.flip();
					client.write(buffer);
					buffer.clear();
				}
			}
		}
		
	}
	
	/**
	 * processCommand
	 */
	 static List<String> processCommand(SelectionKey key, List<String> request)
	 {
		List<String> response = new ArrayList<String>();

		// login
		if(request.get(0).equals("l")) {
			String username = request.get(1);
			key.attach(username);
			response.add("l");
			response.add("okay");
			System.out.println("okay login! >> " + username);
		}
		
		// return response
		System.out.println(" okay ! --------------------" + request.size());
		return response;
	 }
}
