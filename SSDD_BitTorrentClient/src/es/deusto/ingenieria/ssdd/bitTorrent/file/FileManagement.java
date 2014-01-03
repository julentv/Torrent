package es.deusto.ingenieria.ssdd.bitTorrent.file;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

import es.deusto.ingenieria.ssdd.bitTorrent.main.FragmentsInformation;

public class FileManagement {

	private String fileName;
	private RandomAccessFile randomAccessFile;
	
	public FileManagement(String fileName){
	
		this.fileName= fileName;
	}
	
	public void storeInFile(int position, byte[] bytesToStore){
		
		try {
			// Se abre el fichero para lectura y escritura.
			randomAccessFile = new RandomAccessFile (this.fileName, "rw");
			randomAccessFile.seek(position);
			// Escribimos los bytes de la pieza a partir de esa posicion
			randomAccessFile.write(bytesToStore);
			//cerramos el fichero random
			randomAccessFile.close();
		} catch (IOException e) {
			e.printStackTrace();
			
		} 
	}
	
	public byte[] readFromFile(int position, int lenght){
		byte[] read= new byte[lenght];
		try {
			// Se abre el fichero para lectura y escritura.
			randomAccessFile = new RandomAccessFile (this.fileName, "rw");
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
