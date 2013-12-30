package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

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

public class PeerConnection extends Thread{
	// this is the peer we are going to connect to
	private PeerState peerState;
	// this is ourself
	private TorrentClient torrent;

	public PeerConnection(TorrentClient torrentClient, PeerState peer) {
		this.torrent = torrentClient;
		this.peerState = peer;
	}

	private void connectToPeer() {
		/**
		 * si devuelve handshake !=null--parsear procesar requesst
		 */

		if (handshake() != null) {

		}
	}

	public byte[] handshake() {
		// create the handshake message
		Handsake handshakeMessage = new Handsake(new String(this.torrent
				.getMetainf().getInfo().getInfoHash()),
				this.torrent.getPeerId());
		// send handshake to the peer
		try (Socket tcpSocket = new Socket(peerState.getIp(),
				peerState.getPort());
				DataInputStream in = new DataInputStream(
						tcpSocket.getInputStream());
				DataOutputStream out = new DataOutputStream(
						tcpSocket.getOutputStream())) {
			out.write(handshakeMessage.getBytes());
			System.out.println(" - Sent data to '"
					+ tcpSocket.getInetAddress().getHostAddress() + ":"
					+ tcpSocket.getPort() + "' -> '" + handshakeMessage + "'");
			System.out.println("Waiting for the answer.");
			byte[] bytesHandshake = new byte[68];
			int num = in.read(bytesHandshake);
			System.out.println(" - Received data from the peer (" + num
					+ ") -> '" + new String(bytesHandshake) + "'");
			boolean isValidHandshake=Handsake.isValidHandsakeForBitTorrentProtocol(Handsake.parseStringToHandsake(new String(bytesHandshake)),new String(this.torrent.getMetainf().getInfo().getInfoHash()));			
			if(isValidHandshake){
				byte[] lastBytes = new byte[in.available()];
				in.read(lastBytes);
				System.out.println("Resto de bytes: '" + new String(lastBytes)
						+ "'");
				processMessages(PeerProtocolMessage.parseMessages(lastBytes));
				
			}else{
				throw new ReceivedMessageException("The Received Handshake is not correct.");
			}
			
		} catch (UnknownHostException e) {
			System.err.println("# TCPClient Socket error: " + e.getMessage());
			e.printStackTrace();
		} catch (EOFException e) {
			System.err.println("# TCPClient EOF error: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("# TCPClient IO error: " + e.getMessage());
			e.printStackTrace();
		} catch (ReceivedMessageException e) {
			e.printStackTrace();
			return null;
		}
		return null;

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
				int pieceNumber=ToolKit.bigEndianBytesToInt(messageArray.get(i).getPayload(), 0);
				peerState.getBitfield()[pieceNumber]=1;
				break;
			case 5: // bitfield
				// cambiar el bitfield del peer
				peerState.setBitfield(messageArray.get(i).getPayload());
				
				break;

			case 7: // piece

				break;
			
			default: 
				
				break;

			}
		}
	}
	

	private void request() {
		// comprobar pperstate contains currentfragment y si el current fragment
		// es -1 (ha acabao ya)
		// comprobar siguiente subfragmento a descargar
		// descargar subfragmento--mandar request
		// volver a paso 1
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		connectToPeer();
	}

}
