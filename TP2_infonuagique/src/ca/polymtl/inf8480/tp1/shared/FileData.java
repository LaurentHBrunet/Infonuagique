package ca.polymtl.inf8480.tp1.shared;

import java.io.*;

public class FileData implements Serializable {
	public String fileName;
	public String fileContent;
	
	public FileData(File file) {
		fileName = file.getName();
		fileContent = readFileContent(file);
	}
	
	private String readFileContent(File file) {

		String line;
		String fileContent = "";
		try {
			
			BufferedReader fileReader = new BufferedReader(new FileReader(file));
		
			line = fileReader.readLine();
			while (line != null) {
				fileContent += line + "\n";
				line = fileReader.readLine();
			}
			
			fileReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fileContent;
	}

	public File convertToFile(String path) {
		File file = new File(path + fileName);

		FileWriter fw;
		try {
			fw = new FileWriter(file, true);
			fw.write(fileContent);
			fw.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		return file;
	}
}
