package com.example.passport_ocr_service.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class ImagePreprocessService {

    public BufferedImage preprocess(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);

        // Convert to grayscale
        BufferedImage gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = gray.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        // Apply simple binarization (threshold)
        BufferedImage binarized = new BufferedImage(gray.getWidth(), gray.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < gray.getHeight(); y++) {
            for (int x = 0; x < gray.getWidth(); x++) {
                int rgb = gray.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int gVal = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                int avg = (r + gVal + b) / 3;
                if (avg > 128) {
                    binarized.setRGB(x, y, 0xFFFFFF);
                } else {
                    binarized.setRGB(x, y, 0x000000);
                }
            }
        }

        return binarized;
    }
}
