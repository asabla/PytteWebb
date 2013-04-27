package pyttewebb;

import java.io.File;
import java.io.IOException;

public class PytteWebb
{
    public static void main(String[] args)
    {
        String httpPath = "";
        int portnr = 0;
        
        //If no arguments was given it will set default values of: port:8080 and httpPath: C:\http
        if(args.length == 2) 
        { 
            try { portnr = Integer.parseInt(args[0]); httpPath = args[1]; }
            catch(Exception ex) {System.out.println("Usage: Pyttewebb <port> <httpPath>"); System.exit(-1); }
        }
        else { portnr = 8080; httpPath = "C:\\http"; }
        /*else 
        { 
            portnr = 8080; 
            try { httpPath = new File(".").getCanonicalPath(); }    //Tries to fetch application running path
            catch (IOException e) { httpPath = ""; System.err.println("Something went wrong fetching path to application!\n" + e.getMessage()); }
        }*/
        
        TCPServer server = new TCPServer(portnr, httpPath);
        
        try { server.StartServer(); }   //Starts the server
        catch (Exception ex) { System.err.println("The server was not able to start\nException: " + ex.toString()); }
    }
}
