package com.smalik.nicepdf;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.utils.PageRange;
import com.itextpdf.kernel.utils.PdfSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class PdfExtractService {

    private static Logger logger = LoggerFactory.getLogger(PdfExtractService.class);

    private EmbeddedResourceRepository repository;
    private File dir;

    public PdfExtractService(File dir) {
        this.dir = dir;
        repository = new EmbeddedResourceRepository(new File(dir, "resources"));
    }

    public EmbeddedResourceRepository getRepository() {
        return repository;
    }

    public int splitPdf(final File sourcePdf, final String subDirName, final String namePattern, final int numPagesPerPart) throws Exception {

        final AtomicInteger partNumber = new AtomicInteger(0);
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(sourcePdf));

        new File(dir, subDirName).mkdir();
        new PdfSplitter(pdfDoc) {

            @Override
            protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
                try {
                    String name = String.format(namePattern, partNumber.addAndGet(1));
                    return new PdfWriter(new File(new File(dir, subDirName), name));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException();
                }
            }
        }.splitByPageCount(numPagesPerPart, (pdfDocument, pageRange) -> pdfDocument.close());

        pdfDoc.close();
        return partNumber.get();
    }

    public void resourceInfoFromPdf(String subDir, String pdfFile) throws Exception {

        PdfDocument pdfDoc = new PdfDocument(new PdfReader(new File(new File(dir, subDir), pdfFile)));
        for (int i = 0; i < pdfDoc.getNumberOfPages(); i++) {

            PdfDictionary page = pdfDoc.getPage(i + 1).getPdfObject();
            PdfDictionary resources = page.getAsDictionary(PdfName.Resources);

            if (resources != null) {
                processImages(resources.getAsDictionary(PdfName.XObject));
                processFonts(resources.getAsDictionary(PdfName.Font));
            }
        }
        pdfDoc.close();
    }

    private void processImages(PdfDictionary images) {
        if (images != null) {
            for (PdfName name : images.keySet()) {
                PdfStream image = images.getAsStream(name);
                if (PdfName.Image.equals(image.getAsName(PdfName.Subtype))) {
                    logger.info("Found Image - Key:" + name + ", Value:" + image + ", ID:" + image.getAsString(PdfName.ID));
                }
            }
        }
    }

    private void processFonts(PdfDictionary fonts) {
        if (fonts != null) {
            for (PdfName name : fonts.keySet()) {
                PdfDictionary font = fonts.getAsDictionary(name);
                PdfDictionary fontDescriptor = font.getAsDictionary(PdfName.FontDescriptor);
                if (fontDescriptor != null) {
                    PdfStream fontFile = fontDescriptor.getAsStream(PdfName.FontFile2);
                    if (fontFile != null) {
                        logger.info("Found Font - Key:" + name + ", Length:" + fontFile.getLength() + ", ID:" + fontFile.getAsString(PdfName.ID));
                    }
                }
            }
        }
    }


    public void zeroOutResourcesInPdf(String sourceDir, String destDir, String name) throws IOException {

        new File(dir, destDir).mkdir();
        PdfDocument pdfDoc = new PdfDocument(
                new PdfReader(new File(new File(dir, sourceDir), name)),
                new PdfWriter(new File(new File(dir, destDir), name))
        );

        for (int i = 0; i < pdfDoc.getNumberOfPages(); i++) {

            PdfDictionary page = pdfDoc.getPage(i+1).getPdfObject();
            PdfDictionary resources = page.getAsDictionary(PdfName.Resources);
            if (resources != null) {
                zeroOutImages(resources.getAsDictionary(PdfName.XObject));
                zeroOutFonts(resources.getAsDictionary(PdfName.Font));
            }
        }

        pdfDoc.close();
    }

    private void zeroOutImages(PdfDictionary images) throws IOException {
        if (images != null) {
            for (PdfName name : images.keySet()) {
                PdfStream imageAsStream = images.getAsStream(name);
                if (PdfName.Image.equals(imageAsStream.getAsName(PdfName.Subtype)) && imageAsStream.getLength() > 128) {
                    zeroOutStream(imageAsStream, "image");
                }
            }
        }
    }

    private void zeroOutFonts(PdfDictionary fonts) throws IOException {
        if (fonts != null) {
            for (PdfName name : fonts.keySet()) {
                PdfDictionary font = fonts.getAsDictionary(name);
                PdfDictionary fontDescriptor = font.getAsDictionary(PdfName.FontDescriptor);
                if (fontDescriptor != null) {

                    PdfStream fontfile = fontDescriptor.getAsStream(PdfName.FontFile2);
                    if (fontfile != null) {
                        zeroOutStream(fontfile, "font");
                    }
                }
            }
        }
    }

    private void zeroOutStream(PdfStream stream, String type) throws IOException {

        boolean dctDecode = false;
        if (PdfName.DCTDecode.equals(stream.getAsName(PdfName.Filter))) {
            // dct encoded streams should be retrieved as is
            dctDecode = true;
        }

        byte[] bytes = stream.getBytes(!dctDecode);
        String md5 = repository.calculateMd5(bytes);

        EmbeddedResource resource = repository.findEmbeddedResourceByMd5(md5);
        if (resource == null) {
            resource = new EmbeddedResource(stream.getLength(), md5, type, dctDecode);
            repository.addEmbeddedResource(resource, bytes);
        }

        stream.setData(new byte[0]);
        stream.put(PdfName.ID, new PdfString(resource.getId()));
    }


    public void reconstructResourcesInPdf(String sourceDir, String destDir, String name) throws IOException {

        new File(dir, destDir).mkdir();
        PdfDocument pdfDoc = new PdfDocument(
                new PdfReader(new File(new File(dir, sourceDir), name)),
                new PdfWriter(new File(new File(dir, destDir), name))
        );

        for (int i = 0; i < pdfDoc.getNumberOfPages(); i++) {

            PdfDictionary page = pdfDoc.getPage(i + 1).getPdfObject();
            PdfDictionary resources = page.getAsDictionary(PdfName.Resources);

            reconstructWithImages(resources.getAsDictionary(PdfName.XObject));
            reconstructWithFonts(resources.getAsDictionary(PdfName.Font));
        }

        pdfDoc.close();
    }

    private void reconstructWithFonts(PdfDictionary fonts) throws IOException {
        if (fonts != null) {
            for (PdfName name : fonts.keySet()) {
                PdfDictionary font = fonts.getAsDictionary(name);
                PdfDictionary fontDescriptor = font.getAsDictionary(PdfName.FontDescriptor);
                if (fontDescriptor != null) {

                    PdfStream stream = fontDescriptor.getAsStream(PdfName.FontFile2);
                    if (stream != null) {
                        PdfString embeddedId = stream.getAsString(PdfName.ID);

                        if (embeddedId != null) {
                            EmbeddedResource resource = repository.findEmbeddedResourceById(embeddedId.getValue());
                            stream.setData(repository.readEmbeddedResourceData(resource));
                            stream.remove(PdfName.ID);
                        }
                    }
                }
            }
        }
    }

    private void reconstructWithImages(PdfDictionary images) throws IOException {
        if (images != null) {
            for (PdfName name : images.keySet()) {

                PdfStream image = images.getAsStream(name);
                PdfString embeddedImageId = image.getAsString(PdfName.ID);

                if (embeddedImageId != null) {
                    EmbeddedResource resource = repository.findEmbeddedResourceById(embeddedImageId.getValue());
                    image.setData(repository.readEmbeddedResourceData(resource));
                    image.remove(PdfName.ID);
                    if (resource.isDctDecode()) {
                        image.put(PdfName.Filter, PdfName.DCTDecode);
                    }
                }
            }
        }
    }
}
