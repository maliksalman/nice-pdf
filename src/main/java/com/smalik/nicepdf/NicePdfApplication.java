package com.smalik.nicepdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class NicePdfApplication {

    private static Logger logger = LoggerFactory.getLogger(NicePdfApplication.class);

	public static void main(String[] args) {
        try {

            File sourcePdf = new File(args[0]);
            File destDir = new File(sourcePdf.getParent(), sourcePdf.getName().substring(0, sourcePdf.getName().length()-4));
            destDir.mkdir();

            PdfExtractService service = new PdfExtractService(destDir);
            service.getRepository().load();

            // splits single PDF into PDFs
            int pages = service.splitPdf(sourcePdf, "split", "%05d.pdf", Integer.valueOf(args[1]));

            // turn off the resource-info analysis
            boolean skipResourcesInfo = args.length < 3 ? false : "true".equals(args[2]);

            // process each page
            for (int i = 0; i < pages; i++) {

                String index = String.format("%05d.pdf", (i+1));
                logger.info("Processing: " + index);

                // info about images in PDF
                if (!skipResourcesInfo) {
                    service.resourceInfoFromPdf("split", index);
                }

                // zero out images in PDF
                service.zeroOutResourcesInPdf("split", "extracted", index);

                // info about images in PDF
                if (!skipResourcesInfo) {
                    service.resourceInfoFromPdf("extracted", index);
                }

                // reconstruct the pdf with original images
                service.reconstructResourcesInPdf("extracted", "reconstructed", index);

                // info about images in PDF
                if (!skipResourcesInfo) {
                    service.resourceInfoFromPdf("reconstructed", index);
                }
            }

            // persist the repository
            service.getRepository().persist();

        } catch (Exception e) {
            logger.error("Something bad happened", e);
        }
    }
}
