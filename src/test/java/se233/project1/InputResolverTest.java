package se233.project1;

import org.junit.jupiter.api.Test;
import se233.project1.controller.InputResolver;
import se233.project1.exception.InvalidInputException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InputResolverTest {
    @Test
    public void resolve_givenSingleImage_thenOneImage() throws Exception {
        List<File> files = new ArrayList<>();
        files.add(TestImages.image("logo-3colors.png"));
        assertEquals(1, InputResolver.resolve(files).getImages().size());
    }

    @Test
    public void resolve_givenPngAndJpg_thenTwoImages() throws Exception {
        List<File> files = Arrays.asList(TestImages.image("logo-3colors.png"), TestImages.image("orange-logo.jpg"));
        InputResolver.Resolution r = InputResolver.resolve(files);
        assertEquals(2, r.getImages().size());
        assertTrue(r.getSkipped().isEmpty());
    }

    @Test
    public void resolve_givenImageAndTextFile_thenTextFileSkipped() throws Exception {
        List<File> files = Arrays.asList(TestImages.image("logo-3colors.png"), TestImages.input("notes.txt"));
        InputResolver.Resolution r = InputResolver.resolve(files);
        assertEquals(1, r.getImages().size());
        assertEquals("notes.txt", r.getSkipped().get(0));
    }

    @Test
    public void resolve_givenOnlyTextFile_thenInvalidInputException() {
        List<File> files = Arrays.asList(TestImages.input("notes.txt"));
        assertThrows(InvalidInputException.class, () -> InputResolver.resolve(files));
    }

    @Test
    public void resolve_givenZip_thenImagesExtractedAndOthersSkipped() throws Exception {
        List<File> files = Arrays.asList(TestImages.input("images.zip"));
        InputResolver.Resolution r = InputResolver.resolve(files);
        assertEquals(2, r.getImages().size(), "folder/one.png and two.JPG");
        for (File image : r.getImages()) {
            assertTrue(image.exists());
        }
        assertTrue(r.getSkipped().contains("readme.txt"));
        assertTrue(r.getSkipped().contains("../evil.png"), "zip-slip entry must be refused");
    }

    @Test
    public void resolve_givenZipAndImage_thenInvalidInputException() {
        List<File> files = Arrays.asList(TestImages.input("images.zip"), TestImages.image("logo-3colors.png"));
        Exception e = assertThrows(InvalidInputException.class, () -> InputResolver.resolve(files));
        assertEquals("Please drop either image files or a single ZIP archive, not both together.", e.getMessage());
    }

    @Test
    public void resolve_givenTwoZips_thenInvalidInputException() {
        List<File> files = Arrays.asList(TestImages.input("images.zip"), TestImages.input("no-images.zip"));
        assertThrows(InvalidInputException.class, () -> InputResolver.resolve(files));
    }

    @Test
    public void resolve_givenBrokenOrImageLessZip_thenInvalidInputException() {
        assertThrows(InvalidInputException.class, () -> InputResolver.resolve(Arrays.asList(TestImages.input("broken.zip"))));
        assertThrows(InvalidInputException.class, () -> InputResolver.resolve(Arrays.asList(TestImages.input("no-images.zip"))));
    }
}
