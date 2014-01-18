package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.io.IOException;
import java.net.ServerSocket;

public class Listener extends Thread {
	private TorrentClient torrentClient;
	
	public Listener(TorrentClient torrentClient){
		this.torrentClient=torrentClient;
	}

	@Override
	public void run() {
		try (ServerSocket tcpServerSocket = new ServerSocket(this.torrentClient.getPort());) {
			System.out.println(" - Waiting for peer connections '" + 
                    tcpServerSocket.getInetAddress().getHostAddress() + ":" + 
                    tcpServerSocket.getLocalPort() + "' ...");
			while(true){
				SendToPeer sendToPeer=new SendToPeer(torrentClient,tcpServerSocket.accept());
				sendToPeer.start();
				System.out.println("Connection received");
			}
		} catch (IOException e) {
			System.out.println("Listenining socket closed!!!");
			e.printStackTrace();
		}
	}
}
