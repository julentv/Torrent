package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MetainfoFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MultipleFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.SingleFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

public class Torrent {
	private String peerId;
	private int port;
	
	public Torrent(){
		this.peerId=ToolKit.generatePeerId();
		this.port=8888;
	}
	public void dowloadTorrent(String torrentName) throws IOException{
		Torrent torrent= new Torrent();
		MetainfoFile<?> metainf=torrent.obtainMetaInfo(torrentName);
		System.out.println(metainf.toString());
		httprRequest("", metainf);
	}

	public static void main(String[] args) throws IOException {
		System.out.print("Insert the name of the torrent: ");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		//String torrentFileName = reader.readLine();
		String torrentFileName = "SAN IGNACIO.docx.torrent";
		Torrent torrent= new Torrent();
		torrent.dowloadTorrent(torrentFileName);


	}
	/**
	 * Extraer a partir de un torrent file la Metainfo
	 * @param torrentFileName Nombre del fichero torrent a cargar (Se buscara en el directorio torrent)
	 * @return la metainfo extraida del fichero
	 */
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
	public void httprRequest(String urlText, MetainfoFile<?> metainf) throws IOException{
		urlText="http://10.172.203.192:48643/announce?info_hash="+metainf.getInfo().getUrlInfoHash()+"&peer_id="+this.peerId+"&port=3333&uploaded=0&downloaded=0&left=16384&event=started";
		System.out.println(urlText);
		//urlText="http://google.com";
		String USER_AGENT = "Mozilla/5.0"; 
		URL obj = new URL(urlText);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
 
		// optional default is GET
		con.setRequestMethod("GET");
 
		//add request header
		con.setRequestProperty("User-Agent", USER_AGENT);
 
		int responseCode = con.getResponseCode();
 
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
 
		//print result
		System.out.println(response.toString());
        
	}

}
