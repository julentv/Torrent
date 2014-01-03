package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import com.sun.media.sound.Toolkit;

import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.BitfieldMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.CancelMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.ChokeMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.Handsake;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.HaveMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.InterestedMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.NotInterestedMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PeerProtocolMessage;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PieceMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PortMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.RequestMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.UnChokeMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

public class PeerConnection extends Thread {
	// this is the peer we are going to connect to
	private PeerState peerState;
	// this is ourself
	private TorrentClient torrent;
	private Socket tcpSocket;
	private DataInputStream in;
	private DataOutputStream out;
	private int lastFragment=-1;

	public PeerConnection(TorrentClient torrentClient, PeerState peer) {
		this.torrent = torrentClient;
		this.peerState = peer;
		try {
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

	private byte[] sendMessage(byte[] message) {
		try {
			out.write(message);
			System.out.println(" - Sent data to '"
					+ tcpSocket.getInetAddress().getHostAddress() + ":"
					+ tcpSocket.getPort() + "' -> '" + new String(message)
					+ "'");
			System.out.println("Waiting for the answer.");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			byte[] answer = new byte[in.available()];
			in.read(answer);
			System.out.println(" - Received data from the peer () -> '"
					+ new String(answer) + "'");
			return answer;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private void sendInterestedMessage() {
		InterestedMsg interestedMessage = new InterestedMsg();
		// byte[]interested={0,0,0,1,2};
		sendMessage(interestedMessage.getBytes());
	}

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

	private void processMessages(ArrayList<PeerProtocolMessage> messageArray) {
		for (int i = 0, ii = messageArray.size(); i < ii; i++) {
			switch (messageArray.get(i).getType().getId()) {
			case 0: // choke
				// cerrar conexion
				// guardar stado
				break;
			case 1: // unchoke
				// guardo estado
				break;

			case 4: // have
				// cambiar el bitfield del peer
				int pieceNumber = ToolKit.bigEndianBytesToInt(
						messageArray.get(i).getPayload(), 0);
				peerState.getBitfield()[pieceNumber] = 1;
				break;
			case 5: // bitfield
				// cambiar el bitfield del peer
				byte[] payload = messageArray.get(i).getPayload();
				peerState.setBitfield(payload);

				break;

			case 7: // piece

				break;

			default:

				break;

			}
		}
	}

	private void request() {
		FragmentsInformation fragmentInformation = this.torrent
				.getFragmentsInformation();
		while (!fragmentInformation.downloadFinished()) {
			
			// comprobar siguiente subfragmento a descargar
			int[] pieceToAsk = fragmentInformation.pieceToAsk();
			if (pieceToAsk != null) {
				if(this.lastFragment<0){
					lastFragment=pieceToAsk[0];
				}else if(this.lastFragment<pieceToAsk[0]){
					//si se a descargado todo el fragmento notificar que lo tenemos
				}
				// comprobar que el peer contenga el fragmento
				if (this.peerState.hasFragment(pieceToAsk[0])) {
					// bloquear el fragmento
					if (fragmentInformation.blockFragment(pieceToAsk[1])) {
						try {
							// descargar subfragmento
							RequestMsg requestMessage = new RequestMsg(pieceToAsk[0], pieceToAsk[1], pieceToAsk[2]);
							byte[]response=this.sendMessage(requestMessage.getBytes());
							//parsear respuesta
							PieceMsg piece=(PieceMsg) PeerProtocolMessage.parseMessage(response);
							//añadir al array
							byte[]subfragment=ToolKit.subArray(piece.getPayload(), 8, piece.getPayload().length);
							boolean notify=fragmentInformation.addPieceToArray(subfragment, pieceToAsk[1]);
							
						} catch (Exception e) {
							// si algo va mal desbloquear el subfragmento
							fragmentInformation.unblockFragment(pieceToAsk[1]);
							System.out.println("Subfragmento '" + pieceToAsk[1]
									+ "' no descargado!");
							e.printStackTrace();
							//esperar
							try {
								Thread.sleep(100);
							} catch (InterruptedException ex) {
								// TODO Auto-generated catch block
								ex.printStackTrace();
							}
						}
					}

				} else {
					//esperar
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

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			connectToPeer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
