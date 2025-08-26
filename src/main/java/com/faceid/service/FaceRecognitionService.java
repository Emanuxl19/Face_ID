package com.faceid.service;

import com.faceid.model.User;
import com.faceid.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import org.bytedeco.opencv.opencv_core.Size;

@Service
public class FaceRecognitionService {

    private final UserRepository userRepository;

    @Value("${opencv.haarcascade.path}")
    private String haarcascadePath;

    @Value("${app.image.storage.path}")
    private String imageStoragePath;

    private CascadeClassifier faceDetector;
    private FaceRecognizer faceRecognizer;

    public FaceRecognitionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        String actualPath;
        if (haarcascadePath.startsWith("classpath:")) {
            try {
                actualPath = new File(getClass().getClassLoader().getResource(haarcascadePath.substring("classpath:".length())).getFile()).getAbsolutePath();
            } catch (NullPointerException e) {
                throw new RuntimeException("Falha ao encontrar o classificador Haar Cascade no classpath: " + haarcascadePath);
            }
        } else {
            actualPath = haarcascadePath;
        }

        faceDetector = new CascadeClassifier(actualPath);
        if (faceDetector.empty()) {
            throw new RuntimeException("Falha ao carregar o classificador de faces Haar Cascade do caminho: " + actualPath);
        }

        faceRecognizer = LBPHFaceRecognizer.create(1, 8, 8, 8, 120);
    }

    public Mat detectFaces(byte[] imageData) throws IOException {
        Mat image = imdecode(new Mat(imageData), IMREAD_COLOR);
        Mat grayImage = new Mat();
        cvtColor(image, grayImage, COLOR_BGR2GRAY);

        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(grayImage, faces);

        if (faces.size() == 0) {
            throw new RuntimeException("Nenhuma face detectada na imagem.");
        }

        Rect firstFace = faces.get(0);
        Mat detectedFace = new Mat(grayImage, firstFace);
        resize(detectedFace, detectedFace, new Size(100, 100));
        return detectedFace;
    }

    public byte[] extractFaceFeatures(Mat faceImage) {
        return imageToByteArray(faceImage);
    }

    public boolean compareFaceFeatures(byte[] registeredFeatures, byte[] verificationFeatures) {
        return java.util.Arrays.equals(registeredFeatures, verificationFeatures);
    }

    private byte[] imageToByteArray(Mat image) {
        if (image == null || image.empty()) {
            return null;
        }

        BytePointer buf = new BytePointer();
        boolean result = imencode(".png", image, buf);
        if (!result) {
            buf.deallocate();
            throw new RuntimeException("Falha ao codificar a imagem.");
        }

        byte[] byteArray = new byte[(int) buf.limit()];
        buf.get(byteArray);
        buf.deallocate();
        return byteArray;
    }

    public String saveImagePermanently(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(imageStoragePath).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);
        return filePath.toString();
    }

    public String saveImagePermanently(byte[] imageData) throws IOException {
        Path uploadPath = Paths.get(imageStoragePath).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String fileName = UUID.randomUUID().toString() + ".png";
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, imageData);
        return filePath.toString();
    }

    public byte[] decodeBase64ToBytes(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    public User registerFace(Long userId, byte[] imageData) throws IOException {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Usuario nao encontrado com ID: " + userId);
        }
        User user = userOptional.get();

        Mat faceImage = detectFaces(imageData);
        byte[] faceFeatures = extractFaceFeatures(faceImage);
        String imagePath = saveImagePermanently(imageData);

        user.setFaceFeatures(faceFeatures);
        user.setProfileImagePath(imagePath);
        return userRepository.save(user);
    }

    public boolean verifyFace(Long userId, byte[] imageData) throws IOException {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Usuario nao encontrado com ID: " + userId);
        }
        User user = userOptional.get();

        if (user.getFaceFeatures() == null) {
            throw new RuntimeException("Caracteristicas faciais nao registradas para o usuario: " + userId);
        }

        Mat verificationFaceImage = detectFaces(imageData);
        byte[] verificationFaceFeatures = extractFaceFeatures(verificationFaceImage);

        return compareFaceFeatures(user.getFaceFeatures(), verificationFaceFeatures);
    }
}
