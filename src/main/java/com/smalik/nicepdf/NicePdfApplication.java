package com.smalik.nicepdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class NicePdfApplication implements CommandLineRunner {

    private static Logger logger = LoggerFactory.getLogger(NicePdfApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(NicePdfApplication.class, args);
	}

    @Override
    public void run(String... args) {
        try {

            File sourcePdf = new File(args[0]);
            File destDir = new File(sourcePdf.getParent(), sourcePdf.getName().substring(0, sourcePdf.getName().length()-4));
            destDir.mkdir();

            PdfExtractService service = new PdfExtractService(destDir);
            service.getRepository().load();

            // splits single PDF into single-page PDFs
            int pages = service.splitPdf(sourcePdf, "page-%05d.pdf");

            // process each page
            for (int i = 0; i < pages; i++) {

                String index = String.format("-%05d.pdf", (i+1));
                logger.info("Processing: page" + index);

                // info about images in PDF
                service.resourceInfoFromPdf("page" + index);

                // zero out images in PDF
                service.zeroOutResourcesInPdf("page" + index, "extracted" + index);

                // info about images in PDF
                service.resourceInfoFromPdf("extracted" + index);

                // reconstruct the pdf with original images
                service.reconstructResourcesInPdf("extracted" + index, "reconstructed" + index);

                // info about images in PDF
                service.resourceInfoFromPdf("reconstructed" + index);
            }

            // persist the repository
            service.getRepository().persist();

        } catch (Exception e) {
            logger.error("Something bad happened", e);
        }
    }
}
