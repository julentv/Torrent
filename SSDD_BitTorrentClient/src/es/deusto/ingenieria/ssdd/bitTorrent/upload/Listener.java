package es.deusto.ingenieria.ssdd.bitTorrent.upload;

import java.io.IOException;
import java.net.ServerSocket;

import es.deusto.ingenieria.ssdd.bitTorrent.globalInformation.TorrentClient;

/**
 * Thread that opens the tcp socket to get the peers requests
 * and share the file with them.
 * The method run opens the serverSocket and for each new incoming request opens a new thread
 * that is going to manage the connection with each peer.
 *
 */
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
			this.torrentClient.setIp(tcpServerSocket.getInetAddress().getHostAddress());
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
