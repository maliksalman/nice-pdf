package com.smalik.nicepdf;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.utils.PageRange;
import com.itextpdf.kernel.utils.PdfSplitter;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;

public class PdfExtractService {

    public PdfExtractService() {
    }

    public void splitPdf(final String destFolder, String sourcePdf) throws Exception {

        PdfDocument pdfDoc = new PdfDocument(new PdfReader(sourcePdf));

        List<PdfDocument> splitDocuments = new PdfSplitter(pdfDoc) {

            int partNumber = 1;

            @Override
            protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
                try {
                    return new PdfWriter(destFolder + "splitDocument1_" + String.valueOf(partNumber++) + ".pdf");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException();
                }
            }
        }.splitByPageCount(1);

        for (PdfDocument doc : splitDocuments)
            doc.close();
        pdfDoc.close();
    }

    public void extractImagesFromSinglePdf() throws Exception {

        PdfDocument pdfDoc = new PdfDocument(new PdfReader("/tmp/splitDocument1_1.pdf"));
        PdfPage page = pdfDoc.getFirstPage();
        PdfDictionary dict = page.getPdfObject();

        Set<PdfName> set = dict.keySet();

        for (PdfName pdfName:set) {
            PdfObject pdfObject = dict.get(pdfName);
            System.out.println("Key:" + pdfName + " == value:" + pdfObject  + " == class:" + pdfObject.getClass());
        }
    }
}
