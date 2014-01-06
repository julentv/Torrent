package es.deusto.ingenieria.ssdd.bitTorrent.file;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

import es.deusto.ingenieria.ssdd.bitTorrent.main.FragmentsInformation;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

public class FileManagement {

	private String fileName;
	private RandomAccessFile randomAccessFile;
	private int fileLength;
	public FileManagement(String fileName, int fileLength){
		this.fileLength= fileLength;
		this.fileName= "data/"+fileName;
	}
	
	public void storeInFile(int position, byte[] bytesToStore){
		
		try {
			// Se abre el fichero para lectura y escritura.
			randomAccessFile = new RandomAccessFile (this.fileName, "rw");
			randomAccessFile.setLength(fileLength);
			randomAccessFile.seek(position);
			// Escribimos los bytes de la pieza a partir de esa posicion
			randomAccessFile.write(bytesToStore);
			//cerramos el fichero random
			randomAccessFile.close();
		} catch (IOException e) {
			e.printStackTrace();
			
		} 
	}
	
	public void storeInFileWithLast(int position, byte[] bytesToStore,int last){
		storeInFile(position,bytesToStore);
		storeInFile(this.fileLength-4,ToolKit.intToBigEndianBytes(last, new byte[4], 0));
	}
	
	public byte[] readFromFile(int position, int lenght){
		byte[] read= new byte[lenght];
		try {
			// Se abre el fichero para lectura y escritura.
			randomAccessFile = new RandomAccessFile (this.fileName, "rw");
			randomAccessFile.setLength(fileLength);
			randomAccessFile.seek(position);
			randomAccessFile.read(read, position, lenght);
			//cerramos el fichero random
			randomAccessFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return read;
	}

}
