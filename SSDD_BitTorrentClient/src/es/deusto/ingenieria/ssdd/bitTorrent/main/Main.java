package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

	public static void main(String[] args) throws IOException {
		System.out.print("Insert the name of the torrent: ");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		// String torrentFileName = reader.readLine();
		String torrentFileName = "SAN IGNACIO.docx.torrent";
		TorrentClient torrent = new TorrentClient();
		torrent.dowloadTorrent(torrentFileName);

	}

}