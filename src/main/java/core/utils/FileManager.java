package core.utils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

public class FileManager {
	private static final Logger log = LoggerFactory
			.getLogger(FileManager.class);

	public String saveFile(MultipartFile image, String uploadPath) {
		try {
			String originName = image.getOriginalFilename();
			String lastName = originName.substring(originName.lastIndexOf("."),
					originName.length());
			UUID uuid = UUID.randomUUID();
			String fileName = uuid.toString().replace("-", "") + lastName;
			File file = new File(uploadPath + File.separator + fileName);
			image.transferTo(file);
			return fileName;
		}catch (IllegalStateException | IOException e){
			throw new FileAccessException(e);
		}
	}

	public String saveAndRemoveFile(MultipartFile image, String uploadPath,
			String beforeFile) {
		try {
			String originName = image.getOriginalFilename();
			String lastName = originName.substring(originName.lastIndexOf("."),
					originName.length());
			UUID uuid = UUID.randomUUID();
			String fileName = uuid.toString().replace("-", "")+ lastName;
			File file = new File(uploadPath + File.separator + fileName);
			image.transferTo(file);

			File before = new File(beforeFile);

			if (before.isFile()) {
				before.delete();
			}
			image.transferTo(file);
			return fileName;
		} catch (IllegalStateException | IOException e) {
			log.debug("파일 저장 오류 = {}",e.toString());
			throw new FileAccessException(e);
		}
	}
	
	public boolean isValidatedFile(MultipartFile image) {
		String fileName = image.getOriginalFilename();

		if (image.isEmpty()) {
			return false;
		}
		if (!(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName
				.endsWith(".png"))) {
			return false;
		}

		return true;

	}



}
