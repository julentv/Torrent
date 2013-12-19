package es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.test;

import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.Handsake;

public class MessagesTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Handsake handsake= new Handsake("infoHashjjjjjjjjjjjj","peerId");
		System.out.println(handsake.toString());
		String protocol= handsake.toString().substring(1, 20);
		String has= handsake.toString().substring(28, 48);
		System.out.println(protocol);
		System.out.println(has);
		System.out.println(handsake.toString().substring(48));
	}

}
