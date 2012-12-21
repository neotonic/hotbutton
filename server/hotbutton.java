/**
 * HotButton Server by Dima Max and Tim Daniel Evert
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
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
import java.nio.channels.ClosedChannelException;

import java.net.URI;

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
		
		// preparation of html body
		OutputStream responseBody = exchange.getResponseBody();
		OutputStreamWriter out = new OutputStreamWriter(responseBody);
		
		// write header
		writeHeader(out);
		
		// uri
		URI requestedUri = exchange.getRequestURI();
		String path = requestedUri.getPath();
		
		// unlock or lock
		if(path.startsWith("/unlock") || path.startsWith("/lock")) {
			// fill buffer with data
			ByteBuffer byteBuffer = ByteBuffer.allocate(512);
			CharBuffer charBufer = byteBuffer.asCharBuffer();
			if(path.startsWith("/unlock"))
				charBufer.put("unlock\r\n");
			else
				charBufer.put("lock\r\n");
			
			// send buffer to all users
			Set<SelectionKey> allKeys = hotbutton.selector.keys();
			for(SelectionKey key : allKeys)
			{
				if(key == hotbutton.serverkey)
					continue;
			
				SocketChannel client = (SocketChannel)key.channel();
				charBufer.flip();
				client.write(byteBuffer);
			}
		}
		
		// get status of all clients
		Set<SelectionKey> allKeys = hotbutton.selector.keys();
		for(SelectionKey key : allKeys)
		{
			if(key == hotbutton.serverkey)
				continue;
				
			String username = (String)key.attachment();
			SocketChannel client = (SocketChannel)key.channel();
			out.write("<li>" + username + " [" + client.socket().getRemoteSocketAddress() + "] <a class=\"small awesome red\">kick</a></li>");
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
		"<link href=css/style.css rel=stylesheet type=text/css>" +
		"<title>Android Hotbutton Manager</title>" +
		"</head>" +
		"<body>" +
		"<div id=header><h1>Android HotButton Manager</h1></div>" +
		"<div id=body>" +
		"<a class=\"large orange awesome\" href=unlock>Unlock all Hotbuttons</a>" +
		"<a class=\"large green awesome\" href=lock>Lock all Hotbuttons</a>" +
		"<ul id=users>" +
		"<h2>Users</h2>");
	}
	
	/**
	 * writeFooter
	 */
	public void writeFooter(OutputStreamWriter out) throws IOException
	{
		out.write("</ul>" +
		"</div>" +
		"</body>" +
		"</html>");
	}
}

/**
 * images handler
 */
class FileController implements HttpHandler
{
	private String contentType;
	private String chroot;
	
	/**
	 * constructor
	 */
	FileController(String chroot, String contentType)
	{
		this.chroot = chroot;
		this.contentType = contentType;
	}
	
	/**
	 * file handler
	 */
	public void handle(HttpExchange exchange) throws IOException
	{
		// 
		URI requestedUri = exchange.getRequestURI();
		Headers responseHeaders = exchange.getResponseHeaders();
		
		try {
			// uri extraction
			String path = requestedUri.getPath();
			int index = path.lastIndexOf('/');
			String file = this.chroot + "/" + path.substring(index + 1);
			
			// open streams
			FileInputStream in = new FileInputStream(file);
			OutputStream out = exchange.getResponseBody();
			
			// send headers
			responseHeaders.set("Content-Type", this.contentType);
			exchange.sendResponseHeaders(200, 0);
			
			// copy contents of in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch(Exception ex) {
			System.out.println(ex.toString());
			
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(404, 0);
			
			OutputStream out = exchange.getResponseBody();
			out.write(ex.toString().getBytes());
			out.close();
		}
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
	static ServerSocketChannel server;
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
		httpServer.createContext("/", admin);
		httpServer.createContext("/images/", new FileController("./images", "image/png"));
		httpServer.createContext("/css/", new FileController("./css", "text/css"));
		httpServer.start();
		
		// listen server port 31337
		InetSocketAddress addrServer = new InetSocketAddress(31337);
		server = ServerSocketChannel.open();
		selector = Selector.open();
		server.configureBlocking(false);
		server.socket().bind(addrServer);
		serverkey = server.register(selector, SelectionKey.OP_ACCEPT);
		serverkey.attach("Server");
		
		// main loop
		System.out.println("Server started...");
		for(;;)
		{
			// do select
			int readyChannels = selector.select();
			if(readyChannels < 0)
				break;
				
			// call process events
			processEvents(readyChannels);
		}
		
	}
	
	/**
	 * processCommand
	 */
	static List<String> processCommand(SelectionKey key, List<String> request)
	{
		List<String> response = new ArrayList<String>();

		// login
		if(request.get(0).equals("login")) {
			String username = request.get(1);
			System.out.println((String)key.attachment() + " changed his username to " + username);
			key.attach(username);
			response.add("login");
			response.add("okay");
		}
		
		// return response
		return response;
	}
	 
	 /**
	  * processEvents
	  */
	static void processEvents(int eventCount) throws IOException
	{
		// initialisation stuff
		Set<SelectionKey> readyKeys = selector.selectedKeys();
		Iterator iterator = readyKeys.iterator();
		ByteBuffer byteBuffer = ByteBuffer.allocate(512);
		CharBuffer charBuffer = byteBuffer.asCharBuffer();
			
		// iterate through selection
		for(int i = 0; i < eventCount && iterator.hasNext();)
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
				try
				{
					// read data to buffer
					SocketChannel client = (SocketChannel)key.channel();
					int bytesread = client.read(byteBuffer);
					if (bytesread == -1) {
						key.cancel();
						client.close();
					}
					
					// need a string
					if(!byteBuffer.hasArray()) {
						System.out("Ignoring unknown sequeenze from client...\n");
						continue;
					}
					
					String s = new String(byteBuffer.array());
					System.out.println(s);
				
					// generate request
					ArrayList<String> command = new ArrayList<String>();
					StringBuilder s = new StringBuilder();
					byteBuffer.flip();
					while(byteBuffer.hasRemaining()) {
						char c = (char)byteBuffer.get();
						if(c == '-') {
							command.add(s.toString());
							s = new StringBuilder();
						} else {
							s.append(c);
						}
					}
					command.add(s.toString());
					byteBuffer.clear();
				
					// process Command
					List<String> lstResponse = processCommand(key, command);
					Iterator<String> itResponse = lstResponse.iterator();
					StringBuilder strResponse = new StringBuilder();
					while(itResponse.hasNext()) {
						strResponse.append(itResponse.next());
						if(itResponse.hasNext())
							strResponse.append("-");
					}
					strResponse.append("\r\n");
				
					// send response
					charBuffer.clear();
					//charBuffer.put("wie auch immer\r\n");
					charBuffer.put(strResponse.toString());
					charBuffer.flip();
					client.write(byteBuffer);
					byteBuffer.clear();
				
					// ClosedChannelException
				
				} catch(ClosedChannelException ex) {
					System.out.println(ex.toString());
				}
			}
		}
	}
}
