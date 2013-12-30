package es.deusto.ingenieria.ssdd.bitTorrent.main;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MetainfoFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MultipleFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.SingleFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.Handsake;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

public class TorrentClient {
	private String peerId;
	private String ip;
	private int port;
	private PeerStateList peerStateList;
	private int interval;
	private MetainfoFile<?> metainf;

	/***********************Sincronizarlos**********************/
	//si es -1 ha acabado ya
	private int currentFragment;
	private byte[][]downloadingFragments;
	private int fragmentLength;
	/***************************************************************/

	
	public TorrentClient() {
		this.peerId = ToolKit.generatePeerId();
		this.port = 8888;
		this.ip = "127.0.0.1";
		this.peerStateList = new PeerStateList(new PeerState(this.ip,
				this.port, 0));
		this.interval = 0;
		this.metainf = null;
	}
	
	public int getCurrentFragment() {
		return currentFragment;
	}


	public int getNumberOfPieces() {
		int pieces= this.metainf.getInfo().getLength()
				/ this.metainf.getInfo().getPieceLength();
		if(this.metainf.getInfo().getLength()
				% this.metainf.getInfo().getPieceLength()!=0)
		{
			pieces+=1;
		}
		return pieces;
		
	}

	public void downloadTorrent(String torrentName) throws IOException {
		this.metainf = this.obtainMetaInfo(torrentName);
		System.out.println(metainf.toString());
		String trackerResponse = httprRequest(metainf, this.port, 0, 0, 62113);
		if (trackerResponse != null) {
			try {
				// Parse the response
				MetainfoStringHandler mih = new MetainfoStringHandler(
						trackerResponse);
				// Set the local values with the received information
				this.interval = mih.getInterval();
				this.peerStateList = mih.getPeerStateArray(this
						.getNumberOfPieces(), new PeerState(this.ip, this.port,
								this.getNumberOfPieces()));
				// connect to the peers
				//DE MOMENTO PARA UN SOLO PEER
				PeerConnection peerConnection = new PeerConnection(this,this.peerStateList.get(0));
				peerConnection.start();
			} catch (Exception e) {
				System.out.println("Can't parse the tracker response");
				e.printStackTrace();
			}

		} else {
			System.out.println("Can't connect to any tracker");
		}
	}

	/*BORRAR ESTE METODO*/
	public void handShakeToPeer(PeerState peerState, Handsake message) {
		try (Socket tcpSocket = new Socket(peerState.getIp(),
				peerState.getPort());
				DataInputStream in = new DataInputStream(
						tcpSocket.getInputStream());
				DataOutputStream out = new DataOutputStream(
						tcpSocket.getOutputStream())) {
			out.write(message.getBytes());
			System.out.println(" - Sent data to '"
					+ tcpSocket.getInetAddress().getHostAddress() + ":"
					+ tcpSocket.getPort() + "' -> '" + message + "'");
			System.out.println("Waiting for the answer.");
			byte[] bytesHandshake = new byte[68];
			int num = in.read(bytesHandshake);
			System.out.println(" - Received data from the peer (" + num
					+ ") -> '" + new String(bytesHandshake) + "'");
			byte[] lastBytes = new byte[in.available()];
			in.read(lastBytes);
			System.out.println("Resto de bytes: '" + new String(lastBytes)
					+ "'");
		} catch (UnknownHostException e) {
			System.err.println("# TCPClient Socket error: " + e.getMessage());
			e.printStackTrace();
		} catch (EOFException e) {
			System.err.println("# TCPClient EOF error: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("# TCPClient IO error: " + e.getMessage());
			e.printStackTrace();
		}
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
	 * Crea la conexion al tracker y devuelve la respuesta de este. Intenta
	 * conectarse a todos los trackers obtenidos del torrent hasta que la
	 * conexion se realice.
	 * 
	 * @param metainf
	 * @param port
	 * @param uploaded
	 * @param downloaded
	 * @param left
	 * @return
	 * @throws IOException
	 */
	public String httprRequest(MetainfoFile<?> metainf, int port, int uploaded,
			int downloaded, int left) throws IOException {
		List<String> announceList = metainf.getHTTPAnnounceList();
		String result = null;

		// Intenta conectarse a todos los trackers, hasta que se conecta a uno
		for (String announce : announceList) {
			try {
				System.out.println("Trying to connect to: " + announce);
				String urlText = announce + "?info_hash="
						+ metainf.getInfo().getUrlInfoHash() + "&peer_id="
						+ this.peerId + "&port=" + port + "&uploaded="
						+ uploaded + "&downloaded=" + downloaded + "&left="
						+ left + "&event=started";
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
				result = response.toString();
				System.out.println(response.toString());
				break;
			} catch (Exception e) {
				System.out.println("Connection error to: " + announce);

			}
		}
		return result;

	}

	public String getPeerId() {
		return peerId;
	}

	public MetainfoFile<?> getMetainf() {
		return metainf;
	}
	
}
