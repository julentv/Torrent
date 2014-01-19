package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This is the main class of the torrent client. From here the peer can start downloading
 * a file.
 *
 */
public class Main {

	public static void main(String[] args) throws IOException {
		System.out.println("Insert the port: ");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		int port = Integer.parseInt(reader.readLine());
		System.out.print("Insert the name of the torrent: ");
		reader = new BufferedReader(new InputStreamReader(
				System.in));
		String torrentFileName = reader.readLine();
		
		//String torrentFileName = "ALB   Golden Chains (feat. The Shoes).mp3.torrent";
		TorrentClient torrent = new TorrentClient();
		torrent.downloadTorrent(torrentFileName);
	}
}
