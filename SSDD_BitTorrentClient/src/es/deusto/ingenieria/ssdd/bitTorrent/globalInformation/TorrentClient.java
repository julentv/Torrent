package es.deusto.ingenieria.ssdd.bitTorrent.globalInformation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import es.deusto.ingenieria.ssdd.bitTorrent.download.PeerConnection;
import es.deusto.ingenieria.ssdd.bitTorrent.file.FileManagement;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoStringHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MetainfoFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MultipleFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.SingleFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.upload.Listener;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

/**
 * 
 * Main thread of the Torrent.
 *
 */
public class TorrentClient {
	private String peerId;
	private String ip;
	private int port;
	private PeerStateList peerStateList;
	private int interval;
	private MetainfoFile<?> metainf;
	private FragmentsInformation fragmentsInformation;
	private int subfragmentLength;
	private int uploaded;

	public TorrentClient(int port) {
		this.peerId = ToolKit.generatePeerId();
		this.port = port;
		this.peerStateList = new PeerStateList(new PeerState(this.ip,
				this.port, 0));
		this.interval = 0;
		this.metainf = null;
		this.subfragmentLength = 512;
		this.uploaded = 0;
	}
	
	/*************** Getters and setters *****************/
	public String getPeerId() {
		return peerId;
	}

	public MetainfoFile<?> getMetainf() {
		return metainf;
	}

	public FragmentsInformation getFragmentsInformation() {
		return fragmentsInformation;
	}

	public void setFragmentsInformation(
			FragmentsInformation fragmentsInformation) {
		this.fragmentsInformation = fragmentsInformation;
	}

	public int getPort() {
		return port;
	}

	public int getUploaded() {
		return uploaded;
	}

	public synchronized void setUploaded(int uploaded) {
		this.uploaded = uploaded;
	}

	public PeerStateList getPeerStateList() {
		return peerStateList;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
	
	/****************** Other methods **********************/
	
	/**
	 * Calculates the number of pieces of the file
	 * @return the number of pieces
	 */
	public int getNumberOfPieces() {
		int pieces = this.metainf.getInfo().getLength()
				/ this.metainf.getInfo().getPieceLength();
		if (this.metainf.getInfo().getLength()
				% this.metainf.getInfo().getPieceLength() != 0) {
			pieces += 1;
		}
		return pieces;

	}

	/**
	 * Method that starts with the download of the torrent.
	 * Throws all the thread with the different peer that are going to be asked for the file.
	 * Throws the thread that opens the incoming connections to share the file.
	 * @param torrentName Name of the torrent file with the Meta information.
	 * @throws IOException
	 */
	public void downloadTorrent(String torrentName) throws IOException {

		this.metainf = this.obtainMetaInfo(torrentName);
		System.out.println(metainf.toString());
		// Throw the thread that listens for the incoming connections
		Listener listener = new Listener(this);
		listener.start();
		// obtain the current fragment from the file
		int length = this.metainf.getInfo().getLength();
		length += 4;
		FileManagement fileMan = new FileManagement(this.metainf.getInfo()
				.getName(), length);
		int downloaded = 0;
		if (fileMan.exists()) {
			int currentFragment = fileMan.getCurrentFragment();
			downloaded = currentFragment
					* this.metainf.getInfo().getPieceLength();
			if (currentFragment > -1) {
				this.fragmentsInformation = new FragmentsInformation(
						this.metainf.getInfo().getLength(), this.metainf
								.getInfo().getPieceLength(),
						this.subfragmentLength, currentFragment, this.metainf
								.getInfo().getByteSHA1(), this.metainf
								.getInfo().getName());
			} else {
				this.fragmentsInformation = new FragmentsInformation(
						this.metainf.getInfo().getLength(), this.metainf
								.getInfo().getPieceLength(),
						this.subfragmentLength, -1, this.metainf.getInfo()
								.getByteSHA1(), this.metainf.getInfo()
								.getName());
				downloaded = this.metainf.getInfo().getLength();
			}
		} else {
			this.fragmentsInformation = new FragmentsInformation(this.metainf
					.getInfo().getLength(), this.metainf.getInfo()
					.getPieceLength(), this.subfragmentLength, 0, this.metainf
					.getInfo().getByteSHA1(), this.metainf.getInfo().getName());
		}
		
		//first connection with the tracker
		String trackerResponse = httprRequest(metainf, this.port, 0,
				downloaded, this.metainf.getInfo().getLength()-downloaded);
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
				// connect to all the peers
				if(downloaded<this.metainf.getInfo().getLength()){
					for(int i=0,ii=this.peerStateList.size();i<ii;i++){
						PeerConnection peerConnection = new PeerConnection(this,
								this.peerStateList.get(i));
						peerConnection.start();
					}
					
					
				}else{
					System.out.println("Ya esta descargado.");
				}
				//send messages to the tracker periodically.
				trackerUpdate();
			} catch (Exception e) {
				System.out.println("Can't parse the tracker response");
				e.printStackTrace();
			}

		} else {
			System.out.println("Can't connect to any tracker");
		}

	}

	/**
	 * Extracts from the torrent file the Metainfo
	 * 
	 * @param torrentFileName
	 * 			  Name of the torrent file to load (will be searched into the torrent directory)
	 * @return The metainfo obtained from the file.
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
	 * Creates the connection to the tracker and return it's response.
	 * Tries to connect to all the tracker in the metainfo until the connection to anyone is done.
	 * 
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
				//System.out.println("Trying to connect to: " + announce);
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
				//System.out.println(response.toString());
				break;
			} catch (Exception e) {
				//System.out.println("Connection error to: " + announce);

			}
		}
		return result;

	}
	
	/**
	 * Sends the periodical updates to the tracker with the new information.
	 * It waits for the time indicated in interval between every connection.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void trackerUpdate() throws IOException, InterruptedException{
		while(true){
			Thread.sleep(this.interval);
			
			//calculate the downloaded amountof data
			int downloaded=0;
			if(this.fragmentsInformation.isCompleted()){
				downloaded= this.metainf.getInfo().getLength();
			}else{
				downloaded= this.fragmentsInformation.getCurrentFragment()* this.metainf.getInfo().getPieceLength();
			}
			
			String trackerResponse = httprRequest(metainf, this.port, this.uploaded,
					downloaded, this.metainf.getInfo().getLength()-downloaded);
			// Parse the response
			MetainfoStringHandler mih = new MetainfoStringHandler(
					trackerResponse);
			//update peer list
			PeerStateList peerStateListAux=mih.getPeerStateArray(this.getNumberOfPieces(), new PeerState(this.ip, this.port, this.getNumberOfPieces()));
			for(int i=0,ii=peerStateListAux.size();i<ii;i++){
				PeerState peerStateAux=peerStateListAux.get(i);
				if(this.peerStateList.add(peerStateAux)){
					//throw new thread for every new peer
					PeerConnection peerConnection = new PeerConnection(this, peerStateAux);
					peerConnection.start();
					System.out.println("______________________Nuevo hilo lanzado para el nuevo peer__________________________");
				}
			}
		}
		
	}
	

}
