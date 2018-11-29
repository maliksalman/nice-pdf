package com.smalik.nicepdf;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class MakePdfCopies {

    private static final Logger LOGGER = LoggerFactory.getLogger(MakePdfCopies.class);
    private static final PdfName VERIFY_SECURITY = new PdfName("VerifySecurity");

    public static void main(String[] args) {

        boolean showHelpMessage = true;
        if (args.length == 3) {

            File src = new File(args[0]);
            File dest = new File(args[1]);
            Integer copies = Integer.valueOf(args[2]);

            if (src.exists() && dest.getParentFile().exists()) {
                showHelpMessage = false;
                try {
                    MakePdfCopies makePdfCopies = new MakePdfCopies();

                    List<String> keys = makePdfCopies.createPdf(src, dest, copies);
                    makePdfCopies.verifySecurity(dest, keys);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (showHelpMessage) {
            LOGGER.error("Usage: MakePdfCopies <src-pdf> <dest-pdf> <num-copies>");
            System.exit(1);
        }
    }

    private List<String> createPdf(File srcPdf, File destPdf, int copies) throws Exception {

        long start = System.currentTimeMillis();

        PdfDocument destDoc = new PdfDocument(new PdfWriter(destPdf).setSmartMode(true));
        PdfDocument srcDoc = new PdfDocument(new PdfReader(srcPdf));
        ArrayList<String> keys = new ArrayList<>();
        PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);

        int numSrcPages = srcDoc.getNumberOfPages();
        for (int i = 0; i < copies; i++) {
            srcDoc.copyPagesTo(1, numSrcPages, destDoc);
            PdfPage pageObject = destDoc.getPage((i * numSrcPages) + 1);
            PdfDictionary page = pageObject.getPdfObject();

            String key = UUID.randomUUID().toString();
            keys.add(key);
            page.put(VERIFY_SECURITY, new PdfString(key));

            PdfCanvas canvas = new PdfCanvas(pageObject);
            canvas.beginText().setFontAndSize(font, 9)
                    .moveText(400, 770)
                    .setFillColor(ColorConstants.RED)
                    .showText(key)
                    .endText();
        }

        srcDoc.close();
        destDoc.close();

        LOGGER.info("Made copies: Copies=" + copies + " TimeInMilliseconds=" + (System.currentTimeMillis()-start));
        return keys;
    }

    private void verifySecurity(File destPdf, List<String> keys) throws Exception {

        long start = System.currentTimeMillis();

        HashSet<String> found = new HashSet<>();
        PdfDocument destDoc = new PdfDocument(new PdfReader(destPdf));
        for (int i = 0; i < destDoc.getNumberOfPages(); i++) {

            PdfDictionary page = destDoc.getPage(i+1).getPdfObject();
            PdfString verifySecurity = page.getAsString(VERIFY_SECURITY);
            if (verifySecurity != null) {
                found.add(verifySecurity.getValue());
            }
        }

        boolean containsAll = found.containsAll(keys);
        LOGGER.info("Verified keys: Found=" + found.size() + " AllKeysFound=" + containsAll + " TimeInMilliseconds=" + (System.currentTimeMillis()-start));
    }
}
