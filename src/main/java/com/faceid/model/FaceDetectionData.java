package com.faceid.model;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;

/**
 * Resultado da detecção facial com todos os dados necessários para análise de vivacidade.
 *
 * @param fullGrayImage Imagem completa em escala de cinza (para fluxo óptico e centroide).
 * @param faceRect      Bounding box do rosto na imagem original (para centroide e região ocular).
 * @param croppedFace   Rosto recortado e redimensionado para 100x100 (para LBP, Laplaciano e Sobel).
 */
public record FaceDetectionData(Mat fullGrayImage, Rect faceRect, Mat croppedFace) {}
