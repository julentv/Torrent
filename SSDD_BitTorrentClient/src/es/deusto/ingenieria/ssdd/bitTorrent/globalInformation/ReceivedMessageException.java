package es.deusto.ingenieria.ssdd.bitTorrent.globalInformation;

/**
 * Exception to indicate tha the received message is not correct.
 * 
 *
 */
public class ReceivedMessageException extends Exception {

	private static final long serialVersionUID = 1L;
	public ReceivedMessageException(){
		super("The received message is not correct");
	}
	public ReceivedMessageException(String message){
		super(message);
	}

}
