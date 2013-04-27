package pyttewebb;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class TCPServer
{
    private int Port;
    private String httpPath;
    
    private DataOutputStream writer;
    private BufferedReader reader;
    private InputStream input;
    private PrintStream pout;
    private Socket clientSocket = null;
    private ServerSocket serversocket = null;
    
    /**
     * This class will handle socket-connections directed mostly for http/tcp traffic
     * @param Which port will be used during runtime
     * @param Path to where all website-files are located (Suggestion: C:\http) 
     */
    public TCPServer(int port, String httppath) { Port = port;  httpPath = httppath; }
    
    /**
     * This method will start listening for traffic.
     * @throws If something went wrong during startup just print it out
     */
    public void StartServer()
    {
        try
        {
        serversocket = new ServerSocket(Port);
        int ReqNr = 1;  //To keep track of how many requests that has been received
        
        System.out.println("Server is now running on port: " + Port);
        System.out.println("Running from path: " + httpPath);
        
        while(true)
        {
            //http://cs.au.dk/~amoeller/WWW/javaweb/server.html
            try
            {
                clientSocket = serversocket.accept();
                System.out.println("Recieved a Request!");  //Do something here later?
                System.out.println("Request number: " + ReqNr);
                ReqNr++;
                
                writer = new DataOutputStream(clientSocket.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                input = clientSocket.getInputStream();
                pout = new PrintStream(writer);
                
                String request = reader.readLine();    //Get requestcommand to string
                log(clientSocket, request);     //Prints a logmessage
                /*
                final String request = reader.readLine();    //Get requestcommand to string
                log(clientSocket, request);     //Prints a logmessage
                
                new Thread(new Runnable() { public void run()
                    { try { HandleRequest(request); }
                        catch(Exception e) { e.printStackTrace(); } }
                }).start();*/
                
                HandleRequest(request);
            } catch(IOException e) { errlog(e.getMessage()); }   //If something went wrong it will prints out a message
            
            try { if(clientSocket != null) clientSocket.close(); }  //If connection still is up, try to shut it down
            catch (IOException e) { errlog(e.getMessage()); }
        }
        }
        catch(IOException ex) { errlog(ex.getMessage()); }
    }
    
    /**
     * This function let you handle a request, meaning it will be validated meanwhile it also will be handled.<br />
     * @param Request received from the socketclient
     */
    private void HandleRequest(String Request)
    {
        if(!Request.startsWith("GET") || Request.length() < 14 || !(Request.endsWith("HTTP/1.0") || Request.endsWith("HTTP/1.1")))
        {
            //When 400 -> do the error message here
            SendError("400 BAD REQUEST");
            errlog("400 BAD REQUEST");
        }
        else
        {
            //Removes unnecessary stuff like GET and HTTP and only leaves string with the request it self
            String req = Request.substring(4, Request.length()-9).trim();   //Might not work yet, should remove GET command etc...
            if(req.indexOf("..") != -1 || req.indexOf("/.ht") != -1 || req.endsWith("~"))   //Makes sure client not accessing files outside dedicated http-folder
            {
                //Do some antihack stuff here later -> like 403 message and check
                SendError("403 FORBIDDEN");
                errlog("403 FORBIDDEN");
            }
            else
            {
                String path = httpPath + "/" + req; //Builds up the request
                File file = new File(path); //Gets the true filepath
                
                if(file.isDirectory() && !path.endsWith("/"))   //redirects if its a directory (without final '/')
                {
                    //Prints a 301 message to the client
                    pout.print("HTTP/1.0 301 MOVED PERMANELY\r\n" + 
                            "Location: http://" + clientSocket.getLocalAddress().getHostAddress() + ":" +
                            clientSocket.getLocalPort() + "/" + req + "/\r\n\r\n");
                    errlog("301 MOVED PERMANELY");
                }
                else
                {
                    //if only directory, just add index.html at the end of request and return it
                    if(file.isDirectory()) { path = path + "index.html"; file = new File(path); }
                    
                    try
                    {
                        //Tries to open file and guessing what kind of contenttype to be attached
                        InputStream f = new FileInputStream(file);
                        int clength = GetContentLength(f);
                        
                        pout.print("HTTP/1.0 200 OK \r\n" +
                                "Date: " + new Date() + "\r\n" +
                                "Server: PytteWebb 1.0\r\n" +
                                "Content-Length: " + clength + "\r\n" +
                                "Content-Type: " + guessContentType(path) + "\r\n\r\n");
                        
                        sendFile(f, pout);  //Everything went fine, send filebytes to client
                        log(clientSocket, "200 OK");
                    }
                    catch(FileNotFoundException e)  //File not found
                    {
                        //Send back errorfile with message!
                        SendError("404 FILE NOT FOUND");
                        errlog("404 FILE NOT FOUND");
                    }
                }
            }
        }
    }
    
    private int GetContentLength(InputStream stream)
    {
        try { return stream.available(); }
        catch (Exception ex) { System.err.println("Couldn't fetch contentlength\nError: " + ex.getMessage()); return 0; }
    }
    
    /**
     * This method is used to send back errorsite including errorcode+message
     * @param Errorcode to be sent, ex: 404 FILE NOT FOUND 
     */
    private void SendError(String errorCode)
    {
        try
        {
            InputStream f = new FileInputStream(httpPath + "/error.html");
            int clength = GetContentLength(f);
            
            String header = "HTTP/1.0 " + errorCode + "\r\n" +
                            "Date: " + new Date() + "\r\n" +
                            "Server: PytteWebb 0.9\r\n" +
                            "Content-Length: " + clength + "\r\n" + 
                            "Content-Type: text/html\r\n\r\n";
            
            pout.print(header);
            sendFile(f, pout);
        }
        catch(IOException ex) { errlog("ERROR.HTML WAS NOT FOUND!"); }
    }
    
    /**
     * 
     * @param Filetype of the requested resource. Should be a part of path
     * @return Returns Content-type buildup
     */
    private static String guessContentType(String link)
    {
        if(link.endsWith(".html") || link.endsWith(".htm")) { return "text/html"; }
        else if(link.endsWith(".txt") || link.endsWith(".java")) { return "text/plain"; }
        else if(link.endsWith(".gif")) { return "image/gif"; }
        else if(link.endsWith(".class")) { return "application/octet-stream"; }
        else if(link.endsWith(".jpg") || link.endsWith(".jpeg")) { return "image/jpeg"; }
        else { return "text/plain"; }
    }
    
    /**
     * This method will convert any file into bytes to be transfered
     * @param The actual filestream with the file to be converted into bytes
     * @param Will be used for the actual transfer  
     */
    private static void sendFile(InputStream file, OutputStream out)
    {
        try
        {   //Separates file/files into 1000bytes chunks
            byte[] buffer = new byte[1000];
            while(file.available()>0) { out.write(buffer,0,file.read(buffer)); }    //Writes to buffer until finished
        } catch(IOException e) { System.err.println(e); }   //If exception just print it out in the output-console
    }
    
    /**
     * This method is used to print out messages in the console, both output-console or an actual console, depending on 
     * how you're running the server.
     * @param Should be the socket belonging to each client
     * @param What kind of message which will be printed
     */
    private static void log(Socket connection, String msg)
    {
        //Prints out a log message in the output-console
        //format: 20131022 [10.11.0.1:8080] HELO MESSAGE
        System.out.println(new Date() + " [" + connection.getInetAddress().getHostAddress() +
                        ":" + connection.getPort() + "] " + msg);
    }
    
    private static void errlog(String msg)
    {
        System.err.println(new Date() + " " + msg);
    }
}