package com.smalik.nicepdf;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.utils.PageRange;
import com.itextpdf.kernel.utils.PdfSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PdfExtractService {

    private static Logger logger = LoggerFactory.getLogger(PdfExtractService.class);
    private EmbeddedImageRepository repository = new EmbeddedImageRepository(new File("/tmp"));

    public int splitPdf(final String destFolder, String sourcePdf) throws Exception {

        final AtomicInteger partNumber = new AtomicInteger(0);
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(sourcePdf));

        List<PdfDocument> splitDocuments = new PdfSplitter(pdfDoc) {

            @Override
            protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
                try {
                    return new PdfWriter(destFolder + "/page-" + partNumber.addAndGet(1) + ".pdf");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException();
                }
            }
        }.splitByPageCount(1);

        for (PdfDocument doc : splitDocuments) {
            doc.close();
        }

        pdfDoc.close();
        return partNumber.get();
    }

    public void imageInfoFromPdf(String pdfFileNoExtension) throws Exception {

        PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFileNoExtension + ".pdf"));
        for (int i = 0; i < pdfDoc.getNumberOfPages(); i++) {

            PdfDictionary page = pdfDoc.getPage(i + 1).getPdfObject();
            PdfDictionary resources = page.getAsDictionary(PdfName.Resources);
            PdfDictionary xObjects = resources.getAsDictionary(PdfName.XObject);

            for (PdfName pdfName : xObjects.keySet()) {
                PdfStream x = xObjects.getAsStream(pdfName);
                logger.info("Page:" + i + ", Key:" + pdfName + ", Length:" + x.getLength() + ", Type:" + x.getType() + ", ID:" + x.getAsString(PdfName.ID));
            }
        }
    }

    public void zeroOutImagesInPdf(String pdfFileNoExtension) throws Exception {

        PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFileNoExtension + ".pdf"), new PdfWriter(pdfFileNoExtension + ".extracted.pdf"));
        for (int i = 0; i < pdfDoc.getNumberOfPages(); i++) {

            PdfDictionary page = pdfDoc.getPage(i+1).getPdfObject();
            PdfDictionary resources = page.getAsDictionary(PdfName.Resources);
            PdfDictionary xObjects = resources.getAsDictionary(PdfName.XObject);

            for (PdfName pdfName : xObjects.keySet()) {
                PdfStream x = xObjects.getAsStream(pdfName);
                byte[] bytes = x.getBytes(false);
                String md5 = EmbeddedImage.calculateMd5(bytes);

                EmbeddedImage embeddedImage = repository.findEmbeddedImageByMd5(md5);
                if (embeddedImage == null) {
                    embeddedImage = new EmbeddedImage(x.getLength(), md5);
                    repository.addEmbeddedImage(embeddedImage, bytes);
                }

                x.setData(new byte[0]);
                x.put(PdfName.ID, new PdfString(embeddedImage.getId()));
            }
        }

        pdfDoc.close();
    }

    public void reconstructImageInPdf(String pdfFileNoExtension) throws Exception {

        PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFileNoExtension + ".pdf"), new PdfWriter(pdfFileNoExtension + ".reconstructed.pdf"));
        for (int i = 0; i < pdfDoc.getNumberOfPages(); i++) {

            PdfDictionary page = pdfDoc.getPage(i + 1).getPdfObject();
            PdfDictionary resources = page.getAsDictionary(PdfName.Resources);
            PdfDictionary xObjects = resources.getAsDictionary(PdfName.XObject);

            for (PdfName pdfName : xObjects.keySet()) {

                PdfStream x = xObjects.getAsStream(pdfName);
                PdfString embeddedImageId = x.getAsString(PdfName.ID);

                if (embeddedImageId != null) {
                    EmbeddedImage embeddedImage = repository.findEmbeddedImageById(embeddedImageId.getValue());
                    x.setData(repository.readEmbeddedImageData(embeddedImage));
                    x.remove(PdfName.ID);
                }
            }
        }

        pdfDoc.close();
    }

    public static void main(String[] args) {

        PdfExtractService service = new PdfExtractService();
        try {
            // splits single PDF into single-page PDFs
            int pages = service.splitPdf("/tmp", "src/main/resources/sample.pdf");

            // process each page
            for (int i = 0; i < pages; i++) {

                logger.info("Processing: page-" + (i+1));

                // info about images in PDF
                service.imageInfoFromPdf("/tmp/page-" + (i+1));

                // zero out images in PDF
                service.zeroOutImagesInPdf("/tmp/page-" + (i+1));

                // info about images in PDF
                service.imageInfoFromPdf("/tmp/page-" + (i+1) + ".extracted");

                // reconstruct the pdf with original images
                service.reconstructImageInPdf("/tmp/page-" + (i+1));

                // info about images in PDF
                service.imageInfoFromPdf("/tmp/page-" + (i+1) + ".reconstructed");
            }

        } catch (Exception e) {
            logger.error("Some bad happened", e);
        }
    }
}
