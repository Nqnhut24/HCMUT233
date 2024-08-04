package com.fams.api.services;

import com.fams.api.entity.TrainingMaterial;
import com.fams.api.entity.UnitChapter;
import com.fams.api.repository.TrainingMaterialRepository;
import com.fams.api.repository.UnitChapterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TrainingMaterialService {

    private final TrainingMaterialRepository trainingMaterialRepository;
    private final UnitChapterRepository unitChapterRepository;

    private final Path rootLocation = Paths.get("uploads");

    @Autowired
    public TrainingMaterialService(TrainingMaterialRepository trainingMaterialRepository, UnitChapterRepository unitChapterRepository) { // Modify this constructor
        try {
            this.trainingMaterialRepository=trainingMaterialRepository;
            this.unitChapterRepository = unitChapterRepository;
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage!", e);
        }// Initialize the unitChapterRepository
    }

    // Get all training materials
    public List<TrainingMaterial> getAllTrainingMaterials() {
        return trainingMaterialRepository.findAll();
    }

    // Add a training material 
    public TrainingMaterial addTrainingMaterial(MultipartFile file, String chapterId) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }

            System.out.println("====================================DA NHAN FILE TEN:================ "+ file.getOriginalFilename());

            // Create a unique filename
            String filename = System.currentTimeMillis() + "-" + file.getOriginalFilename();
            Path destinationFile = rootLocation.resolve(
                            Paths.get(filename))
                    .normalize().toAbsolutePath();

            // Ensure the file is within the target directory
            if (!destinationFile.getParent().equals(rootLocation.toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside current directory.");
            }

            // Copy the file to the destination directory
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Save file metadata to database
            TrainingMaterial trainingMaterial = new TrainingMaterial();
            trainingMaterial.setFileName(filename);
            trainingMaterial.setName(file.getOriginalFilename());
            trainingMaterial.setUnitChapterId(chapterId);
            trainingMaterial.setUrl("/api/files/download/" + filename); // Set download URL

            trainingMaterialRepository.save(trainingMaterial);

            return trainingMaterial;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    public TrainingMaterial saveNonFileMaterial(TrainingMaterial trainingMaterial) {
        if (trainingMaterial.getUnitChapterId() != null && !trainingMaterial.getUnitChapterId().isEmpty()) {
            if (!unitChapterRepository.existsById(trainingMaterial.getUnitChapterId())) {
                throw new RuntimeException("UnitChapter not found with id: " + trainingMaterial.getUnitChapterId());
            }
        }
        return trainingMaterialRepository.save(trainingMaterial);
    }

    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = rootLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {

                return resource;
            } else {
                throw new RuntimeException("File not found: " + filename);
            }
        } catch (Exception e) {
            throw new RuntimeException("File not found: " + filename, e);
        }
    }

    // Update an existing training material
    public TrainingMaterial updateTrainingMaterial(String trainingMaterialId, TrainingMaterial updatedTrainingMaterial) {
        return trainingMaterialRepository.findById(trainingMaterialId)
                .map(existingTrainingMaterial -> {
                    if (updatedTrainingMaterial.getUnitChapterId() != null && !unitChapterRepository.existsById(updatedTrainingMaterial.getUnitChapterId())) {
                        throw new RuntimeException("UnitChapter not found with id: " + updatedTrainingMaterial.getUnitChapterId());
                    }
                    if (updatedTrainingMaterial.getName() != null) existingTrainingMaterial.setName(updatedTrainingMaterial.getName());
                    if (updatedTrainingMaterial.getFileName() != null) existingTrainingMaterial.setFileName(updatedTrainingMaterial.getFileName());
                    if (updatedTrainingMaterial.getUrl() != null) existingTrainingMaterial.setUrl(updatedTrainingMaterial.getUrl());
                    if (updatedTrainingMaterial.getCreatedBy() != null) existingTrainingMaterial.setCreatedBy(updatedTrainingMaterial.getCreatedBy());
                    if (updatedTrainingMaterial.getCreatedDate() != null) existingTrainingMaterial.setCreatedDate(updatedTrainingMaterial.getCreatedDate());
                    if (updatedTrainingMaterial.getModifiedBy() != null) existingTrainingMaterial.setModifiedBy(updatedTrainingMaterial.getModifiedBy());
                    if (updatedTrainingMaterial.getModifiedDate() != null) existingTrainingMaterial.setModifiedDate(updatedTrainingMaterial.getModifiedDate());
                    existingTrainingMaterial.setDeleted(updatedTrainingMaterial.isDeleted());
                    existingTrainingMaterial.setFile(updatedTrainingMaterial.isFile());
                    if (updatedTrainingMaterial.getUnitChapterId() != null) existingTrainingMaterial.setUnitChapterId(updatedTrainingMaterial.getUnitChapterId());
                    return trainingMaterialRepository.save(existingTrainingMaterial);
                }).orElseThrow(() -> new RuntimeException("TrainingMaterial not found with id " + trainingMaterialId));
    }

    // Delete a training material by ID
    public void deleteTrainingMaterial(String trainingMaterialId) {
        trainingMaterialRepository.deleteById(trainingMaterialId);
    }

    // Get a specific training material by ID
    public TrainingMaterial getTrainingMaterialById(String trainingMaterialId) {
        Optional<TrainingMaterial> trainingMaterial = trainingMaterialRepository.findById(trainingMaterialId);
        if(trainingMaterial.isPresent()) {
            return trainingMaterial.get();
        } else {
            throw new RuntimeException("TrainingMaterial not found with id " + trainingMaterialId);
        }
    }

    // Get a list of Training material by id
    public List<TrainingMaterial> getTrainingMaterialByChapter(String unitChapterId) {
        Optional<UnitChapter> chapterFound = unitChapterRepository.findById(unitChapterId);
        if (chapterFound.isPresent()) {
            List<TrainingMaterial> trainingMaterials = trainingMaterialRepository.findByUnitChapterId(chapterFound.get().getId());

            return trainingMaterials;
        }
        else {
            throw new RuntimeException("Chapter Not Found woth id " + unitChapterId);
        }
    }
}