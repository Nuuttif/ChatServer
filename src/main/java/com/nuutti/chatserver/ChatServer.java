package com.nuutti.chatserver;

import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.*;

import java.net.InetSocketAddress;
import java.security.KeyStore;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import java.io.*;

public class ChatServer 
{

    public static void log(String message) {
        System.out.println(message);
    }

    private static SSLContext chatServerSSLContext(String certificate, String password) throws Exception
    {
        char[] passphrase = password.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(certificate), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
         
        SSLContext ssl = SSLContext.getInstance("TLSv1.2");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ssl;
    }

    public static void main( String[] args ) throws Exception
    {
        if (args.length == 3) {
        try {
            ChatDatabase database = ChatDatabase.getInstance();
            database.open(args[0]);

            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);
            SSLContext sslContext = chatServerSSLContext(args[1], args[2]);	

            server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
                public void configure (HttpsParameters params) {
                    InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                    }
                });

            ChatAuthenticator ca = new ChatAuthenticator();
            HttpContext context = server.createContext("/chat", new ChatHandler());
            context.setAuthenticator(ca);
            server.createContext("/registration", new RegistrationHandler(ca));

            Executor threadpool = Executors.newCachedThreadPool();
            server.setExecutor(threadpool);
            server.start();
            System.out.println( "Server is running.");
            Console console = System.console();
            boolean running = true;
            while (running) {
                if (console.readLine().equals("/quit")) {
                    running = false;
                    server.stop(3);
                    database.close();
                }
            }
        } catch (Exception e) {
            System.out.println("Error in launching the server: " + e.getMessage());
        }
        } else {
            System.out.println("Need 3 arguments.");
        }

    }
}
