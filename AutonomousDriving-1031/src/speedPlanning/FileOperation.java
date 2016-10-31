package speedPlanning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FileOperation {
	public static void createFile(String fileName) throws IOException {
		String filePath = "./Models/" + fileName;
		File pomdpModelFile = new File(filePath);
		if (!pomdpModelFile.exists()) {
			pomdpModelFile.createNewFile();
		} else {
			FileOperation.cleanUpFileContent(pomdpModelFile);
		}
	}

	public static boolean cleanUpFileContent(File file) {
		if (file.exists()) {
			try {
				BufferedWriter pomdpBufferWriter = new BufferedWriter(new FileWriter(file));
				pomdpBufferWriter.write("");
				pomdpBufferWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public static ArrayList<String> readFileContent(String fileName) {
		ArrayList<String> pomdpFileContent = new ArrayList<>();
		String filePath = "./Models/" + fileName;
		File pomdpModelFile = new File(filePath);
		if (pomdpModelFile.exists()) {
			try {
				BufferedReader pomdpBufferReader = new BufferedReader(new FileReader(pomdpModelFile));
				String tempContent = pomdpBufferReader.readLine();
				while (tempContent != null) {
					pomdpFileContent.add(tempContent);
					tempContent = pomdpBufferReader.readLine();
				}
				pomdpBufferReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return pomdpFileContent;
	}

	public static boolean appendFileContent(String fileName, String content) {
		String filePath = "./Models/" + fileName;
		File pomdpModelFile = new File(filePath);
		if (pomdpModelFile.exists()) {
			try {
				BufferedWriter pomdpBufferWriter = new BufferedWriter(new FileWriter(pomdpModelFile, true));
				pomdpBufferWriter.write(content);
				pomdpBufferWriter.close();
				return true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}
}
