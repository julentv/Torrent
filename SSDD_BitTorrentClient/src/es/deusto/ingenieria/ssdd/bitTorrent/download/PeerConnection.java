package es.deusto.ingenieria.ssdd.bitTorrent.download;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import es.deusto.ingenieria.ssdd.bitTorrent.globalInformation.FragmentsInformation;
import es.deusto.ingenieria.ssdd.bitTorrent.globalInformation.PeerState;
import es.deusto.ingenieria.ssdd.bitTorrent.globalInformation.ReceivedMessageException;
import es.deusto.ingenieria.ssdd.bitTorrent.globalInformation.TorrentClient;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.Handsake;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.HaveMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.InterestedMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PeerProtocolMessage;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PieceMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.RequestMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

/**
 * Class for the managing of every connection with the peers that are going to
 * send fragments of the file to download
 *
 */
public class PeerConnection extends Thread {
	// this is the peer we are going to connect to
	private PeerState peerState;
	// this is ourself
	private TorrentClient torrent;
	private Socket tcpSocket;
	private DataInputStream in;
	private DataOutputStream out;
	private int lastFragment=-1;
	private boolean stop=false;

	public PeerConnection(TorrentClient torrentClient, PeerState peer) {
		this.torrent = torrentClient;
		this.peerState = peer;
		try {
			//open the connection
			System.out.println("Connecting to -->"+peerState.getIp()+":"+peerState.getPort());
			this.tcpSocket = new Socket(peerState.getIp(), peerState.getPort());
			in = new DataInputStream(tcpSocket.getInputStream());
			out = new DataOutputStream(tcpSocket.getOutputStream());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		try {
			connectToPeer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initial method that manages the normal flow of the connection.
	 * @throws IOException
	 */
	private void connectToPeer() throws IOException {

		if (handshake()) {
			// send interested
			sendInterestedMessage();
			request();
		}
		in.close();
		out.close();
		tcpSocket.close();
	}
	
	/**
	 * Sends a message to the peer and return the obtained response.
	 * @param message to send.
	 * @return the response of the peer.
	 */
	private byte[] sendMessage(byte[] message) {
		try {
//			System.out.println(" - Sent data to '"
//					+ tcpSocket.getInetAddress().getHostAddress() + ":"
//					+ tcpSocket.getPort() + "' -> '" + new String(message)
//					+ "'");
			out.write(message);
			
			int cont=0;
			byte[] answer=new byte[0];
			
			//si no se recibe nada del socket esperar unos milisegundo y volver a intentar varias veces
			//if we receive nothing from the socket wait some milliseconds and try again.
			do{
				if(cont>=0){
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				answer = new byte[in.available()];
				in.read(answer);
				//System.out.println(" -1 Received data from the peer () -> '"
				//		+ new String(answer) + "'");
				cont++;
			}while(cont<5&&answer.length<1);
			
			return answer;
		} catch (IOException e) {
			e.printStackTrace();
			this.stop=true;
			return null;
		}
	}

	/**
	 * Sends the interested message to the peer.
	 */
	private void sendInterestedMessage() {
		this.peerState.setAm_interested(true);
		InterestedMsg interestedMessage = new InterestedMsg();
		// byte[]interested={0,0,0,1,2};
		byte[]answer=sendMessage(interestedMessage.getBytes());
		PeerProtocolMessage.parseMessages(answer);
	}

	/**
	 * Manages the handshake process with the peer.
	 * @return true if everything went right.
	 */
	public boolean handshake() {
		// create the handshake message
		Handsake handshakeMessage = new Handsake(new String(this.torrent
				.getMetainf().getInfo().getInfoHash()),
				this.torrent.getPeerId());
		// send handshake to the peer
		try {
			byte[] answer = sendMessage(handshakeMessage.getBytes());
			byte[] bytesHandshake = ToolKit.subArray(answer, 0, 68);
			boolean isValidHandshake = Handsake
					.isValidHandsakeForBitTorrentProtocol(Handsake
							.parseStringToHandsake(new String(bytesHandshake)),
							new String(this.torrent.getMetainf().getInfo()
									.getInfoHash()));

			if (isValidHandshake) {
				byte[] elResto = ToolKit.subArray(answer, 68, answer.length);
				processMessages(PeerProtocolMessage.parseMessages(elResto));

			} else {
				throw new ReceivedMessageException(
						"The Received Handshake is not correct.");
			}

		} catch (ReceivedMessageException e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}

	/**
	 * Processes the received messages acting in consequence.
	 * @param messageArray all the messages to procces
	 */
	private void processMessages(ArrayList<PeerProtocolMessage> messageArray) {
		for (int i = 0, ii = messageArray.size(); i < ii; i++) {
			switch (messageArray.get(i).getType().getId()){
			case 0: // choke
				// save the state and close the connection
				this.peerState.setPeer_choking(true);
				System.out.println("Choke recibido");
				break;
			case 1: // unchoke
				// save the state
				this.peerState.setPeer_choking(false);
				System.out.println("Unchoke recibido");
				break;

			case 4: // have
				// update the bitfield of the peer
				int pieceNumber = ToolKit.bigEndianBytesToInt(
						messageArray.get(i).getPayload(), 0);
				peerState.getBitfield()[pieceNumber] = 1;
				break;
			case 5: // bitfield
				// update the bitfield of the peer
				byte[] payload = messageArray.get(i).getPayload();
				peerState.setBitfield(payload);

				break;
			default:

				break;
			}
		}
	}

	/**
	 * Make the fragment requests to the peer.
	 */
	private void request() {
		FragmentsInformation fragmentInformation = this.torrent
				.getFragmentsInformation();
		while (!fragmentInformation.downloadFinished()&&!this.stop) {
			boolean firstLap=true;
			// obtain the next subfragment to download
			int[] pieceToAsk = fragmentInformation.pieceToAsk();
			if (pieceToAsk != null) {
				if(this.lastFragment<0){
					lastFragment=pieceToAsk[0];
				}else if(this.lastFragment<pieceToAsk[0]){
					//if a complete fragment has been downloaded send the have message to the peer.
					HaveMsg haveMessage=new HaveMsg(this.lastFragment);
					try {
						out.write(haveMessage.getBytes());
						System.out.println(" Have message sent to '"
								+ tcpSocket.getInetAddress().getHostAddress() + ":"
								+ tcpSocket.getPort() + "' -> Fragment: '" +this.lastFragment
								+ "'");
						
						
					} catch (IOException e) {
						e.printStackTrace();
					}
					this.lastFragment=pieceToAsk[0];
				}
				
				// Test if the peer has the fragment.
				if (firstLap||this.peerState.hasFragment(pieceToAsk[0])) {
					// block the subfragment to download
					if (fragmentInformation.blockFragment(pieceToAsk[1])) {
						firstLap=false;
						try {
							// download the fragment
							RequestMsg requestMessage = new RequestMsg(pieceToAsk[0], pieceToAsk[1], pieceToAsk[2]);
							byte[]response=this.sendMessage(requestMessage.getBytes());
							//parse the answer
							PieceMsg piece=(PieceMsg)PeerProtocolMessage.parseMessage(response);
							//add the piece to the array
							byte[]subfragment=ToolKit.subArray(piece.getPayload(), 8, piece.getPayload().length);
							fragmentInformation.addPieceToArray(subfragment, pieceToAsk[1]);
							
						}catch (Exception e) {
							// if something goes wrong unblock the fragment
							fragmentInformation.unblockFragment(pieceToAsk[1]);
							System.out.println("Subfragmento '" + pieceToAsk[1]
									+ "' no descargado!");
							e.printStackTrace();
							//wait
							try {
								Thread.sleep(100);
							} catch (InterruptedException ex) {
								ex.printStackTrace();
							}
						}
					}

				} else {
					//wait
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

}
