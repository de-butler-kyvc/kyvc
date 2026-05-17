package com.kyvc.backendadmin.domain.document.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminDocumentStorageTest {

    @TempDir
    private Path tempDir;

    @Test
    void loadFindsBackendModuleStorageWhenAdminStorageHasNoFile() throws IOException {
        Path workspacePath = tempDir.resolve("kyvc");
        Path adminStoragePath = workspacePath.resolve("backend_admin").resolve("storage");
        Path backendDocumentPath = workspacePath.resolve("backend")
                .resolve("storage")
                .resolve("kyc-documents")
                .resolve("10")
                .resolve("BUSINESS_REGISTRATION")
                .resolve("document.pdf");
        Files.createDirectories(backendDocumentPath.getParent());
        Files.writeString(backendDocumentPath, "content", StandardCharsets.UTF_8);

        AdminDocumentStorage storage = new AdminDocumentStorage(
                new AdminDocumentStorageProperties(adminStoragePath.toString())
        );

        AdminDocumentStorage.StoredContent content = storage.load("10/BUSINESS_REGISTRATION/document.pdf");

        assertEquals(backendDocumentPath.toAbsolutePath().normalize(), content.path());
        assertEquals(7L, content.contentLength());
    }
}
