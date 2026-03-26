package com.faceid.service;

import com.faceid.dto.ResponseDTO.FaceVerificationResultDTO;
import com.faceid.exception.BadRequestException;
import com.faceid.exception.ResourceNotFoundException;
import com.faceid.model.AuditEvent;
import com.faceid.model.FaceAngleImage;
import com.faceid.model.FaceDetectionData;
import com.faceid.model.User;
import com.faceid.repository.FaceAngleImageRepository;
import com.faceid.repository.UserRepository;
import com.faceid.util.LBPUtils;
import com.faceid.util.SecurityUtils;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
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
    private final FaceAngleImageRepository faceAngleImageRepository;
    private final AuditLogService auditLogService;

    @Value("${opencv.haarcascade.path}")
    private String haarcascadePath;

    @Value("${app.image.storage.path}")
    private String imageStoragePath;

    @Value("${app.image.max-size-bytes:1048576}")
    private long maxImageSizeBytes;

    /**
     * Threshold de similaridade LBP chi-quadrado para aceitar verificação.
     * exp(-chiSq) ≥ 0.70 equivale a chiSq ≤ ~0.36 — rostos com iluminação/ângulo similar.
     */
    private static final double FACE_SIMILARITY_THRESHOLD = 0.70;

    private CascadeClassifier faceDetector;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/jpg", "image/png");

    public FaceRecognitionService(UserRepository userRepository,
                                  FaceAngleImageRepository faceAngleImageRepository,
                                  AuditLogService auditLogService) {
        this.userRepository           = userRepository;
        this.faceAngleImageRepository = faceAngleImageRepository;
        this.auditLogService          = auditLogService;
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
    }

    public Mat detectFaces(byte[] imageData) {
        return detectFaceWithData(imageData).croppedFace();
    }

    /**
     * Detecta o primeiro rosto na imagem e retorna os dados completos para análise de vivacidade.
     * Retorna: imagem cinza completa, bounding box original e rosto recortado 100×100.
     */
    public FaceDetectionData detectFaceWithData(byte[] imageData) {
        validateImageBytes(imageData);
        Mat image = imdecode(new Mat(imageData), IMREAD_COLOR);
        if (image == null || image.empty()) {
            throw new BadRequestException("Nao foi possivel decodificar a imagem enviada.");
        }

        Mat grayImage = new Mat();
        cvtColor(image, grayImage, COLOR_BGR2GRAY);

        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(grayImage, faces);

        if (faces.size() == 0) {
            throw new BadRequestException("Nenhuma face detectada na imagem.");
        }

        Rect firstFace = faces.get(0);
        Mat detectedFace = new Mat(grayImage, firstFace);
        resize(detectedFace, detectedFace, new Size(100, 100));
        return new FaceDetectionData(grayImage, firstFace, detectedFace);
    }

    public void validateImageUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uma imagem JPG ou PNG e obrigatoria.");
        }
        if (file.getSize() > maxImageSizeBytes) {
            throw new BadRequestException("A imagem excede o limite de 1 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Formato de imagem invalido. Use JPG ou PNG.");
        }
    }

    public byte[] extractFaceFeatures(Mat faceImage) {
        return imageToByteArray(faceImage);
    }

    /**
     * Compara dois rostos armazenados como PNG bytes usando histograma LBP + distância chi-quadrado.
     *
     * Substitui o Arrays.equals() original — que sempre falhava para imagens reais
     * (qualquer variação de iluminação/compressão gerava bytes completamente diferentes).
     *
     * Algoritmo:
     *   1. Decodifica ambos os PNGs de volta para Mat grayscale.
     *   2. Computa histograma LBP normalizado (256 bins) para cada rosto.
     *   3. Calcula similaridade chi-quadrado: sim = exp(-chiSq), onde 1.0 = idêntico.
     *   4. Retorna true se sim >= FACE_SIMILARITY_THRESHOLD (padrão: 0.70).
     */
    public boolean compareFaceFeatures(byte[] registeredFeatures, byte[] verificationFeatures) {
        return computeFaceSimilarity(registeredFeatures, verificationFeatures) >= FACE_SIMILARITY_THRESHOLD;
    }

    /**
     * Retorna o score de similaridade LBP entre 0.0 e 1.0.
     * Exposto para uso no FaceVerificationResultDTO (score visível na resposta da API).
     */
    public double computeFaceSimilarity(byte[] storedBytes, byte[] verifyBytes) {
        Mat stored = imdecode(new Mat(storedBytes), IMREAD_GRAYSCALE);
        Mat verify = imdecode(new Mat(verifyBytes), IMREAD_GRAYSCALE);

        if (stored == null || stored.empty() || verify == null || verify.empty()) {
            throw new BadRequestException("Nao foi possivel decodificar as imagens para comparacao.");
        }

        double[] hist1 = LBPUtils.computeHistogram(stored);
        double[] hist2 = LBPUtils.computeHistogram(verify);
        return LBPUtils.chiSquareSimilarity(hist1, hist2);
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

    public String saveImagePermanently(byte[] imageData) throws IOException {
        Path uploadPath = Paths.get(imageStoragePath).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String fileName = UUID.randomUUID().toString() + ".png";
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, imageData);
        return filePath.toString();
    }

    public byte[] decodeBase64ToBytes(String base64String) {
        try {
            byte[] imageData = Base64.getDecoder().decode(base64String);
            validateImageBytes(imageData);
            return imageData;
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Imagem Base64 invalida.");
        }
    }

    /**
     * Registra a face de um usuário.
     *
     * @Transactional(rollbackFor) garante que, se o save() falhar (ex: constraint violation),
     * toda a operação seja revertida — incluindo quaisquer leituras já executadas na mesma transação.
     * Nota: saveImagePermanently() é I/O de filesystem (não transacional), portanto deve ser
     * chamada APÓS a validação do rosto para minimizar arquivos órfãos em caso de falha.
     */
    @Transactional(rollbackFor = Exception.class)
    public User registerFace(Long userId, byte[] imageData) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com ID: " + userId));
        SecurityUtils.requireSelf(user);

        // 1. Validação e extração (CPU — sem I/O externo)
        Mat faceImage = detectFaces(imageData);
        byte[] faceFeatures = extractFaceFeatures(faceImage);

        // 2. Salva no banco primeiro; só então persiste o arquivo em disco
        user.setFaceFeatures(faceFeatures);
        String imagePath = saveImagePermanently(imageData);
        user.setProfileImagePath(imagePath);
        User saved = userRepository.save(user);
        auditLogService.log(AuditEvent.FACE_REGISTERED, userId, "Face registrada (modo simples)");
        return saved;
    }

    /**
     * Verifica a face de um usuário contra as características registradas.
     *
     * <p>Estratégia multi-ângulo: se o usuário completou o cadastro multi-ângulo,
     * compara o rosto enviado contra TODAS as poses registradas em {@code face_angle_images}
     * e aceita se qualquer ângulo superar o limiar (melhor de N comparações).
     * Caso contrário, faz fallback para a imagem simples em {@code user.faceFeatures}.
     *
     * <p>{@code readOnly=true} instrui o Hibernate a não fazer flush antes da query —
     * melhora performance em operações somente-leitura.
     */
    @Transactional(readOnly = true)
    public FaceVerificationResultDTO verifyFace(Long userId, byte[] imageData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com ID: " + userId));
        SecurityUtils.requireSelf(user);

        Mat verificationFace = detectFaces(imageData);
        byte[] verificationBytes = extractFaceFeatures(verificationFace);

        List<FaceAngleImage> registeredAngles = faceAngleImageRepository.findByUserId(userId);

        double similarity;
        if (!registeredAngles.isEmpty()) {
            // Melhor similaridade dentre todos os ângulos registrados
            similarity = registeredAngles.stream()
                    .mapToDouble(img -> computeFaceSimilarity(img.getFaceFeatures(), verificationBytes))
                    .max()
                    .orElse(0.0);
        } else if (user.getFaceFeatures() != null) {
            // Fallback: cadastro simples de ângulo único
            similarity = computeFaceSimilarity(user.getFaceFeatures(), verificationBytes);
        } else {
            throw new BadRequestException("Caracteristicas faciais nao registradas para o usuario: " + userId);
        }

        boolean verified = similarity >= FACE_SIMILARITY_THRESHOLD;
        String message = verified ? "Identidade confirmada." : "Identidade nao confirmada.";
        double roundedSimilarity = Math.round(similarity * 1000.0) / 1000.0;

        AuditEvent auditEvent = verified ? AuditEvent.FACE_VERIFICATION_SUCCESS : AuditEvent.FACE_VERIFICATION_FAILURE;
        auditLogService.log(auditEvent, userId, "similarity=" + roundedSimilarity);

        return new FaceVerificationResultDTO(verified, roundedSimilarity, message);
    }

    /** Valida um array de bytes de imagem. Exposto para uso no LivenessService. */
    public void validateImageBytes(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            throw new BadRequestException("Uma imagem valida e obrigatoria.");
        }
        if (imageData.length > maxImageSizeBytes) {
            throw new BadRequestException("A imagem excede o limite de 1 MB.");
        }
    }
}
