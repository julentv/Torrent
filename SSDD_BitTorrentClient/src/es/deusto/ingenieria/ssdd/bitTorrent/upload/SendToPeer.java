package es.deusto.ingenieria.ssdd.bitTorrent.upload;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import es.deusto.ingenieria.ssdd.bitTorrent.file.FileManagement;
import es.deusto.ingenieria.ssdd.bitTorrent.globalInformation.FragmentsInformation;
import es.deusto.ingenieria.ssdd.bitTorrent.globalInformation.PeerState;
import es.deusto.ingenieria.ssdd.bitTorrent.globalInformation.TorrentClient;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.BitfieldMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.ChokeMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.Handsake;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.HaveMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PeerProtocolMessage;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PieceMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.RequestMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.UnChokeMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

/**
 * This class is responsible of sending the requested fragments to the corresponding peer.
 *
 */
public class SendToPeer extends Thread {
	private TorrentClient torrentClient;
	private Socket socket;
	private InputStream inputStream;
	private OutputStream outputStream;
	private PeerState peerState;
	
	public SendToPeer(TorrentClient torrentClient, Socket socket){
		this.torrentClient=torrentClient;
		this.socket=socket;
	}
	
	/**
	 * This is the thread that will be used to send the fragments to the peer.
	 */
	@Override
	public void run() {
		try {
			this.inputStream = socket.getInputStream();
			this.outputStream = socket.getOutputStream();
			// Get the peer that is asking me for fragments.
			peerState = torrentClient.getPeerStateList().getByAddress(
					socket.getInetAddress().getHostAddress(), socket.getPort());
			// read handhake
			byte[] received = new byte[68];
			inputStream.read(received);
			//Parse the handshake received
			Handsake handshakeMessage = Handsake
					.parseStringToHandsake(new String(received));
			// validate hash
			String torrentHash = new String(torrentClient.getMetainf()
					.getInfo().getInfoHash());
			//if the received handshake is correct send my handshake
			if (Handsake.isValidHandsakeForBitTorrentProtocol(handshakeMessage,
					torrentHash)) {
				// send handshake
				Handsake handShakeToSend = new Handsake(torrentHash,
						this.torrentClient.getPeerId());
				this.outputStream.write(handShakeToSend.getBytes());
				// enviar bitfield
				try {
					this.sendBitfield();
					// leer
					PeerProtocolMessage message = this.readNormalMessage();
					// Check if I have received an interested message
					if (message.getType().equals(
							PeerProtocolMessage.Type.INTERESTED)) {
						if (this.peerState != null) {
							peerState = torrentClient.getPeerStateList()
									.getByAddress(
											socket.getInetAddress()
													.getHostAddress(),
											socket.getPort());
						}
						//Set the peer status
						if (this.peerState != null) {
							this.peerState.setPeer_interested(true);
						}

						// send unchoke to the peer
						this.outputStream.write(new UnChokeMsg().getBytes());
						
						//set the peer status
						if (this.peerState != null) {
							this.peerState.setAm_choking(false);
						}
						
						//Continue receiveing messages if the message type is Keep_alive, Request or Have.
						boolean continueReading = true;
						while (continueReading) {
							message = this.readNormalMessage();
							continueReading = processMessage(message);
						}

					} else {
						// if I have not received interested send unchoke
						this.outputStream.write(new ChokeMsg().getBytes());
					}

				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			// close connection
			this.inputStream.close();
			this.outputStream.close();
			socket.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Sending thread closed");
			e.printStackTrace();
			try {
				this.inputStream.close();
				this.outputStream.close();
				socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	private boolean processMessage(PeerProtocolMessage message) throws IOException{
		boolean accepted= true;
		switch (message.getType()){
		case KEEP_ALIVE:
			break;
		
		case REQUEST:
			//read the request fragment from the file
			FileManagement fm= new FileManagement(this.torrentClient.getMetainf().getInfo().getName(), this.torrentClient.getMetainf().getInfo().getLength());
			int position=0;
			//Calculate the start position of the fragment in the file
			position=(((RequestMsg) message).getIndex()*this.torrentClient.getFragmentsInformation().getFragmentLength())+((RequestMsg) message).getOffset();
			//Checks if the length of the file is smaller than the start position
			if((position+((RequestMsg) message).getOffset())<this.torrentClient.getMetainf().getInfo().getLength()){
				//Read the fragment
				byte[]bytes=fm.readFromFile(position, ((RequestMsg) message).getPieceLength());
				//send piece
				PieceMsg pieceMsg= new PieceMsg (((RequestMsg) message).getIndex(), ((RequestMsg) message).getOffset(), bytes);
				System.out.println("Sending fragment: "+((RequestMsg) message).getIndex());
				this.outputStream.write(pieceMsg.getBytes());
				this.torrentClient.setUploaded(this.torrentClient.getUploaded()+((RequestMsg) message).getPieceLength());
			}else{
				accepted=false;
			}
			break;
		case HAVE:
			if(this.peerState==null){
				peerState=torrentClient.getPeerStateList().getByAddress(socket.getInetAddress().getHostAddress(), socket.getPort());
			}
			if(this.peerState!=null){
				//update the bitfield of the peerState when a have message arrives
				int pieceNumber=ToolKit.bigEndianBytesToInt(((HaveMsg)message).getPayload(), 0);
				peerState.getBitfield()[pieceNumber]=1;
			}
			break;
		default:
			accepted=false;
			break;
		}
		
		return accepted;
	}
	
	/**
	 * This method send the bitfield to the peer
	 * @throws IOException
	 */
	private void sendBitfield() throws IOException{
		byte[]bitfield=new byte[this.torrentClient.getNumberOfPieces()];
		FragmentsInformation fi=this.torrentClient.getFragmentsInformation();
		//IF the fragment information is null or the current fragment is -1 it means that I have all the pieces
		if(fi==null||fi.getCurrentFragment()==-1){
			for(int i=0,ii=bitfield.length;i<ii;i++){
				bitfield[i]=1;
			}
		}else{
			//we only have some pieces
			int lastFragment=this.torrentClient.getFragmentsInformation().getCurrentFragment();
			int i=0;
			
			for(;i<lastFragment;i++){
				bitfield[i]=1;
			}
			for(int ii=bitfield.length;i<ii;i++){
				bitfield[i]=0;
			}
		}
		
		BitfieldMsg message= new BitfieldMsg(bitfield);
		this.outputStream.write(message.getBytes());
	}
	
	/**
	 * This method reads incoming messages from the peer.
	 * Reads the first 4 bytes that correspond to the length of the message.
	 * Then read the rest of the message and concatenate the length with the payload.
	 * Finally it parses the byte [] to a PeerProtocol Message.
	 * @return PeerProtocol message
	 * @throws IOException
	 */
	private PeerProtocolMessage readNormalMessage() throws IOException{
		byte[]length=new byte[4];
		this.inputStream.read(length);
		int lengthInt=ToolKit.bigEndianBytesToInt(length, 0);
		byte[]message= new byte[lengthInt];
		this.inputStream.read(message);
		byte[]result=new byte[length.length+message.length];
		System.arraycopy(length, 0, result, 0, length.length);
		System.arraycopy(message, 0, result, length.length, message.length);
		return PeerProtocolMessage.parseMessage(result);
	}

}
