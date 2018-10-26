package com.smalik.nicepdf;

import org.junit.Assert;
import org.junit.Test;

public class PdfExtractServiceTests {

    String sourcePdf = "src/main/resources/sample.pdf";

    @Test
    public void testSplitPdfFile() throws Exception {

        //given: we have a multipage PDF file
        PdfExtractService pes = new PdfExtractService();

        //when: we split the pdf file using iText
        pes.splitPdf("/tmp/", sourcePdf);
    }

    @Test
    public void testExtractImagesFromSinglePdf()  throws Exception {

        PdfExtractService pes = new PdfExtractService();
        pes.extractImagesFromSinglePdf();
    }
}
