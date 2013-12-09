package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MetainfoFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MultipleFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.SingleFileHandler;

public class Torrent {

	public static void main(String[] args) throws IOException {
		System.out.print("Insert the name of the torrent: ");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		String torrentFileName = reader.readLine();
		Torrent torrent= new Torrent();
		
		System.out.println(torrent.obtainMetaInfo(torrentFileName).toString());

	}
	public MetainfoFile<?> obtainMetaInfo(String torrentFileName){
		File torrentFile= new File("torrent/"+torrentFileName);
		MetainfoFileHandler<?> handler;
		try {
			handler = new SingleFileHandler();
			handler.parseTorrenFile(torrentFile.getPath());
		} catch (Exception ex) {
			handler = new MultipleFileHandler();
			handler.parseTorrenFile(torrentFile.getPath());
		}
		return handler.getMetainfo();
	}

}
