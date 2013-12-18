package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MetainfoFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MultipleFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.SingleFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

public class Torrent {
	private String peerId;
	private int port;

	public Torrent() {
		this.peerId = ToolKit.generatePeerId();
		this.port = 8888;
	}

	public void dowloadTorrent(String torrentName) throws IOException {
		Torrent torrent = new Torrent();
		MetainfoFile<?> metainf = torrent.obtainMetaInfo(torrentName);
		System.out.println(metainf.toString());
		String trackerResponse=httprRequest(metainf,333, 0,0,62113);
		if(trackerResponse!=null){
			MetainfoFile<?> trackerMetainf;
			try{
				MetainfoStringHandler mih= new MetainfoStringHandler(trackerResponse);
				
			}catch(Exception e){
				System.out.println("Can't parse the tracker response");
				e.printStackTrace();
			}
			
		}else{
			System.out.println("Can't connect to any tracker");
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.print("Insert the name of the torrent: ");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		// String torrentFileName = reader.readLine();
		String torrentFileName = "SAN IGNACIO.docx.torrent";
		Torrent torrent = new Torrent();
		torrent.dowloadTorrent(torrentFileName);

	}

	/**
	 * Extraer a partir de un torrent file la Metainfo
	 * 
	 * @param torrentFileName
	 *            Nombre del fichero torrent a cargar (Se buscara en el
	 *            directorio torrent)
	 * @return la metainfo extraida del fichero
	 */
	public MetainfoFile<?> obtainMetaInfo(String torrentFileName) {
		File torrentFile = new File("torrent/" + torrentFileName);
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

	/**
	 * Crea la conexion al tracker y devuelve la respuesta de este. Intenta conectarse a todos
	 * los trackers obtenidos del torrent hasta que la conexion se realice.
	 * @param metainf
	 * @param port
	 * @param uploaded
	 * @param downloaded
	 * @param left
	 * @return
	 * @throws IOException
	 */
	public String httprRequest(MetainfoFile<?> metainf, int port, int uploaded,int downloaded, int left)
			throws IOException {
		List<String> announceList = metainf.getHTTPAnnounceList();
		String result=null;
		
		//Intenta conectarse a todos los trackers, hasta que se conecta a uno
		for (String announce : announceList) {
			try {
				System.out.println("Trying to connect to: " + announce);
				String urlText = announce
						+ "?info_hash="
						+ metainf.getInfo().getUrlInfoHash()
						+ "&peer_id="
						+ this.peerId
						+ "&port="+port
						+"&uploaded="+uploaded
						+"&downloaded="+downloaded
						+"&left="+left
						+"&event=started";
				// urlText="http://google.com";
				String USER_AGENT = "Mozilla/5.0";
				URL obj = new URL(urlText);
				HttpURLConnection con = (HttpURLConnection) obj
						.openConnection();

				// optional default is GET
				con.setRequestMethod("GET");

				// add request header
				con.setRequestProperty("User-Agent", USER_AGENT);

				int responseCode = con.getResponseCode();

				BufferedReader in = new BufferedReader(new InputStreamReader(
						con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();

				// print result
				result=response.toString();
				System.out.println(response.toString());
				break;
			} catch (Exception e) {
				System.out.println("Connection error to: "+announce);

			}
		}
		return result;

	}
}
